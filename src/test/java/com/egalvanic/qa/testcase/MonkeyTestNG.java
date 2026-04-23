package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.DashboardPage;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;
import com.egalvanic.qa.utils.ai.FlakinessPrevention;
import com.egalvanic.qa.utils.ai.SmartTestDataGenerator;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.FileWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Monkey Exploratory Test Suite — AI-Powered Random Testing
 *
 * Simulates unpredictable human behavior to find bugs that scripted
 * automation misses. Manual testers explore randomly, click unexpected
 * things, enter weird data — this class does the same programmatically.
 *
 * Features:
 *   - Random page navigation via sidebar
 *   - Random clicking on visible interactive elements
 *   - Random input filling with edge-case data
 *   - Health checks after every action (console errors, crash detection)
 *   - Safety blocklist (never clicks logout, delete, deactivate)
 *   - Configurable via system properties
 *   - JSON report output
 *
 * Configuration (system properties):
 *   -Dmonkey.maxActions=100     Max actions per test (default: 100)
 *   -Dmonkey.maxDuration=300    Max seconds per test (default: 300)
 *   -Dmonkey.actionDelay=500    Delay between actions in ms (default: 500)
 *   -Dmonkey.fillInputs=true    Fill discovered inputs (default: true)
 *
 * Architecture: Standalone (own browser, like BugHuntTestNG).
 * Does NOT extend BaseTest — monkey testing is exploratory and should
 * not share state with other test classes.
 */
public class MonkeyTestNG {

    private static final String MODULE = "Monkey Exploratory";
    private static final String FEATURE = "Random Exploration";

    // Configuration
    private static final int MAX_ACTIONS = Integer.parseInt(
            System.getProperty("monkey.maxActions", "100"));
    private static final int MAX_DURATION_SECONDS = Integer.parseInt(
            System.getProperty("monkey.maxDuration", "300"));
    private static final long ACTION_DELAY_MS = Long.parseLong(
            System.getProperty("monkey.actionDelay", "500"));
    private static final boolean FILL_INPUTS = Boolean.parseBoolean(
            System.getProperty("monkey.fillInputs", "true"));

    // Safety: NEVER click elements containing these words
    private static final Set<String> BLOCKLIST = new HashSet<>(Arrays.asList(
            "logout", "sign out", "signout", "log out", "delete", "remove",
            "deactivate", "disable", "destroy", "drop", "purge", "terminate"
    ));

    // Known sidebar navigation items
    private static final String[] NAV_ITEMS = {
            "Dashboard", "Assets", "Locations", "Connections",
            "Issues", "Jobs", "Tasks"
    };

    protected WebDriver driver;
    protected JavascriptExecutor js;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    private long testStartTime;
    private final Random random = new Random();
    private final List<MonkeyAction> actionLog = new ArrayList<>();
    private final List<MonkeyAnomaly> anomalies = new ArrayList<>();
    private int actionCount = 0;

    // ================================================================
    // LIFECYCLE (standalone browser)
    // ================================================================

    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     Monkey Exploratory Test Suite Starting");
        System.out.println("     Max actions: " + MAX_ACTIONS + " | Max duration: " + MAX_DURATION_SECONDS + "s");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================");

        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);
        opts.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);

        if ("true".equals(System.getProperty("headless"))) {
            opts.addArguments("--headless=new");
        }

        driver = new ChromeDriver(opts);
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        js = (JavascriptExecutor) driver;
        js.executeScript("document.body.style.zoom='90%';");

        ScreenshotUtil.setDriver(driver);
        FlakinessPrevention.installNetworkInterceptor(driver);
        FlakinessPrevention.installConsoleErrorCapture(driver);

        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);

        // Login
        loginAndSelectSite();

        FlakinessPrevention.installNetworkInterceptor(driver);
        FlakinessPrevention.installConsoleErrorCapture(driver);
    }

    @AfterClass
    public void classTeardown() {
        writeMonkeyReport();
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @BeforeMethod
    public void testSetup() {
        testStartTime = System.currentTimeMillis();
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;
        if (result.getStatus() == ITestResult.FAILURE) {
            ScreenshotUtil.captureScreenshot("Monkey_" + result.getMethod().getMethodName() + "_FAIL");
        }
        System.out.printf("  [Monkey] %s: %s (%ds)%n",
                result.getMethod().getMethodName(),
                result.getStatus() == ITestResult.SUCCESS ? "PASS" : "FAIL",
                duration / 1000);
    }

    // ================================================================
    // TEST METHODS
    // ================================================================

    @Test(priority = 1, description = "Random sidebar navigation — visit all pages")
    public void testRandomNavigation() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Random Navigation");

        int navigations = Math.min(MAX_ACTIONS, NAV_ITEMS.length * 2);
        int successCount = 0;
        int errorCount = 0;

        for (int i = 0; i < navigations; i++) {
            String target = NAV_ITEMS[random.nextInt(NAV_ITEMS.length)];
            try {
                navigateToSidebarItem(target);
                pause(1000);

                if (!runHealthChecks("after navigating to " + target)) {
                    errorCount++;
                    recoverIfNeeded();
                } else {
                    successCount++;
                }
                logAction("NAVIGATE", target, errorCount == 0 ? "ok" : "anomaly");
            } catch (Exception e) {
                logAction("NAVIGATE", target, "error: " + e.getMessage());
                errorCount++;
                recoverIfNeeded();
            }
            pause(ACTION_DELAY_MS);
        }

        logStep("Navigation complete: " + successCount + " success, " + errorCount + " errors");
        ExtentReportManager.logPass("Random navigation: " + successCount + "/" + navigations + " successful");
    }

    @Test(priority = 2, description = "Random click exploration across pages")
    public void testRandomClicks() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Random Click Exploration");

        int totalClicks = 0;
        int anomalyCount = 0;

        for (String page : NAV_ITEMS) {
            try {
                navigateToSidebarItem(page);
                pause(1500);

                int clicksOnPage = Math.min(MAX_ACTIONS / NAV_ITEMS.length, 15);
                for (int i = 0; i < clicksOnPage; i++) {
                    if (isTimedOut()) break;

                    boolean clicked = clickRandomElement();
                    if (clicked) {
                        totalClicks++;
                        pause(ACTION_DELAY_MS);
                        if (!runHealthChecks("after click #" + totalClicks + " on " + page)) {
                            anomalyCount++;
                            recoverIfNeeded();
                            navigateToSidebarItem(page);
                            pause(1000);
                        }
                    }
                }
            } catch (Exception e) {
                logStep("Error on page " + page + ": " + e.getMessage());
                recoverIfNeeded();
            }
        }

        logStep("Random clicks complete: " + totalClicks + " clicks, " + anomalyCount + " anomalies");
        ExtentReportManager.logPass("Random clicks: " + totalClicks + " performed, " + anomalyCount + " anomalies");
    }

    @Test(priority = 3, description = "Input fuzzing — fill inputs with edge-case data")
    public void testInputFuzzing() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Input Fuzzing");

        if (!FILL_INPUTS) {
            ExtentReportManager.logInfo("Input fuzzing disabled (monkey.fillInputs=false)");
            return;
        }

        int inputsFilled = 0;
        int anomalyCount = 0;
        String[] pagesWithForms = {"Assets", "Issues", "Locations"};

        for (String page : pagesWithForms) {
            try {
                navigateToSidebarItem(page);
                pause(2000);

                // Try to open a create/add form
                boolean formOpened = tryOpenCreateForm();
                if (!formOpened) {
                    logStep("No create form found on " + page);
                    continue;
                }
                pause(1500);

                // Find all visible inputs and fill them
                int filled = fillVisibleInputs();
                inputsFilled += filled;
                logStep("Filled " + filled + " inputs on " + page);

                // Check for errors
                if (!runHealthChecks("after filling inputs on " + page)) {
                    anomalyCount++;
                    ScreenshotUtil.captureScreenshot("Monkey_InputFuzz_" + page);
                }

                // Press Escape to close form without saving (safety)
                js.executeScript(
                    "var evt = new KeyboardEvent('keydown', {key: 'Escape', bubbles: true});" +
                    "document.dispatchEvent(evt);");
                pause(1000);

            } catch (Exception e) {
                logStep("Input fuzzing error on " + page + ": " + e.getMessage());
                recoverIfNeeded();
            }
        }

        logStep("Input fuzzing complete: " + inputsFilled + " inputs filled, " + anomalyCount + " anomalies");
        ExtentReportManager.logPass("Input fuzzing: " + inputsFilled + " inputs, " + anomalyCount + " anomalies");
    }

    @Test(priority = 10, description = "Full monkey exploration — random actions across all pages")
    public void testFullMonkeyExploration() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Full Monkey Exploration");

        long startTime = System.currentTimeMillis();
        int maxActions = MAX_ACTIONS;
        int anomalyCount = 0;
        int consecutiveFailures = 0;

        for (actionCount = 0; actionCount < maxActions; actionCount++) {
            if (isTimedOut()) {
                logStep("Time limit reached (" + MAX_DURATION_SECONDS + "s)");
                break;
            }
            if (consecutiveFailures >= 5) {
                logStep("Too many consecutive failures — stopping");
                break;
            }

            // Pick random action
            int actionType = random.nextInt(5);
            try {
                switch (actionType) {
                    case 0: // Navigate
                        String target = NAV_ITEMS[random.nextInt(NAV_ITEMS.length)];
                        navigateToSidebarItem(target);
                        logAction("NAVIGATE", target, "ok");
                        break;
                    case 1: case 2: // Click (weighted higher)
                        clickRandomElement();
                        logAction("CLICK", "random element", "ok");
                        break;
                    case 3: // Fill input
                        if (FILL_INPUTS) {
                            fillRandomVisibleInput();
                            logAction("FILL_INPUT", "random input", "ok");
                        }
                        break;
                    case 4: // Scroll
                        int scrollY = random.nextInt(1000) - 500;
                        js.executeScript("window.scrollBy(0, " + scrollY + ");");
                        logAction("SCROLL", String.valueOf(scrollY), "ok");
                        break;
                }

                consecutiveFailures = 0;
                pause(ACTION_DELAY_MS);

                // Health check every 3 actions
                if (actionCount % 3 == 0) {
                    if (!runHealthChecks("action #" + actionCount)) {
                        anomalyCount++;
                        recoverIfNeeded();
                    }
                }

            } catch (Exception e) {
                consecutiveFailures++;
                logAction("ERROR", e.getClass().getSimpleName(), e.getMessage());
                recoverIfNeeded();
            }
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        logStep(String.format("Monkey exploration: %d actions in %ds, %d anomalies found",
                actionCount, elapsed, anomalyCount));
        ExtentReportManager.logPass(String.format("Full monkey: %d actions, %d anomalies, %ds duration",
                actionCount, anomalyCount, elapsed));
    }

    // ================================================================
    // MONKEY ENGINE — Core Actions
    // ================================================================

    private void navigateToSidebarItem(String itemText) {
        try {
            String xpath = "//span[normalize-space()='" + itemText + "'] | //a[normalize-space()='" + itemText + "']";
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
            driver.findElement(By.xpath(xpath)).click();
            pause(1500);
        } catch (Exception e) {
            // Try JS click
            js.executeScript(
                "var items = document.querySelectorAll('span, a');" +
                "for (var i = 0; i < items.length; i++) {" +
                "  if (items[i].textContent.trim() === '" + itemText + "' && items[i].offsetWidth > 0) {" +
                "    items[i].click(); return true;" +
                "  }" +
                "} return false;");
            pause(1500);
        }
    }

    private boolean clickRandomElement() {
        try {
            // Find all visible clickable elements
            String script =
                "var els = [];" +
                "document.querySelectorAll('a, button, [role=\"button\"], [onclick], [class*=\"clickable\"]').forEach(function(el) {" +
                "  if (el.offsetWidth === 0 || el.offsetHeight === 0) return;" +
                "  var text = el.textContent.trim().toLowerCase();" +
                "  if (text.length > 100) return;" +
                "  els.push({text: text, tag: el.tagName, index: els.length});" +
                "});" +
                "return JSON.stringify(els);";

            String json = (String) js.executeScript(script);
            org.json.JSONArray arr = new org.json.JSONArray(json);

            if (arr.length() == 0) return false;

            // Filter out blocklisted elements
            List<Integer> safeIndices = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                String text = arr.getJSONObject(i).optString("text", "");
                boolean blocked = false;
                for (String word : BLOCKLIST) {
                    if (text.contains(word)) { blocked = true; break; }
                }
                if (!blocked) safeIndices.add(i);
            }

            if (safeIndices.isEmpty()) return false;

            int targetIndex = safeIndices.get(random.nextInt(safeIndices.size()));

            // Click the element by index
            js.executeScript(
                "var els = [];" +
                "document.querySelectorAll('a, button, [role=\"button\"], [onclick], [class*=\"clickable\"]').forEach(function(el) {" +
                "  if (el.offsetWidth > 0 && el.offsetHeight > 0 && el.textContent.trim().length <= 100) els.push(el);" +
                "});" +
                "if (els[" + targetIndex + "]) {" +
                "  els[" + targetIndex + "].scrollIntoView({block: 'center'});" +
                "  els[" + targetIndex + "].click();" +
                "}");

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int fillVisibleInputs() {
        try {
            String script =
                "var inputs = [];" +
                "document.querySelectorAll('input, textarea').forEach(function(el) {" +
                "  if (el.offsetWidth === 0 || el.offsetHeight === 0) return;" +
                "  if (el.type === 'hidden' || el.type === 'checkbox' || el.type === 'radio' || el.type === 'file') return;" +
                "  if (el.disabled || el.readOnly) return;" +
                "  var label = el.getAttribute('aria-label') || el.placeholder || '';" +
                "  inputs.push({label: label, type: el.type || 'text', index: inputs.length});" +
                "});" +
                "return JSON.stringify(inputs);";

            String json = (String) js.executeScript(script);
            org.json.JSONArray arr = new org.json.JSONArray(json);
            int filled = 0;

            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject input = arr.getJSONObject(i);
                String label = input.optString("label", "");
                String type = input.optString("type", "text");

                // Generate appropriate data
                String value = random.nextBoolean()
                        ? SmartTestDataGenerator.smartValue(label, type)
                        : SmartTestDataGenerator.randomEdgeCase();

                // Use React nativeSetter to fill
                js.executeScript(
                    "var inputs = [];" +
                    "document.querySelectorAll('input, textarea').forEach(function(el) {" +
                    "  if (el.offsetWidth > 0 && el.offsetHeight > 0 && el.type !== 'hidden' && " +
                    "      el.type !== 'checkbox' && el.type !== 'radio' && el.type !== 'file' && " +
                    "      !el.disabled && !el.readOnly) inputs.push(el);" +
                    "});" +
                    "if (inputs[" + i + "]) {" +
                    "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                    "  setter.call(inputs[" + i + "], arguments[0]);" +
                    "  inputs[" + i + "].dispatchEvent(new Event('input', {bubbles: true}));" +
                    "  inputs[" + i + "].dispatchEvent(new Event('change', {bubbles: true}));" +
                    "}", value);

                filled++;
                pause(200);
            }

            return filled;
        } catch (Exception e) {
            return 0;
        }
    }

    private void fillRandomVisibleInput() {
        js.executeScript(
            "var inputs = [];" +
            "document.querySelectorAll('input, textarea').forEach(function(el) {" +
            "  if (el.offsetWidth > 0 && el.offsetHeight > 0 && el.type !== 'hidden' && " +
            "      el.type !== 'checkbox' && el.type !== 'radio' && el.type !== 'file' && " +
            "      !el.disabled && !el.readOnly) inputs.push(el);" +
            "});" +
            "if (inputs.length > 0) {" +
            "  var el = inputs[Math.floor(Math.random() * inputs.length)];" +
            "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "  setter.call(el, arguments[0]);" +
            "  el.dispatchEvent(new Event('input', {bubbles: true}));" +
            "  el.dispatchEvent(new Event('change', {bubbles: true}));" +
            "}", SmartTestDataGenerator.randomEdgeCase());
    }

    private boolean tryOpenCreateForm() {
        try {
            Boolean clicked = (Boolean) js.executeScript(
                "var btns = document.querySelectorAll('button, [role=\"button\"]');" +
                "for (var b of btns) {" +
                "  var t = b.textContent.trim().toLowerCase();" +
                "  if ((t.includes('create') || t.includes('add') || t.includes('new')) && b.offsetWidth > 0) {" +
                "    b.click(); return true;" +
                "  }" +
                "} return false;");
            return clicked != null && clicked;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // HEALTH CHECKS
    // ================================================================

    private boolean runHealthChecks(String context) {
        boolean healthy = true;

        // Check 1: Application error page
        try {
            Boolean isError = (Boolean) js.executeScript(
                "return document.body.innerText.includes('Application Error') || " +
                "       document.body.innerText.includes('something went wrong');");
            if (isError != null && isError) {
                logAnomaly("Application Error page detected " + context, driver.getCurrentUrl());
                healthy = false;
            }
        } catch (Exception ignored) {}

        // Check 2: Console errors
        try {
            List<String> errors = FlakinessPrevention.getConsoleErrors(driver);
            if (errors != null && !errors.isEmpty()) {
                for (String error : errors) {
                    if (error.contains("SEVERE") || error.contains("Error")) {
                        logAnomaly("Console error " + context + ": " + error.substring(0, Math.min(200, error.length())),
                                driver.getCurrentUrl());
                        healthy = false;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Check 3: Blank page
        try {
            String title = driver.getTitle();
            if (title == null || title.isEmpty()) {
                Boolean hasContent = (Boolean) js.executeScript(
                    "return document.body && document.body.innerText.trim().length > 10;");
                if (hasContent != null && !hasContent) {
                    logAnomaly("Blank page detected " + context, driver.getCurrentUrl());
                    healthy = false;
                }
            }
        } catch (Exception ignored) {}

        return healthy;
    }

    private void recoverIfNeeded() {
        try {
            // Dismiss any dialogs/backdrops
            js.executeScript(
                "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
                "  function(b) { b.style.display = 'none'; });" +
                "document.querySelectorAll('[role=\"dialog\"] button').forEach(" +
                "  function(b) { if (b.textContent.includes('Close') || b.textContent.includes('Cancel')) b.click(); });");
            pause(500);

            // If on error page, navigate to dashboard
            Boolean isError = (Boolean) js.executeScript(
                "return document.body.innerText.includes('Application Error');");
            if (isError != null && isError) {
                driver.get(AppConstants.BASE_URL);
                pause(3000);
                // Re-login if needed
                if (driver.findElements(By.id("email")).size() > 0) {
                    loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                    pause(2000);
                }
            }

            FlakinessPrevention.installNetworkInterceptor(driver);
            FlakinessPrevention.installConsoleErrorCapture(driver);
        } catch (Exception e) {
            System.out.println("[Monkey] Recovery failed: " + e.getMessage());
        }
    }

    // ================================================================
    // LOGIN & SITE SELECTION
    // ================================================================

    private void loginAndSelectSite() {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                driver.get(AppConstants.BASE_URL);
                pause(2000);

                new WebDriverWait(driver, Duration.ofSeconds(AppConstants.DEFAULT_TIMEOUT))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));

                loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                pause(2000);
                dashboardPage.waitForDashboard();

                // Select site
                By facilityInput = By.xpath("//input[@placeholder='Select facility']");
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(10))
                            .until(ExpectedConditions.presenceOfElementLocated(facilityInput));
                    driver.findElement(facilityInput).click();
                    pause(500);
                    new WebDriverWait(driver, Duration.ofSeconds(5))
                            .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@role='listbox']")));
                    js.executeScript("document.querySelectorAll('ul[role=\"listbox\"]').forEach(e => e.scrollTop=e.scrollHeight);");
                    pause(300);
                    By testSite = By.xpath("//li[@role='option'][contains(normalize-space(),'" + AppConstants.TEST_SITE_NAME + "')]");
                    driver.findElement(testSite).click();
                    pause(2000);
                } catch (Exception e) {
                    System.out.println("[Monkey] Site selection skipped: " + e.getMessage());
                }

                System.out.println("[Monkey] Login successful");
                return;
            } catch (Exception e) {
                System.out.println("[Monkey] Login attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) throw new RuntimeException("Login failed after " + maxRetries + " attempts", e);
                pause(3000);
            }
        }
    }

    // ================================================================
    // LOGGING & REPORTING
    // ================================================================

    private void logAction(String type, String target, String result) {
        MonkeyAction action = new MonkeyAction();
        action.type = type;
        action.target = target;
        action.result = result;
        action.timestampMs = System.currentTimeMillis();
        actionLog.add(action);
    }

    private void logAnomaly(String description, String pageUrl) {
        MonkeyAnomaly anomaly = new MonkeyAnomaly();
        anomaly.description = description;
        anomaly.pageUrl = pageUrl;
        anomaly.actionIndex = actionCount;
        anomaly.screenshotPath = ScreenshotUtil.captureScreenshot("Monkey_Anomaly_" + anomalies.size());
        anomalies.add(anomaly);
        System.out.println("[Monkey] ANOMALY: " + description);
    }

    private void logStep(String message) {
        System.out.println("[Monkey] " + message);
        ExtentReportManager.logInfo(message);
    }

    private void writeMonkeyReport() {
        try {
            org.json.JSONObject report = new org.json.JSONObject();
            report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            report.put("totalActions", actionLog.size());
            report.put("totalAnomalies", anomalies.size());
            report.put("maxActionsConfig", MAX_ACTIONS);
            report.put("maxDurationConfig", MAX_DURATION_SECONDS);

            // Action breakdown
            Map<String, Integer> actionCounts = new LinkedHashMap<>();
            for (MonkeyAction a : actionLog) {
                actionCounts.merge(a.type, 1, Integer::sum);
            }
            report.put("actionBreakdown", new org.json.JSONObject(actionCounts));

            // Anomalies
            org.json.JSONArray anomalyArr = new org.json.JSONArray();
            for (MonkeyAnomaly a : anomalies) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("description", a.description);
                obj.put("pageUrl", a.pageUrl);
                obj.put("actionIndex", a.actionIndex);
                anomalyArr.put(obj);
            }
            report.put("anomalies", anomalyArr);

            new java.io.File("test-output").mkdirs();
            try (FileWriter fw = new FileWriter("test-output/monkey-test-report.json")) {
                fw.write(report.toString(2));
            }
            System.out.println("[Monkey] Report: test-output/monkey-test-report.json");
        } catch (Exception e) {
            System.out.println("[Monkey] Failed to write report: " + e.getMessage());
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private boolean isTimedOut() {
        return (System.currentTimeMillis() - testStartTime) / 1000 >= MAX_DURATION_SECONDS;
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    // ================================================================
    // DATA CLASSES
    // ================================================================

    static class MonkeyAction {
        String type;
        String target;
        String result;
        long timestampMs;
    }

    static class MonkeyAnomaly {
        String description;
        String pageUrl;
        long actionIndex;
        String screenshotPath;
    }
}
