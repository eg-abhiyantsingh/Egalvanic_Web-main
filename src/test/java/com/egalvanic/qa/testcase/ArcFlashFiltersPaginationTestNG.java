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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arc Flash Readiness — data-driven <b>pagination + Connection-Type filter matrix</b>.
 * Pagination per data tab (rows-per-page present + "1–N of M" consistency) = 6, plus the Connection-Type
 * filter per type (Busway / Cable / DC Cable) = 3. 9 parameterised tests (TC-AF-020 / TC-AF-027).
 */
public class ArcFlashFiltersPaginationTestNG extends BaseTest {

    private static final String MODULE = "Arc Flash Readiness";
    private static final String FEATURE = "Pagination + Connection-Type matrix";
    private static final String SITE = "Android Qa Site1";

    private ArcFlashPage arcFlashPage;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Arc Flash Readiness — pagination + Connection-Type matrix");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        arcFlashPage = new ArcFlashPage(driver);
        ensureSite(SITE);
        try {
            arcFlashPage.navigateToArcFlash();
            arcFlashPage.setEngineeringMode(true);
            arcFlashPage.clickTab("Asset Details"); arcFlashPage.waitForAssetClassFilter(); arcFlashPage.waitForTableRows();
            arcFlashPage.clickTab("Source/Target Connections"); arcFlashPage.waitForTableRows();
            arcFlashPage.clickTab("Connection Details"); arcFlashPage.waitForBuswayReadiness(); arcFlashPage.waitForTableRows();
        } catch (Exception ignored) {}
    }

    @DataProvider(name = "tabs")
    public Object[][] tabs() {
        return new Object[][]{
            {"Asset Details"}, {"Source/Target Connections"}, {"Connection Details"},
        };
    }

    @DataProvider(name = "connectionTypes")
    public Object[][] connectionTypes() {
        return new Object[][]{ {"Busway"}, {"Cable"}, {"DC Cable"} };
    }

    private void openTab(String tab) {
        if (!tab.equals(arcFlashPage.getActiveTab())) {
            arcFlashPage.clickTab(tab);
            if ("Connection Details".equals(tab)) arcFlashPage.waitForBuswayReadiness();
            arcFlashPage.waitForTableRows();
        }
    }

    @Test(priority = 1, dataProvider = "tabs",
          description = "AFP_ROWS: the tab's grid exposes a Rows-per-page control (TC-AF-020)")
    public void testRowsPerPagePresent(String tab) {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFPG_ROWS_" + slug(tab));
        openTab(tab);
        Assert.assertTrue(arcFlashPage.hasRowsPerPage(), "[" + tab + "] should expose a 'Rows per page' control.");
        ExtentReportManager.logPass("[" + tab + "] rows-per-page present (value=" + arcFlashPage.getRowsPerPageValue() + ").");
    }

    @Test(priority = 2, dataProvider = "tabs",
          description = "AFP_DISP: the tab's '1–N of M' displayed-rows count is consistent (TC-AF-020)")
    public void testDisplayedRowsConsistent(String tab) {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFPG_DISP_" + slug(tab));
        openTab(tab);
        String page = arcFlashPage.getPaginationText();
        Matcher m = Pattern.compile("(\\d+)[\\s –—\\-]+(\\d+) of (\\d+)").matcher(page);
        Assert.assertTrue(m.find(), "[" + tab + "] pagination should read '1–N of M'. Got: '" + page + "'");
        int from = Integer.parseInt(m.group(1)), to = Integer.parseInt(m.group(2)), total = Integer.parseInt(m.group(3));
        Assert.assertTrue(from >= 1 && from <= to && to <= total,
                "[" + tab + "] pagination range invalid: " + from + "-" + to + " of " + total);
        ExtentReportManager.logPass("[" + tab + "] displayed-rows consistent: " + page);
    }

    @Test(priority = 3, dataProvider = "connectionTypes",
          description = "AFP_CTYPE: the Connection-Type filter applies for the type (TC-AF-027)")
    public void testConnectionTypeApplies(String type) {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFPG_CTYPE_" + slug(type));
        openTab("Connection Details");
        Assert.assertTrue(arcFlashPage.hasConnectionTypeFilter(), "Connection Details should have a Connection-Type filter.");
        java.util.List<String> options = arcFlashPage.getConnectionTypeOptions();
        Assert.assertTrue(options.contains(type), "Connection-Type filter should offer '" + type + "'. Options: " + options);
        arcFlashPage.selectConnectionType(type);
        Assert.assertEquals(arcFlashPage.getConnectionTypeValue(), type,
                "Connection-Type filter should now read '" + type + "'.");
        Assert.assertTrue(arcFlashPage.isLoaded(), "The page should remain loaded after filtering to '" + type + "'.");
        ExtentReportManager.logPass("Connection-Type filter applied for '" + type + "'.");
    }

    private static String slug(String s) { return s.replaceAll("[^A-Za-z0-9]+", "_"); }

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
