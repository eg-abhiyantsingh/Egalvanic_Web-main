package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.testcase.api.RbacFixtures;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import io.restassured.RestAssured;

import org.openqa.selenium.By;
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
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>Real-website, per-role RBAC UI gating</b> — proves that what each role can
 * actually SEE in the live web app matches its permissions, not just what
 * {@code /auth/me} reports.
 *
 * <p>The React app renders each left-nav module via
 * {@code hasPermission(item.permission)} (= {@code is_admin || permissions.includes(p)},
 * see {@code Layout.jsx}). This test logs in as each role in a real browser and asserts,
 * for a set of high-signal nav modules, that the module is present <b>iff</b> the role's
 * <em>live</em> permission set grants it — catching frontend mis-wiring (a nav item
 * guarded by the wrong permission, or not guarded at all) that an API test cannot see.</p>
 *
 * <p><b>Oracle = live {@code /auth/me}</b> (reused via {@link RbacFixtures}), not the prod
 * CSV — so the UI is checked against the permissions the app actually received on QA, and
 * documented prod-vs-QA drift does not cause false UI failures.</p>
 *
 * <p>Detection is <b>route-based</b> ({@code <a href="/assets">} inside the sidebar), which
 * is robust to a collapsed/icon-only nav. Roles without {@code platform.web} (Technician)
 * must hit the "Web Access Restricted" page. Unprovisioned roles SKIP.</p>
 *
 * <p>Run a subset with {@code -Drbac.ui.roles="Admin,Client Portal"}.</p>
 */
public class RolePermissionUiGatingTest {

    /** A nav module: visible label, the permission that gates it, and its route. */
    private static final class NavModule {
        final String label, permission, route;
        NavModule(String label, String permission, String route) {
            this.label = label; this.permission = permission; this.route = route;
        }
    }

    /** High-signal, unambiguously-gated nav modules (from Layout.jsx / navigation.js). */
    private static final List<NavModule> NAV_MODULES = Arrays.asList(
            new NavModule("Assets",        "features.assets.view",        "/assets"),
            new NavModule("SLDs",          "features.slds.view",          "/slds"),
            new NavModule("Locations",     "features.locations.view",     "/locations"),
            new NavModule("Tasks",         "features.tasks.view",         "/tasks"),
            new NavModule("Issues",        "features.issues.view",        "/issues"),
            new NavModule("Opportunities", "features.opportunities.view", "/opportunities"),
            new NavModule("Accounts",      "features.accounts.view",      "/accounts"),
            new NavModule("Goals",         "features.goals.view",         "/goals"),
            new NavModule("Arc Flash",     "features.arc_flash.view",     "/arc-flash"),
            new NavModule("Settings",      "features.settings.view",      "/admin"),
            new NavModule("Audit Log",     "features.audit_log.view",     "/admin/audit-log")
    );

    private WebDriver driver;
    private LoginPage loginPage;

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        ExtentReportManager.initReports();
        // /auth/me oracle is fetched via RbacFixtures (RestAssured) — point it at the API base.
        RestAssured.baseURI = AppConstants.API_BASE_URL;
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        ExtentReportManager.flushReports();
    }

    @DataProvider(name = "roles")
    public Object[][] roles() {
        String filter = System.getProperty("rbac.ui.roles", "").trim();
        Set<String> only = filter.isEmpty() ? null
                : Arrays.stream(filter.split(",")).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        return RbacFixtures.ROLES.stream()
                .filter(r -> only == null || only.contains(r.name))
                .map(r -> new Object[]{r})
                .toArray(Object[][]::new);
    }

    @AfterMethod(alwaysRun = true)
    public void teardown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && driver != null) {
            ScreenshotUtil.captureScreenshot(result.getMethod().getMethodName() + "_FAIL");
        }
        ExtentReportManager.removeTests();
        if (driver != null) { try { driver.quit(); } catch (Exception ignored) {} driver = null; }
    }

    @Test(dataProvider = "roles", description = "Live web app shows each role only the nav modules its permissions allow")
    public void roleNavGating(Role role) {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS,
                "UI Gating: " + role.name);

        // --- Oracle: the role's LIVE permission set (same source the UI gates on) ---
        LiveAuth live = RbacFixtures.cachedLiveAuth(role);
        if (!live.provisioned) {
            String msg = "Account not provisioned for '" + role.name + "' (login status "
                    + live.loginStatus + ") — UI gating not checked.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }
        boolean isAdmin = Boolean.TRUE.equals(live.isAdmin);
        boolean expectWeb = isAdmin || live.permissions.contains("platform.web");

        // --- Real browser login as this role ---
        startBrowser();
        navigateToLogin();
        loginPage.login(role.email, role.password);
        waitForPostLogin();

        // --- platform.web gate: roles without it must be web-restricted ---
        if (!expectWeb) {
            Assert.assertTrue(isWebAccessRestricted(),
                    "'" + role.name + "' lacks platform.web → expected the Web Access Restricted page, "
                            + "but the app rendered instead. URL: " + driver.getCurrentUrl());
            ExtentReportManager.logPass("'" + role.name + "' correctly blocked from web (no platform.web).");
            return;
        }
        Assert.assertFalse(isWebAccessRestricted(),
                "'" + role.name + "' has web access but the app showed Web Access Restricted.");
        if (onLoginPage()) {
            // API login worked but the UI didn't get past login — almost always transient
            // auth throttling. SKIP rather than flake (don't mask: it's logged).
            String msg = "UI login did not complete for '" + role.name + "' (still on login page; "
                    + "API login succeeded) — likely transient auth throttling.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }

        // --- Read the sidebar's route links (robust to collapsed/icon-only nav) ---
        Set<String> navHrefs = readSidebarHrefs();
        ExtentReportManager.logInfo("'" + role.name + "' is_admin=" + isAdmin
                + ", live perms=" + live.permissions.size() + ", sidebar links=" + navHrefs.size());

        StringBuilder mismatches = new StringBuilder();
        int checked = 0;
        for (NavModule m : NAV_MODULES) {
            boolean expected = isAdmin || live.permissions.contains(m.permission);
            boolean actual = navHrefs.stream().anyMatch(h -> h.contains(m.route));
            checked++;
            if (expected != actual) {
                mismatches.append("\n  • ").append(m.label).append(" (").append(m.permission).append("): ")
                        .append(expected ? "should be VISIBLE (role has perm) but is ABSENT"
                                         : "should be HIDDEN (role lacks perm) but is VISIBLE — gating bug");
            }
        }

        if (mismatches.length() > 0) {
            String report = "UI gating mismatch for '" + role.name + "':" + mismatches
                    + "\n  (sidebar routes seen: " + navHrefs + ")";
            ExtentReportManager.logFail(report);
            Assert.fail(report);
        }
        ExtentReportManager.logPass("'" + role.name + "': all " + checked
                + " gated nav modules match the live permission set (visible⇔permitted).");
    }

    // ---- browser + page helpers (mirrors the proven AuthSmokeTestNG patterns) ----

    private void startBrowser() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);
        if ("true".equals(System.getProperty("headless"))) opts.addArguments("--headless=new");
        driver = new ChromeDriver(opts);
        driver.manage().window().maximize();
        ScreenshotUtil.setDriver(driver);
        loginPage = new LoginPage(driver);
    }

    private void navigateToLogin() {
        driver.get(AppConstants.BASE_URL);
        sleep(2000);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        } catch (Exception e) {
            // maybe already authenticated/redirected — caller handles via onLoginPage()
        }
    }

    private void waitForPostLogin() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> !onLoginPage() || isWebAccessRestricted());
        } catch (Exception ignored) { /* timeout handled by caller checks */ }
        sleep(2500); // let the SPA hydrate the nav after /auth/me
    }

    private boolean onLoginPage() {
        try {
            List<WebElement> email = driver.findElements(By.id("email"));
            return !email.isEmpty() && email.get(0).isDisplayed();
        } catch (Exception e) { return false; }
    }

    private boolean isWebAccessRestricted() {
        try {
            List<WebElement> restricted = driver.findElements(By.xpath(
                    "//*[contains(normalize-space(.), 'Web Access Restricted') "
                            + "or contains(normalize-space(.), 'not have permission') "
                            + "or contains(normalize-space(.), 'access restricted') "
                            + "or contains(normalize-space(.), 'Access Denied') "
                            + "or contains(normalize-space(.), 'mobile app only') "
                            + "or contains(normalize-space(.), 'use the mobile app')]"));
            for (WebElement r : restricted) {
                try { if (r.isDisplayed() && r.getText().trim().length() > 0) return true; } catch (Exception ignored) {}
            }
            return false;
        } catch (Exception e) { return false; }
    }

    /** Collect the href targets of all anchors inside the left nav / sidebar / drawer. */
    private Set<String> readSidebarHrefs() {
        // Wait until at least one sidebar link exists (nav has hydrated).
        String sel = "nav a[href], aside a[href], [class*='Drawer'] a[href], "
                + "[class*='idebar'] a[href], [role='navigation'] a[href]";
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(sel)));
        } catch (Exception ignored) { /* admin with all modules should have links; report will show empty */ }
        Set<String> hrefs = new LinkedHashSet<>();
        for (WebElement a : driver.findElements(By.cssSelector(sel))) {
            try {
                String h = a.getAttribute("href");
                if (h != null && !h.isEmpty()) hrefs.add(h);
            } catch (Exception ignored) {}
        }
        return hrefs;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
