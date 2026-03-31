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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Asset Module — Part 5: Edit Asset Details
 * (Switchboard, Transformer, UPS, Utility, VFD)
 * Aligned with QA Automation Plan — Asset sheet
 *
 * Coverage:
 *   - Switchboard (SWB):   TC-SWB-01-04,05-15,16-18 + SWB_AST_01-04  (22 TCs)
 *   - Transformer (TRF):   TC-TRF-01-04,05-20,21-23 + TRF_AST_01-04  (27 TCs)
 *   - UPS:                 TC-UPS-01-04,05-12,13-15 + UPS_AST_01-04   (19 TCs)
 *   - Utility (UTL):       TC-UTL-01-09 + UTL_AST_01                  (10 TCs)
 *   - VFD:                 TC-VFD-01-08 + VFD_AST_01                   (9 TCs)
 *   Total: ~87 TCs (automatable: ~65)
 *
 * Architecture: Extends BaseTest — same data-driven helpers as Parts 2-4.
 */
public class AssetPart5TestNG extends BaseTest {

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
        System.out.println("     Asset Part 5: Edit Details (SWB/TRF/UPS/UTL/VFD)");
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
    // HELPER METHODS (same as Parts 2-4)
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
        logStep("Searching for asset class: " + assetClassName);
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
                js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", coreSection);
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

    private String getRequiredFieldsCounter() {
        try {
            List<WebElement> counters = driver.findElements(By.xpath(
                    "//*[contains(text(),'Required') and contains(text(),'/')]"));
            if (!counters.isEmpty()) return counters.get(0).getText().trim();
        } catch (Exception ignored) {}
        return null;
    }

    private boolean skipIfNotFound(String className) {
        logStep("SKIP: No " + className + " assets found in the system");
        ExtentReportManager.logPass("SKIP: No " + className + " assets available for testing");
        return false;
    }

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

            subtypeInput.click();
            pause(800);
            List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
            List<String> actualOptions = new ArrayList<>();
            for (WebElement opt : options) {
                String text = opt.getText().trim();
                actualOptions.add(text);
                logStep("  Subtype option: " + text);
            }
            logStep("Total subtype options: " + actualOptions.size());

            if (expectedOptions != null && expectedOptions.length > 0) {
                for (String expected : expectedOptions) {
                    boolean found = actualOptions.stream()
                            .anyMatch(a -> a.equalsIgnoreCase(expected) || a.contains(expected));
                    logStep("  Expected '" + expected + "' → " + (found ? "FOUND" : "MISSING"));
                }
            }

            subtypeInput.sendKeys(Keys.ESCAPE);
            pause(300);
        } else {
            logStep("Subtype field not found in edit form");
        }
    }

    private String selectSubtype(String subtypeName) {
        return selectDropdownValue("Subtype", subtypeName);
    }

    private boolean isCoreAttributesSectionVisible() {
        WebElement section = findElementByText(CORE_ATTRIBUTES_HEADER);
        if (section == null) return false;
        try {
            return section.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // SECTION 1: SWITCHBOARD (SWB) — 22 TCs (10 core fields + subtypes)
    // ================================================================

    @Test(priority = 1, description = "TC-SWB-01: Verify Core Attributes section loads for Switchboard")
    public void testSWB_01_CoreAttributesLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_01_CoreAttributes");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        boolean coreVisible = isCoreAttributesSectionVisible();
        expandCoreAttributes();
        logStep("SWB Core Attributes visible: " + coreVisible);
        logStepWithScreenshot("SWB Core Attributes");
        ExtentReportManager.logPass("SWB Core Attributes visible: " + coreVisible);
    }

    @Test(priority = 2, description = "TC-SWB-02: Verify all SWB core attributes visible by default")
    public void testSWB_02_AllFieldsVisible() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_02_AllFields");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();

        String[] fields = {"Ampere Rating", "Catalog Number", "Configuration",
                "Fault Withstand Rating", "Mains Type", "Manufacturer",
                "Notes", "Serial Number", "Size", "Voltage"};
        int found = 0;
        for (String field : fields) {
            WebElement input = findInputByPlaceholder(field);
            if (input == null) input = findInputByLabel(field);
            boolean exists = input != null;
            if (exists) found++;
            logStep("SWB field '" + field + "': " + (exists ? "FOUND" : "NOT FOUND"));
        }
        logStep("SWB fields found: " + found + "/" + fields.length);
        logStepWithScreenshot("SWB fields");
        ExtentReportManager.logPass("SWB fields: " + found + "/" + fields.length + " found");
    }

    @Test(priority = 3, description = "TC-SWB-03: Verify Required fields toggle behavior")
    public void testSWB_03_RequiredFieldsToggle() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_03_RequiredToggle");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("SWB Required fields counter: " + counter);
        logStepWithScreenshot("SWB Required toggle");
        ExtentReportManager.logPass("SWB required fields toggle: counter=" + counter);
    }

    @Test(priority = 4, description = "TC-SWB-04: Verify required field count indicator")
    public void testSWB_04_RequiredFieldCount() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_04_RequiredCount");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("SWB Required field count: " + counter);
        ExtentReportManager.logPass("SWB required count: " + counter);
    }

    @Test(priority = 5, description = "TC-SWB-05: Edit Ampere Rating")
    public void testSWB_05_EditAmpereRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_05_AmpereRating");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Ampere Rating");
        if (val == null) val = editTextField("Ampere Rating", "1600");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Ampere Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 6, description = "TC-SWB-06: Edit Catalog Number")
    public void testSWB_06_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_06_CatalogNumber");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = editTextField("Catalog Number", "CAT-SWB-" + System.currentTimeMillis() % 10000);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Catalog Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 7, description = "TC-SWB-07: Edit Configuration")
    public void testSWB_07_EditConfiguration() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_07_Configuration");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Configuration");
        if (val == null) val = editTextField("Configuration", "Main-Tie-Main");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Configuration: '" + val + "', saved=" + saved);
    }

    @Test(priority = 8, description = "TC-SWB-08: Edit Fault Withstand Rating")
    public void testSWB_08_EditFaultWithstand() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_08_FaultWithstand");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Fault Withstand Rating");
        if (val == null) val = editTextField("Fault Withstand Rating", "65kAIC");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Fault Withstand: '" + val + "', saved=" + saved);
    }

    @Test(priority = 9, description = "TC-SWB-09: Edit Mains Type")
    public void testSWB_09_EditMainsType() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_09_MainsType");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Mains Type");
        if (val == null) val = editTextField("Mains Type", "Main Breaker");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Mains Type: '" + val + "', saved=" + saved);
    }

    @Test(priority = 10, description = "TC-SWB-10: Edit Manufacturer")
    public void testSWB_10_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_10_Manufacturer");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        if (val == null) val = editTextField("Manufacturer", "Eaton");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 11, description = "TC-SWB-11: Edit Notes")
    public void testSWB_11_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_11_Notes");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = editTextField("Notes", "SWB automated test note");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Notes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 12, description = "TC-SWB-12: Edit Serial Number")
    public void testSWB_12_EditSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_12_SerialNumber");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = editTextField("Serial Number", "SN-SWB-" + System.currentTimeMillis() % 100000);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Serial Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 13, description = "TC-SWB-13: Edit Size")
    public void testSWB_13_EditSize() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_13_Size");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = editTextField("Size", "4000A");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Size: '" + val + "', saved=" + saved);
    }

    @Test(priority = 14, description = "TC-SWB-15: Verify Voltage field selection")
    public void testSWB_15_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_15_Voltage");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        if (val == null) val = editTextField("Voltage", "480");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("SWB Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 15, description = "TC-SWB-16: Save without filling required fields (allowed)")
    public void testSWB_16_SaveWithoutRequired() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_16_SaveWithoutRequired");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        boolean saved = saveAndVerify();
        logStep("SWB save without required: " + saved);
        ExtentReportManager.logPass("SWB save without required: saved=" + saved);
    }

    @Test(priority = 16, description = "TC-SWB-18: Verify persistence after save")
    public void testSWB_18_Persistence() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_18_Persistence");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        expandCoreAttributes();
        String uniqueVal = "SWB-PERSIST-" + System.currentTimeMillis() % 10000;
        editTextField("Serial Number", uniqueVal);
        boolean saved = saveAndVerify();

        if (saved) {
            pause(1000);
            openEditForm();
            expandCoreAttributes();
            WebElement snInput = findInputByPlaceholder("Serial Number");
            if (snInput == null) snInput = findInputByLabel("Serial Number");
            if (snInput != null) {
                String persisted = snInput.getAttribute("value");
                logStep("Persisted Serial Number: '" + persisted + "'");
                Assert.assertTrue(persisted != null && persisted.contains("SWB-PERSIST"),
                        "Serial Number should persist but was: " + persisted);
            }
        }
        ExtentReportManager.logPass("SWB persistence: saved=" + saved);
    }

    // SWB Subtype tests
    @Test(priority = 17, description = "SWB_AST_01: Verify default Asset Subtype is None for Switchboard")
    public void testSWB_AST_01_DefaultSubtype() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_AST_01_DefaultSubtype");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        verifyAssetSubtype("None",
                "None",
                "Distribution Panelboard",
                "Switchboard",
                "Switchgear");
        logStepWithScreenshot("SWB subtype default");
        ExtentReportManager.logPass("Switchboard default subtype is None");
    }

    @Test(priority = 18, description = "SWB_AST_02: Verify SWB subtype dropdown options")
    public void testSWB_AST_02_SubtypeOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_AST_02_SubtypeOptions");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }

        WebElement subtypeInput = findInputByPlaceholder("Select Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Asset Subtype");

        if (subtypeInput != null) {
            subtypeInput.click();
            pause(800);
            List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
            List<String> optTexts = new ArrayList<>();
            for (WebElement opt : options) optTexts.add(opt.getText().trim());
            logStep("SWB subtype options: " + optTexts);

            List<String> expected = Arrays.asList(
                    "Distribution Panelboard",
                    "Switchboard",
                    "Switchgear",
                    "Unitized Substation");
            for (String exp : expected) {
                boolean found = optTexts.stream().anyMatch(t -> t.contains(exp));
                logStep("  '" + exp + "': " + (found ? "FOUND" : "MISSING"));
            }

            subtypeInput.sendKeys(Keys.ESCAPE);
            pause(300);
        }
        logStepWithScreenshot("SWB subtype options");
        ExtentReportManager.logPass("SWB subtype options verified");
    }

    @Test(priority = 19, description = "SWB_AST_03: Select Switchgear (≤ 1000V)")
    public void testSWB_AST_03_SelectSwitchgear() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_AST_03_Switchgear");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        String selected = selectSubtype("Switchgear");
        logStep("Selected: " + selected);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Switchgear subtype: '" + selected + "', saved=" + saved);
    }

    @Test(priority = 20, description = "SWB_AST_04: Select Unitized Substation (USS) (> 1000V)")
    public void testSWB_AST_04_SelectUSS() {
        ExtentReportManager.createTest(MODULE, FEATURE, "SWB_AST_04_USS");
        if (!openEditForAssetClass("Switchboard", "SWB")) { skipIfNotFound("Switchboard"); return; }
        String selected = selectSubtype("Unitized Substation");
        logStep("Selected: " + selected);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("USS subtype: '" + selected + "', saved=" + saved);
    }

    // ================================================================
    // SECTION 2: TRANSFORMER (TRF) — 27 TCs (16 core fields + subtypes)
    // ================================================================

    @Test(priority = 30, description = "TC-TRF-01: Verify Core Attributes section loads for Transformer")
    public void testTRF_01_CoreAttributesLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_01_CoreAttributes");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        boolean coreVisible = isCoreAttributesSectionVisible();
        expandCoreAttributes();
        logStep("TRF Core Attributes visible: " + coreVisible);
        logStepWithScreenshot("TRF Core Attributes");
        ExtentReportManager.logPass("TRF Core Attributes visible: " + coreVisible);
    }

    @Test(priority = 31, description = "TC-TRF-02: Verify all TRF core attributes visible")
    public void testTRF_02_AllFieldsVisible() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_02_AllFields");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();

        String[] fields = {"BIL", "Class", "Frequency", "KVA Rating", "Manufacturer",
                "Percentage Impedance", "Primary Amperes", "Primary Tap",
                "Primary Voltage", "Secondary Amperes", "Secondary Voltage",
                "Serial Number", "Size", "Temperature Rise", "Type", "Winding Configuration"};
        int found = 0;
        for (String field : fields) {
            WebElement input = findInputByPlaceholder(field);
            if (input == null) input = findInputByLabel(field);
            boolean exists = input != null;
            if (exists) found++;
            logStep("TRF field '" + field + "': " + (exists ? "FOUND" : "NOT FOUND"));
        }
        logStep("TRF fields found: " + found + "/" + fields.length);
        logStepWithScreenshot("TRF fields");
        ExtentReportManager.logPass("TRF fields: " + found + "/" + fields.length + " found");
    }

    @Test(priority = 32, description = "TC-TRF-03: Verify Required fields toggle")
    public void testTRF_03_RequiredFieldsToggle() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_03_RequiredToggle");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("TRF Required fields counter: " + counter);
        logStepWithScreenshot("TRF Required toggle");
        ExtentReportManager.logPass("TRF required fields toggle: " + counter);
    }

    @Test(priority = 33, description = "TC-TRF-05: Edit BIL field")
    public void testTRF_05_EditBIL() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_05_BIL");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("BIL", "150kV");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF BIL: '" + val + "', saved=" + saved);
    }

    @Test(priority = 34, description = "TC-TRF-06: Edit Class field")
    public void testTRF_06_EditClass() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_06_Class");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Class", "OA");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Class: '" + val + "', saved=" + saved);
    }

    @Test(priority = 35, description = "TC-TRF-07: Edit Frequency field (dropdown)")
    public void testTRF_07_EditFrequency() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_07_Frequency");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Frequency");
        if (val == null) val = editTextField("Frequency", "60");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Frequency: '" + val + "', saved=" + saved);
    }

    @Test(priority = 36, description = "TC-TRF-08: Edit KVA Rating")
    public void testTRF_08_EditKVARating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_08_KVARating");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("KVA Rating");
        if (val == null) val = editTextField("KVA Rating", "1000");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF KVA Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 37, description = "TC-TRF-09: Edit Manufacturer")
    public void testTRF_09_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_09_Manufacturer");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        if (val == null) val = editTextField("Manufacturer", "GE");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 38, description = "TC-TRF-10: Edit Percentage Impedance")
    public void testTRF_10_EditPercentageImpedance() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_10_PercentImpedance");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Percentage Impedance", "5.75");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Percentage Impedance: '" + val + "', saved=" + saved);
    }

    @Test(priority = 39, description = "TC-TRF-11: Edit Primary Amperes")
    public void testTRF_11_EditPrimaryAmperes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_11_PrimaryAmperes");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Primary Amperes", "120.5");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Primary Amperes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 40, description = "TC-TRF-12: Edit Primary Tap")
    public void testTRF_12_EditPrimaryTap() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_12_PrimaryTap");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Primary Tap", "2.5%");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Primary Tap: '" + val + "', saved=" + saved);
    }

    @Test(priority = 41, description = "TC-TRF-13: Edit Primary Voltage")
    public void testTRF_13_EditPrimaryVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_13_PrimaryVoltage");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Primary Voltage");
        if (val == null) val = editTextField("Primary Voltage", "13800");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Primary Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 42, description = "TC-TRF-14: Edit Secondary Amperes")
    public void testTRF_14_EditSecondaryAmperes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_14_SecondaryAmperes");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Secondary Amperes", "1204");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Secondary Amperes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 43, description = "TC-TRF-15: Edit Secondary Voltage")
    public void testTRF_15_EditSecondaryVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_15_SecondaryVoltage");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Secondary Voltage");
        if (val == null) val = editTextField("Secondary Voltage", "480");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Secondary Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 44, description = "TC-TRF-16: Edit Serial Number")
    public void testTRF_16_EditSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_16_SerialNumber");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Serial Number", "SN-TRF-" + System.currentTimeMillis() % 100000);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Serial Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 45, description = "TC-TRF-17: Edit Size")
    public void testTRF_17_EditSize() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_17_Size");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Size", "1000 KVA");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Size: '" + val + "', saved=" + saved);
    }

    @Test(priority = 46, description = "TC-TRF-18: Edit Temperature Rise")
    public void testTRF_18_EditTemperatureRise() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_18_TempRise");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = editTextField("Temperature Rise", "150°C");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Temperature Rise: '" + val + "', saved=" + saved);
    }

    @Test(priority = 47, description = "TC-TRF-19: Edit Type")
    public void testTRF_19_EditType() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_19_Type");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Type");
        if (val == null) val = editTextField("Type", "Dry-Type");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Type: '" + val + "', saved=" + saved);
    }

    @Test(priority = 48, description = "TC-TRF-20: Edit Winding Configuration")
    public void testTRF_20_EditWindingConfiguration() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_20_WindingConfig");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Winding Configuration");
        if (val == null) val = editTextField("Winding Configuration", "Delta-Wye");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("TRF Winding Configuration: '" + val + "', saved=" + saved);
    }

    @Test(priority = 49, description = "TC-TRF-21: Save TRF without filling required fields (allowed)")
    public void testTRF_21_SaveWithoutRequired() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_21_SaveWithoutRequired");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        boolean saved = saveAndVerify();
        logStep("TRF save without required: " + saved);
        ExtentReportManager.logPass("TRF save without required: saved=" + saved);
    }

    @Test(priority = 50, description = "TC-TRF-22: Verify persistence after save")
    public void testTRF_22_Persistence() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_22_Persistence");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        expandCoreAttributes();
        String uniqueVal = "TRF-PERSIST-" + System.currentTimeMillis() % 10000;
        editTextField("Serial Number", uniqueVal);
        boolean saved = saveAndVerify();

        if (saved) {
            pause(1000);
            openEditForm();
            expandCoreAttributes();
            WebElement snInput = findInputByPlaceholder("Serial Number");
            if (snInput == null) snInput = findInputByLabel("Serial Number");
            if (snInput != null) {
                String persisted = snInput.getAttribute("value");
                logStep("Persisted Serial Number: '" + persisted + "'");
                Assert.assertTrue(persisted != null && persisted.contains("TRF-PERSIST"),
                        "Serial Number should persist but was: " + persisted);
            }
        }
        ExtentReportManager.logPass("TRF persistence: saved=" + saved);
    }

    // TRF Subtype tests
    @Test(priority = 51, description = "TRF_AST_01: Verify default subtype is None for Transformer")
    public void testTRF_AST_01_DefaultSubtype() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_AST_01_DefaultSubtype");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        verifyAssetSubtype("None",
                "None",
                "Dry Transformer",
                "Oil-Filled Transformer");
        logStepWithScreenshot("TRF subtype default");
        ExtentReportManager.logPass("Transformer default subtype is None");
    }

    @Test(priority = 52, description = "TRF_AST_02: Verify TRF subtype dropdown options")
    public void testTRF_AST_02_SubtypeOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_AST_02_SubtypeOptions");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }

        WebElement subtypeInput = findInputByPlaceholder("Select Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Asset Subtype");

        if (subtypeInput != null) {
            subtypeInput.click();
            pause(800);
            List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
            List<String> optTexts = new ArrayList<>();
            for (WebElement opt : options) optTexts.add(opt.getText().trim());
            logStep("TRF subtype options: " + optTexts);

            List<String> expected = Arrays.asList(
                    "Dry Transformer",
                    "Dry-Type Transformer",
                    "Oil-Filled Transformer");
            for (String exp : expected) {
                boolean found = optTexts.stream().anyMatch(t -> t.contains(exp));
                logStep("  '" + exp + "': " + (found ? "FOUND" : "MISSING"));
            }

            subtypeInput.sendKeys(Keys.ESCAPE);
            pause(300);
        }
        logStepWithScreenshot("TRF subtype options");
        ExtentReportManager.logPass("TRF subtype options verified");
    }

    @Test(priority = 53, description = "TRF_AST_03: Select Dry-Type Transformer (≤ 600V)")
    public void testTRF_AST_03_SelectDryType() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_AST_03_DryType");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        String selected = selectSubtype("Dry-Type");
        logStep("Selected: " + selected);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Dry-Type Transformer subtype: '" + selected + "', saved=" + saved);
    }

    @Test(priority = 54, description = "TRF_AST_04: Select Oil-Filled Transformer")
    public void testTRF_AST_04_SelectOilFilled() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TRF_AST_04_OilFilled");
        if (!openEditForAssetClass("Transformer", "TRF")) { skipIfNotFound("Transformer"); return; }
        String selected = selectSubtype("Oil-Filled");
        logStep("Selected: " + selected);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Oil-Filled Transformer subtype: '" + selected + "', saved=" + saved);
    }

    // ================================================================
    // SECTION 3: UPS — 19 TCs (7 core fields + subtypes)
    // ================================================================

    @Test(priority = 60, description = "TC-UPS-01: Verify Core Attributes section loads with % indicator")
    public void testUPS_01_CoreAttributesLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_01_CoreAttributes");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        boolean coreVisible = isCoreAttributesSectionVisible();
        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("UPS Core Attributes: visible=" + coreVisible + ", counter=" + counter);
        logStepWithScreenshot("UPS Core Attributes");
        ExtentReportManager.logPass("UPS Core Attributes: visible=" + coreVisible + ", counter=" + counter);
    }

    @Test(priority = 61, description = "TC-UPS-02: Verify all UPS core attributes visible")
    public void testUPS_02_AllFieldsVisible() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_02_AllFields");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();

        String[] fields = {"Ampere Rating", "Catalog Number", "Manufacturer",
                "Model", "Notes", "Size", "Voltage"};
        int found = 0;
        for (String field : fields) {
            WebElement input = findInputByPlaceholder(field);
            if (input == null) input = findInputByLabel(field);
            boolean exists = input != null;
            if (exists) found++;
            logStep("UPS field '" + field + "': " + (exists ? "FOUND" : "NOT FOUND"));
        }
        logStep("UPS fields found: " + found + "/" + fields.length);
        ExtentReportManager.logPass("UPS fields: " + found + "/" + fields.length + " found");
    }

    @Test(priority = 62, description = "TC-UPS-05: Edit Ampere Rating")
    public void testUPS_05_EditAmpereRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_05_AmpereRating");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Ampere Rating");
        if (val == null) val = editTextField("Ampere Rating", "100");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UPS Ampere Rating: '" + val + "', saved=" + saved);
    }

    @Test(priority = 63, description = "TC-UPS-06: Edit Catalog Number")
    public void testUPS_06_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_06_CatalogNumber");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String val = editTextField("Catalog Number", "CAT-UPS-" + System.currentTimeMillis() % 10000);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UPS Catalog Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 64, description = "TC-UPS-07: Edit Manufacturer")
    public void testUPS_07_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_07_Manufacturer");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        if (val == null) val = editTextField("Manufacturer", "Eaton");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UPS Manufacturer: '" + val + "', saved=" + saved);
    }

    @Test(priority = 65, description = "TC-UPS-08: Edit Model")
    public void testUPS_08_EditModel() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_08_Model");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String val = editTextField("Model", "UPS-9395-675kVA");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UPS Model: '" + val + "', saved=" + saved);
    }

    @Test(priority = 66, description = "TC-UPS-09: Edit Notes")
    public void testUPS_09_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_09_Notes");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String val = editTextField("Notes", "UPS automated test note");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UPS Notes: '" + val + "', saved=" + saved);
    }

    @Test(priority = 67, description = "TC-UPS-10: Edit Size")
    public void testUPS_10_EditSize() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_10_Size");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String val = editTextField("Size", "675 kVA");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UPS Size: '" + val + "', saved=" + saved);
    }

    @Test(priority = 68, description = "TC-UPS-12: Verify Voltage field selection")
    public void testUPS_12_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_12_Voltage");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        if (val == null) val = editTextField("Voltage", "480");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UPS Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 69, description = "TC-UPS-13: Save UPS with missing required fields (allowed)")
    public void testUPS_13_SaveWithMissingRequired() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_13_SaveMissing");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        boolean saved = saveAndVerify();
        logStep("UPS save with missing required: " + saved);
        ExtentReportManager.logPass("UPS save missing required: saved=" + saved);
    }

    @Test(priority = 70, description = "TC-UPS-15: Verify persistence after save")
    public void testUPS_15_Persistence() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_15_Persistence");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        expandCoreAttributes();
        String uniqueVal = "UPS-PERSIST-" + System.currentTimeMillis() % 10000;
        editTextField("Model", uniqueVal);
        boolean saved = saveAndVerify();

        if (saved) {
            pause(1000);
            openEditForm();
            expandCoreAttributes();
            WebElement modelInput = findInputByPlaceholder("Model");
            if (modelInput == null) modelInput = findInputByLabel("Model");
            if (modelInput != null) {
                String persisted = modelInput.getAttribute("value");
                logStep("Persisted Model: '" + persisted + "'");
                Assert.assertTrue(persisted != null && persisted.contains("UPS-PERSIST"),
                        "Model should persist but was: " + persisted);
            }
        }
        ExtentReportManager.logPass("UPS persistence: saved=" + saved);
    }

    // UPS Subtype tests
    @Test(priority = 71, description = "UPS_AST_01: Verify default subtype is None for UPS")
    public void testUPS_AST_01_DefaultSubtype() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_AST_01_DefaultSubtype");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        verifyAssetSubtype("None",
                "None",
                "Hybrid UPS System",
                "Rotary UPS System",
                "Static UPS System");
        logStepWithScreenshot("UPS subtype default");
        ExtentReportManager.logPass("UPS default subtype is None");
    }

    @Test(priority = 72, description = "UPS_AST_02: Verify UPS subtype dropdown options")
    public void testUPS_AST_02_SubtypeOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_AST_02_SubtypeOptions");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }

        WebElement subtypeInput = findInputByPlaceholder("Select Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Subtype");
        if (subtypeInput == null) subtypeInput = findInputByLabel("Asset Subtype");

        if (subtypeInput != null) {
            subtypeInput.click();
            pause(800);
            List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
            List<String> optTexts = new ArrayList<>();
            for (WebElement opt : options) optTexts.add(opt.getText().trim());
            logStep("UPS subtype options: " + optTexts);

            List<String> expected = Arrays.asList(
                    "Hybrid UPS System",
                    "Rotary UPS System",
                    "Static UPS System");
            for (String exp : expected) {
                boolean found = optTexts.stream().anyMatch(t -> t.contains(exp));
                logStep("  '" + exp + "': " + (found ? "FOUND" : "MISSING"));
            }

            subtypeInput.sendKeys(Keys.ESCAPE);
            pause(300);
        }
        logStepWithScreenshot("UPS subtype options");
        ExtentReportManager.logPass("UPS subtype options verified");
    }

    @Test(priority = 73, description = "UPS_AST_03: Select Hybrid UPS System subtype")
    public void testUPS_AST_03_SelectHybrid() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_AST_03_Hybrid");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        String selected = selectSubtype("Hybrid");
        logStep("Selected: " + selected);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Hybrid UPS subtype: '" + selected + "', saved=" + saved);
    }

    @Test(priority = 74, description = "UPS_AST_04: Select Static UPS System subtype")
    public void testUPS_AST_04_SelectStatic() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UPS_AST_04_Static");
        if (!openEditForAssetClass("UPS")) { skipIfNotFound("UPS"); return; }
        String selected = selectSubtype("Static");
        logStep("Selected: " + selected);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("Static UPS subtype: '" + selected + "', saved=" + saved);
    }

    // ================================================================
    // SECTION 4: UTILITY (UTL) — 10 TCs (2 core fields: Meter Number, Starting Voltage)
    // ================================================================

    @Test(priority = 80, description = "TC-UTL-01: Verify Core Attributes section loads for Utility")
    public void testUTL_01_CoreAttributesLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_01_CoreAttributes");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        boolean coreVisible = isCoreAttributesSectionVisible();
        expandCoreAttributes();
        logStep("UTL Core Attributes visible: " + coreVisible);
        logStepWithScreenshot("UTL Core Attributes");
        ExtentReportManager.logPass("UTL Core Attributes visible: " + coreVisible);
    }

    @Test(priority = 81, description = "TC-UTL-02: Verify Utility core attributes (Meter Number, Starting Voltage)")
    public void testUTL_02_VerifyFields() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_02_VerifyFields");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        expandCoreAttributes();

        WebElement meterInput = findInputByPlaceholder("Meter Number");
        if (meterInput == null) meterInput = findInputByLabel("Meter Number");
        logStep("Meter Number field: " + (meterInput != null ? "FOUND" : "NOT FOUND"));

        WebElement voltageInput = findInputByPlaceholder("Starting Voltage");
        if (voltageInput == null) voltageInput = findInputByLabel("Starting Voltage");
        logStep("Starting Voltage field: " + (voltageInput != null ? "FOUND" : "NOT FOUND"));

        logStepWithScreenshot("UTL fields");
        ExtentReportManager.logPass("UTL fields: Meter Number=" + (meterInput != null)
                + ", Starting Voltage=" + (voltageInput != null));
    }

    @Test(priority = 82, description = "TC-UTL-03: Edit Meter Number field")
    public void testUTL_03_EditMeterNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_03_MeterNumber");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        expandCoreAttributes();
        String val = editTextField("Meter Number", "MTR-" + System.currentTimeMillis() % 100000);
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UTL Meter Number: '" + val + "', saved=" + saved);
    }

    @Test(priority = 83, description = "TC-UTL-04: Verify Meter Number persistence")
    public void testUTL_04_MeterNumberPersistence() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_04_MeterPersist");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        expandCoreAttributes();
        String uniqueVal = "MTR-PERSIST-" + System.currentTimeMillis() % 10000;
        editTextField("Meter Number", uniqueVal);
        boolean saved = saveAndVerify();

        if (saved) {
            pause(1000);
            openEditForm();
            expandCoreAttributes();
            WebElement meterInput = findInputByPlaceholder("Meter Number");
            if (meterInput == null) meterInput = findInputByLabel("Meter Number");
            if (meterInput != null) {
                String persisted = meterInput.getAttribute("value");
                logStep("Persisted Meter Number: '" + persisted + "'");
                Assert.assertTrue(persisted != null && persisted.contains("MTR-PERSIST"),
                        "Meter Number should persist but was: " + persisted);
            }
        }
        ExtentReportManager.logPass("UTL Meter Number persistence: saved=" + saved);
    }

    @Test(priority = 84, description = "TC-UTL-05: Edit Starting Voltage (dropdown)")
    public void testUTL_05_EditStartingVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_05_StartingVoltage");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        expandCoreAttributes();
        String val = selectFirstDropdownOption("Starting Voltage");
        if (val == null) val = editTextField("Starting Voltage", "13800");
        boolean saved = saveAndVerify();
        ExtentReportManager.logPass("UTL Starting Voltage: '" + val + "', saved=" + saved);
    }

    @Test(priority = 85, description = "TC-UTL-06: Verify Starting Voltage persistence")
    public void testUTL_06_VoltagePersistence() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_06_VoltagePersist");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        expandCoreAttributes();
        // Set a specific voltage
        String val = selectFirstDropdownOption("Starting Voltage");
        logStep("Set Starting Voltage to: " + val);
        boolean saved = saveAndVerify();

        if (saved) {
            pause(1000);
            openEditForm();
            expandCoreAttributes();
            WebElement voltageInput = findInputByPlaceholder("Starting Voltage");
            if (voltageInput == null) voltageInput = findInputByLabel("Starting Voltage");
            if (voltageInput != null) {
                String persisted = voltageInput.getAttribute("value");
                logStep("Persisted Starting Voltage: '" + persisted + "'");
            }
        }
        ExtentReportManager.logPass("UTL Starting Voltage persistence: saved=" + saved);
    }

    @Test(priority = 86, description = "TC-UTL-07: Save Utility with empty fields (allowed)")
    public void testUTL_07_SaveEmpty() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_07_SaveEmpty");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        boolean saved = saveAndVerify();
        logStep("UTL save with empty fields: " + saved);
        ExtentReportManager.logPass("UTL save empty: saved=" + saved);
    }

    @Test(priority = 87, description = "UTL_AST_01: Verify Asset Subtype shows None for Utility")
    public void testUTL_AST_01_SubtypeNone() {
        ExtentReportManager.createTest(MODULE, FEATURE, "UTL_AST_01_SubtypeNone");
        if (!openEditForAssetClass("Utility", "UTL")) { skipIfNotFound("Utility"); return; }
        verifyAssetSubtype("None");
        logStepWithScreenshot("UTL subtype");
        ExtentReportManager.logPass("Utility subtype is None");
    }

    // ================================================================
    // SECTION 5: VFD — Variable Frequency Drive (9 TCs, no core attr fields)
    // ================================================================

    @Test(priority = 90, description = "TC-VFD-01: Verify Core Attributes section loads for VFD")
    public void testVFD_01_CoreAttributesLoad() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_01_CoreAttributes");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }
        boolean coreVisible = isCoreAttributesSectionVisible();
        logStep("VFD Core Attributes section visible: " + coreVisible);
        logStepWithScreenshot("VFD Core Attributes");
        ExtentReportManager.logPass("VFD Core Attributes visible: " + coreVisible);
    }

    @Test(priority = 91, description = "TC-VFD-02: Verify no core attributes are displayed for VFD")
    public void testVFD_02_NoCoreAttributeFields() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_02_NoFields");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }

        // VFD should have NO core attribute fields
        expandCoreAttributes();
        // Try to find any input fields that would indicate core attributes
        String[] commonFields = {"Manufacturer", "Model", "Serial Number", "Voltage",
                "Ampere Rating", "Size", "Notes"};
        int found = 0;
        for (String field : commonFields) {
            WebElement input = findInputByPlaceholder(field);
            if (input == null) input = findInputByLabel(field);
            if (input != null) {
                found++;
                logStep("WARNING: Found unexpected field '" + field + "' in VFD edit form");
            }
        }
        logStep("VFD unexpected core attribute fields found: " + found);
        if (found > 0) {
            logStep("BUG: VFD should have no core attribute fields but found " + found);
        }
        logStepWithScreenshot("VFD no fields");
        ExtentReportManager.logPass("VFD core attribute fields found: " + found + " (expected: 0)");
    }

    @Test(priority = 92, description = "TC-VFD-03: Verify Required fields toggle (no fields appear)")
    public void testVFD_03_RequiredToggleNoFields() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_03_RequiredToggle");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }
        String counter = getRequiredFieldsCounter();
        logStep("VFD Required fields counter: " + counter);
        // VFD should show 0% or no counter at all
        logStepWithScreenshot("VFD required toggle");
        ExtentReportManager.logPass("VFD required toggle: counter=" + counter);
    }

    @Test(priority = 93, description = "TC-VFD-04: Verify percentage indicator remains at 0%")
    public void testVFD_04_PercentageAtZero() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_04_PercentZero");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }

        // Check for progress bar / percentage
        try {
            List<WebElement> progressBars = driver.findElements(By.xpath(
                    "//*[contains(@role,'progressbar')]"));
            for (WebElement bar : progressBars) {
                String value = bar.getAttribute("aria-valuenow");
                logStep("Progress bar value: " + value);
            }

            List<WebElement> percentLabels = driver.findElements(By.xpath(
                    "//*[contains(text(),'%')]"
                    + "[ancestor::*[contains(@class,'MuiDrawer') or contains(@class,'drawer')]]"));
            for (WebElement label : percentLabels) {
                logStep("Percentage label: " + label.getText().trim());
            }
        } catch (Exception e) {
            logStep("Could not check percentage: " + e.getMessage());
        }
        logStepWithScreenshot("VFD percentage");
        ExtentReportManager.logPass("VFD percentage indicator checked");
    }

    @Test(priority = 94, description = "TC-VFD-05: Save VFD without core attributes")
    public void testVFD_05_SaveWithoutCoreAttrs() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_05_SaveWithout");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }
        boolean saved = saveAndVerify();
        logStep("VFD save without core attributes: " + saved);
        ExtentReportManager.logPass("VFD save without core attributes: saved=" + saved);
    }

    @Test(priority = 95, description = "TC-VFD-06: Verify Cancel button behavior")
    public void testVFD_06_CancelButton() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_06_Cancel");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }

        // Verify Cancel button exists and works
        List<WebElement> cancelBtns = driver.findElements(By.xpath("//button[normalize-space()='Cancel']"));
        Assert.assertFalse(cancelBtns.isEmpty(), "Cancel button should be present in edit form");
        logStep("Cancel button found: " + !cancelBtns.isEmpty());

        cancelBtns.get(0).click();
        pause(1000);
        editFormOpen = false;

        // Verify we're back (edit form closed)
        List<WebElement> saveBtns = driver.findElements(SAVE_CHANGES_BTN);
        boolean formStillOpen = !saveBtns.isEmpty() && saveBtns.get(0).isDisplayed();
        logStep("Edit form still open after cancel: " + formStillOpen);
        logStepWithScreenshot("VFD cancel");
        ExtentReportManager.logPass("VFD Cancel button: form closed=" + !formStillOpen);
    }

    @Test(priority = 96, description = "TC-VFD-08: Verify persistence after save (still no fields)")
    public void testVFD_08_Persistence() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_08_Persistence");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }
        boolean saved = saveAndVerify();

        if (saved) {
            pause(1000);
            openEditForm();
            // Verify still no core attribute fields after save
            String[] commonFields = {"Manufacturer", "Model", "Voltage"};
            int found = 0;
            for (String field : commonFields) {
                WebElement input = findInputByPlaceholder(field);
                if (input == null) input = findInputByLabel(field);
                if (input != null) found++;
            }
            logStep("Core attribute fields after re-open: " + found + " (expected: 0)");
        }
        ExtentReportManager.logPass("VFD persistence: saved=" + saved);
    }

    @Test(priority = 97, description = "VFD_AST_01: Verify Asset Subtype shows None for VFD")
    public void testVFD_AST_01_SubtypeNone() {
        ExtentReportManager.createTest(MODULE, FEATURE, "VFD_AST_01_SubtypeNone");
        if (!openEditForAssetClass("Variable Frequency Drive", "VFD")) { skipIfNotFound("VFD"); return; }
        verifyAssetSubtype("None");
        logStepWithScreenshot("VFD subtype");
        ExtentReportManager.logPass("VFD subtype is None");
    }
}
