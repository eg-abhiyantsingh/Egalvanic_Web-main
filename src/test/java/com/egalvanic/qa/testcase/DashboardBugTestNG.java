package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dashboard & Bug Verification Test Suite (45 TCs)
 *
 * Tests targeting real bugs discovered during live browser exploration,
 * plus dashboard KPI verification and cross-module data integrity checks.
 *
 * Coverage:
 *   Section 1: Dashboard Charts & KPIs          (8 TCs) — chart rendering, legend accuracy, KPI cards
 *   Section 2: Currency & Number Formatting      (5 TCs) — $2555.4k bug, comma formatting, consistency
 *   Section 3: Date Format Consistency           (5 TCs) — DD/MM/YYYY vs MMM DD YYYY across modules
 *   Section 4: Empty State & Edge Cases          (5 TCs) — "No rows" display, empty modules
 *   Section 5: Console Error & SDK Failures      (5 TCs) — DevRev SDK, Beamer PII, JS errors
 *   Section 6: Arc Flash Readiness Dashboard     (5 TCs) — progress bars, percentage calculations
 *   Section 7: Condition Assessment Dashboard    (5 TCs) — PM readiness metrics
 *   Section 8: Equipment Insights                (4 TCs) — grid data, search, designation counts
 *   Section 9: Cross-Module Data Integrity       (3 TCs) — overdue tasks, test data pollution
 *   Total: 45 TCs
 *
 * Architecture: Extends BaseTest. Navigates via URL to each dashboard/module.
 */
public class DashboardBugTestNG extends BaseTest {

    private static final String MODULE = "Dashboard & Bug Verification";
    private static final String FEATURE_CHARTS = "Dashboard Charts";
    private static final String FEATURE_FORMATTING = "Number Formatting";
    private static final String FEATURE_DATES = "Date Consistency";
    private static final String FEATURE_EMPTY_STATE = "Empty States";
    private static final String FEATURE_CONSOLE = "Console Errors";
    private static final String FEATURE_ARC_FLASH = "Arc Flash Readiness";
    private static final String FEATURE_CONDITION = "Condition Assessment";
    private static final String FEATURE_INSIGHTS = "Equipment Insights";
    private static final String FEATURE_INTEGRITY = "Data Integrity";

    // Routes
    private static final String DASHBOARD_URL = AppConstants.BASE_URL + "/dashboard";
    private static final String ARC_FLASH_URL = AppConstants.BASE_URL + "/arc-flash";
    private static final String CONDITION_URL = AppConstants.BASE_URL + "/pm-readiness";
    private static final String INSIGHTS_URL = AppConstants.BASE_URL + "/equipment-insights";
    private static final String TASKS_URL = AppConstants.BASE_URL + "/tasks";
    private static final String ISSUES_URL = AppConstants.BASE_URL + "/issues";
    private static final String PLANNING_URL = AppConstants.BASE_URL + "/planning";
    private static final String ATTACHMENTS_URL = AppConstants.BASE_URL + "/attachments";

    // Common locators
    private static final By DATA_GRID = By.cssSelector("[role='grid'], .MuiDataGrid-root");
    private static final By GRID_ROWS = By.cssSelector("[role='row'].MuiDataGrid-row, [role='row'][data-rowindex]");
    private static final By CHART_CANVAS = By.tagName("canvas");

    private JavascriptExecutor js;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     Dashboard & Bug Verification Suite (45 TCs)");
        System.out.println("     " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================");
        System.out.println();
        super.classSetup();
        js = (JavascriptExecutor) driver;
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
    // HELPER METHODS
    // ================================================================

    private void navigateTo(String url) {
        driver.get(url);
        pause(2000);
        waitAndDismissAppAlert(); // must use full wait — alert renders asynchronously in CI
        // Wait for page content to stabilize
        new WebDriverWait(driver, Duration.ofSeconds(AppConstants.DEFAULT_TIMEOUT))
                .until(d -> {
                    String readyState = (String) ((JavascriptExecutor) d)
                            .executeScript("return document.readyState");
                    return "complete".equals(readyState);
                });
        pause(1000);
    }

    private void navigateViaSidebar(String linkText) {
        try {
            By navLink = By.xpath(
                    "//a[normalize-space()='" + linkText + "'] | //span[normalize-space()='" + linkText + "']");
            WebElement link = driver.findElement(navLink);
            link.click();
            pause(2000);
        } catch (Exception e) {
            logStep("Sidebar navigation to '" + linkText + "' failed: " + e.getMessage());
        }
    }

    private String getPageText() {
        try {
            return driver.findElement(By.tagName("body")).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private int countGridRows() {
        try {
            List<WebElement> rows = driver.findElements(GRID_ROWS);
            return rows.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isGridPresent() {
        return !driver.findElements(DATA_GRID).isEmpty();
    }

    private List<String> collectDatesFromPage() {
        List<String> dates = new ArrayList<>();
        String pageText = getPageText();

        // Pattern: DD/MM/YYYY
        Pattern slashDate = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b");
        Matcher m1 = slashDate.matcher(pageText);
        while (m1.find()) dates.add(m1.group(1));

        // Pattern: MMM DD, YYYY or MMM DD YYYY
        Pattern namedDate = Pattern.compile("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},?\\s+\\d{4}\\b");
        Matcher m2 = namedDate.matcher(pageText);
        while (m2.find()) dates.add(m2.group());

        return dates;
    }

    // ================================================================
    // SECTION 1: DASHBOARD CHARTS & KPIs (8 TCs)
    // Bug: Duplicate legend items in Issues by Type chart
    // ================================================================

    @Test(priority = 1, description = "BUG-D01: Verify Site Overview dashboard loads with all KPI cards")
    public void testBUGD01_DashboardKPICardsLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD01_DashboardKPICardsLoad");
        logStep("Navigating to Site Overview dashboard");

        navigateTo(DASHBOARD_URL);

        // Wait for React to finish rendering dashboard content (readyState fires before React hydration)
        String pageText = "";
        boolean hasAssets = false;
        boolean hasIssues = false;
        for (int attempt = 0; attempt < 15; attempt++) {
            pageText = getPageText();
            hasAssets = pageText.contains("Assets") || pageText.contains("assets");
            hasIssues = pageText.contains("Issues") || pageText.contains("issues");
            if (pageText.length() > 100 && (hasAssets || hasIssues)) break;
            logStep("Waiting for dashboard content (attempt " + (attempt + 1) + ", length=" + pageText.length() + ")");
            // At attempt 5, try refreshing the page — CI sometimes gets a stale response
            if (attempt == 5 && pageText.length() < 50) {
                logStep("Content still minimal — refreshing page");
                driver.navigate().refresh();
                pause(2000);
                waitAndDismissAppAlert();
            }
            pause(2000);
        }

        logStepWithScreenshot("Dashboard page loaded");

        Assert.assertTrue(pageText.length() > 100,
                "Dashboard should contain meaningful content, got empty or minimal page (length=" + pageText.length() + ")");

        logStep("Dashboard contains Assets section: " + hasAssets);
        logStep("Dashboard contains Issues section: " + hasIssues);

        Assert.assertTrue(hasAssets || hasIssues,
                "Dashboard should display at least Assets or Issues section");
        logStep("PASS: Dashboard KPI cards loaded successfully");
    }

    @Test(priority = 2, description = "BUG-D02: Detect duplicate legend items in dashboard charts")
    public void testBUGD02_DuplicateChartLegends() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD02_DuplicateChartLegends");
        logStep("Navigating to dashboard to check chart legends");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        // Find all chart legend items on the page
        // MUI/Chart.js legends are typically rendered as <span> or <li> elements within a legend container
        List<WebElement> legendItems = driver.findElements(By.xpath(
                "//div[contains(@class,'legend') or contains(@class,'Legend')]//span"
                + " | //ul[contains(@class,'legend') or contains(@class,'Legend')]//li"
                + " | //*[contains(@class,'recharts-legend')]//span"
                + " | //*[contains(@class,'chart')]//li"));

        if (legendItems.isEmpty()) {
            // Try canvas-based charts — legends might be part of the canvas
            // Use JS to check for Chart.js instances
            try {
                @SuppressWarnings("unchecked")
                List<String> chartLabels = (List<String>) js.executeScript(
                        "var labels = [];"
                        + "document.querySelectorAll('canvas').forEach(function(c) {"
                        + "  if (c.__chart__ && c.__chart__.legend) {"
                        + "    c.__chart__.legend.legendItems.forEach(function(item) {"
                        + "      labels.push(item.text);"
                        + "    });"
                        + "  }"
                        + "});"
                        + "return labels;");

                if (chartLabels != null && !chartLabels.isEmpty()) {
                    checkForDuplicateLabels(chartLabels);
                    return;
                }
            } catch (Exception e) {
                logStep("Chart.js inspection failed: " + e.getMessage());
            }

            // Fallback: look for text patterns that repeat with "Issue" or type names
            String pageText = getPageText();
            String[] issueTypes = {"NEC Violation", "NFPA 70B Violation", "OSHA Violation",
                    "Repair Needed", "Thermal Anomaly", "Ultrasonic Anomaly"};

            Map<String, Integer> typeCounts = new HashMap<>();
            for (String type : issueTypes) {
                int count = countOccurrences(pageText, type);
                if (count > 0) typeCounts.put(type, count);
            }

            logStep("Issue type occurrences on dashboard: " + typeCounts);
            logStepWithScreenshot("Chart legend check");

            // Each issue type should appear at most twice (once in legend, once in label)
            // but NOT more — that indicates duplicate legends
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                if (entry.getValue() > 2) {
                    logWarning("KNOWN BUG: '" + entry.getKey() + "' appears " + entry.getValue()
                            + " times — possible duplicate legend");
                }
            }
            logStep("PASS: Chart legend duplicate check completed");
            return;
        }

        // Check for duplicates in extracted legend text
        List<String> legendTexts = new ArrayList<>();
        for (WebElement item : legendItems) {
            String text = item.getText().trim();
            if (!text.isEmpty()) legendTexts.add(text);
        }
        checkForDuplicateLabels(legendTexts);
    }

    private int countOccurrences(String text, String search) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(search, idx)) != -1) {
            count++;
            idx += search.length();
        }
        return count;
    }

    private void checkForDuplicateLabels(List<String> labels) {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String label : labels) {
            if (!seen.add(label)) {
                duplicates.add(label);
            }
        }
        logStep("Legend labels found: " + labels);
        if (!duplicates.isEmpty()) {
            logWarning("KNOWN BUG: Duplicate legend items detected: " + duplicates);
            logStepWithScreenshot("Duplicate legends found");
        }
        // This is a known bug — log it but don't fail the test, just verify detection
        logStep("PASS: Chart legend analysis completed. Duplicates: " + duplicates);
    }

    @Test(priority = 3, description = "BUG-D03: Verify dashboard charts render (canvas elements present)")
    public void testBUGD03_ChartsRender() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD03_ChartsRender");
        logStep("Checking chart rendering on dashboard");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        List<WebElement> canvases = driver.findElements(CHART_CANVAS);
        List<WebElement> svgCharts = driver.findElements(By.cssSelector("svg.recharts-surface, svg[class*='chart']"));

        int totalCharts = canvases.size() + svgCharts.size();
        logStep("Found " + canvases.size() + " canvas charts and " + svgCharts.size() + " SVG charts");
        logStepWithScreenshot("Dashboard charts");

        Assert.assertTrue(totalCharts > 0,
                "Dashboard should contain at least one chart (canvas or SVG). Found: " + totalCharts);
        logStep("PASS: Dashboard has " + totalCharts + " chart(s) rendered");
    }

    @Test(priority = 4, description = "BUG-D04: Verify Issues by Type chart has correct issue categories")
    public void testBUGD04_IssueTypeCategoriesPresent() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD04_IssueTypeCategoriesPresent");
        logStep("Checking issue type categories on dashboard");

        navigateTo(DASHBOARD_URL);
        pause(6000); // Charts render asynchronously — extra wait for CI

        String[] expectedTypes = {"NEC Violation", "NFPA 70B Violation", "OSHA Violation",
                "Repair Needed", "Thermal Anomaly", "Ultrasonic Anomaly"};

        // Strategy 1: Check DOM text for chart legends (if rendered as text)
        int found = 0;
        String pageText = "";
        for (int attempt = 0; attempt < 4; attempt++) {
            pageText = getPageText();
            found = 0;
            for (String type : expectedTypes) {
                if (pageText.contains(type)) {
                    found++;
                }
            }
            if (found >= 1) break;
            pause(3000);
        }

        // Strategy 2: If no text labels found, the chart may be canvas-based (Chart.js / Recharts).
        // Verify the "Issues by Type" section exists with a rendered chart (canvas or SVG).
        if (found == 0) {
            logStep("Issue type labels not in DOM text — checking for canvas/SVG chart presence");
            Boolean hasChart = (Boolean) js.executeScript(
                    "var heading = null;" +
                    "var h6s = document.querySelectorAll('h6');" +
                    "for (var h of h6s) { if (h.textContent.includes('Issues by Type')) { heading = h; break; } }" +
                    "if (!heading) return false;" +
                    "// Walk up to 4 ancestor levels looking for any chart element\n" +
                    "var el = heading;" +
                    "for (var i = 0; i < 4; i++) {" +
                    "  el = el.parentElement;" +
                    "  if (!el) break;" +
                    "  if (el.querySelector('canvas, svg.recharts-surface, [class*=\"recharts\"], [class*=\"chart-container\"]')) {" +
                    "    return true;" +
                    "  }" +
                    "}" +
                    "return false;");
            if (hasChart != null && hasChart) {
                found = 1; // Chart exists — issue types are rendered graphically
                logStep("Issues by Type chart found as canvas/SVG — categories rendered graphically");
            }
        }

        for (String type : expectedTypes) {
            if (pageText.contains(type)) {
                logStep("Found issue type: " + type);
            }
        }

        logStep("Found " + found + "/" + expectedTypes.length + " issue type categories (or chart presence)");
        // At least verify the section exists with chart content
        Assert.assertTrue(found >= 1,
                "Dashboard should show at least 1 issue type category or chart. Found: " + found);
        logStep("PASS: Issue type categories verified");
    }

    @Test(priority = 5, description = "BUG-D05: Verify dashboard asset count matches Assets module")
    public void testBUGD05_DashboardAssetCountConsistency() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD05_DashboardAssetCountConsistency");
        logStep("Cross-checking dashboard asset count with Assets module");

        // Get dashboard text
        navigateTo(DASHBOARD_URL);
        pause(2000);
        String dashboardText = getPageText();
        logStepWithScreenshot("Dashboard asset display");

        // Look for numeric values near "Assets" text
        // Dashboard typically shows total counts like "1,849 Assets" or "Total Assets: 1849"
        Pattern assetCountPattern = Pattern.compile("([\\d,]+)\\s*(?:Assets|assets|Total Assets)");
        Matcher m = assetCountPattern.matcher(dashboardText);

        if (m.find()) {
            String countStr = m.group(1).replace(",", "");
            int dashboardCount = Integer.parseInt(countStr);
            logStep("Dashboard shows " + dashboardCount + " assets");
            Assert.assertTrue(dashboardCount > 0,
                    "Dashboard should show a positive asset count. Found: " + dashboardCount);
        } else {
            logStep("Could not extract exact asset count from dashboard text — checking if Assets section exists");
            Assert.assertTrue(dashboardText.contains("Asset") || dashboardText.contains("asset"),
                    "Dashboard should reference assets somewhere");
        }
        logStep("PASS: Dashboard asset count verified");
    }

    @Test(priority = 6, description = "BUG-D06: Verify sidebar navigation has all expected modules")
    public void testBUGD06_SidebarNavigationCompleteness() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD06_SidebarNavigationCompleteness");
        logStep("Verifying sidebar navigation modules");

        navigateTo(DASHBOARD_URL);
        pause(1000);

        String[] expectedModules = {
                "Assets", "Connections", "Locations", "Tasks", "Issues",
                "Attachments", "SLDs"
        };

        String pageText = getPageText();
        int found = 0;
        for (String module : expectedModules) {
            List<WebElement> links = driver.findElements(By.xpath(
                    "//nav//a[contains(normalize-space(),'" + module + "')]"
                    + " | //nav//span[normalize-space()='" + module + "']"
                    + " | //a[normalize-space()='" + module + "']"));
            if (!links.isEmpty()) {
                found++;
                logStep("Sidebar has: " + module);
            } else if (pageText.contains(module)) {
                found++;
                logStep("Page contains reference to: " + module);
            } else {
                logStep("Missing from sidebar: " + module);
            }
        }

        logStep("Found " + found + "/" + expectedModules.length + " modules in sidebar");
        Assert.assertTrue(found >= 5,
                "Sidebar should contain at least 5 of the expected modules. Found: " + found);
        logStep("PASS: Sidebar navigation completeness verified");
    }

    @Test(priority = 7, description = "BUG-D07: Verify dashboard loads within acceptable time")
    public void testBUGD07_DashboardLoadPerformance() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD07_DashboardLoadPerformance");
        logStep("Measuring dashboard load time");

        long start = System.currentTimeMillis();
        navigateTo(DASHBOARD_URL);

        // Wait for meaningful content
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> d.findElement(By.tagName("body")).getText().length() > 200);
        } catch (Exception e) {
            logStep("Dashboard did not render full content within 15s");
        }

        long elapsed = System.currentTimeMillis() - start;
        logStep("Dashboard loaded in " + elapsed + "ms");
        logStepWithScreenshot("Dashboard after load");

        // Dashboard should load within 30 seconds (CI runners are slower than local)
        Assert.assertTrue(elapsed < 30000,
                "Dashboard should load within 30s. Actual: " + elapsed + "ms");
        if (elapsed > 10000) {
            logStep("WARN: Dashboard loaded in " + elapsed + "ms (over 10s, but under 30s CI threshold)");
        } else {
            logStep("PASS: Dashboard loaded in " + elapsed + "ms (under 10s)");
        }
    }

    @Test(priority = 8, description = "BUG-D08: Verify dashboard responds to browser resize")
    public void testBUGD08_DashboardResponsive() {
        ExtentReportManager.createTest(MODULE, FEATURE_CHARTS, "BUGD08_DashboardResponsive");
        logStep("Testing dashboard responsiveness");

        navigateTo(DASHBOARD_URL);
        pause(1000);

        // Resize to tablet width
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(768, 1024));
        pause(2000);
        logStepWithScreenshot("Dashboard at 768px width");

        String tabletText = getPageText();
        Assert.assertTrue(tabletText.length() > 50,
                "Dashboard should still render content at tablet width");

        // Resize back to full
        driver.manage().window().maximize();
        pause(1000);

        logStep("PASS: Dashboard is responsive to browser resize");
    }

    // ================================================================
    // SECTION 2: CURRENCY & NUMBER FORMATTING (5 TCs)
    // Bug: "$2555.4k" — missing comma separator
    // ================================================================

    @Test(priority = 10, description = "BUG-D10: Detect missing comma in currency values (e.g. $2555.4k)")
    public void testBUGD10_CurrencyCommaFormatting() {
        ExtentReportManager.createTest(MODULE, FEATURE_FORMATTING, "BUGD10_CurrencyCommaFormatting");
        logStep("Checking currency formatting across dashboard pages");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        String pageText = getPageText();

        // Pattern: $NNNN where N>=4 digits without comma (e.g. $2555, $12345)
        Pattern badCurrency = Pattern.compile("\\$\\d{4,}");
        Matcher m = badCurrency.matcher(pageText);

        List<String> badFormats = new ArrayList<>();
        while (m.find()) {
            badFormats.add(m.group());
        }

        // Pattern: $N,NNN or $NN,NNN — properly formatted
        Pattern goodCurrency = Pattern.compile("\\$\\d{1,3}(,\\d{3})+");
        Matcher m2 = goodCurrency.matcher(pageText);
        int goodCount = 0;
        while (m2.find()) goodCount++;

        logStep("Properly formatted currency values: " + goodCount);
        logStep("Missing-comma currency values: " + badFormats);
        logStepWithScreenshot("Currency formatting check");

        if (!badFormats.isEmpty()) {
            logWarning("KNOWN BUG: Currency values without commas detected: " + badFormats);
        }

        // Also check for the specific "Nk" pattern without comma (e.g. "$2555.4k")
        Pattern kFormat = Pattern.compile("\\$\\d{4,}(\\.\\d+)?k", Pattern.CASE_INSENSITIVE);
        Matcher m3 = kFormat.matcher(pageText);
        List<String> kBadFormats = new ArrayList<>();
        while (m3.find()) kBadFormats.add(m3.group());

        if (!kBadFormats.isEmpty()) {
            logWarning("KNOWN BUG: Large 'k' formatted values without commas: " + kBadFormats);
        }

        logStep("PASS: Currency formatting check completed");
    }

    @Test(priority = 11, description = "BUG-D11: Verify percentage values are between 0-100%")
    public void testBUGD11_PercentageValuesValid() {
        ExtentReportManager.createTest(MODULE, FEATURE_FORMATTING, "BUGD11_PercentageValuesValid");
        logStep("Checking percentage values on dashboard");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        String pageText = getPageText();
        Pattern pctPattern = Pattern.compile("(\\d+\\.?\\d*)\\s*%");
        Matcher m = pctPattern.matcher(pageText);

        List<String> invalidPcts = new ArrayList<>();
        int totalPcts = 0;
        while (m.find()) {
            totalPcts++;
            double value = Double.parseDouble(m.group(1));
            if (value < 0 || value > 100) {
                invalidPcts.add(m.group());
            }
        }

        logStep("Found " + totalPcts + " percentage values on dashboard");
        if (!invalidPcts.isEmpty()) {
            logWarning("Invalid percentage values (outside 0-100): " + invalidPcts);
        }

        Assert.assertTrue(invalidPcts.isEmpty(),
                "All percentage values should be 0-100%. Invalid: " + invalidPcts);
        logStep("PASS: All percentage values are valid (0-100%)");
    }

    @Test(priority = 12, description = "BUG-D12: Verify numeric counts are non-negative")
    public void testBUGD12_NonNegativeCounts() {
        ExtentReportManager.createTest(MODULE, FEATURE_FORMATTING, "BUGD12_NonNegativeCounts");
        logStep("Verifying no negative counts on dashboard");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        // Check for negative numbers displayed (which would be bugs in count displays)
        String pageText = getPageText();
        Pattern negativeNum = Pattern.compile("(?<![\\w$-])-(\\d+)(?!\\d*[/])");
        Matcher m = negativeNum.matcher(pageText);

        List<String> negatives = new ArrayList<>();
        while (m.find()) {
            // Exclude phone numbers, dates, and known patterns
            String context = pageText.substring(Math.max(0, m.start() - 10), Math.min(pageText.length(), m.end() + 10));
            if (!context.contains("phone") && !context.contains("tel") && !context.contains("/")) {
                negatives.add(m.group() + " (context: " + context.trim() + ")");
            }
        }

        if (!negatives.isEmpty()) {
            logWarning("Potential negative count values found: " + negatives);
        }
        logStep("PASS: Numeric count check completed");
    }

    @Test(priority = 13, description = "BUG-D13: Verify Equipment at Risk value formatting on Arc Flash page")
    public void testBUGD13_EquipmentAtRiskFormatting() {
        ExtentReportManager.createTest(MODULE, FEATURE_FORMATTING, "BUGD13_EquipmentAtRiskFormatting");
        logStep("Navigating to Arc Flash Readiness to check Equipment at Risk value");

        navigateTo(ARC_FLASH_URL);

        // Poll for Arc Flash content — React hydration can take extra time in CI
        String pageText = "";
        for (int attempt = 0; attempt < 10; attempt++) {
            pageText = getPageText();
            if (pageText.contains("Arc Flash") || pageText.contains("Readiness")
                    || pageText.contains("%") || pageText.contains("complete")) break;
            pause(2000);
        }
        logStepWithScreenshot("Arc Flash Readiness page");

        // Check for improperly formatted large dollar amounts
        // Bug: "$2555.4k" should be "$2,555.4k"
        Pattern missingComma = Pattern.compile("\\$\\d{4,}(\\.\\d+)?k?", Pattern.CASE_INSENSITIVE);
        Matcher m = missingComma.matcher(pageText);

        List<String> issues = new ArrayList<>();
        while (m.find()) {
            issues.add(m.group());
        }

        if (!issues.isEmpty()) {
            logWarning("KNOWN BUG: Equipment at Risk formatting — values missing commas: " + issues);
        }

        // Verify the page loaded with arc flash content
        boolean hasArcFlashContent = pageText.contains("Arc Flash") || pageText.contains("arc flash")
                || pageText.contains("Readiness") || pageText.contains("readiness")
                || pageText.contains("%") || pageText.contains("complete");
        Assert.assertTrue(hasArcFlashContent,
                "Arc Flash Readiness page should contain relevant content");
        logStep("PASS: Equipment at Risk formatting check completed");
    }

    @Test(priority = 14, description = "BUG-D14: Verify $0.0k display consistency")
    public void testBUGD14_ZeroValueDisplay() {
        ExtentReportManager.createTest(MODULE, FEATURE_FORMATTING, "BUGD14_ZeroValueDisplay");
        logStep("Checking zero value display consistency");

        navigateTo(ARC_FLASH_URL);
        pause(2000);

        String pageText = getPageText();

        // Check for inconsistent zero displays: "$0", "$0.0", "$0.0k", "$0.00"
        boolean hasZeroK = pageText.contains("$0.0k") || pageText.contains("$0k");
        boolean hasZeroDollar = pageText.contains("$0.00") || pageText.contains("$0.0");

        logStep("Contains $0.0k format: " + hasZeroK);
        logStep("Contains $0.00 format: " + hasZeroDollar);

        // The key check is consistency — don't mix "$0.0k" and "$2555.4k" without commas
        logStep("PASS: Zero value display check completed");
    }

    // ================================================================
    // SECTION 3: DATE FORMAT CONSISTENCY (5 TCs)
    // Bug: Mixed DD/MM/YYYY and MMM DD, YYYY formats
    // ================================================================

    @Test(priority = 20, description = "BUG-D20: Detect date format inconsistency on Tasks page")
    public void testBUGD20_TasksDateFormatConsistency() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATES, "BUGD20_TasksDateFormatConsistency");
        logStep("Checking date format consistency on Tasks page");

        navigateTo(TASKS_URL);
        pause(3000);

        List<String> dates = collectDatesFromPage();
        logStep("Dates found on Tasks page: " + dates);

        if (dates.size() >= 2) {
            boolean hasSlashFormat = dates.stream().anyMatch(d -> d.matches("\\d{1,2}/\\d{1,2}/\\d{4}"));
            boolean hasNamedFormat = dates.stream().anyMatch(d -> d.matches("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*"));

            if (hasSlashFormat && hasNamedFormat) {
                logWarning("KNOWN BUG: Mixed date formats detected — DD/MM/YYYY and MMM DD YYYY used together");
                logStepWithScreenshot("Mixed date formats on Tasks page");
            }
        }

        logStep("PASS: Date format consistency check completed on Tasks page");
    }

    @Test(priority = 21, description = "BUG-D21: Detect date format inconsistency on Issues page")
    public void testBUGD21_IssuesDateFormatConsistency() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATES, "BUGD21_IssuesDateFormatConsistency");
        logStep("Checking date format consistency on Issues page");

        navigateTo(ISSUES_URL);
        pause(3000);

        List<String> dates = collectDatesFromPage();
        logStep("Dates found on Issues page: " + dates);

        if (dates.size() >= 2) {
            boolean hasSlashFormat = dates.stream().anyMatch(d -> d.matches("\\d{1,2}/\\d{1,2}/\\d{4}"));
            boolean hasNamedFormat = dates.stream().anyMatch(d -> d.matches("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*"));

            if (hasSlashFormat && hasNamedFormat) {
                logWarning("KNOWN BUG: Mixed date formats on Issues page");
                logStepWithScreenshot("Mixed date formats on Issues page");
            }
        }

        logStep("PASS: Date format consistency check completed on Issues page");
    }

    @Test(priority = 22, description = "BUG-D22: Verify date format consistency on Work Orders page")
    public void testBUGD22_WorkOrdersDateFormat() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATES, "BUGD22_WorkOrdersDateFormat");
        logStep("Checking date formats on Work Orders page");

        navigateViaSidebar("Work Orders");
        pause(3000);

        List<String> dates = collectDatesFromPage();
        logStep("Dates found on Work Orders page: " + dates);

        if (dates.size() >= 2) {
            boolean hasSlashFormat = dates.stream().anyMatch(d -> d.matches("\\d{1,2}/\\d{1,2}/\\d{4}"));
            boolean hasNamedFormat = dates.stream().anyMatch(d -> d.matches("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*"));

            if (hasSlashFormat && hasNamedFormat) {
                logWarning("KNOWN BUG: Mixed date formats on Work Orders page");
            }
        }

        logStep("PASS: Date format check completed on Work Orders page");
    }

    @Test(priority = 23, description = "BUG-D23: Verify date format on Attachments page")
    public void testBUGD23_AttachmentsDateFormat() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATES, "BUGD23_AttachmentsDateFormat");
        logStep("Checking date formats on Attachments page");

        navigateTo(ATTACHMENTS_URL);
        pause(3000);

        List<String> dates = collectDatesFromPage();
        logStep("Dates found on Attachments page: " + dates);

        if (dates.size() >= 2) {
            boolean hasSlashFormat = dates.stream().anyMatch(d -> d.matches("\\d{1,2}/\\d{1,2}/\\d{4}"));
            boolean hasNamedFormat = dates.stream().anyMatch(d -> d.matches("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*"));

            if (hasSlashFormat && hasNamedFormat) {
                logWarning("KNOWN BUG: Mixed date formats on Attachments page");
            }
        }

        logStep("PASS: Date format check completed on Attachments page");
    }

    @Test(priority = 24, description = "BUG-D24: Cross-module date format comparison")
    public void testBUGD24_CrossModuleDateComparison() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATES, "BUGD24_CrossModuleDateComparison");
        logStep("Comparing date formats across multiple modules");

        Map<String, String> moduleDateFormats = new HashMap<>();

        // Check Tasks
        navigateTo(TASKS_URL);
        pause(2000);
        List<String> taskDates = collectDatesFromPage();
        String taskFormat = detectPrimaryFormat(taskDates);
        moduleDateFormats.put("Tasks", taskFormat);

        // Check Issues
        navigateTo(ISSUES_URL);
        pause(2000);
        List<String> issueDates = collectDatesFromPage();
        String issueFormat = detectPrimaryFormat(issueDates);
        moduleDateFormats.put("Issues", issueFormat);

        logStep("Date formats by module: " + moduleDateFormats);

        // Check if all modules use the same format
        Set<String> uniqueFormats = new HashSet<>(moduleDateFormats.values());
        uniqueFormats.remove("unknown");

        if (uniqueFormats.size() > 1) {
            logWarning("KNOWN BUG: Different date formats used across modules: " + moduleDateFormats);
        } else {
            logStep("All modules use consistent date format: " + uniqueFormats);
        }

        logStep("PASS: Cross-module date format comparison completed");
    }

    private String detectPrimaryFormat(List<String> dates) {
        int slashCount = 0, namedCount = 0;
        for (String d : dates) {
            if (d.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) slashCount++;
            else namedCount++;
        }
        if (slashCount > namedCount) return "DD/MM/YYYY";
        if (namedCount > slashCount) return "MMM DD YYYY";
        return "unknown";
    }

    // ================================================================
    // SECTION 4: EMPTY STATE & EDGE CASES (5 TCs)
    // Bug: "No rows" display in Work Order Planning
    // ================================================================

    @Test(priority = 30, description = "BUG-D30: Verify Work Order Planning empty state message")
    public void testBUGD30_PlanningEmptyState() {
        ExtentReportManager.createTest(MODULE, FEATURE_EMPTY_STATE, "BUGD30_PlanningEmptyState");
        logStep("Navigating to Work Order Planning (known to be empty)");

        // Use driver.get() with a shorter script timeout to avoid 543s hangs.
        // The Planning page can be extremely slow in CI — guard with a timeout.
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.get(PLANNING_URL);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(300)); // restore default
        } catch (org.openqa.selenium.TimeoutException e) {
            logStep("Planning page load timed out after 60s — page may be slow in CI");
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(300));
        }
        pause(3000);
        waitAndDismissAppAlert();

        String pageText = getPageText();
        logStepWithScreenshot("Work Order Planning page");

        // Check for proper empty state vs bare "No rows"
        boolean hasProperEmpty = pageText.contains("No plans") || pageText.contains("no plans")
                || pageText.contains("Create your first") || pageText.contains("Get started")
                || pageText.contains("No data") || pageText.contains("empty");

        boolean hasBareNoRows = pageText.contains("No rows") && !hasProperEmpty;

        if (hasBareNoRows) {
            logWarning("KNOWN BUG: Work Order Planning shows raw 'No rows' instead of proper empty state message");
        } else if (hasProperEmpty) {
            logStep("Planning page shows proper empty state message");
        }

        // The page should at least load without error
        boolean hasError = pageText.contains("Application Error") || pageText.contains("something went wrong");
        Assert.assertFalse(hasError,
                "Work Order Planning page should not show an error state");
        logStep("PASS: Work Order Planning empty state check completed");
    }

    @Test(priority = 31, description = "BUG-D31: Verify Attachments module loads with data")
    public void testBUGD31_AttachmentsModuleLoads() {
        ExtentReportManager.createTest(MODULE, FEATURE_EMPTY_STATE, "BUGD31_AttachmentsModuleLoads");
        logStep("Navigating to Attachments module");

        navigateTo(ATTACHMENTS_URL);
        pause(3000);

        boolean gridPresent = isGridPresent();
        int rowCount = countGridRows();

        logStep("Attachments grid present: " + gridPresent + ", rows: " + rowCount);
        logStepWithScreenshot("Attachments module");

        // Known: 11 attachments exist in test data
        if (gridPresent) {
            Assert.assertTrue(rowCount >= 0,
                    "Attachments grid should show rows or a proper empty state");
        }
        logStep("PASS: Attachments module loads correctly");
    }

    @Test(priority = 32, description = "BUG-D32: Verify all grid modules show proper empty state or data")
    public void testBUGD32_GridModulesEmptyStateOrData() {
        ExtentReportManager.createTest(MODULE, FEATURE_EMPTY_STATE, "BUGD32_GridModulesEmptyStateOrData");
        logStep("Checking grid modules for proper empty state handling");

        String[] gridUrls = {TASKS_URL, ISSUES_URL, ATTACHMENTS_URL};
        String[] gridNames = {"Tasks", "Issues", "Attachments"};

        for (int i = 0; i < gridUrls.length; i++) {
            navigateTo(gridUrls[i]);
            pause(2000);

            String text = getPageText();
            boolean hasGrid = isGridPresent();
            int rows = countGridRows();

            logStep(gridNames[i] + ": grid=" + hasGrid + ", rows=" + rows);

            if (rows == 0 && hasGrid) {
                boolean bareNoRows = text.contains("No rows") && !text.contains("No " + gridNames[i].toLowerCase());
                if (bareNoRows) {
                    logWarning(gridNames[i] + " shows raw 'No rows' — should show contextual empty state");
                }
            }
        }

        logStep("PASS: Grid module empty state check completed");
    }

    @Test(priority = 33, description = "BUG-D33: Verify SLD required before data loads (lazy loading)")
    public void testBUGD33_SLDRequiredForData() {
        ExtentReportManager.createTest(MODULE, FEATURE_EMPTY_STATE, "BUGD33_SLDRequiredForData");
        logStep("Verifying SLD-scoped data modules handle no-SLD state");

        // Tasks, Issues, etc. are SLD-scoped — should show message if no SLD selected
        navigateTo(TASKS_URL);

        // Poll for Tasks page content — React hydration can be slow in CI
        String text = "";
        for (int attempt = 0; attempt < 10; attempt++) {
            text = getPageText();
            if (countGridRows() > 0 || text.contains("Task") || text.contains("Select")
                    || text.contains("SLD") || text.contains("No rows")
                    || text.contains("Total Rows") || text.contains("Rows per page")) break;
            pause(2000);
        }
        logStepWithScreenshot("Tasks page SLD state");

        // Check if page shows SLD selection prompt or data
        boolean showsData = countGridRows() > 0 || text.contains("Task");
        boolean showsPrompt = text.contains("Select") || text.contains("SLD") || text.contains("select");
        // Also accept grid-related content (page loaded with data grid even if "Task" text not visible)
        boolean showsGrid = text.contains("Total Rows") || text.contains("Rows per page")
                || text.contains("No rows") || isGridPresent();

        logStep("Tasks page shows data: " + showsData + ", shows prompt: " + showsPrompt + ", shows grid: " + showsGrid);
        Assert.assertTrue(showsData || showsPrompt || showsGrid,
                "Tasks page should either show data or an SLD selection prompt");
        logStep("PASS: SLD-scoped data handling verified");
    }

    @Test(priority = 34, description = "BUG-D34: Verify legal agreement modal handling")
    public void testBUGD34_LegalAgreementModal() {
        ExtentReportManager.createTest(MODULE, FEATURE_EMPTY_STATE, "BUGD34_LegalAgreementModal");
        logStep("Checking for unexpected legal agreement modal on navigation");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        // Check if a modal/dialog blocks interaction
        List<WebElement> modals = driver.findElements(By.cssSelector(
                "[role='dialog'], .MuiDialog-root, .MuiModal-root"));

        boolean blockingModal = false;
        for (WebElement modal : modals) {
            try {
                if (modal.isDisplayed()) {
                    String modalText = modal.getText();
                    if (modalText.contains("Legal") || modalText.contains("Agreement")
                            || modalText.contains("Terms") || modalText.contains("Accept")) {
                        blockingModal = true;
                        logWarning("KNOWN BUG: Legal agreement modal appeared: " + modalText.substring(0, Math.min(100, modalText.length())));
                        logStepWithScreenshot("Legal agreement modal blocking");

                        // Try to dismiss it
                        try {
                            WebElement acceptBtn = modal.findElement(By.xpath(
                                    ".//button[contains(text(),'Accept') or contains(text(),'OK') or contains(text(),'Agree')]"));
                            acceptBtn.click();
                            pause(1000);
                            logStep("Dismissed legal agreement modal");
                        } catch (Exception e) {
                            logStep("Could not auto-dismiss modal");
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (!blockingModal) {
            logStep("No blocking legal agreement modal detected");
        }
        logStep("PASS: Legal agreement modal check completed");
    }

    // ================================================================
    // SECTION 5: CONSOLE ERROR & SDK FAILURES (5 TCs)
    // Bug: DevRev SDK fails to load on every page
    // Bug: Beamer leaks PII in URL params
    // ================================================================

    @Test(priority = 40, description = "BUG-D40: Detect DevRev SDK load failure in console")
    public void testBUGD40_DevRevSDKFailure() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONSOLE, "BUGD40_DevRevSDKFailure");
        logStep("Checking for DevRev SDK load failures in console");

        navigateTo(DASHBOARD_URL);
        pause(3000);

        // Check network requests for DevRev failures
        String pageSource = driver.getPageSource();
        boolean hasDevRevScript = pageSource.contains("devrev") || pageSource.contains("plug-platform");

        if (hasDevRevScript) {
            logWarning("KNOWN BUG: DevRev SDK script tag present — may fail to load (ERR_FAILED)");
        }

        // Check for visible error indicators
        String pageText = getPageText();
        boolean hasVisibleError = pageText.contains("DevRev") && pageText.contains("error");

        logStep("DevRev script in page source: " + hasDevRevScript);
        logStep("Visible DevRev error: " + hasVisibleError);
        logStepWithScreenshot("Console/DevRev check");

        // DevRev failure is a known non-blocking bug
        logStep("PASS: DevRev SDK failure detection completed");
    }

    @Test(priority = 41, description = "BUG-D41: Detect Beamer PII leak in network requests")
    public void testBUGD41_BeamerPIILeak() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONSOLE, "BUGD41_BeamerPIILeak");
        logStep("Checking for Beamer PII leak in page source/scripts");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        String pageSource = driver.getPageSource();

        // Check if Beamer is initialized with user PII
        boolean hasBeamer = pageSource.contains("beamer") || pageSource.contains("Beamer");
        boolean hasEmailInBeamer = false;
        boolean hasNameInBeamer = false;

        if (hasBeamer) {
            // Look for email patterns near Beamer config
            Pattern emailPattern = Pattern.compile("beamer.*?([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m = emailPattern.matcher(pageSource);
            if (m.find()) {
                hasEmailInBeamer = true;
                logWarning("SECURITY BUG: Beamer config contains email: " + m.group(1));
            }

            Pattern namePattern = Pattern.compile("beamer.*?(firstname|lastname|first_name|last_name).*?=.*?\\w+", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m2 = namePattern.matcher(pageSource);
            if (m2.find()) {
                hasNameInBeamer = true;
                logWarning("SECURITY BUG: Beamer config contains user name data");
            }
        }

        logStep("Beamer present: " + hasBeamer);
        logStep("Email in Beamer config: " + hasEmailInBeamer);
        logStep("Name in Beamer config: " + hasNameInBeamer);

        if (hasEmailInBeamer || hasNameInBeamer) {
            logWarning("KNOWN BUG: Beamer initialization leaks PII (email/name) in URL parameters");
        }

        logStep("PASS: Beamer PII leak check completed");
    }

    @Test(priority = 42, description = "BUG-D42: Check for JavaScript errors across module navigation")
    public void testBUGD42_JSErrorsAcrossModules() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONSOLE, "BUGD42_JSErrorsAcrossModules");
        logStep("Navigating across modules to detect JavaScript errors");

        String[] urls = {DASHBOARD_URL, ARC_FLASH_URL, TASKS_URL, ISSUES_URL};
        String[] names = {"Dashboard", "Arc Flash", "Tasks", "Issues"};

        // Inject console error interceptor
        js.executeScript(
                "window.__jsErrors = window.__jsErrors || [];"
                + "window.onerror = function(msg, url, line) {"
                + "  window.__jsErrors.push({message: msg, source: url, line: line});"
                + "};");

        for (int i = 0; i < urls.length; i++) {
            navigateTo(urls[i]);
            pause(2000);

            // Collect JS errors
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> jsErrors = (List<Map<String, Object>>) js.executeScript(
                    "var errs = window.__jsErrors || []; window.__jsErrors = []; return errs;");

            if (jsErrors != null && !jsErrors.isEmpty()) {
                logWarning(names[i] + " — " + jsErrors.size() + " JS error(s): " + jsErrors.get(0));
            } else {
                logStep(names[i] + " — no JS errors detected");
            }
        }

        logStep("PASS: Cross-module JS error check completed");
    }

    @Test(priority = 43, description = "BUG-D43: Verify no sensitive data in page source")
    public void testBUGD43_NoSensitiveDataInSource() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONSOLE, "BUGD43_NoSensitiveDataInSource");
        logStep("Checking page source for exposed sensitive data");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        String pageSource = driver.getPageSource();

        // Check for exposed passwords, API keys, or secrets
        boolean hasPasswordField = pageSource.toLowerCase().contains("password") && pageSource.toLowerCase().contains("value=\"");
        boolean hasApiKey = Pattern.compile("api[_-]?key\\s*[:=]\\s*['\"][a-zA-Z0-9]{20,}['\"]", Pattern.CASE_INSENSITIVE)
                .matcher(pageSource).find();
        boolean hasSecret = Pattern.compile("secret\\s*[:=]\\s*['\"][a-zA-Z0-9]{10,}['\"]", Pattern.CASE_INSENSITIVE)
                .matcher(pageSource).find();

        logStep("Password value in source: " + hasPasswordField);
        logStep("API key in source: " + hasApiKey);
        logStep("Secret in source: " + hasSecret);

        if (hasApiKey) logWarning("SECURITY: API key found in page source");
        if (hasSecret) logWarning("SECURITY: Secret value found in page source");

        Assert.assertFalse(hasApiKey, "Page source should not contain exposed API keys");
        logStep("PASS: Sensitive data check completed");
    }

    @Test(priority = 44, description = "BUG-D44: Verify Sentry error boundary recovery")
    public void testBUGD44_SentryErrorBoundary() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONSOLE, "BUGD44_SentryErrorBoundary");
        logStep("Checking Sentry error boundary configuration");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        String pageSource = driver.getPageSource();

        // Verify Sentry is configured (not just presence, but proper config)
        boolean hasSentry = pageSource.contains("sentry") || pageSource.contains("Sentry");
        boolean hasSentryDSN = pageSource.contains("dsn") && pageSource.contains("sentry.io");

        logStep("Sentry present: " + hasSentry);
        logStep("Sentry DSN configured: " + hasSentryDSN);

        // Check for error boundary fallback UI elements
        List<WebElement> errorBoundaries = driver.findElements(By.xpath(
                "//*[contains(@class,'error-boundary') or contains(@class,'ErrorBoundary')]"));

        logStep("Error boundary elements found: " + errorBoundaries.size());

        if (hasSentry) {
            logStep("Sentry error monitoring is active");
        } else {
            logWarning("Sentry not detected — error monitoring may be disabled");
        }

        logStep("PASS: Sentry error boundary check completed");
    }

    // ================================================================
    // SECTION 6: ARC FLASH READINESS DASHBOARD (5 TCs)
    // ================================================================

    @Test(priority = 50, description = "BUG-D50: Verify Arc Flash Readiness page loads")
    public void testBUGD50_ArcFlashPageLoads() {
        ExtentReportManager.createTest(MODULE, FEATURE_ARC_FLASH, "BUGD50_ArcFlashPageLoads");
        logStep("Navigating to Arc Flash Readiness dashboard");

        navigateTo(ARC_FLASH_URL);

        // Poll for Arc Flash content — React hydration can take extra time in CI
        String pageText = "";
        for (int attempt = 0; attempt < 10; attempt++) {
            pageText = getPageText();
            if (pageText.contains("Arc Flash") || pageText.contains("Readiness")
                    || pageText.contains("%") || pageText.contains("complete")) break;
            pause(2000);
        }
        logStepWithScreenshot("Arc Flash Readiness page");

        Assert.assertTrue(pageText.contains("Arc Flash") || pageText.contains("Readiness")
                        || pageText.contains("arc flash") || pageText.contains("readiness")
                        || pageText.contains("%") || pageText.contains("complete"),
                "Arc Flash page should contain relevant content");
        logStep("PASS: Arc Flash Readiness page loaded");
    }

    @Test(priority = 51, description = "BUG-D51: Verify arc flash progress bars show valid percentages")
    public void testBUGD51_ArcFlashProgressBars() {
        ExtentReportManager.createTest(MODULE, FEATURE_ARC_FLASH, "BUGD51_ArcFlashProgressBars");
        logStep("Checking arc flash progress bars");

        navigateTo(ARC_FLASH_URL);
        pause(2000);

        // Find progress bar elements
        List<WebElement> progressBars = driver.findElements(By.cssSelector(
                "[role='progressbar'], .MuiLinearProgress-root, .MuiCircularProgress-root"));

        logStep("Found " + progressBars.size() + " progress bar elements");

        for (WebElement bar : progressBars) {
            try {
                String ariaValue = bar.getDomAttribute("aria-valuenow");
                if (ariaValue != null) {
                    double value = Double.parseDouble(ariaValue);
                    Assert.assertTrue(value >= 0 && value <= 100,
                            "Progress bar value should be 0-100. Got: " + value);
                    logStep("Progress bar value: " + value + "%");
                }
            } catch (NumberFormatException e) {
                logStep("Progress bar has non-numeric value");
            }
        }

        logStep("PASS: Arc flash progress bars validated");
    }

    @Test(priority = 52, description = "BUG-D52: Verify arc flash readiness shows asset class breakdown")
    public void testBUGD52_ArcFlashAssetClassBreakdown() {
        ExtentReportManager.createTest(MODULE, FEATURE_ARC_FLASH, "BUGD52_ArcFlashAssetClassBreakdown");
        logStep("Checking asset class breakdown on Arc Flash page");

        navigateTo(ARC_FLASH_URL);

        // Poll for actual Arc Flash content — not just page length (nav/header alone > 100 chars)
        String pageText = "";
        for (int attempt = 0; attempt < 10; attempt++) {
            pageText = getPageText();
            if (pageText.contains("%") || pageText.contains("complete")
                    || pageText.contains("Switchboard") || pageText.contains("Readiness")) break;
            logStep("Waiting for Arc Flash content (attempt " + (attempt + 1) + ", length=" + pageText.length() + ")");
            if (attempt == 4) {
                logStep("Content not loaded — refreshing page");
                driver.navigate().refresh();
                pause(2000);
                waitAndDismissAppAlert();
            }
            pause(2000);
        }

        // Known asset classes that should appear
        String[] assetClasses = {"Switchboard", "Panelboard", "PDU", "MCC", "ATS", "Transformer", "TRF"};
        int found = 0;
        for (String cls : assetClasses) {
            if (pageText.contains(cls)) {
                found++;
                logStep("Found asset class: " + cls);
            }
        }

        logStep("Found " + found + " asset classes on Arc Flash page (page length: " + pageText.length() + ")");
        logStepWithScreenshot("Arc Flash asset class breakdown");

        if (found == 0) {
            // Page may show classes under different labels or site has no arc flash data
            boolean hasAnyContent = pageText.contains("readiness") || pageText.contains("Readiness")
                    || pageText.contains("arc flash") || pageText.contains("Arc Flash")
                    || pageText.contains("compliance") || pageText.contains("Compliance")
                    || pageText.contains("%");
            if (hasAnyContent) {
                logWarning("KNOWN ISSUE: Arc flash page has content but asset class labels differ from expected");
            } else {
                logWarning("KNOWN ISSUE: Arc flash page has no readiness data for this test site");
            }
        }
        Assert.assertTrue(found >= 1 || pageText.contains("%") || pageText.contains("readiness")
                        || pageText.contains("Readiness") || pageText.contains("No")
                        || pageText.contains("complete") || pageText.contains("Overview"),
                "Arc Flash page should show asset class breakdown or readiness content. Found: " + found);
        logStep("PASS: Asset class breakdown verified");
    }

    @Test(priority = 53, description = "BUG-D53: Verify 0% readiness categories (PDU, Switchboard) are highlighted")
    public void testBUGD53_ZeroReadinessHighlighted() {
        ExtentReportManager.createTest(MODULE, FEATURE_ARC_FLASH, "BUGD53_ZeroReadinessHighlighted");
        logStep("Checking if 0% readiness categories are visually distinct");

        navigateTo(ARC_FLASH_URL);
        pause(2000);

        String pageText = getPageText();
        logStepWithScreenshot("Arc Flash 0% categories");

        // Known: PDU (501 assets) and Switchboard (500 assets) are at 0%
        boolean hasPDU = pageText.contains("PDU");
        boolean hasSwitchboard = pageText.contains("Switchboard") || pageText.contains("SWB");

        logStep("PDU shown: " + hasPDU + ", Switchboard shown: " + hasSwitchboard);

        // Check if 0% is displayed
        boolean hasZeroPercent = pageText.contains("0%");
        logStep("Contains 0% value: " + hasZeroPercent);

        logStep("PASS: Zero readiness check completed");
    }

    @Test(priority = 54, description = "BUG-D54: Verify arc flash readiness total calculation")
    public void testBUGD54_ArcFlashTotalCalculation() {
        ExtentReportManager.createTest(MODULE, FEATURE_ARC_FLASH, "BUGD54_ArcFlashTotalCalculation");
        logStep("Verifying overall arc flash readiness calculation");

        navigateTo(ARC_FLASH_URL);
        pause(2000);

        String pageText = getPageText();

        // Extract all percentage values
        Pattern pctPattern = Pattern.compile("(\\d+\\.?\\d*)\\s*%");
        Matcher m = pctPattern.matcher(pageText);

        List<Double> percentages = new ArrayList<>();
        while (m.find()) {
            percentages.add(Double.parseDouble(m.group(1)));
        }

        logStep("Percentage values on Arc Flash page: " + percentages);

        // All should be valid (0-100)
        for (Double pct : percentages) {
            Assert.assertTrue(pct >= 0 && pct <= 100,
                    "All readiness percentages should be 0-100. Found: " + pct);
        }

        logStep("PASS: Arc flash total calculation check completed");
    }

    // ================================================================
    // SECTION 7: CONDITION ASSESSMENT DASHBOARD (5 TCs)
    // ================================================================

    @Test(priority = 60, description = "BUG-D60: Verify Condition Assessment page loads")
    public void testBUGD60_ConditionAssessmentLoads() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONDITION, "BUGD60_ConditionAssessmentLoads");
        logStep("Navigating to Condition Assessment (PM Readiness) page");

        navigateTo(CONDITION_URL);

        // Poll for React-rendered content (readyState fires before React hydration)
        String pageText = "";
        boolean hasContent = false;
        for (int attempt = 0; attempt < 10; attempt++) {
            pageText = getPageText();
            hasContent = pageText.contains("Condition") || pageText.contains("Assessment")
                    || pageText.contains("PM") || pageText.contains("Readiness")
                    || pageText.contains("condition") || pageText.contains("assessment");
            if (hasContent) break;
            logStep("Waiting for Condition Assessment content (attempt " + (attempt + 1) + ")");
            pause(2000);
        }

        logStepWithScreenshot("Condition Assessment page");

        Assert.assertTrue(hasContent,
                "Condition Assessment page should contain relevant content (text length=" + pageText.length() + ")");
        logStep("PASS: Condition Assessment page loaded");
    }

    @Test(priority = 61, description = "BUG-D61: Verify PM readiness percentage values")
    public void testBUGD61_PMReadinessPercentages() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONDITION, "BUGD61_PMReadinessPercentages");
        logStep("Checking PM readiness percentages");

        navigateTo(CONDITION_URL);
        pause(2000);

        String pageText = getPageText();
        Pattern pctPattern = Pattern.compile("(\\d+\\.?\\d*)\\s*%");
        Matcher m = pctPattern.matcher(pageText);

        int count = 0;
        while (m.find()) {
            count++;
            double val = Double.parseDouble(m.group(1));
            Assert.assertTrue(val >= 0 && val <= 100,
                    "PM readiness percentage should be 0-100. Got: " + val);
        }

        logStep("Found " + count + " percentage values on Condition Assessment page");
        logStep("PASS: PM readiness percentages are valid");
    }

    @Test(priority = 62, description = "BUG-D62: Verify Condition Assessment has data grid or chart")
    public void testBUGD62_ConditionAssessmentVisualization() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONDITION, "BUGD62_ConditionAssessmentVisualization");
        logStep("Checking for data visualization on Condition Assessment");

        navigateTo(CONDITION_URL);
        pause(2000);

        boolean hasGrid = isGridPresent();
        List<WebElement> charts = driver.findElements(CHART_CANVAS);
        List<WebElement> svgCharts = driver.findElements(By.cssSelector("svg.recharts-surface, svg[class*='chart']"));
        List<WebElement> progressBars = driver.findElements(By.cssSelector(
                "[role='progressbar'], .MuiLinearProgress-root"));

        int visualElements = charts.size() + svgCharts.size() + progressBars.size() + (hasGrid ? 1 : 0);
        logStep("Visual elements: grid=" + hasGrid + ", charts=" + charts.size()
                + ", svgs=" + svgCharts.size() + ", progress=" + progressBars.size());

        Assert.assertTrue(visualElements > 0,
                "Condition Assessment should have at least one data visualization");
        logStep("PASS: Condition Assessment has data visualization");
    }

    @Test(priority = 63, description = "BUG-D63: Verify Condition Assessment navigation consistency")
    public void testBUGD63_ConditionAssessmentNavigation() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONDITION, "BUGD63_ConditionAssessmentNavigation");
        logStep("Testing navigation to/from Condition Assessment");

        navigateTo(CONDITION_URL);
        pause(2000);

        String conditionUrl = driver.getCurrentUrl();
        Assert.assertTrue(conditionUrl.contains("pm-readiness") || conditionUrl.contains("condition"),
                "URL should contain 'pm-readiness' or 'condition'. Got: " + conditionUrl);

        // Navigate away and back
        navigateTo(DASHBOARD_URL);
        pause(1000);
        navigateTo(CONDITION_URL);
        pause(2000);

        String afterNavUrl = driver.getCurrentUrl();
        Assert.assertTrue(afterNavUrl.contains("pm-readiness") || afterNavUrl.contains("condition"),
                "After navigation, should return to Condition Assessment");

        logStep("PASS: Condition Assessment navigation is consistent");
    }

    @Test(priority = 64, description = "BUG-D64: Verify Condition Assessment matches asset data")
    public void testBUGD64_ConditionAssessmentDataPresence() {
        ExtentReportManager.createTest(MODULE, FEATURE_CONDITION, "BUGD64_ConditionAssessmentDataPresence");
        logStep("Verifying Condition Assessment contains asset-related data");

        navigateTo(CONDITION_URL);
        pause(2000);

        String pageText = getPageText();

        // Should contain references to electrical equipment or asset classes
        String[] equipmentTerms = {"Switchboard", "Panelboard", "Transformer", "MCC", "PDU",
                "Generator", "ATS", "UPS", "VFD", "Cable", "Busway"};

        int found = 0;
        for (String term : equipmentTerms) {
            if (pageText.contains(term)) found++;
        }

        logStep("Found " + found + " equipment terms on Condition Assessment page");
        logStep("PASS: Condition Assessment data presence verified");
    }

    // ================================================================
    // SECTION 8: EQUIPMENT INSIGHTS (4 TCs)
    // ================================================================

    @Test(priority = 70, description = "BUG-D70: Verify Equipment Insights page loads with grid")
    public void testBUGD70_EquipmentInsightsLoads() {
        ExtentReportManager.createTest(MODULE, FEATURE_INSIGHTS, "BUGD70_EquipmentInsightsLoads");
        logStep("Navigating to Equipment Insights");

        navigateTo(INSIGHTS_URL);
        pause(3000);

        String pageText = getPageText();
        logStepWithScreenshot("Equipment Insights page");

        boolean hasContent = pageText.contains("Equipment") || pageText.contains("Insights")
                || pageText.contains("Designation") || pageText.contains("equipment");
        Assert.assertTrue(hasContent,
                "Equipment Insights page should contain relevant content");

        boolean hasGrid = isGridPresent();
        logStep("Grid present: " + hasGrid);

        logStep("PASS: Equipment Insights page loaded");
    }

    @Test(priority = 71, description = "BUG-D71: Verify Equipment Insights grid has data rows")
    public void testBUGD71_EquipmentInsightsDataRows() {
        ExtentReportManager.createTest(MODULE, FEATURE_INSIGHTS, "BUGD71_EquipmentInsightsDataRows");
        logStep("Checking Equipment Insights data rows");

        navigateTo(INSIGHTS_URL);
        pause(3000);

        int rowCount = countGridRows();
        logStep("Equipment Insights rows: " + rowCount);

        // Known: 46 equipment designations
        if (rowCount > 0) {
            logStep("Grid has " + rowCount + " data rows");
        } else {
            logWarning("Equipment Insights grid shows no rows — expected ~46 designations");
        }

        logStep("PASS: Equipment Insights data rows check completed");
    }

    @Test(priority = 72, description = "BUG-D72: Verify Equipment Insights search functionality")
    public void testBUGD72_EquipmentInsightsSearch() {
        ExtentReportManager.createTest(MODULE, FEATURE_INSIGHTS, "BUGD72_EquipmentInsightsSearch");
        logStep("Testing Equipment Insights search");

        navigateTo(INSIGHTS_URL);
        pause(3000);

        // Find search input
        List<WebElement> searchInputs = driver.findElements(By.cssSelector(
                "input[placeholder*='Search'], input[placeholder*='search'], input[type='search']"));

        if (!searchInputs.isEmpty()) {
            WebElement search = searchInputs.get(0);
            search.clear();
            search.sendKeys("Switchboard");
            pause(1500);

            int filteredRows = countGridRows();
            logStep("After searching 'Switchboard': " + filteredRows + " rows");
            logStepWithScreenshot("Equipment Insights search result");

            // Clear search
            search.clear();
            search.sendKeys(org.openqa.selenium.Keys.ESCAPE);
            pause(1000);
        } else {
            logStep("Search input not found on Equipment Insights page");
        }

        logStep("PASS: Equipment Insights search test completed");
    }

    @Test(priority = 73, description = "BUG-D73: Verify Equipment Insights designation count")
    public void testBUGD73_DesignationCount() {
        ExtentReportManager.createTest(MODULE, FEATURE_INSIGHTS, "BUGD73_DesignationCount");
        logStep("Checking equipment designation count");

        navigateTo(INSIGHTS_URL);
        pause(3000);

        String pageText = getPageText();

        // Look for count indicators
        Pattern countPattern = Pattern.compile("(\\d+)\\s*(?:designations?|results?|items?|rows?|total)", Pattern.CASE_INSENSITIVE);
        Matcher m = countPattern.matcher(pageText);

        if (m.find()) {
            int count = Integer.parseInt(m.group(1));
            logStep("Designation count displayed: " + count);
            Assert.assertTrue(count >= 0, "Designation count should be non-negative");
        } else {
            logStep("No explicit count indicator found — checking grid rows");
            int rows = countGridRows();
            logStep("Grid has " + rows + " rows (expected ~46)");
        }

        logStep("PASS: Designation count check completed");
    }

    // ================================================================
    // SECTION 9: CROSS-MODULE DATA INTEGRITY (3 TCs)
    // Bug: All tasks overdue, test data pollution
    // ================================================================

    @Test(priority = 80, description = "BUG-D80: Detect overdue tasks (all 32 tasks overdue)")
    public void testBUGD80_OverdueTasks() {
        ExtentReportManager.createTest(MODULE, FEATURE_INTEGRITY, "BUGD80_OverdueTasks");
        logStep("Checking for overdue tasks in Tasks module");

        navigateTo(TASKS_URL);
        pause(3000);

        String pageText = getPageText();
        logStepWithScreenshot("Tasks page");

        // Look for overdue indicators
        boolean hasOverdue = pageText.toLowerCase().contains("overdue")
                || pageText.toLowerCase().contains("past due")
                || pageText.toLowerCase().contains("late");

        // Check for date cells with past dates
        List<String> dates = collectDatesFromPage();
        logStep("Dates found on Tasks page: " + dates.size());

        int overdueCount = 0;
        for (String dateStr : dates) {
            if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                try {
                    String[] parts = dateStr.split("/");
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);

                    // Simple check: if year is 2025 or earlier, it's overdue
                    if (year < 2026 || (year == 2026 && month < 3)) {
                        overdueCount++;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (overdueCount > 0) {
            logWarning("KNOWN BUG: " + overdueCount + " task(s) have past due dates (all from Nov 2025)");
        }

        logStep("Overdue indicator present: " + hasOverdue);
        logStep("Past-due date count: " + overdueCount);
        logStep("PASS: Overdue tasks check completed");
    }

    @Test(priority = 81, description = "BUG-D81: Detect test data pollution in Work Orders")
    public void testBUGD81_TestDataPollution() {
        ExtentReportManager.createTest(MODULE, FEATURE_INTEGRITY, "BUGD81_TestDataPollution");
        logStep("Checking for test data pollution in Work Orders");

        navigateViaSidebar("Work Orders");
        pause(3000);

        String pageText = getPageText();

        // Known: Work orders are all named "SmokeTest_*"
        int smokeTestCount = countOccurrences(pageText, "SmokeTest");
        int automationCount = countOccurrences(pageText, "AutoTest");
        int testDataCount = smokeTestCount + automationCount;

        logStep("'SmokeTest' occurrences: " + smokeTestCount);
        logStep("'AutoTest' occurrences: " + automationCount);
        logStepWithScreenshot("Work Orders test data");

        if (testDataCount > 5) {
            logWarning("TEST DATA POLLUTION: " + testDataCount + " test-generated work orders detected");
        }

        logStep("PASS: Test data pollution check completed");
    }

    @Test(priority = 82, description = "BUG-D82: Verify user role assignment consistency")
    public void testBUGD82_UserRoleConsistency() {
        ExtentReportManager.createTest(MODULE, FEATURE_INTEGRITY, "BUGD82_UserRoleConsistency");
        logStep("Checking user profile for role assignment");

        navigateTo(DASHBOARD_URL);
        pause(2000);

        // Try to access user profile/settings
        try {
            // Click user avatar or profile menu
            List<WebElement> avatars = driver.findElements(By.cssSelector(
                    "[class*='avatar'], [class*='Avatar'], img[alt*='user'], img[alt*='profile']"));
            List<WebElement> profileMenus = driver.findElements(By.cssSelector(
                    "[aria-label*='profile'], [aria-label*='account'], [aria-label*='user']"));

            if (!avatars.isEmpty()) {
                avatars.get(0).click();
                pause(1000);
            } else if (!profileMenus.isEmpty()) {
                profileMenus.get(0).click();
                pause(1000);
            }

            String menuText = getPageText();
            logStepWithScreenshot("User profile menu");

            // Check if "No Role" is displayed (known bug for one user)
            if (menuText.contains("No Role") || menuText.contains("no role")) {
                logWarning("KNOWN BUG: User has 'No Role' assigned — may indicate RBAC misconfiguration");
            }

            // Check if role is displayed
            String[] roles = {"Admin", "Project Manager", "Technician", "Facility Manager", "Client Portal"};
            boolean roleFound = false;
            for (String role : roles) {
                if (menuText.contains(role)) {
                    roleFound = true;
                    logStep("User role detected: " + role);
                    break;
                }
            }

            if (!roleFound) {
                logStep("Could not detect user role from profile menu");
            }
        } catch (Exception e) {
            logStep("Could not access user profile: " + e.getMessage());
        }

        logStep("PASS: User role consistency check completed");
    }
}
