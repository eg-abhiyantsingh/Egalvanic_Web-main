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
                logWarning("Copy From not available — skipping");
                return;
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
            if (entry == null) { logWarning("No copy entry — skipping"); return; }
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
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_04_error");
            Assert.fail("TC_Copy_04 crashed: " + e.getMessage());
        }
    }
}
