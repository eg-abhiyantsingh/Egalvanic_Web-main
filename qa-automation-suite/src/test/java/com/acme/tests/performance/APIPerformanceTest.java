package com.acme.tests.performance;

import com.acme.config.BaseConfig;
import com.acme.utils.ExtentReporterNG;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class APIPerformanceTest {
    
    @Test(priority = 1)
    public void testLoginAPIPerformance() {
        ExtentReporterNG.createTest("Login API Performance Test");
        ExtentReporterNG.logInfo("Testing login API performance");
        
        // Create login payload
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", BaseConfig.TEST_EMAIL);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        long startTime = System.currentTimeMillis();
        
        Response response = given()
                .contentType("application/json")
                .body(loginPayload.toString())
                .when()
                .post("https://acme.egalvanic.ai/api/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        ExtentReporterNG.logInfo("Login API Response Time: " + responseTime + " ms");
        ExtentReporterNG.logInfo("Login API Status Code: " + response.getStatusCode());
        
        // Validate performance
        Assert.assertEquals(response.getStatusCode(), 200, "Login should be successful");
        Assert.assertTrue(responseTime < 3000, "Login API response time should be less than 3 seconds");
        
        ExtentReporterNG.logPass("Login API performance is acceptable");
    }
    
    @Test(priority = 2)
    public void testGetUsersAPIPerformance() {
        ExtentReporterNG.createTest("Get Users API Performance Test");
        ExtentReporterNG.logInfo("Testing get users API performance");
        
        // First get authentication token
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", BaseConfig.TEST_EMAIL);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        Response loginResponse = given()
                .contentType("application/json")
                .body(loginPayload.toString())
                .when()
                .post("https://acme.egalvanic.ai/api/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        if (loginResponse.getStatusCode() == 200) {
            String token = loginResponse.jsonPath().getString("token");
            
            long startTime = System.currentTimeMillis();
            
            Response response = given()
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("https://acme.egalvanic.ai/api/users/")  // Adjust endpoint as needed
                    .then()
                    .extract().response();
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            ExtentReporterNG.logInfo("Get Users API Response Time: " + responseTime + " ms");
            ExtentReporterNG.logInfo("Get Users API Status Code: " + response.getStatusCode());
            
            // Validate performance
            Assert.assertEquals(response.getStatusCode(), 200, "Get users should be successful");
            Assert.assertTrue(responseTime < 3000, "Get users API response time should be less than 3 seconds");
            
            ExtentReporterNG.logPass("Get users API performance is acceptable");
        } else {
            ExtentReporterNG.logWarning("Could not authenticate to test get users API performance");
        }
    }
    
    @Test(priority = 3)
    public void testAPIResponseSizePerformance() {
        ExtentReporterNG.createTest("API Response Size Performance Test");
        ExtentReporterNG.logInfo("Testing API response size performance");
        
        // First get authentication token
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", BaseConfig.TEST_EMAIL);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        Response loginResponse = given()
                .contentType("application/json")
                .body(loginPayload.toString())
                .when()
                .post("https://acme.egalvanic.ai/api/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        if (loginResponse.getStatusCode() == 200) {
            String token = loginResponse.jsonPath().getString("token");
            
            Response response = given()
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("https://acme.egalvanic.ai/api/users/")  // Adjust endpoint as needed
                    .then()
                    .extract().response();
            
            String responseBody = response.asString();
            int responseSize = responseBody.length();
            
            ExtentReporterNG.logInfo("API Response Size: " + responseSize + " characters");
            ExtentReporterNG.logInfo("API Response Time: " + response.getTime() + " ms");
            
            // Validate performance
            Assert.assertTrue(responseSize < 1000000, "API response size should be less than 1MB");
            Assert.assertTrue(response.getTime() < 3000, "API response time should be less than 3 seconds");
            
            ExtentReporterNG.logPass("API response size performance is acceptable");
        } else {
            ExtentReporterNG.logWarning("Could not authenticate to test API response size performance");
        }
    }
    
    @Test(priority = 4)
    public void testConcurrentAPIPerformance() {
        ExtentReporterNG.createTest("Concurrent API Performance Test");
        ExtentReporterNG.logInfo("Testing concurrent API requests performance");
        
        // First get authentication token
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", BaseConfig.TEST_EMAIL);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        Response loginResponse = given()
                .contentType("application/json")
                .body(loginPayload.toString())
                .when()
                .post("https://acme.egalvanic.ai/api/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        if (loginResponse.getStatusCode() == 200) {
            String token = loginResponse.jsonPath().getString("token");
            
            // Make multiple concurrent requests and measure average response time
            long totalResponseTime = 0;
            int requestCount = 5;
            
            for (int i = 0; i < requestCount; i++) {
                long startTime = System.currentTimeMillis();
                
                Response response = given()
                        .contentType("application/json")
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("https://acme.egalvanic.ai/api/users/")  // Adjust endpoint as needed
                        .then()
                        .extract().response();
                
                long endTime = System.currentTimeMillis();
                totalResponseTime += (endTime - startTime);
                
                Assert.assertEquals(response.getStatusCode(), 200, "Concurrent request " + (i+1) + " should be successful");
            }
            
            long averageResponseTime = totalResponseTime / requestCount;
            
            ExtentReporterNG.logInfo("Average Response Time for " + requestCount + " concurrent requests: " + averageResponseTime + " ms");
            
            // Validate performance
            Assert.assertTrue(averageResponseTime < 3000, "Average response time for concurrent requests should be less than 3 seconds");
            
            ExtentReporterNG.logPass("Concurrent API performance is acceptable");
        } else {
            ExtentReporterNG.logWarning("Could not authenticate to test concurrent API performance");
        }
    }
}