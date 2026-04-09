package com.egalvanic.qa.testcase;

import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.AIPageAnalyzer;
import com.egalvanic.qa.utils.ai.AIPageAnalyzer.PageAnalysis;
import com.egalvanic.qa.utils.ai.AIPageAnalyzer.TestScenario;
import com.egalvanic.qa.utils.ai.ClaudeClient;
import com.egalvanic.qa.utils.ai.FlakinessPrevention;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Page Analyzer Test Suite
 *
 * Analyzes every major page in the application, discovers interactive elements,
 * classifies page types, and suggests test scenarios. This is the AI equivalent
 * of a manual tester's first walkthrough of a new application.
 *
 * Output:
 *   - test-output/page-analysis-report.json (full analysis)
 *   - test-output/generated-tests/ (auto-generated test stubs)
 *
 * Extends BaseTest for standard login/site-selection/browser lifecycle.
 */
public class AIPageAnalyzerTestNG extends BaseTest {

    private static final String MODULE = "AI Page Analysis";
    private static final String FEATURE = "Page Discovery";

    private static final String[] PAGES = {
            "Dashboard", "Assets", "Locations", "Connections",
            "Issues", "Jobs", "Tasks"
    };

    private final Map<String, PageAnalysis> allAnalyses = new LinkedHashMap<>();

    // ================================================================
    // TEST METHODS
    // ================================================================

    @Test(priority = 1, description = "Analyze all major pages and discover testable elements")
    public void testAnalyzeAllPages() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Analyze All Pages");

        int totalElements = 0;
        int totalScenarios = 0;

        for (String page : PAGES) {
            try {
                navigateViaSidebar(page);
                pause(2000);
                FlakinessPrevention.waitForPageReady(driver);
                dismissBackdrops();

                PageAnalysis analysis = AIPageAnalyzer.analyzePage(driver);
                allAnalyses.put(page, analysis);

                List<TestScenario> scenarios = AIPageAnalyzer.suggestTestScenarios(analysis);
                totalElements += analysis.elements.size();
                totalScenarios += scenarios.size();

                logStep(String.format("  %s: type=%s, %d elements, %d inputs, %d buttons, %d scenarios",
                        page, analysis.pageType, analysis.elements.size(),
                        analysis.inputCount, analysis.buttonCount, scenarios.size()));

            } catch (Exception e) {
                logStep("  " + page + ": FAILED — " + e.getMessage());
            }
        }

        logStep(String.format("Analysis complete: %d pages, %d elements, %d scenarios suggested",
                allAnalyses.size(), totalElements, totalScenarios));

        Assert.assertTrue(allAnalyses.size() >= 3,
                "Should analyze at least 3 pages (got " + allAnalyses.size() + ")");

        // Write report
        AIPageAnalyzer.writeAnalysisReport(allAnalyses);
        ExtentReportManager.logPass("Analyzed " + allAnalyses.size() + " pages, " +
                totalElements + " elements, " + totalScenarios + " scenarios");
    }

    @Test(priority = 2, dependsOnMethods = "testAnalyzeAllPages",
          description = "Generate test stubs from page analysis")
    public void testGenerateTestStubs() {
        ExtentReportManager.createTest(MODULE, FEATURE, "Generate Test Stubs");

        int stubsGenerated = 0;
        String outputDir = "test-output/generated-tests";

        for (Map.Entry<String, PageAnalysis> entry : allAnalyses.entrySet()) {
            String pageName = entry.getKey();
            PageAnalysis analysis = entry.getValue();

            String className = pageName.replaceAll("[^a-zA-Z0-9]", "") + "GeneratedTestNG";
            AIPageAnalyzer.writeTestStubs(analysis, className, outputDir);
            stubsGenerated++;

            logStep("  Generated: " + outputDir + "/" + className + ".java");
        }

        logStep("Test stub generation complete: " + stubsGenerated + " files");
        Assert.assertTrue(stubsGenerated > 0, "Should generate at least 1 test stub file");
        ExtentReportManager.logPass("Generated " + stubsGenerated + " test stub files");
    }

    @Test(priority = 3, description = "AI-enhanced analysis (requires CLAUDE_API_KEY)")
    public void testAnalyzeWithAI() {
        ExtentReportManager.createTest(MODULE, FEATURE, "AI-Enhanced Analysis");

        if (!ClaudeClient.isConfigured()) {
            logStep("CLAUDE_API_KEY not configured — skipping AI-enhanced analysis");
            ExtentReportManager.logInfo("AI analysis skipped (no API key)");
            return;
        }

        // Analyze the Dashboard page with AI
        try {
            navigateViaSidebar("Dashboard");
            pause(2000);
            FlakinessPrevention.waitForPageReady(driver);
            dismissBackdrops();

            PageAnalysis analysis = AIPageAnalyzer.analyzePageWithAI(driver);

            if (analysis.aiSuggestions != null && !analysis.aiSuggestions.isEmpty()) {
                logStep("AI suggestions for Dashboard:");
                for (String suggestion : analysis.aiSuggestions) {
                    logStep("  → " + suggestion);
                }
                ExtentReportManager.logPass("AI analysis: " + analysis.aiSuggestions.size() + " additional suggestions");
            } else {
                logStep("AI returned no additional suggestions");
                ExtentReportManager.logPass("AI analysis completed (no extra suggestions)");
            }
        } catch (Exception e) {
            logStep("AI analysis error: " + e.getMessage());
            ExtentReportManager.logInfo("AI analysis failed: " + e.getMessage());
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private void navigateViaSidebar(String linkText) {
        try {
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            js.executeScript(
                "var items = document.querySelectorAll('span, a');" +
                "for (var i = 0; i < items.length; i++) {" +
                "  if (items[i].textContent.trim() === '" + linkText + "' && items[i].offsetWidth > 0) {" +
                "    items[i].click(); return;" +
                "  }" +
                "}");
        } catch (Exception e) {
            driver.get(com.egalvanic.qa.constants.AppConstants.BASE_URL + "/" + linkText.toLowerCase());
        }
    }
}
