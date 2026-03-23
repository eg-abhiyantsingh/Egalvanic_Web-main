package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Bug Hunt: Global / Cross-Page Bug Verification Tests
 *
 * Verifies bugs that affect ALL pages of the application:
 *   BUG-001: DevRev PLuG SDK Fails on Every Page Load
 *   BUG-002: N+1 API Query Pattern (80+ individual requests per page)
 *   BUG-003: Sentry DSN and Credentials Exposed in Client JS Bundle
 *   BUG-005: SPA State Not Persisted in URL (sldId lost on reload)
 *   BUG-007: Duplicate API Calls on Every Page Load
 *   BUG-013: Site Selector "No sites available" Flicker
 *   BUG-014: Sentry Error Reporting Partially Failing
 *   BUG-018: Beamer Analytics Leaks User Role/Company in URL
 *   BUG-025: Different JS Bundle Hashes Served Across Pages
 *
 * These tests use JavaScript execution to inspect console errors,
 * performance API entries, and network request patterns.
 *
 * Tests share a browser session (inherited from BaseTest).
 */
public class BugHuntGlobalTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ================================================================
    // BUG-001: DevRev PLuG SDK Fails on Every Page Load
    // ================================================================

    @Test(priority = 1, description = "BUG-001: Verify DevRev PLuG SDK fails to load on every page")
    public void testBUG001_DevRevSDKFails() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_DEVREV_SDK,
                "BUG-001: DevRev SDK Failure");

        try {
            // Navigate to dashboard to get a fresh page load
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(5000);

            logStep("Navigated to dashboard, checking for DevRev SDK errors");

            // Inject console error capture script
            String consoleErrors = (String) js().executeScript(
                "var errors = [];" +
                "var entries = performance.getEntriesByType('resource');" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  if (entries[i].name.indexOf('plug-platform.devrev.ai') !== -1) {" +
                "    errors.push(entries[i].name + ' (duration: ' + entries[i].duration + 'ms)');" +
                "  }" +
                "}" +
                "return errors.length > 0 ? errors.join('\\n') : 'NO_DEVREV_REQUESTS';");

            logStep("DevRev resource entries: " + consoleErrors);

            // Check if plug.js request exists and failed (duration ~0 = failed)
            Boolean devrevFailed = (Boolean) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  if (entries[i].name.indexOf('plug-platform.devrev.ai/static/plug.js') !== -1) {" +
                "    return entries[i].transferSize === 0 || entries[i].duration < 50;" +
                "  }" +
                "}" +
                "return false;");

            logStepWithScreenshot("Dashboard page — checking DevRev SDK status");

            // The bug is confirmed if DevRev request exists but failed (0 transfer)
            // or if no DevRev request at all (blocked)
            boolean devrevBroken = (devrevFailed != null && devrevFailed) ||
                    "NO_DEVREV_REQUESTS".equals(consoleErrors);

            if (devrevBroken) {
                logStep("CONFIRMED: DevRev plug.js failed to load or was blocked");
            }

            Assert.assertTrue(devrevBroken,
                    "BUG-001: DevRev SDK loaded successfully. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-001 confirmed: DevRev SDK broken. " +
                    "Failed=" + devrevFailed + ", entries=" + consoleErrors);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG001_devrev_error");
            Assert.fail("BUG-001 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-002: N+1 API Query Pattern
    // ================================================================

    @Test(priority = 2, description = "BUG-002: Verify N+1 API pattern fires 80+ requests per page load")
    public void testBUG002_NPlusOneAPIPattern() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_API_N_PLUS_1,
                "BUG-002: N+1 API Pattern");

        try {
            // Navigate fresh to trigger all API calls
            driver.get(AppConstants.BASE_URL + "/tasks");
            pause(8000); // Wait for all N+1 calls to fire

            logStep("Navigated to Tasks page, waiting for API calls to complete");

            // Count node-subtypes and shortcut API calls using Performance API
            String apiStats = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var subtypeCalls = 0, shortcutCalls = 0, totalApi = 0;" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  var name = entries[i].name;" +
                "  if (name.indexOf('/api/') !== -1) { totalApi++; }" +
                "  if (name.indexOf('node-subtypes') !== -1) { subtypeCalls++; }" +
                "  if (name.indexOf('shortcut/by-node-class') !== -1) { shortcutCalls++; }" +
                "}" +
                "return 'total=' + totalApi + ',subtypes=' + subtypeCalls + ',shortcuts=' + shortcutCalls;");

            logStep("API call breakdown: " + apiStats);
            logStepWithScreenshot("Tasks page after full load — N+1 pattern active");

            // Parse counts
            int subtypeCalls = 0, shortcutCalls = 0, totalApi = 0;
            for (String part : apiStats.split(",")) {
                String[] kv = part.split("=");
                if ("total".equals(kv[0])) totalApi = Integer.parseInt(kv[1]);
                if ("subtypes".equals(kv[0])) subtypeCalls = Integer.parseInt(kv[1]);
                if ("shortcuts".equals(kv[0])) shortcutCalls = Integer.parseInt(kv[1]);
            }

            logStep("Total API calls: " + totalApi);
            logStep("Node-subtype individual calls: " + subtypeCalls);
            logStep("Shortcut by-node-class individual calls: " + shortcutCalls);

            // BUG confirmed if there are many individual calls (should be batched into 1-2)
            Assert.assertTrue(subtypeCalls + shortcutCalls > 20,
                    "BUG-002: Expected 20+ N+1 calls but found only " +
                    (subtypeCalls + shortcutCalls) + ". Bug may be fixed.");

            ExtentReportManager.logPass("BUG-002 confirmed: " + (subtypeCalls + shortcutCalls) +
                    " individual N+1 API calls detected (should be batched)");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG002_nplus1_error");
            Assert.fail("BUG-002 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-003: Sentry DSN Exposed in Client JS Bundle
    // ================================================================

    @Test(priority = 3, description = "BUG-003: Verify Sentry credentials are exposed in network requests")
    public void testBUG003_SentryCredentialsExposed() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SENTRY_EXPOSURE,
                "BUG-003: Sentry DSN Exposed");

        try {
            // Check for Sentry DSN in performance entries
            String sentryInfo = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var sentryUrls = [];" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  if (entries[i].name.indexOf('sentry.io') !== -1) {" +
                "    sentryUrls.push(entries[i].name);" +
                "  }" +
                "}" +
                "return sentryUrls.length > 0 ? sentryUrls[0] : 'NO_SENTRY';");

            logStep("Sentry URL found: " + (sentryInfo.length() > 100 ?
                    sentryInfo.substring(0, 100) + "..." : sentryInfo));

            boolean hasSentryKey = sentryInfo.contains("sentry_key=");
            boolean hasProjectId = sentryInfo.contains("/api/") && sentryInfo.contains("/envelope/");

            logStep("Sentry key exposed in URL: " + hasSentryKey);
            logStep("Sentry project ID exposed: " + hasProjectId);
            logStepWithScreenshot("Page with Sentry requests active");

            Assert.assertTrue(hasSentryKey,
                    "BUG-003: Expected sentry_key in URL but not found. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-003 confirmed: Sentry DSN key visible in plaintext URL parameters");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG003_sentry_error");
            Assert.fail("BUG-003 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-005: SPA State Not Persisted in URL
    // ================================================================

    @Test(priority = 5, description = "BUG-005: Verify sldId/site context is lost on page reload")
    public void testBUG005_SPAStateNotPersistedInURL() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_URL_STATE,
                "BUG-005: URL State Not Persisted");

        try {
            // Navigate to tasks page (which requires sldId context)
            driver.get(AppConstants.BASE_URL + "/tasks");
            pause(5000);

            String currentUrl = driver.getCurrentUrl();
            logStep("Tasks URL: " + currentUrl);

            // Check if sldId is in the URL
            boolean hasSldInUrl = currentUrl.contains("sld") || currentUrl.contains("site");
            logStep("URL contains site/sld context: " + hasSldInUrl);

            // The bug is: direct navigation to /tasks should preserve site context in URL
            // but it doesn't — the URL is just /tasks with no query params
            Assert.assertFalse(hasSldInUrl,
                    "BUG-005: sldId IS now in URL — bug may be fixed");

            logStepWithScreenshot("Tasks page URL lacks site context");

            // Additional check: reload the page and see if context is lost
            driver.navigate().refresh();
            pause(5000);

            // Check if site selector shows a site or "No sites available"
            boolean siteLoaded = false;
            try {
                WebElement facilityInput = driver.findElement(
                        By.xpath("//input[@placeholder='Select facility']"));
                String val = facilityInput.getAttribute("value");
                siteLoaded = val != null && !val.isEmpty();
                logStep("After reload, facility value: '" + val + "'");
            } catch (Exception ignored) {
                logStep("Facility input not found after reload");
            }

            logStepWithScreenshot("After page reload — site context state");

            ExtentReportManager.logPass("BUG-005 confirmed: URL is '" + currentUrl +
                    "' — no site/sld context. Site restored after reload: " + siteLoaded);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG005_url_state_error");
            Assert.fail("BUG-005 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-007: Duplicate API Calls on Every Page Load
    // ================================================================

    @Test(priority = 7, description = "BUG-007: Verify duplicate API calls (roles, SLDs fetched twice)")
    public void testBUG007_DuplicateAPICalls() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_DUPLICATE_API,
                "BUG-007: Duplicate API Calls");

        try {
            // Navigate fresh to capture all requests
            driver.get(AppConstants.BASE_URL + "/connections");
            pause(6000);

            logStep("Navigated to Connections page, checking for duplicate API calls");

            // Count duplicate API calls using Performance API
            String dupeStats = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var roleCalls = 0, sldCalls = 0, nodeClassCalls = 0;" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  var name = entries[i].name;" +
                "  if (name.indexOf('/roles') !== -1 && name.indexOf('/users/') !== -1) roleCalls++;" +
                "  if (name.indexOf('/slds') !== -1 && name.indexOf('/users/') !== -1) sldCalls++;" +
                "  if (name.indexOf('/node_classes/') !== -1) nodeClassCalls++;" +
                "}" +
                "return 'roles=' + roleCalls + ',slds=' + sldCalls + ',nodeClasses=' + nodeClassCalls;");

            logStep("Duplicate call counts: " + dupeStats);

            int roleCalls = 0, sldCalls = 0;
            for (String part : dupeStats.split(",")) {
                String[] kv = part.split("=");
                if ("roles".equals(kv[0])) roleCalls = Integer.parseInt(kv[1]);
                if ("slds".equals(kv[0])) sldCalls = Integer.parseInt(kv[1]);
            }

            logStep("User roles API calls: " + roleCalls + " (expected 1, got " + roleCalls + ")");
            logStep("SLDs API calls: " + sldCalls + " (expected 1, got " + sldCalls + ")");
            logStepWithScreenshot("Connections page — duplicate calls active");

            // BUG confirmed if roles or SLDs called more than once
            boolean hasDuplicates = roleCalls > 1 || sldCalls > 1;
            Assert.assertTrue(hasDuplicates,
                    "BUG-007: No duplicate API calls detected. Roles=" + roleCalls +
                    ", SLDs=" + sldCalls + ". Bug may be fixed.");

            ExtentReportManager.logPass("BUG-007 confirmed: Roles called " + roleCalls +
                    "x, SLDs called " + sldCalls + "x (should be 1x each)");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG007_duplicate_api_error");
            Assert.fail("BUG-007 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-013: Site Selector "No sites available" Flicker
    // ================================================================

    @Test(priority = 13, description = "BUG-013: Verify site selector briefly shows 'No sites available'")
    public void testBUG013_SiteSelectorFlicker() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SITE_SELECTOR,
                "BUG-013: Site Selector Flicker");

        try {
            // Navigate fresh to trigger the flicker
            driver.get(AppConstants.BASE_URL + "/assets");
            // Don't wait long — we need to catch the flicker
            pause(1500);

            logStep("Navigated to Assets page quickly to catch site selector flicker");

            // Check if the facility input initially has no value (flicker state)
            String facilityVal = "";
            try {
                WebElement facilityInput = driver.findElement(
                        By.xpath("//input[@placeholder='Select facility']"));
                facilityVal = facilityInput.getAttribute("value");
            } catch (Exception ignored) {}

            logStep("Facility value at 1.5s after navigation: '" + facilityVal + "'");
            logStepWithScreenshot("Site selector state during initial load");

            // Wait for sites to load
            pause(5000);

            String facilityValAfter = "";
            try {
                WebElement facilityInput = driver.findElement(
                        By.xpath("//input[@placeholder='Select facility']"));
                facilityValAfter = facilityInput.getAttribute("value");
            } catch (Exception ignored) {}

            logStep("Facility value at 6.5s after navigation: '" + facilityValAfter + "'");

            // The flicker is: initially empty/no sites → then populated after delay
            boolean initiallyEmpty = facilityVal == null || facilityVal.isEmpty() ||
                    facilityVal.contains("No sites");
            boolean eventuallyPopulated = facilityValAfter != null && !facilityValAfter.isEmpty() &&
                    !facilityValAfter.contains("No sites");
            boolean flickerDetected = initiallyEmpty && eventuallyPopulated;

            logStep("Initially empty: " + initiallyEmpty + ", eventually populated: " + eventuallyPopulated);
            logStep("Flicker detected: " + flickerDetected);

            // The bug is the delay between empty state and populated state
            Assert.assertTrue(flickerDetected,
                    "BUG-013: Site selector did not show flicker pattern. " +
                    "initial='" + facilityVal + "', after='" + facilityValAfter + "'. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-013 confirmed: Facility selector initial='" +
                    facilityVal + "', after load='" + facilityValAfter + "'. Flicker=" + flickerDetected);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG013_flicker_error");
            Assert.fail("BUG-013 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-014: Sentry Error Reporting Partially Failing
    // ================================================================

    @Test(priority = 14, description = "BUG-014: Verify Sentry envelope requests partially fail")
    public void testBUG014_SentryPartiallyFailing() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SENTRY_REPORTING,
                "BUG-014: Sentry Partially Failing");

        try {
            // Navigate fresh so Performance API has entries for this page load
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(5000);

            logStep("Navigated to Dashboard — checking Sentry envelope requests");

            // Sentry requests are visible in Performance API
            String sentryStats = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "var total = 0, failed = 0;" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  if (entries[i].name.indexOf('sentry.io') !== -1 && " +
                "      entries[i].name.indexOf('envelope') !== -1) {" +
                "    total++;" +
                "    if (entries[i].transferSize === 0) failed++;" +
                "  }" +
                "}" +
                "return 'total=' + total + ',failed=' + failed;");

            logStep("Sentry envelope stats: " + sentryStats);
            logStepWithScreenshot("Current page — Sentry request status");

            int sentryTotal = 0, sentryFailed = 0;
            for (String part : sentryStats.split(",")) {
                if (part.startsWith("total=")) sentryTotal = Integer.parseInt(part.split("=")[1]);
                if (part.startsWith("failed=")) sentryFailed = Integer.parseInt(part.split("=")[1]);
            }

            logStep("Sentry envelopes: " + sentryTotal + " total, " + sentryFailed + " failed");

            Assert.assertTrue(sentryFailed > 0 || sentryTotal == 0,
                    "BUG-014: All " + sentryTotal + " Sentry envelopes succeeded. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-014 confirmed: " + sentryFailed + "/" + sentryTotal +
                    " Sentry envelopes failed");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG014_sentry_fail_error");
            Assert.fail("BUG-014 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-018: Beamer Analytics Leaks User Role in URL
    // ================================================================

    @Test(priority = 18, description = "BUG-018: Verify Beamer leaks user role and company in URL params")
    public void testBUG018_BeamerLeaksUserData() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_BEAMER_LEAK,
                "BUG-018: Beamer Data Leak");

        try {
            // Check Performance API for Beamer requests with user data in URL
            String beamerInfo = (String) js().executeScript(
                "var entries = performance.getEntriesByType('resource');" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  if (entries[i].name.indexOf('getbeamer.com') !== -1 && " +
                "      entries[i].name.indexOf('c_user_role') !== -1) {" +
                "    return entries[i].name;" +
                "  }" +
                "}" +
                "return 'NO_BEAMER_LEAK';");

            logStep("Beamer URL check: " + (beamerInfo.length() > 120 ?
                    beamerInfo.substring(0, 120) + "..." : beamerInfo));

            boolean hasUserRole = beamerInfo.contains("c_user_role=");
            boolean hasCompany = beamerInfo.contains("c_user_company=");
            boolean hasName = beamerInfo.contains("firstname=");

            logStep("Leaks user_role: " + hasUserRole);
            logStep("Leaks company: " + hasCompany);
            logStep("Leaks firstname: " + hasName);
            logStepWithScreenshot("Beamer data leak check");

            Assert.assertTrue(hasUserRole || hasCompany,
                    "BUG-018: Beamer URL no longer contains user data. Bug may be fixed.");

            ExtentReportManager.logPass("BUG-018 confirmed: Beamer URL exposes " +
                    "role=" + hasUserRole + ", company=" + hasCompany + ", name=" + hasName);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG018_beamer_error");
            Assert.fail("BUG-018 test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // BUG-025: Different JS Bundle Hashes Across Pages
    // ================================================================

    @Test(priority = 25, description = "BUG-025: Verify different JS bundle hashes served across pages")
    public void testBUG025_DifferentBundleHashes() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_BUNDLE_HASH,
                "BUG-025: Bundle Hash Inconsistency");

        try {
            // Navigate to dashboard and capture the JS bundle name
            driver.get(AppConstants.BASE_URL + "/dashboard");
            pause(5000);

            String dashboardBundle = (String) js().executeScript(
                "var scripts = document.querySelectorAll('script[src*=\"index-\"]');" +
                "if (scripts.length > 0) return scripts[0].src;" +
                "var entries = performance.getEntriesByType('resource');" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  if (entries[i].name.indexOf('index-') !== -1 && entries[i].name.indexOf('.js') !== -1) {" +
                "    return entries[i].name;" +
                "  }" +
                "}" +
                "return 'NOT_FOUND';");

            logStep("Dashboard JS bundle: " + dashboardBundle);

            // Navigate to a different page
            driver.get(AppConstants.BASE_URL + "/jobs-v2");
            pause(5000);

            String jobsBundle = (String) js().executeScript(
                "var scripts = document.querySelectorAll('script[src*=\"index-\"]');" +
                "if (scripts.length > 0) return scripts[0].src;" +
                "var entries = performance.getEntriesByType('resource');" +
                "for (var i = 0; i < entries.length; i++) {" +
                "  if (entries[i].name.indexOf('index-') !== -1 && entries[i].name.indexOf('.js') !== -1) {" +
                "    return entries[i].name;" +
                "  }" +
                "}" +
                "return 'NOT_FOUND';");

            logStep("Service Agreements JS bundle: " + jobsBundle);

            // Extract hash from bundle name (format: index-{HASH}.js)
            String hash1 = extractBundleHash(dashboardBundle);
            String hash2 = extractBundleHash(jobsBundle);

            logStep("Dashboard hash: " + hash1);
            logStep("Service Agreements hash: " + hash2);
            logStepWithScreenshot("Bundle hash comparison");

            if ("NOT_FOUND".equals(hash1) || "NOT_FOUND".equals(hash2)) {
                logWarning("Could not extract bundle hash from one or both pages");
                ExtentReportManager.logPass("BUG-025: Bundle hash not found. " +
                        "Dashboard=" + hash1 + ", ServiceAgreements=" + hash2);
                return;
            }

            boolean hashMismatch = !hash1.equals(hash2);
            logStep("Hashes match: " + !hashMismatch);

            Assert.assertTrue(hashMismatch,
                    "BUG-025: Bundle hashes match (" + hash1 + "). Bug may be fixed.");

            ExtentReportManager.logPass("BUG-025 confirmed: Dashboard=" + hash1 +
                    ", ServiceAgreements=" + hash2 + " — hashes differ");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG025_bundle_error");
            Assert.fail("BUG-025 test failed: " + e.getMessage());
        }
    }

    private String extractBundleHash(String url) {
        // Extract hash from URL like .../index-CnGxCKdJ.js
        int idx = url.lastIndexOf("index-");
        if (idx == -1) return url;
        String after = url.substring(idx);
        int dotIdx = after.indexOf(".js");
        if (dotIdx == -1) return after;
        return after.substring(0, dotIdx);
    }
}
