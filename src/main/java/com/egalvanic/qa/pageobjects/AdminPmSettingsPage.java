package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.List;

/**
 * Page Object for Settings (/admin) → PM section → PM template config (Offices).
 * ZP-323. Live-mapped 2026-07-02 on acme.qa:
 *
 *   - /admin renders section buttons: Sites | Users | Classes | PM
 *   - PM section breadcrumb: "Settings | PM | Offices"
 *   - Offices table: columns Name + Default Language, search "Search Offices...",
 *     MUI TablePagination footer ("1–6 of 6")
 *   - Row click → "Edit Office" dialog (Name* prefilled, Default Language select,
 *     buttons Delete / Cancel / Save)
 *   - "Add Office" → "Create Office" dialog (Name*, Default Language; Save is
 *     DISABLED until Name is non-empty)
 *   - Language options: "Use login language" | "English" | "Français"
 *   - Language helper text: "When set, this language will be used as the default
 *     for all sites, accounts, and users in this office"
 *
 * All dialog inputs are controlled React — driven via the native value setter
 * (sendKeys does not reliably fire MUI onChange; see ArcFlashPage lesson 2026-07-02).
 */
public class AdminPmSettingsPage {

    private final WebDriver driver;
    private final JavascriptExecutor js;

    public AdminPmSettingsPage(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
    }

    // ================================================================
    // LOCATORS
    // ================================================================

    public static final By ADMIN_NAV_LINK = By.cssSelector("a[href='/admin']");
    public static final By DIALOG = By.cssSelector("div[role='dialog']");
    public static final By ADD_OFFICE_BTN = By.xpath("//button[normalize-space()='Add Office']");

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /** Navigate to Settings (/admin) via the left-nav link, then open the PM section. */
    public boolean navigateToPmSection() {
        if (!driver.getCurrentUrl().contains("/admin")) {
            List<WebElement> links = driver.findElements(ADMIN_NAV_LINK);
            boolean clicked = false;
            for (WebElement a : links) {
                try { if (a.isDisplayed()) { a.click(); clicked = true; break; } } catch (Exception ignored) {}
            }
            if (!clicked) driver.get(com.egalvanic.qa.constants.AppConstants.BASE_URL + "/admin");
        }
        // wait for the section switcher to render
        for (int i = 0; i < 30; i++) {
            if (sectionButton("PM") != null) break;
            pause(1000);
        }
        WebElement pm = sectionButton("PM");
        if (pm == null) { System.out.println("[AdminPmSettingsPage] PM section button not found"); return false; }
        try { new Actions(driver).moveToElement(pm).click().perform(); }
        catch (Exception e) { js.executeScript("arguments[0].click();", pm); }
        boolean on = waitForOffices(20);
        System.out.println("[AdminPmSettingsPage] PM section open=" + on);
        return on;
    }

    /** The Sites/Users/Classes/PM switcher button with this exact visible text. */
    private WebElement sectionButton(String label) {
        return (WebElement) js.executeScript(
            "var l=arguments[0];return [].slice.call(document.querySelectorAll('button'))"
          + ".find(function(b){return (b.textContent||'').trim()===l && b.offsetParent;})||null;", label);
    }

    /** Wait until the Offices area renders (breadcrumb + Add Office button). */
    public boolean waitForOffices(int seconds) {
        for (int i = 0; i < seconds; i++) {
            if (isOnPmOffices()) return true;
            pause(1000);
        }
        return false;
    }

    /** True when the PM → Offices area is visible (breadcrumb PM + Offices + Add Office). */
    public boolean isOnPmOffices() {
        Object r = js.executeScript(
            "var txt=(document.body.innerText||'');"
          + "var addBtn=[].slice.call(document.querySelectorAll('button')).some(function(b){"
          + "  return /^Add Office$/.test((b.textContent||'').trim()) && b.offsetParent;});"
          + "return addBtn && /\\bPM\\b/.test(txt) && /Offices/.test(txt);");
        return Boolean.TRUE.equals(r);
    }

    // ================================================================
    // OFFICES TABLE
    // ================================================================

    /** Visible column headers of the Offices table. */
    @SuppressWarnings("unchecked")
    public List<String> getOfficeTableHeaders() {
        Object out = js.executeScript(
            "var t=[];[].slice.call(document.querySelectorAll('th,[role=\"columnheader\"]')).forEach(function(h){"
          + "if(!h.offsetParent)return;var x=(h.textContent||'').trim();if(x)t.push(x);});return t;");
        return out == null ? new ArrayList<>() : (List<String>) out;
    }

    /**
     * Shared JS: visible office data rows. The offices list renders BOTH a plain <table> and a
     * DataGrid wrapper on this page — match either row shape (tr with td / [role=row] with
     * [role=gridcell]), visible only, header rows excluded.
     */
    private static final String ROWS_JS =
        "var rows=[].slice.call(document.querySelectorAll('table tbody tr, [role=\"row\"]')).filter(function(r){"
      + "  if(!r.offsetParent) return false;"
      + "  if(r.querySelector('th,[role=\"columnheader\"]')) return false;"
      + "  return !!r.querySelector('td,[role=\"gridcell\"]');});";

    /** Names (first cell) of all visible office rows. */
    @SuppressWarnings("unchecked")
    public List<String> getOfficeNames() {
        Object out = js.executeScript(
            ROWS_JS
          + "var t=[];rows.forEach(function(r){var c=r.querySelector('td,[role=\"gridcell\"]');"
          + "if(c)t.push((c.textContent||'').trim());});return t;");
        return out == null ? new ArrayList<>() : (List<String>) out;
    }

    /** The Default Language cell text for the office row with this name ("" if absent). */
    public String getOfficeLanguage(String name) {
        Object out = js.executeScript(
            "var n=arguments[0];" + ROWS_JS
          + "for(var i=0;i<rows.length;i++){var cs=rows[i].querySelectorAll('td,[role=\"gridcell\"]');"
          + "if(cs.length>1 && (cs[0].textContent||'').trim()===n) return (cs[1].textContent||'').trim();}return '';", name);
        return out == null ? "" : out.toString();
    }

    /** The visible MuiTablePagination "X–Y of N" footer text ("" if none). */
    public String getPaginationText() {
        Object out = js.executeScript(
            "var e=[].slice.call(document.querySelectorAll('.MuiTablePagination-displayedRows'))"
          + ".find(function(x){return x.offsetParent;});return e?(e.textContent||'').trim():'';");
        return out == null ? "" : out.toString();
    }

    /** Total office count parsed from the pagination footer (-1 if unparseable). */
    public int getOfficeTotal() {
        String p = getPaginationText();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("of\\s+(\\d+)").matcher(p);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Type into the visible "Search Offices..." box (React value setter). Empty string clears. */
    public boolean searchOffices(String text) {
        Object r = js.executeScript(
            "var tx=arguments[0];"
          + "var inp=[].slice.call(document.querySelectorAll('input')).find(function(i){"
          + "  return /Search Offices/i.test(i.placeholder||'') && i.offsetParent;});"
          + "if(!inp) return false;"
          + "inp.scrollIntoView({block:'center'}); inp.focus();"
          + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
          + "set.call(inp,''); inp.dispatchEvent(new Event('input',{bubbles:true}));"
          + "if(tx){set.call(inp,tx); inp.dispatchEvent(new Event('input',{bubbles:true}));}"
          + "return true;", text);
        pause(1500); // debounce + refetch
        System.out.println("[AdminPmSettingsPage] searchOffices('" + text + "') -> " + r);
        return Boolean.TRUE.equals(r);
    }

    /** Poll until the offices table has rendered at least one data row (rows load async
     *  a beat after the Add Office button appears). */
    public boolean waitForOfficeRows(int seconds) {
        for (int i = 0; i < seconds; i++) {
            if (!getOfficeNames().isEmpty()) return true;
            pause(1000);
        }
        System.out.println("[AdminPmSettingsPage] no office rows rendered after ~" + seconds + "s");
        return false;
    }

    /** Poll until the office row with this name is present (or absent) in the table. */
    public boolean waitForOfficeRow(String name, boolean present, int seconds) {
        for (int i = 0; i < seconds; i++) {
            boolean has = getOfficeNames().contains(name);
            if (has == present) return true;
            pause(1000);
        }
        return false;
    }

    // ================================================================
    // CREATE / EDIT OFFICE DIALOG
    // ================================================================

    /** Click Add Office and wait for the Create Office dialog. */
    public boolean openCreateOffice() {
        List<WebElement> btns = driver.findElements(ADD_OFFICE_BTN);
        WebElement b = btns.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
        if (b == null) return false;
        try { new Actions(driver).moveToElement(b).click().perform(); }
        catch (Exception e) { js.executeScript("arguments[0].click();", b); }
        return waitDialog("Create Office", 10);
    }

    /** Click the office row (first cell) and wait for the Edit Office dialog. */
    public boolean openEditOffice(String name) {
        Object r = js.executeScript(
            "var n=arguments[0];" + ROWS_JS
          + "for(var i=0;i<rows.length;i++){var c=rows[i].querySelector('td,[role=\"gridcell\"]');"
          + "if(c && (c.textContent||'').trim()===n){c.scrollIntoView({block:'center'});c.click();return true;}}return false;", name);
        if (!Boolean.TRUE.equals(r)) { System.out.println("[AdminPmSettingsPage] office row not found: " + name); return false; }
        return waitDialog("Edit Office", 10);
    }

    /** Wait for a dialog whose text contains {title}. */
    public boolean waitDialog(String title, int seconds) {
        for (int i = 0; i < seconds * 2; i++) {
            Object r = js.executeScript(
                "var t=arguments[0];var d=document.querySelector('div[role=\"dialog\"]');"
              + "return !!(d && (d.innerText||'').indexOf(t)>-1);", title);
            if (Boolean.TRUE.equals(r)) { pause(400); return true; }
            pause(500);
        }
        System.out.println("[AdminPmSettingsPage] dialog '" + title + "' did not open");
        return false;
    }

    public boolean isDialogOpen() {
        return !driver.findElements(DIALOG).isEmpty();
    }

    /** Full visible text of the open dialog ("" if none). */
    public String dialogText() {
        Object out = js.executeScript(
            "var d=document.querySelector('div[role=\"dialog\"]');return d?(d.innerText||''):'';");
        return out == null ? "" : out.toString();
    }

    /** Buttons visible in the open dialog. */
    @SuppressWarnings("unchecked")
    public List<String> dialogButtons() {
        Object out = js.executeScript(
            "var d=document.querySelector('div[role=\"dialog\"]');if(!d)return [];var t=[];"
          + "[].slice.call(d.querySelectorAll('button')).forEach(function(b){"
          + "var x=(b.textContent||'').trim();if(x)t.push(x);});return t;");
        return out == null ? new ArrayList<>() : (List<String>) out;
    }

    /** Set the Name field in the open dialog via the React value setter. */
    public boolean setOfficeName(String name) {
        Object r = js.executeScript(
            "var tx=arguments[0];var d=document.querySelector('div[role=\"dialog\"]');if(!d)return false;"
          + "var inp=[].slice.call(d.querySelectorAll('input[type=\"text\"],input:not([type])')).find(function(i){return i.offsetParent;});"
          + "if(!inp) return false;"
          + "inp.focus();"
          + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
          + "set.call(inp,''); inp.dispatchEvent(new Event('input',{bubbles:true}));"
          + "set.call(inp,tx); inp.dispatchEvent(new Event('input',{bubbles:true}));"
          + "return true;", name);
        pause(600);
        System.out.println("[AdminPmSettingsPage] setOfficeName('" + name + "') -> " + r);
        return Boolean.TRUE.equals(r);
    }

    /** Current value of the dialog Name field. */
    public String getOfficeNameValue() {
        Object out = js.executeScript(
            "var d=document.querySelector('div[role=\"dialog\"]');if(!d)return '';"
          + "var inp=[].slice.call(d.querySelectorAll('input[type=\"text\"],input:not([type])')).find(function(i){return i.offsetParent;});"
          + "return inp?inp.value:'';");
        return out == null ? "" : out.toString();
    }

    /** Open the Default Language dropdown in the dialog and return its option labels. */
    @SuppressWarnings("unchecked")
    public List<String> getLanguageOptions() {
        js.executeScript(
            "var d=document.querySelector('div[role=\"dialog\"]');if(!d)return;"
          + "var cb=d.querySelector('[role=\"combobox\"]');"
          + "if(cb)cb.dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));");
        pause(1200);
        Object out = js.executeScript(
            "var t=[];[].slice.call(document.querySelectorAll('li[role=\"option\"]')).forEach(function(o){"
          + "if(o.offsetParent)t.push((o.textContent||'').trim());});return t;");
        List<String> opts = out == null ? new ArrayList<>() : (List<String>) out;
        // close the menu again (Escape on body — the listbox is a portal, not the drawer)
        js.executeScript(
            "var m=document.querySelector('ul[role=\"listbox\"]');"
          + "if(m)document.body.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));");
        pause(600);
        return opts;
    }

    /** Pick a Default Language option by its exact visible label. */
    public boolean selectLanguage(String label) {
        js.executeScript(
            "var d=document.querySelector('div[role=\"dialog\"]');if(!d)return;"
          + "var cb=d.querySelector('[role=\"combobox\"]');"
          + "if(cb)cb.dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));");
        pause(1200);
        Object r = js.executeScript(
            "var l=arguments[0];var o=[].slice.call(document.querySelectorAll('li[role=\"option\"]'))"
          + ".find(function(x){return (x.textContent||'').trim()===l && x.offsetParent;});"
          + "if(!o) return false; o.click(); return true;", label);
        pause(800);
        System.out.println("[AdminPmSettingsPage] selectLanguage('" + label + "') -> " + r);
        return Boolean.TRUE.equals(r);
    }

    /** The language-default helper text is present in the open dialog. */
    public boolean hasLanguageHelperText() {
        return dialogText().contains("used as the default for all sites, accounts, and users");
    }

    /** True if the dialog Save button is enabled. */
    public boolean isSaveEnabled() {
        Object r = js.executeScript(
            "var d=document.querySelector('div[role=\"dialog\"]');if(!d)return null;"
          + "var s=[].slice.call(d.querySelectorAll('button')).find(function(b){return /^Save$/.test((b.textContent||'').trim());});"
          + "return s ? !s.disabled : null;");
        return Boolean.TRUE.equals(r);
    }

    private boolean clickDialogButton(String label) {
        Object r = js.executeScript(
            "var l=arguments[0];var d=document.querySelector('div[role=\"dialog\"]');if(!d)return false;"
          + "var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return (x.textContent||'').trim()===l;});"
          + "if(!b||b.disabled) return false; b.click(); return true;", label);
        System.out.println("[AdminPmSettingsPage] dialog '" + label + "' click -> " + r);
        return Boolean.TRUE.equals(r);
    }

    public boolean clickSave()   { return clickDialogButton("Save"); }
    public boolean clickCancel() { return clickDialogButton("Cancel"); }

    /** Click Delete in the Edit dialog and confirm if a confirmation dialog follows. */
    public boolean clickDeleteAndConfirm() {
        if (!clickDialogButton("Delete")) return false;
        pause(1500);
        // a confirmation dialog may replace the edit dialog — press its destructive button
        js.executeScript(
            "var d=document.querySelector('div[role=\"dialog\"]');if(!d)return;"
          + "var b=[].slice.call(d.querySelectorAll('button')).find(function(x){"
          + "return /^(Delete|Confirm|Yes|Remove)$/i.test((x.textContent||'').trim());});"
          + "if(b)b.click();");
        return waitDialogClosed(15);
    }

    /** Wait until no dialog remains open. */
    public boolean waitDialogClosed(int seconds) {
        for (int i = 0; i < seconds * 2; i++) {
            if (driver.findElements(DIALOG).isEmpty()) return true;
            pause(500);
        }
        System.out.println("[AdminPmSettingsPage] dialog still open");
        return false;
    }
}
