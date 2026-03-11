package com.egalvanic.qa.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.egalvanic.qa.constants.AppConstants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Extent Report Manager - Dual Report System
 *
 * Two Reports:
 * 1. DETAILED REPORT - For QA Team (screenshots, logs, step details)
 * 2. CLIENT REPORT - For Client (Module > Feature > Test Name > Pass/Fail only)
 *
 * Features:
 * - Hierarchical structure: Module > Feature > Test
 * - Thread-safe for parallel execution
 */
public class ExtentReportManager {

    private static ExtentReports detailedReport;
    private static ExtentReports clientReport;

    private static ThreadLocal<ExtentTest> detailedTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> clientTest = new ThreadLocal<>();

    // Hierarchical nodes for Client Report
    private static Map<String, ExtentTest> clientModuleNodes = new HashMap<>();
    private static Map<String, ExtentTest> clientFeatureNodes = new HashMap<>();

    private static String timestamp;
    private static String detailedReportPath;
    private static String clientReportPath;

    private ExtentReportManager() {
        // Private constructor
    }

    /**
     * Initialize both reports
     */
    public static void initReports() {
        if (detailedReport != null) {
            System.out.println("Extent Reports already initialized - skipping");
            return;
        }
        timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        new File(AppConstants.DETAILED_REPORT_PATH).mkdirs();
        new File(AppConstants.CLIENT_REPORT_PATH).mkdirs();

        initDetailedReport();
        initClientReport();

        System.out.println("Both Extent Reports initialized");
    }

    /**
     * Initialize Detailed Report - Full details with screenshots
     */
    private static void initDetailedReport() {
        detailedReportPath = AppConstants.DETAILED_REPORT_PATH + "Detailed_Report_" + timestamp + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(detailedReportPath);
        spark.config().setDocumentTitle("eGalvanic Web Automation - Detailed Report");
        spark.config().setReportName("Detailed Test Execution Report");
        spark.config().setTheme(Theme.DARK);
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        spark.config().setEncoding("UTF-8");

        spark.config().setCss(
            ".badge-primary { background-color: #007bff; } " +
            ".badge-success { background-color: #28a745; } " +
            ".badge-danger { background-color: #dc3545; } " +
            ".badge-warning { background-color: #ffc107; } " +
            ".test-content { padding: 15px; } " +
            ".r-img { max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 4px; margin: 10px 0; } " +
            ".screen-img { max-width: 800px; cursor: pointer; } " +
            ".media-container img { max-width: 100%; height: auto; }"
        );

        detailedReport = new ExtentReports();
        detailedReport.attachReporter(spark);

        detailedReport.setSystemInfo("Application", "eGalvanic Web App");
        detailedReport.setSystemInfo("Platform", "Web");
        detailedReport.setSystemInfo("Browser", AppConstants.BROWSER);
        detailedReport.setSystemInfo("Base URL", AppConstants.BASE_URL);
        detailedReport.setSystemInfo("Framework", "Selenium + Page Object Model");
        detailedReport.setSystemInfo("Report Type", "DETAILED (Internal QA)");
        detailedReport.setSystemInfo("Environment", "QA");
        detailedReport.setSystemInfo("Executed By", System.getProperty("user.name"));

        System.out.println("Detailed Report initialized: " + detailedReportPath);
    }

    /**
     * Initialize Client Report - Clean Module > Feature > Test > Pass/Fail only
     */
    private static void initClientReport() {
        clientReportPath = AppConstants.CLIENT_REPORT_PATH + "Client_Report_" + timestamp + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(clientReportPath);
        spark.config().setDocumentTitle("eGalvanic Web - Test Results");
        spark.config().setReportName("Test Execution Summary");
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setTimeStampFormat("MMMM dd, yyyy");
        spark.config().setEncoding("UTF-8");

        spark.config().setCss(
            ".media-container, .r-img, .screen-img, img.r-img { display: none !important; } " +
            "pre, code, .exception-part, .stack-trace { display: none !important; } " +
            ".details-col { display: none !important; } " +
            "table.table tbody tr td:nth-child(3) { display: none !important; } " +
            ".event-row { display: none !important; } " +
            "table.table { display: none !important; } " +
            ".category-list, .tag, .badge-pill, .node-attr { display: none !important; } " +
            ".test-item { border-left: 4px solid #007bff !important; margin-bottom: 10px !important; " +
            "             background: #fff !important; border-radius: 4px !important; box-shadow: 0 1px 3px rgba(0,0,0,0.1) !important; } " +
            ".card { border: none !important; border-left: 3px solid #17a2b8 !important; " +
            "        margin: 8px 0 8px 20px !important; background: #f8f9fa !important; } " +
            ".card-header { background: transparent !important; padding: 10px 15px !important; border-bottom: none !important; } " +
            ".card .card { border-left: 3px solid #28a745 !important; margin-left: 20px !important; " +
            "              background: #fff !important; } " +
            ".node, .card-title a { font-size: 14px !important; font-weight: 500 !important; " +
            "                       color: #333 !important; text-decoration: none !important; } " +
            ".card-title { margin-bottom: 0 !important; } " +
            ".badge.pass-bg, .badge-success, span.badge.log.pass-bg { " +
            "    background-color: #28a745 !important; color: #fff !important; " +
            "    font-size: 11px !important; padding: 4px 10px !important; border-radius: 3px !important; " +
            "    font-weight: 600 !important; } " +
            ".badge.fail-bg, .badge-danger, span.badge.log.fail-bg { " +
            "    background-color: #dc3545 !important; color: #fff !important; " +
            "    font-size: 11px !important; padding: 4px 10px !important; border-radius: 3px !important; " +
            "    font-weight: 600 !important; } " +
            ".badge.skip-bg, .badge-warning { " +
            "    background-color: #ffc107 !important; color: #000 !important; " +
            "    font-size: 11px !important; padding: 4px 10px !important; border-radius: 3px !important; } " +
            ".node-info .badge-default { display: none !important; } " +
            ".header.navbar { background: #2c3e50 !important; } " +
            ".badge-primary { background: #3498db !important; } " +
            ".card-toolbar ul li:not(:first-child) { display: none !important; } " +
            "span.ct, span.et, span.ne, .uri-anchor { display: none !important; } " +
            ".test-list-item .test-item .test-detail { padding: 12px 15px !important; display: block !important; } " +
            ".test-item .name { font-size: 16px !important; font-weight: 600 !important; color: #2c3e50 !important; margin-bottom: 5px !important; } " +
            ".test-item .text-sm { font-size: 12px !important; color: #666 !important; } " +
            ".detail-head .info span.badge-danger, .detail-head .info span.badge-default { display: none !important; } " +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif !important; " +
            "       background: #f5f6fa !important; } " +
            ".main-content { background: #f5f6fa !important; } " +
            ".test-wrapper { padding: 20px !important; } " +
            ".vcontainer { background: #f5f6fa !important; } " +
            ".card-body { display: none !important; } " +
            ".collapse { display: none !important; } "
        );

        clientReport = new ExtentReports();
        clientReport.attachReporter(spark);

        clientReport.setSystemInfo("Application", "eGalvanic Web");
        clientReport.setSystemInfo("Test Date", new SimpleDateFormat("MMMM dd, yyyy").format(new Date()));

        System.out.println("Client Report initialized: " + clientReportPath);
    }

    /**
     * Create test with Module > Feature > Test Name hierarchy
     */
    public static void createTest(String moduleName, String featureName, String testName) {
        // DETAILED REPORT: Flat test with categories
        ExtentTest detailed = detailedReport.createTest(testName);
        detailed.assignCategory(moduleName, featureName);
        detailedTest.set(detailed);

        // CLIENT REPORT: Hierarchical Module > Feature > Test
        String moduleKey = moduleName;
        ExtentTest moduleNode = clientModuleNodes.get(moduleKey);
        if (moduleNode == null) {
            moduleNode = clientReport.createTest(moduleName);
            clientModuleNodes.put(moduleKey, moduleNode);
        }

        String featureKey = moduleName + "|" + featureName;
        ExtentTest featureNode = clientFeatureNodes.get(featureKey);
        if (featureNode == null) {
            featureNode = moduleNode.createNode(featureName);
            clientFeatureNodes.put(featureKey, featureNode);
        }

        ExtentTest testNode = featureNode.createNode(testName);
        clientTest.set(testNode);
    }

    // ================================================================
    // LOGGING METHODS - DETAILED REPORT ONLY
    // ================================================================

    public static void logInfo(String message) {
        ExtentTest test = detailedTest.get();
        if (test != null) {
            test.log(Status.INFO, message);
        }
    }

    public static void logStepWithScreenshot(String step) {
        ExtentTest test = detailedTest.get();
        if (test != null) {
            test.log(Status.INFO, step);
            String base64 = ScreenshotUtil.getScreenshotAsBase64();
            if (base64 != null) {
                try {
                    test.addScreenCaptureFromBase64String(base64);
                } catch (Exception e) {
                    System.out.println("Screenshot attachment failed: " + e.getMessage());
                }
            }
        }
    }

    public static void logWarning(String message) {
        ExtentTest test = detailedTest.get();
        if (test != null) {
            test.log(Status.WARNING, message);
        }
    }

    // ================================================================
    // RESULT METHODS - BOTH REPORTS
    // ================================================================

    public static void logPass(String message) {
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.PASS, message);
        }
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.pass("PASS");
        }
        incrementPassed();
    }

    public static void logFail(String message) {
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.FAIL, message);
        }
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.fail("FAIL");
        }
        incrementFailed();
    }

    public static void logFailWithScreenshot(String message, Throwable throwable) {
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.FAIL, message);
            if (throwable != null) {
                detailed.log(Status.FAIL, throwable);
            }
            String base64 = ScreenshotUtil.getScreenshotAsBase64();
            if (base64 != null) {
                try {
                    detailed.addScreenCaptureFromBase64String(base64);
                } catch (Exception e) {
                    System.out.println("Screenshot attachment failed: " + e.getMessage());
                }
            }
        }
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.fail("FAIL");
        }
        incrementFailed();
    }

    public static void logSkip(String message) {
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.SKIP, message);
        }
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.skip("SKIP");
        }
        incrementSkipped();
    }

    // ================================================================
    // CLEANUP METHODS
    // ================================================================

    public static void removeTests() {
        detailedTest.remove();
        clientTest.remove();
    }

    public static void flushReports() {
        if (detailedReport != null) {
            detailedReport.flush();
            System.out.println("Detailed Report generated: " + detailedReportPath);
        }
        if (clientReport != null) {
            clientReport.flush();
            System.out.println("Client Report generated: " + clientReportPath);
        }

        // Send report email (if enabled)
        EmailUtil.sendReportEmail(detailedReportPath, clientReportPath,
                totalPassed, totalFailed, totalSkipped);
    }

    // ================================================================
    // TEST RESULT COUNTERS (for email summary)
    // ================================================================

    private static int totalPassed = 0;
    private static int totalFailed = 0;
    private static int totalSkipped = 0;

    public static void incrementPassed() { totalPassed++; }
    public static void incrementFailed() { totalFailed++; }
    public static void incrementSkipped() { totalSkipped++; }

    public static String getDetailedReportPath() {
        return detailedReportPath;
    }

    public static String getClientReportPath() {
        return clientReportPath;
    }
}
