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

    public boolean hasAssetClassFilter() {
        return !driver.findElements(ASSET_CLASS_SELECT).isEmpty();
    }

    public String getAssetClassValue() {
        List<WebElement> els = driver.findElements(ASSET_CLASS_SELECT);
        return els.isEmpty() ? "" : els.get(0).getText().trim();
    }

    private void openAssetClassDropdown() {
        WebElement sel = wait.until(ExpectedConditions.presenceOfElementLocated(ASSET_CLASS_SELECT));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", sel);
        pause(150);
        // MUI Select opens only on a trusted click.
        try { new Actions(driver).moveToElement(sel).click().perform(); } catch (Exception e) { sel.click(); }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.visibilityOfElementLocated(LISTBOX_OPTION));
        } catch (Exception ignored) {}
        pause(300);
    }

    /** Open the Asset Class dropdown and return its option texts (menu closed afterwards). */
    public List<String> getAssetClassOptions() {
        openAssetClassDropdown();
        List<String> out = new ArrayList<>();
        for (WebElement li : driver.findElements(LISTBOX_OPTION)) {
            String t = li.getText().trim();
            if (!t.isEmpty()) out.add(t);
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

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
