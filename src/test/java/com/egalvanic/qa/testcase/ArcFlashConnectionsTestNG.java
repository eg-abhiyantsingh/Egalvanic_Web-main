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
