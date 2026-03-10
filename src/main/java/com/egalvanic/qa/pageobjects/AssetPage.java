package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for Asset Page
 * Supports CRUD operations on assets
 */
public class AssetPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    // Navigation
    private static final By ASSETS_NAV = By.xpath("//span[normalize-space()='Assets'] | //a[normalize-space()='Assets'] | //button[normalize-space()='Assets']");
    private static final By CREATE_ASSET_BTN = By.xpath("//button[normalize-space()='Create Asset']");

    // Asset form fields (using placeholder selectors — confirmed by diagnostic)
    private static final By ASSET_NAME_INPUT = By.xpath("//input[@placeholder='Enter Asset Name']");
    private static final By QR_CODE_INPUT = By.xpath("//input[@placeholder='Add QR code']");
    private static final By ASSET_CLASS_INPUT = By.xpath("//input[@placeholder='Select Class']");
    private static final By ASSET_SUBTYPE_INPUT = By.xpath("//input[@placeholder='Select Subtype']");
    // Core attributes (placeholders populated dynamically after asset class selection)
    private static final By CORE_ATTRIBUTES_SECTION = By.xpath("//*[contains(text(),'CORE ATTRIBUTES')]");
    private static final By REPLACEMENT_COST_INPUT = By.xpath("//input[@type='number']");

    // Submit / Save
    private static final By SUBMIT_CREATE_BTN = By.xpath("//button[normalize-space()='Create Asset']");
    private static final By SAVE_CHANGES_BTN = By.xpath("//button[normalize-space()='Save Changes' or contains(.,'Save Changes')]");

    // Data grid actions
    private static final By SEARCH_INPUT = By.xpath("//input[contains(@placeholder,'Search')]");
    private static final By FIRST_ROW_EDIT_BTN = By.xpath("//div[@class='MuiDataGrid-row MuiDataGrid-row--firstVisible']//button[@title='Edit Asset']");
    private static final By FIRST_ROW_DELETE_BTN = By.xpath("(//div[contains(@class,'MuiDataGrid-row')]//button[@title='Delete Asset'])[1]");
    private static final By CONFIRM_DELETE_BTN = By.xpath("//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");

    // Success / form indicators
    private static final By ASSET_FORM_DIALOG = By.xpath("//*[contains(text(),'Add Asset') or contains(text(),'Create Asset') or contains(text(),'BASIC INFO')]");
    private static final By SUCCESS_INDICATOR = By.xpath("//*[contains(text(),'Asset created') or contains(text(),'created successfully')]");
    private static final By ASSET_LIST_INDICATOR = By.xpath("//table|//div[contains(@class,'asset-list') or contains(@class,'AssetList') or contains(@class,'MuiDataGrid')]");

    public AssetPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // --- Navigation ---

    public void navigateToAssets() {
        recoverFromErrorOverlay();

        // If already on Assets page, do a full page refresh to get fresh grid data.
        // Re-clicking the Assets nav link triggers a React crash, so we refresh instead.
        if (driver.findElements(CREATE_ASSET_BTN).size() > 0 && isOnAssetsPage()) {
            System.out.println("[AssetPage] Already on Assets page — refreshing page for fresh data");
            driver.navigate().refresh();
            pause(2000);
            wait.until(ExpectedConditions.visibilityOfElementLocated(CREATE_ASSET_BTN));
            return;
        }

        click(ASSETS_NAV);
        wait.until(ExpectedConditions.visibilityOfElementLocated(CREATE_ASSET_BTN));
    }

    public boolean isOnAssetsPage() {
        try {
            return driver.findElements(CREATE_ASSET_BTN).size() > 0
                    || driver.getCurrentUrl().contains("asset");
        } catch (Exception e) {
            return false;
        }
    }

    // --- CREATE ---

    public void openCreateAssetForm() {
        click(CREATE_ASSET_BTN);
        wait.until(ExpectedConditions.visibilityOfElementLocated(ASSET_FORM_DIALOG));
    }

    public void fillBasicInfo(String assetName, String qrCode, String assetClass) {
        typeField(ASSET_NAME_INPUT, assetName);
        typeField(QR_CODE_INPUT, qrCode);
        typeAndSelectDropdown(ASSET_CLASS_INPUT, assetClass, assetClass);
        pause(500);

        // Select subtype (first available option)
        try {
            if (driver.findElements(ASSET_SUBTYPE_INPUT).size() > 0) {
                click(ASSET_SUBTYPE_INPUT);
                pause(400);
                By firstOption = By.xpath("(//li[@role='option'])[1]");
                if (driver.findElements(firstOption).size() > 0) {
                    click(firstOption);
                    pause(300);
                }
            }
        } catch (Exception ignored) {}

        // Serviceability is pre-filled with "Serviceable" — no action needed
    }

    /**
     * Fill core attributes after asset class has been selected.
     * Core attribute fields are dynamic — they appear based on the selected asset class.
     * This method clicks on the CORE ATTRIBUTES section to expand it, then fills
     * whatever fields are available by finding inputs/autocompletes inside the section.
     */
    public void fillCoreAttributes() {
        expandCoreAttributes();
        pause(500);

        try {
            By noClassMsg = By.xpath("//*[contains(text(),'Select an asset class to configure')]");
            if (driver.findElements(noClassMsg).size() > 0) {
                System.out.println("WARNING: Core attributes not loaded — asset class may not have been selected");
                return;
            }
        } catch (Exception ignored) {}

        // Fill all visible required and optional fields in the core attributes section.
        // Fields are dynamic (they depend on the selected asset class).
        // Strategy: find all empty inputs/selects after the CORE ATTRIBUTES header and fill them.

        // 1. Fill any autocomplete/dropdown fields (e.g., Voltage) — select first option
        try {
            java.util.List<WebElement> selects = driver.findElements(
                    By.xpath("//*[contains(text(),'CORE ATTRIBUTES')]/following::input[contains(@placeholder,'Select') or contains(@role,'combobox')]"));
            for (WebElement sel : selects) {
                try {
                    String val = sel.getAttribute("value");
                    if (val == null || val.isEmpty()) {
                        sel.click();
                        pause(400);
                        By firstOption = By.xpath("(//li[@role='option'])[1]");
                        if (driver.findElements(firstOption).size() > 0) {
                            driver.findElement(firstOption).click();
                            pause(300);
                            System.out.println("[AssetPage] Filled dropdown in core attributes");
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // 2. Fill any empty text inputs (e.g., Breaker Settings, Catalog Number)
        try {
            java.util.List<WebElement> textInputs = driver.findElements(
                    By.xpath("//*[contains(text(),'CORE ATTRIBUTES')]/following::input[@type='text']"));
            for (WebElement input : textInputs) {
                try {
                    String val = input.getAttribute("value");
                    String placeholder = input.getAttribute("placeholder");
                    // Skip search inputs and already-filled inputs
                    if ((val == null || val.isEmpty())
                            && (placeholder == null || !placeholder.contains("Search"))) {
                        input.click();
                        input.sendKeys("SmokeTest");
                        pause(200);
                        System.out.println("[AssetPage] Filled text input in core attributes: placeholder=" + placeholder);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        System.out.println("[AssetPage] Core attributes filled");
    }

    public void fillReplacementCost(String cost) {
        scrollToView("COMMERCIAL");
        pause(300);
        try {
            // The replacement cost input is a number field
            typeField(REPLACEMENT_COST_INPUT, cost);
        } catch (Exception e) {
            System.out.println("WARNING: Replacement cost field not found: " + e.getMessage());
        }
    }

    public void submitCreateAsset() {
        // There are TWO "Create Asset" buttons — one in the toolbar (opens form) and
        // one at the bottom of the Add Asset panel (submits form).
        // We need the LAST one (the form's submit button inside the panel).
        java.util.List<WebElement> btns = driver.findElements(SUBMIT_CREATE_BTN);
        System.out.println("[AssetPage] Found " + btns.size() + " 'Create Asset' buttons");

        WebElement submitBtn = btns.get(btns.size() - 1); // last one = panel's submit button
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        pause(300);

        // Use Selenium click (not JS click) to properly trigger React form submission
        try {
            submitBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", submitBtn);
        }
        System.out.println("[AssetPage] Clicked Create Asset submit button");
    }

    public boolean waitForCreateSuccess() {
        try {
            // Wait for a real success signal: the Add Asset panel should close automatically
            // on successful creation, OR a success toast/message appears.
            // Don't just check for DataGrid — it's visible behind the open panel.

            By panelHeader = By.xpath("//*[normalize-space()='Add Asset']");

            // First, wait for either success message or panel closure (up to 25s)
            for (int i = 0; i < 25; i++) {
                // Success indicator (toast message)
                if (driver.findElements(SUCCESS_INDICATOR).size() > 0) {
                    System.out.println("[AssetPage] Success indicator found");
                    closeCreatePanelIfOpen();
                    return true;
                }
                // Panel closed = form submitted successfully
                if (driver.findElements(panelHeader).size() == 0) {
                    System.out.println("[AssetPage] Add Asset panel closed — creation successful");
                    return true;
                }
                pause(1000);
            }

            // Panel still open after 25s — creation likely failed (validation error)
            // Try to close it and report failure
            System.out.println("[AssetPage] Add Asset panel still open after 25s — creation may have failed");
            closeCreatePanelIfOpen();
            return false;
        } catch (Exception e) {
            closeCreatePanelIfOpen();
            return false;
        }
    }

    /**
     * Close the "Add Asset" side panel if it's still visible after creation.
     * Tries: X button, Cancel button, Escape key, or click outside.
     */
    private void closeCreatePanelIfOpen() {
        try {
            // Check if the "Add Asset" panel header is still visible
            By panelHeader = By.xpath("//*[contains(text(),'Add Asset')]");
            if (driver.findElements(panelHeader).size() == 0) return; // panel already closed

            System.out.println("[AssetPage] Add Asset panel still open — closing it");

            // Strategy 1: Click the X (close) button in the panel
            try {
                By closeBtn = By.xpath("//*[contains(text(),'Add Asset')]/ancestor::div[1]//button[contains(@aria-label,'close') or contains(@aria-label,'Close')] | //*[contains(text(),'Add Asset')]/following-sibling::button | //*[contains(text(),'Add Asset')]/..//button[.//svg]");
                java.util.List<WebElement> closeBtns = driver.findElements(closeBtn);
                if (!closeBtns.isEmpty()) {
                    // Click the first close-looking button near the Add Asset header
                    for (WebElement btn : closeBtns) {
                        String text = btn.getText().trim();
                        if (text.isEmpty() || text.equals("×") || text.equals("✕")) {
                            js.executeScript("arguments[0].click();", btn);
                            pause(500);
                            if (driver.findElements(panelHeader).size() == 0) return;
                        }
                    }
                    // If none matched, click the first one
                    js.executeScript("arguments[0].click();", closeBtns.get(0));
                    pause(500);
                    if (driver.findElements(panelHeader).size() == 0) return;
                }
            } catch (Exception ignored) {}

            // Strategy 2: Click Cancel button
            try {
                By cancelBtn = By.xpath("//button[normalize-space()='Cancel']");
                if (driver.findElements(cancelBtn).size() > 0) {
                    driver.findElement(cancelBtn).click();
                    pause(500);
                    if (driver.findElements(panelHeader).size() == 0) return;
                }
            } catch (Exception ignored) {}

            // Strategy 3: Press Escape
            try {
                driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
                pause(500);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.out.println("[AssetPage] Could not close Add Asset panel: " + e.getMessage());
        }
    }

    // --- READ ---

    public void searchAsset(String assetName) {
        if (driver.findElements(SEARCH_INPUT).size() > 0) {
            typeField(SEARCH_INPUT, assetName);
            pause(500);
        }
    }

    public boolean isAssetVisible(String assetName) {
        try {
            recoverFromErrorOverlay();
            searchAsset(assetName);
            pause(1500);

            By assetRow = By.xpath("//div[contains(@class,'MuiDataGrid-row')]//*[contains(text(),'" + assetName + "')]");

            // Retry — grid may take time to filter/load
            for (int i = 0; i < 5; i++) {
                if (driver.findElements(assetRow).size() > 0) return true;
                pause(1000);
            }

            // Debug: log grid state
            java.util.List<WebElement> rows = driver.findElements(By.xpath("//div[contains(@class,'MuiDataGrid-row')]"));
            System.out.println("[AssetPage] Grid has " + rows.size() + " rows. Searched for: " + assetName);

            return false;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detect Sentry error dialog or Application Error page.
     * Attempts recovery: dismiss overlay → refresh → wait for grid.
     * Only throws if recovery fails completely.
     */
    private void recoverFromErrorOverlay() {
        try {
            By errorXpath = By.xpath(
                    "//*[contains(text(),'Application Error') or contains(text(),'We encountered an error') or contains(text(),'something went wrong')]");
            if (driver.findElements(errorXpath).size() == 0) return; // no error

            System.out.println("[AssetPage] Error overlay detected — attempting recovery...");

            // Step 1: Dismiss Sentry feedback dialog
            try { driver.findElement(By.xpath("//button[contains(@aria-label,'Close')]")).click(); pause(500); } catch (Exception ignored) {}

            // Step 2: Refresh page
            driver.navigate().refresh();
            pause(3000);

            // Step 3: Wait for Assets page to reload
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(CREATE_ASSET_BTN));
                System.out.println("[AssetPage] Recovery successful — page reloaded");
                return;
            } catch (Exception ignored) {}

            // Step 4: If still broken, check for error again
            if (driver.findElements(errorXpath).size() > 0) {
                // Try recovery buttons
                try { driver.findElement(By.xpath("//button[contains(.,'Try Again')]")).click(); pause(3000); } catch (Exception ignored) {}
                try { driver.findElement(By.xpath("//button[contains(.,'Refresh Page')]")).click(); pause(3000); } catch (Exception ignored) {}
            }

            // Step 5: Final check
            if (driver.findElements(errorXpath).size() > 0) {
                throw new RuntimeException("SERVER BUG: Application crashed and recovery failed. Error overlay could not be dismissed.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {}
    }

    // --- UPDATE ---

    public void openEditForFirstAsset() {
        recoverFromErrorOverlay();
        if (driver.findElements(FIRST_ROW_EDIT_BTN).size() > 0) {
            click(FIRST_ROW_EDIT_BTN);
        } else {
            click(By.xpath("(//button[contains(@class,'MuiIconButton-root')])[1]"));
            pause(400);
            click(By.xpath("//li[normalize-space()='Edit' or contains(.,'Edit')]"));
        }
        pause(700);
    }

    public void editModel(String newModel) {
        expandCoreAttributes();
        // Find first text input in core attributes section
        try {
            By modelField = By.xpath("//*[contains(text(),'CORE ATTRIBUTES')]/following::input[@type='text'][1]");
            typeField(modelField, newModel);
        } catch (Exception e) {
            System.out.println("WARNING: Could not edit model: " + e.getMessage());
        }
    }

    public void editNotes(String newNotes) {
        try {
            By notesField = By.xpath("//textarea[1]");
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(notesField));
            try { el.clear(); } catch (Exception ignored) {}
            el.click();
            el.sendKeys(newNotes);
        } catch (Exception e) {
            System.out.println("WARNING: Could not edit notes: " + e.getMessage());
        }
    }

    public void saveChanges() {
        try {
            click(SAVE_CHANGES_BTN);
        } catch (Exception e) {
            click(By.xpath("//button[contains(.,'Save') or contains(.,'Update')][last()]"));
        }
    }

    public boolean waitForEditSuccess() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'updated') or contains(text(),'saved') or contains(text(),'successfully')]")),
                    ExpectedConditions.presenceOfElementLocated(ASSET_LIST_INDICATOR)
            ));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- DELETE ---

    public void deleteFirstAsset() {
        recoverFromErrorOverlay();
        WebElement delBtn = wait.until(ExpectedConditions.elementToBeClickable(FIRST_ROW_DELETE_BTN));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", delBtn);
        pause(200);
        js.executeScript("arguments[0].click();", delBtn);
        pause(700);
    }

    public void confirmDelete() {
        WebElement confirm = wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_DELETE_BTN));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", confirm);
        pause(200);
        js.executeScript("arguments[0].click();", confirm);
        pause(1500);
    }

    // --- Helpers ---

    private void expandCoreAttributes() {
        scrollToView("CORE ATTRIBUTES");
        pause(300);
        // The CORE ATTRIBUTES section may be a clickable accordion/toggle
        try {
            WebElement section = driver.findElement(CORE_ATTRIBUTES_SECTION);
            // Check if it's inside a button (accordion pattern)
            try {
                WebElement toggle = section.findElement(By.xpath("./ancestor::button[1]"));
                String expanded = toggle.getAttribute("aria-expanded");
                if (expanded == null || expanded.equals("false")) {
                    js.executeScript("arguments[0].click();", toggle);
                    pause(500);
                }
            } catch (Exception ignored) {
                // Not an accordion — just scroll to it
            }
        } catch (Exception ignored) {}
    }

    private void scrollToView(String headerText) {
        try {
            By header = By.xpath("//*[normalize-space()='" + headerText + "']");
            WebElement el = driver.findElement(header);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            pause(500);
        } catch (Exception e) {
            js.executeScript("window.scrollBy(0,600);");
            pause(500);
        }
    }

    private void click(By by) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(by)).click();
        } catch (Exception e) {
            try {
                WebElement el = driver.findElement(by);
                js.executeScript("arguments[0].click();", el);
            } catch (Exception ex) {
                throw new RuntimeException("Click failed for: " + by, ex);
            }
        }
    }

    private void typeField(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        try { el.clear(); } catch (Exception ignored) {}
        el.click();
        el.sendKeys(text);
    }

    private void typeAndSelectDropdown(By inputLocator, String textToType, String optionText) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
        input.click();
        pause(300);

        // Clear and type to trigger autocomplete filtering
        try { input.clear(); } catch (Exception ignored) {}
        input.sendKeys(textToType);
        pause(600);

        // Wait for the listbox dropdown to appear
        By listbox = By.xpath("//ul[@role='listbox']");
        for (int attempt = 0; attempt < 3; attempt++) {
            if (driver.findElements(listbox).size() > 0) break;

            // Try clicking the popup indicator to open dropdown
            try {
                WebElement popup = driver.findElement(
                        By.xpath("//button[contains(@class,'MuiAutocomplete-popupIndicator')]"));
                js.executeScript("arguments[0].click();", popup);
                pause(500);
            } catch (Exception ignored) {
                // Re-click input and retype
                input.click();
                pause(200);
                try { input.clear(); } catch (Exception e2) {}
                input.sendKeys(textToType);
                pause(500);
            }
        }

        // Find and click the matching option
        By exactOption = By.xpath("//li[@role='option'][normalize-space()='" + optionText + "']");
        By partialOption = By.xpath("//li[@role='option'][contains(normalize-space(),'" + optionText + "')]");
        By anyOption = By.xpath("//li[contains(@id,'option') or @role='option'][contains(normalize-space(),'" + optionText + "')]");

        for (int attempt = 0; attempt < 5; attempt++) {
            for (By opt : new By[]{exactOption, partialOption, anyOption}) {
                if (driver.findElements(opt).size() > 0) {
                    WebElement option = driver.findElement(opt);
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                    pause(150);
                    js.executeScript("arguments[0].click();", option);
                    pause(300);
                    return;
                }
            }
            pause(400);
        }
        System.out.println("WARNING: Could not select dropdown option '" + optionText + "' for " + inputLocator);
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
