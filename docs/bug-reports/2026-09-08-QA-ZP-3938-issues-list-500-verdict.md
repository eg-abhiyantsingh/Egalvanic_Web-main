# [Web] Every company Issues list returned 500 once background resolution processing shipped

**QA verdict — PASS on the defect itself. `POST /v2/issues/list` answers 200 on QA (1059 issues), and both pages that depend on it — the company Issues register and Pull-Through Work — render their grids on every load, repeatedly, through a long session. An empty result comes back as a clean empty list with intact stat counts, not an error. One discrepancy: the field the ticket names is not what QA serves — rows carry `resolution_processing`, not `resolution_processing_at` — so step 3 could not be confirmed as written.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat.
**Ticket said "dev only, not yet promoted to cicd/qa" — the endpoint is healthy on QA regardless**, which is what matters for a 500 regression: this build does not reproduce it.

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Load the Issues page for a company with at least one issue and confirm it renders with no 500 | ✅ **PASS** — `/issues` rendered its grid (Asset · Title · Issue Class · Priority · Status) with rows on every visit across the session. |
| 2 | Load the Pull-Through Work view for the same company and confirm the same | ✅ **PASS** — `/pull-through-work` rendered stat cards and 25 of 925–1059 rows, spanning several sites, on every visit. |
| 3 | Call `GET /v2/issues/list` directly and confirm 200 with `resolution_processing_at` present on each row (null for issues never evaluated) | ⚠️ **PARTIAL** — the call is a **`POST`** to `/api/v2/issues/list` on this build and answers **200** with `total: 1059`. But **`resolution_processing_at` is not a key on any row**: the resolution-related keys are `proposed_resolution`, `resolution_counts`, **`resolution_processing`**, `resolution_state`. The KeyError the ticket describes is gone; the field name in the step does not match what QA serves. |
| 4 | Confirm an issue currently being evaluated in the background reports a non-null `resolution_processing_at` | ⚠️ **NOT OBSERVED** — no evaluation was in flight when rows were sampled, and the field as named does not exist. A `Re-evaluate` triggered during this session returned `{issues:1, jobs:0}` and completed without leaving a processing timestamp visible on the list row. |
| 5 | Negative: a company with zero issues should return an empty list, not an error | ✅ **PASS in substance** — the acme tenant has 1059 issues, so the empty case was produced with an unmatchable search: 200 `{items: [], total: 0, stat_counts:{…}}`. A genuinely issue-free company was not available to test. |

---

## Findings
### FINDING 1 (Low, documentation) — the serialized field is `resolution_processing`, not `resolution_processing_at`
The ticket's one-line fix is described as adding `resolution_processing_at` to the select list, and step 3 asks a tester to confirm that key on each row. On QA the rows carry `resolution_processing` instead. Either the field was renamed after the ticket was written, or the ticket text is stale. Worth reconciling so the next tester is not looking for a key that will always read `undefined`.

## Test data — direct links (QA)
- Issues register: https://acme.qa.egalvanic.ai/issues
- Pull-Through Work (same company, same list call): https://acme.qa.egalvanic.ai/pull-through-work
- The list endpoint is `POST https://acme.qa.egalvanic.ai/api/v2/issues/list` with `{company_id, page, page_size, filters, search}`

Evidence: `docs/bug-evidence/zp-3938-issues-list-500/`.

## Not covered / honest gaps
- **A company with genuinely zero issues** — approximated with an unmatchable search.
- **An issue mid-background-evaluation** — none was in flight; the field as named does not exist to check.
- **Roles other than Super Admin** — mandatory MFA on QA blocks fresh logins of the other seats.
