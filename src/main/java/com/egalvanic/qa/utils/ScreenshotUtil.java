package com.egalvanic.qa.utils;

import com.egalvanic.qa.constants.AppConstants;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Screenshot Utility - Capture screenshots for test reports
 */
public class ScreenshotUtil {

    private static WebDriver driver;

    private ScreenshotUtil() {
        // Private constructor
    }

    /**
     * Set the driver instance for screenshot capture
     */
    public static void setDriver(WebDriver webDriver) {
        driver = webDriver;
    }

    /**
     * Capture screenshot and save to file
     */
    public static String captureScreenshot(String screenshotName) {
        try {
            File screenshotDir = new File(AppConstants.SCREENSHOT_PATH);
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = screenshotName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + timestamp + ".png";
            String filePath = AppConstants.SCREENSHOT_PATH + fileName;

            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] screenshotBytes = ts.getScreenshotAs(OutputType.BYTES);
            Files.write(Path.of(filePath), screenshotBytes);

            System.out.println("Screenshot captured: " + fileName);
            return new File(filePath).getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get screenshot as Base64 string
     */
    public static String getScreenshotAsBase64() {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            return ts.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            System.err.println("Failed to get Base64 screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cleanup old screenshots (older than specified days)
     */
    public static void cleanupOldScreenshots(int daysOld) {
        try {
            File screenshotDir = new File(AppConstants.SCREENSHOT_PATH);
            if (screenshotDir.exists() && screenshotDir.isDirectory()) {
                File[] files = screenshotDir.listFiles();
                if (files != null) {
                    long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
                    int deletedCount = 0;
                    for (File file : files) {
                        if (file.isFile() && file.lastModified() < cutoffTime) {
                            if (file.delete()) {
                                deletedCount++;
                            }
                        }
                    }
                    if (deletedCount > 0) {
                        System.out.println("Cleaned up " + deletedCount + " old screenshots");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup screenshots: " + e.getMessage());
        }
    }
}
