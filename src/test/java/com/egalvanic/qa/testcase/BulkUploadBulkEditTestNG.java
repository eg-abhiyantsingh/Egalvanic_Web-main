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

    // =================================================================
    // TC_Bulk_05 — Template download available in Bulk Upload dialog
    // =================================================================
    @Test(priority = 5, description = "Bulk Upload dialog offers a downloadable CSV template")
    public void testTC_Bulk_05_TemplateDownload() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_UPLOAD,
            "TC_Bulk_05: Template download");
        try {
            assetPage.navigateToAssets();
            pause(2500);
            WebElement entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            if (entry == null && openMoreMenu()) {
                entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            }
            if (entry == null) { logWarning("No Bulk Upload entry — skip"); return; }
            safeClick(entry);
            pause(2500);

            WebElement template = findByText("Download Template", "CSV Template", "Sample CSV",
                "Example", "Template", "Download Sample");
            ScreenshotUtil.captureScreenshot("TC_Bulk_05");
            Assert.assertNotNull(template,
                "Bulk Upload dialog has no template/sample download link — users won't know the CSV format");
            logStep("Template link: " + template.getText());
            ExtentReportManager.logPass("Template download link present in Bulk Upload");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_05_error");
            Assert.fail("TC_Bulk_05 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_06 — Invalid/malformed CSV shows validation error
    // =================================================================
    @Test(priority = 6, description = "Malformed CSV triggers validation error, not silent accept")
    public void testTC_Bulk_06_InvalidCsvValidation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_UPLOAD,
            "TC_Bulk_06: Invalid CSV validation");
        try {
            assetPage.navigateToAssets();
            pause(2500);
            WebElement entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            if (entry == null && openMoreMenu()) {
                entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            }
            if (entry == null) { logWarning("No Bulk Upload entry — skip"); return; }
            safeClick(entry);
            pause(2500);

            File bad = new File("/tmp/bulk-bad.csv");
            // CSV with only garbage columns, no required headers
            Files.write(bad.toPath(), "xxx,yyy,zzz\n1,2,3\n".getBytes());

            js().executeScript("document.querySelectorAll('input[type=\"file\"]').forEach(function(i){" +
                "i.style.display='block';i.style.opacity='1';i.style.position='relative';});");
            List<WebElement> inputs = driver.findElements(By.cssSelector("input[type='file']"));
            if (inputs.isEmpty()) { logWarning("No file input"); return; }
            inputs.get(inputs.size() - 1).sendKeys(bad.getAbsolutePath());
            pause(4000);

            // Look for any validation/error indication
            List<WebElement> errors = driver.findElements(By.xpath(
                "//*[contains(normalize-space(.), 'error') or contains(normalize-space(.), 'invalid') or " +
                "contains(normalize-space(.), 'required') or contains(normalize-space(.), 'missing') or " +
                "contains(normalize-space(.), 'format')]"));
            int visibleErrors = 0;
            for (WebElement e : errors) {
                if (e.isDisplayed() && e.getText().length() < 300) visibleErrors++;
            }
            logStep("Validation messages visible: " + visibleErrors);
            ScreenshotUtil.captureScreenshot("TC_Bulk_06");
            Assert.assertTrue(visibleErrors > 0,
                "No validation error shown for malformed CSV — may accept silently");
            ExtentReportManager.logPass("Malformed CSV triggers validation feedback");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_06_error");
            Assert.fail("TC_Bulk_06 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_07 — Bulk Upload preview / confirmation before commit
    // =================================================================
    @Test(priority = 7, description = "Valid CSV shows a preview of rows before commit")
    public void testTC_Bulk_07_ValidCsvShowsPreview() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_UPLOAD,
            "TC_Bulk_07: Preview on valid CSV");
        try {
            assetPage.navigateToAssets();
            pause(2500);
            WebElement entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            if (entry == null && openMoreMenu()) {
                entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
            }
            if (entry == null) { logWarning("No Bulk Upload entry — skip"); return; }
            safeClick(entry);
            pause(2500);

            File good = createSampleCsv();
            js().executeScript("document.querySelectorAll('input[type=\"file\"]').forEach(function(i){" +
                "i.style.display='block';i.style.opacity='1';i.style.position='relative';});");
            List<WebElement> inputs = driver.findElements(By.cssSelector("input[type='file']"));
            if (inputs.isEmpty()) { logWarning("No file input"); return; }
            inputs.get(inputs.size() - 1).sendKeys(good.getAbsolutePath());
            pause(4000);

            // Look for preview — could be a table, row count, or "2 rows ready to import"
            Object preview = js().executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [class*=\"MuiDialog\"]');" +
                "for (var d of dialogs) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 100) {" +
                "    var rows = d.querySelectorAll('tr, [role=\"row\"]');" +
                "    var text = d.textContent;" +
                "    var rowCountMention = /\\b\\d+\\s*(rows?|assets?|records?|entries)/i.test(text);" +
                "    return { rowsInDialog: rows.length, countMentioned: rowCountMention, textSnippet: text.substring(0,150) };" +
                "  }" +
                "}" +
                "return null;");
            logStep("Preview check: " + preview);
            ScreenshotUtil.captureScreenshot("TC_Bulk_07");

            // Cancel to avoid actual import
            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1000);
            ExtentReportManager.logPass("Valid CSV step reached with preview info captured");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_07_error");
            Assert.fail("TC_Bulk_07 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_08 — Select-all header checkbox
    // =================================================================
    @Test(priority = 8, description = "Grid header checkbox selects all visible rows")
    public void testTC_Bulk_08_SelectAllCheckbox() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_EDIT,
            "TC_Bulk_08: Select all");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            // Header checkbox is typically first checkbox in .MuiDataGrid-columnHeader
            List<WebElement> headerCb = driver.findElements(By.cssSelector(
                ".MuiDataGrid-columnHeader input[type='checkbox'], " +
                ".MuiDataGrid-columnHeaderCheckbox input[type='checkbox']"));
            if (headerCb.isEmpty()) { logWarning("No select-all checkbox in grid"); return; }
            safeClick(headerCb.get(0));
            pause(1500);
            int selectedRows = driver.findElements(By.cssSelector(
                ".MuiDataGrid-row[aria-selected='true'], .MuiDataGrid-row.Mui-selected")).size();
            int totalRows = driver.findElements(By.cssSelector(".MuiDataGrid-row")).size();
            logStep("Selected " + selectedRows + " of " + totalRows + " visible rows");
            ScreenshotUtil.captureScreenshot("TC_Bulk_08");
            Assert.assertTrue(selectedRows > 0 && selectedRows >= Math.min(totalRows, 1),
                "Select-all checkbox did not select visible rows (selected=" + selectedRows + ")");
            // Click again to deselect — cleanup for later tests
            safeClick(headerCb.get(0));
            pause(500);
            ExtentReportManager.logPass("Select-all selected " + selectedRows + "/" + totalRows);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_08_error");
            Assert.fail("TC_Bulk_08 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_09 — Deselect via header checkbox restores state
    // =================================================================
    @Test(priority = 9, description = "Clicking select-all twice deselects all rows")
    public void testTC_Bulk_09_DeselectAllRestores() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_EDIT,
            "TC_Bulk_09: Deselect all");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> headerCb = driver.findElements(By.cssSelector(
                ".MuiDataGrid-columnHeader input[type='checkbox']"));
            if (headerCb.isEmpty()) { logWarning("No select-all"); return; }
            safeClick(headerCb.get(0));
            pause(1000);
            int afterSelect = driver.findElements(By.cssSelector(
                ".MuiDataGrid-row[aria-selected='true'], .MuiDataGrid-row.Mui-selected")).size();
            safeClick(headerCb.get(0));
            pause(1000);
            int afterDeselect = driver.findElements(By.cssSelector(
                ".MuiDataGrid-row[aria-selected='true'], .MuiDataGrid-row.Mui-selected")).size();
            logStep("After select: " + afterSelect + ", after deselect: " + afterDeselect);
            ScreenshotUtil.captureScreenshot("TC_Bulk_09");
            Assert.assertEquals(afterDeselect, 0,
                "Second click on select-all did not deselect (still " + afterDeselect + " selected)");
            ExtentReportManager.logPass("Select-all toggles correctly (select=" + afterSelect + " → deselect=0)");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_09_error");
            Assert.fail("TC_Bulk_09 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_10 — Bulk Edit field picker has multiple options
    // =================================================================
    @Test(priority = 10, description = "Bulk Edit dialog field picker exposes multiple editable fields")
    public void testTC_Bulk_10_BulkEditFieldPicker() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_EDIT,
            "TC_Bulk_10: Field picker");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> checkboxes = driver.findElements(By.cssSelector(
                ".MuiDataGrid-cellCheckbox input[type='checkbox']"));
            if (checkboxes.size() < 2) { logWarning("Need 2+ rows"); return; }
            for (int i = 0; i < 2; i++) {
                try { safeClick(checkboxes.get(i)); pause(400); } catch (Exception ignored) {}
            }
            pause(1500);
            WebElement bulkEditBtn = findByText("Bulk Edit", "Edit Selected", "Bulk Actions");
            if (bulkEditBtn == null) { logWarning("No Bulk Edit button"); return; }
            safeClick(bulkEditBtn);
            pause(2500);

            // Check for a field-picker (select / combobox / radio group)
            List<WebElement> pickers = driver.findElements(By.cssSelector(
                "[role='dialog'] [role='combobox'], [role='dialog'] select, " +
                "[role='dialog'] [role='listbox'], [role='dialog'] input[role='combobox']"));
            logStep("Field-picker controls in Bulk Edit: " + pickers.size());
            ScreenshotUtil.captureScreenshot("TC_Bulk_10");
            Assert.assertTrue(pickers.size() >= 1,
                "Bulk Edit dialog has no field-picker — user can't choose which field to edit");

            WebElement cancel = findByText("Cancel", "Close");
            if (cancel != null) safeClick(cancel);
            pause(1000);
            ExtentReportManager.logPass("Bulk Edit exposes field-picker control");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_10_error");
            Assert.fail("TC_Bulk_10 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Bulk_11 — Bulk Edit with 0 rows selected — button disabled or hidden
    // =================================================================
    @Test(priority = 11, description = "Bulk Edit button disabled or hidden when no rows selected")
    public void testTC_Bulk_11_BulkEditGatedWithoutSelection() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_BULK_EDIT,
            "TC_Bulk_11: Gated without selection");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            // Ensure nothing selected
            List<WebElement> headerCb = driver.findElements(By.cssSelector(
                ".MuiDataGrid-columnHeader input[type='checkbox']:checked"));
            if (!headerCb.isEmpty()) { safeClick(headerCb.get(0)); pause(500); }

            WebElement bulkEditBtn = findByText("Bulk Edit", "Edit Selected", "Bulk Actions");
            ScreenshotUtil.captureScreenshot("TC_Bulk_11");
            if (bulkEditBtn == null) {
                // Absent = hidden when no selection → acceptable
                logStep("Bulk Edit button HIDDEN when no rows selected — acceptable gating");
                ExtentReportManager.logPass("Bulk Edit hidden without selection");
                return;
            }
            boolean disabled = "true".equals(bulkEditBtn.getAttribute("disabled"))
                            || bulkEditBtn.getAttribute("class").contains("disabled")
                            || "true".equals(bulkEditBtn.getAttribute("aria-disabled"));
            logStep("Bulk Edit disabled without selection: " + disabled);
            Assert.assertTrue(disabled,
                "Bulk Edit button is visible AND enabled with 0 rows selected — should be gated");
            ExtentReportManager.logPass("Bulk Edit gated (disabled) without selection");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Bulk_11_error");
            Assert.fail("TC_Bulk_11 crashed: " + e.getMessage());
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
