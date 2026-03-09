package com.acme.tests.mobile;

import com.acme.config.BaseConfig;
import com.acme.mobile.BaseMobileTest;
import com.acme.mobile.pages.MobileLoginPage;
import com.acme.utils.ExtentReporterNG;
import com.acme.utils.LoggerUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MobileLoginTest extends BaseMobileTest {
    private MobileLoginPage mobileLoginPage;
    
    @BeforeMethod
    public void setUpTest() {
        LoggerUtil.logInfo("Setting up mobile login test");
        // Navigate to login page if needed (for hybrid apps)
        // For native apps, this might not be necessary
        mobileLoginPage = new MobileLoginPage(driver);
    }
    
    @Test(priority = 1)
    public void testValidLogin() {
        ExtentReporterNG.logInfo("Testing valid mobile login with correct credentials");
        LoggerUtil.logInfo("Testing valid mobile login with correct credentials");
        
        try {
            // Use the same credentials as web tests for consistency
            mobileLoginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
            
            // Add assertions based on expected behavior after login
            // This is a placeholder - you would replace with actual validation
            ExtentReporterNG.logPass("Valid mobile login test completed");
            LoggerUtil.logInfo("Valid mobile login test completed");
        } catch (Exception e) {
            LoggerUtil.logError("Error during valid mobile login test", e);
            ExtentReporterNG.logFail("Valid mobile login test failed: " + e.getMessage());
            throw e;
        }
    }
    
    @Test(priority = 2)
    public void testInvalidLogin() {
        ExtentReporterNG.logInfo("Testing invalid mobile login with incorrect credentials");
        LoggerUtil.logInfo("Testing invalid mobile login with incorrect credentials");
        
        try {
            // Try invalid login
            mobileLoginPage.login("invalid@example.com", "wrongpassword");
            
            // Check for error message
            boolean isErrorDisplayed = mobileLoginPage.isErrorMessageDisplayed();
            Assert.assertTrue(isErrorDisplayed, "Error message should be displayed for invalid login");
            
            ExtentReporterNG.logPass("Invalid mobile login handled correctly with error message");
            LoggerUtil.logInfo("Invalid mobile login handled correctly with error message");
        } catch (Exception e) {
            LoggerUtil.logError("Error during invalid mobile login test", e);
            ExtentReporterNG.logFail("Invalid mobile login test failed: " + e.getMessage());
            throw e;
        }
    }
}