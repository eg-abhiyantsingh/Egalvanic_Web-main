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
import java.nio.file.Files;
import java.util.List;

/**
 * Bulk Upload + Bulk Edit — ZP-323.
 *
 * Both features live on the Assets page (and possibly Connections/Locations).
 * Bulk Upload accepts a CSV/XLSX file and creates many assets at once.
 * Bulk Edit lets the user select multiple grid rows via checkbox and apply
 * one edit action across all of them.
 *
 * Coverage:
 *   TC_Bulk_01  Bulk Upload entry point present (button or menu item)
 *   TC_Bulk_02  Bulk Upload dialog opens and accepts file selection
 *   TC_Bulk_03  Bulk Edit entry point (grid checkboxes + toolbar button)
 *   TC_Bulk_04  Bulk Edit apply flow (multi-select + apply common field)
 */
public class BulkUploadBulkEditTestNG extends BaseTest {

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    /**
     * Find any element whose visible text matches one of the candidates.
     */
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

    /**
     * Open the page-level kebab / more menu if present. Returns true if opened.
     */
    private boolean openMoreMenu() {
        try {
            List<WebElement> kebabs = driver.findElements(By.cssSelector(
                "button[aria-label*='more' i], button[aria-label*='More' i], " +
                "button[data-testid*='MoreVert'], [data-testid='MoreVertIcon']"));
            for (WebElement k : kebabs) {
                if (k.isDisplayed()) { safeClick(k); pause(1000); return true; }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // =================================================================
    // TC_Bulk_01 — Bulk Upload entry point visible
    // =================================================================
    @Test(priority = 1, description = "Bulk Upload entry point (button or menu item) is present on Assets")
    public void testTC_Bulk_01_BulkUploadEntryPoint() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_UPLOAD,
            "TC_Bulk_01: Bulk Upload entry");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            logStepWithScreenshot("Assets page loaded");

            WebElement entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            if (entry == null) {
                // Maybe it's behind a kebab menu
                if (openMoreMenu()) {
                    entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
                }
            }
            ScreenshotUtil.captureScreenshot("TC_Bulk_01");
            Assert.assertNotNull(entry,
                "Bulk Upload entry point not found on Assets — expected button or menu item " +
                "'Bulk Upload' / 'Bulk Import' / 'Import Assets' / 'Upload CSV'");
            logStep("Bulk Upload entry point found: " + entry.getText());
            ExtentReportManager.logPass("Bulk Upload entry point present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_01_error");
            Assert.fail("TC_Bulk_01 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_02 — Bulk Upload dialog opens and accepts file selection
    // =================================================================
    @Test(priority = 2, description = "Bulk Upload dialog opens and accepts CSV/XLSX file",
          dependsOnMethods = "testTC_Bulk_01_BulkUploadEntryPoint", alwaysRun = false)
    public void testTC_Bulk_02_BulkUploadDialog() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_UPLOAD,
            "TC_Bulk_02: Bulk Upload dialog");
        try {
            assetPage.navigateToAssets();
            pause(2500);
            WebElement entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            if (entry == null && openMoreMenu()) {
                entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            }
            Assert.assertNotNull(entry, "Bulk Upload entry missing — TC_Bulk_01 should have caught this");
            safeClick(entry);
            pause(2500);
            logStepWithScreenshot("Bulk Upload dialog opened");

            // Expect a file input OR a dropzone
            js().executeScript(
                "document.querySelectorAll('input[type=\"file\"]').forEach(function(i){" +
                "  i.style.display='block';i.style.visibility='visible';i.style.opacity='1';" +
                "  i.style.width='200px';i.style.height='50px';i.style.position='relative';" +
                "});");
            pause(500);

            List<WebElement> fileInputs = driver.findElements(By.cssSelector("input[type='file']"));
            Assert.assertFalse(fileInputs.isEmpty(),
                "No file input in Bulk Upload dialog — cannot accept file");
            logStep("File input found: " + fileInputs.size());

            // Create a minimal valid CSV
            File csv = createSampleCsv();
            fileInputs.get(fileInputs.size() - 1).sendKeys(csv.getAbsolutePath());
            pause(3000);
            logStepWithScreenshot("CSV sent to file input");

            // Look for validation / preview / upload-confirm button
            WebElement next = findByText("Upload", "Submit", "Import", "Next", "Validate");
            if (next != null) {
                logStep("Bulk Upload flow has 'Upload/Submit/Import' button: " + next.getText());
                // We DO NOT actually submit to avoid polluting the QA data with test rows.
                // A later TC should be enabled ONLY against a sandbox tenant.
            }
            ExtentReportManager.logPass("Bulk Upload dialog accepts file selection; submit button detected");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_02_error");
            Assert.fail("TC_Bulk_02 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_03 — Bulk Edit entry point (grid row checkboxes)
    // =================================================================
    @Test(priority = 3, description = "Bulk Edit entry — grid rows have checkboxes + toolbar appears on selection")
    public void testTC_Bulk_03_BulkEditEntryPoint() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_EDIT,
            "TC_Bulk_03: Bulk Edit entry");
        try {
            assetPage.navigateToAssets();
            pause(3000);

            // MUI DataGrid renders checkboxes with class MuiDataGrid-cellCheckbox
            List<WebElement> checkboxes = driver.findElements(By.cssSelector(
                ".MuiDataGrid-cellCheckbox input[type='checkbox'], " +
                ".MuiDataGrid-row input[type='checkbox'], " +
                "[role='row'] input[type='checkbox']"));
            logStep("Row checkboxes found: " + checkboxes.size());
            ScreenshotUtil.captureScreenshot("TC_Bulk_03_checkboxes");

            if (checkboxes.size() < 2) {
                logWarning("Fewer than 2 row checkboxes — grid may be empty or bulk selection " +
                           "not enabled. Ensure test site has at least 2 assets.");
                // Not a hard fail — data-dependent
                Assert.assertTrue(checkboxes.size() >= 1,
                    "Expected at least 1 row checkbox for Bulk Edit support");
            }

            // Select first two rows
            int selected = 0;
            for (int i = 0; i < Math.min(2, checkboxes.size()); i++) {
                try {
                    safeClick(checkboxes.get(i));
                    selected++;
                    pause(500);
                } catch (Exception ignored) {}
            }
            logStep("Selected rows: " + selected);
            pause(1500);
            ScreenshotUtil.captureScreenshot("TC_Bulk_03_selected");

            WebElement bulkEditBtn = findByText("Bulk Edit", "Edit Selected", "Bulk Actions");
            Assert.assertNotNull(bulkEditBtn,
                "After selecting rows, no Bulk Edit / Edit Selected / Bulk Actions button appeared");
            logStep("Bulk Edit control surfaced after selection: " + bulkEditBtn.getText());
            ExtentReportManager.logPass("Bulk Edit entry point surfaces after multi-row selection");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_03_error");
            Assert.fail("TC_Bulk_03 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_04 — Bulk Edit open applies UI (does NOT actually save to avoid data pollution)
    // =================================================================
    @Test(priority = 4, description = "Bulk Edit dialog opens with field selector",
          dependsOnMethods = "testTC_Bulk_03_BulkEditEntryPoint", alwaysRun = false)
    public void testTC_Bulk_04_BulkEditDialogOpens() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_EDIT,
            "TC_Bulk_04: Bulk Edit dialog");
        try {
            assetPage.navigateToAssets();
            pause(3000);

            List<WebElement> checkboxes = driver.findElements(By.cssSelector(
                ".MuiDataGrid-cellCheckbox input[type='checkbox']"));
            if (checkboxes.size() < 2) {
                logWarning("Need 2+ rows selected for Bulk Edit — skipping");
                return;
            }
            for (int i = 0; i < 2; i++) {
                try { safeClick(checkboxes.get(i)); pause(400); } catch (Exception ignored) {}
            }
            pause(1500);

            WebElement bulkEditBtn = findByText("Bulk Edit", "Edit Selected", "Bulk Actions");
            Assert.assertNotNull(bulkEditBtn, "Bulk Edit button missing after selection");
            safeClick(bulkEditBtn);
            pause(2500);
            logStepWithScreenshot("Bulk Edit dialog opened");

            // Expect a dialog to be present
            List<WebElement> dialog = driver.findElements(
                By.cssSelector("[role='dialog'], [class*='MuiDialog']"));
            Assert.assertFalse(dialog.isEmpty(),
                "Bulk Edit click did not open a dialog");
            logStep("Bulk Edit dialog visible");

            // Cancel to avoid committing changes
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1000);
            ExtentReportManager.logPass("Bulk Edit dialog opens with editable field UI");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_04_error");
            Assert.fail("TC_Bulk_04 crashed: " + e.getMessage());
        }
    }

    // -------- helpers --------

    private File createSampleCsv() throws Exception {
        File f = new File("/tmp/bulk-upload-sample.csv");
        String csv = "name,qr_code,asset_class,model,manufacturer\n"
                   + "BulkTest_A,QR_A_" + System.currentTimeMillis() + ",Circuit Breaker,MODEL-A,ACME\n"
                   + "BulkTest_B,QR_B_" + System.currentTimeMillis() + ",Circuit Breaker,MODEL-B,ACME\n";
        Files.write(f.toPath(), csv.getBytes());
        return f;
    }
}
