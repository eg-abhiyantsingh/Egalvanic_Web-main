# Bug hunt — V1.36 grid interactions

**Date:** 2026-08-25 · **Prompt:** "find better bugs related to ui changes check with all the
user crashed or any other major issue" (+ follow-up ruling the bad-URL error handling low priority)

## What changed
- `docs/bug-reports/2026-08-25-grid-sort-page-local-and-created-column.md` — new report
- `docs/bug-reports/evidence/2026-08-25-grid-sort/` — 2 live screenshots
- Artifact: https://claude.ai/code/artifact/4c7a0e40-8485-4f0c-a68c-859c686eeedf

## What was found
| # | Severity | Defect | Where |
|---|---|---|---|
| A | High | Sort only orders the loaded page; the rest of the dataset is never compared | `/assets`, `/connections` |
| B | Medium | `Created` column is marked sortable but does nothing | `/sessions`, `/emps` |

## Method — and why this round produced valid findings
The previous accessibility pass was retracted in full because every finding came from spotting an
**absent attribute** and assuming absence meant defect. This round only reports **observed behaviour
that contradicts what the UI claims**, which cannot fail the same way.

Three techniques did the work:

1. **Differential testing against a control.** Never "column X doesn't sort" on its own — always
   paired with a sibling column on the same grid, clicked identically. `Due Date` firing a request
   and reordering while `Created` does neither is what turns an observation into a defect: it rules
   out my click, my selector, and my timing in one move.

2. **A varied-parameter invariant.** A correct descending sort has a top row that does not depend on
   page size. So: sort, read the top row, change only rows-per-page, read again. The row moved
   (`8N-H1-2` → `CB1` → back), which can only happen if rows outside the page were excluded from the
   comparison. This is stronger than eyeballing order, because it needs no assumption about how the
   data *should* be ordered.

3. **Network-level corroboration.** Counting non-telemetry API calls per sort click separated
   server-sorted grids (a request fires) from browser-sorted ones (none). The request URL then
   confirmed the cause directly — `?page=1&page_size=25` carries no sort parameter at all.

## What was checked and deliberately dropped
- **Assets `Condition` column** looked like a third instance of BUG-A — no reorder on click. Its
  values are empty across the dataset, so there is nothing to sort. Dropped.
- **`/emps` initially looked page-local** on the same evidence as Assets. The page-size test came
  back negative and its sort did fire requests. Dropped from BUG-A; only its `Created` column is
  affected.
- **`/issues` sorts in the browser** but loads all 14 rows, so the sort is complete. Listed clean.
- **Bad-URL detail-route handling** — reported, then ruled low priority by the owner. Out of scope.

Dropping three of five candidates is the point: the page-size test and the empty-column check are
cheap, and each one killed a finding that would have been wrong.

## Coverage
11 grids exercised (load → sort every column → search → clear); 36 nav routes swept with
uncaught-exception, console-error and failed-request capture. Zero crashes, zero failed API calls,
zero blank shells. No data created, modified or deleted.
