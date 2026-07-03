package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * DUPLICATE / REDUNDANT API-CALL detector.
 *
 * Drives each module in the browser and, using the Resource Timing API, records EVERY
 * {@code /api} request the SPA fires during that single page load (NOT deduplicated —
 * unlike {@link NetworkApiContractTestNG}, which collects a Set). It then:
 *   - normalises each URL to a logical endpoint (strip query string; replace UUID / numeric /
 *     long-hex path segments with {id}) so /graph/nodes/A and /graph/nodes/B collapse, and
 *   - counts how many times each logical endpoint — and each EXACT url (incl. query) — fired
 *     within that one load window.
 *
 * A logical endpoint firing 2× on load is often legitimate (list + count). ≥3× is redundant
 * (a React double-render / effect refetch / component-level refetch), and the SAME exact URL
 * (query included) firing ≥2× in one load is almost always a wasteful duplicate.
 *
 * REPORT MODE by default (findings are logged + written to
 * {@code reports/api-duplicate-calls-report.md}); pass {@code -DSTRICT_DUPLICATE_API=true} to
 * make egregious duplicates fail the build (mirrors STRICT_LIST_API_CONTRACT / STRICT_HEALTH_GATES).
 */
public class ApiDuplicateCallTestNG extends BaseTest {

    private static final String MODULE = "API — Duplicate Calls";

    // thresholds
    private static final int LOGICAL_REDUNDANT = 3;   // same logical endpoint ≥3× on one load → redundant
    private static final int LOGICAL_CRITICAL  = 5;   // ≥5× → almost certainly a refetch bug
    private static final int EXACT_DUP         = 2;    // same exact URL (incl query) ≥2× → wasteful duplicate
    private static final int SETTLE_MS         = 6000; // let the SPA finish its on-load bursts

    private static final boolean STRICT = "true".equalsIgnoreCase(System.getProperty("STRICT_DUPLICATE_API", "false"));

    /** Module label → route. */
    private static final String[][] PAGES = {
        {"dashboard","/dashboard"}, {"assets","/assets"}, {"issues","/issues"},
        {"work-orders","/sessions"}, {"connections","/connections"}, {"locations","/locations"},
        {"tasks","/tasks"}, {"opportunities","/opportunities"}, {"arc-flash","/arc-flash"},
        {"accounts","/accounts"}, {"slds","/slds"},
    };

    /** Telemetry / 3rd-party / polling noise — excluded (legit repeats, not our APIs). */
    private static final String[] NOISE = {
        "sentry", "beamer", "devrev", "/web/api/", "envelope", "startSession", "addMetadata",
        "setIdentifier", "user-hash", "session-token", "legal/acceptance", "/auth/login",
        "getbeamer", "formbricks", "amplitude", "segment", "datadog", "/health"
    };

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    /** Strip host + query, collapse id-like segments to {id} → a logical endpoint key. */
    private static String normalize(String url) {
        String p = url.replaceFirst("^https?://[^/]+", "");
        int q = p.indexOf('?');
        if (q >= 0) p = p.substring(0, q);
        String[] seg = p.split("/");
        StringBuilder sb = new StringBuilder();
        for (String s : seg) {
            if (s.isEmpty()) continue;
            boolean idLike =
                s.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") // uuid
             || s.matches("\\d+")                                   // pure number
             || s.matches("[0-9a-fA-F]{16,}")                       // long hex / oid
             || s.matches("[A-Za-z0-9_-]{24,}");                    // long opaque id / base64-ish
            sb.append('/').append(idLike ? "{id}" : s);
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    private static boolean isNoise(String pathOrUrl) {
        for (String n : NOISE) if (pathOrUrl.contains(n)) return true;
        return false;
    }

    @Test(description = "Detect duplicate / redundant /api calls fired by the SPA on each module load")
    public void detectDuplicateApiCalls() {
        ExtentReportManager.createTest(MODULE, "Duplicate calls", "DUP_DetectAcrossModules");

        // module → (logical endpoint → count) and module → (exact url → count)
        List<String[]> logicalDups = new ArrayList<>();   // {module, endpoint, count, severity}
        List<String[]> exactDups = new ArrayList<>();      // {module, url, count}
        int modulesObserved = 0, totalApiCalls = 0;

        for (String[] pg : PAGES) {
            String mod = pg[0];
            try {
                js().executeScript("performance.clearResourceTimings();");
                driver.get(AppConstants.BASE_URL + pg[1]);
                pause(SETTLE_MS);
                @SuppressWarnings("unchecked")
                List<String> urls = (List<String>) js().executeScript(
                    "return performance.getEntriesByType('resource').map(function(e){return e.name;})"
                  + ".filter(function(u){return u.indexOf('/api/')>=0;});");
                if (urls == null) urls = new ArrayList<>();

                Map<String, Integer> logical = new TreeMap<>();
                Map<String, Integer> exact = new LinkedHashMap<>();
                int calls = 0;
                for (String u : urls) {
                    String path = u.replaceFirst("^https?://[^/]+", "");
                    if (!path.startsWith("/api/") || isNoise(path)) continue;
                    calls++;
                    logical.merge(normalize(path), 1, Integer::sum);
                    exact.merge(path, 1, Integer::sum);
                }
                if (calls == 0) { System.out.println("[Dup] " + mod + ": no /api calls observed"); continue; }
                modulesObserved++; totalApiCalls += calls;

                for (Map.Entry<String, Integer> e : logical.entrySet()) {
                    if (e.getValue() >= LOGICAL_REDUNDANT) {
                        String sev = e.getValue() >= LOGICAL_CRITICAL ? "critical" : "warning";
                        logicalDups.add(new String[]{mod, e.getKey(), String.valueOf(e.getValue()), sev});
                    }
                }
                for (Map.Entry<String, Integer> e : exact.entrySet()) {
                    if (e.getValue() >= EXACT_DUP) exactDups.add(new String[]{mod, e.getKey(), String.valueOf(e.getValue())});
                }
                System.out.println("[Dup] " + mod + ": " + calls + " /api calls, "
                        + logical.size() + " distinct endpoints; redundant≥" + LOGICAL_REDUNDANT + "="
                        + logical.values().stream().filter(v -> v >= LOGICAL_REDUNDANT).count());
            } catch (Exception ex) {
                System.out.println("[Dup] capture " + mod + " failed: " + ex.getMessage());
            }
        }

        logStep("Observed " + totalApiCalls + " /api calls across " + modulesObserved + " modules. "
                + "Redundant logical endpoints: " + logicalDups.size() + " | exact-URL duplicates: " + exactDups.size());
        Assert.assertTrue(modulesObserved >= 3, "Capture should observe API traffic on at least 3 modules (got " + modulesObserved + ").");

        writeReport(modulesObserved, totalApiCalls, logicalDups, exactDups);

        long criticals = logicalDups.stream().filter(d -> "critical".equals(d[3])).count();
        if (!logicalDups.isEmpty() || !exactDups.isEmpty()) {
            ExtentReportManager.logWarning("Duplicate/redundant API calls found — logical≥" + LOGICAL_REDUNDANT + ": "
                    + logicalDups.size() + " (critical " + criticals + "), exact-URL≥" + EXACT_DUP + ": " + exactDups.size()
                    + ". See reports/api-duplicate-calls-report.md");
        } else {
            ExtentReportManager.logPass("No redundant/duplicate API calls detected across " + modulesObserved + " modules.");
        }

        if (STRICT) {
            Assert.assertEquals(criticals, 0L,
                    "STRICT: an endpoint is fired ≥" + LOGICAL_CRITICAL + "× on a single load (redundant refetch): "
                    + logicalDups.stream().filter(d -> "critical".equals(d[3])).map(d -> d[0] + " " + d[1] + " ×" + d[2]).reduce((a, b) -> a + "; " + b).orElse(""));
        }
    }

    private void writeReport(int modules, int totalCalls, List<String[]> logicalDups, List<String[]> exactDups) {
        long crit = logicalDups.stream().filter(d -> "critical".equals(d[3])).count();
        StringBuilder md = new StringBuilder();
        md.append("# API Duplicate / Redundant Call Report — ").append(AppConstants.BASE_URL).append("\n\n");
        md.append("**").append(modules).append(" modules** · ").append(totalCalls).append(" /api calls observed · ")
          .append(logicalDups.size()).append(" redundant logical endpoint(s) (≥").append(LOGICAL_REDUNDANT).append("× on one load, ")
          .append(crit).append(" critical ≥").append(LOGICAL_CRITICAL).append("×) · ")
          .append(exactDups.size()).append(" exact-URL duplicate(s) (≥").append(EXACT_DUP).append("×)\n\n");
        md.append("A logical endpoint fired ≥3× or the *same exact URL* fired ≥2× on a single page load usually ")
          .append("means a redundant refetch (React double-render / duplicated effect / component re-fetch).\n\n");

        md.append("## Redundant logical endpoints (id-normalised)\n\n");
        if (logicalDups.isEmpty()) md.append("_None._\n\n");
        else {
            md.append("| Module | Endpoint (normalised) | Times on load | Severity |\n|---|---|---|---|\n");
            logicalDups.sort((a, b) -> b[3].compareTo(a[3]));   // critical first
            for (String[] d : logicalDups) md.append("| ").append(d[0]).append(" | ").append(d[1]).append(" | ")
                    .append(d[2]).append("× | ").append(d[3]).append(" |\n");
            md.append("\n");
        }

        md.append("## Exact-URL duplicates (query string included)\n\n");
        if (exactDups.isEmpty()) md.append("_None._\n\n");
        else {
            md.append("| Module | Exact URL | Times |\n|---|---|---|\n");
            for (String[] d : exactDups) {
                String url = d[1].length() > 120 ? d[1].substring(0, 117) + "…" : d[1];
                md.append("| ").append(d[0]).append(" | `").append(url).append("` | ").append(d[2]).append("× |\n");
            }
            md.append("\n");
        }

        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/api-duplicate-calls-report.md")) { w.write(md.toString()); }
            System.out.println("[Dup] wrote reports/api-duplicate-calls-report.md");
        } catch (Exception e) { System.out.println("[Dup] report write failed: " + e.getMessage()); }
        System.out.println("\n===== API DUPLICATE CALLS =====\n" + md);
    }
}
