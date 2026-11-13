# [Web] Quote Pricing inputs: pressing Enter in a field does not save the answer

**Severity:** Low · **Suggested assignee:** Krunal (frontend)
**Related:** ZP-5255 (Pricing inputs panel)
**Re-proved:** 13 Nov 2026, 14:07 IST, build `index-C6DvLko9.js`. Seen 3 times today (runs 11, 13, bugC).

## Simple explanation
- In the Pricing inputs panel on a quote's Pricing tab, you type an answer (for example Number of stories = 5) and press **Enter**.
- Nothing is saved. The quote does not re-price, and the cursor stays in the field.
- The answer saves only when you click somewhere else.
- If the user presses Enter and then leaves the page (back arrow, other tab, closes the browser), the answer is lost and the price is stale.
- The code means Enter to save (it tries to "blur" the field on Enter), but it blurs the wrong element.

## Links
- Quote: https://acme.qa.egalvanic.ai/plans/7f6f4a25-aa8f-4ffd-9446-37e4036cb3d3 ("QA-DEMO ZP-5255 13 Nov pricing-inputs quote"). It is back at Stories 3 / $4,250 after the test.

## Actual vs Expected
- **Actual:**
  - Type 5 in *Number of stories*, press Enter, wait 6 s: **0** save calls, the total stays **$4,250**, and the field still has focus.
  - Only after clicking elsewhere: 1 save, and the total becomes **$4,550**.
- **Expected:** Enter saves and re-prices straight away, the same as clicking away. The total should be $4,550 (A $2,300 + B $1,100 + C $1,150).

## Test data
- Build `index-C6DvLko9.js`; seat: QA Super Admin (AppConstants `VALID_EMAIL`).
- Quote `7f6f4a25-aa8f-4ffd-9446-37e4036cb3d3` on site "QA-DEMO ZP-5108 ZP-5109 12 Nov".
- Services A `2bdfff84-f608-4152-be34-3b440e3bf06c`, B `598cb2b4-a13e-4981-97a4-51d8ba8127b9`, C `06ec1158-7f20-481a-af3b-29641b6b662d`.
- Answers before the test: Square footage 2000, Stories 3, Access Lift, Crew size 2.

## Steps to reproduce (from a clean start)
1. Sign in to https://acme.qa.egalvanic.ai as a Super Admin.
2. Open the quote above. The **Pricing** tab shows the **Pricing inputs** panel, all answered, total $4,250.
   - Any quote with a self-pricing service works: build one with the services above via New Quote › Build Quote.
3. Click into **Number of stories**, replace 3 with **5**, and press **Enter**.
4. Wait about 5 seconds. The total stays $4,250; nothing is saved.
5. Click anywhere else on the page, for example the "LABOR" heading. Only now the quote re-prices to **$4,550**.
6. Put it back: Number of stories = 3, then click away.

## Evidence
- Video: `QA-13Nov-BUG-pricing-inputs-enter-no-save-video-steps.mp4` (30 s, captioned).
- `QA-13Nov-BUG-pricing-inputs-enter-no-save-1-before-stories-3-total-4250.png`: starting state (orange).
- `QA-13Nov-BUG-pricing-inputs-enter-no-save-2-enter-pressed-not-saved.png`: 5 typed, Enter pressed, total still $4,250 (red).
- `QA-13Nov-BUG-pricing-inputs-enter-no-save-3-saved-only-after-click-away.png`: after clicking away, $4,550 (orange).

## Notes for the developer
- Network:
  - After Enter: no `POST /api/plans/{id}/generate` for 6 s. `document.activeElement` is still the INPUT (value "5"), and the API still has `intake._shared.stories = "3"`.
  - After clicking away: exactly 1 generate with `stories: "5"`. Response `service_pricing`: A 2300, B 1100, C 1150.
- Likely cause (PlanDetail, "Pricing inputs" component):
  - The number field is an MUI `TextField` with `onBlur` (commit) and `onKeyDown: e => { if (e.key === "Enter") e.currentTarget.blur() }`.
  - `TextField` does not pass `onKeyDown` to the `<input>`; it lands on the wrapper div. So `currentTarget` is the div, and `div.blur()` does nothing.
  - Using `e.target.blur()`, or passing the handler through `inputProps={{ onKeyDown }}`, should fix it.
- The Build Quote dialog's PRICING QUESTIONS has no Enter handling. It saves on Generate, so it is not affected.

## Duplicate check (JQL run 13 Nov)
`project = ZP AND created >= -45d AND (text ~ "pricing inputs" OR text ~ "pricing questions" OR text ~ "pricing question" OR text ~ "intake answer")`
No ticket covers it.
