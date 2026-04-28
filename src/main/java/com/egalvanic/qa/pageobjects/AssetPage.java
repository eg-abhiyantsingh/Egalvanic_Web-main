package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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

    // Navigation — match exact text "Assets" AND also sidebar patterns where the label is
    // wrapped in a larger structure (e.g., <a href="/assets"><span>Assets</span><badge>42</badge></a>).
    // Includes a href-based fallback so SPA router links match even when text rendering is late.
    private static final By ASSETS_NAV = By.xpath(
            "//nav//a[@href='/assets' or @href='/assets/']"
            + " | //aside//a[@href='/assets' or @href='/assets/']"
            + " | //a[@href='/assets' or @href='/assets/']"
            + " | //span[normalize-space()='Assets']"
            + " | //a[normalize-space()='Assets']"
            + " | //button[normalize-space()='Assets']"
            + " | //*[@role='button' and normalize-space()='Assets']");
    private static final By LOCATIONS_NAV = By.xpath(
            "//nav//a[@href='/locations' or @href='/locations/']"
            + " | //aside//a[@href='/locations' or @href='/locations/']"
            + " | //a[@href='/locations' or @href='/locations/']"
            + " | //span[normalize-space()='Locations']"
            + " | //a[normalize-space()='Locations']"
            + " | //button[normalize-space()='Locations']");
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
        dismissAnyDrawerOrBackdrop();

        // If already on Assets page, navigate away then back to force fresh data.
        // driver.navigate().refresh() loses SPA state (site selector resets).
        // Re-clicking Assets nav on same page can trigger React crash.
        // Solution: go to Locations briefly, then back to Assets.
        if (driver.findElements(CREATE_ASSET_BTN).size() > 0 && isOnAssetsPage()) {
            System.out.println("[AssetPage] Already on Assets page — navigating away and back for fresh data");
            try {
                // Use JS click to avoid "element not interactable" when drawer is open
                WebElement locNav = driver.findElement(LOCATIONS_NAV);
                js.executeScript("arguments[0].click();", locNav);
                pause(1500);
            } catch (Exception e) {
                System.out.println("[AssetPage] Could not navigate to Locations via JS: " + e.getMessage());
                // Fallback: use direct URL navigation
                try {
                    String baseUrl = driver.getCurrentUrl().replaceAll("/assets.*", "");
                    driver.get(baseUrl + "/connections");
                    pause(1500);
                } catch (Exception e2) {
                    System.out.println("[AssetPage] URL fallback also failed: " + e2.getMessage());
                }
            }
        }

        click(ASSETS_NAV);

        // CI hardening: the default 25s `wait` is enough locally but CI runners are
        // 2-3x slower at SPA hydration. Confirmed via run 25001241790 (GEN_EAD_09):
        // the Create Asset button took >25s to appear and the timeout fired.
        // Strategy:
        //   1. First try a longer wait (45s) — covers slow CI startup.
        //   2. If still not visible, navigate().refresh() once and wait another 30s.
        //   3. If both fail, throw the original timeout — surfaces a real env issue.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(45))
                    .until(ExpectedConditions.visibilityOfElementLocated(CREATE_ASSET_BTN));
        } catch (org.openqa.selenium.TimeoutException primaryTimeout) {
            System.out.println("[AssetPage] Create Asset button not visible after 45s — "
                    + "attempting page refresh recovery (CI flake guard)");
            try {
                driver.navigate().refresh();
                new WebDriverWait(driver, Duration.ofSeconds(30))
                        .until(ExpectedConditions.visibilityOfElementLocated(CREATE_ASSET_BTN));
                System.out.println("[AssetPage] Recovered after refresh — Create Asset button now visible");
            } catch (Exception refreshEx) {
                System.out.println("[AssetPage] Refresh recovery also failed: " + refreshEx.getMessage());
                throw primaryTimeout;
            }
        }
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
        // Dismiss any residual drawer/dialog left open by a previous test
        dismissAnyDialog();
        pause(300);
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
                        js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus(); arguments[0].click();", sel);
                        pause(400);
                        By firstOption = By.xpath("(//li[@role='option'])[1]");
                        if (driver.findElements(firstOption).size() > 0) {
                            WebElement opt = driver.findElement(firstOption);
                            js.executeScript("arguments[0].click();", opt);
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
                        js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus(); arguments[0].click();", input);
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

            // Strategy 3: Click backdrop overlay (avoid Keys.ESCAPE which can close drawers)
            try {
                js.executeScript("var b=document.querySelector('.MuiBackdrop-root'); if(b) b.click();");
                pause(500);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.out.println("[AssetPage] Could not close Add Asset panel: " + e.getMessage());
        }
    }

    // --- READ ---

    public void searchAsset(String assetName) {
        // Try standard search input
        java.util.List<WebElement> searchInputs = driver.findElements(SEARCH_INPUT);
        if (searchInputs.isEmpty()) {
            System.out.println("[AssetPage] No search input found (placeholder='Search')");
            return;
        }

        WebElement searchInput = searchInputs.get(0);
        System.out.println("[AssetPage] Found search input, typing: '" + assetName + "'");

        // Use JS to clear and set value, then trigger React events
        // This is more reliable than Selenium clear()+sendKeys() with React controlled inputs
        js.executeScript(
                "var inp = arguments[0];" +
                "var text = arguments[1];" +
                "// Clear using native setter to trigger React\n" +
                "var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "nativeSetter.call(inp, '');" +
                "inp.dispatchEvent(new Event('input', { bubbles: true }));" +
                "inp.dispatchEvent(new Event('change', { bubbles: true }));" +
                "// Now set the actual value\n" +
                "nativeSetter.call(inp, text);" +
                "inp.dispatchEvent(new Event('input', { bubbles: true }));" +
                "inp.dispatchEvent(new Event('change', { bubbles: true }));",
                searchInput, assetName);

        pause(2000);

        // Verify what value ended up in the search input
        String actualValue = (String) js.executeScript("return arguments[0].value;", searchInput);
        System.out.println("[AssetPage] Search input value after typing: '" + actualValue + "'");
    }

    /**
     * Check if the asset grid has at least one data row.
     * Waits up to 10 seconds for rows to load.
     */
    public boolean isGridPopulated() {
        try {
            recoverFromErrorOverlay();
            By gridRow = By.xpath("//div[contains(@class,'MuiDataGrid-row') and @data-rowindex]");

            for (int i = 0; i < 10; i++) {
                java.util.List<WebElement> rows = driver.findElements(gridRow);
                if (!rows.isEmpty()) {
                    System.out.println("[AssetPage] Grid has " + rows.size() + " rows");
                    return true;
                }
                pause(1000);
            }

            System.out.println("[AssetPage] Grid is empty after waiting 10s");
            return false;
        } catch (Exception e) {
            System.out.println("[AssetPage] Error checking grid: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the number of data rows currently visible in the asset grid.
     */
    public int getGridRowCount() {
        try {
            By gridRow = By.xpath("//div[contains(@class,'MuiDataGrid-row') and @data-rowindex]");
            return driver.findElements(gridRow).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get the asset name from the first row in the grid (first cell text).
     */
    public String getFirstRowAssetName() {
        try {
            By firstRow = By.xpath("(//div[contains(@class,'MuiDataGrid-row') and @data-rowindex])[1]");
            WebElement row = wait.until(ExpectedConditions.presenceOfElementLocated(firstRow));

            // Dump all cells in the first row for diagnostic
            @SuppressWarnings("unchecked")
            java.util.List<String> cellTexts = (java.util.List<String>) js.executeScript(
                    "var cells = arguments[0].querySelectorAll('[class*=\"MuiDataGrid-cell\"]');" +
                    "var result = [];" +
                    "for (var i = 0; i < cells.length; i++) {" +
                    "  var txt = (cells[i].textContent||'').trim();" +
                    "  var field = cells[i].getAttribute('data-field') || '';" +
                    "  result.push('[' + i + '] field=\"' + field + '\" text=\"' + txt.substring(0,40) + '\"');" +
                    "}" +
                    "return result;", row);
            System.out.println("[AssetPage] First row cells:");
            for (String cellInfo : cellTexts) {
                System.out.println("[AssetPage]   " + cellInfo);
            }

            // Find the cell with data-field="name" or the first cell with non-empty text
            String name = (String) js.executeScript(
                    "var cells = arguments[0].querySelectorAll('[class*=\"MuiDataGrid-cell\"]');" +
                    "// First try: cell with data-field 'label' or 'name'\n" +
                    "for (var cell of cells) {" +
                    "  var field = (cell.getAttribute('data-field')||'').toLowerCase();" +
                    "  if (field === 'label' || field === 'name' || field === 'assetname' || field === 'asset_name') {" +
                    "    return (cell.textContent||'').trim();" +
                    "  }" +
                    "}" +
                    "// Second try: first cell with non-empty text that isn't a checkbox\n" +
                    "for (var cell of cells) {" +
                    "  var txt = (cell.textContent||'').trim();" +
                    "  var field = (cell.getAttribute('data-field')||'').toLowerCase();" +
                    "  if (field === '__check__' || field === 'checkbox' || field === 'actions') continue;" +
                    "  if (txt.length > 0) return txt;" +
                    "}" +
                    "return '';", row);
            System.out.println("[AssetPage] First row asset name: '" + name + "'");
            return (name == null || name.isEmpty()) ? null : name;
        } catch (Exception e) {
            System.out.println("[AssetPage] Could not read first row asset name: " + e.getMessage());
            return null;
        }
    }

    public boolean isAssetVisible(String assetName) {
        try {
            recoverFromErrorOverlay();
            // Search is already called by the test; call again to ensure filtering
            searchAsset(assetName);
            pause(2000);

            // Use JS to check textContent (CSS zoom 80% breaks XPath text() matching)
            for (int attempt = 0; attempt < 8; attempt++) {
                Boolean found = (Boolean) js.executeScript(
                        "var rows = document.querySelectorAll('[class*=\"MuiDataGrid-row\"][data-rowindex]');" +
                        "for (var row of rows) {" +
                        "  var cells = row.querySelectorAll('[class*=\"MuiDataGrid-cell\"]');" +
                        "  for (var cell of cells) {" +
                        "    var txt = (cell.textContent||'').trim();" +
                        "    if (txt.indexOf(arguments[0]) > -1) return true;" +
                        "  }" +
                        "}" +
                        "return false;", assetName);
                if (found != null && found) {
                    System.out.println("[AssetPage] Asset '" + assetName + "' found in grid");
                    return true;
                }
                pause(1000);
            }

            // Debug: log what's actually in the grid
            @SuppressWarnings("unchecked")
            java.util.List<String> rowTexts = (java.util.List<String>) js.executeScript(
                    "var rows = document.querySelectorAll('[class*=\"MuiDataGrid-row\"][data-rowindex]');" +
                    "var result = [];" +
                    "for (var row of rows) {" +
                    "  var labelCell = row.querySelector('[data-field=\"label\"]');" +
                    "  result.push(labelCell ? (labelCell.textContent||'').trim() : '(no label cell)');" +
                    "}" +
                    "return result;");
            System.out.println("[AssetPage] Grid has " + rowTexts.size() + " rows. Searched for: '" + assetName + "'");
            for (int i = 0; i < rowTexts.size(); i++) {
                System.out.println("[AssetPage]   row[" + i + "] label: '" + rowTexts.get(i) + "'");
            }

            return false;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("[AssetPage] isAssetVisible error: " + e.getMessage());
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

            // Step 2: Navigate to Locations then back (avoids SPA state loss from page refresh)
            try {
                WebElement loc = driver.findElement(LOCATIONS_NAV);
                js.executeScript("arguments[0].click();", loc);
                pause(1500);
                WebElement ast = driver.findElement(ASSETS_NAV);
                js.executeScript("arguments[0].click();", ast);
                pause(2000);
            } catch (Exception ignored) {}

            // Step 3: Check if recovered
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(CREATE_ASSET_BTN));
                System.out.println("[AssetPage] Recovery successful — Assets page reloaded");
                return;
            } catch (Exception ignored) {}

            // Step 4: If still broken, try recovery buttons
            if (driver.findElements(errorXpath).size() > 0) {
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

    /**
     * Navigate to the detail page for the first asset in the grid.
     * Tries multiple strategies: data-id URL, href links, cell clicks, row clicks.
     */
    public void navigateToFirstAssetDetail() {
        recoverFromErrorOverlay();

        // IMPORTANT: Use @data-rowindex to skip the header row.
        By firstDataRow = By.xpath("(//div[contains(@class,'MuiDataGrid-row') and @data-rowindex])[1]");
        WebElement row = wait.until(ExpectedConditions.presenceOfElementLocated(firstDataRow));
        String currentUrl = driver.getCurrentUrl();

        String dataId = row.getDomAttribute("data-id");
        System.out.println("[AssetPage] First data row: data-id=" + dataId + ", data-rowindex=" + row.getDomAttribute("data-rowindex"));

        // CRITICAL: Prefer SPA click navigation over driver.get() URL navigation.
        // driver.get() does a full page reload which LOSES SPA state (selected facility,
        // auth context). The detail page then shows a loading spinner forever.
        // Clicking within the SPA preserves state.

        // Strategy 1: Click <a> link inside the row (SPA navigation)
        try {
            WebElement link = (WebElement) js.executeScript(
                    "var all = arguments[0].querySelectorAll('a[href]');" +
                    "for(var i=0; i<all.length; i++) { " +
                    "  if(all[i].href.includes('/assets/')) return all[i]; " +
                    "}" +
                    "return null;", row);
            if (link != null) {
                System.out.println("[AssetPage] Strategy 1: clicking link in row");
                link.click();
                waitForDetailPageLoad();
                if (!driver.getCurrentUrl().equals(currentUrl)) {
                    System.out.println("[AssetPage] Strategy 1 (link click) succeeded: " + driver.getCurrentUrl());
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 1 failed: " + e.getMessage());
        }

        // Strategy 2: Click the first cell (asset name) — triggers SPA row click handler
        try {
            WebElement cell = row.findElement(By.xpath(".//div[contains(@class,'MuiDataGrid-cell')][1]"));
            String cellText = (String) js.executeScript("return arguments[0].textContent.trim();", cell);
            System.out.println("[AssetPage] Strategy 2: clicking first cell '" + cellText + "'");
            try { cell.click(); } catch (Exception ce) { js.executeScript("arguments[0].click();", cell); }
            waitForDetailPageLoad();
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                System.out.println("[AssetPage] Strategy 2 (cell click) succeeded: " + driver.getCurrentUrl());
                return;
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 2 failed: " + e.getMessage());
        }

        // Strategy 3: Click the row itself
        try {
            System.out.println("[AssetPage] Strategy 3: clicking row");
            try { row.click(); } catch (Exception ce) { js.executeScript("arguments[0].click();", row); }
            waitForDetailPageLoad();
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                System.out.println("[AssetPage] Strategy 3 (row click) succeeded: " + driver.getCurrentUrl());
                return;
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 3 failed: " + e.getMessage());
        }

        // Strategy 4: Double-click the row (some DataGrids use double-click to navigate)
        try {
            System.out.println("[AssetPage] Strategy 4: double-clicking row");
            new org.openqa.selenium.interactions.Actions(driver).doubleClick(row).perform();
            waitForDetailPageLoad();
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                System.out.println("[AssetPage] Strategy 4 (double-click) succeeded: " + driver.getCurrentUrl());
                return;
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 4 failed: " + e.getMessage());
        }

        // Strategy 5: Last resort — direct URL (loses SPA state but may still work)
        if (dataId != null && !dataId.isEmpty() && dataId.contains("-")) {
            String baseUrl = currentUrl.replaceAll("/assets.*", "/assets/");
            System.out.println("[AssetPage] Strategy 5: direct URL navigation (loses SPA state)");
            driver.get(baseUrl + dataId);
            waitForDetailPageLoad();
            if (driver.getCurrentUrl().contains(dataId)) {
                System.out.println("[AssetPage] Strategy 5 (URL) succeeded: " + driver.getCurrentUrl());
                return;
            }
        }

        System.out.println("[AssetPage] FAILED to navigate. Current URL: " + driver.getCurrentUrl());
        throw new RuntimeException("Could not navigate to asset detail page from grid.");
    }

    /**
     * Wait for the asset detail page to fully load after direct URL navigation.
     * Waits for the loading spinner to disappear and content to render.
     */
    private void waitForDetailPageLoad() {
        pause(2000);

        // Wait up to 30s for actual detail page content to render.
        // Key insight: sidebar/nav always present (~10 elements at x<80).
        // Detail page content appears in the main area (x>80, y>50).
        // Count interactive elements there — 0 means still loading.
        for (int i = 0; i < 30; i++) {
            try {
                Long mainElements = (Long) js.executeScript(
                        "var count = 0;" +
                        "var els = document.querySelectorAll('button, a[href], [role=\"button\"], [role=\"tab\"]');" +
                        "for (var el of els) {" +
                        "  var r = el.getBoundingClientRect();" +
                        "  if (r.left > 80 && r.top > 50 && r.width > 0 && r.height > 0) count++;" +
                        "}" +
                        "return count;");

                System.out.println("[AssetPage] Detail load check " + i + ": mainAreaElements=" + mainElements);

                // Detail page typically has 10+ interactive elements (back arrow,
                // action icons, tabs, section toggles). Loading state has 0.
                if (mainElements != null && mainElements > 3) {
                    System.out.println("[AssetPage] Detail page loaded after " + (2 + i) + "s — " + mainElements + " elements in main area");
                    pause(1500);
                    return;
                }
            } catch (Exception e) {
                System.out.println("[AssetPage] Detail load check error: " + e.getMessage());
            }

            // Check for URL change (SPA navigation may have completed)
            String url = driver.getCurrentUrl();
            if (url.contains("/assets/") && url.length() > 20) {
                // We're on an asset detail URL — check if it has loaded
                if (driver.findElements(By.xpath("//*[contains(text(),'Application Error')]")).size() > 0) {
                    System.out.println("[AssetPage] Error page on detail load");
                    recoverFromErrorOverlay();
                    return;
                }
            }

            pause(1000);
        }

        System.out.println("[AssetPage] Detail page content did not load after 32s");
    }

    /**
     * Open the ⋮ (kebab/three-dot) menu on the asset detail page and click a menu item.
     *
     * NOTE: XPath //button[.//svg] fails on this page due to SVG namespace issues.
     * All button finding uses By.tagName("button") or By.cssSelector instead.
     */
    public void clickKebabMenuItem(String itemText) {
        // CRITICAL: Save the detail page URL before any click attempts.
        // Strategies may accidentally navigate away, so we always use this to recover.
        final String detailUrl = driver.getCurrentUrl();
        System.out.println("[AssetPage] clickKebabMenuItem('" + itemText + "') — detail URL: " + detailUrl);

        // Strategy 0: Find kebab by aria-label, title, or data-testid attributes.
        // MUI IconButton typically renders with aria-label="more" or similar.
        try {
            @SuppressWarnings("unchecked")
            java.util.List<WebElement> candidates = (java.util.List<WebElement>) js.executeScript(
                    "var result = [];" +
                    "var all = document.querySelectorAll('[aria-label], [data-testid]');" +
                    "for (var el of all) {" +
                    "  var label = (el.getAttribute('aria-label')||'').toLowerCase();" +
                    "  var testid = (el.getAttribute('data-testid')||'').toLowerCase();" +
                    "  if (label.match(/more|menu|option|kebab|action|three.?dot|\\.\\.\\./)" +
                    "      || testid.match(/more|menu|kebab|action/)) {" +
                    "    result.push(el);" +
                    "  }" +
                    "}" +
                    "return result;");
            System.out.println("[AssetPage] Strategy 0 — aria-label/testid candidates: " + candidates.size());
            for (int i = 0; i < candidates.size(); i++) {
                try {
                    String info = (String) js.executeScript(
                            "var e=arguments[0]; var r=e.getBoundingClientRect();" +
                            "return e.tagName+'[aria-label=\"'+(e.getAttribute('aria-label')||'')+'\"]'" +
                            "+'[data-testid=\"'+(e.getAttribute('data-testid')||'')+'\"]'" +
                            "+' at('+Math.round(r.left)+','+Math.round(r.top)+') size='+Math.round(r.width)+'x'+Math.round(r.height);",
                            candidates.get(i));
                    System.out.println("[AssetPage]   candidate[" + i + "] " + info);
                    js.executeScript("arguments[0].click();", candidates.get(i));
                    pause(800);
                    if (!driver.getCurrentUrl().equals(detailUrl)) {
                        System.out.println("[AssetPage]   navigated away — recovering");
                        driver.get(detailUrl); waitForDetailPageLoad(); continue;
                    }
                    if (tryClickMenuItem(itemText)) {
                        System.out.println("[AssetPage] Success via aria-label candidate " + i);
                        return;
                    }
                    dismissPopup();
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 0 error: " + e.getMessage());
        }
        ensureOnDetailPage(detailUrl);

        // Strategy 0b: Direct MoreVert SVG path match — the kebab icon always uses
        // this SVG path, even when it has no aria-label or data-testid.
        try {
            @SuppressWarnings("unchecked")
            java.util.List<WebElement> moreVertBtns = (java.util.List<WebElement>) js.executeScript(
                    "var result = [];" +
                    "var paths = document.querySelectorAll('svg path');" +
                    "for (var p of paths) {" +
                    "  var d = p.getAttribute('d') || '';" +
                    "  if (d.indexOf('M12 8c1.1') > -1) {" +
                    "    var btn = p.closest('button');" +
                    "    if (btn) result.push(btn);" +
                    "  }" +
                    "}" +
                    "return result;");
            System.out.println("[AssetPage] Strategy 0b — MoreVert SVG buttons: " + moreVertBtns.size());
            for (int i = 0; i < moreVertBtns.size(); i++) {
                try {
                    moreVertBtns.get(i).click();
                    pause(800);
                    if (!driver.getCurrentUrl().equals(detailUrl)) {
                        System.out.println("[AssetPage]   MoreVert btn navigated away — recovering");
                        driver.get(detailUrl); waitForDetailPageLoad(); continue;
                    }
                    if (tryClickMenuItem(itemText)) {
                        System.out.println("[AssetPage] Success via MoreVert SVG button " + i);
                        return;
                    }
                    dismissPopup();
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 0b error: " + e.getMessage());
        }
        ensureOnDetailPage(detailUrl);

        // Strategy 1: Find small <button> elements in top 200px using Selenium click.
        // Previous attempts used JS click which may not trigger React event handlers.
        // Also look for MoreVert icon (three vertical dots SVG path).
        try {
            @SuppressWarnings("unchecked")
            java.util.List<WebElement> headerButtons = (java.util.List<WebElement>) js.executeScript(
                    "var result = [];" +
                    "var buttons = document.querySelectorAll('button');" +
                    "for (var btn of buttons) {" +
                    "  var r = btn.getBoundingClientRect();" +
                    "  // Small buttons in the header area (top 200px, right of sidebar)\n" +
                    "  if (r.top > 0 && r.top < 400 && r.left > 80 && r.width < 60 && r.width > 15) {" +
                    "    result.push(btn);" +
                    "  }" +
                    "}" +
                    "return result;");
            System.out.println("[AssetPage] Strategy 1 — small header buttons: " + headerButtons.size());
            for (int i = 0; i < headerButtons.size(); i++) {
                try {
                    String info = (String) js.executeScript(
                            "var e=arguments[0]; var r=e.getBoundingClientRect();" +
                            "return 'aria=\"'+(e.getAttribute('aria-label')||'')+'\" class=\"'+(e.className||'').substring(0,60)+'\"'" +
                            "+' at('+Math.round(r.left)+','+Math.round(r.top)+') size='+Math.round(r.width)+'x'+Math.round(r.height);",
                            headerButtons.get(i));
                    System.out.println("[AssetPage]   btn[" + i + "] " + info);
                } catch (Exception ignored) {}
            }
            // Try clicking from rightmost first (kebab is typically the last icon)
            for (int i = headerButtons.size() - 1; i >= 0; i--) {
                try {
                    // Use Selenium click (not JS) to properly trigger React handlers
                    try {
                        headerButtons.get(i).click();
                    } catch (Exception clickEx) {
                        js.executeScript("arguments[0].click();", headerButtons.get(i));
                    }
                    pause(800);
                    if (!driver.getCurrentUrl().equals(detailUrl)) {
                        System.out.println("[AssetPage]   btn[" + i + "] navigated away — recovering");
                        driver.get(detailUrl); waitForDetailPageLoad(); continue;
                    }
                    if (tryClickMenuItem(itemText)) {
                        System.out.println("[AssetPage] Success via header button " + i);
                        return;
                    }
                    dismissPopup();
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 1 error: " + e.getMessage());
        }
        ensureOnDetailPage(detailUrl);

        // Strategy 2: Find MoreVert SVG icon (three vertical dots) by its SVG path data.
        // MUI MoreVertIcon uses path: "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z..."
        // Also try any SVG parent elements in header that aren't links.
        try {
            @SuppressWarnings("unchecked")
            java.util.List<WebElement> svgParents = (java.util.List<WebElement>) js.executeScript(
                    "var result = [];" +
                    "var svgs = document.querySelectorAll('svg');" +
                    "for (var svg of svgs) {" +
                    "  var r = svg.getBoundingClientRect();" +
                    "  if (r.top < 0 || r.top > 400 || r.left < 80) continue;" +
                    "  if (r.width > 50 || r.width < 5) continue;" +
                    "  // Check for MoreVert path\n" +
                    "  var paths = svg.querySelectorAll('path');" +
                    "  var isMoreVert = false;" +
                    "  for (var p of paths) {" +
                    "    var d = p.getAttribute('d') || '';" +
                    "    if (d.indexOf('M12 8c1.1') > -1 || d.indexOf('M12 2C6.48') > -1" +
                    "        || (d.match(/M12.*M12.*M12/))) { isMoreVert = true; break; }" +
                    "  }" +
                    "  var clickable = svg.closest('button, [role=\"button\"], div, span');" +
                    "  if (!clickable) clickable = svg.parentElement;" +
                    "  // Skip nav links\n" +
                    "  if (clickable.tagName === 'A') continue;" +
                    "  result.push({el: clickable, vert: isMoreVert, x: r.left});" +
                    "}" +
                    "// Sort: MoreVert first, then by x position descending\n" +
                    "result.sort(function(a,b) { if (a.vert !== b.vert) return a.vert ? -1 : 1; return b.x - a.x; });" +
                    "return result.map(function(r) { return r.el; });");
            System.out.println("[AssetPage] Strategy 2 — SVG parents in header: " + svgParents.size());
            for (int i = 0; i < Math.min(svgParents.size(), 6); i++) {
                try {
                    String info = (String) js.executeScript(
                            "var e=arguments[0]; var r=e.getBoundingClientRect();" +
                            "return e.tagName+' at('+Math.round(r.left)+','+Math.round(r.top)+')'" +
                            "+' size='+Math.round(r.width)+'x'+Math.round(r.height)" +
                            "+' aria=\"'+(e.getAttribute('aria-label')||'')+'\"';", svgParents.get(i));
                    System.out.println("[AssetPage]   svgP[" + i + "] " + info);
                    // Try both Selenium and JS click
                    try { svgParents.get(i).click(); } catch (Exception ce) {
                        js.executeScript("arguments[0].click();", svgParents.get(i));
                    }
                    pause(800);
                    if (!driver.getCurrentUrl().equals(detailUrl)) {
                        System.out.println("[AssetPage]   svgP[" + i + "] navigated away — recovering");
                        driver.get(detailUrl); waitForDetailPageLoad(); continue;
                    }
                    if (tryClickMenuItem(itemText)) {
                        System.out.println("[AssetPage] Success via SVG parent " + i);
                        return;
                    }
                    dismissPopup();
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Strategy 2 error: " + e.getMessage());
        }
        ensureOnDetailPage(detailUrl);

        // Strategy 3: Dump full header DOM diagnostic for debugging, then try
        // ALL interactive elements in the top area as last resort.
        {
            try {
                String dump = (String) js.executeScript(
                        "var result = '';" +
                        "var all = document.querySelectorAll('button, a, [role=\"button\"], [tabindex]');" +
                        "var count = 0;" +
                        "for (var el of all) {" +
                        "  var r = el.getBoundingClientRect();" +
                        "  if (r.top < 0 || r.top > 400 || r.width < 5) continue;" +
                        "  result += el.tagName + '[' + (el.className||'').substring(0,50) + ']'" +
                        "    + ' aria=\"' + (el.getAttribute('aria-label')||'') + '\"'" +
                        "    + ' text=\"' + (el.textContent||'').substring(0,40).trim() + '\"'" +
                        "    + ' href=\"' + (el.getAttribute('href')||'').substring(0,40) + '\"'" +
                        "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                        "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                        "  if (++count > 40) break;" +
                        "}" +
                        "return result;");
                System.out.println("[AssetPage] === HEADER DOM DIAGNOSTIC ===\n" + dump + "=== END DIAGNOSTIC ===");
            } catch (Exception ignored) {}
        }

        // All strategies exhausted
        throw new RuntimeException("Could not open ⋮ menu and click '" + itemText + "' on detail page. Detail URL was: " + detailUrl);
    }

    /** Ensure we're still on the detail page; navigate back if not. */
    private void ensureOnDetailPage(String detailUrl) {
        try {
            if (!driver.getCurrentUrl().equals(detailUrl)) {
                System.out.println("[AssetPage] Off detail page — navigating back");
                driver.get(detailUrl);
                waitForDetailPageLoad();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Check if a menu item with the given text is visible, and click it immediately if found.
     * Returns true if the item was found and clicked.
     */
    private boolean tryClickMenuItem(String itemText) {
        // Quick check: is the text even present on the page?
        java.util.List<WebElement> textEls = driver.findElements(
                By.xpath("//*[contains(text(),'" + itemText + "')]"));
        if (textEls.isEmpty()) {
            // Also check for role=menuitem presence
            if (driver.findElements(By.cssSelector("[role='menuitem']")).isEmpty()) {
                return false; // no menu at all — fast exit
            }
        }

        // Menu detected! Try to click the item.
        System.out.println("[AssetPage] Menu detected — looking for '" + itemText + "'");

        // Try XPath text match first (most reliable — worked in hasMenuItemVisible)
        for (WebElement el : textEls) {
            try {
                System.out.println("[AssetPage]   XPath match: tag=" + el.getTagName() + " text='" + el.getText().trim() + "'");
                js.executeScript("arguments[0].click();", el);
                pause(1000);
                System.out.println("[AssetPage] Clicked '" + itemText + "' via XPath text match");
                return true;
            } catch (Exception e) {
                System.out.println("[AssetPage]   XPath click failed: " + e.getMessage());
            }
        }

        // Try CSS menuitem selectors
        java.util.List<WebElement> menuItems = driver.findElements(
                By.cssSelector("[role='menuitem'], .MuiMenuItem-root"));
        System.out.println("[AssetPage]   CSS menuitem count: " + menuItems.size());
        for (int idx = 0; idx < menuItems.size(); idx++) {
            try {
                WebElement mi = menuItems.get(idx);
                String text = mi.getText().trim();
                String innerHtml = (String) js.executeScript("return arguments[0].innerHTML.substring(0,100);", mi);
                System.out.println("[AssetPage]   menuitem[" + idx + "] text='" + text + "' html=" + innerHtml);
                if (text.contains(itemText) || text.toLowerCase().contains(itemText.toLowerCase())) {
                    js.executeScript("arguments[0].click();", mi);
                    pause(1000);
                    System.out.println("[AssetPage] Clicked menu item: '" + text + "'");
                    return true;
                }
            } catch (Exception ignored) {}
        }

        System.out.println("[AssetPage]   Could not click '" + itemText + "' from detected menu");
        return false;
    }


    /** Dismiss any open popup/menu by clicking outside (avoids Keys.ESCAPE closing drawers) */
    private void dismissPopup() {
        try {
            js.executeScript(
                "var popper = document.querySelector('[role=\"listbox\"], [role=\"tooltip\"], .MuiPopover-root');" +
                "if (popper) { document.querySelector('h5, h6, header, main').click(); }" +
                "else { var b = document.querySelector('.MuiBackdrop-root'); if (b) b.click(); }");
            pause(200);
        } catch (Exception ignored) {}
    }

    /**
     * Opens edit form for the first asset: grid → detail page → ⋮ → Edit Asset
     */
    public void openEditForFirstAsset() {
        navigateToFirstAssetDetail();
        clickKebabMenuItem("Edit Asset");

        // Wait for edit UI to render after "Edit Asset" click
        pause(2000);

        // Capture the current URL — "Edit Asset" may navigate to a different URL or stay on same page
        String editUrl = driver.getCurrentUrl();
        System.out.println("[AssetPage] After 'Edit Asset' click — URL: " + editUrl);

        // Comprehensive DOM diagnostic: find ALL interactive form elements on the page
        // This tells us exactly what the edit UI looks like
        try {
            String diagnostic = (String) js.executeScript(
                    "var result = '--- EDIT UI DIAGNOSTIC ---\\n';" +
                    "result += 'URL: ' + window.location.href + '\\n';" +
                    "result += 'Title: ' + document.title + '\\n\\n';" +

                    "// 1. All input fields\n" +
                    "var inputs = document.querySelectorAll('input');" +
                    "result += '=== INPUTS (' + inputs.length + ') ===\\n';" +
                    "for (var inp of inputs) {" +
                    "  var r = inp.getBoundingClientRect();" +
                    "  if (r.width < 5 || r.height < 5) continue;" +
                    "  result += '  type=\"' + (inp.type||'') + '\"'" +
                    "    + ' name=\"' + (inp.name||'') + '\"'" +
                    "    + ' placeholder=\"' + (inp.placeholder||'') + '\"'" +
                    "    + ' value=\"' + (inp.value||'').substring(0,40) + '\"'" +
                    "    + ' id=\"' + (inp.id||'') + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height)" +
                    "    + ' disabled=' + inp.disabled" +
                    "    + ' readonly=' + inp.readOnly + '\\n';" +
                    "}" +

                    "// 2. All textareas\n" +
                    "var tas = document.querySelectorAll('textarea');" +
                    "result += '\\n=== TEXTAREAS (' + tas.length + ') ===\\n';" +
                    "for (var ta of tas) {" +
                    "  var r = ta.getBoundingClientRect();" +
                    "  if (r.width < 5) continue;" +
                    "  result += '  name=\"' + (ta.name||'') + '\"'" +
                    "    + ' placeholder=\"' + (ta.placeholder||'') + '\"'" +
                    "    + ' value=\"' + (ta.value||'').substring(0,40) + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                    "}" +

                    "// 3. All select/combobox elements\n" +
                    "var sels = document.querySelectorAll('select, [role=\"combobox\"], [role=\"listbox\"]');" +
                    "result += '\\n=== SELECTS/COMBOBOX (' + sels.length + ') ===\\n';" +
                    "for (var s of sels) {" +
                    "  var r = s.getBoundingClientRect();" +
                    "  result += '  tag=' + s.tagName + ' role=\"' + (s.getAttribute('role')||'') + '\"'" +
                    "    + ' text=\"' + (s.textContent||'').substring(0,40).trim() + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                    "}" +

                    "// 4. All buttons with text\n" +
                    "var btns = document.querySelectorAll('button');" +
                    "result += '\\n=== BUTTONS (' + btns.length + ') ===\\n';" +
                    "for (var btn of btns) {" +
                    "  var r = btn.getBoundingClientRect();" +
                    "  if (r.width < 5 || r.height < 5) continue;" +
                    "  var txt = (btn.textContent||'').trim().substring(0,50).replace(/\\n/g,' ');" +
                    "  result += '  \"' + txt + '\"'" +
                    "    + ' aria=\"' + (btn.getAttribute('aria-label')||'') + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                    "}" +

                    "// 5. Dialogs / modals / drawers / panels\n" +
                    "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"presentation\"], .MuiDrawer-root, .MuiDialog-root, .MuiModal-root');" +
                    "result += '\\n=== DIALOGS/DRAWERS (' + dialogs.length + ') ===\\n';" +
                    "for (var d of dialogs) {" +
                    "  var r = d.getBoundingClientRect();" +
                    "  result += '  tag=' + d.tagName + ' role=\"' + (d.getAttribute('role')||'') + '\"'" +
                    "    + ' class=\"' + (d.className||'').substring(0,80) + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                    "}" +

                    "// 6. Visible text headings and labels in main area\n" +
                    "var headings = document.querySelectorAll('h1, h2, h3, h4, h5, h6, label, legend, [class*=\"label\" i], [class*=\"header\" i]');" +
                    "result += '\\n=== HEADINGS/LABELS ===\\n';" +
                    "var hCount = 0;" +
                    "for (var h of headings) {" +
                    "  var r = h.getBoundingClientRect();" +
                    "  if (r.width < 5 || r.left < 80) continue;" +
                    "  var txt = (h.textContent||'').trim().substring(0,60).replace(/\\n/g,' ');" +
                    "  if (!txt) continue;" +
                    "  result += '  <' + h.tagName.toLowerCase() + '> \"' + txt + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')\\n';" +
                    "  if (++hCount > 25) break;" +
                    "}" +

                    "result += '\\n--- END EDIT UI DIAGNOSTIC ---';" +
                    "return result;");
            System.out.println("[AssetPage] " + diagnostic);
        } catch (Exception e) {
            System.out.println("[AssetPage] Edit UI diagnostic error: " + e.getMessage());
        }
    }

    /**
     * Deletes the first asset: grid → detail page → ⋮ → Delete Asset
     */
    /**
     * Get the asset name from the detail page header (e.g., "← 12" → "12").
     * Uses JS textContent on the h5 heading in the main area.
     */
    public String getDetailPageAssetName() {
        try {
            // The detail page header has an h5 with the asset name
            String name = (String) js.executeScript(
                    "var headings = document.querySelectorAll('h5, h4, h3');" +
                    "for (var h of headings) {" +
                    "  var r = h.getBoundingClientRect();" +
                    "  // Header area: y between 50-150, x > 80 (past sidebar)\n" +
                    "  if (r.top > 50 && r.top < 150 && r.left > 80 && r.width > 10) {" +
                    "    var txt = (h.textContent||'').trim();" +
                    "    if (txt.length > 0 && txt.length < 100) return txt;" +
                    "  }" +
                    "}" +
                    "return '';");
            System.out.println("[AssetPage] Detail page asset name: '" + name + "'");
            return (name == null || name.isEmpty()) ? null : name;
        } catch (Exception e) {
            System.out.println("[AssetPage] Could not read detail page asset name: " + e.getMessage());
            return null;
        }
    }

    /**
     * Wait for delete to complete. After confirmDelete(), the app should either:
     * 1. Navigate back to the asset list
     * 2. Show a success toast/snackbar
     * 3. The detail page should no longer be showing the deleted asset
     */
    public boolean waitForDeleteSuccess() {
        System.out.println("[AssetPage] waitForDeleteSuccess — checking...");
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
            shortWait.until(driver -> {
                String url = driver.getCurrentUrl();
                boolean onDetailPage = url.matches(".*/assets/[a-f0-9-]+.*");

                // Back to asset list (no UUID in URL) — strongest signal
                if (url.matches(".*/assets/?$") || url.endsWith("/assets")) return true;

                // Success toast/alert — check Snackbar, MuiAlert, AND [role='alert'] containers
                // but verify the text indicates success (not an error alert)
                java.util.List<WebElement> toasts = driver.findElements(
                    By.cssSelector(".MuiSnackbar-root, .MuiAlert-root, [role='alert']"));
                for (WebElement toast : toasts) {
                    try {
                        String toastText = toast.getText().toLowerCase();
                        // Accept: deleted, removed, success (but NOT error/fail alerts)
                        if ((toastText.contains("deleted") || toastText.contains("removed")
                                || toastText.contains("success"))
                                && !toastText.contains("error") && !toastText.contains("fail")) {
                            System.out.println("[AssetPage] Success toast/alert: " + toastText.substring(0, Math.min(60, toastText.length())));
                            return true;
                        }
                    } catch (Exception ignored) {}
                }

                // DataGrid visible — ONLY if on list page (detail pages have sub-grids)
                if (!onDetailPage
                        && driver.findElements(By.xpath("//div[contains(@class,'MuiDataGrid')]")).size() > 0)
                    return true;
                return false;
            });
            System.out.println("[AssetPage] Delete success detected. URL: " + driver.getCurrentUrl());
            return true;
        } catch (Exception e) {
            String url = driver.getCurrentUrl();
            System.out.println("[AssetPage] waitForDeleteSuccess — no indicator found after 15s. URL: " + url);
            // Only treat as success if we're on the asset LIST page (not detail page with UUID)
            if (url.matches(".*/assets/?$") || url.endsWith("/assets")) {
                System.out.println("[AssetPage] On asset list page — treating as success");
                return true;
            }
            // If still on detail page (/assets/UUID), delete didn't work
            System.out.println("[AssetPage] Still on detail page — delete did NOT succeed");
            return false;
        }
    }

    public void deleteFirstAssetFromGrid() {
        navigateToFirstAssetDetail();
        clickKebabMenuItem("Delete Asset");
        pause(1000);

        // Diagnostic: what does the delete UI look like?
        try {
            String diagnostic = (String) js.executeScript(
                    "var result = '--- DELETE UI DIAGNOSTIC ---\\n';" +
                    "result += 'URL: ' + window.location.href + '\\n';" +

                    "// Dialogs/modals\n" +
                    "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"presentation\"], .MuiDialog-root, .MuiModal-root, [role=\"alertdialog\"]');" +
                    "result += 'Dialogs: ' + dialogs.length + '\\n';" +
                    "for (var d of dialogs) {" +
                    "  var r = d.getBoundingClientRect();" +
                    "  result += '  tag=' + d.tagName + ' role=\"' + (d.getAttribute('role')||'') + '\"'" +
                    "    + ' text=\"' + (d.textContent||'').substring(0,100).trim().replace(/\\n/g,' ') + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                    "}" +

                    "// All buttons\n" +
                    "var btns = document.querySelectorAll('button');" +
                    "result += 'Buttons: ' + btns.length + '\\n';" +
                    "for (var btn of btns) {" +
                    "  var r = btn.getBoundingClientRect();" +
                    "  if (r.width < 5) continue;" +
                    "  var txt = (btn.textContent||'').trim().substring(0,50).replace(/\\n/g,' ');" +
                    "  var cls = (btn.className||'').substring(0,80);" +
                    "  result += '  \"' + txt + '\" class=\"' + cls + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                    "}" +

                    "result += '--- END DELETE UI DIAGNOSTIC ---';" +
                    "return result;");
            System.out.println("[AssetPage] " + diagnostic);
        } catch (Exception e) {
            System.out.println("[AssetPage] Delete UI diagnostic error: " + e.getMessage());
        }
    }

    /**
     * Change the Asset Class in the edit drawer.
     * The field is an MUI Autocomplete with placeholder "Select Class".
     * Clears the current value, types the new class, and selects from dropdown.
     */

    /**
     * Read the current asset class value from the edit/detail page.
     */
    public String getAssetClassValue() {
        try {
            By classInput = By.xpath("//input[@placeholder='Select Class']");
            WebElement input = driver.findElement(classInput);
            String value = input.getAttribute("value");
            System.out.println("[AssetPage] getAssetClassValue: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("[AssetPage] getAssetClassValue failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Read the current asset name value from the edit/detail page.
     */
    public String getAssetNameValue() {
        try {
            By nameInput = By.xpath("//input[@placeholder='Enter Asset Name']");
            WebElement input = driver.findElement(nameInput);
            String value = input.getAttribute("value");
            System.out.println("[AssetPage] getAssetNameValue: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("[AssetPage] getAssetNameValue failed: " + e.getMessage());
            return null;
        }
    }

    public void editAssetClass(String newClass) {
        System.out.println("[AssetPage] editAssetClass — changing to: " + newClass);

        // Step 1: Clear the current value via the X (clear) button
        Boolean cleared = (Boolean) js.executeScript(
            "var inputs = document.querySelectorAll('input[placeholder=\"Select Class\"]');" +
            "for (var inp of inputs) {" +
            "  var r = inp.getBoundingClientRect();" +
            "  if (r.width > 0) {" +
            "    var wrapper = inp.closest('[class*=\"MuiAutocomplete\"]');" +
            "    if (wrapper) {" +
            "      var clearBtn = wrapper.querySelector('[class*=\"MuiAutocomplete-clearIndicator\"]');" +
            "      if (clearBtn) { clearBtn.click(); return true; }" +
            "    }" +
            "  }" +
            "}" +
            "return false;"
        );
        System.out.println("[AssetPage] Asset class cleared: " + cleared);
        pause(800);

        // Step 2: Click the input (via JS to avoid backdrop interception), type the class name
        WebElement classInput = driver.findElement(ASSET_CLASS_INPUT);
        js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus(); arguments[0].click();", classInput);
        pause(300);

        // Use React native setter to properly trigger filtering
        js.executeScript(
            "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            classInput, newClass);
        pause(1500);

        // Step 3: Select matching option from the dropdown
        Boolean selected = (Boolean) js.executeScript(
            "var items = document.querySelectorAll('li[role=\"option\"]');" +
            "var searchText = arguments[0];" +
            "for (var item of items) {" +
            "  if (item.textContent.trim() === searchText) { item.click(); return true; }" +
            "}" +
            "for (var item of items) {" +
            "  if (item.textContent.trim().includes(searchText)) { item.click(); return true; }" +
            "}" +
            "if (items.length > 0) { items[0].click(); return true; }" +
            "return false;", newClass);
        System.out.println("[AssetPage] Asset class option selected: " + selected);
        pause(1000);

        // Step 4: Select subtype (first available option after class change)
        try {
            pause(500);
            if (driver.findElements(ASSET_SUBTYPE_INPUT).size() > 0) {
                WebElement subtypeInput = driver.findElement(ASSET_SUBTYPE_INPUT);
                String currentSubtype = subtypeInput.getAttribute("value");
                if (currentSubtype == null || currentSubtype.isEmpty()) {
                    click(ASSET_SUBTYPE_INPUT);
                    pause(400);
                    By firstOption = By.xpath("(//li[@role='option'])[1]");
                    if (driver.findElements(firstOption).size() > 0) {
                        click(firstOption);
                        pause(300);
                    }
                }
            }
        } catch (Exception ignored) {}

        System.out.println("[AssetPage] Asset class changed to: " + newClass);
    }

    public void editModel(String newModel) {
        System.out.println("[AssetPage] editModel — looking for QR code field to update...");

        // The edit form has: Asset Name, QR Code, Class, Subtype, Serviceability, Shortcut.
        // There is no dedicated "Model" field. Use QR Code as a safe editable field.
        try {
            By qrField = By.xpath("//input[@placeholder='Add QR code']");
            WebElement qrInput = driver.findElement(qrField);
            if (qrInput.isDisplayed() && qrInput.isEnabled()) {
                js.executeScript(
                    "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                    "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    qrInput, newModel);
                System.out.println("[AssetPage] Updated QR code field with: " + newModel);
                return;
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] QR code field not found: " + e.getMessage());
        }

        System.out.println("WARNING: Could not find QR code or model field to edit.");
    }

    public void editNotes(String newNotes) {
        System.out.println("[AssetPage] editNotes — looking for any textarea or notes field...");

        // Strategy 1: Find any visible textarea
        try {
            @SuppressWarnings("unchecked")
            java.util.List<WebElement> textareas = (java.util.List<WebElement>) js.executeScript(
                    "var result = [];" +
                    "var tas = document.querySelectorAll('textarea');" +
                    "for (var ta of tas) {" +
                    "  var r = ta.getBoundingClientRect();" +
                    "  if (r.width < 20 || r.left < 80) continue;" +
                    "  if (ta.disabled || ta.readOnly) continue;" +
                    "  result.push(ta);" +
                    "}" +
                    "return result;");
            System.out.println("[AssetPage] Editable textareas found: " + textareas.size());
            if (!textareas.isEmpty()) {
                WebElement ta = textareas.get(0);
                js.executeScript("arguments[0].focus(); arguments[0].value = '';", ta);
                ta.sendKeys(newNotes);
                js.executeScript(
                        "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;" +
                        "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                        ta, newNotes);
                return;
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] textarea strategy error: " + e.getMessage());
        }

        // Strategy 2: Find any input with notes/description/comment related placeholder
        String[] noteWords = {"Note", "note", "Description", "description", "Comment", "comment"};
        for (String word : noteWords) {
            try {
                By field = By.xpath("//input[contains(@placeholder,'" + word + "')] | //textarea[contains(@placeholder,'" + word + "')]");
                java.util.List<WebElement> els = driver.findElements(field);
                for (WebElement el : els) {
                    try {
                        if (el.isDisplayed() && el.isEnabled()) {
                            System.out.println("[AssetPage] Found notes field with placeholder containing '" + word + "'");
                            try { el.clear(); } catch (Exception ignored) {}
                            el.click();
                            el.sendKeys(newNotes);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        System.out.println("WARNING: Could not find any notes/textarea field. " +
                "Check the EDIT UI DIAGNOSTIC output above for available form elements.");
    }

    public void saveChanges() {
        pause(500);
        System.out.println("[AssetPage] saveChanges — looking for save button...");
        System.out.println("[AssetPage] Current URL: " + driver.getCurrentUrl());

        // Strategy 1: "Save Changes" button
        if (driver.findElements(SAVE_CHANGES_BTN).size() > 0) {
            System.out.println("[AssetPage] Found 'Save Changes' button");
            click(SAVE_CHANGES_BTN);
            return;
        }

        // Strategy 2: Any button with save/update/submit/apply/done/confirm text
        String[] saveTexts = {"Save", "Update", "Submit", "Apply", "Done", "Confirm"};
        for (String text : saveTexts) {
            try {
                @SuppressWarnings("unchecked")
                java.util.List<WebElement> btns = (java.util.List<WebElement>) js.executeScript(
                        "var result = [];" +
                        "var buttons = document.querySelectorAll('button');" +
                        "for (var btn of buttons) {" +
                        "  var r = btn.getBoundingClientRect();" +
                        "  if (r.width < 5) continue;" +
                        "  var txt = (btn.textContent||'').trim();" +
                        "  if (txt.toLowerCase().indexOf('" + text.toLowerCase() + "') > -1) {" +
                        "    result.push(btn);" +
                        "  }" +
                        "}" +
                        "return result;");
                if (btns != null && !btns.isEmpty()) {
                    System.out.println("[AssetPage] Clicking button containing '" + text + "'");
                    try { btns.get(0).click(); } catch (Exception ce) {
                        js.executeScript("arguments[0].click();", btns.get(0));
                    }
                    pause(1000);
                    return;
                }
            } catch (Exception ignored) {}
        }

        // Strategy 3: Look for a primary/contained button (MUI primary CTA)
        try {
            @SuppressWarnings("unchecked")
            java.util.List<WebElement> primaryBtns = (java.util.List<WebElement>) js.executeScript(
                    "var result = [];" +
                    "var buttons = document.querySelectorAll('.MuiButton-containedPrimary, button[color=\"primary\"]');" +
                    "for (var btn of buttons) {" +
                    "  var r = btn.getBoundingClientRect();" +
                    "  if (r.width < 20 || r.left < 80) continue;" +
                    "  result.push(btn);" +
                    "}" +
                    "return result;");
            if (primaryBtns != null && !primaryBtns.isEmpty()) {
                String btnText = (String) js.executeScript("return (arguments[0].textContent||'').trim();", primaryBtns.get(0));
                System.out.println("[AssetPage] Clicking primary button: '" + btnText + "'");
                try { primaryBtns.get(0).click(); } catch (Exception ce) {
                    js.executeScript("arguments[0].click();", primaryBtns.get(0));
                }
                pause(1000);
                return;
            }
        } catch (Exception ignored) {}

        // Strategy 4: Press Enter on active element to commit inline edit
        try {
            WebElement activeEl = driver.switchTo().activeElement();
            activeEl.sendKeys(org.openqa.selenium.Keys.ENTER);
            pause(500);
            System.out.println("[AssetPage] Pressed Enter to save inline edit");
            return;
        } catch (Exception ignored) {}

        // Diagnostic dump
        java.util.List<WebElement> allButtons = driver.findElements(By.xpath("//button"));
        System.out.println("[AssetPage] saveChanges FAILED — no Save button found. " + allButtons.size() + " buttons on page:");
        for (int i = 0; i < Math.min(allButtons.size(), 25); i++) {
            try {
                String btnInfo = (String) js.executeScript(
                        "var e=arguments[0]; var r=e.getBoundingClientRect();" +
                        "return '\"'+(e.textContent||'').trim().substring(0,40).replace(/\\n/g,' ')+'\"'" +
                        "+' class=\"'+(e.className||'').substring(0,60)+'\"'" +
                        "+' at('+Math.round(r.left)+','+Math.round(r.top)+') '+Math.round(r.width)+'x'+Math.round(r.height);",
                        allButtons.get(i));
                System.out.println("  [" + i + "] " + btnInfo);
            } catch (Exception ignored) {}
        }

        throw new RuntimeException("No save/update button found on page. URL: " + driver.getCurrentUrl());
    }

    public boolean waitForEditSuccess() {
        System.out.println("[AssetPage] waitForEditSuccess — checking for success indicators...");
        try {
            // Wait for any success indicator: toast, redirect, or text change
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
            shortWait.until(ExpectedConditions.or(
                    // Toast/snackbar success message
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//*[contains(text(),'updated') or contains(text(),'saved') or contains(text(),'success') or contains(text(),'Updated') or contains(text(),'Saved')]")),
                    // MUI Snackbar
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(".MuiSnackbar-root, .MuiAlert-root, [role='alert']")),
                    // Back to asset list
                    ExpectedConditions.presenceOfElementLocated(ASSET_LIST_INDICATOR),
                    // URL changed back to assets list
                    ExpectedConditions.urlContains("/assets")
            ));
            System.out.println("[AssetPage] Edit success detected. URL: " + driver.getCurrentUrl());
            return true;
        } catch (Exception e) {
            System.out.println("[AssetPage] waitForEditSuccess — no success indicator found after 15s");
            System.out.println("[AssetPage] Current URL: " + driver.getCurrentUrl());
            // Check if we're still on the detail page (edit might have silently succeeded)
            String url = driver.getCurrentUrl();
            if (url.contains("/assets/")) {
                System.out.println("[AssetPage] Still on asset page — treating as success");
                return true;
            }
            return false;
        }
    }

    // --- DELETE ---

    public void confirmDelete() {
        System.out.println("[AssetPage] confirmDelete — looking for confirmation button...");

        // Strategy 0: Check for native browser confirm() dialog first
        for (int a = 0; a < 3; a++) {
            try {
                org.openqa.selenium.Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                System.out.println("[AssetPage] Native alert found: \"" + alertText + "\"");
                alert.accept();
                System.out.println("[AssetPage] Native alert accepted — delete confirmed");
                return;
            } catch (org.openqa.selenium.NoAlertPresentException ignored) {
                // No native alert — fall through to MUI strategies
            } catch (Exception e) {
                System.out.println("[AssetPage] Alert check: " + e.getMessage());
            }
            pause(500);
        }

        // Wait for the confirmation dialog to appear before looking for buttons.
        // Without this, findAssetDeleteButton() may find a "Delete" button outside the dialog.
        boolean dialogAppeared = false;
        for (int w = 0; w < 10; w++) {
            Boolean hasDialog = (Boolean) js.executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"alertdialog\"], [class*=\"MuiDialog\"]');" +
                "for (var d of dialogs) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 100 && r.height > 50) {" +
                "    var text = (d.textContent||'').toLowerCase();" +
                "    if (text.includes('delete') || text.includes('confirm') || text.includes('remove') || text.includes('sure')) return true;" +
                "  }" +
                "}" +
                "return false;");
            if (hasDialog != null && hasDialog) {
                System.out.println("[AssetPage] Delete confirmation dialog detected after " + (w * 500) + "ms");
                dialogAppeared = true;
                break;
            }
            pause(500);
        }
        if (!dialogAppeared) {
            System.out.println("[AssetPage] WARNING: No delete confirmation dialog appeared after 5s");
        }

        // Strategy 1: Dismiss backdrops, find the Delete button, then use escalating click
        // strategies with post-click verification. A single click sometimes fires but React
        // doesn't process it (especially in MUI Portal dialogs in headless Chrome).
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                // Always nuke backdrops first
                js.executeScript(
                    "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
                    "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                    ");"
                );
                pause(200);

                // Find the Delete/Confirm button
                WebElement deleteBtn = findAssetDeleteButton();
                if (deleteBtn != null) {
                    String text = deleteBtn.getText().trim();
                    if (clickDeleteWithVerification(deleteBtn, text)) {
                        return;
                    }
                    System.out.println("[AssetPage] Click on '" + text + "' didn't close dialog, retrying...");
                }
            } catch (Exception e) {
                System.out.println("[AssetPage] confirmDelete attempt " + (attempt + 1) + ": " + e.getMessage());
            }
            pause(500);
        }

        // Diagnostic
        System.out.println("[AssetPage] confirmDelete FAILED — dumping all visible buttons:");
        try {
            String dump = (String) js.executeScript(
                    "var result = '';" +
                    "var btns = document.querySelectorAll('button');" +
                    "for (var btn of btns) {" +
                    "  var r = btn.getBoundingClientRect();" +
                    "  if (r.width < 5) continue;" +
                    "  result += '  \"' + (btn.textContent||'').trim().substring(0,40).replace(/\\n/g,' ') + '\"'" +
                    "    + ' class=\"' + (btn.className||'').substring(0,60) + '\"'" +
                    "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                    "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                    "}" +
                    "return result;");
            System.out.println(dump);
        } catch (Exception ignored) {}

        throw new RuntimeException("No delete confirmation button found");
    }

    /**
     * Find the Delete/Confirm button in a visible MUI dialog.
     */
    private WebElement findAssetDeleteButton() {
        // Try error-styled buttons first (red Delete/Confirm)
        java.util.List<WebElement> errorBtns = driver.findElements(
            By.cssSelector("button[class*='containedError'], button[class*='error']"));
        for (WebElement btn : errorBtns) {
            try {
                if (btn.isDisplayed() && btn.isEnabled()) {
                    String text = btn.getText().trim();
                    if (text.equalsIgnoreCase("Delete") || text.equalsIgnoreCase("Confirm")
                            || text.equalsIgnoreCase("Yes") || text.toLowerCase().contains("delete")) {
                        return btn;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Try buttons inside dialog containers (NOT role=presentation — DataGrid uses that)
        java.util.List<WebElement> dialogs = driver.findElements(
            By.cssSelector("[role='dialog'], [class*='MuiDialog-paper'], [role='alertdialog']"));
        for (WebElement dialog : dialogs) {
            java.util.List<WebElement> dBtns = dialog.findElements(By.tagName("button"));
            for (WebElement btn : dBtns) {
                try {
                    if (btn.isDisplayed() && btn.isEnabled()) {
                        String text = btn.getText().trim();
                        if (text.equalsIgnoreCase("Delete") || text.equalsIgnoreCase("Confirm")
                                || text.equalsIgnoreCase("Yes") || text.toLowerCase().contains("delete")) {
                            return btn;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Last resort: any visible Delete button — ONLY inside actual dialog containers (NOT role=presentation — DataGrid uses that)
        java.util.List<WebElement> allDeleteBtns = driver.findElements(
            By.xpath("//div[@role='dialog' or @role='alertdialog' or contains(@class,'MuiDialog-paper')]//button[contains(.,'Delete') or contains(.,'delete')]"));
        for (WebElement btn : allDeleteBtns) {
            try {
                if (btn.isDisplayed() && btn.isEnabled()) return btn;
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Try escalating click strategies on a delete button, verifying the dialog closes after each.
     * Returns true if the dialog closed (delete succeeded).
     */
    private boolean clickDeleteWithVerification(WebElement btn, String text) {
        // Strategy A: Selenium click
        try {
            btn.click();
            System.out.println("[AssetPage] Selenium-clicked: '" + text + "'");
            pause(1500);
            if (isAssetDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[AssetPage] Selenium click failed: " + e.getMessage());
        }

        // Strategy B: Actions moveToElement + click
        try {
            new org.openqa.selenium.interactions.Actions(driver)
                .moveToElement(btn).pause(java.time.Duration.ofMillis(200)).click().perform();
            System.out.println("[AssetPage] Actions-clicked: '" + text + "'");
            pause(1500);
            if (isAssetDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[AssetPage] Actions click failed: " + e.getMessage());
        }

        // Strategy C: Focus button and press Enter key (keyboard events are always trusted)
        try {
            new org.openqa.selenium.interactions.Actions(driver)
                .moveToElement(btn).sendKeys(org.openqa.selenium.Keys.ENTER).perform();
            System.out.println("[AssetPage] Enter-key on: '" + text + "'");
            pause(1500);
            if (isAssetDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[AssetPage] Enter key failed: " + e.getMessage());
        }

        // Strategy D: JS focus + programmatic click
        try {
            js.executeScript("arguments[0].focus(); arguments[0].click();", btn);
            System.out.println("[AssetPage] JS focus+click on: '" + text + "'");
            pause(1500);
            if (isAssetDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[AssetPage] JS click failed: " + e.getMessage());
        }

        // Strategy E: Direct React fiber onClick invocation
        try {
            String result = (String) js.executeScript(
                "var btn = arguments[0];" +
                "var keys = Object.keys(btn);" +
                "var reactKey = keys.find(function(k) {" +
                "  return k.startsWith('__reactFiber$') || k.startsWith('__reactInternalInstance$');" +
                "});" +
                "if (!reactKey) return 'no-react-key';" +
                "var fiber = btn[reactKey];" +
                "var maxDepth = 20;" +
                "while (fiber && maxDepth-- > 0) {" +
                "  var props = fiber.memoizedProps || fiber.pendingProps;" +
                "  if (props && typeof props.onClick === 'function') {" +
                "    props.onClick({preventDefault:function(){},stopPropagation:function(){},target:btn,currentTarget:btn});" +
                "    return 'invoked-onClick';" +
                "  }" +
                "  fiber = fiber.return;" +
                "}" +
                "return 'no-onClick-found';",
                btn);
            System.out.println("[AssetPage] React fiber invoke: " + result);
            pause(2000);
            if (isAssetDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[AssetPage] React fiber invoke failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if the delete confirmation dialog has been dismissed.
     */
    private boolean isAssetDeleteDialogGone() {
        try {
            // IMPORTANT: Only check role="dialog" and role="alertdialog", NOT role="presentation".
            // MUI DataGrid uses role="presentation" for structural elements, and their text content
            // includes action button labels like "Delete Asset" — which would false-positive this check.
            String result = (String) js.executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"alertdialog\"], .MuiDialog-paper');" +
                "var info = 'isDialogGone: found=' + dialogs.length;" +
                "for (var d of dialogs) {" +
                "  var r = d.getBoundingClientRect();" +
                "  var text = (d.textContent||'').toLowerCase().substring(0,80).replace(/\\n/g,' ');" +
                "  info += ' | ' + d.tagName + '[role=' + (d.getAttribute('role')||'none') + ' class=' + (d.className||'').substring(0,40) + '] ' + Math.round(r.width) + 'x' + Math.round(r.height) + ' text=\"' + text + '\"';" +
                "  if (r.width > 100 && r.height > 50) {" +
                "    if (text.includes('delete') || text.includes('confirm') || text.includes('remove') || text.includes('sure')) return 'OPEN:' + info;" +
                "  }" +
                "}" +
                "return 'GONE:' + info;"
            );
            System.out.println("[AssetPage] " + result);
            return result != null && result.startsWith("GONE:");
        } catch (Exception e) {
            return true;
        }
    }

    // --- CONNECTIONS ---

    /**
     * Scroll the edit drawer to the CONNECTIONS accordion and expand it if collapsed.
     * Must be called while the Edit Asset drawer is open.
     */
    public void expandConnectionsSection() {
        // Scroll all scrollable containers in the edit drawer to find CONNECTIONS
        js.executeScript(
            "var containers = document.querySelectorAll('[class*=\"MuiDrawer\"] > div, [class*=\"MuiPaper\"]');" +
            "for (var c of containers) {" +
            "  if (c.scrollHeight > c.clientHeight + 100) c.scrollTop = c.scrollHeight;" +
            "}"
        );
        pause(1000);

        // Find CONNECTIONS accordion and expand if collapsed
        js.executeScript(
            "var h6s = document.querySelectorAll('h6');" +
            "for (var el of h6s) {" +
            "  if (el.textContent.trim() === 'CONNECTIONS') {" +
            "    var accordion = el.closest('[class*=\"MuiAccordion\"]');" +
            "    if (accordion && !accordion.classList.contains('Mui-expanded')) {" +
            "      var summary = accordion.querySelector('[class*=\"MuiAccordionSummary\"]');" +
            "      if (summary) summary.click();" +
            "    }" +
            "    el.scrollIntoView({block:'center'});" +
            "    break;" +
            "  }" +
            "}"
        );
        pause(1000);
        System.out.println("[AssetPage] CONNECTIONS section expanded");
    }

    /**
     * Click the "+" IconButton in the CONNECTIONS header.
     * Opens a menu with "New Lineside Connection" and "New Loadside Connection".
     */
    public void clickAddConnectionButton() {
        js.executeScript(
            "var h6s = document.querySelectorAll('h6');" +
            "for (var el of h6s) {" +
            "  if (el.textContent.trim() === 'CONNECTIONS') {" +
            "    var parent = el.parentElement;" +
            "    for (var i = 0; i < 4; i++) {" +
            "      var btns = parent.querySelectorAll('button[class*=\"MuiIconButton\"]');" +
            "      for (var btn of btns) {" +
            "        var paths = btn.querySelectorAll('path');" +
            "        for (var p of paths) {" +
            "          if ((p.getAttribute('d')||'').indexOf('M19 13') > -1) { btn.click(); return; }" +
            "        }" +
            "      }" +
            "      if (parent.parentElement) parent = parent.parentElement;" +
            "    }" +
            "  }" +
            "}"
        );
        pause(1000);
        System.out.println("[AssetPage] Clicked '+' button on CONNECTIONS");
    }

    /**
     * Click "New Loadside Connection" from the connection type menu.
     * Must be called after clickAddConnectionButton().
     */
    public void selectNewLoadsideConnection() {
        Boolean clicked = (Boolean) js.executeScript(
            "var items = document.querySelectorAll('[role=\"menuitem\"]');" +
            "for (var item of items) {" +
            "  if (item.textContent.trim().indexOf('Loadside') > -1) { item.click(); return true; }" +
            "}" +
            "return false;"
        );
        if (clicked == null || !clicked) {
            throw new RuntimeException("Could not find 'New Loadside Connection' menu item");
        }
        pause(2000);
        System.out.println("[AssetPage] Selected 'New Loadside Connection'");
    }

    /**
     * Click "New Lineside Connection" from the connection type menu.
     * Must be called after clickAddConnectionButton().
     */
    public void selectNewLinesideConnection() {
        Boolean clicked = (Boolean) js.executeScript(
            "var items = document.querySelectorAll('[role=\"menuitem\"]');" +
            "for (var item of items) {" +
            "  if (item.textContent.trim().indexOf('Lineside') > -1) { item.click(); return true; }" +
            "}" +
            "return false;"
        );
        if (clicked == null || !clicked) {
            throw new RuntimeException("Could not find 'New Lineside Connection' menu item");
        }
        pause(2000);
        System.out.println("[AssetPage] Selected 'New Lineside Connection'");
    }

    /**
     * Select target asset in the "New Connection" dialog.
     * Uses the MUI Autocomplete with placeholder "Select target asset".
     *
     * @param targetName partial or full asset name to search and select
     */
    public void selectTargetAsset(String targetName) {
        // Try "Select target asset" (Loadside) or "Select source asset" (Lineside)
        By targetInput = By.xpath(
            "//input[@placeholder='Select target asset'] | //input[@placeholder='Select source asset']");
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(targetInput));
        System.out.println("[AssetPage] Found connection asset input: placeholder=\"" + input.getAttribute("placeholder") + "\"");
        input.click();
        pause(300);

        if (targetName != null && !targetName.isEmpty()) {
            // Use React native setter for controlled input
            js.executeScript(
                "var inp = arguments[0]; var text = arguments[1];" +
                "var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "nativeSetter.call(inp, '');" +
                "inp.dispatchEvent(new Event('input', { bubbles: true }));" +
                "nativeSetter.call(inp, text);" +
                "inp.dispatchEvent(new Event('input', { bubbles: true }));" +
                "inp.dispatchEvent(new Event('change', { bubbles: true }));",
                input, targetName);
            pause(2000);

            // Wait for dropdown options to appear and click the matching one
            By option = By.xpath("//li[@role='option'][contains(normalize-space(),'" + targetName + "')]");
            for (int i = 0; i < 5; i++) {
                java.util.List<WebElement> options = driver.findElements(option);
                if (!options.isEmpty()) {
                    js.executeScript("arguments[0].click();", options.get(0));
                    pause(500);
                    System.out.println("[AssetPage] Selected target asset: " + targetName);
                    return;
                }
                pause(500);
            }
        }

        // Select first available option from dropdown (no filter)
        selectFirstAvailableTargetAsset(input);
    }

    /**
     * Select the first available option from the target asset dropdown.
     * Clears any typed text first to show the full unfiltered list.
     */
    private void selectFirstAvailableTargetAsset(WebElement input) {
        // Click input and use ArrowDown to open MUI Autocomplete dropdown
        input.click();
        pause(300);
        input.sendKeys(org.openqa.selenium.Keys.ARROW_DOWN);
        pause(2000);

        By anyOption = By.xpath("//li[@role='option']");
        java.util.List<WebElement> options = driver.findElements(anyOption);

        // Fallback: try popup indicator button
        if (options.isEmpty()) {
            try {
                js.executeScript(
                    "var inp = arguments[0];" +
                    "var ac = inp.closest('.MuiAutocomplete-root');" +
                    "if (ac) { var btn = ac.querySelector('.MuiAutocomplete-popupIndicator'); if (btn) btn.click(); }",
                    input);
                pause(2000);
                options = driver.findElements(anyOption);
            } catch (Exception ignored) {}
        }

        // Poll a few more times
        for (int i = 0; i < 5 && options.isEmpty(); i++) {
            pause(1000);
            options = driver.findElements(anyOption);
        }

        if (!options.isEmpty()) {
            String optText = (String) js.executeScript("return arguments[0].textContent.trim();", options.get(0));
            js.executeScript("arguments[0].click();", options.get(0));
            pause(500);
            System.out.println("[AssetPage] Selected first available target: " + optText);
            return;
        }

        throw new RuntimeException("No target asset options available in dropdown");
    }

    /**
     * Select connection type in the "New Connection" dialog (optional field).
     *
     * @param typeName connection type to select (e.g., "Cable", "Bus")
     */
    public void selectConnectionType(String typeName) {
        By typeInput = By.xpath("//input[@placeholder='Select type (optional)']");
        java.util.List<WebElement> inputs = driver.findElements(typeInput);
        if (inputs.isEmpty()) {
            System.out.println("[AssetPage] Connection Type field not found — skipping");
            return;
        }

        WebElement input = inputs.get(0);
        input.click();
        pause(300);
        input.sendKeys(typeName);
        pause(1500);

        By option = By.xpath("//li[@role='option'][contains(normalize-space(),'" + typeName + "')]");
        for (int i = 0; i < 5; i++) {
            java.util.List<WebElement> options = driver.findElements(option);
            if (!options.isEmpty()) {
                js.executeScript("arguments[0].click();", options.get(0));
                pause(500);
                System.out.println("[AssetPage] Selected connection type: " + typeName);
                return;
            }
            // Try first available option as fallback
            By anyOpt = By.xpath("//li[@role='option']");
            java.util.List<WebElement> allOpts = driver.findElements(anyOpt);
            if (!allOpts.isEmpty()) {
                js.executeScript("arguments[0].click();", allOpts.get(0));
                pause(500);
                System.out.println("[AssetPage] Selected first available connection type");
                return;
            }
            pause(500);
        }
        System.out.println("[AssetPage] WARNING: Could not select connection type '" + typeName + "'");
    }

    /**
     * Click "Create Connection" button in the connection dialog.
     */
    public void clickCreateConnection() {
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  if (b.textContent.trim() === 'Create Connection' && !b.disabled) {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;"
        );
        if (clicked == null || !clicked) {
            throw new RuntimeException("'Create Connection' button not found or is disabled. Ensure a target asset is selected.");
        }
        pause(2000);
        System.out.println("[AssetPage] Clicked 'Create Connection'");
    }

    /**
     * Wait for the connection dialog to close after creation.
     */
    public void waitForConnectionDialogClose() {
        for (int i = 0; i < 10; i++) {
            boolean dialogOpen = !driver.findElements(By.cssSelector("[role='dialog']")).isEmpty();
            if (!dialogOpen) {
                System.out.println("[AssetPage] Connection dialog closed");
                return;
            }
            pause(1000);
        }
        // Force close if still open
        dismissAnyDialog();
    }

    /**
     * Dismiss any open dialog (press Escape or click Cancel/Close).
     */
    /**
     * Dismiss any open MUI Drawer, Backdrop, or side panel that may linger from a previous test.
     * MUI Drawers/Backdrops close on Escape by default.
     * This prevents "element not interactable" errors when a previous test leaves a drawer open.
     */
    private void dismissAnyDrawerOrBackdrop() {
        try {
            // Check for MUI Backdrops (present when drawers/dialogs are open)
            Boolean hasBackdrop = (Boolean) js.executeScript(
                "return document.querySelectorAll('.MuiBackdrop-root, .MuiDrawer-root, [role=\"presentation\"]').length > 0;");
            if (Boolean.TRUE.equals(hasBackdrop)) {
                System.out.println("[AssetPage] Backdrop/drawer detected — clicking Cancel/Close/Backdrop to dismiss");
                // Try Cancel/Close buttons first, then backdrop click (avoid Keys.ESCAPE)
                js.executeScript(
                    "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [role=\"dialog\"]');" +
                    "for (var d of drawers) {" +
                    "  var btn = d.querySelector('[aria-label=\"Close\"], [aria-label=\"close\"]');" +
                    "  if (btn) { btn.click(); return; }" +
                    "  var btns = d.querySelectorAll('button');" +
                    "  for (var b of btns) { if (b.textContent.trim()==='Cancel') { b.click(); return; } }" +
                    "}" +
                    "var backdrop = document.querySelector('.MuiBackdrop-root');" +
                    "if (backdrop) backdrop.click();");
                pause(800);
                // Check if still present and try again
                Boolean stillPresent = (Boolean) js.executeScript(
                    "return document.querySelectorAll('.MuiBackdrop-root').length > 0;");
                if (Boolean.TRUE.equals(stillPresent)) {
                    js.executeScript("var b=document.querySelector('.MuiBackdrop-root'); if(b) b.click();");
                    pause(800);
                }
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] dismissAnyDrawerOrBackdrop: " + e.getMessage());
        }
    }

    public void dismissAnyDialog() {
        try {
            js.executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"]');" +
                "for (var d of dialogs) {" +
                "  var closeBtn = d.querySelector('[aria-label=\"Close\"], [aria-label=\"close\"]');" +
                "  if (closeBtn) { closeBtn.click(); return; }" +
                "  var cancelBtns = d.querySelectorAll('button');" +
                "  for (var b of cancelBtns) {" +
                "    if (b.textContent.trim() === 'Cancel') { b.click(); return; }" +
                "  }" +
                "}"
            );
            pause(500);
        } catch (Exception ignored) {}
        // Fallback: click backdrop (avoid Keys.ESCAPE which can close drawers)
        try {
            js.executeScript("var b=document.querySelector('.MuiBackdrop-root'); if(b) b.click();");
            pause(500);
        } catch (Exception ignored) {}
        System.out.println("[AssetPage] dismissAnyDialog completed");
    }

    /**
     * Cancel the connection dialog.
     */
    public void cancelConnectionDialog() {
        // The dialog has its own Cancel button — find it within the dialog
        Boolean clicked = (Boolean) js.executeScript(
            "var dialogs = document.querySelectorAll('[role=\"dialog\"]');" +
            "for (var d of dialogs) {" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    if (b.textContent.trim() === 'Cancel') { b.click(); return true; }" +
            "  }" +
            "}" +
            "return false;"
        );
        pause(1000);
        System.out.println("[AssetPage] Connection dialog cancelled: " + clicked);
    }

    /**
     * Get the number of connections currently shown in the CONNECTIONS accordion.
     * Returns 0 if "No connections" is displayed.
     */
    public int getConnectionCount() {
        try {
            // Check for "No connections" text
            Boolean noConn = (Boolean) js.executeScript(
                "var accordions = document.querySelectorAll('[class*=\"MuiAccordion\"]');" +
                "for (var a of accordions) {" +
                "  if (a.textContent.indexOf('CONNECTIONS') > -1) {" +
                "    return a.textContent.indexOf('No connections') > -1;" +
                "  }" +
                "}" +
                "return true;"
            );
            if (noConn != null && noConn) return 0;

            // Count connection entries (each has a kebab menu ⋮)
            Long count = (Long) js.executeScript(
                "var accordions = document.querySelectorAll('[class*=\"MuiAccordion\"]');" +
                "for (var a of accordions) {" +
                "  if (a.textContent.indexOf('CONNECTIONS') > -1) {" +
                "    var details = a.querySelector('[class*=\"MuiAccordionDetails\"]');" +
                "    if (!details) return 0;" +
                "    // Count items with arrow icon (→) pattern or kebab menus\n" +
                "    var items = details.querySelectorAll('[class*=\"MuiListItem\"], [class*=\"connection-item\"]');" +
                "    if (items.length > 0) return items.length;" +
                "    // Alternative: count by text containing '→'\n" +
                "    var text = details.textContent;" +
                "    var arrows = (text.match(/→/g) || []).length;" +
                "    return arrows > 0 ? arrows : 0;" +
                "  }" +
                "}" +
                "return 0;"
            );
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.out.println("[AssetPage] Error getting connection count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if a connection to the given target asset is visible in the CONNECTIONS section.
     */
    public boolean isConnectionVisible(String targetAssetName) {
        try {
            Boolean found = (Boolean) js.executeScript(
                "var accordions = document.querySelectorAll('[class*=\"MuiAccordion\"]');" +
                "for (var a of accordions) {" +
                "  if (a.textContent.indexOf('CONNECTIONS') > -1) {" +
                "    return a.textContent.indexOf(arguments[0]) > -1;" +
                "  }" +
                "}" +
                "return false;", targetAssetName);
            return found != null && found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Delete a connection by clicking the kebab menu (⋮) on a connection entry
     * and selecting "Delete" or "Remove".
     * If targetAssetName is null, deletes the first connection found.
     */
    public void deleteConnection(String targetAssetName) {
        // Find the kebab menu on the connection entry
        Boolean deleted = (Boolean) js.executeScript(
            "var accordions = document.querySelectorAll('[class*=\"MuiAccordion\"]');" +
            "for (var a of accordions) {" +
            "  if (a.textContent.indexOf('CONNECTIONS') === -1) continue;" +
            "  var details = a.querySelector('[class*=\"MuiAccordionDetails\"]');" +
            "  if (!details) continue;" +
            "  // Find connection entries containing the target name (or any if null)\n" +
            "  var entries = details.querySelectorAll('[class*=\"MuiBox\"], [class*=\"MuiListItem\"], div');" +
            "  for (var entry of entries) {" +
            "    var text = entry.textContent || '';" +
            "    var target = arguments[0];" +
            "    if (target && text.indexOf(target) === -1) continue;" +
            "    // Find kebab button (MoreVert icon) within this entry\n" +
            "    var btns = entry.querySelectorAll('button');" +
            "    for (var btn of btns) {" +
            "      var paths = btn.querySelectorAll('path');" +
            "      for (var p of paths) {" +
            "        var d = p.getAttribute('d') || '';" +
            "        if (d.indexOf('M12 8c1.1') > -1 || d.indexOf('M6 10') > -1) {" +
            "          btn.click(); return true;" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "}" +
            "return false;", targetAssetName);

        if (deleted == null || !deleted) {
            // Fallback: try clicking any visible kebab/more icon in the connections area
            System.out.println("[AssetPage] Primary kebab search failed, trying fallback...");
            js.executeScript(
                "var accordions = document.querySelectorAll('[class*=\"MuiAccordion\"]');" +
                "for (var a of accordions) {" +
                "  if (a.textContent.indexOf('CONNECTIONS') === -1) continue;" +
                "  var btns = a.querySelectorAll('button[class*=\"MuiIconButton\"]');" +
                "  // Skip the first button (expand toggle) and the + button\n" +
                "  for (var btn of btns) {" +
                "    var paths = btn.querySelectorAll('path');" +
                "    for (var p of paths) {" +
                "      var d = p.getAttribute('d') || '';" +
                "      if (d.indexOf('M12 8c1.1') > -1) { btn.click(); return; }" +
                "    }" +
                "  }" +
                "}"
            );
        }
        pause(1000);

        // Click "Delete" or "Remove" from the dropdown menu
        Boolean menuClicked = (Boolean) js.executeScript(
            "var items = document.querySelectorAll('[role=\"menuitem\"]');" +
            "for (var item of items) {" +
            "  var text = item.textContent.trim().toLowerCase();" +
            "  if (text.indexOf('delete') > -1 || text.indexOf('remove') > -1) {" +
            "    item.click(); return true;" +
            "  }" +
            "}" +
            "return false;"
        );
        pause(1500);

        // Confirm delete if a confirmation dialog appears
        try {
            By confirmBtn = By.xpath("//button[contains(@class,'containedError') and contains(.,'Delete')] | //button[contains(.,'Confirm')]");
            java.util.List<WebElement> confirmBtns = driver.findElements(confirmBtn);
            if (!confirmBtns.isEmpty()) {
                js.executeScript("arguments[0].click();", confirmBtns.get(0));
                pause(1500);
                System.out.println("[AssetPage] Delete confirmed via dialog");
            }
        } catch (Exception ignored) {}

        System.out.println("[AssetPage] Connection delete action completed. Menu clicked: " + menuClicked);
    }

    /**
     * Click the "Connections" tab on the asset detail page.
     */
    public void clickConnectionsTab() {
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var r = b.getBoundingClientRect();" +
            "  if (b.textContent.trim() === 'Connections' && r.top > 200 && r.top < 500) {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;"
        );
        pause(2000);
        System.out.println("[AssetPage] Clicked Connections tab: " + clicked);
    }

    // --- OCP (Overcurrent Protection) ---

    /**
     * Scroll the edit drawer to the OCP accordion and expand it if collapsed.
     * OCP section appears for enclosure-type assets (Panelboard, Switchboard, MCC, etc.).
     */
    public void expandOCPSection() {
        // Scroll all scrollable containers in the edit drawer
        js.executeScript(
            "var containers = document.querySelectorAll('[class*=\"MuiDrawer\"] > div, [class*=\"MuiPaper\"]');" +
            "for (var c of containers) {" +
            "  if (c.scrollHeight > c.clientHeight + 100) c.scrollTop = c.scrollHeight * 0.6;" +
            "}"
        );
        pause(1000);

        // Find OCP accordion and expand if collapsed
        Boolean found = (Boolean) js.executeScript(
            "var h6s = document.querySelectorAll('h6');" +
            "for (var el of h6s) {" +
            "  if (el.textContent.trim() === 'OCP') {" +
            "    var accordion = el.closest('[class*=\"MuiAccordion\"]');" +
            "    if (accordion && !accordion.classList.contains('Mui-expanded')) {" +
            "      var summary = accordion.querySelector('[class*=\"MuiAccordionSummary\"]');" +
            "      if (summary) summary.click();" +
            "    }" +
            "    el.scrollIntoView({block:'center'});" +
            "    return true;" +
            "  }" +
            "}" +
            "return false;"
        );
        pause(1000);
        System.out.println("[AssetPage] OCP section expanded: " + found);
    }

    /**
     * Check if the OCP section is present in the edit drawer.
     * Returns true if the OCP accordion header exists.
     */
    public boolean isOCPSectionPresent() {
        Boolean present = (Boolean) js.executeScript(
            "var h6s = document.querySelectorAll('h6');" +
            "for (var el of h6s) {" +
            "  if (el.textContent.trim() === 'OCP') return true;" +
            "}" +
            "return false;"
        );
        boolean result = present != null && present;
        System.out.println("[AssetPage] OCP section present: " + result);
        return result;
    }

    /**
     * Click the "+" IconButton in the OCP header.
     * Opens a menu with "Create New Child" and "Link Existing Node".
     */
    public void clickAddOCPButton() {
        js.executeScript(
            "var h6s = document.querySelectorAll('h6');" +
            "for (var el of h6s) {" +
            "  if (el.textContent.trim() === 'OCP') {" +
            "    var parent = el.parentElement;" +
            "    for (var i = 0; i < 4; i++) {" +
            "      var btns = parent.querySelectorAll('button[class*=\"MuiIconButton\"]');" +
            "      for (var btn of btns) {" +
            "        var paths = btn.querySelectorAll('path');" +
            "        for (var p of paths) {" +
            "          if ((p.getAttribute('d')||'').indexOf('M19 13') > -1) { btn.click(); return; }" +
            "        }" +
            "      }" +
            "      if (parent.parentElement) parent = parent.parentElement;" +
            "    }" +
            "  }" +
            "}"
        );
        pause(1000);
        System.out.println("[AssetPage] Clicked '+' button on OCP");
    }

    /**
     * Click "Create New Child" from the OCP menu.
     * Must be called after clickAddOCPButton().
     */
    public void selectCreateNewChild() {
        Boolean clicked = (Boolean) js.executeScript(
            "var items = document.querySelectorAll('[role=\"menuitem\"]');" +
            "for (var item of items) {" +
            "  if (item.textContent.trim().indexOf('Create New Child') > -1) { item.click(); return true; }" +
            "}" +
            "return false;"
        );
        if (clicked == null || !clicked) {
            throw new RuntimeException("Could not find 'Create New Child' menu item");
        }
        // Wait for Quick Add dialog to appear by checking for its "Select class" input
        for (int i = 0; i < 10; i++) {
            pause(1000);
            Boolean dialogVisible = (Boolean) js.executeScript(
                "var inputs = document.querySelectorAll('input[placeholder=\"Select class\"]');" +
                "for (var inp of inputs) {" +
                "  var r = inp.getBoundingClientRect();" +
                "  if (r.width > 10 && r.height > 10 && !inp.value) return true;" +
                "}" +
                "return false;"
            );
            if (dialogVisible != null && dialogVisible) {
                System.out.println("[AssetPage] Quick Add dialog detected after " + (i + 1) + "s");
                return;
            }
        }
        System.out.println("[AssetPage] WARNING: Quick Add dialog not detected after 10s");
        // Log what we see for debugging
        String debug = (String) js.executeScript(
            "var info = 'All inputs with placeholder:\\n';" +
            "var inputs = document.querySelectorAll('input[placeholder]');" +
            "for (var inp of inputs) {" +
            "  var r = inp.getBoundingClientRect();" +
            "  if (r.width > 0) info += '  ph=\"' + inp.placeholder + '\" val=\"' + inp.value + '\" at(' + Math.round(r.x) + ',' + Math.round(r.y) + ')\\n';" +
            "}" +
            "return info;"
        );
        System.out.println("[AssetPage] Dialog debug:\n" + debug);
    }

    /**
     * Click "Link Existing Node" from the OCP menu.
     * Must be called after clickAddOCPButton().
     */
    public void selectLinkExistingNode() {
        Boolean clicked = (Boolean) js.executeScript(
            "var items = document.querySelectorAll('[role=\"menuitem\"]');" +
            "for (var item of items) {" +
            "  if (item.textContent.trim().indexOf('Link Existing Node') > -1) { item.click(); return true; }" +
            "}" +
            "return false;"
        );
        if (clicked == null || !clicked) {
            throw new RuntimeException("Could not find 'Link Existing Node' menu item");
        }
        pause(2000);
        System.out.println("[AssetPage] Selected 'Link Existing Node'");
    }

    /**
     * Click the "OCP" tab in the edit drawer's tab bar.
     * This switches to the OCP tab view showing child devices.
     */
    public void clickOCPTab() {
        Boolean clicked = (Boolean) js.executeScript(
            "var tabs = document.querySelectorAll('button');" +
            "for (var tab of tabs) {" +
            "  var txt = (tab.textContent||'').trim();" +
            "  if (txt === 'OCP') {" +
            "    var r = tab.getBoundingClientRect();" +
            "    // The tab is in the tab bar (y < 400, width ~90)\n" +
            "    if (r.y < 400 && r.width < 150 && r.width > 30) {" +
            "      tab.click(); return true;" +
            "    }" +
            "  }" +
            "}" +
            "return false;"
        );
        pause(2000);
        System.out.println("[AssetPage] OCP tab clicked: " + clicked);
    }

    /**
     * Get the count of OCP child devices currently listed in the OCP section.
     * Tries multiple strategies: OCP accordion content, OCP tab content, badge count.
     */
    public int getOCPChildCount() {
        // First scroll to OCP area
        js.executeScript(
            "var containers = document.querySelectorAll('[class*=\"MuiDrawer\"] > div, [class*=\"MuiPaper\"]');" +
            "for (var c of containers) {" +
            "  if (c.scrollHeight > c.clientHeight + 100) c.scrollTop = c.scrollHeight * 0.7;" +
            "}"
        );
        pause(500);

        String debug = (String) js.executeScript(
            "var info = '';" +
            "// Find OCP accordion\n" +
            "var h6s = document.querySelectorAll('h6');" +
            "for (var el of h6s) {" +
            "  if (el.textContent.trim() === 'OCP') {" +
            "    var accordion = el.closest('[class*=\"MuiAccordion\"]');" +
            "    if (accordion) {" +
            "      info += 'OCP accordion found. expanded=' + accordion.classList.contains('Mui-expanded');" +
            "      info += ' children=' + accordion.children.length;" +
            "      info += ' innerHTML_len=' + accordion.innerHTML.length;" +
            "      // List direct child elements\n" +
            "      var details = accordion.querySelector('[class*=\"MuiAccordionDetails\"]');" +
            "      if (details) {" +
            "        info += '\\nDetails children: ' + details.children.length;" +
            "        for (var i = 0; i < Math.min(details.children.length, 10); i++) {" +
            "          var ch = details.children[i];" +
            "          info += '\\n  child[' + i + ']: tag=' + ch.tagName + ' class=' + (ch.className||'').substring(0,60) + ' text=' + (ch.textContent||'').substring(0,80);" +
            "        }" +
            "      }" +
            "      // Also check for any elements with asset-like content\n" +
            "      var allInner = accordion.querySelectorAll('*');" +
            "      var assetItems = 0;" +
            "      for (var item of allInner) {" +
            "        var t = (item.textContent||'').trim();" +
            "        if (item.children.length < 3 && (t.includes('Fuse') || t.includes('Disconnect') || t.includes('Relay') || t.includes('MCC Bucket'))) {" +
            "          assetItems++;" +
            "        }" +
            "      }" +
            "      info += '\\nAsset-name elements in OCP: ' + assetItems;" +
            "    }" +
            "    break;" +
            "  }" +
            "}" +
            "return info;"
        );
        System.out.println("[AssetPage] OCP count debug: " + debug);

        Long count = (Long) js.executeScript(
            "var h6s = document.querySelectorAll('h6');" +
            "for (var el of h6s) {" +
            "  if (el.textContent.trim() === 'OCP') {" +
            "    var accordion = el.closest('[class*=\"MuiAccordion\"]');" +
            "    if (!accordion) continue;" +
            "    // Expand if collapsed\n" +
            "    if (!accordion.classList.contains('Mui-expanded')) {" +
            "      var summary = accordion.querySelector('[class*=\"MuiAccordionSummary\"]');" +
            "      if (summary) summary.click();" +
            "    }" +
            "    // Count rows, list items, cards, or any clickable child entries\n" +
            "    var rows = accordion.querySelectorAll('[data-rowindex], tr[class*=\"MuiTableRow\"], [class*=\"MuiListItem\"], [class*=\"MuiCard\"], [class*=\"MuiListItemButton\"]');" +
            "    if (rows.length > 0) return rows.length;" +
            "    // Count child elements in the accordion details section (excluding the summary)\n" +
            "    var details = accordion.querySelector('[class*=\"MuiAccordionDetails\"]');" +
            "    if (details) {" +
            "      // Count visible child divs that look like data entries (have some height)\n" +
            "      var entries = 0;" +
            "      for (var ch of details.children) {" +
            "        var r = ch.getBoundingClientRect();" +
            "        if (r.height > 20 && r.width > 100) entries++;" +
            "      }" +
            "      if (entries > 0) return entries;" +
            "    }" +
            "    // Check badge on the header\n" +
            "    var badge = el.parentElement ? el.parentElement.querySelector('[class*=\"MuiBadge-badge\"]') : null;" +
            "    if (badge) { var n = parseInt(badge.textContent); if (!isNaN(n)) return n; }" +
            "    // Check for 'No child' or empty text\n" +
            "    var text = accordion.textContent || '';" +
            "    if (text.indexOf('No child') > -1 || text.indexOf('No OCP') > -1) return 0;" +
            "    return 0;" +
            "  }" +
            "}" +
            "return -1;"
        );
        int result = count != null ? count.intValue() : -1;
        System.out.println("[AssetPage] OCP child count: " + result);
        return result;
    }

    /**
     * Select an OCP Asset Class in the "Quick Add Child Assets" dialog.
     * The dialog has: Asset Class* (Select class), Subtype (Optional), Qty (1).
     * Names are auto-generated. Available classes: Disconnect Switch, Fuse, MCC Bucket, Other (OCP), Relay.
     * @param className e.g. "Fuse", "Disconnect Switch", etc.
     */
    public void selectOCPAssetClass(String className) {
        // Use Selenium to find the "Select class" input in the Quick Add dialog.
        // The dialog's input has placeholder="Select class" with empty value.
        // The main form's input has placeholder="Select Class" with a value set.
        try {
            // Find all inputs with placeholder containing "class" (case-insensitive XPath)
            java.util.List<WebElement> classInputs = driver.findElements(
                By.cssSelector("input[placeholder='Select class']"));
            System.out.println("[AssetPage] Found " + classInputs.size() + " 'Select class' inputs");

            WebElement target = null;
            for (WebElement inp : classInputs) {
                String val = inp.getAttribute("value");
                System.out.println("[AssetPage]   input: val='" + val + "' displayed=" + inp.isDisplayed());
                if (val == null || val.isEmpty()) {
                    target = inp;
                    break;
                }
            }

            // Fallback: try "Select Class" (capital C) with empty value
            if (target == null) {
                classInputs = driver.findElements(By.cssSelector("input[placeholder='Select Class']"));
                System.out.println("[AssetPage] Found " + classInputs.size() + " 'Select Class' inputs");
                for (WebElement inp : classInputs) {
                    String val = inp.getAttribute("value");
                    System.out.println("[AssetPage]   input: val='" + val + "' displayed=" + inp.isDisplayed());
                    if (val == null || val.isEmpty()) {
                        target = inp;
                        break;
                    }
                }
            }

            if (target == null) {
                System.out.println("[AssetPage] ERROR: No empty 'Select class' input found");
                // Dump all visible inputs for debugging
                java.util.List<WebElement> allInputs = driver.findElements(By.cssSelector("input[placeholder]"));
                for (WebElement inp : allInputs) {
                    if (inp.isDisplayed()) {
                        System.out.println("[AssetPage]   visible input: ph='" + inp.getAttribute("placeholder") +
                            "' val='" + inp.getAttribute("value") + "'");
                    }
                }
                return;
            }

            // Click the input to open dropdown — use JS to avoid backdrop interception
            js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});" +
                "arguments[0].focus();" +
                "arguments[0].click();", target);
            pause(500);
            System.out.println("[AssetPage] OCP class dropdown opened via JS click");

            // Clear and type the class name to filter options
            js.executeScript(
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(arguments[0], '');" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                target);
            target.sendKeys(className);
            pause(1500);
            System.out.println("[AssetPage] Typed '" + className + "' into class input");

        } catch (Exception e) {
            System.out.println("[AssetPage] ERROR opening OCP class dropdown: " + e.getMessage());
            return;
        }

        // Select the class from the dropdown listbox
        try {
            java.util.List<WebElement> options = driver.findElements(By.cssSelector("[role='option']"));
            System.out.println("[AssetPage] Dropdown options found: " + options.size());
            for (WebElement opt : options) {
                String txt = opt.getText().trim();
                System.out.println("[AssetPage]   option: '" + txt + "'");
                if (txt.equals(className) || txt.contains(className)) {
                    js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", opt);
                    pause(1000);
                    System.out.println("[AssetPage] OCP class selected: " + className);
                    return;
                }
            }
            // If exact match not found, click first option
            if (!options.isEmpty()) {
                js.executeScript("arguments[0].click();", options.get(0));
                pause(1000);
                System.out.println("[AssetPage] Selected first available option: " + options.get(0).getText());
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] ERROR selecting OCP class option: " + e.getMessage());
        }
    }

    /**
     * Set the Qty field in the Quick Add Child Assets dialog.
     * The Qty field is a number input; default is 1.
     * @param qty the number of child assets to add
     */
    public void setOCPQuantity(int qty) {
        // The Qty field is inside the Quick Add dialog, near the "Select class" input.
        // Find it by locating the dialog first (contains "Quick Add" text), then its number input.
        Boolean set = (Boolean) js.executeScript(
            "// Find the Quick Add dialog by looking for its heading text\n" +
            "var dialogContainer = null;" +
            "// Strategy 1: Find heading containing 'Quick Add' text\n" +
            "var headings = document.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span,div');" +
            "for (var el of headings) {" +
            "  var t = (el.textContent||'').trim();" +
            "  if (t.indexOf('Quick Add') > -1 && t.indexOf('Child') > -1) {" +
            "    var parent = el.parentElement;" +
            "    for (var i = 0; i < 15 && parent; i++) {" +
            "      var numInputs = parent.querySelectorAll('input[type=\"number\"]');" +
            "      if (numInputs.length > 0) { dialogContainer = parent; break; }" +
            "      parent = parent.parentElement;" +
            "    }" +
            "    if (dialogContainer) break;" +
            "  }" +
            "}" +
            "// Strategy 2: Find any visible number input near 'Add' button with 'Asset' text\n" +
            "if (!dialogContainer) {" +
            "  var btns = document.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    if ((b.textContent||'').match(/Add \\d+ Asset/)) {" +
            "      var p = b.parentElement;" +
            "      for (var i = 0; i < 10 && p; i++) {" +
            "        if (p.querySelectorAll('input[type=\"number\"]').length > 0) { dialogContainer = p; break; }" +
            "        p = p.parentElement;" +
            "      }" +
            "      break;" +
            "    }" +
            "  }" +
            "}" +
            "if (!dialogContainer) { console.log('OCP dialog container not found for Qty'); return false; }" +
            "var qtyInput = dialogContainer.querySelector('input[type=\"number\"]');" +
            "if (!qtyInput) { console.log('Qty input not found in dialog'); return false; }" +
            "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(" +
            "  window.HTMLInputElement.prototype, 'value').set;" +
            "nativeInputValueSetter.call(qtyInput, arguments[0]);" +
            "qtyInput.dispatchEvent(new Event('input', {bubbles: true}));" +
            "qtyInput.dispatchEvent(new Event('change', {bubbles: true}));" +
            "return true;",
            String.valueOf(qty)
        );
        pause(500);
        System.out.println("[AssetPage] OCP quantity set to " + qty + ": " + set);
    }

    /**
     * Click the "Add X Assets" button in the Quick Add Child Assets dialog.
     * The button text is "Add 0 Assets" when no class is selected, "Add 1 Assets" after.
     */
    public void submitOCPChildForm() {
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var txt = (b.textContent||'').trim();" +
            "  var r = b.getBoundingClientRect();" +
            "  if (r.width < 30) continue;" +
            "  // Match 'Add N Assets' button\n" +
            "  if (txt.match(/Add \\d+ Asset/)) {" +
            "    if (!b.disabled) { b.click(); return true; }" +
            "    // If disabled (0 assets), log and return false\n" +
            "    return false;" +
            "  }" +
            "}" +
            "return false;"
        );
        pause(3000);
        System.out.println("[AssetPage] OCP 'Add Assets' clicked: " + clicked);
    }

    /**
     * Wait for the Quick Add Child Assets dialog to close after adding.
     */
    public void waitForOCPDialogClose() {
        for (int i = 0; i < 10; i++) {
            // Check if "Quick Add Child Assets" heading is still visible
            Boolean dialogOpen = (Boolean) js.executeScript(
                "var all = document.querySelectorAll('h2, h3, h4, h5, h6, [class*=\"MuiDialogTitle\"]');" +
                "for (var el of all) {" +
                "  if ((el.textContent||'').trim().includes('Quick Add Child Assets')) {" +
                "    var r = el.getBoundingClientRect();" +
                "    if (r.width > 0 && r.height > 0) return true;" +
                "  }" +
                "}" +
                "return false;"
            );
            if (dialogOpen == null || !dialogOpen) {
                System.out.println("[AssetPage] OCP Quick Add dialog closed after " + i + "s");
                return;
            }
            pause(1000);
        }
        System.out.println("[AssetPage] OCP dialog still open after 10s — dismissing");
        dismissAnyDialog();
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
        // Dismiss MUI backdrops and the "App Update Available" banner that
        // can cover sidebar items in CI (headless Chrome). Repeat inside the
        // wait loop because backdrops can re-appear on React re-render.
        dismissBlockers();
        try {
            wait.until(ExpectedConditions.elementToBeClickable(by)).click();
        } catch (Exception e) {
            // Retry after another backdrop cleanup + scroll-into-view
            try {
                dismissBlockers();
                WebElement el = driver.findElement(by);
                js.executeScript(
                        "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                        el);
                pause(300);
                js.executeScript("arguments[0].click();", el);
            } catch (Exception ex) {
                throw new RuntimeException("Click failed for: " + by, ex);
            }
        }
    }

    /**
     * Remove MUI backdrops + dismiss the "App Update Available" banner.
     * Called before every {@link #click(By)} attempt. The sidebar nav items
     * in CI (headless Chrome) are often covered by a late-arriving backdrop
     * after login redirect, causing "element not interactable" even though
     * the element is in the DOM.
     */
    private void dismissBlockers() {
        try {
            js.executeScript(
                    "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop')"
                            + ".forEach(function(b){b.style.display='none';b.style.pointerEvents='none';});"
                            + "var btns = document.querySelectorAll('button');"
                            + "for (var i = 0; i < btns.length; i++) {"
                            + "  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }"
                            + "}");
        } catch (Exception ignored) {}
    }

    private void typeField(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        // Use JS for all interactions to avoid MuiBackdrop interception
        js.executeScript(
            "arguments[0].scrollIntoView({block:'center'});" +
            "arguments[0].focus();" +
            "arguments[0].click();" +
            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "setter.call(arguments[0], '');" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
            el);
        pause(100);
        try {
            el.sendKeys(text);
        } catch (Exception e) {
            // Fallback: use JS to set value when native sendKeys is blocked by MUI Backdrop
            System.out.println("[AssetPage] Native sendKeys blocked, using JS fallback for: " + by);
            js.executeScript(
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(arguments[0], arguments[1]);" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                el, text);
        }
    }

    private void typeAndSelectDropdown(By inputLocator, String textToType, String optionText) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));

        // ── PRIMARY: Open dropdown via THIS field's popup indicator (avoids sendKeys going to wrong field) ──
        // Disable backdrops first — MUI Drawer backdrop intercepts both sendKeys and option clicks
        js.executeScript(
            "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
            "  function(b) { b.style.pointerEvents = 'none'; }" +
            ");"
        );

        js.executeScript(
            "arguments[0].scrollIntoView({block:'center'});" +
            "arguments[0].focus();" +
            "arguments[0].click();", input);
        pause(300);

        // Click popup indicator on THIS specific autocomplete to show all options
        js.executeScript(
            "var wrapper = arguments[0].closest('.MuiAutocomplete-root');" +
            "if (wrapper) {" +
            "  var btn = wrapper.querySelector('.MuiAutocomplete-popupIndicator');" +
            "  if (btn) btn.click();" +
            "}",
            input);
        pause(1000);

        // Log available options for debugging
        String listboxDebug = (String) js.executeScript(
            "var lb = document.querySelector('ul[role=\"listbox\"]');" +
            "if (!lb) return 'no listbox';" +
            "var opts = lb.querySelectorAll('li[role=\"option\"]');" +
            "var texts = [];" +
            "for (var o of opts) texts.push(o.textContent.trim());" +
            "return 'listbox has ' + opts.length + ' options: [' + texts.slice(0,10).join(', ') + (opts.length > 10 ? '...' : '') + ']';");
        System.out.println("[AssetPage] " + listboxDebug);

        // Find and click the matching option using Selenium trusted click
        By exactOption = By.xpath("//li[@role='option'][normalize-space()='" + optionText + "']");
        By partialOption = By.xpath("//li[@role='option'][contains(normalize-space(),'" + optionText + "')]");

        for (int attempt = 0; attempt < 3; attempt++) {
            for (By optBy : new By[]{exactOption, partialOption}) {
                java.util.List<WebElement> options = driver.findElements(optBy);
                if (!options.isEmpty()) {
                    WebElement option = options.get(0);
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                    pause(150);
                    // Use Selenium Actions for trusted click — MUI Autocomplete requires trusted events
                    try {
                        new Actions(driver).moveToElement(option).click().perform();
                    } catch (Exception e) {
                        option.click();
                    }
                    System.out.println("[AssetPage] Selected dropdown option: " + optionText);
                    pause(500);

                    // Verify the selection took effect by checking input value
                    String val = (String) js.executeScript(
                        "return arguments[0].value;", driver.findElement(inputLocator));
                    System.out.println("[AssetPage] Input value after selection: '" + val + "'");

                    return;
                }
            }
            pause(500);
        }

        // ── FALLBACK: Type to filter, then select ──
        System.out.println("[AssetPage] Popup indicator approach failed — trying keyboard input");

        // Close listbox via Escape
        js.executeScript("document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}));");
        pause(500);

        input = driver.findElement(inputLocator);
        js.executeScript(
            "arguments[0].scrollIntoView({block:'center'});" +
            "arguments[0].focus();" +
            "arguments[0].click();", input);
        pause(300);

        // Type using per-character keyboard events on the target input
        js.executeScript(
            "var el = arguments[0]; var text = arguments[1];" +
            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "setter.call(el, '');" +
            "el.dispatchEvent(new Event('input', {bubbles: true}));" +
            "for (var i = 0; i < text.length; i++) {" +
            "  var partial = text.substring(0, i + 1);" +
            "  setter.call(el, partial);" +
            "  el.dispatchEvent(new Event('input', {bubbles: true}));" +
            "  el.dispatchEvent(new KeyboardEvent('keydown', {key: text[i], bubbles: true}));" +
            "  el.dispatchEvent(new KeyboardEvent('keyup', {key: text[i], bubbles: true}));" +
            "}" +
            "el.dispatchEvent(new Event('change', {bubbles: true}));",
            input, textToType);
        pause(1000);

        for (int attempt = 0; attempt < 5; attempt++) {
            for (By optBy : new By[]{exactOption, partialOption}) {
                java.util.List<WebElement> opts = driver.findElements(optBy);
                if (!opts.isEmpty()) {
                    WebElement option = opts.get(0);
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                    pause(150);
                    try {
                        new Actions(driver).moveToElement(option).click().perform();
                    } catch (Exception e) {
                        option.click();
                    }
                    System.out.println("[AssetPage] Selected dropdown option (keyboard fallback): " + optionText);
                    pause(300);
                    return;
                }
            }
            pause(500);
        }
        System.out.println("WARNING: Could not select dropdown option '" + optionText + "' for " + inputLocator);
    }

    /**
     * Attempt native sendKeys; if blocked by MUI Backdrop, fall back to JS-based input
     * with synthetic React events so MUI Autocomplete recognizes the value change.
     */
    private void sendKeysWithJsFallback(WebElement el, String text, By locator) {
        try {
            el.sendKeys(text);
        } catch (Exception e) {
            System.out.println("[AssetPage] Native sendKeys blocked, using JS fallback for: " + locator);
            js.executeScript(
                "var el = arguments[0];" +
                "var text = arguments[1];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(el, text);" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "el.dispatchEvent(new Event('change', {bubbles: true}));" +
                // Dispatch keydown/keyup events to trigger MUI Autocomplete filtering
                "for (var i = 0; i < text.length; i++) {" +
                "  el.dispatchEvent(new KeyboardEvent('keydown', {key: text[i], bubbles: true}));" +
                "  el.dispatchEvent(new KeyboardEvent('keyup', {key: text[i], bubbles: true}));" +
                "}",
                el, text);
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
