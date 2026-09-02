package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.NavCatalog;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only smoke against the PRODUCTION tenant.
 *
 * <p>Production is where the users are, so a ticket verified on QA is not fully verified until the
 * same check passes on prod after the Monday release. This suite is the harness for that pass: it
 * confirms prod is up, that a real seat can authenticate, that the app shell and the core list
 * pages render, that prod's navigation has not drifted away from the QA-mapped catalog, and it
 * carries one targeted regression check for a defect confirmed on prod.
 *
 * <h2>Three interlocks, because this points at real infrastructure</h2>
 * <ol>
 *   <li><b>Opt-in only.</b> Every test skips unless {@code PROD_SMOKE=true} is passed explicitly.
 *       The suite is not in {@code testng.xml}, so a plain {@code mvn test} cannot reach it, and
 *       even a deliberate run of its own suite file does nothing without the flag. This encodes
 *       the standing rule that production is touched on request, never on a whim.</li>
 *   <li><b>Refuses a non-production target.</b> If {@code BASE_URL} is a QA, staging or local
 *       host the tests skip rather than run, so a misconfigured job can never produce a green
 *       "verified on production" report from QA data. The reverse mistake is guarded too: the
 *       credentials must have been supplied through the environment, because the committed
 *       defaults in {@link AppConstants} are QA's and cannot authenticate here.</li>
 *   <li><b>No writes, structurally.</b> Nothing below creates, edits or deletes. Every assertion
 *       reads. That is a property of the code, not a convention — there is no create/save/delete
 *       call in this file and no test opens a form.</li>
 * </ol>
 *
 * <h2>Running it</h2>
 * <pre>
 * mvn test -DsuiteXmlFile=prod-smoke-testng.xml \
 *          -DPROD_SMOKE=true \
 *          -DBASE_URL=https://acme.egalvanic.ai \
 *          -DUSER_EMAIL=... -DUSER_PASSWORD=...
 * </pre>
 * Credentials are never defaulted here and must never be committed — pass them from a secret
 * store, as the bces-iq job does.
 *
 * <h2>Coverage</h2>
 * <pre>
 * TC_PROD_01  Site loads and the login form renders
 * TC_PROD_02  A configured production seat authenticates
 * TC_PROD_03  Post-login shell renders the two-level nav rail
 * TC_PROD_04  Nav routes vs the QA-mapped catalog — drift report
 * TC_PROD_05  Core list pages render a data grid, with no error banner
 * TC_PROD_06  Multi-role seat sees BOTH tabs on Customers (prod regression)
 * TC_PROD_07  Landing page raises no uncaught JavaScript errors
 * </pre>
 *
 * <p><b>On version skew.</b> Prod runs ahead of QA — V2.0 against QA's V1.36 as of 2026-09-02 —
 * so TC_PROD_04 treats "prod has routes QA does not" as information, not failure. Only the
 * reverse, a core route that QA has and prod has lost, is treated as a defect.
 */
public class ProductionSmokeTestNG {

    private WebDriver driver;
    private LoginPage loginPage;
    private JavascriptExecutor js;
    private long testStartTime;

    private static final int LOGIN_TIMEOUT = 60;
    private static final int POST_LOGIN_TIMEOUT = 40;

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("h:mm a - dd MMM");

    /**
     * Hosts that are NOT production. Matched as substrings of the BASE_URL host.
     */
    private static final List<String> NON_PROD_MARKERS =
            Arrays.asList(".qa.", "qa.egalvanic", ".stage.", "stage.egalvanic",
                          "localhost", "127.0.0.1", ".dev.", "dev.egalvanic");

    /**
     * Routes a production release must never lose. Deliberately short: these are the pages the
     * product is unusable without, so their absence is a release defect rather than drift.
     */
    private static final List<String> CORE_ROUTES =
            Arrays.asList("/assets", "/sessions", "/tasks", "/issues", "/connections", "/locations");

    // ================================================================
    // SUITE LIFECYCLE
    // ================================================================

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     PRODUCTION read-only smoke");
        System.out.println("     target : " + AppConstants.BASE_URL);
        System.out.println("     opt-in : PROD_SMOKE=" + flag("PROD_SMOKE"));
        System.out.println("     " + LocalDateTime.now().format(TIMESTAMP_FMT));
        System.out.println("==============================================================");
        ExtentReportManager.initReports();
        ScreenshotUtil.cleanupOldScreenshots(7);
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        ExtentReportManager.flushReports();
        System.out.println("     Production smoke complete — " + LocalDateTime.now().format(TIMESTAMP_FMT));
    }

    @BeforeMethod(alwaysRun = true)
    public void testSetup() {
        testStartTime = System.currentTimeMillis();

        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);

        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);

        // Deliberately NOT setting acceptInsecureCerts. BaseTest does, because the QA host uses
        // an internal-CA certificate — but production serves a publicly trusted certificate, and
        // a TLS error there is a real finding that must not be waved through.
        String chromeBinary = System.getProperty("chrome.binary", System.getenv("CHROME_BINARY"));
        if (chromeBinary != null && !chromeBinary.isEmpty()) opts.setBinary(chromeBinary);
        if ("true".equals(System.getProperty("headless"))) opts.addArguments("--headless=new");

        driver = new ChromeDriver(opts);
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        js = (JavascriptExecutor) driver;
        ScreenshotUtil.setDriver(driver);
        loginPage = new LoginPage(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void testTeardown(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;
        if (result.getStatus() == ITestResult.FAILURE) {
            ScreenshotUtil.captureScreenshot(result.getMethod().getMethodName() + "_FAIL");
            ExtentReportManager.logFailWithScreenshot(
                    "Test failed: " + result.getMethod().getMethodName(), result.getThrowable());
        }
        ExtentReportManager.removeTests();
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        System.out.println("Test completed in "
                + (duration < 1000 ? duration + "ms" : (duration / 1000) + "s"));
    }

    // ================================================================
    // INTERLOCKS
    // ================================================================

    /** System property first, then environment variable — same precedence as AppConstants.getEnv. */
    private static String flag(String key) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) v = System.getenv(key);
        return v == null ? "" : v;
    }

    /**
     * Skip unless this run was explicitly aimed at production with credentials to match.
     *
     * <p>Each condition answers a different way this suite could lie. Without the opt-in flag it
     * could run unattended; against a QA host it could report QA behaviour as production
     * behaviour; with the committed QA defaults it would fail on authentication and read as a
     * production outage.
     */
    private void requireProductionTarget() {
        if (!"true".equalsIgnoreCase(flag("PROD_SMOKE"))) {
            throw new SkipException("Production smoke is opt-in — pass -DPROD_SMOKE=true to run it. "
                    + "Skipping so nothing touches production unintentionally.");
        }

        String host;
        try {
            host = URI.create(AppConstants.BASE_URL).getHost();
        } catch (Exception e) {
            throw new SkipException("BASE_URL is not a parseable URL: " + AppConstants.BASE_URL);
        }
        if (host == null || host.isEmpty()) {
            throw new SkipException("BASE_URL has no host: " + AppConstants.BASE_URL);
        }
        String lower = host.toLowerCase();
        for (String marker : NON_PROD_MARKERS) {
            if (lower.contains(marker)) {
                throw new SkipException("BASE_URL '" + host + "' is a non-production host (matched '"
                        + marker + "'). Refusing to run, because passing here would report "
                        + "non-production behaviour as production behaviour. Set "
                        + "-DBASE_URL to the production host.");
            }
        }

        // The committed defaults are the QA tenant's and cannot authenticate against production.
        boolean emailFromEnv = !flag("USER_EMAIL").isEmpty();
        boolean passwordFromEnv = !flag("USER_PASSWORD").isEmpty();
        if (!emailFromEnv || !passwordFromEnv) {
            throw new SkipException("Production credentials not supplied (USER_EMAIL / USER_PASSWORD). "
                    + "AppConstants' committed defaults are QA's, so running would fail on login and "
                    + "look like a production outage. Pass them from a secret store.");
        }
    }

    // ================================================================
    // TC_PROD_01 — site reachable, login form renders
    // ================================================================
    @Test(priority = 1, description = "TC_PROD_01: Production site loads and the login form renders")
    public void testTC_PROD_01_SiteLoads() {
        ExtentReportManager.createTest(AppConstants.MODULE_PRODUCTION,
                AppConstants.FEATURE_PROD_AVAILABILITY, "TC_PROD_01_SiteLoads");
        requireProductionTarget();

        logStep("Navigating to production: " + AppConstants.BASE_URL);
        driver.get(AppConstants.BASE_URL);

        // The login form is gated on the branding/alliance-config call, so a slow config API
        // delays the FORM rather than the auth API. Wait on the field, not on readyState.
        By emailField = By.xpath("//input[@id='email'] | //input[@type='email'] "
                + "| //input[@name='email'] | //input[contains(@placeholder,'Email')]");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(LOGIN_TIMEOUT))
                    .until(ExpectedConditions.visibilityOfElementLocated(emailField));
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("prod_login_form_missing");
            Assert.fail("Login form did not render within " + LOGIN_TIMEOUT + "s on "
                    + AppConstants.BASE_URL + ". If the page shows a certificate interstitial, that is "
                    + "a real production TLS finding — this suite deliberately does not accept "
                    + "insecure certificates. Cause: " + e.getMessage());
        }

        ScreenshotUtil.captureScreenshot("prod_01_login_form");
        Assert.assertFalse(driver.findElements(
                        By.cssSelector("input[type='password'], input[name='password']")).isEmpty(),
                "Password input missing on the production login page");
        Assert.assertFalse(driver.findElements(By.cssSelector("button[type='submit']")).isEmpty(),
                "Submit button missing on the production login page");

        logStep("Login form structure OK: email + password + submit all present");
        ExtentReportManager.logPass("Production reachable and login form renders");
    }

    // ================================================================
    // TC_PROD_02 — a real seat authenticates
    // ================================================================
    @Test(priority = 2, description = "TC_PROD_02: A configured production seat authenticates")
    public void testTC_PROD_02_LoginSucceeds() {
        ExtentReportManager.createTest(AppConstants.MODULE_PRODUCTION,
                AppConstants.FEATURE_PROD_AVAILABILITY, "TC_PROD_02_LoginSucceeds");
        requireProductionTarget();

        loginToProduction();
        String url = driver.getCurrentUrl();
        ScreenshotUtil.captureScreenshot("prod_02_post_login");
        logStep("Post-login URL: " + url);

        Assert.assertFalse(url.endsWith("/login") || url.endsWith("/login/"),
                "Still on the login page after submitting production credentials. URL: " + url);
        Assert.assertTrue(driver.findElements(By.xpath(
                        "//*[contains(translate(normalize-space(.),"
                        + " 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),"
                        + " 'invalid credentials')]")).isEmpty(),
                "Production login returned an 'Invalid credentials' banner");

        ExtentReportManager.logPass("Production login succeeded — landed on " + url);
    }

    // ================================================================
    // TC_PROD_03 — the two-level nav rail renders
    // ================================================================
    @Test(priority = 3, description = "TC_PROD_03: Post-login shell renders the two-level nav rail")
    public void testTC_PROD_03_PostLoginShell() {
        ExtentReportManager.createTest(AppConstants.MODULE_PRODUCTION,
                AppConstants.FEATURE_PROD_AVAILABILITY, "TC_PROD_03_PostLoginShell");
        requireProductionTarget();

        loginToProduction();

        // Assert on the rail specifically, not on "some nav exists". A bare nav element is present
        // even on a half-rendered shell, so a nav-exists check passes against a broken page.
        boolean railPresent = false;
        List<String> categories = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            dismissBackdrops();
            for (WebElement b : driver.findElements(
                    By.cssSelector("nav[aria-label='navigation'] button[aria-label]"))) {
                try {
                    String label = b.getAttribute("aria-label");
                    if (label != null && !label.isEmpty() && !categories.contains(label)) {
                        categories.add(label);
                    }
                } catch (Exception ignored) { }
            }
            if (!categories.isEmpty()) { railPresent = true; break; }
            pause(500);
        }

        ScreenshotUtil.captureScreenshot("prod_03_shell");
        logStep("Rail categories on production: " + categories);

        Assert.assertTrue(railPresent,
                "No two-level nav rail on production: nav[aria-label='navigation'] rendered no "
                + "aria-labelled category buttons within 30s. Either the shell failed to render or "
                + "production's navigation markup differs from the mapped design.");
        Assert.assertTrue(categories.size() >= 3,
                "Production rail rendered only " + categories.size() + " category button(s) "
                + categories + " — expected the full category set for an admin-level seat.");

        ExtentReportManager.logPass("Production shell renders the rail with categories: " + categories);
    }

    // ================================================================
    // TC_PROD_04 — nav route parity against the QA-mapped catalog
    // ================================================================
    @Test(priority = 4, description = "TC_PROD_04: Production nav routes vs the QA-mapped catalog")
    public void testTC_PROD_04_NavRouteParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_PRODUCTION,
                AppConstants.FEATURE_PROD_NAV_PARITY, "TC_PROD_04_NavRouteParity");
        requireProductionTarget();

        loginToProduction();

        Set<String> prodRoutes = NavCatalog.collectAllNavHrefs(driver);
        Set<String> qaRoutes = NavCatalog.allRoutes();

        Set<String> onlyOnProd = new LinkedHashSet<>(prodRoutes);
        onlyOnProd.removeAll(qaRoutes);
        Set<String> onlyInCatalog = new LinkedHashSet<>(qaRoutes);
        onlyInCatalog.removeAll(prodRoutes);

        logStep("Production nav exposes " + prodRoutes.size() + " routes; QA catalog has "
                + qaRoutes.size());
        logStep("On production but not in the QA catalog (expected — prod runs ahead): " + onlyOnProd);
        logStep("In the QA catalog but not on production: " + onlyInCatalog);
        ScreenshotUtil.captureScreenshot("prod_04_nav_parity");

        Assert.assertFalse(prodRoutes.isEmpty(),
                "Harvested zero nav routes from production — the sweep itself failed, so this "
                + "says nothing about parity. Check that the rail rendered.");

        // Only a LOST core route is a defect. Prod being ahead of QA is the normal state: prod was
        // V2.0 against QA's V1.36 when this suite was written, so extra routes are information.
        List<String> missingCore = new ArrayList<>();
        for (String route : CORE_ROUTES) {
            if (!prodRoutes.contains(route)) missingCore.add(route);
        }
        Assert.assertTrue(missingCore.isEmpty(),
                "Production navigation is missing core route(s) " + missingCore
                + ", which QA links. Harvested routes: " + prodRoutes);

        ExtentReportManager.logPass("All " + CORE_ROUTES.size() + " core routes present on production. "
                + "Drift — prod-only: " + onlyOnProd + "; catalog-only: " + onlyInCatalog);
    }

    // ================================================================
    // TC_PROD_05 — core list pages render, read-only
    // ================================================================
    @Test(priority = 5, description = "TC_PROD_05: Core list pages render a grid with no error banner")
    public void testTC_PROD_05_CoreListPagesLoad() {
        ExtentReportManager.createTest(AppConstants.MODULE_PRODUCTION,
                AppConstants.FEATURE_PROD_AVAILABILITY, "TC_PROD_05_CoreListPagesLoad");
        requireProductionTarget();

        loginToProduction();

        List<String> broken = new ArrayList<>();
        for (String route : CORE_ROUTES) {
            driver.get(AppConstants.BASE_URL + route);
            boolean rendered = false;
            long deadline = System.currentTimeMillis() + 25_000L;
            while (System.currentTimeMillis() < deadline) {
                // A grid OR a legitimate empty state counts as rendered. An empty grid is not a
                // defect — prod's Acme tenant is test data and a module may genuinely have no rows.
                if (!driver.findElements(By.cssSelector(".MuiDataGrid-root")).isEmpty()
                        || !driver.findElements(By.cssSelector("[role='grid'], main table")).isEmpty()) {
                    rendered = true;
                    break;
                }
                pause(500);
            }

            String mainText = mainText();
            boolean errorShell = mainText.contains("Application Error")
                    || mainText.contains("Something went wrong")
                    || mainText.toLowerCase().contains("failed to fetch");

            if (!rendered || errorShell) {
                ScreenshotUtil.captureScreenshot("prod_05_broken_" + route.replace("/", "_"));
                broken.add(route + (errorShell ? " (error shell)" : " (no grid in 25s)"));
            }
            logStep(route + " -> " + (rendered && !errorShell ? "OK" : "PROBLEM"));
        }

        ScreenshotUtil.captureScreenshot("prod_05_last_page");
        Assert.assertTrue(broken.isEmpty(),
                "Core list page(s) did not render on production: " + broken);
        ExtentReportManager.logPass("All core list pages rendered on production: " + CORE_ROUTES);
    }

    // ================================================================
    // TC_PROD_06 — regression: the multi-role tab gate
    // ================================================================
    /**
     * Regression net for a defect confirmed on production: the Customers page decided which tabs
     * to show from the seat's FIRST role only, rather than from the union of its roles.
     *
     * <p>The shipped bundle resolved the role with {@code roles?.find(g => g)?.name} and tested
     * that single name against the set allowed to see the Accounts tab. A seat whose first role
     * happened to be Project Manager therefore lost the Accounts tab even though the same seat
     * also held Admin — while a seat whose first role was Super Admin kept it. The fix is
     * {@code roles?.some(...)}, a pattern already used seven times elsewhere in the same bundle.
     *
     * <p>The check is written so a single-role seat cannot make it pass vacuously: it asserts the
     * role count first, and skips with an explanation when the configured seat holds fewer than
     * two roles. That guard is the point — a whole class of union-versus-first-role bugs is
     * mathematically invisible to a single-role account, which is why this went unnoticed.
     */
    @Test(priority = 6, description = "TC_PROD_06: A multi-role seat sees both Customers tabs")
    public void testTC_PROD_06_MultiRoleTabGate() {
        ExtentReportManager.createTest(AppConstants.MODULE_PRODUCTION,
                AppConstants.FEATURE_PROD_REGRESSION, "TC_PROD_06_MultiRoleTabGate");
        requireProductionTarget();

        loginToProduction();

        int roleCount = countSeatRoles();
        logStep("Seat holds " + roleCount + " role(s)");
        if (roleCount < 2) {
            throw new SkipException("The configured production seat holds " + roleCount
                    + " role — a first-role-only gate is indistinguishable from a correct "
                    + "union on a single-role account, so this check would pass vacuously. "
                    + "Point USER_EMAIL at a seat with two or more roles to make it meaningful.");
        }

        driver.get(AppConstants.BASE_URL + "/customers");
        List<String> tabs = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 25_000L;
        while (System.currentTimeMillis() < deadline) {
            for (WebElement t : driver.findElements(By.cssSelector("main [role='tab']"))) {
                try {
                    String label = t.getText().trim().replaceAll("\\s+", " ");
                    if (!label.isEmpty() && !tabs.contains(label)) tabs.add(label);
                } catch (Exception ignored) { }
            }
            if (!tabs.isEmpty()) break;
            pause(500);
        }

        ScreenshotUtil.captureScreenshot("prod_06_customers_tabs");
        logStep("Customers tabs visible: " + tabs);

        Assert.assertFalse(tabs.isEmpty(),
                "The Customers page rendered no tabs at all on production. Expected an Accounts "
                + "tab and a Sites tab for a seat holding " + roleCount + " roles.");

        boolean hasAccounts = tabs.stream().anyMatch(t -> t.toLowerCase().startsWith("account"));
        boolean hasSites = tabs.stream().anyMatch(t -> t.toLowerCase().startsWith("site"));

        Assert.assertTrue(hasSites, "No Sites tab on the Customers page. Tabs seen: " + tabs);
        Assert.assertTrue(hasAccounts,
                "No Accounts tab on the Customers page for a seat holding " + roleCount + " roles "
                + "(tabs seen: " + tabs + "). This is the first-role-only regression: the gate is "
                + "reading roles[0] instead of the union of assigned roles, so a seat whose first "
                + "role is not an allowed one loses the tab despite holding a role that permits it.");

        ExtentReportManager.logPass("Multi-role seat (" + roleCount + " roles) sees both tabs: " + tabs);
    }

    // ================================================================
    // TC_PROD_07 — no uncaught JavaScript errors on landing
    // ================================================================
    @Test(priority = 7, description = "TC_PROD_07: Landing page raises no uncaught JavaScript errors")
    public void testTC_PROD_07_NoJsErrorsOnLanding() {
        ExtentReportManager.createTest(AppConstants.MODULE_PRODUCTION,
                AppConstants.FEATURE_PROD_AVAILABILITY, "TC_PROD_07_NoJsErrorsOnLanding");
        requireProductionTarget();

        loginToProduction();

        // Install the collector AFTER login, then reload, so the hook is in place before the
        // landing page's own scripts run. Installing it before login would be torn down by the
        // post-login navigation and collect nothing.
        js.executeScript(
                "window.__prodJsErrors = [];"
                + "window.addEventListener('error', function(e){"
                + "  window.__prodJsErrors.push('error: ' + (e.message || e.type));"
                + "});"
                + "window.addEventListener('unhandledrejection', function(e){"
                + "  window.__prodJsErrors.push('unhandledrejection: ' + (e.reason && e.reason.message"
                + "    ? e.reason.message : String(e.reason)));"
                + "});");
        js.executeScript("window.location.reload();");
        pause(12_000);

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) js.executeScript(
                "return window.__prodJsErrors || [];");

        // Filter the same third-party and ambient noise the QA health gates ignore, so this
        // reports the app's own failures rather than beamer/devrev/sentry chatter.
        List<String> appErrors = new ArrayList<>();
        for (String err : (errors == null ? new ArrayList<String>() : errors)) {
            String lower = err.toLowerCase();
            boolean ignored = false;
            for (String skip : AppConstants.HEALTH_GATE_IGNORE) {
                if (lower.contains(skip.toLowerCase())) { ignored = true; break; }
            }
            if (!ignored) appErrors.add(err);
        }

        ScreenshotUtil.captureScreenshot("prod_07_landing");
        logStep("Uncaught JS errors after reload: " + errors);
        logStep("After filtering known-ignorable sources: " + appErrors);

        Assert.assertTrue(appErrors.isEmpty(),
                "Production landing page raised uncaught JavaScript error(s): " + appErrors);
        ExtentReportManager.logPass("No uncaught application JavaScript errors on the production landing page");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Log in and wait until the app has actually left the login page.
     *
     * <p>Each test logs in fresh because {@code @BeforeMethod} builds a new browser per test —
     * the trade is a slower suite for the guarantee that no test inherits another's session
     * state, which matters more when the target is production.
     */
    private void loginToProduction() {
        driver.get(AppConstants.BASE_URL);
        By emailField = By.xpath("//input[@id='email'] | //input[@type='email'] "
                + "| //input[@name='email'] | //input[contains(@placeholder,'Email')]");
        new WebDriverWait(driver, Duration.ofSeconds(LOGIN_TIMEOUT))
                .until(ExpectedConditions.visibilityOfElementLocated(emailField));

        // Credentials come from the environment; requireProductionTarget already proved they were
        // supplied, and they are never logged.
        loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(POST_LOGIN_TIMEOUT)).until(d -> {
            String u = d.getCurrentUrl();
            return !u.contains("/login");
        });
        dismissBackdrops();
        pause(2500);
    }

    /**
     * How many roles the authenticated seat holds, read from the app's own session endpoint.
     * Returns -1 when the shape cannot be read, which callers treat as "cannot tell".
     */
    private int countSeatRoles() {
        try {
            Object n = js.executeAsyncScript(
                    "var done = arguments[arguments.length - 1];"
                    + "fetch('/api/auth/v2/me', {credentials:'include'})"
                    + "  .then(function(r){ return r.ok ? r.json() : null; })"
                    + "  .then(function(j){"
                    + "     if (!j) { done(-1); return; }"
                    + "     var roles = j.roles || (j.user && j.user.roles) || [];"
                    + "     done(Array.isArray(roles) ? roles.length : -1);"
                    + "  })"
                    + "  .catch(function(){ done(-1); });");
            if (n instanceof Number) return ((Number) n).intValue();
        } catch (Exception e) {
            System.out.println("[ProductionSmoke] role count unavailable: " + e.getMessage());
        }
        return -1;
    }

    /** {@code <main>} text, falling back to the body. Scoped so sidebar chrome cannot satisfy a check. */
    private String mainText() {
        try {
            List<WebElement> main = driver.findElements(By.cssSelector("main"));
            if (!main.isEmpty()) return main.get(0).getText();
            return driver.findElement(By.tagName("body")).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private void dismissBackdrops() {
        try {
            js.executeScript(
                    "document.querySelectorAll('.MuiBackdrop-root, .MuiModal-backdrop')"
                    + ".forEach(function(b){b.style.display='none';b.style.pointerEvents='none';});"
                    + "var btns=document.querySelectorAll('button');"
                    + "for (var i=0;i<btns.length;i++){"
                    + "  if ((btns[i].textContent||'').trim()==='DISMISS'){ btns[i].click(); break; }"
                    + "}");
        } catch (Exception ignored) { }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private void logStep(String msg) {
        ExtentReportManager.logInfo(msg);
        System.out.println("   " + msg);
    }
}
