package com.egalvanic.qa.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.safari.SafariDriver;

/**
 * Utility class for managing WebDriver instances
 */
public class DriverManager {
    private static WebDriver driver;
    
    /**
     * Get WebDriver instance, creating one if it doesn't exist
     * @param browserType Browser type (chrome, firefox, edge, safari)
     * @return WebDriver instance
     */
    public static WebDriver getDriver(String browserType) {
        if (driver == null) {
            createDriver(browserType);
        }
        return driver;
    }
    
    /**
     * Create a new WebDriver instance
     * @param browserType Browser type (chrome, firefox, edge, safari)
     */
    public static void createDriver(String browserType) {
        if (driver != null) {
            driver.quit();
        }
        
        switch(browserType.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;
            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;
            case "safari":
                WebDriverManager.safaridriver().setup();
                driver = new SafariDriver();
                break;
            case "chrome":
            default:
                WebDriverManager.chromedriver().clearDriverCache();
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOpts = new ChromeOptions();
                chromeOpts.addArguments("--start-maximized");
                chromeOpts.addArguments("--remote-allow-origins=*");
                chromeOpts.addArguments("--disable-blink-features=AutomationControlled");
                chromeOpts.addArguments("--no-sandbox");
                chromeOpts.addArguments("--disable-dev-shm-usage");
                chromeOpts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
                chromeOpts.setExperimentalOption("useAutomationExtension", false);
                driver = new ChromeDriver(chromeOpts);
                break;
        }
        
        driver.manage().window().maximize();
    }
    
    /**
     * Quit the current WebDriver instance
     */
    public static void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
    
    /**
     * Navigate to a URL
     * @param url URL to navigate to
     */
    public static void navigateTo(String url) {
        if (driver != null) {
            driver.get(url);
        }
    }
}