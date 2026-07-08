# CI/CD API test data-field audit — 3 response-envelope fixes

- **Date:** 2026-07-08
- **Prompt:** "check all the test cases data field in cicd, fix them in local first and check in git."
- **Type:** Correctness audit of API-test data-field references vs the live API + fixes.

---

## Method
Parallel per-class audit (20 CI/CD API classes) extracting every payload key + response-field read, calling
each endpoint live, diffing against the real response, then adversarially re-verifying each mismatch (curl).
**7 audit units clean; 3 real envelope bugs fixed**, each re-validated live.

## Fixes
1. **ApiHealthCheckApiTest** — quoteId resolved via `GET /quotes/` (returns SPA HTML) reading `data[]` →
   always null → `/planned-workorders/by-quote/{quote_id}` silently skipped every run. Fixed to
   `GET /company/{id}/quotes` reading `quotes[0].id`. (50/50 green; quote now resolves + endpoint probed.)
2. **WorkOrderEditEnforcementApiTest** — `GET /job/{id}` is `{job:{…},success}`; the whole envelope was sent
   as the PUT body (backend ignored it). Fixed to unwrap `.job`, drop server-managed fields; corrected the
   "never mutates real data" docstring (a real edit bumps `modified_at`). (5/5 green.)
3. **MutationSemanticsApiTest** — `readTaskTitle` read `data.title` off the FLAT `GET /task/{id}` → always "".
   Fixed to envelope-tolerant. (`detailReadable<3s=true` now.)

## Retraction
Fix #3 invalidates the earlier "read-after-write lag" claim — it was this test's envelope bug, not a backend
lag. Memory corrected.

## Verified envelope map
`task/create`→`{data,message}`; `GET task/{id}`→FLAT; `job/`→`{jobs[]}` but `job/{id}`→`{job}`;
`company/{id}/quotes`→`{quotes[],count}`; `/users/` & `/issue_classes`→bare arrays; `/v2/issues/list`→`{data:{items,total}}`.

## Files
`ApiHealthCheckApiTest`, `WorkOrderEditEnforcementApiTest`, `MutationSemanticsApiTest` (+ changelog, memory).
Validated locally then committed to `main`.
