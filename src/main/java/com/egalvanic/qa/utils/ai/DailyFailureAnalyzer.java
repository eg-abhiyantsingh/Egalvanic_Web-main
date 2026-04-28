package com.egalvanic.qa.utils.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Daily Failure Analyzer — the "Routine Run" executor (Use Case 1 + 4 from user spec).
 *
 * Reads test artifacts from the most recent CI run and asks Claude to produce an
 * executive summary suitable for a manager / standup. Output goes to
 * <code>daily-summary/&lt;YYYY-MM-DD&gt;.md</code> — a static, git-committed,
 * human-readable file.
 *
 * Why this is structured as a main-method utility (not a JUnit/TestNG test):
 *   - It runs OUTSIDE the test execution lifecycle. Test execution writes the
 *     reports; this analyzer reads them. Treating it as a test would create a
 *     chicken-and-egg loop (where would its own report go?).
 *   - It's invokable from CI via `mvn exec:java` or directly via `java -cp`.
 *
 * Inputs (read in this order):
 *   1. <code>test-output/bug-detection-report.json</code> — JSON array of BugReport
 *      entries written by SmartBugDetector at end of every run
 *   2. <code>ready-bug/*.md</code> — per-failure markdown files from ReadyBugGenerator
 *   3. (optional) <code>test-output/bug-detection-report-prev.json</code> —
 *      yesterday's report for trend analysis
 *
 * Output:
 *   <code>daily-summary/&lt;YYYY-MM-DD&gt;.md</code> with sections:
 *     - Executive Summary (3-5 sentences)
 *     - Top 5 Failures (ranked)
 *     - Per-failure: Root cause, severity, real-bug-vs-flake, suggested fix
 *     - Trend (today vs yesterday)
 *     - 3-bullet Action Items for the next CI run
 *
 * No-Claude-key fallback:
 *   If <code>ClaudeClient.isConfigured()</code> is false, this writer produces a
 *   deterministic non-AI summary using only the rules-based fields from
 *   bug-detection-report.json. The summary is shorter but still useful — keeps
 *   the framework working without a paid API key.
 *
 * Invocation:
 *   <pre>
 *   mvn exec:java -Dexec.mainClass="com.egalvanic.qa.utils.ai.DailyFailureAnalyzer"
 *   # OR
 *   java -cp target/classes:target/dependency/* com.egalvanic.qa.utils.ai.DailyFailureAnalyzer
 *   </pre>
 *
 * Exit codes:
 *   0 — summary written successfully (any number of failures)
 *   1 — input missing (no test-output/bug-detection-report.json found)
 *   2 — output write failed
 */
public final class DailyFailureAnalyzer {

    private static final String INPUT_REPORT = "test-output/bug-detection-report.json";
    private static final String INPUT_PREV   = "test-output/bug-detection-report-prev.json";
    private static final String READY_BUG_DIR = "ready-bug";
    private static final String OUTPUT_DIR    = "daily-summary";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DailyFailureAnalyzer() {}

    public static void main(String[] args) {
        try {
            int code = run();
            System.exit(code);
        } catch (Exception e) {
            System.err.println("[DailyAnalyzer] Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    /** Invokable from a TestNG test for local sanity-check. Returns the file path. */
    public static Path runAndReturnPath() throws Exception {
        File report = new File(INPUT_REPORT);
        if (!report.exists()) {
            System.out.println("[DailyAnalyzer] No test-output/bug-detection-report.json found. "
                + "Run the test suite first.");
            return null;
        }
        return writeSummary();
    }

    private static int run() throws Exception {
        File report = new File(INPUT_REPORT);
        if (!report.exists()) {
            System.err.println("[DailyAnalyzer] Input missing: " + INPUT_REPORT);
            return 1;
        }
        Path output = writeSummary();
        return output == null ? 2 : 0;
    }

    private static Path writeSummary() throws IOException {
        // Load today's bug report
        List<BugEntry> today = loadBugReport(INPUT_REPORT);
        // Load yesterday's report (optional, for trend)
        List<BugEntry> yesterday = new ArrayList<>();
        File prev = new File(INPUT_PREV);
        if (prev.exists()) {
            yesterday = loadBugReport(INPUT_PREV);
        }
        // Load recent ready-bug markdown files (last 24h) for richer context
        List<String> recentReadyBugSummaries = loadRecentReadyBugSummaries(24);

        // Build the markdown summary
        String markdown = buildMarkdown(today, yesterday, recentReadyBugSummaries);

        // Write to daily-summary/<today>.md
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create " + OUTPUT_DIR);
        }
        Path outputPath = Paths.get(OUTPUT_DIR, LocalDate.now().format(DATE) + ".md");
        try (FileWriter w = new FileWriter(outputPath.toFile())) {
            w.write(markdown);
        }
        System.out.println("[DailyAnalyzer] Wrote " + outputPath);
        return outputPath;
    }

    /** Parse bug-detection-report.json into BugEntry POJOs. */
    private static List<BugEntry> loadBugReport(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        if (content.isBlank()) return new ArrayList<>();
        JSONArray arr = new JSONArray(content);
        List<BugEntry> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            BugEntry e = new BugEntry();
            e.testName = o.optString("testName", "");
            e.classification = o.optString("classification", "UNKNOWN");
            e.confidence = o.optInt("confidence", 0);
            e.severity = o.optString("riskLevel", "MEDIUM");
            e.exceptionType = o.optString("exceptionType", "");
            e.exceptionMessage = o.optString("exceptionMessage", "");
            e.rootCause = o.optString("rootCause", "");
            e.suggestedFix = o.optString("suggestedFix", "");
            e.pageUrl = o.optString("pageUrl", "");
            e.timestamp = o.optString("timestamp", "");
            e.aiEnhanced = o.optBoolean("aiEnhanced", false);
            out.add(e);
        }
        return out;
    }

    /** Load .md filenames from ready-bug/ that were modified in the last `hours`. */
    private static List<String> loadRecentReadyBugSummaries(int hours) {
        File dir = new File(READY_BUG_DIR);
        if (!dir.exists()) return new ArrayList<>();
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        try (Stream<Path> stream = Files.list(dir.toPath())) {
            return stream
                .filter(p -> p.toString().endsWith(".md") && !p.getFileName().toString().equals("README.md"))
                .filter(p -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                        return attrs.lastModifiedTime().toInstant().isAfter(cutoff);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .map(p -> p.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /** Build the daily-summary markdown body. Falls back gracefully without Claude. */
    private static String buildMarkdown(List<BugEntry> today, List<BugEntry> yesterday,
                                         List<String> readyBugFiles) {
        StringBuilder sb = new StringBuilder();
        String date = LocalDate.now().format(DATE);
        String stamp = LocalDateTime.now().format(STAMP);

        sb.append("# Daily Test Failure Summary — ").append(date).append("\n\n");
        sb.append("**Generated:** ").append(stamp).append(" (local)\n");
        sb.append("**Source:** `").append(INPUT_REPORT).append("` + `").append(READY_BUG_DIR).append("/`\n");
        sb.append("**AI-enhanced:** ").append(ClaudeClient.isConfigured() ? "yes" : "no (rules-based only)").append("\n\n");

        // ── Executive Summary ──
        sb.append("## Executive Summary\n\n");
        if (today.isEmpty()) {
            sb.append("No failures recorded in today's CI run. ");
            sb.append("Test suite is fully green or no run has occurred since last reset.\n\n");
            return sb.toString();
        }

        // Counts breakdown
        Map<String, Long> classificationCounts = today.stream()
            .collect(Collectors.groupingBy(b -> b.classification, Collectors.counting()));
        Map<String, Long> severityCounts = today.stream()
            .collect(Collectors.groupingBy(b -> b.severity, Collectors.counting()));

        sb.append("- **Total failures today:** ").append(today.size()).append("\n");
        sb.append("- **By classification:** ").append(classificationCounts).append("\n");
        sb.append("- **By severity:** ").append(severityCounts).append("\n");
        if (!yesterday.isEmpty()) {
            int delta = today.size() - yesterday.size();
            sb.append("- **Trend vs yesterday:** ").append(yesterday.size())
                .append(" → ").append(today.size())
                .append(" (").append(delta > 0 ? "+" : "").append(delta).append(")\n");
        }
        sb.append("\n");

        // ── Top 5 failures (ranked) ──
        sb.append("## Top Failures (Ranked)\n\n");
        // Rank order: REAL_BUG > LOCATOR_CHANGE > FLAKY_TEST > ENVIRONMENT_ISSUE > OTHER,
        // then by confidence descending within each.
        List<BugEntry> ranked = today.stream()
            .sorted(Comparator
                .comparingInt(DailyFailureAnalyzer::classificationPriority).reversed()
                .thenComparing((BugEntry b) -> b.confidence, Comparator.reverseOrder()))
            .limit(5)
            .collect(Collectors.toList());

        int rank = 1;
        for (BugEntry e : ranked) {
            sb.append("### ").append(rank++).append(". ")
                .append("[").append(e.classification).append(" · ").append(e.severity).append("] ")
                .append(shortName(e.testName)).append("\n\n");
            sb.append("- **Confidence:** ").append(e.confidence).append("%\n");
            sb.append("- **Page URL:** ").append(e.pageUrl == null || e.pageUrl.isBlank() ? "_n/a_" : e.pageUrl).append("\n");
            sb.append("- **Exception:** `").append(e.exceptionType).append("`\n");
            sb.append("- **Message:** ").append(truncate(e.exceptionMessage, 250)).append("\n");
            sb.append("- **Root cause (rules-based):** ").append(e.rootCause).append("\n");
            if (e.aiEnhanced) {
                sb.append("- **AI-enhanced:** yes (root cause / fix sections include Claude analysis)\n");
            }
            sb.append("- **Suggested fix:** ").append(e.suggestedFix).append("\n\n");
        }

        // ── AI executive summary (Claude) ──
        if (ClaudeClient.isConfigured()) {
            sb.append("## AI Executive Summary\n\n");
            String aiSummary = askClaudeForSummary(today, yesterday);
            sb.append(aiSummary == null || aiSummary.isBlank()
                ? "_(Claude call returned no content)_\n"
                : aiSummary).append("\n\n");
        } else {
            sb.append("## AI Executive Summary\n\n");
            sb.append("_(skipped — `CLAUDE_API_KEY` not set; rules-based summary above is the full output)_\n\n");
        }

        // ── Action items ──
        sb.append("## Action Items for Next CI Run\n\n");
        sb.append(buildActionItems(today, yesterday)).append("\n");

        // ── Ready-bug files reference ──
        if (!readyBugFiles.isEmpty()) {
            sb.append("## Ready-Bug Files Generated (last 24h)\n\n");
            sb.append("Each of these is a copy-paste-ready markdown file for Jira:\n\n");
            for (String f : readyBugFiles) {
                sb.append("- [`").append(f).append("`](../").append(READY_BUG_DIR).append("/").append(f).append(")\n");
            }
            sb.append("\n");
        }

        // ── Footer ──
        sb.append("---\n\n");
        sb.append("_Auto-generated by `DailyFailureAnalyzer`. Per project rule, this file does NOT trigger any "
            + "Jira / Slack / email actions — it's a static markdown artifact for human review. "
            + "Inputs: `test-output/bug-detection-report.json` + `ready-bug/` folder._\n");
        return sb.toString();
    }

    private static int classificationPriority(BugEntry e) {
        switch (e.classification) {
            case "REAL_BUG": return 4;
            case "LOCATOR_CHANGE": return 3;
            case "FLAKY_TEST": return 2;
            case "ENVIRONMENT_ISSUE": return 1;
            default: return 0;
        }
    }

    /** Build deterministic action items from the report (no AI required). */
    private static String buildActionItems(List<BugEntry> today, List<BugEntry> yesterday) {
        StringBuilder sb = new StringBuilder();

        long realBugs = today.stream().filter(b -> "REAL_BUG".equals(b.classification)).count();
        long flakes = today.stream().filter(b -> "FLAKY_TEST".equals(b.classification)).count();
        long locatorChanges = today.stream().filter(b -> "LOCATOR_CHANGE".equals(b.classification)).count();

        if (realBugs > 0) {
            sb.append("1. Triage the **").append(realBugs).append(" real-bug failure(s)** — "
                + "highest leverage (each represents a product regression). Review the corresponding "
                + "`ready-bug/*.md` files and decide which to file in Jira.\n");
        }
        if (locatorChanges > 0) {
            sb.append("2. Fix the **").append(locatorChanges).append(" locator-change failure(s)** — "
                + "test-side, fast to fix (selectors went stale after a UI refactor).\n");
        }
        if (flakes > 0) {
            sb.append("3. Investigate the **").append(flakes).append(" flaky test(s)** — "
                + "review for stale-element / timing patterns; consider adding `WebDriverWait` or "
                + "scoping locators tighter.\n");
        }
        if (today.isEmpty()) {
            sb.append("- Suite is green. Consider running smoke + critical-path on production-mirror to confirm.\n");
        }
        if (!yesterday.isEmpty() && today.size() < yesterday.size()) {
            sb.append("- ✅ Failure count is decreasing (").append(yesterday.size())
                .append(" → ").append(today.size()).append("). Keep momentum.\n");
        }
        if (!yesterday.isEmpty() && today.size() > yesterday.size()) {
            sb.append("- ⚠️ Failure count is increasing (").append(yesterday.size())
                .append(" → ").append(today.size()).append("). Investigate the new failures first.\n");
        }
        return sb.toString();
    }

    /** Ask Claude for an exec summary. Returns null on failure — caller falls back. */
    private static String askClaudeForSummary(List<BugEntry> today, List<BugEntry> yesterday) {
        try {
            String systemPrompt =
                "You are a Principal SDET writing the executive summary for a daily test "
                + "failure report on a multi-tenant SaaS web app (React + Java backend). "
                + "Be concise: 4-6 sentences total. Distinguish PRODUCT bugs from TEST-SIDE "
                + "issues. End with one sentence on which failure to triage first. "
                + "Plain prose, no Markdown headers — those are added by the caller.";
            StringBuilder user = new StringBuilder();
            user.append("Today's failures (").append(today.size()).append("):\n");
            for (BugEntry e : today) {
                user.append("- ").append(shortName(e.testName))
                    .append(" [").append(e.classification).append(" ").append(e.confidence).append("%]: ")
                    .append(redact(truncate(e.exceptionMessage, 120))).append("\n");
            }
            if (!yesterday.isEmpty()) {
                user.append("\nYesterday's failures (").append(yesterday.size()).append("):\n");
                for (BugEntry e : yesterday) {
                    user.append("- ").append(shortName(e.testName))
                        .append(" [").append(e.classification).append("]\n");
                }
            }
            user.append("\nWrite the executive summary now.");
            return ClaudeClient.ask(systemPrompt, user.toString());
        } catch (Exception e) {
            System.out.println("[DailyAnalyzer] Claude call failed: " + e.getMessage());
            return null;
        }
    }

    private static String shortName(String fullName) {
        if (fullName == null) return "(unknown test)";
        int dot = fullName.lastIndexOf('.');
        if (dot < 0) return fullName;
        // Strip the package, keep ClassName.methodName
        String afterPackage = fullName.substring(dot + 1);
        // Find the previous dot to grab class name too
        int classDot = fullName.lastIndexOf('.', dot - 1);
        return classDot < 0 ? afterPackage : fullName.substring(classDot + 1);
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

    /** Internal POJO for parsed bug entries. */
    static final class BugEntry {
        String testName;
        String classification;
        int confidence;
        String severity;
        String exceptionType;
        String exceptionMessage;
        String rootCause;
        String suggestedFix;
        String pageUrl;
        String timestamp;
        boolean aiEnhanced;
    }
}
