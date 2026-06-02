package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page Object Model for the Work Order Planning page (route: /planning).
 *
 * IMPORTANT: This is a DISTINCT module from Work Orders (/sessions). A "Plan" is the
 * template/definition that rolls up into Work Orders. Verified live on QA 2026-06-02.
 *
 * UI structure (from live Playwright exploration, May/June 2026 web release):
 *   - Grid columns (data-field): title, quote_type, description, sld_name,
 *     created_date, total_hours, status, actions
 *   - Column header labels: Name, Type, Description, Facility, Created, Est. Hours, Status, Actions
 *   - Search input placeholder: "Search Work Order Planning..." (a hidden duplicate
 *     "Search" input also exists — always pick the VISIBLE one)
 *   - "Create New Plan" button opens a Dialog with: Plan Type* (combo), Facility* (combo),
 *     Plan Name* (text), Description (textarea); buttons Cancel / Create
 *   - Each row's Actions column has buttons title="Edit Plan" and title="Delete Plan"
 *   - Delete opens a confirmation dialog; pagination via .MuiTablePagination-displayedRows
 */
public class PlanningPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    public PlanningPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // ================================================================
    // LOCATORS
    // ================================================================

    public static final By GRID = By.cssSelector("[role='grid'], .MuiDataGrid-root");
    public static final By GRID_ROWS = By.cssSelector(".MuiDataGrid-row[data-rowindex]");
    public static final By COLUMN_HEADERS = By.cssSelector("[role='columnheader']");
    public static final By PAGINATION = By.cssSelector(".MuiTablePagination-displayedRows");

    // Search — prefer the planning-specific placeholder; the bare "Search" match is the
    // hidden duplicate the new web renders, so callers must filter to the VISIBLE one.
    public static final By SEARCH_INPUT = By.xpath(
            "//input[contains(@placeholder,'Search Work Order Planning')]"
            + " | //input[contains(@placeholder,'Search')]");

    public static final By CREATE_PLAN_BTN = By.xpath(
            "//button[contains(normalize-space(),'Create New Plan')"
            + " or contains(normalize-space(),'Create Plan')]");

    // Action buttons within a row
    public static final By EDIT_PLAN_BTN = By.cssSelector("button[title='Edit Plan']");
    public static final By DELETE_PLAN_BTN = By.cssSelector("button[title='Delete Plan']");

    // Dialog
    public static final By DIALOG = By.cssSelector("[role='dialog'], .MuiDialog-paper");
    public static final By DIALOG_CREATE_SUBMIT = By.xpath(
            "//div[contains(@class,'MuiDialog')]//button[normalize-space()='Create']");
    public static final By DIALOG_SAVE_SUBMIT = By.xpath(
            "//div[contains(@class,'MuiDialog')]//button[normalize-space()='Save'"
            + " or normalize-space()='Update' or normalize-space()='Save Changes']");
    public static final By DIALOG_CANCEL = By.xpath(
            "//div[contains(@class,'MuiDialog')]//button[normalize-space()='Cancel']");
    public static final By DIALOG_DELETE_CONFIRM = By.xpath(
            "//div[contains(@class,'MuiDialog')]//button[normalize-space()='Delete'"
            + " or normalize-space()='Confirm' or normalize-space()='Yes']");

    // Create-dialog field by its MUI label (Plan Name is a text input; Plan Type / Facility are combos)
    public static final By PLAN_NAME_INPUT = By.xpath(
            "//div[contains(@class,'MuiFormControl') or contains(@class,'MuiTextField')]"
            + "[.//label[contains(normalize-space(),'Plan Name')]]//input");
    public static final By DESCRIPTION_INPUT = By.xpath(
            "//div[contains(@class,'MuiFormControl') or contains(@class,'MuiTextField')]"
            + "[.//label[contains(normalize-space(),'Description')]]//textarea[not(@aria-hidden='true')]");

    // ================================================================
    // NAVIGATION + GRID READINESS
    // ================================================================

    /** Returns the first VISIBLE search input (the new web renders a hidden duplicate). */
    public WebElement visibleSearchInput() {
        for (WebElement el : driver.findElements(SEARCH_INPUT)) {
            try {
                if (el.isDisplayed()) return el;
            } catch (Exception ignored) {}
        }
        throw new org.openqa.selenium.NoSuchElementException("No visible search input on Planning page");
    }

    /** Two-phase wait: app shell hydrated, then the DataGrid is present. */
    public void waitForGrid() {
        try {
            // Phase 0: SPA shell — driver.get() triggers a full reload; wait out "Loading...".
            new WebDriverWait(driver, Duration.ofSeconds(35)).until(d -> {
                Object body = js.executeScript("return document.body && document.body.innerText || '';");
                String t = body == null ? "" : body.toString();
                return (t.contains("DASHBOARDS") || t.contains("Site Overview")) && !t.startsWith("Loading");
            });
            // Phase 1: grid container present
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> !d.findElements(GRID).isEmpty());
        } catch (Exception ignored) {
            // leave it to the caller's assertions to report
        }
    }

    public int rowCount() {
        try {
            return driver.findElements(GRID_ROWS).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Total plan count from the visible MuiTablePagination "X–Y of N" footer. */
    public int paginationTotal() {
        try {
            for (WebElement el : driver.findElements(PAGINATION)) {
                if (!el.isDisplayed()) continue;
                Matcher m = Pattern.compile("of\\s+([\\d,]+)").matcher(el.getText());
                if (m.find()) {
                    return Integer.parseInt(m.group(1).replaceAll("[^0-9]", ""));
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public String bodyText() {
        try {
            return driver.findElement(By.tagName("body")).getText();
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // SEARCH
    // ================================================================

    /** Type into the planning search; JS native-setter fallback if React re-mounts the input. */
    public void search(String term) {
        org.openqa.selenium.WebDriverException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement box = visibleSearchInput();
                box.click();
                box.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                box.sendKeys(Keys.DELETE);
                box = visibleSearchInput();
                box.sendKeys(term);
                pause(2500);
                return;
            } catch (org.openqa.selenium.ElementNotInteractableException
                    | org.openqa.selenium.StaleElementReferenceException e) {
                last = e;
                pause(500);
            }
        }
        // Fallback: native React-compatible value setter
        try {
            WebElement box = visibleSearchInput();
            js.executeScript(
                "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                + "s.call(arguments[0],'');arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                + "s.call(arguments[0],arguments[1]);arguments[0].dispatchEvent(new Event('input',{bubbles:true}));",
                box, term);
            pause(2500);
        } catch (Exception ignored) {
            if (last != null) throw last;
        }
    }

    public void clearSearch() {
        try {
            WebElement box = visibleSearchInput();
            box.click();
            box.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            box.sendKeys(Keys.DELETE);
            pause(2000);
        } catch (Exception e) {
            // JS fallback
            try {
                WebElement box = visibleSearchInput();
                js.executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],'');arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", box);
                pause(2000);
            } catch (Exception ignored) {}
        }
    }

    // ================================================================
    // CREATE
    // ================================================================

    public boolean openCreateDialog() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(CREATE_PLAN_BTN)).click();
        } catch (Exception e) {
            try { js.executeScript("arguments[0].click();", driver.findElement(CREATE_PLAN_BTN)); }
            catch (Exception ignored) { return false; }
        }
        return waitForDialog();
    }

    public boolean waitForDialog() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
            pause(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDialogOpen() {
        for (WebElement d : driver.findElements(DIALOG)) {
            try { if (d.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Fill the create form and submit. Plan Type + Facility are MUI combos — we pick the
     * first available option for each. Returns true if the dialog closed (created).
     */
    public boolean createPlan(String planName, String description) {
        if (!isDialogOpen() && !openCreateDialog()) return false;
        selectFirstComboOption("Plan Type");
        selectFirstComboOption("Facility");
        typeByLabel("Plan Name", planName);
        if (description != null && !description.isEmpty()) {
            try { typeByLabel("Description", description); } catch (Exception ignored) {}
        }
        return submitCreate();
    }

    public boolean submitCreate() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(DIALOG_CREATE_SUBMIT)).click();
        } catch (Exception e) {
            try { js.executeScript("arguments[0].click();", driver.findElement(DIALOG_CREATE_SUBMIT)); }
            catch (Exception ignored) { return false; }
        }
        return waitForDialogClose();
    }

    public void cancelDialog() {
        try {
            for (WebElement b : driver.findElements(DIALOG_CANCEL)) {
                if (b.isDisplayed()) { b.click(); pause(500); return; }
            }
        } catch (Exception ignored) {}
    }

    public boolean waitForDialogClose() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.invisibilityOfElementLocated(DIALOG));
            pause(1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // EDIT
    // ================================================================

    /** Open the Edit dialog for the row whose Name (title) matches; falls back to first row. */
    public boolean openEditForPlan(String planName) {
        WebElement row = findRowByName(planName);
        WebElement editBtn = null;
        if (row != null) {
            List<WebElement> btns = row.findElements(EDIT_PLAN_BTN);
            if (!btns.isEmpty()) editBtn = btns.get(0);
        }
        if (editBtn == null) {
            List<WebElement> any = driver.findElements(EDIT_PLAN_BTN);
            if (!any.isEmpty()) editBtn = any.get(0);
        }
        if (editBtn == null) return false;
        try { editBtn.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", editBtn); }
        return waitForDialog();
    }

    public boolean saveEdit() {
        // Edit dialog may use "Save"/"Update" or reuse "Create"
        for (By submit : new By[]{ DIALOG_SAVE_SUBMIT, DIALOG_CREATE_SUBMIT }) {
            List<WebElement> btns = driver.findElements(submit);
            for (WebElement b : btns) {
                if (b.isDisplayed() && b.isEnabled()) {
                    try { b.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", b); }
                    return waitForDialogClose();
                }
            }
        }
        return false;
    }

    // ================================================================
    // DELETE
    // ================================================================

    public boolean openDeleteForPlan(String planName) {
        WebElement row = findRowByName(planName);
        WebElement delBtn = null;
        if (row != null) {
            List<WebElement> btns = row.findElements(DELETE_PLAN_BTN);
            if (!btns.isEmpty()) delBtn = btns.get(0);
        }
        if (delBtn == null) {
            List<WebElement> any = driver.findElements(DELETE_PLAN_BTN);
            if (!any.isEmpty()) delBtn = any.get(0);
        }
        if (delBtn == null) return false;
        try { delBtn.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", delBtn); }
        return waitForDialog();
    }

    public boolean confirmDelete() {
        for (WebElement b : driver.findElements(DIALOG_DELETE_CONFIRM)) {
            if (b.isDisplayed() && b.isEnabled()) {
                try { b.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", b); }
                return waitForDialogClose();
            }
        }
        return false;
    }

    // ================================================================
    // ROW / CELL HELPERS
    // ================================================================

    /** Find a grid row whose title cell contains the given name (case-insensitive). */
    public WebElement findRowByName(String name) {
        if (name == null) return null;
        for (WebElement row : driver.findElements(GRID_ROWS)) {
            try {
                List<WebElement> titleCells = row.findElements(
                        By.cssSelector(".MuiDataGrid-cell[data-field='title']"));
                if (!titleCells.isEmpty()
                        && titleCells.get(0).getText().toLowerCase().contains(name.toLowerCase())) {
                    return row;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public boolean isPlanInGrid(String name) {
        return findRowByName(name) != null || bodyText().toLowerCase().contains(name.toLowerCase());
    }

    /** All Est. Hours (total_hours) cell texts in the current grid page. */
    public java.util.List<String> estHoursValues() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (WebElement cell : driver.findElements(
                By.cssSelector(".MuiDataGrid-cell[data-field='total_hours']"))) {
            try { out.add(cell.getText().trim()); } catch (Exception ignored) {}
        }
        return out;
    }

    /** Column header labels in display order. */
    public java.util.List<String> columnHeaders() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (WebElement h : driver.findElements(COLUMN_HEADERS)) {
            try {
                String t = h.getText().trim();
                if (!t.isEmpty()) out.add(t);
            } catch (Exception ignored) {}
        }
        return out;
    }

    // ================================================================
    // INTERNAL — MUI combo + text typing with React-safe fallbacks
    // ================================================================

    /** Type into a dialog text/textarea field located by its MUI label, re-finding to beat staleness. */
    private void typeByLabel(String label, String text) {
        By by = By.xpath(
                "//div[contains(@class,'MuiFormControl') or contains(@class,'MuiTextField')]"
                + "[.//label[contains(normalize-space(),'" + label + "')]]"
                + "//*[self::input or self::textarea][not(@aria-hidden='true')]");
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                el.click();
                el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                el.sendKeys(Keys.DELETE);
                el = driver.findElement(by);
                el.sendKeys(text);
                return;
            } catch (org.openqa.selenium.ElementNotInteractableException
                    | org.openqa.selenium.StaleElementReferenceException e) {
                pause(400);
            }
        }
        // JS native-setter fallback
        try {
            WebElement el = driver.findElement(by);
            String tag = el.getTagName();
            String proto = "textarea".equalsIgnoreCase(tag)
                    ? "window.HTMLTextAreaElement.prototype" : "window.HTMLInputElement.prototype";
            js.executeScript(
                "var s=Object.getOwnPropertyDescriptor(" + proto + ",'value').set;"
                + "s.call(arguments[0],'');arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                + "s.call(arguments[0],arguments[1]);"
                + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                el, text);
        } catch (Exception ignored) {}
    }

    /** Open a MUI combo by its label and click the first option in the popup. */
    private void selectFirstComboOption(String label) {
        try {
            By comboBy = By.xpath(
                    "//div[contains(@class,'MuiFormControl')]"
                    + "[.//label[contains(normalize-space(),'" + label + "')]]"
                    + "//div[@role='combobox' or contains(@class,'MuiSelect-select')]"
                    + " | //div[contains(@class,'MuiFormControl')]"
                    + "[.//label[contains(normalize-space(),'" + label + "')]]//input");
            WebElement combo = wait.until(ExpectedConditions.elementToBeClickable(comboBy));
            try { combo.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", combo); }
            pause(700);
            // Options render in a popup listbox
            List<WebElement> options = driver.findElements(By.cssSelector("li[role='option'], .MuiMenuItem-root"));
            for (WebElement opt : options) {
                if (opt.isDisplayed() && !opt.getText().trim().isEmpty()) {
                    try { opt.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", opt); }
                    pause(500);
                    return;
                }
            }
            // Nothing to pick — close the popup
            try { combo.sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}
        } catch (Exception ignored) {
            // Field may be optional or pre-filled
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
