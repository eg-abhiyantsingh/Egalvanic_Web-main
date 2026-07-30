package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.AccountsPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Web v1.35 Account coverage EXTENSION — pins the behaviors the base
 * {@link AccountV135RegressionTestNG} does not cover, all live-verified on QA on
 * 2026-07-30 (badge V1.36, post-rename: the account list now lives at /customers,
 * "Customers" under the OPERATIONS sidebar group; /accounts redirects there):
 *
 * <ul>
 *   <li><b>Old-route redirect</b> — a bookmark to /accounts must land on the account
 *       list (today: redirect to /customers) with the grid intact (ZP-3157 AC-3).</li>
 *   <li><b>ZP-3156 email validation (two layers)</b> — a well-formed but generic-domain
 *       contact email (gmail.com) passes the button-gate and is rejected at CLICK time
 *       with the spec's exact copy: "Enter a valid business email address. Generic
 *       domains (e.g., Gmail, Outlook) are not allowed." (in-dialog [role=alert], no
 *       create request fires). A malformed FORMAT is caught earlier by the button-gate
 *       (Create disabled once validation settles). Either way, no account is created —
 *       the malformed test pins that OUTCOME (it proved unstable to pin WHICH layer
 *       across real-gesture vs synthetic-event sessions).</li>
 *   <li><b>Edit-dialog contract</b> — "Edit Account" carries Account Details (name,
 *       owner), the Primary-Contact block (firstname/lastname/job_title; the email
 *       DOMAIN is locked — only the local part is editable), an optional Address
 *       section (address_line_1/2, city, state_province, postal_code, country_code),
 *       and Manage Contacts / Cancel / Save Changes. No subdomain anywhere.</li>
 *   <li><b>ZP-3185 cross-page</b> — the facility selector is absent on the account
 *       list but present again on /assets, and absent again on return.</li>
 *   <li><b>ZP-3049 API contract</b> — POST /api/account/by-company/{companyId}/v2
 *       {page, page_size, search, filters, sort_by, sort_dir} answers
 *       {success, data:{items[], page, page_size, total}} with page_size honored
 *       server-side (verified: page_size 5 → 5 items of total 46).</li>
 *   <li><b>Duplicate-name tripwire</b> (known-product-bug) — ZP-3156 AC-6 requires a
 *       duplicate-domain/name block, but on 2026-07-30 creating the SAME account name
 *       twice returned two 201s (and the tenant already holds several accounts sharing
 *       subdomain "egalvanic"). RED here = the defect is still live.</li>
 *   <li><b>Role matrix</b> — for Admin and Account Manager: if the sidebar offers the
 *       account item, the deep link must work (menu and access must not disagree).</li>
 * </ul>
 *
 * Same conventions as the base class: one browser per class, role tests LAST with a
 * Super-Admin restore, all created rows self-clean (QA_AVX_* names only), MUI driven
 * via the JS patterns proven on 2026-07-27/30.
 */
public class AccountV135ExtendedTestNG extends BaseTest {

    private static final String MODULE = "Accounts";
    private static final String FEATURE = "Accounts v1.35 extension (validation/redirect/edit/API/roles)";

    private static final List<String> ROLE_NAMES = Arrays.asList(
            "Admin", "Project Manager", "Account Manager", "Super Admin", "Electrical Engineer");
    private static final String HOME_ROLE = "Super Admin";

    /** Exact copy shipped for both the generic-domain and malformed-email rejections. */
    private static final String BUSINESS_EMAIL_ERROR_FRAGMENT = "valid business email";
    private static final String GENERIC_DOMAINS_FRAGMENT = "Generic domains";

    private AccountsPage page;
    /** Name used by the duplicate tripwire — cleaned in @AfterClass even on failure. */
    private static String dupAccountName;

    @BeforeClass(alwaysRun = true)   // group-filtered runs (-Dgroups=known-product-bug) still need the browser
    @Override
    public void classSetup() {
        super.classSetup();
        page = new AccountsPage(driver);
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(45));
    }

    @AfterClass(alwaysRun = true)
    public void restoreRoleAndCleanup() {
        try { if (!HOME_ROLE.equals(currentRole())) switchRole(HOME_ROLE); } catch (Exception ignored) { }
        try {
            if (dupAccountName != null) {
                goToAccounts();
                for (int i = 0; i < 4; i++) {
                    page.search(dupAccountName);
                    pause(1500);
                    if (!page.hasRowContaining(dupAccountName)) break;
                    deleteRowByName(dupAccountName);
                }
                page.clearSearch();
            }
        } catch (Exception ignored) { }
    }

    private void goToAccounts() {
        page.open();
        dismissBackdrops();
        waitAndDismissAppAlert();
    }

    private static boolean onAccountsRoute(String url) {
        return url.contains("/accounts") || url.contains("/customers");
    }

    // ================================================================
    // Old-route redirect (ZP-3157 AC-3: bookmarks keep working)
    // ================================================================

    @Test(priority = 1,
          description = "TC_AVX_01: a deep link to the old /accounts route lands on the account list with the grid intact")
    public void testOldAccountsRouteRedirects() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_01 — old /accounts deep link redirects to the live list");
        driver.get(AppConstants.BASE_URL + "/accounts");
        pause(5000);
        waitAndDismissAppAlert();
        String url = driver.getCurrentUrl();
        logStep("Requested /accounts, landed on: " + url);
        Assert.assertTrue(onAccountsRoute(url),
                "A bookmark to /accounts must land on the account list (old or renamed route). Landed: " + url);
        page.waitForContent();
        Assert.assertTrue(page.isGridPresent(), "The account grid must render after the redirect.");
        if (url.contains("/customers")) {
            logStep("Renamed contract active: /accounts redirected to /customers (observed since 2026-07-30).");
        }
        ExtentReportManager.logPass("Old /accounts deep link keeps working (landed " + url + " with the grid).");
    }

    // ================================================================
    // ZP-3156 — create-flow email validation (click-time, in-dialog alert)
    // ================================================================

    @Test(priority = 10,
          description = "TC_AVX_02: a generic-domain contact email (gmail.com) blocks Create with the business-email error and creates nothing")
    public void testGenericEmailDomainBlockedOnCreate() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_02 — generic email domain blocks account creation");
        String name = "QA_AVX_gen_" + System.currentTimeMillis();
        goToAccounts();
        page.openCreateDialog();
        try {
            page.setAccountName(name);
            selectFirstOwner();
            fillContact("Probe", "Generic", "qa.avx.probe@gmail.com", "QA Engineer");
            clickCreate();
            pause(2000);
            Assert.assertTrue(page.isDialogOpen(),
                    "The create dialog must STAY OPEN when the contact email is on a generic domain.");
            String alert = dialogAlertText();
            logStep("Alert after Create with gmail address: " + alert);
            Assert.assertTrue(alert.contains(GENERIC_DOMAINS_FRAGMENT) || alert.contains(BUSINESS_EMAIL_ERROR_FRAGMENT),
                    "Expected the business-email rejection ('Enter a valid business email address. Generic"
                    + " domains (e.g., Gmail, Outlook) are not allowed.') but the alert said: '" + alert + "'");
        } finally {
            page.clickCancel();
            pause(800);
        }
        page.search(name);
        pause(1500);
        Assert.assertFalse(page.hasRowContaining(name),
                "No account may be created from a submission rejected for a generic email domain.");
        page.clearSearch();
        ExtentReportManager.logPass("gmail.com contact email is rejected at Create with the spec's error copy; nothing was created.");
    }

    @Test(priority = 11,
          description = "TC_AVX_03: a malformed contact email must not produce an account (button-gate or click-time rejection)")
    public void testMalformedEmailBlockedOnCreate() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_03 — malformed email cannot create an account");
        String name = "QA_AVX_fmt_" + System.currentTimeMillis();
        goToAccounts();
        page.openCreateDialog();
        try {
            page.setAccountName(name);
            selectFirstOwner();
            fillContact("Probe", "Malformed", "not-an-email", "QA Engineer");
            // A malformed email is caught by the button-gate (Create disabled), but the
            // gate settles AFTER a debounced re-validation — a single snapshot right
            // after filling races the transition (seen 2026-07-30: enabled for one
            // instant, then disabled). Poll up to 5s for it to settle to disabled.
            boolean buttonGated = false;
            for (int i = 0; i < 10 && !buttonGated; i++) {
                pause(500);
                buttonGated = !page.isSaveEnabled();
            }
            if (buttonGated) {
                logStep("Malformed email gates Create (button disabled after validation settled).");
            } else {
                // Fallback path if a build ever enables the button: clicking must NOT
                // create an account (verified by the no-row assertion below).
                logStep("Create was enabled with a malformed email — clicking to confirm nothing is created.");
                clickCreate();
                pause(2500);
            }
        } finally {
            if (page.isDialogOpen()) page.clickCancel();
            pause(800);
        }
        // The decisive outcome either way: nothing was created.
        page.search(name);
        pause(2000);
        Assert.assertFalse(page.hasRowContaining(name),
                "No account may exist after a malformed-email submission attempt.");
        page.clearSearch();
        ExtentReportManager.logPass("Malformed contact email cannot produce an account (gate or click-time rejection verified).");
    }

    // ================================================================
    // Edit-dialog contract (row Edit action → all sections + locked email domain)
    // ================================================================

    @Test(priority = 12,
          description = "TC_AVX_04: the Edit Account dialog exposes account, primary-contact and optional address sections; email domain is locked; no subdomain")
    public void testEditDialogContract() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_04 — Edit Account dialog contract");
        goToAccounts();
        page.waitForContent();
        List<String> names = page.columnValues("name");
        if (names.isEmpty()) throw new SkipException("No accounts available to edit.");
        String rowName = names.get(0);
        if (!clickRowIconAction(rowName, "Edit")) {
            throw new SkipException("No Edit action available on row '" + rowName + "'.");
        }
        try {
            pause(1200);
            Assert.assertTrue(page.isDialogOpen(), "The Edit dialog should open from the row Edit action.");
            String text = page.dialogText();
            // innerText carries the CSS text-transform (section headers render as
            // "ACCOUNT DETAILS"), so match sections case-insensitively.
            String textLc = text.toLowerCase();
            for (String section : new String[]{"edit account", "account details", "contact details", "address"}) {
                Assert.assertTrue(textLc.contains(section),
                        "Edit dialog must show the '" + section + "' section. Dialog text: " + snippet(text));
            }
            Assert.assertFalse(text.toLowerCase().contains("subdomain"),
                    "Subdomain must not appear in the Edit dialog (ZP-3156).");
            // Every persisted field is a stable name= input (live-verified 2026-07-30).
            for (String nameAttr : new String[]{"name", "firstname", "lastname", "email", "job_title",
                    "address_line_1", "address_line_2", "city", "state_province", "postal_code"}) {
                Assert.assertFalse(driver.findElements(
                        By.cssSelector("div[role='dialog'] input[name='" + nameAttr + "']")).isEmpty(),
                        "Edit dialog must carry the input name='" + nameAttr + "'.");
            }
            String prefilled = String.valueOf(js().executeScript(
                    "return document.querySelector('div[role=\"dialog\"] input[name=\"name\"]').value;"));
            Assert.assertEquals(prefilled.trim(), rowName.trim(),
                    "The Account Name field must be prefilled with the row's name.");
            // The primary contact's email domain is locked on edit: the dialog renders the
            // fixed '@domain' suffix as text next to the local-part input.
            Assert.assertTrue(text.contains("@"),
                    "The locked email-domain suffix ('@<account domain>') must be visible in the Edit dialog.");
            for (String btn : new String[]{"Save Changes", "Manage Contacts", "Cancel"}) {
                Assert.assertTrue(dialogHasButton(btn), "Edit dialog must offer the '" + btn + "' button.");
            }
            logStep("Edit dialog verified for row '" + rowName + "' — sections, name= inputs, locked domain, buttons.");
        } finally {
            page.clickCancel();
            pause(800);
        }
        ExtentReportManager.logPass("Edit Account dialog contract holds (sections, prefill, locked email domain, no subdomain).");
    }

    // ================================================================
    // ZP-3185 — the facility selector is page-scoped, not gone globally
    // ================================================================

    @Test(priority = 13,
          description = "TC_AVX_05: the facility selector is absent on the account list, present on /assets, and absent again on return")
    public void testSiteSelectorCrossPageBehavior() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_05 — facility selector only off on the account pages");
        goToAccounts();
        page.waitForContent();
        Assert.assertFalse(facilitySelectorPresent(),
                "The account list must not show the 'Select facility' selector (ZP-3185).");

        driver.get(AppConstants.BASE_URL + "/assets");
        boolean onAssets = false;
        for (int i = 0; i < 20 && !onAssets; i++) {   // /assets is a slow page; poll up to ~15s
            pause(750);
            onAssets = facilitySelectorPresent();
        }
        Assert.assertTrue(onAssets,
                "The 'Select facility' selector must still exist on site-scoped pages like /assets —"
                + " ZP-3185 removes it from Accounts only.");
        logStep("/assets shows the facility selector as before.");

        goToAccounts();
        page.waitForContent();
        Assert.assertFalse(facilitySelectorPresent(),
                "Returning to the account list must hide the facility selector again.");
        ExtentReportManager.logPass("Facility selector: hidden on the account list, intact on /assets, hidden again on return.");
    }

    // ================================================================
    // ZP-3049 — server-side pagination contract of the v2 list API
    // ================================================================

    @Test(priority = 14,
          description = "TC_AVX_06: POST /api/account/by-company/{id}/v2 honors page/page_size server-side and echoes them with a total")
    public void testListApiV2PaginationContract() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_06 — account list v2 API pagination contract");
        goToAccounts();
        page.waitForContent();
        // The page itself just fired the list call — recover the companyId from it rather
        // than hardcoding tenant data.
        Object result = js().executeAsyncScript(
                "var done=arguments[arguments.length-1];"
              + "(async function(){"
              + "  try {"
              + "    var entry=performance.getEntriesByType('resource').map(function(e){return e.name;})"
              + "      .find(function(n){return /\\/api\\/account\\/by-company\\/[0-9a-f-]+\\/v2/.test(n);});"
              + "    if(!entry){ done('ERR:list request not observed'); return; }"
              + "    var cid=entry.match(/by-company\\/([0-9a-f-]+)\\/v2/)[1];"
              + "    var call=function(page){ return fetch('/api/account/by-company/'+cid+'/v2',{"
              + "      method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},"
              + "      body:JSON.stringify({page:page,page_size:5,search:'',filters:{},sort_by:'created_at',sort_dir:'desc'})"
              + "    }).then(function(r){ return r.json(); }); };"
              + "    var p1=await call(1); var p2=await call(2);"
              + "    done(JSON.stringify({"
              + "      ok1:p1.success===true, len1:(p1.data&&p1.data.items)?p1.data.items.length:-1,"
              + "      page1:p1.data?p1.data.page:null, size1:p1.data?p1.data.page_size:null,"
              + "      total:p1.data?p1.data.total:null,"
              + "      len2:(p2.data&&p2.data.items)?p2.data.items.length:-1,"
              + "      firstId1:(p1.data&&p1.data.items[0])?p1.data.items[0].id:null,"
              + "      firstId2:(p2.data&&p2.data.items[0])?p2.data.items[0].id:null}));"
              + "  } catch(e){ done('ERR:'+e); }"
              + "})();");
        String raw = String.valueOf(result);
        logStep("v2 list API probe: " + raw);
        if (raw.startsWith("ERR:")) throw new SkipException("Could not probe the v2 list API: " + raw);

        Assert.assertTrue(raw.contains("\"ok1\":true"), "The v2 list API must answer success:true. Got: " + raw);
        Assert.assertTrue(raw.contains("\"len1\":5"),
                "page_size=5 must be honored server-side (5 items on page 1). Got: " + raw);
        Assert.assertTrue(raw.contains("\"page1\":1") && raw.contains("\"size1\":5"),
                "The response must echo page and page_size. Got: " + raw);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"total\":(\\d+)").matcher(raw);
        Assert.assertTrue(m.find() && Integer.parseInt(m.group(1)) > 5,
                "data.total must report the full collection size (>5). Got: " + raw);
        java.util.regex.Matcher ids = java.util.regex.Pattern.compile(
                "\"firstId1\":\"([^\"]+)\".*\"firstId2\":\"([^\"]+)\"").matcher(raw);
        Assert.assertTrue(ids.find() && !ids.group(1).equals(ids.group(2)),
                "Page 2 must return different records than page 1. Got: " + raw);
        ExtentReportManager.logPass("v2 list API pagination is server-side: page/page_size honored + echoed, total reported, pages disjoint.");
    }

    // ================================================================
    // Duplicate-name tripwire — VERIFIED defect (2026-07-30): two 201s
    // ================================================================

    @Test(priority = 20, groups = {"known-product-bug"},
          description = "TC_AVX_07: creating a second account with an existing name must be rejected (ZP-3156 AC-6)"
                  + " — verified live 2026-07-30: the API answered 201 twice (duplicate created, subdomains already collide)")
    public void testDuplicateAccountNameRejected() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_07 — duplicate account name must be rejected (tripwire)");
        dupAccountName = "QA_AVX_dup_" + System.currentTimeMillis();
        goToAccounts();

        // 1st create — must succeed (this is the baseline row).
        page.openCreateDialog();
        page.setAccountName(dupAccountName);
        selectFirstOwner();
        fillContact("Probe", "DupOne", "qa.avx.dup1@egalvanic.com", "QA Engineer");
        chooseLicense("No license");
        clickCreate();
        if (!waitForDialogClosed(20000)) {
            page.clickCancel();
            throw new SkipException("Baseline create did not complete — cannot exercise the duplicate check.");
        }
        page.search(dupAccountName);
        if (!waitForRow(dupAccountName, 20000)) {
            throw new SkipException("Baseline account row never appeared — cannot exercise the duplicate check.");
        }
        page.clearSearch();
        pause(1000);

        // 2nd create with the SAME name — the spec (ZP-3156 AC-6) requires a rejection.
        page.openCreateDialog();
        page.setAccountName(dupAccountName);
        selectFirstOwner();
        fillContact("Probe", "DupTwo", "qa.avx.dup2@egalvanic.com", "QA Engineer");
        chooseLicense("No license");
        clickCreate();
        pause(5000);

        boolean rejected = page.isDialogOpen() && !dialogAlertText().isEmpty();
        if (page.isDialogOpen()) { page.clickCancel(); pause(800); }
        page.search(dupAccountName);
        pause(2000);
        int copies = rowsContaining(dupAccountName);
        page.clearSearch();
        logStep("Duplicate attempt — rejected=" + rejected + ", rows named '" + dupAccountName + "': " + copies);
        Assert.assertTrue(rejected && copies == 1,
                "ZP-3156 AC-6: a duplicate account name/domain must be blocked with an error and no partial"
                + " records. Observed instead: rejected=" + rejected + ", copies=" + copies
                + " (2026-07-30: POST /api/account/v2 returned 201 twice; tenant already holds several"
                + " accounts sharing subdomain 'egalvanic').");
        ExtentReportManager.logPass("Duplicate account name was rejected with an error and only one row exists.");
    }

    // ================================================================
    // Role matrix — menu visibility and deep-link access must agree
    // ================================================================

    @Test(priority = 30,
          description = "TC_AVX_08: for Admin and Account Manager, a visible account menu item implies a working account list")
    public void testRoleVisibilityMatrixAdminAndAccountManager() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AVX_08 — Admin/Account-Manager account access matrix");
        if (currentRole() == null) throw new SkipException("Role switcher not available on this session.");
        StringBuilder matrix = new StringBuilder();
        try {
            for (String role : new String[]{"Admin", "Account Manager"}) {
                switchRole(role);
                boolean navItem = sidebarHasText("Accounts") || sidebarHasText("Customers");
                driver.get(AppConstants.BASE_URL + "/accounts");
                pause(6000);
                waitAndDismissAppAlert();
                boolean reachable = onAccountsRoute(driver.getCurrentUrl()) && page.isGridPresent();
                String body = page.bodyText();
                matrix.append(role).append(": menu=").append(navItem)
                      .append(", deep-link=").append(reachable).append("; ");
                logStep(role + " — account item in menu: " + navItem + ", deep link works: " + reachable
                        + " (landed " + driver.getCurrentUrl() + ")");
                Assert.assertFalse(body.contains("Something went wrong"),
                        role + ": the app must not crash when probing account access.");
                if (navItem) {
                    Assert.assertTrue(reachable,
                            role + ": the menu offers the account item but the deep link fails — menu and"
                            + " access disagree (ZP-3198/3210 family).");
                }
            }
        } finally {
            switchRole(HOME_ROLE);
        }
        ExtentReportManager.logPass("Role access matrix consistent — " + matrix);
    }

    // ================================================================
    // Helpers (JS patterns shared with AccountV135RegressionTestNG)
    // ================================================================

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private static String snippet(String t) {
        return t == null ? "" : t.substring(0, Math.min(160, t.length())).replace('\n', ' ');
    }

    /** Text of any [role=alert]/MuiAlert currently shown (create-dialog validation surface). */
    private String dialogAlertText() {
        Object t = js().executeScript(
                "var els=document.querySelectorAll(\"[role='alert'], .MuiAlert-root\");"
              + "var out='';for(var i=0;i<els.length;i++){out+=els[i].textContent.trim()+' ';}"
              + "return out.trim();");
        return t == null ? "" : String.valueOf(t);
    }

    private boolean dialogHasButton(String label) {
        Object r = js().executeScript(
                "var dlg=document.querySelector('[role=\"dialog\"]'); if(!dlg) return false;"
              + "var bs=dlg.querySelectorAll('button');"
              + "for(var i=0;i<bs.length;i++){ if(bs[i].textContent.trim()===arguments[0]) return true; }"
              + "return false;", label);
        return Boolean.TRUE.equals(r);
    }

    private boolean facilitySelectorPresent() {
        return !driver.findElements(By.cssSelector("input[placeholder='Select facility']")).isEmpty();
    }

    private int rowsContaining(String text) {
        int n = 0;
        for (WebElement r : page.rows()) {
            try { if (r.getText().contains(text)) n++; } catch (Exception ignored) { }
        }
        return n;
    }

    private boolean sidebarHasText(String text) {
        Object r = js().executeScript(
                "var d=document.querySelector('.MuiDrawer-root, [class*=\"MuiDrawer\"]');"
              + "if(!d) return false;"
              + "var re=new RegExp('\\\\b'+arguments[0].replace(/[.*+?^${}()|[\\]\\\\]/g,'\\\\$&')+'\\\\b');"
              + "return re.test(d.innerText||'');", text);
        return Boolean.TRUE.equals(r);
    }

    private String currentRole() {
        Object v = js().executeScript(
                "var names=" + jsArray(ROLE_NAMES) + ";"
              + "var ins=document.querySelectorAll('input');"
              + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0) return ins[i].value; }"
              + "return null;");
        return v == null ? null : String.valueOf(v);
    }

    private void switchRole(String target) {
        if (target.equals(currentRole())) return;
        logStep("Switching role → " + target);
        String openScript =
                "var names=" + jsArray(ROLE_NAMES) + ";"
              + "var input=null, ins=document.querySelectorAll('input');"
              + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0){ input=ins[i]; break; } }"
              + "if(!input) throw 'role input not found';"
              + "input.scrollIntoView({block:'center'}); input.focus();"
              + "var w=input.closest('.MuiAutocomplete-root');"
              + "var b=w?w.querySelector('.MuiAutocomplete-popupIndicator'):null;"
              + "if(b) b.click(); else input.click();";
        // options render async — re-open and poll up to ~9s for the target option
        boolean clicked = false;
        for (int attempt = 1; attempt <= 3 && !clicked; attempt++) {
            js().executeScript(openScript);
            for (int i = 0; i < 12 && !clicked; i++) {
                pause(750);
                Object done = js().executeScript(
                        "var t=arguments[0];"
                      + "var opts=document.querySelectorAll(\"li[role='option']\");"
                      + "for (var i=0;i<opts.length;i++){"
                      + "  if(opts[i].textContent.trim()===t){"
                      + "    ['pointerdown','mousedown','pointerup','mouseup','click'].forEach(function(ev){"
                      + "      opts[i].dispatchEvent(new MouseEvent(ev,{bubbles:true,cancelable:true}));});"
                      + "    return true; } }"
                      + "return false;", target);
                clicked = Boolean.TRUE.equals(done);
            }
        }
        if (!clicked) throw new SkipException("Role option never offered after 3 open attempts: " + target);
        for (int i = 0; i < 30; i++) {
            pause(1000);
            try { if (target.equals(currentRole())) break; } catch (Exception ignored) { }
        }
        pause(2500);
        waitAndDismissAppAlert();
        logStep("Role now: " + currentRole() + " @ " + driver.getCurrentUrl());
    }

    private static String jsArray(List<String> vals) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vals.size(); i++) sb.append(i > 0 ? "," : "").append("'").append(vals.get(i)).append("'");
        return sb.append("]").toString();
    }

    private void selectFirstOwner() {
        WebElement owner = null;
        for (WebElement in : driver.findElements(By.cssSelector("div[role='dialog'] input"))) {
            String ph = in.getAttribute("placeholder");
            if (ph != null && ph.toLowerCase().contains("account owner")) { owner = in; break; }
        }
        if (owner == null) throw new SkipException("Account Owner input not found in the create dialog.");
        // The ~170-contact list loads async and occasionally misses the first open
        // gesture entirely (observed 2026-07-30: one 12s poll expired) — so run the
        // whole open-and-poll sequence up to twice before skipping.
        boolean haveOpts = false;
        for (int attempt = 1; attempt <= 2 && !haveOpts; attempt++) {
            js().executeScript("arguments[0].scrollIntoView({block:'center'});", owner);
            try { owner.click(); } catch (Exception e) { js().executeScript("arguments[0].click();", owner); }
            pause(600);
            js().executeScript(
                    "var w=arguments[0].closest('.MuiAutocomplete-root');"
                  + "var b=w?w.querySelector('.MuiAutocomplete-popupIndicator'):null;"
                  + "if(b && (w.getAttribute('aria-expanded')!=='true')) b.click();", owner);
            haveOpts = optionCount() > 0;
            if (!haveOpts && attempt == 2) {
                try { owner.sendKeys("a"); } catch (Exception ignored) { }
            }
            for (int i = 0; i < 24 && !haveOpts; i++) {   // up to ~12s per attempt
                pause(500);
                haveOpts = optionCount() > 0;
            }
        }
        if (!haveOpts) throw new SkipException("Account Owner options never loaded (2 attempts, ~24s).");
        js().executeScript(
                "var opts=document.querySelectorAll(\"li[role='option']\");"
              + "['pointerdown','mousedown','pointerup','mouseup','click'].forEach(function(ev){"
              + "  opts[0].dispatchEvent(new MouseEvent(ev,{bubbles:true,cancelable:true}));});");
        pause(800);
    }

    private int optionCount() {
        Object n = js().executeScript("return document.querySelectorAll(\"li[role='option']\").length;");
        return n instanceof Number ? ((Number) n).intValue() : 0;
    }

    private void fillContact(String first, String last, String email, String jobTitle) {
        String[][] fields = {{"firstname", first}, {"lastname", last}, {"email", email}, {"job_title", jobTitle}};
        for (String[] f : fields) {
            js().executeScript(
                    "var i=document.querySelector('[role=\"dialog\"] input[name=\"" + f[0] + "\"]');"
                  + "if(!i) throw 'contact input missing: " + f[0] + "';"
                  + "i.scrollIntoView({block:'center'});"
                  + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                  + "set.call(i, arguments[0]); i.dispatchEvent(new Event('input',{bubbles:true}));"
                  + "i.dispatchEvent(new Event('blur',{bubbles:true}));", f[1]);
            pause(200);
        }
    }

    private void chooseLicense(String label) {
        js().executeScript(
                "var dlg=document.querySelector('[role=\"dialog\"]');"
              + "var els=dlg.querySelectorAll('label, [class*=\"MuiFormControlLabel\"]');"
              + "for (var i=0;i<els.length;i++){"
              + "  if(els[i].textContent.indexOf(arguments[0])>=0){"
              + "    var r=els[i].querySelector('input[type=\"radio\"]');"
              + "    if(r){ r.click(); return; } els[i].click(); return; } }", label);
        pause(500);
    }

    private void clickCreate() {
        js().executeScript(
                "var dlg=document.querySelector('[role=\"dialog\"]');"
              + "var bs=dlg.querySelectorAll('button');"
              + "for (var i=0;i<bs.length;i++){ if(bs[i].textContent.trim()==='Create'){ bs[i].click(); return; } }"
              + "throw 'Create button not found';");
    }

    private boolean waitForDialogClosed(long ms) {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            if (!page.isDialogOpen()) return true;
            pause(500);
        }
        return !page.isDialogOpen();
    }

    private boolean waitForRow(String name, long ms) {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            if (page.hasRowContaining(name)) return true;
            pause(800);
        }
        return page.hasRowContaining(name);
    }

    private boolean clickRowIconAction(String name, String verb) {
        WebElement row = page.findRowContaining(name);
        if (row == null) return false;
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", row);
        List<WebElement> btns = row.findElements(By.cssSelector(
                "button[aria-label='" + verb + " Account'], button[aria-label*='" + verb + "'],"
              + " button[title*='" + verb + "'], button svg[data-testid='" + verb + "Icon']"));
        if (btns.isEmpty()) return false;
        WebElement b = btns.get(0);
        if ("svg".equalsIgnoreCase(b.getTagName())) {
            js().executeScript("arguments[0].closest('button').click();", b);
        } else {
            try { b.click(); } catch (Exception e) { js().executeScript("arguments[0].click();", b); }
        }
        pause(1000);
        return true;
    }

    private boolean deleteRowByName(String name) {
        if (!clickRowIconAction(name, "Delete")) {
            logStep("No delete control on row '" + name + "'.");
            return false;
        }
        pause(1000);
        List<WebElement> confirm = driver.findElements(AccountsPage.CONFIRM_DELETE_BTN);
        if (!confirm.isEmpty()) {
            js().executeScript("arguments[0].click();", confirm.get(0));
        }
        pause(2500);
        return !page.hasRowContaining(name);
    }
}
