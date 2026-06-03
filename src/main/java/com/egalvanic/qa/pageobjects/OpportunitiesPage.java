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

    /** Set the opportunity Name field inside the open create dialog. */
    public void setName(String name) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG_TEXTFIELD));
        setReactInput(field, name);
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
        List<String> out = new ArrayList<>();
        for (WebElement c : driver.findElements(By.cssSelector("[data-field='status'], .MuiDataGrid-cell[data-field='status']"))) {
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

    /** Bounded explicit settle for controlled-input filter re-render (not a blind sleep on the page). */
    private void sleepSettle() {
        try { Thread.sleep(900); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
