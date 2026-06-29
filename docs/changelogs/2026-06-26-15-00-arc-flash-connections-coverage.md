# Arc Flash Readiness — Source/Target (Edit Asset) + Connection Details (Edit Connection) coverage

- **Date:** 2026-06-26
- **Prompt:** Arc Flash workflow — Source/Target Connections → select a (Node Bus) asset → Edit Asset
  modal → close via X; Connection Details → Busway readiness table → open a busway connection → Edit
  Connection → set Conductor Material + Length → Save Changes; verify progress.
- **Type:** `ArcFlashPage` extensions + new test class (`ArcFlashConnectionsTestNG`, 4 tests) + suite
  wiring. Complements the earlier `ArcFlashTestNG` (Engineering Mode / Asset Details / Asset Class).

---

## What the workflow does (discovered live, then asserted)

Driven live on site **"Android Qa Site1"** (Playwright MCP) before writing assertions:

- **Source/Target Connections** tab → a table (Asset, Asset Class, Needs Source, …). Clicking an asset
  row opens an **Edit Asset** modal (Asset Name*, Asset Class*, Location, Asset Photos tabs
  Profile/Nameplate/Schedule/Arc Flash Label) → closed via the **top-right X** icon button.
- **Connection Details** tab → a **"Busway Arc Flash Readiness"** section + a Busway connection table
  (Type, Source, Target, Conductor Material, Length (ft), Neutral Wire Size, Amperage of Busway,
  Phase A/B/C Wire Size). Clicking a connection row opens an **Edit Connection** modal:
  - **BASIC INFO**: Source Node, Target Node, Connection Type (pre-filled Autocompletes).
  - **CORE ATTRIBUTES** (required*): **Conductor Material** (MUI Autocomplete — Aluminum/Copper),
    **Length (ft)** (number), Neutral Wire Size, Amperage of Busway, Phase A/B/C Wire Size.
  - Setting Conductor Material + Length and clicking **Save Changes** persists (the modal closes — Save
    accepts partial required fields).

Both tables are virtualised **MuiDataGrid**s (with a coexisting hidden `tbody` table — handled).

## `ArcFlashPage` — new methods
`openFirstAssetEdit`, `isEditAssetModalOpen`, `editAssetHas`, `hasBuswayReadiness`,
`waitForBuswayReadiness`, `openFirstConnectionEdit`, `isEditConnectionModalOpen`, `editConnectionHas`,
`selectConductorMaterial`, `getConductorMaterialValue`, `enterLength`, `getLengthValue`,
`isSaveChangesEnabled`, `saveChanges`, `isAnyModalOpen`, `closeModal` (top-right X), plus
`waitForTableRows` / `visibleDataRows` / `openRowEditModal` (patient, retrying row-open).

## `ArcFlashConnectionsTestNG` (new, scoped to "Android Qa Site1")
- **AFC_01** — Source/Target: asset row → Edit Asset modal (Name/Class/Location/Photos) → close via X.
- **AFC_02** — Connection Details: Busway Arc Flash Readiness section + connection columns.
- **AFC_03** — connection row → Edit Connection (BASIC INFO + CORE ATTRIBUTES).
- **AFC_04** — set Conductor Material = Copper + Length = 50 → **Save Changes** (persists; modal closes).

## Suite wiring
- New `suite-arc-flash-connections.xml`.
- Added `ArcFlashConnectionsTestNG` to `suite-dashboard-bughunt.xml` (the **dashboard-bughunt** CI group,
  home to the other Arc Flash tests) — no dashboard-array changes. Bumped counts (128 → 132 suite;
  111 → 115 dashboard). Mapped the class → "Arc Flash Readiness" in `consolidated-report.py`.

## Validation
_Headed (no headless), `mvn test -DsuiteXmlFile=suite-arc-flash-connections.xml`, site "Android Qa Site1"._

**4/4 PASS — `Tests run: 4, Failures: 0, Errors: 0` (BUILD SUCCESS).**
```
PASSED: AFC_01_EditAssetModalFromSourceTarget (20s)
PASSED: AFC_02_ConnectionDetailsBuswayReadiness (9s)
PASSED: AFC_03_EditConnectionModalStructure (11s)
PASSED: AFC_04_SaveConnectionCoreAttributes (19s)   # Conductor Material=Copper, Length=50, Save Changes persisted
```

### Iterating to green — the timing lessons
The first runs flaked (4 → 2 → 1 → 0 failures) on **async tab-content loading**, all now fixed:
- After a tab switch the grid/section loads async — added `waitForTableRows()` / `waitForBuswayReadiness()`
  and made `openRowEditModal` wait for the modal's **content** (≥2 inputs) before returning.
- The first test ate the **heavy cold load** of the Source/Target "Source Connection Status" computation —
  `classSetup` now **warms all three tabs once** so no individual test pays that cost.
- `Save Changes` close-wait widened to 25s for slow saves.
