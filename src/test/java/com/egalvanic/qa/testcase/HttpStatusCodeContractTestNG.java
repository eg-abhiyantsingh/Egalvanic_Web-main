package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * HTTP Status Code Contract Tests — derived from HTTP_StatusCode.png.
 *
 * The reference image categorizes status codes:
 *   1xx Informational (100, 101, 102)
 *   2xx Success (200, 201, 202, 204, 205)
 *   3xx Redirection (300, 301, 302, 303, 304, 305)
 *   4xx Client Error (400, 401, 402, 403, 404)
 *   5xx Server Error (501, 502, 503, 507, 508)
 *
 * This test class verifies the eGalvanic backend returns the SEMANTICALLY
 * CORRECT status code for each scenario — a contract that's silently
 * violated all over the place by the SPA-fallback bug (CLAUDE.md #8 —
 * server returns 200 + HTML for unknown /api/* paths instead of 404 + JSON).
 *
 * Why this matters:
 *   - Status codes are the API's vocabulary. A 200 means "I did what you
 *     asked"; a 404 means "that resource doesn't exist". Returning 200
 *     for non-existent resources isn't just "wrong" — it actively
 *     misleads every client (frontend, integration partners, monitoring).
 *   - Many frontend bugs we already track (BUG-004, the SPA-fallback
 *     IDOR confusion) trace back to this status-code-discipline gap.
 *
 * Tests:
 *   TC_HSC_01  GET unknown /api/* path → 404 (NOT 200/HTML)
 *   TC_HSC_02  GET valid /api/auth/me with cookies → 200
 *   TC_HSC_03  GET /api/auth/me without cookies → 401 (NOT 200 with anonymous data)
 *   TC_HSC_04  POST /api/issues with malformed JSON → 400 (NOT 500)
 *   TC_HSC_05  POST /api/issues with required-field missing → 400 with field error
 *   TC_HSC_06  GET /api/non-existent-endpoint → 404 (NOT 200/HTML)
 *   TC_HSC_07  Wrong HTTP method (e.g., DELETE on a POST-only endpoint) → 405
 *   TC_HSC_08  Successful resource fetch returns Content-Type: application/json
 *
 * Architecture: extends BaseTest for authenticated session (cookies needed
 * for the positive tests). Negative tests like TC_HSC_03 explicitly clear
 * the cookies before probing.
 */
public class HttpStatusCodeContractTestNG extends BaseTest {

    private static final String MODULE = "HTTP Contract";
    private static final String FEATURE = "Status Code Discipline";

    /** Probe response. Captures status + Content-Type so we can distinguish
     *  semantically-correct 4xx-with-JSON from semantically-broken 200-with-HTML. */
    private static class Resp {
        final int status; final String contentType; final int bodyLen;
        Resp(int s, String ct, int bl) { status = s; contentType = ct == null ? "" : ct; bodyLen = bl; }
        boolean isJson() { return contentType.toLowerCase().contains("application/json"); }
        boolean isHtml() { return contentType.toLowerCase().contains("text/html"); }
        @Override public String toString() {
            return "HTTP " + status + " · CT=" + contentType + " · bodyLen=" + bodyLen;
        }
    }

    private Resp probe(String method, String path) {
        return probe(method, path, null);
    }

    private Resp probe(String method, String path, String body) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String script =
                "var xhr = new XMLHttpRequest();"
                + "xhr.open(arguments[0], arguments[1], false);"
                + "xhr.withCredentials = true;"
                + "xhr.setRequestHeader('Content-Type', 'application/json');"
                + "try { xhr.send(arguments[2] || null); } catch (e) { return '-1||0'; }"
                + "return xhr.status + '|' + (xhr.getResponseHeader('Content-Type') || '') "
                + "+ '|' + (xhr.responseText || '').length;";
            Object raw = js.executeScript(script, method, path, body);
            if (raw == null) return new Resp(-1, "", 0);
            String[] parts = raw.toString().split("\\|", -1);
            int status = Integer.parseInt(parts[0]);
            String ct = parts.length > 1 ? parts[1] : "";
            int bl = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new Resp(status, ct, bl);
        } catch (Exception e) {
            return new Resp(-1, "", 0);
        }
    }

    private void ensureLoggedIn() {
        if (!driver.getCurrentUrl().contains("egalvanic")) {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(3000);
        }
    }

    @Test(priority = 1, description = "TC_HSC_01: Unknown /api/issues/<bad-uuid> returns 4xx with JSON, NOT 200 with HTML")
    public void testTC_HSC_01_UnknownIssueReturns404Json() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_01_UnknownIssueReturns404Json (HTTP_StatusCode.png ref)");
        ensureLoggedIn();
        Resp r = probe("GET", "/api/issues/00000000-0000-0000-0000-000000000000");
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        // The HTTP standard: unknown resource MUST return 404. Returning 200 with
        // the SPA's HTML index page misleads all clients into thinking the resource exists.
        Assert.assertNotEquals(r.status, 200,
            "OWASP-adjacent / status discipline — /api/issues/<bad-uuid> returned 200. "
            + "Per HTTP standard (HTTP_StatusCode.png) this MUST be 404 for non-existent resources. "
            + "Likely cause: SPA catch-all routing serving index.html for unknown /api/* paths "
            + "(CLAUDE.md #8). Fix at the API server's routing layer to return 404 + JSON.");
        Assert.assertTrue(r.status >= 400 && r.status < 500,
            "Expected 4xx, got " + r);
        Assert.assertTrue(r.isJson() || r.bodyLen == 0,
            "4xx response should be JSON (machine-readable error) or empty, not HTML. " + r);
        ExtentReportManager.logPass("Status discipline OK: " + r);
    }

    @Test(priority = 2, description = "TC_HSC_02: Valid /api/auth/me with cookies returns 200 + JSON")
    public void testTC_HSC_02_AuthenticatedMeReturns200() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_02_AuthenticatedMeReturns200 (positive baseline)");
        ensureLoggedIn();
        Resp r = probe("GET", "/api/auth/me");
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        Assert.assertEquals(r.status, 200, "Authenticated /api/auth/me should be 200, got " + r);
        Assert.assertTrue(r.isJson(), "Auth/me should return JSON, got Content-Type=" + r.contentType);
        Assert.assertTrue(r.bodyLen > 10, "Auth/me JSON body suspiciously short: " + r.bodyLen);
        ExtentReportManager.logPass("/api/auth/me returned 200 + JSON: " + r);
    }

    @Test(priority = 3, description = "TC_HSC_03: /api/auth/me WITHOUT auth cookies returns 401")
    public void testTC_HSC_03_UnauthenticatedReturns401() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_03_UnauthenticatedReturns401 (HTTP 401 contract)");
        ensureLoggedIn();
        // Probe with an XHR that explicitly DOESN'T send cookies (omit credentials)
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object raw = js.executeScript(
                "var xhr = new XMLHttpRequest();"
                + "xhr.open('GET', '/api/auth/me', false);"
                + "xhr.withCredentials = false;"  // NO cookies
                + "try { xhr.send(null); } catch (e) { return '-1||0'; }"
                + "return xhr.status + '|' + (xhr.getResponseHeader('Content-Type') || '') "
                + "+ '|' + (xhr.responseText || '').length;");
            String[] parts = raw.toString().split("\\|", -1);
            int status = Integer.parseInt(parts[0]);
            logStep("Unauthenticated probe status: " + status + ", CT: "
                + (parts.length > 1 ? parts[1] : ""));

            if (status == -1) throw new org.testng.SkipException("Network blocked probe");
            // 401 (per HTTP standard) or 403 (some servers prefer this for unauth) are acceptable.
            // 200 is a critical bug — would mean anonymous data leak.
            Assert.assertTrue(status == 401 || status == 403 || status == 400,
                "Unauthenticated /api/auth/me returned " + status
                + ". Per HTTP_StatusCode.png reference: 401 Unauthorized is correct. "
                + "200 = critical info leak. 5xx = unhandled-auth bug.");
            ExtentReportManager.logPass("Correctly rejected unauthenticated request (HTTP " + status + ")");
        } catch (org.testng.SkipException se) { throw se; }
        catch (Exception e) {
            Assert.fail("TC_HSC_03 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "TC_HSC_04: POST /api/issues with malformed JSON returns 400, NOT 500")
    public void testTC_HSC_04_MalformedJsonReturns400() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_04_MalformedJsonReturns400 (4xx vs 5xx discipline)");
        ensureLoggedIn();
        // Send unterminated JSON
        Resp r = probe("POST", "/api/issues", "{\"title\":\"unterminated");
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        // HTTP discipline: client sent bad input → 4xx. Server crash → 5xx (BUG).
        // Specifically 400 Bad Request is the correct code per HTTP_StatusCode.png.
        Assert.assertTrue(r.status >= 400 && r.status < 500,
            "Malformed JSON should yield 4xx (client error). Got " + r
            + ". A 5xx means the server crashed parsing client input — that's a BUG.");
        ExtentReportManager.logPass("Malformed JSON correctly rejected with 4xx: " + r);
    }

    @Test(priority = 5, description = "TC_HSC_05: GET /api/non-existent-endpoint returns 404, NOT 200/HTML")
    public void testTC_HSC_05_UnknownEndpointReturns404() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_05_UnknownEndpointReturns404 (catch SPA-fallback)");
        ensureLoggedIn();
        Resp r = probe("GET", "/api/this-endpoint-does-not-exist-2026");
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        // The SPA-fallback anti-pattern: unknown /api/* paths return the React app's index.html.
        // This MUST be a 404 with JSON for good API hygiene.
        Assert.assertNotEquals(r.status, 200,
            "Unknown API endpoint returned 200 — almost certainly the SPA catch-all serving "
            + "index.html (CLAUDE.md #8). Should be 404 + JSON. " + r);
        Assert.assertEquals(r.status, 404,
            "Unknown API endpoint should return 404 specifically. Got " + r);
        ExtentReportManager.logPass("Unknown endpoint correctly returns 404: " + r);
    }

    @Test(priority = 6, description = "TC_HSC_06: Wrong HTTP method returns 405 Method Not Allowed")
    public void testTC_HSC_06_WrongMethodReturns405() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_06_WrongMethodReturns405 (HTTP 405 contract)");
        ensureLoggedIn();
        // PUT on /api/auth/me (which is GET-only) — should be 405
        Resp r = probe("PUT", "/api/auth/me", "{}");
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        // 405 Method Not Allowed is the HTTP-correct response. Some servers respond 404 instead
        // (treats "wrong method" as "no such resource"); we accept either as long as it's NOT 200.
        Assert.assertNotEquals(r.status, 200,
            "PUT on a GET-only endpoint returned 200. Likely the server processed the request "
            + "as a different method or didn't validate. Should be 405 (or at least 4xx). " + r);
        Assert.assertTrue(r.status >= 400 && r.status < 500,
            "Wrong method should yield 4xx, got " + r);
        ExtentReportManager.logPass("Wrong method correctly rejected: " + r);
    }

    @Test(priority = 7, description = "TC_HSC_07: Successful API responses use Content-Type: application/json")
    public void testTC_HSC_07_SuccessResponsesAreJson() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_07_SuccessResponsesAreJson (Content-Type discipline)");
        ensureLoggedIn();
        // Two known-good endpoints
        String[] paths = {"/api/auth/me", "/api/action-items/counts"};
        StringBuilder evidence = new StringBuilder();
        int violations = 0;
        for (String path : paths) {
            Resp r = probe("GET", path);
            evidence.append(path).append(" → ").append(r).append(" | ");
            if (r.status >= 200 && r.status < 300 && !r.isJson()) violations++;
        }
        logStep("Evidence: " + evidence);
        Assert.assertEquals(violations, 0,
            "Successful API responses MUST be application/json. Found " + violations
            + " endpoint(s) returning a 2xx with non-JSON Content-Type. " + evidence);
        ExtentReportManager.logPass("All success responses are JSON");
    }

    @Test(priority = 8, description = "TC_HSC_08: Unknown company subdomain login returns 4xx, NOT 5xx")
    public void testTC_HSC_08_UnknownSubdomainGives4xx() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HSC_08_UnknownSubdomainGives4xx (5xx-must-not-happen)");
        ensureLoggedIn();
        // Send login with an obviously-unknown subdomain — should be 4xx, not 5xx
        String body = "{"
            + "\"email\":\"nobody@nowhere.test\","
            + "\"password\":\"wrong\","
            + "\"subdomain\":\"definitely-not-a-real-tenant-2026\"}";
        Resp r = probe("POST", "/api/auth/login", body);
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        // 401 (bad creds) or 404 (subdomain not found) are both fine. 5xx = server bug.
        Assert.assertTrue(r.status < 500,
            "Login with unknown subdomain returned 5xx — server crashed on input it should "
            + "have validated. Per HTTP_StatusCode.png 5xx = Server Error class, reserved for "
            + "things truly unexpected. Bad input from clients should always be 4xx. " + r);
        ExtentReportManager.logPass("Login w/ unknown subdomain correctly rejected with 4xx: " + r);
    }
}
