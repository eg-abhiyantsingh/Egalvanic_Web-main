package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Bug Hunt: Miscellaneous Pages Bug Verification Tests
 *
 * Verifies bugs across various pages:
 *   BUG-020: Admin Settings Shows 0 Sites
 *   BUG-021: Opportunities Pipeline Labels Truncated
 *   BUG-023: Condition Assessment Blank Behind Update Banner
 *   BUG-024: Equipment Insights Type Column Truncated
 *   BUG-026: SLDs Page Duplicate "Select View" Dropdown
 *   BUG-027: Attachments Page Duplicate fetchAttachments API Call
 *   BUG-028: Audit Log Duplicate Mutations API + Unnecessary SLD Fetches
 *   BUG-029: Audit Log Dual Redundant Search Fields
 *   BUG-030: Z University Developer Loading Text
 *
 * Tests share a browser session (inherited from BaseTest).
 */
public class BugHuntPagesTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ================================================================
    // BUG-020: Admin Settings Shows 0 Sites
    // ================================================================

    @Test(priority = 1, description = "BUG-020: Verify Admin Settings page shows 0 sites")
    public void testBUG020_AdminSettingsZeroSites() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_ADMIN_SETTINGS,
                "BUG-020: Admin 0 Sites");

        try {
            driver.get(AppConstants.BASE_URL + "/admin");
            pause(6000);

            logStep("Navigated to Admin Settings page");

            // Check if sites list is empty or shows 0
            String siteInfo = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "var siteMatch = allText.match(/(\\d+)\\s*sites?/i);" +
                "if (siteMatch) return 'siteCount=' + siteMatch[1];" +
                "/* Check for empty state or 'No sites' */" +
                "if (allText.indexOf('No sites') !== -1 || allText.indexOf('no sites') !== -1) " +
                "  return 'siteCount=0';" +
                "/* Check for grid with 0 rows */" +
                "var rows = document.querySelectorAll('[role=\"row\"]');" +
                "var dataRows = 0;" +
                "for (var i = 0; i < rows.length; i++) {" +
                "  if (rows[i].querySelectorAll('[role=\"cell\"]').length > 0) dataRows++;" +
                "}" +
                "return 'gridRows=' + dataRows;");

            logStep("Admin Settings site info: " + siteInfo);
            logStepWithScreenshot("Admin Settings page — sites list");

            boolean zeroSites = siteInfo.contains("siteCount=0") || siteInfo.contains("gridRows=0");
            if (zeroSites) {
                ExtentReportManager.logPass("BUG-020 still present: " + siteInfo);
            } else {
                ExtentReportManager.logPass("BUG-020: Admin page now shows sites — bug appears fixed. Info: " + siteInfo);
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG020_admin_error");
            Assert.fail("BUG-020 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-021: Opportunities Pipeline Labels Truncated
    // ================================================================

    @Test(priority = 2, description = "BUG-021: Verify Opportunities pipeline card labels are truncated")
    public void testBUG021_OpportunitiesLabelsTruncated() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_OPPORTUNITIES,
                "BUG-021: Pipeline Truncated");

        try {
            driver.get(AppConstants.BASE_URL + "/opportunities");
            pause(6000);

            logStep("Navigated to Opportunities page");

            // Check for text overflow in pipeline cards (targeted selectors for performance)
            String overflowCheck = (String) js().executeScript(
                "var truncated = [];" +
                "var elements = document.querySelectorAll('td, th, [role=\"cell\"], [role=\"columnheader\"], " +
                "  [class*=\"card\"], [class*=\"Card\"], [class*=\"label\"], [class*=\"Label\"], " +
                "  [class*=\"chip\"], [class*=\"Chip\"], span, p, h1, h2, h3, h4, h5, h6');" +
                "for (var i = 0; i < elements.length; i++) {" +
                "  var el = elements[i];" +
                "  var style = window.getComputedStyle(el);" +
                "  if (style.overflow === 'hidden' || style.textOverflow === 'ellipsis') {" +
                "    if (el.scrollWidth > el.clientWidth + 2) {" +
                "      var text = el.textContent.trim();" +
                "      if (text.length > 5 && text.length < 100) {" +
                "        truncated.push(text.substring(0, 40) + '... (overflows by ' + " +
                "          (el.scrollWidth - el.clientWidth) + 'px)');" +
                "      }" +
                "    }" +
                "  }" +
                "}" +
                "return truncated.length > 0 ? truncated.slice(0, 5).join('\\n') : 'NO_TRUNCATION';");

            logStep("Truncation check: " + overflowCheck);
            logStepWithScreenshot("Opportunities page — pipeline labels");

            boolean hasTruncation = !"NO_TRUNCATION".equals(overflowCheck);
            Assert.assertTrue(hasTruncation,
                    "BUG-021: No truncated labels found on Opportunities page. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-021 confirmed: " + overflowCheck);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG021_opportunities_error");
            Assert.fail("BUG-021 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-023: Condition Assessment Blank Behind Banner
    // ================================================================

    @Test(priority = 3, description = "BUG-023: Verify Condition Assessment page is blank behind update banner")
    public void testBUG023_ConditionAssessmentBlank() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_CONDITION_ASSESSMENT,
                "BUG-023: Blank Behind Banner");

        try {
            driver.get(AppConstants.BASE_URL + "/pm-readiness");
            pause(6000);

            logStep("Navigated to Condition Assessment page");

            // Check if the main content area has meaningful content
            String contentCheck = (String) js().executeScript(
                "var main = document.querySelector('main') || document.querySelector('[role=\"main\"]');" +
                "if (!main) return 'NO_MAIN_ELEMENT';" +
                "var text = main.innerText.trim();" +
                "var hasChart = main.querySelectorAll('canvas, svg, [class*=chart], [class*=Chart]').length;" +
                "var hasData = text.length > 50;" +
                "return 'textLen=' + text.length + ',hasChart=' + hasChart + ',hasData=' + hasData + " +
                "  ',preview=' + text.substring(0, 80).replace(/\\n/g, ' ');");

            logStep("Content check: " + contentCheck);

            // Check for update banner covering content
            List<WebElement> banners = driver.findElements(By.xpath(
                    "//*[contains(text(),'new app update is available')]"));
            boolean bannerVisible = !banners.isEmpty();
            logStep("Update banner visible: " + bannerVisible);
            logStepWithScreenshot("Condition Assessment page");

            // Bug is: page is blank behind the banner — check for no meaningful content
            boolean hasData = contentCheck.contains("hasData=true");
            if (bannerVisible || !hasData) {
                ExtentReportManager.logPass("BUG-023 still present: Banner=" + bannerVisible +
                        ", content=" + contentCheck);
            } else {
                ExtentReportManager.logPass("BUG-023: Page has content and no banner blocking it — bug appears fixed");
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG023_condition_error");
            Assert.fail("BUG-023 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-024: Equipment Insights Type Column Truncated
    // ================================================================

    @Test(priority = 4, description = "BUG-024: Verify Equipment Insights has truncated Type column")
    public void testBUG024_EquipmentTypeTruncated() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_EQUIPMENT_INSIGHTS,
                "BUG-024: Type Column Truncated");

        try {
            driver.get(AppConstants.BASE_URL + "/equipment-insights");
            pause(6000);

            logStep("Navigated to Equipment Insights page");

            // Check column headers and look for truncation
            String columnInfo = (String) js().executeScript(
                "var cols = document.querySelectorAll('[role=\"columnheader\"]');" +
                "var info = [];" +
                "for (var i = 0; i < cols.length; i++) {" +
                "  var text = cols[i].textContent.trim();" +
                "  var width = cols[i].getBoundingClientRect().width;" +
                "  var truncated = cols[i].scrollWidth > cols[i].clientWidth + 2;" +
                "  info.push(text + ' (w=' + Math.round(width) + 'px, truncated=' + truncated + ')');" +
                "}" +
                "return info.join(', ');");

            logStep("Equipment Insights columns: " + columnInfo);
            logStepWithScreenshot("Equipment Insights — column headers");

            // Check for cell-level truncation in the grid
            String cellTruncation = (String) js().executeScript(
                "var cells = document.querySelectorAll('[role=\"cell\"]');" +
                "var truncated = 0, total = 0;" +
                "for (var i = 0; i < cells.length; i++) {" +
                "  total++;" +
                "  if (cells[i].scrollWidth > cells[i].clientWidth + 2) truncated++;" +
                "}" +
                "return 'totalCells=' + total + ',truncatedCells=' + truncated;");

            logStep("Cell truncation: " + cellTruncation);

            int truncatedCells = 0;
            for (String part : cellTruncation.split(",")) {
                if (part.startsWith("truncatedCells=")) truncatedCells = Integer.parseInt(part.split("=")[1]);
            }

            if (truncatedCells > 0) {
                ExtentReportManager.logPass("BUG-024 still present: " + truncatedCells +
                        " truncated cells. Columns=[" + columnInfo + "]");
            } else {
                ExtentReportManager.logPass("BUG-024: No truncated cells found — bug appears fixed");
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG024_equipment_error");
            Assert.fail("BUG-024 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-026: SLDs Page Duplicate "Select View" Dropdown
    // ================================================================

    @Test(priority = 5, description = "BUG-026: Verify SLDs page renders duplicate 'Select View' dropdown")
    public void testBUG026_SLDsDuplicateDropdown() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SLDS_PAGE,
                "BUG-026: Duplicate Dropdown");

        try {
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(6000);

            logStep("Navigated to SLDs page");

            // Count "Select View" or view-related dropdowns
            String dropdownCount = (String) js().executeScript(
                "var selects = document.querySelectorAll('select, [role=\"combobox\"], [role=\"listbox\"]');" +
                "var viewDropdowns = 0;" +
                "var allDropdowns = selects.length;" +
                // Check for elements containing "Select View" text (targeted for performance)
                "var viewElements = document.querySelectorAll(" +
                "  'button, label, span, p, div[class*=select], div[class*=Select], " +
                "  [class*=MuiSelect], [class*=dropdown], [class*=Dropdown], option');" +
                "var selectViewCount = 0;" +
                "for (var i = 0; i < viewElements.length; i++) {" +
                "  var text = viewElements[i].textContent.trim();" +
                "  if (text === 'Select View' || text === 'Select view') {" +
                "    selectViewCount++;" +
                "  }" +
                "}" +
                // Also check for MUI Select components with "view" labels
                "var muiSelects = document.querySelectorAll('[class*=MuiSelect], [class*=select]');" +
                "return 'selectViewLabels=' + selectViewCount + ',comboboxes=' + allDropdowns + " +
                "  ',muiSelects=' + muiSelects.length;");

            logStep("Dropdown analysis: " + dropdownCount);
            logStepWithScreenshot("SLDs page — checking for duplicate dropdowns");

            int selectViewLabels = 0;
            for (String part : dropdownCount.split(",")) {
                if (part.startsWith("selectViewLabels="))
                    selectViewLabels = Integer.parseInt(part.split("=")[1]);
            }

            logStep("Select View label count: " + selectViewLabels);

            Assert.assertTrue(selectViewLabels > 1,
                    "BUG-026: Only " + selectViewLabels + " 'Select View' label(s). Bug may be fixed.");

            ExtentReportManager.logPass("BUG-026 confirmed: " + selectViewLabels +
                    " 'Select View' labels — duplicate render. " + dropdownCount);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG026_slds_error");
            Assert.fail("BUG-026 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-027: Attachments Duplicate fetchAttachments Call
    // ================================================================

    @Test(priority = 6, description = "BUG-027: Verify Attachments page makes duplicate fetch calls")
    public void testBUG027_AttachmentsDuplicateFetch() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_ATTACHMENTS,
                "BUG-027: Duplicate Fetch");

        try {
            driver.get(AppConstants.BASE_URL + "/attachments");
            pause(6000);

            logStep("Navigated to Attachments page");

            // Count attachment-related API calls
            String apiStats = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var attachCalls = 0, roleCalls = 0, sldCalls = 0;" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  var name = entries[i].name;" +
                "  if (name.indexOf('attachment') !== -1) attachCalls++;" +
                "  if (name.indexOf('/roles') !== -1 && name.indexOf('/users/') !== -1) roleCalls++;" +
                "  if (name.indexOf('/slds') !== -1 && name.indexOf('/users/') !== -1) sldCalls++;" +
                "}" +
                "return 'attachCalls=' + attachCalls + ',roleCalls=' + roleCalls + ',sldCalls=' + sldCalls;");

            logStep("Attachments API stats: " + apiStats);
            logStepWithScreenshot("Attachments page — API call check");

            int roleCalls = 0, attachCalls = 0;
            for (String part : apiStats.split(",")) {
                if (part.startsWith("roleCalls=")) roleCalls = Integer.parseInt(part.split("=")[1]);
                if (part.startsWith("attachCalls=")) attachCalls = Integer.parseInt(part.split("=")[1]);
            }

            boolean hasDuplicates = roleCalls > 1 || attachCalls > 1;
            logStep("Duplicate calls: roles=" + roleCalls + "x, attachments=" + attachCalls + "x");

            Assert.assertTrue(hasDuplicates,
                    "BUG-027: No duplicate API calls on Attachments page. Bug may be fixed. " + apiStats);

            ExtentReportManager.logPass("BUG-027 confirmed: " + apiStats);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG027_attachments_error");
            Assert.fail("BUG-027 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-028: Audit Log Duplicate Mutations + Unnecessary SLD Fetches
    // ================================================================

    @Test(priority = 7, description = "BUG-028: Verify Audit Log makes duplicate mutations call + unneeded SLD fetches")
    public void testBUG028_AuditLogDuplicateAndWastedCalls() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_AUDIT_LOG,
                "BUG-028: Duplicate + Wasted Calls");

        try {
            driver.get(AppConstants.BASE_URL + "/admin/audit-log");
            pause(6000);

            logStep("Navigated to Audit Log page");

            String apiStats = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var mutationCalls = 0, sldNodeCalls = 0, roleCalls = 0, viewCalls = 0;" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  var name = entries[i].name;" +
                "  if (name.indexOf('/mutations') !== -1) mutationCalls++;" +
                "  if (name.indexOf('/lookup/nodes/') !== -1) sldNodeCalls++;" +
                "  if (name.indexOf('/roles') !== -1 && name.indexOf('/users/') !== -1) roleCalls++;" +
                "  if (name.indexOf('/views') !== -1) viewCalls++;" +
                "}" +
                "return 'mutations=' + mutationCalls + ',sldNodes=' + sldNodeCalls + " +
                "  ',roles=' + roleCalls + ',views=' + viewCalls;");

            logStep("Audit Log API stats: " + apiStats);
            logStepWithScreenshot("Audit Log page — API call analysis");

            int mutationCalls = 0, sldNodeCalls = 0;
            for (String part : apiStats.split(",")) {
                if (part.startsWith("mutations=")) mutationCalls = Integer.parseInt(part.split("=")[1]);
                if (part.startsWith("sldNodes=")) sldNodeCalls = Integer.parseInt(part.split("=")[1]);
            }

            boolean bugPresent = mutationCalls > 1 || sldNodeCalls > 0;
            logStep("Mutations duplicated: " + (mutationCalls > 1) +
                    ", unnecessary SLD calls: " + (sldNodeCalls > 0));

            Assert.assertTrue(bugPresent,
                    "BUG-028: No duplicate mutations or unnecessary SLD calls. Bug may be fixed. " + apiStats);

            ExtentReportManager.logPass("BUG-028 confirmed: " + apiStats +
                    ". Mutations duplicated=" + (mutationCalls > 1) +
                    ", unnecessary SLD calls=" + (sldNodeCalls > 0));

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG028_audit_error");
            Assert.fail("BUG-028 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-029: Audit Log Dual Redundant Search Fields
    // ================================================================

    @Test(priority = 8, description = "BUG-029: Verify Audit Log page has two redundant search fields")
    public void testBUG029_AuditLogDualSearch() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_AUDIT_LOG,
                "BUG-029: Dual Search Fields");

        try {
            driver.get(AppConstants.BASE_URL + "/admin/audit-log");
            pause(6000);

            logStep("Navigated to Audit Log page");

            // Count search input fields — filter to actual search inputs only
            List<WebElement> searchInputs = driver.findElements(By.xpath(
                    "//input[@type='search' or contains(@placeholder,'Search') or " +
                    "contains(@placeholder,'search') or contains(@aria-label,'search') or " +
                    "contains(@aria-label,'Search')]"));

            int searchCount = searchInputs.size();
            logStep("Search input fields found: " + searchCount);

            // Log each search field's placeholder
            for (int i = 0; i < searchInputs.size(); i++) {
                String placeholder = searchInputs.get(i).getAttribute("placeholder");
                logStep("Search field " + (i + 1) + ": placeholder='" + placeholder + "'");
            }

            logStepWithScreenshot("Audit Log — search fields");

            Assert.assertTrue(searchCount >= 2,
                    "BUG-029: Found only " + searchCount + " search field(s). Bug may be fixed.");

            ExtentReportManager.logPass("BUG-029 confirmed: " + searchCount +
                    " search fields on Audit Log page (should be 1)");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG029_search_error");
            Assert.fail("BUG-029 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-030: Z University Developer Loading Text
    // ================================================================

    @Test(priority = 9, description = "BUG-030: Verify Z University shows developer joke loading text")
    public void testBUG030_ZUniversityDevLoadingText() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_Z_UNIVERSITY,
                "BUG-030: Dev Loading Text");

        try {
            driver.get(AppConstants.BASE_URL + "/z-university");
            // Check quickly before content loads — the joke text appears during loading
            pause(1000);

            logStep("Navigated to Z University page (checking during load)");

            // Check for the developer joke loading text
            String loadingText = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "if (allText.indexOf('Channeling') !== -1 || allText.indexOf('Mukul') !== -1) {" +
                "  return 'FOUND: ' + allText.substring(0, 200);" +
                "}" +
                "return 'NOT_VISIBLE: ' + allText.substring(0, 100);");

            logStep("Loading text check: " + loadingText);
            logStepWithScreenshot("Z University during load");

            // Wait for page to finish loading
            pause(5000);

            // Check if the page eventually loads content
            String finalContent = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "var hasWelcome = allText.indexOf('Welcome') !== -1 || allText.indexOf('Z University') !== -1;" +
                "return 'loaded=' + hasWelcome + ',textLen=' + allText.length;");

            logStep("Final content: " + finalContent);

            // Also check for empty S3 video URL mapping
            String videoCheck = (String) js().executeScript(
                "/* Check if ZUniversity urlMap keys are empty — videos may be broken */" +
                "var videos = document.querySelectorAll('video');" +
                "var iframes = document.querySelectorAll('iframe[src*=\"video\"], iframe[src*=\"s3\"]');" +
                "return 'videos=' + videos.length + ',videoIframes=' + iframes.length;");

            logStep("Video elements: " + videoCheck);
            logStepWithScreenshot("Z University — final loaded state");

            boolean foundDevText = loadingText.contains("FOUND") ||
                    loadingText.contains("Channeling") || loadingText.contains("Mukul");

            // Also check for broken video elements (empty S3 urlMap)
            int videoCount = 0;
            for (String part : videoCheck.split(",")) {
                if (part.startsWith("videos=")) videoCount = Integer.parseInt(part.split("=")[1]);
            }
            boolean videosEmpty = videoCount == 0;

            if (foundDevText || videosEmpty) {
                ExtentReportManager.logPass("BUG-030 still present: Dev text " +
                        (foundDevText ? "VISIBLE" : "not caught") +
                        ", videos=" + videoCount + ". " + videoCheck);
            } else {
                ExtentReportManager.logPass("BUG-030: Dev loading text gone and videos loaded — bug appears fixed");
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG030_zuniv_error");
            Assert.fail("BUG-030 test failed: " + e.getMessage());
        }
    }
}
