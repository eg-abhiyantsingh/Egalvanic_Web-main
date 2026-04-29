# Phase 1 Bug Hunter — Round 4 (TC_BH_14..17)

**Date / Time:** 2026-04-29, 15:16 IST
**Branch:** `main`
**Site:** `acme.qa.egalvanic.ai`
**Result:** 4/4 PASS live. 0 new product bugs surfaced (clean run).

## What you asked for

> "test all by yourself"

I designed and live-tested 4 more adversarial tests autonomously:
- TC_BH_14 — silent API failures beyond console (fetch/XHR monkey-patch)
- TC_BH_15 — search filter coherence across detail-back navigation
- TC_BH_16 — DataGrid row uniqueness (no duplicate IDs)
- TC_BH_17 — DOM doesn't leak across drawer open/close cycles

## The 4 tests

### TC_BH_14 — No silent 4xx/5xx API responses (network-level)

**Bug class:** silent backend failures masked by app-level try/catch. TC_BH_12 catches console.log SEVERE entries, but fetch/XHR errors caught by app code don't always log SEVERE. Backend regressions can ship undetected.

**Strategy:** monkey-patch `window.fetch` and `XMLHttpRequest.prototype.send` to record any response with status >= 400 to a `window.__bh_failed` array. Trigger fresh API activity (search, pagination), then read the array.

**Filtered:** the 2 known endpoints from TC_BH_12 (`/api/auth/v2/me`, `/api/auth/v2/refresh`) are excluded to avoid double-counting.

**Result:** 0 unfiltered 4xx/5xx. The app's API surface is clean beyond the 3 known issues.

### TC_BH_15 — Filter coherence across navigation

**Bug class:** SPA state preservation. Common failure: filter applied → click row → back → either filter is gone but grid still shows filtered count, OR filter shown but grid shows full count. Either is a state-sync bug.

**Falsifier (soft, supports two valid behaviors):** after back, EITHER
- Filter preserved AND grid count matches post-filter count, OR
- Filter cleared AND grid count matches pre-filter count

What's NOT OK: any inconsistency between displayed filter and visible row count.

**Result:** PASS — filter and grid stayed coherent. The app uses the "preserve filter" pattern consistently.

### TC_BH_16 — DataGrid row uniqueness

**Bug class:** data duplication from bad backend JOINs, optimistic-update races, or React key collisions. A row appearing twice silently violates user trust.

**Falsifier:** collect `data-id` (or `data-rowindex`) from all visible rows. `Set(ids).size === ids.length`.

**Result:** PASS — all visible row IDs unique.

### TC_BH_17 — DOM doesn't leak across drawer open/close cycles

**Bug class:** React component leaks. If a Drawer component doesn't fully unmount when closed (forgotten subscriptions, ref'd children), repeated open/close cycles accumulate dead DOM. Eventually: memory bloat, perf degradation, weird stale-state bugs.

**Strategy:** count `document.querySelectorAll('*').length` baseline → cycle (row click → kebab → Edit Asset → Cancel → back) 5 times → recount. Growth threshold: 2000 nodes (generous; tighten once we have healthy baseline).

**Result:** PASS — DOM actually SHRANK by 36 nodes across 5 cycles. Drawer/Menu unmount is working correctly; lazy-loaded components clean up too.

## Live-run summary

```
$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_14_NoSilentApiFailures'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (58s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_15_FilterPersistsAcrossBackButton'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (61s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_16_DataGridNoDuplicateRowIds'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (52s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_17_DrawerOpenCloseDoesntLeakDom'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (110s)
```

**4/4 PASS. 0 new product bugs surfaced.**

## Cumulative bug-class coverage (BH_01..17)

| # | Class | Test | Status |
|---|---|---|---|
| 1 | Cross-view consistency | TC_BH_01 | PASS |
| 2 | Validation bypass | TC_BH_02 | PASS |
| 3 | Silent data-fetch fail | TC_BH_03 | PASS |
| 4 | Layout overflow | TC_BH_04 | PASS |
| 5 | XSS (DOM-side) | TC_BH_05 | PASS |
| 6 | SQL injection (DB-side) | TC_BH_06 | PASS |
| 7 | Sort no-op | TC_BH_07 | PASS |
| 8 | Persistence leak on Cancel | TC_BH_08 | PASS |
| 9 | Pagination total flicker | TC_BH_09 | PASS |
| 10 | Missing button debounce | TC_BH_10 | PASS |
| 11 | SPA history corruption | TC_BH_11 | PASS |
| 12 | Silent JS console errors | TC_BH_12 | PASS (3 real bugs filtered) |
| 13 | Menu focus trap (a11y) | TC_BH_13 | SKIP (1 real bug) |
| 14 | Silent network 4xx/5xx | TC_BH_14 | PASS |
| 15 | Filter state coherence | TC_BH_15 | PASS |
| 16 | Row duplicate detection | TC_BH_16 | PASS |
| 17 | DOM-leak (component unmount) | TC_BH_17 | PASS |

**17 bug classes covered. 4 real product bugs surfaced (all in round 3).**

## Round comparison

| Round | New tests | Real bugs found |
|---|---|---|
| 1 | 5 (BH_01..05) | 0 |
| 2 | 4 (BH_06..09) | 0 |
| 3 | 4 (BH_10..13) | **4** |
| 4 | 4 (BH_14..17) | 0 |

The round-3 yield (PLuG, /me 401, /refresh 400, kebab focus trap) suggests the highest-yield test classes are: console-error inspection, accessibility close-mechanism probing. Network 4xx/5xx (R4) didn't add new finds beyond R3's console check — the 2 known endpoints were already caught.

## Files changed

| File | Lines |
|---|---|
| `src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` | +~410 / -1 |

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.
