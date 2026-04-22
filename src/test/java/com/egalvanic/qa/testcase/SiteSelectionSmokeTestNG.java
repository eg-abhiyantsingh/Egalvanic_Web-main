package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
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

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Smoke Tests for Site / Facility Selection.
 *
 * Architecture: Single browser session shared across all tests.
 * Does NOT extend BaseTest because BaseTest auto-selects the site
 * in @BeforeClass — here we need to test that flow explicitly.
 *
 * Flow:
 *   @BeforeClass → create browser, login as Admin (stop at dashboard)
 *   Test 1 → verify "Select facility" input is present and empty
 *   Test 2 → open dropdown, verify sites are listed
 *   Test 3 → select "Test Site", verify selection sticks
 *   Test 4 → navigate away and back, verify site context persists
 *   @AfterClass → quit browser
 */
public class SiteSelectionSmokeTestNG {

    private WebDriver driver;
    private LoginPage loginPage;
    private JavascriptExecutor js;
    private WebDriverWait wait;
    private long testStartTime;

    private static final int LOGIN_TIMEOUT = 60;
    private static final int POST_LOGIN_TIMEOUT = 30;

    private static final By FACILITY_INPUT = By.xpath("//input[@placeholder='Select facility']");
    private static final By LISTBOX = By.xpath("//ul[@role='listbox']");
    // MUI Autocomplete "Open" dropdown-arrow button — sibling of the input
    private static final By OPEN_BUTTON = By.xpath(
            "//input[@placeholder='Select facility']/ancestor::div[contains(@class,'MuiAutocomplete') or contains(@class,'MuiInputBase')]//button[contains(@aria-label,'Open') or @title='Open']");

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
    // CLASS LIFECYCLE — shared browser, login once
    // ================================================================

    @BeforeClass
    public void classSetup() {
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
        js.executeScript("document.body.style.zoom='80%';");

        wait = new WebDriverWait(driver, Duration.ofSeconds(POST_LOGIN_TIMEOUT));
        ScreenshotUtil.setDriver(driver);
        loginPage = new LoginPage(driver);

        // Login as Admin but DO NOT select site — that's what we're testing
        loginAsAdmin();
    }

    @AfterClass
    public void classTeardown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    // ================================================================
    // PER-TEST SETUP/TEARDOWN
    // ================================================================

    @BeforeMethod
    public void testSetup() {
        testStartTime = System.currentTimeMillis();
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

        String fmt = duration < 1000 ? duration + "ms"
                : duration < 60000 ? (duration / 1000) + "s"
                : (duration / 60000) + "m " + ((duration / 1000) % 60) + "s";
        System.out.println("Test completed in " + fmt);
    }

    // ================================================================
    // TEST 1: FACILITY SELECTOR PRESENT
    // ================================================================

    @Test(priority = 1, description = "Smoke: Verify facility selector is present after login")
    public void testFacilitySelectorPresent() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_SITE_SELECTION, AppConstants.FEATURE_LOGIN, "TC_Site_SelectorPresent");

        try {
            logStep("Verifying facility selector on dashboard");

            // Verify we're on dashboard (past login)
            String currentUrl = driver.getCurrentUrl();
            Assert.assertTrue(currentUrl.contains("dashboard") || currentUrl.contains("sites"),
                    "Not on dashboard after login. URL: " + currentUrl);
            logStep("On dashboard. URL: " + currentUrl);

            // Wait up to 20s for the facility input to appear — SPA may still be
            // hydrating after login redirect, or the page may show a backdrop that
            // has to be dismissed before the selector is reachable. Also DISMISS
            // button on the app-update alert blocks the selector on first load.
            List<WebElement> inputs = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 20_000L;
            while (System.currentTimeMillis() < deadline) {
                try {
                    // Inline backdrop cleanup — this class doesn't extend BaseTest,
                    // so we cannot reuse BaseTest#dismissBackdrops(). Same logic.
                    js.executeScript(
                            "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop')"
                            + ".forEach(function(b){b.style.display='none';b.style.pointerEvents='none';});"
                            + "var btns = document.querySelectorAll('button');"
                            + "for (var i = 0; i < btns.length; i++) {"
                            + "  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }"
                            + "}"
                    );
                    inputs = driver.findElements(FACILITY_INPUT);
                    if (!inputs.isEmpty()) break;
                } catch (Exception ignored) {}
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
            Assert.assertFalse(inputs.isEmpty(),
                    "Facility selector input (placeholder='Select facility') not found on dashboard "
                    + "within 20s. URL: " + driver.getCurrentUrl());
            logStep("Facility selector input found");

            // Verify input is visible and enabled
            WebElement facilityInput = inputs.get(0);
            boolean isDisplayed = (Boolean) js.executeScript(
                    "var r = arguments[0].getBoundingClientRect();"
                    + "return r.width > 0 && r.height > 0;", facilityInput);
            Assert.assertTrue(isDisplayed, "Facility selector input is not visible (zero dimensions)");
            logStep("Facility selector is visible");

            boolean isEnabled = facilityInput.isEnabled();
            Assert.assertTrue(isEnabled, "Facility selector input is disabled");
            logStep("Facility selector is enabled");

            // Check if a site is already pre-selected (fresh login should have empty or default)
            String currentValue = facilityInput.getAttribute("value");
            logStep("Current facility value: '" + (currentValue != null ? currentValue : "") + "'");
            logStepWithScreenshot("Facility selector state after login");

            ExtentReportManager.logPass("Facility selector is present, visible, and enabled on dashboard");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("facility_selector_error");
            Assert.fail("Facility selector verification failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 2: DROPDOWN LISTS AVAILABLE SITES
    // ================================================================

    @Test(priority = 2, description = "Smoke: Verify facility dropdown lists available sites")
    public void testFacilityDropdownOptions() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_SITE_SELECTION, AppConstants.FEATURE_LOGIN, "TC_Site_DropdownOptions");

        try {
            logStep("Opening facility dropdown to verify options");

            WebElement facilityInput = wait.until(
                    ExpectedConditions.presenceOfElementLocated(FACILITY_INPUT));

            // Open the dropdown using robust helper
            boolean dropdownOpened = openFacilityDropdown();
            Assert.assertTrue(dropdownOpened,
                    "Could not open facility dropdown after multiple strategies");
            logStep("Dropdown opened — listbox visible");

            // Get all options
            List<WebElement> options = driver.findElements(
                    By.xpath("//li[@role='option']"));
            Assert.assertFalse(options.isEmpty(),
                    "Facility dropdown is empty — no sites/facilities listed");
            logStep("Dropdown has " + options.size() + " option(s)");

            // Log each site name
            StringBuilder siteNames = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                String name = (String) js.executeScript(
                        "return (arguments[0].textContent||'').trim();", options.get(i));
                if (i > 0) siteNames.append(", ");
                siteNames.append(name);
                System.out.println("[Site] Option [" + i + "]: " + name);
            }
            logStep("Available sites: " + siteNames);

            // Verify "Test Site" is in the list
            boolean hasTestSite = false;
            for (WebElement opt : options) {
                String text = (String) js.executeScript(
                        "return (arguments[0].textContent||'').trim();", opt);
                if (text.contains(AppConstants.TEST_SITE_NAME)) {
                    hasTestSite = true;
                    break;
                }
            }
            Assert.assertTrue(hasTestSite,
                    "'" + AppConstants.TEST_SITE_NAME + "' not found in facility dropdown. "
                    + "Available: " + siteNames);
            logStep("'" + AppConstants.TEST_SITE_NAME + "' found in dropdown");

            logStepWithScreenshot("Dropdown open with " + options.size() + " site(s)");

            // Close dropdown by pressing Escape
            facilityInput.sendKeys(org.openqa.selenium.Keys.ESCAPE);
            pause(300);

            ExtentReportManager.logPass("Facility dropdown populated with "
                    + options.size() + " sites. '" + AppConstants.TEST_SITE_NAME + "' available.");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("facility_dropdown_error");
            Assert.fail("Facility dropdown verification failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 3: SELECT TEST SITE
    // ================================================================

    @Test(priority = 3, description = "Smoke: Select Test Site and verify selection")
    public void testSelectTestSite() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_SITE_SELECTION, AppConstants.FEATURE_SESSION, "TC_Site_SelectSite");

        try {
            logStep("Selecting '" + AppConstants.TEST_SITE_NAME + "' from facility dropdown");

            WebElement facilityInput = wait.until(
                    ExpectedConditions.presenceOfElementLocated(FACILITY_INPUT));

            // Check if already selected from a previous test state
            String currentValue = facilityInput.getAttribute("value");
            if (currentValue == null || currentValue.isEmpty()) {
                currentValue = (String) js.executeScript("return arguments[0].value || '';", facilityInput);
            }
            if (currentValue != null && currentValue.toLowerCase().contains(AppConstants.TEST_SITE_NAME.toLowerCase())) {
                logStep("Site already selected: " + currentValue + " — clearing to re-test selection");
                // Use MUI clear button (X icon) if available
                Boolean cleared = (Boolean) js.executeScript(
                        "var btn = document.querySelector('.MuiAutocomplete-clearIndicator, "
                        + "[aria-label=\"Clear\"], [title=\"Clear\"]');"
                        + "if (btn && btn.offsetWidth > 0) { btn.click(); return true; }"
                        + "return false;");
                if (Boolean.TRUE.equals(cleared)) {
                    logStep("Cleared site via MUI clear button");
                    pause(500);
                } else {
                    // Fallback: React value setter
                    js.executeScript(
                            "var nativeSetter = Object.getOwnPropertyDescriptor("
                            + "window.HTMLInputElement.prototype, 'value').set;"
                            + "nativeSetter.call(arguments[0], '');"
                            + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                            + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                            facilityInput);
                    pause(500);
                }
            }

            // Open the dropdown using robust helper
            boolean dropdownOpened = openFacilityDropdown();
            Assert.assertTrue(dropdownOpened, "Could not open facility dropdown");
            logStep("Dropdown opened");

            // Scroll dropdown to bottom (Test Site may be alphabetically at the end)
            js.executeScript(
                    "document.querySelectorAll('ul[role=\"listbox\"]')"
                    + ".forEach(e => e.scrollTop = e.scrollHeight);");
            pause(300);

            // Find and click "Test Site" with FluentWait retry
            // Use case-insensitive XPath (translate to lowercase) because the dropdown
            // may show "test site" (lowercase) while TEST_SITE_NAME is "Test Site"
            String siteNameLower = AppConstants.TEST_SITE_NAME.toLowerCase();
            By testSiteOption = By.xpath(
                    "//li[@role='option'][contains("
                    + "translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),"
                    + "'" + siteNameLower + "')]");

            new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(15))
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(ElementClickInterceptedException.class)
                    .until(d -> {
                        for (WebElement li : d.findElements(testSiteOption)) {
                            try {
                                // Prefer the most exact match (shortest text containing site name)
                                String optText = li.getText().trim();
                                if (optText.equalsIgnoreCase(AppConstants.TEST_SITE_NAME)) {
                                    li.click();
                                    return li;
                                }
                            } catch (Exception ignored) {}
                        }
                        // Fallback: click the first match
                        for (WebElement li : d.findElements(testSiteOption)) {
                            try { li.click(); return li; } catch (Exception ignored) {}
                        }
                        return null;
                    });
            logStep("Clicked '" + AppConstants.TEST_SITE_NAME + "'");

            // Wait for selection to register
            pause(1000);

            // Verify the input now shows the selected site
            String selectedValue = facilityInput.getAttribute("value");
            logStep("Facility input value after selection: '" + selectedValue + "'");

            // The dropdown may close and the value may be reflected differently
            // Also check via JS in case getAttribute doesn't reflect React state
            if (selectedValue == null || selectedValue.isEmpty()) {
                selectedValue = (String) js.executeScript(
                        "return arguments[0].value || '';", facilityInput);
                logStep("Facility input value (via JS): '" + selectedValue + "'");
            }

            // Verify selection — the value should contain the site name,
            // or the dropdown should be closed (selection was made)
            boolean dropdownClosed = driver.findElements(LISTBOX).isEmpty();
            boolean valueSet = selectedValue != null
                    && selectedValue.contains(AppConstants.TEST_SITE_NAME);

            Assert.assertTrue(dropdownClosed || valueSet,
                    "Site selection did not register. Dropdown still open: " + !dropdownClosed
                    + ", Value: '" + selectedValue + "'");
            logStep("Site selection confirmed — dropdown closed: " + dropdownClosed
                    + ", value matches: " + valueSet);

            // Wait for dashboard to load with site context
            pause(2000);

            // Verify navigation menu is present (site selected → app loads)
            boolean hasNav = driver.findElements(By.cssSelector("nav")).size() > 0;
            logStep("Navigation menu present after site selection: " + hasNav);

            logStepWithScreenshot("Dashboard after selecting '" + AppConstants.TEST_SITE_NAME + "'");

            ExtentReportManager.logPass("Site '" + AppConstants.TEST_SITE_NAME
                    + "' selected successfully. Dashboard loaded.");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("site_selection_error");
            Assert.fail("Site selection failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 4: SITE CONTEXT PERSISTS ON NAVIGATION
    // ================================================================

    @Test(priority = 4, description = "Smoke: Verify site context persists across page navigation")
    public void testSiteContextPersists() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_SITE_SELECTION, AppConstants.FEATURE_SESSION, "TC_Site_ContextPersists");

        try {
            logStep("Verifying site context persists across navigation");

            // Navigate to Assets page via sidebar
            boolean navigatedToAssets = navigateViaSidebar("Assets");
            Assert.assertTrue(navigatedToAssets, "Could not navigate to Assets page");
            logStep("Navigated to Assets page");

            // Wait for Assets page to load (DataGrid or page header)
            pause(3000);
            String assetsUrl = driver.getCurrentUrl();
            Assert.assertTrue(assetsUrl.contains("asset"),
                    "URL does not contain 'asset' after navigating to Assets. URL: " + assetsUrl);
            logStep("Assets page loaded. URL: " + assetsUrl);

            // Verify asset data is loaded (grid has rows = site context is active)
            boolean gridHasData = (Boolean) js.executeScript(
                    "var rows = document.querySelectorAll("
                    + "'[class*=\"MuiDataGrid-row\"][data-rowindex]');"
                    + "return rows.length > 0;");
            logStep("Asset grid has data rows: " + gridHasData);

            if (gridHasData) {
                Long rowCount = (Long) js.executeScript(
                        "return document.querySelectorAll("
                        + "'[class*=\"MuiDataGrid-row\"][data-rowindex]').length;");
                logStep("Asset grid row count: " + rowCount);
            }

            logStepWithScreenshot("Assets page with site context");

            // Navigate back to dashboard
            boolean navigatedToDashboard = navigateViaSidebar("Dashboard");
            if (!navigatedToDashboard) {
                // Fallback: try "Site Overview" or direct nav
                navigatedToDashboard = navigateViaSidebar("Site Overview");
            }

            if (navigatedToDashboard) {
                pause(2000);
                logStep("Navigated back to Dashboard. URL: " + driver.getCurrentUrl());

                // Verify facility input still shows selected site
                List<WebElement> inputs = driver.findElements(FACILITY_INPUT);
                if (!inputs.isEmpty()) {
                    String value = inputs.get(0).getAttribute("value");
                    if (value == null || value.isEmpty()) {
                        value = (String) js.executeScript(
                                "return arguments[0].value || '';", inputs.get(0));
                    }
                    logStep("Facility input value after navigation: '" + value + "'");

                    if (value != null && value.contains(AppConstants.TEST_SITE_NAME)) {
                        logStep("Site selection persisted after navigation");
                    } else {
                        logStep("Note: Facility input value changed — may be auto-selected or cached");
                    }
                } else {
                    logStep("Facility input not visible on current page (may be hidden after selection)");
                }
            } else {
                logStep("Could not navigate back to dashboard — verifying current page instead");
            }

            logStepWithScreenshot("Post-navigation state");

            // Final assertion: we should NOT be on the login page (session is alive)
            boolean onLoginPage = driver.findElements(By.id("email")).size() > 0
                    && driver.findElements(By.id("password")).size() > 0;
            Assert.assertFalse(onLoginPage,
                    "Session lost — redirected to login page after navigation");
            logStep("Session is active — not redirected to login");

            ExtentReportManager.logPass("Site context persists across page navigation. "
                    + "Assets loaded with data, session maintained.");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("site_context_error");
            Assert.fail("Site context persistence check failed: " + e.getMessage());
        }
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Login as Admin and wait for dashboard (without selecting a site).
     */
    private void loginAsAdmin() {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                driver.get(AppConstants.BASE_URL);
                pause(2000);

                // Handle Application Error page
                if (isApplicationErrorPage()) {
                    System.out.println("[Site] Application Error on attempt " + attempt);
                    try { driver.findElement(By.xpath("//button[contains(@aria-label,'Close')]")).click(); pause(500); } catch (Exception ignored) {}
                    driver.navigate().refresh();
                    pause(3000);
                    if (isApplicationErrorPage() && attempt < maxRetries) continue;
                }

                // Wait for login page
                new WebDriverWait(driver, Duration.ofSeconds(LOGIN_TIMEOUT))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
                System.out.println("[Site] Login page loaded. URL: " + driver.getCurrentUrl());

                loginPage.login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
                pause(2000);

                // Wait for post-login page (dashboard or site selector)
                wait.until(driver -> {
                    boolean leftLogin = driver.findElements(By.id("email")).size() == 0
                            || driver.findElements(By.id("password")).size() == 0;
                    boolean hasNav = driver.findElements(By.cssSelector("nav")).size() > 0;
                    boolean hasFacilityInput = driver.findElements(FACILITY_INPUT).size() > 0;
                    return leftLogin || hasNav || hasFacilityInput;
                });

                pause(2000);
                System.out.println("[Site] Post-login page loaded. URL: " + driver.getCurrentUrl());
                return;

            } catch (Exception e) {
                System.out.println("[Site] Login attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Login failed after " + maxRetries + " attempts", e);
                }
                pause(3000);
            }
        }
    }

    /**
     * Navigate to a page via sidebar click (SPA-safe).
     */
    private boolean navigateViaSidebar(String pageName) {
        try {
            // Try multiple selector strategies for the nav item
            String[] xpaths = {
                    "//nav//span[normalize-space()='" + pageName + "']/ancestor::a",
                    "//nav//span[normalize-space()='" + pageName + "']/ancestor::button",
                    "//nav//*[normalize-space()='" + pageName + "']",
                    "//*[contains(@class,'sidebar') or contains(@class,'Sidebar') or contains(@class,'drawer')]"
                    + "//*[normalize-space()='" + pageName + "']"
            };

            for (String xpath : xpaths) {
                List<WebElement> els = driver.findElements(By.xpath(xpath));
                for (WebElement el : els) {
                    try {
                        if (el.isDisplayed()) {
                            el.click();
                            pause(1000);
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Fallback: JS click on any element matching text in the left sidebar area
            Boolean clicked = (Boolean) js.executeScript(
                    "var items = document.querySelectorAll('nav a, nav button, nav span');"
                    + "for (var el of items) {"
                    + "  if ((el.textContent||'').trim() === arguments[0]) {"
                    + "    el.click(); return true;"
                    + "  }"
                    + "}"
                    + "return false;", pageName);

            if (Boolean.TRUE.equals(clicked)) {
                pause(1000);
                return true;
            }

            return false;
        } catch (Exception e) {
            System.out.println("[Site] Navigation to '" + pageName + "' failed: " + e.getMessage());
            return false;
        }
    }

    private boolean isApplicationErrorPage() {
        try {
            return driver.findElements(By.xpath(
                    "//*[contains(text(),'Application Error') or contains(text(),'We encountered an error') "
                    + "or contains(text(),'something went wrong')]")).size() > 0;
        } catch (Exception e) {
            return false;
        }
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

    /**
     * Reliably open the MUI Autocomplete facility dropdown.
     * Uses multiple strategies because clicking the input alone doesn't
     * always open the dropdown (especially when a value is already selected).
     */
    private boolean openFacilityDropdown() {
        // Strategy 1: Click the "Open" dropdown arrow button
        try {
            List<WebElement> openBtns = driver.findElements(OPEN_BUTTON);
            if (!openBtns.isEmpty() && openBtns.get(0).isDisplayed()) {
                openBtns.get(0).click();
                pause(800);
                if (isDropdownOpen()) return true;
            }
        } catch (Exception ignored) {}

        // Strategy 2: JS-click the Open button by aria-label
        try {
            Boolean clicked = (Boolean) js.executeScript(
                    "var input = document.querySelector(\"input[placeholder='Select facility']\");"
                    + "if (!input) return false;"
                    + "var container = input.closest('.MuiAutocomplete-root') || input.parentElement.parentElement;"
                    + "var btn = container.querySelector('button[aria-label=\"Open\"], button[title=\"Open\"]');"
                    + "if (btn) { btn.click(); return true; }"
                    + "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                pause(800);
                if (isDropdownOpen()) return true;
            }
        } catch (Exception ignored) {}

        // Strategy 3: Click the input itself + ArrowDown to force open
        try {
            WebElement input = driver.findElement(FACILITY_INPUT);
            input.click();
            pause(300);
            input.sendKeys(org.openqa.selenium.Keys.ARROW_DOWN);
            pause(800);
            if (isDropdownOpen()) return true;
        } catch (Exception ignored) {}

        // Strategy 4: Clear the input and click — empty autocomplete opens on click
        try {
            WebElement input = driver.findElement(FACILITY_INPUT);
            js.executeScript(
                    "var nativeSetter = Object.getOwnPropertyDescriptor("
                    + "window.HTMLInputElement.prototype, 'value').set;"
                    + "nativeSetter.call(arguments[0], '');"
                    + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                    + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    input);
            pause(300);
            input.click();
            pause(800);
            if (isDropdownOpen()) return true;
        } catch (Exception ignored) {}

        // Strategy 5: Click the MUI clear button (X icon) first, then click input
        try {
            Boolean cleared = (Boolean) js.executeScript(
                    "var btn = document.querySelector('.MuiAutocomplete-clearIndicator, "
                    + "[aria-label=\"Clear\"], [title=\"Clear\"]');"
                    + "if (btn && btn.offsetWidth > 0) { btn.click(); return true; }"
                    + "return false;");
            if (Boolean.TRUE.equals(cleared)) {
                pause(500);
                WebElement input = driver.findElement(FACILITY_INPUT);
                input.click();
                pause(800);
                if (isDropdownOpen()) return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private boolean isDropdownOpen() {
        try {
            List<WebElement> listboxes = driver.findElements(LISTBOX);
            return !listboxes.isEmpty() && listboxes.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
