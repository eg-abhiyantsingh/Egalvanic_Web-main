package com.acme.tests.ui;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import com.acme.pages.DashboardPage;
import com.acme.utils.DataProviderUtil;
import com.acme.utils.ExtentReporterNG;
import com.acme.utils.LoggerUtil;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DataDrivenLoginTest {
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    
    @BeforeClass
    public void setUp() {
        ExtentReporterNG.createClassTest(this.getClass().getSimpleName());
        LoggerUtil.logInfo("Initializing WebDriver for data-driven login testing");
        
        try {
            driver = BaseConfig.getDriver();
        } catch (Exception e) {
            LoggerUtil.logError("Failed to initialize WebDriver", e);
            throw e;
        }
    }
    
    @DataProvider(name = "loginData")
    public Object[][] getLoginData() {
        // Using the utility to read data from CSV
        return DataProviderUtil.readLoginTestData("src/main/resources/testdata/login_test_data.csv");
    }
    
    @Test(dataProvider = "loginData", retryAnalyzer = com.acme.utils.RetryAnalyzer.class)
    public void testDataDrivenLogin(String email, String password, String expectedResult) {
        String testName = "Login Test - Email: " + email + ", Password: " + password;
        ExtentReporterNG.logInfo("Starting " + testName);
        LoggerUtil.logInfo("Starting " + testName);
        
        try {
            // Navigate to login page for each test
            driver.get(BaseConfig.BASE_URL);
            loginPage = new LoginPage(driver);
            dashboardPage = new DashboardPage(driver);
            
            // Perform login
            loginPage.login(email, password);
            
            // Validate result based on expected outcome
            if ("success".equals(expectedResult)) {
                boolean isLoggedIn = dashboardPage.waitForDashboardToLoad();
                Assert.assertTrue(isLoggedIn, "Expected successful login but failed for credentials: " + email);
                ExtentReporterNG.logPass("Login successful for: " + email);
                LoggerUtil.logInfo("Login successful for: " + email);
            } else {
                boolean isErrorDisplayed = loginPage.waitForErrorMessage();
                Assert.assertTrue(isErrorDisplayed, "Expected login failure but succeeded for credentials: " + email);
                ExtentReporterNG.logPass("Login correctly failed for: " + email);
                LoggerUtil.logInfo("Login correctly failed for: " + email);
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error during data-driven login test", e);
            ExtentReporterNG.logFail("Test failed: " + e.getMessage());
            throw e;
        } finally {
            // Attach screenshot for each test
            ExtentReporterNG.attachScreenshot(driver, "DataDrivenLogin_" + email.replace("@", "_").replace(".", "_"));
        }
    }
    
    @AfterClass
    public void tearDown() {
        LoggerUtil.logInfo("Closing WebDriver for data-driven login tests");
        try {
            BaseConfig.closeDriver();
        } catch (Exception e) {
            LoggerUtil.logWarning("Error while closing WebDriver: " + e.getMessage());
        }
        ExtentReporterNG.flush();
    }
}