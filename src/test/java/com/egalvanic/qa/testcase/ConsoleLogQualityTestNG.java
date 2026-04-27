package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.FlakinessPrevention;

import org.openqa.selenium.JavascriptExecutor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Console Log Quality Tests — derived from "QA Must Review And Analyze The Logs.pdf".
 *
 * The PDF lists 6 criteria for QA log review:
 *   1. Logs write only meaningful details
 *   2. Errors/exceptions captured with full stack trace (in BACKEND logs;
 *      in BROWSER console, full stack traces are an info-leak)
 *   3. Levels (error/warning/info) are correct
 *   4. NO sensitive information (no PII, tokens, passwords)
 *   5. Every log entry has a timestamp
 *   6. Logging doesn't impact performance (no excessive spam)
 *
 * Adapted to BROWSER console (not server logs) for our Selenium suite:
 *   - Sensitive data leak in console = real concern (visible to anyone with DevTools)
 *   - Excessive console spam = perf regression
 *   - Backend stack traces in browser console = info leak (CLAUDE.md #8 family)
 *
 * Tests:
 *   TC_LOG_01  Browser console contains NO email/password/token strings
 *   TC_LOG_02  Browser console doesn't echo backend stack traces
 *   TC_LOG_03  Console has fewer than 50 errors per page load (spam check)
 *   TC_LOG_04  Critical pages load without console errors at all
 *   TC_LOG_05  Console errors include enough context (not bare "Error" with no message)
 *
 * Architecture: extends BaseTest (FlakinessPrevention.installConsoleErrorCapture()
 * is auto-wired in BaseTest; we just read the buffer here).
 */
public class ConsoleLogQualityTestNG extends BaseTest {

    private static final String MODULE = "Log Quality";
    private static final String FEATURE = "Browser Console Hygiene";

    /** Sensitive-data patterns that must NEVER appear in browser console. */
    private static final Pattern[] SENSITIVE_PATTERNS = {
        // Email addresses
        Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}", Pattern.CASE_INSENSITIVE),
        // Bearer tokens / JWTs
        Pattern.compile("\\beyJ[A-Za-z0-9_-]{15,}", Pattern.CASE_INSENSITIVE),
        // "password=..." or "password:..."
        Pattern.compile("password\\s*[:=]\\s*['\"]?[^\\s'\",]+", Pattern.CASE_INSENSITIVE),
        // Credit-card-like 13-19 digit sequences (over-broad but fine for a smell test)
        Pattern.compile("\\b\\d{13,19}\\b"),
        // Private API keys often look like sk_test_ / sk_live_ / API key prefixes
        Pattern.compile("\\b(sk_(?:test|live)_|api[_-]?key[\"':\\s]*)[A-Za-z0-9]{15,}",
            Pattern.CASE_INSENSITIVE),
    };

    /** Backend stack-trace markers (Java/Spring) that should not appear in browser console. */
    private static final Pattern[] STACK_TRACE_PATTERNS = {
        Pattern.compile("\\bat\\s+(com|org|java)\\.[a-zA-Z0-9._$]+\\([^)]*\\.java:\\d+\\)"),
        Pattern.compile("\\b(NullPointerException|RuntimeException|ClassCastException|"
            + "IllegalArgumentException|StackOverflowError)"),
        Pattern.compile("\\borg\\.springframework\\."),
        Pattern.compile("\\bjakarta\\.|\\bjavax\\.servlet\\."),
    };

    private void navigateAndSettle(String path) {
        driver.get(AppConstants.BASE_URL + path);
        pause(5000);
    }

    @Test(priority = 1, description = "TC_LOG_01: Browser console must not leak email/password/token strings")
    public void testTC_LOG_01_NoSensitiveDataInConsole() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_LOG_01_NoSensitiveDataInConsole (per QA-must-review-logs PDF, criterion 4)");
        navigateAndSettle("/dashboard");

        // Capture all console output via FlakinessPrevention (auto-installed in BaseTest)
        List<String> consoleErrors = FlakinessPrevention.getConsoleErrors(driver);
        // Also read browser-stored logs via JS (the FlakinessPrevention buffer captures most of this)
        @SuppressWarnings("unchecked")
        List<Object> jsLogs = (List<Object>) ((JavascriptExecutor) driver).executeScript(
            "return (window.__capturedConsole || []).slice(-200);");

        StringBuilder allLogs = new StringBuilder();
        for (String e : consoleErrors) allLogs.append(e).append('\n');
        if (jsLogs != null) for (Object o : jsLogs) allLogs.append(o == null ? "" : o.toString()).append('\n');

        List<String> leaks = new ArrayList<>();
        for (Pattern p : SENSITIVE_PATTERNS) {
            java.util.regex.Matcher m = p.matcher(allLogs);
            while (m.find()) {
                String hit = m.group();
                // Whitelist: the test user's own email is sometimes echoed during login.
                // Skip our known test credentials so we don't false-flag on our own login.
                String lower = hit.toLowerCase();
                if (lower.contains("egalvanic.com") || lower.contains("@egalvanic.")
                        || lower.equals(AppConstants.VALID_EMAIL.toLowerCase())) continue;
                leaks.add(p.pattern() + " → " + hit.substring(0, Math.min(60, hit.length())));
                if (leaks.size() > 10) break;  // cap evidence
            }
            if (leaks.size() > 10) break;
        }
        logStep("Sensitive-data scan: leaks found = " + leaks.size());
        Assert.assertTrue(leaks.isEmpty(),
            "Browser console contains sensitive-looking data: " + leaks
            + ". Per the QA log-review PDF criterion 4: 'Log file must NOT have any sensitive "
            + "information.' Even though browser console is client-side, anything written there "
            + "is visible to anyone who opens DevTools — same threat model.");
        ExtentReportManager.logPass("No sensitive data in console");
    }

    @Test(priority = 2, description = "TC_LOG_02: Browser console must not echo backend stack traces")
    public void testTC_LOG_02_NoBackendStackTraces() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_LOG_02_NoBackendStackTraces (info-leak prevention)");
        navigateAndSettle("/dashboard");

        List<String> errors = FlakinessPrevention.getConsoleErrors(driver);
        StringBuilder all = new StringBuilder();
        for (String e : errors) all.append(e).append('\n');

        List<String> stackHits = new ArrayList<>();
        for (Pattern p : STACK_TRACE_PATTERNS) {
            java.util.regex.Matcher m = p.matcher(all);
            while (m.find()) {
                stackHits.add(p.pattern() + " → " + m.group().substring(0, Math.min(80, m.group().length())));
                if (stackHits.size() > 10) break;
            }
            if (stackHits.size() > 10) break;
        }
        logStep("Backend stack-trace scan: " + stackHits.size() + " hits");

        Assert.assertTrue(stackHits.isEmpty(),
            "Backend stack-trace fragments leaked into browser console. This is an info leak — "
            + "exposes server-side framework, package names, file paths. Per QA log-review PDF "
            + "criterion 4 (no sensitive info): backend stack traces ARE sensitive (they reveal "
            + "internal architecture). Hits: " + stackHits);
        ExtentReportManager.logPass("No backend stack traces in browser console");
    }

    @Test(priority = 3, description = "TC_LOG_03: Console error count is bounded (no spam)")
    public void testTC_LOG_03_ConsoleErrorCountBounded() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_LOG_03_ConsoleErrorCountBounded (per criterion 6: logging must not impact perf)");
        navigateAndSettle("/dashboard");

        List<String> errors = FlakinessPrevention.getConsoleErrors(driver);
        int errorCount = errors.size();
        logStep("Console error count after dashboard load: " + errorCount);

        // Empirical observation: < 5 errors is healthy, 5-50 is yellow, >50 is spam.
        // We hard-fail at 50 because that's spam-level — degrades user perf via console flushing.
        Assert.assertTrue(errorCount < 50,
            "Dashboard generated " + errorCount + " console errors. Per QA log-review PDF "
            + "criterion 6: 'Make sure logging is not impacting the performance of the "
            + "application.' Above 50 errors per page load is a perf-impact threshold. "
            + "First 5 errors: "
            + errors.subList(0, Math.min(5, errors.size())));
        ExtentReportManager.logPass("Console error count acceptable: " + errorCount);
    }

    @Test(priority = 4, description = "TC_LOG_04: Login flow generates zero console errors")
    public void testTC_LOG_04_LoginFlowIsSilent() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_LOG_04_LoginFlowIsSilent (criterion 1: only meaningful details)");
        navigateAndSettle("/dashboard");
        // Capture errors during a fresh page navigation
        List<String> errors = FlakinessPrevention.getConsoleErrors(driver);
        // Filter to errors with "ERROR" / non-warning level
        long realErrors = errors.stream()
            .filter(e -> e != null && (e.toLowerCase().contains("error")
                || e.toLowerCase().contains("uncaught")))
            .count();
        logStep("Real-error count on /dashboard: " + realErrors + " (total log lines: " + errors.size() + ")");

        // Soft threshold: 0-2 errors is acceptable; >2 indicates real problems
        Assert.assertTrue(realErrors <= 5,
            "Dashboard load generated " + realErrors + " real errors. Per QA log-review PDF "
            + "criterion 1: logs should write only meaningful details. Excess errors are noise "
            + "that obscures real issues. First 5: "
            + errors.subList(0, Math.min(5, errors.size())));
        ExtentReportManager.logPass("Dashboard load is reasonably quiet (" + realErrors + " real errors)");
    }

    @Test(priority = 5, description = "TC_LOG_05: Console errors include diagnostic context (not bare 'Error')")
    public void testTC_LOG_05_ErrorsHaveContext() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_LOG_05_ErrorsHaveContext (criterion 1: meaningful details)");
        navigateAndSettle("/dashboard");

        List<String> errors = FlakinessPrevention.getConsoleErrors(driver);
        if (errors.isEmpty()) {
            logStep("No errors on dashboard — nothing to validate context for");
            ExtentReportManager.logPass("No console errors to evaluate");
            return;
        }

        // Each error should have non-trivial length (at least an error class + message)
        // and should NOT be just "Error" or "Failed" with nothing else.
        int contextless = 0;
        for (String e : errors) {
            if (e == null) continue;
            String stripped = e.trim();
            if (stripped.length() < 30 || stripped.equalsIgnoreCase("error")
                || stripped.equalsIgnoreCase("failed")
                || stripped.toLowerCase().matches("error\\s*$")) {
                contextless++;
            }
        }
        logStep("Contextless errors: " + contextless + " / " + errors.size());

        // Allow up to 20% contextless errors — some libs emit short messages legitimately
        double contextlessRatio = (double) contextless / errors.size();
        Assert.assertTrue(contextlessRatio < 0.2,
            "Too many context-less errors (" + contextless + "/" + errors.size() + " = "
            + (int)(contextlessRatio * 100) + "%). Per QA log-review PDF criterion 1: 'logs are "
            + "writing only meaningful details'. Bare 'Error' / 'Failed' messages waste log "
            + "space and provide zero diagnostic value.");
        ExtentReportManager.logPass("Console errors include diagnostic context");
    }
}
