package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * API Authentication Tests — /login endpoint
 *
 * Tests:
 *   1. Valid login → 200 + token
 *   2. Invalid login → 401
 *   3. Missing fields → 400
 *   4. Login performance → < 3s
 */
public class AuthenticationAPITest extends BaseAPITest {

    @Test(priority = 1, description = "API: Valid login returns 200 and auth token")
    public void testValidLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Authentication", "API_ValidLogin");
        ExtentReportManager.logInfo("Testing valid login with correct credentials");

        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", AppConstants.VALID_EMAIL);
        loginPayload.put("password", AppConstants.VALID_PASSWORD);

        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")
                .then()
                .extract().response();

        logAPIDetails(response, "Valid Login Test");

        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200 for successful login");
        Assert.assertTrue(response.getTime() < 5000,
                "Response time should be less than 5 seconds. Got: " + response.getTime() + "ms");

        String token = response.jsonPath().getString("token");
        if (token == null) token = response.jsonPath().getString("access_token");
        Assert.assertNotNull(token, "Authentication token should be present in response");
        Assert.assertFalse(token.isEmpty(), "Authentication token should not be empty");

        ExtentReportManager.logInfo("Token received (length: " + token.length() + ")");
        ExtentReportManager.logPass("Valid login successful with authentication token");
    }

    @Test(priority = 2, description = "API: Invalid login returns 401")
    public void testInvalidLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Authentication", "API_InvalidLogin");
        ExtentReportManager.logInfo("Testing invalid login with incorrect credentials");

        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", "invalid@example.com");
        loginPayload.put("password", "wrongpassword");

        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")
                .then()
                .extract().response();

        logAPIDetails(response, "Invalid Login Test");

        Assert.assertEquals(response.getStatusCode(), 401,
                "Status code should be 401 for invalid credentials. Got: " + response.getStatusCode());

        ExtentReportManager.logPass("Invalid login correctly rejected with 401");
    }

    @Test(priority = 3, description = "API: Login with missing fields returns 400")
    public void testLoginWithMissingFields() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Authentication", "API_MissingFieldsLogin");
        ExtentReportManager.logInfo("Testing login with missing email or password");

        // Test 1: Missing email
        JSONObject missingEmail = new JSONObject();
        missingEmail.put("password", AppConstants.VALID_PASSWORD);

        Response response1 = getRequestSpec()
                .body(missingEmail.toString())
                .when()
                .post("/login")
                .then()
                .extract().response();

        logAPIDetails(response1, "Login Missing Email");
        Assert.assertEquals(response1.getStatusCode(), 400,
                "Missing email should return 400. Got: " + response1.getStatusCode());
        ExtentReportManager.logInfo("Missing email correctly rejected: " + response1.getStatusCode());

        // Test 2: Missing password
        JSONObject missingPassword = new JSONObject();
        missingPassword.put("email", AppConstants.VALID_EMAIL);

        Response response2 = getRequestSpec()
                .body(missingPassword.toString())
                .when()
                .post("/login")
                .then()
                .extract().response();

        logAPIDetails(response2, "Login Missing Password");
        Assert.assertEquals(response2.getStatusCode(), 400,
                "Missing password should return 400. Got: " + response2.getStatusCode());
        ExtentReportManager.logInfo("Missing password correctly rejected: " + response2.getStatusCode());

        ExtentReportManager.logPass("Login with missing fields correctly rejected");
    }

    @Test(priority = 4, description = "API: Login endpoint performance < 3s")
    public void testLoginPerformance() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Authentication", "API_LoginPerformance");
        ExtentReportManager.logInfo("Testing performance of login endpoint");

        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", AppConstants.VALID_EMAIL);
        loginPayload.put("password", AppConstants.VALID_PASSWORD);

        long startTime = System.currentTimeMillis();

        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")
                .then()
                .extract().response();

        long responseTime = System.currentTimeMillis() - startTime;

        logAPIDetails(response, "Login Performance Test");

        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200 for successful login");
        Assert.assertTrue(responseTime < 3000,
                "Response time should be less than 3 seconds. Got: " + responseTime + "ms");

        ExtentReportManager.logInfo("Login response time: " + responseTime + " ms");
        ExtentReportManager.logPass("Login endpoint performance is acceptable");
    }
}
