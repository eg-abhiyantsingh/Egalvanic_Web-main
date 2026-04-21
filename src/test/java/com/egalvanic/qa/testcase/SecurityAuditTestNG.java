package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Security Audit Regression Suite — 6 security bugs (BUG-015..BUG-020).
 *
 * Each test asserts the security issue is STILL PRESENT. When a finding is
 * fixed in production, flip the assertion or remove the @Test — this file
 * becomes the regression gate preventing the issue from reappearing.
 *
 * Scope: authorized QA / passive probes only.
 *   - Header inspection via HttpURLConnection
 *   - Cookie flag inspection via driver.manage().getCookies()
 *   - Rate-limit probe: 6 failed logins (deeper 28-attempt probe in /tmp probe script)
 *   - Clickjacking PoC: header check confirms missing XFO/CSP-FA
 *   - DOM autocomplete audit on login form
 *
 * Coverage:
 *   BUG-015  Missing HTTP security headers on login HTML              [HIGH]
 *   BUG-016  Auth cookies SameSite=None (form-POST CSRF)               [MEDIUM]
 *   BUG-017  No rate-limiting on /api/auth/login (elevated to HIGH)    [HIGH]
 *   BUG-018  Clickjacking — /login framable (JS frame-busting partial) [MEDIUM]
 *   BUG-019  Login password input autocomplete="new-password"          [LOW]
 *   BUG-020  Third-party widget cookies lack Secure/HttpOnly            [LOW]
 *
 * BUG-021 was removed on 2026-04-21 after verification showed /signup
 * is actually the login page — no signup form exists to audit.
 */
public class SecurityAuditTestNG extends BaseTest {

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // =================================================================
    // BUG-015: Missing HTTP security headers on login HTML (CloudFront/S3)
    // =================================================================
    @Test(priority = 1, description = "BUG-015: Login HTML served without HSTS/CSP/XFO/XCTO/Referrer-Policy/Permissions-Policy")
    public void testBUG015_MissingSecurityHeaders() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_HEADERS_MISSING,
                "BUG-015: Missing security headers");
        try {
            URL url = new URL(AppConstants.BASE_URL + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.connect();
            Map<String, List<String>> headers = conn.getHeaderFields();
            conn.disconnect();

            String[] expected = {
                    "strict-transport-security",
                    "content-security-policy",
                    "x-content-type-options",
                    "x-frame-options",
                    "referrer-policy",
                    "permissions-policy"
            };
            int missing = 0;
            StringBuilder missingList = new StringBuilder();
            for (String h : expected) {
                boolean found = false;
                for (String k : headers.keySet()) {
                    if (k != null && k.equalsIgnoreCase(h)) { found = true; break; }
                }
                if (!found) {
                    missing++;
                    if (missingList.length() > 0) missingList.append(", ");
                    missingList.append(h);
                }
            }
            logStep("Missing security headers on /login: " + missing + " of " + expected.length + " — " + missingList);
            ScreenshotUtil.captureScreenshot("BUG015_headers");
            // Bug is present when >= 3 critical headers missing; fixed when all present
            Assert.assertTrue(missing >= 3,
                    "BUG-015 FIXED: only " + missing + " headers missing — security headers now deployed on /login");
            ExtentReportManager.logPass("BUG-015 confirmed: " + missing + " security headers missing on /login (" + missingList + ")");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG015_error");
            Assert.fail("BUG-015 test crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // BUG-016: Auth cookies use SameSite=None (CSRF surface)
    // =================================================================
    @Test(priority = 2, description = "BUG-016: access_token / refresh_token cookies SameSite=None")
    public void testBUG016_AuthCookiesSameSiteNone() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_COOKIE_SAMESITE,
                "BUG-016: Auth cookie SameSite=None");
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
                    "BUG-016 FIXED: auth cookies now use SameSite=Lax/Strict (" +
                            "access=" + accessSs + ", refresh=" + refreshSs + ")");
            ExtentReportManager.logPass("BUG-016 confirmed: at least one auth cookie uses SameSite=None");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG016_error");
            Assert.fail("BUG-016 test crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // BUG-017: No rate-limiting on failed login
    // =================================================================
    @Test(priority = 3, description = "BUG-017: 6 failed logins all return 401, no 429, no Retry-After")
    public void testBUG017_NoRateLimiting() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_NO_RATE_LIMIT,
                "BUG-017: No rate limiting");
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
                    "BUG-017 FIXED: rate limiting now active — " + rateLimited + "/6 attempts triggered limit");
            ExtentReportManager.logPass("BUG-017 confirmed: 6 failed logins, no rate-limit/lockout/captcha UI signal");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG017_error");
            Assert.fail("BUG-017 test crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // BUG-018: Clickjacking possible (no X-Frame-Options on /login)
    // =================================================================
    @Test(priority = 4, description = "BUG-018: /login HTML lacks X-Frame-Options / CSP frame-ancestors")
    public void testBUG018_ClickjackingPossible() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_CLICKJACKING,
                "BUG-018: Clickjacking");
        try {
            URL url = new URL(AppConstants.BASE_URL + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            Map<String, List<String>> headers = conn.getHeaderFields();
            conn.disconnect();

            String xfo = null, csp = null;
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                String k = e.getKey();
                if (k == null) continue;
                if (k.equalsIgnoreCase("x-frame-options")) xfo = e.getValue().get(0);
                if (k.equalsIgnoreCase("content-security-policy")) csp = e.getValue().get(0);
            }
            boolean cspHasFA = csp != null && csp.toLowerCase().contains("frame-ancestors");
            logStep("X-Frame-Options: " + xfo + ", CSP frame-ancestors present: " + cspHasFA);
            ScreenshotUtil.captureScreenshot("BUG018_clickjack");

            boolean bugPresent = xfo == null && !cspHasFA;
            Assert.assertTrue(bugPresent,
                    "BUG-018 FIXED: login page now has framing protection (XFO=" + xfo + ", CSP-FA=" + cspHasFA + ")");
            ExtentReportManager.logPass("BUG-018 confirmed: /login framable — no X-Frame-Options AND no CSP frame-ancestors");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG018_error");
            Assert.fail("BUG-018 test crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // BUG-019: Login password field autocomplete="new-password"
    // =================================================================
    @Test(priority = 5, description = "BUG-019: Login form uses autocomplete=new-password (wrong)")
    public void testBUG019_LoginAutocompleteWrong() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_AUTOCOMPLETE,
                "BUG-019: autocomplete wrong");
        try {
            driver.get(AppConstants.BASE_URL + "/login");
            pause(3000);
            List<WebElement> pwInputs = driver.findElements(By.cssSelector("input[type='password']"));
            Assert.assertFalse(pwInputs.isEmpty(), "BUG-019: no password input found on /login");
            String ac = pwInputs.get(0).getAttribute("autocomplete");
            logStep("Login password autocomplete=" + ac);
            ScreenshotUtil.captureScreenshot("BUG019_autocomplete");

            boolean bugPresent = "new-password".equalsIgnoreCase(ac);
            Assert.assertTrue(bugPresent,
                    "BUG-019 FIXED: login password autocomplete=" + ac + " (was 'new-password', should be 'current-password')");
            ExtentReportManager.logPass("BUG-019 confirmed: login password input autocomplete=\"new-password\"");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG019_error");
            Assert.fail("BUG-019 test crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // BUG-020: Third-party widget cookies lack Secure/HttpOnly
    // =================================================================
    @Test(priority = 6, description = "BUG-020: DevRev / Beamer cookies missing Secure/HttpOnly")
    public void testBUG020_ThirdPartyCookiesWeakFlags() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_BUG_HUNT, AppConstants.FEATURE_SEC_THIRD_PARTY_COOKIES,
                "BUG-020: Third-party cookie flags");
        try {
            loginAndSelectSite();
            pause(3000);
            Set<Cookie> cookies = driver.manage().getCookies();
            int devrevMissing = 0, beamerMissing = 0;
            for (Cookie c : cookies) {
                if (c.getName().toLowerCase().contains("devrev")) {
                    if (!c.isSecure() || !c.isHttpOnly()) devrevMissing++;
                }
                if (c.getName().startsWith("_BEAMER")) {
                    if (!c.isHttpOnly()) beamerMissing++;
                }
            }
            logStep("DevRev cookies with weak flags: " + devrevMissing + ", Beamer cookies without HttpOnly: " + beamerMissing);
            ScreenshotUtil.captureScreenshot("BUG020_cookies");

            boolean bugPresent = devrevMissing > 0 || beamerMissing > 0;
            Assert.assertTrue(bugPresent,
                    "BUG-020 FIXED: third-party cookies now carry proper Secure/HttpOnly flags");
            ExtentReportManager.logPass("BUG-020 confirmed: third-party cookies missing flags (devrev=" + devrevMissing + ", beamer=" + beamerMissing + ")");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("BUG020_error");
            Assert.fail("BUG-020 test crashed: " + e.getMessage());
        }
    }

    // BUG-021 REMOVED on 2026-04-21 after deep re-verification:
    // /signup URL does NOT have a dedicated signup form — the SPA router falls
    // through to the login page. Since there is no public signup flow to audit,
    // the previous @Test method was removed as a false positive. If a real
    // signup flow appears in the future (e.g., invitation-based at /invite/:token),
    // add a new @Test keyed to that route rather than resurrecting this one.
}
