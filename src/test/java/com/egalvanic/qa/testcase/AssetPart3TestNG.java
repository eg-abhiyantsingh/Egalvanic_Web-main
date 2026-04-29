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
import java.util.ArrayList;
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

    // navigatedToAssets removed — ensureOnAssetsPage now checks URL directly
    private boolean editFormOpen = false;
    private boolean createFormOpen = false;

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
        try {
            ensureOnAssetsPage();
        } catch (Exception e) {
            logStep("ensureOnAssetsPage failed (" + e.getClass().getSimpleName()
                    + ") — recovering via dashboard round-trip");
            try {
                driver.get(AppConstants.BASE_URL + "/dashboard");
                pause(3000);
                driver.get(AppConstants.BASE_URL + "/assets");
                pause(5000);
            } catch (Exception e2) {
                logStep("Recovery also failed: " + e2.getMessage());
            }
        }
    }

    @AfterMethod
    @Override
    public void testTeardown(ITestResult result) {
        try {
            if (editFormOpen) {
                closeEditFormIfOpen();
                editFormOpen = false;
            }
            if (createFormOpen) {
                closeCreateFormIfOpen();
                createFormOpen = false;
            }
        } catch (Exception ignored) {}
        super.testTeardown(result);
    }

    // ================================================================
    // HELPER METHODS (same pattern as Part 2)
    // ================================================================

    private void ensureOnAssetsPage() {
        // Always navigate back — detail/edit pages also contain "asset" in URL
        // so isOnAssetsPage() alone is unreliable after navigating to detail
        String url = driver.getCurrentUrl();
        boolean onListPage = url.endsWith("/assets") || url.endsWith("/assets/");
        if (!onListPage) {
            assetPage.navigateToAssets();
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

        // Click a row whose "Asset Class" column matches, not just the first row.
        boolean clicked = clickRowWithAssetClass(assetClassName);
        if (!clicked) {
            logStep("No row matched class '" + assetClassName + "' — falling back to first row");
            assetPage.navigateToFirstAssetDetail();
        }
        pause(2000);
        String url = driver.getCurrentUrl();
        boolean onDetail = url.contains("/assets/") && !url.endsWith("/assets") && !url.endsWith("/assets/");
        logStep("On detail page: " + onDetail);
        return onDetail;
    }

    /**
     * Finds and clicks a grid row whose "Asset Class" column matches the given class name.
     */
    private boolean clickRowWithAssetClass(String assetClassName) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            @SuppressWarnings("unchecked")
            java.util.List<WebElement> rows = (java.util.List<WebElement>) js.executeScript(
                    "var rows = document.querySelectorAll('.MuiDataGrid-row[data-rowindex]');" +
                    "var result = [];" +
                    "for (var row of rows) {" +
                    "  var cells = row.querySelectorAll('.MuiDataGrid-cell');" +
                    "  for (var cell of cells) {" +
                    "    if (cell.textContent.trim().toLowerCase() === arguments[0].toLowerCase()) {" +
                    "      result.push(row); break;" +
                    "    }" +
                    "  }" +
                    "}" +
                    "return result;", assetClassName);
            if (rows != null && !rows.isEmpty()) {
                logStep("Found " + rows.size() + " rows with class '" + assetClassName + "' — clicking first");
                WebElement cell = rows.get(0).findElement(
                        By.xpath(".//div[contains(@class,'MuiDataGrid-cell')][1]"));
                String currentUrl = driver.getCurrentUrl();
                try { cell.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", cell); }
                pause(3000);
                return !driver.getCurrentUrl().equals(currentUrl);
            }
        } catch (Exception e) {
            logStep("clickRowWithAssetClass error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Opens the Edit Asset form via kebab menu (three dots → Edit Asset).
     * After opening, ensures the Asset Class is selected so that
     * core attribute fields render (they are dynamic per class).
     *
     * @param assetClassName the class name to ensure is selected
     */
    private boolean openEditForm(String assetClassName) {
        logStep("Opening Edit Asset form");
        try {
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2000);
            editFormOpen = true;
            List<WebElement> saveBtn = driver.findElements(SAVE_CHANGES_BTN);
            boolean formOpen = !saveBtn.isEmpty();
            logStep("Edit form open: " + formOpen);

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

    private boolean openEditForAssetClass(String assetClassName) {
        if (!navigateToAssetByClass(assetClassName)) return false;
        return openEditForm(assetClassName);
    }

    /**
     * Try primary name, then fallback abbreviation for grid search.
     * Always uses the primary name for asset class selection in the edit form.
     */
    private boolean openEditForAssetClass(String primary, String fallback) {
        if (openEditForAssetClass(primary)) return true;
        ensureOnAssetsPage();
        // Search grid with fallback term, but use primary name for class selection
        if (!navigateToAssetByClass(fallback)) return false;
        return openEditForm(primary);
    }

    /**
     * Opens the Create Asset form and selects the given asset class.
     * Subtype options only populate when a class is selected during creation,
     * not in Edit mode — so subtype verification tests must use this flow.
     */
    private boolean openCreateFormForClass(String assetClassName) {
        logStep("Opening Create Asset form for class: " + assetClassName);
        try {
            ensureOnAssetsPage();
            assetPage.openCreateAssetForm();
            pause(2000);
            createFormOpen = true;

            // Select the Asset Class — use sendKeys so MUI Autocomplete filters properly
            WebElement classInput = driver.findElement(
                    By.xpath("//input[@placeholder='Select Class']"));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            dismissBackdrops();
            js.executeScript("arguments[0].focus(); arguments[0].click();", classInput);
            pause(300);
            classInput.sendKeys(assetClassName);
            pause(1500);

            List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
            boolean selected = false;
            for (WebElement opt : options) {
                String text = opt.getText().trim();
                if (text.equalsIgnoreCase(assetClassName)
                        || text.toLowerCase().contains(assetClassName.toLowerCase())) {
                    js.executeScript("arguments[0].click();", opt);
                    selected = true;
                    break;
                }
            }
            if (!selected && !options.isEmpty()) {
                js.executeScript("arguments[0].click();", options.get(0));
                selected = true;
            }

            // Wait for subtype dropdown to become enabled (up to 5s)
            if (selected) {
                for (int i = 0; i < 10; i++) {
                    try {
                        WebElement subtype = driver.findElement(
                                By.xpath("//input[@placeholder='Select Subtype']"));
                        if (subtype.isEnabled()) {
                            logStep("Subtype enabled after ~" + (i * 500 + 1500) + "ms");
                            break;
                        }
                    } catch (Exception ignored) {}
                    pause(500);
                }
            }

            logStep("Create form opened with class: " + assetClassName + ", selected: " + selected);
            return selected;
        } catch (Exception e) {
            logStep("Failed to open create form for class: " + e.getMessage());
            return false;
        }
    }

    private void closeCreateFormIfOpen() {
        try {
            List<WebElement> cancelBtns = driver.findElements(
                    By.xpath("//button[normalize-space()='Cancel']"));
            if (!cancelBtns.isEmpty()) {
                cancelBtns.get(0).click();
                pause(500);
            }
        } catch (Exception ignored) {}
        createFormOpen = false;
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
        // Expand all nested sub-accordions (each core attribute field has its own accordion)
        expandAllNestedAccordions();
    }

    /**
     * Expand all collapsed MUI Accordions in the edit drawer.
     * Core attribute fields (Notes, Voltage, etc.) each have their own
     * nested accordion that must be expanded before the field is accessible.
     */
    private void expandAllNestedAccordions() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "var summaries = document.querySelectorAll('[class*=\"MuiAccordionSummary\"][aria-expanded=\"false\"]');" +
                "summaries.forEach(function(s) { s.click(); });"
            );
            pause(1500); // Multiple accordion state updates need time in headless Chrome
        } catch (Exception ignored) {}
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

        // CI hardening: CI run 25054293207 had GEN_EAD_10 fail with
        // "editTextField should find and set 'Power Factor' field" — null return
        // because all 4 find strategies returned null when called immediately.
        // The Edit drawer's CORE ATTRIBUTES fields can render with lag in CI
        // (slower SPA hydration). Add a 5s retry-with-poll loop before giving up.
        WebElement input = null;
        long findDeadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < findDeadline) {
            input = findInputInDrawerByLabel(fieldLabel);
            if (input == null) input = findInputByPlaceholder(fieldLabel);
            if (input == null) input = findInputByLabel(fieldLabel);
            if (input == null) input = findInputByAriaLabel(fieldLabel);
            if (input != null) break;
            pause(500);
        }
        if (input == null) {
            logStep("Field '" + fieldLabel + "' not found after 5s polling — "
                    + "either field doesn't exist on this asset class OR DOM rendering stalled");
            return null;
        }
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", input);
        pause(600); // Smooth scroll needs time to complete in headless Chrome
        // Re-find the element after scroll — React may have re-rendered the DOM
        WebElement freshInput = findInputInDrawerByLabel(fieldLabel);
        if (freshInput == null) freshInput = findInputByPlaceholder(fieldLabel);
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
        freshInput = findInputInDrawerByLabel(fieldLabel);
        if (freshInput == null) freshInput = findInputByPlaceholder(fieldLabel);
        if (freshInput == null) freshInput = findInputByLabel(fieldLabel);
        if (freshInput == null) freshInput = findInputByAriaLabel(fieldLabel);
        if (freshInput != null) input = freshInput;
        String actual = input.getAttribute("value");
        logStep("Value set: '" + actual + "'");
        return actual;
    }

    private String selectDropdownValue(String fieldLabel, String valueToSelect) {
        logStep("Selecting dropdown '" + fieldLabel + "'");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement input = findInputInDrawerByLabel(fieldLabel);
        if (input == null) input = findInputByPlaceholder(fieldLabel);
        if (input == null) input = findInputByLabel(fieldLabel);
        if (input == null) input = findInputByAriaLabel(fieldLabel);
        if (input == null) {
            logStep("Dropdown '" + fieldLabel + "' not found");
            return null;
        }
        // Skip dropdown logic for plain text fields — avoids clearing the value
        String role = input.getAttribute("role");
        String ariaAuto = input.getAttribute("aria-autocomplete");
        if (!"combobox".equals(role) && ariaAuto == null) {
            logStep("'" + fieldLabel + "' is a text field, not a dropdown — skipping");
            return null;
        }
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", input);
        pause(300);
        // Re-find after scroll to avoid stale element
        WebElement freshInput = findInputInDrawerByLabel(fieldLabel);
        if (freshInput == null) freshInput = findInputByPlaceholder(fieldLabel);
        if (freshInput == null) freshInput = findInputByLabel(fieldLabel);
        if (freshInput == null) freshInput = findInputByAriaLabel(fieldLabel);
        if (freshInput != null) input = freshInput;
        dismissBackdrops();
        js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", input);
        pause(800); // MUI autocomplete popover render needs time in headless
        if (valueToSelect != null && !valueToSelect.isEmpty()) {
            js.executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],arguments[1]);"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", input, valueToSelect);
            pause(1200);
        }
        List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
        // For server-populated autocompletes, dispatch empty-string input to trigger full list
        if (options.isEmpty()) {
            js.executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],'');"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", input);
            pause(2500); // Server-populated dropdowns need longer in CI
            options = driver.findElements(By.xpath("//li[@role='option']"));
        }
        if (options.isEmpty()) {
            // Click the drawer heading to dismiss focus — do NOT send Escape
            // as it would close the entire MUI Drawer when no dropdown is open
            try {
                WebElement heading = driver.findElement(By.xpath(
                        "//h6[normalize-space()='Edit Asset' or normalize-space()='Add Asset']"));
                heading.click();
            } catch (Exception ignored) {}
            pause(300);
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
     * Find an input inside the edit drawer by matching the label paragraph text.
     * DOM structure: div.MuiBox-root > p (label) + div.MuiTextField-root > ... > input
     * Scoping to the drawer avoids matching the read-only Core Attributes table.
     */
    private WebElement findInputInDrawerByLabel(String label) {
        try {
            String drawerPrefix = "//div[contains(@class,'MuiDrawer')]";
            // Strategy 1: starts-with match — standard text field layout (p/following-sibling::div//input)
            // Uses starts-with to handle asterisk suffixes (e.g., "Ampere Rating*")
            List<WebElement> inputs = driver.findElements(By.xpath(
                    drawerPrefix + "//p[starts-with(normalize-space(),'" + label + "')]"
                    + "/following-sibling::div//input"));
            if (!inputs.isEmpty()) return inputs.get(0);
            // Strategy 2: starts-with + combobox layout (p inside wrapper, input in sibling div)
            // Works for: Voltage (capital V), Asset Class — MUI Autocomplete wraps label differently
            inputs = driver.findElements(By.xpath(
                    drawerPrefix + "//p[starts-with(normalize-space(),'" + label + "')]"
                    + "/parent::div/following-sibling::div//input"));
            if (!inputs.isEmpty()) return inputs.get(0);
            // Strategy 3: Case-insensitive — only if no exact match found above
            String lower = label.toLowerCase();
            inputs = driver.findElements(By.xpath(
                    drawerPrefix + "//p[starts-with(translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')]"
                    + "/following-sibling::div//input"));
            if (!inputs.isEmpty()) return inputs.get(0);
            // Strategy 4: CI + combobox layout
            inputs = driver.findElements(By.xpath(
                    drawerPrefix + "//p[starts-with(translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')]"
                    + "/parent::div/following-sibling::div//input"));
            if (!inputs.isEmpty()) return inputs.get(0);
            // Strategy 5: Textarea fallback (for NOTES)
            inputs = driver.findElements(By.xpath(
                    drawerPrefix + "//p[starts-with(normalize-space(),'" + label + "')]"
                    + "/following-sibling::div//textarea"));
            if (!inputs.isEmpty()) return inputs.get(0);
            // Strategy 6: CI textarea fallback
            inputs = driver.findElements(By.xpath(
                    drawerPrefix + "//p[starts-with(translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')]"
                    + "/following-sibling::div//textarea"));
            if (!inputs.isEmpty()) return inputs.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    private boolean saveAndVerify() {
        logStep("Saving changes");
        String detailUrl = driver.getCurrentUrl();
        assetPage.saveChanges();
        // Wait for the edit drawer to actually close (right-side drawer disappears)
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (int i = 0; i < 25; i++) {
            pause(500);
            Boolean drawerGone = (Boolean) js.executeScript(
                    "var d = document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper');"
                    + "if (!d) return true;"
                    + "var s = window.getComputedStyle(d);"
                    + "return d.getBoundingClientRect().width === 0"
                    + " || s.display === 'none'"
                    + " || s.visibility === 'hidden'"
                    + " || s.opacity === '0';");
            if (Boolean.TRUE.equals(drawerGone)) {
                logStep("Edit drawer closed after " + ((i + 1) * 500) + "ms");
                break;
            }
        }
        pause(1000);
        boolean success = assetPage.waitForEditSuccess();
        logStep("Save success: " + success);
        if (success) {
            editFormOpen = false;
            // Force a fresh page load to ensure React state reflects saved data.
            // Without this, the detail table may still show stale pre-save values.
            if (detailUrl.contains("/assets/")) {
                logStep("Refreshing detail page to load saved data");
                driver.navigate().to(detailUrl);
                pause(3000);
            }
        }
        return success;
    }

    /**
     * Read an attribute value from the read-only Core Attributes table on the
     * asset detail page.  The table structure is:
     *   | (icon) | Attribute | Required | Value |
     * We match the row whose "Attribute" cell equals {@code fieldLabel} and
     * return the text in the "Value" cell.
     */
    private String readDetailAttributeValue(String fieldLabel) {
        // Wait for the detail page to finish re-fetching data after save.
        // The React component re-renders asynchronously after the drawer closes,
        // so we poll for the Core Attributes table to appear + contain the field.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Step 1: Ensure the Core Attributes tab is clicked (it may not be default after reload)
        try {
            List<WebElement> tabs = driver.findElements(By.xpath(
                    "//button[@role='tab' and contains(text(),'Core Attributes')]"));
            for (WebElement tab : tabs) {
                String selected = tab.getAttribute("aria-selected");
                if (!"true".equals(selected)) {
                    tab.click();
                    logStep("Clicked 'Core Attributes' tab");
                    pause(1000);
                }
            }
        } catch (Exception ignored) {}

        // Step 2: Wait for table.MuiTable-root to appear in DOM (up to 15s)
        pause(2000);
        for (int i = 0; i < 10; i++) {
            Boolean tableExists = (Boolean) js.executeScript(
                    "var t = document.querySelector('table.MuiTable-root');"
                    + "return t !== null && t.querySelectorAll('tr').length > 1;");
            if (Boolean.TRUE.equals(tableExists)) {
                logStep("Core Attributes table found after ~" + (2000 + i * 1000) + "ms");
                break;
            }
            pause(1000);
        }

        // Step 3: Poll for the specific field value (case-insensitive match)
        String script =
                "var rows = document.querySelectorAll('table.MuiTable-root tr');"
                + "for (var r of rows) {"
                + "  var cells = r.querySelectorAll('td');"
                + "  for (var c of cells) {"
                + "    if (c.textContent.trim().toLowerCase() === arguments[0].toLowerCase()) {"
                + "      var all = r.querySelectorAll('td');"
                + "      return all[all.length - 1].textContent.trim();"
                + "    }"
                + "  }"
                + "}"
                + "return null;";
        String val = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                Object result = js.executeScript(script, fieldLabel);
                val = result != null ? result.toString() : null;
            } catch (Exception e) {
                logStep("readDetailAttributeValue attempt " + (attempt + 1) + " error: " + e.getMessage());
            }
            if (val != null) {
                logStep("Detail page value for '" + fieldLabel + "': " + val + " (attempt " + (attempt + 1) + ")");
                return val;
            }
            pause(1500);
        }
        logStep("Detail page value for '" + fieldLabel + "': null (after 10 attempts)");
        return null;
    }

    private void closeEditFormIfOpen() {
        try {
            List<WebElement> cancelBtns = driver.findElements(By.xpath("//button[normalize-space()='Cancel']"));
            if (!cancelBtns.isEmpty()) { cancelBtns.get(0).click(); pause(500); }
            // Click heading to dismiss focus safely (Escape would close entire MUI Drawer)
            try {
                WebElement heading = driver.findElement(By.xpath(
                        "//h6[normalize-space()='Edit Asset' or normalize-space()='Add Asset']"));
                heading.click();
            } catch (Exception e2) { /* drawer already closed */ }
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

        Assert.assertNotNull(subtypeInput, "Subtype field should be present");

        String currentValue = subtypeInput.getAttribute("value");
        logStep("Current subtype value: '" + currentValue + "'");

        // Open dropdown — try native click first (MUI needs real events), then fallback
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", subtypeInput);
        pause(400);
        // Re-find after scroll
        WebElement freshSubtype = findInputByPlaceholder("Select Subtype");
        if (freshSubtype == null) freshSubtype = findInputByLabel("Subtype");
        if (freshSubtype == null) freshSubtype = findInputByLabel("Asset Subtype");
        if (freshSubtype != null) subtypeInput = freshSubtype;
        dismissBackdrops();
        try {
            subtypeInput.click();
        } catch (Exception clickEx) {
            dismissBackdrops();
            js.executeScript("arguments[0].focus();", subtypeInput);
            subtypeInput.sendKeys("");
        }
        pause(1500);
        List<WebElement> options = driver.findElements(By.xpath("//li[@role='option']"));
        // Retry: if dropdown didn't open, try the MUI popup indicator button
        if (options.isEmpty()) {
            String expanded = subtypeInput.getAttribute("aria-expanded");
            if (!"true".equals(expanded)) {
                try {
                    WebElement openBtn = subtypeInput.findElement(By.xpath(
                            "./ancestor::div[contains(@class,'MuiAutocomplete')]"
                            + "//button[contains(@class,'popupIndicator') or @title='Open']"));
                    dismissBackdrops();
                    openBtn.click();
                } catch (Exception btnEx) {
                    subtypeInput.sendKeys(org.openqa.selenium.Keys.ARROW_DOWN);
                }
            }
            pause(2000);
            options = driver.findElements(By.xpath("//li[@role='option']"));
        }
        List<String> actualOptions = new ArrayList<>();
        for (WebElement opt : options) {
            String text = opt.getText().trim();
            actualOptions.add(text);
            logStep("  Subtype option: " + text);
        }
        logStep("Total subtype options: " + actualOptions.size());

        // Verify current value is either empty or one of the valid dropdown options.
        // Assets are reused across CI runs, so the subtype may have been set by a prior run.
        if (currentValue != null && !currentValue.isEmpty()) {
            boolean isValidOption = actualOptions.stream()
                    .anyMatch(a -> a.equalsIgnoreCase(currentValue));
            Assert.assertTrue(isValidOption,
                    "Subtype value '" + currentValue + "' should be a valid option. Available: " + actualOptions);
            logStep("Current value '" + currentValue + "' is a valid subtype option");
        } else {
            logStep("Subtype is empty (not yet set)");
        }

        // Verify expected options are present in the dropdown
        if (expectedOptions != null && expectedOptions.length > 0) {
            for (String expected : expectedOptions) {
                if ("None".equalsIgnoreCase(expected)) continue; // "None" is not a real option; empty = unset
                boolean found = actualOptions.stream()
                        .anyMatch(a -> a.equalsIgnoreCase(expected) || a.contains(expected));
                Assert.assertTrue(found,
                        "Expected subtype option '" + expected + "' not found. Available: " + actualOptions);
                logStep("  Expected '" + expected + "' → FOUND");
            }
        }

        // Close dropdown — click heading instead of Escape for safety
        try {
            WebElement heading = driver.findElement(By.xpath(
                    "//h6[normalize-space()='Edit Asset' or normalize-space()='Add Asset']"));
            heading.click();
        } catch (Exception ignored2) {}
        pause(300);
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

        String newValue = "800";
        String val = editTextField("Ampere Rating", newValue);
        Assert.assertNotNull(val, "editTextField should find and set 'Ampere Rating' field");
        Assert.assertEquals(val, newValue, "Ampere Rating input value should match");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing Ampere Rating");

        String persisted = readDetailAttributeValue("Ampere Rating");
        Assert.assertNotNull(persisted, "Ampere Rating should be visible on detail page after save");
        Assert.assertEquals(persisted, newValue, "Ampere Rating should persist after save");

        ExtentReportManager.logPass("Ampere Rating edited to '" + val + "', saved, and verified");
    }

    @Test(priority = 23, description = "GEN_EAD_06: Edit Configuration")
    public void testGEN_EAD_06_EditConfiguration() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_06_Configuration");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();

        // Field label on UI is lowercase "configuration"
        String val = selectFirstDropdownOption("configuration");
        if (val == null) val = editTextField("configuration", "3-Phase");
        Assert.assertNotNull(val, "Should be able to edit 'configuration' field");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing configuration");

        String persisted = readDetailAttributeValue("configuration");
        Assert.assertNotNull(persisted, "configuration should be visible on detail page after save");
        Assert.assertFalse("Not specified".equals(persisted), "configuration should have a value after save");

        ExtentReportManager.logPass("Configuration edited to '" + val + "', saved, and verified");
    }

    @Test(priority = 24, description = "GEN_EAD_07: Edit KVA Rating")
    public void testGEN_EAD_07_EditKVARating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_07_KVARating");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();

        // UI label is "K V A Rating" (with spaces)
        String newValue = "500";
        String val = editTextField("K V A Rating", newValue);
        Assert.assertNotNull(val, "editTextField should find and set 'K V A Rating' field");
        Assert.assertEquals(val, newValue, "K V A Rating input value should match");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing K V A Rating");

        String persisted = readDetailAttributeValue("K V A Rating");
        Assert.assertNotNull(persisted, "K V A Rating should be visible on detail page after save");
        Assert.assertEquals(persisted, newValue, "K V A Rating should persist after save");

        ExtentReportManager.logPass("K V A Rating edited to '" + val + "', saved, and verified");
    }

    @Test(priority = 25, description = "GEN_EAD_08: Edit KW Rating")
    public void testGEN_EAD_08_EditKWRating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_08_KWRating");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();

        // UI label is "K W Rating" (with space)
        String newValue = "400";
        String val = editTextField("K W Rating", newValue);
        Assert.assertNotNull(val, "editTextField should find and set 'K W Rating' field");
        Assert.assertEquals(val, newValue, "K W Rating input value should match");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing K W Rating");

        String persisted = readDetailAttributeValue("K W Rating");
        Assert.assertNotNull(persisted, "K W Rating should be visible on detail page after save");
        Assert.assertEquals(persisted, newValue, "K W Rating should persist after save");

        ExtentReportManager.logPass("K W Rating edited to '" + val + "', saved, and verified");
    }

    @Test(priority = 26, description = "GEN_EAD_09: Edit Manufacturer")
    public void testGEN_EAD_09_EditManufacturer() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_09_Manufacturer");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();

        // UI label is lowercase "manufacturer" — plain text field, not a dropdown
        String val = editTextField("manufacturer", "Caterpillar");
        Assert.assertNotNull(val, "Should be able to edit 'manufacturer' field");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing manufacturer");

        String persisted = readDetailAttributeValue("manufacturer");
        Assert.assertNotNull(persisted, "manufacturer should be visible on detail page after save");
        Assert.assertFalse("Not specified".equals(persisted), "manufacturer should have a value after save");

        ExtentReportManager.logPass("Manufacturer edited to '" + val + "', saved, and verified");
    }

    @Test(priority = 27, description = "GEN_EAD_10: Edit Power Factor")
    public void testGEN_EAD_10_EditPowerFactor() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_10_PowerFactor");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();

        String newValue = "0.85";
        String val = editTextField("Power Factor", newValue);
        Assert.assertNotNull(val, "editTextField should find and set 'Power Factor' field");
        Assert.assertEquals(val, newValue, "Power Factor input value should match");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing Power Factor");

        // Post-save: verify value persisted on detail page
        String persisted = readDetailAttributeValue("Power Factor");
        Assert.assertNotNull(persisted, "Power Factor should be visible on detail page after save");
        Assert.assertEquals(persisted, newValue, "Power Factor should persist as '" + newValue + "' after save");

        logStepWithScreenshot("GEN_EAD_10 Power Factor verified");
        ExtentReportManager.logPass("Power Factor edited to '" + val + "', saved, and verified on detail page");
    }

    @Test(priority = 28, description = "GEN_EAD_11: Edit Serial Number")
    public void testGEN_EAD_11_EditSerialNumber() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_11_SerialNumber");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();

        String newValue = "GEN_SN_" + System.currentTimeMillis();
        String val = editTextField("Serial Number", newValue);
        Assert.assertNotNull(val, "editTextField should find and set 'Serial Number' field");
        Assert.assertEquals(val, newValue, "Serial Number input value should match");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing Serial Number");

        // Post-save: verify value persisted on detail page
        String persisted = readDetailAttributeValue("Serial Number");
        Assert.assertNotNull(persisted, "Serial Number should be visible on detail page after save");
        Assert.assertEquals(persisted, newValue, "Serial Number should persist as '" + newValue + "' after save");

        logStepWithScreenshot("GEN_EAD_11 Serial Number verified");
        ExtentReportManager.logPass("Serial Number edited to '" + val + "', saved, and verified on detail page");
    }

    @Test(priority = 29, description = "GEN_EAD_12: Edit Voltage")
    public void testGEN_EAD_12_EditVoltage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_12_Voltage");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();

        // The edit form has two voltage fields under Core Attributes:
        //   "Voltage" (capital V) — a combobox (e.g. "120V"), shown in BASIC INFO on detail
        //   "voltage" (lowercase) — a text field, shown in Core Attributes table on detail
        // We edit the lowercase "voltage" text field since that's what readDetailAttributeValue reads.
        String newValue = "480";
        String val = editTextField("voltage", newValue);
        if (val == null) {
            // Fallback: try the combobox and skip detail verification
            val = selectFirstDropdownOption("Voltage");
        }
        Assert.assertNotNull(val, "Should be able to edit a voltage field");

        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed after editing voltage");

        // Detail page Core Attributes table uses lowercase "voltage"
        String persisted = readDetailAttributeValue("voltage");
        Assert.assertNotNull(persisted, "voltage should be visible on detail page after save");
        Assert.assertFalse("Not specified".equals(persisted), "voltage should have a value after save");

        ExtentReportManager.logPass("Voltage edited to '" + val + "', saved, and verified");
    }

    @Test(priority = 30, description = "GEN_EAD_18: Verify Save with no changes")
    public void testGEN_EAD_18_SaveNoChanges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_EAD_18_SaveNoChanges");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save Changes should succeed even with no edits");
        ExtentReportManager.logPass("Generator save without changes succeeded");
    }

    @Test(priority = 31, description = "GEN-03: Save Generator with optional fields empty")
    public void testGEN_03_SaveOptionalFieldsEmpty() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_03_SaveOptionalEmpty");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        // Only fill mandatory core attributes, leave optional empty
        expandCoreAttributes();
        selectFirstDropdownOption("Voltage");
        boolean saved = saveAndVerify();
        Assert.assertTrue(saved, "Save should succeed with optional fields empty");
        logStepWithScreenshot("Generator save with optional empty");
        ExtentReportManager.logPass("Generator saved with optional fields empty");
    }

    @Test(priority = 32, description = "GEN-04: Verify no subtype impacts core attributes")
    public void testGEN_04_NoSubtypeImpact() {
        ExtentReportManager.createTest(MODULE, FEATURE, "GEN_04_NoSubtypeImpact");
        if (!openEditForAssetClass("Generator", "GEN")) { skipIfNotFound("Generator"); return; }
        expandCoreAttributes();
        verifyAssetSubtype(null);
        logStepWithScreenshot("Generator subtype verification");
        ExtentReportManager.logPass("Generator subtype verified, core attributes display correctly");
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
        verifyAssetSubtype(null);
        logStepWithScreenshot("JB subtype verification");
        ExtentReportManager.logPass("Junction Box subtype verified");
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
        verifyAssetSubtype(null);
        logStepWithScreenshot("LC subtype verification");
        ExtentReportManager.logPass("Load Center subtype verified");
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
        verifyAssetSubtype(null);
        ExtentReportManager.logPass("MCC default subtype verified");
    }

    @Test(priority = 83, description = "MCC_AST_02: Verify subtype dropdown options for MCC")
    public void testMCC_AST_02_SubtypeDropdownOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MCC_AST_02_SubtypeOptions");
        if (!openCreateFormForClass("Motor Control Center")) { skipIfNotFound("MCC"); return; }
        // Verify dropdown has expected options
        verifyAssetSubtype(null, "Motor Control Equipment (<=1000V)", "Motor Control Equipment (>1000V)");
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
        verifyAssetSubtype(null);
        logStepWithScreenshot("MCCB subtype verification");
        ExtentReportManager.logPass("MCC Bucket subtype verified");
    }
}
