# Web v2.2 deep pass — running notes (2026-09-21, build index-BV-phiFE.js, admin seat)

## Dashboard
- ZP-4038 Site Overview single-site: tiles 201 assets / 39 active WOs / $7,690 at risk; Assets by Type (8 slices, 201) + Open Issues by Type (27, 7 classes). Single picked site "17 July 2026", no company roll-up. -> 01-dashboard-site-overview.jpg

## Maintenance -> Reports (/maintenance/reports)
- ZP-4113: "Issue Report" card present as its own card (8 cards + WO reports table). Click -> Generate Report modal: config=Issue Report (HTML), PDF, options = Include resolved and closed issues / Include thermal (IR) photos / Include issue photos (exactly the 3 in the ticket). -> 02, 03
- ZP-4041: clicked Generate. ONLY Issue Report card shows spinner + "Generating..." chip + primary border; other 6 cards opacity .5, pointer-events none, 0 spinners. Toast "Generating report - the download starts when it's ready." -> 04

## Condition Assessment (/pm-readiness)
- ZP-4113: header has BOTH "Export issue report" and "Export assessment report". Issue export modal = Issue Report only. Assessment export dropdown = Program Compliance Report / EMP Lite / Condition Assessment / Asset Service History / Annual Maintenance Report, NO Issue Report. Scoping verified both ways. -> 05, 06, 07
- ZP-4043: Equipment Health band reads "0 Healthy 0% / 0 Assessment expired 0% / 0 At risk 0% / 201 Not assessed 100%" -> the `expired` band is in the UI. -> 05
- ZP-4084: Condition Distribution = Condition 1 (175) / 2 (9) / 3 (7) / Non-Serviceable (0); single vocabulary, no separate serviceability. -> 05
- ZP-4041: Asset Details tab columns CRIT/ENV/MAINT dots: Condition 1 rows = green/green/green, Condition 2 rows = green/green/amber -> dots agree with chip. -> 09
- ZP-4039: Asset Details filter row = Search assets / Location / Asset class / Shutdown rule / Condition / Assessment + Bulk Ops button (the isolate-and-fix tools). Findings tab has per-row edit pencil. -> 08, 09
- OBSERVATION (not filed): same page shows "ASSETS ASSESSED 0 (0% of 201)" tile + "201 Not assessed 100%" vs "Condition Distribution 191 assessed". Two definitions of "assessed" on one page (calculator-run vs condition-set). Confusing; check intent before filing.
- ZP-4152 side-signal: assessment export modal offers "Maintenance standard: Company default (NFPA 70B 2026)".

## Compliance -> Visualizer (/maintenance/compliance)
- ZP-4112: Visualizer tab present (Overview / Deviations / Acknowledgements / Program Elements 11 / Visualizer). Pinned ASSET + COND columns. Group by = No grouping / Asset class / Location. Filters tray = Location / Asset class / Service / Condition + Clear all / Done. Export button present (disabled while grid empty). Full screen button works (rail hidden). Empty state: "The standard prescribes nothing for this site's assets yet." Standard picker shows "Standard..." (none chosen) -> data blocker for the due-dot grid, same as ZP-4045/4060. -> 10, 11, 12

## Assets grid (/assets)
- Search "Disconnect" -> 6 Disconnect Switch rows; two carry Subtype chip "Fused Disconnect..." (subtype column populated -> ZP-4171 subtype data exists on this tenant). -> 13

## BUILD CHANGE mid-session
- QA rebuilt during the deep pass: `index-BV-phiFE.js` -> `index-BOaMecwk.js` (confirmed by curl). Dashboard/Reports/Condition Assessment/Compliance findings above were on BV-phiFE; everything from the Assets class-form checks onward is on BOaMecwk. Re-check key items on the new bundle before sign-off.

## Assets -> Edit drawer -> Engineering (class-specific fields, ZP-4167 / ZP-4174 / ZP-4171)
- Junction Box (new bundle): Engineering section = "Trust the Photos" + System Voltage ONLY. No Mains Type, no Manufacturer, no Panel Type, no SCCR. Matches ZP-4167 spec exactly.
- Incidental: /arc-flash Readiness shows Busduct class at 100% (17/17 complete) — consistent with the hide flags removing fields Busduct can never carry (ZP-4174). -> 15
- Disconnect Switch, subtype "Fused Disconnect", asset "test 14 sept" (new bundle): Engineering = Asset Subtype / System Voltage / **Mains Type*** only. No Manufacturer/Panel Type block, no SCCR in Engineering. (Manufacturer + Interrupting Rating + Ampere Rating live under CUSTOM ATTRIBUTES, not Engineering.) Matches ZP-4167: "Disconnect Switch: Mains Type present, no Manufacturer / Panel Type block". -> 16
- Tooling note: the Edit drawer's section nav button "Engineering" precedes the left-rail "Engineering" in DOM order; clicking the rail one navigates to /arc-flash. Scope to the drawer.
- Busduct, asset "37 Test Asset" (new bundle): Engineering = "Trust the Photos" + System Voltage ONLY. No Mains Type, no Manufacturer, no Panel Type, no SCCR. Matches ZP-4174 (bceseng_a3 gives Bus Duct the Junction Box treatment). -> 17
- Panelboard "Asset A2" (negative control, new bundle): Engineering = Asset Subtype / System Voltage / Phase Configuration* / Mains Type* / Manufacturer / Panel Type / SCCR (Label). Every field the bus classes hide is present here -> the hide is scoped to the flagged classes only. ZP-4167 + ZP-4174 have positive AND negative UI proof across 4 classes. -> 18
- ZP-4084 filters (new bundle): Assets filter row = Asset Class / Asset Subtype / Shutdown / **Condition** / Location / Parent Asset Name. ONE Condition filter; no separate COM + Serviceability filters. -> 19
- ZP-4084 Condition filter options (new bundle) = 1 / 2 / 3 / Non-serviceable / Not assessed. No "Not rated (0)". Verbatim match to the ticket. -> 20

## Admin -> Config -> Asset Classes (/asset-classes)
- 49 classes; columns Name / Box OK / OCP OK / Needs Source; Create Asset Class + Bulk Ops. Rail (Admin): Setup, Platform Users, Guest Portal Users, Customers, Offices, Asset Classes, Connection Classes, Issue Classes, Devices, Test Equipment. -> 21
- Edit Asset Class (Circuit Breaker, global): Name / Key / Description / Icon / Orientation / Device Role / Classification (Box OK, OCP OK, Needs Source) / Core Attributes (+Add) / PM Plans / Asset Subtypes (+Add Subtype; e.g. Low-Voltage Insulated Case CB, Low-Voltage Molded Case CB <=225). -> 22
- ZP-4171 class editor (new bundle): Core Attributes -> Add reveals the attribute row header **Name* / Type / Subtypes (empty = all) / Default Value / Description**. The per-attribute subtype mask control is live. -> 23
- ZP-4086: "Condition section" Listed/Required controls NOT visible on the global Circuit Breaker attribute row header (ticket: globals are disabled). Re-check on a class with existing core attributes (Panelboard) before concluding.

## Admin -> Config -> Devices (/devices)  (ZP-4110 / ZP-4111)
- Page exists: heading Devices, "Add device" button, search box + filter icon, columns Device / Catalog / Style / Classes / Services / Rules. Grid = "No rows", 0-0 of 0 -> acme has no device rules yet. -> 24
- ZP-4110 filter tray (new bundle): Manufacturer / Asset class / Service / Frame / Rules (Company and global) + search box. Matches the ticket's tray verbatim. -> 25
- ZP-4110 add flow: "Add a device" dialog -> Service ("Choose a custom service...") -> Asset class ("Pick a service first") -> Device ("Pick the asset class first - it decides which part of the library to search") -> "Continue to rules". Class-scoped library search as specified. -> 26
- ZP-4110 add flow end to end (new bundle): Service = Infrared Thermography -> Asset class options = Circuit Breaker / Disconnect Switch / Fuse / Relay / Tie Breaker -> Device = Manufacturer select + "Type, catalog number or style..." search. -> 27
- ZP-4111 picker: typing "FA" returns SKM device rows scoped to Circuit Breaker ("FA 350 · 3000A — ALLIS-CHALMERS", "PCCFA · PCCF_A_2000F — EATON/CUTLER-HAMMER · Low Voltage Breakers · Static Trip · 600 V" ...). Picker functional. The 100-row cap + "Showing the first 100 matches" caption were not captured in this pass; API-level check earlier showed the backend `sources=` parameter is NOT implemented on QA (skm / bogus / omitted all return identical rows), so the ZP-4111 fix itself is not on this environment. -> 28

## ZP-4042 re-hit on the NEW bundle (index-BOaMecwk.js)
- GET /api/reporting/history?limit=5 -> **500 internal_error**, trace_id d3bde59f740e4233a7109eb950fdde3b. Positive control GET /api/reporting/configs?limit=1 -> 200. The promotion blocker survived the mid-session rebuild. Third confirmation today (becd79c4…, 7dc46732…, d3bde59f…).
- WATCH: /asset-classes rendered 49 rows at ~20:12 (screenshot 21) and "No rows, 0-0 of 0" at ~20:25 after the rebuild to index-BOaMecwk.js (screenshot 29). Verify via API + reload before calling it a regression.

## Builder -> Services (/services)
- Grid: Service / Type (Primitive|Custom) / Work Order Type / Asset Classes / De-Energized / Status (Ready | Needs a listing) / Actions; 20 services; Create Service. Custom v2 services present: "70 B Service Type", "corrective IR". Rail: Reports, Services, PM Plans, Issue Suggestions, Forms. -> 30
- ZP-4109 (new bundle): custom service "corrective IR" detail (/services/46f811dc-...): Asset classes section carries the **"By class | All methods"** toggle + "Add class"; header has Pricing setup / Update service / ⋯. -> 31
- Asset Classes empty grid: /api/lookup/node-classes -> 200 with 47 classes, so the data is there; the empty grid is frontend-side. Re-check with a hard reload.
- ZP-4152 (new bundle): service header ⋯ menu = **Rename** / Edit description / Change to IR Checklist / Version history. Plain rename item present (no AI round-trip). -> 32
- ZP-4109 (new bundle): "All methods" view = flat grid, columns Class / Method / Description / Labor / Est. / Forms / Test equip. / Kits / Materials / Per-unit, checkbox per row, text filter "Class, method, labor, equipment", count "1 methods". This service has a single method (ATS inspection), so the multi-select "Edit N selected" path can only be shown with N=1 here. -> 33
- SAFETY: my class-row click opened "Remove ATS?" (confirm dialog). Cancelled. Nothing removed.
- Service class row expanded (ATS): method "ATS inspection · Apprentice Electrician 30m · Megger · Material Preset", "Add procedure", RULES "checked in order — first match wins", "Edit rules", rule 1 "Every other asset → ATS inspection". No "Device specific overrides" text on this service (no device rules exist on acme; the overrides dialog lists device rules, so nothing to show). -> 34

## Builder -> PM Plans (/pm-plans)  (ZP-4152 default standard)
- 11 standards, columns PM Standard / Plans / Actions. Every COMPANY-OWNED row has star(outline) + pencil + delete; the GLOBAL "NFPA 70B 2026" (228 plans) row has NO star and no actions -> "only company-owned rows expose the set-default action" holds. 10 star buttons, 0 filled -> no default starred on acme (matches API is_default=false on all 11). UI present; star deliberately not clicked (changes company-wide default for other testers). -> 35

## CANDIDATE REGRESSION (new bundle index-BOaMecwk.js): Admin -> Asset Classes renders "No rows"
- Three full page loads of /asset-classes on the new bundle -> "No rows", "0-0 of 0" (screens 29, 36). GET /api/lookup/node-classes -> 200 with 47 classes in the same session. The single 49-row render (screen 21) was reached by an in-app rail click while the OLD bundle was still in memory (SPA), so the contrast is old-vs-new bundle, not flakiness. Console/network pull next.
- Asset Classes regression, sharpened: GET /api/lookup/node-classes = 47 rows, ALL company_id = acme (d59d449b-…), is_global=false on every row, none deleted-flag checked. So "grid now hides globals" is NOT the explanation — these are company-owned classes and the grid shows none. Mounting the page (rail click) fires NO API request at all (fetch/XHR hooked; only DevRev telemetry). Frontend-side, reproducible 4x on index-BOaMecwk.js. Candidate for a new ticket.

## Maintenance Program (/maintenance/program)  (ZP-4044 / ZP-4212)
- Page: banner "201 of 201 assets have not completed their condition assessment"; "Nothing unscheduled comes due in the next 90 days"; controls Search assets… / Group by Asset / Axis Months / Record completion / **Defer** / Filters / Export / full-screen; 2026 month grid; legend of service codes (AFDC, AFLP, C, CA, CTT, DEVI, IRT, NETA, PSU, QA…). "Showing 10 of 201 assets — the rest have no program and nothing on record in this window." -> 37
- ZP-4044 (new bundle): typing "transformer" in "Search assets…" fired ONE program API request carrying a query string (4,061 bytes back) and the grid went from 10 programmed assets to "No assets on this site have a maintenance program yet…" (none of the 10 are transformers). Server-side filter confirmed by behaviour; earlier direct API call showed `?q=transformer` narrowing 254 -> 2 by label OR class. -> 38
- ZP-4212 (QA-filed, open): "Defer" opens "Defer scheduled service — Moves the next due date — the commitment stays on the program": Asset / Service… / New due date (dd/mm/yyyy, "Stands until the service is performed on or after it") / Cancel / Defer. Dialog intact; the failing-submit raw-error path NOT re-executed (would need a deliberately failing defer on shared data). Status stays open in Jira. -> 39

## Sales -> Site Walks (/site-walks)  (ZP-4068 / ZP-4153)
- ZP-4068 (new bundle): registry of 48 walks shows a **JOURNAL** chip beside journal-mode walks (e.g. "14 augest abhiyant", 15 Sept 2026, Condition Assessment +1, 2 locations, Complete). "New Site Walk" dialog offers **Count** ("Tally assets by class and location. Quantities and pricing straight away.") vs **Journal** ("Talk and shoot as you go. The assets, condition and feeds are read out of it for you.") as two radio cards, Count preselected; Services* / Site / Walk name; Cancel / Start Walk. Verbatim match. -> 40, 41
- ZP-4068 Journal Review (/site-walks/5b85ca51-…, new bundle): header "Journal Review · Abhiyant · 14 augest abhiyant · Read 15/09/2026 · JOURNAL"; left rail QUESTIONS + JOURNAL ENTRIES per location ("Unnamed location — 3 entries · 2 photos"; "Unnamed location — Nothing yet"); centre = field-order timeline (17:55 NOTE typed "Hhhh"; 18:00 PHOTOS ×2 with thumbnails; 18:26 PHOTOS ×0); right = INTERPRETED MODEL "Nothing interpreted yet … Run Interpret to turn its entries into assets, quantities and condition." with list/graph toggle. -> 42
- ZP-4153: the locations rail is present (locations listed per entry group); add/rename/delete controls not surfaced in page text (may be hover/kebab). No interpreted unit exists on any acme journal walk (interpretation needs the pipeline runner, ZP-4067, not a web surface), so "a unit names the photos that show it" cannot be exercised here. Present-unexercised.
