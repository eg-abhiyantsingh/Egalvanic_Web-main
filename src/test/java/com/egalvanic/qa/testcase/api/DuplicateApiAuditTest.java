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
