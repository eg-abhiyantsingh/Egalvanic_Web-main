package com.acme.tests.api;

import com.acme.utils.ExtentReporterNG;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class UserAPITest extends BaseAPITest {
    
    @Test(priority = 1)
    public void testGetUsersWithValidToken() {
        ExtentReporterNG.createTest("Get Users with Valid Token");
        ExtentReporterNG.logInfo("Testing GET /users endpoint with valid authentication token");
        
        Response response = getAuthenticatedRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();
        
        logAPIDetails(response, "Get Users with Valid Token");
        
        // Validate response
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for successful request");
        Assert.assertTrue(response.getTime() < 5000, "Response time should be less than 5 seconds");
        
        ExtentReporterNG.logPass("Successfully retrieved users with valid token");
    }
    
    @Test(priority = 2)
    public void testGetUsersWithoutToken() {
        ExtentReporterNG.createTest("Get Users without Token");
        ExtentReporterNG.logInfo("Testing GET /users endpoint without authentication token");
        
        Response response = getRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();
        
        logAPIDetails(response, "Get Users without Token");
        
        // Validate response
        Assert.assertEquals(response.getStatusCode(), 401, "Status code should be 401 for unauthorized request");
        
        ExtentReporterNG.logPass("Correctly rejected request without authentication token");
    }
    
    @Test(priority = 3)
    public void testGetUsersWithInvalidToken() {
        ExtentReporterNG.createTest("Get Users with Invalid Token");
        ExtentReporterNG.logInfo("Testing GET /users endpoint with invalid authentication token");
        
        Response response = getRequestSpec()
                .header("Authorization", "Bearer invalid_token")
                .when()
                .get("/users/")
                .then()
                .extract().response();
        
        logAPIDetails(response, "Get Users with Invalid Token");
        
        // Validate response
        Assert.assertEquals(response.getStatusCode(), 401, "Status code should be 401 for invalid token");
        
        ExtentReporterNG.logPass("Correctly rejected request with invalid authentication token");
    }
    
    @Test(priority = 4)
    public void testGetUsersPerformance() {
        ExtentReporterNG.createTest("Get Users Performance Test");
        ExtentReporterNG.logInfo("Testing performance of GET /users endpoint");
        
        long startTime = System.currentTimeMillis();
        
        Response response = getAuthenticatedRequestSpec()
                .when()
                .get("/users/")
                .then()
                .extract().response();
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        logAPIDetails(response, "Get Users Performance Test");
        
        // Validate performance
        Assert.assertTrue(responseTime < 3000, "Response time should be less than 3 seconds");
        ExtentReporterNG.logInfo("Response time: " + responseTime + " ms");
        
        ExtentReporterNG.logPass("GET /users endpoint performance is acceptable");
    }
}