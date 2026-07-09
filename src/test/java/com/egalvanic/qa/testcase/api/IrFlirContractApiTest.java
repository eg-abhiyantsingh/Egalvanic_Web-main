package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <b>IR / FLIR photo pipeline — contract coverage + FLIR-IND contract WATCH.</b>
 *
 * <p>The FLIR Camera app feature (ticket: "generate visual images from IR photo data",
 * params {@code ir_photo_key, visual_photo_key, ir_photo_url, photo_type=FLIR-IND, platform})
 * is a NEW backend endpoint that is <b>not yet in the live spec</b> (checked 2026-07-09). What the
 * spec DOES already expose is the existing IR→visual pipeline this feature extends:
 * {@code POST /ir_photo/{id}/extract_visual}, {@code POST /ir_photo/extract_visual/batch},
 * {@code POST /ir_photo/create_and_extract}, plus the {@code ir_session} read models.</p>
 *
 * <p>This class does two things:</p>
 * <ol>
 *   <li><b>Covers the EXISTING IR surface now</b> — every {@code ir_photo}/{@code ir_session}
 *       mutation must be auth-gated (401/403 unauth, never 2xx = unauthenticated write into the
 *       photo pipeline, never 5xx-with-leak), and the ir_session read models must answer JSON
 *       (not the SPA shell) for an authenticated user.</li>
 *   <li><b>Watches for the FLIR-IND endpoint</b> — {@link #testFlirIndEndpointContract()} scans the
 *       live spec each run for an operation matching the new contract (path or params mentioning
 *       {@code flir} / {@code ir_photo_key}). Until it ships: SKIP with a clear "not yet deployed"
 *       message. The moment it appears in swagger, the test AUTO-ACTIVATES and asserts the
 *       acceptance criteria that are safely probeable: unauth → 401/403; authed with
 *       {@code photo_type} ≠ FLIR-IND → 400 (never 5xx, never a silent 2xx); authed with missing
 *       required params → 4xx. (The happy path needs a real S3 IR asset and is left to the
 *       backend's own unit tests per the ticket.)</li>
 * </ol>
 */
public class IrFlirContractApiTest extends BaseAPITest {

    private static final String MODULE = "API — IR/FLIR Photo Pipeline";
    private static final String ZERO = "00000000-0000-0000-0000-000000000000";

    private static final RestAssuredConfig PROBE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 30000));

    private static final Pattern LEAK = Pattern.compile(
            "(?i)psycopg2|sqlalchemy|traceback \\(most recent|programmingerror");

    private static JSONObject SPEC_PATHS;   // lazily fetched

    @BeforeClass(alwaysRun = true)
    public void base() { RestAssured.baseURI = API_BASE_URL; }

    private static synchronized JSONObject specPaths() {
        if (SPEC_PATHS != null) return SPEC_PATHS;
        Response spec = RestAssured.given().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/swagger.json").then().extract().response();
        if (spec.statusCode() != 200) throw new SkipException("swagger.json not fetchable.");
        SPEC_PATHS = new JSONObject(spec.asString()).getJSONObject("paths");
        return SPEC_PATHS;
    }

    private static String shapeOf(String body) {
        String b = body == null ? "" : body.trim();
        return b.startsWith("[") ? "array" : b.startsWith("{") ? "object" : b.startsWith("<") ? "html" : "-";
    }

    // ================================================================
    // 1a — existing IR mutations must be auth-gated
    // ================================================================

    @DataProvider(name = "irMutations")
    public Object[][] irMutations() {
        JSONObject paths = specPaths();
        List<Object[]> rows = new ArrayList<>();
        for (String raw : paths.keySet()) {
            String p = raw.startsWith("/api/") ? raw.substring(4) : raw;
            if (!(p.startsWith("/ir_photo") || p.startsWith("/ir_session") || p.startsWith("/ir-photos"))) continue;
            for (String m : paths.getJSONObject(raw).keySet()) {
                String u = m.toUpperCase(Locale.ROOT);
                if (u.matches("POST|PUT|PATCH|DELETE")) rows.add(new Object[]{u, p.replace("{photo_id}", ZERO)
                        .replace("{session_id}", ZERO).replace("{sld_id}", ZERO)});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "irMutations",
          description = "Every IR-pipeline mutation rejects unauthenticated calls (photo pipeline is auth-gated)")
    public void testIrMutationAuthGate(String method, String path) {
        ExtentReportManager.createTest(MODULE, "auth-gate", method + " " + path);
        Response r = getRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation().body("{}")
                .when().request(method, path).then().extract().response();
        int s = r.getStatusCode();
        String body = r.asString(); String shape = shapeOf(body);
        if (!"html".equals(shape)) {
            Assert.assertFalse(LEAK.matcher(body == null ? "" : body).find(),
                    method + " " + path + " leaked internals on the unauth path: " + body);
        }
        if ("html".equals(shape)) throw new SkipException(method + " " + path + " → SPA catch-all (route param mismatch), not assertable.");
        Assert.assertTrue(s == 401 || s == 403,
                "IR-pipeline mutation " + method + " " + path + " must reject unauth calls 401/403 but returned "
                        + s + " (" + shape + ") — " + (s < 300 ? "UNAUTHENTICATED WRITE into the photo pipeline" : "unexpected") + ".");
        ExtentReportManager.logPass(method + " " + path + " auth-gated (HTTP " + s + ").");
    }

    // ================================================================
    // 1b — ir_session read models answer JSON for an authenticated user
    // ================================================================

    @Test(description = "GET /ir_session answers JSON (not the SPA shell) for an authenticated user")
    public void testIrSessionListReadable() {
        ExtentReportManager.createTest(MODULE, "read-model", "GET /ir_session");
        if (!hasAuthToken()) throw new SkipException("No auth token.");
        Response r = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/ir_session").then().extract().response();
        Assert.assertEquals(r.statusCode(), 200, "GET /ir_session should be 200: " + r.asString());
        Assert.assertNotEquals(shapeOf(r.asString()), "html", "GET /ir_session returned the SPA shell, not JSON.");
        ExtentReportManager.logPass("GET /ir_session → 200 " + shapeOf(r.asString()) + " (" + r.time() + "ms).");
    }

    @Test(description = "GET /ir_photos/{unknown sld} answers a clean 4xx/empty JSON — never 5xx / SQL leak")
    public void testIrPhotosUnknownIdRobust() {
        ExtentReportManager.createTest(MODULE, "read-model", "GET /ir_photos/{zero}");
        if (!hasAuthToken()) throw new SkipException("No auth token.");
        Response r = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/ir_photos/" + ZERO).then().extract().response();
        String body = r.asString();
        Assert.assertTrue(r.statusCode() < 500,
                "GET /ir_photos/{unknown} must not 5xx (unhandled exception on bad id): HTTP " + r.statusCode() + " " + body);
        Assert.assertFalse(LEAK.matcher(body == null ? "" : body).find(),
                "GET /ir_photos/{unknown} leaked internals: " + body);
        ExtentReportManager.logPass("GET /ir_photos/{zero} → HTTP " + r.statusCode() + ", no leak.");
    }

    // ================================================================
    // 2 — FLIR-IND endpoint contract WATCH (auto-activates when deployed)
    // ================================================================

    @Test(description = "FLIR-IND visual-generation endpoint: SKIP until it ships, then assert its validation contract")
    public void testFlirIndEndpointContract() {
        ExtentReportManager.createTest(MODULE, "flir-watch", "FLIR-IND visual generation contract");
        JSONObject paths = specPaths();

        // Find a candidate op: path or raw definition mentioning flir / ir_photo_key.
        String found = null;
        for (String raw : paths.keySet()) {
            String def = paths.getJSONObject(raw).toString().toLowerCase(Locale.ROOT);
            if (raw.toLowerCase(Locale.ROOT).contains("flir") || def.contains("flir")
                    || def.contains("ir_photo_key") || def.contains("visual_photo_key")) {
                found = raw.startsWith("/api/") ? raw.substring(4) : raw;
                break;
            }
        }
        if (found == null) {
            String msg = "FLIR-IND visual-generation endpoint is NOT yet in the live spec (checked "
                    + paths.length() + " paths for flir/ir_photo_key/visual_photo_key). The existing IR→visual "
                    + "pipeline (/ir_photo/{id}/extract_visual, /extract_visual/batch, /create_and_extract) is "
                    + "covered by the auth-gate tests in this class. This test AUTO-ACTIVATES when the new "
                    + "endpoint lands in swagger — no code change needed.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }

        // Endpoint exists → assert the safely-probeable acceptance criteria.
        ExtentReportManager.logInfo("FLIR endpoint detected in spec: " + found);
        // (a) unauth must be rejected
        Response unauth = getRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation().body("{}")
                .when().post(found).then().extract().response();
        Assert.assertTrue(unauth.statusCode() == 401 || unauth.statusCode() == 403,
                "FLIR endpoint must be auth-gated; unauth POST returned " + unauth.statusCode());
        if (!hasAuthToken()) throw new SkipException("No auth token for the validation probes.");
        // (b) unsupported photo_type → 400 (per acceptance criteria), never 5xx, never 2xx
        JSONObject bad = new JSONObject().put("ir_photo_key", "qa-probe/nonexistent.jpg")
                .put("visual_photo_key", "qa-probe/nonexistent-visual.jpg")
                .put("ir_photo_url", "https://example.invalid/qa-probe.jpg")
                .put("photo_type", "NOT-A-REAL-TYPE").put("platform", "android");
        Response r1 = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .body(bad.toString()).when().post(found).then().extract().response();
        Assert.assertTrue(r1.statusCode() >= 400 && r1.statusCode() < 500,
                "photo_type != FLIR-IND must yield 400-class, got " + r1.statusCode() + ": " + r1.asString());
        Assert.assertFalse(LEAK.matcher(String.valueOf(r1.asString())).find(), "validation error leaked internals");
        // (c) missing required params → 4xx
        Response r2 = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .body("{}").when().post(found).then().extract().response();
        Assert.assertTrue(r2.statusCode() >= 400 && r2.statusCode() < 500,
                "missing required params must yield 400-class, got " + r2.statusCode());
        ExtentReportManager.logPass("FLIR-IND endpoint " + found + " passes auth-gate + validation contract "
                + "(unauth " + unauth.statusCode() + ", bad type " + r1.statusCode() + ", empty " + r2.statusCode() + ").");
    }
}
