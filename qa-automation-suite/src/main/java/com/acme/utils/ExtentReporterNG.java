package com.acme.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.ISuiteListener;
import org.testng.ISuite;
import org.testng.ITestContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExtentReporterNG implements ITestListener, ISuiteListener {
    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> classTest = new ThreadLocal<>();
    private static Map<String, ExtentTest> classTestsMap = new HashMap<>();
    
    /**
     * Initialize ExtentReports
     */
    public static ExtentReports getInstance() {
        if (extent == null) {
            createInstance();
        }
        return extent;
    }
    
    /**
     * Create ExtentReports instance
     */
    private static void createInstance() {
        // Create reports directory if it doesn't exist
        Path reportsDir = Paths.get("test-output", "reports");
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            System.err.println("Failed to create reports directory: " + e.getMessage());
        }
        
        // Create ExtentSparkReporter
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(
            reportsDir.resolve("AutomationReport.html").toString());
        
        // Configure reporter
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle(System.getProperty("report.title", "ACME Automation Report"));
        sparkReporter.config().setReportName(System.getProperty("report.name", "ACME Test Automation Report"));
        
        // Initialize ExtentReports
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        
        // Set system info
        extent.setSystemInfo("Organization", System.getProperty("organization", "ACME"));
        extent.setSystemInfo("Environment", System.getProperty("environment", "Test"));
        extent.setSystemInfo("Browser", System.getProperty("browser", "Chrome"));
        extent.setSystemInfo("Tester", "QA Automation Engineer");
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("OS", System.getProperty("os.name"));
    }
    
    /**
     * Create a test in the report with hierarchical structure
     */
    public static void createTest(String testName) {
        if (test.get() == null) {
            test.set(getInstance().createTest(testName));
        }
    }
    
    /**
     * Create a class-level test node
     */
    public static void createClassTest(String className) {
        ExtentTest classTestNode = classTestsMap.get(className);
        if (classTestNode == null) {
            classTestNode = getInstance().createTest("Class: " + className);
            classTestsMap.put(className, classTestNode);
        }
        classTest.set(classTestNode);
    }
    
    /**
     * Create a method-level test node under class node
     */
    public static void createMethodTest(String className, String methodName) {
        ExtentTest classTestNode = classTestsMap.get(className);
        if (classTestNode == null) {
            classTestNode = getInstance().createTest("Class: " + className);
            classTestsMap.put(className, classTestNode);
        }
        ExtentTest methodTest = classTestNode.createNode("Method: " + methodName);
        test.set(methodTest);
    }
    
    /**
     * Log info message
     */
    public static void logInfo(String message) {
        if (test.get() != null) {
            test.get().log(Status.INFO, message);
        }
    }
    
    /**
     * Log pass message
     */
    public static void logPass(String message) {
        if (test.get() != null) {
            test.get().log(Status.PASS, message);
        }
    }
    
    /**
     * Log fail message
     */
    public static void logFail(String message) {
        if (test.get() != null) {
            test.get().log(Status.FAIL, message);
        }
    }
    
    /**
     * Log warning message
     */
    public static void logWarning(String message) {
        if (test.get() != null) {
            test.get().log(Status.WARNING, message);
        }
    }
    
    /**
     * Log skip message
     */
    public static void logSkip(String message) {
        if (test.get() != null) {
            test.get().log(Status.SKIP, message);
        }
    }
    
    /**
     * Attach screenshot to report
     */
    public static void attachScreenshot(WebDriver driver, String testName) {
        try {
            // Take screenshot
            TakesScreenshot ts = (TakesScreenshot) driver;
            File source = ts.getScreenshotAs(OutputType.FILE);
            
            // Create screenshots directory
            Path screenshotsDir = Paths.get("test-output", "screenshots");
            Files.createDirectories(screenshotsDir);
            
            // Create screenshot file name
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = testName + "_" + timestamp + ".png";
            Path destination = screenshotsDir.resolve(fileName);
            
            // Copy screenshot to destination
            Files.copy(source.toPath(), destination);
            
            // Attach to report
            if (test.get() != null) {
                test.get().addScreenCaptureFromPath("../screenshots/" + fileName);
            } else {
                // If no test context, create a temporary one
                ExtentTest tempTest = getInstance().createTest("Screenshot: " + testName);
                tempTest.addScreenCaptureFromPath("../screenshots/" + fileName);
            }
        } catch (Exception e) {
            System.err.println("Failed to attach screenshot: " + e.getMessage());
        }
    }
    
    /**
     * Flush the report
     */
    public static void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
    
    // Implementing ITestListener methods
    @Override
    public void onTestStart(ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        createMethodTest(className, methodName);
        logInfo("Test started: " + methodName);
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        logPass("Test passed: " + result.getMethod().getMethodName());
        // Clear the thread local after test completion
        test.remove();
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        logFail("Test failed: " + result.getMethod().getMethodName());
        logFail("Failure reason: " + result.getThrowable().getMessage());
        
        // Attach screenshot on failure
        try {
            WebDriver driver = BaseConfig.getDriver();
            if (driver != null) {
                attachScreenshot(driver, "FAILURE_" + result.getMethod().getMethodName());
            }
        } catch (Exception e) {
            System.err.println("Failed to attach failure screenshot: " + e.getMessage());
        }
        
        // Clear the thread local after test completion
        test.remove();
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        logSkip("Test skipped: " + result.getMethod().getMethodName());
        // Clear the thread local after test completion
        test.remove();
    }
    
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        logWarning("Test failed but within success percentage: " + result.getMethod().getMethodName());
    }
    
    // Implementing ISuiteListener methods
    @Override
    public void onStart(ISuite suite) {
        getInstance(); // Initialize the extent report
    }
    
    @Override
    public void onFinish(ISuite suite) {
        flush(); // Flush the report at the end
    }
}