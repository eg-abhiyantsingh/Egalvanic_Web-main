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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Pagination BEHAVIOR audit</b> — sibling of {@link ListApiContractApiTest}.
 *
 * <p>The contract test audits pagination <i>policy</i> (is {@code per_page} honored, is the
 * default/max bounded). This class audits pagination <i>semantics</i> — the bugs users hit
 * when paging actually runs:</p>
 * <ol>
 *   <li><b>Disjoint pages:</b> page=1 and page=2 must not return overlapping records
 *       (duplicate rows across pages = classic unstable-sort pagination bug).</li>
 *   <li><b>Stable ordering:</b> the same page requested twice returns the same records in
 *       the same order (non-deterministic ordering silently loses/duplicates records
 *       while a user pages through a grid).</li>
 *   <li><b>Beyond-the-end page:</b> a page far past the data must return an empty 2xx list
 *       (or a clean 4xx) — never a 5xx and never the full unpaginated collection.</li>
 *   <li><b>Abusive params:</b> page=0 / -1 / non-numeric, per_page=0 / -5 / non-numeric
 *       must never 5xx (4xx or sane-default fallback are both acceptable).</li>
 *   <li><b>Total-count consistency:</b> when the envelope exposes a total, it is identical
 *       across pages, and page maths agree with it.</li>
 * </ol>
 *
 * <p><b>Failure policy</b> (mirrors the sibling): a 5xx / non-JSON body / &gt;{@value #RESP_HARD_MS}ms
 * response is an objective defect → hard fail. Semantic violations on endpoints that DO paginate
 * are hard failures too (they corrupt user-visible data). Endpoints that ignore pagination
 * entirely (the known 2026-07-01 audit gap) are reported WARN + skipped for semantics — set
 * {@code -DSTRICT_LIST_API_CONTRACT=true} to escalate those to failures once the backend complies.</p>
 *
 * <p>Endpoints: the same 7 collection APIs as the contract audit, plus the two large real-data
 * collections ({@code /company/{id}/sessions} ~800 rows, {@code /company/{id}/workorders} ~770 rows)
 * where paging behavior actually matters. Ids resolved dynamically — nothing tenant-hardcoded.</p>
 */
public class PaginationBehaviorApiTest extends BaseAPITest {

    private static final long RESP_HARD_MS = 8000;
    private static final int  PROBE_SIZE   = 5;      // per_page used for behavioral probes
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_LIST_API_CONTRACT",
                    System.getenv().getOrDefault("STRICT_LIST_API_CONTRACT", "false")));

    private static final String MODULE = "API — Pagination Behavior";

    private String companyId; private boolean companyResolved;
    private String siteId;    private boolean siteResolved;

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    // ── id resolution (same pattern as ListApiContractApiTest) ──────────────

    private String companyId() {
        if (!companyResolved) {
            companyResolved = true;
            try {
                Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
                if (me.statusCode() == 200 && !me.asString().trim().startsWith("<")) {
                    companyId = new JSONObject(me.asString()).optString("company_id", null);
                }
            } catch (Exception e) { System.out.println("[PageBehavior] companyId resolve failed: " + e.getMessage()); }
        }
        return companyId;
    }

    private String siteId() {
        if (!siteResolved) {
            siteResolved = true;
            try {
                String co = companyId();
                if (co != null) {
                    Response r = getAuthenticatedRequestSpec().when().get("/company/" + co + "/slds")
                            .then().extract().response();
                    if (r.statusCode() == 200 && !r.asString().trim().startsWith("<")) {
                        JSONArray slds = new JSONObject(r.asString()).optJSONArray("slds");
                        if (slds != null && slds.length() > 0) {
                            siteId = slds.getJSONObject(0).optString("site_id",
                                     slds.getJSONObject(0).optString("id", null));
                        }
                    }
                }
            } catch (Exception e) { System.out.println("[PageBehavior] siteId resolve failed: " + e.getMessage()); }
        }
        return siteId;
    }

    @DataProvider(name = "pagedEndpoints")
    public Object[][] pagedEndpoints() {
        return new Object[][]{
            {"node_classes",          "/node_classes"},
            {"edge_classes",          "/edge_classes"},
            {"issue_classes",         "/issue_classes"},
            {"company.opportunities", "/company/{companyId}/opportunities"},
            {"company.slds",          "/company/{companyId}/slds"},
            {"sld.library-designations", "/sld/{siteId}/library-designations"},
            {"lookup.nodes",          "/lookup/nodes/{siteId}"},
            {"company.sessions",      "/company/{companyId}/sessions"},
            {"company.workorders",    "/company/{companyId}/workorders"},
        };
    }

    // ── envelope helpers ─────────────────────────────────────────────────────

    private static final String[] ENVELOPE_KEYS =
            {"data", "items", "results", "sessions", "workorders", "opportunities", "slds", "nodes"};

    /** Parsed page: items + optional total, or null when the body is not a JSON collection. */
    private static final class Page {
        final List<JSONObject> items = new ArrayList<>();
        long total = -1;
        static Page of(String body) {
            String t = body == null ? "" : body.trim();
            try {
                JSONArray arr = null;
                Page p = new Page();
                if (t.startsWith("[")) {
                    arr = new JSONArray(t);
                } else if (t.startsWith("{")) {
                    JSONObject o = new JSONObject(t);
                    for (String k : ENVELOPE_KEYS) {
                        if (o.optJSONArray(k) != null) { arr = o.getJSONArray(k); break; }
                    }
                    for (String tk : new String[]{"total", "total_count", "count", "total_records"}) {
                        if (o.has(tk) && o.opt(tk) instanceof Number) { p.total = o.getLong(tk); break; }
                    }
                    // nested {data:{items:[...], total:n}}
                    if (arr == null && o.optJSONObject("data") != null) {
                        JSONObject d = o.getJSONObject("data");
                        for (String k : ENVELOPE_KEYS) {
                            if (d.optJSONArray(k) != null) { arr = d.getJSONArray(k); break; }
                        }
                        for (String tk : new String[]{"total", "total_count", "count"}) {
                            if (d.has(tk) && d.opt(tk) instanceof Number) { p.total = d.getLong(tk); break; }
                        }
                    }
                }
                if (arr == null) return null;
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.opt(i) instanceof JSONObject) p.items.add(arr.getJSONObject(i));
                }
                return p;
            } catch (Exception e) { return null; }
        }
    }

    /** Stable identity for a record: id-ish field, else the full JSON text. */
    private static String idOf(JSONObject o) {
        for (String k : new String[]{"id", "uuid", "node_id", "sld_id", "session_id", "workorder_id"}) {
            String v = o.optString(k, null);
            if (v != null && !v.isEmpty()) return v;
        }
        return o.toString();
    }

    private static List<String> ids(Page p) {
        List<String> out = new ArrayList<>();
        for (JSONObject o : p.items) out.add(idOf(o));
        return out;
    }

    private String resolve(String template, String label) {
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
        return path;
    }

    /** Authed GET; hard-fails the objective layer (5xx / hang) and returns the response. */
    private Response get(String pathWithQuery, String label) {
        long t0 = System.currentTimeMillis();
        Response r = getAuthenticatedRequestSpec().when().get(pathWithQuery).then().extract().response();
        long ms = System.currentTimeMillis() - t0;
        Assert.assertTrue(r.statusCode() < 500,
                label + ": GET " + pathWithQuery + " returned " + r.statusCode() + " (5xx = objective defect)");
        Assert.assertTrue(ms <= RESP_HARD_MS,
                label + ": GET " + pathWithQuery + " took " + ms + "ms (> " + RESP_HARD_MS + "ms hard ceiling)");
        return r;
    }

    /** True when the endpoint actually honors per_page (the semantics tests need this). */
    private Page probePaginated(String path, String label) {
        String sep = path.contains("?") ? "&" : "?";
        Response r1 = get(path + sep + "page=1&per_page=" + PROBE_SIZE, label);
        Page p1 = Page.of(r1.asString());
        if (p1 == null) throw new SkipException(label + ": response is not a JSON collection — cannot audit pagination.");
        if (p1.items.size() > PROBE_SIZE) {
            String msg = label + ": per_page=" + PROBE_SIZE + " ignored (got " + p1.items.size()
                    + " records) — endpoint is UNPAGINATED (known 2026-07-01 contract gap).";
            REPORT.add(new String[]{label, "UNPAGINATED", p1.items.size() + " records on per_page=" + PROBE_SIZE});
            if (STRICT) Assert.fail(msg);
            ExtentReportManager.logWarning(msg);
            throw new SkipException(msg + " Semantics checks skipped until it paginates.");
        }
        return p1;
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test(dataProvider = "pagedEndpoints",
          description = "Pagination semantics: page1/page2 disjoint, no duplicate records across pages")
    public void testPagesAreDisjoint(String label, String template) {
        ExtentReportManager.createTest(MODULE, "Disjoint pages", "PAGE_DISJOINT_" + label);
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        String path = resolve(template, label);
        String sep = path.contains("?") ? "&" : "?";

        Page p1 = probePaginated(path, label);
        if (p1.items.isEmpty()) throw new SkipException(label + ": 0 records — nothing to page (not a bug).");
        Page p2 = Page.of(get(path + sep + "page=2&per_page=" + PROBE_SIZE, label).asString());
        Assert.assertNotNull(p2, label + ": page=2 did not return a JSON collection.");
        if (p2.items.isEmpty()) {
            REPORT.add(new String[]{label, "OK", "single page of data (" + p1.items.size() + " records)"});
            ExtentReportManager.logPass(label + ": collection fits one page — disjointness trivially holds.");
            return;
        }
        Set<String> s1 = new LinkedHashSet<>(ids(p1));
        Set<String> overlap = new LinkedHashSet<>(ids(p2));
        overlap.retainAll(s1);
        Assert.assertTrue(overlap.isEmpty(),
                label + ": page 1 and page 2 share " + overlap.size() + " record(s) " + overlap
                + " — duplicate rows across pages (unstable sort / offset bug). Users paging a grid"
                + " see repeated rows and silently MISS others.");
        REPORT.add(new String[]{label, "OK", "pages disjoint (" + p1.items.size() + "+" + p2.items.size() + " records)"});
        ExtentReportManager.logPass(label + ": page 1 ∩ page 2 = ∅.");
    }

    @Test(dataProvider = "pagedEndpoints",
          description = "Pagination semantics: same page fetched twice returns identical record order")
    public void testOrderingStable(String label, String template) {
        ExtentReportManager.createTest(MODULE, "Stable ordering", "PAGE_STABLE_" + label);
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        String path = resolve(template, label);
        String sep = path.contains("?") ? "&" : "?";

        Page a = probePaginated(path, label);
        if (a.items.isEmpty()) throw new SkipException(label + ": 0 records — nothing to compare (not a bug).");
        Page b = Page.of(get(path + sep + "page=1&per_page=" + PROBE_SIZE, label).asString());
        Assert.assertNotNull(b, label + ": repeat fetch did not return a JSON collection.");
        Assert.assertEquals(ids(b), ids(a),
                label + ": two identical requests returned different records/order — no stable sort key."
                + " Paging over an unstable order duplicates some records and drops others.");
        REPORT.add(new String[]{label, "OK", "ordering stable across identical requests"});
        ExtentReportManager.logPass(label + ": ordering deterministic.");
    }

    @Test(dataProvider = "pagedEndpoints",
          description = "Pagination robustness: page far beyond the data is an empty 2xx/clean 4xx, never 5xx or full dump")
    public void testBeyondEndPage(String label, String template) {
        ExtentReportManager.createTest(MODULE, "Beyond-end page", "PAGE_BEYOND_" + label);
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        String path = resolve(template, label);
        String sep = path.contains("?") ? "&" : "?";

        probePaginated(path, label); // establishes the endpoint paginates (skips/report otherwise)
        Response r = get(path + sep + "page=999999&per_page=" + PROBE_SIZE, label); // get() asserts <500
        if (r.statusCode() >= 400) {
            REPORT.add(new String[]{label, "OK", "beyond-end page → clean " + r.statusCode()});
            ExtentReportManager.logPass(label + ": beyond-end page → " + r.statusCode() + " (acceptable).");
            return;
        }
        Page p = Page.of(r.asString());
        Assert.assertNotNull(p, label + ": beyond-end page returned non-collection body.");
        Assert.assertTrue(p.items.size() <= PROBE_SIZE,
                label + ": page=999999 returned " + p.items.size() + " records — pagination ignored past the end.");
        Assert.assertTrue(p.items.isEmpty(),
                label + ": page=999999 still returned " + p.items.size() + " record(s) — page offset not applied.");
        REPORT.add(new String[]{label, "OK", "beyond-end page → empty list"});
        ExtentReportManager.logPass(label + ": beyond-end page → empty list.");
    }

    @Test(dataProvider = "pagedEndpoints",
          description = "Pagination robustness: abusive page/per_page values must never 5xx")
    public void testAbusiveParamsNever5xx(String label, String template) {
        ExtentReportManager.createTest(MODULE, "Abusive params", "PAGE_ABUSE_" + label);
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        String path = resolve(template, label);
        String sep = path.contains("?") ? "&" : "?";

        String[] abuses = {
            "page=0&per_page=" + PROBE_SIZE,
            "page=-1&per_page=" + PROBE_SIZE,
            "page=abc&per_page=" + PROBE_SIZE,
            "page=1&per_page=0",
            "page=1&per_page=-5",
            "page=1&per_page=abc",
        };
        List<String> broken = new ArrayList<>();
        for (String q : abuses) {
            Response r = getAuthenticatedRequestSpec().when().get(path + sep + q).then().extract().response();
            if (r.statusCode() >= 500) broken.add(q + " → " + r.statusCode());
        }
        Assert.assertTrue(broken.isEmpty(),
                label + ": server 5xx on malformed pagination input (client input must never crash the server): "
                + broken);
        REPORT.add(new String[]{label, "OK", "6 abusive param combos → no 5xx"});
        ExtentReportManager.logPass(label + ": no 5xx across " + abuses.length + " abusive param combos.");
    }

    @Test(dataProvider = "pagedEndpoints",
          description = "Pagination metadata: envelope total is consistent across pages and matches page maths")
    public void testTotalConsistency(String label, String template) {
        ExtentReportManager.createTest(MODULE, "Total consistency", "PAGE_TOTAL_" + label);
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        String path = resolve(template, label);
        String sep = path.contains("?") ? "&" : "?";

        Page p1 = probePaginated(path, label);
        if (p1.total < 0) {
            REPORT.add(new String[]{label, "NO-TOTAL", "envelope exposes no total/count field"});
            throw new SkipException(label + ": envelope has no total field — nothing to reconcile (reported).");
        }
        Page p2 = Page.of(get(path + sep + "page=2&per_page=" + PROBE_SIZE, label).asString());
        Assert.assertNotNull(p2, label + ": page=2 did not return a JSON collection.");
        if (p2.total >= 0) {
            Assert.assertEquals(p2.total, p1.total,
                    label + ": total changed between page 1 (" + p1.total + ") and page 2 (" + p2.total
                    + ") within seconds — count is per-page, cached inconsistently, or racing.");
        }
        if (p1.total <= PROBE_SIZE) {
            Assert.assertTrue(p2.items.isEmpty(),
                    label + ": total=" + p1.total + " fits page 1 (per_page=" + PROBE_SIZE + ") but page 2 returned "
                    + p2.items.size() + " record(s) — total and paging disagree.");
        } else {
            Assert.assertFalse(p2.items.isEmpty(),
                    label + ": total=" + p1.total + " > per_page=" + PROBE_SIZE + " but page 2 is empty"
                    + " — records unreachable by paging (data loss for grid users).");
        }
        REPORT.add(new String[]{label, "OK", "total=" + p1.total + " consistent across pages"});
        ExtentReportManager.logPass(label + ": total consistent (" + p1.total + ").");
    }

    // ── summary report (same pattern as the sibling audit) ──────────────────

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        try {
            new File("reports").mkdirs();
            StringBuilder md = new StringBuilder("# Pagination Behavior Audit\n\n");
            md.append("| Endpoint | Check | Result |\n|---|---|---|\n");
            for (String[] row : REPORT) md.append("| ").append(row[0]).append(" | ")
                    .append(row[1]).append(" | ").append(row[2]).append(" |\n");
            md.append("\nSTRICT_LIST_API_CONTRACT=").append(STRICT)
              .append(" — UNPAGINATED endpoints ").append(STRICT ? "FAIL" : "are reported as WARN + skipped")
              .append(".\n");
            try (FileWriter w = new FileWriter("reports/pagination-behavior-report.md")) { w.write(md.toString()); }
            System.out.println("[PageBehavior] wrote reports/pagination-behavior-report.md (" + REPORT.size() + " rows)");
        } catch (Exception e) {
            System.out.println("[PageBehavior] report write failed: " + e.getMessage());
        }
    }
}
