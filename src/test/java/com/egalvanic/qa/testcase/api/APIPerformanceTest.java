package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * API Performance Tests
 *
 * Tests:
 *   1. Login API performance (< 3s)
 *   2. Get users API performance (< 3s)
 *   3. API response size (< 1MB)
 *   4. Sequential request performance (avg < 3s)
 */
public class APIPerformanceTest extends BaseAPITest {

    @Test(priority = 1, description = "Performance: Login API responds within 3s")
    public void testLoginAPIPerformance() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Performance", "API_LoginPerf");
        ExtentReportManager.logInfo("Testing login API performance");

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

        logAPIDetails(response, "Login API Performance");

        ExtentReportManager.logInfo("Login API Response Time: " + responseTime + " ms");
        ExtentReportManager.logInfo("Login API Status Code: " + response.getStatusCode());

        Assert.assertEquals(response.getStatusCode(), 200,
                "Login should be successful. Got: " + response.getStatusCode());
        Assert.assertTrue(responseTime < 3000,
                "Login API response time should be less than 3 seconds. Got: " + responseTime + "ms");

        ExtentReportManager.logPass("Login API performance is acceptable (" + responseTime + "ms)");
    }

    @Test(priority = 2, description = "Performance: Get users API responds within 3s")
    public void testGetUsersAPIPerformance() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Performance", "API_GetUsersPerf");
        ExtentReportManager.logInfo("Testing get users API performance");

        if (!hasAuthToken()) {
            ExtentReportManager.logWarning("Auth token not available — skipping test");
            throw new SkipException("Auth token not available. Login API may not return JSON token.");
        }

        long startTime = System.currentTimeMillis();

        Response response = getAuthenticatedRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();

        long responseTime = System.currentTimeMillis() - startTime;

        logAPIDetails(response, "Get Users API Performance");

        ExtentReportManager.logInfo("Get Users API Response Time: " + responseTime + " ms");
        ExtentReportManager.logInfo("Get Users API Status Code: " + response.getStatusCode());

        Assert.assertEquals(response.getStatusCode(), 200,
                "Get users should be successful. Got: " + response.getStatusCode());
        Assert.assertTrue(responseTime < 3000,
                "Get users API response time should be less than 3 seconds. Got: " + responseTime + "ms");

        ExtentReportManager.logPass("Get users API performance is acceptable (" + responseTime + "ms)");
    }

    @Test(priority = 3, description = "Performance: API response size < 1MB")
    public void testAPIResponseSizePerformance() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Performance", "API_ResponseSize");
        ExtentReportManager.logInfo("Testing API response size performance");

        if (!hasAuthToken()) {
            ExtentReportManager.logWarning("Auth token not available — skipping test");
            throw new SkipException("Auth token not available. Login API may not return JSON token.");
        }

        Response response = getAuthenticatedRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();

        String responseBody = response.asString();
        int responseSize = responseBody.length();

        ExtentReportManager.logInfo("API Response Size: " + responseSize + " characters");
        ExtentReportManager.logInfo("API Response Time: " + response.getTime() + " ms");

        Assert.assertTrue(responseSize < 1000000,
                "API response size should be less than 1MB. Got: " + responseSize + " chars");
        Assert.assertTrue(response.getTime() < 3000,
                "API response time should be less than 3 seconds. Got: " + response.getTime() + "ms");

        ExtentReportManager.logPass("API response size is acceptable (" + responseSize + " chars)");
    }

    @Test(priority = 4, description = "Performance: Sequential requests avg < 3s")
    public void testConcurrentAPIPerformance() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "API Performance", "API_ConcurrentPerf");
        ExtentReportManager.logInfo("Testing sequential API requests performance");

        if (!hasAuthToken()) {
            ExtentReportManager.logWarning("Auth token not available — skipping test");
            throw new SkipException("Auth token not available. Login API may not return JSON token.");
        }

        int requestCount = 5;
        long totalResponseTime = 0;

        for (int i = 0; i < requestCount; i++) {
            long startTime = System.currentTimeMillis();

            Response response = getAuthenticatedRequestSpec()
                    .when()
                    .get("/users/")
                    .then()
                    .extract().response();

            long elapsed = System.currentTimeMillis() - startTime;
            totalResponseTime += elapsed;

            Assert.assertEquals(response.getStatusCode(), 200,
                    "Request " + (i + 1) + " should be successful. Got: " + response.getStatusCode());
            ExtentReportManager.logInfo("Request " + (i + 1) + ": " + elapsed + " ms");
        }

        long averageResponseTime = totalResponseTime / requestCount;

        ExtentReportManager.logInfo("Average Response Time for " + requestCount
                + " sequential requests: " + averageResponseTime + " ms");

        Assert.assertTrue(averageResponseTime < 3000,
                "Average response time should be less than 3 seconds. Got: " + averageResponseTime + "ms");

        ExtentReportManager.logPass("Sequential API performance is acceptable (avg " + averageResponseTime + "ms)");
    }
}
