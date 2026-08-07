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
                        System.out.println("[API] Auth token obtained (length: " + token.length() + ")");
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
                .header("Accept", "application/json");
    }

    /**
     * Create an authenticated request specification.
     */
    protected RequestSpecification getAuthenticatedRequestSpec() {
        return getRequestSpec()
                .header("Authorization", "Bearer " + authToken);
    }

    /**
     * Re-login once and hand back a fresh token, or null if the re-login failed.
     *
     * WHY THIS EXISTS: the class-level token is captured once in setUp(), but the backend
     * invalidates a user's earlier session when that user authenticates again. Any concurrently
     * running UI class logging in as the same account therefore kills this token mid-class, and
     * every remaining row fails with 401 {"error":"Authentication failed"} — 57 such failures in
     * one run on 2026-08-08, i.e. 91% of that run's total, none of them real. The UI already
     * self-heals a 401 via /auth/v2/refresh; the API layer needs the same courtesy.
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
