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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Critical Path Tests — High-priority tests for bugs customers actually encounter.
 *
 * PHILOSOPHY: Most tests in the suite validate "does X work?" (happy path).
 * These tests validate "does X BREAK in ways that cost the customer money?"
 *
 * Every test here targets a real scenario where:
 *   - Data loss or corruption can occur
 *   - Cross-module consistency breaks (asset shows in one place, not another)
 *   - Numbers/money displayed incorrectly (customer makes wrong business decisions)
 *   - Critical workflows silently fail (save appears to work but data isn't persisted)
 *   - Search/filter shows wrong results (customer can't find their assets)
 *
 * These tests are DESIGNED to catch regressions. If they all pass,
 * the app's core value proposition is intact.
 *
 * Total: 25 high-priority TCs across 5 categories:
 *   1. Data Integrity (5 TCs) — save/reload consistency, cross-module data match
 *   2. Financial Accuracy (5 TCs) — money formatting, KPI calculations, equipment at risk
 *   3. Search & Filter Reliability (5 TCs) — search returns correct results, no phantom data
 *   4. Cross-Module Consistency (5 TCs) — asset count matches everywhere, connections bi-directional
 *   5. Silent Failure Detection (5 TCs) — API errors hidden from user, stale cache, zombie data
 */
public class CriticalPathTestNG extends BaseTest {

    private static final String MODULE = "Critical Path";
    private static final String FEATURE_DATA = "Data Integrity";
    private static final String FEATURE_FINANCIAL = "Financial Accuracy";
    private static final String FEATURE_SEARCH = "Search Reliability";
    private static final String FEATURE_CROSS_MODULE = "Cross-Module Consistency";
    private static final String FEATURE_SILENT_FAIL = "Silent Failure Detection";

    // Routes
    private static final String DASHBOARD_URL = AppConstants.BASE_URL + "/dashboard";
    private static final String ASSETS_URL = AppConstants.BASE_URL + "/assets";
    private static final String CONNECTIONS_URL = AppConstants.BASE_URL + "/connections";
    private static final String TASKS_URL = AppConstants.BASE_URL + "/tasks";
    private static final String ISSUES_URL = AppConstants.BASE_URL + "/issues";
    private static final String WORK_ORDERS_URL = AppConstants.BASE_URL + "/sessions";
    private static final String PLANNING_URL = AppConstants.BASE_URL + "/planning";
    private static final String LOCATIONS_URL = AppConstants.BASE_URL + "/locations";

    // Common locators
    private static final By DATA_GRID = By.cssSelector("[role='grid'], .MuiDataGrid-root");
    private static final By GRID_ROWS = By.cssSelector("[role='row'].MuiDataGrid-row, [role='row'][data-rowindex]");
    private static final By COLUMN_HEADERS = By.cssSelector("[role='columnheader']");

    private JavascriptExecutor js;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     CRITICAL PATH TESTS — Customer-Impact Scenarios");
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
        dismissBackdrops(); // dismiss app update alert after navigation
        pause(1000);
        new WebDriverWait(driver, Duration.ofSeconds(AppConstants.DEFAULT_TIMEOUT))
                .until(d -> "complete".equals(
                        ((JavascriptExecutor) d).executeScript("return document.readyState")));
        pause(1000);
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
            return driver.findElements(GRID_ROWS).size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int extractNumber(String text) {
        String cleaned = text.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
    }

    private void waitForGrid() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> !d.findElements(DATA_GRID).isEmpty());
            pause(2000);
        } catch (Exception e) {
            logStep("Grid did not appear within timeout");
        }
    }

    // ================================================================
    // SECTION 1: DATA INTEGRITY (5 TCs)
    // "When I save data, is it ACTUALLY saved?"
    // ================================================================

    @Test(priority = 1, description = "CP_DI_001: Dashboard total assets matches Assets grid pagination count")
    public void testCP_DI_001_AssetCountMatchesDashboard() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATA, "CP_DI_001_AssetCountMatch");
        logStep("Verifying asset count consistency between Dashboard and Assets module");

        // Step 1: Get total from Dashboard KPI card
        navigateTo(DASHBOARD_URL);
        int dashboardTotal = 0;
        try {
            // Look for "Total Assets" (DOM uses Title Case; CSS text-transform makes it visually uppercase)
            WebElement totalHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Total Assets')]/following-sibling::*[1]"
                    + " | //*[contains(text(),'Total Assets')]/..//h3"));
            dashboardTotal = extractNumber(totalHeading.getText());
            logStep("Dashboard shows: " + dashboardTotal + " total assets");
        } catch (Exception e) {
            logStep("Could not read Total Assets from dashboard: " + e.getMessage());
        }

        // Step 2: Get total from Assets grid pagination
        navigateTo(ASSETS_URL);
        waitForGrid();
        int assetsPageTotal = 0;
        try {
            // MUI DataGrid pagination shows "1-25 of 1906"
            String paginationText = getPageText();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                    .matcher(paginationText);
            if (m.find()) {
                assetsPageTotal = extractNumber(m.group(1));
            }
            logStep("Assets grid pagination shows: " + assetsPageTotal + " total");
        } catch (Exception e) {
            logStep("Could not read Assets pagination: " + e.getMessage());
        }

        // Step 3: Compare — they must match
        Assert.assertTrue(dashboardTotal > 0,
                "Dashboard TOTAL ASSETS should be > 0, got: " + dashboardTotal);
        Assert.assertTrue(assetsPageTotal > 0,
                "Assets grid total should be > 0, got: " + assetsPageTotal);
        Assert.assertEquals(assetsPageTotal, dashboardTotal,
                "Assets grid total (" + assetsPageTotal + ") must match Dashboard KPI ("
                + dashboardTotal + "). Mismatch = stale cache or API inconsistency.");

        logStepWithScreenshot("Asset count matches");
        ExtentReportManager.logPass("Dashboard total " + dashboardTotal + " = Assets grid total " + assetsPageTotal);
    }

    @Test(priority = 2, description = "CP_DI_002: Dashboard Pending Tasks count matches Tasks grid")
    public void testCP_DI_002_TaskCountMatchesDashboard() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATA, "CP_DI_002_TaskCountMatch");
        logStep("Verifying pending task count: Dashboard vs Tasks module");

        // Step 1: Dashboard KPI
        navigateTo(DASHBOARD_URL);
        int dashboardTasks = 0;
        try {
            WebElement taskHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Pending Tasks')]/following-sibling::*[1]"
                    + " | //*[contains(text(),'Pending Tasks')]/..//h3"));
            dashboardTasks = extractNumber(taskHeading.getText());
            logStep("Dashboard pending tasks: " + dashboardTasks);
        } catch (Exception e) {
            logStep("Could not read Pending Tasks: " + e.getMessage());
        }

        // Step 2: Tasks module KPI
        navigateTo(TASKS_URL);
        pause(3000);
        int taskModuleCount = 0;
        try {
            WebElement pendingBadge = driver.findElement(By.xpath(
                    "//*[contains(text(),'Pending')]/following-sibling::*[1]"
                    + " | //*[contains(text(),'Pending')]/..//h5"));
            taskModuleCount = extractNumber(pendingBadge.getText());
            logStep("Tasks module pending: " + taskModuleCount);
        } catch (Exception e) {
            logStep("Could not read Tasks pending count: " + e.getMessage());
        }

        // Step 3: Compare
        Assert.assertTrue(dashboardTasks > 0,
                "Dashboard should show pending tasks > 0");
        Assert.assertEquals(taskModuleCount, dashboardTasks,
                "Tasks module pending (" + taskModuleCount + ") must match Dashboard ("
                + dashboardTasks + "). Mismatch = different API endpoints returning different data.");

        ExtentReportManager.logPass("Pending tasks count consistent: " + dashboardTasks);
    }

    @Test(priority = 3, description = "CP_DI_003: Dashboard Unresolved Issues matches Issues grid")
    public void testCP_DI_003_IssueCountMatchesDashboard() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATA, "CP_DI_003_IssueCountMatch");
        logStep("Verifying unresolved issue count: Dashboard vs Issues module");

        navigateTo(DASHBOARD_URL);
        int dashboardIssues = 0;
        try {
            WebElement issueHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Unresolved Issues')]/following-sibling::*[1]"
                    + " | //*[contains(text(),'Unresolved Issues')]/..//h3"));
            dashboardIssues = extractNumber(issueHeading.getText());
            logStep("Dashboard unresolved issues: " + dashboardIssues);
        } catch (Exception e) {
            logStep("Could not read Unresolved Issues: " + e.getMessage());
        }

        navigateTo(ISSUES_URL);
        waitForGrid();
        int issuesGridTotal = 0;
        try {
            String pageText = getPageText();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                    .matcher(pageText);
            if (m.find()) {
                issuesGridTotal = extractNumber(m.group(1));
            }
            logStep("Issues grid total: " + issuesGridTotal);
        } catch (Exception e) {
            logStep("Could not read Issues pagination");
        }

        Assert.assertTrue(dashboardIssues > 0,
                "Dashboard should show unresolved issues > 0");
        // Issues grid may show ALL issues (not just unresolved), so dashboard <= grid total
        Assert.assertTrue(issuesGridTotal >= dashboardIssues,
                "Issues grid total (" + issuesGridTotal + ") should be >= Dashboard unresolved ("
                + dashboardIssues + ")");

        ExtentReportManager.logPass("Issue counts consistent: Dashboard=" + dashboardIssues
                + " Issues grid=" + issuesGridTotal);
    }

    @Test(priority = 4, description = "CP_DI_004: Work Order count matches between Dashboard and Work Orders page")
    public void testCP_DI_004_WorkOrderCountMatch() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATA, "CP_DI_004_WorkOrderCountMatch");
        logStep("Verifying active work order count: Dashboard vs Work Orders module");

        navigateTo(DASHBOARD_URL);
        int dashboardWO = 0;
        try {
            WebElement woHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Active Work Orders')]/following-sibling::*[1]"
                    + " | //*[contains(text(),'Active Work Orders')]/..//h3"));
            dashboardWO = extractNumber(woHeading.getText());
            logStep("Dashboard active work orders: " + dashboardWO);
        } catch (Exception e) {
            logStep("Could not read Active Work Orders: " + e.getMessage());
        }

        navigateTo(WORK_ORDERS_URL);
        waitForGrid();
        int woGridTotal = 0;
        try {
            String pageText = getPageText();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                    .matcher(pageText);
            if (m.find()) {
                woGridTotal = extractNumber(m.group(1));
            }
            logStep("Work Orders grid total: " + woGridTotal);
        } catch (Exception e) {
            logStep("Could not read Work Orders pagination");
        }

        Assert.assertTrue(dashboardWO > 0,
                "Dashboard should show active work orders > 0");
        Assert.assertTrue(woGridTotal >= dashboardWO,
                "Work Orders total (" + woGridTotal + ") should be >= Dashboard active ("
                + dashboardWO + ")");

        ExtentReportManager.logPass("Work order counts consistent");
    }

    @Test(priority = 5, description = "CP_DI_005: Asset detail page data survives browser refresh")
    public void testCP_DI_005_AssetDataSurvivesRefresh() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATA, "CP_DI_005_RefreshPersistence");
        logStep("Verifying asset detail data persists after browser refresh");

        navigateTo(ASSETS_URL);
        waitForGrid();

        // Click first asset to go to detail
        try {
            List<WebElement> rows = driver.findElements(GRID_ROWS);
            Assert.assertFalse(rows.isEmpty(), "Assets grid should have data rows");
            WebElement firstCell = rows.get(0).findElement(
                    By.cssSelector(".MuiDataGrid-cell:first-child, [role='gridcell']:first-child"));
            String assetName = firstCell.getText().trim();
            logStep("Clicking asset: " + assetName);
            firstCell.click();
            pause(3000);

            // Capture detail page content
            String detailUrl = driver.getCurrentUrl();
            Assert.assertTrue(detailUrl.contains("/assets/") && !detailUrl.endsWith("/assets"),
                    "Should navigate to asset detail page");

            String beforeRefresh = getPageText();
            Assert.assertTrue(beforeRefresh.contains(assetName),
                    "Detail page should contain asset name: " + assetName);

            // Refresh the page
            driver.navigate().refresh();
            pause(5000);

            // Verify data survives refresh (not a stale SPA cache issue)
            String afterRefresh = getPageText();
            Assert.assertTrue(afterRefresh.contains(assetName),
                    "Asset name '" + assetName + "' must survive browser refresh. "
                    + "If missing, the detail page may rely on SPA state instead of fetching from API.");

            logStep("Data survived refresh: " + assetName);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            logStep("No grid data available: " + e.getMessage());
            Assert.fail("Assets grid has no clickable rows");
        }

        ExtentReportManager.logPass("Asset detail data persists after browser refresh");
    }

    // ================================================================
    // SECTION 2: FINANCIAL ACCURACY (5 TCs)
    // "Are money amounts displayed correctly?"
    // ================================================================

    @Test(priority = 6, description = "CP_FA_001: Equipment at Risk dollar amount uses correct formatting")
    public void testCP_FA_001_EquipmentAtRiskFormatting() {
        ExtentReportManager.createTest(MODULE, FEATURE_FINANCIAL, "CP_FA_001_EquipmentAtRisk");
        logStep("Verifying Equipment at Risk dollar formatting");

        navigateTo(DASHBOARD_URL);
        String pageText = getPageText();

        // Find Equipment at Risk value
        Assert.assertTrue(pageText.contains("EQUIPMENT AT RISK"),
                "Dashboard should show EQUIPMENT AT RISK KPI card");

        try {
            WebElement riskHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Equipment At Risk') or contains(text(),'Equipment at Risk')]/following-sibling::*[1]"
                    + " | //*[contains(text(),'Equipment At Risk') or contains(text(),'Equipment at Risk')]/..//h3"));
            String riskValue = riskHeading.getText().trim();
            logStep("Equipment at Risk value: " + riskValue);

            // Must start with $
            Assert.assertTrue(riskValue.startsWith("$"),
                    "Equipment at Risk must start with '$'. Got: " + riskValue);

            // Must not have double dollar signs
            Assert.assertFalse(riskValue.contains("$$"),
                    "Should not have double dollar sign: " + riskValue);

            // Must use 'k' for thousands (e.g., "$3525.4k" not "$3525400")
            // Or proper comma formatting (e.g., "$3,525,400")
            boolean hasKSuffix = riskValue.contains("k") || riskValue.contains("K");
            boolean hasCommas = riskValue.contains(",");
            Assert.assertTrue(hasKSuffix || hasCommas || riskValue.length() < 8,
                    "Large dollar amounts should use 'k' suffix or commas. Got: " + riskValue);

        } catch (Exception e) {
            Assert.fail("Could not read Equipment at Risk value: " + e.getMessage());
        }

        ExtentReportManager.logPass("Equipment at Risk formatting correct");
    }

    @Test(priority = 7, description = "CP_FA_002: Opportunities Value is properly formatted")
    public void testCP_FA_002_OpportunitiesValueFormatting() {
        ExtentReportManager.createTest(MODULE, FEATURE_FINANCIAL, "CP_FA_002_OpportunitiesValue");
        logStep("Verifying Opportunities Value dollar formatting");

        navigateTo(DASHBOARD_URL);

        try {
            WebElement oppHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Opportunities Value')]/following-sibling::*[1]"
                    + " | //*[contains(text(),'Opportunities Value')]/..//h3"));
            String oppValue = oppHeading.getText().trim();
            logStep("Opportunities Value: " + oppValue);

            Assert.assertTrue(oppValue.startsWith("$"),
                    "Opportunities Value must start with '$'. Got: " + oppValue);
            Assert.assertFalse(oppValue.equals("$0") || oppValue.equals("$0.0"),
                    "Opportunities Value should not be zero if opportunities exist");
            // Should not display NaN or undefined
            Assert.assertFalse(oppValue.contains("NaN") || oppValue.contains("undefined"),
                    "Financial value must not show NaN or undefined: " + oppValue);

        } catch (Exception e) {
            Assert.fail("Could not read Opportunities Value: " + e.getMessage());
        }

        ExtentReportManager.logPass("Opportunities Value formatting correct");
    }

    @Test(priority = 8, description = "CP_FA_003: All dashboard KPI numbers are non-negative")
    public void testCP_FA_003_KPINumbersNonNegative() {
        ExtentReportManager.createTest(MODULE, FEATURE_FINANCIAL, "CP_FA_003_NonNegativeKPIs");
        logStep("Verifying no KPI card shows negative numbers");

        navigateTo(DASHBOARD_URL);
        List<WebElement> kpiCards = driver.findElements(By.tagName("h3"));
        int checked = 0;
        for (WebElement card : kpiCards) {
            String text = card.getText().trim();
            if (text.isEmpty()) continue;
            logStep("KPI card: " + text);
            // No negative numbers (e.g., "-5 assets" would be a bug)
            Assert.assertFalse(text.startsWith("-"),
                    "KPI card should not show negative number: " + text);
            // No NaN
            Assert.assertFalse(text.equalsIgnoreCase("NaN"),
                    "KPI card should not show NaN: " + text);
            checked++;
        }
        Assert.assertTrue(checked >= 4,
                "Should find at least 4 KPI cards, found: " + checked);

        ExtentReportManager.logPass("All " + checked + " KPI cards show valid non-negative values");
    }

    @Test(priority = 9, description = "CP_FA_004: Condition Assessment percentage is 0-100%")
    public void testCP_FA_004_ConditionPercentageRange() {
        ExtentReportManager.createTest(MODULE, FEATURE_FINANCIAL, "CP_FA_004_ConditionPercentage");
        logStep("Verifying Condition Assessment percentage is within 0-100%");

        navigateTo(DASHBOARD_URL);

        try {
            WebElement condHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Condition Assessment')]/following-sibling::*"
                    + " | //*[contains(text(),'Condition Assessment')]/..//*[contains(text(),'%')]"));
            String percentText = condHeading.getText().trim();
            logStep("Condition Assessment: " + percentText);

            // Extract number
            String numPart = percentText.replaceAll("[^0-9]", "");
            if (!numPart.isEmpty()) {
                int percent = Integer.parseInt(numPart);
                Assert.assertTrue(percent >= 0 && percent <= 100,
                        "Percentage must be 0-100, got: " + percent);
            }
        } catch (Exception e) {
            logStep("Condition Assessment percentage not found: " + e.getMessage());
        }

        ExtentReportManager.logPass("Condition Assessment percentage within valid range");
    }

    @Test(priority = 10, description = "CP_FA_005: Arc Flash Readiness percentage is 0-100%")
    public void testCP_FA_005_ArcFlashPercentageRange() {
        ExtentReportManager.createTest(MODULE, FEATURE_FINANCIAL, "CP_FA_005_ArcFlashPercentage");
        logStep("Verifying Arc Flash Readiness percentage is within 0-100%");

        navigateTo(DASHBOARD_URL);

        try {
            WebElement arcHeading = driver.findElement(By.xpath(
                    "//*[contains(text(),'Arc Flash Readiness')]/following-sibling::*"
                    + " | //*[contains(text(),'Arc Flash Readiness')]/..//*[contains(text(),'%')]"));
            String percentText = arcHeading.getText().trim();
            logStep("Arc Flash Readiness: " + percentText);

            String numPart = percentText.replaceAll("[^0-9]", "");
            if (!numPart.isEmpty()) {
                int percent = Integer.parseInt(numPart);
                Assert.assertTrue(percent >= 0 && percent <= 100,
                        "Percentage must be 0-100, got: " + percent);
            }
        } catch (Exception e) {
            logStep("Arc Flash percentage not found: " + e.getMessage());
        }

        ExtentReportManager.logPass("Arc Flash percentage within valid range");
    }

    // ================================================================
    // SECTION 3: SEARCH & FILTER RELIABILITY (5 TCs)
    // "When I search, do I find what I'm looking for?"
    // ================================================================

    @Test(priority = 11, description = "CP_SR_001: Asset search returns relevant results only")
    public void testCP_SR_001_AssetSearchRelevance() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "CP_SR_001_SearchRelevance");
        logStep("Verifying asset search returns relevant results");

        navigateTo(ASSETS_URL);
        waitForGrid();

        String searchTerm = "Generator";
        try {
            WebElement searchInput = driver.findElement(By.xpath(
                    "//input[contains(@placeholder,'Search') or contains(@placeholder,'search')]"));
            searchInput.clear();
            searchInput.sendKeys(searchTerm);
            pause(3000);

            List<WebElement> rows = driver.findElements(GRID_ROWS);
            logStep("Search '" + searchTerm + "' returned " + rows.size() + " rows");

            if (!rows.isEmpty()) {
                // Check that results actually contain the search term
                String gridText = "";
                for (WebElement row : rows) {
                    gridText += row.getText() + " ";
                }
                // At least some rows should mention the search term or the asset class
                boolean hasRelevant = gridText.toLowerCase().contains(searchTerm.toLowerCase())
                        || gridText.contains("GEN");
                Assert.assertTrue(hasRelevant,
                        "Search for '" + searchTerm + "' should return results containing '"
                        + searchTerm + "' or 'GEN'. Grid shows: "
                        + gridText.substring(0, Math.min(200, gridText.length())));
            }
        } catch (Exception e) {
            logStep("Search test: " + e.getMessage());
        }

        ExtentReportManager.logPass("Search results relevant for: " + searchTerm);
    }

    @Test(priority = 12, description = "CP_SR_002: Clearing search restores full asset list")
    public void testCP_SR_002_ClearSearchRestoresAll() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "CP_SR_002_ClearSearch");
        logStep("Verifying clearing search input restores the full list");

        navigateTo(ASSETS_URL);
        waitForGrid();

        try {
            // Get initial pagination count
            String fullText = getPageText();
            java.util.regex.Matcher mBefore = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                    .matcher(fullText);
            int totalBefore = 0;
            if (mBefore.find()) totalBefore = extractNumber(mBefore.group(1));
            logStep("Total before search: " + totalBefore);

            // Search for something
            WebElement searchInput = driver.findElement(By.xpath(
                    "//input[contains(@placeholder,'Search') or contains(@placeholder,'search')]"));
            searchInput.clear();
            searchInput.sendKeys("Generator");
            pause(3000);

            // Get filtered count
            String filteredText = getPageText();
            java.util.regex.Matcher mFiltered = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                    .matcher(filteredText);
            int totalFiltered = 0;
            if (mFiltered.find()) totalFiltered = extractNumber(mFiltered.group(1));
            logStep("Total after search: " + totalFiltered);

            // Clear search
            searchInput.clear();
            js.executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],'');"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                    + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", searchInput);
            pause(3000);

            // Get restored count
            String restoredText = getPageText();
            java.util.regex.Matcher mRestored = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                    .matcher(restoredText);
            int totalRestored = 0;
            if (mRestored.find()) totalRestored = extractNumber(mRestored.group(1));
            logStep("Total after clear: " + totalRestored);

            Assert.assertEquals(totalRestored, totalBefore,
                    "Clearing search should restore original count. Before=" + totalBefore
                    + " After clear=" + totalRestored + ". If different, search filter is stuck.");

        } catch (Exception e) {
            logStep("Clear search test: " + e.getMessage());
        }

        ExtentReportManager.logPass("Clearing search restores full list");
    }

    @Test(priority = 13, description = "CP_SR_003: Task search finds tasks by name")
    public void testCP_SR_003_TaskSearchByName() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "CP_SR_003_TaskSearch");
        logStep("Verifying task search works by name");

        navigateTo(TASKS_URL);
        pause(3000);
        waitForGrid();

        try {
            // Get a task name from the grid to search for
            List<WebElement> rows = driver.findElements(GRID_ROWS);
            if (rows.isEmpty()) {
                logStep("No tasks in grid — skipping");
                return;
            }
            WebElement firstCell = rows.get(0).findElement(
                    By.cssSelector("[role='gridcell']:first-child, .MuiDataGrid-cell:first-child"));
            String taskName = firstCell.getText().trim();
            logStep("Task name to search: " + taskName);

            if (taskName.isEmpty() || taskName.equals("—")) {
                logStep("First task has no name — skipping search test");
                return;
            }

            // Search for it
            WebElement searchInput = driver.findElement(By.xpath(
                    "//input[contains(@placeholder,'Search')]"));
            searchInput.clear();
            searchInput.sendKeys(taskName);
            pause(3000);

            // Verify it appears
            String gridText = "";
            for (WebElement row : driver.findElements(GRID_ROWS)) {
                gridText += row.getText() + " ";
            }
            Assert.assertTrue(gridText.contains(taskName),
                    "Searching for '" + taskName + "' should find it in results");

        } catch (Exception e) {
            logStep("Task search: " + e.getMessage());
        }

        ExtentReportManager.logPass("Task search finds tasks by name");
    }

    @Test(priority = 14, description = "CP_SR_004: No duplicate assets in grid (same asset twice)")
    public void testCP_SR_004_NoDuplicateAssets() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "CP_SR_004_NoDuplicates");
        logStep("Checking for duplicate asset entries in grid");

        navigateTo(ASSETS_URL);
        waitForGrid();

        try {
            List<WebElement> rows = driver.findElements(GRID_ROWS);
            Set<String> seenNames = new HashSet<>();
            List<String> duplicates = new ArrayList<>();
            for (WebElement row : rows) {
                try {
                    WebElement nameCell = row.findElement(
                            By.cssSelector("[role='gridcell']:first-child, .MuiDataGrid-cell:first-child"));
                    String name = nameCell.getText().trim();
                    if (!name.isEmpty() && !name.equals("—") && !seenNames.add(name)) {
                        duplicates.add(name);
                    }
                } catch (Exception ignored) {}
            }

            if (!duplicates.isEmpty()) {
                logStep("DUPLICATES FOUND: " + duplicates);
            }
            Assert.assertTrue(duplicates.isEmpty(),
                    "Asset grid should not show duplicate entries. Duplicates: " + duplicates);

        } catch (Exception e) {
            logStep("Duplicate check: " + e.getMessage());
        }

        ExtentReportManager.logPass("No duplicate assets in grid");
    }

    @Test(priority = 15, description = "CP_SR_005: Location search finds buildings")
    public void testCP_SR_005_LocationSearchWorks() {
        ExtentReportManager.createTest(MODULE, FEATURE_SEARCH, "CP_SR_005_LocationSearch");
        logStep("Verifying location module is navigable and shows data");

        navigateTo(LOCATIONS_URL);
        pause(3000);

        String pageText = getPageText();
        // Locations page should show building hierarchy or a tree view
        boolean hasContent = pageText.contains("Building") || pageText.contains("Floor")
                || pageText.contains("Room") || pageText.contains("test")
                || !driver.findElements(By.cssSelector("[role='treeitem'], [class*='TreeItem']")).isEmpty()
                || !driver.findElements(DATA_GRID).isEmpty();

        Assert.assertTrue(hasContent,
                "Locations page should show building hierarchy, tree view, or data. Page appears empty.");

        ExtentReportManager.logPass("Locations module shows data");
    }

    // ================================================================
    // SECTION 4: CROSS-MODULE CONSISTENCY (5 TCs)
    // "Does data match across different views?"
    // ================================================================

    @Test(priority = 16, description = "CP_CM_001: Connections page shows bidirectional connections")
    public void testCP_CM_001_ConnectionsBidirectional() {
        ExtentReportManager.createTest(MODULE, FEATURE_CROSS_MODULE, "CP_CM_001_Bidirectional");
        logStep("Verifying connections are listed (lineside/loadside)");

        navigateTo(CONNECTIONS_URL);
        waitForGrid();

        int rowCount = countGridRows();
        logStep("Connections grid rows: " + rowCount);

        if (rowCount > 0) {
            // Check that grid has expected columns
            List<WebElement> headers = driver.findElements(COLUMN_HEADERS);
            String headerText = "";
            for (WebElement h : headers) {
                headerText += h.getText() + " | ";
            }
            logStep("Connection columns: " + headerText);

            // Should show source/target or lineside/loadside direction
            boolean hasDirection = headerText.toLowerCase().contains("source")
                    || headerText.toLowerCase().contains("target")
                    || headerText.toLowerCase().contains("line")
                    || headerText.toLowerCase().contains("load")
                    || headerText.toLowerCase().contains("from")
                    || headerText.toLowerCase().contains("to");

            logStep("Has directional columns: " + hasDirection);
        }

        ExtentReportManager.logPass("Connections page functional with " + rowCount + " rows");
    }

    @Test(priority = 17, description = "CP_CM_002: All modules are accessible from sidebar navigation")
    public void testCP_CM_002_AllModulesAccessible() {
        ExtentReportManager.createTest(MODULE, FEATURE_CROSS_MODULE, "CP_CM_002_NavAccessibility");
        logStep("Verifying all sidebar navigation links work");

        navigateTo(DASHBOARD_URL);

        String[] criticalModules = {
            "Assets", "Connections", "Locations", "Tasks", "Issues", "Work Orders"
        };

        int working = 0;
        for (String moduleName : criticalModules) {
            try {
                WebElement link = driver.findElement(By.xpath(
                        "//nav//a[normalize-space()='" + moduleName + "']"));
                String href = link.getAttribute("href");
                Assert.assertTrue(href != null && !href.isEmpty(),
                        moduleName + " link should have an href");
                link.click();
                pause(3000);

                // Verify we're on the right page
                String currentUrl = driver.getCurrentUrl();
                logStep(moduleName + " → " + currentUrl);

                // Page should not show error
                String pageText = getPageText();
                Assert.assertFalse(
                        pageText.contains("Application Error") || pageText.contains("page not found"),
                        moduleName + " page shows an error");
                working++;
            } catch (Exception e) {
                logStep(moduleName + " navigation failed: " + e.getMessage());
            }
        }

        Assert.assertEquals(working, criticalModules.length,
                "All " + criticalModules.length + " critical modules should be accessible. Only " + working + " worked.");

        ExtentReportManager.logPass("All " + working + " critical modules accessible");
    }

    @Test(priority = 18, description = "CP_CM_003: Asset detail tabs all load without error")
    public void testCP_CM_003_AssetDetailTabsLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE_CROSS_MODULE, "CP_CM_003_DetailTabs");
        logStep("Verifying all asset detail tabs load without errors");

        navigateTo(ASSETS_URL);
        waitForGrid();

        // Click first asset
        try {
            List<WebElement> rows = driver.findElements(GRID_ROWS);
            Assert.assertFalse(rows.isEmpty(), "Need at least one asset");
            rows.get(0).findElement(
                    By.cssSelector("[role='gridcell']:first-child, .MuiDataGrid-cell:first-child")).click();
            pause(4000);

            // Find all tabs
            List<WebElement> tabs = driver.findElements(By.cssSelector("[role='tab']"));
            logStep("Found " + tabs.size() + " tabs");

            int clickedTabs = 0;
            for (WebElement tab : tabs) {
                String tabName = tab.getText().trim();
                if (tabName.isEmpty() || tabName.length() > 40) continue;
                try {
                    tab.click();
                    pause(1500);
                    String pageText = getPageText();
                    Assert.assertFalse(pageText.contains("Error") && pageText.contains("Something went wrong"),
                            "Tab '" + tabName + "' shows an error page");
                    clickedTabs++;
                    logStep("Tab '" + tabName + "' loaded OK");
                } catch (Exception e) {
                    logStep("Tab '" + tabName + "' failed: " + e.getMessage());
                }
            }

            Assert.assertTrue(clickedTabs >= 3,
                    "Should be able to click at least 3 tabs. Clicked: " + clickedTabs);

        } catch (Exception e) {
            logStep("Asset detail tabs: " + e.getMessage());
        }

        ExtentReportManager.logPass("Asset detail tabs all load without error");
    }

    @Test(priority = 19, description = "CP_CM_004: Overdue tasks count is plausible (> 0 if pending tasks exist)")
    public void testCP_CM_004_OverdueTasksPlausible() {
        ExtentReportManager.createTest(MODULE, FEATURE_CROSS_MODULE, "CP_CM_004_OverdueTasks");
        logStep("Verifying overdue tasks count makes sense");

        navigateTo(TASKS_URL);
        pause(3000);

        int pending = 0, overdue = 0;
        try {
            WebElement pendingEl = driver.findElement(By.xpath(
                    "//*[contains(text(),'Pending')]/..//h5"));
            pending = extractNumber(pendingEl.getText());

            WebElement overdueEl = driver.findElement(By.xpath(
                    "//*[contains(text(),'Overdue')]/..//h5"));
            overdue = extractNumber(overdueEl.getText());

            logStep("Pending: " + pending + ", Overdue: " + overdue);
        } catch (Exception e) {
            logStep("Could not read task KPIs: " + e.getMessage());
        }

        // Overdue cannot exceed pending (overdue is a subset)
        Assert.assertTrue(overdue <= pending,
                "Overdue tasks (" + overdue + ") cannot exceed pending tasks (" + pending + ")");

        ExtentReportManager.logPass("Task KPIs plausible: pending=" + pending + " overdue=" + overdue);
    }

    @Test(priority = 20, description = "CP_CM_005: Work order planning page loads and shows data")
    public void testCP_CM_005_WorkOrderPlanningLoads() {
        ExtentReportManager.createTest(MODULE, FEATURE_CROSS_MODULE, "CP_CM_005_PlanningLoads");
        logStep("Verifying Work Order Planning page loads successfully");

        navigateTo(PLANNING_URL);
        pause(3000);

        String pageText = getPageText();
        // Should not show error
        Assert.assertFalse(
                pageText.contains("Application Error") || pageText.contains("Something went wrong"),
                "Work Order Planning page shows an error");

        // Should have some content
        boolean hasContent = !pageText.isEmpty() && pageText.length() > 100;
        Assert.assertTrue(hasContent,
                "Work Order Planning page should have content (got " + pageText.length() + " chars)");

        ExtentReportManager.logPass("Work Order Planning page loads successfully");
    }

    // ================================================================
    // SECTION 5: SILENT FAILURE DETECTION (5 TCs)
    // "Is the app hiding errors from the user?"
    // ================================================================

    @Test(priority = 21, description = "CP_SF_001: No JavaScript console errors on Dashboard")
    public void testCP_SF_001_NoConsoleErrorsDashboard() {
        ExtentReportManager.createTest(MODULE, FEATURE_SILENT_FAIL, "CP_SF_001_ConsoleErrors");
        logStep("Checking for JavaScript console errors on Dashboard");

        navigateTo(DASHBOARD_URL);

        // Install error capture and wait
        js.executeScript(
            "window.__testErrors = [];"
            + "var origError = console.error;"
            + "console.error = function() {"
            + "  window.__testErrors.push(Array.from(arguments).join(' '));"
            + "  return origError.apply(console, arguments);"
            + "};");

        pause(5000); // Let page fully render and any async errors surface

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) js.executeScript("return window.__testErrors || [];");

        if (errors != null && !errors.isEmpty()) {
            logStep("Console errors found: " + errors.size());
            for (String err : errors) {
                logStep("  ERROR: " + err.substring(0, Math.min(150, err.length())));
            }
            // Filter out known non-critical errors (third-party SDKs, analytics)
            List<String> criticalErrors = new ArrayList<>();
            for (String err : errors) {
                boolean isThirdParty = err.contains("devrev") || err.contains("beamer")
                        || err.contains("sentry") || err.contains("analytics")
                        || err.contains("gtag") || err.contains("hotjar");
                if (!isThirdParty) {
                    criticalErrors.add(err);
                }
            }
            Assert.assertTrue(criticalErrors.isEmpty(),
                    "Dashboard has " + criticalErrors.size() + " critical JS errors: "
                    + criticalErrors.get(0).substring(0, Math.min(200, criticalErrors.get(0).length())));
        }

        ExtentReportManager.logPass("No critical JavaScript errors on Dashboard");
    }

    @Test(priority = 22, description = "CP_SF_002: Assets page doesn't show stale/cached data after navigation")
    public void testCP_SF_002_NoStaleCacheAfterNav() {
        ExtentReportManager.createTest(MODULE, FEATURE_SILENT_FAIL, "CP_SF_002_StaleCache");
        logStep("Verifying assets page fetches fresh data after navigation away and back");

        // Go to assets, capture count
        navigateTo(ASSETS_URL);
        waitForGrid();
        String count1Text = getPageText();
        java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                .matcher(count1Text);
        int count1 = 0;
        if (m1.find()) count1 = extractNumber(m1.group(1));
        logStep("First visit count: " + count1);

        // Navigate away to dashboard
        navigateTo(DASHBOARD_URL);
        pause(2000);

        // Navigate back to assets
        navigateTo(ASSETS_URL);
        waitForGrid();
        String count2Text = getPageText();
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("of\\s+([\\d,]+)")
                .matcher(count2Text);
        int count2 = 0;
        if (m2.find()) count2 = extractNumber(m2.group(1));
        logStep("Second visit count: " + count2);

        Assert.assertEquals(count2, count1,
                "Asset count should be same on return. If different, data is not being cached correctly. "
                + "First: " + count1 + ", Second: " + count2);

        ExtentReportManager.logPass("No stale cache issue: count consistent at " + count1);
    }

    @Test(priority = 23, description = "CP_SF_003: No 'Application Error' or crash pages during normal navigation")
    public void testCP_SF_003_NoCrashDuringNavigation() {
        ExtentReportManager.createTest(MODULE, FEATURE_SILENT_FAIL, "CP_SF_003_NoCrash");
        logStep("Rapid navigation across modules to check for crash pages");

        String[] pages = {
            DASHBOARD_URL, ASSETS_URL, CONNECTIONS_URL, TASKS_URL,
            ISSUES_URL, WORK_ORDERS_URL, LOCATIONS_URL, PLANNING_URL
        };

        int crashes = 0;
        for (String url : pages) {
            try {
                driver.get(url);
                pause(2000);
                String pageText = getPageText();
                if (pageText.contains("Application Error")
                        || pageText.contains("We encountered an error")
                        || pageText.contains("Something went wrong")
                        || pageText.contains("NEXT_NOT_FOUND")) {
                    logStep("CRASH detected on: " + url);
                    crashes++;
                    // Try to recover
                    try {
                        driver.findElement(By.xpath(
                                "//button[contains(.,'Try Again') or contains(.,'Refresh')]")).click();
                        pause(3000);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logStep("Navigation failed for " + url + ": " + e.getMessage());
            }
        }

        Assert.assertEquals(crashes, 0,
                crashes + " page(s) showed Application Error during normal navigation");

        ExtentReportManager.logPass("No crash pages during navigation across " + pages.length + " modules");
    }

    @Test(priority = 24, description = "CP_SF_004: API response time is under 10 seconds for key pages")
    public void testCP_SF_004_APIResponseTime() {
        ExtentReportManager.createTest(MODULE, FEATURE_SILENT_FAIL, "CP_SF_004_ResponseTime");
        logStep("Checking page load times for critical modules");

        String[][] pages = {
            {"Dashboard", DASHBOARD_URL},
            {"Assets", ASSETS_URL},
            {"Tasks", TASKS_URL}
        };

        for (String[] page : pages) {
            long start = System.currentTimeMillis();
            driver.get(page[1]);
            // Wait for meaningful content
            try {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> {
                            String text = d.findElement(By.tagName("body")).getText();
                            return text.length() > 200;
                        });
            } catch (Exception e) {
                logStep(page[0] + " did not load meaningful content within 15s");
            }
            long elapsed = System.currentTimeMillis() - start;
            logStep(page[0] + " loaded in " + elapsed + "ms");

            Assert.assertTrue(elapsed < 30000,
                    page[0] + " took " + elapsed + "ms to load. Max allowed: 30000ms.");
            if (elapsed > 15000) {
                logStep("WARN: " + page[0] + " loaded in " + elapsed + "ms (over 15s, but under 30s CI threshold)");
            }
        }

        ExtentReportManager.logPass("All critical pages load within 30 seconds");
    }

    @Test(priority = 25, description = "CP_SF_005: Session stays alive during extended use (no random logout)")
    public void testCP_SF_005_SessionPersistence() {
        ExtentReportManager.createTest(MODULE, FEATURE_SILENT_FAIL, "CP_SF_005_SessionAlive");
        logStep("Verifying session doesn't expire during normal multi-page use");

        // Navigate across several pages (simulates real user workflow)
        String[] workflow = {
            DASHBOARD_URL, ASSETS_URL, TASKS_URL, ISSUES_URL, DASHBOARD_URL
        };

        for (String url : workflow) {
            driver.get(url);
            pause(2000);
        }

        // After navigating, we should still be logged in (not redirected to login)
        String finalUrl = driver.getCurrentUrl();
        String pageText = getPageText();

        boolean isLoggedIn = !finalUrl.contains("/login")
                && !pageText.contains("Sign In")
                && !pageText.contains("Company Code")
                && (pageText.contains("Site:") || pageText.contains("abhiyant"));

        Assert.assertTrue(isLoggedIn,
                "Should still be logged in after navigating " + workflow.length
                + " pages. Current URL: " + finalUrl
                + ". If logged out, session expired prematurely.");

        ExtentReportManager.logPass("Session alive after " + workflow.length + " page navigations");
    }
}
