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
 * Page Object for the Accounts [SALES] module (/accounts).
 *
 * Customer accounts (the third SALES nav item alongside Goals + Opportunities).
 * Grid columns: Account Name, Owner, Created, Actions. Create dialog ("Create New
 * Account") collects Account Name, Subdomain, Account Owner, and a full Address.
 * The account detail (/accounts/:id) carries tabs: Details, Internal Team, Portal
 * Access, Contacts, Opportunities, Sites, Goals, Notes — Contacts are what an
 * Opportunity quote's "Recipient" pulls from (/accounts/:id?tab=contacts).
 *
 * NOTE (test safety): creating an account collects a Subdomain (tenant-ish) so the
 * suite does NOT submit new accounts — it exercises create-dialog VALIDATION and
 * cancels. Read/search/detail run against existing accounts; delete is confirmation-
 * gated and cancelled (never deletes shared data).
 *
 * Patterns mirror OpportunitiesPage: CSS-first locators, relative XPath only for MUI
 * text buttons, explicit waits (no blind sleeps that force green), tolerant JS clicks.
 */
public class AccountsPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public static final By GRID = By.cssSelector(".MuiDataGrid-root, [role='grid']");
    public static final By GRID_ROW = By.cssSelector(".MuiDataGrid-row");
    public static final By COLUMN_HEADER = By.cssSelector(".MuiDataGrid-columnHeaderTitle, [role='columnheader']");
    public static final By SEARCH_INPUT = By.cssSelector(
            "input[type='search'], input[placeholder*='Search'], input[placeholder*='search']");
    public static final By DIALOG = By.cssSelector("[role='dialog'], .MuiDialog-root");
    public static final By DIALOG_TEXTFIELD = By.cssSelector("[role='dialog'] input[type='text'], .MuiDialog-root input:not([type='hidden'])");

    public static final By NEW_BTN = By.xpath(
            "//button[normalize-space()='New Account' or normalize-space()='New' or normalize-space()='Add Account' or normalize-space()='+ New']");
    public static final By SAVE_BTN = By.xpath(
            "//div[@role='dialog']//button[normalize-space()='Create' or normalize-space()='Save' or normalize-space()='Add' or normalize-space()='Save Changes']");
    public static final By CANCEL_BTN = By.xpath(
            "//div[@role='dialog']//button[normalize-space()='Cancel' or normalize-space()='Close' or normalize-space()='Discard']");
    public static final By CONFIRM_DELETE_BTN = By.xpath(
            "//div[@role='dialog']//button[normalize-space()='Delete' or normalize-space()='Confirm' or normalize-space()='Yes' or normalize-space()='Remove']");

    public AccountsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public void open() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            driver.get(AppConstants.BASE_URL + "/accounts");
            try {
                waitForLoaded();
                waitForContent();
                return;
            } catch (org.openqa.selenium.TimeoutException te) {
                if (attempt == 2) throw te;
            }
        }
    }

    public void waitForLoaded() {
        wait.until(d -> {
            Object ready = ((JavascriptExecutor) d).executeScript(
                "var shell=document.querySelector('header,[role=banner],nav,.MuiDrawer-root,.MuiAppBar-root');"
                + "var t=(document.body&&document.body.innerText)||'';"
                + "return !!shell && t.length>40 && !/^\\s*Loading/i.test(t);");
            return Boolean.TRUE.equals(ready);
        });
    }

    /** Wait for real Accounts content — the grid, the search box, or a definitive empty state. */
    public void waitForContent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25)).until(d -> {
                if (!d.findElements(GRID).isEmpty()) return true;
                if (!d.findElements(SEARCH_INPUT).isEmpty()) return true;
                String t = bodyText().toLowerCase();
                return t.contains("no account") || t.contains("no records")
                        || t.contains("get started") || t.contains("create your first");
            });
        } catch (org.openqa.selenium.TimeoutException te) {
            // do not swallow into a pass — the caller's assertion surfaces a genuine "never loaded".
        }
        settle();
    }

    public boolean isGridPresent() { return !driver.findElements(GRID).isEmpty(); }

    public String bodyText() {
        Object b = ((JavascriptExecutor) driver).executeScript(
                "return (document.body && document.body.innerText) || '';");
        return b == null ? "" : b.toString();
    }

    public List<String> columnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement h : driver.findElements(COLUMN_HEADER)) {
            String t = h.getText().trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    public List<WebElement> rows() { return driver.findElements(GRID_ROW); }
    public int rowCount() { return rows().size(); }

    /** React-native value setter — sendKeys often won't trigger the controlled-input filter. */
    public void search(String term) {
        WebElement box = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
        setReactInput(box, term);
        settle();
    }

    public void clearSearch() {
        WebElement box = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
        setReactInput(box, "");
        settle();
    }

    public boolean isNewButtonPresent() { return !driver.findElements(NEW_BTN).isEmpty(); }

    public void openCreateDialog() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(NEW_BTN));
        jsClick(btn);
        wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
        settle();
    }

    public boolean isDialogOpen() {
        return driver.findElements(DIALOG).stream().anyMatch(WebElement::isDisplayed);
    }

    /** Visible text of the open dialog ("" if none). */
    public String dialogText() {
        WebElement d = driver.findElements(DIALOG).stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
        try { return d == null ? "" : d.getText(); } catch (Exception e) { return ""; }
    }

    /** Labels of required fields ("*") in the open create dialog. */
    public List<String> requiredFieldLabels() {
        List<String> out = new ArrayList<>();
        for (WebElement l : driver.findElements(By.cssSelector("[role='dialog'] label"))) {
            try {
                String t = l.getText().trim();
                if (t.contains("*")) out.add(t.replace("*", "").trim());
            } catch (Exception ignore) { }
        }
        return out;
    }

    /** Set the "Account Name" field (first text field) with real keystrokes so validation gates Create. */
    public void setAccountName(String name) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
        WebElement field = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var dlg=document.querySelector('[role=\"dialog\"],.MuiDialog-root'); if(!dlg) return null;"
          + "var target=null;"
          + "dlg.querySelectorAll('.MuiFormControl-root,.MuiTextField-root').forEach(function(c){"
          + "  var l=c.querySelector('label'); var i=c.querySelector('input,textarea');"
          + "  if(i && l && /account name|^\\s*name/i.test(l.textContent||'') "
          + "     && i.getAttribute('role')!=='combobox' && !target) target=i; });"
          + "if(!target){var ins=dlg.querySelectorAll(\"input[type='text']\");"
          + "  for(var k=0;k<ins.length;k++){var i=ins[k];"
          + "    if(i.getAttribute('role')!=='combobox' && !i.closest('.MuiAutocomplete-root')){target=i;break;}}}"
          + "return target;");
        if (field == null) field = wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG_TEXTFIELD));
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", field);
            field.click();
            String existing = field.getAttribute("value");
            if (existing != null && !existing.isEmpty()) {
                field.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
                field.sendKeys(org.openqa.selenium.Keys.DELETE);
            }
            field.sendKeys(name);
        } catch (Exception e) {
            setReactInput(field, name);
        }
    }

    public boolean isSaveEnabled() {
        List<WebElement> b = driver.findElements(SAVE_BTN);
        if (b.isEmpty()) return false;
        WebElement s = b.get(0);
        return s.isEnabled() && s.getAttribute("disabled") == null
                && !"true".equals(s.getAttribute("aria-disabled"));
    }

    public boolean waitForSaveEnabled(long ms) {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            if (isSaveEnabled()) return true;
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        return isSaveEnabled();
    }

    public void clickCancel() {
        List<WebElement> c = driver.findElements(CANCEL_BTN);
        if (!c.isEmpty()) jsClick(c.get(0));
        settle();
    }

    public WebElement findRowContaining(String name) {
        for (WebElement r : rows()) {
            try { if (r.getText().contains(name)) return r; } catch (Exception ignore) { }
        }
        return null;
    }

    public boolean hasRowContaining(String name) { return findRowContaining(name) != null; }

    /** Open the first row's account detail; returns the URL after navigation. */
    public String openFirstRow() {
        List<WebElement> rs = rows();
        if (rs.isEmpty()) return null;
        WebElement cell = rs.get(0).findElements(By.cssSelector(".MuiDataGrid-cell, a")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(rs.get(0));
        jsClick(cell);
        settle();
        return driver.getCurrentUrl();
    }

    /** Tab labels present on the current (account detail) screen. */
    public List<String> tabLabels() {
        List<String> out = new ArrayList<>();
        for (WebElement t : driver.findElements(By.cssSelector("[role='tab'], .MuiTab-root"))) {
            String s = t.getText().trim();
            if (!s.isEmpty() && !out.contains(s)) out.add(s);
        }
        return out;
    }

    /** Poll until a detail tab whose label contains `label` (case-insensitive) renders (BUG-A async lag). */
    public boolean waitForTabContaining(String label, long ms) {
        String want = label.toLowerCase();
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            for (String t : tabLabels()) { if (t.toLowerCase().contains(want)) return true; }
            try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        for (String t : tabLabels()) { if (t.toLowerCase().contains(want)) return true; }
        return false;
    }

    /** Click a detail tab by visible label (e.g. "Contacts"); returns true if found+clicked. */
    public boolean clickTab(String label) {
        WebElement tab = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var want=arguments[0].toLowerCase();"
          + "var ts=document.querySelectorAll('[role=tab],.MuiTab-root');"
          + "for(var i=0;i<ts.length;i++){ if((ts[i].textContent||'').trim().toLowerCase()===want) return ts[i]; }"
          + "return null;", label);
        if (tab == null) return false;
        jsClick(tab);
        settle();
        return true;
    }

    /** Row-level delete control (Actions-column trash icon, else kebab→Delete). Null if none. */
    public WebElement findDeleteControl() {
        WebElement icon = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var row=document.querySelector('.MuiDataGrid-row'); if(!row) return null;"
          + "var svg=row.querySelector('[data-testid*=\"Delete\"],[data-testid*=\"delete\"],[data-testid*=\"Trash\"]');"
          + "if(svg){return svg.closest('button')||svg;}"
          + "var act=row.querySelector('.MuiDataGrid-cell[data-field=\"actions\"],.MuiDataGrid-actionsCell');"
          + "if(act){var b=act.querySelector('button'); if(b) return b;}"
          + "var aria=row.querySelector('[aria-label*=\"elete\" i],[title*=\"elete\" i]');"
          + "if(aria) return aria;"
          + "return null;");
        return icon;
    }

    public List<String> columnValues(String dataField) {
        List<String> out = new ArrayList<>();
        for (WebElement c : driver.findElements(By.cssSelector(".MuiDataGrid-cell[data-field='" + dataField + "']"))) {
            String t = c.getText().trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
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

    private void settle() {
        try { Thread.sleep(900); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
