package com.acme.tests.security;

import com.acme.config.BaseConfig;
import com.acme.utils.ExtentReporterNG;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class APISecurityTest {
    
    @Test(priority = 1)
    public void testSQLInjectionInAPIParameters() {
        ExtentReporterNG.createTest("SQL Injection Test - API Parameters");
        ExtentReporterNG.logInfo("Testing SQL injection payload in API parameters");
        
        String sqlInjectionPayload = "'; DROP TABLE users; --";
        
        // Test with SQL injection in email parameter
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", sqlInjectionPayload);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        Response response = given()
                .contentType("application/json")
                .body(loginPayload.toString())
                .when()
                .post("https://acme.egalvanic.ai/api/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        ExtentReporterNG.logInfo("SQL Injection Response Status: " + response.getStatusCode());
        ExtentReporterNG.logInfo("SQL Injection Response Time: " + response.getTime() + " ms");
        
        // Application should reject SQL injection attempts
        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 401, 
            "Application should reject SQL injection attempts with 400 or 401 status");
        
        ExtentReporterNG.logPass("SQL injection payload in API parameters handled correctly");
    }
    
    @Test(priority = 2)
    public void testXSSInAPIBody() {
        ExtentReporterNG.createTest("XSS Test - API Body");
        ExtentReporterNG.logInfo("Testing XSS payload in API request body");
        
        String xssPayload = "<script>alert('XSS')</script>";
        
        // Test with XSS payload in email parameter
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", xssPayload);
        loginPayload.put("password", BaseConfig.TEST_PASSWORD);
        
        Response response = given()
                .contentType("application/json")
                .body(loginPayload.toString())
                .when()
                .post("https://acme.egalvanic.ai/api/login")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        ExtentReporterNG.logInfo("XSS Response Status: " + response.getStatusCode());
        ExtentReporterNG.logInfo("XSS Response Time: " + response.getTime() + " ms");
        
        // Application should sanitize or reject XSS payloads
        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 401, 
            "Application should handle XSS payloads with 400 or 401 status");
        
        ExtentReporterNG.logPass("XSS payload in API body handled correctly");
    }
    
    @Test(priority = 3)
    public void testJWTTokenTampering() {
        ExtentReporterNG.createTest("JWT Token Tampering Test");
        ExtentReporterNG.logInfo("Testing JWT token tampering");
        
        // Get a valid token first
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
            String validToken = loginResponse.jsonPath().getString("token");
            
            // Tamper with the token
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
            
            // Try to use tampered token
            Response response = given()
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + tamperedToken)
                    .when()
                    .get("https://acme.egalvanic.ai/api/users/")  // Adjust endpoint as needed
                    .then()
                    .extract().response();
            
            ExtentReporterNG.logInfo("Tampered Token Response Status: " + response.getStatusCode());
            
            // Application should reject tampered tokens
            Assert.assertEquals(response.getStatusCode(), 401, 
                "Application should reject tampered JWT tokens with 401 status");
            
            ExtentReporterNG.logPass("JWT token tampering correctly detected and rejected");
        } else {
            ExtentReporterNG.logWarning("Could not obtain valid token for tampering test");
        }
    }
    
    @Test(priority = 4)
    public void testMissingAuthentication() {
        ExtentReporterNG.createTest("Missing Authentication Test");
        ExtentReporterNG.logInfo("Testing API endpoints without authentication");
        
        // Try to access protected endpoint without authentication
        Response response = given()
                .contentType("application/json")
                .when()
                .get("https://acme.egalvanic.ai/api/users/")  // Adjust endpoint as needed
                .then()
                .extract().response();
        
        ExtentReporterNG.logInfo("Missing Auth Response Status: " + response.getStatusCode());
        
        // Application should require authentication
        Assert.assertEquals(response.getStatusCode(), 401, 
            "Application should require authentication with 401 status");
        
        ExtentReporterNG.logPass("Missing authentication correctly detected and rejected");
    }
    
    @Test(priority = 5)
    public void testParameterManipulation() {
        ExtentReporterNG.createTest("Parameter Manipulation Test");
        ExtentReporterNG.logInfo("Testing parameter manipulation in API requests");
        
        // Try to access user data with manipulated user ID
        Response response = given()
                .contentType("application/json")
                .header("Authorization", "Bearer invalid_token")
                .when()
                .get("https://acme.egalvanic.ai/api/users/999999")  // Non-existent user ID
                .then()
                .extract().response();
        
        ExtentReporterNG.logInfo("Parameter Manipulation Response Status: " + response.getStatusCode());
        
        // Application should handle invalid parameters gracefully
        Assert.assertTrue(response.getStatusCode() == 401 || response.getStatusCode() == 404, 
            "Application should handle parameter manipulation with 401 or 404 status");
        
        ExtentReporterNG.logPass("Parameter manipulation handled correctly");
    }
}