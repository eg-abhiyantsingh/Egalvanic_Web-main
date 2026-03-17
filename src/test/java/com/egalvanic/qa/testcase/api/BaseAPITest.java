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
    protected static String authToken;

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
     * Handles both JSON and non-JSON (HTML) responses gracefully.
     */
    protected String loginAndGetToken() {
        try {
            JSONObject loginPayload = new JSONObject();
            loginPayload.put("email", AppConstants.VALID_EMAIL);
            loginPayload.put("password", AppConstants.VALID_PASSWORD);

            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(loginPayload.toString())
                    .when()
                    .post("/login")
                    .then()
                    .extract().response();

            String body = response.asString();
            System.out.println("[API] Login status: " + response.getStatusCode()
                    + ", Content-Type: " + response.getContentType());

            if (response.getStatusCode() == 200) {
                // Check if response is JSON (not HTML)
                if (body != null && !body.trim().startsWith("<")) {
                    try {
                        String token = response.jsonPath().getString("token");
                        if (token == null) {
                            token = response.jsonPath().getString("access_token");
                        }
                        if (token != null) {
                            System.out.println("[API] Auth token obtained from JSON response");
                            return token;
                        }
                    } catch (Exception jsonErr) {
                        System.out.println("[API] Could not parse token from response: " + jsonErr.getMessage());
                    }
                }

                // Try extracting token from cookies
                String cookieToken = response.getCookie("token");
                if (cookieToken == null) cookieToken = response.getCookie("access_token");
                if (cookieToken == null) cookieToken = response.getCookie("auth_token");
                if (cookieToken != null) {
                    System.out.println("[API] Auth token obtained from cookie");
                    return cookieToken;
                }

                // Try extracting from response header
                String headerToken = response.getHeader("Authorization");
                if (headerToken != null && headerToken.startsWith("Bearer ")) {
                    System.out.println("[API] Auth token obtained from header");
                    return headerToken.substring(7);
                }

                System.out.println("[API] Login returned 200 but no token found in body/cookies/headers");
                System.out.println("[API] Response body (first 200 chars): "
                        + (body.length() > 200 ? body.substring(0, 200) : body));
            } else {
                System.out.println("[API] Login returned status " + response.getStatusCode());
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
