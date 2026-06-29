# Arc Flash Readiness coverage — Engineering Mode → Asset Details → Asset Class filter

- **Date:** 2026-06-26
- **Prompt:** Arc Flash workflow — go to /arc-flash, enable Engineering Mode (top-right toggle), switch to
  the Asset Details tab, verify Engineering Mode stays on, then filter the table by Asset Class.
- **Type:** New page object (`ArcFlashPage`) + new test class (`ArcFlashTestNG`, 6 tests) + suite wiring.
  Arc Flash previously had only a page-load smoke + a dashboard-percentage check — no coverage of the
  Engineering Mode / Asset Details / Asset Class filter workflow.

---

## What the workflow does (discovered live, then asserted)

Driven live on site **"Android Qa Site1"** (Playwright MCP) before writing assertions:

- **/arc-flash** shows three circular readiness indicators — **Asset Details**, **Source/Target
  Connections**, **Connection Details** — and a tab strip with those three plus **Overview**.
- **Engineering Mode** is a MUI `<Switch>` in a FormControlLabel (top-right by the profile). Enabling it
  reveals extra table columns, and it **persists across tab switches** (verified).
- The **Asset Details** tab shows a filterable table — **Label · Interrupting Rating · Ampere Rating ·
  Mains Type · Voltage · % Completion** (+ eng-mode columns: Class, Designation, Status, Approved,
  Actions) — with an **Asset Class** filter above it: a MUI `<Select>` (default **ATS**) whose 21 options
  are the site's asset classes (ATS, Battery, Busway, Circuit Breaker, Disconnect Switch, Fuse,
  Generator, Motor Starter, Relay, Switchboard, Transformer, …). Selecting a class filters the table.
- **Interaction nuance:** the Asset Class MUI Select opens only on a **trusted** click — driven with
  Selenium `Actions`, not a JS click.

## `ArcFlashPage` (new page object)
`navigateToArcFlash`, `isLoaded`/`waitLoaded`, `hasProgressIndicator`, `hasEngineeringModeToggle`,
`isEngineeringModeOn`, `setEngineeringMode(on)`, `clickTab`/`getActiveTab`, `hasAssetClassFilter`,
`getAssetClassValue`, `getAssetClassOptions`, `selectAssetClass`, `getColumnHeaders`/`hasColumn`,
`getRowCount`.

## `ArcFlashTestNG` (new, scoped to "Android Qa Site1")
- **AF_01** — dashboard + tab strip render (Overview / Asset Details / Source-Target / Connection Details) + Engineering Mode toggle present.
- **AF_02** — Engineering Mode can be enabled.
- **AF_03** — switching to Asset Details **keeps Engineering Mode enabled** (the spec's verify step).
- **AF_04** — Asset Class filter lists the classes (incl ATS + Circuit Breaker).
- **AF_05** — Asset Details table exposes Label / Interrupting Rating / Ampere Rating / Mains Type / Voltage.
- **AF_06** — **full workflow**: /arc-flash → enable Engineering Mode → Asset Details → verify it persists → **filter by Asset Class (Circuit Breaker)** → asserts the filter applied + table still rendered.

## Suite wiring
- New `suite-arc-flash.xml`.
- Added `ArcFlashTestNG` to `suite-dashboard-bughunt.xml` (the **dashboard-bughunt** CI group — already
  home to the Arc Flash dashboard checks + New-Modules smoke) so it runs in the parallel suite with **no**
  dashboard-array changes. Bumped the group's display name/count (122 → 128 suite; 105 → 111 dashboard).
- Mapped `ArcFlashTestNG → "Arc Flash Readiness"` in `consolidated-report.py` so the client summary files
  it under its own module rather than "Other".

## Validation
_Headed (no headless), `mvn test -DsuiteXmlFile=suite-arc-flash.xml`, site "Android Qa Site1"._

**6/6 PASS on the first run — `Tests run: 6, Failures: 0, Errors: 0` (BUILD SUCCESS).**

```
site 'Android Qa Site1' selected=true
PASSED: AF_01_DashboardAndTabs (1s)
PASSED: AF_02_EnableEngineeringMode (1s)            # Engineering Mode set to true (now=true)
PASSED: AF_03_EngineeringModePersistsOnAssetDetails (2s)
PASSED: AF_04_AssetClassOptions (2s)
PASSED: AF_05_AssetDetailsColumns (7s)
PASSED: AF_06_FilterByAssetClassWorkflow (14s)      # Selected Asset Class: Circuit Breaker
```
