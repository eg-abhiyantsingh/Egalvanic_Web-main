# RBAC Per-Cell Matrix Coverage — every cell of the sheet is a test case

- **Date:** 2026-06-15
- **Time:** ~18:20 IST
- **Prompt:** "i want you to cover every test case for this sheet rbac @testcase/prod_permissions-by-role_202606151113.csv"
- **Module:** Authentication → Role-Based Access / Permission Matrix
- **Type:** Granular coverage expansion (per-cell), building on the role-level contract test.

---

## What "every test case" means here

The sheet is a 7-role × 113-permission matrix. Earlier work asserted each role's permission set
**as a whole** (7 role-level tests). This change makes **every cell its own reported test case**:

| | count | assertion |
|---|------:|-----------|
| **granted cells** (role has the permission) | 555 | `/auth/me` MUST contain it (positive) |
| **denied cells** (role lacks the permission) | 236 | `/auth/me` MUST NOT contain it (negative) |
| **total** | **791** | one TestNG test case each |

7 × 113 = 791. Every cell of the sheet is now independently asserted and individually visible in
the report — true 1:1 coverage, including the entire negative space.

## How

- **`RolePermissionMatrix.allPermissions()`** — new accessor: the full 113-permission vocabulary
  (union across roles), i.e. the universe each role is checked against cell-by-cell.
- **`RbacFixtures`** (new, shared) — single source of truth for the role roster + credentials,
  expected role_ids, the documented `KNOWN_QA_DRIFT`, and a **cached** login+`/auth/me`
  (`cachedLiveAuth`) so the whole suite logs in each role **once** (not per cell, not per class),
  with a retry to ride out auth throttling. `RoleBasedPermissionContractTest` was refactored onto it.
- **`RolePermissionMatrixCellTest`** (new) — `@DataProvider` emits all 791 cells; each cell asserts
  presence/absence against the cached live set. granted-but-absent that is documented drift → SKIP;
  granted-but-absent otherwise → FAIL; denied-but-present → FAIL (privilege escalation);
  unprovisioned role → all its cells SKIP. Report groups cells under `RBAC: <role>` → `<perm> [granted|denied]`.
- Suites: cell test added to `testng-rbac-permissions.xml` and `testng-full.xml`.

## Validation (live QA)

**Green** (`mvn -o test -DsuiteXmlFile=testng-rbac-permissions.xml`):
`Tests run: 799, Failures: 0, Skipped: 344`. Per-cell: **450 PASS, 0 FAIL, 341 SKIP** of 791.
The 341 skips = 3 unprovisioned roles × 113 (339) + 2 documented drift cells. Math closes exactly.

**Red-on-regression** (doctored CSV targeting live Client Portal): exactly **2 cell failures**, both precise:
```
PRIVILEGE ESCALATION: matrix does NOT grant 'jobs.view' to 'Client Portal' but /auth/me returns it.
MISSING permission: matrix grants 'god.mode' to 'Client Portal' but /auth/me does NOT return it.
```
Only the doctored cells failed → per-cell precision confirmed.

## Credential status discovered this run (action items, NOT product bugs)

- **Admin** now returns `{"error":"Invalid credentials"}` for the shared password `RP@egalvanic123`
  (Project Manager / Technician / Client Portal still work with it). The Admin password was rotated
  to a unique one — same migration as **Facility Manager** (now `+fm` / unique password). Provide the
  Admin password (and create **Electrical Engineer** / **Account Manager** accounts) to light up their
  cells automatically; the test SKIPs them until then.
- Prod-vs-QA drift unchanged: Admin `features.equipment_insights.view`, PM `accounts.view`,
  FM `features.locations.view` (documented `KNOWN_QA_DRIFT`).

## Hardening from an adversarial self-review

A 4-dimension review workflow (logic / CSV-parsing / auth-cache / integration), each finding then
independently verified, surfaced **6 confirmed issues** (2 refuted). All fixed:

| Sev | Issue | Fix |
|-----|-------|-----|
| CRITICAL | `JsonPath.getBoolean()` returns a **primitive** in RestAssured 5.4 → NPE if `/auth/me` omits `is_admin`/`has_web_access`, swallowed by `catch`, silently downgrading a 200 role to UNPROVISIONED (all its cells SKIP, suite stays green) | read flags as **boxed** `jsonPath().get(...)` (null, no unbox); narrow the catch |
| HIGH | `cachedLiveAuth` memoized **any** failure (incl. transient 5xx/timeout) for the whole run → one blip silently drops a role's coverage | only cache **authoritative** results (`LiveAuth.cacheable()`: 200, or genuine 401/403); transient → re-attempted |
| MEDIUM | retry covered only login + fired on deterministic 401 (wasting ~2s × 3 roles) | shared `callWithRetry`, **transient-only** (`0/429/5xx`), applied to `/auth/me` too — suite runtime **19s→11s** |
| MEDIUM | pinned `555`/`113` magic numbers + dated CSV path → confusing red / false green on re-export | derived invariant `sum == totalGrants()` (also catches duplicate rows) + labeled `EXPECTED_SNAPSHOT_*` constants + `resolveCsv()` globs newest `prod_permissions-by-role_*.csv` |
| MEDIUM | all 791 cells named `permissionCell` in native/JUnit/CI reports | implement `ITest` → cells render `Role / perm [granted\|denied]` |
| MEDIUM | RBAC suite missing `ConsoleProgressListener` every other suite has | added |

Re-validated after fixes: green (799 / 0 fail, 450 cell PASS / 341 SKIP) **and** red-on-regression
(doctored CP → exactly 2 cell failures with correct messages).

## Why per-cell on top of role-level

- **Pinpoint reporting.** A role-level failure says "Client Portal is wrong"; a cell failure says
  "Client Portal must not have `jobs.view`" — the exact offending grant, as its own red line.
- **Negative space is first-class.** The 236 denied cells explicitly prove each role LACKS what it
  shouldn't have — privilege-escalation coverage, not just "has everything it should".
- **Login-once caching** keeps 791 assertions to 7 logins, so the granularity costs no extra runtime.
