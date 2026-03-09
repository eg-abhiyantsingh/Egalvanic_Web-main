package com.acme.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtil {
    private static final Logger logger = LoggerFactory.getLogger(LoggerUtil.class);
    
    /**
     * Log info message
     * @param message Message to log
     */
    public static void logInfo(String message) {
        logger.info(message);
        ExtentReporterNG.logInfo(message);
    }
    
    /**
     * Log debug message
     * @param message Message to log
     */
    public static void logDebug(String message) {
        logger.debug(message);
    }
    
    /**
     * Log warning message
     * @param message Message to log
     */
    public static void logWarning(String message) {
        logger.warn(message);
        ExtentReporterNG.logWarning(message);
    }
    
    /**
     * Log error message
     * @param message Message to log
     */
    public static void logError(String message) {
        logger.error(message);
        ExtentReporterNG.logFail(message);
    }
    
    /**
     * Log error message with exception
     * @param message Message to log
     * @param throwable Exception to log
     */
    public static void logError(String message, Throwable throwable) {
        logger.error(message, throwable);
        ExtentReporterNG.logFail(message + ": " + throwable.getMessage());
    }
    
    /**
     * Log fatal message
     * @param message Message to log
     */
    public static void logFatal(String message) {
        logger.error("FATAL: " + message);
        ExtentReporterNG.logFail("FATAL: " + message);
    }
}