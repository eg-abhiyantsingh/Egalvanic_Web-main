package com.acme.tests.ui;

import com.acme.config.BaseConfig;
import com.acme.pages.LoginPage;
import com.acme.pages.DashboardPage;
import com.acme.utils.ExtentReporterNG;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.time.Duration;

public class QuickDropdownTest {
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    
    @BeforeClass
    public void setUp() {
        driver = BaseConfig.getDriver();
        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
    }
    
    @Test
    public void quickDropdownTest() {
        System.out.println("Starting quick dropdown test...");
        
        // Login first
        driver.get(BaseConfig.BASE_URL);
        loginPage.login(BaseConfig.TEST_EMAIL, BaseConfig.TEST_PASSWORD);
        
        // Wait for dashboard
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("Checking if site dropdown is present...");
        
        // Check if dropdown is present using the updated method
        boolean isDropdownPresent = dashboardPage.isSiteDropdownPresent();
        System.out.println("Site dropdown present: " + isDropdownPresent);
        
        if (isDropdownPresent) {
            System.out.println("Dropdown is present, attempting to select Test Site...");
            boolean selectionSuccessful = dashboardPage.selectTestSite();
            System.out.println("Selection successful: " + selectionSuccessful);
        } else {
            // Let's try to find the dropdown with a more direct approach
            System.out.println("Dropdown not found with page object method, trying direct XPath...");
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement dropdownElement = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//div[contains(@class, 'MuiSelect-root') or contains(@class, 'site-selector') or @aria-label='Site Selector' or @id='site-selector' or contains(@class, 'MuiInputBase-root')][.//div[contains(@class, 'MuiSelect-select') or contains(text(), 'Select Site') or contains(text(), 'Test Site')]]")
                    )
                );
                System.out.println("Direct XPath found dropdown element: " + (dropdownElement != null));
                if (dropdownElement != null) {
                    System.out.println("Dropdown element text: " + dropdownElement.getText());
                    System.out.println("Dropdown element tag: " + dropdownElement.getTagName());
                    System.out.println("Dropdown element class: " + dropdownElement.getAttribute("class"));
                }
            } catch (Exception e) {
                System.out.println("Direct XPath approach also failed: " + e.getMessage());
                
                // Try an even broader search
                try {
                    java.util.List<WebElement> potentialDropdowns = driver.findElements(
                        By.xpath("//div[contains(@class, 'MuiSelect') or contains(@class, 'site-selector') or @aria-label='Site Selector' or contains(text(), 'Select') or contains(text(), 'Test Site')]")
                    );
                    System.out.println("Found " + potentialDropdowns.size() + " potential dropdown elements");
                    for (int i = 0; i < potentialDropdowns.size(); i++) {
                        WebElement element = potentialDropdowns.get(i);
                        System.out.println("Element " + i + ": Tag=" + element.getTagName() + 
                                         ", Text='" + element.getText() + "'" + 
                                         ", Class='" + element.getAttribute("class") + "'" +
                                         ", ID='" + element.getAttribute("id") + "'");
                    }
                } catch (Exception e2) {
                    System.out.println("Broad search also failed: " + e2.getMessage());
                }
            }
        }
    }
    
    @AfterClass
    public void tearDown() {
        BaseConfig.closeDriver();
    }
}