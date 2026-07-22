package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.WorkTypeCatalog;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeClass;

import java.time.Duration;
import java.util.List;

/**
 * Shared base for the per-Work-Type matrix suites (ZP-3000 Services V2, 2026-07-21).
 *
 * All pinned facts (scope counts, fixture WOs, 13+1 type catalog) were live-captured on
 * site {@link WorkTypeCatalog#FIXTURE_SITE} (Z1), so every subclass pins the session to Z1
 * after BaseTest's default "Test Site" login. Site selection uses REAL keystrokes — the
 * tenant's ~130-site virtualised picker ignores JS value-sets (WorkOrderCreateTestNG pattern).
 */
public abstract class WorkTypeUiBase extends BaseTest {

    protected static final String WT_MODULE = "Work Orders";

    /** BaseTest keeps no shared JavascriptExecutor — cast on demand. */
    protected org.openqa.selenium.JavascriptExecutor js() {
        return (org.openqa.selenium.JavascriptExecutor) driver;
    }

    @Override
    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        super.classSetup();
        boolean onZ1 = ensureSiteByKeystrokes(WorkTypeCatalog.FIXTURE_SITE);
        System.out.println("[WorkTypeUiBase] site pinned to " + WorkTypeCatalog.FIXTURE_SITE + ": " + onZ1);
    }

    /**
     * Select a facility/site by exact name using REAL keystrokes (the virtualised picker
     * doesn't react to a JS value-set). Mirrors WorkOrderCreateTestNG/AssetLocationTestNG.
     */
    protected boolean ensureSiteByKeystrokes(String name) {
        By facility = By.xpath("//input[@placeholder='Select facility']");
        try {
            WebElement inp = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(facility));
            if (name.equalsIgnoreCase(String.valueOf(inp.getAttribute("value")))) return true;
            inp.click();
            pause(300);
            Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
            try { inp.sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
            inp.sendKeys(name);
            pause(900);
            String lower = name.toLowerCase();
            By exact = By.xpath("//li[@role='option'][translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='" + lower + "']");
            WebElement opt = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(exact));
            opt.click();
            pause(900);
            return name.equalsIgnoreCase(String.valueOf(driver.findElement(facility).getAttribute("value")));
        } catch (Exception e) {
            logStep("ensureSiteByKeystrokes('" + name + "') failed: " + e.getMessage());
            return false;
        }
    }

    /** Navigate to /sessions (idempotent) and wait for the WO grid. */
    protected void ensureOnWorkOrdersList() {
        String url = String.valueOf(driver.getCurrentUrl());
        if (!url.endsWith("/sessions")) {
            workOrderPage.navigateToWorkOrders();
            pause(1200);
        }
        for (int i = 0; i < 20 && driver.findElements(By.cssSelector("[role='grid']")).isEmpty(); i++) pause(500);
        waitAndDismissAppAlert();
    }

    /**
     * Make sure the Create New Work Order dialog is open, opening it fresh if needed.
     * Matrix rows reuse an already-open dialog (selecting a different Work Type in the same
     * dialog is exactly the user behavior under test and keeps 140 rows CI-fast).
     */
    protected boolean ensureCreateDialogOpen() {
        if (workOrderPage.isCreateWorkOrderDialogOpen()) return true;
        ensureOnWorkOrdersList();
        workOrderPage.openCreateWorkOrderForm();
        for (int i = 0; i < 20 && !workOrderPage.isCreateWorkOrderDialogOpen(); i++) pause(500);
        return workOrderPage.isCreateWorkOrderDialogOpen();
    }

    /** Close the create dialog if open (Cancel), best-effort — used in @AfterClass cleanups. */
    protected void closeCreateDialogIfOpen() {
        if (workOrderPage.isCreateWorkOrderDialogOpen()) {
            workOrderPage.cancelCreateDialog();
        }
        dismissBackdrops();
    }

    /**
     * Select a Work Type and verify the value actually COMMITTED (MUI Autocomplete quirk:
     * raw option clicks set display text without committing React state). Retries once.
     */
    protected boolean selectWorkTypeCommitted(String typeName) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            workOrderPage.selectWorkType(typeName);
            pause(600);
            if (typeName.equals(workOrderPage.getWorkTypeValue())) return true;
            System.out.println("[WorkTypeUiBase] work type '" + typeName + "' did not commit (attempt " + attempt + ")");
        }
        return typeName.equals(workOrderPage.getWorkTypeValue());
    }

    /**
     * Find a WO row by exact name via the list search box with retry — the backend index
     * lags creations by 5-15s. Returns true when a row whose text contains {@code name} renders.
     */
    protected boolean findWorkOrderInGrid(String name, int attempts) {
        ensureOnWorkOrdersList();
        By searchBox = By.xpath("//input[contains(@placeholder,'Search work orders')]");
        for (int i = 0; i < attempts; i++) {
            try {
                WebElement s = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(searchBox));
                js().executeScript(
                        "var i=arguments[0], v=arguments[1];"
                        + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                        + "set.call(i, v); i.dispatchEvent(new Event('input',{bubbles:true}));", s, name);
                pause(2500);
                List<WebElement> rows = driver.findElements(By.cssSelector("[role='rowgroup'] [role='row']"));
                for (WebElement r : rows) {
                    if (r.getText().contains(name)) return true;
                }
            } catch (Exception e) {
                System.out.println("[WorkTypeUiBase] findWorkOrderInGrid attempt " + (i + 1) + " failed: " + e.getMessage());
            }
            pause(2000);
        }
        return false;
    }

    /** Clear the WO list search box (React-native setter; grid restores unfiltered). */
    protected void clearWorkOrderSearch() {
        try {
            WebElement s = driver.findElement(By.xpath("//input[contains(@placeholder,'Search work orders')]"));
            js().executeScript(
                    "var i=arguments[0];"
                    + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "set.call(i, ''); i.dispatchEvent(new Event('input',{bubbles:true}));", s);
            pause(1500);
        } catch (Exception ignored) {}
    }

    /** Click the (already found) WO row by exact name; waits for the /sessions/{id} detail URL. */
    protected boolean openWorkOrderByName(String name) {
        try {
            for (WebElement r : driver.findElements(By.cssSelector("[role='rowgroup'] [role='row']"))) {
                if (r.getText().contains(name)) {
                    js().executeScript("arguments[0].scrollIntoView({block:'center'});", r);
                    pause(200);
                    try { r.click(); } catch (Exception e) { js().executeScript("arguments[0].click();", r); }
                    for (int i = 0; i < 25 && !workOrderPage.isOnWoDetailPage(); i++) pause(400);
                    if (workOrderPage.isOnWoDetailPage()) { pause(2500); return true; }
                }
            }
        } catch (Exception e) { System.out.println("[WorkTypeUiBase] openWorkOrderByName failed: " + e.getMessage()); }
        return false;
    }

    // ---------------- API-side cleanup (UI classes create WOs; delete them via API) ----------------

    private static volatile String wtApiToken;

    /** Bearer token for cleanup calls (POST /api/auth/login, cached per JVM). Null when login fails. */
    protected String workTypeApiToken() {
        if (wtApiToken != null) return wtApiToken;
        try {
            io.restassured.response.Response r = io.restassured.RestAssured.given()
                    .baseUri(com.egalvanic.qa.constants.AppConstants.BASE_URL + "/api")
                    .contentType("application/json")
                    .body("{\"email\":\"" + com.egalvanic.qa.constants.AppConstants.VALID_EMAIL + "\","
                            + "\"password\":\"" + com.egalvanic.qa.constants.AppConstants.VALID_PASSWORD + "\","
                            + "\"subdomain\":\"" + com.egalvanic.qa.constants.AppConstants.VALID_COMPANY_CODE + "\"}")
                    .post("/auth/login");
            String token = r.jsonPath().getString("access_token");
            if (token == null) token = r.jsonPath().getString("token");
            wtApiToken = token;
        } catch (Exception e) {
            System.out.println("[WorkTypeUiBase] API login for cleanup failed: " + e.getMessage());
        }
        return wtApiToken;
    }

    /**
     * Delete a created WO via DELETE /api/ir_session/{id} (live-verified 2026-07-21: requires
     * Content-Type application/json; returns 200 with an async {"_mutation":{"status":"received"}} receipt).
     * Best-effort — cleanup must never fail a test.
     */
    protected boolean apiDeleteWorkOrder(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return false;
        try {
            String token = workTypeApiToken();
            if (token == null) return false;
            int status = io.restassured.RestAssured.given()
                    .baseUri(com.egalvanic.qa.constants.AppConstants.BASE_URL + "/api")
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .body("{}")
                    .delete("/ir_session/" + sessionId)
                    .statusCode();
            System.out.println("[WorkTypeUiBase] cleanup DELETE /ir_session/" + sessionId + " -> " + status);
            return status >= 200 && status < 300;
        } catch (Exception e) {
            System.out.println("[WorkTypeUiBase] apiDeleteWorkOrder failed: " + e.getMessage());
            return false;
        }
    }

    /** Deep-link to a WO detail page and wait for its tab strip. Re-installs health capture (full reload). */
    protected boolean openWorkOrderById(String sessionId) {
        driver.get(com.egalvanic.qa.constants.AppConstants.BASE_URL + "/sessions/" + sessionId);
        reinstallHealthCapture();
        waitAndDismissAppAlert();
        for (int i = 0; i < 30 && driver.findElements(By.xpath("//button[@role='tab']")).isEmpty(); i++) pause(500);
        pause(1500);
        return !driver.findElements(By.xpath("//button[@role='tab']")).isEmpty();
    }
}
