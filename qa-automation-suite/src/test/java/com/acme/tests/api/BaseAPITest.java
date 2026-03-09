package com.acme.tests.api;

import com.acme.config.BaseConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;

import static io.restassured.RestAssured.given;

public class BaseAPITest {
    protected static final String BASE_URL = BaseConfig.API_BASE_URL;
    protected static String authToken;
    
    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        // Get authentication token
        authToken = BaseConfig.getAuthToken();
    }
    
    /**
     * Create a request specification with common headers
     */
    protected RequestSpecification getRequestSpec() {
        return given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json");
    }
    
    /**
     * Create an authenticated request specification
     */
    protected RequestSpecification getAuthenticatedRequestSpec() {
        return getRequestSpec()
                .header("Authorization", "Bearer " + authToken);
    }
    
    /**
     * Log API request and response details
     */
    protected void logAPIDetails(Response response, String testName) {
        System.out.println("=== " + testName + " ===");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Time: " + response.getTime() + " ms");
        System.out.println("Response Body: " + response.asString());
        System.out.println("========================");
    }
}