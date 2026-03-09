package com.acme.mobile;

import com.acme.utils.LoggerUtil;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class MobileActions {
    private AppiumDriver driver;
    private WebDriverWait wait;
    private TouchAction touchAction;
    
    public MobileActions(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.touchAction = new TouchAction(driver);
    }
    
    /**
     * Tap on an element
     */
    public void tap(WebElement element) {
        try {
            element.click();
            LoggerUtil.logInfo("Tapped on element");
        } catch (Exception e) {
            LoggerUtil.logError("Failed to tap on element", e);
            throw e;
        }
    }
    
    /**
     * Long press on an element
     */
    public void longPress(WebElement element) {
        try {
            touchAction.longPress(PointOption.point(element.getLocation().getX(), element.getLocation().getY()))
                      .release()
                      .perform();
            LoggerUtil.logInfo("Long pressed on element");
        } catch (Exception e) {
            LoggerUtil.logError("Failed to long press on element", e);
            throw e;
        }
    }
    
    /**
     * Swipe from one point to another
     */
    public void swipe(int startX, int startY, int endX, int endY) {
        try {
            touchAction.press(PointOption.point(startX, startY))
                      .waitAction(WaitOptions.waitOptions(Duration.ofMillis(1000)))
                      .moveTo(PointOption.point(endX, endY))
                      .release()
                      .perform();
            LoggerUtil.logInfo("Swiped from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
        } catch (Exception e) {
            LoggerUtil.logError("Failed to swipe", e);
            throw e;
        }
    }
    
    /**
     * Swipe up (scroll down)
     */
    public void swipeUp() {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.8);
            int endX = size.width / 2;
            int endY = (int) (size.height * 0.2);
            
            swipe(startX, startY, endX, endY);
            LoggerUtil.logInfo("Swiped up");
        } catch (Exception e) {
            LoggerUtil.logError("Failed to swipe up", e);
            throw e;
        }
    }
    
    /**
     * Swipe down (scroll up)
     */
    public void swipeDown() {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.2);
            int endX = size.width / 2;
            int endY = (int) (size.height * 0.8);
            
            swipe(startX, startY, endX, endY);
            LoggerUtil.logInfo("Swiped down");
        } catch (Exception e) {
            LoggerUtil.logError("Failed to swipe down", e);
            throw e;
        }
    }
    
    /**
     * Swipe left
     */
    public void swipeLeft() {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = (int) (size.width * 0.8);
            int startY = size.height / 2;
            int endX = (int) (size.width * 0.2);
            int endY = size.height / 2;
            
            swipe(startX, startY, endX, endY);
            LoggerUtil.logInfo("Swiped left");
        } catch (Exception e) {
            LoggerUtil.logError("Failed to swipe left", e);
            throw e;
        }
    }
    
    /**
     * Swipe right
     */
    public void swipeRight() {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = (int) (size.width * 0.2);
            int startY = size.height / 2;
            int endX = (int) (size.width * 0.8);
            int endY = size.height / 2;
            
            swipe(startX, startY, endX, endY);
            LoggerUtil.logInfo("Swiped right");
        } catch (Exception e) {
            LoggerUtil.logError("Failed to swipe right", e);
            throw e;
        }
    }
    
    /**
     * Hide keyboard
     */
    public void hideKeyboard() {
        try {
            driver.hideKeyboard();
            LoggerUtil.logInfo("Keyboard hidden");
        } catch (Exception e) {
            LoggerUtil.logWarning("Failed to hide keyboard: " + e.getMessage());
        }
    }
    
    /**
     * Get screen dimensions
     */
    public Dimension getScreenSize() {
        return driver.manage().window().getSize();
    }
}