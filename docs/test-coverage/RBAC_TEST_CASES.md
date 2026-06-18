# RBAC — Complete Test-Case Catalog

Source of truth for permissions: `testcase/prod_permissions-by-role_202606151113.csv`
(7 roles, 113 distinct permissions, 555 grants). The per-cell enumeration (791 rows) lives in
[`RBAC_TEST_CASES.csv`](RBAC_TEST_CASES.csv).

## Roles under test (credentials)

| Role | Email | Password | Web access? |
|------|-------|----------|:-----------:|
| Admin | `abhiyant.singh+admin@egalvanic.com` | `RP@egalvanic123` | ✅ |
| Project Manager | `abhiyant.singh+project@egalvanic.com` | `b2y1SzOZh1aE!` | ✅ |
| Technician | `abhiyant.singh+tec@egalvanic.com` | `IVr3-kCug1aE!` | ❌ (mobile only) |
| Facility Manager | `abhiyant.singh+fm@egalvanic.com` | `JngowqLgr1aE!` | ✅ |
| Account Manager | `abhiyant.singh+accountm@egalvanic.com` | `eOr2wZWpe1aE!` | ✅ |
| Client Portal | `abhiyant.singh+clientportal@egalvanic.com` | `RP@egalvanic123` | ✅ |
| Electrical Engineer | *(no QA account — excluded)* | — | — |

All use subdomain/company code **`acme`** at `https://acme.qa.egalvanic.ai`. Credentials are
env-overridable (`ADMIN_EMAIL`, `PM_PASSWORD`, …) in `AppConstants`.

## Totals — 823 RBAC test cases across 3 suites

| Suite | File | Cases | Browser? |
|-------|------|------:|:--------:|
| API permission contract + matrix + edit + action enforcement | `testng-rbac-permissions.xml` | **834** | no (REST) |
| UI nav gating + work-order edit | `testng-rbac-ui.xml` | **8** | Chrome |
| Full-login E2E | `testng-rbac-login.xml` | **9** | Chrome |
| **Total (defined, 7 roles)** | | **851** | |

> **Admin and Electrical Engineer are excluded by default** (`RbacFixtures.DEFAULT_EXCLUDED`) because
> neither QA account can be asserted: the `+admin` account is mis-provisioned as Project Manager
> (2026-06-17), and Electrical Engineer has **no QA account** (login → 401). Both were previously left
> in the matrix and only ever SKIPped (~120 noise-skips/run for EE alone); excluding them keeps the
> report clean. Force either with `-Drbac.roles="Electrical Engineer"` / `-Drbac.roles=Admin` once its
> account exists. So a default run actively executes **5 roles** (PM, Technician, FM, Account Manager,
> Client Portal) — and the only expected skips are the **2 documented prod-vs-QA drift cells** (PM
> `accounts.view`, FM `features.locations.view`).

---

## 1. API — Permission contract + matrix + enforcement (`testng-rbac-permissions.xml`, 834)

### `RoleBasedPermissionContractTest` — 8
- `testRolePermissionContract(role)` × **7 roles** — log in, `GET /auth/me`, assert the live
  permission set == matrix: identity (`roles[0].id/name`), **no privilege escalation** (no extra
  perms), completeness (no missing beyond documented drift), `has_web_access`⇔`platform.web`,
  `is_admin`⇔Admin.
- `testMatrixCsvIntegrity()` × **1** — CSV parses to the pinned snapshot (7 roles / 555 grants /
  113 perms / correct role_ids; derived `sum == totalGrants` catches dup rows).

### `RolePermissionMatrixCellTest` — 791
- `permissionCell(role, permission, expectedGranted)` × **791** — every role×permission cell:
  granted ⇒ must be present in `/auth/me`; denied ⇒ must be absent. (555 granted + 236 denied.)
  Per-cell ITest names render as `Role / perm [granted|denied]`. Full list: `RBAC_TEST_CASES.csv`.

### `WorkOrderEditEnforcementApiTest` — 7
- `workOrderEditEnforcement(role)` × **7 roles** — no-op `PUT /api/job/{id}`; allowed ⟺ role has
  `jobs.manage` (else backend returns explicit `permission_denied`). PM/Admin/Tech/FM/AM allowed,
  Client Portal denied.

### `RoleActionEnforcementApiTest` — 28 (7 roles × 4 actions)
- `actionEnforcement(role, action)` for **Edit Asset (node)** `PUT /node/update/{id}` (`nodes.manage`),
  **Edit Task** `PUT /task/update/{id}` + **Create Task** `POST /task/create` (`tasks.manage`),
  **Edit Issue** `PUT /issue/update/{id}` (`issues.manage`). Allowed ⟺ the role has the entity's
  `*.manage` permission (or is admin), else explicit `permission_denied`. PM/Technician/FM/Account
  Manager allowed; **Client Portal (read-only) denied** on all four. Non-destructive: the permission
  gate fires before entity lookup, so edits use a zero-UUID (allowed → async mutation no-ops); create
  uses a labelled minimal payload that doesn't persist.

### `RoleCrudContractApiTest` — 2 entities × roles (real CRUD lifecycle)
- `crudContract(role, entity)` for **Task** (`tasks.manage`) and **Asset/node** (`nodes.manage`).
  Goes beyond the gate: a role that HAS the permission performs a **real** `POST …/create` →
  `PUT …/update/{id}` → `DELETE …/delete/{id}` and we assert the API's **actual response contract**
  at each step; a role that lacks it is asserted **denied** (so nothing persists).
- **Verified live contract (acme.qa, 2026-06-18):** the platform is event-sourced/async (CQRS) —
  every mutation is acked with **HTTP 200** + `{"_mutation":{"status":"received"},"id":…,"name":…}`.
  It does **NOT** use `201 Created` / `202 Accepted`. The test asserts the real 200-async envelope
  (+ a real UUID id on create, + the new name echoed on edit), logs the 200-vs-201/202 deviation as a
  NOTE on every create, and **deletes the record it created** (cleanup, so runs don't accumulate data).
  Flip `EXPECT_REST_CREATED=true` to make it demand 201/202 instead (it then fails until the API changes).

---

## 2. UI — nav gating + work-order edit (`testng-rbac-ui.xml`, 8)

### `RolePermissionUiGatingTest` — 7
- `roleNavGating(role)` × **7 roles** — real browser; each high-signal nav module is visible **iff**
  the role's live permissions allow it (positive + negative, e.g. Client Portal must NOT see
  Settings/Accounts/Goals). Technician → Web Access Restricted. Oracle = live `/auth/me`.

### `WorkOrderEditUiTest` — 1
- `projectManagerCanReachWorkOrderEdit()` × **1** — PM opens a work order (`/jobs/{id}`) in the live
  UI (edit authorization itself is proven by the API enforcement test).

---

## 3. Full-Login E2E (`testng-rbac-login.xml`, 9) — real browser

### `RoleLoginE2ETest`
- `testLoginPageIntegrity()` — login page renders email + **masked** password + Sign In + Forgot password.
- `testInvalidCredentialsRejected()` — wrong password → no app access, stays on `/login`.
- `testEmptyCredentialsCannotLogin()` — empty fields → cannot authenticate.
- `roleLoginJourney(role)` × **5 web-access roles** (Admin, PM, FM, Account Manager, Client Portal) —
  full journey: login → reaches `/dashboard` (nav) → **identity** (user-menu email matches) →
  logout returns to `/login`.
- `technicianCannotAccessWeb()` × **1** — Technician authenticates but is blocked at
  **"Web Access Restricted"** (no `platform.web`); no dashboard. *(A web-less role needs only this one case.)*

---

## How to run

```bash
# API (fast, no browser)
mvn -o test -DsuiteXmlFile=testng-rbac-permissions.xml

# UI nav gating + WO edit (Chrome; headless in CI via -Dheadless=true)
mvn -o test -DsuiteXmlFile=testng-rbac-ui.xml

# Full-login E2E (Chrome)
mvn -o test -DsuiteXmlFile=testng-rbac-login.xml
```

### One-click manual runner (single dropdown)

`.github/workflows/rbac.yml` (`workflow_dispatch`) exposes a **single "Which RBAC to run"
dropdown**. Each option maps to a suite (the pom always feeds surefire a `suiteXmlFile`, so
every option is a suite — a `-Dtest=` class filter would be ignored):

| Dropdown option | Suite | What runs |
|-----------------|-------|-----------|
| `all` | `testng-rbac-all.xml` | everything (API + UI + login) |
| `api-all` | `testng-rbac-permissions.xml` | all 4 API classes |
| `ui-all` | `testng-rbac-ui.xml` | nav gating + work-order edit (browser) |
| `login-e2e` | `testng-rbac-login.xml` | full login journey (browser) |
| `permission-contract` | `testng-rbac-contract.xml` | `RoleBasedPermissionContractTest` |
| `permission-matrix-cells` | `testng-rbac-cells.xml` | `RolePermissionMatrixCellTest` (791 cells) |
| `workorder-edit-enforcement` | `testng-rbac-workorder-api.xml` | `WorkOrderEditEnforcementApiTest` |
| `action-enforcement-create-edit` | `testng-rbac-actions.xml` | `RoleActionEnforcementApiTest` (gate probe) |
| `crud-contract` | `testng-rbac-crud.xml` | `RoleCrudContractApiTest` (real create→edit→delete + 200-async contract) |
| `nav-gating-ui` | `testng-rbac-nav-ui.xml` | `RolePermissionUiGatingTest` (browser) |
| `workorder-edit-ui` | `testng-rbac-workorder-ui.xml` | `WorkOrderEditUiTest` (browser) |
| `role-project-manager` … `role-admin` | `testng-rbac-all.xml` + `-Drbac.roles=<Role>` | everything, one role only |

CI workflows: `.github/workflows/rbac-tests.yml` (API), `rbac-ui-tests.yml` (UI),
`rbac-login-tests.yml` (login). Gates are authoritative on Maven's own outcome.

## Documented exceptions (not product bugs)
- **Prod-vs-QA drift** (granted in prod CSV, absent on QA): Admin `features.equipment_insights.view`,
  PM `accounts.view`, FM `features.locations.view` — those granted-cells SKIP.
- **QA role_id override**: Account Manager's QA UUID differs from prod (same name + permissions).
- **Electrical Engineer**: no QA account → excluded / SKIPPED.
- **Backend list-endpoint enforcement** (e.g. `GET /accounts/` returns 200 for Client Portal without
  `accounts.view`) is unreliable (SPA catch-all) → not asserted. The **edit** endpoint enforces
  cleanly and IS asserted.
