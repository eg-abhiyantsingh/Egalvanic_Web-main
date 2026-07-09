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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * <b>Spec-driven, catalog-wide LIST-API pagination audit.</b>
 *
 * <p>{@link ListApiContractApiTest} enforces the pagination contract on a curated set of 7 known-good
 * list endpoints (hard assertions). This complements it by applying the SAME contract to EVERY
 * collection endpoint the live catalog exposes — the {@code /api/swagger.json} spec is enumerated at
 * run time, so a newly-added list endpoint is audited automatically with no code change. As of the
 * 2026-07-09 spec ~63 of the param-free GET roots return JSON collections (17 with ≥20 records:
 * {@code eqp-lib/manufacturers}=448, {@code reporting/page-templates}=366, {@code attachment/}=217,
 * {@code opportunity/}=193, {@code contact/}, {@code account/}, {@code mutations}, …) — none of which
 * the curated 7 covered.</p>
 *
 * <p>For each discovered collection this audits the eGalvanic list contract (mirrors the
 * {@code api-contract-review} skill): <b>paginated</b> ({@code page/per_page} or {@code limit}
 * honored), <b>bounded default</b> page size (≤ {@value #MAX_ALLOWED}, ideal ≤ {@value #DEFAULT_IDEAL}),
 * <b>max-limit</b> capped (a {@code per_page=1000} may not dump the whole table), a
 * <b>filter/search</b> param that does not 5xx, and a response <b>within a few seconds</b>.</p>
 *
 * <p><b>Discovery, not enforcement of shape.</b> A candidate that is not a JSON collection (single
 * object / HTML / error / too few records) is SKIPPED, never failed — only real collections are
 * audited. <b>Report-mode</b> by default (keeps the green-by-default Suite 3 monitor green); set
 * {@code -DSTRICT_LIST_API_CONTRACT=true} — the SAME gate {@link ListApiContractApiTest} uses — to
 * make contract violations hard-fail. GET-only: zero mutation risk. Findings →
 * {@code reports/list-api-catalog-report.md}.</p>
 */
public class ListApiCatalogAuditTest extends BaseAPITest {

    private static final long RESP_HARD_MS = 8000;
    private static final long RESP_WARN_MS = 2500;
    private static final int  MAX_ALLOWED  = 100;
    private static final int  DEFAULT_IDEAL = 20;
    private static final int  MIN_COLLECTION = 1;   // a "collection" must return at least one record to audit
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_LIST_API_CONTRACT",
                    System.getenv().getOrDefault("STRICT_LIST_API_CONTRACT", "false")));

    private static final String MODULE = "API — List Catalog Audit (spec-driven)";

    private static final RestAssuredConfig PROBE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 30000));

    private static Object[][] endpointRows;
    private static int candidateCount = -1;
    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> AUDITED = Collections.synchronizedList(new ArrayList<>());

    // ── candidate enumeration: every param-free GET root in the spec ──────────
    // parallel: probes are independent, read-only, and record into synchronized lists.
    @DataProvider(name = "collections", parallel = true)
    public Object[][] collections() {
        if (endpointRows != null) return endpointRows;
        Response spec = RestAssured.given().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/swagger.json").then().extract().response();
        if (spec.statusCode() != 200) {
            throw new SkipException("swagger.json not fetchable (HTTP " + spec.statusCode()
                    + ") — cannot enumerate list endpoints.");
        }
        JSONObject paths = new JSONObject(spec.asString()).getJSONObject("paths");
        List<Object[]> rows = new ArrayList<>();
        for (String rawPath : new TreeMap<>(paths.toMap()).keySet()) {
            String path = rawPath.startsWith("/api/") ? rawPath.substring(4) : rawPath;
            if (path.contains("{")) continue;                       // collection ROOTS only (no id to resolve)
            JSONObject ops = paths.getJSONObject(rawPath);
            boolean hasGet = ops.keySet().stream().anyMatch(k -> k.equalsIgnoreCase("get"));
            if (!hasGet) continue;
            rows.add(new Object[]{ categoryOf(path), path });
        }
        candidateCount = rows.size();
        System.out.println("[ListCatalog] enumerated " + candidateCount + " param-free GET roots from the spec.");
        endpointRows = rows.toArray(new Object[0][]);
        return endpointRows;
    }

    private static String categoryOf(String path) {
        int i = path.indexOf('/', 1);
        return i > 0 ? path.substring(1, i) : path.substring(1);
    }

    @Test(dataProvider = "collections",
          description = "Every collection endpoint in the catalog satisfies the LIST-API pagination contract")
    public void auditCollection(String category, String path) {
        ExtentReportManager.createTest(MODULE, category, "LISTCAT_" + path.replaceAll("[^A-Za-z0-9]+", "_"));
        if (!hasAuthToken()) throw new SkipException("No API auth token — cannot audit " + path + ".");

        Probe base = probe(path);
        if (base.timedOut) base = probe(path);   // one retry — the QA host has transient latency spikes

        // Unresponsive (read timed out on BOTH attempts) → do NOT silently skip. An endpoint too slow to
        // even answer a default GET can't be classified, and a genuinely slow one is likely streaming an
        // unbounded collection (this is the /planned_workorder_line/ case: ~30s, previously dropped by the
        // "not 200" skip). Reported as a DISTINCT "UNRESPONSIVE" outcome — NOT a confirmed pagination
        // violation and NOT STRICT-gated, because under the suite's parallel load a slow-but-fine endpoint
        // can also time out; this is a "couldn't audit, verify pagination manually" advisory, not a bug.
        if (base.timedOut) {
            String v = "UNRESPONSIVE — GET timed out after ~" + base.ms + "ms (> " + RESP_HARD_MS
                    + "ms ceiling) on both attempts; too slow to classify. Verify manually: an endpoint "
                    + "this slow likely returns an unbounded collection and needs ?page/per_page pagination.";
            REPORT.add(new String[]{ category, path, "timeout", "?", "unknown", "unknown", "unknown",
                    "TIMEOUT " + base.ms + "ms", "UNRESPONSIVE" });
            AUDITED.add(path);
            System.out.println("[ListCatalog][WARN] [" + path + "] " + v);
            ExtentReportManager.logWarning("[" + path + "] " + v);
            return;
        }

        // Discovery filter: only real JSON collections are auditable. Everything else SKIPS (not a fail).
        if (base.status == 404) throw new SkipException(path + " → 404 (not deployed).");
        if (base.html)         throw new SkipException(path + " serves the SPA shell (not a JSON API collection).");
        if (base.status != 200) throw new SkipException(path + " → HTTP " + base.status + " (not an accessible collection).");
        if (!base.jsonList)    throw new SkipException(path + " returns a single object, not a collection.");
        if (base.count < MIN_COLLECTION) throw new SkipException(path + " is an empty collection (0 records) — nothing to paginate.");

        int defaultCount = base.count;
        int total = base.total >= 0 ? base.total : defaultCount;

        // ── pagination honored? ──
        Boolean paginated = null; String pageParam = "—";
        if (total >= 2) {
            Probe p1 = probe(path + "?page=1&per_page=1");
            if (p1.status == 200 && p1.count == 1) { paginated = true; pageParam = "per_page"; }
            else {
                Probe l1 = probe(path + "?page=1&limit=1");
                if (l1.status == 200 && l1.count == 1) { paginated = true; pageParam = "limit"; }
                else paginated = false;
            }
        }
        // ── max-limit enforced? (only decidable when the collection exceeds the cap) ──
        Boolean maxLimitEnforced = null; int bigCount = -1;
        if (total > MAX_ALLOWED) {
            Probe big = probe(path + "?page=1&per_page=1000");
            bigCount = big.count;
            maxLimitEnforced = big.status == 200 && big.count <= MAX_ALLOWED;
        }
        boolean defaultBounded = defaultCount <= MAX_ALLOWED;
        boolean defaultIdeal   = defaultCount <= DEFAULT_IDEAL;

        // ── filter/search accepted (no 5xx)? ──
        Probe filt = probe(path + "?search=zzq_unlikely_filter_zzq");
        boolean filterNo5xx = filt.status < 500;
        boolean filterEffective = filt.status == 200 && filt.count >= 0 && filt.count < defaultCount;

        String pag  = paginated == null ? "n/a(<2 recs)" : (paginated ? "yes(" + pageParam + ")" : "NO");
        String maxc = maxLimitEnforced == null ? "n/a(≤" + MAX_ALLOWED + ")" : (maxLimitEnforced ? "yes" : "NO(" + bigCount + ")");
        String defc = defaultCount + (defaultIdeal ? "" : (defaultBounded ? " (>ideal)" : " UNBOUNDED"));
        String respFlag = base.ms > RESP_WARN_MS ? ("SLOW " + base.ms + "ms") : (base.ms + "ms");

        // A VIOLATION only where pagination genuinely matters — an endpoint that can dump a LARGE
        // collection. Small fixed lists (enums, lookups, a 2-element roles[] on /auth/me) that
        // return everything are NOT defects; they're recorded as advisories, not violations, so the
        // report flags real risk (unbounded large lists) instead of crying wolf on every tiny array.
        List<String> violations = new ArrayList<>();
        List<String> advisories = new ArrayList<>();
        boolean large = total > MAX_ALLOWED;                 // exceeds the max page → must paginate + cap
        boolean overIdeal = total > DEFAULT_IDEAL;           // bigger than the ideal default page
        if (Boolean.FALSE.equals(paginated)) {
            if (large) violations.add("NOT paginated — returns all " + total + " (> max " + MAX_ALLOWED + "; unbounded growth risk)");
            else if (overIdeal) advisories.add("not paginated (returns all " + total + "; small today, no page control)");
        }
        if (Boolean.FALSE.equals(maxLimitEnforced)) violations.add("NO max-limit (per_page=1000 returned " + bigCount + " > " + MAX_ALLOWED + ")");
        if (!defaultBounded) violations.add("default page size unbounded (" + defaultCount + " > " + MAX_ALLOWED + ")");
        else if (!defaultIdeal && large) advisories.add("default page " + defaultCount + " > ideal " + DEFAULT_IDEAL);
        if (!filterNo5xx) violations.add("search/filter param caused HTTP " + filt.status + " (server error)");
        if (base.ms > RESP_HARD_MS) violations.add("response " + base.ms + "ms > " + RESP_HARD_MS + "ms hard ceiling");
        else if (base.ms > RESP_WARN_MS) advisories.add("slow (" + base.ms + "ms > " + RESP_WARN_MS + "ms target)");

        boolean compliant = violations.isEmpty();
        REPORT.add(new String[]{ category, path, String.valueOf(total), defc, pag, maxc,
                (filterEffective ? "yes" : (filterNo5xx ? "ignored" : "5xx")), respFlag,
                compliant ? (advisories.isEmpty() ? "OK" : "OK*") : "VIOLATION" });
        AUDITED.add(path);

        String line = String.format("[%s] records=%d total=%d | paginated=%s | maxLimit=%s | default=%s | filter=%s | %s",
                path, defaultCount, total, pag, maxc, defc,
                (filterEffective ? "effective" : (filterNo5xx ? "ignored/none" : "5xx")), respFlag);
        System.out.println("[ListCatalog] " + line);
        if (!advisories.isEmpty()) {
            ExtentReportManager.logInfo("[" + path + "] advisory: " + String.join("; ", advisories));
        }

        if (violations.isEmpty()) {
            ExtentReportManager.logPass("[" + path + "] list-API contract OK — " + line);
        } else {
            String msg = "[" + path + "] LIST-API CONTRACT: " + String.join("; ", violations) + " | " + line;
            if (STRICT) {
                ExtentReportManager.logFail(msg);
                Assert.fail(msg);
            } else {
                ExtentReportManager.logWarning(msg + "  (reported; set -DSTRICT_LIST_API_CONTRACT=true to enforce)");
                System.out.println("[ListCatalog][WARN] " + msg);
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        StringBuilder md = new StringBuilder();
        md.append("# List-API Catalog Audit (spec-driven)\n\n");
        md.append("Every param-free GET collection in `/api/swagger.json`, audited against the list contract: ")
          .append("**paginated**, **filter/search**, **max limit ≤ ").append(MAX_ALLOWED)
          .append("**, **bounded default (ideal ≤ ").append(DEFAULT_IDEAL).append(")**, responds within a few seconds.\n\n");
        md.append("| Category | Path | Total | Default | Paginated | MaxLimit≤").append(MAX_ALLOWED)
          .append(" | Filter | RespTime | Verdict |\n|---|---|---|---|---|---|---|---|---|\n");
        List<String[]> rows = new ArrayList<>(REPORT);
        rows.sort((a, b) -> a[1].compareTo(b[1]));
        int violations = 0, unresponsive = 0;
        for (String[] r : rows) {
            md.append("| ").append(String.join(" | ", r)).append(" |\n");
            if ("VIOLATION".equals(r[8])) violations++;
            else if ("UNRESPONSIVE".equals(r[8])) unresponsive++;
        }
        md.append("\n**").append(rows.size()).append(" collection APIs audited")
          .append(candidateCount >= 0 ? " (of " + candidateCount + " param-free GET roots enumerated)" : "")
          .append(" — ").append(violations).append(" pagination violation(s)")
          .append(STRICT ? " (STRICT: enforced)" : " (reported; not enforced)")
          .append(", ").append(unresponsive).append(" unresponsive/too-slow-to-audit (advisory).**\n");
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/list-api-catalog-report.md")) { w.write(md.toString()); }
            System.out.println("\n[ListCatalog] Report → reports/list-api-catalog-report.md");
        } catch (Exception e) { System.out.println("[ListCatalog] report write failed: " + e.getMessage()); }
        System.out.println("\n===== LIST-API CATALOG AUDIT =====\n" + md);
    }

    // ── probe helper (envelope-tolerant: bare array or {…: [ ]} wrapper) ──────
    private static class Probe {
        int status; long ms; int count = -1; int total = -1; boolean jsonList; boolean html;
        boolean timedOut;   // socket/read timeout — the endpoint was too slow to answer within PROBE_CONFIG
    }

    private Probe probe(String pathWithQuery) {
        Probe p = new Probe();
        long t0 = System.currentTimeMillis();
        try {
            Response r = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                    .when().get(pathWithQuery).then().extract().response();
            p.status = r.statusCode();
            p.ms = r.time();
            String body = r.asString() == null ? "" : r.asString().trim();
            if (body.startsWith("<")) { p.html = true; return p; }
            if (body.startsWith("[")) {
                p.jsonList = true; p.count = new JSONArray(body).length();
            } else if (body.startsWith("{")) {
                JSONObject o = new JSONObject(body);
                for (String k : o.keySet()) {
                    if (o.optJSONArray(k) != null) { p.jsonList = true; p.count = o.getJSONArray(k).length(); break; }
                }
                if (o.has("total")) p.total = o.optInt("total", -1);
                else if (o.has("count")) p.total = o.optInt("count", -1);
                else if (o.has("total_count")) p.total = o.optInt("total_count", -1);
            }
        } catch (Exception e) {
            p.ms = System.currentTimeMillis() - t0;
            String cls = e.getClass().getSimpleName();
            p.timedOut = cls.contains("Timeout") || String.valueOf(e.getMessage()).toLowerCase().contains("timed out");
            System.out.println("[ListCatalog] probe(" + pathWithQuery + ") "
                    + (p.timedOut ? "TIMED OUT after ~" + p.ms + "ms" : "error: " + e.getMessage()));
        }
        return p;
    }
}
