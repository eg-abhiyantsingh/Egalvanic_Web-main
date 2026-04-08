package com.egalvanic.qa.utils.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Smart Bug Detection — AI-powered failure analysis.
 *
 * When a test fails, this class:
 * 1. Captures page state (URL, DOM snippet, console errors, network errors)
 * 2. Takes a screenshot
 * 3. Classifies the failure: REAL_BUG | FLAKY_TEST | ENVIRONMENT_ISSUE | LOCATOR_CHANGE
 * 4. Generates a root-cause analysis with suggested fix
 * 5. Writes results to a structured JSON report
 *
 * Works in two modes:
 *   - WITH Claude API key: Full AI analysis (recommended)
 *   - WITHOUT API key: Rule-based heuristic classification (no cost, always available)
 */
public class SmartBugDetector {

    private static final String REPORT_PATH = "test-output/bug-detection-report.json";
    private static final List<BugReport> reports = new ArrayList<>();

    /**
     * Analyze a test failure and produce a structured bug report.
     *
     * @param driver       WebDriver instance (must still be alive)
     * @param testName     fully qualified test name (e.g., "AssetPart3TestNG.testGEN_EAD_12")
     * @param throwable    the exception that caused the failure
     * @param testDuration how long the test ran before failing (ms)
     * @return BugReport with classification and analysis
     */
    public static BugReport analyze(WebDriver driver, String testName, Throwable throwable, long testDuration) {
        System.out.println("[BugDetect] Analyzing failure: " + testName);

        BugReport report = new BugReport();
        report.testName = testName;
        report.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        report.exceptionType = throwable.getClass().getSimpleName();
        report.exceptionMessage = throwable.getMessage() != null
                ? throwable.getMessage().substring(0, Math.min(500, throwable.getMessage().length()))
                : "null";
        report.testDurationMs = testDuration;

        // Capture page state
        try {
            report.pageUrl = driver.getCurrentUrl();
            report.pageTitle = driver.getTitle();
        } catch (Exception e) {
            report.pageUrl = "unavailable (driver error)";
            report.pageTitle = "unavailable";
        }

        // Capture console errors
        report.consoleErrors = captureConsoleErrors(driver);

        // Capture DOM snippet (for context)
        report.domSnippet = captureDomSnippet(driver);

        // Capture screenshot as base64
        report.screenshotBase64 = captureScreenshot(driver);

        // Classify: rule-based first, then AI-enhanced
        classifyWithRules(report);

        if (ClaudeClient.isConfigured()) {
            enhanceWithAI(report);
        }

        reports.add(report);
        System.out.println("[BugDetect] Classification: " + report.classification
                + " | Confidence: " + report.confidence + "%");
        System.out.println("[BugDetect] Root cause: " + report.rootCause);

        return report;
    }

    // =========================================================================
    // RULE-BASED CLASSIFICATION (always available, no API cost)
    // =========================================================================

    private static void classifyWithRules(BugReport report) {
        String ex = report.exceptionType;
        String msg = report.exceptionMessage.toLowerCase();

        // --- ENVIRONMENT_ISSUE indicators ---
        if (ex.equals("SessionNotCreatedException") || ex.equals("WebDriverException")
                || msg.contains("chrome not reachable") || msg.contains("session not created")
                || msg.contains("connection refused") || msg.contains("ERR_CONNECTION")) {
            report.classification = Classification.ENVIRONMENT_ISSUE;
            report.confidence = 95;
            report.rootCause = "Browser/WebDriver infrastructure failure";
            report.suggestedFix = "Check CI runner health, Chrome version, and network connectivity";
            return;
        }

        if (msg.contains("timeout") || ex.equals("TimeoutException")) {
            if (report.testDurationMs > 30000) {
                report.classification = Classification.ENVIRONMENT_ISSUE;
                report.confidence = 80;
                report.rootCause = "Page load or element wait timeout — likely slow CI environment";
                report.suggestedFix = "Increase timeout or check server response time in CI";
            } else {
                report.classification = Classification.FLAKY_TEST;
                report.confidence = 70;
                report.rootCause = "Element not ready within timeout — possible race condition";
                report.suggestedFix = "Add explicit wait or polling for element visibility";
            }
            return;
        }

        // --- LOCATOR_CHANGE indicators ---
        if (ex.equals("NoSuchElementException")) {
            report.classification = Classification.LOCATOR_CHANGE;
            report.confidence = 85;
            report.rootCause = "Element not found in DOM — locator may be stale after UI update";
            report.suggestedFix = "Use SelfHealingLocator or update the XPath/CSS selector";
            return;
        }

        // --- FLAKY_TEST indicators ---
        if (ex.equals("StaleElementReferenceException")) {
            report.classification = Classification.FLAKY_TEST;
            report.confidence = 90;
            report.rootCause = "Element was found but became detached from DOM — React re-render race";
            report.suggestedFix = "Re-find element after scroll/click or use WebDriverWait for staleness";
            return;
        }

        if (ex.equals("ElementClickInterceptedException")) {
            report.classification = Classification.FLAKY_TEST;
            report.confidence = 85;
            report.rootCause = "Click intercepted by overlay (MUI Backdrop, Beamer, dialog)";
            report.suggestedFix = "Call dismissBackdrops() before click, or use JS click fallback";
            return;
        }

        // --- REAL_BUG indicators ---
        if (ex.equals("AssertionError") || ex.equals("AssertionError")) {
            // Check if it's a data mismatch (likely real bug) vs timing issue
            if (msg.contains("expected") && msg.contains("but found") || msg.contains("but was")) {
                if (report.consoleErrors != null && !report.consoleErrors.isEmpty()) {
                    report.classification = Classification.REAL_BUG;
                    report.confidence = 90;
                    report.rootCause = "Assertion failed with console errors — likely application bug";
                } else {
                    report.classification = Classification.REAL_BUG;
                    report.confidence = 75;
                    report.rootCause = "Assertion failed — data mismatch between expected and actual";
                }
                report.suggestedFix = "Verify the application behavior manually. Check API response data.";
                return;
            }
        }

        // Default
        report.classification = Classification.REAL_BUG;
        report.confidence = 50;
        report.rootCause = "Unclassified failure — needs manual investigation";
        report.suggestedFix = "Review the stack trace and screenshot";
    }

    // =========================================================================
    // AI-ENHANCED ANALYSIS (uses Claude for deeper insight)
    // =========================================================================

    private static void enhanceWithAI(BugReport report) {
        try {
            String systemPrompt =
                    "You are a senior QA automation engineer analyzing a Selenium test failure. "
                    + "Classify the failure and provide root cause analysis. "
                    + "The application is a React MUI enterprise platform (eGalvanic). "
                    + "Respond ONLY as JSON: {\"classification\": \"REAL_BUG|FLAKY_TEST|ENVIRONMENT_ISSUE|LOCATOR_CHANGE\", "
                    + "\"confidence\": 0-100, \"rootCause\": \"one sentence\", \"suggestedFix\": \"one sentence\", "
                    + "\"riskLevel\": \"HIGH|MEDIUM|LOW\"}";

            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("TEST: ").append(report.testName).append("\n");
            userPrompt.append("EXCEPTION: ").append(report.exceptionType).append(": ").append(report.exceptionMessage).append("\n");
            userPrompt.append("DURATION: ").append(report.testDurationMs).append("ms\n");
            userPrompt.append("PAGE URL: ").append(report.pageUrl).append("\n");
            userPrompt.append("CONSOLE ERRORS: ").append(report.consoleErrors).append("\n");
            userPrompt.append("RULE-BASED CLASSIFICATION: ").append(report.classification).append(" (").append(report.confidence).append("%)\n");
            if (report.domSnippet != null) {
                userPrompt.append("DOM CONTEXT (truncated):\n").append(report.domSnippet).append("\n");
            }

            String aiResponse;
            // Use vision if we have a screenshot and it's a visual issue
            if (report.screenshotBase64 != null
                    && (report.classification == Classification.REAL_BUG
                        || report.classification == Classification.LOCATOR_CHANGE)) {
                aiResponse = ClaudeClient.askWithImage(systemPrompt, userPrompt.toString(), report.screenshotBase64);
            } else {
                aiResponse = ClaudeClient.ask(systemPrompt, userPrompt.toString());
            }

            if (aiResponse != null) {
                JSONObject ai = extractJsonObject(aiResponse);
                if (ai != null) {
                    report.classification = Classification.valueOf(ai.getString("classification"));
                    report.confidence = ai.getInt("confidence");
                    report.rootCause = ai.getString("rootCause");
                    report.suggestedFix = ai.getString("suggestedFix");
                    report.riskLevel = ai.optString("riskLevel", "MEDIUM");
                    report.aiEnhanced = true;
                }
            }
        } catch (Exception e) {
            System.out.println("[BugDetect] AI enhancement failed: " + e.getMessage());
            // Keep rule-based classification
        }
    }

    // =========================================================================
    // DATA CAPTURE
    // =========================================================================

    private static String captureConsoleErrors(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) js.executeScript(
                    "if (!window.__capturedErrors) return [];"
                    + "return window.__capturedErrors.slice(-5);");
            return errors != null && !errors.isEmpty() ? String.join("; ", errors) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String captureDomSnippet(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            return (String) js.executeScript(
                    "var main = document.querySelector('main') || document.body;"
                    + "var html = main.innerHTML;"
                    + "return html.substring(0, Math.min(3000, html.length));");
        } catch (Exception e) {
            return null;
        }
    }

    private static String captureScreenshot(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // REPORT GENERATION
    // =========================================================================

    /**
     * Write all accumulated bug reports to a JSON file.
     */
    public static void writeReport() {
        if (reports.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray();
            for (BugReport r : reports) {
                JSONObject obj = new JSONObject();
                obj.put("testName", r.testName);
                obj.put("timestamp", r.timestamp);
                obj.put("classification", r.classification.name());
                obj.put("confidence", r.confidence);
                obj.put("rootCause", r.rootCause);
                obj.put("suggestedFix", r.suggestedFix);
                obj.put("riskLevel", r.riskLevel);
                obj.put("exceptionType", r.exceptionType);
                obj.put("exceptionMessage", r.exceptionMessage);
                obj.put("pageUrl", r.pageUrl);
                obj.put("testDurationMs", r.testDurationMs);
                obj.put("aiEnhanced", r.aiEnhanced);
                arr.put(obj);
            }

            Path path = Paths.get(REPORT_PATH);
            Files.createDirectories(path.getParent());
            Files.write(path, arr.toString(2).getBytes());
            System.out.println("[BugDetect] Report written: " + REPORT_PATH + " (" + reports.size() + " entries)");
        } catch (Exception e) {
            System.out.println("[BugDetect] Could not write report: " + e.getMessage());
        }
    }

    /**
     * Get a human-readable summary of all detected bugs.
     */
    public static String getSummary() {
        if (reports.isEmpty()) return "No failures analyzed in this session.";
        StringBuilder sb = new StringBuilder();
        sb.append("=== SMART BUG DETECTION REPORT ===\n");

        int bugs = 0, flaky = 0, env = 0, locator = 0;
        for (BugReport r : reports) {
            switch (r.classification) {
                case REAL_BUG: bugs++; break;
                case FLAKY_TEST: flaky++; break;
                case ENVIRONMENT_ISSUE: env++; break;
                case LOCATOR_CHANGE: locator++; break;
            }
        }
        sb.append(String.format("Total: %d | Bugs: %d | Flaky: %d | Env: %d | Locator: %d\n\n",
                reports.size(), bugs, flaky, env, locator));

        for (BugReport r : reports) {
            String aiTag = r.aiEnhanced ? " [AI]" : " [Rules]";
            sb.append(String.format("  %s %s (%d%% confidence)%s\n", r.classification, r.testName, r.confidence, aiTag));
            sb.append(String.format("    Root cause: %s\n", r.rootCause));
            sb.append(String.format("    Fix: %s\n\n", r.suggestedFix));
        }
        return sb.toString();
    }

    /**
     * Clear accumulated reports (call between suites).
     */
    public static void reset() {
        reports.clear();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static JSONObject extractJsonObject(String response) {
        try {
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return new JSONObject(response.substring(start, end + 1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    // DATA CLASSES
    // =========================================================================

    public enum Classification {
        REAL_BUG,
        FLAKY_TEST,
        ENVIRONMENT_ISSUE,
        LOCATOR_CHANGE
    }

    public static class BugReport {
        public String testName;
        public String timestamp;
        public Classification classification;
        public int confidence;
        public String rootCause;
        public String suggestedFix;
        public String riskLevel = "MEDIUM";
        public String exceptionType;
        public String exceptionMessage;
        public String pageUrl;
        public String pageTitle;
        public long testDurationMs;
        public String consoleErrors;
        public String domSnippet;
        public String screenshotBase64;
        public boolean aiEnhanced = false;
    }
}
