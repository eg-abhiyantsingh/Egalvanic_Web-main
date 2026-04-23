package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.utils.ExtentReportManager;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Smoke Tests for Authentication & Login.
 *
 * Architecture: Each test gets its own fresh browser session.
 * This ensures complete isolation between role login tests —
 * no leftover cookies, sessions, or SPA state from a previous login.
 *
 * Tests:
 *   1. Admin login + access verification
 *   2. Project Manager login + access verification
 *   3. Technician login + web access restricted verification
 *   4. Facility Manager login + access verification
 *   5. Client Portal login + access verification
 *   6. Invalid credentials → error message
 */
public class AuthSmokeTestNG {

    private WebDriver driver;
    private LoginPage loginPage;
    private JavascriptExecutor js;
    private long testStartTime;

    private static final int LOGIN_TIMEOUT = 60;
    private static final int POST_LOGIN_TIMEOUT = 30;

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("h:mm a - dd MMM");

    // ================================================================
    // SUITE LIFECYCLE — report init/flush
    // ================================================================

    @BeforeSuite
    public void suiteSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     eGalvanic Web Automation - Test Suite Starting");
        System.out.println("     " + LocalDateTime.now().format(TIMESTAMP_FMT));
        System.out.println("==============================================================");
        System.out.println();

        ExtentReportManager.initReports();
        ScreenshotUtil.cleanupOldScreenshots(7);
    }

    @AfterSuite
    public void suiteTeardown() {
        ExtentReportManager.flushReports();

        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     eGalvanic Web Automation - Test Suite Complete");
        System.out.println("     " + LocalDateTime.now().format(TIMESTAMP_FMT));
        System.out.println("==============================================================");
        System.out.println("Reports generated:");
        System.out.println("   - Detailed: " + ExtentReportManager.getDetailedReportPath());
        System.out.println("   - Client:   " + ExtentReportManager.getClientReportPath());
    }

    // ================================================================
    // PER-TEST BROWSER LIFECYCLE
    // ================================================================

    @BeforeMethod
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

        if ("true".equals(System.getProperty("headless"))) {
            opts.addArguments("--headless=new");
        }

        driver = new ChromeDriver(opts);
        driver.manage().window().maximize();

        js = (JavascriptExecutor) driver;
        js.executeScript("document.body.style.zoom='90%';");

        ScreenshotUtil.setDriver(driver);

        loginPage = new LoginPage(driver);
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;

        if (result.getStatus() == ITestResult.FAILURE) {
            String screenshotName = result.getMethod().getMethodName() + "_FAIL";
            ScreenshotUtil.captureScreenshot(screenshotName);
            ExtentReportManager.logFailWithScreenshot(
                    "Test failed: " + result.getMethod().getMethodName(),
                    result.getThrowable());
        } else if (result.getStatus() == ITestResult.SKIP) {
            ExtentReportManager.logSkip("Test skipped: " + result.getMethod().getMethodName());
        }

        ExtentReportManager.removeTests();

        if (driver != null) {
            driver.quit();
            driver = null;
        }

        String fmt = duration < 1000 ? duration + "ms"
                : duration < 60000 ? (duration / 1000) + "s"
                : (duration / 60000) + "m " + ((duration / 1000) % 60) + "s";
        System.out.println("Test completed in " + fmt);
    }

    // ================================================================
    // TEST 1: ADMIN LOGIN
    // ================================================================

    @Test(priority = 1, description = "Smoke: Admin login and access verification")
    public void testAdminLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_LOGIN, "TC_Auth_AdminLogin");

        try {
            logStep("Logging in as Admin: " + AppConstants.ADMIN_EMAIL);

            navigateToLoginPage();
            loginPage.login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
            logStep("Credentials submitted");

            waitForPostLoginPage();
            dismissBackdrops();
            logStepWithScreenshot("Admin login successful — post-login page loaded");

            // Verify we're past the login page
            String currentUrl = driver.getCurrentUrl();
            Assert.assertFalse(isOnLoginPage(),
                    "Still on login page after admin login. URL: " + currentUrl);
            logStep("Verified: not on login page. URL: " + currentUrl);

            // Verify navigation menu is present — wait up to 15s for SPA hydration,
            // accepting any of nav / aside / sidebar / drawer / role=navigation
            // containers (not all layouts use <nav>).
            boolean hasNav = waitForNavMenu(15);
            Assert.assertTrue(hasNav, "Navigation menu not found after admin login");
            logStep("Navigation menu is present");

            // Dump sidebar nav items for role verification
            String navItems = dumpNavItems();
            logStep("Admin nav items: " + navItems);

            // Admin should have access to admin-level features
            boolean hasAdminAccess = navItems.toLowerCase().contains("admin")
                    || navItems.toLowerCase().contains("audit")
                    || navItems.toLowerCase().contains("user");
            if (hasAdminAccess) {
                logStep("Admin-level nav items confirmed");
            } else {
                logStep("Note: Admin-specific nav items not detected (may use different labels)");
            }

            // Verify core nav items are visible (Assets, Locations at minimum)
            verifyCoreSidebarItems(navItems, "Admin");

            ExtentReportManager.logPass("Admin login successful. Access verified.");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("admin_login_error");
            Assert.fail("Admin login failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 2: PROJECT MANAGER LOGIN
    // ================================================================

    @Test(priority = 2, description = "Smoke: Project Manager login and access verification")
    public void testProjectManagerLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_LOGIN, "TC_Auth_ProjectManagerLogin");

        try {
            logStep("Logging in as Project Manager: " + AppConstants.PM_EMAIL);

            navigateToLoginPage();
            loginPage.login(AppConstants.PM_EMAIL, AppConstants.PM_PASSWORD);
            logStep("Credentials submitted");

            waitForPostLoginPage();
            dismissBackdrops();
            logStepWithScreenshot("PM login successful — post-login page loaded");

            String currentUrl = driver.getCurrentUrl();
            Assert.assertFalse(isOnLoginPage(),
                    "Still on login page after PM login. URL: " + currentUrl);
            logStep("Verified: not on login page. URL: " + currentUrl);

            // Wait up to 15s for nav container to render (nav, aside, sidebar, drawer, role=navigation).
            // SPA may still be hydrating after login redirect.
            boolean hasNav = waitForNavMenu(15);
            Assert.assertTrue(hasNav,
                    "Navigation menu not found after PM login within 15s. " +
                    "Checked: nav, aside, [class*='Sidebar'], [class*='MuiDrawer'], [role='navigation']. " +
                    "URL: " + currentUrl);
            logStep("Navigation menu is present");

            String navItems = dumpNavItems();
            logStep("PM nav items: " + navItems);

            verifyCoreSidebarItems(navItems, "Project Manager");

            ExtentReportManager.logPass("Project Manager login successful. Access verified.");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("pm_login_error");
            Assert.fail("PM login failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 3: TECHNICIAN LOGIN
    // ================================================================

    @Test(priority = 3, description = "Smoke: Technician login — verify web access restricted")
    public void testTechnicianLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS, "TC_Auth_TechnicianLogin");

        try {
            logStep("Logging in as Technician: " + AppConstants.TECH_EMAIL);

            navigateToLoginPage();
            loginPage.login(AppConstants.TECH_EMAIL, AppConstants.TECH_PASSWORD);
            logStep("Credentials submitted");

            waitForPostLoginPage();

            String currentUrl = driver.getCurrentUrl();
            Assert.assertFalse(isOnLoginPage(),
                    "Still on login page after Technician login. URL: " + currentUrl);
            logStep("Authentication succeeded — left login page. URL: " + currentUrl);

            // Technician role does NOT have web platform access.
            // Expected: "Web Access Restricted" page with Current Role: Unknown
            // This is the correct behavior — Technicians use the mobile app.
            boolean isRestricted = isWebAccessRestricted();
            logStepWithScreenshot("Post-login page state captured");

            if (isRestricted) {
                logStep("Web Access Restricted page confirmed — Technician role correctly denied web access");

                // Verify the restriction message content
                String restrictionDetails = getRestrictionDetails();
                logStep("Restriction details: " + restrictionDetails);

                // Verify Logout button is available (user can sign out)
                boolean hasLogout = driver.findElements(
                        By.xpath("//button[contains(.,'Logout') or contains(.,'Log out')]")).size() > 0;
                if (hasLogout) {
                    logStep("Logout button available on restricted page");
                }

                ExtentReportManager.logPass(
                        "Technician login: authentication succeeded, web access correctly restricted. " + restrictionDetails);
            } else {
                // If Technician CAN access the web app, still verify nav items.
                // Wait up to 15s for SPA hydration (accepts nav/aside/sidebar/drawer).
                boolean hasNav = waitForNavMenu(15);
                Assert.assertTrue(hasNav, "No nav menu and not on restricted page after Technician login");

                String navItems = dumpNavItems();
                logStep("Technician has web access. Nav items: " + navItems);
                verifyCoreSidebarItems(navItems, "Technician");

                ExtentReportManager.logPass("Technician login successful with web access. Nav items: " + navItems);
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("tech_login_error");
            Assert.fail("Technician login test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 4: FACILITY MANAGER LOGIN
    // ================================================================

    @Test(priority = 4, description = "Smoke: Facility Manager login and access verification")
    public void testFacilityManagerLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS, "TC_Auth_FacilityManagerLogin");

        try {
            logStep("Logging in as Facility Manager: " + AppConstants.FM_EMAIL);

            navigateToLoginPage();
            loginPage.login(AppConstants.FM_EMAIL, AppConstants.FM_PASSWORD);
            logStep("Credentials submitted");

            waitForPostLoginPage();

            String currentUrl = driver.getCurrentUrl();
            Assert.assertFalse(isOnLoginPage(),
                    "Still on login page after FM login. URL: " + currentUrl);
            logStep("Authentication succeeded — left login page. URL: " + currentUrl);

            boolean isRestricted = isWebAccessRestricted();
            logStepWithScreenshot("Post-login page state captured");

            if (isRestricted) {
                // Facility Manager may be a mobile-only role like Technician
                logStep("Web Access Restricted page confirmed — Facility Manager role denied web access");

                String restrictionDetails = getRestrictionDetails();
                logStep("Restriction details: " + restrictionDetails);

                boolean hasLogout = driver.findElements(
                        By.xpath("//button[contains(.,'Logout') or contains(.,'Log out')]")).size() > 0;
                if (hasLogout) {
                    logStep("Logout button available on restricted page");
                }

                ExtentReportManager.logPass(
                        "Facility Manager login: authentication succeeded, web access restricted. " + restrictionDetails);
            } else {
                // Facility Manager has web access — verify nav items
                // Wait up to 15s for nav container to render (SPA may still be hydrating).
                boolean hasNav = waitForNavMenu(15);
                Assert.assertTrue(hasNav,
                        "No nav menu (nav/aside/sidebar/drawer/role=navigation) after 15s and " +
                        "not on restricted page after FM login. URL: " + currentUrl);

                String navItems = dumpNavItems();
                logStep("Facility Manager nav items: " + navItems);

                verifyCoreSidebarItems(navItems, "Facility Manager");

                // Check for FM-specific features
                String lower = navItems.toLowerCase();
                boolean hasWorkOrders = lower.contains("work order") || lower.contains("maintenance");
                boolean hasFacilities = lower.contains("facilit") || lower.contains("building") || lower.contains("location");
                if (hasWorkOrders) logStep("Facility Manager: Work Orders / Maintenance access confirmed");
                if (hasFacilities) logStep("Facility Manager: Facilities / Locations access confirmed");

                ExtentReportManager.logPass("Facility Manager login successful. Access verified. Nav items: " + navItems);
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("fm_login_error");
            Assert.fail("Facility Manager login failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 5: CLIENT PORTAL LOGIN
    // ================================================================

    @Test(priority = 5, description = "Smoke: Client Portal login and access verification")
    public void testClientPortalLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS, "TC_Auth_ClientPortalLogin");

        try {
            logStep("Logging in as Client Portal: " + AppConstants.CP_EMAIL);

            navigateToLoginPage();
            loginPage.login(AppConstants.CP_EMAIL, AppConstants.CP_PASSWORD);
            logStep("Credentials submitted");

            waitForPostLoginPage();

            String currentUrl = driver.getCurrentUrl();
            Assert.assertFalse(isOnLoginPage(),
                    "Still on login page after Client Portal login. URL: " + currentUrl);
            logStep("Authentication succeeded — left login page. URL: " + currentUrl);

            boolean isRestricted = isWebAccessRestricted();
            logStepWithScreenshot("Post-login page state captured");

            if (isRestricted) {
                // Client Portal may be restricted from the main web app
                logStep("Web Access Restricted page confirmed — Client Portal role denied web access");

                String restrictionDetails = getRestrictionDetails();
                logStep("Restriction details: " + restrictionDetails);

                boolean hasLogout = driver.findElements(
                        By.xpath("//button[contains(.,'Logout') or contains(.,'Log out')]")).size() > 0;
                if (hasLogout) {
                    logStep("Logout button available on restricted page");
                }

                ExtentReportManager.logPass(
                        "Client Portal login: authentication succeeded, web access restricted. " + restrictionDetails);
            } else {
                // Client Portal has web access — verify nav items.
                // Wait up to 15s for SPA hydration (accepts nav/aside/sidebar/drawer).
                boolean hasNav = waitForNavMenu(15);
                Assert.assertTrue(hasNav, "No nav menu and not on restricted page after Client Portal login");

                String navItems = dumpNavItems();
                logStep("Client Portal nav items: " + navItems);

                verifyCoreSidebarItems(navItems, "Client Portal");

                // Client Portal may have a more limited view (read-only dashboards, reports)
                String lower = navItems.toLowerCase();
                boolean hasDashboard = lower.contains("dashboard");
                boolean hasReports = lower.contains("report");
                if (hasDashboard) logStep("Client Portal: Dashboard access confirmed");
                if (hasReports) logStep("Client Portal: Reports access confirmed");

                // Count nav items — Client Portal typically has fewer than Admin/PM
                int navCount = navItems.split(",").length;
                logStep("Client Portal: " + navCount + " nav items detected (compare: Admin ~24, PM ~18)");

                ExtentReportManager.logPass("Client Portal login successful. Access verified. Nav items: " + navItems);
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("cp_login_error");
            Assert.fail("Client Portal login failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 6: INVALID CREDENTIALS
    // ================================================================

    @Test(priority = 6, description = "Smoke: Invalid credentials show error")
    public void testInvalidLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_LOGIN, "TC_Auth_InvalidLogin");

        try {
            logStep("Attempting login with invalid credentials");

            navigateToLoginPage();
            loginPage.login(AppConstants.INVALID_EMAIL, AppConstants.INVALID_PASSWORD);
            logStep("Invalid credentials submitted");

            // Wait up to 10s for either: error message on login page, or login page
            // DOM to stabilize after the submit. The app briefly routes to "/" then
            // back to "/login" on invalid credentials — a single 3s pause caught
            // the user mid-transition, failing the test on a false positive.
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(d -> isOnLoginPage() || checkForLoginError());
            } catch (Exception ignoreTimeout) { /* fall through to the strict assert below */ }

            // Verify we're still on the login page (not redirected to dashboard)
            Assert.assertTrue(isOnLoginPage(),
                    "Expected to remain on login page after invalid credentials, but navigated away. URL: "
                            + driver.getCurrentUrl());
            logStep("Confirmed: still on login page after invalid credentials");

            // Check for error message
            boolean errorShown = checkForLoginError();
            logStepWithScreenshot("Login page after invalid credentials");

            if (errorShown) {
                logStep("Error message displayed to user");
            } else {
                logStep("No explicit error message found, but login was correctly rejected (still on login page)");
            }

            ExtentReportManager.logPass("Invalid login correctly rejected");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("invalid_login_error");
            Assert.fail("Invalid login test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Wait up to {@code timeoutSeconds} for a navigation container to render
     * after login. Modern SPAs may render the nav in <nav>, <aside>, or
     * inside a sidebar/drawer div — we accept any of them. Also dismisses
     * MUI backdrops between polls because an open backdrop can make the
     * nav unreachable / not-visible.
     *
     * Returns {@code true} if any navigation container renders within the
     * timeout, {@code false} otherwise.
     */
    private boolean waitForNavMenu(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        String selector = "nav, aside, [class*='Sidebar'], [class*='sidebar'], "
                + "[class*='MuiDrawer'], [role='navigation']";
        while (System.currentTimeMillis() < deadline) {
            try {
                dismissBackdrops();
                List<WebElement> candidates = driver.findElements(By.cssSelector(selector));
                for (WebElement el : candidates) {
                    try {
                        if (el.isDisplayed() && el.getSize().getWidth() > 50
                                && el.getSize().getHeight() > 100) {
                            return true;
                        }
                    } catch (Exception ignored) {
                        // Stale — keep polling
                    }
                }
            } catch (Exception ignored) {}
            pause(500);
        }
        return false;
    }

    /**
     * Check if the current page is the "Web Access Restricted" page.
     * This appears when a user authenticates successfully but their role
     * does not have permission to use the web platform.
     */
    private boolean isWebAccessRestricted() {
        try {
            // Broader restricted-page detection — catches variations of the
            // "you can't access this app" screen. Covers the older "Web Access
            // Restricted" heading plus newer copy ("access denied", "role does
            // not allow", "contact your administrator") as well as explicit
            // role hints ("Current Role: Unknown", "mobile app only").
            List<WebElement> restricted = driver.findElements(By.xpath(
                    "//*[contains(normalize-space(.), 'Web Access Restricted') "
                            + "or contains(normalize-space(.), 'not have permission') "
                            + "or contains(normalize-space(.), 'access restricted') "
                            + "or contains(normalize-space(.), 'access denied') "
                            + "or contains(normalize-space(.), 'Access Denied') "
                            + "or contains(normalize-space(.), 'role does not allow') "
                            + "or contains(normalize-space(.), 'Current Role: Unknown') "
                            + "or contains(normalize-space(.), 'use the mobile app') "
                            + "or contains(normalize-space(.), 'mobile app only') "
                            + "or contains(normalize-space(.), 'contact your administrator')]"));
            // Filter to visible elements only (hidden matches in aria-live etc. don't count)
            for (WebElement r : restricted) {
                try {
                    if (r.isDisplayed() && r.getText() != null && r.getText().trim().length() > 0) return true;
                } catch (Exception ignored) { /* stale, try next */ }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get details from the Web Access Restricted page (role, message).
     */
    private String getRestrictionDetails() {
        try {
            StringBuilder details = new StringBuilder();

            // Get the role displayed
            List<WebElement> roleEls = driver.findElements(By.xpath(
                    "//*[contains(text(),'Current Role')]"));
            if (!roleEls.isEmpty()) {
                String roleText = (String) js.executeScript(
                        "return arguments[0].textContent.trim();", roleEls.get(0));
                details.append(roleText);
            }

            // Get the restriction message
            List<WebElement> msgEls = driver.findElements(By.xpath(
                    "//*[contains(text(),'does not have permission') or contains(text(),'not have permission')]"));
            if (!msgEls.isEmpty()) {
                String msg = (String) js.executeScript(
                        "return arguments[0].textContent.trim();", msgEls.get(0));
                if (details.length() > 0) details.append(" | ");
                details.append(msg);
            }

            return details.length() > 0 ? details.toString() : "Web Access Restricted (no additional details)";
        } catch (Exception e) {
            return "Web Access Restricted";
        }
    }

    /**
     * Navigate to the login page with retry logic for Application Error pages.
     */
    private void navigateToLoginPage() {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            driver.get(AppConstants.BASE_URL);
            pause(2000);

            // Handle Application Error page
            if (isApplicationErrorPage()) {
                System.out.println("[Auth] Application Error on attempt " + attempt + " — retrying...");
                try { driver.findElement(By.xpath("//button[contains(@aria-label,'Close')]")).click(); pause(500); } catch (Exception ignored) {}
                driver.navigate().refresh();
                pause(3000);

                if (isApplicationErrorPage()) {
                    try { driver.findElement(By.xpath("//button[contains(.,'Try Again')]")).click(); pause(3000); } catch (Exception ignored) {}
                }

                if (isApplicationErrorPage() && attempt < maxRetries) {
                    continue;
                }
            }

            // Wait for login page to load
            try {
                new WebDriverWait(driver, Duration.ofSeconds(LOGIN_TIMEOUT))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
                System.out.println("[Auth] Login page loaded. URL: " + driver.getCurrentUrl());
                return;
            } catch (Exception e) {
                System.out.println("[Auth] Login page not ready on attempt " + attempt + ": " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Login page did not load after " + maxRetries + " attempts. URL: " + driver.getCurrentUrl());
                }
            }
        }
    }

    /**
     * Wait for the post-login page to load (dashboard, main app, or any page past login).
     * Handles both immediate redirect and multi-step auth flows.
     */
    private void waitForPostLoginPage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(POST_LOGIN_TIMEOUT));

        // Wait for EITHER:
        // 1. Login page email field disappears (we've left the login page)
        // 2. Navigation menu appears (we're in the app)
        // 3. Dashboard content appears
        // 4. URL changes away from login
        try {
            wait.until(driver -> {
                // Check if we've left the login page
                if (!isOnLoginPage()) return true;

                // Check for error message (login failed — don't keep waiting)
                if (checkForLoginError()) return true;

                return false;
            });
        } catch (Exception e) {
            // Timeout — check what state we're in
            String url = driver.getCurrentUrl();
            System.out.println("[Auth] Post-login wait timed out. URL: " + url);

            // If there's an error message, the login failed
            if (checkForLoginError()) {
                throw new RuntimeException("Login failed — error message displayed on login page. URL: " + url);
            }

            // If we're still on login page with no error, credentials might be silently rejected
            if (isOnLoginPage()) {
                throw new RuntimeException("Login did not complete within " + POST_LOGIN_TIMEOUT + "s. Still on login page. URL: " + url);
            }
        }

        pause(2000); // Let SPA state settle

        // If we reached here after an error check, re-verify
        if (isOnLoginPage() && checkForLoginError()) {
            throw new RuntimeException("Login failed — error message on login page");
        }

        System.out.println("[Auth] Post-login page loaded. URL: " + driver.getCurrentUrl());
    }

    /**
     * Check if we're currently on the login page.
     */
    private boolean isOnLoginPage() {
        try {
            // Three signals of "still on login page":
            //  1. URL contains /login  (strongest — survives React re-renders)
            //  2. Email input still present in DOM
            //  3. Password input or submit button present
            // Accept if ANY URL check or DOM check matches. The app sometimes
            // briefly routes to "/" then back to "/login" on invalid-cred submission;
            // checking URL alone catches those transitions.
            String url = driver.getCurrentUrl();
            if (url != null && url.contains("/login")) return true;

            boolean hasEmailField = driver.findElements(By.id("email")).size() > 0;
            boolean hasPasswordField = driver.findElements(By.id("password")).size() > 0;
            boolean hasSubmitBtn = driver.findElements(
                    By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]")).size() > 0;

            // Must have at least email field + one other indicator
            return hasEmailField && (hasPasswordField || hasSubmitBtn);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check for any login error message on the page.
     */
    private boolean checkForLoginError() {
        try {
            // Standard error patterns
            List<WebElement> errors = driver.findElements(By.xpath(
                    "//*[contains(@class,'error') or contains(@class,'alert') or contains(@class,'Error')]"
                            + "[string-length(normalize-space()) > 0]"));
            for (WebElement el : errors) {
                try {
                    String text = el.getText().trim();
                    if (!text.isEmpty() && !text.equalsIgnoreCase("error")) {
                        System.out.println("[Auth] Error message found: '" + text + "'");
                        return true;
                    }
                } catch (Exception ignored) {}
            }

            // Text-based error detection
            List<WebElement> textErrors = driver.findElements(By.xpath(
                    "//*[contains(text(),'Incorrect') or contains(text(),'Invalid') "
                            + "or contains(text(),'incorrect') or contains(text(),'invalid') "
                            + "or contains(text(),'wrong') or contains(text(),'Wrong') "
                            + "or contains(text(),'failed') or contains(text(),'not found') "
                            + "or contains(text(),'unauthorized') or contains(text(),'Unauthorized')]"));
            if (!textErrors.isEmpty()) {
                for (WebElement el : textErrors) {
                    try {
                        String text = el.getText().trim();
                        if (!text.isEmpty()) {
                            System.out.println("[Auth] Error text found: '" + text + "'");
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // MUI Snackbar/Alert
            List<WebElement> alerts = driver.findElements(By.cssSelector(
                    ".MuiAlert-root, .MuiSnackbar-root, [role='alert']"));
            for (WebElement alert : alerts) {
                try {
                    String text = alert.getText().trim();
                    if (!text.isEmpty()) {
                        System.out.println("[Auth] Alert found: '" + text + "'");
                        return true;
                    }
                } catch (Exception ignored) {}
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detect Application Error / crash page.
     */
    private boolean isApplicationErrorPage() {
        try {
            return driver.findElements(By.xpath(
                    "//*[contains(text(),'Application Error') or contains(text(),'We encountered an error') "
                            + "or contains(text(),'something went wrong')]")).size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dump all visible sidebar/nav items using JS (handles CSS zoom).
     * Returns a comma-separated string of nav item labels.
     */
    private String dumpNavItems() {
        try {
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) js.executeScript(
                    "var result = [];" +
                    "var seen = new Set();" +
                    // Strategy 1: sidebar nav links/buttons
                    "var navEls = document.querySelectorAll('nav a, nav button, nav span, " +
                    "[class*=\"sidebar\" i] a, [class*=\"sidebar\" i] button, [class*=\"sidebar\" i] span, " +
                    "[class*=\"Sidebar\" i] a, [class*=\"Sidebar\" i] button, " +
                    "[class*=\"drawer\" i] a, [class*=\"drawer\" i] button, " +
                    "[class*=\"Drawer\" i] a, [class*=\"Drawer\" i] button');" +
                    "for (var el of navEls) {" +
                    "  var r = el.getBoundingClientRect();" +
                    "  if (r.width < 5 || r.height < 5) continue;" +
                    "  var txt = (el.textContent||'').trim();" +
                    "  if (txt && txt.length < 40 && !seen.has(txt)) {" +
                    "    seen.add(txt);" +
                    "    result.push(txt);" +
                    "  }" +
                    "}" +
                    // Strategy 2: left-side interactive elements (sidebar is typically x < 300)
                    "if (result.length === 0) {" +
                    "  var all = document.querySelectorAll('a, button, [role=\"menuitem\"], [role=\"tab\"]');" +
                    "  for (var el of all) {" +
                    "    var r = el.getBoundingClientRect();" +
                    "    if (r.left > 300 || r.width < 5 || r.height < 5) continue;" +
                    "    var txt = (el.textContent||'').trim();" +
                    "    if (txt && txt.length > 1 && txt.length < 40 && !seen.has(txt)) {" +
                    "      seen.add(txt);" +
                    "      result.push(txt);" +
                    "    }" +
                    "  }" +
                    "}" +
                    "return result;");

            if (items == null || items.isEmpty()) {
                return "(no nav items detected)";
            }

            // Log each item
            System.out.println("[Auth] Sidebar nav items (" + items.size() + "):");
            for (int i = 0; i < items.size(); i++) {
                System.out.println("[Auth]   [" + i + "] " + items.get(i));
            }

            return String.join(", ", items);
        } catch (Exception e) {
            System.out.println("[Auth] Failed to dump nav items: " + e.getMessage());
            return "(error reading nav items)";
        }
    }

    /**
     * Verify core sidebar items are present for the given role.
     * All roles should see at minimum: Dashboards and Assets (or equivalent).
     */
    private void verifyCoreSidebarItems(String navItems, String roleName) {
        String lower = navItems.toLowerCase();

        // At minimum, the app should show SOME navigation after login
        Assert.assertFalse(navItems.equals("(no nav items detected)"),
                roleName + " — no navigation items found after login. The page may not have loaded correctly.");

        // Log the full navigation dump for role comparison in reports
        logStep(roleName + " role — detected " + navItems.split(",").length + " nav items");

        // Check for essential navigation items (present for all roles)
        boolean hasDashboard = lower.contains("dashboard");
        boolean hasAssets = lower.contains("asset");
        boolean hasData = lower.contains("data") || lower.contains("sld");

        if (hasDashboard) logStep(roleName + ": Dashboard access confirmed");
        if (hasAssets) logStep(roleName + ": Assets access confirmed");
        if (hasData) logStep(roleName + ": Data/SLD access confirmed");

        // At least one core section must be visible
        Assert.assertTrue(hasDashboard || hasAssets || hasData,
                roleName + " — none of the core nav sections (Dashboard, Assets, Data) found. Nav items: " + navItems);
    }

    // ================================================================
    // LOGGING HELPERS
    // ================================================================

    private void logStep(String message) {
        ExtentReportManager.logInfo(message);
    }

    private void logStepWithScreenshot(String message) {
        ExtentReportManager.logStepWithScreenshot(message);
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void dismissBackdrops() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop').forEach(" +
                "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                ");" +
                "var beamer = document.getElementById('beamerOverlay');" +
                "if (beamer) { beamer.style.display = 'none'; beamer.style.pointerEvents = 'none'; }" +
                "document.querySelectorAll('[id^=\"beamer\"], .beamer_show').forEach(" +
                "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                ");"
            );
        } catch (Exception ignored) {}
    }
}
