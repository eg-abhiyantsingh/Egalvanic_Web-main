package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

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

/**
 * <b>List-API contract audit</b> — the non-functional requirement raised by Dharmesh Avaiya / Mukul Panchal:
 * every list / collection API should (1) return the expected records, (2) be <b>paginated</b> with a
 * <b>default page size (ideally 10–20)</b> and a <b>max limit (50–100)</b> when it has a filter/search/list,
 * and (3) respond within a few seconds.
 *
 * <p>For each catalogued collection endpoint this probes the live API and audits:</p>
 * <ul>
 *   <li><b>Health (hard):</b> 200 + JSON list + responds ≤ {@value #RESP_HARD_MS} ms + a filter/search param
 *       does not 5xx. A violation here is an objective defect and fails the build.</li>
 *   <li><b>Pagination policy (reported):</b> is {@code per_page} honored, is a huge page size capped at
 *       ≤ {@value #MAX_ALLOWED}, is the default page size bounded (≤ {@value #MAX_ALLOWED}, ideally
 *       ≤ {@value #DEFAULT_IDEAL})? These are logged as WARN + collected into a compliance report
 *       ({@code reports/list-api-contract-report.md}); set {@code -DSTRICT_LIST_API_CONTRACT=true} (mirrors
 *       {@code STRICT_HEALTH_GATES}) to make policy violations hard-fail once the backend complies.</li>
 * </ul>
 *
 * <p>Endpoints + pagination shape live-verified 2026-07-01 (page/per_page; envelopes {@code data/items/
 * opportunities/slds}); ids are resolved dynamically (companyId from {@code /auth/v2/me}, siteId from the
 * first {@code /company/{id}/slds} item), so nothing is hardcoded to a mutable tenant snapshot.</p>
 */
public class ListApiContractApiTest extends BaseAPITest {

    private static final long RESP_HARD_MS = 8000;   // objective "broken" ceiling
    private static final long RESP_WARN_MS = 2500;   // "within a few seconds" target
    private static final int  MAX_ALLOWED  = 100;    // max page size the policy allows (50–100)
    private static final int  DEFAULT_IDEAL = 20;    // preferred default page size (10–20)
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_LIST_API_CONTRACT",
                    System.getenv().getOrDefault("STRICT_LIST_API_CONTRACT", "false")));

    private static final String MODULE = "API — List Contract";

    private String companyId; private boolean companyResolved;
    private String siteId;    private boolean siteResolved;

    /** Accumulated audit rows for the summary report. */
    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    // ── id resolution (cached; lazy so it never races @BeforeClass) ──────────

    private String companyId() {
        if (!companyResolved) {
            companyResolved = true;
            try {
                Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
                if (me.statusCode() == 200 && !me.asString().trim().startsWith("<")) {
                    companyId = new JSONObject(me.asString()).optString("company_id", null);
                }
            } catch (Exception e) { System.out.println("[ListApi] companyId resolve failed: " + e.getMessage()); }
            System.out.println("[ListApi] companyId=" + companyId);
        }
        return companyId;
    }

    private String siteId() {
        if (!siteResolved) {
            siteResolved = true;
            try {
                String co = companyId();
                if (co != null) {
                    Response r = getAuthenticatedRequestSpec().when().get("/company/" + co + "/slds").then().extract().response();
                    if (r.statusCode() == 200 && r.asString().trim().startsWith("{")) {
                        JSONArray slds = new JSONObject(r.asString()).optJSONArray("slds");
                        if (slds != null && slds.length() > 0) {
                            JSONObject s0 = slds.getJSONObject(0);
                            siteId = s0.optString("id", s0.optString("sld_id", s0.optString("uuid", null)));
                        }
                    }
                }
            } catch (Exception e) { System.out.println("[ListApi] siteId resolve failed: " + e.getMessage()); }
            System.out.println("[ListApi] siteId=" + siteId);
        }
        return siteId;
    }

    // ── endpoint catalogue (templates resolved at runtime) ───────────────────

    @DataProvider(name = "listEndpoints")
    public Object[][] listEndpoints() {
        return new Object[][]{
            {"node_classes",          "/node_classes"},
            {"edge_classes",          "/edge_classes"},
            {"issue_classes",         "/issue_classes"},
            {"company.opportunities", "/company/{companyId}/opportunities"},
            {"company.slds",          "/company/{companyId}/slds"},
            {"sld.library-designations", "/sld/{siteId}/library-designations"},
            {"lookup.nodes",          "/lookup/nodes/{siteId}"},
        };
    }

    @Test(dataProvider = "listEndpoints",
          description = "LIST-API contract: records + pagination + default/max page size + filter + response time")
    public void auditListEndpoint(String label, String template) {
        ExtentReportManager.createTest(MODULE, "List pagination/limit", "LIST_" + label);
        if (!hasAuthToken()) throw new SkipException("No API auth token — cannot audit " + label + ".");

        String path = template;
        if (path.contains("{companyId}")) {
            String co = companyId();
            if (co == null) throw new SkipException("Could not resolve companyId — skipping " + label + ".");
            path = path.replace("{companyId}", co);
        }
        if (path.contains("{siteId}")) {
            String site = siteId();
            if (site == null) throw new SkipException("Could not resolve siteId — skipping " + label + ".");
            path = path.replace("{siteId}", site);
        }

        // ── baseline (default page) ──
        Probe base = probe(path);
        if (base.status == 404 || base.html) {
            throw new SkipException(label + " (" + path + ") is not a JSON collection endpoint (status "
                    + base.status + (base.html ? ", HTML" : "") + ").");
        }
        // HARD health assertions
        Assert.assertEquals(base.status, 200, "[" + label + "] list endpoint should return 200. " + path);
        Assert.assertTrue(base.jsonList, "[" + label + "] should return a JSON list/collection. body starts: " + base.snippet);
        Assert.assertTrue(base.ms <= RESP_HARD_MS,
                "[" + label + "] responded in " + base.ms + "ms (> " + RESP_HARD_MS + "ms hard ceiling).");

        int defaultCount = base.count;
        int total = base.total >= 0 ? base.total : defaultCount;   // best estimate of collection size

        // ── pagination honored? request a single item ──
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

        // ── max-limit enforced? only determinable when collection > MAX_ALLOWED ──
        Boolean maxLimitEnforced = null; int bigCount = -1;
        if (total > MAX_ALLOWED) {
            Probe big = probe(path + "?page=1&per_page=1000");
            bigCount = big.count;
            maxLimitEnforced = big.status == 200 && big.count <= MAX_ALLOWED;
        }

        // ── default page size bounded? ──
        boolean defaultBounded = defaultCount <= MAX_ALLOWED;
        boolean defaultIdeal   = defaultCount <= DEFAULT_IDEAL;

        // ── filter/search accepted (no 5xx)? ──
        Probe filt = probe(path + "?search=zzq_unlikely_filter_zzq");
        boolean filterNo5xx = filt.status < 500;
        boolean filterEffective = filt.status == 200 && filt.count >= 0 && filt.count < defaultCount;

        // HARD: a filter/search param must not blow up the endpoint
        Assert.assertTrue(filterNo5xx, "[" + label + "] a search param caused a " + filt.status + " (server error).");

        String respFlag = base.ms > RESP_WARN_MS ? ("SLOW " + base.ms + "ms") : (base.ms + "ms");

        // ── verdict + report row ──
        String pag  = paginated == null ? "n/a(<2 recs)" : (paginated ? "yes(" + pageParam + ")" : "NO");
        String maxc = maxLimitEnforced == null ? "n/a(≤" + MAX_ALLOWED + ")" : (maxLimitEnforced ? "yes" : "NO(" + bigCount + ")");
        String defc = defaultCount + (defaultIdeal ? "" : (defaultBounded ? " (>ideal)" : " UNBOUNDED"));
        boolean compliant = (paginated == null || paginated) && (maxLimitEnforced == null || maxLimitEnforced) && defaultBounded;
        REPORT.add(new String[]{ label, path, String.valueOf(total), defc, pag, maxc,
                (filterEffective ? "yes" : (filterNo5xx ? "ignored" : "5xx")), respFlag, compliant ? "OK" : "VIOLATION" });

        String line = String.format("[%s] records=%d total=%d | paginated=%s | maxLimit=%s | default=%s | filter=%s | %s",
                label, defaultCount, total, pag, maxc, defc, (filterEffective ? "effective" : (filterNo5xx ? "ignored/none" : "5xx")), respFlag);
        System.out.println("[ListApi] " + line);

        // ── policy: report by default; enforce only under STRICT ──
        List<String> violations = new ArrayList<>();
        if (Boolean.FALSE.equals(paginated)) violations.add("NOT paginated (per_page/limit ignored; returns all " + total + ")");
        if (Boolean.FALSE.equals(maxLimitEnforced)) violations.add("NO max-limit (per_page=1000 returned " + bigCount + " > " + MAX_ALLOWED + ")");
        if (!defaultBounded) violations.add("default page size unbounded (" + defaultCount + " > " + MAX_ALLOWED + ")");
        if (base.ms > RESP_WARN_MS) violations.add("slow (" + base.ms + "ms > " + RESP_WARN_MS + "ms target)");

        if (violations.isEmpty()) {
            ExtentReportManager.logPass("[" + label + "] list-API contract OK — " + line);
        } else {
            String msg = "[" + label + "] LIST-API CONTRACT: " + String.join("; ", violations) + " | " + line;
            if (STRICT) {
                Assert.fail(msg);
            } else {
                ExtentReportManager.logWarning(msg + "  (reported; set -DSTRICT_LIST_API_CONTRACT=true to enforce)");
                System.out.println("[ListApi][WARN] " + msg);
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        StringBuilder md = new StringBuilder();
        md.append("# List-API Contract Audit\n\n");
        md.append("Requirement: every list API should be **paginated**, expose **filters/search**, cap at a ")
          .append("**max limit ≤ ").append(MAX_ALLOWED).append("**, and use a **bounded default (ideal ≤ ")
          .append(DEFAULT_IDEAL).append(")**, responding within a few seconds.\n\n");
        md.append("| Endpoint | Path | Total | Default | Paginated | MaxLimit≤").append(MAX_ALLOWED)
          .append(" | Filter | RespTime | Verdict |\n");
        md.append("|---|---|---|---|---|---|---|---|---|\n");
        List<String[]> rows = new ArrayList<>(REPORT);
        rows.sort((a, b) -> a[0].compareTo(b[0]));
        int violations = 0;
        for (String[] r : rows) {
            md.append("| ").append(String.join(" | ", r)).append(" |\n");
            if ("VIOLATION".equals(r[8])) violations++;
        }
        md.append("\n**").append(rows.size()).append(" list APIs audited — ").append(violations)
          .append(" violation(s)").append(STRICT ? " (STRICT: enforced)" : " (reported; not enforced)").append(".**\n");
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/list-api-contract-report.md")) { w.write(md.toString()); }
            System.out.println("\n[ListApi] Compliance report → reports/list-api-contract-report.md");
        } catch (Exception e) { System.out.println("[ListApi] report write failed: " + e.getMessage()); }
        System.out.println("\n===== LIST-API CONTRACT AUDIT =====\n" + md);
    }

    // ── probe helper ─────────────────────────────────────────────────────────

    private static class Probe {
        int status; long ms; int count = -1; int total = -1; boolean jsonList; boolean html; String snippet = "";
    }

    /** GET {pathWithQuery} (authed) and parse status / time / record count / envelope total. */
    private Probe probe(String pathWithQuery) {
        Probe p = new Probe();
        try {
            Response r = getAuthenticatedRequestSpec().when().get(pathWithQuery).then().extract().response();
            p.status = r.statusCode();
            p.ms = r.time();
            String body = r.asString() == null ? "" : r.asString().trim();
            p.snippet = body.substring(0, Math.min(30, body.length()));
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
            System.out.println("[ListApi] probe(" + pathWithQuery + ") error: " + e.getMessage());
        }
        return p;
    }
}
