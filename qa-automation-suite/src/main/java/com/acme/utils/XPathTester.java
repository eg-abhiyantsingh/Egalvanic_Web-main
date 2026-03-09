package com.acme.utils;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.util.List;

public class XPathTester {
    public static void main(String[] args) {
        System.out.println("Starting XPath test...");
        
        WebDriver driver = null;
        try {
            // Initialize driver
            driver = BaseConfig.getDriver();
            
            // Login
            driver.get(BaseConfig.BASE_URL);
            LoginPage loginPage = new LoginPage(driver);
            loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
            
            // Wait for page to load
            Thread.sleep(5000);
            
            System.out.println("Page loaded, testing XPath expressions...");
            
            // Test the updated XPath from DashboardPage
            testXPath(driver, "//div[contains(@class, 'MuiSelect-root') or contains(@class, 'site-selector') or @aria-label='Site Selector' or @id='site-selector' or contains(@class, 'MuiInputBase-root')][.//div[contains(@class, 'MuiSelect-select') or contains(text(), 'Select Site') or contains(text(), 'Test Site')]]", "Updated XPath");
            
            // Test simpler XPaths
            testXPath(driver, "//div[contains(@class, 'MuiSelect-root')]", "MuiSelect-root");
            testXPath(driver, "//div[contains(@class, 'site-selector')]", "site-selector class");
            testXPath(driver, "//div[@aria-label='Site Selector']", "aria-label Site Selector");
            testXPath(driver, "//div[@id='site-selector']", "id site-selector");
            testXPath(driver, "//div[contains(text(), 'Select Site')]", "Select Site text");
            testXPath(driver, "//div[contains(text(), 'Test Site')]", "Test Site text");
            
            // Test broader search
            testXPath(driver, "//div[contains(@class, 'MuiSelect')]", "MuiSelect (broad)");
            testXPath(driver, "//div[contains(@aria-label, 'Site')]", "aria-label containing Site");
            
            System.out.println("XPath testing completed.");
            
        } catch (Exception e) {
            System.err.println("Error during XPath testing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    private static void testXPath(WebDriver driver, String xpath, String description) {
        try {
            System.out.println("\nTesting: " + description);
            System.out.println("XPath: " + xpath);
            
            List<WebElement> elements = driver.findElements(By.xpath(xpath));
            System.out.println("Found " + elements.size() + " elements");
            
            for (int i = 0; i < elements.size(); i++) {
                WebElement element = elements.get(i);
                System.out.println("  Element " + i + ":");
                System.out.println("    Tag: " + element.getTagName());
                System.out.println("    Text: '" + element.getText() + "'");
                System.out.println("    Class: '" + element.getAttribute("class") + "'");
                System.out.println("    ID: '" + element.getAttribute("id") + "'");
                System.out.println("    Displayed: " + element.isDisplayed());
            }
        } catch (Exception e) {
            System.err.println("  Error testing " + description + ": " + e.getMessage());
        }
    }
}