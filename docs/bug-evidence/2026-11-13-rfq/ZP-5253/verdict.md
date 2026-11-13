# ZP-5253 · One pricing-input answer per question across a plan's services

**Verdict:** PASS on QA, build `index-C6DvLko9.js` (13 Nov 2026, re-checked 14:00 IST). Jira was re-read before testing: no comments.

The ticket says "live on production (hotfix); not yet promoted to dev/qa". QA runs the new resolver anyway. Fingerprint: a value answered only for service A is reused by services B and C (case c4). The old per-service-only lookup cannot do that.

**Recommended status:** READY TO RELEASE. The resolver behaves exactly as described.
The UI side effect of the "choice reused only when valid" rule is filed under ZP-5255 as BUG-B: the panel ticks "all answered" while the other service stays unanswered.

## How it was tested
The test runs on QA-DEMO services A (sqft, stories, access easy/ladder/lift), B (sqft, access easy/lift), C (stories, crew_size) and D (access easy/ladder/lift, sqft). The services are described in ZP-5254 / ZP-5255.
- Resolver matrix: the QA-DEMO plan https://acme.qa.egalvanic.ai/plans/9c9dfd6c-527a-4688-91b6-0d7957abd8f2 was re-generated 11 times with different `intake` objects. Each run used `POST /plans/{id}/generate`, the call the quote page makes when you save. After each run the script read `content.pricing_inputs[].questions[].value`, `missing` and the per-service `service_price`.
- UI: quotes 7f6f4a25 (A+B+C, one panel answer), 7d5cbf50 (D+B choice case), fd5283de (site-walk quote) and the older plan "Q4 2026" 07745b75 (read-only).

## Checked
| Check | How | Result |
|---|---|---|
| QA row 1: one shared answer reaches every service | Panel answer Square footage 2000, then Show calculation per service | PASS. Service A shows Square footage 2000 ($2,100) and B shows 2000 ($1,100) (shots 1–2). API: `intake._shared` only. A, B and C each resolve their own questions, with 0 missing. |
| Shared answer beats the service's own answer | `{_shared:{sqft:2000}, A:{sqft:1500}}` | PASS. A = 2000, B = 2000. |
| Own answer beats another service's answer | `{A:{sqft:1500}, B:{sqft:900}}` | PASS. A = 1500, B = 900. |
| Another service's non-blank answer is reused | `{A:{sqft:1500, stories:4}}` | PASS. B sqft = 1500, C stories = 4. |
| Own blank answer → another service's answer | `{A:{sqft:""}, B:{sqft:900}}` | PASS. A = 900. |
| QA row 2: choice reused only when valid | `{A:{access:"ladder"}}` → B; `{A:{access:"lift"}}` → B | PASS. "ladder" is not one of B's options, so B stays unanswered (`missing:["access"]`). "lift" is reused by B. In the UI, D = Ladder leaves B unanswered (shot 3). Note: `{_shared:{access:"ladder"}}` also leaves B unanswered, which is consistent with the rule. |
| UI answers sent as strings | `"2000"` vs `2000` | PASS. Both resolve to the number 2000 and give the same prices. |
| Math | Every priced case | A = labor $600 (4 h × $150) + sqft×0.5 + stories×100 + 200 (lift). B = $300 + sqft×0.25 + 300. C = $450 × crew + stories×50. At 2000 / 3 / lift / 2 that gives $2,100 + $1,100 + $1,050 = $4,250, which matches the API and the screen. |
| QA row 3: site-walk plans price as before | QA-DEMO walk ffe080df answered sqft 1200 / stories 2 / easy; then `GET /site-walk/{id}/evaluate` vs the walk-sourced quote | PASS. Walk evaluate = $800 (0 counted assets: 1200×0.5 + 2×100). The walk-sourced quote's work order = $800. Adding a plan-level `_shared.square_footage=5000` still gives $800: walk rows keep the walk's answers. Shot 4. |
| QA row 4: older plans keep their pricing | All 109 plans `GET /api/plans`, 13:20 IST (build CDvWXKDj) vs 14:0x IST (C6DvLko9); open 2 older plans in the UI | PASS. 109 / 109 have the same `total_sell` and `modified_at`. Opening "Q4 2026" (25 Oct, $6,762.49) and "Test_26_06 - EMP Test 1" ($51,566.83) sent no plan writes, and the totals were unchanged. Shot 5. Note: no plan on QA was saved with per-service answers before the change (all 109 had empty intake). The "old-style" per-service-only cases above cover that path. |
| Tenant / roles | Same as ZP-5254 | The service lists return only global and Acme services. Plan intake is only written by roles that can edit plans (SA / PM / AM). CP gets 422. |
| Flags | No ticket flag | Not applicable. The v2 plan pages sit under `feature-emp-v2` (ON). |
| Performance | generate per case | 0.33–1.3 s per generate, ~7 KB. |

## Bugs found
None in the resolver itself.
- Related UI bug: ZP-5255 BUG-B. When a shared choice is invalid for one service, the panel still ticks "all answered" and that service prices $0.
- Related: ZP-5254 BUG-A. A service with an unanswered question shows $0 and "Generate RFQ".

## Enhancement suggestions
- When a shared choice can't be reused by a service, say so on that service, e.g. "Access: Ladder isn't an option for Torque (B) — pick one". Today it fails silently.

## Mobile impact
Site-walk intake (mobile walk app) is unchanged by design. Web confirmed the walk path still prices from the walk's own answers.

## Not covered
- Plans really saved before the change with saved answers: none exist on QA, so this was simulated with per-service-only intake.
- Boolean and text question types in the resolver.

## Shared settings
None changed.
