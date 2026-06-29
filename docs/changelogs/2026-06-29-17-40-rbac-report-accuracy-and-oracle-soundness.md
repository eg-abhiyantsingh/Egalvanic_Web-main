# RBAC report now shows all 336 matrix cases (was collapsing to "8 Total Tests") + oracle soundness fix

- **Date:** 2026-06-29 17:40
- **Prompt:** "8 Total Tests are you crazy? … I want at least 300 test cases total for RBAC — e.g. PM
  create/edit/delete asset, same for other roles." (plus: "why can't I see the client/detailed report?")
- **Type:** Bug fix (report accuracy) + test-oracle correctness.

---

## 1. Why the client report said "8 Total Tests" for a 350-test suite

The 336 RBAC matrix cases all run as **one** data-driven `@Test` method (`uiPermissionCell`, invoked 336×
over role × module × action). `consolidated-report.py` deduplicated test rows by **`class#method`** — so all
336 invocations collapsed into a single row, and the client report showed ~8 "tests" total. The cases all
ran (the CI `testng-results.xml` literally records 350 tests); the **report** was hiding them.

### Fix
- **`consolidated-report.py`** now parses each `<test-method>`'s `<params>` and dedupes by
  **`class#method#params`**, so every data-driven invocation counts and displays as its own row. True
  artifact duplicates (same class+method+params downloaded twice) still collapse. Multi-param cells render
  as **"Project Manager · Assets · Create"** (the parameters become the test-case name); single-param cells
  show `name [param]`.
- **`RbacUiPermissionMatrixTest.Module.toString()`** now returns the module label, so testng-results store
  the readable label (`Assets`) instead of `Module@hash`. This makes the report rows readable **and** gives
  the re-run override a stable key across runs (see the two-report changelog).
- Result, verified against the real CI results file and a fresh local run: **350 Total Tests**, one row per
  role·module·action.

> Note on "I can't see the report": the client/detailed reports are produced by the **summary** job and
> attached as **run artifacts** (`consolidated-client-report-suite2`, `consolidated-detailed-report-suite2`)
> at the bottom of the *run* page — not on the individual test-job page.

## 2. Oracle soundness fix (independent per-role audit vs live `/auth/me`)

An independent audit of the matrix's *expected* outcomes against each role's live `/auth/me` found the
manage-side security invariant sound, but the **view-side keys were mis-modeled**: this tenant mixes a
`features.<m>.view` namespace with bare `<entity>.view` keys, and which one a role holds varies
(Account Manager has bare `issues.view`/`tasks.view`/`edges.view`/`locations.view`; Client Portal has
`jobs.view` not `features.jobs.view`; Facility Manager has `locations.view` not `features.locations.view`).

### Fix
- View/manage perms are now **pipe-separated alternatives** — a role "can" if it holds **any** listed key
  (e.g. Connections = `features.connections.view|edges.view`, Work Orders manage = `jobs.manage|workorders.manage`).
- The **View** assertion now hard-fails **only** on the security direction (a route visible to a role that
  lacks the permission = leak). "Permitted-but-hidden" is logged, not failed, because this app's web nav is
  role-curated and feature-flag-gated, so a permitted module can legitimately be absent from the sidebar.
- Accounts → `accounts.view`; Goals → `features.goals.manage` (no `goals.view` exists); Panel Schedules
  marked flag-coupled.

## Validation
_Headed, local, no headless._ Full 7-role suite after the fixes: **0 failures**, 302 passed, 48 skipped
(unprovisioned account). Client report renders **350** distinct rows. The Client Portal · Forms · Create
finding remains tracked as a known-finding skip (see the matrix changelog).
