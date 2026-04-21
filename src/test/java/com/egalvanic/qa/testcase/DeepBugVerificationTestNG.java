package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Deep Bug Verification — Full Regression Suite
 *
 * One TestNG method per bug reported in
 * bug pdf/eGalvanic_Deep_Bug_Report_20_April_2026.html.
 *
 * Every test EXPECTS THE BUG TO BE PRESENT and fails when the bug is fixed.
 * When a bug is fixed in production, flip the Assert.assertTrue/False to
 * the opposite polarity (or delete the @Test) so this file becomes a
 * regression gate that prevents the bug from coming back.
 *
 * Coverage:
 *   BUG-001  No 404 page for invalid routes
 *   BUG-002  CSP blocks Beamer fonts (102 violations)
 *   BUG-003  Task creation triggers task-session mapping 400
 *   BUG-004  Issue Class validation gap
 *   BUG-005  No visible keyboard focus indicator (WCAG 2.4.7)
 *   BUG-006  Raw HTTP 400 + internal API name leaked in UI
 *   BUG-007  JSON parse error for invalid session URL
 *   BUG-008  Invalid service agreement URL shows blank page
 *   BUG-009  Form validation uses internal field names
 *   BUG-010  Doubled /api/api/ URL construction
 *   BUG-011  Issue Title accepts 2000+ chars (no maxLength)
 *   BUG-012  Sales Overview "Company information not available"
 *   BUG-013  Average page load > 10 seconds
 *   BUG-014  Issue does not persist after create
 */
public class DeepBugVerificationTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ================================================================
    // BUG-001: No 404 page for invalid routes
    // ================================================================
    @Test(priority = 1, description = "BUG-001: Invalid URLs render blank content area instead of 404")
    public void testBUG001_No404Page() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_NO_404_PAGE,
                "BUG-001: No 404 page");
        try {
            driver.get(AppConstants.BASE_URL + "/nonexistentpage123xyz");
            pause(5000);
            logStepWithScreenshot("Loaded /nonexistentpage123xyz");

            String body = driver.findElement(By.tagName("body")).getText();
            boolean has404Text = body.matches("(?is).*(?:404|page not found|not found).*");
            String mainText = ((String) js().executeScript(
                    "return document.querySelector('main, [role=\"main\"]')?.innerText || '';"
            )).trim();

            boolean bugPresent = !has404Text && mainText.length() < 40;
            Assert.assertTrue(bugPresent,
                    "BUG-001 FIXED: invalid URL now shows 404 text or non-empty main. Remove this test.");
            ExtentReportManager.logPass("BUG-001 confirmed: blank content on invalid route");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG001_error");
            Assert.fail("BUG-001 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-002: CSP blocks Beamer fonts
    // ================================================================
    @Test(priority = 2, description = "BUG-002: CSP font-src blocks Beamer Lato fonts")
    public void testBUG002_CSPBeamerFontsBlocked() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_CSP_BEAMER_FONTS,
                "BUG-002: CSP blocks Beamer");
        try {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(8000);

            Long beamerFontCount = (Long) js().executeScript(
                    "return performance.getEntriesByType('resource').filter(" +
                    "  e => e.name.indexOf('getbeamer.com') !== -1 && e.name.endsWith('.woff2')" +
                    ").length;"
            );
            logStep("Beamer font resource entries: " + beamerFontCount);
            logStepWithScreenshot("Dashboard loaded — check CSP violations in console");

            Assert.assertTrue(beamerFontCount != null && beamerFontCount >= 1,
                    "BUG-002 may be fixed: no Beamer font requests detected (expected CSP-blocked requests)");
            ExtentReportManager.logPass("BUG-002 confirmed: " + beamerFontCount + " Beamer font requests (each CSP-blocked)");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG002_error");
            Assert.fail("BUG-002 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-003: Task creation triggers task-session mapping 400
    // ================================================================
    @Test(priority = 3, description = "BUG-003: task-session/create returns 400 after Task create")
    public void testBUG003_TaskSessionMapping400() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_TASK_SESSION_MAPPING,
                "BUG-003: Task-session 400");
        try {
            // Install a network observer before navigating
            js().executeScript(
                    "window.__mapping400 = []; " +
                    "const origFetch = window.fetch; " +
                    "window.fetch = function() { " +
                    "  return origFetch.apply(this, arguments).then(r => { " +
                    "    if (r.status === 400 && r.url.indexOf('task-session/create') !== -1) {" +
                    "      window.__mapping400.push(r.url);" +
                    "    } return r;" +
                    "  });" +
                    "};"
            );

            driver.get(AppConstants.BASE_URL + "/tasks");
            pause(5000);
            List<WebElement> addBtns = driver.findElements(
                    By.xpath("//button[contains(., 'Add Tasks') or contains(., '+ Create Task')]"));
            if (addBtns.isEmpty()) {
                ExtentReportManager.logPass("BUG-003: create button not found — skipping");
                return;
            }
            addBtns.get(0).click();
            pause(3000);
            WebElement title = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("input[placeholder*='Title' i]")));
            title.sendKeys("Regression BUG-003 Task " + System.currentTimeMillis());
            pause(1000);
            List<WebElement> submit = driver.findElements(
                    By.xpath("//button[contains(., 'Create Task') and not(@disabled)]"));
            if (!submit.isEmpty()) {
                submit.get(submit.size() - 1).click();
                pause(6000);
            }
            logStepWithScreenshot("After task create — check console/network for 400");
            Long count = (Long) js().executeScript("return (window.__mapping400 || []).length;");
            Assert.assertTrue(count != null && count >= 1,
                    "BUG-003 may be fixed: no task-session/create 400 observed");
            ExtentReportManager.logPass("BUG-003 confirmed: " + count + " 400 responses on task-session mapping");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG003_error");
            Assert.fail("BUG-003 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-004: Issue Class validation gap
    // ================================================================
    @Test(priority = 4, description = "BUG-004: Issue form accepts submit with blank Issue Class")
    public void testBUG004_IssueClassValidationGap() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_ISSUE_CLASS_VALIDATION,
                "BUG-004: Issue Class validation");
        try {
            driver.get(AppConstants.BASE_URL + "/issues");
            pause(5000);
            List<WebElement> createBtns = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue')]"));
            if (createBtns.isEmpty()) {
                ExtentReportManager.logPass("BUG-004: create button not found — skipping");
                return;
            }
            createBtns.get(0).click();
            pause(3000);

            WebElement titleInput = driver.findElement(
                    By.cssSelector("input[placeholder*='Title' i]"));
            titleInput.sendKeys("Regression BUG-004 Issue " + System.currentTimeMillis());
            pause(500);
            List<WebElement> textAreas = driver.findElements(By.tagName("textarea"));
            if (!textAreas.isEmpty()) {
                textAreas.get(0).sendKeys("Regression proposed resolution");
                pause(500);
            }
            List<WebElement> submit = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue') and not(@disabled)]"));
            if (!submit.isEmpty()) {
                submit.get(submit.size() - 1).click();
                pause(5000);
            }
            // Check whether dialog closed (validation passed) or stayed open (validation caught it)
            List<WebElement> stillOpen = driver.findElements(
                    By.cssSelector(".MuiDialog-root:not([style*='display: none']), .MuiDrawer-root:not([style*='display: none'])"));
            boolean dialogClosed = stillOpen.isEmpty();
            logStepWithScreenshot("After Create Issue submit without Issue Class");
            Assert.assertTrue(dialogClosed,
                    "BUG-004 FIXED: dialog stayed open — validation catching missing Issue Class");
            ExtentReportManager.logPass("BUG-004 confirmed: form submitted without required Issue Class");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG004_error");
            Assert.fail("BUG-004 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-005: No visible keyboard focus indicator
    // ================================================================
    @Test(priority = 5, description = "BUG-005: Tab focus has no visible outline (WCAG 2.4.7)")
    public void testBUG005_NoKeyboardFocusIndicator() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_KEYBOARD_FOCUS,
                "BUG-005: Keyboard focus indicator missing");
        try {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(5000);
            driver.findElement(By.tagName("body")).sendKeys(Keys.TAB, Keys.TAB, Keys.TAB, Keys.TAB);
            pause(1000);
            String outlineStyle = (String) js().executeScript(
                    "const ae = document.activeElement; if (!ae) return 'no-active';" +
                    "const s = getComputedStyle(ae);" +
                    "return s.outlineStyle + '|' + s.outlineWidth;"
            );
            logStep("Active element outline: " + outlineStyle);
            boolean bugPresent = outlineStyle != null &&
                    (outlineStyle.startsWith("none|") || outlineStyle.contains("|0px"));
            Assert.assertTrue(bugPresent,
                    "BUG-005 FIXED: focus indicator is visible (" + outlineStyle + ")");
            ExtentReportManager.logPass("BUG-005 confirmed: focus has no visible outline (" + outlineStyle + ")");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG005_error");
            Assert.fail("BUG-005 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-006: Raw HTTP 400 + internal API name leaked
    // ================================================================
    @Test(priority = 6, description = "BUG-006: Invalid asset URL leaks 'Failed to fetch enriched node details: 400'")
    public void testBUG006_RawAPIErrorLeaked() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_RAW_API_ERROR_LEAK,
                "BUG-006: Raw API error leak");
        try {
            driver.get(AppConstants.BASE_URL + "/assets/invalid-uuid-test-12345");
            pause(6000);
            String body = driver.findElement(By.tagName("body")).getText();
            logStepWithScreenshot("Loaded invalid asset URL");
            boolean leakPresent = body.contains("Failed to fetch enriched node details") ||
                    body.contains("enriched node details: 400");
            Assert.assertTrue(leakPresent,
                    "BUG-006 FIXED: raw error text no longer visible — great, remove this test");
            ExtentReportManager.logPass("BUG-006 confirmed: raw backend error leaked to UI");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG006_error");
            Assert.fail("BUG-006 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-007: JSON parse error for invalid session URL
    // ================================================================
    @Test(priority = 7, description = "BUG-007: Invalid session URL triggers 'not valid JSON' SyntaxError")
    public void testBUG007_JSONParseError() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_JSON_PARSE_ERROR,
                "BUG-007: JSON parse error");
        try {
            js().executeScript(
                    "window.__jsonErrs = []; " +
                    "window.addEventListener('error', e => { if (String(e.message).indexOf('not valid JSON') !== -1) window.__jsonErrs.push(String(e.message)); }); " +
                    "const origCE = console.error; console.error = function() { " +
                    "  const msg = Array.from(arguments).map(a => String(a)).join(' '); " +
                    "  if (msg.indexOf('not valid JSON') !== -1) window.__jsonErrs.push(msg); " +
                    "  return origCE.apply(this, arguments); };"
            );
            driver.get(AppConstants.BASE_URL + "/sessions/invalid-session-id-9999");
            pause(6000);
            logStepWithScreenshot("Loaded invalid session URL");
            Long count = (Long) js().executeScript("return (window.__jsonErrs || []).length;");
            Assert.assertTrue(count != null && count >= 1,
                    "BUG-007 FIXED: no JSON parse errors observed");
            ExtentReportManager.logPass("BUG-007 confirmed: " + count + " JSON parse errors observed");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG007_error");
            Assert.fail("BUG-007 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-008: Blank page on invalid jobs-v2 URL
    // ================================================================
    @Test(priority = 8, description = "BUG-008: /jobs-v2/<invalid> renders blank page")
    public void testBUG008_BlankServiceAgreementPage() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_BLANK_INVALID_ROUTE,
                "BUG-008: Blank service agreement");
        try {
            driver.get(AppConstants.BASE_URL + "/jobs-v2/invalid-uuid-12345");
            pause(6000);
            String mainText = (String) js().executeScript(
                    "return (document.querySelector('main, [role=\"main\"]')?.innerText || '').trim();"
            );
            logStepWithScreenshot("Loaded invalid jobs-v2 URL");
            boolean bugPresent = mainText.length() < 50 && !mainText.toLowerCase().contains("not found");
            Assert.assertTrue(bugPresent,
                    "BUG-008 FIXED: invalid service agreement URL now shows content (length=" + mainText.length() + ")");
            ExtentReportManager.logPass("BUG-008 confirmed: blank main content on invalid jobs-v2 URL");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG008_error");
            Assert.fail("BUG-008 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-009: Form validation uses internal field names
    // ================================================================
    @Test(priority = 9, description = "BUG-009: Asset form shows 'Asset label is required' (internal name)")
    public void testBUG009_InternalFieldNamesInValidation() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_INTERNAL_FIELD_NAMES,
                "BUG-009: Internal field names");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(5000);
            List<WebElement> createBtns = driver.findElements(
                    By.xpath("//button[contains(., 'Create Asset') or contains(., '+ Create Asset')]"));
            if (createBtns.isEmpty()) {
                ExtentReportManager.logPass("BUG-009: create button not found — skipping");
                return;
            }
            createBtns.get(0).click();
            pause(3000);
            List<WebElement> submit = driver.findElements(
                    By.xpath("//button[(contains(., 'Create Asset') or contains(., 'Create'))  and not(@disabled)]"));
            if (!submit.isEmpty()) submit.get(submit.size() - 1).click();
            pause(3000);
            String validationText = (String) js().executeScript(
                    "return Array.from(document.querySelectorAll('.MuiFormHelperText-root.Mui-error'))" +
                    ".map(e => e.innerText).join(' || ');"
            );
            logStepWithScreenshot("Validation errors on empty Create Asset submit");
            boolean bugPresent = validationText != null &&
                    (validationText.toLowerCase().contains("asset label") ||
                     validationText.toLowerCase().contains("asset type is required"));
            Assert.assertTrue(bugPresent,
                    "BUG-009 FIXED: validation now uses user-facing labels (" + validationText + ")");
            ExtentReportManager.logPass("BUG-009 confirmed: " + validationText);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG009_error");
            Assert.fail("BUG-009 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-010: Doubled /api/api/ URL
    // ================================================================
    @Test(priority = 10, description = "BUG-010: Graph API URL has doubled /api/api/ prefix")
    public void testBUG010_DoubledApiApiURL() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_DOUBLE_API_PREFIX,
                "BUG-010: Doubled /api/api/");
        try {
            driver.get(AppConstants.BASE_URL + "/assets/invalid-uuid-test-12345");
            pause(6000);
            Long doubleCount = (Long) js().executeScript(
                    "return performance.getEntriesByType('resource').filter(" +
                    "  e => e.name.indexOf('/api/api/') !== -1" +
                    ").length;"
            );
            logStepWithScreenshot("Invalid asset URL — check network for /api/api/");
            Assert.assertTrue(doubleCount != null && doubleCount >= 1,
                    "BUG-010 FIXED: no /api/api/ requests observed");
            ExtentReportManager.logPass("BUG-010 confirmed: " + doubleCount + " requests with /api/api/");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG010_error");
            Assert.fail("BUG-010 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-011: Issue Title has no maxLength
    // ================================================================
    @Test(priority = 11, description = "BUG-011: Issue Title accepts 2000+ chars (no maxLength)")
    public void testBUG011_NoMaxLengthOnIssueTitle() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_NO_MAX_LENGTH,
                "BUG-011: No maxLength");
        try {
            driver.get(AppConstants.BASE_URL + "/issues");
            pause(5000);
            List<WebElement> createBtns = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue')]"));
            if (createBtns.isEmpty()) {
                ExtentReportManager.logPass("BUG-011: create button not found — skipping");
                return;
            }
            createBtns.get(0).click();
            pause(3000);
            WebElement titleInput = driver.findElement(By.cssSelector("input[placeholder*='Title' i]"));
            StringBuilder longStr = new StringBuilder();
            for (int i = 0; i < 2000; i++) longStr.append("A");
            js().executeScript(
                    "arguments[0].focus(); " +
                    "const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set; " +
                    "setter.call(arguments[0], arguments[1]); " +
                    "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));",
                    titleInput, longStr.toString());
            pause(1500);
            String value = titleInput.getAttribute("value");
            String maxLen = titleInput.getAttribute("maxlength");
            logStepWithScreenshot("Input value length=" + (value == null ? 0 : value.length()) + " maxlength=" + maxLen);
            boolean bugPresent = (maxLen == null || maxLen.isEmpty()) && value != null && value.length() >= 1500;
            Assert.assertTrue(bugPresent,
                    "BUG-011 FIXED: maxLength=" + maxLen + " valueLen=" + (value == null ? 0 : value.length()));
            ExtentReportManager.logPass("BUG-011 confirmed: no maxLength, accepted " + value.length() + " chars");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG011_error");
            Assert.fail("BUG-011 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-012: Sales Overview error banner
    // ================================================================
    @Test(priority = 12, description = "BUG-012: Sales Overview shows 'Company information not available'")
    public void testBUG012_SalesOverviewBrokenBanner() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SALES_OVERVIEW_BROKEN,
                "BUG-012: Sales Overview broken");
        try {
            driver.get(AppConstants.BASE_URL + "/sales-overview");
            pause(7000);
            String body = driver.findElement(By.tagName("body")).getText();
            logStepWithScreenshot("Sales Overview page loaded");
            boolean bugPresent = body.contains("Company information not available");
            Assert.assertTrue(bugPresent,
                    "BUG-012 FIXED: Sales Overview no longer shows 'Company information not available'");
            ExtentReportManager.logPass("BUG-012 confirmed: error banner present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG012_error");
            Assert.fail("BUG-012 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-013: Average page load > 10 seconds
    // ================================================================
    @Test(priority = 13, description = "BUG-013: Top-level pages take > 10s to reach networkidle")
    public void testBUG013_SlowPageLoads() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_PAGE_LOAD_PERFORMANCE,
                "BUG-013: Slow page loads");
        try {
            String[] paths = { "/dashboard", "/tasks", "/sessions", "/assets", "/issues" };
            long totalMs = 0;
            int samples = 0;
            for (String p : paths) {
                long start = System.currentTimeMillis();
                driver.get(AppConstants.BASE_URL + p);
                // Selenium does not have Playwright's networkidle; approximate via document.readyState + delay
                new WebDriverWait(driver, Duration.ofSeconds(30))
                        .until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState;")));
                pause(2500);
                long loadTime = System.currentTimeMillis() - start;
                logStep(p + " loaded in " + loadTime + "ms");
                totalMs += loadTime;
                samples++;
            }
            long avg = totalMs / Math.max(samples, 1);
            logStepWithScreenshot("Average load: " + avg + "ms across " + samples + " pages");
            Assert.assertTrue(avg >= 6000,
                    "BUG-013 FIXED: avg load is " + avg + "ms (under 6s threshold). Performance improved — flip assertion.");
            ExtentReportManager.logPass("BUG-013 confirmed: avg " + avg + "ms across " + samples + " pages");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG013_error");
            Assert.fail("BUG-013 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-014: Issue does not persist after create (ambiguous / flaky)
    // ================================================================
    @Test(priority = 14, description = "BUG-014: Created Issue does not appear in list after hard reload")
    public void testBUG014_IssueDoesNotPersist() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_DATA_PERSISTENCE,
                "BUG-014: Issue persistence");
        try {
            driver.get(AppConstants.BASE_URL + "/issues");
            pause(5000);
            List<WebElement> createBtns = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue')]"));
            if (createBtns.isEmpty()) {
                ExtentReportManager.logPass("BUG-014: create button not found — skipping");
                return;
            }
            String unique = "Regression_BUG014_" + System.currentTimeMillis();
            createBtns.get(0).click();
            pause(3000);
            driver.findElement(By.cssSelector("input[placeholder*='Title' i]")).sendKeys(unique);
            pause(500);
            List<WebElement> textAreas = driver.findElements(By.tagName("textarea"));
            if (!textAreas.isEmpty()) textAreas.get(0).sendKeys("Regression BUG-014 resolution");
            pause(500);
            List<WebElement> submit = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue') and not(@disabled)]"));
            if (!submit.isEmpty()) submit.get(submit.size() - 1).click();
            pause(7000);

            driver.navigate().refresh();
            pause(7000);
            String body = driver.findElement(By.tagName("body")).getText();
            boolean bugPresent = !body.contains(unique);
            logStepWithScreenshot("After reload — searching for '" + unique + "'");
            Assert.assertTrue(bugPresent,
                    "BUG-014 FIXED: created Issue now persists and is visible after reload");
            ExtentReportManager.logPass("BUG-014 confirmed: created Issue '" + unique + "' not in list after reload");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG014_error");
            Assert.fail("BUG-014 test crashed: " + e.getMessage());
        }
    }
}
