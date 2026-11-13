# ZP-5254 · Ask each self-pricing service's pricing questions in the plan setup dialogs

**Verdict:** PASS on QA, build `index-C6DvLko9.js` (13 Nov 2026, deployed 07:55 GMT / 13:25 IST).
The round started on `index-CDvWXKDj.js`. Only the read-only discovery runs (01–04) used that build. Every check below ran on `C6DvLko9`, which was re-checked at 14:00 IST. The `PlanDetail` chunk is byte-identical between the two builds; only the import hashes differ.
Tester seat: Super Admin (SA), plus PM / AM / FM / CP for the role checks. Jira was re-read before testing: no comments.

**Recommended status:** READY TO RELEASE. One new bug is filed separately (BUG-A below; also listed under ZP-5255).

## Test data (QA-DEMO, created by me; no QA service had pricing questions before)
At the start, all 28 QA services returned `pricing_questions: []`. All 109 plans had empty intake. There were no self-pricing services with questions anywhere on EG-ACME.
I created the data below with the same endpoints the Create Service ("Create Without AI"), service versions (Add asset class / Pricing setup) and PM-standard screens call.
- Service A `QA-DEMO ZP-5254 13 Nov Visual (A)` (de-energized, 60 min/asset on panelboard and switchboard), questions square_footage, stories, access (easy/ladder/lift).
  - `service_price = evaluated_labor + sqft×0.5 + stories×100 + IF(access=='lift',200,0)`
  - https://acme.qa.egalvanic.ai/services/2bdfff84-f608-4152-be34-3b440e3bf06c
- Service B `… Torque (B)` (30 min), questions square_footage, access (easy/lift).
  - `evaluated_labor + sqft×0.25 + IF(access=='lift',300,0)`
  - https://acme.qa.egalvanic.ai/services/598cb2b4-a13e-4981-97a4-51d8ba8127b9
- Service C `… Testing (C)` (45 min), questions stories, crew_size, plus `site_notes`, which the equation does not read.
  - `evaluated_labor×crew_size + stories×50`
  - https://acme.qa.egalvanic.ai/services/06ec1158-7f20-481a-af3b-29641b6b662d
- PM standard `QA-DEMO ZP-5254 13 Nov standard` (A+B+C on panelboard and switchboard, 12 months): https://acme.qa.egalvanic.ai/pm-plans/c5d8046e-48d2-4de5-893d-d75a413f3994
- Quotes, all on site "QA-DEMO ZP-5108 ZP-5109 12 Nov" (4 covered assets):
  - ad-hoc https://acme.qa.egalvanic.ai/plans/ccce32ba-35ad-4296-b28f-a1ee38e0eabc
  - EMP https://acme.qa.egalvanic.ai/plans/542e33a2-08f1-4f47-9f88-2539626bca93
  - walk-sourced https://acme.qa.egalvanic.ai/plans/fd5283de-8efd-4fb4-a895-d0c1d5cc2f0c
  - no-pricing https://acme.qa.egalvanic.ai/plans/3a1c6d20-9c00-4f52-8a8d-babd9d68217a

## Checked
| Check | How | Result |
|---|---|---|
| API GET `/procedures-v2/services` | SA, PM, AM, FM in-page fetch | PASS. 200 for each. Every row has `pricing_questions`. For A, B and C it returns only `{key,label,type,options,required,value}`: no `expr`, no `formulas`, no `site_walk_config`. `site_notes` (not read by the equation) is excluded. 251 ms, 3.4 KB. |
| API GET `/procedures-v2/pm-plans/services?pm_standard_id=` | SA, PM, AM, FM | PASS. 200 with the same descriptors for the QA-DEMO standard (672 ms). A random uuid gives 403 "pm_standard_id not available to this company". A missing id gives 400. A non-uuid gives 500 `internal_error` (minor, see below). |
| Tenant scope | company_id of every row, all seats | PASS. Only `null` (global) and Acme. CP gets 422 `permission_denied` on both. |
| QA row 1: section lists each question once | Quotes › New Quote (Standard) › Build Quote, add A+B+C | PASS. PRICING QUESTIONS shows 4 rows for 7 asks (Crew size, Number of stories, Access, Square footage). Hovering a label lists the services, e.g. "Square footage ← Torque (B), Visual (A)". Shot 1. |
| QA row 2: first generate prices by equation | Answer 2000 / 3 / Lift / 2, then Generate Quote; capture network and read `GET /plans/{id}` | PASS. Exactly 1 `POST /plans/{id}/generate`, body `intake._shared={square_footage:"2000",stories:"3",access:"lift",crew_size:"2"}`. First render: A 600+1000+300+200=**$2,100**, B 300+500+300=**$1,100**, C 450×2+150=**$1,050**, total **$4,250**. Labor checks out at 9 h × $150 sell / $50 cost. The math matches the screen. Shot 2. |
| QA row 3: Edit seeds saved answers | Edit Quote on the same quote | PASS. 2000 / 3 / Lift / 2 are pre-filled. Shot 3. |
| QA row 4a: hidden when pricing is off | Quote with `include_pricing:false` (created through `POST /plans`; New Quote always forces pricing on opportunity quotes), then Edit Quote | PASS. No PRICING QUESTIONS section, no Pricing tab. The EMP wizard shows no "Include pricing" toggle on opportunity quotes (`forcePricing`), so that toggle cannot be tested from the UI. |
| QA row 4b: hidden for walk-only rows | Walk-sourced QA-DEMO quote › Edit Quote | PASS. A "Site walk" row shows no section (shot 4). Switching the row to "Both" makes the section appear, which proves the check discriminates. |
| QA row 5: AI Edit gone | Plan page buttons and text; bundle | PASS. No "AI Edit" / "Edit with AI" button or text on the plan page. The string is absent from every chunk. |
| QA row 6: combined shutdown with several self-pricing services | Ad-hoc quote; EMP wizard on the QA-DEMO standard | PASS. The ad-hoc quote makes one "Shutdown" WO with A+B+C, all 3 priced. EMP wizard step 1 shows the same 4 questions once (shot 5). Generate makes 6 annual shutdowns, each carrying A $2,100 / B $1,100 / C $1,050. Year 1 is $4,250, later years add 3 %/yr inflation, full quote $27,490.70 (shot 6). |
| String answers | UI sends strings | PASS. The engine stores `"2000"` and evaluates it as 2000. |
| Roles | SA, PM, AM, FM, CP | SA, PM and AM open the quote, see the section and panel, and can edit. FM is sent to the Maintenance Portal (no plan page). CP gets 422 on the APIs and the portal UI. See the out-of-scope note on FM API access. |
| Flags | Live LD for Acme: `feature-emp-v2`=ON, `feature-planned-work-v2`=ON; no new flag for this ticket | SIMULATED `feature-emp-v2` OFF: New Quote offers only "Classic quote, built by hand" (no v2 builder, so no questions). Existing v2 quotes still open with the panel. |
| Performance | Plan page load on the 3-service quote | 28 API calls. None over 2 s, and no true duplicates (3× `procedures?service_id=`, one per service). `POST generate` 0.4–0.8 s, ~7 KB. One-off: `GET /plans?opportunity_id=` took 11.7 s once (not repeated later). |

## Bugs found
**BUG-A: A self-pricing service with an unanswered question is priced $0, and its labor turns into an unquoted subcontract.** Medium. Suggested: Avani (engine), with Krunal if only the copy changes.
1. Quotes › New Quote › Standard, site "QA-DEMO ZP-5108 ZP-5109 12 Nov".
2. Build Quote: add QA-DEMO ZP-5254 13 Nov Visual (A), Torque (B), Testing (C).
3. Answer only Square footage = 2000, then Generate Quote.
4. Look at the Pricing tab.
- Actual: $0 sell. The labor line shows "Sub: Yes · 1 unquoted · Generate RFQ", and the toolbar shows "Pending: 1 sub quote to enter". The API returns `subcontracted:true, sub_quote_needed:true, pending_subs[{missing:"quote"}]`.
- Expected: the dialog says "until then those services price from labor and rates". That would be 9 h × $150 = $1,350, or at least a plain "waiting for answers" state, not an RFQ prompt.
- Re-proved 4 times on 13 Nov: runs 09, 11, 13, 18.
- Evidence: `../ZP-5255/QA-13Nov-ZP-5255-1-clear-sqft-clears-for-all.png` (A and B lines show "1 unquoted · Generate RFQ · $0"), `.playwright-mcp/rfq13/pricing-questions/out/13.json` (`partialState`).
- JQL run: `project = ZP AND created >= -60d AND (text ~ "Generate RFQ" OR text ~ "unquoted" OR text ~ "sub quote" OR text ~ "labor and rates" OR text ~ "answers required" OR text ~ "self-pricing" OR text ~ "service equation")`. No match.

Minor:
- `GET /procedures-v2/pm-plans/services?pm_standard_id=not-a-uuid` returns 500 `internal_error`. It carries a trace_id and leaks nothing, but should be 400. This is the same class as the closed tickets ZP-4842 and ZP-4855. API only. Avani.

## Enhancement suggestions
- Show which services ask each question inline (e.g. "used by 2 services"), not only in a hover tooltip. Users can't hover on touch screens.
- In the setup dialog, mark questions that are left blank as "service will not price yet". Today a blank answer silently zeroes that service (BUG-A).

## Mobile impact
None directly: plan setup is web only. Mobile site-walk intake uses the same service `site_walk_config`. A tester could check that a walk with two services sharing a question still asks it once. The site-walk path itself was unchanged on web: the walk evaluated to $800 and the walk-sourced quote to $800.

## Not covered
- A real LD flip: there is no ticket flag, and `feature-emp-v2` OFF was only SIMULATED.
- "Include pricing" toggle in the EMP wizard: hidden for opportunity quotes by design (`forcePricing`).
- Yes/no and Text question types inside an equation.
- Corrective (issue) work rows in the ad-hoc builder.

## Out-of-scope lead (for the main session)
The FM seat (guest-portal Facility Manager) is never shown a plan page. Through the API it still gets:
- `GET /api/plans`: 120 quotes.
- `GET /api/plans/{id}`: cost, margin and intake.
- `POST /plans/{id}/generate` with an empty body returns 400 validation, not 403. That means the write passes the role check.

This was re-proved in runs 15 and 20. JQL `summary ~ "Facility Manager" … quote|plans|margin|cost` found nothing that covers it. It is outside these tickets, so I recommend a separate check.

## Shared settings
None changed. Company labor rounding and mobilization read `null` at 13:3x IST. Evaluated hours were exact (4 / 2 / 3 h).
