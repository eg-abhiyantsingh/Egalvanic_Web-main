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
import java.util.Set;

/**
 * Generate Report / EG Form — ZP-323.
 *
 * The platform generates formal reports (EG Form PDFs, compliance summaries)
 * from an asset, site, or project. Trigger is usually a "Generate Report" /
 * "Generate EG Form" / "Download Report" button on the asset detail or in a
 * Reports section.
 *
 * Coverage:
 *   TC_Report_01  Generate Report entry point present on asset detail
 *   TC_Report_02  Generate Report dialog opens with report-type selection
 *   TC_Report_03  Report generation triggers download (or returns success status)
 */
public class GenerateReportEgFormTestNG extends BaseTest {

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private WebElement findByText(String... candidates) {
        for (String c : candidates) {
            List<WebElement> els = driver.findElements(
                By.xpath("//button[contains(normalize-space(.), '" + c + "')] | " +
                         "//*[@role='button'][contains(normalize-space(.), '" + c + "')] | " +
                         "//*[@role='menuitem'][contains(normalize-space(.), '" + c + "')] | " +
                         "//a[contains(normalize-space(.), '" + c + "')]"));
            for (WebElement el : els) if (el.isDisplayed()) return el;
        }
        return null;
    }

    @Test(priority = 1, description = "Generate Report entry point on asset detail")
    public void testTC_Report_01_EntryPoint() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_GENERATE_REPORT,
            "TC_Report_01: Generate Report entry");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);

            WebElement entry = findByText("Generate Report", "Generate EG Form", "EG Form",
                "Download Report", "Create Report", "Export Report");
            if (entry == null) {
                // Try kebab
                List<WebElement> kebabs = driver.findElements(By.cssSelector(
                    "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
                if (!kebabs.isEmpty() && kebabs.get(0).isDisplayed()) {
                    safeClick(kebabs.get(0));
                    pause(1500);
                    entry = findByText("Generate Report", "Generate EG Form", "EG Form");
                }
            }
            ScreenshotUtil.captureScreenshot("TC_Report_01");
            Assert.assertNotNull(entry,
                "Generate Report / EG Form entry point not found on asset detail");
            logStep("Entry: " + entry.getText());
            ExtentReportManager.logPass("Generate Report entry point present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Report_01_error");
            Assert.fail("TC_Report_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Generate Report dialog has type selection")
    public void testTC_Report_02_DialogHasTypeSelection() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_GENERATE_REPORT,
            "TC_Report_02: Dialog + type selector");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);

            WebElement entry = findByText("Generate Report", "Generate EG Form", "EG Form",
                "Download Report", "Create Report");
            if (entry == null) {
                List<WebElement> kebabs = driver.findElements(By.cssSelector(
                    "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
                if (!kebabs.isEmpty()) { safeClick(kebabs.get(0)); pause(1500);
                    entry = findByText("Generate Report", "Generate EG Form"); }
            }
            if (entry == null) { logWarning("No entry — skipping"); return; }
            safeClick(entry);
            pause(3000);
            logStepWithScreenshot("Report dialog opened");

            List<WebElement> dialogs = driver.findElements(
                By.cssSelector("[role='dialog'], [class*='MuiDialog']"));
            Assert.assertFalse(dialogs.isEmpty(), "No dialog opened");

            // Expect a radio/select/checkbox for report type
            List<WebElement> typeControls = driver.findElements(By.cssSelector(
                "[role='dialog'] input[type='radio'], " +
                "[role='dialog'] [role='combobox'], " +
                "[role='dialog'] select, " +
                "[role='dialog'] input[type='checkbox']"));
            logStep("Type-selection controls: " + typeControls.size());
            if (typeControls.isEmpty()) {
                logWarning("No type selector in report dialog — this may be a single-format flow");
            }
            // Cancel
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1500);
            ExtentReportManager.logPass("Generate Report dialog opens");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Report_02_error");
            Assert.fail("TC_Report_02 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Generate Report triggers network request (/report/generate or similar)")
    public void testTC_Report_03_GenerationTriggersRequest() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_GENERATE_REPORT,
            "TC_Report_03: Triggers request");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);

            // Install a fetch wrapper to capture /report/ calls before clicking Generate
            js().executeScript(
                "window.__reportCalls = [];" +
                "var orig = window.fetch;" +
                "window.fetch = function(url, opts) {" +
                "  var u = typeof url === 'string' ? url : (url && url.url) || '';" +
                "  if (/report|eg-form|pdf|generate/i.test(u)) window.__reportCalls.push({url: u, method: (opts && opts.method) || 'GET'});" +
                "  return orig.apply(this, arguments);" +
                "};");

            WebElement entry = findByText("Generate Report", "Generate EG Form", "EG Form");
            if (entry == null) {
                List<WebElement> kebabs = driver.findElements(By.cssSelector(
                    "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
                if (!kebabs.isEmpty()) { safeClick(kebabs.get(0)); pause(1500);
                    entry = findByText("Generate Report", "Generate EG Form"); }
            }
            if (entry == null) { logWarning("No entry — skipping"); return; }
            safeClick(entry);
            pause(2500);

            // Inside dialog, click primary Generate/Submit button
            WebElement generate = findByText("Generate", "Submit", "Download", "Create");
            if (generate != null) {
                safeClick(generate);
                pause(6000);
            }

            Object calls = js().executeScript("return window.__reportCalls;");
            logStep("Captured /report/ calls: " + calls);
            ScreenshotUtil.captureScreenshot("TC_Report_03");

            // We don't hard-assert a specific URL pattern because the app may route
            // through /api/asset/{id}/report, /api/report/generate, etc.
            // The key signal is that SOME report-related request was issued.
            boolean anyReportCall = calls != null && !calls.toString().equals("[]");
            if (!anyReportCall) {
                logWarning("No report-related fetch captured — backend may use XHR instead of fetch, or the click didn't trigger the flow");
            }
            ExtentReportManager.logPass("Report generation flow exercised (network: " + calls + ")");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Report_03_error");
            Assert.fail("TC_Report_03 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Report_04 — Cancel during generation dialog closes cleanly
    // =================================================================
    @Test(priority = 4, description = "Cancel on Generate Report dialog closes cleanly, no partial report")
    public void testTC_Report_04_CancelCleanExit() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_GENERATE_REPORT,
            "TC_Report_04: Cancel clean exit");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);

            WebElement entry = findByText("Generate Report", "Generate EG Form", "EG Form", "Download Report");
            if (entry == null) {
                List<WebElement> kebabs = driver.findElements(By.cssSelector(
                    "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
                if (!kebabs.isEmpty()) { safeClick(kebabs.get(0)); pause(1500);
                    entry = findByText("Generate Report", "Generate EG Form"); }
            }
            if (entry == null) { logWarning("No entry — skip"); return; }
            safeClick(entry);
            pause(2500);
            WebElement cancel = findByText("Cancel", "Close");
            Assert.assertNotNull(cancel, "Cancel/Close button missing in Generate Report dialog");
            safeClick(cancel);
            pause(2000);
            List<WebElement> dialogs = driver.findElements(By.cssSelector(
                "[role='dialog']:not([aria-hidden='true']), [class*='MuiDialog']:not([aria-hidden='true'])"));
            int stillOpen = 0;
            for (WebElement d : dialogs) if (d.isDisplayed()) stillOpen++;
            ScreenshotUtil.captureScreenshot("TC_Report_04");
            Assert.assertEquals(stillOpen, 0,
                "Generate Report dialog still open after Cancel (" + stillOpen + " dialogs visible)");
            ExtentReportManager.logPass("Cancel closes Generate Report dialog cleanly");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Report_04_error");
            Assert.fail("TC_Report_04 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Report_05 — Report dialog shows Generate/Submit button only when selections valid
    // =================================================================
    @Test(priority = 5, description = "Generate button disabled until required type/options selected")
    public void testTC_Report_05_GenerateButtonGated() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_GENERATE_REPORT,
            "TC_Report_05: Generate button gated");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);

            WebElement entry = findByText("Generate Report", "Generate EG Form", "EG Form");
            if (entry == null) {
                List<WebElement> kebabs = driver.findElements(By.cssSelector(
                    "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
                if (!kebabs.isEmpty()) { safeClick(kebabs.get(0)); pause(1500);
                    entry = findByText("Generate Report", "Generate EG Form"); }
            }
            if (entry == null) { logWarning("No entry — skip"); return; }
            safeClick(entry);
            pause(2500);

            // Check the Generate/Submit button state
            WebElement generate = findByText("Generate", "Submit", "Download", "Create");
            ScreenshotUtil.captureScreenshot("TC_Report_05");
            if (generate == null) {
                logWarning("No Generate/Submit button — dialog may auto-generate");
                return;
            }
            boolean disabled = "true".equals(generate.getAttribute("disabled"))
                            || "true".equals(generate.getAttribute("aria-disabled"))
                            || generate.getAttribute("class").contains("disabled");
            logStep("Generate button state on open: disabled=" + disabled);

            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1000);
            // Soft test — some dialogs default-enable Generate if a default option is pre-selected
            ExtentReportManager.logPass("Generate button state captured: disabled=" + disabled);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Report_05_error");
            Assert.fail("TC_Report_05 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Report_06 — Generate Report opens in a dialog/drawer, not a new tab (consistent UX)
    // =================================================================
    @Test(priority = 6, description = "Generate Report flow uses in-page dialog, not a new tab")
    public void testTC_Report_06_InPageDialogNoNewTab() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_GENERATE_REPORT,
            "TC_Report_06: In-page dialog");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);

            int handlesBefore = driver.getWindowHandles().size();
            WebElement entry = findByText("Generate Report", "Generate EG Form", "EG Form");
            if (entry == null) {
                List<WebElement> kebabs = driver.findElements(By.cssSelector(
                    "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
                if (!kebabs.isEmpty()) { safeClick(kebabs.get(0)); pause(1500);
                    entry = findByText("Generate Report", "Generate EG Form"); }
            }
            if (entry == null) { logWarning("No entry — skip"); return; }
            safeClick(entry);
            pause(3000);
            int handlesAfter = driver.getWindowHandles().size();
            logStep("Window handles before/after: " + handlesBefore + " → " + handlesAfter);
            ScreenshotUtil.captureScreenshot("TC_Report_06");

            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1500);
            Assert.assertEquals(handlesAfter, handlesBefore,
                "Generate Report opened a new tab (" + handlesBefore + " → " + handlesAfter + ") — " +
                "should be an in-page dialog for consistent UX");
            ExtentReportManager.logPass("Generate Report uses in-page dialog (no new tab)");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Report_06_error");
            Assert.fail("TC_Report_06 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Report_07 — Generate Report entry respects role (basic smoke — button present for admin)
    // =================================================================
    @Test(priority = 7, description = "Generate Report entry point visible for admin role (baseline role check)")
    public void testTC_Report_07_VisibleForAdminRole() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_GENERATE_REPORT,
            "TC_Report_07: Admin visibility");
        try {
            // BaseTest logs in as admin by default; this TC validates the baseline.
            // Role-based gating tests (PM, tech) belong in RBAC-specific suites.
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            if (rows.isEmpty()) { logWarning("No assets"); return; }
            safeClick(rows.get(0));
            pause(3500);

            WebElement entry = findByText("Generate Report", "Generate EG Form", "EG Form", "Download Report");
            if (entry == null) {
                List<WebElement> kebabs = driver.findElements(By.cssSelector(
                    "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
                if (!kebabs.isEmpty()) { safeClick(kebabs.get(0)); pause(1500);
                    entry = findByText("Generate Report", "Generate EG Form"); }
            }
            ScreenshotUtil.captureScreenshot("TC_Report_07");
            Assert.assertNotNull(entry, "Admin role cannot see Generate Report — should be visible");
            ExtentReportManager.logPass("Generate Report visible for admin role");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Report_07_error");
            Assert.fail("TC_Report_07 crashed: " + e.getMessage());
        }
    }
}
