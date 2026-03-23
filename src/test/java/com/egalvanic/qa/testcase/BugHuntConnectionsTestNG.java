package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Bug Hunt: Connections Page Bug Verification Tests
 *
 * Verifies bugs on the /connections page:
 *   BUG-006: Connections Grid Shows All Identical Rows
 *   BUG-016: Arc Flash Readiness at 2% (5 of 308 required fields)
 *
 * Tests share a browser session (inherited from BaseTest).
 */
public class BugHuntConnectionsTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private void navigateToConnections() {
        driver.get(AppConstants.BASE_URL + "/connections");
        pause(6000);
    }

    // ================================================================
    // BUG-006: Grid Shows All Identical Rows
    // ================================================================

    @Test(priority = 1, description = "BUG-006: Verify connections grid shows identical/duplicate rows")
    public void testBUG006_ConnectionsIdenticalRows() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_CONNECTIONS_GRID,
                "BUG-006: Identical Grid Rows");

        try {
            navigateToConnections();
            logStep("Navigated to Connections page");

            // Extract all visible row data from the grid
            String rowData = (String) js().executeScript(
                "var rows = document.querySelectorAll('[role=\"row\"]');" +
                "var data = [], uniqueRows = new Set();" +
                "for (var i = 0; i < rows.length; i++) {" +
                "  var cells = rows[i].querySelectorAll('[role=\"cell\"]');" +
                "  if (cells.length < 2) continue;" + // skip header
                "  var rowText = '';" +
                "  for (var j = 0; j < Math.min(cells.length, 3); j++) {" +
                "    rowText += cells[j].textContent.trim() + ' | ';" +
                "  }" +
                "  data.push(rowText);" +
                "  uniqueRows.add(rowText);" +
                "}" +
                "return 'totalRows=' + data.length + ',uniqueRows=' + uniqueRows.size + " +
                "  ',sample=' + (data.length > 0 ? data[0] : 'EMPTY');");

            logStep("Connection grid analysis: " + rowData);
            logStepWithScreenshot("Connections grid — checking for duplicates");

            // Parse counts
            int totalRows = 0, uniqueRows = 0;
            for (String part : rowData.split(",")) {
                if (part.startsWith("totalRows=")) totalRows = Integer.parseInt(part.split("=")[1]);
                if (part.startsWith("uniqueRows=")) uniqueRows = Integer.parseInt(part.split("=")[1]);
            }

            logStep("Total visible rows: " + totalRows);
            logStep("Unique rows: " + uniqueRows);

            if (totalRows > 1) {
                // BUG confirmed if all rows are identical (uniqueRows == 1)
                boolean allIdentical = uniqueRows == 1;
                boolean manyDuplicates = uniqueRows < totalRows / 2;

                logStep("All rows identical: " + allIdentical);
                logStep("Significant duplicates: " + manyDuplicates);

                Assert.assertTrue(allIdentical || manyDuplicates,
                        "BUG-006: Grid has " + uniqueRows + " unique rows out of " +
                        totalRows + ". Bug may be fixed.");

                ExtentReportManager.logPass("BUG-006 confirmed: " + uniqueRows + " unique out of " +
                        totalRows + " rows — " + (allIdentical ? "ALL identical" : "many duplicates"));
            } else {
                ExtentReportManager.logPass("BUG-006: Grid has " + totalRows +
                        " rows — insufficient data to verify duplicate bug");
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG006_identical_error");
            Assert.fail("BUG-006 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-016: Arc Flash Readiness at 2%
    // ================================================================

    @Test(priority = 2, description = "BUG-016: Verify Arc Flash Readiness shows very low completion")
    public void testBUG016_ArcFlashReadinessLow() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_ARC_FLASH,
                "BUG-016: Arc Flash Readiness Low");

        try {
            navigateToConnections();
            logStep("On Connections page — checking Arc Flash readiness banner");

            // Look for Arc Flash readiness text
            String readinessText = (String) js().executeScript(
                "var body = document.body.innerText;" +
                "var match = body.match(/(\\d+)\\s*of\\s*(\\d+)\\s*required\\s*fields/i);" +
                "if (match) return 'completed=' + match[1] + ',total=' + match[2];" +
                "var pctMatch = body.match(/(\\d+)%\\s*(?:complete|ready)/i);" +
                "if (pctMatch) return 'percent=' + pctMatch[1];" +
                "return 'NOT_FOUND';");

            logStep("Arc Flash readiness: " + readinessText);
            logStepWithScreenshot("Connections page — Arc Flash readiness banner");

            if (readinessText.contains("completed=")) {
                int completed = 0, total = 0;
                for (String part : readinessText.split(",")) {
                    String[] kv = part.split("=");
                    if ("completed".equals(kv[0])) completed = Integer.parseInt(kv[1]);
                    if ("total".equals(kv[0])) total = Integer.parseInt(kv[1]);
                }

                double pct = total > 0 ? (double) completed / total * 100 : 0;
                logStep("Arc Flash: " + completed + "/" + total + " = " +
                        String.format("%.1f", pct) + "% complete");

                Assert.assertTrue(pct < 10,
                        "BUG-016: Arc Flash is now at " + String.format("%.1f", pct) +
                        "%. Bug may be addressed.");

                ExtentReportManager.logPass("BUG-016 confirmed: Arc Flash at " +
                        String.format("%.1f", pct) + "% (" + completed + "/" + total + " fields)");
            } else {
                ExtentReportManager.logPass("BUG-016: Arc Flash readiness text = " + readinessText);
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG016_arcflash_error");
            Assert.fail("BUG-016 test failed: " + e.getMessage());
        }
    }
}
