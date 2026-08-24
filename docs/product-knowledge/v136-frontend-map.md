# V1.36 frontend map — complete click-through inventory

**Captured 2026-08-24** on https://acme.qa.egalvanic.ai (V1.36) by clicking through the real
UI — sidebar rail → module link → every tab → first grid row → detail tabs. No URL-jumping,
no API shortcuts. Everything below renders; deviations are called out.

## Rail (7 items, top to bottom)

| Rail item | Opens |
|---|---|
| **Logo button** (`img alt="Egalvanic — Dashboards"`) | Dashboards panel + navigates to /dashboard. The ONLY sidebar way back to dashboards from a module page. |
| Site Data | DASHBOARDS: Condition Assessment · DATA: Assets, Connections, Locations, Issues, Tasks · OTHER: Attachments |
| Operations | PLANNING: EMPs, Planned Work, Scheduling · OPS: Work Orders |
| Engineering | DIAGRAM: SLDs · ARC FLASH/COORDINATION: Arc Flash Readiness, Panel Schedules · LIBRARY: Equipment Designations |
| Sales | PIPELINE: Site Walks, Quotes · DATA: Customers |
| Builder | Reports, Services, Forms |
| Admin | Setup · SALES: Labor, Materials · ACCOUNT: Users, Offices · OPERATIONS: PM Plans, Test Equipment, Classes · OTHER: Audit Log, Legacy Procedures, Legacy Forms |

Below the rail: **Updates** (Beamer changelog iframe, app.getbeamer.com) and the avatar
(account popover: read-only Roles text, Edit Company, Email Preferences, English/Français,
Sign Out). A DevRev "plug" support widget also loads.

## List-page tabs (`role="tab"`, every one clicked live)

| Route | Tabs | Notes |
|---|---|---|
| /pm-readiness | Overview, Asset Details | Asset Details grid: Asset/Class/Status/Subtype/Crit./Env./Maint./COM/PM Plan |
| /arc-flash | Overview, Asset Details, Source/Target Connections, Connection Details | each tab has its own grid |
| /customers | Accounts, Sites | Sites tab: "Create Site", cols Site Name/Account/Access Complexity/Address/City/State/Country |
| /labor | Rates, Types, Unions | + Create Labor Rate, Bulk Ops, AI Setup |
| /materials | Material Library, Material Presets, Material Types, Material Units | + Create Material, Bulk Ops, AI Setup |
| /test-equipment | Test Equipment Library, Equipment | Equipment adds Serial/Calibration Date/Due cols |
| /classes | Asset Classes, Connection Classes, Issue Classes | + Create Asset Class, Bulk Ops |

Tab badges vary with data ("Contacts 2") — match startsWith, not equals.
`NavCatalog.TABS` / `tabsFor()` / `clickTab()` encode all of this.

## Detail-page tabs (first row clicked on each grid)

| Detail | Route | Tabs |
|---|---|---|
| Asset | /assets/{id} | Basic Info, Engineering, Inspections, Issues, Schedule, Connections, Photos, Attachments (8) |
| Work Order | /sessions/{id} | Assets, Tasks, Forms, Issues, IR Photos, Attachments (6) |
| Account | /accounts/{id} | Details, Internal Team, Contacts, Quotes, Sites, Notes (6) |
| Issue | /issues/{id} | Details, Class Details, Photos, Status History (4) |
| **Quote** | **/plans/{id}** | none |
| **EMP** | **/plans/{id}** | none |
| Site Walk | /site-walks/{id} | none |

**/plans/{id} is the V1.36 quote/EMP editor.** The legacy `/quotes/{id}` renders
**"Quote not found"** even for a live id (verified with the same UUID both ways) — dead, not
redirected. The Java trap: `"/plans/x".contains("/plan/")` is **false** ('s' follows "plan"),
which is why `PlanningPage.isOnEditorPage()` was permanently false until fixed.

## Sidebar sub-links (render only while their module is open)

- **Quotes**: All → /opportunities · Draft → `?status=draft` · Pending Response →
  `?status=pendingResponse` · **Closed Won → `?status=accepted`** · **Closed Lost →
  `?status=rejected`** · Cancelled → `?status=cancelled`. Labels ≠ params for the closed pair.
- **Planned Work**: All · Overdue → `?bucket=overdue` · Due this month → `?bucket=due_30` ·
  Due This Quarter → `?bucket=due_quarter` · Due this year → `?bucket=due_year`.

## Non-tab switch surfaces

- **/reporting/builder**: "Report Builder | Branding" — plain button toggle, not tabs.
  Branding renders an HTML stylesheet editor with Regenerate Default / Save.
- **/scheduling**: calendar with toolbar Today/Back/Next + Quarter/Month/Week/Day, weekday
  columns (Sun…Sat). No grid.
- **/locations**: master-detail TREE ("Select a location to view details"), no grid, no
  pagination — grid-based sweeps must skip it.
- **/admin-dashboard** (Setup): action-items feed (e.g. "X has no sites/contacts"), no tabs.
- **/sites** deep link → redirects to `/customers?tab=sites` (works, with Create Site).

## Row-click behaviors that are NOT navigations

- **Issues** row → navigates to /issues/{id} (tabbed page).
- **Tasks** row → (no rows on ZTest_28_07 at capture time — re-verify with data).
- **Planned Work** row → stays on /planned-work (opens in place).

## Product regressions found during this walk

1. `/quotes/{id}` → "Quote not found" for a valid plan id — legacy quote deep links are broken
   for users' old bookmarks (no redirect to /plans/{id}).

Not a bug (owner ruling 2026-08-24): `/accounts/goals` API-500s on load, but that legacy path
is **not in use** — nothing links to it and the module lives at /goals — so it is not to be
ticketed. Tests must simply never target it.

## Coverage backing this map

`NavCatalogSelfValidation` (main(), excluded from suites) proves the rail mechanics + all 20
list tabs; `TabbedModulesSmokeTestNG` (18 tests, green 2026-08-24) walks every catalogued tab,
all quote-status params, all planned-work buckets, the Branding toggle, and the Scheduling
toolbar on every run.
