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
 * Cross-tenant smoke for <a href="https://acme.bces-iq.com">acme.bces-iq.com</a>.
 *
 * The same eGalvanic application is deployed to multiple tenants. This suite
 * validates that the bces-iq tenant is reachable, login works, and the core
 * application shell renders post-login &mdash; without running the full smoke
 * battery (which is tuned for the acme.qa.egalvanic.ai tenant's data).
 *
 * Configuration (set via workflow env vars, NOT hardcoded):
 *   BASE_URL       &mdash; https://acme.bces-iq.com
 *   USER_EMAIL     &mdash; shubham.goswami+acme@egalvanic.com
 *   USER_PASSWORD  &mdash; from BCES_IQ_PASSWORD secret
 *
 * The tests read these from AppConstants (which reads env via getEnv()),
 * so the same class can run against any tenant if the env is set.
 *
 * Coverage:
 *   TC_BcesIq_01  Site loads (HTTP 200, login form renders)
 *   TC_BcesIq_02  Login succeeds with configured credentials
 *   TC_BcesIq_03  Post-login shell renders (nav / sidebar / drawer)
 *
 * Safety: read-only &mdash; no create / edit / delete operations are performed.
 * The suite only verifies login + landing, so it cannot pollute tenant data.
 */
public class BcesIqSmokeTestNG {

    private WebDriver driver;
    private LoginPage loginPage;
    private JavascriptExecutor js;
    private long testStartTime;

    private static final int LOGIN_TIMEOUT = 60;
    private static final int POST_LOGIN_TIMEOUT = 30;

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("h:mm a - dd MMM");

    // ================================================================
    // SUITE LIFECYCLE
    // ================================================================

    @BeforeSuite
    public void suiteSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     BCES-IQ Tenant Smoke &mdash; " + AppConstants.BASE_URL);
        System.out.println("     " + LocalDateTime.now().format(TIMESTAMP_FMT));
        System.out.println("==============================================================");

        ExtentReportManager.initReports();
        ScreenshotUtil.cleanupOldScreenshots(7);
    }

    @AfterSuite
    public void suiteTeardown() {
        ExtentReportManager.flushReports();
        System.out.println();
        System.out.println("     BCES-IQ smoke complete &mdash; " + LocalDateTime.now().format(TIMESTAMP_FMT));
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
        }
        ExtentReportManager.removeTests();
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        System.out.println("Test completed in " +
                (duration < 1000 ? duration + "ms" : (duration / 1000) + "s"));
    }

    // ================================================================
    // TC_BcesIq_01 — Site loads and login form is reachable
    // ================================================================
    @Test(priority = 1, description = "BCES-IQ: Site loads and login form renders")
    public void testTC_BcesIq_01_SiteLoads() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_LOGIN,
                "TC_BcesIq_01_SiteLoads");
        try {
            logStep("Navigating to tenant: " + AppConstants.BASE_URL);
            driver.get(AppConstants.BASE_URL);

            // Wait for email input to appear (signals login form rendered)
            new WebDriverWait(driver, Duration.ofSeconds(LOGIN_TIMEOUT))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
            logStep("Login form rendered");
            ScreenshotUtil.captureScreenshot("bcesiq_site_loaded");

            // Sanity: email + password fields present, submit button clickable
            List<WebElement> emailFields = driver.findElements(By.id("email"));
            List<WebElement> pwFields = driver.findElements(By.cssSelector("input[type='password'], input[name='password']"));
            List<WebElement> submitBtns = driver.findElements(By.cssSelector("button[type='submit']"));

            Assert.assertFalse(emailFields.isEmpty(), "Email input not found on " + AppConstants.BASE_URL);
            Assert.assertFalse(pwFields.isEmpty(), "Password input not found on " + AppConstants.BASE_URL);
            Assert.assertFalse(submitBtns.isEmpty(), "Submit button not found on " + AppConstants.BASE_URL);

            logStep("Login form structure OK: email + password + submit present");
            ExtentReportManager.logPass("BCES-IQ site reachable, login form renders correctly");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("bcesiq_site_load_error");
            Assert.fail("BCES-IQ site did not load or login form missing: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BcesIq_02 — Login succeeds with configured credentials
    // ================================================================
    @Test(priority = 2, description = "BCES-IQ: Login succeeds with configured credentials")
    public void testTC_BcesIq_02_LoginSucceeds() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_LOGIN,
                "TC_BcesIq_02_LoginSucceeds");
        try {
            logStep("Navigating to " + AppConstants.BASE_URL);
            driver.get(AppConstants.BASE_URL);
            new WebDriverWait(driver, Duration.ofSeconds(LOGIN_TIMEOUT))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));

            logStep("Submitting credentials for user: " + AppConstants.VALID_EMAIL);
            loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);

            // Wait for URL change OR for an error message
            new WebDriverWait(driver, Duration.ofSeconds(POST_LOGIN_TIMEOUT))
                    .until(d -> {
                        String url = d.getCurrentUrl();
                        if (!url.contains("/login") && !url.endsWith(AppConstants.BASE_URL)
                                && !url.endsWith(AppConstants.BASE_URL + "/")) {
                            return true;
                        }
                        // Error banner?
                        List<WebElement> errs = d.findElements(By.xpath(
                                "//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',"
                                        + " 'abcdefghijklmnopqrstuvwxyz'), 'invalid') or "
                                        + "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',"
                                        + " 'abcdefghijklmnopqrstuvwxyz'), 'incorrect')]"));
                        return !errs.isEmpty();
                    });

            String currentUrl = driver.getCurrentUrl();
            logStep("Post-submit URL: " + currentUrl);
            ScreenshotUtil.captureScreenshot("bcesiq_post_login");

            Assert.assertFalse(currentUrl.endsWith("/login") || currentUrl.endsWith("/login/"),
                    "Still on login page after submit &mdash; credentials rejected or auth failed. URL: " + currentUrl);

            // Sanity: no "invalid credentials" banner visible
            List<WebElement> errorBanners = driver.findElements(By.xpath(
                    "//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',"
                            + " 'abcdefghijklmnopqrstuvwxyz'), 'invalid credentials')]"));
            Assert.assertTrue(errorBanners.isEmpty(),
                    "Login returned 'Invalid credentials' banner &mdash; check BCES_IQ credentials");

            ExtentReportManager.logPass("BCES-IQ login succeeded. URL: " + currentUrl);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("bcesiq_login_error");
            Assert.fail("BCES-IQ login failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BcesIq_03 — Post-login shell renders (nav menu reachable)
    // ================================================================
    @Test(priority = 3, description = "BCES-IQ: Post-login app shell renders with navigation")
    public void testTC_BcesIq_03_PostLoginShell() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_DASHBOARD,
                "TC_BcesIq_03_PostLoginShell");
        try {
            driver.get(AppConstants.BASE_URL);
            new WebDriverWait(driver, Duration.ofSeconds(LOGIN_TIMEOUT))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
            loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);

            // Wait up to 30s for nav container to render — accept nav/aside/sidebar/drawer.
            boolean hasNav = waitForNavMenu(30);
            ScreenshotUtil.captureScreenshot("bcesiq_post_login_shell");

            Assert.assertTrue(hasNav,
                    "Post-login shell did not render any nav container within 30s on " + AppConstants.BASE_URL
                            + ". Checked: nav, aside, [class*='Sidebar'], [class*='MuiDrawer'], [role='navigation'].");

            // Capture any visible nav item labels (diagnostic, non-blocking)
            String navSnippet = captureNavSnippet();
            logStep("Visible nav items: " + navSnippet);
            ExtentReportManager.logPass("BCES-IQ app shell renders with navigation. Nav items: " + navSnippet);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("bcesiq_shell_error");
            Assert.fail("BCES-IQ post-login shell did not render: " + e.getMessage());
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Poll for a navigation container for up to {@code timeoutSeconds}.
     * Accepts nav / aside / sidebar / drawer / role=navigation. Dismisses
     * MUI backdrops between polls to handle late-arriving update alerts.
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
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    /**
     * Inline backdrop cleanup — this class doesn't extend BaseTest, so we
     * cannot reuse BaseTest#dismissBackdrops(). Same logic here.
     */
    private void dismissBackdrops() {
        try {
            js.executeScript(
                    "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop')"
                            + ".forEach(function(b){b.style.display='none';b.style.pointerEvents='none';});"
                            + "var btns = document.querySelectorAll('button');"
                            + "for (var i = 0; i < btns.length; i++) {"
                            + "  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }"
                            + "}"
            );
        } catch (Exception ignored) {}
    }

    private String captureNavSnippet() {
        try {
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) js.executeScript(
                    "var seen = new Set();"
                            + "var els = document.querySelectorAll('nav a, nav button, aside a, aside button, "
                            + "  [class*=\"Sidebar\" i] a, [class*=\"Sidebar\" i] button,"
                            + "  [class*=\"MuiDrawer\"] a, [class*=\"MuiDrawer\"] button,"
                            + "  [role=\"navigation\"] a, [role=\"navigation\"] button');"
                            + "var out = [];"
                            + "for (var e of els) {"
                            + "  var r = e.getBoundingClientRect();"
                            + "  if (r.width < 5 || r.height < 5) continue;"
                            + "  var t = (e.textContent || '').trim();"
                            + "  if (t && t.length > 0 && t.length < 40 && !seen.has(t)) { seen.add(t); out.push(t); }"
                            + "}"
                            + "return out;");
            if (items == null || items.isEmpty()) return "(none detected)";
            return String.join(", ", items.size() > 10 ? items.subList(0, 10) : items);
        } catch (Exception e) { return "(error: " + e.getMessage() + ")"; }
    }

    private void logStep(String msg) {
        ExtentReportManager.logInfo(msg);
        System.out.println("   " + msg);
    }
}
