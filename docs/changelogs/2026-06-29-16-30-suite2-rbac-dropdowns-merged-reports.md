# Parallel Suite 2 — RBAC API + RBAC Front-End dropdowns, reports merged into consolidated client + detailed

- **Date:** 2026-06-29
- **Prompt:** "provide dropdown in parallel suite 2 to select rbac api test and … rbac front end test to run,
  and report will be generated the same way as client report and detailed report."
- **Type:** CI restructure (`parallel-suite-2.yml`, `full-suite-dashboard.sh`, `consolidated-report.py`)
  + 2 suite XMLs. No test-logic change.

---

## The problem
RBAC ran as **two standalone jobs** (`rbac-api`, `rbac-full`) that uploaded their own artifacts
(`rbac-api-reports-s2`, `rbac-full-reports-s2`) and **bypassed the consolidation** — so RBAC never
appeared in `consolidated-client-report-suite2` or `consolidated-detailed-report-suite2`. (That's why
the RBAC report couldn't be found on the run page.)

## The fix — run RBAC as normal matrix groups
RBAC now flows through the **same `test-group` pipeline** as every other module, so its `reports-s2-*`
artifacts are downloaded into `all-reports` and merged by `consolidated-report.py` (client) and
`consolidated-detailed-report.py` (detailed) — identical to Assets, Work Orders, etc.

- **`parallel-suite-2.yml`**
  - Dropdowns: **`rbac_api`** ("RBAC — API tests") and **`rbac_frontend`** ("RBAC — Front-End (UI)
    tests") — replacing the old `rbac_api` + `rbac_all` (both default off).
  - Added two CATALOG groups: `rbac-api` → `suite-rbac-api.xml` (stagger 82) and `rbac-frontend` →
    `suite-rbac-frontend.xml` (stagger 90); added `RBAC_API` / `RBAC_FRONTEND` to the env + ENABLED loop.
  - **Removed** the standalone `rbac-api` / `rbac-full` jobs (the ones that bypassed consolidation).
- **`full-suite-dashboard.sh`** — registered `rbac-api` (idx 24) and `rbac-frontend` (idx 25) in all four
  group arrays (now 26 aligned entries), `get_group_index`, and the Valid list. (Shared script, so both
  parallel suites can resolve the groups.)
- **`consolidated-report.py`** — mapped the 8 RBAC classes to **`RBAC — API`** (RoleBasedPermissionContract,
  RolePermissionMatrixCell, WorkOrderEditEnforcementApi, RoleActionEnforcementApi, RoleCrudContractApi) and
  **`RBAC — UI`** (RoleLoginE2E, RolePermissionUiGating, WorkOrderEditUi), and added both to `MODULE_ORDER`.
- New **`suite-rbac-api.xml`** (5 API classes) + **`suite-rbac-frontend.xml`** (the 3 UI classes; the
  336-case UI permission matrix is being added next).

All 5 RBAC API classes and the 3 UI classes already call `ExtentReportManager`, so they produce
`Detailed_Report_*.html` → they appear in the **detailed** report; their `testng-results.xml` (mapped via
`CLASS_TO_MODULE`) puts them in the **client** report.

## How to run
Actions → **Parallel Suite 2** → tick **"RBAC — API tests"** and/or **"RBAC — Front-End (UI) tests"**.
Their results show in the consolidated client + detailed reports alongside any other ticked modules.

## Verified
- `parallel-suite-2.yml` parses; CATALOG is valid JSON (17 groups incl. rbac-api + rbac-frontend).
- `full-suite-dashboard.sh` passes `bash -n`; all four arrays have **26 aligned entries**; both RBAC
  groups resolve group → index → suite XML to existing files.
- `consolidated-report.py` compiles; the 8 RBAC classes resolve to the RBAC modules.
- `suite-rbac-api.xml` + `suite-rbac-frontend.xml` parse.
