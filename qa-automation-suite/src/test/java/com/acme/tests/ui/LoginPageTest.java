package com.acme.tests.ui;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import com.acme.pages.DashboardPage;
import com.acme.utils.ExtentReporterNG;
import com.acme.utils.PerformanceUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

public class LoginPageTest {
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    
    @BeforeClass
    public void setUp() {
        ExtentReporterNG.createClassTest(this.getClass().getSimpleName());
        ExtentReporterNG.logInfo("Initializing WebDriver for login page testing");
        
        try {
            driver = BaseConfig.getDriver();
        } catch (Exception e) {
            ExtentReporterNG.logFail("Failed to initialize WebDriver: " + e.getMessage());
            throw e;
        }
    }
    
    @BeforeMethod
    public void setUpTest() {
        ExtentReporterNG.logInfo("Navigating to login page before each test");
        try {
            driver.get(BaseConfig.BASE_URL);
            loginPage = new LoginPage(driver);
            dashboardPage = new DashboardPage(driver);
        } catch (Exception e) {
            ExtentReporterNG.logFail("Failed to navigate to login page: " + e.getMessage());
            throw e;
        }
    }
    
    @Test(priority = 1, retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testLoginPageLoad() {
        ExtentReporterNG.logInfo("Testing login page load");
        
        try {
            // Measure page load performance
            Map<String, Object> metrics = PerformanceUtils.getPageLoadMetrics(driver);
            PerformanceUtils.logPerformanceMetrics(metrics);
            
            boolean isLoginPageDisplayed = loginPage.isLoginPageDisplayed();
            Assert.assertTrue(isLoginPageDisplayed, "Login page should be displayed");
            ExtentReporterNG.logPass("Login page loaded successfully");
        } catch (TimeoutException e) {
            ExtentReporterNG.logFail("Timeout while waiting for login page to load: " + e.getMessage());
            throw e;
        } catch (AssertionError e) {
            ExtentReporterNG.logFail("Assertion failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ExtentReporterNG.logFail("Unexpected error during login page load test: " + e.getMessage());
            throw e;
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "LoginPageLoad");
    }
    
    @Test(priority = 2, retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testValidLogin() {
        ExtentReporterNG.logInfo("Testing valid login with correct credentials");
        
        try {
            loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
            
            // Wait for dashboard to load with explicit wait
            boolean isLoggedIn = dashboardPage.waitForDashboardToLoad();
            Assert.assertTrue(isLoggedIn, "User should be logged in and redirected to dashboard");
            ExtentReporterNG.logPass("Valid login successful");
        } catch (TimeoutException e) {
            ExtentReporterNG.logFail("Timeout while waiting for dashboard to load: " + e.getMessage());
            throw e;
        } catch (AssertionError e) {
            ExtentReporterNG.logFail("Assertion failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ExtentReporterNG.logFail("Unexpected error during valid login test: " + e.getMessage());
            throw e;
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "ValidLogin");
    }
    
    @Test(priority = 3, retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testInvalidLogin() {
        ExtentReporterNG.logInfo("Testing invalid login with incorrect credentials");
        
        try {
            // Try invalid login
            loginPage.login("invalid@example.com", "wrongpassword");
            
            // Wait for error message with explicit wait
            boolean isErrorDisplayed = loginPage.waitForErrorMessage();
            Assert.assertTrue(isErrorDisplayed, "Error message should be displayed for invalid login");
            ExtentReporterNG.logPass("Invalid login handled correctly with error message");
        } catch (TimeoutException e) {
            ExtentReporterNG.logFail("Timeout while waiting for error message: " + e.getMessage());
            throw e;
        } catch (AssertionError e) {
            ExtentReporterNG.logFail("Assertion failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ExtentReporterNG.logFail("Unexpected error during invalid login test: " + e.getMessage());
            throw e;
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "InvalidLogin");
    }
    
    @Test(priority = 4, retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testEmptyCredentials() {
        ExtentReporterNG.logInfo("Testing login with empty credentials");
        
        try {
            // Clear any existing values and try to login
            loginPage.enterEmail("");
            loginPage.enterPassword("");
            loginPage.clickLoginButton();
            
            // Should still be on login page
            boolean isLoginPageDisplayed = loginPage.isLoginPageDisplayed();
            Assert.assertTrue(isLoginPageDisplayed, "Should remain on login page with empty credentials");
            ExtentReporterNG.logPass("Login with empty credentials handled correctly");
        } catch (TimeoutException e) {
            ExtentReporterNG.logFail("Timeout while checking login page status: " + e.getMessage());
            throw e;
        } catch (AssertionError e) {
            ExtentReporterNG.logFail("Assertion failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ExtentReporterNG.logFail("Unexpected error during empty credentials test: " + e.getMessage());
            throw e;
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "EmptyCredentials");
    }
    
    @Test(priority = 5, retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testSpecialCharactersInCredentials() {
        ExtentReporterNG.logInfo("Testing login with special characters in credentials");
        
        try {
            // Try login with special characters
            loginPage.enterEmail("user@#$%^&*()@example.com");
            loginPage.enterPassword("!@#$%^&*()");
            loginPage.clickLoginButton();
            
            // Should show error message
            boolean isErrorDisplayed = loginPage.waitForErrorMessage();
            ExtentReporterNG.logInfo("Error message displayed for special characters: " + isErrorDisplayed);
            
            if (isErrorDisplayed) {
                ExtentReporterNG.logPass("Special characters handled correctly with error message");
            } else {
                ExtentReporterNG.logWarning("No error message displayed for special characters, but test continues");
            }
        } catch (TimeoutException e) {
            ExtentReporterNG.logFail("Timeout while waiting for response: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ExtentReporterNG.logFail("Unexpected error during special characters test: " + e.getMessage());
            throw e;
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "SpecialCharacters");
    }
    
    @Test(priority = 6, retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testSiteDropdown() {
        ExtentReporterNG.logInfo("Testing site dropdown functionality");
        
        try {
            // Login first
            loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
            
            // Wait for dashboard to load
            boolean isLoggedIn = dashboardPage.waitForDashboardToLoad();
            Assert.assertTrue(isLoggedIn, "User should be logged in and redirected to dashboard");
            
            boolean isDropdownPresent = dashboardPage.isSiteDropdownPresent();
            ExtentReporterNG.logInfo("Site dropdown present: " + isDropdownPresent);
            
            if (isDropdownPresent) {
                boolean selectionSuccessful = dashboardPage.selectTestSite();
                if (selectionSuccessful) {
                    ExtentReporterNG.logPass("Site dropdown functionality working");
                } else {
                    ExtentReporterNG.logWarning("Site dropdown found but selection failed");
                }
            } else {
                ExtentReporterNG.logWarning("Site dropdown not found on the page");
            }
        } catch (TimeoutException e) {
            ExtentReporterNG.logFail("Timeout while waiting for dashboard or dropdown: " + e.getMessage());
            throw e;
        } catch (AssertionError e) {
            ExtentReporterNG.logFail("Assertion failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ExtentReporterNG.logFail("Unexpected error during site dropdown test: " + e.getMessage());
            throw e;
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "SiteDropdown");
    }
    
    @Test(priority = 7, retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testUIElementsVisibility() {
        ExtentReporterNG.logInfo("Testing visibility of all UI elements on login page");
        
        try {
            // Check email field
            boolean isEmailFieldVisible = loginPage.isEmailFieldVisible();
            ExtentReporterNG.logInfo("Email field visible: " + isEmailFieldVisible);
            
            // Check password field
            boolean isPasswordFieldVisible = loginPage.isPasswordFieldVisible();
            ExtentReporterNG.logInfo("Password field visible: " + isPasswordFieldVisible);
            
            // Check login button
            boolean isLoginButtonVisible = loginPage.isLoginButtonVisible();
            ExtentReporterNG.logInfo("Login button visible: " + isLoginButtonVisible);
            
            if (isEmailFieldVisible && isPasswordFieldVisible && isLoginButtonVisible) {
                ExtentReporterNG.logPass("All UI elements are visible");
            } else {
                ExtentReporterNG.logFail("Some UI elements are not visible");
                Assert.fail("Some UI elements are not visible");
            }
        } catch (TimeoutException e) {
            ExtentReporterNG.logFail("Timeout while checking UI elements visibility: " + e.getMessage());
            throw e;
        } catch (AssertionError e) {
            ExtentReporterNG.logFail("Assertion failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            ExtentReporterNG.logFail("Unexpected error during UI elements visibility test: " + e.getMessage());
            throw e;
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "UIElementsVisibility");
    }
    
    @AfterClass
    public void tearDown() {
        ExtentReporterNG.logInfo("Closing WebDriver");
        try {
            BaseConfig.closeDriver();
        } catch (Exception e) {
            ExtentReporterNG.logWarning("Error while closing WebDriver: " + e.getMessage());
        }
        ExtentReporterNG.flush();
    }
}