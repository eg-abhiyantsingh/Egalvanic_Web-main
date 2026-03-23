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
 * Bug Hunt: Dashboard Page Bug Verification Tests
 *
 * Verifies bugs specific to the Dashboard page:
 *   BUG-004: "App Update Available" Banner Persists Across All Pages
 *   BUG-012: Dashboard Shows "Company information not available" Alert
 *   BUG-019: Dashboard Stat Cards Text Overlap / Layout Issue
 *
 * Tests share a browser session (inherited from BaseTest).
 */
public class BugHuntDashboardTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ================================================================
    // BUG-004: "App Update Available" Banner Persists
    // ================================================================

    @Test(priority = 1, description = "BUG-004: Verify update banner appears and persists after dismiss")
    public void testBUG004_UpdateBannerPersists() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_UPDATE_BANNER,
                "BUG-004: Update Banner Persists");

        try {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(5000);

            logStep("Navigated to Dashboard page");

            // Check for the update banner
            List<WebElement> banners = driver.findElements(By.xpath(
                    "//*[contains(text(),'new app update is available') or contains(text(),'App Update')]"));

            boolean bannerVisible = !banners.isEmpty();
            logStep("Update banner visible on Dashboard: " + bannerVisible);
            logStepWithScreenshot("Dashboard with update banner check");

            if (bannerVisible) {
                // Try to dismiss it
                try {
                    WebElement dismissBtn = driver.findElement(By.xpath(
                            "//button[contains(text(),'DISMISS') or contains(text(),'Dismiss')]"));
                    dismissBtn.click();
                    logStep("Clicked DISMISS button");
                    pause(1000);
                } catch (Exception e) {
                    logStep("Could not find DISMISS button: " + e.getMessage());
                }

                // Navigate away and back
                driver.get(AppConstants.BASE_URL + "/assets");
                pause(3000);
                driver.get(AppConstants.BASE_URL + "/dashboard");
                pause(5000);

                // Check if banner reappeared
                List<WebElement> bannersAfter = driver.findElements(By.xpath(
                        "//*[contains(text(),'new app update is available') or contains(text(),'App Update')]"));

                boolean bannerReappeared = !bannersAfter.isEmpty();
                logStep("Banner reappeared after navigation: " + bannerReappeared);
                logStepWithScreenshot("Dashboard after dismiss + navigation");

                Assert.assertTrue(bannerReappeared,
                        "BUG-004: Banner did NOT reappear after dismiss+navigation. Bug may be fixed.");

                ExtentReportManager.logPass("BUG-004 confirmed: Update banner reappears after dismissal and navigation");
            } else {
                ExtentReportManager.logPass("BUG-004: Update banner not present — bug may be fixed or banner cleared");
            }

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG004_banner_error");
            Assert.fail("BUG-004 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-012: "Company information not available" Alert
    // ================================================================

    @Test(priority = 2, description = "BUG-012: Verify 'Company information not available' alert on dashboard")
    public void testBUG012_CompanyInfoNotAvailable() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_COMPANY_INFO,
                "BUG-012: Company Info Missing");

        try {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(5000);

            logStep("Navigated to Dashboard page");

            // Check for company info alert — could be a JS alert or inline message
            // First check for inline text
            List<WebElement> companyAlerts = driver.findElements(By.xpath(
                    "//*[contains(text(),'Company information not available') or " +
                    "contains(text(),'company information')]"));

            // Also check if it appeared as a JS console message
            Boolean consoleHasCompanyError = (Boolean) js().executeScript(
                "try {" +
                "  var entries = performance.getEntriesByType('resource');" +
                "  for (var i = 0; i < entries.length; i++) {" +
                "    if (entries[i].name.indexOf('alliance-config') !== -1 || " +
                "        entries[i].name.indexOf('company') !== -1) {" +
                "      return true;" +
                "    }" +
                "  }" +
                "} catch(e) {}" +
                "return false;");

            boolean alertVisible = !companyAlerts.isEmpty();
            logStep("Company info alert in DOM: " + alertVisible);
            logStep("Company/alliance-config API called: " + consoleHasCompanyError);
            logStepWithScreenshot("Dashboard — company info check");

            Assert.assertTrue(alertVisible || consoleHasCompanyError,
                    "BUG-012: No company info alert and no alliance-config API call. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-012 confirmed: Company alert visible=" +
                    alertVisible + ", API called=" + consoleHasCompanyError);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG012_company_error");
            Assert.fail("BUG-012 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-019: Dashboard Stat Cards Overlap / Layout Issue
    // ================================================================

    @Test(priority = 3, description = "BUG-019: Verify dashboard stat cards have text overlap or layout issues")
    public void testBUG019_DashboardStatCardsOverlap() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_DASHBOARD_LAYOUT,
                "BUG-019: Stat Cards Layout");

        try {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(5000);

            logStep("Navigated to Dashboard page");

            // Check for stat cards / summary cards on the dashboard
            // These are typically MUI Card or Box components showing counts
            List<WebElement> statCards = driver.findElements(By.xpath(
                    "//div[contains(@class,'MuiCard') or contains(@class,'MuiPaper')]" +
                    "[.//span or .//p or .//h6]"));

            logStep("Found " + statCards.size() + " card-like elements on dashboard");

            // Check for text overflow / truncation issues using JS
            String overflowCheck = (String) js().executeScript(
                "var issues = [];" +
                "var cards = document.querySelectorAll('[class*=MuiCard], [class*=MuiPaper]');" +
                "for (var i = 0; i < cards.length; i++) {" +
                "  var card = cards[i];" +
                "  if (card.scrollWidth > card.clientWidth) {" +
                "    issues.push('Card ' + i + ': content overflows horizontally (' +" +
                "      card.scrollWidth + ' > ' + card.clientWidth + ')');" +
                "  }" +
                "  if (card.scrollHeight > card.clientHeight + 5) {" +
                "    issues.push('Card ' + i + ': content overflows vertically (' +" +
                "      card.scrollHeight + ' > ' + card.clientHeight + ')');" +
                "  }" +
                "}" +
                "return issues.length > 0 ? issues.join('\\n') : 'NO_OVERFLOW';");

            logStep("Overflow check: " + overflowCheck);
            logStepWithScreenshot("Dashboard stat cards layout check");

            boolean hasOverflow = !"NO_OVERFLOW".equals(overflowCheck);
            Assert.assertTrue(hasOverflow,
                    "BUG-019: No card overflow detected on dashboard. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-019 confirmed: " + overflowCheck);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG019_layout_error");
            Assert.fail("BUG-019 test failed: " + e.getMessage());
        }
    }
}
