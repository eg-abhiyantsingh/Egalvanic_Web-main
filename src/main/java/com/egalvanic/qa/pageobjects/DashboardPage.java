package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Page Object Model for Dashboard Page
 */
public class DashboardPage {
    
    WebDriver driver;
    WebDriverWait wait;
    
    // PageFactory elements
    @FindBy(css = "nav")
    WebElement navigationMenu;
    
    @FindBy(xpath = "//*[contains(text(),'Dashboard') or contains(text(),'Sites')]")
    WebElement dashboardHeader;
    
    @FindBy(xpath = "//a[contains(@href,'logout') or contains(text(),'Logout')]")
    WebElement logoutLink;
    
    static final int DEFAULT_TIMEOUT = 25;
    
    /**
     * Constructor that initializes PageFactory elements
     */
    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        // This initElements method will create all WebElements
        PageFactory.initElements(driver, this);
    }
    
    /**
     * Wait for dashboard to load
     * @return True if dashboard loaded, false otherwise
     */
    public boolean waitForDashboard() {
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("nav")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Dashboard') or contains(text(),'Sites')]"))
            ));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if user is on dashboard page
     * @return True if on dashboard, false otherwise
     */
    public boolean isOnDashboard() {
        return driver.getCurrentUrl().contains("dashboard") || driver.getCurrentUrl().contains("sites");
    }
    
    /**
     * Click logout link
     */
    public void clickLogout() {
        logoutLink.click();
    }
}