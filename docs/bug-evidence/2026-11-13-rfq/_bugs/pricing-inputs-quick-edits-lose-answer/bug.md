# [Web] Quote Pricing inputs: two quick edits lose one of the answers

**Severity:** Medium · **Suggested assignee:** Krunal (frontend)
**Related:** ZP-5255 (Pricing inputs panel)
**Re-proved:** 13 Nov 2026, 14:09–14:14 IST, build `index-C6DvLko9.js`. It depends on timing: an answer was lost in **4 of 5** quick-edit attempts today (runs 11, 13, bugD try 1, bugD-2 try 2). In the one attempt where both stuck, the second pick came about 0.4 s after the first save.

## Simple explanation
- In the Pricing inputs panel each answer saves on its own when you leave the field. Each save re-prices the whole quote.
- If you change one answer and go **straight** to the next one (type a Crew size, then right away pick an Access option), two saves go out about 0.25–0.3 s apart.
- The second save is built from the quote **as it was before** the first save came back. So it sends the **old** value of the first answer and overwrites it.
- The user sees their first answer jump back to the old value, and the quote is priced on the wrong input. After a reload it is still the old value, so the answer is really lost.

## Links
- Quote: https://acme.qa.egalvanic.ai/plans/7f6f4a25-aa8f-4ffd-9446-37e4036cb3d3 ("QA-DEMO ZP-5255 13 Nov pricing-inputs quote"). It is back at Crew 2 / Lift / $4,250 after the test.

## Actual vs Expected
- **Actual:** wanted Crew size 4 and Access Lift. Saved Crew size **3** and Lift; the 4 was lost. Total **$4,700**.
  - In the first video run: wanted Crew 3 and Easy, saved Crew **2** and Easy, total $3,750.
- **Expected:** both answers are kept: Crew size 4 and Lift. Total **$5,150**:
  - A $2,100
  - B $1,100
  - C $450 × 4 + $150 = $1,950

## Test data
- Build `index-C6DvLko9.js`; seat: QA Super Admin (AppConstants `VALID_EMAIL`).
- Quote `7f6f4a25-aa8f-4ffd-9446-37e4036cb3d3` on site "QA-DEMO ZP-5108 ZP-5109 12 Nov".
- Services A `2bdfff84-f608-4152-be34-3b440e3bf06c`, B `598cb2b4-a13e-4981-97a4-51d8ba8127b9`, C `06ec1158-7f20-481a-af3b-29641b6b662d` (C's formula is `evaluated_labor*intake.crew_size + intake.stories*50`).
- Start state: Square footage 2000, Stories 3, Access Lift, Crew size 2 ($4,250).

## Steps to reproduce (from a clean start)
1. Sign in to https://acme.qa.egalvanic.ai as a Super Admin.
2. Open the quote above and go to the **Pricing** tab. **Pricing inputs** shows Crew size 2, Access Lift.
3. Click **Crew size**, change it to **4** (or 3).
4. **Without pausing**, click the **Access** dropdown and pick a different option (Easy, or Lift if Easy is selected). Do this within about 0.3 s of leaving Crew size.
5. Wait about 5 s for the quote to re-price.
6. Look at Crew size: it is back to its previous value. Reload the page and it is still the previous value.
7. If both values stick, repeat steps 3–5 a little faster. Today it failed in 4 of 5 tries.
8. Put the answers back afterwards: Crew size 2, Access Lift.

## Evidence
- Video: `QA-13Nov-BUG-pricing-inputs-quick-edits-lose-answer-video-steps.mp4` (41 s, captioned). The first try in the video happened to keep both answers. The second try (Crew 4 → Lift) loses the Crew size, and a reload confirms it.
- `QA-13Nov-BUG-pricing-inputs-quick-edits-lose-answer-1-crew-typed-then-access-picked.png`: a value typed in Crew size, Access menu open for the second edit (orange).
- `QA-13Nov-BUG-pricing-inputs-quick-edits-lose-answer-2-one-answer-lost.png`: Crew size shows 3 although 4 was typed (red); the total of $4,700 is priced without it (orange).
- `QA-13Nov-BUG-pricing-inputs-quick-edits-lose-answer-3-after-reload-still-lost.png`: after a reload, still 3 (red).

## Notes for the developer (network evidence)
Save bodies (`POST /api/plans/{id}/generate`, `intake._shared`) in the losing tries:

| Try | 1st save (Crew size leaves focus) | 2nd save (Access picked) | Gap | Ends with |
|---|---|---|---|---|
| Run bugD-2, try 2 | `{crew_size:"4", access:"easy"}` | `{crew_size:"3", access:"lift"}` | 244 ms | crew 3 / lift; the 4 is lost |
| Run bugD, try 1 | `{crew_size:"3", access:"lift"}` | `{crew_size:"2", access:"easy"}` | 291 ms | crew 2 / easy; the 3 is lost |

In each 2nd save, the crew value (`"3"` and `"2"`) is the old one.

How it goes wrong:
- The panel's commit handler (PlanDetail: copy `plan.instructions` → set `intake._shared[key]` → POST generate → replace the plan with the response) copies whatever plan is in state at that moment.
- The second commit runs before the first response has landed, so it carries the old value of the first key. The server keeps whichever save it processes last.
- While a save runs, the Access select gets `aria-disabled="true"`, but a menu that is already open still accepts the click.

Possible fixes:
- Queue the saves.
- Build each save from the latest local answers (a ref, or a functional update) instead of the plan snapshot.
- Send only the changed key, and let the server merge it.

## Duplicate check (JQL run 13 Nov)
`project = ZP AND created >= -45d AND (text ~ "pricing inputs" OR text ~ "pricing questions" OR text ~ "pricing question" OR text ~ "intake answer")`
No ticket covers it.
