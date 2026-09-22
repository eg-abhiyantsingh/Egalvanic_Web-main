# Grid sort sweep — 2026-09-22 (bundle index-C9NJAR1x.js, +admin, site Android Site 2 = sld aadcee4c-7dd0-45b3-81b9-309c5c166084)

Method per grid: (1) pull the FULL data set from the API and compute the true alphabetical first/last; (2) click the header once (asc) and twice (desc) and read row 1; (3) count non-telemetry API calls per click; (4) change rows-per-page and re-read row 1 (varied-parameter invariant). A correct sort surfaces the true extreme regardless of page size.

## /assets — Asset Name (data-field `label`) — PAGE-LOCAL SORT, PROVEN
- Grid: 1–25 of 343, server-paginated (`GET /api/lookup/v2/nodes/{sld}?page=1&page_size=25`).
- TRUTH from the API, all 343 rows, `label`, case-insensitive: ascending first = `11N-H1-1`; **descending first = `yu`**, then `ytutyu`, then `Utility-1`.
- Click 1 (aria-sort=ascending): row 1 = `11N-H1-1`. Looks right only because the loaded page happens to start there. **0 API calls.**
- Click 2 (aria-sort=descending): row 1 = **`5N-H1-2`**, then 5N-H1-1, 4N-H1-2, 4N-H1-1, 3rd assst. `yu` is nowhere on the page. **0 API calls.** Pager still 1–25 of 343.
- `5N-H1-2` is exactly the alphabetical last of the 25 rows the API returned for page 1. The grid sorted the loaded page, not the data set. -> 01
- Repeat on a fresh load: two header clicks -> descending, row 1 `5N-H1-2`, still 1–25 of 343, **0 API calls across both clicks** (fetch hooked, telemetry excluded). -> 02

## /connections — Source Node (data-field `sourceLabel`) — SAME PATTERN
- Grid: 1–25 of 166, server-paginated. Columns Source Node / Target Node / Connection Type all marked sortable.
- Default row 1 = `Fuse2`. Click 1 (ascending): row 1 = `ATS-EM-EL`. Click 2 (descending): row 1 = **`U6`**, then U4, U2; page ends `Fuse6`, `Fuse22`. **0 API calls on either click.** -> 03
- Truth pull for the full 166 follows.
- TRUTH for /connections, all 166 rows from `GET /api/connections/v2/sld/{sld}?page=N&page_size=100`, field `source_label`: descending first = `U6`, `U4`, `U2` — the SAME three the grid shows. So on this data set row identity cannot separate page-local from server sort; `U6` happens to sit on page 1. Falling back to the page-2 continuity check.

## /sessions (Work Orders, Open) — SERVER-SORTED, and the old "stuck Created" defect is GONE
- Grid: 1–25 of 147. Sortable: Created, Work Order, Account, Quote / EMP, Facility, Due Date. Not sortable: Priority, Service, Status, Actions.
- **Created** click 1 -> ascending, row 1 `Jul 21, 2026`, then Jul 29, Aug 11 (oldest first — the thing the 25 Aug report said was impossible); click 2 -> descending, `Sep 22, 2026`. **Each click fires `POST /api/company/{id}/workorders/v2`.**
- **Work Order** click 1 -> ascending `11 sep`, `11 sep abs`, `1234`; click 2 -> descending `ZP-4018 QA AF+IR check-offs (delete me)`, `WOTest_20260815_183113`, `Work Order - Sep 8, 5:20 PM web ir check`. Each click fires the same POST. The extremes are consistent with a sort over all 147, not over the 25 loaded. -> 04
- Verdict: /sessions sorts on the server for every sortable column, including Created. The 2026-08-25 "stuck Created" finding no longer reproduces on this build.
- **/connections PROVEN page-local by continuity.** Descending applied (aria-sort=descending on both pages). Page 1 ends `Fuse21`, `Fuse2`, `Fuse19`. Pressed "Go to next page" -> pager 26–50 of 166, one call to `/api/connections/v2/sld/{sld}` -> page 2 begins **`U10`**, then `RM057-SWGR-CP-1` ×4, and ends `PH-SWBD-H-1`, `PH-DIST-H-2`. Under a true descending order nothing on page 2 may sort after page 1's last row; `U10` sorts after `Fuse19`. Each page is sorted on its own. -> 05
- /sessions request bodies captured: Created click -> `{"page":1,"page_size":25,…,"sort_by":"created_at","sort_dir":"desc"}`; Work Order click -> `…,"sort_by":"name","sort_dir":"asc"`. The sort travels to the server. This is what the broken grids should be doing.

## /issues — Title (data-field `title`) — page-local pattern
- Grid: 1–25 of 48, data via `POST /api/v2/issues/list`. All five data columns marked sortable.
- Click 1 (ascending): row 1 `Indicating light / meter not functional — 11N-H1-2`; click 2 (descending): row 1 `Thermal Anomaly on ATS-EM-L`; page ends `Repair Needed`, `QA-DEMO SCCR issue for needs-pick (delete me)`. **0 API calls on either click.** Continuity check follows. -> 06
- **/issues PROVEN page-local by continuity.** Descending on Title; page 1 ends `QA-DEMO SCCR issue for needs-pick (delete me)`; next page (26–48 of 48, `POST /api/v2/issues/list` with `page:2`) begins `Thermal Anomaly`, `Thermal Anomaly`, `Test IR — Thermal Anomaly`. T sorts after Q under descending. The request body is `{company_id, sld_id, page, page_size, filters:{status}, search}` — **no sort field at all**. -> 07

## /panel-schedules — Panel Name (data-field `label`) — PROVEN page-local, and the sort is DROPPED on paging
- Grid: 1–25 of 51, `GET /api/panels/sld/{sld}?page&per_page` (no sort param). Sortable: Panel Name, Amperage, Voltage, Building, Floor, Room, Usage, Status.
- Click 1 (ascending) `11N-H1-1`; click 2 (descending) `9N-H1-2`, `9N-H1-1`, `8N-H1-2` — digits only, no letters, on a site whose panels include `A78989`, `AIC RAting fix`, `P1-EM-SWBD`, `Panelboard 1`. **0 API calls on either click.**
- Next page -> 26–50 of 51 begins **`A78989`** (sorts after page 1's last, `3N-H1-2`) and the header's `aria-sort` has reset to **none**: the user's sort choice is also lost when they page. -> 08

## /asset-classes — Name — client-paginated, sort covers all rows: OK
- 1–25 of 49 but "Go to next page" fires **0 API calls** (all 49 rows are already in the browser). Sorting a fully loaded set locally is correct; not part of this defect.

## /reporting/builder — NO sortable columns
- 1–25 of 112, grouped by service type. Name / Type / Service Type / Template Format carry no sort control at all. Not a sort defect (nothing is offered); noted as a gap. -> 09

## /site-walks — re-check pending (first pass left the header in aria=none before the continuity step). `/api/site-walk/list` takes no page params and next-page fired 0 calls -> probably client-paginated.

## Grids that sort correctly (controls for the defect)
- **/opportunities (Quotes)** — 1–10 of 205. Quote (title) click 1 -> `POST /api/company/{id}/quotes/v2` body `{"page":1,"page_size":10,"search":"","sort_by":"title","sort_dir":"asc"}` -> `10 - Sep`; click 2 -> same POST with `sort_dir:"desc"` -> `Yxbzjzxj - EMP -kd`. Next page (11–20 of 205) fires the POST, page 2 begins `test today` after page 1 ended `testtt - Rev 1`: consistent. **Server-sorted, correct.** -> 12
- **/users (Platform Users)** — 1–25 of 227. Name click -> `POST /api/users/company/{id}/v2`, descending `zz ddd`, `tttt uyyuyu`, `test user`; next page fires the POST; page 2 begins `Rajat Patel` after `Shubham qafacility`: consistent. **Server-sorted, correct.**
- **/sessions (Work Orders)** — server-sorted on every sortable column including Created (see above).
- **/site-walks** — 1–25 of 50 but `/api/site-walk/list` takes no page params and next-page fires 0 calls: all 50 rows are in the browser, so the local sort covers the whole set. Descending `Z Platform`, `Z - Hospital`, `TestBusPanel2`; page 2 begins `DemoFinal_02_07` after page 1 ended `Pavlos said`: consistent. **Client-paginated, correct.**
- **/asset-classes** — same shape (49 rows all loaded, 0 calls on next page). **Correct.**

## Not applicable
- **/customers** — a tree, not a grid (`/api/customers/tree/v2?limit&offset`, 1–25 of 102); no sortable headers are offered. -> 11
- **/locations** — a tree; no grid, no sort controls.
- **/reporting/builder** — a grid but no column is sortable (gap, not a defect).
- **/tasks** — 1–25 of 33. Name click -> `POST /api/v2/tasks/list` body `{sld_id, page, page_size, filters:{status:"pending"}, search, "sort_by":"title","sort_dir":"asc|desc"}`; descending `yuouio`, `XT-ONLINE-CTL-N26 QA-VERIFY delete me`; next page fires the POST, page 2 begins `Arc Flash Label Placement` after `QA-VERIFY own-offline 140324 delete me`: consistent. **Server-sorted, correct.** -> 13
- **/emps** — 1–25 of 206. EMP # click -> `POST /api/company/{id}/committed-quotes/v2` with `sort_by:"commitment_number"`; next page fires the POST; consistent. **Server-sorted, correct.**
- Single-page grids (all rows already loaded, so a local sort is complete and correct): **/materials** 1–18 of 18 -> 14 · **/labor** 1–20 of 20 · **/test-equipment** 1–11 of 11 · **/attachments** 1–5 of 5 -> 15.

## /eg-forms (Builder -> Forms) — Title — PROVEN page-local
- 1–25 of 331. Sortable: Title, Form Type, Template (Asset Class not sortable). Click 1 ascending / click 2 descending: **0 API calls each**. Descending row 1 = `Cleaning & Lubrication — Lubrication`.
- Next page (26–50 of 331, one request) begins `Clean, Tighten, Torque — Cleaning` after page 1 ended `26 may abhiyant`. `C` after `2` under descending = page-local. 331 rows makes this the worst-affected grid by volume.

## /guest-portal-users (Admin -> Guest Portal Users) — Name — PROVEN page-local
- 1–25 of 63. Sortable: Name, Email, Customer, License, Roles, Status. Sort click: **0 API calls**; descending row 1 = `test portal`.
- Next page (26–50 of 63, one request) begins **`zz ddd`** after page 1 ended `Avani Patel`. `zz ddd` is the alphabetically last name on the whole list and it is sitting on page 2 of a "descending" sort.

## More controls
- **/planned-work** — 1–9 of 9 but the sort still goes to the server: `GET /api/planned-workorders?page&page_size&sort_by&sort_dir`. Correct.
- **/emps Created** — click 1 ascending `Jan 29, 2026`, click 2 descending `Sep 22, 2026`, each a `POST …/committed-quotes/v2` with `sort_by:"created_at"`. The 25 Aug "stuck Created on /emps" finding no longer reproduces.
- Single page: **/services** 20, **/offices** 6.
- Evidence -> 16 (/eg-forms page 1, `Cleaning & Lubrication…` first, ends `26 may abhiyant`), 17 (page 2, `Clean, Tighten, Torque — Cleaning`), 19 (/guest-portal-users page 1 ends `Avani Patel`), 20 (page 2 begins `zz ddd`).

## /assets second column — Asset Class — also page-local
- TRUTH from all 343 rows, `node_class_name`, descending first = **`Utility`**, then `Transformer`, `Test_21`. Grid after two clicks on Asset Class: `Transformer`, `Test_21`, `Test`. No `Utility` asset was on the loaded page, so the whole grid is affected, not just Asset Name. -> 18

## Side observation (separate defect, not part of the sort ticket)
- **/connection-classes and /issue-classes also open at "0–0 of 0"** on a cold load, the same shape as the Asset Classes regression. Refresh-icon check follows.

## FINAL SCOPE OF THE SORT DEFECT — six grids, all server-paginated, all sorting the loaded page only, 0 requests per header click
/assets (343) · /connections (166) · /issues (48) · /panel-schedules (51, and the sort state is dropped on paging) · /eg-forms (331) · /guest-portal-users (63).
Correct controls: /sessions, /emps, /opportunities, /users, /tasks, /planned-work (server-sorted, `sort_by`/`sort_dir` on the wire); /site-walks, /asset-classes + all single-page grids (fully loaded, local sort complete).
- Refresh-icon check: **/connection-classes** 0–0 of 0 -> **1–3 of 3** after one press; **/issue-classes** 0–0 of 0 -> **1–8 of 8**. The "opens empty, refresh fills it" regression covers ALL THREE Admin class grids (Asset / Connection / Issue Classes), not just Asset Classes. Still unfiled — owner's go-ahead pending.

## FILED: ZP-4338 — https://egalvanic.atlassian.net/browse/ZP-4338
"Web: Column sorting only sorts the rows on the current page — Assets, Connections, Issues, Panel Schedules, Forms and Guest Portal Users". Bug · High · assignee Krunal · fixVersion Web v2.2 (14156) · sprint Z-26-09-S2 (1222). One ticket, six pages, with the correct grids listed as scope controls and the endpoints in a closing developer note.
