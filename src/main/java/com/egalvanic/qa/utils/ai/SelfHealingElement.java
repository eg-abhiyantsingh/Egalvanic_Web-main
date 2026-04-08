package com.egalvanic.qa.utils.ai;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Self-Healing WebElement Wrapper.
 *
 * Wraps every WebElement returned by SelfHealingDriver and intercepts ALL operations
 * to handle the two most common flakiness sources:
 *
 * 1. StaleElementReferenceException — element was found but DOM re-rendered (React).
 *    Recovery: re-find using the original locator, then retry the operation.
 *
 * 2. ElementClickInterceptedException — overlay/backdrop blocking the click.
 *    Recovery: scroll into view, dismiss MUI backdrops, retry with JS click fallback.
 *
 * TRANSPARENT: Existing code sees a normal WebElement. No API changes needed.
 *
 * Statistics are tracked globally to report how many stale recoveries and
 * click interceptions were auto-healed during a test run.
 */
public class SelfHealingElement implements WebElement, WrapsElement {

    private WebElement delegate;
    private final By locator;
    private final SelfHealingDriver healingDriver;

    // Global statistics
    private static final AtomicInteger staleRecoveries = new AtomicInteger(0);
    private static final AtomicInteger clickInterceptions = new AtomicInteger(0);

    private static final int MAX_STALE_RETRIES = 3;
    private static final int MAX_CLICK_RETRIES = 3;
    private static final long STALE_RETRY_DELAY_MS = 300;

    // =========================================================================
    // CONSTRUCTION
    // =========================================================================

    private SelfHealingElement(WebElement element, By locator, SelfHealingDriver driver) {
        this.delegate = element;
        this.locator = locator;
        this.healingDriver = driver;
    }

    /**
     * Wrap a raw WebElement with self-healing capabilities.
     */
    static SelfHealingElement wrap(WebElement element, By locator, SelfHealingDriver driver) {
        if (element instanceof SelfHealingElement) {
            return (SelfHealingElement) element;
        }
        return new SelfHealingElement(element, locator, driver);
    }

    @Override
    public WebElement getWrappedElement() {
        return delegate;
    }

    // =========================================================================
    // CLICK — most flaky operation, gets full treatment
    // =========================================================================

    @Override
    public void click() {
        for (int attempt = 0; attempt <= MAX_CLICK_RETRIES; attempt++) {
            try {
                delegate.click();
                return; // Success
            } catch (StaleElementReferenceException e) {
                delegate = refind("click");
                if (attempt == MAX_CLICK_RETRIES) throw e;
            } catch (ElementClickInterceptedException e) {
                clickInterceptions.incrementAndGet();
                if (attempt == MAX_CLICK_RETRIES) {
                    // Last resort: JS click
                    jsClick();
                    return;
                }
                // Try to dismiss whatever is blocking
                dismissOverlays();
                scrollIntoView();
                sleep(300);
            }
        }
    }

    // =========================================================================
    // TEXT INPUT — sendKeys + clear with stale recovery
    // =========================================================================

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        retryOnStale(() -> delegate.sendKeys(keysToSend), "sendKeys");
    }

    @Override
    public void clear() {
        retryOnStale(() -> delegate.clear(), "clear");
    }

    @Override
    public void submit() {
        retryOnStale(() -> delegate.submit(), "submit");
    }

    // =========================================================================
    // PROPERTY ACCESS — with stale recovery
    // =========================================================================

    @Override
    public String getTagName() {
        return retryOnStaleReturn(() -> delegate.getTagName(), "getTagName");
    }

    @Override
    public String getAttribute(String name) {
        return retryOnStaleReturn(() -> delegate.getAttribute(name), "getAttribute(" + name + ")");
    }

    @Override
    public String getDomProperty(String name) {
        return retryOnStaleReturn(() -> delegate.getDomProperty(name), "getDomProperty");
    }

    @Override
    public String getDomAttribute(String name) {
        return retryOnStaleReturn(() -> delegate.getDomAttribute(name), "getDomAttribute");
    }

    @Override
    public String getAriaRole() {
        return retryOnStaleReturn(() -> delegate.getAriaRole(), "getAriaRole");
    }

    @Override
    public String getAccessibleName() {
        return retryOnStaleReturn(() -> delegate.getAccessibleName(), "getAccessibleName");
    }

    @Override
    public boolean isSelected() {
        return retryOnStaleReturn(() -> delegate.isSelected(), "isSelected");
    }

    @Override
    public boolean isEnabled() {
        return retryOnStaleReturn(() -> delegate.isEnabled(), "isEnabled");
    }

    @Override
    public String getText() {
        return retryOnStaleReturn(() -> delegate.getText(), "getText");
    }

    @Override
    public boolean isDisplayed() {
        return retryOnStaleReturn(() -> delegate.isDisplayed(), "isDisplayed");
    }

    @Override
    public Point getLocation() {
        return retryOnStaleReturn(() -> delegate.getLocation(), "getLocation");
    }

    @Override
    public Dimension getSize() {
        return retryOnStaleReturn(() -> delegate.getSize(), "getSize");
    }

    @Override
    public Rectangle getRect() {
        return retryOnStaleReturn(() -> delegate.getRect(), "getRect");
    }

    @Override
    public String getCssValue(String propertyName) {
        return retryOnStaleReturn(() -> delegate.getCssValue(propertyName), "getCssValue");
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        return retryOnStaleReturn(() -> delegate.getScreenshotAs(target), "getScreenshotAs");
    }

    // =========================================================================
    // CHILD ELEMENT FINDING — with stale recovery
    // =========================================================================

    @Override
    public WebElement findElement(By by) {
        return retryOnStaleReturn(() -> {
            WebElement child = delegate.findElement(by);
            return SelfHealingElement.wrap(child, by, healingDriver);
        }, "findElement");
    }

    @Override
    public List<WebElement> findElements(By by) {
        return retryOnStaleReturn(() -> delegate.findElements(by), "findElements");
    }

    // =========================================================================
    // STALE ELEMENT RECOVERY — the core anti-flake mechanism
    // =========================================================================

    /**
     * Re-find the element using the original locator.
     * This is called when StaleElementReferenceException is caught.
     */
    private WebElement refind(String operation) {
        staleRecoveries.incrementAndGet();
        for (int attempt = 1; attempt <= MAX_STALE_RETRIES; attempt++) {
            sleep(STALE_RETRY_DELAY_MS * attempt);
            try {
                WebElement fresh = healingDriver.refindElement(locator);
                System.out.println("[SelfHeal] Stale recovery succeeded for " + operation
                        + " on attempt " + attempt + ": " + locator);
                return fresh;
            } catch (NoSuchElementException e) {
                if (attempt == MAX_STALE_RETRIES) {
                    throw new StaleElementReferenceException(
                        "[SelfHeal] Could not recover stale element after " + MAX_STALE_RETRIES
                        + " attempts. Locator: " + locator + ", Operation: " + operation);
                }
            }
        }
        // Should not reach here
        throw new StaleElementReferenceException("[SelfHeal] Stale recovery exhausted for: " + locator);
    }

    /**
     * Retry a void operation on stale element exception.
     */
    private void retryOnStale(Runnable action, String operationName) {
        for (int attempt = 0; attempt <= MAX_STALE_RETRIES; attempt++) {
            try {
                action.run();
                return;
            } catch (StaleElementReferenceException e) {
                if (attempt == MAX_STALE_RETRIES) throw e;
                delegate = refind(operationName);
            }
        }
    }

    /**
     * Retry an operation that returns a value on stale element exception.
     */
    private <T> T retryOnStaleReturn(java.util.function.Supplier<T> action, String operationName) {
        for (int attempt = 0; attempt <= MAX_STALE_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                if (attempt == MAX_STALE_RETRIES) throw e;
                delegate = refind(operationName);
            }
        }
        // Should not reach here
        throw new StaleElementReferenceException("[SelfHeal] Retry exhausted for: " + operationName);
    }

    // =========================================================================
    // CLICK RECOVERY HELPERS
    // =========================================================================

    /**
     * JavaScript click fallback — bypasses any overlay.
     */
    private void jsClick() {
        try {
            WebDriver rawDriver = healingDriver.getWrappedDriver();
            if (rawDriver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) rawDriver).executeScript("arguments[0].click();", delegate);
            }
        } catch (StaleElementReferenceException e) {
            delegate = refind("jsClick");
            WebDriver rawDriver = healingDriver.getWrappedDriver();
            if (rawDriver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) rawDriver).executeScript("arguments[0].click();", delegate);
            }
        }
    }

    /**
     * Scroll the element into the viewport center.
     */
    private void scrollIntoView() {
        try {
            WebDriver rawDriver = healingDriver.getWrappedDriver();
            if (rawDriver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) rawDriver).executeScript(
                    "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", delegate);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Dismiss MUI Backdrop overlays and Beamer notification panels.
     * This handles the most common click interception cause in eGalvanic.
     */
    private void dismissOverlays() {
        try {
            WebDriver rawDriver = healingDriver.getWrappedDriver();
            if (rawDriver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) rawDriver).executeScript(
                    // Remove MUI Backdrop overlays
                    "document.querySelectorAll('.MuiBackdrop-root').forEach(function(b){"
                    + "  b.style.display='none'; b.style.pointerEvents='none';"
                    + "});"
                    // Remove Beamer notification panel
                    + "var beamer = document.getElementById('beamer-last-post-container');"
                    + "if(beamer) beamer.style.display='none';"
                    // Remove any fixed-position overlays blocking clicks
                    + "document.querySelectorAll('[class*=\"overlay\"],[class*=\"Overlay\"]').forEach(function(o){"
                    + "  if(window.getComputedStyle(o).position==='fixed') o.style.display='none';"
                    + "});");
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Get global statistics for stale element recoveries and click interceptions.
     */
    public static String getStatsSummary() {
        int stale = staleRecoveries.get();
        int clicks = clickInterceptions.get();
        if (stale == 0 && clicks == 0) {
            return "[SelfHeal] Elements: no stale recoveries or click interceptions needed.";
        }
        return String.format(
            "[SelfHeal] Elements: %d stale recoveries | %d click interception recoveries",
            stale, clicks);
    }

    public static int getStaleRecoveries() { return staleRecoveries.get(); }
    public static int getClickInterceptions() { return clickInterceptions.get(); }

    /**
     * Reset statistics (call between suites).
     */
    public static void resetStats() {
        staleRecoveries.set(0);
        clickInterceptions.set(0);
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Override
    public String toString() {
        return "[SelfHealingElement locator=" + locator + " delegate=" + delegate + "]";
    }
}
