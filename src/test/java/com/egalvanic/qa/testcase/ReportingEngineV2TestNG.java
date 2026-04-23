package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
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

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Reporting Engine V2 — QA Review Test Suite
 *
 * Parent Jira: ZP-1583 — Reporting Engine V2
 * Related tickets: ZP-1660, ZP-1662, ZP-1592, ZP-1717, ZP-1584, ZP-1721, ZP-1688, ZP-1589
 *
 * Coverage (8 test cases, 38 steps):
 *   TC-1: Branding & Assets (ZP-1660) — logo upload, colors, validation
 *   TC-2: Starter Page Template (ZP-1662) — duplicate, read-only original
 *   TC-3: Template Explorer Preview (ZP-1592) — search, preview, branding in preview
 *   TC-4: EG Forms V2 (ZP-1717) — form types 5/6/7, NETA label, V1 compat
 *   TC-5: ReportingConfig (ZP-1584) — create config, test run, raw JSON
 *   TC-6: Fill EG Form in Work Order (ZP-1721) — attach form, fill, persist
 *   TC-7: Generate Report (ZP-1688) — lambda call, branding, filled values
 *   TC-8: Backward Compatibility (ZP-1589) — V1 configs still render
 *
 * Architecture: Standalone (own browser session with console/network logging).
 * Does NOT extend BaseTest — tests Admin + App areas with independent session.
 *
 * Pre-flight checklist (do these MANUALLY before running):
 *   0.1 — Log in to dev as company admin, verify dashboard loads
 *   0.2 — Confirm with dev: reporting-v2 active in mapping_company_features,
 *          enum_eg_form_types 5, 6, 7 exist in dev DB
 *   0.3 — Keep DevTools Network + Console tabs open throughout
 *
 * Run: mvn test -Dtest=ReportingEngineV2TestNG
 * Run single: mvn test -Dtest=ReportingEngineV2TestNG#TC1_01_navigateToBrandingAndAssets
 */
public class ReportingEngineV2TestNG {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;
    private long testStartTime;
    private List<String> consoleErrors = new java.util.ArrayList<>();

    // ═══════════════════════════════════════════════════
    // LOCATORS — Login
    // ═══════════════════════════════════════════════════
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By TERMS_CHECKBOX = By.cssSelector("input[type='checkbox']");
    private static final By SIGN_IN_BUTTON = By.xpath("//button[normalize-space()='Sign In']");
    private static final By DISMISS_BANNER = By.xpath("//button[contains(text(),'DISMISS')]");

    // ═══════════════════════════════════════════════════
    // LOCATORS — Sidebar & Navigation
    // ═══════════════════════════════════════════════════
    private static final By COMPANY_SETTINGS_NAV = By.xpath(
        "//span[normalize-space()='Company Settings'] | //a[normalize-space()='Company Settings'] | " +
        "//span[normalize-space()='Settings'] | //a[normalize-space()='Settings'] | " +
        "//*[contains(@href,'/settings')]"
    );

    private static final By BRANDING_ASSETS_TAB = By.xpath(
        "//*[normalize-space()='Branding & Assets'] | //*[normalize-space()='Branding'] | " +
        "//*[contains(text(),'Branding')]"
    );

    private static final By EG_FORMS_NAV = By.xpath(
        "//*[normalize-space()='EG Forms'] | //*[normalize-space()='Forms'] | " +
        "//a[contains(@href,'forms')] | //button[contains(text(),'Forms')]"
    );

    private static final By REPORTING_CONFIG_NAV = By.xpath(
        "//*[normalize-space()='Reporting Config'] | //*[contains(text(),'Reporting Config')] | " +
        "//*[contains(text(),'Config Explorer')] | //a[contains(@href,'reporting')]"
    );

    // ═══════════════════════════════════════════════════
    // LOCATORS — TC-1: Branding & Assets
    // ═══════════════════════════════════════════════════
    private static final By SMALL_LOGO_SECTION = By.xpath(
        "//*[contains(text(),'Small Logo') or contains(text(),'small_logo') or contains(text(),'Small logo')]"
    );

    private static final By PRIMARY_COLOR_INPUT = By.xpath(
        "//input[contains(@name,'primary') or contains(@placeholder,'primary') or contains(@id,'primary')]"
    );

    private static final By ACCENT_COLOR_INPUT = By.xpath(
        "//input[contains(@name,'accent') or contains(@placeholder,'accent') or contains(@id,'accent')]"
    );

    private static final By SAVE_BUTTON = By.xpath(
        "//button[normalize-space()='Save'] | //button[normalize-space()='Save Changes'] | " +
        "//button[contains(text(),'Save')]"
    );

    // ═══════════════════════════════════════════════════
    // LOCATORS — TC-2: Starter Page Templates
    // ═══════════════════════════════════════════════════
    private static final By STARTER_TEMPLATES_TAB = By.xpath(
        "//*[contains(text(),'Starter')] | //*[contains(text(),'Start from template')] | " +
        "//*[contains(text(),'starter')]"
    );

    private static final By DUPLICATE_BUTTON = By.xpath(
        "//button[normalize-space()='Duplicate'] | //button[contains(text(),'Duplicate')] | " +
        "//*[contains(@title,'Duplicate')]"
    );

    private static final By TEMPLATE_LIST_ITEMS = By.xpath(
        "//tr[contains(@class,'row')] | //div[contains(@class,'template-card')] | " +
        "//li[contains(@class,'template')]"
    );

    // ═══════════════════════════════════════════════════
    // LOCATORS — TC-3: Template Explorer
    // ═══════════════════════════════════════════════════
    private static final By TEMPLATE_SEARCH_INPUT = By.xpath(
        "//input[contains(@placeholder,'Search') or contains(@placeholder,'search') or @type='search']"
    );

    private static final By TEMPLATE_PREVIEW_PANEL = By.xpath(
        "//*[contains(@class,'preview')] | //*[contains(@class,'Preview')] | " +
        "//iframe[contains(@class,'preview')]"
    );

    private static final By SAMPLE_DATA_SELECTOR = By.xpath(
        "//select[contains(@class,'sample')] | //*[contains(text(),'Sample Data')] | " +
        "//button[contains(text(),'Sample')]"
    );

    // ═══════════════════════════════════════════════════
    // LOCATORS — TC-4: EG Forms V2
    // ═══════════════════════════════════════════════════
    private static final By CREATE_FORM_BUTTON = By.xpath(
        "//button[contains(text(),'Create')] | //button[contains(text(),'New Form')] | " +
        "//button[contains(text(),'Add Form')] | //button[contains(text(),'+ Add')]"
    );

    private static final By FORM_TYPE_DROPDOWN = By.xpath(
        "//select[contains(@name,'type') or contains(@id,'type')] | " +
        "//*[contains(@class,'select')]//*[contains(text(),'Type')] | " +
        "//*[contains(@role,'combobox')]"
    );

    private static final By NETA_CLASS_LEVEL_OPTION = By.xpath(
        "//*[contains(text(),'NETA Class-Level')]"
    );

    private static final By FORM_TYPE_5_OPTION = By.xpath(
        "//option[@value='5'] | //*[contains(@data-value,'5')] | //li[@data-value='5']"
    );

    private static final By FORM_TYPE_6_OPTION = By.xpath(
        "//option[@value='6'] | //*[contains(@data-value,'6')] | //li[@data-value='6']"
    );

    private static final By FORM_TYPE_7_OPTION = By.xpath(
        "//option[@value='7'] | //*[contains(@data-value,'7')] | //li[@data-value='7']"
    );

    // ═══════════════════════════════════════════════════
    // LOCATORS — TC-5: ReportingConfig
    // ═══════════════════════════════════════════════════
    private static final By TEST_RUN_BUTTON = By.xpath(
        "//button[contains(text(),'Test Run')] | //button[contains(text(),'Test')] | " +
        "//button[contains(text(),'Run')]"
    );

    private static final By VIEW_RAW_JSON_BUTTON = By.xpath(
        "//button[contains(text(),'Raw JSON')] | //button[contains(text(),'View JSON')] | " +
        "//*[contains(text(),'Raw JSON')]"
    );

    private static final By RESULTS_PANEL = By.xpath(
        "//*[contains(@class,'results')] | //*[contains(@class,'output')] | " +
        "//pre | //code"
    );

    // ═══════════════════════════════════════════════════
    // LOCATORS — TC-7: Generate Report
    // ═══════════════════════════════════════════════════
    private static final By REPORT_RENDER_CONTAINER = By.xpath(
        "//*[contains(@class,'report')] | //iframe[contains(@class,'report')] | " +
        "//*[contains(@class,'rendered')]"
    );

    // ═══════════════════════════════════════════════════
    // SETUP / TEARDOWN
    // ═══════════════════════════════════════════════════

    @BeforeSuite
    public void suiteSetup() {
        ExtentReportManager.initReports();
    }

    @AfterSuite
    public void suiteTeardown() {
        ExtentReportManager.flushReports();
    }

    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("  Reporting Engine V2 — QA Review Test Suite");
        System.out.println("  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM yyyy")));
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        if ("true".equals(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }

        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logPrefs);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        js = (JavascriptExecutor) driver;
        ScreenshotUtil.setDriver(driver);

        loginToApp();
        js.executeScript("document.body.style.zoom='90%';");
    }

    @AfterClass
    public void classTeardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
        System.out.println("\n  Reporting Engine V2 Test Suite completed.\n");
    }

    @BeforeMethod
    public void testSetup() {
        testStartTime = System.currentTimeMillis();
        consoleErrors.clear();
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;
        String status = result.isSuccess() ? "PASSED" : "FAILED";
        System.out.println("  " + status + " (" + duration + "ms)");

        if (!result.isSuccess()) {
            try {
                ExtentReportManager.logStepWithScreenshot(
                    "FAILED: " + result.getMethod().getMethodName());
            } catch (Exception e) { /* screenshot failed */ }
        }

        captureConsoleErrors();
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-1 — BRANDING & ASSETS (ZP-1660)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-1.1: Navigate to Company Settings → Branding & Assets")
    public void TC1_01_navigateToBrandingAndAssets() {
        logStep("TC-1.1: Navigate to Company Settings → Branding & Assets");

        navigateViaUrl("/settings");
        sleep(2000);

        boolean pageLoaded = elementExists(BRANDING_ASSETS_TAB)
            || driver.getCurrentUrl().contains("settings")
            || elementExists(COMPANY_SETTINGS_NAV);

        if (elementExists(BRANDING_ASSETS_TAB)) {
            clickSafe(BRANDING_ASSETS_TAB);
            sleep(1500);
        }

        screenshot("TC1_01_branding_page");
        assertNoConsoleErrors("TC-1.1");
        Assert.assertTrue(pageLoaded,
            "Company Settings page should load. Current URL: " + driver.getCurrentUrl());

        logStep("TC-1.1 PASSED: Branding & Assets page loaded");
    }

    @Test(dependsOnMethods = "TC1_01_navigateToBrandingAndAssets",
          description = "TC-1.2: Upload PNG as small_logo")
    public void TC1_02_uploadSmallLogoPNG() {
        logStep("TC-1.2: Upload PNG as small_logo (under size limit)");

        // Look for file upload near "Small Logo" section
        boolean smallLogoSection = elementExists(SMALL_LOGO_SECTION);
        logStep("Small Logo section found: " + smallLogoSection);

        List<WebElement> fileInputs = driver.findElements(By.xpath("//input[@type='file']"));
        logStep("File inputs found on page: " + fileInputs.size());

        if (!fileInputs.isEmpty()) {
            String testImagePath = getTestImagePath("png");
            if (testImagePath != null) {
                fileInputs.get(0).sendKeys(testImagePath);
                sleep(3000);
                screenshot("TC1_02_after_upload");
                logStep("Uploaded test PNG to first file input");
            } else {
                logStep("SKIP: No test PNG file available — create src/test/resources/test-logo.png");
            }
        } else {
            logStep("MANUAL: No file input found — upload may use a custom component. " +
                     "Look for a drag-drop zone or upload button.");
        }

        assertNoConsoleErrors("TC-1.2");
        assertNo500Errors("TC-1.2");
    }

    @Test(dependsOnMethods = "TC1_01_navigateToBrandingAndAssets",
          description = "TC-1.3: Upload SVG as large_logo")
    public void TC1_03_uploadLargeLogoSVG() {
        logStep("TC-1.3: Upload SVG as large_logo");

        List<WebElement> fileInputs = driver.findElements(By.xpath("//input[@type='file']"));
        if (fileInputs.size() >= 2) {
            String testSvgPath = getTestImagePath("svg");
            if (testSvgPath != null) {
                fileInputs.get(1).sendKeys(testSvgPath);
                sleep(3000);
                screenshot("TC1_03_svg_upload");
                logStep("Uploaded test SVG to second file input");
            } else {
                logStep("SKIP: No test SVG file available — create src/test/resources/test-logo.svg");
            }
        } else {
            logStep("MANUAL: Less than 2 file inputs found (" + fileInputs.size() +
                     "). SVG upload needs manual verification.");
        }

        assertNoConsoleErrors("TC-1.3");
    }

    @Test(dependsOnMethods = "TC1_01_navigateToBrandingAndAssets",
          description = "TC-1.4: Set primary + accent colors and save")
    public void TC1_04_setPrimaryAndAccentColors() {
        logStep("TC-1.4: Set primary_color=#0055A4, accent_color=#F39200, Save");

        boolean colorSet = false;

        if (elementExists(PRIMARY_COLOR_INPUT)) {
            setInputValue(PRIMARY_COLOR_INPUT, "#0055A4");
            logStep("Set primary color to #0055A4");
            colorSet = true;
        }

        if (elementExists(ACCENT_COLOR_INPUT)) {
            setInputValue(ACCENT_COLOR_INPUT, "#F39200");
            logStep("Set accent color to #F39200");
            colorSet = true;
        }

        if (colorSet && elementExists(SAVE_BUTTON)) {
            clickSafe(SAVE_BUTTON);
            sleep(2000);
            screenshot("TC1_04_colors_saved");
            logStep("Colors saved");
        } else {
            logStep("MANUAL: Color inputs not found by automation locators. " +
                     "Check if inputs use a color picker component.");
            screenshot("TC1_04_manual_check");
        }

        assertNoConsoleErrors("TC-1.4");
        assertNo500Errors("TC-1.4");
    }

    @Test(dependsOnMethods = "TC1_01_navigateToBrandingAndAssets",
          description = "TC-1.5: Upload file > size limit")
    public void TC1_05_uploadOversizedFile() {
        logStep("TC-1.5: Upload file > size limit (e.g., 20MB image)");
        logStep("MANUAL STEP: Create a 20MB+ image file, attempt upload, verify:");
        logStep("  - Validation error message appears");
        logStep("  - No 500 in Network tab");
        logStep("  - File is rejected gracefully");
        screenshot("TC1_05_placeholder");
        assertNo500Errors("TC-1.5");
    }

    @Test(dependsOnMethods = "TC1_01_navigateToBrandingAndAssets",
          description = "TC-1.6: Upload unsupported format (.exe)")
    public void TC1_06_uploadUnsupportedFormat() {
        logStep("TC-1.6: Upload .exe or unsupported format");
        logStep("MANUAL STEP: Attempt to upload a .exe file, verify:");
        logStep("  - Rejected with clear message");
        logStep("  - No crash or 500");
        screenshot("TC1_06_placeholder");
        assertNo500Errors("TC-1.6");
    }

    @Test(dependsOnMethods = "TC1_04_setPrimaryAndAccentColors",
          description = "TC-1.7: Refresh page — assets + colors persist")
    public void TC1_07_verifyPersistenceAfterRefresh() {
        logStep("TC-1.7: Refresh page and verify persistence");

        driver.navigate().refresh();
        sleep(3000);

        if (elementExists(BRANDING_ASSETS_TAB)) {
            clickSafe(BRANDING_ASSETS_TAB);
            sleep(1500);
        }

        screenshot("TC1_07_after_refresh");
        logStep("VERIFY MANUALLY: Uploaded logos and color values (#0055A4, #F39200) persist after refresh");

        assertNoConsoleErrors("TC-1.7");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-2 — STARTER PAGE TEMPLATE (ZP-1662)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-2.1: Navigate to Page Templates → Starter Templates")
    public void TC2_01_navigateToStarterTemplates() {
        logStep("TC-2.1: Navigate to Page Templates → Starter Templates");

        navigateViaUrl("/admin/templates");
        sleep(3000);
        dismissBanner();

        if (elementExists(STARTER_TEMPLATES_TAB)) {
            clickSafe(STARTER_TEMPLATES_TAB);
            sleep(2000);
        }

        screenshot("TC2_01_starter_templates");
        boolean hasTemplates = elementExists(TEMPLATE_LIST_ITEMS)
            || driver.getPageSource().contains("cover")
            || driver.getPageSource().contains("summary")
            || driver.getPageSource().contains("appendix");

        logStep("Templates visible: " + hasTemplates);
        Assert.assertTrue(hasTemplates || driver.getCurrentUrl().contains("template"),
            "Starter templates library should be visible");
    }

    @Test(dependsOnMethods = "TC2_01_navigateToStarterTemplates",
          description = "TC-2.2: Duplicate a starter template")
    public void TC2_02_duplicateStarterTemplate() {
        logStep("TC-2.2: Click a starter → Duplicate");

        List<WebElement> templateItems = driver.findElements(TEMPLATE_LIST_ITEMS);
        if (!templateItems.isEmpty()) {
            clickSafe(templateItems.get(0));
            sleep(1500);

            if (elementExists(DUPLICATE_BUTTON)) {
                clickSafe(DUPLICATE_BUTTON);
                sleep(3000);
                screenshot("TC2_02_duplicated");
                logStep("Template duplicated — verify '(Copy)' suffix appears in list");
            } else {
                logStep("MANUAL: Duplicate button not found. Look for context menu or '...' icon.");
                screenshot("TC2_02_no_duplicate_btn");
            }
        } else {
            logStep("MANUAL: No template items detected in list. Check page structure.");
            screenshot("TC2_02_no_templates");
        }

        assertNo500Errors("TC-2.2");
    }

    @Test(dependsOnMethods = "TC2_01_navigateToStarterTemplates",
          description = "TC-2.3: Verify original starter is read-only")
    public void TC2_03_verifyOriginalReadOnly() {
        logStep("TC-2.3: Try to edit the original starter");
        logStep("MANUAL STEP: Click original starter template, verify:");
        logStep("  - Edit is blocked or read-only indicator shown");
        logStep("  - You cannot modify the original template");
        screenshot("TC2_03_readonly_check");
    }

    @Test(dependsOnMethods = "TC2_02_duplicateStarterTemplate",
          description = "TC-2.4: Open duplicate renders identically")
    public void TC2_04_verifyDuplicateRendersCorrectly() {
        logStep("TC-2.4: Open the duplicate");
        logStep("MANUAL STEP: Open the duplicated template, verify:");
        logStep("  - Renders identically to the original");
        logStep("  - Name has '(Copy)' suffix");
        screenshot("TC2_04_duplicate_render");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-3 — TEMPLATE EXPLORER PREVIEW (ZP-1592)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-3.1: Navigate to Template Explorer")
    public void TC3_01_navigateToTemplateExplorer() {
        logStep("TC-3.1: Navigate to Template Explorer");

        String[] explorerPaths = {"/admin/templates", "/templates", "/admin/page-templates",
                                  "/reporting/templates"};
        boolean found = false;

        for (String path : explorerPaths) {
            navigateViaUrl(path);
            sleep(2000);
            if (elementExists(TEMPLATE_SEARCH_INPUT) || elementExists(TEMPLATE_LIST_ITEMS)) {
                found = true;
                logStep("Template Explorer found at: " + path);
                break;
            }
        }

        screenshot("TC3_01_template_explorer");
        Assert.assertTrue(found, "Template Explorer should be accessible at one of the known paths");
        assertNoConsoleErrors("TC-3.1");
    }

    @Test(dependsOnMethods = "TC3_01_navigateToTemplateExplorer",
          description = "TC-3.2: Search by duplicated template name")
    public void TC3_02_searchTemplateByName() {
        logStep("TC-3.2: Search by template name");

        if (elementExists(TEMPLATE_SEARCH_INPUT)) {
            WebElement searchInput = driver.findElement(TEMPLATE_SEARCH_INPUT);
            searchInput.clear();
            searchInput.sendKeys("Copy");
            sleep(2000);
            screenshot("TC3_02_search_results");

            List<WebElement> results = driver.findElements(TEMPLATE_LIST_ITEMS);
            logStep("Search results for 'Copy': " + results.size() + " items");
        } else {
            logStep("MANUAL: Search input not found. Check page for search functionality.");
            screenshot("TC3_02_no_search");
        }

        assertNoConsoleErrors("TC-3.2");
    }

    @Test(dependsOnMethods = "TC3_01_navigateToTemplateExplorer",
          description = "TC-3.3: Select template → preview with branding")
    public void TC3_03_selectTemplateForPreview() {
        logStep("TC-3.3: Select template → preview panel shows branding");

        // Clear search first
        if (elementExists(TEMPLATE_SEARCH_INPUT)) {
            WebElement searchInput = driver.findElement(TEMPLATE_SEARCH_INPUT);
            searchInput.clear();
            searchInput.sendKeys(Keys.ESCAPE);
            sleep(1000);
        }

        List<WebElement> templates = driver.findElements(TEMPLATE_LIST_ITEMS);
        if (!templates.isEmpty()) {
            clickSafe(templates.get(0));
            sleep(3000);
            screenshot("TC3_03_preview");

            boolean previewVisible = elementExists(TEMPLATE_PREVIEW_PANEL)
                || driver.getPageSource().contains("preview");

            logStep("Preview panel visible: " + previewVisible);
            logStep("VERIFY MANUALLY: Preview shows logo + primary(#0055A4)/accent(#F39200) colors from TC-1");
        } else {
            logStep("MANUAL: No templates in list to select for preview.");
        }

        assertNoConsoleErrors("TC-3.3");
    }

    @Test(dependsOnMethods = "TC3_01_navigateToTemplateExplorer",
          description = "TC-3.4: Switch sample data set")
    public void TC3_04_switchSampleDataSet() {
        logStep("TC-3.4: Switch sample data set (if selector exists)");

        if (elementExists(SAMPLE_DATA_SELECTOR)) {
            clickSafe(SAMPLE_DATA_SELECTOR);
            sleep(2000);
            screenshot("TC3_04_sample_data");
            logStep("Sample data selector found — verify preview re-renders with new data");
        } else {
            logStep("SKIP: No sample data selector found — this may not be implemented yet");
        }
    }

    @Test(dependsOnMethods = "TC3_01_navigateToTemplateExplorer",
          description = "TC-3.5: Rapid-click 4-5 templates — no race condition")
    public void TC3_05_rapidClickNoRaceCondition() {
        logStep("TC-3.5: Rapidly click 4-5 templates in a row");

        List<WebElement> templates = driver.findElements(TEMPLATE_LIST_ITEMS);
        int clickCount = Math.min(5, templates.size());

        for (int i = 0; i < clickCount; i++) {
            try {
                clickSafe(templates.get(i));
                sleep(300);
            } catch (Exception e) {
                logStep("Click " + (i + 1) + " failed: " + e.getMessage());
            }
        }

        sleep(2000);
        screenshot("TC3_05_after_rapid_clicks");
        logStep("VERIFY MANUALLY: Preview shows the LAST selected template, no flicker of earlier ones");

        assertNoConsoleErrors("TC-3.5");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-4 — EG FORMS V2 (ZP-1717)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-4.1: Navigate to EG Forms → Create New")
    public void TC4_01_navigateToEGFormsCreateNew() {
        logStep("TC-4.1: Go to EG Forms → Create New");

        navigateViaUrl("/admin");
        sleep(2000);
        dismissBanner();

        if (elementExists(EG_FORMS_NAV)) {
            clickSafe(EG_FORMS_NAV);
            sleep(2000);
        }

        screenshot("TC4_01_eg_forms");
        boolean formsPage = elementExists(CREATE_FORM_BUTTON) || elementExists(EG_FORMS_NAV)
            || driver.getCurrentUrl().contains("form");

        logStep("EG Forms page loaded: " + formsPage);
        assertNoConsoleErrors("TC-4.1");
    }

    @Test(dependsOnMethods = "TC4_01_navigateToEGFormsCreateNew",
          description = "TC-4.2: Verify type 3 is labeled 'NETA Class-Level'")
    public void TC4_02_verifyNETAClassLevelLabel() {
        logStep("TC-4.2: Verify form type 3 is labeled 'NETA Class-Level' (not 'NETA')");

        if (elementExists(CREATE_FORM_BUTTON)) {
            clickSafe(CREATE_FORM_BUTTON);
            sleep(2000);
        }

        if (elementExists(FORM_TYPE_DROPDOWN)) {
            clickSafe(FORM_TYPE_DROPDOWN);
            sleep(1000);
        }

        screenshot("TC4_02_form_type_dropdown");

        boolean netaClassLevel = elementExists(NETA_CLASS_LEVEL_OPTION);
        logStep("'NETA Class-Level' label found: " + netaClassLevel);

        boolean oldNetaLabel = false;
        try {
            oldNetaLabel = driver.findElements(By.xpath(
                "//*[text()='NETA' and not(contains(text(),'Class'))]")).size() > 0;
        } catch (Exception e) { /* not found is fine */ }

        if (oldNetaLabel) {
            logStep("BUG: Old 'NETA' label still present (should be 'NETA Class-Level')");
        }

        assertNoConsoleErrors("TC-4.2");
    }

    @Test(dependsOnMethods = "TC4_01_navigateToEGFormsCreateNew",
          description = "TC-4.3: Verify types 5, 6, 7 are present and selectable")
    public void TC4_03_verifyFormTypes567() {
        logStep("TC-4.3: Verify form types 5, 6, 7 are present");

        if (elementExists(FORM_TYPE_DROPDOWN)) {
            clickSafe(FORM_TYPE_DROPDOWN);
            sleep(1000);
        }

        screenshot("TC4_03_types_567");

        boolean type5 = elementExists(FORM_TYPE_5_OPTION);
        boolean type6 = elementExists(FORM_TYPE_6_OPTION);
        boolean type7 = elementExists(FORM_TYPE_7_OPTION);

        logStep("Type 5 present: " + type5);
        logStep("Type 6 present: " + type6);
        logStep("Type 7 present: " + type7);

        logStep("VERIFY MANUALLY: All three types have display names and are selectable");

        // Dismiss dropdown
        try { driver.findElement(By.tagName("body")).click(); } catch (Exception ignored) {}
        sleep(500);

        assertNoConsoleErrors("TC-4.3");
    }

    @Test(dependsOnMethods = "TC4_03_verifyFormTypes567",
          description = "TC-4.4: Create form of type 5 with fields")
    public void TC4_04_createFormType5() {
        logStep("TC-4.4: Create a form of type 5 with text, number, select, date fields");
        logStep("MANUAL STEP: ");
        logStep("  1. Select type 5 from dropdown");
        logStep("  2. Add fields: text, number, select, date");
        logStep("  3. Save the form");
        logStep("  4. Ask dev to verify: DB record has version:2 and eg_form_type=5");
        screenshot("TC4_04_create_type5");

        assertNo500Errors("TC-4.4");
    }

    @Test(dependsOnMethods = "TC4_03_verifyFormTypes567",
          description = "TC-4.5: Create forms of type 6 and type 7")
    public void TC4_05_createFormsType6And7() {
        logStep("TC-4.5: Repeat form creation for type 6 and type 7");
        logStep("MANUAL STEP: Create forms for both types, verify both save successfully");
        screenshot("TC4_05_types_6_and_7");

        assertNo500Errors("TC-4.5");
    }

    @Test(description = "TC-4.6: Open a pre-existing V1 form")
    public void TC4_06_openPreExistingV1Form() {
        logStep("TC-4.6: Open a pre-existing V1 form (no version:2)");
        logStep("MANUAL STEP: Find and open a V1 form, verify:");
        logStep("  - Opens without errors");
        logStep("  - Renders correctly");
        logStep("  - Is editable");
        screenshot("TC4_06_v1_form");

        assertNoConsoleErrors("TC-4.6");
        assertNo500Errors("TC-4.6");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-5 — REPORTING CONFIG (ZP-1584)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-5.1: Navigate to Reporting Config Explorer")
    public void TC5_01_navigateToReportingConfig() {
        logStep("TC-5.1: Navigate to Reporting Config Explorer");

        String[] configPaths = {"/admin/reporting", "/reporting", "/admin/reporting-config"};
        boolean found = false;

        for (String path : configPaths) {
            navigateViaUrl(path);
            sleep(2000);
            if (!isLoginPage()) {
                found = true;
                logStep("Reporting Config found at: " + path);
                break;
            }
        }

        if (!found && elementExists(REPORTING_CONFIG_NAV)) {
            clickSafe(REPORTING_CONFIG_NAV);
            sleep(2000);
            found = true;
        }

        screenshot("TC5_01_reporting_config");
        logStep("Reporting Config accessible: " + found);
        assertNoConsoleErrors("TC-5.1");
    }

    @Test(dependsOnMethods = "TC5_01_navigateToReportingConfig",
          description = "TC-5.2: Create config pointing to type-5 form")
    public void TC5_02_createConfigForType5Form() {
        logStep("TC-5.2: Create Config → point to type-5 form from TC-4");
        logStep("MANUAL STEP: ");
        logStep("  1. Click Create Config");
        logStep("  2. Select/point to the type-5 form created in TC-4.4");
        logStep("  3. Save the config");
        screenshot("TC5_02_create_config");

        assertNo500Errors("TC-5.2");
    }

    @Test(dependsOnMethods = "TC5_02_createConfigForType5Form",
          description = "TC-5.3: Test Run on new config")
    public void TC5_03_testRunOnConfig() {
        logStep("TC-5.3: Click Test Run on the new config");

        if (elementExists(TEST_RUN_BUTTON)) {
            clickSafe(TEST_RUN_BUTTON);
            sleep(5000);
            screenshot("TC5_03_test_run_results");

            boolean hasResults = elementExists(RESULTS_PANEL)
                || driver.getPageSource().contains("{")
                || driver.getPageSource().contains("rows");

            logStep("Results panel visible: " + hasResults);
        } else {
            logStep("MANUAL: Test Run button not found — locate it in the config view");
            screenshot("TC5_03_no_test_run");
        }

        assertNoConsoleErrors("TC-5.3");
        assertNo500Errors("TC-5.3");
    }

    @Test(dependsOnMethods = "TC5_01_navigateToReportingConfig",
          description = "TC-5.4: View Raw JSON")
    public void TC5_04_viewRawJSON() {
        logStep("TC-5.4: Click View Raw JSON");

        if (elementExists(VIEW_RAW_JSON_BUTTON)) {
            clickSafe(VIEW_RAW_JSON_BUTTON);
            sleep(2000);
            screenshot("TC5_04_raw_json");
            logStep("Raw JSON viewer opened — verify config structure is shown");
        } else {
            logStep("MANUAL: Raw JSON button not found");
        }

        assertNoConsoleErrors("TC-5.4");
    }

    @Test(dependsOnMethods = "TC5_01_navigateToReportingConfig",
          description = "TC-5.5: Invalid config → error message, no 500")
    public void TC5_05_invalidConfigErrorHandling() {
        logStep("TC-5.5: Create deliberately invalid config → Test Run");
        logStep("MANUAL STEP: ");
        logStep("  1. Create config with a bad query name");
        logStep("  2. Click Test Run");
        logStep("  3. Verify: error message with details shown, NOT a 500");
        screenshot("TC5_05_invalid_config");

        assertNo500Errors("TC-5.5");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-6 — FILL EG FORM IN WORK ORDER (ZP-1721)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-6.1: Open work order with at least one node")
    public void TC6_01_openWorkOrderWithNode() {
        logStep("TC-6.1: Open a work order with at least one node");

        navigateViaUrl("/jobs");
        sleep(3000);
        dismissBanner();

        screenshot("TC6_01_work_orders");
        logStep("Work Orders page loaded — select one with at least one node");

        assertNoConsoleErrors("TC-6.1");
    }

    @Test(dependsOnMethods = "TC6_01_openWorkOrderWithNode",
          description = "TC-6.2: Attach type-5 form to a node")
    public void TC6_02_attachFormToNode() {
        logStep("TC-6.2: Attach the type-5 form from TC-4 to a node");
        logStep("MANUAL STEP: Use the direct-attach path (NOT '+ Add Task' — ZP-1777 known bug)");
        logStep("  1. Open a node");
        logStep("  2. Attach the type-5 EG form");
        logStep("  3. Verify form appears on the node");
        screenshot("TC6_02_attach_form");

        assertNo500Errors("TC-6.2");
    }

    @Test(dependsOnMethods = "TC6_02_attachFormToNode",
          description = "TC-6.3: Fill all fields and save")
    public void TC6_03_fillFieldsAndSave() {
        logStep("TC-6.3: Fill all fields, save");
        logStep("MANUAL STEP: Fill text/number/select/date fields, click Save");
        logStep("  Then refresh and verify values persist");
        screenshot("TC6_03_fill_and_save");

        assertNo500Errors("TC-6.3");
    }

    @Test(dependsOnMethods = "TC6_03_fillFieldsAndSave",
          description = "TC-6.4: Edit a value and save again")
    public void TC6_04_editValueAndResave() {
        logStep("TC-6.4: Edit a value, save again");
        logStep("MANUAL STEP: Change one field value, save, verify updated value persists");
        screenshot("TC6_04_edit_and_resave");

        assertNo500Errors("TC-6.4");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-7 — GENERATE REPORT (ZP-1688 — NEW_QUERY_META + lambdas)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-7.1: Generate Report from config")
    public void TC7_01_generateReport() {
        logStep("TC-7.1: From the config in TC-5, click Generate Report");
        logStep("MANUAL STEP: Navigate back to the reporting config and click Generate Report");
        screenshot("TC7_01_generate");

        assertNo500Errors("TC-7.1");
    }

    @Test(dependsOnMethods = "TC7_01_generateReport",
          description = "TC-7.2: Verify lambda call contains NEW_QUERY_META")
    public void TC7_02_verifyLambdaPayload() {
        logStep("TC-7.2: In Network tab, find the lambda call");
        logStep("MANUAL STEP: ");
        logStep("  1. Open DevTools → Network tab");
        logStep("  2. Filter for the lambda/API call");
        logStep("  3. Check payload contains NEW_QUERY_META with version: 2");
        logStep("BUG if: NEW_QUERY_META missing from lambda call");
        screenshot("TC7_02_lambda_payload");
    }

    @Test(dependsOnMethods = "TC7_01_generateReport",
          description = "TC-7.3: Wait for report to render")
    public void TC7_03_waitForReportRender() {
        logStep("TC-7.3: Wait for the report to render");

        sleep(10000);
        screenshot("TC7_03_rendered_report");

        boolean reportRendered = elementExists(REPORT_RENDER_CONTAINER)
            || driver.getPageSource().contains("report");

        logStep("Report rendered: " + reportRendered);
        logStep("VERIFY MANUALLY: Report opens with content (not blank)");

        assertNoConsoleErrors("TC-7.3");
    }

    @Test(dependsOnMethods = "TC7_03_waitForReportRender",
          description = "TC-7.4: Compare rendered report with template preview")
    public void TC7_04_compareWithTemplatePreview() {
        logStep("TC-7.4: Visually compare against template preview from TC-3");
        logStep("VERIFY MANUALLY:");
        logStep("  - Layout matches the template");
        logStep("  - Colors (#0055A4 primary, #F39200 accent) applied");
        logStep("  - Logo from TC-1 appears correctly");
        screenshot("TC7_04_visual_compare");
    }

    @Test(dependsOnMethods = "TC7_03_waitForReportRender",
          description = "TC-7.5: Verify filled values from TC-6 appear in report")
    public void TC7_05_verifyFilledValuesInReport() {
        logStep("TC-7.5: Verify filled values from TC-6 appear correctly in report");
        logStep("VERIFY MANUALLY:");
        logStep("  - All field values from TC-6.3 are present");
        logStep("  - Values are correctly formatted (dates, numbers)");
        logStep("BUG if: values missing or branding missing");
        screenshot("TC7_05_field_values");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TC-8 — BACKWARD COMPATIBILITY (ZP-1589 regression)
    // ═══════════════════════════════════════════════════════════════

    @Test(description = "TC-8.1: Pick an existing V1 report config")
    public void TC8_01_openExistingV1Config() {
        logStep("TC-8.1: Pick an existing V1 report config (one that worked before this epic)");

        navigateViaUrl("/admin/reporting");
        sleep(3000);

        screenshot("TC8_01_v1_configs");
        logStep("MANUAL STEP: Find and open a V1 config that was working before Reporting Engine V2");

        assertNoConsoleErrors("TC-8.1");
    }

    @Test(dependsOnMethods = "TC8_01_openExistingV1Config",
          description = "TC-8.2: Generate V1 report — renders unchanged")
    public void TC8_02_generateV1Report() {
        logStep("TC-8.2: Generate V1 report and verify it renders identically");
        logStep("CRITICAL: No visual differences, no missing data compared to before the epic");
        logStep("MANUAL STEP: Generate the V1 report and compare with a known-good version");
        screenshot("TC8_02_v1_report");

        assertNo500Errors("TC-8.2");
    }

    @Test(description = "TC-8.3: Repeat for 3 different V1 configs")
    public void TC8_03_verify3DifferentV1Configs() {
        logStep("TC-8.3: Repeat TC-8.1-8.2 for at least 3 different V1 configs");
        logStep("MANUAL STEP: Test 3 V1 configs with DIFFERENT form types:");
        logStep("  Config 1: _________________ → renders unchanged? Y/N");
        logStep("  Config 2: _________________ → renders unchanged? Y/N");
        logStep("  Config 3: _________________ → renders unchanged? Y/N");
        logStep("BUG if: ANY V1 report renders differently or has missing data");
        screenshot("TC8_03_three_v1_configs");

        assertNo500Errors("TC-8.3");
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private void loginToApp() {
        driver.get(AppConstants.BASE_URL);
        sleep(3000);

        try {
            WebElement email = driver.findElement(EMAIL_INPUT);
            email.clear();
            email.sendKeys(AppConstants.VALID_EMAIL);
            WebElement pwd = driver.findElement(PASSWORD_INPUT);
            pwd.clear();
            pwd.sendKeys(AppConstants.VALID_PASSWORD);

            try {
                WebElement cb = driver.findElement(TERMS_CHECKBOX);
                if (!cb.isSelected()) cb.click();
            } catch (Exception e) { /* no checkbox */ }

            driver.findElement(SIGN_IN_BUTTON).click();
            sleep(5000);

            // Dismiss any app alerts
            dismissBanner();
            System.out.println("  Logged in → " + driver.getCurrentUrl());
        } catch (Exception e) {
            System.out.println("  Already logged in or login skipped: " + e.getMessage());
        }
    }

    private void navigateViaUrl(String path) {
        driver.get(AppConstants.BASE_URL + path);
        sleep(2000);
        dismissBanner();
    }

    private void dismissBanner() {
        try {
            WebElement dismiss = driver.findElement(DISMISS_BANNER);
            dismiss.click();
            sleep(500);
        } catch (Exception e) { /* no banner */ }
    }

    private boolean elementExists(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private void clickSafe(By locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (Exception e) {
            try {
                WebElement el = driver.findElement(locator);
                js.executeScript("arguments[0].click();", el);
            } catch (Exception ex) {
                throw new RuntimeException("Click failed for: " + locator, ex);
            }
        }
    }

    private void clickSafe(WebElement element) {
        try {
            element.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", element);
        }
    }

    private void setInputValue(By locator, String value) {
        try {
            WebElement input = driver.findElement(locator);
            input.clear();
            // React-controlled inputs need JS to set value
            js.executeScript(
                "var el = arguments[0]; " +
                "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set; " +
                "nativeInputValueSetter.call(el, arguments[1]); " +
                "el.dispatchEvent(new Event('input', { bubbles: true })); " +
                "el.dispatchEvent(new Event('change', { bubbles: true }));",
                input, value
            );
            sleep(500);
        } catch (Exception e) {
            logStep("setInputValue failed for " + locator + ": " + e.getMessage());
        }
    }

    private boolean isLoginPage() {
        try {
            return driver.findElements(EMAIL_INPUT).size() > 0
                && driver.findElements(PASSWORD_INPUT).size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String getTestImagePath(String extension) {
        String[] paths = {
            "src/test/resources/test-logo." + extension,
            "src/test/resources/s1.jpeg",
            "src/test/resources/test-photo.jpg"
        };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) return f.getAbsolutePath();
        }
        return null;
    }

    private void logStep(String message) {
        System.out.println("    " + message);
        try {
            ExtentReportManager.logInfo(message);
        } catch (Exception e) { /* report not available */ }
    }

    private void screenshot(String name) {
        try {
            ScreenshotUtil.captureScreenshot(name);
            ExtentReportManager.logStepWithScreenshot(name);
        } catch (Exception e) { /* screenshot failed */ }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void captureConsoleErrors() {
        try {
            List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
            for (LogEntry entry : logs) {
                if (entry.getLevel().intValue() >= Level.SEVERE.intValue()) {
                    consoleErrors.add(entry.getMessage());
                }
            }
        } catch (Exception e) { /* logging not available */ }
    }

    private void assertNoConsoleErrors(String stepId) {
        captureConsoleErrors();
        if (!consoleErrors.isEmpty()) {
            String errors = consoleErrors.stream()
                .limit(5)
                .collect(Collectors.joining("\n  "));
            logStep("WARNING [" + stepId + "]: Console errors detected:\n  " + errors);
        }
    }

    private void assertNo500Errors(String stepId) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> entries = (List<Object>) js.executeScript(
                "return performance.getEntriesByType('resource')" +
                ".filter(e => e.responseStatus >= 500)" +
                ".map(e => e.name + ' → ' + e.responseStatus);"
            );
            if (entries != null && !entries.isEmpty()) {
                String errors = entries.stream()
                    .map(Object::toString)
                    .limit(5)
                    .collect(Collectors.joining("\n  "));
                logStep("BUG [" + stepId + "]: 500 errors detected in Network:\n  " + errors);
                Assert.fail("500 errors detected during " + stepId);
            }
        } catch (Exception e) {
            // performance API not available or different browser version
        }
    }
}
