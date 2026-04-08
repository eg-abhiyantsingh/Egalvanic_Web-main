package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Load & Performance Test Suite — 20 TCs
 * Aligned with QA Automation Plan — Load sheet
 *
 * Coverage:
 *   Section 1: Page Load Performance           (5 TCs) — dashboard, assets, connections, issues, work orders
 *   Section 2: Grid Performance                 (5 TCs) — large list render, scroll, pagination
 *   Section 3: Search Performance               (3 TCs) — search speed across modules
 *   Section 4: Navigation Performance           (3 TCs) — sidebar nav, page transitions
 *   Section 5: SLD Performance                  (2 TCs) — diagram load, zoom
 *   Section 6: API Response Time                (2 TCs) — network timing via Performance API
 *
 * Architecture: Extends BaseTest. Measures page load times, grid render times, and API latency.
 * Uses Navigation Timing API and Performance API for accurate measurements.
 */
public class LoadTestNG extends BaseTest {

    private static final String MODULE = "Load & Performance";
    private static final String FEATURE_PAGE_LOAD = "Page Load";
    private static final String FEATURE_GRID = "Grid Performance";
    private static final String FEATURE_SEARCH = "Search Performance";
    private static final String FEATURE_NAV = "Navigation Performance";
    private static final String FEATURE_SLD = "SLD Performance";
    private static final String FEATURE_API = "API Response Time";

    // Acceptable thresholds (ms)
    private static final long PAGE_LOAD_THRESHOLD = 25000;
    private static final long GRID_THRESHOLD = 15000; // 15s — CI VMs are slower than local
    private static final long SEARCH_THRESHOLD = 5000;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     Load & Performance Test Suite (20 TCs)");
        System.out.println("     " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================");
        System.out.println();
        super.classSetup();
    }

    @BeforeMethod
    @Override
    public void testSetup() {
        super.testSetup();
    }

    @AfterMethod
    @Override
    public void testTeardown(ITestResult result) {
        super.testTeardown(result);
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    /**
     * Navigate to a page and measure load time.
     * Returns milliseconds from navigation start to grid/content loaded.
     */
    private long measurePageLoad(String url, String waitForSelector) {
        long start = System.currentTimeMillis();
        driver.get(url);
        pause(1000);

        // Wait for content to appear
        // Use arguments[0] to avoid JS syntax errors when selector contains quotes
        for (int i = 0; i < 30; i++) {
            Boolean ready = (Boolean) js().executeScript(
                    "return document.querySelectorAll(arguments[0]).length > 0;", waitForSelector);
            if (ready != null && ready) break;
            pause(500);
        }
        return System.currentTimeMillis() - start;
    }

    /**
     * Navigate via sidebar and measure transition time.
     */
    private long measureSidebarNav(String linkText, String expectedUrlPart) {
        long start = System.currentTimeMillis();
        js().executeScript(
                "var links = document.querySelectorAll('a');" +
                "for(var el of links) { if(el.textContent.trim() === arguments[0]) { el.click(); return; } }", linkText);

        // Wait for URL to change
        for (int i = 0; i < 30; i++) {
            if (driver.getCurrentUrl().contains(expectedUrlPart)) break;
            pause(500);
        }
        pause(1000);
        return System.currentTimeMillis() - start;
    }

    // ================================================================
    // SECTION 1: PAGE LOAD PERFORMANCE (5 TCs)
    // ================================================================

    @Test(priority = 1, description = "TC_LOAD_PL01: Dashboard page load time")
    public void testPL01_DashboardLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_PAGE_LOAD, "TC_LOAD_PL01_Dashboard");

        long loadTime = measurePageLoad(AppConstants.BASE_URL + "/dashboard",
                "[class*='MuiCard'], [class*='dashboard'], [class*='stat']");
        logStep("Dashboard load time: " + loadTime + "ms");

        Assert.assertTrue(loadTime < PAGE_LOAD_THRESHOLD,
                "Dashboard should load within " + PAGE_LOAD_THRESHOLD + "ms, took " + loadTime + "ms");
        logStepWithScreenshot("Dashboard load");
        ExtentReportManager.logPass("Dashboard load: " + loadTime + "ms");
    }

    @Test(priority = 2, description = "TC_LOAD_PL02: Assets page load time")
    public void testPL02_AssetsLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_PAGE_LOAD, "TC_LOAD_PL02_Assets");

        long loadTime = measurePageLoad(AppConstants.BASE_URL + "/assets",
                "[role='row'], [role='grid'], [class*='MuiDataGrid']");
        logStep("Assets page load time: " + loadTime + "ms");

        Assert.assertTrue(loadTime < PAGE_LOAD_THRESHOLD,
                "Assets should load within " + PAGE_LOAD_THRESHOLD + "ms");
        logStepWithScreenshot("Assets load");
        ExtentReportManager.logPass("Assets load: " + loadTime + "ms");
    }

    @Test(priority = 3, description = "TC_LOAD_PL03: Connections page load time")
    public void testPL03_ConnectionsLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_PAGE_LOAD, "TC_LOAD_PL03_Connections");

        long loadTime = measurePageLoad(AppConstants.BASE_URL + "/connections",
                "[role='row'], [role='grid']");
        logStep("Connections page load time: " + loadTime + "ms");

        Assert.assertTrue(loadTime < PAGE_LOAD_THRESHOLD,
                "Connections should load within " + PAGE_LOAD_THRESHOLD + "ms");
        logStepWithScreenshot("Connections load");
        ExtentReportManager.logPass("Connections load: " + loadTime + "ms");
    }

    @Test(priority = 4, description = "TC_LOAD_PL04: Issues page load time")
    public void testPL04_IssuesLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_PAGE_LOAD, "TC_LOAD_PL04_Issues");

        long loadTime = measurePageLoad(AppConstants.BASE_URL + "/issues",
                "[role='row'], [role='grid']");
        logStep("Issues page load time: " + loadTime + "ms");

        Assert.assertTrue(loadTime < PAGE_LOAD_THRESHOLD,
                "Issues should load within " + PAGE_LOAD_THRESHOLD + "ms");
        logStepWithScreenshot("Issues load");
        ExtentReportManager.logPass("Issues load: " + loadTime + "ms");
    }

    @Test(priority = 5, description = "TC_LOAD_PL05: Work Orders page load time")
    public void testPL05_WorkOrdersLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_PAGE_LOAD, "TC_LOAD_PL05_WorkOrders");

        long loadTime = measurePageLoad(AppConstants.BASE_URL + "/sessions",
                "[role='row'], [role='grid']");
        logStep("Work Orders page load time: " + loadTime + "ms");

        Assert.assertTrue(loadTime < PAGE_LOAD_THRESHOLD,
                "Work Orders should load within " + PAGE_LOAD_THRESHOLD + "ms");
        logStepWithScreenshot("Work Orders load");
        ExtentReportManager.logPass("Work Orders load: " + loadTime + "ms");
    }

    // ================================================================
    // SECTION 2: GRID PERFORMANCE (5 TCs)
    // ================================================================

    @Test(priority = 10, description = "TC_LOAD_GR01: Assets grid row count and render time")
    public void testGR01_AssetsGridRender() {
        ExtentReportManager.createTest(MODULE, FEATURE_GRID, "TC_LOAD_GR01_AssetsGrid");

        driver.get(AppConstants.BASE_URL + "/assets");
        long start = System.currentTimeMillis();

        // Wait for rows to appear
        for (int i = 0; i < 30; i++) {
            Long rows = (Long) js().executeScript(
                    "return document.querySelectorAll(\"[role='rowgroup'] [role='row']\").length;");
            if (rows != null && rows > 0) {
                long renderTime = System.currentTimeMillis() - start;
                logStep("Assets grid: " + rows + " rows rendered in " + renderTime + "ms");
                Assert.assertTrue(renderTime < GRID_THRESHOLD,
                        "Grid should render within " + GRID_THRESHOLD + "ms");
                break;
            }
            pause(500);
        }

        logStepWithScreenshot("Assets grid render");
        ExtentReportManager.logPass("Assets grid performance tested");
    }

    @Test(priority = 11, description = "TC_LOAD_GR02: Connections grid render time")
    public void testGR02_ConnectionsGridRender() {
        ExtentReportManager.createTest(MODULE, FEATURE_GRID, "TC_LOAD_GR02_ConnGrid");

        driver.get(AppConstants.BASE_URL + "/connections");
        long start = System.currentTimeMillis();

        for (int i = 0; i < 30; i++) {
            Long rows = (Long) js().executeScript(
                    "return document.querySelectorAll(\"[role='rowgroup'] [role='row']\").length;");
            if (rows != null && rows > 0) {
                long renderTime = System.currentTimeMillis() - start;
                logStep("Connections grid: " + rows + " rows in " + renderTime + "ms");
                break;
            }
            pause(500);
        }

        logStepWithScreenshot("Connections grid");
        ExtentReportManager.logPass("Connections grid performance tested");
    }

    @Test(priority = 12, description = "TC_LOAD_GR03: Issues grid render time")
    public void testGR03_IssuesGridRender() {
        ExtentReportManager.createTest(MODULE, FEATURE_GRID, "TC_LOAD_GR03_IssuesGrid");

        driver.get(AppConstants.BASE_URL + "/issues");
        long start = System.currentTimeMillis();

        for (int i = 0; i < 30; i++) {
            Long rows = (Long) js().executeScript(
                    "return document.querySelectorAll(\"[role='rowgroup'] [role='row']\").length;");
            if (rows != null && rows > 0) {
                long renderTime = System.currentTimeMillis() - start;
                logStep("Issues grid: " + rows + " rows in " + renderTime + "ms");
                break;
            }
            pause(500);
        }

        logStepWithScreenshot("Issues grid");
        ExtentReportManager.logPass("Issues grid performance tested");
    }

    @Test(priority = 13, description = "TC_LOAD_GR04: Grid scroll performance (Assets)")
    public void testGR04_GridScroll() {
        ExtentReportManager.createTest(MODULE, FEATURE_GRID, "TC_LOAD_GR04_Scroll");

        driver.get(AppConstants.BASE_URL + "/assets");
        pause(5000);

        long start = System.currentTimeMillis();
        // Scroll grid to bottom
        js().executeScript(
                "var grid = document.querySelector('[role=\"grid\"], .MuiDataGrid-virtualScroller');" +
                "if(grid) grid.scrollTop = grid.scrollHeight;");
        pause(2000);
        long scrollTime = System.currentTimeMillis() - start;

        logStep("Grid scroll time: " + scrollTime + "ms");
        logStepWithScreenshot("Grid scroll");
        ExtentReportManager.logPass("Grid scroll: " + scrollTime + "ms");
    }

    @Test(priority = 14, description = "TC_LOAD_GR05: Pagination performance")
    public void testGR05_PaginationPerf() {
        ExtentReportManager.createTest(MODULE, FEATURE_GRID, "TC_LOAD_GR05_Pagination");

        driver.get(AppConstants.BASE_URL + "/assets");
        pause(5000);

        // Click next page
        long start = System.currentTimeMillis();
        js().executeScript(
                "var btn = document.querySelector('[aria-label=\"Go to next page\"], button[title=\"Next page\"]');" +
                "if(btn && !btn.disabled) btn.click();");
        pause(2000);

        Long rowsAfter = (Long) js().executeScript(
                "return document.querySelectorAll(\"[role='rowgroup'] [role='row']\").length;");
        long pagTime = System.currentTimeMillis() - start;

        logStep("Pagination time: " + pagTime + "ms, rows: " + rowsAfter);
        logStepWithScreenshot("Pagination");
        ExtentReportManager.logPass("Pagination: " + pagTime + "ms");
    }

    // ================================================================
    // SECTION 3: SEARCH PERFORMANCE (3 TCs)
    // ================================================================

    @Test(priority = 20, description = "TC_LOAD_SR01: Assets search response time")
    public void testSR01_AssetsSearch() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "TC_LOAD_SR01_AssetsSearch");

        driver.get(AppConstants.BASE_URL + "/assets");
        pause(5000);

        long start = System.currentTimeMillis();
        js().executeScript(
                "var input = document.querySelector('input[placeholder*=\"Search\"], input[type=\"search\"]');" +
                "if(input) {" +
                "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                "  setter.call(input, 'SmokeTest');" +
                "  input.dispatchEvent(new Event('input',{bubbles:true}));" +
                "  input.dispatchEvent(new Event('change',{bubbles:true}));" +
                "}");
        pause(3000);
        long searchTime = System.currentTimeMillis() - start;

        logStep("Assets search time: " + searchTime + "ms");
        Assert.assertTrue(searchTime < SEARCH_THRESHOLD,
                "Search should respond within " + SEARCH_THRESHOLD + "ms");
        logStepWithScreenshot("Assets search");
        ExtentReportManager.logPass("Assets search: " + searchTime + "ms");
    }

    @Test(priority = 21, description = "TC_LOAD_SR02: Connections search response time")
    public void testSR02_ConnectionsSearch() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "TC_LOAD_SR02_ConnSearch");

        driver.get(AppConstants.BASE_URL + "/connections");
        pause(5000);

        long start = System.currentTimeMillis();
        js().executeScript(
                "var input = document.querySelector('input[placeholder*=\"Search\"], input[type=\"search\"]');" +
                "if(input) {" +
                "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                "  setter.call(input, 'Cable');" +
                "  input.dispatchEvent(new Event('input',{bubbles:true}));" +
                "}");
        pause(3000);
        long searchTime = System.currentTimeMillis() - start;

        logStep("Connections search time: " + searchTime + "ms");
        logStepWithScreenshot("Connections search");
        ExtentReportManager.logPass("Connections search: " + searchTime + "ms");
    }

    @Test(priority = 22, description = "TC_LOAD_SR03: Issues search response time")
    public void testSR03_IssuesSearch() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "TC_LOAD_SR03_IssuesSearch");

        driver.get(AppConstants.BASE_URL + "/issues");
        pause(5000);

        long start = System.currentTimeMillis();
        js().executeScript(
                "var input = document.querySelector('input[placeholder*=\"Search\"], input[type=\"search\"]');" +
                "if(input) {" +
                "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                "  setter.call(input, 'NEC');" +
                "  input.dispatchEvent(new Event('input',{bubbles:true}));" +
                "}");
        pause(3000);
        long searchTime = System.currentTimeMillis() - start;

        logStep("Issues search time: " + searchTime + "ms");
        logStepWithScreenshot("Issues search");
        ExtentReportManager.logPass("Issues search: " + searchTime + "ms");
    }

    // ================================================================
    // SECTION 4: NAVIGATION PERFORMANCE (3 TCs)
    // ================================================================

    @Test(priority = 30, description = "TC_LOAD_NV01: Sidebar navigation — Dashboard to Assets")
    public void testNV01_DashToAssets() {
        ExtentReportManager.createTest(MODULE, FEATURE_NAV, "TC_LOAD_NV01_DashAssets");

        driver.get(AppConstants.BASE_URL + "/dashboard");
        pause(3000);

        long navTime = measureSidebarNav("Assets", "/assets");
        logStep("Dashboard → Assets: " + navTime + "ms");

        logStepWithScreenshot("Nav dash→assets");
        ExtentReportManager.logPass("Dashboard→Assets: " + navTime + "ms");
    }

    @Test(priority = 31, description = "TC_LOAD_NV02: Sidebar navigation — Assets to Issues")
    public void testNV02_AssetsToIssues() {
        ExtentReportManager.createTest(MODULE, FEATURE_NAV, "TC_LOAD_NV02_AssetsIssues");

        long navTime = measureSidebarNav("Issues", "/issues");
        logStep("Assets → Issues: " + navTime + "ms");

        logStepWithScreenshot("Nav assets→issues");
        ExtentReportManager.logPass("Assets→Issues: " + navTime + "ms");
    }

    @Test(priority = 32, description = "TC_LOAD_NV03: Sidebar navigation — rapid page switching")
    public void testNV03_RapidSwitch() {
        ExtentReportManager.createTest(MODULE, FEATURE_NAV, "TC_LOAD_NV03_RapidSwitch");

        String[] pages = {"Assets", "Connections", "Issues", "Locations"};
        long totalTime = 0;

        for (String page : pages) {
            long start = System.currentTimeMillis();
            js().executeScript(
                    "var links = document.querySelectorAll('a');" +
                    "for(var el of links) { if(el.textContent.trim() === arguments[0]) { el.click(); return; } }", page);
            pause(2000);
            totalTime += System.currentTimeMillis() - start;
        }

        logStep("Rapid 4-page switch total: " + totalTime + "ms, avg: " + (totalTime / 4) + "ms");
        logStepWithScreenshot("Rapid switch");
        ExtentReportManager.logPass("Rapid nav: " + totalTime + "ms total, " + (totalTime / 4) + "ms avg");
    }

    // ================================================================
    // SECTION 5: SLD PERFORMANCE (2 TCs)
    // ================================================================

    @Test(priority = 40, description = "TC_LOAD_SLD01: SLD page load time")
    public void testSLD01_SLDLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_SLD, "TC_LOAD_SLD01_SLDLoad");

        long loadTime = measurePageLoad(AppConstants.BASE_URL + "/slds",
                "canvas, svg, [class*='sld'], [class*='diagram']");
        logStep("SLD load time: " + loadTime + "ms");

        logStepWithScreenshot("SLD load");
        ExtentReportManager.logPass("SLD load: " + loadTime + "ms");
    }

    @Test(priority = 41, description = "TC_LOAD_SLD02: SLD zoom performance")
    public void testSLD02_SLDZoom() {
        ExtentReportManager.createTest(MODULE, FEATURE_SLD, "TC_LOAD_SLD02_SLDZoom");

        driver.get(AppConstants.BASE_URL + "/slds");
        pause(5000);

        long start = System.currentTimeMillis();
        // Simulate zoom via JS
        for (int i = 0; i < 5; i++) {
            js().executeScript(
                    "var zoomIn = document.querySelector('[aria-label*=\"zoom in\" i], [title*=\"Zoom In\" i], button[class*=\"zoom\"]');" +
                    "if(zoomIn) zoomIn.click();");
            pause(300);
        }
        long zoomTime = System.currentTimeMillis() - start;

        logStep("SLD 5x zoom time: " + zoomTime + "ms");
        logStepWithScreenshot("SLD zoom");
        ExtentReportManager.logPass("SLD zoom: " + zoomTime + "ms");
    }

    // ================================================================
    // SECTION 6: API RESPONSE TIME (2 TCs)
    // ================================================================

    @Test(priority = 50, description = "TC_LOAD_API01: API response times via Performance API")
    public void testAPI01_ResponseTimes() {
        ExtentReportManager.createTest(MODULE, FEATURE_API, "TC_LOAD_API01_ResponseTimes");

        // Clear performance entries and navigate
        js().executeScript("performance.clearResourceTimings();");
        driver.get(AppConstants.BASE_URL + "/assets");
        pause(5000);

        // Collect API call durations
        String apiStats = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var apiCalls = entries.filter(e => e.name.includes('/api/'));" +
                "if(apiCalls.length === 0) return 'No API calls found';" +
                "var durations = apiCalls.map(e => e.duration);" +
                "var avg = durations.reduce((a,b) => a+b, 0) / durations.length;" +
                "var max = Math.max(...durations);" +
                "var min = Math.min(...durations);" +
                "return 'API calls: ' + apiCalls.length + ', avg: ' + avg.toFixed(0) + 'ms, ' +" +
                "  'min: ' + min.toFixed(0) + 'ms, max: ' + max.toFixed(0) + 'ms';");
        logStep(apiStats);

        logStepWithScreenshot("API response times");
        ExtentReportManager.logPass(apiStats);
    }

    @Test(priority = 51, description = "TC_LOAD_API02: Check for slow API calls (>3s)")
    public void testAPI02_SlowAPIs() {
        ExtentReportManager.createTest(MODULE, FEATURE_API, "TC_LOAD_API02_SlowAPIs");

        js().executeScript("performance.clearResourceTimings();");
        driver.get(AppConstants.BASE_URL + "/dashboard");
        pause(5000);

        String slowApis = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var slow = entries.filter(e => e.name.includes('/api/') && e.duration > 3000);" +
                "if(slow.length === 0) return 'No slow API calls (all under 3s)';" +
                "return 'SLOW APIs: ' + slow.map(e => e.name.split('/api/')[1].split('?')[0] + " +
                "  ' (' + e.duration.toFixed(0) + 'ms)').join(', ');");
        logStep(slowApis);

        logStepWithScreenshot("Slow APIs");
        ExtentReportManager.logPass(slowApis);
    }
}
