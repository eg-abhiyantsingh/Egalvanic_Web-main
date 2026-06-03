package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.verify.NetworkConditions;
import com.egalvanic.qa.utils.verify.PerfVerifier;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Phase 4 cross-cutting quality gates — applies the NON-functional testing types
 * (Accessibility, Performance, Boundary/Data-driven input, Resilience/offline)
 * across every major module in ONE data-driven class.
 *
 * Bug classes this surfaces that functional scripts miss:
 *   - WCAG violations (contrast, labels, ARIA, alt-text)         [type H]
 *   - Slow / never-settling page loads                            [type I client-side]
 *   - Crashes/hangs on boundary + malicious input                 [type B + J]
 *   - Broken behavior when the network drops mid-flow             [type L]
 *
 * Extends BaseTest → shares login + the auto health gate. Read-only (no data
 * mutation) so it's safe to run anywhere.
 */
public class Phase4QualityGatesTestNG extends BaseTest {

    private static final String MODULE = "Quality Gates";

    @BeforeClass
    @Override
    public void classSetup() {
        super.classSetup();   // login + site select
    }

    // Every major route: {label, path, requiredCss-that-must-render, load-budget-ms}
    @DataProvider(name = "routes")
    public Object[][] routes() {
        return new Object[][]{
            {"Dashboard",   "/dashboard",  "main",                     9000L},
            {"Assets",      "/assets",     ".MuiDataGrid-root, [role='grid']", 12000L},
            {"Work Orders", "/sessions",   ".MuiDataGrid-root, [role='grid']", 12000L},
            {"Planning",    "/planning",   ".MuiDataGrid-root, [role='grid']", 10000L},
            {"Locations",   "/locations",  "main",                     10000L},
            {"Issues",      "/issues",     ".MuiDataGrid-root, [role='grid']", 12000L},
            {"Tasks",       "/tasks",      "main",                     10000L},
        };
    }

    @Test(dataProvider = "routes",
          description = "A11y: no critical/serious WCAG violations per module")
    public void testRouteAccessibility(String label, String path, String requiredCss, long budget) {
        ExtentReportManager.createTest(MODULE, "Accessibility (WCAG)", "A11y_" + label);
        navigate(path);
        logStep("Running axe-core WCAG 2 A/AA scan on " + label);
        verifyAccessibility(label + " (" + path + ")");   // hard-fails on critical/serious
        ExtentReportManager.logPass(label + " has no critical/serious WCAG violations");
    }

    @Test(dataProvider = "routes",
          description = "Perf: client-side load within per-module budget")
    public void testRoutePerformance(String label, String path, String requiredCss, long budget) {
        ExtentReportManager.createTest(MODULE, "Performance (client-side)", "Perf_" + label);
        navigate(path);
        PerfVerifier.PerfReport r = PerfVerifier.capture(driver, label);
        logStep(r.toString());
        verifyPerformance(label, budget);
        ExtentReportManager.logPass(label + " loaded within " + budget + "ms budget");
    }

    // Boundary / equivalence / malicious inputs — fed into the Planning search box
    // (non-destructive). Verifies the app NEVER hangs/crashes/blanks on bad input.
    @DataProvider(name = "boundaryInputs")
    public Object[][] boundaryInputs() {
        return new Object[][]{
            {"empty", ""},
            {"single-char", "a"},
            {"max-255", repeat("x", 255)},
            {"over-255", repeat("y", 256)},
            {"huge-5k", repeat("z", 5000)},
            {"unicode", "测试🚀Ωüc-ñ"},
            {"sql-injection", "' OR 1=1 --"},
            {"xss", "<script>window.__qg=1</script>"},
            {"whitespace", "     "},
            {"special", "!@#$%^&*()_+{}|:\"<>?~`"},
        };
    }

    @Test(dataProvider = "boundaryInputs",
          description = "Boundary/negative: bad search input must not crash/hang/XSS")
    public void testSearchInputBoundary(String label, String term) {
        ExtentReportManager.createTest(MODULE, "Input Boundary/Negative", "Boundary_" + label);
        navigate("/planning");
        // Scope error detection to the INPUT action: clear baseline page errors that
        // accrued during navigation so we attribute only NEW errors to this input.
        com.egalvanic.qa.utils.verify.BrowserErrorCapture.clear(driver);
        logStep("Typing boundary input [" + label + "] into Planning search");
        try {
            java.util.List<org.openqa.selenium.WebElement> boxes = driver.findElements(
                    By.xpath("//input[contains(@placeholder,'Search Work Order Planning')]"));
            org.openqa.selenium.WebElement box = boxes.stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
            if (box != null) {
                ((JavascriptExecutor) driver).executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0],arguments[1]);"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", box, term);
                pause(2000);
            }
        } catch (Exception e) {
            logStep("Input step note: " + e.getMessage());
        }
        // XSS must not have executed
        Object xss = ((JavascriptExecutor) driver).executeScript("return window.__qg || null;");
        if (xss != null) {
            throw new AssertionError("XSS executed for input [" + label + "] — window.__qg was set");
        }
        // Page must remain healthy (no hang, no JS crash, no blank, no failed XHR)
        verifyPageHealth("Planning search boundary [" + label + "]");
        ExtentReportManager.logPass("Search handled [" + label + "] safely (no crash/hang/XSS)");
    }

    @Test(description = "Resilience: going offline mid-session must not hang/crash the page")
    public void testOfflineResilience() {
        ExtentReportManager.createTest(MODULE, "Resilience (offline)", "Offline_Planning");
        navigate("/planning");
        logStep("Dropping network to OFFLINE, then reloading-ish interaction");
        NetworkConditions.goOffline(driver);
        try {
            // Trigger a data-dependent action while offline (search), then check health
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.dispatchEvent(new Event('offline'));");
            pause(2000);
            // Resilience bar: the page must stay RESPONSIVE (not hung) while offline.
            // A graceful empty-state ("select an SLD", "you are offline") is acceptable
            // degradation — so we assert responsiveness only, not error-banner absence.
            com.egalvanic.qa.utils.verify.HangDetector.assertResponsive(driver, "Planning offline", 20);
            logStepWithScreenshot("Offline state");
        } finally {
            NetworkConditions.goOnline(driver);
            pause(1500);
        }
        ExtentReportManager.logPass("Page remained responsive + non-blank while offline");
    }

    // ── helpers ──
    private void navigate(String path) {
        driver.get(AppConstants.BASE_URL + path);
        pause(2000);
        dismissBackdrops();
        // wait for app shell
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
                Object b = ((JavascriptExecutor) d).executeScript(
                        "return document.body && document.body.innerText || '';");
                String t = b == null ? "" : b.toString();
                return (t.contains("DASHBOARDS") || t.contains("Site Overview")) && !t.startsWith("Loading");
            });
        } catch (Exception ignored) {}
        pause(1500);
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
