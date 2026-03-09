package com.acme.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;

public class LoginPage {
    private WebDriver driver;
    private WebDriverWait wait;
    private static final int DEFAULT_TIMEOUT = 20;
    
    // Constructor
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        PageFactory.initElements(driver, this);
    }
    
    // Page elements with more specific locators based on actual page structure
    @FindBy(id = "email")
    private WebElement emailField;
    
    @FindBy(id = "password")
    private WebElement passwordField;
    
    @FindBy(xpath = "//button[@type='submit' and contains(text(), 'Sign In')]")
    private WebElement loginButton;
    
    @FindBy(xpath = "//div[contains(@class, 'MuiAlert-root') or contains(@class, 'error') or contains(@class, 'Mui-error')]")
    private WebElement errorMessage;
    
    // Methods
    public void enterEmail(String email) {
        try {
            wait.until(ExpectedConditions.visibilityOf(emailField));
            emailField.clear();
            emailField.sendKeys(email);
        } catch (TimeoutException e) {
            throw new RuntimeException("Email field not visible within timeout: " + e.getMessage(), e);
        }
    }
    
    public void enterPassword(String password) {
        try {
            wait.until(ExpectedConditions.visibilityOf(passwordField));
            passwordField.clear();
            passwordField.sendKeys(password);
        } catch (TimeoutException e) {
            throw new RuntimeException("Password field not visible within timeout: " + e.getMessage(), e);
        }
    }
    
    public void clickLoginButton() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(loginButton));
            loginButton.click();
        } catch (TimeoutException e) {
            throw new RuntimeException("Login button not clickable within timeout: " + e.getMessage(), e);
        }
    }
    
    public boolean isErrorMessageDisplayed() {
        try {
            return errorMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean waitForErrorMessage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(errorMessage));
            return errorMessage.isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
    
    public String getErrorMessageText() {
        if (isErrorMessageDisplayed()) {
            return errorMessage.getText();
        }
        return "";
    }
    
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
    }
    
    public boolean isLoginPageDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(emailField));
            wait.until(ExpectedConditions.visibilityOf(passwordField));
            return emailField.isDisplayed() && passwordField.isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
    
    public boolean isEmailFieldVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(emailField));
            return emailField.isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
    
    public boolean isPasswordFieldVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(passwordField));
            return passwordField.isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
    
    public boolean isLoginButtonVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(loginButton));
            return loginButton.isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
}