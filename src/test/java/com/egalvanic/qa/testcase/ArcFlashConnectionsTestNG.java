package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.ArcFlashPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arc Flash Readiness — <b>Source/Target Connections</b> (Edit Asset modal) and <b>Connection Details</b>
 * (Edit Connection modal: Conductor Material + Length → Save).
 *
 * <p><b>Flow (verified live 2026-06-29 on site "Android Qa Site1"):</b></p>
 * <ul>
 *   <li><b>Source/Target Connections</b> tab → clicking an asset row opens an <b>Edit Asset</b> modal
 *       (Asset Name*, Asset Class*, Location, Asset Photos tabs Profile/Nameplate/Schedule/Arc Flash
 *       Label) → closed via the top-right <b>X</b>.</li>
 *   <li><b>Connection Details</b> tab → a <b>"Busway Arc Flash Readiness"</b> section + a Busway
 *       connection table (Type, Source, Target, Conductor Material, Length (ft), Neutral Wire Size,
 *       Amperage of Busway, Phase A/B/C Wire Size). Clicking a connection row opens an <b>Edit
 *       Connection</b> modal with BASIC INFO (Source/Target Node, Connection Type) + CORE ATTRIBUTES
 *       (Conductor Material* Autocomplete = Aluminum/Copper, Length (ft)* number, …). Setting Conductor
 *       Material + Length and clicking <b>Save Changes</b> persists (the modal closes).</li>
 * </ul>
 */
public class ArcFlashConnectionsTestNG extends BaseTest {

    private static final String MODULE = "Arc Flash Readiness";
    private static final String FEATURE = "Connections / Edit Connection";
    private static final String SITE = "Android Qa Site1";

    private ArcFlashPage arcFlashPage;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Arc Flash Readiness — Source/Target + Connection Details (Edit modals)");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        arcFlashPage = new ArcFlashPage(driver);
        boolean ok = ensureSite(SITE);
        System.out.println("[ArcFlashConn] site '" + SITE + "' selected=" + ok);
        // Warm the Arc Flash page + each tab's data ONCE, so no individual test eats the heavy cold
        // first-load (the Source/Target "Source Connection Status" computation is especially slow).
        try {
            arcFlashPage.navigateToArcFlash();
            arcFlashPage.setEngineeringMode(true);
            arcFlashPage.clickTab("Source/Target Connections"); arcFlashPage.waitForTableRows();
            arcFlashPage.clickTab("Connection Details"); arcFlashPage.waitForBuswayReadiness();
        } catch (Exception ignored) {}
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup(ITestResult result) {
        try { if (arcFlashPage.isAnyModalOpen()) arcFlashPage.closeModal(); } catch (Exception ignored) {}
    }

    // ================================================================
    // TESTS
    // ================================================================

    @Test(priority = 1, description = "AFC_01: Source/Target Connections — clicking an asset row opens the Edit Asset modal; close via X")
    public void testAFC_01_EditAssetModalFromSourceTarget() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_01_EditAssetModalFromSourceTarget");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Source/Target Connections");
        Assert.assertEquals(arcFlashPage.getActiveTab(), "Source/Target Connections", "Should be on Source/Target Connections.");

        Assert.assertTrue(arcFlashPage.openFirstAssetEdit(), "Clicking an asset row should open the Edit Asset modal.");
        Assert.assertTrue(arcFlashPage.editAssetHas("Asset Name"), "Edit Asset modal should show Asset Name.");
        Assert.assertTrue(arcFlashPage.editAssetHas("Asset Class"), "Edit Asset modal should show Asset Class.");
        Assert.assertTrue(arcFlashPage.editAssetHas("Location"), "Edit Asset modal should show Location.");
        Assert.assertTrue(arcFlashPage.editAssetHas("Profile"), "Edit Asset modal should show the Asset Photos tabs (Profile).");
        logStepWithScreenshot("Edit Asset modal");

        Assert.assertTrue(arcFlashPage.closeModal(), "The Edit Asset modal should close via the X button.");
        Assert.assertFalse(arcFlashPage.isEditAssetModalOpen(), "Edit Asset modal should be closed.");
        ExtentReportManager.logPass("Source/Target Connections → asset row opens Edit Asset (Name/Class/Location/Photos), closed via X.");
    }

    @Test(priority = 2, description = "AFC_02: Connection Details — Busway Arc Flash Readiness section + connection table columns")
    public void testAFC_02_ConnectionDetailsBuswayReadiness() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_02_ConnectionDetailsBuswayReadiness");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Connection Details");
        Assert.assertEquals(arcFlashPage.getActiveTab(), "Connection Details", "Should be on Connection Details.");

        Assert.assertTrue(arcFlashPage.waitForBuswayReadiness(),
                "Connection Details should show a 'Busway Arc Flash Readiness' section (waited for it to render).");
        for (String col : new String[]{"Type", "Source", "Target", "Conductor Material", "Length (ft)"}) {
            Assert.assertTrue(arcFlashPage.hasColumn(col), "Connection table should have a '" + col + "' column.");
        }
        logStep("Connection columns: " + arcFlashPage.getColumnHeaders());
        logStepWithScreenshot("Connection Details — Busway readiness");
        ExtentReportManager.logPass("Connection Details shows Busway Arc Flash Readiness + connection columns (Type/Source/Target/Conductor Material/Length).");
    }

    @Test(priority = 3, description = "AFC_03: Connection Details — clicking a connection row opens the Edit Connection modal (BASIC INFO + CORE ATTRIBUTES)")
    public void testAFC_03_EditConnectionModalStructure() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_03_EditConnectionModalStructure");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Connection Details");
        Assert.assertTrue(arcFlashPage.openFirstConnectionEdit(), "Clicking a connection row should open the Edit Connection modal.");

        Assert.assertTrue(arcFlashPage.editConnectionHas("Source Node"), "Edit Connection should show Source Node (BASIC INFO).");
        Assert.assertTrue(arcFlashPage.editConnectionHas("Connection Type"), "Edit Connection should show Connection Type.");
        Assert.assertTrue(arcFlashPage.editConnectionHas("Conductor Material"), "Edit Connection should show Conductor Material (CORE ATTRIBUTES).");
        Assert.assertTrue(arcFlashPage.editConnectionHas("Length (ft)"), "Edit Connection should show Length (ft).");
        logStepWithScreenshot("Edit Connection modal");

        Assert.assertTrue(arcFlashPage.closeModal(), "Edit Connection modal should close via X.");
        ExtentReportManager.logPass("Connection row opens Edit Connection with BASIC INFO (Source/Target/Type) + CORE ATTRIBUTES (Conductor Material, Length).");
    }

    @Test(priority = 4, description = "AFC_04: Edit Connection — set Conductor Material + Length and Save Changes (persists)")
    public void testAFC_04_SaveConnectionCoreAttributes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_04_SaveConnectionCoreAttributes");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Connection Details");
        Assert.assertTrue(arcFlashPage.openFirstConnectionEdit(), "Should open the Edit Connection modal.");

        arcFlashPage.selectConductorMaterial("Copper");                 // step 1-2
        Assert.assertEquals(arcFlashPage.getConductorMaterialValue(), "Copper", "Conductor Material should read 'Copper'.");
        arcFlashPage.enterLength("50");                                 // step 3-4
        Assert.assertEquals(arcFlashPage.getLengthValue(), "50", "Length (ft) should read '50'.");
        Assert.assertTrue(arcFlashPage.isSaveChangesEnabled(), "Save Changes should be enabled.");
        logStepWithScreenshot("Edit Connection — Conductor Material=Copper, Length=50");

        boolean saved = arcFlashPage.saveChanges();                     // Save Changes
        Assert.assertTrue(saved, "Save Changes should persist and close the Edit Connection modal.");
        logStepWithScreenshot("After Save Changes");
        ExtentReportManager.logPass("Edit Connection: set Conductor Material=Copper + Length=50, Save Changes persisted (modal closed).");
    }

    // ================================================================
    // SOURCE/TARGET analytics — summary banner, columns, status, search, sort
    // ================================================================

    @Test(priority = 5, description = "AFC_05: Source/Target summary banner is internally consistent (TC-AF-021/023)")
    public void testAFC_05_SourceSummaryReconciles() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_05_SourceSummaryReconciles");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Source/Target Connections");
        arcFlashPage.waitForTableRows();

        String summary = arcFlashPage.getSourceConnectionSummary();
        logStep("Source Connection Status banner: '" + summary + "'");
        Assert.assertFalse(summary.isEmpty(), "Source/Target tab should show a 'require source … connected … missing' banner.");

        // "113 assets require source • 21 connected (21 direct, 0 indirect) • 92 missing"
        Matcher m = Pattern.compile(
                "(\\d+)\\s+assets?\\s+require source.*?(\\d+)\\s+connected\\s*\\((\\d+)\\s+direct,\\s*(\\d+)\\s+indirect\\).*?(\\d+)\\s+missing",
                Pattern.CASE_INSENSITIVE).matcher(summary);
        Assert.assertTrue(m.find(), "Banner should match the require/connected(direct,indirect)/missing shape: '" + summary + "'");
        int require = Integer.parseInt(m.group(1)), connected = Integer.parseInt(m.group(2));
        int direct = Integer.parseInt(m.group(3)), indirect = Integer.parseInt(m.group(4)), missing = Integer.parseInt(m.group(5));
        logStep(String.format("require=%d connected=%d (direct=%d indirect=%d) missing=%d", require, connected, direct, indirect, missing));
        Assert.assertEquals(direct + indirect, connected,
                "direct + indirect should equal the connected total.");
        Assert.assertEquals(connected + missing, require,
                "connected + missing should equal the assets-requiring-source total.");

        String pct = arcFlashPage.getSourceCompletePercent();
        logStep("Source completion = " + pct);
        Assert.assertTrue(pct.matches("\\d{1,3}%"), "A completion percentage should accompany the banner. Got: '" + pct + "'");
        logStepWithScreenshot("Source/Target summary reconciled");
        ExtentReportManager.logPass("Source/Target summary reconciles: " + connected + " connected (" + direct + "+" + indirect
                + ") + " + missing + " missing = " + require + " require; " + pct + " complete.");
    }

    @Test(priority = 6, description = "AFC_06: Source/Target table columns + Status values are valid (TC-AF-022/023)")
    public void testAFC_06_SourceColumnsAndStatus() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_06_SourceColumnsAndStatus");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Source/Target Connections");
        arcFlashPage.waitForTableRows();

        for (String col : new String[]{"Asset", "Asset Class", "Needs Source", "Direct Source", "Indirect Source", "Status"}) {
            Assert.assertTrue(arcFlashPage.hasColumn(col), "Source/Target table should have a '" + col + "' column.");
        }
        List<String> statuses = arcFlashPage.getColumnValues("Status");
        logStep("Status values (" + statuses.size() + "): first 10 = " + statuses.subList(0, Math.min(10, statuses.size())));
        Assert.assertFalse(statuses.isEmpty(), "Status column should have values.");
        int connected = 0, missing = 0;
        for (String s : statuses) {
            Assert.assertTrue(s.isEmpty() || s.equalsIgnoreCase("Connected") || s.equalsIgnoreCase("Missing"),
                    "Status should be Connected/Missing (or empty): '" + s + "'");
            if (s.equalsIgnoreCase("Connected")) connected++;
            else if (s.equalsIgnoreCase("Missing")) missing++;
        }
        Assert.assertTrue(connected + missing > 0, "There should be at least one Connected/Missing status.");
        logStepWithScreenshot("Source/Target columns + status");
        ExtentReportManager.logPass("Source/Target columns present; Status values valid (" + connected + " connected, " + missing + " missing in view).");
    }

    @Test(priority = 7, description = "AFC_07: 'Search items…' filters the Source/Target table live (TC-AF-024)")
    public void testAFC_07_SearchFilters() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_07_SearchFilters");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Source/Target Connections");
        arcFlashPage.waitForTableRows();
        Assert.assertTrue(arcFlashPage.hasSearchBox(), "Source/Target tab should have a 'Search items…' box.");

        List<String> assetsBefore = arcFlashPage.getColumnValues("Asset");
        Assert.assertFalse(assetsBefore.isEmpty(), "There should be assets to search.");
        String term = assetsBefore.get(0);
        logStep("Searching for first asset name: '" + term + "' (rows before = " + assetsBefore.size() + ")");

        arcFlashPage.searchItems(term);
        List<String> assetsAfter = arcFlashPage.getColumnValues("Asset");
        logStep("Rows after search = " + assetsAfter.size() + " : " + assetsAfter.subList(0, Math.min(8, assetsAfter.size())));
        Assert.assertFalse(assetsAfter.isEmpty(), "The searched asset should still be visible.");
        Assert.assertTrue(assetsAfter.size() <= assetsBefore.size(), "Search should not increase the row count.");
        boolean termVisible = false;
        for (String a : assetsAfter) if (a.toLowerCase().contains(term.toLowerCase())) { termVisible = true; break; }
        Assert.assertTrue(termVisible, "At least one visible row should contain the search term '" + term + "'. Got: " + assetsAfter);
        logStepWithScreenshot("Filtered by search '" + term + "'");

        arcFlashPage.clearSearch();
        List<String> assetsCleared = arcFlashPage.getColumnValues("Asset");
        logStep("Rows after clearing search = " + assetsCleared.size());
        Assert.assertTrue(assetsCleared.size() >= assetsAfter.size(), "Clearing the search should restore rows.");
        ExtentReportManager.logPass("Search '" + term + "' filtered " + assetsBefore.size() + " → " + assetsAfter.size()
                + " rows; cleared back to " + assetsCleared.size() + ".");
    }

    @Test(priority = 8, description = "AFC_08: Sorting by Status reorders the Source/Target table (TC-AF-025)")
    public void testAFC_08_SortByStatus() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_08_SortByStatus");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Source/Target Connections");
        arcFlashPage.waitForTableRows();

        String state0 = arcFlashPage.getColumnSortState("Status");
        List<String> order0 = arcFlashPage.getColumnValues("Status");
        logStep("Status sort before = '" + state0 + "', first values = " + order0.subList(0, Math.min(6, order0.size())));

        String state1 = arcFlashPage.sortByColumn("Status");
        List<String> order1 = arcFlashPage.getColumnValues("Status");
        logStep("Status sort after click = '" + state1 + "', first values = " + order1.subList(0, Math.min(6, order1.size())));

        Assert.assertFalse(state1.isEmpty(), "After clicking the Status header it should carry an aria-sort state.");
        boolean stateToggled = !state1.equals(state0);
        boolean orderChanged = !order1.equals(order0);
        Assert.assertTrue(stateToggled || orderChanged,
                "Clicking the Status header should change the sort direction or row order (state " + state0 + "→" + state1 + ").");
        logStepWithScreenshot("Sorted by Status");
        ExtentReportManager.logPass("Status sort responded to the header click: state " + state0 + " → " + state1
                + (orderChanged ? " (row order changed)" : ""));
    }

    // ================================================================
    // CONNECTION DETAILS — Connection Type filter
    // ================================================================

    @Test(priority = 9, description = "AFC_09: Connection Type filter lists types and applies (TC-AF-027)")
    public void testAFC_09_ConnectionTypeFilter() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFC_09_ConnectionTypeFilter");
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.setEngineeringMode(true);
        arcFlashPage.clickTab("Connection Details");
        Assert.assertTrue(arcFlashPage.waitForBuswayReadiness(), "Connection Details should render.");
        arcFlashPage.waitForTableRows();

        Assert.assertTrue(arcFlashPage.hasConnectionTypeFilter(), "Connection Details should have a Connection Type filter.");
        String current = arcFlashPage.getConnectionTypeValue();
        List<String> options = arcFlashPage.getConnectionTypeOptions();
        logStep("Connection Type = '" + current + "', options = " + options);
        Assert.assertFalse(options.isEmpty(), "Connection Type filter should list options.");
        Assert.assertTrue(options.contains(current) || current.isEmpty(),
                "The current Connection Type '" + current + "' should be among the options " + options);

        // If there is more than one type, switch to a different one and confirm the filter applied.
        String other = null;
        for (String o : options) if (!o.equals(current)) { other = o; break; }
        if (other != null) {
            arcFlashPage.selectConnectionType(other);
            Assert.assertEquals(arcFlashPage.getConnectionTypeValue(), other,
                    "Connection Type filter should now read '" + other + "'.");
            logStep("Switched Connection Type to '" + other + "'");
            arcFlashPage.selectConnectionType(current.isEmpty() ? other : current); // restore
        } else {
            logStep("Only one connection type ('" + current + "') exists for this site — presence + options verified.");
        }
        logStepWithScreenshot("Connection Type filter");
        ExtentReportManager.logPass("Connection Type filter present with options " + options + " (current '" + current + "').");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Select a facility/site by exact name using REAL keystrokes (the ~130-site virtualised picker
     *  ignores JS value-sets). Mirrors the other Arc Flash / asset / work-order tests. */
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
