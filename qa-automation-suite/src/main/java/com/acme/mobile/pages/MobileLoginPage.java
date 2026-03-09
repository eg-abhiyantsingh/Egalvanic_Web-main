package com.acme.mobile.pages;

import com.acme.mobile.MobileActions;
import com.acme.utils.LoggerUtil;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

public class MobileLoginPage {
    private AppiumDriver driver;
    private MobileActions mobileActions;
    
    // Page elements for Android
    @AndroidFindBy(id = "email")
    private WebElement emailFieldAndroid;
    
    @AndroidFindBy(id = "password")
    private WebElement passwordFieldAndroid;
    
    @AndroidFindBy(xpath = "//android.widget.Button[@text='Sign In']")
    private WebElement loginButtonAndroid;
    
    // Page elements for iOS
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND name CONTAINS 'email'")
    private WebElement emailFieldIOS;
    
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeSecureTextField' AND name CONTAINS 'password'")
    private WebElement passwordFieldIOS;
    
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND name == 'Sign In'")
    private WebElement loginButtonIOS;
    
    // Common elements (if applicable)
    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text, 'Error')]")
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND name CONTAINS 'Error'")
    private WebElement errorMessage;
    
    public MobileLoginPage(AppiumDriver driver) {
        this.driver = driver;
        this.mobileActions = new MobileActions(driver);
        PageFactory.initElements(driver, this);
    }
    
    public void enterEmail(String email) {
        try {
            LoggerUtil.logInfo("Entering email: " + email);
            WebElement emailField = getEmailField();
            emailField.clear();
            emailField.sendKeys(email);
            mobileActions.hideKeyboard();
        } catch (Exception e) {
            LoggerUtil.logError("Failed to enter email", e);
            throw e;
        }
    }
    
    public void enterPassword(String password) {
        try {
            LoggerUtil.logInfo("Entering password");
            WebElement passwordField = getPasswordField();
            passwordField.clear();
            passwordField.sendKeys(password);
            mobileActions.hideKeyboard();
        } catch (Exception e) {
            LoggerUtil.logError("Failed to enter password", e);
            throw e;
        }
    }
    
    public void clickLoginButton() {
        try {
            LoggerUtil.logInfo("Clicking login button");
            WebElement loginButton = getLoginButton();
            mobileActions.tap(loginButton);
        } catch (Exception e) {
            LoggerUtil.logError("Failed to click login button", e);
            throw e;
        }
    }
    
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
    }
    
    public boolean isErrorMessageDisplayed() {
        try {
            return errorMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getErrorMessageText() {
        if (isErrorMessageDisplayed()) {
            return errorMessage.getText();
        }
        return "";
    }
    
    // Helper methods to get platform-specific elements
    private WebElement getEmailField() {
        String platform = driver.getCapabilities().getPlatformName().toString().toLowerCase();
        if (platform.contains("android")) {
            return emailFieldAndroid;
        } else {
            return emailFieldIOS;
        }
    }
    
    private WebElement getPasswordField() {
        String platform = driver.getCapabilities().getPlatformName().toString().toLowerCase();
        if (platform.contains("android")) {
            return passwordFieldAndroid;
        } else {
            return passwordFieldIOS;
        }
    }
    
    private WebElement getLoginButton() {
        String platform = driver.getCapabilities().getPlatformName().toString().toLowerCase();
        if (platform.contains("android")) {
            return loginButtonAndroid;
        } else {
            return loginButtonIOS;
        }
    }
}