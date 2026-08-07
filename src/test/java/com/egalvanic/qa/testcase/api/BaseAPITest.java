package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static io.restassured.RestAssured.given;

/**
 * Base class for all API tests.
 * Provides RestAssured configuration, authentication token management,
 * and common request specification builders.
 *
 * API Base URL: {BASE_URL}/api  (e.g., https://acme.qa.egalvanic.ai/api)
 */
public class BaseAPITest {

    protected static final String API_BASE_URL = AppConstants.BASE_URL + "/api";
    // volatile: written once in @BeforeClass, read from many probe threads under the catalog's
    // parallel @DataProvider — make the write's visibility unconditional across threads.
    protected static volatile String authToken;

    /** Wall-clock ms at which {@link #authToken} stops being accepted (0 = unknown/not yet set). */
    private static volatile long tokenExpiresAtMs = 0L;

    /** Fallback lifetime if the login body omits {@code expires_in}. QA measures 3600s. */
    private static final long DEFAULT_TOKEN_LIFETIME_MS = 3_600_000L;

    /**
     * Refresh the token this far BEFORE it actually expires, so a request issued just under the
     * wire cannot land on the far side of expiry while in flight.
     */
    private static final long TOKEN_REFRESH_MARGIN_MS = 300_000L;   // 5 min

    @BeforeSuite
    public void suiteSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     eGalvanic Web - API Test Suite");
        System.out.println("==============================================================");
        System.out.println("     API Base URL: " + API_BASE_URL);
        System.out.println();

        ExtentReportManager.initReports();
    }

    @AfterSuite
    public void suiteTeardown() {
        ExtentReportManager.flushReports();

        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     API Test Suite Complete");
        System.out.println("==============================================================");
    }

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = API_BASE_URL;
        // Get authentication token
        authToken = loginAndGetToken();
    }

    /**
     * Perform login via API and return the auth token.
     * Endpoint: POST /api/auth/login
     * Payload: { "email": "...", "password": "...", "subdomain": "acme" }
     * Response: { "access_token": "...", "expires_in": 3600, ... }
     */
    protected String loginAndGetToken() {
        try {
            JSONObject loginPayload = new JSONObject();
            loginPayload.put("email", AppConstants.VALID_EMAIL);
            loginPayload.put("password", AppConstants.VALID_PASSWORD);
            loginPayload.put("subdomain", AppConstants.VALID_COMPANY_CODE);

            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(loginPayload.toString())
                    .when()
                    .post("/auth/login")
                    .then()
                    .extract().response();

            System.out.println("[API] Login status: " + response.getStatusCode()
                    + ", Content-Type: " + response.getContentType());

            if (response.getStatusCode() == 200) {
                String body = response.asString();
                if (body != null && !body.trim().startsWith("<")) {
                    String token = response.jsonPath().getString("access_token");
                    if (token == null) token = response.jsonPath().getString("token");
                    if (token != null) {
                        // Record when this token dies. MEASURED 2026-08-08 against the QA host: the
                        // JWT carries exp - iat = 3600s exactly, and the login body reports the same
                        // via expires_in. A full parallel suite runs LONGER than that, so a token
                        // cached once at class setUp() is guaranteed to be dead for later classes —
                        // which is what produced the 57-call 401 storm in WorkTypeCatalogApiTest.
                        Integer expiresIn = null;
                        try {
                            expiresIn = response.jsonPath().getInt("expires_in");
                        } catch (Exception ignored) { /* field absent — fall back below */ }
                        long lifetimeMs = (expiresIn != null && expiresIn > 0)
                                ? expiresIn * 1000L
                                : DEFAULT_TOKEN_LIFETIME_MS;
                        tokenExpiresAtMs = System.currentTimeMillis() + lifetimeMs;
                        System.out.println("[API] Auth token obtained (length: " + token.length()
                                + ", valid for " + (lifetimeMs / 60000) + " min)");
                        return token;
                    }
                }
                System.out.println("[API] Login returned 200 but no token found");
            } else {
                System.out.println("[API] Login returned status " + response.getStatusCode()
                        + ": " + response.asString());
            }
        } catch (Exception e) {
            System.out.println("[API] Login failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if auth token is available. Use in tests that require authentication.
     */
    protected boolean hasAuthToken() {
        return authToken != null && !authToken.isEmpty();
    }

    /**
     * Safely extract a token from a login response, handling HTML/non-JSON.
     */
    protected String extractTokenFromResponse(Response response) {
        if (response.getStatusCode() != 200) return null;

        String body = response.asString();
        if (body == null || body.trim().startsWith("<")) return null;

        try {
            String token = response.jsonPath().getString("token");
            if (token == null) token = response.jsonPath().getString("access_token");
            return token;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create a request specification with common headers (no auth).
     */
    protected RequestSpecification getRequestSpec() {
        return given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .filter(SPA_SHELL_WARNING);
    }

    /**
     * Loudly flags any API response that is actually the SPA's index.html.
     *
     * WHY THIS EXISTS: unmatched paths under {@code /api} do NOT 404 — they fall through to the
     * app's catch-all and return {@code 200 text/html} (the ~2KB app shell). An API test that probes
     * a path which does not exist therefore gets a perfectly ordinary-looking 200, and can conclude
     * anything at all from it. This produced TWO fabricated Broken-Access-Control findings that were
     * filed as real product defects (BUG-E, and TC_ACC's flat-endpoint test — both retracted
     * 2026-08-08): they asserted "must be 401" against a static HTML page, so they could never pass.
     *
     * The direction that is easy to miss: the same trap produces false PASSES just as readily. A test
     * asserting {@code statusCode() != 200} to prove a route is gated will happily "pass" against the
     * SPA shell even if the real route has genuinely lost its auth guard.
     *
     * REST Assured gives no per-response hook, so this filter prints the warning centrally for every
     * request built through {@link #getRequestSpec()}. Grep runs for "SPA SHELL" to catch the trap.
     */
    private static final io.restassured.filter.Filter SPA_SHELL_WARNING =
            (requestSpec, responseSpec, ctx) -> {
                Response r = ctx.next(requestSpec, responseSpec);
                try {
                    String ct = String.valueOf(r.getContentType()).toLowerCase();
                    if (ct.contains("text/html")) {
                        System.out.println("[API] *** SPA SHELL *** " + requestSpec.getMethod() + " "
                                + requestSpec.getURI() + " -> " + r.getStatusCode()
                                + " text/html. This path has NO API handler, so its status code says"
                                + " NOTHING about auth or behaviour. Do not assert on it — verify the"
                                + " real route first (see the BUG-E retraction in BUGS.md).");
                    }
                } catch (Exception ignored) { /* never let diagnostics break a test */ }
                return r;
            };

    /**
     * Create an authenticated request specification.
     */
    protected RequestSpecification getAuthenticatedRequestSpec() {
        return getRequestSpec()
                .header("Authorization", "Bearer " + freshToken());
    }

    /**
     * The cached token, re-minted first if it is expired or about to be.
     *
     * WHY: the QA token lives exactly 3600s (measured 2026-08-08 — the JWT's exp-iat and the login
     * body's expires_in agree). A full suite runs longer than that, so a token captured once in
     * setUp() is simply DEAD by the time later API classes execute, and every call returns 401. The
     * failure text then blames the endpoint ("async-delete semantics changed: now returns 401"),
     * which is how one expired token turned into 57 bogus contract failures.
     *
     * Refreshing on a clock is strictly better than reacting to 401s: withAuthRetry() still exists
     * as a safety net, but by the time it fires the assertion has already seen a wrong status.
     */
    protected static synchronized String freshToken() {
        boolean expiring = tokenExpiresAtMs > 0
                && System.currentTimeMillis() > (tokenExpiresAtMs - TOKEN_REFRESH_MARGIN_MS);
        if (authToken == null || authToken.isEmpty() || expiring) {
            if (expiring) {
                System.out.println("[API] Token is within " + (TOKEN_REFRESH_MARGIN_MS / 60000)
                        + " min of expiry — refreshing before use");
            }
            String fresh = new BaseAPITest().loginAndGetToken();
            if (fresh != null && !fresh.isEmpty()) authToken = fresh;
        }
        return authToken;
    }

    /**
     * Re-login once and hand back a fresh token, or null if the re-login failed.
     *
     * WHY THIS EXISTS: the class-level token is captured once in setUp() and then simply EXPIRES —
     * the QA token lives exactly 3600s. A full suite runs longer than that, so later API classes
     * authenticate with a dead token and every call returns 401 {"error":"Authentication failed"}.
     * 57 such failures in one run on 2026-08-08, i.e. 91% of that run's total, none of them real.
     *
     * CORRECTION (verified 2026-08-08): this was first attributed to the backend invalidating a
     * user's earlier session when that user logs in again. That is FALSE — measured directly:
     * login #1 -> token1, login #2 -> token2 (different), and token1 STILL returns 200 on
     * /auth/v2/me afterwards. Concurrent logins as the same account are harmless. The cause is
     * plain 60-minute expiry, which is why {@link #freshToken()} now refreshes on a clock and this
     * method is only the safety net behind it.
     */
    protected static synchronized String reauthenticate() {
        String fresh = new BaseAPITest().loginAndGetToken();
        if (fresh != null && !fresh.isEmpty()) {
            authToken = fresh;
            System.out.println("[API] Token was rejected (401) — re-authenticated successfully");
            return fresh;
        }
        System.out.println("[API] Token was rejected (401) and re-authentication ALSO failed");
        return null;
    }

    /**
     * Run an authenticated call and, if it comes back 401/403 because the cached token was
     * invalidated elsewhere, re-login once and run it again. Use for every authenticated API
     * assertion so a concurrent login can never masquerade as a product failure.
     */
    protected Response withAuthRetry(java.util.function.Supplier<Response> call) {
        Response r = call.get();
        if (r != null && (r.statusCode() == 401 || r.statusCode() == 403)) {
            String body = r.asString();
            if (body == null || body.toLowerCase().contains("authentication failed")
                    || body.toLowerCase().contains("unauthorized") || r.statusCode() == 401) {
                if (reauthenticate() != null) {
                    return call.get();
                }
            }
        }
        return r;
    }

    /**
     * Log API request and response details to console and report.
     */
    protected void logAPIDetails(Response response, String testName) {
        System.out.println("=== " + testName + " ===");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Time: " + response.getTime() + " ms");
        String body = response.asString();
        // Truncate long responses for console
        if (body.length() > 500) {
            System.out.println("Response Body: " + body.substring(0, 500) + "... [truncated]");
        } else {
            System.out.println("Response Body: " + body);
        }
        System.out.println("========================");
    }
}
