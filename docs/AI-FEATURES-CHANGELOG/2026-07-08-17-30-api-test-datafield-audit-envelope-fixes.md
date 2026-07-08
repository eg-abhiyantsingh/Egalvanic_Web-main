# Data-field audit of the CI/CD API tests — 3 response-envelope bugs fixed (+ 1 retraction)

**Date:** 2026-07-08
**Author:** Claude Code (automation), for abhiyant.singh@egalvanic.com

## What was asked
> "check all the test cases data field in cicd, fix them in local first and check in git."

Audit every **data-field reference** (payload keys written + response fields read) in the CI/CD API test
classes against the **live** API, fix any stale/wrong ones locally, validate, and commit.

## Method
A parallel audit workflow: one agent per class (20 classes across `suite-api-health.xml` + `suite-api.xml`)
extracted every `.put(...)` payload key and every `.opt/get*("field")` / `.has(...)` / jsonPath read, then
**called each endpoint live** and diffed the referenced fields against the real response. Every flagged
mismatch was then **adversarially re-verified** (curl again, default to "not a bug" unless independently
reproduced). I re-confirmed all survivors myself with curl before editing.

Result: **7 audit units clean**, **3 real bugs** (all response-envelope mismatches, all curl-reproduced).

## Fixes (each validated live after the change)
1. **`ApiHealthCheckApiTest`** — resolved `quoteId` via `GET /quotes/`, which returns the **SPA HTML shell**
   (not JSON), and read a `data[]` array. So `quoteId` was **null every run** and the
   `/planned-workorders/by-quote/{quote_id}` endpoint was **silently skipped and never health-checked**.
   Fixed to resolve via `GET /company/{companyId}/quotes` (envelope `{count, quotes, success}`) reading
   `quotes[0].id`. Verified: `quote=5c3d7803-…` now resolves and the by-quote endpoint is probed (50/50 green).
2. **`WorkOrderEditEnforcementApiTest`** — `GET /job/{id}` returns a **wrapped** `{job:{…}, success}` envelope,
   but the test stored the whole envelope as the PUT body (comment claimed "bare job object"). The backend
   silently ignored the unrecognized top-level keys, so the job's own fields never round-tripped. Fixed to
   **unwrap `.job`** and drop server-managed fields (`modified_at`/`created_at`); corrected the docstring's
   "never mutates real data" claim (a successful edit still bumps the server-managed `modified_at`). Verified
   5/5 green, RBAC enforcement assertions intact.
3. **`MutationSemanticsApiTest`** — `readTaskTitle()` read `optJSONObject("data")` on `GET /task/{id}`, but that
   GET returns a **FLAT** object (title at top level, no `data` wrapper) — only `POST /task/create` wraps in
   `{data:{…}}`. So it always returned `""`. Fixed to be envelope-tolerant. Verified: `detailReadable<3s=true`
   now (was false).

## Retraction (accuracy correction)
Fix #3 **invalidates the "read-after-write lag" finding** I reported in the 2026-07-08 CRUD-suites changelog
and memory. There is **no read-after-write lag** — `GET /task/{id}` reflects a direct-write create immediately;
the earlier `detailReadable<3s=false` was caused by this test's own `data.title` envelope bug, not backend
behavior. Memory has been corrected; the misleading warning wording in the test is fixed so it now only fires
on a genuine lag.

## Envelope map (verified live — reference for future tests)
`POST /task/create` → `{data,message}` · `GET /task/{id}` → **FLAT** · `GET /job/` → `{jobs[],success}` but
`GET /job/{id}` → `{job,success}` · `/company/{id}/quotes` → `{count,quotes,success}` · `/users/` → bare array ·
`/issue_classes` → bare array · `/v2/issues/list` → `{data:{items,total}}` · `/company/{id}/slds` → `{slds[],count}`.
The safe pattern (now used) is envelope-tolerant: `o.optJSONObject("data") != null ? o.getJSONObject("data") : o`.

## Clean (verified, no changes needed)
`AuthenticationAPITest`, `BaseAPITest` (login → `access_token`, correct), `UserAPITest` (`/users/`, status-only),
`RbacFixtures` + `RoleBasedPermissionContractTest`, `RoleCrud`/`RoleAction`/`RolePermissionMatrix`,
`NodeClass`/`EdgeClass`/`IssueClass` taxonomy contracts, `ListApiContractApiTest`, `APISecurityTest`,
`APIPerformanceTest`, and the 4 recent CRUD suites' payloads (Playwright-captured).

## Depth note (learning / manager review)
The bug class here is subtle and worth internalizing: **the same resource has different envelopes on different
verbs/paths** — `create` wraps in `data`, single-GET is flat, list-GET wraps in a plural key, and one entity's
detail-GET wraps in a singular key. A test that hardcodes one envelope silently mis-reads another: it doesn't
crash, it returns `""`/`null`, so the assertion either skips or "passes" while checking nothing. That's exactly
how my own `read-after-write lag` mirage happened. Two lessons: (1) read responses envelope-tolerantly; (2) when
a test reports a *backend* anomaly, first rule out that the test is mis-reading the payload — verify the raw
bytes before believing the narrative.
