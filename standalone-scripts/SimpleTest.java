import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

public class SimpleTest {
    public static void main(String[] args) {
        // Create ExtentReports instance
        ExtentReports extent = new ExtentReports();
        
        // Create reporter
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter("test-output/SimpleTestReport.html");
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Simple Test Report");
        sparkReporter.config().setReportName("Simple Test Report");
        
        // Attach reporter
        extent.attachReporter(sparkReporter);
        
        // Create hierarchical test structure
        ExtentTest moduleTest = extent.createTest("Module: Assets");
        ExtentTest featureTest = moduleTest.createNode("Feature: List");
        ExtentTest crudSubFeatureTest = featureTest.createNode("Sub-Feature: CRUD Assets");
        ExtentTest searchSubFeatureTest = featureTest.createNode("Sub-Feature: Search");
        
        // Add some test steps
        crudSubFeatureTest.log(Status.INFO, "Starting CRUD operations");
        crudSubFeatureTest.log(Status.PASS, "Asset created successfully");
        crudSubFeatureTest.log(Status.PASS, "Asset updated successfully");
        crudSubFeatureTest.log(Status.PASS, "Asset deleted successfully");
        
        searchSubFeatureTest.log(Status.INFO, "Starting search operations");
        searchSubFeatureTest.log(Status.PASS, "Asset search completed successfully");
        
        // Flush the report
        extent.flush();
        
        System.out.println("Simple test report generated successfully!");
    }
}