package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * API User Endpoint Tests — /users endpoint
 *
 * Tests:
 *   1. Get users with valid token → 200
 *   2. Get users without token → 401
 *   3. Get users with invalid token → 401
 *   4. Get users performance → < 3s
 */
public class UserAPITest extends BaseAPITest {

    @Test(priority = 1, description = "API: Get users with valid token returns 200")
    public void testGetUsersWithValidToken() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Users", "API_GetUsersValidToken");
        ExtentReportManager.logInfo("Testing GET /users endpoint with valid authentication token");

        Response response = getAuthenticatedRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();

        logAPIDetails(response, "Get Users with Valid Token");

        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200 for authorized request. Got: " + response.getStatusCode());
        Assert.assertTrue(response.getTime() < 5000,
                "Response time should be less than 5 seconds. Got: " + response.getTime() + "ms");

        ExtentReportManager.logInfo("Response time: " + response.getTime() + " ms");
        ExtentReportManager.logPass("Successfully retrieved users with valid token");
    }

    @Test(priority = 2, description = "API: Get users without token returns 401")
    public void testGetUsersWithoutToken() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Users", "API_GetUsersNoToken");
        ExtentReportManager.logInfo("Testing GET /users endpoint without authentication token");

        Response response = getRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();

        logAPIDetails(response, "Get Users without Token");

        Assert.assertEquals(response.getStatusCode(), 401,
                "Status code should be 401 for unauthorized request. Got: " + response.getStatusCode());

        ExtentReportManager.logPass("Correctly rejected request without authentication token");
    }

    @Test(priority = 3, description = "API: Get users with invalid token returns 401")
    public void testGetUsersWithInvalidToken() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Users", "API_GetUsersInvalidToken");
        ExtentReportManager.logInfo("Testing GET /users endpoint with invalid authentication token");

        Response response = getRequestSpec()
                .header("Authorization", "Bearer invalid_token_abc123")
                .when()
                .get("/users/")
                .then()
                .extract().response();

        logAPIDetails(response, "Get Users with Invalid Token");

        Assert.assertEquals(response.getStatusCode(), 401,
                "Status code should be 401 for invalid token. Got: " + response.getStatusCode());

        ExtentReportManager.logPass("Correctly rejected request with invalid authentication token");
    }

    @Test(priority = 4, description = "API: Get users performance < 3s")
    public void testGetUsersPerformance() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Users", "API_GetUsersPerformance");
        ExtentReportManager.logInfo("Testing performance of GET /users endpoint");

        long startTime = System.currentTimeMillis();

        Response response = getAuthenticatedRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();

        long responseTime = System.currentTimeMillis() - startTime;

        logAPIDetails(response, "Get Users Performance Test");

        Assert.assertTrue(responseTime < 3000,
                "Response time should be less than 3 seconds. Got: " + responseTime + "ms");

        ExtentReportManager.logInfo("Response time: " + responseTime + " ms");
        ExtentReportManager.logPass("GET /users endpoint performance is acceptable");
    }
}
