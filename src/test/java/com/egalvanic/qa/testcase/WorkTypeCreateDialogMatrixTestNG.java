package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.constants.WorkTypeCatalog;
import com.egalvanic.qa.constants.WorkTypeCatalog.WorkTypeProfile;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Work Orders — <b>Work Type Create Dialog Matrix</b> (ZP-3000 Services V2, 153 data-driven cases).
 *
 * <p>Pins the v1.35 "Create New Work Order" dialog contract PER WORK TYPE against
 * {@link WorkTypeCatalog} (live-verified 2026-07-21 on acme.qa / site Z1):</p>
 * <ul>
 *   <li>the 14-option Work Type catalog itself (exact labels, exact order);</li>
 *   <li>type selection commits in the MUI Autocomplete for every option;</li>
 *   <li>scope-preview semantics per type: procedures &gt; 0 → "N matching assets" settles,
 *       0 procedures (Shutdown (Composite)) → no-procedures notice, General → neither;</li>
 *   <li>notice/preview mutual exclusivity across the 13 services;</li>
 *   <li>Auto-Schedule renders for every type and stays disabled without Est. Hours + technician;</li>
 *   <li>Advanced Settings defaults (Priority=Medium, Photo Type=FLIR-SEP, Start=today, Due=empty)
 *       survive type switching — selecting a type must not clobber them;</li>
 *   <li>the five dialog sections (Scope/Team/Advanced Settings/Schedule/Equipment) and the three
 *       required-field asterisks (WO Name, Facility, Work Type) render for every type;</li>
 *   <li>Create-button gating (WO Name + Work Type) per type, from a fresh dialog each row;</li>
 *   <li>type switching retargets the scope preview (13 adjacent pairs over the display order);</li>
 *   <li>"Start Empty Instead" clears the auto-pulled scope for every preview service.</li>
 * </ul>
 *
 * <p><b>Efficiency contract:</b> priorities 1–11 REUSE one open dialog across rows (selecting a
 * different Work Type in the same dialog is exactly the user behavior under test and keeps the
 * matrix CI-fast). Priorities 12–13 need pristine state and close+reopen the dialog per row.
 * Non-destructive: no work order is ever created — gating rows Cancel after checking the button.</p>
 */
public class WorkTypeCreateDialogMatrixTestNG extends WorkTypeUiBase {

    private static final String FEATURE = "Work Type Create Dialog Matrix";

    // Run on a LIGHT site: this suite opens the create dialog / previews scope repeatedly, which
    // is slow on the huge Z1 SLD (~23s creates, laggy previews). Scope-count assertions here are
    // tolerant (n >= 0), so a small site works and runs far faster / more reliably.
    @Override protected String workTypeSite()  { return WorkTypeCatalog.CREATE_SITE; }
    @Override protected String workTypeSldId() { return WorkTypeCatalog.CREATE_SLD_ID; }

    /** Same dialog root the page object anchors on (its constant is private). */
    private static final String DIALOG_XPATH =
            "//div[@role='dialog'][.//*[normalize-space()='Create New Work Order']]";

    private static final Pattern MATCHING_ASSETS_TEXT = Pattern.compile("\\d+\\s+matching assets");

    private static final String[] DIALOG_SECTIONS =
            {"Scope", "Team", "Advanced Settings", "Schedule", "Equipment"};

    // ================================================================
    // DATA PROVIDERS (all built from WorkTypeCatalog — the authoritative list)
    // ================================================================

    @DataProvider(name = "optionCatalogRows")
    public Object[][] optionCatalogRows() {
        return new Object[][]{{"14-option catalog pinned (13 services + General)"}};
    }

    @DataProvider(name = "anatomyRows")
    public Object[][] anatomyRows() {
        return new Object[][]{{"title"}, {"initial-create-disabled"}};
    }

    /** All 14 dropdown options in display order. */
    @DataProvider(name = "allTypes")
    public Object[][] allTypes() {
        List<Object[]> rows = new ArrayList<>();
        for (WorkTypeProfile p : WorkTypeCatalog.ALL) rows.add(new Object[]{p});
        return rows.toArray(new Object[0][]);
    }

    /** The 13 service work types (ALL minus General). */
    @DataProvider(name = "serviceTypes")
    public Object[][] serviceTypes() {
        List<Object[]> rows = new ArrayList<>();
        for (WorkTypeProfile p : WorkTypeCatalog.SERVICES) rows.add(new Object[]{p});
        return rows.toArray(new Object[0][]);
    }

    /** 13 adjacent (i, i+1) pairs over the 14-option display order. */
    @DataProvider(name = "adjacentPairs")
    public Object[][] adjacentPairs() {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < WorkTypeCatalog.ALL.size() - 1; i++) rows.add(new Object[]{Integer.valueOf(i)});
        return rows.toArray(new Object[0][]);
    }

    /** The 12 services with procedures (scope preview expected) — excludes Shutdown (Composite). */
    @DataProvider(name = "previewServices")
    public Object[][] previewServices() {
        List<Object[]> rows = new ArrayList<>();
        for (WorkTypeProfile p : WorkTypeCatalog.SERVICES) {
            if (p.expectsScopePreview()) rows.add(new Object[]{p});
        }
        return rows.toArray(new Object[0][]);
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Every row's precondition: the create dialog is open (reused or freshly opened). Hard-fails. */
    private void requireDialogOpen() {
        Assert.assertTrue(ensureCreateDialogOpen(),
                "Precondition: the Create New Work Order dialog must be open.");
    }

    /** Select a work type and hard-fail unless the value actually committed. */
    private void requireTypeCommitted(String typeName) {
        Assert.assertTrue(selectWorkTypeCommitted(typeName),
                "Precondition: Work Type '" + typeName + "' must commit in the Autocomplete (value now '"
                        + workOrderPage.getWorkTypeValue() + "').");
    }

    /** Poll for the 0-procedure notice; true as soon as it renders (attempts x stepMs budget). */
    private boolean pollNoProceduresNotice(int attempts, long stepMs) {
        for (int i = 0; i < attempts; i++) {
            if (workOrderPage.hasNoProceduresNotice()) return true;
            pause(stepMs);
        }
        return workOrderPage.hasNoProceduresNotice();
    }

    /** True while the dialog still shows a settled "N matching assets" text. */
    private boolean matchingAssetsTextPresent() {
        return MATCHING_ASSETS_TEXT.matcher(workOrderPage.getCreateDialogText()).find();
    }

    /** True when the dialog has a label containing {@code labelFragment} that carries a '*' marker. */
    private boolean requiredLabelMarker(String labelFragment) {
        return !driver.findElements(By.xpath(DIALOG_XPATH
                + "//label[contains(.,'" + labelFragment + "')][contains(.,'*')]")).isEmpty();
    }

    // ================================================================
    // PRIORITIES 1-11 — dialog-REUSING groups (one dialog across rows)
    // ================================================================

    @Test(priority = 1, dataProvider = "optionCatalogRows",
          description = "TC_WTD_001: Work Type dropdown lists all 14 options in order")
    public void testTC_WTD_001_OptionCatalogPinned(String label) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_001 — " + label);
        requireDialogOpen();
        logStep("Opening the Work Type dropdown and reading its options in display order");
        List<String> actual = workOrderPage.getWorkTypeOptions();
        List<String> expected = WorkTypeCatalog.expectedOptionLabels();
        logStep("Live options (" + actual.size() + "): " + actual);
        logStep("Expected  (" + expected.size() + "): " + expected);
        Assert.assertEquals(actual, expected,
                "Work Type dropdown must offer EXACTLY the 14 catalog options in display order "
                        + "(catalog drifted? re-pull GET /api/procedures-v2/services).");
        ExtentReportManager.logPass("All 14 Work Type options match WorkTypeCatalog exactly, in order.");
    }

    @Test(priority = 2, dataProvider = "anatomyRows",
          description = "TC_WTD_002: Dialog anatomy + Create disabled when empty")
    public void testTC_WTD_002_DialogAnatomy(String aspect) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_002 — " + aspect);
        requireDialogOpen();
        if ("title".equals(aspect)) {
            boolean open = workOrderPage.isCreateWorkOrderDialogOpen();
            String text = workOrderPage.getCreateDialogText();
            logStep("Dialog open: " + open + "; text starts: '"
                    + text.substring(0, Math.min(80, text.length())).replace("\n", " | ") + "'");
            Assert.assertTrue(open && text.contains("Create New Work Order"),
                    "The create dialog must be open with the 'Create New Work Order' heading.");
            ExtentReportManager.logPass("Dialog open with 'Create New Work Order' heading.");
        } else {
            String typeVal = workOrderPage.getWorkTypeValue();
            boolean createEnabled = workOrderPage.isCreateButtonEnabled();
            logStep("Fresh dialog state — Work Type value: '" + typeVal + "', Create enabled: " + createEnabled);
            Assert.assertTrue(typeVal.isEmpty(),
                    "Precondition: Work Type must still be empty on the fresh dialog (was '" + typeVal + "').");
            Assert.assertFalse(createEnabled,
                    "Create must be DISABLED while both WO Name and Work Type are empty.");
            ExtentReportManager.logPass("Create is disabled on the pristine dialog (no name, no type).");
        }
    }

    @Test(priority = 3, dataProvider = "allTypes",
          description = "TC_WTD_003: Work Type selection commits")
    public void testTC_WTD_003_TypeCommits(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_003 — " + profile.name);
        requireDialogOpen();
        logStep("Selecting Work Type '" + profile.name + "' (" + profile.family + ")");
        boolean committed = selectWorkTypeCommitted(profile.name);
        String value = workOrderPage.getWorkTypeValue();
        logStep("committed=" + committed + ", input value now '" + value + "'");
        Assert.assertTrue(committed,
                "Work Type '" + profile.name + "' must commit in the MUI Autocomplete (value stayed '" + value + "').");
        Assert.assertEquals(value, profile.name,
                "Committed Work Type value must equal the selected option label.");
        ExtentReportManager.logPass("'" + profile.name + "' committed as the Work Type value.");
    }

    @Test(priority = 4, dataProvider = "allTypes",
          description = "TC_WTD_004: Scope preview per type")
    public void testTC_WTD_004_ScopePreviewPerType(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_004 — " + profile.name);
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        if (profile.expectsScopePreview()) {
            logStep("'" + profile.name + "' has " + profile.procedureCountAtCapture
                    + " procedures at capture — expecting the 'N matching asset(s)' preview to settle (<=20s)");
            int n = workOrderPage.getMatchingAssetsCount(20);
            logStep("Scope preview settled: " + n + " matching asset(s)");
            // The count is SITE-DATA dependent (rules resolve against Z1's assets): DGA matches 0
            // on Z1, UPS exactly 1 (singular label). The per-type contract is that the preview
            // SETTLES; zero matches must additionally render the eligibility notice.
            Assert.assertTrue(n >= 0,
                    "'" + profile.name + "' fires scope-preview and must settle on a 'N matching asset(s)' count (got " + n + ").");
            if (n == 0) {
                Assert.assertTrue(workOrderPage.hasNoEligibleAssetsNotice(),
                        "'" + profile.name + "' settled at 0 matching assets — the zero-state must show "
                                + "'No assets in this site are eligible for this work type with the current filters.'");
                ExtentReportManager.logPass("Preview settled at 0 for '" + profile.name + "' WITH the eligibility notice (pinned zero-state).");
            } else {
                ExtentReportManager.logPass("Preview settled on " + n + " matching asset(s) for '" + profile.name + "'.");
            }
        } else if (profile.expectsNoProceduresNotice()) {
            logStep("'" + profile.name + "' has 0 procedures — expecting the no-procedures notice (<=10s)");
            boolean notice = pollNoProceduresNotice(20, 500);
            Assert.assertTrue(notice,
                    "'" + profile.name + "' (0 procedures) must show 'This work type has no procedures configured "
                            + "— no assets will be pulled in automatically.' within 10s.");
            ExtentReportManager.logPass("No-procedures notice shown for '" + profile.name + "'.");
        } else {
            logStep("'General' has no service — waiting ~6s to prove NO preview and NO notice ever render");
            pause(6000);
            int n = workOrderPage.getMatchingAssetsCount(2);
            boolean notice = workOrderPage.hasNoProceduresNotice();
            logStep("After settle window: matchingAssetsCount=" + n + ", noProceduresNotice=" + notice);
            Assert.assertEquals(n, -1,
                    "'General' fires no scope-preview and must never render a 'N matching assets' count.");
            Assert.assertFalse(notice,
                    "'General' must not show the no-procedures notice (it has no service at all).");
            ExtentReportManager.logPass("'General' shows neither a matching-assets count nor a notice.");
        }
    }

    @Test(priority = 5, dataProvider = "serviceTypes",
          description = "TC_WTD_005: Preview vs no-procedures notice are exclusive")
    public void testTC_WTD_005_NoticeExclusivity(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_005 — " + profile.name);
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        if (profile.expectsScopePreview()) {
            int n = workOrderPage.getMatchingAssetsCount(20);
            boolean notice = workOrderPage.hasNoProceduresNotice();
            logStep("Preview settled at " + n + " matching asset(s); noProceduresNotice=" + notice);
            Assert.assertTrue(n >= 0,
                    "Precondition: preview must settle for '" + profile.name + "' before checking exclusivity (got " + n + ").");
            Assert.assertFalse(notice,
                    "'" + profile.name + "' renders a scope preview and must NOT also show the no-procedures notice.");
            ExtentReportManager.logPass("'" + profile.name + "': preview only (n=" + n + "), no notice — exclusive as pinned.");
        } else {
            boolean notice = pollNoProceduresNotice(20, 500);
            int n = workOrderPage.getMatchingAssetsCount(2);
            logStep("noProceduresNotice=" + notice + "; matchingAssetsCount=" + n);
            Assert.assertTrue(notice,
                    "Precondition: '" + profile.name + "' (0 procedures) must show the no-procedures notice.");
            Assert.assertEquals(n, -1,
                    "'" + profile.name + "' shows the notice and must NOT also render a 'matching assets' count.");
            ExtentReportManager.logPass("'" + profile.name + "': notice only, no matching-assets count — exclusive as pinned.");
        }
    }

    @Test(priority = 6, dataProvider = "allTypes",
          description = "TC_WTD_006: Auto-Schedule renders but stays disabled")
    public void testTC_WTD_006_AutoSchedulePresence(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_006 — " + profile.name);
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        // NOTE: this row deliberately sets NO Est. Hours and NO Field Technician —
        // the button must render but remain disabled until those preconditions exist.
        boolean present = workOrderPage.isAutoScheduleButtonPresent();
        for (int i = 0; i < 16 && !present; i++) {
            pause(500);
            present = workOrderPage.isAutoScheduleButtonPresent();
        }
        boolean enabled = workOrderPage.isAutoScheduleButtonEnabled();
        logStep("Auto-Schedule present=" + present + ", enabled=" + enabled
                + " (deEnergized=" + profile.deEnergized + ")");
        Assert.assertTrue(present,
                "Auto-Schedule button must render for '" + profile.name + "' (it renders for ALL 14 types).");
        Assert.assertFalse(enabled,
                "Auto-Schedule must stay DISABLED for '" + profile.name
                        + "' while Est. Hours + Field Technician are unset.");
        ExtentReportManager.logPass("Auto-Schedule present and correctly disabled for '" + profile.name + "'.");
    }

    @Test(priority = 7, dataProvider = "allTypes",
          description = "TC_WTD_007: Advanced defaults: Priority=Medium, Photo Type=FLIR-SEP")
    public void testTC_WTD_007_AdvancedDefaults(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_007 — " + profile.name);
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        String priority = workOrderPage.getPriorityValue();
        for (int i = 0; i < 12 && !"Medium".equals(priority); i++) {   // poll up to 6s for render
            pause(500);
            priority = workOrderPage.getPriorityValue();
        }
        String photoType = workOrderPage.getPhotoTypeValue();
        logStep("Advanced Settings after selecting '" + profile.name + "': Priority='" + priority
                + "', Photo Type='" + photoType + "'");
        Assert.assertEquals(priority, "Medium",
                "Priority must default to 'Medium' (Advanced Settings expanded by default) with '"
                        + profile.name + "' selected.");
        Assert.assertEquals(photoType, "FLIR-SEP",
                "Photo Type must default to 'FLIR-SEP' with '" + profile.name + "' selected.");
        ExtentReportManager.logPass("Defaults hold for '" + profile.name + "': Priority=Medium, Photo Type=FLIR-SEP.");
    }

    @Test(priority = 8, dataProvider = "allTypes",
          description = "TC_WTD_008: Dates survive a Work Type switch")
    public void testTC_WTD_008_DateDefaults(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_008 — " + profile.name);
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        String start = workOrderPage.getStartDateValue();
        for (int i = 0; i < 12 && start.isEmpty(); i++) {              // poll up to 6s for render
            pause(500);
            start = workOrderPage.getStartDateValue();
        }
        String due = workOrderPage.getDueDateValue();
        logStep("Dates with '" + profile.name + "' selected: Start='" + start + "', Due='" + due + "'");
        // These rows run against ONE reused dialog across all 14 types — so this pins that
        // switching Work Type does NOT clobber the Advanced Settings date fields.
        Assert.assertFalse(start.isEmpty(),
                "Start Date must stay pre-filled (today) after selecting '" + profile.name
                        + "' — type switching must not clobber Advanced Settings.");
        Assert.assertTrue(due.isEmpty(),
                "Due Date must stay empty after selecting '" + profile.name + "' (got '" + due + "').");
        ExtentReportManager.logPass("Dates stable for '" + profile.name + "': Start pre-filled, Due empty.");
    }

    @Test(priority = 9, dataProvider = "allTypes",
          description = "TC_WTD_009: All five dialog sections render")
    public void testTC_WTD_009_SectionsPresent(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_009 — " + profile.name);
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        List<String> missing = new ArrayList<>();
        for (String section : DIALOG_SECTIONS) {
            if (!workOrderPage.woFieldPresent(section)) missing.add(section);
        }
        logStep("Section check for '" + profile.name + "': missing=" + missing);
        Assert.assertTrue(missing.isEmpty(),
                "All five dialog sections (Scope/Team/Advanced Settings/Schedule/Equipment) must render for '"
                        + profile.name + "' — missing: " + missing);
        ExtentReportManager.logPass("All 5 sections present for '" + profile.name + "'.");
    }

    @Test(priority = 10, dataProvider = "allTypes",
          description = "TC_WTD_010: Required-field asterisks render")
    public void testTC_WTD_010_RequiredMarkers(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_010 — " + profile.name);
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        boolean woName = requiredLabelMarker("WO Name")
                || workOrderPage.getCreateDialogText().contains("WO Name / # *");
        boolean facility = requiredLabelMarker("Facility");
        boolean workType = requiredLabelMarker("Work Type");
        List<String> missing = new ArrayList<>();
        if (!woName) missing.add("WO Name");
        if (!facility) missing.add("Facility");
        if (!workType) missing.add("Work Type");
        logStep("Required markers with '" + profile.name + "': WO Name=" + woName
                + ", Facility=" + facility + ", Work Type=" + workType);
        Assert.assertTrue(missing.isEmpty(),
                "All three required-field asterisks must render with '" + profile.name
                        + "' selected — missing: " + missing);
        ExtentReportManager.logPass("Required markers intact for '" + profile.name + "' (WO Name*, Facility*, Work Type*).");
    }

    @Test(priority = 11, dataProvider = "adjacentPairs",
          description = "TC_WTD_011: Type switch retargets scope preview")
    public void testTC_WTD_011_TypeSwitchRetargets(Integer pairIdx) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE,
                "TC_WTD_011 — " + WorkTypeCatalog.ALL.get(pairIdx).name
                        + " -> " + WorkTypeCatalog.ALL.get(pairIdx + 1).name);
        WorkTypeProfile first = WorkTypeCatalog.ALL.get(pairIdx);
        WorkTypeProfile second = WorkTypeCatalog.ALL.get(pairIdx + 1);
        requireDialogOpen();
        requireTypeCommitted(first.name);
        logStep("Switching '" + first.name + "' -> '" + second.name + "' in the SAME open dialog");
        boolean switched = selectWorkTypeCommitted(second.name);
        String value = workOrderPage.getWorkTypeValue();
        logStep("switch committed=" + switched + ", value now '" + value + "'");
        Assert.assertTrue(switched,
                "Switching to '" + second.name + "' must commit (value stayed '" + value + "').");
        Assert.assertEquals(value, second.name,
                "After the switch the committed Work Type must be the SECOND type.");
        if (second.expectsScopePreview()) {
            int n = workOrderPage.getMatchingAssetsCount(20);
            logStep("Preview retargeted for '" + second.name + "': " + n + " matching asset(s)");
            Assert.assertTrue(n >= 0,
                    "After switching '" + first.name + "' -> '" + second.name
                            + "' the scope preview must retarget and settle on a positive count (got " + n + ").");
        }
        ExtentReportManager.logPass("Switch '" + first.name + "' -> '" + second.name + "' retargeted correctly.");
    }

    // ================================================================
    // PRIORITIES 12-13 — FRESH-dialog groups (close + reopen per row)
    // ================================================================

    @Test(priority = 12, dataProvider = "allTypes",
          description = "TC_WTD_012: Create gating: needs WO Name + Work Type")
    public void testTC_WTD_012_CreateGatingPerType(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_012 — " + profile.name);
        // Fresh dialog per row — gating depends on pristine WO Name state. NOTE (live-verified
        // 2026-07-21): the dialog KEEPS the WO Name as a sticky draft across Cancel + reopen, so
        // "fresh" is not enough — the previous row's name must be cleared explicitly or the
        // dual gate is already half-satisfied and Create lights up with the type alone.
        closeCreateDialogIfOpen();
        requireDialogOpen();
        workOrderPage.clearWoName();
        Assert.assertEquals(workOrderPage.getWoNameValue(), "",
                "Precondition: WO Name must be empty after clearing the sticky draft.");
        requireTypeCommitted(profile.name);
        boolean disabledWithTypeOnly = !workOrderPage.isCreateButtonEnabled();
        logStep("Type-only state ('" + profile.name + "', no WO Name): Create disabled=" + disabledWithTypeOnly);
        String woName = "WTGATE_" + System.currentTimeMillis();
        workOrderPage.fillWoName(woName);
        boolean enabledAfterName = false;
        for (int i = 0; i < 16 && !enabledAfterName; i++) {            // poll up to 8s
            enabledAfterName = workOrderPage.isCreateButtonEnabled();
            if (!enabledAfterName) pause(500);
        }
        logStep("After WO Name '" + woName + "': Create enabled=" + enabledAfterName);
        boolean cancelled = workOrderPage.cancelCreateDialog();        // never actually create
        logStep("Dialog cancelled (nothing created): " + cancelled);
        Assert.assertTrue(disabledWithTypeOnly,
                "Create must be DISABLED with Work Type '" + profile.name + "' committed but no WO Name.");
        Assert.assertTrue(enabledAfterName,
                "Create must ENABLE once WO Name + Work Type '" + profile.name
                        + "' are both set (Facility defaults to the current site).");
        ExtentReportManager.logPass("Create gating correct for '" + profile.name + "': type-only disabled, name+type enabled.");
    }

    @Test(priority = 13, dataProvider = "previewServices",
          description = "TC_WTD_013: Start Empty clears the auto-pulled scope")
    public void testTC_WTD_013_StartEmptyInstead(WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "TC_WTD_013 — " + profile.name);
        // Fresh dialog per row — Start Empty mutates scope state that must not leak between rows.
        closeCreateDialogIfOpen();
        requireDialogOpen();
        requireTypeCommitted(profile.name);
        int n = workOrderPage.getMatchingAssetsCount(20);
        logStep("Preview settled at " + n + " matching asset(s) for '" + profile.name + "'");
        Assert.assertTrue(n >= 0,
                "Precondition: the scope preview must settle for '" + profile.name
                        + "' before clicking Start Empty Instead (got " + n + ").");
        workOrderPage.clickStartEmptyInstead();
        boolean cleared = false;
        for (int i = 0; i < 10 && !cleared; i++) {                     // poll 10 x 500ms
            boolean allAssetsScope = workOrderPage.scopeDefaultsToAllAssets();
            boolean countGone = !matchingAssetsTextPresent();
            cleared = !allAssetsScope || countGone;
            if (!cleared) pause(500);
        }
        logStep("Scope after Start Empty Instead: cleared=" + cleared
                + " (allAssetsScope=" + workOrderPage.scopeDefaultsToAllAssets()
                + ", matchingAssetsText=" + matchingAssetsTextPresent() + ")");
        boolean cancelled = workOrderPage.cancelCreateDialog();
        logStep("Dialog cancelled: " + cancelled);
        Assert.assertTrue(cleared,
                "'Start Empty Instead' must clear the auto-pulled scope for '" + profile.name
                        + "' (all-assets copy gone OR the 'N matching assets' text removed).");
        ExtentReportManager.logPass("'Start Empty Instead' cleared the scope for '" + profile.name + "'.");
    }

    // ================================================================
    // CLEANUP
    // ================================================================

    @AfterClass(alwaysRun = true)
    public void closeDialogAfterClass() {
        closeCreateDialogIfOpen();
    }
}
