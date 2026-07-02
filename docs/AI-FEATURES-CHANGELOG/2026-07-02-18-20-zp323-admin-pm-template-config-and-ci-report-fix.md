# ZP-323 Admin — PM template config automation + CI report-pipeline & headless-viewport fixes

**Date:** 2026-07-02
**Time:** 18:20 IST
**Author:** Claude Code (automation), for abhiyant.singh@egalvanic.com

## What was asked
1. (Earlier in session) "I can't see any client or details report" on Parallel Suite 2 run 28584446985.
2. Jira **ZP-323** (Epic): *QA Automation — Admin PM [ADMIN] — PM template config. Scope: Settings
   sub-tab, PM template config. Status: NOT YET AUTOMATED.*

## Part 1 — Why the CI reports were missing (and the fix)

**Root cause:** every Parallel Suite 2 matrix job runs `.github/scripts/full-suite-dashboard.sh`,
which maps `group → suite XML` via its own hardcoded arrays. The 8 arc-flash groups existed in the
workflow *catalog* but not in the *script*, so each job printed `Unknown group: arc-flash-*`, exited
in ~1s (non-blocking → job still green), ran **0 tests**, and uploaded no `reports-s2-*` artifacts.
The summary job then aggregated 0 tests and crashed on the missing consolidated report — that crash
was the visible symptom.

**Fix (commit 6bea378):** added all 11 missing catalog groups to the script (8 arc-flash +
`api-rest-contract`, `api-network`, `doc-inspired` which had the same latent bug), verified every
catalog group resolves offline.

**Proof:** verification run 28588272715 (arc_flash only) — all 8 groups ran real tests and produced
`reports-s2-*` (detailed report + screenshots per group), `consolidated-client-report-suite2`,
`consolidated-detailed-report-suite2`, and the after-rerun variants. Overall conclusion: success.

### The CI failures that surfaced once tests actually ran
After-rerun consolidated: **131 total / 116 passed / 13 failed / 2 skipped (88.5%)**.
Failure analysis (from the group report artifacts): AFE_01 failed with
`Headers: [Label, Poles, Frame Amps, Type, Manufacturer, Library row]` — only 6 of 8 columns.
**Root cause: `--headless=new` + `maximize()` leaves a ~800×600 window, and MUI DataGrids
virtualize columns horizontally** — right-side columns (Sensor/Plug Amps, Phase A/B/C Wire Size,
Amperage of Busway, Indirect Source, Status, Source Info) never enter the DOM on CI. That single
viewport issue explains ~10 of the 13 failures across grid-matrix/connections/engineering groups
(the rest are the documented role-recompute/XHR-settle slowness, worse on shared CI runners).

**Fix (this commit):** `BaseTest` now forces `--window-size=1920,1080` (plus an explicit
`setSize`) whenever `-Dheadless=true` — CI gets the same desktop viewport as local headed runs.

## Part 2 — ZP-323: Admin — PM template config (NEW automation)

### Where the feature actually lives (live-explored, since nothing existed in the repo)
Left-nav **Settings → `/admin`** → section switcher **Sites | Users | Classes | PM** → the PM
section renders breadcrumb `Settings | PM | Offices`:
- **Offices table**: columns *Name* + *Default Language*, search `"Search Offices..."`,
  MUI pagination (6 pre-existing offices).
- **Row click → Edit Office** dialog: prefilled Name*, Default Language select, **Delete/Cancel/Save**.
- **Add Office → Create Office** dialog: Name* (required — **Save disabled while empty**),
  Default Language (options: `Use login language` | `English` | `Français`), helper text:
  *"When set, this language will be used as the default for all sites, accounts, and users in this office"*.

### New code
- **`AdminPmSettingsPage`** (page object): PM-section navigation, table/pagination/search readers,
  Create/Edit dialog drivers (React value-setter for the controlled Name input, trusted `mousedown`
  for the MUI language select), Save/Cancel/Delete+confirm, row-wait helpers.
- **`AdminPmSettingsTestNG`** (12 TCs, ordered): nav+columns render → pagination reconciles →
  search filter+clear → create-validation (Save gating) → language options+helper text →
  cancel-no-persist → **create** → edit-prefill+buttons → **rename** → **language change (Français)**
  → **delete (+totals reconcile)** → section-switch roundtrip.
- **Data policy:** mutates only its own `AutoQA_PM_*` offices; `@AfterClass` sweeps any stale
  `AutoQA_PM_*` from crashed runs (proven — it cleaned run 1's orphan). The 6 real offices are read-only.

### Result (live QA, headed)
`total=12 passed=12 failed=0 skipped=0` — Detailed Report "Admin — PM (0 failures)".
Iteration bugs fixed on the way: (1) office rows render as BOTH plain `<table>` and DataGrid
markup — row readers now match either shape; (2) rows load async a beat after the Add Office
button → added `waitForOfficeRows` poll (same lesson as every other module).

### CI wiring (both places, per today's lesson)
- `suite-admin-pm.xml` (preserve-order).
- Parallel Suite 2: new **`admin_pm`** toggle (default off) + env + catalog entry
  (`group: admin-pm`, 12 TCs, stagger 102).
- `full-suite-dashboard.sh`: `admin-pm` group added to all four arrays + index map (verified offline).

## Depth explanation (for learning / manager review)
- **Config drift kills silently:** the group list lived in two places (workflow catalog + shell
  script). The unknown-group exit was non-blocking, so 8 green jobs ran zero tests. The guard that
  caught it was the summary job's result-JSON aggregation (`status: error, total: 0`) — evidence
  that "green" must be backed by counted results.
- **Headless ≠ headed:** virtualized MUI grids make column presence a function of viewport width.
  Any column-presence assertion is only meaningful at a pinned window size — CI now pins 1920×1080.
- **"PM template config" semantics:** the PM admin area configures per-office language templates
  (the helper text confirms propagation to sites/accounts/users). The epic's "Settings sub-tab" =
  the `/admin` Settings page's PM section. Test PM_05 asserts the exact option set and helper text
  so a product change to the template semantics will be caught.
