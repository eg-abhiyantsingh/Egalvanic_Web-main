package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.AssetPage;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.pageobjects.DashboardPage;
import com.egalvanic.qa.utils.ConfigReader;
import com.egalvanic.qa.utils.ReportManager;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Smoke Tests for Asset CRUD operations
 * Create -> Read -> Update -> Delete
 */
public class AssetSmokeTestNG {

    private WebDriver driver;
    private AssetPage assetPage;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    private ExtentTest moduleTest;
    private ExtentTest featureTest;
    private ExtentTest testCase;

    private static final String BASE_URL = ConfigReader.getBaseUrl();
    private static final String EMAIL = ConfigReader.getUserEmail();
    private static final String PASSWORD = ConfigReader.getUserPassword();
    private static final int DEFAULT_TIMEOUT = ConfigReader.getDefaultTimeout();

    // Test data
    private static final String TEST_ASSET_NAME = "SmokeTest_Asset_" + System.currentTimeMillis();
    private static final String TEST_QR_CODE = "QR_" + System.currentTimeMillis();
    private static final String TEST_ASSET_CLASS = "3-Pole Breaker";
    private static final String TEST_CONDITION = "2";
    private static final String TEST_MODEL = "TestModel";
    private static final String TEST_NOTES = "Smoke test notes";
    private static final String TEST_AMPERE_RATING = "20 A";
    private static final String TEST_MANUFACTURER = "Eaton";
    private static final String TEST_CATALOG_NUMBER = "CAT001";
    private static final String TEST_BREAKER_SETTINGS = "Setting1";
    private static final String TEST_INTERRUPTING_RATING = "10 kA";
    private static final String TEST_KA_RATING = "18 kA";
    private static final String TEST_REPLACEMENT_COST = "30000";

    @BeforeClass
    public void suiteSetup() {
        ReportManager.initReports("AssetSmokeTestReport.html");
        moduleTest = ReportManager.createTest("Asset Module");
        moduleTest.assignCategory("Asset");
        featureTest = ReportManager.createNode(moduleTest, "CRUD Operations");
        featureTest.assignCategory("Smoke");
    }

    @BeforeMethod
    public void setUp() {
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

        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
        assetPage = new AssetPage(driver);

        // Login and select site
        driver.get(BASE_URL);
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        loginPage.login(EMAIL, PASSWORD);
        dashboardPage.waitForDashboard();
        selectTestSite();
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @AfterClass
    public void suiteTearDown() {
        ReportManager.flushReports();
    }

    // --- CRUD Tests (ordered via priority) ---

    @Test(priority = 1, description = "Smoke: Create a new asset with all fields")
    public void testCreateAsset() {
        testCase = ReportManager.createNode(featureTest, "TC_Asset_Create");
        testCase.log(Status.INFO, "Creating asset: " + TEST_ASSET_NAME);

        try {
            assetPage.navigateToAssets();
            takeScreenshot("asset_page_loaded");
            testCase.log(Status.INFO, "Navigated to Assets page");

            assetPage.openCreateAssetForm();
            testCase.log(Status.INFO, "Opened create asset form");

            assetPage.fillBasicInfo(TEST_ASSET_NAME, TEST_QR_CODE, TEST_ASSET_CLASS, TEST_CONDITION);
            testCase.log(Status.INFO, "Filled basic info");

            assetPage.fillCoreAttributes(TEST_MODEL, TEST_NOTES, TEST_AMPERE_RATING,
                    TEST_MANUFACTURER, TEST_CATALOG_NUMBER, TEST_BREAKER_SETTINGS,
                    TEST_INTERRUPTING_RATING, TEST_KA_RATING);
            testCase.log(Status.INFO, "Filled core attributes");

            assetPage.fillReplacementCost(TEST_REPLACEMENT_COST);
            assetPage.submitCreateAsset();
            takeScreenshot("asset_created");

            boolean success = assetPage.waitForCreateSuccess();
            Assert.assertTrue(success, "Asset creation did not complete successfully");
            testCase.log(Status.PASS, "Asset created successfully: " + TEST_ASSET_NAME);

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Asset creation failed: " + e.getMessage());
            takeScreenshot("asset_create_error");
            Assert.fail("Asset creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Read/verify asset exists in grid", dependsOnMethods = "testCreateAsset")
    public void testReadAsset() {
        testCase = ReportManager.createNode(featureTest, "TC_Asset_Read");
        testCase.log(Status.INFO, "Searching for asset: " + TEST_ASSET_NAME);

        try {
            assetPage.navigateToAssets();
            boolean found = assetPage.isAssetVisible(TEST_ASSET_NAME);
            takeScreenshot("asset_search_result");

            Assert.assertTrue(found, "Asset not found in grid: " + TEST_ASSET_NAME);
            testCase.log(Status.PASS, "Asset found in grid: " + TEST_ASSET_NAME);

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Asset read failed: " + e.getMessage());
            takeScreenshot("asset_read_error");
            Assert.fail("Asset read failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Update asset model and notes", dependsOnMethods = "testReadAsset")
    public void testUpdateAsset() {
        testCase = ReportManager.createNode(featureTest, "TC_Asset_Update");
        testCase.log(Status.INFO, "Updating asset: " + TEST_ASSET_NAME);

        try {
            assetPage.navigateToAssets();
            assetPage.searchAsset(TEST_ASSET_NAME);
            assetPage.openEditForFirstAsset();
            testCase.log(Status.INFO, "Opened edit form");

            assetPage.editModel("UpdatedModel");
            assetPage.editNotes("Updated notes from smoke test");
            assetPage.saveChanges();
            takeScreenshot("asset_updated");

            boolean success = assetPage.waitForEditSuccess();
            Assert.assertTrue(success, "Asset update did not complete successfully");
            testCase.log(Status.PASS, "Asset updated successfully");

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Asset update failed: " + e.getMessage());
            takeScreenshot("asset_update_error");
            Assert.fail("Asset update failed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Smoke: Delete the created asset", dependsOnMethods = "testUpdateAsset")
    public void testDeleteAsset() {
        testCase = ReportManager.createNode(featureTest, "TC_Asset_Delete");
        testCase.log(Status.INFO, "Deleting asset: " + TEST_ASSET_NAME);

        try {
            assetPage.navigateToAssets();
            assetPage.searchAsset(TEST_ASSET_NAME);
            assetPage.deleteFirstAsset();
            testCase.log(Status.INFO, "Clicked delete on asset");

            assetPage.confirmDelete();
            takeScreenshot("asset_deleted");

            testCase.log(Status.PASS, "Asset deleted successfully");

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Asset delete failed: " + e.getMessage());
            takeScreenshot("asset_delete_error");
            Assert.fail("Asset delete failed: " + e.getMessage());
        }
    }

    // --- Helpers ---

    private void selectTestSite() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));

            w.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'MuiAutocomplete-root')]"))).click();
            w.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@role='listbox']")));
            js.executeScript("document.querySelectorAll('ul[role=\"listbox\"]').forEach(e => e.scrollTop=e.scrollHeight);");

            By testSite = By.xpath("//li[normalize-space()='Test Site']");
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

    private void takeScreenshot(String name) {
        try {
            String fname = name + "_" + stamp() + ".png";
            String destPath = "test-output/screenshots/" + fname;
            Files.createDirectories(Path.of("test-output/screenshots"));
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(Path.of(destPath), bytes);
            if (testCase != null) {
                testCase.addScreenCaptureFromBase64String(Base64.getEncoder().encodeToString(bytes), name);
            }
        } catch (Exception e) {
            System.out.println("Screenshot failed: " + e.getMessage());
        }
    }

    private String stamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
