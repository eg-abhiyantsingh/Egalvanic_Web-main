package com.acme.tests.api;

import com.acme.config.BaseConfig;
import com.acme.utils.ExtentReporterNG;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class AuthenticationTest extends BaseAPITest {
    
    @Test(priority = 1)
    public void testValidLogin() {
        ExtentReporterNG.createTest("Valid Login Test");
        ExtentReporterNG.logInfo("Testing valid login with correct credentials");
        
        // Create login payload
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", BaseConfig.TEST_EMAIL);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        logAPIDetails(response, "Valid Login Test");
        
        // Validate response
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for successful login");
        Assert.assertTrue(response.getTime() < 5000, "Response time should be less than 5 seconds");
        
        // Check if token is present in response
        String token = response.jsonPath().getString("token");
        Assert.assertNotNull(token, "Authentication token should be present in response");
        Assert.assertFalse(token.isEmpty(), "Authentication token should not be empty");
        
        ExtentReporterNG.logPass("Valid login successful with authentication token");
    }
    
    @Test(priority = 2)
    public void testInvalidLogin() {
        ExtentReporterNG.createTest("Invalid Login Test");
        ExtentReporterNG.logInfo("Testing invalid login with incorrect credentials");
        
        // Create invalid login payload
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", "invalid@example.com");
        loginPayload.put("password", "wrongpassword");
        
        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        logAPIDetails(response, "Invalid Login Test");
        
        // Validate response
        Assert.assertEquals(response.getStatusCode(), 401, "Status code should be 401 for invalid credentials");
        
        ExtentReporterNG.logPass("Invalid login correctly rejected");
    }
    
    @Test(priority = 3)
    public void testLoginWithMissingFields() {
        ExtentReporterNG.createTest("Login with Missing Fields Test");
        ExtentReporterNG.logInfo("Testing login with missing email or password");
        
        // Create login payload with missing email
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        logAPIDetails(response, "Login with Missing Email Test");
        
        // Validate response
        Assert.assertEquals(response.getStatusCode(), 400, "Status code should be 400 for missing fields");
        
        // Create login payload with missing password
        loginPayload = new JSONObject();
        loginPayload.put("email", BaseConfig.TEST_EMAIL);
        
        response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        logAPIDetails(response, "Login with Missing Password Test");
        
        // Validate response
        Assert.assertEquals(response.getStatusCode(), 400, "Status code should be 400 for missing fields");
        
        ExtentReporterNG.logPass("Login with missing fields correctly rejected");
    }
    
    @Test(priority = 4)
    public void testLoginPerformance() {
        ExtentReporterNG.createTest("Login Performance Test");
        ExtentReporterNG.logInfo("Testing performance of login endpoint");
        
        // Create login payload
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", BaseConfig.TEST_EMAIL);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        long startTime = System.currentTimeMillis();
        
        Response response = getRequestSpec()
                .body(loginPayload.toString())
                .when()
                .post("/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        logAPIDetails(response, "Login Performance Test");
        
        // Validate performance
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for successful login");
        Assert.assertTrue(responseTime < 3000, "Response time should be less than 3 seconds");
        ExtentReporterNG.logInfo("Login response time: " + responseTime + " ms");
        
        ExtentReporterNG.logPass("Login endpoint performance is acceptable");
    }
}