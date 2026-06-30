package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.ArcFlashPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arc Flash Readiness — data-driven <b>per-asset-class matrix</b>: for each standard electrical asset
 * class, (1) the Asset Details <b>Asset Class filter</b> applies, and (2) the Overview <b>per-class
 * breakdown card</b> is internally consistent. 15 classes × 2 = 30 parameterised tests.
 *
 * <p><b>Why the curated list + skip:</b> the QA tenant's asset data is shared and mutable (classes appear
 * and disappear as other suites create/delete assets). We parameterise over the 15 <i>standard electrical
 * asset types</i> (ATS, Circuit Breaker, Transformer, …) that the platform models, and a test
 * {@link SkipException skips} (does not fail) if its class happens to have no data on the site this run —
 * so the matrix stays green across data drift instead of going brittle on a hardcoded snapshot.</p>
 *
 * <p><b>Breakdown oracle:</b> the card's percentage is a <i>field-completion</i> metric while "X/Y complete"
 * counts <i>fully-complete assets</i> — different numbers (e.g. "Disconnect Switch 0%, 0/4"). The sound
 * invariant is {@code 0<=X<=Y}, {@code 0<=pct<=100}, and {@code pct==100 <=> X==Y}.</p>
 */
public class ArcFlashAssetClassMatrixTestNG extends BaseTest {

    private static final String MODULE = "Arc Flash Readiness";
    private static final String FEATURE = "Asset-class matrix";
    private static final String SITE = "Android Qa Site1";

    private ArcFlashPage arcFlashPage;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Arc Flash Readiness — per-asset-class matrix (filter + breakdown)");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        arcFlashPage = new ArcFlashPage(driver);
        ensureSite(SITE);
        try {
            arcFlashPage.navigateToArcFlash();
            arcFlashPage.setEngineeringMode(false);
            arcFlashPage.clickTab("Overview");
        } catch (Exception ignored) {}
    }

    /** Standard electrical asset classes the platform models (stable set; drift handled via SkipException). */
    @DataProvider(name = "assetClasses")
    public Object[][] assetClasses() {
        return new Object[][]{
            {"ATS"}, {"Battery"}, {"Busway"}, {"Circuit Breaker"}, {"Disconnect Switch"},
            {"Fuse"}, {"Generator"}, {"Motor"}, {"Motor Starter"}, {"Panelboard"},
            {"Relay"}, {"Switchboard"}, {"Transformer"}, {"Utility"}, {"Other"},
        };
    }

    @Test(priority = 1, dataProvider = "assetClasses",
          description = "AFAC_FILTER: the Asset Class filter applies for the class (TC-AF-016)")
    public void testAssetClassFilterApplies(String assetClass) {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFAC_FILTER_" + slug(assetClass));
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Asset Details");
        Assert.assertTrue(arcFlashPage.waitForAssetClassFilter(), "Asset Class filter should render.");
        List<String> options = arcFlashPage.getAssetClassOptions();
        if (!options.contains(assetClass)) {
            throw new SkipException("Asset class '" + assetClass + "' has no data on this site right now — options: " + options);
        }
        arcFlashPage.selectAssetClass(assetClass);
        Assert.assertEquals(arcFlashPage.getAssetClassValue(), assetClass,
                "Asset Class filter should now read '" + assetClass + "'.");
        Assert.assertTrue(arcFlashPage.isLoaded(), "The Asset Details table should remain after filtering by " + assetClass + ".");
        ExtentReportManager.logPass("Asset Class filter applied for '" + assetClass + "' (footer: " + arcFlashPage.getShowingFooter() + ").");
    }

    @Test(priority = 2, dataProvider = "assetClasses",
          description = "AFAC_BREAKDOWN: the Overview per-class card is internally consistent (TC-AF-010)")
    public void testClassBreakdownConsistent(String assetClass) {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFAC_BREAKDOWN_" + slug(assetClass));
        arcFlashPage.navigateToArcFlash();
        arcFlashPage.clickTab("Overview");
        List<String> cards = arcFlashPage.getClassBreakdown();
        String card = null;
        for (String c : cards) if (c.startsWith(assetClass + "|")) { card = c; break; }
        if (card == null) {
            throw new SkipException("No '" + assetClass + "' breakdown card on this site right now — cards: " + cards);
        }
        // "Class|NN%|X/Y complete"
        String[] parts = card.split("\\|");
        int pct = Integer.parseInt(parts[1].replace("%", "").trim());
        Matcher m = Pattern.compile("(\\d+)/(\\d+)").matcher(parts[2]);
        Assert.assertTrue(m.find(), "Card should carry an X/Y count: " + card);
        int done = Integer.parseInt(m.group(1)), total = Integer.parseInt(m.group(2));

        Assert.assertTrue(pct >= 0 && pct <= 100, "Completion % must be 0..100: " + card);
        Assert.assertTrue(done >= 0 && done <= total, "0 <= complete(" + done + ") <= total(" + total + "): " + card);
        Assert.assertEquals(pct == 100, done == total,
                "Invariant pct==100 <=> all assets complete must hold for: " + card);
        ExtentReportManager.logPass("'" + assetClass + "' breakdown consistent: " + pct + "%, " + done + "/" + total + " complete.");
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
