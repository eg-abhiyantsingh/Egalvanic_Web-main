package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.AssetPage;
import com.egalvanic.qa.pageobjects.ConnectionPage;
import com.egalvanic.qa.pageobjects.DashboardPage;
import com.egalvanic.qa.pageobjects.IssuePage;
import com.egalvanic.qa.pageobjects.LocationPage;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.pageobjects.WorkOrderPage;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;
import com.egalvanic.qa.utils.ai.FlakinessPrevention;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import com.egalvanic.qa.utils.ai.SelfHealingElement;
import com.egalvanic.qa.utils.ai.SmartBugDetector;
import com.egalvanic.qa.utils.verify.AssetLoadVerifier;
import com.egalvanic.qa.utils.verify.BrowserErrorCapture;
import com.egalvanic.qa.utils.verify.HangDetector;
import com.egalvanic.qa.utils.verify.UIStateValidator;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base Test - Parent class for all Web Test classes
 *
 * Architecture: ONE browser session per test CLASS (not per method).
 * All CRUD test methods within a class share the same browser session.
 * This avoids repeated login/logout cycles which trigger server-side
 * Application Error pages on rapid sequential sessions.
 *
 * Lifecycle:
 *   @BeforeSuite  → init reports
 *   @BeforeClass  → create browser, login, select site (ONCE per class)
 *   @BeforeMethod → record start time (per test)
 *   [test runs]
 *   @AfterMethod  → failure screenshot, timing (per test, does NOT quit driver)
 *   @AfterClass   → quit browser (ONCE per class)
 *   @AfterSuite   → flush reports
 */
public class BaseTest {

    protected WebDriver driver;
    protected LoginPage loginPage;
    protected DashboardPage dashboardPage;
    protected AssetPage assetPage;
    protected ConnectionPage connectionPage;
    protected LocationPage locationPage;
    protected IssuePage issuePage;
    protected WorkOrderPage workOrderPage;

    private long testStartTime;

    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("h:mm a - dd MMM");

    protected String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FMT);
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        long seconds = ms / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainingSecs = seconds % 60;
        return minutes + "m " + remainingSecs + "s";
    }

    // ================================================================
    // SUITE LEVEL SETUP/TEARDOWN
    // ================================================================

    @BeforeSuite
    public void suiteSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     eGalvanic Web Automation - Test Suite Starting");
        System.out.println("     " + timestamp());
        System.out.println("==============================================================");
        System.out.println();

        ExtentReportManager.initReports();
        ScreenshotUtil.cleanupOldScreenshots(7);
    }

    /**
     * Suite-level teardown — final report flush + the report email.
     *
     * <p><b>alwaysRun = true</b> is the root-cause fix for the lost-Detailed-Report defect.
     * TestNG records a failed configuration method against its <em>declaring</em> class, and
     * under the default {@code configfailurepolicy=skip} it then skips every other
     * non-{@code alwaysRun} configuration method on that class. Because {@code classSetup}
     * ({@code @BeforeClass}) and this {@code @AfterSuite} are both declared on
     * {@code BaseTest}, a failing {@code @BeforeClass} suppressed this method — and with it
     * the only {@code flushReports()} call, so no Detailed Report HTML was ever written.</p>
     *
     * <p><b>Position matters, which is what makes this defect so intermittent.</b> A later
     * <em>successful</em> {@code @BeforeClass} on the same declaring class clears the failure
     * record, so a mid-suite failure is "healed" and {@code @AfterSuite} still runs. Only a
     * failure in the LAST {@code <test>} block leaves the record dirty at suite end. Verified
     * against this project's TestNG 7.8.0 with a standalone repro mirroring the real shape
     * (base class + subclass that {@code @Override}s {@code classSetup} and throws): failing
     * class first → {@code @AfterSuite} ran; failing class last → it did NOT run; with
     * {@code alwaysRun = true} it ran in both.</p>
     *
     * <p>Evidence: CI run 31233370537 "Dashboard + BugHunt" — 271 tests executed and surefire
     * finished normally, 14 configuration methods were skipped, and the group produced zero
     * {@code reports/detail-report/*.html}. The failing class,
     * {@code ArcFlashEngineeringE2ETestNG}, was the suite's LAST {@code <test>} block.</p>
     *
     * <p>{@code alwaysRun} alone is not the whole fix: it cannot help when the JVM dies before
     * suite end. The incremental per-class flush in {@link #classTeardown()} is the belt to
     * this braces.</p>
     */
    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        ExtentReportManager.flushReports();

        // Write AI bug detection report and print summary
        SmartBugDetector.writeReport();
        String bugSummary = SmartBugDetector.getSummary();
        if (!bugSummary.startsWith("No failures")) {
            System.out.println();
            System.out.println(bugSummary);
        }

        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     eGalvanic Web Automation - Test Suite Complete");
        System.out.println("     " + timestamp());
        System.out.println("==============================================================");
        // Print self-healing and flakiness prevention statistics
        System.out.println(SelfHealingElement.getStatsSummary());
        System.out.println(FlakinessPrevention.getStatsSummary());

        System.out.println("Reports generated:");
        for (java.util.Map.Entry<String, String> e :
                ExtentReportManager.getDetailedReportPathsByModule().entrySet()) {
            int fails = ExtentReportManager.getFailsByModule().getOrDefault(e.getKey(), 0);
            System.out.println("   - Detailed [" + e.getKey() + "] (" + fails + "F): " + e.getValue());
        }
        System.out.println("   - Client:   " + ExtentReportManager.getClientReportPath());
        System.out.println("   - Bug Detection: test-output/bug-detection-report.json");
        System.out.println("   - Healed Locators: test-output/healed-locators.json");
    }

    // ================================================================
    // CLASS LEVEL SETUP/TEARDOWN (shared browser per test class)
    // ================================================================

    @BeforeClass
    public void classSetup() {
        // Create ChromeDriver (Selenium 4.29+ manages driver automatically)
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);

        // Allow an explicit browser binary (CI images / Playwright-managed Chromium)
        // when Selenium Manager can't auto-resolve. Empty by default.
        if (!AppConstants.CHROME_BINARY.isEmpty()) {
            opts.setBinary(AppConstants.CHROME_BINARY);
        }

        // Accept the QA host's self-signed / internal-CA TLS certificate. Without
        // this, a browser that doesn't trust the cert (e.g. a Playwright-managed
        // Chromium or a clean CI image) is stuck on Chrome's "Your connection is
        // not private" interstitial, the login form never renders, and BaseTest
        // fails with a misleading "Login failed after 3 attempts" timeout.
        opts.setAcceptInsecureCerts(true);
        opts.addArguments("--ignore-certificate-errors");

        // Capture native SEVERE browser logs so BrowserErrorCapture can drain them
        // in addition to its JS-injected hooks.
        org.openqa.selenium.logging.LoggingPreferences logPrefs =
                new org.openqa.selenium.logging.LoggingPreferences();
        logPrefs.enable(org.openqa.selenium.logging.LogType.BROWSER, java.util.logging.Level.SEVERE);
        opts.setCapability("goog:loggingPrefs", logPrefs);

        // Disable "Save password?" popup
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);

        // Use "eager" page load strategy to avoid 300s hangs on SPA navigations
        opts.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

        if ("true".equals(System.getProperty("headless"))) {
            // In --headless=new, --start-maximized/maximize() leaves a ~800x600 window.
            // MUI DataGrids VIRTUALIZE columns horizontally, so right-side columns
            // (Sensor/Plug Amps, Phase A/B/C Wire Size, Status…) never enter the DOM and
            // column-presence tests fail CI-only (root cause of the 2026-07-02 arc-flash
            // parallel-suite failures). Force a desktop-sized viewport.
            opts.addArguments("--headless=new", "--window-size=1920,1080");
        }

        // Wrap ChromeDriver with self-healing capabilities — ALL findElement/findElements
        // calls now auto-retry, recover from stale elements, and try alternative locators.
        // Existing test code requires ZERO changes.
        driver = SelfHealingDriver.wrap(new ChromeDriver(opts));
        driver.manage().window().maximize();
        if ("true".equals(System.getProperty("headless"))) {
            // maximize() is a no-op in headless; pin the size explicitly as well.
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
        }
        // Reduce pageLoad timeout from default 300s to 60s to prevent browser death
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.body.style.zoom='90%';");

        // Install flakiness prevention interceptors (network tracking + console error capture)
        FlakinessPrevention.installNetworkInterceptor(driver);
        FlakinessPrevention.installConsoleErrorCapture(driver);

        // Install destructive-testing health capture: full JS error capture
        // (uncaught/promise/resource) + failed XHR/fetch (4xx/5xx) recording.
        BrowserErrorCapture.install(driver);
        AssetLoadVerifier.installFailedRequestCapture(driver);

        // Set driver for screenshot utility
        ScreenshotUtil.setDriver(driver);

        // Initialize page objects
        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
        assetPage = new AssetPage(driver);
        connectionPage = new ConnectionPage(driver);
        locationPage = new LocationPage(driver);
        issuePage = new IssuePage(driver);
        workOrderPage = new WorkOrderPage(driver);

        // Login and select site
        loginAndSelectSite();

        // Re-install interceptors after login (page navigations reset JS state)
        FlakinessPrevention.installNetworkInterceptor(driver);
        FlakinessPrevention.installConsoleErrorCapture(driver);
        BrowserErrorCapture.install(driver);
        AssetLoadVerifier.installFailedRequestCapture(driver);
    }

    // ================================================================
    // HEALTH GATES — reusable destructive-testing verifiers
    // ================================================================

    /**
     * Hard-assert that the current page is healthy: responsive (not hung),
     * no severe JS/console errors, no failed app XHR/fetch, and a valid UI
     * state (not blank / no error banner). Call after navigations in module
     * tests, optionally passing CSS selectors that MUST be present.
     *
     * 3rd-party noise (beamer/devrev/sentry/analytics/etc.) is whitelisted via
     * AppConstants.HEALTH_GATE_IGNORE so only eGalvanic app failures fail the test.
     */
    protected void verifyPageHealth(String context, String... requiredCss) {
        HangDetector.assertResponsive(driver, context, 30);
        BrowserErrorCapture.assertNoSevereErrors(driver, context, AppConstants.HEALTH_GATE_IGNORE);
        AssetLoadVerifier.assertNoFailedRequests(driver, context, AppConstants.HEALTH_GATE_IGNORE);
        UIStateValidator.assertHealthy(driver, context, requiredCss);
    }

    /**
     * Hard-assert no critical/serious WCAG accessibility violations on the current
     * page (axe-core). Moderate/minor are logged only. Opt-in: call after a page
     * is fully rendered. Catches contrast, missing labels, ARIA, alt-text, etc.
     */
    /** Month names/abbrevs + status/enum words that appear in grids but are NOT searchable text. */
    private static final java.util.Set<String> NON_SEARCHABLE_WORDS =
            new java.util.HashSet<>(java.util.Arrays.asList(
                    "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
                    "january", "february", "march", "april", "june", "july", "august",
                    "september", "october", "november", "december",
                    "draft", "open", "closed", "active", "inactive", "pending", "standard",
                    "none", "null", "true", "false", "yes", "no", "n/a"));

    /**
     * Pick a token from a grid row that the backend can plausibly match on, for search tests.
     *
     * WHY THIS EXISTS: the obvious `row.getText().split(" ")[0]` takes the FIRST cell, which in
     * every grid in this app is the rendered DATE — so the search term becomes a month abbreviation
     * ("Aug 5, 2026" -> "Aug"). Nothing indexes formatted date strings, so such a query tells you
     * nothing about the filter and produces a failure that looks like a product bug but is not.
     *
     * Skips dates, currency, pure numbers, IDs and status/enum words; prefers the longest remaining
     * alphabetic word (the most distinctive, least likely to match everything).
     *
     * @return a usable token, or {@code null} if the row has no searchable text.
     */
    protected String searchableTokenFrom(String rowText) {
        if (rowText == null || rowText.trim().isEmpty()) return null;
        String best = null;
        for (String raw : rowText.split("[\\s\\n\\r,]+")) {
            String w = raw.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");   // trim punctuation
            if (w.length() < 4) continue;                                  // too short to be selective
            if (NON_SEARCHABLE_WORDS.contains(w.toLowerCase())) continue;  // month / status word
            if (!w.matches(".*\\p{L}.*")) continue;                        // needs at least one letter
            if (w.matches(".*\\d.*") && w.matches(".*[-_].*")) continue;    // looks like an ID/uuid chunk
            if (w.matches("\\p{L}{1,3}\\d+")) continue;                    // e.g. "WO123" style codes
            if (best == null || w.length() > best.length()) best = w;
        }
        return best;
    }

    protected void verifyAccessibility(String context) {
        com.egalvanic.qa.utils.verify.A11yVerifier.assertNoBlockingViolations(driver, context);
    }

    /**
     * A11y check scoped to the current page's OWN markup — the app's shared chrome (sidebar,
     * global FAB, third-party widgets) is excluded. Use this in FUNCTIONAL tests; use
     * {@link #verifyAccessibility} in dedicated a11y tests where whole-page scope is the point.
     * See {@code A11yVerifier.SHARED_CHROME_SELECTORS} for the verified rationale.
     */
    protected void verifyAccessibilityPageOnly(String context) {
        com.egalvanic.qa.utils.verify.A11yVerifier.assertNoPageSpecificViolations(driver, context);
    }

    /**
     * Hard-assert the current page loaded within {@code loadBudgetMs} (client-side
     * Navigation Timing). Use a realistic per-module budget. Logs TTFB/DCL/load/FCP.
     */
    protected void verifyPerformance(String context, long loadBudgetMs) {
        com.egalvanic.qa.utils.verify.PerfVerifier.assertWithinBudget(driver, context, loadBudgetMs);
    }

    /**
     * Re-install JS capture hooks after a full page reload / driver.get(), which
     * wipes injected state. Call this whenever a test reloads or hard-navigates.
     */
    protected void reinstallHealthCapture() {
        BrowserErrorCapture.install(driver);
        AssetLoadVerifier.installFailedRequestCapture(driver);
        FlakinessPrevention.installNetworkInterceptor(driver);
    }

    /**
     * Class-level teardown.
     *
     * <p><b>alwaysRun = true</b> is load-bearing: TestNG's default
     * {@code configfailurepolicy=skip} skips the remaining configuration methods of a class
     * whose {@code @BeforeClass} failed, which previously left ChromeDriver processes
     * orphaned on the CI runner (seen in run 31233370537: "Terminate orphan process:
     * chromedriver / chrome" at job cleanup after
     * {@code ArcFlashEngineeringE2ETestNG.classSetup} failed).</p>
     */
    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        // Persist the Extent HTML NOW, not only at @AfterSuite. ExtentReports holds the
        // whole run in memory until flush(), so a suite that never reaches @AfterSuite —
        // skipped config, killed JVM, cancelled job — loses 100% of its Detailed Reports.
        // flush() rewrites the HTML from the in-memory model, so calling it per class is
        // idempotent; each call just supersedes the file with a more complete one.
        // Deliberately BEFORE driver.quit() so evidence lands even if quit() hangs.
        try {
            ExtentReportManager.flushDetailedReportsOnly();
        } catch (Throwable t) {
            // Never let report bookkeeping fail a test run.
            System.out.println("[ExtentReport] incremental flush failed (non-fatal): " + t);
        }

        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
                // A dead/unreachable browser must not turn into a config failure that
                // cascades into the rest of the suite's teardown.
            }
            driver = null;
        }
    }

    // ================================================================
    // TEST LEVEL SETUP/TEARDOWN (lightweight, per method)
    // ================================================================

    @BeforeMethod
    public void testSetup() {
        testStartTime = System.currentTimeMillis();
        recoverFromErrorPage();
        dismissBackdrops();
        // Auto-capture the starting page state on every test so the Detailed Report
        // shows what the test was looking at when it began.
        try {
            ExtentReportManager.captureScreenshot("Initial page state");
        } catch (Exception ignored) {}
    }

    /**
     * If the previous test left an error overlay or crash page,
     * recover before starting the next test.
     */
    private void recoverFromErrorPage() {
        try {
            // Check for unexpected login-page redirect FIRST.
            // This catches the scenario where the "app update" alert blocks sidebar
            // navigation and driver.get() redirects to login — which is NOT an
            // Application Error page, so isApplicationErrorPage() returns false.
            if (!isApplicationErrorPage()) {
                if (driver.findElements(By.xpath(
                    "//input[@id='email'] | //input[@type='email']"
                    + " | //input[@name='email']"
                    + " | //input[@placeholder='Email Address' or @placeholder='Email']"
                    + " | //input[@aria-label='Email Address' or @aria-label='Email']")).size() > 0) {
                    System.out.println("[BaseTest] Unexpected login page detected — session may have expired. Re-logging in...");
                    loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                    pause(2000);
                    dashboardPage.waitForDashboard();
                    waitAndDismissAppAlert();
                    selectTestSite();
                }
                return;
            }

            System.out.println("[BaseTest] Error page detected — recovering before next test...");

            // Strategy 1: Dismiss Sentry dialog
            try { driver.findElement(By.xpath("//button[contains(@aria-label,'Close')]")).click(); pause(500); } catch (Exception ignored) {}

            // Strategy 2: Navigate away using sidebar nav (preserves SPA state unlike page refresh).
            // The 'Locations' link is absent on the V1.36 setup console, so fall back to the direct
            // route in the same strategy rather than silently skipping to the full-reload path.
            try {
                By locationsNav = By.xpath("//span[normalize-space()='Locations'] | //a[normalize-space()='Locations']");
                java.util.List<WebElement> nav = driver.findElements(locationsNav);
                if (!nav.isEmpty()) {
                    nav.get(0).click();
                } else {
                    driver.get(AppConstants.BASE_URL + "/locations");
                }
                pause(2000);
                if (!isApplicationErrorPage()) {
                    System.out.println("[BaseTest] Recovery successful via nav to Locations");
                    return;
                }
            } catch (Exception ignored) {}

            // Strategy 3: Navigate to base URL (last resort — may require re-login)
            driver.get(AppConstants.BASE_URL);
            pause(3000);

            // Strategy 4: Click recovery buttons if still on error page
            if (isApplicationErrorPage()) {
                try { driver.findElement(By.xpath("//button[contains(.,'Try Again')]")).click(); pause(3000); } catch (Exception ignored) {}
                try { driver.findElement(By.xpath("//button[contains(.,'Refresh Page')]")).click(); pause(3000); } catch (Exception ignored) {}
            }

            // If we ended up on the login page, re-login
            if (driver.findElements(By.xpath(
                    "//input[@id='email'] | //input[@type='email']"
                    + " | //input[@name='email']"
                    + " | //input[@placeholder='Email Address' or @placeholder='Email']"
                    + " | //input[@aria-label='Email Address' or @aria-label='Email']")).size() > 0) {
                System.out.println("[BaseTest] Re-logging in after recovery...");
                loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                pause(2000);
                dashboardPage.waitForDashboard();
                ensureActiveRole(AppConstants.DEFAULT_ACTIVE_ROLE);
                selectTestSite();
            }
        } catch (Exception e) {
            System.out.println("[BaseTest] Recovery failed: " + e.getMessage());
        }
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;

        // Capture failure screenshot (driver is still alive — it persists across methods)
        if (result.getStatus() == ITestResult.FAILURE) {
            String screenshotName = result.getMethod().getMethodName() + "_FAIL";
            ScreenshotUtil.captureScreenshot(screenshotName);
            ExtentReportManager.logFailWithScreenshot(
                    "Test failed: " + result.getMethod().getMethodName(),
                    result.getThrowable());

            // AI-powered failure analysis — classifies the failure automatically
            if (driver != null && result.getThrowable() != null) {
                try {
                    String testName = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
                    SmartBugDetector.BugReport report =
                        SmartBugDetector.analyze(driver, testName, result.getThrowable(), duration);

                    // Use Case 1 from user spec: enrich with Claude root-cause + fix
                    // when ClaudeClient.isConfigured() and rule-based confidence is low.
                    com.egalvanic.qa.utils.ai.AIBugAnalyzer.analyze(report);

                    // Use Case 5 from user spec: write a copy-paste-ready bug file
                    // under ready-bug/. NEVER uploaded to Jira automatically.
                    String shotPath = "test-output/screenshots/" + screenshotName + ".png";
                    com.egalvanic.qa.utils.ai.ReadyBugGenerator.generate(report, shotPath);
                } catch (Exception e) {
                    System.out.println("[BaseTest] SmartBugDetector analysis failed: " + e.getMessage());
                }
            }
        } else if (result.getStatus() == ITestResult.SKIP) {
            ExtentReportManager.logSkip("Test skipped: " + result.getMethod().getMethodName());
        } else if (result.getStatus() == ITestResult.SUCCESS) {
            // Auto-capture the final page state on every PASS so the Detailed Report
            // always has a visual end-state, not just step screenshots.
            try {
                ExtentReportManager.captureScreenshot("Final page state");
            } catch (Exception ignored) {}

            // GLOBAL HEALTH GATE: every passing test is now also a JS-error and
            // failed-request detector. By default this only WARNS (so the suite
            // isn't flipped red before a baseline run); set STRICT_HEALTH_GATES=true
            // to make these hard failures. 3rd-party noise is whitelisted.
            runGlobalHealthGate(result);
        }

        ExtentReportManager.removeTests();

        System.out.println("Test completed in " + formatDuration(duration));
    }

    /**
     * Inspect captured browser errors + failed requests after a passing test.
     * WARN-only by default; flips the result to FAILURE when STRICT_HEALTH_GATES
     * is set. Never throws (a throw here would be a TestNG config failure that
     * skips later tests) — instead it mutates the ITestResult directly.
     */
    private void runGlobalHealthGate(ITestResult result) {
        if (driver == null) return;
        try {
            java.util.List<String> issues = new java.util.ArrayList<>();

            for (BrowserErrorCapture.JsError e : BrowserErrorCapture.getErrors(driver)) {
                issues.add("JS " + e);
            }
            for (AssetLoadVerifier.FailedRequest f : AssetLoadVerifier.getFailedRequests(driver)) {
                if (!isIgnoredHealth(f.url)) issues.add("NET " + f);
            }

            if (issues.isEmpty()) {
                // scope detection per-test so the next test starts clean
                BrowserErrorCapture.clear(driver);
                return;
            }

            StringBuilder sb = new StringBuilder("Health gate found ")
                    .append(issues.size()).append(" issue(s) on a passing test:");
            for (String i : issues) sb.append("\n  - ").append(i);
            String msg = sb.toString();

            if (AppConstants.STRICT_HEALTH_GATES) {
                result.setStatus(ITestResult.FAILURE);
                result.setThrowable(new AssertionError(msg));
                ExtentReportManager.logFail("[HealthGate-STRICT] " + msg);
                ScreenshotUtil.captureScreenshot(result.getMethod().getMethodName() + "_HEALTHGATE");
            } else {
                logWarning("[HealthGate] " + msg + "\n  (set STRICT_HEALTH_GATES=true to fail the build)");
            }
            BrowserErrorCapture.clear(driver);
        } catch (Exception e) {
            System.out.println("[BaseTest] Health gate check failed: " + e.getMessage());
        }
    }

    private boolean isIgnoredHealth(String url) {
        if (url == null) return false;
        String low = url.toLowerCase();
        for (String ig : AppConstants.HEALTH_GATE_IGNORE) {
            if (low.contains(ig)) return true;
        }
        return false;
    }

    // ================================================================
    // LOGIN AND SITE SELECTION HELPERS
    // ================================================================

    /**
     * Perform full login and site selection flow.
     * Includes retry logic for transient Application Error pages.
     */
    protected void loginAndSelectSite() {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                driver.get(AppConstants.BASE_URL);

                // Wait for page to fully load before checking for elements.
                // In CI, a fresh Chrome instance can take 30-60s to render the SPA.
                new WebDriverWait(driver, Duration.ofSeconds(60))
                        .until(d -> {
                            Object ready = ((JavascriptExecutor) d).executeScript("return document.readyState");
                            return "complete".equals(ready) || "interactive".equals(ready);
                        });
                pause(2000);

                // Check if already logged in (nav present, no login form) — can happen
                // when multiple test classes run sequentially sharing a browser session
                boolean hasNav = !driver.findElements(By.cssSelector("nav")).isEmpty();
                boolean hasLoginForm = !driver.findElements(By.xpath(
                    "//input[@id='email'] | //input[@type='email']"
                    + " | //input[@name='email']"
                    + " | //input[@placeholder='Email Address' or @placeholder='Email']"
                    + " | //input[@aria-label='Email Address' or @aria-label='Email']")).isEmpty();
                if (hasNav && !hasLoginForm) {
                    System.out.println("[BaseTest] Already logged in (nav present, no login form). URL: " + driver.getCurrentUrl());
                    waitAndDismissAppAlert();
                    selectTestSite();
                    return;
                }

                // Check for Application Error / "We encountered an error" page
                if (isApplicationErrorPage()) {
                    System.out.println("Error page on attempt " + attempt + " — recovering...");
                    // Dismiss Sentry feedback dialog if present
                    try { driver.findElement(By.xpath("//button[contains(@aria-label,'Close')]")).click(); pause(500); } catch (Exception ignored) {}
                    driver.navigate().refresh();
                    pause(3000);
                    if (isApplicationErrorPage()) {
                        try { driver.findElement(By.xpath("//button[contains(.,'Try Again')]")).click(); pause(3000); } catch (Exception ignored) {}
                        try { driver.findElement(By.xpath("//button[contains(.,'Refresh Page')]")).click(); pause(3000); } catch (Exception ignored) {}
                    }
                    if (isApplicationErrorPage()) {
                        driver.get(AppConstants.BASE_URL);
                        pause(3000);
                    }
                    if (isApplicationErrorPage() && attempt < maxRetries) {
                        continue;
                    }
                }

                // Wait for login page to load (use 60s on first attempt for cold CI starts).
                // May 2026 release dropped id="email" — use the same 5-way fallback the LoginPage uses.
                int loginTimeout = (attempt == 1) ? 60 : AppConstants.DEFAULT_TIMEOUT;
                new WebDriverWait(driver, Duration.ofSeconds(loginTimeout))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                                "//input[@id='email'] | //input[@type='email']"
                                + " | //input[@name='email']"
                                + " | //input[@placeholder='Email Address' or @placeholder='Email']"
                                + " | //input[@aria-label='Email Address' or @aria-label='Email']")));

                loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                pause(2000);

                // Check for error after login
                if (isApplicationErrorPage()) {
                    System.out.println("Application Error after login on attempt " + attempt);
                    if (attempt < maxRetries) continue;
                }

                dashboardPage.waitForDashboard();
                // Robustly dismiss the "app update available" alert.
                // In CI headless Chrome, the alert renders AFTER the dashboard shell
                // — the fire-and-forget JS in dismissBackdrops() misses it because
                // the DISMISS button doesn't exist in the DOM yet.
                // WebDriverWait polls repeatedly until the button appears (up to 10s).
                waitAndDismissAppAlert();
                ensureActiveRole(AppConstants.DEFAULT_ACTIVE_ROLE);
                selectTestSite();
                return; // success
            } catch (Exception e) {
                System.out.println("Login attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Login failed after " + maxRetries + " attempts", e);
                }
                pause(3000);
            }
        }
    }

    /**
     * Detect the "Application Error" crash page or Sentry error overlay
     */
    private boolean isApplicationErrorPage() {
        try {
            return driver.findElements(By.xpath(
                    "//*[contains(text(),'Application Error') or contains(text(),'We encountered an error') or contains(text(),'something went wrong')]"
            )).size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Select "Test Site" from the site dropdown
     */
    protected void selectTestSite() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // The facility selector uses placeholder='Select facility'
            By facilityInput = By.xpath("//input[@placeholder='Select facility']");

            // V1.36 (live 2026-08-05): the company dashboard NO LONGER carries the facility
            // selector — it only exists on site-scoped module pages (/assets, /slds, /sessions…).
            // Since login lands on /dashboard, the old 15s wait always timed out and site
            // selection was SILENTLY SKIPPED for every class ("Facility selector not found after
            // 15s"), leaving tests on a sticky/arbitrary site — the root of many data-dependent
            // search/create failures. Hop to /assets (facility-scoped) to make the selection.
            // Remember where we were: selectTestSite() is ALSO called from the mid-test recovery
            // paths, and hopping to /assets without coming back stranded the test on the wrong
            // page (testCreateWorkOrder searched the WO list while sitting on /assets, 2026-08-06).
            String routeBeforeSiteSelection = null;
            if (driver.findElements(facilityInput).isEmpty()) {
                routeBeforeSiteSelection = driver.getCurrentUrl();
                System.out.println("[BaseTest] No facility selector on " + routeBeforeSiteSelection
                        + " (V1.36 dashboard) — hopping to /assets to select the site");
                driver.get(AppConstants.BASE_URL + "/assets");
                pause(2000);
                try { reinstallHealthCapture(); } catch (Exception ignored) { }
            }
            final String returnRoute = routeBeforeSiteSelection;

            // Wait for facility input to appear (React may still be hydrating)
            try {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.presenceOfElementLocated(facilityInput));
            } catch (Exception waitTimeout) {
                System.out.println("Facility selector not found after 15s — skipping site selection");
                restoreRoute(returnRoute);
                return;
            }

            // Check if correct site is already selected
            String currentValue = driver.findElement(facilityInput).getAttribute("value");
            if (currentValue != null && currentValue.toLowerCase().contains(
                    AppConstants.TEST_SITE_NAME.toLowerCase())) {
                System.out.println("Correct site already selected: " + currentValue);
                restoreRoute(returnRoute);
                return;
            }
            if (currentValue != null && !currentValue.isEmpty()) {
                System.out.println("Wrong site selected: " + currentValue + " — switching to " + AppConstants.TEST_SITE_NAME);
            }

            // Click the facility input to open dropdown
            WebElement input = driver.findElement(facilityInput);
            input.click();
            pause(500);

            // Wait for listbox
            WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                w.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@role='listbox']")));
            } catch (Exception e) {
                System.out.println("Facility dropdown did not open — skipping");
                restoreRoute(returnRoute);
                return;
            }

            // Scroll and find the test site
            js.executeScript("document.querySelectorAll('ul[role=\"listbox\"]').forEach(e => e.scrollTop=e.scrollHeight);");
            pause(300);

            String siteNameLower = AppConstants.TEST_SITE_NAME.toLowerCase();
            By testSite = By.xpath("//li[@role='option'][contains("
                + "translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),"
                + "'" + siteNameLower + "')]");
            new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(ElementClickInterceptedException.class)
                    .until(d -> {
                        for (WebElement li : d.findElements(testSite)) {
                            try { li.click(); return li; } catch (Exception ignored) {}
                        }
                        return null;
                    });
            pause(500);
            System.out.println("Site selected: " + AppConstants.TEST_SITE_NAME);
            restoreRoute(returnRoute);
        } catch (Exception e) {
            System.out.println("Site selection skipped or failed: " + e.getMessage());
        }
    }

    /**
     * Navigate back to {@code route} after the /assets hop that site selection may need.
     * No-op when the hop never happened (route == null) or we are already there.
     */
    private void restoreRoute(String route) {
        if (route == null) return;
        try {
            if (driver.getCurrentUrl().equals(route)) return;
            System.out.println("[BaseTest] Returning to " + route + " after site selection");
            driver.get(route);
            pause(1500);
            try { reinstallHealthCapture(); } catch (Exception ignored) { }
        } catch (Exception e) {
            System.out.println("[BaseTest] Could not return to " + route + ": " + e.getMessage());
        }
    }

    /**
     * Select an arbitrary site/facility by name from the header "Site:" selector
     * (input[placeholder='Select facility']). Generalises {@link #selectTestSite()} so a
     * module can scope itself to a data-rich site (e.g. Opportunities -> "gyu", whose
     * facility name matches the site so create + grid data line up). Returns true if selected.
     */
    protected boolean selectSiteByName(String name) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            By facilityInput = By.xpath("//input[@placeholder='Select facility']");
            // Not every route carries the facility selector (V1.36 keeps it only on site-scoped
            // module pages — /opportunities has none). Hopping to /assets to make the selection and
            // then returning is what makes scoping work off-route; without it this returned false
            // and the caller's whole class ran unscoped, then skipped itself with "no data"
            // (Opportunities: 46 skips while the API held 146 quotes — 2026-08-06).
            String routeBeforeSiteSelection = null;
            if (driver.findElements(facilityInput).isEmpty()) {
                routeBeforeSiteSelection = driver.getCurrentUrl();
                System.out.println("[site] no facility selector on " + routeBeforeSiteSelection
                        + " — hopping to /assets to select '" + name + "'");
                driver.get(AppConstants.BASE_URL + "/assets");
                pause(2000);
                try { reinstallHealthCapture(); } catch (Exception ignored) { }
            }
            final String returnRoute = routeBeforeSiteSelection;
            try {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.presenceOfElementLocated(facilityInput));
            } catch (Exception waitTimeout) {
                System.out.println("[site] facility selector not found — cannot select '" + name + "'");
                restoreRoute(returnRoute);
                return false;
            }
            String currentValue = driver.findElement(facilityInput).getAttribute("value");
            if (currentValue != null && currentValue.toLowerCase().contains(name.toLowerCase())) {
                System.out.println("[site] already on '" + currentValue + "'");
                restoreRoute(returnRoute);
                return true;
            }
            WebElement input = driver.findElement(facilityInput);
            input.click();
            pause(500);
            // type to filter (the selector is searchable) — react-native value set + input event
            try {
                js.executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],arguments[1]);"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", input, name);
                pause(700);
            } catch (Exception ignored) {}
            WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                w.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@role='listbox']")));
            } catch (Exception e) {
                System.out.println("[site] dropdown did not open for '" + name + "'");
                restoreRoute(returnRoute);
                return false;
            }
            String lower = name.toLowerCase();
            By option = By.xpath("//li[@role='option'][contains("
                + "translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),"
                + "'" + lower + "')]");
            WebElement picked = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(ElementClickInterceptedException.class)
                    .until(d -> {
                        for (WebElement li : d.findElements(option)) {
                            try { li.click(); return li; } catch (Exception ignored) {}
                        }
                        return null;
                    });
            pause(600);
            System.out.println("[site] selected '" + name + "'" + (picked != null ? "" : " (no exact option?)"));
            restoreRoute(returnRoute);
            return picked != null;
        } catch (Exception e) {
            System.out.println("[site] selectSiteByName('" + name + "') failed: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // HELPER / LOGGING METHODS
    // ================================================================

    protected void logStep(String message) {
        ExtentReportManager.logInfo(message);
    }

    protected void logStepWithScreenshot(String message) {
        ExtentReportManager.logStepWithScreenshot(message);
    }

    protected void logWarning(String message) {
        ExtentReportManager.logWarning(message);
    }

    // Global pause scaling — lets CI shorten every Thread.sleep in the suite
    // from one knob without touching the ~700 call sites. -Dpause.scale / PAUSE_SCALE
    // is clamped to (0.1 .. 1.0]: it can only ever speed the suite up (never slow it
    // down), and never collapses a pause to ~0. Default 1.0 = no behaviour change.
    private static final double PAUSE_SCALE = resolvePauseScale();

    private static double resolvePauseScale() {
        String v = System.getProperty("pause.scale");
        if (v == null || v.isEmpty()) v = System.getenv("PAUSE_SCALE");
        if (v == null || v.isEmpty()) return 1.0;
        try {
            double d = Double.parseDouble(v.trim());
            return Math.max(0.1, Math.min(1.0, d));
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    protected void pause(long ms) {
        long scaled = PAUSE_SCALE == 1.0 ? ms : (long) Math.ceil(ms * PAUSE_SCALE);
        try {
            Thread.sleep(scaled);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    protected void shortWait() { pause(200); }
    protected void mediumWait() { pause(400); }
    protected void longWait() { pause(800); }

    // ================================================================
    // APP UPDATE ALERT — robust WebDriverWait-based dismissal
    // ================================================================

    /**
     * Robustly dismiss the "App Update Available" alert that appears asynchronously
     * in the QA environment. This alert blocks the site selector and page interactions.
     *
     * Unlike the fire-and-forget JS in dismissBackdrops() (which runs once and returns
     * immediately), this method uses WebDriverWait to POLL for the DISMISS button
     * over 10 seconds. This catches the alert even when React renders it AFTER the
     * dashboard shell — the #1 cause of CI failures in headless Chrome.
     *
     * Flow:
     *   1. Wait up to 10s for DISMISS button (catches late-rendering alerts)
     *   2. Click it via Selenium (reliable cross-browser)
     *   3. Pause 1s for React to re-render after alert dismissal
     *   4. Run fire-and-forget dismissBackdrops() as safety net
     *
     * Call this after login and after any driver.get() / driver.navigate().refresh()
     * that triggers a full page reload — those re-initialize the React app state
     * including the update alert.
     */
    protected void waitAndDismissAppAlert() {
        // Step 1: Poll for the DISMISS button (catches late-rendering alerts). The wait used to be
        // a fixed 10s elementToBeClickable — which blocked the FULL 10s in the common no-alert
        // case, costing ~10s × every class setup per run. A short presence-poll keeps the CI
        // late-render coverage without the constant tax; APP_ALERT_WAIT_SECONDS overrides for
        // environments where the alert renders slower.
        int waitSecs = Integer.parseInt(System.getProperty("APP_ALERT_WAIT_SECONDS",
                System.getenv().getOrDefault("APP_ALERT_WAIT_SECONDS", "4")));
        try {
            By dismiss = By.xpath("//button[text()='DISMISS']");
            long deadline = System.currentTimeMillis() + waitSecs * 1000L;
            while (System.currentTimeMillis() < deadline) {
                java.util.List<WebElement> btns = driver.findElements(dismiss);
                if (!btns.isEmpty() && btns.get(0).isDisplayed()) {
                    btns.get(0).click();
                    System.out.println("[BaseTest] Dismissed app update alert via Selenium click");
                    pause(1000); // let React re-render after alert dismissal
                    break;
                }
                pause(500);
            }
        } catch (Exception e) {
            // No alert / stale click — normal; fall through to backdrop cleanup.
        }
        // Step 2: Always run fire-and-forget cleanup as safety net
        // (removes any residual MUI backdrops, Beamer overlays, etc.)
        dismissBackdrops();
    }

    // ================================================================
    // ACTIVE-ROLE PINNING (V1.36 dual-console fix)
    // ================================================================

    /** The five roles the header Role Autocomplete offers on this tenant (V1.36). */
    private static final String[] HEADER_ROLE_NAMES =
            {"Admin", "Project Manager", "Account Manager", "Super Admin", "Electrical Engineer"};

    /**
     * Pin the header Role dropdown to {@code target} right after login.
     *
     * <p><b>No longer functional as of the V1.36 nav redesign — verified 2026-08-24.</b> The
     * role switcher has been REMOVED from the UI: the account menu (the avatar at the foot of
     * the icon rail) now renders Roles as a read-only {@code <p>} — "Project Manager, Account
     * Manager, Electrical Engineer, Super Admin, Admin" — with no Autocomplete, Select or
     * combobox anywhere in the popover. The only controls left are Edit Company, Email
     * Preferences, the English/Français toggle and Sign Out.
     *
     * <p>So this method now always logs "Role switcher not found" and returns. It is kept
     * rather than deleted because it is tolerant by contract (it must never fail a suite by
     * itself) and because the control may return; deleting it would silently drop the pin from
     * every caller. <b>Do not rely on it to make the landing console deterministic.</b>
     *
     * <p>The dual-console problem it existed to solve is also gone: with no role switching, a
     * multi-role admin now gets all six rail categories at once. What replaced it as the real
     * navigation hazard is the two-level rail — see {@link com.egalvanic.qa.utils.NavCatalog}.
     */
    protected void ensureActiveRole(String target) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String namesJs = "['" + String.join("','", HEADER_ROLE_NAMES) + "']";
            String readRole =
                    "var names=" + namesJs + ";"
                  + "var ins=document.querySelectorAll('input');"
                  + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0) return ins[i].value; }"
                  + "return null;";
            // The role input renders with the header shell; poll briefly for it.
            Object current = null;
            for (int i = 0; i < 10 && current == null; i++) {
                current = js.executeScript(readRole);
                if (current == null) pause(500);
            }
            if (current == null) {
                System.out.println("[BaseTest] Role switcher not found — leaving active role as-is");
                return;
            }
            if (target.equals(current)) {
                System.out.println("[BaseTest] Active role already '" + target + "'");
                return;
            }
            System.out.println("[BaseTest] Active role is '" + current + "' — switching to '" + target + "'");
            String openScript =
                    "var names=" + namesJs + ";"
                  + "var input=null, ins=document.querySelectorAll('input');"
                  + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0){ input=ins[i]; break; } }"
                  + "if(!input) return false;"
                  + "input.scrollIntoView({block:'center'}); input.focus();"
                  + "var w=input.closest('.MuiAutocomplete-root');"
                  + "var b=w?w.querySelector('.MuiAutocomplete-popupIndicator'):null;"
                  + "if(b) b.click(); else input.click(); return true;";
            // Options render async — reopen + poll for the target option (mirrors the proven
            // AccountV135RegressionTestNG.switchRole flow).
            boolean clicked = false;
            for (int attempt = 1; attempt <= 3 && !clicked; attempt++) {
                js.executeScript(openScript);
                for (int i = 0; i < 10 && !clicked; i++) {
                    pause(700);
                    Object done = js.executeScript(
                            "var t=arguments[0];"
                          + "var opts=document.querySelectorAll(\"li[role='option']\");"
                          + "for (var i=0;i<opts.length;i++){"
                          + "  if(opts[i].textContent.trim()===t){"
                          + "    ['pointerdown','mousedown','pointerup','mouseup','click'].forEach(function(ev){"
                          + "      opts[i].dispatchEvent(new MouseEvent(ev,{bubbles:true,cancelable:true}));});"
                          + "    return true; } }"
                          + "return false;", target);
                    clicked = Boolean.TRUE.equals(done);
                }
            }
            if (!clicked) {
                System.out.println("[BaseTest] Role option '" + target + "' never offered — continuing on '" + current + "'");
                return;
            }
            // The switch reloads the app shell — poll until the switcher reflects the target.
            for (int i = 0; i < 30; i++) {
                pause(1000);
                try { if (target.equals(js.executeScript(readRole))) break; } catch (Exception ignored) { }
            }
            pause(2000);
            waitAndDismissAppAlert();
            // The shell reload dropped the JS health hooks — reinstall for the health gates.
            try { reinstallHealthCapture(); } catch (Exception ignored) { }
            System.out.println("[BaseTest] Active role now: " + js.executeScript(readRole)
                    + " @ " + driver.getCurrentUrl());
        } catch (Exception e) {
            System.out.println("[BaseTest] ensureActiveRole failed (non-fatal): " + e.getMessage());
        }
    }

    // ================================================================
    // MUI BACKDROP HANDLING — prevents ElementClickInterceptedException
    // ================================================================

    /**
     * Remove all MUI backdrop overlays that intercept clicks in headless Chrome.
     * Called automatically before every test via @BeforeMethod.
     *
     * NOTE: This is fire-and-forget — it runs the JS once and returns immediately.
     * If the DISMISS button hasn't rendered yet (common in CI), it will miss it.
     * For reliable alert dismissal after login or page reload, use
     * waitAndDismissAppAlert() instead.
     */
    protected void dismissBackdrops() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "/* Remove MUI backdrops */" +
                "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop').forEach(" +
                "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                ");" +
                "/* Remove Beamer notification overlay — blocks clicks on CI */" +
                "var beamer = document.getElementById('beamerOverlay');" +
                "if (beamer) { beamer.style.display = 'none'; beamer.style.pointerEvents = 'none'; }" +
                "document.querySelectorAll('[id^=\"beamer\"], .beamer_show').forEach(" +
                "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                ");" +
                "/* Dismiss 'app update available' alert banner — blocks site selector in CI */" +
                "var btns = document.querySelectorAll('button');" +
                "for (var i = 0; i < btns.length; i++) {" +
                "  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }" +
                "}"
            );
        } catch (Exception ignored) {}
    }

    /**
     * Safe click that removes MUI backdrops first, then tries Selenium click
     * with JS click fallback. Handles both "click intercepted" (backdrop over
     * element) AND "element not interactable" (element hidden / disabled /
     * overlapped) — the latter was the root cause of the 28-test "element
     * not interactable" cascade in Location + Task in run 26442766019.
     */
    /**
     * Capture a screenshot after a click action, gated by AUTO_SCREENSHOT_EVERY_CLICK.
     * Quietly no-ops if disabled or if ExtentReport has no active test.
     */
    private void postClickScreenshot() {
        if (!AppConstants.AUTO_SCREENSHOT_EVERY_CLICK) return;
        try {
            ExtentReportManager.captureScreenshot(null);
        } catch (Exception ignored) {}
    }

    protected void safeClick(WebElement element) {
        dismissBackdrops();
        try {
            element.click();
            postClickScreenshot();
            return;
        } catch (org.openqa.selenium.ElementNotInteractableException e) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            try {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
                dismissBackdrops();
                pause(300);
                js.executeScript("arguments[0].click();", element);
                postClickScreenshot();
            } catch (Exception ignored) {
                try {
                    js.executeScript(
                        "var ev = new MouseEvent('click', {bubbles:true, cancelable:true, view:window});" +
                        "arguments[0].dispatchEvent(ev);", element);
                    postClickScreenshot();
                } catch (Exception ignored2) {}
            }
        }
    }

    /**
     * Safe click by locator — finds element, removes backdrops, clicks with fallback.
     */
    protected void safeClick(By locator) {
        dismissBackdrops();
        WebElement element = driver.findElement(locator);
        try {
            element.click();
            postClickScreenshot();
            return;
        } catch (org.openqa.selenium.ElementNotInteractableException e) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            try {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
                dismissBackdrops();
                pause(300);
                js.executeScript("arguments[0].click();", element);
                postClickScreenshot();
            } catch (Exception ignored) {
                try {
                    js.executeScript(
                        "var ev = new MouseEvent('click', {bubbles:true, cancelable:true, view:window});" +
                        "arguments[0].dispatchEvent(ev);", element);
                    postClickScreenshot();
                } catch (Exception ignored2) {}
            }
        }
    }
}
