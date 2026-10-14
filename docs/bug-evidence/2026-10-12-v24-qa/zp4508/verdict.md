# ZP-4508: Journal interpretation. Walk-wide fold engine + SQS/Lambda runner (behind JOURNAL_RUNNER, default sfn)

**Verdict:** PARTIAL. The engine behaviour passes on QA's default `sfn` runner: cross-room references, counted groups, 3-pole main, and no reconcile stage. The SQS/Lambda runner is NOT WEB-TESTABLE on QA (QA is not bootstrapped; every job reports `runner: "sfn"`). One medium UX bug: after a failed run the walk shows "Queued" forever and nothing retries it. Intermittent run failures ("the run produced no assembly.json") were also seen.
**Build:** QA `index-BdXGlAq5.js` → `index-D1IWZ6SI.js` (frontend redeployed 12 Oct 13:17:58 UTC = 18:48 IST; journal source maps identical).
**Test data (mine):** walk A https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29 (2 rooms, 2 notes, 2 photos, later +2 notes), control walk B https://acme.qa.egalvanic.ai/site-walks/88d2bb50-f04d-4e79-b696-d277119939ef (same 2 notes, no photos). Site `QA-DEMO ZP-4561 site (delete me)`.

## Interpretation runs observed on QA (all `runner: "sfn"`)
| Walk | Job | Entries | Result | Wall time |
|---|---|---|---|---|
| A | 20914fde | 1 | succeeded | ≈ 55 s |
| A | 608b2c4c | 4 (2 photos) | **failed: "the run produced no assembly.json"** (13:17:30 → 13:18:47 UTC, during the 13:17:58 redeploy) | 77 s |
| A | 157a5117 | 4 (2 photos) | succeeded, 4 rows / 19 units | 74 s |
| B | f5d061dd → 4c17dd26 | 1 → 2 | succeeded, 4 rows / 19 units | ≈ 60 s each |
| A | 4f549f74 | 5 (after Accept) | succeeded, 20 units | ≈ 108 s |
| B | 3d8c40e1 | 3 (after Accept) | **failed** (see B-retry below) | ≈ 99 s |
| B | (retry) | 3 | see "B-retry" | |

## Checked
| Check | How | Result |
|---|---|---|
| Cross-room reference resolves to ONE node | Room 2: "LP-1 is fed from transformer T1 in the main electrical room downstairs" | PASS (A and B). The feed `T1 (room 1) → LP-1` uses the existing T1; no implied second T1. |
| Counted group expands to N nodes with N feeds | "MSB feeds four dry-type transformers T1, T2, T3 and T4" | PASS. 4 transformer units T1–T4 (75 kVA each) and 4 separate feeds, not one asset with quantity 4. |
| 3-phase main = 3-pole line-side node with the rating; 1P/2P branch breakers are not nodes | "2000 amp three-pole main breaker"; walk B "LP-1 has 42 circuits" | PASS. "MSB main" Circuit Breaker, 3P, 2000 A, inside MSB. LP-1's 42 circuits did not become 42 nodes (see B-retry). |
| Feeds attach to the most specific node, and feed resolution never creates nodes | Feeds | PASS. The feeds leave the claimed feeder breakers (T1–T4), not the board; no "implied" units were created. |
| No reconcile pass | Every `/interpret/status` poll | PASS. `progress` was null in every poll and the UI never showed "Reconciling across locations". |
| Latency "≈50–70 s → a few seconds" | Timed above | **Not achieved on QA, as expected.** sfn still takes 55–108 s. The fast path is the SQS Lambda, which QA does not run. |
| Mid-walk save picked up (hash-prefix resume) | Saved 4 changes in about 40 s | PASS (behaviourally). A second run started for the later entries and the final model holds all of them. Checkpoint internals: NOT WEB-TESTABLE. |
| Cancel marker honoured | n/a | NOT WEB-TESTABLE. There is no cancel control on the web. |
| Negative: `JOURNAL_RUNNER=sfn` (default) still works | Every QA run above | PASS. The Step Function path works on QA after the merge. |
| UI during a run | Header and right pane while live | PASS. "Read 0 of 1 · 1 to go · Interpreting" and a "LIVE · 0/1" chip with a progress bar. |
| **After a failed run** | Walk A after 608b2c4c failed; reloaded | **FAIL (UX).** The header says **"Queued"** in orange. Nothing was queued: no new run started for ≥ 9 min (13:18:47 → 13:28, when I edited a note by hand). The real failure text shows only to someone who happened to be polling at that moment; after a reload there is no error. With Re-interpret removed (ZP-4511), the only way to retry is to edit an entry. |
| Roles / tenant / flag / perf | See the ZP-4561 role table (walk endpoints). `feature-site-walks` = true. `GET /site-walk/<id>` ≈ 30 KB, 0.36–0.53 s; status poll ≈ 0.3 s every 3 s while live and every 10 s idle. | See ZP-4561. |

## Bugs found
1. **A failed journal interpretation leaves the walk saying "Queued" forever; nothing retries it and there is no retry action** (Medium; frontend Krunal for the hint, backend Avani for the sweeper retry; owner Eric).
   Steps: 1) Journal walk with notes and photos. 2) Wait for a run that ends `status: failed` (seen on 2 runs today; job 608b2c4c on walk A). 3) Reload the walk page.
   Actual: header "Queued" (orange), no error, no new run for ≥ 9 minutes. `GET …/interpret/status` = `status failed, needs_interpretation true, error "the run produced no assembly.json"`.
   Expected: say "Last run failed" (the code has this branch, but it is unreachable while `needs_interpretation` is true) and retry automatically or offer a "Try again". The ticket removed the manual re-read, so a failed walk otherwise stays un-read until someone happens to edit it.
   Evidence: `QA-12Oct-ZP-4508-3-after-failed-run-header-says-queued-FAIL.png`; code `SiteWalkJournal.jsx` InterpretControls (`needs ? … "Queued"` is checked before `status === "failed"`). No existing ticket covers it.
2. **Intermittent interpretation failures "the run produced no assembly.json"** (observation for the developer, not a confirmed bug). 2 of 9 runs failed on QA today: 608b2c4c (walk A) and 3d8c40e1 (walk B). Other runs on the same walks with the same content succeeded, so I cannot reproduce it on demand. The first failure overlaps the 13:17:58 UTC redeploy. Please check the Step Function executions for those 2 job ids (`eg-pz-qa-…journal…` state machine, CloudWatch for the Fargate task) and say whether it is deploy-related.

## Enhancement suggestions
- Show the job's `warnings` in Journal Review. They are very useful ("pole count assumed", "labels assigned arbitrarily", "catalog number did not fit") but no screen shows them.
- Show elapsed time and "usually about a minute" while Interpreting. The sfn path takes 1–2 min with no stage text (`progress` is always null).

## Flags / environment flips needed
Not a LaunchDarkly flag. The SQS runner needs an **environment change on QA**: run `infra/bootstrap_journal_fold.sh qa`, set `JOURNAL_RUNNER=sqs` and `JOURNAL_FOLD_QUEUE_URL` on `eg-pz-qa-be-td-ohio`, and grant `sqs:SendMessage`. Ask Shubham in eg-internal-dev to have it done when the team wants the SQS path verified on QA. QA will then re-run the same 2 walks and compare latency (target a few seconds per fold).

## Mobile part for Nency
Phone journal walks are read by the same engine. Please dictate a cross-room walk on iOS/Android ("…fed from transformer T1 downstairs") and check the web model shows one T1. Also check the phone shows a sensible state if a run fails (it may have the same "Queued" problem).

## Not covered and why
SQS FIFO/Lambda runner, checkpoint re-send near the deadline, cancel marker, per-entry chain-hash cache, read-only DB user. All are server/infra and not on QA (runner sfn). Developer evidence: CloudWatch for `journal-fold-lambda` on dev showing per-fold durations of a few seconds, and one cancelled run with its cancel marker.

## Proposed Jira comment
```
h3. QA 12 Oct (QA env, build index-BdXGlAq5.js / index-D1IWZ6SI.js): PARTIAL (engine PASS on sfn; SQS runner not on QA) + 1 bug

Walks: QA-DEMO ZP-4561/4564 journal (2 rooms, notes + photos) and a notes-only control. Every job says runner "sfn".
*Passed:* "T1 in the main electrical room downstairs" resolves to the existing T1 (one node); "MSB feeds four transformers T1-T4" -> 4 transformer nodes + 4 feeds; 2000 A three-pole main = 3P node inside MSB; LP-1's 42 circuits are not nodes; no reconcile stage; the sfn default path still works.
*Not on QA:* SQS/Lambda runner, few-second folds (sfn runs took 55-108 s), checkpoint, cancel.
!QA-12Oct-ZP-4508-4-cross-room-lp1-fed-from-t1-PASS.png|width=720!

*Bug:* after a failed run ("the run produced no assembly.json", job 608b2c4c) the header says "Queued" forever; no retry for 9+ minutes until I edited a note. There is no Try again now that Re-interpret is gone.
!QA-12Oct-ZP-4508-3-after-failed-run-header-says-queued-FAIL.png|width=720!
*Please check logs:* 2 of 9 runs failed with "the run produced no assembly.json" (jobs 608b2c4c, 3d8c40e1).

To verify SQS on QA: bootstrap_journal_fold.sh qa + JOURNAL_RUNNER=sqs on eg-pz-qa-be-td-ohio.
Recommended: stay In QA.
```

**Recommended status:** stay In QA (SQS runner not on QA; the failed-run bug can be split into its own ticket).
