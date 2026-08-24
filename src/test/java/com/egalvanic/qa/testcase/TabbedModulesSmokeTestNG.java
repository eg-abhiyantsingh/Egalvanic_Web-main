package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.NavCatalog;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab-level smoke coverage for the V1.36 tabbed list pages — driven by the
 * {@link NavCatalog} tab catalog, which was built by CLICKING every tab live on 2026-08-24.
 *
 * <p>Why this suite exists: the 2026-08-24 coverage audit found that of the seven tabbed list
 * pages, only Arc Flash Readiness had its tabs exercised by name. The other sixteen tabs —
 * Condition Assessment (2), Customers (2), Labor (3), Materials (4), Test Equipment (2),
 * Classes (3) — were covered by page-shell loads only, so a tab could disappear or render
 * empty without any suite noticing. This class walks each catalogued tab through the real
 * sidebar (rail category → module link → tab click) and fails loudly when a catalogued tab is
 * missing or its panel renders nothing.
 *
 * <p>It also covers the three "not-a-tab" switch surfaces the audit flagged as uncovered:
 * the Quotes sidebar status filters, the Planned Work due-date buckets, and the Reporting
 * "Report Builder | Branding" view toggle — plus the Scheduling calendar toolbar.
 */
public class TabbedModulesSmokeTestNG extends BaseTest {

    private static final String MODULE = "Tabbed Modules";

    // ================================================================
    // 1. Every catalogued list-page tab
    // ================================================================

    @DataProvider(name = "tabbedRoutes")
    public Object[][] tabbedRoutes() {
        List<Object[]> rows = new ArrayList<>();
        for (String route : NavCatalog.tabbedRoutes()) {
            if (route.contains("{id}")) continue;   // detail tab sets need a data row — module suites own those
            rows.add(new Object[]{route, NavCatalog.labelFor(route)});
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "tabbedRoutes",
          description = "Every catalogued tab on the page exists, clicks, and renders content")
    public void testAllTabsPresentAndRender(String route, String label) {
        ExtentReportManager.createTest(MODULE, label, "Tabs_" + route.replaceAll("\\W+", "_"));

        Assert.assertTrue(NavCatalog.navigateTo(driver, route),
                "Could not reach " + route + " (" + label + ") through the sidebar");
        pause(4000);
        waitAndDismissAppAlert();

        StringBuilder summary = new StringBuilder();
        for (String tab : NavCatalog.tabsFor(route)) {
            boolean clicked = NavCatalog.clickTab(driver, tab);
            Assert.assertTrue(clicked,
                    route + ": catalogued tab '" + tab + "' is MISSING from the live page — "
                    + "either the UI changed again (update NavCatalog.TABS) or the tab regressed.");
            pause(2500);

            // The tab's panel must render something: a grid/table, or non-trivial main content.
            boolean hasGrid = !driver.findElements(By.cssSelector(
                    ".MuiDataGrid-root, [role='grid'], table")).isEmpty();
            int mainLen = mainTextLength();
            Assert.assertTrue(hasGrid || mainLen > 150,
                    route + " tab '" + tab + "' rendered no content (no grid, main text only "
                    + mainLen + " chars) — blank-panel regression.");
            summary.append(tab).append(" ✓  ");
        }
        logStepWithScreenshot(label + " tabs verified: " + summary);
        ExtentReportManager.logPass(label + " (" + route + "): all "
                + NavCatalog.tabsFor(route).size() + " tabs present and rendering — " + summary);
    }

    // ================================================================
    // 2. Quotes sidebar status filters (label -> query param is NOT 1:1)
    // ================================================================

    @DataProvider(name = "quoteStatuses")
    public Object[][] quoteStatuses() {
        // Live-verified mapping 2026-08-24. Note "Closed Won"->accepted, "Closed Lost"->rejected.
        return new Object[][]{
            {"Draft",            "status=draft"},
            {"Pending Response", "status=pendingResponse"},
            {"Closed Won",       "status=accepted"},
            {"Closed Lost",      "status=rejected"},
            {"Cancelled",        "status=cancelled"},
        };
    }

    @Test(dataProvider = "quoteStatuses",
          description = "Quotes sidebar status sub-links navigate to the right ?status= param")
    public void testQuoteStatusSubLink(String linkLabel, String expectedParam) {
        ExtentReportManager.createTest(MODULE, "Quotes status filters",
                "QuoteStatus_" + linkLabel.replaceAll("\\W+", "_"));

        // The status sub-links render under the Quotes item only while Quotes is the open
        // module, so go there through the sidebar first.
        Assert.assertTrue(NavCatalog.navigateTo(driver, "/opportunities"),
                "Could not reach Quotes through the sidebar");
        pause(3500);

        boolean clicked = clickSidebarItem(linkLabel);
        Assert.assertTrue(clicked, "Quotes status sub-link '" + linkLabel
                + "' not found in the expanded sidebar");
        pause(3000);

        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains(expectedParam),
                "'" + linkLabel + "' should filter with " + expectedParam + " — got: " + url);
        logStep("'" + linkLabel + "' -> " + url);
        ExtentReportManager.logPass("Quotes status '" + linkLabel + "' filters via " + expectedParam);
    }

    // ================================================================
    // 3. Planned Work due-date buckets
    // ================================================================

    @DataProvider(name = "planBuckets")
    public Object[][] planBuckets() {
        return new Object[][]{
            {"Overdue",          "bucket=overdue"},
            {"Due this month",   "bucket=due_30"},
            {"Due This Quarter", "bucket=due_quarter"},
            {"Due this year",    "bucket=due_year"},
        };
    }

    @Test(dataProvider = "planBuckets",
          description = "Planned Work sidebar buckets navigate to the right ?bucket= param")
    public void testPlannedWorkBucket(String linkLabel, String expectedParam) {
        ExtentReportManager.createTest(MODULE, "Planned Work buckets",
                "Bucket_" + linkLabel.replaceAll("\\W+", "_"));

        Assert.assertTrue(NavCatalog.navigateTo(driver, "/planned-work"),
                "Could not reach Planned Work through the sidebar");
        pause(3500);

        boolean clicked = clickSidebarItem(linkLabel);
        Assert.assertTrue(clicked, "Planned Work bucket '" + linkLabel
                + "' not found in the expanded sidebar");
        pause(3000);

        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains(expectedParam),
                "'" + linkLabel + "' should filter with " + expectedParam + " — got: " + url);
        logStep("'" + linkLabel + "' -> " + url);
        ExtentReportManager.logPass("Planned Work bucket '" + linkLabel + "' filters via " + expectedParam);
    }

    // ================================================================
    // 4. Reporting "Report Builder | Branding" view toggle
    // ================================================================

    @Test(description = "Reporting Branding view toggle renders the stylesheet editor")
    public void testReportingBrandingToggle() {
        ExtentReportManager.createTest(MODULE, "Reporting", "BrandingToggle");

        Assert.assertTrue(NavCatalog.navigateTo(driver, "/reporting/builder"),
                "Could not reach Reports through the sidebar");
        pause(4000);

        boolean toggled = clickVisibleButton("Branding");
        Assert.assertTrue(toggled, "The 'Branding' view-toggle button is missing on /reporting/builder");
        pause(3000);

        String body = driver.findElement(By.tagName("body")).getText();
        Assert.assertTrue(body.contains("Regenerate Default") || body.contains("Stylesheet"),
                "Branding view did not render the stylesheet editor (no 'Regenerate Default'/'Stylesheet')");
        // and back
        Assert.assertTrue(clickVisibleButton("Report Builder"),
                "Could not toggle back to the Report Builder view");
        logStepWithScreenshot("Branding toggle roundtrip");
        ExtentReportManager.logPass("Reporting Branding view renders the stylesheet editor and toggles back");
    }

    // ================================================================
    // 5. Scheduling calendar toolbar
    // ================================================================

    @Test(description = "Scheduling renders its calendar toolbar and switches views")
    public void testSchedulingToolbar() {
        ExtentReportManager.createTest(MODULE, "Scheduling", "CalendarToolbar");

        Assert.assertTrue(NavCatalog.navigateTo(driver, "/scheduling"),
                "Could not reach Scheduling through the sidebar");
        pause(5000);

        for (String btn : new String[]{"Today", "Back", "Next", "Quarter", "Month", "Week", "Day"}) {
            Assert.assertTrue(buttonVisible(btn),
                    "Scheduling toolbar button '" + btn + "' is missing — calendar may have regressed");
        }
        // Switch to Week view and confirm the calendar re-renders with day columns.
        Assert.assertTrue(clickVisibleButton("Week"), "Could not click the 'Week' view button");
        pause(2500);
        String body = driver.findElement(By.tagName("body")).getText();
        Assert.assertTrue(body.contains("Sun") || body.contains("Mon"),
                "Week view did not render weekday columns");
        logStepWithScreenshot("Scheduling calendar (Week view)");
        ExtentReportManager.logPass("Scheduling calendar toolbar complete; Week view renders");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private int mainTextLength() {
        try {
            return driver.findElement(By.cssSelector("main")).getText().trim().length();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Click a sidebar entry (link or list item) by its exact visible text, left rail area only. */
    private boolean clickSidebarItem(String label) {
        Object r = ((JavascriptExecutor) driver).executeScript(
            "var l=arguments[0];"
          + "var els=[].slice.call(document.querySelectorAll('a,li'));"
          + "var hit=els.find(function(e){var rc=e.getBoundingClientRect();"
          + "  return rc.width>0 && rc.x<320 && (e.innerText||'').trim()===l;});"
          + "if(hit){ (hit.tagName==='LI' && hit.querySelector('a') ? hit.querySelector('a') : hit).click(); return true; }"
          + "return false;", label);
        return Boolean.TRUE.equals(r);
    }

    private boolean buttonVisible(String label) {
        for (WebElement b : driver.findElements(By.xpath(
                "//button[normalize-space()='" + label + "']"))) {
            try { if (b.isDisplayed()) return true; } catch (Exception ignored) { }
        }
        return false;
    }

    private boolean clickVisibleButton(String label) {
        for (WebElement b : driver.findElements(By.xpath(
                "//button[normalize-space()='" + label + "']"))) {
            try {
                if (!b.isDisplayed()) continue;
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b);
                return true;
            } catch (Exception ignored) { }
        }
        return false;
    }
}
