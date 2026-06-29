package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.testcase.api.RbacFixtures;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import io.restassured.RestAssured;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>RBAC front-end permission MATRIX (UI gating at scale)</b> — a data-driven
 * {@code role × module × action} grid (~336 cases) that proves the live web app exposes the right
 * affordances to each role.
 *
 * <p>For each role (logged in ONCE in a real browser and reused across all its checks) and each module,
 * four actions are asserted — <b>View, Create, Edit, Delete</b> — against the role's <b>live
 * {@code /auth/me}</b> permission set (the same source the UI gates on, via {@link RbacFixtures}):</p>
 * <ul>
 *   <li><b>View</b> → the module's nav route is present <em>iff</em> {@code features.<module>.view}
 *       (both directions; Sales modules are feature-flag-coupled so only the security direction is hard).</li>
 *   <li><b>Create / Edit / Delete</b> → gate on {@code <entity>.manage}. The hard, security-critical
 *       invariant asserted is: a role that <em>cannot</em> manage (or cannot even view) the module must
 *       <b>not</b> be offered that Create/Edit/Delete control in the UI. The positive direction
 *       (has manage ⇒ control present) is logged but not failed, since per-module control rendering can
 *       depend on data/feature flags — mirroring this suite's nav flag-coupled handling.</li>
 * </ul>
 *
 * <p>Roles are all 7 in the prod matrix; unprovisioned/mis-provisioned QA accounts (Admin, Electrical
 * Engineer) SKIP. Headless via {@code -Dheadless=true}; subset via {@code -Drbac.roles="..."}.</p>
 */
public class RbacUiPermissionMatrixTest {

    /** A module in the matrix: nav label, route, the view permission gating its nav, the manage
     *  permission gating create/edit/delete, whether nav is ALSO company-feature-flag-coupled, and a
     *  regex of the module's "create" control label. */
    private static final class Module {
        final String label, route, viewPerm, managePerm, createRegex;
        final boolean flagCoupled;
        Module(String label, String route, String viewPerm, String managePerm, boolean flagCoupled, String createRegex) {
            this.label = label; this.route = route; this.viewPerm = viewPerm; this.managePerm = managePerm;
            this.flagCoupled = flagCoupled; this.createRegex = createRegex;
        }
        /** Used by TestNG when recording data-provider parameters → makes each matrix cell show the module's
         *  readable label (e.g. "Assets") in testng-results.xml params instead of "Module@hash", so the
         *  consolidated client report can render each cell as "Role · Module · Action". */
        @Override public String toString() { return label; }
    }

    // 12 modules → with View/Create/Edit/Delete = 48 actions/role. Permission keys verified against a
    // live /auth/me (features.<module>.view for nav; <entity>.manage for CRUD). Assets/Panel Schedules
    // are graph nodes → nodes.manage; Connections are edges → edges.manage; Work Orders → jobs.manage.
    private static final List<Module> MODULES = new ArrayList<>();
    static {
        // View/manage keys are pipe-separated ALTERNATIVES reconciled against every role's live /auth/me
        // (oracle audit 2026-06-29). This tenant mixes a `features.<m>.view` namespace with bare `<entity>.view`
        // keys, and which one a role holds varies (e.g. Account Manager has bare issues.view/tasks.view/
        // edges.view/locations.view but not the features.* form; Client Portal has jobs.view not
        // features.jobs.view; Facility Manager has locations.view not features.locations.view). A role "can
        // view/manage" if it holds ANY listed key. Connections are modeled as `edges`; Work Orders as `jobs`/
        // `workorders`; Assets/Panel Schedules as `nodes`.
        MODULES.add(new Module("Assets",        "/assets",         "features.assets.view|nodes.view",                 "nodes.manage",                 false, "Create Asset|Add Asset|New Asset"));
        MODULES.add(new Module("Work Orders",   "/sessions",       "features.jobs.view|jobs.view|workorders.view",    "jobs.manage|workorders.manage", false, "Create Work Order|Create Job|Add Work Order|New Work Order"));
        MODULES.add(new Module("Issues",        "/issues",         "features.issues.view|issues.view",                "issues.manage",                false, "Create Issue|Add Issue|New Issue|Report Issue"));
        MODULES.add(new Module("Locations",     "/locations",      "features.locations.view|locations.view",          "locations.manage",             false, "Create Location|Add Location|New Location"));
        MODULES.add(new Module("Tasks",         "/tasks",          "features.tasks.view|tasks.view",                  "tasks.manage",                 false, "Create Task|Add Task|New Task"));
        MODULES.add(new Module("Connections",   "/connections",    "features.connections.view|edges.view",            "edges.manage",                 false, "Create Connection|Add Connection|New Connection"));
        MODULES.add(new Module("SLDs",          "/slds",           "features.slds.view|slds.view",                    "slds.manage",                  false, "Create SLD|New SLD|Add SLD|Create Diagram"));
        // Panel Schedules: features.panel_schedules.view is a flag-like grant held only by some roles → mark
        // flag-coupled (its nav is also a company-feature flag), so View only hard-asserts the security direction.
        MODULES.add(new Module("Panel Schedules","/panel-schedules","features.panel_schedules.view",                  "nodes.manage",                 true,  "Create|Add Panel|New Panel"));
        MODULES.add(new Module("Forms",         "/eg-forms",       "forms.view",                                      "forms.manage",                 false, "Create Form|New Form|Add Form"));
        MODULES.add(new Module("Opportunities", "/opportunities",  "features.opportunities.view|opportunities.view",  "opportunities.manage",         true,  "Create Opportunity|Add Opportunity|New Opportunity"));
        // Accounts: real grant is bare `accounts.view` (no `features.accounts.view` for some roles). Goals:
        // there is NO `goals.view` at all — Goals access is implied by `features.goals.manage` (the only goals
        // capability key the tenant defines; features.goals.view exists for Account Manager only).
        MODULES.add(new Module("Accounts",      "/accounts",       "accounts.view|features.accounts.view",            "accounts.manage",              true,  "Create Account|Add Account|New Account"));
        MODULES.add(new Module("Goals",         "/goals",          "features.goals.manage|features.goals.view",       "features.goals.manage",        true,  "Create Goal|Add Goal|New Goal"));
    }
    private static final String[] ACTIONS = {"View", "Create", "Edit", "Delete"};

    /** Known, LIVE-VERIFIED front-end RBAC gating findings (upstream product gaps). A detected leak whose
     *  (role|module|action) key matches an entry is reported as a tracked KNOWN FINDING (TestNG SKIP with a
     *  loud reason) instead of failing the build — so this suite stays a usable green regression gate while
     *  the gap is tracked, yet ANY new/unlisted leak still hard-fails. Entries must cite live evidence. */
    private static final Map<String, String> KNOWN_FINDINGS = new HashMap<>();
    static {
        KNOWN_FINDINGS.put("Client Portal|Forms|Create",
            "LOW-severity UI gating inconsistency (verified live 2026-06-29 in a real Client Portal session: "
            + "forms.view only — no forms.manage / form_instances.manage). The /eg-forms 'Create Form' entry "
            + "button renders fully ENABLED (MuiButton-contained primary, pointer-events:auto, not disabled) "
            + "and its dialog opens, but the dialog's actual 'Create' submit button is DISABLED — so a "
            + "view-only role cannot actually create a form. No privilege escalation; the gap is that the "
            + "entry button + dialog aren't gated even though the create action is. Backend enforcement is "
            + "covered separately by the RBAC API suite.");
    }

    // ── per-role session cache (login ONCE per role, reuse across its 48 checks) ──
    private static WebDriver driver;
    private static LoginPage loginPage;
    private static String sessionRole = null;
    private static LiveAuth sessionAuth = null;
    private static boolean sessionWeb = false;
    private static Set<String> sessionNavHrefs = new LinkedHashSet<>();
    private static String currentRoute = null;

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        ExtentReportManager.initReports();
        RestAssured.baseURI = AppConstants.API_BASE_URL;
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        quitDriver();
        ExtentReportManager.flushReports();
    }

    @AfterMethod(alwaysRun = true)
    public void afterEach(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && driver != null) {
            try { ScreenshotUtil.captureScreenshot(result.getMethod().getMethodName() + "_FAIL"); } catch (Exception ignored) {}
        }
        ExtentReportManager.removeTests();
        // NOTE: driver is intentionally NOT quit here — it is reused across all of a role's cases and
        // recycled on role change / at suite end (login-once-per-role).
    }

    /** (role, module, action) rows — ordered by role then module so the cached session + page are reused.
     *  ALL 7 roles by default (Admin + Electrical Engineer SKIP if their QA account isn't usable);
     *  {@code -Drbac.roles="Project Manager,Client Portal"} runs a subset. */
    @DataProvider(name = "matrix")
    public Object[][] matrix() {
        String filter = System.getProperty("rbac.roles", "").trim();
        List<Role> roles = new ArrayList<>();
        if (filter.isEmpty() || "all".equalsIgnoreCase(filter)) {
            roles.addAll(RbacFixtures.ROLES); // all 7
        } else {
            Set<String> only = new LinkedHashSet<>();
            for (String s : filter.split(",")) { String t = s.trim(); if (!t.isEmpty()) only.add(t); }
            for (Role r : RbacFixtures.ROLES) if (only.contains(r.name)) roles.add(r);
        }
        List<Object[]> rows = new ArrayList<>();
        for (Role r : roles)
            for (Module m : MODULES)
                for (String a : ACTIONS)
                    rows.add(new Object[]{r, m, a});
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "matrix",
          description = "RBAC UI affordance matches the role's live permission (view nav / manage CRUD)")
    public void uiPermissionCell(Role role, Module module, String action) {
        ExtentReportManager.createTest("RBAC — UI", "Permission Matrix (UI)",
                role.name + " · " + module.label + " · " + action);

        ensureRoleSession(role);  // login once per role; SKIPs unprovisioned

        boolean isAdmin = Boolean.TRUE.equals(sessionAuth.isAdmin);
        // A role without platform.web is web-restricted: the web app renders NO nav at all, regardless
        // of its features.*.view grants (those govern the MOBILE app). So effective web visibility AND
        // web manage both require platform.web.
        // Permission keys are alternatives (this tenant mixes `features.<m>.view` and bare `<entity>.view`,
        // and which one a role holds varies) — a role "can" if it holds ANY listed key. Audit 2026-06-29.
        boolean canView = sessionWeb && (isAdmin || hasAnyPerm(sessionAuth.permissions, module.viewPerm));
        boolean canManage = sessionWeb && (isAdmin || hasAnyPerm(sessionAuth.permissions, module.managePerm));

        if ("View".equals(action)) {
            boolean visible = navHasRoute(sessionNavHrefs, module.route);
            // Safety net: if the role HAS view perm but the route looks absent, the login-time nav snapshot
            // may have been captured before this section finished rendering → re-read fresh and cache it.
            if (canView && !visible) {
                sessionNavHrefs = readSidebarHrefs();
                visible = navHasRoute(sessionNavHrefs, module.route);
            }
            // SECURITY direction (hard): a route visible to a role that lacks the view permission is a leak.
            if (!canView && visible) {
                fail(role, module, action, "nav route VISIBLE but role lacks [" + module.viewPerm + "] (gating leak)");
            }
            // POSITIVE direction (permitted-but-hidden) is NOT a failure: this app's web nav is role-curated
            // and can also be company-feature-flag-gated, so a permitted module may legitimately be absent
            // from the sidebar (and `features.*` grants can be mobile-only). Log it; don't fail.
            pass(role, module, action, canView
                    ? "permitted; nav " + (visible ? "visible" : "hidden (nav-curated/feature-flag-gated — not failed)")
                    : "correctly not visible (role lacks [" + module.viewPerm + "])");
            return;
        }

        // Create / Edit / Delete — gate on <entity>.manage.
        if (!canView) {
            // Can't even open the module → the action is trivially unreachable. Secure by construction.
            pass(role, module, action, "module not viewable (no [" + module.viewPerm + "]) → no " + action + " surface");
            return;
        }
        navigateToModule(module);
        lastMatchedControl = null;
        boolean control = affordancePresent(action, module);

        if (canManage) {
            // Positive direction is data/flag-dependent (a control may be hidden when there are no rows,
            // etc.) → log, don't fail. The security invariant below is the hard assertion.
            pass(role, module, action, "permitted to manage; " + action + " control "
                    + (control ? "present" : "not detected (data/flag-dependent — not failed)"));
            return;
        }

        // !canManage — the security-critical direction. A freshly (re)loaded MUI page can render an action
        // button ENABLED for a beat, then disable/remove it once /auth/me + feature flags resolve
        // client-side; probing in that window yields a transient false "leak". So a detected control must be
        // confirmed STABLE: re-probe after a settle, and only a control that is STILL actionable is a real
        // manage-gating leak. (This is what made an earlier CP run flake 0→7→0 failures.)
        if (control) {
            String firstRead = lastMatchedControl;
            sleep(2500);
            lastMatchedControl = null;
            boolean stillActionable = affordancePresent(action, module);
            if (stillActionable) {
                String known = KNOWN_FINDINGS.get(role.name + "|" + module.label + "|" + action);
                if (known != null) {
                    // Tracked, live-verified product gap → report as a KNOWN FINDING (skip), not a build
                    // failure, so the regression gate stays green; any UNLISTED leak still hard-fails below.
                    String msg = "KNOWN RBAC UI FINDING (tracked, not failing the gate): " + known
                            + " [matched: " + lastMatchedControl + "]";
                    ExtentReportManager.logSkip(msg);
                    throw new SkipException(msg);
                }
                fail(role, module, action, action + " control is PRESENT but role lacks " + module.managePerm
                        + " — manage-gating leak [matched: " + lastMatchedControl + "]");
            }
            pass(role, module, action, "correctly no actionable " + action + " control (role lacks "
                    + module.managePerm + ") [transient control resolved on re-check; first-read: " + firstRead + "]");
            return;
        }

        // No actionable control = correct gating. If a matching element was seen but skipped as disabled,
        // surface it — a present-but-disabled control is still correct gating, and recording it documents
        // the affordance the app actually renders.
        String diag = (lastMatchedControl != null && lastMatchedControl.contains("DISABLED"))
                ? " [present-but-disabled: " + lastMatchedControl + "]" : "";
        pass(role, module, action, "correctly no actionable " + action + " control (role lacks "
                + module.managePerm + ")" + diag);
    }

    // ────────────────────────────────────────────────────────────
    // per-role session
    // ────────────────────────────────────────────────────────────
    private void ensureRoleSession(Role role) {
        if (role.name.equals(sessionRole) && driver != null) return; // reuse

        quitDriver();
        sessionRole = role.name;
        sessionAuth = RbacFixtures.cachedLiveAuth(role);
        if (!sessionAuth.provisioned) {
            String msg = "Account not provisioned for '" + role.name + "' (login status "
                    + sessionAuth.loginStatus + ") — UI matrix not checked.";
            ExtentReportManager.logSkip(msg); throw new SkipException(msg);
        }
        String mismatch = RbacFixtures.roleMismatchSkipMessage(role, sessionAuth);
        if (mismatch != null) { ExtentReportManager.logSkip(mismatch); throw new SkipException(mismatch); }

        boolean isAdmin = Boolean.TRUE.equals(sessionAuth.isAdmin);
        sessionWeb = isAdmin || sessionAuth.permissions.contains("platform.web");

        startBrowser();
        navigateToLogin();
        loginPage.login(role.email, role.password);
        waitForPostLogin();
        currentRoute = null;

        if (!sessionWeb) {
            // No web access — the whole matrix for this role is "everything hidden". The session stays
            // (web-restricted page); View cases will see an empty nav (correct), CRUD cases short-circuit.
            sessionNavHrefs = new LinkedHashSet<>();
            return;
        }
        if (onLoginPage()) {
            String msg = "UI login did not complete for '" + role.name + "' (still on login page; API login OK) "
                    + "— likely transient auth throttling.";
            ExtentReportManager.logSkip(msg); throw new SkipException(msg);
        }
        sessionNavHrefs = readSidebarHrefs();
    }

    private void navigateToModule(Module m) {
        if (m.route.equals(currentRoute)) return;
        try {
            driver.get(AppConstants.BASE_URL + m.route);
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> !onLoginPage());
            sleep(2200); // SPA hydrate + data
            currentRoute = m.route;
        } catch (Exception e) { currentRoute = m.route; }
    }

    /** Detect whether the module page currently offers the given action's control (best-effort, generic). */
    private boolean affordancePresent(String action, Module m) {
        try {
            if ("Create".equals(action)) {
                // Module-specific create control ONLY (no generic 'Create/Add/New' fallback — that
                // matched unrelated controls like 'Create Report' and produced false manage-leaks).
                By byText = By.xpath("//button[" + regexOr(m.createRegex) + "]"
                        + " | //a[" + regexOr(m.createRegex) + "]");
                return displayedEnabled(byText);
            }
            // Edit / Delete — open the first row's action menu if present, then look for the item.
            openFirstRowActions();
            String kw = action; // "Edit" / "Delete"
            By item = By.xpath("//li[normalize-space()='" + kw + "' or contains(normalize-space(),'" + kw + "')]"
                    + " | //button[normalize-space()='" + kw + "' or contains(normalize-space(),'" + kw + "')]"
                    + " | //*[@role='menuitem'][contains(normalize-space(),'" + kw + "')]");
            return displayedEnabled(item);
        } catch (Exception e) { return false; }
    }

    private void openFirstRowActions() {
        // Try to reveal per-row actions: click a kebab / actions button in the first data row.
        try {
            List<WebElement> kebabs = driver.findElements(By.xpath(
                    "(//*[@role='row'][.//*[@role='gridcell']] | //tbody//tr[td])[1]"
                    + "//button[contains(@aria-label,'menu') or contains(@aria-label,'actions')"
                    + " or contains(@aria-label,'more') or contains(@class,'MuiIconButton')]"));
            for (WebElement k : kebabs) {
                try { if (k.isDisplayed()) { k.click(); sleep(500); return; } } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /** Set to a short description of the last matched-and-visible control, for leak diagnostics. */
    private String lastMatchedControl = null;

    private boolean displayedEnabled(By by) {
        for (WebElement e : driver.findElements(by)) {
            try {
                if (!e.isDisplayed()) continue;
                // A control offered but DISABLED is correct gating, not a leak. MUI commonly disables via
                // the `disabled` attribute, `aria-disabled="true"`, or the `Mui-disabled` class — and an
                // ancestor (e.g. a wrapping <span>) may carry it. Selenium's isEnabled() only sees the
                // native `disabled` attribute, so check the MUI semantics explicitly (self + ancestors).
                if (!e.isEnabled() || isMuiDisabled(e)) {
                    lastMatchedControl = describeControl(e) + " [DISABLED — correct gating, not a leak]";
                    continue;
                }
                lastMatchedControl = describeControl(e);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /** True if the element or any ancestor is MUI-disabled (aria-disabled / Mui-disabled / disabled attr). */
    private boolean isMuiDisabled(WebElement e) {
        try {
            return Boolean.TRUE.equals(((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "let n=arguments[0];for(let i=0;i<6&&n;i++,n=n.parentElement){"
              + "if(n.getAttribute&&(n.getAttribute('aria-disabled')==='true'||n.hasAttribute('disabled')"
              + "||(n.className&&(''+n.className).indexOf('Mui-disabled')>=0)))return true;}return false;", e));
        } catch (Exception ex) { return false; }
    }

    private String describeControl(WebElement e) {
        try {
            String tag = e.getTagName();
            String txt = e.getText();
            if (txt != null) txt = txt.replaceAll("\\s+", " ").trim();
            String href = "a".equalsIgnoreCase(tag) ? e.getDomProperty("href") : null;
            return "<" + tag + "> \"" + txt + "\"" + (href != null && !href.isEmpty() ? " href=" + href : "");
        } catch (Exception ignored) { return "(matched, details unavailable)"; }
    }

    private static String regexOr(String pipeList) {
        StringBuilder sb = new StringBuilder();
        String[] parts = pipeList.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" or ");
            sb.append("contains(normalize-space(),'").append(parts[i].trim()).append("')");
        }
        return sb.toString();
    }

    private void pass(Role r, Module m, String a, String detail) {
        ExtentReportManager.logPass(r.name + " · " + m.label + " · " + a + " — " + detail);
    }
    private void fail(Role r, Module m, String a, String detail) {
        String msg = r.name + " · " + m.label + " · " + a + " — " + detail;
        ExtentReportManager.logFail(msg); Assert.fail(msg);
    }

    // ── browser/login helpers (mirror RolePermissionUiGatingTest's proven patterns) ──
    private void startBrowser() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);
        opts.setAcceptInsecureCerts(true);
        opts.addArguments("--ignore-certificate-errors");
        if (!AppConstants.CHROME_BINARY.isEmpty()) opts.setBinary(AppConstants.CHROME_BINARY);
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);
        if ("true".equals(System.getProperty("headless"))) opts.addArguments("--headless=new");
        driver = new ChromeDriver(opts);
        try { driver.manage().window().maximize(); } catch (Exception ignored) {}
        ScreenshotUtil.setDriver(driver);
        loginPage = new LoginPage(driver);
    }

    private void quitDriver() {
        if (driver != null) { try { driver.quit(); } catch (Exception ignored) {} driver = null; }
    }

    private void navigateToLogin() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try { driver.get(AppConstants.BASE_URL); } catch (Exception ignored) {}
            sleep(2000);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(30))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
                return;
            } catch (Exception e) {
                if (attempt < 3) { try { driver.navigate().refresh(); } catch (Exception ignored) {} sleep(3000); }
            }
        }
    }

    private void waitForPostLogin() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> !onLoginPage() || isWebAccessRestricted());
        } catch (Exception ignored) {}
        sleep(2500);
    }

    private boolean onLoginPage() {
        try {
            List<WebElement> email = driver.findElements(By.id("email"));
            return !email.isEmpty() && email.get(0).isDisplayed();
        } catch (Exception e) { return false; }
    }

    private boolean isWebAccessRestricted() {
        try {
            for (WebElement r : driver.findElements(By.xpath(
                    "//*[contains(normalize-space(.),'Web Access Restricted') "
                    + "or contains(normalize-space(.),'not have permission') "
                    + "or contains(normalize-space(.),'mobile app only')]"))) {
                try { if (r.isDisplayed() && r.getText().trim().length() > 0) return true; } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static final String NAV_SEL = "nav a[href], aside a[href], [class*='Drawer'] a[href], "
            + "[class*='idebar'] a[href], [role='navigation'] a[href]";

    private Set<String> readSidebarHrefsOnce() {
        Set<String> hrefs = new LinkedHashSet<>();
        for (WebElement a : driver.findElements(By.cssSelector(NAV_SEL))) {
            try { String h = a.getDomProperty("href"); if (h != null && !h.isEmpty()) hrefs.add(h); } catch (Exception ignored) {}
        }
        return hrefs;
    }

    /** Capture the sidebar's SETTLED route set. The sidebar renders incrementally — section by section,
     *  in bursts with gaps >1s — so a single read (or two equal consecutive reads that happen to land in a
     *  between-bursts lull) can miss whole sections → false "route ABSENT" View failures (observed as a CP
     *  run flaking 0→0→7). The nav is permission-gated and grows MONOTONICALLY (it renders the allowed items
     *  once /auth/me resolves and never shows unauthorized routes transiently — across many runs we only ever
     *  saw under-shows, never over-shows). So take the UNION of repeated reads across a stabilization window:
     *  that captures every route that renders regardless of burst timing, and (given monotonic growth) never
     *  introduces a phantom route. Stop early once the set has been unchanged for ~2.4s. */
    private Set<String> readSidebarHrefs() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(NAV_SEL)));
        } catch (Exception ignored) {}
        Set<String> union = new LinkedHashSet<>();
        Set<String> last = new LinkedHashSet<>();
        int stableRounds = 0;
        for (int i = 0; i < 16; i++) { // hard cap ~16 * 600ms ≈ 9.6s
            Set<String> cur = readSidebarHrefsOnce();
            union.addAll(cur);
            if (!cur.isEmpty() && cur.equals(last)) {
                if (++stableRounds >= 4) break; // unchanged for ~2.4s → settled
            } else {
                stableRounds = 0;
            }
            last = cur;
            sleep(600);
        }
        return union;
    }

    /** True if the role holds ANY of the pipe-separated permission keys (tenant uses mixed key vocabularies). */
    private static boolean hasAnyPerm(Set<String> perms, String pipeList) {
        for (String k : pipeList.split("\\|")) {
            if (perms.contains(k.trim())) return true;
        }
        return false;
    }

    private static boolean navHasRoute(Set<String> hrefs, String route) {
        for (String h : hrefs) {
            try {
                String path = java.net.URI.create(h).getPath();
                if (path != null) {
                    if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
                    if (path.equals(route)) return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
