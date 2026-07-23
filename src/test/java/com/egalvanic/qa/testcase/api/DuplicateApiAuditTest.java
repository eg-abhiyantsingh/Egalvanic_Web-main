package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * <b>Spec-level duplicate / near-duplicate API detection.</b>
 *
 * <p>Complement to the browser-driven {@code ApiDuplicateCallTestNG} (Suite 2's {@code api} toggle),
 * which catches RUNTIME duplicate calls (the same endpoint refetched 3–4× on one page load). This
 * class instead audits the API SURFACE itself — {@code /api/swagger.json} fetched at run time — for
 * duplicated endpoint definitions, which confuse clients, split traffic across two implementations,
 * and rot independently:</p>
 * <ul>
 *   <li><b>Dash/underscore twins</b> — the same path published in both spellings. Verified live
 *       2026-07-09: BOTH {@code /planned-workorder-line/*} and {@code /planned_workorder_line/*}
 *       families exist (create/update/delete/hard-delete/{id} on each side).</li>
 *   <li><b>Trailing-slash twins</b> — {@code /thing} and {@code /thing/} both defined.</li>
 *   <li><b>Singular/plural root families</b> — e.g. {@code /contact/…} AND {@code /contacts/…},
 *       {@code /user} AND {@code /users}. Reported informationally (sometimes intentional
 *       list-vs-item split, often drift).</li>
 *   <li><b>v1/v2 overlaps</b> — the same resource published with and without a {@code /v2} prefix
 *       (migration debt tracker).</li>
 * </ul>
 *
 * <p>Report-mode (keeps the green-by-default Suite 3 monitor green): findings go to
 * {@code reports/api-duplicate-endpoints-report.md}; {@code -DSTRICT_SPEC_HYGIENE=true} makes
 * exact twins (dash/underscore + trailing-slash) hard-fail. Read-only: the audit is pure spec
 * analysis, no endpoint is called beyond fetching swagger.json.</p>
 *
 * <p><b>FIX-CHECK tripwires (added 2026-07-23, HARD-FAIL, exempt from report-mode):</b> the dev
 * team reported the critical planned-workorder-line twin fixed; live re-verification proved it is
 * NOT — {@code GET /planned_workorder_line/?page=1&per_page=5} still ignores pagination and times
 * out (40s+, same as 17 Jul), BOTH spellings are still registered (6 dash + 12 underscore paths),
 * and the paginated dash list ({@code /planned-workorder-lines/}) that worked on 17 Jul has been
 * REMOVED from the spec, leaving the broken unbounded read as the only list. The two
 * {@code testFixCheck*} tests below encode the fix contract and stay RED until it is genuinely
 * met. They SKIP (not fail) when a control endpoint shows the QA backend is in one of its ambient
 * 502/504 degradation episodes, so they never produce false reds.</p>
 */
public class DuplicateApiAuditTest extends BaseAPITest {

    private static final String MODULE = "API — Duplicate Endpoint Audit (spec-driven)";
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_SPEC_HYGIENE",
                    System.getenv().getOrDefault("STRICT_SPEC_HYGIENE", "false")));

    private static final RestAssuredConfig PROBE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 30000));

    /** rawPath → sorted methods, from the live spec. */
    private static Map<String, TreeSet<String>> SPEC;
    private static final List<String[]> FINDINGS = Collections.synchronizedList(new ArrayList<>());

    @BeforeClass(alwaysRun = true)
    public void fetchSpec() {
        RestAssured.baseURI = API_BASE_URL;
        Response spec = RestAssured.given().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/swagger.json").then().extract().response();
        if (spec.statusCode() != 200) {
            throw new SkipException("swagger.json not fetchable (HTTP " + spec.statusCode() + ").");
        }
        JSONObject paths = new JSONObject(spec.asString()).getJSONObject("paths");
        SPEC = new TreeMap<>();
        for (String raw : paths.keySet()) {
            String p = raw.startsWith("/api/") ? raw.substring(4) : raw;
            TreeSet<String> ms = new TreeSet<>();
            for (String m : paths.getJSONObject(raw).keySet()) {
                String u = m.toUpperCase(Locale.ROOT);
                if (u.matches("GET|POST|PUT|PATCH|DELETE")) ms.add(u);
            }
            if (!ms.isEmpty()) SPEC.put(p, ms);
        }
        System.out.println("[DupAudit] spec fetched: " + SPEC.size() + " paths.");
    }

    /** Collapse {param} names + case so only structure differs. */
    private static String structural(String p) {
        return p.toLowerCase(Locale.ROOT).replaceAll("\\{[^}]*}", "{}");
    }

    private void report(String kind, String severity, String a, String b, String note) {
        FINDINGS.add(new String[]{kind, severity, a, b, note});
        String line = "[" + kind + "/" + severity + "] " + a + "  <->  " + b + (note.isEmpty() ? "" : " — " + note);
        System.out.println("[DupAudit] " + line);
        if ("critical".equals(severity)) ExtentReportManager.logWarning(line);
        else ExtentReportManager.logInfo(line);
    }

    @Test(description = "No path is published in BOTH dash and underscore spellings (exact twin = split implementation)")
    public void testDashUnderscoreTwins() {
        ExtentReportManager.createTest(MODULE, "spec-hygiene", "Dash/underscore twin paths");
        Map<String, List<String>> byNorm = new TreeMap<>();
        for (String p : SPEC.keySet()) {
            byNorm.computeIfAbsent(structural(p).replace('-', '_'), k -> new ArrayList<>()).add(p);
        }
        int twins = 0;
        for (Map.Entry<String, List<String>> e : byNorm.entrySet()) {
            List<String> variants = e.getValue();
            if (variants.size() < 2) continue;
            // real twin only if the variants actually differ in dash/underscore (not just param names)
            TreeSet<String> distinct = new TreeSet<>();
            for (String v : variants) distinct.add(structural(v));
            if (distinct.size() < 2) continue;
            twins++;
            String a = variants.get(0), b = variants.get(1);
            report("dash-underscore-twin", "critical", a + " " + SPEC.get(a), b + " " + SPEC.get(b),
                    "same path in both spellings — two registrations to drift apart");
        }
        conclude("dash/underscore twins", twins, true);
    }

    @Test(description = "No path is published both with and without a trailing slash")
    public void testTrailingSlashTwins() {
        ExtentReportManager.createTest(MODULE, "spec-hygiene", "Trailing-slash twin paths");
        int twins = 0;
        for (String p : SPEC.keySet()) {
            if (p.length() > 1 && p.endsWith("/") && SPEC.containsKey(p.substring(0, p.length() - 1))) {
                twins++;
                String q = p.substring(0, p.length() - 1);
                report("trailing-slash-twin", "critical", p + " " + SPEC.get(p), q + " " + SPEC.get(q),
                        "same path with and without trailing slash");
            }
        }
        conclude("trailing-slash twins", twins, true);
    }

    @Test(description = "Singular AND plural root families for the same resource (drift indicator, informational)")
    public void testSingularPluralRootFamilies() {
        ExtentReportManager.createTest(MODULE, "spec-hygiene", "Singular/plural root families");
        TreeSet<String> roots = new TreeSet<>();
        for (String p : SPEC.keySet()) {
            String r = p.replaceAll("^/([a-zA-Z0-9_-]+).*", "$1").toLowerCase(Locale.ROOT);
            roots.add(r);
        }
        int fams = 0;
        for (String r : roots) {
            String plural = r.endsWith("y") ? r.substring(0, r.length() - 1) + "ies" : r + "s";
            if (roots.contains(plural)) {
                fams++;
                long nA = SPEC.keySet().stream().filter(p -> p.toLowerCase(Locale.ROOT).startsWith("/" + r + "/") || p.equalsIgnoreCase("/" + r)).count();
                long nB = SPEC.keySet().stream().filter(p -> p.toLowerCase(Locale.ROOT).startsWith("/" + plural + "/") || p.equalsIgnoreCase("/" + plural)).count();
                report("singular-plural-family", "info", "/" + r + " (" + nA + " paths)", "/" + plural + " (" + nB + " paths)",
                        "both roots exist — verify intentional (item-vs-list) vs drift");
            }
        }
        conclude("singular/plural families", fams, false);
    }

    @Test(description = "Same resource published with and without a /v2 prefix (migration-debt tracker, informational)")
    public void testV2Overlaps() {
        ExtentReportManager.createTest(MODULE, "spec-hygiene", "v2 overlap families");
        int overlaps = 0;
        for (String p : SPEC.keySet()) {
            if (!p.startsWith("/v2/")) continue;
            String unversioned = p.substring(3);
            if (SPEC.containsKey(unversioned)) {
                overlaps++;
                report("v1-v2-overlap", "info", p + " " + SPEC.get(p), unversioned + " " + SPEC.get(unversioned),
                        "same path with and without /v2 — track the migration");
            }
        }
        conclude("v1/v2 overlaps", overlaps, false);
    }

    // ── FIX-CHECK tripwires (2026-07-23) — hard-fail until the planned-workorder-line twin is fixed ──

    /** 35s socket budget: long enough to measure the defect (hangs 40s+), far above the 10s pass bar. */
    private static final RestAssuredConfig TRIPWIRE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 10000)
                    .setParam("http.socket.timeout", 35000));

    /**
     * Ambient-degradation guard: the QA host periodically 502/504s EVERYTHING (verified again
     * 2026-07-23: even action-items/counts returned 502 after 41s). A tripwire that reds on those
     * episodes would be noise — so skip unless an unrelated control endpoint is healthy.
     */
    private void requireHealthyBackend() {
        if (!hasAuthToken()) throw new SkipException("No auth token — cannot probe authenticated endpoints.");
        try {
            Response r = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                    .when().get("/action-items/counts").then().extract().response();
            if (r.statusCode() >= 500 || r.getTime() > 10000) {
                throw new SkipException("QA backend in ambient degradation (control /action-items/counts → HTTP "
                        + r.statusCode() + " in " + r.getTime() + "ms) — tripwire probe would be meaningless. Re-run later.");
            }
        } catch (SkipException se) { throw se; }
        catch (Exception e) {
            throw new SkipException("QA backend unreachable (control probe: " + e.getMessage() + ") — re-run later.");
        }
    }

    @Test(description = "API testing - FIX CHECK: planned_workorder_line list honours pagination and answers <10s (RED until fixed)")
    public void testFixCheckUnderscoreListPaginates() {
        ExtentReportManager.createTest(MODULE, "fix-check", "API testing - FIX CHECK: planned_workorder_line list paginates <10s");
        requireHealthyBackend();
        String path = "/planned_workorder_line/?page=1&per_page=5";
        long t0 = System.currentTimeMillis();
        try {
            Response r = getAuthenticatedRequestSpec().config(TRIPWIRE_CONFIG).relaxedHTTPSValidation()
                    .when().get(path).then().extract().response();
            long ms = System.currentTimeMillis() - t0;
            String msg = "GET " + path + " → HTTP " + r.statusCode() + " in " + ms + "ms ("
                    + r.asString().length() + " bytes)";
            System.out.println("[DupAudit/FixCheck] " + msg);
            if (r.statusCode() != 200 || ms > 10000) {
                ExtentReportManager.logFail(msg);
                Assert.fail("NOT FIXED — " + msg + ". Contract: the list read must honour page/per_page and answer"
                        + " <10s. First flagged 2026-07-09, dev-reported fixed, re-verified broken 2026-07-23"
                        + " (hangs 40s+, unbounded read). Fix: paginate the read and collapse the dash/underscore"
                        + " twin onto ONE canonical route.");
            }
            ExtentReportManager.logPass("FIXED: " + msg);
        } catch (AssertionError ae) { throw ae; }
        catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            String msg = "GET " + path + " gave NO response in " + ms + "ms (socket timeout — unbounded/unpaginated read)";
            ExtentReportManager.logFail(msg);
            Assert.fail("NOT FIXED — " + msg + ". Same signature as 2026-07-09 and 2026-07-17; re-verified live"
                    + " 2026-07-23 (45s browser probe aborted with no response while bounded reads on the same"
                    + " resource answered). The endpoint ignores page/per_page and scans unbounded.");
        }
    }

    @Test(description = "API testing - FIX CHECK: planned-workorder-line exists in ONE spelling only (RED until twin retired)")
    public void testFixCheckSingleSpelling() {
        ExtentReportManager.createTest(MODULE, "fix-check", "API testing - FIX CHECK: planned-workorder-line single spelling");
        List<String> dash = new ArrayList<>(), under = new ArrayList<>();
        for (Map.Entry<String, TreeSet<String>> e : SPEC.entrySet()) {
            if (e.getKey().contains("planned-workorder-line")) dash.add(e.getKey() + " " + e.getValue());
            if (e.getKey().contains("planned_workorder_line")) under.add(e.getKey() + " " + e.getValue());
        }
        String detail = "dash family (" + dash.size() + "): " + dash + " | underscore family (" + under.size() + "): " + under;
        System.out.println("[DupAudit/FixCheck] " + detail);
        if (!dash.isEmpty() && !under.isEmpty()) {
            ExtentReportManager.logFail(detail);
            Assert.fail("NOT FIXED — the resource is still registered under BOTH spellings (" + dash.size()
                    + " dash + " + under.size() + " underscore paths; 17 Jul it was 6+13, so the twin persists and"
                    + " the families have drifted: writes live on both sides, the paginated dash LIST was removed)."
                    + " Contract: ONE canonical route family. " + detail);
        }
        ExtentReportManager.logPass("FIXED: only one spelling remains — " + detail);
    }

    private void conclude(String what, int n, boolean gated) {
        if (n == 0) { ExtentReportManager.logPass("No " + what + " in the spec."); return; }
        String msg = n + " " + what + " found in the live spec (see api-duplicate-endpoints-report.md).";
        if (gated && STRICT) { ExtentReportManager.logFail(msg); Assert.fail(msg); }
        ExtentReportManager.logWarning(msg + (gated ? "  (reported; -DSTRICT_SPEC_HYGIENE=true enforces)" : "  (informational)"));
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        StringBuilder md = new StringBuilder();
        md.append("# API Duplicate-Endpoint Audit (spec-driven)\n\n");
        md.append("The live `/api/swagger.json` surface, audited for duplicated endpoint definitions: ")
          .append("dash/underscore twins, trailing-slash twins, singular/plural root families, v1/v2 overlaps. ")
          .append("Exact twins split traffic across two registrations that drift independently.\n\n");
        md.append("**FIX-CHECK status (tripwires above in this class):** the critical ")
          .append("`planned_workorder_line` twin was dev-reported fixed and re-verified NOT fixed on ")
          .append("2026-07-23 — the underscore list still ignores pagination (40s+ timeout), both spellings ")
          .append("remain registered, and the paginated dash list was removed from the spec. Full evidence: ")
          .append("`docs/bug-repro/duplicate-api-endpoints/`.\n\n");
        md.append("Runtime duplicate CALLS (same endpoint refetched on one page load) are covered separately by ")
          .append("the browser-driven `ApiDuplicateCallTestNG` (Suite 2 `api` toggle) — latest findings: ")
          .append("21 redundant logical endpoints (3–4x per load) + 68 exact-URL duplicates.\n\n");
        md.append("| Kind | Severity | Variant A | Variant B | Note |\n|---|---|---|---|---|\n");
        Map<String, Integer> byKind = new LinkedHashMap<>();
        for (String[] f : FINDINGS) {
            md.append("| ").append(String.join(" | ", f)).append(" |\n");
            byKind.merge(f[0], 1, Integer::sum);
        }
        md.append("\n**").append(SPEC == null ? 0 : SPEC.size()).append(" spec paths audited — ")
          .append(FINDINGS.size()).append(" finding(s): ").append(byKind).append(".**\n");
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/api-duplicate-endpoints-report.md")) { w.write(md.toString()); }
            System.out.println("[DupAudit] Report → reports/api-duplicate-endpoints-report.md");
        } catch (Exception e) { System.out.println("[DupAudit] report write failed: " + e.getMessage()); }
        System.out.println("\n===== DUPLICATE-ENDPOINT AUDIT =====\n" + md);
    }
}
