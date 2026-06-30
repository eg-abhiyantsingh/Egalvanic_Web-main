package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.ArcFlashPage;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
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
 * Arc Flash Readiness — platform / cross-cutting behaviours: the <b>Role</b> view selector, the
 * <b>SLD viewer</b> presence, <b>responsive layout</b>, and the <b>Generate Report</b> reality check.
 *
 * <p>Isolated in its own class (own browser session) so the (reversible) role switch in AFP_05 can never
 * bleed into the analytics tests in {@code ArcFlashTestNG} / {@code ArcFlashConnectionsTestNG}.</p>
 *
 * <p><b>Verified live 2026-06-30 (admin role, site "Android Qa Site1"):</b> /arc-flash exposes a "Role:"
 * view selector (placeholder "Select role", 4 roles: Admin / Project Manager / Account Manager /
 * Electrical Engineer), an embedded <b>SLD diagram canvas below the readiness analytics</b>, and a
 * <b>Generate Report</b> control. Switching the Role view recomputes the readiness (verified
 * Admin→Project Manager, then restored). The SLD canvas + Generate Report render below the fold / after the
 * analytics load, so the probes scroll and retry before asserting presence.</p>
 */
public class ArcFlashPlatformTestNG extends BaseTest {

    private static final String MODULE = "Arc Flash Readiness";
    private static final String FEATURE = "Platform / Role / Responsive";
    private static final String SITE = "Android Qa Site1";

    private ArcFlashPage arcFlashPage;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Arc Flash Readiness — Platform (Role / SLD / Responsive / Report)");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
        arcFlashPage = new ArcFlashPage(driver);
        boolean ok = ensureSite(SITE);
        System.out.println("[ArcFlashPlatform] site '" + SITE + "' selected=" + ok);
    }

    @Test(priority = 1, description = "AFP_01: A Role view selector is present and lists roles (TC-AF-006)")
    public void testAFP_01_RoleSelectorPresent() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFP_01_RoleSelectorPresent");
        arcFlashPage.navigateToArcFlash();
        Assert.assertTrue(arcFlashPage.hasRoleSelector(), "Arc Flash header should have a 'Select role' view selector.");
        List<String> roles = arcFlashPage.getRoleOptions();
        logStep("Role value='" + arcFlashPage.getRoleValue() + "', options=" + roles);
        Assert.assertFalse(roles.isEmpty(), "The Role selector should list at least one role option.");
        logStepWithScreenshot("Role selector + options");
        ExtentReportManager.logPass("Role view selector present with " + roles.size() + " options: " + roles);
    }

    @Test(priority = 2, description = "AFP_02: An SLD diagram canvas renders on /arc-flash (TC-AF-004)")
    public void testAFP_02_SldViewerPresence() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFP_02_SldViewerPresence");
        arcFlashPage.navigateToArcFlash();
        // The SLD canvas sits below the readiness analytics and loads async — scroll + retry before asserting.
        boolean sld = false;
        for (int i = 0; i < 6 && !sld; i++) { sld = arcFlashPage.hasSldViewer(); if (!sld) pause(1500); }
        Assert.assertTrue(arcFlashPage.isLoaded(), "The Arc Flash page should be loaded for the SLD probe.");
        logStepWithScreenshot("SLD viewer (present=" + sld + ")");
        Assert.assertTrue(sld, "An embedded SLD/diagram canvas should render on the Arc Flash Readiness page (TC-AF-004).");
        ExtentReportManager.logPass("An embedded SLD/diagram canvas renders on the Arc Flash Readiness page.");
    }

    @Test(priority = 3, description = "AFP_03: Layout stays usable at a narrow (mobile) viewport (TC-AF-033)")
    public void testAFP_03_ResponsiveLayout() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFP_03_ResponsiveLayout");
        arcFlashPage.navigateToArcFlash();
        Dimension original = driver.manage().window().getSize();
        try {
            driver.manage().window().setSize(new Dimension(480, 900)); // narrow / phone width
            pause(1500);
            // The tab strip must still be reachable (tabs present in the DOM, even if scrolled/stacked).
            boolean tabsPresent = !driver.findElements(By.xpath("//*[@role='tab'][normalize-space()='Overview']")).isEmpty();
            boolean drawerToggle = !driver.findElements(By.xpath(
                    "//button[@aria-label='open drawer' or contains(@aria-label,'menu') or contains(@aria-label,'Open')]")).isEmpty();
            logStep("Narrow viewport: tabsPresent=" + tabsPresent + ", drawer/menu toggle=" + drawerToggle);
            Assert.assertTrue(tabsPresent || drawerToggle,
                    "At a narrow width the tabs should remain present or be reachable via a drawer/menu toggle.");
            logStepWithScreenshot("Narrow (480px) layout");
        } finally {
            driver.manage().window().setSize(new Dimension(Math.max(1280, original.getWidth()), Math.max(800, original.getHeight())));
            pause(1200);
        }
        Assert.assertTrue(arcFlashPage.isLoaded(), "After restoring width the tab strip should be present again.");
        ExtentReportManager.logPass("Arc Flash layout remained usable at 480px wide and restored cleanly.");
    }

    @Test(priority = 4, description = "AFP_04: A Generate Report control is present on /arc-flash (TC-AF-029)")
    public void testAFP_04_GenerateReportPresent() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFP_04_GenerateReportPresent");
        arcFlashPage.navigateToArcFlash();
        boolean has = false;
        for (int i = 0; i < 6 && !has; i++) { has = arcFlashPage.hasGenerateReport(); if (!has) pause(1500); }
        Assert.assertTrue(arcFlashPage.isLoaded(), "The Arc Flash page should be loaded for the report check.");
        logStep("'Generate Report' control present on /arc-flash = " + has);
        Assert.assertTrue(has, "A 'Generate Report' control should be present on the Arc Flash Readiness page (TC-AF-029).");
        ExtentReportManager.logPass("A 'Generate Report' control is present on the Arc Flash Readiness page.");
        logStepWithScreenshot("Generate Report control present");
    }

    @Test(priority = 5, description = "AFP_05: Switching the Role view recomputes the page and stays healthy (TC-AF-006)")
    public void testAFP_05_SwitchRoleView() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AFP_05_SwitchRoleView");
        arcFlashPage.navigateToArcFlash();
        String original = arcFlashPage.getRoleValue();
        List<String> roles = arcFlashPage.getRoleOptions();
        logStep("Original role='" + original + "', options=" + roles);
        if (roles.size() < 2) {
            logStep("Only " + roles.size() + " role option available — switch not applicable; presence already covered by AFP_01.");
            ExtentReportManager.logPass("Single role available; role-switch not applicable on this account/site.");
            return;
        }
        String target = null;
        for (String r : roles) if (!r.equalsIgnoreCase(original)) { target = r; break; }
        Assert.assertNotNull(target, "Should find a role different from the current one to switch to.");
        try {
            boolean switched = arcFlashPage.selectRole(target);
            Assert.assertTrue(switched, "Selecting role '" + target + "' should update the Role selector.");
            Assert.assertTrue(arcFlashPage.isLoaded(), "The page should still be loaded after the role switch.");
            // The readiness view recomputes asynchronously under the new role lens — poll until it re-renders.
            arcFlashPage.clickTab("Overview");
            List<String> g = arcFlashPage.waitForOverviewGauges();
            Assert.assertEquals(g.size(), 3,
                    "The three readiness gauges should re-render under the '" + target + "' role view. Got: " + g);
            logStepWithScreenshot("Role switched to " + target);
            ExtentReportManager.logPass("Switched Role view '" + original + "' → '" + target + "'; page recomputed and stayed healthy.");
        } finally {
            // Restore the original role view (reversible — this is a client-side view lens).
            if (original != null && !original.isEmpty()) {
                try { arcFlashPage.selectRole(original); } catch (Exception ignored) {}
            } else {
                try { driver.navigate().refresh(); pause(2000); } catch (Exception ignored) {}
            }
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Select a facility/site by exact name using REAL keystrokes (the ~130-site virtualised picker
     *  ignores JS value-sets). Mirrors the other Arc Flash tests. */
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
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(exact)).click();
            pause(900);
            return name.equalsIgnoreCase(String.valueOf(driver.findElement(facility).getAttribute("value")));
        } catch (Exception e) {
            logStep("ensureSite('" + name + "') failed: " + e.getMessage());
            return false;
        }
    }
}
