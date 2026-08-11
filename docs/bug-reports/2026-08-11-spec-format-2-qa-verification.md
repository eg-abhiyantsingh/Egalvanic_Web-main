# QA Verification — Accept `spec_format` 2 (UUID catalog refs) in `apply_service_version`

**PR:** eg-pz-backend #937 · **Env:** QA (`https://acme.qa.egalvanic.ai`), V1.36 · **Tested:** 2026-08-11
**Ticket scope said "dev only"** — it is now on QA. Confirmed by the runner's own Step Function ARN:
`arn:aws:states:us-east-2:…:eg-pz-**qa**-ai-service-spec-sfn-ohio`.

Backend repo is not readable to me, so everything below is measured from the running system.

## Result: 3 pass, 1 partial, 2 not verifiable by me

| # | QA item | Verdict |
|---|---|---|
| 1 | Apply a format-2 spec, `_finalize_service_build` applies it | ✅ **PASS** |
| 2 | Labor, presets **and** test-equipment resolve by UUID | ⚠️ **PARTIAL** — labor yes; presets/test-equipment never exercised |
| 3 | Tenancy: another company's id is refused | ❌ **NOT TESTED** — needs a second tenant |
| 4 | A global id resolves for any company | ✅ **PASS** |
| 5 | Regression: format-1 versions still apply via name fallback | ✅ **PASS** |
| 6 | Kickoff payload carries `service_type` and `user_id` | ❌ **NOT OBSERVABLE** from the client |

## Evidence

Service **`abhiyant cortniess`** (`e40d05ec-…537e95`, company-owned, `is_global:false`) has 6
versions, read via `GET /api/procedures-v2/services/{id}/versions/{versionId}`:

| Version | `spec_format` | Catalog refs | Created |
|---|---|---|---|
| **v1** `e3d27bc4` | **2** | **`labor_type_id` × 70 — all UUIDs**, `labor_type` names alongside | 2026-08-10 08:15 |
| v2–v5 | 1 | names only | 08:15–10:01 |
| v6 `9a17a111` (current) | 1 | `labor_type` × 72 names, **0 UUID refs** | 10:02 |

### Item 1 — format-2 spec applied ✅
`build_job` on that service:
```json
{ "status": "applied",
  "applied_version_id": "e3d27bc4-934c-4b46-a626-d8353a5b21c5",
  "job_id": "ed9687a0-c4eb-4175-8d6b-a601a271aead",
  "execution_arn": "…eg-pz-qa-ai-service-spec-sfn-ohio:ed9687a0…",
  "error": null, "started_at": "2026-08-10T08:12:52Z" }
```
`applied_version_id` points **exactly** at the `spec_format: 2` version, `error` is null, and the
service carries its 35 procedures. `_finalize_service_build` applied a runner-produced format-2
spec on QA.

### Item 4 — global ids resolve for a company-owned service ✅
The spec's 70 `labor_type_id` refs resolve to **2 distinct UUIDs**, both present in
`GET /api/labor-types` (9 entries) and **both global** (`company_id: null`) — applied
successfully to a **company-owned** service. That is precisely the "global id resolves for any
company" branch.

### Item 5 — format-1 name fallback intact ✅
Versions 2–6 are `spec_format: 1` with **zero** UUID refs and 72 name-based `labor_type` values;
v6 is current with the same 35 procedures. Format-1 specs created *after* the format-2 apply
still apply normally.

### Item 2 — PARTIAL, and this is the gap worth acting on ⚠️
Labor resolution by UUID is proven (70 refs). **Presets and test-equipment are not.** In the
applied format-2 spec every procedure carries:
```json
"material_presets": [],  "test_equipment": []
```
Both arrays are **empty throughout**, so the UUID-resolution path for presets and test-equipment
was **never executed** by this build. The `_catalog_maps` scoped-id logic for those two catalogs
is untested on QA.

**To close it:** produce a runner spec for a service whose procedures actually carry material
presets and test equipment, then re-check that the applied version resolves both by UUID.

### Item 3 — NOT TESTED ❌
Confirming "an id belonging to another company is refused" requires an id from a second tenant.
I have one company and one login, and tenancy correctly prevents me from seeing another
company's catalog ids — so I cannot construct the negative case. A random UUID would only test
*unknown id*, not *foreign-tenant id*, which is a different code path.

### Item 6 — NOT OBSERVABLE ❌
`service_type` / `user_id` are added to the payload the **backend** sends to the runner Step
Function. `build_job` exposes only `applied_version_id`, `error`, `execution_arn`, `job_id`,
`revision_request`, `started_at`, `status` — no kickoff payload. Verify from the Step Function
execution input in AWS, or from backend logs.

---

## Adjacent observation (NOT this PR's defect) — build jobs stuck in `running`

Two of the four build jobs on QA have been `running` for ~a day with `error: null` and no applied
version:

| Service | Status | Started | Age | Applied |
|---|---|---|---|---|
| abhiyant cortniess | applied | 2026-08-10 08:12 | 24.3 h | ✅ |
| NETA Re-TEST | applied | 2026-08-10 08:50 | 23.7 h | ✅ |
| **Test** | **running** | 2026-08-10 08:46 | **23.7 h** | ❌ |
| **70 B Service Type** | **running** | 2026-08-10 11:26 | **21.1 h** | ❌ |

Either those Step Function executions never terminated, or a terminal state is not written back
to `build_job` — in which case the UI shows a permanent "building" state with no error. Worth a
separate look at the build lifecycle; it is not evidence against #937 (the two that finished
finished cleanly, one of them via format 2).
