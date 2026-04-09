package com.egalvanic.qa.utils.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * AI Page Analyzer — Intelligent DOM Analysis & Test Scenario Suggestion
 *
 * Manual testers look at a page and immediately identify what to test.
 * This utility does the same: scans a page's DOM, discovers all interactive
 * elements, classifies the page type, and suggests test scenarios.
 *
 * Features:
 *   - Discovers all interactive elements (inputs, buttons, links, grids, modals)
 *   - Classifies page type: LIST_PAGE, FORM_PAGE, DETAIL_PAGE, DASHBOARD
 *   - Generates rule-based test scenarios per page type
 *   - Optional Claude AI enhancement for smarter suggestions
 *   - Test stub generation (compilable Java code)
 *   - JSON analysis report output
 *
 * Usage:
 *   PageAnalysis analysis = AIPageAnalyzer.analyzePage(driver);
 *   List<TestScenario> scenarios = AIPageAnalyzer.suggestTestScenarios(analysis);
 */
public class AIPageAnalyzer {

    private AIPageAnalyzer() {}

    // ================================================================
    // CORE ANALYSIS
    // ================================================================

    /**
     * Analyze the current page and discover all interactive elements.
     */
    public static PageAnalysis analyzePage(WebDriver driver) {
        PageAnalysis analysis = new PageAnalysis();
        analysis.url = driver.getCurrentUrl();
        analysis.title = driver.getTitle();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Discover interactive elements via JavaScript
        analysis.elements = discoverInteractiveElements(js);

        // Count element types
        for (DiscoveredElement el : analysis.elements) {
            switch (el.tagName.toLowerCase()) {
                case "input": case "textarea": case "select": analysis.inputCount++; break;
                case "button": analysis.buttonCount++; break;
                case "a": analysis.linkCount++; break;
            }
        }

        // Detect special MUI components
        analysis.hasDataGrid = (Boolean) js.executeScript(
                "return document.querySelector('[class*=\"MuiDataGrid\"]') !== null;");
        analysis.hasDrawer = (Boolean) js.executeScript(
                "return document.querySelector('[class*=\"MuiDrawer\"]') !== null;");
        analysis.hasDialog = (Boolean) js.executeScript(
                "return document.querySelector('[role=\"dialog\"]') !== null;");

        // Get headings
        analysis.headings = getHeadings(js);

        // Classify page type
        analysis.pageType = classifyPageType(analysis);

        return analysis;
    }

    /**
     * Analyze page with Claude AI enhancement for deeper insights.
     */
    public static PageAnalysis analyzePageWithAI(WebDriver driver) {
        PageAnalysis analysis = analyzePage(driver);

        if (!ClaudeClient.isConfigured()) {
            System.out.println("[AIPageAnalyzer] Claude API not configured — using rule-based analysis only");
            return analysis;
        }

        try {
            // Build a summary of what we found
            StringBuilder summary = new StringBuilder();
            summary.append("Page URL: ").append(analysis.url).append("\n");
            summary.append("Page Type: ").append(analysis.pageType).append("\n");
            summary.append("Headings: ").append(analysis.headings).append("\n");
            summary.append("Inputs: ").append(analysis.inputCount)
                   .append(", Buttons: ").append(analysis.buttonCount)
                   .append(", Links: ").append(analysis.linkCount).append("\n");
            summary.append("Has DataGrid: ").append(analysis.hasDataGrid)
                   .append(", Has Drawer: ").append(analysis.hasDrawer).append("\n");
            summary.append("\nInteractive elements:\n");

            for (DiscoveredElement el : analysis.elements) {
                summary.append("  - ").append(el.tagName);
                if (el.label != null && !el.label.isEmpty()) summary.append(" label='").append(el.label).append("'");
                if (el.type != null) summary.append(" type=").append(el.type);
                if (el.text != null && !el.text.isEmpty()) summary.append(" text='").append(el.text).append("'");
                if (el.isRequired) summary.append(" [REQUIRED]");
                summary.append("\n");
            }

            String aiResponse = ClaudeClient.ask(
                "You are a senior QA tester analyzing a web application page for testing.",
                "Analyze this page and suggest additional test scenarios that rule-based analysis might miss.\n" +
                "Focus on: business logic tests, workflow tests, error handling, edge cases, accessibility.\n" +
                "Respond with a JSON array of objects: [{\"name\": \"...\", \"description\": \"...\", \"priority\": \"HIGH|MEDIUM|LOW\"}]\n\n" +
                summary.toString()
            );

            if (aiResponse != null) {
                analysis.aiSuggestions = new ArrayList<>();
                try {
                    JSONArray arr = new JSONArray(aiResponse);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        analysis.aiSuggestions.add(obj.optString("name", "") +
                                " — " + obj.optString("description", ""));
                    }
                } catch (Exception e) {
                    // AI returned non-JSON — store as single suggestion
                    analysis.aiSuggestions.add(aiResponse);
                }
            }
        } catch (Exception e) {
            System.out.println("[AIPageAnalyzer] AI analysis failed: " + e.getMessage());
        }

        return analysis;
    }

    // ================================================================
    // ELEMENT DISCOVERY
    // ================================================================

    private static List<DiscoveredElement> discoverInteractiveElements(JavascriptExecutor js) {
        List<DiscoveredElement> elements = new ArrayList<>();

        String script =
            "var results = [];" +
            // Inputs and textareas
            "document.querySelectorAll('input, textarea, select').forEach(function(el) {" +
            "  if (el.offsetWidth === 0 && el.offsetHeight === 0) return;" +
            "  if (el.type === 'hidden') return;" +
            "  var label = '';" +
            "  if (el.id) { var lbl = document.querySelector('label[for=\"' + el.id + '\"]'); if (lbl) label = lbl.textContent.trim(); }" +
            "  if (!label && el.getAttribute('aria-label')) label = el.getAttribute('aria-label');" +
            "  if (!label && el.placeholder) label = el.placeholder;" +
            "  if (!label) {" +
            "    var p = el.closest('.MuiFormControl-root, .MuiTextField-root');" +
            "    if (p) { var l = p.querySelector('label, p'); if (l) label = l.textContent.trim(); }" +
            "  }" +
            "  results.push({" +
            "    tagName: el.tagName.toLowerCase()," +
            "    type: el.type || 'text'," +
            "    label: label," +
            "    placeholder: el.placeholder || ''," +
            "    text: ''," +
            "    role: el.getAttribute('role') || ''," +
            "    isRequired: el.required || el.getAttribute('aria-required') === 'true'," +
            "    isDisabled: el.disabled || el.getAttribute('aria-disabled') === 'true'," +
            "    section: ''" +
            "  });" +
            "});" +
            // Buttons
            "document.querySelectorAll('button, [role=\"button\"]').forEach(function(el) {" +
            "  if (el.offsetWidth === 0 && el.offsetHeight === 0) return;" +
            "  var text = el.textContent.trim();" +
            "  if (!text || text.length > 100) return;" + // skip icon-only or huge text
            "  results.push({" +
            "    tagName: 'button'," +
            "    type: el.type || 'button'," +
            "    label: ''," +
            "    placeholder: ''," +
            "    text: text.substring(0, 80)," +
            "    role: el.getAttribute('role') || 'button'," +
            "    isRequired: false," +
            "    isDisabled: el.disabled || el.getAttribute('aria-disabled') === 'true'," +
            "    section: ''" +
            "  });" +
            "});" +
            // Links
            "document.querySelectorAll('a[href]').forEach(function(el) {" +
            "  if (el.offsetWidth === 0 && el.offsetHeight === 0) return;" +
            "  var text = el.textContent.trim();" +
            "  if (!text || text.length > 100) return;" +
            "  results.push({" +
            "    tagName: 'a'," +
            "    type: ''," +
            "    label: ''," +
            "    placeholder: ''," +
            "    text: text.substring(0, 80)," +
            "    role: el.getAttribute('role') || ''," +
            "    isRequired: false," +
            "    isDisabled: false," +
            "    section: ''" +
            "  });" +
            "});" +
            "return JSON.stringify(results);";

        try {
            String json = (String) js.executeScript(script);
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                DiscoveredElement el = new DiscoveredElement();
                el.tagName = obj.optString("tagName", "");
                el.type = obj.optString("type", "");
                el.label = obj.optString("label", "");
                el.placeholder = obj.optString("placeholder", "");
                el.text = obj.optString("text", "");
                el.role = obj.optString("role", "");
                el.isRequired = obj.optBoolean("isRequired", false);
                el.isDisabled = obj.optBoolean("isDisabled", false);
                el.section = obj.optString("section", "");
                elements.add(el);
            }
        } catch (Exception e) {
            System.out.println("[AIPageAnalyzer] Element discovery failed: " + e.getMessage());
        }

        return elements;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getHeadings(JavascriptExecutor js) {
        try {
            String script =
                "var headings = [];" +
                "document.querySelectorAll('h1, h2, h3, h4').forEach(function(h) {" +
                "  if (h.offsetWidth > 0 && h.textContent.trim()) headings.push(h.textContent.trim().substring(0, 100));" +
                "});" +
                "return headings;";
            Object result = js.executeScript(script);
            if (result instanceof List) {
                return (List<String>) result;
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    // ================================================================
    // PAGE CLASSIFICATION
    // ================================================================

    private static String classifyPageType(PageAnalysis analysis) {
        // DataGrid + search → LIST_PAGE
        if (analysis.hasDataGrid && analysis.inputCount <= 3) {
            return "LIST_PAGE";
        }
        // Many inputs + save button → FORM_PAGE
        boolean hasSaveButton = analysis.elements.stream()
                .anyMatch(e -> e.text != null && (
                    e.text.toLowerCase().contains("save") ||
                    e.text.toLowerCase().contains("submit") ||
                    e.text.toLowerCase().contains("create")));
        if (analysis.inputCount >= 3 && hasSaveButton) {
            return "FORM_PAGE";
        }
        // Drawer open → FORM_PAGE (MUI drawer forms)
        if (analysis.hasDrawer && analysis.inputCount >= 2) {
            return "FORM_PAGE";
        }
        // Charts, badges, KPIs → DASHBOARD
        String url = analysis.url != null ? analysis.url.toLowerCase() : "";
        if (url.contains("dashboard") || url.contains("overview")) {
            return "DASHBOARD";
        }
        // Edit button + few inputs → DETAIL_PAGE
        boolean hasEditButton = analysis.elements.stream()
                .anyMatch(e -> e.text != null && e.text.toLowerCase().contains("edit"));
        if (hasEditButton && analysis.inputCount <= 2) {
            return "DETAIL_PAGE";
        }
        // URL-based fallbacks
        if (url.contains("settings") || url.contains("admin")) return "SETTINGS";

        return analysis.inputCount > 0 ? "FORM_PAGE" : "LIST_PAGE";
    }

    // ================================================================
    // TEST SCENARIO SUGGESTION
    // ================================================================

    /**
     * Generate test scenarios based on page analysis.
     */
    public static List<TestScenario> suggestTestScenarios(PageAnalysis analysis) {
        List<TestScenario> scenarios = new ArrayList<>();

        switch (analysis.pageType) {
            case "LIST_PAGE":
                scenarios.add(scenario("Verify data grid loads with rows", "Check that the grid renders and contains at least one row", "Validation", "HIGH"));
                scenarios.add(scenario("Search for existing item", "Enter a known value in search and verify results filter", "CRUD", "HIGH"));
                scenarios.add(scenario("Search for non-existent item", "Enter invalid search term and verify 0 results", "CRUD", "HIGH"));
                scenarios.add(scenario("Clear search restores all rows", "After searching, clear the input and verify all rows return", "CRUD", "MEDIUM"));
                if (analysis.elements.stream().anyMatch(e -> e.text != null && e.text.toLowerCase().contains("create"))) {
                    scenarios.add(scenario("Create new item button works", "Click Create/Add button and verify form opens", "CRUD", "HIGH"));
                }
                scenarios.add(scenario("Click row opens detail/edit", "Click the first row and verify navigation or drawer opens", "Navigation", "HIGH"));
                scenarios.add(scenario("Grid handles empty state", "Verify appropriate message when no data exists", "Edge Case", "MEDIUM"));
                break;

            case "FORM_PAGE":
                // One scenario per required field
                for (DiscoveredElement el : analysis.elements) {
                    if (el.isRequired && isInputElement(el)) {
                        String fieldName = el.label.isEmpty() ? el.placeholder : el.label;
                        if (fieldName.isEmpty()) continue;
                        scenarios.add(scenario("Submit without " + fieldName, "Leave " + fieldName + " empty and verify validation error", "Validation", "HIGH"));
                    }
                }
                scenarios.add(scenario("Submit with all required fields", "Fill all required fields with valid data and submit", "CRUD", "HIGH"));
                scenarios.add(scenario("Submit with special characters", "Fill text fields with special chars and verify handling", "Edge Case", "MEDIUM"));
                scenarios.add(scenario("Submit with max-length input", "Fill text fields with very long strings", "Edge Case", "MEDIUM"));
                scenarios.add(scenario("Cancel form without saving", "Fill form then cancel — verify no data saved", "CRUD", "MEDIUM"));
                scenarios.add(scenario("Double-submit prevention", "Click submit twice rapidly — verify no duplicate", "Edge Case", "HIGH"));
                break;

            case "DETAIL_PAGE":
                scenarios.add(scenario("Verify all attributes display", "Check that key fields are visible and have values", "Validation", "HIGH"));
                scenarios.add(scenario("Edit button opens edit form", "Click Edit and verify form/drawer opens", "Navigation", "HIGH"));
                scenarios.add(scenario("Back navigation works", "Click back and verify return to list page", "Navigation", "MEDIUM"));
                scenarios.add(scenario("Verify breadcrumb navigation", "Check breadcrumb links are present and clickable", "Navigation", "LOW"));
                break;

            case "DASHBOARD":
                scenarios.add(scenario("Verify KPI cards display", "Check that stat cards render with numeric values", "Validation", "HIGH"));
                scenarios.add(scenario("Verify charts render", "Check that canvas/chart elements are present", "Validation", "HIGH"));
                scenarios.add(scenario("Click navigation cards", "Click each dashboard card and verify it navigates correctly", "Navigation", "MEDIUM"));
                scenarios.add(scenario("No stuck spinners", "Verify no loading indicators remain after page load", "Validation", "HIGH"));
                scenarios.add(scenario("Console error free", "Check that no JavaScript errors in console", "Validation", "MEDIUM"));
                break;

            default:
                scenarios.add(scenario("Page loads without error", "Verify the page loads and no error screen", "Validation", "HIGH"));
                scenarios.add(scenario("No console errors", "Check JavaScript console for errors", "Validation", "MEDIUM"));
                break;
        }

        // Universal scenarios (apply to all page types)
        scenarios.add(scenario("No JavaScript console errors", "Verify no JS errors after page interactions", "Validation", "MEDIUM"));
        scenarios.add(scenario("Page responsive to resize", "Resize browser and verify layout adapts", "Edge Case", "LOW"));

        return scenarios;
    }

    /**
     * Enhanced scenario suggestion using Claude AI.
     */
    public static List<TestScenario> suggestWithAI(PageAnalysis analysis) {
        List<TestScenario> scenarios = suggestTestScenarios(analysis);

        if (!ClaudeClient.isConfigured() || analysis.aiSuggestions == null) {
            return scenarios;
        }

        for (String suggestion : analysis.aiSuggestions) {
            scenarios.add(scenario(suggestion, suggestion, "AI Suggested", "MEDIUM"));
        }

        return scenarios;
    }

    // ================================================================
    // TEST STUB GENERATION
    // ================================================================

    /**
     * Generate compilable Java test method stubs.
     */
    public static String generateTestStubs(PageAnalysis analysis, String className) {
        List<TestScenario> scenarios = suggestTestScenarios(analysis);
        StringBuilder sb = new StringBuilder();

        sb.append("package com.egalvanic.qa.testcase;\n\n");
        sb.append("import com.egalvanic.qa.utils.ExtentReportManager;\n");
        sb.append("import com.egalvanic.qa.utils.ai.SmartTestDataGenerator;\n");
        sb.append("import org.testng.Assert;\n");
        sb.append("import org.testng.annotations.Test;\n\n");
        sb.append("/**\n");
        sb.append(" * Auto-generated test stubs from AIPageAnalyzer\n");
        sb.append(" * Page: ").append(analysis.url).append("\n");
        sb.append(" * Type: ").append(analysis.pageType).append("\n");
        sb.append(" * Elements: ").append(analysis.elements.size()).append("\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append(" extends BaseTest {\n\n");
        sb.append("    private static final String MODULE = \"").append(analysis.pageType).append("\";\n");
        sb.append("    private static final String FEATURE = \"Auto-Generated\";\n\n");

        int priority = 1;
        for (TestScenario scenario : scenarios) {
            String methodName = "test_" + priority + "_" + sanitizeMethodName(scenario.name);
            sb.append("    @Test(priority = ").append(priority).append(", description = \"")
              .append(escapeJava(scenario.name)).append("\")\n");
            sb.append("    public void ").append(methodName).append("() {\n");
            sb.append("        ExtentReportManager.createTest(MODULE, FEATURE, \"")
              .append(escapeJava(scenario.name)).append("\");\n");
            sb.append("        // TODO: Implement — ").append(scenario.description).append("\n");
            sb.append("        // Category: ").append(scenario.category)
              .append(" | Priority: ").append(scenario.priority).append("\n");
            sb.append("    }\n\n");
            priority++;
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Write test stubs to file.
     */
    public static void writeTestStubs(PageAnalysis analysis, String className, String outputDir) {
        try {
            String stubs = generateTestStubs(analysis, className);
            new File(outputDir).mkdirs();
            String filePath = outputDir + "/" + className + ".java";
            try (FileWriter fw = new FileWriter(filePath)) {
                fw.write(stubs);
            }
            System.out.println("[AIPageAnalyzer] Test stubs written to: " + filePath);
        } catch (Exception e) {
            System.out.println("[AIPageAnalyzer] Failed to write stubs: " + e.getMessage());
        }
    }

    // ================================================================
    // REPORTING
    // ================================================================

    /**
     * Write analysis report as JSON.
     */
    public static void writeAnalysisReport(Map<String, PageAnalysis> analyses) {
        try {
            JSONObject report = new JSONObject();
            report.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            report.put("totalPages", analyses.size());

            int totalElements = 0, totalScenarios = 0;
            JSONArray pages = new JSONArray();

            for (Map.Entry<String, PageAnalysis> entry : analyses.entrySet()) {
                PageAnalysis a = entry.getValue();
                List<TestScenario> scenarios = suggestTestScenarios(a);
                totalElements += a.elements.size();
                totalScenarios += scenarios.size();

                JSONObject page = new JSONObject();
                page.put("name", entry.getKey());
                page.put("url", a.url);
                page.put("pageType", a.pageType);
                page.put("inputs", a.inputCount);
                page.put("buttons", a.buttonCount);
                page.put("links", a.linkCount);
                page.put("hasDataGrid", a.hasDataGrid);
                page.put("headings", a.headings);
                page.put("suggestedScenarios", scenarios.size());

                JSONArray scenarioArr = new JSONArray();
                for (TestScenario s : scenarios) {
                    JSONObject so = new JSONObject();
                    so.put("name", s.name);
                    so.put("category", s.category);
                    so.put("priority", s.priority);
                    scenarioArr.put(so);
                }
                page.put("scenarios", scenarioArr);
                pages.put(page);
            }

            report.put("totalElements", totalElements);
            report.put("totalScenarios", totalScenarios);
            report.put("pages", pages);

            new File("test-output").mkdirs();
            try (FileWriter fw = new FileWriter("test-output/page-analysis-report.json")) {
                fw.write(report.toString(2));
            }
            System.out.println("[AIPageAnalyzer] Report: test-output/page-analysis-report.json");
            System.out.println("[AIPageAnalyzer] Total: " + analyses.size() + " pages, " +
                    totalElements + " elements, " + totalScenarios + " scenarios");

        } catch (Exception e) {
            System.out.println("[AIPageAnalyzer] Failed to write report: " + e.getMessage());
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private static boolean isInputElement(DiscoveredElement el) {
        String tag = el.tagName.toLowerCase();
        return tag.equals("input") || tag.equals("textarea") || tag.equals("select");
    }

    private static TestScenario scenario(String name, String description, String category, String priority) {
        TestScenario s = new TestScenario();
        s.name = name;
        s.description = description;
        s.category = category;
        s.priority = priority;
        s.source = "RULE_BASED";
        return s;
    }

    private static String sanitizeMethodName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "");
    }

    private static String escapeJava(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ================================================================
    // DATA CLASSES
    // ================================================================

    public static class PageAnalysis {
        public String url;
        public String title;
        public String pageType;
        public List<DiscoveredElement> elements = new ArrayList<>();
        public List<String> aiSuggestions;
        public int inputCount;
        public int buttonCount;
        public int linkCount;
        public boolean hasDataGrid;
        public boolean hasDrawer;
        public boolean hasDialog;
        public List<String> headings = new ArrayList<>();
    }

    public static class DiscoveredElement {
        public String tagName;
        public String type;
        public String label;
        public String placeholder;
        public String text;
        public String role;
        public boolean isRequired;
        public boolean isDisabled;
        public String section;
    }

    public static class TestScenario {
        public String name;
        public String description;
        public String category;
        public String priority;
        public String source;
    }
}
