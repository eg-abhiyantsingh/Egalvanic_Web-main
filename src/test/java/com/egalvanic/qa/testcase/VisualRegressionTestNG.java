package com.egalvanic.qa.testcase;

import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.FlakinessPrevention;
import com.egalvanic.qa.utils.ai.VisualRegressionUtil;
import com.egalvanic.qa.utils.ai.VisualRegressionUtil.ComparisonResult;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * Visual Regression Test Suite
 *
 * Manual testers catch UI bugs instantly — misalignment, missing elements,
 * broken layouts, style regressions. This suite brings that capability to CI.
 *
 * Visits each major page, takes a screenshot, and compares it against
 * a stored baseline. Differences beyond the threshold are flagged.
 *
 * First run: Use -Dvisual.updateBaselines=true to generate baselines.
 * Subsequent runs: Compares against baselines, fails on visual regressions.
 *
 * Extends BaseTest for standard login/site-selection/browser lifecycle.
 */
public class VisualRegressionTestNG extends BaseTest {

    private static final String MODULE = "Visual Regression";
    private static final String FEATURE = "Baseline Comparison";

    private void navigateAndWait(String url, String pageName) {
        driver.get(url);
        pause(2000);
        FlakinessPrevention.waitForPageReady(driver);
        dismissBackdrops();
        pause(1000);
    }

    private void checkVisual(String pageName) {
        ComparisonResult result = VisualRegressionUtil.compareWithBaseline(driver, pageName);
        VisualRegressionUtil.logToExtentReport(result);
        Assert.assertTrue(result.passed(), result.summary());
    }

    // ================================================================
    // PAGE VISUAL TESTS
    // ================================================================

    @Test(priority = 1, description = "Visual: Dashboard page")
    public void testDashboardVisual() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Dashboard Visual");
        navigateAndWait(appUrl("/dashboard"), "dashboard");
        checkVisual("dashboard");
        ExtentReportManager.logPass("Dashboard visual check complete");
    }

    @Test(priority = 2, description = "Visual: Assets list page")
    public void testAssetsVisual() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Assets Visual");
        navigateViaSidebar("Assets");
        pause(2000);
        FlakinessPrevention.waitForPageReady(driver);
        dismissBackdrops();
        checkVisual("assets");
        ExtentReportManager.logPass("Assets visual check complete");
    }

    @Test(priority = 3, description = "Visual: Locations page")
    public void testLocationsVisual() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Locations Visual");
        navigateViaSidebar("Locations");
        pause(2000);
        FlakinessPrevention.waitForPageReady(driver);
        dismissBackdrops();
        checkVisual("locations");
        ExtentReportManager.logPass("Locations visual check complete");
    }

    @Test(priority = 4, description = "Visual: Connections page")
    public void testConnectionsVisual() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Connections Visual");
        navigateViaSidebar("Connections");
        pause(2000);
        FlakinessPrevention.waitForPageReady(driver);
        dismissBackdrops();
        checkVisual("connections");
        ExtentReportManager.logPass("Connections visual check complete");
    }

    @Test(priority = 5, description = "Visual: Issues page")
    public void testIssuesVisual() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Issues Visual");
        navigateViaSidebar("Issues");
        pause(2000);
        FlakinessPrevention.waitForPageReady(driver);
        dismissBackdrops();
        checkVisual("issues");
        ExtentReportManager.logPass("Issues visual check complete");
    }

    @Test(priority = 6, description = "Visual: Work Orders page")
    public void testWorkOrdersVisual() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Work Orders Visual");
        navigateViaSidebar("Jobs");
        pause(2000);
        FlakinessPrevention.waitForPageReady(driver);
        dismissBackdrops();
        checkVisual("workorders");
        ExtentReportManager.logPass("Work Orders visual check complete");
    }

    @Test(priority = 7, description = "Visual: Tasks page")
    public void testTasksVisual() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Tasks Visual");
        navigateViaSidebar("Tasks");
        pause(2000);
        FlakinessPrevention.waitForPageReady(driver);
        dismissBackdrops();
        checkVisual("tasks");
        ExtentReportManager.logPass("Tasks visual check complete");
    }

    // ================================================================
    // CLEANUP
    // ================================================================

    @AfterClass
    @Override
    public void classTeardown() {
        VisualRegressionUtil.printSummary();
        super.classTeardown();
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private String appUrl(String path) {
        return com.egalvanic.qa.constants.AppConstants.BASE_URL + path;
    }

    private void navigateViaSidebar(String linkText) {
        try {
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            js.executeScript(
                "var items = document.querySelectorAll('span, a');" +
                "for (var i = 0; i < items.length; i++) {" +
                "  if (items[i].textContent.trim() === '" + linkText + "' && items[i].offsetWidth > 0) {" +
                "    items[i].click(); return;" +
                "  }" +
                "}");
        } catch (Exception e) {
            driver.get(appUrl("/" + linkText.toLowerCase()));
        }
    }
}
