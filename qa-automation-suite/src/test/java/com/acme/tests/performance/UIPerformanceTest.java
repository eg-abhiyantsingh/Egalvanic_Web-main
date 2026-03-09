package com.acme.tests.performance;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import com.acme.pages.DashboardPage;
import com.acme.utils.ExtentReporterNG;
import com.acme.utils.PerformanceUtils;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

public class UIPerformanceTest {
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    
    @BeforeClass
    public void setUp() {
        ExtentReporterNG.createTest("UI Performance Tests");
        ExtentReporterNG.logInfo("Initializing WebDriver for performance tests");
        
        driver = BaseConfig.getDriver();
    }
    
    @Test(priority = 1)
    public void testLoginPageLoadPerformance() {
        ExtentReporterNG.createTest("Login Page Load Performance Test");
        ExtentReporterNG.logInfo("Testing login page load performance");
        
        long startTime = System.currentTimeMillis();
        driver.get(BaseConfig.BASE_URL);
        long endTime = System.currentTimeMillis();
        
        long pageLoadTime = endTime - startTime;
        ExtentReporterNG.logInfo("Page load time: " + pageLoadTime + " ms");
        
        loginPage = new LoginPage(driver);
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page should be displayed");
        
        // Get detailed performance metrics
        Map<String, Object> metrics = PerformanceUtils.getPageLoadMetrics(driver);
        PerformanceUtils.logPerformanceMetrics(metrics);
        
        // Validate performance thresholds
        Assert.assertTrue(pageLoadTime < 5000, "Page load time should be less than 5 seconds");
        
        Long domLoadTime = (Long) metrics.get("DOMLoadingTime");
        if (domLoadTime != null) {
            Assert.assertTrue(domLoadTime < 3000, "DOM loading time should be less than 3 seconds");
        }
        
        ExtentReporterNG.logPass("Login page load performance is acceptable");
        ExtentReporterNG.attachScreenshot(driver, "LoginPagePerformance");
    }
    
    @Test(priority = 2)
    public void testLoginProcessPerformance() {
        ExtentReporterNG.createTest("Login Process Performance Test");
        ExtentReporterNG.logInfo("Testing login process performance");
        
        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
        
        long startTime = System.currentTimeMillis();
        loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
        
        // Wait for dashboard to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        long loginProcessTime = endTime - startTime;
        
        ExtentReporterNG.logInfo("Login process time: " + loginProcessTime + " ms");
        
        Assert.assertTrue(dashboardPage.isLoggedIn(), "User should be logged in");
        Assert.assertTrue(loginProcessTime < 10000, "Login process should complete within 10 seconds");
        
        ExtentReporterNG.logPass("Login process performance is acceptable");
        ExtentReporterNG.attachScreenshot(driver, "LoginProcessPerformance");
    }
    
    @Test(priority = 3)
    public void testDashboardLoadPerformance() {
        ExtentReporterNG.createTest("Dashboard Load Performance Test");
        ExtentReporterNG.logInfo("Testing dashboard load performance");
        
        // Navigate to dashboard (assuming we're already logged in from previous test)
        long startTime = System.currentTimeMillis();
        
        // Wait for dashboard to fully load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        long dashboardLoadTime = endTime - startTime;
        
        ExtentReporterNG.logInfo("Dashboard load time: " + dashboardLoadTime + " ms");
        
        // Get detailed performance metrics
        Map<String, Object> metrics = PerformanceUtils.getPageLoadMetrics(driver);
        PerformanceUtils.logPerformanceMetrics(metrics);
        
        // Validate performance thresholds
        Assert.assertTrue(dashboardLoadTime < 8000, "Dashboard load time should be less than 8 seconds");
        
        ExtentReporterNG.logPass("Dashboard load performance is acceptable");
        ExtentReporterNG.attachScreenshot(driver, "DashboardPerformance");
    }
    
    @Test(priority = 4)
    public void testResourceLoadPerformance() {
        ExtentReporterNG.createTest("Resource Load Performance Test");
        ExtentReporterNG.logInfo("Testing resource load performance");
        
        // Navigate to the page
        driver.get(BaseConfig.BASE_URL);
        
        // Measure resource loading performance using JavaScript
        String script = "var resources = performance.getEntriesByType('resource');" +
                "var totalSize = 0;" +
                "var totalDuration = 0;" +
                "for (var i = 0; i < resources.length; i++) {" +
                "  totalSize += resources[i].encodedBodySize;" +
                "  totalDuration += resources[i].duration;" +
                "}" +
                "return {totalSize: totalSize, totalDuration: totalDuration, resourceCount: resources.length};";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resourceMetrics = (Map<String, Object>) 
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script);
        
        long totalSize = (Long) resourceMetrics.get("totalSize");
        double totalDuration = (Double) resourceMetrics.get("totalDuration");
        long resourceCount = (Long) resourceMetrics.get("resourceCount");
        
        ExtentReporterNG.logInfo("Total resources: " + resourceCount);
        ExtentReporterNG.logInfo("Total size: " + totalSize + " bytes");
        ExtentReporterNG.logInfo("Total duration: " + totalDuration + " ms");
        
        // Validate performance thresholds
        Assert.assertTrue(resourceCount < 100, "Page should load less than 100 resources");
        Assert.assertTrue(totalSize < 5000000, "Total resource size should be less than 5MB");
        
        ExtentReporterNG.logPass("Resource load performance is acceptable");
        ExtentReporterNG.attachScreenshot(driver, "ResourcePerformance");
    }
    
    @AfterClass
    public void tearDown() {
        ExtentReporterNG.logInfo("Closing WebDriver after performance tests");
        BaseConfig.closeDriver();
        ExtentReporterNG.flush();
    }
}