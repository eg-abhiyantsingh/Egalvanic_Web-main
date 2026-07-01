package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;

/**
 * <b>Network-driven API coverage</b> — the QA-team ask: "cover API test cases by checking network → then
 * checking response." This drives each module in a real browser, records every <code>/api</code> GET the
 * SPA actually fires (real endpoints, real params, real ids), then <b>replays each call with an auth token
 * and validates the response</b>: status, JSON shape, record count, and response time. Collection endpoints
 * are additionally flagged when they return unbounded (no pagination), complementing the curated deep-dive
 * in {@link com.egalvanic.qa.testcase.api.ListApiContractApiTest}.
 *
 * <p><b>Hard-fails</b> on objective defects only — a captured GET returning 5xx, or slower than
 * {@value #RESP_HARD_MS} ms. Everything else (4xx that need params, unbounded collections, slow-but-ok) is
 * reported to <code>reports/network-api-audit-report.md</code>. This keeps the suite green while surfacing
 * the real API surface for the module-by-module review.</p>
 */
public class NetworkApiContractTestNG extends BaseTest {

    private static final long RESP_HARD_MS = 8000;
    private static final long RESP_WARN_MS = 2500;
    private static final int  UNBOUNDED_AT = 100;   // a collection larger than this with no paging is flagged
    private static final int  MIN_CAPTURED = 8;      // sanity: capture must actually observe traffic

    private static final String MODULE = "API — Network Capture";

    private String apiToken;

    /** Module route → the page whose list/detail API calls we want to observe. */
    private static final String[][] PAGES = {
        {"dashboard","/dashboard"}, {"assets","/assets"}, {"issues","/issues"},
        {"work-orders","/work-orders"}, {"connections","/connections"}, {"locations","/locations"},
        {"tasks","/tasks"}, {"opportunities","/opportunities"}, {"arc-flash","/arc-flash"},
    };

    /** Noise (telemetry / third-party / auth) to exclude from the API contract check. */
    private static final String[] NOISE = {
        "sentry", "beamer", "devrev", "/web/api/", "envelope", "/auth/login",
        "startSession", "addMetadata", "setIdentifier", "user-hash", "session-token", "legal/acceptance"
    };

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    @Override
    @BeforeClass
    public void classSetup() {
        super.classSetup();               // browser login + site selection
        apiToken = apiLogin();            // separate token for authenticated replay
        System.out.println("[NetApi] replay token acquired=" + (apiToken != null));
    }

    @Test(description = "Capture every /api GET the SPA fires across modules, then validate each response")
    public void captureAndValidateNetworkApis() {
        ExtentReportManager.createTest(MODULE, "Network → response", "NetApi_CaptureAndValidate");

        // ── 1) CAPTURE: drive each module, collect the /api GET URLs it fires ──
        Set<String> endpoints = new LinkedHashSet<>();
        for (String[] pg : PAGES) {
            try {
                js().executeScript("performance.clearResourceTimings();");
                driver.get(AppConstants.BASE_URL + pg[1]);
                pause(5500);
                @SuppressWarnings("unchecked")
                List<String> urls = (List<String>) js().executeScript(
                    "return performance.getEntriesByType('resource').map(function(e){return e.name;})"
                  + ".filter(function(u){return u.indexOf('/api/')>=0;});");
                int before = endpoints.size();
                for (String u : urls) {
                    String path = u.replaceFirst("^https?://[^/]+", "");
                    if (!path.startsWith("/api/")) continue;
                    boolean noise = false;
                    for (String n : NOISE) if (path.contains(n)) { noise = true; break; }
                    if (!noise) endpoints.add(path);
                }
                System.out.println("[NetApi] " + pg[0] + ": +" + (endpoints.size() - before) + " new (total " + endpoints.size() + ")");
            } catch (Exception e) {
                System.out.println("[NetApi] capture " + pg[0] + " failed: " + e.getMessage());
            }
        }
        logStep("Captured " + endpoints.size() + " distinct /api endpoints across " + PAGES.length + " modules.");
        Assert.assertTrue(endpoints.size() >= MIN_CAPTURED,
                "Network capture should observe at least " + MIN_CAPTURED + " API calls (got " + endpoints.size() + ").");
        Assert.assertNotNull(apiToken, "Need an API token to validate the captured responses.");

        // ── 2) VALIDATE: replay each captured GET and check the response ──
        RestAssured.baseURI = AppConstants.BASE_URL;
        List<String[]> rows = new ArrayList<>();
        List<String> serverErrors = new ArrayList<>();
        List<String> tooSlow = new ArrayList<>();
        int ok = 0, unbounded = 0;

        for (String path : endpoints) {
            String status = "ERR", shape = "-", flag = "";
            long ms = -1;
            try {
                Response r = given().header("Authorization", "Bearer " + apiToken)
                        .header("Accept", "application/json")
                        .when().get(path).then().extract().response();
                ms = r.time();
                int code = r.statusCode();
                status = String.valueOf(code);
                String body = r.asString() == null ? "" : r.asString().trim();

                int count = -1; boolean jsonList = false;
                if (body.startsWith("[")) { jsonList = true; count = new JSONArray(body).length(); }
                else if (body.startsWith("{")) {
                    JSONObject o = new JSONObject(body);
                    for (String k : o.keySet()) {
                        if (o.optJSONArray(k) != null) { jsonList = true; count = o.getJSONArray(k).length(); break; }
                    }
                }
                shape = jsonList ? ("list[" + count + "]") : (body.startsWith("{") ? "object" : (body.startsWith("<") ? "HTML" : "other"));

                if (code >= 500) serverErrors.add(path + " → " + code);
                else if (code < 400) ok++;
                if (ms > RESP_HARD_MS) tooSlow.add(path + " → " + ms + "ms");
                if (jsonList && count > UNBOUNDED_AT) { unbounded++; flag = "UNBOUNDED(" + count + ")"; }
                else if (ms > RESP_WARN_MS) flag = "slow";
            } catch (Exception e) {
                serverErrors.add(path + " → exception " + e.getMessage());
            }
            rows.add(new String[]{ path, status, (ms < 0 ? "-" : ms + "ms"), shape, flag });
        }

        writeReport(endpoints.size(), ok, unbounded, rows, serverErrors, tooSlow);

        // ── 3) ASSERT: objective defects fail; policy/unbounded are reported ──
        if (!serverErrors.isEmpty()) {
            ExtentReportManager.logFail("Captured GET(s) returned 5xx / errored: " + serverErrors);
        }
        Assert.assertTrue(serverErrors.isEmpty(),
                "These live API GETs returned a server error (5xx): " + serverErrors);
        Assert.assertTrue(tooSlow.isEmpty(),
                "These live API GETs exceeded " + RESP_HARD_MS + "ms: " + tooSlow);

        if (unbounded > 0) {
            ExtentReportManager.logWarning(unbounded + " captured collection endpoint(s) return > " + UNBOUNDED_AT
                    + " records unbounded — see reports/network-api-audit-report.md (candidates for pagination).");
        }
        ExtentReportManager.logPass("Validated " + endpoints.size() + " live API endpoints: " + ok
                + " OK, 0 server errors, " + unbounded + " unbounded collection(s) flagged.");
    }

    private void writeReport(int total, int ok, int unbounded, List<String[]> rows,
                             List<String> errors, List<String> slow) {
        StringBuilder md = new StringBuilder();
        md.append("# Network-Captured API Audit\n\n");
        md.append("Endpoints the web app actually calls, captured by driving each module in the browser, then ")
          .append("replayed and validated. **").append(total).append("** endpoints · **").append(ok)
          .append("** OK · **").append(errors.size()).append("** server errors · **").append(unbounded)
          .append("** unbounded collections · **").append(slow.size()).append("** over ").append(RESP_HARD_MS).append("ms.\n\n");
        md.append("| Endpoint | Status | Response | Shape | Flag |\n|---|---|---|---|---|\n");
        rows.sort((a, b) -> a[0].compareTo(b[0]));
        for (String[] r : rows) md.append("| `").append(r[0]).append("` | ").append(r[1]).append(" | ")
                .append(r[2]).append(" | ").append(r[3]).append(" | ").append(r[4]).append(" |\n");
        if (!errors.isEmpty()) md.append("\n**Server errors:** ").append(String.join("; ", errors)).append("\n");
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/network-api-audit-report.md")) { w.write(md.toString()); }
            System.out.println("[NetApi] report → reports/network-api-audit-report.md");
        } catch (Exception e) { System.out.println("[NetApi] report write failed: " + e.getMessage()); }
        System.out.println("\n===== NETWORK API AUDIT =====\n" + md);
    }

    /** Fresh API login (mirrors BaseAPITest) for authenticated replay of captured GETs. */
    private String apiLogin() {
        try {
            JSONObject body = new JSONObject()
                    .put("email", AppConstants.VALID_EMAIL)
                    .put("password", AppConstants.VALID_PASSWORD)
                    .put("subdomain", AppConstants.VALID_COMPANY_CODE);
            Response r = given().baseUri(AppConstants.BASE_URL + "/api")
                    .contentType(ContentType.JSON).body(body.toString())
                    .when().post("/auth/login").then().extract().response();
            if (r.statusCode() == 200 && !r.asString().trim().startsWith("<")) {
                String t = r.jsonPath().getString("access_token");
                if (t == null) t = r.jsonPath().getString("token");
                return t;
            }
        } catch (Exception e) { System.out.println("[NetApi] apiLogin failed: " + e.getMessage()); }
        return null;
    }
}
