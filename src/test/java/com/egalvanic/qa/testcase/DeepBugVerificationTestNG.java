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
 *   BUG-002  CSP blocks Beamer fonts                              [MEDIUM]
 *   BUG-003  Issue Class validation gap                           [MEDIUM]
 *   BUG-004  Raw HTTP 400 + internal API name leaked in UI        [HIGH]
 *   BUG-005  Issue Title accepts 2000+ chars (no maxLength)       [LOW]
 *   BUG-006  Average page load > 10 seconds                       [MEDIUM]
 *
 * Assertion contract (updated 2026-04-22):
 *   PASS  = feature works correctly (bug NOT detected in this run)
 *   FAIL  = bug detected — the assertion message describes the defect and fix
 *
 * This is the conventional polarity: a green CI dashboard means the product
 * is healthy; a red CI dashboard means a regression is active. The earlier
 * inverted pattern (pass = bug present) was replaced because it made green
 * runs misleading for clients.
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
            Assert.assertFalse(bugPresent,
                    "BUG-001: Invalid URL /nonexistentpage123xyz renders blank content instead of a 404 page. " +
                    "Expected: 404 or 'Page Not Found' message with nav back to dashboard. " +
                    "Actual: has404Text=" + has404Text + ", mainTextLen=" + mainText.length());
            ExtentReportManager.logPass("Invalid URL shows a 404 / Page Not Found page correctly");
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

            // Count ONLY Beamer font entries that failed (transferSize===0 with very short duration
            // indicates CSP block or network failure). Loaded fonts have non-zero transferSize.
            Long blockedCount = (Long) js().executeScript(
                    "return performance.getEntriesByType('resource').filter(function(e){" +
                    "  return /getbeamer\\.com/.test(e.name) && " +
                    "         /\\.woff2?$/.test(e.name) && " +
                    "         e.transferSize === 0 && e.duration < 10;" +
                    "}).length;"
            );
            Long totalBeamerFonts = (Long) js().executeScript(
                    "return performance.getEntriesByType('resource').filter(function(e){" +
                    "  return /getbeamer\\.com/.test(e.name) && /\\.woff2?$/.test(e.name);" +
                    "}).length;"
            );
            logStep("Beamer font entries — total: " + totalBeamerFonts + ", CSP-blocked: " + blockedCount);
            logStepWithScreenshot("Dashboard loaded — Beamer font load status");

            long blocked = blockedCount == null ? 0L : blockedCount.longValue();
            long totalFonts = totalBeamerFonts == null ? 0L : totalBeamerFonts.longValue();

            // Guard against false-positive PASS: if Beamer didn't load AT ALL,
            // blocked==0 would silently pass even though we haven't verified anything.
            // Treat "Beamer never loaded" as an inconclusive failure so the CI report
            // doesn't claim the bug is fixed when we couldn't check.
            Assert.assertTrue(totalFonts > 0L,
                    "BUG-002: Cannot verify — Beamer didn't load any fonts in this run. "
                    + "Either Beamer SDK failed to init (separate issue) or CSP already blocked it pre-font. "
                    + "Manually verify on /dashboard, then re-run.");

            Assert.assertEquals(blocked, 0L,
                    "BUG-002: " + blocked + " Beamer font(s) blocked by CSP (out of " + totalFonts + " total). "
                    + "Fix: add fonts.getbeamer.com or *.getbeamer.com to the CSP font-src directive.");
            ExtentReportManager.logPass("No CSP-blocked Beamer fonts detected (of " + totalFonts + " loaded)");
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

            // Fix (live-verified 2026-04-24): the original test filled
            // textAreas.get(0) which is the "Describe the issue" textarea, NOT
            // "Describe the proposed resolution". Proposed Resolution IS a
            // required field, so the original test's submit was rejected on
            // PropRes validation — the drawer stayed open, which the test then
            // misread as "Issue Class validation caught it". We have to fill
            // Proposed Resolution specifically and leave ONLY Issue Class blank.
            List<WebElement> propResAreas = driver.findElements(
                    By.cssSelector("textarea[placeholder*='proposed resolution' i]"));
            List<WebElement> descAreas = driver.findElements(
                    By.cssSelector("textarea[placeholder*='Describe the issue' i]"));
            if (!descAreas.isEmpty()) { descAreas.get(0).sendKeys("Regression issue description"); pause(300); }
            if (!propResAreas.isEmpty()) { propResAreas.get(0).sendKeys("Regression proposed resolution"); pause(300); }
            logStep("Filled Title + Description + Proposed Resolution. Issue Class INTENTIONALLY blank.");

            List<WebElement> submit = driver.findElements(
                    By.xpath("//button[contains(., 'Create Issue') and not(@disabled)]"));
            if (!submit.isEmpty()) {
                submit.get(submit.size() - 1).click();
                pause(5000);
            }

            // Fix (live-verified 2026-04-24): the original "still open" selector
            // matched .MuiDrawer-root which ALWAYS matches the left-sidebar nav
            // (a permanent MuiDrawer). The sidebar was making dialogClosed always
            // false, falsely reporting "bug fixed". Correct check: is the Create
            // Issue form (the drawer/dialog CONTAINING the Title input) still
            // visible? If yes → validation caught it; if no → form accepted
            // submit despite blank Issue Class (bug present).
            boolean createFormStillOpen = (Boolean) js().executeScript(
                    "var drawers = document.querySelectorAll('.MuiDrawer-paper, [role=\"dialog\"]');"
                    + "for (var d of drawers) {"
                    + "  var r = d.getBoundingClientRect();"
                    + "  if (r.width > 400 && r.height > 300) {"
                    + "    var titleInput = d.querySelector('input[placeholder*=\"Title\" i]');"
                    + "    if (titleInput) return true;"
                    + "  }"
                    + "}"
                    + "return false;");
            logStepWithScreenshot("After submit — Create Issue form still open: " + createFormStillOpen);

            // bug present when: form CLOSED (submit accepted). form still open means validation worked.
            Assert.assertTrue(createFormStillOpen,
                    "BUG-003: Create Issue form accepted submit with blank Issue Class "
                    + "(form CLOSED without error = validation did NOT catch the missing required field). "
                    + "Fix: add required-field validation on Issue Class in both frontend and backend.");
            ExtentReportManager.logPass("Issue form correctly blocks submit when Issue Class is blank (form still open)");
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
            // Fix (live-verified 2026-04-24): Selenium's WebElement.getText() filters
            // based on CSS visibility and can miss text that document.body.innerText
            // DOES include. We verified manually that "Failed to fetch enriched node
            // details: 400" appears in body.innerText on this tenant — but the test
            // was falsely passing in CI because getText() missed it. Switch to JS
            // innerText + outerHTML to cover the same surface a user sees.
            String body = (String) js().executeScript(
                    "return document.body && document.body.innerText ? document.body.innerText : '';");
            String pageSource = (String) js().executeScript(
                    "return document.documentElement ? document.documentElement.outerHTML : '';");
            if (body == null) body = "";
            if (pageSource == null) pageSource = "";
            logStepWithScreenshot("Loaded invalid asset URL");

            String[] leakMarkers = {
                    "Failed to fetch enriched node details",
                    "enriched node details: 400",
                    "enriched_node_details",    // snake_case variant
                    "/api/asset/enriched",      // raw endpoint path
                    "enrichedNodeDetails",      // camelCase variant
            };
            String foundIn = null;
            for (String marker : leakMarkers) {
                if (body.contains(marker)) { foundIn = "body.innerText[" + marker + "]"; break; }
                if (pageSource.contains(marker)) { foundIn = "pageSource[" + marker + "]"; break; }
            }
            boolean leakPresent = foundIn != null;
            logStep("Leak marker found: " + foundIn);
            Assert.assertFalse(leakPresent,
                    "BUG-004: Invalid asset URL exposes raw backend internals (" + foundIn + "). "
                    + "Fix: catch the 400 in the frontend, show a generic 'Asset not found' message instead.");
            ExtentReportManager.logPass("Invalid asset URL handled cleanly (no raw API error leaked in body.innerText OR pageSource)");
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
            // Fix (live-verified 2026-04-24): Selenium's WebElement.getAttribute("maxlength")
            // can return the DOM .maxLength PROPERTY (-1 when unset) rather than the HTML
            // attribute (null when unset). That non-null "-1" string made maxLenSet=true
            // and maxLenInt=-1, which failed the >1000 check, giving a false PASS.
            // Fix: read raw HTML attribute via JS getAttribute('maxlength') — returns
            // null if no maxlength="" in HTML, regardless of DOM-property default.
            Object liveValueObj = js().executeScript("return arguments[0].value;", titleInput);
            String liveValue = liveValueObj == null ? "" : liveValueObj.toString();
            // Raw HTML attribute, not DOM property — this is the authoritative "is there
            // a maxlength on this input?" signal.
            Object rawMaxLenObj = js().executeScript("return arguments[0].getAttribute('maxlength');", titleInput);
            String maxLen = rawMaxLenObj == null ? null : rawMaxLenObj.toString();
            // Also capture the DOM .maxLength property as diagnostic info (not used in
            // the assertion — -1 means "no limit").
            Long maxLenProp = (Long) js().executeScript(
                    "var v = arguments[0].maxLength; return (typeof v === 'number') ? v : null;", titleInput);

            int valueLen = liveValue.length();
            logStepWithScreenshot("Input valueLen=" + valueLen + " htmlMaxlength=" + maxLen
                    + " domMaxLengthProp=" + maxLenProp);

            // Guard against false-positive PASS: if the JS native-setter injection
            // silently failed, valueLen would be near-0 and the test would report
            // "no bug". Require that the setter actually took effect (valueLen >= 1500).
            boolean maxLenSet = maxLen != null && !maxLen.isEmpty() && !maxLen.equals("-1");
            int maxLenInt = -1;
            if (maxLenSet) {
                try { maxLenInt = Integer.parseInt(maxLen); } catch (NumberFormatException ignored) {}
            }

            if (!maxLenSet && valueLen < 1500) {
                Assert.fail("BUG-005: Cannot verify — JS native-setter injected only " + valueLen
                        + " chars (expected >= 1500). Either the React component prevented the write "
                        + "(would be an implicit fix worth confirming manually) or the test helper failed. "
                        + "Manually paste 2000+ chars into the Issue Title and re-run.");
            }

            // Bug is present when either:
            //  (a) No maxlength attribute AND input accepted 1500+ chars, OR
            //  (b) maxlength IS set but unreasonably high (>1000) — effectively unlimited
            //      so DB/UI damage is still possible (long titles break list views, CSV
            //      exports, email notifications).
            boolean bugPresent = (!maxLenSet && valueLen >= 1500)
                              || (maxLenSet && maxLenInt > 1000);
            Assert.assertFalse(bugPresent,
                    "BUG-005: Issue Title input accepts excessive length. "
                    + "maxlength=" + maxLen + ", value-length=" + valueLen + ". "
                    + "Fix: set maxlength to a reasonable value (e.g., 255) to protect downstream "
                    + "list views, CSV exports, and email notifications from oversized titles.");
            ExtentReportManager.logPass("Issue Title has a reasonable character limit (maxLength="
                    + maxLen + ", value=" + valueLen + ")");
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
            long maxMs = 0;
            String slowestPath = "";
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
                if (loadTime > maxMs) { maxMs = loadTime; slowestPath = p; }
                samples++;
            }
            long avg = totalMs / Math.max(samples, 1);
            logStepWithScreenshot("Avg load: " + avg + "ms, slowest: " + slowestPath + " at " + maxMs
                    + "ms, across " + samples + " pages");

            // Dual threshold:
            //  (1) Average < 6s (existing check) — catches broad slowness
            //  (2) Max page load < 8s — catches "one page is really slow" even when avg is fine
            // CI network is faster than a typical user's corporate VPN, so the thresholds are lenient.
            // If either breaks, the product has a genuine performance issue.
            boolean avgFast = avg < 6000;
            boolean maxFast = maxMs < 8000;
            Assert.assertTrue(avgFast && maxFast,
                    "BUG-006: Page-load performance regression. "
                    + "Avg=" + avg + "ms (threshold 6000ms, pass=" + avgFast + "), "
                    + "Slowest=" + slowestPath + " at " + maxMs + "ms (threshold 8000ms, pass=" + maxFast + "). "
                    + "Fix: investigate N+1 API calls, bundle size, initial render blockers on the slowest page first.");
            ExtentReportManager.logPass("Page-load performance OK — avg " + avg + "ms, slowest "
                    + slowestPath + " at " + maxMs + "ms");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG013_error");
            Assert.fail("BUG-006 test crashed: " + e.getMessage());
        }
    }
}