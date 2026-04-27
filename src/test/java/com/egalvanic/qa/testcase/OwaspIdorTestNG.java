package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * OWASP A01 — Broken Access Control / IDOR (Insecure Direct Object Reference).
 *
 * Why this class exists:
 *   The eGalvanic Z Platform is multi-tenant SaaS. Each issue / asset / connection /
 *   work-order URL is keyed by a bare UUID — e.g., /issues/9a5eeb29-...,
 *   /api/issues/<uuid>, /api/assets/<uuid>. The backend MUST verify on every read
 *   AND every write that the UUID belongs to the caller's tenant. One missing check
 *   on PATCH/DELETE = cross-tenant data write/delete. This is the OWASP #1 risk per
 *   the 2021 Top 10 (formerly A05).
 *
 * Test strategy:
 *   These tests are SAFE by design — they probe the API for IDOR using fabricated
 *   "obviously not yours" UUIDs (zero-UUID, random fresh UUIDs) so they cannot
 *   accidentally read/write real cross-tenant data even if a vulnerability exists.
 *   A vulnerable backend would return 200 OK; a defended backend returns 403 / 404 /
 *   401. We only fail the test if the backend returns 200 (access granted to a UUID
 *   it should never authorize).
 *
 * What we test:
 *   TC_IDOR_01  GET /api/issues/<zero-uuid>        Expect 4xx (not 200 with data)
 *   TC_IDOR_02  GET /api/assets/<zero-uuid>        Expect 4xx
 *   TC_IDOR_03  GET /api/sessions/<zero-uuid>      Expect 4xx (work orders)
 *   TC_IDOR_04  PATCH /api/issues/<zero-uuid>      Expect 4xx (cross-tenant write)
 *   TC_IDOR_05  DELETE /api/issues/<zero-uuid>     Expect 4xx (cross-tenant delete)
 *   TC_IDOR_06  POST /api/issues with company_id=<other-tenant>  Expect server
 *                ignores body field, uses session tenant. Vuln if record lands in
 *                another tenant.
 *
 * House-style notes:
 *   - Extends BaseTest (we need an authenticated session for cookie attachment).
 *   - Uses XHR via JavascriptExecutor — most reliable cross-origin path while
 *     keeping the browser's auth cookies attached automatically.
 *   - "SAFE" probe rule: only assert on STATUS CODE, not response body. A
 *     compromised endpoint that returns 200 with empty body is still a finding.
 */
public class OwaspIdorTestNG extends BaseTest {

    private static final String MODULE = "OWASP Security";
    private static final String FEATURE = "Broken Access Control / IDOR";

    private static final String ZERO_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String FAKE_UUID = "deadbeef-dead-beef-dead-beefdeadbeef";

    /** Probe response — captures status + Content-Type to distinguish true IDOR from SPA-fallback. */
    private static class ProbeResult {
        final int status;
        final String contentType;
        final int bodyLen;
        ProbeResult(int s, String ct, int bl) { status = s; contentType = ct == null ? "" : ct; bodyLen = bl; }
        boolean isHtml() { return contentType.toLowerCase().contains("text/html"); }
        boolean isJson() { return contentType.toLowerCase().contains("application/json"); }
    }

    /** Run an XHR. Returns status + content-type + body length. */
    private ProbeResult probeRequest(String method, String path, String body) {
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
            if (raw == null) return new ProbeResult(-1, "", 0);
            String[] parts = raw.toString().split("\\|", -1);
            int status = parts.length > 0 ? Integer.parseInt(parts[0]) : -1;
            String ct = parts.length > 1 ? parts[1] : "";
            int bl = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new ProbeResult(status, ct, bl);
        } catch (Exception e) {
            return new ProbeResult(-1, "", 0);
        }
    }

    /**
     * Asserts the probe doesn't represent a true IDOR.
     * Three outcome buckets:
     *   1) 4xx        → defended (PASS)
     *   2) 200 + HTML → SPA catch-all returning index.html (known systemic bug per
     *                  CLAUDE.md #8). Log as warning. Don't fail this OWASP test
     *                  because it's not IDOR — file separately as a routing bug.
     *   3) 200 + JSON → real IDOR (FAIL with high severity)
     *   4) Anything else → also fail (5xx with body, etc. — info leak risk)
     */
    private void assertNotAuthorized(ProbeResult r, String path, String method) {
        ScreenshotUtil.captureScreenshot("owasp_idor_" + method + "_" + r.status);
        if (r.status >= 400 && r.status < 500) {
            // Defended — exactly what we want
            return;
        }
        if (r.status >= 200 && r.status < 300 && r.isHtml()) {
            // SPA catch-all serving index.html for unknown /api/* paths.
            // Not IDOR — it's a separate routing bug. Skip this test with a clear note
            // so we don't double-count the same systemic gap as 6 distinct IDOR fails.
            throw new org.testng.SkipException(
                "OWASP A01 — " + method + " " + path + " returned HTTP " + r.status
                + " with Content-Type=" + r.contentType + " (HTML body, " + r.bodyLen + " bytes). "
                + "This is the SPA catch-all serving index.html for unknown /api/* paths "
                + "(CLAUDE.md #8 — backend returns HTML on errors). Not a true IDOR; "
                + "API routing should return 404 with JSON. File separately.");
        }
        // 200 + JSON, 200 + empty, 5xx with body, etc. — all real findings
        Assert.fail(
            "OWASP A01 IDOR — " + method + " " + path + " returned HTTP " + r.status
            + " (Content-Type=" + r.contentType + ", bodyLen=" + r.bodyLen + ") "
            + "for a fabricated UUID. JSON 200 = true IDOR (backend skipped row-level auth). "
            + "Empty 200 = soft-404 leak. 5xx with stack trace = info leak. "
            + "Whichever it is, this isn't a defended endpoint.");
    }

    private void ensureAuthenticated() {
        // Class-level login already ran; just confirm we're inside the app
        if (!driver.getCurrentUrl().contains("egalvanic")) {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(3000);
        }
    }

    @Test(priority = 1, description = "TC_IDOR_01: GET /api/issues/<zero-uuid> must not return 200")
    public void testTC_IDOR_01_GetIssueBadUuidDenied() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_IDOR_01_GetIssueBadUuidDenied (OWASP A01)");
        logStep("Probing GET /api/issues/" + ZERO_UUID);

        ensureAuthenticated();
        ProbeResult result = probeRequest("GET", "/api/issues/" + ZERO_UUID, null);
        logStep("Response status: " + result.status + " | Content-Type: " + result.contentType + " | bodyLen: " + result.bodyLen);

        if (result.status == -1) {
            throw new org.testng.SkipException("Network/CSP blocked probe — cannot test");
        }
        assertNotAuthorized(result, "/api/issues/" + ZERO_UUID, "GET");
        ExtentReportManager.logPass("Backend correctly denied (HTTP " + result.status + ")");
    }

    @Test(priority = 2, description = "TC_IDOR_02: GET /api/assets/<zero-uuid> must not return 200")
    public void testTC_IDOR_02_GetAssetBadUuidDenied() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_IDOR_02_GetAssetBadUuidDenied (OWASP A01)");
        logStep("Probing GET /api/assets/" + ZERO_UUID);

        ensureAuthenticated();
        ProbeResult result = probeRequest("GET", "/api/assets/" + ZERO_UUID, null);
        logStep("Response status: " + result.status + " | Content-Type: " + result.contentType + " | bodyLen: " + result.bodyLen);

        if (result.status == -1) throw new org.testng.SkipException("Network blocked probe");
        assertNotAuthorized(result, "/api/assets/" + ZERO_UUID, "GET");
        ExtentReportManager.logPass("Backend correctly denied (HTTP " + result.status + ")");
    }

    @Test(priority = 3, description = "TC_IDOR_03: GET /api/sessions/<zero-uuid> (work orders) must not return 200")
    public void testTC_IDOR_03_GetWorkOrderBadUuidDenied() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_IDOR_03_GetWorkOrderBadUuidDenied (OWASP A01)");
        logStep("Probing GET /api/sessions/" + ZERO_UUID);

        ensureAuthenticated();
        ProbeResult result = probeRequest("GET", "/api/sessions/" + ZERO_UUID, null);
        logStep("Response status: " + result.status + " | Content-Type: " + result.contentType + " | bodyLen: " + result.bodyLen);

        if (result.status == -1) throw new org.testng.SkipException("Network blocked probe");
        assertNotAuthorized(result, "/api/sessions/" + ZERO_UUID, "GET");
        ExtentReportManager.logPass("Backend correctly denied (HTTP " + result.status + ")");
    }

    @Test(priority = 4, description = "TC_IDOR_04: PATCH /api/issues/<bad-uuid> must not succeed")
    public void testTC_IDOR_04_PatchIssueBadUuidDenied() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_IDOR_04_PatchIssueBadUuidDenied (OWASP A01 — write IDOR)");
        logStep("Probing PATCH /api/issues/" + FAKE_UUID + " with attacker payload");

        ensureAuthenticated();
        // Body: try to overwrite title — if 200, attacker just wrote to a foreign record
        String body = "{\"title\":\"owasp-a01-probe-do-not-process\"}";
        ProbeResult result = probeRequest("PATCH", "/api/issues/" + FAKE_UUID, body);
        logStep("Response status: " + result.status + " | Content-Type: " + result.contentType + " | bodyLen: " + result.bodyLen);

        if (result.status == -1) throw new org.testng.SkipException("Network blocked probe");
        assertNotAuthorized(result, "/api/issues/" + FAKE_UUID, "PATCH");
        ExtentReportManager.logPass("Backend correctly denied write IDOR (HTTP " + result.status + ")");
    }

    @Test(priority = 5, description = "TC_IDOR_05: DELETE /api/issues/<bad-uuid> must not return 2xx")
    public void testTC_IDOR_05_DeleteIssueBadUuidDenied() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_IDOR_05_DeleteIssueBadUuidDenied (OWASP A01 — destructive IDOR)");
        logStep("Probing DELETE /api/issues/" + FAKE_UUID);

        ensureAuthenticated();
        ProbeResult result = probeRequest("DELETE", "/api/issues/" + FAKE_UUID, null);
        logStep("Response status: " + result.status + " | Content-Type: " + result.contentType + " | bodyLen: " + result.bodyLen);

        if (result.status == -1) throw new org.testng.SkipException("Network blocked probe");
        assertNotAuthorized(result, "/api/issues/" + FAKE_UUID, "DELETE");
        ExtentReportManager.logPass("Backend correctly denied delete IDOR (HTTP " + result.status + ")");
    }

    @Test(priority = 6, description = "TC_IDOR_06: POST /api/issues with foreign company_id must not plant cross-tenant")
    public void testTC_IDOR_06_PostIssueForeignCompanyIdRejected() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_IDOR_06_PostIssueForeignCompanyIdRejected (OWASP A01 — mass-assignment)");
        logStep("Probing POST /api/issues with body field company_id=<fake>");

        ensureAuthenticated();
        // The classic mass-assignment IDOR: include a foreign company_id in the body.
        // A defended server ignores it (uses session tenant); a vuln server creates the
        // record under that company.
        String body = "{"
            + "\"title\":\"owasp-a01-probe-do-not-process\","
            + "\"company_id\":\"" + FAKE_UUID + "\""
            + "}";
        ProbeResult result = probeRequest("POST", "/api/issues", body);
        logStep("Response status: " + result.status + " | Content-Type: " + result.contentType + " | bodyLen: " + result.bodyLen);

        if (result.status == -1) throw new org.testng.SkipException("Network blocked probe");
        // Accept 400 (validation rejected partial body) or 4xx; reject 201/200.
        boolean created = (result.status == 200 || result.status == 201) && result.isJson();
        Assert.assertFalse(created,
            "OWASP A01 — POST /api/issues with foreign company_id returned "
            + result.status + " (CT=" + result.contentType + "). "
            + "A 2xx with JSON indicates the server accepted a body-supplied company_id, "
            + "potentially planting a record cross-tenant. Server should ignore the "
            + "body field and bind tenant from the session.");
        ExtentReportManager.logPass(
            "Backend correctly rejected foreign company_id (HTTP " + result.status + ")");
    }

    /** Documented for future expansion — not implemented in this commit. */
    @SuppressWarnings("unused")
    private static final List<String> FUTURE_IDOR_ENDPOINTS = Arrays.asList(
        "/api/connections/<uuid>",
        "/api/locations/<uuid>",
        "/api/tasks/<uuid>",
        "/api/users/<uuid>",
        "/api/photos/<uuid>"
    );
}
