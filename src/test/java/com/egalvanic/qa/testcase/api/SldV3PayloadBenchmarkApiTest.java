package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>SLD V3 load-performance &amp; payload benchmark</b> (QA coverage for the "SLD V3 Payload Optimization"
 * ticket — follow-up to ZP-2985). The optimization itself is a <em>backend</em> change (DB-query reduction +
 * serialization) that lives in the read-only product repo; this is the QA side — a black-box benchmark that
 * <b>measures and proves the observable acceptance criteria</b> and guards against regression:
 *
 * <ul>
 *   <li><b>View/SLD load latency</b> — times {@code GET /sld/v3/{id}} (and {@code /sld/v2/{id}} for the
 *       before/after delta) per SLD, classified small / medium / large by node count.</li>
 *   <li><b>Payload size</b> — total bytes + a per-top-level-key breakdown so the dominant contributor is
 *       named (e.g. {@code eg_forms}, {@code nodes}), and the <b>{@code node_terminals}</b> total (summed
 *       across nodes) — the ~8.6 MB bottleneck the ticket calls out for 8k+ node SLDs.</li>
 *   <li><b>No API-structure regression</b> — asserts the v3 response keeps its expected top-level shape
 *       (an optimization that drops/renames fields would break clients).</li>
 * </ul>
 *
 * <p><b>Out of black-box scope (documented, not silently skipped):</b> the "~15-18 → ~3 DB queries" criterion
 * is a server-internal metric not visible over the API — it needs backend instrumentation / APM (e.g. a
 * SQLAlchemy query-count assertion or a request-scoped {@code X-DB-Query-Count} header). The report calls this
 * out so the gap is explicit.</p>
 *
 * <p>Report-mode (never reddens the monitor by default): findings + the before/after table go to
 * {@code reports/sld-v3-payload-benchmark.md}; {@code -DSTRICT_SLD_PERF=true} makes latency/size-target misses
 * hard-fail. A genuine 5xx / non-JSON / structure regression fails regardless. Read-only (GET), zero mutation.
 * Sample size via {@code -Dsld.benchmark.sample=N} (default 8); explicit ids via {@code -Dsld.benchmark.ids=a,b}.</p>
 */
public class SldV3PayloadBenchmarkApiTest extends BaseAPITest {

    private static final String MODULE = "API — SLD V3 Payload Benchmark";

    // Targets from the ticket. Small common-path target ~0.8s; treat >8s as a hard ceiling (broken).
    private static final long TARGET_SMALL_MS = 800, RESP_WARN_MS = 2500, RESP_HARD_MS = 8000;
    private static final long NODE_TERMINALS_WARN_BYTES = 2L * 1024 * 1024;   // 2 MB — flag heavy terminal payloads
    private static final int  SMALL_MAX = 100, MEDIUM_MAX = 1000;            // node-count buckets
    // v3 must keep these top-level keys — the client depends on them (structure-regression guard).
    private static final String[] REQUIRED_KEYS = {"id", "nodes", "edges", "mappings"};
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_SLD_PERF",
                    System.getenv().getOrDefault("STRICT_SLD_PERF", "false")));

    private static final RestAssuredConfig PROBE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 90000));   // large SLDs are slow — generous read timeout

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());
    private String companyId;

    @BeforeClass(alwaysRun = true)
    public void base() { RestAssured.baseURI = API_BASE_URL; }

    private static int bytes(Object o) {
        return o == null ? 0 : o.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    @DataProvider(name = "slds")
    public Object[][] slds() {
        if (!hasAuthToken()) throw new SkipException("No API auth token — cannot benchmark SLD loads.");
        String explicit = System.getProperty("sld.benchmark.ids", "").trim();
        List<Object[]> rows = new ArrayList<>();
        if (!explicit.isEmpty()) {
            for (String id : explicit.split(",")) if (!id.trim().isEmpty()) rows.add(new Object[]{id.trim()});
            return rows.toArray(new Object[0][]);
        }
        int sample = Integer.getInteger("sld.benchmark.sample", 8);
        try {
            Response me = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                    .when().get("/auth/v2/me").then().extract().response();
            companyId = new JSONObject(me.asString()).optString("company_id", null);
            Response list = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                    .when().get("/company/" + companyId + "/slds").then().extract().response();
            JSONArray arr = new JSONObject(list.asString()).optJSONArray("slds");
            for (int i = 0; arr != null && i < arr.length() && rows.size() < sample; i++) {
                String id = arr.getJSONObject(i).optString("id", null);
                if (id != null) rows.add(new Object[]{id});
            }
        } catch (Exception e) {
            throw new SkipException("Could not resolve SLDs to benchmark: " + e.getMessage());
        }
        if (rows.isEmpty()) throw new SkipException("No SLDs available to benchmark for this account.");
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "slds",
          description = "SLD V3 loads within target, keeps its response structure, and node_terminals payload is bounded")
    public void benchmarkSldV3(String sldId) {
        ExtentReportManager.createTest(MODULE, "sld/v3", "SLD load " + sldId);

        Response v3 = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/sld/v3/" + sldId).then().extract().response();
        long v3ms = v3.time();
        String body = v3.asString();

        // Hard: must be a live JSON object (a 5xx / SPA-HTML here is a real break, not a perf nuance).
        Assert.assertEquals(v3.statusCode(), 200, "GET /sld/v3/" + sldId + " must return 200; got " + v3.statusCode());
        Assert.assertTrue(body != null && body.trim().startsWith("{"),
                "GET /sld/v3/" + sldId + " must return a JSON object, not: " + (body == null ? "null" : body.substring(0, Math.min(80, body.length()))));

        JSONObject root = new JSONObject(body);
        JSONObject data = root.optJSONObject("data") != null ? root.getJSONObject("data") : root;

        // Hard: structure-regression guard — the client depends on these keys.
        List<String> missingKeys = new ArrayList<>();
        for (String k : REQUIRED_KEYS) if (!data.has(k)) missingKeys.add(k);
        Assert.assertTrue(missingKeys.isEmpty(),
                "SLD v3 response STRUCTURE regression — missing top-level key(s) " + missingKeys + " for " + sldId);

        int totalBytes = bytes(body);
        JSONArray nodes = data.optJSONArray("nodes");
        int nodeCount = nodes == null ? 0 : nodes.length();
        JSONArray edges = data.optJSONArray("edges");
        int edgeCount = edges == null ? 0 : edges.length();

        // node_terminals: summed serialized bytes across every node (the ticket's 8.6 MB bottleneck).
        long ntBytes = 0;
        for (int i = 0; nodes != null && i < nodes.length(); i++) {
            ntBytes += bytes(nodes.getJSONObject(i).opt("node_terminals"));
        }

        // Per-top-level-key size breakdown → name the dominant contributor.
        Map<String, Integer> keyBytes = new LinkedHashMap<>();
        for (String k : data.keySet()) keyBytes.put(k, bytes(data.opt(k)));
        String topKeys = keyBytes.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(3)
                .map(e -> e.getKey() + "=" + kb(e.getValue()))
                .reduce((a, b) -> a + ", " + b).orElse("-");

        String bucket = nodeCount <= SMALL_MAX ? "small" : nodeCount <= MEDIUM_MAX ? "medium" : "large";

        // before/after delta: time v2 (the pre-optimization path) for the same SLD.
        String v2delta;
        long v2ms = -1; int v2bytes = -1; int v2status = -1;
        try {
            Response v2 = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                    .when().get("/sld/v2/" + sldId).then().extract().response();
            v2status = v2.statusCode();
            if (v2status == 200) { v2ms = v2.time(); v2bytes = bytes(v2.asString()); }
            v2delta = v2status == 200 ? (v2ms + "ms / " + kb(v2bytes)) : ("v2 HTTP " + v2status);
        } catch (Exception e) { v2delta = "v2 error"; }

        String line = String.format("%s | nodes=%d edges=%d | %dms | total=%s | node_terminals=%s | top: %s | v2: %s",
                bucket, nodeCount, edgeCount, v3ms, kb(totalBytes), kb((int) Math.min(ntBytes, Integer.MAX_VALUE)), topKeys, v2delta);
        System.out.println("[SLDv3] " + sldId + " → " + line);

        List<String> findings = new ArrayList<>();
        if (v3ms > RESP_HARD_MS) findings.add("load " + v3ms + "ms > " + RESP_HARD_MS + "ms hard ceiling");
        else if ("small".equals(bucket) && v3ms > TARGET_SMALL_MS) findings.add("small SLD load " + v3ms + "ms > " + TARGET_SMALL_MS + "ms target");
        else if (v3ms > RESP_WARN_MS) findings.add("slow load " + v3ms + "ms");
        if (ntBytes > NODE_TERMINALS_WARN_BYTES) findings.add("node_terminals " + kb((int) Math.min(ntBytes, Integer.MAX_VALUE)) + " (serialization bottleneck)");
        // ZP-3120: v3 is the performance OPTIMIZATION over v2 — guard against a regression where v3
        // is materially slower than v2. Only flag when v3 is meaningfully worse (>25% AND >300ms
        // absolute, to absorb network jitter on already-fast loads); both must be real 200 responses.
        if (v2status == 200 && v2ms > 0 && v3ms > v2ms * 1.25 && (v3ms - v2ms) > 300) {
            findings.add("v3 SLOWER than v2 (" + v3ms + "ms vs " + v2ms + "ms) — optimization regression");
        }

        REPORT.add(new String[]{ bucket, sldId, String.valueOf(nodeCount), v3ms + "ms",
                kb(totalBytes), kb((int) Math.min(ntBytes, Integer.MAX_VALUE)), topKeys, v2delta,
                findings.isEmpty() ? "OK" : String.join("; ", findings) });

        if (!findings.isEmpty()) {
            String msg = "[SLD " + sldId + " · " + bucket + "] " + String.join("; ", findings) + " | " + line;
            if (STRICT) { ExtentReportManager.logFail(msg); Assert.fail(msg); }
            else { ExtentReportManager.logWarning(msg + "  (reported; -DSTRICT_SLD_PERF=true enforces)"); }
        } else {
            ExtentReportManager.logPass("SLD v3 load OK — " + line);
        }
    }

    private static String kb(int b) { return b >= 1024 * 1024 ? String.format("%.1fMB", b / 1048576.0) : (b / 1024) + "KB"; }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        StringBuilder md = new StringBuilder();
        md.append("# SLD V3 Load Performance & Payload Benchmark\n\n");
        md.append("QA coverage for the SLD V3 Payload Optimization ticket (follow-up to ZP-2985). Black-box ")
          .append("measurement of `GET /sld/v3/{id}` — load latency, total payload size, the per-node ")
          .append("`node_terminals` total (the ~8.6 MB bottleneck on 8k+ node SLDs), the dominant payload keys, ")
          .append("and a `/sld/v2/{id}` before/after delta. Targets: small load ≤ ").append(TARGET_SMALL_MS)
          .append("ms, hard ceiling ").append(RESP_HARD_MS).append("ms.\n\n");
        md.append("| Size | SLD id | Nodes | v3 load | Total payload | node_terminals | Top keys | v2 (before) | Findings |\n");
        md.append("|---|---|---|---|---|---|---|---|---|\n");
        List<String[]> rows = new ArrayList<>(REPORT);
        rows.sort((a, b) -> a[0].compareTo(b[0]));
        int violations = 0;
        for (String[] r : rows) {
            md.append("| ").append(String.join(" | ", r)).append(" |\n");
            if (!"OK".equals(r[8])) violations++;
        }
        md.append("\n**").append(rows.size()).append(" SLD(s) benchmarked — ").append(violations)
          .append(" with findings").append(STRICT ? " (STRICT: enforced)" : " (reported)").append(".**\n\n");
        md.append("### Acceptance-criteria coverage\n");
        md.append("- ✅ **View/SLD load latency** (2–3x faster target): measured per SLD above (v3 vs v2 delta).\n");
        md.append("- ✅ **node_terminals serialization bottleneck**: measured (summed across nodes) per SLD.\n");
        md.append("- ✅ **No API-structure regression**: v3 top-level keys ").append(java.util.Arrays.toString(REQUIRED_KEYS))
          .append(" asserted present.\n");
        md.append("- ⚠️ **DB query count (~15-18 → ~3)**: NOT black-box observable over the API — requires backend ")
          .append("instrumentation/APM (e.g. a request-scoped `X-DB-Query-Count` header or a SQLAlchemy query-count ")
          .append("assertion in a backend test). Out of scope for API-level QA; flagged for the backend team.\n");
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/sld-v3-payload-benchmark.md")) { w.write(md.toString()); }
            System.out.println("[SLDv3] Report → reports/sld-v3-payload-benchmark.md");
        } catch (Exception e) { System.out.println("[SLDv3] report write failed: " + e.getMessage()); }
        System.out.println("\n===== SLD V3 PAYLOAD BENCHMARK =====\n" + md);
    }
}
