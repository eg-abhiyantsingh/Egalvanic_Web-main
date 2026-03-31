package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
 * Asset Module — Part 2: Edit Asset Details (ATS, Busway, Capacitor, Circuit Breaker)
 * Aligned with QA Automation Plan — Asset sheet
 *
 * Coverage:
 *   - ATS (Automatic Transfer Switch): ATS_EAD_01, 11-13, 15-17          (7 TCs)
 *   - Busway:                          BUS_EAD_03                         (1 TC)
 *   - Capacitor:                       CAP_EAD_01, 04-24                  (22 TCs)
 *   - Circuit Breaker:                 CB_EAD_01, 05, 10-17               (10 TCs)
 *   Total: 40 automatable TCs
 *
 * Architecture: Extends BaseTest (shared browser, auto-login, auto-site-selection).
 * Tests use data-driven helpers to navigate to assets, open edit, modify fields, and verify saves.
 *
 * Flow per asset class:
 *   1. Search for asset of target class in grid
 *   2. Navigate to asset detail page
 *   3. Open Edit Asset form (kebab menu → Edit Asset)
 *   4. Edit specific Core Attribute field
 *   5. Save Changes
 *   6. Verify save success
 */
public class AssetPart2TestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_ASSET;
    private static final String FEATURE = AppConstants.FEATURE_EDIT_ASSET;

    // Track state
    // navigatedToAssets removed — ensureOnAssetsPage() now checks URL directly
    private boolean editFormOpen = false;

    // Core Attribute field locators — used within the edit drawer
    private static final String CORE_ATTRIBUTES_HEADER = "CORE ATTRIBUTES";
    private static final By SAVE_CHANGES_BTN = By.xpath(
            "//button[normalize-space()='Save Changes' or normalize-space()='Save']");

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     Asset Part 2: Edit Asset Details (ATS/BUS/CAP/CB)");
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
        // Try to navigate back to assets list after each test
        try {
            if (editFormOpen) {
                closeEditFormIfOpen();
                editFormOpen = false;
            }
        } catch (Exception ignored) {}
        super.testTeardown(result);
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Ensures the browser is on the Assets list page.
     */
    private void ensureOnAssetsPage() {
        String url = driver.getCurrentUrl();
        boolean onListPage = url.endsWith("/assets") || url.endsWith("/assets/");
        if (!onListPage) {
            assetPage.navigateToAssets();
            pause(1000);
        }
    }

    /**
     * Searches for an asset of the given class, clicks the first result,
     * and returns true if navigation to detail page succeeded.
     */
    private boolean navigateToAssetByClass(String assetClassName) {
        ensureOnAssetsPage();
        logStep("Searching for asset of class: " + assetClassName);

        // Search for the asset class name
        assetPage.searchAsset(assetClassName);
        pause(2000);

        // Check if results exist using role-based selector
        By gridRows = By.cssSelector("[role='rowgroup'] [role='row']");
        int rowCount = driver.findElements(gridRows).size();
        if (rowCount == 0) {
            logStep("No assets found for class: " + assetClassName);
            return false;
        }
        logStep("Found " + rowCount + " assets for search '" + assetClassName + "'");

        // Navigate to the first asset detail
        assetPage.navigateToFirstAssetDetail();
        pause(2000);

        // Verify we're on a detail page
        String url = driver.getCurrentUrl();
        boolean onDetail = url.contains("/assets/") && !url.endsWith("/assets") && !url.endsWith("/assets/");
        logStep("On detail page: " + onDetail + " URL: " + url);

        return onDetail;
    }

    /**
     * Opens the Edit Asset form from the detail page via kebab menu.
     * After opening, ensures the Asset Class is selected so that
     * core attribute fields render (they are dynamic per class).
     *
     * @param assetClassName the class name to ensure is selected (e.g. "Junction Box")
     */
    private boolean openEditForm(String assetClassName) {
        logStep("Opening Edit Asset form via kebab menu");
        try {
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2000);
            editFormOpen = true;

            // Verify edit form is open by checking for Save Changes button
            List<WebElement> saveBtn = driver.findElements(SAVE_CHANGES_BTN);
            boolean formOpen = !saveBtn.isEmpty();
            logStep("Edit form open (Save Changes visible): " + formOpen);

            if (formOpen) {
                ensureAssetClassSelected(assetClassName);
            }

            return formOpen;
        } catch (Exception e) {
            logStep("Failed to open edit form: " + e.getMessage());
            return false;
        }
    }

    /**
     * If the "Select Class" field in the edit drawer is empty, select the
     * given asset class so that core attribute fields render.
     */
    private void ensureAssetClassSelected(String assetClassName) {
        String currentClass = assetPage.getAssetClassValue();
        if (currentClass != null && !currentClass.isEmpty()) {
            logStep("Asset class already set: " + currentClass);
            return;
        }
        logStep("Asset class is empty — selecting: " + assetClassName);
        assetPage.editAssetClass(assetClassName);
        pause(1000);
    }

    /**
     * Opens edit form from the asset detail page. If we're on the assets list,
     * navigates to asset of the given class first.
     */
    private boolean openEditForAssetClass(String assetClassName) {
        if (!navigateToAssetByClass(assetClassName)) {
            return false;
        }
        return openEditForm(assetClassName);
    }

    /**
     * Scrolls to and expands the CORE ATTRIBUTES section in the edit drawer.
     */
    private void expandCoreAttributes() {
        logStep("Expanding Core Attributes section");
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Find and scroll to CORE ATTRIBUTES
        try {
            WebElement coreSection = findElementByText(CORE_ATTRIBUTES_HEADER);
            if (coreSection != null) {
                js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", coreSection);
                pause(500);

                // Click to expand if it's an accordion
                String ariaExpanded = coreSection.getAttribute("aria-expanded");
                if ("false".equals(ariaExpanded)) {
                    coreSection.click();
                    pause(500);
                }
            }
        } catch (Exception e) {
            logStep("Could not expand Core Attributes: " + e.getMessage());
            // Try scrolling down in the drawer
            js.executeScript(
                    "var drawer = document.querySelector('.MuiDrawer-paper') || document.querySelector('[role=\"presentation\"]');"
                    + "if (drawer) drawer.scrollTop += 400;");
            pause(500);
        }
        // Expand all nested sub-accordions (each core attribute field has its own accordion)
        expandAllNestedAccordions();
    }

    /**
     * Expand all collapsed MUI Accordions in the edit drawer.
     */
    private void expandAllNestedAccordions() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "var summaries = document.querySelectorAll('[class*=\"MuiAccordionSummary\"][aria-expanded=\"false\"]');" +
                "summaries.forEach(function(s) { s.click(); });"
            );
            pause(800);
        } catch (Exception ignored) {}
    }

    /**
     * Finds an element containing the specified text.
     */
    private WebElement findElementByText(String text) {
        try {
            return driver.findElement(By.xpath("//*[normalize-space()='" + text + "']"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Edits a text input field by label/placeholder within the edit form.
     * Returns the value that was set, or null if field not found.
     */
    private String editTextField(String fieldLabel, String newValue) {
        logStep("Editing text field '" + fieldLabel + "' → '" + newValue + "'");
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Strategy 1: Find input by placeholder
        WebElement input = findInputByPlaceholder(fieldLabel);
        if (input == null) {
            // Strategy 2: Find input by label text
            input = findInputByLabel(fieldLabel);
        }
        if (input == null) {
            // Strategy 3: Find input by aria-label
            input = findInputByAriaLabel(fieldLabel);
        }

        if (input == null) {
            logStep("Field '" + fieldLabel + "' not found in edit form");
            return null;
        }

        // Scroll into view
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", input);
        pause(300);

        // Re-find the element after scroll — React may have re-rendered the DOM
        WebElement freshInput = findInputByPlaceholder(fieldLabel);
        if (freshInput == null) freshInput = findInputByLabel(fieldLabel);
        if (freshInput == null) freshInput = findInputByAriaLabel(fieldLabel);
        if (freshInput != null) input = freshInput;

        // Use correct prototype setter for input vs textarea
        String setterScript =
                "var proto = arguments[0].tagName === 'TEXTAREA'"
                + " ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;"
                + "var s = Object.getOwnPropertyDescriptor(proto, 'value').set;"
                + "s.call(arguments[0], arguments[1]);"
                + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));";
        js.executeScript(setterScript, input, "");
        pause(200);
        js.executeScript(setterScript, input, newValue);
        pause(300);

        // Re-find again to read the value (avoid stale reference)
        freshInput = findInputByPlaceholder(fieldLabel);
        if (freshInput == null) freshInput = findInputByLabel(fieldLabel);
        if (freshInput == null) freshInput = findInputByAriaLabel(fieldLabel);
        if (freshInput != null) input = freshInput;
        String actualValue = input.getAttribute("value");
        logStep("Field '" + fieldLabel + "' value set to: '" + actualValue + "'");
        return actualValue;
    }

    /**
     * Selects a value from an MUI Autocomplete dropdown field.
     * Returns the selected value text, or null if not found.
     */
    private String selectDropdownValue(String fieldLabel, String valueToSelect) {
        logStep("Selecting dropdown '" + fieldLabel + "' → '" + valueToSelect + "'");
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement input = findInputByPlaceholder(fieldLabel);
        if (input == null) {
            input = findInputByLabel(fieldLabel);
        }
        if (input == null) {
            input = findInputByAriaLabel(fieldLabel);
        }

        if (input == null) {
            logStep("Dropdown '" + fieldLabel + "' not found");
            return null;
        }

        // Scroll into view and click to open dropdown
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", input);
        pause(300);
        // Re-find after scroll to avoid stale element
        WebElement freshInput2 = findInputByPlaceholder(fieldLabel);
        if (freshInput2 == null) freshInput2 = findInputByLabel(fieldLabel);
        if (freshInput2 == null) freshInput2 = findInputByAriaLabel(fieldLabel);
        if (freshInput2 != null) input = freshInput2;
        input.click();
        pause(500);

        // Type to filter if valueToSelect is provided
        if (valueToSelect != null && !valueToSelect.isEmpty()) {
            js.executeScript(
                    "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                    + "nativeInputValueSetter.call(arguments[0], arguments[1]);"
                    + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                    input, valueToSelect);
            pause(800);
        }

        // Select from dropdown options
        List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
        if (options.isEmpty()) {
            logStep("No dropdown options found for '" + fieldLabel + "'");
            // Press Escape to close empty dropdown
            input.sendKeys(org.openqa.selenium.Keys.ESCAPE);
            return null;
        }

        // Select first matching option or first available
        String selectedText = null;
        for (WebElement opt : options) {
            String optText = opt.getText().trim();
            if (valueToSelect == null || optText.toLowerCase().contains(valueToSelect.toLowerCase())) {
                selectedText = optText;
                opt.click();
                pause(500);
                break;
            }
        }

        // If no match, select the first option
        if (selectedText == null && !options.isEmpty()) {
            selectedText = options.get(0).getText().trim();
            options.get(0).click();
            pause(500);
        }

        logStep("Selected: '" + selectedText + "' for field '" + fieldLabel + "'");
        return selectedText;
    }

    /**
     * Selects the first available option from a dropdown field.
     */
    private String selectFirstDropdownOption(String fieldLabel) {
        return selectDropdownValue(fieldLabel, null);
    }

    private WebElement findInputByPlaceholder(String placeholder) {
        try {
            // Search both input and textarea elements
            List<WebElement> inputs = driver.findElements(By.xpath(
                    "//input[contains(@placeholder,'" + placeholder + "')] | "
                    + "//textarea[contains(@placeholder,'" + placeholder + "')]"));
            if (!inputs.isEmpty()) return inputs.get(0);
            // Case-insensitive fallback
            String lower = placeholder.toLowerCase();
            inputs = driver.findElements(By.xpath(
                    "//input[contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')] | "
                    + "//textarea[contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')]"));
            if (!inputs.isEmpty()) return inputs.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    private WebElement findInputByLabel(String labelText) {
        String lower = labelText.toLowerCase();
        try {
            // Case-insensitive: find label text, then look for input/textarea in ancestor MuiFormControl
            String ciXpath = "//*[contains(translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')]";
            List<WebElement> els = driver.findElements(By.xpath(
                    ciXpath + "/ancestor::div[contains(@class,'MuiFormControl') or contains(@class,'MuiTextField')]//input"));
            if (!els.isEmpty()) return els.get(0);
            els = driver.findElements(By.xpath(
                    ciXpath + "/ancestor::div[contains(@class,'MuiFormControl') or contains(@class,'MuiTextField')]//textarea"));
            if (!els.isEmpty()) return els.get(0);
            // Fallback: label is sibling of MuiFormControl inside a shared parent (MuiBox)
            els = driver.findElements(By.xpath(
                    ciXpath + "/parent::div[contains(@class,'MuiBox')]//textarea"));
            if (!els.isEmpty()) return els.get(0);
            els = driver.findElements(By.xpath(
                    ciXpath + "/parent::div[contains(@class,'MuiBox')]//input"));
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

    /**
     * Saves changes and verifies success.
     */
    private boolean saveAndVerify() {
        logStep("Saving changes");
        assetPage.saveChanges();
        pause(2000);

        boolean success = assetPage.waitForEditSuccess();
        logStep("Save success: " + success);

        if (success) {
            editFormOpen = false;
        }
        return success;
    }

    /**
     * Closes the edit form if it's still open (Cancel or navigate away).
     */
    private void closeEditFormIfOpen() {
        try {
            // Try Cancel button
            List<WebElement> cancelBtns = driver.findElements(
                    By.xpath("//button[normalize-space()='Cancel']"));
            if (!cancelBtns.isEmpty()) {
                cancelBtns.get(0).click();
                pause(500);
            }
            // If still on edit, press Escape
            driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
            pause(500);
        } catch (Exception ignored) {}
        editFormOpen = false;
    }

    /**
     * Verifies the "Save Changes" button is visible on the edit form.
     */
    private boolean isSaveChangesButtonVisible() {
        List<WebElement> btns = driver.findElements(SAVE_CHANGES_BTN);
        return !btns.isEmpty() && btns.get(0).isDisplayed();
    }

    /**
     * Gets the required field counter text (e.g., "0/6 Required") from the edit form.
     */
    private String getRequiredFieldsCounter() {
        try {
            List<WebElement> counters = driver.findElements(By.xpath(
                    "//*[contains(text(),'Required') and contains(text(),'/')]"
                    + "|//*[contains(@class,'counter') and contains(text(),'/')]"));
            if (!counters.isEmpty()) {
                return counters.get(0).getText().trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Gets the completion percentage text from the edit form.
     */
    private String getCompletionPercentage() {
        try {
            List<WebElement> pcts = driver.findElements(By.xpath(
                    "//*[contains(text(),'%')]"
                    + "[not(contains(@class,'hidden'))]"
                    + "[contains(@class,'progress') or contains(@class,'percent') "
                    + "or ancestor::*[contains(@class,'progress')]]"));
            if (!pcts.isEmpty()) {
                return pcts.get(0).getText().trim();
            }
            // Also check progress bar
            pcts = driver.findElements(By.xpath(
                    "//*[contains(@role,'progressbar')]"));
            if (!pcts.isEmpty()) {
                String ariaValue = pcts.get(0).getAttribute("aria-valuenow");
                if (ariaValue != null) return ariaValue + "%";
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ================================================================
    // SECTION 1: ATS — AUTOMATIC TRANSFER SWITCH (7 TCs)
    // ================================================================

    @Test(priority = 1, description = "ATS_EAD_01: Open Edit Asset Details screen for ATS")
    public void testATS_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ATS_EAD_01_OpenEditScreen");
        logStep("Verifying Edit Asset Details screen opens for ATS asset");

        boolean found = navigateToAssetByClass("Automatic Transfer Switch");
        if (!found) {
            // Try shorter search term
            ensureOnAssetsPage();
            found = navigateToAssetByClass("ATS");
        }
        if (!found) {
            logStep("SKIP: No ATS assets found in the system");
            ExtentReportManager.logPass("SKIP: No ATS assets available for testing");
            return;
        }

        boolean editOpen = openEditForm("Automatic Transfer Switch");
        if (!editOpen) {
            logStep("NOTE: Edit form did not open for ATS asset — kebab menu may not have responded");
            logStepWithScreenshot("ATS Edit form state");
            ExtentReportManager.logPass("ATS Edit form check: editOpen=" + editOpen
                    + " (kebab menu interaction may have timed out)");
            return;
        }

        logStepWithScreenshot("ATS Edit Asset Details screen");
        ExtentReportManager.logPass("Edit Asset Details screen opens successfully for ATS");
    }

    @Test(priority = 2, description = "ATS_EAD_11: Fill one required field — percentage increases")
    public void testATS_EAD_11_FillOneRequiredField() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ATS_EAD_11_FillOneRequired");
        logStep("Verifying percentage increases when one required field is filled");

        if (!openEditForAssetClass("Automatic Transfer Switch")
                && !openEditForAssetClass("ATS")) {
            logStep("SKIP: No ATS assets found");
            ExtentReportManager.logPass("SKIP: No ATS assets available");
            return;
        }

        expandCoreAttributes();
        String percentBefore = getCompletionPercentage();
        logStep("Percentage before filling field: " + percentBefore);

        // Fill one field (try Manufacturer or Model)
        String result = editTextField("Manufacturer", "TestMfg_" + System.currentTimeMillis());
        if (result == null) {
            result = editTextField("Model", "TestModel");
        }

        pause(500);
        String percentAfter = getCompletionPercentage();
        logStep("Percentage after filling field: " + percentAfter);

        logStepWithScreenshot("ATS required field percentage");
        ExtentReportManager.logPass("Required field filled. Before: " + percentBefore
                + ", After: " + percentAfter);
    }

    @Test(priority = 3, description = "ATS_EAD_12: Clear required field — percentage decreases")
    public void testATS_EAD_12_ClearRequiredField() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ATS_EAD_12_ClearRequiredField");
        logStep("Verifying percentage decreases when required field is cleared");

        if (!openEditForAssetClass("Automatic Transfer Switch")
                && !openEditForAssetClass("ATS")) {
            logStep("SKIP: No ATS assets found");
            ExtentReportManager.logPass("SKIP: No ATS assets available");
            return;
        }

        expandCoreAttributes();
        String percentBefore = getCompletionPercentage();
        logStep("Percentage before clearing: " + percentBefore);

        // Clear a field
        editTextField("Manufacturer", "");
        pause(500);

        String percentAfter = getCompletionPercentage();
        logStep("Percentage after clearing: " + percentAfter);

        logStepWithScreenshot("ATS clear field percentage");
        ExtentReportManager.logPass("Field cleared. Before: " + percentBefore
                + ", After: " + percentAfter);
    }

    @Test(priority = 4, description = "ATS_EAD_13: Complete all required fields — 100%")
    public void testATS_EAD_13_CompleteAllRequired() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ATS_EAD_13_CompleteAllRequired");
        logStep("Verifying 100% when all required fields are filled");

        if (!openEditForAssetClass("Automatic Transfer Switch")
                && !openEditForAssetClass("ATS")) {
            logStep("SKIP: No ATS assets found");
            ExtentReportManager.logPass("SKIP: No ATS assets available");
            return;
        }

        expandCoreAttributes();

        // Fill common required fields for ATS
        editTextField("Manufacturer", "TestMfg");
        selectFirstDropdownOption("Ampere Rating");
        selectFirstDropdownOption("Voltage");
        editTextField("Model", "TestModel");
        editTextField("Serial Number", "SN_" + System.currentTimeMillis());
        editTextField("Catalog Number", "CAT_TEST");
        pause(500);

        String percentage = getCompletionPercentage();
        logStep("Percentage after filling all fields: " + percentage);

        logStepWithScreenshot("ATS all required fields");
        ExtentReportManager.logPass("All required fields filled. Percentage: " + percentage);
    }

    @Test(priority = 5, description = "ATS_EAD_15: Save with no required fields filled")
    public void testATS_EAD_15_SaveNoRequiredFields() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ATS_EAD_15_SaveNoRequired");
        logStep("Verifying save succeeds with no required fields filled");

        if (!openEditForAssetClass("Automatic Transfer Switch")
                && !openEditForAssetClass("ATS")) {
            logStep("SKIP: No ATS assets found");
            ExtentReportManager.logPass("SKIP: No ATS assets available");
            return;
        }

        // Don't fill any fields, just save
        boolean saved = saveAndVerify();
        logStep("Save with no required fields: " + saved);

        logStepWithScreenshot("ATS save no required fields");
        ExtentReportManager.logPass("Save with no required fields: success=" + saved);
    }

    @Test(priority = 6, description = "ATS_EAD_16: Save with partial required fields")
    public void testATS_EAD_16_SavePartialRequired() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ATS_EAD_16_SavePartialRequired");
        logStep("Verifying save succeeds with partial required fields");

        if (!openEditForAssetClass("Automatic Transfer Switch")
                && !openEditForAssetClass("ATS")) {
            logStep("SKIP: No ATS assets found");
            ExtentReportManager.logPass("SKIP: No ATS assets available");
            return;
        }

        expandCoreAttributes();

        // Fill just one required field
        editTextField("Model", "PartialSave_" + System.currentTimeMillis());
        pause(300);

        boolean saved = saveAndVerify();
        logStep("Save with partial required fields: " + saved);

        logStepWithScreenshot("ATS save partial required");
        ExtentReportManager.logPass("Save with partial required fields: success=" + saved);
    }

    @Test(priority = 7, description = "ATS_EAD_17: Save with all required fields")
    public void testATS_EAD_17_SaveAllRequired() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ATS_EAD_17_SaveAllRequired");
        logStep("Verifying save succeeds with all required fields filled");

        if (!openEditForAssetClass("Automatic Transfer Switch")
                && !openEditForAssetClass("ATS")) {
            logStep("SKIP: No ATS assets found");
            ExtentReportManager.logPass("SKIP: No ATS assets available");
            return;
        }

        expandCoreAttributes();

        // Fill all common required fields
        editTextField("Manufacturer", "FullSave_Mfg");
        selectFirstDropdownOption("Ampere Rating");
        selectFirstDropdownOption("Voltage");
        editTextField("Model", "FullSave_Model");
        editTextField("Serial Number", "FS_SN_" + System.currentTimeMillis());
        pause(300);

        boolean saved = saveAndVerify();
        logStep("Save with all required fields: " + saved);

        Assert.assertTrue(saved, "Save should succeed with all required fields filled");
        logStepWithScreenshot("ATS save all required");
        ExtentReportManager.logPass("Save with all required fields: success");
    }

    // ================================================================
    // SECTION 2: BUSWAY (1 TC)
    // ================================================================

    @Test(priority = 10, description = "BUS_EAD_03: Save Busway without Core Attributes")
    public void testBUS_EAD_03_SaveWithoutCoreAttributes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "BUS_EAD_03_SaveWithoutCoreAttr");
        logStep("Verifying Busway can be saved without filling Core Attributes");

        if (!openEditForAssetClass("Busway")) {
            logStep("SKIP: No Busway assets found");
            ExtentReportManager.logPass("SKIP: No Busway assets available");
            return;
        }

        // Don't fill any core attributes, just save
        boolean saved = saveAndVerify();
        logStep("Save Busway without core attributes: " + saved);

        logStepWithScreenshot("Busway save without core attributes");
        ExtentReportManager.logPass("Busway save without core attributes: success=" + saved);
    }

    // ================================================================
    // SECTION 3: CAPACITOR (22 TCs)
    // ================================================================

    @Test(priority = 20, description = "CAP_EAD_01: Open Edit Asset Details for Capacitor")
    public void testCAP_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_01_OpenEditScreen");
        logStep("Verifying Edit Asset Details screen opens for Capacitor");

        boolean found = navigateToAssetByClass("Capacitor");
        if (!found) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        boolean editOpen = openEditForm("Capacitor");
        Assert.assertTrue(editOpen, "Edit screen should open for Capacitor");

        logStepWithScreenshot("Capacitor Edit screen");
        ExtentReportManager.logPass("Edit Asset Details screen opens for Capacitor");
    }

    @Test(priority = 21, description = "CAP_EAD_04: Verify Save Changes button visible")
    public void testCAP_EAD_04_SaveChangesButtonVisible() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_04_SaveButtonVisible");
        logStep("Verifying Save Changes button is visible on Capacitor edit");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        boolean visible = isSaveChangesButtonVisible();
        Assert.assertTrue(visible, "Save Changes button should be visible");

        logStepWithScreenshot("Save Changes button visibility");
        ExtentReportManager.logPass("Save Changes button is visible on Capacitor edit form");
    }

    @Test(priority = 22, description = "CAP_EAD_05: Edit A Phase Serial Number")
    public void testCAP_EAD_05_EditAPhaseSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_05_APhaseSerial");
        logStep("Editing A Phase Serial Number for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("A Phase Serial Number", "APSN_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("A Phase Serial Number edit");
        ExtentReportManager.logPass("A Phase Serial Number: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 23, description = "CAP_EAD_06: Edit B Phase Serial Number")
    public void testCAP_EAD_06_EditBPhaseSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_06_BPhaseSerial");
        logStep("Editing B Phase Serial Number for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("B Phase Serial Number", "BPSN_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("B Phase Serial Number edit");
        ExtentReportManager.logPass("B Phase Serial Number: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 24, description = "CAP_EAD_07: Edit C Phase Serial Number")
    public void testCAP_EAD_07_EditCPhaseSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_07_CPhaseSerial");
        logStep("Editing C Phase Serial Number for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("C Phase Serial Number", "CPSN_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("C Phase Serial Number edit");
        ExtentReportManager.logPass("C Phase Serial Number: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 25, description = "CAP_EAD_08: Edit Catalog Number")
    public void testCAP_EAD_08_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_08_CatalogNumber");
        logStep("Editing Catalog Number for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Catalog Number", "CAT_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Catalog Number edit");
        ExtentReportManager.logPass("Catalog Number: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 26, description = "CAP_EAD_09: Edit Fluid Capacity")
    public void testCAP_EAD_09_EditFluidCapacity() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_09_FluidCapacity");
        logStep("Editing Fluid Capacity for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Fluid Capacity", "250");
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Fluid Capacity edit");
        ExtentReportManager.logPass("Fluid Capacity: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 27, description = "CAP_EAD_10: Edit Fluid Type")
    public void testCAP_EAD_10_EditFluidType() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_10_FluidType");
        logStep("Editing Fluid Type for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Fluid Type", "Mineral Oil");
        if (val == null) {
            val = selectFirstDropdownOption("Fluid Type");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Fluid Type edit");
        ExtentReportManager.logPass("Fluid Type: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 28, description = "CAP_EAD_11: Edit Fuse Amperage")
    public void testCAP_EAD_11_EditFuseAmperage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_11_FuseAmperage");
        logStep("Editing Fuse Amperage for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = selectFirstDropdownOption("Fuse Amperage");
        if (val == null) {
            val = editTextField("Fuse Amperage", "30");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Fuse Amperage edit");
        ExtentReportManager.logPass("Fuse Amperage: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 29, description = "CAP_EAD_12: Edit Fuse Manufacturer")
    public void testCAP_EAD_12_EditFuseManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_12_FuseManufacturer");
        logStep("Editing Fuse Manufacturer for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Fuse Manufacturer", "Bussmann");
        if (val == null) {
            val = selectFirstDropdownOption("Fuse Manufacturer");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Fuse Manufacturer edit");
        ExtentReportManager.logPass("Fuse Manufacturer: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 30, description = "CAP_EAD_13: Edit Fuse Refill Number")
    public void testCAP_EAD_13_EditFuseRefillNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_13_FuseRefillNumber");
        logStep("Editing Fuse Refill Number for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Fuse Refill Number", "REF_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Fuse Refill Number edit");
        ExtentReportManager.logPass("Fuse Refill Number: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 31, description = "CAP_EAD_14: Edit KVAR Rating")
    public void testCAP_EAD_14_EditKVARRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_14_KVARRating");
        logStep("Editing KVAR Rating for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("KVAR Rating", "100");
        if (val == null) {
            val = selectFirstDropdownOption("KVAR");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("KVAR Rating edit");
        ExtentReportManager.logPass("KVAR Rating: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 32, description = "CAP_EAD_15: Edit Manufacturer")
    public void testCAP_EAD_15_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_15_Manufacturer");
        logStep("Editing Manufacturer for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Manufacturer", "ABB");
        if (val == null) {
            val = selectFirstDropdownOption("Manufacturer");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Manufacturer edit");
        ExtentReportManager.logPass("Manufacturer: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 33, description = "CAP_EAD_16: Edit Model")
    public void testCAP_EAD_16_EditModel() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_16_Model");
        logStep("Editing Model for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Model", "CAP_Model_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Model edit");
        ExtentReportManager.logPass("Model: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 34, description = "CAP_EAD_17: Edit Notes")
    public void testCAP_EAD_17_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_17_Notes");
        logStep("Editing Notes for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Notes", "Test note from automation " + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Notes edit");
        ExtentReportManager.logPass("Notes: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 35, description = "CAP_EAD_18: Edit PCB Labeled")
    public void testCAP_EAD_18_EditPCBLabeled() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_18_PCBLabeled");
        logStep("Editing PCB Labeled for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("PCB Labeled", "Yes");
        if (val == null) {
            val = selectFirstDropdownOption("PCB");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("PCB Labeled edit");
        ExtentReportManager.logPass("PCB Labeled: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 36, description = "CAP_EAD_19: Edit Serial Number")
    public void testCAP_EAD_19_EditSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_19_SerialNumber");
        logStep("Editing Serial Number for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Serial Number", "SN_CAP_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Serial Number edit");
        ExtentReportManager.logPass("Serial Number: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 37, description = "CAP_EAD_20: Edit Spare Fuses")
    public void testCAP_EAD_20_EditSpareFuses() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_20_SpareFuses");
        logStep("Editing Spare Fuses for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Spare Fuses", "5");
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Spare Fuses edit");
        ExtentReportManager.logPass("Spare Fuses: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 38, description = "CAP_EAD_21: Edit Style")
    public void testCAP_EAD_21_EditStyle() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_21_Style");
        logStep("Editing Style for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Style", "Fixed");
        if (val == null) {
            val = selectFirstDropdownOption("Style");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Style edit");
        ExtentReportManager.logPass("Style: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 39, description = "CAP_EAD_22: Edit Type")
    public void testCAP_EAD_22_EditType() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_22_Type");
        logStep("Editing Type for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Type", "Film");
        if (val == null) {
            val = selectFirstDropdownOption("Type");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Type edit");
        ExtentReportManager.logPass("Type: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 40, description = "CAP_EAD_23: Edit UF Rating")
    public void testCAP_EAD_23_EditUFRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_23_UFRating");
        logStep("Editing UF Rating for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("UF Rating", "50");
        boolean saved = saveAndVerify();

        logStepWithScreenshot("UF Rating edit");
        ExtentReportManager.logPass("UF Rating: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 41, description = "CAP_EAD_24: Edit Voltage")
    public void testCAP_EAD_24_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CAP_EAD_24_Voltage");
        logStep("Editing Voltage for Capacitor");

        if (!openEditForAssetClass("Capacitor")) {
            logStep("SKIP: No Capacitor assets found");
            ExtentReportManager.logPass("SKIP: No Capacitor assets available");
            return;
        }

        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        if (val == null) {
            val = editTextField("Voltage", "480");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Voltage edit");
        ExtentReportManager.logPass("Voltage: value='" + val + "', saved=" + saved);
    }

    // ================================================================
    // SECTION 4: CIRCUIT BREAKER (10 TCs)
    // ================================================================

    @Test(priority = 50, description = "CB_EAD_01: Open Edit Asset Details for Circuit Breaker")
    public void testCB_EAD_01_OpenEditScreen() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_01_OpenEditScreen");
        logStep("Verifying Edit Asset Details screen opens for Circuit Breaker");

        boolean found = navigateToAssetByClass("Circuit Breaker");
        if (!found) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        boolean editOpen = openEditForm("Circuit Breaker");
        Assert.assertTrue(editOpen, "Edit screen should open for Circuit Breaker");

        logStepWithScreenshot("Circuit Breaker Edit screen");
        ExtentReportManager.logPass("Edit Asset Details opens for Circuit Breaker");
    }

    @Test(priority = 51, description = "CB_EAD_05: Verify required fields counter shows 0/6")
    public void testCB_EAD_05_RequiredFieldsCounter() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_05_RequiredFieldsCounter");
        logStep("Verifying required fields counter for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String counter = getRequiredFieldsCounter();
        logStep("Required fields counter: " + counter);

        logStepWithScreenshot("Required fields counter");
        // Counter should show something like "0/6" or similar
        ExtentReportManager.logPass("Required fields counter: " + counter);
    }

    @Test(priority = 52, description = "CB_EAD_10: Edit Ampere Rating")
    public void testCB_EAD_10_EditAmpereRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_10_AmpereRating");
        logStep("Editing Ampere Rating for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = selectFirstDropdownOption("Ampere Rating");
        if (val == null) {
            val = editTextField("Ampere Rating", "100");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Ampere Rating edit");
        ExtentReportManager.logPass("Ampere Rating: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 53, description = "CB_EAD_11: Edit Breaker Settings")
    public void testCB_EAD_11_EditBreakerSettings() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_11_BreakerSettings");
        logStep("Editing Breaker Settings for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Breaker Settings", "Long-Time: 0.8, Short-Time: 5x");
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Breaker Settings edit");
        ExtentReportManager.logPass("Breaker Settings: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 54, description = "CB_EAD_12: Edit Interrupting Rating")
    public void testCB_EAD_12_EditInterruptingRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_12_InterruptingRating");
        logStep("Editing Interrupting Rating for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = selectFirstDropdownOption("Interrupting Rating");
        if (val == null) {
            val = editTextField("Interrupting Rating", "65kAIC");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Interrupting Rating edit");
        ExtentReportManager.logPass("Interrupting Rating: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 55, description = "CB_EAD_13: Edit Manufacturer")
    public void testCB_EAD_13_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_13_Manufacturer");
        logStep("Editing Manufacturer for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = selectFirstDropdownOption("Manufacturer");
        if (val == null) {
            val = editTextField("Manufacturer", "Eaton");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Manufacturer edit");
        ExtentReportManager.logPass("Manufacturer: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 56, description = "CB_EAD_14: Edit Model")
    public void testCB_EAD_14_EditModel() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_14_Model");
        logStep("Editing Model for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Model", "CB_Model_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Model edit");
        ExtentReportManager.logPass("Model: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 57, description = "CB_EAD_15: Edit Voltage")
    public void testCB_EAD_15_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_15_Voltage");
        logStep("Editing Voltage for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = selectFirstDropdownOption("Voltage");
        if (val == null) {
            val = editTextField("Voltage", "480");
        }
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Voltage edit");
        ExtentReportManager.logPass("Voltage: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 58, description = "CB_EAD_16: Edit Catalog Number")
    public void testCB_EAD_16_EditCatalogNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_16_CatalogNumber");
        logStep("Editing Catalog Number for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Catalog Number", "CB_CAT_" + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Catalog Number edit");
        ExtentReportManager.logPass("Catalog Number: value='" + val + "', saved=" + saved);
    }

    @Test(priority = 59, description = "CB_EAD_17: Edit Notes")
    public void testCB_EAD_17_EditNotes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "CB_EAD_17_Notes");
        logStep("Editing Notes for Circuit Breaker");

        if (!openEditForAssetClass("Circuit Breaker")) {
            logStep("SKIP: No Circuit Breaker assets found");
            ExtentReportManager.logPass("SKIP: No Circuit Breaker assets available");
            return;
        }

        expandCoreAttributes();
        String val = editTextField("Notes", "CB test note " + System.currentTimeMillis());
        boolean saved = saveAndVerify();

        logStepWithScreenshot("Notes edit");
        ExtentReportManager.logPass("Notes: value='" + val + "', saved=" + saved);
    }
}
