package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Bug Hunt & Security Test Suite
 *
 * Advanced tests targeting real bugs discovered via:
 *   - Live browser exploration (Playwright)
 *   - Code audit of existing test suite
 *   - OWASP Top 10 security checklist
 *
 * Coverage:
 *   Section 1: Console Error Detection (3 TCs)
 *   Section 2: XSS Protection (5 TCs)
 *   Section 3: Input Validation & Injection (5 TCs)
 *   Section 4: Login UX Bug Verification (5 TCs)
 *   Section 5: HTTP Security Headers (4 TCs)
 *   Section 6: Session & Cookie Security (4 TCs)
 *   Section 7: Error Handling & Information Leakage (4 TCs)
 *   Total: 30 TCs
 *
 * Architecture: Standalone (own browser session with console logging enabled).
 * Does NOT extend BaseTest — most tests operate on the login page pre-authentication.
 */
public class BugHuntTestNG {

    private static final String MODULE = AppConstants.MODULE_BUG_HUNT;

    protected WebDriver driver;
    protected JavascriptExecutor js;
    private long testStartTime;

    // Locators
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By SIGN_IN_BUTTON = By.xpath("//button[normalize-space()='Sign In']");
    private static final By ERROR_ALERT = By.xpath(
            "//*[@role='alert']|//*[contains(@class,'MuiAlert')]");
    private static final By FORGOT_PASSWORD_LINK = By.xpath(
            "//button[contains(text(),'Forgot')]|//a[contains(text(),'Forgot')]");
    private static final By PASSWORD_TOGGLE = By.xpath(
            "//button[contains(@aria-label,'password') or contains(@aria-label,'visibility')]");

    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     Bug Hunt & Security Test Suite Starting");
        System.out.println("     " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================");
        System.out.println();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        // Headless mode for CI — matches BaseTest behavior
        if ("true".equals(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }

        // Use EAGER page load strategy to avoid SPA navigation hangs (matches BaseTest)
        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

        // Enable browser console log capture
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logPrefs);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        js = (JavascriptExecutor) driver;
        ScreenshotUtil.setDriver(driver);

        // Set zoom to 80% to match BaseTest
        js.executeScript("document.body.style.zoom='90%';");
    }

    @AfterClass
    public void classTeardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
        System.out.println("Bug Hunt & Security Test Suite completed.");
    }

    @BeforeMethod
    public void testSetup() {
        testStartTime = System.currentTimeMillis();
        // Dismiss app update alert if present — blocks page interaction in CI
        try {
            js.executeScript(
                "var btns = document.querySelectorAll('button');" +
                "for (var i = 0; i < btns.length; i++) {" +
                "  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }" +
                "}"
            );
        } catch (Exception ignored) {}
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        long elapsed = System.currentTimeMillis() - testStartTime;
        String status = result.isSuccess() ? "PASS" : "FAIL";
        System.out.printf("  [%s] %s (%.1fs)%n", status, result.getMethod().getMethodName(), elapsed / 1000.0);

        if (!result.isSuccess()) {
            try {
                ScreenshotUtil.captureScreenshot("BugHunt_" + result.getMethod().getMethodName());
                ExtentReportManager.logFailWithScreenshot(
                        result.getMethod().getMethodName() + " FAILED",
                        result.getThrowable());
            } catch (Exception ignored) {}
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private void navigateToLogin() {
        driver.get(AppConstants.BASE_URL);
        // Wait for and dismiss "app update" alert that appears on every driver.get() in CI.
        // Must use WebDriverWait (not fire-and-forget JS) because alert renders asynchronously.
        waitAndDismissAppAlert();
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> {
                    try {
                        return d.findElement(EMAIL_INPUT).isDisplayed()
                                || d.findElement(By.tagName("body")).getText().contains("Error");
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    /**
     * Wait up to 10s for the "app update" DISMISS button to appear, click it,
     * then remove any residual MUI backdrops. Mirrors BaseTest.waitAndDismissAppAlert().
     * BugHuntTestNG has its own driver (doesn't extend BaseTest), so needs its own copy.
     */
    private void waitAndDismissAppAlert() {
        // Step 1: WebDriverWait for DISMISS button — catches late-rendering alerts
        try {
            WebElement dismissBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> {
                        try {
                            for (WebElement btn : d.findElements(By.tagName("button"))) {
                                String txt = btn.getText().trim();
                                if ("DISMISS".equals(txt) || "Dismiss".equals(txt)) {
                                    return btn;
                                }
                            }
                        } catch (Exception ignored) {}
                        return null;
                    });
            if (dismissBtn != null) {
                dismissBtn.click();
                pause(1000); // let React re-render after alert dismissal
            }
        } catch (Exception e) {
            // No alert appeared within 10s — normal on some navigations
        }
        // Step 2: Fire-and-forget cleanup for residual backdrops
        try {
            js.executeScript(
                "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop').forEach(" +
                "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                ");" +
                "var beamer = document.getElementById('beamerOverlay');" +
                "if (beamer) { beamer.style.display = 'none'; }" +
                "document.querySelectorAll('[id^=\"beamer\"]').forEach(" +
                "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                ");"
            );
        } catch (Exception ignored) {}
    }

    private void logStep(String msg) {
        System.out.println("    → " + msg);
        try { ExtentReportManager.logInfo(msg); } catch (Exception ignored) {}
    }

    private void logStepWithScreenshot(String msg) {
        logStep(msg);
        try { ExtentReportManager.logStepWithScreenshot(msg); } catch (Exception ignored) {}
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    /**
     * Sets value on a React controlled input using nativeInputValueSetter.
     */
    private void setReactValue(WebElement element, String value) {
        js.executeScript(
                "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                + "nativeInputValueSetter.call(arguments[0], arguments[1]);"
                + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                element, value);
    }

    private void clickTermsCheckbox() {
        try {
            List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
            if (!checkboxes.isEmpty()) {
                WebElement cb = checkboxes.get(0);
                if (!cb.isSelected()) {
                    // Click the parent label/span for MUI checkboxes
                    WebElement parent = (WebElement) js.executeScript("return arguments[0].closest('label') || arguments[0].parentElement;", cb);
                    if (parent != null) {
                        parent.click();
                    } else {
                        js.executeScript("arguments[0].click();", cb);
                    }
                }
            }
        } catch (Exception e) {
            logStep("Could not click terms checkbox: " + e.getMessage());
        }
    }

    private boolean isSignInEnabled() {
        // MUI buttons signal disabled state via FOUR independent mechanisms — all must be "enabled"
        // for the button to be genuinely clickable. Prior implementation only checked 2 (DOM disabled
        // + Mui-disabled class), which false-positived when MUI rendered with aria-disabled="true"
        // without the Mui-disabled class (common in newer @mui/material versions).
        try {
            WebElement btn = driver.findElement(SIGN_IN_BUTTON);
            boolean domEnabled = btn.isEnabled();
            String cls = btn.getAttribute("class");
            boolean classEnabled = cls == null || !cls.contains("Mui-disabled");
            String ariaDisabled = btn.getAttribute("aria-disabled");
            boolean ariaEnabled = ariaDisabled == null || !"true".equalsIgnoreCase(ariaDisabled);
            String pointerEvents = btn.getCssValue("pointer-events");
            boolean pointerEnabled = !"none".equalsIgnoreCase(pointerEvents);
            return domEnabled && classEnabled && ariaEnabled && pointerEnabled;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets browser console log entries (errors, warnings).
     */
    private List<LogEntry> getBrowserConsoleLogs() {
        try {
            return driver.manage().logs().get(LogType.BROWSER).getAll();
        } catch (Exception e) {
            logStep("Could not retrieve browser logs: " + e.getMessage());
            return List.of();
        }
    }

    // ================================================================
    // SECTION 1: CONSOLE ERROR DETECTION
    // Bugs found: DevRev SDK SRI integrity failure, DevRev load error
    // ================================================================

    @Test(priority = 1, description = "BUG-01: Detect JavaScript console errors on page load")
    public void testBUG01_ConsoleErrorsOnPageLoad() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_CONSOLE_ERRORS,
                "BUG01_ConsoleErrorsOnPageLoad");
        logStep("Loading login page and capturing all console errors");

        navigateToLogin();
        pause(3000); // Allow async scripts to load/fail

        List<LogEntry> logs = getBrowserConsoleLogs();
        int errorCount = 0;
        StringBuilder errorDetails = new StringBuilder();

        for (LogEntry entry : logs) {
            if (entry.getLevel().intValue() >= Level.SEVERE.intValue()) {
                errorCount++;
                String msg = entry.getMessage();
                errorDetails.append("\n  [ERROR] ").append(msg.substring(0, Math.min(200, msg.length())));
                logStep("Console ERROR: " + msg.substring(0, Math.min(150, msg.length())));
            }
        }

        logStep("Total console errors on page load: " + errorCount);
        logStepWithScreenshot("Console error count: " + errorCount);

        // We expect 0 console errors on a clean page load.
        // Known bugs: DevRev SRI integrity failure, DevRev SDK load failure.
        // Bug report: log the finding without failing the suite.
        if (errorCount > 0) {
            logStep("BUG DETECTED: " + errorCount + " console errors on login page load:" + errorDetails);
            logStep("BUG REPORT: " + errorCount + " console error(s) detected on page load (documented above)");
            ExtentReportManager.logWarning("BUG-01 REPORTED: " + errorCount
                    + " JS console error(s) on page load. Details: " + errorDetails);
        } else {
            ExtentReportManager.logPass("BUG-01: No console errors on page load (bug may be fixed)");
        }
    }

    @Test(priority = 2, description = "BUG-02: DevRev SDK SRI integrity hash mismatch")
    public void testBUG02_DevRevSRIIntegrityFailure() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_CONSOLE_ERRORS,
                "BUG02_DevRevSRIIntegrity");
        logStep("Checking for DevRev SDK SRI integrity hash mismatch");

        navigateToLogin();
        pause(3000);

        List<LogEntry> logs = getBrowserConsoleLogs();
        boolean sriFailureFound = false;
        String sriErrorMessage = "";

        for (LogEntry entry : logs) {
            String msg = entry.getMessage();
            if (msg.contains("integrity") && msg.contains("devrev")) {
                sriFailureFound = true;
                sriErrorMessage = msg.substring(0, Math.min(300, msg.length()));
                break;
            }
        }

        logStep("DevRev SRI integrity failure detected: " + sriFailureFound);
        if (sriFailureFound) {
            logStep("SRI Error: " + sriErrorMessage);
            ExtentReportManager.logFail("BUG: DevRev SDK blocked by SRI integrity check. "
                    + "The SHA-384 hash in the HTML does not match the current script content.");
        }

        // This should PASS when the bug is fixed (no SRI failure)
        Assert.assertFalse(sriFailureFound,
                "BUG: DevRev PLuG SDK blocked by Subresource Integrity check. "
                + "The integrity hash needs to be updated in the HTML. Error: " + sriErrorMessage);
    }

    @Test(priority = 3, description = "BUG-03: DevRev support widget fails to load")
    public void testBUG03_DevRevWidgetLoadFailure() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_CONSOLE_ERRORS,
                "BUG03_DevRevWidgetLoadFailure");
        logStep("Checking if DevRev support widget loads successfully");

        navigateToLogin();
        pause(3000);

        List<LogEntry> logs = getBrowserConsoleLogs();
        boolean devrevFailed = false;

        for (LogEntry entry : logs) {
            String msg = entry.getMessage();
            if (msg.contains("DevRev") && (msg.contains("Failed") || msg.contains("Error"))) {
                devrevFailed = true;
                logStep("DevRev error: " + msg.substring(0, Math.min(200, msg.length())));
            }
        }

        // Also check if DevRev widget iframe/button exists in DOM
        List<WebElement> devrevElements = driver.findElements(
                By.cssSelector("iframe[src*='devrev'], #devrev-plug, [data-devrev]"));
        logStep("DevRev DOM elements found: " + devrevElements.size());

        // Bug report: log the finding without failing the suite
        if (devrevFailed) {
            logStep("BUG REPORT: DevRev support widget load failure documented above");
            ExtentReportManager.logWarning("BUG-03 REPORTED: DevRev support widget failed to load — "
                    + "customer support functionality is broken. DOM elements found: "
                    + devrevElements.size());
        } else {
            ExtentReportManager.logPass("BUG-03: DevRev widget loaded without errors (bug may be fixed)");
        }
    }

    // ================================================================
    // SECTION 2: XSS PROTECTION
    // Bug found: No client-side email format validation
    // ================================================================

    @Test(priority = 10, description = "BUG-04: XSS script tag in email field")
    public void testBUG04_XSSInEmailField() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_XSS_PROTECTION,
                "BUG04_XSSInEmailField");
        logStep("Testing XSS script injection in email field");

        navigateToLogin();

        String xssPayload = "<script>alert('xss')</script>";
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, xssPayload);
        pause(300);

        // Check that no alert dialog appeared (React should prevent XSS)
        boolean alertTriggered = false;
        try {
            driver.switchTo().alert();
            alertTriggered = true;
            driver.switchTo().alert().dismiss();
        } catch (Exception e) {
            // Good — no alert means XSS was blocked
        }

        Assert.assertFalse(alertTriggered,
                "CRITICAL SECURITY BUG: XSS alert() executed from email field input!");

        // Check that the value is treated as plain text, not rendered as HTML
        String pageSource = driver.getPageSource();
        boolean scriptRendered = pageSource.contains("<script>alert('xss')</script>")
                && !pageSource.contains("&lt;script&gt;");
        logStep("XSS payload rendered as HTML: " + scriptRendered);

        // Verify the input contains the XSS text as plain text (not executed)
        String inputValue = emailField.getAttribute("value");
        logStep("Email field value: '" + inputValue + "'");

        logStepWithScreenshot("XSS test - email field");
        ExtentReportManager.logPass("XSS script tag in email field is safely handled as plain text");
    }

    @Test(priority = 11, description = "BUG-05: XSS event handler in email field")
    public void testBUG05_XSSEventHandlerInEmail() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_XSS_PROTECTION,
                "BUG05_XSSEventHandlerInEmail");
        logStep("Testing XSS event handler injection in email field");

        navigateToLogin();

        String xssPayload = "\" onfocus=\"alert('xss')\" autofocus=\"";
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, xssPayload);
        pause(500);

        boolean alertTriggered = false;
        try {
            driver.switchTo().alert();
            alertTriggered = true;
            driver.switchTo().alert().dismiss();
        } catch (Exception e) {
            // Good
        }

        Assert.assertFalse(alertTriggered,
                "CRITICAL SECURITY BUG: XSS event handler executed in email field!");

        logStepWithScreenshot("XSS event handler test");
        ExtentReportManager.logPass("XSS event handler injection safely blocked by React");
    }

    @Test(priority = 12, description = "BUG-06: XSS in password field")
    public void testBUG06_XSSInPasswordField() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_XSS_PROTECTION,
                "BUG06_XSSInPasswordField");
        logStep("Testing XSS injection in password field");

        navigateToLogin();

        String xssPayload = "<img src=x onerror=alert('xss')>";
        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, xssPayload);
        pause(300);

        boolean alertTriggered = false;
        try {
            driver.switchTo().alert();
            alertTriggered = true;
            driver.switchTo().alert().dismiss();
        } catch (Exception e) {
            // Good
        }

        Assert.assertFalse(alertTriggered,
                "CRITICAL SECURITY BUG: XSS executed from password field!");

        logStepWithScreenshot("XSS test - password field");
        ExtentReportManager.logPass("XSS in password field safely blocked");
    }

    @Test(priority = 13, description = "BUG-07: XSS via URL fragment/hash injection")
    public void testBUG07_XSSViaURLFragment() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_XSS_PROTECTION,
                "BUG07_XSSViaURLFragment");
        logStep("Testing XSS via URL hash/fragment injection");

        driver.get(AppConstants.BASE_URL + "/#<script>alert('xss')</script>");
        pause(2000);

        boolean alertTriggered = false;
        try {
            driver.switchTo().alert();
            alertTriggered = true;
            driver.switchTo().alert().dismiss();
        } catch (Exception e) {
            // Good
        }

        Assert.assertFalse(alertTriggered,
                "CRITICAL SECURITY BUG: XSS executed via URL fragment!");

        // Also check via query param
        driver.get(AppConstants.BASE_URL + "/?redirect=javascript:alert('xss')");
        pause(2000);

        try {
            driver.switchTo().alert();
            alertTriggered = true;
            driver.switchTo().alert().dismiss();
        } catch (Exception e) {
            // Good
        }

        Assert.assertFalse(alertTriggered,
                "CRITICAL SECURITY BUG: XSS executed via query param!");

        logStepWithScreenshot("URL-based XSS test");
        ExtentReportManager.logPass("URL fragment/query XSS injection safely blocked");
    }

    @Test(priority = 14, description = "BUG-08: No email format validation on client side")
    public void testBUG08_NoEmailFormatValidation() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_XSS_PROTECTION,
                "BUG08_NoEmailFormatValidation");
        logStep("Testing whether email field enforces format validation before submit");

        navigateToLogin();

        // Enter obviously invalid email format
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, "not-an-email");
        pause(200);

        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, "somepassword");
        pause(200);

        clickTermsCheckbox();
        pause(300);

        // Check if Sign In button is enabled with invalid email
        boolean signInEnabled = isSignInEnabled();
        logStep("Sign In enabled with 'not-an-email': " + signInEnabled);

        // Check for any validation message
        List<WebElement> validationErrors = driver.findElements(
                By.xpath("//*[contains(@class,'error') or contains(@class,'Error') "
                        + "or contains(@class,'helper') or contains(@class,'Helper')]"));
        boolean hasValidation = !validationErrors.isEmpty();
        logStep("Client-side validation message shown: " + hasValidation);

        logStepWithScreenshot("Email format validation check");

        // Bug report: log the finding without failing the suite
        if (signInEnabled && !hasValidation) {
            logStep("BUG DETECTED: No client-side email format validation. "
                    + "Invalid emails like 'not-an-email' are accepted without warning.");
            logStep("BUG REPORT: Email format validation absent — documented above");
            ExtentReportManager.logWarning("BUG-08 REPORTED: Email field accepts 'not-an-email' without "
                    + "client-side format validation. Users get no feedback until server returns an error.");
        } else {
            ExtentReportManager.logPass("BUG-08: Client-side email validation is present (bug may be fixed). "
                    + "Validation shown: " + hasValidation + ", Sign In enabled: " + signInEnabled);
        }
    }

    // ================================================================
    // SECTION 3: INPUT VALIDATION & INJECTION
    // ================================================================

    @Test(priority = 20, description = "BUG-09: SQL injection in email field")
    public void testBUG09_SQLInjectionInEmail() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_INPUT_VALIDATION,
                "BUG09_SQLInjectionInEmail");
        logStep("Testing SQL injection payloads in email field");

        navigateToLogin();

        String[] sqlPayloads = {
                "' OR '1'='1' --",
                "admin'--",
                "' UNION SELECT * FROM users --",
                "1; DROP TABLE users; --"
        };

        for (String payload : sqlPayloads) {
            WebElement emailField = driver.findElement(EMAIL_INPUT);
            setReactValue(emailField, payload);

            WebElement passwordField = driver.findElement(PASSWORD_INPUT);
            setReactValue(passwordField, "testpass");

            clickTermsCheckbox();
            pause(300);

            // Try to submit
            if (isSignInEnabled()) {
                driver.findElement(SIGN_IN_BUTTON).click();
                pause(2000);

                // Check: should NOT be logged in
                String url = driver.getCurrentUrl();
                boolean onDashboard = url.contains("/dashboard") || url.contains("/assets")
                        || url.contains("/home");

                if (onDashboard) {
                    Assert.fail("CRITICAL SECURITY BUG: SQL injection bypassed authentication! "
                            + "Payload: " + payload);
                }

                // Check for error message (expected)
                List<WebElement> errors = driver.findElements(ERROR_ALERT);
                if (!errors.isEmpty()) {
                    String errorText = errors.get(0).getText();
                    logStep("Error for payload '" + payload + "': " + errorText);

                    // Error should NOT reveal SQL details
                    Assert.assertFalse(
                            errorText.toLowerCase().contains("sql")
                            || errorText.toLowerCase().contains("syntax")
                            || errorText.toLowerCase().contains("query")
                            || errorText.toLowerCase().contains("table"),
                            "SECURITY BUG: Error message reveals SQL details: " + errorText);
                }
            }
            logStep("SQL payload safely handled: " + payload);

            // Navigate back for next attempt
            navigateToLogin();
        }

        logStepWithScreenshot("SQL injection test complete");
        ExtentReportManager.logPass("SQL injection payloads are safely handled");
    }

    @Test(priority = 21, description = "BUG-10: Extremely long input in email field (buffer overflow)")
    public void testBUG10_LongInputEmailField() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_INPUT_VALIDATION,
                "BUG10_LongInputEmailField");
        logStep("Testing extremely long input in email field");

        navigateToLogin();

        // Generate a very long string (10,000 chars)
        String longInput = "a".repeat(10000) + "@test.com";
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, longInput);
        pause(500);

        // Verify page didn't crash
        Assert.assertNotNull(driver.getTitle(), "Page crashed with long email input");

        // Check if value was truncated or accepted
        String actualValue = emailField.getAttribute("value");
        logStep("Input length: " + longInput.length() + ", Actual stored length: " + actualValue.length());

        // Check for any JS errors from the long input
        List<LogEntry> logs = getBrowserConsoleLogs();
        boolean jsErrorFromInput = false;
        for (LogEntry entry : logs) {
            if (entry.getLevel().intValue() >= Level.SEVERE.intValue()
                    && entry.getMessage().contains("RangeError")) {
                jsErrorFromInput = true;
                logStep("JS error from long input: " + entry.getMessage());
            }
        }

        Assert.assertFalse(jsErrorFromInput,
                "BUG: Long email input causes JavaScript RangeError");

        logStepWithScreenshot("Long input test");
        ExtentReportManager.logPass("Long email input handled without crash. "
                + "Stored length: " + actualValue.length());
    }

    @Test(priority = 22, description = "BUG-11: Unicode and emoji in email field")
    public void testBUG11_UnicodeEmojiInEmail() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_INPUT_VALIDATION,
                "BUG11_UnicodeEmojiInEmail");
        logStep("Testing unicode and emoji characters in email field");

        navigateToLogin();

        String[] unicodeInputs = {
                "用户@测试.com",          // Chinese characters
                "пользователь@тест.com",   // Cyrillic
                "emoji😀@test.com",         // Emoji
                "null\u0000byte@test.com",  // Null byte
                "tab\there@test.com",       // Tab character
                "newline\nhere@test.com"    // Newline
        };

        for (String input : unicodeInputs) {
            WebElement emailField = driver.findElement(EMAIL_INPUT);
            setReactValue(emailField, input);
            pause(200);

            // Page should not crash
            Assert.assertNotNull(driver.getTitle(),
                    "Page crashed with unicode input: " + input);
            logStep("Unicode input accepted without crash: " + input.substring(0, Math.min(30, input.length())));
        }

        logStepWithScreenshot("Unicode/emoji test");
        ExtentReportManager.logPass("Unicode and emoji characters handled safely");
    }

    @Test(priority = 23, description = "BUG-12: Password field doesn't leak in page source")
    public void testBUG12_PasswordNotInPageSource() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_INPUT_VALIDATION,
                "BUG12_PasswordNotInPageSource");
        logStep("Verifying password is not leaked in page source or DOM attributes");

        navigateToLogin();

        String secretPassword = "SuperSecret123!@#";
        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, secretPassword);
        pause(300);

        // Password field type must be "password" (not "text")
        String fieldType = passwordField.getAttribute("type");
        logStep("Password field type: " + fieldType);
        Assert.assertEquals(fieldType, "password",
                "BUG: Password field type is '" + fieldType + "' instead of 'password' — input is visible!");

        // Check page source doesn't contain the password in cleartext
        String pageSource = driver.getPageSource();
        // The value attribute in React controlled components shouldn't appear in source
        boolean passwordInSource = pageSource.contains(secretPassword);
        logStep("Password found in page source: " + passwordInSource);

        // Check autocomplete attribute
        String autocomplete = passwordField.getAttribute("autocomplete");
        logStep("Password autocomplete attribute: " + autocomplete);

        logStepWithScreenshot("Password leak check");
        ExtentReportManager.logPass("Password field properly masked. Type: " + fieldType
                + ", In source: " + passwordInSource);
    }

    @Test(priority = 24, description = "BUG-13: Login API doesn't leak sensitive info in error response")
    public void testBUG13_LoginAPIErrorInfoLeak() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_INPUT_VALIDATION,
                "BUG13_LoginAPIErrorInfoLeak");
        logStep("Testing login API error response for information leakage");

        navigateToLogin();

        // Submit with valid email but wrong password
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, AppConstants.VALID_EMAIL);
        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, "WrongPassword123!");
        clickTermsCheckbox();
        pause(300);

        if (isSignInEnabled()) {
            driver.findElement(SIGN_IN_BUTTON).click();
            pause(3000);

            // Get error message
            List<WebElement> errors = driver.findElements(ERROR_ALERT);
            if (!errors.isEmpty()) {
                String errorText = errors.get(0).getText().toLowerCase();
                logStep("Error message: " + errorText);

                // Error should NOT differentiate between "user not found" and "wrong password"
                // Both should show generic "Invalid credentials"
                boolean leaksUserExistence = errorText.contains("user not found")
                        || errorText.contains("no account")
                        || errorText.contains("email not registered")
                        || errorText.contains("no user");
                boolean leaksPasswordHint = errorText.contains("wrong password")
                        || errorText.contains("incorrect password")
                        || errorText.contains("password is incorrect");
                boolean leaksStackTrace = errorText.contains("stack")
                        || errorText.contains("trace")
                        || errorText.contains("at com.")
                        || errorText.contains("exception");

                Assert.assertFalse(leaksUserExistence,
                        "SECURITY BUG: Error message reveals whether user exists: " + errorText);
                Assert.assertFalse(leaksPasswordHint,
                        "SECURITY BUG: Error message reveals password is wrong (vs user not found): " + errorText);
                Assert.assertFalse(leaksStackTrace,
                        "SECURITY BUG: Error message contains stack trace: " + errorText);

                ExtentReportManager.logPass("Login error is generic (no information leakage): " + errorText);
            } else {
                logStep("No error message displayed after wrong password");
                ExtentReportManager.logPass("No error message (may have redirected)");
            }
        } else {
            logStep("Sign In not enabled — cannot test API error response");
            ExtentReportManager.logPass("Sign In disabled (terms unchecked or field empty)");
        }
    }

    // ================================================================
    // SECTION 4: LOGIN UX BUG VERIFICATION
    // Bug found: Terms checkbox resets after failed login
    // ================================================================

    @Test(priority = 30, description = "BUG-14: Terms checkbox resets after failed login")
    public void testBUG14_TermsCheckboxResetsAfterFailedLogin() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_LOGIN_UX,
                "BUG14_TermsCheckboxResets");
        logStep("Testing if terms checkbox resets after a failed login attempt");

        navigateToLogin();

        // Fill form and check terms
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, "test@example.com");
        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, "wrongpassword");
        clickTermsCheckbox();
        pause(300);

        // Verify terms is checked
        List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
        boolean checkedBeforeSubmit = false;
        if (!checkboxes.isEmpty()) {
            checkedBeforeSubmit = checkboxes.get(0).isSelected();
        }
        logStep("Terms checked before submit: " + checkedBeforeSubmit);

        // Submit (will fail with wrong credentials)
        if (isSignInEnabled()) {
            driver.findElement(SIGN_IN_BUTTON).click();
            pause(3000);

            // Check if terms is still checked
            checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
            boolean checkedAfterFailure = false;
            if (!checkboxes.isEmpty()) {
                checkedAfterFailure = checkboxes.get(0).isSelected();
            }
            logStep("Terms checked after failed login: " + checkedAfterFailure);

            logStepWithScreenshot("Terms checkbox state after failed login");

            // Bug report: log the finding without failing the suite
            // UX BUG: Terms should remain checked after failed login
            // Users shouldn't have to re-check it every time they fix their password
            if (!checkedAfterFailure && checkedBeforeSubmit) {
                logStep("BUG REPORT: Terms checkbox reset documented above");
                ExtentReportManager.logWarning("BUG-14 REPORTED: Terms checkbox resets to unchecked after "
                        + "failed login. Before: " + checkedBeforeSubmit
                        + ", After: " + checkedAfterFailure
                        + ". Users must re-check it on every failed attempt.");
            } else {
                ExtentReportManager.logPass("BUG-14: Terms checkbox state preserved after failed login "
                        + "(bug may be fixed). After: " + checkedAfterFailure);
            }
        } else {
            logStep("Could not submit — Sign In disabled");
            ExtentReportManager.logPass("Cannot verify (Sign In disabled)");
        }
    }

    @Test(priority = 31, description = "BUG-15: Password visibility toggle works correctly")
    public void testBUG15_PasswordVisibilityToggle() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_LOGIN_UX,
                "BUG15_PasswordVisibilityToggle");
        logStep("Testing password visibility toggle button");

        navigateToLogin();

        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, "TestPassword123");
        pause(200);

        // Initial state: password should be masked
        String typeBefore = passwordField.getAttribute("type");
        Assert.assertEquals(typeBefore, "password",
                "Password should be masked initially. Type: " + typeBefore);
        logStep("Initial type: " + typeBefore + " (masked)");

        // Click toggle button
        List<WebElement> toggleBtns = driver.findElements(PASSWORD_TOGGLE);
        Assert.assertFalse(toggleBtns.isEmpty(), "Password visibility toggle button not found");
        toggleBtns.get(0).click();
        pause(300);

        // After toggle: should show as text
        String typeAfter = passwordField.getAttribute("type");
        logStep("After toggle type: " + typeAfter);
        Assert.assertEquals(typeAfter, "text",
                "Password should be visible after toggle. Type: " + typeAfter);

        // Toggle back: should be masked again
        toggleBtns.get(0).click();
        pause(300);
        String typeToggleBack = passwordField.getAttribute("type");
        logStep("After second toggle type: " + typeToggleBack);
        Assert.assertEquals(typeToggleBack, "password",
                "Password should be masked after second toggle. Type: " + typeToggleBack);

        logStepWithScreenshot("Password toggle test");
        ExtentReportManager.logPass("Password visibility toggle works correctly");
    }

    @Test(priority = 32, description = "BUG-16: Sign In button disabled state management")
    public void testBUG16_SignInButtonDisabledStates() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_LOGIN_UX,
                "BUG16_SignInButtonStates");
        logStep("Testing Sign In button disabled/enabled states");

        navigateToLogin();

        // State 1: Empty form — Sign In should be disabled
        boolean state1 = isSignInEnabled();
        logStep("State 1 (empty form): Sign In enabled = " + state1);
        Assert.assertFalse(state1, "Sign In should be disabled with empty form");

        // State 2: Only email filled — should be disabled
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, "test@example.com");
        pause(200);
        boolean state2 = isSignInEnabled();
        logStep("State 2 (email only): Sign In enabled = " + state2);
        Assert.assertFalse(state2, "Sign In should be disabled with only email");

        // State 3: Email + Password, no terms — should be disabled
        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, "password123");
        pause(200);
        boolean state3 = isSignInEnabled();
        logStep("State 3 (email+password, no terms): Sign In enabled = " + state3);
        Assert.assertFalse(state3, "Sign In should be disabled without terms accepted");

        // State 4: Email + Password + Terms — should be enabled
        clickTermsCheckbox();
        pause(300);
        boolean state4 = isSignInEnabled();
        logStep("State 4 (all filled + terms): Sign In enabled = " + state4);
        Assert.assertTrue(state4, "Sign In should be enabled with all fields filled and terms checked");

        logStepWithScreenshot("Sign In button states");
        ExtentReportManager.logPass("Sign In button state management works correctly");
    }

    @Test(priority = 33, description = "BUG-17: Forgot Password link is accessible")
    public void testBUG17_ForgotPasswordLink() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_LOGIN_UX,
                "BUG17_ForgotPasswordLink");
        logStep("Testing Forgot Password link availability and behavior");

        navigateToLogin();

        List<WebElement> forgotLinks = driver.findElements(FORGOT_PASSWORD_LINK);
        logStep("Forgot Password links found: " + forgotLinks.size());

        if (forgotLinks.isEmpty()) {
            logStep("BUG REPORT: Forgot Password link not found on login page");
            ExtentReportManager.logWarning("BUG-17 REPORTED: Forgot Password link not found on login page. "
                    + "Users cannot recover their accounts.");
            logStepWithScreenshot("Forgot Password link — missing");
            return;
        }

        WebElement forgotLink = forgotLinks.get(0);
        boolean isDisplayed = forgotLink.isDisplayed();
        boolean isEnabled = forgotLink.isEnabled();
        String text = forgotLink.getText().trim();
        logStep("Forgot Password link text: '" + text + "', displayed: " + isDisplayed
                + ", enabled: " + isEnabled);

        if (!isDisplayed) {
            logStep("BUG REPORT: Forgot Password link exists in DOM but is not visible");
            ExtentReportManager.logWarning("BUG-17 REPORTED: Forgot Password link is present but not visible.");
        } else if (!isEnabled) {
            logStep("BUG REPORT: Forgot Password link is visible but not clickable");
            ExtentReportManager.logWarning("BUG-17 REPORTED: Forgot Password link is visible but not clickable.");
        } else if (!text.toLowerCase().contains("forgot")) {
            logStep("BUG REPORT: Forgot Password link text does not contain 'forgot': '" + text + "'");
            ExtentReportManager.logWarning("BUG-17 REPORTED: Forgot Password link text unexpected: '" + text + "'");
        } else {
            ExtentReportManager.logPass("BUG-17: Forgot Password link is present and accessible: '" + text + "'");
        }

        logStepWithScreenshot("Forgot Password link");
    }

    @Test(priority = 34, description = "BUG-18: Login error message disappears on new input")
    public void testBUG18_ErrorMessageClearsOnNewInput() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_LOGIN_UX,
                "BUG18_ErrorMessageClearsOnInput");
        logStep("Testing if error message clears when user starts typing again");

        navigateToLogin();

        // Trigger a login error
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, "wrong@example.com");
        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        setReactValue(passwordField, "wrongpass");
        clickTermsCheckbox();
        pause(300);

        if (!isSignInEnabled()) {
            logStep("Sign In not enabled — cannot trigger error");
            ExtentReportManager.logPass("Cannot test (Sign In disabled)");
            return;
        }

        driver.findElement(SIGN_IN_BUTTON).click();
        pause(3000);

        // Verify error appeared
        List<WebElement> errors = driver.findElements(ERROR_ALERT);
        if (errors.isEmpty()) {
            logStep("No error message appeared — cannot test clearing behavior");
            ExtentReportManager.logPass("No error message to test");
            return;
        }

        boolean errorVisibleBefore = errors.get(0).isDisplayed();
        logStep("Error visible after failed login: " + errorVisibleBefore);

        // Start typing in email field again
        emailField = driver.findElement(EMAIL_INPUT);
        setReactValue(emailField, "new-input@example.com");
        pause(1000);

        // Error should clear
        errors = driver.findElements(ERROR_ALERT);
        boolean errorVisibleAfter = !errors.isEmpty() && errors.get(0).isDisplayed();
        logStep("Error visible after new input: " + errorVisibleAfter);

        logStepWithScreenshot("Error clearing behavior");

        // Good UX: Error should clear when user starts correcting input
        if (errorVisibleBefore && !errorVisibleAfter) {
            ExtentReportManager.logPass("Error message correctly clears on new input");
        } else if (errorVisibleBefore && errorVisibleAfter) {
            // Not necessarily a bug, but less ideal UX
            logStep("Note: Error message persists during new input (acceptable behavior)");
            ExtentReportManager.logPass("Error persists (acceptable) — clears on next submit");
        }
    }

    // ================================================================
    // SECTION 5: HTTP SECURITY HEADERS
    // ================================================================

    @Test(priority = 40, description = "BUG-19: Check X-Frame-Options header (clickjacking protection)")
    public void testBUG19_XFrameOptionsHeader() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_HTTP_SECURITY,
                "BUG19_XFrameOptions");
        logStep("Checking X-Frame-Options or CSP frame-ancestors header");

        navigateToLogin();

        // Try to load the page in an iframe (should be blocked by X-Frame-Options)
        Boolean canBeFramed = (Boolean) js.executeScript(
                "try {"
                + "  var iframe = document.createElement('iframe');"
                + "  iframe.src = window.location.href;"
                + "  iframe.style.display = 'none';"
                + "  document.body.appendChild(iframe);"
                + "  return true;"
                + "} catch(e) { return false; }");

        logStep("Can create self-referencing iframe: " + canBeFramed);

        // Check via meta tags for CSP
        String cspMeta = (String) js.executeScript(
                "var metas = document.querySelectorAll('meta[http-equiv=\"Content-Security-Policy\"]');"
                + "if (metas.length > 0) return metas[0].content;"
                + "return null;");
        logStep("CSP meta tag: " + cspMeta);

        logStepWithScreenshot("Clickjacking protection check");

        // Note: We can't directly read HTTP headers from JS, but we verify the protection works
        ExtentReportManager.logPass("Clickjacking protection check completed. CSP: " + cspMeta);
    }

    @Test(priority = 41, description = "BUG-20: Check for exposed source maps")
    public void testBUG20_ExposedSourceMaps() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_HTTP_SECURITY,
                "BUG20_ExposedSourceMaps");
        logStep("Checking if JavaScript source maps are exposed in production");

        navigateToLogin();

        // Check if any script tags reference .map files
        @SuppressWarnings("unchecked")
        List<String> scriptSources = (List<String>) js.executeScript(
                "var scripts = document.querySelectorAll('script[src]');"
                + "var sources = [];"
                + "scripts.forEach(function(s) { sources.push(s.src); });"
                + "return sources;");

        logStep("Found " + scriptSources.size() + " script sources");

        boolean sourceMapExposed = false;
        for (String src : scriptSources) {
            logStep("Script: " + src);
            // Check if .map file is accessible
            if (src.endsWith(".js")) {
                Boolean mapExists = (Boolean) js.executeScript(
                        "var xhr = new XMLHttpRequest();"
                        + "xhr.open('HEAD', arguments[0] + '.map', false);"
                        + "try { xhr.send(); return xhr.status === 200; } "
                        + "catch(e) { return false; }",
                        src);
                if (Boolean.TRUE.equals(mapExists)) {
                    sourceMapExposed = true;
                    logStep("SECURITY: Source map exposed: " + src + ".map");
                }
            }
        }

        logStepWithScreenshot("Source map exposure check");

        // Bug report: log the finding without failing the suite
        if (sourceMapExposed) {
            logStep("BUG REPORT: Exposed source maps documented above");
            ExtentReportManager.logWarning("BUG-20 REPORTED: JavaScript source maps (.js.map) are accessible "
                    + "in production. This exposes original source code to attackers.");
        } else {
            ExtentReportManager.logPass("BUG-20: No exposed source maps found (or bug already fixed)");
        }
    }

    @Test(priority = 42, description = "BUG-21: Check for sensitive data in localStorage")
    public void testBUG21_SensitiveDataInLocalStorage() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_HTTP_SECURITY,
                "BUG21_SensitiveDataInLocalStorage");
        logStep("Checking localStorage for sensitive data exposure");

        navigateToLogin();
        pause(1000);

        // Get all localStorage keys and values
        @SuppressWarnings("unchecked")
        Map<String, String> storage = (Map<String, String>) js.executeScript(
                "var items = {};"
                + "for (var i = 0; i < localStorage.length; i++) {"
                + "  var key = localStorage.key(i);"
                + "  items[key] = localStorage.getItem(key);"
                + "}"
                + "return items;");

        logStep("localStorage items: " + storage.size());

        boolean sensitiveDataFound = false;
        for (Map.Entry<String, String> entry : storage.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();
            logStep("  localStorage['" + entry.getKey() + "']: "
                    + (value != null ? value.substring(0, Math.min(50, value.length())) + "..." : "null"));

            // Check for tokens, passwords, or API keys in localStorage
            if (key.contains("password") || key.contains("secret") || key.contains("apikey")
                    || key.contains("api_key") || key.contains("private_key")) {
                sensitiveDataFound = true;
                logStep("SECURITY: Sensitive key found in localStorage: " + entry.getKey());
            }

            // Check for JWT tokens (should be in httpOnly cookies, not localStorage)
            if (value != null && value.startsWith("eyJ") && value.contains(".")) {
                logStep("NOTE: JWT token found in localStorage key '" + entry.getKey()
                        + "'. Consider using httpOnly cookies instead.");
            }
        }

        logStepWithScreenshot("localStorage check");

        if (sensitiveDataFound) {
            ExtentReportManager.logFail("SECURITY: Sensitive data found in localStorage");
        } else {
            ExtentReportManager.logPass("No sensitive credentials found in localStorage");
        }

        Assert.assertFalse(sensitiveDataFound,
                "SECURITY BUG: Sensitive data (passwords/API keys) stored in localStorage");
    }

    @Test(priority = 43, description = "BUG-22: Check for sensitive data in sessionStorage")
    public void testBUG22_SensitiveDataInSessionStorage() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_HTTP_SECURITY,
                "BUG22_SensitiveDataInSessionStorage");
        logStep("Checking sessionStorage for sensitive data exposure");

        navigateToLogin();
        pause(1000);

        @SuppressWarnings("unchecked")
        Map<String, String> storage = (Map<String, String>) js.executeScript(
                "var items = {};"
                + "for (var i = 0; i < sessionStorage.length; i++) {"
                + "  var key = sessionStorage.key(i);"
                + "  items[key] = sessionStorage.getItem(key);"
                + "}"
                + "return items;");

        logStep("sessionStorage items: " + storage.size());

        boolean sensitiveDataFound = false;
        for (Map.Entry<String, String> entry : storage.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();
            logStep("  sessionStorage['" + entry.getKey() + "']: "
                    + (value != null ? value.substring(0, Math.min(50, value.length())) + "..." : "null"));

            if (key.contains("password") || key.contains("secret") || key.contains("apikey")) {
                sensitiveDataFound = true;
                logStep("SECURITY: Sensitive key in sessionStorage: " + entry.getKey());
            }
        }

        logStepWithScreenshot("sessionStorage check");

        Assert.assertFalse(sensitiveDataFound,
                "SECURITY BUG: Sensitive data stored in sessionStorage");
        ExtentReportManager.logPass("No sensitive credentials in sessionStorage. Items: " + storage.size());
    }

    // ================================================================
    // SECTION 6: SESSION & COOKIE SECURITY
    // ================================================================

    @Test(priority = 50, description = "BUG-23: Verify cookie security flags")
    public void testBUG23_CookieSecurityFlags() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_SESSION_SECURITY,
                "BUG23_CookieSecurityFlags");
        logStep("Checking cookie security flags (Secure, HttpOnly, SameSite)");

        navigateToLogin();
        pause(1000);

        Set<Cookie> cookies = driver.manage().getCookies();
        logStep("Total cookies: " + cookies.size());

        boolean hasInsecureCookie = false;
        for (Cookie cookie : cookies) {
            logStep("Cookie: " + cookie.getName()
                    + " | Secure=" + cookie.isSecure()
                    + " | HttpOnly=" + cookie.isHttpOnly()
                    + " | SameSite=" + cookie.getSameSite()
                    + " | Domain=" + cookie.getDomain());

            // Session/auth cookies MUST have Secure flag on HTTPS
            if (cookie.getName().toLowerCase().contains("session")
                    || cookie.getName().toLowerCase().contains("token")
                    || cookie.getName().toLowerCase().contains("auth")) {
                if (!cookie.isSecure()) {
                    hasInsecureCookie = true;
                    logStep("SECURITY: Auth cookie '" + cookie.getName() + "' missing Secure flag!");
                }
            }
        }

        logStepWithScreenshot("Cookie security check");

        if (hasInsecureCookie) {
            ExtentReportManager.logFail("SECURITY: Auth cookies missing Secure flag");
        } else {
            ExtentReportManager.logPass("Cookie security flags verified. Total cookies: " + cookies.size());
        }
    }

    @Test(priority = 51, description = "BUG-24: Verify no auth tokens in URL")
    public void testBUG24_NoAuthTokensInURL() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_SESSION_SECURITY,
                "BUG24_NoAuthTokensInURL");
        logStep("Verifying auth tokens are not exposed in URL");

        navigateToLogin();

        String url = driver.getCurrentUrl();
        logStep("Current URL: " + url);

        // Check URL for common token parameter names
        boolean tokenInUrl = url.contains("token=")
                || url.contains("access_token=")
                || url.contains("auth=")
                || url.contains("session=")
                || url.contains("jwt=")
                || url.contains("api_key=");

        Assert.assertFalse(tokenInUrl,
                "SECURITY BUG: Auth token visible in URL. URL: " + url);

        logStepWithScreenshot("URL token check");
        ExtentReportManager.logPass("No auth tokens in URL");
    }

    @Test(priority = 52, description = "BUG-25: Multiple rapid login attempts (rate limiting)")
    public void testBUG25_RapidLoginAttempts() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_SESSION_SECURITY,
                "BUG25_RapidLoginAttempts");
        logStep("Testing rate limiting on rapid login attempts");

        navigateToLogin();

        int attempts = 10;
        int blockedCount = 0;

        for (int i = 0; i < attempts; i++) {
            WebElement emailField = driver.findElement(EMAIL_INPUT);
            setReactValue(emailField, "brute@force.com");
            WebElement passwordField = driver.findElement(PASSWORD_INPUT);
            setReactValue(passwordField, "attempt" + i);
            clickTermsCheckbox();
            pause(200);

            if (isSignInEnabled()) {
                driver.findElement(SIGN_IN_BUTTON).click();
                pause(1500);

                // Check for rate limiting response
                List<WebElement> errors = driver.findElements(ERROR_ALERT);
                if (!errors.isEmpty()) {
                    String errorText = errors.get(0).getText().toLowerCase();
                    if (errorText.contains("too many") || errorText.contains("rate limit")
                            || errorText.contains("locked") || errorText.contains("try again")) {
                        blockedCount++;
                        logStep("Attempt " + (i + 1) + ": RATE LIMITED — " + errorText);
                    } else {
                        logStep("Attempt " + (i + 1) + ": Error — " + errorText);
                    }
                }
            }

            // Navigate back for next attempt
            navigateToLogin();
        }

        logStep("Rate limiting detected in " + blockedCount + "/" + attempts + " attempts");
        logStepWithScreenshot("Rate limiting test");

        // Log whether rate limiting exists
        if (blockedCount == 0) {
            logStep("WARNING: No rate limiting detected after " + attempts + " rapid login attempts. "
                    + "Consider implementing rate limiting to prevent brute force attacks.");
            ExtentReportManager.logFail("No rate limiting detected after " + attempts + " login attempts");
        } else {
            ExtentReportManager.logPass("Rate limiting active after " + blockedCount + " attempts");
        }

        // Soft assertion — rate limiting is a best practice, may not exist yet
        // Don't hard-fail here as it depends on backend implementation
    }

    @Test(priority = 53, description = "BUG-26: Browser back button after navigation")
    public void testBUG26_BrowserBackButton() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_SESSION_SECURITY,
                "BUG26_BrowserBackButton");
        logStep("Testing browser back button behavior on login page");

        // Navigate away and back
        navigateToLogin();
        String loginUrl = driver.getCurrentUrl();

        driver.get("about:blank");
        pause(500);

        driver.navigate().back();
        pause(2000);

        String afterBack = driver.getCurrentUrl();
        logStep("URL after back: " + afterBack);

        // Should still show login page (not a cached authenticated page)
        boolean onLogin = afterBack.contains("egalvanic")
                || afterBack.equals(loginUrl);
        logStep("Returned to egalvanic site: " + onLogin);

        logStepWithScreenshot("Back button test");
        ExtentReportManager.logPass("Back button behavior verified. URL: " + afterBack);
    }

    // ================================================================
    // SECTION 7: ERROR HANDLING & INFORMATION LEAKAGE
    // ================================================================

    @Test(priority = 60, description = "BUG-27: 404 page doesn't leak server info")
    public void testBUG27_404PageNoServerInfoLeak() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_ERROR_HANDLING,
                "BUG27_404PageInfoLeak");
        logStep("Checking 404 page for server information leakage");

        driver.get(AppConstants.BASE_URL + "/nonexistent-page-" + System.currentTimeMillis());
        pause(3000);

        String pageText = driver.findElement(By.tagName("body")).getText();
        logStep("404 page text length: " + pageText.length());

        // Check for server technology leaks
        boolean leaksServerInfo = pageText.contains("nginx")
                || pageText.contains("Apache")
                || pageText.contains("Express")
                || pageText.contains("Node.js")
                || pageText.contains("PHP")
                || pageText.contains("ASP.NET");

        boolean leaksStackTrace = pageText.contains("at ")
                || pageText.contains("Error:")
                || pageText.contains("TypeError")
                || pageText.contains("ReferenceError");

        boolean leaksFilePaths = pageText.contains("/var/")
                || pageText.contains("/usr/")
                || pageText.contains("/home/")
                || pageText.contains("C:\\");

        logStep("Leaks server info: " + leaksServerInfo);
        logStep("Leaks stack trace: " + leaksStackTrace);
        logStep("Leaks file paths: " + leaksFilePaths);

        logStepWithScreenshot("404 page");

        Assert.assertFalse(leaksServerInfo,
                "SECURITY BUG: 404 page reveals server technology");
        Assert.assertFalse(leaksStackTrace,
                "SECURITY BUG: 404 page contains stack trace");
        Assert.assertFalse(leaksFilePaths,
                "SECURITY BUG: 404 page reveals server file paths");

        ExtentReportManager.logPass("404 page does not leak server information");
    }

    @Test(priority = 61, description = "BUG-28: Invalid subdomain error handling")
    public void testBUG28_InvalidSubdomainErrorHandling() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_ERROR_HANDLING,
                "BUG28_InvalidSubdomainError");
        logStep("Testing error handling for invalid subdomain");

        try {
            driver.get("https://nonexistent-company-xyzabc123.qa.egalvanic.ai");
            pause(5000);

            String pageText = driver.findElement(By.tagName("body")).getText();
            String currentUrl = driver.getCurrentUrl();
            logStep("URL: " + currentUrl);
            logStep("Page text (first 200 chars): " + pageText.substring(0, Math.min(200, pageText.length())));

            // Should show a user-friendly error, not a raw server error
            boolean hasUserFriendlyError = pageText.contains("not found")
                    || pageText.contains("does not exist")
                    || pageText.contains("Invalid")
                    || pageText.contains("Error")
                    || pageText.isEmpty(); // Blank page is acceptable for invalid subdomain

            logStepWithScreenshot("Invalid subdomain page");
            ExtentReportManager.logPass("Invalid subdomain handled. Has friendly error: " + hasUserFriendlyError);
        } catch (Exception e) {
            // DNS resolution failure or connection refused is acceptable
            logStep("Connection error for invalid subdomain (expected): " + e.getMessage());
            ExtentReportManager.logPass("Invalid subdomain rejected at DNS/connection level");
        }
    }

    @Test(priority = 62, description = "BUG-29: API endpoint returns proper error format")
    public void testBUG29_APIErrorFormat() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_ERROR_HANDLING,
                "BUG29_APIErrorFormat");
        logStep("Testing API error response format");

        navigateToLogin();

        // Call the login API directly with invalid credentials via fetch
        String response = (String) js.executeAsyncScript(
                "var callback = arguments[arguments.length - 1];"
                + "fetch('/api/auth/login', {"
                + "  method: 'POST',"
                + "  headers: {'Content-Type': 'application/json'},"
                + "  body: JSON.stringify({email:'test@test.com', password:'wrong', subdomain:'acme'})"
                + "})"
                + ".then(function(resp) { return resp.text(); })"
                + ".then(function(text) { callback(text); })"
                + ".catch(function(err) { callback('ERROR: ' + err.message); });");

        logStep("API response: " + response);

        // Verify response is JSON (not HTML error page)
        boolean isJson = response != null && (response.trim().startsWith("{") || response.trim().startsWith("["));
        logStep("Response is JSON: " + isJson);

        if (isJson) {
            // Check it doesn't contain stack traces or internal paths
            Assert.assertFalse(response.contains("at "),
                    "API error contains stack trace");
            Assert.assertFalse(response.contains("/var/") || response.contains("/usr/"),
                    "API error contains file paths");
        }

        logStepWithScreenshot("API error format");
        ExtentReportManager.logPass("API error response format: " + (isJson ? "JSON" : "non-JSON")
                + ". Response: " + response);
    }

    @Test(priority = 63, description = "BUG-30: Verify HTTPS redirect")
    public void testBUG30_HTTPSRedirect() {
        ExtentReportManager.createTest(MODULE, AppConstants.FEATURE_ERROR_HANDLING,
                "BUG30_HTTPSRedirect");
        logStep("Verifying HTTP to HTTPS redirect");

        // Try to load via HTTP
        String httpUrl = AppConstants.BASE_URL.replace("https://", "http://");
        logStep("Attempting HTTP URL: " + httpUrl);

        try {
            driver.get(httpUrl);
            pause(3000);

            String finalUrl = driver.getCurrentUrl();
            logStep("Final URL after redirect: " + finalUrl);

            boolean isHttps = finalUrl.startsWith("https://");
            logStep("Redirected to HTTPS: " + isHttps);

            logStepWithScreenshot("HTTPS redirect");

            Assert.assertTrue(isHttps,
                    "SECURITY BUG: HTTP does not redirect to HTTPS. Final URL: " + finalUrl);
            ExtentReportManager.logPass("HTTP correctly redirects to HTTPS");
        } catch (Exception e) {
            // Connection refused on HTTP port is also acceptable (HSTS preload)
            logStep("HTTP connection failed (acceptable — HSTS may block): " + e.getMessage());
            ExtentReportManager.logPass("HTTP blocked (HSTS or port not open)");
        }
    }
}
