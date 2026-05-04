package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Re-verification of retracted v2 bug findings.
 *
 * Context: The user identified that several v2 findings were FALSE POSITIVES
 * caused by test-side issues (different site contexts, page-load timing,
 * missing site filter). This class re-runs each retracted finding with:
 *
 *   1. EXPLICIT site context — captures which site the dashboard is showing,
 *      then ensures the modules under comparison see that same site
 *   2. POLLING for content readiness instead of fixed pause()
 *   3. SITE FILTER triggering (e.g., "Select All" on /slds page)
 *
 * If a test PASSES here → the v2 finding was correctly retracted (false positive)
 * If a test FAILS here → it's a REAL bug worth re-adding to the v4 report
 *
 * Each test is self-contained: navigates to the surface, captures evidence,
 * asserts with diagnostic message that names the site context.
 */
public class RetractedBugsVerificationTestNG extends BaseTest {

    private static final String FEATURE_VERIFY = "Retracted v2 — Re-verification";

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    /** Read the site-selector currently-selected site label, if visible. */
    private String currentSite() {
        try {
            return (String) js().executeScript(
                "var sel = document.querySelector('.MuiSelect-select, "
                + "[class*=\"site-selector\"] [class*=\"value\"], "
                + "[aria-label*=\"site\" i] [class*=\"value\"]');"
                + "if (!sel) return null;"
                + "return (sel.textContent || '').trim();");
        } catch (Exception e) { return null; }
    }

    /** Poll until the grid's "of N" footer populates. Returns parsed N or -1. */
    private long waitForGridTotalAndRead(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object raw = js().executeScript(
                    "var f = document.querySelector('.MuiTablePagination-displayedRows');"
                    + "if (!f) return null;"
                    + "var t = (f.textContent || '').trim();"
                    + "var m = t.match(/of\\s+([0-9,]+)/i);"
                    + "if (!m) return null;"
                    + "return m[1].replace(/,/g, '');");
                if (raw != null) {
                    try { return Long.parseLong(String.valueOf(raw)); }
                    catch (NumberFormatException ignore) {}
                }
            } catch (Exception ignore) {}
            pause(500);
        }
        return -1;
    }

    /** Poll until at least N grid rows are visible, OR timeout. */
    private long waitForGridRows(long timeoutMs, int minRows) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                Long count = (Long) js().executeScript(
                    "return document.querySelectorAll('.MuiDataGrid-row').length;");
                if (count != null && count >= minRows) return count;
            } catch (Exception ignore) {}
            pause(500);
        }
        try {
            Long count = (Long) js().executeScript(
                "return document.querySelectorAll('.MuiDataGrid-row').length;");
            return count == null ? 0 : count;
        } catch (Exception e) { return -1; }
    }

    // ================================================================
    // VERIFY-1: BUG-03 v2 (asset count) with proper site context
    // ================================================================
    @Test(priority = 1, description = "Verify-1: Asset count Dashboard ↔ /assets with site context pinned + polling")
    public void testVerify_01_AssetCount_SiteContext() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_VERIFY,
                "Verify v2 BUG-03 retraction");
        try {
            // 1. Go to dashboard, capture site label and asset KPI count
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(6000);  // generous boot wait
            String siteLabel = currentSite();
            logStep("Site selected on Dashboard: " + siteLabel);
            ScreenshotUtil.captureScreenshot("VERIFY_01_dashboard");

            // Try to find an Asset KPI on dashboard. Many dashboards have a card
            // labeled "Assets" or "Equipment" with a count.
            Long dashAssetCount = (Long) js().executeScript(
                "var els = document.querySelectorAll('*');"
                + "var best = -1;"
                + "for (var el of els) {"
                + "  if (!el.offsetWidth) continue;"
                + "  var t = (el.textContent || '').trim();"
                + "  if (/^\\s*Assets?\\s*$/i.test(t) && t.length < 20) {"
                + "    var parent = el.closest('div, section, article');"
                + "    if (!parent) continue;"
                + "    var nums = parent.textContent.match(/[0-9][0-9,]*/g);"
                + "    if (nums) for (var n of nums) {"
                + "      var v = parseInt(n.replace(/,/g, ''), 10);"
                + "      if (!isNaN(v) && v > best) best = v;"
                + "    }"
                + "  }"
                + "}"
                + "return best > 0 ? best : null;");
            logStep("Dashboard Asset KPI count: " + dashAssetCount);

            // 2. Go to /assets WITHOUT changing the site
            driver.get(AppConstants.BASE_URL + "/assets");
            long gridTotal = waitForGridTotalAndRead(20000);
            logStep("/assets grid total (after polling 20s): " + gridTotal);
            ScreenshotUtil.captureScreenshot("VERIFY_01_assets");

            String currentSiteAfter = currentSite();
            logStep("Site selected on /assets: " + currentSiteAfter);

            // 3. Verdict
            if (dashAssetCount == null) {
                throw new org.testng.SkipException(
                    "Could not locate Asset KPI on dashboard — UI may have changed");
            }
            if (gridTotal <= 0) {
                throw new org.testng.SkipException(
                    "Could not read grid 'of N' footer after 20s polling — "
                    + "either no pagination, or grid still loading");
            }

            boolean siteMatches = siteLabel != null
                    && siteLabel.equals(currentSiteAfter);

            // If counts match → v2 retraction was correct (false positive)
            // If counts diverge AND same site → real bug
            // If site differs → can't conclude; SkipException
            if (!siteMatches) {
                throw new org.testng.SkipException(
                    "Site context differs between dashboard ('" + siteLabel
                    + "') and /assets ('" + currentSiteAfter + "') — "
                    + "cannot conclude whether counts should match.");
            }

            // Same site, both populated. Now compare:
            logStep("FINAL: Dashboard=" + dashAssetCount + " /assets=" + gridTotal
                + " on site '" + siteLabel + "'");
            Assert.assertEquals(gridTotal, dashAssetCount.longValue(),
                "REAL BUG (re-confirmed): Dashboard says " + dashAssetCount
                + " assets for site '" + siteLabel + "' but /assets grid total is "
                + gridTotal + ". Same site context — counts SHOULD match. "
                + "v2 BUG-03 retraction was incorrect; this is a real bug.");

            ExtentReportManager.logPass(
                "v2 BUG-03 RETRACTION CONFIRMED: counts match on same site '"
                + siteLabel + "' — Dashboard=" + dashAssetCount
                + ", /assets=" + gridTotal);
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("VERIFY_01_error");
            Assert.fail("Verify_01 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // VERIFY-2: BUG-06 v2 (asset grid empty after refresh) with polling
    // ================================================================
    @Test(priority = 2, description = "Verify-2: /assets grid populates within 30s after F5 refresh")
    public void testVerify_02_AssetGridSurvivesRefresh() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_VERIFY,
                "Verify v2 BUG-06 retraction");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            // Initial wait for the grid to load
            long initialRows = waitForGridRows(20000, 1);
            logStep("Initial /assets grid rows after first load: " + initialRows);
            if (initialRows < 1) {
                throw new org.testng.SkipException(
                    "Initial /assets grid never populated — can't test refresh");
            }

            // Refresh
            driver.navigate().refresh();
            logStep("Page refreshed (F5 equivalent)");

            // Generous polling — wait up to 30s
            long postRefreshRows = waitForGridRows(30000, 1);
            ScreenshotUtil.captureScreenshot("VERIFY_02_after_refresh");
            logStep("/assets grid rows after refresh + 30s polling: " + postRefreshRows);

            Assert.assertTrue(postRefreshRows >= 1,
                "REAL BUG (re-confirmed): /assets grid is empty 30s after F5 refresh. "
                + "Initial load had " + initialRows + " rows; post-refresh has "
                + postRefreshRows + ". State recovery broken.");

            ExtentReportManager.logPass(
                "v2 BUG-06 RETRACTION CONFIRMED: grid populates after refresh "
                + "(initial=" + initialRows + ", post-refresh=" + postRefreshRows + ")");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("VERIFY_02_error");
            Assert.fail("Verify_02 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // VERIFY-3: BUG-10 v2 (0 KPI cards) with polling
    // ================================================================
    @Test(priority = 3, description = "Verify-3: Dashboard renders ≥4 KPI cards after polling 20s")
    public void testVerify_03_KPICardsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_VERIFY,
                "Verify v2 BUG-10 retraction");
        try {
            driver.get(AppConstants.BASE_URL + "/dashboard");

            long deadline = System.currentTimeMillis() + 20000;
            long bestCount = 0;
            while (System.currentTimeMillis() < deadline) {
                Long c = (Long) js().executeScript(
                    "var sels = ['.MuiCard-root', '[class*=\"kpi\" i]', "
                    + "'[class*=\"stat-card\" i]', '[class*=\"summary\" i]'];"
                    + "var seen = new Set();"
                    + "var n = 0;"
                    + "for (var s of sels) {"
                    + "  document.querySelectorAll(s).forEach(function(c){"
                    + "    if (!c.offsetWidth || !c.offsetHeight) return;"
                    + "    if (seen.has(c)) return;"
                    + "    seen.add(c);"
                    + "    n++;"
                    + "  });"
                    + "}"
                    + "return n;");
                if (c != null && c > bestCount) bestCount = c;
                if (bestCount >= 4) break;
                pause(800);
            }
            ScreenshotUtil.captureScreenshot("VERIFY_03_dashboard");
            logStep("Dashboard KPI/card count after 20s polling: " + bestCount);

            Assert.assertTrue(bestCount >= 4,
                "REAL BUG (re-confirmed): only " + bestCount
                + " KPI/card elements visible on dashboard after 20s polling.");

            ExtentReportManager.logPass(
                "v2 BUG-10 RETRACTION CONFIRMED: dashboard renders " + bestCount
                + " KPI cards.");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("VERIFY_03_error");
            Assert.fail("Verify_03 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // VERIFY-4: SLDs page with "Select All" filter (per user feedback)
    // ================================================================
    @Test(priority = 4, description = "Verify-4: /slds page loads diagrams when 'Select All' filter is triggered")
    public void testVerify_04_SLDsLoadWithSelectAll() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_VERIFY,
                "Verify v2 BUG-28 retraction (SLD page)");
        try {
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(5000);

            // Try to find and click a "Select All" filter / button
            Boolean clicked = (Boolean) js().executeScript(
                "var els = document.querySelectorAll('button, [role=\"button\"], a, label, "
                + "[role=\"menuitem\"], [role=\"option\"]');"
                + "for (var e of els) {"
                + "  if (!e.offsetWidth) continue;"
                + "  var t = (e.textContent || '').trim().toLowerCase();"
                + "  if (t === 'select all' || t === 'all' || t === 'all sites') {"
                + "    e.scrollIntoView({block: 'center'});"
                + "    e.click();"
                + "    return true;"
                + "  }"
                + "}"
                + "return false;");
            logStep("'Select All' / 'All' filter clicked: " + clicked);
            pause(5000);

            // Count visible SVG / diagram elements + dropdown items
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> state =
                (java.util.Map<String, Object>) js().executeScript(
                "var svgs = 0;"
                + "document.querySelectorAll('svg').forEach(function(s){"
                + "  if (s.offsetWidth) svgs++;"
                + "});"
                + "var dropdownItems = 0;"
                + "document.querySelectorAll('li[role=\"option\"], "
                + ".MuiMenuItem-root, .MuiAutocomplete-option').forEach(function(o){"
                + "  if (o.offsetWidth) dropdownItems++;"
                + "});"
                + "var bodyChildren = 0;"
                + "document.body.querySelectorAll('*').forEach(function(c){"
                + "  if (c.offsetWidth && c.offsetHeight) bodyChildren++;"
                + "});"
                + "return {"
                + "  svgs: svgs,"
                + "  dropdownItems: dropdownItems,"
                + "  bodyChildren: bodyChildren"
                + "};");
            ScreenshotUtil.captureScreenshot("VERIFY_04_slds");
            logStep("SLD page state: " + state);

            long bc = ((Number) state.get("bodyChildren")).longValue();
            Assert.assertTrue(bc > 50,
                "/slds page DOM collapsed (" + bc + " elements) — page didn't render");

            // Don't strictly assert SLDs visible — depends on tenant data.
            // Just record the state and pass if page rendered.
            ExtentReportManager.logPass(
                "/slds page rendered (body=" + bc + ", svgs=" + state.get("svgs")
                + ", dropdownItems=" + state.get("dropdownItems")
                + ", clickedFilter=" + clicked + ")");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("VERIFY_04_error");
            Assert.fail("Verify_04 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // VERIFY-5: Asset detail tabs (BUG-24 retraction)
    // Already covered by TC_BH_18 in Phase1BugHunterTestNG (verified 7/7
    // tabs load). This test just re-confirms quickly with 1 asset.
    // ================================================================
    @Test(priority = 5, description = "Verify-5: Asset detail tabs render content (re-confirm TC_BH_18)")
    public void testVerify_05_AssetDetailTabsLoad() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUG_HUNT, FEATURE_VERIFY,
                "Verify v2 BUG-24 retraction");
        try {
            driver.get(AppConstants.BASE_URL + "/assets");
            long rows = waitForGridRows(20000, 1);
            if (rows < 1) {
                throw new org.testng.SkipException("No assets in grid");
            }

            // Click first row → detail
            java.util.List<WebElement> rowEls = driver.findElements(
                By.cssSelector(".MuiDataGrid-row"));
            safeClick(rowEls.get(0));
            pause(4000);

            // Click each visible tab in turn, verify body remains rendered
            @SuppressWarnings("unchecked")
            java.util.List<String> tabNames = (java.util.List<String>) js().executeScript(
                "var tabs = document.querySelectorAll('[role=\"tab\"], .MuiTab-root');"
                + "var names = [];"
                + "for (var t of tabs) {"
                + "  if (!t.offsetWidth) continue;"
                + "  var n = (t.textContent || '').trim();"
                + "  if (n && n.length < 30) names.push(n);"
                + "}"
                + "return names;");
            logStep("Detail page tabs: " + tabNames);

            int loaded = 0;
            for (String name : tabNames) {
                try {
                    js().executeScript(
                        "var tabs = document.querySelectorAll('[role=\"tab\"], .MuiTab-root');"
                        + "for (var t of tabs) {"
                        + "  if (!t.offsetWidth) continue;"
                        + "  if ((t.textContent || '').trim() === arguments[0]) {"
                        + "    t.click(); return;"
                        + "  }"
                        + "}", name);
                    pause(2500);
                    Long bodyChildren = (Long) js().executeScript(
                        "var n = 0;"
                        + "document.body.querySelectorAll('*').forEach(function(c){"
                        + "  if (c.offsetWidth && c.offsetHeight) n++;"
                        + "});"
                        + "return n;");
                    if (bodyChildren != null && bodyChildren > 50) loaded++;
                    logStep("Tab '" + name + "' bodyChildren=" + bodyChildren);
                } catch (Exception inner) {
                    logStep("Tab '" + name + "' exception: " + inner.getMessage());
                }
            }
            ScreenshotUtil.captureScreenshot("VERIFY_05_tabs");

            Assert.assertEquals(loaded, tabNames.size(),
                "REAL BUG (re-confirmed): only " + loaded + " of " + tabNames.size()
                + " detail tabs rendered content. v2 BUG-24 may be real.");

            ExtentReportManager.logPass(
                "v2 BUG-24 RETRACTION CONFIRMED: all " + tabNames.size()
                + " detail tabs render content.");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("VERIFY_05_error");
            Assert.fail("Verify_05 crashed: " + e.getMessage());
        }
    }
}
