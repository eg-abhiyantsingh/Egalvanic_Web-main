package com.egalvanic.qa.utils;

import com.egalvanic.qa.constants.AppConstants;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;

/**
 * Screenshot Utility - Capture screenshots for test reports
 */
public class ScreenshotUtil {

    static {
        // ImageIO defaults to disk-backed caches which leak file descriptors over
        // hundreds of compressions in one VM. Use memory-only caches instead and
        // explicitly release the writer after each use (already done in the JPEG path).
        ImageIO.setUseCache(false);
    }

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
     * Get screenshot as Base64 string (PNG, uncompressed).
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
     * Get screenshot as JPEG base64 with quality compression + optional downscale.
     * For a typical 1366x768 desktop screenshot:
     *   PNG raw: ~150 KB → base64 ~205 KB
     *   JPEG q60 raw: ~35 KB → base64 ~48 KB
     * Reduces report HTML size by ~4x with no visible loss on UI screenshots.
     *
     * @param quality JPEG quality 0.0-1.0 (recommend 0.6 for UI screenshots)
     * @param maxWidth optional downscale width in pixels; 0 = no resize
     * @return JPEG base64 string (without data: prefix) or null on failure
     */
    public static String getCompressedScreenshotAsBase64(float quality, int maxWidth) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] pngBytes = ts.getScreenshotAs(OutputType.BYTES);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (source == null) return null;

            BufferedImage target = source;
            if (maxWidth > 0 && source.getWidth() > maxWidth) {
                double scale = (double) maxWidth / source.getWidth();
                int newH = (int) (source.getHeight() * scale);
                target = new BufferedImage(maxWidth, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = target.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(source, 0, 0, maxWidth, newH, null);
                g.dispose();
            } else if (source.getType() != BufferedImage.TYPE_INT_RGB) {
                // JPEG can't carry an alpha channel — flatten to RGB
                target = new BufferedImage(source.getWidth(), source.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = target.createGraphics();
                g.drawImage(source, 0, 0, null);
                g.dispose();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) return null;
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                ImageWriteParam params = writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(quality);
                writer.write(null, new IIOImage(target, null, null), params);
            } finally {
                writer.dispose();
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            System.err.println("Failed to get compressed screenshot: " + e.getMessage());
            // Fall back to raw PNG so callers still get *something* in the report
            return getScreenshotAsBase64();
        }
    }

    /**
     * Convenience: JPEG quality 60%, max width 1280px — recommended default for reports.
     */
    public static String getCompressedScreenshotAsBase64() {
        return getCompressedScreenshotAsBase64(0.6f, 1280);
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
