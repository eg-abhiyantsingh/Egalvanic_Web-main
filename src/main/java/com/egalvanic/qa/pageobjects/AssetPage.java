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
    private static final By LOCATIONS_NAV = By.xpath("//span[normalize-space()='Locations'] | //a[normalize-space()='Locations'] | //button[normalize-space()='Locations']");
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

        // If already on Assets page, navigate away then back to force fresh data.
        // driver.navigate().refresh() loses SPA state (site selector resets).
        // Re-clicking Assets nav on same page can trigger React crash.
        // Solution: go to Locations briefly, then back to Assets.
        if (driver.findElements(CREATE_ASSET_BTN).size() > 0 && isOnAssetsPage()) {
            System.out.println("[AssetPage] Already on Assets page — navigating away and back for fresh data");
            try {
                driver.findElement(LOCATIONS_NAV).click();
                pause(1500);
            } catch (Exception e) {
                System.out.println("[AssetPage] Could not navigate to Locations: " + e.getMessage());
            }
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
     * Get the asset name from the first row in the grid (first cell text).
     */
    public String getFirstRowAssetName() {
        try {
            By firstCell = By.xpath("(//div[contains(@class,'MuiDataGrid-row') and @data-rowindex])[1]//div[contains(@class,'MuiDataGrid-cell')][1]");
            // Use presenceOfElementLocated instead of visibilityOfElementLocated
            // because CSS zoom (80%) causes Selenium to miscalculate element visibility
            WebElement cell = wait.until(ExpectedConditions.presenceOfElementLocated(firstCell));
            // Use JS textContent — more reliable than getText() with CSS zoom
            String text = (String) js.executeScript("return arguments[0].textContent.trim();", cell);
            System.out.println("[AssetPage] First row asset name: '" + text + "'");
            return (text == null || text.isEmpty()) ? null : text;
        } catch (Exception e) {
            System.out.println("[AssetPage] Could not read first row asset name: " + e.getMessage());
            return null;
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

            // Step 2: Navigate to Locations then back (avoids SPA state loss from page refresh)
            try {
                driver.findElement(LOCATIONS_NAV).click();
                pause(1500);
                driver.findElement(ASSETS_NAV).click();
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
        // The header row also matches MuiDataGrid-row (via MuiDataGrid-row--borderBottom)
        // but does NOT have data-rowindex. Only real data rows have it.
        By firstDataRow = By.xpath("(//div[contains(@class,'MuiDataGrid-row') and @data-rowindex])[1]");
        WebElement row = wait.until(ExpectedConditions.presenceOfElementLocated(firstDataRow));
        String currentUrl = driver.getCurrentUrl();

        String dataId = row.getDomAttribute("data-id");
        System.out.println("[AssetPage] First data row: data-id=" + dataId + ", data-rowindex=" + row.getDomAttribute("data-rowindex"));

        // Strategy 1: data-id UUID → direct URL navigation (most reliable)
        if (dataId != null && !dataId.isEmpty() && dataId.contains("-")) {
            String baseUrl = currentUrl.replaceAll("/assets.*", "/assets/");
            driver.get(baseUrl + dataId);
            waitForDetailPageLoad();
            System.out.println("[AssetPage] Navigated via data-id — URL: " + driver.getCurrentUrl());
            if (driver.getCurrentUrl().contains(dataId)) return;
        }

        // Strategy 2: Find <a> link inside the row
        try {
            String href = (String) js.executeScript(
                    "var all = arguments[0].querySelectorAll('a[href]');" +
                    "for(var i=0; i<all.length; i++) { " +
                    "  if(all[i].href.includes('/assets/')) return all[i].href; " +
                    "}" +
                    "return '';", row);
            if (href != null && !href.isEmpty()) {
                driver.get(href);
                pause(3000);
                if (!driver.getCurrentUrl().equals(currentUrl)) {
                    System.out.println("[AssetPage] Strategy 2 (href) succeeded: " + driver.getCurrentUrl());
                    return;
                }
            }
        } catch (Exception ignored) {}

        // Strategy 3: Click the first cell (asset name)
        try {
            WebElement cell = row.findElement(By.xpath(".//div[contains(@class,'MuiDataGrid-cell')][1]"));
            System.out.println("[AssetPage] Clicking first cell: '" + cell.getText() + "'");
            cell.click();
            pause(3000);
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                System.out.println("[AssetPage] Strategy 3 (cell click) succeeded: " + driver.getCurrentUrl());
                return;
            }
        } catch (Exception e) {
            System.out.println("[AssetPage] Cell click failed: " + e.getMessage());
        }

        // Strategy 4: Click the row itself
        try {
            row.click();
            pause(3000);
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                System.out.println("[AssetPage] Strategy 4 (row click) succeeded: " + driver.getCurrentUrl());
                return;
            }
        } catch (Exception ignored) {}

        System.out.println("[AssetPage] FAILED to navigate. Current URL: " + driver.getCurrentUrl());
        throw new RuntimeException("Could not navigate to asset detail page from grid.");
    }

    /**
     * Wait for the asset detail page to fully load after direct URL navigation.
     * Waits for the loading spinner to disappear and content to render.
     */
    private void waitForDetailPageLoad() {
        // Wait for initial page load
        pause(2000);

        // Wait for spinner to disappear (up to 20s)
        for (int i = 0; i < 20; i++) {
            // Check if content has loaded — use tagName (not XPath) due to SVG namespace issues
            java.util.List<WebElement> buttons = driver.findElements(By.tagName("button"));
            java.util.List<WebElement> headings = driver.findElements(
                    By.xpath("//h1 | //h2 | //h3 | //h4 | //h5 | //h6 | //*[contains(@class,'title') or contains(@class,'header')]//span"));

            // Check for loading spinner
            java.util.List<WebElement> spinners = driver.findElements(
                    By.xpath("//*[contains(@class,'MuiCircularProgress') or contains(@class,'spinner') or contains(@class,'loading')]" +
                            " | //svg[contains(@class,'MuiCircularProgress')]" +
                            " | //*[@role='progressbar']"));

            System.out.println("[AssetPage] Detail page load check " + i + ": buttons=" + buttons.size()
                    + ", headings=" + headings.size() + ", spinners=" + spinners.size());

            // Content loaded if we have enough buttons (spinners may persist for lazy-loaded sections)
            if (buttons.size() > 10) {
                System.out.println("[AssetPage] Detail page loaded after " + (2 + i) + "s — " + buttons.size() + " buttons found");
                pause(1000); // extra buffer for React rendering
                return;
            }

            // Also check if an error page appeared
            if (driver.findElements(By.xpath("//*[contains(text(),'Application Error')]")).size() > 0) {
                System.out.println("[AssetPage] Error page on detail load — attempting recovery");
                recoverFromErrorOverlay();
                return;
            }

            pause(1000);
        }

        System.out.println("[AssetPage] Detail page may not have fully loaded after 22s");
    }

    /**
     * Open the ⋮ (kebab/three-dot) menu on the asset detail page and click a menu item.
     *
     * NOTE: XPath //button[.//svg] fails on this page due to SVG namespace issues.
     * All button finding uses By.tagName("button") or By.cssSelector instead.
     */
    private void clickKebabMenuItem(String itemText) {
        // CRITICAL: Save the detail page URL before any click attempts.
        // Strategies may accidentally navigate away, so we always use this to recover.
        final String detailUrl = driver.getCurrentUrl();
        System.out.println("[AssetPage] clickKebabMenuItem('" + itemText + "') — detail URL: " + detailUrl);

        // Extract UUID from detail URL for use in Strategy 4 (direct URL)
        String uuid = "";
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("/assets/([a-f0-9-]{36})").matcher(detailUrl);
            if (m.find()) uuid = m.group(1);
        } catch (Exception ignored) {}

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
                    "  if (r.top > 0 && r.top < 200 && r.left > 200 && r.width < 60 && r.width > 15) {" +
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
                    "  if (r.top < 0 || r.top > 200 || r.left < 200) continue;" +
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
                        "  if (r.top < 0 || r.top > 200 || r.width < 5) continue;" +
                        "  result += el.tagName + '[' + (el.className||'').substring(0,40) + ']'" +
                        "    + ' aria=\"' + (el.getAttribute('aria-label')||'') + '\"'" +
                        "    + ' text=\"' + (el.textContent||'').substring(0,30).trim() + '\"'" +
                        "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')'" +
                        "    + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                        "  if (++count > 30) break;" +
                        "}" +
                        "return result;");
                System.out.println("[AssetPage] === HEADER DOM DIAGNOSTIC ===\n" + dump + "=== END DIAGNOSTIC ===");
            } catch (Exception ignored) {}
        }

        // Strategy 4: Direct URL for edit (bypass ⋮ entirely)
        if (itemText.contains("Edit") && !uuid.isEmpty()) {
            String editUrl = detailUrl.replaceAll("/assets/.*", "/assets/" + uuid + "/edit");
            System.out.println("[AssetPage] Strategy 4 — trying direct edit URL: " + editUrl);
            driver.get(editUrl);
            pause(3000);
            String newUrl = driver.getCurrentUrl();
            if (newUrl.contains("/edit") || newUrl.contains(uuid)) {
                System.out.println("[AssetPage] Direct edit URL result: " + newUrl);
                return;
            }
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


    /** Dismiss any open popup/menu by pressing Escape */
    private void dismissPopup() {
        try {
            driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
            pause(200);
        } catch (Exception ignored) {}
    }

    /**
     * Opens edit form for the first asset: grid → detail page → ⋮ → Edit Asset
     */
    public void openEditForFirstAsset() {
        navigateToFirstAssetDetail();
        clickKebabMenuItem("Edit Asset");
    }

    /**
     * Deletes the first asset: grid → detail page → ⋮ → Delete Asset
     */
    public void deleteFirstAssetFromGrid() {
        navigateToFirstAssetDetail();
        clickKebabMenuItem("Delete Asset");
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
        pause(500);

        // Strategy 1: "Save Changes" button
        if (driver.findElements(SAVE_CHANGES_BTN).size() > 0) {
            click(SAVE_CHANGES_BTN);
            return;
        }

        // Strategy 2: Any button with save/update/submit/apply/done text
        String[] saveTexts = {"Save", "Update", "Submit", "Apply", "Done", "Confirm"};
        for (String text : saveTexts) {
            By btn = By.xpath("//button[contains(.,'" + text + "')]");
            if (driver.findElements(btn).size() > 0) {
                System.out.println("[AssetPage] Clicking save button with text: " + text);
                click(btn);
                return;
            }
        }

        // Strategy 3: If we're in inline edit mode (DataGrid), press Enter to commit
        try {
            org.openqa.selenium.WebElement activeEl = driver.switchTo().activeElement();
            activeEl.sendKeys(org.openqa.selenium.Keys.ENTER);
            pause(500);
            System.out.println("[AssetPage] Pressed Enter to save inline edit");
            return;
        } catch (Exception ignored) {}

        // Diagnostic: log all visible buttons on the page
        java.util.List<WebElement> allButtons = driver.findElements(By.xpath("//button"));
        System.out.println("[AssetPage] saveChanges — no Save button found. Page has " + allButtons.size() + " buttons:");
        for (int i = 0; i < Math.min(allButtons.size(), 20); i++) {
            String btnText = "";
            try { btnText = allButtons.get(i).getText().trim().replace("\n", " "); } catch (Exception ignored) {}
            System.out.println("  [" + i + "] '" + btnText + "'");
        }
        System.out.println("[AssetPage] Current URL: " + driver.getCurrentUrl());

        throw new RuntimeException("No save/update button found on page");
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
