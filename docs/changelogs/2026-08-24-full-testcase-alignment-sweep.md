# Full test-case alignment sweep — V1.36

**Date:** 2026-08-24 · **Prompt:** "try to check everything is proper update our all test case according to that"

Swept the whole test tree against the verified V1.36 frontend map. Every finding below was
confirmed by reading the code and, where it touched the app, by clicking through live QA.

## 1. Page objects: sidebar navigation made rail-aware (6 files)

`ConnectionPage`, `IssuePage`, `LocationPage`, `WorkOrderPage`, `AssetPage`, `ArcFlashPage` all
navigated by raw `<a>`-text loops. Under the two-level rail those match nothing whenever another
category is open — a **silent no-op**, not an error.

Worst shape was the "navigate away and back for fresh data" block: the away-click no-opped, the
back-click no-opped, and because `isOnXPage()` was still true the direct-URL fallback was skipped
too — so the refresh never happened while the method logged success. `LocationPage` even printed
"Navigated via JS `<a>` click" having navigated nowhere.

All now route through `NavCatalog`, and the away-hops target **same-category siblings**
(Work Orders → EMPs, the Site Data pages → Assets) so they stay inside the open panel.
`ArcFlashPage` and `AssetPage` additionally stop burning their 25–30s `elementToBeClickable`
waits on every cross-category entry.

## 2. Dead routes still being tested

| File | Was | Now |
|---|---|---|
| `DashboardBugTestNG` (4 uses) | `/equipment-insights` | `/equipment-designations` |
| `BugHuntPagesTestNG` BUG-024 | `/equipment-insights` | `/equipment-designations` |
| `ZP1997DOMPurifyXssTestNG` | `/z-university`, `/zuniversity`, `/help`, `/learn` | `/z-university` only |

`/equipment-insights` was the dangerous one: BUGD70 asserted
`pageText.contains("Equipment")`, which the sidebar's "Equipment Designations" link satisfies on
**every** route — so the test passed on a blank page. Assertion retightened to require a grid.

The Z-University probe was **inverted**: the real `/z-university` scores ~7 visible elements
until it hydrates (~9s, and it renders no `<main>`), while the dead `/help`, `/learn` and
`/zuniversity` shells score 59 each. The `bcount > 30` guard therefore *rejected the real page and
accepted a dead one*, then ran the DOMPurify assertions against nav chrome. Now probes the one
live route, waits 9s, and requires the page's own text.

## 3. Vacuous measurements

- `LoadTestNG.measureSidebarNav()` — text click no-opped, the wait loop spun its full 15s, and
  ~16s was reported as a "sidebar navigation time". Now expands the category first (outside the
  timer) and says explicitly when a nav never landed instead of returning the timeout as data.
- `LoadTestNG` rapid 4-page switch — expands Site Data once so the burst is real.
- `MonkeyTestNG` — `NAV_ITEMS` contained **"Dashboard"** and **"Jobs"**, neither of which exists
  in V1.36 ("Site Overview", "Work Orders"). Those hops matched nothing, so the monkey re-ran its
  health checks on the same page while counting a success. Breadth is the entire point of that
  suite. Labels corrected, list widened across categories, and hops go through
  `NavCatalog.routeForLabel()` (new) which returns null for a renamed/removed label instead of
  failing silently.

## 4. Presence checks that depended on where the driver stood

`AccountV135RegressionTestNG.sidebarHasText()` read `.MuiDrawer-root` innerText — i.e. only the
**open** panel. `sidebarHasText("Customers")` worked here purely because the panel auto-opens to
the current route's category. Replaced with route-based `NavCatalog.collectAllNavHrefs()` checks;
the helper (and the dead `legacyContract` text branch) removed. Also removed
`ReportingEngineV2TestNG.COMPANY_SETTINGS_NAV`, an unused locator for the gone "Settings" label.

## 5. Real defect fixed: Locations delete (3 tests, fail → pass)

`testNB_004_ValidInput`, `testNB_008_OptionalAccessNotes`, `testNB_009_SpecialCharsNotes` were
failing with `Delete button not found for location`. **Confirmed pre-existing** by reverting my
`LocationPage` change and re-running — all three still failed, so this was not caused by the
rewiring.

Root cause is the V1.36 Locations redesign. `/locations` is a master-detail tree with no Delete
control at all; the real flow is select node → `aria-label="Edit Location"` → dialog "Danger
Zone" → **"Delete Building"/"Delete Floor"/"Delete Room"**. All five existing strategies were
written for a grid UI; strategy 4 came closest but keyed on `svg[data-testid='EditIcon']` when the
live button has no icon testid and is labelled "Edit Location". Added that flow as Strategy 0.

Result: **3 fail → 3 pass**, log confirming
`Delete clicked via Edit Location -> Danger Zone ('Delete Building')`.

## 6. Suite wiring

`TabbedModulesSmokeTestNG` added to `testng-full.xml` — it existed but no suite ran it.

## Validation (run against live QA, not just compiled)

- `ConnectionTestNG` — **31/31 pass, 0 skipped**
- `LocationTestNG` 3 targeted tests — **3/3 pass** (were 3/3 fail)
- Full `LocationTestNG` + `IssueTestNG` — see run log
- All `testng*.xml` suites verified to reference classes that exist

## Note on scope

A parallel 6-agent workflow sweep was launched over the whole test tree; **all six agents died on
"out of usage credits"** and returned nothing. Its empty result is not evidence of a clean
codebase. Everything above came from direct greps and live checks instead.
