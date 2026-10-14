# ZP-4719 — Staff activity-report endpoint for the internal MCP

**Verdict:** PASS (deny half) / NOT WEB-TESTABLE (staff happy-path) — build `index-D1IWZ6SI.js`. Endpoint `GET /api/staff/activity-report?start=&end=` present (swagger, tag "staff", security `@require_eg_staff` via X-Act-As-Company). The web app never calls it; the consumer is the internal MCP `get_customer_activity`.

## Checked
| Check | How | Result |
|---|---|---|
| Every customer seat refused | 9 seats: Super Admin, Admin, PM, AM, EE, FM, CP, Tech, Portal-Sales, valid range + `X-Act-As-Company: acme` | **401 `eg_staff_denied`** — "Your token was not issued by the Egalvanic staff directory." for ALL nine. PASS (shot -1) |
| No auth | missing bearer | 401 `eg_staff_denied` "Staff authentication required." PASS |
| No `X-Act-As-Company` header | staff-shaped call, no header | 401 (guard fires before tenant resolution). PASS |
| Range validation surface | start>end, bad format, >3 years, no params | all 401 — the `@require_eg_staff` guard runs *before* range parsing, so the `400 invalid_range` path is unreachable from any web seat. Range validation itself NOT web-testable. |
| Foreign company header | `X-Act-As-Company: {another company id}` from staff-shaped customer token | 401 (no staff token). PASS |
| Other staff routes consistency | `/staff/companies|sites|accounts|entitlements|module-roles` | all 401 `eg_staff_denied` from customer + Super-Admin-customer tokens. Consistent. |
| Data leak to non-staff | response body on refusal | only `{error, message, success}` — no users/sites/assets/WO aggregates. PASS (shot -2) |

Note: the QA "Super Admin"/"Admin" seats are customer tokens (not the staff directory), so even they are refused — correct, because `@require_eg_staff` checks the token *issuer*, not a role. Staff-console/MCP tokens are the only ones that pass, and those are out of scope per the brief ("Do NOT call the Egalvanic - Internal MCP tools").

## Bugs found
None. The deny path is clean and leaks nothing.

## Enhancement suggestions
- The two refusals use different messages ("token was not issued by the staff directory" vs "Staff authentication required") — minor, but one canonical `eg_staff_denied` message is clearer.

## Mobile part for Nency
Nothing — this is an internal MCP endpoint with no mobile surface.

## Not covered (why)
- Staff happy-path: 200 with aggregate blocks (users; sites with live+deleted assets; assets/day; WO/tasks/issues by month; customer actions by day/user/site), eGalvanic-staff excluded + counted separately, no row-level PII. **NOT WEB-TESTABLE** — needs a staff-directory (Cognito staff pool) token, which only the internal MCP / staff console holds; the brief forbids calling that MCP.
- Range `400 invalid_range` cases — unreachable without a staff token.
- These must be verified by the dev (Avani) or via the MCP by the main session.

## Proposed Jira comment
h3. QA — ZP-4719 PASS (deny path) / staff happy-path needs the MCP (QA build index-D1IWZ6SI.js)

{{GET /api/staff/activity-report}} is {{@require_eg_staff}}. Verified from the web:
* All 9 seats — Super Admin, Admin, PM, AM, EE, FM, CP, Technician, Portal Sales — get *401 eg_staff_denied* ("token was not issued by the Egalvanic staff directory"), for a valid range *and* for start>end / bad-format / >3y (the staff guard runs before range parsing).
* No-auth and missing {{X-Act-As-Company}} → 401. Response body to a non-staff caller is only {{\{error,message,success\}}} — no tenant aggregates leak.

!QA-12Oct-ZP-4719-1-customer-seat-refused-401-proof.png|width=720!

The staff 200 happy-path (aggregates, staff excluded+counted, range 400s) is *not web-testable* — it needs a staff-directory token / the internal MCP consumer, out of scope here. Suggest the dev confirm the 200 shape + {{400 invalid_range}} via the MCP.

!QA-12Oct-ZP-4719-2-range-and-no-leak-proof.png|width=720!

## Recommended status
**Stay In QA** — web-observable deny path passes cleanly, but the staff happy-path + range validation still need a staff-token / MCP check (dev or main session) before RTR.
