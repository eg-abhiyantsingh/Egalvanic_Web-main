package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;

import org.json.JSONArray;
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
import java.util.Iterator;
import java.util.List;

/**
 * <b>Error-status contract audit</b> — how the API behaves when things go WRONG.
 *
 * <p>Suite 3's other classes check the happy path (health, catalog reachability,
 * pagination). This class checks the error contract, spec-driven from
 * {@code /api/swagger.json} and strictly GET-only (zero mutation risk):</p>
 * <ol>
 *   <li><b>Unknown resource (404 contract):</b> GET detail ops with random UUIDs —
 *       must never 5xx; SHOULD be 404 (200s are reported: SPA-catch-all / silent-empty
 *       responses hide broken links from clients).</li>
 *   <li><b>Malformed path params (400/404 contract):</b> non-UUID junk (<code>abc</code>,
 *       <code>-1</code>, 512-char strings, URL-encoded control chars) in id slots —
 *       must never 5xx (a 500 on bad input = unhandled exception, OWASP-adjacent).</li>
 *   <li><b>Unauthenticated GETs (401 contract):</b> a catalog sample fetched with no
 *       token — 401/403 expected; 200-with-data is reported as a data-exposure candidate.</li>
 *   <li><b>5xx stability probe (the "502 detector"):</b> the critical-path panel
 *       (health, alliance-config, branding, company config, auth/me, sessions,
 *       workorders, ops/sales dashboards) sampled repeatedly — catches the
 *       INTERMITTENT 502s/timeouts that single-shot health checks miss (the
 *       2026-07-03 incident produced exactly this signature for ~2 hours).</li>
 * </ol>
 *
 * <p><b>Failure policy:</b> any 5xx on malformed/unknown input is a hard fail (server
 * must never crash on client input). The stability probe hard-fails on ≥2 5xx/timeouts
 * (one blip → WARN). Soft contract gaps (200 instead of 404/401) are reported to
 * {@code reports/error-contract-report.md}; set {@code -DSTRICT_ERROR_CONTRACT=true}
 * to escalate them once the backend complies.</p>
 */
public class ErrorContractApiTest extends BaseAPITest {

    private static final int  SAMPLE_OPS      = 40;    // catalog GET ops sampled per probe
    private static final long RESP_HARD_MS    = 15000; // per-call hang ceiling for probes
    private static final int  STABILITY_ROUNDS = 4;    // panel sweeps for the 502 detector
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_ERROR_CONTRACT",
                    System.getenv().getOrDefault("STRICT_ERROR_CONTRACT", "false")));

    private static final String MODULE = "API — Error Contract";
    private static final String RANDOM_UUID = "00000000-dead-beef-0000-000000000000";

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    private final List<String> catalogGetPaths = new ArrayList<>();
    private String companyId;

    // ── catalog + id resolution ──────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    public void loadCatalog() {
        try {
            Response spec = getAuthenticatedRequestSpec().when().get("/swagger.json").then().extract().response();
            if (spec.statusCode() != 200) return;
            JSONObject paths = new JSONObject(spec.asString()).getJSONObject("paths");
            for (Iterator<String> it = paths.keys(); it.hasNext(); ) {
                String p = it.next();
                if (paths.getJSONObject(p).has("get")) {
                    catalogGetPaths.add(p.replaceFirst("^/api", ""));
                }
            }
            Collections.sort(catalogGetPaths); // deterministic sampling run-to-run
            System.out.println("[ErrorContract] catalog GET ops: " + catalogGetPaths.size());
        } catch (Exception e) {
            System.out.println("[ErrorContract] swagger load failed: " + e.getMessage());
        }
        try {
            Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
            if (me.statusCode() == 200 && !me.asString().trim().startsWith("<")) {
                companyId = new JSONObject(me.asString()).optString("company_id", null);
            }
        } catch (Exception ignored) {}
    }

    /** Every k-th GET op that has at least one path param — bounded, deterministic sample. */
    private List<String> sampleParamPaths() {
        List<String> withParams = new ArrayList<>();
        for (String p : catalogGetPaths) if (p.contains("{")) withParams.add(p);
        List<String> out = new ArrayList<>();
        if (withParams.isEmpty()) return out;
        int step = Math.max(1, withParams.size() / SAMPLE_OPS);
        for (int i = 0; i < withParams.size() && out.size() < SAMPLE_OPS; i += step) out.add(withParams.get(i));
        return out;
    }

    private static String fill(String template, String value) {
        return template.replaceAll("\\{[^}]+}", value);
    }

    // ── 1. unknown resource → never 5xx, should be 404 ──────────────────────

    @Test(description = "Error contract: GET with random UUIDs never 5xx; 404 expected (200 = hidden-error candidate)")
    public void testUnknownResourceContract() {
        ExtentReportManager.createTest(MODULE, "404 contract", "ERR_404_CONTRACT");
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        List<String> sample = sampleParamPaths();
        if (sample.isEmpty()) throw new SkipException("Swagger catalog unavailable — cannot sample.");

        List<String> crashes = new ArrayList<>(); int notFound = 0, ok200 = 0, other = 0;
        for (String t : sample) {
            String path = fill(t, RANDOM_UUID);
            try {
                Response r = getAuthenticatedRequestSpec().when().get(path).then().extract().response();
                int c = r.statusCode();
                if (c >= 500) crashes.add(path + " → " + c);
                else if (c == 404) notFound++;
                else if (c == 200) ok200++;
                else other++;
            } catch (Exception e) { crashes.add(path + " → " + e.getClass().getSimpleName()); }
        }
        String summary = sample.size() + " ops: 404=" + notFound + " 200=" + ok200 + " other-4xx=" + other
                + " 5xx=" + crashes.size();
        REPORT.add(new String[]{"unknown-resource", crashes.isEmpty() ? (ok200 == 0 ? "OK" : "SOFT-GAP") : "FAIL", summary});
        Assert.assertTrue(crashes.isEmpty(),
                "Unknown-resource GETs must never 5xx (unhandled exception on bad id). Broken: " + crashes);
        if (STRICT) Assert.assertEquals(ok200, 0,
                ok200 + " ops return 200 for a nonexistent id — clients cannot distinguish 'missing' from 'exists'.");
        else if (ok200 > 0) ExtentReportManager.logWarning(ok200 + "/" + sample.size()
                + " ops return 200 for a nonexistent UUID (should be 404) — reported, STRICT_ERROR_CONTRACT gates.");
        ExtentReportManager.logPass("No 5xx across " + sample.size() + " unknown-resource GETs (" + summary + ").");
    }

    // ── 2. malformed path params → never 5xx ────────────────────────────────

    @Test(description = "Error contract: malformed path params (non-UUID junk) never 5xx")
    public void testMalformedPathParams() {
        ExtentReportManager.createTest(MODULE, "400 contract", "ERR_MALFORMED_PATH");
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        List<String> sample = sampleParamPaths();
        if (sample.isEmpty()) throw new SkipException("Swagger catalog unavailable — cannot sample.");
        // 12 ops x 4 junk values keeps runtime bounded while covering the input classes.
        List<String> ops = sample.subList(0, Math.min(12, sample.size()));
        String longStr = "x".repeat(512);
        String[] junk = {"abc", "-1", longStr, "%27%00"};

        List<String> crashes = new ArrayList<>();
        for (String t : ops) {
            for (String v : junk) {
                String path = fill(t, v);
                try {
                    Response r = getAuthenticatedRequestSpec().when().get(path).then().extract().response();
                    if (r.statusCode() >= 500) crashes.add(path + " → " + r.statusCode());
                } catch (Exception e) { crashes.add(path + " → " + e.getClass().getSimpleName()); }
            }
        }
        REPORT.add(new String[]{"malformed-path", crashes.isEmpty() ? "OK" : "FAIL",
                ops.size() + " ops x " + junk.length + " junk values, 5xx=" + crashes.size()});
        Assert.assertTrue(crashes.isEmpty(),
                "Malformed path params must yield 4xx, never 5xx (server crash on client input): " + crashes);
        ExtentReportManager.logPass("No 5xx across " + (ops.size() * junk.length) + " malformed-path probes.");
    }

    // ── 3. unauthenticated GETs → 401/403 ────────────────────────────────────

    @Test(description = "Error contract: catalog GETs without a token are 401/403 (200 = exposure candidate)")
    public void testUnauthenticatedGets() {
        ExtentReportManager.createTest(MODULE, "401 contract", "ERR_401_CONTRACT");
        List<String> sample = sampleParamPaths();
        if (sample.isEmpty()) throw new SkipException("Swagger catalog unavailable — cannot sample.");

        List<String> crashes = new ArrayList<>(); List<String> exposed = new ArrayList<>(); int gated = 0;
        for (String t : sample) {
            String path = fill(t, RANDOM_UUID);
            try {
                Response r = getRequestSpec().when().get(path).then().extract().response();
                int c = r.statusCode();
                String body = r.asString() == null ? "" : r.asString().trim();
                if (c >= 500) crashes.add(path + " → " + c);
                else if (c == 401 || c == 403) gated++;
                // 200 JSON with content and not the SPA shell = data served without auth
                else if (c == 200 && (body.startsWith("{") || body.startsWith("[")) && body.length() > 25) {
                    exposed.add(path);
                }
            } catch (Exception e) { crashes.add(path + " → " + e.getClass().getSimpleName()); }
        }
        String summary = sample.size() + " ops: 401/403=" + gated + " exposed-200=" + exposed.size()
                + " 5xx=" + crashes.size();
        REPORT.add(new String[]{"unauthenticated-get",
                crashes.isEmpty() && exposed.isEmpty() ? "OK" : (crashes.isEmpty() ? "SOFT-GAP" : "FAIL"), summary});
        Assert.assertTrue(crashes.isEmpty(), "Unauthenticated GETs must never 5xx: " + crashes);
        if (STRICT) Assert.assertTrue(exposed.isEmpty(),
                "JSON served WITHOUT auth (exposure candidates): " + exposed);
        else if (!exposed.isEmpty()) ExtentReportManager.logWarning(exposed.size()
                + " op(s) served JSON without a token — review for data exposure: " + exposed);
        ExtentReportManager.logPass("Auth gate: " + summary + ".");
    }

    // ── 4. intermittent-5xx stability probe (the 502 detector) ──────────────

    @Test(description = "Stability probe: critical-path panel sampled repeatedly — catches intermittent 502/timeout")
    public void testCriticalPathStability() {
        ExtentReportManager.createTest(MODULE, "502 detector", "ERR_5XX_STABILITY");
        if (!hasAuthToken()) throw new SkipException("No API auth token.");

        List<String> panel = new ArrayList<>();
        panel.add("/health");
        panel.add("/company/alliance-config/acme");
        panel.add("/branding/company/acme");
        panel.add("/company/config/acme");
        panel.add("/auth/v2/me");
        if (companyId != null) {
            panel.add("/company/" + companyId + "/sessions?page=1&per_page=5");
            panel.add("/company/" + companyId + "/workorders?page=1&per_page=5");
            panel.add("/company/" + companyId + "/ops-dashboard");
            panel.add("/company/" + companyId + "/sales-dashboard");
        }

        List<String> incidents = new ArrayList<>(); long worst = 0; String worstEp = "-";
        for (int round = 1; round <= STABILITY_ROUNDS; round++) {
            for (String ep : panel) {
                long t0 = System.currentTimeMillis();
                String verdict = null;
                try {
                    Response r = getAuthenticatedRequestSpec().when().get(ep).then().extract().response();
                    long ms = System.currentTimeMillis() - t0;
                    if (ms > worst) { worst = ms; worstEp = ep; }
                    if (r.statusCode() >= 500)      verdict = r.statusCode() + " in " + ms + "ms";
                    else if (ms > RESP_HARD_MS)     verdict = "TIMEOUT-CLASS " + ms + "ms (HTTP " + r.statusCode() + ")";
                } catch (Exception e) {
                    verdict = "EXCEPTION " + e.getClass().getSimpleName();
                }
                if (verdict != null) {
                    incidents.add("round" + round + " " + ep + " → " + verdict);
                    System.out.println("[ErrorContract][5xx-probe] " + incidents.get(incidents.size() - 1));
                }
            }
        }
        String summary = panel.size() + " endpoints x " + STABILITY_ROUNDS + " rounds; incidents="
                + incidents.size() + "; worst=" + worst + "ms (" + worstEp + ")";
        REPORT.add(new String[]{"stability-probe",
                incidents.isEmpty() ? "OK" : (incidents.size() == 1 ? "WARN" : "FAIL"), summary});
        if (incidents.size() == 1) {
            ExtentReportManager.logWarning("Single 5xx/timeout blip (tolerated): " + incidents.get(0));
        }
        Assert.assertTrue(incidents.size() < 2,
                "Backend instability: ≥2 5xx/timeouts across the critical-path panel — same signature as the "
                + "2026-07-03 config-service incident (login blocked, dashboards bricked). " + incidents);
        ExtentReportManager.logPass("Critical path stable: " + summary + ".");
    }

    // ── summary report ───────────────────────────────────────────────────────

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        try {
            new File("reports").mkdirs();
            StringBuilder md = new StringBuilder("# Error-Contract Audit\n\n| Probe | Result | Detail |\n|---|---|---|\n");
            for (String[] row : REPORT) md.append("| ").append(row[0]).append(" | ")
                    .append(row[1]).append(" | ").append(row[2]).append(" |\n");
            md.append("\nSTRICT_ERROR_CONTRACT=").append(STRICT).append('\n');
            try (FileWriter w = new FileWriter("reports/error-contract-report.md")) { w.write(md.toString()); }
            System.out.println("[ErrorContract] wrote reports/error-contract-report.md");
        } catch (Exception e) {
            System.out.println("[ErrorContract] report write failed: " + e.getMessage());
        }
    }
}
