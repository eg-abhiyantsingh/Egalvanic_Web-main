package com.egalvanic.qa.utils;

import com.egalvanic.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The live V1.36 sidebar, encoded once.
 *
 * <p>Live-mapped against acme.qa on 2026-08-24. The sidebar is now TWO levels: a narrow
 * (76px) icon rail of six category buttons — Site Data, Operations, Engineering, Sales,
 * Builder, Admin — and a sub-panel that renders the module links for whichever category is
 * open. The panel auto-opens to the category owning the current route.
 *
 * <p><b>Why this class exists.</b> Only the OPEN category's module anchors are in the DOM at
 * all — the others are not merely hidden, they are absent. So the old
 * {@code //a[normalize-space()='Work Orders']} style of sidebar click silently matches
 * nothing and no-ops: the driver stays put, no exception is raised, and any
 * {@code isOnXPage()} guard that follows still reports the OLD page, so the direct-URL
 * fallback is skipped too. Tests then assert against stale data and log a false success.
 * Proven on 2026-08-24: from /assets, {@code a[text()='Work Orders']} is absent from the DOM,
 * a text click leaves the driver on /assets, and expanding "Operations" first makes the same
 * click land on /sessions.
 *
 * <p>Use {@link #navigateTo(WebDriver, String)} for sidebar navigation and
 * {@link #collectAllNavHrefs(WebDriver)} anywhere the FULL nav must be inspected (RBAC
 * visibility gating) — a single-category read under-reports every other category.
 */
public final class NavCatalog {

    private NavCatalog() { }

    // ================================================================
    // RAIL CATEGORIES
    // ================================================================

    public static final String SITE_DATA   = "Site Data";
    public static final String OPERATIONS  = "Operations";
    public static final String ENGINEERING = "Engineering";
    public static final String SALES       = "Sales";
    public static final String BUILDER     = "Builder";
    public static final String ADMIN       = "Admin";

    /** The six rail buttons, in on-screen order. */
    public static final List<String> CATEGORIES = Collections.unmodifiableList(
            Arrays.asList(SITE_DATA, OPERATIONS, ENGINEERING, SALES, BUILDER, ADMIN));

    // ================================================================
    // ROUTE -> OWNING CATEGORY
    // ================================================================

    /**
     * Every module route in the live nav, mapped to the rail category that must be expanded
     * before its anchor exists. Routes NOT under a rail category (the three dashboards and
     * Help) map to {@code null} — they are always present and need no expansion.
     */
    private static final Map<String, String> ROUTE_CATEGORY = new LinkedHashMap<>();
    /** Route -> the label the sidebar renders for it (V1.36 wording). */
    private static final Map<String, String> ROUTE_LABEL = new LinkedHashMap<>();

    private static void put(String route, String category, String label) {
        ROUTE_CATEGORY.put(route, category);
        ROUTE_LABEL.put(route, label);
    }

    static {
        // Always visible — outside the rail.
        put("/dashboard",       null, "Site Overview");
        put("/sales-overview",  null, "Sales Overview");
        put("/ops-dashboard",   null, "Ops Overview");
        put("/z-university",    null, "Help");

        put("/pm-readiness",    SITE_DATA, "Condition Assessment");
        put("/assets",          SITE_DATA, "Assets");
        put("/connections",     SITE_DATA, "Connections");
        put("/locations",       SITE_DATA, "Locations");
        put("/issues",          SITE_DATA, "Issues");
        put("/tasks",           SITE_DATA, "Tasks");
        put("/attachments",     SITE_DATA, "Attachments");

        put("/emps",            OPERATIONS, "EMPs");
        put("/planned-work",    OPERATIONS, "Planned Work");
        put("/scheduling",      OPERATIONS, "Scheduling");
        put("/sessions",        OPERATIONS, "Work Orders");

        put("/slds",                    ENGINEERING, "SLDs");
        put("/arc-flash",               ENGINEERING, "Arc Flash Readiness");
        put("/panel-schedules",         ENGINEERING, "Panel Schedules");
        put("/equipment-designations",  ENGINEERING, "Equipment Designations");

        put("/site-walks",      SALES, "Site Walks");
        put("/opportunities",   SALES, "Quotes");
        put("/customers",       SALES, "Customers");

        put("/reporting/builder", BUILDER, "Reports");
        put("/services",          BUILDER, "Services");
        put("/eg-forms",          BUILDER, "Forms");

        put("/admin-dashboard",    ADMIN, "Setup");
        put("/labor",              ADMIN, "Labor");
        put("/materials",          ADMIN, "Materials");
        put("/users",              ADMIN, "Users");
        put("/offices",            ADMIN, "Offices");
        put("/pm-plans",           ADMIN, "PM Plans");
        put("/test-equipment",     ADMIN, "Test Equipment");
        put("/classes",            ADMIN, "Classes");
        put("/admin/audit-log",    ADMIN, "Audit Log");
        put("/legacy-procedures",  ADMIN, "Legacy Procedures");
        put("/legacy-forms",       ADMIN, "Legacy Forms");
    }

    /** Every route the live sidebar links to. */
    public static Set<String> allRoutes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(ROUTE_CATEGORY.keySet()));
    }

    /** The rail category owning {@code route}, or null when it needs no expansion. */
    public static String categoryFor(String route) {
        return ROUTE_CATEGORY.get(normalise(route));
    }

    /** The V1.36 sidebar label for {@code route}, or null when the route is not in the nav. */
    public static String labelFor(String route) {
        return ROUTE_LABEL.get(normalise(route));
    }

    /** True when {@code route} is a real sidebar destination on V1.36. */
    public static boolean isNavRoute(String route) {
        return ROUTE_CATEGORY.containsKey(normalise(route));
    }

    // ================================================================
    // LEGACY ROUTES — renamed or redirected by the V1.36 redesign
    // ================================================================

    /**
     * Routes the redesign moved, mapped old -> live. These still HTTP-redirect, so a
     * {@code driver.get()} on the old path lands on the right page — but the sidebar anchor
     * is gone and {@code getCurrentUrl().contains(oldRoute)} is false afterwards, which is
     * what silently breaks the assertions.
     */
    private static final Map<String, String> RENAMED = new LinkedHashMap<>();
    static {
        RENAMED.put("/accounts", "/customers");       // account LIST moved; /accounts/{id} detail unchanged
        RENAMED.put("/admin",    "/admin-dashboard"); // /admin now redirects to /users; hub is /admin-dashboard
        RENAMED.put("/accounts/goals", "/goals");     // legacy path now 500s; see GOALS note below
        RENAMED.put("/jobs-v2",  "/emps");            // redirects
        RENAMED.put("/sites",    "/customers");       // redirects to /customers?tab=sites
    }

    /**
     * Goals moved to a top-level {@code /goals}. The legacy {@code /accounts/goals} still routes
     * but its API returns <b>500 "An internal error occurred."</b> every time (reproduced twice
     * on 2026-08-24, distinct trace_ids), so the page renders an error instead of the grid —
     * while {@code /goals} serves the module normally. A suite pinned to the legacy path is
     * testing a broken screen, so point Goals coverage at {@link #GOALS_ROUTE}.
     */
    public static final String GOALS_ROUTE = "/goals";

    /** The live route for a possibly-legacy path (identity when it was never renamed). */
    public static String resolve(String route) {
        String r = normalise(route);
        return RENAMED.getOrDefault(r, r);
    }

    /**
     * Routes that still resolve but render an EMPTY shell — nav chrome with an empty
     * {@code <main>}. Each was walked with a real browser on 2026-08-24; none is a 404, so a
     * test that merely checks "no error banner" passes vacuously against a blank page.
     *
     * <p>Treat this as the "do not point a test here" list. Note that being absent from the
     * sidebar does NOT imply dead: {@code /planning}, {@code /reporting}, {@code /maintenance},
     * {@code /notes}, {@code /agent}, {@code /analyzer}, {@code /reporting/legacy} and
     * {@code /goals} all render real content while having no nav entry, so they were verified
     * individually rather than inferred from the nav.
     */
    public static final Set<String> DEAD_ROUTES = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList("/admin/templates", "/admin/forms", "/admin/reporting",
                          "/admin/page-templates", "/admin/reporting-config", "/admin/version-rules",
                          "/test-equipment-library", "/equipment-library", "/equipment-insights",
                          "/settings", "/work-orders", "/schedule", "/calendar", "/templates",
                          "/jobs", "/release-updates", "/zuniversity", "/help", "/learn")));

    /** True when {@code route} renders an empty shell and must not be used as a test target. */
    public static boolean isDeadRoute(String route) {
        return DEAD_ROUTES.contains(normalise(route));
    }

    // ================================================================
    // TAB CATALOG — every role="tab" set on V1.36, verified by CLICKING each tab live
    // (2026-08-24). Keys are routes; detail pages use the {id} placeholder.
    // ================================================================

    /**
     * Route -> ordered tab labels. Numeric badges (e.g. "Engineering 8", "Contacts 2") are
     * stripped — match tabs with starts-with, not equality, because badge counts vary with data.
     *
     * <p>Not listed here because they are NOT tabs: the Quotes sidebar status filters
     * (?status=draft / pendingResponse / accepted=Closed Won / rejected=Closed Lost /
     * cancelled), the Planned Work due buckets (?bucket=overdue / due_30 / due_quarter /
     * due_year), and /reporting/builder's "Report Builder | Branding" view toggle (plain
     * buttons). Quote and EMP detail (both land on <b>/plans/{id}</b>), Site Walk detail
     * (/site-walks/{id}), and /admin-dashboard have no tabs at all. /locations is a
     * master-detail tree with no grid and no tabs.
     */
    private static final Map<String, List<String>> TABS = new LinkedHashMap<>();
    static {
        TABS.put("/pm-readiness",   Arrays.asList("Overview", "Asset Details"));
        TABS.put("/arc-flash",      Arrays.asList("Overview", "Asset Details",
                                                  "Source/Target Connections", "Connection Details"));
        TABS.put("/customers",      Arrays.asList("Accounts", "Sites"));
        TABS.put("/labor",          Arrays.asList("Rates", "Types", "Unions"));
        TABS.put("/materials",      Arrays.asList("Material Library", "Material Presets",
                                                  "Material Types", "Material Units"));
        TABS.put("/test-equipment", Arrays.asList("Test Equipment Library", "Equipment"));
        TABS.put("/classes",        Arrays.asList("Asset Classes", "Connection Classes", "Issue Classes"));
        TABS.put("/assets/{id}",    Arrays.asList("Basic Info", "Engineering", "Inspections", "Issues",
                                                  "Schedule", "Connections", "Photos", "Attachments"));
        TABS.put("/sessions/{id}",  Arrays.asList("Assets", "Tasks", "Forms", "Issues",
                                                  "IR Photos", "Attachments"));
        TABS.put("/accounts/{id}",  Arrays.asList("Details", "Internal Team", "Contacts",
                                                  "Quotes", "Sites", "Notes"));
        TABS.put("/issues/{id}",    Arrays.asList("Details", "Class Details", "Photos", "Status History"));
    }

    /** The verified tab labels for a route ({id} form for detail pages), or an empty list. */
    public static List<String> tabsFor(String route) {
        List<String> t = TABS.get(normalise(route));
        return t == null ? Collections.emptyList() : t;
    }

    /** All routes that carry a tab set, in catalog order. */
    public static Set<String> tabbedRoutes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(TABS.keySet()));
    }

    /**
     * Click the tab whose label STARTS WITH {@code label} (badge counts like "Contacts 2"
     * make exact matching brittle). Returns true when a matching visible tab was clicked.
     */
    public static boolean clickTab(WebDriver driver, String label) {
        try {
            for (WebElement t : driver.findElements(By.cssSelector("[role='tab']"))) {
                try {
                    if (!t.isDisplayed()) continue;
                    String txt = t.getText().trim().replaceAll("\\s+", " ");
                    if (txt.equals(label) || txt.startsWith(label + " ")) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", t);
                        Thread.sleep(1500);
                        return true;
                    }
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return false;
    }

    // ================================================================
    // DRIVER-SIDE NAVIGATION
    // ================================================================

    private static By railButton(String category) {
        return By.xpath("//button[.//span[normalize-space()='" + category + "']"
                + " or normalize-space()='" + category + "']");
    }

    /**
     * The logo button at the very top of the rail — the only sidebar way BACK to the
     * Dashboards panel (Site Overview / Sales Overview / Ops Overview) from a module page.
     * Its {@code <img>} carries {@code alt="Egalvanic — Dashboards"}. Verified by clicking it
     * live 2026-08-24: from /locations it lands on /dashboard with the three dashboard links
     * visible. Without this, the dashboard anchors are simply not in the DOM on module pages.
     */
    private static final By DASHBOARDS_LOGO = By.xpath(
            "//button[.//img[contains(@alt,'Dashboards')]]");

    /** Open the Dashboards panel via the rail logo. Returns false when the logo is absent. */
    public static boolean openDashboards(WebDriver driver) {
        try {
            for (WebElement b : driver.findElements(DASHBOARDS_LOGO)) {
                if (!b.isDisplayed()) continue;
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b);
                Thread.sleep(1500);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /**
     * Expand a rail category so its module anchors enter the DOM. Returns false when the
     * category button is absent — which is meaningful, not merely a miss: a role without any
     * module in that category does not get the rail button at all.
     */
    public static boolean openCategory(WebDriver driver, String category) {
        try {
            List<WebElement> buttons = driver.findElements(railButton(category));
            for (WebElement b : buttons) {
                try {
                    if (!b.isDisplayed()) continue;
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b);
                    Thread.sleep(700);
                    return true;
                } catch (Exception ignored) { /* try the next match */ }
            }
        } catch (Exception ignored) { }
        return false;
    }

    /**
     * Navigate to {@code route} through the sidebar the way a user does: expand the owning
     * category, then click the anchor. Falls back to a direct {@code driver.get()} when the
     * anchor never appears, so a nav regression degrades to "still tested" rather than
     * "silently skipped". Returns true if the driver ended up on the route.
     *
     * <p>Pass a legacy path (/accounts, /admin) and it is resolved to the live one first.
     */
    public static boolean navigateTo(WebDriver driver, String route) {
        String target = resolve(route);
        String category = categoryFor(target);
        if (category != null) {
            openCategory(driver, category);
        } else if (isNavRoute(target) && !"/z-university".equals(target)) {
            // Dashboards (and only they) live outside the rail categories. Their anchors are
            // absent on module pages, so open the Dashboards panel via the rail logo first —
            // this keeps dashboard navigation on the by-frontend path instead of silently
            // degrading to the driver.get() fallback below. Help (/z-university) is the one
            // null-category route that is always present, so it needs no opening step.
            openDashboards(driver);
        }

        try {
            List<WebElement> links = driver.findElements(
                    By.cssSelector("a[href='" + target + "'], a[href='" + target + "/']"));
            for (WebElement a : links) {
                try {
                    if (!a.isDisplayed()) continue;
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", a);
                    Thread.sleep(2000);
                    if (onRoute(driver, target)) return true;
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }

        // Sidebar click did not land — take the direct route so the test still runs.
        try {
            driver.get(AppConstants.BASE_URL + target);
            Thread.sleep(2000);
        } catch (Exception ignored) { }
        return onRoute(driver, target);
    }

    /**
     * Exact-path match against the current URL. Substring matching is wrong here: "/admin"
     * is a substring of both "/admin-dashboard" and "/admin/audit-log", and "dashboard" is a
     * substring of "/admin-dashboard", "/ops-dashboard" and "/dashboard" alike.
     */
    public static boolean onRoute(WebDriver driver, String route) {
        try {
            return pathOf(driver.getCurrentUrl()).equals(normalise(resolve(route)));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Every sidebar href reachable by this session, gathered by expanding all six rail
     * categories in turn.
     *
     * <p>Reading the sidebar without expanding returns only the current category — roughly
     * seven of the thirty-odd links — so a permission-gating check built on a single read
     * reports almost every module as "not visible" and fails whole roles for a UI reason
     * rather than a permission one.
     *
     * <p><b>Side effect:</b> opening the Dashboards panel via the rail logo NAVIGATES to
     * /dashboard (verified live), so after this call the driver may be on /dashboard rather
     * than where it started. Callers that need to stay put should re-navigate afterwards.
     */
    public static Set<String> collectAllNavHrefs(WebDriver driver) {
        Set<String> paths = new LinkedHashSet<>();
        harvestVisibleHrefs(driver, paths);
        // The three dashboard anchors live behind the rail LOGO, not a category — on a module
        // page they are absent until the Dashboards panel is opened, so a category-only sweep
        // under-reports them (observed live: /assets sweep found 33 routes, none of them
        // /dashboard //sales-overview //ops-dashboard).
        if (openDashboards(driver)) harvestVisibleHrefs(driver, paths);
        for (String category : CATEGORIES) {
            if (openCategory(driver, category)) harvestVisibleHrefs(driver, paths);
        }
        return paths;
    }

    private static void harvestVisibleHrefs(WebDriver driver, Set<String> into) {
        try {
            for (WebElement a : driver.findElements(By.cssSelector("a[href]"))) {
                try {
                    if (!a.isDisplayed()) continue;
                    String path = pathOf(a.getAttribute("href"));
                    if (!path.isEmpty()) into.add(path);
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Absolute or relative href -> normalised path ("/assets/" -> "/assets"). */
    public static String pathOf(String href) {
        if (href == null || href.isEmpty()) return "";
        String path = href;
        try {
            if (href.startsWith("http")) {
                String p = java.net.URI.create(href).getPath();
                path = (p == null) ? "" : p;
            }
        } catch (Exception ignored) {
            return "";
        }
        return normalise(path);
    }

    private static String normalise(String route) {
        if (route == null) return "";
        String r = route.trim();
        int q = r.indexOf('?');
        if (q >= 0) r = r.substring(0, q);
        int h = r.indexOf('#');
        if (h >= 0) r = r.substring(0, h);
        while (r.length() > 1 && r.endsWith("/")) r = r.substring(0, r.length() - 1);
        return r;
    }
}
