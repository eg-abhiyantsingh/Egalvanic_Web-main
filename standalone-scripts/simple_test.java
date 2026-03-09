import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.github.bonigarcia.wdm.WebDriverManager;

public class SimpleTest {
    public static void main(String[] args) {
        WebDriver driver = null;
        try {
            // Setup WebDriver
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            
            driver = new ChromeDriver(options);
            
            // Navigate to the ACME site
            System.out.println("Navigating to ACME site...");
            driver.get("https://acme.egalvanic.ai");
            
            // Wait for page to load
            Thread.sleep(5000);
            
            // Take screenshot
            takeScreenshot(driver, "simple_test_screenshot");
            
            System.out.println("Test completed. Screenshot saved.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close the browser
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    private static void takeScreenshot(WebDriver driver, String name) {
        try {
            // Create screenshots directory if it doesn't exist
            Path screenshotsDir = Paths.get("dropdown_test_screenshots");
            Files.createDirectories(screenshotsDir);
            
            // Generate timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // Take screenshot
            TakesScreenshot ts = (TakesScreenshot) driver;
            File source = ts.getScreenshotAs(OutputType.FILE);
            
            // Save screenshot
            Path destination = screenshotsDir.resolve(name + "_" + timestamp + ".png");
            Files.copy(source.toPath(), destination);
            
            System.out.println("Screenshot saved: " + destination.toString());
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }
}