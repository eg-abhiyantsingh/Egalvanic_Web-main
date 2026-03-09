package com.acme.config;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static io.restassured.RestAssured.given;

public class BaseConfig {
    // Base URL for the application
    public static final String BASE_URL = getConfigProperty("BASE_URL", "https://acme.egalvanic.ai");
    public static final String API_BASE_URL = getConfigProperty("API_BASE_URL", "https://acme.egalvanic.ai/api");
    
    // Test credentials
    public static final String TEST_EMAIL = getConfigProperty("TEST_EMAIL", "rahul+acme@egalvanic.com");
    public static final String TEST_PASSWORD = getConfigProperty("TEST_PASSWORD", "RP@egalvanic123");
    
    // WebDriver instance
    protected static ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    
    // Authentication token (to be set after login)
    protected static ThreadLocal<String> authTokenThreadLocal = new ThreadLocal<>();
    
    // Configuration properties
    private static Properties configProperties = new Properties();
    
    static {
        loadConfigProperties();
    }
    
    /**
     * Load configuration properties from config file
     */
    private static void loadConfigProperties() {
        try {
            FileInputStream fis = new FileInputStream("src/main/resources/config.properties");
            configProperties.load(fis);
            fis.close();
        } catch (IOException e) {
            System.out.println("Config file not found, using default values");
        }
    }
    
    /**
     * Get configuration property with default value
     */
    private static String getConfigProperty(String key, String defaultValue) {
        return configProperties.getProperty(key, defaultValue);
    }
    
    /**
     * Get browser type from system property or config
     */
    public static String getBrowserType() {
        return System.getProperty("browser", getConfigProperty("BROWSER", "chrome")).toLowerCase();
    }
    
    /**
     * Initialize WebDriver based on browser type
     */
    public static void initializeDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            String browserType = getBrowserType();
            switch (browserType) {
                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    firefoxOptions.addArguments("--start-maximized");
                    driver = new FirefoxDriver(firefoxOptions);
                    break;
                    
                case "edge":
                    WebDriverManager.edgedriver().setup();
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.addArguments("--start-maximized");
                    driver = new EdgeDriver(edgeOptions);
                    break;
                    
                case "safari":
                    WebDriverManager.safaridriver().setup();
                    SafariOptions safariOptions = new SafariOptions();
                    driver = new SafariDriver(safariOptions);
                    break;
                    
                case "chrome":
                default:
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--start-maximized");
                    chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    chromeOptions.setExperimentalOption("useAutomationExtension", false);
                    chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
                    driver = new ChromeDriver(chromeOptions);
                    break;
            }
            driverThreadLocal.set(driver);
        }
    }
    
    /**
     * Close WebDriver
     */
    public static void closeDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }
    
    /**
     * Get WebDriver instance
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            initializeDriver();
            driver = driverThreadLocal.get();
        }
        return driver;
    }
    
    /**
     * Perform login and get authentication token
     */
    public static String loginAndGetToken() {
        String authToken = authTokenThreadLocal.get();
        if (authToken != null) {
            return authToken;
        }
        
        try {
            // Set base URI for REST Assured
            RestAssured.baseURI = API_BASE_URL;
            
            // Create login request payload
            String loginPayload = "{\n" +
                    "  \"email\": \"" + TEST_EMAIL + "\",\n" +
                    "  \"password\": \"" + TEST_PASSWORD + "\"\n" +
                    "}";
            
            // Send POST request to login endpoint
            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(loginPayload)
                    .when()
                    .post("/login")
                    .then()
                    .extract().response();
            
            // Extract token from response
            if (response.getStatusCode() == 200) {
                authToken = response.jsonPath().getString("token");
                if (authToken == null) {
                    // Try alternative token paths
                    authToken = response.jsonPath().getString("access_token");
                }
                authTokenThreadLocal.set(authToken);
                return authToken;
            }
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get authentication token
     */
    public static String getAuthToken() {
        String authToken = authTokenThreadLocal.get();
        if (authToken == null) {
            authToken = loginAndGetToken();
            authTokenThreadLocal.set(authToken);
        }
        return authToken;
    }
    
    /**
     * Clear authentication token
     */
    public static void clearAuthToken() {
        authTokenThreadLocal.remove();
    }
}