package com.egalvanic.qa.pageobjects;

import com.egalvanic.qa.testcase.BaseTest;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.By;

/**
 * Page Object Model for Login Page
 * Implements PageFactory pattern for better element management
 */
public class LoginPage {
    
    WebDriver driver;
    
    // PageFactory elements
    @FindBy(id = "email")
    WebElement emailField;
    
    @FindBy(id = "password")
    WebElement passwordField;
    
    @FindBy(xpath = "//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]")
    WebElement loginButton;
    
    @FindBy(xpath = "//div[contains(@class,'error') or contains(@class,'alert') or contains(text(),'Incorrect')]")
    WebElement errorMessage;
    
    /**
     * Constructor that initializes PageFactory elements
     */
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        // This initElements method will create all WebElements
        PageFactory.initElements(driver, this);
    }
    
    /**
     * Enter email in the email field
     * @param email Email to enter
     */
    public void enterEmail(String email) {
        emailField.clear();
        emailField.sendKeys(email);
    }
    
    /**
     * Enter password in the password field
     * @param password Password to enter
     */
    public void enterPassword(String password) {
        passwordField.clear();
        passwordField.sendKeys(password);
    }
    
    /**
     * Click the login button
     */
    public void clickLoginButton() {
        loginButton.click();
    }
    
    /**
     * Get the error message element
     * @return Error message WebElement
     */
    public String getErrorMessageText() {
        return errorMessage.getText();
    }
    
    /**
     * Check if error message is displayed
     * @return True if error message is displayed, false otherwise
     */
    public boolean isErrorMessageDisplayed() {
        try {
            return errorMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Perform login with email and password
     * @param email Email to login with
     * @param password Password to login with
     */
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
    }
}