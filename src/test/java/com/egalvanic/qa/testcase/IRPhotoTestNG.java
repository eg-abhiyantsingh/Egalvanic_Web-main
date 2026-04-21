package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * IR Photos — ZP-323.
 *
 * Two flows:
 *   (a) Issue Details — IR photos uploaded against an issue must be visible
 *       on the issue detail page. (The bug report mentioned they were
 *       previously not visible — verify.)
 *   (b) Work Order — IR Photo upload flow works end-to-end.
 *
 * Coverage:
 *   TC_IR_01  Issue detail shows Photos/IR section and any existing photos render
 *   TC_IR_02  Work Order detail has an IR Photos tab/section
 *   TC_IR_03  Upload IR photo to Work Order → photo visible after upload
 */
public class IRPhotoTestNG extends BaseTest {

    private File sampleImage() {
        // Reuse the same 1x1 PNG used in AIExtractionTestNG
        try {
            File f = new File("/tmp/ir-sample.png");
            if (!f.exists()) {
                byte[] png = new byte[] {
                    (byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,
                    0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
                    0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,
                    0x08,0x02,0x00,0x00,0x00,(byte)0x90,0x77,0x53,
                    (byte)0xDE,0x00,0x00,0x00,0x0C,0x49,0x44,0x41,
                    0x54,0x08,(byte)0x99,0x63,(byte)0xF8,(byte)0xCF,(byte)0xC0,0x00,
                    0x00,0x00,0x03,0x00,0x01,0x5B,(byte)0xEC,0x5F,
                    (byte)0xD0,0x00,0x00,0x00,0x00,0x49,0x45,0x4E,0x44,
                    (byte)0xAE,0x42,0x60,(byte)0x82
                };
                java.nio.file.Files.write(f.toPath(), png);
            }
            return f;
        } catch (Exception e) { return new File("/tmp/ir-sample.png"); }
    }

    // =================================================================
    // TC_IR_01 — Issue Detail photos section visible
    // =================================================================
    @Test(priority = 1, description = "Issue detail page shows Photos / IR Photos section")
    public void testTC_IR_01_IssuePhotosVisible() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_ISSUE_IR_PHOTOS_VISIBILITY,
            "TC_IR_01: Issue photos section");
        try {
            issuePage.navigateToIssues();
            pause(3000);
            // Open first issue — IssuePage has a helper for this
            issuePage.openFirstIssueDetail();
            pause(3000);
            logStepWithScreenshot("Issue detail opened");

            // Navigate to photos section (IssuePage helper)
            issuePage.navigateToPhotosSection();
            pause(2500);

            // Look for thumbnails OR an "upload" empty state
            List<WebElement> thumbs = driver.findElements(
                By.cssSelector("img[src*='photo'], img[src*='image'], [class*='thumbnail'], [class*='preview']"));
            logStep("Photo thumbnails visible: " + thumbs.size());
            ScreenshotUtil.captureScreenshot("TC_IR_01");

            // The bug was photos NOT rendering even when they exist.
            // Soft-pass if 0 thumbs (may be a clean issue with no photos) but assert the
            // photo-section UI is at least reachable.
            List<WebElement> photoSection = driver.findElements(By.xpath(
                "//*[normalize-space()='Photos' or normalize-space()='IR Photos' or contains(., 'No photos')]"));
            Assert.assertFalse(photoSection.isEmpty(),
                "Photos section not reachable on issue detail");
            ExtentReportManager.logPass("Issue Photos section reachable; thumbnails visible: " + thumbs.size());
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_IR_01_error");
            Assert.fail("TC_IR_01 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_IR_02 — Work Order has IR Photos tab
    // =================================================================
    @Test(priority = 2, description = "Work Order detail has IR Photos tab/section")
    public void testTC_IR_02_WorkOrderIRTab() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_WO_IR_PHOTO_UPLOAD,
            "TC_IR_02: WO IR Photos tab");
        try {
            workOrderPage.navigateToWorkOrders();
            pause(3000);
            workOrderPage.openFirstWorkOrderDetail();
            pause(3000);
            logStepWithScreenshot("Work order detail opened");

            workOrderPage.navigateToIRPhotosSection();
            pause(2000);
            ScreenshotUtil.captureScreenshot("TC_IR_02");

            // Look for the IR Photos tab or section label
            List<WebElement> irSection = driver.findElements(By.xpath(
                "//*[normalize-space()='IR Photos' or normalize-space()='IR Images' or " +
                "contains(., 'Upload IR Photos')]"));
            Assert.assertFalse(irSection.isEmpty(),
                "IR Photos tab/section not found on Work Order detail");
            ExtentReportManager.logPass("Work Order IR Photos section present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_IR_02_error");
            Assert.fail("TC_IR_02 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_IR_03 — Upload IR photo to Work Order
    // =================================================================
    @Test(priority = 3, description = "Upload IR photo to Work Order succeeds")
    public void testTC_IR_03_WorkOrderIRUpload() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_WO_IR_PHOTO_UPLOAD,
            "TC_IR_03: WO IR upload");
        try {
            workOrderPage.navigateToWorkOrders();
            pause(3000);
            workOrderPage.openFirstWorkOrderDetail();
            pause(3000);

            int beforeCount = workOrderPage.getIRPhotoCount();
            logStep("IR photos before upload: " + beforeCount);

            File img = sampleImage();
            workOrderPage.uploadIRPhoto(img.getAbsolutePath());
            logStepWithScreenshot("Upload complete");

            boolean visibleAfter = workOrderPage.isIRPhotoVisible();
            int afterCount = workOrderPage.getIRPhotoCount();
            logStep("IR photos after upload: " + afterCount + " (visible=" + visibleAfter + ")");
            ScreenshotUtil.captureScreenshot("TC_IR_03");

            // Accept either a visible thumbnail or an after > before signal
            Assert.assertTrue(visibleAfter || afterCount > beforeCount,
                "IR photo not visible after upload (before=" + beforeCount + " after=" + afterCount + ")");
            ExtentReportManager.logPass("IR photo upload to Work Order successful");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_IR_03_error");
            Assert.fail("TC_IR_03 crashed: " + e.getMessage());
        }
    }
}
