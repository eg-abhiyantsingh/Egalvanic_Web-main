package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.utils.NavCatalog;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Regression harness for {@link NavCatalog} — a {@code main()}, excluded from every suite,
 * in the same spirit as {@code VerifierSelfValidation}.
 *
 * <p>It proves against the LIVE app the two claims NavCatalog exists to make, either of which
 * silently breaks navigation if it stops holding:
 *
 * <ol>
 *   <li><b>A collapsed category's links are absent from the DOM.</b> Reading the sidebar
 *       without expanding returns only the open category, so a permission-gating check built
 *       on one read under-reports massively. Asserted by comparing a naive single read against
 *       {@link NavCatalog#collectAllNavHrefs}.</li>
 *   <li><b>Cross-category navigation needs the rail.</b> From /assets, clicking "Work Orders"
 *       by text no-ops; going through NavCatalog lands on /sessions.</li>
 * </ol>
 *
 * Run:
 * <pre>
 * mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
 * java -cp "target/classes:target/test-classes:$(cat /tmp/cp.txt)" \
 *   com.egalvanic.qa.testcase.NavCatalogSelfValidation
 * </pre>
 */
public final class NavCatalogSelfValidation {

    private static int failures = 0;

    public static void main(String[] args) {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);   // QA host uses an internal-CA cert
        String binary = System.getProperty("chrome.binary", System.getenv("CHROME_BINARY"));
        if (binary != null && !binary.isEmpty()) options.setBinary(binary);

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().window().maximize();
            login(driver);

            checkAllCategoriesHarvested(driver);
            checkCrossCategoryNavigation(driver);
            checkCatalogRoutesResolve(driver);
            checkDashboardsInCategories(driver);
            checkTabCatalog(driver);

            System.out.println("\n==================================================");
            System.out.println(failures == 0
                    ? "NavCatalog self-validation: ALL CHECKS PASSED"
                    : "NavCatalog self-validation: " + failures + " CHECK(S) FAILED");
            System.out.println("==================================================");
        } finally {
            driver.quit();
        }
        if (failures > 0) System.exit(1);
    }

    private static void login(WebDriver driver) {
        driver.get(AppConstants.BASE_URL + "/login");
        LoginPage login = new LoginPage(driver);
        login.waitForPageLoaded(30);
        login.selectEnglishIfOffered();
        login.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
        sleep(8000);
        System.out.println("[setup] logged in, landed on " + driver.getCurrentUrl());
    }

    /** Claim 1: expanding every rail category finds far more routes than a single read. */
    private static void checkAllCategoriesHarvested(WebDriver driver) {
        driver.get(AppConstants.BASE_URL + "/assets");
        sleep(6000);

        Set<String> naive = new LinkedHashSet<>();
        for (org.openqa.selenium.WebElement a : driver.findElements(By.cssSelector("a[href]"))) {
            try {
                if (!a.isDisplayed()) continue;
                String p = NavCatalog.pathOf(a.getAttribute("href"));
                if (!p.isEmpty()) naive.add(p);
            } catch (Exception ignored) { }
        }
        Set<String> full = NavCatalog.collectAllNavHrefs(driver);

        System.out.println("\n[check 1] single read     : " + naive.size() + " routes " + naive);
        System.out.println("[check 1] all-categories  : " + full.size() + " routes");

        // Every category must contribute at least one route to the harvest.
        for (String category : NavCatalog.CATEGORIES) {
            boolean covered = false;
            for (String route : NavCatalog.allRoutes()) {
                if (category.equals(NavCatalog.categoryFor(route)) && full.contains(route)) {
                    covered = true;
                    break;
                }
            }
            check(covered,
                    "category '" + category + "' contributed routes",
                    "category '" + category + "' contributed NO route to the harvest");
        }
        check(full.size() > naive.size(),
                "expanding categories revealed " + (full.size() - naive.size()) + " routes a single read missed",
                "expanding categories found no extra routes (" + full.size() + " vs " + naive.size()
                + ") — the rail may no longer collapse, re-check NavCatalog's premise");
        check(full.size() >= 25,
                "harvested " + full.size() + " sidebar routes for an all-modules admin",
                "expected >=25 sidebar routes, got " + full.size() + ": " + full);
    }

    /** Claim 2: a cross-category jump only works through the rail. */
    private static void checkCrossCategoryNavigation(WebDriver driver) {
        driver.get(AppConstants.BASE_URL + "/assets");
        sleep(6000);

        boolean presentBefore = !driver.findElements(
                By.xpath("//a[normalize-space()='Work Orders']")).isEmpty();
        System.out.println("\n[check 2] 'Work Orders' anchor present while on /assets: " + presentBefore);

        check(!presentBefore,
                "'Work Orders' is absent from the DOM while Site Data is open — the rail really does collapse",
                "'Work Orders' was already in the DOM — categories may no longer collapse, "
                + "re-check whether NavCatalog's expansion step is still needed");

        boolean landed = NavCatalog.navigateTo(driver, "/sessions");
        System.out.println("[check 2] NavCatalog.navigateTo(/sessions) -> " + driver.getCurrentUrl());
        check(landed && NavCatalog.onRoute(driver, "/sessions"),
                "NavCatalog reached /sessions from /assets across categories",
                "NavCatalog could not reach /sessions from /assets — landed on " + driver.getCurrentUrl());
    }

    /** Every route the catalog claims is a nav destination must actually resolve. */
    private static void checkCatalogRoutesResolve(WebDriver driver) {
        System.out.println("\n[check 3] resolving every catalogued route");
        for (String route : NavCatalog.allRoutes()) {
            driver.get(AppConstants.BASE_URL + route);
            sleep(2500);
            String landed = NavCatalog.pathOf(driver.getCurrentUrl());
            boolean ok = landed.equals(route);
            if (!ok) {
                System.out.println("   [FAIL] " + route + " -> " + landed);
                failures++;
            }
        }
        System.out.println("[check 3] done");
    }

    /**
     * The three dashboards must be reachable from a module page BY CLICKING, not by URL.
     *
     * <p>Re-mapped 2026-09-02: the dashboards no longer live behind the rail logo. They moved
     * into ordinary rail categories — /dashboard under Site Data, /ops-dashboard under
     * Operations, /sales-overview under Sales — so reaching them is a normal category
     * expansion and {@code openDashboards} is deprecated.
     *
     * <p>This check asserts the SIDEBAR path specifically, by confirming the anchor is in the
     * DOM after the owning category is expanded. The previous version asserted only that
     * {@code navigateTo} ended up on the route, which its own {@code driver.get()} fallback
     * satisfies even when the sidebar click never worked — the check passed against a session
     * where the app had not rendered a single anchor.
     */
    private static void checkDashboardsInCategories(WebDriver driver) {
        System.out.println("\n[check 4] dashboards reachable through their rail categories");
        String[][] dashboards = {
            {"/dashboard",      NavCatalog.SITE_DATA},
            {"/ops-dashboard",  NavCatalog.OPERATIONS},
            {"/sales-overview", NavCatalog.SALES},
        };
        for (String[] pair : dashboards) {
            String route = pair[0];
            String category = pair[1];

            check(category.equals(NavCatalog.categoryFor(route)),
                    route + " is catalogued under '" + category + "'",
                    route + " should be catalogued under '" + category + "' but the catalog says '"
                            + NavCatalog.categoryFor(route) + "'");

            // Start from a page in a DIFFERENT category so the anchor really is absent first.
            NavCatalog.navigateTo(driver, "/assets");
            sleep(2500);
            NavCatalog.openCategory(driver, category);
            sleep(1800);
            boolean anchorPresent = !driver.findElements(
                    By.cssSelector("a[href='" + route + "']")).isEmpty();
            check(anchorPresent,
                    route + " anchor is in the DOM after expanding '" + category + "'",
                    route + " anchor is NOT in the DOM after expanding '" + category
                            + "' — the sidebar path is broken, so navigateTo would silently "
                            + "fall back to a direct URL and stop testing the nav");

            boolean landed = NavCatalog.navigateTo(driver, route);
            check(landed && NavCatalog.onRoute(driver, route),
                    "reached " + route + " from /assets",
                    "could not reach " + route + " from /assets — landed on " + driver.getCurrentUrl());
        }
    }

    /**
     * Every tab the catalog claims for a tabbed LIST route must exist and be clickable on the
     * live page. (Detail-page tab sets need a data row and are exercised by module suites.)
     */
    private static void checkTabCatalog(WebDriver driver) {
        System.out.println("\n[check 5] tab catalog vs live pages");
        for (String route : NavCatalog.tabbedRoutes()) {
            if (route.contains("{id}")) continue;
            NavCatalog.navigateTo(driver, route);
            sleep(4000);
            for (String tab : NavCatalog.tabsFor(route)) {
                boolean clicked = NavCatalog.clickTab(driver, tab);
                check(clicked,
                        route + " tab '" + tab + "' present and clicked",
                        route + " tab '" + tab + "' NOT FOUND on the live page");
            }
        }
    }

    /** Report a check, stating what actually held on pass and what went wrong on fail. */
    private static void check(boolean condition, String onPass, String onFail) {
        if (condition) {
            System.out.println("   [PASS] " + onPass);
        } else {
            System.out.println("   [FAIL] " + onFail);
            failures++;
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
