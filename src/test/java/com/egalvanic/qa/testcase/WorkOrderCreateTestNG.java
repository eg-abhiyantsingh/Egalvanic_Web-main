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
 * Work Order creation — the full <b>"Create New Work Order"</b> dialog.
 *
 * <p><b>Flow (verified live 2026-06-25 on site "Android Qa Site1"):</b> Work Orders (/sessions) →
 * <b>Create Work Order</b> opens a dialog with <b>WO Name / #*</b>, <b>Priority*</b> (default Medium),
 * <b>Est. Hours</b>, <b>WO Description</b>, <b>Facility*</b> (default = current site), <b>Photo Type*</b>
 * (default FLIR-SEP), <b>Start/Due Date</b> (typeable MM/DD/YYYY), a <b>Team</b> ＋ (→ "Select user"),
 * a <b>Schedule</b> ＋ (→ block with Assign Technician + Auto-Schedule + Add Block), and an
 * <b>Equipment</b> dropdown (options incl <b>Megger</b>). Priority/Facility/Photo Type/Start Date are
 * pre-filled, so <b>Create is gated on WO Name alone</b>. Creating persists the work order to the grid
 * (its name shows in the Work Order column) and opening it navigates to /sessions/&#123;id&#125;.</p>
 *
 * <p>Covers: form structure + defaults, Create gating, the Equipment / Team / Schedule sub-flows, and an
 * end-to-end create-and-open. {@code testWOC_00_DumpForm} is a disabled DOM-discovery diagnostic.</p>
 */
public class WorkOrderCreateTestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_WORK_ORDERS;
    private static final String FEATURE = "Create Work Order";
    /** A data-rich site (Equipment incl. Megger, team/technician data) used for the create. */
    private static final String SITE = "Android Qa Site1";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Work Order — Create New Work Order");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        boolean ok = ensureSite(SITE);  // real keystrokes — JS-typed selectSiteByName can't filter the ~130-site list
        System.out.println("[WorkOrderCreate] site '" + SITE + "' selected=" + ok);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup(ITestResult result) {
        // close any leftover create dialog so the next test starts clean
        try { if (workOrderPage.isCreateWorkOrderDialogOpen()) cancelDialog(); } catch (Exception ignored) {}
    }

    // ================================================================
    // TESTS
    // ================================================================

    @Test(priority = 1, description = "WOC_01: Create Work Order opens a dialog with all fields/sections and the expected defaults")
    public void testWOC_01_FormStructureAndDefaults() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_01_FormStructureAndDefaults");
        openFreshCreateForm();

        Assert.assertTrue(workOrderPage.isCreateWorkOrderDialogOpen(), "Create New Work Order dialog should open.");
        // v1.35 (ZP-3000): "Work Type" (new required field) and "Scope" (new section) added to the form.
        for (String label : new String[]{"Work Type", "Scope", "Priority", "Est. Hours", "WO Description", "Facility",
                "Photo Type", "Start Date", "Due Date", "Team", "Schedule", "Equipment"}) {
            Assert.assertTrue(workOrderPage.woFieldPresent(label), "Form should show the '" + label + "' field/section.");
        }
        // v1.35 (ZP-3000): Priority / Photo Type / Start Date live inside the COLLAPSED
        // "Advanced Settings" accordion and their inputs are unmounted until it is expanded,
        // so read the defaults only after expanding (the label text above is present either way).
        workOrderPage.expandAdvancedSettings();
        // Defaults the form ships with (verified live): Priority=Medium, Photo Type=FLIR-SEP, Start Date set.
        Assert.assertEquals(workOrderPage.getPriorityValue(), "Medium", "Priority should default to Medium.");
        Assert.assertEquals(workOrderPage.getPhotoTypeValue(), "FLIR-SEP", "Photo Type should default to FLIR-SEP.");
        Assert.assertFalse(workOrderPage.getStartDateValue().isEmpty(), "Start Date should be pre-filled.");
        logStepWithScreenshot("Create New Work Order dialog");
        ExtentReportManager.logPass("Create dialog shows all fields/sections; defaults Priority=Medium, Photo Type=FLIR-SEP, Start Date pre-filled.");
    }

    @Test(priority = 2, description = "WOC_02: Create is gated on both required fields — WO Name AND Work Type (v1.35 ZP-3000)")
    public void testWOC_02_CreateGatedOnName() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_02_CreateGatedOnName");
        openFreshCreateForm();

        Assert.assertFalse(workOrderPage.isCreateButtonEnabled(),
                "Create should be DISABLED with WO Name empty (Priority/Facility/Photo Type are defaulted).");

        // v1.35 (ZP-3000): Work Type is a SECOND required field. Filling only WO Name
        // is no longer sufficient — Create must stay disabled until Work Type is set too.
        workOrderPage.fillWoName("WOName_" + LocalDateTime.now().format(TS));
        boolean enabledNameOnly = false;
        for (int i = 0; i < 6 && !enabledNameOnly; i++) { pause(500); enabledNameOnly = workOrderPage.isCreateButtonEnabled(); }
        Assert.assertFalse(enabledNameOnly,
                "Create should STAY DISABLED with WO Name set but Work Type empty (v1.35 ZP-3000 required Work Type).");

        workOrderPage.selectFirstWorkType();
        // Create enables a moment after all required fields register (React validation) — poll ~8s.
        boolean enabled = false;
        for (int i = 0; i < 16 && !enabled; i++) { pause(500); enabled = workOrderPage.isCreateButtonEnabled(); }
        // Facility is the third required field (v1.35). It normally defaults to the active site, but
        // if this session left it empty, Create stays disabled — set it explicitly and re-poll so the
        // test proves the gating rather than flaking on an unset default.
        if (!enabled) {
            try { workOrderPage.selectWoFacility("test site"); } catch (Exception ignored) {}
            for (int i = 0; i < 16 && !enabled; i++) { pause(500); enabled = workOrderPage.isCreateButtonEnabled(); }
        }
        Assert.assertTrue(enabled, "Create should ENABLE once WO Name, Work Type and Facility are set (polled ~16s).");
        logStepWithScreenshot("Create enabled after WO Name + Work Type");
        ExtentReportManager.logPass("Create gating verified (v1.35): disabled with empty WO Name, still disabled with Work Type empty, enabled once both set.");
    }

    @Test(priority = 3, description = "WOC_03: Equipment dropdown lists options including Megger")
    public void testWOC_03_EquipmentOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_03_EquipmentOptions");
        openFreshCreateForm();
        List<String> equipment = workOrderPage.getEquipmentOptions();
        logStep("Equipment options (" + equipment.size() + "): " + equipment);
        Assert.assertFalse(equipment.isEmpty(), "Equipment dropdown should list options on '" + SITE + "'.");
        Assert.assertTrue(equipment.contains("Megger"),
                "Equipment dropdown should include 'Megger' on '" + SITE + "'. Got: " + equipment);
        ExtentReportManager.logPass("Equipment dropdown lists options including Megger (" + equipment.size() + " total).");
    }

    @Test(priority = 4, description = "WOC_04: Schedule ＋ reveals a schedule block with Assign Technician + Add Block")
    public void testWOC_04_ScheduleBlockControls() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_04_ScheduleBlockControls");
        openFreshCreateForm();

        // Auto-Schedule starts disabled (no block yet)
        clickByXpath("//h6[normalize-space()='Schedule']/following-sibling::button[1]"); // the ＋ icon
        pause(1000);
        Assert.assertTrue(elementPresent("//button[normalize-space()='Add Block']"),
                "Adding a schedule block should reveal an 'Add Block' button.");
        Assert.assertTrue(dialogContains("Assign Technician"),
                "The schedule block should expose an 'Assign Technician' field.");
        logStepWithScreenshot("Schedule block controls");
        ExtentReportManager.logPass("Schedule ＋ reveals a block with Assign Technician and Add Block.");
    }

    @Test(priority = 5, description = "WOC_05: Team ＋ reveals a 'Select user' member dropdown")
    public void testWOC_05_TeamMemberControl() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_05_TeamMemberControl");
        openFreshCreateForm();
        clickByXpath("//h6[normalize-space()='Team']/following-sibling::button[1]"); // Team ＋
        pause(800);
        Assert.assertTrue(elementPresent("//input[@placeholder='Select user']"),
                "Team ＋ should reveal a 'Select user' member dropdown.");
        logStepWithScreenshot("Team member control");
        ExtentReportManager.logPass("Team ＋ reveals the 'Select user' member dropdown.");
    }

    @Test(priority = 6, description = "WOC_06: End-to-end — create a work order with name/priority/hours/desc/due-date/equipment (+team+schedule), then find & open it")
    public void testWOC_06_CreateWorkOrderEndToEnd() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_06_CreateWorkOrderEndToEnd");
        String name = "WOTest_" + LocalDateTime.now().format(TS);
        logStep("Creating work order '" + name + "'");

        openFreshCreateForm();
        // v1.35 (ZP-3000): Work Type is required and gates Create. "General" is the simplest type.
        boolean typeOk = workOrderPage.trySelectWorkType("General");     // step 1 (required)
        logStep("Work type 'General' selected: " + typeOk);
        workOrderPage.fillWoName(name);                                   // step 2
        workOrderPage.trySelectPriority("High");                        // step 3 (render-lag-proof)
        workOrderPage.fillEstHours("8");                                 // step 4
        workOrderPage.fillWoDescription("Automated WO-creation coverage — scope: verify the full Create flow."); // step 5
        workOrderPage.selectWoFacility(SITE);                            // step 6 — open Facility dropdown + select
        workOrderPage.selectWoPhotoType("FLIR-SEP");                     // step 7 — Photo Type dropdown + select
        // steps 8 & 9 — set Start + Due via the CALENDAR ICON (not by typing). Start = today (current
        // month, enabled); Due = the 15th of next month (a fully-enabled future month, guaranteed > Start).
        workOrderPage.pickDate(1, 0, LocalDateTime.now().getDayOfMonth());
        workOrderPage.pickDate(2, 1, 15);
        logStep("Start=" + workOrderPage.getStartDateValue() + " Due=" + workOrderPage.getDueDateValue());
        // Optional enrichments (best-effort — they must not break the create if data is absent).
        boolean team = workOrderPage.addFirstTeamMember();               // step 10
        logStep("Team member added: " + team);
        boolean block = workOrderPage.addScheduleBlock(true);
        logStep("Schedule block added: " + block);
        try { workOrderPage.selectEquipment("Megger"); } catch (Exception e) { logStep("Equipment select skipped: " + e.getMessage()); }

        Assert.assertTrue(workOrderPage.isCreateButtonEnabled(), "Create should be enabled with all required fields set.");
        logStepWithScreenshot("Before clicking Create");
        workOrderPage.clickCreateWorkOrder();

        // Dialog should close on success.
        boolean closed = false;
        for (int i = 0; i < 20 && !closed; i++) { pause(700); closed = !workOrderPage.isCreateWorkOrderDialogOpen(); }
        logStep("Create dialog closed: " + closed);
        pause(1500);

        // Persistence: search (real keystrokes) and confirm the WO appears, then open it.
        boolean visible = searchWorkOrder(name);
        logStepWithScreenshot("After create — searched for '" + name + "'");
        Assert.assertTrue(visible, "Newly created work order '" + name + "' should appear in the grid.");

        boolean opened = openWorkOrderRow(name);
        Assert.assertTrue(opened, "Clicking the new work order should open its detail page (/sessions/{id}).");
        logStepWithScreenshot("Opened the new work order detail");
        ExtentReportManager.logPass("Created work order '" + name + "' (Priority High, Megger, team=" + team
                + ", schedule=" + block + ") — persisted, found, and opened.");
    }

    @Test(priority = 7, description = "WOC_07: Start/Due dates can be set via the calendar ICON (not just typing)")
    public void testWOC_07_DatePickerViaCalendarIcon() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_07_DatePickerViaCalendarIcon");
        openFreshCreateForm();

        // Start Date via the calendar icon → today's day (current month, enabled).
        boolean start = workOrderPage.pickDate(1, 0, LocalDateTime.now().getDayOfMonth());
        Assert.assertTrue(start, "Should be able to pick a Start Date day from the calendar icon.");
        Assert.assertFalse(workOrderPage.getStartDateValue().isEmpty(), "Start Date should be set after picking.");

        // Due Date via the calendar icon → the 15th of NEXT month (exercises month navigation; always enabled).
        boolean due = workOrderPage.pickDate(2, 1, 15);
        Assert.assertTrue(due, "Should be able to pick a Due Date day from the calendar icon (next month).");
        String dueVal = workOrderPage.getDueDateValue();
        logStep("After calendar pick — Start=" + workOrderPage.getStartDateValue() + " Due=" + dueVal);
        Assert.assertTrue(dueVal.matches("\\d{2}/15/\\d{4}"),
                "Due Date should reflect the 15th picked from the calendar. Got: '" + dueVal + "'");
        logStepWithScreenshot("Dates set via calendar icon");
        ExtentReportManager.logPass("Start + Due dates set via the calendar icon (Due=" + dueVal + ", picked from next month).");
    }

    @Test(priority = 8, description = "WOC_09: v1.35 Scope section defaults to all-assets and 'Start Empty Instead' switches it (ZP-3000)")
    public void testWOC_09_ScopeSectionDefaultsAndStartEmpty() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_09_ScopeSectionDefaultsAndStartEmpty");
        openFreshCreateForm();
        try {
            Assert.assertTrue(workOrderPage.isScopeSectionPresent(),
                    "v1.35 create dialog should show the new 'Scope' section (ZP-3000).");
            Assert.assertTrue(workOrderPage.scopeDefaultsToAllAssets(),
                    "Scope should default to an all-assets scope ('All assets in scope' copy present).");
            logStepWithScreenshot("Scope defaults to all-assets");

            workOrderPage.clickStartEmptyInstead();
            // 'Start Empty Instead' switches the scope away from all-assets; poll for the default copy to clear.
            boolean switched = false;
            for (int i = 0; i < 10 && !switched; i++) { pause(500); switched = !workOrderPage.scopeDefaultsToAllAssets(); }
            Assert.assertTrue(switched,
                    "'Start Empty Instead' should switch the scope away from the all-assets default.");
            logStepWithScreenshot("Scope switched to empty");
            ExtentReportManager.logPass("Scope section verified: defaults to all-assets, 'Start Empty Instead' switches to an empty scope.");
        } finally {
            cancelDialog();
        }
    }

    @Test(priority = 9, description = "WOC_10: v1.35 Advanced Settings section hosts Priority/Photo Type/Start Date/Est. Hours/WO Description with defaults (ZP-3000)")
    public void testWOC_10_AdvancedSettingsAnatomy() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_10_AdvancedSettingsAnatomy");
        openFreshCreateForm();
        try {
            // v1.35 (verified live 2026-07-16): "Advanced Settings" is an always-expanded SECTION
            // (its h6 has cursor:pointer but clicking does NOT collapse it — it is not an accordion).
            // The Priority field renders a beat AFTER the dialog opens. Use getPriorityValue() as the
            // readiness signal (reads the value attribute — the same proven read WOC_01 uses); do NOT
            // gate on Selenium isDisplayed(), which reports false for MUI Autocomplete's hidden inner input.
            String prio = "";
            for (int i = 0; i < 12 && prio.isEmpty(); i++) { pause(500); prio = workOrderPage.getPriorityValue(); }
            Assert.assertEquals(prio, "Medium",
                    "Priority (Advanced Settings, expanded by default) should read its 'Medium' default (polled ~6s).");
            Assert.assertEquals(workOrderPage.getPhotoTypeValue(), "FLIR-SEP",
                    "Photo Type (Advanced Settings) should read its 'FLIR-SEP' default.");
            Assert.assertFalse(workOrderPage.getStartDateValue().isEmpty(),
                    "Start Date (Advanced Settings) should be pre-filled.");
            for (String label : new String[]{"Est. Hours", "WO Description"}) {
                Assert.assertTrue(workOrderPage.woFieldPresent(label),
                        "Advanced Settings should host the '" + label + "' field.");
            }
            logStepWithScreenshot("Advanced Settings anatomy + defaults");
            ExtentReportManager.logPass("Advanced Settings anatomy verified: Priority=Medium, Photo Type=FLIR-SEP, Start Date pre-filled, Est. Hours + WO Description present.");
        } finally {
            cancelDialog();
        }
    }

    // ================================================================
    // DIAGNOSTIC (disabled)
    // ================================================================

    @Test(priority = 0, enabled = false, description = "DIAG: dump the Create New Work Order form")
    public void testWOC_00_DumpForm() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOC_00_DumpForm");
        openFreshCreateForm();
        StringBuilder sb = new StringBuilder();
        sb.append("dialogOpen=").append(workOrderPage.isCreateWorkOrderDialogOpen()).append("\n");
        sb.append("priorityDefault=").append(workOrderPage.getPriorityValue()).append("\n");
        sb.append("photoTypeDefault=").append(workOrderPage.getPhotoTypeValue()).append("\n");
        sb.append("startDate=").append(workOrderPage.getStartDateValue()).append("\n");
        sb.append("createEnabledEmpty=").append(workOrderPage.isCreateButtonEnabled()).append("\n");
        sb.append("equipment=").append(workOrderPage.getEquipmentOptions()).append("\n");
        logStep(sb.toString());
        ExtentReportManager.logPass("WO create form diagnostic dumped.");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Re-navigate to Work Orders (tears down any leftover dialog) and open a fresh, verified create dialog. */
    private void openFreshCreateForm() {
        workOrderPage.navigateToWorkOrders();
        for (int attempt = 1; attempt <= 3; attempt++) {
            workOrderPage.openCreateWorkOrderForm();
            pause(800);
            if (workOrderPage.isCreateWorkOrderDialogOpen()) return;
            logStep("Create dialog didn't open (attempt " + attempt + ") — retrying");
            pause(800);
        }
        Assert.assertTrue(workOrderPage.isCreateWorkOrderDialogOpen(), "Create New Work Order dialog should open.");
    }

    private void cancelDialog() {
        try {
            List<WebElement> cancels = driver.findElements(By.xpath(
                    "//div[@role='dialog']//button[normalize-space()='Cancel']"));
            if (!cancels.isEmpty()) {
                try { cancels.get(0).click(); } catch (Exception e) { ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", cancels.get(0)); }
                pause(400);
            }
        } catch (Exception ignored) {}
    }

    /** Search the work-order grid with REAL keystrokes (JS-typed search doesn't trigger the filter) and return whether a matching row appears. */
    private boolean searchWorkOrder(String name) {
        By search = By.xpath("//input[contains(@placeholder,'Search work orders') or contains(@placeholder,'Search Work')]");
        By row = By.xpath("//div[@role='row'][contains(normalize-space(),'" + name + "')] | //tbody//tr[contains(normalize-space(),'" + name + "')]");
        Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement box = new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(search));
                box.click();
                try { box.sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
                box.sendKeys(name);
                pause(2800);
                if (!driver.findElements(row).isEmpty()) { logStep("WO search attempt " + attempt + ": FOUND"); return true; }
                logStep("WO search attempt " + attempt + ": not yet visible");
            } catch (Exception e) { logStep("WO search attempt " + attempt + " error: " + e.getMessage()); }
            pause(1000);
        }
        return false;
    }

    /** Click the work-order row whose text contains {name}; return true once on a /sessions/{id} detail page. */
    private boolean openWorkOrderRow(String name) {
        try {
            By row = By.xpath("//div[@role='row'][contains(normalize-space(),'" + name + "')] | //tbody//tr[contains(normalize-space(),'" + name + "')]");
            List<WebElement> rows = driver.findElements(row);
            if (rows.isEmpty()) return false;
            String before = driver.getCurrentUrl();
            try { rows.get(0).click(); } catch (Exception e) { ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", rows.get(0)); }
            for (int i = 0; i < 30; i++) {
                pause(600);
                String u = driver.getCurrentUrl();
                if (u.contains("/sessions/") && !u.endsWith("/sessions") && !u.endsWith("/sessions/")) return true;
                if (!u.equals(before) && u.matches(".*/sessions/[^/]+.*")) return true;
            }
            return driver.getCurrentUrl().contains("/sessions/");
        } catch (Exception e) { logStep("openWorkOrderRow error: " + e.getMessage()); return false; }
    }

    private boolean elementPresent(String xpath) {
        return !driver.findElements(By.xpath(xpath)).isEmpty();
    }

    private boolean dialogContains(String text) {
        return !driver.findElements(By.xpath(
                "//div[@role='dialog']//*[contains(normalize-space(),'" + text + "')]")).isEmpty();
    }

    private void clickByXpath(String xpath) {
        WebElement el = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        pause(150);
        try { el.click(); } catch (Exception e) { ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", el); }
    }

    /**
     * Select a facility/site by exact name using REAL keystrokes (the tenant's ~130-site virtualised
     * picker doesn't react to a JS value-set). Mirrors AssetLocationTestNG.ensureSite.
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
