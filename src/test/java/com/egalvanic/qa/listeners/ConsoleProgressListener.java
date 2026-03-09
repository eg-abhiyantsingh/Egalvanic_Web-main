package com.egalvanic.qa.listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * ConsoleProgressListener - Clean real-time test progress for GitHub Actions
 *
 * Shows:
 * - Test start/pass/fail/skip status
 * - Progress percentage [completed/total - %]
 * - Duration in seconds
 * - Clean summary at end of each test block
 */
public class ConsoleProgressListener implements ITestListener {

    private int passed = 0;
    private int failed = 0;
    private int skipped = 0;
    private int total = 0;
    private long suiteStartTime;

    @Override
    public void onStart(ITestContext context) {
        passed = 0;
        failed = 0;
        skipped = 0;
        total = context.getAllTestMethods().length;
        suiteStartTime = System.currentTimeMillis();

        System.out.println();
        System.out.println("======================================================================");
        System.out.println("  STARTING TEST SUITE: " + context.getName());
        System.out.println("  Total Tests: " + total);
        System.out.println("======================================================================");
        System.out.println();
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getRealClass().getSimpleName();
        System.out.println(">> RUNNING: " + className + "." + testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        passed++;
        printProgress("PASSED", result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        failed++;
        printProgress("FAILED", result);
        Throwable error = result.getThrowable();
        if (error != null) {
            System.out.println("   Error: " + error.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        skipped++;
        printProgress("SKIPPED", result);
    }

    private void printProgress(String status, ITestResult result) {
        int completed = passed + failed + skipped;
        int percent = total > 0 ? Math.min((completed * 100) / total, 100) : 0;
        long duration = (result.getEndMillis() - result.getStartMillis()) / 1000;

        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getRealClass().getSimpleName();

        int barLength = 20;
        int filledLength = Math.min((percent * barLength) / 100, barLength);
        int emptyLength = Math.max(barLength - filledLength, 0);
        String progressBar = "#".repeat(filledLength) + "-".repeat(emptyLength);

        System.out.println(String.format("%s: %s.%s (%ds)",
            status, className, testName, duration));
        System.out.println(String.format("   Progress: [%s] %d/%d (%d%%)",
            progressBar, completed, total, percent));
        System.out.println();
    }

    @Override
    public void onFinish(ITestContext context) {
        long totalDuration = (System.currentTimeMillis() - suiteStartTime) / 1000;
        int minutes = (int) (totalDuration / 60);
        int seconds = (int) (totalDuration % 60);

        String statusText = failed == 0 ? "ALL TESTS PASSED!" : "SOME TESTS FAILED";

        System.out.println();
        System.out.println("======================================================================");
        System.out.println("  TEST RESULTS SUMMARY");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("  Status:  " + statusText);
        System.out.println("  Passed:  " + passed);
        System.out.println("  Failed:  " + failed);
        System.out.println("  Skipped: " + skipped);
        System.out.println("  Total:   " + total);
        System.out.println("  Duration: " + minutes + "m " + seconds + "s");
        System.out.println("======================================================================");
        System.out.println();
    }
}
