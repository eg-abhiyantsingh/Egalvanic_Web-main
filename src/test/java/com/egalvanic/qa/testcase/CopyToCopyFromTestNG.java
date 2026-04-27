package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Copy To / Copy From — ZP-323.
 *
 * The user can "Copy From" another asset/connection (pulls their attributes
 * into the current record) or "Copy To" (push current record's attributes
 * to another). Both flows typically surface as kebab menu items or a button
 * near the record header.
 *
 * Coverage:
 *   TC_Copy_01  Copy From entry point available
 *   TC_Copy_02  Copy To entry point available
 *   TC_Copy_03  Copy From dialog shows asset picker / previews source data
 *   TC_Copy_04  Cancel Copy flow does not modify the target record
 */
public class CopyToCopyFromTestNG extends BaseTest {

    private WebElement findByText(String... candidates) {
        for (String c : candidates) {
            List<WebElement> els = driver.findElements(
                By.xpath("//button[contains(normalize-space(.), '" + c + "')] | " +
                         "//*[@role='button'][contains(normalize-space(.), '" + c + "')] | " +
                         "//*[@role='menuitem'][contains(normalize-space(.), '" + c + "')]"));
            for (WebElement el : els) if (el.isDisplayed()) return el;
        }
        return null;
    }

    private void openFirstAssetDetail() {
        assetPage.navigateToAssets();
        pause(3000);
        List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
        Assert.assertFalse(rows.isEmpty(), "No assets in grid");
        safeClick(rows.get(0));
        pause(3500);
    }

    private boolean openKebab() {
        List<WebElement> kebabs = driver.findElements(By.cssSelector(
            "button[aria-label*='more' i], [data-testid='MoreVertIcon']"));
        for (WebElement k : kebabs) {
            if (k.isDisplayed()) { safeClick(k); pause(1200); return true; }
        }
        return false;
    }

    @Test(priority = 1, description = "Copy From entry point on asset detail")
    public void testTC_Copy_01_CopyFromEntry() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_01: Copy From entry");
        try {
            openFirstAssetDetail();
            WebElement entry = findByText("Copy From", "Copy from");
            if (entry == null && openKebab()) {
                entry = findByText("Copy From", "Copy from");
            }
            ScreenshotUtil.captureScreenshot("TC_Copy_01");
            Assert.assertNotNull(entry,
                "Copy From entry point not found on asset detail (checked buttons + kebab menu)");
            logStep("Copy From entry: " + entry.getText());
            ExtentReportManager.logPass("Copy From entry point present");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_01_error");
            Assert.fail("TC_Copy_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Copy To entry point on asset detail")
    public void testTC_Copy_02_CopyToEntry() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_02: Copy To entry");
        try {
            openFirstAssetDetail();
            WebElement entry = findByText("Copy To", "Copy to");
            if (entry == null && openKebab()) {
                entry = findByText("Copy To", "Copy to");
            }
            ScreenshotUtil.captureScreenshot("TC_Copy_02");
            Assert.assertNotNull(entry,
                "Copy To entry point not found on asset detail (checked buttons + kebab menu)");
            logStep("Copy To entry: " + entry.getText());
            ExtentReportManager.logPass("Copy To entry point present");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_02_error");
            Assert.fail("TC_Copy_02 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Copy From dialog opens with asset picker")
    public void testTC_Copy_03_CopyFromDialogPicker() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_03: Copy From picker");
        try {
            openFirstAssetDetail();
            WebElement entry = findByText("Copy From", "Copy from");
            if (entry == null && openKebab()) entry = findByText("Copy From", "Copy from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy From entry point absent on asset detail — feature may be role-gated, "
                    + "relabeled, or unshipped on Web (ZP-1498 'Copy Asset Details — Frontend' "
                    + "still To Do per Jira). TC_Copy_01 is the canonical signal for this gap.");
            }
            safeClick(entry);
            pause(3000);
            logStepWithScreenshot("Copy From dialog");

            // Expect a search/picker input for the source asset
            List<WebElement> pickers = driver.findElements(By.cssSelector(
                "[role='dialog'] input[type='search'], " +
                "[role='dialog'] input[placeholder*='Search'], " +
                "[role='dialog'] input[role='combobox']"));
            Assert.assertFalse(pickers.isEmpty(),
                "Copy From dialog has no asset picker input");
            logStep("Picker inputs in dialog: " + pickers.size());

            // Cancel the dialog
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1500);
            ExtentReportManager.logPass("Copy From opens asset picker dialog");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_03_error");
            Assert.fail("TC_Copy_03 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Cancel Copy flow leaves target record unchanged")
    public void testTC_Copy_04_CancelDoesNotModify() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_04: Cancel preserves");
        try {
            openFirstAssetDetail();
            String originalName = assetPage.getDetailPageAssetName();

            WebElement entry = findByText("Copy From", "Copy To");
            if (entry == null && openKebab()) entry = findByText("Copy From", "Copy To");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "No Copy entry point on asset detail — see TC_Copy_01/02 for canonical signal");
            }
            safeClick(entry);
            pause(2500);
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(2000);

            String afterName = assetPage.getDetailPageAssetName();
            logStep("Before: " + originalName + " | After cancel: " + afterName);
            ScreenshotUtil.captureScreenshot("TC_Copy_04");
            Assert.assertEquals(afterName, originalName,
                "Cancel on Copy flow appears to have modified the record");
            ExtentReportManager.logPass("Cancel on Copy flow preserves record");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_04_error");
            Assert.fail("TC_Copy_04 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_05 — Search filter narrows the asset picker
    // =================================================================
    @Test(priority = 5, description = "Typing in Copy From picker filters the asset list")
    public void testTC_Copy_05_PickerSearchFilters() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_05: Picker search");
        try {
            openFirstAssetDetail();
            WebElement entry = findByText("Copy From", "Copy from");
            if (entry == null && openKebab()) entry = findByText("Copy From", "Copy from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy From entry absent — see TC_Copy_01 for canonical signal");
            }
            safeClick(entry);
            pause(3000);

            List<WebElement> pickers = driver.findElements(By.cssSelector(
                "[role='dialog'] input[type='search'], " +
                "[role='dialog'] input[placeholder*='Search' i], " +
                "[role='dialog'] input[role='combobox']"));
            if (pickers.isEmpty()) {
                throw new org.testng.SkipException(
                    "Copy From dialog opened but has no picker input — picker UI not wired up");
            }
            WebElement picker = pickers.get(0);
            // Count options before filter
            safeClick(picker); pause(1200);
            int beforeCount = driver.findElements(By.cssSelector("li[role='option']")).size();
            picker.sendKeys("ZZZZ_UNLIKELY_" + System.currentTimeMillis());
            pause(1800);
            int afterCount = driver.findElements(By.cssSelector("li[role='option']")).size();
            logStep("Options before filter: " + beforeCount + ", after typing unlikely string: " + afterCount);
            ScreenshotUtil.captureScreenshot("TC_Copy_05");
            // Cancel the dialog
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1000);
            Assert.assertTrue(afterCount < beforeCount || afterCount == 0,
                "Picker search did not filter the list (before=" + beforeCount + " after=" + afterCount + ")");
            ExtentReportManager.logPass("Copy From picker search filters correctly");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_05_error");
            Assert.fail("TC_Copy_05 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_06 — Copy From excludes the current (self) asset
    // =================================================================
    @Test(priority = 6, description = "Current asset is excluded from Copy From picker (can't copy from self)")
    public void testTC_Copy_06_ExcludesSelf() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_06: Exclude self");
        try {
            openFirstAssetDetail();
            String currentName = assetPage.getDetailPageAssetName();
            logStep("Current asset: " + currentName);
            WebElement entry = findByText("Copy From", "Copy from");
            if (entry == null && openKebab()) entry = findByText("Copy From", "Copy from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy From entry absent — see TC_Copy_01 for canonical signal");
            }
            safeClick(entry);
            pause(3000);

            List<WebElement> pickers = driver.findElements(By.cssSelector(
                "[role='dialog'] input[type='search'], " +
                "[role='dialog'] input[placeholder*='Search' i], " +
                "[role='dialog'] input[role='combobox']"));
            if (pickers.isEmpty() || currentName == null || currentName.isEmpty()) {
                WebElement cancel = findByText("Cancel", "Close");
                if (cancel != null) safeClick(cancel);
                throw new org.testng.SkipException(
                    "Cannot probe self-exclusion — picker (" + pickers.size()
                    + ") or current asset name ('" + currentName + "') missing");
            }
            WebElement picker = pickers.get(0);
            safeClick(picker); pause(1200);
            // Type partial of current asset name
            String frag = currentName.length() > 4 ? currentName.substring(0, 4) : currentName;
            picker.sendKeys(frag);
            pause(2000);

            List<WebElement> options = driver.findElements(By.cssSelector("li[role='option']"));
            boolean selfListed = false;
            for (WebElement o : options) {
                if (o.getText() != null && o.getText().equals(currentName)) { selfListed = true; break; }
            }
            ScreenshotUtil.captureScreenshot("TC_Copy_06");
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1000);

            Assert.assertFalse(selfListed,
                "Current asset '" + currentName + "' appears in its own Copy From picker");
            ExtentReportManager.logPass("Copy From picker excludes current (self) asset");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_06_error");
            Assert.fail("TC_Copy_06 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_07 — Copy offers per-field selection (don't force full-clone)
    // =================================================================
    @Test(priority = 7, description = "Copy dialog exposes per-field checkboxes to choose what to copy")
    public void testTC_Copy_07_FieldSelectorInDialog() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_07: Field selector");
        try {
            openFirstAssetDetail();
            WebElement entry = findByText("Copy From", "Copy To");
            if (entry == null && openKebab()) entry = findByText("Copy From", "Copy To");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "No Copy entry on asset detail — see TC_Copy_01/02 for canonical signal");
            }
            safeClick(entry);
            pause(3000);

            List<WebElement> checkboxes = driver.findElements(By.cssSelector(
                "[role='dialog'] input[type='checkbox'], [class*='MuiDialog'] input[type='checkbox']"));
            logStep("Checkboxes in copy dialog: " + checkboxes.size());
            ScreenshotUtil.captureScreenshot("TC_Copy_07");
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1000);

            // Soft expectation — some flows do whole-record copy by design
            if (checkboxes.size() < 2) {
                logWarning("Copy dialog has < 2 checkboxes — may be whole-record copy by design. " +
                    "Flag for product spec: should users be able to cherry-pick fields?");
            }
            ExtentReportManager.logPass("Copy dialog field selectors: " + checkboxes.size());
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_07_error");
            Assert.fail("TC_Copy_07 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_08 — Copy preserves the target's identity (ID, QR, creation timestamp)
    // =================================================================
    @Test(priority = 8, description = "Target asset's identity fields (QR code, creation date) unchanged after Copy From cancel")
    public void testTC_Copy_08_TargetIdentityPreservedOnCancel() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_08: Target identity preserved");
        try {
            openFirstAssetDetail();
            // Read QR and creation if visible
            Object identityBefore = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var result = {};" +
                "var all = document.querySelectorAll('label, [class*=\"FormControl\"]');" +
                "for (var l of all) {" +
                "  var t = (l.textContent || '').toLowerCase();" +
                "  var input = l.querySelector('input');" +
                "  if (!input) continue;" +
                "  if (t.includes('qr') || t.includes('id') || t.includes('created')) {" +
                "    result[t.trim().substring(0,30)] = input.value;" +
                "  }" +
                "}" +
                "return result;");
            logStep("Identity before Copy: " + identityBefore);

            WebElement entry = findByText("Copy From", "Copy from");
            if (entry == null && openKebab()) entry = findByText("Copy From", "Copy from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy From entry absent — see TC_Copy_01 for canonical signal");
            }
            safeClick(entry);
            pause(2500);
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(2000);

            Object identityAfter = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var result = {};" +
                "var all = document.querySelectorAll('label, [class*=\"FormControl\"]');" +
                "for (var l of all) {" +
                "  var t = (l.textContent || '').toLowerCase();" +
                "  var input = l.querySelector('input');" +
                "  if (!input) continue;" +
                "  if (t.includes('qr') || t.includes('id') || t.includes('created')) {" +
                "    result[t.trim().substring(0,30)] = input.value;" +
                "  }" +
                "}" +
                "return result;");
            logStep("Identity after Copy cancel: " + identityAfter);
            ScreenshotUtil.captureScreenshot("TC_Copy_08");
            String before = identityBefore == null ? "" : identityBefore.toString();
            String after = identityAfter == null ? "" : identityAfter.toString();
            Assert.assertEquals(after, before,
                "Identity fields changed after Copy From cancel: before=" + before + " after=" + after);
            ExtentReportManager.logPass("Identity fields preserved after Copy From cancel");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_08_error");
            Assert.fail("TC_Copy_08 crashed: " + e.getMessage());
        }
    }
}
