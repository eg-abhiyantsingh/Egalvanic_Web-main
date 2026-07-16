package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>All-Pages 502 / 5xx Stability Sweep</b> (Parallel Suite 3).
 *
 * <p>Every app page fires a known set of backing API calls on load. This test sweeps the
 * <b>real per-page endpoints</b> — captured live from the browser's network panel for each route
 * (dashboard, assets, issues, connections, locations, tasks, SLDs, arc-flash, work orders,
 * opportunities, accounts, EMPs, PM-readiness, plus the shell endpoints every page hits) — repeatedly,
 * and flags any <b>502 / 5xx / timeout</b>. This catches the <em>intermittent</em> gateway 502s that a
 * single page load or one-shot health check misses, attributed to the PAGE a real user would see fail.</p>
 *
 * <p>The panel is grounded in captured traffic (not the swagger catalog) so it mirrors exactly what the
 * SPA requests — a 502 here means a real page would have surfaced an error to the user.</p>
 *
 * <p>Report-mode by default (a single blip is tolerated as QA-host jitter; ≥2 incidents surface as a
 * FAIL entry in {@code reports/all-pages-502-report.md} and the consolidated findings dashboard).
 * {@code -DSTRICT_ALL_PAGES=true} hard-fails the build. Read-only; GET except the list POSTs the pages
 * actually use.</p>
 */
public class AllPagesStabilityApiTest extends BaseAPITest {

    private static final String MODULE = "API — All-Pages 502 Sweep";
    private static final int STABILITY_ROUNDS = 3;      // sweeps of the whole panel
    private static final long RESP_HARD_MS = 8000;      // > this = timeout-class incident
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_ALL_PAGES",
                    System.getenv().getOrDefault("STRICT_ALL_PAGES", "false")));

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    private String companyId, siteId, userId, sldId, subdomain;

    @BeforeClass(alwaysRun = true)
    public void resolveContext() {
        RestAssured.baseURI = API_BASE_URL;
        if (!hasAuthToken()) return;
        try {
            Response me = getAuthenticatedRequestSpec().relaxedHTTPSValidation()
                    .when().get("/auth/v2/me").then().extract().response();
            if (me.statusCode() == 200 && !me.asString().trim().startsWith("<")) {
                JSONObject j = new JSONObject(me.asString());
                companyId = j.optString("company_id", null);
                userId = j.optString("sub", null);
                subdomain = j.optString("company_subdomain", "acme");
            }
        } catch (Exception ignored) {}
        // siteId + a sample sldId from the SLD list (proven resolution pattern).
        if (companyId != null) {
            try {
                Response slds = getAuthenticatedRequestSpec().relaxedHTTPSValidation()
                        .when().get("/company/" + companyId + "/slds").then().extract().response();
                if (slds.statusCode() == 200 && !slds.asString().trim().startsWith("<")) {
                    JSONArray arr = new JSONObject(slds.asString()).optJSONArray("slds");
                    if (arr != null && arr.length() > 0) {
                        // Site and SLD are 1:1 on this platform — SLD rows carry no site_id field;
                        // the row's own id IS the siteId the SPA passes to /lookup/nodes/{siteId} etc.
                        // (same site_id→id fallback the proven PaginationBehaviorApiTest uses).
                        sldId = arr.getJSONObject(0).optString("id", null);
                        siteId = arr.getJSONObject(0).optString("site_id", sldId);
                    }
                }
            } catch (Exception ignored) {}
        }
        System.out.println("[AllPages] context company=" + companyId + " site=" + siteId + " user=" + userId);
    }

    /** One panel entry: page label, HTTP method, path (ids substituted), optional JSON body. */
    private static final class Ep {
        final String page, method, path, body;
        Ep(String page, String method, String path, String body) {
            this.page = page; this.method = method; this.path = path; this.body = body;
        }
    }

    /** Build the per-page panel from the live-captured route→endpoint map. */
    private List<Ep> panel() {
        List<Ep> p = new ArrayList<>();
        String sub = subdomain == null ? "acme" : subdomain;
        // ── Shell (fired on EVERY page load) ──
        p.add(new Ep("shell", "GET", "/company/alliance-config/" + sub, null));
        p.add(new Ep("shell", "GET", "/auth/v2/me", null));
        p.add(new Ep("shell", "GET", "/legal/acceptance/check", null));
        p.add(new Ep("shell", "GET", "/action-items/counts", null));
        if (companyId != null) p.add(new Ep("shell", "GET", "/company/" + companyId + "/timezone", null));
        if (userId != null) {
            p.add(new Ep("shell", "GET", "/users/" + userId + "/roles", null));
            p.add(new Ep("shell", "GET", "/users/" + userId + "/slds", null));
        }
        // ── Site-scoped pages (need siteId) ──
        if (siteId != null) {
            p.add(new Ep("Dashboard (Site Overview)", "GET", "/lookup/site-overview/" + siteId + "?company_id=" + companyId, null));
            p.add(new Ep("Dashboard (Site Overview)", "GET", "/sites/" + siteId + "/status?narrative=false", null));
            p.add(new Ep("Assets", "GET", "/lookup/nodes/" + siteId, null));
            p.add(new Ep("Assets", "GET", "/lookup/v2/nodes/" + siteId + "?page=1&page_size=25", null));
            p.add(new Ep("Connections", "GET", "/connections/v2/sld/" + siteId + "?page=1&page_size=25", null));
            p.add(new Ep("Locations", "GET", "/location/sld/" + siteId, null));
            p.add(new Ep("Tasks", "GET", "/tasks/" + siteId, null));
            p.add(new Ep("SLDs", "GET", "/sld/" + siteId + "/views", null));
            p.add(new Ep("SLDs", "GET", "/sld/" + siteId + "/view-mappings", null));
            p.add(new Ep("SLDs", "GET", "/sld/" + siteId + "/graph", null));
            p.add(new Ep("Arc Flash", "GET", "/lookup/nodes/" + siteId + "?include_bus_nodes=true", null));
            p.add(new Ep("PM Readiness", "GET", "/lookup/nodes/" + siteId, null));
        }
        // ── Company-scoped pages (need companyId) ──
        if (companyId != null) {
            p.add(new Ep("Work Orders", "GET", "/company/" + companyId + "/sessions?page=1&per_page=5", null));
            p.add(new Ep("Work Orders", "GET", "/company/" + companyId + "/workorders?page=1&per_page=5", null));
            p.add(new Ep("Opportunities", "POST", "/company/" + companyId + "/opportunities/v2", "{\"page\":1,\"page_size\":25}"));
            p.add(new Ep("Accounts", "POST", "/account/by-company/" + companyId + "/v2", "{\"page\":1,\"page_size\":25}"));
            p.add(new Ep("EMPs", "POST", "/company/" + companyId + "/committed-quotes/v2", "{\"page\":1,\"page_size\":25}"));
            p.add(new Ep("SLD list", "GET", "/company/" + companyId + "/slds", null));
        }
        // ── POST list endpoints the pages use ──
        p.add(new Ep("Issues", "POST", "/v2/issues/list", "{\"page\":1,\"page_size\":25}"));
        p.add(new Ep("PM Readiness", "POST", "/lookup/class-availability", "{}"));
        // ── Per-diagram SLD load (the heaviest, most 502-prone path) ──
        if (sldId != null) {
            p.add(new Ep("SLD view (v3)", "GET", "/sld/v3/" + sldId, null));
            p.add(new Ep("SLD view (v2)", "GET", "/sld/v2/" + sldId, null));
        }
        return p;
    }

    @Test(description = "Sweep every app page's backing API endpoints repeatedly; flag any 502/5xx/timeout per page")
    public void allPagesStabilitySweep() {
        ExtentReportManager.createTest(MODULE, "502 sweep", "ALL_PAGES_502");
        if (!hasAuthToken()) throw new SkipException("No API auth token — cannot sweep pages.");

        List<Ep> panel = panel();
        if (panel.isEmpty()) throw new SkipException("Could not resolve context — empty panel.");

        // page → incident detail(s); tolerate a single blip, escalate on ≥2 (report-mode).
        Map<String, List<String>> incidentsByPage = new LinkedHashMap<>();
        int total5xx = 0, total502 = 0, calls = 0;

        for (int round = 1; round <= STABILITY_ROUNDS; round++) {
            for (Ep ep : panel) {
                calls++;
                long t0 = System.currentTimeMillis();
                String incident = null;
                try {
                    Response r;
                    if ("POST".equals(ep.method)) {
                        r = getAuthenticatedRequestSpec().relaxedHTTPSValidation()
                                .contentType(ContentType.JSON).body(ep.body == null ? "{}" : ep.body)
                                .when().post(ep.path).then().extract().response();
                    } else {
                        r = getAuthenticatedRequestSpec().relaxedHTTPSValidation()
                                .when().get(ep.path).then().extract().response();
                    }
                    long ms = System.currentTimeMillis() - t0;
                    int sc = r.statusCode();
                    if (sc == 502) { incident = "502 Bad Gateway in " + ms + "ms"; total502++; total5xx++; }
                    else if (sc >= 500) { incident = sc + " in " + ms + "ms"; total5xx++; }
                    else if (ms > RESP_HARD_MS) { incident = "TIMEOUT-CLASS " + ms + "ms (HTTP " + sc + ")"; }
                } catch (Exception e) {
                    incident = "EXCEPTION " + e.getClass().getSimpleName();
                }
                if (incident != null) {
                    String detail = "round" + round + " " + ep.method + " " + ep.path + " → " + incident;
                    incidentsByPage.computeIfAbsent(ep.page, k -> new ArrayList<>()).add(detail);
                    System.out.println("[AllPages][incident] [" + ep.page + "] " + detail);
                }
            }
        }

        int pagesAffected = incidentsByPage.size();
        int totalIncidents = incidentsByPage.values().stream().mapToInt(List::size).sum();
        for (Map.Entry<String, List<String>> e : incidentsByPage.entrySet()) {
            REPORT.add(new String[]{ e.getKey(), String.valueOf(e.getValue().size()),
                    String.join(" | ", e.getValue()) });
        }
        String summary = panel.size() + " endpoints x " + STABILITY_ROUNDS + " rounds (" + calls + " calls); "
                + total502 + " × 502, " + total5xx + " × 5xx; " + pagesAffected + " page(s) affected, "
                + totalIncidents + " incident(s).";
        System.out.println("[AllPages] " + summary);

        if (total5xx == 0 && totalIncidents == 0) {
            ExtentReportManager.logPass("All pages stable across the sweep: " + summary);
            return;
        }
        // A single isolated blip on the whole panel = tolerated QA-host jitter.
        if (totalIncidents == 1 && total5xx == 0) {
            ExtentReportManager.logWarning("Single timeout-class blip (tolerated): " + summary);
            return;
        }
        String msg = "Page-load instability — " + summary + " Affected pages: " + incidentsByPage.keySet();
        if (total502 > 0) msg = "502 BAD GATEWAY on page load(s) — " + msg;
        if (STRICT) { ExtentReportManager.logFail(msg); Assert.fail(msg); }
        else { ExtentReportManager.logWarning(msg + "  (reported; -DSTRICT_ALL_PAGES=true enforces)"); }
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        StringBuilder md = new StringBuilder();
        md.append("# All-Pages 502 / 5xx Stability Sweep\n\n");
        md.append("Per-page backing API endpoints (captured live from the SPA's network panel) swept ")
          .append(STABILITY_ROUNDS).append("x for 502/5xx/timeout. An incident here means a real user on ")
          .append("that page would have hit a server error.\n\n");
        List<String[]> rows = new ArrayList<>(REPORT);
        if (rows.isEmpty()) {
            md.append("**No 502/5xx/timeout incidents — all pages stable across the sweep.**\n");
        } else {
            md.append("| Page | Incidents | Detail |\n|---|---|---|\n");
            for (String[] r : rows) md.append("| ").append(String.join(" | ", r)).append(" |\n");
            md.append("\n**").append(rows.size()).append(" page(s) with incidents")
              .append(STRICT ? " (STRICT: enforced)" : " (reported)").append(".**\n");
        }
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/all-pages-502-report.md")) { w.write(md.toString()); }
            System.out.println("[AllPages] Report → reports/all-pages-502-report.md");
        } catch (Exception e) { System.out.println("[AllPages] report write failed: " + e.getMessage()); }
    }
}
