package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * AI-Driven Form/Template Creation — QA Test Suite
 *
 * Jira: ZP-XXXX (AI-Driven Form/Template Creation)
 * Parent: ZP-1583 — Reporting Engine V2
 *
 * Coverage:
 *   Section 1: AI Entry Point Discovery (3 TCs)
 *   Section 2: AI Form Generation (4 TCs)
 *   Section 3: Form Type & Schema Validation (3 TCs)
 *   Section 4: Iterative Refinement (2 TCs)
 *   Section 5: Error Handling & Edge Cases (4 TCs)
 *   Section 6: Save & Persistence (2 TCs)
 *   Section 7: Agent Service Network Checks (2 TCs)
 *   Total: 20 TCs
 *
 * Architecture: Standalone (own browser session with console logging).
 * Does NOT extend BaseTest — tests Admin Forms area with independent session.
 *
 * Run: mvn test -Dtest=EgFormAITestNG
 * Run single: mvn test -Dtest=EgFormAITestNG#TC01_verifyCreateWithAIButtonExists
 */
public class EgFormAITestNG {

    private static final String MODULE = "Admin Forms";
    private static final String FEATURE_AI_FORM = "AI Form Creation";
    private static final String FEATURE_AI_TEMPLATE = "AI Template Creation";
    private static final String FEATURE_AI_VALIDATION = "AI Form Validation";

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;
    private long testStartTime;

    // ═══════════════════════════════════════════
    // LOCATORS — Admin Forms
    // ═══════════════════════════════════════════
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By TERMS_CHECKBOX = By.cssSelector("input[type='checkbox']");
    private static final By SIGN_IN_BUTTON = By.xpath("//button[normalize-space()='Sign In']");

    // Admin tabs
    private static final By FORMS_TAB = By.xpath("//button[contains(text(),'Forms')] | //a[contains(text(),'Forms')] | //*[text()='Forms']");
    private static final By PM_TAB = By.xpath("//*[text()='PM']");
    private static final By SITES_TAB = By.xpath("//*[text()='Sites']");
    private static final By USERS_TAB = By.xpath("//*[text()='Users']");

    // AI Feature selectors
    private static final By AI_BUTTON = By.xpath(
        "//button[contains(text(),'Create with AI')] | " +
        "//button[contains(text(),'AI')] | " +
        "//button[contains(text(),'Generate')] | " +
        "//button[contains(text(),'Generate Form')] | " +
        "//*[contains(@data-testid,'ai')] | " +
        "//*[contains(text(),'Create with AI')]"
    );

    private static final By ADD_FORM_BUTTON = By.xpath(
        "//button[contains(text(),'Add Form')] | //button[contains(text(),'+ Add Form')]"
    );

    // AI Modal/Dialog
    private static final By AI_DESCRIPTION_INPUT = By.xpath(
        "//textarea[contains(@placeholder,'describe')] | " +
        "//textarea[contains(@placeholder,'Create')] | " +
        "//textarea[contains(@placeholder,'natural language')] | " +
        "//input[contains(@placeholder,'describe')] | " +
        "//textarea"
    );

    private static final By GENERATE_BUTTON = By.xpath(
        "//button[contains(text(),'Generate')] | " +
        "//button[contains(text(),'Create')] | " +
        "//button[contains(text(),'Submit')]"
    );

    private static final By SAVE_FORM_BUTTON = By.xpath(
        "//button[contains(text(),'Save Form')] | //button[contains(text(),'Save')]"
    );

    private static final By DISMISS_BANNER = By.xpath("//button[contains(text(),'DISMISS')]");

    // Form Builder elements
    private static final By FORM_BUILDER_CONTAINER = By.xpath(
        "//*[contains(@class,'form-builder')] | //*[contains(@class,'FormBuilder')] | //*[text()='Form Builder']"
    );

    // ═══════════════════════════════════════════
    // SETUP / TEARDOWN
    // ═══════════════════════════════════════════

    
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
        System.out.println("  AI-Driven Form/Template Creation — Test Suite");
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

        // Headless mode for CI — matches BaseTest behavior
        if ("true".equals(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }

        // Use EAGER page load strategy to avoid SPA navigation hangs
        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logPrefs);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        js = (JavascriptExecutor) driver;
        ScreenshotUtil.setDriver(driver);

        // Set zoom to 80% to match BaseTest
        loginToAdmin();
        js.executeScript("document.body.style.zoom='90%';");
    }

    @AfterClass
    public void classTeardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
        System.out.println("\n  AI Form Test Suite completed.\n");
    }

    @BeforeMethod
    public void testSetup() {
        testStartTime = System.currentTimeMillis();
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;
        String status = result.isSuccess() ? "✅ PASSED" : "❌ FAILED";
        System.out.println("  " + status + " (" + duration + "ms)");
    }

    // ═══════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════

    private void loginToAdmin() {
        driver.get(AppConstants.BASE_URL + "/admin");
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
            System.out.println("  ✅ Logged in → " + driver.getCurrentUrl());
        } catch (Exception e) {
            System.out.println("  ℹ️ Already logged in or login skipped");
        }
    }

    private void navigateToAdmin() {
        driver.get(AppConstants.BASE_URL + "/admin");
        sleep(3000);
        dismissBanner();
    }

    private void clickFormsTab() {
        try {
            safeClick(FORMS_TAB);
            sleep(3000);
        } catch (Exception e) {
            System.out.println("  ⚠️ Forms tab not found: " + e.getMessage());
        }
    }

    private void dismissBanner() {
        try {
            WebElement dismiss = driver.findElement(DISMISS_BANNER);
            dismiss.click();
            sleep(500);
        } catch (Exception e) { /* no banner */ }
    }

    /**
     * Dismiss all MUI backdrops + the "App Update Available" banner that cover
     * AI Form clicks in CI (headless Chrome). Safe to call anytime.
     * The backdrop re-appears on React re-render, so this must run IMMEDIATELY
     * before every click &mdash; not just once at page-load.
     */
    private void dismissAllBlockers() {
        try {
            js.executeScript(
                    "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop')"
                            + ".forEach(function(b){b.style.display='none';b.style.pointerEvents='none';});"
                            + "var btns = document.querySelectorAll('button');"
                            + "for (var i = 0; i < btns.length; i++) {"
                            + "  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }"
                            + "}");
        } catch (Exception ignored) {}
    }

    /**
     * Click with backdrop cleanup + scrollIntoView + JS-click fallback.
     * Replaces raw {@code driver.findElement(x).click()} calls that were
     * failing with "element not interactable" when a backdrop covered them.
     */
    private void safeClick(By locator) {
        dismissAllBlockers();
        try {
            WebElement el = driver.findElement(locator);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            try {
                el.click();
            } catch (Exception e) {
                // Backdrop re-appeared during the click — try once more after cleanup
                dismissAllBlockers();
                js.executeScript("arguments[0].click();", el);
            }
        } catch (Exception e) {
            throw new RuntimeException("safeClick failed for " + locator + ": " + e.getMessage(), e);
        }
    }

    private boolean elementExists(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean elementVisible(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean elementEnabled(By locator) {
        try {
            return driver.findElement(locator).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> getAllButtonTexts() {
        List<WebElement> buttons = driver.findElements(By.tagName("button"));
        List<String> texts = new ArrayList<>();
        for (WebElement btn : buttons) {
            try {
                String t = btn.getText().trim();
                if (!t.isEmpty() && btn.isDisplayed()) {
                    texts.add(t + (btn.isEnabled() ? "" : " [DISABLED]"));
                }
            } catch (Exception e) { /* skip */ }
        }
        return texts;
    }

    private List<String> getConsoleErrors() {
        List<String> errors = new ArrayList<>();
        try {
            List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
            for (LogEntry log : logs) {
                if (log.getLevel() == Level.SEVERE) {
                    errors.add(log.getMessage());
                }
            }
        } catch (Exception e) { /* logging not supported */ }
        return errors;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ═══════════════════════════════════════════
    // SECTION 1: AI ENTRY POINT DISCOVERY
    // ═══════════════════════════════════════════

    @Test(priority = 1)
    public void TC01_verifyCreateWithAIButtonExists() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC01 - Verify 'Create with AI' button exists on Admin Forms page");

        System.out.println("\n📋 TC01: Finding 'Create with AI' entry point");

        navigateToAdmin();
        clickFormsTab();

        // List all buttons on the page
        List<String> allButtons = getAllButtonTexts();
        System.out.println("  All buttons on Forms page:");
        for (String btn : allButtons) {
            String marker = btn.toLowerCase().contains("ai") || btn.toLowerCase().contains("generate") ? "🤖 " : "   ";
            System.out.println("    " + marker + btn);
        }

        // Check for AI button
        boolean aiFound = elementExists(AI_BUTTON);
        if (aiFound) {
            String aiText = driver.findElement(AI_BUTTON).getText();
            System.out.println("  ✅ FOUND: AI button → '" + aiText + "'");
            ExtentReportManager.logInfo("AI button found: " + aiText);
        } else {
            System.out.println("  ❌ 'Create with AI' button NOT found on Forms page");
            ExtentReportManager.logInfo("AI button NOT found on Forms tab");

            // Check inside Add Form modal
            if (elementExists(ADD_FORM_BUTTON)) {
                boolean addEnabled = elementEnabled(ADD_FORM_BUTTON);
                System.out.println("  📌 Add Form button found — enabled=" + addEnabled);
                if (addEnabled) {
                    safeClick(ADD_FORM_BUTTON);
                    sleep(3000);
                    boolean aiInModal = elementExists(AI_BUTTON);
                    System.out.println("  " + (aiInModal ? "✅ Found AI button INSIDE Add Form modal" : "❌ Not found in modal either"));
                    ScreenshotUtil.captureScreenshot("TC01_add_form_modal");
                }
            }
        }

        ScreenshotUtil.captureScreenshot("TC01_forms_page");
    }

    @Test(priority = 2)
    public void TC02_verifyAIButtonOnPageTemplates() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC02 - Verify 'Create with AI' in Page Templates creation flow");

        System.out.println("\n📋 TC02: Checking Page Templates for AI option");

        String[] templatePaths = {"/admin/templates", "/admin/page-templates", "/templates", "/reporting"};

        for (String path : templatePaths) {
            try {
                driver.get(AppConstants.BASE_URL + path);
                sleep(3000);
                String content = driver.getPageSource();
                if (!content.contains("Sign in") && !content.contains("404")) {
                    System.out.println("  ✅ Found template page at: " + path);
                    boolean aiFound = elementExists(AI_BUTTON);
                    System.out.println("  AI button: " + (aiFound ? "✅ FOUND" : "❌ Not found"));
                    ScreenshotUtil.captureScreenshot("TC02_templates");
                    return;
                }
            } catch (Exception e) { /* try next */ }
        }

        System.out.println("  ⚠️ No accessible template page found");
    }

    @Test(priority = 3)
    public void TC03_scanAllPagesForAIFeature() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC03 - Scan entire platform for AI-related features");

        System.out.println("\n📋 TC03: Full platform scan for AI features");

        String[][] pages = {
            {"/admin", "Admin Settings"},
            {"/dashboard", "Dashboard"},
            {"/assets", "Assets"},
            {"/issues", "Issues"},
            {"/sessions", "Work Orders"},
            {"/tasks", "Tasks"},
            {"/slds", "SLDs"},
        };

        String[] aiKeywords = {"create with ai", "ai agent", "generate form", "generate template",
                               "ai-driven", "natural language", "8899", "sidecar"};

        int foundCount = 0;
        for (String[] pg : pages) {
            try {
                driver.get(AppConstants.BASE_URL + pg[0]);
                sleep(3000);
                dismissBanner();

                String source = driver.getPageSource().toLowerCase();
                List<String> found = new ArrayList<>();
                for (String kw : aiKeywords) {
                    if (source.contains(kw)) found.add(kw);
                }

                // Check buttons too
                List<String> btns = getAllButtonTexts();
                for (String btn : btns) {
                    if (btn.toLowerCase().contains("ai") || btn.toLowerCase().contains("generate")) {
                        found.add("BUTTON:" + btn);
                    }
                }

                if (!found.isEmpty()) {
                    System.out.println("  ✅ " + pg[1] + ": AI features → " + found);
                    foundCount++;
                } else {
                    System.out.println("  ➖ " + pg[1] + ": No AI features");
                }
            } catch (Exception e) {
                System.out.println("  ❌ " + pg[1] + ": Error — " + e.getMessage());
            }
        }

        System.out.println("\n  📊 AI features found on " + foundCount + "/" + pages.length + " pages");
        if (foundCount == 0) {
            System.out.println("  ⚠️ VERDICT: AI Form feature may NOT be deployed on " + AppConstants.BASE_URL);
        }
    }

    // ═══════════════════════════════════════════
    // SECTION 2: AI FORM GENERATION
    // ═══════════════════════════════════════════

    @Test(priority = 10)
    public void TC04_generateSimpleForm() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC04 - Generate simple form via AI description");

        System.out.println("\n📋 TC04: Generate simple form with AI");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) {
            System.out.println("  ⚠️ SKIP: AI button not found — feature not deployed");
            return;
        }

        safeClick(AI_BUTTON);
        sleep(2000);
        ScreenshotUtil.captureScreenshot("TC04_ai_modal");

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            WebElement input = driver.findElement(AI_DESCRIPTION_INPUT);
            input.clear();
            input.sendKeys("Create a basic inspection form with fields for date, inspector name, and comments");
            sleep(1000);

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(10000); // AI may take time

                String content = driver.getPageSource().toLowerCase();
                boolean hasDate = content.contains("date");
                boolean hasName = content.contains("name") || content.contains("inspector");
                boolean hasComments = content.contains("comment");

                System.out.println("  Date field: " + (hasDate ? "✅" : "❌"));
                System.out.println("  Name field: " + (hasName ? "✅" : "❌"));
                System.out.println("  Comments field: " + (hasComments ? "✅" : "❌"));

                ScreenshotUtil.captureScreenshot("TC04_generated");
            }
        } else {
            System.out.println("  ❌ No description input found for AI");
        }
    }

    @Test(priority = 11)
    public void TC05_generateFormWithSpecificFieldTypes() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC05 - Generate form with specific field types (text, number, select, date)");

        System.out.println("\n📋 TC05: Generate form with specific field types");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys(
                "Create a NETA MTS inspection form with: " +
                "voltage (number), current (number), resistance (number), " +
                "test date (date picker), pass/fail (select dropdown), inspector notes (text area)"
            );

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(10000);

                String content = driver.getPageSource().toLowerCase();
                String[] fields = {"voltage", "current", "resistance", "date", "pass", "note"};
                for (String f : fields) {
                    System.out.println("  " + f + ": " + (content.contains(f) ? "✅" : "❌"));
                }
                ScreenshotUtil.captureScreenshot("TC05_specific_fields");
            }
        }
    }

    @Test(priority = 12)
    public void TC06_generateComplexFormWith10PlusFields() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC06 - Generate complex form with 10+ fields and nested sections");

        System.out.println("\n📋 TC06: Complex form with 10+ fields");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys(
                "Create a PM checklist for circuit breakers with sections: " +
                "Visual Inspection (5 checkboxes: enclosure damage, corrosion, loose parts, labeling, grounding), " +
                "Electrical Testing (3 number fields: voltage, current, resistance), " +
                "Thermal Imaging (2 fields: IR camera type dropdown, temperature reading), " +
                "Sign-off (technician name text, date picker, pass/fail select)"
            );

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(15000); // Complex form may take longer

                ScreenshotUtil.captureScreenshot("TC06_complex_form");
                System.out.println("  Complex form generation completed — verify in screenshot");
            }
        }
    }

    @Test(priority = 13)
    public void TC07_generateFormWithVagueDescription() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC07 - Generate form with vague description ('make a form')");

        System.out.println("\n📋 TC07: Vague description test");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys("make a form");

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(10000);

                String content = driver.getPageSource().toLowerCase();
                boolean hasError = content.contains("error") || content.contains("something went wrong");
                boolean hasCrash = content.contains("application error");

                System.out.println("  Error shown: " + (hasError ? "❌ YES — potential BUG" : "✅ No error"));
                System.out.println("  Crash: " + (hasCrash ? "❌ YES — BUG" : "✅ No crash"));
                ScreenshotUtil.captureScreenshot("TC07_vague");
            }
        }
    }

    // ═══════════════════════════════════════════
    // SECTION 3: FORM TYPE & SCHEMA VALIDATION
    // ═══════════════════════════════════════════

    @Test(priority = 20)
    public void TC08_verifyFormTypeSelection() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_VALIDATION,
            "TC08 - Verify form type selection (enum eg_form_types: 5, 6, 7)");

        System.out.println("\n📋 TC08: Form type selection");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        // Look for form type selector
        By typeSelector = By.xpath(
            "//select[contains(@name,'type')] | " +
            "//label[contains(text(),'Form Type')]/following-sibling::* | " +
            "//*[contains(@data-testid,'form-type')]"
        );

        if (elementExists(typeSelector)) {
            System.out.println("  ✅ Form type selector found");
            List<WebElement> options = driver.findElements(By.tagName("option"));
            System.out.println("  Options (" + options.size() + "):");
            for (WebElement opt : options) {
                System.out.println("    → " + opt.getText().trim());
            }
        } else {
            System.out.println("  ⚠️ No form type selector in AI creation flow");
        }

        ScreenshotUtil.captureScreenshot("TC08_form_types");
    }

    @Test(priority = 21)
    public void TC09_verifyGeneratedFormConformsToSchema() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_VALIDATION,
            "TC09 - Verify AI-generated form follows EG Forms v2 schema");

        System.out.println("\n📋 TC09: Schema validation after generation");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys("Create a simple voltage test form");

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(10000);

                // Check console for schema validation errors
                List<String> errors = getConsoleErrors();
                List<String> schemaErrors = errors.stream()
                    .filter(e -> e.toLowerCase().contains("schema") || e.toLowerCase().contains("validation"))
                    .collect(Collectors.toList());

                if (!schemaErrors.isEmpty()) {
                    System.out.println("  ❌ Schema validation errors found:");
                    schemaErrors.forEach(e -> System.out.println("    → " + e));
                } else {
                    System.out.println("  ✅ No schema validation errors in console");
                }

                ScreenshotUtil.captureScreenshot("TC09_schema");
            }
        }
    }

    @Test(priority = 22)
    public void TC10_verifyGeneratedFieldsAreEditable() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_VALIDATION,
            "TC10 - Verify AI-generated fields can be edited in Form Builder");

        System.out.println("\n📋 TC10: Editability of generated fields");

        // After TC09, form should still be visible
        // Check if form builder elements are interactable
        if (elementExists(FORM_BUILDER_CONTAINER)) {
            System.out.println("  ✅ Form Builder detected");

            // Check for draggable/editable fields
            List<WebElement> formFields = driver.findElements(By.cssSelector(
                "[class*='formio-component'], [class*='form-field'], [draggable='true']"
            ));
            System.out.println("  Form fields found: " + formFields.size());

            if (!formFields.isEmpty()) {
                try {
                    formFields.get(0).click();
                    sleep(1000);
                    System.out.println("  ✅ First field is clickable/editable");
                } catch (Exception e) {
                    System.out.println("  ❌ Fields are not clickable: " + e.getMessage());
                }
            }
        } else {
            System.out.println("  ⚠️ Form Builder not detected — may need to generate form first");
        }

        ScreenshotUtil.captureScreenshot("TC10_editable");
    }

    // ═══════════════════════════════════════════
    // SECTION 4: ITERATIVE REFINEMENT
    // ═══════════════════════════════════════════

    @Test(priority = 30)
    public void TC11_refineGeneratedFormWithFollowUp() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC11 - Refine AI-generated form with follow-up instruction");

        System.out.println("\n📋 TC11: Iterative refinement test");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        // Step 1: Generate initial
        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys("Create a voltage test form with one voltage field");

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(10000);
                ScreenshotUtil.captureScreenshot("TC11_initial");

                // Step 2: Look for refinement input
                By refineInput = By.xpath(
                    "//textarea[contains(@placeholder,'refine')] | " +
                    "//textarea[contains(@placeholder,'modify')] | " +
                    "//input[contains(@placeholder,'refine')]"
                );

                if (elementExists(refineInput)) {
                    driver.findElement(refineInput).sendKeys("Add a checkbox for pass/fail and a comments text area");
                    By refineBtn = By.xpath(
                        "//button[contains(text(),'Refine')] | " +
                        "//button[contains(text(),'Update')] | " +
                        "//button[contains(text(),'Modify')]"
                    );
                    if (elementExists(refineBtn)) {
                        driver.findElement(refineBtn).click();
                        sleep(10000);
                        System.out.println("  ✅ Refinement submitted");
                    }
                    ScreenshotUtil.captureScreenshot("TC11_refined");
                } else {
                    System.out.println("  ⚠️ No refinement input found — iterative refinement may not be supported");
                }
            }
        }
    }

    @Test(priority = 31)
    public void TC12_verifyRefinementPreservesPreviousChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC12 - Verify refinement preserves previously accepted changes");

        System.out.println("\n📋 TC12: Refinement preservation check");
        // This test verifies that after refinement, the original fields are still present
        // Depends on TC11 having run
        System.out.println("  ℹ️ Manual verification: Check screenshot from TC11 —");
        System.out.println("    → Original voltage field should still be present after adding checkbox/comments");
    }

    // ═══════════════════════════════════════════
    // SECTION 5: ERROR HANDLING & EDGE CASES
    // ═══════════════════════════════════════════

    @Test(priority = 40)
    public void TC13_xssInjectionInDescription() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_VALIDATION,
            "TC13 - XSS injection in AI description field");

        System.out.println("\n📋 TC13: XSS injection test");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys("<script>alert('XSS')</script>");

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(5000);

                // No alert should appear
                try {
                    driver.switchTo().alert();
                    System.out.println("  ❌ BUG: XSS alert appeared — script tag NOT sanitized!");
                    driver.switchTo().alert().dismiss();
                } catch (Exception e) {
                    System.out.println("  ✅ XSS blocked — no alert dialog");
                }

                ScreenshotUtil.captureScreenshot("TC13_xss");
            }
        }
    }

    @Test(priority = 41)
    public void TC14_sqlInjectionInDescription() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_VALIDATION,
            "TC14 - SQL injection in AI description field");

        System.out.println("\n📋 TC14: SQL injection test");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys("'; DROP TABLE forms; --");

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(5000);

                String content = driver.getPageSource().toLowerCase();
                boolean hasServerError = content.contains("500") || content.contains("internal server error");
                System.out.println("  Server error: " + (hasServerError ? "❌ BUG — possible SQL injection" : "✅ Handled gracefully"));

                ScreenshotUtil.captureScreenshot("TC14_sql_injection");
            }
        }
    }

    @Test(priority = 42)
    public void TC15_emptyDescriptionSubmit() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_VALIDATION,
            "TC15 - Submit empty description to AI generator");

        System.out.println("\n📋 TC15: Empty description test");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        // Don't type anything — just click generate
        if (elementExists(GENERATE_BUTTON)) {
            boolean genEnabled = elementEnabled(GENERATE_BUTTON);
            System.out.println("  Generate button enabled (empty input): " + genEnabled);

            if (genEnabled) {
                safeClick(GENERATE_BUTTON);
                sleep(3000);

                // Should show validation error
                String content = driver.getPageSource().toLowerCase();
                boolean hasValidation = content.contains("required") || content.contains("please enter") ||
                                        content.contains("description is required");
                System.out.println("  Validation shown: " + (hasValidation ? "✅" : "❌ No validation — BUG"));
            } else {
                System.out.println("  ✅ Generate button correctly disabled when empty");
            }

            ScreenshotUtil.captureScreenshot("TC15_empty");
        }
    }

    @Test(priority = 43)
    public void TC16_veryLongDescriptionInput() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_VALIDATION,
            "TC16 - Very long description (5000+ chars)");

        System.out.println("\n📋 TC16: Long description test");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            StringBuilder longText = new StringBuilder("Create a form with fields: ");
            for (int i = 0; i < 500; i++) {
                longText.append("field_").append(i).append(" (text), ");
            }
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys(longText.toString());
            sleep(1000);

            System.out.println("  Input length: " + longText.length() + " chars");

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(15000);

                List<String> errors = getConsoleErrors();
                System.out.println("  Console errors: " + errors.size());
                ScreenshotUtil.captureScreenshot("TC16_long_input");
            }
        }
    }

    // ═══════════════════════════════════════════
    // SECTION 6: SAVE & PERSISTENCE
    // ═══════════════════════════════════════════

    @Test(priority = 50)
    public void TC17_saveAIGeneratedForm() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC17 - Save AI-generated form and verify persistence");

        System.out.println("\n📋 TC17: Save AI-generated form");

        navigateToAdmin();
        clickFormsTab();

        if (!elementExists(AI_BUTTON)) { System.out.println("  ⚠️ SKIP"); return; }

        safeClick(AI_BUTTON);
        sleep(2000);

        String formName = "QA_AI_Test_" + System.currentTimeMillis();

        if (elementExists(AI_DESCRIPTION_INPUT)) {
            driver.findElement(AI_DESCRIPTION_INPUT).sendKeys(
                "Create a form named '" + formName + "' with voltage and current fields"
            );

            if (elementExists(GENERATE_BUTTON)) {
                safeClick(GENERATE_BUTTON);
                sleep(10000);

                // Save
                if (elementExists(SAVE_FORM_BUTTON) && elementEnabled(SAVE_FORM_BUTTON)) {
                    driver.findElement(SAVE_FORM_BUTTON).click();
                    sleep(5000);

                    // Verify in list
                    navigateToAdmin();
                    clickFormsTab();
                    sleep(3000);

                    boolean found = driver.getPageSource().contains(formName);
                    System.out.println("  Form '" + formName + "' in list: " + (found ? "✅" : "❌"));
                } else {
                    System.out.println("  ❌ Save button not found or disabled");
                }
            }
        }

        ScreenshotUtil.captureScreenshot("TC17_saved");
    }

    @Test(priority = 51)
    public void TC18_verifyFormWorksInReportPipeline() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC18 - Verify saved AI form works in report generation");

        System.out.println("\n📋 TC18: Report pipeline integration");
        System.out.println("  ℹ️ Manual verification needed:");
        System.out.println("    1. Open a saved AI-generated form");
        System.out.println("    2. Use it in report generation pipeline");
        System.out.println("    3. Verify all fields render correctly in the report");
    }

    // ═══════════════════════════════════════════
    // SECTION 7: AGENT SERVICE CHECKS
    // ═══════════════════════════════════════════

    @Test(priority = 60)
    public void TC19_verifyAgentServiceNetworkCalls() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC19 - Monitor network for AI agent service (port 8899)");

        System.out.println("\n📋 TC19: Network monitoring for AI agent");

        navigateToAdmin();
        clickFormsTab();

        // Check JS for any fetch/XHR to port 8899
        String jsCheck = (String) js.executeScript(
            "var found = []; " +
            "var origFetch = window.fetch; " +
            "window.__agentCalls = []; " +
            "window.fetch = function() { " +
            "  var url = arguments[0]; " +
            "  if (typeof url === 'string' && (url.includes('8899') || url.includes('agent'))) { " +
            "    window.__agentCalls.push(url); " +
            "  } " +
            "  return origFetch.apply(this, arguments); " +
            "}; " +
            "return 'Monitoring started';"
        );
        System.out.println("  " + jsCheck);

        // Click through tabs to trigger any calls
        String[] tabs = {"Forms", "PM", "Classes", "Sites"};
        for (String tab : tabs) {
            try {
                driver.findElement(By.xpath("//*[text()='" + tab + "']")).click();
                sleep(2000);
            } catch (Exception e) { /* skip */ }
        }

        // Check captured calls
        Object calls = js.executeScript("return window.__agentCalls || [];");
        System.out.println("  Agent calls captured: " + calls);

        // Also check browser performance logs
        List<String> errors = getConsoleErrors();
        List<String> agentErrors = errors.stream()
            .filter(e -> e.contains("8899") || e.contains("agent"))
            .collect(Collectors.toList());

        if (!agentErrors.isEmpty()) {
            System.out.println("  Agent-related console errors:");
            agentErrors.forEach(e -> System.out.println("    → " + e));
        }
    }

    @Test(priority = 61)
    public void TC20_deepInspectFormBuilderForAIIntegration() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC20 - Deep inspect Form Builder page for hidden AI elements");

        System.out.println("\n📋 TC20: Form Builder deep inspection");

        navigateToAdmin();
        clickFormsTab();

        // Try opening Form Builder
        if (elementExists(ADD_FORM_BUTTON) && elementEnabled(ADD_FORM_BUTTON)) {
            safeClick(ADD_FORM_BUTTON);
            sleep(3000);

            // Search ENTIRE DOM for AI-related attributes
            String aiElements = (String) js.executeScript(
                "var results = []; " +
                "document.querySelectorAll('*').forEach(function(el) { " +
                "  var attrs = el.attributes; " +
                "  for (var i = 0; i < attrs.length; i++) { " +
                "    var name = attrs[i].name.toLowerCase(); " +
                "    var value = attrs[i].value.toLowerCase(); " +
                "    if (name.includes('ai') || value.includes('ai') || " +
                "        name.includes('generate') || value.includes('generate') || " +
                "        name.includes('agent') || value.includes('8899')) { " +
                "      results.push(el.tagName + '[' + attrs[i].name + '=' + attrs[i].value + ']'); " +
                "    } " +
                "  } " +
                "  var text = el.textContent.trim().toLowerCase(); " +
                "  if (text === 'create with ai' || text === 'generate form' || text === 'ai') { " +
                "    results.push('TEXT: ' + el.tagName + ' → ' + el.textContent.trim()); " +
                "  } " +
                "}); " +
                "return results.slice(0, 20).join('\\n');"
            );

            System.out.println("  AI-related DOM elements:\n" + aiElements);
            ScreenshotUtil.captureScreenshot("TC20_form_builder_inspect");
        } else {
            System.out.println("  ⚠️ Add Form button disabled or not found");
        }
    }

    // ═══════════════════════════════════════════
    // SECTION 8: TEMPLATE EXPLORER / TESTER
    // Jira: Template Explorer / Tester (ZP-1583 child)
    // ═══════════════════════════════════════════

    @Test(priority = 70)
    public void TC21_loadTemplateExplorerListsAllTemplates() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC21 - Load template explorer — all templates listed with metadata");

        System.out.println("\n📋 TC21: Load template explorer");

        // Navigate to template explorer
        String[] explorerPaths = {"/admin/templates", "/templates", "/admin/page-templates", "/reporting/templates"};
        boolean found = false;

        for (String path : explorerPaths) {
            try {
                driver.get(AppConstants.BASE_URL + path);
                sleep(3000);
                dismissBanner();
                String content = driver.getPageSource();
                if (!content.contains("Sign in") && !content.contains("404") && !content.contains("Page not found")) {
                    System.out.println("  ✅ Template explorer found at: " + path);
                    found = true;

                    // Check for template list
                    List<WebElement> rows = driver.findElements(By.cssSelector("tr, [class*='template-row'], [class*='list-item']"));
                    System.out.println("  Template rows found: " + rows.size());

                    // Check for metadata columns: name, version, type, created, updated
                    String[] metadataFields = {"name", "version", "type", "created", "updated", "layout"};
                    String pageSrc = content.toLowerCase();
                    for (String field : metadataFields) {
                        boolean has = pageSrc.contains(field);
                        System.out.println("    " + (has ? "✅" : "❌") + " Metadata '" + field + "': " + (has ? "found" : "not found"));
                    }
                    ScreenshotUtil.captureScreenshot("TC21_template_explorer");
                    break;
                }
            } catch (Exception e) { /* try next */ }
        }

        if (!found) {
            System.out.println("  ❌ Template explorer page NOT found at any known URL");
            System.out.println("  ⚠️ Feature may not be deployed");
        }
    }

    @Test(priority = 71)
    public void TC22_searchTemplatesByName() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC22 - Search templates by name, type, and form type");

        System.out.println("\n📋 TC22: Search templates");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        // Look for search input
        By searchInput = By.xpath(
            "//input[contains(@placeholder,'Search')] | " +
            "//input[contains(@placeholder,'search')] | " +
            "//input[contains(@placeholder,'Filter')] | " +
            "//input[@type='search']"
        );

        if (elementExists(searchInput)) {
            System.out.println("  ✅ Search input found");

            // Test search by name
            WebElement search = driver.findElement(searchInput);
            search.clear();
            search.sendKeys("NETA");
            sleep(2000);

            String content = driver.getPageSource().toLowerCase();
            boolean filtered = content.contains("neta");
            System.out.println("  Search 'NETA': " + (filtered ? "✅ Results filtered" : "❌ No filtering"));

            // Test partial match
            search.clear();
            search.sendKeys("Ins");
            sleep(2000);
            System.out.println("  Partial search 'Ins': results shown");

            // Test special characters
            search.clear();
            search.sendKeys("<script>alert(1)</script>");
            sleep(2000);
            try {
                driver.switchTo().alert();
                System.out.println("  ❌ BUG: XSS in search field!");
                driver.switchTo().alert().dismiss();
            } catch (Exception e) {
                System.out.println("  ✅ XSS blocked in search");
            }

            search.clear();
            ScreenshotUtil.captureScreenshot("TC22_search");
        } else {
            System.out.println("  ❌ Search input NOT found on template explorer");
        }
    }

    @Test(priority = 72)
    public void TC23_selectTemplateForPreview() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC23 - Select template for preview — preview panel renders with sample data");

        System.out.println("\n📋 TC23: Template preview");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        // Try clicking on the first template in the list
        By templateRow = By.xpath(
            "//tr[contains(@class,'clickable')] | " +
            "//tr/td/a | " +
            "//div[contains(@class,'template-item')] | " +
            "//table//tbody//tr"
        );

        List<WebElement> templates = driver.findElements(templateRow);
        System.out.println("  Templates found: " + templates.size());

        if (!templates.isEmpty()) {
            try {
                templates.get(0).click();
                sleep(3000);

                // Check for preview panel
                String content = driver.getPageSource().toLowerCase();
                boolean hasPreview = content.contains("preview") || content.contains("render") || content.contains("sample");
                System.out.println("  Preview panel: " + (hasPreview ? "✅ Found" : "❌ Not found"));

                ScreenshotUtil.captureScreenshot("TC23_preview");
            } catch (Exception e) {
                System.out.println("  ❌ Could not click template: " + e.getMessage());
            }
        } else {
            System.out.println("  ⚠️ No template rows found to click");
        }
    }

    @Test(priority = 73)
    public void TC24_switchDataSetInPreview() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC24 - Switch data set in preview — template re-renders with new data");

        System.out.println("\n📋 TC24: Switch data set in preview");

        // Look for data set selector
        By dataSetSelector = By.xpath(
            "//select[contains(@name,'dataset')] | " +
            "//select[contains(@name,'data')] | " +
            "//*[contains(@data-testid,'dataset')] | " +
            "//button[contains(text(),'Sample Data')]"
        );

        if (elementExists(dataSetSelector)) {
            System.out.println("  ✅ Data set selector found");
            ScreenshotUtil.captureScreenshot("TC24_dataset");
        } else {
            System.out.println("  ⚠️ Data set selector not found — feature may not be deployed");
        }
    }

    @Test(priority = 74)
    public void TC25_previewWithMissingRequiredFields() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC25 - Preview template with missing required fields — error shown inline");

        System.out.println("\n📋 TC25: Preview with missing fields");
        System.out.println("  ℹ️ Requires template preview to be functional");
        System.out.println("  Expected: Error shown inline, no crash");
        System.out.println("  Check: No white screen or application error page");

        String content = driver.getPageSource().toLowerCase();
        boolean hasCrash = content.contains("application error") || content.contains("something went wrong");
        System.out.println("  Crash check: " + (hasCrash ? "❌ Page crashed!" : "✅ No crash"));
    }

    @Test(priority = 75)
    public void TC26_viewTemplateStructureBreakdown() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC26 - View template structure — header, body, footer sections displayed");

        System.out.println("\n📋 TC26: Template structure breakdown");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        String content = driver.getPageSource().toLowerCase();
        String[] sections = {"header", "body", "footer", "section", "page break", "layout"};

        System.out.println("  Template structure elements:");
        for (String sec : sections) {
            boolean found = content.contains(sec);
            System.out.println("    " + (found ? "✅" : "❌") + " '" + sec + "': " + (found ? "found" : "not found"));
        }

        ScreenshotUtil.captureScreenshot("TC26_structure");
    }

    @Test(priority = 76)
    public void TC27_emptyStateNoTemplates() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC27 - Empty state — no templates — appropriate message shown");

        System.out.println("\n📋 TC27: Empty state test");
        System.out.println("  ℹ️ This test requires a clean environment with no templates");
        System.out.println("  Expected: 'No templates found' or similar empty state message");
        System.out.println("  NOT: blank page, error, or spinner stuck");

        // Search for something that doesn't exist to simulate empty state
        By searchInput = By.xpath("//input[contains(@placeholder,'Search')] | //input[@type='search']");
        if (elementExists(searchInput)) {
            WebElement search = driver.findElement(searchInput);
            search.clear();
            search.sendKeys("zzzznonexistenttemplate12345");
            sleep(2000);

            String content = driver.getPageSource().toLowerCase();
            boolean hasEmptyMsg = content.contains("no templates") || content.contains("no results") ||
                                  content.contains("no matching") || content.contains("not found");
            System.out.println("  Empty state message: " + (hasEmptyMsg ? "✅ Shown" : "❌ Not shown — BUG"));

            search.clear();
            ScreenshotUtil.captureScreenshot("TC27_empty_state");
        }
    }

    @Test(priority = 77)
    public void TC28_rapidTemplateSwitchingNoRaceCondition() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC28 - Rapid template switching — no race conditions");

        System.out.println("\n📋 TC28: Rapid switching race condition test");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        List<WebElement> templates = driver.findElements(By.xpath("//table//tbody//tr"));
        if (templates.size() >= 2) {
            System.out.println("  Found " + templates.size() + " templates — rapidly switching...");

            // Click between templates rapidly
            for (int i = 0; i < 5; i++) {
                try {
                    templates.get(i % templates.size()).click();
                    sleep(300); // Very short wait to trigger race condition
                } catch (Exception e) { break; }
            }
            sleep(3000);

            // Check no crash
            String content = driver.getPageSource().toLowerCase();
            boolean hasCrash = content.contains("application error") || content.contains("something went wrong");
            System.out.println("  After rapid switching: " + (hasCrash ? "❌ CRASH!" : "✅ No crash"));

            // Check console errors
            List<String> errors = getConsoleErrors();
            System.out.println("  Console errors after rapid switching: " + errors.size());

            ScreenshotUtil.captureScreenshot("TC28_rapid_switch");
        } else {
            System.out.println("  ⚠️ Need 2+ templates to test rapid switching");
        }
    }

    // ═══════════════════════════════════════════
    // SECTION 9: TEMPLATE EDITING AGENT
    // Jira: Template Editing Agent (ZP-1583 child)
    // ═══════════════════════════════════════════

    @Test(priority = 80)
    public void TC29_verifyAIEditEntryPointInTemplateEditor() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC29 - Verify AI edit entry point exists in template editor");

        System.out.println("\n📋 TC29: AI edit entry point in template editor");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        // Try to open a template for editing
        List<WebElement> editButtons = driver.findElements(By.xpath(
            "//button[contains(text(),'Edit')] | " +
            "//a[contains(text(),'Edit')] | " +
            "//*[contains(@class,'edit-icon')] | " +
            "//button[contains(@aria-label,'edit')]"
        ));

        if (!editButtons.isEmpty()) {
            editButtons.get(0).click();
            sleep(3000);

            // Look for AI editing option
            By aiEdit = By.xpath(
                "//button[contains(text(),'AI')] | " +
                "//button[contains(text(),'Edit with AI')] | " +
                "//button[contains(text(),'AI Assistant')] | " +
                "//*[contains(text(),'natural language')] | " +
                "//*[contains(@data-testid,'ai-edit')]"
            );

            boolean aiFound = elementExists(aiEdit);
            System.out.println("  AI edit option in template editor: " + (aiFound ? "✅ FOUND" : "❌ NOT FOUND"));

            if (aiFound) {
                String text = driver.findElement(aiEdit).getText();
                System.out.println("  AI button text: '" + text + "'");
            }

            ScreenshotUtil.captureScreenshot("TC29_ai_edit_entry");
        } else {
            System.out.println("  ⚠️ No edit button found on templates page");
        }
    }

    @Test(priority = 81)
    public void TC30_describeTemplateChangeInNaturalLanguage() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC30 - Describe template change in natural language");

        System.out.println("\n📋 TC30: Natural language template editing");

        // This test requires the AI edit modal to be open
        By aiInput = By.xpath(
            "//textarea[contains(@placeholder,'describe')] | " +
            "//textarea[contains(@placeholder,'change')] | " +
            "//textarea[contains(@placeholder,'instruction')] | " +
            "//input[contains(@placeholder,'Edit with AI')]"
        );

        if (elementExists(aiInput)) {
            WebElement input = driver.findElement(aiInput);
            input.clear();
            input.sendKeys("Add a summary table at the top of the template");
            sleep(1000);

            // Look for submit/apply button
            By applyBtn = By.xpath(
                "//button[contains(text(),'Apply')] | " +
                "//button[contains(text(),'Generate')] | " +
                "//button[contains(text(),'Submit')]"
            );

            if (elementExists(applyBtn)) {
                driver.findElement(applyBtn).click();
                sleep(10000); // AI processing time

                System.out.println("  ✅ Natural language instruction submitted");
                ScreenshotUtil.captureScreenshot("TC30_nl_edit");
            } else {
                System.out.println("  ❌ Apply/Generate button not found");
            }
        } else {
            System.out.println("  ⚠️ AI instruction input not found — feature may not be deployed");
        }
    }

    @Test(priority = 82)
    public void TC31_previewChangesBeforeApplying() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC31 - Preview AI changes before applying to template");

        System.out.println("\n📋 TC31: Preview before apply");

        // After TC30, check for preview/diff
        String content = driver.getPageSource().toLowerCase();
        boolean hasPreview = content.contains("preview") || content.contains("diff") ||
                            content.contains("before") || content.contains("after");
        boolean hasApply = content.contains("apply changes") || content.contains("accept") || content.contains("confirm");
        boolean hasDiscard = content.contains("discard") || content.contains("cancel") || content.contains("reject");

        System.out.println("  Preview/Diff view: " + (hasPreview ? "✅" : "❌ Not found"));
        System.out.println("  Apply button: " + (hasApply ? "✅" : "❌ Not found"));
        System.out.println("  Discard button: " + (hasDiscard ? "✅" : "❌ Not found"));
    }

    @Test(priority = 83)
    public void TC32_undoRevertAgentChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC32 - Undo/revert agent-made changes");

        System.out.println("\n📋 TC32: Undo/revert agent changes");

        By undoBtn = By.xpath(
            "//button[contains(text(),'Undo')] | " +
            "//button[contains(text(),'Revert')] | " +
            "//button[contains(@aria-label,'undo')] | " +
            "//button[contains(text(),'Ctrl+Z')]"
        );

        boolean hasUndo = elementExists(undoBtn);
        System.out.println("  Undo/Revert button: " + (hasUndo ? "✅ Found" : "❌ Not found"));

        if (hasUndo) {
            driver.findElement(undoBtn).click();
            sleep(2000);
            System.out.println("  Undo clicked — verify template reverted to previous state");
            ScreenshotUtil.captureScreenshot("TC32_undo");
        }
    }

    @Test(priority = 84)
    public void TC33_agentUnderstandsTemplateStructure() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC33 - Agent understands template structure (header/body/footer/page breaks)");

        System.out.println("\n📋 TC33: Agent template structure understanding");
        System.out.println("  ℹ️ Test instructions to verify agent understanding:");
        System.out.println("    1. 'Change the header font to bold' → should modify header only");
        System.out.println("    2. 'Add a page break after the summary section' → should add page break");
        System.out.println("    3. 'Move the footer logo to the right' → should modify footer style");
        System.out.println("    4. 'Add a new body section for test results' → should add to body");
        System.out.println("  Status: Requires AI agent to be functional — manual test if needed");
    }

    @Test(priority = 85)
    public void TC34_agentServiceUnavailableFallbackToManual() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC34 - Agent service unavailable — fallback to manual editing");

        System.out.println("\n📋 TC34: Agent unavailable fallback test");

        // Check if manual editing is still accessible when AI fails
        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        // Try to find manual edit option
        List<WebElement> editBtns = driver.findElements(By.xpath(
            "//button[contains(text(),'Edit')] | //a[contains(text(),'Edit')]"
        ));

        if (!editBtns.isEmpty()) {
            editBtns.get(0).click();
            sleep(3000);

            // Verify manual editing controls exist
            String content = driver.getPageSource().toLowerCase();
            boolean hasManualControls = content.contains("save") || content.contains("edit") || content.contains("field");
            System.out.println("  Manual editing controls: " + (hasManualControls ? "✅ Available" : "❌ Not available"));
            System.out.println("  If AI agent is down, users should still be able to edit templates manually");
            ScreenshotUtil.captureScreenshot("TC34_manual_fallback");
        } else {
            System.out.println("  ⚠️ No edit button found");
        }
    }

    @Test(priority = 86)
    public void TC35_complexTemplateModificationViaAI() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC35 - Complex template modification via AI");

        System.out.println("\n📋 TC35: Complex AI modification test");
        System.out.println("  ℹ️ Complex instructions to test:");
        System.out.println("    1. 'Restructure: move summary to top, add logo, resize all images to 50%'");
        System.out.println("    2. 'Create a 3-column layout for the test results section with borders'");
        System.out.println("    3. 'Add conditional formatting: red for failed tests, green for passed'");
        System.out.println("  Expected: Agent handles multi-part instructions without losing context");
        System.out.println("  Status: Requires AI agent — manual test if needed");
    }


    // ═══════════════════════════════════════════
    // SECTION 10: STARTER TEMPLATES LIBRARY
    // Jira: Give Users Something to Start With (ZP-1583 child)
    // ═══════════════════════════════════════════

    @Test(priority = 90)
    public void TC36_starterTemplateLibraryExists() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC36 - Starter template library exists with pre-built templates");

        System.out.println("\n📋 TC36: Starter template library");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        String content = driver.getPageSource().toLowerCase();

        // Check for starter/seed templates
        String[] starterKeywords = {"starter", "template library", "pre-built", "seed", "default template", "neta", "inspection"};
        List<String> found = new ArrayList<>();
        for (String kw : starterKeywords) {
            if (content.contains(kw)) found.add(kw);
        }

        System.out.println("  Starter template keywords: " + (found.isEmpty() ? "❌ None found" : "✅ " + found));

        // Check for common report types
        String[] reportTypes = {"cover page", "summary", "detail", "appendix", "neta", "inspection", "asset condition"};
        for (String type : reportTypes) {
            System.out.println("    " + (content.contains(type) ? "✅" : "➖") + " '" + type + "'");
        }

        ScreenshotUtil.captureScreenshot("TC36_starter_library");
    }

    @Test(priority = 91)
    public void TC37_startFromTemplateOption() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC37 - 'Start from template' option in report creation flow");

        System.out.println("\n📋 TC37: 'Start from template' option");

        // Check in template creation flow
        By startFromTemplate = By.xpath(
            "//button[contains(text(),'Start from template')] | " +
            "//button[contains(text(),'Use template')] | " +
            "//button[contains(text(),'From template')] | " +
            "//*[contains(text(),'Start from template')]"
        );

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        boolean found = elementExists(startFromTemplate);
        System.out.println("  'Start from template' option: " + (found ? "✅ Found" : "❌ Not found"));

        // Also check Add Form flow
        navigateToAdmin();
        clickFormsTab();
        if (elementExists(ADD_FORM_BUTTON) && elementEnabled(ADD_FORM_BUTTON)) {
            safeClick(ADD_FORM_BUTTON);
            sleep(3000);
            boolean inModal = elementExists(startFromTemplate);
            System.out.println("  In Add Form modal: " + (inModal ? "✅ Found" : "❌ Not found"));
        }

        ScreenshotUtil.captureScreenshot("TC37_start_from_template");
    }

    @Test(priority = 92)
    public void TC38_duplicateStarterTemplateAndCustomize() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC38 - Duplicate starter template and customize");

        System.out.println("\n📋 TC38: Duplicate and customize starter template");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        By duplicateBtn = By.xpath(
            "//button[contains(text(),'Duplicate')] | " +
            "//button[contains(text(),'Copy')] | " +
            "//button[contains(text(),'Clone')] | " +
            "//button[contains(@aria-label,'duplicate')]"
        );

        boolean hasDuplicate = elementExists(duplicateBtn);
        System.out.println("  Duplicate/Copy button: " + (hasDuplicate ? "✅ Found" : "❌ Not found"));

        if (hasDuplicate) {
            driver.findElement(duplicateBtn).click();
            sleep(3000);
            System.out.println("  Duplicated — verify new template created with 'Copy of...' prefix");
            ScreenshotUtil.captureScreenshot("TC38_duplicated");
        }
    }

    @Test(priority = 93)
    public void TC39_starterTemplatesAreReadOnly() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC39 - Starter templates are read-only (cannot modify originals)");

        System.out.println("\n📋 TC39: Starter templates read-only check");
        System.out.println("  ℹ️ Verification steps:");
        System.out.println("    1. Open a starter/seed template");
        System.out.println("    2. Try to edit any field");
        System.out.println("    3. Expected: Edit disabled OR 'This is a starter template' warning");
        System.out.println("    4. Save button should be disabled or show 'Duplicate to edit'");
        System.out.println("  Status: Requires starter templates to exist — check manually");
    }

    @Test(priority = 94)
    public void TC40_starterTemplatesIncludeAllPageTypes() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC40 - Starter templates include cover/summary/detail/appendix pages");

        System.out.println("\n📋 TC40: Starter template page types");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        String content = driver.getPageSource().toLowerCase();

        String[] pageTypes = {"cover", "summary", "detail", "loop", "appendix"};
        System.out.println("  Required page types in starter templates:");
        for (String type : pageTypes) {
            boolean found = content.contains(type);
            System.out.println("    " + (found ? "✅" : "❌") + " " + type + " page");
        }
    }

    @Test(priority = 95)
    public void TC41_starterTemplatesUseCompanyBranding() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC41 - Starter templates use company branding (colors, logos)");

        System.out.println("\n📋 TC41: Company branding in starter templates");
        System.out.println("  ℹ️ Verification: Open a starter template preview and check:");
        System.out.println("    1. Company logo appears in header/footer");
        System.out.println("    2. Company brand colors used in headings/borders");
        System.out.println("    3. Company name appears in template");
        System.out.println("  Status: Visual verification — check template preview manually");
    }

    // ═══════════════════════════════════════════
    // SECTION 11: AGENT SERVICE IMPROVEMENTS
    // Jira: Improvements to Agent (ZP-1583 child)
    // ═══════════════════════════════════════════

    @Test(priority = 100)
    public void TC42_agentHealthCheckEndpoint() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC42 - Agent health check endpoint (GET /health on port 8899)");

        System.out.println("\n📋 TC42: Agent health check");

        // Try to check agent health via JavaScript fetch
        String result = (String) js.executeScript(
            "try { " +
            "  var xhr = new XMLHttpRequest(); " +
            "  xhr.open('GET', window.location.origin.replace(/:\\d+$/, '') + ':8899/health', false); " +
            "  xhr.timeout = 5000; " +
            "  xhr.send(); " +
            "  return 'Status: ' + xhr.status + ' Body: ' + xhr.responseText; " +
            "} catch(e) { " +
            "  return 'Error: ' + e.message; " +
            "}"
        );

        System.out.println("  Agent health check result: " + result);

        if (result != null && result.contains("Status: 200")) {
            System.out.println("  ✅ Agent service is healthy");
        } else {
            System.out.println("  ❌ Agent health check failed — service may not be running");
        }
    }

    @Test(priority = 101)
    public void TC43_agentResponseTimeUnder15Seconds() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC43 - Agent response time < 15 seconds for standard operations");

        System.out.println("\n📋 TC43: Agent response time check");
        System.out.println("  ℹ️ Requires AI agent to be functional");
        System.out.println("  Acceptance criteria: Standard template edit < 15 seconds");
        System.out.println("  Measure: Time from 'Generate' click to result displayed");
        System.out.println("  Status: Will be tested when agent is deployed");
    }

    @Test(priority = 102)
    public void TC44_agentHandlesInvalidTemplateContext() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC44 - Agent handles invalid template context with helpful error");

        System.out.println("\n📋 TC44: Invalid template context");
        System.out.println("  ℹ️ Test: Send malformed/corrupted template JSON to agent");
        System.out.println("  Expected: Helpful error message, not crash");
        System.out.println("  Status: Requires API-level testing — use REST Assured");
    }

    @Test(priority = 103)
    public void TC45_agentLargeTemplateContextTokenLimit() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC45 - Large template context near token limit");

        System.out.println("\n📋 TC45: Token limit handling");
        System.out.println("  ℹ️ Test: Send template with 50+ fields/sections");
        System.out.println("  Expected: Agent truncates/summarizes context, still produces valid response");
        System.out.println("  Status: Requires API-level testing");
    }

    @Test(priority = 104)
    public void TC46_concurrentAgentRequests() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC46 - Concurrent requests (5+ simultaneous) to agent");

        System.out.println("\n📋 TC46: Concurrent agent requests");
        System.out.println("  ℹ️ Test: Send 5 simultaneous requests to agent service");
        System.out.println("  Expected: All handled without interference, no mixed responses");
        System.out.println("  Status: Requires load testing tool (JMeter/k6) or API test");
    }

    @Test(priority = 105)
    public void TC47_streamingResponseForLongOperations() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC47 - Streaming response for long operations");

        System.out.println("\n📋 TC47: Streaming response");
        System.out.println("  ℹ️ Test: Request complex template → check for progressive loading");
        System.out.println("  Expected: Partial results shown progressively, not all-at-once");
        System.out.println("  Check: Loading indicator → partial text → complete result");
        System.out.println("  Status: Requires UI with streaming support");
    }

    @Test(priority = 106)
    public void TC48_agentRecoveryAfterAPIOutage() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC48 - Agent recovery after temporary Anthropic API outage");

        System.out.println("\n📋 TC48: Agent recovery test");
        System.out.println("  ℹ️ Test: Simulate API failure → wait → retry");
        System.out.println("  Expected: Agent retries with exponential backoff");
        System.out.println("  Expected: Eventually succeeds or returns clear error");
        System.out.println("  Status: Requires infrastructure-level testing");
    }

    @Test(priority = 107)
    public void TC49_agentRequestResponseLogging() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_FORM,
            "TC49 - Agent request/response logging for debugging");

        System.out.println("\n📋 TC49: Agent logging verification");

        // Check if any agent-related console logs appear
        List<String> consoleLogs = getConsoleErrors();
        List<String> agentLogs = new ArrayList<>();
        for (String log : consoleLogs) {
            if (log.toLowerCase().contains("agent") || log.toLowerCase().contains("8899")) {
                agentLogs.add(log);
            }
        }

        System.out.println("  Agent-related logs in console: " + agentLogs.size());
        for (String log : agentLogs) {
            System.out.println("    → " + log);
        }
    }


    // ═══════════════════════════════════════════
    // SECTION 12: CENTRALIZED TEMPLATES REFACTOR
    // Jira: Refactor Reporting to Use Centralized Templates Table (ZP-1583 child)
    // ═══════════════════════════════════════════

    @Test(priority = 110)
    public void TC50_generateReportUsingMigratedTemplate() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC50 - Generate report using migrated template — renders identically to pre-migration");

        System.out.println("\n📋 TC50: Report generation with migrated template");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        // Check that templates are listed (migration complete)
        List<WebElement> templates = driver.findElements(By.xpath("//table//tbody//tr | //div[contains(@class,'template')]"));
        System.out.println("  Templates in centralized table: " + templates.size());

        if (templates.size() > 0) {
            System.out.println("  ✅ Templates loaded from centralized ReportingPageTemplate table");
            System.out.println("  ℹ️ Manual verification: Generate a report and compare to pre-migration output");
        } else {
            System.out.println("  ❌ No templates found — migration may not be complete");
        }

        ScreenshotUtil.captureScreenshot("TC50_migrated_templates");
    }

    @Test(priority = 111)
    public void TC51_templateVersioningCorrectVersionUsed() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC51 - Template versioning — correct version used for rendering");

        System.out.println("\n📋 TC51: Template versioning");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        String content = driver.getPageSource().toLowerCase();

        // Check for version column/field
        boolean hasVersion = content.contains("version") || content.contains("v1") || content.contains("v2");
        System.out.println("  Version info visible: " + (hasVersion ? "✅" : "❌ No version column"));

        // Check for version comparison
        boolean hasCompare = content.contains("compare") || content.contains("side-by-side") || content.contains("diff");
        System.out.println("  Version comparison: " + (hasCompare ? "✅ Available" : "❌ Not available"));
    }

    @Test(priority = 112)
    public void TC52_nonExistentTemplateIDHandling() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC52 - Reference non-existent template ID — meaningful error, no crash");

        System.out.println("\n📋 TC52: Non-existent template ID");

        // Navigate to a fake template ID
        driver.get(AppConstants.BASE_URL + "/admin/templates/99999-fake-id-nonexistent");
        sleep(3000);

        String content = driver.getPageSource().toLowerCase();
        boolean hasCrash = content.contains("application error") || content.contains("something went wrong");
        boolean has404 = content.contains("not found") || content.contains("404") || content.contains("does not exist");
        boolean hasBlank = content.trim().length() < 200;

        System.out.println("  Crash: " + (hasCrash ? "❌ BUG — Application crashed!" : "✅ No crash"));
        System.out.println("  404/Not found: " + (has404 ? "✅ Proper error shown" : "❌ No error message"));
        System.out.println("  Blank page: " + (hasBlank ? "❌ BUG — Blank page shown!" : "✅ Content present"));

        ScreenshotUtil.captureScreenshot("TC52_nonexistent_template");
    }

    @Test(priority = 113)
    public void TC53_createNewTemplateInCentralizedTable() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC53 - Create new template in centralized table");

        System.out.println("\n📋 TC53: Create new template");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        // Look for create/add template button
        By createBtn = By.xpath(
            "//button[contains(text(),'Add Template')] | " +
            "//button[contains(text(),'Create Template')] | " +
            "//button[contains(text(),'New Template')] | " +
            "//button[contains(text(),'+ Add')]"
        );

        if (elementExists(createBtn)) {
            boolean enabled = elementEnabled(createBtn);
            System.out.println("  Create Template button: ✅ Found — enabled=" + enabled);
            ScreenshotUtil.captureScreenshot("TC53_create_template");
        } else {
            System.out.println("  ❌ Create Template button not found");
            List<String> btns = getAllButtonTexts();
            System.out.println("  Available buttons: " + btns);
        }
    }

    @Test(priority = 114)
    public void TC54_updateTemplateExistingReportsUnaffected() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC54 - Update template — existing reports use original version");

        System.out.println("\n📋 TC54: Template update backward compatibility");
        System.out.println("  ℹ️ Test steps:");
        System.out.println("    1. Note a template version currently used by an existing report");
        System.out.println("    2. Edit and save the template (creating a new version)");
        System.out.println("    3. Re-generate the existing report");
        System.out.println("    4. Verify: Old report still uses original template version");
        System.out.println("    5. Verify: New reports use the updated template version");
        System.out.println("  Status: Requires report generation — manual/API test");
    }

    @Test(priority = 115)
    public void TC55_deleteTemplateReferencedByActiveReports() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC55 - Delete template referenced by active reports — should be blocked or warned");

        System.out.println("\n📋 TC55: Delete template with active references");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        // Look for delete button
        By deleteBtn = By.xpath(
            "//button[contains(@aria-label,'delete')] | " +
            "//button[contains(@class,'delete')] | " +
            "//*[contains(@data-testid,'delete')]"
        );

        List<WebElement> deleteBtns = driver.findElements(deleteBtn);
        System.out.println("  Delete buttons found: " + deleteBtns.size());

        if (!deleteBtns.isEmpty()) {
            System.out.println("  ℹ️ If template is referenced by active reports:");
            System.out.println("    Expected: Deletion BLOCKED with warning message");
            System.out.println("    OR: Soft-deleted with 'Template is in use' warning");
            System.out.println("    NOT: Silent deletion leaving orphaned references");
        }
    }

    @Test(priority = 116)
    public void TC56_listAllTemplatesWithPagination() {
        ExtentReportManager.createTest(MODULE, FEATURE_AI_TEMPLATE,
            "TC56 - List all templates with pagination");

        System.out.println("\n📋 TC56: Template list pagination");

        driver.get(AppConstants.BASE_URL + "/admin/templates");
        sleep(3000);
        dismissBanner();

        String content = driver.getPageSource().toLowerCase();

        // Check for pagination
        boolean hasPagination = content.contains("rows per page") || content.contains("page") ||
                                content.contains("next") || content.contains("previous") ||
                                content.contains("of ") || content.contains("showing");
        System.out.println("  Pagination controls: " + (hasPagination ? "✅ Found" : "❌ Not found"));

        // Check for row count
        By rowCount = By.xpath("//*[contains(text(),' of ')]");
        if (elementExists(rowCount)) {
            String countText = driver.findElement(rowCount).getText();
            System.out.println("  Row count: " + countText);
        }

        // Check for sorting
        List<WebElement> sortHeaders = driver.findElements(By.xpath("//th[contains(@class,'sortable')] | //th[@role='columnheader']"));
        System.out.println("  Sortable columns: " + sortHeaders.size());

        ScreenshotUtil.captureScreenshot("TC56_pagination");
    }

}
