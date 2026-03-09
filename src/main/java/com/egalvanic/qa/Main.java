package com.egalvanic.qa;

import com.egalvanic.qa.testcase.AuthenticationTest;
import com.egalvanic.qa.utils.ConfigReader;

/**
 * Main entry point for the QA Automation Suite
 * Loads configuration from property files
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting QA Automation Suite...");
        System.out.println("Loading configuration from property file...");
        
        // Load configuration
        String baseUrl = ConfigReader.getBaseUrl();
        String email = ConfigReader.getUserEmail();
        String password = ConfigReader.getUserPassword();
        
        System.out.println("Configuration loaded:");
        System.out.println("- Base URL: " + baseUrl);
        System.out.println("- User Email: " + email);
        // Note: We don't print password for security reasons
        
        AuthenticationTest authTest = new AuthenticationTest();
        authTest.runAllTests();
        System.out.println("QA Automation Suite completed.");
    }
}