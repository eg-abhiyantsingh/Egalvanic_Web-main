package com.egalvanic.qa.utils.ai;

import com.egalvanic.qa.utils.ai.SmartBugDetector.BugReport;

/**
 * AI Bug Analyzer — Use Case 1 from the user's spec.
 *
 * After SmartBugDetector classifies a failure with rules-based heuristics, this
 * helper sends the failure to Claude (via ClaudeClient) for a deeper, narrative
 * root-cause analysis + a suggested fix. The output is written back into the
 * BugReport so ReadyBugGenerator picks it up in the final markdown.
 *
 * Why this is separate from SmartBugDetector:
 *   - SmartBugDetector is rule-based and ALWAYS runs (free, fast).
 *   - AIBugAnalyzer is Claude-based and only runs when:
 *       (a) ClaudeClient.isConfigured() returns true (API key present), AND
 *       (b) the classification confidence is ambiguous (e.g., < 80%) — saving
 *           tokens on cases the rules already nailed.
 *   - This keeps the framework usable WITHOUT a Claude API key (rules-only)
 *     while giving teams that have a key an upgrade path.
 *
 * Threat model: we never send sensitive data (passwords, tokens) to Claude.
 * The BugReport's exceptionMessage / consoleErrors fields can contain user
 * inputs — we redact obvious credential patterns before sending.
 */
public final class AIBugAnalyzer {

    private AIBugAnalyzer() {}

    /** Analyze + enrich the report in place. Returns true if Claude was called. */
    public static boolean analyze(BugReport report) {
        if (report == null) return false;
        if (!ClaudeClient.isConfigured()) {
            return false;
        }
        // Skip Claude when the rules engine is already confident — saves tokens
        if (report.confidence >= 80 && report.rootCause != null && !report.rootCause.isBlank()) {
            return false;
        }

        try {
            String systemPrompt =
                "You are a Principal SDET reviewing a single test failure on a "
                + "multi-tenant SaaS web application built with React + Java backend. "
                + "Be concise: 3-5 sentences max for the root cause, then 2-3 bullets "
                + "for the suggested fix. Distinguish PRODUCT bugs from TEST-SIDE issues "
                + "(timing, stale selectors). Output PLAIN TEXT, no Markdown headers.";

            String userPrompt = buildPrompt(report);
            String reply = ClaudeClient.ask(systemPrompt, userPrompt);
            if (reply == null || reply.isBlank()) return false;

            // Heuristic: split reply into root cause + suggested fix on the bullet boundary
            String[] split = splitRootCauseAndFix(reply);
            // Append (don't replace) — the rules-based root cause stays as evidence
            report.rootCause = mergeText(report.rootCause, split[0]);
            report.suggestedFix = mergeText(report.suggestedFix, split[1]);
            report.aiEnhanced = true;
            return true;
        } catch (Exception e) {
            System.out.println("[AIBugAnalyzer] Claude call failed: " + e.getMessage());
            return false;
        }
    }

    /** Build the prompt body — redacted of obvious credentials. */
    private static String buildPrompt(BugReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Test: ").append(r.testName).append("\n");
        sb.append("Page URL: ").append(redact(r.pageUrl)).append("\n");
        sb.append("Page Title: ").append(redact(r.pageTitle)).append("\n");
        sb.append("Exception: ").append(r.exceptionType).append("\n");
        sb.append("Message: ").append(redact(r.exceptionMessage)).append("\n");
        sb.append("Rules-based classification: ").append(r.classification)
            .append(" (").append(r.confidence).append("%)\n");
        if (r.consoleErrors != null && !r.consoleErrors.isBlank()) {
            sb.append("Console errors (truncated): ")
                .append(redact(truncate(r.consoleErrors, 800))).append("\n");
        }
        if (r.domSnippet != null && !r.domSnippet.isBlank()) {
            sb.append("Relevant DOM (truncated): ")
                .append(truncate(r.domSnippet, 800)).append("\n");
        }
        sb.append("\nTask: Identify the most likely root cause and propose a concrete fix. "
            + "Differentiate product bug vs test-side issue.\n");
        return sb.toString();
    }

    private static String[] splitRootCauseAndFix(String reply) {
        // Look for any of: "Suggested fix", "Fix:", "Recommendation:", or first bullet
        String[] markers = {"suggested fix:", "fix:", "recommendation:", "remediation:"};
        String lower = reply.toLowerCase();
        int splitAt = -1;
        for (String marker : markers) {
            int idx = lower.indexOf(marker);
            if (idx > 0 && (splitAt < 0 || idx < splitAt)) splitAt = idx;
        }
        if (splitAt < 0) {
            // Fallback: split at first bullet ("- " or "•") on its own line
            int bullet = -1;
            int nl = reply.indexOf('\n');
            while (nl >= 0 && bullet < 0) {
                if (nl + 1 < reply.length()
                        && (reply.charAt(nl + 1) == '-' || reply.charAt(nl + 1) == '•')) {
                    bullet = nl + 1;
                }
                nl = reply.indexOf('\n', nl + 1);
            }
            splitAt = bullet;
        }
        if (splitAt < 0) {
            return new String[]{reply, ""};
        }
        return new String[]{
            reply.substring(0, splitAt).trim(),
            reply.substring(splitAt).trim()
        };
    }

    private static String mergeText(String existing, String addition) {
        if (addition == null || addition.isBlank()) return existing;
        if (existing == null || existing.isBlank()) return addition;
        return existing + "\n\n— AI-enhanced —\n" + addition;
    }

    /** Redact obvious credentials. We don't send passwords or tokens to Claude. */
    private static String redact(String s) {
        if (s == null) return "";
        // Strip Bearer tokens, JWTs, password=... forms
        return s.replaceAll("(?i)bearer\\s+[A-Za-z0-9._-]+", "Bearer <REDACTED>")
            .replaceAll("eyJ[A-Za-z0-9_-]{15,}", "<JWT-REDACTED>")
            .replaceAll("(?i)password[\"':\\s]*[^\\s\",]+", "password=<REDACTED>")
            .replaceAll("(?i)token[\"':\\s]*[^\\s\",]+", "token=<REDACTED>");
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s == null ? "" : s;
        return s.substring(0, max) + "...";
    }
}
