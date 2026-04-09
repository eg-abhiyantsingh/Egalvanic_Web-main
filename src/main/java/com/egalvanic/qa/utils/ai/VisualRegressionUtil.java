package com.egalvanic.qa.utils.ai;

import com.egalvanic.qa.utils.ExtentReportManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Visual Regression Utility — Pixel-Level Screenshot Comparison
 *
 * Manual testers catch UI bugs at a glance: misalignment, missing elements,
 * broken layouts, color changes. This utility brings that capability to automation.
 *
 * Features:
 *   - Pixel-level comparison with configurable threshold
 *   - Baseline management (save/load/compare)
 *   - Red-highlighted diff image generation
 *   - Claude vision fallback for intelligent analysis
 *   - ExtentReport integration with diff image embedding
 *
 * No external dependencies — uses Java's built-in BufferedImage + ImageIO.
 *
 * Usage:
 *   ComparisonResult result = VisualRegressionUtil.compareWithBaseline(driver, "dashboard");
 *   Assert.assertTrue(result.passed(), result.summary());
 */
public class VisualRegressionUtil {

    private VisualRegressionUtil() {}

    // ================================================================
    // CONFIGURATION
    // ================================================================

    private static final String BASELINE_DIR = System.getProperty(
            "visual.baselineDir", "test-output/visual-baselines");
    private static final String DIFF_DIR = System.getProperty(
            "visual.diffDir", "test-output/visual-diffs");
    private static final double DEFAULT_THRESHOLD = Double.parseDouble(
            System.getProperty("visual.threshold", "0.02")); // 2% pixel diff
    private static final boolean UPDATE_BASELINES = Boolean.parseBoolean(
            System.getProperty("visual.updateBaselines", "false"));
    private static final boolean AI_ANALYSIS_ENABLED = Boolean.parseBoolean(
            System.getProperty("visual.aiAnalysis", "true"));
    private static final int COLOR_TOLERANCE = 25; // per RGB channel

    // Track results for report
    private static final List<ComparisonResult> allResults = new ArrayList<>();

    // ================================================================
    // CORE API
    // ================================================================

    /**
     * Compare current page screenshot against stored baseline.
     */
    public static ComparisonResult compareWithBaseline(WebDriver driver, String pageName) {
        return compareWithBaseline(driver, pageName, DEFAULT_THRESHOLD);
    }

    /**
     * Compare with custom threshold.
     */
    public static ComparisonResult compareWithBaseline(WebDriver driver, String pageName, double threshold) {
        ComparisonResult result = new ComparisonResult();
        result.pageName = pageName;
        result.threshold = threshold;

        try {
            // Take current screenshot
            byte[] currentBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            BufferedImage current = ImageIO.read(new ByteArrayInputStream(currentBytes));

            // Save current for reference
            new File(DIFF_DIR).mkdirs();
            String currentPath = DIFF_DIR + "/" + pageName + "_current.png";
            ImageIO.write(current, "png", new File(currentPath));
            result.currentPath = currentPath;

            // Load baseline
            String baselinePath = BASELINE_DIR + "/" + pageName + ".png";
            result.baselinePath = baselinePath;
            File baselineFile = new File(baselinePath);

            if (!baselineFile.exists()) {
                if (UPDATE_BASELINES) {
                    saveBaseline(current, pageName);
                    result.status = Status.BASELINE_CREATED;
                    System.out.println("[Visual] Baseline created: " + baselinePath);
                } else {
                    result.status = Status.NO_BASELINE;
                    System.out.println("[Visual] No baseline for '" + pageName + "' — run with -Dvisual.updateBaselines=true");
                }
                allResults.add(result);
                return result;
            }

            BufferedImage baseline = ImageIO.read(baselineFile);

            // Compare
            result.diffPercentage = calculateDiffPercentage(baseline, current);

            if (result.diffPercentage <= threshold) {
                result.status = Status.MATCH;
                System.out.printf("[Visual] MATCH: %s (%.2f%% diff, threshold %.2f%%)%n",
                        pageName, result.diffPercentage * 100, threshold * 100);
            } else {
                result.status = Status.DIFF_FOUND;

                // Generate diff image
                BufferedImage diffImage = generateDiffImage(baseline, current);
                String diffPath = DIFF_DIR + "/" + pageName + "_diff.png";
                ImageIO.write(diffImage, "png", new File(diffPath));
                result.diffImagePath = diffPath;

                System.out.printf("[Visual] DIFF: %s (%.2f%% diff, threshold %.2f%%)%n",
                        pageName, result.diffPercentage * 100, threshold * 100);

                // AI analysis if enabled
                if (AI_ANALYSIS_ENABLED && ClaudeClient.isConfigured()) {
                    try {
                        BufferedImage sideBySide = createSideBySide(baseline, current);
                        String compositeBase64 = imageToBase64(sideBySide);
                        result.aiAnalysis = analyzeWithAI(compositeBase64, pageName, result.diffPercentage);
                    } catch (Exception e) {
                        System.out.println("[Visual] AI analysis failed: " + e.getMessage());
                    }
                }
            }

            // Update baseline if requested
            if (UPDATE_BASELINES && result.status == Status.DIFF_FOUND) {
                saveBaseline(current, pageName);
                System.out.println("[Visual] Baseline updated: " + baselinePath);
            }

        } catch (Exception e) {
            result.status = Status.ERROR;
            result.errorMessage = e.getMessage();
            System.out.println("[Visual] Error comparing " + pageName + ": " + e.getMessage());
        }

        allResults.add(result);
        return result;
    }

    /**
     * Compare a specific element against its baseline.
     */
    public static ComparisonResult compareElement(WebDriver driver, WebElement element, String elementName) {
        ComparisonResult result = new ComparisonResult();
        result.pageName = elementName;
        result.threshold = DEFAULT_THRESHOLD;

        try {
            byte[] elementBytes = element.getScreenshotAs(OutputType.BYTES);
            BufferedImage current = ImageIO.read(new ByteArrayInputStream(elementBytes));

            String baselinePath = BASELINE_DIR + "/elements/" + elementName + ".png";
            File baselineFile = new File(baselinePath);

            if (!baselineFile.exists()) {
                if (UPDATE_BASELINES) {
                    new File(baselineFile.getParent()).mkdirs();
                    ImageIO.write(current, "png", baselineFile);
                    result.status = Status.BASELINE_CREATED;
                } else {
                    result.status = Status.NO_BASELINE;
                }
                allResults.add(result);
                return result;
            }

            BufferedImage baseline = ImageIO.read(baselineFile);
            result.diffPercentage = calculateDiffPercentage(baseline, current);
            result.status = result.diffPercentage <= DEFAULT_THRESHOLD ? Status.MATCH : Status.DIFF_FOUND;

            if (result.status == Status.DIFF_FOUND) {
                new File(DIFF_DIR + "/elements").mkdirs();
                BufferedImage diff = generateDiffImage(baseline, current);
                result.diffImagePath = DIFF_DIR + "/elements/" + elementName + "_diff.png";
                ImageIO.write(diff, "png", new File(result.diffImagePath));
            }

        } catch (Exception e) {
            result.status = Status.ERROR;
            result.errorMessage = e.getMessage();
        }

        allResults.add(result);
        return result;
    }

    /**
     * Force-save current screenshot as baseline.
     */
    public static void saveBaseline(WebDriver driver, String pageName) {
        try {
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            saveBaseline(image, pageName);
        } catch (IOException e) {
            System.out.println("[Visual] Failed to save baseline: " + e.getMessage());
        }
    }

    private static void saveBaseline(BufferedImage image, String pageName) throws IOException {
        new File(BASELINE_DIR).mkdirs();
        ImageIO.write(image, "png", new File(BASELINE_DIR + "/" + pageName + ".png"));
    }

    // ================================================================
    // PIXEL COMPARISON ENGINE
    // ================================================================

    /**
     * Calculate the percentage of pixels that differ between two images.
     * Returns 1.0 if dimensions differ (complete layout shift).
     */
    static double calculateDiffPercentage(BufferedImage baseline, BufferedImage current) {
        int bw = baseline.getWidth(), bh = baseline.getHeight();
        int cw = current.getWidth(), ch = current.getHeight();

        // If dimensions differ significantly, it's a layout shift
        if (Math.abs(bw - cw) > 10 || Math.abs(bh - ch) > 10) {
            return 1.0;
        }

        int width = Math.min(bw, cw);
        int height = Math.min(bh, ch);
        int totalPixels = width * height;
        int diffPixels = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int baseRGB = baseline.getRGB(x, y);
                int currRGB = current.getRGB(x, y);

                int br = (baseRGB >> 16) & 0xFF, bg = (baseRGB >> 8) & 0xFF, bb = baseRGB & 0xFF;
                int cr = (currRGB >> 16) & 0xFF, cg = (currRGB >> 8) & 0xFF, cb = currRGB & 0xFF;

                if (Math.abs(br - cr) > COLOR_TOLERANCE ||
                    Math.abs(bg - cg) > COLOR_TOLERANCE ||
                    Math.abs(bb - cb) > COLOR_TOLERANCE) {
                    diffPixels++;
                }
            }
        }

        return (double) diffPixels / totalPixels;
    }

    /**
     * Generate a diff image highlighting changed pixels in red.
     */
    static BufferedImage generateDiffImage(BufferedImage baseline, BufferedImage current) {
        int width = Math.max(baseline.getWidth(), current.getWidth());
        int height = Math.max(baseline.getHeight(), current.getHeight());

        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Out of bounds for either image = red
                if (x >= baseline.getWidth() || y >= baseline.getHeight() ||
                    x >= current.getWidth() || y >= current.getHeight()) {
                    diff.setRGB(x, y, Color.RED.getRGB());
                    continue;
                }

                int baseRGB = baseline.getRGB(x, y);
                int currRGB = current.getRGB(x, y);

                int br = (baseRGB >> 16) & 0xFF, bg = (baseRGB >> 8) & 0xFF, bb = baseRGB & 0xFF;
                int cr = (currRGB >> 16) & 0xFF, cg = (currRGB >> 8) & 0xFF, cb = currRGB & 0xFF;

                boolean isDiff = Math.abs(br - cr) > COLOR_TOLERANCE ||
                                 Math.abs(bg - cg) > COLOR_TOLERANCE ||
                                 Math.abs(bb - cb) > COLOR_TOLERANCE;

                if (isDiff) {
                    diff.setRGB(x, y, Color.RED.getRGB());
                } else {
                    // Ghost the original at 40% opacity on gray background
                    int gray = (br + bg + bb) / 3;
                    int ghosted = (int) (gray * 0.4 + 150 * 0.6);
                    diff.setRGB(x, y, new Color(ghosted, ghosted, ghosted).getRGB());
                }
            }
        }

        return diff;
    }

    /**
     * Stitch two images side by side for Claude vision comparison.
     */
    static BufferedImage createSideBySide(BufferedImage left, BufferedImage right) {
        int gap = 4;
        int width = left.getWidth() + gap + right.getWidth();
        int height = Math.max(left.getHeight(), right.getHeight());

        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = composite.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height); // Red background for gap
        g.drawImage(left, 0, 0, null);
        g.drawImage(right, left.getWidth() + gap, 0, null);
        g.dispose();

        return composite;
    }

    // ================================================================
    // AI VISION ANALYSIS
    // ================================================================

    private static String analyzeWithAI(String compositeBase64, String pageName, double diffPercent) {
        String prompt = String.format(
            "Compare these two screenshots of the '%s' page (left = baseline, right = current). " +
            "There is a %.1f%% pixel difference. " +
            "Classify the change as one of: LAYOUT_SHIFT, CONTENT_CHANGE, STYLE_CHANGE, DATA_CHANGE, REAL_BUG. " +
            "Describe what changed. Is this a bug or expected variation? " +
            "Respond concisely in 2-3 sentences.",
            pageName, diffPercent * 100);

        return ClaudeClient.askWithImage(
            "You are a visual QA tester analyzing UI changes between a baseline and current screenshot.",
            prompt,
            compositeBase64);
    }

    // ================================================================
    // REPORTING
    // ================================================================

    /**
     * Log comparison result to ExtentReport.
     */
    public static void logToExtentReport(ComparisonResult result) {
        if (result.status == Status.MATCH) {
            ExtentReportManager.logInfo("Visual baseline match: " + result.pageName +
                    String.format(" (%.2f%% diff)", result.diffPercentage * 100));
        } else if (result.status == Status.DIFF_FOUND) {
            String msg = String.format("Visual diff detected: %s (%.2f%% diff, threshold %.2f%%)",
                    result.pageName, result.diffPercentage * 100, result.threshold * 100);
            if (result.aiAnalysis != null) {
                msg += " | AI: " + result.aiAnalysis;
            }
            ExtentReportManager.logWarning(msg);
        } else if (result.status == Status.BASELINE_CREATED) {
            ExtentReportManager.logInfo("Visual baseline created: " + result.pageName);
        } else if (result.status == Status.NO_BASELINE) {
            ExtentReportManager.logInfo("No visual baseline for: " + result.pageName);
        }
    }

    /**
     * Get all comparison results from this session.
     */
    public static List<ComparisonResult> getAllResults() {
        return new ArrayList<>(allResults);
    }

    /**
     * Print summary of all visual comparisons.
     */
    public static void printSummary() {
        if (allResults.isEmpty()) return;

        int matches = 0, diffs = 0, created = 0, noBl = 0, errors = 0;
        for (ComparisonResult r : allResults) {
            switch (r.status) {
                case MATCH: matches++; break;
                case DIFF_FOUND: diffs++; break;
                case BASELINE_CREATED: created++; break;
                case NO_BASELINE: noBl++; break;
                case ERROR: errors++; break;
            }
        }

        System.out.println("\n=== Visual Regression Summary ===");
        System.out.printf("  Matches: %d | Diffs: %d | Baselines created: %d | No baseline: %d | Errors: %d%n",
                matches, diffs, created, noBl, errors);
        if (diffs > 0) {
            System.out.println("  Diff images saved to: " + DIFF_DIR);
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private static String imageToBase64(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // ================================================================
    // DATA CLASSES
    // ================================================================

    public enum Status {
        MATCH, DIFF_FOUND, NO_BASELINE, BASELINE_CREATED, ERROR
    }

    public static class ComparisonResult {
        public String pageName;
        public Status status;
        public double diffPercentage;
        public double threshold;
        public String baselinePath;
        public String currentPath;
        public String diffImagePath;
        public String aiAnalysis;
        public String errorMessage;

        public boolean passed() {
            return status == Status.MATCH || status == Status.NO_BASELINE || status == Status.BASELINE_CREATED;
        }

        public String summary() {
            switch (status) {
                case MATCH:
                    return String.format("%s: MATCH (%.2f%% diff)", pageName, diffPercentage * 100);
                case DIFF_FOUND:
                    return String.format("%s: DIFF FOUND (%.2f%% > %.2f%% threshold)%s",
                            pageName, diffPercentage * 100, threshold * 100,
                            aiAnalysis != null ? " | AI: " + aiAnalysis : "");
                case NO_BASELINE:
                    return pageName + ": No baseline — run with -Dvisual.updateBaselines=true";
                case BASELINE_CREATED:
                    return pageName + ": Baseline created";
                case ERROR:
                    return pageName + ": ERROR — " + errorMessage;
                default:
                    return pageName + ": " + status;
            }
        }
    }
}
