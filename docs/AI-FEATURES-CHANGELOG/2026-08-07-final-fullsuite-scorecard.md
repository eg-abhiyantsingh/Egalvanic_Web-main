# Final full-suite scorecard — 78 → 15 failures, every one accounted for

**Date:** 2026-08-06 → 08-07 (14.6 h run)
**Prompt:** "check 1st to last everything. I don't want any wrong fail test case."

## Result

| Run | Pass | Fail | Skip | Notes |
|---|---|---|---|---|
| Baseline (08-04) | 1441 | **78** | 45 | pre-fix |
| Mid (08-05/06) | 1460 | 56 | 46 | partial fixes |
| **FINAL (08-07)** | **1563** | **15** | 46 | 1629 tests, all 51 classes |

**Failures cut 78 → 15 (−81%). Pass count +122.** Not one of the 15 is unexplained.

## Every remaining failure, with its verdict

### Genuinely red — leave them red (3)
| Test | Finding |
|---|---|
| `AuthenticationTestNG.TC_SEC_02` | **No login rate-limiting** — 10 bad logins → `401 ×10`, never 429/423/403 (ZP-2025, tracked). |
| `NewModulesSmokeTestNG.TC_NM_03_Goals` | **`/goals` crashes for every role** — "Application Error" boundary, console `TypeError: Cannot read properties of undefined (reading 'length')`, on-screen error ID `5daf74a67017`. Reproduced live under Super Admin and via the test's Admin retry. **Ready to file.** |
| `Phase1BugHunterTestNG.TC_BH_37` | **JS heap +217 MB across 3 navigations** (threshold 50 MB) — third consecutive run (191 → 217 MB). Held as *likely-real*: Selenium/CDP inflates heap, so it needs a harness-independent measurement before filing. |

### Awaiting your product decision (4) — deliberately NOT rewritten
| Test | Question |
|---|---|
| `WorkOrderTestNG.ZP3027_ServicesNavDistinct` | `/services` now appears **only in the setup console**. If operational users should see it → real regression. If it moved by design → the test needs role context. |
| `WorkOrderTestNG.ZP3027_WoListHealthy` | The **"Show planned" toggle is gone** from `/sessions`. Deliberate removal? |
| `BugHuntTasksTestNG.BUG008_AllTasksOverdue` | Its own message says *"Bug may be fixed"* — Pending (211) ≠ Overdue (172). Retire or invert? |
| `BugHuntDashboardTestNG.BUG012_CompanyInfoNotAvailable` | Banner is **not** showing (`alertVisible=false`), i.e. the bug isn't reproducing, but the assertion fails anyway. Retire or invert? |

### Fixed after this run — will be green next time (8)
| Test(s) | Root cause (all verified live) | Commit |
|---|---|---|
| `TC_SS_011`, `TC_SF_002`, `ISS_046` | Timing races: single-shot counts against an async ~180-site dropdown / debounced server-side search / delete→search-index lag. Now settled-polled (15–30 s). | `a4a0e27` |
| `CP_DI_002`, `CP_DI_003`, `CP_CM_002` | V1.36 dashboard: no Pending-Tasks widget (KPI row = TOTAL ASSETS / ACTIVE WORK ORDERS / EQUIPMENT AT RISK); "Unresolved Issues" card → "Open Issues by Site" donut; nav link text is `"Work Orders 99+"` (count badge) so `normalize-space()=` missed it. | `deaaf38` |
| `Opp06_SldColumnMatches` | `/opportunities` is now a **company-wide pipeline** — no SLD selector anywhere, grid column is Facility, rows legitimately span facilities. The "single distinct SLD" premise described the pre-V1.36 scoped view. | `621e709` |
| `WorkTypeCatalogApiTest` | Catalog pinned to exactly 13; tenant holds 15 because users created two services ("abhiyant Preventive", "abhiyant service corrective"). Now a canonical-subset contract — a *missing* canonical service still fails loudly. | `acdee84` |

## Big wins proven at full scale
- **`WorkTypeCreateE2EMatrix` completed normally.** It burned ~2 h stalling in *both* prior runs; the list-vs-detail navigation fix cleared it entirely.
- **The 13-fail Issues cluster is gone** (list-vs-detail predicate + `searchIssues` self-heal).
- **The 8-fail GEN block is gone** (badge-tolerant Engineering-tab match).
- **The ~10 stale-element flakes are gone** (retry wrappers at both throw sites).
- **`TC_BH_31` passes** — proven live that corrupting localStorage makes the app render a working login form (graceful re-auth), not a collapsed DOM.

## Skips: 46 (unchanged headline, but the composition changed)
- **Opportunities 27** — these are now *create-dialog* preconditions (`Opp04/06/07/10/12`, "No grid/SLD"), not the old "class never scoped to its site". The `selectSiteByName` /assets-hop fix let the class reach its data; the residue is the create dialog's own SLD requirement.
- **Goals 12** — data preconditions on a module that currently crashes (see the `/goals` real bug); these should clear once that's fixed.
- Remaining 7 spread across Phase1BugHunter (3), WorkOrderPart2 (2), Accounts (2).

## What still isn't proven
The 8 post-run fixes are compiled and unit-verified but have **not** yet run inside a full suite. One more full run would confirm the projected **~7 remaining failures** (3 real/likely-real + 4 owner-decision).
