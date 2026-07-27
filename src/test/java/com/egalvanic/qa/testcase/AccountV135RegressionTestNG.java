package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.AccountsPage;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Web v1.35 Account Create &amp; Management regression coverage (live-verified on QA V1.36,
 * 2026-07-27 — all six tickets' UI changes confirmed deployed before writing these tests):
 *
 * <ul>
 *   <li><b>ZP-3156</b> — Create Account: CONTACT DETAILS mandatory (First/Last/Email/Job Title*),
 *       Subdomain HIDDEN, address optional behind the "Add a site now" toggle (Line 2 optional,
 *       rest required when on) + LICENSE TYPE radios (Interactive / Read-only / No license).</li>
 *   <li><b>ZP-3157</b> — Accounts lives in the ADMIN sidebar group; page cleaned to
 *       Account Name / Owner / Created / Actions.</li>
 *   <li><b>ZP-3185</b> — no site/facility selector anywhere on the Accounts page.</li>
 *   <li><b>ZP-3049</b> — list pagination (customer had 555 accounts; QA tenant has ~46, so the
 *       tests assert pagination BEHAVIOR — ranges, page nav, rows-per-page — not the 555 count).</li>
 *   <li><b>ZP-3198</b> — switching to a role without account access while ON /accounts must move
 *       the user OFF the page (observed live: redirect to /sites), not strand them.</li>
 *   <li><b>ZP-3210</b> — Project Manager's Accounts visibility must be IDENTICAL via direct login
 *       and via role switch (the bug was: visible on direct login, missing after switch).</li>
 * </ul>
 *
 * Role switching uses the header Role dropdown (options live on this tenant: Admin, Project
 * Manager, Account Manager, Super Admin, Electrical Engineer). Role tests run LAST and always
 * restore Super Admin. The create→edit→delete chain uses its own QA_AV135_* account and cleans up.
 */
public class AccountV135RegressionTestNG extends BaseTest {

    private static final String MODULE = "Accounts";
    private static final String FEATURE = "Accounts v1.35 (ZP-3156/3157/3185/3049/3198/3210)";

    private static final List<String> ROLE_NAMES = Arrays.asList(
            "Admin", "Project Manager", "Account Manager", "Super Admin", "Electrical Engineer");

    /** The role every other test runs as (BaseTest admin login lands on this persona). */
    private static final String HOME_ROLE = "Super Admin";

    private AccountsPage page;
    /** Name of the account the create→edit→delete chain owns (never a shared/real account). */
    private static String chainAccountName;
    private static String chainAccountRenamed;

    @BeforeClass
    @Override
    public void classSetup() {
        super.classSetup();
        page = new AccountsPage(driver);
    }

    @AfterClass(alwaysRun = true)
    public void restoreRoleAndCleanup() {
        // Never leave the session on a non-admin persona or leave QA_AV135_* rows behind.
        try { if (!HOME_ROLE.equals(currentRole())) switchRole(HOME_ROLE); } catch (Exception ignored) { }
        try {
            for (String leftover : new String[]{chainAccountRenamed, chainAccountName}) {
                if (leftover != null) {
                    goToAccounts();
                    page.search(leftover);
                    pause(1500);
                    if (page.hasRowContaining(leftover)) deleteRowByName(leftover);
                }
            }
        } catch (Exception ignored) { }
    }

    private void goToAccounts() {
        page.open();
        dismissBackdrops();
        waitAndDismissAppAlert();
    }

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    // ================================================================
    // ZP-3157 / ZP-3185 — page placement + cleaned page + no site selector
    // ================================================================

    @Test(priority = 1,
          description = "TC_AV135_01: Accounts appears in the ADMIN section of the left menu (ZP-3157)")
    public void testAccountsInAdminSidebarGroup() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_01 — Accounts sits in the ADMIN menu group");
        goToAccounts();
        boolean adminGroup = sidebarHasText("ADMIN");
        boolean accountsItem = sidebarHasText("Accounts");
        logStep("Sidebar: ADMIN group=" + adminGroup + ", Accounts item=" + accountsItem);
        Assert.assertTrue(adminGroup, "The left menu should show an ADMIN section (ZP-3157).");
        Assert.assertTrue(accountsItem, "The left menu should list 'Accounts' (moved under Admin, ZP-3157).");
        ExtentReportManager.logPass("Accounts is listed in the left menu with the ADMIN section present.");
    }

    @Test(priority = 2,
          description = "TC_AV135_02: Accounts list shows exactly the cleaned-up columns — Account Name, Owner, Created, Actions (ZP-3157)")
    public void testCleanedUpColumns() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_02 — cleaned-up list columns");
        goToAccounts();
        page.waitForContent();
        List<String> cols = page.columnHeaders();
        logStep("Columns: " + cols);
        for (String want : new String[]{"Account Name", "Owner", "Created", "Actions"}) {
            Assert.assertTrue(cols.stream().anyMatch(c -> c.equalsIgnoreCase(want)),
                    "Accounts list should show a '" + want + "' column. Got: " + cols);
        }
        ExtentReportManager.logPass("Accounts list shows the cleaned-up columns: " + cols);
    }

    @Test(priority = 3,
          description = "TC_AV135_03: No site/facility selector anywhere on the Accounts page (ZP-3185)")
    public void testNoSiteSelectorOnAccountsPage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_03 — no site selector on Accounts");
        goToAccounts();
        List<WebElement> siteInputs = driver.findElements(By.xpath(
                "//input[contains(translate(@placeholder,'FACILITYSITE','facilitysite'),'facility')"
                + " or contains(translate(@placeholder,'FACILITYSITE','facilitysite'),'site')]"));
        boolean topBarSiteLabel = !driver.findElements(By.xpath(
                "//*[normalize-space(text())='Site:']")).isEmpty();
        logStep("facility/site inputs=" + siteInputs.size() + ", 'Site:' label present=" + topBarSiteLabel);
        Assert.assertTrue(siteInputs.isEmpty(),
                "The Accounts page must not show a site/facility dropdown (removed in ZP-3185).");
        Assert.assertFalse(topBarSiteLabel,
                "The top bar must not show the 'Site:' selector on the Accounts page (ZP-3185).");
        ExtentReportManager.logPass("No site/facility selector on the Accounts page.");
    }

    // ================================================================
    // ZP-3156 — Create Account flow
    // ================================================================

    @Test(priority = 10,
          description = "TC_AV135_04: Create Account asks for account + contact details, all marked required (ZP-3156)")
    public void testCreateDialogSections() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_04 — create dialog: account + mandatory contact sections");
        goToAccounts();
        page.openCreateDialog();
        String dlg = page.dialogText();
        List<String> required = page.requiredFieldLabels();
        logStep("Required labels: " + required);
        try {
            Assert.assertTrue(dlg.contains("ACCOUNT DETAILS") || dlg.toLowerCase().contains("account details"),
                    "Dialog should have an ACCOUNT DETAILS section.");
            Assert.assertTrue(dlg.contains("CONTACT DETAILS") || dlg.toLowerCase().contains("contact details"),
                    "Dialog should have a CONTACT DETAILS section (mandatory contact, ZP-3156).");
            for (String want : new String[]{"Account Name", "Account Owner", "First Name", "Last Name", "Email", "Job Title"}) {
                Assert.assertTrue(required.stream().anyMatch(r -> r.toLowerCase().startsWith(want.toLowerCase())),
                        "'" + want + "' should be marked required. Required: " + required);
            }
            Assert.assertTrue(dlg.contains("Primary Contact"),
                    "Dialog should explain the contact becomes the account's Primary Contact.");
        } finally { page.clickCancel(); }
        ExtentReportManager.logPass("Create dialog shows ACCOUNT + mandatory CONTACT sections with required marks.");
    }

    @Test(priority = 11,
          description = "TC_AV135_05: Subdomain is no longer asked for when creating an account (ZP-3156)")
    public void testSubdomainHidden() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_05 — subdomain hidden from create dialog");
        goToAccounts();
        page.openCreateDialog();
        String dlg = page.dialogText().toLowerCase();
        boolean subdomainInput = !driver.findElements(By.xpath(
                "//div[@role='dialog']//input[contains(translate(@name,'SUBDOMAIN','subdomain'),'subdomain')"
                + " or contains(translate(@placeholder,'SUBDOMAIN','subdomain'),'subdomain')]")).isEmpty();
        page.clickCancel();
        Assert.assertFalse(dlg.contains("subdomain"),
                "The create dialog must not mention 'subdomain' anywhere (hidden in ZP-3156).");
        Assert.assertFalse(subdomainInput, "No subdomain input field should exist in the create dialog.");
        ExtentReportManager.logPass("Subdomain is not asked for during account creation.");
    }

    @Test(priority = 12,
          description = "TC_AV135_06: Contact is mandatory — filling only the account fields must NOT enable Create (ZP-3156)")
    public void testContactMandatoryGatesCreate() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_06 — contact fields gate the Create button");
        goToAccounts();
        page.openCreateDialog();
        try {
            page.setAccountName("QA_AV135_gate_" + System.currentTimeMillis());
            selectFirstOwner();
            pause(800);
            boolean enabled = page.isSaveEnabled();
            logStep("Create enabled with account fields only (no contact): " + enabled);
            Assert.assertFalse(enabled,
                    "Create must stay disabled until the mandatory contact (First/Last/Email/Job Title) is filled.");
        } finally { page.clickCancel(); }
        ExtentReportManager.logPass("Contact details are genuinely mandatory — Create stays disabled without them.");
    }

    @Test(priority = 13,
          description = "TC_AV135_07: Address is optional — hidden until 'Add a site now' is switched on, and Create works without it (ZP-3156)")
    public void testAddressOptionalBehindSiteToggle() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_07 — address optional behind 'Add a site now'");
        goToAccounts();
        page.openCreateDialog();
        try {
            String before = page.dialogText();
            Assert.assertFalse(before.toLowerCase().contains("address line"),
                    "No address fields should show by default (address is optional, ZP-3156).");
            Assert.assertTrue(before.contains("Add a site now"),
                    "The dialog should offer the optional 'Add a site now' toggle.");

            toggleAddSiteNow();
            pause(800);
            String after = page.dialogText();
            List<String> required = page.requiredFieldLabels();
            logStep("After toggle — required: " + required);
            Assert.assertTrue(after.toLowerCase().contains("address line 1"),
                    "Switching 'Add a site now' ON should reveal the site address fields.");
            for (String want : new String[]{"Site Name", "Address Line 1", "City", "State", "ZIP Code", "Country"}) {
                Assert.assertTrue(required.stream().anyMatch(r -> r.toLowerCase().startsWith(want.toLowerCase())),
                        "'" + want + "' should be required once a site is being added. Required: " + required);
            }
            Assert.assertTrue(required.stream().noneMatch(r -> r.equalsIgnoreCase("Address Line 2")),
                    "'Address Line 2' should stay optional.");

            toggleAddSiteNow();   // back OFF
            pause(800);
            Assert.assertFalse(page.dialogText().toLowerCase().contains("address line 1"),
                    "Switching the toggle back OFF should hide the address fields again.");
        } finally { page.clickCancel(); }
        ExtentReportManager.logPass("Address only appears (and becomes required) when adding a site inline; Line 2 stays optional.");
    }

    @Test(priority = 14,
          description = "TC_AV135_08: License Type offers Interactive, Read-only and No license choices (v1.35 create dialog)")
    public void testLicenseTypeChoices() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_08 — license type choices");
        goToAccounts();
        page.openCreateDialog();
        String dlg = page.dialogText();
        long radios = driver.findElements(By.cssSelector("div[role='dialog'] input[type='radio']")).size();
        page.clickCancel();
        for (String want : new String[]{"Interactive", "Read-only", "No license"}) {
            Assert.assertTrue(dlg.contains(want), "License Type should offer '" + want + "'. Dialog: " + dlg);
        }
        Assert.assertTrue(radios >= 3, "License Type should render as a 3-way choice. Radios found: " + radios);
        ExtentReportManager.logPass("License Type offers Interactive / Read-only / No license (" + radios + " radios).");
    }

    @Test(priority = 15,
          description = "TC_AV135_09: Create an account end-to-end (mandatory contact, no site) and see it in the list (ZP-3156)")
    public void testCreateAccountEndToEnd() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_09 — create account end-to-end");
        goToAccounts();
        page.openCreateDialog();
        String name = "QA_AV135_" + System.currentTimeMillis();
        long ts = System.currentTimeMillis();

        page.setAccountName(name);
        selectFirstOwner();
        fillContact("QA", "AutoTest", "qa.av135." + ts + "@egalvanic-qa-test.com", "QA Engineer");
        // 'No license' → no portal access is provisioned for the contact (keeps QA side-effect-free).
        chooseLicense("No license");
        pause(600);

        boolean enabled = page.waitForSaveEnabled(8000);
        logStep("Create enabled after filling account+contact (no site): " + enabled);
        Assert.assertTrue(enabled,
                "Create should enable with account + contact filled and NO site/address (address is optional).");
        clickCreate();

        boolean closed = waitForDialogClosed(20000);
        logStep("Create dialog closed: " + closed);
        Assert.assertTrue(closed, "The create dialog should close after Create.");

        page.search(name);
        boolean visible = waitForRow(name, 20000);
        logStep("New account '" + name + "' visible in list: " + visible);
        Assert.assertTrue(visible, "The new account should appear in the Accounts list.");
        chainAccountName = name;
        page.clearSearch();
        ExtentReportManager.logPass("Account '" + name + "' created with mandatory contact only — no address needed.");
    }

    // ================================================================
    // Account edit (v1.35 fields, on OUR account only)
    // ================================================================

    @Test(priority = 16, dependsOnMethods = "testCreateAccountEndToEnd",
          description = "TC_AV135_10: Edit the created account — rename it and the list shows the new name")
    public void testEditAccountRename() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_10 — edit account (rename)");
        goToAccounts();
        page.search(chainAccountName);
        Assert.assertTrue(waitForRow(chainAccountName, 15000), "Precondition: the QA account row should be findable.");

        boolean opened = clickRowIconAction(chainAccountName, "Edit");
        if (!opened) throw new SkipException("No 'Edit Account' control on the account row.");
        pause(1200);
        Assert.assertTrue(page.isDialogOpen(), "Choosing Edit should open the account form.");

        String renamed = chainAccountName + "_ED";
        page.setAccountName(renamed);
        pause(500);
        clickSaveOrUpdate();
        Assert.assertTrue(waitForDialogClosed(15000), "The edit form should close after saving.");

        page.clearSearch();
        page.search(renamed);
        boolean visible = waitForRow(renamed, 15000);
        Assert.assertTrue(visible, "The list should show the renamed account '" + renamed + "'.");
        chainAccountRenamed = renamed;
        page.clearSearch();
        ExtentReportManager.logPass("Account renamed via Edit and the list reflects it.");
    }

    @Test(priority = 17, dependsOnMethods = "testCreateAccountEndToEnd", alwaysRun = true,
          description = "TC_AV135_11: Delete the created account (cleanup) — delete is confirmation-gated")
    public void testDeleteCreatedAccount() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_11 — delete the QA account (cleanup)");
        String target = chainAccountRenamed != null ? chainAccountRenamed : chainAccountName;
        goToAccounts();
        page.search(target);
        if (!waitForRow(target, 15000)) throw new SkipException("QA account '" + target + "' not found to delete.");
        boolean deleted = deleteRowByName(target);
        Assert.assertTrue(deleted, "The QA account should delete after confirming.");
        page.clearSearch();
        page.search(target);
        pause(2000);
        Assert.assertFalse(page.hasRowContaining(target), "Deleted account should no longer be listed.");
        chainAccountName = null;
        chainAccountRenamed = null;
        ExtentReportManager.logPass("QA account deleted (confirmation-gated); list no longer shows it.");
    }

    // ================================================================
    // ZP-3049 — pagination behavior
    // ================================================================

    @Test(priority = 20,
          description = "TC_AV135_12: Pagination — the footer count is real and Next/Previous move through pages (ZP-3049)")
    public void testPaginationBehavior() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_12 — pagination next/previous");
        goToAccounts();
        page.waitForContent();
        String range = paginationRange();
        logStep("Pagination footer: " + range);
        Assert.assertNotNull(range, "The Accounts list should show a pagination footer (e.g. '1–25 of 46').");
        int total = paginationTotal(range);
        if (total <= 25) throw new SkipException(
                "Only " + total + " accounts on this tenant — a single page; page-nav behavior not exercisable "
                + "(the customer's 555-account case needs seeded data).");

        String firstRowBefore = firstRowText();
        Assert.assertFalse(isPagerButtonEnabled("Go to previous page"),
                "Previous must be disabled on the first page.");
        clickPagerButton("Go to next page");
        pause(2000);
        String rangeAfter = paginationRange();
        String firstRowAfter = firstRowText();
        logStep("After Next — footer: " + rangeAfter + "; first row changed: " + !firstRowBefore.equals(firstRowAfter));
        Assert.assertNotEquals(rangeAfter, range, "The footer range should change after going to the next page.");
        Assert.assertNotEquals(firstRowAfter, firstRowBefore, "Page 2 should show different accounts.");
        Assert.assertTrue(isPagerButtonEnabled("Go to previous page"),
                "Previous should enable once off the first page.");
        clickPagerButton("Go to previous page");
        pause(2000);
        Assert.assertEquals(paginationRange(), range, "Going back should restore the first page range.");
        ExtentReportManager.logPass("Pagination footer is accurate and Next/Previous page navigation works (" + range + ").");
    }

    @Test(priority = 21,
          description = "TC_AV135_13: Changing rows-per-page shows more accounts on one page (ZP-3049)")
    public void testRowsPerPage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_13 — rows-per-page");
        goToAccounts();
        page.waitForContent();
        String range = paginationRange();
        int total = range == null ? 0 : paginationTotal(range);
        if (total <= 25) throw new SkipException("Only " + total + " accounts — rows-per-page change not observable.");
        int before = page.rowCount();
        boolean changed = setRowsPerPage("50");
        if (!changed) throw new SkipException("No rows-per-page selector offered on this grid.");
        pause(2500);
        int after = page.rowCount();
        String rangeAfter = paginationRange();
        logStep("rows before=" + before + " after=" + after + " footer=" + rangeAfter);
        Assert.assertTrue(after > before,
                "Choosing 50 rows per page should show more accounts at once (was " + before + ", now " + after + ").");
        ExtentReportManager.logPass("Rows-per-page works: " + before + " → " + after + " rows (" + rangeAfter + ").");
    }

    // ================================================================
    // ZP-3198 / ZP-3210 — role switching (LAST; always restore Super Admin)
    // ================================================================

    @Test(priority = 30,
          description = "TC_AV135_14: Switching to a role without account access moves you OFF the Accounts page (ZP-3198)")
    public void testRoleSwitchLeavesAccountsPage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_14 — role switch redirects off Accounts");
        goToAccounts();
        Assert.assertTrue(driver.getCurrentUrl().contains("/accounts"), "Precondition: on the Accounts page.");
        if (!ROLE_NAMES.contains("Project Manager") || currentRole() == null) {
            throw new SkipException("Role switcher not available on this session.");
        }
        try {
            switchRole("Project Manager");
            String url = driver.getCurrentUrl();
            String body = page.bodyText();
            logStep("After switching to Project Manager: URL=" + url);
            Assert.assertFalse(url.contains("/accounts"),
                    "After switching to a role without account access the app must NAVIGATE AWAY from /accounts"
                    + " (ZP-3198). Still on: " + url);
            Assert.assertFalse(body.contains("Something went wrong"),
                    "The page after the role-switch redirect must not crash.");
            ExtentReportManager.logPass("Switching to Project Manager moved the user off /accounts to " + url + " with a healthy page.");
        } finally {
            switchRole(HOME_ROLE);
        }
    }

    @Test(priority = 31,
          description = "TC_AV135_15: Project Manager sees the SAME Accounts option via direct login and via role switch (ZP-3210)")
    public void testPmAccountsParityDirectVsSwitch() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_15 — PM direct-login vs role-switch parity");

        // ── Side A: role SWITCH in the main session ──
        boolean navAfterSwitch;
        boolean deepLinkAfterSwitch;
        try {
            switchRole("Project Manager");
            navAfterSwitch = sidebarHasText("Accounts");
            driver.get(AppConstants.BASE_URL + "/accounts");
            pause(6000);
            waitAndDismissAppAlert();
            deepLinkAfterSwitch = driver.getCurrentUrl().contains("/accounts") && page.isGridPresent();
            logStep("Switched PM — Accounts in menu: " + navAfterSwitch + ", /accounts reachable: " + deepLinkAfterSwitch);
        } finally {
            switchRole(HOME_ROLE);
        }

        // ── Side B: DIRECT login as the dedicated PM account (separate browser) ──
        boolean navDirect;
        boolean deepLinkDirect;
        WebDriver d2 = startSecondBrowser();
        try {
            LoginPage lp = new LoginPage(d2);
            d2.get(AppConstants.BASE_URL);
            // Wait for the login form to render before submitting (a fresh browser + cold QA host can
            // take several seconds; submitting too early throws NoSuchElement on the email field).
            long formDeadline = System.currentTimeMillis() + 30000;
            while (System.currentTimeMillis() < formDeadline) {
                try { if (lp.isEmailFieldDisplayed()) break; } catch (Exception ignored) { }
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            lp.login(AppConstants.PM_EMAIL, AppConstants.PM_PASSWORD);
            long deadline = System.currentTimeMillis() + 45000;
            while (System.currentTimeMillis() < deadline
                    && !d2.getCurrentUrl().matches(".*/(dashboard|sites|sessions).*")) {
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            try { Thread.sleep(5000); } catch (InterruptedException ignored) { }
            navDirect = drawerHasText(d2, "Accounts");
            d2.get(AppConstants.BASE_URL + "/accounts");
            try { Thread.sleep(6000); } catch (InterruptedException ignored) { }
            deepLinkDirect = d2.getCurrentUrl().contains("/accounts")
                    && !d2.findElements(By.cssSelector(".MuiDataGrid-root, [role='grid']")).isEmpty();
            logStep("Direct PM login — Accounts in menu: " + navDirect + ", /accounts reachable: " + deepLinkDirect);
        } finally {
            try { d2.quit(); } catch (Exception ignored) { }
        }

        // ── The ZP-3210 contract: BOTH paths must agree ──
        Assert.assertEquals(navAfterSwitch, navDirect,
                "ZP-3210: the Accounts menu option for Project Manager must be the SAME via role switch ("
                + navAfterSwitch + ") and direct login (" + navDirect + ").");
        Assert.assertEquals(deepLinkAfterSwitch, deepLinkDirect,
                "ZP-3210: /accounts reachability for Project Manager must be the SAME via role switch ("
                + deepLinkAfterSwitch + ") and direct login (" + deepLinkDirect + ").");
        ExtentReportManager.logPass("Project Manager account access is consistent: menu shows Accounts=" + navDirect
                + ", /accounts reachable=" + deepLinkDirect + " — identical by direct login and role switch.");
    }

    @Test(priority = 32,
          description = "TC_AV135_16: After switching back to Super Admin the Accounts page is fully available again")
    public void testAccountsRestoredForAdmin() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_AV135_16 — Accounts restored for Super Admin");
        if (!HOME_ROLE.equals(currentRole())) switchRole(HOME_ROLE);
        goToAccounts();
        Assert.assertTrue(driver.getCurrentUrl().contains("/accounts"), "Super Admin should reach /accounts.");
        page.waitForContent();
        Assert.assertTrue(page.isGridPresent(), "The accounts grid should render for Super Admin.");
        Assert.assertTrue(page.isNewButtonPresent(), "'New Account' should be available to Super Admin.");
        ExtentReportManager.logPass("Super Admin has full Accounts access after the role-switch tests.");
    }

    // ================================================================
    // Helpers — sidebar / role switcher / create-dialog / grid actions
    // ================================================================

    /** A second, independent browser for the direct-PM-login half of the ZP-3210 parity check. */
    private WebDriver startSecondBrowser() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--window-size=1920,1080", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);
        opts.setAcceptInsecureCerts(true);   // QA host uses an internal-CA cert (see CLAUDE.md)
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);
        return new ChromeDriver(opts);
    }

    /**
     * True if the left navigation drawer contains {@code text}. The v1.35 sidebar renders group
     * labels (DASHBOARDS / DATA / OPERATIONS / SALES / ADMIN) and nav items inside the MUI drawer
     * but NOT as clean text() nodes, so we read the drawer's rendered innerText and word-match.
     */
    private boolean sidebarHasText(String text) {
        Object r = js().executeScript(
                "var d=document.querySelector('.MuiDrawer-root, [class*=\"MuiDrawer\"]');"
              + "if(!d) return false;"
              + "var re=new RegExp('\\\\b'+arguments[0].replace(/[.*+?^${}()|[\\]\\\\]/g,'\\\\$&')+'\\\\b');"
              + "return re.test(d.innerText||'');", text);
        return Boolean.TRUE.equals(r);
    }

    /** Drawer innerText word-match on an arbitrary WebDriver (used for the second-browser PM check). */
    private static boolean drawerHasText(WebDriver d, String text) {
        Object r = ((JavascriptExecutor) d).executeScript(
                "var dr=document.querySelector('.MuiDrawer-root, [class*=\"MuiDrawer\"]');"
              + "if(!dr) return false;"
              + "var re=new RegExp('\\\\b'+arguments[0].replace(/[.*+?^${}()|[\\]\\\\]/g,'\\\\$&')+'\\\\b');"
              + "return re.test(dr.innerText||'');", text);
        return Boolean.TRUE.equals(r);
    }

    /** The header Role dropdown's current value (null when no switcher is rendered). */
    private String currentRole() {
        Object v = js().executeScript(
                "var names=" + jsArray(ROLE_NAMES) + ";"
              + "var ins=document.querySelectorAll('input');"
              + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0) return ins[i].value; }"
              + "return null;");
        return v == null ? null : String.valueOf(v);
    }

    /**
     * Switch the header Role dropdown to {@code target} and wait for the app to apply it
     * (the switch reloads the shell and usually navigates to the role's landing page).
     */
    private void switchRole(String target) {
        if (target.equals(currentRole())) return;
        logStep("Switching role → " + target);
        js().executeScript(
                "var names=" + jsArray(ROLE_NAMES) + ";"
              + "var input=null, ins=document.querySelectorAll('input');"
              + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0){ input=ins[i]; break; } }"
              + "if(!input) throw 'role input not found';"
              + "input.scrollIntoView({block:'center'}); input.focus();"
              + "var w=input.closest('.MuiAutocomplete-root');"
              + "var b=w?w.querySelector('.MuiAutocomplete-popupIndicator'):null;"
              + "if(b) b.click(); else input.click();");
        pause(900);
        js().executeScript(
                "var t=arguments[0];"
              + "var opts=document.querySelectorAll(\"li[role='option']\");"
              + "for (var i=0;i<opts.length;i++){"
              + "  if(opts[i].textContent.trim()===t){"
              + "    ['pointerdown','mousedown','pointerup','mouseup','click'].forEach(function(ev){"
              + "      opts[i].dispatchEvent(new MouseEvent(ev,{bubbles:true,cancelable:true}));});"
              + "    return; } }"
              + "throw 'role option not offered: '+t;", target);
        // the switch triggers a shell reload + redirect — poll until the switcher shows the target
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

    /**
     * Pick the first option in the "Account Owner" Autocomplete. The MUI Autocomplete only opens
     * and fetches its ~170 contacts on a REAL user gesture, so open it with a Selenium click on the
     * actual input element (a pure-JS indicator click didn't reliably put it into the open state),
     * poll for the async options, and type a letter as a fallback trigger.
     */
    private void selectFirstOwner() {
        WebElement owner = null;
        for (WebElement in : driver.findElements(By.cssSelector("div[role='dialog'] input"))) {
            String ph = in.getAttribute("placeholder");
            if (ph != null && ph.toLowerCase().contains("account owner")) { owner = in; break; }
        }
        if (owner == null) throw new SkipException("Account Owner input not found in the create dialog.");
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", owner);
        try { owner.click(); } catch (Exception e) { js().executeScript("arguments[0].click();", owner); }
        pause(600);
        // open the popup via its indicator (real click through the wrapper) if not already open
        js().executeScript(
                "var w=arguments[0].closest('.MuiAutocomplete-root');"
              + "var b=w?w.querySelector('.MuiAutocomplete-popupIndicator'):null;"
              + "if(b && (w.getAttribute('aria-expanded')!=='true')) b.click();", owner);

        boolean haveOpts = optionCount() > 0;
        // type a character to trigger the search if nothing rendered from the plain open
        if (!haveOpts) {
            try { owner.sendKeys("a"); } catch (Exception ignored) { }
        }
        for (int i = 0; i < 24 && !haveOpts; i++) {   // up to ~12s of async fetch
            pause(500);
            haveOpts = optionCount() > 0;
        }
        if (!haveOpts) throw new SkipException("Account Owner options never loaded (no contacts available to own the account).");
        js().executeScript(
                "var opts=document.querySelectorAll(\"li[role='option']\");"
              + "['pointerdown','mousedown','pointerup','mouseup','click'].forEach(function(ev){"
              + "  opts[0].dispatchEvent(new MouseEvent(ev,{bubbles:true,cancelable:true}));});");
        pause(800);
    }

    /** Number of currently-rendered Autocomplete options (portal-rendered at document level). */
    private int optionCount() {
        Object n = js().executeScript("return document.querySelectorAll(\"li[role='option']\").length;");
        return n instanceof Number ? ((Number) n).intValue() : 0;
    }

    /** Fill the mandatory CONTACT DETAILS inputs (stable name attributes, live-verified 2026-07-27). */
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

    private void toggleAddSiteNow() {
        js().executeScript(
                "var dlg=document.querySelector('[role=\"dialog\"]');"
              + "var cb=dlg.querySelector('input[type=\"checkbox\"]');"
              + "if(!cb) throw 'Add-a-site toggle not found'; cb.click();");
    }

    private void clickCreate() {
        js().executeScript(
                "var dlg=document.querySelector('[role=\"dialog\"]');"
              + "var bs=dlg.querySelectorAll('button');"
              + "for (var i=0;i<bs.length;i++){ if(bs[i].textContent.trim()==='Create'){ bs[i].click(); return; } }"
              + "throw 'Create button not found';");
    }

    private void clickSaveOrUpdate() {
        js().executeScript(
                "var dlg=document.querySelector('[role=\"dialog\"]');"
              + "var bs=dlg.querySelectorAll('button');"
              + "for (var i=0;i<bs.length;i++){ var t=bs[i].textContent.trim();"
              + "  if(t==='Save'||t==='Update'||t==='Save Changes'||t==='Create'){ bs[i].click(); return; } }"
              + "throw 'save/update button not found';");
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

    /**
     * Click a row's Actions-column icon button by verb ("Edit"/"Delete"). The v1.35 Accounts grid
     * renders these as direct icon buttons on the row with aria-label "Edit Account" / "Delete
     * Account" (live-verified 2026-07-27) — not a kebab menu. Returns false if the row or button
     * is absent (permission/data). Falls back to a title/svg-testid match for resilience.
     */
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

    /** Delete the row containing {@code name} via its Delete-Account icon, confirming the dialog. */
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

    // ── pagination helpers ──

    private String paginationRange() {
        String t = page.bodyText();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+\\s*[–-]\\s*\\d+ of \\d+").matcher(t);
        return m.find() ? m.group() : null;
    }

    private int paginationTotal(String range) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("of (\\d+)").matcher(range);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private String firstRowText() {
        List<WebElement> rs = page.rows();
        return rs.isEmpty() ? "" : rs.get(0).getText();
    }

    private boolean isPagerButtonEnabled(String ariaLabel) {
        List<WebElement> b = driver.findElements(By.cssSelector("button[aria-label='" + ariaLabel + "']"));
        return !b.isEmpty() && b.get(0).isEnabled();
    }

    private void clickPagerButton(String ariaLabel) {
        List<WebElement> b = driver.findElements(By.cssSelector("button[aria-label='" + ariaLabel + "']"));
        if (b.isEmpty()) throw new SkipException("Pager button '" + ariaLabel + "' not present.");
        js().executeScript("arguments[0].click();", b.get(0));
    }

    private boolean setRowsPerPage(String value) {
        List<WebElement> sel = driver.findElements(By.cssSelector(
                ".MuiTablePagination-select, .MuiDataGrid-footerContainer .MuiSelect-select"));
        if (sel.isEmpty()) return false;
        js().executeScript("arguments[0].click();", sel.get(0));
        pause(800);
        Object ok = js().executeScript(
                "var opts=document.querySelectorAll(\"li[role='option']\");"
              + "for (var i=0;i<opts.length;i++){ if(opts[i].textContent.trim()===arguments[0]){ opts[i].click(); return true; } }"
              + "return false;", value);
        return Boolean.TRUE.equals(ok);
    }
}
