package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
    private static final By GRID_ROWS = By.cssSelector("[role='rowgroup'] [role='row']");

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
        // Dismiss any open drawers/modals/backdrops first — they block all clicks
        dismissOverlays();

        if (isOnConnectionsPage()) {
            System.out.println("[ConnectionPage] Already on Connections — navigating away and back");
            // Use JS click to avoid backdrop interception on sidebar links
            js.executeScript(
                "var links = document.querySelectorAll('a');" +
                "for (var el of links) {" +
                "  if (el.textContent.trim() === 'Assets') { el.click(); return; }" +
                "}"
            );
            pause(1500);
        }

        // Click Connections in sidebar via JS
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
     * Open the Source Node dropdown and select the first available option.
     */
    public void selectFirstAvailableSource() {
        selectFirstAvailableOption(SOURCE_NODE_INPUT, "Source node");
    }

    /**
     * Open the Target Node dropdown and select the first available option.
     */
    public void selectFirstAvailableTarget() {
        selectFirstAvailableOption(TARGET_NODE_INPUT, "Target node");
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
            "var rows = document.querySelectorAll(\"[role='rowgroup'] [role='row']\");" +
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
            "var rows = document.querySelectorAll(\"[role='rowgroup'] [role='row']\");" +
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

        // Install network interceptor to monitor DELETE requests
        js.executeScript(
            "window._deleteApiCalls = [];" +
            "var _origFetch = window.fetch;" +
            "window.fetch = function() {" +
            "  var url = arguments[0]; var opts = arguments[1] || {};" +
            "  var method = (opts.method || 'GET').toUpperCase();" +
            "  if (method === 'DELETE') {" +
            "    return _origFetch.apply(this, arguments).then(function(resp) {" +
            "      window._deleteApiCalls.push({url: url, status: resp.status, ok: resp.ok});" +
            "      return resp;" +
            "    }).catch(function(err) {" +
            "      window._deleteApiCalls.push({url: url, error: err.message});" +
            "      throw err;" +
            "    });" +
            "  }" +
            "  return _origFetch.apply(this, arguments);" +
            "};" +
            "// Also intercept XMLHttpRequest\n" +
            "var _origOpen = XMLHttpRequest.prototype.open;" +
            "var _origSend = XMLHttpRequest.prototype.send;" +
            "XMLHttpRequest.prototype.open = function(method, url) {" +
            "  this._deleteMethod = method; this._deleteUrl = url;" +
            "  return _origOpen.apply(this, arguments);" +
            "};" +
            "XMLHttpRequest.prototype.send = function() {" +
            "  if (this._deleteMethod && this._deleteMethod.toUpperCase() === 'DELETE') {" +
            "    var self = this;" +
            "    this.addEventListener('load', function() {" +
            "      window._deleteApiCalls.push({url: self._deleteUrl, status: self.status, xhr: true});" +
            "    });" +
            "  }" +
            "  return _origSend.apply(this, arguments);" +
            "};"
        );

        // Log available buttons in the row for debugging
        String debug = (String) js.executeScript(
            "var rows = document.querySelectorAll(\"[role='rowgroup'] [role='row']\");" +
            "if (rows.length <= arguments[0]) return 'no rows';" +
            "var row = rows[arguments[0]];" +
            "var info = 'Row ' + arguments[0] + ' buttons: ';" +
            "var btns = row.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  info += '[title=\"' + (b.getAttribute('title')||'') + '\" aria=\"' + (b.getAttribute('aria-label')||'') + '\" text=\"' + b.textContent.trim().substring(0,20) + '\"] ';" +
            "}" +
            "var actionCell = row.querySelector('[data-field=\"actions\"]');" +
            "if (actionCell) info += ' | actions cell btns: ' + actionCell.querySelectorAll('button').length;" +
            "return info;", idx);
        System.out.println("[ConnectionPage] " + debug);

        Boolean clicked = (Boolean) js.executeScript(
            "var rows = document.querySelectorAll(\"[role='rowgroup'] [role='row']\");" +
            "if (rows.length <= arguments[0]) return false;" +
            "var row = rows[arguments[0]];" +
            "// Strategy 1: title attribute (exact)\n" +
            "var btns = row.querySelectorAll('button[title=\"Delete connection\"], button[title=\"Delete\"]');" +
            "if (btns.length > 0) { btns[0].click(); return true; }" +
            "// Strategy 2: aria-label containing delete\n" +
            "btns = row.querySelectorAll('button[aria-label*=\"delete\" i], button[aria-label*=\"Delete\"]');" +
            "if (btns.length > 0) { btns[0].click(); return true; }" +
            "// Strategy 3: action cell — last button (usually delete)\n" +
            "var actionCell = row.querySelector('[data-field=\"actions\"]');" +
            "if (actionCell) {" +
            "  var allBtns = actionCell.querySelectorAll('button');" +
            "  if (allBtns.length > 0) { allBtns[allBtns.length - 1].click(); return true; }" +
            "}" +
            "// Strategy 4: any button with red/error color (delete buttons are often red)\n" +
            "btns = row.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var style = window.getComputedStyle(b);" +
            "  var color = style.color;" +
            "  if (color.includes('244') || color.includes('239') || color.includes('211')) {" +
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
        // First check for native browser confirm() dialog (like Issues uses)
        for (int a = 0; a < 3; a++) {
            try {
                org.openqa.selenium.Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                System.out.println("[ConnectionPage] Native alert found: \"" + alertText + "\"");
                alert.accept();
                System.out.println("[ConnectionPage] Native alert accepted — delete confirmed");
                return;
            } catch (org.openqa.selenium.NoAlertPresentException ignored) {
                // No native alert — fall through to MUI dialog handling
            } catch (Exception e) {
                System.out.println("[ConnectionPage] Alert check: " + e.getMessage());
            }
            pause(500);
        }

        // Wait for the confirmation dialog to appear before looking for buttons.
        // Without this, findDeleteButtonInDialog() may match a "Delete" button outside the dialog.
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
                System.out.println("[ConnectionPage] Delete confirmation dialog detected after " + (w * 500) + "ms");
                dialogAppeared = true;
                break;
            }
            pause(500);
        }
        if (!dialogAppeared) {
            System.out.println("[ConnectionPage] WARNING: No delete confirmation dialog appeared after 5s");
        }

        // Use escalating click strategies with post-click verification.
        // A single Selenium click sometimes fires but React doesn't process it.
        // Limit to 5 retries to stay within TestNG timeout (90s).
        for (int i = 0; i < 5; i++) {
            try {
                // Always nuke ALL backdrops first — they block clicks in headless Chrome CI/CD
                js.executeScript(
                    "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
                    "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
                    ");"
                );
                pause(200);

                // Find the delete button via error-styled CSS or dialog container
                WebElement deleteBtn = findDeleteButtonInDialog();
                if (deleteBtn != null) {
                    String text = deleteBtn.getText().trim();
                    // On first attempt, dump dialog diagnostic for debugging
                    if (i == 0) {
                        try {
                            String diag = (String) js.executeScript(
                                "var result = 'Dialogs: ';" +
                                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"presentation\"], [class*=\"MuiDialog\"]');" +
                                "result += dialogs.length + '\\n';" +
                                "for (var d of dialogs) {" +
                                "  var r = d.getBoundingClientRect();" +
                                "  if (r.width < 10) continue;" +
                                "  result += '  tag=' + d.tagName + ' role=\"' + (d.getAttribute('role')||'') + '\"'" +
                                "    + ' text=\"' + (d.textContent||'').trim().substring(0,100).replace(/\\n/g,' ') + '\"'" +
                                "    + ' at(' + Math.round(r.left) + ',' + Math.round(r.top) + ') ' + Math.round(r.width) + 'x' + Math.round(r.height) + '\\n';" +
                                "}" +
                                "return result;");
                            System.out.println("[ConnectionPage] " + diag);
                        } catch (Exception ignored) {}
                    }

                    if (clickButtonWithVerification(deleteBtn, text, "ConnectionPage")) {
                        return;
                    }
                    System.out.println("[ConnectionPage] Click on '" + text + "' didn't close dialog, retrying...");
                }
            } catch (Exception e) {
                System.out.println("[ConnectionPage] confirmDelete attempt " + (i+1) + ": " + e.getMessage());
            }
            pause(500);
        }
        System.out.println("[ConnectionPage] WARNING: No delete confirmation button found after 5 attempts");
    }

    /**
     * Find the Delete/Confirm button inside a visible MUI dialog.
     */
    private WebElement findDeleteButtonInDialog() {
        // Strategy 1: error-styled buttons (red Delete/Confirm)
        java.util.List<WebElement> errorBtns = driver.findElements(
            By.cssSelector("button[class*='containedError'], button[class*='error']"));
        for (WebElement btn : errorBtns) {
            try {
                if (btn.isDisplayed() && btn.isEnabled()) {
                    String text = btn.getText().trim();
                    if ("Delete".equalsIgnoreCase(text) || "Confirm".equalsIgnoreCase(text)
                            || "Yes".equalsIgnoreCase(text) || text.toLowerCase().contains("delete")) {
                        return btn;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Strategy 2: buttons inside dialog containers
        java.util.List<WebElement> dialogs = driver.findElements(
            By.cssSelector("[role='dialog'], [class*='MuiDialog-paper'], [role='alertdialog']"));
        for (WebElement dialog : dialogs) {
            java.util.List<WebElement> dBtns = dialog.findElements(By.tagName("button"));
            for (WebElement btn : dBtns) {
                try {
                    if (btn.isDisplayed() && btn.isEnabled()) {
                        String text = btn.getText().trim();
                        if ("Delete".equalsIgnoreCase(text) || "Confirm".equalsIgnoreCase(text)
                                || "Yes".equalsIgnoreCase(text) || text.toLowerCase().contains("delete")) {
                            return btn;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Strategy 3: any visible Delete button — but ONLY if inside a dialog/presentation overlay
        java.util.List<WebElement> allDeleteBtns = driver.findElements(
            By.xpath("//div[@role='dialog' or @role='presentation' or @role='alertdialog']//button[contains(.,'Delete') or contains(.,'delete')]"));
        for (WebElement btn : allDeleteBtns) {
            try {
                if (btn.isDisplayed() && btn.isEnabled()) return btn;
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Try escalating click strategies on a button, verifying the dialog closes after each.
     * Returns true if the dialog closed (delete succeeded).
     */
    private boolean clickButtonWithVerification(WebElement btn, String text, String tag) {
        // Strategy A: Selenium click
        try {
            btn.click();
            System.out.println("[" + tag + "] Selenium-clicked: '" + text + "'");
            pause(1500);
            if (isDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[" + tag + "] Selenium click failed: " + e.getMessage());
        }

        // Strategy B: Actions moveToElement + click
        try {
            new Actions(driver).moveToElement(btn).pause(java.time.Duration.ofMillis(200)).click().perform();
            System.out.println("[" + tag + "] Actions-clicked: '" + text + "'");
            pause(1500);
            if (isDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[" + tag + "] Actions click failed: " + e.getMessage());
        }

        // Strategy C: Focus button and press Enter key (keyboard events are always trusted)
        try {
            new Actions(driver).moveToElement(btn).sendKeys(org.openqa.selenium.Keys.ENTER).perform();
            System.out.println("[" + tag + "] Enter-key on: '" + text + "'");
            pause(1500);
            if (isDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[" + tag + "] Enter key failed: " + e.getMessage());
        }

        // Strategy D: JS focus + programmatic click
        try {
            js.executeScript("arguments[0].focus(); arguments[0].click();", btn);
            System.out.println("[" + tag + "] JS focus+click on: '" + text + "'");
            pause(1500);
            if (isDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[" + tag + "] JS click failed: " + e.getMessage());
        }

        // Strategy E: Direct React fiber onClick invocation — bypasses event system entirely.
        // When all UI click strategies fail, the React Portal's event delegation may be broken.
        // This reaches into React's internal fiber tree to call the handler directly.
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
            System.out.println("[" + tag + "] React fiber invoke: " + result);
            pause(2000);
            if (isDeleteDialogGone()) return true;
        } catch (Exception e) {
            System.out.println("[" + tag + "] React fiber invoke failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if the delete confirmation dialog has been dismissed.
     */
    private boolean isDeleteDialogGone() {
        try {
            // IMPORTANT: Only check role="dialog" and role="alertdialog", NOT role="presentation".
            // MUI DataGrid uses role="presentation" for structural elements, and their text content
            // includes action button labels like "Delete Connection" — which would false-positive this check.
            Boolean dialogOpen = (Boolean) js.executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"alertdialog\"], .MuiDialog-paper');" +
                "for (var d of dialogs) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 100 && r.height > 50) {" +
                "    var text = (d.textContent||'').toLowerCase();" +
                "    if (text.includes('delete') || text.includes('confirm') || text.includes('remove') || text.includes('sure')) return true;" +
                "  }" +
                "}" +
                "return false;"
            );
            return dialogOpen == null || !dialogOpen;
        } catch (Exception e) {
            return true; // If we can't check, assume it's gone
        }
    }

    /**
     * Wait for delete to complete (row count decreased or grid refreshed).
     */
    public boolean waitForDeleteSuccess() {
        // Wait for any dialog/modal to close
        for (int i = 0; i < 15; i++) {
            Boolean dialogOpen = (Boolean) js.executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [role=\"presentation\"], [class*=\"MuiDialog\"]');" +
                "for (var d of dialogs) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 100 && r.height > 100) {" +
                "    var text = (d.textContent||'').toLowerCase();" +
                "    if (text.includes('delete') || text.includes('confirm') || text.includes('remove')) return true;" +
                "  }" +
                "}" +
                "return false;"
            );
            if (dialogOpen == null || !dialogOpen) {
                System.out.println("[ConnectionPage] Delete dialog closed — success");

                // Check intercepted network calls to see if DELETE API was actually called
                String apiInfo = (String) js.executeScript(
                    "var calls = window._deleteApiCalls || [];" +
                    "if (calls.length === 0) return 'NO DELETE API CALLS DETECTED';" +
                    "var result = '';" +
                    "for (var c of calls) {" +
                    "  result += 'DELETE ' + c.url + ' → status=' + c.status + ' ok=' + c.ok;" +
                    "  if (c.error) result += ' error=' + c.error;" +
                    "  if (c.xhr) result += ' (xhr)';" +
                    "  result += ' | ';" +
                    "}" +
                    "return result;"
                );
                System.out.println("[ConnectionPage] API calls after delete: " + apiInfo);

                return true;
            }
            pause(1000);
        }
        System.out.println("[ConnectionPage] Delete dialog still open after 15s");
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
     * Uses JS for all interactions to avoid MuiBackdrop and MuiAutocomplete-noOptions interception.
     */
    private void selectAutocomplete(By inputLocator, String searchText) {
        // Dismiss any "No Options" overlay or open listbox from a previous autocomplete
        js.executeScript(
            "var noOpts = document.querySelector('.MuiAutocomplete-noOptions');" +
            "if (noOpts) noOpts.style.display = 'none';" +
            "var lb = document.querySelector('[role=\"listbox\"]');" +
            "if (lb) lb.style.display = 'none';"
        );
        pause(300);

        // Re-find element fresh each time to avoid stale references
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));

        // Use JS for scroll, focus, click — avoids backdrop/overlay interception
        // Also clear and type using JS to avoid "element not interactable"
        js.executeScript(
            "var el = arguments[0]; var text = arguments[1];" +
            "el.scrollIntoView({block:'center'});" +
            "el.focus(); el.click();" +
            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "setter.call(el, '');" +
            "el.dispatchEvent(new Event('input', {bubbles: true}));" +
            "el.dispatchEvent(new Event('change', {bubbles: true}));" +
            "setter.call(el, text);" +
            "el.dispatchEvent(new Event('input', {bubbles: true}));" +
            "el.dispatchEvent(new Event('change', {bubbles: true}));",
            input, searchText);
        pause(1000);

        // Wait for listbox and click matching option
        for (int attempt = 0; attempt < 6; attempt++) {
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
                "return false;", searchText);
            if (selected != null && selected) {
                System.out.println("[ConnectionPage] Selected autocomplete option: " + searchText);
                pause(500);
                return;
            }

            // If "No Options" appeared, clear and retry via popup indicator
            Boolean noOptions = (Boolean) js.executeScript(
                "return !!document.querySelector('.MuiAutocomplete-noOptions');");
            if (noOptions != null && noOptions) {
                System.out.println("[ConnectionPage] 'No Options' detected on attempt " + attempt + ", retrying...");
                // Re-find input to avoid stale reference after DOM changes
                input = driver.findElement(inputLocator);
                js.executeScript(
                    "var el = arguments[0];" +
                    "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                    "setter.call(el, '');" +
                    "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                    "el.dispatchEvent(new Event('change', {bubbles: true}));",
                    input);
                pause(300);
                // Click popup indicator to get full list
                js.executeScript(
                    "var wrapper = arguments[0].closest('.MuiAutocomplete-root');" +
                    "if (wrapper) {" +
                    "  var btn = wrapper.querySelector('.MuiAutocomplete-popupIndicator');" +
                    "  if (btn) btn.click();" +
                    "}",
                    input);
                pause(800);
                // Re-type
                js.executeScript(
                    "var el = arguments[0]; var text = arguments[1];" +
                    "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                    "setter.call(el, text);" +
                    "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                    "el.dispatchEvent(new Event('change', {bubbles: true}));",
                    input, searchText);
                pause(1000);
                continue;
            }

            pause(500);
        }
        System.out.println("[ConnectionPage] WARNING: Could not select autocomplete option: " + searchText);
    }

    /**
     * Open an autocomplete dropdown and select the first available option.
     * Used when we don't know which nodes are available.
     */
    private void selectFirstAvailableOption(By inputLocator, String fieldLabel) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));

        // Click to open the dropdown via JS
        js.executeScript(
            "arguments[0].scrollIntoView({block:'center'});" +
            "arguments[0].focus(); arguments[0].click();", input);
        pause(500);

        // Click the popup indicator to show all options
        js.executeScript(
            "var wrapper = arguments[0].closest('.MuiAutocomplete-root');" +
            "if (wrapper) {" +
            "  var btn = wrapper.querySelector('.MuiAutocomplete-popupIndicator');" +
            "  if (btn) btn.click();" +
            "}",
            input);
        pause(1000);

        // Select the first available option
        for (int attempt = 0; attempt < 5; attempt++) {
            String result = (String) js.executeScript(
                "var items = document.querySelectorAll('li[role=\"option\"]');" +
                "if (items.length > 0) {" +
                "  var text = items[0].textContent.trim();" +
                "  items[0].click();" +
                "  return text;" +
                "}" +
                "return null;");
            if (result != null) {
                System.out.println("[ConnectionPage] " + fieldLabel + " selected: " + result);
                pause(500);
                return;
            }
            pause(500);
        }
        System.out.println("[ConnectionPage] WARNING: No options available for " + fieldLabel);
    }

    /**
     * Get text from a specific grid cell.
     */
    private String getGridCellText(int rowIndex, String fieldName) {
        try {
            String text = (String) js.executeScript(
                "var rows = document.querySelectorAll(\"[role='rowgroup'] [role='row']\");" +
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

    /**
     * Dismiss any open drawers, modals, and backdrops that block interaction.
     */
    private void dismissOverlays() {
        // Close any open drawers via Cancel/X button
        js.executeScript(
            "// Click Cancel in any open drawer\n" +
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "for (var d of drawers) {" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    if (b.textContent.trim() === 'Cancel') { b.click(); return; }" +
            "  }" +
            "}" +
            "// Press Escape to close modals\n" +
            "document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}));"
        );
        pause(500);

        // Remove any remaining backdrops
        js.executeScript(
            "var backdrops = document.querySelectorAll('.MuiBackdrop-root, .MuiModal-backdrop');" +
            "for (var b of backdrops) { b.click(); }"
        );
        pause(500);
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
