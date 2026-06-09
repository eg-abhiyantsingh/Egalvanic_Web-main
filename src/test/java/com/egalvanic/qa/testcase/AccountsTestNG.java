package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.AccountsPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.response.Response;
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
 * Accounts [SALES] — customer accounts (/accounts), the third SALES nav item.
 *
 * First dedicated coverage for this module (previously only smoke-touched). Covers
 * read / columns / search, create-dialog VALIDATION (without submitting — the create
 * form collects a Subdomain that may provision a tenant, so we never create accounts),
 * the account DETAIL + Contacts tab (the source of an Opportunity quote's Recipient),
 * confirmation-gated delete (cancelled — never deletes shared data), plus quarantined
 * tripwires for the app-wide bugs (BUG-A crash, BUG-B WCAG, BUG-E flat-endpoint auth).
 *
 * Verification rule (same as the rest of the suite): assert the real contract, never
 * "element present" alone; functional tests assert FUNCTION and do NOT call
 * verifyPageHealth (that is reserved for the known-product-bug tripwires).
 */
public class AccountsTestNG extends BaseTest {

    private static final String MODULE = "Accounts";
    private AccountsPage page;

    @BeforeClass
    @Override
    public void classSetup() {
        super.classSetup();   // login + default site selection
        page = new AccountsPage(driver);
    }

    private void goToAccounts() {
        page.open();
        dismissBackdrops();
    }

    // ───────────────────────────── Load / structure ─────────────────────────────

    // Quarantined tripwire — SALES pages intermittently emit a severe-error storm (BUG-A on Opp;
    // the notes-fetch-returns-HTML storm seen on /goals, BUG-F). Page health is intermittently
    // red app-wide, so this stays out of the functional gate. Assertion NOT weakened.
    @Test(priority = 1, groups = {"known-product-bug"},
          description = "TC_ACC_01: Accounts page loads healthy — no severe JS/network errors [tripwire: BUG-A / BUG-F]")
    public void testAcc01_PageLoadsHealthy() {
        ExtentReportManager.createTest(MODULE, "Navigation", "Acc_01_LoadHealthy");
        goToAccounts();
        Assert.assertTrue(page.isGridPresent() || page.bodyText().toLowerCase().contains("account"),
                "Accounts page should show a grid or a definitive accounts state.");
        verifyPageHealth("Accounts page");
        ExtentReportManager.logPass("Accounts page healthy");
    }

    @Test(priority = 2, description = "TC_ACC_02: Accounts grid exposes the expected columns (Account Name, Owner, Created)")
    public void testAcc02_GridColumns() {
        ExtentReportManager.createTest(MODULE, "Navigation", "Acc_02_Columns");
        goToAccounts();
        if (!page.isGridPresent()) throw new SkipException("No accounts grid (empty state) — column check N/A");
        List<String> headers = page.columnHeaders();
        logStep("Columns: " + headers);
        String joined = String.join(" | ", headers).toLowerCase();
        Assert.assertTrue(joined.contains("account name") || joined.contains("name"),
                "Grid must have an Account Name column. Got: " + headers);
        Assert.assertTrue(joined.contains("owner"), "Grid must have an Owner column. Got: " + headers);
        Assert.assertTrue(joined.contains("created"), "Grid must have a Created column. Got: " + headers);
        ExtentReportManager.logPass("Account columns present: " + headers);
    }

    // ───────────────────────────── CREATE: validation (no submit) ─────────────────────────────

    @Test(priority = 3, description = "TC_ACC_03: New Account dialog opens with the required fields")
    public void testAcc03_CreateDialogRequiredFields() {
        ExtentReportManager.createTest(MODULE, "Create / Validation", "Acc_03_DialogFields");
        goToAccounts();
        if (!page.isNewButtonPresent()) throw new SkipException("No 'New Account' button (permission/empty precondition)");
        page.openCreateDialog();
        List<String> required = page.requiredFieldLabels();
        String dlg = page.dialogText().toLowerCase();
        page.clickCancel();
        logStep("Required fields: " + required);
        Assert.assertTrue(dlg.contains("account name"), "Create dialog should ask for an Account Name. Text: " + dlg);
        Assert.assertTrue(required.stream().anyMatch(r -> r.toLowerCase().contains("account name")),
                "Account Name must be a required field. Required: " + required);
        Assert.assertTrue(required.size() >= 3,
                "Create New Account should mark multiple required fields (name/subdomain/owner/address). Got: " + required);
        ExtentReportManager.logPass("Create dialog exposes required fields: " + required);
    }

    @Test(priority = 4, description = "TC_ACC_04: Create requires a name — empty name must NOT be savable")
    public void testAcc04_CreateNameRequired() {
        ExtentReportManager.createTest(MODULE, "Create / Validation", "Acc_04_NameRequired");
        goToAccounts();
        if (!page.isNewButtonPresent()) throw new SkipException("No 'New Account' button");
        page.openCreateDialog();
        boolean enabledEmpty = page.isSaveEnabled();
        page.clickCancel();
        Assert.assertFalse(enabledEmpty, "With all fields empty, Create must be disabled.");
        ExtentReportManager.logPass("Empty form blocks Create");
    }

    @Test(priority = 5, description = "TC_ACC_05: Name alone does not satisfy create — other required fields still gate it")
    public void testAcc05_RequiredFieldsGateCreate() {
        ExtentReportManager.createTest(MODULE, "Create / Validation", "Acc_05_RequiredGate");
        goToAccounts();
        if (!page.isNewButtonPresent()) throw new SkipException("No 'New Account' button");
        page.openCreateDialog();
        page.setAccountName("AcctValidate_" + System.currentTimeMillis());
        pause(500);
        boolean enabledNameOnly = page.isSaveEnabled();
        page.clickCancel();
        // Subdomain/Owner/Address are also required, so name-only must NOT enable Create.
        Assert.assertFalse(enabledNameOnly,
                "Create must stay disabled with only the name filled (Subdomain/Owner/Address are required).");
        ExtentReportManager.logPass("Name-only does not satisfy the required-field set");
    }

    @Test(priority = 6, description = "TC_ACC_06: Cancel create closes the dialog and creates nothing")
    public void testAcc06_CancelCreate() {
        ExtentReportManager.createTest(MODULE, "Create / Lifecycle", "Acc_06_Cancel");
        goToAccounts();
        if (!page.isNewButtonPresent()) throw new SkipException("No 'New Account' button");
        int before = page.rowCount();
        page.openCreateDialog();
        page.setAccountName("CancelAcct_" + System.currentTimeMillis());
        page.clickCancel();
        pause(800);
        Assert.assertFalse(page.isDialogOpen(), "Cancel should close the create dialog.");
        Assert.assertEquals(page.rowCount(), before, "Cancel must not create an account (row count unchanged).");
        ExtentReportManager.logPass("Cancel closed the dialog, created nothing");
    }

    @Test(priority = 7, description = "TC_ACC_07: XSS in the Account Name field must NOT execute script")
    public void testAcc07_XssNameNotExecuted() {
        ExtentReportManager.createTest(MODULE, "Create / Security", "Acc_07_XSS");
        goToAccounts();
        if (!page.isNewButtonPresent()) throw new SkipException("No 'New Account' button");
        ((JavascriptExecutor) driver).executeScript("window.__accXss = 0;");
        page.openCreateDialog();
        page.setAccountName("<img src=x onerror=\"window.__accXss=1\"><script>window.__accXss=1</script>");
        pause(800);
        Object flag = ((JavascriptExecutor) driver).executeScript("return window.__accXss || 0;");
        page.clickCancel();
        Assert.assertEquals(String.valueOf(flag), "0",
                "SECURITY: XSS payload in the account name executed (window.__accXss was set).");
        ExtentReportManager.logPass("XSS payload in account name did not execute");
    }

    // ───────────────────────────── READ / SEARCH ─────────────────────────────

    @Test(priority = 8, description = "TC_ACC_08: Search narrows the grid to matching accounts")
    public void testAcc08_SearchFilters() {
        ExtentReportManager.createTest(MODULE, "Read / Search", "Acc_08_Search");
        goToAccounts();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No accounts to search (empty grid)");
        String firstRow = page.rows().get(0).getText().replace("\n", " ").trim();
        String token = firstRow.split(" ")[0];
        if (token.length() < 2) throw new SkipException("First row has no usable search token");
        int before = page.rowCount();
        page.search(token);
        int after = page.rowCount();
        Assert.assertTrue(after <= before, "Search must not increase row count (" + before + "->" + after + ").");
        for (WebElement r : page.rows()) {
            Assert.assertTrue(r.getText().toLowerCase().contains(token.toLowerCase()),
                    "Filtered row does not contain search token '" + token + "': " + r.getText());
        }
        page.clearSearch();
        ExtentReportManager.logPass("Search filtered " + before + " -> " + after + " rows, all match '" + token + "'");
    }

    @Test(priority = 9, description = "TC_ACC_09: Search with no matches shows an empty result, not a crash")
    public void testAcc09_SearchNoResults() {
        ExtentReportManager.createTest(MODULE, "Read / Search", "Acc_09_NoResults");
        goToAccounts();
        if (!page.isGridPresent()) throw new SkipException("No accounts grid");
        page.search("zzqqxx_nomatch_" + System.nanoTime());
        Assert.assertEquals(page.rowCount(), 0, "A no-match search must yield 0 rows.");
        Assert.assertFalse(page.bodyText().toLowerCase().contains("undefined is not"),
                "No-result render must not surface a raw JS error in the body.");
        page.clearSearch();
        ExtentReportManager.logPass("No-result search handled gracefully");
    }

    @Test(priority = 10, description = "TC_ACC_10: Clearing the search restores the full account list")
    public void testAcc10_ClearSearchRestores() {
        ExtentReportManager.createTest(MODULE, "Read / Search", "Acc_10_ClearRestores");
        goToAccounts();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No accounts to search");
        int before = page.rowCount();
        String token = page.rows().get(0).getText().replace("\n", " ").trim().split(" ")[0];
        page.search(token);
        page.clearSearch();
        // poll for the grid to repopulate (async re-render)
        int after = 0;
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            after = page.rowCount();
            if (after >= before) break;
            pause(500);
        }
        Assert.assertTrue(after >= before,
                "Clearing search should restore the full list (" + before + " -> " + after + ").");
        ExtentReportManager.logPass("Clear search restored the list (" + after + " rows)");
    }

    // ───────────────────────────── DETAIL + relationships ─────────────────────────────

    @Test(priority = 11, description = "TC_ACC_11: Opening an account detail shows its relationship tabs (Contacts/Opportunities/Sites/Goals)")
    public void testAcc11_DetailOpensWithTabs() {
        ExtentReportManager.createTest(MODULE, "Detail", "Acc_11_DetailTabs");
        goToAccounts();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No accounts to open");
        String before = driver.getCurrentUrl();
        String after = page.openFirstRow();
        boolean opened = after != null && (after.contains("/accounts/") && !after.equals(before));
        if (!opened) throw new SkipException("Row click did not open an account detail view");
        page.waitForTabContaining("Contacts", 12000);   // detail tabs mount async (BUG-A render lag)
        List<String> tabs = page.tabLabels();
        logStep("Account detail tabs: " + tabs);
        String joined = String.join(" | ", tabs).toLowerCase();
        Assert.assertTrue(joined.contains("contacts"), "Account detail must expose a Contacts tab. Tabs: " + tabs);
        Assert.assertTrue(joined.contains("opportunities") || joined.contains("sites") || joined.contains("goals"),
                "Account detail should expose SALES relationship tabs (Opportunities/Sites/Goals). Tabs: " + tabs);
        ExtentReportManager.logPass("Account detail tabs present: " + tabs);
    }

    @Test(priority = 12, description = "TC_ACC_12: The Contacts tab loads (the source of an Opportunity quote's Recipient)")
    public void testAcc12_ContactsTabLoads() {
        ExtentReportManager.createTest(MODULE, "Detail / Contacts", "Acc_12_Contacts");
        goToAccounts();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No accounts to open");
        String url = page.openFirstRow();
        if (url == null || !url.contains("/accounts/")) throw new SkipException("Row did not open a detail view");
        page.waitForTabContaining("Contacts", 12000);   // detail tabs mount async (BUG-A render lag)
        if (!page.clickTab("Contacts")) throw new SkipException("No Contacts tab on this account detail");
        pause(1500);
        String body = page.bodyText().toLowerCase();
        // The contacts panel renders a contacts grid/empty-state or an add-contact affordance — not a crash.
        Assert.assertTrue(body.contains("contact") || !driver.findElements(By.cssSelector(".MuiDataGrid-root, table")).isEmpty(),
                "Contacts tab should render a contacts list/empty-state/add-contact UI.");
        Assert.assertFalse(body.contains("something went wrong") || body.contains("page not found"),
                "Contacts tab must not render an error page.");
        ExtentReportManager.logPass("Contacts tab loaded");
    }

    // ───────────────────────────── DELETE (gating only; never deletes shared data) ─────────────────────────────

    @Test(priority = 13, description = "TC_ACC_13: Delete asks for confirmation (no immediate destructive delete)")
    public void testAcc13_DeleteConfirmationRequired() {
        ExtentReportManager.createTest(MODULE, "Delete", "Acc_13_DeleteConfirm");
        goToAccounts();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No accounts to delete");
        int before = page.rowCount();
        WebElement del = page.findDeleteControl();
        if (del == null) throw new SkipException("No row-level delete control (may live in detail view only)");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", del);
        pause(1200);
        boolean confirmShown = page.isDialogOpen()
                || page.bodyText().toLowerCase().contains("are you sure")
                || page.bodyText().toLowerCase().contains("confirm");
        page.clickCancel();   // do NOT confirm — never delete shared accounts
        pause(800);
        int after = page.rowCount();
        Assert.assertTrue(confirmShown, "Delete must show a confirmation before removing an account.");
        Assert.assertEquals(after, before, "Cancelling delete must keep the account (row count unchanged).");
        ExtentReportManager.logPass("Delete is confirmation-gated; cancel kept the account");
    }

    // ───────────────────────────── Non-functional + API ─────────────────────────────

    // Tripwire: app-wide WCAG violations (BUG-B) fail this on /accounts too.
    @Test(priority = 14, groups = {"known-product-bug"},
          description = "TC_ACC_14: Accessibility — no critical/serious WCAG violations [tripwire: BUG-B]")
    public void testAcc14_Accessibility() {
        ExtentReportManager.createTest(MODULE, "Accessibility", "Acc_14_A11y");
        goToAccounts();
        verifyAccessibility("Accounts (/accounts)");
        ExtentReportManager.logPass("No critical/serious WCAG violations on Accounts");
    }

    // Quarantined tripwire — account detail health is intermittently red (severe-error storms,
    // incl. the notes-fetch-returns-HTML issue BUG-F). Stays out of the functional gate.
    @Test(priority = 15, groups = {"known-product-bug"},
          description = "TC_ACC_15: Account detail opens healthy — no severe JS/network errors [tripwire: BUG-A / BUG-F]")
    public void testAcc15_DetailHealth() {
        ExtentReportManager.createTest(MODULE, "Detail", "Acc_15_DetailHealth");
        goToAccounts();
        if (!page.isGridPresent() || page.rowCount() == 0) throw new SkipException("No accounts to open");
        String url = page.openFirstRow();
        if (url == null || !url.contains("/accounts/")) throw new SkipException("Row did not open a detail view");
        verifyPageHealth("Account detail");
        ExtentReportManager.logPass("Account detail healthy");
    }

    // Tripwire (BUG-E, see BUGS.md): the flat /accounts/ API endpoint responds 200 with NO auth
    // (a null-field template, no data leak) while company-scoped reads require it — same auth
    // inconsistency as /opportunities/ and /quotes/. Assertion NOT weakened.
    @Test(priority = 16, groups = {"known-product-bug"},
          description = "TC_ACC_API: flat /accounts/ API should require auth [tripwire: BUG-E BAC inconsistency]")
    public void testAcc_ApiFlatEndpointRequiresAuth() {
        ExtentReportManager.createTest(MODULE, "API Security", "Acc_API_FlatAuth");
        RestAssured.baseURI = AppConstants.API_BASE_URL;
        Response flat;
        try {
            flat = given().get("/accounts/");
        } catch (Exception e) {
            throw new SkipException("API host unreachable from this network: " + e.getMessage());
        }
        Assert.assertTrue(flat.statusCode() == 401 || flat.statusCode() == 403,
                "BAC: GET /accounts/ should require auth, but returned " + flat.statusCode() + " unauthenticated.");
        ExtentReportManager.logPass("Flat /accounts/ enforces auth");
    }
}
