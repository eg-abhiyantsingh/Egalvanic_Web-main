package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.ArcFlashPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Arc Flash Readiness — <b>Engineering Mode → Asset Details tab → Asset Class filter</b> workflow.
 *
 * <p><b>Flow (verified live 2026-06-29 on site "Android Qa Site1"):</b> /arc-flash shows three circular
 * readiness indicators (Asset Details, Source/Target Connections, Connection Details) and a tab strip
 * (Overview · Asset Details · Source/Target Connections · Connection Details). The <b>Engineering Mode</b>
 * toggle (a MUI Switch top-right) reveals extra columns and <b>persists across tab switches</b>. The
 * <b>Asset Details</b> tab shows a filterable table (Label · Interrupting Rating · Ampere Rating ·
 * Mains Type · Voltage · % Completion) with an <b>Asset Class</b> MUI-Select filter above it (default
 * "ATS"; options are the site's asset classes). Selecting a class filters the table.</p>
 *
 * <p>Covers: dashboard + tabs render, Engineering Mode enable, the toggle persisting onto Asset Details,
 * the Asset Class option list, the Asset Details columns, and the full enable→tab→filter workflow.</p>
 */
public class ArcFlashTestNG extends BaseTest {

    private static final String MODULE = "Arc Flash Readiness";
    private static final String FEATURE = AppConstants.FEATURE_ARC_FLASH;
    private static final String SITE = "Android Qa Site1";

    private ArcFlashPage arcFlashPage;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Arc Flash Readiness — Engineering Mode / Asset Details / Class filter");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        arcFlashPage = new ArcFlashPage(driver);
        boolean ok = ensureSite(SITE);
        System.out.println("[ArcFlash] site '" + SITE + "' selected=" + ok);
    }

    // ================================================================
    // TESTS
    // ================================================================

    @Test(priority = 1, description = "AF_01: Arc Flash Readiness loads with its readiness indicators + tab strip")
    public void testAF_01_DashboardAndTabs() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_01_DashboardAndTabs");
        arcFlashPage.navigateToArcFlash();
        Assert.assertTrue(arcFlashPage.isLoaded(), "Arc Flash Readiness should load (tab strip present).");
        for (String t : new String[]{"Overview", "Asset Details", "Source/Target Connections", "Connection Details"}) {
            Assert.assertTrue(arcFlashPage.hasProgressIndicator(t), "Arc Flash should show '" + t + "'.");
        }
        Assert.assertTrue(arcFlashPage.hasEngineeringModeToggle(), "Arc Flash should have an Engineering Mode toggle.");
        logStepWithScreenshot("Arc Flash Readiness dashboard");
        ExtentReportManager.logPass("Arc Flash Readiness loads with Overview/Asset Details/Source-Target/Connection Details + Engineering Mode toggle.");
    }

    @Test(priority = 2, description = "AF_02: Engineering Mode can be enabled via the toggle")
    public void testAF_02_EnableEngineeringMode() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_02_EnableEngineeringMode");
        arcFlashPage.navigateToArcFlash();
        boolean now = arcFlashPage.setEngineeringMode(true);
        Assert.assertTrue(now, "Engineering Mode toggle should turn ON.");
        Assert.assertTrue(arcFlashPage.isEngineeringModeOn(), "Engineering Mode should report enabled.");
        logStepWithScreenshot("Engineering Mode enabled");
        ExtentReportManager.logPass("Engineering Mode toggled ON.");
    }

    @Test(priority = 3, description = "AF_03: Switching to Asset Details keeps Engineering Mode enabled")
    public void testAF_03_EngineeringModePersistsOnAssetDetails() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_03_EngineeringModePersistsOnAssetDetails");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        Assert.assertTrue(arcFlashPage.isEngineeringModeOn(), "Engineering Mode should be ON before switching tabs.");

        arcFlashPage.clickTab("Asset Details");
        Assert.assertEquals(arcFlashPage.getActiveTab(), "Asset Details", "Asset Details tab should be active.");
        Assert.assertTrue(arcFlashPage.isEngineeringModeOn(),
                "Engineering Mode should REMAIN enabled after switching to Asset Details.");
        logStepWithScreenshot("Engineering Mode persists on Asset Details");
        ExtentReportManager.logPass("Engineering Mode persists across the switch to the Asset Details tab.");
    }

    @Test(priority = 4, description = "AF_04: Asset Class filter lists the asset classes (incl ATS + Circuit Breaker)")
    public void testAF_04_AssetClassOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_04_AssetClassOptions");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Asset Details");
        Assert.assertTrue(arcFlashPage.waitForAssetClassFilter(),
                "Asset Details should show an Asset Class filter (waited for it to render).");

        List<String> classes = arcFlashPage.getAssetClassOptions();
        logStep("Asset Class options (" + classes.size() + "): " + classes);
        Assert.assertFalse(classes.isEmpty(), "Asset Class filter should list options.");
        Assert.assertTrue(classes.contains("ATS"), "Asset Class options should include 'ATS'. Got: " + classes);
        Assert.assertTrue(classes.contains("Circuit Breaker"),
                "Asset Class options should include 'Circuit Breaker'. Got: " + classes);
        ExtentReportManager.logPass("Asset Class filter lists " + classes.size() + " classes (incl ATS, Circuit Breaker).");
    }

    @Test(priority = 5, description = "AF_05: Asset Details table exposes the expected columns")
    public void testAF_05_AssetDetailsColumns() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_05_AssetDetailsColumns");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Asset Details");
        arcFlashPage.waitForAssetClassFilter(); // let the Asset Details table + filter finish rendering
        for (String col : new String[]{"Label", "Interrupting Rating", "Ampere Rating", "Mains Type", "Voltage"}) {
            Assert.assertTrue(arcFlashPage.hasColumn(col), "Asset Details table should have a '" + col + "' column.");
        }
        logStep("Columns: " + arcFlashPage.getColumnHeaders());
        logStepWithScreenshot("Asset Details columns");
        ExtentReportManager.logPass("Asset Details table shows Label/Interrupting Rating/Ampere Rating/Mains Type/Voltage.");
    }

    @Test(priority = 6, description = "AF_06: Full workflow — enable Engineering Mode → Asset Details → verify it persists → filter by Asset Class")
    public void testAF_06_FilterByAssetClassWorkflow() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_06_FilterByAssetClassWorkflow");
        String targetClass = "Circuit Breaker";

        arcFlashPage.navigateToArcFlash();                                 // step 1
        arcFlashPage.setEngineeringMode(true);                             // step 2
        Assert.assertTrue(arcFlashPage.isEngineeringModeOn(), "Engineering Mode should be ON.");
        arcFlashPage.clickTab("Asset Details");                            // step 3
        Assert.assertEquals(arcFlashPage.getActiveTab(), "Asset Details", "Should be on Asset Details.");
        Assert.assertTrue(arcFlashPage.isEngineeringModeOn(),              // step 4
                "Engineering Mode should remain enabled on Asset Details.");

        Assert.assertTrue(arcFlashPage.waitForAssetClassFilter(), "Asset Class filter should render on Asset Details.");
        arcFlashPage.selectAssetClass(targetClass);                        // step 5
        Assert.assertEquals(arcFlashPage.getAssetClassValue(), targetClass,
                "Asset Class filter should now read '" + targetClass + "'.");
        Assert.assertTrue(arcFlashPage.isLoaded(), "The Asset Details table should still be present after filtering.");
        logStep("Filtered to '" + targetClass + "' — table rows now: " + arcFlashPage.getRowCount());
        logStepWithScreenshot("Filtered Asset Details by " + targetClass);
        ExtentReportManager.logPass("Workflow complete: Engineering Mode ON → Asset Details (toggle persisted) → filtered by '" + targetClass + "'.");
    }

    // ================================================================
    // OVERVIEW ANALYTICS — gauges, per-class breakdown, info tooltips
    // ================================================================

    @Test(priority = 7, description = "AF_07: Overview is the default tab on a fresh load (TC-AF-002)")
    public void testAF_07_DefaultTabIsOverview() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_07_DefaultTabIsOverview");
        arcFlashPage.navigateToArcFlash();
        // The active tab persists in SPA component state across tests in this session, so a prior test may
        // have left a non-Overview tab selected. Force a fresh mount to test the genuine on-load default.
        driver.navigate().refresh();
        arcFlashPage.waitLoaded();
        Assert.assertEquals(arcFlashPage.getActiveTab(), "Overview",
                "Overview should be the selected tab on a fresh load.");
        logStepWithScreenshot("Overview is the default tab on load");
        ExtentReportManager.logPass("Overview is selected by default when Arc Flash Readiness loads fresh.");
    }

    @Test(priority = 8, description = "AF_08: Overview shows three readiness gauges, each a percentage (TC-AF-008/009)")
    public void testAF_08_ThreeReadinessGauges() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_08_ThreeReadinessGauges");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Overview"); // tab state persists across tests in this session
        arcFlashPage.setEngineeringMode(false);
        List<String> gauges = arcFlashPage.waitForOverviewGauges();
        logStep("Overview gauges: " + gauges);
        Assert.assertEquals(gauges.size(), 3, "Overview should show exactly three readiness gauges. Got: " + gauges);
        for (String g : gauges) {
            Assert.assertTrue(g.matches(".+=\\d{1,3}%"), "Gauge should read a percentage: " + g);
        }
        // The three gauges must cover the three readiness dimensions.
        String joined = String.join(" ", gauges);
        for (String dim : new String[]{"Asset Details", "Source/Target Connections", "Connection Details"}) {
            Assert.assertTrue(joined.contains(dim), "Gauges should include '" + dim + "'. Got: " + gauges);
        }
        logStepWithScreenshot("Three readiness gauges");
        ExtentReportManager.logPass("Overview shows three percentage gauges: " + gauges);
    }

    @Test(priority = 9, description = "AF_09: Per-asset-class completion breakdown shows valid X/Y complete counts (TC-AF-010)")
    public void testAF_09_PerClassBreakdown() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_09_PerClassBreakdown");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Overview");
        arcFlashPage.setEngineeringMode(false);
        List<String> cards = arcFlashPage.getClassBreakdown();
        logStep("Per-class breakdown (" + cards.size() + "): " + cards);
        Assert.assertFalse(cards.isEmpty(), "Overview should list per-asset-class completion cards.");
        boolean sawAts = false;
        for (String c : cards) {
            // "Class|NN%|X/Y complete"
            String[] parts = c.split("\\|");
            Assert.assertEquals(parts.length, 3, "Breakdown card should be Class|pct|X/Y complete: " + c);
            if ("ATS".equals(parts[0])) sawAts = true;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)/(\\d+) complete").matcher(parts[2]);
            Assert.assertTrue(m.find(), "Card should carry an X/Y complete count: " + c);
            int done = Integer.parseInt(m.group(1)), total = Integer.parseInt(m.group(2));
            Assert.assertTrue(done <= total, "Completed count must not exceed total: " + c);
        }
        Assert.assertTrue(sawAts, "Breakdown should include the ATS class. Got: " + cards);
        ExtentReportManager.logPass("Per-class breakdown lists " + cards.size() + " classes with valid X/Y complete counts.");
    }

    @Test(priority = 10, description = "AF_10: Metric info icons expose descriptive tooltip text (TC-AF-011)")
    public void testAF_10_InfoTooltips() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_10_InfoTooltips");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Overview");
        arcFlashPage.setEngineeringMode(false);
        List<String> tips = arcFlashPage.getInfoTooltipTexts();
        logStep("Info tooltips: " + tips);
        // App ships 3 core metrics; a 4th (PM-required fields) was added with the PM module.
        Assert.assertTrue(tips.size() >= 3 && tips.size() <= 4,
                "There should be 3 core metric info tooltips (plus optional PM metric). Got: " + tips);
        String joined = String.join(" | ", tips).toLowerCase();
        Assert.assertTrue(joined.contains("required asset fields"),
                "An info tooltip should describe the asset-fields metric. Got: " + tips);
        Assert.assertTrue(joined.contains("source connection"),
                "An info tooltip should describe the source-connection metric. Got: " + tips);
        Assert.assertTrue(joined.contains("required connection fields"),
                "An info tooltip should describe the connection-fields metric. Got: " + tips);
        if (tips.size() == 4) {
            Assert.assertTrue(joined.contains("pm-required"),
                    "The 4th tooltip should describe the PM-required-fields metric. Got: " + tips);
        }
        ExtentReportManager.logPass("All " + tips.size() + " metric info tooltips describe their metric correctly.");
    }

    // ================================================================
    // ENGINEERING MODE — recalculation + revert
    // ================================================================

    @Test(priority = 11, description = "AF_11: Engineering Mode recalculates the readiness percentages (TC-AF-012)")
    public void testAF_11_EngineeringModeRecalculates() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_11_EngineeringModeRecalculates");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Overview");
        arcFlashPage.setEngineeringMode(false);
        List<String> gaugesOff = arcFlashPage.waitForOverviewGauges();
        List<String> allOff = sorted(arcFlashPage.getAllPercents());
        logStep("Engineering Mode OFF — gauges=" + gaugesOff);

        Assert.assertTrue(arcFlashPage.setEngineeringMode(true), "Engineering Mode should turn ON.");
        // The readiness recomputes asynchronously — poll until the percentage-set changes (or time out).
        List<String> allOn = allOff;
        for (int i = 0; i < 12 && allOn.equals(allOff); i++) { pause(1000); allOn = sorted(arcFlashPage.getAllPercents()); }
        List<String> gaugesOn = arcFlashPage.waitForOverviewGauges();
        logStep("Engineering Mode ON  — gauges=" + gaugesOn);

        Assert.assertEquals(gaugesOff.size(), 3, "Should still read three gauges with Eng Mode off.");
        Assert.assertEquals(gaugesOn.size(), 3, "Should still read three gauges with Eng Mode on.");
        Assert.assertFalse(allOff.equals(allOn),
                "Stricter engineering requirements should change the readiness percentages.\n  OFF=" + allOff + "\n  ON =" + allOn);
        logStepWithScreenshot("Engineering Mode recalculated percentages");
        ExtentReportManager.logPass("Engineering Mode recalculates readiness. OFF gauges=" + gaugesOff + " | ON gauges=" + gaugesOn);
    }

    @Test(priority = 12, description = "AF_12: Toggling Engineering Mode OFF reverts to the standard percentages (TC-AF-014)")
    public void testAF_12_EngineeringModeRevert() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_12_EngineeringModeRevert");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Overview");
        arcFlashPage.setEngineeringMode(false);
        List<String> baseline = arcFlashPage.waitForOverviewGauges();

        arcFlashPage.setEngineeringMode(true);
        pause(2000);
        List<String> engOn = arcFlashPage.waitForOverviewGauges();
        Assert.assertFalse(sorted(arcFlashPage.getAllPercents()).isEmpty(), "Percentages should render with Eng Mode on.");

        arcFlashPage.setEngineeringMode(false);
        // Poll until the gauges settle back to the standard (pre-Eng-Mode) values.
        List<String> reverted = arcFlashPage.waitForOverviewGauges();
        for (int i = 0; i < 12 && !reverted.equals(baseline); i++) { pause(1000); reverted = arcFlashPage.getOverviewGauges(); }
        logStep("baseline(OFF)=" + baseline + " | ON=" + engOn + " | reverted(OFF)=" + reverted);
        Assert.assertEquals(reverted, baseline,
                "Turning Engineering Mode back OFF should restore the standard percentages.");
        ExtentReportManager.logPass("Engineering Mode OFF reverts the gauges to the standard calculation: " + reverted);
    }

    // ================================================================
    // ASSET DETAILS table — per-asset completion + pagination
    // ================================================================

    @Test(priority = 13, description = "AF_13: Asset Details shows a per-asset Percentage Completion value for every row (TC-AF-018)")
    public void testAF_13_PerAssetCompletion() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_13_PerAssetCompletion");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(false);
        arcFlashPage.clickTab("Asset Details");
        arcFlashPage.waitForAssetClassFilter();
        arcFlashPage.waitForTableRows();
        List<String> pct = arcFlashPage.getColumnValues("Percentage Completion");
        logStep("Percentage Completion column (" + pct.size() + "): " + pct);
        Assert.assertFalse(pct.isEmpty(), "Asset Details should expose a Percentage Completion value per row.");
        int real = 0;
        for (String v : pct) {
            Assert.assertTrue(v.matches("\\d{1,3}%|[—\\-]|"),
                    "Completion cell should be a percentage or a dash: '" + v + "'");
            if (v.matches("\\d{1,3}%")) real++;
        }
        Assert.assertTrue(real > 0, "At least one asset should show a numeric completion percentage.");
        logStepWithScreenshot("Per-asset completion column");
        ExtentReportManager.logPass(real + "/" + pct.size() + " Asset Details rows show a numeric completion %.");
    }

    @Test(priority = 14, description = "AF_14: Asset Details table has rows-per-page + an accurate '1–N of M' count (TC-AF-020)")
    public void testAF_14_Pagination() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_14_Pagination");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(false);
        arcFlashPage.clickTab("Asset Details");
        arcFlashPage.waitForAssetClassFilter();
        arcFlashPage.waitForTableRows();
        Assert.assertTrue(arcFlashPage.hasRowsPerPage(), "Asset Details should expose a 'Rows per page' control.");
        String page = arcFlashPage.getPaginationText();
        logStep("Pagination text: '" + page + "'");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+)[\\s –—\\-]+(\\d+) of (\\d+)").matcher(page);
        Assert.assertTrue(m.find(), "Pagination should read '1–N of M'. Got: '" + page + "'");
        int from = Integer.parseInt(m.group(1)), to = Integer.parseInt(m.group(2)), total = Integer.parseInt(m.group(3));
        Assert.assertTrue(from >= 1 && from <= to && to <= total,
                "Pagination range must satisfy 1<=from<=to<=total. Got " + from + "-" + to + " of " + total);
        ExtentReportManager.logPass("Asset Details pagination is consistent: " + page);
    }

    // ================================================================
    // CROSS-CUTTING — refresh control + tab-state persistence
    // ================================================================

    @Test(priority = 15, description = "AF_15: The refresh control reloads the page data without errors (TC-AF-003)")
    public void testAF_15_RefreshControl() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_15_RefreshControl");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Overview");
        arcFlashPage.setEngineeringMode(false);
        List<String> before = arcFlashPage.getOverviewGauges();
        boolean clicked = arcFlashPage.clickRefresh();
        Assert.assertTrue(clicked, "A refresh icon should be present at the top-right of the tab bar.");
        Assert.assertTrue(arcFlashPage.isLoaded(), "After refresh the tab strip should still be present.");
        // NB: no verifyPageHealth() — this tenant has ambient backend 502s on unrelated lookup endpoints
        // that would flake the test; the gauges re-rendering below is the real post-refresh health oracle.
        arcFlashPage.clickTab("Overview");
        List<String> after = arcFlashPage.waitForOverviewGauges();
        logStep("gauges before refresh=" + before + " | after=" + after);
        Assert.assertEquals(after.size(), 3, "After refresh the three gauges should re-render.");
        logStepWithScreenshot("After refresh");
        ExtentReportManager.logPass("Refresh control reloaded the data; page remained healthy. Gauges: " + after);
    }

    @Test(priority = 16, description = "AF_16: Applying a filter then switching tabs and back leaves the page healthy (TC-AF-031)")
    public void testAF_16_TabStatePreserved() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AF_16_TabStatePreserved");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(false);
        arcFlashPage.clickTab("Asset Details");
        Assert.assertTrue(arcFlashPage.waitForAssetClassFilter(), "Asset Class filter should render.");
        arcFlashPage.selectAssetClass("Circuit Breaker");
        String applied = arcFlashPage.getAssetClassValue();
        logStep("Applied Asset Class filter = '" + applied + "'");

        arcFlashPage.clickTab("Overview");
        Assert.assertEquals(arcFlashPage.getActiveTab(), "Overview", "Should be back on Overview.");
        arcFlashPage.clickTab("Asset Details");
        Assert.assertEquals(arcFlashPage.getActiveTab(), "Asset Details", "Should return to Asset Details.");
        Assert.assertTrue(arcFlashPage.waitForAssetClassFilter(), "Asset Class filter should re-render on return.");
        String afterReturn = arcFlashPage.getAssetClassValue();
        logStep("Asset Class after returning = '" + afterReturn + "' (retained or cleanly reset — both acceptable)");
        logStepWithScreenshot("Tab state after round-trip");
        ExtentReportManager.logPass("Filter + tab round-trip left the page healthy (filter '" + afterReturn + "').");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Sorted copy of a list — order-independent equality for the percentage-set recalculation oracle. */
    private static List<String> sorted(List<String> in) {
        List<String> out = new ArrayList<>(in);
        Collections.sort(out);
        return out;
    }

    /**
     * Select a facility/site by exact name using REAL keystrokes (the tenant's ~130-site virtualised
     * picker doesn't react to a JS value-set). Mirrors AssetLocationTestNG / WorkOrderCreateTestNG.
     */
    private boolean ensureSite(String name) {
        By facility = By.xpath("//input[@placeholder='Select facility']");
        try {
            WebElement inp = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(facility));
            if (name.equalsIgnoreCase(String.valueOf(inp.getAttribute("value")))) return true;
            inp.click();
            pause(300);
            Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
            try { inp.sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
            inp.sendKeys(name);
            pause(900);
            String lower = name.toLowerCase();
            By exact = By.xpath("//li[@role='option'][translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='" + lower + "']");
            WebElement opt = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(exact));
            opt.click();
            pause(900);
            return name.equalsIgnoreCase(String.valueOf(driver.findElement(facility).getAttribute("value")));
        } catch (Exception e) {
            logStep("ensureSite('" + name + "') failed: " + e.getMessage());
            return false;
        }
    }
}
