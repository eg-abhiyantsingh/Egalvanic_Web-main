package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
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

/**
 * Asset creation — <b>"Select or Create Location"</b> modal (Building › Floor › Room cascade).
 *
 * <p><b>Flow (verified live 2026-06-25 on site "Android Qa Site1"):</b> Assets → <b>Create Asset</b>
 * opens the Add Asset drawer. The BASIC INFO section has Asset Name*, QR Code, Asset Class*, and a
 * <b>Location</b> area whose <b>Select Location</b> button opens a MUI dialog titled
 * <b>"Select or Create Location"</b>. The dialog holds three MUI {@code <Select>} dropdowns in a strict
 * cascade — <b>Building → Floor → Room</b> (Floor is disabled until a Building is chosen, Room until a
 * Floor is chosen) — each with a <b>New Building/Floor/Room</b> create button, a
 * <b>"Selected Location: B›F›R"</b> preview, and a confirm button (also labelled <b>Select Location</b>)
 * that stays <b>disabled</b> until all three are picked. Confirming closes the dialog and the drawer's
 * Location area then shows the chosen path (e.g. {@code B1›F1›R1}) plus a <b>Change</b> button.</p>
 *
 * <p>Covers: modal structure, the cascade enable/disable gating, the confirm-button gating, real
 * building options (incl. B1), the full select-and-apply, cancel-leaves-unset, and an end-to-end
 * create-with-location that persists to the grid. {@code testLOC_00_DumpModal} is a disabled
 * DOM-discovery diagnostic.</p>
 */
public class AssetLocationTestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_ASSET;
    private static final String FEATURE = "Create Asset — Location";
    /** A location-rich site verified to have a complete Building › Floor › Room chain (B1 › F1 › R1). */
    private static final String SITE = "Android Qa Site1";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Indices of the three cascade dropdowns inside the modal.
    private static final int BUILDING = 0, FLOOR = 1, ROOM = 2;


    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Asset — Select or Create Location (Building > Floor > Room)");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        // Scope the whole class to a site with a verified complete Building>Floor>Room chain.
        // NOTE: BaseTest.selectSiteByName types via a JS value-set, which does NOT filter this
        // tenant's 130-site (virtualised) facility list — so it can't reach this site. ensureSite()
        // below uses REAL keystrokes, which do filter the list down to the exact match.
        boolean ok = ensureSite(SITE);
        System.out.println("[AssetLocation] site '" + SITE + "' selected=" + ok);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup(ITestResult result) {
        try { if (assetPage.isLocationModalOpen()) assetPage.cancelLocationModal(); } catch (Exception ignored) {}
    }

    // ================================================================
    // TESTS
    // ================================================================

    @Test(priority = 1, description = "LOC_01: Select Location opens the 'Select or Create Location' modal with the Building/Floor/Room cascade + New* + confirm")
    public void testLOC_01_LocationModalStructure() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LOC_01_LocationModalStructure");
        openFreshCreateForm();
        assetPage.openLocationModal();

        Assert.assertTrue(assetPage.isLocationModalOpen(),
                "Clicking 'Select Location' should open the 'Select or Create Location' modal.");
        // The three cascade fields (their labels), the Building create affordance, and Cancel.
        // NOTE: only 'New Building' is present on a fresh modal — 'New Floor'/'New Room' are
        // contextual and render after a Building/Floor is chosen (verified live).
        Assert.assertTrue(modalHas("Building"), "Modal should have a Building field.");
        Assert.assertTrue(modalHas("Floor"), "Modal should have a Floor field.");
        Assert.assertTrue(modalHas("Room"), "Modal should have a Room field.");
        Assert.assertTrue(modalHasButton("New Building"), "Modal should offer 'New Building' (the create affordance).");
        Assert.assertTrue(modalHasButton("Cancel"), "Modal should have a Cancel button.");
        logStepWithScreenshot("Select or Create Location modal");
        ExtentReportManager.logPass("Location modal opens with the Building/Floor/Room cascade fields, New Building and Cancel.");
    }

    @Test(priority = 2, description = "LOC_02: Cascade gating — Floor disabled until Building chosen, Room until Floor; confirm disabled until all three")
    public void testLOC_02_CascadeGating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LOC_02_CascadeGating");
        openFreshCreateForm();
        assetPage.openLocationModal();

        // On open: only Building is enabled; confirm is disabled.
        Assert.assertTrue(assetPage.isLocationFieldEnabled(BUILDING), "Building should be enabled on open.");
        Assert.assertFalse(assetPage.isLocationFieldEnabled(FLOOR), "Floor should be DISABLED until a Building is chosen.");
        Assert.assertFalse(assetPage.isLocationFieldEnabled(ROOM), "Room should be DISABLED until a Floor is chosen.");
        Assert.assertFalse(assetPage.isLocationConfirmEnabled(), "Confirm should be DISABLED with nothing selected.");

        // Use the verified-complete B1 › F1 › R1 chain (the first-listed building on this site has a
        // floor with no rooms, which is fine data but useless for exercising the full cascade).
        // Choose Building → Floor enables, Room still disabled, confirm still disabled.
        assetPage.chooseLocationOption(BUILDING, "B1");
        logStep("Picked building: B1");
        Assert.assertTrue(assetPage.isLocationFieldEnabled(FLOOR), "Floor should ENABLE once a Building is chosen.");
        Assert.assertFalse(assetPage.isLocationFieldEnabled(ROOM), "Room should still be disabled (no Floor yet).");
        Assert.assertFalse(assetPage.isLocationConfirmEnabled(), "Confirm should still be disabled (only Building chosen).");

        // Choose Floor → Room enables, confirm still disabled.
        assetPage.chooseLocationOption(FLOOR, "F1");
        logStep("Picked floor: F1");
        Assert.assertTrue(assetPage.isLocationFieldEnabled(ROOM), "Room should ENABLE once a Floor is chosen.");
        Assert.assertFalse(assetPage.isLocationConfirmEnabled(), "Confirm should still be disabled (no Room yet).");

        // Choose Room → confirm finally enables.
        assetPage.chooseLocationOption(ROOM, "R1");
        logStep("Picked room: R1");
        Assert.assertTrue(assetPage.isLocationConfirmEnabled(),
                "Confirm ('Select Location') should ENABLE only once Building+Floor+Room are all chosen.");
        logStepWithScreenshot("Cascade complete: B1 > F1 > R1");
        ExtentReportManager.logPass("Cascade gating verified: Building→Floor→Room enable in order; confirm gated on all three.");
    }

    @Test(priority = 3, description = "LOC_03: Building dropdown lists real buildings (incl. B1) on the scoped site")
    public void testLOC_03_BuildingOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LOC_03_BuildingOptions");
        openFreshCreateForm();
        assetPage.openLocationModal();

        List<String> buildings = assetPage.getLocationDropdownOptions(BUILDING);
        logStep("Building options (" + buildings.size() + "): " + buildings);
        Assert.assertFalse(buildings.isEmpty(), "Building dropdown should list at least one real building on '" + SITE + "'.");
        Assert.assertTrue(buildings.contains("B1"),
                "Building dropdown should include 'B1' on '" + SITE + "' (verified test data). Got: " + buildings);
        ExtentReportManager.logPass("Building dropdown lists real buildings including B1 (" + buildings.size() + " total).");
    }

    @Test(priority = 4, description = "LOC_04: Selecting Building→Floor→Room and confirming applies the location to the drawer")
    public void testLOC_04_SelectAndApplyLocation() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LOC_04_SelectAndApplyLocation");
        openFreshCreateForm();

        // Prefer the verified B1 › F1 › R1 chain; fall back to first-available if data shifts.
        String applied;
        try {
            assetPage.selectLocation("B1", "F1", "R1");
            applied = "B1 › F1 › R1";
        } catch (Exception e) {
            logStep("Explicit B1/F1/R1 not selectable (" + e.getMessage() + ") — using first-available chain");
            applied = assetPage.selectFirstAvailableLocation();
        }
        Assert.assertNotNull(applied, "A complete location chain should be selectable on '" + SITE + "'.");

        Assert.assertFalse(assetPage.isLocationModalOpen(), "Confirming should close the location modal.");
        String shown = assetPage.getSelectedLocationText();
        logStep("Drawer Location area now reads: '" + shown + "'");
        Assert.assertTrue(assetPage.isLocationSet(),
                "The drawer's Location should reflect the chosen location (not 'Select Location'). Got: '" + shown + "'");
        // The applied building/floor/room names should appear in the drawer's location text.
        for (String part : applied.replace("›", " ").split("\\s+")) {
            if (part.isEmpty()) continue;
            Assert.assertTrue(shown.contains(part),
                    "Drawer Location '" + shown + "' should contain '" + part + "'.");
        }
        logStepWithScreenshot("Location applied to drawer");
        ExtentReportManager.logPass("Location " + applied + " selected via the modal and applied to the Add Asset drawer.");
    }

    @Test(priority = 5, description = "LOC_05: Cancelling the location modal leaves the asset Location unset")
    public void testLOC_05_CancelLeavesLocationUnset() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LOC_05_CancelLeavesLocationUnset");
        openFreshCreateForm();
        assetPage.openLocationModal();
        Assert.assertTrue(assetPage.isLocationModalOpen(), "Modal should be open before cancel.");

        // Pick a building only, then Cancel — nothing should be applied.
        assetPage.pickFirstLocationOption(BUILDING);
        assetPage.cancelLocationModal();

        Assert.assertFalse(assetPage.isLocationModalOpen(), "Cancel should close the modal.");
        Assert.assertFalse(assetPage.isLocationSet(),
                "After Cancel the drawer Location should still be unset. Got: '" + assetPage.getSelectedLocationText() + "'");
        ExtentReportManager.logPass("Cancelling the location modal discards the selection (Location stays unset).");
    }

    @Test(priority = 6, description = "LOC_06: End-to-end — create an asset WITH a location and verify it persists to the grid")
    public void testLOC_06_CreateAssetWithLocation() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LOC_06_CreateAssetWithLocation");
        String name = "LocTest_" + LocalDateTime.now().format(TS);
        logStep("Creating asset '" + name + "' with a Building › Floor › Room location");

        openFreshCreateForm();
        assetPage.fillBasicInfo(name, "QR-" + System.currentTimeMillis(), "Circuit Breaker");
        logStep("Basic info filled (name + QR + class)");

        // Apply a location via the modal (the workflow under test).
        String applied;
        try {
            assetPage.selectLocation("B1", "F1", "R1");
            applied = "B1 › F1 › R1";
        } catch (Exception e) {
            applied = assetPage.selectFirstAvailableLocation();
        }
        Assert.assertNotNull(applied, "A location should be selectable for the new asset.");
        Assert.assertTrue(assetPage.isLocationSet(), "Location should be applied to the drawer before submit.");
        logStep("Location applied: " + applied);

        assetPage.fillCoreAttributes();
        assetPage.fillReplacementCost("25000");
        logStepWithScreenshot("Before submitting create (with location)");

        assetPage.submitCreateAsset();
        boolean success = assetPage.waitForCreateSuccess();
        logStep("Create success signal: " + success);
        pause(2000);

        // Real persistence check: the asset must be findable in the grid. We search with REAL
        // keystrokes — the asset grid's search is a debounced server-side filter that a JS value-set
        // (AssetPage.searchAsset) does NOT trigger, so it would check an unfiltered grid and miss the row.
        boolean visible = searchGridFor(name);
        logStepWithScreenshot("After create — searched for '" + name + "'");
        Assert.assertTrue(visible,
                "Newly created asset '" + name + "' (with location " + applied + ") should appear in the grid"
                + " (create success signal was " + success + ").");
        ExtentReportManager.logPass("Created asset '" + name + "' with location " + applied + " — persisted and found in grid.");
    }

    // ================================================================
    // DIAGNOSTIC (disabled) — DOM dump of the location modal
    // ================================================================

    @Test(priority = 0, enabled = false, description = "DIAG: dump the Select-or-Create-Location modal DOM")
    public void testLOC_00_DumpModal() {
        ExtentReportManager.createTest(MODULE, FEATURE, "LOC_00_DumpModal");
        openFreshCreateForm();
        assetPage.openLocationModal();
        StringBuilder sb = new StringBuilder();
        sb.append("modalOpen=").append(assetPage.isLocationModalOpen()).append("\n");
        sb.append("buildingEnabled=").append(assetPage.isLocationFieldEnabled(BUILDING)).append("\n");
        sb.append("floorEnabled=").append(assetPage.isLocationFieldEnabled(FLOOR)).append("\n");
        sb.append("roomEnabled=").append(assetPage.isLocationFieldEnabled(ROOM)).append("\n");
        sb.append("confirmEnabled=").append(assetPage.isLocationConfirmEnabled()).append("\n");
        sb.append("buildingOptions=").append(assetPage.getLocationDropdownOptions(BUILDING)).append("\n");
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/asset-location-diag.txt"),
                    sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) { logStep("diag write failed: " + e.getMessage()); }
        logStep(sb.toString());
        ExtentReportManager.logPass("Location modal diagnostic dumped.");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Give every test a guaranteed-clean Add Asset drawer. We ALWAYS re-navigate to Assets first
     * (the away-and-back inside navigateToAssets tears down any drawer a prior test left open, so a
     * previously-applied location can't carry over), then open the create form and confirm the drawer
     * actually rendered (its Asset Name field is showing) — retrying the open if it didn't, because
     * openCreateAssetForm's success check also matches the always-present toolbar "Create Asset" text.
     */
    private void openFreshCreateForm() {
        assetPage.navigateToAssets();
        for (int attempt = 1; attempt <= 3; attempt++) {
            assetPage.openCreateAssetForm();
            pause(800);
            if (assetPage.isCreateDrawerOpen()) return;
            logStep("Add Asset drawer did not open (attempt " + attempt + ") — retrying");
            pause(800);
        }
        Assert.assertTrue(assetPage.isCreateDrawerOpen(),
                "Add Asset create drawer should open (Asset Name field visible).");
    }

    /**
     * Select a facility/site by exact name using REAL keystrokes. The tenant has ~130 sites in a
     * virtualised MUI Autocomplete; a JS value-set (BaseTest.selectSiteByName) does not trigger the
     * filter, so the target is never rendered. Real {@code sendKeys} filters the list to the exact
     * match (verified: typing the full name narrows it to a single option).
     */
    private boolean ensureSite(String name) {
        By facility = By.xpath("//input[@placeholder='Select facility']");
        try {
            WebElement inp = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(facility));
            if (name.equalsIgnoreCase(String.valueOf(inp.getAttribute("value")))) return true;
            inp.click();
            pause(300);
            // Clear any residual text cross-platform, then type to filter.
            Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac")
                    ? Keys.COMMAND : Keys.CONTROL;
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

    /**
     * Search the asset grid for {name} using REAL keystrokes and return whether a matching row
     * appears. The grid's "Search Assets..." box is a debounced server-side filter that only reacts
     * to genuine key events — a JS value-set leaves the grid unfiltered. Retries to ride out the
     * debounce + server round-trip.
     */
    private boolean searchGridFor(String name) {
        By search = By.xpath("//input[contains(@placeholder,'Search Assets')]");
        By matchRow = By.xpath("//div[@role='row'][contains(normalize-space(),'" + name + "')]");
        Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac")
                ? Keys.COMMAND : Keys.CONTROL;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement box = new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(search));
                box.click();
                try { box.sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
                box.sendKeys(name);
                pause(2800); // debounce + server filter
                if (!driver.findElements(matchRow).isEmpty()) {
                    logStep("Persistence check (real-keystroke search) attempt " + attempt + ": FOUND");
                    return true;
                }
                logStep("Persistence check attempt " + attempt + ": not yet visible");
            } catch (Exception e) {
                logStep("Persistence search attempt " + attempt + " error: " + e.getMessage());
            }
            pause(1200);
        }
        return false;
    }

    /** True if the open location modal contains the given text anywhere. */
    private boolean modalHas(String text) {
        return !driver.findElements(By.xpath(
                "//div[@role='dialog'][.//*[normalize-space()='Select or Create Location']]"
                + "//*[contains(normalize-space(),'" + text + "')]")).isEmpty();
    }

    /** True if the open location modal has a button with the given exact text. */
    private boolean modalHasButton(String text) {
        return !driver.findElements(By.xpath(
                "//div[@role='dialog'][.//*[normalize-space()='Select or Create Location']]"
                + "//button[normalize-space()='" + text + "']")).isEmpty();
    }
}
