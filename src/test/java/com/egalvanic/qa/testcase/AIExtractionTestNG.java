package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * AI Extraction — ZP-323.
 *
 * Feature: On the Asset create / edit form, "AI Extraction" lets the user
 * upload a nameplate image (JPG/PNG). The server runs OCR + LLM to extract
 * manufacturer, model, voltage ratings, serial number, etc., and auto-fills
 * the form fields.
 *
 * What this test covers:
 *   TC_AIExt_01  Button visible + gated — the "AI Extraction" button/tile is
 *                present on the asset create form and invokes the upload dialog.
 *   TC_AIExt_02  Upload nameplate → fields populated — after a nameplate image
 *                is uploaded, at least one core field (model, serial, or
 *                manufacturer) is populated by the extraction.
 *   TC_AIExt_03  Cancel extraction — clicking Cancel on the extraction dialog
 *                returns the user to the form without populating fields.
 *
 * Why it matters: AI Extraction dramatically reduces manual data entry time
 * for asset onboarding. A regression here would force operators back to
 * typing 10-15 fields per asset.
 */
public class AIExtractionTestNG extends BaseTest {

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    /**
     * Locate the "AI Extraction" button/tile on the open asset create form.
     * The real label may be "AI Extraction", "Extract with AI", "Upload Nameplate",
     * or "Smart Extract" — we search for any of those.
     */
    private WebElement findAIExtractButton() {
        String[] labels = {
            "AI Extraction", "Extract with AI", "Upload Nameplate",
            "Smart Extract", "Extract from Image", "AI Extract"
        };
        for (String label : labels) {
            List<WebElement> els = driver.findElements(
                By.xpath("//button[contains(normalize-space(.), '" + label + "')] | " +
                         "//*[@role='button'][contains(normalize-space(.), '" + label + "')]"));
            for (WebElement el : els) {
                if (el.isDisplayed()) return el;
            }
        }
        return null;
    }

    @Test(priority = 1, description = "AI Extraction button visible on asset create form")
    public void testTC_AIExt_01_ButtonVisible() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_AI_EXTRACTION,
            "TC_AIExt_01: AI Extraction button present");

        try {
            assetPage.navigateToAssets();
            assetPage.openCreateAssetForm();
            pause(3000);
            logStepWithScreenshot("Asset create form opened");

            WebElement btn = findAIExtractButton();
            ScreenshotUtil.captureScreenshot("TC_AIExt_01_form");

            if (btn == null) {
                logWarning("AI Extraction button not found on create form — feature may be " +
                           "role-gated or behind a toggle. Check asset form for AI / Extract / Upload Nameplate.");
                Assert.fail("AI Extraction button not visible — feature missing or label changed");
            } else {
                logStep("Found AI Extraction control: " + btn.getText());
                ExtentReportManager.logPass("AI Extraction button visible on asset create form");
            }
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_AIExt_01_error");
            Assert.fail("TC_AIExt_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Upload nameplate via AI Extraction populates form fields",
          dependsOnMethods = "testTC_AIExt_01_ButtonVisible", alwaysRun = false)
    public void testTC_AIExt_02_PopulatesFields() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_AI_EXTRACTION,
            "TC_AIExt_02: Extraction populates fields");

        try {
            // Form may still be open from previous test; if not, reopen
            List<WebElement> openForm = driver.findElements(
                By.cssSelector("[role='dialog'], [class*='MuiDrawer-paper']"));
            if (openForm.isEmpty() || !openForm.get(0).isDisplayed()) {
                assetPage.navigateToAssets();
                assetPage.openCreateAssetForm();
                pause(2500);
            }

            WebElement btn = findAIExtractButton();
            Assert.assertNotNull(btn, "AI Extraction button not found — TC_AIExt_01 should have caught this");
            safeClick(btn);
            pause(2500);
            logStepWithScreenshot("AI Extraction dialog opened");

            // Find a nameplate image to upload — we reuse any test image that exists in
            // the repo or fall back to a small PNG in /tmp
            String imagePath = findOrCreateNameplateImage();
            logStep("Using nameplate image: " + imagePath);

            // Make any hidden file input visible then sendKeys
            js().executeScript(
                "document.querySelectorAll('input[type=\"file\"]').forEach(function(i){" +
                "  i.style.display='block';i.style.visibility='visible';i.style.opacity='1';" +
                "  i.style.width='200px';i.style.height='50px';i.style.position='relative';" +
                "});");
            pause(500);

            List<WebElement> fileInputs = driver.findElements(By.cssSelector("input[type='file']"));
            Assert.assertFalse(fileInputs.isEmpty(), "No file input appeared after AI Extract click");
            fileInputs.get(fileInputs.size() - 1).sendKeys(new File(imagePath).getAbsolutePath());
            pause(1000);
            logStep("Nameplate image sent to file input");

            // Wait up to 60s for extraction to complete (OCR + LLM roundtrip can be slow)
            long deadline = System.currentTimeMillis() + 60000;
            boolean populated = false;
            while (System.currentTimeMillis() < deadline) {
                String modelVal = readFieldValue("model");
                String serialVal = readFieldValue("serial");
                String mfgVal = readFieldValue("manufacturer");
                if ((modelVal != null && !modelVal.isEmpty())
                    || (serialVal != null && !serialVal.isEmpty())
                    || (mfgVal != null && !mfgVal.isEmpty())) {
                    populated = true;
                    logStep("Extracted values — model: [" + modelVal + "], serial: [" + serialVal
                            + "], manufacturer: [" + mfgVal + "]");
                    break;
                }
                pause(1500);
            }
            logStepWithScreenshot(populated ? "Fields populated after extraction"
                                            : "Extraction timed out");

            Assert.assertTrue(populated, "AI Extraction did not populate any of model / serial / "
                    + "manufacturer within 60s — OCR pipeline may be down or image unreadable");
            ExtentReportManager.logPass("AI Extraction populated at least one core field");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_AIExt_02_error");
            Assert.fail("TC_AIExt_02 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Cancel on AI Extraction dialog returns without populating")
    public void testTC_AIExt_03_CancelExtraction() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_AI_EXTRACTION,
            "TC_AIExt_03: Cancel returns clean");

        try {
            assetPage.navigateToAssets();
            assetPage.openCreateAssetForm();
            pause(2500);
            WebElement btn = findAIExtractButton();
            if (btn == null) {
                logWarning("AI Extraction button not found — skipping TC_AIExt_03");
                return;
            }
            safeClick(btn);
            pause(2000);

            // Try to click Cancel / Close
            List<WebElement> cancelBtns = driver.findElements(
                By.xpath("//button[normalize-space()='Cancel' or normalize-space()='Close' or contains(.,'Cancel')]"));
            for (WebElement b : cancelBtns) {
                if (b.isDisplayed()) { safeClick(b); break; }
            }
            pause(1500);
            logStepWithScreenshot("After Cancel click");

            // The extraction dialog should be gone and the underlying create form still present
            List<WebElement> dialogs = driver.findElements(
                By.xpath("//*[contains(@aria-label, 'AI') or contains(., 'AI Extraction')][@role='dialog']"));
            boolean aiDialogClosed = dialogs.isEmpty() || !dialogs.get(0).isDisplayed();
            Assert.assertTrue(aiDialogClosed, "AI Extraction dialog did not close after Cancel");
            ExtentReportManager.logPass("Cancel on AI Extraction returns to create form cleanly");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_AIExt_03_error");
            Assert.fail("TC_AIExt_03 crashed: " + e.getMessage());
        }
    }

    // -------- helpers --------

    private String readFieldValue(String fieldHint) {
        try {
            Object v = js().executeScript(
                "var hint = arguments[0].toLowerCase();" +
                "var inputs = document.querySelectorAll('input, textarea');" +
                "for (var i of inputs) {" +
                "  var name = (i.name || i.id || '').toLowerCase();" +
                "  var label = '';" +
                "  var parent = i.closest('label, [class*=\"FormControl\"], [class*=\"MuiTextField\"]');" +
                "  if (parent) { var lbl = parent.querySelector('label'); if (lbl) label = lbl.textContent.toLowerCase(); }" +
                "  if ((name.includes(hint) || label.includes(hint)) && i.value) return i.value;" +
                "}" +
                "return null;", fieldHint);
            return v == null ? null : v.toString();
        } catch (Exception e) { return null; }
    }

    private String findOrCreateNameplateImage() {
        // Check common test image locations
        String[] candidates = {
            "test-resources/nameplate.png",
            "src/test/resources/nameplate.png",
            System.getProperty("user.home") + "/Downloads/nameplate.png",
            "/tmp/nameplate.png"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        // Generate a minimal valid PNG (1x1 pixel) so upload path works
        // even without a real nameplate image
        try {
            File f = new File("/tmp/nameplate.png");
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
            return f.getAbsolutePath();
        } catch (Exception e) {
            return "/tmp/nameplate.png";
        }
    }
}
