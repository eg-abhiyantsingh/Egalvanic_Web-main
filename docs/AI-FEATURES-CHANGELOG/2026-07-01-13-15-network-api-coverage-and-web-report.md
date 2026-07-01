# Network-driven API coverage + full web report + Parallel Suite 2 `api` toggle

- **Title:** Added `NetworkApiContractTestNG` (capture the SPA's live `/api` calls → replay + validate each
  response), a dedicated `api` toggle in Parallel Suite 2 (REST contract + network), and a shareable full
  web-automation coverage report (Artifact). Network run: **28 endpoints, 27 OK, 0 server errors, 1 unbounded.**
- **Date:** 2026-07-01
- **Time:** 13:15
- **Prompt:** "share me full report of web; add this in parallel suite also; cover test cases for API testing
  too — by checking network → then checking response."

---

## What changed

**1. `NetworkApiContractTestNG` (network → response).** Drives each module in a real browser, records every
`/api` GET the SPA fires (via Resource Timing — real endpoints, params, ids), then replays each with an auth
token and validates the response: status, JSON shape, record count, response time. Hard-fails only on
objective defects (a captured GET returning 5xx, or > 8s); reports the rest (4xx-needing-params, unbounded
collections, slow-but-ok) to `reports/network-api-audit-report.md`. Complements the curated deep pagination
audit in `ListApiContractApiTest`.

Live result: **28 distinct endpoints captured across 9 modules, 27 OK, 0 server errors, 0 over 8s, 1
unbounded** (`/users/{id}/slds` → 138). Correctly detected `/v2/issues/list` as POST-only (405, not failed)
and discovered a compliant v2 paginated endpoint (`/lookup/v2/nodes?page=&page_size=25`).

**2. Parallel Suite 2 — new `api` toggle** (opt-in) → two groups: `api-rest-contract` (`suite-api.xml`,
incl. the List-API pagination audit) and `api-network` (`suite-network-api.xml`). Consolidated the earlier
ad-hoc `list-api-contract` entry (it now runs inside `suite-api.xml` under the `api` toggle).

**3. Full web coverage report (Artifact).** Shareable dashboard of the whole web suite: 105 test classes /
1,648 @Test methods (~2,600+ runtime), module-by-module coverage, the last green CI run (both parallel
suites, 0 failures), the list-API contract audit (6/7 violations), and the network-capture result.

## Depth explanation (for learning + manager review)

**Capture-then-replay beats trying to read response bodies from the browser.** Selenium's CDP is version-
brittle here (the "no CDP for 149" warnings), and Resource Timing only exposes URLs, not bodies. So the test
splits the problem: the *browser* answers "which endpoints, with which real ids/params does the app call?"
(the network view), and *REST Assured* answers "what does each of those return?" (the response view) by
replaying the captured GET with a fresh same-user token. Deterministic, no CDP flakiness, and it reuses the
existing REST-Assured auth path. GETs are idempotent so replay is side-effect-free; POST-only endpoints
surface as 405 and are reported, not failed.

**Two API tests, two jobs.** `ListApiContractApiTest` is *depth* — a curated set probed hard for the
pagination/limit/default contract. `NetworkApiContractTestNG` is *breadth* — whatever the app actually calls,
validated for healthy responses and flagged if unbounded. Together they answer the team's three questions
(expected records? paginated? fast?) both precisely and broadly, and both stay green in report mode so they
inform the module-by-module rollout without blocking the pipeline.

## Validation
`suite-network-api.xml` headed: **1 test, 0 failures** — 28 endpoints validated, report written.
Parallel Suite 2 workflow: YAML valid, `api` toggle resolves to `[api-rest-contract, api-network]`.
Report Artifact published (shareable link).
