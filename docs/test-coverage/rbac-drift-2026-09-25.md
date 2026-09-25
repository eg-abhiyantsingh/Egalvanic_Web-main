# RBAC permission drift: recorded matrix vs live `/auth/me` (QA, 25 Sep 2026)

**Environment:** QA, `https://acme.qa.egalvanic.ai`, Web v2.2 release day. Frontend bundle `index-S8OG5dN0.js`.
**Captured:** 25 Sep 2026, 18:09 IST.
**What fails:** the `rbac-api` group (Suite 2, `suite-rbac-api.xml`) has 35 failures out of 606 checks: 30
per-cell checks in `RolePermissionMatrixCellTest` and 5 role-level checks in `RoleBasedPermissionContractTest`.
CI runs 36121537799 and 36131500406 match this capture exactly.

**Nothing in the matrix was edited.** The matrix is the recorded contract. For each row below, the product
owner decides which side is right: the matrix (then the live grant is a bug) or the live grant (then the
matrix needs a new export).

## 1. How this was measured

- **Live side.** Each role logged in through the API by the same code the tests use:
  `RbacFixtures.fetchLiveAuth()`, which sends `POST /api/auth/login {email, password, subdomain:"acme"}`
  and then `GET /api/auth/me` with the bearer token. It uses the QA seat credentials already in `AppConstants`.
  No password was typed into a browser. The frontend reads `/api/auth/v2/me`, so I read that too:
  for every single-role seat it returns exactly the same permission set as `/auth/me`.
- **Matrix side.** `testcase/prod_permissions-by-role_202606151113.csv`: 7 roles, 555 grants, 113 distinct
  permissions. It was **exported from production** on 15 Jun 2026 at 11:13.
- **Earlier live readings used for dating.**
  (a) `docs/bug-reports/evidence/2026-08-26-rbac3-matrix-drift/live-permissions-2026-08-26.json`, taken from
  real browser sessions on 26 Aug.
  (b) Every CI `failed-list-s2-rbac-api` artifact from 3 Jul to 25 Sep (17 runs). The cell names in those
  lists are shifted by one (see section 7), so I rebuilt the real cells from the data-provider order. The rebuilt
  list for 25 Sep matches today's 30 failure messages exactly, which confirms the method works.

## 2. Matrix history: when each row changed, and who changed it

`git blame` puts **all 556 lines** (the header plus 555 rows) in a single commit. That commit is the only
one that has ever touched the file:

| Commit | Date | Author | Message |
|---|---|---|---|
| `bc559d9` | 2026-06-15 16:54 IST | abhiyantsingh007 (Abhiyant Singh) | test(rbac): cover full prod permission matrix via /auth/me contract tests |

So no row has its own history. Every row is the 15 Jun prod export, and the file has never changed since.
Everything in the table below changed on the **live** side.

## 3. Is each seat on the role it should be on?

| Role under test | Seat | Live `roles[]` | Live role_id vs expected | is_admin / web | Live perms | Verdict |
|---|---|---|---|---|---|---|
| Project Manager | `+project@` | [Project Manager] | `242dbe6a…` = expected | false / true | 96 | correct seat |
| Technician | `+tec@` | [Technician] | `e84a0fbb…` = expected | false / **false** | 95 | correct seat (mobile-only, as the matrix says) |
| Facility Manager | `+fm@` | [Facility Manager] | `54021b71…` = expected | false / true | 76 | correct seat |
| Account Manager | `+accountm@` | [Account Manager] | `392a2233…` = documented QA override (prod id `92f38105…`) | false / true | 78 | correct seat |
| Client Portal | `+clientportal@` | [Client Portal] | `2a85145f…` = expected | false / true | 35 | correct seat |
| Admin *(excluded by default)* | `+admin@` | [Super Admin, Admin, Portal Sales, Electrical Engineer, Project Manager, Account Manager] | roles[0] `b60006dd…` = the CSV "Admin" id, now **named "Super Admin"** (V1.36 rename) | true / true, **is_eg_admin=true** | 133 | cannot be asserted: six roles, including EG Admin (`e9ad3158…`, now named "Admin"), are merged into one set. Sending `x-active-role-id` no longer narrows the set, because permissions are now the union of all held roles (ZP-4033 / ZP-3973). |
| Electrical Engineer *(excluded by default)* | `+electric@` | [Electrical Engineer] | **`baefd87a…` ≠ CSV `fd6b624e…`** | false / true | 82 | **This seat now logs in** (200). RbacFixtures still says "no QA account exists (401)". The id difference is the same kind as the Account Manager one. |

**No seat is on the wrong role.** The five asserted seats all return the expected role name and id, with one
role each and no EG-Admin overlay. So a reassigned seat does not explain any block of failures: every
difference below is a change to what the role itself grants.

## 4. Summary counts

The five asserted roles differ from the matrix in **54 role × permission cells**:

| Role | Matrix has, live does not (MISSING) | Live has, matrix does not (EXTRA) | Total | Failing cell tests | Role contract test |
|---|---|---|---|---|---|
| Project Manager | 7 | 10 (8 are keys that are not in the CSV) | 17 | 9 | FAIL |
| Technician | 0 | 5 (all 5 not in the CSV) | 5 | 0 | FAIL |
| Facility Manager | 6 (1 is known drift, skipped) | 5 (3 not in the CSV) | 11 | 7 | FAIL |
| Account Manager | 9 | 10 (7 not in the CSV) | 19 | 12 | FAIL |
| Client Portal | 2 | 0 | 2 | 2 | FAIL |
| **Total** | **24** | **30** | **54** | **30** | **5** |

The per-cell test only covers the 113 keys in the CSV. That is why the 23 cells whose key is new since 15 Jun
show up only in the role-level contract test.

**By likely cause:**

| Group | What it means | Cells | Who should decide |
|---|---|---|---|
| **A: new permission key** | The key did not exist when the matrix was exported. It arrived with a later feature. | 23 | Mostly matrix-stale. One suspicious row: row 8, PM `accounts.view_detail_page`. |
| **B: capability removed by a product decision** | A Done ticket removed the capability from the role (Settings lockdown, forms/report builder limited to EG Admin, no folder management for FM). | 10 | Likely matrix-stale. Confirm, then re-export. |
| **C: dashboard or landing swap backed by a ticket** | The role's dashboard moved, and a later ticket explains the move (ZP-4036, ZP-4123). | 4 | Likely matrix-stale. Confirm. |
| **D: single-permission drift, no ticket found** | One key added or removed. Several of these go against a Done ticket. | 17 | **The owner has to answer these.** Some look like regressions (section 6). |

**How the failing cells changed over time** (rebuilt from CI):

| Period | Failing cells | Why |
|---|---|---|
| 3 Jul | 1 (PM `features.audit_log.view`) | the only drift then |
| 9 Jul to 22 Aug | 0 | all 5 seats also carried EG Admin. That full set made every "granted" cell pass, and the "denied" cells were skipped. **This hid the drift for 6 weeks.** |
| 25 Aug | 28 (27, plus Technician `platform.web`) | overlay removed. Drift visible again. RBAC-1 (Technician web access) filed. |
| 27 Aug | 27 | Technician `platform.web` fixed |
| 25 Sep | 30 | New since 27 Aug: PM lost `features.slds.view`; AM lost `features.site_visits.view`, `forms.manage` and `reports.manage`; PM gained `features.planning.view` and `features.site_overview.view`. Restored since 27 Aug: PM `features.arc_flash.view` and `features.condition_assessment.view`, CP `features.condition_assessment.view`. |

## 5. ZP-4410 (Activity Logs) and the Audit Log permission

- ZP-4410 ("remove the Activity Logs permission from all roles", merged to QA through eg-pz-frontend PR #1543,
  now READY TO RELEASE) concerns **`features.activity_logs.view`** (the `/admin/activity-logs` menu entry and route)
  and the new key `activity_logs.view`. **Neither key is in the matrix**, so ZP-4410 **causes none of the 35
  failures**.
- Live today, **no seat except `+admin@` holds either key.** That covers PM, Technician, FM, AM, CP and EE.
  The `+admin@` seat gets them through its six merged roles, including EG Admin. Whether an admin role should
  keep them is the owner's call (the ticket says "all roles").
- Do not confuse this with **`features.audit_log.view`** (the **Audit Log** at `/admin/audit-log`, a separate
  menu entry, route and key). That key is missing for Project Manager (and Electrical Engineer) **since at
  least 3 Jul**, so the loss predates ZP-4410 by 12 weeks. This is row 1 of the table.

## 6. Items for the owner to decide

1. **Re-baseline or not.** Groups A, B and C (37 cells) match shipped tickets, which suggests the matrix is stale.
   If you agree, the fix is a **new matrix export** (from prod or QA; you choose which environment is the
   contract). Tests should not be edited to pass.
2. **Group D rows that change what a user can do in the UI today.** In CI 36131500406 the UI permission matrix
   records the menu hiding each of these:
   - **Project Manager lost SLDs** (`features.slds.view`, lost between 27 Aug and 25 Sep) and **Panel Schedules**.
   - **Account Manager lost Work Orders.** `features.site_visits.view` gates both the `/sessions` menu entry and
     the route; it was lost between 27 Aug and 25 Sep. AM also lost SLDs (`features.slds.view`) and
     **attachment viewing** (`attachments.view` and `.manage`, though it can still upload). ZP-2259 says AM can
     view files.
   - **Facility Manager has no Locations menu.** ZP-3767 (Done 14 Aug) asked to *add* it, but
     `features.locations.view` is absent. The test hides this under `KNOWN_QA_DRIFT`.
     FM also lost `reports.view` (ZP-2259 says FM can view reports) and Equipment Designations.
   - **Client Portal lost Tasks and Arc Flash** (`features.tasks.view`, `features.arc_flash.view`).
3. **Possible over-grants.** The contract flags each of these as "privilege escalation":
   - **PM `accounts.view_detail_page`.** ZP-3628 made PM the *one* role without it. QA confirmed that on 18 Aug,
     it was still absent on 26 Aug, and today PM has it. Possible regression.
   - **AM `ir_photos.upload`.** ZP-2259's table says AM must not upload IR photos.
4. **Not flagged by any test, but inconsistent.** Technician still holds `forms.manage` and `reports.manage`,
   although ZP-3158 limits building forms and reports to EG Admin. The matrix grants them to Technician too,
   so no test fails.
5. **Fixture housekeeping** (the owner decides; nothing was changed):
   - `KNOWN_QA_DRIFT` entry PM `accounts.view` is stale: PM has held `accounts.view` since at least 26 Aug.
   - The Electrical Engineer seat now logs in. Re-enabling it needs a `QA_ROLE_ID_OVERRIDE` for `baefd87a…`
     and a decision on its 13 differences (7 missing, 6 extra; listed in section 8).
   - The UI matrix skips all 48 Admin cells with "mis-provisioned: logs in as 'Super Admin'". The real cause is
     the V1.36 name swap plus a six-role seat. A clean single-role admin seat would be needed to cover Admin.

## 7. Test-side defect found and fixed on this branch (does not change any result)

`RolePermissionMatrixCellTest` set its ITest name inside the test body. TestNG and surefire read the name
before the body runs, so **every cell was reported under the previous cell's name**. Example: the failure
"matrix grants `features.audit_log.view` to Project Manager" appeared as the test "Project Manager — should
have 'features.attachments.view'". So the dated `failed-suites/failed-tests-2026-09-25.xml`, the failed-list
artifacts and every JUnit report listed **30 wrong cells**. The fix sets the name in a `@BeforeMethod` from the
data-provider row. Local check with the same suite: before the fix, 0/30 failing cells were named correctly in
surefire `TEST-TestSuite.xml`; after it, 30/30. `collect-failed-tests.py` now emits the 30 real cells. Pass and
fail results are identical before and after (571 run, 35 fail, 1 skip).

## 8. Full diff table (the five asserted roles)

Columns: **Matrix** = the CSV grant. **Live 25 Sep** = `/auth/me` today. **Live 26 Aug** = the RBAC-3 browser
snapshot. **Which test fails**: "cell + contract" = one of the 30 failing cells; "contract only" = the key is
not in the CSV, so only the role-level test sees it.

| # | Role | Permission | Matrix (CSV) | Live /auth/me 25 Sep | Live 26 Aug | Which test fails | Cause group | Evidence / note |
|---|---|---|---|---|---|---|---|---|
| 1 | Project Manager | `features.audit_log.view` | granted | **absent** | absent | cell + contract | D | Absent since at least 3 Jul (the only failing cell in the 3 Jul CI run). NOT ZP-4410: that ticket is Activity Logs (features.activity_logs.view, /admin/activity-logs), a different key and route from Audit Log (/admin/audit-log). Electrical Engineer lost it too. |
| 2 | Project Manager | `features.panel_schedules.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. PM no longer sees Panel Schedules (CI: "nav hides /panel-schedules"). ZP-1800 expects PM to see it. |
| 3 | Project Manager | `features.settings.pm.view` | granted | **absent** | absent | cell + contract | B | ZP-3442 (Done 2026-08-07): Settings only for Admin + EG Admin. ZP-3476 took PM Plans out of the menu. |
| 4 | Project Manager | `features.settings.view` | granted | **absent** | absent | cell + contract | B | ZP-3442 (Done 2026-08-07): Settings only for Admin + EG Admin. ZP-3476 took PM Plans out of the menu. |
| 5 | Project Manager | `features.slds.view` | granted | **absent** | present | cell + contract | D | LOST between 27 Aug and 25 Sep (live on 26-27 Aug). The UI today hides SLDs from PM (CI 36131500406: "nav hides /slds"). |
| 6 | Project Manager | `forms.manage` | granted | **absent** | absent | cell + contract | B | ZP-3158 (Done 2026-08-07): building forms/reports is EG Admin only. Technician still holds forms.manage (owner item 5). |
| 7 | Project Manager | `reports.manage` | granted | **absent** | absent | cell + contract | B | ZP-3158 (Done 2026-08-07): Report Builder is EG Admin only. Technician still holds reports.manage (owner item 5). |
| 8 | Project Manager | `accounts.view_detail_page` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key from ZP-3628 (migration acctperm_a1, 2026-08-05). ZP-3628 says Project Manager is the ONE role that must NOT hold it (QA confirmed that on 18 Aug; absent on 26 Aug). PM has it again today: possible regression. |
| 9 | Project Manager | `features.customers.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Customers rebuild, ZP-3973). The bundle never checks it: Customers = accounts.view || features.accounts.view. |
| 10 | Project Manager | `features.planned_work.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Planned Work). The bundle never checks it: the Planned Work menu uses features.planning.view. |
| 11 | Project Manager | `features.planning.view` | not granted | **PRESENT** | absent | cell + contract | C | Added between 27 Aug and 25 Sep. Gates the Planned Work menu (bundle); PM already holds features.planned_work.view. |
| 12 | Project Manager | `features.settings.test_equipment.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Test Equipment settings page). |
| 13 | Project Manager | `features.site_overview.view` | not granted | **PRESENT** | absent | cell + contract | C | Added between 27 Aug and 9 Sep. The ZP-4123 QA comment (9 Sep) relies on PM holding it (Maintenance Program). |
| 14 | Project Manager | `features.test_equipment.view` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key (Test Equipment). Appeared after 26 Aug. |
| 15 | Project Manager | `maintenance_portal.manage` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key (Maintenance Portal, ZP-4138/ZP-4148). Appeared after 26 Aug. |
| 16 | Project Manager | `site_walks.manage` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Site Walks feature). |
| 17 | Project Manager | `site_walks.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Site Walks feature). |
| 18 | Technician | `accounts.view_detail_page` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key from ZP-3628 (migration acctperm_a1, 2026-08-05): opening an account DETAIL page. Derived for every role that holds accounts.view, so expected here. |
| 19 | Technician | `features.planned_work.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Planned Work). The bundle never checks it: the Planned Work menu uses features.planning.view. |
| 20 | Technician | `issue_suggestions.manage` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key (Issue Suggestions, v2.2). Appeared after 26 Aug. |
| 21 | Technician | `routes.manage` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key (routes). Appeared after 26 Aug. |
| 22 | Technician | `routes.view` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key (routes). Appeared after 26 Aug. |
| 23 | Facility Manager | `features.equipment_designations.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. |
| 24 | Facility Manager | `features.locations.view` | granted | **absent** | absent | none (known drift, skipped) | D | Known QA drift (RbacFixtures.KNOWN_QA_DRIFT), so the test skips it. But ZP-3767 (Done 2026-08-14) asked to ADD Locations for FM, and today FM still has no Locations menu (CI: "nav hides /locations"). |
| 25 | Facility Manager | `features.site_overview.view` | granted | **absent** | absent | cell + contract | C | Pair: the Facility Manager lost Site Overview and gained the Operations dashboard. ZP-4036 (Done 2026-08-31) sends a seat without site_overview from /dashboard to /ops-dashboard, which needs opsdb.view. |
| 26 | Facility Manager | `folders.manage` | granted | **absent** | absent | cell + contract | B | ZP-2259 (Done): Facility Manager gets no folder management. |
| 27 | Facility Manager | `forms.manage` | granted | **absent** | absent | cell + contract | B | ZP-3158 (Done 2026-08-07): building forms/reports is EG Admin only. Technician still holds forms.manage (owner item 5). |
| 28 | Facility Manager | `reports.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. Goes against ZP-2259, whose table says Facility Manager can view reports. |
| 29 | Facility Manager | `features.customers.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Customers rebuild, ZP-3973). The bundle never checks it: Customers = accounts.view || features.accounts.view. |
| 30 | Facility Manager | `features.opsdb.view` | not granted | **PRESENT** | present | cell + contract | C | Pair: the Facility Manager lost Site Overview and gained the Operations dashboard. ZP-4036 (Done 2026-08-31) sends a seat without site_overview from /dashboard to /ops-dashboard, which needs opsdb.view. |
| 31 | Facility Manager | `features.panel_schedules.view` | not granted | **PRESENT** | present | cell + contract | D | Gained between 3 Jul and 25 Aug. FM now sees Panel Schedules. |
| 32 | Facility Manager | `features.planned_work.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Planned Work). The bundle never checks it: the Planned Work menu uses features.planning.view. |
| 33 | Facility Manager | `maintenance_portal.manage` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key (Maintenance Portal, ZP-4138/ZP-4148). Appeared after 26 Aug. |
| 34 | Account Manager | `attachments.manage` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. AM keeps attachments.upload/update but cannot view. ZP-2259 says AM can upload AND view files. |
| 35 | Account Manager | `attachments.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. AM keeps attachments.upload/update but cannot view. ZP-2259 says AM can upload AND view files. |
| 36 | Account Manager | `features.jobs.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. |
| 37 | Account Manager | `features.settings.pm.view` | granted | **absent** | absent | cell + contract | B | ZP-3442 (Done 2026-08-07): Settings only for Admin + EG Admin. ZP-3476 took PM Plans out of the menu. |
| 38 | Account Manager | `features.settings.view` | granted | **absent** | absent | cell + contract | B | ZP-3442 (Done 2026-08-07): Settings only for Admin + EG Admin. ZP-3476 took PM Plans out of the menu. |
| 39 | Account Manager | `features.site_visits.view` | granted | **absent** | present | cell + contract | D | LOST between 27 Aug and 25 Sep. This key gates the Work Orders menu and route (/sessions) and the Planned Work route, so AM has lost Work Orders in the UI (CI: "nav hides /sessions"). |
| 40 | Account Manager | `features.slds.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. AM no longer sees SLDs (CI: "nav hides /slds"). |
| 41 | Account Manager | `forms.manage` | granted | **absent** | present | cell + contract | B | Same ZP-3158 decision, but AM still held both on 26-27 Aug: removed between 27 Aug and 25 Sep. |
| 42 | Account Manager | `reports.manage` | granted | **absent** | present | cell + contract | B | Same ZP-3158 decision, but AM still held both on 26-27 Aug: removed between 27 Aug and 25 Sep. |
| 43 | Account Manager | `View Sales Agent` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key, oddly named (not dotted like every other key). |
| 44 | Account Manager | `accounts.view_detail_page` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key from ZP-3628 (migration acctperm_a1, 2026-08-05): opening an account DETAIL page. Derived for every role that holds accounts.view, so expected here. |
| 45 | Account Manager | `features.arc_flash.view` | not granted | **PRESENT** | present | cell + contract | D | Gained between 3 Jul and 3 Aug (the AM menu recorded that day already showed Arc Flash + Issues). |
| 46 | Account Manager | `features.customers.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Customers rebuild, ZP-3973). The bundle never checks it: Customers = accounts.view || features.accounts.view. |
| 47 | Account Manager | `features.issues.view` | not granted | **PRESENT** | present | cell + contract | D | Gained between 3 Jul and 3 Aug (the AM menu recorded that day already showed Arc Flash + Issues). |
| 48 | Account Manager | `features.planned_work.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Planned Work). The bundle never checks it: the Planned Work menu uses features.planning.view. |
| 49 | Account Manager | `ir_photos.upload` | not granted | **PRESENT** | present | cell + contract | D | Gained between 3 Jul and 25 Aug. Goes against ZP-2259, whose table says Account Manager must NOT upload IR photos. Possible over-grant. |
| 50 | Account Manager | `maintenance_portal.manage` | not granted | **PRESENT** | absent | contract only (key not in CSV) | A | New key (Maintenance Portal, ZP-4138/ZP-4148). Appeared after 26 Aug. |
| 51 | Account Manager | `site_walks.manage` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Site Walks feature). |
| 52 | Account Manager | `site_walks.view` | not granted | **PRESENT** | present | contract only (key not in CSV) | A | New key (Site Walks feature). |
| 53 | Client Portal | `features.arc_flash.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. CP no longer sees Tasks (CI: "nav hides /tasks") or Arc Flash. |
| 54 | Client Portal | `features.tasks.view` | granted | **absent** | absent | cell + contract | D | Lost between 3 Jul and 25 Aug. CP no longer sees Tasks (CI: "nav hides /tasks") or Arc Flash. |

### 8b. Roles excluded by default (for information only; not part of the 35 failures)

**Electrical Engineer.** The `+electric@` seat logs in (200) and holds one role, id `baefd87a…`; the CSV id is
`fd6b624e…`. It is excluded through `RbacFixtures.DEFAULT_EXCLUDED`. Compared with the matrix:

| Direction | Permissions |
|---|---|
| Matrix has, live does not (7) | `features.audit_log.view`, `features.settings.classes.view`, `features.settings.view`, `forms.manage`, `reports.generate`, `reports.manage`, `reports.view` |
| Live has, matrix does not (6) | `accounts.view`, `accounts.view_detail_page`*, `features.condition_assessment.view`, `features.customers.view`*, `ir_photos.upload`, `maintenance_portal.manage`* |

\* = key not in the CSV.

**Admin.** The `+admin@` seat returns 133 permissions: the union of six roles, including EG Admin (`is_eg_admin=true`).
All 98 of the matrix's Admin grants are present. The 35 extra permissions cannot be attributed to the Admin role
itself from this seat.

## 9. RBAC Front-End (UI) group: verdicts

| Failure | Run | Verdict | Evidence |
|---|---|---|---|
| RoleLoginE2ETest.roleLoginJourney × 4 (PM, FM, AM, CP): "Login form must be present" | 36121537799 (morning) | **Stale test.** Fixed in `0450087`. | v2.2 sign-in asks for the email first; Sign In appears only after "Use my password". All 4 PASS in 36131500406. |
| RoleLoginE2ETest.testEmptyCredentialsCannotLogin: no password input | morning | **Stale test.** Fixed in `0450087`. | Same two-step page. PASS in 36131500406. |
| RoleLoginE2ETest.testLoginPageIntegrity: password field not visible | morning | **Stale test.** Fixed in `0450087`. | Same. PASS in 36131500406. |
| RolePermissionUiGatingTest.roleNavGating (PM): Customers visible without `features.accounts.view` | morning | **Stale test.** Fixed in `0450087`. | The bundle shows Customers when `Y("accounts.view") \|\| Y("features.accounts.view")`, and PM holds `accounts.view`. PASS in 36131500406. |
| RbacUiPermissionMatrixTest: **Client Portal · Quotes · Create** ("New Quote" is enabled although CP lacks `opportunities.manage`) | 36131500406 (new) | **Real front-end gating gap, low severity.** It is not stale: the morning PASS was a false pass. | (1) The `/opportunities` route has no guard in the bundle (`path:"/opportunities",element:FGe`). Only the menu entry checks `features.opportunities.view`, which CP lacks, so CP reaches the page only by typing the URL. (2) The Quotes page always renders `createButtonText:"New Quote"`; no permission check is made. (3) The backend does refuse: CP gets **422 permission_denied** ("Required: opportunities.manage", "Required: quotes.manage") on an empty `POST /api/opportunity/` and `POST /api/quote/create`, while AM gets a 400 validation error on the same calls. So there is no escalation. (4) The morning run "passed" this cell on a spinner-only screenshot, because the probe ran before the page loaded. Reproduced headed locally: 48 run, 1 fail (this cell). |

**Test fix for the UI matrix (this branch).** `RbacUiPermissionMatrixTest` probed Create/Edit/Delete about 2 s
after `driver.get()`. Under load that lands on the bare boot spinner, and locally on the v2.2 "Set up
two-factor authentication" chooser, which covers the whole app after a reload until "Set up later" is pressed
once in the tab. In both cases "no control found" **passed** a security cell without looking at the page. CI
examples: "PM · Work Orders · Create" and "CP · Quotes · Create" in the morning run were both screenshots of a
spinner. Local examples: "CP · Assets / Work Orders / Issues / Locations · Create" were all screenshots of the two-factor chooser. The probe now waits
for the app shell, presses "Set up later" if the chooser opens, and waits for the module's spinners to clear.
If the chooser still covers the page, the cell is SKIPPED as "not verified" rather than passed.

## 10. Validation runs (local, against QA, 25 Sep)

| Run | Suite | Result |
|---|---|---|
| API, cell test before the name fix | `RoleBasedPermissionContractTest` + `RolePermissionMatrixCellTest` | 571 run, 35 fail, 1 skip. Surefire names 0/30 failing cells correctly. |
| API, after the name fix | same | 571 run, 35 fail, 1 skip (the same 35). Names 30/30 correctly. `collect-failed-tests.py` lists the real cells. |
| UI, Client Portal only, before the probe fix (headed) | `RbacUiPermissionMatrixTest` | 48 run, 1 fail. Four Create cells (Assets, Work Orders, Issues, Locations) passed on screenshots of the two-factor chooser. |
| UI, Client Portal only, after the probe fix (headed) | same | 48 run, 1 fail (CP · Quotes · Create). Every probe screenshot shows the rendered module. |
| UI, whole group, after the fix (headed, shared browser slot) | `suite-rbac-frontend.xml` | **303 run: 254 pass, 1 fail (CP · Quotes · Create), 48 skip (Admin seat)**. RoleLoginE2E 8/8, UI gating 5/5, WO edit 1/1. Identical to CI run 36131500406. 18 min. |

Scripts and raw captures (not committed) are in the session scratchpad under `agentD/`:
`RbacLiveDump.java` (same login path as the tests), `live-authme.json`, `live-v2.json`, `hist_recon.py`
(CI history rebuild), and `QuoteCreateProbe.java` (the 422 check).
