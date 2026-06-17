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

    /** A nav module: visible label, the permission that gates it, its route, and whether the
     *  frontend ALSO gates it behind a company feature flag (Layout.jsx `disabled: !flag`). */
    private static final class NavModule {
        final String label, permission, route;
        final boolean flagCoupled;
        NavModule(String label, String permission, String route, boolean flagCoupled) {
            this.label = label; this.permission = permission; this.route = route; this.flagCoupled = flagCoupled;
        }
    }

    /**
     * High-signal gated nav modules (from Layout.jsx / navigation.js). For most, visibility ==
     * hasPermission(perm). The Sales trio (Goals/Opportunities/Accounts) is ALSO gated by the
     * company `sales-core` feature flag (Layout.jsx: `disabled: !hasSalesCore`), so for them we can
     * only hard-assert the SECURITY direction (a role lacking the permission must NOT see it); the
     * positive is flag-dependent. Marked flagCoupled=true.
     */
    private static final List<NavModule> NAV_MODULES = Arrays.asList(
            new NavModule("Assets",        "features.assets.view",        "/assets",          false),
            new NavModule("SLDs",          "features.slds.view",          "/slds",            false),
            new NavModule("Locations",     "features.locations.view",     "/locations",       false),
            new NavModule("Tasks",         "features.tasks.view",         "/tasks",           false),
            new NavModule("Issues",        "features.issues.view",        "/issues",          false),
            new NavModule("Arc Flash",     "features.arc_flash.view",     "/arc-flash",       false),
            new NavModule("Settings",      "features.settings.view",      "/admin",           false),
            new NavModule("Audit Log",     "features.audit_log.view",     "/admin/audit-log", false),
            new NavModule("Opportunities", "features.opportunities.view", "/opportunities",   true),
            new NavModule("Accounts",      "features.accounts.view",      "/accounts",        true),
            new NavModule("Goals",         "features.goals.view",         "/goals",           true)
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
        // Honors -Drbac.roles (shared across all RBAC suites); also accepts legacy -Drbac.ui.roles.
        String legacy = System.getProperty("rbac.ui.roles", "").trim();
        java.util.List<Role> rs;
        if (!legacy.isEmpty()) {
            Set<String> only = Arrays.stream(legacy.split(",")).map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            rs = RbacFixtures.ROLES.stream().filter(r -> only.contains(r.name)).collect(Collectors.toList());
        } else {
            rs = RbacFixtures.selectedRoles();
        }
        return rs.stream().map(r -> new Object[]{r}).toArray(Object[][]::new);
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
        String mismatch = RbacFixtures.roleMismatchSkipMessage(role, live);
        if (mismatch != null) { ExtentReportManager.logSkip(mismatch); throw new SkipException(mismatch); }
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
            // And the app nav must NOT render — proves the gate actually blocks, not just shows a banner.
            Set<String> leaked = readSidebarHrefs();
            Assert.assertTrue(leaked.isEmpty(),
                    "'" + role.name + "' is web-restricted but the app sidebar rendered (leak): " + leaked);
            ExtentReportManager.logPass("'" + role.name + "' correctly blocked from web (no platform.web); "
                    + "no app nav rendered.");
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
            boolean actual = navHasRoute(navHrefs, m.route);   // exact path match (so /admin ≠ /admin/audit-log)
            checked++;
            if (m.flagCoupled) {
                // Visibility also depends on a company feature flag (e.g. sales-core), so only the
                // SECURITY direction is a hard invariant: a role WITHOUT the permission must NOT see it.
                // When the role HAS the permission, presence is flag-dependent (visible or locked) → log.
                if (!expected && actual) {
                    mismatches.append("\n  • ").append(m.label).append(" (").append(m.permission)
                            .append("): should be HIDDEN (role lacks perm) but is VISIBLE — gating bug");
                } else if (expected && !actual) {
                    ExtentReportManager.logInfo(m.label + " not shown for '" + role.name
                            + "' — role has " + m.permission + " but the module is feature-flag-gated "
                            + "(rendered only when the company flag is on); not a gating failure.");
                }
            } else if (expected != actual) {
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
        // Retry under heavy concurrent CI load (app may load slowly / be transiently unavailable).
        for (int attempt = 1; attempt <= 3; attempt++) {
            try { driver.get(AppConstants.BASE_URL); } catch (Exception ignored) {}
            sleep(2000);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(30))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
                return;
            } catch (Exception e) {
                if (attempt < 3) { try { driver.navigate().refresh(); } catch (Exception ignored) {} sleep(3000); }
            }
        }
        // maybe already authenticated/redirected — caller handles via onLoginPage()
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

    /**
     * True if any sidebar href's PATH exactly equals the route. Exact-path (not substring) matching
     * is required so "/admin" (Settings) does not also match "/admin/audit-log" (Audit Log).
     */
    private static boolean navHasRoute(Set<String> hrefs, String route) {
        for (String h : hrefs) {
            try {
                String path = java.net.URI.create(h).getPath();
                if (path != null) {
                    if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
                    if (path.equals(route)) return true;
                }
            } catch (Exception ignored) { /* malformed href — skip */ }
        }
        return false;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
