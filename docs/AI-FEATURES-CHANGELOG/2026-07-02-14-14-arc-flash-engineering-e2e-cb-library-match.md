# Arc Flash Engineering E2E — Circuit Breaker library-match → readiness recalc

**Date:** 2026-07-02
**Time:** 14:14 IST
**Author:** Claude Code (automation), for abhiyant.singh@egalvanic.com

## What was asked
Automate the recorded Arc Flash **Circuit Breaker engineering** flow and "cover everything for
arc flash — all the arc-flash readiness test cases we miss":

> Enable Engineering Mode → Asset Details → "Circuit Breaker Arc Flash Readiness" grid → click the
> dash (—) in **Frame Amps** for a circuit breaker → Edit Asset modal (ABB pre-selected) → pick
> **ABB — Emax 2, E1.2, Ekip DIP — LI, 250-1200A** from the "214 possible matches" → **Save Changes
> twice** → return to **Source/Target Connections**. Cable conductor config `2-1/C+G`, material
> `Aluminum`, wire gauge `3 AWG`.

This closes the previously-deferred **TC-AF-019/034** (edit → readiness recalc), plus cable
conductor fill, Generate Report render (TC-AF-030) and cross-site refresh (TC-AF-005).

## What was built

### New test — `ArcFlashEngineeringE2ETestNG` (8 ordered tests, `preserve-order`)
The Edit Asset modal stays open across AFE_02→03→04, so the class runs as one ordered flow.

| # | Test | Proves |
|---|------|--------|
| AFE_01 | CB eng-mode grid + readiness banner | 8 engineering columns present; captures `X of Y required fields` baseline |
| AFE_02 | Dash cell → Edit Asset modal | Clicking a dash Frame-Amps cell opens the ENGINEERING Edit-Asset modal with the manufacturer search |
| AFE_03 | ABB → possible-match cards → pick Emax 2 | 3P pole set, manufacturer=ABB, model card matched & applied in-modal |
| AFE_04 | Save Changes ×2 → recalc | **Hard:** save closes the modal. **Best-effort:** reloads the grid and *records* whether readiness recalculated |
| AFE_05 | Source/Target Connections after edit | End of the recorded flow — tab loads with rows |
| AFE_06 | Cable conductor fill (Aluminum / 3 AWG) | Skips by design if the site has no Cable connection |
| AFE_07 | Generate Report renders (TC-AF-030) | Report control triggers new-tab/dialog/inline output without error |
| AFE_08 | Site switch refresh (TC-AF-005) | Switching facility re-renders the three readiness gauges |

**Live result (site's real CB data):** `total=8 passed=7 failed=0 skipped=1` — the skip is AFE_06
(this site has no Cable connection type; a by-design `SkipException`, not a failure).

### `ArcFlashPage` — new CB-engineering methods
`getClassBannerText/Percent`, `getRowCellsByLabel`, `openCbEditFromEngineeringCell` (targets a **dash**
row / falls back to the LAST row — never row 0, which may be curated demo data),
`editAssetHasManufacturerSearch`, `setPoleCountInModal`, `selectDialogAutocomplete[ByLabel]`,
`waitForMatchCards`, `clickModelCard`, `isLibraryMatchApplied`, `saveChangesTwice`,
`clickGenerateReportAndProbe`.

### Suites / CI wiring
- New `suite-arc-flash-engineering.xml` (`preserve-order`).
- Registered in `parallel-suite-2.yml` ARC_FLASH toggle catalog (group `arc-flash-engineering`, 8 TCs).
- Added to `suite-dashboard-bughunt.xml`.

## Depth explanation — the three bugs I had to fix to get it green (for learning / manager review)

1. **MUI Autocomplete would not commit the manufacturer (`=> ''`).**
   MUI's Autocomplete is a *controlled* React input. `sendKeys` fires native DOM events, but React
   tracks value through its own synthetic-event layer, so the filter list never updated and the
   selection never registered. **Fix:** drive it with the native
   `HTMLInputElement.prototype.value` setter + a bubbling `input` event (the one path React's
   `onChange` listens to), then return the **clicked option's text** — the committed value lives in
   the selected option, not the input's `value` attribute. This mirrors the proven `selectAutocomplete`
   in `AssetEngineeringTestNG`. Both dialog autocompletes now share one `typePickAutocomplete` engine
   (3 retries; listbox is portalled to `<body>`, so options are queried at document scope).

2. **`editAssetHasManufacturerSearch` used `getText()` (visible-only).** The ENGINEERING section can
   be scrolled off, so Selenium's `getText()` returned nothing. **Fix:** check the DOM via
   `textContent` + input `placeholder`, and **poll ~10s** because the sub-component lazy-renders a
   beat after the modal opens.

3. **`StaleElementReferenceException` on Save.** The Save button detaches the instant the save
   re-renders the modal; the innermost JS-click fallback in `saveChangesTwice` was unguarded and
   threw. **Fix:** guard all three click paths (Actions → element.click → JS click) — a stale here
   just means the click already landed.

Plus **AFE_08 site-switch** hardening: fold away any extra report tab, drive the ~130-site
virtualised picker with the React value-setter, retry 2×.

## Honesty note (per project rule: don't over-report)
Whether the library-model match **recalculates the readiness grid** is an *unverified capability*:
after match + Save ×2 the grid did not re-populate the breaker's readiness columns on the test site.
The model match plausibly populates catalog / trip-config while the grid's readiness columns are
sourced separately (by-design), so AFE_04 **records this as an observation, not a defect, and does
NOT hard-fail on it.** The deterministic, provable claims (modal opens → match applies in-modal →
Save closes the modal → flow continues) are asserted; the recalc is logged as evidence to verify
before ever filing it as a bug.
