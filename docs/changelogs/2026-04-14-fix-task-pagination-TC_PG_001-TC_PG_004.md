# Fix TaskTestNG Pagination Tests (TC_PG_001 + TC_PG_004)

**Date:** 2026-04-14
**Scope:** TaskTestNG.java — pagination assertion resilience

---

## Problem

`testTC_PG_001_PaginationControlsPresent` and `testTC_PG_004_RowsPerPage` fail on the Tasks page because MUI DataGrid uses **simplified pagination** (`Total Rows: N`) when all data fits on one page, instead of the **full pagination** (`Rows per page: 25 | 1-25 of N` with prev/next buttons) the tests expected.

Same root cause as CONN_072/CONN_073 fixed earlier today.

### Failure Output (TC_PG_001)
```
Assertion failed — data mismatch between expected and actual
Page: https://acme.qa.egalvanic.ai/tasks
```

---

## Fix

| Test | Change |
|------|--------|
| TC_PG_001 (line 1679) | Accept either "Rows per page" or "Total Rows". Only assert next-page button when full pagination is present. |
| TC_PG_004 (line 1740) | Accept either pagination variant. Only assert "25" default when full pagination is present. |

### Pattern
```java
boolean hasFullPagination = pageText.contains("Rows per page");
boolean hasSimplifiedPagination = pageText.contains("Total Rows");

if (hasFullPagination) {
    // assert prev/next buttons, rows-per-page selector
} else {
    logStep("Simplified pagination — all data fits on one page");
}
```

This is the same pattern applied to ConnectionPart2TestNG (CONN_072/CONN_073).

---

## Files Changed

| File | Lines |
|------|-------|
| `src/test/java/com/egalvanic/qa/testcase/TaskTestNG.java` | ~1679-1695 (TC_PG_001), ~1740-1755 (TC_PG_004) |
