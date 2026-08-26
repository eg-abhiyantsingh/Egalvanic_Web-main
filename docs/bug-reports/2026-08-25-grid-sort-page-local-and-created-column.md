# Data-grid sort defects — QA V1.36

**Env:** `https://acme.qa.egalvanic.ai` · **Date:** 2026-08-25 · **Writes made:** none
**Artifact:** https://claude.ai/code/artifact/8ca35956-54c9-4c95-92f9-108c3705a2cd (consolidated register — the per-report artifact was superseded)

Found while sweeping the V1.36 grid redesign for user-visible breakage. 11 grids exercised
(load → sort each column → search → clear). Two defects confirmed.

---

## BUG-A — HIGH — Sorting only sorts the rows already loaded

**Affected:** `/assets` (172 rows), `/connections` (183 rows)

The grid is paginated server-side (`1–25 of 172`) but the sort runs in the browser over the
25 rows currently loaded. Rows 26–172 keep their positions and are never compared. The column
header shows a normal descending arrow, so nothing indicates the result is partial.

### Reproduce
1. Open `/assets` on a site with >25 assets (ZTest_28_07 = 172).
2. Click **Asset Name** twice → header shows the descending arrow.
3. Top row is `8N-H1-2`.
4. Change **Rows per page** to 100. Do not touch the sort.
5. Top row is now `CB1`, and 60+ `Cable*` assets appear above where the old top row sat.

### Evidence
| Rows per page | Top row (Asset Name, descending) |
|---|---|
| 25  | `8N-H1-2` — the 25th name ascending, i.e. page one reversed |
| 100 | `CB1` |
| 25  | `8N-H1-2` — toggles straight back |

- `Cable99` sorts **above** `8N-H1-2` descending (`C` > `8`) yet is absent from the 25/page view.
- Network on every sort click: **0 requests**.
- The only request the grid makes: `GET /api/lookup/v2/nodes/{sld}?page=1&page_size=25`
  — no sort parameter, ever.
- `/connections`: same pattern — Source Node descending gives `U2` at 25/page, `U6` at 100/page.

Screenshots: `evidence/2026-08-25-grid-sort/sort_assets_desc_25.jpg`, `..._100.jpg`

### Fix direction
Pass sort field + direction to `/api/lookup/v2/nodes` and the connections endpoint, the way
`/workorders/v2` and the customers endpoint already do, so the server orders the full set
before paginating.

---

## BUG-B — MEDIUM — The `Created` column cannot be sorted

**Affected:** `/sessions` (Work Orders), `/emps` (EMPs)

`Created` loads descending and stays there. Three clicks produce no reorder, no `aria-sort`
change and no request. There is no way to list either grid oldest-first.

| Grid | Column | 3 clicks → aria-sort | Reordered | Requests |
|---|---|---|---|---|
| `/sessions` | `Created` | descending · descending · descending | no | 0 |
| `/sessions` | `Due Date` *(control)* | ascending | yes | 1 |
| `/emps` | `Created` | descending · descending · descending | no | 0 |
| `/emps` | `Site` *(control)* | ascending | yes | 2 |

Controls are on the same grids, clicked the same way — this is the column, not the click.
`Created` is marked sortable in the DOM, so it advertises a capability it does not have.

---

## Grid-by-grid result

| Grid | Rows | Sort runs | Verdict |
|---|---|---|---|
| `/assets` | 172 | browser | page-local — BUG-A |
| `/connections` | 183 | browser | page-local — BUG-A |
| `/sessions` | 16+ | server | Created stuck — BUG-B |
| `/emps` | 170 | server | Created stuck — BUG-B |
| `/customers` | 70 | server | correct |
| `/opportunities` | 169 | server | correct |
| `/planned-work` | 9 | server | correct |
| `/issues` | 14 | browser | **fine** — whole result set is loaded, so the client sort is complete |

Client-side sorting is only a defect when the server is paginating. `/issues` loads all 14 rows,
so it is clean, not a third instance of BUG-A.

---

## Deliberately NOT reported
- **Assets `Condition` column shows no reordering** — its values are empty across the dataset;
  there is nothing to sort. Not a defect.
- **DevRev `plug.js` SRI console error on every page** — third-party integrity mismatch,
  unrelated to this work.
- **Bad-URL detail-route error handling** (raw `Unexpected token '<'` message, blank
  `/customers/{missing}`, 500 on malformed path param) — owner ruled this low priority /
  not a blocker on 2026-08-25. Kept out of scope.

## Clean
36 nav routes swept with uncaught-exception, console-error and failed-request capture:
zero JS exceptions, zero failed API calls, zero blank shells, zero Application Error overlays.
Search filters correctly on every grid tested and clearing restores the rows.
