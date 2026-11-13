# ZP-5255 · Plan-level pricing inputs panel on the Pricing tab

**Verdict:** PARTLY on QA, build `index-C6DvLko9.js` (13 Nov 2026, re-checked 14:00 IST).
The panel does its main job. Three panel bugs were found, listed below.
Jira was re-read before testing: no comments.

The QA build already carries ZP-5256 (a separate Eric ticket, Resolved). That ticket moves the panel onto the pricing toolbar as a foldable "Pricing inputs" line with a green check or an amber "N answers required". It drops the "services priced by equation" count from the header. So that one QA row is judged against the ZP-5256 design, and the missing count is not a defect.

**Recommended status:** READY TO RELEASE, with BUG-B, BUG-C and BUG-D filed as separate bugs (Krunal). The main purpose works end to end.
If the owner wants the header-count edge case (BUG-B) fixed inside this ticket, keep it In QA instead.

## Test data (QA-DEMO, mine)
These are the services A, B and C from ZP-5254, plus:
- Service D `QA-DEMO ZP-5253 13 Nov Inspection (D)`, Access options easy/ladder/lift. It sorts before B.
- Quotes:
  - Pricing-inputs quote https://acme.qa.egalvanic.ai/plans/7f6f4a25-aa8f-4ffd-9446-37e4036cb3d3: A+B+C in one Shutdown.
  - Choice-merge quote https://acme.qa.egalvanic.ai/plans/7d5cbf50-8144-4bf2-93cd-9b242377c589: D+B.
  - Walk-sourced quote https://acme.qa.egalvanic.ai/plans/fd5283de-8efd-4fb4-a895-d0c1d5cc2f0c, from QA-DEMO walk ffe080df.
  - No-pricing quote https://acme.qa.egalvanic.ai/plans/3a1c6d20-9c00-4f52-8a8d-babd9d68217a.

## Checked
| Check | How | Result |
|---|---|---|
| QA row 1: panel on the Pricing tab, each question once | Pricing tab of the A+B+C quote | PASS. "Pricing inputs" sits at the top of the Pricing tab, on the toolbar row per ZP-5256. It lists Crew size, Number of stories, Access and Square footage once each. The tooltip shows which services ask each one. It is open by default and folds. |
| QA row 2: one answer re-prices every service that uses it | Change Square footage from 2000 to 3000 in the panel and click away | PASS. 1 generate, `_shared.square_footage="3000"`. A 600+1500+300+200=**$2,600**, B 300+750+300=**$1,350**, C unchanged **$1,050**, total **$5,000**. Back to 2000 gives $4,250. Shot 2. |
| QA row 3: clearing clears for all | Empty Square footage | PASS. The key is removed from `_shared`. A and B both report `missing:["square_footage"]`. The header shows "1 answer required" and the total drops to $1,050. Shot 1, which also shows BUG-A. |
| QA row 4: header counts | Partial and full answers | PASS for the normal cases: 3 → 2 → 1 → green check as answers are added. **FAIL** for a merged choice question whose option is invalid for one of the services (BUG-B). The "services priced by equation" count is superseded by ZP-5256. |
| QA row 5a: hidden for walk-sourced quotes | Walk-sourced QA-DEMO quote | PASS. No panel. The quote prices from the walk's own answers ($800). Shot 5. |
| QA row 5b: hidden for external viewers | Customer View (`?view=customer`); FM and CP seats | PASS. No panel in Customer View (shot 3). FM is sent to the Maintenance Portal and CP gets 422 / portal, so neither can open the plan page. |
| QA row 5c: hidden without pricing | `include_pricing:false` quote | PASS. No Pricing tab, no panel. |
| QA row 5d: read-only when locked | Approve the outage window, Mark as Sent, open the Pricing tab | PASS. Status "Sent to Customer". All panel inputs are disabled (`disabled` / `aria-disabled=true`) and Edit Quote is disabled. Shot 4. Back to Draft afterwards restored "draft". |
| Writes | Network capture | Each committed answer makes 1 `POST /plans/{id}/generate` with the whole instructions. It writes `_shared[key]` and deletes the same key from per-service intake. ~0.4–0.8 s per save. |
| Roles | SA, PM, AM, FM, CP | SA, PM and AM can see and edit the panel. FM and CP have no plan page. The FM API is an out-of-scope lead; see ZP-5254. |
| Flags | No ticket flag. `feature-emp-v2` ON live; OFF SIMULATED | With the flag OFF, existing v2 quotes still show the panel. New quotes are classic only. |
| Performance | Plan page load | 28 API calls, none over 2 s, no true duplicates. |

## Bugs found
**BUG-B: A merged choice question shows "all answered" while one service is still unanswered and unpriced.** Medium. Suggested: Krunal (frontend).
1. Quote with QA-DEMO ZP-5253 13 Nov Inspection (D) (Access: easy/ladder/lift) and QA-DEMO ZP-5254 13 Nov Torque (B) (Access: easy/lift).
2. Build Quote. The single "Access" question offers D's options: Easy, Ladder, Lift.
3. Pick Ladder, set Square footage 1000, then Generate Quote.
- Actual: the panel shows the green check (all answered) with Access = Ladder. B gets no answer, because Ladder is not one of its options. B reports `missing:["access"]`, and its 2 h show as "1 unquoted · Generate RFQ · $0". The total of $300 is D only.
- Expected: offer only options valid for every service that asks the question, or ask it per service. Either way, count B's question as unanswered ("1 answer required").
- Re-proved twice (runs 14 and 18).
- Evidence: `QA-13Nov-ZP-5255-6-FAIL-all-answered-but-B-unpriced.png`, `.playwright-mcp/rfq13/pricing-questions/out/shots/d-access-options.png`.

**BUG-C: Pressing Enter in a Pricing inputs field does not save.** Low. Suggested: Krunal.
1. Pricing tab: type 3 in Number of stories.
2. Press Enter and wait 6 s.
- Actual: no save (0 generate calls) and focus stays in the field. The answer saves only after clicking somewhere else.
- Expected: Enter saves, which is what the code intends: `onKeyDown` with Enter calls `blur()`. It is attached to the TextField wrapper, so `currentTarget` is a div, not the input.
- Re-proved twice (runs 11 and 13).
- Evidence: `.playwright-mcp/rfq13/pricing-questions/out/13.json` (`T1_enter`).

**BUG-D: Two quick edits in the panel lose one answer.** Medium. Suggested: Krunal.
1. Pricing tab: type 2 in Crew size.
2. Straight away, open Access and pick Easy.
- Actual: two generate calls 200 ms apart. The second one is built from the old plan (it carries crew_size 5, the earlier value). The plan ends with Lift and the Easy pick is lost. In run 11 the Number of stories answer was lost the same way. The Access select reported `aria-disabled=true`, but its already-open menu still took the click.
- Expected: no answer is lost. Queue the saves, or build each save from the latest answers.
- Re-proved twice (runs 11 and 13).
- Evidence: `out/13.json` (`T3_race`), `out/11.json` (`writes`), `out/shots/t3-after-race.png`.

BUG-A ($0 plus "Generate RFQ" for a service with an unanswered question) also shows on this panel. It is written up in ZP-5254.

JQL run for B, C and D: `project = ZP AND created >= -45d AND (text ~ "pricing inputs" OR text ~ "pricing questions" OR text ~ "pricing question" OR text ~ "intake answer")`. Only ZP-5253, 5254, 5255, 5256 and 5274 came back; none covers these bugs.

## Enhancement suggestions
- Show per service, in the panel, which services are priced by equation and which are waiting for an answer. That was the count ZP-5255 promised, and it would make BUG-A and BUG-B visible.
- Save on Enter and show a small "Saving…" / "Saved" state next to the field, so the user knows the re-price happened.

## Mobile impact
None. The panel is web only.

## Not covered
- A real LD flip: no ticket flag.
- Client Portal as an "external viewer" on the plan page: CP cannot open plans at all (422).
- Boolean and text question types in the panel.

## Shared settings
None changed. Plans I set to Sent were put back to Draft: 7f6f4a25 and ccce32ba, both read "draft" afterwards.
