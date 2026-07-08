package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FULL API CATALOG health check — spec-driven, covers EVERY operation in
 * {@code /api/swagger.json} (the source behind https://acme.qa.egalvanic.ai/api/docs).
 * 2026-07-03 spec: 862 paths / 973 operations (420 GET · 319 POST · 138 PUT · 84 DELETE · 12 PATCH).
 * Because the catalog is fetched at RUN TIME, new endpoints are covered automatically —
 * no hardcoded list to go stale.
 *
 * Probe policy (zero mutation risk, by construction):
 *   - GET            → AUTHENTICATED probe. Path params substituted with a random UUID
 *                      (never a real id), so parameterized GETs exercise the 404/validation
 *                      path. Records status/latency/payload/shape; flags HTML responses
 *                      (SPA-fallback bug class) and 5xx.
 *   - POST/PUT/PATCH/DELETE → UNAUTHENTICATED probe with an empty JSON body. The ONLY
 *                      assertion-worthy signal is the auth gate: 401/403 = correctly gated.
 *                      2xx without auth = CRITICAL security finding. 5xx on an unauth
 *                      probe = server mishandles bad input (the TC_HSC_04 bug class).
 *                      No token is ever attached, and ids are random UUIDs — nothing can
 *                      mutate real QA data.
 *   - Public-by-design endpoints (login/signup/forgot-password/branding/health…) are
 *                      whitelisted: they only must not 5xx.
 *
 * Failure semantics match ApiHealthCheckApiTest: a test FAILS only on a genuine outage
 * (connection error / timeout). Everything else is recorded + reported (pass/warn + recs)
 * so Suite 3 stays a report-mode monitor, with findings in reports/api-catalog-report.md
 * (rendered to the styled HTML by gen_api_health_report.py).
 */
public class ApiFullCatalogHealthApiTest extends BaseAPITest {

    private static final String MODULE = "API — Full Catalog (spec-driven)";

    private static final int SLOW_MS = 1500, VERY_SLOW_MS = 4000;

    // fetched once; reused across the data-driven run
    private static Object[][] catalogRows;
    private static int specOperations = -1;
    private static final List<String[]> RESULTS = Collections.synchronizedList(new ArrayList<>());
    private static final List<String[]> RECS = Collections.synchronizedList(new ArrayList<>());

    private static final RestAssuredConfig PROBE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 30000));

    /** Endpoints that are public by design — unauthenticated access is expected. */
    private static final Pattern PUBLIC_ENDPOINTS = Pattern.compile(
            "(?i)^/(auth/(login|logout|signup|register|forgot|reset|verify-token|sso|refresh)"
            + "|branding/|health$|mutation/health$|swagger|docs)");

    // ================================================================
    // CATALOG (fetched live from the swagger spec)
    // ================================================================

    // parallel=true: the ~973 probes are independent, read-only/auth-gate-only, and record into
    // Collections.synchronizedList (RESULTS/RECS) — safe to fan out. Suite sets data-provider-thread-count.
    @DataProvider(name = "catalog", parallel = true)
    public Object[][] catalog() {
        if (catalogRows != null) return catalogRows;
        Response spec = RestAssured.given().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/swagger.json").then().extract().response();
        if (spec.statusCode() != 200) {
            throw new SkipException("swagger.json not fetchable (HTTP " + spec.statusCode() + ") — cannot enumerate the catalog.");
        }
        JSONObject root = new JSONObject(spec.asString());
        JSONObject paths = root.getJSONObject("paths");
        List<Object[]> rows = new ArrayList<>();
        for (String rawPath : new TreeMap<>(paths.toMap()).keySet()) {
            JSONObject ops = paths.getJSONObject(rawPath);
            for (String method : ops.keySet()) {
                String m = method.toUpperCase(Locale.ROOT);
                if (!m.matches("GET|POST|PUT|PATCH|DELETE")) continue;
                // paths in this spec are absolute (/api/...); baseURI already ends in /api
                String path = rawPath.startsWith("/api/") ? rawPath.substring(4) : rawPath;
                rows.add(new Object[]{ categoryOf(path), m, path });
            }
        }
        specOperations = rows.size();
        System.out.println("[Catalog] swagger spec: " + paths.length() + " paths / " + specOperations + " operations enumerated");
        catalogRows = rows.toArray(new Object[0][]);
        return catalogRows;
    }

    private static String categoryOf(String path) {
        Matcher m = Pattern.compile("^/([a-zA-Z0-9_-]+)").matcher(path);
        return m.find() ? m.group(1) : "root";
    }

    /** Substitute every {param} with a random UUID (id-like) or a probe token (name-like). */
    private static String resolvePath(String template) {
        Matcher m = Pattern.compile("\\{([^}]+)}").matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String p = m.group(1).toLowerCase(Locale.ROOT);
            String v = (p.endsWith("_id") || p.equals("id") || p.endsWith("id")) ? UUID.randomUUID().toString() : "healthcheck-probe";
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ================================================================
    // THE PROBE
    // ================================================================

    @Test(dataProvider = "catalog",
          description = "Spec-driven health probe: GET authed (no 5xx/HTML), mutations unauth (401/403 gate)")
    public void probeEndpoint(String category, String method, String template) {
        ExtentReportManager.createTest(MODULE, category,
                "CAT_" + method + "_" + template.replaceAll("[^A-Za-z0-9]+", "_"));
        if ("GET".equals(method) && !hasAuthToken()) throw new SkipException("No API token — cannot probe " + template);

        String path = resolvePath(template);
        boolean parameterized = !template.equals(path);
        boolean isPublic = PUBLIC_ENDPOINTS.matcher(template).find();

        Probe pr = doProbe(method, path);
        if (pr.timedOut) {                       // one retry — the QA host has occasional slow spikes
            Probe retry = doProbe(method, path);
            if (!retry.timedOut) pr = retry;
        }
        int http = pr.http; long latency = pr.latency; int payloadKb = pr.payloadKb; String shape = pr.shape;

        if (pr.connectionError) {
            // genuine outage (connection refused / DNS / reset) — the ONLY hard failure
            record(category, method, template, "fail", 0, latency, payloadKb, shape, 0);
            ExtentReportManager.logFail(method + " " + template + " UNREACHABLE — " + pr.detail);
            Assert.fail("[CATALOG] " + method + " " + template + " unreachable: " + pr.detail);
            return;
        }
        if (pr.timedOut) {
            // slow/unresponsive after retry — surface as a critical rec, but DON'T redden the monitor
            record(category, method, template, "warn", 0, latency, payloadKb, shape, 1);
            RECS.add(new String[]{"critical", method + " " + template, "Unresponsive: read timed out after 30s (retried once)"});
            System.out.println("[Catalog] " + method + " " + template + " TIMEOUT (recorded as warn)");
            ExtentReportManager.logWarning(method + " " + template + " — unresponsive (read timeout, retried)");
            return;
        }

        // classify + recommend
        boolean html = "html".equals(shape);
        String status = "pass";
        List<String> recs = new ArrayList<>();
        if ("GET".equals(method)) {
            if (http >= 500) { status = "warn"; recs.add("critical|5xx on authenticated GET (" + http + ")"); }
            else if (http == 200 && html) { status = "warn"; recs.add("critical|Returns 200 with HTML body for an API path (SPA fallback instead of JSON)"); }
            else if (!parameterized && http >= 400 && http != 404) { status = "warn"; recs.add("info|Authenticated GET returns HTTP " + http); }
            if (latency >= VERY_SLOW_MS) recs.add("critical|Very slow response: " + latency + "ms");
            else if (latency >= SLOW_MS) recs.add("warning|Slow response: " + latency + "ms");
        } else if (isPublic) {
            if (http >= 500) { status = "warn"; recs.add("critical|Public endpoint 5xx on empty-body probe (" + http + ")"); }
        } else {
            // For a mutation probed WITHOUT auth:
            if (http == 401 || http == 403) { status = "pass"; }                       // correctly auth-gated
            else if (http == 200 && html) {                                            // fell through to the SPA — NOT an auth bypass
                status = "warn"; recs.add("critical|Returns 200 with HTML body for an API path (SPA fallback instead of JSON)");
            }
            else if (http >= 500) { status = "warn"; recs.add("warning|5xx on UNAUTH empty-body probe — validates before auth (" + http + ")"); }
            else if (http >= 200 && http < 300) {                                      // real 2xx JSON with no token = genuine gap
                status = "warn"; recs.add("critical|Accepts UNAUTHENTICATED " + method + " with a JSON response (HTTP " + http + ") — auth gate missing");
            }
            else { recs.add("info|Unauth probe answered HTTP " + http + " before auth check (validation-before-auth ordering)"); }
        }

        record(category, method, template, status, http, latency, payloadKb, shape, recs.size());
        String line = String.format("[Catalog] %-6s %-60s %s http=%d %dms", method, template, status.toUpperCase(Locale.ROOT), http, latency);
        System.out.println(line);
        for (String r : recs) { String[] p = r.split("\\|", 2); RECS.add(new String[]{p[0], method + " " + template, p[1]}); }

        if (recs.stream().anyMatch(r -> r.startsWith("critical"))) {
            ExtentReportManager.logWarning(method + " " + template + " — " + recs);
        } else if ("warn".equals(status)) {
            ExtentReportManager.logWarning(method + " " + template + " — HTTP " + http);
        } else {
            ExtentReportManager.logPass(method + " " + template + " healthy (HTTP " + http + ", " + latency + "ms).");
        }
    }

    /** One probe outcome. connectionError = true outage (refused/DNS); timedOut = read-timeout. */
    private static class Probe {
        int http; long latency; int payloadKb; String shape = "-"; String detail = "";
        boolean connectionError; boolean timedOut;
    }

    /** Issue the probe request: GET authenticated; mutations UNAUTHENTICATED with an empty JSON body. */
    private Probe doProbe(String method, String path) {
        Probe p = new Probe();
        long t0 = System.currentTimeMillis();
        try {
            Response resp;
            if ("GET".equals(method)) {
                resp = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                        .when().get(path).then().extract().response();
            } else {
                RequestSpecification unauth = getRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation();
                switch (method) {
                    case "POST":   resp = unauth.body("{}").when().post(path).then().extract().response(); break;
                    case "PUT":    resp = unauth.body("{}").when().put(path).then().extract().response(); break;
                    case "PATCH":  resp = unauth.body("{}").when().patch(path).then().extract().response(); break;
                    default:       resp = unauth.when().delete(path).then().extract().response(); break;
                }
            }
            p.http = resp.statusCode();
            p.latency = resp.time();
            String body = resp.asString();
            p.payloadKb = body == null ? 0 : (int) Math.round(body.getBytes().length / 1024.0);
            String b = body == null ? "" : body.trim();
            p.shape = b.startsWith("[") ? "array" : b.startsWith("{") ? "object" : b.startsWith("<") ? "html" : "-";
            p.detail = "HTTP " + p.http;
        } catch (Exception e) {
            p.latency = System.currentTimeMillis() - t0;
            p.detail = e.getClass().getSimpleName() + ": " + e.getMessage();
            String cls = e.getClass().getSimpleName();
            // SocketTimeoutException("Read timed out") = slow endpoint; everything else = outage.
            p.timedOut = cls.contains("Timeout") || String.valueOf(e.getMessage()).toLowerCase().contains("timed out");
            p.connectionError = !p.timedOut;
        }
        return p;
    }

    // Last column is a numeric recommendation count (not free text) so the shared
    // gen_api_health_report.py row parser — which requires a trailing \d+ — renders these rows too.
    private void record(String category, String method, String template, String status,
                        int http, long latency, int payloadKb, String shape, int recsCount) {
        RESULTS.add(new String[]{ category, method + " " + template, template, status,
                http == 0 ? "-" : String.valueOf(http), latency + "ms", "-", shape, payloadKb + "KB", String.valueOf(recsCount) });
    }

    // ================================================================
    // COVERAGE PROOF + REPORT
    // ================================================================

    @Test(dependsOnMethods = "probeEndpoint", alwaysRun = true,
          description = "Every operation in the swagger spec was probed (full catalog coverage)")
    public void testCatalogCoverage() {
        ExtentReportManager.createTest(MODULE, "Coverage", "CAT_COVERAGE");
        Assert.assertTrue(specOperations > 0, "The swagger spec should have been fetched and enumerated.");
        int probed = RESULTS.size();
        System.out.println("[Catalog] coverage: probed " + probed + " of " + specOperations + " spec operations");
        Assert.assertEquals(probed, specOperations,
                "Every spec operation must be probed (spec=" + specOperations + ", probed=" + probed + ").");
        ExtentReportManager.logPass("Full catalog covered: " + probed + "/" + specOperations + " operations probed.");
    }

    @AfterClass(alwaysRun = true)
    public void writeCatalogReport() {
        List<String[]> rows = new ArrayList<>(RESULTS);
        rows.sort((a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
        int pass = 0, warn = 0, fail = 0; long latSum = 0, latN = 0;
        for (String[] r : rows) {
            if ("pass".equals(r[3])) pass++; else if ("warn".equals(r[3])) warn++; else fail++;
            try { latSum += Long.parseLong(r[5].replace("ms", "")); latN++; } catch (Exception ignored) {}
        }
        int crit = 0, wrn = 0, info = 0;
        for (String[] r : RECS) { if ("critical".equals(r[0])) crit++; else if ("warning".equals(r[0])) wrn++; else info++; }

        StringBuilder md = new StringBuilder();
        // Title kept parseable by gen_api_health_report.py's "# API ... Health ... — <url>" regex.
        md.append("# API Full Catalog Health — ").append(AppConstants.BASE_URL).append("\n\n");
        md.append("**").append(rows.size()).append(" operations** (spec-driven from /api/swagger.json) · ")
          .append(pass).append(" pass · ").append(warn).append(" warn · ").append(fail).append(" fail · ")
          .append("avg ").append(latN > 0 ? (latSum / latN) : 0).append("ms · ")
          .append("recommendations: ").append(crit).append(" critical / ").append(wrn).append(" warning / ").append(info).append(" info\n\n");
        md.append("Probe policy: GET = authenticated (random-UUID path params); POST/PUT/PATCH/DELETE = ")
          .append("UNAUTHENTICATED empty-body probe (auth-gate check only — zero mutation risk).\n\n");
        md.append("| Category | Endpoint | Path | Status | HTTP | Latency | Items | Shape | Payload | Recs |\n");
        md.append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (String[] r : rows) md.append("| ").append(String.join(" | ", r)).append(" |\n");
        if (!RECS.isEmpty()) {
            md.append("\n## Recommendations\n\n");
            RECS.sort((a, b) -> a[0].compareTo(b[0]));
            for (String[] r : RECS) md.append("- **").append(r[0]).append("** · ").append(r[1]).append(" — ").append(r[2]).append("\n");
        }
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/api-catalog-report.md")) { w.write(md.toString()); }
            System.out.println("[Catalog] wrote reports/api-catalog-report.md (" + rows.size() + " rows)");
        } catch (Exception e) { System.out.println("[Catalog] report write failed: " + e.getMessage()); }
    }
}
