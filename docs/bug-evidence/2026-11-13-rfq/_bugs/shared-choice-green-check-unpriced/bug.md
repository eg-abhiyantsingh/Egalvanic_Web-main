# [Web] Quote Pricing inputs: a shared choice question shows the green "all answered" check while one service is still unanswered and priced $0

**Severity:** Medium · **Suggested assignee:** Krunal (frontend)
**Related:** ZP-5255 (Pricing inputs panel), ZP-5254 (pricing questions in the setup dialog), ZP-5253 (choice answers are reused only when valid).
**Re-proved:** 13 Nov 2026, 14:06 IST, build `index-C6DvLko9.js`. Seen 3 times today (runs 14, 18, bugB).

## Simple explanation
- Two services on one quote both ask "Access", but with different choices:
  - Inspection (D): easy / ladder / lift.
  - Torque (B): easy / lift.
- The quote shows the question once and only offers the **first** service's choices, so "Ladder" is offered.
- If you pick Ladder, the server correctly does **not** give Ladder to Torque (B), because it isn't one of B's choices. So B stays unanswered and prices **$0** ("1 unquoted · Generate RFQ").
- But the Pricing inputs header shows the **green check** ("all answered"), so nothing tells the user that B still needs an answer.
- Result: the quote silently misses B's price ($300 instead of $300 + B's price).

## Links
- Quote in the bug state: https://acme.qa.egalvanic.ai/plans/d17666b2-6fbf-4130-9d94-42dde9feccb1 ("QA-DEMO BUG-B 13 Nov shared choice")
- Earlier quote, same state: https://acme.qa.egalvanic.ai/plans/7d5cbf50-8144-4bf2-93cd-9b242377c589
- Service B's choices: https://acme.qa.egalvanic.ai/services/598cb2b4-a13e-4981-97a4-51d8ba8127b9 (Pricing setup › Access › 2 choices)

## Actual vs Expected
- **Actual:**
  - "Access" offers Easy / Ladder / Lift.
  - After picking Ladder and generating, the header shows the green check and the total is $300 (D only).
  - Torque (B)'s 2 h line shows "1 unquoted · Generate RFQ · $0".
- **Expected:**
  - Offer only choices that every asking service accepts, or ask per service when the choices differ.
  - The header must count B's Access as unanswered ("1 answer required") as long as any service is still missing it.

## Test data
- Build `index-C6DvLko9.js`; seat: QA Super Admin (AppConstants `VALID_EMAIL`).
- Site "QA-DEMO ZP-5108 ZP-5109 12 Nov" (`e1aeadfa-6b58-484a-acf1-1ef09496b8b9`).
- Service D `QA-DEMO ZP-5253 13 Nov Inspection (D)` `4834ac4e-8ab9-449a-bca4-7fe2f3f92b46`:
  - Access easy/ladder/lift, square_footage; 15 min/asset.
  - Formula: `evaluated_labor + intake.square_footage*0.1 + IF(intake.access=='ladder',50,0)`
- Service B `QA-DEMO ZP-5254 13 Nov Torque (B)` `598cb2b4-a13e-4981-97a4-51d8ba8127b9`:
  - Access easy/lift, square_footage; 30 min/asset.
  - Formula: `evaluated_labor + intake.square_footage*0.25 + IF(intake.access=='lift',300,0)`

## Steps to reproduce (from a clean start)
1. Sign in to https://acme.qa.egalvanic.ai as a Super Admin.
2. Optional check: Builder › Services › `QA-DEMO ZP-5254 13 Nov Torque (B)` › **Pricing setup** › Access › **2 choices** shows easy, lift.
3. Sales › Quotes › **New Quote**. Fill in:
   - Title: any.
   - Facility: "QA-DEMO ZP-5108 ZP-5109 12 Nov".
   - Type: **Standard**.
   Then click **Create Quote**.
4. In **Build Quote**, add `QA-DEMO ZP-5253 13 Nov Inspection (D)` and `QA-DEMO ZP-5254 13 Nov Torque (B)`.
5. In **PRICING QUESTIONS**, open **Access**. It offers Easy, **Ladder**, Lift.
6. Pick **Ladder**, type **1000** in Square footage, and click **Generate Quote**.
7. On the Pricing tab:
   - "Pricing inputs" shows the green check.
   - The total is $300 (D only).
   - B's labor line shows "1 unquoted · Generate RFQ · $0".

## Evidence
- Video: `QA-13Nov-BUG-shared-choice-green-check-unpriced-video-steps.mp4` (42 s, captioned).
- `QA-13Nov-BUG-shared-choice-green-check-unpriced-1-B-access-choices-easy-lift.png`: Torque (B)'s Access choices are only easy and lift (orange).
- `QA-13Nov-BUG-shared-choice-green-check-unpriced-2-access-offers-ladder.png`: the merged Access question offers Ladder (red).
- `QA-13Nov-BUG-shared-choice-green-check-unpriced-3-green-check-but-B-unpriced.png`: green check and B's "Generate RFQ" line (red); Ladder and the $300 total (orange).

## Notes for the developer (API evidence)
- The generate body sends `intake: {_shared: {access: "ladder", square_footage: "1000"}}`.
- Response `content.pricing_inputs`:
  - D: `{access: "ladder", square_footage: 1000}`, `missing: []`, `priced_by_equation: true`.
  - B: `{access: null, square_footage: 1000}`, `missing: ["access"]`, `priced_by_equation: false`.
- `pending_subs`: `[{hours: 2, missing: "quote"}]`. Total sell $300 = D only.
  - D's $300 = 1 h × $150 + 1000 × 0.1 + 50 (ladder).
- The backend is right here: ZP-5253 says a choice answer is reused only when it is a valid option for that service.
- The UI side is where it goes wrong:
  - The panel (PlanDetail, the "Pricing inputs" component) merges questions by key. It keeps the **first** service's descriptor (options), and the merged value is the first non-blank value across services.
  - So the unanswered count (`answers required`) is 0 → green check, even though `pricing_inputs[B].missing` is not empty.
  - The Build Quote / EMP wizard merge (`pricing_questions` grouped by key) also keeps the first service's options. Services come in name order, so "Inspection (D)" comes before "Torque (B)".
- Possible fix:
  - Count unanswered from `pricing_inputs[].missing` per service.
  - For select questions, offer the intersection of the options, or show the question per service when the options differ.

## Duplicate check (JQL run 13 Nov)
`project = ZP AND created >= -45d AND (text ~ "pricing inputs" OR text ~ "pricing questions" OR text ~ "pricing question" OR text ~ "intake answer")`
Only ZP-5253, 5254, 5255, 5256 and 5274 came back; none covers this.
