package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.ArcFlashPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Arc Flash Readiness — data-driven <b>DataGrid column matrix</b>: every column of all three grids is
 * verified for <b>presence</b> and for <b>sortability</b>. 23 columns × 2 = 46 parameterised tests.
 *
 * <p>Columns (live-verified 2026-06-30): Asset Details (6) · Source/Target (7) · Connection Details (10).
 * Ordered by tab so each tab loads once. Engineering Mode is ON (the superset of columns).</p>
 */
public class ArcFlashGridMatrixTestNG extends BaseTest {

    private static final String MODULE = "Arc Flash Readiness";
    private static final String FEATURE = "Grid column matrix";
    private static final String SITE = "Android Qa Site1";

    private ArcFlashPage arcFlashPage;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Arc Flash Readiness — DataGrid column matrix (presence + sort)");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        arcFlashPage = new ArcFlashPage(driver);
        ensureSite(SITE);
        // Warm every tab's grid ONCE (Source/Target is the slow one) so the per-column tests run fast.
        try {
            arcFlashPage.navigateToArcFlash();
            arcFlashPage.setEngineeringMode(true);
            arcFlashPage.clickTab("Asset Details"); arcFlashPage.waitForAssetClassFilter(); arcFlashPage.waitForTableRows();
            arcFlashPage.clickTab("Source/Target Connections"); arcFlashPage.waitForTableRows();
            arcFlashPage.clickTab("Connection Details"); arcFlashPage.waitForBuswayReadiness(); arcFlashPage.waitForTableRows();
        } catch (Exception ignored) {}
    }

    /** {tab, column} for every column of the three grids, ordered by tab. */
    @DataProvider(name = "columns")
    public Object[][] columns() {
        return new Object[][]{
            {"Asset Details", "Label"},
            {"Asset Details", "Interrupting Rating"},
            {"Asset Details", "Ampere Rating"},
            {"Asset Details", "Mains Type"},
            {"Asset Details", "Voltage"},
            {"Asset Details", "Percentage Completion"},
            {"Source/Target Connections", "Asset"},
            {"Source/Target Connections", "Asset Class"},
            {"Source/Target Connections", "Needs Source"},
            {"Source/Target Connections", "Direct Source"},
            {"Source/Target Connections", "Indirect Source"},
            {"Source/Target Connections", "Status"},
            {"Source/Target Connections", "Source Info"},
            {"Connection Details", "Type"},
            {"Connection Details", "Source"},
            {"Connection Details", "Target"},
            {"Connection Details", "Conductor Material"},
            {"Connection Details", "Length (ft)"},
            {"Connection Details", "Neutral Wire Size"},
            {"Connection Details", "Amperage of Busway"},
            {"Connection Details", "Phase A Wire Size"},
            {"Connection Details", "Phase B Wire Size"},
            {"Connection Details", "Phase C Wire Size"},
        };
    }

    private void openTab(String tab) {
        if (!tab.equals(arcFlashPage.getActiveTab())) {
            arcFlashPage.clickTab(tab);
            if ("Connection Details".equals(tab)) arcFlashPage.waitForBuswayReadiness();
            arcFlashPage.waitForTableRows();
        }
    }

    @Test(priority = 1, dataProvider = "columns",
          description = "AFG_COL: the named column is present in its DataGrid (TC-AF-017/022/026)")
    public void testColumnPresent(String tab, String column) {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFG_COL_" + slug(tab) + "_" + slug(column));
        openTab(tab);
        Assert.assertTrue(arcFlashPage.hasColumn(column),
                "[" + tab + "] grid should expose a '" + column + "' column. Headers: " + arcFlashPage.getColumnHeaders());
        ExtentReportManager.logPass("[" + tab + "] column '" + column + "' present.");
    }

    @Test(priority = 2, dataProvider = "columns",
          description = "AFG_SORT: the named column sorts when its header is clicked (TC-AF-025)")
    public void testColumnSortable(String tab, String column) {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFG_SORT_" + slug(tab) + "_" + slug(column));
        openTab(tab);
        Assert.assertTrue(arcFlashPage.hasColumn(column), "Precondition: '" + column + "' column present on " + tab + ".");
        String state = arcFlashPage.sortByColumn(column);
        Assert.assertTrue(state.equals("ascending") || state.equals("descending"),
                "Clicking the '" + column + "' header should sort it (aria-sort ascending/descending). Got: '" + state + "'");
        ExtentReportManager.logPass("[" + tab + "] column '" + column + "' sortable — aria-sort='" + state + "'.");
    }

    private static String slug(String s) {
        return s.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+$", "");
    }

    /** Select a facility/site by exact name using REAL keystrokes (the ~130-site virtualised picker). */
    private boolean ensureSite(String name) {
        By facility = By.xpath("//input[@placeholder='Select facility']");
        try {
            WebElement inp = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(facility));
            if (name.equalsIgnoreCase(String.valueOf(inp.getAttribute("value")))) return true;
            inp.click(); pause(300);
            Keys mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
            try { inp.sendKeys(Keys.chord(mod, "a"), Keys.DELETE); } catch (Exception ignored) {}
            inp.sendKeys(name); pause(900);
            String lower = name.toLowerCase();
            By exact = By.xpath("//li[@role='option'][translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='" + lower + "']");
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(exact)).click();
            pause(900);
            return name.equalsIgnoreCase(String.valueOf(driver.findElement(facility).getAttribute("value")));
        } catch (Exception e) { logStep("ensureSite('" + name + "') failed: " + e.getMessage()); return false; }
    }
}
