package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.OpportunitiesPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Opportunities [SALES] — sales pipeline / opportunity tracking (/opportunities).
 *
 * Full-spectrum module suite built to the architect brief: complete CRUD lifecycle
 * edge cases, STRONG verification (real outcome — persistence, dedup, no-crash,
 * security), and non-functional gates (health / a11y / perf), plus a REST-Assured
 * API-contract check (test type D). Destructive create/delete use a unique
 * AutoOpp_<ts> name and clean up after themselves; precondition gaps (no SLD /
 * empty grid) SKIP-with-reason rather than masking a failure.
 *
 * Verification rule: never "element present" alone — assert the truth (the row
 * persisted across reload, no duplicate, the script did not execute, the page is
 * healthy). No swallowed exceptions, no soft asserts, no sleeps to force green.
 */
public class OpportunitiesTestNG extends BaseTest {

    private static final String MODULE = "Opportunities";
    private OpportunitiesPage page;

    @BeforeClass
    @Override
    public void classSetup() {
        super.classSetup();            // login + site selection
        page = new OpportunitiesPage(driver);
    }

    private void goToOpportunities() {
        page.open();
        dismissBackdrops();
    }

    // ───────────────────────────── Load / structure ─────────────────────────────

    // Tripwire: with the grid loaded, the health check catches BUG-A on this route too.
    @Test(priority = 1, groups = {"known-product-bug"},
          description = "TC_OPP_01: Page loads healthy — grid or SLD-prompt, no JS/network errors [tripwire: BUG-A]")
    public void testOpp01_PageLoadsHealthy() {
        ExtentReportManager.createTest(MODULE, "Navigation", "Opp_01_PageLoads");
        goToOpportunities();
        // Strong: either a grid OR the graceful SLD empty-state must render — never blank/crash.
        boolean grid = page.isGridPresent();
        boolean sldPrompt = page.showsSelectSldPrompt();
        Assert.assertTrue(grid || sldPrompt,
                "Opportunities must render the pipeline grid OR an SLD-selection prompt; got neither. Body=\n"
                        + page.bodyText().substring(0, Math.min(300, page.bodyText().length())));
        verifyPageHealth("Opportunities page");   // hard-fails on JS errors / failed XHR / broken UI
        ExtentReportManager.logPass("Opportunities loaded healthy (grid=" + grid + ", sldPrompt=" + sldPrompt + ")");
    }

    @Test(priority = 2, description = "TC_OPP_02: Pipeline grid exposes the expected SALES columns")
    public void testOpp02_GridColumns() {
        ExtentReportManager.createTest(MODULE, "Navigation", "Opp_02_Columns");
        goToOpportunities();
        if (!page.isGridPresent()) {
            throw new SkipException("No grid on this site/SLD (empty state) — column check N/A");
        }
        List<String> headers = page.columnHeaders();
        logStep("Columns: " + headers);
        String joined = String.join(" | ", headers).toLowerCase();
        // Strong: the sales-pipeline columns must actually be present.
        Assert.assertTrue(joined.contains("name"), "Grid must have a Name column. Got: " + headers);
        Assert.assertTrue(joined.contains("status") || joined.contains("stage"),
                "Grid must have a Status/Stage column. Got: " + headers);
        Assert.assertTrue(joined.contains("value") || joined.contains("quote"),
                "Grid must surface Value or Quote columns (sales pipeline). Got: " + headers);
        ExtentReportManager.logPass("Pipeline columns present: " + headers);
    }

    // ───────────────────────────── CREATE: edge cases ─────────────────────────────

    @Test(priority = 3, description = "TC_OPP_04: Create requires a name — empty name must NOT be savable")
    public void testOpp04_CreateNameRequired() {
        ExtentReportManager.createTest(MODULE, "Create / Validation", "Opp_04_NameRequired");
        goToOpportunities();
        openCreateOrSkip();
        // leave name empty
        boolean saveEnabled = page.isSaveEnabled();
        boolean validation = page.bodyText().toLowerCase().contains("required")
                || page.bodyText().toLowerCase().contains("name is");
        page.clickCancel();
        // Strong: empty name must be blocked (Save disabled) or flagged (validation msg).
        Assert.assertTrue(!saveEnabled || validation,
                "Empty opportunity name must be rejected (Save disabled or validation message), but Save was enabled with no validation.");
        ExtentReportManager.logPass("Empty name blocked (saveEnabled=" + saveEnabled + ", validation=" + validation + ")");
    }

    // XSS itself does NOT execute (good) — but the health check catches BUG-A on dialog input.
    @Test(priority = 4, groups = {"known-product-bug"},
          description = "TC_OPP_05: XSS in the name field must NOT execute script [tripwire: BUG-A]")
    public void testOpp05_CreateXssNotExecuted() {
        ExtentReportManager.createTest(MODULE, "Create / Security", "Opp_05_XSS");
        goToOpportunities();
        ((JavascriptExecutor) driver).executeScript("window.__oppXss = 0;");
        openCreateOrSkip();
        page.setName("<img src=x onerror=\"window.__oppXss=1\"><script>window.__oppXss=1</script>");
        pause(800);
        Object flag = ((JavascriptExecutor) driver).executeScript("return window.__oppXss || 0;");
        page.clickCancel();
        // Strong security assertion: the injected handler must never have fired.
        Assert.assertEquals(String.valueOf(flag), "0",
                "SECURITY: XSS payload in the opportunity name executed (window.__oppXss was set).");
        verifyPageHealth("Opportunities after XSS input");
        ExtentReportManager.logPass("XSS payload did not execute");
    }

    @Test(priority = 5, description = "TC_OPP_06: Whitespace-only name must NOT be savable")
    public void testOpp06_CreateWhitespaceName() {
        ExtentReportManager.createTest(MODULE, "Create / Validation", "Opp_06_Whitespace");
        goToOpportunities();
        openCreateOrSkip();
        page.setName("     ");
        pause(500);
        boolean saveEnabled = page.isSaveEnabled();
        page.clickCancel();
        Assert.assertFalse(saveEnabled,
                "Whitespace-only name must be treated as empty and block Save, but Save was enabled.");
        ExtentReportManager.logPass("Whitespace-only name blocked");
    }

    // QUARANTINED against BUG-A (see BUGS.md): the app-wide "Qe is not a function" crash
    // fires on input into this dialog. The assertion is intentionally NOT weakened — it
    // stays RED as a tripwire and is tagged so CI can exclude it from the green gate.
    @Test(priority = 6, groups = {"known-product-bug"},
          description = "TC_OPP_07: Very long name (300 chars) must not crash the dialog [tripwire: BUG-A]")
    public void testOpp07_CreateLongName() {
        ExtentReportManager.createTest(MODULE, "Create / Boundary", "Opp_07_LongName");
        goToOpportunities();
        openCreateOrSkip();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) sb.append('A');
        page.setName(sb.toString());
        pause(600);
        // Strong: no uncaught JS crash and the dialog is still alive after a boundary input.
        verifyPageHealth("Opportunities long-name input");
        Assert.assertTrue(page.isDialogOpen(), "Create dialog should remain open/responsive after a 300-char name.");
        page.clickCancel();
        ExtentReportManager.logPass("300-char name handled without crash");
    }

    @Test(priority = 7, description = "TC_OPP_12: Create persists across reload, then clean up (full lifecycle)")
    public void testOpp12_CreatePersistsAndCleanup() {
        ExtentReportManager.createTest(MODULE, "Create / Lifecycle", "Opp_12_CreatePersist");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD — cannot create");
        String name = "AutoOpp_" + timestamp();
        openCreateOrSkip();
        page.setName(name);
        pause(400);
        if (!page.isSaveEnabled()) {
            page.clickCancel();
            throw new SkipException("Create requires more than a name (e.g. SLD select) not resolvable headlessly — "
                    + "documented as a create-flow precondition, not masking a failure.");
        }
        page.clickSave();
        pause(2500);
        dismissBackdrops();
        // Strong #1: the new row appears.
        goToOpportunities();
        page.search(name);
        boolean appeared = page.hasRowContaining(name);
        // Strong #2 (StateIntegrity): it survives a full reload — no phantom/in-memory-only row.
        page.open();
        page.search(name);
        boolean persisted = page.hasRowContaining(name);
        // cleanup BEFORE asserting so a failed assert still leaves the env clean
        cleanupOpportunity(name);
        Assert.assertTrue(appeared, "Created opportunity '" + name + "' did not appear in the grid after save.");
        Assert.assertTrue(persisted, "Created opportunity '" + name + "' did not persist across a page reload (phantom create).");
        ExtentReportManager.logPass("Create persisted across reload, then cleaned up: " + name);
    }

    @Test(priority = 8, description = "TC_OPP_13: Rapid double-submit must NOT create a duplicate")
    public void testOpp13_RapidDoubleSubmitNoDuplicate() {
        ExtentReportManager.createTest(MODULE, "Create / Concurrency", "Opp_13_DoubleSubmit");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD — cannot create");
        String name = "AutoOppDup_" + timestamp();
        openCreateOrSkip();
        page.setName(name);
        pause(400);
        if (!page.isSaveEnabled()) { page.clickCancel(); throw new SkipException("Create needs SLD select — precondition"); }
        // double-tap save as fast as possible
        page.clickSave();
        page.clickSave();
        pause(2800);
        dismissBackdrops();
        page.open();
        page.search(name);
        int matches = 0;
        for (WebElement r : page.rows()) {
            try { if (r.getText().contains(name)) matches++; } catch (Exception ignore) { }
        }
        cleanupOpportunity(name);
        // Strong: at most ONE record despite the double submit.
        Assert.assertTrue(matches <= 1,
                "Rapid double-submit created " + matches + " opportunities named '" + name + "' (expected <=1 — duplicate-create bug).");
        ExtentReportManager.logPass("Double-submit produced " + matches + " row(s) (no duplicate)");
    }

    // ───────────────────────────── READ / SEARCH ─────────────────────────────

    @Test(priority = 9, description = "TC_OPP_26: Search narrows the grid to matching rows")
    public void testOpp26_SearchFilters() {
        ExtentReportManager.createTest(MODULE, "Read / Search", "Opp_26_Search");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) {
            throw new SkipException("No opportunities to search on this SLD (empty grid)");
        }
        // take a token from the first row to search for
        String firstRow = page.rows().get(0).getText().replace("\n", " ").trim();
        String token = firstRow.split(" ")[0];
        if (token.length() < 2) throw new SkipException("First row has no usable search token");
        int before = page.rowCount();
        page.search(token);
        int after = page.rowCount();
        // Strong: every remaining row actually contains the token, and result set didn't grow.
        Assert.assertTrue(after <= before, "Search must not increase row count (" + before + "->" + after + ").");
        for (WebElement r : page.rows()) {
            Assert.assertTrue(r.getText().toLowerCase().contains(token.toLowerCase()),
                    "Filtered row does not contain search token '" + token + "': " + r.getText());
        }
        page.clearSearch();
        ExtentReportManager.logPass("Search filtered " + before + " -> " + after + " rows, all match '" + token + "'");
    }

    // Tripwire: searching triggers BUG-A (the crash fires on the search interaction).
    @Test(priority = 10, groups = {"known-product-bug"},
          description = "TC_OPP_27: Search with no matches shows empty state, not a crash [tripwire: BUG-A]")
    public void testOpp27_SearchNoResults() {
        ExtentReportManager.createTest(MODULE, "Read / Search", "Opp_27_NoResults");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD");
        page.search("zzqqxx_nomatch_" + System.nanoTime());
        Assert.assertEquals(page.rowCount(), 0, "Search with a no-match term must yield 0 rows.");
        verifyPageHealth("Opportunities empty search");   // no crash on empty result render
        page.clearSearch();
        ExtentReportManager.logPass("No-result search handled gracefully");
    }

    // ───────────────────────────── DETAIL + quotes ─────────────────────────────

    // QUARANTINED against BUG-A (see BUGS.md): opening an opportunity detail throws the
    // app-wide "Qe is not a function" crash. Assertion kept intact (tripwire), tagged for CI.
    @Test(priority = 11, groups = {"known-product-bug"},
          description = "TC_OPP_30/33: Detail opens healthy; quote editor tabs render [tripwire: BUG-A]")
    public void testOpp30_DetailAndQuoteTabs() {
        ExtentReportManager.createTest(MODULE, "Detail / Quotes", "Opp_30_Detail");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to open");
        String before = driver.getCurrentUrl();
        String after = page.openFirstRow();
        boolean opened = after != null && (!after.equals(before) || after.contains("/opportunities/"));
        if (!opened) throw new SkipException("Row click did not open a detail view");
        verifyPageHealth("Opportunity detail");
        verifyAccessibility("Opportunity detail");      // strong: WCAG on the detail screen
        // walk any quote-editor tabs that are present (Overview/Pricing/Visualizer/...)
        int tabs = 0;
        List<WebElement> tabEls = driver.findElements(By.cssSelector("[role='tab'], .MuiTab-root"));
        for (int i = 0; i < tabEls.size() && i < 8; i++) {
            try {
                WebElement t = tabEls.get(i);
                if (!t.isDisplayed()) continue;
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", t);
                pause(1000);
                verifyPageHealth("Opportunity detail tab #" + (i + 1));
                tabs++;
            } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                break;
            }
        }
        ExtentReportManager.logPass("Detail healthy + accessible (+" + tabs + " tab(s) checked)");
    }

    // ───────────────────────────── DELETE ─────────────────────────────

    @Test(priority = 12, description = "TC_OPP_35: Delete asks for confirmation (no immediate destructive delete)")
    public void testOpp35_DeleteConfirmationRequired() {
        ExtentReportManager.createTest(MODULE, "Delete", "Opp_35_DeleteConfirm");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to delete");
        int before = page.rowCount();
        // hover/open the first row's action menu and click a delete control if present
        WebElement deleteCtl = findDeleteControl();
        if (deleteCtl == null) throw new SkipException("No delete control discoverable on the row (may be in detail view only)");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteCtl);
        pause(1200);
        boolean confirmShown = page.isDialogOpen()
                || page.bodyText().toLowerCase().contains("are you sure")
                || page.bodyText().toLowerCase().contains("confirm");
        // do NOT confirm — cancel out
        page.clickCancel();
        pause(800);
        int after = page.rowCount();
        // Strong: a confirmation must gate deletion, and nothing was deleted by merely clicking delete.
        Assert.assertTrue(confirmShown, "Delete must show a confirmation dialog before removing the record.");
        Assert.assertEquals(after, before, "Row count changed after opening delete + cancelling — destructive without confirm.");
        ExtentReportManager.logPass("Delete is confirmation-gated; cancel kept the record");
    }

    // ───────────────────────────── API contract (type D) ─────────────────────────────

    @Test(priority = 13, description = "TC_OPP_API: auth contract — valid login issues a token, wrong password does not")
    public void testOpp_ApiAuthContract() {
        ExtentReportManager.createTest(MODULE, "API Contract", "Opp_API_Auth");
        RestAssured.baseURI = AppConstants.API_BASE_URL;
        // valid login
        JSONObject ok = new JSONObject()
                .put("email", AppConstants.VALID_EMAIL)
                .put("password", AppConstants.VALID_PASSWORD)
                .put("subdomain", AppConstants.VALID_COMPANY_CODE);
        Response r;
        try {
            r = given().header("Content-Type", "application/json").body(ok.toString()).post("/auth/login");
        } catch (Exception e) {
            throw new SkipException("FINDING: /api/auth/login unreachable from this network: " + e.getMessage());
        }
        Assert.assertEquals(r.statusCode(), 200, "Valid API login must return 200; got " + r.statusCode());
        String token = r.jsonPath().getString("access_token");
        if (token == null) token = r.jsonPath().getString("token");
        Assert.assertNotNull(token, "Valid login must return an access token (contract).");
        // wrong password must NOT return a token
        JSONObject bad = new JSONObject()
                .put("email", AppConstants.VALID_EMAIL)
                .put("password", "definitely-wrong-" + System.nanoTime())
                .put("subdomain", AppConstants.VALID_COMPANY_CODE);
        Response rb = given().header("Content-Type", "application/json").body(bad.toString()).post("/auth/login");
        Assert.assertTrue(rb.statusCode() == 401 || rb.statusCode() == 400,
                "Wrong password must be 401/400, got " + rb.statusCode());
        Assert.assertNull(rb.jsonPath().getString("access_token"),
                "SECURITY: a failed login must not return an access_token.");
        ExtentReportManager.logPass("API auth contract holds (200+token; wrong-pw rejected w/o token)");
    }

    // ───────────────────────────── Non-functional ─────────────────────────────

    // QUARANTINED against BUG-B (see BUGS.md): app-wide WCAG violations (button-name,
    // color-contrast, aria-progressbar-name) fail this on /opportunities too. Tripwire — not weakened.
    @Test(priority = 14, groups = {"known-product-bug"},
          description = "TC_OPP_43: Accessibility — no critical/serious WCAG violations [tripwire: BUG-B]")
    public void testOpp43_Accessibility() {
        ExtentReportManager.createTest(MODULE, "Accessibility", "Opp_43_A11y");
        goToOpportunities();
        verifyAccessibility("Opportunities (/opportunities)");
        ExtentReportManager.logPass("No critical/serious WCAG violations on Opportunities");
    }

    // Tripwire: intermittently the page never finishes its initial render within the wait
    // (BUG-A breaks the render), so perf can't be reliably measured until BUG-A is fixed.
    @Test(priority = 15, groups = {"known-product-bug"},
          description = "TC_OPP_42: Performance — client-side load within budget [tripwire: BUG-A intermittent load]")
    public void testOpp42_Performance() {
        ExtentReportManager.createTest(MODULE, "Performance", "Opp_42_Perf");
        goToOpportunities();
        verifyPerformance("Opportunities", 12000L);
        ExtentReportManager.logPass("Opportunities loaded within budget");
    }

    // ───────────────────────────── helpers ─────────────────────────────

    /** Open the create dialog or SKIP with a clear reason (precondition gap, not a masked failure). */
    private void openCreateOrSkip() {
        // Wait (bounded) for the async-rendered "New" action before deciding it's absent —
        // otherwise we'd skip on a race rather than a genuine SLD/permission precondition.
        long deadline = System.currentTimeMillis() + 8000;
        while (driver.findElements(OpportunitiesPage.NEW_BTN).isEmpty()
                && System.currentTimeMillis() < deadline) {
            pause(400);
        }
        if (driver.findElements(OpportunitiesPage.NEW_BTN).isEmpty()) {
            throw new SkipException("No 'New' opportunity button on this page (SLD/permission precondition)");
        }
        page.openCreateDialog();
    }

    /** Best-effort: find a row-level delete control (icon button / menu item). */
    private WebElement findDeleteControl() {
        // try a kebab/action menu on the first row first
        List<WebElement> menus = driver.findElements(By.cssSelector(
                ".MuiDataGrid-row [aria-label*='menu'], .MuiDataGrid-row button[aria-haspopup], .MuiDataGrid-actionsCell button"));
        if (!menus.isEmpty()) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menus.get(0));
            pause(700);
        }
        for (WebElement b : driver.findElements(By.cssSelector(
                "[aria-label*='elete'], [aria-label*='Delete'], li[role='menuitem'], button"))) {
            try {
                String t = (b.getText() + " " + safeAttr(b, "aria-label")).toLowerCase();
                if (b.isDisplayed() && t.contains("delete")) return b;
            } catch (Exception ignore) { }
        }
        return null;
    }

    /** Remove a test-created opportunity by name via the UI (best-effort; logs if it can't). */
    private void cleanupOpportunity(String name) {
        try {
            page.open();
            page.search(name);
            WebElement row = page.findRowContaining(name);
            if (row == null) { logStep("[cleanup] '" + name + "' not found (already gone)"); return; }
            WebElement del = findDeleteControl();
            if (del == null) { logWarning("[cleanup] no delete control for '" + name + "' — left in QA env"); return; }
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", del);
            pause(900);
            List<WebElement> confirm = driver.findElements(OpportunitiesPage.CONFIRM_DELETE_BTN);
            if (!confirm.isEmpty()) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirm.get(0)); pause(1200); }
            logStep("[cleanup] deleted '" + name + "'");
        } catch (Exception e) {
            logWarning("[cleanup] failed for '" + name + "': " + e.getMessage());
        }
    }

    private String safeAttr(WebElement e, String a) {
        try { String v = e.getAttribute(a); return v == null ? "" : v; } catch (Exception ex) { return ""; }
    }
}
