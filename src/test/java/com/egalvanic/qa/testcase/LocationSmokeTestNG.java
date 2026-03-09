package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.LocationPage;
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
 * Smoke Tests for Location CRUD operations
 * Create Building > Floor > Room, Read, Update, Delete
 */
public class LocationSmokeTestNG {

    private WebDriver driver;
    private LocationPage locationPage;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    private ExtentTest moduleTest;
    private ExtentTest featureTest;
    private ExtentTest testCase;

    private static final String BASE_URL = ConfigReader.getBaseUrl();
    private static final String EMAIL = ConfigReader.getUserEmail();
    private static final String PASSWORD = ConfigReader.getUserPassword();
    private static final int DEFAULT_TIMEOUT = ConfigReader.getDefaultTimeout();

    // Test data with unique timestamps
    private static final String BUILDING_NAME = "SmokeTest_Building_" + System.currentTimeMillis();
    private static final String FLOOR_NAME = "SmokeTest_Floor_" + System.currentTimeMillis();
    private static final String ROOM_NAME = "SmokeTest_Room_" + System.currentTimeMillis();
    private static final String ACCESS_NOTES = "Smoke test access notes";

    @BeforeClass
    public void suiteSetup() {
        ReportManager.initReports("LocationSmokeTestReport.html");
        moduleTest = ReportManager.createTest("Location Module");
        moduleTest.assignCategory("Location");
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
        js.executeScript("document.body.style.zoom='90%';");

        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
        locationPage = new LocationPage(driver);

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

    // --- CRUD Tests ---

    @Test(priority = 1, description = "Smoke: Create Building > Floor > Room hierarchy")
    public void testCreateLocationHierarchy() {
        testCase = ReportManager.createNode(featureTest, "TC_Location_Create");
        testCase.log(Status.INFO, "Creating location hierarchy");

        try {
            locationPage.navigateToLocations();
            Assert.assertTrue(locationPage.isOnLocationsPage(), "Not on Locations page");
            takeScreenshot("locations_page_loaded");

            // Create Building
            locationPage.createBuilding(BUILDING_NAME, ACCESS_NOTES);
            testCase.log(Status.INFO, "Building created: " + BUILDING_NAME);
            takeScreenshot("building_created");

            // Create Floor under Building
            locationPage.createFloor(BUILDING_NAME, FLOOR_NAME, ACCESS_NOTES + " floor");
            testCase.log(Status.INFO, "Floor created: " + FLOOR_NAME);
            takeScreenshot("floor_created");

            // Create Room under Floor
            locationPage.createRoom(FLOOR_NAME, ROOM_NAME, ACCESS_NOTES + " room");
            testCase.log(Status.INFO, "Room created: " + ROOM_NAME);
            takeScreenshot("room_created");

            testCase.log(Status.PASS, "Full location hierarchy created successfully");

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Location creation failed: " + e.getMessage());
            takeScreenshot("location_create_error");
            Assert.fail("Location creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Verify created locations are visible", dependsOnMethods = "testCreateLocationHierarchy")
    public void testReadLocations() {
        testCase = ReportManager.createNode(featureTest, "TC_Location_Read");
        testCase.log(Status.INFO, "Verifying locations are visible");

        try {
            locationPage.navigateToLocations();

            boolean buildingVisible = locationPage.isLocationVisible(BUILDING_NAME);
            Assert.assertTrue(buildingVisible, "Building not visible: " + BUILDING_NAME);
            testCase.log(Status.INFO, "Building visible: " + BUILDING_NAME);

            locationPage.expandNode(BUILDING_NAME);

            boolean floorVisible = locationPage.isLocationVisible(FLOOR_NAME);
            Assert.assertTrue(floorVisible, "Floor not visible: " + FLOOR_NAME);
            testCase.log(Status.INFO, "Floor visible: " + FLOOR_NAME);

            takeScreenshot("locations_verified");
            testCase.log(Status.PASS, "All created locations are visible in the tree");

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Location read failed: " + e.getMessage());
            takeScreenshot("location_read_error");
            Assert.fail("Location read failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Update building name", dependsOnMethods = "testReadLocations")
    public void testUpdateLocation() {
        testCase = ReportManager.createNode(featureTest, "TC_Location_Update");
        String updatedName = BUILDING_NAME + "_Updated";
        testCase.log(Status.INFO, "Updating building to: " + updatedName);

        try {
            locationPage.navigateToLocations();
            locationPage.selectAndEditLocation(BUILDING_NAME, updatedName);
            takeScreenshot("location_updated");

            testCase.log(Status.PASS, "Location updated successfully");

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Location update failed: " + e.getMessage());
            takeScreenshot("location_update_error");
            Assert.fail("Location update failed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Smoke: Delete the created room", dependsOnMethods = "testCreateLocationHierarchy")
    public void testDeleteLocation() {
        testCase = ReportManager.createNode(featureTest, "TC_Location_Delete");
        testCase.log(Status.INFO, "Deleting room: " + ROOM_NAME);

        try {
            locationPage.navigateToLocations();
            locationPage.expandNode(BUILDING_NAME);
            locationPage.expandNode(FLOOR_NAME);

            locationPage.deleteLocation(ROOM_NAME);
            locationPage.confirmDelete();
            takeScreenshot("location_deleted");

            testCase.log(Status.PASS, "Location deleted successfully");

        } catch (Exception e) {
            testCase.log(Status.FAIL, "Location delete failed: " + e.getMessage());
            takeScreenshot("location_delete_error");
            Assert.fail("Location delete failed: " + e.getMessage());
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
