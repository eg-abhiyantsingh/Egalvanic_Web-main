package com.egalvanic.qa.utils.ai;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Transparent Self-Healing WebDriver Wrapper.
 *
 * Wraps any WebDriver instance and intercepts ALL findElement/findElements calls
 * to add automatic retry, alternative locator strategies, and stale element recovery.
 *
 * ZERO changes needed to existing test code — just wrap the driver at creation:
 *   driver = SelfHealingDriver.wrap(new ChromeDriver(opts));
 *
 * Every element returned is also wrapped in SelfHealingElement, which automatically
 * retries operations that fail due to StaleElementReferenceException.
 *
 * Features:
 *   - Auto-retry findElement with configurable retry count and backoff
 *   - Alternative locator strategies when primary locator fails
 *   - Stale element recovery (re-find + re-execute)
 *   - Click interception recovery (dismiss overlays, retry)
 *   - Persistent healing registry (learns from past failures)
 *   - Statistics tracking (healed count, retry count, etc.)
 */
public class SelfHealingDriver implements WebDriver, JavascriptExecutor, TakesScreenshot, WrapsDriver {

    private final WebDriver delegate;
    private final JavascriptExecutor jsDelegate;
    private final TakesScreenshot screenshotDelegate;

    // Configuration
    private int maxRetries = 3;
    private long retryDelayMs = 500;
    private boolean healingEnabled = true;
    private boolean autoWaitEnabled = true;

    // Statistics
    private final AtomicInteger totalFinds = new AtomicInteger(0);
    private final AtomicInteger retriedFinds = new AtomicInteger(0);
    private final AtomicInteger healedFinds = new AtomicInteger(0);
    private final AtomicInteger failedFinds = new AtomicInteger(0);
    private final AtomicLong totalRetryTimeMs = new AtomicLong(0);

    // =========================================================================
    // CONSTRUCTION
    // =========================================================================

    private SelfHealingDriver(WebDriver driver) {
        this.delegate = driver;
        this.jsDelegate = (driver instanceof JavascriptExecutor) ? (JavascriptExecutor) driver : null;
        this.screenshotDelegate = (driver instanceof TakesScreenshot) ? (TakesScreenshot) driver : null;
    }

    /**
     * Wrap a WebDriver with self-healing capabilities.
     * This is the main entry point — call this in BaseTest instead of using the raw driver.
     */
    public static SelfHealingDriver wrap(WebDriver driver) {
        if (driver instanceof SelfHealingDriver) {
            return (SelfHealingDriver) driver; // Don't double-wrap
        }
        return new SelfHealingDriver(driver);
    }

    /**
     * Get the underlying raw driver (needed for driver.quit(), etc.)
     */
    @Override
    public WebDriver getWrappedDriver() {
        return delegate;
    }

    // =========================================================================
    // CONFIGURATION
    // =========================================================================

    public SelfHealingDriver withMaxRetries(int retries) {
        this.maxRetries = retries;
        return this;
    }

    public SelfHealingDriver withRetryDelay(long delayMs) {
        this.retryDelayMs = delayMs;
        return this;
    }

    public SelfHealingDriver withHealing(boolean enabled) {
        this.healingEnabled = enabled;
        return this;
    }

    public SelfHealingDriver withAutoWait(boolean enabled) {
        this.autoWaitEnabled = enabled;
        return this;
    }

    // =========================================================================
    // CORE: findElement with self-healing
    // =========================================================================

    @Override
    public WebElement findElement(By by) {
        totalFinds.incrementAndGet();
        long startTime = System.currentTimeMillis();

        // Attempt 1: Direct find
        try {
            WebElement el = delegate.findElement(by);
            return SelfHealingElement.wrap(el, by, this);
        } catch (NoSuchElementException firstFailure) {
            // Fall through to retry/healing
        }

        // Attempt 2-N: Retry with backoff (handles timing issues)
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            retriedFinds.incrementAndGet();
            sleep(retryDelayMs * attempt); // Progressive backoff

            try {
                WebElement el = delegate.findElement(by);
                long elapsed = System.currentTimeMillis() - startTime;
                totalRetryTimeMs.addAndGet(elapsed);
                System.out.println("[SelfHeal] Retry #" + attempt + " succeeded for: " + by
                        + " (" + elapsed + "ms)");
                return SelfHealingElement.wrap(el, by, this);
            } catch (NoSuchElementException e) {
                // Continue retrying
            }
        }

        // Attempt N+1: Self-healing strategies (try alternative locators)
        if (healingEnabled) {
            WebElement healed = tryHealingStrategies(by);
            if (healed != null) {
                healedFinds.incrementAndGet();
                long elapsed = System.currentTimeMillis() - startTime;
                totalRetryTimeMs.addAndGet(elapsed);
                return healed;
            }
        }

        // All attempts exhausted
        failedFinds.incrementAndGet();
        throw new NoSuchElementException(
                "[SelfHeal] Element not found after " + maxRetries + " retries + healing: " + by);
    }

    @Override
    public List<WebElement> findElements(By by) {
        totalFinds.incrementAndGet();

        // Attempt 1: Direct find
        List<WebElement> elements = delegate.findElements(by);
        if (!elements.isEmpty()) {
            return wrapElements(elements, by);
        }

        // Retry with backoff
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            retriedFinds.incrementAndGet();
            sleep(retryDelayMs * attempt);

            elements = delegate.findElements(by);
            if (!elements.isEmpty()) {
                return wrapElements(elements, by);
            }
        }

        // Return empty list (findElements doesn't throw)
        return new ArrayList<>();
    }

    /**
     * Internal re-find used by SelfHealingElement when recovering from stale references.
     * Does NOT count towards statistics to avoid inflating numbers.
     */
    WebElement refindElement(By by) {
        return delegate.findElement(by);
    }

    /**
     * Internal re-findElements used by SelfHealingElement.
     */
    List<WebElement> refindElements(By by) {
        return delegate.findElements(by);
    }

    // =========================================================================
    // HEALING STRATEGIES
    // =========================================================================

    private WebElement tryHealingStrategies(By originalBy) {
        String byStr = originalBy.toString();
        System.out.println("[SelfHeal] Attempting healing for: " + byStr);

        List<By> alternatives = generateAlternativeLocators(originalBy);

        for (By alt : alternatives) {
            try {
                List<WebElement> found = delegate.findElements(alt);
                if (!found.isEmpty()) {
                    WebElement el = findFirstVisible(found);
                    if (el != null) {
                        System.out.println("[SelfHeal] HEALED: " + byStr + " → " + alt);
                        // Register in SelfHealingLocator's persistent registry for future runs
                        SelfHealingLocator.registerHealFromDriver(originalBy, alt);
                        return SelfHealingElement.wrap(el, alt, this);
                    }
                }
            } catch (Exception ignored) {
                // Strategy failed, try next
            }
        }

        // Last resort: use SelfHealingLocator's AI-powered healing
        if (healingEnabled && jsDelegate != null) {
            try {
                // Extract a human-readable description from the locator
                String description = extractDescription(byStr);
                WebElement healed = SelfHealingLocator.findElement(delegate, originalBy, description);
                if (healed != null) {
                    return SelfHealingElement.wrap(healed, originalBy, this);
                }
            } catch (Exception e) {
                // AI healing also failed
            }
        }

        return null;
    }

    /**
     * Generate alternative locators from the original.
     * Extracts text, IDs, classes, attributes from the original XPath/CSS and tries variants.
     */
    private List<By> generateAlternativeLocators(By originalBy) {
        List<By> alternatives = new ArrayList<>();
        String byStr = originalBy.toString();

        // Check SelfHealingLocator's persistent registry first
        try {
            By cached = SelfHealingLocator.getCachedHeal(originalBy);
            if (cached != null) {
                alternatives.add(cached);
            }
        } catch (Exception ignored) {}

        // Extract text content from locator patterns
        String text = extractQuotedText(byStr, "normalize-space()='", "'");
        if (text == null) text = extractQuotedText(byStr, "text()='", "'");
        if (text == null) text = extractQuotedText(byStr, "contains(normalize-space(),'", "'");
        if (text == null) text = extractQuotedText(byStr, "contains(text(),'", "'");

        if (text != null) {
            // Try variations: exact text, contains, aria-label, title
            alternatives.add(By.xpath("//*[normalize-space()='" + text + "']"));
            alternatives.add(By.xpath("//*[contains(normalize-space(),'" + text + "')]"));
            alternatives.add(By.xpath("//*[@aria-label='" + text + "' or @title='" + text + "']"));
        }

        // Extract ID
        String id = extractQuotedText(byStr, "@id='", "'");
        if (id != null) {
            alternatives.add(By.id(id));
            alternatives.add(By.cssSelector("#" + id));
            // Try partial ID match (React sometimes appends random suffixes)
            alternatives.add(By.cssSelector("[id*='" + id + "']"));
        }

        // Extract class name
        String className = extractQuotedText(byStr, "contains(@class,'", "'");
        if (className != null) {
            alternatives.add(By.cssSelector("[class*='" + className + "']"));
        }

        // Extract placeholder
        String placeholder = extractQuotedText(byStr, "@placeholder='", "'");
        if (placeholder != null) {
            alternatives.add(By.xpath("//input[@placeholder='" + placeholder + "']"));
            alternatives.add(By.xpath("//textarea[@placeholder='" + placeholder + "']"));
            alternatives.add(By.cssSelector("input[placeholder='" + placeholder + "']"));
        }

        // Extract data-testid
        String testId = extractQuotedText(byStr, "@data-testid='", "'");
        if (testId != null) {
            alternatives.add(By.cssSelector("[data-testid='" + testId + "']"));
        }

        // Extract role
        String role = extractQuotedText(byStr, "@role='", "'");
        if (role != null && text != null) {
            alternatives.add(By.xpath("//*[@role='" + role + "'][contains(normalize-space(),'" + text + "')]"));
        }

        return alternatives;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private WebElement findFirstVisible(List<WebElement> elements) {
        for (WebElement el : elements) {
            try {
                if (el.isDisplayed()) return el;
            } catch (StaleElementReferenceException e) {
                // Element went stale between find and visibility check
            }
        }
        // If none visible, return first (may be hidden but interactable via JS)
        return elements.isEmpty() ? null : elements.get(0);
    }

    private List<WebElement> wrapElements(List<WebElement> elements, By by) {
        List<WebElement> wrapped = new ArrayList<>(elements.size());
        for (WebElement el : elements) {
            wrapped.add(SelfHealingElement.wrap(el, by, this));
        }
        return wrapped;
    }

    private String extractQuotedText(String source, String prefix, String suffix) {
        int idx = source.indexOf(prefix);
        if (idx >= 0) {
            int start = idx + prefix.length();
            int end = source.indexOf(suffix, start);
            if (end > start) return source.substring(start, end);
        }
        return null;
    }

    private String extractDescription(String byString) {
        // Try to extract a readable description from the locator
        String text = extractQuotedText(byString, "normalize-space()='", "'");
        if (text != null) return text;
        text = extractQuotedText(byString, "@placeholder='", "'");
        if (text != null) return text;
        text = extractQuotedText(byString, "@aria-label='", "'");
        if (text != null) return text;
        text = extractQuotedText(byString, "text()='", "'");
        if (text != null) return text;
        // Default: use the locator itself (truncated)
        return byString.length() > 80 ? byString.substring(0, 80) : byString;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Get a summary of self-healing activity for this session.
     */
    public String getStatsSummary() {
        int total = totalFinds.get();
        int retried = retriedFinds.get();
        int healed = healedFinds.get();
        int failed = failedFinds.get();
        long retryTime = totalRetryTimeMs.get();

        if (total == 0) return "[SelfHeal] No element lookups performed.";

        double healRate = total > 0 ? (healed * 100.0 / total) : 0;
        double retryRate = total > 0 ? (retried * 100.0 / total) : 0;

        return String.format(
            "[SelfHeal] Stats: %d total finds | %d retried (%.1f%%) | %d healed (%.1f%%) | %d failed | %dms retry time",
            total, retried, retryRate, healed, healRate, failed, retryTime);
    }

    public int getTotalFinds() { return totalFinds.get(); }
    public int getRetriedFinds() { return retriedFinds.get(); }
    public int getHealedFinds() { return healedFinds.get(); }
    public int getFailedFinds() { return failedFinds.get(); }

    // =========================================================================
    // DELEGATED WebDriver METHODS
    // =========================================================================

    @Override public void get(String url) { delegate.get(url); }
    @Override public String getCurrentUrl() { return delegate.getCurrentUrl(); }
    @Override public String getTitle() { return delegate.getTitle(); }
    @Override public String getPageSource() { return delegate.getPageSource(); }
    @Override public void close() { delegate.close(); }
    @Override public void quit() { delegate.quit(); }
    @Override public Set<String> getWindowHandles() { return delegate.getWindowHandles(); }
    @Override public String getWindowHandle() { return delegate.getWindowHandle(); }
    @Override public TargetLocator switchTo() { return delegate.switchTo(); }
    @Override public Navigation navigate() { return delegate.navigate(); }
    @Override public Options manage() { return delegate.manage(); }

    // =========================================================================
    // DELEGATED JavascriptExecutor METHODS
    // =========================================================================

    @Override
    public Object executeScript(String script, Object... args) {
        if (jsDelegate == null) throw new UnsupportedOperationException("Driver does not support JavaScript execution");
        // Unwrap any SelfHealingElements before passing to JS executor
        Object[] unwrapped = unwrapArgs(args);
        return jsDelegate.executeScript(script, unwrapped);
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        if (jsDelegate == null) throw new UnsupportedOperationException("Driver does not support JavaScript execution");
        Object[] unwrapped = unwrapArgs(args);
        return jsDelegate.executeAsyncScript(script, unwrapped);
    }

    /**
     * Unwrap SelfHealingElement instances back to raw WebElements for JavascriptExecutor.
     * JS executor needs the raw Selenium element, not our wrapper.
     */
    private Object[] unwrapArgs(Object[] args) {
        if (args == null) return null;
        Object[] unwrapped = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SelfHealingElement) {
                unwrapped[i] = ((SelfHealingElement) args[i]).getWrappedElement();
            } else if (args[i] instanceof List) {
                // Handle List<WebElement> arguments
                List<?> list = (List<?>) args[i];
                List<Object> unwrappedList = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof SelfHealingElement) {
                        unwrappedList.add(((SelfHealingElement) item).getWrappedElement());
                    } else {
                        unwrappedList.add(item);
                    }
                }
                unwrapped[i] = unwrappedList;
            } else {
                unwrapped[i] = args[i];
            }
        }
        return unwrapped;
    }

    // =========================================================================
    // DELEGATED TakesScreenshot METHODS
    // =========================================================================

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        if (screenshotDelegate == null) throw new UnsupportedOperationException("Driver does not support screenshots");
        return screenshotDelegate.getScreenshotAs(target);
    }
}
