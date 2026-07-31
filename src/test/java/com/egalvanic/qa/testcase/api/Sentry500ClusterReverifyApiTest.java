package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;

/**
 * <b>Sentry 500-cluster re-verification</b> — the six endpoints behind the 80 × HTTP 500
 * events recorded 22–23 Jul 2026 (66% of all 5xx in the period). Run this suite after every
 * backend deploy to answer "is this cluster fixed?" in one command:
 *
 * <pre>mvn test -DsuiteXmlFile=suite-sentry500-reverify.xml</pre>
 *
 * <p>Each probe was reproduced with curl on 2026-07-31 before being encoded (per the
 * don't-over-report rule). Live state at encoding time — the two RED tests are the point,
 * they hard-fail until the backend stops 500ing on client input:</p>
 *
 * <ul>
 *   <li><b>POST /mapping/node-session/bulk-create</b> (13 events) — STILL 500s: a valid
 *       session_id plus one nonexistent node UUID answers
 *       {@code 500 {"error":"An internal error occurred.","trace_id":…}} (FK violation
 *       reaches the driver unhandled). trace_ids d908012d… / 42eb8478… captured.</li>
 *   <li><b>POST /skm/import-xml/preview</b> (7 events) — STILL 500s two ways: (a) genuinely
 *       malformed XML → {@code 500 "Failed to preview SKM XML: mismatched tag…"} (raw expat
 *       message leaked, should be 400); (b) a LEGAL declaration without an {@code encoding}
 *       attribute ({@code <?xml version="1.0"?>}) → {@code 500 "XML declaration not
 *       well-formed: line 1, column 12"} — spec-valid input rejected.</li>
 *   <li><b>GET /lookup/procedures</b> (26 events) — NOT reproducible: 200/JSON across
 *       repeats + param variants; junk UUID answers a clean 400. Residual risk: the bare
 *       list is unbounded (~2 MB, 1 196 rows), a load-dependent 500/timeout candidate.</li>
 *   <li><b>POST /reporting/quote_preview_html</b> (19 events) — NOT reproducible: render
 *       errors are now caught per page and returned inside a 200 body
 *       ({@code render_error:"Template '' is not HTML…"}), which looks like the fix for
 *       the render-exception cluster.</li>
 *   <li><b>GET /bulk-edit/rules-status/{id}</b> (10 events) — route ABSENT on the QA build:
 *       answers the SPA HTML shell for any id while the sibling
 *       {@code POST /bulk-edit/explain-warnings} answers JSON, so the prefix is routed.</li>
 *   <li><b>POST /onboarding/jobs</b> (2 events) — NOT reproducible: every payload shape
 *       tried answers a clean 400 {@code "sld_id is required"}.</li>
 * </ul>
 */
public class Sentry500ClusterReverifyApiTest extends BaseAPITest {

    private static final String GHOST = "00000000-0000-4000-8000-000000000001";

    private String companyId, sessionId, sldId;

    @BeforeClass(dependsOnMethods = "setUp")
    public void resolveContext() {
        if (!hasAuthToken()) throw new SkipException("No auth token — login failed, cannot probe.");

        Response me = getAuthenticatedRequestSpec().when().get("/auth/me").then().extract().response();
        if (me.statusCode() == 200) {
            companyId = new JSONObject(me.asString()).optString("company_id", null);
        }
        if (companyId == null) throw new SkipException("auth/me gave no company_id.");

        try {
            Response s = getAuthenticatedRequestSpec()
                    .when().get("/company/" + companyId + "/sessions?page=1&per_page=1")
                    .then().extract().response();
            JSONArray sessions = new JSONObject(s.asString()).optJSONArray("sessions");
            if (sessions != null && sessions.length() > 0)
                sessionId = sessions.getJSONObject(0).optString("id", null);
        } catch (Exception ignored) { }

        try {
            Response r = getAuthenticatedRequestSpec()
                    .when().get("/company/" + companyId + "/slds").then().extract().response();
            JSONArray slds = new JSONObject(r.asString()).optJSONArray("slds");
            if (slds != null && slds.length() > 0)
                sldId = slds.getJSONObject(0).optString("id", null);
        } catch (Exception ignored) { }

        System.out.println("[Sentry500] context company=" + companyId
                + " session=" + sessionId + " sld=" + sldId);
    }

    private static boolean isHtmlShell(Response r) {
        String b = r.asString();
        return b != null && b.trim().startsWith("<");
    }

    private void assertNot5xx(Response r, String what) {
        logAPIDetails(r, what);
        Assert.assertTrue(r.statusCode() < 500,
                what + " answered " + r.statusCode() + " — server exception on client input. Body: "
                        + r.asString().substring(0, Math.min(300, r.asString().length())));
    }

    // ── 1. POST /mapping/node-session/bulk-create — 13 Sentry events ────────────────────

    @Test(description = "bulk-create with a nonexistent node UUID must be 4xx, never 500")
    public void bulkCreateGhostNodeNever5xx() {
        ExtentReportManager.createTest("Sentry500", "bulk-create", "ghost node UUID → 4xx not 500");
        if (sessionId == null) throw new SkipException("No session id resolved.");
        // Nonexistent UUID → nothing can be inserted, so this mutates nothing either way.
        Response r = getAuthenticatedRequestSpec()
                .body(new JSONObject().put("session_id", sessionId)
                        .put("node_ids", new JSONArray().put(GHOST)).toString())
                .when().post("/mapping/node-session/bulk-create").then().extract().response();
        assertNot5xx(r, "POST /mapping/node-session/bulk-create (ghost node)");
    }

    @Test(description = "bulk-create malformed bodies answer clean 400s (regression guard)")
    public void bulkCreateValidationStillClean() {
        ExtentReportManager.createTest("Sentry500", "bulk-create", "malformed bodies → 400");
        Response empty = getAuthenticatedRequestSpec().body("{}")
                .when().post("/mapping/node-session/bulk-create").then().extract().response();
        assertNot5xx(empty, "POST bulk-create {}");
        Assert.assertEquals(empty.statusCode() / 100, 4, "{} should be a 4xx");

        Response junk = getAuthenticatedRequestSpec()
                .body("{\"session_id\":\"not-a-uuid\",\"node_ids\":[1]}")
                .when().post("/mapping/node-session/bulk-create").then().extract().response();
        assertNot5xx(junk, "POST bulk-create junk types");
        Assert.assertEquals(junk.statusCode() / 100, 4, "junk types should be a 4xx");
    }

    // ── 2. POST /skm/import-xml/preview — 7 Sentry events ───────────────────────────────

    private Response postSkmXml(String xml) throws Exception {
        File f = File.createTempFile("skm-probe", ".xml");
        f.deleteOnExit();
        try (FileWriter w = new FileWriter(f)) { w.write(xml); }
        return getAuthenticatedRequestSpec()
                .contentType("multipart/form-data")
                .multiPart("xml_file", f, "text/xml")
                .multiPart("sld_id", sldId)
                .when().post("/skm/import-xml/preview").then().extract().response();
    }

    @Test(description = "SKM preview: malformed XML is a client error — 400, never 500")
    public void skmPreviewMalformedXmlNever5xx() throws Exception {
        ExtentReportManager.createTest("Sentry500", "skm-preview", "malformed XML → 400 not 500");
        if (sldId == null) throw new SkipException("No sld id resolved.");
        Response r = postSkmXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?><DAPPER><unclosed></DAPPER>");
        assertNot5xx(r, "POST /skm/import-xml/preview (mismatched tag)");
    }

    @Test(description = "SKM preview: legal declaration without encoding attr must be accepted")
    public void skmPreviewLegalDeclarationNever5xx() throws Exception {
        ExtentReportManager.createTest("Sentry500", "skm-preview", "<?xml version=\"1.0\"?> is legal");
        if (sldId == null) throw new SkipException("No sld id resolved.");
        // encoding= is OPTIONAL per the XML spec; control <?xml version="1.0" encoding="UTF-8"?> passes.
        Response r = postSkmXml("<?xml version=\"1.0\"?>\n<DAPPER><PROJECT NAME=\"qa-probe\"/></DAPPER>\n");
        assertNot5xx(r, "POST /skm/import-xml/preview (no encoding attr)");
    }

    // ── 3. GET /lookup/procedures — 26 Sentry events ─────────────────────────────────────

    @Test(description = "lookup/procedures answers 200 JSON; junk param answers 400 not 500")
    public void lookupProceduresHealthy() {
        ExtentReportManager.createTest("Sentry500", "lookup-procedures", "200 JSON + clean 400");
        Response ok = getAuthenticatedRequestSpec().when().get("/lookup/procedures")
                .then().extract().response();
        Assert.assertEquals(ok.statusCode(), 200, "GET /lookup/procedures");
        Assert.assertFalse(isHtmlShell(ok), "lookup/procedures returned the SPA shell");

        Response junk = getAuthenticatedRequestSpec()
                .when().get("/lookup/procedures?node_class_id=not-a-uuid").then().extract().response();
        assertNot5xx(junk, "GET /lookup/procedures?node_class_id=not-a-uuid");
    }

    // ── 4. POST /reporting/quote_preview_html — 19 Sentry events ─────────────────────────

    @Test(description = "quote_preview_html never 5xx on empty or unknown quote_id")
    public void quotePreviewNever5xx() {
        ExtentReportManager.createTest("Sentry500", "quote-preview", "empty/ghost quote_id → never 5xx");
        Response empty = getAuthenticatedRequestSpec().body("{}")
                .when().post("/reporting/quote_preview_html").then().extract().response();
        assertNot5xx(empty, "POST quote_preview_html {}");

        Response ghost = getAuthenticatedRequestSpec()
                .body(new JSONObject().put("quote_id", GHOST).toString())
                .when().post("/reporting/quote_preview_html").then().extract().response();
        assertNot5xx(ghost, "POST quote_preview_html ghost quote_id");
    }

    // ── 5. GET /bulk-edit/rules-status/{id} — 10 Sentry events ───────────────────────────

    @Test(description = "rules-status: never 5xx; warn while the route is absent (SPA shell)")
    public void rulesStatusRouteNever5xx() {
        ExtentReportManager.createTest("Sentry500", "rules-status", "route reachability + never 5xx");
        Response r = getAuthenticatedRequestSpec()
                .when().get("/bulk-edit/rules-status/" + GHOST).then().extract().response();
        assertNot5xx(r, "GET /bulk-edit/rules-status/{ghost}");
        if (isHtmlShell(r)) {
            String msg = "GET /bulk-edit/rules-status/{id} answers the SPA HTML shell — route not"
                    + " deployed on this QA build (sibling POST /bulk-edit/explain-warnings answers"
                    + " JSON). The 10 Sentry events cannot be re-verified here until it ships.";
            System.out.println("[Sentry500][WARNING] " + msg);
            ExtentReportManager.logWarning(msg);
        }
    }

    // ── 6. POST /onboarding/jobs — 2 Sentry events ───────────────────────────────────────

    @Test(description = "onboarding/jobs answers clean 4xx on empty/ghost payloads, never 500")
    public void onboardingJobsNever5xx() {
        ExtentReportManager.createTest("Sentry500", "onboarding-jobs", "empty/ghost payloads → 4xx");
        Response empty = getAuthenticatedRequestSpec().body("{}")
                .when().post("/onboarding/jobs").then().extract().response();
        assertNot5xx(empty, "POST /onboarding/jobs {}");
        Assert.assertEquals(empty.statusCode() / 100, 4, "{} should be a 4xx");

        Response ghost = getAuthenticatedRequestSpec()
                .body(new JSONObject().put("sld_id", GHOST).toString())
                .when().post("/onboarding/jobs").then().extract().response();
        assertNot5xx(ghost, "POST /onboarding/jobs ghost sld_id");
    }
}
