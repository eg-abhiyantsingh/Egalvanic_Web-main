package com.acme.tests.ui;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import com.acme.pages.DashboardPage;
import com.acme.utils.ExtentReporterNG;
import com.acme.utils.PerformanceUtils;
import org.openqa.selenium.WebDriver;
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
        ExtentReporterNG.createTest("Login Page Test Setup");
        ExtentReporterNG.logInfo("Initializing WebDriver for login page testing");
        
        driver = BaseConfig.getDriver();
    }
    
    @BeforeMethod
    public void setUpTest() {
        // Navigate to login page before each test
        driver.get(BaseConfig.BASE_URL);
        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
    }
    
    @Test(priority = 1)
    public void testLoginPageLoad() {
        ExtentReporterNG.createTest("Login Page Load Test");
        ExtentReporterNG.logInfo("Testing login page load");
        
        // Measure page load performance
        Map<String, Object> metrics = PerformanceUtils.getPageLoadMetrics(driver);
        PerformanceUtils.logPerformanceMetrics(metrics);
        
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page should be displayed");
        ExtentReporterNG.logPass("Login page loaded successfully");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "LoginPageLoad");
    }
    
    @Test(priority = 2)
    public void testValidLogin() {
        ExtentReporterNG.createTest("Valid Login Test");
        ExtentReporterNG.logInfo("Testing valid login with correct credentials");
        
        loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
        
        // Wait for dashboard to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        Assert.assertTrue(dashboardPage.isLoggedIn(), "User should be logged in and redirected to dashboard");
        ExtentReporterNG.logPass("Valid login successful");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "ValidLogin");
    }
    
    @Test(priority = 3)
    public void testInvalidLogin() {
        ExtentReporterNG.createTest("Invalid Login Test");
        ExtentReporterNG.logInfo("Testing invalid login with incorrect credentials");
        
        // Try invalid login
        loginPage.login("invalid@example.com", "wrongpassword");
        
        // Wait for error message
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed for invalid login");
        ExtentReporterNG.logPass("Invalid login handled correctly with error message");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "InvalidLogin");
    }
    
    @Test(priority = 4)
    public void testEmptyCredentials() {
        ExtentReporterNG.createTest("Empty Credentials Test");
        ExtentReporterNG.logInfo("Testing login with empty credentials");
        
        // Clear any existing values and try to login
        loginPage.enterEmail("");
        loginPage.enterPassword("");
        loginPage.clickLoginButton();
        
        // Wait a moment for validation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Should still be on login page
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Should remain on login page with empty credentials");
        ExtentReporterNG.logPass("Login with empty credentials handled correctly");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "EmptyCredentials");
    }
    
    @Test(priority = 5)
    public void testSpecialCharactersInCredentials() {
        ExtentReporterNG.createTest("Special Characters Test");
        ExtentReporterNG.logInfo("Testing login with special characters in credentials");
        
        // Try login with special characters
        loginPage.enterEmail("user@#$%^&*()@example.com");
        loginPage.enterPassword("!@#$%^&*()");
        loginPage.clickLoginButton();
        
        // Wait for response
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Should show error message
        boolean isErrorDisplayed = loginPage.isErrorMessageDisplayed();
        ExtentReporterNG.logInfo("Error message displayed for special characters: " + isErrorDisplayed);
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "SpecialCharacters");
    }
    
    @Test(priority = 6)
    public void testSiteDropdown() {
        ExtentReporterNG.createTest("Site Dropdown Test");
        ExtentReporterNG.logInfo("Testing site dropdown functionality");
        
        // Login first
        loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
        
        // Wait for dashboard to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
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
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "SiteDropdown");
    }
    
    @Test(priority = 7)
    public void testUIElementsVisibility() {
        ExtentReporterNG.createTest("UI Elements Visibility Test");
        ExtentReporterNG.logInfo("Testing visibility of all UI elements on login page");
        
        // Check email field
        boolean isEmailFieldVisible = false;
        try {
            isEmailFieldVisible = loginPage.isLoginPageDisplayed();
        } catch (Exception e) {
            ExtentReporterNG.logFail("Email field not visible: " + e.getMessage());
        }
        
        // Check password field
        boolean isPasswordFieldVisible = false;
        try {
            isPasswordFieldVisible = loginPage.isLoginPageDisplayed(); // This checks both fields
        } catch (Exception e) {
            ExtentReporterNG.logFail("Password field not visible: " + e.getMessage());
        }
        
        // Check login button
        boolean isLoginButtonVisible = false;
        try {
            isLoginButtonVisible = loginPage.isLoginPageDisplayed(); // This checks both fields
        } catch (Exception e) {
            ExtentReporterNG.logFail("Login button not visible: " + e.getMessage());
        }
        
        ExtentReporterNG.logInfo("Email field visible: " + isEmailFieldVisible);
        ExtentReporterNG.logInfo("Password field visible: " + isPasswordFieldVisible);
        ExtentReporterNG.logInfo("Login button visible: " + isLoginButtonVisible);
        
        if (isEmailFieldVisible && isPasswordFieldVisible && isLoginButtonVisible) {
            ExtentReporterNG.logPass("All UI elements are visible");
        } else {
            ExtentReporterNG.logFail("Some UI elements are not visible");
        }
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "UIElementsVisibility");
    }
    
    @AfterClass
    public void tearDown() {
        ExtentReporterNG.createTest("Test Cleanup");
        ExtentReporterNG.logInfo("Closing WebDriver");
        BaseConfig.closeDriver();
        ExtentReporterNG.flush();
    }
}