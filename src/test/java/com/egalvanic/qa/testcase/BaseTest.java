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

    @AfterSuite
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
        System.out.println("   - Detailed: " + ExtentReportManager.getDetailedReportPath());
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

        // Disable "Save password?" popup
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);

        // Use "eager" page load strategy to avoid 300s hangs on SPA navigations
        opts.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

        if ("true".equals(System.getProperty("headless"))) {
            opts.addArguments("--headless=new");
        }

        // Wrap ChromeDriver with self-healing capabilities — ALL findElement/findElements
        // calls now auto-retry, recover from stale elements, and try alternative locators.
        // Existing test code requires ZERO changes.
        driver = SelfHealingDriver.wrap(new ChromeDriver(opts));
        driver.manage().window().maximize();
        // Reduce pageLoad timeout from default 300s to 60s to prevent browser death
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.body.style.zoom='80%';");

        // Install flakiness prevention interceptors (network tracking + console error capture)
        FlakinessPrevention.installNetworkInterceptor(driver);
        FlakinessPrevention.installConsoleErrorCapture(driver);

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
    }

    @AfterClass
    public void classTeardown() {
        if (driver != null) {
            driver.quit();
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
    }

    /**
     * If the previous test left an error overlay or crash page,
     * recover before starting the next test.
     */
    private void recoverFromErrorPage() {
        try {
            if (!isApplicationErrorPage()) return;

            System.out.println("[BaseTest] Error page detected — recovering before next test...");

            // Strategy 1: Dismiss Sentry dialog
            try { driver.findElement(By.xpath("//button[contains(@aria-label,'Close')]")).click(); pause(500); } catch (Exception ignored) {}

            // Strategy 2: Navigate away using sidebar nav (preserves SPA state unlike page refresh)
            try {
                By locationsNav = By.xpath("//span[normalize-space()='Locations'] | //a[normalize-space()='Locations']");
                driver.findElement(locationsNav).click();
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
            if (driver.findElements(By.id("email")).size() > 0) {
                System.out.println("[BaseTest] Re-logging in after recovery...");
                loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                pause(2000);
                dashboardPage.waitForDashboard();
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
                    SmartBugDetector.analyze(driver, testName, result.getThrowable(), duration);
                } catch (Exception e) {
                    System.out.println("[BaseTest] SmartBugDetector analysis failed: " + e.getMessage());
                }
            }
        } else if (result.getStatus() == ITestResult.SKIP) {
            ExtentReportManager.logSkip("Test skipped: " + result.getMethod().getMethodName());
        }

        ExtentReportManager.removeTests();

        System.out.println("Test completed in " + formatDuration(duration));
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
                pause(2000);

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

                // Wait for login page to load
                new WebDriverWait(driver, Duration.ofSeconds(AppConstants.DEFAULT_TIMEOUT))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));

                loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                pause(2000);

                // Check for error after login
                if (isApplicationErrorPage()) {
                    System.out.println("Application Error after login on attempt " + attempt);
                    if (attempt < maxRetries) continue;
                }

                dashboardPage.waitForDashboard();
                dismissBackdrops(); // dismiss app update alert before site selection
                pause(1000);
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

            // Wait for facility input to appear (React may still be hydrating)
            try {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.presenceOfElementLocated(facilityInput));
            } catch (Exception waitTimeout) {
                System.out.println("Facility selector not found after 15s — skipping site selection");
                return;
            }

            // Check if correct site is already selected
            String currentValue = driver.findElement(facilityInput).getAttribute("value");
            if (currentValue != null && currentValue.toLowerCase().contains(
                    AppConstants.TEST_SITE_NAME.toLowerCase())) {
                System.out.println("Correct site already selected: " + currentValue);
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
        } catch (Exception e) {
            System.out.println("Site selection skipped or failed: " + e.getMessage());
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

    protected void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    protected void shortWait() { pause(200); }
    protected void mediumWait() { pause(400); }
    protected void longWait() { pause(800); }

    // ================================================================
    // MUI BACKDROP HANDLING — prevents ElementClickInterceptedException
    // ================================================================

    /**
     * Remove all MUI backdrop overlays that intercept clicks in headless Chrome.
     * Called automatically before every test via @BeforeMethod.
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
     * with JS click fallback. Use this instead of element.click() to avoid
     * ElementClickInterceptedException in headless Chrome CI/CD.
     */
    protected void safeClick(WebElement element) {
        dismissBackdrops();
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // Backdrop appeared between dismissal and click — use JS
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", element);
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
        } catch (ElementClickInterceptedException e) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", element);
        }
    }
}
