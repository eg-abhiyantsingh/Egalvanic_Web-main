package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Login Consent — ZP-1828 verification suite (standalone, pre-auth).
 *
 * Why this is standalone (not extending BaseTest):
 *   BaseTest performs a class-level login during @BeforeClass. Once authenticated,
 *   /login redirects to /dashboard and the React SPA preserves the auth state in
 *   memory — even after clearing cookies + localStorage + sessionStorage + IndexedDB
 *   + ServiceWorker registrations, the URL becomes /login but the body keeps
 *   rendering dashboard content. Validating the LOGIN PAGE itself requires a
 *   ChromeDriver that has never been authenticated. Same pattern as BugHuntTestNG.
 *
 * What this verifies (post ZP-1828 "Web Sign-in: Remove T&C checkbox" shipped):
 *   TC_LC_01  Inline consent text "By signing in, you agree to our Terms and
 *             Conditions and Privacy Policy" present under Sign In button
 *   TC_LC_02  Standalone T&C-labeled checkbox is GONE (regression tripwire)
 *   TC_LC_03  Terms and Privacy Policy links exist and have valid hrefs
 *             (compliance: links must be navigable, not href="#" or javascript:)
 *   TC_LC_04  Sign In button is correctly gated — disabled on empty form
 *             (covers BUG16 finding from this week)
 *
 * Why each test matters:
 *   - 01 confirms the new consent design shipped (catches a release where the
 *     consent text is forgotten and only the checkbox-removal makes it through)
 *   - 02 catches a regression where a developer adds a stray checkbox back
 *     without realising ZP-1828 explicitly removed it
 *   - 03 catches a "we shipped an empty href" deployment screw-up
 *   - 04 catches the security UX gap where Sign In becomes clickable on empty
 *     form, currently confirmed as a real product bug this week
 */
public class LoginConsentTestNG {

    private static final String MODULE = AppConstants.MODULE_AUTHENTICATION;
    private static final String FEATURE = "Login Consent (ZP-1828)";

    private WebDriver driver;
    private JavascriptExecutor js;

    // Locators
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By SIGN_IN_BUTTON = By.xpath("//button[normalize-space()='Sign In']");

    @BeforeClass
    public void classSetup() {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("     Login Consent (ZP-1828) Test Suite Starting");
        System.out.println("     " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================");

        // Initialize ExtentReports for isolated runs
        ExtentReportManager.initReports();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        if ("true".equals(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        js = (JavascriptExecutor) driver;
        ScreenshotUtil.setDriver(driver);

        // Navigate to login page (we're never authenticated, so it actually renders)
        driver.get(AppConstants.BASE_URL + "/login");
        sleep(4000);
        js.executeScript("document.body.style.zoom='80%';");
    }

    @AfterClass
    public void classTeardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
        System.out.println("Login Consent Test Suite completed.");
    }

    @Test(priority = 1, description = "TC_LC_01: Inline consent text present under Sign In button (ZP-1828)")
    public void testTC_LC_01_InlineConsentTextPresent() {
        ExtentReportManager.createTest(MODULE, FEATURE,
                "TC_LC_01_InlineConsentTextPresent (ZP-1828 design verification)");
        ExtentReportManager.logInfo("Verifying inline consent statement present under Sign In button");

        ensureOnLoginPage();
        ScreenshotUtil.captureScreenshot("TC_LC_01_login_page");

        String bodyText = driver.findElement(By.tagName("body")).getText();
        boolean hasConsentText = bodyText.matches(
                "(?is).*by\\s+signing\\s+in.*you\\s+agree.*terms.*and\\s+conditions.*privacy.*policy.*");

        Assert.assertTrue(hasConsentText,
                "Inline consent text 'By signing in, you agree to our Terms and Conditions and "
                + "Privacy Policy' missing on login page. ZP-1828 design regression. "
                + "Body excerpt: " + bodyText.substring(0, Math.min(400, bodyText.length())));
        ExtentReportManager.logPass("ZP-1828 inline consent text rendered correctly");
    }

    @Test(priority = 2, description = "TC_LC_02: T&C-labeled checkbox is removed from login form (ZP-1828)")
    public void testTC_LC_02_TermsCheckboxAbsent() {
        ExtentReportManager.createTest(MODULE, FEATURE,
                "TC_LC_02_TermsCheckboxAbsent (ZP-1828 regression tripwire)");
        ExtentReportManager.logInfo("Verifying T&C-labeled checkbox is NOT on login page");

        ensureOnLoginPage();

        // Filter to checkboxes whose label/aria mentions Terms/Conditions/Agree.
        // A "Show password" checkbox or "Remember me" — if those exist — would still pass.
        List<WebElement> tcCheckboxes = driver.findElements(By.xpath(
                "//input[@type='checkbox'][contains(translate(@aria-label,'TERMS','terms'),'terms') "
                + "or contains(translate(@aria-label,'AGREE','agree'),'agree') "
                + "or contains(translate(@aria-label,'CONDITIONS','conditions'),'conditions')]"
                + " | //label[contains(translate(., 'TERMS','terms'), 'terms') "
                + "or contains(translate(., 'AGREE','agree'), 'agree')]//input[@type='checkbox']"));

        int visible = 0;
        for (WebElement cb : tcCheckboxes) {
            if (cb.isDisplayed()) visible++;
        }

        ScreenshotUtil.captureScreenshot("TC_LC_02_checkboxes");
        Assert.assertEquals(visible, 0,
                "Standalone T&C checkbox detected on login page — ZP-1828 explicitly removed it. "
                + "Found " + visible + " visible T&C-labeled checkbox(es).");
        ExtentReportManager.logPass("No T&C checkbox on login (ZP-1828 still effective)");
    }

    @Test(priority = 3, description = "TC_LC_03: Terms + Privacy Policy links exist with valid hrefs")
    public void testTC_LC_03_TermsAndPrivacyLinksValid() {
        ExtentReportManager.createTest(MODULE, FEATURE,
                "TC_LC_03_TermsAndPrivacyLinksValid (compliance)");
        ExtentReportManager.logInfo("Verifying T&C and Privacy Policy links have navigable hrefs");

        ensureOnLoginPage();

        // Find anchor tags with link text matching T&C / Privacy Policy
        List<WebElement> termsLinks = driver.findElements(By.xpath(
                "//a[contains(normalize-space(.), 'Terms') and contains(normalize-space(.), 'Conditions')]"
                + " | //a[normalize-space(.)='Terms of Service']"));
        List<WebElement> privacyLinks = driver.findElements(By.xpath(
                "//a[contains(normalize-space(.), 'Privacy Policy')] "
                + " | //a[contains(normalize-space(.), 'Privacy Notice')]"));

        Assert.assertFalse(termsLinks.isEmpty(),
                "Terms and Conditions link missing on login — compliance regression");
        Assert.assertFalse(privacyLinks.isEmpty(),
                "Privacy Policy link missing on login — compliance regression");

        // Validate hrefs aren't dummy values
        String termsHref = termsLinks.get(0).getAttribute("href");
        String privacyHref = privacyLinks.get(0).getAttribute("href");
        boolean termsValid = isValidNavigableHref(termsHref);
        boolean privacyValid = isValidNavigableHref(privacyHref);

        ScreenshotUtil.captureScreenshot("TC_LC_03_links");
        Assert.assertTrue(termsValid,
                "Terms link href is not a valid navigable URL: '" + termsHref + "'");
        Assert.assertTrue(privacyValid,
                "Privacy link href is not a valid navigable URL: '" + privacyHref + "'");
        ExtentReportManager.logPass("T&C + Privacy Policy links valid (terms=" + termsHref
                + ", privacy=" + privacyHref + ")");
    }

    @Test(priority = 4, description = "TC_LC_04: Sign In button correctly disabled on empty form (BUG16)")
    public void testTC_LC_04_SignInGatedOnEmptyForm() {
        ExtentReportManager.createTest(MODULE, FEATURE,
                "TC_LC_04_SignInGatedOnEmptyForm (covers BUG16)");
        ExtentReportManager.logInfo("Verifying Sign In is disabled on empty form (no email + password)");

        ensureOnLoginPage();

        // Make sure form is empty
        try {
            driver.findElement(EMAIL_INPUT).clear();
            driver.findElement(PASSWORD_INPUT).clear();
        } catch (Exception ignored) {}

        WebElement signIn = driver.findElement(SIGN_IN_BUTTON);
        boolean enabled = isElementClickable(signIn);
        ScreenshotUtil.captureScreenshot("TC_LC_04_empty_form");

        Assert.assertFalse(enabled,
                "Sign In button is enabled on completely empty form — should be gated until "
                + "email + password are filled. This was confirmed live as a real product bug "
                + "(BUG16) on 2026-04-24 — not covered by the ZP-1828 T&C removal which still "
                + "left this gating gap.");
        ExtentReportManager.logPass("Sign In correctly disabled on empty form");
    }

    /** Checks 4 disabled-state mechanisms (DOM, class, aria, pointer-events). */
    private boolean isElementClickable(WebElement el) {
        if (el == null) return false;
        boolean domEnabled = el.isEnabled();
        String cls = el.getAttribute("class");
        boolean classEnabled = cls == null || !cls.contains("Mui-disabled");
        String ariaDisabled = el.getAttribute("aria-disabled");
        boolean ariaEnabled = ariaDisabled == null || !"true".equalsIgnoreCase(ariaDisabled);
        String pointerEvents = el.getCssValue("pointer-events");
        boolean pointerEnabled = !"none".equalsIgnoreCase(pointerEvents);
        return domEnabled && classEnabled && ariaEnabled && pointerEnabled;
    }

    private boolean isValidNavigableHref(String href) {
        if (href == null || href.isBlank()) return false;
        // Reject dummy hrefs that look intentional but don't navigate anywhere meaningful
        String lower = href.toLowerCase().trim();
        if (lower.equals("#") || lower.startsWith("javascript:") || lower.equals("javascript:void(0)")) {
            return false;
        }
        return lower.startsWith("http") || lower.startsWith("/") || lower.startsWith("mailto:");
    }

    private void ensureOnLoginPage() {
        if (!driver.getCurrentUrl().contains("/login")
                && driver.findElements(EMAIL_INPUT).isEmpty()) {
            driver.get(AppConstants.BASE_URL + "/login");
            sleep(3000);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
