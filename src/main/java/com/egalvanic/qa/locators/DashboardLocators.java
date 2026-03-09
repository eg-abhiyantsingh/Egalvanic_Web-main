package com.egalvanic.qa.locators;

import org.openqa.selenium.By;

/**
 * Locators for Dashboard Page Elements
 */
public class DashboardLocators {
    public static final By NAVIGATION_MENU = By.cssSelector("nav");
    public static final By DASHBOARD_HEADER = By.xpath("//*[contains(text(),'Dashboard') or contains(text(),'Sites')]");
    public static final By LOGOUT_LINK = By.xpath("//a[contains(@href,'logout') or contains(text(),'Logout')]");
    public static final By USER_PROFILE_ICON = By.cssSelector(".user-profile, .avatar");
    public static final By SETTINGS_LINK = By.linkText("Settings");
    public static final By HELP_LINK = By.linkText("Help");
}