package com.egalvanic.qa.testcase;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import com.egalvanic.qa.utils.ConfigReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base test class containing common functionality for all test cases
 */
public class BaseTest {
    protected static ExtentReports extent;
    protected static ExtentSparkReporter sparkReporter;
    
    protected static final int DEFAULT_TIMEOUT = ConfigReader.getDefaultTimeout();
    protected static final String BASE_URL = ConfigReader.getBaseUrl();
    protected static final String EMAIL = ConfigReader.getUserEmail();
    protected static final String PASSWORD = ConfigReader.getUserPassword();

    /**
     * Setup Extent Reports for test reporting
     */
    protected static void setupExtentReports() {
        try {
            String reportDir = ConfigReader.getProperty("report.directory", "test-output/reports");
            String screenshotDir = ConfigReader.getProperty("screenshot.directory", "test-output/screenshots");
            Files.createDirectories(Path.of(reportDir));
            Files.createDirectories(Path.of(screenshotDir));
        } catch (Exception e) {
            System.out.println("Failed to create directories: " + e.getMessage());
        }

        String reportFileName = ConfigReader.getProperty("report.directory", "test-output/reports") + "/TestReport.html";
        sparkReporter = new ExtentSparkReporter(reportFileName);
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
    }

    /**
     * Pause execution for specified milliseconds
     * @param ms Milliseconds to pause
     */
    protected static void pause(long ms) { 
        try { 
            Thread.sleep(ms); 
        } catch (InterruptedException ignored) {} 
    }

    /**
     * Generate timestamp for file naming
     * @return Formatted timestamp string
     */
    protected static String stamp() { 
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")); 
    }
}