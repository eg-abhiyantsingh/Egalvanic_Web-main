package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.constants.WorkTypeCatalog;
import com.egalvanic.qa.constants.WorkTypeCatalog.WorkTypeProfile;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Work Orders — <b>Work Type Auto-Schedule &amp; Edge</b> matrix (ZP-3000 Services V2, 2026-07-21).
 *
 * <p>34 data-driven cases on the Create New Work Order dialog (site Z1) covering the edges the
 * per-family happy-path suites do not touch:</p>
 * <ol>
 *   <li><b>Auto-Schedule enablement</b> across all 14 work types (Est. Hours + Field Technician
 *       preconditions) — accumulates a truth table so a single failure prints everything observed.</li>
 *   <li><b>Shutdown (Composite)</b> — the only 0-procedure service: no-procedures notice pinned,
 *       create succeeds with 0 assets, API cleanup verified.</li>
 *   <li><b>General</b> — no-service WO: PINS the previously-unknown detail-page contract
 *       (tabs / chips) by recording the observed lists inside the assertion messages.</li>
 *   <li>Cancel discards, reopen resets state, name edge cases (256-char / XSS / unicode /
 *       whitespace-only), facility change re-fires the scope preview, duplicate-name behavior
 *       pin, past due date behavior pin.</li>
 * </ol>
 *
 * <p>Non-destructive: every created WO is deleted via DELETE /api/ir_session/{id}
 * (async receipt) and verified gone from the grid; leftovers are swept in @AfterClass.</p>
 */
public class WorkTypeAutoScheduleEdgeTestNG extends WorkTypeUiBase {

    private static final String FEATURE = "Work Type Auto-Schedule & Edge";

    /** Accumulated Auto-Schedule enablement truth table (work type -> enabled after preconditions). */
    private static final Map<String, Boolean> AUTO_SCHEDULE_TRUTH = new LinkedHashMap<>();

    /** ir_session ids created by this class and not yet confirmed deleted (swept in @AfterClass). */
    private final Set<String> createdIds = new LinkedHashSet<>();

    // Sequential-flow shared state (priority-ordered tests within one class/browser session)
    private String shutdownWoName;
    private String shutdownSessionId;
    private String generalWoName;
    private String generalSessionId;
    private List<String> generalTabs;
    private List<String> generalChips;

    // ================================================================
    // DATA PROVIDERS (built from WorkTypeCatalog)
    // ================================================================

    @DataProvider(name = "allWorkTypes")
    public Object[][] allWorkTypes() {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (WorkTypeProfile p : WorkTypeCatalog.ALL) rows.add(new Object[]{p});
        return rows.toArray(new Object[0][]);
    }

    /** One service (Infrared Thermography) + General — the cheap representative pair. */
    @DataProvider(name = "servicePlusGeneral")
    public Object[][] servicePlusGeneral() {
        List<Object[]> rows = new ArrayList<Object[]>();
        rows.add(new Object[]{WorkTypeCatalog.byName("Infrared Thermography")});
        rows.add(new Object[]{WorkTypeCatalog.GENERAL});
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "nameEdges")
    public Object[][] nameEdges() {
        // whitespace-only moved to its own known-product-bug tripwire (TC_WTE_011b) —
        // live-verified 2026-07-22: Create enables AND the backend accepts a whitespace-only
        // name, producing a blank-named WO in the list.
        return new Object[][]{
                {"max-length-256"},
                {"xss-script-tag"},
                {"unicode-emoji-quotes"}
        };
    }

    @DataProvider(name = "facilityFlips")
    public Object[][] facilityFlips() {
        return new Object[][]{
                {"Z1 -> Test Site"},
                {"Z1 -> Test Site -> back to Z1"}
        };
    }

    @DataProvider(name = "dupRuns")
    public Object[][] dupRuns() {
        return new Object[][]{{"run-1"}, {"run-2"}};
    }

    // ================================================================
    // 1) AUTO-SCHEDULE ENABLEMENT — all 14 work types (fresh dialog per row)
    // ================================================================

    @Test(priority = 1, dataProvider = "allWorkTypes",
          description = "TC_WTE_001: Auto-Schedule not enabled by Est Hours + Technician alone")
    public void testTC_WTE_001_AutoScheduleEnablement(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_001 — " + profile.name);
        // LIVE-VERIFIED 2026-07-22 (full 14-type truth table, fresh dialog per type, site Z1):
        // the top-level Schedule-section Auto-Schedule button renders for EVERY work type but
        // stays DISABLED after setting WO Name + Est. Hours + Field Technician — for all 13
        // services (matching-asset scope 1..173) AND General alike. Enabling it requires opening
        // the Schedule "+" block sub-flow, which is a separate behavior (not pinned here to keep
        // this matrix reproducible). So this row pins the reproducible negative: those two inputs
        // are NOT sufficient. If a type ever enables here, the truth table in the message shows it.
        boolean opened = freshCreateDialog();
        boolean committed = opened && selectWorkTypeCommitted(profile.name);
        boolean techFilled = false;
        boolean present = false;
        boolean enabled = false;
        if (committed) {
            workOrderPage.fillWoName("WTE_AS_" + System.currentTimeMillis());
            workOrderPage.fillEstHours("8");
            techFilled = fillEmptyFieldTechnician();
            pause(2000);
            present = workOrderPage.isAutoScheduleButtonPresent();
            enabled = workOrderPage.isAutoScheduleButtonEnabled();
        }
        AUTO_SCHEDULE_TRUTH.put(profile.name, Boolean.valueOf(enabled));
        logStep("Auto-Schedule for '" + profile.name + "': present=" + present + ", enabledAfterEstHours+Tech="
                + enabled + " (techFilled=" + techFilled + ", deEnergized=" + profile.deEnergized + ")");
        workOrderPage.cancelCreateDialog();   // NEVER create in this matrix

        Assert.assertTrue(opened, "Create dialog should open for '" + profile.name + "'.");
        Assert.assertTrue(committed, "Work type '" + profile.name + "' should commit in the Autocomplete.");
        Assert.assertTrue(present,
                "Auto-Schedule button must RENDER for '" + profile.name + "' (it renders for all 14 types).");
        Assert.assertTrue(techFilled,
                "Precondition: a Field Technician must be selectable/committed for '" + profile.name + "'.");
        Assert.assertFalse(enabled,
                "Auto-Schedule must stay DISABLED for '" + profile.name + "' with only WO Name + Est. Hours + "
                + "Field Technician set — those are NOT sufficient (enabling needs the Schedule block sub-flow). "
                + "OBSERVED TRUTH TABLE SO FAR: " + AUTO_SCHEDULE_TRUTH);
    }

    // ================================================================
    // 2) SHUTDOWN (COMPOSITE) — the 0-procedure service (3 sequential TCs)
    // ================================================================

    @Test(priority = 2,
          description = "TC_WTE_002: Shutdown shows the no-procedures notice")
    public void testTC_WTE_002_ShutdownNoProceduresNoticePinned() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_002 — Shutdown (Composite) notice pinned");
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open.");
        boolean committed = selectWorkTypeCommitted("Shutdown (Composite)");
        Assert.assertTrue(committed, "'Shutdown (Composite)' should commit.");
        boolean notice = false;
        for (int i = 0; i < 20 && !notice; i++) {   // notice can render just after selection settles
            notice = workOrderPage.hasNoProceduresNotice();
            if (!notice) pause(500);
        }
        int count = workOrderPage.getMatchingAssetsCount(3);
        logStep("Shutdown (Composite): noProceduresNotice=" + notice + ", matchingAssetsCount=" + count
                + " — dialog text head: " + head(workOrderPage.getCreateDialogText(), 300));
        Assert.assertTrue(notice,
                "Shutdown (Composite) (0 procedures) must show the 'no procedures configured' notice. Dialog text: "
                + head(workOrderPage.getCreateDialogText(), 500));
        Assert.assertEquals(count, -1,
                "Shutdown (Composite) must NOT render an 'N matching assets' count (observed " + count + ").");
        // dialog intentionally left open — TC_WTE_003 continues in the same dialog
    }

    @Test(priority = 3,
          description = "TC_WTE_003: Shutdown WO creates with 0 assets")
    public void testTC_WTE_003_ShutdownZeroProcedureCreateSucceeds() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_003 — Shutdown (Composite) create with 0 assets");
        boolean opened = ensureCreateDialogOpen();
        Assert.assertTrue(opened, "Create dialog should be open (reused from TC_WTE_002 or reopened).");
        if (!"Shutdown (Composite)".equals(workOrderPage.getWorkTypeValue())) {
            Assert.assertTrue(selectWorkTypeCommitted("Shutdown (Composite)"),
                    "'Shutdown (Composite)' should commit on the (re)opened dialog.");
        }
        shutdownWoName = "WTE_SHUT_" + System.currentTimeMillis();
        workOrderPage.fillWoName(shutdownWoName);
        Assert.assertTrue(workOrderPage.isCreateButtonEnabled(),
                "Create should be enabled with name + work type set.");
        workOrderPage.clickCreateWorkOrder();
        boolean closed = waitForCreateDialogClosed(20);
        String residual = closed ? "" : head(workOrderPage.getCreateDialogText(), 500);
        Assert.assertTrue(closed, "Create dialog should close after creating '" + shutdownWoName + "'. Dialog text: " + residual);

        boolean found = findWorkOrderInGrid(shutdownWoName, 6);
        Assert.assertTrue(found, "'" + shutdownWoName + "' should appear in the WO grid (index lags 5-15s).");
        boolean onDetail = openWorkOrderByName(shutdownWoName);
        Assert.assertTrue(onDetail, "Should open the '" + shutdownWoName + "' detail page from the grid.");
        shutdownSessionId = workOrderPage.getWoDetailId();
        trackCreated(shutdownSessionId);
        logStep("Shutdown WO created: id=" + shutdownSessionId);

        int assetRows = driver.findElements(By.cssSelector("[role='rowgroup'] [role='row']")).size();
        String assetsTabRaw = rawTabText("Assets");
        boolean badgeZero = assetsTabRaw.equals("Assets") || assetsTabRaw.equals("Assets0");
        logStep("Shutdown WO Assets state: gridRows=" + assetRows + ", assetsTabRaw='" + assetsTabRaw + "'");
        Assert.assertTrue(assetRows == 0 || badgeZero,
                "Shutdown (0-procedure) WO must start with ZERO assets — observed grid rows=" + assetRows
                + ", Assets tab raw text='" + assetsTabRaw + "' (badge absent/0 expected).");
    }

    @Test(priority = 4,
          description = "TC_WTE_004: Delete the Shutdown WO")
    public void testTC_WTE_004_ShutdownCleanupDeleted() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_004 — Shutdown (Composite) cleanup");
        Assert.assertNotNull(shutdownWoName, "Precondition: TC_WTE_003 must have created the Shutdown WO.");
        boolean deleted = deleteTracked(shutdownSessionId);
        logStep("DELETE /api/ir_session/" + shutdownSessionId + " ok=" + deleted + " (async receipt)");
        boolean gone = workOrderGoneFromGrid(shutdownWoName, 8);
        Assert.assertTrue(gone,
                "'" + shutdownWoName + "' must disappear from the grid after DELETE (apiDeleteOk=" + deleted
                + ", id=" + shutdownSessionId + ", deletion receipt is async — polled 8x).");
    }

    // ================================================================
    // 3) GENERAL — pin the no-service detail contract (4 sequential TCs)
    // ================================================================

    @Test(priority = 5,
          description = "TC_WTE_005: Create a General WO")
    public void testTC_WTE_005_GeneralCreateSucceeds() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_005 — General create");
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open.");
        Assert.assertTrue(selectWorkTypeCommitted("General"), "'General' should commit.");
        generalWoName = "WTE_GEN_" + System.currentTimeMillis();
        workOrderPage.fillWoName(generalWoName);
        Assert.assertTrue(workOrderPage.isCreateButtonEnabled(), "Create should be enabled with name + General type.");
        workOrderPage.clickCreateWorkOrder();
        boolean closed = waitForCreateDialogClosed(20);
        String residual = closed ? "" : head(workOrderPage.getCreateDialogText(), 500);
        Assert.assertTrue(closed, "Create dialog should close for General WO '" + generalWoName + "'. Dialog text: " + residual);
        Assert.assertTrue(findWorkOrderInGrid(generalWoName, 6),
                "'" + generalWoName + "' should appear in the WO grid.");
    }

    @Test(priority = 6,
          description = "TC_WTE_006: General WO detail-page contract")
    public void testTC_WTE_006_GeneralDetailContractPinned() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_006 — General detail contract pin");
        Assert.assertNotNull(generalWoName, "Precondition: TC_WTE_005 must have created the General WO.");
        ensureOnWorkOrdersList();
        searchWorkOrders(generalWoName);
        pause(2500);
        boolean onDetail = openWorkOrderByName(generalWoName);
        Assert.assertTrue(onDetail, "Should open the General WO detail page for '" + generalWoName + "'.");
        generalSessionId = workOrderPage.getWoDetailId();
        trackCreated(generalSessionId);

        generalTabs = workOrderPage.getDetailTabNames();
        generalChips = workOrderPage.getWoHeaderChips();
        String body = driver.findElement(By.tagName("body")).getText();
        logStep("PINNED — General detail tabs (previously unknown contract): " + generalTabs);
        logStep("PINNED — General header chips: " + generalChips);
        Assert.assertTrue(generalTabs.contains("Assets"),
                "General WO detail must at least have an 'Assets' tab — OBSERVED tabs=" + generalTabs
                + ", chips=" + generalChips + " (this run PINS the General family contract, WorkTypeCatalog.Family.GENERAL is null).");
        Assert.assertTrue(body.contains(generalWoName),
                "General WO detail page must show the WO name '" + generalWoName + "' — OBSERVED tabs="
                + generalTabs + ", chips=" + generalChips);
    }

    @Test(priority = 7,
          description = "TC_WTE_007: General WO header chips")
    public void testTC_WTE_007_GeneralHeaderChips() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_007 — General header chips truth");
        Assert.assertNotNull(generalChips, "Precondition: TC_WTE_006 must have captured the chips.");
        boolean containsGeneral = false;
        for (String c : generalChips) {
            if (c.toLowerCase().contains("general")) { containsGeneral = true; break; }
        }
        logStep("General chips truth: containsGeneral=" + containsGeneral + ", chips=" + generalChips);
        Assert.assertFalse(generalChips.isEmpty(),
                "General WO must render header chips. TRUTH: containsGeneral=" + containsGeneral
                + ", OBSERVED chips=" + generalChips
                + " (service WOs pin chip[0]=work-type name; this pins whether General follows suit).");
    }

    @Test(priority = 8,
          description = "TC_WTE_008: Delete the General WO")
    public void testTC_WTE_008_GeneralCleanupDeleted() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_008 — General cleanup");
        Assert.assertNotNull(generalWoName, "Precondition: TC_WTE_005 must have created the General WO.");
        boolean deleted = deleteTracked(generalSessionId);
        logStep("DELETE /api/ir_session/" + generalSessionId + " ok=" + deleted + " (async receipt)");
        boolean gone = workOrderGoneFromGrid(generalWoName, 8);
        Assert.assertTrue(gone,
                "'" + generalWoName + "' must disappear from the grid after DELETE (apiDeleteOk=" + deleted
                + ", id=" + generalSessionId + ").");
    }

    // ================================================================
    // 4) CANCEL DISCARDS — fully-filled dialog leaves no trace
    // ================================================================

    @Test(priority = 9, dataProvider = "servicePlusGeneral",
          description = "TC_WTE_009: Cancel discards a filled dialog")
    public void testTC_WTE_009_CancelDiscards(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_009 — cancel discards / " + profile.name);
        String name = "WTE_CXL_" + System.currentTimeMillis();
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open.");
        Assert.assertTrue(selectWorkTypeCommitted(profile.name), "'" + profile.name + "' should commit.");
        workOrderPage.fillWoName(name);
        workOrderPage.typeDueDate("12/31/2026");
        workOrderPage.fillWoDescription("cancel-discard probe for " + profile.name);
        boolean cancelled = workOrderPage.cancelCreateDialog();
        boolean found = findWorkOrderInGrid(name, 2);
        clearWorkOrderSearch();
        Assert.assertTrue(cancelled, "Cancel should close the create dialog.");
        Assert.assertFalse(found,
                "Cancelled '" + profile.name + "' WO must NOT be created — but '" + name + "' was found in the grid.");
    }

    // ================================================================
    // 5) REOPEN STATE RESET — no sticky work type / name after cancel
    // ================================================================

    @Test(priority = 10, dataProvider = "servicePlusGeneral",
          description = "TC_WTE_010: Reopen resets Work Type, keeps WO Name draft")
    public void testTC_WTE_010_ReopenStateReset(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_010 — reopen state / " + profile.name);
        // PINNED live 2026-07-21 (Playwright + Selenium agree): after Cancel + reopen the dialog
        // KEEPS the WO Name as a sticky draft but RESETS the Work Type — so Create is disabled
        // again (type is the missing gate). Draft-keeping may be by-design; this row pins it so
        // any change (full reset OR full restore) surfaces deliberately.
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open.");
        Assert.assertTrue(selectWorkTypeCommitted(profile.name), "'" + profile.name + "' should commit.");
        String draftName = "WTE_RST_" + System.currentTimeMillis();
        workOrderPage.fillWoName(draftName);
        workOrderPage.cancelCreateDialog();
        boolean reopened = ensureCreateDialogOpen();
        String typeValue = workOrderPage.getWorkTypeValue();
        String nameValue = woNameValue();
        boolean createEnabled = workOrderPage.isCreateButtonEnabled();
        logStep("Reopened dialog after cancelling a '" + profile.name + "' fill: workType='" + typeValue
                + "', name='" + nameValue + "', createEnabled=" + createEnabled);
        workOrderPage.clearWoName();   // leave no draft behind for later rows
        workOrderPage.cancelCreateDialog();
        Assert.assertTrue(reopened, "Create dialog should reopen after cancel.");
        Assert.assertEquals(typeValue, "",
                "Reopened dialog must NOT remember the work type — observed '" + typeValue
                + "' after cancelling a '" + profile.name + "' fill.");
        Assert.assertEquals(nameValue, draftName,
                "PINNED CONTRACT: the reopened dialog KEEPS the WO Name draft after Cancel "
                + "(live-verified 2026-07-21). If this fails the draft behavior changed — re-pin deliberately.");
        Assert.assertFalse(createEnabled,
                "Create must be disabled on reopen: the sticky name alone must not satisfy the dual gate.");
    }

    // ================================================================
    // 6) NAME EDGE CASES — 256-char / XSS / unicode / whitespace-only
    // ================================================================

    @Test(priority = 11, dataProvider = "nameEdges",
          description = "TC_WTE_011: WO name edge cases render literally")
    public void testTC_WTE_011_NameEdgeCases(String kind) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_011 — name edge / " + kind);
        long millis = System.currentTimeMillis();
        String prefix = "WTE_EDGE_" + millis;
        String name;
        String literalMarker;
        if ("max-length-256".equals(kind)) {
            StringBuilder sb = new StringBuilder(prefix + "_");
            while (sb.length() < 256) sb.append('X');
            name = sb.toString();
            literalMarker = prefix;
        } else if ("xss-script-tag".equals(kind)) {
            name = prefix + "_<script>alert(1)</script>";
            literalMarker = "<script>alert(1)</script>";
        } else {
            name = prefix + "_⚡️ \"double\" 'single'";
            literalMarker = "⚡";
        }

        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open for edge '" + kind + "'.");
        Assert.assertTrue(selectWorkTypeCommitted("Infrared Thermography"), "'Infrared Thermography' should commit.");
        try {
            workOrderPage.clickStartEmptyInstead();   // start-empty scope keeps the create light
        } catch (Exception e) {
            logStep("Start Empty Instead not clickable (continuing with default scope): " + e.getMessage());
        }
        workOrderPage.fillWoName(name);
        pause(800);

        boolean createEnabled = workOrderPage.isCreateButtonEnabled();
        Assert.assertTrue(createEnabled, "Create should be enabled for edge name kind '" + kind + "'.");
        workOrderPage.clickCreateWorkOrder();
        boolean closed = waitForCreateDialogClosed(20);
        boolean alertFree = noJsAlertPresent();   // dismisses + logs if one fired
        String residual = closed ? "" : head(workOrderPage.getCreateDialogText(), 500);
        if (!closed) {
            // Nothing was created — cancel so later rows start clean, then pin the failure.
            workOrderPage.cancelCreateDialog();
        }
        Assert.assertTrue(closed,
                "Create should succeed for edge name kind '" + kind + "' — dialog stayed open with: " + residual);

        boolean found = findWorkOrderInGrid(prefix, 6);
        String rowText = found ? matchingRowText(prefix) : "";
        boolean alertFreeAfterGrid = noJsAlertPresent();
        logStep("Edge '" + kind + "' grid row text: " + head(rowText, 300));

        // self-clean before asserting so a failed pin never leaks a WO
        String id = openNthMatchingRow(prefix, 0);
        trackCreated(id);
        boolean deleted = deleteTracked(id);
        logStep("Edge '" + kind + "' cleanup: id=" + id + ", apiDeleteOk=" + deleted);
        ensureOnWorkOrdersList();
        clearWorkOrderSearch();

        Assert.assertTrue(found, "Edge WO (kind '" + kind + "', prefix '" + prefix + "') should appear in the grid.");
        Assert.assertTrue(alertFree && alertFreeAfterGrid,
                "XSS gate: NO JavaScript alert may fire for name kind '" + kind + "' (afterCreate=" + alertFree
                + ", afterGrid=" + alertFreeAfterGrid + ").");
        Assert.assertTrue(rowText.contains(literalMarker),
                "Grid must render the edge name LITERALLY for kind '" + kind + "' — expected marker '"
                + literalMarker + "' in row text: " + head(rowText, 300));
    }

    /**
     * VERIFIED PRODUCT BUG (live 2026-07-22, full stack): a whitespace-only WO Name ("   ")
     * (a) enables the Create button and (b) is ACCEPTED by the backend — the dialog closes and
     * the app lands on the new WO's detail page, leaving a blank-named row in /sessions.
     * This tripwire asserts the CORRECT behavior (Create stays disabled) and therefore fails
     * RED until the product trims/validates the name. Excluded from green gates via the
     * known-product-bug group; run deliberately with -Dgroups=known-product-bug.
     * The probe WO created during verification was deleted (DELETE /ir_session/{id}).
     */
    @Test(priority = 11, groups = {"known-product-bug"},
          description = "TC_WTE_011b: Whitespace-only name should block Create (known bug)")
    public void testTC_WTE_011b_WhitespaceOnlyNameBlocked() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE,
                "TC_WTE_011b — whitespace-only name tripwire (known product bug)");
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open.");
        Assert.assertTrue(selectWorkTypeCommitted("General"), "'General' should commit.");
        workOrderPage.fillWoName("   ");
        pause(800);
        boolean enabled = workOrderPage.isCreateButtonEnabled();
        logStep("Whitespace-only name ('   ') -> Create enabled=" + enabled);
        workOrderPage.clearWoName();          // never leave the sticky whitespace draft behind
        workOrderPage.cancelCreateDialog();   // never actually create
        Assert.assertFalse(enabled,
                "Create must stay DISABLED for a whitespace-only WO name — verified 2026-07-22 that it "
                + "currently ENABLES and the backend even accepts the create (blank-named WO). "
                + "When the product adds trim-validation this tripwire turns green: remove the "
                + "known-product-bug group then.");
    }

    // ================================================================
    // 7) FACILITY CHANGE RE-FIRES THE SCOPE PREVIEW
    // ================================================================

    @Test(priority = 12, dataProvider = "facilityFlips",
          description = "TC_WTE_012: Facility change re-fires the scope preview")
    public void testTC_WTE_012_FacilityChangeRefiresPreview(String direction) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_012 — facility flip / " + direction);
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open.");
        Assert.assertTrue(selectWorkTypeCommitted("Infrared Thermography"), "'Infrared Thermography' should commit.");
        int countZ1 = workOrderPage.getMatchingAssetsCount(20);
        logStep("IR on Z1 initial matching assets = " + countZ1 + " (46-ish live 2026-07-21)");

        workOrderPage.selectWoFacility("Test Site");
        pause(1000);
        int countTestSite = settleMatchingCount(20);
        boolean stillOpenAfterFirstFlip = workOrderPage.isCreateWorkOrderDialogOpen();
        logStep("After Facility -> 'Test Site': matching=" + countTestSite + ", facility='"
                + workOrderPage.getFacilityValue() + "', dialogOpen=" + stillOpenAfterFirstFlip);

        if (direction.contains("back to Z1")) {
            workOrderPage.selectWoFacility(WorkTypeCatalog.FIXTURE_SITE);
            pause(1000);
            int countBackZ1 = settleMatchingCount(20);
            boolean stillOpen = workOrderPage.isCreateWorkOrderDialogOpen();
            logStep("After Facility back -> Z1: matching=" + countBackZ1 + ", dialogOpen=" + stillOpen);
            workOrderPage.cancelCreateDialog();
            Assert.assertTrue(countZ1 > 0, "IR on Z1 should preview >0 matching assets — observed " + countZ1);
            Assert.assertTrue(stillOpen, "Dialog must survive both facility flips.");
            Assert.assertTrue(countBackZ1 > 0,
                    "Back on Z1 the IR preview must settle >0 again — observed " + countBackZ1
                    + " (initial Z1=" + countZ1 + ", Test Site=" + countTestSite + ").");
        } else {
            workOrderPage.cancelCreateDialog();
            Assert.assertTrue(countZ1 > 0, "IR on Z1 should preview >0 matching assets — observed " + countZ1);
            Assert.assertTrue(stillOpenAfterFirstFlip, "Dialog must remain open after the facility change.");
            Assert.assertTrue(countTestSite >= 0,
                    "After Facility -> 'Test Site' the preview must re-settle to a fresh value >=0 — observed "
                    + countTestSite + " (Z1 was " + countZ1 + ").");
        }
    }

    // ================================================================
    // 8) DUPLICATE NAME — pin allowed-or-blocked deterministically
    // ================================================================

    @Test(priority = 13, dataProvider = "dupRuns",
          description = "TC_WTE_013: Duplicate WO name — allowed or blocked (pinned)")
    public void testTC_WTE_013_DuplicateNamePinned(String run) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_013 — duplicate name / " + run);
        String name = "WTE_DUP_" + System.currentTimeMillis();

        // First create (must succeed)
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open for the first create.");
        Assert.assertTrue(selectWorkTypeCommitted("General"), "'General' should commit (first create).");
        workOrderPage.fillWoName(name);
        workOrderPage.clickCreateWorkOrder();
        boolean firstClosed = waitForCreateDialogClosed(20);
        Assert.assertTrue(firstClosed, "First create of '" + name + "' must succeed — dialog text: "
                + (firstClosed ? "" : head(workOrderPage.getCreateDialogText(), 500)));
        Assert.assertTrue(findWorkOrderInGrid(name, 6), "First '" + name + "' should appear in the grid.");
        String id1 = openNthMatchingRow(name, 0);
        trackCreated(id1);
        logStep("First WO created: id=" + id1);

        // Duplicate attempt
        boolean reopened = freshCreateDialog();
        Assert.assertTrue(reopened, "Create dialog should open for the duplicate attempt.");
        Assert.assertTrue(selectWorkTypeCommitted("General"), "'General' should commit (duplicate attempt).");
        workOrderPage.fillWoName(name);
        workOrderPage.clickCreateWorkOrder();
        boolean dupClosed = waitForCreateDialogClosed(15);

        if (dupClosed) {
            // OUTCOME A: duplicates ALLOWED -> exactly 2 rows, clean both up.
            int rows = countRowsContaining(name, 6, 2);
            String idA = openNthMatchingRow(name, 0);
            String idB = openNthMatchingRow(name, 1);
            if (idB.isEmpty() || idB.equals(idA)) idB = openNthMatchingRow(name, 0);  // grid-reorder fallback
            trackCreated(idA);
            trackCreated(idB);
            Set<String> distinct = new LinkedHashSet<String>();
            if (!idA.isEmpty()) distinct.add(idA);
            if (!idB.isEmpty()) distinct.add(idB);
            for (String id : distinct) deleteTracked(id);
            ensureOnWorkOrdersList();
            clearWorkOrderSearch();
            logStep("PINNED: duplicate WO names ALLOWED — rows=" + rows + ", ids=" + distinct);
            Assert.assertEquals(rows, 2,
                    "PINNED BEHAVIOR: duplicate WO names are ALLOWED — the grid must then show exactly 2 rows for '"
                    + name + "' (observed " + rows + ", ids=" + distinct + ").");
        } else {
            // OUTCOME B: duplicates BLOCKED -> dialog stays open with a visible validation error.
            boolean stillOpen = workOrderPage.isCreateWorkOrderDialogOpen();
            String dialogText = head(workOrderPage.getCreateDialogText(), 600);
            logStep("PINNED: duplicate WO name BLOCKED — dialog text: " + dialogText);
            workOrderPage.cancelCreateDialog();
            deleteTracked(id1);
            ensureOnWorkOrdersList();
            clearWorkOrderSearch();
            Assert.assertTrue(stillOpen && !dialogText.isEmpty(),
                    "PINNED BEHAVIOR: duplicate WO name BLOCKED — the dialog must stay open showing a validation error."
                    + " stillOpen=" + stillOpen + ", dialog text: " + dialogText);
        }
    }

    // ================================================================
    // 9) PAST DUE DATE — pin the behavior, never crash
    // ================================================================

    @Test(priority = 14,
          description = "TC_WTE_014: Past due date doesn't crash the dialog")
    public void testTC_WTE_014_PastDueDate() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTE_014 — past due date 01/01/2020");
        String name = "WTE_DUE_" + System.currentTimeMillis();
        boolean opened = freshCreateDialog();
        Assert.assertTrue(opened, "Create dialog should open.");
        Assert.assertTrue(selectWorkTypeCommitted("General"), "'General' should commit.");
        workOrderPage.fillWoName(name);
        workOrderPage.typeDueDate("01/01/2020");
        pause(800);
        String dueValue = workOrderPage.getDueDateValue();
        boolean enabledWithPast = workOrderPage.isCreateButtonEnabled();
        boolean dialogAlive = workOrderPage.isCreateWorkOrderDialogOpen();
        logStep("Past due date typed: value='" + dueValue + "', createEnabled=" + enabledWithPast
                + ", dialogAlive=" + dialogAlive);
        Assert.assertTrue(dialogAlive, "Dialog must not crash after typing past due date 01/01/2020 (value='" + dueValue + "').");

        if (enabledWithPast) {
            workOrderPage.clickCreateWorkOrder();
            boolean closed = waitForCreateDialogClosed(15);
            if (closed) {
                boolean found = findWorkOrderInGrid(name, 6);
                String id = found ? openNthMatchingRow(name, 0) : "";
                trackCreated(id);
                boolean deleted = deleteTracked(id);
                ensureOnWorkOrdersList();
                clearWorkOrderSearch();
                Assert.assertTrue(found,
                        "OBSERVED: past due date ACCEPTED (Create enabled, dialog closed) — WO '" + name
                        + "' must then exist in the grid (found=" + found + ", cleanedUp=" + deleted
                        + ", dueValue='" + dueValue + "').");
            } else {
                String dialogText = head(workOrderPage.getCreateDialogText(), 600);
                boolean cancelled = workOrderPage.cancelCreateDialog();
                Assert.assertTrue(cancelled,
                        "OBSERVED: past due date BLOCKED at submit (dialog stayed open: " + dialogText
                        + ") — the dialog must still cancel cleanly (cancelled=" + cancelled + ").");
            }
        } else {
            boolean cancelled = workOrderPage.cancelCreateDialog();
            Assert.assertTrue(cancelled,
                    "OBSERVED: past due date DISABLES Create (dueValue='" + dueValue
                    + "') — the dialog must still cancel cleanly (cancelled=" + cancelled + ").");
        }
    }

    // ================================================================
    // CLEANUP
    // ================================================================

    @AfterClass(alwaysRun = true)
    public void edgeSuiteCleanup() {
        try { closeCreateDialogIfOpen(); } catch (Exception ignored) {}
        for (String id : new ArrayList<String>(createdIds)) {
            try {
                if (apiDeleteWorkOrder(id)) createdIds.remove(id);
            } catch (Exception ignored) {}
        }
        System.out.println("[WTE] Auto-Schedule enablement truth table: " + AUTO_SCHEDULE_TRUTH);
        if (!createdIds.isEmpty()) {
            System.out.println("[WTE] WARNING — leftover WO ids not confirmed deleted: " + createdIds);
        }
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    /** Cancel any open create dialog and open a genuinely fresh one. */
    private boolean freshCreateDialog() {
        closeCreateDialogIfOpen();
        return ensureCreateDialogOpen();
    }

    /**
     * Fill the EMPTY field_technician Team slot: the dialog pre-fills the certifier row with the
     * current admin, so among the 'Select user' inputs we pick the one with an empty value, open
     * its popup via the popup-indicator (JS — same approach as WorkOrderPage.selectFirstAutocompleteOption)
     * and click the first option with a real Actions move+click.
     */
    private boolean fillEmptyFieldTechnician() {
        try {
            List<WebElement> slots = driver.findElements(By.xpath("//input[@placeholder='Select user']"));
            WebElement empty = null;
            for (WebElement s : slots) {
                String v = String.valueOf(s.getAttribute("value"));
                if (v == null || v.trim().isEmpty() || "null".equals(v)) { empty = s; break; }
            }
            if (empty == null) {
                logStep("No empty 'Select user' slot found among " + slots.size() + " team inputs");
                return false;
            }
            js().executeScript(
                    "arguments[0].scrollIntoView({block:'center'}); arguments[0].focus(); arguments[0].click();"
                    + "var w=arguments[0].closest('.MuiAutocomplete-root');"
                    + "if(w){var b=w.querySelector('.MuiAutocomplete-popupIndicator'); if(b) b.click();}", empty);
            pause(800);
            By opt = By.xpath("//ul[@role='listbox']/li[@role='option']");
            for (int i = 0; i < 8 && driver.findElements(opt).isEmpty(); i++) pause(300);
            List<WebElement> opts = driver.findElements(opt);
            if (opts.isEmpty()) {
                logStep("field_technician dropdown offered no options");
                return false;
            }
            WebElement first = opts.get(0);
            String chosen = first.getText().trim();
            js().executeScript("arguments[0].scrollIntoView({block:'center'});", first);
            pause(150);
            try { new Actions(driver).moveToElement(first).click().perform(); }
            catch (Exception e) { first.click(); }
            pause(600);
            logStep("field_technician set to '" + chosen + "'");
            return true;
        } catch (Exception e) {
            logStep("fillEmptyFieldTechnician failed: " + e.getMessage());
            return false;
        }
    }

    /** Poll until the create dialog is closed (creation is async; up to {@code seconds}). */
    private boolean waitForCreateDialogClosed(int seconds) {
        for (int i = 0; i < seconds * 2; i++) {
            if (!workOrderPage.isCreateWorkOrderDialogOpen()) return true;
            pause(500);
        }
        return !workOrderPage.isCreateWorkOrderDialogOpen();
    }

    /** The WO Name input's current value ('' when absent). */
    private String woNameValue() {
        List<WebElement> e = driver.findElements(By.xpath("//input[@placeholder='e.g., Q1 2024 Maintenance']"));
        return e.isEmpty() ? "" : String.valueOf(e.get(0).getAttribute("value"));
    }

    /** Raw (badge-included) text of the detail tab whose stripped name matches {@code stripped}. */
    private String rawTabText(String stripped) {
        for (WebElement t : driver.findElements(By.xpath("//button[@role='tab']"))) {
            String raw = t.getText().trim();
            if (raw.replaceAll("\\d+\\+?$", "").trim().equalsIgnoreCase(stripped)) return raw;
        }
        return "";
    }

    /** Set the WO list search box via the React-native value setter. */
    private void searchWorkOrders(String text) {
        try {
            WebElement s = driver.findElement(By.xpath("//input[contains(@placeholder,'Search work orders')]"));
            js().executeScript(
                    "var i=arguments[0], v=arguments[1];"
                    + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "set.call(i, v); i.dispatchEvent(new Event('input',{bubbles:true}));", s, text);
        } catch (Exception e) {
            System.out.println("[WTE] searchWorkOrders failed: " + e.getMessage());
        }
    }

    /** Text of the first grid row containing {@code needle} (search must already be applied). */
    private String matchingRowText(String needle) {
        for (WebElement r : driver.findElements(By.cssSelector("[role='rowgroup'] [role='row']"))) {
            String t = r.getText();
            if (t.contains(needle)) return t;
        }
        return "";
    }

    /**
     * Poll the grid until at least {@code minExpected} rows containing {@code needle} render
     * (creation indexing lags 5-15s). Returns the final observed count.
     */
    private int countRowsContaining(String needle, int attempts, int minExpected) {
        ensureOnWorkOrdersList();
        int last = 0;
        for (int i = 0; i < attempts; i++) {
            searchWorkOrders(needle);
            pause(2500);
            last = 0;
            for (WebElement r : driver.findElements(By.cssSelector("[role='rowgroup'] [role='row']"))) {
                if (r.getText().contains(needle)) last++;
            }
            if (last >= minExpected) break;
            pause(2000);
        }
        return last;
    }

    /**
     * Open the {@code index}-th grid row containing {@code needle} and return its session id
     * ('' on failure). Leaves the browser on the detail page.
     */
    private String openNthMatchingRow(String needle, int index) {
        try {
            ensureOnWorkOrdersList();
            searchWorkOrders(needle);
            pause(2500);
            List<WebElement> matches = new ArrayList<WebElement>();
            for (WebElement r : driver.findElements(By.cssSelector("[role='rowgroup'] [role='row']"))) {
                if (r.getText().contains(needle)) matches.add(r);
            }
            if (index >= matches.size()) {
                logStep("openNthMatchingRow: only " + matches.size() + " rows for '" + needle + "', wanted index " + index);
                return "";
            }
            WebElement row = matches.get(index);
            js().executeScript("arguments[0].scrollIntoView({block:'center'});", row);
            pause(200);
            try { row.click(); } catch (Exception e) { js().executeScript("arguments[0].click();", row); }
            for (int i = 0; i < 25 && !workOrderPage.isOnWoDetailPage(); i++) pause(400);
            if (!workOrderPage.isOnWoDetailPage()) return "";
            pause(2000);
            return workOrderPage.getWoDetailId();
        } catch (Exception e) {
            logStep("openNthMatchingRow failed: " + e.getMessage());
            return "";
        }
    }

    /** Poll until no grid row contains {@code name} (delete receipt is async). Clears the search after. */
    private boolean workOrderGoneFromGrid(String name, int attempts) {
        ensureOnWorkOrdersList();
        for (int i = 0; i < attempts; i++) {
            searchWorkOrders(name);
            pause(2500);
            boolean present = false;
            for (WebElement r : driver.findElements(By.cssSelector("[role='rowgroup'] [role='row']"))) {
                if (r.getText().contains(name)) { present = true; break; }
            }
            if (!present) {
                clearWorkOrderSearch();
                return true;
            }
            pause(2000);
        }
        clearWorkOrderSearch();
        return false;
    }

    /** Wait out a loading scope preview, then read the settled count within the remaining budget. */
    private int settleMatchingCount(int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end && workOrderPage.isScopePreviewLoading()) pause(500);
        int remain = (int) Math.max(2L, (end - System.currentTimeMillis()) / 1000L);
        return workOrderPage.getMatchingAssetsCount(remain);
    }

    /** True when no native JS alert is present; dismisses + logs one if it fired (XSS evidence). */
    private boolean noJsAlertPresent() {
        try {
            Alert a = driver.switchTo().alert();
            String text = a.getText();
            a.dismiss();
            logStep("JS ALERT FIRED (XSS!): '" + text + "'");
            return false;
        } catch (NoAlertPresentException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /** Track a created session id for @AfterClass sweeping. */
    private void trackCreated(String id) {
        if (id != null && !id.isEmpty()) createdIds.add(id);
    }

    /** Delete a WO via API; untrack on success, keep tracked on failure. Best-effort, never throws. */
    private boolean deleteTracked(String id) {
        if (id == null || id.isEmpty()) return false;
        createdIds.add(id);
        boolean ok = apiDeleteWorkOrder(id);
        if (ok) createdIds.remove(id);
        return ok;
    }

    /** First {@code max} chars of {@code s} (single-line) for compact assertion messages. */
    private String head(String s, int max) {
        if (s == null) return "";
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }
}
