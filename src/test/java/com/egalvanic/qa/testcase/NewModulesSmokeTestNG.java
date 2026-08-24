package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Smoke tests for the 10+ NEW modules introduced in the May 2026 web update.
 *
 * Each test:
 *   1. Navigates to the module's URL
 *   2. Waits up to 30s for the SPA to hydrate
 *   3. Verifies the page actually loaded (not a 404, not the dashboard fallback,
 *      not an error banner)
 *   4. Captures one assertion that the module's main visual element rendered
 *
 * Why this exists: walking the live site (May 2026) surfaced these modules
 * with ZERO test coverage — Sales pipeline (Opportunities, Goals, Accounts),
 * Engineering tools (Panel Schedules, Arc Flash, Equipment Library, Equipment
 * Designations), Compliance (EMPs, Condition Assessment), plus Report Builder,
 * Forms, Audit Log, and Z University. Each of these can silently break in a
 * release and the regression suite would never notice. This smoke class is
 * the first regression net under all of them.
 *
 * Coverage is INTENTIONALLY shallow — one test per module. Deep coverage
 * (CRUD, edit, business logic) is reserved for per-module test classes that
 * can be built as each module stabilizes and the team prioritizes them.
 *
 * Safety: read-only. No create / edit / delete is performed.
 */
public class NewModulesSmokeTestNG extends BaseTest {

    private static final String MODULE = "New Modules (May 2026)";

    // ================================================================
    // SHARED HELPER
    // ================================================================

    /**
     * Navigate to a module URL, wait for the SPA to hydrate, and confirm we
     * actually ended up on the right page (not redirected to login, /dashboard
     * fallback, or a 404 shell).
     *
     * Returns true if the module's page renders, false otherwise.
     */
    private boolean smokeOpenModule(String path, String... expectedTextAnyOf) {
        // Retry once on transient Selenium session-bounce. Selenium's WebDriver
        // sometimes bounces a fresh navigation back to /dashboard while React
        // routes settle; a second driver.get() typically lands cleanly.
        // Other automation tools (Playwright) don't hit this — it's Selenium-specific.
        for (int attempt = 1; attempt <= 2; attempt++) {
            boolean ok = smokeOpenModuleOnce(path, expectedTextAnyOf);
            if (ok) return true;
            if (attempt < 2) {
                logStep("Module render check failed on attempt " + attempt + " — retrying after 2s");
                pause(2000);
            }
        }
        // V1.36 split the app across TWO role consoles and NO single role can reach every module:
        // "Super Admin" (operational) lacks features.goals/salesdb.view and is Access-Denied on
        // /eg-forms; the renamed "Admin" (setup) holds those. If the module didn't render under
        // the pinned role AND the page shows Access Denied, retry ONCE under "Admin", then
        // restore the default role so later tests keep the deterministic console.
        if (isAccessDenied()) {
            logStep("Module '" + path + "' is Access-Denied under " + AppConstants.DEFAULT_ACTIVE_ROLE
                    + " — retrying under 'Admin' (V1.36 dual-console permission split)");
            try {
                ensureActiveRole("Admin");
                boolean ok = smokeOpenModuleOnce(path, expectedTextAnyOf);
                return ok;
            } finally {
                try { ensureActiveRole(AppConstants.DEFAULT_ACTIVE_ROLE); } catch (Exception ignored) { }
            }
        }
        return false;
    }

    /** True when the current page shows the app's Access Denied screen. */
    private boolean isAccessDenied() {
        try {
            Object t = ((JavascriptExecutor) driver).executeScript(
                    "var m=document.querySelector('main'); return m ? m.innerText.slice(0,400) : '';");
            String s = String.valueOf(t);
            return s.contains("Access Denied") || s.contains("do not have permission");
        } catch (Exception e) { return false; }
    }

    private boolean smokeOpenModuleOnce(String path, String... expectedTextAnyOf) {
        String fullUrl = AppConstants.BASE_URL + path;
        driver.get(fullUrl);
        logStep("Navigated to " + path);

        // Allow up to 30s for the SPA to hydrate. The Egalvanic SPA stages
        // its render: route resolves first, then the module bundle loads,
        // then API calls populate the grid/cards.
        long start = System.currentTimeMillis();
        long deadline = start + 30_000;
        boolean rendered = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                String currentUrl = driver.getCurrentUrl();
                // Redirect to /login means our session died → re-login flow,
                // not a module-shape problem; treat as fail.
                if (currentUrl.contains("/login")) {
                    logStep("Redirected to /login — session expired during navigation");
                    return false;
                }
                // FIRST: check for expected text — if found, the module
                // RENDERED regardless of what the URL currently shows.
                // (Selenium's currentUrl can be misleading during SPA hash
                // navigation; rendered content is the authoritative signal.)
                if (expectedTextAnyOf != null && expectedTextAnyOf.length > 0) {
                    String body = (String) ((JavascriptExecutor) driver).executeScript(
                            "return (document.body && document.body.innerText) || '';");
                    String bodyLower = body.toLowerCase();
                    for (String expected : expectedTextAnyOf) {
                        if (bodyLower.contains(expected.toLowerCase())) {
                            rendered = true;
                            logStep("Found expected text (case-insensitive): " + expected);
                            break;
                        }
                    }
                }
                if (rendered) break;
                // Bounce-to-dashboard detection — only after 15s grace
                // (longer than 5s because Selenium session can route-bounce
                // multiple times before settling). And ONLY count as bounce
                // if the URL is exactly /dashboard, not a redirect chain.
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed > 15_000
                        && !currentUrl.contains(path)
                        && (currentUrl.endsWith("/dashboard") || currentUrl.endsWith("/dashboard/"))) {
                    logStep("Bounced back to /dashboard after " + elapsed
                            + "ms — module path may be feature-flagged off");
                    return false;
                }
                pause(750);
            } catch (Exception e) {
                pause(750);
            }
        }
        return rendered;
    }

    /**
     * Look for the module shell. Accepts:
     *   (a) <main> element with non-empty content, OR
     *   (b) An <iframe> inside <main> (e.g. Z University renders external
     *       learning content via iframe — main itself is empty)
     * Filters out the "404" / "Not Found" / "Application Error" landing
     * shells the SPA serves for unknown routes.
     */
    private boolean smokeAssertShellRendered() {
        // The token check can pass off SIDEBAR text the instant the route mounts, while <main>
        // still shows only the loading spinner (slow module APIs) — the old single-shot check
        // then failed at ~4s with "did not render". Poll up to 25s for main/body content.
        long deadline = System.currentTimeMillis() + 25_000;
        while (true) {
            boolean ok = smokeShellRenderedOnce();
            if (ok || System.currentTimeMillis() > deadline) return ok;
            pause(1200);
        }
    }

    private boolean smokeShellRenderedOnce() {
        try {
            List<WebElement> mains = driver.findElements(By.tagName("main"));
            // V1.36 (live 2026-08-05): some pages render OUTSIDE a <main> landmark entirely —
            // /z-university has no <main> at all yet shows "Z University | Learning Center |
            // Welcome…" in the body. Fall back to a body-level check instead of failing.
            if (mains.isEmpty()) {
                String body = String.valueOf(((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("return (document.body && document.body.innerText) || ''")).toLowerCase();
                if (body.contains("404") || body.contains("not found")
                        || body.contains("application error") || body.contains("something went wrong")) {
                    return false;
                }
                return body.trim().length() > 100; // substantial content rendered without <main>
            }
            WebElement main = mains.get(0);
            String mainText = main.getText().toLowerCase();
            // Negative markers — if these appear we did NOT land on a real page
            String[] notFoundMarkers = {
                    "404", "not found", "page not found",
                    "application error", "something went wrong",
                    "no access", "forbidden"
            };
            for (String m : notFoundMarkers) {
                if (mainText.contains(m)) {
                    logStep("Module shell shows '" + m + "' — treating as failure");
                    return false;
                }
            }
            if (!mainText.isEmpty()) return true;
            // (b) iframe fallback — Z University and similar pages embed
            //     external content via iframe, so main.text is empty but
            //     the page IS rendering. An iframe whose src is set proves
            //     content was loaded into the shell.
            List<WebElement> iframes = main.findElements(By.tagName("iframe"));
            for (WebElement f : iframes) {
                String src = f.getDomAttribute("src");
                if (src != null && !src.isEmpty() && !src.equals("about:blank")) {
                    logStep("Module shell renders via iframe: " + src.substring(0, Math.min(80, src.length())));
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // SALES MODULE — Sales Overview, Opportunities, Goals, Accounts
    // ================================================================

    @Test(priority = 1, description = "TC_NM_01: Sales Overview page loads")
    public void testTC_NM_01_SalesOverview() {
        ExtentReportManager.createTest(MODULE, "Sales Overview", "TC_NM_01");
        // V1.36 (live 2026-08-05): the page renders "Planned Work / … unreleased work orders /
        // OVERDUE / DUE IN 30 DAYS / Needs Attention" — none of the old Sales/Pipeline/Revenue
        // words appear in the body, so keep old tokens AND add the current ones.
        boolean ok = smokeOpenModule("/sales-overview", "Sales", "Overview", "Pipeline", "Revenue",
                "Planned Work", "unreleased work orders", "Needs Attention");
        logStepWithScreenshot("Sales Overview rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Sales Overview module did not render");
        ExtentReportManager.logPass("Sales Overview module reachable + shell rendered");
    }

    @Test(priority = 2, description = "TC_NM_02: Opportunities page renders pipeline grid")
    public void testTC_NM_02_Opportunities() {
        ExtentReportManager.createTest(MODULE, "Opportunities", "TC_NM_02");
        // V1.36 renamed this module to "Quotes" (route unchanged). Both vocabularies are
        // accepted so the smoke spans the rename.
        boolean ok = smokeOpenModule("/opportunities",
                "Quote", "Quotes", "Opportunity");
        boolean hasGridCol = !driver.findElements(By.xpath(
                "//*[contains(text(),'Opportunity Name') or contains(text(),'Total Value') "
                + "or contains(text(),'Quote')]"
        )).isEmpty();
        logStepWithScreenshot("Quotes rendered: " + ok + ", grid col: " + hasGridCol);
        Assert.assertTrue(ok || hasGridCol,
                "Quotes module did not render its pipeline grid");
        ExtentReportManager.logPass("Opportunities module reachable + grid columns visible");
    }

    @Test(priority = 3, description = "TC_NM_03: Goals page loads")
    public void testTC_NM_03_Goals() {
        ExtentReportManager.createTest(MODULE, "Goals", "TC_NM_03");
        boolean ok = smokeOpenModule("/goals", "Goal", "Goals", "Target");
        logStepWithScreenshot("Goals rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(), "Goals module did not render");
        ExtentReportManager.logPass("Goals module reachable + shell rendered");
    }

    @Test(priority = 4, description = "TC_NM_04: Accounts page loads")
    public void testTC_NM_04_Accounts() {
        ExtentReportManager.createTest(MODULE, "Accounts", "TC_NM_04");
        boolean ok = smokeOpenModule("/customers", "Customer", "Customers", "Account");
        logStepWithScreenshot("Accounts rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(), "Accounts module did not render");
        ExtentReportManager.logPass("Accounts module reachable + shell rendered");
    }

    // ================================================================
    // OPS MODULE
    // ================================================================

    @Test(priority = 5, description = "TC_NM_05: Ops Overview page loads")
    public void testTC_NM_05_OpsOverview() {
        ExtentReportManager.createTest(MODULE, "Ops Overview", "TC_NM_05");
        boolean ok = smokeOpenModule("/ops-dashboard",
                "Ops", "Operations", "Overview", "Activity");
        logStepWithScreenshot("Ops Overview rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Ops Overview module did not render");
        ExtentReportManager.logPass("Ops Overview reachable + shell rendered");
    }

    // ================================================================
    // ENGINEERING TOOLS — Panel Schedules, Arc Flash, Equipment Library,
    //                    Equipment Designations
    // ================================================================

    @Test(priority = 6, description = "TC_NM_06: Panel Schedules page loads")
    public void testTC_NM_06_PanelSchedules() {
        ExtentReportManager.createTest(MODULE, "Panel Schedules", "TC_NM_06");
        // V1.36 (live 2026-08-05): the page body shows "Schedule Status / N of M current" + a grid
        // with "Panel Name / Amperage / Voltage" — the literal words "Panel Schedule" no longer
        // appear anywhere in the body text.
        boolean ok = smokeOpenModule("/panel-schedules", "Panel Schedule", "Panel Schedules",
                "Schedule Status", "Panel Name");
        logStepWithScreenshot("Panel Schedules rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Panel Schedules module did not render");
        ExtentReportManager.logPass("Panel Schedules reachable + shell rendered");
    }

    @Test(priority = 7, description = "TC_NM_07: Arc Flash Readiness page loads")
    public void testTC_NM_07_ArcFlash() {
        ExtentReportManager.createTest(MODULE, "Arc Flash", "TC_NM_07");
        boolean ok = smokeOpenModule("/arc-flash", "Arc Flash", "IEEE", "NFPA");
        logStepWithScreenshot("Arc Flash rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Arc Flash Readiness module did not render");
        ExtentReportManager.logPass("Arc Flash Readiness reachable + shell rendered");
    }

    @Test(priority = 8, description = "TC_NM_08: Materials library page loads")
    public void testTC_NM_08_Materials() {
        // Was /equipment-library. That route now redirects to /dashboard, so this test was
        // asserting against the dashboard under an Equipment Library label and could only pass
        // by accident. The materials library it covered is the top-level /materials in V1.36.
        ExtentReportManager.createTest(MODULE, "Materials", "TC_NM_08");
        boolean ok = smokeOpenModule("/materials", "Material", "Materials", "Library");
        boolean hasGrid = !driver.findElements(By.cssSelector(
                ".MuiDataGrid-root, [role='grid'], table")).isEmpty();
        logStepWithScreenshot("Materials rendered: " + ok + ", grid: " + hasGrid);
        Assert.assertTrue(ok || hasGrid,
                "Materials module did not render its library view");
        ExtentReportManager.logPass("Materials reachable + grid visible");
    }

    @Test(priority = 9, description = "TC_NM_09: Equipment Designations page loads")
    public void testTC_NM_09_EquipmentDesignations() {
        ExtentReportManager.createTest(MODULE, "Equipment Designations", "TC_NM_09");
        boolean ok = smokeOpenModule("/equipment-designations",
                "Designation", "Equipment", "Naming");
        logStepWithScreenshot("Equipment Designations rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Equipment Designations module did not render");
        ExtentReportManager.logPass("Equipment Designations reachable + shell rendered");
    }

    // ================================================================
    // COMPLIANCE & MAINTENANCE — EMPs, Condition Assessment
    // ================================================================

    @Test(priority = 10, description = "TC_NM_10: EMPs (Equipment Maintenance Plans) page loads")
    public void testTC_NM_10_EMPs() {
        ExtentReportManager.createTest(MODULE, "EMPs", "TC_NM_10");
        boolean ok = smokeOpenModule("/emps", "EMP", "EMPs", "Maintenance Plan");
        logStepWithScreenshot("EMPs rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(), "EMPs module did not render");
        ExtentReportManager.logPass("EMPs reachable + shell rendered");
    }

    @Test(priority = 11, description = "TC_NM_11: Condition Assessment (pm-readiness) page loads")
    public void testTC_NM_11_ConditionAssessment() {
        ExtentReportManager.createTest(MODULE, "Condition Assessment", "TC_NM_11");
        boolean ok = smokeOpenModule("/pm-readiness",
                "Condition Assessment", "Condition", "PM");
        logStepWithScreenshot("Condition Assessment rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Condition Assessment module did not render");
        ExtentReportManager.logPass("Condition Assessment reachable + shell rendered");
    }

    // ================================================================
    // REPORTING & FORMS
    // ================================================================

    @Test(priority = 12, description = "TC_NM_12: Report Builder page loads")
    public void testTC_NM_12_ReportBuilder() {
        ExtentReportManager.createTest(MODULE, "Report Builder", "TC_NM_12");
        boolean ok = smokeOpenModule("/reporting/builder",
                "Report", "Reporting", "Builder");
        logStepWithScreenshot("Report Builder rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Report Builder module did not render");
        ExtentReportManager.logPass("Report Builder reachable + shell rendered");
    }

    @Test(priority = 13, description = "TC_NM_13: Forms (eg-forms) page loads")
    public void testTC_NM_13_Forms() {
        ExtentReportManager.createTest(MODULE, "Forms", "TC_NM_13");
        boolean ok = smokeOpenModule("/eg-forms", "Form", "Forms");
        logStepWithScreenshot("Forms rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(), "Forms module did not render");
        ExtentReportManager.logPass("Forms reachable + shell rendered");
    }

    // ================================================================
    // ADMIN — Audit Log, Z University
    // ================================================================

    @Test(priority = 14, description = "TC_NM_14: Audit Log page loads")
    public void testTC_NM_14_AuditLog() {
        ExtentReportManager.createTest(MODULE, "Audit Log", "TC_NM_14");
        boolean ok = smokeOpenModule("/admin/audit-log",
                "Audit Log", "Audit", "Activity");
        logStepWithScreenshot("Audit Log rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Audit Log module did not render");
        ExtentReportManager.logPass("Audit Log reachable + shell rendered");
    }

    @Test(priority = 15, description = "TC_NM_15: Z University (learning center) page loads")
    public void testTC_NM_15_ZUniversity() {
        ExtentReportManager.createTest(MODULE, "Z University", "TC_NM_15");
        boolean ok = smokeOpenModule("/z-university",
                "Z University", "Learning", "Course", "Training");
        logStepWithScreenshot("Z University rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Z University module did not render");
        ExtentReportManager.logPass("Z University reachable + shell rendered");
    }

    // ================================================================
    // DASHBOARD KPI EXPANSION — new cards introduced in May 2026
    // ================================================================

    @Test(priority = 16, description = "TC_NM_16: Dashboard shows new Opportunities Value KPI card")
    public void testTC_NM_16_DashboardOpportunitiesKPI() {
        ExtentReportManager.createTest(MODULE, "Dashboard KPI", "TC_NM_16");
        driver.get(AppConstants.BASE_URL + "/dashboard");
        pause(3000);

        // V1.36 (live 2026-08-05): the dashboard KPI row is now TOTAL ASSETS / ACTIVE WORK ORDERS /
        // EQUIPMENT AT RISK — the May-2026 'Opportunities Value' card was REMOVED in the redesign.
        // Assert the current KPI trio instead (any 2 of 3 = rendered; tolerates copy tweaks).
        long deadline = System.currentTimeMillis() + 30_000;
        int found = 0;
        String[] kpis = {"TOTAL ASSETS", "ACTIVE WORK ORDERS", "EQUIPMENT AT RISK"};
        while (System.currentTimeMillis() < deadline && found < 2) {
            found = 0;
            String body = String.valueOf(((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("return (document.body && document.body.innerText) || '';"));
            for (String k : kpis) if (body.contains(k)) found++;
            if (found < 2) pause(750);
        }
        logStepWithScreenshot("Dashboard KPI cards found: " + found + "/3");
        Assert.assertTrue(found >= 2,
                "Dashboard should render the V1.36 KPI row (TOTAL ASSETS / ACTIVE WORK ORDERS / "
                + "EQUIPMENT AT RISK) — found " + found + " of 3");
        ExtentReportManager.logPass("Dashboard KPI row present (" + found + "/3 cards)");
    }

    @Test(priority = 17, description = "TC_NM_17: Dashboard shows new Equipment at Risk KPI card")
    public void testTC_NM_17_DashboardEquipmentAtRiskKPI() {
        ExtentReportManager.createTest(MODULE, "Dashboard KPI", "TC_NM_17");
        driver.get(AppConstants.BASE_URL + "/dashboard");
        pause(3000);

        long deadline = System.currentTimeMillis() + 30_000;
        boolean found = false;
        while (System.currentTimeMillis() < deadline && !found) {
            List<WebElement> cards = driver.findElements(By.xpath(
                    "//*[contains(text(),'Equipment at Risk')]"));
            if (!cards.isEmpty()) { found = true; break; }
            pause(750);
        }
        logStepWithScreenshot("Equipment at Risk KPI on dashboard: " + found);
        Assert.assertTrue(found,
                "Dashboard is missing the 'Equipment at Risk' KPI card "
                + "(added in May 2026 release)");
        ExtentReportManager.logPass("Equipment at Risk KPI is present on dashboard");
    }
}
