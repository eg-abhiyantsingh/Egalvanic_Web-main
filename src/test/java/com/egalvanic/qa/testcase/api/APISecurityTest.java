package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

/**
 * API Security Tests
 *
 * Tests:
 *   1. SQL Injection in login parameters
 *   2. XSS in API request body
 *   3. JWT token tampering
 *   4. Missing authentication on protected endpoint
 *   5. Parameter manipulation (non-existent user ID)
 */
public class APISecurityTest extends BaseAPITest {

    @Test(priority = 1, description = "Security: SQL injection in login parameters rejected")
    public void testSQLInjectionInAPIParameters() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Security", "API_SQLInjection");
        ExtentReportManager.logInfo("Testing SQL injection payload in API parameters");

        String sqlInjectionPayload = "'; DROP TABLE users; --";

        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", sqlInjectionPayload);
        loginPayload.put("password", AppConstants.VALID_PASSWORD);
        loginPayload.put("subdomain", AppConstants.VALID_COMPANY_CODE);

        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/auth/login")
                .then()
                .extract().response();

        ExtentReportManager.logInfo("SQL Injection Response Status: " + response.getStatusCode());
        ExtentReportManager.logInfo("SQL Injection Response Time: " + response.getTime() + " ms");

        logAPIDetails(response, "SQL Injection Test");

        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 401,
                "Application should reject SQL injection attempts with 400 or 401 status. Got: "
                + response.getStatusCode());

        ExtentReportManager.logPass("SQL injection payload in API parameters handled correctly");
    }

    @Test(priority = 2, description = "Security: XSS in API body rejected")
    public void testXSSInAPIBody() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Security", "API_XSS");
        ExtentReportManager.logInfo("Testing XSS payload in API request body");

        String xssPayload = "<script>alert('XSS')</script>";

        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", xssPayload);
        loginPayload.put("password", AppConstants.VALID_PASSWORD);
        loginPayload.put("subdomain", AppConstants.VALID_COMPANY_CODE);

        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/auth/login")
                .then()
                .extract().response();

        ExtentReportManager.logInfo("XSS Response Status: " + response.getStatusCode());
        ExtentReportManager.logInfo("XSS Response Time: " + response.getTime() + " ms");

        logAPIDetails(response, "XSS Test");

        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 401,
                "Application should handle XSS payloads with 400 or 401 status. Got: "
                + response.getStatusCode());

        ExtentReportManager.logPass("XSS payload in API body handled correctly");
    }

    @Test(priority = 3, description = "Security: Tampered JWT token rejected")
    public void testJWTTokenTampering() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Security", "API_JWTTampering");
        ExtentReportManager.logInfo("Testing JWT token tampering");

        if (!hasAuthToken()) {
            ExtentReportManager.logWarning("Auth token not available — skipping JWT tampering test");
            throw new org.testng.SkipException("Auth token not available. Login API may not return JSON token.");
        }

        // Tamper with the valid token from BaseAPITest
        String tamperedToken = authToken.substring(0, authToken.length() - 5) + "XXXXX";

        Response response = given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + tamperedToken)
                .when()
                .get(API_BASE_URL + "/users/")
                .then()
                .extract().response();

        ExtentReportManager.logInfo("Tampered Token Response Status: " + response.getStatusCode());
        logAPIDetails(response, "JWT Tampering Test");

        Assert.assertEquals(response.getStatusCode(), 401,
                "Application should reject tampered JWT tokens with 401 status. Got: "
                + response.getStatusCode());

        ExtentReportManager.logPass("JWT token tampering correctly detected and rejected");
    }

    @Test(priority = 4, description = "Security: Protected endpoint rejects missing auth")
    public void testMissingAuthentication() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Security", "API_MissingAuth");
        ExtentReportManager.logInfo("Testing API endpoints without authentication");

        Response response = getRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();

        ExtentReportManager.logInfo("Missing Auth Response Status: " + response.getStatusCode());
        logAPIDetails(response, "Missing Authentication Test");

        Assert.assertEquals(response.getStatusCode(), 401,
                "Application should require authentication with 401 status. Got: "
                + response.getStatusCode());

        ExtentReportManager.logPass("Missing authentication correctly detected and rejected");
    }

    @Test(priority = 5, description = "Security: Non-existent user ID handled gracefully")
    public void testParameterManipulation() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Security", "API_ParamManipulation");
        ExtentReportManager.logInfo("Testing parameter manipulation in API requests");

        Response response = given()
                .contentType("application/json")
                .header("Authorization", "Bearer invalid_token")
                .when()
                .get(API_BASE_URL + "/users/999999")
                .then()
                .extract().response();

        ExtentReportManager.logInfo("Parameter Manipulation Response Status: " + response.getStatusCode());
        logAPIDetails(response, "Parameter Manipulation Test");

        Assert.assertTrue(response.getStatusCode() == 401 || response.getStatusCode() == 404,
                "Application should handle parameter manipulation with 401 or 404 status. Got: "
                + response.getStatusCode());

        ExtentReportManager.logPass("Parameter manipulation handled correctly");
    }
}
