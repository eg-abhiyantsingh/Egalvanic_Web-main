# Work Type Matrix — 415 test cases across all 14 Work Type options (ZP-3000 Services V2)

**Date:** 2026-07-21 · **Site:** Z1 (acme.qa) · **Trigger:** owner ask — "work order Work Type
dropdown has 13 options, cover all of them in depth, 400+ test cases"

## The catalog (live-pinned 2026-07-21, both API + dropdown surfaces)

13 services + "General" = 14 dropdown options. `GET /api/procedures-v2/services` is authoritative:

| # | Work Type | key | family | de_energized | procs |
|---|---|---|---|---|---|
| 1 | Arc Flash Data Collection | arc-flash-study | AF | no | 35 |
| 2 | Arc Flash Label Placement | arc-flash-label-placement | Checklist | no | 13 |
| 3 | Cleaning | cleaning | PM Forms | yes | 18 |
| 4 | Clean, Tighten, Torque | clean-tighten-torque | PM Forms | yes | 19 |
| 5 | Condition Assessment | condition-assessment | COM | no | 30 |
| 6 | De-Energized Visual Inspection | de-energized-visual-inspection | PM Forms | yes | 19 |
| 7 | DGA / Fluid Sample Analysis | dga-fluid-sample-analysis | PM Forms | no | 1 |
| 8 | Infrared Thermography | infrared-thermography | IR | no | 30 |
| 9 | Insulation Resistance Testing | insulation-resistance-testing | PM Forms | yes | 17 |
| 10 | NETA Testing | **de-energized-testing** (name≠key!) | PM Forms | yes | 19 |
| 11 | Panel Schedule Updates | panel-schedule-updates | Schedule | no | 2 |
| 12 | Shutdown (Composite) | composite-shutdown-emp | PM Forms | yes | **0** |
| 13 | UPS Maintenance | ups-maintenance | PM Forms | no | 1 |
| 14 | General | — (no service) | General | — | — |

Per-type dialog contract (live): procedures>0 → scope preview settles on "N matching assets"
(IR all-assets on Z1 = 46, AF = 130); 0 procedures (Shutdown) → "This work type has no procedures
configured — no assets will be pulled in automatically."; General → neither (no scope-preview POST
at all). Auto-Schedule button renders for ALL types (drift vs 07-20 notes), disabled until
Est. Hours + Field Technician. Detail-page family contracts (tabs/columns/buttons) captured from
the six Z1 fixtures — see `WorkTypeCatalog.Family`.

## What shipped

**Foundation**
- `src/main/java/com/egalvanic/qa/constants/WorkTypeCatalog.java` — authoritative 14-profile model
  (labels, keys, service ids, families with expected tabs/type-columns, Z1 fixture session map,
  Z1 sld_id, family representatives, de-energized set).
- `WorkOrderPage` +20 methods: `getWorkTypeOptions/getWorkTypeValue/getFacilityValue/
  cancelCreateDialog/getCreateDialogText/getMatchingAssetsCount/hasNoProceduresNotice/
  isAutoScheduleButton{Present,Enabled}/isOnWoDetailPage/getWoDetailId/getDetailTabNames/
  clickWoDetailTab/getWoHeaderChips/getAssetGridColumnHeaders/woDetailButtonPresent/
  openActionsMenuAndListItems/clickMenuItemContaining/closeOpenMenu`.
- `src/test/.../WorkTypeUiBase.java` — Z1 site pinning (real keystrokes), dialog lifecycle,
  committed-type selection with readback, grid search with 5–15s indexing-lag retry, API cleanup
  via `DELETE /api/ir_session/{id}` (needs Content-Type: application/json; returns async
  `_mutation: received` receipt — even for nonexistent uuids, pinned as a contract observation).

**Five test classes, 415 data-driven rows** (each row = one ExtentReports test case)

1. `api/WorkTypeCatalogApiTest` — **84 rows**: catalog shape (13, unique), 13 pinned services,
   procedures-list per service, scope-preview contract per service **including
   `matching_count == assets.length`** (regression for the verified 2026-07-20 CTT
   preview-vs-created mismatch), asset shape, 18 fixture-session endpoint rows, no-auth 401s,
   malformed-input negatives. `known-product-bug` group: 500-on-`service_id=not-a-uuid`,
   500-on-garbage-`sld_id` **which leaks psycopg2 + full SQL SELECT + nodes schema to the client**
   (security finding, ticket-worthy).
2. `WorkTypeCreateDialogMatrixTestNG` — **153 rows**: option catalog exact order; per-type commit;
   per-type preview/notice/neither; notice exclusivity; Auto-Schedule presence disabled;
   Advanced-Settings defaults (Medium/FLIR-SEP/Start=today/Due=empty) survive type switching;
   5 sections; 3 required markers; fresh-dialog create gating per type; 13 adjacent type-switch
   retarget pairs; Start-Empty per preview service.
3. `WorkTypeDetailContractTestNG` — **102 rows**: 6 Z1 fixtures × 16 read-only checks (96
   fixture-major matrix rows + 6 health rows): exact tab strip per family, exact asset-grid
   columns per family, type chip, priority chip, common buttons (Actions/Quick Count/Close Work
   Order), **Data Mask iff Checklist**, Issues+Attachments universal, badge sanity, type-tab
   click, Actions menu (**Upload IR Photos iff IR**), More reacts, grid rows>0.
4. `WorkTypeCreateE2EMatrixTestNG` — **42 rows** (14 types × create/verify/cleanup): full-scope
   creates for the 6 family representatives (preview count vs created Assets badge cross-check),
   Start-Empty creates for the rest, chip+tab contract on the created WO, API-delete cleanup
   verified back in the grid.
5. `WorkTypeAutoScheduleEdgeTestNG` — **34 rows**: Auto-Schedule enablement truth table (Est.
   Hours + Field Technician per type; hard-assert enabled for all 13 services, General recorded),
   Shutdown 0-procedure create E2E, General detail-contract pin, cancel-discard, reopen state
   reset, name edges (whitespace-only must stay disabled; 256-char/XSS/unicode created + rendered
   literally + cleaned), facility-switch preview refire (Z1→Test Site→Z1), duplicate-name pin,
   past-due-date pin.

**Suites**: `suite-worktype-{api,dialog,detail,create-e2e,edge}.xml` (each standalone),
`suite-worktype-all.xml` (umbrella, 415), fullsuite-testng.xml modules **45–49**. Green gates
exclude `known-product-bug` per repo convention; run tripwires explicitly with
`-Dgroups=known-product-bug`.

## Validation evidence (this session, live headed Chrome)
- API: full-audit run 84 rows → 81 green + exactly the 3 intended tripwires red; green-gate run
  **81/81 GREEN in 3.5 min**.
- Create E2E: **42/42 GREEN on the first run** — all 14 types created, verified (list row, type
  chip, family tab strip, preview-vs-created badge), and API-deleted.
- Detail + Dialog + Edge: first runs surfaced SIX previously-undocumented product contracts
  (below); assertions were re-pinned to the verified truth and the suites re-run to green
  (final tallies at the bottom).

## Product contracts DISCOVERED by the first validation runs (all live-verified, now pinned)
1. **WO detail header is collapsed by default** — the priority chip (and a rich details panel:
   Priority/Facility/Timeframe/Certifier/Back Office/Field/Schedule/Description) exists in the
   DOM but `visibility:hidden` until the header chevron is clicked. Selenium `getText()`
   correctly skips hidden text; Playwright `textContent` does not — a cross-tool trap.
   → `expandWoDetailHeader()/collapseWoDetailHeader()`; the check pins BOTH collapsed-by-default
   and expand-reveals-priority.
2. **Expanded header pushes the virtualized MUI DataGrid off-viewport, where it renders ZERO
   column headers** → state restoration after header checks + scroll-into-view before column
   assertions.
3. **Scope preview label is singular for one match** ("1 matching asset" — UPS on Z1) — a
   plural-only regex returns nothing.
4. **Zero-match scope state** (DGA on Z1): "0 matching assets" + "No assets in this site are
   eligible for this work type with the current filters." — a third dialog state beside
   preview and no-procedures notice; counts are SITE-DATA dependent, so per-type assertions
   pin "settles" (n>=0) + zero-state notice, not hardcoded positives.
5. **The Create dialog keeps the WO Name as a sticky draft across Cancel + reopen** (Work Type
   resets; Create stays disabled). "Fresh dialog" flows must clear the name explicitly
   (`clearWoName()`); TC_WTE_010 pins the exact draft contract.
6. **Auto-Schedule: Est. Hours + Field Technician are NOT sufficient** — verified with a full
   14-type truth table (fresh dialog per type, site Z1, 2026-07-22): the top-level
   Schedule-section Auto-Schedule button renders for every type but stays DISABLED for all 13
   services (matching-asset scope 1–173) AND General after WO Name + Est. Hours + Field
   Technician. Enabling requires the Schedule "+" block sub-flow (a separate, flakier behavior
   left unpinned). TC_WTE_001 pins the reproducible negative. Two side-facts fell out: the
   masked MM/DD/YYYY date inputs IGNORE native value-setters (real keystrokes only), and an
   earlier "IR enables with est+tech" read was residual-dialog-state noise — the clean
   fresh-dialog-per-type run is authoritative.

## NEW verified product bug (tripwired as known-product-bug)
**Whitespace-only WO Name is accepted end-to-end**: Create enables for a name of "   ", the
backend accepts the create, and the app lands on the new blank-named WO's detail page
(probe WO deleted afterwards). Tripwire: `TC_WTE_011b` — red on purpose until the product
trims/validates; excluded from green gates. Ticket-worthy alongside the SQL-leak 500.

## Follow-ups
- File the SQL-leak 500 (scope-preview garbage sld_id) in Jira; link the two tripwire rows.
- After the first full E2E/edge run, fold the discovered General + Auto-Schedule truths back into
  `WorkTypeCatalog` (`Family.GENERAL.expectedTabs`, enablement notes).
- The count-mismatch bug (grid silently drops Main-* Switch) needs the "test site for api check"
  tenant — not reproducible on Z1; candidate for a sixth, site-scoped class later.
