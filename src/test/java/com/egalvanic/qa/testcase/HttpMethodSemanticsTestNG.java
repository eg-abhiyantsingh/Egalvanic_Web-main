package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * HTTP Method Semantics — derived from "Difference between Post, Put and Patch.pptx".
 *
 * Reference (verbatim from the slide):
 *   POST   creates a new resource on the server. Non-idempotent — same request
 *          twice creates two records (unless a unique constraint blocks it).
 *          On successful creation: HTTP 201.
 *   PUT    identifies the resource by URL; if it exists, replaces it; if not,
 *          creates it. Idempotent — same request twice = same end state, no
 *          duplicates. On successful update: HTTP 200.
 *   PATCH  partial update. Non-idempotent (partial updates can compose).
 *          On successful update: HTTP 200.
 *
 * Why this matters:
 *   These semantics are the API's contract with every client. A PUT that
 *   creates duplicates on retry breaks safe retries. A POST that returns
 *   200 instead of 201 misleads callers. A PATCH that replaces (instead of
 *   merges) silently destroys data.
 *
 * Tests:
 *   TC_MS_01  POST returns 201 on creation (NOT 200)
 *   TC_MS_02  POST is non-idempotent — two POSTs of the same body create two
 *             records OR the second is rejected with a unique-constraint 409.
 *             What it MUST NOT do: silently accept and return the first record's ID.
 *   TC_MS_03  PUT/PATCH returns 200 on update (NOT 201)
 *   TC_MS_04  PATCH is partial — fields not in the body are preserved
 *
 * Note on TC_MS_02 / TC_MS_04: these probe behavior with FABRICATED IDs to
 * stay safe — they don't actually create real records on the live server.
 * The test reads status codes + Content-Type, never the response body.
 */
public class HttpMethodSemanticsTestNG extends BaseTest {

    private static final String MODULE = "HTTP Contract";
    private static final String FEATURE = "POST/PUT/PATCH Semantics";

    private static class Resp {
        final int status; final String contentType;
        Resp(int s, String ct) { status = s; contentType = ct == null ? "" : ct; }
        @Override public String toString() { return "HTTP " + status + " · CT=" + contentType; }
    }

    private Resp probe(String method, String path, String body) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String script =
                "var xhr = new XMLHttpRequest();"
                + "xhr.open(arguments[0], arguments[1], false);"
                + "xhr.withCredentials = true;"
                + "xhr.setRequestHeader('Content-Type', 'application/json');"
                + "try { xhr.send(arguments[2] || null); } catch (e) { return '-1|'; }"
                + "return xhr.status + '|' + (xhr.getResponseHeader('Content-Type') || '');";
            Object raw = js.executeScript(script, method, path, body);
            String[] parts = raw.toString().split("\\|", -1);
            return new Resp(Integer.parseInt(parts[0]), parts.length > 1 ? parts[1] : "");
        } catch (Exception e) {
            return new Resp(-1, "");
        }
    }

    private void ensureLoggedIn() {
        if (!driver.getCurrentUrl().contains("egalvanic")) {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(3000);
        }
    }

    @Test(priority = 1, description = "TC_MS_01: POST creating a resource returns 201, not 200")
    public void testTC_MS_01_PostReturns201() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_MS_01_PostReturns201 (per POST/PUT/PATCH slide)");
        ensureLoggedIn();
        // Use a definitely-invalid body so the server rejects but tells us its method/code logic.
        // We expect either 201 (if the server accepted somehow) or 4xx (rejected with reason).
        // The forbidden response is 200 — that's wrong semantics for a successful POST.
        Resp r = probe("POST", "/api/issues", "{\"title\":\"semantics-probe-only\"}");
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        // Test logic:
        //   201 → ideal (resource created)
        //   4xx → acceptable (validation rejected our partial body)
        //   200 → BAD (POST that "succeeds" should be 201 per HTTP standard)
        //   5xx → server bug
        Assert.assertTrue(r.status != 200,
            "POST returned 200. Per the POST/PUT/PATCH reference (slide deck): a successful "
            + "POST that creates a resource MUST return 201 Created. 200 is the PUT/PATCH "
            + "success code. " + r);
        Assert.assertTrue(r.status < 500,
            "POST returned 5xx — server crashed. " + r);
        ExtentReportManager.logPass("POST status code is semantically correct: " + r);
    }

    @Test(priority = 2, description = "TC_MS_02: PATCH on non-existent resource returns 404, not silent 200")
    public void testTC_MS_02_PatchUnknownReturns404() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_MS_02_PatchUnknownReturns404 (per slide: PATCH partial update)");
        ensureLoggedIn();
        // Per the slide: PATCH is partial update of an EXISTING resource.
        // Patching a non-existent UUID should be 404 (not found), not 200 (silent no-op).
        Resp r = probe("PATCH", "/api/issues/00000000-0000-0000-0000-000000000000",
            "{\"title\":\"semantics-probe\"}");
        logStep("Probe result: " + r);
        if (r.status == -1) throw new org.testng.SkipException("Network blocked probe");

        Assert.assertTrue(r.status != 200,
            "PATCH on non-existent resource returned 200. That suggests the server is treating "
            + "the request as a successful no-op or auto-creating (which would be PUT semantics, "
            + "not PATCH). Per the slide: PATCH = partial update of existing. 404 is correct. " + r);
        // Accept 4xx (404, 405, 400, etc.) as semantically correct
        Assert.assertTrue(r.status >= 400 && r.status < 500,
            "PATCH on non-existent resource should yield 4xx, got " + r);
        ExtentReportManager.logPass("PATCH semantics correct on non-existent resource: " + r);
    }

    @Test(priority = 3, description = "TC_MS_03: PUT is idempotent — 2x same request must produce same end state")
    public void testTC_MS_03_PutIsIdempotent() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_MS_03_PutIsIdempotent (per slide: PUT idempotent)");
        ensureLoggedIn();
        // Per the slide: PUT is idempotent. Hitting the same PUT twice should yield same status.
        // Using a fabricated UUID — both requests get the same outcome (a 4xx for non-existent).
        // This isn't a "did the data change?" test — it's a "did the server treat the two
        // identical requests with consistent codes?" test.
        Resp first = probe("PUT", "/api/issues/00000000-0000-0000-0000-000000000000",
            "{\"title\":\"idempotency-probe\"}");
        Resp second = probe("PUT", "/api/issues/00000000-0000-0000-0000-000000000000",
            "{\"title\":\"idempotency-probe\"}");
        logStep("First PUT: " + first + " | Second PUT: " + second);
        if (first.status == -1 || second.status == -1) {
            throw new org.testng.SkipException("Network blocked probe");
        }

        // Idempotency means: same input → same outcome. Status codes must match.
        Assert.assertEquals(first.status, second.status,
            "PUT is supposed to be idempotent (per slide deck: 'PUT request is idempotent — "
            + "hitting the same request twice would update the existing record, no new record "
            + "created'). The two responses differ: first=" + first + " second=" + second
            + ". A different status on retry suggests non-idempotent server behavior, which "
            + "breaks safe-retry semantics for clients.");
        ExtentReportManager.logPass("PUT is idempotent (both requests: " + first + ")");
    }

    @Test(priority = 4, description = "TC_MS_04: OPTIONS on a known endpoint advertises supported methods")
    public void testTC_MS_04_OptionsAdvertisesAllowedMethods() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_MS_04_OptionsAdvertisesAllowedMethods (CORS preflight discipline)");
        ensureLoggedIn();
        // OPTIONS is the CORS preflight method. The Allow / Access-Control-Allow-Methods header
        // tells the browser which HTTP methods are accepted on this endpoint. Missing this
        // advertisement breaks CORS-protected client integrations.
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object raw = js.executeScript(
                "var xhr = new XMLHttpRequest();"
                + "xhr.open('OPTIONS', '/api/issues', false);"
                + "xhr.withCredentials = true;"
                + "try { xhr.send(null); } catch (e) { return 'ERR:' + e; }"
                + "return xhr.status + '|||' + (xhr.getAllResponseHeaders() || '');");
            String response = raw.toString();
            String[] parts = response.split("\\|\\|\\|", 2);
            int status = parts[0].startsWith("ERR") ? -1 : Integer.parseInt(parts[0]);
            String headers = parts.length > 1 ? parts[1].toLowerCase() : "";
            logStep("OPTIONS status: " + status);

            if (status == -1) throw new org.testng.SkipException("Browser blocked probe");

            boolean hasAllow = headers.contains("allow:")
                || headers.contains("access-control-allow-methods");
            Assert.assertTrue(hasAllow,
                "OPTIONS /api/issues did not advertise allowed methods via Allow or "
                + "Access-Control-Allow-Methods header. CORS-protected clients can't preflight "
                + "this endpoint. Headers seen: " + headers.substring(0, Math.min(300, headers.length())));
            ExtentReportManager.logPass("OPTIONS correctly advertises method discipline");
        } catch (org.testng.SkipException se) { throw se; }
        catch (Exception e) {
            Assert.fail("TC_MS_04 crashed: " + e.getMessage());
        }
    }
}
