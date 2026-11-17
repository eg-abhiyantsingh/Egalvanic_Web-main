# ZP-5283 [Web] Quote Pricing inputs: two quick edits lose one of the answers: verdict

**Result: PASS → READY TO RELEASE.** I ran the ticket's race 10 times on the ticket's quote: type a Crew size, then pick Access 0.20–0.36 s later. Both answers were kept every time, in the saved intake and after a reload. The two saves now run one after the other. The second save starts 0–9 ms after the first one returns, and it carries the new Crew size. On 13 Nov the second save went out ~0.25 s after the first, in parallel, with the old Crew size. The totals always match a slow, single save of the same answers. The other writes the PR moved into the same queue also keep both edits: a labor Rate pick, a Pricing settings save, and the walk-quote "Show calculation" answers. A failed first save no longer leaves a half-saved state.

- **Build:** QA `index-CkkAeA9o.js` (deployed 14:45 IST). Re-checked at 15:35 IST. Tested 14:50–15:35 IST. The fix was already in the earlier build `index-LGhrn-48.js`, whose `PlanDetail-CdTHcrUi.js` also had the queue.
- **Fix:** eg-pz-frontend PR #2039 (`bugfix/zp-5283/quote-pricing-inputs-quick-edits-answer-loss` → `cicd/qa`), merged 06:22 UTC. It changes only `src/pages/PlanDetail.jsx`:
  - It adds a per-quote write queue (`enqueuePlanWrite`). Each write is built from the plan the previous write returned (`latestPlanRef`), not from the snapshot taken at render time.
  - The queue owns the spinner (`rendering`).
  - A job whose quote is no longer on screen is skipped, and its in-flight response is dropped.
  - The sibling-quote switcher is disabled while saving.
  - A picked answer stays on screen while its save waits (`setDraft`).
  - Every handler that replaces the plan now uses the queue: pricing inputs (plan and walk), rate overrides, sub quotes, ride-through, pricing settings, recipients, vendors, approvals, change orders, status actions, reset, roster/variants, EMP wizard and refreshes.
  - I confirmed the live `PlanDetail-DzTpIkz4.js` has this code.
- **Seat:** Super Admin (the ticket's seat). No feature flag is involved.
- **Quote:** https://acme.qa.egalvanic.ai/plans/7f6f4a25-aa8f-4ffd-9446-37e4036cb3d3 ("QA-DEMO ZP-5255 13 Nov pricing-inputs quote").

## The ticket's race, timed with Playwright (in-page timestamps on `focusout`, option click and the `POST /api/plans/{id}/generate` fetch)
| Try | Wanted | Pick after leaving Crew size | 1st save body (crew/access) | 2nd save body | 2nd save started after 1st returned | Saved (API) | Total | Kept? |
|---|---|---|---|---|---|---|---|---|
| 1 | 4 / easy | 205 ms | 4 / lift | **4** / easy | 0 ms | 4 / easy | $5,325 | yes |
| 2 | 3 / lift | 216 ms | 3 / easy | **3** / lift | 2 ms | 3 / lift | $5,262.50 | yes |
| 3 | 5 / easy | 203 ms | 5 / lift | **5** / easy | 2 ms | 5 / easy | $5,887.50 | yes |
| 4 | 4 / lift | 207 ms | 4 / easy | **4** / lift | 2 ms | 4 / lift | $5,825 | yes |
| 5 | 6 / easy | 228 ms | 6 / lift | **6** / easy | 1 ms | 6 / easy | $6,450 | yes |
| 6 | 3 / lift | 266 ms | 3 / easy | **3** / lift | 1 ms | 3 / lift | $5,262.50 | yes |
| 7 | 2 / easy | 359 ms | 2 / lift | **2** / easy | 1 ms | 2 / easy | $4,200 | yes |
| 8 | 4 / lift | 235 ms | 4 / easy | **4** / lift | 5 ms | 4 / lift | $5,825 | yes |
| video 1 | 4 / Easy | 231 ms | 4 / lift | **4** / easy | 9 ms | 4 / easy | $5,325 | yes |
| video 2 | 3 / Lift | 339 ms | 3 / easy | **3** / lift | 4 ms | 3 / lift | $5,262.50 | yes |

- On screen, 150 ms after each pick, both the typed Crew size and the picked Access showed (the draft is held). The UI matched the saved values after the re-price.
- After a reload, the last answers were still there: try 8 gave Crew 4 / Lift / $5,825, and the video gave Crew 3 / Lift.
- Totals: every total equals Access part + $562.50 × Crew size + $150. The Access part is $3,425 for Lift and $2,925 for Easy. The same numbers come from slow single saves: the baseline save of Crew 2 / Lift is $4,700, and the restore gives $4,700.

**Why the totals differ from the ticket ($4,250 for Crew 2 / Lift, $5,150 for Crew 4 / Lift).** All three labor lines on this quote now have 25% more hours than on 13 Nov: 5 / 2.5 / 3.75 h instead of 4 / 2 / 3 h, so 11.3 est hrs instead of 9.
- The quote has no pricing overrides (`instructions.pricing` is null), and the company plan-pricing defaults are all null.
- It was already like that on my first save, before I changed anything. The stored content from 10:32 IST still had the old hours.
- So the pricing basis changed somewhere else today. It is not caused by this fix, because quick and slow saves give the same totals.
- I did not chase the cause. It is worth a look if the 13 Nov numbers matter to someone.

## Other orders and the other queued writes
| Case | Result |
|---|---|
| Access first, then Crew size (typed 0.6 s later), click away | PASS. 2 saves. The 2nd carried access easy + crew 5. Saved 5 / easy, $5,887.50. |
| Crew size, then Number of stories straight away | PASS. Both saved (crew 4, stories 4). The stories field is disabled while the first save runs, so typing goes in once it re-enables (~1.4 s here). |
| Three quick edits: Crew size → Access → Number of stories | PASS. 3 saves, one after the other, each carrying the earlier answers. Saved crew 6 / lift / stories 3, $6,950. |
| Crew size 5, then straight away the labor Rate dropdown › "Auto (best match)" (271 ms) | PASS. The rate save carried crew 5 (`rate_overrides: {}`), and crew 5 was saved. |
| Crew size 3, then straight away Pricing settings (gear) › Per move 30 › Save Changes (0.77 s) | PASS. The 2nd save carried crew 3 + `mobilization_minutes: 30`. Both saved. |
| Walk-sourced quote › Show calculation › Square footage 1300, then Access › Ladder (319 ms) (`handleSetWalkIntake`) | PASS. `site_walk.intake_overrides` = square footage 1300 + access ladder, $850. |
| First of two quick saves fails (500, simulated in my browser only) | PASS. An error banner shows. The 2nd save still runs, built from the last good plan. Crew size goes back to 2 on screen, so the lost edit is visible, not silent. The spinner clears. |

Side check, no defect: on the walk quote, "Not answered" for Access and clearing Square footage behave the same. Both show "1 unanswered question — … prices from labor and rates." The total of $0 is correct, because that walk has 0 labor hours.

## Restore
- Pricing quote: back to Crew 2 / Access Lift / Square footage 2000 / Stories 3. The instructions are byte-identical to the start of each run (`same: true`). Total **$4,700** on today's pricing basis; the ticket's $4,250 no longer applies (see above).
- Walk-sourced quote https://acme.qa.egalvanic.ai/plans/fd5283de-8efd-4fb4-a895-d0c1d5cc2f0c: restored to its exact instructions ($800).
- Pricing settings (Per move 30) and the rate pick were undone by the exact restore. No company setting was changed.

## Not covered
- The sibling-quote switcher being disabled while a save runs. This quote's opportunity has no second quote, and I did not create one.
- Regression of the other refactored handlers I didn't click: approvals in Action Items, change orders, Mark as Sent / status actions, Reset, recipients, vendor assignment, PM roster/variants, the EMP wizard. They are covered only by reading the diff. A short regression pass on these before release would be good.
- Roles other than Super Admin: the panel itself was checked for PM/AM on 13 Nov (ZP-5255), and the fix doesn't change permissions.

## Enhancement ideas
- Other answer fields are disabled while a save runs, so fast typing into a second number field is dropped until it re-enables (0.3–1.9 s here). Now that saves queue safely, the fields could stay editable, with a small "Saving…" next to the panel title.
- The error banner for a failed save shows raw text: "API call failed: 500 Internal Server Error - \{json\}".
- Seen on the walk quote: the AI text "HOW THIS PRICE WAS REACHED" still said "access rating was easy" after Access was set to Ladder. The price itself was right ($850). This was not investigated.

## New defects
None.

## Cross-platform
- **Twins:** none. JQL for "pricing inputs" / "pricing questions" / "intake answer" / "quote pricing" (60 days) finds only web tickets: ZP-5253…5256, 5278…5283, 5304.
- **iOS:** not on this platform (the pricing inputs).
  - In the iOS source (`ios-src`, release/qa 1.75), the Quotes tab (`Views/Quotes/QuotesView.swift`) lists local `Quote` records tied to an SLD (title, description, status counts). Its `QuoteDetailView` is a placeholder that shows "Quote details would appear here...".
  - The `Quote` model holds title/sow/tnm/status only. There are no pricing inputs and no `/plans/{id}/generate` call.
  - iOS does have site-walk intake questions (`SiteWalkDetailView` › Questions, `IntakeQuestionRow`). These are saved locally on each change and pushed later. They feed walk-sourced quotes.
- **Android:** not checked (no Android source available to me, emulator lane busy).
- **Checks for the device testers:**
  - iOS, QA build: Site › Quotes tab › open any quote. Expected: only title/description and the placeholder; no pricing inputs to edit.
  - iOS: Site Walks › walk "QA-DEMO ZP-5255 13 Nov walk" (ffe080df-6d87-4cf8-8c0c-5b7ececc9532) › Questions. Change Access and Square footage one right after the other, and wait for sync.
    - On web, the walk-sourced quote https://acme.qa.egalvanic.ai/plans/fd5283de-8efd-4fb4-a895-d0c1d5cc2f0c › Show calculation should show both new values as "Walk recorded".
    - Expected: both kept.
    - Put the answers back afterwards: Access Easy, Square footage 1200.
  - Android: does the app have Quotes or site-walk questions? If yes, do the same two checks.

## Evidence
- Video: `QA-17Nov-ZP-5283-quick-edits-both-kept-video.mp4` (42 s: two timed tries, reload, restore)
- `QA-17Nov-ZP-5283-1-quick-edit-both-answers-kept.png`, `-2-after-reload-still-kept.png`, `-3-walk-quote-dialog-both-kept.png`
- Raw:
  - `raw/b1.json`: the 8 timed tries, other orders, reload, restore.
  - `raw/b2.json`, `raw/b3.json`: the Pricing settings, rate and walk-dialog races.
  - `raw/b4.json`: walk "Not answered" side check.
  - `raw/b0.json`: labor hours / pricing basis.
  - `raw/v5283.json`: video run.
  - Screenshots `raw/b*.png`.
  - Simulated-failure run: `raw/b5.json` (shot `raw/b5-first-save-fails-simulated.png`).
