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

    // Facility dropdown (required field on Create form)
    private static final By FACILITY_INPUT = By.xpath(
            "//label[contains(text(),'Facility')]/following::input[1]"
            + " | //input[@placeholder='Select facility' or @placeholder='Facility'"
            + " or @placeholder='Select a facility']");

    // Photo Type dropdown (required field on Create form)
    private static final By PHOTO_TYPE_INPUT = By.xpath(
            "//label[contains(text(),'Photo Type')]/following::input[1]"
            + " | //input[@placeholder='Photo Type' or @placeholder='Select photo type'"
            + " or @placeholder='Select Photo Type']");

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

        // Wait for form to appear — could be a drawer (MuiDrawer) or dialog (MuiDialog)
        boolean formFound = false;
        for (int i = 0; i < 20; i++) {
            Boolean found = (Boolean) js.executeScript(
                "var containers = document.querySelectorAll(" +
                "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]'" +
                ");" +
                "for (var d of containers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 300 && d.querySelectorAll('input').length > 2) return true;" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(found)) {
                formFound = true;
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

        if (!formFound) {
            String diag = (String) js.executeScript(
                "var containers = document.querySelectorAll(" +
                "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]'" +
                ");" +
                "var info = 'Containers(' + containers.length + '): ';" +
                "for (var d of containers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  info += '{w=' + Math.round(r.width) + ' inputs=' + d.querySelectorAll('input').length + ' text=' + d.textContent.substring(0,50) + '} ';" +
                "}" +
                "return info;");
            System.out.println("[WorkOrderPage] DIAGNOSTIC: Form not found. " + diag);
        }
        System.out.println("[WorkOrderPage] Create form opened: " + formFound);
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
     * Select facility from dropdown (required field).
     */
    public void selectFacility(String facilityName) {
        typeAndSelectDropdown(FACILITY_INPUT, facilityName, facilityName);
        System.out.println("[WorkOrderPage] Selected facility: " + facilityName);
    }

    /**
     * Select photo type from dropdown (required field).
     */
    public void selectPhotoType(String photoType) {
        typeAndSelectDropdown(PHOTO_TYPE_INPUT, photoType, photoType);
        System.out.println("[WorkOrderPage] Selected photo type: " + photoType);
    }

    /**
     * Submit the Create Work Order form (click Save/Create inside the drawer).
     */
    public void submitCreateWorkOrder() {
        // Auto-fill any empty required fields (Facility, Photo Type) before submitting
        autoFillRequiredFields();

        // Check submit button state
        String btnState = (String) js.executeScript(
            "var containers = document.querySelectorAll(" +
            "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]'" +
            ");" +
            "for (var d of containers) {" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    var text = b.textContent.trim();" +
            "    if (text === 'Create' || text === 'Create Work Order' || text === 'Create Job'" +
            "        || text === 'Save' || text === 'Submit') {" +
            "      return text + ':disabled=' + b.disabled;" +
            "    }" +
            "  }" +
            "}" +
            "return 'NO_SUBMIT_BTN_FOUND';");
        System.out.println("[WorkOrderPage] Submit button state: " + btnState);

        // If button is disabled, try auto-fill again and wait
        if (btnState.contains("disabled=true")) {
            System.out.println("[WorkOrderPage] Create button disabled — retrying auto-fill");
            autoFillRequiredFields();
            pause(2000);
        }

        // Click the submit button
        String result = (String) js.executeScript(
            "var containers = document.querySelectorAll(" +
            "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]'" +
            ");" +
            "for (var d of containers) {" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    var text = b.textContent.trim();" +
            "    if ((text === 'Create' || text === 'Create Work Order' || text === 'Create Job'" +
            "        || text === 'Save' || text === 'Submit')" +
            "        && b.getBoundingClientRect().width > 0) {" +
            "      b.scrollIntoView({block:'center'});" +
            "      b.click();" +
            "      return 'CLICKED:' + text + ':disabled=' + b.disabled;" +
            "    }" +
            "  }" +
            "}" +
            "return 'NOT_CLICKED';");
        System.out.println("[WorkOrderPage] Submit result: " + result);
    }

    /**
     * Auto-fill required fields that may be empty on the Create form.
     * Fills Facility and Photo Type with defaults if they are blank.
     */
    private void autoFillRequiredFields() {
        // Check which required fields are empty and fill them
        try {
            // Fill Facility if empty
            Boolean facilityEmpty = (Boolean) js.executeScript(
                "var labels = document.querySelectorAll('label');" +
                "for (var l of labels) {" +
                "  if (l.textContent.includes('Facility')) {" +
                "    var fc = l.closest('.MuiFormControl-root') || l.parentElement;" +
                "    var inp = fc.querySelector('input');" +
                "    if (inp && !inp.value) return true;" +
                "  }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(facilityEmpty)) {
                System.out.println("[WorkOrderPage] Facility is empty — auto-filling with 'test site'");
                selectFacility("test site");
                pause(500);
            }
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Facility auto-fill skipped: " + e.getMessage());
        }

        try {
            // Fill Photo Type if empty
            Boolean photoTypeEmpty = (Boolean) js.executeScript(
                "var labels = document.querySelectorAll('label');" +
                "for (var l of labels) {" +
                "  if (l.textContent.includes('Photo Type')) {" +
                "    var fc = l.closest('.MuiFormControl-root') || l.parentElement;" +
                "    var inp = fc.querySelector('input');" +
                "    if (inp && !inp.value) return true;" +
                "  }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(photoTypeEmpty)) {
                System.out.println("[WorkOrderPage] Photo Type is empty — auto-filling with 'FLIR-SEP'");
                selectPhotoType("FLIR-SEP");
                pause(500);
            }
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Photo Type auto-fill skipped: " + e.getMessage());
        }
    }

    /**
     * Wait for work order creation to succeed (drawer closes or success toast).
     */
    public boolean waitForCreateSuccess() {
        for (int i = 0; i < 20; i++) {
            // Check 1: Is the form (drawer OR dialog) gone?
            Boolean formGone = (Boolean) js.executeScript(
                "var containers = document.querySelectorAll(" +
                "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]'" +
                ");" +
                "for (var d of containers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  var t = d.textContent || '';" +
                "  if (r.width > 300 && (" +
                "    t.includes('Create New Work Order') || t.includes('Add Work Order') ||" +
                "    t.includes('Add Job') || t.includes('Create Work Order') ||" +
                "    t.includes('Create Job') || t.includes('BASIC INFO') ||" +
                "    t.includes('New Work Order') || t.includes('WO Name')" +
                "  )) return false;" +
                "}" +
                "return true;");
            if (Boolean.TRUE.equals(formGone)) {
                System.out.println("[WorkOrderPage] Create form closed — creation successful");
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
        // Log diagnostic — check both drawers and dialogs
        String diag = (String) js.executeScript(
            "var containers = document.querySelectorAll(" +
            "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]'" +
            ");" +
            "var info = 'Containers(' + containers.length + '): ';" +
            "for (var d of containers) {" +
            "  var r = d.getBoundingClientRect();" +
            "  info += '{w=' + Math.round(r.width) + ' text=' + d.textContent.substring(0,60) + '} ';" +
            "}" +
            "var errors = document.querySelectorAll('.Mui-error, [class*=\"Mui-error\"], [class*=\"helperText\"][class*=\"error\"]');" +
            "info += ' Errors(' + errors.length + '): ';" +
            "for (var e of errors) info += '\"' + e.textContent.trim().substring(0,40) + '\" ';" +
            "var createBtn = null;" +
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) { if (b.textContent.trim() === 'Create') { createBtn = b; break; } }" +
            "if (createBtn) info += ' CreateBtn: disabled=' + createBtn.disabled;" +
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
     * Dismiss any "What's new" or announcement popups that overlay the page.
     */
    public void dismissPopups() {
        try {
            Boolean dismissed = (Boolean) js.executeScript(
                "// Close 'What's new on Z platform' or similar announcement modals\n" +
                "var closeBtns = document.querySelectorAll('button, [role=\"button\"], svg');" +
                "for (var b of closeBtns) {" +
                "  var r = b.getBoundingClientRect();" +
                "  // Look for X/close button inside a modal/popup\n" +
                "  var text = (b.textContent || '').trim();" +
                "  var ariaLabel = (b.getAttribute('aria-label') || '').toLowerCase();" +
                "  if (ariaLabel.includes('close') || text === '✕' || text === '×' || text === 'X' || text === 'x') {" +
                "    b.click(); return true;" +
                "  }" +
                "}" +
                "// Also try clicking the X icon in any dialog/modal\n" +
                "var dialogs = document.querySelectorAll('[class*=\"modal\"], [class*=\"Modal\"], [class*=\"dialog\"], [class*=\"Dialog\"], [class*=\"popup\"], [class*=\"Popup\"], [class*=\"whatsnew\"], [class*=\"announcement\"]');" +
                "for (var d of dialogs) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 100) {" +
                "    var closeBtn = d.querySelector('[aria-label*=\"close\" i], [aria-label*=\"Close\"], button:last-child');" +
                "    if (closeBtn) { closeBtn.click(); return true; }" +
                "  }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(dismissed)) {
                System.out.println("[WorkOrderPage] Dismissed popup/modal");
                pause(500);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Click the Edit button on the detail page.
     * Tries: 1) Direct Edit button  2) Kebab menu (⋮) → Edit menu item
     */
    public void clickEdit() {
        // First dismiss any popups blocking the page
        dismissPopups();

        // Strategy 1: Direct Edit button
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Edit' || text === 'Edit Work Order' || text === 'Edit Job') {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;");

        if (Boolean.TRUE.equals(clicked)) {
            System.out.println("[WorkOrderPage] Clicked direct Edit button");
            pause(1500);
            System.out.println("[WorkOrderPage] Edit mode entered");
            return;
        }

        // Strategy 2: Click kebab menu (⋮) — the 3-dot icon in top-right of detail page
        System.out.println("[WorkOrderPage] No direct Edit button, trying kebab menu (⋮)");
        Boolean kebabClicked = (Boolean) js.executeScript(
            "// Find the kebab/more icon button — usually near the top-right with aria-label or SVG\n" +
            "var btns = document.querySelectorAll('button, [role=\"button\"]');" +
            "var candidates = [];" +
            "for (var b of btns) {" +
            "  var r = b.getBoundingClientRect();" +
            "  var text = b.textContent.trim();" +
            "  var ariaLabel = (b.getAttribute('aria-label') || '').toLowerCase();" +
            "  // Kebab menu: small button, near top, has MoreVert icon or aria-label\n" +
            "  if (r.width > 15 && r.width < 60 && r.top < 300 && r.right > window.innerWidth - 200) {" +
            "    // Check if it has SVG (icon button) or specific aria labels\n" +
            "    var hasSvg = b.querySelector('svg') !== null;" +
            "    if (hasSvg && (text.length === 0 || text === '⋮' || text === '...')) {" +
            "      candidates.push({el: b, right: r.right});" +
            "    }" +
            "    if (ariaLabel.includes('more') || ariaLabel.includes('menu') || ariaLabel.includes('option')) {" +
            "      candidates.push({el: b, right: r.right});" +
            "    }" +
            "  }" +
            "}" +
            "// Click the rightmost candidate (most likely the kebab)\n" +
            "if (candidates.length > 0) {" +
            "  candidates.sort(function(a,b) { return b.right - a.right; });" +
            "  candidates[0].el.click();" +
            "  return true;" +
            "}" +
            "return false;");

        if (Boolean.TRUE.equals(kebabClicked)) {
            System.out.println("[WorkOrderPage] Clicked kebab menu");
            pause(1000);

            // Now look for Edit in the dropdown menu
            String menuItems = (String) js.executeScript(
                "var items = document.querySelectorAll('li[role=\"menuitem\"], [class*=\"MuiMenuItem\"], [class*=\"menuitem\"], [role=\"option\"]');" +
                "var info = '';" +
                "for (var i of items) {" +
                "  var r = i.getBoundingClientRect();" +
                "  if (r.width > 0) info += '[' + i.textContent.trim() + '] ';" +
                "}" +
                "return info;");
            System.out.println("[WorkOrderPage] Kebab menu items: " + menuItems);

            Boolean editClicked = (Boolean) js.executeScript(
                "var items = document.querySelectorAll('li[role=\"menuitem\"], [class*=\"MuiMenuItem\"], [class*=\"menuitem\"], [role=\"option\"]');" +
                "for (var i of items) {" +
                "  var text = i.textContent.trim();" +
                "  if (text === 'Edit' || text === 'Edit Work Order' || text === 'Edit Job' || text.includes('Edit')) {" +
                "    i.click(); return true;" +
                "  }" +
                "}" +
                "return false;");

            if (Boolean.TRUE.equals(editClicked)) {
                System.out.println("[WorkOrderPage] Selected Edit from kebab menu");
            } else {
                System.out.println("[WorkOrderPage] No Edit option in kebab menu");
            }
        } else {
            System.out.println("[WorkOrderPage] No kebab menu found");
        }

        pause(2000);
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
     * Returns true if a field was found and edited, false otherwise.
     */
    public boolean editDescription(String newDescription) {
        // The description field in edit mode may use click-to-edit:
        // The value is displayed as text, and clicking it activates an inline editor.

        // Strategy 0: Click on the Description value text to activate inline editing
        Boolean clickedDescValue = (Boolean) js.executeScript(
            "var labels = document.querySelectorAll('p, label, span, h6, div');" +
            "for (var l of labels) {" +
            "  var t = l.textContent.trim();" +
            "  if (t === 'Description' || t === 'Description*') {" +
            "    // Look for the sibling/adjacent value element to click\n" +
            "    var parent = l.parentElement;" +
            "    for (var up = 0; up < 5; up++) {" +
            "      if (!parent) break;" +
            "      // Find all text-bearing siblings that aren't the label itself\n" +
            "      var children = parent.querySelectorAll('p, span, div');" +
            "      for (var c of children) {" +
            "        if (c === l || c.contains(l) || l.contains(c)) continue;" +
            "        var ct = c.textContent.trim();" +
            "        var r = c.getBoundingClientRect();" +
            "        if (ct.length > 0 && ct !== 'Description' && r.width > 50 && r.height > 10) {" +
            "          c.scrollIntoView({block:'center'});" +
            "          c.click();" +
            "          return true;" +
            "        }" +
            "      }" +
            "      parent = parent.parentElement;" +
            "    }" +
            "  }" +
            "}" +
            "return false;");
        if (Boolean.TRUE.equals(clickedDescValue)) {
            System.out.println("[WorkOrderPage] Clicked description value to activate inline edit");
            try { Thread.sleep(1000); } catch (Exception ignored) {}
        }

        // Strategy 1: Try XPath locator first (short timeout — 5s)
        WebElement el = null;
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            el = shortWait.until(ExpectedConditions.visibilityOfElementLocated(DESCRIPTION_INPUT));
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Description XPath not found, trying JS approach");
        }

        // Strategy 2: Find via JS — textarea, input, or contentEditable near "Description" label
        if (el == null) {
            el = (WebElement) js.executeScript(
                "var labels = document.querySelectorAll('p, label, span, h6, div');" +
                "for (var l of labels) {" +
                "  var t = l.textContent.trim();" +
                "  if (t === 'Description' || t === 'Description*') {" +
                "    var container = l.closest('.MuiFormControl-root') || l.closest('[class*=\"MuiGrid\"]') || l.parentElement;" +
                "    for (var up = 0; up < 6; up++) {" +
                "      if (!container) break;" +
                "      var ta = container.querySelector('textarea:not([aria-hidden=\"true\"])');" +
                "      if (ta && ta.getBoundingClientRect().width > 50) { ta.scrollIntoView({block:'center'}); return ta; }" +
                "      var inp = container.querySelector('input[type=\"text\"]:not([disabled])');" +
                "      if (inp && inp.getBoundingClientRect().width > 50 && (inp.placeholder||'').indexOf('Search') === -1) { inp.scrollIntoView({block:'center'}); return inp; }" +
                "      var ce = container.querySelector('[contenteditable=\"true\"]');" +
                "      if (ce && ce.getBoundingClientRect().width > 50) { ce.scrollIntoView({block:'center'}); return ce; }" +
                "      container = container.parentElement;" +
                "    }" +
                "  }" +
                "}" +
                "// Fallback: any visible textarea\n" +
                "var textareas = document.querySelectorAll('textarea:not([aria-hidden=\"true\"])');" +
                "for (var ta of textareas) {" +
                "  var r = ta.getBoundingClientRect();" +
                "  if (r.width > 100 && r.height > 20) { ta.scrollIntoView({block:'center'}); return ta; }" +
                "}" +
                "// Fallback: contentEditable\n" +
                "var editables = document.querySelectorAll('[contenteditable=\"true\"]');" +
                "for (var ed of editables) {" +
                "  var r = ed.getBoundingClientRect();" +
                "  if (r.width > 100 && r.height > 20) { ed.scrollIntoView({block:'center'}); return ed; }" +
                "}" +
                "return null;");
        }

        if (el != null) {
            editElementText(el, newDescription);
            System.out.println("[WorkOrderPage] Edited description");
            return true;
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
                "var editables = document.querySelectorAll('[contenteditable=\"true\"]');" +
                "info += ' CEs(' + editables.length + '): ';" +
                "for (var ed of editables) {" +
                "  var r = ed.getBoundingClientRect();" +
                "  info += '{w=' + Math.round(r.width) + ' h=' + Math.round(r.height) + '} ';" +
                "}" +
                "var inputs = document.querySelectorAll('input:not([type=\"hidden\"]):not([type=\"file\"])');" +
                "info += ' INPUTS(' + inputs.length + '): ';" +
                "for (var inp of inputs) {" +
                "  var r = inp.getBoundingClientRect();" +
                "  if (r.width > 50) info += '{ph=\"' + (inp.placeholder||'').substring(0,20) + '\" w=' + Math.round(r.width) + ' dis=' + inp.disabled + '} ';" +
                "}" +
                "return info;");
            System.out.println("[WorkOrderPage] Edit description failed — no field found. DIAG: " + diag);
            return false;
        }
    }

    /**
     * Edit any available text field on the edit page.
     * Tries name/title first, then any visible text input.
     * Returns true if a field was found and edited.
     */
    public boolean editAnyField(String newValue) {
        WebElement el = (WebElement) js.executeScript(
            "// Try to find name/title input\n" +
            "var labels = document.querySelectorAll('p, label, span, h6');" +
            "for (var l of labels) {" +
            "  var t = l.textContent.trim();" +
            "  if (t === 'Name' || t === 'Title' || t === 'Name*' || t === 'Title*' || t === 'Work Order Name' || t === 'Job Name') {" +
            "    var container = l.closest('.MuiFormControl-root') || l.closest('[class*=\"MuiGrid\"]') || l.parentElement;" +
            "    for (var up = 0; up < 5; up++) {" +
            "      if (!container) break;" +
            "      var inp = container.querySelector('input[type=\"text\"]:not([disabled])');" +
            "      if (inp && inp.getBoundingClientRect().width > 50) { inp.scrollIntoView({block:'center'}); return inp; }" +
            "      container = container.parentElement;" +
            "    }" +
            "  }" +
            "}" +
            "// Fallback: any enabled, visible text input (exclude search bars)\n" +
            "var inputs = document.querySelectorAll('input[type=\"text\"]:not([disabled]):not([readonly])');" +
            "for (var inp of inputs) {" +
            "  var r = inp.getBoundingClientRect();" +
            "  var ph = (inp.placeholder||'').toLowerCase();" +
            "  if (r.width > 100 && r.height > 10 && ph.indexOf('search') === -1) { inp.scrollIntoView({block:'center'}); return inp; }" +
            "}" +
            "// Fallback: textarea\n" +
            "var tas = document.querySelectorAll('textarea:not([aria-hidden=\"true\"]):not([disabled])');" +
            "for (var ta of tas) {" +
            "  var r = ta.getBoundingClientRect();" +
            "  if (r.width > 100) { ta.scrollIntoView({block:'center'}); return ta; }" +
            "}" +
            "return null;");

        if (el != null) {
            editElementText(el, newValue);
            System.out.println("[WorkOrderPage] Edited field with value: " + newValue.substring(0, Math.min(30, newValue.length())));
            return true;
        }
        System.out.println("[WorkOrderPage] No editable field found on page");
        return false;
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
            // Check 1: Success toast/snackbar
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

            // Check 2: Save/Update/Cancel buttons are gone = exited edit mode
            Boolean saveGone = (Boolean) js.executeScript(
                "var hasSave = false;" +
                "var btns = document.querySelectorAll('button');" +
                "for (var b of btns) {" +
                "  var t = b.textContent.trim();" +
                "  if (t === 'Save' || t === 'Update' || t === 'Save Changes' || t === 'Cancel') hasSave = true;" +
                "}" +
                "return !hasSave;");
            if (Boolean.TRUE.equals(saveGone)) {
                System.out.println("[WorkOrderPage] Edit success — Save/Cancel buttons gone (back to view mode)");
                return true;
            }

            // Check 3: No more editable form fields (textareas gone, no focused inputs)
            if (i >= 3) {
                Boolean noEditableFields = (Boolean) js.executeScript(
                    "var tas = document.querySelectorAll('textarea:not([aria-hidden=\"true\"])');" +
                    "var visibleTas = 0;" +
                    "for (var ta of tas) { if (ta.getBoundingClientRect().width > 50) visibleTas++; }" +
                    "return visibleTas === 0;");
                if (Boolean.TRUE.equals(saveGone) || Boolean.TRUE.equals(noEditableFields)) {
                    // Double-check: still on the detail page
                    if (driver.getCurrentUrl().contains("/sessions/")) {
                        System.out.println("[WorkOrderPage] Edit success — no editable fields, still on detail page (iteration " + i + ")");
                        return true;
                    }
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
            // Diagnostic: dump the IR Photos section content
            String diag = (String) js.executeScript(
                "var info = 'IR_SECTION: ';" +
                "var fileInputs = document.querySelectorAll('input[type=\"file\"]');" +
                "info += 'FileInputs(' + fileInputs.length + ') ';" +
                "var btns = document.querySelectorAll('button, [role=\"button\"], .MuiFab-root, .MuiIconButton-root');" +
                "var relevantBtns = [];" +
                "for (var b of btns) {" +
                "  var text = (b.textContent || '').trim();" +
                "  var label = b.getAttribute('aria-label') || '';" +
                "  var r = b.getBoundingClientRect();" +
                "  if (r.width > 0 && (text.toLowerCase().match(/upload|photo|add|import|browse|attach/) || " +
                "      label.toLowerCase().match(/upload|photo|add|import/))) {" +
                "    relevantBtns.push('{\"' + text.substring(0,30) + '\" aria=\"' + label.substring(0,30) + '\" at(' + Math.round(r.left) + ',' + Math.round(r.top) + ')}');" +
                "  }" +
                "}" +
                "info += 'UploadBtns[' + relevantBtns.join(', ') + '] ';" +
                "var dropzones = document.querySelectorAll('[class*=\"dropzone\"], [class*=\"Dropzone\"], [class*=\"drop-zone\"], [class*=\"upload-area\"]');" +
                "info += 'Dropzones(' + dropzones.length + ') ';" +
                "var labels = document.querySelectorAll('label');" +
                "var uploadLabels = [];" +
                "for (var l of labels) {" +
                "  var t = l.textContent.trim().toLowerCase();" +
                "  if (t.match(/upload|photo|browse|file/) && l.getBoundingClientRect().width > 0) {" +
                "    uploadLabels.push(l.textContent.trim().substring(0, 30));" +
                "  }" +
                "}" +
                "info += 'Labels[' + uploadLabels.join(', ') + ']';" +
                "return info;");
            System.out.println("[WorkOrderPage] " + diag);

            // Count existing file inputs BEFORE clicking anything
            int inputsBefore = driver.findElements(PHOTO_UPLOAD_INPUT).size();
            System.out.println("[WorkOrderPage] File inputs before: " + inputsBefore);

            // Strategy 1: Find and Selenium-click upload/add/photo buttons to trigger
            // the IR photo upload dialog. Use Selenium click (not JS) for React compatibility.
            boolean triggerClicked = false;
            List<WebElement> allButtons = driver.findElements(By.cssSelector(
                "button, [role='button'], .MuiFab-root, .MuiIconButton-root, label[for]"));
            for (WebElement btn : allButtons) {
                try {
                    if (!btn.isDisplayed() || btn.getSize().getWidth() < 5) continue;
                    String text = btn.getText().trim().toLowerCase();
                    String ariaLabel = btn.getDomAttribute("aria-label");
                    if (ariaLabel == null) ariaLabel = "";
                    ariaLabel = ariaLabel.toLowerCase();

                    if (text.matches(".*\\b(upload|add photo|add image|import)\\b.*")
                            || ariaLabel.matches(".*\\b(upload|add photo|add image)\\b.*")) {
                        System.out.println("[WorkOrderPage] Clicking upload trigger: '" + btn.getText().trim() + "'");
                        btn.click();
                        triggerClicked = true;
                        pause(2000);
                        break;
                    }
                } catch (Exception ignored) {}
            }

            // Strategy 2: If no explicit upload button, click FAB/icon buttons near the IR section
            if (!triggerClicked) {
                System.out.println("[WorkOrderPage] No upload button found — trying FAB/icon buttons");
                for (WebElement btn : allButtons) {
                    try {
                        String cls = btn.getDomAttribute("class");
                        if (cls != null && (cls.contains("MuiFab") || cls.contains("MuiIconButton"))) {
                            if (btn.isDisplayed()) {
                                System.out.println("[WorkOrderPage] Clicking FAB/icon button");
                                btn.click();
                                pause(2000);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Check if a NEW file input appeared
            pause(1000);
            // Make all file inputs visible
            js.executeScript(
                "document.querySelectorAll('input[type=\"file\"]').forEach(function(input) {" +
                "  input.style.display = 'block'; input.style.visibility = 'visible';" +
                "  input.style.opacity = '1'; input.style.width = '200px'; input.style.height = '50px';" +
                "  input.style.position = 'relative'; input.style.zIndex = '9999';" +
                "});");
            pause(500);

            List<WebElement> fileInputs = driver.findElements(PHOTO_UPLOAD_INPUT);
            int inputsAfter = fileInputs.size();
            System.out.println("[WorkOrderPage] File inputs after trigger: " + inputsAfter);

            if (inputsAfter > inputsBefore) {
                // New input appeared — send file to the LAST one (most recently created)
                WebElement newInput = fileInputs.get(fileInputs.size() - 1);
                newInput.sendKeys(absolutePath);
                js.executeScript("arguments[0].dispatchEvent(new Event('change', {bubbles: true}));", newInput);
                System.out.println("[WorkOrderPage] File sent to NEW input (last of " + inputsAfter + ")");
                pause(2000);
            } else if (!fileInputs.isEmpty()) {
                // Try each existing input
                for (int fi = 0; fi < fileInputs.size(); fi++) {
                    try {
                        WebElement input = fileInputs.get(fi);
                        input.sendKeys(absolutePath);
                        js.executeScript("arguments[0].dispatchEvent(new Event('change', {bubbles: true}));", input);
                        System.out.println("[WorkOrderPage] File sent to existing input[" + fi + "] of " + fileInputs.size());
                        pause(1500);
                    } catch (Exception e) {
                        System.out.println("[WorkOrderPage] Input[" + fi + "] failed: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("[WorkOrderPage] WARNING: No file input found after all strategies");
                return;
            }

            // After file selection, click "Upload" button if one appears
            boolean uploaded = false;
            for (int attempt = 0; attempt < 5; attempt++) {
                try {
                    js.executeScript(
                        "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
                        "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; });");
                    pause(200);

                    List<WebElement> allBtns = driver.findElements(By.tagName("button"));
                    for (WebElement btn : allBtns) {
                        try {
                            if (btn.isDisplayed() && btn.isEnabled()) {
                                String text = btn.getText().trim();
                                if ("Upload".equalsIgnoreCase(text) || "Confirm Upload".equalsIgnoreCase(text)
                                        || "Submit".equalsIgnoreCase(text) || "Save".equalsIgnoreCase(text)) {
                                    try {
                                        btn.click();
                                        System.out.println("[WorkOrderPage] Selenium-clicked: '" + text + "'");
                                    } catch (org.openqa.selenium.ElementClickInterceptedException e1) {
                                        new org.openqa.selenium.interactions.Actions(driver)
                                            .moveToElement(btn).click().perform();
                                        System.out.println("[WorkOrderPage] Actions-clicked: '" + text + "'");
                                    }
                                    uploaded = true;
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    if (uploaded) break;
                } catch (Exception e) {
                    System.out.println("[WorkOrderPage] Upload button attempt " + (attempt + 1) + ": " + e.getMessage());
                }
                pause(1000);
            }
            if (!uploaded) {
                System.out.println("[WorkOrderPage] No Upload button found — file may auto-upload on selection");
            }
            pause(3000);
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
                "// Count ALL visible img elements (excluding icons/logos which are tiny)\n" +
                "var allImgs = document.querySelectorAll('img');" +
                "var visible = 0;" +
                "for (var img of allImgs) {" +
                "  var r = img.getBoundingClientRect();" +
                "  var src = (img.src || img.getAttribute('src') || '').toLowerCase();" +
                "  // Skip tiny icons (< 40px) and SVG data URIs\n" +
                "  if (r.width > 40 && r.height > 40 && !src.startsWith('data:image/svg')) visible++;" +
                "}" +
                "// Also check for background-image divs\n" +
                "var divs = document.querySelectorAll('[class*=\"photo\"], [class*=\"Photo\"], [class*=\"thumbnail\"], [class*=\"image\"], [class*=\"Image\"], [class*=\"preview\"], [class*=\"Preview\"], [class*=\"gallery\"], [class*=\"Gallery\"]');" +
                "for (var d of divs) {" +
                "  var bg = window.getComputedStyle(d).backgroundImage;" +
                "  var r = d.getBoundingClientRect();" +
                "  if (bg && bg !== 'none' && r.width > 40 && r.height > 40) visible++;" +
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
     * Click "Add Tasks" via the Actions dropdown on the WO detail page.
     * UI: Actions button → dropdown menu → "+ Add Tasks"
     */
    public void clickAddTask() {
        // Step 1: Open the Actions dropdown
        Boolean actionsClicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Actions' || text.startsWith('Actions')) {" +
            "    b.scrollIntoView({block:'center'});" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;");
        System.out.println("[WorkOrderPage] Actions dropdown clicked: " + actionsClicked);
        pause(1000);

        // Step 2: Click "Add Tasks" menu item
        Boolean addTaskClicked = (Boolean) js.executeScript(
            "var items = document.querySelectorAll('li[role=\"menuitem\"], [class*=\"MuiMenuItem\"], [class*=\"menuItem\"]');" +
            "for (var item of items) {" +
            "  var text = item.textContent.trim();" +
            "  if (text.includes('Add Task')) {" +
            "    item.click(); return true;" +
            "  }" +
            "}" +
            "// Fallback: any clickable element in a popover/menu\n" +
            "var allClickable = document.querySelectorAll('[role=\"menu\"] li, [class*=\"MuiPopover\"] li, [class*=\"MuiMenu\"] li');" +
            "for (var c of allClickable) {" +
            "  if (c.textContent.trim().includes('Add Task')) { c.click(); return true; }" +
            "}" +
            "return false;");
        System.out.println("[WorkOrderPage] Add Tasks menu item clicked: " + addTaskClicked);
        pause(2000);

        // Diagnostic: what opened after clicking Add Tasks?
        if (Boolean.TRUE.equals(addTaskClicked)) {
            String diag = (String) js.executeScript(
                "var containers = document.querySelectorAll(" +
                "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]'" +
                ");" +
                "var info = 'Forms: ';" +
                "for (var d of containers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 200) {" +
                "    var inputs = d.querySelectorAll('input:not([type=\"hidden\"])');" +
                "    var btns = d.querySelectorAll('button');" +
                "    var btnTexts = [];" +
                "    for (var b of btns) { var t = b.textContent.trim(); if (t.length > 0 && t.length < 25) btnTexts.push(t); }" +
                "    info += '{w=' + Math.round(r.width) + ' inputs=' + inputs.length + ' btns=[' + btnTexts.join(',') + ']} ';" +
                "  }" +
                "}" +
                "return info;");
            System.out.println("[WorkOrderPage] Add Task form diagnostic: " + diag);
        }
    }

    /**
     * Fill the task title field in the Create Task form.
     * Targets the input with placeholder "Enter task title".
     */
    public void fillTaskName(String taskName) {
        try {
            // Target the exact "Enter task title" input inside the form
            Boolean filled = (Boolean) js.executeScript(
                "var containers = document.querySelectorAll(" +
                "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]'" +
                ");" +
                "for (var d of containers) {" +
                "  var r = d.getBoundingClientRect();" +
                "  if (r.width > 400) {" +
                "    var inputs = d.querySelectorAll('input');" +
                "    for (var inp of inputs) {" +
                "      var ph = (inp.placeholder || '').toLowerCase();" +
                "      if (ph.includes('task title') || ph.includes('task name') || ph.includes('enter task')) {" +
                "        inp.scrollIntoView({block:'center'});" +
                "        inp.focus(); inp.click();" +
                "        var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "        setter.call(inp, arguments[0]);" +
                "        inp.dispatchEvent(new Event('input', {bubbles: true}));" +
                "        inp.dispatchEvent(new Event('change', {bubbles: true}));" +
                "        return true;" +
                "      }" +
                "    }" +
                "  }" +
                "}" +
                "return false;",
                taskName);

            if (!Boolean.TRUE.equals(filled)) {
                // Fallback: use Selenium to find and fill the input
                WebElement titleInput = driver.findElement(
                    By.xpath("//input[@placeholder='Enter task title' or @placeholder='Task title' or @placeholder='Task Name']"));
                titleInput.clear();
                titleInput.sendKeys(taskName);
                filled = true;
            }

            System.out.println("[WorkOrderPage] Filled task title: " + taskName + " (success=" + filled + ")");
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Fill task name failed: " + e.getMessage());
        }
    }

    /**
     * Submit the task form.
     */
    public void submitTask() {
        // Diagnostic: log all inputs and their values before submit
        String formState = (String) js.executeScript(
            "var containers = document.querySelectorAll(" +
            "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]'" +
            ");" +
            "for (var d of containers) {" +
            "  var r = d.getBoundingClientRect();" +
            "  if (r.width > 400) {" +
            "    var info = 'INPUTS: ';" +
            "    var inputs = d.querySelectorAll('input:not([type=\"hidden\"]):not([type=\"file\"])');" +
            "    for (var inp of inputs) {" +
            "      var ir = inp.getBoundingClientRect();" +
            "      if (ir.width > 30) {" +
            "        var label = '';" +
            "        var fc = inp.closest('.MuiFormControl-root');" +
            "        if (fc) { var lbl = fc.querySelector('label'); if (lbl) label = lbl.textContent.trim(); }" +
            "        info += '{' + label + ' val=\"' + (inp.value||'').substring(0,30) + '\" ph=\"' + (inp.placeholder||'').substring(0,20) + '\"} ';" +
            "      }" +
            "    }" +
            "    var createBtn = null;" +
            "    var btns = d.querySelectorAll('button');" +
            "    for (var b of btns) {" +
            "      if (b.textContent.trim() === 'Create Task') {" +
            "        info += ' CreateTask: disabled=' + b.disabled;" +
            "        createBtn = b;" +
            "      }" +
            "    }" +
            "    return info;" +
            "  }" +
            "}" +
            "return 'NO_FORM';");
        System.out.println("[WorkOrderPage] Task form state before submit: " + formState);

        // Click Create Task button
        String result = (String) js.executeScript(
            "var containers = document.querySelectorAll(" +
            "  '[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]'" +
            ");" +
            "for (var d of containers) {" +
            "  var r = d.getBoundingClientRect();" +
            "  if (r.width > 400) {" +
            "    var btns = d.querySelectorAll('button');" +
            "    for (var b of btns) {" +
            "      var text = b.textContent.trim();" +
            "      if (text === 'Create Task' || text === 'Add Task' || text === 'Create' || text === 'Save') {" +
            "        b.scrollIntoView({block:'center'});" +
            "        b.click();" +
            "        return 'CLICKED:' + text + ':disabled=' + b.disabled;" +
            "      }" +
            "    }" +
            "  }" +
            "}" +
            "return 'NOT_CLICKED';");
        System.out.println("[WorkOrderPage] Task submit result: " + result);
        pause(3000);
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

    /**
     * Clear and type text into any element (input, textarea, or contentEditable).
     */
    private void editElementText(WebElement el, String text) {
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", el);
            pause(300);
            Actions actions = new Actions(driver);
            actions.moveToElement(el).click().perform();
            pause(200);
            // Select all + delete (use platform-appropriate key)
            String os = System.getProperty("os.name", "").toLowerCase();
            Keys modifier = os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;
            actions.keyDown(modifier).sendKeys("a").keyUp(modifier).perform();
            pause(100);
            actions.sendKeys(Keys.DELETE).perform();
            pause(100);
            actions.sendKeys(text).perform();
            pause(300);
        } catch (Exception e) {
            System.out.println("[WorkOrderPage] Actions failed, using JS fallback: " + e.getMessage());
            try {
                String tag = el.getTagName().toUpperCase();
                if (tag.equals("DIV") || tag.equals("SPAN") || tag.equals("P")) {
                    // contentEditable
                    js.executeScript(
                        "var el = arguments[0]; el.focus(); el.textContent = arguments[1];" +
                        "el.dispatchEvent(new Event('input', {bubbles: true}));",
                        el, text);
                } else {
                    String proto = tag.equals("TEXTAREA") ? "HTMLTextAreaElement" : "HTMLInputElement";
                    js.executeScript(
                        "var el = arguments[0]; var val = arguments[1];" +
                        "el.focus(); el.click();" +
                        "var setter = Object.getOwnPropertyDescriptor(window." + proto + ".prototype, 'value').set;" +
                        "setter.call(el, val);" +
                        "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles: true}));",
                        el, text);
                }
            } catch (Exception e2) {
                System.out.println("[WorkOrderPage] JS fallback also failed: " + e2.getMessage());
            }
        }
    }

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
