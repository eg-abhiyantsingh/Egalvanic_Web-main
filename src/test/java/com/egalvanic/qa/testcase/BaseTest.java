package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.AssetPage;
import com.egalvanic.qa.pageobjects.DashboardPage;
import com.egalvanic.qa.pageobjects.LocationPage;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base Test - Parent class for all Web Test classes
 * Handles driver lifecycle, report initialization, login, and site selection
 */
public class BaseTest {

    protected WebDriver driver;
    protected LoginPage loginPage;
    protected DashboardPage dashboardPage;
    protected AssetPage assetPage;
    protected LocationPage locationPage;

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

        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     eGalvanic Web Automation - Test Suite Complete");
        System.out.println("     " + timestamp());
        System.out.println("==============================================================");
        System.out.println("Reports generated:");
        System.out.println("   - Detailed: " + ExtentReportManager.getDetailedReportPath());
        System.out.println("   - Client:   " + ExtentReportManager.getClientReportPath());
    }

    // ================================================================
    // TEST LEVEL SETUP/TEARDOWN
    // ================================================================

    @BeforeMethod
    public void testSetup() {
        testStartTime = System.currentTimeMillis();

        // Create ChromeDriver (Selenium 4.29+ manages driver automatically)
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);

        if ("true".equals(System.getProperty("headless"))) {
            opts.addArguments("--headless=new");
        }

        driver = new ChromeDriver(opts);
        driver.manage().window().maximize();

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.body.style.zoom='80%';");

        // Set driver for screenshot utility
        ScreenshotUtil.setDriver(driver);

        // Initialize page objects
        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
        assetPage = new AssetPage(driver);
        locationPage = new LocationPage(driver);

        // Login and select site
        loginAndSelectSite();
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;

        // Capture failure screenshot
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

        // Quit driver
        if (driver != null) {
            driver.quit();
            driver = null;
        }

        System.out.println("Test completed in " + formatDuration(duration));
    }

    // ================================================================
    // LOGIN AND SITE SELECTION HELPERS
    // ================================================================

    /**
     * Perform full login and site selection flow
     */
    protected void loginAndSelectSite() {
        driver.get(AppConstants.BASE_URL);

        // Wait for login page to load
        new WebDriverWait(driver, Duration.ofSeconds(AppConstants.DEFAULT_TIMEOUT))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));

        loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
        dashboardPage.waitForDashboard();
        selectTestSite();
    }

    /**
     * Select "Test Site" from the site dropdown
     */
    protected void selectTestSite() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(AppConstants.DEFAULT_TIMEOUT));

            w.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'MuiAutocomplete-root')]"))).click();
            w.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@role='listbox']")));
            js.executeScript("document.querySelectorAll('ul[role=\"listbox\"]').forEach(e => e.scrollTop=e.scrollHeight);");

            By testSite = By.xpath("//li[normalize-space()='" + AppConstants.TEST_SITE_NAME + "']");
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
}
