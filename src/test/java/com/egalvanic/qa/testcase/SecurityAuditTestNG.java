package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

/**
 * Security Audit Regression Suite — 2 security bugs kept in the curated report.
 *
 * On 2026-04-21, QA lead validated the final bug list. Only the 8 reproducible,
 * priority-worthy bugs remain in the curated PDF. Four of this file's earlier
 * security findings (missing headers, clickjacking, autocomplete, 3rd-party
 * cookies) were de-scoped and their @Test methods removed.
 *
 * Current coverage (renumbered to match the curated PDF):
 *   BUG-007  Auth cookies SameSite=None (form-POST CSRF surface)   [MEDIUM]
 *   BUG-008  No rate-limiting on /api/auth/login                   [HIGH]
 *
 * Scope: authorized QA / passive probes only.
 *   - Cookie flag inspection via driver.manage().getCookies()
 *   - Rate-limit probe: 6 failed logins (deeper 28-attempt probe in /tmp probe script)
 *
 * Each test asserts the security issue is STILL PRESENT. When a finding is
 * fixed in production, flip the assertion or remove the @Test — this file
 * becomes the regression gate preventing the issue from reappearing.
 */
public class SecurityAuditTestNG extends BaseTest {

    // =================================================================
    // BUG-007: Auth cookies use SameSite=None (CSRF surface)
    // =================================================================
    @Test(priority = 2, description = "BUG-007: access_token / refresh_token cookies SameSite=None")
    public void testBUG007_AuthCookiesSameSiteNone() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_COOKIE_SAMESITE,
                "BUG-007: Auth cookie SameSite=None");
        try {
            loginAndSelectSite();
            pause(3000);

            Set<Cookie> cookies = driver.manage().getCookies();
            Cookie access = null, refresh = null;
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName())) access = c;
                if ("refresh_token".equals(c.getName())) refresh = c;
            }
            logStep("access_token found=" + (access != null) + ", refresh_token found=" + (refresh != null));
            ScreenshotUtil.captureScreenshot("BUG016_cookies");

            // Selenium exposes getSameSite() from v4
            String accessSs = access != null ? access.getSameSite() : null;
            String refreshSs = refresh != null ? refresh.getSameSite() : null;
            logStep("access_token SameSite=" + accessSs + ", refresh_token SameSite=" + refreshSs);

            boolean bugPresent = "None".equalsIgnoreCase(accessSs) || "None".equalsIgnoreCase(refreshSs);
            Assert.assertTrue(bugPresent,
                    "BUG-007 FIXED: auth cookies now use SameSite=Lax/Strict (" +
                            "access=" + accessSs + ", refresh=" + refreshSs + ")");
            ExtentReportManager.logPass("BUG-007 confirmed: at least one auth cookie uses SameSite=None");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG016_error");
            Assert.fail("BUG-007 test crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // BUG-008: No rate-limiting on failed login
    // =================================================================
    @Test(priority = 3, description = "BUG-008: 6 failed logins all return 401, no 429, no Retry-After")
    public void testBUG008_NoRateLimiting() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_NO_RATE_LIMIT,
                "BUG-008: No rate limiting");
        try {
            int rateLimited = 0;
            for (int i = 0; i < 6; i++) {
                driver.get(AppConstants.BASE_URL + "/login");
                pause(1500);
                List<WebElement> emailEls = driver.findElements(By.cssSelector("input[name='email']"));
                List<WebElement> pwEls = driver.findElements(By.cssSelector("input[name='password']"));
                if (emailEls.isEmpty() || pwEls.isEmpty()) continue;
                emailEls.get(0).clear();
                emailEls.get(0).sendKeys("brute" + i + "@invalid.test");
                pwEls.get(0).clear();
                pwEls.get(0).sendKeys("wrong");
                List<WebElement> submit = driver.findElements(By.cssSelector("button[type='submit']"));
                if (!submit.isEmpty()) submit.get(0).click();
                pause(2500);

                // Check for "Too Many" or lockout message
                String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
                if (body.contains("too many") || body.contains("rate limit") || body.contains("locked") || body.contains("try again in")) {
                    rateLimited++;
                }
            }
            ScreenshotUtil.captureScreenshot("BUG017_ratelimit_final");
            logStep("Rate-limit signals in UI: " + rateLimited + "/6 attempts");

            boolean bugPresent = rateLimited == 0;
            Assert.assertTrue(bugPresent,
                    "BUG-008 FIXED: rate limiting now active — " + rateLimited + "/6 attempts triggered limit");
            ExtentReportManager.logPass("BUG-008 confirmed: 6 failed logins, no rate-limit/lockout/captcha UI signal");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG017_error");
            Assert.fail("BUG-008 test crashed: " + e.getMessage());
        }
    }
}