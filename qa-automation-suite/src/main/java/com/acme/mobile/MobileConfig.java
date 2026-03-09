package com.acme.mobile;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class MobileConfig {
    private static ThreadLocal<AppiumDriver> driverThreadLocal = new ThreadLocal<>();
    
    // Mobile platform constants
    public static final String ANDROID = "android";
    public static final String IOS = "ios";
    
    // Default capabilities
    private static final String DEFAULT_PLATFORM = "android";
    private static final String DEFAULT_DEVICE_NAME = "emulator";
    private static final String DEFAULT_APP_PACKAGE = "com.example.app";
    private static final String DEFAULT_APP_ACTIVITY = "MainActivity";
    private static final String DEFAULT_BUNDLE_ID = "com.example.app";
    private static final String APPIUM_SERVER_URL = "http://127.0.0.1:4723/wd/hub";
    
    /**
     * Initialize Appium driver based on platform
     */
    public static void initializeDriver() {
        AppiumDriver driver = driverThreadLocal.get();
        if (driver == null) {
            String platform = System.getProperty("mobile.platform", DEFAULT_PLATFORM).toLowerCase();
            
            try {
                switch (platform) {
                    case ANDROID:
                        driver = createAndroidDriver();
                        break;
                    case IOS:
                        driver = createIOSDriver();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mobile platform: " + platform);
                }
                driverThreadLocal.set(driver);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to initialize Appium driver", e);
            }
        }
    }
    
    /**
     * Create Android driver
     */
    private static AndroidDriver createAndroidDriver() throws MalformedURLException {
        DesiredCapabilities caps = new DesiredCapabilities();
        
        // Platform capabilities
        caps.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        caps.setCapability(MobileCapabilityType.DEVICE_NAME, 
            System.getProperty("mobile.device.name", DEFAULT_DEVICE_NAME));
        
        // App capabilities
        caps.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, 
            System.getProperty("mobile.app.package", DEFAULT_APP_PACKAGE));
        caps.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, 
            System.getProperty("mobile.app.activity", DEFAULT_APP_ACTIVITY));
        
        // Automation capabilities
        caps.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");
        caps.setCapability(MobileCapabilityType.NO_RESET, 
            Boolean.parseBoolean(System.getProperty("mobile.noReset", "false")));
        
        // Additional capabilities from system properties
        setAdditionalCapabilities(caps);
        
        return new AndroidDriver(new URL(APPIUM_SERVER_URL), caps);
    }
    
    /**
     * Create iOS driver
     */
    private static IOSDriver createIOSDriver() throws MalformedURLException {
        DesiredCapabilities caps = new DesiredCapabilities();
        
        // Platform capabilities
        caps.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
        caps.setCapability(MobileCapabilityType.DEVICE_NAME, 
            System.getProperty("mobile.device.name", DEFAULT_DEVICE_NAME));
        caps.setCapability(MobileCapabilityType.PLATFORM_VERSION, 
            System.getProperty("mobile.platform.version", ""));
        
        // App capabilities
        caps.setCapability(IOSMobileCapabilityType.BUNDLE_ID, 
            System.getProperty("mobile.bundle.id", DEFAULT_BUNDLE_ID));
        
        // Automation capabilities
        caps.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");
        caps.setCapability(MobileCapabilityType.NO_RESET, 
            Boolean.parseBoolean(System.getProperty("mobile.noReset", "false")));
        
        // Additional capabilities from system properties
        setAdditionalCapabilities(caps);
        
        return new IOSDriver(new URL(APPIUM_SERVER_URL), caps);
    }
    
    /**
     * Set additional capabilities from system properties
     */
    private static void setAdditionalCapabilities(DesiredCapabilities caps) {
        // Allow setting any additional capability through system properties
        Properties systemProps = System.getProperties();
        for (String key : systemProps.stringPropertyNames()) {
            if (key.startsWith("mobile.capability.")) {
                String capabilityName = key.substring("mobile.capability.".length());
                String capabilityValue = systemProps.getProperty(key);
                caps.setCapability(capabilityName, capabilityValue);
            }
        }
    }
    
    /**
     * Get Appium driver instance
     */
    public static AppiumDriver getDriver() {
        AppiumDriver driver = driverThreadLocal.get();
        if (driver == null) {
            initializeDriver();
            driver = driverThreadLocal.get();
        }
        return driver;
    }
    
    /**
     * Close Appium driver
     */
    public static void closeDriver() {
        AppiumDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }
}