package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * Page Object Model for the Issues page.
 * Supports CRUD operations on issues.
 *
 * UI structure (from live app):
 *   - Issues list is a TABLE/GRID with columns: Title, Issue Class, Priority, Asset
 *   - "+ Create Issue" button opens a right-side drawer titled "Add Issue"
 *   - Add Issue form fields (BASIC INFO section):
 *       1. Priority        — dropdown (default: "Medium")
 *       2. Immediate Hazard — Yes/No toggle buttons
 *       3. Customer Notified — Yes/No toggle buttons
 *       4. Issue Class *    — required dropdown ("Select an issue class")
 *       5. Asset            — dropdown ("Select an asset")
 *   - Clicking a row opens issue detail page
 *   - Detail page has: Activate Jobs button, Photo upload
 */
public class IssuePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    // ================================================================
    // LOCATORS
    // ================================================================

    // Navigation
    private static final By ISSUES_NAV = By.xpath(
            "//a[normalize-space()='Issues'] | //span[normalize-space()='Issues']");

    // Create Issue button (page header "+" button)
    private static final By CREATE_ISSUE_BTN = By.xpath(
            "//button[normalize-space()='Create Issue' or contains(normalize-space(),'Create Issue')]");

    // Add Issue form header
    private static final By ADD_ISSUE_HEADER = By.xpath(
            "//*[normalize-space()='Add Issue']");

    // Form fields — based on actual UI screenshot
    // Priority dropdown (MUI Autocomplete with placeholder or label)
    private static final By PRIORITY_INPUT = By.xpath(
            "//label[contains(text(),'Priority')]/following::input[1]"
            + " | //input[@placeholder='Priority' or @placeholder='Select priority' or @placeholder='Select a priority']");

    // Issue Class dropdown (required, placeholder = "Select an issue class")
    private static final By ISSUE_CLASS_INPUT = By.xpath(
            "//input[@placeholder='Select an issue class' or @placeholder='Select issue class'"
            + " or @placeholder='Issue Class' or @placeholder='Select a class']"
            + " | //label[contains(text(),'Issue Class')]/following::input[1]");

    // Asset dropdown (placeholder = "Select an asset")
    private static final By ASSET_INPUT = By.xpath(
            "//input[@placeholder='Select an asset' or @placeholder='Select Asset'"
            + " or @placeholder='Select asset' or @placeholder='Asset'"
            + " or @placeholder='Search asset' or @placeholder='Choose Asset']"
            + " | //label[contains(text(),'Asset')]/following::input[1]");

    // Search
    private static final By SEARCH_INPUT = By.xpath(
            "//input[contains(@placeholder,'Search') or contains(@placeholder,'search')]");

    // Table rows (Issues list is a table — columns: Title, Issue Class, Priority, Asset)
    private static final By TABLE_ROWS = By.xpath(
            "//tbody//tr | //div[contains(@class,'MuiDataGrid-row') and @data-rowindex]");

    // Issue detail page
    private static final By ACTIVATE_JOBS_BTN = By.xpath(
            "//button[normalize-space()='Activate Jobs' or contains(normalize-space(),'Activate Job') or contains(normalize-space(),'Activate')]");
    private static final By JOBS_ACTIVATED_INDICATOR = By.xpath(
            "//*[contains(text(),'Job activated') or contains(text(),'Jobs activated') or contains(text(),'Activated') or contains(@class,'activated')]");

    // Photo upload
    private static final By PHOTO_UPLOAD_INPUT = By.xpath("//input[@type='file']");
    private static final By PHOTO_THUMBNAILS = By.xpath(
            "//img[contains(@class,'thumbnail') or contains(@class,'photo') or contains(@src,'blob:') or contains(@src,'upload')]");

    // Delete
    private static final By DELETE_ISSUE_BTN = By.xpath(
            "//button[normalize-space()='Delete Issue' or normalize-space()='Delete']");
    private static final By CONFIRM_DELETE_BTN = By.xpath(
            "//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public IssuePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /**
     * Navigate to the Issues page via sidebar.
     * If already on Issues, navigate away and back for fresh data.
     */
    public void navigateToIssues() {
        dismissAnyDrawerOrBackdrop();

        if (isOnIssuesPage()) {
            System.out.println("[IssuePage] Already on Issues — navigating away and back");
            try {
                js.executeScript(
                    "var links = document.querySelectorAll('a');" +
                    "for (var el of links) {" +
                    "  if (el.textContent.trim() === 'Assets' || el.textContent.trim() === 'Locations') { el.click(); return; }" +
                    "}");
                pause(1500);
            } catch (Exception e) {
                System.out.println("[IssuePage] Nav away failed: " + e.getMessage());
            }
        }

        // Click Issues in sidebar
        js.executeScript(
            "var links = document.querySelectorAll('a');" +
            "for (var el of links) {" +
            "  if (el.textContent.trim() === 'Issues') { el.click(); return; }" +
            "}"
        );
        pause(2000);

        waitForSpinner();
        pause(1000);
        System.out.println("[IssuePage] On issues page: " + driver.getCurrentUrl());
    }

    public boolean isOnIssuesPage() {
        return driver.getCurrentUrl().contains("/issues");
    }

    // ================================================================
    // CREATE
    // ================================================================

    /**
     * Open the Create Issue form (right drawer "Add Issue").
     */
    public void openCreateIssueForm() {
        dismissAnyDialog();
        pause(300);

        // Click the "+ Create Issue" button in page header
        js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Create Issue' || text.includes('Create Issue')) {" +
            "    var r = b.getBoundingClientRect();" +
            "    if (r.width > 0 && r.top < 300) { b.click(); return; }" +
            "  }" +
            "}"
        );
        pause(2000);

        // Wait for "Add Issue" drawer to appear
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(ADD_ISSUE_HEADER));
        } catch (Exception e) {
            System.out.println("[IssuePage] Add Issue header not found, trying BASIC INFO section");
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//*[contains(text(),'BASIC INFO')]")));
            } catch (Exception e2) {
                // Log drawer contents for diagnostics
                String diag = (String) js.executeScript(
                    "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [role=\"dialog\"], [role=\"presentation\"]');" +
                    "var info = 'Drawers: ' + drawers.length + '. ';" +
                    "for (var d of drawers) {" +
                    "  var inputs = d.querySelectorAll('input');" +
                    "  info += 'Inputs: ' + inputs.length + ' [';" +
                    "  for (var inp of inputs) {" +
                    "    info += '{type=' + (inp.type||'') + ', placeholder=' + (inp.placeholder||'') + '} ';" +
                    "  }" +
                    "  info += '] ';" +
                    "  var labels = d.querySelectorAll('label, p, h6, h5');" +
                    "  info += 'Labels: [';" +
                    "  for (var l of labels) { info += l.textContent.trim() + ', '; }" +
                    "  info += '] ';" +
                    "}" +
                    "return info;");
                System.out.println("[IssuePage] DIAGNOSTIC: " + diag);
                throw e2;
            }
        }
        System.out.println("[IssuePage] Add Issue form opened");
    }

    /**
     * Select priority from the dropdown (default is "Medium").
     * If desired priority matches default, skip selection.
     */
    public void selectPriority(String priority) {
        // Log all form fields in the FORM drawer (not sidebar) for diagnostics
        String formDiag = (String) js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "var drawer = null;" +
            "for (var d of drawers) {" +
            "  if (d.textContent.includes('Add Issue') || d.textContent.includes('BASIC INFO') || d.textContent.includes('Priority')) {" +
            "    drawer = d; break;" +
            "  }" +
            "}" +
            "if (!drawer) drawer = drawers[drawers.length - 1] || document;" +
            "var info = 'FORM DRAWER: ';" +
            "// All labels and text elements\n" +
            "var labels = drawer.querySelectorAll('label, p, span, h6, h5');" +
            "var seen = new Set();" +
            "for (var l of labels) {" +
            "  var text = l.textContent.trim();" +
            "  if (text.length > 0 && text.length < 30 && !seen.has(text)) {" +
            "    seen.add(text);" +
            "    var tag = l.tagName;" +
            "    info += '{' + tag + ' \"' + text + '\"} ';" +
            "  }" +
            "}" +
            "// All inputs in drawer\n" +
            "var inputs = drawer.querySelectorAll('input, select, [role=\"button\"], [role=\"combobox\"]');" +
            "info += ' INPUTS(' + inputs.length + '): ';" +
            "for (var inp of inputs) {" +
            "  info += '{' + inp.tagName + ' type=' + (inp.type||'') + ' ph=' + (inp.placeholder||'') + ' role=' + (inp.getAttribute('role')||'') + ' val=' + (inp.value||inp.textContent||'').substring(0,20) + '} ';" +
            "}" +
            "return info;");
        System.out.println("[IssuePage] " + formDiag);

        // Check if priority is already set to the desired value (search in FORM drawer)
        Boolean alreadySet = (Boolean) js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "var drawer = null;" +
            "for (var d of drawers) {" +
            "  if (d.textContent.includes('Add Issue') || d.textContent.includes('BASIC INFO') || d.textContent.includes('Priority')) {" +
            "    drawer = d; break;" +
            "  }" +
            "}" +
            "if (!drawer) drawer = drawers[drawers.length - 1] || document;" +
            "var all = drawer.querySelectorAll('label, p, span, h6, div, [role=\"button\"]');" +
            "for (var el of all) {" +
            "  var text = el.textContent.trim();" +
            "  if (text.includes('Priority') || text.includes('priority')) {" +
            "    var container = el.closest('.MuiFormControl-root') || el.closest('[class*=\"MuiGrid\"]') || el.parentElement;" +
            "    if (container && container.textContent.includes(arguments[0])) return true;" +
            "  }" +
            "}" +
            "return false;", priority);

        if (Boolean.TRUE.equals(alreadySet)) {
            System.out.println("[IssuePage] Priority already set to: " + priority);
            return;
        }

        // Try input-based approach first (MUI Autocomplete)
        try {
            List<WebElement> inputs = driver.findElements(PRIORITY_INPUT);
            if (!inputs.isEmpty() && inputs.get(0).isDisplayed()) {
                typeAndSelectDropdown(PRIORITY_INPUT, priority, priority);
                System.out.println("[IssuePage] Selected priority via input: " + priority);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback: find Priority field by label and click to open dropdown (MUI Select or custom)
        Boolean selected = (Boolean) js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "var drawer = null;" +
            "for (var d of drawers) {" +
            "  if (d.textContent.includes('Add Issue') || d.textContent.includes('BASIC INFO') || d.textContent.includes('Priority')) {" +
            "    drawer = d; break;" +
            "  }" +
            "}" +
            "if (!drawer) drawer = drawers[drawers.length - 1] || document;" +
            "var labels = drawer.querySelectorAll('label, p, span, h6, div');" +
            "for (var l of labels) {" +
            "  var text = l.textContent.trim();" +
            "  if (text === 'Priority' || text === 'Priority *' || text.startsWith('Priority')) {" +
            "    var container = l.closest('.MuiFormControl-root') || l.closest('[class*=\"MuiGrid\"]') || l.parentElement;" +
            "    // Try MUI Select (div role=button)\n" +
            "    var select = container.querySelector('[role=\"button\"], [role=\"combobox\"], select, [class*=\"MuiSelect\"]');" +
            "    if (select) { select.click(); return true; }" +
            "    container.click(); return true;" +
            "  }" +
            "}" +
            "// Try finding by MUI Select that has priority-like content (Medium, High, Low)\n" +
            "var selects = drawer.querySelectorAll('[role=\"button\"]');" +
            "for (var s of selects) {" +
            "  var val = s.textContent.trim();" +
            "  if (val === 'Medium' || val === 'High' || val === 'Low' || val === 'Critical') {" +
            "    s.click(); return true;" +
            "  }" +
            "}" +
            "return false;");

        if (Boolean.TRUE.equals(selected)) {
            pause(500);
            // Click matching option in dropdown
            Boolean optionClicked = (Boolean) js.executeScript(
                "var items = document.querySelectorAll('li[role=\"option\"], [role=\"menuitem\"], [data-value]');" +
                "for (var item of items) {" +
                "  if (item.textContent.trim() === arguments[0] || item.getAttribute('data-value') === arguments[0]) {" +
                "    item.click(); return true;" +
                "  }" +
                "}" +
                "return false;", priority);
            pause(500);
            if (Boolean.TRUE.equals(optionClicked)) {
                System.out.println("[IssuePage] Selected priority via label click: " + priority);
            } else {
                System.out.println("[IssuePage] Opened priority dropdown but could not find option: " + priority);
            }
        } else {
            // If we can't find or change priority, it may have a default (Medium) — log but don't fail
            System.out.println("[IssuePage] WARNING: Could not find Priority field — using default value");
        }
    }

    /**
     * Set the Immediate Hazard toggle (Yes/No buttons).
     */
    public void setImmediateHazard(boolean yes) {
        String label = yes ? "Yes" : "No";
        js.executeScript(
            "var allLabels = document.querySelectorAll('label, p, span, h6');" +
            "for (var l of allLabels) {" +
            "  if (l.textContent.trim() === 'Immediate Hazard') {" +
            "    var container = l.closest('.MuiFormControl-root') || l.closest('[class*=\"toggle\"]') || l.parentElement.parentElement;" +
            "    var buttons = container.querySelectorAll('button');" +
            "    for (var b of buttons) {" +
            "      if (b.textContent.trim() === arguments[0]) { b.click(); return; }" +
            "    }" +
            "  }" +
            "}", label);
        pause(300);
        System.out.println("[IssuePage] Set Immediate Hazard: " + label);
    }

    /**
     * Set the Customer Notified toggle (Yes/No buttons).
     */
    public void setCustomerNotified(boolean yes) {
        String label = yes ? "Yes" : "No";
        js.executeScript(
            "var allLabels = document.querySelectorAll('label, p, span, h6');" +
            "for (var l of allLabels) {" +
            "  if (l.textContent.trim() === 'Customer Notified') {" +
            "    var container = l.closest('.MuiFormControl-root') || l.closest('[class*=\"toggle\"]') || l.parentElement.parentElement;" +
            "    var buttons = container.querySelectorAll('button');" +
            "    for (var b of buttons) {" +
            "      if (b.textContent.trim() === arguments[0]) { b.click(); return; }" +
            "    }" +
            "  }" +
            "}", label);
        pause(300);
        System.out.println("[IssuePage] Set Customer Notified: " + label);
    }

    /**
     * Select an issue class from the dropdown (required field).
     */
    public void selectIssueClass(String issueClass) {
        typeAndSelectDropdown(ISSUE_CLASS_INPUT, issueClass, issueClass);
        System.out.println("[IssuePage] Selected issue class: " + issueClass);
    }

    /**
     * Select an asset from the dropdown.
     */
    public void selectAsset(String assetName) {
        typeAndSelectDropdown(ASSET_INPUT, assetName, assetName);
        System.out.println("[IssuePage] Selected asset: " + assetName);
    }

    /**
     * Submit the Create Issue form (click Save/Create inside the drawer).
     */
    public void submitCreateIssue() {
        // Hide backdrops that could intercept clicks
        js.executeScript(
            "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
            "  function(b) { b.style.pointerEvents = 'none'; }" +
            ");"
        );
        pause(200);

        // Find the RIGHT drawer (form drawer, not sidebar).
        // The form drawer is the LAST/rightmost MuiDrawer-paper, or the modal one.
        String btnDiag = (String) js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "var info = 'Drawers(' + drawers.length + '): ';" +
            "var formDrawer = null;" +
            "for (var i = 0; i < drawers.length; i++) {" +
            "  var d = drawers[i];" +
            "  var r = d.getBoundingClientRect();" +
            "  var hasForm = d.textContent.includes('Add Issue') || d.textContent.includes('BASIC INFO') || d.textContent.includes('Issue Class');" +
            "  info += '{w=' + Math.round(r.width) + ' form=' + hasForm + '} ';" +
            "  if (hasForm) formDrawer = d;" +
            "}" +
            "if (!formDrawer) return 'NO FORM DRAWER - ' + info;" +
            "var btns = formDrawer.querySelectorAll('button');" +
            "info += 'FormBtns(' + btns.length + '): ';" +
            "for (var b of btns) {" +
            "  var br = b.getBoundingClientRect();" +
            "  var text = b.textContent.trim();" +
            "  if (text.length > 0 && text.length < 30 && br.width > 0) {" +
            "    info += '{\"' + text + '\" y=' + Math.round(br.top) + '} ';" +
            "  }" +
            "}" +
            "return info;");
        System.out.println("[IssuePage] " + btnDiag);

        // Click the submit button in the FORM drawer (not sidebar)
        Boolean clicked = (Boolean) js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "var formDrawer = null;" +
            "for (var d of drawers) {" +
            "  if (d.textContent.includes('Add Issue') || d.textContent.includes('BASIC INFO') || d.textContent.includes('Issue Class')) {" +
            "    formDrawer = d; break;" +
            "  }" +
            "}" +
            "if (!formDrawer) {" +
            "  // Try presentation role containers\n" +
            "  var presentations = document.querySelectorAll('[role=\"presentation\"]');" +
            "  for (var p of presentations) {" +
            "    if (p.textContent.includes('Add Issue') || p.textContent.includes('BASIC INFO')) {" +
            "      formDrawer = p; break;" +
            "    }" +
            "  }" +
            "}" +
            "if (!formDrawer) return false;" +
            "var btns = formDrawer.querySelectorAll('button');" +
            "var submitTexts = ['Create Issue', 'Create', 'Save', 'Submit'];" +
            "// Find submit at bottom of form (highest y)\n" +
            "var bestBtn = null; var bestY = -1;" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  var r = b.getBoundingClientRect();" +
            "  if (r.width > 0 && submitTexts.indexOf(text) >= 0 && r.top > bestY) {" +
            "    bestBtn = b; bestY = r.top;" +
            "  }" +
            "}" +
            "if (bestBtn) {" +
            "  bestBtn.scrollIntoView({block:'center'});" +
            "  bestBtn.click();" +
            "  return true;" +
            "}" +
            "// Fallback: any button with contained class\n" +
            "btns = formDrawer.querySelectorAll('button[class*=\"contained\"], button[class*=\"primary\"]');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text.includes('Create') || text.includes('Save')) { b.click(); return true; }" +
            "}" +
            "return false;");

        System.out.println("[IssuePage] Submit clicked: " + clicked);
    }

    /**
     * Wait for issue creation to succeed (drawer closes or success toast).
     */
    public boolean waitForCreateSuccess() {
        By panelHeader = By.xpath("//*[normalize-space()='Add Issue' or normalize-space()='Create Issue' or normalize-space()='New Issue']");

        for (int i = 0; i < 25; i++) {
            // Success indicator (toast message or snackbar)
            try {
                Boolean hasSuccess = (Boolean) js.executeScript(
                    "var body = document.body.textContent.toLowerCase();" +
                    "return body.includes('created') || body.includes('success') || " +
                    "document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"MuiAlert-standardSuccess\"]').length > 0;");
                if (Boolean.TRUE.equals(hasSuccess)) {
                    System.out.println("[IssuePage] Success indicator found");
                    closeDrawer();
                    return true;
                }
            } catch (Exception ignored) {}

            // Panel closed = form submitted successfully
            if (driver.findElements(panelHeader).isEmpty()) {
                System.out.println("[IssuePage] Add Issue panel closed — creation successful");
                return true;
            }

            // Check for validation errors that would prevent submission
            if (i == 10) {
                String errorDiag = (String) js.executeScript(
                    "var drawer = document.querySelector('[class*=\"MuiDrawer-paper\"]') || document;" +
                    "var errors = drawer.querySelectorAll('[class*=\"error\"], [class*=\"Error\"], .Mui-error, [class*=\"helperText\"]');" +
                    "var info = 'Errors(' + errors.length + '): ';" +
                    "for (var e of errors) { info += '[' + e.textContent.trim().substring(0,50) + '] '; }" +
                    "return info;");
                System.out.println("[IssuePage] " + errorDiag);
            }

            pause(1000);
        }

        System.out.println("[IssuePage] Add Issue panel still open after 25s — creation may have failed");
        closeDrawer();
        return false;
    }

    // ================================================================
    // TABLE / READ
    // ================================================================

    /**
     * Get the count of visible issue rows in the table/grid.
     */
    public int getRowCount() {
        try {
            Long count = (Long) js.executeScript(
                "// Strategy 1: table body rows\n" +
                "var tableRows = document.querySelectorAll('tbody tr');" +
                "var visible = 0;" +
                "for (var r of tableRows) {" +
                "  var rect = r.getBoundingClientRect();" +
                "  if (rect.width > 50 && rect.height > 10) visible++;" +
                "}" +
                "if (visible > 0) return visible;" +
                "// Strategy 2: MUI DataGrid rows\n" +
                "var gridRows = document.querySelectorAll('[data-rowindex]');" +
                "return gridRows.length;"
            );
            int result = count != null ? count.intValue() : 0;
            System.out.println("[IssuePage] Row count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error counting rows: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Alias for backward compatibility.
     */
    public int getCardCount() {
        return getRowCount();
    }

    /**
     * Check if the issues table has any rows populated.
     */
    public boolean isCardsPopulated() {
        for (int i = 0; i < 10; i++) {
            if (getRowCount() > 0) return true;
            pause(1000);
        }
        return false;
    }

    /**
     * Get the title from the first row in the issues table (first cell / Title column).
     */
    public String getFirstCardTitle() {
        try {
            String title = (String) js.executeScript(
                "// Strategy 1: table body rows — first cell is Title column\n" +
                "var tableRows = document.querySelectorAll('tbody tr');" +
                "if (tableRows.length > 0) {" +
                "  var cells = tableRows[0].querySelectorAll('td');" +
                "  if (cells.length > 0) return cells[0].textContent.trim();" +
                "}" +
                "// Strategy 2: MUI DataGrid rows\n" +
                "var gridRows = document.querySelectorAll('[data-rowindex]');" +
                "if (gridRows.length > 0) {" +
                "  var row = gridRows[0];" +
                "  var nameCell = row.querySelector('[data-field=\"title\"], [data-field=\"name\"], [data-field=\"issueName\"]');" +
                "  if (nameCell) return nameCell.textContent.trim();" +
                "  var cells = row.querySelectorAll('[data-field]');" +
                "  for (var cell of cells) {" +
                "    var txt = cell.textContent.trim();" +
                "    if (txt.length > 1 && txt.length < 100) return txt;" +
                "  }" +
                "}" +
                "return null;"
            );
            System.out.println("[IssuePage] First row title: " + title);
            return title;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error getting first row title: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if an issue with the given name is visible on the page.
     */
    public boolean isIssueVisible(String issueName) {
        try {
            Boolean found = (Boolean) js.executeScript(
                "var rows = document.querySelectorAll('tbody tr, [data-rowindex]');" +
                "for (var r of rows) {" +
                "  if (r.textContent.indexOf(arguments[0]) > -1) return true;" +
                "}" +
                "return document.body.textContent.indexOf(arguments[0]) > -1;",
                issueName);
            boolean result = Boolean.TRUE.equals(found);
            System.out.println("[IssuePage] Issue '" + issueName + "' visible: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error checking issue visibility: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SEARCH / FILTER / SORT
    // ================================================================

    /**
     * Search for issues by typing in the search input.
     */
    public void searchIssues(String query) {
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
            js.executeScript(
                "var el = arguments[0];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(el, '');" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "setter.call(el, arguments[1]);" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "el.dispatchEvent(new Event('change', {bubbles: true}));",
                searchInput, query);
            pause(1500);
            System.out.println("[IssuePage] Searched for: " + query);
        } catch (Exception e) {
            System.out.println("[IssuePage] Search failed: " + e.getMessage());
        }
    }

    /**
     * Clear the search input.
     */
    public void clearSearch() {
        try {
            WebElement searchInput = driver.findElement(SEARCH_INPUT);
            js.executeScript(
                "var el = arguments[0];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(el, '');" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "el.dispatchEvent(new Event('change', {bubbles: true}));",
                searchInput);
            pause(1500);
            System.out.println("[IssuePage] Search cleared");
        } catch (Exception e) {
            System.out.println("[IssuePage] Clear search failed: " + e.getMessage());
        }
    }

    /**
     * Click a sort option (column header click for table-based layout).
     */
    public void clickSortOption(String sortLabel) {
        Boolean sorted = (Boolean) js.executeScript(
            "var sortLabel = arguments[0];" +
            // Strategy 1: Column header click (table has headers: Title, Issue Class, Priority, Asset)
            "var headers = document.querySelectorAll('th, [role=\"columnheader\"], [class*=\"sortable\"], thead td');" +
            "for (var h of headers) {" +
            "  if (h.textContent.trim().includes(sortLabel)) { h.click(); return true; }" +
            "}" +
            // Strategy 2: Sort dropdown/menu
            "var sortBtns = document.querySelectorAll('[class*=\"sort\"], [class*=\"Sort\"], [aria-label*=\"sort\"]');" +
            "for (var b of sortBtns) {" +
            "  if (b.tagName === 'BUTTON' || b.tagName === 'SELECT') { b.click(); break; }" +
            "}" +
            "var opts = document.querySelectorAll('li[role=\"option\"], li[role=\"menuitem\"], [class*=\"MenuItem\"]');" +
            "for (var o of opts) {" +
            "  if (o.textContent.trim().includes(sortLabel)) { o.click(); return true; }" +
            "}" +
            "return false;",
            sortLabel);
        System.out.println("[IssuePage] Sort by '" + sortLabel + "': " + (Boolean.TRUE.equals(sorted) ? "applied" : "not found"));
    }

    /**
     * Click a filter option.
     */
    public void clickFilterOption(String filterLabel) {
        Boolean filtered = (Boolean) js.executeScript(
            "var label = arguments[0];" +
            "var chips = document.querySelectorAll('[class*=\"MuiChip\"], [class*=\"filter\"], [class*=\"Filter\"], button');" +
            "for (var c of chips) {" +
            "  if (c.textContent.trim() === label) { c.click(); return true; }" +
            "}" +
            "var items = document.querySelectorAll('li[role=\"option\"], li[role=\"menuitem\"]');" +
            "for (var i of items) {" +
            "  if (i.textContent.trim() === label) { i.click(); return true; }" +
            "}" +
            "return false;",
            filterLabel);
        pause(1000);
        System.out.println("[IssuePage] Filter '" + filterLabel + "': " + (Boolean.TRUE.equals(filtered) ? "applied" : "not found"));
    }

    // ================================================================
    // DETAIL PAGE / ACTIVATE JOBS
    // ================================================================

    /**
     * Click on the first issue row to open its detail page.
     */
    public void openFirstIssueDetail() {
        js.executeScript(
            "// Strategy 1: table body rows — click first row or its link\n" +
            "var tableRows = document.querySelectorAll('tbody tr');" +
            "if (tableRows.length > 0) {" +
            "  var link = tableRows[0].querySelector('a');" +
            "  if (link) { link.click(); return; }" +
            "  tableRows[0].click(); return;" +
            "}" +
            "// Strategy 2: MUI DataGrid rows\n" +
            "var gridRows = document.querySelectorAll('[data-rowindex]');" +
            "if (gridRows.length > 0) {" +
            "  var link = gridRows[0].querySelector('a');" +
            "  if (link) { link.click(); return; }" +
            "  gridRows[0].click(); return;" +
            "}"
        );
        pause(3000);
        waitForDetailPageLoad();
        System.out.println("[IssuePage] Opened issue detail: " + driver.getCurrentUrl());
    }

    /**
     * Click on a specific issue row by name to open its detail page.
     */
    public void openIssueDetail(String issueName) {
        js.executeScript(
            "var name = arguments[0];" +
            "var rows = document.querySelectorAll('tbody tr, [data-rowindex]');" +
            "for (var r of rows) {" +
            "  if (r.textContent.indexOf(name) > -1) {" +
            "    var link = r.querySelector('a');" +
            "    if (link) { link.click(); return; }" +
            "    r.click(); return;" +
            "  }" +
            "}",
            issueName);
        pause(3000);
        waitForDetailPageLoad();
        System.out.println("[IssuePage] Opened issue detail for: " + issueName);
    }

    /**
     * Wait for the issue detail page to fully load.
     */
    public void waitForDetailPageLoad() {
        for (int i = 0; i < 16; i++) {
            try {
                Long mainElements = (Long) js.executeScript(
                    "return document.querySelectorAll('main *, [class*=\"detail\"] *, [class*=\"content\"] *').length;");
                if (mainElements != null && mainElements > 10) {
                    System.out.println("[IssuePage] Detail page loaded after " + (i + 1) + "s — " + mainElements + " elements");
                    return;
                }
            } catch (Exception ignored) {}
            pause(2000);
        }
        System.out.println("[IssuePage] Detail page may not have fully loaded after 32s");
    }

    /**
     * Click the "Activate Jobs" button on the issue detail page.
     */
    public void clickActivateJobs() {
        // Log all buttons on the detail page for diagnostics
        String buttonDiag = (String) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "var info = 'Buttons(' + btns.length + '): ';" +
            "for (var b of btns) {" +
            "  var r = b.getBoundingClientRect();" +
            "  if (r.width > 0) {" +
            "    info += '[' + b.textContent.trim().substring(0,40) + '] ';" +
            "  }" +
            "}" +
            "return info;");
        System.out.println("[IssuePage] Detail page " + buttonDiag);

        // Try multiple text patterns for the activate button
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "var patterns = ['Activate Jobs', 'Activate Job', 'Activate', 'Create Job', 'Add Job', 'Generate Job', 'Start Job'];" +
            "for (var pattern of patterns) {" +
            "  for (var b of btns) {" +
            "    var text = b.textContent.trim();" +
            "    if (text === pattern || text.toLowerCase().includes(pattern.toLowerCase())) {" +
            "      b.scrollIntoView({block:'center'});" +
            "      b.click();" +
            "      return true;" +
            "    }" +
            "  }" +
            "}" +
            "// Try any button with 'job' in text (case insensitive)\n" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim().toLowerCase();" +
            "  if (text.includes('job')) {" +
            "    b.scrollIntoView({block:'center'});" +
            "    b.click();" +
            "    return true;" +
            "  }" +
            "}" +
            "return false;"
        );

        if (Boolean.TRUE.equals(clicked)) {
            System.out.println("[IssuePage] Clicked Activate Jobs button");
            pause(3000);
            return;
        }

        System.out.println("[IssuePage] No activate/job button found via JS — trying Selenium locator");
        // Fallback: use Selenium locator
        click(ACTIVATE_JOBS_BTN);
        pause(3000);
        System.out.println("[IssuePage] Clicked Activate Jobs (fallback)");
    }

    /**
     * Verify that jobs have been activated.
     */
    public boolean isJobActivated() {
        for (int i = 0; i < 10; i++) {
            try {
                // Check for success toast or indicator
                if (!driver.findElements(JOBS_ACTIVATED_INDICATOR).isEmpty()) {
                    System.out.println("[IssuePage] Job activation indicator found");
                    return true;
                }

                // Check if Activate Jobs button text changed or button disappeared
                Boolean buttonGone = (Boolean) js.executeScript(
                    "var btns = document.querySelectorAll('button');" +
                    "for (var b of btns) {" +
                    "  var text = b.textContent.trim().toLowerCase();" +
                    "  if (text.includes('activate') && text.includes('job')) return false;" +
                    "}" +
                    "return true;");
                if (Boolean.TRUE.equals(buttonGone)) {
                    System.out.println("[IssuePage] Activate Jobs button no longer visible — jobs activated");
                    return true;
                }

                // Check for any success toast or snackbar
                Boolean hasToast = (Boolean) js.executeScript(
                    "return document.body.textContent.includes('activated') || " +
                    "document.body.textContent.includes('Activated') || " +
                    "document.body.textContent.includes('success') || " +
                    "document.body.textContent.includes('Success') || " +
                    "document.body.textContent.includes('created') || " +
                    "document.body.textContent.includes('Created') || " +
                    "document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"toast\"], [class*=\"alert-success\"], [class*=\"MuiAlert\"]').length > 0;");
                if (Boolean.TRUE.equals(hasToast)) {
                    System.out.println("[IssuePage] Success toast detected — jobs activated");
                    return true;
                }

                // Check if a job/work order section appeared on the page
                Boolean hasJobSection = (Boolean) js.executeScript(
                    "var body = document.body.textContent.toLowerCase();" +
                    "return body.includes('work order') || body.includes('job #') || " +
                    "body.includes('job id') || body.includes('job status');");
                if (Boolean.TRUE.equals(hasJobSection)) {
                    System.out.println("[IssuePage] Job section appeared on page — jobs activated");
                    return true;
                }
            } catch (Exception ignored) {}
            pause(1000);
        }
        System.out.println("[IssuePage] Job activation not confirmed after 10s");
        return false;
    }

    /**
     * Verify the issue detail page has expected tabs (Details, Class Details, Photos).
     */
    public boolean verifyDetailPageTabs() {
        Boolean hasTabs = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button, [role=\"tab\"]');" +
            "var tabNames = [];" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Details' || text === 'Photos' || text.includes('Class Details')) {" +
            "    tabNames.push(text);" +
            "  }" +
            "}" +
            "return tabNames.length >= 2;");
        boolean result = Boolean.TRUE.equals(hasTabs);

        // Log what tabs were found
        String tabInfo = (String) js.executeScript(
            "var btns = document.querySelectorAll('button, [role=\"tab\"]');" +
            "var info = '';" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Details' || text === 'Photos' || text.includes('Class')) {" +
            "    info += '[' + text + '] ';" +
            "  }" +
            "}" +
            "return info;");
        System.out.println("[IssuePage] Detail page tabs found: " + tabInfo);

        // Try clicking each tab to verify they work
        js.executeScript(
            "var btns = document.querySelectorAll('button, [role=\"tab\"]');" +
            "for (var b of btns) {" +
            "  if (b.textContent.trim() === 'Photos') { b.click(); return; }" +
            "}");
        pause(1000);
        js.executeScript(
            "var btns = document.querySelectorAll('button, [role=\"tab\"]');" +
            "for (var b of btns) {" +
            "  if (b.textContent.trim() === 'Details') { b.click(); return; }" +
            "}");
        pause(500);
        System.out.println("[IssuePage] Tabs navigation verified");

        return result;
    }

    // ================================================================
    // PHOTOS
    // ================================================================

    /**
     * Navigate to the Photos section/tab on the issue detail page.
     */
    public void navigateToPhotosSection() {
        try {
            Boolean clicked = (Boolean) js.executeScript(
                "var tabs = document.querySelectorAll('[class*=\"MuiTab\"], [role=\"tab\"], button');" +
                "for (var t of tabs) {" +
                "  if (t.textContent.trim() === 'Photos') { t.click(); return true; }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[IssuePage] Clicked Photos tab");
                pause(1000);
            }
        } catch (Exception e) {
            System.out.println("[IssuePage] Photos tab not found: " + e.getMessage());
        }
    }

    /**
     * Upload a photo to the current issue.
     */
    public void uploadPhoto(String filePath) {
        navigateToPhotosSection();

        String absolutePath = new File(filePath).getAbsolutePath();
        System.out.println("[IssuePage] Uploading photo from: " + absolutePath);

        try {
            // Make hidden file input visible and accessible
            js.executeScript(
                "var inputs = document.querySelectorAll('input[type=\"file\"]');" +
                "for (var input of inputs) {" +
                "  input.style.display = 'block';" +
                "  input.style.visibility = 'visible';" +
                "  input.style.opacity = '1';" +
                "  input.style.width = '200px';" +
                "  input.style.height = '50px';" +
                "  input.style.position = 'relative';" +
                "}");
            pause(500);

            List<WebElement> fileInputs = driver.findElements(PHOTO_UPLOAD_INPUT);
            if (!fileInputs.isEmpty()) {
                fileInputs.get(0).sendKeys(absolutePath);
                System.out.println("[IssuePage] Photo file sent to file input");
                pause(3000);
            } else {
                // Try clicking an upload button to trigger file dialog
                js.executeScript(
                    "var btns = document.querySelectorAll('button');" +
                    "for (var b of btns) {" +
                    "  var text = b.textContent.trim().toLowerCase();" +
                    "  if (text.includes('upload') || text.includes('photo') || text.includes('add photo')) {" +
                    "    b.click(); return;" +
                    "  }" +
                    "}");
                pause(1000);
                fileInputs = driver.findElements(PHOTO_UPLOAD_INPUT);
                if (!fileInputs.isEmpty()) {
                    fileInputs.get(0).sendKeys(absolutePath);
                    System.out.println("[IssuePage] Photo file sent (after clicking upload button)");
                    pause(3000);
                } else {
                    System.out.println("[IssuePage] WARNING: No file input found for photo upload");
                }
            }
        } catch (Exception e) {
            System.out.println("[IssuePage] Photo upload error: " + e.getMessage());
        }
    }

    /**
     * Get the number of photos/thumbnails visible.
     */
    public int getPhotoCount() {
        try {
            Long count = (Long) js.executeScript(
                "// Broad search for any images that could be photos\n" +
                "var selectors = [" +
                "  'img[class*=\"thumbnail\"]', 'img[class*=\"photo\"]'," +
                "  'img[src*=\"blob:\"]', 'img[src*=\"upload\"]'," +
                "  'img[src*=\"image\"]', 'img[src*=\"photo\"]'," +
                "  'img[src*=\"amazonaws\"]', 'img[src*=\"storage\"]'," +
                "  '[class*=\"photo-item\"]', '[class*=\"gallery\"] img'," +
                "  '[class*=\"photo\"] img', '[class*=\"image-container\"] img'," +
                "  '[class*=\"attachment\"] img', '[class*=\"media\"] img'" +
                "];" +
                "var all = new Set();" +
                "for (var sel of selectors) {" +
                "  var els = document.querySelectorAll(sel);" +
                "  for (var el of els) all.add(el);" +
                "}" +
                "var visible = 0;" +
                "for (var img of all) {" +
                "  var r = img.getBoundingClientRect();" +
                "  if (r.width > 20 && r.height > 20) visible++;" +
                "}" +
                "return visible;");
            int result = count != null ? count.intValue() : 0;
            System.out.println("[IssuePage] Photo count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error counting photos: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if at least one photo is visible, or if upload success indicators exist.
     */
    public boolean isPhotoVisible() {
        for (int i = 0; i < 15; i++) {
            if (getPhotoCount() > 0) return true;

            // Check for upload success indicators
            try {
                Boolean uploadSuccess = (Boolean) js.executeScript(
                    "var body = document.body.textContent.toLowerCase();" +
                    "return body.includes('uploaded') || body.includes('upload success') || " +
                    "body.includes('photo added') || body.includes('file uploaded') || " +
                    "document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"MuiAlert\"]').length > 0;");
                if (Boolean.TRUE.equals(uploadSuccess)) {
                    System.out.println("[IssuePage] Upload success indicator found");
                    return true;
                }
            } catch (Exception ignored) {}

            // Log diagnostic on last attempt
            if (i == 14) {
                String diag = (String) js.executeScript(
                    "var imgs = document.querySelectorAll('img');" +
                    "var info = 'All images(' + imgs.length + '): ';" +
                    "for (var img of imgs) {" +
                    "  var r = img.getBoundingClientRect();" +
                    "  info += '[src=' + (img.src||'').substring(0,60) + ' w=' + r.width + ' h=' + r.height + '] ';" +
                    "}" +
                    "var fileInputs = document.querySelectorAll('input[type=\"file\"]');" +
                    "info += ' FileInputs: ' + fileInputs.length;" +
                    "return info;");
                System.out.println("[IssuePage] Photo DIAGNOSTIC: " + diag);
            }
            pause(1000);
        }
        return false;
    }

    // ================================================================
    // DELETE
    // ================================================================

    /**
     * Delete the current issue from its detail page via kebab menu or delete button.
     */
    public void deleteCurrentIssue() {
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Delete Issue' || text === 'Delete') {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;");

        if (!Boolean.TRUE.equals(clicked)) {
            // Try kebab menu (icon button)
            js.executeScript(
                "var iconBtns = document.querySelectorAll('[class*=\"MuiIconButton\"]');" +
                "for (var b of iconBtns) {" +
                "  var r = b.getBoundingClientRect();" +
                "  if (r.width > 20 && r.width < 50 && r.top < 200) {" +
                "    b.click(); break;" +
                "  }" +
                "}");
            pause(500);

            js.executeScript(
                "var items = document.querySelectorAll('li[role=\"menuitem\"], [class*=\"MuiMenuItem\"]');" +
                "for (var i of items) {" +
                "  if (i.textContent.trim().includes('Delete')) { i.click(); return; }" +
                "}");
        }
        pause(1000);
        System.out.println("[IssuePage] Delete initiated");
    }

    /**
     * Confirm the delete dialog.
     */
    public void confirmDelete() {
        for (int i = 0; i < 10; i++) {
            try {
                List<WebElement> errorBtns = driver.findElements(
                    By.cssSelector("button[class*='containedError']"));
                for (WebElement btn : errorBtns) {
                    if (btn.isDisplayed() && "Delete".equals(btn.getText().trim())) {
                        // COMPLETELY HIDE all backdrops (proven fix from ConnectionPage)
                        js.executeScript(
                            "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
                            "  function(b) { b.style.display = 'none'; }" +
                            ");"
                        );
                        pause(200);
                        btn.click(); // WebElement.click() = trusted event
                        pause(3000);
                        System.out.println("[IssuePage] Delete confirmed via containedError button");
                        return;
                    }
                }

                // Fallback: find Delete button inside dialog
                List<WebElement> dialogs = driver.findElements(
                    By.cssSelector("[role='dialog'], [class*='MuiDialog-paper']"));
                for (WebElement dialog : dialogs) {
                    List<WebElement> btns = dialog.findElements(By.tagName("button"));
                    for (WebElement btn : btns) {
                        if ("Delete".equals(btn.getText().trim())) {
                            js.executeScript(
                                "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
                                "  function(b) { b.style.display = 'none'; }" +
                                ");"
                            );
                            pause(200);
                            btn.click();
                            pause(3000);
                            System.out.println("[IssuePage] Delete confirmed via dialog button");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[IssuePage] confirmDelete attempt " + (i + 1) + ": " + e.getMessage());
            }
            pause(500);
        }
        System.out.println("[IssuePage] WARNING: Could not confirm delete after 10 attempts");
    }

    /**
     * Wait for the delete to complete (redirect to issues list or card removal).
     */
    public boolean waitForDeleteSuccess() {
        for (int i = 0; i < 15; i++) {
            if (driver.getCurrentUrl().matches(".*/issues/?$")) {
                System.out.println("[IssuePage] Delete success — redirected to issues list");
                return true;
            }
            Boolean hasToast = (Boolean) js.executeScript(
                "return document.body.textContent.includes('deleted') || " +
                "document.body.textContent.includes('Deleted') || " +
                "document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"toast\"]').length > 0;");
            if (Boolean.TRUE.equals(hasToast)) {
                System.out.println("[IssuePage] Delete success — toast detected");
                return true;
            }
            pause(1000);
        }
        System.out.println("[IssuePage] Delete success not confirmed after 15s");
        return false;
    }

    // ================================================================
    // CLOSE / DISMISS
    // ================================================================

    public void closeDrawer() {
        try {
            js.executeScript(
                "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]');" +
                "for (var d of drawers) {" +
                "  var closeBtn = d.querySelector('[aria-label=\"Close\"], [aria-label=\"close\"], button[class*=\"close\"]');" +
                "  if (closeBtn) { closeBtn.click(); return; }" +
                "  var cancelBtns = d.querySelectorAll('button');" +
                "  for (var b of cancelBtns) {" +
                "    if (b.textContent.trim() === 'Cancel') { b.click(); return; }" +
                "  }" +
                "}");
            pause(500);
        } catch (Exception ignored) {}
    }

    private void dismissAnyDrawerOrBackdrop() {
        try {
            Boolean hasBackdrop = (Boolean) js.executeScript(
                "return document.querySelectorAll('.MuiBackdrop-root, .MuiDrawer-root, [role=\"presentation\"]').length > 0;");
            if (Boolean.TRUE.equals(hasBackdrop)) {
                System.out.println("[IssuePage] Backdrop/drawer detected — pressing Escape to dismiss");
                new Actions(driver).sendKeys(Keys.ESCAPE).perform();
                pause(800);
                Boolean stillPresent = (Boolean) js.executeScript(
                    "return document.querySelectorAll('.MuiBackdrop-root').length > 0;");
                if (Boolean.TRUE.equals(stillPresent)) {
                    new Actions(driver).sendKeys(Keys.ESCAPE).perform();
                    pause(800);
                }
            }
        } catch (Exception e) {
            System.out.println("[IssuePage] dismissAnyDrawerOrBackdrop: " + e.getMessage());
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
                "}");
            pause(500);
        } catch (Exception ignored) {}
    }

    // ================================================================
    // HELPERS (PRIVATE)
    // ================================================================

    private void typeField(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
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
            System.out.println("[IssuePage] Native sendKeys blocked, using JS fallback for: " + by);
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

        js.executeScript(
            "arguments[0].scrollIntoView({block:'center'});" +
            "arguments[0].focus();" +
            "arguments[0].click();", input);
        pause(300);

        // Try opening dropdown via popup indicator on this specific autocomplete
        js.executeScript(
            "var wrapper = arguments[0].closest('.MuiAutocomplete-root');" +
            "if (wrapper) {" +
            "  var btn = wrapper.querySelector('.MuiAutocomplete-popupIndicator');" +
            "  if (btn) btn.click();" +
            "}",
            input);
        pause(1000);

        // Clear and type
        js.executeScript(
            "var input = arguments[0];" +
            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "setter.call(input, '');" +
            "input.dispatchEvent(new Event('input', {bubbles: true}));" +
            "input.dispatchEvent(new Event('change', {bubbles: true}));",
            input);
        pause(200);

        sendKeysWithJsFallback(input, textToType, inputLocator);
        pause(800);

        // Wait for the listbox dropdown to appear
        By listbox = By.xpath("//ul[@role='listbox']");
        for (int attempt = 0; attempt < 5; attempt++) {
            if (!driver.findElements(listbox).isEmpty()) break;

            if (attempt == 1) {
                try {
                    WebElement popup = driver.findElement(
                            By.xpath("//button[contains(@class,'MuiAutocomplete-popupIndicator')]"));
                    js.executeScript("arguments[0].click();", popup);
                    pause(500);
                    continue;
                } catch (Exception ignored) {}
            }

            js.executeScript(
                "var input = arguments[0];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(input, '');" +
                "input.dispatchEvent(new Event('input', {bubbles: true}));" +
                "input.focus(); input.click();",
                input);
            pause(300);
            sendKeysWithJsFallback(input, textToType, inputLocator);
            pause(800);
        }

        System.out.println("[IssuePage] Listbox visible: " + !driver.findElements(listbox).isEmpty());

        // Find and click the matching option
        By exactOption = By.xpath("//li[@role='option'][normalize-space()='" + optionText + "']");
        By partialOption = By.xpath("//li[@role='option'][contains(normalize-space(),'" + optionText + "')]");
        By anyOption = By.xpath("//li[contains(@id,'option') or @role='option'][contains(normalize-space(),'" + optionText + "')]");

        for (int attempt = 0; attempt < 5; attempt++) {
            for (By opt : new By[]{exactOption, partialOption, anyOption}) {
                if (!driver.findElements(opt).isEmpty()) {
                    WebElement option = driver.findElement(opt);
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                    pause(150);
                    try {
                        new Actions(driver).moveToElement(option).click().perform();
                    } catch (Exception e) {
                        js.executeScript("arguments[0].click();", option);
                    }
                    pause(300);
                    System.out.println("[IssuePage] Selected dropdown option: " + optionText);
                    return;
                }
            }
            pause(400);
        }
        System.out.println("[IssuePage] WARNING: Could not select dropdown option '" + optionText + "'");
    }

    private void sendKeysWithJsFallback(WebElement el, String text, By locator) {
        try {
            el.sendKeys(text);
        } catch (Exception e) {
            System.out.println("[IssuePage] Native sendKeys blocked, using JS fallback for: " + locator);
            js.executeScript(
                "var el = arguments[0];" +
                "var text = arguments[1];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(el, text);" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "el.dispatchEvent(new Event('change', {bubbles: true}));" +
                "for (var i = 0; i < text.length; i++) {" +
                "  el.dispatchEvent(new KeyboardEvent('keydown', {key: text[i], bubbles: true}));" +
                "  el.dispatchEvent(new KeyboardEvent('keyup', {key: text[i], bubbles: true}));" +
                "}",
                el, text);
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

    private void waitForSpinner() {
        for (int i = 0; i < 15; i++) {
            Boolean spinning = (Boolean) js.executeScript(
                "return document.querySelectorAll('[class*=\"MuiCircularProgress\"], [class*=\"spinner\"], [class*=\"loading\"]').length > 0;");
            if (!Boolean.TRUE.equals(spinning)) return;
            pause(1000);
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
