package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <b>API issue EVIDENCE capture.</b> For every issue class the suite tests, this makes the actual live
 * call and records the FULL HTTP transaction — request line, (redacted) request headers/body, response
 * status, key response headers, latency, and a response-body snippet — into
 * {@code reports/api-evidence.json}. The consolidated report renders each as a visual "capture card"
 * (the REST-API equivalent of a screenshot: for an HTTP call the request/response transcript IS the
 * evidence). Report-only: never fails the build; it documents, it doesn't assert.
 *
 * <p>Issue classes captured (each a real, reproducible transaction):</p>
 * <ul>
 *   <li><b>invalid-request / security</b> — malformed path params that reach the DB layer and return
 *       500 with a psycopg2 SQL statement in the body (OWASP API8 information disclosure). Curl-verified.</li>
 *   <li><b>availability / 502</b> — repeated-sample the critical path panel; capture any 5xx/502.</li>
 *   <li><b>performance</b> — the heaviest endpoints, with observed latency.</li>
 *   <li><b>pagination</b> — large collections that ignore page/per_page and dump everything uncapped.</li>
 *   <li><b>spa-fallback</b> — an API path with a nonexistent id returning the SPA shell (200 HTML)
 *       instead of a JSON 404.</li>
 *   <li><b>auth-gate</b> — an unauthenticated mutation correctly rejected 401.</li>
 *   <li><b>agent-token boundary</b> — an /agents/* endpoint rejecting an ordinary user token.</li>
 * </ul>
 */
public class ApiEvidenceCaptureApiTest extends BaseAPITest {

    private static final String MODULE = "API — Evidence Capture";
    private static final RestAssuredConfig CFG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 30000));
    private static final String ZERO = "00000000-0000-0000-0000-000000000000";

    private static final JSONArray EVIDENCE = new JSONArray();

    @BeforeClass(alwaysRun = true)
    public void base() { RestAssured.baseURI = API_BASE_URL; }

    // ── capture one transaction ──────────────────────────────────────────────
    private void capture(String category, String severity, String note,
                         String method, String path, boolean authed, String body) {
        JSONObject ev = new JSONObject();
        ev.put("category", category).put("severity", severity).put("note", note)
          .put("method", method).put("url", API_BASE_URL + path)
          .put("auth", authed ? "Bearer <redacted>" : "(none)")
          .put("requestBody", body == null ? "" : body);
        long t0 = System.currentTimeMillis();
        try {
            RequestSpecification spec = (authed ? getAuthenticatedRequestSpec() : getRequestSpec())
                    .config(CFG).relaxedHTTPSValidation();
            if (body != null) spec = spec.body(body);
            Response r = spec.when().request(method, path).then().extract().response();
            recordResponse(ev, r);
        } catch (Exception e) {
            ev.put("status", 0).put("latencyMs", System.currentTimeMillis() - t0)
              .put("shape", "TRANSPORT ERROR").put("leak", false)
              .put("respSnippet", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        emit(ev, category, method, path);
    }

    /** Fill the transaction fields from an already-obtained response (used by repeated-sampling). */
    private void recordResponse(JSONObject ev, Response r) {
        String rb = r.asString(); rb = rb == null ? "" : rb;
        String shape = rb.trim().startsWith("[") ? "JSON array" : rb.trim().startsWith("{") ? "JSON object"
                : rb.trim().startsWith("<") ? "HTML (SPA shell)" : "text";
        boolean leak = rb.toLowerCase(Locale.ROOT).matches("(?s).*(psycopg2|sqlalchemy|traceback \\(most recent|programmingerror).*");
        ev.put("status", r.statusCode()).put("latencyMs", r.time())
          .put("contentType", String.valueOf(r.getContentType())).put("shape", shape).put("leak", leak)
          .put("respSnippet", rb.length() > 700 ? rb.substring(0, 700) + " …[truncated]" : rb);
    }

    private void emit(JSONObject ev, String category, String method, String path) {
        EVIDENCE.put(ev);
        System.out.println("[Evidence] " + category + " " + method + " " + path + " -> "
                + ev.optInt("status") + " (" + ev.optString("shape") + ")");
        ExtentReportManager.createTest(MODULE, category, method + " " + path);
        ExtentReportManager.logInfo("Captured " + method + " " + path + " → HTTP " + ev.optInt("status")
                + " " + ev.optString("shape") + (ev.optBoolean("leak") ? " [INTERNALS LEAK]" : ""));
    }

    @Test(description = "Invalid-request handling: malformed path params must not 500 / leak SQL internals")
    public void captureInvalidRequestEvidence() {
        // Verified live: these return 500 with a psycopg2 SQL statement — OWASP API8 info disclosure.
        capture("invalid-request", "critical", "Malformed UUID path param reaches the DB → 500 + psycopg2 SQL leak",
                "GET", "/account/by-company/abc", true, null);
        capture("invalid-request", "critical", "Negative id → 500 + psycopg2 SQL leak",
                "GET", "/company/-1/slds", true, null);
        capture("invalid-request", "critical", "Malformed id on contacts-by-sld → 500 + psycopg2 SQL leak",
                "GET", "/contact/by-sld/abc", true, null);
        capture("invalid-request", "high", "Array-valued filter on issues list → 500 SQL leak (scalar expected)",
                "POST", "/v2/issues/list", true, "{\"filters\":{\"status\":[\"Open\"]},\"page\":1,\"page_size\":10}");
    }

    @Test(description = "Availability: repeated-sample critical paths, capture any 5xx / 502")
    public void captureAvailabilityEvidence() {
        String[] paths = {"/mutations", "/opportunity/", "/account/", "/attachment/", "/reporting/configs"};
        for (String p : paths) {
            for (int i = 1; i <= 3; i++) {
                JSONObject ev = new JSONObject();
                ev.put("category", "availability").put("method", "GET").put("url", API_BASE_URL + p).put("auth", "Bearer <redacted>").put("requestBody", "");
                try {
                    Response r = getAuthenticatedRequestSpec().config(CFG).relaxedHTTPSValidation()
                            .when().get(p).then().extract().response();
                    boolean bad = r.statusCode() >= 500 || r.time() >= 8000;
                    if (bad || i == 3) {
                        recordResponse(ev, r);
                        ev.put("severity", r.statusCode() >= 500 ? "critical" : (r.time() >= 8000 ? "warning" : "info"))
                          .put("note", bad ? ("Degraded on sample " + i + "/3: HTTP " + r.statusCode() + " in " + r.time() + "ms")
                                           : "Stable across 3 samples (no 5xx/502, < 8s)");
                        emit(ev, "availability", "GET", p);
                        break;
                    }
                } catch (Exception e) {
                    ev.put("status", 0).put("latencyMs", 0).put("shape", "TRANSPORT ERROR").put("leak", false)
                      .put("severity", "critical").put("note", "Transport failure sample " + i + "/3")
                      .put("respSnippet", e.getClass().getSimpleName() + ": " + e.getMessage());
                    emit(ev, "availability", "GET", p);
                    break;
                }
            }
        }
    }

    @Test(description = "Performance: capture the heaviest endpoints with observed latency")
    public void capturePerformanceEvidence() {
        // /planned_workorder_line/ is the ticket's slow endpoint (read-timeout class); capture live.
        capture("performance", "critical", "Heavy unpaginated collection — the 15s+ endpoint from the pagination ticket",
                "GET", "/planned_workorder_line/", true, null);
        capture("performance", "warning", "Large mutation ledger", "GET", "/mutations", true, null);
    }

    @Test(description = "Pagination: capture large collections that ignore page/per_page and return everything")
    public void capturePaginationEvidence() {
        capture("pagination", "high", "Ignores per_page — returns the whole collection uncapped",
                "GET", "/eqp-lib/manufacturers?page=1&per_page=1", true, null);
        capture("pagination", "high", "Ignores per_page — returns all rows",
                "GET", "/attachment/?page=1&per_page=1", true, null);
        capture("pagination", "high", "Ignores per_page — returns all opportunities",
                "GET", "/opportunity/?page=1&per_page=1", true, null);
    }

    @Test(description = "SPA fallback: an API path with a nonexistent id returns the app shell instead of JSON 404")
    public void captureSpaFallbackEvidence() {
        capture("spa-fallback", "warning", "Nonexistent id → 200 HTML (SPA shell) instead of a JSON 404",
                "GET", "/account/" + ZERO, true, null);
        capture("spa-fallback", "warning", "Fixed API path served the SPA shell (route shadowed)",
                "GET", "/equipment-library/datstyle/resolve", true, null);
    }

    @Test(description = "Auth gates: unauthenticated mutation + user-token on the agent API are correctly rejected")
    public void captureAuthGateEvidence() {
        capture("auth-gate", "info", "Unauthenticated write correctly rejected (401)",
                "POST", "/task/create", false, "{}");
        capture("agent-token", "info", "Ordinary user token rejected by the agent-token boundary (401)",
                "GET", "/agents/quoteagent/labor-rates", true, null);
    }

    @Test(dependsOnMethods = {"captureInvalidRequestEvidence", "captureAvailabilityEvidence",
            "capturePerformanceEvidence", "capturePaginationEvidence", "captureSpaFallbackEvidence",
            "captureAuthGateEvidence"}, alwaysRun = true,
          description = "Persist captured evidence to reports/api-evidence.json for the consolidated report")
    public void writeEvidence() {
        ExtentReportManager.createTest(MODULE, "capture", "Write evidence file");
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/api-evidence.json")) { w.write(EVIDENCE.toString(2)); }
            System.out.println("[Evidence] wrote reports/api-evidence.json (" + EVIDENCE.length() + " captures).");
            ExtentReportManager.logPass("Wrote " + EVIDENCE.length() + " evidence captures to reports/api-evidence.json.");
        } catch (Exception e) {
            System.out.println("[Evidence] write failed: " + e.getMessage());
            ExtentReportManager.logWarning("Evidence write failed: " + e.getMessage());
        }
    }
}
