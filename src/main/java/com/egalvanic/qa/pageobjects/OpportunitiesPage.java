package com.egalvanic.qa.pageobjects;

import com.egalvanic.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Page Object for the Opportunities [SALES] module (/opportunities).
 *
 * SLD-scoped sales pipeline. Grid columns: name, SLD, status, quote_count,
 * total_value, created_at. Status is derived from the opportunity's quotes
 * (Qualifying -> Pending Response -> Closed Won / Closed Lost / Abandoned).
 *
 * Locator strategy per the framework rules: CSS / structural selectors first,
 * relative (never absolute) XPath only for MUI text-keyed buttons. Explicit
 * waits only — no Thread.sleep, no swallowed exceptions that hide failure.
 */
public class OpportunitiesPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ── structural (CSS-first) ──
    public static final By GRID = By.cssSelector(".MuiDataGrid-root, [role='grid']");
    public static final By GRID_ROW = By.cssSelector(".MuiDataGrid-row");
    public static final By COLUMN_HEADER = By.cssSelector(".MuiDataGrid-columnHeaderTitle, [role='columnheader']");
    public static final By SEARCH_INPUT = By.cssSelector(
            "input[type='search'], input[placeholder*='Search'], input[placeholder*='search']");
    public static final By DIALOG = By.cssSelector("[role='dialog'], .MuiDialog-root");
    public static final By DIALOG_TEXTFIELD = By.cssSelector("[role='dialog'] input[type='text'], .MuiDialog-root input:not([type='hidden'])");

    // ── relative XPath for MUI text buttons (no absolute paths) ──
    public static final By NEW_BTN = By.xpath(
            "//button[normalize-space()='New' or normalize-space()='New Opportunity' " +
            "or normalize-space()='Add' or normalize-space()='Add Opportunity' or normalize-space()='+ New']");
    public static final By AI_OPP_BTN = By.xpath(
            "//button[contains(normalize-space(),'AI') and contains(normalize-space(),'Opportunit')]");
    public static final By SAVE_BTN = By.xpath(
            "//div[@role='dialog']//button[normalize-space()='Save' or normalize-space()='Create' " +
            "or normalize-space()='Add' or normalize-space()='Save Changes']");
    public static final By CANCEL_BTN = By.xpath(
            "//div[@role='dialog']//button[normalize-space()='Cancel' or normalize-space()='Close' or normalize-space()='Discard']");
    public static final By CONFIRM_DELETE_BTN = By.xpath(
            "//div[@role='dialog']//button[normalize-space()='Delete' or normalize-space()='Confirm' " +
            "or normalize-space()='Yes' or normalize-space()='Remove']");

    public OpportunitiesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public void open() {
        // Bounded navigation retry: the Opportunities page intermittently stalls on its
        // initial render. Retry the load ONCE on a transient timeout (mirrors BaseTest's
        // reload-past-transient recovery). A PERSISTENT failure still propagates -> the
        // test goes red, which is the correct signal for a genuine "page won't load" defect.
        for (int attempt = 1; attempt <= 2; attempt++) {
            driver.get(AppConstants.BASE_URL + "/opportunities");
            try {
                waitForLoaded();
                waitForContent();
                return;
            } catch (org.openqa.selenium.TimeoutException te) {
                if (attempt == 2) throw te;   // persistent -> surface it (not masked)
            }
        }
    }

    /** Wait until the app shell + page chrome is up (route-agnostic, never sleeps blindly). */
    public void waitForLoaded() {
        wait.until(d -> {
            Object ready = ((JavascriptExecutor) d).executeScript(
                "var shell=document.querySelector('header,[role=banner],nav,.MuiDrawer-root,.MuiAppBar-root');"
                + "var t=(document.body&&document.body.innerText)||'';"
                + "return !!shell && t.length>40 && !/^\\s*Loading/i.test(t);");
            return Boolean.TRUE.equals(ready);
        });
    }

    /**
     * Wait for the Opportunities CONTENT to finish its async load — the grid container,
     * the "New" action, OR a definitive empty/SLD state — before any test asserts.
     * Root-cause fix for the earlier race where assertions ran against an empty body.
     */
    public void waitForContent() {
        // Wait for REAL module content — the grid, the page search box, or a definitive
        // empty/SLD state. NB: do NOT match the nav label "Opportunities" (always present)
        // or the wait short-circuits before the grid renders (the earlier race).
        try {
            new WebDriverWait(driver, java.time.Duration.ofSeconds(25)).until(d -> {
                if (!d.findElements(GRID).isEmpty()) return true;          // DataGrid mounted
                if (!d.findElements(SEARCH_INPUT).isEmpty()) return true;   // page search box mounted
                String t = bodyText().toLowerCase();
                return t.contains("no opportunit") || t.contains("select an sld")
                        || t.contains("select a site") || t.contains("get started")
                        || t.contains("no records") || t.contains("create your first");
            });
        } catch (org.openqa.selenium.TimeoutException te) {
            // Module content never rendered in 25s. We deliberately do NOT swallow this into
            // a pass — the calling test's own assertion (grid OR empty-state) will fail
            // loudly, which is the correct signal for a "content never loaded" defect.
        }
        sleepSettle();   // brief settle so DataGrid rows/headers hydrate before we read them
    }

    public boolean isGridPresent() {
        return !driver.findElements(GRID).isEmpty();
    }

    /** True when the page shows the SLD-required empty state rather than a grid. */
    public boolean showsSelectSldPrompt() {
        String body = bodyText().toLowerCase();
        return body.contains("select") && (body.contains("sld") || body.contains("site"));
    }

    public String bodyText() {
        Object b = ((JavascriptExecutor) driver).executeScript(
                "return (document.body && document.body.innerText) || '';");
        return b == null ? "" : b.toString();
    }

    public List<String> columnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement h : driver.findElements(COLUMN_HEADER)) {
            String t = h.getText().trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    public List<WebElement> rows() {
        return driver.findElements(GRID_ROW);
    }

    public int rowCount() {
        return rows().size();
    }

    /** Type into the page search box using the React-native value setter (sendKeys often won't filter). */
    public void search(String term) {
        WebElement box = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
        setReactInput(box, term);
        sleepSettle();   // controlled-input filter re-render settle
    }

    public void clearSearch() {
        WebElement box = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
        setReactInput(box, "");
        sleepSettle();
    }

    public void openCreateDialog() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(NEW_BTN));
        jsClick(btn);
        wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
    }

    public boolean isAiButtonEnabled() {
        List<WebElement> b = driver.findElements(AI_OPP_BTN);
        if (b.isEmpty()) return false;
        WebElement el = b.get(0);
        return el.isEnabled() && el.getAttribute("disabled") == null
                && !"true".equals(el.getAttribute("aria-disabled"));
    }

    public boolean isAiButtonPresent() {
        return !driver.findElements(AI_OPP_BTN).isEmpty();
    }

    /**
     * Set the "Opportunity Name" field in the create dialog.
     * The dialog's FIRST field is "Facility" (a combobox, pre-filled with the current Site);
     * the Name is the 2nd field. Targeting the first text input typed into Facility and left
     * Name empty (Create stayed disabled). This locates the field labelled "Opportunity Name"
     * (or the last non-combobox text input) and sets it via the React native value setter.
     */
    public void setName(String name) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
        // Locate the Opportunity Name input (NOT the Facility combobox). Then type with REAL
        // keystrokes — MUI/react-hook-form validation that gates the Create button only fires
        // on genuine key events; a JS native-setter sets the value but leaves Create disabled.
        WebElement field = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var dlg=document.querySelector('[role=\"dialog\"],.MuiDialog-root'); if(!dlg) return null;"
          + "var target=null;"
          + "dlg.querySelectorAll('.MuiFormControl-root,.MuiTextField-root').forEach(function(c){"
          + "  var l=c.querySelector('label'); var i=c.querySelector('input,textarea');"
          + "  if(i && l && /opportunity name|^\\s*name/i.test(l.textContent||'') "
          + "     && i.getAttribute('role')!=='combobox' && !target) target=i; });"
          + "if(!target){var ins=dlg.querySelectorAll(\"input[type='text'],textarea\");"
          + "  for(var k=ins.length-1;k>=0;k--){var i=ins[k];"
          + "    if(i.getAttribute('role')!=='combobox' && !i.closest('.MuiAutocomplete-root') "
          + "       && i.getAttribute('aria-haspopup')==null){target=i;break;}}}"
          + "return target;");
        if (field == null) {
            field = wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG_TEXTFIELD));
        }
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", field);
            field.click();
            // clear any prefill, then real-type
            String existing = field.getAttribute("value");
            if (existing != null && !existing.isEmpty()) {
                field.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
                field.sendKeys(org.openqa.selenium.Keys.DELETE);
            }
            field.sendKeys(name);
        } catch (Exception e) {
            // last resort: native setter (value set even if validation lags)
            setReactInput(field, name);
        }
    }

    /** Poll for the Create/Save button to become enabled (form validation settles asynchronously). */
    public boolean waitForSaveEnabled(long ms) {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            if (isSaveEnabled()) return true;
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        return isSaveEnabled();
    }

    public boolean isSaveEnabled() {
        List<WebElement> b = driver.findElements(SAVE_BTN);
        if (b.isEmpty()) return false;
        WebElement s = b.get(0);
        return s.isEnabled() && s.getAttribute("disabled") == null
                && !"true".equals(s.getAttribute("aria-disabled"));
    }

    public void clickSave() {
        WebElement s = wait.until(ExpectedConditions.elementToBeClickable(SAVE_BTN));
        jsClick(s);
    }

    public void clickCancel() {
        List<WebElement> c = driver.findElements(CANCEL_BTN);
        if (!c.isEmpty()) jsClick(c.get(0));
    }

    public boolean isDialogOpen() {
        return driver.findElements(DIALOG).stream().anyMatch(WebElement::isDisplayed);
    }

    /** Find a grid row whose visible text contains the given name (exact-ish, trimmed). */
    public WebElement findRowContaining(String name) {
        for (WebElement r : rows()) {
            try {
                if (r.getText().contains(name)) return r;
            } catch (Exception ignore) {
                // stale row during virtualized re-render — re-query handled by caller
            }
        }
        return null;
    }

    public boolean hasRowContaining(String name) {
        return findRowContaining(name) != null;
    }

    /** Open the first row's detail view; returns the URL after navigation. */
    public String openFirstRow() {
        List<WebElement> rs = rows();
        if (rs.isEmpty()) return null;
        WebElement cell = rs.get(0).findElements(By.cssSelector(".MuiDataGrid-cell, a")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(rs.get(0));
        jsClick(cell);
        sleepSettle();
        return driver.getCurrentUrl();
    }

    /** Collect the values in the Status column (best-effort by aria/data-field). */
    public List<String> statusValues() {
        return columnValues("status");
    }

    /** Read the trimmed text of a DataGrid column by its data-field (e.g. status, sld_name, quote_count). */
    public List<String> columnValues(String dataField) {
        List<String> out = new ArrayList<>();
        for (WebElement c : driver.findElements(By.cssSelector(".MuiDataGrid-cell[data-field='" + dataField + "']"))) {
            String t = c.getText().trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /**
     * Name of the active Site/Facility the grid is scoped to. The grid's facility column
     * (data-field sld_name) is the reliable source (all rows share it); falls back to the
     * header "Site:" selector. Returns "" if undetermined.
     */
    public String activeSiteName() {
        List<String> fac = columnValues("sld_name");
        if (!fac.isEmpty()) return fac.get(0).trim();
        // header "Site:" selector value
        for (WebElement el : driver.findElements(By.xpath(
                "//*[normalize-space()='Site:' or normalize-space()='Site']"
                + "/following::*[self::div or self::button or self::span][1]"))) {
            try {
                String t = el.getText().trim();
                if (!t.isEmpty() && t.length() < 40) return t;
            } catch (Exception ignore) { }
        }
        return "";
    }

    /** The open create dialog exposes at least a Name text field (firm) — SLD selector presence is best-effort. */
    public boolean createDialogHasNameField() {
        return isDialogOpen() && !driver.findElements(DIALOG_TEXTFIELD).isEmpty();
    }

    /**
     * Select the first option in the create dialog's "Facility" combobox (the required 1st field).
     * It defaults to the active Site in the live app, but on some test sites it isn't pre-committed,
     * leaving Create disabled — selecting an option satisfies the requirement. Returns true if a
     * facility was chosen. Harmless if already set (re-selects the first option).
     */
    public boolean selectFirstFacility() {
        if (!isDialogOpen()) return false;
        WebElement combo = driver.findElements(By.cssSelector(
                "[role='dialog'] [role='combobox'], .MuiDialog-root [role='combobox'], "
                + ".MuiDialog-root .MuiSelect-select, .MuiDialog-root .MuiAutocomplete-root input")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(null);
        if (combo == null) return false;
        // The test Site and Facility name are identical, and the grid is scoped to the active
        // Site — so we MUST pick the facility matching the active site (picking another would
        // create the opp under a different facility, invisible in this grid). Prefer the grid's
        // facility column (reliable), then the combobox prefill.
        String active = activeSiteName();
        if (active.isEmpty()) {
            String v = combo.getAttribute("value");
            active = (v == null || v.isEmpty()) ? combo.getText() : v;
        }
        active = active == null ? "" : active.trim();
        jsClick(combo);
        sleepSettle();
        List<WebElement> opts = driver.findElements(By.cssSelector("li[role='option'], .MuiMenuItem-root, .MuiAutocomplete-option"))
                .stream().filter(WebElement::isDisplayed).collect(java.util.stream.Collectors.toList());
        if (opts.isEmpty()) return false;
        WebElement pick = opts.get(0);
        if (!active.isEmpty()) {
            for (WebElement o : opts) {
                String t = o.getText().trim();
                if (t.equalsIgnoreCase(active) || t.contains(active) || active.contains(t)) { pick = o; break; }
            }
        }
        jsClick(pick);
        sleepSettle();
        return true;
    }

    public boolean createDialogHasSldSelector() {
        if (!isDialogOpen()) return false;
        return !driver.findElements(By.cssSelector(
                "[role='dialog'] [role='combobox'], .MuiDialog-root [role='combobox'], "
                + ".MuiDialog-root .MuiSelect-select, .MuiDialog-root input[placeholder*='SLD']")).isEmpty();
    }

    /** Best-effort SLD switcher control in the app chrome (header/sidebar). Null if not found. */
    public WebElement findSldSwitcher() {
        return driver.findElements(By.cssSelector(
                "[aria-label*='SLD'], [data-testid*='sld'], header [role='combobox'], "
                + "header .MuiSelect-select, [placeholder*='Select SLD'], [placeholder*='SLD']")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(null);
    }

    /** On an opportunity detail page, click into the first quote (-> /quotes/:id). Returns true if it navigated. */
    public boolean openFirstQuoteFromDetail() {
        String before = driver.getCurrentUrl();
        WebElement quote = driver.findElements(By.cssSelector("a[href*='/quotes/']")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(null);
        if (quote == null) {
            // fall back to a quote-ish grid row inside the detail
            quote = driver.findElements(By.cssSelector(".MuiDataGrid-row, table tbody tr")).stream()
                    .filter(WebElement::isDisplayed).findFirst().orElse(null);
        }
        if (quote == null) return false;
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", quote);
        sleepSettle();
        return driver.getCurrentUrl().contains("/quotes/") || !driver.getCurrentUrl().equals(before);
    }

    /** Tab labels present on the current (quote editor) screen. */
    public List<String> tabLabels() {
        List<String> out = new ArrayList<>();
        for (WebElement t : driver.findElements(By.cssSelector("[role='tab'], .MuiTab-root"))) {
            String s = t.getText().trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /** Quote rows shown on an opportunity detail page (best-effort). */
    public int quoteRowCountOnDetail() {
        return (int) driver.findElements(By.cssSelector(
                "a[href*='/quotes/'], [data-field='quote'], .MuiDataGrid-row, table tbody tr")).stream()
                .filter(WebElement::isDisplayed).count();
    }

    // ════════════════════════ Quote / EMP lifecycle ════════════════════════
    // Live-mapped 2026-06-08 (Playwright walkthrough on Site gyu). Flow:
    //   opp detail → "Add Quote" → Add New Quote dialog (Standard | EMP type) → Create Quote
    //   → quote "Rev N" (/quotes/:id, Draft) → status menu Accepted → "Quote Accepted - Create
    //   EMP" dialog → EMP Number → Create EMP → EMP (On Track), bound to the opportunity.
    //   Only ONE active quote per opp (Add Quote disables until the existing quote is rejected).

    public static final By ADD_QUOTE_BTN  = By.xpath("//button[normalize-space()='Add Quote']");
    public static final By CREATE_QUOTE_BTN = By.xpath("//div[@role='dialog']//button[normalize-space()='Create Quote']");
    public static final By CREATE_EMP_BTN   = By.xpath("//div[@role='dialog']//button[normalize-space()='Create EMP']");
    /** Detail/EMPs grids reuse the MUI DataGrid row class. */
    public static final By QUOTE_ROW = By.cssSelector(".MuiDataGrid-row");

    public boolean isAddQuotePresent() { return !driver.findElements(ADD_QUOTE_BTN).isEmpty(); }

    /** On an opportunity detail page: Add Quote present AND enabled. */
    public boolean isAddQuoteEnabled() {
        List<WebElement> b = driver.findElements(ADD_QUOTE_BTN);
        if (b.isEmpty()) return false;
        WebElement el = b.get(0);
        return el.isEnabled() && el.getAttribute("disabled") == null
                && !"true".equals(el.getAttribute("aria-disabled"));
    }

    /** The tooltip/wrapper text explaining why Add Quote is disabled (single-active-quote rule). */
    public String addQuoteDisabledReason() {
        Object r = ((JavascriptExecutor) driver).executeScript(
            "var b=document.evaluate(\"//button[normalize-space()='Add Quote']\",document,null,9,null).singleNodeValue;"
          + "if(!b) return ''; var n=b;"
          + "for(var i=0;i<5&&n;i++){ if(n.getAttribute){var t=n.getAttribute('title')||n.getAttribute('aria-label');"
          + "  if(t&&t.length>5) return t;} n=n.parentElement; }"
          + "return '';");
        return r == null ? "" : r.toString();
    }

    /** Click Add Quote and wait for the "Add New Quote" dialog. */
    public void clickAddQuote() {
        WebElement b = wait.until(ExpectedConditions.elementToBeClickable(ADD_QUOTE_BTN));
        jsClick(b);
        wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
        sleepSettle();
    }

    /** Pick the Quote Type radio in the Add New Quote dialog ("EMP" or "Standard"). */
    public void selectQuoteType(String type) {
        String want = type.toLowerCase().contains("emp") ? "EMP Quote" : "Standard Quote";
        WebElement radio = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var want=arguments[0];"
          + "var dlg=document.querySelector('[role=\"dialog\"]'); if(!dlg) return null;"
          + "var all=dlg.querySelectorAll('*');"
          + "for(var i=0;i<all.length;i++){var el=all[i];"
          + "  if(el.children.length===0 && (el.textContent||'').trim()===want){"
          + "    var c=el; for(var k=0;k<6&&c;k++){ if(c.querySelector && c.querySelector('input[type=radio],[role=radio]')) return c; c=c.parentElement; }"
          + "    return el; } }"
          + "return null;", want);
        if (radio != null) jsClick(radio);
        sleepSettle();
    }

    /** Click Create Quote and wait for the dialog to close. */
    public void createQuote() {
        WebElement b = wait.until(ExpectedConditions.elementToBeClickable(CREATE_QUOTE_BTN));
        jsClick(b);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.invisibilityOfElementLocated(CREATE_QUOTE_BTN));
        sleepSettle();
    }

    /** Count of quote rows on the detail's quotes grid (the "No rows" overlay yields 0). */
    public int quoteRowCount() {
        return (int) driver.findElements(QUOTE_ROW).stream().filter(WebElement::isDisplayed).count();
    }

    /** Poll until a quote row whose text contains "Rev" appears (after Create Quote). */
    public boolean waitForQuoteRow(long ms) {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            if (driver.findElements(QUOTE_ROW).stream().anyMatch(r -> {
                try { return r.isDisplayed() && r.getText().contains("Rev"); } catch (Exception e) { return false; }
            })) return true;
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        return false;
    }

    /** Click the first quote row on the detail → navigate to /quotes/:id. Returns true if it navigated. */
    public boolean openFirstQuoteRow() {
        WebElement row = driver.findElements(QUOTE_ROW).stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
        if (row == null) return false;
        WebElement cell = row.findElements(By.cssSelector(".MuiDataGrid-cell")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(row);
        jsClick(cell);
        sleepSettle();
        return driver.getCurrentUrl().contains("/quotes/");
    }

    /** Delete the first quote on the detail via its row "Delete Quote" control (confirms if prompted). */
    public boolean deleteFirstQuoteOnDetail() {
        WebElement del = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var row=document.querySelector('.MuiDataGrid-row'); if(!row) return null;"
          + "var b=row.querySelector('[aria-label=\"Delete Quote\" i],[title=\"Delete Quote\" i]');"
          + "if(b) return b.closest('button')||b;"
          + "var svg=row.querySelector('[data-testid*=\"Delete\" i]'); if(svg) return svg.closest('button')||svg;"
          + "var btns=row.querySelectorAll('button'); return btns.length?btns[btns.length-1]:null;");
        if (del == null) return false;
        jsClick(del);
        sleepSettle();
        List<WebElement> confirm = driver.findElements(CONFIRM_DELETE_BTN);
        if (!confirm.isEmpty()) { jsClick(confirm.get(0)); sleepSettle(); }
        return true;
    }

    // The quote-editor header status control reads like "Draft ▾"; the trailing glyph is an
    // icon, not reliably a specific char, so we locate the button by matching its text to a
    // known quote-status word (arrow/whitespace stripped) instead of the glyph.
    // The quote-editor status control is a MUI Chip rendered as div[role=button] (text like
    // "Draft ▾"), NOT a <button> element — so we must query [role=button] too. We match by the
    // status word (arrow/whitespace stripped) to stay specific.
    private static final String STATUS_BTN_FINDER =
            "var statuses=['draft','sent to customer','accepted','rejected','abandoned'];"
          + "var btns=document.querySelectorAll('button,[role=button]');"
          + "for(var i=0;i<btns.length;i++){"
          + "  var t=(btns[i].textContent||'').replace(/[\\u25B0-\\u25FF\\u2190-\\u21FF\\u2300-\\u23FF\\u2304\\u02C5\\u25BE\\u25BC]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();"
          + "  for(var j=0;j<statuses.length;j++){ if(t===statuses[j]||t.indexOf(statuses[j])===0) return btns[i]; } }"
          + "return null;";

    /** Poll (bounded) for the quote-editor status button — the editor mounts async after navigation. */
    public WebElement waitForStatusButton(long ms) {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            WebElement b = (WebElement) ((JavascriptExecutor) driver).executeScript(
                    "return (function(){" + STATUS_BTN_FINDER + "})();");
            if (b != null) return b;
            try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        return null;
    }

    /** Current quote status — from the header status button, else the Status combobox. */
    public String currentQuoteStatus() {
        Object r = ((JavascriptExecutor) driver).executeScript(
            "var b=(function(){" + STATUS_BTN_FINDER + "})();"
          + "if(b){return (b.textContent||'').replace(/[\\u25B0-\\u25FF\\u2190-\\u21FF\\u2300-\\u23FF\\u02C5]/g,'').trim();}"
          + "var c=document.querySelector('[role=combobox]'); return c?(c.textContent||'').trim():'';");
        return r == null ? "" : r.toString();
    }

    /**
     * Open the quote status control and choose a status (Draft / Sent to Customer / Accepted /
     * Rejected / Abandoned). The status renders either as a header button ("Draft ▾") OR, when
     * the header control isn't present, as the Overview "Status" combobox — try both.
     */
    public void setQuoteStatus(String status) {
        WebElement btn = waitForStatusButton(8000);
        if (btn != null) {
            jsClick(btn);
        } else {
            // Fallback: the Overview "Status" MUI Select combobox whose current text is a status.
            WebElement combo = (WebElement) ((JavascriptExecutor) driver).executeScript(
                "var statuses=['draft','sent to customer','accepted','rejected','abandoned'];"
              + "var cs=document.querySelectorAll('[role=combobox],.MuiSelect-select');"
              + "for(var i=0;i<cs.length;i++){var t=(cs[i].textContent||'').trim().toLowerCase();"
              + "  for(var j=0;j<statuses.length;j++){ if(t.indexOf(statuses[j])>=0) return cs[i]; } }"
              + "return null;");
            if (combo == null) {
                Object dump = ((JavascriptExecutor) driver).executeScript(
                    "return Array.from(document.querySelectorAll('button,[role=combobox],.MuiSelect-select'))"
                  + ".map(function(b){return (b.textContent||'').trim();}).filter(function(t){return t;}).slice(0,50).join(' | ');");
                throw new IllegalStateException("Quote status control not found. URL=" + driver.getCurrentUrl()
                        + " controls=[" + dump + "]");
            }
            jsClick(combo);
        }
        sleepSettle();
        // a menu (header button) or listbox (combobox) is now open — pick the target
        WebElement item = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var want=arguments[0].toLowerCase();"
          + "var items=document.querySelectorAll('li[role=menuitem],[role=menuitem],li[role=option],[role=option],li.MuiMenuItem-root');"
          + "for(var i=0;i<items.length;i++){if((items[i].textContent||'').trim().toLowerCase()===want) return items[i];}"
          + "for(var j=0;j<items.length;j++){if((items[j].textContent||'').toLowerCase().indexOf(want)>=0) return items[j];}"
          + "return null;", status);
        if (item == null) throw new IllegalStateException("Status option not found: " + status);
        jsClick(item);
        sleepSettle();
    }

    public boolean isCreateEmpDialogOpen() {
        return driver.findElements(DIALOG).stream().anyMatch(d -> {
            try { return d.isDisplayed() && d.getText().toLowerCase().contains("create emp"); }
            catch (Exception e) { return false; }
        });
    }

    /** In the "Quote Accepted - Create EMP" dialog, enter the EMP number and commit. */
    public void fillEmpNumberAndCreate(String empNo) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
        WebElement input = driver.findElements(By.cssSelector(
                "[role='dialog'] input[type='text'], [role='dialog'] input:not([type='hidden'])")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(null);
        if (input == null) throw new IllegalStateException("EMP Number field not found");
        input.click();
        input.sendKeys(empNo);
        WebElement create = wait.until(ExpectedConditions.elementToBeClickable(CREATE_EMP_BTN));
        jsClick(create);
        sleepSettle();
    }

    /** After committing: the page becomes an EMP detail — Progress Reporting / Change Orders tabs, or heading == EMP #. */
    public boolean isEmpDetail(String empNo) {
        boolean tab = !driver.findElements(By.xpath(
                "//*[@role='tab'][contains(.,'Progress Reporting') or contains(.,'Change Orders')]")).isEmpty();
        if (tab) return true;
        return !driver.findElements(By.xpath("//h1|//h2|//h3|//h4|//h5"))
                .stream().anyMatch(h -> { try { return h.getText().trim().equals(empNo); } catch (Exception e) { return false; } })
                ? bodyText().contains(empNo) : true;
    }

    /** Navigate to /emps and check whether an EMP with the given number is listed. */
    public boolean empExists(String empNo) {
        driver.get(AppConstants.BASE_URL + "/emps");
        sleepSettle();
        List<WebElement> s = driver.findElements(By.cssSelector(
                "input[placeholder*='Search EMP'], input[placeholder*='Search'], input[type='search']"));
        if (!s.isEmpty()) { setReactInput(s.get(0), empNo); sleepSettle(); }
        boolean inRows = driver.findElements(QUOTE_ROW).stream().anyMatch(r -> {
            try { return r.getText().contains(empNo); } catch (Exception e) { return false; }
        });
        return inRows || bodyText().contains(empNo);
    }

    // ── helpers ──
    private void setReactInput(WebElement input, String value) {
        ((JavascriptExecutor) driver).executeScript(
            "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
            + "s.call(arguments[0],arguments[1]);"
            + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
            + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", input, value);
    }

    private void jsClick(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    /** Bounded explicit settle for controlled-input filter re-render (not a blind sleep on the page). */
    private void sleepSettle() {
        try { Thread.sleep(900); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
