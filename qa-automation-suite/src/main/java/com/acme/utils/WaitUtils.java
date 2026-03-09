package com.acme.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class WaitUtils {
    private static final int DEFAULT_TIMEOUT = 20;
    private static final int POLLING_INTERVAL = 500;
    
    /**
     * Get WebDriverWait with default timeout
     * @param driver WebDriver instance
     * @return WebDriverWait instance
     */
    public static WebDriverWait getWebDriverWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    /**
     * Get WebDriverWait with custom timeout
     * @param driver WebDriver instance
     * @param timeoutInSeconds Timeout in seconds
     * @return WebDriverWait instance
     */
    public static WebDriverWait getWebDriverWait(WebDriver driver, int timeoutInSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
    }
    
    /**
     * Get FluentWait with custom configuration
     * @param driver WebDriver instance
     * @param timeoutInSeconds Timeout in seconds
     * @param pollingIntervalInMillis Polling interval in milliseconds
     * @return Wait instance
     */
    public static Wait<WebDriver> getFluentWait(WebDriver driver, int timeoutInSeconds, int pollingIntervalInMillis) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                .pollingEvery(Duration.ofMillis(pollingIntervalInMillis))
                .ignoring(org.openqa.selenium.NoSuchElementException.class);
    }
    
    /**
     * Wait for element to be visible
     * @param driver WebDriver instance
     * @param element WebElement to wait for
     * @return True if element is visible, false otherwise
     */
    public static boolean waitForElementVisible(WebDriver driver, WebElement element) {
        try {
            getWebDriverWait(driver).until(ExpectedConditions.visibilityOf(element));
            return element.isDisplayed();
        } catch (Exception e) {
            LoggerUtil.logError("Element not visible within timeout", e);
            return false;
        }
    }
    
    /**
     * Wait for element to be clickable
     * @param driver WebDriver instance
     * @param element WebElement to wait for
     * @return True if element is clickable, false otherwise
     */
    public static boolean waitForElementClickable(WebDriver driver, WebElement element) {
        try {
            getWebDriverWait(driver).until(ExpectedConditions.elementToBeClickable(element));
            return element.isEnabled();
        } catch (Exception e) {
            LoggerUtil.logError("Element not clickable within timeout", e);
            return false;
        }
    }
    
    /**
     * Wait for element to be present
     * @param driver WebDriver instance
     * @param locator Locator for the element
     * @return WebElement if present, null otherwise
     */
    public static WebElement waitForElementPresence(WebDriver driver, org.openqa.selenium.By locator) {
        try {
            return getWebDriverWait(driver).until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            LoggerUtil.logError("Element not present within timeout", e);
            return null;
        }
    }
    
    /**
     * Wait for text to be present in element
     * @param driver WebDriver instance
     * @param element WebElement to check
     * @param text Text to wait for
     * @return True if text is present, false otherwise
     */
    public static boolean waitForTextToBePresentInElement(WebDriver driver, WebElement element, String text) {
        try {
            return getWebDriverWait(driver).until(ExpectedConditions.textToBePresentInElement(element, text));
        } catch (Exception e) {
            LoggerUtil.logError("Text not present in element within timeout", e);
            return false;
        }
    }
    
    /**
     * Wait for a custom condition
     * @param driver WebDriver instance
     * @param condition Expected condition
     * @param <T> Return type
     * @return Result of the condition
     */
    public static <T> T waitForCondition(WebDriver driver, Function<WebDriver, T> condition) {
        try {
            return getWebDriverWait(driver).until(condition);
        } catch (Exception e) {
            LoggerUtil.logError("Custom condition not met within timeout", e);
            return null;
        }
    }
}