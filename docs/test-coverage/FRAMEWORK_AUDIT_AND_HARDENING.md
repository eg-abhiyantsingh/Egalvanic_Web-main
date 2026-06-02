# eGalvanic Web Automation — Framework Audit & Hardening Plan

**Author:** Senior Web Test Automation Architect (destructive-testing mindset)
**Date:** 2026-06-02
**Scope audited:** `com.egalvanic.qa` — 97 Java files, Java 11, Selenium 4.29, TestNG 7.8, REST Assured, ExtentReports. Target app: `https://acme.qa.egalvanic.ai` (React + MUI SPA).

> Core belief driving this audit: **a green suite that finds no bugs means the tests are weak, not that the app is bug-free.** Every finding below is grounded in the actual code, with file/line references.

---

## PHASE 0 — Coverage Inventory

### 0.1 Modules / routes enumerated (from `AppConstants` + `pageobjects` + test classes)

| Module | Page object | Route(s) | Test classes today |
|---|---|---|---|
| Authentication | `LoginPage` | `/login`, `/signup`, `/consent` | AuthenticationTestNG, AuthSmokeTestNG, LoginConsentTestNG |
| Site Selection | (BaseTest helper) | facility selector | SiteSelectionTestNG, SiteSelectionSmokeTestNG |
| Dashboard | `DashboardPage` | `/dashboard` | DashboardBugTestNG, BugHuntDashboardTestNG |
| Asset Management | `AssetPage` | `/assets` | AssetPart1–5, AssetSmoke, CopyToCopyFrom, BulkUploadBulkEdit |
| Locations | `LocationPage` | `/locations` | LocationTestNG, LocationPart2, LocationSmoke, BugHuntLocations |
| Connections | `ConnectionPage` | `/connections` | ConnectionTestNG, ConnectionPart2, ConnectionCoreAttrs, ConnectionSmoke |
| Issues | `IssuePage` | `/issues` | IssueTestNG, IssuePart2, IssuesSmoke, IRPhoto |
| Work Orders | `WorkOrderPage` | `/work-orders` | WorkOrderTestNG, WorkOrderPart2, WorkOrderSmoke |
| Work Order Planning | `PlanningPage` | `/planning` | WorkOrderPlanningTestNG |
| Tasks | — | `/tasks` (session mapping) | TaskTestNG |
| SLD | — | `/slds` | SLDTestNG |
| Goals / Misc / New modules | — | various | GoalsTestNG, MiscFeaturesTestNG, NewModulesSmokeTestNG |
| EG Form / Reporting | — | report engine | EgFormAITestNG, ReportingEngineV2, GenerateReportEgForm |
| API | (REST Assured) | `/api/*` | api/* (5 classes) |
| Security / OWASP | — | cross-cutting | Owasp*, SecurityAudit, ZP1997DOMPurifyXss |
| Exploratory | — | all | MonkeyTestNG, AIPageAnalyzer, AIExtraction |

### 0.2 Coverage matrix — verification quality (the honest view)

"Covered" = exercised **AND** a strong assertion verifies the real outcome. Anything weaker is a **GAP**.

| Module | Element/Flow | States to verify | Covered by | Verification today | Verdict |
|---|---|---|---|---|---|
| Auth | Login happy path | populated | scripted | `waitForDashboard()` | ✅ strong |
| Auth | Invalid creds, lockout, rate-limit | error | scripted | assertions present | ✅ strong |
| Dashboard | Charts render | populated | scripted | `assertTrue(totalCharts>0)` | ⚠️ weak (count, not rendered) |
| Dashboard | No JS error on load | error | partial | not gated on most tests | ❌ GAP |
| Asset | Create → grid shows row | populated | scripted | `isDisplayed()` / row count | ⚠️ weak (no integrity check) |
| Asset | PDF / report renders | populated | none | **no `naturalWidth`, no PDF render check** | ❌ GAP |
| Asset | Reload preserves data | state | none | — | ❌ GAP |
| Locations | CRUD | populated | scripted | row count | ⚠️ weak |
| Connections | Core attrs persist | state | scripted | field read-back (partial) | ⚠️ mixed |
| Issues | Photos load | assets | scripted | `isDisplayed()` only | ❌ GAP (img not verified) |
| Work Orders | Grid paging | populated | scripted | `rows>0` | ⚠️ weak |
| Planning | Plan totals correct | state | scripted | value asserts | ⚠️ partial |
| All | Hang / infinite spinner | loading | none | `waitForPageReady` returns anyway | ❌ GAP |
| All | Broken images | assets | none | — | ❌ GAP |
| All | Failed XHR/fetch (4xx/5xx) | network | partial | counted, never asserted | ❌ GAP |
| All | Blank / broken layout | error | none | — | ❌ GAP |
| All | Console SEVERE errors | error | 3 of ~90 classes | not gated | ❌ GAP |
| All | Offline/throttled network | network | none | — | ❌ GAP |
| All | Back/forward/reload integrity | state | none | — | ❌ GAP |
| All | Functional flow ORDER | cross-module | none | each class shares 1 session, tests independent | ❌ GAP |

**Not-yet-covered (explicit gap list):** PDF/asset render verification; broken-image detection; failed-request gating; hang/freeze detection; blank/broken-UI detection; per-test console-error gating; data-integrity across reload/back/duplicate; network-condition simulation (offline/slow/timeout); multi-tab & session-expiry; cross-module ordered workflows.

---

## PHASE 1 — Diagnosis: why the suite stays green while users hit bugs

Ranked by impact. Each line = the weakness + the one-line fix.

1. **Console errors captured but never gate the build.** `BaseTest.classSetup()` installs `FlakinessPrevention.installConsoleErrorCapture()`, but `@AfterMethod` only inspects errors on `FAILURE`; only 3 of ~90 classes ever call `getConsoleErrors`. → **Fix:** assert `BrowserErrorCapture.assertNoSevereErrors()` in `@AfterMethod` for every passing test.

2. **Console capture only hooks `console.error`** (`FlakinessPrevention:450`). It misses uncaught exceptions (`window.onerror`), unhandled promise rejections, and failed resource loads — the errors that actually mean "page broke." → **Fix:** `BrowserErrorCapture.install()` (adds all three).

3. **Hangs are treated as success.** `waitForReactIdle`/`waitForNetworkIdle`/`waitForPageReady` all *"continue anyway — don't block the test forever"* on timeout (`FlakinessPrevention:115,213`). EAGER page-load + 60s timeout compound this. → **Fix:** `HangDetector.assertResponsive()` converts a never-settling page into a hard failure with evidence.

4. **No asset/PDF verification at all.** Zero `naturalWidth` usages in the codebase; PDFs/photos asserted with `isDisplayed()`, which is `true` for a blank/broken frame. → **Fix:** `AssetLoadVerifier.assertAllImagesLoaded()` / `assertPdfRendered()`.

5. **Network failures invisible.** The XHR/fetch interceptor only *counts* pending requests (`FlakinessPrevention:146`) — it never records status. A 500 on a data call lets the test pass with an empty grid. → **Fix:** `AssetLoadVerifier.installFailedRequestCapture()` + `assertNoFailedRequests()`.

6. **Pervasive silent failure.** 450 empty `catch { }` blocks and 1,588 catch blocks total; 24 `Thread.sleep` calls (incl. `pause()` used 100+ times). Recovery/site-selection swallow every exception (`BaseTest.selectTestSite`, `recoverFromErrorPage`). Real breakage gets masked as "skipped step." → **Fix:** never swallow in verifiers; replace `pause()` with explicit waits; let verifier assertions throw.

7. **Weak assertions dominate.** 182 `isDisplayed()`/`count>0` assertions verify *presence*, not *outcome* — e.g. `assertTrue(totalCharts>0)` passes for an all-zero broken chart. → **Fix:** `UIStateValidator.assertHealthy()` + outcome-level data assertions.

8. **No state-integrity checks.** Nothing captures data before/after a flow; create-duplication, edit-to-wrong-record, and reload data-loss are undetectable. → **Fix:** `StateIntegrityChecker` snapshots + `assertUnchanged/assertGrew`.

9. **No functional-order modeling.** One browser session per *class* with independent `@Test` methods; nothing asserts that a flow only works in an unrealistic order (create-before-select, export-before-save). → **Fix:** dedicated ordered `@Test(dependsOnMethods=...)` workflow classes + AI workflow inference (Phase 3).

10. **No network-condition / multi-tab / session-expiry / viewport coverage.** → **Fix:** CDP `Network.emulateNetworkConditions` + Phase-3 exploratory layer.

> Secondary (not bug-finding but worth flagging): hardcoded credentials and a Gmail app-password live in `AppConstants` (lines 23, 274). Move to env/secret store.

---

## PHASE 2 — Hardened scripted layer (built & compiling)

Four reusable verifiers + one capture installer, in `src/main/java/com/egalvanic/qa/utils/verify/`. All compile against the existing project (`mvn compile` green) and reuse `ScreenshotUtil` + `ExtentReportManager`. Rules followed: CSS/id-first, explicit waits only, **no swallowed exceptions, hard build failures.**

| Class | Bug class it catches |
|---|---|
| `BrowserErrorCapture` | crashes / JS errors (uncaught, promise, resource, console) |
| `HangDetector` | frozen / never-loading pages, infinite spinners, pending network |
| `AssetLoadVerifier` | PDFs/images that fail to render; failed XHR/fetch (4xx/5xx) |
| `UIStateValidator` | blank pages, error banners, clipped/off-screen layout |
| `StateIntegrityChecker` | data loss / duplication / corruption across a flow |

### Concrete runnable examples

**Wire global gates into `BaseTest` (one-time, covers all ~90 classes):**
```java
// in classSetup(), after login:
BrowserErrorCapture.install(driver);
AssetLoadVerifier.installFailedRequestCapture(driver);

// in @AfterMethod, for PASS/at end of each test:
if (result.getStatus() == ITestResult.SUCCESS) {
    BrowserErrorCapture.assertNoSevereErrors(driver, result.getMethod().getMethodName());
    AssetLoadVerifier.assertNoFailedRequests(driver, result.getMethod().getMethodName(),
            "beamer", "devrev", "sentry", "analytics");   // whitelist known 3rd-party noise only
}
```
> Enable native SEVERE logs by adding to `ChromeOptions`:
> ```java
> LoggingPreferences lp = new LoggingPreferences();
> lp.enable(LogType.BROWSER, Level.SEVERE);
> opts.setCapability("goog:loggingPrefs", lp);
> ```

**HangDetector — a real navigation:**
```java
dashboardPage.openAssets();
HangDetector.assertResponsive(driver, "Assets page load", 30);   // fails with screenshot if spinner never clears
```

**AssetLoadVerifier — PDF report + images:**
```java
reportPage.clickGenerateReport();
HangDetector.assertResponsive(driver, "report render", 45);
AssetLoadVerifier.assertPdfRendered(driver, By.cssSelector(".report-preview iframe, embed[type='application/pdf']"), "EG report");
AssetLoadVerifier.assertAllImagesLoaded(driver, "issue photos");   // naturalWidth>0 for every visible img
```

**UIStateValidator — after navigating to a grid page:**
```java
locationPage.open();
UIStateValidator.assertHealthy(driver, "Locations", ".MuiDataGrid-root");   // blank/error/clipped/missing -> fail
```

**StateIntegrityChecker — create + reload round-trip:**
```java
var before = StateIntegrityChecker.snapshotRows(driver, By.cssSelector(".MuiDataGrid-row"));
assetPage.createAsset("SmokeTest-Asset");
var after  = StateIntegrityChecker.snapshotRows(driver, By.cssSelector(".MuiDataGrid-row"));
StateIntegrityChecker.assertGrew(before, after, 1, "create asset");        // exactly +1, no dup, no loss

var a = StateIntegrityChecker.snapshotRows(driver, By.cssSelector(".MuiDataGrid-row"));
driver.navigate().refresh();
HangDetector.assertResponsive(driver, "reload", 30);
var b = StateIntegrityChecker.snapshotRows(driver, By.cssSelector(".MuiDataGrid-row"));
StateIntegrityChecker.assertUnchanged(a, b, "reload");                      // data must survive reload
```

---

## PHASE 3 — AI exploration layer (extends existing `MonkeyTestNG` + AI utils)

The repo already has the seed: `MonkeyTestNG` (random nav/click/fuzz), `AIPageAnalyzer` (DOM discovery + Claude suggestions), `ClaudeClient.askWithImage()` (vision), `VisualRegressionUtil` (screenshot diff + AI analysis), `SmartBugDetector`. The gap is that the monkey **doesn't run the Phase-2 verifiers after each step** and doesn't reconstruct repro steps. Architecture to wire:

```
                ┌──────────────────────────────────────────────┐
                │              ExplorerEngine (loop)             │
                │  pick next action  → execute → VERIFY → record │
                └───────┬───────────────┬───────────────┬───────┘
                        │               │               │
        ActionPlanner (AI)        Phase-2 Verifiers   StepRecorder
        - AIPageAnalyzer            - HangDetector      - action log
          discovers elements        - AssetLoadVerifier - on failure:
        - ClaudeClient ranks        - UIStateValidator    minimal repro
          high-risk pages           - BrowserErrorCapture  (replayable JSON)
        - generates edge/negative   - StateIntegrity      + screenshot
          form inputs               - VisualRegression
                        │
                 CDP / DevTools layer
        Network.enable (status+failures), Network.emulateNetworkConditions
        (offline/slow/3G), Page console, Performance timing
```

**Component responsibilities:**
- **ActionPlanner (AI calls sit here).** Calls `AIPageAnalyzer.analyzePageWithAI(driver)` to enumerate interactive elements per route; sends the page summary to `ClaudeClient.ask()` to (a) rank pages by risk, (b) generate edge/negative inputs per field (empty, max-length+1, unicode, SQLi/XSS strings, negative numbers, huge paste), (c) infer the *expected* user workflow order.
- **Verifier hook.** After **every** action: `HangDetector.check` → `UIStateValidator.validate` → `BrowserErrorCapture.getErrors` → `AssetLoadVerifier.find*`/`getFailedRequests` → optional `VisualRegressionUtil.compareWithBaseline`. Any non-clean result records an anomaly.
- **Visual anomaly detection.** Feed the failure screenshot to `ClaudeClient.askWithImage()` with a "is this page broken (blank/overlap/error)?" prompt — catches breakage no DOM rule encodes.
- **StepRecorder / repro.** Maintain an action stack `{route, locator, input, timestamp}`; on anomaly, emit a minimal replayable JSON + screenshot into `ready-bug/` (reuse `ReadyBugGenerator`).
- **CDP/DevTools usage (Selenium 4):** `((ChromeDriver)driver).getDevTools()` → `Network.enable` to capture real response status & failures (stronger than the JS wrapper), `Network.emulateNetworkConditions(offline / 400ms / 50kbps)` for the offline/slow/timeout matrix, and `Runtime`/`Log` for console.
- **Web-condition matrix to drive:** offline, throttled 3G, request timeout, back/forward/reload, multiple tabs (`driver.switchTo().newWindow`), session expiry (clear cookies mid-flow), viewports (`375×812`, `768×1024`, `1920×1080`).
- **Feedback loop:** every discovered route/element/anomaly is appended to the Phase-0 inventory (`AIPageAnalyzer.writeAnalysisReport`) so coverage stays honest, and flows that only succeed out of natural order are flagged as `ORDER_DEPENDENT` bugs.

**Buildable entry point** (skeleton — drop into `testcase` extending the existing monkey scaffold):
```java
for (String route : planner.rankRoutesByRisk(allRoutes)) {       // AI-ranked
    navigate(route);
    HangDetector.assertResponsive(driver, route, 30);
    for (Action a : planner.planActions(driver, route)) {        // AI edge/negative inputs
        recorder.push(a);
        a.execute(driver);
        var hang = HangDetector.check(driver, a.desc(), 15);
        var ui   = UIStateValidator.validate(driver, a.desc());
        var errs = BrowserErrorCapture.getErrors(driver);
        var net  = AssetLoadVerifier.getFailedRequests(driver);
        if (hang.hung || !ui.ok() || !errs.isEmpty() || !net.isEmpty())
            recorder.recordAnomaly(a, hang, ui, errs, net);       // -> ready-bug/ repro
        BrowserErrorCapture.clear(driver);
    }
}
```

---

## PHASE 4 — Coverage map & completeness report

Per module, grouped by the 7 dimensions. Legend: ✅ scripted-strong · 🟡 scripted-weak · 🤖 AI-explorable (after Phase 3) · ❌ gap.

| Module | Nav/Routing | Input/Forms | Assets/PDF | State/Data | Network (off/slow/timeout) | Browser (back/fwd/reload/tabs/resize) | Session/Auth |
|---|---|---|---|---|---|---|---|
| Authentication | ✅ | ✅ | ❌ | 🟡 | ❌ 🤖 | ❌ 🤖 | ✅ |
| Site Selection | ✅ | 🟡 | n/a | ❌ | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Dashboard | ✅ | n/a | ❌ | 🟡 | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Assets | ✅ | 🟡 | ❌→✅* | ❌→✅* | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Locations | ✅ | 🟡 | ❌ | ❌→✅* | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Connections | ✅ | 🟡 | ❌ | 🟡 | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Issues | ✅ | 🟡 | ❌→✅* | 🟡 | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Work Orders | ✅ | 🟡 | ❌ | 🟡 | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Planning | ✅ | 🟡 | ❌ | 🟡 | ❌ 🤖 | ❌ 🤖 | 🟡 |
| Tasks | 🟡 | 🟡 | ❌ | ❌ | ❌ 🤖 | ❌ 🤖 | 🟡 |
| SLD | 🟡 | n/a | ❌→✅* | ❌ | ❌ 🤖 | ❌ 🤖 | 🟡 |
| EG Form/Report | 🟡 | 🟡 | ❌→✅* | ❌ | ❌ 🤖 | ❌ 🤖 | 🟡 |
| API | ✅ | ✅ | n/a | 🟡 | 🟡 | n/a | ✅ |

`*` = becomes ✅ once the Phase-2 verifiers are wired into that module's tests (verifiers are built; wiring is the remaining task).

### Completeness summary (honest, un-inflated)
- **Modules with at least one strong end-to-end verification:** Authentication, API (~15% of the matrix cells).
- **Dominant state today:** 🟡 weak (presence-only) assertions on navigation/forms, plus systemic ❌ gaps on assets, hang, network, state-integrity, and browser-action dimensions.
- **Biggest remaining gaps (in priority order):** per-test console-error gate not wired; asset/PDF verifiers not yet called by module tests; no network-condition/offline coverage; no reload/back-button integrity tests; no ordered cross-module workflow tests; credentials hardcoded.

---

## Do this first — top 5

1. **Wire `BrowserErrorCapture.assertNoSevereErrors()` into `BaseTest.@AfterMethod` for every passing test** (+ enable `goog:loggingPrefs SEVERE`). Single highest-yield change: instantly turns ~90 green classes into JS-error detectors.
2. **Replace `waitForPageReady`'s silent timeout with `HangDetector.assertResponsive()`** at every navigation. Stops the suite from passing on frozen pages.
3. **Call `AssetLoadVerifier` in the Issues/Asset/Report/SLD modules** — `assertAllImagesLoaded` + `assertPdfRendered`. Closes the entire asset/PDF blind spot (currently zero coverage).
4. **Install `installFailedRequestCapture` + `assertNoFailedRequests`** globally (whitelist only beamer/devrev/sentry). Catches 4xx/5xx data-call failures that today render as empty-but-green grids.
5. **Add reload/back-button integrity tests with `StateIntegrityChecker.assertUnchanged`** on each CRUD grid, and start one ordered cross-module workflow class (create → open → export → delete) to expose order-dependent flows.

> Everything in Phase 2 is built and compiling. The remaining work is *wiring* (calling the verifiers from module tests + `BaseTest`) and building the Phase-3 `ExplorerEngine` on top of the existing `MonkeyTestNG`/`AIPageAnalyzer` scaffold.
