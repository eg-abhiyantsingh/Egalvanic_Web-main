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
 * Page Object Model for the Job/Work Orders page.
 * Supports CRUD operations on work orders plus IR Photos, Locations, and Tasks.
 *
 * UI structure (from live app):
 *   - Work Orders list is a TABLE/GRID with columns
 *   - "Create Work Order" / "Create Job" button opens a right-side drawer
 *   - Detail page has: Tasks section, IR Photos, Locations, Edit capability
 *   - Clicking a row opens the work order detail page
 */
public class WorkOrderPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    // ================================================================
    // LOCATORS
    // ================================================================

    // Navigation — sidebar link (may be "Jobs", "Work Orders", or "Job/Work Orders")
    private static final By WORK_ORDERS_NAV = By.xpath(
            "//a[normalize-space()='Jobs' or normalize-space()='Work Orders' or normalize-space()='Job/Work Orders']"
            + " | //span[normalize-space()='Jobs' or normalize-space()='Work Orders' or normalize-space()='Job/Work Orders']");

    // Create button (page header)
    private static final By CREATE_WORK_ORDER_BTN = By.xpath(
            "//button[normalize-space()='Create Work Order' or normalize-space()='Create Job'"
            + " or contains(normalize-space(),'Create Work Order') or contains(normalize-space(),'Create Job')]");

    // Form header (drawer title)
    private static final By ADD_WORK_ORDER_HEADER = By.xpath(
            "//*[normalize-space()='Add Work Order' or normalize-space()='Add Job'"
            + " or normalize-space()='Create Work Order' or normalize-space()='Create Job'"
            + " or normalize-space()='New Work Order' or normalize-space()='New Job']");

    // Form fields — flexible multi-strategy locators for MUI Autocomplete
    private static final By NAME_INPUT = By.xpath(
            "//input[@placeholder='Enter Work Order Name' or @placeholder='Enter Job Name'"
            + " or @placeholder='Work Order Name' or @placeholder='Job Name'"
            + " or @placeholder='Name' or @placeholder='Enter name']"
            + " | //label[contains(text(),'Name')]/following::input[1]"
            + " | //div[contains(@class,'MuiDrawer-paper')]//input[@type='text'][1]");

    private static final By DESCRIPTION_INPUT = By.xpath(
            "//textarea[@placeholder='Description' or @placeholder='Enter description'"
            + " or @placeholder='Enter Description' or @placeholder='Work Order Description']"
            + " | //label[contains(text(),'Description')]/following::textarea[1]"
            + " | //label[contains(text(),'Description')]/following::input[1]");

    private static final By PRIORITY_INPUT = By.xpath(
            "//label[contains(text(),'Priority')]/following::input[1]"
            + " | //input[@placeholder='Priority' or @placeholder='Select priority'"
            + " or @placeholder='Select a priority']");

    private static final By STATUS_INPUT = By.xpath(
            "//label[contains(text(),'Status')]/following::input[1]"
            + " | //input[@placeholder='Status' or @placeholder='Select status'"
            + " or @placeholder='Select a status']");

    private static final By ASSET_INPUT = By.xpath(
            "//input[@placeholder='Select an asset' or @placeholder='Select Asset'"
            + " or @placeholder='Select asset' or @placeholder='Asset'"
            + " or @placeholder='Search asset' or @placeholder='Choose Asset']"
            + " | //label[contains(text(),'Asset')]/following::input[1]");

    private static final By LOCATION_INPUT = By.xpath(
            "//input[@placeholder='Select a location' or @placeholder='Select Location'"
            + " or @placeholder='Location' or @placeholder='Select location']"
            + " | //label[contains(text(),'Location')]/following::input[1]");

    private static final By ASSIGNEE_INPUT = By.xpath(
            "//input[@placeholder='Select assignee' or @placeholder='Assignee'"
            + " or @placeholder='Select an assignee' or @placeholder='Assign to']"
            + " | //label[contains(text(),'Assign')]/following::input[1]");

    // Search
    private static final By SEARCH_INPUT = By.xpath(
            "//input[contains(@placeholder,'Search') or contains(@placeholder,'search')]");

    // Table rows
    private static final By TABLE_ROWS = By.xpath(
            "//tbody//tr | //div[contains(@class,'MuiDataGrid-row') and @data-rowindex]");

    // IR Photo upload
    private static final By PHOTO_UPLOAD_INPUT = By.xpath("//input[@type='file']");
    private static final By PHOTO_THUMBNAILS = By.xpath(
            "//img[contains(@class,'thumbnail') or contains(@class,'photo') or contains(@src,'blob:') or contains(@src,'upload')]");

    // Tasks section
    private static final By ADD_TASK_BTN = By.xpath(
            "//button[normalize-space()='Add Task' or contains(normalize-space(),'Add Task')"
            + " or normalize-space()='Create Task' or contains(normalize-space(),'Create Task')]");

    private static final By TASK_NAME_INPUT = By.xpath(
            "//input[@placeholder='Task Name' or @placeholder='Enter task name'"
            + " or @placeholder='Task name' or @placeholder='Enter Task Name']"
            + " | //label[contains(text(),'Task')]/following::input[1]");

    // Delete
    private static final By DELETE_BTN = By.xpath(
            "//button[normalize-space()='Delete Work Order' or normalize-space()='Delete Job'"
            + " or normalize-space()='Delete']");
    private static final By CONFIRM_DELETE_BTN = By.xpath(
            "//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");

    // Edit
    private static final By EDIT_BTN = By.xpath(
            "//button[normalize-space()='Edit' or normalize-space()='Edit Work Order'"
            + " or normalize-space()='Edit Job' or contains(normalize-space(),'Edit')]");

    private static final By SAVE_BTN = By.xpath(
            "//button[normalize-space()='Save' or normalize-space()='Update'"
            + " or normalize-space()='Save Changes']");

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public WorkOrderPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /**
     * Navigate to the Work Orders page via sidebar.
     * If already on the page, navigate away and back for fresh data.
     */
    public void navigateToWorkOrders() {
        dismissAnyDrawerOrBackdrop();

        if (isOnWorkOrdersPage()) {
            System.out.println("[WorkOrderPage] Already on Work Orders — navigating away and back");
            try {
                js.executeScript(
                    "var links = document.querySelectorAll('a');" +
                    "for (var el of links) {" +
                    "  if (el.textContent.trim() === 'Assets' || el.textContent.trim() === 'Locations') { el.click(); return; }" +
                    "}");
                pause(1500);
            } catch (Exception e) {
                System.out.println("[WorkOrderPage] Nav away failed: " + e.getMessage());
            }
        }

        // Click Jobs/Work Orders/Sessions in sidebar
        js.executeScript(
            "var links = document.querySelectorAll('a');" +
            "for (var el of links) {" +
            "  var text = el.textContent.trim();" +
            "  if (text === 'Jobs' || text === 'Work Orders' || text === 'Job/Work Orders' || text === 'Sessions') { el.click(); return; }" +
            "}"
        );
        pause(2000);

        waitForSpinner();
        pause(1000);
        System.out.println("[WorkOrderPage] On work orders page: " + driver.getCurrentUrl());
    }

    public boolean isOnWorkOrdersPage() {
        String url = driver.getCurrentUrl().toLowerCase();
        return url.contains("/jobs") || url.contains("/work-orders") || url.contains("/workorders")
            || url.contains("/sessions");
    }

    // ================================================================
    // CREATE
    // ================================================================

    /**
     * Open the Create Work Order form (right drawer).
     */
    public void openCreateWorkOrderForm() {
        dismissAnyDialog();
        pause(300);

        // Click the "Create Work Order" / "Create Job" button in page header
        js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Create Work Order' || text === 'Create Job'" +
            "      || text.includes('Create Work Order') || text.includes('Create Job')) {" +
            "    var r = b.getBoundingClientRect();" +
            "    if (r.width > 0 && r.top < 300) { b.click(); return; }" +
            "  }" +
            "}"
        );
        pause(2000);

        // Wait for form drawer to appear using JS polling (more reliable than XPath waits)
        boolean drawerFound = false;
        for (int i = 0; i < 20; i++) {
            Boolean found = (Boolean) js.executeScript(
                "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
                "for (var d of drawers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 400 && d.querySelectorAll('input').length > 0) return true;" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(found)) {
                drawerFound = true;
                break;
            }
            // Retry click at iteration 10
            if (i == 10) {
                js.executeScript(
                    "var btns = document.querySelectorAll('button');" +
                    "for (var b of btns) {" +
                    "  var text = b.textContent.trim();" +
                    "  if (text.includes('Create Work Order') || text.includes('Create Job')) {" +
                    "    b.click(); return;" +
                    "  }" +
                    "}");
            }
            pause(1000);
        }

        if (!drawerFound) {
            String diag = (String) js.executeScript(
                "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
                "var info = 'Drawers(' + drawers.length + '): ';" +
                "for (var d of drawers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  info += '{w=' + Math.round(r.width) + ' inputs=' + d.querySelectorAll('input').length + '} ';" +
                "}" +
                "return info;");
            System.out.println("[WorkOrderPage] DIAGNOSTIC: Form drawer not found. " + diag);
        }
        System.out.println("[WorkOrderPage] Create form opened: " + drawerFound);
    }

    /**
     * Fill the work order name field.
     */
    public void fillName(String name) {
        typeField(NAME_INPUT, name);
        System.out.println("[WorkOrderPage] Filled name: " + name);
    }

    /**
     * Fill the description field.
     */
    public void fillDescription(String description) {
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(DESCRIPTION_INPUT));
            js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});" +
                "arguments[0].focus();" +
                "arguments[0].click();", el);
            pause(200);
            el.sendKeys(description);
            pause(300);
            System.out.println("[WorkOrderPage] Filled description");
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Description field not found or not fillable: " + e.getMessage());
        }
    }

    /**
     * Select priority from dropdown.
     */
    public void selectPriority(String priority) {
        typeAndSelectDropdown(PRIORITY_INPUT, priority, priority);
        System.out.println("[WorkOrderPage] Selected priority: " + priority);
    }

    /**
     * Select asset from dropdown.
     */
    public void selectAsset(String assetName) {
        typeAndSelectDropdown(ASSET_INPUT, assetName, assetName);
        System.out.println("[WorkOrderPage] Selected asset: " + assetName);
    }

    /**
     * Select location from dropdown.
     */
    public void selectLocation(String locationName) {
        typeAndSelectDropdown(LOCATION_INPUT, locationName, locationName);
        System.out.println("[WorkOrderPage] Selected location: " + locationName);
    }

    /**
     * Select assignee from dropdown.
     */
    public void selectAssignee(String assigneeName) {
        typeAndSelectDropdown(ASSIGNEE_INPUT, assigneeName, assigneeName);
        System.out.println("[WorkOrderPage] Selected assignee: " + assigneeName);
    }

    /**
     * Submit the Create Work Order form (click Save/Create inside the drawer).
     */
    public void submitCreateWorkOrder() {
        js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]');" +
            "for (var d of drawers) {" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    var text = b.textContent.trim();" +
            "    if ((text === 'Create Work Order' || text === 'Create Job' || text === 'Create' || text === 'Save' || text === 'Submit')" +
            "        && b.getBoundingClientRect().width > 0) {" +
            "      b.scrollIntoView({block:'center'});" +
            "      b.click();" +
            "      return;" +
            "    }" +
            "  }" +
            "}"
        );
        System.out.println("[WorkOrderPage] Clicked submit button");
    }

    /**
     * Wait for work order creation to succeed (drawer closes or success toast).
     */
    public boolean waitForCreateSuccess() {
        for (int i = 0; i < 20; i++) {
            // Check 1: Is the form drawer gone?
            // Look for any wide drawer containing form keywords
            Boolean drawerGone = (Boolean) js.executeScript(
                "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
                "for (var d of drawers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 400 && (" +
                "    d.textContent.includes('Add Work Order') || d.textContent.includes('Add Job') ||" +
                "    d.textContent.includes('Create Work Order') || d.textContent.includes('Create Job') ||" +
                "    d.textContent.includes('BASIC INFO') || d.textContent.includes('New Work Order')" +
                "  )) return false;" +
                "}" +
                "return true;");
            if (Boolean.TRUE.equals(drawerGone)) {
                System.out.println("[WorkOrderPage] Create form drawer closed — creation successful");
                return true;
            }

            // Check 2: Success toast/snackbar (only check specific toast elements, not body text)
            try {
                Boolean hasToast = (Boolean) js.executeScript(
                    "var snackbars = document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"MuiAlert\"], [class*=\"notistack\"], [class*=\"toast\"]');" +
                    "for (var s of snackbars) {" +
                    "  var text = s.textContent.toLowerCase();" +
                    "  var r = s.getBoundingClientRect();" +
                    "  if (r.width > 0 && (text.includes('created') || text.includes('success') || text.includes('saved'))) return true;" +
                    "}" +
                    "return false;");
                if (Boolean.TRUE.equals(hasToast)) {
                    System.out.println("[WorkOrderPage] Success toast found");
                    closeDrawer();
                    return true;
                }
            } catch (Exception ignored) {}

            pause(1000);
        }

        System.out.println("[WorkOrderPage] Create form still open after 20s — creation may have failed");
        // Log diagnostic
        String diag = (String) js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
            "var info = 'Drawers(' + drawers.length + '): ';" +
            "for (var d of drawers) {" +
            "  var r = d.getBoundingClientRect();" +
            "  info += '{w=' + Math.round(r.width) + '} ';" +
            "}" +
            "var errors = document.querySelectorAll('.Mui-error, [class*=\"Mui-error\"], [class*=\"helperText\"][class*=\"error\"]');" +
            "info += ' Errors(' + errors.length + '): ';" +
            "for (var e of errors) info += '\"' + e.textContent.trim().substring(0,40) + '\" ';" +
            "return info;");
        System.out.println("[WorkOrderPage] DIAGNOSTIC: " + diag);
        closeDrawer();
        return false;
    }

    // ================================================================
    // TABLE / READ
    // ================================================================

    /**
     * Get the count of visible work order rows in the table/grid.
     */
    public int getRowCount() {
        try {
            Long count = (Long) js.executeScript(
                "var tableRows = document.querySelectorAll('tbody tr');" +
                "var visible = 0;" +
                "for (var r of tableRows) {" +
                "  var rect = r.getBoundingClientRect();" +
                "  if (rect.width > 50 && rect.height > 10) visible++;" +
                "}" +
                "if (visible > 0) return visible;" +
                "var gridRows = document.querySelectorAll('[data-rowindex]');" +
                "return gridRows.length;"
            );
            int result = count != null ? count.intValue() : 0;
            System.out.println("[WorkOrderPage] Row count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error counting rows: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if the work orders table has any rows populated.
     */
    public boolean isRowsPopulated() {
        for (int i = 0; i < 10; i++) {
            if (getRowCount() > 0) return true;
            pause(1000);
        }
        return false;
    }

    /**
     * Get the title/name from the first row.
     */
    public String getFirstRowTitle() {
        try {
            String title = (String) js.executeScript(
                "var tableRows = document.querySelectorAll('tbody tr');" +
                "if (tableRows.length > 0) {" +
                "  var cells = tableRows[0].querySelectorAll('td');" +
                "  if (cells.length > 0) return cells[0].textContent.trim();" +
                "}" +
                "var gridRows = document.querySelectorAll('[data-rowindex]');" +
                "if (gridRows.length > 0) {" +
                "  var row = gridRows[0];" +
                "  var nameCell = row.querySelector('[data-field=\"title\"], [data-field=\"name\"], [data-field=\"jobName\"], [data-field=\"workOrderName\"]');" +
                "  if (nameCell) return nameCell.textContent.trim();" +
                "  var cells = row.querySelectorAll('[data-field]');" +
                "  for (var cell of cells) {" +
                "    var txt = cell.textContent.trim();" +
                "    if (txt.length > 1 && txt.length < 100) return txt;" +
                "  }" +
                "}" +
                "return null;"
            );
            System.out.println("[WorkOrderPage] First row title: " + title);
            return title;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error getting first row title: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a work order with the given name is visible on the page.
     */
    public boolean isWorkOrderVisible(String name) {
        try {
            Boolean found = (Boolean) js.executeScript(
                "var rows = document.querySelectorAll('tbody tr, [data-rowindex]');" +
                "for (var r of rows) {" +
                "  if (r.textContent.indexOf(arguments[0]) > -1) return true;" +
                "}" +
                "return document.body.textContent.indexOf(arguments[0]) > -1;",
                name);
            boolean result = Boolean.TRUE.equals(found);
            System.out.println("[WorkOrderPage] Work order '" + name + "' visible: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error checking visibility: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SEARCH / FILTER
    // ================================================================

    /**
     * Search for work orders by typing in the search input.
     */
    public void searchWorkOrders(String query) {
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
            System.out.println("[WorkOrderPage] Searched for: " + query);
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Search failed: " + e.getMessage());
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
            System.out.println("[WorkOrderPage] Search cleared");
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Clear search failed: " + e.getMessage());
        }
    }

    /**
     * Click a filter chip or filter option.
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
        System.out.println("[WorkOrderPage] Filter '" + filterLabel + "': " + (Boolean.TRUE.equals(filtered) ? "applied" : "not found"));
    }

    /**
     * Click a sort option (column header).
     */
    public void clickSortOption(String sortLabel) {
        Boolean sorted = (Boolean) js.executeScript(
            "var sortLabel = arguments[0];" +
            "var headers = document.querySelectorAll('th, [role=\"columnheader\"], [class*=\"sortable\"], thead td');" +
            "for (var h of headers) {" +
            "  if (h.textContent.trim().includes(sortLabel)) { h.click(); return true; }" +
            "}" +
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
        System.out.println("[WorkOrderPage] Sort by '" + sortLabel + "': " + (Boolean.TRUE.equals(sorted) ? "applied" : "not found"));
    }

    // ================================================================
    // DETAIL PAGE
    // ================================================================

    /**
     * Click on the first work order row to open its detail page.
     */
    public void openFirstWorkOrderDetail() {
        js.executeScript(
            "var tableRows = document.querySelectorAll('tbody tr');" +
            "if (tableRows.length > 0) {" +
            "  var link = tableRows[0].querySelector('a');" +
            "  if (link) { link.click(); return; }" +
            "  tableRows[0].click(); return;" +
            "}" +
            "var gridRows = document.querySelectorAll('[data-rowindex]');" +
            "if (gridRows.length > 0) {" +
            "  var link = gridRows[0].querySelector('a');" +
            "  if (link) { link.click(); return; }" +
            "  gridRows[0].click(); return;" +
            "}"
        );
        pause(3000);
        waitForDetailPageLoad();
        System.out.println("[WorkOrderPage] Opened work order detail: " + driver.getCurrentUrl());
    }

    /**
     * Open a specific work order by name.
     */
    public void openWorkOrderDetail(String name) {
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
            name);
        pause(3000);
        waitForDetailPageLoad();
        System.out.println("[WorkOrderPage] Opened work order detail for: " + name);
    }

    /**
     * Wait for the detail page to fully load.
     */
    public void waitForDetailPageLoad() {
        for (int i = 0; i < 16; i++) {
            try {
                Long mainElements = (Long) js.executeScript(
                    "return document.querySelectorAll('main *, [class*=\"detail\"] *, [class*=\"content\"] *').length;");
                if (mainElements != null && mainElements > 10) {
                    System.out.println("[WorkOrderPage] Detail page loaded after " + (i + 1) + "s — " + mainElements + " elements");
                    return;
                }
            } catch (Exception ignored) {}
            pause(2000);
        }
        System.out.println("[WorkOrderPage] Detail page may not have fully loaded after 32s");
    }

    // ================================================================
    // EDIT
    // ================================================================

    /**
     * Click the Edit button on the detail page.
     */
    public void clickEdit() {
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Edit' || text === 'Edit Work Order' || text === 'Edit Job') {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;");
        if (!Boolean.TRUE.equals(clicked)) {
            // Try kebab/icon menu
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
                "  if (i.textContent.trim().includes('Edit')) { i.click(); return; }" +
                "}");
        }
        pause(1500);
        System.out.println("[WorkOrderPage] Edit mode entered");
    }

    /**
     * Edit the name field on the detail/edit page.
     */
    public void editName(String newName) {
        By editNameInput = By.xpath(
            "//input[@placeholder='Enter Work Order Name' or @placeholder='Enter Job Name'"
            + " or @placeholder='Name' or @placeholder='Work Order Name' or @placeholder='Job Name']"
            + " | //label[contains(text(),'Name')]/following::input[1]");
        typeField(editNameInput, newName);
        System.out.println("[WorkOrderPage] Edited name to: " + newName);
    }

    /**
     * Edit the description field on the detail/edit page.
     */
    public void editDescription(String newDescription) {
        // Strategy 1: Try XPath locator first
        WebElement el = null;
        try {
            el = wait.until(ExpectedConditions.visibilityOfElementLocated(DESCRIPTION_INPUT));
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Description XPath not found, trying JS approach");
        }

        // Strategy 2: Find via JS — look for textarea/input near "Description" label
        if (el == null) {
            el = (WebElement) js.executeScript(
                "// Find description field by label proximity\n" +
                "var labels = document.querySelectorAll('p, label, span, h6');" +
                "for (var l of labels) {" +
                "  var t = l.textContent.trim();" +
                "  if (t === 'Description' || t === 'Description*') {" +
                "    // Look for textarea or input in nearby containers\n" +
                "    var container = l.closest('.MuiFormControl-root') || l.closest('[class*=\"MuiGrid\"]') || l.parentElement;" +
                "    for (var up = 0; up < 5; up++) {" +
                "      if (!container) break;" +
                "      var ta = container.querySelector('textarea:not([aria-hidden=\"true\"])');" +
                "      if (ta && ta.getBoundingClientRect().width > 50) { ta.scrollIntoView({block:'center'}); return ta; }" +
                "      var inp = container.querySelector('input[type=\"text\"]');" +
                "      if (inp && inp.getBoundingClientRect().width > 50) { inp.scrollIntoView({block:'center'}); return inp; }" +
                "      container = container.parentElement;" +
                "    }" +
                "  }" +
                "}" +
                "// Fallback: any visible textarea on the page\n" +
                "var textareas = document.querySelectorAll('textarea:not([aria-hidden=\"true\"])');" +
                "for (var ta of textareas) {" +
                "  var r = ta.getBoundingClientRect();" +
                "  if (r.width > 100 && r.height > 20) { ta.scrollIntoView({block:'center'}); return ta; }" +
                "}" +
                "return null;");
        }

        if (el != null) {
            // Clear and type using Actions for trusted events
            try {
                js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", el);
                pause(300);
                Actions actions = new Actions(driver);
                actions.moveToElement(el).click().perform();
                pause(200);
                actions.keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL).perform();
                pause(100);
                actions.sendKeys(Keys.DELETE).perform();
                pause(100);
                actions.sendKeys(newDescription).perform();
                pause(300);
                System.out.println("[WorkOrderPage] Edited description via Actions");
            } catch (Exception e) {
                // Fallback: JS setter + sendKeys
                System.out.println("[WorkOrderPage] Actions failed for description: " + e.getMessage());
                try {
                    String tag = el.getTagName().toUpperCase();
                    String proto = tag.equals("TEXTAREA") ? "HTMLTextAreaElement" : "HTMLInputElement";
                    js.executeScript(
                        "var el = arguments[0]; var val = arguments[1];" +
                        "el.focus(); el.click();" +
                        "var setter = Object.getOwnPropertyDescriptor(window." + proto + ".prototype, 'value').set;" +
                        "setter.call(el, val);" +
                        "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles: true}));",
                        el, newDescription);
                    System.out.println("[WorkOrderPage] Edited description via JS setter");
                } catch (Exception e2) {
                    System.out.println("[WorkOrderPage] JS setter also failed: " + e2.getMessage());
                }
            }
        } else {
            // Log diagnostic for debugging
            String diag = (String) js.executeScript(
                "var info = 'LABELS: ';" +
                "var labels = document.querySelectorAll('p, label, span, h6');" +
                "var seen = new Set();" +
                "for (var l of labels) {" +
                "  var t = l.textContent.trim();" +
                "  if (t.length > 1 && t.length < 40 && !seen.has(t)) { seen.add(t); info += '[' + t + '] '; }" +
                "}" +
                "var textareas = document.querySelectorAll('textarea');" +
                "info += ' TAs(' + textareas.length + '): ';" +
                "for (var ta of textareas) {" +
                "  var r = ta.getBoundingClientRect();" +
                "  info += '{w=' + Math.round(r.width) + ' h=' + Math.round(r.height) + ' hidden=' + ta.getAttribute('aria-hidden') + '} ';" +
                "}" +
                "return info;");
            System.out.println("[WorkOrderPage] Edit description failed — no field found. DIAG: " + diag);
        }
    }

    /**
     * Click Save / Update to save edits.
     */
    public void saveEdit() {
        js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Save' || text === 'Update' || text === 'Save Changes') {" +
            "    b.scrollIntoView({block:'center'});" +
            "    b.click(); return;" +
            "  }" +
            "}");
        pause(2000);
        System.out.println("[WorkOrderPage] Save clicked");
    }

    /**
     * Wait for edit save to succeed.
     */
    public boolean waitForEditSuccess() {
        for (int i = 0; i < 15; i++) {
            // Check 1: Success toast/snackbar (specific elements only, not body text)
            try {
                Boolean hasToast = (Boolean) js.executeScript(
                    "var snackbars = document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"MuiAlert\"], [class*=\"notistack\"], [class*=\"toast\"], [class*=\"alert-success\"]');" +
                    "for (var s of snackbars) {" +
                    "  var text = s.textContent.toLowerCase();" +
                    "  var r = s.getBoundingClientRect();" +
                    "  if (r.width > 0 && (text.includes('updated') || text.includes('saved') || text.includes('success'))) return true;" +
                    "}" +
                    "return false;");
                if (Boolean.TRUE.equals(hasToast)) {
                    System.out.println("[WorkOrderPage] Edit success — toast detected");
                    return true;
                }
            } catch (Exception ignored) {}

            // Check 2: Save button is gone (replaced by Edit button) = back to view mode
            Boolean saveGone = (Boolean) js.executeScript(
                "var hasSave = false; var hasEdit = false;" +
                "var btns = document.querySelectorAll('button');" +
                "for (var b of btns) {" +
                "  var t = b.textContent.trim();" +
                "  if (t === 'Save' || t === 'Update' || t === 'Save Changes') hasSave = true;" +
                "  if (t === 'Edit' || t === 'Edit Work Order' || t === 'Edit Job') hasEdit = true;" +
                "}" +
                "return !hasSave && hasEdit;");
            if (Boolean.TRUE.equals(saveGone)) {
                System.out.println("[WorkOrderPage] Edit success — Save button gone, Edit button present");
                return true;
            }

            // Check 3: No more editable form (no focused/active textareas)
            if (i >= 5) {
                Boolean noEditMode = (Boolean) js.executeScript(
                    "var editIndicators = document.querySelectorAll('input:not([type=\"hidden\"]):not([type=\"file\"]):focus, textarea:focus');" +
                    "return editIndicators.length === 0;");
                Boolean editBtnPresent = (Boolean) js.executeScript(
                    "var btns = document.querySelectorAll('button');" +
                    "for (var b of btns) {" +
                    "  if (b.textContent.trim() === 'Edit' || b.textContent.trim() === 'Edit Work Order') return true;" +
                    "}" +
                    "return false;");
                if (Boolean.TRUE.equals(editBtnPresent)) {
                    System.out.println("[WorkOrderPage] Edit success — Edit button visible (iteration " + i + ")");
                    return true;
                }
            }
            pause(1000);
        }
        System.out.println("[WorkOrderPage] Edit success not confirmed after 15s");
        return false;
    }

    // ================================================================
    // IR PHOTOS
    // ================================================================

    /**
     * Navigate to the IR Photos section/tab on the detail page.
     */
    public void navigateToIRPhotosSection() {
        try {
            Boolean clicked = (Boolean) js.executeScript(
                "var tabs = document.querySelectorAll('[class*=\"MuiTab\"], [role=\"tab\"], button');" +
                "for (var t of tabs) {" +
                "  var text = t.textContent.trim();" +
                "  if (text === 'IR Photos' || text === 'Photos' || text === 'Images' || text === 'IR Images') {" +
                "    t.click(); return true;" +
                "  }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[WorkOrderPage] Clicked IR Photos tab");
                pause(1000);
            } else {
                System.out.println("[WorkOrderPage] IR Photos tab not found — may be inline");
            }
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] IR Photos tab navigation failed: " + e.getMessage());
        }
    }

    /**
     * Upload an IR photo.
     */
    public void uploadIRPhoto(String filePath) {
        navigateToIRPhotosSection();

        String absolutePath = new File(filePath).getAbsolutePath();
        System.out.println("[WorkOrderPage] Uploading IR photo from: " + absolutePath);

        try {
            // Make hidden file input visible
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
                System.out.println("[WorkOrderPage] IR photo file sent to file input");
                pause(3000);
            } else {
                // Try clicking an upload button
                js.executeScript(
                    "var btns = document.querySelectorAll('button');" +
                    "for (var b of btns) {" +
                    "  var text = b.textContent.trim().toLowerCase();" +
                    "  if (text.includes('upload') || text.includes('photo') || text.includes('add photo') || text.includes('ir photo')) {" +
                    "    b.click(); return;" +
                    "  }" +
                    "}");
                pause(1000);
                fileInputs = driver.findElements(PHOTO_UPLOAD_INPUT);
                if (!fileInputs.isEmpty()) {
                    fileInputs.get(0).sendKeys(absolutePath);
                    System.out.println("[WorkOrderPage] IR photo file sent (after clicking upload button)");
                    pause(3000);
                } else {
                    System.out.println("[WorkOrderPage] WARNING: No file input found for IR photo upload");
                }
            }
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] IR photo upload error: " + e.getMessage());
        }
    }

    /**
     * Get the number of IR photos/thumbnails visible.
     */
    public int getIRPhotoCount() {
        try {
            Long count = (Long) js.executeScript(
                "var imgs = document.querySelectorAll(" +
                "  'img[class*=\"thumbnail\"], img[class*=\"photo\"], img[class*=\"image\"]," +
                "  img[src*=\"blob:\"], img[src*=\"upload\"], img[src*=\"photo\"], img[src*=\"image\"]," +
                "  img[src*=\"amazonaws\"], img[src*=\"storage\"], img[src*=\"s3\"]," +
                "  [class*=\"photo-item\"], [class*=\"gallery\"] img," +
                "  [class*=\"ir-photo\"] img, [class*=\"IRPhoto\"] img," +
                "  [class*=\"photo\"] img, [class*=\"Photo\"] img');" +
                "var visible = 0;" +
                "for (var img of imgs) {" +
                "  var r = img.getBoundingClientRect();" +
                "  if (r.width > 20 && r.height > 20) visible++;" +
                "}" +
                "// Also check for background-image divs (some apps use div backgrounds)\n" +
                "if (visible === 0) {" +
                "  var divs = document.querySelectorAll('[class*=\"photo\"], [class*=\"Photo\"], [class*=\"thumbnail\"], [class*=\"image\"]');" +
                "  for (var d of divs) {" +
                "    var bg = window.getComputedStyle(d).backgroundImage;" +
                "    var r = d.getBoundingClientRect();" +
                "    if (bg && bg !== 'none' && r.width > 20 && r.height > 20) visible++;" +
                "  }" +
                "}" +
                "return visible;");
            int result = count != null ? count.intValue() : 0;
            System.out.println("[WorkOrderPage] IR photo count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error counting IR photos: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if at least one IR photo is visible.
     */
    public boolean isIRPhotoVisible() {
        for (int i = 0; i < 10; i++) {
            if (getIRPhotoCount() > 0) return true;
            pause(1000);
        }
        return false;
    }

    // ================================================================
    // LOCATIONS (on detail page)
    // ================================================================

    /**
     * Navigate to the Locations section/tab on the detail page.
     */
    public void navigateToLocationsSection() {
        try {
            Boolean clicked = (Boolean) js.executeScript(
                "var tabs = document.querySelectorAll('[class*=\"MuiTab\"], [role=\"tab\"], button');" +
                "for (var t of tabs) {" +
                "  var text = t.textContent.trim();" +
                "  if (text === 'Locations' || text === 'Location') { t.click(); return true; }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[WorkOrderPage] Clicked Locations tab");
                pause(1000);
            } else {
                // Try scrolling to a Locations section header
                js.executeScript(
                    "var headers = document.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span');" +
                    "for (var h of headers) {" +
                    "  if (h.textContent.trim() === 'Locations' || h.textContent.trim() === 'Location') {" +
                    "    h.scrollIntoView({block:'center'}); return;" +
                    "  }" +
                    "}");
                pause(500);
                System.out.println("[WorkOrderPage] Scrolled to Locations section");
            }
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Locations section navigation failed: " + e.getMessage());
        }
    }

    /**
     * Check if a location is displayed on the work order detail page.
     */
    public boolean isLocationDisplayed(String locationName) {
        try {
            Boolean found = (Boolean) js.executeScript(
                "return document.body.textContent.indexOf(arguments[0]) > -1;",
                locationName);
            boolean result = Boolean.TRUE.equals(found);
            System.out.println("[WorkOrderPage] Location '" + locationName + "' displayed: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error checking location: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the count of locations listed on the detail page.
     */
    public int getLocationCount() {
        try {
            Long count = (Long) js.executeScript(
                "var locationItems = document.querySelectorAll('[class*=\"location\"] li, [class*=\"Location\"] li, [class*=\"location-item\"], [class*=\"location-row\"]');" +
                "if (locationItems.length > 0) return locationItems.length;" +
                "// Fallback: check for location text in a list\n" +
                "var lists = document.querySelectorAll('ul li, [role=\"listitem\"]');" +
                "var count = 0;" +
                "for (var l of lists) {" +
                "  var text = l.textContent.trim().toLowerCase();" +
                "  if (text.includes('building') || text.includes('floor') || text.includes('room') || text.includes('location')) count++;" +
                "}" +
                "return count;");
            int result = count != null ? count.intValue() : 0;
            System.out.println("[WorkOrderPage] Location count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error counting locations: " + e.getMessage());
            return 0;
        }
    }

    // ================================================================
    // TASKS
    // ================================================================

    /**
     * Navigate to the Tasks section/tab on the detail page.
     */
    public void navigateToTasksSection() {
        try {
            Boolean clicked = (Boolean) js.executeScript(
                "var tabs = document.querySelectorAll('[class*=\"MuiTab\"], [role=\"tab\"], button');" +
                "for (var t of tabs) {" +
                "  var text = t.textContent.trim();" +
                "  if (text === 'Tasks' || text === 'Task List') { t.click(); return true; }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[WorkOrderPage] Clicked Tasks tab");
                pause(1000);
            } else {
                // Scroll to Tasks section
                js.executeScript(
                    "var headers = document.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span');" +
                    "for (var h of headers) {" +
                    "  if (h.textContent.trim() === 'Tasks' || h.textContent.trim() === 'Task List') {" +
                    "    h.scrollIntoView({block:'center'}); return;" +
                    "  }" +
                    "}");
                pause(500);
                System.out.println("[WorkOrderPage] Scrolled to Tasks section");
            }
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Tasks section navigation failed: " + e.getMessage());
        }
    }

    /**
     * Click the "Add Task" button.
     */
    public void clickAddTask() {
        js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Add Task' || text.includes('Add Task') || text === 'Create Task' || text.includes('Create Task')) {" +
            "    b.scrollIntoView({block:'center'});" +
            "    b.click(); return;" +
            "  }" +
            "}");
        pause(1500);
        System.out.println("[WorkOrderPage] Clicked Add Task button");
    }

    /**
     * Fill the task name field.
     */
    public void fillTaskName(String taskName) {
        try {
            // Try specific task name input first
            List<WebElement> taskInputs = driver.findElements(TASK_NAME_INPUT);
            if (!taskInputs.isEmpty()) {
                WebElement el = taskInputs.get(taskInputs.size() - 1); // Use last (newest) task input
                js.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});" +
                    "arguments[0].focus(); arguments[0].click();", el);
                pause(200);
                el.sendKeys(taskName);
            } else {
                // Fallback: find input in the task dialog/drawer
                js.executeScript(
                    "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]');" +
                    "for (var d of drawers) {" +
                    "  var inputs = d.querySelectorAll('input[type=\"text\"]');" +
                    "  if (inputs.length > 0) {" +
                    "    var input = inputs[0];" +
                    "    input.focus(); input.click();" +
                    "    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                    "    setter.call(input, arguments[0]);" +
                    "    input.dispatchEvent(new Event('input', {bubbles: true}));" +
                    "    input.dispatchEvent(new Event('change', {bubbles: true}));" +
                    "    return;" +
                    "  }" +
                    "}",
                    taskName);
            }
            pause(300);
            System.out.println("[WorkOrderPage] Filled task name: " + taskName);
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Fill task name failed: " + e.getMessage());
        }
    }

    /**
     * Submit the task form.
     */
    public void submitTask() {
        js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]');" +
            "for (var d of drawers) {" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    var text = b.textContent.trim();" +
            "    if (text === 'Create' || text === 'Add' || text === 'Save' || text === 'Add Task' || text === 'Create Task') {" +
            "      b.click(); return;" +
            "    }" +
            "  }" +
            "}" +
            "// Fallback: click any save/create on page\n" +
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Add Task' || text === 'Create Task') {" +
            "    b.click(); return;" +
            "  }" +
            "}");
        pause(2000);
        System.out.println("[WorkOrderPage] Task submitted");
    }

    /**
     * Get count of tasks visible on the detail page.
     */
    public int getTaskCount() {
        try {
            Long count = (Long) js.executeScript(
                "// Strategy 1: task list items\n" +
                "var taskItems = document.querySelectorAll('[class*=\"task-item\"], [class*=\"task-row\"], [class*=\"TaskItem\"], [class*=\"taskItem\"]');" +
                "if (taskItems.length > 0) return taskItems.length;" +
                "// Strategy 2: checkboxes in tasks section\n" +
                "var checkboxes = document.querySelectorAll('[class*=\"task\"] [type=\"checkbox\"], [class*=\"Task\"] [type=\"checkbox\"]');" +
                "if (checkboxes.length > 0) return checkboxes.length;" +
                "// Strategy 3: table rows in tasks area\n" +
                "var taskRows = document.querySelectorAll('[class*=\"task\"] tbody tr, [class*=\"Task\"] tbody tr');" +
                "if (taskRows.length > 0) return taskRows.length;" +
                "// Strategy 4: list items near 'Tasks' heading\n" +
                "var headers = document.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span');" +
                "for (var h of headers) {" +
                "  if (h.textContent.trim() === 'Tasks') {" +
                "    var parent = h.parentElement;" +
                "    while (parent && !parent.querySelector('li, [role=\"listitem\"]')) { parent = parent.parentElement; }" +
                "    if (parent) return parent.querySelectorAll('li, [role=\"listitem\"]').length;" +
                "  }" +
                "}" +
                "return 0;"
            );
            int result = count != null ? count.intValue() : 0;
            System.out.println("[WorkOrderPage] Task count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error counting tasks: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if a task with the given name is visible.
     */
    public boolean isTaskVisible(String taskName) {
        try {
            Boolean found = (Boolean) js.executeScript(
                "return document.body.textContent.indexOf(arguments[0]) > -1;",
                taskName);
            boolean result = Boolean.TRUE.equals(found);
            System.out.println("[WorkOrderPage] Task '" + taskName + "' visible: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Error checking task: " + e.getMessage());
            return false;
        }
    }

    /**
     * Toggle a task's completion status (click the checkbox).
     */
    public void toggleTaskComplete(String taskName) {
        js.executeScript(
            "var name = arguments[0];" +
            "var elements = document.querySelectorAll('li, [role=\"listitem\"], [class*=\"task\"], tr');" +
            "for (var el of elements) {" +
            "  if (el.textContent.indexOf(name) > -1) {" +
            "    var checkbox = el.querySelector('[type=\"checkbox\"], [role=\"checkbox\"]');" +
            "    if (checkbox) { checkbox.click(); return; }" +
            "    el.click(); return;" +
            "  }" +
            "}",
            taskName);
        pause(1000);
        System.out.println("[WorkOrderPage] Toggled task: " + taskName);
    }

    // ================================================================
    // DELETE
    // ================================================================

    /**
     * Delete the current work order from its detail page.
     */
    public void deleteCurrentWorkOrder() {
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Delete Work Order' || text === 'Delete Job' || text === 'Delete') {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;");

        if (!Boolean.TRUE.equals(clicked)) {
            // Try kebab menu
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
        System.out.println("[WorkOrderPage] Delete initiated");
    }

    /**
     * Confirm the delete dialog.
     */
    public void confirmDelete() {
        for (int i = 0; i < 10; i++) {
            Boolean clicked = (Boolean) js.executeScript(
                "var errorBtns = document.querySelectorAll('button[class*=\"containedError\"]');" +
                "for (var b of errorBtns) {" +
                "  var r = b.getBoundingClientRect();" +
                "  if (r.width > 0 && b.textContent.trim() === 'Delete') {" +
                "    b.dispatchEvent(new MouseEvent('mousedown', {bubbles: true}));" +
                "    b.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}));" +
                "    b.dispatchEvent(new MouseEvent('click', {bubbles: true}));" +
                "    return true;" +
                "  }" +
                "}" +
                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [class*=\"MuiDialog-paper\"]');" +
                "for (var d of dialogs) {" +
                "  var text = (d.textContent||'').toLowerCase();" +
                "  if (text.includes('sure') || text.includes('confirm') || text.includes('delete')) {" +
                "    var btns = d.querySelectorAll('button');" +
                "    for (var b of btns) {" +
                "      if (b.textContent.trim() === 'Delete') { b.click(); return true; }" +
                "    }" +
                "  }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[WorkOrderPage] Delete confirmed");
                break;
            }
            pause(500);
        }
        pause(3000);
    }

    /**
     * Wait for the delete to complete.
     */
    public boolean waitForDeleteSuccess() {
        for (int i = 0; i < 15; i++) {
            String url = driver.getCurrentUrl().toLowerCase();
            if (url.matches(".*/jobs/?$") || url.matches(".*/work-orders/?$") || url.matches(".*/workorders/?$")) {
                System.out.println("[WorkOrderPage] Delete success — redirected to list");
                return true;
            }
            Boolean hasToast = (Boolean) js.executeScript(
                "return document.body.textContent.includes('deleted') || " +
                "document.body.textContent.includes('Deleted') || " +
                "document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"toast\"]').length > 0;");
            if (Boolean.TRUE.equals(hasToast)) {
                System.out.println("[WorkOrderPage] Delete success — toast detected");
                return true;
            }
            pause(1000);
        }
        System.out.println("[WorkOrderPage] Delete success not confirmed after 15s");
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
                System.out.println("[WorkOrderPage] Backdrop/drawer detected — pressing Escape");
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
            System.out.println("[WorkOrderPage] dismissAnyDrawerOrBackdrop: " + e.getMessage());
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
            System.out.println("[WorkOrderPage] Native sendKeys blocked, using JS fallback for: " + by);
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

        // Try opening dropdown via popup indicator
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

        // Wait for listbox
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

        System.out.println("[WorkOrderPage] Listbox visible: " + !driver.findElements(listbox).isEmpty());

        // Find and click matching option
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
                    System.out.println("[WorkOrderPage] Selected dropdown option: " + optionText);
                    return;
                }
            }
            pause(400);
        }
        System.out.println("[WorkOrderPage] WARNING: Could not select dropdown option '" + optionText + "'");
    }

    private void sendKeysWithJsFallback(WebElement el, String text, By locator) {
        try {
            el.sendKeys(text);
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Native sendKeys blocked, using JS fallback for: " + locator);
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
