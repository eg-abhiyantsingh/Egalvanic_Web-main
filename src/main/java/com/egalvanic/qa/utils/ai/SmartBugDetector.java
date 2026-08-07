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
    // SYNCHRONIZED: a parallel suite adds reports from multiple worker threads; an unsynchronized
    // ArrayList drops entries or throws ArrayIndexOutOfBounds mid-add, so failures would go
    // unrecorded in the bug-detection report.
    private static final List<BugReport> reports =
            java.util.Collections.synchronizedList(new ArrayList<BugReport>());

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
        String url = report.pageUrl == null ? "" : report.pageUrl.toLowerCase();
        String dom = report.domSnippet == null ? "" : report.domSnippet;

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

        // =====================================================================
        // PAGE-STATE-AWARE RULES — every signature below was verified during the
        // 2026-08 V1.36 full-suite audit. They fire BEFORE the generic rules so a
        // failure lands on its known cause instead of a misleading "REAL_BUG 75%".
        // =====================================================================

        // (a) Session died mid-test — the page is the login form, not the module.
        if (url.contains("/login")) {
            report.classification = Classification.ENVIRONMENT_ISSUE;
            report.confidence = 90;
            report.rootCause = "Session expired mid-test — the failure page is the LOGIN form, so every "
                    + "assertion after this point is meaningless. Not a product bug.";
            report.suggestedFix = "Re-run the test. If it recurs, check token lifetime vs class duration.";
            return;
        }

        // (b) App crash boundary — this IS a real bug; capture the on-screen error ID.
        if (dom.contains("Application Error") || dom.contains("something went wrong")
                || dom.contains("We're sorry, but something went wrong")) {
            report.classification = Classification.REAL_BUG;
            report.confidence = 95;
            String errId = "";
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("error ID[:\\s]*</?[^>]*>?\\s*([a-f0-9]{8,16})").matcher(dom);
            if (m.find()) errId = " On-screen error ID: " + m.group(1) + ".";
            report.rootCause = "The app crashed to its 'Application Error' boundary — a genuine product "
                    + "crash on " + report.pageUrl + "." + errId + " (Example: /goals TypeError crash, 2026-08-05.)";
            report.suggestedFix = "File to ZP with the error ID + a console-error capture from this route.";
            return;
        }

        // (c) Access Denied — the route is role-gated and the session's ACTIVE ROLE can't see it
        //     (V1.36 split modules across the 'Super Admin' operational and 'Admin' setup consoles).
        if (dom.contains("Access Denied") || dom.contains("do not have permission")) {
            report.classification = Classification.TEST_OR_DATA_ISSUE;
            report.confidence = 90;
            report.rootCause = "Page shows ACCESS DENIED — the V1.36 role consoles split module access "
                    + "('Super Admin' = operational, 'Admin' = setup: Forms/Classes/PM-Plans/Goals-perms). "
                    + "The test ran under a role that cannot see this route. Not a functional product bug "
                    + "(unless the active role SHOULD hold this permission — check /auth/v2/me).";
            report.suggestedFix = "Pin/switch the role before this step (BaseTest.ensureActiveRole / "
                    + "RolePinUtil), or assert the denial intentionally.";
            return;
        }

        // (d) WO create-redirect trap — a successful create redirects to /sessions/{id} (detail page,
        //     no grid/search box); grid assertions there fail even though the create WORKED.
        if (url.matches(".*/sessions/[a-f0-9-]{8,}.*")
                && (msg.contains("grid") || msg.contains("search") || msg.contains("appear"))) {
            report.classification = Classification.TEST_OR_DATA_ISSUE;
            report.confidence = 85;
            report.rootCause = "The failure page is a WORK-ORDER DETAIL (/sessions/{id}) — the create "
                    + "succeeded and redirected; the grid/search this assertion needs only exists on the "
                    + "LIST page. Navigate back to /sessions before verifying.";
            report.suggestedFix = "Use isOnWoDetailPage() -> navigateToWorkOrders() before grid asserts.";
            return;
        }

        // (e) Checked while still loading — spinner in <main> when the 'did not render' verdict fired.
        if ((dom.contains("MuiCircularProgress") || dom.contains("MuiSkeleton"))
                && (msg.contains("did not render") || msg.contains("not visible") || msg.contains("should show"))) {
            report.classification = Classification.TEST_OR_DATA_ISSUE;
            report.confidence = 85;
            report.rootCause = "The page was STILL LOADING (spinner/skeleton in <main>) when the check "
                    + "concluded 'not rendered'. Slow module API, not a missing module.";
            report.suggestedFix = "Poll for content (see NewModulesSmokeTestNG.smokeAssertShellRendered) "
                    + "instead of a single-shot check.";
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

        // --- Assertion mismatches: REAL only with corroboration ---
        // The old rule stamped every "expected X but found Y" as REAL_BUG 75%, which flooded the
        // reports with false bugs (the 2026-08 audit found ~72 of 78 such fails were stale test
        // contracts after the V1.36 redesign). A mismatch is only claimed REAL when the page also
        // shows independent evidence (console errors). Otherwise it is explicitly UNVERIFIED.
        if (ex.equals("AssertionError")) {
            if (msg.contains("expected") && msg.contains("but found") || msg.contains("but was")) {
                if (report.consoleErrors != null && !report.consoleErrors.isEmpty()) {
                    report.classification = Classification.REAL_BUG;
                    report.confidence = 85;
                    report.rootCause = "Assertion failed AND the page logged console errors — likely a "
                            + "genuine application bug (corroborated).";
                    report.suggestedFix = "Reproduce once manually, then file with the console errors attached.";
                } else {
                    report.classification = Classification.TEST_OR_DATA_ISSUE;
                    report.confidence = 50;
                    report.rootCause = "Assertion mismatch WITHOUT corroborating signals (no console "
                            + "errors, no crash page, no denial). Most such mismatches in the 2026-08 "
                            + "V1.36 audit were STALE TEST EXPECTATIONS (moved fields, redesigned "
                            + "schemas, renamed labels) — reproduce manually before treating as a bug.";
                    report.suggestedFix = "Open the page yourself and compare the live contract against "
                            + "the test's expectation; update the test if the product changed by design.";
                }
                return;
            }
        }

        // Default — honest 'unknown', not a fabricated bug claim.
        report.classification = Classification.TEST_OR_DATA_ISSUE;
        report.confidence = 40;
        report.rootCause = "Unclassified failure — needs manual investigation before any bug claim";
        report.suggestedFix = "Review the stack trace, screenshot, and page state captured in this report";
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
                    + "Respond ONLY as JSON: {\"classification\": \"REAL_BUG|FLAKY_TEST|ENVIRONMENT_ISSUE|LOCATOR_CHANGE|TEST_OR_DATA_ISSUE\", "
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

        int bugs = 0, flaky = 0, env = 0, locator = 0, testIssue = 0;
        for (BugReport r : reports) {
            switch (r.classification) {
                case REAL_BUG: bugs++; break;
                case FLAKY_TEST: flaky++; break;
                case ENVIRONMENT_ISSUE: env++; break;
                case LOCATOR_CHANGE: locator++; break;
                case TEST_OR_DATA_ISSUE: testIssue++; break;
            }
        }
        sb.append(String.format("Total: %d | Bugs: %d | Test/Data: %d | Flaky: %d | Env: %d | Locator: %d\n\n",
                reports.size(), bugs, testIssue, flaky, env, locator));

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
        LOCATOR_CHANGE,
        /**
         * The failure is on the TEST side (stale contract, wrong role/console, wrong page,
         * data precondition) or cannot be corroborated as a product bug. 2026-08 V1.36 audit:
         * ~72 of 78 full-suite failures were this — do NOT file these to Jira without a manual
         * reproduction first.
         */
        TEST_OR_DATA_ISSUE
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
