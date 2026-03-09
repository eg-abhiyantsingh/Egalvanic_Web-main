package com.egalvanic.qa.locators;

import org.openqa.selenium.By;

/**
 * Locators for Login Page Elements
 */
public class LoginLocators {
    public static final By EMAIL_FIELD = By.id("email");
    public static final By PASSWORD_FIELD = By.id("password");
    public static final By LOGIN_BUTTON = By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]");
    public static final By ERROR_MESSAGE = By.xpath("//div[contains(@class,'error') or contains(@class,'alert') or contains(text(),'Incorrect')]");
    public static final By FORGOT_PASSWORD_LINK = By.linkText("Forgot Password");
}