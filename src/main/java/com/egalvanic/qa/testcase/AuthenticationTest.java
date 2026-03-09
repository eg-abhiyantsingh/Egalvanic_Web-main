package com.egalvanic.qa.testcase;

import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.pageobjects.DashboardPage;
import com.egalvanic.qa.utils.DriverManager;
import com.egalvanic.qa.utils.ReportManager;
import com.egalvanic.qa.utils.ConfigReader;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Comprehensive Authentication Test Automation
 * Each test case runs independently without impacting others
 * Generates detailed HTML reports with screenshots following Module/Feature pattern
 */
public class AuthenticationTest {
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    private ExtentTest moduleTest;
    private ExtentTest featureTest;
    
    private static final String BASE_URL = ConfigReader.getBaseUrl();
    private static final String EMAIL = ConfigReader.getUserEmail();
    private static final String PASSWORD = ConfigReader.getUserPassword();
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static int totalTests = 0;

    public void runAllTests() {
        // Setup reports
        ReportManager.initReports("AuthenticationReport.html");
        
        // Create the hierarchical test structure for reporting
        moduleTest = ReportManager.createTest("Authentication");
        moduleTest.assignCategory("Authentication");
        
        featureTest = ReportManager.createNode(moduleTest, "Login");
        featureTest.assignCategory("Login");
        
        try {
            System.out.println("🧪 Starting Independent Authentication Test Execution");
            
            // Run all test cases independently
            runTestCaseIndependently("TC01_ValidCredentials");
            runTestCaseIndependently("TC02_InvalidCredentials");
            
            // Add summary to report
            featureTest.log(Status.INFO, "Test Execution Summary");
            featureTest.log(Status.INFO, "Total Tests: " + totalTests);
            featureTest.log(Status.PASS, "Passed Tests: " + passedTests);
            featureTest.log(Status.FAIL, "Failed Tests: " + failedTests);
            featureTest.log(Status.INFO, "Success Rate: " + (totalTests > 0 ? (passedTests * 100 / totalTests) : 0) + "%");
            
            System.out.println("\nTEST EXECUTION SUMMARY");
            System.out.println("Total Tests: " + totalTests);
            System.out.println("Passed Tests: " + passedTests);
            System.out.println("Failed Tests: " + failedTests);
            System.out.println("Success Rate: " + (totalTests > 0 ? (passedTests * 100 / totalTests) : 0) + "%");
            System.out.println("\nALL AUTHENTICATION TESTS COMPLETED INDEPENDENTLY");
            System.out.println("Detailed report generated at: test-output/reports/AuthenticationReport.html");
            
        } catch (Exception e) {
            System.out.println("Fatal error in test suite: " + e.getMessage());
            featureTest.log(Status.FAIL, "Fatal error in test suite: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ReportManager.flushReports();
            System.out.println("Report generation completed");
        }
    }

    private void runTestCaseIndependently(String testCaseName) {
        totalTests++;
        ExtentTest testCase = ReportManager.createNode(featureTest, testCaseName);
        testCase.log(Status.INFO, "Starting independent execution of " + testCaseName);
        testCase.log(Status.INFO, "Execution timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));        
        
        try {
            // Setup fresh driver for each test
            DriverManager.createDriver("chrome");
            driver = DriverManager.getDriver("chrome");
            loginPage = new LoginPage();
            dashboardPage = new DashboardPage();
            
            // Execute the specific test case
            switch(testCaseName) {
                case "TC01_ValidCredentials":
                    execute_TC01_ValidCredentials(testCase);
                    break;
                case "TC02_InvalidCredentials":
                    execute_TC02_InvalidCredentials(testCase);
                    break;
                default:
                    testCase.log(Status.FAIL, "Unknown test case: " + testCaseName);
                    failedTests++;
                    break;
            }
        } catch (Exception e) {
            testCase.log(Status.FAIL, "Exception in " + testCaseName + ": " + e.getMessage());
            failedTests++;
            try {
                takeShotAndAttachReport("error_" + testCaseName, "Error in " + testCaseName, testCase);
            } catch (Exception ignored) {}
        } finally {
            // Teardown driver for this test case
            DriverManager.quitDriver();
            testCase.log(Status.INFO, "Completed execution of " + testCaseName);
        }
    }

    private void takeShotAndAttachReport(String name, String testName, ExtentTest test) {
        try {
            String fname = name + "_" + stamp() + ".png";
            String destPath = "test-output/screenshots/" + fname;
            
            // Create screenshots directory if it doesn't exist
            Files.createDirectories(Path.of("test-output/screenshots"));
            
            // Take screenshot
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            byte[] screenshotBytes = screenshot.getScreenshotAs(OutputType.BYTES);
            Files.write(Path.of(destPath), screenshotBytes);
            
            System.out.println("✔ Screenshot saved: " + fname);
            
            // Attach to report
            String base64Image = Base64.getEncoder().encodeToString(screenshotBytes);
            test.addScreenCaptureFromBase64String(base64Image, testName + " - " + stamp());
        } catch (Exception e) {
            System.out.println("Screenshot failed: " + e.getMessage());
            test.log(Status.WARNING, "Screenshot failed: " + e.getMessage());
        }
    }

    private String stamp() { 
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")); 
    }

    // ---------------- Individual Test Case Implementations ----------------
    
    // TC01: Valid Credentials
    private void execute_TC01_ValidCredentials(ExtentTest test) {
        test.log(Status.INFO, "Executing TC01: Valid Credentials Login");
        try {
            DriverManager.navigateTo(BASE_URL);
            takeShotAndAttachReport("tc01_before_login", "TC01 Before Login", test);
            
            loginPage.login(EMAIL, PASSWORD);
            
            // Wait for successful login
            if (dashboardPage.waitForDashboard()) {
                test.log(Status.PASS, "TC01 PASSED: User logged in successfully and redirected to dashboard");
                takeShotAndAttachReport("tc01_after_login", "TC01 After Login", test);
                passedTests++;
            } else {
                test.log(Status.FAIL, "TC01 FAILED: User not redirected to dashboard after login");
                takeShotAndAttachReport("tc01_error", "TC01 Error", test);
                failedTests++;
            }
        } catch (Exception e) {
            test.log(Status.FAIL, "TC01 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc01_error", "TC01 Error", test);
            failedTests++;
        }
    }
    
    // TC02: Invalid Credentials
    private void execute_TC02_InvalidCredentials(ExtentTest test) {
        test.log(Status.INFO, "Executing TC02: Invalid Credentials Login");
        try {
            DriverManager.navigateTo(BASE_URL);
            takeShotAndAttachReport("tc02_before_login", "TC02 Before Login", test);
            
            loginPage.login("invalid@example.com", "wrongpassword");
            
            // Wait and check for error message
            try {
                Thread.sleep(3000);
                if (loginPage.isErrorMessageDisplayed()) {
                    test.log(Status.PASS, "TC02 PASSED: Error message displayed for invalid credentials");
                    takeShotAndAttachReport("tc02_error_message", "TC02 Error Message", test);
                    passedTests++;
                } else {
                    test.log(Status.FAIL, "TC02 FAILED: Error message not displayed");
                    takeShotAndAttachReport("tc02_no_error", "TC02 No Error Displayed", test);
                    failedTests++;
                }
            } catch (Exception e) {
                test.log(Status.WARNING, "TC02: Could not locate explicit error message, checking URL");
                // Check if still on login page
                if (driver.getCurrentUrl().contains("login")) {
                    test.log(Status.PASS, "TC02 PASSED: Remained on login page after invalid credentials");
                } else {
                    test.log(Status.FAIL, "TC02 FAILED: Navigated away from login page unexpectedly");
                }
                takeShotAndAttachReport("tc02_check", "TC02 Check", test);
                passedTests++;
            }
        } catch (Exception e) {
            test.log(Status.FAIL, "TC02 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc02_error", "TC02 Error", test);
            failedTests++;
        }
    }
}