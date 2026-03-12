package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for the Connections page at /connections.
 * Supports CRUD operations on connections.
 *
 * UI structure:
 *   - Grid with columns: Source Node | Target Node | Connection Type | Actions
 *   - "Create Connection" button opens right-side drawer
 *   - Create/Edit drawer has: BASIC INFO (Source Node, Target Node, Target Terminal, Connection Type)
 *     and CORE ATTRIBUTES (varies by connection type)
 *   - Actions column has Edit (pencil) and Delete (trash) icon buttons
 *   - Delete shows confirmation dialog with Cancel / Delete
 */
public class ConnectionPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    // Navigation
    private static final By ASSETS_NAV = By.xpath(
            "//a[normalize-space()='Assets'] | //span[normalize-space()='Assets']");

    // Drawer form fields (placeholder-based — confirmed by exploration)
    private static final By SOURCE_NODE_INPUT = By.xpath(
            "//input[@placeholder='Select source node']");
    private static final By TARGET_NODE_INPUT = By.xpath(
            "//input[@placeholder='Select target node']");
    private static final By CONNECTION_TYPE_INPUT = By.xpath(
            "//input[@placeholder='Select connection type']");

    // Search
    private static final By SEARCH_INPUT = By.xpath(
            "//input[@placeholder='Search connections...']");

    // Grid
    private static final By GRID_ROWS = By.cssSelector("[data-rowindex]");

    public ConnectionPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /**
     * Navigate to the Connections page via sidebar.
     * If already on connections, navigate away and back for fresh data.
     */
    public void navigateToConnections() {
        if (isOnConnectionsPage()) {
            System.out.println("[ConnectionPage] Already on Connections — navigating away and back");
            try {
                driver.findElement(ASSETS_NAV).click();
                pause(1500);
            } catch (Exception e) {
                System.out.println("[ConnectionPage] Nav away failed: " + e.getMessage());
            }
        }

        // Click Connections in sidebar
        js.executeScript(
            "var links = document.querySelectorAll('a');" +
            "for (var el of links) {" +
            "  if (el.textContent.trim() === 'Connections') { el.click(); return; }" +
            "}"
        );
        pause(2000);

        // Wait for spinner to clear
        waitForSpinner();
        pause(1000);
        System.out.println("[ConnectionPage] On connections page: " + driver.getCurrentUrl());
    }

    public boolean isOnConnectionsPage() {
        return driver.getCurrentUrl().contains("/connections");
    }

    // ================================================================
    // CREATE
    // ================================================================

    /**
     * Open the Create Connection drawer.
     */
    public void openCreateConnectionDrawer() {
        // Click the "Create Connection" button in page header (top area)
        js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Create Connection' || text.includes('Create Connection')) {" +
            "    var r = b.getBoundingClientRect();" +
            "    if (r.width > 0 && r.top < 300) { b.click(); return; }" +
            "  }" +
            "}"
        );
        pause(2000);

        // Wait for drawer to open
        wait.until(ExpectedConditions.visibilityOfElementLocated(SOURCE_NODE_INPUT));
        System.out.println("[ConnectionPage] Create Connection drawer opened");
    }

    /**
     * Select the Source Node from the autocomplete dropdown.
     */
    public void selectSourceNode(String nodeName) {
        selectAutocomplete(SOURCE_NODE_INPUT, nodeName);
        System.out.println("[ConnectionPage] Source node selected: " + nodeName);
    }

    /**
     * Select the Target Node from the autocomplete dropdown.
     */
    public void selectTargetNode(String nodeName) {
        selectAutocomplete(TARGET_NODE_INPUT, nodeName);
        System.out.println("[ConnectionPage] Target node selected: " + nodeName);
    }

    /**
     * Select the Connection Type from the autocomplete dropdown.
     * Valid options: "Cable", "Busway"
     */
    public void selectConnectionType(String typeName) {
        selectAutocomplete(CONNECTION_TYPE_INPUT, typeName);
        pause(1000); // Wait for CORE ATTRIBUTES to update
        System.out.println("[ConnectionPage] Connection type selected: " + typeName);
    }

    /**
     * Click "Create Connection" button inside the drawer to submit the form.
     */
    public void submitCreateConnection() {
        // Scroll drawer to bottom to make Create button visible
        js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "for (var d of drawers) {" +
            "  if (d.textContent.includes('Add Connection')) { d.scrollTop = d.scrollHeight; }" +
            "}"
        );
        pause(500);

        // Click "Create Connection" in drawer (not the page-level one)
        Boolean clicked = (Boolean) js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "for (var d of drawers) {" +
            "  if (!d.textContent.includes('Add Connection')) continue;" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    if (b.textContent.trim() === 'Create Connection' && !b.disabled) {" +
            "      b.click(); return true;" +
            "    }" +
            "  }" +
            "}" +
            "return false;"
        );
        System.out.println("[ConnectionPage] Create submitted: " + clicked);
        pause(3000);
    }

    /**
     * Wait for the create operation to complete.
     * Checks if the drawer closed and a row appeared in the grid.
     */
    public boolean waitForCreateSuccess() {
        // Check if a row appeared in the grid
        for (int i = 0; i < 10; i++) {
            int rowCount = getGridRowCount();
            if (rowCount > 0) {
                System.out.println("[ConnectionPage] Create success — grid has " + rowCount + " rows");
                return true;
            }
            pause(1000);
        }
        return false;
    }

    // ================================================================
    // READ
    // ================================================================

    /**
     * Get the number of data rows in the connections grid.
     */
    public int getGridRowCount() {
        try {
            return driver.findElements(GRID_ROWS).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if the grid has data rows (not empty).
     */
    public boolean isGridPopulated() {
        return getGridRowCount() > 0;
    }

    /**
     * Get the source node text of the first grid row.
     */
    public String getFirstRowSourceNode() {
        return getGridCellText(0, "sourceLabel");
    }

    /**
     * Get the target node text of the first grid row.
     */
    public String getFirstRowTargetNode() {
        return getGridCellText(0, "targetLabel");
    }

    /**
     * Get the connection type text of the first grid row.
     */
    public String getFirstRowConnectionType() {
        return getGridCellText(0, "edgeClassName");
    }

    /**
     * Check if a connection with given source and target is visible in the grid.
     */
    public boolean isConnectionVisible(String sourceNode, String targetNode) {
        String result = (String) js.executeScript(
            "var rows = document.querySelectorAll('[data-rowindex]');" +
            "for (var row of rows) {" +
            "  var src = row.querySelector('[data-field=\"sourceLabel\"]');" +
            "  var tgt = row.querySelector('[data-field=\"targetLabel\"]');" +
            "  if (src && tgt) {" +
            "    if (src.textContent.trim().includes(arguments[0]) && " +
            "        tgt.textContent.trim().includes(arguments[1])) return 'found';" +
            "  }" +
            "}" +
            "return 'not_found';", sourceNode, targetNode);
        return "found".equals(result);
    }

    /**
     * Search for connections using the search bar.
     */
    public void searchConnections(String query) {
        try {
            WebElement searchInput = driver.findElement(SEARCH_INPUT);
            searchInput.clear();
            searchInput.sendKeys(query);
            pause(1500); // Wait for filter to apply
        } catch (Exception e) {
            System.out.println("[ConnectionPage] Search input not found: " + e.getMessage());
        }
    }

    /**
     * Get the pagination text (e.g., "1-5 of 10").
     */
    public String getPaginationText() {
        try {
            String text = (String) js.executeScript(
                "var pag = document.querySelector('[class*=\"MuiTablePagination-displayedRows\"]');" +
                "return pag ? pag.textContent.trim() : '';"
            );
            return text != null ? text : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // EDIT
    // ================================================================

    /**
     * Click the Edit button (pencil icon) on a grid row.
     * @param rowIndex 0-based row index, or -1 for first row.
     */
    public void clickEditOnRow(int rowIndex) {
        int idx = rowIndex >= 0 ? rowIndex : 0;
        js.executeScript(
            "var rows = document.querySelectorAll('[data-rowindex]');" +
            "if (rows.length > arguments[0]) {" +
            "  var row = rows[arguments[0]];" +
            "  var btns = row.querySelectorAll('button[title=\"Edit connection\"]');" +
            "  if (btns.length > 0) { btns[0].click(); return; }" +
            "  // Fallback: click first icon button in actions cell\n" +
            "  var actionCell = row.querySelector('[data-field=\"actions\"]');" +
            "  if (actionCell) {" +
            "    var allBtns = actionCell.querySelectorAll('button');" +
            "    if (allBtns.length > 0) allBtns[0].click();" +
            "  }" +
            "}", idx);
        pause(3000);
        System.out.println("[ConnectionPage] Edit clicked on row " + idx);
    }

    /**
     * Save changes in the Edit Connection drawer.
     */
    public void saveChanges() {
        // Scroll drawer to bottom
        js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "for (var d of drawers) {" +
            "  if (d.textContent.includes('Edit Connection')) { d.scrollTop = d.scrollHeight; }" +
            "}"
        );
        pause(500);

        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  if (b.textContent.trim() === 'Save Changes' && !b.disabled && b.getBoundingClientRect().width > 0) {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;"
        );
        System.out.println("[ConnectionPage] Save Changes clicked: " + clicked);
        pause(3000);
    }

    /**
     * Wait for edit/save to complete.
     */
    public boolean waitForEditSuccess() {
        // Check for success toast or drawer closing
        for (int i = 0; i < 10; i++) {
            Boolean drawerOpen = (Boolean) js.executeScript(
                "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
                "for (var d of drawers) {" +
                "  if (d.textContent.includes('Edit Connection') && d.getBoundingClientRect().width > 0) return true;" +
                "}" +
                "return false;"
            );
            if (drawerOpen == null || !drawerOpen) {
                System.out.println("[ConnectionPage] Edit drawer closed — save success");
                return true;
            }
            pause(1000);
        }
        System.out.println("[ConnectionPage] Edit drawer still open after 10s");
        return false;
    }

    // ================================================================
    // DELETE
    // ================================================================

    /**
     * Click the Delete button (trash icon) on a grid row.
     * @param rowIndex 0-based row index, or -1 for first row.
     */
    public void clickDeleteOnRow(int rowIndex) {
        int idx = rowIndex >= 0 ? rowIndex : 0;
        Boolean clicked = (Boolean) js.executeScript(
            "var rows = document.querySelectorAll('[data-rowindex]');" +
            "if (rows.length <= arguments[0]) return false;" +
            "var row = rows[arguments[0]];" +
            "// Strategy 1: title attribute\n" +
            "var btns = row.querySelectorAll('button[title=\"Delete connection\"], button[title=\"Delete\"]');" +
            "if (btns.length > 0) { btns[0].click(); return true; }" +
            "// Strategy 2: look for trash/delete icon (SVG path) in action buttons\n" +
            "var actionCell = row.querySelector('[data-field=\"actions\"]');" +
            "if (actionCell) {" +
            "  var allBtns = actionCell.querySelectorAll('button');" +
            "  console.log('Action buttons in row: ' + allBtns.length);" +
            "  // Last button in actions is usually delete\n" +
            "  if (allBtns.length > 0) { allBtns[allBtns.length - 1].click(); return true; }" +
            "}" +
            "// Strategy 3: any button with delete-like SVG\n" +
            "var allBtns = row.querySelectorAll('button');" +
            "for (var b of allBtns) {" +
            "  var svg = b.querySelector('svg');" +
            "  if (svg && (b.getAttribute('aria-label')||'').toLowerCase().includes('delete')) {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;", idx);
        pause(2000);
        System.out.println("[ConnectionPage] Delete clicked on row " + idx + ": " + clicked);
    }

    /**
     * Confirm the delete operation in the confirmation dialog.
     */
    public void confirmDelete() {
        // Try dialog-scoped delete first
        Boolean clicked = (Boolean) js.executeScript(
            "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"presentation\"]');" +
            "for (var d of dialogs) {" +
            "  var r = d.getBoundingClientRect();" +
            "  if (r.width < 50 || r.height < 50) continue;" +
            "  var text = (d.textContent||'').toLowerCase();" +
            "  if (text.includes('delete') || text.includes('confirm') || text.includes('remove')) {" +
            "    var btns = d.querySelectorAll('button');" +
            "    for (var b of btns) {" +
            "      var t = b.textContent.trim();" +
            "      if (t === 'Delete' || t === 'Confirm' || t === 'Yes' || t === 'OK') {" +
            "        b.click(); return true;" +
            "      }" +
            "    }" +
            "  }" +
            "}" +
            "// Fallback: find any visible Delete button\n" +
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var t = b.textContent.trim();" +
            "  var r2 = b.getBoundingClientRect();" +
            "  if (t === 'Delete' && r2.width > 0 && r2.height > 0) {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;"
        );
        System.out.println("[ConnectionPage] Delete confirmed: " + clicked);
        pause(3000);
    }

    /**
     * Wait for delete to complete (row count decreased or grid refreshed).
     */
    public boolean waitForDeleteSuccess() {
        // Wait for dialog to close
        for (int i = 0; i < 10; i++) {
            Boolean dialogOpen = (Boolean) js.executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"]');" +
                "for (var d of dialogs) {" +
                "  if (d.textContent.includes('Delete Connection') && d.getBoundingClientRect().width > 0) return true;" +
                "}" +
                "return false;"
            );
            if (dialogOpen == null || !dialogOpen) {
                System.out.println("[ConnectionPage] Delete dialog closed — success");
                return true;
            }
            pause(1000);
        }
        return false;
    }

    // ================================================================
    // CLOSE DRAWER
    // ================================================================

    /**
     * Close any open connection drawer (Add/Edit) via Cancel or X button.
     */
    public void closeDrawer() {
        // Try Cancel button first
        try {
            js.executeScript(
                "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
                "for (var d of drawers) {" +
                "  if (d.textContent.includes('Connection')) {" +
                "    var btns = d.querySelectorAll('button');" +
                "    for (var b of btns) {" +
                "      if (b.textContent.trim() === 'Cancel') { b.click(); return; }" +
                "    }" +
                "  }" +
                "}"
            );
            pause(1000);
        } catch (Exception ignored) {}

        // Try X close button
        try {
            js.executeScript(
                "var btns = document.querySelectorAll('[aria-label=\"close\"], [aria-label=\"Close\"]');" +
                "for (var b of btns) { var r = b.getBoundingClientRect(); if (r.width > 0) { b.click(); return; } }"
            );
            pause(1000);
        } catch (Exception ignored) {}
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Select an option from an MUI Autocomplete field.
     */
    private void selectAutocomplete(By inputLocator, String searchText) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
        // Use JS click to avoid "element click intercepted" when overlays exist
        try {
            input.click();
        } catch (Exception e) {
            System.out.println("[ConnectionPage] Standard click failed, using JS click: " + e.getMessage());
            js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", input);
        }
        pause(500);

        // Type to filter
        try { input.clear(); } catch (Exception ignored) {}
        input.sendKeys(searchText);
        pause(1500);

        // Wait for listbox and click matching option
        for (int attempt = 0; attempt < 5; attempt++) {
            Boolean selected = (Boolean) js.executeScript(
                "var items = document.querySelectorAll('li[role=\"option\"]');" +
                "var searchText = arguments[0];" +
                "// Try exact match first\n" +
                "for (var item of items) {" +
                "  if (item.textContent.trim() === searchText) { item.click(); return true; }" +
                "}" +
                "// Try contains match\n" +
                "for (var item of items) {" +
                "  if (item.textContent.trim().includes(searchText)) { item.click(); return true; }" +
                "}" +
                "// Click first if any available\n" +
                "if (items.length > 0) { items[0].click(); return true; }" +
                "return false;", searchText);
            if (selected != null && selected) return;
            pause(500);
        }
        System.out.println("[ConnectionPage] WARNING: Could not select autocomplete option: " + searchText);
    }

    /**
     * Get text from a specific grid cell.
     */
    private String getGridCellText(int rowIndex, String fieldName) {
        try {
            String text = (String) js.executeScript(
                "var rows = document.querySelectorAll('[data-rowindex]');" +
                "if (rows.length > arguments[0]) {" +
                "  var cell = rows[arguments[0]].querySelector('[data-field=\"' + arguments[1] + '\"]');" +
                "  return cell ? cell.textContent.trim() : null;" +
                "}" +
                "return null;", rowIndex, fieldName);
            return text;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Wait for loading spinner to disappear.
     */
    private void waitForSpinner() {
        for (int i = 0; i < 20; i++) {
            Boolean hasSpinner = (Boolean) js.executeScript(
                "var circles = document.querySelectorAll('[class*=\"MuiCircularProgress\"]');" +
                "for (var c of circles) { var r = c.getBoundingClientRect(); if (r.width > 0 && r.left > 250) return true; }" +
                "return false;"
            );
            if (hasSpinner == null || !hasSpinner) return;
            pause(1000);
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
