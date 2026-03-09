import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class demo_hierarchy {
    public static void main(String[] args) {
        try {
            // Create reports directory if it doesn't exist
            Files.createDirectories(Path.of("test-output/reports"));
            Files.createDirectories(Path.of("test-output/screenshots"));
        } catch (Exception e) {
            System.out.println("Failed to create directories: " + e.getMessage());
        }
        
        // Initialize ExtentSparkReporter
        String reportFileName = "test-output/reports/HierarchyDemoReport.html";
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportFileName);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Hierarchy Demo Report");
        sparkReporter.config().setReportName("Hierarchy Demo Report");
        
        // Initialize ExtentReports
        ExtentReports extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        extent.setSystemInfo("Organization", "ACME");
        extent.setSystemInfo("Environment", "Demo");
        extent.setSystemInfo("Tester", "QA Automation Engineer");
        
        // Create the hierarchical test structure as requested
        // Module = Assets
        ExtentTest moduleTest = extent.createTest("Module: Assets");
        moduleTest.assignCategory("Assets");
        
        // Feature = List
        ExtentTest featureTest = moduleTest.createNode("Feature: List");
        featureTest.assignCategory("List");
        
        // Sub-Feature = CRUD Assets
        ExtentTest crudSubFeatureTest = featureTest.createNode("Sub-Feature: CRUD Assets");
        crudSubFeatureTest.assignCategory("CRUD");
        
        // Sub-Feature = Search
        ExtentTest searchSubFeatureTest = featureTest.createNode("Sub-Feature: Search");
        searchSubFeatureTest.assignCategory("Search");
        
        // Add sample log entries to demonstrate the hierarchy
        crudSubFeatureTest.log(Status.INFO, "Starting UI testing for dashboard, create asset and edit asset flow");
        crudSubFeatureTest.log(Status.INFO, "Starting UI testing");
        crudSubFeatureTest.log(Status.PASS, "Login Successful");
        crudSubFeatureTest.log(Status.PASS, "Site Selection Successful");
        crudSubFeatureTest.log(Status.PASS, "Assets Page Loaded");
        crudSubFeatureTest.log(Status.PASS, "Asset Creation Phase 1 Successful");
        crudSubFeatureTest.log(Status.PASS, "Asset Edit Phase 2 Successful");
        crudSubFeatureTest.log(Status.PASS, "Asset deleted successfully");
        crudSubFeatureTest.log(Status.PASS, "Full UI flow completed successfully");
        
        searchSubFeatureTest.log(Status.INFO, "Starting search functionality testing");
        searchSubFeatureTest.log(Status.PASS, "Asset search completed successfully");
        searchSubFeatureTest.log(Status.PASS, "Search results displayed correctly");
        
        // Flush the report
        extent.flush();
        
        System.out.println("Hierarchy demo report generated successfully at: " + new File(reportFileName).getAbsolutePath());
    }
}