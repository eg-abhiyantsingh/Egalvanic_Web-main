# Asset creation — "Select or Create Location" modal (Building › Floor › Room cascade) coverage

- **Date:** 2026-06-25
- **Prompt:** Asset-creation workflow spec — BASIC INFO (Asset Name, QR Code, Asset Class) +
  the **Select Location** button → **"Select or Create Location"** modal (Building → Floor → Room).
- **Type:** New page-object methods (`AssetPage`) + new test class (`AssetLocationTestNG`, 6 active
  + 1 disabled diagnostic) + suite wiring.

---

## What the workflow does (discovered live, then asserted)

Driven live on site **"Android Qa Site1"** (Playwright MCP) before writing any assertion:

- Assets → **Create Asset** opens the **Add Asset** drawer. BASIC INFO has Asset Name*, QR Code,
  Asset Class*, and a **Location** area with a **Select Location** button (it becomes **Change**
  once a location is set).
- That button opens a MUI dialog titled **"Select or Create Location"** containing **three MUI
  `<Select>` dropdowns** in a strict cascade:
  - **Building** (always enabled) → enables **Floor** → enables **Room**. Floor/Room carry
    `aria-disabled=true` until their parent is chosen.
  - Each has a **New Building / New Floor / New Room** button (the "or Create" affordance).
  - A **"Selected Location: B›F›R"** live preview.
  - A confirm button (also labelled **Select Location**) that stays **disabled until all three are
    chosen**. Confirming closes the dialog; the drawer's Location then shows the path (e.g.
    `B1›F1›R1`) plus a **Change** button.
- **Key interaction nuance:** MUI `<Select>` opens its listbox only on a **trusted** click — a JS
  `.click()` silently fails. The page object drives it with Selenium `Actions`.

This modal was previously **not modelled** anywhere — the only existing coverage was two brittle
"(Partial)" tests in `AssetPart1TestNG` that clicked generic `li[role=option]` items and bailed with
a PASS if nothing matched. They never exercised the Building→Floor→Room cascade.

## `AssetPage` — new location-modal API
`openLocationModal()`, `isLocationModalOpen()`, `isLocationFieldEnabled(i)` (cascade gating),
`isLocationConfirmEnabled()`, `getLocationDropdownOptions(i)`, `chooseLocationOption(i, text)`,
`pickFirstLocationOption(i)`, `confirmLocationSelection()`, `cancelLocationModal()`,
`selectLocation(building, floor, room)`, `selectFirstAvailableLocation()` (site-agnostic),
`getSelectedLocationText()`, `isLocationSet()`, and `isCreateDrawerOpen()`. All scoped to the dialog
so they never collide with the drawer's identically-labelled "Select Location" button.

## `AssetLocationTestNG` (new, scoped to site "Android Qa Site1")
- **LOC_01** — Select Location opens the modal with the Building/Floor/Room fields + New* buttons + Cancel.
- **LOC_02** — **Cascade gating**: Floor disabled until Building, Room until Floor, confirm disabled
  until all three (drives the verified B1 → F1 → R1 chain, asserting enablement between each step).
- **LOC_03** — Building dropdown lists real buildings, including **B1**.
- **LOC_04** — Select B1 → F1 → R1, confirm → modal closes and the drawer Location shows the path.
- **LOC_05** — **Cancel** discards the selection (Location stays unset).
- **LOC_06** — **End-to-end**: create an asset *with* a location (name + QR + class + location +
  core attrs + cost) → submit → assert it **persists** to the grid.
- **LOC_00** — disabled DOM-discovery diagnostic.

## Suite wiring
- New `suite-asset-location.xml`.
- Added `AssetLocationTestNG` to `suite-asset-1-2.xml` (the existing **asset-1-2** CI group) so it runs
  in both parallel suites with **no** change to the dashboard's positional group arrays. Bumped the
  group's display name/count in `full-suite-dashboard.sh` (69 → 75) for accuracy.

## Validation
_Headed (no headless), `mvn test -DsuiteXmlFile=suite-asset-location.xml`, site "Android Qa Site1"._

**6/6 PASS — `Tests run: 6, Failures: 0, Errors: 0` (BUILD SUCCESS).**

```
site 'Android Qa Site1' selected=true
PASSED: LOC_01_LocationModalStructure (93s)
PASSED: LOC_02_CascadeGating (16s)
PASSED: LOC_03_BuildingOptions (13s)
PASSED: LOC_04_SelectAndApplyLocation (22s)
PASSED: LOC_05_CancelLeavesLocationUnset (15s)
PASSED: LOC_06_CreateAssetWithLocation (51s)
```

Independently verified in the live app that LOC_06 persists with the location applied — searching
the grid shows the created asset with its Building/Floor/Room columns populated, e.g.
`LocTest_20260625_141505 · Circuit Breaker · B1 · F1 · R1`.

### Notable findings while iterating to green (all fixed)
- **Site selection at scale:** the tenant has ~130 facilities in a virtualised dropdown;
  `BaseTest.selectSiteByName` (JS value-set) can't filter it. The test selects the site with **real
  keystrokes** (`ensureSite`), which narrows it to the exact match.
- **Grid search needs real keystrokes too:** `AssetPage.searchAsset` (JS value-set) does not trigger the
  grid's debounced server-side filter, so the post-create check was looking at an unfiltered grid.
  LOC_06 verifies persistence with a real-keystroke search (`searchGridFor`). _(The created asset was
  fine all along — the verification was the bug.)_
- **Contextual buttons:** only **New Building** shows on a fresh modal; **New Floor/New Room** render
  after a Building/Floor is chosen — LOC_01 asserts accordingly.
- **MUI Select = trusted click:** the cascade dropdowns open only on a real pointer click (Selenium
  `Actions`), not a JS `.click()`.
