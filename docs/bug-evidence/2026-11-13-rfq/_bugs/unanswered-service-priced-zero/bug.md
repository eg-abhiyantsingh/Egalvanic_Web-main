# [Web] Quote: a self-pricing service with an unanswered pricing question is priced $0 and its labor shows as an unquoted subcontract ("Generate RFQ")

**Severity:** Medium · **Suggested assignee:** Avani (pricing engine). If the intended fix is only the wording, Krunal.
**Related:** ZP-5254 (pricing questions in the setup dialog), ZP-5255 (Pricing inputs panel).
**Re-proved:** 13 Nov 2026, 14:05 IST, build `index-C6DvLko9.js`. Seen 5 times today (runs 09, 11, 13, 18, bugA).

## Simple explanation
- Some services price themselves from a formula that reads answers like "Square footage" or "Number of stories".
- The Build Quote dialog says: *"Anything left blank can be answered later on the Pricing tab — until then those services price from labor and rates."*
- That does not happen. While any of a service's questions is blank, that service sells **$0**.
- Its labor also turns into a subcontract line ("Sub: Yes · 1 unquoted · Generate RFQ"), and the toolbar says "Pending: 1 sub quote to enter".
- So a quote with one missing answer can go out at $0 and points the user to the wrong action (an RFQ).

## Links
- Quote used in the video: https://acme.qa.egalvanic.ai/plans/01a369be-5daa-4bac-a140-d00858125f36. All 4 questions were answered at the end of the video ($4,250). Clear any one answer in Pricing inputs to see the bug again.
- Another quote showing it: https://acme.qa.egalvanic.ai/plans/7f6f4a25-aa8f-4ffd-9446-37e4036cb3d3. Clear "Square footage" in Pricing inputs.

## Actual vs Expected
- **Actual:** only Square footage answered → $0 sell / $0 cost. The labor line reads "Journeyman Electrician · 1 unquoted · Sub Yes · Generate RFQ · $0". The toolbar shows "Pending: 1 sub quote to enter".
- **Expected:** as the dialog says, the 3 services price from labor and rates until answered: 9 h × $150 = **$1,350 sell** ($450 cost). At the very least, show a plain "waiting for answers" state, not an RFQ prompt.

## Test data
- Build `index-C6DvLko9.js`; seat: QA Super Admin (AppConstants `VALID_EMAIL`). The same happens for any seat that can edit quotes.
- Site "QA-DEMO ZP-5108 ZP-5109 12 Nov" (`e1aeadfa-6b58-484a-acf1-1ef09496b8b9`). The services cover 4 assets there (panelboards and switchboards).
- Services (QA-DEMO, de-energized; Journeyman Electrician de-energized rate $150 sell / $50 cost):
  - A `QA-DEMO ZP-5254 13 Nov Visual (A)` `2bdfff84-f608-4152-be34-3b440e3bf06c`: 60 min/asset.
    - Formula: `evaluated_labor + intake.square_footage*0.5 + intake.stories*100 + IF(intake.access=='lift',200,0)`
  - B `… Torque (B)` `598cb2b4-a13e-4981-97a4-51d8ba8127b9`: 30 min/asset.
    - Formula: `evaluated_labor + intake.square_footage*0.25 + IF(intake.access=='lift',300,0)`
  - C `… Testing (C)` `06ec1158-7f20-481a-af3b-29641b6b662d`: 45 min/asset.
    - Formula: `evaluated_labor*intake.crew_size + intake.stories*50`

## Steps to reproduce (from a clean start)
1. Sign in to https://acme.qa.egalvanic.ai as a Super Admin.
2. Sales › Quotes › **New Quote**. Fill in:
   - Title: any.
   - Facility: "QA-DEMO ZP-5108 ZP-5109 12 Nov".
   - Type: **Standard**.
   Then click **Create Quote**.
3. In **Build Quote**, open *Service(s)* and add `QA-DEMO ZP-5254 13 Nov Visual (A)`, `… Torque (B)` and `… Testing (C)` (all assets).
4. Scroll to **PRICING QUESTIONS**. Type **2000** in *Square footage*; leave the other 3 blank.
5. Click **Generate Quote** and look at the **Pricing** tab.
6. You see $0 sell, the labor line "1 unquoted · Sub Yes · Generate RFQ", and "Pending: 1 sub quote to enter".
7. For contrast, answer the other 3 questions in **Pricing inputs**:
   - Number of stories: 3.
   - Crew size: 2.
   - Access: Lift.
   Click away after each. The same labor now prices: A $2,100 + B $1,100 + C $1,050 = **$4,250**.

## Evidence
- Video: `QA-13Nov-BUG-unanswered-service-priced-zero-video-steps.mp4` (51 s, captioned).
- `QA-13Nov-BUG-unanswered-service-priced-zero-1-dialog-says-labor-and-rates.png`: the dialog text promising "price from labor and rates" (orange).
- `QA-13Nov-BUG-unanswered-service-priced-zero-2-zero-price-generate-rfq.png`: "1 sub quote to enter", "Generate RFQ" and the $0 line (red); "3 answers required" (orange).
- `QA-13Nov-BUG-unanswered-service-priced-zero-3-all-answered-for-contrast.png`: after all 4 answers, $4,250 "priced by service equation" (orange).

## Notes for the developer (API evidence)
- Save call: `POST /api/plans/{id}/generate` with `intake: {_shared: {square_footage: "2000"}}`.
- Response for this case:
  - `content.pricing_inputs[*]`: `priced_by_equation:false`, with `missing` = A `[access, stories]`, B `[access]`, C `[crew_size, stories]`.
  - `content.workorders[0].labor_lines`: `[{hours: 9, subcontracted: true, sub_quote_needed: true}]`, with no cost or sell.
  - `content.pricing.pending_subs`: `[{hours: 9, labor_type: "Journeyman Electrician", missing: "quote", workorder: "Shutdown"}]`.
  - `content.pricing.totals`: all 0.
- With every answer present, the same lines come back as `subcontracted:false, priced_by_equation:true`, costs 200 / 100 / 150, and `service_pricing` A 2100 / B 1100 / C 1050.
- Two texts on screen disagree:
  - The Build Quote and EMP wizard text (PlanDetail, "PRICING QUESTIONS" block) says blank services price from labor and rates.
  - The per-work-order calculation text says "N unanswered questions — this service can't price until they're filled in below".
- Please pick one behaviour. Either way, do not flag own-crew labor as a subcontract that needs an RFQ.
- No company labor rounding or mobilization was set at test time (all `null`), so the hours are exact (4 / 2 / 3 h).

## Duplicate check (JQL run 13 Nov)
`project = ZP AND created >= -60d AND (text ~ "Generate RFQ" OR text ~ "unquoted" OR text ~ "sub quote" OR text ~ "labor and rates" OR text ~ "answers required" OR text ~ "self-pricing" OR text ~ "service equation")`
No ticket covers it. Closest are ZP-4218 (RFQ button for a subcontracted line, Done) and ZP-5256 (toolbar move).
