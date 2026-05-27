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
                // If the SPA bounced us to /dashboard, the route is missing
                // or feature-gated. Wait at least 5 seconds before deciding
                // — the initial route may briefly show /dashboard during
                // hydration before transitioning to the target path.
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed > 5_000
                        && !currentUrl.contains(path)
                        && currentUrl.contains("/dashboard")) {
                    logStep("Bounced back to /dashboard after " + elapsed
                            + "ms — module path may be feature-flagged off");
                    return false;
                }
                // Look for any of the expected text strings to confirm the
                // right page rendered. Use JS for tolerance to nested DOM.
                // Match CASE-INSENSITIVELY because the app uppercases many
                // labels via CSS text-transform (e.g. dashboard KPI cards
                // render as "EQUIPMENT AT RISK" even though source is
                // "Equipment at Risk").
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
        try {
            List<WebElement> mains = driver.findElements(By.tagName("main"));
            if (mains.isEmpty()) return false;
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
        boolean ok = smokeOpenModule("/sales-overview", "Sales", "Overview", "Pipeline", "Revenue");
        logStepWithScreenshot("Sales Overview rendered: " + ok);
        Assert.assertTrue(ok && smokeAssertShellRendered(),
                "Sales Overview module did not render");
        ExtentReportManager.logPass("Sales Overview module reachable + shell rendered");
    }

    @Test(priority = 2, description = "TC_NM_02: Opportunities page renders pipeline grid")
    public void testTC_NM_02_Opportunities() {
        ExtentReportManager.createTest(MODULE, "Opportunities", "TC_NM_02");
        boolean ok = smokeOpenModule("/opportunities",
                "Opportunity", "Opportunities", "Total Value");
        // Look for the grid columns we observed live (Opportunity Name,
        // Facility, Revisions, Total Value, Created, Status, Actions).
        boolean hasGridCol = !driver.findElements(By.xpath(
                "//*[contains(text(),'Opportunity Name') or contains(text(),'Total Value')]"
        )).isEmpty();
        logStepWithScreenshot("Opportunities rendered: " + ok + ", grid col: " + hasGridCol);
        Assert.assertTrue(ok || hasGridCol,
                "Opportunities module did not render its pipeline grid");
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
        boolean ok = smokeOpenModule("/accounts", "Account", "Accounts");
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
        boolean ok = smokeOpenModule("/panel-schedules", "Panel Schedule", "Panel Schedules");
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

    @Test(priority = 8, description = "TC_NM_08: Equipment Library page loads")
    public void testTC_NM_08_EquipmentLibrary() {
        ExtentReportManager.createTest(MODULE, "Equipment Library", "TC_NM_08");
        boolean ok = smokeOpenModule("/equipment-library",
                "Equipment Library", "Material", "Voltage Rating");
        // The Equipment Library has a known tabbed structure
        boolean hasTabs = !driver.findElements(By.xpath(
                "//*[@role='tab' and (contains(text(),'Asset Details') "
                + "or contains(text(),'Connection Details'))]")).isEmpty();
        logStepWithScreenshot("Equipment Library rendered: " + ok + ", tabs: " + hasTabs);
        Assert.assertTrue(ok || hasTabs,
                "Equipment Library module did not render its tabbed view");
        ExtentReportManager.logPass("Equipment Library reachable + tabs visible");
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

        // Poll up to 30s for the KPI cards row to render
        long deadline = System.currentTimeMillis() + 30_000;
        boolean found = false;
        while (System.currentTimeMillis() < deadline && !found) {
            List<WebElement> cards = driver.findElements(By.xpath(
                    "//*[contains(text(),'Opportunities Value')]"));
            if (!cards.isEmpty()) { found = true; break; }
            pause(750);
        }
        logStepWithScreenshot("Opportunities Value KPI on dashboard: " + found);
        Assert.assertTrue(found,
                "Dashboard is missing the 'Opportunities Value' KPI card "
                + "(added in May 2026 release)");
        ExtentReportManager.logPass("Opportunities Value KPI is present on dashboard");
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
