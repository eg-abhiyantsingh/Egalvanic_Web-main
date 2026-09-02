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
 * <p>Live-mapped against acme.qa on 2026-08-24, <b>re-mapped 2026-09-02</b> (see the
 * "re-map" notes below — the 08-24 map had drifted in about twenty places). The sidebar is
 * TWO levels: a narrow icon rail of seven category buttons — Site Data, Operations,
 * Engineering, Sales, Builder, Maintenance Portal, Admin — and a sub-panel that renders the
 * module links for whichever category is open. The panel auto-opens to the category owning
 * the current route.
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
 * <p><b>Re-map 2026-09-02 — what moved.</b> Harvested by clicking every rail button and
 * reading the resulting {@code a.MuiListItemButton-root[href]} set, so the map below is the
 * anchors the app actually renders rather than the labels it used to:
 * <ul>
 *   <li>A seventh category, <b>Maintenance Portal</b>, exists and was entirely absent here.
 *       {@link #collectAllNavHrefs} therefore under-reported its five routes, which reads as
 *       "the role cannot see them" in any RBAC visibility check.</li>
 *   <li>The three dashboards are no longer rail-less: {@code /dashboard} is under Site Data,
 *       {@code /ops-dashboard} under Operations, {@code /sales-overview} under Sales. They
 *       need a category expansion like anything else, so {@link #openDashboards} is now
 *       vestigial.</li>
 *   <li>{@code /panel-schedules} moved Engineering &rarr; Site Data, {@code /customers}
 *       Sales &rarr; Admin, {@code /pm-plans} Admin &rarr; Builder,
 *       {@code /test-equipment} is in <i>both</i> Operations and Admin.</li>
 *   <li>Fourteen routes were missing: {@code /maintenance/program},
 *       {@code /maintenance/compliance}, {@code /maintenance/reports},
 *       {@code /short-circuit-ratings}, {@code /feeder-schedule}, {@code /ocpd-settings},
 *       {@code /transformer-schedule}, {@code /pull-through-work},
 *       {@code /guest-portal-users}, {@code /custom-devices},
 *       {@code /asset-classes}, {@code /connection-classes}, {@code /issue-classes},
 *       and the five {@code /maintenance-portal/*} routes.</li>
 *   <li>{@code /classes} split into three separate routes. {@code /emps} and
 *       {@code /equipment-designations} left the nav but still render real content — probed
 *       2026-09-02 — so they are unlinked, NOT dead. Reach them by direct URL.</li>
 *   <li>Labels changed: "SLDs" &rarr; "SLD", "Users" &rarr; "Platform Users".</li>
 * </ul>
 *
 * <p><b>Labels are no longer unique — match on href, not text.</b> The re-map found the same
 * label on several different routes: <b>Reports</b> is {@code /maintenance/reports},
 * {@code /reporting/builder} <i>and</i> {@code /maintenance-portal/reports};
 * <b>Condition Assessment</b>, <b>Maintenance Program</b>, <b>Compliance</b> and
 * <b>Test Equipment</b> are each on two routes. A locator like
 * {@code //a[normalize-space()='Reports']} is therefore ambiguous by construction — it will
 * click whichever the open category happens to render. Use {@link #navigateTo}, which keys on
 * href, and prefer {@link #routeForLabel(String, String)} over the single-argument form.
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

    public static final String SITE_DATA           = "Site Data";
    public static final String OPERATIONS          = "Operations";
    public static final String ENGINEERING         = "Engineering";
    public static final String SALES               = "Sales";
    public static final String BUILDER             = "Builder";
    public static final String MAINTENANCE_PORTAL  = "Maintenance Portal";
    public static final String ADMIN               = "Admin";

    /** The seven rail buttons, in on-screen order (Maintenance Portal added 2026-09-02). */
    public static final List<String> CATEGORIES = Collections.unmodifiableList(
            Arrays.asList(SITE_DATA, OPERATIONS, ENGINEERING, SALES, BUILDER,
                          MAINTENANCE_PORTAL, ADMIN));

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
        // Help is the one nav anchor present in every category's panel.
        put("/z-university",    null, "Help");

        // ---- Site Data (12) ----
        put("/dashboard",               SITE_DATA, "Site Overview");
        put("/pm-readiness",            SITE_DATA, "Condition Assessment");
        put("/assets",                  SITE_DATA, "Assets");
        put("/panel-schedules",         SITE_DATA, "Panel Schedules");
        put("/connections",             SITE_DATA, "Connections");
        put("/locations",               SITE_DATA, "Locations");
        put("/issues",                  SITE_DATA, "Issues");
        put("/tasks",                   SITE_DATA, "Tasks");
        put("/maintenance/program",     SITE_DATA, "Maintenance Program");
        put("/maintenance/compliance",  SITE_DATA, "Compliance");
        put("/maintenance/reports",     SITE_DATA, "Reports");
        put("/attachments",             SITE_DATA, "Attachments");

        // ---- Operations (5) ----
        put("/ops-dashboard",   OPERATIONS, "Ops Overview");
        put("/planned-work",    OPERATIONS, "Planned Work");
        put("/scheduling",      OPERATIONS, "Scheduling");
        put("/sessions",        OPERATIONS, "Work Orders");
        put("/test-equipment",  OPERATIONS, "Test Equipment"); // also rendered under Admin

        // ---- Engineering (6) ----
        put("/arc-flash",               ENGINEERING, "Arc Flash Readiness");
        put("/slds",                    ENGINEERING, "SLD");
        put("/short-circuit-ratings",   ENGINEERING, "Short-Circuit Ratings");
        put("/feeder-schedule",         ENGINEERING, "Feeder Schedule");
        put("/ocpd-settings",           ENGINEERING, "OCPD Settings");
        put("/transformer-schedule",    ENGINEERING, "Transformer Schedule");

        // ---- Sales (4) ----
        put("/sales-overview",      SALES, "Sales Overview");
        put("/site-walks",          SALES, "Site Walks");
        put("/pull-through-work",   SALES, "Pull-Through Work");
        put("/opportunities",       SALES, "Quotes");

        // ---- Builder (4) ----
        put("/reporting/builder", BUILDER, "Reports");
        put("/services",          BUILDER, "Services");
        put("/pm-plans",          BUILDER, "PM Plans");
        put("/eg-forms",          BUILDER, "Forms");

        // ---- Maintenance Portal (5) — the category the 08-24 map missed entirely ----
        put("/maintenance-portal/overview",    MAINTENANCE_PORTAL, "Site Health");
        put("/maintenance-portal/condition",   MAINTENANCE_PORTAL, "Condition Assessment");
        put("/maintenance-portal/program",     MAINTENANCE_PORTAL, "Maintenance Program");
        put("/maintenance-portal/compliance",  MAINTENANCE_PORTAL, "Compliance");
        put("/maintenance-portal/reports",     MAINTENANCE_PORTAL, "Reports");

        // ---- Admin (15) ----
        put("/admin-dashboard",     ADMIN, "Setup");
        put("/users",               ADMIN, "Platform Users");
        put("/guest-portal-users",  ADMIN, "Guest Portal Users");
        put("/customers",           ADMIN, "Customers");
        put("/offices",             ADMIN, "Offices");
        put("/asset-classes",       ADMIN, "Asset Classes");
        put("/connection-classes",  ADMIN, "Connection Classes");
        put("/issue-classes",       ADMIN, "Issue Classes");
        put("/labor",               ADMIN, "Labor");
        put("/materials",           ADMIN, "Materials");
        put("/custom-devices",      ADMIN, "Custom Devices");
        put("/admin/audit-log",     ADMIN, "Audit Log");
        put("/legacy-procedures",   ADMIN, "Legacy Procedures");
        put("/legacy-forms",        ADMIN, "Legacy Forms");
    }

    /**
     * Routes that render real content but have NO sidebar anchor, so {@link #navigateTo} can
     * only reach them by direct URL — and a "not in the nav" reading must not be reported as
     * a dead page or a permission denial. Each was probed with a real browser on 2026-09-02
     * and returned its own heading and, where applicable, a populated grid.
     *
     * <p>{@code /classes} still serves the combined Classes page even though the nav now
     * links the three split routes instead.
     */
    public static final Set<String> UNLINKED_LIVE_ROUTES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "/emps", "/equipment-designations", "/classes", "/goals",
                    "/planning", "/reporting", "/maintenance", "/notes",
                    "/agent", "/analyzer", "/reporting/legacy")));

    /** True when {@code route} is live but has no sidebar anchor (direct URL only). */
    public static boolean isUnlinkedLive(String route) {
        return UNLINKED_LIVE_ROUTES.contains(normalise(route));
    }

    /**
     * Sidebar labels that the re-map found on more than one route. Text-based nav locators
     * are ambiguous for these by construction: the click lands on whichever route the
     * currently-open category renders.
     */
    public static final Set<String> AMBIGUOUS_LABELS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "Reports", "Condition Assessment", "Maintenance Program",
                    "Compliance", "Test Equipment")));

    /** True when {@code label} appears on more than one live route. */
    public static boolean isAmbiguousLabel(String label) {
        return label != null && AMBIGUOUS_LABELS.contains(label.trim());
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

    /**
     * A non-null human label for {@code route}, falling back to the route itself.
     *
     * <p>{@link #labelFor} returning null is meaningful — it says "the nav does not link this"
     * — but it is the wrong value to hand to a report or a message. A live-but-unlinked route
     * is the normal case for this: {@code /classes} still serves the combined Classes page and
     * still has three tabs, yet since the 2026-09-02 re-map the nav links
     * {@code /asset-classes}, {@code /connection-classes} and {@code /issue-classes} instead,
     * so it has no sidebar label. Passing that null into
     * {@code ExtentReportManager.createTest(module, feature, name)} throws
     * "Test name must not be null or empty", which fails the test for a reporting reason and
     * says nothing about the page. Use this wherever a label is being displayed rather than
     * interrogated.
     */
    public static String displayLabelFor(String route) {
        String label = labelFor(route);
        return (label == null || label.isEmpty()) ? normalise(route) : label;
    }

    /**
     * Reverse lookup: the route a sidebar LABEL points at, or null when no live item uses that
     * label. Useful for suites that were written against label lists rather than routes — a
     * null answer is the signal that the label was renamed or removed (e.g. "Jobs" and
     * "Dashboard" no longer exist; they are "Work Orders" and "Site Overview").
     *
     * <p><b>Ambiguous for five labels since the 2026-09-02 re-map</b> — see
     * {@link #AMBIGUOUS_LABELS}. "Reports" alone sits on three different routes, and this
     * method returns whichever comes first in catalog order, which is arbitrary from the
     * caller's point of view. Prefer {@link #routeForLabel(String, String)} whenever the
     * category is known; use {@link #isAmbiguousLabel(String)} to detect the cases where a
     * label on its own cannot identify a route.
     */
    public static String routeForLabel(String label) {
        if (label == null) return null;
        String want = label.trim();
        for (Map.Entry<String, String> e : ROUTE_LABEL.entrySet()) {
            if (e.getValue().equalsIgnoreCase(want)) return e.getKey();
        }
        return null;
    }

    /**
     * Reverse lookup scoped to one rail category — the unambiguous form of
     * {@link #routeForLabel(String)}. Returns null when that category has no item with that
     * label, which distinguishes "this label belongs to a different category" from "this
     * label no longer exists anywhere".
     */
    public static String routeForLabel(String category, String label) {
        if (label == null) return null;
        String want = label.trim();
        for (Map.Entry<String, String> e : ROUTE_LABEL.entrySet()) {
            if (!e.getValue().equalsIgnoreCase(want)) continue;
            String owner = ROUTE_CATEGORY.get(e.getKey());
            if (category == null ? owner == null : category.equals(owner)) return e.getKey();
        }
        return null;
    }

    /** Every route the given rail category links to, in on-screen order. */
    public static List<String> routesIn(String category) {
        List<String> out = new java.util.ArrayList<>();
        for (Map.Entry<String, String> e : ROUTE_CATEGORY.entrySet()) {
            String owner = e.getValue();
            if (category == null ? owner == null : category.equals(owner)) out.add(e.getKey());
        }
        return Collections.unmodifiableList(out);
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
     * Goals lives at the top-level {@code /goals}. The legacy {@code /accounts/goals} path is
     * <b>not in use</b> (owner ruling 2026-08-24) — nothing links to it and its API 500s on
     * load, so it is neither a test target nor a bug to report. Point all Goals coverage at
     * {@link #GOALS_ROUTE}.
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
        // NOT /customers. The "Accounts | Sites" tab split is a PRODUCTION-family surface, not a
        // QA one: on QA V1.36 the page renders a card list of customers with zero [role=tab]
        // elements and no data grid (verified live 2026-09-02), while prod and BCES-IQ — both on
        // V2.0 — do show the two tabs. Listing it here made the tab catalog assert a surface QA
        // does not have, which is a guaranteed-red check that says nothing about QA.
        // The tab split IS covered, against the environment that has it, by
        // ProductionSmokeTestNG#testTC_PROD_06_MultiRoleTabGate.
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

    /**
     * The rail button for a category, keyed on the {@code aria-label} the rail actually
     * carries — verified live 2026-09-02: {@code nav[aria-label="navigation"]} holds one
     * {@code button[aria-label="…"]} per category.
     *
     * <p><b>Why not text.</b> The old locator was
     * {@code //button[.//span[normalize-space()='Engineering'] or normalize-space()='Engineering']},
     * which matches anywhere in the document. Four category names collide with in-page
     * controls — most sharply "Engineering", which is also a tab on the asset detail page:
     * an unscoped match hits the rail button and silently navigates to /arc-flash instead of
     * switching tabs. Scoping to the rail {@code nav} and keying on aria-label removes both
     * the collision and the span-vs-button guesswork.
     *
     * <p>The text form is kept as a second branch so the locator still resolves if the rail
     * ever drops its aria-labels, but it is scoped to the rail nav either way.
     */
    private static By railButton(String category) {
        return By.xpath(
                "//nav[@aria-label='navigation']//button[@aria-label='" + category + "']"
                + " | //nav[@aria-label='navigation']//button[.//span[normalize-space()='"
                + category + "'] or normalize-space()='" + category + "']");
    }

    /**
     * A second-level module anchor, keyed on href. This is the only unambiguous way to click
     * a nav item since the 2026-09-02 re-map put the same label on up to three routes.
     */
    private static By navAnchor(String route) {
        return By.cssSelector("a[href='" + route + "'], a[href='" + route + "/']");
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

    /**
     * Open the Dashboards panel via the rail logo.
     *
     * @deprecated Vestigial since the 2026-09-02 re-map: the three dashboards moved into
     *     Site Data ({@code /dashboard}), Operations ({@code /ops-dashboard}) and Sales
     *     ({@code /sales-overview}), so {@link #navigateTo} reaches them by expanding the
     *     owning category and no caller needs the logo. Kept because it still resolves and
     *     external callers may reference it; it navigates to /dashboard as a side effect.
     */
    @Deprecated
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
        }
        // Everything else needs no expansion: Help (/z-university) is rendered in every
        // category's panel, and the three dashboards are now inside Site Data / Operations /
        // Sales like any other module (re-map 2026-09-02), so they are handled by the branch
        // above. Routes in UNLINKED_LIVE_ROUTES have no anchor at all and fall through to the
        // direct-URL step below by design.

        try {
            List<WebElement> links = driver.findElements(navAnchor(target));
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
        // Since the 2026-09-02 re-map the three dashboards sit inside Site Data / Operations /
        // Sales, so expanding the seven categories covers the whole nav — the rail-logo step
        // this method used to need is gone. Maintenance Portal is one of those seven; before
        // it was added, its five routes were invisible to this sweep and every RBAC
        // visibility check scored them as "role cannot see it".
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
