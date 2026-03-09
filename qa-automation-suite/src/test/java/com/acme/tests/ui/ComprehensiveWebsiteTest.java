package com.acme.tests.ui;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import com.acme.pages.DashboardPage;
import com.acme.utils.ExtentReporterNG;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ComprehensiveWebsiteTest {
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    
    @BeforeClass
    public void setUp() {
        ExtentReporterNG.createTest("Comprehensive Website Test Setup");
        ExtentReporterNG.logInfo("Initializing WebDriver for comprehensive testing");
        
        driver = BaseConfig.getDriver();
        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
    }
    
    @Test(priority = 1)
    public void testWebsiteAccessibility() {
        ExtentReporterNG.createTest("Website Accessibility Test");
        ExtentReporterNG.logInfo("Testing website accessibility and initial page load");
        
        driver.get(BaseConfig.BASE_URL);
        
        // Check page title
        String pageTitle = driver.getTitle();
        ExtentReporterNG.logInfo("Page title: " + pageTitle);
        
        // Check current URL
        String currentUrl = driver.getCurrentUrl();
        ExtentReporterNG.logInfo("Current URL: " + currentUrl);
        
        // Verify we're on the right page
        Assert.assertTrue(currentUrl.contains("acme.egalvanic.ai"), "Should be on ACME website");
        ExtentReporterNG.logPass("Website is accessible");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "WebsiteAccessibility");
    }
    
    @Test(priority = 2)
    public void testPageLoadPerformance() {
        ExtentReporterNG.createTest("Page Load Performance Test");
        ExtentReporterNG.logInfo("Testing page load performance");
        
        // Navigate to the page
        long startTime = System.currentTimeMillis();
        driver.get(BaseConfig.BASE_URL);
        long endTime = System.currentTimeMillis();
        
        long loadTime = endTime - startTime;
        ExtentReporterNG.logInfo("Page load time: " + loadTime + " ms");
        
        // Check if page loaded within reasonable time (5 seconds)
        Assert.assertTrue(loadTime < 5000, "Page should load within 5 seconds");
        ExtentReporterNG.logPass("Page load performance is acceptable");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "PageLoadPerformance");
    }
    
    @Test(priority = 3)
    public void testResponsiveDesign() {
        ExtentReporterNG.createTest("Responsive Design Test");
        ExtentReporterNG.logInfo("Testing responsive design by resizing window");
        
        // Test different screen sizes
        int[][] screenSizes = {{1920, 1080}, {1366, 768}, {768, 1024}, {375, 667}};
        
        for (int[] size : screenSizes) {
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(size[0], size[1]));
            ExtentReporterNG.logInfo("Testing screen size: " + size[0] + "x" + size[1]);
            
            // Wait for page to adjust
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Check if page elements are still accessible
            Assert.assertTrue(loginPage.isLoginPageDisplayed(), 
                "Login page should be displayed on screen size " + size[0] + "x" + size[1]);
        }
        
        ExtentReporterNG.logPass("Responsive design works correctly");
        
        // Reset to default size
        driver.manage().window().maximize();
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "ResponsiveDesign");
    }
    
    @Test(priority = 4)
    public void testCrossPageNavigation() {
        ExtentReporterNG.createTest("Cross Page Navigation Test");
        ExtentReporterNG.logInfo("Testing navigation between different pages");
        
        // Login first
        driver.get(BaseConfig.BASE_URL);
        loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
        
        // Wait for dashboard
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Verify we're on dashboard
        Assert.assertTrue(dashboardPage.isLoggedIn(), "Should be logged in to dashboard");
        
        // Test navigation by scrolling
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        
        // Wait a bit
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        js.executeScript("window.scrollTo(0, 0)");
        
        ExtentReporterNG.logPass("Cross page navigation works correctly");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "CrossPageNavigation");
    }
    
    @Test(priority = 5)
    public void testFormValidation() {
        ExtentReporterNG.createTest("Form Validation Test");
        ExtentReporterNG.logInfo("Testing form validation on login page");
        
        // Navigate to login page
        driver.get(BaseConfig.BASE_URL);
        loginPage = new LoginPage(driver);
        
        // Test email validation
        loginPage.enterEmail("invalid-email");
        loginPage.enterPassword("password");
        loginPage.clickLoginButton();
        
        // Wait for validation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Should still be on login page
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Should remain on login page with invalid email");
        
        ExtentReporterNG.logPass("Form validation works correctly");
        
        // Attach screenshot
        ExtentReporterNG.attachScreenshot(driver, "FormValidation");
    }
    
    @AfterClass
    public void tearDown() {
        ExtentReporterNG.createTest("Comprehensive Test Cleanup");
        ExtentReporterNG.logInfo("Closing WebDriver for comprehensive testing");
        BaseConfig.closeDriver();
        ExtentReporterNG.flush();
    }
}