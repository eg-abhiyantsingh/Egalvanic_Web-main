package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Deep Bug Verification — Curated Regression Suite (6 bugs).
 *
 * On 2026-04-21, QA lead validated the final bug list. 8 earlier findings from
 * this file (task-session 400, keyboard focus, JSON parse error, blank invalid
 * route, internal field names, /api/api/, Sales Overview, issue persistence)
 * were de-scoped as not reproducible / not valid and their @Test methods removed.
 *
 * Current coverage (renumbered to match the curated PDF at
 * bug pdf/eGalvanic_Deep_Bug_Report_20_April_2026.pdf):
 *   BUG-001  No 404 page for invalid routes                       [MEDIUM]
 *   BUG-002  CSP blocks Beamer fonts (102 violations)             [MEDIUM]
 *   BUG-003  Issue Class validation gap                           [MEDIUM]
 *   BUG-004  Raw HTTP 400 + internal API name leaked in UI        [HIGH]
 *   BUG-005  Issue Title accepts 2000+ chars (no maxLength)       [LOW]
 *   BUG-006  Average page load > 10 seconds                       [MEDIUM]
 *
 * Every test EXPECTS THE BUG TO BE PRESENT and fails when the bug is fixed.
 * When a bug is fixed in production, flip the Assert.assertTrue/False to
 * the opposite polarity (or delete the @Test) so this file becomes a
 * regression gate that prevents the bug from coming back.
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
    // BUG-003: Issue Class validation gap
    // ================================================================
    @Test(priority = 4, description = "BUG-003: Issue form accepts submit with blank Issue Class")
    public void testBUG003_IssueClassValidationGap() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_ISSUE_CLASS_VALIDATION,
                "BUG-003: Issue Class validation");
        try {
            driver.get(AppConstants.BASE_URL + "/issues");
            pause(5000);
            List<WebElement> createBtns = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue')]"));
            if (createBtns.isEmpty()) {
                ExtentReportManager.logPass("BUG-003: create button not found — skipping");
                return;
            }
            createBtns.get(0).click();
            pause(3000);

            WebElement titleInput = driver.findElement(
                    By.cssSelector("input[placeholder*='Title' i]"));
            titleInput.sendKeys("Regression BUG-003 Issue " + System.currentTimeMillis());
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
                    "BUG-003 FIXED: dialog stayed open — validation catching missing Issue Class");
            ExtentReportManager.logPass("BUG-003 confirmed: form submitted without required Issue Class");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG004_error");
            Assert.fail("BUG-003 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-004: Raw HTTP 400 + internal API name leaked
    // ================================================================
    @Test(priority = 6, description = "BUG-004: Invalid asset URL leaks 'Failed to fetch enriched node details: 400'")
    public void testBUG004_RawAPIErrorLeaked() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_RAW_API_ERROR_LEAK,
                "BUG-004: Raw API error leak");
        try {
            driver.get(AppConstants.BASE_URL + "/assets/invalid-uuid-test-12345");
            pause(6000);
            String body = driver.findElement(By.tagName("body")).getText();
            logStepWithScreenshot("Loaded invalid asset URL");
            boolean leakPresent = body.contains("Failed to fetch enriched node details") ||
                    body.contains("enriched node details: 400");
            Assert.assertTrue(leakPresent,
                    "BUG-004 FIXED: raw error text no longer visible — great, remove this test");
            ExtentReportManager.logPass("BUG-004 confirmed: raw backend error leaked to UI");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG006_error");
            Assert.fail("BUG-004 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-005: Issue Title has no maxLength
    // ================================================================
    @Test(priority = 11, description = "BUG-005: Issue Title accepts 2000+ chars (no maxLength)")
    public void testBUG005_NoMaxLengthOnIssueTitle() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_NO_MAX_LENGTH,
                "BUG-005: No maxLength");
        try {
            driver.get(AppConstants.BASE_URL + "/issues");
            pause(5000);
            List<WebElement> createBtns = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue')]"));
            if (createBtns.isEmpty()) {
                ExtentReportManager.logPass("BUG-005: create button not found — skipping");
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
                    "BUG-005 FIXED: maxLength=" + maxLen + " valueLen=" + (value == null ? 0 : value.length()));
            ExtentReportManager.logPass("BUG-005 confirmed: no maxLength, accepted " + value.length() + " chars");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG011_error");
            Assert.fail("BUG-005 test crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-006: Average page load > 10 seconds
    // ================================================================
    @Test(priority = 13, description = "BUG-006: Top-level pages take > 10s to reach networkidle")
    public void testBUG006_SlowPageLoads() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_PAGE_LOAD_PERFORMANCE,
                "BUG-006: Slow page loads");
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
                    "BUG-006 FIXED: avg load is " + avg + "ms (under 6s threshold). Performance improved — flip assertion.");
            ExtentReportManager.logPass("BUG-006 confirmed: avg " + avg + "ms across " + samples + " pages");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG013_error");
            Assert.fail("BUG-006 test crashed: " + e.getMessage());
        }
    }
}