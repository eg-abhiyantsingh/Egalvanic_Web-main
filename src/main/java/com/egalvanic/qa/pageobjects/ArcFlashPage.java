package com.egalvanic.qa.pageobjects;

import com.egalvanic.qa.constants.AppConstants;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Page Object for the <b>Arc Flash Readiness</b> module ({@code /arc-flash}).
 *
 * <p><b>Discovered live 2026-06-29 (site "Android Qa Site1"):</b> the dashboard shows three circular
 * progress indicators — <b>Asset Details</b>, <b>Source/Target Connections</b>, <b>Connection
 * Details</b> — and a tab strip with the same three plus <b>Overview</b>. An <b>Engineering Mode</b>
 * toggle (a MUI {@code <Switch>} in a FormControlLabel, top-right by the profile) reveals extra columns;
 * it persists across tab switches. On the <b>Asset Details</b> tab a filterable table (Label,
 * Interrupting Rating, Ampere Rating, Mains Type, Voltage, % Completion, …) sits below an <b>Asset
 * Class</b> filter — a MUI {@code <Select>} (default "ATS") whose options are the site's asset classes
 * (ATS, Battery, Busway, Circuit Breaker, Disconnect Switch, Fuse, Generator, Motor Starter, Relay,
 * Switchboard, Transformer, …). MUI Select opens its listbox only on a TRUSTED click, so selections are
 * driven with Selenium {@link Actions}.</p>
 */
public class ArcFlashPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;
    private static final int TIMEOUT = 30;

    private static final By ARC_FLASH_NAV = By.xpath(
            "//a[@href='/arc-flash' or @href='/arc-flash/']"
            + " | //span[normalize-space()='Arc Flash Readiness']"
            + " | //p[normalize-space()='Arc Flash Readiness']");

    // Tab strip (role=tab). The page renders responsive duplicates — we always act on the visible one.
    private static By tab(String name) {
        return By.xpath("//*[@role='tab'][normalize-space()='" + name + "']");
    }
    private static final By ACTIVE_TAB = By.xpath("//*[@role='tab'][@aria-selected='true']");

    // Engineering Mode = a MUI Switch inside a FormControlLabel that contains the text "Engineering Mode".
    private static final By ENG_MODE_INPUT = By.xpath(
            "//label[contains(@class,'MuiFormControlLabel-root')][contains(.,'Engineering Mode')]"
            + "//input[@role='switch' or @type='checkbox']");
    private static final By ENG_MODE_LABEL = By.xpath(
            "//label[contains(@class,'MuiFormControlLabel-root')][contains(.,'Engineering Mode')]");

    // Asset Class filter = a MUI Select in a FormControl whose label is "Asset Class".
    private static final By ASSET_CLASS_SELECT = By.xpath(
            "//div[contains(@class,'MuiFormControl-root')][./label[normalize-space()='Asset Class']]"
            + "//div[contains(@class,'MuiSelect-select')]");
    private static final By LISTBOX_OPTION = By.xpath("//ul[@role='listbox']//li[@role='option']");

    private static final By COLUMN_HEADERS = By.xpath("//*[@role='columnheader'] | //table//th");
    private static final By GRID_ROWS = By.xpath("//*[@role='row']");

    public ArcFlashPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // ── Navigation ──────────────────────────────────────────────

    /** Navigate to Arc Flash Readiness (SPA nav click; falls back to direct URL) and wait for the tabs. */
    public void navigateToArcFlash() {
        if (isOnArcFlash()) { waitLoaded(); return; }
        try {
            WebElement nav = wait.until(ExpectedConditions.elementToBeClickable(ARC_FLASH_NAV));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", nav);
            try { nav.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", nav); }
        } catch (Exception e) {
            driver.get(AppConstants.BASE_URL + "/arc-flash");
        }
        waitLoaded();
    }

    public boolean isOnArcFlash() {
        return driver.getCurrentUrl().contains("/arc-flash");
    }

    /** Wait until the Arc Flash tab strip (Asset Details tab) has rendered. */
    public void waitLoaded() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(45))
                    .until(ExpectedConditions.presenceOfElementLocated(tab("Asset Details")));
        } catch (Exception e) {
            System.out.println("[ArcFlashPage] Asset Details tab not present after 45s");
        }
        pause(600);
    }

    public boolean isLoaded() {
        return !driver.findElements(tab("Asset Details")).isEmpty();
    }

    // ── Engineering Mode toggle ─────────────────────────────────

    public boolean isEngineeringModeOn() {
        List<WebElement> els = driver.findElements(ENG_MODE_INPUT);
        if (els.isEmpty()) return false;
        try { return els.get(0).isSelected(); } catch (Exception e) { return false; }
    }

    public boolean hasEngineeringModeToggle() {
        return !driver.findElements(ENG_MODE_LABEL).isEmpty();
    }

    /** Set the Engineering Mode toggle to {on}; no-op if already in that state. Returns the resulting state. */
    public boolean setEngineeringMode(boolean on) {
        if (isEngineeringModeOn() == on) return on;
        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(ENG_MODE_INPUT));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        pause(150);
        // The label is the reliable click target for a MUI Switch; fall back to the input + JS.
        try {
            WebElement label = driver.findElement(ENG_MODE_LABEL);
            try { new Actions(driver).moveToElement(label).click().perform(); }
            catch (Exception e) { label.click(); }
        } catch (Exception e) {
            try { input.click(); } catch (Exception e2) { js.executeScript("arguments[0].click();", input); }
        }
        pause(700);
        boolean now = isEngineeringModeOn();
        if (now != on) { // one retry via JS click on the input
            try { js.executeScript("arguments[0].click();", driver.findElement(ENG_MODE_INPUT)); pause(700); } catch (Exception ignored) {}
            now = isEngineeringModeOn();
        }
        System.out.println("[ArcFlashPage] Engineering Mode set to " + on + " (now=" + now + ")");
        return now;
    }

    // ── Tabs ────────────────────────────────────────────────────

    /** Click a tab (Overview / Asset Details / Source/Target Connections / Connection Details). */
    public void clickTab(String name) {
        List<WebElement> tabs = driver.findElements(tab(name));
        WebElement visible = null;
        for (WebElement t : tabs) {
            try { if (t.getRect().getWidth() > 0 && t.isDisplayed()) { visible = t; break; } } catch (Exception ignored) {}
        }
        if (visible == null && !tabs.isEmpty()) visible = tabs.get(0);
        if (visible == null) throw new RuntimeException("Tab not found: " + name);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", visible);
        pause(150);
        try { visible.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", visible); }
        pause(1200);
        System.out.println("[ArcFlashPage] Clicked tab: " + name);
    }

    public String getActiveTab() {
        List<WebElement> els = driver.findElements(ACTIVE_TAB);
        return els.isEmpty() ? "" : els.get(0).getText().trim();
    }

    // ── Asset Class filter ──────────────────────────────────────

    /**
     * The Asset Class filter Select element that is actually VISIBLE. The page has several elements
     * whose text is "Asset Class" (the filter label + table headers + responsive duplicates), and some
     * matched Selects are hidden/non-interactable — clicking those throws ElementNotInteractable. We
     * use findElements (not a heal-prone findElement) and return the first displayed match.
     */
    private WebElement visibleAssetClassSelect() {
        List<WebElement> els = driver.findElements(ASSET_CLASS_SELECT);
        for (WebElement e : els) {
            try { if (e.isDisplayed() && e.getRect().getWidth() > 0) return e; } catch (Exception ignored) {}
        }
        return els.isEmpty() ? null : els.get(0);
    }

    public boolean hasAssetClassFilter() {
        WebElement e = visibleAssetClassSelect();
        try { return e != null && e.isDisplayed(); } catch (Exception ex) { return false; }
    }

    /**
     * Wait for a VISIBLE Asset Class filter to render after switching to the Asset Details tab. On a
     * slow (headless CI) host the tab's table + filter load async, so a point-in-time check can miss it.
     */
    public boolean waitForAssetClassFilter() {
        for (int i = 0; i < 45; i++) {
            if (hasAssetClassFilter()) return true;
            pause(1000);
        }
        System.out.println("[ArcFlashPage] visible Asset Class filter not present after 45s");
        return false;
    }

    public String getAssetClassValue() {
        WebElement e = visibleAssetClassSelect();
        return e == null ? "" : e.getText().trim();
    }

    /** Open the Asset Class MUI Select and wait for its listbox; retries with different click strategies
     *  (native → JS mousedown/up → Actions) against the VISIBLE select, since a stray hidden duplicate
     *  is not interactable. */
    private void openAssetClassDropdown() {
        if (!driver.findElements(LISTBOX_OPTION).isEmpty()) return; // already open
        for (int attempt = 0; attempt < 4; attempt++) {
            WebElement sel = visibleAssetClassSelect();
            if (sel == null) { pause(500); continue; }
            try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", sel); } catch (Exception ignored) {}
            pause(150);
            try {
                if (attempt == 0) {
                    sel.click();                                   // native: fires mousedown → opens MUI Select
                } else if (attempt == 1) {
                    new Actions(driver).moveToElement(sel).click().perform();
                } else {
                    js.executeScript(
                        "var el=arguments[0];"
                        + "['pointerdown','mousedown','mouseup','click'].forEach(function(t){"
                        + "  el.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,view:window}));});", sel);
                }
            } catch (Exception e) {
                System.out.println("[ArcFlashPage] open attempt " + attempt + " click failed: " + e.getMessage());
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.visibilityOfElementLocated(LISTBOX_OPTION));
            } catch (Exception ignored) {}
            if (!driver.findElements(LISTBOX_OPTION).isEmpty()) { pause(250); return; }
            pause(400);
        }
        System.out.println("[ArcFlashPage] WARNING: Asset Class listbox did not open after retries");
    }

    /** Open the Asset Class dropdown and return its option texts (menu closed afterwards). */
    public List<String> getAssetClassOptions() {
        List<String> out = new ArrayList<>();
        for (int attempt = 0; attempt < 2 && out.isEmpty(); attempt++) {
            openAssetClassDropdown();
            for (WebElement li : driver.findElements(LISTBOX_OPTION)) {
                String t = li.getText().trim();
                if (!t.isEmpty()) out.add(t);
            }
            if (out.isEmpty()) pause(600);
        }
        try { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); } catch (Exception ignored) {}
        pause(300);
        return out;
    }

    /** Open the Asset Class dropdown and select {className} (trusted click). */
    public void selectAssetClass(String className) {
        openAssetClassDropdown();
        By exact = By.xpath("//ul[@role='listbox']//li[@role='option'][normalize-space()='" + className + "']");
        for (int attempt = 0; attempt < 3; attempt++) {
            List<WebElement> opts = driver.findElements(exact);
            if (!opts.isEmpty()) {
                WebElement opt = opts.get(0);
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", opt);
                pause(150);
                try { new Actions(driver).moveToElement(opt).click().perform(); } catch (Exception e) { opt.click(); }
                pause(1200); // table re-filters
                System.out.println("[ArcFlashPage] Selected Asset Class: " + className);
                return;
            }
            pause(400);
        }
        throw new RuntimeException("Asset Class option not found: " + className);
    }

    // ── Table / dashboard ───────────────────────────────────────

    public List<String> getColumnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement h : driver.findElements(COLUMN_HEADERS)) {
            String t = h.getText().trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    public boolean hasColumn(String header) {
        for (String h : getColumnHeaders()) {
            if (h.equalsIgnoreCase(header) || h.toLowerCase().contains(header.toLowerCase())) return true;
        }
        return false;
    }

    /** Approximate data-row count (role=row minus the header row). */
    public int getRowCount() {
        int rows = driver.findElements(GRID_ROWS).size();
        return Math.max(0, rows - 1);
    }

    /** True if the Arc Flash dashboard shows the named circular progress indicator (e.g. "Asset Details"). */
    public boolean hasProgressIndicator(String label) {
        return !driver.findElements(By.xpath(
                "//*[normalize-space()='" + label + "']")).isEmpty();
    }

    // ════════════════════════════════════════════════════════════
    // SOURCE/TARGET CONNECTIONS + CONNECTION DETAILS tabs and their modals
    // ----------------------------------------------------------------
    // Source/Target Connections: a table (Asset, Asset Class, Needs Source, …) — clicking an asset row
    //   opens an "Edit Asset" modal (Asset Name*, Asset Class*, Location, Asset Photos tabs); close via
    //   the top-right X icon button.
    // Connection Details: a "Busway Arc Flash Readiness" section + a Busway connection table (Type,
    //   Source, Target, Conductor Material, Length (ft), Neutral Wire Size, Amperage of Busway, Phase
    //   A/B/C Wire Size). Clicking a connection row opens an "Edit Connection" modal with BASIC INFO
    //   (Source/Target Node, Connection Type) + CORE ATTRIBUTES (Conductor Material* Autocomplete:
    //   Aluminum/Copper, Length (ft)* number, …). Setting Conductor Material + Length + Save Changes
    //   persists (Save accepts partial required fields). Verified live 2026-06-29.
    // ════════════════════════════════════════════════════════════

    private static final By GRID_DATA_ROWS = By.xpath(
            "//*[@role='row'][.//*[@role='gridcell']] | //tbody//tr[td]");
    private static final By EDIT_ASSET_DIALOG = By.xpath("//div[@role='dialog'][.//*[normalize-space()='Edit Asset']]");
    private static final By EDIT_CONNECTION_DIALOG = By.xpath("//div[@role='dialog'][.//*[normalize-space()='Edit Connection']]");
    private static final By SAVE_CHANGES_BTN = By.xpath("//div[@role='dialog']//button[normalize-space()='Save Changes']");

    /** Visible, non-header data rows of whatever table is showing. */
    private List<WebElement> visibleDataRows() {
        List<WebElement> out = new ArrayList<>();
        for (WebElement r : driver.findElements(GRID_DATA_ROWS)) {
            try {
                if (!r.findElements(By.xpath(".//*[@role='columnheader']")).isEmpty()) continue;
                if (r.isDisplayed() && r.getRect().getWidth() > 0) out.add(r);
            } catch (Exception ignored) {}
        }
        return out;
    }

    /** Wait (up to ~45s) for the current table to render at least one data row. */
    public boolean waitForTableRows() {
        for (int i = 0; i < 60; i++) {
            if (!visibleDataRows().isEmpty()) return true;
            pause(750);
        }
        System.out.println("[ArcFlashPage] no data rows rendered after ~45s");
        return false;
    }

    /** True once the open modal has rendered its form content (>=2 inputs) — guards against checking
     *  fields before the modal body has loaded. */
    private boolean modalContentReady(By dialogLocator) {
        List<WebElement> d = driver.findElements(dialogLocator);
        if (d.isEmpty()) return false;
        try { return d.get(0).findElements(By.xpath(".//input")).size() >= 2; } catch (Exception e) { return false; }
    }

    /**
     * Click the first table rows (trying the first few) until {dialogLocator} appears with content.
     * Waits for the table to load (the tab content loads async after a tab switch); if no rows load,
     * re-clicks {tabName} once to re-trigger the data fetch.
     */
    private boolean openRowEditModal(By dialogLocator, String tabName) {
        for (int pass = 0; pass < 2; pass++) {
            if (waitForTableRows()) {
                int tries = Math.min(3, visibleDataRows().size());
                for (int idx = 0; idx < tries; idx++) {
                    try {
                        List<WebElement> rows = visibleDataRows();
                        if (idx >= rows.size()) break;
                        WebElement cell = rows.get(idx).findElements(By.xpath(".//*[@role='gridcell'] | .//td")).stream().findFirst().orElse(rows.get(idx));
                        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cell);
                        pause(200);
                        WebElement target = cell.findElements(By.xpath(".//a | .//button | .//p | .//span")).stream().findFirst().orElse(cell);
                        try { target.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", target); }
                        for (int w = 0; w < 20; w++) { // up to 10s for the modal + its content
                            if (modalContentReady(dialogLocator)) { pause(400); return true; }
                            pause(500);
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (tabName != null) { try { clickTab(tabName); } catch (Exception ignored) {} } // re-trigger fetch
        }
        return !driver.findElements(dialogLocator).isEmpty();
    }

    // ── Source/Target Connections → Edit Asset modal ────────────

    /** Click the first asset row on the Source/Target Connections tab to open its Edit Asset modal. */
    public boolean openFirstAssetEdit() {
        return openRowEditModal(EDIT_ASSET_DIALOG, "Source/Target Connections");
    }

    public boolean isEditAssetModalOpen() {
        return !driver.findElements(EDIT_ASSET_DIALOG).isEmpty();
    }

    /** True if the open Edit Asset modal shows the given field/label text (Asset Name, Asset Class, Location, Profile, …). */
    public boolean editAssetHas(String text) {
        return !driver.findElements(By.xpath(
                "//div[@role='dialog'][.//*[normalize-space()='Edit Asset']]//*[contains(normalize-space(),'" + text + "')]")).isEmpty();
    }

    // ── Connection Details → Edit Connection modal ──────────────

    public boolean hasBuswayReadiness() {
        return !driver.findElements(By.xpath("//*[contains(normalize-space(),'Busway Arc Flash Readiness')]")).isEmpty();
    }

    /** Wait (up to ~30s) for the Connection Details "Busway Arc Flash Readiness" section to render. */
    public boolean waitForBuswayReadiness() {
        for (int i = 0; i < 40; i++) {
            if (hasBuswayReadiness()) return true;
            pause(750);
        }
        System.out.println("[ArcFlashPage] Busway Arc Flash Readiness not present after ~30s");
        return false;
    }

    /** Click the first busway connection row on the Connection Details tab to open its Edit Connection modal. */
    public boolean openFirstConnectionEdit() {
        return openRowEditModal(EDIT_CONNECTION_DIALOG, "Connection Details");
    }

    public boolean isEditConnectionModalOpen() {
        return !driver.findElements(EDIT_CONNECTION_DIALOG).isEmpty();
    }

    public boolean editConnectionHas(String text) {
        return !driver.findElements(By.xpath(
                "//div[@role='dialog'][.//*[normalize-space()='Edit Connection']]//*[contains(normalize-space(),'" + text + "')]")).isEmpty();
    }

    /** Select a Conductor Material (e.g. "Copper"/"Aluminum") in the Edit Connection modal — a MUI Autocomplete. */
    public void selectConductorMaterial(String material) {
        By input = By.xpath("(//div[@role='dialog']//p[starts-with(normalize-space(),'Conductor Material')]"
                + "/following::input[@role='combobox'])[1]");
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(input));
        js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus(); arguments[0].click();"
                + "var w=arguments[0].closest('.MuiAutocomplete-root'); if(w){var b=w.querySelector('.MuiAutocomplete-popupIndicator'); if(b) b.click();}", el);
        pause(700);
        By opt = By.xpath("//ul[@role='listbox']//li[@role='option'][normalize-space()='" + material + "']");
        for (int i = 0; i < 8 && driver.findElements(opt).isEmpty(); i++) pause(300);
        List<WebElement> opts = driver.findElements(opt);
        if (opts.isEmpty()) throw new RuntimeException("Conductor Material option not found: " + material);
        WebElement o = opts.get(0);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", o);
        try { new Actions(driver).moveToElement(o).click().perform(); } catch (Exception e) { o.click(); }
        pause(500);
        System.out.println("[ArcFlashPage] Conductor Material: " + material);
    }

    public String getConductorMaterialValue() {
        By input = By.xpath("(//div[@role='dialog']//p[starts-with(normalize-space(),'Conductor Material')]"
                + "/following::input[@role='combobox'])[1]");
        List<WebElement> els = driver.findElements(input);
        return els.isEmpty() ? "" : String.valueOf(els.get(0).getAttribute("value"));
    }

    /** Enter the Length (ft) in the Edit Connection modal (the modal's number input). */
    public void enterLength(String lengthFt) {
        By input = By.xpath("//div[@role='dialog']//input[@type='number']");
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(input));
        js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus(); arguments[0].click();"
                + "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                + "s.call(arguments[0],'');"
                + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", el);
        pause(100);
        try { el.sendKeys(lengthFt); } catch (Exception e) {
            js.executeScript("var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],arguments[1]);"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                    + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", el, lengthFt);
        }
        pause(300);
        System.out.println("[ArcFlashPage] Length (ft): " + lengthFt);
    }

    public String getLengthValue() {
        List<WebElement> els = driver.findElements(By.xpath("//div[@role='dialog']//input[@type='number']"));
        return els.isEmpty() ? "" : String.valueOf(els.get(0).getAttribute("value"));
    }

    public boolean isSaveChangesEnabled() {
        List<WebElement> b = driver.findElements(SAVE_CHANGES_BTN);
        return !b.isEmpty() && b.get(0).isEnabled();
    }

    /** Click "Save Changes" in the open modal and wait for it to close. Returns true if it closed. */
    public boolean saveChanges() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(SAVE_CHANGES_BTN));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        pause(150);
        try { btn.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", btn); }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25))
                    .until(ExpectedConditions.invisibilityOfElementLocated(EDIT_CONNECTION_DIALOG));
        } catch (Exception ignored) {}
        pause(500);
        return driver.findElements(EDIT_CONNECTION_DIALOG).isEmpty();
    }

    // ── Generic modal close (the top-right X icon button) ───────

    public boolean isAnyModalOpen() {
        for (WebElement d : driver.findElements(By.xpath("//div[@role='dialog'] | //div[contains(@class,'MuiDialog-paper')]")))
            try { if (d.isDisplayed() && d.getRect().getWidth() > 200) return true; } catch (Exception ignored) {}
        return false;
    }

    /** Close the open modal via its top-right X icon button (no aria-label; sits in the header). */
    public boolean closeModal() {
        Object r = js.executeScript(
            "var dlgs=[].slice.call(document.querySelectorAll('[role=\"dialog\"],.MuiDialog-paper'))"
          + ".filter(function(d){return d.getBoundingClientRect().width>200;});"
          + "if(!dlgs.length) return 'no-dialog';"
          + "var d=dlgs[dlgs.length-1], dr=d.getBoundingClientRect();"
          + "var btns=[].slice.call(d.querySelectorAll('button')).filter(function(b){"
          + "  return (b.textContent||'').trim()==='' && b.querySelector('svg');});"
          + "var hdr=btns.filter(function(b){return (b.getBoundingClientRect().top-dr.top)<80;});"
          + "var pool=hdr.length?hdr:btns;"
          + "pool.sort(function(a,b){return b.getBoundingClientRect().right-a.getBoundingClientRect().right;});"
          + "if(pool[0]){pool[0].click(); return 'clicked';} return 'no-btn';");
        System.out.println("[ArcFlashPage] closeModal: " + r);
        pause(700);
        return !isAnyModalOpen();
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
