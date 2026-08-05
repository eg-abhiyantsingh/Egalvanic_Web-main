# Framework hardening — toward ≥90% *correct* results, fewer skips

**Date:** 2026-08-05
**Prompt:** "use your super intelligence… I can expect at least 90% correct result. If you need to update framework then do it. Result should be more correct and less skip test cases."

## The two deepest root causes found this pass

### 1. V1.36 removed the facility selector from the dashboard — and `BaseTest.selectTestSite()` silently skipped for EVERY class
Login lands on `/dashboard`; the facility picker now exists **only on site-scoped module pages** (`/assets`, `/slds`, `/sessions`…). `selectTestSite()` waited 15s on the dashboard, printed "Facility selector not found — skipping site selection", and every class ran against whatever site was sticky in the session. This is the hidden cause behind most **data-dependent failures** (Issue/Task/WO searches returning nothing, "grid empty" skips in Opportunities — whose data still exists: SLD 'gyu' holds 24 quotes, verified via `POST /api/company/{id}/quotes/v2`).
**Fix:** `selectTestSite()` hops to `/assets` when no facility input is present, selects the site there, reinstalls health hooks.

### 2. SiteSelection classes: stale `By.id("email")` + dashboard-anchored premise
The May-2026 login page dropped `id="email"`; both standalone classes (they don't extend BaseTest) still waited on it → classSetup threw → **14 + 3 tests failed at 0s** in the full run. Their whole premise ("facility selector on the dashboard") is also obsolete.
**Fix:** 5-way email/password locators (same as LoginPage), class anchor moved to `/assets`, `RolePinUtil` (new shared helper) pins the operational role after login, accepted-URL predicate includes `/assets`.

## Other correctness fixes in this pass
- **`NewModulesSmokeTestNG`**:
  - On **Access Denied** under the pinned role, retry once under **"Admin"** (V1.36 split modules across two role consoles — Goals/Forms/SalesDB perms live on Admin), then restore the default role.
  - `smokeAssertShellRendered()` now **polls up to 25s** — the old single-shot check failed at ~4s while `<main>` still showed the loading spinner (token match hits sidebar text instantly; slow module APIs hadn't hydrated). Also accepts pages with no `<main>` landmark at all (`/z-university`).
  - Stale expected-text contracts updated to live V1.36: `/sales-overview` (now "Planned Work / unreleased work orders / Needs Attention"), `/panel-schedules` (now "Schedule Status / Panel Name"), dashboard KPI row (now TOTAL ASSETS / ACTIVE WORK ORDERS / EQUIPMENT AT RISK — the May-2026 'Opportunities Value' card was removed).
- **Subtype-less asset classes** (OCP/Utility/MCCB — verified: subtype-bearing VFD passes): `verifyAssetSubtype(null)` treats an absent Subtype control as the valid V1.36 state instead of failing (patched in AssetPart3/4/5).
- **Cable core-attributes contract** (`CONN_081b`): updated to the live field set (Conductors Description / Parallel Sets / Conductor Material / Wire Size-H / Diameter / Raceway Material; "Wire Size-N" and "# of Conductors" removed in the redesign).

## Verified results (before → after)
| Scope | Before (2026-08-04 full run) | After |
|---|---|---|
| SiteSelectionTestNG (30 TCs) | 14 FAIL at 0s + 16 never-ran | **30/30 PASS** |
| SiteSelectionSmoke + NewModulesSmoke + SiteSelectionTestNG | 23 fails combined | 51 run / 5 fail (then 5 residuals fixed → final verification in verify3.log) |
| AssetPart3 GEN edits (sample of 10-fail cluster) | 10 FAIL | **3/3 re-run PASS** (Engineering-tab fix) |
| Full suite | 1441 pass / 78 fail / 45 skip | fixes cover ~65 of the 78; re-measure on next full run |

## Remaining acknowledged gaps (documented, not silently ignored)
- ~6 REAL tripwire findings stay red by design (ZP-2025 rate-limit, slow /assets API, heap growth, localStorage recovery, BUG-008/012 state) — recommend moving to the `known-product-bug` group so green = "no new regressions".
- WorkType catalog / Opportunities status-enum / ZP3027 stale contracts still need product-owner confirmation before pinning new expectations.
- **Goals module: CONFIRMED REAL PRODUCT BUG (2026-08-05).** `/goals` crashes to the "Application Error" boundary under BOTH roles (test retried under Admin; independently reproduced in a live browser under Super Admin). Console: `TypeError: Cannot read properties of undefined (reading 'length')`; on-screen error ID `5daf74a67017`. `TC_NM_03_Goals` is correctly red — file to ZP with this evidence.

## Final verification (this pass)
`SiteSelectionSmokeTestNG + NewModulesSmokeTestNG + SiteSelectionTestNG`: was **23 fails** across these classes in the full run → now **51 pass / 1 fail / 0 skip** total (30/30 + 20/21), and the single remaining fail is the confirmed-real Goals crash. That is the target state: red = real.
