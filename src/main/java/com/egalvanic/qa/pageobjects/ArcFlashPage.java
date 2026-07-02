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

    // ════════════════════════════════════════════════════════════
    // OVERVIEW ANALYTICS — gauges, per-class breakdown, info tooltips
    // (DOM verified live 2026-06-30, site "Android Qa Site1", admin role)
    // ════════════════════════════════════════════════════════════

    /** The three Overview readiness gauges as "Label=NN%" strings (Asset Details / Source-Target / Connection Details). */
    @SuppressWarnings("unchecked")
    public List<String> getOverviewGauges() {
        Object out = js.executeScript(
            "var labels=['Asset Details','Source/Target Connections','Connection Details'];var L=[];"
          + "function leaves(txt){var a=[];document.querySelectorAll('body *').forEach(function(e){"
          + "  if(e.children.length===0 && (e.textContent||'').trim()===txt){var r=e.getBoundingClientRect();if(r.width>0)a.push(e);}});return a;}"
          + "labels.forEach(function(lb){var nodes=leaves(lb);"
          + "  outer: for(var i=0;i<nodes.length;i++){var cur=nodes[i];"
          + "    for(var up=0; up<5 && cur.parentElement; up++){cur=cur.parentElement;"
          + "      var pcts=[].slice.call(cur.querySelectorAll('*')).filter(function(x){return x.children.length===0 && /^\\d{1,3}%$/.test((x.textContent||'').trim());});"
          + "      if(pcts.length){L.push(lb+'='+pcts[0].textContent.trim()); break outer;}}}});"
          + "return L;");
        return (List<String>) out;
    }

    /** Poll until the three Overview gauges have rendered — a tab switch / Eng-Mode toggle / role switch /
     *  refresh all recompute the readiness asynchronously, so the gauges briefly disappear then re-render. */
    public List<String> waitForOverviewGauges() {
        List<String> g = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            g = getOverviewGauges();
            if (g.size() >= 3) return g;
            pause(1000);
        }
        System.out.println("[ArcFlashPage] only " + g.size() + " Overview gauges after ~15s");
        return g;
    }

    /** Every distinct "NN%" badge currently rendered on the page (the recalculation oracle for Engineering Mode). */
    @SuppressWarnings("unchecked")
    public List<String> getAllPercents() {
        Object out = js.executeScript(
            "var L=[];document.querySelectorAll('body *').forEach(function(e){if(e.children.length)return;"
          + "var t=(e.textContent||'').trim();if(/^\\d{1,3}%$/.test(t)){var r=e.getBoundingClientRect();if(r.width>0)L.push(t);}});return L;");
        return (List<String>) out;
    }

    /** Per-asset-class completion cards as "Class|NN%|X/Y complete" strings (ATS, Battery, Circuit Breaker, …). */
    @SuppressWarnings("unchecked")
    public List<String> getClassBreakdown() {
        Object out = js.executeScript(
            "var L=[];document.querySelectorAll('span,p,div').forEach(function(s){if(s.children.length)return;"
          + "var t=(s.textContent||'').trim();var m=t.match(/^(\\d+)\\/(\\d+) complete$/);if(!m)return;"
          + "var cur=s,name='',pct='';"
          + "for(var up=0; up<6 && cur.parentElement; up++){cur=cur.parentElement;"
          + "  var sub=cur.querySelector('.MuiTypography-subtitle1');"
          + "  var pc=[].slice.call(cur.querySelectorAll('*')).filter(function(x){return x.children.length===0 && /^\\d{1,3}%$/.test((x.textContent||'').trim());});"
          + "  if(sub && pc.length){name=sub.textContent.trim();pct=pc[0].textContent.trim();break;}}"
          + "if(name)L.push(name+'|'+pct+'|'+t);});return L;");
        return (List<String>) out;
    }

    /** The info-icon tooltip descriptions (exposed as the icon buttons' aria-labels — start with "Percentage"). */
    @SuppressWarnings("unchecked")
    public List<String> getInfoTooltipTexts() {
        Object out = js.executeScript(
            "var L=[];document.querySelectorAll('button[aria-label]').forEach(function(b){"
          + "var a=b.getAttribute('aria-label')||'';if(a.indexOf('Percentage')===0)L.push(a);});return L;");
        return (List<String>) out;
    }

    // ── Refresh control (icon-only button, top-right of the tab bar) ──

    /** Click the unlabeled refresh icon button at the top-right of the tab strip. Returns false if none found. */
    public boolean clickRefresh() {
        WebElement btn = (WebElement) js.executeScript(
            "var bs=[].slice.call(document.querySelectorAll('button')).filter(function(b){"
          + " var r=b.getBoundingClientRect();"
          + " return r.width>0 && r.top>=40 && r.top<=120 && (b.textContent||'').trim()==='' "
          + " && !(b.getAttribute('aria-label')||'') && b.querySelector('svg');});"
          + "bs.sort(function(a,b){return b.getBoundingClientRect().left-a.getBoundingClientRect().left;});"
          + "return bs.length?bs[0]:null;");
        if (btn == null) { System.out.println("[ArcFlashPage] no refresh icon button found in the tab-bar region"); return false; }
        try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn); } catch (Exception ignored) {}
        pause(150);
        try { new Actions(driver).moveToElement(btn).click().perform(); }
        catch (Exception e) { try { btn.click(); } catch (Exception e2) { js.executeScript("arguments[0].click();", btn); } }
        pause(1500);
        System.out.println("[ArcFlashPage] clicked refresh icon");
        return true;
    }

    // ── Role selector (header "Select role" combobox / Autocomplete) ──

    public String getRoleValue() {
        List<WebElement> els = driver.findElements(By.xpath("//input[@placeholder='Select role']"));
        return els.isEmpty() ? "" : String.valueOf(els.get(0).getAttribute("value"));
    }

    public boolean hasRoleSelector() {
        return !driver.findElements(By.xpath("//input[@placeholder='Select role']")).isEmpty();
    }

    /** Open the role Autocomplete and read all options (polls + retries — a plain click can race the listbox). */
    public List<String> getRoleOptions() {
        for (int attempt = 0; attempt < 3; attempt++) {
            List<WebElement> inps = driver.findElements(By.xpath("//input[@placeholder='Select role']"));
            if (inps.isEmpty()) return new ArrayList<>();
            WebElement inp = inps.get(0);
            try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", inp); } catch (Exception ignored) {}
            try { new Actions(driver).moveToElement(inp).click().perform(); } catch (Exception e) { try { inp.click(); } catch (Exception ignored) {} }
            for (int i = 0; i < 12 && driver.findElements(LISTBOX_OPTION).isEmpty(); i++) pause(300);
            List<String> out = new ArrayList<>();
            for (WebElement li : driver.findElements(LISTBOX_OPTION)) {
                String t = li.getText().trim(); if (!t.isEmpty()) out.add(t);
            }
            try { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); } catch (Exception ignored) {}
            pause(300);
            if (!out.isEmpty()) return out;
            pause(400);
        }
        return new ArrayList<>();
    }

    /**
     * Select a role in the header view selector — a MUI <b>Autocomplete</b>. Robust pattern: focus, clear,
     * <b>type the name to filter</b> the option list (avoids virtualisation/scroll misses), then click the
     * filtered exact match. Retries up to 3×. Returns true once the selector reflects {name}.
     */
    public boolean selectRole(String name) {
        if (matchesRole(name)) return true; // already selected
        Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
        By inputBy = By.xpath("//input[@placeholder='Select role']");
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                // Re-find + wait clickable each attempt (a role-switch recompute can briefly overlay/re-mount it).
                WebElement inp = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(inputBy));
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", inp);
                try { new Actions(driver).moveToElement(inp).click().perform(); } catch (Exception e) { inp.click(); }
                pause(300);
                try { inp.sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
                pause(250);
                inp.sendKeys(name);                       // type → filters the Autocomplete to matching options
                pause(800);
                // Keyboard-commit the highlighted filtered option (robust vs click-target / virtualisation).
                inp.sendKeys(Keys.ARROW_DOWN);
                pause(250);
                inp.sendKeys(Keys.ENTER);
                // The readiness recomputes under the new role (some roles, e.g. Electrical Engineer, are slow) —
                // poll for the selector to settle rather than checking once.
                for (int w = 0; w < 12; w++) {
                    pause(1000);
                    if (matchesRole(name)) {
                        System.out.println("[ArcFlashPage] selectRole('" + name + "') -> value now '" + getRoleValue() + "'");
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            try { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); } catch (Exception ignored) {}
            pause(500);
        }
        System.out.println("[ArcFlashPage] selectRole('" + name + "') FAILED — value '" + getRoleValue() + "'");
        return matchesRole(name);
    }

    private boolean matchesRole(String name) {
        String v = getRoleValue();
        return name.equalsIgnoreCase(v) || (!v.isEmpty() && v.toLowerCase().contains(name.toLowerCase()));
    }

    // ════════════════════════════════════════════════════════════
    // SOURCE/TARGET CONNECTIONS analytics — summary banner, status, search, sort
    // ════════════════════════════════════════════════════════════

    /** The "N assets require source • N connected (N direct, N indirect) • N missing" summary banner text. */
    public String getSourceConnectionSummary() {
        for (WebElement e : driver.findElements(By.xpath(
                "//*[contains(normalize-space(),'require source')][contains(normalize-space(),'connected')]"))) {
            try { String t = e.getText().trim(); if (!t.isEmpty() && t.length() < 200) return t; } catch (Exception ignored) {}
        }
        return "";
    }

    /** The "NN%" Complete figure shown beside the Source Connection Status banner. */
    public String getSourceCompletePercent() {
        Object out = js.executeScript(
            "var res='';document.querySelectorAll('body *').forEach(function(e){if(e.children.length)return;"
          + "if((e.textContent||'').trim()!=='Complete')return;var cur=e;"
          + "for(var up=0; up<4 && cur.parentElement; up++){cur=cur.parentElement;"
          + "  var pc=[].slice.call(cur.querySelectorAll('*')).filter(function(x){return x.children.length===0 && /^\\d{1,3}%$/.test((x.textContent||'').trim());});"
          + "  if(pc.length){res=pc[0].textContent.trim();return;}}});return res;");
        return out == null ? "" : out.toString();
    }

    /** Values in a named DataGrid column (matched by header text, read via aria-colindex). */
    @SuppressWarnings("unchecked")
    public List<String> getColumnValues(String header) {
        Object out = js.executeScript(
            "var h=arguments[0];var hs=[].slice.call(document.querySelectorAll('[role=columnheader]'));var col=null;"
          + "for(var i=0;i<hs.length;i++){if((hs[i].textContent||'').trim().indexOf(h)>=0){col=hs[i].getAttribute('aria-colindex');break;}}"
          + "if(col===null)return [];var vals=[];"
          + "document.querySelectorAll('[role=row]').forEach(function(r){"
          + "  var c=r.querySelector('[role=gridcell][aria-colindex=\"'+col+'\"]');"
          + "  if(c){vals.push((c.textContent||'').trim());}});return vals;", header);
        return (List<String>) out;
    }

    private static final By SEARCH_INPUT = By.xpath("//input[@placeholder='Search items...']");

    public boolean hasSearchBox() { return !driver.findElements(SEARCH_INPUT).isEmpty(); }

    /** Type into the "Search items…" box (clears first). The DataGrid filters live. */
    public void searchItems(String text) {
        WebElement inp = wait.until(ExpectedConditions.elementToBeClickable(SEARCH_INPUT));
        Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
        try { inp.click(); inp.sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
        inp.sendKeys(text);
        pause(1400);
        System.out.println("[ArcFlashPage] searchItems('" + text + "')");
    }

    public void clearSearch() {
        List<WebElement> els = driver.findElements(SEARCH_INPUT);
        if (els.isEmpty()) return;
        Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
        try { els.get(0).click(); els.get(0).sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
        pause(1200);
    }

    /** aria-sort state ("ascending"/"descending"/"none"/"") of a named DataGrid column header. */
    public String getColumnSortState(String header) {
        Object out = js.executeScript(
            "var h=arguments[0];var hs=[].slice.call(document.querySelectorAll('[role=columnheader]'));"
          + "for(var i=0;i<hs.length;i++){if((hs[i].textContent||'').trim().indexOf(h)>=0)return hs[i].getAttribute('aria-sort')||'';}return '';", header);
        return out == null ? "" : out.toString();
    }

    /** Click a DataGrid column header to (re)sort by it; returns the resulting aria-sort state. */
    public String sortByColumn(String header) {
        By hdr = By.xpath("//*[@role='columnheader'][contains(normalize-space(),'" + header + "')]");
        List<WebElement> els = driver.findElements(hdr);
        if (els.isEmpty()) { System.out.println("[ArcFlashPage] sort header not found: " + header); return ""; }
        WebElement h = els.get(0);
        try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", h); } catch (Exception ignored) {}
        pause(150);
        // Click the title cell of the header (the DataGrid sort target).
        WebElement target = h.findElements(By.xpath(".//*[contains(@class,'MuiDataGrid-columnHeaderTitleContainer')] | .//*[contains(@class,'columnHeaderTitle')]"))
                .stream().findFirst().orElse(h);
        try { new Actions(driver).moveToElement(target).click().perform(); } catch (Exception e) { try { target.click(); } catch (Exception e2) { js.executeScript("arguments[0].click();", target); } }
        pause(1200);
        String state = getColumnSortState(header);
        System.out.println("[ArcFlashPage] sortByColumn('" + header + "') -> aria-sort=" + state);
        return state;
    }

    // ── Pagination (MUI TablePagination) ────────────────────────

    public boolean hasRowsPerPage() {
        return !driver.findElements(By.xpath("//*[contains(normalize-space(),'Rows per page')]")).isEmpty();
    }

    /** The "1–N of M" displayed-rows text from the table footer. */
    public String getPaginationText() {
        for (WebElement e : driver.findElements(By.xpath(
                "//*[contains(@class,'MuiTablePagination-displayedRows')]"))) {
            try { String t = e.getText().trim(); if (!t.isEmpty()) return t; } catch (Exception ignored) {}
        }
        // fallback: any short element with the "X of Y" shape
        for (WebElement e : driver.findElements(By.xpath("//*[contains(normalize-space(),' of ')]"))) {
            String t;
            try { t = e.getText().trim(); } catch (Exception ex) { continue; }
            if (t.matches(".*\\d+[\\u2009\\u2013\\u2014\\-]\\d+ of \\d+.*")) return t;
        }
        return "";
    }

    // ════════════════════════════════════════════════════════════
    // CONNECTION DETAILS — Connection Type filter (a MUI Select)
    // ════════════════════════════════════════════════════════════

    /** The visible Connection-Type MUI Select (the one whose value is a type name, not the numeric rows-per-page). */
    private WebElement connectionTypeSelect() {
        for (WebElement e : driver.findElements(By.xpath("//div[contains(@class,'MuiSelect-select')]"))) {
            try {
                if (!e.isDisplayed() || e.getRect().getWidth() <= 0) continue;
                String t = e.getText().trim();
                if (t.isEmpty() || t.matches("\\d+")) continue; // skip empty + rows-per-page
                return e;
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getConnectionTypeValue() {
        WebElement e = connectionTypeSelect();
        return e == null ? "" : e.getText().trim();
    }

    public boolean hasConnectionTypeFilter() { return connectionTypeSelect() != null; }

    private boolean openSelect(WebElement sel) {
        if (sel == null) return false;
        if (!driver.findElements(LISTBOX_OPTION).isEmpty()) return true;
        for (int a = 0; a < 4; a++) {
            try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", sel); } catch (Exception ignored) {}
            pause(150);
            try {
                if (a == 0) sel.click();
                else if (a == 1) new Actions(driver).moveToElement(sel).click().perform();
                else js.executeScript("['pointerdown','mousedown','mouseup','click'].forEach(function(t){"
                        + "arguments[0].dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,view:window}));});", sel);
            } catch (Exception ignored) {}
            try { new WebDriverWait(driver, Duration.ofSeconds(6)).until(ExpectedConditions.visibilityOfElementLocated(LISTBOX_OPTION)); } catch (Exception ignored) {}
            if (!driver.findElements(LISTBOX_OPTION).isEmpty()) { pause(250); return true; }
            pause(400);
        }
        return false;
    }

    public List<String> getConnectionTypeOptions() {
        List<String> out = new ArrayList<>();
        if (!openSelect(connectionTypeSelect())) return out;
        for (WebElement li : driver.findElements(LISTBOX_OPTION)) {
            String t = li.getText().trim(); if (!t.isEmpty()) out.add(t);
        }
        try { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); } catch (Exception ignored) {}
        pause(300);
        return out;
    }

    public void selectConnectionType(String type) {
        if (!openSelect(connectionTypeSelect())) throw new RuntimeException("Connection Type select did not open");
        By exact = By.xpath("//ul[@role='listbox']//li[@role='option'][normalize-space()='" + type + "']");
        for (int i = 0; i < 8 && driver.findElements(exact).isEmpty(); i++) pause(300);
        List<WebElement> opts = driver.findElements(exact);
        if (opts.isEmpty()) throw new RuntimeException("Connection Type option not found: " + type);
        try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", opts.get(0)); } catch (Exception ignored) {}
        try { new Actions(driver).moveToElement(opts.get(0)).click().perform(); } catch (Exception e) { opts.get(0).click(); }
        pause(1200);
        System.out.println("[ArcFlashPage] selected Connection Type: " + type);
    }

    // ── SLD viewer presence (TC-AF-004) ─────────────────────────

    /** True if /arc-flash embeds an SLD/react-flow diagram canvas (scrolls the page to check below the fold). */
    public boolean hasSldViewer() {
        try { js.executeScript("window.scrollTo(0, document.body.scrollHeight);"); } catch (Exception ignored) {}
        pause(800);
        Boolean has = (Boolean) js.executeScript(
            "return !!(document.querySelector('canvas, .react-flow, [class*=\"react-flow\"], svg[class*=\"diagram\"], [class*=\"diagram\"], [data-testid*=\"sld\" i]'));");
        try { js.executeScript("window.scrollTo(0,0);"); } catch (Exception ignored) {}
        pause(300);
        return Boolean.TRUE.equals(has);
    }

    /** True if a "Generate Report" / "Export"/"Download Report" control exists on the page (verifies the spec). */
    public boolean hasGenerateReport() {
        return !driver.findElements(By.xpath(
                "//button[contains(normalize-space(),'Generate Report') or contains(normalize-space(),'Generate EG') "
              + "or contains(normalize-space(),'Download Report')] "
              + "| //*[@role='button'][contains(normalize-space(),'Generate Report')]")).isEmpty();
    }

    // ════════════════════════════════════════════════════════════
    // CIRCUIT BREAKER ENGINEERING — Asset Details → CB class → Edit Asset modal → library match
    // (DOM verified live 2026-07-02, site "abhiiyant 17 june site": Eng-Mode CB columns are Label /
    //  Poles / Frame Amps / Type / Manufacturer / Library row / Sensor Amps / Plug Amps / Amperage /
    //  Percentage Completion; clicking a dash engineering cell opens the "Edit Asset" MUI dialog with
    //  BASIC INFO + ENGINEERING (Pole Count buttons, "Select manufacturer" Autocomplete, search term)
    //  + CUSTOM ATTRIBUTES (Ampere Rating / Breaker Settings / Catalog Number / Interrupting Rating /
    //  Model / Voltage) + COMMERCIAL + Cancel / Save Changes.)
    // ════════════════════════════════════════════════════════════

    private static final By EDIT_ASSET_DLG = By.xpath("//div[@role='dialog'][.//*[normalize-space()='Edit Asset']]");

    /** The per-class readiness banner line, e.g. "2 assets • 7 of 12 required fields completed". */
    public String getClassBannerText() {
        Object out = js.executeScript(
            "var e=[].slice.call(document.querySelectorAll('p,div,span')).find(function(x){"
          + "return x.children.length<2 && /required fields completed/i.test(x.textContent||'');});"
          + "return e? e.textContent.trim() : '';");
        return out == null ? "" : out.toString();
    }

    /** The "NN%" completion figure shown beside the class banner (first percent near 'Complete'). */
    public String getClassBannerPercent() {
        Object out = js.executeScript(
            "var res='';[].slice.call(document.querySelectorAll('body *')).some(function(e){"
          + "if(e.children.length) return false; if((e.textContent||'').trim()!=='Complete') return false;"
          + "var cur=e; for(var up=0; up<4 && cur.parentElement; up++){cur=cur.parentElement;"
          + "  var pc=[].slice.call(cur.querySelectorAll('*')).filter(function(x){return x.children.length===0 && /^\\d{1,3}%$/.test((x.textContent||'').trim());});"
          + "  if(pc.length){res=pc[0].textContent.trim(); return true;}} return false;});return res;");
        return out == null ? "" : out.toString();
    }

    /** Cell texts of the row whose Label cell equals {label} (empty list if absent). */
    @SuppressWarnings("unchecked")
    public List<String> getRowCellsByLabel(String label) {
        Object out = js.executeScript(
            "var lbl=arguments[0];var rows=[].slice.call(document.querySelectorAll(\"[role='row']\"))"
          + ".filter(function(r){return r.querySelector(\"[role='gridcell']\");});"
          + "for(var i=0;i<rows.length;i++){var cells=[].slice.call(rows[i].querySelectorAll(\"[role='gridcell']\"));"
          + "  if(cells.length && cells[0].textContent.trim()===lbl) return cells.map(function(c){return c.textContent.trim();});}"
          + "return [];", label);
        return (List<String>) out;
    }

    /**
     * Open the Edit Asset modal from an engineering cell in the CB grid. Prefers a row whose
     * "Frame Amps" cell is a dash (an incomplete breaker); falls back to the LAST data row (never the
     * first — that may be manually-curated demo data). Returns the clicked row's Label, or null.
     */
    public String openCbEditFromEngineeringCell() {
        Object label = js.executeScript(
            "var hs=[].slice.call(document.querySelectorAll(\"[role='columnheader']\"));"
          + "var idx=null; hs.forEach(function(h){ if(/frame amps/i.test(h.textContent)) idx=h.getAttribute('aria-colindex'); });"
          + "if(idx===null) return null;"
          + "var rows=[].slice.call(document.querySelectorAll(\"[role='row']\")).filter(function(r){return r.querySelector(\"[role='gridcell']\");});"
          + "if(!rows.length) return null;"
          + "var target=null;"
          + "for(var i=0;i<rows.length;i++){var c=rows[i].querySelector(\"[role='gridcell'][aria-colindex='\"+idx+\"']\");"
          + "  if(c && /^[—\\-–]?$/.test(c.textContent.trim())){ target=rows[i]; break; }}"
          + "if(!target) target=rows[rows.length-1];"
          + "var cell=target.querySelector(\"[role='gridcell'][aria-colindex='\"+idx+\"']\") || target.querySelector(\"[role='gridcell']\");"
          + "var t=cell.querySelector('button, svg, a, span')||cell;"
          + "t.scrollIntoView({block:'center'}); t.click();"
          + "return target.querySelector(\"[role='gridcell']\").textContent.trim();");
        if (label == null) { System.out.println("[ArcFlashPage] no CB engineering cell to click"); return null; }
        for (int i = 0; i < 24; i++) {                       // modal + its form content load async
            if (!driver.findElements(EDIT_ASSET_DLG).isEmpty()
                    && editAssetDialogText().contains("ENGINEERING")) {
                System.out.println("[ArcFlashPage] Edit Asset modal open for '" + label + "'");
                pause(600);
                return label.toString();
            }
            pause(500);
        }
        System.out.println("[ArcFlashPage] Edit Asset modal did not open/render for '" + label + "'");
        return null;
    }

    private String editAssetDialogText() {
        List<WebElement> d = driver.findElements(EDIT_ASSET_DLG);
        if (d.isEmpty()) return "";
        try { return d.get(0).getText(); } catch (Exception e) { return ""; }
    }

    public boolean editAssetHasManufacturerSearch() {
        // Poll ~10s: the manufacturer sub-component lazy-renders a beat after the modal opens.
        // DOM check (not getText, which is visible-only — the ENGINEERING section may be scrolled off).
        for (int i = 0; i < 20; i++) {
            Object r = js.executeScript(
                "var d=document.querySelector(\"[role='dialog']\"); if(!d) return false;"
              + "var hasMfg=[].slice.call(d.querySelectorAll('input')).some(function(i){return /manufacturer/i.test(i.placeholder||'');});"
              + "var t=d.textContent||'';"
              + "return hasMfg || /search term|pick a manufacturer/i.test(t);");
            if (Boolean.TRUE.equals(r)) return true;
            pause(500);
        }
        return false;
    }

    /** Click a Pole Count button (1P / 2P / 3P) inside the Edit Asset modal. */
    public boolean setPoleCountInModal(String pole) {
        Object r = js.executeScript(
            "var p=arguments[0];var d=document.querySelector(\"[role='dialog']\"); if(!d) return false;"
          + "var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return x.textContent.trim()===p;});"
          + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;", pole);
        System.out.println("[ArcFlashPage] pole count " + pole + " -> " + r);
        pause(800);
        return Boolean.TRUE.equals(r);
    }

    /**
     * Select an option in a MUI Autocomplete inside the open dialog, located by input placeholder regex.
     * Types to filter, then clicks the first option containing {optionContains} (case-insensitive);
     * keyboard-commits (ArrowDown+Enter) as fallback. Returns the input's value afterwards.
     */
    public String selectDialogAutocomplete(String placeholderRegex, String typeText, String optionContains) {
        // Locator JS for a dialog-scoped input by placeholder regex — reused across retries.
        String findByPlaceholder =
            "var re=new RegExp(arguments[0],'i');var d=document.querySelector(\"[role='dialog']\")||document;"
          + "var inp=[].slice.call(d.querySelectorAll('input')).find(function(i){return re.test(i.placeholder||'');});"
          + "if(!inp) return false;";
        return typePickAutocomplete(findByPlaceholder, placeholderRegex, typeText, optionContains, "placeholder=" + placeholderRegex);
    }

    /**
     * Shared type→pick engine for MUI Autocomplete inside the open dialog. Uses the native React value
     * setter (sendKeys does not reliably drive controlled inputs on this app) and returns the CLICKED
     * option's text (the committed value lives in the option, not input.value). Retries 3× — the slow QA
     * option list can lag the first keystroke. {@code findJs} must locate the input into var {@code inp}
     * and {@code return false} if not found. Listbox options are portalled to <body>, so query document.
     */
    private String typePickAutocomplete(String findJs, String arg0, String typeText, String optionContains, String tag) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Object filled = js.executeScript(
                    findJs
                  + "inp.scrollIntoView({block:'center'}); inp.focus(); inp.click();"
                  + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                  + "set.call(inp,''); inp.dispatchEvent(new Event('input',{bubbles:true}));"
                  + "set.call(inp,arguments[1]); inp.dispatchEvent(new Event('input',{bubbles:true}));"
                  + "return true;", arg0, typeText);
                if (!Boolean.TRUE.equals(filled)) { pause(700); continue; }
                pause(1300);
                Object txt = js.executeScript(
                    "var want=arguments[0].toLowerCase();"
                  + "var opts=[].slice.call(document.querySelectorAll(\"li[role='option']\"));"
                  + "var m=opts.find(function(o){return o.textContent.trim().toLowerCase()===want;})"
                  + "      || opts.find(function(o){return o.textContent.toLowerCase().indexOf(want)>-1;});"
                  + "if(!m && opts.length===1) m=opts[0];"
                  + "if(!m) return null; m.scrollIntoView({block:'center'}); m.click(); return m.textContent.trim();",
                    optionContains);
                if (txt != null) {
                    System.out.println("[ArcFlashPage] dialog autocomplete " + tag + " <- '" + typeText + "' => '" + txt + "'");
                    pause(600);
                    return txt.toString();
                }
                pause(800);
            } catch (Exception e) { pause(700); }
        }
        System.out.println("[ArcFlashPage] dialog autocomplete " + tag + " <- '" + typeText + "' => (no option for '" + optionContains + "')");
        return null;
    }

    /**
     * Like {@link #selectDialogAutocomplete} but locates the input by the text of its nearest preceding
     * label/paragraph (e.g. "Phase A Wire Size", "Conductor Material") instead of a placeholder.
     */
    public String selectDialogAutocompleteByLabel(String labelRegex, String typeText, String optionContains) {
        // Locate the input by nearest label/paragraph text, then hand off to the shared type→pick engine.
        String findByLabel =
            "var re=new RegExp(arguments[0],'i');var d=document.querySelector(\"[role='dialog']\")||document;"
          + "var labs=[].slice.call(d.querySelectorAll('p,label,span')).filter(function(l){"
          + "  return l.children.length<2 && re.test((l.textContent||'').trim()) && (l.textContent||'').trim().length<60;});"
          + "var inp=null;"
          + "for(var i=0;i<labs.length && !inp;i++){var cur=labs[i];"
          + "  for(var up=0; up<4 && cur; up++){"
          + "    var c=(cur.parentElement||cur).querySelector('input');"
          + "    if(c && c.getBoundingClientRect().width>0){inp=c;break;}"
          + "    cur=cur.parentElement;}}"
          + "if(!inp) return false;";
        return typePickAutocomplete(findByLabel, labelRegex, typeText, optionContains, "label=" + labelRegex);
    }

    /** Wait until the library "possible matches" cards render in the dialog (after picking a manufacturer). */
    public boolean waitForMatchCards(int seconds, String cardToken) {
        for (int i = 0; i < seconds; i++) {
            String t = editAssetDialogText();
            if (t.matches("(?s).*\\d+ possible match.*") || t.contains(cardToken)) return true;
            pause(1000);
        }
        System.out.println("[ArcFlashPage] library match cards did not render in " + seconds + "s");
        return false;
    }

    /** The "N possible matches" text currently shown in the dialog ("" if absent). */
    public String getPossibleMatchesText() {
        String t = editAssetDialogText();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+ possible match(es)?").matcher(t);
        return m.find() ? m.group() : "";
    }

    /**
     * Click the library model card matching ALL {tokens} (and excluding LSI/Touch variants) inside the
     * dialog — ported from AssetEngineeringTestNG.matchLibraryModel, scoped to the dialog.
     */
    public boolean clickModelCard(String[] tokens) {
        Object r = js.executeScript(
            "var toks=arguments[0];var d=document.querySelector(\"[role='dialog']\")||document;"
          + "function ok(t){ if(!t) return false; for(var i=0;i<toks.length;i++){ if(t.indexOf(toks[i])<0) return false; }"
          + " return t.indexOf('LSI')<0 && t.indexOf('Touch')<0; }"
          + "var cands=[].slice.call(d.querySelectorAll(\"li,[role='option'],[role='button'],.MuiListItemButton-root,.MuiListItem-root,.MuiCard-root,div\"))"
          + ".filter(function(e){return ok(e.textContent||'') && (e.textContent||'').length<250;});"
          + "if(!cands.length) return false;"
          + "cands.sort(function(a,b){return a.textContent.length-b.textContent.length;});"
          + "cands[0].scrollIntoView({block:'center'}); cands[0].click(); return true;", (Object) tokens);
        System.out.println("[ArcFlashPage] clickModelCard -> " + r);
        pause(1500);
        return Boolean.TRUE.equals(r);
    }

    /** True once the dialog reflects an applied library match (matched card / populated trip fields). */
    public boolean isLibraryMatchApplied(String frameToken) {
        String t = editAssetDialogText();
        return t.contains("LIBRARY MATCHED") || t.contains("Library Matched")
                || (frameToken != null && t.contains(frameToken));
    }

    /**
     * Click "Save Changes" in the Edit Asset modal; if the modal stays open with the button still present
     * (the app asks for a second confirming click after a library match), click it again. Waits for the
     * dialog to close. Returns true once closed.
     */
    public boolean saveChangesTwice() {
        for (int click = 0; click < 2; click++) {
            List<WebElement> btns = driver.findElements(By.xpath(
                    "//div[@role='dialog']//button[normalize-space()='Save Changes']"));
            if (btns.isEmpty()) break;
            WebElement b = btns.get(btns.size() - 1);
            try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", b); } catch (Exception ignored) {}
            // All three click paths are guarded: the button detaches the instant the save re-renders the
            // modal, so a StaleElementReferenceException here just means the click already landed.
            try { new Actions(driver).moveToElement(b).click().perform(); }
            catch (Exception e) {
                try { b.click(); }
                catch (Exception e2) { try { js.executeScript("arguments[0].click();", b); } catch (Exception ignored) {} }
            }
            System.out.println("[ArcFlashPage] Save Changes click #" + (click + 1));
            for (int i = 0; i < 6; i++) {                    // give it ~3s to either close or re-arm
                pause(500);
                if (driver.findElements(EDIT_ASSET_DLG).isEmpty()) return true;
            }
        }
        // final wait — saving a matched breaker persists engineering data and can take a while
        for (int i = 0; i < 40; i++) {
            if (driver.findElements(EDIT_ASSET_DLG).isEmpty()) return true;
            pause(500);
        }
        System.out.println("[ArcFlashPage] Edit Asset modal still open after Save Changes x2");
        return false;
    }

    /** Click the Generate Report control and report what rendered (dialog / new tab / inline preview). */
    public String clickGenerateReportAndProbe() {
        int windowsBefore = driver.getWindowHandles().size();
        Object clicked = js.executeScript(
            "var b=[].slice.call(document.querySelectorAll(\"button,[role='button']\"))"
          + ".find(function(x){return /generate report/i.test(x.textContent||'');});"
          + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;");
        if (!Boolean.TRUE.equals(clicked)) return "";
        for (int i = 0; i < 20; i++) {
            pause(1000);
            if (driver.getWindowHandles().size() > windowsBefore) return "new-tab";
            if (!driver.findElements(By.xpath("//div[@role='dialog']")).isEmpty()) return "dialog";
            Boolean preview = (Boolean) js.executeScript(
                "return !!(document.querySelector('iframe, embed, canvas.pdf, [class*=\"report-preview\" i]'));");
            if (Boolean.TRUE.equals(preview)) return "inline-preview";
        }
        return "clicked-no-visible-output";
    }

    // ════════════════════════════════════════════════════════════
    // PAGINATION (MUI TablePagination) — rows-per-page + next/prev + footer
    // ════════════════════════════════════════════════════════════

    /** The visible rows-per-page MUI Select (the numeric one inside the table footer). */
    private WebElement rowsPerPageSelect() {
        for (WebElement e : driver.findElements(By.xpath(
                "//*[contains(@class,'MuiTablePagination-root')]//div[contains(@class,'MuiSelect-select')] "
              + "| //div[contains(@class,'MuiTablePagination-select')]"))) {
            try { if (e.isDisplayed() && e.getRect().getWidth() > 0) return e; } catch (Exception ignored) {}
        }
        // fallback: a visible MuiSelect-select whose text is purely numeric (the rows-per-page value)
        for (WebElement e : driver.findElements(By.xpath("//div[contains(@class,'MuiSelect-select')]"))) {
            try { if (e.isDisplayed() && e.getText().trim().matches("\\d+")) return e; } catch (Exception ignored) {}
        }
        return null;
    }

    public String getRowsPerPageValue() {
        WebElement e = rowsPerPageSelect();
        return e == null ? "" : e.getText().trim();
    }

    public List<String> getRowsPerPageOptions() {
        List<String> out = new ArrayList<>();
        if (!openSelect(rowsPerPageSelect())) return out;
        for (WebElement li : driver.findElements(LISTBOX_OPTION)) {
            String t = li.getText().trim(); if (!t.isEmpty()) out.add(t);
        }
        try { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); } catch (Exception ignored) {}
        pause(300);
        return out;
    }

    /** Set rows-per-page to {n} (e.g. "50"); returns true if it now reads {n}. */
    public boolean setRowsPerPage(String n) {
        if (!openSelect(rowsPerPageSelect())) return false;
        By exact = By.xpath("//ul[@role='listbox']//li[@role='option'][normalize-space()='" + n + "']");
        for (int i = 0; i < 8 && driver.findElements(exact).isEmpty(); i++) pause(300);
        List<WebElement> opts = driver.findElements(exact);
        if (opts.isEmpty()) { try { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); } catch (Exception ignored) {} return false; }
        try { new Actions(driver).moveToElement(opts.get(0)).click().perform(); } catch (Exception e) { opts.get(0).click(); }
        pause(1200);
        return n.equals(getRowsPerPageValue());
    }

    public boolean isNextPageEnabled() {
        List<WebElement> b = driver.findElements(By.xpath("//button[@aria-label='Go to next page']"));
        try { return !b.isEmpty() && b.get(0).isEnabled(); } catch (Exception e) { return false; }
    }

    /** Click "Go to next page" if it is enabled; returns false if there is no next page. */
    public boolean goToNextPage() {
        List<WebElement> b = driver.findElements(By.xpath("//button[@aria-label='Go to next page']"));
        if (b.isEmpty() || !b.get(0).isEnabled()) return false;
        try { new Actions(driver).moveToElement(b.get(0)).click().perform(); }
        catch (Exception e) { try { b.get(0).click(); } catch (Exception e2) { js.executeScript("arguments[0].click();", b.get(0)); } }
        pause(1200);
        return true;
    }

    public boolean goToPrevPage() {
        List<WebElement> b = driver.findElements(By.xpath("//button[@aria-label='Go to previous page']"));
        if (b.isEmpty() || !b.get(0).isEnabled()) return false;
        try { new Actions(driver).moveToElement(b.get(0)).click().perform(); }
        catch (Exception e) { try { b.get(0).click(); } catch (Exception e2) { js.executeScript("arguments[0].click();", b.get(0)); } }
        pause(1200);
        return true;
    }

    /** The "Showing X of Y …" footer text (Asset Details / Source-Target / Connection Details). */
    public String getShowingFooter() {
        for (WebElement e : driver.findElements(By.xpath(
                "//*[contains(normalize-space(),'Showing') and (contains(normalize-space(),'asset') "
              + "or contains(normalize-space(),'connection') or contains(normalize-space(),'of '))]"))) {
            try { String t = e.getText().trim(); if (!t.isEmpty() && t.length() < 200) return t; } catch (Exception ignored) {}
        }
        return "";
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
