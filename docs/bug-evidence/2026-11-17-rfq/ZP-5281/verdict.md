# ZP-5281 — Quote Pricing inputs: a shared choice question showed the green "all answered" check while one service was still unanswered and priced $0

**Verdict: PASS for the reported scenario. One new edge-case bug, filed separately (`BUG-disjoint-choices.txt`).**

- **Build:** QA `index-oFAZQ3rK.js` (17 Nov 2026). PlanDetail chunk `PlanDetail-DTqnObBk.js`.
- **Fix:**
  - eg-pz-backend PR #1818, per Dharmesh's comment. This GitHub account cannot read eg-pz-backend.
  - eg-pz-frontend PR #2036 (merged 16 Nov, 13:13 UTC). `unionPricingQuestions` now keeps only the choices **every** asking service accepts. `offeredAnswer` shows a saved choice the narrowed list no longer offers as "Not answered" and drops it on the next save.
  - On the server side, `content.pricing_inputs[].questions[].options` now comes back narrowed for both services (D shows `[easy, lift]`).
- **Seat:** Super Admin for the matrix. Project Manager and Account Manager: see "Roles".
- **Data (QA-DEMO, site "QA-DEMO ZP-5108 ZP-5109 12 Nov"):**
  - D "Inspection (D)" `4834ac4e…`: easy/ladder/lift.
  - B "Torque (B)" `598cb2b4…`: easy/lift.
  - A "Visual (A)" `2bdfff84…`: easy/ladder/lift.
  - **New** E "QA-DEMO ZP-5281 17 Nov Scaffold (E)" `751591c2-aca7-4e64-9cd2-724d3bd1fca4`: stairs/scaffold, for the no-overlap case.
  - All definitions were re-read today. D still has ladder.

## What was run

| # | Quote | Access offered (Build Quote / Pricing inputs / Edit Quote) | Answer | Result |
|---|---|---|---|---|
| 1 | **D + B, the ticket's steps.** https://acme.qa.egalvanic.ai/plans/84defc7e-f057-4651-833a-76d8a734a99f | **Easy, Lift** in all three places (Ladder is gone) | Lift, Square footage 1000 | Both `missing: []`, `priced_by_equation: true`: D $250 + B $850 = **$1,100**. The green check is now true. No RFQ, `pending_subs: []`. |
| 1b | same | — | switched to Easy | D $250 + B $550 = $800, both priced, green check |
| 2 | Opposite case: **A + D, same 3 choices.** https://acme.qa.egalvanic.ai/plans/249e79ed-69fd-4b03-a3ed-fa024f9baba3 | **Easy, Ladder, Lift** (not narrowed when the lists match) | Ladder, Square footage 1000, Stories 2 | D $300 (ladder +$50) + A $1,300 = $1,600, both priced, green check |
| 3 | **13 Nov BUG-B quote with a saved "ladder"** (read only). https://acme.qa.egalvanic.ai/plans/d17666b2-6fbf-4130-9d94-42dde9feccb1 | Easy, Lift | (stored `_shared.access = "ladder"`) | Access shows **"Not answered"**, header **"1 answer required"**, no green check. B `missing [access]` falls back to labor × rate ($300, ZP-5280), so no $0/RFQ. Total $600. |
| 4 | Video quote (D + B): https://acme.qa.egalvanic.ai/plans/fd23838b-5e16-4cae-b472-6667b80dd9c4 | Easy, Lift | Lift, 1000 | $1,100, both priced |
| 5 | **B + E, no common choice** (new edge case). https://acme.qa.egalvanic.ai/plans/6db35572-aa9b-45cd-85c2-3c73eb35ebc9 | Easy, Lift (B's list, because B sorts first) | Easy, then Lift | Header correctly says **"1 answer required"** (no false green check), and E falls back to labor × rate. **But E can never be answered**: see the new bug. |

The ticket's two expectations:
- (1) "Offer only choices that every asking service accepts": **met** (rows 1, 2, 4).
- (2) "The header must count B's Access as unanswered as long as any service is still missing it": **met** in every state that has a missing service (rows 3 and 5).

## Roles
Project Manager and Account Manager seats, each on its own:
- 13 Nov quote d17666b2 → "Pricing inputs | 1 answer required", no green check.
- D + B quote 84defc7e → green check, $800, both priced. Edit Quote → Access offers "Easy, Lift" only.

Same as Super Admin.

## Not covered / notes
- Backend PR #1818 diff (no repo access); judged by behaviour.
- EMP wizard (same `unionPricingQuestions` merge); not built today.
- Minor observation, legacy data only: on the 13 Nov quote (row 3), Access shows "Not answered", yet D is still priced **with** the stored "ladder" (+$50, D = $300). The stored answer is dropped on the next save, and the header asks the user to answer, so the effect is small. Only quotes saved before the fix (or written through the API) can be in this state.
- Mobile not checked. Quote Pricing inputs is a web Sales screen.

## New defect (separate from this ticket's fix)
**BUG: when two services ask the same choice question with no choice in common, the question can never be answered. One service never prices by its equation, the header says "1 answer required" forever, and Access shows "Not answered" even after a valid pick.** Full draft in `BUG-disjoint-choices.txt`, with video `QA-17Nov-BUG-disjoint-choices-never-answerable-video-steps.mp4` and screenshots `QA-17Nov-BUG-disjoint-choices-never-answerable-1..4`.
