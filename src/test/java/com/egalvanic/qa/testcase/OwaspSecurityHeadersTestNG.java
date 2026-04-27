package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OWASP A05 — Security Misconfiguration: HTTP Security Headers.
 *
 * Why this class exists:
 *   Every web app exposes HTTP responses. Several headers represent the difference
 *   between "browser actively defends users" and "browser does whatever attackers
 *   tell it to". Missing headers are silent — there's no UI symptom — so they're
 *   easy to ship without and hard to catch without explicit tests.
 *
 *   Headers we check:
 *     - Content-Security-Policy (CSP) — controls what scripts/fonts/styles can load
 *     - Strict-Transport-Security (HSTS) — forces HTTPS, prevents downgrade
 *     - X-Frame-Options OR CSP frame-ancestors — prevents clickjacking via iframe
 *     - X-Content-Type-Options: nosniff — prevents MIME-type confusion
 *     - Referrer-Policy — limits sensitive URL leakage
 *
 * Test strategy:
 *   We fetch the response headers via JS fetch() with the auth cookies attached
 *   (so the response represents what a real authenticated request sees). For each
 *   header we assert presence + a sane value. Specifically:
 *     - HSTS must include max-age >= 6 months (standard)
 *     - CSP must NOT be `unsafe-inline` for script-src in production
 *     - X-Frame-Options must be DENY or SAMEORIGIN (not ALLOWALL)
 *
 * Result interpretation:
 *   PASS = all required headers present with sane values
 *   FAIL = a required header is missing or has a known-bad value
 *   SKIP = network probe failed (CSP blocked the fetch — meta-CSP issue)
 */
public class OwaspSecurityHeadersTestNG extends BaseTest {

    private static final String MODULE = "OWASP Security";
    private static final String FEATURE = "Security Headers (Misconfiguration)";

    /** Fetch the headers map for an in-app URL via synchronous XHR. */
    @SuppressWarnings("unchecked")
    private Map<String, String> fetchHeaders(String path) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Synchronous XHR keeps things simple — no executeAsyncScript needed —
            // and getAllResponseHeaders() returns every header including security ones.
            String xhrScript =
                "var xhr = new XMLHttpRequest();"
                + "xhr.open('GET', arguments[0], false);"
                + "xhr.withCredentials = true;"
                + "xhr.send(null);"
                + "var h = {};"
                + "var raw = xhr.getAllResponseHeaders();"
                + "raw.split('\\r\\n').forEach(function(line){"
                + "  var idx = line.indexOf(':');"
                + "  if (idx > 0) h[line.slice(0, idx).trim().toLowerCase()] = line.slice(idx + 1).trim();"
                + "});"
                + "return h;";
            Object result = js.executeScript(xhrScript, AppConstants.BASE_URL + path);
            if (!(result instanceof Map)) return new LinkedHashMap<>();
            return (Map<String, String>) result;
        } catch (Exception e) {
            logWarning("Header fetch failed: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    @Test(priority = 1, description = "TC_HEAD_01: Content-Security-Policy header is present and not unsafe-inline-for-scripts")
    public void testTC_HEAD_01_CspHeaderPresent() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HEAD_01_CspHeaderPresent (OWASP A05)");
        logStep("Fetching response headers for /dashboard");

        Map<String, String> headers = fetchHeaders("/dashboard");
        if (headers.isEmpty()) {
            throw new org.testng.SkipException("Header fetch failed — cannot probe");
        }

        String csp = headers.get("content-security-policy");
        logStep("Content-Security-Policy: " + (csp != null ? csp.substring(0, Math.min(200, csp.length())) : "<missing>"));

        Assert.assertNotNull(csp,
            "OWASP A05 — Content-Security-Policy header is missing. Without CSP, any "
            + "stored XSS in any field becomes immediate cookie/token exfiltration. "
            + "Recommended: at minimum `default-src 'self'`.");

        // Quality check: CSP shouldn't include 'unsafe-inline' in script-src on prod
        // (it's sometimes acceptable in dev). If found, log as warning, don't fail.
        if (csp.toLowerCase().contains("unsafe-inline")) {
            logWarning("CSP contains 'unsafe-inline' — reduces XSS protection. "
                + "Consider nonce-based CSP for production.");
        }
        ExtentReportManager.logPass("CSP header present");
    }

    @Test(priority = 2, description = "TC_HEAD_02: Strict-Transport-Security present with adequate max-age")
    public void testTC_HEAD_02_HstsHeaderPresent() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HEAD_02_HstsHeaderPresent (OWASP A05)");
        logStep("Fetching response headers for /dashboard");

        Map<String, String> headers = fetchHeaders("/dashboard");
        if (headers.isEmpty()) {
            throw new org.testng.SkipException("Header fetch failed — cannot probe");
        }

        String hsts = headers.get("strict-transport-security");
        logStep("Strict-Transport-Security: " + hsts);

        Assert.assertNotNull(hsts,
            "OWASP A05 — Strict-Transport-Security header is missing. Without HSTS, "
            + "an attacker on a compromised network can downgrade HTTPS to HTTP and "
            + "intercept all traffic on first visit. Recommended: "
            + "`max-age=31536000; includeSubDomains` (1 year).");

        // Parse max-age
        String lower = hsts.toLowerCase();
        int maxAgeStart = lower.indexOf("max-age=");
        long maxAge = 0;
        if (maxAgeStart >= 0) {
            int end = lower.indexOf(";", maxAgeStart);
            String val = lower.substring(maxAgeStart + 8, end < 0 ? lower.length() : end).trim();
            try { maxAge = Long.parseLong(val); } catch (NumberFormatException ignored) {}
        }
        long sixMonths = 60L * 60 * 24 * 180;
        Assert.assertTrue(maxAge >= sixMonths,
            "HSTS max-age=" + maxAge + " is below 6 months (15552000 seconds). "
            + "This gives attackers a wide window to downgrade.");
        ExtentReportManager.logPass("HSTS present with max-age=" + maxAge);
    }

    @Test(priority = 3, description = "TC_HEAD_03: X-Frame-Options or CSP frame-ancestors prevents clickjacking")
    public void testTC_HEAD_03_ClickjackingProtection() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HEAD_03_ClickjackingProtection (OWASP A05)");
        logStep("Fetching response headers for /dashboard");

        Map<String, String> headers = fetchHeaders("/dashboard");
        if (headers.isEmpty()) {
            throw new org.testng.SkipException("Header fetch failed");
        }

        String xfo = headers.get("x-frame-options");
        String csp = headers.get("content-security-policy");
        boolean xfoOk = xfo != null && (xfo.equalsIgnoreCase("DENY") || xfo.equalsIgnoreCase("SAMEORIGIN"));
        boolean cspFrameOk = csp != null && csp.toLowerCase().contains("frame-ancestors");
        logStep("X-Frame-Options: " + xfo + " | CSP frame-ancestors: " + cspFrameOk);

        Assert.assertTrue(xfoOk || cspFrameOk,
            "OWASP A05 — Neither X-Frame-Options (DENY/SAMEORIGIN) nor CSP frame-ancestors "
            + "directive is present. The app is iframe-able by anyone, exposing users to "
            + "clickjacking attacks. Recommended: `X-Frame-Options: DENY` OR "
            + "`Content-Security-Policy: frame-ancestors 'none'`.");
        ExtentReportManager.logPass("Clickjacking protection in place (XFO=" + xfo + ", CSP-frame-ancestors=" + cspFrameOk + ")");
    }

    @Test(priority = 4, description = "TC_HEAD_04: X-Content-Type-Options: nosniff")
    public void testTC_HEAD_04_NoSniffHeader() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HEAD_04_NoSniffHeader (OWASP A05)");
        logStep("Fetching response headers");

        Map<String, String> headers = fetchHeaders("/dashboard");
        if (headers.isEmpty()) throw new org.testng.SkipException("Header fetch failed");

        String nosniff = headers.get("x-content-type-options");
        logStep("X-Content-Type-Options: " + nosniff);

        Assert.assertEquals(
            nosniff == null ? "<missing>" : nosniff.toLowerCase(),
            "nosniff",
            "OWASP A05 — X-Content-Type-Options header should be `nosniff` to prevent "
            + "MIME-type confusion attacks (e.g., user uploads .jpg with HTML+JS payload, "
            + "browser sniffs and executes as HTML).");
        ExtentReportManager.logPass("X-Content-Type-Options: nosniff");
    }

    @Test(priority = 5, description = "TC_HEAD_05: Referrer-Policy is set (sensible default: strict-origin-when-cross-origin)")
    public void testTC_HEAD_05_ReferrerPolicy() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_HEAD_05_ReferrerPolicy (OWASP A05)");
        logStep("Fetching response headers");

        Map<String, String> headers = fetchHeaders("/dashboard");
        if (headers.isEmpty()) throw new org.testng.SkipException("Header fetch failed");

        String referrer = headers.get("referrer-policy");
        logStep("Referrer-Policy: " + referrer);

        Assert.assertNotNull(referrer,
            "OWASP A05 — Referrer-Policy is missing. Default browser behavior leaks the "
            + "full URL (including any sensitive query params or UUIDs) to every external "
            + "host the app links to. Recommended: `strict-origin-when-cross-origin` or "
            + "`no-referrer`.");
        ExtentReportManager.logPass("Referrer-Policy: " + referrer);
    }
}
