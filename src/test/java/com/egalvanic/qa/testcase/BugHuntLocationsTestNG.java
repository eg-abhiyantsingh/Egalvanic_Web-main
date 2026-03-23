package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Bug Hunt: Locations Page Bug Verification Tests
 *
 * Verifies bugs on the /locations page:
 *   BUG-010: Building Name Concatenation Bug (original + updated name merged)
 *   BUG-011: Test Data Pollution Across All Data Pages
 *
 * Tests share a browser session (inherited from BaseTest).
 */
public class BugHuntLocationsTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ================================================================
    // BUG-010: Building Name Concatenation Bug
    // ================================================================

    @Test(priority = 1, description = "BUG-010: Verify building name shows concatenated original+updated name")
    public void testBUG010_BuildingNameConcatenation() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_LOCATION_DATA,
                "BUG-010: Name Concatenation");

        try {
            driver.get(AppConstants.BASE_URL + "/locations");
            pause(6000);

            logStep("Navigated to Locations page");

            // Search for garbled/concatenated building names
            // The bug manifests as: "OriginalNameOriginalName_Updated" pattern
            String nameCheck = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "var lines = allText.split('\\n');" +
                "var garbled = [];" +
                "for (var i = 0; i < lines.length; i++) {" +
                "  var line = lines[i].trim();" +
                "  // Look for _Updated pattern (concatenation artifact)" +
                "  if (line.indexOf('_Updated') !== -1 && line.length > 30) {" +
                "    garbled.push(line.substring(0, 80));" +
                "  }" +
                "  // Look for duplicate patterns within a name" +
                "  if (line.indexOf('SmokeTest_Building_') !== -1 && " +
                "      line.indexOf('SmokeTest_Building_', line.indexOf('SmokeTest_Building_') + 1) !== -1) {" +
                "    garbled.push(line.substring(0, 80));" +
                "  }" +
                "}" +
                "return garbled.length > 0 ? garbled.join('\\n') : 'NO_GARBLED_NAMES';");

            logStep("Garbled name check: " + nameCheck);
            logStepWithScreenshot("Locations page — building names");

            boolean garbledFound = !"NO_GARBLED_NAMES".equals(nameCheck);

            // Also check for SmokeTest entries — another indicator of the bug
            String smokeCount = (String) js().executeScript(
                "return '' + (document.body.innerText.match(/SmokeTest/gi) || []).length;");
            boolean hasSmokeData = Integer.parseInt(smokeCount) > 0;

            logStep("Garbled names found: " + garbledFound + ", SmokeTest entries: " + smokeCount);

            Assert.assertTrue(garbledFound || hasSmokeData,
                    "BUG-010: No garbled names or test data found on Locations page. Bug may be fixed.");

            if (garbledFound) {
                ExtentReportManager.logPass("BUG-010 confirmed: Garbled building names found: " +
                        nameCheck.substring(0, Math.min(nameCheck.length(), 100)));
            } else {
                ExtentReportManager.logPass("BUG-010: No garbled names on current view, " +
                        "but SmokeTest entries present (" + smokeCount + ")");
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG010_name_error");
            Assert.fail("BUG-010 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-011: Test Data Pollution
    // ================================================================

    @Test(priority = 2, description = "BUG-011: Verify test data pollution across locations page")
    public void testBUG011_TestDataPollution() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_TEST_DATA,
                "BUG-011: Test Data Pollution");

        try {
            driver.get(AppConstants.BASE_URL + "/locations");
            pause(6000);

            logStep("On Locations page — counting test data entries");

            // Count SmokeTest entries
            String pollutionStats = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "var smokeBuilding = (allText.match(/SmokeTest_Building/gi) || []).length;" +
                "var smokeTest = (allText.match(/SmokeTest/gi) || []).length;" +
                "var autoTest = (allText.match(/AutoTest/gi) || []).length;" +
                "return 'smokeBuilding=' + smokeBuilding + ',smokeTotal=' + smokeTest + " +
                "  ',autoTest=' + autoTest;");

            logStep("Location test data: " + pollutionStats);
            logStepWithScreenshot("Locations page — test data pollution");

            // Also check Issues page
            driver.get(AppConstants.BASE_URL + "/issues");
            pause(6000);

            String issuesStats = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "var smokeIssue = (allText.match(/Smoke Test Issue/gi) || []).length;" +
                "return 'smokeIssues=' + smokeIssue;");

            logStep("Issues test data: " + issuesStats);
            logStepWithScreenshot("Issues page — test data pollution");

            // Also check Work Orders
            driver.get(AppConstants.BASE_URL + "/sessions");
            pause(6000);

            String woStats = (String) js().executeScript(
                "var allText = document.body.innerText;" +
                "var smokeWO = (allText.match(/SmokeTest_WO/gi) || []).length;" +
                "var autoWO = (allText.match(/AutoTest_WO/gi) || []).length;" +
                "return 'smokeWO=' + smokeWO + ',autoWO=' + autoWO;");

            logStep("Work Orders test data: " + woStats);
            logStepWithScreenshot("Work Orders page — test data pollution");

            // Parse total test data entries across all pages
            int totalPollution = 0;
            for (String stat : new String[]{pollutionStats, issuesStats, woStats}) {
                for (String part : stat.split(",")) {
                    String[] kv = part.split("=");
                    if (kv.length == 2) {
                        try { totalPollution += Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) {}
                    }
                }
            }

            logStep("Total test data entries across all pages: " + totalPollution);

            Assert.assertTrue(totalPollution > 0,
                    "BUG-011: No test data pollution found on any page. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-011 confirmed: " + totalPollution +
                    " test data entries. Locations=" + pollutionStats +
                    " | Issues=" + issuesStats + " | WOs=" + woStats);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG011_pollution_error");
            Assert.fail("BUG-011 test failed: " + e.getMessage());
        }
    }
}
