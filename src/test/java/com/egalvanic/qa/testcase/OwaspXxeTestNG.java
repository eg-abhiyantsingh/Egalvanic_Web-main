package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * OWASP A05 — XML External Entity (XXE) Injection.
 *
 * Why this class exists:
 *   The eGalvanic backend exposes JSON APIs primarily, but it also serves PDF
 *   exports, asset import (CSV/XML), and panel-schedule uploads. ANY XML parsing
 *   path is a potential XXE surface unless the parser explicitly disables external
 *   entity resolution. A vulnerable parser given a payload like:
 *
 *     <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
 *     <root>&xxe;</root>
 *
 *   would fetch /etc/passwd from the server and embed it in the response, OR make
 *   an outbound HTTP request to an attacker-controlled URL (data exfiltration).
 *
 * Test strategy:
 *   We POST XML payloads to endpoints that historically accept XML (or where there's
 *   reason to think they might — anywhere "import" or "upload" appears). For each:
 *     - If the server returns 200 with the entity-expanded text, that's a critical XXE
 *     - If the server returns 415 (Unsupported Media Type), 400, or 4xx, that's safe
 *     - If the server returns 5xx, log as suspicious — could mean the parser tried to
 *       resolve the entity and failed in a way that leaks info via stack trace
 *
 *   We use BENIGN payloads — no actual exfiltration. The probe only checks whether
 *   external-entity resolution would be ATTEMPTED, not whether real data leaves.
 *
 * Test methods:
 *   TC_XXE_01  POST XML to /api/issues — should reject (Content-Type guard)
 *   TC_XXE_02  POST XML containing DOCTYPE entity to /api/issues — must not 200
 *   TC_XXE_03  POST XML billion-laughs (DoS) — server must not hang/echo expanded
 *   TC_XXE_04  Verify /api/* endpoints don't advertise application/xml as accepted
 */
public class OwaspXxeTestNG extends BaseTest {

    private static final String MODULE = "OWASP Security";
    private static final String FEATURE = "XML External Entity (XXE)";

    /** Benign DOCTYPE-with-entity probe — references a non-existent file so a real
     *  exfiltration is impossible even on a vulnerable parser. We're checking
     *  whether the server *attempts* to parse external entities, not whether real
     *  data leaks. */
    private static final String XXE_PROBE_PAYLOAD =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<!DOCTYPE owaspProbe ["
        + "<!ENTITY benignProbe SYSTEM \"file:///nonexistent-owasp-probe-do-not-process\">"
        + "]>"
        + "<root>&benignProbe;</root>";

    /** "Billion laughs" — recursive entity expansion DoS. Many parsers default-block;
     *  some don't. A vulnerable parser would CPU-spin trying to expand the 10^9
     *  characters and might 502 / time out. We send a SHORT version so we don't
     *  actually DoS the server — just deep enough to detect parser engagement. */
    private static final String BILLION_LAUGHS_LITE =
        "<?xml version=\"1.0\"?>"
        + "<!DOCTYPE lolz ["
        + "<!ENTITY lol \"lol\">"
        + "<!ENTITY lol2 \"&lol;&lol;&lol;\">"
        + "<!ENTITY lol3 \"&lol2;&lol2;&lol2;\">"
        + "]>"
        + "<lolz>&lol3;</lolz>";

    /** Run an XHR with the requested Content-Type and body. Returns status (-1 on err). */
    private int probeXml(String path, String body) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String script =
                "var xhr = new XMLHttpRequest();"
                + "xhr.open('POST', arguments[0], false);"
                + "xhr.withCredentials = true;"
                + "xhr.setRequestHeader('Content-Type', 'application/xml');"
                + "try { xhr.send(arguments[1]); } catch (e) { return -1; }"
                + "return xhr.status;";
            Object raw = js.executeScript(script, path, body);
            return raw == null ? -1 : ((Number) raw).intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Read the response body when sending XML — used for XXE-detection (entity expanded?). */
    private String probeXmlAndReadBody(String path, String body) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String script =
                "var xhr = new XMLHttpRequest();"
                + "xhr.open('POST', arguments[0], false);"
                + "xhr.withCredentials = true;"
                + "xhr.setRequestHeader('Content-Type', 'application/xml');"
                + "try { xhr.send(arguments[1]); } catch (e) { return 'ERR:' + e.message; }"
                + "return xhr.status + '|' + (xhr.responseText || '').slice(0, 1000);";
            Object raw = js.executeScript(script, path, body);
            return raw == null ? "" : raw.toString();
        } catch (Exception e) {
            return "ERR:" + e.getMessage();
        }
    }

    private void ensureAuthenticated() {
        if (!driver.getCurrentUrl().contains("egalvanic")) {
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(3000);
        }
    }

    @Test(priority = 1, description = "TC_XXE_01: POST application/xml to /api/issues should be rejected (415)")
    public void testTC_XXE_01_XmlContentTypeRejected() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_XXE_01_XmlContentTypeRejected (OWASP A05)");
        logStep("Sending application/xml to /api/issues");

        ensureAuthenticated();
        int status = probeXml("/api/issues", "<root></root>");
        logStep("Status: " + status);
        if (status == -1) throw new org.testng.SkipException("Network blocked probe");

        // 200/201 would mean the server accepts XML — surface for XXE
        // 415 (Unsupported Media Type) or 400 = good
        boolean accepted = (status >= 200 && status < 300);
        Assert.assertFalse(accepted,
            "OWASP A05 — /api/issues accepted application/xml (HTTP " + status + "). "
            + "JSON-only endpoints should reject XML to eliminate the XXE attack surface "
            + "entirely. Recommended: 415 Unsupported Media Type.");
        ExtentReportManager.logPass("XML rejected (HTTP " + status + ")");
    }

    @Test(priority = 2, description = "TC_XXE_02: POST DOCTYPE-entity payload — must not echo expanded content")
    public void testTC_XXE_02_DoctypeEntityNotResolved() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_XXE_02_DoctypeEntityNotResolved (OWASP A05)");
        logStep("Sending DOCTYPE-entity probe to /api/issues");

        ensureAuthenticated();
        String response = probeXmlAndReadBody("/api/issues", XXE_PROBE_PAYLOAD);
        logStep("Response (first 500 chars): "
            + response.substring(0, Math.min(500, response.length())));
        if (response.startsWith("ERR:")) {
            throw new org.testng.SkipException("Browser blocked probe: " + response);
        }

        // Vulnerable behavior: response body contains either:
        //   - the literal entity reference resolved to a file path / attempted file content, OR
        //   - a stack trace mentioning XML / SAX / DocumentBuilder / Xerces (parser engaged)
        String lower = response.toLowerCase();
        boolean parserEngaged = lower.contains("file://")
            || lower.contains("nonexistent-owasp-probe")
            || lower.contains("documentbuilder")
            || lower.contains("xerces")
            || lower.contains("saxparser");
        Assert.assertFalse(parserEngaged,
            "OWASP A05 — Response suggests the XML parser engaged with the DOCTYPE entity "
            + "(found references to file://, internal parser names, or echoed entity payload). "
            + "Even if the file doesn't exist, parser engagement = XXE surface. "
            + "Disable external entity resolution explicitly.");
        ExtentReportManager.logPass("Parser did not engage with DOCTYPE entity");
    }

    @Test(priority = 3, description = "TC_XXE_03: Billion-laughs lite — server must not echo expanded entities")
    public void testTC_XXE_03_BillionLaughsLiteContained() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_XXE_03_BillionLaughsLiteContained (OWASP A05)");
        logStep("Sending billion-laughs-lite probe");

        ensureAuthenticated();
        String response = probeXmlAndReadBody("/api/issues", BILLION_LAUGHS_LITE);
        logStep("Response: "
            + response.substring(0, Math.min(300, response.length())));
        if (response.startsWith("ERR:")) {
            throw new org.testng.SkipException("Browser blocked probe");
        }

        // If a vulnerable parser expanded "lol3" we'd see at least 27 instances of "lol"
        int lolCount = response.split("lol").length - 1;
        Assert.assertTrue(lolCount < 9,
            "OWASP A05 — Server appears to have expanded recursive entities ("
            + lolCount + " instances of 'lol' in response). Vulnerable parser "
            + "configuration. Disable external entity expansion at parser level.");
        ExtentReportManager.logPass("No entity expansion observed (lolCount=" + lolCount + ")");
    }

    @Test(priority = 4, description = "TC_XXE_04: /api/issues OPTIONS does not advertise application/xml as Accept")
    public void testTC_XXE_04_DoesNotAdvertiseXmlSupport() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_XXE_04_DoesNotAdvertiseXmlSupport (OWASP A05)");
        logStep("Probing OPTIONS /api/issues for advertised content types");

        ensureAuthenticated();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String script =
                "var xhr = new XMLHttpRequest();"
                + "xhr.open('OPTIONS', '/api/issues', false);"
                + "xhr.withCredentials = true;"
                + "try { xhr.send(null); } catch (e) { return 'ERR:' + e.message; }"
                + "return xhr.getAllResponseHeaders();";
            Object raw = js.executeScript(script);
            String headers = raw == null ? "" : raw.toString();
            logStep("OPTIONS headers: "
                + headers.substring(0, Math.min(400, headers.length())));

            boolean advertisesXml = headers.toLowerCase().contains("application/xml");
            Assert.assertFalse(advertisesXml,
                "OWASP A05 — /api/issues advertises application/xml in its CORS/Accept "
                + "headers. JSON-only APIs should not declare XML support — that's the "
                + "fastest way to give attackers a known-good content-type for XXE probes.");
            ExtentReportManager.logPass("XML not advertised by OPTIONS");
        } catch (Exception e) {
            throw new org.testng.SkipException("OPTIONS probe failed: " + e.getMessage());
        }
    }
}
