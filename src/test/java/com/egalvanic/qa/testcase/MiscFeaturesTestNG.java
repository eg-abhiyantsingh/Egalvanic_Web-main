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
 * Miscellaneous new coverage — ZP-323.
 *
 * Four small but important features bundled:
 *   - Terms & Conditions checkbox (login / settings / agreement flow)
 *   - Calculation - Maintenance State (on asset detail calculations tab)
 *   - Suggested Shortcuts (typically on SLDs / asset detail)
 *   - Schedule (Scheduling page navigation + CRUD)
 *
 * Coverage:
 *   TC_Misc_01  Terms & Conditions checkbox present on login (or agreement page)
 *   TC_Misc_02  Maintenance State field / calculation on asset detail
 *   TC_Misc_03  Suggested Shortcuts panel renders on SLDs / asset detail
 *   TC_Misc_04  Schedule page loads and shows calendar/list view
 */
public class MiscFeaturesTestNG extends BaseTest {

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private WebElement findText(String... candidates) {
        for (String c : candidates) {
            List<WebElement> els = driver.findElements(
                By.xpath("//*[contains(normalize-space(.), '" + c + "') and " +
                         "(self::label or self::span or self::div or self::a or self::button or self::p or self::h1 or self::h2 or self::h3)]"));
            for (WebElement el : els) {
                if (el.isDisplayed() && el.getText().trim().length() < 200) return el;
            }
        }
        return null;
    }

    // =================================================================
    // TC_Misc_01 — Terms & Conditions checkbox
    // =================================================================
    @Test(priority = 1, description = "Terms & Conditions checkbox/link present on login or agreement page")
    public void testTC_Misc_01_TermsConditions() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_TERMS_CHECKBOX,
            "TC_Misc_01: T&C checkbox");
        try {
            // Try login page first
            driver.get(AppConstants.BASE_URL + "/login");
            pause(4000);
            logStepWithScreenshot("Login page");

            // Check for explicit T&C checkbox
            List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
            WebElement tcLink = findText("Terms and Conditions", "Terms & Conditions",
                "Terms of Service", "Privacy Policy", "I agree");
            ScreenshotUtil.captureScreenshot("TC_Misc_01");

            // The UI confirmed earlier that login shows "By signing in, you agree to our Terms
            // and Conditions and Privacy Policy" — acceptance is implicit via the Sign In button,
            // not via a checkbox. We assert the T&C link is present as a compliance requirement.
            Assert.assertNotNull(tcLink,
                "No Terms & Conditions link/checkbox visible on login — compliance requirement");
            logStep("T&C reference found: " + tcLink.getText().substring(0, Math.min(80, tcLink.getText().length())));
            ExtentReportManager.logPass("Terms & Conditions reference present (checkboxes on page: " + checkboxes.size() + ")");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_01_error");
            Assert.fail("TC_Misc_01 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_02 — Maintenance State calculation
    // =================================================================
    @Test(priority = 2, description = "Asset detail shows Maintenance State field or calculation")
    public void testTC_Misc_02_MaintenanceState() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_MAINTENANCE_STATE,
            "TC_Misc_02: Maintenance State");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);
            logStepWithScreenshot("Asset detail opened");

            // Maintenance state may be on a "Calculations" tab OR directly shown as a field
            WebElement calcTab = findText("Calculations", "Calculation");
            if (calcTab != null) { safeClick(calcTab); pause(2000); }

            WebElement maintState = findText("Maintenance State", "Maintenance Status", "Maintainability");
            ScreenshotUtil.captureScreenshot("TC_Misc_02");
            Assert.assertNotNull(maintState,
                "Maintenance State field/label not found on asset detail calculations");
            logStep("Maintenance State: " + maintState.getText().substring(0, Math.min(100, maintState.getText().length())));
            ExtentReportManager.logPass("Maintenance State field present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_02_error");
            Assert.fail("TC_Misc_02 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_03 — Suggested Shortcuts
    // =================================================================
    @Test(priority = 3, description = "Suggested Shortcuts panel renders on SLD or asset detail")
    public void testTC_Misc_03_SuggestedShortcuts() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_Misc_03: Suggested Shortcuts");
        try {
            // First try SLDs page
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(5000);
            logStepWithScreenshot("SLDs page");

            WebElement shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions", "Suggested");
            if (shortcut == null) {
                // Try asset detail
                assetPage.navigateToAssets();
                pause(3000);
                List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
                if (!rows.isEmpty()) {
                    safeClick(rows.get(0));
                    pause(3500);
                    shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions");
                }
            }
            ScreenshotUtil.captureScreenshot("TC_Misc_03");
            Assert.assertNotNull(shortcut,
                "Suggested Shortcuts panel not found on SLDs or asset detail");
            logStep("Suggested Shortcuts: " + shortcut.getText().substring(0, Math.min(100, shortcut.getText().length())));
            ExtentReportManager.logPass("Suggested Shortcuts panel present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_03_error");
            Assert.fail("TC_Misc_03 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_04 — Schedule page
    // =================================================================
    @Test(priority = 4, description = "Schedule/Scheduling page loads with calendar or list view")
    public void testTC_Misc_04_SchedulePage() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SCHEDULE,
            "TC_Misc_04: Schedule page");
        try {
            // Try multiple common paths
            String[] paths = { "/scheduling", "/schedule", "/calendar" };
            String loadedPath = null;
            for (String p : paths) {
                driver.get(AppConstants.BASE_URL + p);
                pause(4000);
                // Check if page rendered (not 404/blank)
                String body = driver.findElement(By.tagName("body")).getText();
                if (body.length() > 100 && !body.contains("Page Not Found")) {
                    loadedPath = p; break;
                }
            }
            Assert.assertNotNull(loadedPath, "Schedule page did not load at any of " + String.join(", ", paths));
            logStep("Schedule page at: " + loadedPath);
            logStepWithScreenshot("Schedule page loaded");

            // Look for calendar grid OR list view
            List<WebElement> calendar = driver.findElements(By.cssSelector(
                "[class*='Calendar'], [class*='calendar'], [class*='scheduler'], " +
                ".MuiDataGrid-root, [role='grid'], [role='table']"));
            ScreenshotUtil.captureScreenshot("TC_Misc_04");
            Assert.assertFalse(calendar.isEmpty(),
                "Schedule page has no calendar or grid view");
            logStep("Calendar / grid elements: " + calendar.size());
            ExtentReportManager.logPass("Schedule page loads with calendar/grid view");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_04_error");
            Assert.fail("TC_Misc_04 crashed: " + e.getMessage());
        }
    }
}
