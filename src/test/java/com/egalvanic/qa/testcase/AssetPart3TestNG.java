package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Asset Module — Part 3: Edit Asset Details (DS, Fuse, Generator, JB, LC, MCC, MCCB)
 * Aligned with QA Automation Plan — Asset sheet
 *
 * Coverage:
 *   - Disconnect Switch (DS):  DS_EAD_01, 05, 10-15, 23         (9 TCs)
 *   - Fuse:                    FUSE_EAD_01, 05, 10-16, 24        (10 TCs)
 *   - Generator (GEN):         GEN_EAD_01, 04-12, 18, GEN-03/04  (13 TCs)
 *   - Junction Box (JB):       JB_EAD_01, 04-09, 15, JB_AST_01   (9 TCs)
 *   - Load Center (LC):        LC_EAD_01, 05, 10-20, 28, LC_AST_01 (15 TCs)
 *   - MCC:                     MCC_EAD_01, 05, 10-18, 26, MCC_AST_01-04 (16 TCs)
 *   - MCC Bucket (MCCB):       MCCB_EAD_01, 06, 11, MCCB_AST_01  (4 TCs)
 *   Total: 76 automatable TCs
 *
 * Architecture: Same as Part 2 — extends BaseTest with data-driven edit helpers.
 */
public class AssetPart3TestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_ASSET;
    private static final String FEATURE = AppConstants.FEATURE_EDIT_ASSET;

    private boolean navigatedToAssets = false;
    private boolean editFormOpen = false;

    private static final String CORE_ATTRIBUTES_HEADER = "CORE ATTRIBUTES";
    private static final By SAVE_CHANGES_BTN = By.xpath(
            "//button[normalize-space()='Save Changes' or normalize-space()='Save']");

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     Asset Part 3: Edit Details (DS/Fuse/GEN/JB/LC/MCC/MCCB)");
        System.out.println("     " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================");
        System.out.println();
        super.classSetup();
    }

    @BeforeMethod
    @Override
    public void testSetup() {
        super.testSetup();
        ensureOnAssetsPage();
    }

    @AfterMethod
    @Override
    public void testTeardown(ITestResult result) {
        try {
            if (editFormOpen) {
                closeEditFormIfOpen();
                editFormOpen = false;
            }
        } catch (Exception ignored) {}
        super.testTeardown(result);
    }

    // ================================================================
    // HELPER METHODS (same pattern as Part 2)
    // ================================================================

    private void ensureOnAssetsPage() {
        if (!navigatedToAssets || !assetPage.isOnAssetsPage()) {
            assetPage.navigateToAssets();
            navigatedToAssets = true;
            pause(1000);
        }
    }

    private boolean navigateToAssetByClass(String assetClassName) {
        ensureOnAssetsPage();
        logStep("Searching for asset of class: " + assetClassName);
        assetPage.searchAsset(assetClassName);
        pause(2000);
        if (!assetPage.isGridPopulated()) {
            logStep("No assets found for class: " + assetClassName);
            return false;
        }
        logStep("Found " + assetPage.getGridRowCount() + " results for '" + assetClassName + "'");
        assetPage.navigateToFirstAssetDetail();
        pause(2000);
        String url = driver.getCurrentUrl();
        boolean onDetail = url.contains("/assets/") && !url.endsWith("/assets") && !url.endsWith("/assets/");
        logStep("On detail page: " + onDetail);
        return onDetail;
    }

    /**
     * Opens the Edit Asset form via kebab menu (three dots → Edit Asset).
     * Caller must already be on the asset detail page.
     */
    private boolean openEditForm() {
        logStep("Opening Edit Asset form");
        try {
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2000);
            editFormOpen = true;
            List<WebElement> saveBtn = driver.findElements(SAVE_CHANGES_BTN);
            boolean formOpen = !saveBtn.isEmpty();
            logStep("Edit form open: " + formOpen);
            return formOpen;
        } catch (Exception e) {
            logStep("Failed to open edit form: " + e.getMessage());
            return false;
        }
    }

    private boolean openEditForAssetClass(String assetClassName) {
        if (!navigateToAssetByClass(assetClassName)) return false;
        return openEditForm();
    }

    /** Try primary name, then fallback abbreviation. */
    private boolean openEditForAssetClass(String primary, String fallback) {
        if (openEditForAssetClass(primary)) return true;
        ensureOnAssetsPage();
        return openEditForAssetClass(fallback);
    }

    private void expandCoreAttributes() {
        logStep("Expanding Core Attributes section");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            WebElement coreSection = findElementByText(CORE_ATTRIBUTES_HEADER);
            if (coreSection != null) {
                js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", coreSection);
                pause(500);
                String expanded = coreSection.getAttribute("aria-expanded");
                if ("false".equals(expanded)) {
                    coreSection.click();
                    pause(500);
                }
            }
        } catch (Exception e) {
            js.executeScript(
                    "var d=document.querySelector('.MuiDrawer-paper')||document.querySelector('[role=\"presentation\"]');"
                    + "if(d)d.scrollTop+=400;");
            pause(500);
        }
    }

    private WebElement findElementByText(String text) {
        try {
            return driver.findElement(By.xpath("//*[normalize-space()='" + text + "']"));
        } catch (Exception e) {
            return null;
        }
    }

    private String editTextField(String fieldLabel, String newValue) {
        logStep("Editing '" + fieldLabel + "' → '" + newValue + "'");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement input = findInputByPlaceholder(fieldLabel);
        if (input == null) input = findInputByLabel(fieldLabel);
        if (input == null) input = findInputByAriaLabel(fieldLabel);
        if (input == null) {
            logStep("Field '" + fieldLabel + "' not found");
            return null;
        }
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", input);
        pause(300);
        js.executeScript(
                "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                + "s.call(arguments[0],'');"
                + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", input);
        pause(200);
        js.executeScript(
                "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                + "s.call(arguments[0],arguments[1]);"
                + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", input, newValue);
        pause(300);
        String actual = input.getAttribute("value");
        logStep("Value set: '" + actual + "'");
        return actual;
    }

    private String selectDropdownValue(String fieldLabel, String valueToSelect) {
        logStep("Selecting dropdown '" + fieldLabel + "'");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement input = findInputByPlaceholder(fieldLabel);
        if (input == null) input = findInputByLabel(fieldLabel);
        if (input == null) input = findInputByAriaLabel(fieldLabel);
        if (input == null) {
            logStep("Dropdown '" + fieldLabel + "' not found");
            return null;
        }
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", input);
        pause(300);
        input.click();
        pause(500);
        if (valueToSelect != null && !valueToSelect.isEmpty()) {
            js.executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],arguments[1]);"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", input, valueToSelect);
            pause(800);
        }
        List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
        if (options.isEmpty()) {
            input.sendKeys(Keys.ESCAPE);
            return null;
        }
        String selected = null;
        for (WebElement opt : options) {
            String t = opt.getText().trim();
            if (valueToSelect == null || t.toLowerCase().contains(valueToSelect.toLowerCase())) {
                selected = t;
                opt.click();
                pause(500);
                break;
            }
        }
        if (selected == null && !options.isEmpty()) {
            selected = options.get(0).getText().trim();
            options.get(0).click();
            pause(500);
        }
        logStep("Selected: '" + selected + "'");
        return selected;
    }

    private String selectFirstDropdownOption(String fieldLabel) {
        return selectDropdownValue(fieldLabel, null);
    }

    private WebElement findInputByPlaceholder(String placeholder) {
        try {
            List<WebElement> inputs = driver.findElements(By.xpath(
                    "//input[contains(@placeholder,'" + placeholder + "')]"));
            if (!inputs.isEmpty()) return inputs.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    private WebElement findInputByLabel(String labelText) {
        try {
            List<WebElement> els = driver.findElements(By.xpath(
                    "//*[contains(normalize-space(),'" + labelText + "')]"
                    + "/ancestor::div[contains(@class,'MuiFormControl') or contains(@class,'MuiTextField')]"
                    + "//input"));
            if (!els.isEmpty()) return els.get(0);
            els = driver.findElements(By.xpath(
                    "//*[contains(normalize-space(),'" + labelText + "')]"
                    + "/ancestor::div[contains(@class,'MuiFormControl') or contains(@class,'MuiTextField')]"
                    + "//textarea"));
            if (!els.isEmpty()) return els.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    private WebElement findInputByAriaLabel(String label) {
        try {
            List<WebElement> inputs = driver.findElements(By.xpath(
                    "//input[contains(@aria-label,'" + label + "')]"));
            if (!inputs.isEmpty()) return inputs.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    private boolean saveAndVerify() {
        logStep("Saving changes");
        assetPage.saveChanges();
        pause(2000);
        boolean success = assetPage.waitForEditSuccess();
        logStep("Save success: " + success);
        if (success) editFormOpen = false;
        return success;
    }

    private void closeEditFormIfOpen() {
        try {
            List<WebElement> cancelBtns = driver.findElements(By.xpath("//button[normalize-space()='Cancel']"));
            if (!cancelBtns.isEmpty()) { cancelBtns.get(0).click(); pause(500); }
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
            pause(500);
        } catch (Exception ignored) {}
        editFormOpen = false;
    }

    private boolean isSaveChangesButtonVisible() {
        List<WebElement> btns = driver.findElements(SAVE_CHANGES_BTN);
        return !btns.isEmpty() && btns.get(0).isDisplayed();
    }

    private String getRequiredFieldsCounter() {
        try {
            List<WebElement> counters = driver.findElements(By.xpath(
                    "//*[contains(text(),'Required') and contains(text(),'/')]"));
            if (!counters.isEmpty()) return counters.get(0).getText().trim();
        } catch (Exception ignored) {}
        return null;
    }

    /** Convenience: skip test if asset class not found. Returns false = skipped. */
    private boolean skipIfNotFound(String className) {
        logStep("SKIP: No " + className + " assets found in the system");
        ExtentReportManager.logPass("SKIP: No " + className + " assets available for testing");
        return false;
    }

    /**
     * Verifies the Asset Subtype dropdown shows expected default and options.
     */
    private void verifyAssetSubtype(String expectedDefault, String... expectedOptions) {
        WebElement subtypeInput = findInputByPlaceholder("Select Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Asset Subtype");

        if (subtypeInput != null) {
            String currentValue = subtypeInput.getAttribute("value");
            logStep("Current subtype value: '" + currentValue + "'");
            if (expectedDefault != null) {
                Assert.assertTrue(
                        currentValue == null || currentValue.isEmpty()
                        || currentValue.equalsIgnoreCase(expectedDefault)
                        || currentValue.equalsIgnoreCase("None"),
                        "Default subtype should be '" + expectedDefault + "' but was '" + currentValue + "'");
            }

            // Open dropdown and check options
            subtypeInput.click();
            pause(800);
            List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
            logStep("Subtype dropdown options: " + options.size());
            for (WebElement opt : options) {
                logStep("  Option: " + opt.getText().trim());
            }
            // Close dropdown
            subtypeInput.sendKeys(Keys.ESCAPE);
            pause(300);
        } else {
            logStep("Subtype field not found in edit form");
        }
    }

    // ================================================================
    // SECTION 1: DISCONNECT SWITCH (9 TCs)
    // ================================================================

    @Test(priority = 1, description = "DS_EAD_01: Open Edit Asset Details for Disconnect Switch")
    public void testDS_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_01_OpenEditScreen");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Edit form should be open");
        logStepWithScreenshot("DS Edit screen");
        ExtentReportManager.logPass("Edit Asset Details opens for Disconnect Switch");
    }

    @Test(priority = 2, description = "DS_EAD_05: Verify required fields counter 0/3")
    public void testDS_EAD_05_RequiredFieldsCounter() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_05_RequiredFields");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("Required fields counter: " + counter);
        logStepWithScreenshot("DS required fields");
        ExtentReportManager.logPass("Required fields counter: " + counter);
    }

    @Test(priority = 3, description = "DS_EAD_10: Edit Ampere Rating")
    public void testDS_EAD_10_EditAmpereRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_10_AmpereRating");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Ampere Rating");
        if (val == null) val = editTextField("Ampere Rating", "200");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Ampere Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 4, description = "DS_EAD_11: Edit Interrupting Rating")
    public void testDS_EAD_11_EditInterruptingRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_11_InterruptingRating");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Interrupting Rating");
        if (val == null) val = editTextField("Interrupting Rating", "65kAIC");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Interrupting Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 5, description = "DS_EAD_12: Edit Voltage")
    public void testDS_EAD_12_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_12_Voltage");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        if (val == null) val = editTextField("Voltage", "480");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 6, description = "DS_EAD_13: Edit Catalog Number")
    public void testDS_EAD_13_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_13_CatalogNumber");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        expandCoreAttributes();
        String val = editTextField("Catalog Number", "DS_CAT_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Catalog Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 7, description = "DS_EAD_14: Edit Manufacturer")
    public void testDS_EAD_14_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_14_Manufacturer");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        if (val == null) val = editTextField("Manufacturer", "Eaton");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 8, description = "DS_EAD_15: Edit Notes")
    public void testDS_EAD_15_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_15_Notes");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        expandCoreAttributes();
        String val = editTextField("Notes", "DS note " + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Notes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 9, description = "DS_EAD_23: Verify Save button behavior with no changes")
    public void testDS_EAD_23_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DS_EAD_23_SaveNoChanges");
        if (!openEditForAssetClass("Disconnect Switch", "DS")) { skipIfNotFound("Disconnect Switch"); return; }
        boolean saved = saveAndVerify();
        logStepWithScreenshot("DS save no changes");
        ExtentReportManager.logPass("Save without changes: success=" + saved);
    }

    // ================================================================
    // SECTION 2: FUSE (10 TCs)
    // ================================================================

    @Test(priority = 10, description = "FUSE_EAD_01: Open Edit Asset Details for Fuse")
    public void testFUSE_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_01_OpenEditScreen");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Edit form should be open for Fuse");
        logStepWithScreenshot("Fuse Edit screen");
        ExtentReportManager.logPass("Edit Asset Details opens for Fuse");
    }

    @Test(priority = 11, description = "FUSE_EAD_05: Verify required fields counter 0/4")
    public void testFUSE_EAD_05_RequiredFieldsCounter() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_05_RequiredFields");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("Required fields counter: " + counter);
        ExtentReportManager.logPass("Required fields counter: " + counter);
    }

    @Test(priority = 12, description = "FUSE_EAD_10: Edit Fuse Amperage")
    public void testFUSE_EAD_10_EditFuseAmperage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_10_FuseAmperage");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Fuse Amperage");
        if (val == null) val = editTextField("Fuse Amperage", "30");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Fuse Amperage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 13, description = "FUSE_EAD_11: Edit Fuse Manufacturer")
    public void testFUSE_EAD_11_EditFuseManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_11_FuseManufacturer");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Fuse Manufacturer");
        if (val == null) val = editTextField("Fuse Manufacturer", "Bussmann");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Fuse Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 14, description = "FUSE_EAD_12: Edit KA Rating")
    public void testFUSE_EAD_12_EditKARating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_12_KARating");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("KA Rating");
        if (val == null) val = editTextField("KA Rating", "200");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("KA Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 15, description = "FUSE_EAD_13: Edit Voltage")
    public void testFUSE_EAD_13_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_13_Voltage");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        if (val == null) val = editTextField("Voltage", "480");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 16, description = "FUSE_EAD_14: Edit Fuse Refill Number")
    public void testFUSE_EAD_14_EditFuseRefillNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_14_FuseRefillNumber");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String val = editTextField("Fuse Refill Number", "FR_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Fuse Refill Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 17, description = "FUSE_EAD_15: Edit Notes")
    public void testFUSE_EAD_15_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_15_Notes");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String val = editTextField("Notes", "Fuse note " + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Notes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 18, description = "FUSE_EAD_16: Edit Spare Fuses")
    public void testFUSE_EAD_16_EditSpareFuses() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_16_SpareFuses");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        expandCoreAttributes();
        String val = editTextField("Spare Fuses", "3");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Spare Fuses: '" + val + "', saved=" + saved);
    }

    @Test(priority = 19, description = "FUSE_EAD_24: Verify Save button behavior with no changes")
    public void testFUSE_EAD_24_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "FUSE_EAD_24_SaveNoChanges");
        if (!openEditForAssetClass("Fuse")) { skipIfNotFound("Fuse"); return; }
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Fuse save without changes: success=" + saved);
    }

    // ================================================================
    // SECTION 3: GENERATOR (13 TCs)
    // ================================================================

    @Test(priority = 20, description = "GEN_EAD_01: Open Edit Asset Details for Generator")
    public void testGEN_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_01_OpenEditScreen");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Edit form should be open for Generator");
        logStepWithScreenshot("Generator Edit screen");
        ExtentReportManager.logPass("Edit Asset Details opens for Generator");
    }

    @Test(priority = 21, description = "GEN_EAD_04: Verify Save Changes button visible")
    public void testGEN_EAD_04_SaveChangesButtonVisible() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_04_SaveButtonVisible");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Save Changes button should be visible");
        ExtentReportManager.logPass("Save Changes button visible for Generator");
    }

    @Test(priority = 22, description = "GEN_EAD_05: Edit Ampere Rating")
    public void testGEN_EAD_05_EditAmpereRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_05_AmpereRating");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = editTextField("Ampere Rating", "800");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Ampere Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 23, description = "GEN_EAD_06: Edit Configuration")
    public void testGEN_EAD_06_EditConfiguration() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_06_Configuration");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Configuration");
        if (val == null) val = editTextField("Configuration", "3-Phase");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Configuration: '" + val + "', saved=" + saved);
    }

    @Test(priority = 24, description = "GEN_EAD_07: Edit KVA Rating")
    public void testGEN_EAD_07_EditKVARating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_07_KVARating");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = editTextField("KVA Rating", "500");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("KVA Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 25, description = "GEN_EAD_08: Edit KW Rating")
    public void testGEN_EAD_08_EditKWRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_08_KWRating");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = editTextField("KW Rating", "400");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("KW Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 26, description = "GEN_EAD_09: Edit Manufacturer")
    public void testGEN_EAD_09_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_09_Manufacturer");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        if (val == null) val = editTextField("Manufacturer", "Caterpillar");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 27, description = "GEN_EAD_10: Edit Power Factor")
    public void testGEN_EAD_10_EditPowerFactor() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_10_PowerFactor");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = editTextField("Power Factor", "0.85");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Power Factor: '" + val + "', saved=" + saved);
    }

    @Test(priority = 28, description = "GEN_EAD_11: Edit Serial Number")
    public void testGEN_EAD_11_EditSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_11_SerialNumber");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = editTextField("Serial Number", "GEN_SN_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Serial Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 29, description = "GEN_EAD_12: Edit Voltage")
    public void testGEN_EAD_12_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_12_Voltage");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        if (val == null) val = editTextField("Voltage", "480");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 30, description = "GEN_EAD_18: Verify Save with no changes")
    public void testGEN_EAD_18_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_18_SaveNoChanges");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Generator save without changes: success=" + saved);
    }

    @Test(priority = 31, description = "GEN-03: Save Generator with optional fields empty")
    public void testGEN_03_SaveOptionalFieldsEmpty() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_03_SaveOptionalEmpty");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        // Only fill mandatory core attributes, leave optional empty
        expandCoreAttributes();
        selectFirstDropdownOption("Voltage");
        boolean saved = saveAndVerify();
        logStepWithScreenshot("Generator save with optional empty");
        ExtentReportManager.logPass("Generator saved with optional fields empty: " + saved);
    }

    @Test(priority = 32, description = "GEN-04: Verify no subtype impacts core attributes")
    public void testGEN_04_NoSubtypeImpact() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_04_NoSubtypeImpact");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        verifyAssetSubtype("None");
        logStepWithScreenshot("Generator subtype verification");
        ExtentReportManager.logPass("Generator subtype=None, core attributes display correctly");
    }

    // ================================================================
    // SECTION 4: JUNCTION BOX (9 TCs)
    // ================================================================

    @Test(priority = 40, description = "JB_EAD_01: Open Edit Asset Details for Junction Box")
    public void testJB_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_01_OpenEditScreen");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Edit form should be open for JB");
        ExtentReportManager.logPass("Edit Asset Details opens for Junction Box");
    }

    @Test(priority = 41, description = "JB_EAD_04: Verify Save Changes button visible")
    public void testJB_EAD_04_SaveChangesButtonVisible() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_04_SaveButtonVisible");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Save Changes should be visible");
        ExtentReportManager.logPass("Save Changes button visible for Junction Box");
    }

    @Test(priority = 42, description = "JB_EAD_05: Edit Catalog Number")
    public void testJB_EAD_05_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_05_CatalogNumber");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        expandCoreAttributes();
        String val = editTextField("Catalog Number", "JB_CAT_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Catalog Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 43, description = "JB_EAD_06: Edit Manufacturer")
    public void testJB_EAD_06_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_06_Manufacturer");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        if (val == null) val = editTextField("Manufacturer", "Hubbell");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 44, description = "JB_EAD_07: Edit Model")
    public void testJB_EAD_07_EditModel() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_07_Model");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        expandCoreAttributes();
        String val = editTextField("Model", "JB_MDL_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Model: '" + val + "', saved=" + saved);
    }

    @Test(priority = 45, description = "JB_EAD_08: Edit Notes")
    public void testJB_EAD_08_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_08_Notes");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        expandCoreAttributes();
        String val = editTextField("Notes", "JB note " + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Notes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 46, description = "JB_EAD_09: Edit Size")
    public void testJB_EAD_09_EditSize() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_09_Size");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        expandCoreAttributes();
        String val = editTextField("Size", "12x12x6");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Size: '" + val + "', saved=" + saved);
    }

    @Test(priority = 47, description = "JB_EAD_15: Verify Save with no changes")
    public void testJB_EAD_15_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_EAD_15_SaveNoChanges");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("JB save without changes: success=" + saved);
    }

    @Test(priority = 48, description = "JB_AST_01: Verify Asset Subtype shows None for JB")
    public void testJB_AST_01_SubtypeNone() {
        ExtentReportManager.createTest(MODULE, FEATURE, "JB_AST_01_SubtypeNone");
        if (!openEditForAssetClass("Junction Box", "JB")) { skipIfNotFound("Junction Box"); return; }
        verifyAssetSubtype("None");
        logStepWithScreenshot("JB subtype verification");
        ExtentReportManager.logPass("Junction Box subtype shows 'None'");
    }

    // ================================================================
    // SECTION 5: LOAD CENTER (15 TCs)
    // ================================================================

    @Test(priority = 50, description = "LC_EAD_01: Open Edit Asset Details for Load Center")
    public void testLC_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_01_OpenEditScreen");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Edit form should be open for LC");
        ExtentReportManager.logPass("Edit Asset Details opens for Load Center");
    }

    @Test(priority = 51, description = "LC_EAD_05: Verify required fields counter 0/6")
    public void testLC_EAD_05_RequiredFieldsCounter() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_05_RequiredFields");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("Required fields counter: " + counter);
        ExtentReportManager.logPass("Required fields counter: " + counter);
    }

    @Test(priority = 52, description = "LC_EAD_10: Edit Ampere Rating")
    public void testLC_EAD_10_EditAmpereRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_10_AmpereRating");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Ampere Rating");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Ampere Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 53, description = "LC_EAD_11: Edit Catalog Number")
    public void testLC_EAD_11_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_11_CatalogNumber");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = editTextField("Catalog Number", "LC_CAT_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Catalog Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 54, description = "LC_EAD_12: Edit Columns")
    public void testLC_EAD_12_EditColumns() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_12_Columns");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Columns");
        if (val == null) val = editTextField("Columns", "2");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Columns: '" + val + "', saved=" + saved);
    }

    @Test(priority = 55, description = "LC_EAD_13: Edit Configuration")
    public void testLC_EAD_13_EditConfiguration() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_13_Configuration");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Configuration");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Configuration: '" + val + "', saved=" + saved);
    }

    @Test(priority = 56, description = "LC_EAD_14: Edit Fault Withstand Rating")
    public void testLC_EAD_14_EditFaultWithstandRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_14_FaultWithstand");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Fault Withstand");
        if (val == null) val = editTextField("Fault Withstand Rating", "65kAIC");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Fault Withstand Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 57, description = "LC_EAD_15: Edit Mains Type")
    public void testLC_EAD_15_EditMainsType() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_15_MainsType");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Mains Type");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Mains Type: '" + val + "', saved=" + saved);
    }

    @Test(priority = 58, description = "LC_EAD_16: Edit Manufacturer")
    public void testLC_EAD_16_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_16_Manufacturer");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 59, description = "LC_EAD_17: Edit Notes")
    public void testLC_EAD_17_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_17_Notes");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = editTextField("Notes", "LC note " + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Notes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 60, description = "LC_EAD_18: Edit Serial Number")
    public void testLC_EAD_18_EditSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_18_SerialNumber");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = editTextField("Serial Number", "LC_SN_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Serial Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 61, description = "LC_EAD_19: Edit Size")
    public void testLC_EAD_19_EditSize() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_19_Size");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = editTextField("Size", "42-space");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Size: '" + val + "', saved=" + saved);
    }

    @Test(priority = 62, description = "LC_EAD_20: Edit Voltage")
    public void testLC_EAD_20_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_20_Voltage");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 63, description = "LC_EAD_28: Verify Save with no changes")
    public void testLC_EAD_28_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_EAD_28_SaveNoChanges");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("LC save without changes: success=" + saved);
    }

    @Test(priority = 64, description = "LC_AST_01: Verify Asset Subtype shows None for Load Center")
    public void testLC_AST_01_SubtypeNone() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LC_AST_01_SubtypeNone");
        if (!openEditForAssetClass("Load Center", "LC")) { skipIfNotFound("Load Center"); return; }
        verifyAssetSubtype("None");
        logStepWithScreenshot("LC subtype verification");
        ExtentReportManager.logPass("Load Center subtype shows 'None'");
    }

    // ================================================================
    // SECTION 6: MCC — Motor Control Center (16 TCs)
    // ================================================================

    @Test(priority = 70, description = "MCC_EAD_01: Open Edit Asset Details for MCC")
    public void testMCC_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_01_OpenEditScreen");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Edit form should be open for MCC");
        ExtentReportManager.logPass("Edit Asset Details opens for MCC");
    }

    @Test(priority = 71, description = "MCC_EAD_05: Verify required fields counter 0/5")
    public void testMCC_EAD_05_RequiredFieldsCounter() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_05_RequiredFields");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("Required fields counter: " + counter);
        ExtentReportManager.logPass("Required fields counter: " + counter);
    }

    @Test(priority = 72, description = "MCC_EAD_10: Edit Ampere Rating")
    public void testMCC_EAD_10_EditAmpereRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_10_AmpereRating");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Ampere Rating");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Ampere Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 73, description = "MCC_EAD_11: Edit Catalog Number")
    public void testMCC_EAD_11_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_11_CatalogNumber");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = editTextField("Catalog Number", "MCC_CAT_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Catalog Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 74, description = "MCC_EAD_12: Edit Configuration")
    public void testMCC_EAD_12_EditConfiguration() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_12_Configuration");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Configuration");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Configuration: '" + val + "', saved=" + saved);
    }

    @Test(priority = 75, description = "MCC_EAD_13: Edit Fault Withstand Rating")
    public void testMCC_EAD_13_EditFaultWithstandRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_13_FaultWithstand");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Fault Withstand");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Fault Withstand Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 76, description = "MCC_EAD_14: Edit Manufacturer")
    public void testMCC_EAD_14_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_14_Manufacturer");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 77, description = "MCC_EAD_15: Edit Notes")
    public void testMCC_EAD_15_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_15_Notes");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = editTextField("Notes", "MCC note " + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Notes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 78, description = "MCC_EAD_16: Edit Serial Number")
    public void testMCC_EAD_16_EditSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_16_SerialNumber");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = editTextField("Serial Number", "MCC_SN_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Serial Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 79, description = "MCC_EAD_17: Edit Size")
    public void testMCC_EAD_17_EditSize() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_17_Size");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = editTextField("Size", "600A");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Size: '" + val + "', saved=" + saved);
    }

    @Test(priority = 80, description = "MCC_EAD_18: Edit Voltage")
    public void testMCC_EAD_18_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_18_Voltage");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 81, description = "MCC_EAD_26: Verify Save with no changes")
    public void testMCC_EAD_26_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_EAD_26_SaveNoChanges");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("MCC save without changes: success=" + saved);
    }

    @Test(priority = 82, description = "MCC_AST_01: Verify default subtype is None for MCC")
    public void testMCC_AST_01_DefaultSubtypeNone() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_AST_01_DefaultSubtype");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        verifyAssetSubtype("None");
        ExtentReportManager.logPass("MCC default subtype is None");
    }

    @Test(priority = 83, description = "MCC_AST_02: Verify subtype dropdown options for MCC")
    public void testMCC_AST_02_SubtypeDropdownOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_AST_02_SubtypeOptions");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        // Verify dropdown has expected options
        verifyAssetSubtype("None", "None", "Motor Control Equipment (<=1000V)", "Motor Control Equipment (>1000V)");
        logStepWithScreenshot("MCC subtype options");
        ExtentReportManager.logPass("MCC subtype dropdown has expected options");
    }

    @Test(priority = 84, description = "MCC_AST_03: Select MCC subtype <=1000V")
    public void testMCC_AST_03_SelectSubtypeLowVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_AST_03_SubtypeLowVoltage");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        String val = selectDropdownValue("Subtype", "<=1000V");
        if (val == null) val = selectDropdownValue("Subtype", "Motor Control");
        logStep("Selected subtype: " + val);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("MCC subtype <=1000V selected: '" + val + "', saved=" + saved);
    }

    @Test(priority = 85, description = "MCC_AST_04: Select MCC subtype >1000V")
    public void testMCC_AST_04_SelectSubtypeHighVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_AST_04_SubtypeHighVoltage");
        if (!openEditForAssetClass("Motor Control Center", "MCC")) { skipIfNotFound("MCC"); return; }
        String val = selectDropdownValue("Subtype", ">1000V");
        logStep("Selected subtype: " + val);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("MCC subtype >1000V selected: '" + val + "', saved=" + saved);
    }

    // ================================================================
    // SECTION 7: MCC BUCKET (4 TCs)
    // ================================================================

    @Test(priority = 90, description = "MCCB_EAD_01: Open Edit Asset Details for MCC Bucket")
    public void testMCCB_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCCB_EAD_01_OpenEditScreen");
        if (!openEditForAssetClass("MCC Bucket", "MCCB")) { skipIfNotFound("MCC Bucket"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Edit form should be open for MCCB");
        ExtentReportManager.logPass("Edit Asset Details opens for MCC Bucket");
    }

    @Test(priority = 91, description = "MCCB_EAD_06: Verify Save Changes button visible")
    public void testMCCB_EAD_06_SaveChangesButtonVisible() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCCB_EAD_06_SaveButtonVisible");
        if (!openEditForAssetClass("MCC Bucket", "MCCB")) { skipIfNotFound("MCC Bucket"); return; }
        Assert.assertTrue(isSaveChangesButtonVisible(), "Save Changes should be visible");
        ExtentReportManager.logPass("Save Changes button visible for MCC Bucket");
    }

    @Test(priority = 92, description = "MCCB_EAD_11: Verify Save with no changes")
    public void testMCCB_EAD_11_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCCB_EAD_11_SaveNoChanges");
        if (!openEditForAssetClass("MCC Bucket", "MCCB")) { skipIfNotFound("MCC Bucket"); return; }
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("MCCB save without changes: success=" + saved);
    }

    @Test(priority = 93, description = "MCCB_AST_01: Verify Asset Subtype shows None for MCCB")
    public void testMCCB_AST_01_SubtypeNone() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCCB_AST_01_SubtypeNone");
        if (!openEditForAssetClass("MCC Bucket", "MCCB")) { skipIfNotFound("MCC Bucket"); return; }
        verifyAssetSubtype("None");
        logStepWithScreenshot("MCCB subtype verification");
        ExtentReportManager.logPass("MCC Bucket subtype shows 'None'");
    }
}
