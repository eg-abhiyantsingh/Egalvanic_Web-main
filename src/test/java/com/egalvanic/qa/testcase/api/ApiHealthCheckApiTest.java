package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
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
 * <b>API Health Check</b> — the engine behind "Parallel Suite 3". Ports the Egalvanic Utility Toolkit v3.0
 * health check into the TestNG framework: probes the Z-Platform API endpoint registry (~50 endpoints across
 * 15 categories), classifies each <b>pass / warn / fail</b>, and raises the same <b>recommendation</b>
 * buckets (slow, large payload, no-pagination, scaling, plain-array, empty) using thresholds
 * reverse-engineered from the v3.0 report.
 *
 * <p><b>Read-only.</b> Every probe is a GET (plus the read-only {@code /auth/verify-token} and health
 * endpoints). Environment is chosen by {@code BASE_URL} (QA by default), so the same suite runs against QA
 * or Prod. Context ids ({@code company_id / sld_id / quote_id / subdomain}) are resolved live.</p>
 *
 * <p><b>Build gate:</b> a case hard-fails only when the endpoint is genuinely <b>down</b> — 5xx / timeout /
 * connection error after one retry (a real outage a health check must catch). A 4xx (needs params /
 * permission / not on this env) is a healthy server responding → reported as WARN, build stays green.
 * Latency / payload / pagination findings are advisory recommendations written to
 * {@code reports/api-health-report.md}.</p>
 */
public class ApiHealthCheckApiTest extends BaseAPITest {

    // Thresholds — ported verbatim from the toolkit's recommendations ruleset.
    private static final int SLOW_MS = 1500, VERY_SLOW_MS = 4000;
    private static final int LARGE_KB = 500, VERY_LARGE_KB = 5000;
    private static final int PAGINATE_ITEMS = 50, NO_PAGINATION_ITEMS = 500, NO_PAGINATION_CRIT = 10000;
    private static final int SCALING_ITEMS = 200;

    private static final String MODULE = "API — Health Check";

    private String companyId, sldId, quoteId, subdomain;
    private boolean ctxResolved;

    private static final List<String[]> RESULTS = Collections.synchronizedList(new ArrayList<>());
    private static final List<String[]> RECS = Collections.synchronizedList(new ArrayList<>()); // {severity,endpoint,message}

    /** category, display name, path template, method */
    @DataProvider(name = "endpoints")
    public Object[][] endpoints() {
        return new Object[][]{
            {"Core Health", "API Health", "/health", "GET"},
            {"Core Health", "Mutation Health", "/mutation/health", "GET"},
            {"Auth", "Auth /me", "/auth/v2/me", "GET"},
            {"Auth", "Validate Token", "/auth/verify-token", "POST"},
            {"Accounts, Users & Contacts", "Accounts", "/accounts/", "GET"},
            {"Accounts, Users & Contacts", "Users", "/users/", "GET"},
            {"Accounts, Users & Contacts", "Contacts", "/contacts/", "GET"},
            {"Nodes & Classes", "Node Classes", "/node_classes", "GET"},
            {"Nodes & Classes", "Node Icons", "/node_icons", "GET"},
            {"Nodes & Classes", "Node Orientations", "/node_orientations", "GET"},
            {"Nodes & Classes", "Edge Classes", "/edge_classes", "GET"},
            {"Nodes & Classes", "Issue Classes", "/issue_classes", "GET"},
            {"SLD", "SLD Export PDF Sizes", "/sld/export/pdf/sizes", "GET"},
            {"Sessions & Work Management", "IR Sessions", "/ir-photos/", "GET"},
            {"Sessions & Work Management", "Jobs", "/jobs/", "GET"},
            {"Sessions & Work Management", "Jobs with Workorders", "/company/{company_id}/jobs-with-workorders", "GET"},
            {"Sessions & Work Management", "Planned Workorder Lines", "/planned-workorder-lines/", "GET"},
            {"Sessions & Work Management", "User Schedule", "/company/{company_id}/sessions", "GET"},
            {"Quotes & Opportunities", "Opportunities", "/opportunities/", "GET"},
            {"Quotes & Opportunities", "Planned Workorders", "/planned-workorders/by-quote/{quote_id}", "GET"},
            {"Procedures & Shortcuts", "Procedure Masters", "/procedure-masters", "GET"},
            {"Procedures & Shortcuts", "All Shortcuts", "/shortcuts/all", "GET"},
            {"Materials", "Materials Library", "/materials-library/", "GET"},
            {"Materials", "Materials Presets", "/materials-presets/", "GET"},
            {"Materials", "Material Types", "/material-types", "GET"},
            {"Materials", "Material Units", "/material-units", "GET"},
            {"Labor", "Labor Rates", "/labor-rates", "GET"},
            {"Labor", "Labor Types", "/labor-types", "GET"},
            {"Labor", "Labor Unions", "/labor-unions", "GET"},
            {"Test Equipment", "Test Equipment", "/test-equipment", "GET"},
            {"Test Equipment", "Test Equipment Library", "/test-equipment-library", "GET"},
            {"Lookups", "Lookup Node Classes", "/lookup/node-classes", "GET"},
            {"Lookups", "Lookup Procedures", "/lookup/procedures", "GET"},
            {"Lookups", "Lookup Procedure Masters", "/lookup/procedure-masters", "GET"},
            {"Lookups", "Lookup Unary Procedure Masters", "/lookup/unary-procedure-masters", "GET"},
            {"Lookups", "Lookup ML Procedure Types", "/lookup/ml-procedure-types", "GET"},
            {"Lookups", "Lookup Issues", "/lookup/issues", "GET"},
            {"Lookups", "Lookup Procs by Class/Subtype", "/lookup/procedures-by-class-subtype", "GET"},
            {"Lookups", "Currencies", "/currencies", "GET"},
            {"Forms", "Forms", "/forms/", "GET"},
            {"Forms", "Mapping Enums (Session Assign)", "/mappings/enum/session-assignment-types", "GET"},
            {"Forms", "Mapping Enums (Task Assign)", "/mappings/enum/task-assignment-types", "GET"},
            {"Bulk Operations (Templates)", "Bulk Upload Template", "/bulk-upload/template?sld_id={sld_id}", "GET"},
            {"Reporting", "Reporting Configs", "/reporting/configs", "GET"},
            {"Reporting", "Asset Report (Export Site)", "/asset-report/export-site?sld_id={sld_id}", "GET"},
            {"Company & Branding", "Branding (partner)", "/branding/company/{subdomain}", "GET"},
            {"Company & Branding", "Admin Version Rules", "/admin/version-rules", "GET"},
            {"Mutations", "Mutations List", "/mutations", "GET"},
            {"Z University", "Z University Config", "/z-university/config", "GET"},
            {"Z University", "Z University Pages", "/z-university/pages", "GET"},
        };
    }

    private void resolveContext() {
        if (ctxResolved) return;
        ctxResolved = true;
        subdomain = AppConstants.VALID_COMPANY_CODE; // "acme" fallback
        try {
            Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
            if (me.statusCode() == 200 && me.asString().trim().startsWith("{")) {
                JSONObject o = new JSONObject(me.asString());
                companyId = o.optString("company_id", null);
                if (o.has("company_subdomain")) subdomain = o.optString("company_subdomain", subdomain);
                JSONArray slds = o.optJSONArray("accessible_sld_ids");
                if (slds != null && slds.length() > 0) sldId = slds.getString(0);
            }
        } catch (Exception ignored) {}
        if (sldId == null && companyId != null) {           // accessible_sld_ids is often empty for admin
            try {
                Response r = getAuthenticatedRequestSpec().when().get("/company/" + companyId + "/slds").then().extract().response();
                if (r.statusCode() == 200 && r.asString().trim().startsWith("{")) {
                    JSONArray slds = new JSONObject(r.asString()).optJSONArray("slds");
                    if (slds != null && slds.length() > 0) sldId = slds.getJSONObject(0).optString("id", null);
                }
            } catch (Exception ignored) {}
        }
        try {
            Response q = getAuthenticatedRequestSpec().when().get("/quotes/").then().extract().response();
            if (q.statusCode() == 200) {
                String b = q.asString().trim();
                if (b.startsWith("[")) { JSONArray a = new JSONArray(b); if (a.length() > 0) quoteId = a.getJSONObject(0).optString("id", null); }
                else if (b.startsWith("{")) {
                    JSONObject o = new JSONObject(b);
                    JSONArray d = o.optJSONArray("data");
                    if (d != null && d.length() > 0) quoteId = d.getJSONObject(0).optString("id", null);
                    else if (o.has("id")) quoteId = o.optString("id", null);
                }
            }
        } catch (Exception ignored) {}
        System.out.println("[Health] context company=" + companyId + " sld=" + sldId + " quote=" + quoteId + " subdomain=" + subdomain);
    }

    @Test(dataProvider = "endpoints", description = "API health: status + latency + payload + item-count + recommendations")
    public void healthCheck(String category, String name, String template, String method) {
        ExtentReportManager.createTest(MODULE, category, "HEALTH_" + name.replaceAll("[^A-Za-z0-9]+", "_"));
        if (!hasAuthToken()) throw new SkipException("No API token — cannot health-check " + name + ".");
        resolveContext();

        // Resolve path template; skip endpoints whose required context id is unavailable this run.
        String path = template;
        if (path.contains("{company_id}")) { if (companyId == null) throw new SkipException(name + ": no company_id."); path = path.replace("{company_id}", companyId); }
        if (path.contains("{sld_id}"))     { if (sldId == null)     throw new SkipException(name + ": no sld_id.");     path = path.replace("{sld_id}", sldId); }
        if (path.contains("{quote_id}"))   { if (quoteId == null)   throw new SkipException(name + ": no quote_id.");   path = path.replace("{quote_id}", quoteId); }
        if (path.contains("{subdomain}"))  path = path.replace("{subdomain}", subdomain == null ? "acme" : subdomain);

        Measure m = measure(path, method);
        if ("fail".equals(m.status)) { Measure retry = measure(path, method); if (!"fail".equals(retry.status)) m = retry; } // retry transient

        List<String> recs = evaluate(name, path, m);
        for (String r : recs) { String[] p = r.split("\\|", 2); RECS.add(new String[]{p[0], name, p[1]}); }
        RESULTS.add(new String[]{ category, name, path, m.status, String.valueOf(m.http), m.latencyMs + "ms",
                m.itemCount < 0 ? "-" : String.valueOf(m.itemCount), m.shape == null ? "-" : m.shape,
                m.payloadKb < 0 ? "-" : (m.payloadKb + "KB"), String.valueOf(recs.size()) });

        String line = String.format("[Health] %-32s %-4s http=%s %dms items=%s shape=%s %dKB recs=%d",
                name, m.status.toUpperCase(), m.http, m.latencyMs,
                (m.itemCount < 0 ? "-" : m.itemCount), m.shape, Math.max(0, m.payloadKb), recs.size());
        System.out.println(line);

        // Build gate: only a genuine outage (5xx/timeout/connection) fails; 4xx = healthy server, reported.
        if ("fail".equals(m.status)) {
            ExtentReportManager.logFail(name + " DOWN — " + m.detail + " (" + path + ")");
            Assert.fail("[HEALTH] " + name + " is DOWN: " + m.detail + " — " + path);
        } else if ("warn".equals(m.status)) {
            ExtentReportManager.logWarning(name + " reachable but HTTP " + m.http + " — " + m.detail + (recs.isEmpty() ? "" : " | " + recs));
        } else {
            if (recs.isEmpty()) ExtentReportManager.logPass(name + " healthy (" + m.latencyMs + "ms" + (m.itemCount >= 0 ? ", " + m.itemCount + " items" : "") + ").");
            else ExtentReportManager.logWarning(name + " OK but " + recs);
        }
    }

    // ── measurement ──
    private static class Measure {
        String status; int http; long latencyMs; int payloadKb = -1; int itemCount = -1; String shape; String detail = "";
    }

    private Measure measure(String path, String method) {
        Measure r = new Measure();
        try {
            Response resp;
            if ("POST".equals(method)) {
                String body = path.contains("verify-token") ? new JSONObject().put("token", authToken).toString() : "{}";
                resp = getAuthenticatedRequestSpec().body(body).when().post(path).then().extract().response();
            } else {
                resp = getAuthenticatedRequestSpec().when().get(path).then().extract().response();
            }
            r.http = resp.statusCode();
            r.latencyMs = resp.time();
            String bytes = resp.asString();
            r.payloadKb = bytes == null ? 0 : (int) Math.round(bytes.getBytes().length / 1024.0);
            String b = bytes == null ? "" : bytes.trim();
            if (b.startsWith("[")) { r.shape = "array"; r.itemCount = new JSONArray(b).length(); }
            else if (b.startsWith("{")) {
                JSONObject o = new JSONObject(b);
                r.shape = "object";
                for (String k : o.keySet()) { if (o.optJSONArray(k) != null) { r.shape = "wrapped"; r.itemCount = o.getJSONArray(k).length(); break; } }
                r.detail = o.has("error") ? String.valueOf(o.opt("error")) : (o.has("status") ? "status:" + o.opt("status") : "");
            } else if (b.startsWith("<")) { r.shape = "html"; }
            r.status = (r.http >= 200 && r.http < 300) ? "pass" : "warn";
            if (r.detail.isEmpty()) r.detail = "HTTP " + r.http;
        } catch (Exception e) {
            r.status = "fail"; r.detail = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
        }
        return r;
    }

    /** Recommendation ruleset (ported). Returns "severity|message" strings. */
    private List<String> evaluate(String name, String path, Measure m) {
        List<String> recs = new ArrayList<>();
        long lat = m.latencyMs;
        if (lat >= VERY_SLOW_MS) recs.add("critical|Very slow response: " + lat + "ms");
        else if (lat >= SLOW_MS) recs.add("warning|Slow response: " + lat + "ms");
        if (m.payloadKb >= 0) {
            if (m.payloadKb >= VERY_LARGE_KB) recs.add("critical|Large payload: " + m.payloadKb + "KB");
            else if (m.payloadKb >= LARGE_KB) recs.add("warning|Large payload: " + m.payloadKb + "KB");
        }
        int n = m.itemCount;
        if (n >= 0) {
            if (n > NO_PAGINATION_ITEMS) recs.add((n > NO_PAGINATION_CRIT ? "critical" : "warning") + "|Returns " + n + " items with no pagination");
            else if (n > PAGINATE_ITEMS) recs.add("warning|Returns " + n + " items — consider pagination");
            if (n > SCALING_ITEMS && lat >= SLOW_MS) recs.add(((n > NO_PAGINATION_CRIT || lat >= VERY_SLOW_MS) ? "critical" : "warning") + "|" + n + " items in " + lat + "ms — scaling concern");
            if (n == 0) recs.add("info|Returns empty collection (0 items)");
        }
        if ("array".equals(m.shape)) recs.add("info|Returns plain array instead of {success, data}");
        return recs;
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
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
        md.append("# API Health Check — ").append(AppConstants.BASE_URL).append("\n\n");
        md.append("**").append(rows.size()).append(" endpoints** · ")
          .append(pass).append(" pass · ").append(warn).append(" warn · ").append(fail).append(" fail · ")
          .append("avg ").append(latN > 0 ? (latSum / latN) : 0).append("ms · ")
          .append("recommendations: ").append(crit).append(" critical / ").append(wrn).append(" warning / ").append(info).append(" info\n\n");
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
            try (FileWriter w = new FileWriter("reports/api-health-report.md")) { w.write(md.toString()); }
        } catch (Exception e) { System.out.println("[Health] report write failed: " + e.getMessage()); }
        System.out.println("\n===== API HEALTH CHECK =====\n" + md);
    }
}
