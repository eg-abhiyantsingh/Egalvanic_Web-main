# ZP-5246 — Task #275: Hour-block material units count mobilization time

**Verdict: PASS on QA** — build `index-C6DvLko9.js` (QA web build changed from `index-CDvWXKDj.js` at 13:25 IST; the
pricing chunks are byte-identical after normalising hashes, and every result below was re-run on `index-C6DvLko9.js`).
Tester seat: SA (staff Super Admin). Date: 13 Nov 2026, 13:30–14:00 IST.

**Recommended status: READY TO RELEASE**

## Test data (all mine, QA-DEMO, on QA only)
- Plan `QA-DEMO ZP-5245 13 Nov rounding probe` — https://acme.qa.egalvanic.ai/plans/1dfb8acc-dccd-45ce-94cd-b15bf34adaf6
  (Plans v2, Arc Flash Data Collection = rate-priced, all 141 assets of site SLD `e2c57dc6…`).
- Materials `QA-DEMO ZP-5246 13 Nov 1-Hour Block` ($10, unit 1-Hour Block) and `QA-DEMO ZP-5246 13 Nov 8-Hour Block`
  ($100, unit 8-Hour Block), 0 % margin.
- Only per-plan Pricing Settings were used (no company-level change for this ticket).

## Math (inputs → expected → actual, from `GET /api/plans/{id}`)
Inputs: crew wall-clock `est_mins` 1710 = **28.5 h**; labor EE 25.33 h, JE 23.83 h; moves = 2 floors (C1, F1) + 4 rooms
(C1/R1, F1/R1, F1/Test1, F1/testing) + 1 (49 assets with no location) = **7**.

| Case | Expected | Actual (1-Hour / 8-Hour) | |
|---|---|---|---|
| No mobilization | ceil(28.5)=29, ceil(28.5/8)=4 | ×29 ($290) / ×4 ($400) | PASS |
| Mobilization 30 min | 7×30 = 3.5 h per tech, once → 32.0 h → 32 / 4 (per-line would be 35.5 → 36 / 5) | ×32 / ×4 | PASS |
| Mobilization 60 min | 7 h per tech, once → 35.5 h → 36 / 5 (per-line would be 42.5 → 43 / 6) | ×36 / ×5 | PASS |
| Remove EE mobilization row (JE still 7 h) | longest remaining = 7 h → 36 / 5 | ×36 / ×5 | PASS |
| Remove both mobilization rows | travel leaves → 29 / 4 | ×29 / ×4 | PASS |
| Ticket worked example | ceil(53.1/8)=7, ceil(66.1/8)=9; ceil(121.9/8)=ceil(125.4/8)=16 | same formula as observed | consistent |

The 8-Hour Block moving 4 → 5 with mobilization is the same shape as the ticket's "7 vs 9" example.

## Checked
| Check | How | Result |
|---|---|---|
| Blocks = ceil((crew wall-clock + mobilization)/N) | API math above, 5 states | PASS |
| Mobilization counted once (crew moves together) | 2 techs × 7 h → +7 h, not +14 h | PASS |
| Longest mobilization on the WO is used | removed EE row, JE row kept → blocks unchanged | PASS |
| Removing mobilization rows takes travel back out | UI trash icon "Remove this labor type from the work order" on both MOBILIZATION rows → 29/4 | PASS |
| Company with no mobilization unchanged | baseline (company + plan mobilization off) = 29 / 4, same as before | PASS |
| UI | plan Pricing tab Materials table shows ×29/×32/×36 and ×4/×5 live after each Save; Restore… link offered after removal | PASS |
| API contract | `POST /api/plans/{id}/generate` 200 (0.3–0.9 s); materials carry `quantity`, `unit`, `calculated:true`, cost/sell | PASS |
| Tenant | plan pricing is plan-scoped; company defaults endpoint for another company → 422 `permission_denied` (all seats) | PASS |
| Roles | SA/PM/AM/EE see the plan Pricing tab + Pricing settings; CP 422 on `/api/plans/{id}`. FM: see separate bug below | PASS (FM: new bug) |
| Flags | no LaunchDarkly gate on this logic; plan page reads `feature-planned-work-v2` (live ON). SIMULATED OFF: plan page and pricing still render | n/a |
| Performance | plan page: 38 API calls, no duplicate app calls; only slow call `POST /api/reporting/plan_preview_html` 2.2 s (report preview, not this ticket); plan GET 14 KB | OK |

## Bugs found
None in this ticket's scope. Out-of-scope finding shared with the group (see ZP-5242 verdict, bug 2): a Facility Manager
(guest-portal) seat can read and re-price internal quotes through `/api/plans`.

## Enhancement suggestions
- Show the block math on hover of an hour-block quantity ("28.5 h crew + 3.5 h mobilization = 32 h → 32 × 1-Hour Block"), so an
  estimator can see why the count changed after a mobilization edit.

## Mobile impact
None expected: quotes and their materials are web-only.

## Not covered
- v1 plan engine / legacy quote hour blocks (no v1/legacy fixtures on QA; the ticket names Plans v2 behaviour).

## Evidence
- Video: `QA-13Nov-ZP-5246-video-steps.mp4` (58 s, contact-sheet checked).
- `QA-13Nov-ZP-5246-1-no-mobilization-29x1h-4x8h.png` (PASS)
- `QA-13Nov-ZP-5246-2-mobilization-60-adds-7h-once-36x1h-5x8h.png` (PASS)
- `QA-13Nov-ZP-5246-3-one-mobilization-row-removed-blocks-unchanged.png` (PASS)
- `QA-13Nov-ZP-5246-4-mobilization-rows-removed-back-to-29x1h-4x8h.png` (PASS)
- Raw API logs: `.playwright-mcp/rfq13/pricing-settings/out/r5_engine.json`, `v5246.json`.
