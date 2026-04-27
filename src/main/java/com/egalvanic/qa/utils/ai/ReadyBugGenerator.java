package com.egalvanic.qa.utils.ai;

import com.egalvanic.qa.utils.ai.SmartBugDetector.BugReport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Ready-Bug Generator.
 *
 * When a test fails and SmartBugDetector classifies it (Real Bug / Test-Side Flake /
 * Env Issue), this writer produces a COPY-PASTE-READY bug report file under
 * <code>ready-bug/</code>. The user copies the contents into Jira (or any tracker)
 * directly — we never upload anything to Jira automatically (per memory rule
 * <code>feedback_never_modify_jira.md</code>).
 *
 * File format (Markdown, 1 file per failure):
 * <pre>
 * ready-bug/
 *   2026-04-27-19-45-AssetTestNG-testTC_Asset_07_EditAmpereRating.md
 *   2026-04-27-19-46-IssueTestNG-testTC_Issue_03_BlankIssueClass.md
 *   ...
 * </pre>
 *
 * Each file contains:
 *   - Title (test name + classification)
 *   - Severity (mapped from BugReport.riskLevel)
 *   - Steps to Reproduce (extracted from logs / page URL)
 *   - Actual Result (exception message + console errors)
 *   - Expected Result (inferred from assertion message)
 *   - Screenshot Path (saved next to the .md file)
 *   - AI Root Cause (if SmartBugDetector + Claude wrote one)
 *   - Suggested Fix (if SmartBugDetector + Claude wrote one)
 *
 * Why this rather than direct Jira upload:
 *   - The user explicitly forbade Jira modifications without per-action permission.
 *   - A markdown file is reviewable BEFORE submission — a human reads and corrects
 *     anything wrong before pasting into Jira.
 *   - Files persist in source control with full git history, so we can diff how
 *     bug reports evolved.
 *
 * Thread-safe (each test failure produces its own file).
 * Idempotent (re-running the same failure overwrites the file with the latest data).
 */
public final class ReadyBugGenerator {

    private static final String READY_BUG_DIR = "ready-bug";
    private static final DateTimeFormatter FILENAME_TS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");

    private ReadyBugGenerator() {}

    /**
     * Generate a copy-paste-ready bug file from a SmartBugDetector report.
     * Returns the path of the created file, or empty if generation failed.
     */
    public static Optional<Path> generate(BugReport report, String screenshotPath) {
        if (report == null || report.testName == null) return Optional.empty();
        try {
            File dir = ensureDir();
            String filename = filenameFor(report);
            File file = new File(dir, filename);
            String body = buildMarkdown(report, screenshotPath);
            try (FileWriter w = new FileWriter(file)) {
                w.write(body);
            }
            // Copy screenshot next to the file for self-contained portability
            if (screenshotPath != null && !screenshotPath.isBlank()) {
                Path src = Paths.get(screenshotPath);
                if (Files.exists(src)) {
                    Path dst = Paths.get(dir.getPath(),
                        filename.replace(".md", "") + "-screenshot" + extOf(src));
                    try {
                        Files.copy(src, dst,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException copyErr) {
                        // Screenshot copy is nice-to-have; don't fail the report
                        System.out.println("[ReadyBug] Could not copy screenshot: "
                            + copyErr.getMessage());
                    }
                }
            }
            System.out.println("[ReadyBug] Wrote " + file.getPath());
            return Optional.of(file.toPath());
        } catch (Exception e) {
            System.out.println("[ReadyBug] Failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static File ensureDir() throws IOException {
        File dir = new File(READY_BUG_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create ready-bug directory");
        }
        return dir;
    }

    private static String filenameFor(BugReport r) {
        String safeName = r.testName.replaceAll("[^A-Za-z0-9._-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        if (safeName.length() > 120) safeName = safeName.substring(0, 120);
        return LocalDateTime.now().format(FILENAME_TS) + "-" + safeName + ".md";
    }

    private static String extOf(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".png";
    }

    /**
     * Build the markdown body. Format is intentionally close to Jira-friendly:
     * Title / Severity / Steps / Actual / Expected / Screenshot / Notes.
     * The user can copy-paste this directly into a Jira ticket's description.
     */
    private static String buildMarkdown(BugReport r, String screenshotPath) {
        StringBuilder sb = new StringBuilder();

        // Title
        sb.append("# ").append(deriveTitle(r)).append("\n\n");

        // Quick metadata block
        sb.append("**Test:** `").append(r.testName).append("`\n");
        sb.append("**Date:** ").append(r.timestamp == null
            ? LocalDateTime.now().toString()
            : r.timestamp).append("\n");
        sb.append("**Classification:** ").append(r.classification == null
            ? "UNKNOWN"
            : r.classification.name())
            .append(" (").append(r.confidence).append("% confidence)\n");
        sb.append("**Severity:** ").append(severityFor(r)).append("\n");
        sb.append("**Page URL:** ").append(safe(r.pageUrl)).append("\n");
        sb.append("**Page Title:** ").append(safe(r.pageTitle)).append("\n");
        sb.append("**Test Duration:** ").append(r.testDurationMs).append(" ms\n\n");

        // Steps to Reproduce — best-effort from page URL + test name
        sb.append("## Steps to Reproduce\n\n");
        sb.append(buildSteps(r)).append("\n\n");

        // Actual Result
        sb.append("## Actual Result\n\n");
        sb.append("```\n");
        sb.append(safe(r.exceptionType)).append(": ").append(safe(r.exceptionMessage)).append("\n");
        sb.append("```\n\n");

        // Expected Result — derived from assertion message, when present
        sb.append("## Expected Result\n\n");
        sb.append(deriveExpected(r)).append("\n\n");

        // Screenshot reference
        sb.append("## Screenshot\n\n");
        if (screenshotPath != null && !screenshotPath.isBlank()) {
            String fileName = new File(screenshotPath).getName();
            sb.append("Attached: `").append(fileName).append("`")
                .append(" (also copied next to this report as ")
                .append("`<this-file>-screenshot").append(extOf(Paths.get(screenshotPath)))
                .append("`)\n\n");
        } else {
            sb.append("_(no screenshot captured at failure time)_\n\n");
        }

        // Console errors snippet (so dev sees the noise)
        if (r.consoleErrors != null && !r.consoleErrors.isBlank()) {
            sb.append("## Console Errors at Failure Time\n\n");
            sb.append("```\n");
            sb.append(truncate(r.consoleErrors, 1500));
            sb.append("\n```\n\n");
        }

        // AI-driven root cause (if Claude was called)
        if (r.aiEnhanced || (r.rootCause != null && !r.rootCause.isBlank())) {
            sb.append("## Likely Root Cause\n\n");
            sb.append(safe(r.rootCause)).append("\n\n");
        }
        if (r.suggestedFix != null && !r.suggestedFix.isBlank()) {
            sb.append("## Suggested Fix\n\n");
            sb.append(r.suggestedFix).append("\n\n");
        }

        // Final hint to the human reviewer
        sb.append("---\n\n");
        sb.append("_Auto-generated by `ReadyBugGenerator`. Review the above, edit if "
            + "needed, then copy-paste into your Jira ticket. **Do not** push this "
            + "file's contents directly to Jira via tooling — per project rule, "
            + "Jira modifications need explicit per-ticket approval._\n");
        return sb.toString();
    }

    private static String deriveTitle(BugReport r) {
        String severity = severityFor(r);
        String classification = r.classification == null
            ? "Failure"
            : friendly(r.classification.name());
        // e.g. "[Bug | Medium] AssetTestNG.testTC_Asset_07_EditAmpereRating"
        return "[" + classification + " | " + severity + "] " + r.testName;
    }

    private static String severityFor(BugReport r) {
        if (r.riskLevel != null && !r.riskLevel.isBlank()) return r.riskLevel;
        if (r.classification == null) return "MEDIUM";
        switch (r.classification) {
            case REAL_BUG: return r.confidence >= 70 ? "HIGH" : "MEDIUM";
            case FLAKY_TEST: return "LOW";
            case ENVIRONMENT_ISSUE: return "LOW";
            case LOCATOR_CHANGE: return "MEDIUM";
            default: return "MEDIUM";
        }
    }

    private static String friendly(String enumName) {
        if (enumName == null) return "Unknown";
        switch (enumName) {
            case "REAL_BUG": return "Bug";
            case "FLAKY_TEST": return "Flake";
            case "ENVIRONMENT_ISSUE": return "Env";
            case "LOCATOR_CHANGE": return "Locator";
            default: return enumName.toLowerCase(Locale.ROOT);
        }
    }

    /** Best-effort step generation from URL + test name. Numbers steps continuously. */
    private static String buildSteps(BugReport r) {
        java.util.List<String> steps = new java.util.ArrayList<>();
        steps.add("Login to `" + safe(r.pageUrl).replaceAll("/[^/]*$", "")
            + "` with the standard QA credentials");
        if (r.pageUrl != null && !r.pageUrl.isBlank()) {
            steps.add("Navigate to " + safe(r.pageUrl));
        }
        String lower = r.testName == null ? "" : r.testName.toLowerCase();
        if (lower.contains("create") || lower.contains("add")) {
            steps.add("Open the Create / Add form for the relevant entity");
            steps.add("Fill required fields and submit");
        } else if (lower.contains("delete") || lower.contains("remove")) {
            steps.add("Locate the target record in the list");
            steps.add("Trigger the delete action and confirm");
        } else if (lower.contains("edit") || lower.contains("update")) {
            steps.add("Open the existing record's Edit drawer/form");
            steps.add("Modify the relevant field and save");
        } else if (lower.contains("login") || lower.contains("auth") || lower.contains("signin")) {
            steps.add("Go to the login page");
            steps.add("Enter the credentials matching the test inputs");
        } else {
            steps.add("Reproduce the test scenario manually using the test source as reference");
        }
        steps.add("Observe the result described under \"Actual Result\"");

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            out.append(i + 1).append(". ").append(steps.get(i)).append('\n');
        }
        return out.toString();
    }

    private static String deriveExpected(BugReport r) {
        // Many TestNG failures have the assertion message in exceptionMessage like:
        //   "expected [true] but found [false]: <message>"
        // We split on "expected" if present to reconstruct the intent.
        String msg = r.exceptionMessage == null ? "" : r.exceptionMessage;
        if (msg.toLowerCase(Locale.ROOT).contains("expected")) {
            // Use the full message — it already encodes expected vs actual
            return "Per the assertion: `" + msg.replaceAll("`", "'") + "`";
        }
        // Otherwise generic — the test should pass without raising the captured exception
        return "The test should complete without raising `"
            + safe(r.exceptionType) + "`. See the test's Javadoc / @Test description for the "
            + "specific business intent.";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "\n... [truncated " + (s.length() - max) + " chars]";
    }
}
