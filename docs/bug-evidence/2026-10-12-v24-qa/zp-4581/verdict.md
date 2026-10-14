# ZP-4581 — UI: Building → Floor → Room filter on Arc Flash Readiness (Task #78 / TKT-1428)

**Verdict:** PARTIAL. The filter itself works on QA build `index-C-Cc1qD1.js` (checked live 13 Oct 2026, 15:55–17:07 IST; checked again just before this verdict). Every QA Review row passes. One layout bug comes with this change: on a 1366 px laptop screen, once a building is picked, the **Connection Details** tab is pushed out of view and can't be clicked (the tab strip has no scroll arrows).

The arc-flash code chunk is byte-for-byte the same as yesterday's build `index-D1IWZ6SI.js` (only the import hashes differ). So yesterday's large-site timings (Performance sld, 8,481 assets) still apply.

Jira: status In QA, no comments (read live 13 Oct). No LaunchDarkly gate: the filter is not behind a flag, and the `/arc-flash` route needs only the `features.arc_flash.view` permission. **No flag flip is needed.**

## Checked

| # | Check | How | Result |
|---|---|---|---|
| QA-1 | Default shows all buildings | London, UK (80 assets, 6 buildings) | PASS. Shows "All buildings", gauges 3% / 12% / 64%, 18 class cards adding up to 80 (shot 1) |
| QA-2 | Building narrows gauges, cards and Asset Details; Floor appears | Building-1 | PASS. 23 assets; the page matches the API exactly (23). Gauges 1% / 7% / 29%. Floor dropdown appears (shot 2) |
| QA-3 | Floor, then room, each narrow further | Ahmedabad › third › 105 | PASS. 15 → 15 → **1** asset. The room list has 15 rooms, 101–115 (shot 8). Large site "new site account" (40 buildings): Red Pile 14 → room 1 (shots 10, 11) |
| QA-4 | Changing the building clears floor and room | Building-1/Floor-1/Room-1 → Ahmedabad | PASS. Floor and room reset to "All floors" and the count shows 15 |
| QA-5 | Asset Details class picker lists only classes in the location | Building-1 / Floor-1 | PASS. Exactly the 13 classes present there, including Loadcenter (shot 6) |
| QA-6 | Connection Details counts a connection when either end is in the location | Building-1 / Floor-1 | PASS. Busway shows 2 rows, and the API has 3 location edges across all types. Yesterday, on the 8,481-asset site, every type matched the API exactly (same code) (shot 7) |
| QA-7 | "No building" shows unlocated assets | London | PASS. 8 assets = API 8; its Floor list offers "No floor" (shot 5) |
| QA-8 | Site switch drops levels that don't exist | London "Navratna Park / A Wing - Floor 1" → Richmond, CA | PASS. "Navratna Park" also exists on Richmond, so it is **kept**: 26 assets = API 26. The floor doesn't exist there, so it is **dropped** (shot 9) |
| QA-9 | French labels | App language fr | PASS. Shows "Tous les bâtiments", "Tous les étages", "Toutes les pièces", "Sans bâtiment" (shot 12) |
| UI | "All rooms" restores the floor view; "All buildings" restores the full site | London | PASS. 1 → 15 → 80 |
| UI | Engineering Mode with a filter on | Building-1 | PASS. The filter is kept; cards recompute within the location (e.g. Fuse 0/3 → 3/3) |
| UI | Tab strip width with a filter set | 3 levels set, at 1600 / 1440 / 1366 px | **FAIL at 1366 px.** "Connection Details" is 0 px visible and "Source/Target Connec…" is cut. At 1440 px only 24 of 154 px show. At 1600 px it is fine. The tab strip has no scroll arrows (shot 4). See Bug 1 |
| UI | Refresh / back button / URL | Set a filter → refresh; go to Assets → Back | The filter resets to "All buildings" in both cases. The URL never holds the filter. It is not in the ticket, so this is listed as an enhancement |
| UI | Empty combinations | Cascade logic + live | Can't happen: each lower list only shows names that exist under the level above |
| Export | "Export readiness report" with a filter on | Building-1 / Floor-1 → Generate Report (request captured, then aborted) | The request body has no location (`{sld_id, type, reporting_config_id, output_format, vars:{}}`), so the report covers the whole site. Reports are not in the ticket's list, so this is an enhancement/out-of-scope note |
| API | No new calls | Network log while changing the filter | PASS. Zero `/api` calls on a filter change; it is client-side only, as the ticket says |
| API | Data source carries the location | `GET /api/lookup/nodes/{sld}?include_bus_nodes=true` | PASS. Each node has `building_name` / `floor_name` / `room_name` |
| Tenant | Another company's site | PM, AM, EE, FM and CP call `/lookup/nodes/f86052b7…` (Saveland Avenue Lofts) and `/sld/f86052b7…/edges` | No data: the nodes call gives the HTML fallback and edges give 422. The Super Admin also gets HTML here from the acme host, so there is no positive control. Result: "no leak seen" |
| Tenant | Acme site not assigned to the seat | PM `/lookup/nodes/<id>` for 3 sites outside its 116 | The API still answers with JSON (Test site A 1 row, "QA-DEMO ZP-4675 site…" 96, "Accout Site" 24). This is the **known** `/lookup/*` site-scope gap (memory, since 11 Sep; ZP-4709 is in progress). It is not caused by this ticket, which adds no API |
| Roles | Super Admin, PM, AM, EE, FM | Sign in, open the page, choose a building | PASS for all. Each sees its own site's buildings and the counts narrow (shots 13–17). PM, AM and FM are offered the page in the nav; EE and Super Admin find it under Engineering |
| Roles | Client Portal (no `features.arc_flash.view`) | CP nav and typed `/arc-flash` | Nav: not offered (no Engineering rail). **Typed URL opens the whole page with data, the new filter and "Export readiness report"** (shot 18). This is an old route-guard gap, not caused by this ticket. See out-of-scope bug A |
| Perf | Filter change time | London; new site account (1,141 nodes); Performance sld (8,481, yesterday, same code) | 0.6–1.6 s per pick including opening the dropdown (yesterday 564–671 ms for the re-render on 8,481 assets). No extra requests |
| Console | | All runs | No new errors. Only noise: beamer CSP font, 429 rate limits, and 422 from my own foreign-site probe |

## Bugs found

**Bug 1 (in scope, frontend, Medium): on a 1366 px screen the "Connection Details" tab disappears after you pick a building, floor and room**
1. Use a 1366×768 laptop window and sign in as a Project Manager or Admin.
2. Open Engineering › Arc Flash Readiness on site "London, UK".
3. Pick Building "Building-1", Floor "Floor-1", Room "Room-1".
4. Look at the tab row.
- What happens: the three new dropdowns squeeze the tab row. "Source/Target Connections" is cut to "Source/Target Connec", and **"Connection Details" is gone**. There are no scroll arrows and you can't scroll sideways. You can only reach it with the keyboard (Tab, then arrow keys), and then Overview scrolls out of view instead.
- What should happen: all four tabs stay reachable. Either wrap the filters onto their own row or turn on scroll buttons for the tabs.
- Why it matters: a field crew on a normal laptop who filters to their building can no longer open Connection Details for that building.
- Evidence: `QA-13Oct-ZP-4581-4-1366px-three-levels-connection-details-tab-hidden-FAIL.png`. Measured: tab row 368 px wide, content 572 px, `overflow-x:hidden`, 0 scroll buttons. At 1440 px only 24 px of the tab shows; at 1600 px it is fine.
- Suggested assignee: Krunal (frontend). Per the owner's 13 Oct rule, file to Avani.
- Duplicates: JQL search (tab / Connection Details / Arc Flash Readiness, last 60 days) found no existing ticket.

## Out-of-scope bugs (not caused by this ticket)

**A. A Client Portal user can open Arc Flash Readiness by typing /arc-flash (Low–Medium)**
1. Sign in as the Client Portal user (`CP_EMAIL`).
2. The left rail shows only Site Data and Maintenance Portal; there is no Engineering menu.
3. Type `https://acme.qa.egalvanic.ai/arc-flash` in the address bar.
- What happens: the full Arc Flash Readiness page opens for London, UK, with gauges, class cards, the new Building/Floor/Room filter and "Export readiness report".
- What should happen: the page should be refused (Access Denied, or a redirect), because the CP role doesn't have `features.arc_flash.view`. The route table even lists `{path:"/arc-flash", permission:"features.arc_flash.view"}`.
- The bundle mounts `/arc-flash` straight from the layout (`pathname==="/arc-flash"`), so the permission check is skipped.
- Evidence: `QA-13Oct-ZP-4581-18-client-portal-typed-url-opens-arc-flash-FAIL-out-of-scope.png`.
- Related: ZP-4699 (Done), which removed Engineering surfaces from the Client Portal. It does not cover this route. Suggested: Avani.

## Enhancement suggestions
- Keep the Building/Floor/Room choice in the URL (e.g. `?building=…&floor=…`). Then refresh, Back and a shared link keep the view. Today a refresh drops it.
- Make "Export readiness report" follow the location filter, or at least say "whole site" in the dialog. Today a crew filtered to one building gets the whole-site report.
- Show a count next to each building/floor/room option (e.g. "Ahmedabad (15)"), as the class cards do. Then empty or tiny locations are visible before clicking.
- On sites with 40 buildings (new site account), add type-to-search to the Building dropdown.
- Add a single "Clear location" chip/button when any level is set; today you reset each dropdown by hand.

## Mobile part for Nency
This is a web-only change (eg-pz-frontend #1618). The arc-flash readiness screens on iOS/Android don't change in this ticket, so there is nothing to check unless the mobile apps add the same filter later.

## Not covered
- **Large-site timing on today's build:** the Performance sld (8,481 assets, 48 MB nodes payload) was not re-run. Yesterday's run on the identical chunk applies: 564–671 ms per change, and counts per building and floor matched the API exactly.
- **Real LaunchDarkly OFF/ON:** there is no flag for this feature.
- **Technician:** no web access, as expected.

## Proposed Jira comment
```
h3. QA result on QA (build index-C-Cc1qD1.js, 13 Oct 2026): filter works, 1 layout bug

The Building → Floor → Room filter works on every row of the QA Review list. I tested on London, UK (6 buildings), "new site account" (40 buildings) and Richmond, CA. Every count matched the asset data from the API.

||Check||Result||
|All buildings by default; picking a building narrows gauges, class cards, Asset Details|PASS|
|Floor, then room, narrow further (Ahmedabad › third › 105 = 1 asset)|PASS|
|Changing the building clears floor and room|PASS|
|Asset Details class picker only lists classes in the location|PASS|
|Connection Details counts a connection if either end is in the location|PASS|
|"No building" shows unlocated assets|PASS|
|Site switch keeps a building that exists on the new site, drops a floor that doesn't|PASS|
|French labels|PASS|
|No extra API calls when the filter changes|PASS|
|PM, AM, EE, FM, Super Admin|PASS|

*Bug: on a 1366 px laptop screen the Connection Details tab disappears after you pick a building/floor/room*
# Use a 1366×768 window. Open Engineering › Arc Flash Readiness on London, UK.
# Pick Building-1, Floor-1, Room-1.
# Look at the tabs.
*Actual:* "Source/Target Connec…" is cut and "Connection Details" is gone. There are no scroll arrows, so you can't click it.
*Expected:* all four tabs can be reached (filters on their own row, or tab scroll buttons).

!QA-13Oct-ZP-4581-4-1366px-three-levels-connection-details-tab-hidden-FAIL.png|width=720!
!QA-13Oct-ZP-4581-8-ahmedabad-third-room105-one-asset-PASS.png|width=720!
!QA-13Oct-ZP-4581-9-site-switch-keeps-shared-building-drops-floor-PASS.png|width=720!

Notes: the filter is lost on refresh, and "Export readiness report" still exports the whole site. These are suggestions, not part of this ticket.

*Status:* stays In QA until the 1366 px tab bug is fixed or accepted as a follow-up.
```

## Recommended status
**Stay In QA.** The ticket's own behaviour passes, but the new header dropdowns hide a tab on a common laptop width. The owner/dev should decide whether to fix it here or accept it as a separate follow-up bug. If they accept a follow-up, READY TO RELEASE is fine: there is no flag, so no flip is needed.

## Evidence (this folder, 13 Oct)
1 default · 2 floor list · 3 three levels (Building-1 has 1 floor/1 room) · **4 1366 px FAIL** · 5 No building · 6 class picker · 7 Connection Details · 8 room 105 = 1 asset · 9 site switch · 10 forty buildings · 11 large site three levels · 12 French · 13–17 roles PM/AM/EE/FM/Super Admin · **18 Client Portal typed URL (out of scope)**.
Yesterday's files `QA-12Oct-ZP-4581-*` (old build, same chunk code) are kept for the 8,481-asset performance run (shots 8–10, 14).
Scripts/JSON: `.playwright-mcp/v24/q13-4581-4751/` (a1, a2, c1, c2, af_lib.js, roles.js).
