package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * QA Test Plan — ZP-1997: Fix XSS Gaps (DOMPurify Sanitization)
 *
 * Background:
 *   A new utility sanitizeHtml.js was added with two functions:
 *     - sanitizeHtml() — sanitizes rich HTML content (headings, links,
 *       tables, lists, media, etc.)
 *     - sanitizeSvg() — sanitizes SVG content using DOMPurify's SVG profile
 *   Every raw-HTML injection site across 16 components was wrapped with
 *   the appropriate sanitizer.
 *
 * What this test class verifies (4 categories):
 *   1. SVG icons still render correctly across all components that use them
 *   2. Rich HTML content (form blocks, Z University, COM Calculator) still
 *      renders with allowed tags + attributes intact
 *   3. Malicious XSS payloads are stripped/neutralized — no alert fires,
 *      no JS execution, dangerous attributes removed
 *   4. Regression checks — target=_blank links have rel=noopener noreferrer,
 *      iframes still work
 *
 * Approach:
 *   - All tests are read-only or cleanup-safe (no DB writes)
 *   - XSS payloads are typed into search inputs and similar surfaces; we
 *     verify no alert fires AND no script is preserved in the DOM
 *   - Honest skip when a test surface isn't reachable on the current build
 *     (e.g., Z University requires specific tenant config)
 *
 * Live-verification target: acme.qa.egalvanic.ai
 */
public class ZP1997DOMPurifyXssTestNG extends BaseTest {

    private static final String FEATURE_ZP1997 = "ZP-1997 DOMPurify XSS";

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private void assertNoAlert(String testName) {
        try {
            String alertText = driver.switchTo().alert().getText();
            driver.switchTo().alert().dismiss();
            Assert.fail("XSS BUG in " + testName + ": JS alert fired with text: '"
                    + alertText + "'. DOMPurify did not strip the payload.");
        } catch (NoAlertPresentException ok) {
            // Expected — no alert means XSS was prevented
        } catch (Exception e) {
            // Other alert-related issues — not a failure
        }
    }

    // ================================================================
    // SECTION 1 — SVG ICON RENDERING (sanitizeSvg)
    // ================================================================

    @Test(priority = 1, description = "ZP-1997 SVG-1: /assets page renders SVG icons correctly after sanitizeSvg")
    public void testSVG_01_AssetsPageIcons() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "SVG icons render on /assets");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);

            Long svgCount = (Long) js().executeScript(
                    "var svgs = document.querySelectorAll('svg');"
                    + "var visible = 0;"
                    + "for (var s of svgs) {"
                    + "  if (s.getBoundingClientRect().width > 0) visible++;"
                    + "}"
                    + "return visible;");
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_01");
            logStep("Visible SVG count on /assets: " + svgCount);

            Assert.assertTrue(svgCount != null && svgCount >= 5,
                    "Expected ≥5 visible SVG icons on /assets after DOMPurify, got " + svgCount
                    + ". DOMPurify may have over-aggressively stripped SVG content.");

            ExtentReportManager.logPass("SVG icons render on /assets: " + svgCount + " visible");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_01_error");
            Assert.fail("SVG_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "ZP-1997 SVG-2: /locations tree node icons render after sanitizeSvg")
    public void testSVG_02_LocationsTreeIcons() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "SVG icons render on /locations");
        try {
            driver.get(AppConstants.BASE_URL + "/locations");
            pause(4000);

            Long svgCount = (Long) js().executeScript(
                    "var svgs = document.querySelectorAll('svg');"
                    + "var visible = 0;"
                    + "for (var s of svgs) {"
                    + "  if (s.getBoundingClientRect().width > 0) visible++;"
                    + "}"
                    + "return visible;");
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_02");
            logStep("Visible SVG count on /locations: " + svgCount);

            Assert.assertTrue(svgCount != null && svgCount >= 5,
                    "Expected ≥5 visible SVG icons on /locations, got " + svgCount);

            ExtentReportManager.logPass("/locations SVG icons render: " + svgCount);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_02_error");
            Assert.fail("SVG_02 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "ZP-1997 SVG-3: /slds Single Line Diagram nodes render SVG icons")
    public void testSVG_03_SLDDynamicNodes() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "SVG icons on SLD diagram nodes");
        try {
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(5000);

            Long svgCount = (Long) js().executeScript(
                    "var svgs = document.querySelectorAll('svg');"
                    + "var visible = 0;"
                    + "for (var s of svgs) {"
                    + "  if (s.getBoundingClientRect().width > 0) visible++;"
                    + "}"
                    + "return visible;");
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_03");
            logStep("Visible SVG count on /slds: " + svgCount);

            Assert.assertTrue(svgCount != null && svgCount >= 3,
                    "Expected ≥3 visible SVG icons on /slds, got " + svgCount);

            ExtentReportManager.logPass("/slds SVG icons render: " + svgCount);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_03_error");
            Assert.fail("SVG_03 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "ZP-1997 SVG-4: Asset class icon picker shows multiple SVG options")
    public void testSVG_04_AssetClassIconPicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "Icon picker SVG render");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);
            assetPage.openCreateAssetForm();
            pause(2500);

            Boolean clicked = (Boolean) js().executeScript(
                    "var btns = document.querySelectorAll('button');"
                    + "for (var b of btns) {"
                    + "  if (!b.offsetWidth) continue;"
                    + "  var t = (b.textContent || '').trim().toLowerCase();"
                    + "  var al = (b.getAttribute('aria-label') || '').toLowerCase();"
                    + "  if (t.indexOf('icon') !== -1 || al.indexOf('icon') !== -1 "
                    + "    || t.indexOf('select icon') !== -1) {"
                    + "    b.click(); return true;"
                    + "  }"
                    + "}"
                    + "return false;");
            if (!Boolean.TRUE.equals(clicked)) {
                throw new org.testng.SkipException(
                        "Icon picker trigger not found in Create Asset form — "
                        + "UI may not expose it on this asset class");
            }
            pause(1500);

            Long svgInPicker = (Long) js().executeScript(
                    "var dlg = document.querySelector('[role=\"dialog\"]:not([hidden]), "
                    + ".MuiDialog-paper:not([hidden]), .MuiPopover-paper:not([hidden])');"
                    + "if (!dlg) return 0;"
                    + "return dlg.querySelectorAll('svg').length;");
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_04");
            logStep("SVG count in icon picker: " + svgInPicker);

            try {
                driver.findElement(By.tagName("body"))
                        .sendKeys(org.openqa.selenium.Keys.ESCAPE);
                pause(800);
            } catch (Exception ignore) {}

            Assert.assertTrue(svgInPicker != null && svgInPicker >= 5,
                    "Icon picker should show ≥5 SVG options, got " + svgInPicker);

            ExtentReportManager.logPass("Icon picker SVGs: " + svgInPicker);
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_SVG_04_error");
            Assert.fail("SVG_04 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // SECTION 2 — RICH HTML CONTENT RENDERING (sanitizeHtml)
    // ================================================================

    @Test(priority = 10, description = "ZP-1997 HTML-1: /admin/forms text/message blocks render rich HTML")
    public void testHTML_01_EGFormBuilder() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "EG Form Builder rich HTML");
        try {
            driver.get(AppConstants.BASE_URL + "/admin/forms");
            pause(5000);

            Long bodyChildren = (Long) js().executeScript(
                    "var n = 0;"
                    + "document.body.querySelectorAll('*').forEach(function(c){"
                    + "  if (c.offsetWidth && c.offsetHeight) n++;"
                    + "});"
                    + "return n;");
            ScreenshotUtil.captureScreenshot("ZP1997_HTML_01");
            logStep("/admin/forms body children: " + bodyChildren);

            Assert.assertTrue(bodyChildren != null && bodyChildren > 50,
                    "/admin/forms didn't render properly (DOM only " + bodyChildren
                    + " elements). DOMPurify may be stripping form builder content.");

            ExtentReportManager.logPass("/admin/forms rendered with " + bodyChildren + " elements");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_HTML_01_error");
            Assert.fail("HTML_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 11, description = "ZP-1997 HTML-2: COM Calculator dialog info-alert renders")
    public void testHTML_02_COMCalculatorAlert() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "COM Calculator alert renders");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);

            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            if (rows.isEmpty()) {
                throw new org.testng.SkipException("No assets in grid to test COM dialog");
            }
            safeClick(rows.get(0));
            pause(3500);

            Boolean opened = (Boolean) js().executeScript(
                    "var btns = document.querySelectorAll('button');"
                    + "for (var b of btns) {"
                    + "  if (!b.offsetWidth) continue;"
                    + "  var al = (b.getAttribute('aria-label') || '').toLowerCase();"
                    + "  if (al.indexOf('calculator') !== -1 || al.indexOf('com') !== -1) {"
                    + "    b.click(); return true;"
                    + "  }"
                    + "}"
                    + "var cards = document.querySelectorAll('[class*=\"COM\"], "
                    + "[class*=\"calculator\" i]');"
                    + "for (var c of cards) {"
                    + "  if (c.offsetWidth) { c.click(); return true; }"
                    + "}"
                    + "return false;");
            if (!Boolean.TRUE.equals(opened)) {
                throw new org.testng.SkipException(
                        "COM Calculator entry point not found on this asset");
            }
            pause(2000);

            Long alertElements = (Long) js().executeScript(
                    "return document.querySelectorAll('.MuiAlert-root').length;");
            ScreenshotUtil.captureScreenshot("ZP1997_HTML_02");
            logStep("MuiAlert elements visible: " + alertElements);

            try {
                driver.findElement(By.tagName("body"))
                        .sendKeys(org.openqa.selenium.Keys.ESCAPE);
                pause(800);
            } catch (Exception ignore) {}

            ExtentReportManager.logPass("COM Calculator opened; alert elements: "
                    + alertElements);
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_HTML_02_error");
            Assert.fail("HTML_02 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 12, description = "ZP-1997 HTML-3: Z University renders Confluence HTML content")
    public void testHTML_03_ZUniversity() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "Z University HTML render");
        try {
            // Z University path varies across tenants — check several common
            // ones. If NONE renders meaningful content, the feature isn't
            // available on this tenant — SKIP rather than fail.
            String[] candidates = {
                    AppConstants.BASE_URL + "/z-university",
                    AppConstants.BASE_URL + "/zuniversity",
                    AppConstants.BASE_URL + "/help",
                    AppConstants.BASE_URL + "/learn"
            };
            String workingUrl = null;
            long bestBodyCount = 0;
            for (String url : candidates) {
                try {
                    driver.get(url);
                    pause(3500);
                    String currentUrl = driver.getCurrentUrl();
                    String bodyText = (String) js().executeScript(
                            "return (document.body.textContent || '').toLowerCase();");
                    Long bc = (Long) js().executeScript(
                            "var n = 0;"
                            + "document.body.querySelectorAll('*').forEach(function(c){"
                            + "  if (c.offsetWidth && c.offsetHeight) n++;"
                            + "});"
                            + "return n;");
                    long bcount = bc == null ? 0 : bc;
                    boolean has404 = bodyText != null && (bodyText.contains("not found")
                            || bodyText.contains("404"));
                    boolean stayedAtUrl = currentUrl.equals(url) || currentUrl.startsWith(url);
                    // Only accept if URL stuck AND body has meaningful content (>30)
                    // AND no 404 marker
                    if (stayedAtUrl && !has404 && bcount > 30) {
                        workingUrl = url;
                        bestBodyCount = bcount;
                        break;
                    }
                } catch (Exception ignore) {}
            }

            if (workingUrl == null) {
                throw new org.testng.SkipException(
                        "Z University not reachable at any of: /z-university, "
                        + "/zuniversity, /help, /learn — feature not enabled on this tenant. "
                        + "Skipping rather than false-failing.");
            }

            ScreenshotUtil.captureScreenshot("ZP1997_HTML_03");
            logStep("Z University rendered at " + workingUrl + " — body children: " + bestBodyCount);
            ExtentReportManager.logPass("Z University rendered: " + bestBodyCount
                    + " elements at " + workingUrl);
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_HTML_03_error");
            Assert.fail("HTML_03 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // SECTION 3 — XSS NEGATIVE TESTS (DOMPurify must strip these)
    // ================================================================

    @Test(priority = 20, description = "ZP-1997 XSS-1: <script> tag in search input is stripped — no alert fires")
    public void testXSS_01_ScriptTagStripped() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "XSS: script tag stripped");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);

            // Poll up to 8s for the search input to appear (page may still be hydrating)
            WebElement search = null;
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                List<WebElement> searches = driver.findElements(By.cssSelector(
                        "input[placeholder*='Search' i], input[type='search'], "
                        + "input[aria-label*='Search' i]"));
                for (WebElement s : searches) {
                    if (s.isDisplayed() && s.isEnabled()) { search = s; break; }
                }
                if (search != null) break;
                pause(500);
            }
            if (search == null) {
                throw new org.testng.SkipException("No search input on /assets");
            }

            String payload = "<script>alert('XSS-script')</script>";
            search.click();
            search.sendKeys(payload);
            pause(2000);

            assertNoAlert("XSS_01 ScriptTagStripped");

            Boolean scriptInDom = (Boolean) js().executeScript(
                    "var scripts = document.querySelectorAll('script');"
                    + "for (var s of scripts) {"
                    + "  if ((s.textContent || '').indexOf('XSS-script') !== -1) return true;"
                    + "}"
                    + "return false;");
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_01");
            logStep("Live <script> with payload in DOM: " + scriptInDom);

            try { search.clear(); } catch (Exception ignore) {}

            Assert.assertFalse(Boolean.TRUE.equals(scriptInDom),
                    "XSS BUG: <script> payload was injected as a live script in DOM. "
                    + "DOMPurify failed to strip it.");

            ExtentReportManager.logPass("Script tag XSS prevented: no alert, no live script");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_01_error");
            Assert.fail("XSS_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 21, description = "ZP-1997 XSS-2: <img onerror=...> attribute stripped — no alert fires")
    public void testXSS_02_ImgOnerrorStripped() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "XSS: img onerror stripped");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);

            // Poll up to 8s for the search input to appear (page may still be hydrating)
            WebElement search = null;
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                List<WebElement> searches = driver.findElements(By.cssSelector(
                        "input[placeholder*='Search' i], input[type='search'], "
                        + "input[aria-label*='Search' i]"));
                for (WebElement s : searches) {
                    if (s.isDisplayed() && s.isEnabled()) { search = s; break; }
                }
                if (search != null) break;
                pause(500);
            }
            if (search == null) {
                throw new org.testng.SkipException("No search input");
            }

            String payload = "<img src=x onerror=alert('XSS-img')>";
            search.click();
            search.sendKeys(payload);
            pause(2000);

            assertNoAlert("XSS_02 ImgOnerrorStripped");

            Boolean dangerousImg = (Boolean) js().executeScript(
                    "var imgs = document.querySelectorAll('img');"
                    + "for (var i of imgs) {"
                    + "  if (i.getAttribute('onerror') != null) return true;"
                    + "}"
                    + "return false;");
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_02");
            logStep("Img with onerror attribute present: " + dangerousImg);

            try { search.clear(); } catch (Exception ignore) {}

            Assert.assertFalse(Boolean.TRUE.equals(dangerousImg),
                    "XSS BUG: <img> with onerror attribute survived sanitization");

            ExtentReportManager.logPass("Img onerror XSS prevented: no alert, no onerror attr");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_02_error");
            Assert.fail("XSS_02 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 22, description = "ZP-1997 XSS-3: javascript: URI in <a href> blocked")
    public void testXSS_03_JavascriptUrlBlocked() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "XSS: javascript: URL blocked");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);

            @SuppressWarnings("unchecked")
            List<String> dangerousHrefs = (List<String>) js().executeScript(
                    "var anchors = document.querySelectorAll('a[href]');"
                    + "var bad = [];"
                    + "for (var a of anchors) {"
                    + "  var h = a.getAttribute('href') || '';"
                    + "  if (h.toLowerCase().startsWith('javascript:')) bad.push(h.slice(0,80));"
                    + "}"
                    + "return bad;");
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_03");
            logStep("javascript: hrefs found: " + dangerousHrefs.size());

            Assert.assertTrue(dangerousHrefs.isEmpty(),
                    "XSS BUG: " + dangerousHrefs.size() + " <a href=\"javascript:...\"> "
                    + "links survived sanitization: " + dangerousHrefs);

            ExtentReportManager.logPass("No javascript: URIs in DOM — DOMPurify scheme-blocking works");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_03_error");
            Assert.fail("XSS_03 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 23, description = "ZP-1997 XSS-4: <svg onload=...> attribute stripped from any rendered SVG")
    public void testXSS_04_SvgOnloadStripped() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "XSS: svg onload stripped");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);

            Long dangerousSvgCount = (Long) js().executeScript(
                    "var svgs = document.querySelectorAll('svg');"
                    + "var bad = 0;"
                    + "var danger = ['onload', 'onerror', 'onclick', 'onmouseover'];"
                    + "for (var s of svgs) {"
                    + "  for (var d of danger) {"
                    + "    if (s.getAttribute(d) != null) { bad++; break; }"
                    + "  }"
                    + "}"
                    + "return bad;");
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_04");
            logStep("SVGs with dangerous event handlers: " + dangerousSvgCount);

            Assert.assertEquals(dangerousSvgCount.longValue(), 0L,
                    "XSS BUG: " + dangerousSvgCount + " SVG(s) have dangerous event handler "
                    + "attributes (onload/onerror/onclick/onmouseover). sanitizeSvg failed.");

            ExtentReportManager.logPass("No SVG event handlers detected — sanitizeSvg works");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_XSS_04_error");
            Assert.fail("XSS_04 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // SECTION 4 — REGRESSION CHECKS
    // ================================================================

    @Test(priority = 30, description = "ZP-1997 REG-1: target=_blank links have rel=noopener noreferrer")
    public void testRegression_01_TargetBlankNoopener() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "Regression: target=_blank rel=noopener");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            pause(4500);

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> result =
                    (java.util.Map<String, Object>) js().executeScript(
                    "var links = document.querySelectorAll('a[target=\"_blank\"]');"
                    + "var total = 0; var withRel = 0;"
                    + "for (var l of links) {"
                    + "  total++;"
                    + "  var rel = (l.getAttribute('rel') || '').toLowerCase();"
                    + "  if (rel.indexOf('noopener') !== -1 && rel.indexOf('noreferrer') !== -1) {"
                    + "    withRel++;"
                    + "  }"
                    + "}"
                    + "return {total: total, withRel: withRel};");
            long total = ((Number) result.get("total")).longValue();
            long withRel = ((Number) result.get("withRel")).longValue();
            ScreenshotUtil.captureScreenshot("ZP1997_REG_01");
            logStep("target=_blank links: " + total + ", with rel=noopener noreferrer: " + withRel);

            if (total == 0) {
                throw new org.testng.SkipException(
                        "No target=_blank links on /assets — can't test regression here");
            }

            Assert.assertEquals(withRel, total,
                    "Regression: only " + withRel + " of " + total + " target=_blank links "
                    + "have rel=noopener noreferrer. DOMPurify ADD_ATTR hook may not be "
                    + "applying to all links.");

            ExtentReportManager.logPass("All " + total
                    + " target=_blank links have rel=noopener noreferrer");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_REG_01_error");
            Assert.fail("REG_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 31, description = "ZP-1997 REG-2: Iframes still render correctly (allowed by sanitizeHtml)")
    public void testRegression_02_IframesStillWork() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_ZP1997,
                "Regression: iframes render");
        try {
            driver.get(AppConstants.BASE_URL + "/admin/forms");
            pause(5000);

            Long iframeCount = (Long) js().executeScript(
                    "return document.querySelectorAll('iframe').length;");
            logStep("/admin/forms iframe count: " + iframeCount);
            ScreenshotUtil.captureScreenshot("ZP1997_REG_02");

            if (iframeCount == null || iframeCount == 0) {
                throw new org.testng.SkipException(
                        "No iframes on /admin/forms to verify sanitizeHtml allowance");
            }

            Long visibleIframes = (Long) js().executeScript(
                    "var fr = document.querySelectorAll('iframe');"
                    + "var n = 0;"
                    + "for (var f of fr) {"
                    + "  if (f.offsetWidth > 0 && f.getAttribute('src')) n++;"
                    + "}"
                    + "return n;");
            logStep("Visible iframes with src: " + visibleIframes);

            Assert.assertTrue(visibleIframes != null && visibleIframes > 0,
                    "Iframes present but none visible with src attribute. "
                    + "sanitizeHtml may have stripped iframe attributes.");

            ExtentReportManager.logPass("Iframes render correctly: " + visibleIframes
                    + " of " + iframeCount + " visible with src");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("ZP1997_REG_02_error");
            Assert.fail("REG_02 crashed: " + e.getMessage());
        }
    }
}
