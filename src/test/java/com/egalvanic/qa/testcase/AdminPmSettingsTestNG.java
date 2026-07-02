package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.AdminPmSettingsPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ZP-323 — QA Automation: Admin — PM [ADMIN] — PM template config.
 *
 * Covers the Settings (/admin) → PM section → Offices template config area:
 * table render, search, pagination, Create Office (validation + language options
 * + helper text), Edit Office (rename + language change), Delete.
 *
 * DATA POLICY: mutates ONLY its own "AutoQA_PM_*" offices (created here, deleted
 * in @AfterClass). The 6 pre-existing offices (English/Francais/FrenchOffice/
 * Montreal/Navarnta/Sculptsoft) are read-only reference data.
 */
public class AdminPmSettingsTestNG extends BaseTest {

    private static final String MODULE = "Admin — PM";
    private static final String FEATURE = "PM template config (Settings → PM → Offices)";

    private AdminPmSettingsPage pmPage;

    private final String officeName = "AutoQA_PM_" + System.currentTimeMillis();
    private final String officeRenamed = officeName + "_Renamed";

    private int totalBefore = -1;      // pagination total before our create
    private boolean officeCreated;     // gates the edit/delete chain

    @BeforeClass(alwaysRun = true)
    public void pmSetup() {
        pmPage = new AdminPmSettingsPage(driver);
    }

    /** Best-effort cleanup: remove any office this class created (also stale AutoQA_PM_* from crashed runs). */
    @AfterClass(alwaysRun = true)
    public void pmCleanup() {
        try {
            if (!pmPage.isOnPmOffices()) pmPage.navigateToPmSection();
            pmPage.searchOffices("AutoQA_PM_");
            for (String stale : pmPage.getOfficeNames()) {
                if (!stale.startsWith("AutoQA_PM_")) continue;
                if (pmPage.openEditOffice(stale) && pmPage.clickDeleteAndConfirm()) {
                    System.out.println("[Cleanup] deleted office: " + stale);
                }
            }
            pmPage.searchOffices("");
        } catch (Exception e) {
            System.out.println("[Cleanup] AdminPm cleanup skipped: " + e.getMessage());
        }
    }

    private void requireOffices() {
        if (!pmPage.isOnPmOffices() && !pmPage.navigateToPmSection()) {
            throw new SkipException("PM → Offices area not reachable this run.");
        }
    }

    // ================================================================
    // TC01 — navigation + table render
    // ================================================================
    @Test(priority = 1, description = "ZP-323: Settings → PM opens the Offices template config table")
    public void testPM_01_NavigateAndTableRenders() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_01_NavigateAndTableRenders");
        Assert.assertTrue(pmPage.navigateToPmSection(), "Settings → PM (Offices) should open from the left nav.");
        List<String> headers = pmPage.getOfficeTableHeaders();
        logStep("Office table headers: " + headers);
        Assert.assertTrue(headers.stream().anyMatch(h -> h.contains("Name")),
                "Offices table should have a Name column. Headers: " + headers);
        Assert.assertTrue(headers.stream().anyMatch(h -> h.contains("Default Language")),
                "Offices table should have a Default Language column. Headers: " + headers);
        Assert.assertTrue(pmPage.waitForOfficeRows(20), "Offices table should render at least one row (rows load async).");
        logStepWithScreenshot("PM → Offices table");
        ExtentReportManager.logPass("PM section renders the Offices config table: " + headers);
    }

    // ================================================================
    // TC02 — pagination footer reconciles with row count
    // ================================================================
    @Test(priority = 2, description = "ZP-323: Offices pagination footer matches the rendered rows")
    public void testPM_02_PaginationReconciles() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_02_PaginationReconciles");
        requireOffices();
        String footer = pmPage.getPaginationText();
        int total = pmPage.getOfficeTotal();
        int rows = pmPage.getOfficeNames().size();
        logStep("Pagination: '" + footer + "' | total=" + total + " | visible rows=" + rows);
        Assert.assertTrue(total >= 1, "Pagination footer should expose a total ('" + footer + "').");
        // one page (default 25/page): rendered rows must equal the reported total
        if (total <= 25) {
            Assert.assertEquals(rows, total, "Rendered row count should reconcile with the footer total.");
        }
        totalBefore = total;
        ExtentReportManager.logPass("Pagination reconciles: " + footer + " with " + rows + " rows.");
    }

    // ================================================================
    // TC03 — search filters the table and clears back
    // ================================================================
    @Test(priority = 3, description = "ZP-323: Search Offices filters rows and clearing restores them")
    public void testPM_03_SearchFilterAndClear() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_03_SearchFilterAndClear");
        requireOffices();
        List<String> all = pmPage.getOfficeNames();
        if (all.isEmpty()) throw new SkipException("No offices to search against.");
        String target = all.get(all.size() - 1);

        Assert.assertTrue(pmPage.searchOffices(target), "Search Offices input should accept text.");
        List<String> filtered = pmPage.getOfficeNames();
        logStep("Search '" + target + "' -> " + filtered);
        Assert.assertTrue(filtered.contains(target), "Filtered rows should contain '" + target + "'.");
        Assert.assertTrue(filtered.size() <= all.size(), "Filtering should not grow the row set.");

        pmPage.searchOffices("");
        List<String> restored = pmPage.getOfficeNames();
        logStepWithScreenshot("Search cleared");
        Assert.assertEquals(restored.size(), all.size(), "Clearing search should restore all rows.");
        ExtentReportManager.logPass("Search filters to " + filtered.size() + " row(s) and clears back to " + restored.size() + ".");
    }

    // ================================================================
    // TC04 — Create Office dialog: required-name validation
    // ================================================================
    @Test(priority = 4, description = "ZP-323: Create Office Save stays disabled until Name is filled")
    public void testPM_04_CreateValidation() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_04_CreateValidation");
        requireOffices();
        Assert.assertTrue(pmPage.openCreateOffice(), "Add Office should open the Create Office dialog.");
        Assert.assertFalse(pmPage.isSaveEnabled(), "Save must be DISABLED while Name is empty (required field).");
        Assert.assertTrue(pmPage.setOfficeName("x"), "Name field should accept input.");
        Assert.assertTrue(pmPage.isSaveEnabled(), "Save should ENABLE once Name is non-empty.");
        pmPage.setOfficeName("");
        Assert.assertFalse(pmPage.isSaveEnabled(), "Save should re-disable when Name is cleared.");
        logStepWithScreenshot("Create Office validation");
        pmPage.clickCancel();
        Assert.assertTrue(pmPage.waitDialogClosed(10), "Cancel should close the dialog.");
        ExtentReportManager.logPass("Required-name validation gates the Save button correctly.");
    }

    // ================================================================
    // TC05 — language options + helper text (the 'template' semantics)
    // ================================================================
    @Test(priority = 5, description = "ZP-323: Default Language offers login-default/English/Français with the propagation helper text")
    public void testPM_05_LanguageOptionsAndHelper() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_05_LanguageOptionsAndHelper");
        requireOffices();
        Assert.assertTrue(pmPage.openCreateOffice(), "Add Office should open the Create Office dialog.");
        Assert.assertTrue(pmPage.hasLanguageHelperText(),
                "Dialog should explain the language default propagates to sites/accounts/users.");
        List<String> opts = pmPage.getLanguageOptions();
        logStep("Language options: " + opts);
        Assert.assertTrue(opts.contains("English"), "Language options must include English. Got: " + opts);
        Assert.assertTrue(opts.stream().anyMatch(o -> o.startsWith("Fran")),
                "Language options must include Français. Got: " + opts);
        Assert.assertTrue(opts.stream().anyMatch(o -> o.toLowerCase().contains("login language")),
                "Language options must include the 'Use login language' default. Got: " + opts);
        logStepWithScreenshot("Language options");
        pmPage.clickCancel();
        pmPage.waitDialogClosed(10);
        ExtentReportManager.logPass("Language template options verified: " + opts);
    }

    // ================================================================
    // TC06 — Cancel does not create
    // ================================================================
    @Test(priority = 6, description = "ZP-323: Cancelling Create Office persists nothing")
    public void testPM_06_CancelDoesNotCreate() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_06_CancelDoesNotCreate");
        requireOffices();
        String ghost = "AutoQA_PM_Ghost_" + System.currentTimeMillis();
        Assert.assertTrue(pmPage.openCreateOffice(), "Add Office should open the Create Office dialog.");
        pmPage.setOfficeName(ghost);
        pmPage.clickCancel();
        Assert.assertTrue(pmPage.waitDialogClosed(10), "Cancel should close the dialog.");
        Assert.assertFalse(pmPage.waitForOfficeRow(ghost, true, 5),
                "Cancelled office '" + ghost + "' must NOT appear in the table.");
        ExtentReportManager.logPass("Cancel discards the form — no ghost office created.");
    }

    // ================================================================
    // TC07 — create office (CRUD: C)
    // ================================================================
    @Test(priority = 7, description = "ZP-323: Creating an office persists it to the Offices table")
    public void testPM_07_CreateOffice() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_07_CreateOffice");
        requireOffices();
        if (totalBefore < 0) totalBefore = pmPage.getOfficeTotal();

        Assert.assertTrue(pmPage.openCreateOffice(), "Add Office should open the Create Office dialog.");
        Assert.assertTrue(pmPage.setOfficeName(officeName), "Name should accept '" + officeName + "'.");
        Assert.assertTrue(pmPage.selectLanguage("English"), "Should select Default Language = English.");
        Assert.assertTrue(pmPage.clickSave(), "Save should be clickable.");
        Assert.assertTrue(pmPage.waitDialogClosed(15), "Dialog should close after Save.");

        Assert.assertTrue(pmPage.waitForOfficeRow(officeName, true, 15),
                "Created office '" + officeName + "' should appear in the table.");
        int totalAfter = pmPage.getOfficeTotal();
        logStep("Office total: " + totalBefore + " -> " + totalAfter);
        Assert.assertEquals(totalAfter, totalBefore + 1, "Pagination total should increment after create.");
        officeCreated = true;
        logStepWithScreenshot("Office created");
        ExtentReportManager.logPass("Office '" + officeName + "' created (total " + totalBefore + " → " + totalAfter + ").");
    }

    // ================================================================
    // TC08 — edit dialog opens prefilled with Delete/Cancel/Save
    // ================================================================
    @Test(priority = 8, description = "ZP-323: Row click opens Edit Office prefilled, with Delete/Cancel/Save")
    public void testPM_08_EditDialogPrefilled() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_08_EditDialogPrefilled");
        requireOffices();
        if (!officeCreated) throw new SkipException("Create (PM_07) did not run/pass — no own office to edit.");

        Assert.assertTrue(pmPage.openEditOffice(officeName), "Row click should open Edit Office for '" + officeName + "'.");
        Assert.assertEquals(pmPage.getOfficeNameValue(), officeName, "Edit dialog should be prefilled with the office name.");
        List<String> btns = pmPage.dialogButtons();
        logStep("Edit dialog buttons: " + btns);
        for (String b : new String[]{"Delete", "Cancel", "Save"}) {
            Assert.assertTrue(btns.contains(b), "Edit dialog should offer '" + b + "'. Got: " + btns);
        }
        logStepWithScreenshot("Edit Office dialog");
        pmPage.clickCancel();
        pmPage.waitDialogClosed(10);
        ExtentReportManager.logPass("Edit dialog prefilled with Delete/Cancel/Save controls.");
    }

    // ================================================================
    // TC09 — rename (CRUD: U on name)
    // ================================================================
    @Test(priority = 9, description = "ZP-323: Renaming an office persists to the table")
    public void testPM_09_RenameOffice() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_09_RenameOffice");
        requireOffices();
        if (!officeCreated) throw new SkipException("Create (PM_07) did not run/pass — no own office to rename.");

        Assert.assertTrue(pmPage.openEditOffice(officeName), "Edit Office should open for '" + officeName + "'.");
        Assert.assertTrue(pmPage.setOfficeName(officeRenamed), "Name should accept the rename.");
        Assert.assertTrue(pmPage.clickSave(), "Save should be clickable.");
        Assert.assertTrue(pmPage.waitDialogClosed(15), "Dialog should close after Save.");
        Assert.assertTrue(pmPage.waitForOfficeRow(officeRenamed, true, 15),
                "Renamed office '" + officeRenamed + "' should appear.");
        Assert.assertTrue(pmPage.waitForOfficeRow(officeName, false, 10),
                "Old name '" + officeName + "' should be gone.");
        logStepWithScreenshot("Office renamed");
        ExtentReportManager.logPass("Rename persisted: " + officeName + " → " + officeRenamed);
    }

    // ================================================================
    // TC10 — change default language (CRUD: U on config; the core template config)
    // ================================================================
    @Test(priority = 10, description = "ZP-323: Changing an office's Default Language persists to the table")
    public void testPM_10_ChangeLanguage() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_10_ChangeLanguage");
        requireOffices();
        if (!officeCreated) throw new SkipException("Create (PM_07) did not run/pass — no own office to configure.");

        Assert.assertTrue(pmPage.openEditOffice(officeRenamed), "Edit Office should open for '" + officeRenamed + "'.");
        Assert.assertTrue(pmPage.selectLanguage("Français"), "Should select Français.");
        Assert.assertTrue(pmPage.clickSave(), "Save should be clickable.");
        Assert.assertTrue(pmPage.waitDialogClosed(15), "Dialog should close after Save.");

        String lang = "";
        for (int i = 0; i < 15; i++) {
            lang = pmPage.getOfficeLanguage(officeRenamed);
            if (lang.startsWith("Fran")) break;
            pause(1000);
        }
        logStep("Language cell after change: '" + lang + "'");
        Assert.assertTrue(lang.startsWith("Fran"),
                "Table should show Français for '" + officeRenamed + "' (got '" + lang + "').");
        logStepWithScreenshot("Language changed to Français");
        ExtentReportManager.logPass("Default Language config persisted: English → Français.");
    }

    // ================================================================
    // TC11 — delete own office (CRUD: D) + total restored
    // ================================================================
    @Test(priority = 11, description = "ZP-323: Deleting an office removes the row and restores the total")
    public void testPM_11_DeleteOffice() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_11_DeleteOffice");
        requireOffices();
        if (!officeCreated) throw new SkipException("Create (PM_07) did not run/pass — no own office to delete.");

        Assert.assertTrue(pmPage.openEditOffice(officeRenamed), "Edit Office should open for '" + officeRenamed + "'.");
        Assert.assertTrue(pmPage.clickDeleteAndConfirm(), "Delete should complete and close the dialog(s).");
        Assert.assertTrue(pmPage.waitForOfficeRow(officeRenamed, false, 15),
                "Deleted office '" + officeRenamed + "' should disappear from the table.");
        int totalAfter = pmPage.getOfficeTotal();
        logStep("Office total after delete: " + totalAfter + " (baseline " + totalBefore + ")");
        Assert.assertEquals(totalAfter, totalBefore, "Pagination total should return to the pre-create baseline.");
        officeCreated = false;
        logStepWithScreenshot("Office deleted");
        ExtentReportManager.logPass("Delete removed the office; totals reconciled back to " + totalAfter + ".");
    }

    // ================================================================
    // TC12 — section switching keeps PM state reachable (smoke on the switcher)
    // ================================================================
    @Test(priority = 12, description = "ZP-323: Switching Settings sections away and back re-renders PM → Offices")
    public void testPM_12_SectionSwitchRoundtrip() {
        ExtentReportManager.createTest(MODULE, FEATURE, "PM_12_SectionSwitchRoundtrip");
        requireOffices();
        // hop to Sites and back to PM via the page-object entry point
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var b=[].slice.call(document.querySelectorAll('button')).find(function(e){"
              + "return (e.textContent||'').trim()==='Sites' && e.offsetParent;});if(b)b.click();");
            pause(2500);
        } catch (Exception ignored) {}
        Assert.assertTrue(pmPage.navigateToPmSection(), "PM section should re-open after visiting Sites.");
        Assert.assertTrue(pmPage.waitForOfficeRows(20), "Offices table should re-render rows after the roundtrip (async load).");
        logStepWithScreenshot("PM section after Sites roundtrip");
        ExtentReportManager.logPass("Settings section switcher roundtrip keeps PM → Offices healthy.");
    }
}
