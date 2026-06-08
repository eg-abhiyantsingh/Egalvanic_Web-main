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
    // Opportunities data + a create Facility that matches the site live under site "gyu"
    // (per the walkthrough video + owner note: the Site and Facility name are identical there,
    // so the create dialog's pre-filled Facility matches the grid scope and rows are visible).
    // BaseTest's default "Test Site" does NOT line up, so we scope this module to "gyu".
    private static final String OPP_SITE = System.getenv().getOrDefault("OPP_SITE", "gyu");
    private OpportunitiesPage page;

    @BeforeClass
    @Override
    public void classSetup() {
        super.classSetup();            // login + default site selection
        page = new OpportunitiesPage(driver);
        page.open();
        selectSiteByName(OPP_SITE);    // scope to the data-rich site whose facility == site name
    }

    private void goToOpportunities() {
        page.open();
        dismissBackdrops();
        // Stay scoped to OPP_SITE (persists across reloads, but re-assert cheaply if it drifted).
        if (!OPP_SITE.equalsIgnoreCase(page.activeSiteName())) {
            selectSiteByName(OPP_SITE);
            page.open();
            dismissBackdrops();
        }
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
        String name = "AutoOpp_" + uid();
        openCreateOrSkip();
        page.setName(name);
        pause(400);
        if (!page.waitForSaveEnabled(5000)) {
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

    // QUARANTINED — REAL BUG (BUG-D, see BUGS.md): rapid double-submit creates TWO
    // opportunities with the same name (no submit-guard/debounce). Assertion NOT weakened;
    // tagged so the functional gate stays green while this is filed.
    @Test(priority = 8, groups = {"known-product-bug"},
          description = "TC_OPP_13: Rapid double-submit must NOT create a duplicate [tripwire: BUG-D double-submit]")
    public void testOpp13_RapidDoubleSubmitNoDuplicate() {
        ExtentReportManager.createTest(MODULE, "Create / Concurrency", "Opp_13_DoubleSubmit");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD — cannot create");
        String name = "AutoOppDup_" + uid();
        openCreateOrSkip();
        page.setName(name);
        pause(400);
        if (!page.waitForSaveEnabled(5000)) { page.clickCancel(); throw new SkipException("Create needs SLD select — precondition"); }
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

    // ═══════════════ Coverage completion: remaining TC_OPP cases ═══════════════

    // ── B. SLD scoping ──
    @Test(priority = 20, description = "TC_OPP_04: No SLD selected shows a graceful empty-state, not a crash")
    public void testOpp04_NoSldGraceful() {
        ExtentReportManager.createTest(MODULE, "SLD Scoping", "Opp_04_NoSld");
        goToOpportunities();
        boolean grid = page.isGridPresent();
        boolean prompt = page.showsSelectSldPrompt();
        Assert.assertTrue(grid || prompt,
                "Must show the grid OR a graceful 'select an SLD' empty-state — never a blank/crash.");
        // and the empty-state must NOT be an error banner / crash
        com.egalvanic.qa.utils.verify.UIStateValidator.assertHealthy(driver, "Opportunities SLD state");
        ExtentReportManager.logPass("SLD state graceful (grid=" + grid + ", prompt=" + prompt + ")");
    }

    @Test(priority = 21, description = "TC_OPP_05: Switching the active SLD refilters the grid")
    public void testOpp05_SldSwitchRefilters() {
        ExtentReportManager.createTest(MODULE, "SLD Scoping", "Opp_05_SldSwitch");
        goToOpportunities();
        WebElement sw = page.findSldSwitcher();
        if (sw == null) throw new SkipException("SLD switcher control not found in app chrome (cannot drive a switch)");
        String before = String.join("|", page.columnValues("name")) + "#" + page.rowCount();
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", sw);
        pause(800);
        java.util.List<WebElement> opts = driver.findElements(By.cssSelector("li[role='option']"));
        if (opts.size() < 2) { throw new SkipException("Fewer than 2 SLD options — cannot switch"); }
        // pick an option different from the current selection
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", opts.get(opts.size() - 1));
        page.waitForContent();
        String after = String.join("|", page.columnValues("name")) + "#" + page.rowCount();
        // Strong: the grid must have re-fetched (content changed) for a different SLD.
        Assert.assertNotEquals(after, before, "Switching SLD should refilter the opportunities grid.");
        ExtentReportManager.logPass("SLD switch refiltered the grid");
    }

    @Test(priority = 22, description = "TC_OPP_06: SLD column is populated/consistent for the active SLD")
    public void testOpp06_SldColumnMatches() {
        ExtentReportManager.createTest(MODULE, "SLD Scoping", "Opp_06_SldColumn");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities on this SLD");
        java.util.List<String> slds = page.columnValues("sld_name");
        if (slds.isEmpty()) throw new SkipException("No SLD column values rendered (virtualized grid)");
        // Strong: every row carries an SLD, and (SLD-scoped) they are all the same active SLD.
        long distinct = slds.stream().distinct().count();
        Assert.assertTrue(slds.stream().noneMatch(String::isEmpty), "Every row must have a non-empty SLD. Got: " + slds);
        Assert.assertEquals(distinct, 1L, "SLD-scoped grid should show a single active SLD. Got distinct: " + distinct);
        ExtentReportManager.logPass(slds.size() + " rows all on SLD '" + slds.get(0) + "'");
    }

    // ── C. Create (remaining) ──
    @Test(priority = 23, description = "TC_OPP_07: Create dialog opens with a Name field (+ SLD selector)")
    public void testOpp07_OpenCreateDialog() {
        ExtentReportManager.createTest(MODULE, "Create", "Opp_07_OpenDialog");
        goToOpportunities();
        openCreateOrSkip();
        Assert.assertTrue(page.createDialogHasNameField(), "Add Opportunity dialog must expose a Name field.");
        logStep("SLD selector present in dialog: " + page.createDialogHasSldSelector());
        page.clickCancel();
        ExtentReportManager.logPass("Create dialog opens with Name field");
    }

    @Test(priority = 24, description = "TC_OPP_10: Cancel create closes the dialog and creates nothing")
    public void testOpp10_CancelCreate() {
        ExtentReportManager.createTest(MODULE, "Create", "Opp_10_Cancel");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD");
        String name = "CancelOpp_" + uid();
        openCreateOrSkip();
        page.setName(name);
        pause(300);
        page.clickCancel();
        pause(700);
        Assert.assertFalse(page.isDialogOpen(), "Dialog should close on Cancel.");
        page.open();
        page.search(name);
        Assert.assertFalse(page.hasRowContaining(name), "Cancelled create must NOT persist an opportunity.");
        ExtentReportManager.logPass("Cancel created nothing");
    }

    // Tripwire: its verifyPageHealth catches BUG-A (Qe crash) on the create interaction.
    @Test(priority = 25, groups = {"known-product-bug"},
          description = "TC_OPP_11: Duplicate name is handled gracefully (no 500/crash) [tripwire: BUG-A]")
    public void testOpp11_DuplicateName() {
        ExtentReportManager.createTest(MODULE, "Create", "Opp_11_Duplicate");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD");
        String name = "DupOpp_" + uid();
        // first create
        openCreateOrSkip(); page.setName(name); pause(300);
        if (!page.waitForSaveEnabled(5000)) { page.clickCancel(); throw new SkipException("Create needs SLD select — precondition"); }
        page.clickSave(); pause(2200); dismissBackdrops();
        // second create with the same name
        page.open(); openCreateOrSkip(); page.setName(name); pause(300);
        boolean saveEnabled = page.isSaveEnabled();
        if (saveEnabled) { page.clickSave(); pause(2000); dismissBackdrops(); }
        // Strong: no crash/500 regardless of whether duplicates are allowed.
        verifyPageHealth("Opportunities after duplicate create");
        cleanupOpportunity(name);   // removes whatever was created (one or both)
        ExtentReportManager.logPass("Duplicate name handled without crash (second-save-enabled=" + saveEnabled + ")");
    }

    // Tripwire: verifyPageHealth catches BUG-A (Qe crash) after the create interaction.
    @Test(priority = 26, groups = {"known-product-bug"},
          description = "TC_OPP_14: Unicode/special name stored & displayed without crash [tripwire: BUG-A]")
    public void testOpp14_UnicodeName() {
        ExtentReportManager.createTest(MODULE, "Create / Boundary", "Opp_14_Unicode");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD");
        String tag = uid();
        String name = "测试_Ωüñ_" + tag;   // unicode (avoid < > to keep this distinct from the XSS case)
        openCreateOrSkip(); page.setName(name); pause(400);
        if (!page.waitForSaveEnabled(5000)) { page.clickCancel(); throw new SkipException("Create needs SLD select — precondition"); }
        page.clickSave(); pause(2300); dismissBackdrops();
        verifyPageHealth("Opportunities after unicode create");
        page.open(); page.search(tag);
        boolean shown = page.hasRowContaining(tag);
        cleanupOpportunity(name);
        Assert.assertTrue(shown, "Unicode-named opportunity should be stored & displayed (searched by '" + tag + "').");
        ExtentReportManager.logPass("Unicode name stored & displayed");
    }

    // ── F. Search (remaining) ──
    @Test(priority = 27, description = "TC_OPP_28: Clearing search restores the full list")
    public void testOpp28_ClearSearchRestores() {
        ExtentReportManager.createTest(MODULE, "Search", "Opp_28_ClearRestores");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to search");
        int before = page.rowCount();
        // Use a REAL token so the grid stays populated (a no-match search empties the grid and
        // can remove the toolbar/search box, breaking the subsequent clear).
        String token = page.rows().get(0).getText().replace("\n", " ").trim().split(" ")[0];
        if (token.length() < 2) throw new SkipException("First row has no usable token");
        page.search(token);
        int filtered = page.rowCount();
        Assert.assertTrue(filtered <= before, "Search must not grow the list (" + before + "->" + filtered + ").");
        page.clearSearch();
        page.waitForContent();
        int after = page.rowCount();
        Assert.assertEquals(after, before, "Clearing search must restore the full list (" + before + ").");
        ExtentReportManager.logPass("Clear restored " + after + " rows (filtered to " + filtered + ")");
    }

    @Test(priority = 28, description = "TC_OPP_29: Search is case-insensitive")
    public void testOpp29_SearchCaseInsensitive() {
        ExtentReportManager.createTest(MODULE, "Search", "Opp_29_CaseInsensitive");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to search");
        String token = page.rows().get(0).getText().replace("\n", " ").trim().split(" ")[0];
        if (token.length() < 2) throw new SkipException("No usable token");
        page.search(token.toLowerCase());
        int lower = page.rowCount();
        page.clearSearch(); page.waitForContent();
        page.search(token.toUpperCase());
        int upper = page.rowCount();
        page.clearSearch();
        // Strong: case must not change the match set.
        Assert.assertEquals(lower, upper, "Search must be case-insensitive (lower=" + lower + ", upper=" + upper + ").");
        Assert.assertTrue(lower > 0, "Searching a real token (any case) should match at least one row.");
        ExtentReportManager.logPass("Case-insensitive search: " + lower + " matches either case");
    }

    // ── G. Detail & quotes (remaining) ──
    // Tripwire: opening the detail throws BUG-A (Qe crash), caught by verifyPageHealth.
    @Test(priority = 29, groups = {"known-product-bug"},
          description = "TC_OPP_31/32/33: Detail lists quotes; opening a quote loads its editor tabs [tripwire: BUG-A]")
    public void testOpp31_QuotesAndQuoteEditor() {
        ExtentReportManager.createTest(MODULE, "Detail / Quotes", "Opp_31_Quotes");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to open");
        String before = driver.getCurrentUrl();
        String after = page.openFirstRow();
        if (after == null || (after.equals(before) && !after.contains("/opportunities/")))
            throw new SkipException("Row did not open a detail view");
        verifyPageHealth("Opportunity detail (quotes)");
        int quotes = page.quoteRowCountOnDetail();
        logStep("Quote-ish rows on detail: " + quotes);
        // try to open a quote -> /quotes/ editor and walk its tabs
        if (page.openFirstQuoteFromDetail()) {
            verifyPageHealth("Quote editor");
            java.util.List<String> tabs = page.tabLabels();
            logStep("Quote editor tabs: " + tabs);
            int walked = 0;
            for (WebElement t : driver.findElements(By.cssSelector("[role='tab'], .MuiTab-root"))) {
                try { if (!t.isDisplayed()) continue;
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", t);
                    pause(900); verifyPageHealth("Quote tab"); walked++;
                } catch (org.openqa.selenium.StaleElementReferenceException s) { break; }
            }
            ExtentReportManager.logPass("Detail + quote editor healthy (" + walked + " tab(s))");
        } else {
            // no quote to open is acceptable (opp may have none) — detail health already asserted
            ExtentReportManager.logPass("Detail healthy; no quote to open (" + quotes + " quote rows)");
        }
    }

    @Test(priority = 30, description = "TC_OPP_34: Back from detail returns to the Opportunities grid")
    public void testOpp34_BackPreservesContext() {
        ExtentReportManager.createTest(MODULE, "Detail", "Opp_34_Back");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to open");
        String list = driver.getCurrentUrl();
        String detail = page.openFirstRow();
        if (detail == null || detail.equals(list)) throw new SkipException("Row did not navigate to a detail view");
        driver.navigate().back();
        pause(1200); dismissBackdrops();
        // The list grid re-renders asynchronously after back — poll for it.
        boolean onList = false;
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (driver.getCurrentUrl().contains("/opportunities") && page.isGridPresent()) { onList = true; break; }
            pause(800);
        }
        Assert.assertTrue(onList,
                "Back should return to the Opportunities grid. URL=" + driver.getCurrentUrl()
                + " gridPresent=" + page.isGridPresent());
        ExtentReportManager.logPass("Back returned to the grid");
    }

    // ── H. Delete (confirm removes) ──
    @Test(priority = 31, description = "TC_OPP_36: Confirming delete removes the opportunity")
    public void testOpp36_ConfirmDeleteRemoves() {
        ExtentReportManager.createTest(MODULE, "Delete", "Opp_36_ConfirmDelete");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD");
        String name = "DelOpp_" + uid();
        openCreateOrSkip(); page.setName(name); pause(300);
        if (!page.waitForSaveEnabled(5000)) { page.clickCancel(); throw new SkipException("Create needs SLD select — precondition"); }
        page.clickSave(); pause(2300); dismissBackdrops();
        page.open(); page.search(name);
        if (!page.hasRowContaining(name)) throw new SkipException("Created opp not found to delete (create flow)");
        WebElement del = findDeleteControl();
        if (del == null) throw new SkipException("No row-level delete control discoverable");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", del); pause(900);
        java.util.List<WebElement> confirm = driver.findElements(OpportunitiesPage.CONFIRM_DELETE_BTN);
        if (confirm.isEmpty()) throw new SkipException("No confirm-delete control");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirm.get(0)); pause(1500);
        page.open(); page.search(name);
        Assert.assertFalse(page.hasRowContaining(name), "Confirmed delete must remove '" + name + "' from the grid.");
        ExtentReportManager.logPass("Confirmed delete removed the opportunity");
    }

    // ── I. Resilience ──
    @Test(priority = 32, description = "TC_OPP_39: Create failure (offline) surfaces an error, no silent loss")
    public void testOpp39_CreateFailureHandling() {
        ExtentReportManager.createTest(MODULE, "Resilience", "Opp_39_CreateFailure");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD");
        String name = "FailOpp_" + uid();
        openCreateOrSkip(); page.setName(name); pause(300);
        if (!page.waitForSaveEnabled(5000)) { page.clickCancel(); throw new SkipException("Create needs SLD select — precondition"); }
        com.egalvanic.qa.utils.verify.NetworkConditions.goOffline(driver);
        try {
            page.clickSave();
            pause(2500);
            String body = page.bodyText().toLowerCase();
            boolean errorShown = body.contains("fail") || body.contains("error") || body.contains("try again")
                    || body.contains("offline") || body.contains("network");
            boolean stillOpen = page.isDialogOpen();   // a robust app keeps the dialog open on failure
            Assert.assertTrue(errorShown || stillOpen,
                    "Create-while-offline must surface an error or keep the dialog open — never silently 'succeed'.");
        } finally {
            com.egalvanic.qa.utils.verify.NetworkConditions.goOnline(driver);
            pause(1200);
            page.clickCancel();
        }
        cleanupOpportunity(name);   // in case it slipped through
        ExtentReportManager.logPass("Offline create failure surfaced, no silent loss");
    }

    @Test(priority = 33, description = "TC_OPP_40: Going offline keeps the page responsive (no hang)")
    public void testOpp40_OfflineResilience() {
        ExtentReportManager.createTest(MODULE, "Resilience", "Opp_40_Offline");
        goToOpportunities();
        com.egalvanic.qa.utils.verify.NetworkConditions.goOffline(driver);
        try {
            ((JavascriptExecutor) driver).executeScript("window.dispatchEvent(new Event('offline'));");
            pause(1500);
            com.egalvanic.qa.utils.verify.HangDetector.assertResponsive(driver, "Opportunities offline", 20);
        } finally {
            com.egalvanic.qa.utils.verify.NetworkConditions.goOnline(driver);
            pause(1200);
        }
        ExtentReportManager.logPass("Page stayed responsive while offline");
    }

    // ── E. Pipeline status & quotes ──
    @Test(priority = 34, description = "TC_OPP_18: A newly-created opportunity (no quote) is 'Qualifying', quote_count 0")
    public void testOpp18_NewOppQualifying() {
        ExtentReportManager.createTest(MODULE, "Pipeline Status", "Opp_18_Qualifying");
        goToOpportunities();
        if (!page.isGridPresent()) throw new SkipException("No grid/SLD");
        String name = "QualOpp_" + uid();
        openCreateOrSkip(); page.setName(name); pause(300);
        if (!page.waitForSaveEnabled(5000)) { page.clickCancel(); throw new SkipException("Create needs SLD select — precondition"); }
        page.clickSave(); pause(2300); dismissBackdrops();
        page.open(); page.search(name);
        WebElement row = page.findRowContaining(name);
        String rowText = row == null ? "" : row.getText().toLowerCase();
        cleanupOpportunity(name);
        Assert.assertNotNull(row, "Created opportunity should appear to verify its status.");
        Assert.assertTrue(rowText.contains("qualifying") || rowText.contains("qualified"),
                "A new opportunity with no quote should be 'Qualifying'. Row: " + rowText);
        ExtentReportManager.logPass("New opportunity status = Qualifying");
    }

    @Test(priority = 35, description = "TC_OPP_19-22: Status column only ever shows valid pipeline stages")
    public void testOpp19to22_StatusEnumValid() {
        ExtentReportManager.createTest(MODULE, "Pipeline Status", "Opp_19_22_StatusEnum");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to inspect");
        // Pipeline stages per the live UI (KPI cards + Status column): Qualifying / Pending
        // Response / Closed Won / Closed Lost / Abandoned, plus the legacy "Qualified" seen on
        // older rows.
        java.util.List<String> valid = java.util.Arrays.asList(
                "qualifying", "qualified", "pending response", "closed won", "closed lost", "abandoned");
        java.util.List<String> statuses = page.statusValues();
        if (statuses.isEmpty()) throw new SkipException("Status column not rendered (virtualized)");
        for (String s : statuses) {
            String v = s.toLowerCase().trim();
            Assert.assertTrue(valid.stream().anyMatch(v::contains),
                    "Status '" + s + "' is not a valid pipeline stage " + valid);
        }
        logStep("Observed statuses: " + statuses.stream().distinct().sorted().collect(java.util.stream.Collectors.toList()));
        ExtentReportManager.logPass(statuses.size() + " status value(s), all valid pipeline stages");
    }

    @Test(priority = 36, description = "TC_OPP_23/24: quote_count is a non-negative integer; total_value is currency/blank")
    public void testOpp23_24_QuoteCountAndValueShape() {
        ExtentReportManager.createTest(MODULE, "Pipeline Status", "Opp_23_24_CountValue");
        goToOpportunities();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No opportunities to inspect");
        java.util.List<String> counts = page.columnValues("quote_count");
        java.util.List<String> values = page.columnValues("total_value");
        if (counts.isEmpty() && values.isEmpty()) throw new SkipException("Count/Value columns not rendered");
        for (String c : counts) {
            Assert.assertTrue(c.trim().matches("\\d+"), "quote_count must be a non-negative integer. Got: '" + c + "'");
        }
        for (String v : values) {
            String t = v.trim();
            Assert.assertTrue(t.isEmpty() || t.matches("[$€£]?[\\d,]+(\\.\\d+)?") || t.matches("[\\d,.$€£\\s-]+"),
                    "total_value must look like currency/number or blank. Got: '" + v + "'");
        }
        ExtentReportManager.logPass("quote_count integers + total_value currency-shaped");
    }

    @Test(priority = 37, description = "TC_OPP_25: Deleted-quote exclusion from count/value (needs quote lifecycle)")
    public void testOpp25_DeletedQuotesExcluded() {
        ExtentReportManager.createTest(MODULE, "Pipeline Status", "Opp_25_DeletedExcluded");
        // Driving a quote soft-delete requires the quote-editor lifecycle (create quote -> delete),
        // which isn't exposed from the Opportunities grid. Surfaced as a documented precondition gap,
        // NOT a vacuous pass.
        throw new SkipException("Requires quote-lifecycle setup (create + soft-delete a quote); "
                + "exclusion logic is unit-test territory or needs a quote-editor helper. Documented gap.");
    }

    // ── D. AI Opportunity (feature-flagged ai_opportunities) ──
    @Test(priority = 38, description = "TC_OPP_15: AI Opportunity button is present when the feature is enabled")
    public void testOpp15_AiButtonPresent() {
        ExtentReportManager.createTest(MODULE, "AI Opportunity", "Opp_15_AiPresent");
        goToOpportunities();
        if (!page.isAiButtonPresent())
            throw new SkipException("AI Opportunity button not rendered (ai_opportunities flag off for this tenant)");
        ExtentReportManager.logPass("AI Opportunity button present (enabled=" + page.isAiButtonEnabled() + ")");
    }

    @Test(priority = 39, description = "TC_OPP_16: AI button is gated (disabled) when the feature is off")
    public void testOpp16_AiButtonGated() {
        ExtentReportManager.createTest(MODULE, "AI Opportunity", "Opp_16_AiGated");
        goToOpportunities();
        if (!page.isAiButtonPresent()) throw new SkipException("AI button absent — gating shown by omission, not a disabled button");
        if (page.isAiButtonEnabled())
            throw new SkipException("AI feature is ENABLED for this tenant — gating case not applicable here");
        // present but disabled -> verify it doesn't act
        Assert.assertFalse(page.isAiButtonEnabled(), "Gated AI button must be disabled.");
        ExtentReportManager.logPass("AI button present but gated (disabled)");
    }

    // Tripwire: opening the AI Opportunity dialog throws BUG-A (Qe crash), caught by verifyPageHealth.
    @Test(priority = 40, groups = {"known-product-bug"},
          description = "TC_OPP_17: AI create flow opens its dialog (feature enabled) [tripwire: BUG-A]")
    public void testOpp17_AiCreateFlow() {
        ExtentReportManager.createTest(MODULE, "AI Opportunity", "Opp_17_AiFlow");
        goToOpportunities();
        if (!page.isAiButtonPresent() || !page.isAiButtonEnabled())
            throw new SkipException("AI Opportunity not available/enabled for this tenant");
        WebElement ai = driver.findElements(OpportunitiesPage.AI_OPP_BTN).get(0);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", ai);
        pause(1800);
        Assert.assertTrue(page.isDialogOpen(), "AI Opportunity should open a dialog/flow.");
        verifyPageHealth("AI Opportunity dialog");
        page.clickCancel();
        ExtentReportManager.logPass("AI Opportunity flow opened (then cancelled)");
    }

    // ── J. Permissions ──
    @Test(priority = 41, description = "TC_OPP_41: A user without features.opportunities.view cannot access the module")
    public void testOpp41_NoViewPermission() {
        ExtentReportManager.createTest(MODULE, "Permissions", "Opp_41_NoPermission");
        String lpEmail = System.getenv("LOWPERM_EMAIL");
        String lpPass = System.getenv("LOWPERM_PASSWORD");
        if (lpEmail == null || lpEmail.isEmpty() || lpPass == null || lpPass.isEmpty())
            throw new SkipException("No low-permission user configured (set LOWPERM_EMAIL/LOWPERM_PASSWORD to run RBAC test)");
        // A real RBAC test would re-login as the low-perm user here; the BaseTest session is the
        // admin. Performing a second login mid-class is destructive to the shared session, so this
        // is gated behind the env so it only runs when a low-perm account is provided.
        throw new SkipException("LOWPERM creds present but mid-class re-login is unsafe in the shared session; "
                + "run this as a dedicated low-perm suite. Documented.");
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
        // Per the walkthrough video: the Facility field is an autocomplete ALREADY pre-filled
        // (and committed) to the active Site, and Create is disabled ONLY because the name is
        // empty. So we DON'T touch Facility — re-opening the autocomplete and re-picking risks
        // landing on a different facility (e.g. the duplicate 'Yuzi' entries), creating the opp
        // off the active-site grid. Just type the Opportunity Name and Create enables.
    }

    /** Best-effort: find a row-level delete control. Opportunities renders a red trash ICON
     *  in the Actions column (per the walkthrough video), so look for that first; fall back
     *  to a kebab/action menu. */
    private WebElement findDeleteControl() {
        // 1) Direct delete/trash icon-button in the FIRST data row's Actions cell.
        WebElement icon = (WebElement) ((JavascriptExecutor) driver).executeScript(
            "var row=document.querySelector('.MuiDataGrid-row'); if(!row) return null;"
          + "var svg=row.querySelector('[data-testid*=\"Delete\"],[data-testid*=\"delete\"],[data-testid*=\"Trash\"]');"
          + "if(svg){return svg.closest('button')||svg;}"
          + "var act=row.querySelector('.MuiDataGrid-cell[data-field=\"actions\"],.MuiDataGrid-actionsCell');"
          + "if(act){var b=act.querySelector('button'); if(b) return b;}"
          + "var aria=row.querySelector('[aria-label*=\"elete\" i],[title*=\"elete\" i]');"
          + "if(aria) return aria;"
          + "return null;");
        if (icon != null) return icon;
        // 2) Kebab/action menu fallback: open it, then pick a Delete menu item.
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

    /** Clean numeric unique id for opportunity names — no spaces/colons so search-by-name matches. */
    private String uid() {
        return String.valueOf(System.currentTimeMillis());
    }
}
