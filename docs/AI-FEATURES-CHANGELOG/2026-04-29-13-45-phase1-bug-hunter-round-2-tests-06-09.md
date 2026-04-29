# Phase 1 Bug Hunter — Round 2 (TC_BH_06..09)

**Date / Time:** 2026-04-29, 13:45 IST
**Branch:** `main`
**Site:** `acme.qa.egalvanic.ai`
**Result:** 4/4 PASS live. 0 real product bugs surfaced.

## What you asked for

> "find more bugs and create test case for that"

The first round (TC_BH_01..05) covered cross-view consistency, validation bypass, silent data-fetch fail, layout overflow, and XSS. This round expands to 4 more bug *classes* targeting backend boundaries, UI invariants, persistence integrity, and pagination.

## File modified

`src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` — added 4 new tests.

## The 4 new tests — what each one hunts

### TC_BH_06 — Asset search safely handles SQL-injection payload

**Bug class:** server-side SQL injection. Counterpart to TC_BH_05 (XSS, DOM-side).

**Payload:** `' OR '1'='1 --` — a classic tautology that bypasses parameterized WHERE clauses if the input is concatenated into raw SQL.

**Two falsifiers (both must pass):**
1. Post-injection visible row count must NOT exceed pre-injection count. A working injection would make the query return ALL rows (more than the search-narrowed result).
2. No raw SQL error markers leak into the DOM: `syntax error`, `near 'or'`, `ora-`, `sqlstate`, `psqlexception`, `mysql_fetch`, `unclosed quotation`, `sqliteexception`, `pg_query`.

**Why two falsifiers:** an injection that's silently ignored (input sanitized) AND an injection that errors loudly are different bug classes — both are bad, but for different reasons. Catching either is win.

**Result:** PASS — search row count stayed bounded, no SQL errors leaked.

### TC_BH_07 — Asset Name sort actually reorders rows

**Bug class:** silent UI no-op. Column-header sorts in MUI DataGrid are a frequent regression point — the visual arrow flips but the underlying data array isn't re-sorted, so the user sees "sort applied" but nothing actually changed.

**Honest preconditions (skip if not met):**
- Grid must have ≥2 visible rows
- Rows must have ≥2 distinct names

Without these, any order is "sorted" — the test cannot falsify.

**Falsifier:** click Asset Name column header (sort asc) → click again (sort desc). The first row text after asc MUST differ from first row text after desc. If they're equal AND there are ≥2 distinct names in the grid, sort is broken.

**Result:** PASS — first row changed between asc/desc directions.

### TC_BH_08 — Create form Cancel discards typed name (no leak)

**Bug class:** persistence leakage on cancel. Some apps debounce-save to drafts, auto-create on first keystroke, or save on blur. If Cancel doesn't actually rollback, the user's "I changed my mind" leaves a ghost asset in production data.

**Sentinel-string pattern:** the test types `BH08Cancel_<timestamp>` — a string only the test owns. After Cancel, the test searches the grid for that sentinel.

```
Type sentinel into Asset Name (via React native-setter) →
Click Cancel →
Navigate back to Assets list →
Type sentinel into search input →
Count matching rows →
Assert == 0
```

**Why native-setter:** `input.value = 'x'` doesn't notify React. Must call `Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, 'x')` then dispatch input + change events. Otherwise the React state never sees the value and the test is testing nothing.

**Result:** PASS — 0 grid matches for the sentinel after Cancel. No leak.

### TC_BH_09 — Pagination "of N" total stays constant across pages

**Bug class:** pagination total flicker. Common failure modes:
- Backend recomputes COUNT on every page request, and concurrent writes shift the total
- Frontend computes total from `visible_rows.length` instead of the API's total field, so page 2 shows "25 of 25" instead of "25 of 477"
- Cursor-based pagination losing total on subsequent fetches

**Falsifier:** parse `of N` from the MUI pagination footer on page 1. Click next. Parse on page 2. The two N values MUST be identical.

**Honest skips:**
- No pagination footer found → SKIP (different paging widget; can't falsify)
- Footer doesn't match `of N` regex → SKIP (UI variant; can't falsify)
- Next page button disabled → SKIP (single-page dataset; can't falsify cross-page stability)

**Result:** PASS — N stayed constant across page-1 → page-2 transition.

## Live verification

```
$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_06_AssetSearchSqlInjectionSafe'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (57s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_07_AssetNameSortChangesOrder'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (55s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_08_CreateAssetCancelNoLeak'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (74s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_09_PaginationTotalStable'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (54s)
```

**4/4 PASS. 0 product bugs surfaced.**

## Why "0 bugs found" is still a win (round 2)

Same logic as round 1: these tests live in the suite forever. Future regressions trigger them automatically.

- The first time a developer changes the search endpoint to use raw SQL, TC_BH_06 fires.
- The first time the DataGrid sort handler regresses, TC_BH_07 fires.
- The first time Cancel becomes a save-on-blur, TC_BH_08 fires.
- The first time pagination switches to client-side counting, TC_BH_09 fires.

That's 4 new bug-class tripwires.

## Bug classes covered (cumulative across BH_01..09)

| # | Class | Test | Surface |
|---|---|---|---|
| 1 | Cross-view consistency | TC_BH_01 | Asset list ↔ Edit drawer |
| 2 | Validation bypass | TC_BH_02 | Edit Asset form |
| 3 | Silent data-fetch fail | TC_BH_03 | Create form Asset Class |
| 4 | Layout overflow | TC_BH_04 | Asset DataGrid |
| 5 | XSS (DOM-side) | TC_BH_05 | Connections search |
| 6 | SQL injection (DB-side) | TC_BH_06 | Assets search |
| 7 | Sort no-op | TC_BH_07 | Assets DataGrid header |
| 8 | Persistence leak on Cancel | TC_BH_08 | Create Asset form |
| 9 | Pagination total flicker | TC_BH_09 | Assets DataGrid footer |

## Still-uncovered bug classes (future rounds)

- Race conditions (rapid double-click on Save → duplicate create)
- Unicode handling round-trip (emoji + RTL + CJK)
- Authorization bypass (read-only role hits write endpoints)
- Stale token handling (auth refresh mid-action)
- Concurrent edits (two browsers, last-write-wins?)
- Browser back-button after navigation (state preservation)
- Network offline / slow-3G graceful degrade

## Files changed

| File | Lines |
|---|---|
| `src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` | +~360 / -1 |

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.
