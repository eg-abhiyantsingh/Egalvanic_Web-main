package com.egalvanic.qa.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for reading configuration properties
 */
public class ConfigReader {
    private static Properties properties = new Properties();
    
    static {
        loadProperties();
    }
    
    /**
     * Load properties from config file
     */
    private static void loadProperties() {
        try {
            FileInputStream fis = new FileInputStream("src/main/resources/config.properties");
            properties.load(fis);
            fis.close();
        } catch (IOException e) {
            System.out.println("Could not load config.properties file. Using default values.");
            // Set default values
            properties.setProperty("base.url", "https://acme.qa.egalvanic.ai");
            properties.setProperty("user.email", "rahul+acme@egalvanic.com");
            properties.setProperty("user.password", "RP@egalvanic123");
            properties.setProperty("default.timeout", "25");
        }
    }
    
    /**
     * Get property value by key
     * @param key Property key
     * @return Property value
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Get property value by key with default value
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value or default value
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get base URL
     * @return Base URL
     */
    public static String getBaseUrl() {
        return properties.getProperty("base.url", "https://acme.qa.egalvanic.ai");
    }
    
    /**
     * Get user email
     * @return User email
     */
    public static String getUserEmail() {
        return properties.getProperty("user.email", "rahul+acme@egalvanic.com");
    }
    
    /**
     * Get user password
     * @return User password
     */
    public static String getUserPassword() {
        return properties.getProperty("user.password", "RP@egalvanic123");
    }
    
    /**
     * Get default timeout
     * @return Default timeout as integer
     */
    public static int getDefaultTimeout() {
        return Integer.parseInt(properties.getProperty("default.timeout", "25"));
    }
}