package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.verify.NetworkConditions;
import com.egalvanic.qa.utils.verify.PerfVerifier;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Phase 4 cross-cutting quality gates — applies the NON-functional testing types
 * (Accessibility, Performance, Boundary/Data-driven input, Resilience/offline)
 * across every major module in ONE data-driven class.
 *
 * Bug classes this surfaces that functional scripts miss:
 *   - WCAG violations (contrast, labels, ARIA, alt-text)         [type H]
 *   - Slow / never-settling page loads                            [type I client-side]
 *   - Crashes/hangs on boundary + malicious input                 [type B + J]
 *   - Broken behavior when the network drops mid-flow             [type L]
 *   - Broken images / blank / clipped layout on any page          [type K]
 *   - Unhealthy or inaccessible DETAIL screens (drill-in)         [type H/K]
 *   - Horizontal-overflow responsive breakage at phone widths     [type K]
 *
 * Coverage: all 50 navigable routes (NAV_ROUTES + standalone pages + Settings
 * deep sub-views), every grid module's detail page, and a phone-viewport pass.
 *
 * Extends BaseTest → shares login + the auto health gate. Read-only (no data
 * mutation) so it's safe to run anywhere.
 */
public class Phase4QualityGatesTestNG extends BaseTest {

    private static final String MODULE = "Quality Gates";

    @BeforeClass
    @Override
    public void classSetup() {
        super.classSetup();   // login + site select
    }

    // EVERY navigable page in the app: {label, path, requiredCss-that-must-render, load-budget-ms}.
    // Pages a given role can't reach are SKIPPED (not failed) by the render-guard below,
    // so this single provider safely covers the whole app regardless of permission set.
    //
    // REBUILT 2026-08-24 against the live V1.36 app — every route below was walked with a real
    // browser rather than copied from the frontend's route table. Three classes of rot were
    // removed:
    //
    //  1. Two deep-link BLOCKS that had quietly collapsed into one screen each. All 17
    //     "/admin?view=<key>" rows now redirect to /users with the query dropped, and all 9
    //     "/reporting?view=<key>" rows render the same "Reports — Coming Soon" stub. They were
    //     reporting 26 green "distinct screens" while covering two. The Settings sub-views are
    //     replaced below by the real top-level routes they became; the Reporting ones by
    //     /reporting/builder, which already had a row.
    //  2. Dead routes that render an empty shell (no 404, so the render-guard SKIPPED them
    //     forever and the zero coverage was invisible): /equipment-library, /release-updates,
    //     /jobs. /jobs-v2 dropped as a duplicate — it redirects to /emps.
    //  3. Renames: /accounts -> /customers, /admin -> /admin-dashboard.
    //
    // Kept deliberately, having been verified alive despite NOT appearing in the sidebar:
    // /planning, /maintenance, /notes, /agent, /analyzer, /reporting, /goals. Absence from the
    // nav is not evidence a route is dead, so each was checked individually.
    // SLDs and Connections are back in the primary nav and are no longer omitted.
    @DataProvider(name = "routes")
    public Object[][] routes() {
        return new Object[][]{
            // ── Dashboards ──
            {"Dashboard",        "/dashboard",       "main",                              9000L},
            {"Sales Overview",   "/sales-overview",  "main",                             12000L},
            {"Ops Dashboard",    "/ops-dashboard",   "main",                             12000L},
            // ── Site Data ──
            {"Condition Assessment", "/pm-readiness", "main",                            14000L},
            {"Assets",           "/assets",          ".MuiDataGrid-root, [role='grid']", 12000L},
            {"Connections",      "/connections",     "main",                             12000L},
            {"Locations",        "/locations",       "main",                             10000L},
            {"Issues",           "/issues",          ".MuiDataGrid-root, [role='grid']", 12000L},
            {"Tasks",            "/tasks",           "main",                             10000L},
            {"Attachments",      "/attachments",     "main",                             12000L},
            // ── Operations ──
            {"EMPs",             "/emps",            "main",                             12000L},
            {"Planned Work",     "/planned-work",    "main",                             12000L},
            {"Scheduling",       "/scheduling",      "main",                             12000L},
            {"Work Orders",      "/sessions",        ".MuiDataGrid-root, [role='grid']", 12000L},
            // ── Engineering ──
            {"SLDs",             "/slds",            "main",                             14000L},
            {"Arc Flash Readiness", "/arc-flash",    "main",                             14000L},
            {"Panel Schedules",  "/panel-schedules", "main",                             14000L},
            {"Equipment Designations", "/equipment-designations", "main",                14000L},
            // ── Sales ──
            {"Site Walks",       "/site-walks",      "main",                             12000L},
            {"Quotes",           "/opportunities",   "main",                             10000L},
            {"Customers",        "/customers",       ".MuiDataGrid-root, [role='grid']", 12000L},
            // ── Builder ──
            {"Reports",          "/reporting/builder","main",                            12000L},
            {"Services",         "/services",        "main",                             12000L},
            {"Forms",            "/eg-forms",        "main",                             12000L},
            // ── Admin / Setup (these are the real routes the old /admin?view= links became) ──
            {"Setup",            "/admin-dashboard", "main",                             12000L},
            {"Labor",            "/labor",           "main",                             12000L},
            {"Materials",        "/materials",       "main",                             12000L},
            {"Users",            "/users",           "main",                             12000L},
            {"Offices",          "/offices",         "main",                             12000L},
            {"PM Plans",         "/pm-plans",        "main",                             14000L},
            {"Test Equipment",   "/test-equipment",  "main",                             12000L},
            {"Classes",          "/classes",         "main",                             12000L},
            {"Audit Log",        "/admin/audit-log", "main",                             12000L},
            {"Legacy Procedures","/legacy-procedures","main",                            12000L},
            {"Legacy Forms",     "/legacy-forms",    "main",                             12000L},
            // ── Alive but not in the sidebar (verified individually 2026-08-24) ──
            {"Help",             "/z-university",    "main",                             14000L},
            {"Planning",         "/planning",        ".MuiDataGrid-root, [role='grid']", 10000L},
            {"Goals",            "/goals",           "main",                             10000L},
            {"Reporting",        "/reporting",       "main",                             12000L},
            {"Reporting Legacy", "/reporting/legacy","main",                             12000L},
            {"Maintenance",      "/maintenance",     "main",                             14000L},
            {"Notes",            "/notes",           "main",                             10000L},
            {"AI Agent",         "/agent",           "main",                             14000L},
            {"Analyzer",         "/analyzer",        "main",                             14000L},
        };
    }

    @Test(dataProvider = "routes",
          description = "A11y: no critical/serious WCAG violations per module")
    public void testRouteAccessibility(String label, String path, String requiredCss, long budget) {
        ExtentReportManager.createTest(MODULE, "Accessibility (WCAG)", "A11y_" + label);
        navigate(path);
        skipIfNotRendered(label, path);
        logStep("Running axe-core WCAG 2 A/AA scan on " + label);
        verifyAccessibility(label + " (" + path + ")");   // hard-fails on critical/serious
        ExtentReportManager.logPass(label + " has no critical/serious WCAG violations");
    }

    @Test(dataProvider = "routes",
          description = "Perf: client-side load within per-module budget")
    public void testRoutePerformance(String label, String path, String requiredCss, long budget) {
        ExtentReportManager.createTest(MODULE, "Performance (client-side)", "Perf_" + label);
        navigate(path);
        skipIfNotRendered(label, path);
        PerfVerifier.PerfReport r = PerfVerifier.capture(driver, label);
        logStep(r.toString());
        verifyPerformance(label, budget);
        ExtentReportManager.logPass(label + " loaded within " + budget + "ms budget");
    }

    // Boundary / equivalence / malicious inputs — fed into the Planning search box
    // (non-destructive). Verifies the app NEVER hangs/crashes/blanks on bad input.
    @DataProvider(name = "boundaryInputs")
    public Object[][] boundaryInputs() {
        return new Object[][]{
            {"empty", ""},
            {"single-char", "a"},
            {"max-255", repeat("x", 255)},
            {"over-255", repeat("y", 256)},
            {"huge-5k", repeat("z", 5000)},
            {"unicode", "测试🚀Ωüc-ñ"},
            {"sql-injection", "' OR 1=1 --"},
            {"xss", "<script>window.__qg=1</script>"},
            {"whitespace", "     "},
            {"special", "!@#$%^&*()_+{}|:\"<>?~`"},
        };
    }

    // KNOWN REAL BUG (2026-06-03, see ready-bug/2026-06-03-planning-search-crash-qe.md):
    // Firing any input event on the Planning search box throws an uncaught
    // "TypeError: Qe is not a function" (index-*.js). Verified live across two runs —
    // it reproduces on essentially every input, INCLUDING empty, so the crash is in
    // the search onChange/input handler itself, not a result-render edge case.
    // These cases stay RED intentionally — the gate is correctly catching a live
    // front-end crash, not flaking. Do NOT whitelist "Qe is not a function".
    @Test(dataProvider = "boundaryInputs",
          description = "Boundary/negative: bad search input must not crash/hang/XSS")
    public void testSearchInputBoundary(String label, String term) {
        ExtentReportManager.createTest(MODULE, "Input Boundary/Negative", "Boundary_" + label);
        navigate("/planning");
        // Scope error detection to the INPUT action: clear baseline page errors that
        // accrued during navigation so we attribute only NEW errors to this input.
        com.egalvanic.qa.utils.verify.BrowserErrorCapture.clear(driver);
        logStep("Typing boundary input [" + label + "] into Planning search");
        try {
            java.util.List<org.openqa.selenium.WebElement> boxes = driver.findElements(
                    By.xpath("//input[contains(@placeholder,'Search Work Order Planning')]"));
            org.openqa.selenium.WebElement box = boxes.stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
            if (box != null) {
                ((JavascriptExecutor) driver).executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],arguments[1]);"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", box, term);
                pause(2000);
            }
        } catch (Exception e) {
            logStep("Input step note: " + e.getMessage());
        }
        // XSS must not have executed
        Object xss = ((JavascriptExecutor) driver).executeScript("return window.__qg || null;");
        if (xss != null) {
            throw new AssertionError("XSS executed for input [" + label + "] — window.__qg was set");
        }
        // Page must remain healthy (no hang, no JS crash, no blank, no failed XHR)
        verifyPageHealth("Planning search boundary [" + label + "]");
        ExtentReportManager.logPass("Search handled [" + label + "] safely (no crash/hang/XSS)");
    }

    @Test(description = "Resilience: going offline mid-session must not hang/crash the page")
    public void testOfflineResilience() {
        ExtentReportManager.createTest(MODULE, "Resilience (offline)", "Offline_Planning");
        navigate("/planning");
        logStep("Dropping network to OFFLINE, then reloading-ish interaction");
        NetworkConditions.goOffline(driver);
        try {
            // Trigger a data-dependent action while offline (search), then check health
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.dispatchEvent(new Event('offline'));");
            pause(2000);
            // Resilience bar: the page must stay RESPONSIVE (not hung) while offline.
            // A graceful empty-state ("select an SLD", "you are offline") is acceptable
            // degradation — so we assert responsiveness only, not error-banner absence.
            com.egalvanic.qa.utils.verify.HangDetector.assertResponsive(driver, "Planning offline", 20);
            logStepWithScreenshot("Offline state");
        } finally {
            NetworkConditions.goOnline(driver);
            pause(1500);
        }
        ExtentReportManager.logPass("Page remained responsive + non-blank while offline");
    }

    // ────────────────────────────────────────────────────────────────────
    // Broken-asset + blank/clipped-page sweep across EVERY route. Cheap
    // (no axe) so it runs the full route set: catches broken <img> (404 /
    // naturalWidth 0), blank pages, error banners, and clipped layout that
    // a functional test on a single happy-path element would never notice.
    // ────────────────────────────────────────────────────────────────────
    @Test(dataProvider = "routes",
          description = "Assets+state: no broken images / blank / clipped layout per page")
    public void testRouteAssetsAndState(String label, String path, String requiredCss, long budget) {
        ExtentReportManager.createTest(MODULE, "Broken Assets + UI State", "Assets_" + label);
        navigate(path);
        skipIfNotRendered(label, path);
        logStep("Checking images + UI state on " + label);
        java.util.List<com.egalvanic.qa.utils.verify.AssetLoadVerifier.BrokenAsset> broken =
                com.egalvanic.qa.utils.verify.AssetLoadVerifier.findBrokenImages(driver);
        // UI state first (blank/error/clipped) — hard fail.
        com.egalvanic.qa.utils.verify.UIStateValidator.assertHealthy(driver, label + " (" + path + ")");
        // Then broken images — hard fail with the offending URLs.
        com.egalvanic.qa.utils.verify.AssetLoadVerifier.assertAllImagesLoaded(driver, label + " (" + path + ")");
        ExtentReportManager.logPass(label + " — all images loaded, page not blank/clipped"
                + (broken.isEmpty() ? "" : " (note: " + broken.size() + " transient)"));
    }

    // ────────────────────────────────────────────────────────────────────
    // Detail-page coverage: drill into the FIRST row of each grid module and
    // health-check the detail screen. Detail/editor pages are the biggest
    // untested surface — functional suites mostly exercise the list grids.
    // ────────────────────────────────────────────────────────────────────
    @DataProvider(name = "gridModules")
    public Object[][] gridModules() {
        return new Object[][]{
            {"Asset Detail",       "/assets"},
            {"Work Order Detail",  "/sessions"},
            {"Issue Detail",       "/issues"},
            // The account LIST moved to /customers; the DETAIL it drills into is still /accounts/{id}.
            {"Account Detail",     "/customers"},
            {"Quote Detail",       "/opportunities"},
            {"Task Detail",        "/tasks"},
            // "Location Detail" removed 2026-08-24: /locations is a master-detail TREE with no
            // grid rows, so the row-drill never found anything and the health check silently
            // skipped every run while looking covered.
            {"Panel Editor",       "/panel-schedules"},   // -> PanelEditor / PanelView
            {"EMP Detail",         "/emps"},              // -> CommittedQuotes / QuoteDetail
        };
    }

    @Test(dataProvider = "gridModules",
          description = "Detail page: open first row, detail screen must be healthy + accessible")
    public void testDetailPageHealth(String label, String listPath) {
        ExtentReportManager.createTest(MODULE, "Detail Page Health", "Detail_" + label);
        navigate(listPath);
        skipIfNotRendered(label, listPath);
        String urlBefore = driver.getCurrentUrl();
        // Find the first real data row (DataGrid row, ARIA row with an id, or table row).
        java.util.List<org.openqa.selenium.WebElement> rows = driver.findElements(By.cssSelector(
                ".MuiDataGrid-row, [role='row'][data-id], table tbody tr"));
        org.openqa.selenium.WebElement row = rows.stream()
                .filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
        if (row == null) {
            logStep("No data rows on " + label + " — nothing to drill into; skipping");
            throw new org.testng.SkipException("No rows to open on " + listPath + " (empty grid)");
        }
        logStep("Opening first row of " + label);
        try {
            // Click a cell with content (more reliable than the row container) via JS.
            org.openqa.selenium.WebElement target = row.findElements(
                    By.cssSelector(".MuiDataGrid-cell, td, a, button")).stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(row);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);
        } catch (Exception e) {
            logStep("Row click note: " + e.getMessage());
        }
        pause(2500);
        dismissBackdrops();
        // Detail opened if the URL changed (e.g. /assets/<id>) OR a dialog/drawer appeared.
        boolean urlChanged = !driver.getCurrentUrl().equals(urlBefore);
        boolean overlay = !driver.findElements(By.cssSelector(
                "[role='dialog'], .MuiDialog-root, .MuiDrawer-root .MuiBox-root")).isEmpty();
        if (!urlChanged && !overlay) {
            logStep(label + ": row click did not open a detail view — skipping (not a failure)");
            throw new org.testng.SkipException("Row click on " + listPath + " opened no detail view");
        }
        logStepWithScreenshot(label + " detail opened");
        // The detail screen must be healthy (no JS crash / failed XHR / blank) and accessible.
        verifyPageHealth(label + " detail");
        verifyAccessibility(label + " detail");
        // DEEPER: walk the detail page's own tab strip (Quote detail has 7 tabs; Session/
        // Asset/Job detail are tabbed too). Each tab is a distinct screen — health + a11y it.
        int tabs = walkDetailTabs(label);
        ExtentReportManager.logPass(label + " detail screen healthy + accessible"
                + (tabs > 0 ? " (+" + tabs + " detail tab(s) checked)" : ""));
    }

    /** Click through the detail page's tab strip, health + a11y checking each tab. */
    private int walkDetailTabs(String label) {
        java.util.List<org.openqa.selenium.WebElement> tabs = driver.findElements(
                By.cssSelector("[role='tab'], .MuiTab-root"));
        int checked = 0;
        for (int i = 0; i < tabs.size() && i < 8; i++) {
            try {
                org.openqa.selenium.WebElement tab = tabs.get(i);
                if (!tab.isDisplayed()) continue;
                String tabName = tab.getText().trim();
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tab);
                pause(1200);
                verifyPageHealth(label + " detail tab '" + (tabName.isEmpty() ? ("#" + (i + 1)) : tabName) + "'");
                checked++;
            } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                break; // tab strip re-rendered; stop walking
            } catch (Exception e) {
                logStep(label + " detail tab note: " + e.getMessage());
            }
        }
        if (checked > 0) logStep(label + ": walked " + checked + " detail tab(s)");
        return checked;
    }

    // ────────────────────────────────────────────────────────────────────
    // Mobile-responsive layout: at a phone viewport the page must not
    // overflow horizontally (a classic responsive bug) nor go blank/clipped.
    // Representative key pages (not all 50) to keep runtime sane — the set is
    // logged so coverage is explicit, not silently truncated.
    // ────────────────────────────────────────────────────────────────────
    @DataProvider(name = "keyPages")
    public Object[][] keyPages() {
        return new Object[][]{
            {"Dashboard", "/dashboard"}, {"Assets", "/assets"}, {"Work Orders", "/sessions"},
            {"Issues", "/issues"}, {"Planning", "/planning"}, {"Locations", "/locations"},
            {"Tasks", "/tasks"}, {"Customers", "/customers"}, {"Scheduling", "/scheduling"},
            {"Reporting", "/reporting"}, {"Setup", "/admin-dashboard"}, {"Users", "/users"},
        };
    }

    @Test(dataProvider = "keyPages",
          description = "Responsive: no horizontal overflow / blank at 390px phone viewport")
    public void testMobileResponsive(String label, String path) {
        ExtentReportManager.createTest(MODULE, "Responsive (mobile)", "Mobile_" + label);
        org.openqa.selenium.Dimension original = driver.manage().window().getSize();
        try {
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(390, 844)); // iPhone-ish
            navigate(path);
            skipIfNotRendered(label, path);
            logStep("Checking 390px layout for " + label);
            // Horizontal overflow: document wider than the viewport by a real margin.
            Long overflow = (Long) ((JavascriptExecutor) driver).executeScript(
                    "return Math.max(0, (document.documentElement.scrollWidth||0) "
                    + "- (document.documentElement.clientWidth||0));");
            com.egalvanic.qa.utils.verify.UIStateValidator.assertHealthy(driver, label + " @390px");
            if (overflow != null && overflow > 24) {
                throw new AssertionError("[" + label + "] horizontal overflow at 390px viewport: "
                        + overflow + "px wider than the screen (responsive bug)");
            }
            ExtentReportManager.logPass(label + " has no horizontal overflow at 390px");
        } finally {
            driver.manage().window().setSize(original);
        }
    }

    // ── helpers ──
    private void navigate(String path) {
        driver.get(AppConstants.BASE_URL + path);
        pause(1500);
        dismissBackdrops();
        // Route-agnostic "app shell ready" wait: the persistent MUI chrome (AppBar/Drawer)
        // plus some real body text, and we're past the "Loading" splash. Works for EVERY
        // page (the old wait keyed on dashboard-only text and stalled 30s on other routes).
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
                Object ready = ((JavascriptExecutor) d).executeScript(
                        "var shell=document.querySelector('header,[role=banner],nav,.MuiDrawer-root,.MuiAppBar-root');"
                        + "var t=(document.body&&document.body.innerText)||'';"
                        + "return !!shell && t.length>40 && !/^\\s*Loading/i.test(t);");
                return Boolean.TRUE.equals(ready);
            });
        } catch (Exception ignored) {}
        pause(1200);
    }

    /**
     * Skip (not fail) a route the current role can't actually open. After navigate(),
     * if the SPA bounced us off the requested path (redirect to /dashboard, /login, etc.)
     * or rendered an explicit access-denied / not-found state, this route isn't part of
     * this role's surface — record it and skip so it doesn't masquerade as a real defect.
     */
    private void skipIfNotRendered(String label, String path) {
        String url = driver.getCurrentUrl();
        String body = "";
        try {
            Object b = ((JavascriptExecutor) driver).executeScript(
                    "return (document.body && document.body.innerText || '').slice(0,4000);");
            body = b == null ? "" : b.toString();
        } catch (Exception ignored) {}
        String lower = body.toLowerCase();
        boolean blocked = lower.contains("access denied") || lower.contains("not authorized")
                || lower.contains("don't have permission") || lower.contains("page not found")
                || lower.contains("404") || lower.contains("forbidden");
        // Normalise the leading path segment (ignore query/trailing) for the redirect check.
        String seg = path.split("\\?")[0];
        boolean offRoute = url != null && !url.contains(seg);
        if (offRoute || blocked) {
            String why = offRoute ? "redirected to " + url : "access blocked on page";
            logStep("Route [" + label + " " + path + "] not reachable for this role (" + why + ") — skipping");
            throw new org.testng.SkipException("Route " + path + " not accessible for this role (" + why + ")");
        }
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
