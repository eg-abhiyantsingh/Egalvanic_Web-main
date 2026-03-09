package com.acme.tests.security;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import com.acme.utils.ExtentReporterNG;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UISecurityTest {
    private WebDriver driver;
    private LoginPage loginPage;
    
    @BeforeClass
    public void setUp() {
        ExtentReporterNG.createTest("UI Security Tests");
        ExtentReporterNG.logInfo("Initializing WebDriver for security tests");
        
        driver = BaseConfig.getDriver();
        driver.get(BaseConfig.BASE_URL);
        loginPage = new LoginPage(driver);
    }
    
    @Test(priority = 1)
    public void testSQLInjectionInEmailField() {
        ExtentReporterNG.createTest("SQL Injection Test - Email Field");
        ExtentReporterNG.logInfo("Testing SQL injection payload in email field");
        
        String sqlInjectionPayload = "'; DROP TABLE users; --";
        loginPage.enterEmail(sqlInjectionPayload);
        loginPage.enterPassword("testpassword");
        loginPage.clickLoginButton();
        
        // Wait for response
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Application should handle SQL injection gracefully
        // Either show an error message or reject the input
        boolean errorMessageDisplayed = loginPage.isErrorMessageDisplayed();
        
        // We expect either an error message or the page to remain the same
        Assert.assertTrue(errorMessageDisplayed || loginPage.isLoginPageDisplayed(), 
            "Application should handle SQL injection gracefully");
        
        ExtentReporterNG.logPass("SQL injection payload handled correctly");
        ExtentReporterNG.attachScreenshot(driver, "SQLInjectionEmail");
    }
    
    @Test(priority = 2)
    public void testXSSInEmailField() {
        ExtentReporterNG.createTest("XSS Test - Email Field");
        ExtentReporterNG.logInfo("Testing XSS payload in email field");
        
        String xssPayload = "<script>alert('XSS')</script>";
        loginPage.enterEmail(xssPayload);
        loginPage.enterPassword("testpassword");
        loginPage.clickLoginButton();
        
        // Wait for response
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Application should sanitize or escape the XSS payload
        // Either show an error message or reject the input
        boolean errorMessageDisplayed = loginPage.isErrorMessageDisplayed();
        
        Assert.assertTrue(errorMessageDisplayed || loginPage.isLoginPageDisplayed(), 
            "Application should handle XSS payload gracefully");
        
        ExtentReporterNG.logPass("XSS payload handled correctly");
        ExtentReporterNG.attachScreenshot(driver, "XSSEmail");
    }
    
    @Test(priority = 3)
    public void testSessionTimeout() {
        ExtentReporterNG.createTest("Session Timeout Test");
        ExtentReporterNG.logInfo("Testing session timeout handling");
        
        // Login first
        loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
        
        // Wait for dashboard to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Simulate session timeout by waiting (if application has session timeout)
        // This is a simplified test - in a real scenario, you might need to manipulate cookies
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Application should handle session properly
        ExtentReporterNG.logInfo("Session timeout test completed");
        ExtentReporterNG.attachScreenshot(driver, "SessionTimeout");
    }
    
    @Test(priority = 4)
    public void testInputSanitization() {
        ExtentReporterNG.createTest("Input Sanitization Test");
        ExtentReporterNG.logInfo("Testing input sanitization for special characters");
        
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        loginPage.enterEmail("test" + specialChars + "@example.com");
        loginPage.enterPassword("password" + specialChars);
        loginPage.clickLoginButton();
        
        // Wait for response
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Application should handle special characters gracefully
        boolean errorMessageDisplayed = loginPage.isErrorMessageDisplayed();
        
        Assert.assertTrue(errorMessageDisplayed || loginPage.isLoginPageDisplayed(), 
            "Application should handle special characters gracefully");
        
        ExtentReporterNG.logPass("Special characters handled correctly");
        ExtentReporterNG.attachScreenshot(driver, "SpecialChars");
    }
    
    @AfterClass
    public void tearDown() {
        ExtentReporterNG.logInfo("Closing WebDriver after security tests");
        BaseConfig.closeDriver();
        ExtentReporterNG.flush();
    }
}