package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.JavascriptExecutor;
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
     * Check if user is on one of the dashboard pages.
     *
     * <p>Matches the PATH exactly. A substring test on "dashboard" also matched V1.36's
     * {@code /admin-dashboard} (the Setup hub) and {@code /ops-dashboard}, so a test that had
     * been redirected into Setup still reported "on dashboard" and carried on. The old
     * {@code "sites"} arm matched no live route at all — Sites is now a tab on /customers.
     */
    public boolean isOnDashboard() {
        String path = com.egalvanic.qa.utils.NavCatalog.pathOf(driver.getCurrentUrl());
        return path.equals("/dashboard")
                || path.equals("/sales-overview")
                || path.equals("/ops-dashboard");
    }

    /**
     * Sign out via the account menu.
     *
     * <p>V1.36 moved this: there is no logout anchor any more. The avatar button sits at the
     * bottom of the icon rail and opens a popover whose action is labelled "Sign Out".
     */
    public void clickLogout() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "var av=document.querySelector('.MuiAvatar-root');"
          + "if(av){ var b=av.closest('button')||av; b.click(); }");
        try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        By signOut = By.xpath("//button[normalize-space()='Sign Out' or normalize-space()='Sign out'"
                + " or normalize-space()='Logout' or normalize-space()='Log Out']");
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(signOut))
                .click();
    }
}