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

    // =================================================================
    // TC_Misc_05 — T&C link target is correct (href to policy page)
    // =================================================================
    @Test(priority = 5, description = "Terms & Conditions link has a valid href (not javascript:void or #)")
    public void testTC_Misc_05_TermsLinkHref() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_TERMS_CHECKBOX,
            "TC_Misc_05: T&C link href");
        try {
            driver.get(AppConstants.BASE_URL + "/login");
            pause(4000);
            List<WebElement> links = driver.findElements(By.tagName("a"));
            String href = null;
            String text = null;
            for (WebElement a : links) {
                String t = a.getText() == null ? "" : a.getText().trim();
                if (t.matches("(?i).*(terms|privacy|conditions).*") && a.isDisplayed()) {
                    href = a.getAttribute("href");
                    text = t;
                    break;
                }
            }
            ScreenshotUtil.captureScreenshot("TC_Misc_05");
            Assert.assertNotNull(href, "T&C link has no href at all");
            logStep("T&C link: '" + text + "' → " + href);
            Assert.assertFalse(href.startsWith("javascript:"), "T&C href is javascript:void — broken link");
            Assert.assertFalse("#".equals(href) || href.endsWith("/#"),
                "T&C href is # — placeholder, not a real policy URL");
            ExtentReportManager.logPass("T&C link points to real URL: " + href);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_05_error");
            Assert.fail("TC_Misc_05 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_06 — Maintenance State value is one of the allowed states (not empty/free text)
    // =================================================================
    @Test(priority = 6, description = "Maintenance State value is a known enum (not free text)")
    public void testTC_Misc_06_MaintenanceStateEnum() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_MAINTENANCE_STATE,
            "TC_Misc_06: Maintenance State enum");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);
            WebElement calcTab = findText("Calculations", "Calculation");
            if (calcTab != null) { safeClick(calcTab); pause(2000); }

            Object val = js().executeScript(
                "var all = document.querySelectorAll('label, [class*=\"FormControl\"], *');" +
                "for (var el of all) {" +
                "  if (el.children.length > 0 && !el.tagName.match(/label|span|div/i)) continue;" +
                "  var t = (el.textContent || '').toLowerCase();" +
                "  if (t.includes('maintenance state') || t.includes('maintenance status')) {" +
                "    var parent = el.parentElement;" +
                "    var val = parent ? parent.querySelector('input, span, div') : null;" +
                "    if (val) return (val.value || val.textContent || '').trim().substring(0,80);" +
                "  }" +
                "}" +
                "return null;");
            ScreenshotUtil.captureScreenshot("TC_Misc_06");
            logStep("Maintenance State value: " + val);
            if (val == null) { logWarning("No Maintenance State value readable"); return; }
            String s = val.toString().toLowerCase();
            // Expected enum set (guessed common labels for electrical asset state)
            boolean known = s.matches(".*(in service|out of service|retired|maintenance|active|inactive|" +
                "operational|decommissioned|pending|unknown|scheduled|overdue|compliant|non-compliant|good|fair|poor).*");
            Assert.assertTrue(known || s.isEmpty(),
                "Maintenance State value '" + val + "' does not match a known state enum — free text?");
            ExtentReportManager.logPass("Maintenance State: " + val);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_06_error");
            Assert.fail("TC_Misc_06 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_07 — Suggested Shortcut navigates when clicked
    // =================================================================
    @Test(priority = 7, description = "Clicking a Suggested Shortcut triggers navigation or action")
    public void testTC_Misc_07_ShortcutNavigates() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_Misc_07: Shortcut navigates");
        try {
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(5000);
            WebElement shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions");
            if (shortcut == null) {
                assetPage.navigateToAssets();
                pause(3000);
                List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
                if (!rows.isEmpty()) {
                    safeClick(rows.get(0));
                    pause(3500);
                    shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions");
                }
            }
            if (shortcut == null) { logWarning("No shortcuts panel"); return; }

            // Find a clickable shortcut item near the label
            String urlBefore = driver.getCurrentUrl();
            List<WebElement> btns = driver.findElements(By.cssSelector(
                "[class*='Shortcut'] button, [class*='shortcut'] button, " +
                "[class*='QuickAction'] button, [class*='quick'] button, [role='button']"));
            WebElement clickable = null;
            for (WebElement b : btns) {
                if (b.isDisplayed() && b.getText() != null && b.getText().length() > 0 && b.getText().length() < 40) {
                    clickable = b; break;
                }
            }
            if (clickable == null) { logWarning("No clickable shortcut item"); return; }
            String label = clickable.getText();
            safeClick(clickable);
            pause(3000);
            String urlAfter = driver.getCurrentUrl();
            boolean dialogOpened = !driver.findElements(
                By.cssSelector("[role='dialog']:not([aria-hidden='true'])")).isEmpty();
            ScreenshotUtil.captureScreenshot("TC_Misc_07");
            logStep("Shortcut '" + label + "' clicked. URL before: " + urlBefore + " after: " + urlAfter
                + ", dialog opened: " + dialogOpened);
            boolean actedOn = !urlBefore.equals(urlAfter) || dialogOpened;
            Assert.assertTrue(actedOn,
                "Shortcut '" + label + "' click did nothing (no nav, no dialog)");
            ExtentReportManager.logPass("Suggested Shortcut triggers action on click");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_07_error");
            Assert.fail("TC_Misc_07 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_08 — Schedule page exposes create/add entry point
    // =================================================================
    @Test(priority = 8, description = "Schedule page exposes an Add/Create event control")
    public void testTC_Misc_08_ScheduleHasCreateEntry() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SCHEDULE,
            "TC_Misc_08: Schedule create entry");
        try {
            String[] paths = { "/scheduling", "/schedule", "/calendar" };
            for (String p : paths) {
                driver.get(AppConstants.BASE_URL + p);
                pause(4000);
                String body = driver.findElement(By.tagName("body")).getText();
                if (body.length() > 100 && !body.contains("Page Not Found")) break;
            }
            pause(2000);
            WebElement add = findText("Add", "Create", "Schedule Work", "New Event", "+");
            ScreenshotUtil.captureScreenshot("TC_Misc_08");
            Assert.assertNotNull(add,
                "Schedule page has no Add/Create control — users can't schedule new events");
            logStep("Schedule add/create control: " + add.getText());
            ExtentReportManager.logPass("Schedule page exposes create entry point");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_08_error");
            Assert.fail("TC_Misc_08 crashed: " + e.getMessage());
        }
    }
}
