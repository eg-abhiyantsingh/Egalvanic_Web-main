# Full-suite failure triage — why "lots of fails are invalid", and the systemic fixes

**Date:** 2026-08-04 → 08-05
**Prompt:** "cover everything, check why lots of test cases are failed when I run full automation, lots of fails are invalid."

## Run baseline
`fullsuite-testng.xml` (51 classes), headed, live QA V1.36. Captured **1441 pass / 78 fail / 45 skip** across **49 of 51 classes** before I stopped it — the run had stalled ~2h on `WorkTypeCreateE2EMatrixTestNG.testTC_WTC_001_CreatePerType` slowly re-failing the same create-nav cluster (see below); the last two classes are the same family and add no new signal.

## Headline
**~72 of 78 failures are INVALID (test-side), not product bugs.** They fall into a small number of causes created by the V1.36 redesign — a role/console change, moved fields, redesigned schemas, and dead routes — plus data-dependent search. **~6 are REAL** product signals, all from tripwire tests designed to catch them.

## The dominant systemic cause: V1.36 role rename → dual-console nondeterminism
The tenant renamed roles: `b60006dd` = **"Super Admin"** (operational console: Dashboards/Assets/Issues/SLDs/Work Orders) and `e9ad3158` (old internal "EG Admin") = **"Admin"** (setup console: Setup/Customers/Labor/Forms/Classes… — **none** of the operational sidebar links). A fresh login lands on **either** console. Every page object that navigates by *clicking a sidebar link by its visible text* silently no-ops when the session is on the wrong console, and the test then dies downstream with a misleading "element not found / dialog didn't open" — which looks like a random invalid failure and is exactly what the owner observed. (Proven live 2026-08-03: WorkOrderCreate went 0-green → 8-green after adding a URL fallback.)

## Invalid-failure buckets (with fix)
| Bucket | # | Cause | Fix |
|---|---|---|---|
| SiteSelection infra | 17 | Class builds its own driver + anchors on the facility selector, which only exists on some consoles | `ensureActiveRole` + facility-selector guard (partial; class needs a role-pin of its own) |
| WO create dialog/nav + WorkType create-matrix | ~10 | Create redirects to `/sessions/{id}`; `isOnWorkOrdersPage()` treated the detail page as "on the list", so nav never returned to the grid and every following create failed ("Create form opened: false") | `isOnWorkOrdersPage()` now distinguishes list vs `/sessions/{id}`; URL fallback returns to the list |
| GEN/FUSE electrical-attr reads | ~6 | V1.36 moved Generator electrical attributes to the detail page's **Engineering** tab; the reader looked only at Basic Info → null (values DID save) | `readDetailAttributeValue` flips to the Engineering tab if the field isn't on the current tab |
| NewModulesSmoke "module did not render" | 6 | Smoke checks reach modules via sidebar text on the wrong console | covered by `ensureActiveRole` role-pin |
| CriticalPath cross-checks / "link should have href" | ~7 | Dashboard-vs-module counts + "Work Orders link should have an href" — console-dependent nav + sparse-site data | `ensureActiveRole`; a few are data-dependent |
| search input / results (Issue/Task/WO) | ~9 | Search box present but the class's site had little/no matching data, or grid not yet rendered | data-dependency (0 data ≠ bug) + poll-after-render |
| Subtype-less classes (OCP/UTL/MCCB) | 4 | `verifyAssetSubtype` asserts a Subtype field for classes that legitimately have none (VFD, which has subtypes, passed) | test should skip subtype-less classes (documented; small test edit) |
| Connection Cable core-attrs (CONN_081b) | 1 | Cable's core-attribute schema was redesigned (now Conductors Description / Parallel Sets / Conductor Material / Wire Size-H / Diameter / Raceway Material) | update the test's expected field set to the new contract |
| Stale "Services should be absent" / catalog / status-enum expectations | ~4 | `ZP3027` expects Services nav ABSENT (it's deployed now); WorkType catalog "drifted"; Opportunities status `Draft`/columns changed | update stale expectations to the shipped contract |
| stale-element / flake | ~3 | MUI re-render races (WTD_006/011, FUSE_EAD_13) | re-locate after re-render |

## REAL product signals (tripwires doing their job — keep, file/track)
| Test | Finding |
|---|---|
| `AuthenticationTestNG.TC_SEC_02` | **No login rate-limiting** — 10 bad logins all 401, no 429/423/403 (ZP-2025, already tracked) |
| `Phase1BugHunterTestNG.TC_BH_33` | A `/api/*` request exceeded the 5s SLA on `/assets` load (matches the known slow-endpoint area) |
| `Phase1BugHunterTestNG.TC_BH_37` | JS heap grew **191 MB** across 3 navigation cycles (>50 MB threshold) — likely listener leak |
| `Phase1BugHunterTestNG.TC_BH_31` | App didn't recover from localStorage corruption (DOM collapsed) — resilience gap to verify |
| `BugHuntDashboard/Tasks` | BUG-012 / BUG-008 tripwires — assert current bug state; verify against product before filing |

These are **not** invalid — they are the suite catching genuine (mostly already-known) product characteristics. They should stay red until the product changes, or be moved behind the `known-product-bug` group so they don't dilute the signal.

## Fixes applied in this change
- **`AppConstants.DEFAULT_ACTIVE_ROLE`** (`Super Admin`, env `ACTIVE_ROLE`).
- **`BaseTest.ensureActiveRole()`** — pins the header Role after login and after recovery re-login, making the landing console deterministic for the whole class (the single highest-leverage fix; neutralizes the dual-console nondeterminism behind SiteSelection, NewModulesSmoke, CriticalPath-nav, and many search/create fails).
- **URL-fallback navigation** in `AssetPage`, `ConnectionPage`, `IssuePage`; fixed `LocationPage`'s broken fallback (built `/admin-dashboard/locations`) and its loose `contains("location")` predicate.
- **`WorkOrderPage.isOnWorkOrdersPage()`** — list vs `/sessions/{id}` detail (fixes the WorkType create-matrix stall).
- **`AssetPart3TestNG.readDetailAttributeValue()`** — Engineering-tab fallback (fixes the GEN cluster).
- **`BaseTest.waitAndDismissAppAlert()`** — poll instead of a fixed 10s block (saves ~8–13 min per full run).
- **Recovery** paths no longer assume a 'Locations' sidebar link.

## Recommended next (not done here — needs product confirmation or larger edits)
- SiteSelection classes: give them their own role-pin / stop building a bespoke driver.
- Update the stale-contract tests (Cable core-attrs, WorkType catalog, Opportunities status enum, ZP3027 Services-absent) to the shipped V1.36 contracts.
- Move the confirmed-real tripwires behind `known-product-bug` so a green full run means "no *new* regressions."
