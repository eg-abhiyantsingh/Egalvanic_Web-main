# Full Local Suite Run + CI Exclusion (Connection + SLD)

**Date:** 2026-05-28
**Time:** 14:54 IST
**Trigger:** "run all the test local and fix one by one" + "sld test case hide in ci cd and connection test case hide too in ci cd"

---

## Changes

1. Ran 8 active test suites locally non-headless (~816 active TCs).
2. Iteratively fixed test-infra issues over multiple rounds per suite.
3. Excluded Connection + SLD modules from all CI workflows.
4. Result: ~91% effective pass rate across active suites; only documented real product bugs remain.

---

## Depth explanation (for manager review)

### The recurring "invisible duplicate" pattern

The May 2026 web release uses MUI DataGrid with column virtualization. Some inputs/buttons render TWICE in the DOM: a visible interactive copy + an invisible measurement scaffold. Selenium's `findElement()` returns the **first** match in document order, often the scaffold. Three different test classes hit this bug (CriticalPath, AssetPart1, WorkOrder). The fix is consistent: `findElements().stream().filter(isDisplayed).findFirst()`.

This is now a documented pattern. Any future test that interacts with a search box, pagination, or KPI button should use the visible-filter idiom from day one.

### Why CI exclusion matters

The May 2026 release **hid the Connection tab** entirely (no UI access). It also **deprecated SLD** (no longer maintained). 145 tests for these features kept failing in CI and polluting the failure reports. Excluding them at the workflow level surfaces only the failures that actually matter — making the daily AI-analysis bot (and human reviewers) focus on real product issues.

### The "be lenient with transients" principle

`waitForGrid()` now does a 1-time refresh if it sees "Error loading assets" in the body. This hides intermittent backend flakes but still surfaces persistent failures — if the page errors on both load attempts, the test still fails. This is the right place to put retry logic: at the layer that knows what "data loaded" looks like.

---

## Files

### Source changes (committed)
- `src/test/java/com/egalvanic/qa/testcase/CriticalPathTestNG.java` — full rewrite of helpers (waitForGrid 3-phase, waitForDashboardKpis 2-phase, readGridPaginationTotal via MuiTablePagination, visible-filter)
- `src/test/java/com/egalvanic/qa/testcase/BaseTest.java` — recoverFromErrorPage uses new 5-way email locator
- `src/test/java/com/egalvanic/qa/testcase/AssetPart1TestNG.java` — visible search filter
- `src/main/java/com/egalvanic/qa/pageobjects/AssetPage.java` — SEARCH_INPUT prefers "Search Assets..."
- `src/test/java/com/egalvanic/qa/testcase/WorkOrderTestNG.java` — findVisibleSearchInput() helper

### CI workflows (committed)
- `suite-auth-site.xml` (new)
- `.github/workflows/parallel-suite.yml`
- `.github/workflows/full-suite.yml`
- `.github/workflows/smoke-tests.yml`
- `.github/workflows/web-tests-smoke-repodeveloper.yml`
- `.github/scripts/full-suite-dashboard.sh`
- `.github/scripts/smoke-dashboard.sh`

### Real product bugs documented
- BUG-012 regression (Company info not available alert)
- BUG-02 (DevRev PLuG.js SRI hash mismatch)
- Existing v9: ZP-2025, CWO2_007, IL_004, ISS_015, ISS_046
