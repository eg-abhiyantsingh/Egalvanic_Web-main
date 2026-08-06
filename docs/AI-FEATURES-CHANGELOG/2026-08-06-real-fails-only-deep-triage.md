# "Make sure a fail is a real fail" — full-run deep triage, every failure root-caused

**Date:** 2026-08-05 → 08-06
**Prompt:** "full automation should be more clear and better, not false fake fail. Test cases should be readable. Make sure fails are real fails — go in depth before concluding."

## Method (the discipline, not just the result)
Ran the whole suite on the fixed framework and treated **every** red as unproven until it was: (1) traced in the run log, (2) reproduced on the live app in a separate browser, and where possible (3) checked against the **API** to remove UI noise. Nothing was labelled "real bug" on an assertion message alone.

## Result: 1460 pass / 56 fail / 46 skip at the time of writing (last class still grinding)
Baseline for comparison — the 2026-08-04 run: **1441 / 78 / 45** with the *same* framework minus these fixes.

**Verified real product findings: 2** (plus 2 performance tripwires held as "likely-real, needs harness-independent measurement").
**Everything else was test-side — and every one is now fixed at source.**

## Real product findings (evidence attached)
| Finding | Evidence |
|---|---|
| **`/goals` crashes for every role** | "Application Error" boundary; console `TypeError: Cannot read properties of undefined (reading 'length')`; on-screen error ID `5daf74a67017`. Reproduced live under Super Admin AND via the test's Admin-role retry. The new classifier auto-labels it `REAL_BUG` (95%). **File to ZP.** |
| **No login rate-limiting (ZP-2025)** | 10 consecutive failed logins → `401 ×10`, never a 429/423/403. Already tracked. |
| *Heap +217 MB across 3 navigations* (threshold 50 MB) | Consistent across runs (191 MB → 217 MB). Held as **likely-real**; Selenium/CDP inflates heap, so it needs a harness-independent measurement before filing. |
| *`/api/*` >5 s on /assets load* | Consistent; matches the known slow-endpoint area. |

## False failures found and fixed (the "fake fails")
| Cluster | # | What it looked like | What was actually true |
|---|---|---|---|
| Issues search/delete | 13 | "could not locate the search input" | **All 13 stranded on ONE detail page** `/issues/81eca474-…`, which has no search box. `contains("/issues")` treated a detail page as the list. Fixed: list-vs-detail predicate + `searchIssues()` self-heals back to the list. |
| GEN asset edits | 8 | "Ampere Rating should be visible after save" | Values **did** save. V1.36 moved them to the detail **Engineering** tab, whose label carries a count badge ("Engineering 5") — my earlier exact-match tab flip missed it. Fixed with `starts-with`. |
| Stale-element flakes (DS/FUSE/PB/TRF/UPS) | 10 | `StaleElementReferenceException` | MUI DataGrid + edit drawer re-render mid-interaction. Fixed with retry wrappers at **both** throw sites (`openEditForAssetClass`, `editTextField`) in AssetPart2/3/4/5. |
| `TC_BH_31` localStorage corruption | 1 | "BUG: app didn't recover — DOM collapsed to 44 elements" | **Disproven live.** Corrupt all 16 non-auth keys → reload → the app renders a working **login form** (stable 44 elements): graceful session invalidation, not a crash. Test now accepts re-auth as recovery. |
| `Opp29` case-insensitive search | 1 | "Search must be case-insensitive (lower=6, upper=9)" | **API proves the product is correct**: `POST /quotes/v2` with `test`/`TEST`/`Test` all return **80**. The UI counted rows mid-debounce. Fixed with settled-count polling. |
| `WTD_001` pinned work-type catalog | 1 | "must offer EXACTLY the 14 catalog options… found 16" | The tenant now has **15 services, two hand-made** ("abhiyant Preventive", "abhiyant service corrective"). Someone's fixture, not a product change. Fixed to a subset+relative-order contract: a *missing canonical* type still fails loudly. |
| `TC_CWO_003` create flow | 1 | "create flow crashed: waiting for Facility field" | The /sessions list has its own "Select facility" **filter**; when the modal opens MUI marks the background `inert`, so that input can never be "visible" → 25 s timeout. Fixed by scoping the locator to the dialog. |
| Dashboard contract (CriticalPath, NewModulesSmoke) | 4 | "Should find at least 4 KPI cards, found 3" / "Opportunities Value" missing | V1.36 KPI row is **3 cards** (TOTAL ASSETS / ACTIVE WORK ORDERS / EQUIPMENT AT RISK); the May-2026 "Opportunities Value" card was **removed**. Expectations updated to the live contract. |
| `Opp02` grid columns | 1 | "Grid must have a Name column" | Column renamed **Name → Opportunity** in V1.36. Fixed. |
| `TaskTestNG` search | 1 | "Search for 'T1' should return results" | Hardcoded magic token; the site has 8 tasks, **none** named T1. Rewritten to take its search token **from the grid**, so it tests the feature, not the fixture. |
| `testCreateWorkOrder` | 1 | "WO not found in list after creation" | **Self-inflicted:** my own `selectTestSite()` /assets hop didn't navigate back, stranding mid-test recovery on /assets. Fixed — the route is now captured and restored on all 5 exit paths. |

## Readability improvements (so a red explains itself)
- **Honest classifier** (`SmartBugDetector`): new `TEST_OR_DATA_ISSUE` verdict. An assertion mismatch with **no** corroboration (no console errors, no crash page, no denial) is no longer stamped `[Bug | HIGH] REAL_BUG 75%` — it reads `[Test/Data (verify before filing) | LOW]` and says *reproduce manually first*. Page-state rules now name the cause outright: session-died-to-login, crash boundary (auto-extracts the error ID), Access-Denied-under-wrong-role, the `/sessions/{id}` create-redirect trap, and "asserted while a spinner was still on screen".
- Rewritten tests open with a **"WHAT THIS VERIFIES"** line, and their failure messages state the real contract, e.g. *"Work Type dropdown is MISSING canonical option(s) [X] — live options were [...]"* instead of *"lists don't have the same size expected [14] but found [16]"*.

## Four systemic patterns now recorded in memory (check these first next time)
1. **`isOn*Page()` must mean the LIST, not `/route/{id}`** — bit WorkOrder, Issue, Connection, Asset.
2. **V1.36 badges tabs/headings** ("Engineering 5", "Connections !", "OCP 3") — every exact-text locator silently misses.
3. **DataGrid/drawer re-render → StaleElement** — wrap find→act→verify, don't just re-find once.
4. **MUI modal makes the background `inert`** — an unscoped locator matching a background control can never be "visible"; scope to the dialog.

## Still open (deliberately not silently rewritten — needs your call)
- `ZP3027_ServicesNavDistinct`: asserts a `/services` sidebar link; live it exists only in the **setup console**. Product intent? If operational users should see it → **real regression**.
- `ZP3027_WoListHealthy`: expects the **"Show planned" toggle** on /sessions; it is **gone** in V1.36. Deliberate?
- `BUG-008` / `BUG-012` tripwires: both say "bug may be fixed" — they no longer reproduce and should be retired or inverted.
- `WorkTypeDetailContract`: WO-detail header chips are now `[Overdue, 47, 96]`; the work type likely moved into the expandable details panel — needs the V1.36 header mapping.
- Opportunities data-precondition **skips** (46 total run-wide): the API shows **146 quotes** exist, so these are SLD/site *scoping* gaps, not missing data — the next lever for cutting skips.
