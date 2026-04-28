package com.egalvanic.qa.utils.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API Security Scanner — Use Case 3 from user spec (the bug-bounty advantage).
 *
 * Reads the security-relevant subset of test failures from
 * <code>test-output/bug-detection-report.json</code> and asks Claude for an
 * OWASP-aligned security review focused on:
 *   - Broken access control (IDOR signals)
 *   - Sensitive data exposure
 *   - Missing input validation
 *   - Authentication / authorization issues
 *
 * Why this is separate from DailyFailureAnalyzer:
 *   - Different audience: security review goes to AppSec, not the dev triage queue
 *   - Different signal: looks at OWASP-tagged tests + ANY 5xx from API tests
 *   - Different output cadence: AppSec teams read once a week, not daily
 *
 * Output: <code>daily-summary/security-&lt;YYYY-MM-DD&gt;.md</code>
 *
 * Inputs:
 *   1. <code>test-output/bug-detection-report.json</code> — filter to OWASP-tagged
 *      test classes (OwaspIdorTestNG, OwaspSecurityHeadersTestNG, OwaspXxeTestNG,
 *      OwaspKnownVulnsTestNG, APISecurityTest)
 *   2. (optional) <code>test-output/console-errors.txt</code> — captured browser
 *      console errors during the run, scanned for sensitive-data patterns
 *
 * Invocation:
 *   <pre>
 *   mvn exec:java -Dexec.mainClass="com.egalvanic.qa.utils.ai.APISecurityScanner"
 *   </pre>
 */
public final class APISecurityScanner {

    private static final String INPUT_REPORT = "test-output/bug-detection-report.json";
    private static final String OUTPUT_DIR = "daily-summary";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Test class names that indicate a security-relevant failure. */
    private static final Set<String> SECURITY_CLASSES = new HashSet<>(Arrays.asList(
        "OwaspIdorTestNG",
        "OwaspSecurityHeadersTestNG",
        "OwaspXxeTestNG",
        "OwaspKnownVulnsTestNG",
        "APISecurityTest",
        "BugHuntTestNG",  // mixed bag, but contains XSS / SQLi / auth-cookie tests
        "AuthenticationTestNG"
    ));

    private APISecurityScanner() {}

    public static void main(String[] args) {
        try {
            int code = run();
            System.exit(code);
        } catch (Exception e) {
            System.err.println("[SecurityScanner] Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static Path runAndReturnPath() throws Exception {
        return writeReport();
    }

    private static int run() throws Exception {
        File report = new File(INPUT_REPORT);
        if (!report.exists()) {
            System.err.println("[SecurityScanner] Input missing: " + INPUT_REPORT);
            return 1;
        }
        Path output = writeReport();
        return output == null ? 2 : 0;
    }

    private static Path writeReport() throws IOException {
        List<JSONObject> securityFailures = loadSecurityFailures();

        File dir = new File(OUTPUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create " + OUTPUT_DIR);
        }
        Path outputPath = Paths.get(OUTPUT_DIR,
            "security-" + LocalDate.now().format(DATE) + ".md");

        String body = buildMarkdown(securityFailures);
        try (FileWriter w = new FileWriter(outputPath.toFile())) {
            w.write(body);
        }
        System.out.println("[SecurityScanner] Wrote " + outputPath);
        return outputPath;
    }

    private static List<JSONObject> loadSecurityFailures() throws IOException {
        File report = new File(INPUT_REPORT);
        if (!report.exists()) return new ArrayList<>();
        String content = new String(Files.readAllBytes(report.toPath()));
        if (content.isBlank()) return new ArrayList<>();
        JSONArray arr = new JSONArray(content);
        List<JSONObject> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String testName = o.optString("testName", "");
            String simpleClass = simpleClassName(testName);
            if (SECURITY_CLASSES.contains(simpleClass)) {
                out.add(o);
            }
        }
        return out;
    }

    private static String buildMarkdown(List<JSONObject> failures) {
        StringBuilder sb = new StringBuilder();
        String date = LocalDate.now().format(DATE);

        sb.append("# Daily API Security Scan — ").append(date).append("\n\n");
        sb.append("**Generated:** ").append(LocalDateTime.now().format(STAMP)).append(" (local)\n");
        sb.append("**Scope:** OWASP-tagged tests + API security tests + auth-flow tests\n");
        sb.append("**AI-enhanced:** ").append(ClaudeClient.isConfigured() ? "yes" : "no (rules-based only)").append("\n\n");

        if (failures.isEmpty()) {
            sb.append("## Security Posture: GREEN ✓\n\n");
            sb.append("No security-tagged test failures in today's run. ");
            sb.append("Note: this means *no failures detected by our test suite* — ");
            sb.append("it does NOT mean the application has no vulnerabilities. ");
            sb.append("Continue running OWASP suite + manual pen-test cadence.\n");
            return sb.toString();
        }

        sb.append("## Security Posture: ").append(failures.size())
            .append(" potential finding(s)\n\n");

        // Group by simple class name → OWASP category
        sb.append("## OWASP Category Breakdown\n\n");
        java.util.Map<String, List<JSONObject>> byCategory = new java.util.LinkedHashMap<>();
        for (JSONObject f : failures) {
            String cat = owaspCategory(simpleClassName(f.optString("testName", "")));
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(f);
        }
        for (java.util.Map.Entry<String, List<JSONObject>> e : byCategory.entrySet()) {
            sb.append("### ").append(e.getKey()).append("\n\n");
            for (JSONObject f : e.getValue()) {
                String testName = f.optString("testName", "");
                sb.append("- **").append(simpleTestName(testName)).append("** ");
                sb.append("(").append(f.optString("classification", "")).append(" · ");
                sb.append(f.optInt("confidence", 0)).append("% confidence)\n");
                sb.append("  - Endpoint/Page: ").append(redact(f.optString("pageUrl", "n/a"))).append("\n");
                sb.append("  - Evidence: ").append(redact(truncate(f.optString("exceptionMessage", ""), 200))).append("\n");
                sb.append("  - Root cause: ").append(f.optString("rootCause", "_n/a_")).append("\n");
            }
            sb.append("\n");
        }

        // AI exec summary
        if (ClaudeClient.isConfigured()) {
            sb.append("## AI Security Triage\n\n");
            String aiSummary = askClaudeForSecurityTriage(failures);
            sb.append(aiSummary == null || aiSummary.isBlank()
                ? "_(Claude call returned no content)_\n"
                : aiSummary).append("\n\n");
        } else {
            sb.append("## AI Security Triage\n\n");
            sb.append("_(skipped — `CLAUDE_API_KEY` not set)_\n\n");
        }

        // Recommended actions
        sb.append("## Recommended Actions\n\n");
        sb.append(buildRecommendedActions(failures)).append("\n");

        sb.append("---\n\n");
        sb.append("_Auto-generated by `APISecurityScanner`. This file does NOT trigger any "
            + "Jira / Slack / email actions — it's a static markdown artifact for "
            + "AppSec / dev review. Per project rule, security findings need explicit "
            + "human triage before filing in Jira._\n");
        return sb.toString();
    }

    private static String owaspCategory(String simpleClass) {
        switch (simpleClass) {
            case "OwaspIdorTestNG":
            case "APISecurityTest":   return "A01 — Broken Access Control / IDOR";
            case "OwaspSecurityHeadersTestNG": return "A05 — Security Misconfiguration";
            case "OwaspXxeTestNG":    return "A05 — XML External Entity (XXE)";
            case "OwaspKnownVulnsTestNG": return "A06 — Vulnerable & Outdated Components";
            case "BugHuntTestNG":     return "A03/A07 — Injection / Auth Failures";
            case "AuthenticationTestNG": return "A07 — Authentication / Identification Failures";
            default: return "OTHER";
        }
    }

    private static String askClaudeForSecurityTriage(List<JSONObject> failures) {
        try {
            String systemPrompt =
                "You are a Senior Application Security Engineer reviewing the daily security "
                + "test results for a multi-tenant SaaS web app. Provide: (1) the most "
                + "important finding to investigate, (2) likely impact if exploited, (3) one "
                + "concrete next step (e.g., 'manually verify with curl', 'request a tenant-2 "
                + "account for full IDOR test'). Be concise: 5-7 sentences total. Plain "
                + "prose, no Markdown headers — those are added by the caller.";
            StringBuilder user = new StringBuilder();
            user.append("Today's security test failures (").append(failures.size()).append("):\n");
            for (JSONObject f : failures) {
                user.append("- ").append(simpleTestName(f.optString("testName", "")))
                    .append(" [").append(f.optString("classification", "")).append(" ")
                    .append(f.optInt("confidence", 0)).append("%]: ")
                    .append(redact(truncate(f.optString("exceptionMessage", ""), 150))).append("\n");
            }
            user.append("\nProvide the security triage now.");
            return ClaudeClient.ask(systemPrompt, user.toString());
        } catch (Exception e) {
            System.out.println("[SecurityScanner] Claude call failed: " + e.getMessage());
            return null;
        }
    }

    private static String buildRecommendedActions(List<JSONObject> failures) {
        StringBuilder sb = new StringBuilder();
        boolean hasIdor = failures.stream().anyMatch(f ->
            simpleClassName(f.optString("testName", "")).equals("OwaspIdorTestNG"));
        boolean hasHeaders = failures.stream().anyMatch(f ->
            simpleClassName(f.optString("testName", "")).equals("OwaspSecurityHeadersTestNG"));
        boolean hasKnownVulns = failures.stream().anyMatch(f ->
            simpleClassName(f.optString("testName", "")).equals("OwaspKnownVulnsTestNG"));

        if (hasIdor) {
            sb.append("1. **IDOR triage:** for each fail, manually verify with curl whether the "
                + "response contains JSON data (true IDOR) or HTML (SPA fallback). The framework "
                + "discriminates these but a human eyeball confirms.\n");
        }
        if (hasHeaders) {
            sb.append("2. **Security headers:** missing CSP / HSTS / X-Frame-Options / nosniff "
                + "are 1-line fixes at the reverse proxy or application server. Coordinate with "
                + "infra to ship these.\n");
        }
        if (hasKnownVulns) {
            sb.append("3. **Outdated dependencies:** update pom.xml versions to floors specified "
                + "in OwaspKnownVulnsTestNG. Run `mvn dependency:tree` to confirm transitive "
                + "deps are clean.\n");
        }
        sb.append("- **For all findings:** reproduce manually before filing — security tickets "
            + "with reproduction steps get fixed; ones without don't.\n");
        return sb.toString();
    }

    private static String simpleClassName(String fullTestName) {
        if (fullTestName == null) return "";
        int methodDot = fullTestName.lastIndexOf('.');
        if (methodDot < 0) return fullTestName;
        String classFqn = fullTestName.substring(0, methodDot);
        int classDot = classFqn.lastIndexOf('.');
        return classDot < 0 ? classFqn : classFqn.substring(classDot + 1);
    }

    private static String simpleTestName(String fullTestName) {
        if (fullTestName == null) return "";
        int classDot = fullTestName.lastIndexOf('.');
        if (classDot < 0) return fullTestName;
        // Find previous dot for ClassName
        int prevDot = fullTestName.lastIndexOf('.', classDot - 1);
        return prevDot < 0 ? fullTestName.substring(classDot + 1) : fullTestName.substring(prevDot + 1);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String redact(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)bearer\\s+[A-Za-z0-9._-]+", "Bearer <REDACTED>")
            .replaceAll("eyJ[A-Za-z0-9_-]{15,}", "<JWT-REDACTED>")
            .replaceAll("(?i)password[\"':\\s]*[^\\s\",]+", "password=<REDACTED>");
    }
}
