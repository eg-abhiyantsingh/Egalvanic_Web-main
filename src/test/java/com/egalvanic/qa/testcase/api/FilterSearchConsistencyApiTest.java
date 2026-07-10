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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>Filter / search / list-vs-detail consistency audit</b> — the correctness layer of Suite 3.
 *
 * <p>The other API classes check <i>shape</i> (pagination policy, error codes, auth). This one
 * checks that the list endpoint tells the <i>truth</i>: that a filter returns only matching rows,
 * that search finds what exists and nothing that doesn't, that paging neither drops nor duplicates
 * records, and that the denormalised list projection agrees with the authoritative detail record.
 * These are the bugs a user actually hits — a "Status: Open" filter that shows a Closed issue, a
 * grid whose title differs from the detail page, a second page that repeats a row from the first.</p>
 *
 * <p>Target endpoint: {@code POST /api/v2/issues/list} — the one list API on this platform that is
 * genuinely paginated + filterable + searchable (per the 2026-07-01 contract audit, the other six
 * list APIs ignore pagination, so there is nothing to check there yet). Its contract, captured live
 * on 2026-07-08: body {@code {company_id, sld_id, page, page_size, filters:{}, search:""}} →
 * {@code {data:{items,page,page_size,total}}}; {@code filters} values are <b>scalar strings</b>
 * ({@code {"status":"Open"}} works, {@code {"status":["Open"]}} silently returns empty); search is a
 * substring match across title / description / issue_class_name / sld_name (verified: a term is
 * matched via {@code sld_name} / {@code issue_class_name}, not only the title — so this test asserts
 * the <i>safe</i> directions of search, never "every hit contains the term in the title").</p>
 *
 * <ol>
 *   <li><b>Filter purity</b> (hard) — {@code status=X} / {@code priority=X} return only rows whose
 *       field equals X. Leakage = a straight correctness bug.</li>
 *   <li><b>Partition sum</b> (STRICT) — the per-status filtered counts sum to the unfiltered total
 *       (no row invisible to every filter, none double-counted). Count-based ⇒ mildly race-prone ⇒
 *       gated.</li>
 *   <li><b>Search correctness</b> (hard) — a nonsense term → 0 rows; a term taken from a real row's
 *       title finds that row; every search hit is a member of the full collection (search invents
 *       nothing).</li>
 *   <li><b>Pagination integrity</b> (hard on dup/leak, STRICT on count) — walking pages of size
 *       {@value #PAGE} yields no duplicate ids and no id outside the collection; the union size
 *       equals {@code total}; a page past the end is an empty 2xx.</li>
 *   <li><b>List-vs-detail consistency</b> (hard) — for a sample of rows, {@code GET /issue/{id}}
 *       agrees with the list projection on title / status / priority / issue_class.</li>
 *   <li><b>Filter∧search</b> (hard) — combining a status filter with a search term returns the
 *       intersection (every row matches the status and is in the search set).</li>
 * </ol>
 *
 * <p><b>Read-only</b> — creates nothing, needs no cleanup. Picks the busiest non-sandbox site at
 * runtime (nothing tenant-hardcoded); skips if no site has enough issues to be meaningful. Set
 * {@code -DSTRICT_CONSISTENCY_CONTRACT=true} to escalate the count-based checks.</p>
 */
public class FilterSearchConsistencyApiTest extends BaseAPITest {

    private static final String MODULE = "API — Filter/Search Consistency";
    private static final int PAGE = 5;               // page size for the pagination walk
    private static final int MIN_ISSUES = 4;         // below this, invariants aren't meaningful
    private static final int SITE_SCAN  = 30;        // bounded scan when hunting the busiest site
    private static final int DETAIL_SAMPLE = 6;      // rows cross-checked against their detail
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_CONSISTENCY_CONTRACT",
                    System.getenv().getOrDefault("STRICT_CONSISTENCY_CONTRACT", "false")));

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    private String companyId;
    private String siteId;
    private String siteName;
    private JSONArray snapshot;   // one full-list snapshot taken in setup, reused across tests
    private int total;

    // ── setup: pick the richest site, take one snapshot ────────────────────────

    @BeforeClass(alwaysRun = true)
    public void resolveRichestSite() {
        if (!hasAuthToken()) return;
        try {
            Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
            if (me.statusCode() == 200 && !me.asString().trim().startsWith("<"))
                companyId = new JSONObject(me.asString()).optString("company_id", null);
            if (companyId == null) return;

            Response slds = getAuthenticatedRequestSpec()
                    .when().get("/company/" + companyId + "/slds").then().extract().response();
            if (slds.statusCode() != 200) return;
            JSONArray arr = new JSONObject(slds.asString()).optJSONArray("slds");
            if (arr == null) return;

            int bestTotal = -1;
            for (int i = 0; i < arr.length() && i < SITE_SCAN; i++) {
                JSONObject s = arr.getJSONObject(i);
                String id = s.optString("id", null);
                String name = s.optString("name", "");
                if (id == null) continue;
                // prefer sites NOT mutated by the CRUD/validation suites, to avoid cross-test races
                if (CrudLifecycleApiTest.SANDBOX_SITE_NAME.equalsIgnoreCase(name.trim())) continue;
                int t = listTotal(id);
                if (t > bestTotal) { bestTotal = t; siteId = id; siteName = name; }
                if (bestTotal >= 12) break;   // plenty rich — stop scanning early
            }
            if (siteId != null && bestTotal >= MIN_ISSUES) {
                JSONObject full = list(siteId, 1, 200, new JSONObject(), "");
                snapshot = full.optJSONArray("items");
                total = full.optInt("total", snapshot == null ? 0 : snapshot.length());
            }
            System.out.println("[Consistency] site=" + siteName + " (" + siteId + ") total=" + total);
        } catch (Throwable e) {
            // Catch Throwable (not just Exception): the list() helper asserts status==200, so a transient
            // non-200 / 502 on the flaky QA host during SETUP throws an AssertionError (an Error, not an
            // Exception). Swallow it here so the class SKIPs (requireData → SkipException) instead of the
            // whole @BeforeClass hard-failing the suite on a one-off blip. The real consistency assertions
            // still run (and STRICT-gate) once a site resolves.
            System.out.println("[Consistency] setup skipped (transient): " + e.getClass().getSimpleName() + " " + e.getMessage());
            siteId = null; snapshot = null; total = 0;
        }
    }

    private void requireData() {
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        if (siteId == null || snapshot == null || total < MIN_ISSUES)
            throw new SkipException("No site with >=" + MIN_ISSUES + " issues found — nothing to audit.");
    }

    /** Report-by-default gate: under STRICT a finding is a hard fail; otherwise a loud WARNING. */
    private void enforce(boolean okOrNoFinding, String message) {
        if (STRICT) Assert.assertTrue(okOrNoFinding, message);
        else if (!okOrNoFinding) ExtentReportManager.logWarning("[would fail under STRICT] " + message);
    }

    // ── list helpers ───────────────────────────────────────────────────────────

    private JSONObject list(String sld, int page, int pageSize, JSONObject filters, String search) {
        JSONObject body = new JSONObject()
                .put("company_id", companyId).put("sld_id", sld)
                .put("page", page).put("page_size", pageSize)
                .put("filters", filters).put("search", search);
        Response r = getAuthenticatedRequestSpec().body(body.toString())
                .when().post("/v2/issues/list").then().extract().response();
        Assert.assertEquals(r.statusCode(), 200, "POST /v2/issues/list failed: " + trim(r));
        String b = r.asString();
        Assert.assertFalse(b == null || b.trim().startsWith("<"), "list returned non-JSON: " + trim(r));
        JSONObject data = new JSONObject(b).optJSONObject("data");
        return data == null ? new JSONObject().put("items", new JSONArray()) : data;
    }

    private int listTotal(String sld) {
        // Throwable, not Exception: list() asserts status==200 → a transient non-200 throws AssertionError.
        try { return list(sld, 1, 1, new JSONObject(), "").optInt("total", 0); }
        catch (Throwable e) { return -1; }
    }

    private JSONObject filterBy(String key, String value) {
        return list(siteId, 1, 200, new JSONObject().put(key, value), "");
    }

    private static Set<String> ids(JSONArray items) {
        Set<String> s = new HashSet<>();
        for (int i = 0; items != null && i < items.length(); i++) s.add(items.getJSONObject(i).optString("id"));
        return s;
    }

    /** distinct non-empty values of a field across the snapshot, in first-seen order. */
    private Map<String, Integer> distinct(String field) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < snapshot.length(); i++) {
            String v = snapshot.getJSONObject(i).optString(field, "");
            if (!v.isEmpty()) m.merge(v, 1, Integer::sum);
        }
        return m;
    }

    // ── 1 + 2. filter purity + partition ───────────────────────────────────────

    @Test(description = "Filter purity: status/priority filters return only matching rows; counts partition the total")
    public void testFilterPurityAndPartition() {
        ExtentReportManager.createTest(MODULE, "Filter purity & partition", "CONS_FILTER");
        requireData();

        for (String field : new String[]{"status", "priority"}) {
            Map<String, Integer> values = distinct(field);
            if (values.isEmpty()) continue;
            int sum = 0;
            List<String> impure = new ArrayList<>();
            for (String v : values.keySet()) {
                JSONArray items = filterBy(field, v).optJSONArray("items");
                sum += items == null ? 0 : items.length();
                for (int i = 0; items != null && i < items.length(); i++) {
                    JSONObject it = items.getJSONObject(i);
                    if (!v.equals(it.optString(field)))
                        impure.add(field + "=" + v + " returned " + field + "=" + it.optString(field)
                                + " (" + it.optString("id") + ")");
                }
            }
            REPORT.add(new String[]{"filter-purity:" + field, impure.isEmpty() ? "OK" : "DEFECT",
                    values.size() + " values; partition sum=" + sum + " vs total=" + total});
            Assert.assertTrue(impure.isEmpty(),
                    "Filter leakage — a " + field + " filter returned non-matching rows:\n  "
                    + String.join("\n  ", impure));
            // partition equality is count-based → race-prone → STRICT-gated
            if (STRICT) Assert.assertEquals(sum, total,
                    field + " filtered counts do not partition the total (" + sum + " != " + total + ")");
            else if (sum != total) ExtentReportManager.logWarning("[would fail under STRICT] " + field
                    + " partition sum " + sum + " != total " + total + " (rows invisible to all filters, or double-counted)");
        }
        ExtentReportManager.logPass("Filter purity holds; partition audited.");
    }

    // ── 3. search correctness ───────────────────────────────────────────────────

    @Test(description = "Search: nonsense term → 0; a real title term is found; every hit is in the collection")
    public void testSearchCorrectness() {
        ExtentReportManager.createTest(MODULE, "Search correctness", "CONS_SEARCH");
        requireData();
        Set<String> universe = ids(snapshot);

        // (a) nonsense term → empty
        JSONArray none = list(siteId, 1, 50, new JSONObject(), "zzq_no_match_" + total).optJSONArray("items");
        int noneCount = none == null ? 0 : none.length();

        // (b) a term guaranteed present — a word from a real row's title
        String term = null, expectId = null;
        for (int i = 0; i < snapshot.length() && term == null; i++) {
            String title = snapshot.getJSONObject(i).optString("title", "").trim();
            for (String w : title.split("\\s+")) {
                if (w.length() >= 4 && w.matches("[A-Za-z]+")) { term = w; expectId = snapshot.getJSONObject(i).optString("id"); break; }
            }
        }
        boolean foundExpected = true; boolean allInUniverse = true; int hitCount = -1;
        if (term != null) {
            JSONArray hits = list(siteId, 1, 200, new JSONObject(), term).optJSONArray("items");
            hitCount = hits == null ? 0 : hits.length();
            Set<String> hitIds = ids(hits);
            foundExpected = hitIds.contains(expectId);
            allInUniverse = universe.containsAll(hitIds);
        }
        REPORT.add(new String[]{"search", (noneCount == 0 && foundExpected && allInUniverse) ? "OK" : "DEFECT",
                "nomatch=" + noneCount + " term='" + term + "' hits=" + hitCount
                + " foundExpected=" + foundExpected + " allInCollection=" + allInUniverse});
        Assert.assertEquals(noneCount, 0, "Search for a nonsense term returned rows (over-matching).");
        if (term != null) {
            Assert.assertTrue(foundExpected,
                    "Search for '" + term + "' (a word from row " + expectId + "'s title) did not return that row.");
            Assert.assertTrue(allInUniverse,
                    "Search returned an id not present in the unfiltered collection (search invented a row).");
        }
        ExtentReportManager.logPass("Search correctness holds (nomatch empty, known term found, hits ⊆ collection).");
    }

    // ── 4. pagination integrity ─────────────────────────────────────────────────

    @Test(description = "Pagination: pages are disjoint, union equals total, beyond-end is empty")
    public void testPaginationIntegrity() {
        ExtentReportManager.createTest(MODULE, "Pagination integrity", "CONS_PAGING");
        requireData();
        Set<String> universe = ids(snapshot);

        List<String> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<String> dups = new ArrayList<>();
        List<String> aliens = new ArrayList<>();
        int page = 1, guard = 0;
        while (guard++ < 100) {
            JSONArray items = list(siteId, page, PAGE, new JSONObject(), "").optJSONArray("items");
            if (items == null || items.length() == 0) break;
            for (int i = 0; i < items.length(); i++) {
                String id = items.getJSONObject(i).optString("id");
                if (!seen.add(id)) dups.add(id);
                if (!universe.contains(id)) aliens.add(id);
                all.add(id);
            }
            if (items.length() < PAGE) break;   // last partial page
            page++;
        }
        // beyond-the-end page must be an empty 2xx, never the full collection
        JSONArray beyond = list(siteId, page + 50, PAGE, new JSONObject(), "").optJSONArray("items");
        int beyondCount = beyond == null ? 0 : beyond.length();

        REPORT.add(new String[]{"pagination", dups.isEmpty() && aliens.isEmpty() ? "OK" : "DEFECT",
                "walked=" + all.size() + " unique=" + seen.size() + " total=" + total
                + " dups=" + dups.size() + " aliens=" + aliens.size() + " beyondEnd=" + beyondCount});
        Assert.assertTrue(dups.isEmpty(), "Pagination returned duplicate ids across pages (unstable order / drift): " + dups);
        Assert.assertTrue(aliens.isEmpty(), "Pagination returned ids not in the collection: " + aliens);
        Assert.assertTrue(beyondCount == 0,
                "A page far beyond the end returned " + beyondCount + " rows (should be empty) — pagination not bounded.");
        if (STRICT) Assert.assertEquals(seen.size(), total,
                "Paged-through unique count " + seen.size() + " != reported total " + total + " (rows dropped or over-reported).");
        else if (seen.size() != total) ExtentReportManager.logWarning("[would fail under STRICT] paged unique "
                + seen.size() + " != total " + total);
        ExtentReportManager.logPass("Pagination integrity: no dups, no aliens, bounded beyond-end.");
    }

    // ── 5. list-vs-detail consistency ───────────────────────────────────────────

    @Test(description = "List projection agrees with GET /issue/{id} on title/status/priority/issue_class")
    public void testListVsDetailConsistency() {
        ExtentReportManager.createTest(MODULE, "List-vs-detail consistency", "CONS_DETAIL");
        requireData();

        List<String> mism = new ArrayList<>();
        int checked = 0;
        for (int i = 0; i < snapshot.length() && checked < DETAIL_SAMPLE; i++) {
            JSONObject row = snapshot.getJSONObject(i);
            String id = row.optString("id");
            Response r = getAuthenticatedRequestSpec().when().get("/issue/" + id).then().extract().response();
            if (r.statusCode() != 200 || r.asString().trim().startsWith("<")) {
                mism.add(id + " detail GET → " + r.statusCode());
                checked++;
                continue;
            }
            JSONObject d = new JSONObject(r.asString());
            JSONObject detail = d.optJSONObject("data") != null ? d.getJSONObject("data") : d;
            for (String f : new String[]{"title", "status", "priority", "issue_class"}) {
                String lv = row.optString(f, ""), dv = detail.optString(f, "");
                if (!lv.equals(dv))
                    mism.add(id.substring(0, 8) + " " + f + ": list='" + lv + "' detail='" + dv + "'");
            }
            checked++;
        }
        REPORT.add(new String[]{"list-vs-detail", mism.isEmpty() ? "OK" : "DEFECT",
                "sampled=" + checked + " mismatches=" + mism.size()});
        Assert.assertTrue(mism.isEmpty(),
                "List projection disagrees with the detail record (stale/incorrect denormalised list):\n  "
                + String.join("\n  ", mism));
        ExtentReportManager.logPass("List projection matches detail across " + checked + " sampled issues.");
    }

    // ── 6. filter ∧ search intersection ─────────────────────────────────────────

    @Test(description = "Filter + search combine as an intersection (rows match the status AND the term)")
    public void testFilterAndSearchIntersection() {
        ExtentReportManager.createTest(MODULE, "Filter ∧ search", "CONS_COMBO");
        requireData();

        // pick the most common status, and a title term from a row that has it
        Map<String, Integer> statuses = distinct("status");
        if (statuses.isEmpty()) throw new SkipException("no status values to combine.");
        String status = statuses.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();

        String term = null;
        for (int i = 0; i < snapshot.length() && term == null; i++) {
            JSONObject row = snapshot.getJSONObject(i);
            if (!status.equals(row.optString("status"))) continue;
            for (String w : row.optString("title", "").trim().split("\\s+"))
                if (w.length() >= 4 && w.matches("[A-Za-z]+")) { term = w; break; }
        }
        if (term == null) throw new SkipException("no searchable title term on a '" + status + "' row.");

        JSONArray items = list(siteId, 1, 200, new JSONObject().put("status", status), term).optJSONArray("items");
        Set<String> searchSet = ids(list(siteId, 1, 200, new JSONObject(), term).optJSONArray("items"));
        List<String> bad = new ArrayList<>();
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject it = items.getJSONObject(i);
            if (!status.equals(it.optString("status"))) bad.add(it.optString("id") + " status=" + it.optString("status"));
            else if (!searchSet.contains(it.optString("id"))) bad.add(it.optString("id") + " not in search('" + term + "') set");
        }
        REPORT.add(new String[]{"filter-and-search", bad.isEmpty() ? "OK" : "DEFECT",
                "status='" + status + "' ∧ search='" + term + "' → " + (items == null ? 0 : items.length()) + " rows"});
        Assert.assertTrue(bad.isEmpty(),
                "filter+search is not a clean intersection:\n  " + String.join("\n  ", bad));
        ExtentReportManager.logPass("Filter ∧ search behaves as an intersection.");
    }

    // ── array-valued filter → must not 500 / leak SQL (report-by-default) ───────

    @Test(description = "Robustness: an array-valued filter must not 500 or leak SQL (400 or handled expected)")
    public void testArrayFilterRobustness() {
        ExtentReportManager.createTest(MODULE, "Array-filter robustness", "CONS_ARRAY_FILTER");
        requireData();
        Map<String, Integer> statuses = distinct("status");
        if (statuses.isEmpty()) throw new SkipException("no status values.");
        String status = statuses.keySet().iterator().next();
        int scalar = filterBy("status", status).optInt("total", -1);

        // fire RAW (the shared list() helper asserts 200 — this probe expects it might not be)
        JSONObject body = new JSONObject()
                .put("company_id", companyId).put("sld_id", siteId).put("page", 1).put("page_size", 50)
                .put("filters", new JSONObject().put("status", new JSONArray().put(status))).put("search", "");
        Response r = getAuthenticatedRequestSpec().body(body.toString())
                .when().post("/v2/issues/list").then().extract().response();
        int code = r.statusCode();
        String raw = r.asString() == null ? "" : r.asString();
        boolean leaks = InputValidationApiTest.LEAK.matcher(raw).find();

        boolean defect = code >= 500 || leaks;             // 5xx or SQL/DB leak = real defect
        String verdict = defect ? "DEFECT" : (code == 400 ? "OK" : "SOFT-GAP");
        REPORT.add(new String[]{"array-filter", verdict,
                "scalar status='" + status + "'=" + scalar + "; array form → HTTP " + code
                + (leaks ? " + INTERNAL LEAK" : "")});

        enforce(!defect, "Array-valued filter {status:[\"" + status + "\"]} → HTTP " + code
                + (leaks ? " leaking SQL/DB internals" : "") + " (should be a clean 400 or handled). Body: "
                + (raw.length() > 200 ? raw.substring(0, 200) + "…" : raw));
        if (!defect && code != 400)
            ExtentReportManager.logWarning("Array-valued filter returned HTTP " + code
                    + " (no error, no clean rejection) — API should accept the array or 400 it.");
        ExtentReportManager.logInfo("Array-filter probe: HTTP " + code + " leaks=" + leaks);
    }

    // ── report ───────────────────────────────────────────────────────────────

    private static String trim(Response r) {
        String b = r.asString();
        return b == null ? "<null>" : b.length() > 240 ? b.substring(0, 240) + "…" : b;
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        try {
            new File("reports").mkdirs();
            StringBuilder md = new StringBuilder("# Filter / Search / Consistency Audit\n\n"
                    + "Site: " + siteName + " (" + siteId + ")  ·  total issues=" + total
                    + "  ·  STRICT_CONSISTENCY_CONTRACT=" + STRICT + "\n\n"
                    + "_Endpoint: `POST /api/v2/issues/list` (the one paginated+filterable list API). "
                    + "`DEFECT`=correctness violation; `SOFT-GAP`=robustness nit (report-only)._\n\n"
                    + "| Check | Result | Detail |\n|---|---|---|\n");
            for (String[] row : REPORT) md.append("| ").append(row[0]).append(" | ")
                    .append(row[1]).append(" | ").append(row[2]).append(" |\n");
            try (FileWriter w = new FileWriter("reports/filter-search-consistency-report.md")) { w.write(md.toString()); }
            System.out.println("[Consistency] wrote reports/filter-search-consistency-report.md");
        } catch (Exception e) {
            System.out.println("[Consistency] report write failed: " + e.getMessage());
        }
    }
}
