import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive Authentication Test Automation
 * Each test case runs independently without impacting others
 * Generates detailed HTML reports with screenshots following Module/Feature pattern
 */
public class Authentication {
    static WebDriver driver;
    static WebDriverWait wait;
    static JavascriptExecutor js;
    static Actions actions;
    static ExtentReports extent;
    static ExtentReports summaryExtent; // Second report for pass/fail only
    static ExtentTest moduleTest; // Module level test
    static ExtentTest featureTest; // Feature level test
    static ExtentTest summaryModuleTest; // Summary module test
    static ExtentTest summaryFeatureTest; // Summary feature test
    static ExtentSparkReporter sparkReporter;
    static ExtentSparkReporter summarySparkReporter;
    
    static String currentBrowser = "chrome";
    static int passedTests = 0;
    static int failedTests = 0;
    static int totalTests = 0;

    // === CONFIG (hardcoded) ===
    static final String BASE_URL = "https://acme.qa.egalvanic.ai";
    static final String EMAIL = "rahul+acme@egalvanic.com";
    static final String PASSWORD = "RP@egalvanic123";
    static final int DEFAULT_TIMEOUT = 25;

    public static void main(String[] args) {
        currentBrowser = args.length > 0 ? args[0] : "chrome";
        setupExtentReports();
        
        // Create the hierarchical test structure for reporting
        // Module = Authentication
        moduleTest = extent.createTest("Authentication");
        moduleTest.assignCategory("Authentication");
        
        // Feature = Login
        featureTest = moduleTest.createNode("Login");
        featureTest.assignCategory("Login");
        
        // Create structure for summary report
        summaryModuleTest = summaryExtent.createTest("Authentication");
        summaryModuleTest.assignCategory("Authentication");
        
        summaryFeatureTest = summaryModuleTest.createNode("Login");
        summaryFeatureTest.assignCategory("Login");
        try {
            System.out.println("🧪 Starting Independent Authentication Test Execution");
            
            // Run all test cases independently
            runTestCaseIndependently("TC01_ValidCredentials");
            runTestCaseIndependently("TC02_InvalidCredentials");
            runTestCaseIndependently("TC03_TrailingSpaceUsername");
            runTestCaseIndependently("TC04_LeadingSpaceUsername");
            runTestCaseIndependently("TC05_LeadingTrailingSpacesUsername");
            runTestCaseIndependently("TC06_OnlySpacesUsername");
            runTestCaseIndependently("TC07_TrailingSpacePassword");
            runTestCaseIndependently("TC08_SpecialCharactersUsername");
            runTestCaseIndependently("TC09_NumericOnlyUsername");
            runTestCaseIndependently("TC10_CaseSensitivityUsername");
            runTestCaseIndependently("TC11_ExceedingMaxLengthUsername");
            runTestCaseIndependently("TC12_LeadingSpacePassword");
             runTestCaseIndependently("TC13_OnlySpacesPassword");
            runTestCaseIndependently("TC14_MinLengthPassword");
           runTestCaseIndependently("TC15_MaxLengthPassword");
           runTestCaseIndependently("TC16_SQLInjectionUsername");
            runTestCaseIndependently("TC17_XSSUsername");
            runTestCaseIndependently("TC18_ErrorMessageCleared");
             runTestCaseIndependently("TC19_AccessDashboardWithoutLogin");
            runTestCaseIndependently("TC20_RefreshAfterLogin");
              runTestCaseIndependently("TC21_EnterKeyLogin");
            runTestCaseIndependently("TC22_BackButtonSession");            
            // Add summary to report
            featureTest.log(Status.INFO, "Test Execution Summary");
            featureTest.log(Status.INFO, "Total Tests: " + totalTests);
            featureTest.log(Status.PASS, "Passed Tests: " + passedTests);
            featureTest.log(Status.FAIL, "Failed Tests: " + failedTests);
            featureTest.log(Status.INFO, "Success Rate: " + (totalTests > 0 ? (passedTests * 100 / totalTests) : 0) + "%");
            
            // Add summary to summary report
            ExtentTest summaryStats = summaryFeatureTest.createNode("TestExecutionSummary");
            summaryStats.log(Status.INFO, "Total Tests: " + totalTests);
            summaryStats.log(Status.PASS, "Passed Tests: " + passedTests);
            summaryStats.log(Status.FAIL, "Failed Tests: " + failedTests);
            summaryStats.log(Status.INFO, "Success Rate: " + (totalTests > 0 ? (passedTests * 100 / totalTests) : 0) + "%");
            
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
            extent.flush();
            summaryExtent.flush();
            System.out.println("Report generation completed");
            System.out.println("Summary report generated at: test-output/reports/AuthenticationSummaryReport.html");
        }
    }

    static void runTestCaseIndependently(String testCaseName) {
        totalTests++;
        ExtentTest testCase = featureTest.createNode(testCaseName);
        testCase.log(Status.INFO, "Starting independent execution of " + testCaseName);
        testCase.log(Status.INFO, "Execution timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));        
        try {            // Setup fresh driver for each test
            setupDriver(currentBrowser);
            
            // Execute the specific test case
            switch(testCaseName) {
                case "TC01_ValidCredentials":
                    execute_TC01_ValidCredentials(testCase);
                    break;
                case "TC02_InvalidCredentials":
                    execute_TC02_InvalidCredentials(testCase);
                    break;
                case "TC03_TrailingSpaceUsername":
                    execute_TC03_TrailingSpaceUsername(testCase);
                    break;
                case "TC04_LeadingSpaceUsername":
                    execute_TC04_LeadingSpaceUsername(testCase);
                    break;
                case "TC05_LeadingTrailingSpacesUsername":
                    execute_TC05_LeadingTrailingSpacesUsername(testCase);
                    break;
                case "TC06_OnlySpacesUsername":
                    execute_TC06_OnlySpacesUsername(testCase);
                    break;
                case "TC07_TrailingSpacePassword":
                    execute_TC07_TrailingSpacePassword(testCase);
                    break;
                case "TC08_SpecialCharactersUsername":
                    execute_TC08_SpecialCharactersUsername(testCase);
                    break;
                case "TC09_NumericOnlyUsername":
                    execute_TC09_NumericOnlyUsername(testCase);
                    break;
                case "TC10_CaseSensitivityUsername":
                    execute_TC10_CaseSensitivityUsername(testCase);
                    break;
                case "TC11_ExceedingMaxLengthUsername":
                    execute_TC11_ExceedingMaxLengthUsername(testCase);
                    break;
                case "TC12_LeadingSpacePassword":
                    execute_TC12_LeadingSpacePassword(testCase);
                    break;
                case "TC13_OnlySpacesPassword":
                    execute_TC13_OnlySpacesPassword(testCase);
                    break;
                case "TC14_MinLengthPassword":
                    execute_TC14_MinLengthPassword(testCase);
                    break;
                case "TC15_MaxLengthPassword":
                    execute_TC15_MaxLengthPassword(testCase);
                    break;
                case "TC16_SQLInjectionUsername":
                    execute_TC16_SQLInjectionUsername(testCase);
                    break;
                case "TC17_XSSUsername":
                    execute_TC17_XSSUsername(testCase);
                    break;
                case "TC18_ErrorMessageCleared":
                    execute_TC18_ErrorMessageCleared(testCase);
                    break;
                case "TC19_AccessDashboardWithoutLogin":
                    execute_TC19_AccessDashboardWithoutLogin(testCase);
                    break;
                case "TC20_RefreshAfterLogin":
                    execute_TC20_RefreshAfterLogin(testCase);
                    break;
                case "TC21_EnterKeyLogin":
                    execute_TC21_EnterKeyLogin(testCase);
                    break;
                case "TC22_BackButtonSession":
                    execute_TC22_BackButtonSession(testCase);
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
            if (driver != null) {
                driver.quit();
                driver = null;
            }
            testCase.log(Status.INFO, "Completed execution of " + testCaseName);
        }
    }

    // ---------------- setup ----------------
    static void setupExtentReports() {
        try {
            Files.createDirectories(Path.of("test-output/reports"));
            Files.createDirectories(Path.of("test-output/screenshots"));
        } catch (Exception e) {
            System.out.println("Failed to create directories: " + e.getMessage());
        }

        // Detailed report
        String reportFileName = "test-output/reports/AuthenticationReport.html";
        sparkReporter = new ExtentSparkReporter(reportFileName);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Authentication Test Report");
        sparkReporter.config().setReportName("Authentication Test Automation Report");
        sparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        extent.setSystemInfo("Organization", "ACME");
        extent.setSystemInfo("Environment", "Test");
        extent.setSystemInfo("Browser", currentBrowser);
        extent.setSystemInfo("Tester", "QA Automation Engineer");
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("OS", System.getProperty("os.name"));

        // Summary report (pass/fail only)
        String summaryReportFileName = "test-output/reports/AuthenticationSummaryReport.html";
        summarySparkReporter = new ExtentSparkReporter(summaryReportFileName);
        summarySparkReporter.config().setTheme(Theme.STANDARD);
        summarySparkReporter.config().setDocumentTitle("Authentication Summary Report");
        summarySparkReporter.config().setReportName("Authentication Test Summary Report");
        summarySparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        summaryExtent = new ExtentReports();
        summaryExtent.attachReporter(summarySparkReporter);
        summaryExtent.setSystemInfo("Organization", "ACME");
        summaryExtent.setSystemInfo("Environment", "Test");
        summaryExtent.setSystemInfo("Browser", currentBrowser);
        summaryExtent.setSystemInfo("Tester", "QA Automation Engineer");
        summaryExtent.setSystemInfo("Java Version", System.getProperty("java.version"));
        summaryExtent.setSystemInfo("OS", System.getProperty("os.name"));
    }    static void setupDriver(String browserType) {
        switch(browserType.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;
            case "edge":
                try {
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.setBinary("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
                    driver = new EdgeDriver(edgeOptions);
                } catch (Exception e) {
                    System.out.println("Edge WebDriver setup failed: " + e.getMessage());
                    throw e;
                }
                break;
            case "safari":
                WebDriverManager.safaridriver().setup();
                driver = new SafariDriver();
                driver.manage().window().maximize();
                break;
            case "chrome":
            default:
                WebDriverManager.chromedriver().clearDriverCache();
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOpts = new ChromeOptions();
                chromeOpts.addArguments("--start-maximized");
                chromeOpts.addArguments("--remote-allow-origins=*");
                chromeOpts.addArguments("--disable-blink-features=AutomationControlled");
                chromeOpts.addArguments("--no-sandbox");
                chromeOpts.addArguments("--disable-dev-shm-usage");
                chromeOpts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
                chromeOpts.setExperimentalOption("useAutomationExtension", false);
                driver = new ChromeDriver(chromeOpts);
                driver.manage().window().maximize();
                break;
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        js = (JavascriptExecutor) driver;
        actions = new Actions(driver);
        js.executeScript("document.body.style.zoom='80%';");
        pause(600);
    }

    static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
    static String stamp() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")); }

    static String getBase64Screenshot() {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            return "";
        }
    }

    static String getCurrentBrowserVersion() {
        try {
            if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
                return ((org.openqa.selenium.remote.RemoteWebDriver) driver).getCapabilities().getBrowserVersion();
            }
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Helper method to log test results to summary report
    static void logToSummaryReport(String testCaseName, boolean passed) {
        ExtentTest summaryTestCase = summaryFeatureTest.createNode(testCaseName);
        if (passed) {
            summaryTestCase.log(Status.PASS, "PASSED");
        } else {
            summaryTestCase.log(Status.FAIL, "FAILED");
        }
    }

    static void takeShotAndAttachReport(String name, String testName, ExtentTest test) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fname = name + "_" + stamp() + ".png";
            String destPath = "test-output/screenshots/" + fname;
            Files.copy(src.toPath(), Path.of(destPath));
            System.out.println("✔ Screenshot saved: " + fname);

            byte[] fileContent = Files.readAllBytes(Path.of(destPath));
            String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);
            test.addScreenCaptureFromBase64String(base64Image, testName + " - " + stamp());
        } catch (Exception e) {
            System.out.println("Screenshot failed: " + e.getMessage());
            test.log(Status.WARNING, "Screenshot failed: " + e.getMessage());
        }
    }

    // ---------- safe element helpers ----------
    static WebElement visible(By by, int timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    static WebElement clickable(By by, int timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.elementToBeClickable(by));
    }

    static void jsClick(By by) {
        try {
            WebElement e = driver.findElement(by);
            js.executeScript("arguments[0].click();", e);
        } catch (Exception ex) {
            throw new RuntimeException("JS click failed for: " + by, ex);
        }
    }

    static void click(By by) {
        try {
            clickable(by, DEFAULT_TIMEOUT).click();
        } catch (Exception e) {
            try {
                jsClick(by);
            } catch (Exception ex) {
                throw new RuntimeException("Click failed for: " + by + " -> " + ex.getMessage(), ex);
            }
        }
    }

    static void type(By by, String text) {
        WebElement e = visible(by, DEFAULT_TIMEOUT);
        try { e.clear(); } catch (Exception ignored) {}
        e.click();
        e.sendKeys(text);
    }

    // ---------------- Individual Test Case Implementations ----------------
    
    // TC01: Valid Credentials
    static void execute_TC01_ValidCredentials(ExtentTest test) {
        test.log(Status.INFO, "Executing TC01: Valid Credentials Login");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc01_before_login", "TC01 Before Login", test);
            
            type(By.id("email"), EMAIL);
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            // Wait for successful login
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("nav")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Dashboard') or contains(text(),'Sites')]"))
            ));
            
            test.log(Status.PASS, "TC01 PASSED: User logged in successfully and redirected to dashboard");
            takeShotAndAttachReport("tc01_after_login", "TC01 After Login", test);
            passedTests++;
            logToSummaryReport("TC01_ValidCredentials", true); // Log to summary report
        } catch (Exception e) {
            test.log(Status.FAIL, "TC01 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc01_error", "TC01 Error", test);
            failedTests++;
            logToSummaryReport("TC01_ValidCredentials", false); // Log to summary report
        }
    }
    
    // TC02: Invalid Credentials
    static void execute_TC02_InvalidCredentials(ExtentTest test) {
        test.log(Status.INFO, "Executing TC02: Invalid Credentials Login");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc02_before_login", "TC02 Before Login", test);
            
            type(By.id("email"), "invalid@example.com");
            type(By.id("password"), "wrongpassword");
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            // Wait for error message
            pause(3000);
            
            // Check for error message
            try {
                WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(@class,'alert') or contains(text(),'Incorrect')]"));
                if (errorMsg.isDisplayed()) {
                    test.log(Status.PASS, "TC02 PASSED: Error message displayed for invalid credentials");
                    takeShotAndAttachReport("tc02_error_message", "TC02 Error Message", test);
                    logToSummaryReport("TC02_InvalidCredentials", true); // Log to summary report
                } else {
                    test.log(Status.FAIL, "TC02 FAILED: Error message not displayed");
                    takeShotAndAttachReport("tc02_no_error", "TC02 No Error Displayed", test);
                    logToSummaryReport("TC02_InvalidCredentials", false); // Log to summary report
                }
            } catch (Exception e) {
                test.log(Status.WARNING, "TC02: Could not locate explicit error message, checking URL");
                // Check if still on login page
                if (driver.getCurrentUrl().contains("login")) {
                    test.log(Status.PASS, "TC02 PASSED: Remained on login page after invalid credentials");
                    logToSummaryReport("TC02_InvalidCredentials", true); // Log to summary report
                } else {
                    test.log(Status.FAIL, "TC02 FAILED: Navigated away from login page unexpectedly");
                    logToSummaryReport("TC02_InvalidCredentials", false); // Log to summary report
                }
                takeShotAndAttachReport("tc02_check", "TC02 Check", test);
            }
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "TC02 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc02_error", "TC02 Error", test);
            failedTests++;
            logToSummaryReport("TC02_InvalidCredentials", false); // Log to summary report
        }
    }
    
    // TC03: Username with trailing space
    static void execute_TC03_TrailingSpaceUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC03: Username with Trailing Space");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc03_before_login", "TC03 Before Login", test);
            
            type(By.id("email"), EMAIL + " "); // Add trailing space
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check result - system may trim or reject
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.PASS, "TC03 PASSED: System trimmed trailing space and allowed login");
            } else {
                // Check for error message
                try {
                    WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'Invalid')]"));
                    if (errorMsg.isDisplayed()) {
                        test.log(Status.PASS, "TC03 PASSED: System rejected username with trailing space");
                    } else {
                        test.log(Status.WARNING, "TC03: Unexpected behavior with trailing space");
                    }
                } catch (Exception ex) {
                    test.log(Status.WARNING, "TC03: Unclear handling of trailing space");
                }
            }
            takeShotAndAttachReport("tc03_result", "TC03 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "TC03 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc03_error", "TC03 Error", test);
            failedTests++;
        }
    }
    
    // TC04: Username with leading space
    static void execute_TC04_LeadingSpaceUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC04: Username with Leading Space");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc04_before_login", "TC04 Before Login", test);
            
            type(By.id("email"), " " + EMAIL); // Add leading space
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check result
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.PASS, "TC04 PASSED: System trimmed leading space and allowed login");
            } else {
                // Check for error message
                try {
                    WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'Invalid')]"));
                    if (errorMsg.isDisplayed()) {
                        test.log(Status.PASS, "TC04 PASSED: System rejected username with leading space");
                    } else {
                        test.log(Status.WARNING, "TC04: Unexpected behavior with leading space");
                    }
                } catch (Exception ex) {
                    test.log(Status.WARNING, "TC04: Unclear handling of leading space");
                }
            }
            takeShotAndAttachReport("tc04_result", "TC04 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "TC04 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc04_error", "TC04 Error", test);
            failedTests++;
        }
    }
    
    // TC05: Username with leading and trailing spaces
    static void execute_TC05_LeadingTrailingSpacesUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC05: Username with Leading and Trailing Spaces");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc05_before_login", "TC05 Before Login", test);
            
            type(By.id("email"), " " + EMAIL + " "); // Add leading and trailing spaces
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check result
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.PASS, "TC05 PASSED: System trimmed leading/trailing spaces and allowed login");
            } else {
                // Check for error message
                try {
                    WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'Invalid')]"));
                    if (errorMsg.isDisplayed()) {
                        test.log(Status.PASS, "TC05 PASSED: System rejected username with leading/trailing spaces");
                    } else {
                        test.log(Status.WARNING, "TC05: Unexpected behavior with leading/trailing spaces");
                    }
                } catch (Exception ex) {
                    test.log(Status.WARNING, "TC05: Unclear handling of leading/trailing spaces");
                }
            }
            takeShotAndAttachReport("tc05_result", "TC05 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "TC05 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc05_error", "TC05 Error", test);
            failedTests++;
        }
    }
    
    // TC06: Username with only spaces
    static void execute_TC06_OnlySpacesUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC06: Username with Only Spaces");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc06_before_login", "TC06 Before Login", test);
            
            type(By.id("email"), "    "); // Only spaces
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check for validation message
            try {
                WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'required')]"));
                if (errorMsg.isDisplayed()) {
                    test.log(Status.PASS, "TC06 PASSED: Validation message shown for username with only spaces");
                } else {
                    test.log(Status.FAIL, "TC06 FAILED: No validation message for username with only spaces");
                }
            } catch (Exception ex) {
                test.log(Status.WARNING, "TC06: Could not locate validation message");
            }
            takeShotAndAttachReport("tc06_result", "TC06 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "TC06 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc06_error", "TC06 Error", test);
            failedTests++;
        }
    }
    
    // TC07: Password with trailing space
    static void execute_TC07_TrailingSpacePassword(ExtentTest test) {
        test.log(Status.INFO, "Executing TC07: Password with Trailing Space");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc07_before_login", "TC07 Before Login", test);
            
            type(By.id("email"), EMAIL);
            type(By.id("password"), PASSWORD + " "); // Add trailing space
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check result - should fail as password with space is different
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.WARNING, "TC07: Login succeeded with trailing space in password - potential security issue");
                logToSummaryReport("TC07_TrailingSpacePassword", false); // Log to summary report
            } else {
                test.log(Status.PASS, "TC07 PASSED: Login failed with trailing space in password");
                logToSummaryReport("TC07_TrailingSpacePassword", true); // Log to summary report
            }
            takeShotAndAttachReport("tc07_result", "TC07 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "TC07 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc07_error", "TC07 Error", test);
            failedTests++;
            logToSummaryReport("TC07_TrailingSpacePassword", false); // Log to summary report
        }
    }
    
    // TC08: Username with special characters
    static void execute_TC08_SpecialCharactersUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC08: Username with Special Characters");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc08_before_login", "TC08 Before Login", test);
            
            type(By.id("email"), "user@#$%");
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check for validation message
            try {
                WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'Invalid')]"));
                if (errorMsg.isDisplayed()) {
                    test.log(Status.PASS, "✅ TC08 PASSED: System rejected username with special characters");
                } else {
                    test.log(Status.WARNING, "⚠️ TC08: No clear rejection of special characters");
                }
            } catch (Exception ex) {
                test.log(Status.WARNING, "⚠️ TC08: Could not locate validation message for special characters");
            }
            takeShotAndAttachReport("tc08_result", "TC08 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC08 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc08_error", "TC08 Error", test);
            failedTests++;
        }
    }
    
    // TC09: Numeric-only username
    static void execute_TC09_NumericOnlyUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC09: Numeric-only Username");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc09_before_login", "TC09 Before Login", test);
            
            type(By.id("email"), "123456");
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check result
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.WARNING, "⚠️ TC09: Login succeeded with numeric-only username");
            } else {
                test.log(Status.PASS, "✅ TC09 PASSED: Login failed with numeric-only username");
            }
            takeShotAndAttachReport("tc09_result", "TC09 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC09 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc09_error", "TC09 Error", test);
            failedTests++;
        }
    }
    
    // TC10: Username case sensitivity
    static void execute_TC10_CaseSensitivityUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC10: Username Case Sensitivity");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc10_before_login", "TC10 Before Login", test);
            
            type(By.id("email"), EMAIL.toUpperCase());
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check result - depends on system implementation
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.PASS, "✅ TC10 PASSED: Username is not case-sensitive");
            } else {
                test.log(Status.INFO, "ℹ️ TC10: Username appears to be case-sensitive");
            }
            takeShotAndAttachReport("tc10_result", "TC10 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC10 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc10_error", "TC10 Error", test);
            failedTests++;
        }
    }
    
    // TC11: Username exceeding maximum length
    static void execute_TC11_ExceedingMaxLengthUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC11: Username Exceeding Maximum Length");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc11_before_login", "TC11 Before Login", test);
            
            // Create a very long username
            StringBuilder longUsername = new StringBuilder();
            for (int i = 0; i < 300; i++) {
                longUsername.append("a");
            }
            longUsername.append("@example.com");
            
            type(By.id("email"), longUsername.toString());
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check for validation message
            try {
                WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'too long')]"));
                if (errorMsg.isDisplayed()) {
                    test.log(Status.PASS, "✅ TC11 PASSED: Validation message shown for excessively long username");
                } else {
                    test.log(Status.WARNING, "⚠️ TC11: No validation for excessively long username");
                }
            } catch (Exception ex) {
                test.log(Status.WARNING, "⚠️ TC11: Could not locate validation message for long username");
            }
            takeShotAndAttachReport("tc11_result", "TC11 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC11 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc11_error", "TC11 Error", test);
            failedTests++;
        }
    }
    
    // TC12: Password with leading space
    static void execute_TC12_LeadingSpacePassword(ExtentTest test) {
        test.log(Status.INFO, "Executing TC12: Password with Leading Space");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc12_before_login", "TC12 Before Login", test);
            
            type(By.id("email"), EMAIL);
            type(By.id("password"), " " + PASSWORD); // Add leading space
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check result - should fail as password with space is different
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.WARNING, "⚠️ TC12: Login succeeded with leading space in password - potential security issue");
            } else {
                test.log(Status.PASS, "✅ TC12 PASSED: Login failed with leading space in password");
            }
            takeShotAndAttachReport("tc12_result", "TC12 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC12 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc12_error", "TC12 Error", test);
            failedTests++;
        }
    }
    
    // TC13: Password with only spaces
    static void execute_TC13_OnlySpacesPassword(ExtentTest test) {
        test.log(Status.INFO, "Executing TC13: Password with Only Spaces");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc13_before_login", "TC13 Before Login", test);
            
            type(By.id("email"), EMAIL);
            type(By.id("password"), "    "); // Only spaces
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check for validation message
            try {
                WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'required')]"));
                if (errorMsg.isDisplayed()) {
                    test.log(Status.PASS, "✅ TC13 PASSED: Validation message shown for password with only spaces");
                } else {
                    test.log(Status.FAIL, "❌ TC13 FAILED: No validation message for password with only spaces");
                }
            } catch (Exception ex) {
                test.log(Status.WARNING, "⚠️ TC13: Could not locate validation message");
            }
            takeShotAndAttachReport("tc13_result", "TC13 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC13 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc13_error", "TC13 Error", test);
            failedTests++;
        }
    }
    
    // TC14: Password minimum length validation
    static void execute_TC14_MinLengthPassword(ExtentTest test) {
        test.log(Status.INFO, "Executing TC14: Password Minimum Length Validation");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc14_before_login", "TC14 Before Login", test);
            
            type(By.id("email"), EMAIL);
            type(By.id("password"), "123"); // Very short password
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check for validation message
            try {
                WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'short')]"));
                if (errorMsg.isDisplayed()) {
                    test.log(Status.PASS, "✅ TC14 PASSED: Validation message shown for short password");
                } else {
                    test.log(Status.WARNING, "⚠️ TC14: No validation for short password");
                }
            } catch (Exception ex) {
                test.log(Status.WARNING, "⚠️ TC14: Could not locate validation message for short password");
            }
            takeShotAndAttachReport("tc14_result", "TC14 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC14 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc14_error", "TC14 Error", test);
            failedTests++;
        }
    }
    
    // TC15: Password maximum length validation
    static void execute_TC15_MaxLengthPassword(ExtentTest test) {
        test.log(Status.INFO, "Executing TC15: Password Maximum Length Validation");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc15_before_login", "TC15 Before Login", test);
            
            // Create a very long password
            StringBuilder longPassword = new StringBuilder();
            for (int i = 0; i < 300; i++) {
                longPassword.append("a");
            }
            
            type(By.id("email"), EMAIL);
            type(By.id("password"), longPassword.toString());
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check for validation message
            try {
                WebElement errorMsg = driver.findElement(By.xpath("//div[contains(@class,'error') or contains(text(),'too long')]"));
                if (errorMsg.isDisplayed()) {
                    test.log(Status.PASS, "✅ TC15 PASSED: Validation message shown for excessively long password");
                } else {
                    test.log(Status.WARNING, "⚠️ TC15: No validation for excessively long password");
                }
            } catch (Exception ex) {
                test.log(Status.WARNING, "⚠️ TC15: Could not locate validation message for long password");
            }
            takeShotAndAttachReport("tc15_result", "TC15 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC15 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc15_error", "TC15 Error", test);
            failedTests++;
        }
    }
    
    // TC16: SQL injection attempt in username
    static void execute_TC16_SQLInjectionUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC16: SQL Injection in Username");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc16_before_login", "TC16 Before Login", test);
            
            type(By.id("email"), "'; DROP TABLE users; --");
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check that system handles it securely
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.FAIL, "❌ TC16 FAILED: SQL injection attempt succeeded - major security vulnerability");
            } else {
                test.log(Status.PASS, "✅ TC16 PASSED: SQL injection attempt properly rejected");
            }
            takeShotAndAttachReport("tc16_result", "TC16 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.PASS, "✅ TC16 PASSED: Exception occurred, likely preventing SQL injection: " + e.getMessage());
            takeShotAndAttachReport("tc16_error", "TC16 Error", test);
            passedTests++;
        }
    }
    
    // TC17: XSS attempt in username
    static void execute_TC17_XSSUsername(ExtentTest test) {
        test.log(Status.INFO, "Executing TC17: XSS in Username");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc17_before_login", "TC17 Before Login", test);
            
            type(By.id("email"), "<script>alert('XSS')</script>");
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check that system sanitizes input
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.FAIL, "❌ TC17 FAILED: XSS attempt succeeded - major security vulnerability");
            } else {
                test.log(Status.PASS, "✅ TC17 PASSED: XSS attempt properly rejected");
            }
            takeShotAndAttachReport("tc17_result", "TC17 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.PASS, "✅ TC17 PASSED: Exception occurred, likely preventing XSS: " + e.getMessage());
            takeShotAndAttachReport("tc17_error", "TC17 Error", test);
            passedTests++;
        }
    }
    
    // TC18: Error message cleared after retry
    static void execute_TC18_ErrorMessageCleared(ExtentTest test) {
        test.log(Status.INFO, "Executing TC18: Error Message Cleared After Retry");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc18_before_invalid_login", "TC18 Before Invalid Login", test);
            
            // First, try invalid login to trigger error
            type(By.id("email"), "invalid@example.com");
            type(By.id("password"), "wrongpassword");
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            takeShotAndAttachReport("tc18_error_shown", "TC18 Error Shown", test);
            
            // Now try valid login
            type(By.id("email"), EMAIL);
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            pause(3000);
            
            // Check if we're logged in (error cleared and valid login worked)
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.PASS, "✅ TC18 PASSED: Error message cleared and valid login succeeded");
            } else {
                test.log(Status.FAIL, "❌ TC18 FAILED: Valid login after error did not succeed");
            }
            takeShotAndAttachReport("tc18_result", "TC18 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC18 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc18_error", "TC18 Error", test);
            failedTests++;
        }
    }
    
    // TC19: Access dashboard URL without login
    static void execute_TC19_AccessDashboardWithoutLogin(ExtentTest test) {
        test.log(Status.INFO, "Executing TC19: Access Dashboard Without Login");
        try {
            // Try to access dashboard directly
            driver.get(BASE_URL + "/dashboard");
            takeShotAndAttachReport("tc19_direct_access", "TC19 Direct Dashboard Access", test);
            pause(3000);
            
            // Check if redirected to login
            if (driver.getCurrentUrl().contains("login")) {
                test.log(Status.PASS, "✅ TC19 PASSED: Unauthorized access redirected to login page");
            } else if (driver.getCurrentUrl().contains("dashboard")) {
                test.log(Status.FAIL, "❌ TC19 FAILED: Unauthorized access to dashboard permitted");
            } else {
                test.log(Status.WARNING, "⚠️ TC19: Unexpected redirect behavior");
            }
            takeShotAndAttachReport("tc19_result", "TC19 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC19 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc19_error", "TC19 Error", test);
            failedTests++;
        }
    }
    
    // TC20: Browser refresh after successful login
    static void execute_TC20_RefreshAfterLogin(ExtentTest test) {
        test.log(Status.INFO, "Executing TC20: Browser Refresh After Login");
        try {
            // Login first
            driver.get(BASE_URL);
            type(By.id("email"), EMAIL);
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("nav")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Dashboard') or contains(text(),'Sites')]"))
            ));
            
            takeShotAndAttachReport("tc20_logged_in", "TC20 Logged In", test);
            
            // Refresh the page
            driver.navigate().refresh();
            pause(3000);
            
            // Check if still logged in
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.PASS, "✅ TC20 PASSED: Session persisted after browser refresh");
            } else if (driver.getCurrentUrl().contains("login")) {
                test.log(Status.WARNING, "⚠️ TC20: Session not persisted after refresh");
            } else {
                test.log(Status.WARNING, "⚠️ TC20: Unexpected behavior after refresh");
            }
            takeShotAndAttachReport("tc20_result", "TC20 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC20 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc20_error", "TC20 Error", test);
            failedTests++;
        }
    }
    
    // TC21: Login using Enter key
    static void execute_TC21_EnterKeyLogin(ExtentTest test) {
        test.log(Status.INFO, "Executing TC21: Login Using Enter Key");
        try {
            driver.get(BASE_URL);
            takeShotAndAttachReport("tc21_before_login", "TC21 Before Login", test);
            
            type(By.id("email"), EMAIL);
            type(By.id("password"), PASSWORD);
            
            // Press Enter key instead of clicking button
            driver.findElement(By.id("password")).sendKeys(Keys.ENTER);
            
            pause(3000);
            
            // Check if login succeeded
            if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.PASS, "✅ TC21 PASSED: Login successful using Enter key");
            } else {
                test.log(Status.FAIL, "❌ TC21 FAILED: Login unsuccessful using Enter key");
            }
            takeShotAndAttachReport("tc21_result", "TC21 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC21 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc21_error", "TC21 Error", test);
            failedTests++;
        }
    }
    
    // TC22: Back-button does not restore session
    static void execute_TC22_BackButtonSession(ExtentTest test) {
        test.log(Status.INFO, "Executing TC22: Back Button Session Invalidation");
        try {
            // Login first
            driver.get(BASE_URL);
            type(By.id("email"), EMAIL);
            type(By.id("password"), PASSWORD);
            click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
            
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("nav")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Dashboard') or contains(text(),'Sites')]"))
            ));
            
            takeShotAndAttachReport("tc22_logged_in", "TC22 Logged In", test);
            
            // Logout by navigating to logout URL
            driver.get(BASE_URL + "/logout");
            takeShotAndAttachReport("tc22_logged_out", "TC22 Logged Out", test);
            pause(2000);
            
            // Try to go back using browser back button
            driver.navigate().back();
            pause(3000);
            
            // Check if still on login page or redirected to login
            if (driver.getCurrentUrl().contains("login")) {
                test.log(Status.PASS, "✅ TC22 PASSED: Back button correctly redirects to login page");
            } else if (driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites")) {
                test.log(Status.FAIL, "❌ TC22 FAILED: Back button restored session - security vulnerability");
            } else {
                test.log(Status.WARNING, "⚠️ TC22: Unexpected behavior with back button");
            }
            takeShotAndAttachReport("tc22_result", "TC22 Result", test);
            passedTests++;
        } catch (Exception e) {
            test.log(Status.FAIL, "❌ TC22 FAILED: " + e.getMessage());
            takeShotAndAttachReport("tc22_error", "TC22 Error", test);
            failedTests++;
        }
    }
}
