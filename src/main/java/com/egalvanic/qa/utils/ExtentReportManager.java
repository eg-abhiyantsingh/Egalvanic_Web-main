package com.egalvanic.qa.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.egalvanic.qa.constants.AppConstants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    // Per-module Detailed Reports — one HTML file per Module so reviewers can open just
    // the module they care about (Auth, Asset, WO, etc.) and see all its screenshots.
    // Insertion-ordered so generation order matches first-seen order.
    private static final Map<String, ExtentReports> detailedByModule = new LinkedHashMap<>();
    private static final Map<String, String> detailedPathsByModule = new LinkedHashMap<>();
    // Per-module fail counters — used by EmailUtil to prioritize attachments under the size cap.
    private static final Map<String, Integer> failsByModule = new HashMap<>();

    private static ExtentReports clientReport;

    private static ThreadLocal<ExtentTest> detailedTest = new ThreadLocal<>();
    private static ThreadLocal<String> activeModule = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> clientTest = new ThreadLocal<>();

    // Hierarchical nodes for Client Report
    private static Map<String, ExtentTest> clientModuleNodes = new HashMap<>();
    private static Map<String, ExtentTest> clientFeatureNodes = new HashMap<>();

    private static String timestamp;
    private static String clientReportPath;

    private ExtentReportManager() {
        // Private constructor
    }

    /**
     * Initialize both reports
     */
    public static void initReports() {
        if (clientReport != null) {
            System.out.println("Extent Reports already initialized - skipping");
            return;
        }
        timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        new File(AppConstants.DETAILED_REPORT_PATH).mkdirs();
        new File(AppConstants.CLIENT_REPORT_PATH).mkdirs();

        // Per-module Detailed Reports are created lazily on first test of each module.
        initClientReport();

        System.out.println("Reports initialized — Client Report ready; Detailed Reports will create per module on demand");
    }

    /**
     * Lazily initialize a Detailed Report for a single module. Called the first time
     * createTest(module, ...) is invoked for a given module name. Idempotent.
     */
    private static ExtentReports getOrCreateModuleReport(String moduleName) {
        ExtentReports existing = detailedByModule.get(moduleName);
        if (existing != null) return existing;

        String safeName = moduleName == null ? "_Unknown" : moduleName.replaceAll("[^a-zA-Z0-9_-]+", "_");
        String path = AppConstants.DETAILED_REPORT_PATH
                + "Detailed_Report_" + safeName + "_" + timestamp + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(path);
        spark.config().setDocumentTitle("eGalvanic Web Automation - " + moduleName + " Detailed Report");
        spark.config().setReportName(moduleName + " — Detailed Test Execution");
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

        ExtentReports report = new ExtentReports();
        report.attachReporter(spark);
        report.setSystemInfo("Application", "eGalvanic Web App");
        report.setSystemInfo("Module", moduleName);
        report.setSystemInfo("Platform", "Web");
        report.setSystemInfo("Browser", AppConstants.BROWSER);
        report.setSystemInfo("Base URL", AppConstants.BASE_URL);
        report.setSystemInfo("Framework", "Selenium + Page Object Model");
        report.setSystemInfo("Report Type", "DETAILED (Internal QA)");
        report.setSystemInfo("Environment", "QA");
        report.setSystemInfo("Executed By", System.getProperty("user.name"));

        detailedByModule.put(moduleName, report);
        detailedPathsByModule.put(moduleName, path);
        failsByModule.put(moduleName, 0);
        System.out.println("Detailed Report initialized for module '" + moduleName + "': " + path);
        return report;
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
     * Create test with Module > Feature > Test Name hierarchy.
     * The Detailed Report is per-module — each module gets its own HTML file,
     * lazy-created the first time a test names that module.
     */
    public static void createTest(String moduleName, String featureName, String testName) {
        // DETAILED REPORT: per-module — create or reuse this module's ExtentReports,
        // then create the test inside it with Feature as category for grouping.
        String safeModule = (moduleName == null || moduleName.isEmpty()) ? "_Unknown" : moduleName;
        ExtentReports moduleReport = getOrCreateModuleReport(safeModule);
        ExtentTest detailed = moduleReport.createTest(testName);
        if (featureName != null) {
            detailed.assignCategory(featureName);
        }
        detailedTest.set(detailed);
        activeModule.set(safeModule);

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

    /**
     * Build an HTML log message that renders the screenshot INLINE in the step row
     * (not as a collapsible "base64 img" badge). ExtentSpark renders log message text
     * as HTML, so embedding a data-URI &lt;img&gt; tag puts the screenshot directly
     * inside the step row.
     */
    private static String buildInlineScreenshotHtml(String message, String base64) {
        StringBuilder sb = new StringBuilder();
        if (message != null && !message.isEmpty()) {
            sb.append("<div style='margin-bottom:6px;'>").append(message).append("</div>");
        }
        sb.append("<img src='data:image/jpeg;base64,").append(base64)
          .append("' style='max-width:900px;width:100%;height:auto;");
        sb.append("border:1px solid #444;border-radius:4px;display:block;margin-top:4px;' ");
        sb.append("alt='step screenshot'/>");
        return sb.toString();
    }

    public static void logStepWithScreenshot(String step) {
        ExtentTest test = detailedTest.get();
        if (test == null) return;
        String base64 = ScreenshotUtil.getCompressedScreenshotAsBase64();
        if (base64 != null) {
            try {
                // Inline render — the <img> lives inside the step's log message HTML.
                test.log(Status.INFO, buildInlineScreenshotHtml(step, base64));
            } catch (Exception e) {
                System.out.println("Screenshot attachment failed: " + e.getMessage());
                test.log(Status.INFO, step);
            }
        } else {
            test.log(Status.INFO, step);
        }
    }

    /**
     * Log a step + capture a screenshot every time (for high-detail reports).
     * Alias for logStepWithScreenshot — kept short for inline use in tests.
     */
    public static void step(String message) {
        logStepWithScreenshot(message);
    }

    /**
     * Capture a screenshot without a step message — used by BaseTest lifecycle
     * hooks to record page state at test boundaries. Renders inline (not as a badge).
     */
    public static void captureScreenshot(String caption) {
        ExtentTest test = detailedTest.get();
        if (test == null) return;
        String base64 = ScreenshotUtil.getCompressedScreenshotAsBase64();
        if (base64 == null) return;
        try {
            test.log(Status.INFO, buildInlineScreenshotHtml(caption, base64));
        } catch (Exception e) {
            System.out.println("Screenshot attachment failed: " + e.getMessage());
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
            String base64 = ScreenshotUtil.getCompressedScreenshotAsBase64();
            if (base64 != null) {
                // Inline render — fail message AND screenshot share one row with red border
                String html = "<div style='margin-bottom:6px;'>" + message + "</div>"
                        + "<img src='data:image/jpeg;base64," + base64
                        + "' style='max-width:900px;width:100%;height:auto;"
                        + "border:2px solid #dc3545;border-radius:4px;display:block;margin-top:4px;' "
                        + "alt='failure screenshot'/>";
                try {
                    detailed.log(Status.FAIL, html);
                } catch (Exception e) {
                    System.out.println("Screenshot attachment failed: " + e.getMessage());
                    detailed.log(Status.FAIL, message);
                }
            } else {
                detailed.log(Status.FAIL, message);
            }
            if (throwable != null) {
                detailed.log(Status.FAIL, throwable);
            }
        }
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.fail("FAIL");
        }
        // Per-module counter for email prioritization
        String module = activeModule.get();
        if (module != null) {
            failsByModule.merge(module, 1, Integer::sum);
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
        activeModule.remove();
        clientTest.remove();
    }

    public static void flushReports() {
        // Flush every per-module Detailed Report
        List<String> detailedPaths = new ArrayList<>();
        for (Map.Entry<String, ExtentReports> e : detailedByModule.entrySet()) {
            e.getValue().flush();
            String path = detailedPathsByModule.get(e.getKey());
            detailedPaths.add(path);
            int fails = failsByModule.getOrDefault(e.getKey(), 0);
            System.out.println("Detailed Report generated for module '" + e.getKey()
                    + "' (" + fails + " failures): " + path);
        }
        if (clientReport != null) {
            clientReport.flush();
            System.out.println("Client Report generated: " + clientReportPath);
        }

        // Send report email (if enabled)
        EmailUtil.sendReportEmail(detailedPaths, detailedPathsByModule, failsByModule,
                clientReportPath, totalPassed, totalFailed, totalSkipped);
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

    /** @deprecated Detailed Reports are now per-module — use {@link #getDetailedReportPaths()}. */
    @Deprecated
    public static String getDetailedReportPath() {
        if (detailedPathsByModule.isEmpty()) return null;
        return detailedPathsByModule.values().iterator().next();
    }

    public static List<String> getDetailedReportPaths() {
        return new ArrayList<>(detailedPathsByModule.values());
    }

    public static Map<String, String> getDetailedReportPathsByModule() {
        return new LinkedHashMap<>(detailedPathsByModule);
    }

    public static Map<String, Integer> getFailsByModule() {
        return new HashMap<>(failsByModule);
    }

    public static String getClientReportPath() {
        return clientReportPath;
    }
}
