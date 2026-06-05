# Full-automation run + failure triage — 2026-06-05

## What ran
- **Full automation:** Parallel Full Suite — Core Regression (848 TCs, run 45) + Parallel
  Suite 2 (run 21). The failed-test collector captured **122 failed test entries** into
  `failed-suites/failed-tests-2026-06-05.xml`.
- **Local re-run at latest code** (`failed-tests-latest.xml`, data-driven-expanded): **179
  pass / 97 fail / 16 skip** — i.e. **179 previously-failing tests now recover** (gold-
  conformance fixes + flaky recovery). The 97 still-failing are triaged below with exact
  messages.

## Triage of the still-failing — by root cause

### A. BUG-B — app-wide WCAG (REAL, known) · ~45 failures
`Phase4QualityGatesTestNG.testRouteAccessibility[*]` — **every** route (Dashboard, Assets,
Tasks, Planning, all Settings `?view=`, all Reporting `?view=`, …) reports 2–4
critical/serious WCAG violations (button-name, color-contrast, aria-progressbar-name).
Single root cause = shared components. Quarantine-able tripwire; filed BUG-B.

### B. BUG-A — `Qe is not a function` crash / hang (REAL, known) · ~33 failures
- `testSearchInputBoundary[*]` (10) — every input crashes Planning search.
- `testDetailPageHealth[Account/Opportunity/EMP]` (3) — detail open crashes.
- `testModuleInteraction[*]` (~13) — Scheduling, Attachments, Notes, Opportunities, EMPs,
  PM Readiness, Equipment Library, Panel Schedules, Sales/Ops Overview, Audit Log all throw
  severe errors on interaction; **Arc Flash HANGS** (spinner >30s — `HangDetector`).
Filed BUG-A; escalated (breaks interactivity). Quarantine-able tripwires.

### C. NEW real product bugs surfaced (HIGH value) · ~8
| Test | Finding |
|---|---|
| `RetractedBugsVerificationTestNG.testVerify_03_KPICardsRender` | **Dashboard renders 0 KPI/card elements** — re-confirmed real bug |
| `BugHuntGlobalTestNG.testBUG007_DuplicateAPICalls` | **Duplicate API calls** (Roles fetched 2×, expected ≤1) — regression |
| `SecurityAuditTestNG.testBUG008_NoRateLimiting` + `AuthenticationTestNG.testTC_SEC_02_LoginRateLimitAfterFailures` | **No login rate-limiting** — 6 consecutive failed logins → 0 throttle/lockout (429/423/403) |
| `DeepBugVerificationTestNG.testBUG001_No404Page` | **No 404 page** — invalid URL renders blank content |
| `NewModulesSmokeTestNG.testTC_NM_06_PanelSchedules`, `testTC_NM_15_ZUniversity` | Module **did not render** (blank) — feature-flag or load bug |
These are genuine product defects (or re-confirmed filed bugs), not test issues.

### D. Gold-conformance / test-bug (FIX THE TEST) · ~5
| Test | Cause | Fix |
|---|---|---|
| `AssetSmokeTestNG.testAddOCPChild` | Uses **Panelboard** for an OCP child, but gold `Panelboard ocp=No` → OCP section correctly absent → wrong expectation | Use an `ocp=Yes` class (Circuit Breaker) |
| `AssetSmokeTestNG.testCreateAsset` / `testAssetFullLifecycle` | Create fails (newly-created asset not in grid) — `fillCoreAttributes` types `"SmokeTest"` into Autocomplete **selects** | Drive selects via first-option; text only into textfields (gold `type`) |
| `AssetSmokeTestNG.testUpdateAsset` | `editModel` writes the **QR Code** field, not Model | real Model handler |
| `CriticalPathTestNG.testCP_DI_005_AssetDataSurvivesRefresh` | Inverted assertion (`expected false but found true`) | fix assertion logic |

### E. Functional flow / locator (investigate) · ~4
- `IssuesSmokeTestNG.testCreateIssue` — visibility timeout in create flow.
- `LocationSmokeTestNG.testUpdateLocation` — visibility timeout in update flow.
- `ConnectionTestNG.testCC_002_OpenCreateDrawer` — Create Connection drawer didn't open after 4 attempts.
- `AssetSmokeTestNG` create paths (overlap with D).
(These may be BUG-A-adjacent — the page/drawer not rendering — or genuine locator drift.)

### F. Perf borderline (env/real) · ~2
- `WorkOrderPart2TestNG.testTC_PERF2_001_ListLoadTime` — 20.85s vs 20s budget (just over).
- `APIPerformanceTest.testConcurrentAPIPerformance` / `testLoginAPIPerformance` — API timing.

### G. UI-state flag (verify) · ~2
- `testRouteAssetsAndState[Sales Overview / Ops Dashboard]` — "ERROR banner/crash text
  visible". Likely the "company not available" / sales-core empty-state, or a UIStateValidator
  false-positive (cf. the SLD empty-state fix). Needs a look before trusting.

### H. Inconclusive (test can't verify) · ~2
- `SecurityAuditTestNG.testBUG007_AuthCookiesSameSiteNone` — neither cookie found → can't assess.
- `DeepBugVerificationTestNG.testBUG002_CSPBeamerFontsBlocked` — Beamer loaded no fonts this run.

### Pending precise messages (core-regression classes, re-run in progress)
`AssetPart1` (28), `Task` (16), `BugHunt` (8), `DashboardBug` (7), `WorkOrderPlanning` (6),
`SiteSelection` (5) — hypothesis: AssetPart1 = gold-conformance (class create/edit + the
type-then-click-first-option class picker) + Task = weak `pageText.contains` happy-path drift.
Confirmed messages appended when the full-122 local re-run completes.

## Bottom line
- **179 recovered** at latest code — the gold-conformance work is paying off.
- Of the 97 still-failing: **~78 are the two known app-wide bugs** (BUG-A crash ~33, BUG-B
  WCAG ~45) — quarantine-able tripwires, one root cause each.
- **~8 are genuine NEW/ re-confirmed product bugs** (0-KPI dashboard, duplicate API calls,
  no rate-limiting, no 404 page, Panel Schedules/Z-University blank) — the highest-value
  findings; route to dev.
- **~9 are test/gold-conformance/flow issues** to fix in the suite (AssetSmoke OCP class,
  create-via-selects, inverted assertion, locator drift).
