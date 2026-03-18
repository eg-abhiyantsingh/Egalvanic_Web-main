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

    // PageFactory elements
    @FindBy(id = "email")
    WebElement emailField;

    @FindBy(id = "password")
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
     * Check if Terms and Conditions checkbox is displayed.
     */
    public boolean isTermsCheckboxDisplayed() {
        try {
            List<WebElement> labels = driver.findElements(By.xpath(
                    "//label[contains(.,'Terms') or contains(.,'agree')]"));
            for (WebElement label : labels) {
                if (label.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
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
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
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
