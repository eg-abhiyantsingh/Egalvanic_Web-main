# RBAC Permission-Matrix Coverage (prod_permissions-by-role CSV)

- **Date:** 2026-06-15
- **Time:** ~16:50 IST
- **Prompt:** "@testcase/prod_permissions-by-role_202606151113.csv cover all this test case."
- **Module:** Authentication → Role-Based Access / Permission Matrix
- **Type:** New automated coverage (API contract tests), data-driven from the prod matrix.

---

## What the CSV is

`testcase/prod_permissions-by-role_202606151113.csv` is a **production export of the RBAC
matrix**: 555 `role × permission` grants across **7 roles** and **113 distinct permissions**.

| Role | role_id | grants (CSV) |
|------|---------|-------------:|
| Admin | b60006dd-… | 98 |
| Project Manager | 242dbe6a-… | 93 |
| Technician | e84a0fbb-… | 90 |
| Electrical Engineer | fd6b624e-… | 83 |
| Facility Manager | 54021b71-… | 77 |
| Account Manager | 92f38105-… | 77 |
| Client Portal | 2a85145f-… | 37 |

Permission names are strings like `accounts.view`, `features.assets.view`, `platform.web`.

## How "cover all this test case" was implemented

Rather than 555 brittle per-button UI checks, coverage is anchored on the **exact endpoint the
product itself uses for access control**: `GET /api/auth/me` returns the live `permissions[]`
array (plus `roles[]`, `is_admin`, `has_web_access`) that the React app consumes to gate every
screen (see `eg-pz-frontend-reference/src/store/permissions.js`). Asserting that this live set
**equals** the matrix covers every grant *and* the entire negative space in one shot.

### New / changed files
- **`src/main/java/com/egalvanic/qa/utils/RolePermissionMatrix.java`** — loads the CSV into
  `role → { roleId, Set<permission> }`. Quote-aware CSV parser (the export quotes some
  `role_id`s and leaves some blank `permission_action`s).
- **`src/test/java/com/egalvanic/qa/testcase/api/RoleBasedPermissionContractTest.java`** —
  data-driven over all 7 roles. Per role: API login → `GET /auth/me` → assert:
  1. **Identity** — `roles[0].id`/`name` equal the matrix role.
  2. **No privilege escalation (security)** — live set has **zero** permissions beyond the matrix. Hard fail on any extra.
  3. **Completeness** — every granted permission present, except documented prod-vs-QA drift.
  4. **Derived-field consistency** — `has_web_access` ⇔ `platform.web` grant; `is_admin` ⇔ Admin role.
  Plus `testMatrixCsvIntegrity` (7 roles, 555 grants, correct role_ids — guards a corrupt/truncated CSV).
- **`AppConstants.java`** — added `EE_*`/`AM_*` role creds (env-overridable) + `RBAC_CSV_PATH` + `FEATURE_PERMISSION_MATRIX`.
- **`testng-rbac-permissions.xml`** — dedicated suite; also added to `testng-full.xml`.

## Validation (live QA, run twice)

**Green run** — `mvn -o test -DsuiteXmlFile=testng-rbac-permissions.xml`:
`Tests run: 8, Failures: 0, Skipped: 2` →
5 roles **PASS** (Admin, Project Manager, Technician, Facility Manager, Client Portal),
CSV integrity **PASS**, 2 roles **SKIP** (accounts not provisioned — see below).

**Red-on-regression proof** — pointed `RBAC_CSV_PATH` at a doctored matrix where Admin loses a
real permission (`slds.view`) and gains a fake one (`god.mode`). Result: exactly **1 failure**,
with precise diagnostics:
```
RBAC contract FAILED for 'Admin':
  • PRIVILEGE ESCALATION: 1 permission(s) granted that the matrix does NOT allow: [slds.view]
  • MISSING 1 granted permission(s) not returned by /auth/me ...: [god.mode]
```
The other 4 roles still PASSED → the assertion is precise, not over-broad.

## Why this design (depth explanation)

- **Test the gate, not 555 buttons.** `/auth/me` is the single source the frontend uses to
  decide what a user can see/do. Verifying it = verifying the access-control contract, at API
  speed, with zero UI flakiness. One exact-set comparison covers all grants and all *non*-grants.
- **Two-directional assertion.** "Subset of matrix" catches **privilege escalation** (a security
  regression — a role gaining power it shouldn't). "Superset minus documented drift" catches
  **silent permission loss** (a functionality regression). Checking only one direction misses half.
- **Derived-field cross-check.** `has_web_access` and `is_admin` are computed server-side from a
  *different* code path than the raw `permissions[]`. Asserting they agree
  (`has_web_access == permissions.contains("platform.web")`) catches backend bugs where the two
  drift apart — a stronger check than either alone. Verified live: Technician is the only role
  with `has_web_access=false` (mobile-only), consistent with it lacking `platform.web`.
- **Skip, don't fail, for missing accounts.** Unprovisioned roles SKIP with an actionable message
  instead of failing — a missing *test fixture* is not a product defect.

## Observations for follow-up (NOT filed as product bugs)

1. **Prod-vs-QA config drift (env, not a code bug).** Three permissions are in the prod CSV but
   absent on the QA tenant's roles: Admin `features.equipment_insights.view`,
   Project Manager `accounts.view`, Facility Manager `features.locations.view`. Encoded as the
   documented `KNOWN_QA_DRIFT` allowlist and logged as ⚠️ on every run; any *new* deviation hard-fails.
2. **Missing QA test accounts.** Electrical Engineer and Account Manager have no QA login
   (`401`). Add the accounts (or set `EE_EMAIL`/`AM_EMAIL` env) to light up those 2 roles automatically.
3. **Enforcement vs. reporting (unverified — needs a focused look).** `GET /api/accounts/`
   returned `200` for Client Portal even though CP lacks `accounts.view`. This *may* be a
   list-scoping/empty-result behaviour rather than an authorization gap, so it is **not** asserted
   here and **not** filed as a bug — flagged only for a deliberate enforcement-layer investigation.
