package com.egalvanic.qa;

import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.utils.ConfigReader;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Demo class to show Page Object Model with Page Factory in action
 */
public class PageObjectDemo {
    public static void main(String[] args) {
        System.out.println("Page Object Model with Page Factory Demo");
        
        // Load configuration
        String baseUrl = ConfigReader.getBaseUrl();
        String email = ConfigReader.getUserEmail();
        String password = ConfigReader.getUserPassword();
        
        System.out.println("Configuration loaded:");
        System.out.println("- Base URL: " + baseUrl);
        System.out.println("- User Email: " + email);
        
        // Setup WebDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOpts = new ChromeOptions();
        chromeOpts.addArguments("--headless"); // Run in headless mode for demo
        chromeOpts.addArguments("--no-sandbox");
        chromeOpts.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(chromeOpts);
        
        try {
            // Navigate to the login page
            driver.get(baseUrl);
            System.out.println("Navigated to login page");
            
            // Create Page Object using Page Factory
            LoginPage loginPage = new LoginPage(driver);
            System.out.println("LoginPage object created with Page Factory");
            
            // Use the page object methods
            loginPage.enterEmail(email);
            System.out.println("Email entered");
            
            loginPage.enterPassword(password);
            System.out.println("Password entered");
            
            System.out.println("Demo completed successfully!");
        } catch (Exception e) {
            System.err.println("Error in demo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up
            if (driver != null) {
                driver.quit();
            }
        }
    }
}