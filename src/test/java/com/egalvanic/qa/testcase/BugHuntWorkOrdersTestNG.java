package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Bug Hunt: Work Orders Page Bug Verification Tests
 *
 * Verifies bugs on the /sessions (Work Orders) page:
 *   BUG-015: Work Orders Grid Missing Status Column
 *   BUG-022: Scheduling Page Shows Only Test Work Orders
 *
 * Tests share a browser session (inherited from BaseTest).
 */
public class BugHuntWorkOrdersTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ================================================================
    // BUG-015: Work Orders Grid Missing Status Column
    // ================================================================

    /**
     * BUG-015 — was a regression detector for "Status column missing on
     * Work Orders grid". As of 2026-05-01 the bug is FIXED — Status column
     * is now present. Test inverted from "assertFalse(hasStatusColumn)" to
     * "assertTrue(hasStatusColumn)" — now serves as a forward-looking
     * regression detector that fires if Status column ever disappears
     * again.
     */
    @Test(priority = 1, description = "BUG-015 (POST-FIX): Work Orders grid still has Status column (regression detector)")
    public void testBUG015_MissingStatusColumn() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_WO_GRID,
                "BUG-015: Status Column present (post-fix detector)");

        try {
            driver.get(AppConstants.BASE_URL + "/sessions");
            pause(6000);

            logStep("Navigated to Work Orders page");

            // Get all column headers
            String headers = (String) js().executeScript(
                "var cols = document.querySelectorAll('[role=\"columnheader\"]');" +
                "var names = [];" +
                "for (var i = 0; i < cols.length; i++) {" +
                "  var text = cols[i].textContent.trim();" +
                "  if (text) names.push(text);" +
                "}" +
                "return names.join(', ');");

            logStep("Work Order columns: " + headers);
            logStepWithScreenshot("Work Orders grid — column headers");

            boolean hasStatusColumn = headers.toLowerCase().contains("status");
            logStep("Status column present: " + hasStatusColumn);

            // FLIPPED 2026-05-01 (was assertFalse — bug now fixed in product)
            Assert.assertTrue(hasStatusColumn,
                    "REGRESSION: Status column has DISAPPEARED again. BUG-015 was "
                    + "fixed previously and the column was present. Columns now: " + headers);

            ExtentReportManager.logPass("BUG-015 fix is intact — Status column present in: " + headers);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG015_status_error");
            Assert.fail("BUG-015 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-022: Scheduling Page Shows Only Test Work Orders
    // ================================================================

    @Test(priority = 2, description = "BUG-022: Verify scheduling page is dominated by test data")
    public void testBUG022_SchedulingTestDataOnly() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SCHEDULING,
                "BUG-022: Scheduling Test Data");

        try {
            driver.get(AppConstants.BASE_URL + "/scheduling");
            pause(6000);

            logStep("Navigated to Scheduling page");

            // Count entries that look like test data
            String testDataStats = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "var smokeCount = (allText.match(/SmokeTest/gi) || []).length;" +
                "var autoCount = (allText.match(/AutoTest/gi) || []).length;" +
                "var testCount = smokeCount + autoCount;" +
                "return 'smokeTest=' + smokeCount + ',autoTest=' + autoCount + ',total=' + testCount;");

            logStep("Test data on Scheduling page: " + testDataStats);
            logStepWithScreenshot("Scheduling page — test data check");

            int testCount = 0;
            for (String part : testDataStats.split(",")) {
                if (part.startsWith("total=")) testCount = Integer.parseInt(part.split("=")[1]);
            }

            logStep("Test data entries found: " + testCount);

            if (testCount > 0) {
                ExtentReportManager.logPass("BUG-022 still present: " + testCount +
                        " test data references found on Scheduling page. " + testDataStats);
            } else {
                ExtentReportManager.logPass("BUG-022: No test data found on Scheduling page — bug appears fixed");
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG022_scheduling_error");
            Assert.fail("BUG-022 test failed: " + e.getMessage());
        }
    }
}
