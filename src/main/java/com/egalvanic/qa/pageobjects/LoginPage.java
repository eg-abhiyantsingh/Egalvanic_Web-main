package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object Model for Login Page
 * Implements PageFactory pattern for better element management
 */
public class LoginPage {

    WebDriver driver;

    // PageFactory elements — locator chain tolerates the May 2026 login page
    // which dropped the id="email"/"password" attributes in favor of MUI
    // accessibility (aria-label / placeholder / type-based selectors).
    @FindBy(xpath = "//input[@id='email'] | //input[@type='email']"
            + " | //input[@name='email']"
            + " | //input[@placeholder='Email Address' or @placeholder='Email']"
            + " | //input[@aria-label='Email Address' or @aria-label='Email']")
    WebElement emailField;

    @FindBy(xpath = "//input[@id='password'] | //input[@type='password']"
            + " | //input[@name='password']"
            + " | //input[@placeholder='Password']"
            + " | //input[@aria-label='Password']")
    WebElement passwordField;

    @FindBy(xpath = "//button[@type='submit'][contains(.,'Sign In') or contains(.,'Sign in') or contains(.,'Login')]")
    WebElement loginButton;

    @FindBy(xpath = "//label[contains(.,'Terms') or contains(.,'agree')]//input[@type='checkbox'] | //input[@type='checkbox'][ancestor::label[contains(.,'Terms') or contains(.,'agree')]]")
    WebElement termsCheckbox;

    @FindBy(xpath = "//div[contains(@class,'error') or contains(@class,'alert') or contains(text(),'Incorrect')]")
    WebElement errorMessage;

    /**
     * Constructor that initializes PageFactory elements
     */
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ================================================================
    // CORE LOGIN METHODS
    // ================================================================

    /**
     * Set a React-controlled input value using the native value setter
     * so that React's internal state is updated properly.
     */
    private void setReactInputValue(WebElement element, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "var el = arguments[0]; "
                + "el.focus(); "
                + "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set; "
                + "nativeInputValueSetter.call(el, arguments[1]); "
                + "el.dispatchEvent(new Event('input', { bubbles: true })); "
                + "el.dispatchEvent(new Event('change', { bubbles: true }));",
                element, value);
    }

    /**
     * Enter email in the email field.
     * Uses React-compatible native value setter to update React state.
     */
    public void enterEmail(String email) {
        try {
            setReactInputValue(emailField, email);
        } catch (Exception e) {
            // Fallback to standard Selenium
            emailField.click();
            emailField.clear();
            emailField.sendKeys(email);
        }
    }

    /**
     * Enter password in the password field.
     * Uses React-compatible native value setter to update React state.
     */
    public void enterPassword(String password) {
        try {
            setReactInputValue(passwordField, password);
        } catch (Exception e) {
            // Fallback to standard Selenium
            passwordField.click();
            passwordField.clear();
            passwordField.sendKeys(password);
        }
    }

    /**
     * Click the login button. Falls back to JS click if element not interactable.
     */
    public void clickLoginButton() {
        try {
            loginButton.click();
        } catch (org.openqa.selenium.ElementNotInteractableException e) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", loginButton);
        }
    }

    /**
     * Accept Terms and Conditions checkbox if present and unchecked.
     */
    public void acceptTermsIfPresent() {
        try {
            List<WebElement> checkboxLabels = driver.findElements(By.xpath(
                    "//label[contains(.,'Terms') or contains(.,'agree')]"));
            for (WebElement label : checkboxLabels) {
                if (label.isDisplayed()) {
                    // Check if checkbox inside is already checked
                    WebElement cb = label.findElement(By.cssSelector("input[type='checkbox']"));
                    if (!cb.isSelected()) {
                        // Click the label (more reliable than the hidden checkbox)
                        label.click();
                    }
                    return;
                }
            }
        } catch (Exception e) {
            // No terms checkbox — proceed
        }
    }

    /**
     * Check if a Terms and Conditions CHECKBOX is displayed.
     *
     * <p>Live state (2026-08-24): there is none. The login page carries zero checkboxes —
     * consent is now the plain sentence "By signing in, you agree to our Terms and Conditions
     * and Privacy Policy" with two links.
     *
     * <p>The previous implementation only looked for a {@code <label>} mentioning
     * "Terms"/"agree" and never checked for a checkbox at all, so it answered the wrong
     * question: any wrapper around that new sentence would make it report a checkbox that does
     * not exist. Assert on the actual control.
     */
    public boolean isTermsCheckboxDisplayed() {
        try {
            for (WebElement cb : driver.findElements(By.cssSelector("input[type='checkbox']"))) {
                if (cb.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Pin the login page to English.
     *
     * <p>V1.36 added an English/Français toggle to the login screen, and the choice persists.
     * A session left in French renders "Se connecter" instead of "Sign In", which defeats every
     * text-matched locator here — so pin the language before asserting on any copy. No-op when
     * the toggle is absent or English is already active.
     */
    public void selectEnglishIfOffered() {
        try {
            for (WebElement b : driver.findElements(By.xpath(
                    "//button[normalize-space()='English' or @value='en']"))) {
                if (!b.isDisplayed()) continue;
                if (!"true".equals(b.getAttribute("aria-pressed"))) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b);
                    Thread.sleep(600);
                }
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // Toggle not present on this build — nothing to pin.
        }
    }

    /**
     * Perform login with email and password.
     * Automatically accepts Terms checkbox if present.
     */
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        acceptTermsIfPresent();
        // Wait for Sign In button to become enabled
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[@type='submit'][contains(.,'Sign In') or contains(.,'Sign in') or contains(.,'Login')]")));
        } catch (Exception e) {
            // Proceed anyway
        }
        clickLoginButton();
        dismissMfaEnrollmentIfPresent();
    }

    /**
     * Dismiss the "Set up your authenticator app" enrollment screen that QA began showing
     * after a successful sign-in (first observed 2026-09-02).
     *
     * <p><b>Why this is here and not in one test class.</b> Authentication SUCCEEDS — the
     * session cookie is set and the app routes to /dashboard — and then this screen covers the
     * whole application. Measured on QA: {@code #root} drops to 178 bytes, and the page has
     * zero anchors, zero {@code [role=tab]} elements and no data grid. So every UI test that
     * logs in finds an empty application: the symptom is "every locator on every page stopped
     * matching", which reads exactly like a redesign broke the selectors. Putting the dismissal
     * at the end of {@link #login(String, String)} fixes {@code BaseTest} and every standalone
     * suite in one place, because they all authenticate through here.
     *
     * <p>Enrollment is optional — the screen offers "Set up later", and its own footnote says
     * "You'll be asked again next time you sign in" — so clicking it is the behaviour of a user
     * who declines, not a workaround that skips a required step. It reappears per sign-in,
     * which for a per-class browser session means once per class.
     *
     * <p>The poll exits as soon as EITHER the enrollment screen is handled OR the app shell has
     * mounted, so a login that never sees the prompt pays a few hundred milliseconds rather
     * than the full timeout.
     */
    public void dismissMfaEnrollmentIfPresent() {
        By setUpLater = By.xpath(
                "//button[normalize-space(.)='Set up later'"
                + " or normalize-space(.)='Set up Later'"
                + " or normalize-space(.)='Skip for now'"
                + " or normalize-space(.)='Remind me later']");
        By enrollmentHeading = By.xpath(
                "//*[contains(normalize-space(.),'Set up your authenticator app')"
                + " or contains(normalize-space(.),'Scan this code with an authenticator app')]");
        // The mounted app shell — the signal that no prompt is in the way.
        By appShell = By.cssSelector("nav[aria-label='navigation'], main");

        long deadline = System.currentTimeMillis() + 15000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.util.List<WebElement> later = driver.findElements(setUpLater);
                if (!later.isEmpty()) {
                    WebElement btn = later.get(0);
                    try {
                        ((org.openqa.selenium.JavascriptExecutor) driver)
                                .executeScript("arguments[0].click();", btn);
                    } catch (Exception e) {
                        btn.click();
                    }
                    System.out.println("[LoginPage] Declined authenticator-app enrollment via 'Set up later'");
                    // Give the shell a moment to mount behind the dismissed screen.
                    try {
                        new WebDriverWait(driver, Duration.ofSeconds(20))
                                .until(ExpectedConditions.presenceOfElementLocated(appShell));
                    } catch (Exception ignored) { }
                    return;
                }

                boolean promptShowing = !driver.findElements(enrollmentHeading).isEmpty();
                if (!promptShowing && !driver.findElements(appShell).isEmpty()) {
                    return;   // no prompt, shell is up — nothing to do
                }
                if (promptShowing) {
                    System.out.println("[LoginPage] Authenticator enrollment screen visible; "
                            + "waiting for its 'Set up later' control");
                }
            } catch (Exception ignored) { }
            try { Thread.sleep(400); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        // Not fatal on its own: report it and let the caller's own waits fail with their
        // context, rather than throwing from a helper that only tries to clear an overlay.
        if (!driver.findElements(enrollmentHeading).isEmpty()) {
            System.out.println("[LoginPage] WARNING: authenticator enrollment screen is still up after 15s "
                    + "and no 'Set up later' control was found. The app shell is likely covered, so "
                    + "page locators will not match — this is an auth-flow blocker, not a selector problem.");
        }
    }

    // ================================================================
    // FIELD DISPLAY / VISIBILITY CHECKS
    // ================================================================

    /**
     * Check if the login page is loaded (email field visible)
     */
    public boolean isPageLoaded() {
        try {
            return emailField.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for the login page to load within a timeout
     */
    public boolean waitForPageLoaded(int timeoutSeconds) {
        // The new May 2026 login page dropped @id="email". Match by any of
        // id / type / placeholder / aria-label, same as the LoginPage
        // PageFactory locator chain.
        org.openqa.selenium.By emailFieldAny = org.openqa.selenium.By.xpath(
                "//input[@id='email'] | //input[@type='email']"
                + " | //input[@placeholder='Email Address' or @placeholder='Email']"
                + " | //input[@aria-label='Email Address' or @aria-label='Email']");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(emailFieldAny));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if email field is displayed
     */
    public boolean isEmailFieldDisplayed() {
        try {
            return emailField.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if password field is displayed
     */
    public boolean isPasswordFieldDisplayed() {
        try {
            return passwordField.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if sign in / login button is displayed
     */
    public boolean isSignInButtonDisplayed() {
        try {
            if (loginButton.isDisplayed()) return true;
        } catch (Exception e) {
            // PageFactory element not found, try dynamic search
        }
        try {
            List<WebElement> btns = driver.findElements(By.xpath(
                    "//button[@type='submit'][contains(.,'Sign In') or contains(.,'Sign in') or contains(.,'Login')]"));
            for (WebElement btn : btns) {
                // Scroll into view to ensure visibility in headless mode
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", btn);
                if (btn.isDisplayed()) return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * Check if sign in button is enabled (not disabled)
     */
    public boolean isSignInButtonEnabled() {
        try {
            return loginButton.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // FIELD STATE METHODS
    // ================================================================

    /**
     * Check if email field is empty
     */
    public boolean isEmailFieldEmpty() {
        try {
            String value = emailField.getAttribute("value");
            return value == null || value.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Check if password field is empty
     */
    public boolean isPasswordFieldEmpty() {
        try {
            String value = passwordField.getAttribute("value");
            return value == null || value.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get the current text in the email field
     */
    public String getEmailText() {
        try {
            return emailField.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the current text in the password field
     */
    public String getPasswordText() {
        try {
            return passwordField.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the email field placeholder text
     */
    public String getEmailPlaceholder() {
        try {
            return emailField.getAttribute("placeholder");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the password field placeholder text
     */
    public String getPasswordPlaceholder() {
        try {
            return passwordField.getAttribute("placeholder");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the type attribute of the password field (should be "password" for masking)
     */
    public String getPasswordFieldType() {
        try {
            return passwordField.getAttribute("type");
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // CLEAR METHODS
    // ================================================================

    /**
     * Clear the email field
     */
    public void clearEmail() {
        try {
            emailField.clear();
            // Also use JS to ensure field is truly empty (React controlled components)
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "var el = arguments[0]; "
                    + "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set; "
                    + "nativeInputValueSetter.call(el, ''); "
                    + "el.dispatchEvent(new Event('input', { bubbles: true })); "
                    + "el.dispatchEvent(new Event('change', { bubbles: true }));",
                    emailField);
        } catch (Exception e) {
            // Fallback: select all and delete
            emailField.sendKeys(org.openqa.selenium.Keys.chord(
                    org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.DELETE);
        }
    }

    /**
     * Clear the password field
     */
    public void clearPassword() {
        try {
            passwordField.clear();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "var el = arguments[0]; "
                    + "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set; "
                    + "nativeInputValueSetter.call(el, ''); "
                    + "el.dispatchEvent(new Event('input', { bubbles: true })); "
                    + "el.dispatchEvent(new Event('change', { bubbles: true }));",
                    passwordField);
        } catch (Exception e) {
            passwordField.sendKeys(org.openqa.selenium.Keys.chord(
                    org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.DELETE);
        }
    }

    /**
     * Clear both email and password fields
     */
    public void clearAllFields() {
        clearEmail();
        clearPassword();
    }

    // ================================================================
    // ERROR MESSAGE METHODS
    // ================================================================

    /**
     * Get the error message text
     */
    public String getErrorMessageText() {
        return errorMessage.getText();
    }

    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        try {
            // Check PageFactory element
            if (errorMessage.isDisplayed()) return true;
        } catch (Exception e) {
            // Not found via PageFactory, try broader search
        }

        try {
            // Broader search for error messages
            List<WebElement> errors = driver.findElements(By.xpath(
                    "//*[contains(@class,'error') or contains(@class,'alert') or contains(@class,'Error')]"
                    + "[string-length(normalize-space()) > 0]"));
            for (WebElement el : errors) {
                try {
                    String text = el.getText().trim();
                    if (!text.isEmpty() && !text.equalsIgnoreCase("error")) return true;
                } catch (Exception ignored) {}
            }

            // Text-based error detection
            List<WebElement> textErrors = driver.findElements(By.xpath(
                    "//*[contains(text(),'Incorrect') or contains(text(),'Invalid') "
                    + "or contains(text(),'incorrect') or contains(text(),'invalid') "
                    + "or contains(text(),'wrong') or contains(text(),'Wrong') "
                    + "or contains(text(),'failed') or contains(text(),'not found')]"));
            if (!textErrors.isEmpty()) {
                for (WebElement el : textErrors) {
                    try {
                        if (!el.getText().trim().isEmpty()) return true;
                    } catch (Exception ignored) {}
                }
            }

            // MUI Alert/Snackbar
            List<WebElement> alerts = driver.findElements(By.cssSelector(
                    ".MuiAlert-root, .MuiSnackbar-root, [role='alert']"));
            for (WebElement alert : alerts) {
                try {
                    if (!alert.getText().trim().isEmpty()) return true;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // ignore
        }

        return false;
    }

    /**
     * Wait for error message to appear within timeout
     */
    public boolean waitForErrorMessage(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            if (isErrorMessageDisplayed()) return true;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    // ================================================================
    // LINK / EXTRA ELEMENT CHECKS
    // ================================================================

    /**
     * Check if "Forgot Password" link is displayed
     */
    public boolean isForgotPasswordDisplayed() {
        try {
            List<WebElement> links = driver.findElements(By.xpath(
                    "//a[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'forgot')] "
                    + "| //button[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'forgot')] "
                    + "| //*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'forgot password')]"));
            for (WebElement link : links) {
                if (link.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if "Change Company" or similar link is displayed on the login page
     */
    public boolean isChangeCompanyLinkDisplayed() {
        try {
            List<WebElement> links = driver.findElements(By.xpath(
                    "//a[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'change company')] "
                    + "| //a[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'switch company')] "
                    + "| //button[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'change company')]"));
            for (WebElement link : links) {
                if (link.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click the Sign In / Login button (alias for clickLoginButton)
     */
    public void tapSignIn() {
        clickLoginButton();
    }
}
