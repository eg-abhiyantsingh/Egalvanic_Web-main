package com.egalvanic.qa.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for managing Extent Reports
 */
public class ReportManager {
    private static ExtentReports extent;
    private static ExtentReports summaryExtent; // Second report for pass/fail only
    private static ExtentSparkReporter sparkReporter;
    private static ExtentSparkReporter summarySparkReporter;
    
    /**
     * Initialize Extent Reports
     * @param reportFileName Name of the detailed report file
     * @param summaryReportFileName Name of the summary report file
     */
    public static void initReports(String reportFileName, String summaryReportFileName) {
        try {
            Files.createDirectories(Path.of("test-output/reports"));
            Files.createDirectories(Path.of("test-output/screenshots"));
        } catch (Exception e) {
            System.out.println("Failed to create directories: " + e.getMessage());
        }

        // Detailed report
        sparkReporter = new ExtentSparkReporter("test-output/reports/" + reportFileName);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("QA Automation Test Report");
        sparkReporter.config().setReportName("QA Automation Report");
        sparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        extent.setSystemInfo("Organization", "ACME");
        extent.setSystemInfo("Environment", "Test");
        extent.setSystemInfo("Tester", "QA Automation Engineer");
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        
        // Summary report (minimal)
        summarySparkReporter = new ExtentSparkReporter("test-output/reports/" + summaryReportFileName);
        summarySparkReporter.config().setTheme(Theme.STANDARD);
        summarySparkReporter.config().setDocumentTitle("QA Automation Summary Report");
        summarySparkReporter.config().setReportName("QA Automation Summary Report");
        summarySparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        summaryExtent = new ExtentReports();
        summaryExtent.attachReporter(summarySparkReporter);
        summaryExtent.setSystemInfo("Organization", "ACME");
        summaryExtent.setSystemInfo("Environment", "Test");
        summaryExtent.setSystemInfo("Tester", "QA Automation Engineer");
        summaryExtent.setSystemInfo("Java Version", System.getProperty("java.version"));
        summaryExtent.setSystemInfo("OS", System.getProperty("os.name"));
    }
    
    /**
     * Initialize Extent Reports (backward compatibility)
     * @param reportFileName Name of the report file
     */
    public static void initReports(String reportFileName) {
        initReports(reportFileName, "SummaryReport.html");
    }
    
    /**
     * Create a test in the detailed report
     * @param testName Name of the test
     * @return ExtentTest instance
     */
    public static ExtentTest createTest(String testName) {
        return extent.createTest(testName);
    }
    
    /**
     * Create a test in the summary report
     * @param testName Name of the test
     * @return ExtentTest instance
     */
    public static ExtentTest createSummaryTest(String testName) {
        return summaryExtent.createTest(testName);
    }
    
    /**
     * Create a node under a test in the detailed report
     * @param parentTest Parent test
     * @param nodeName Name of the node
     * @return ExtentTest instance for the node
     */
    public static ExtentTest createNode(ExtentTest parentTest, String nodeName) {
        return parentTest.createNode(nodeName);
    }
    
    /**
     * Create a node under a test in the summary report
     * @param parentTest Parent test
     * @param nodeName Name of the node
     * @return ExtentTest instance for the node
     */
    public static ExtentTest createSummaryNode(ExtentTest parentTest, String nodeName) {
        return parentTest.createNode(nodeName);
    }
    
    /**
     * Log a message with status to a test in the detailed report
     * @param test Test to log to
     * @param status Status of the log
     * @param message Message to log
     */
    public static void log(ExtentTest test, Status status, String message) {
        test.log(status, message);
    }
    
    /**
     * Log a message with status to a test in the summary report
     * For summary report, we want to show INFO messages for statistics as well
     * @param test Test to log to
     * @param status Status of the log
     * @param message Message to log
     */
    public static void logSummary(ExtentTest test, Status status, String message) {
        // For summary report, we log PASS/FAIL/INFO to show statistics
        if (status == Status.PASS || status == Status.FAIL || status == Status.INFO) {
            test.log(status, message);
        }
    }
    
    /**
     * Flush both reports to disk
     */
    public static void flushReports() {
        if (extent != null) {
            extent.flush();
        }
        if (summaryExtent != null) {
            summaryExtent.flush();
        }
    }
    
    /**
     * Get the detailed ExtentReports instance
     * @return ExtentReports instance
     */
    public static ExtentReports getExtent() {
        return extent;
    }
    
    /**
     * Get the summary ExtentReports instance
     * @return ExtentReports instance
     */
    public static ExtentReports getSummaryExtent() {
        return summaryExtent;
    }
}