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
    // HELPERS
    // ================================================================

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
