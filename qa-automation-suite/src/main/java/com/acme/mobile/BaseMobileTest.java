package com.acme.mobile;

import com.acme.utils.ExtentReporterNG;
import com.acme.utils.LoggerUtil;
import io.appium.java_client.AppiumDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class BaseMobileTest {
    protected AppiumDriver driver;
    
    @BeforeClass
    public void setUp() {
        ExtentReporterNG.createClassTest(this.getClass().getSimpleName());
        LoggerUtil.logInfo("Initializing Appium driver for mobile testing");
        
        try {
            driver = MobileConfig.getDriver();
        } catch (Exception e) {
            LoggerUtil.logError("Failed to initialize Appium driver", e);
            throw e;
        }
    }
    
    @AfterClass
    public void tearDown() {
        LoggerUtil.logInfo("Closing Appium driver");
        try {
            MobileConfig.closeDriver();
        } catch (Exception e) {
            LoggerUtil.logWarning("Error while closing Appium driver: " + e.getMessage());
        }
        ExtentReporterNG.flush();
    }
}