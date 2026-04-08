package com.egalvanic.qa.utils.ai;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Flakiness Prevention — proactive stability utilities for Selenium tests.
 *
 * While SelfHealingDriver/Element RECOVER from failures, FlakinessPrevention
 * PREVENTS them from happening. Three core capabilities:
 *
 * 1. React-Aware Waits — detect React render cycles, not just DOM presence
 * 2. Page Readiness — wait for network idle + no pending React state updates
 * 3. Stable Element Waits — wait until an element stops moving/resizing (animations)
 *
 * Designed for eGalvanic's React MUI stack where the most common flake causes are:
 *   - Clicking before MUI animation completes (drawer slide-in, accordion expand)
 *   - Reading text before React async state update renders new data
 *   - Interacting with elements while SPA navigation is in progress
 *
 * Usage in test code:
 *   FlakinessPrevention.waitForReactIdle(driver);
 *   FlakinessPrevention.waitForStableElement(driver, element);
 *   FlakinessPrevention.waitForPageReady(driver);
 */
public class FlakinessPrevention {

    // Default timeouts
    private static final int DEFAULT_TIMEOUT_SECONDS = 15;
    private static final int STABILITY_CHECK_INTERVAL_MS = 200;
    private static final int STABILITY_THRESHOLD_CHECKS = 3;

    // Statistics
    private static final AtomicInteger reactWaits = new AtomicInteger(0);
    private static final AtomicInteger stabilityWaits = new AtomicInteger(0);
    private static final AtomicInteger networkWaits = new AtomicInteger(0);
    private static final AtomicInteger preventedFlakes = new AtomicInteger(0);

    // =========================================================================
    // 1. REACT-AWARE WAITS
    // =========================================================================

    /**
     * Wait for React to finish all pending state updates and re-renders.
     *
     * Detects React by checking for the __REACT_DEVTOOLS_GLOBAL_HOOK__ or
     * _reactRootContainer on the root element. Waits until no pending
     * setState callbacks or useEffect hooks are executing.
     *
     * For non-React pages, falls back to document.readyState check.
     */
    public static void waitForReactIdle(WebDriver driver) {
        waitForReactIdle(driver, DEFAULT_TIMEOUT_SECONDS);
    }

    public static void waitForReactIdle(WebDriver driver, int timeoutSeconds) {
        reactWaits.incrementAndGet();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        // Install a MutationObserver on first call to track DOM changes
        installDomStabilityObserver(js);

        while (System.currentTimeMillis() < deadline) {
            try {
                Boolean isIdle = (Boolean) js.executeScript(
                    // Check 1: document.readyState must be 'complete'
                    "if (document.readyState !== 'complete') return false;"
                    // Check 2: No active XHR/fetch requests
                    + "if (window.__pendingRequests && window.__pendingRequests > 0) return false;"
                    // Check 3: No recent DOM mutations (our observer tracks this)
                    + "if (window.__domMutationCount && window.__domMutationCount > 0) {"
                    + "  var count = window.__domMutationCount;"
                    + "  window.__domMutationCount = 0;"
                    + "  return false;"
                    + "}"
                    // Check 4: No pending React updates (React 18 concurrent mode)
                    + "var root = document.getElementById('root') || document.getElementById('__next');"
                    + "if (root && root._reactRootContainer) {"
                    + "  var fiber = root._reactRootContainer._internalRoot || root._reactRootContainer;"
                    + "  if (fiber && fiber.current && fiber.current.memoizedState) {"
                    + "    var state = fiber.current.memoizedState;"
                    + "    if (state && state.element && state.element.pendingProps) return false;"
                    + "  }"
                    + "}"
                    // Check 5: No active MUI transitions (Slide, Fade, Grow, Collapse)
                    + "var transitions = document.querySelectorAll("
                    + "  '[class*=\"MuiCollapse-entering\"],"
                    + "   [class*=\"MuiSlide-entering\"],"
                    + "   [class*=\"MuiFade-entering\"],"
                    + "   [class*=\"MuiGrow-entering\"],"
                    + "   [style*=\"transition\"]'"
                    + ");"
                    + "for (var t of transitions) {"
                    + "  var style = window.getComputedStyle(t);"
                    + "  if (style.transitionDuration && style.transitionDuration !== '0s') {"
                    + "    return false;"
                    + "  }"
                    + "}"
                    + "return true;");

                if (Boolean.TRUE.equals(isIdle)) {
                    return;
                }
            } catch (Exception e) {
                // JS execution failed (page navigating, etc.) — retry
            }
            sleep(STABILITY_CHECK_INTERVAL_MS);
        }
        // Timeout reached — continue anyway (don't block the test forever)
    }

    /**
     * Install a MutationObserver that counts DOM changes.
     * Used by waitForReactIdle to detect when React re-renders stop.
     */
    private static void installDomStabilityObserver(JavascriptExecutor js) {
        try {
            js.executeScript(
                "if (window.__domObserverInstalled) return;"
                + "window.__domObserverInstalled = true;"
                + "window.__domMutationCount = 0;"
                + "var observer = new MutationObserver(function(mutations) {"
                + "  window.__domMutationCount += mutations.length;"
                + "});"
                + "observer.observe(document.body, {"
                + "  childList: true, subtree: true, attributes: true"
                + "});"
            );
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // 2. NETWORK IDLE DETECTION
    // =========================================================================

    /**
     * Install XHR/fetch interceptors to track pending network requests.
     * Call this once after page load to enable network-aware waits.
     */
    public static void installNetworkInterceptor(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            js.executeScript(
                "if (window.__networkInterceptorInstalled) return;"
                + "window.__networkInterceptorInstalled = true;"
                + "window.__pendingRequests = 0;"

                // Intercept XMLHttpRequest
                + "var origXhrOpen = XMLHttpRequest.prototype.open;"
                + "var origXhrSend = XMLHttpRequest.prototype.send;"
                + "XMLHttpRequest.prototype.open = function() {"
                + "  this.__tracked = true;"
                + "  return origXhrOpen.apply(this, arguments);"
                + "};"
                + "XMLHttpRequest.prototype.send = function() {"
                + "  if (this.__tracked) {"
                + "    window.__pendingRequests++;"
                + "    this.addEventListener('loadend', function() {"
                + "      window.__pendingRequests = Math.max(0, window.__pendingRequests - 1);"
                + "    });"
                + "  }"
                + "  return origXhrSend.apply(this, arguments);"
                + "};"

                // Intercept fetch
                + "var origFetch = window.fetch;"
                + "window.fetch = function() {"
                + "  window.__pendingRequests++;"
                + "  return origFetch.apply(this, arguments).finally(function() {"
                + "    window.__pendingRequests = Math.max(0, window.__pendingRequests - 1);"
                + "  });"
                + "};"
            );
        } catch (Exception ignored) {}
    }

    /**
     * Wait until all pending XHR/fetch requests complete.
     */
    public static void waitForNetworkIdle(WebDriver driver) {
        waitForNetworkIdle(driver, DEFAULT_TIMEOUT_SECONDS);
    }

    public static void waitForNetworkIdle(WebDriver driver, int timeoutSeconds) {
        networkWaits.incrementAndGet();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        int stableCount = 0;
        while (System.currentTimeMillis() < deadline) {
            try {
                Long pending = (Long) js.executeScript(
                    "return window.__pendingRequests || 0;");
                if (pending != null && pending == 0) {
                    stableCount++;
                    if (stableCount >= STABILITY_THRESHOLD_CHECKS) {
                        return; // Network truly idle for multiple consecutive checks
                    }
                } else {
                    stableCount = 0;
                }
            } catch (Exception e) {
                stableCount = 0;
            }
            sleep(STABILITY_CHECK_INTERVAL_MS);
        }
    }

    // =========================================================================
    // 3. STABLE ELEMENT WAITS (animation-aware)
    // =========================================================================

    /**
     * Wait until an element's position and size stop changing (animation complete).
     * Critical for MUI drawers (slide-in), accordions (expand), and dialogs (fade-in).
     *
     * @param driver  WebDriver instance
     * @param element the element to wait for
     * @return the same element (for chaining)
     */
    public static WebElement waitForStableElement(WebDriver driver, WebElement element) {
        return waitForStableElement(driver, element, DEFAULT_TIMEOUT_SECONDS);
    }

    public static WebElement waitForStableElement(WebDriver driver, WebElement element, int timeoutSeconds) {
        stabilityWaits.incrementAndGet();
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        int prevX = -1, prevY = -1, prevW = -1, prevH = -1;
        int stableCount = 0;

        while (System.currentTimeMillis() < deadline) {
            try {
                // Unwrap if SelfHealingElement
                WebElement raw = element;
                if (element instanceof SelfHealingElement) {
                    raw = ((SelfHealingElement) element).getWrappedElement();
                }

                org.openqa.selenium.Rectangle rect = raw.getRect();
                int x = rect.getX();
                int y = rect.getY();
                int w = rect.getWidth();
                int h = rect.getHeight();

                if (x == prevX && y == prevY && w == prevW && h == prevH) {
                    stableCount++;
                    if (stableCount >= STABILITY_THRESHOLD_CHECKS) {
                        return element; // Element is stable
                    }
                } else {
                    stableCount = 0;
                    prevX = x; prevY = y; prevW = w; prevH = h;
                }
            } catch (Exception e) {
                stableCount = 0;
            }
            sleep(STABILITY_CHECK_INTERVAL_MS);
        }

        preventedFlakes.incrementAndGet();
        return element; // Return anyway after timeout
    }

    /**
     * Wait until an element's text content stops changing.
     * Useful for async-loaded data displays (tables, counters, labels).
     */
    public static String waitForStableText(WebDriver driver, WebElement element) {
        return waitForStableText(driver, element, DEFAULT_TIMEOUT_SECONDS);
    }

    public static String waitForStableText(WebDriver driver, WebElement element, int timeoutSeconds) {
        stabilityWaits.incrementAndGet();
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        String prevText = null;
        int stableCount = 0;

        while (System.currentTimeMillis() < deadline) {
            try {
                String currentText = element.getText();
                if (currentText != null && currentText.equals(prevText)) {
                    stableCount++;
                    if (stableCount >= STABILITY_THRESHOLD_CHECKS) {
                        return currentText;
                    }
                } else {
                    stableCount = 0;
                    prevText = currentText;
                }
            } catch (Exception e) {
                stableCount = 0;
            }
            sleep(STABILITY_CHECK_INTERVAL_MS);
        }

        preventedFlakes.incrementAndGet();
        return prevText;
    }

    // =========================================================================
    // 4. COMPOSITE READY CHECKS
    // =========================================================================

    /**
     * Full page readiness check: document complete + network idle + React idle + no animations.
     * Call this after any navigation or major UI state change.
     */
    public static void waitForPageReady(WebDriver driver) {
        waitForPageReady(driver, DEFAULT_TIMEOUT_SECONDS);
    }

    public static void waitForPageReady(WebDriver driver, int timeoutSeconds) {
        // Install interceptors if not already
        installNetworkInterceptor(driver);

        // Wait for document.readyState + network
        waitForNetworkIdle(driver, timeoutSeconds);

        // Wait for React renders to settle
        waitForReactIdle(driver, timeoutSeconds);
    }

    /**
     * Wait for MUI Drawer to finish its slide-in animation and be fully interactive.
     * Detects the drawer paper element and waits for its transform to stabilize.
     */
    public static boolean waitForMuiDrawerReady(WebDriver driver) {
        return waitForMuiDrawerReady(driver, DEFAULT_TIMEOUT_SECONDS);
    }

    public static boolean waitForMuiDrawerReady(WebDriver driver, int timeoutSeconds) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            try {
                Boolean ready = (Boolean) js.executeScript(
                    "var paper = document.querySelector('.MuiDrawer-paper');"
                    + "if (!paper) return false;"
                    + "var style = window.getComputedStyle(paper);"
                    // Drawer is ready when transform is none or identity matrix
                    + "var transform = style.transform || style.webkitTransform || '';"
                    + "var isSettled = transform === 'none' || transform === 'matrix(1, 0, 0, 1, 0, 0)'"
                    + "  || transform === '';"
                    // Also check visibility and dimensions
                    + "var rect = paper.getBoundingClientRect();"
                    + "var isVisible = rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden';"
                    + "return isSettled && isVisible;");

                if (Boolean.TRUE.equals(ready)) {
                    // Extra stability check — wait one more interval to confirm
                    sleep(STABILITY_CHECK_INTERVAL_MS);
                    return true;
                }
            } catch (Exception e) {
                // Page may be transitioning
            }
            sleep(STABILITY_CHECK_INTERVAL_MS);
        }
        return false;
    }

    /**
     * Wait for MUI DataGrid to finish loading (spinner gone, rows rendered).
     */
    public static boolean waitForDataGridReady(WebDriver driver) {
        return waitForDataGridReady(driver, DEFAULT_TIMEOUT_SECONDS);
    }

    public static boolean waitForDataGridReady(WebDriver driver, int timeoutSeconds) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            try {
                Boolean ready = (Boolean) js.executeScript(
                    "var grid = document.querySelector('.MuiDataGrid-root');"
                    + "if (!grid) return false;"
                    // Check loading overlay is gone
                    + "var loading = grid.querySelector('.MuiDataGrid-overlay');"
                    + "if (loading && loading.offsetParent !== null) return false;"
                    // Check spinner is gone
                    + "var spinner = grid.querySelector('.MuiCircularProgress-root');"
                    + "if (spinner) return false;"
                    // Check at least one row exists
                    + "var rows = grid.querySelectorAll('.MuiDataGrid-row');"
                    + "return rows.length > 0;");

                if (Boolean.TRUE.equals(ready)) {
                    return true;
                }
            } catch (Exception e) {
                // Grid may not be mounted yet
            }
            sleep(STABILITY_CHECK_INTERVAL_MS);
        }
        return false;
    }

    /**
     * Wait for MUI Accordion to finish expand/collapse animation.
     */
    public static boolean waitForAccordionReady(WebDriver driver, WebElement accordion) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long deadline = System.currentTimeMillis() + (DEFAULT_TIMEOUT_SECONDS * 1000L);

        while (System.currentTimeMillis() < deadline) {
            try {
                WebElement raw = accordion;
                if (accordion instanceof SelfHealingElement) {
                    raw = ((SelfHealingElement) accordion).getWrappedElement();
                }
                Boolean ready = (Boolean) js.executeScript(
                    "var el = arguments[0];"
                    + "var collapse = el.querySelector('.MuiCollapse-root');"
                    + "if (!collapse) return true;" // No collapse wrapper = no animation
                    + "var style = window.getComputedStyle(collapse);"
                    // Collapse is done when height is not 'auto' being animated
                    + "return !collapse.classList.contains('MuiCollapse-entering')"
                    + "  && !collapse.classList.contains('MuiCollapse-exiting');",
                    raw);

                if (Boolean.TRUE.equals(ready)) {
                    return true;
                }
            } catch (Exception e) {
                // Element may be stale
            }
            sleep(STABILITY_CHECK_INTERVAL_MS);
        }
        return false;
    }

    // =========================================================================
    // 5. CONSOLE ERROR MONITORING
    // =========================================================================

    /**
     * Install a console.error interceptor to capture JS errors.
     * Call once after page load. Captured errors are available via getConsoleErrors().
     */
    public static void installConsoleErrorCapture(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            js.executeScript(
                "if (window.__consoleErrorsCaptured) return;"
                + "window.__consoleErrorsCaptured = true;"
                + "window.__capturedErrors = [];"
                + "var origError = console.error;"
                + "console.error = function() {"
                + "  var msg = Array.from(arguments).map(function(a) {"
                + "    return typeof a === 'string' ? a : JSON.stringify(a);"
                + "  }).join(' ');"
                + "  window.__capturedErrors.push(msg);"
                + "  if (window.__capturedErrors.length > 50) window.__capturedErrors.shift();"
                + "  return origError.apply(console, arguments);"
                + "};"
            );
        } catch (Exception ignored) {}
    }

    /**
     * Get all console errors captured since last clear.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getConsoleErrors(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            return (List<String>) js.executeScript(
                "return window.__capturedErrors || [];");
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Clear captured console errors.
     */
    public static void clearConsoleErrors(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.__capturedErrors = [];");
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    public static String getStatsSummary() {
        int react = reactWaits.get();
        int stability = stabilityWaits.get();
        int network = networkWaits.get();
        int prevented = preventedFlakes.get();

        if (react == 0 && stability == 0 && network == 0) {
            return "[FlakePrev] No flakiness prevention waits invoked.";
        }
        return String.format(
            "[FlakePrev] Stats: %d React waits | %d stability waits | %d network waits | %d timeout warnings",
            react, stability, network, prevented);
    }

    public static void resetStats() {
        reactWaits.set(0);
        stabilityWaits.set(0);
        networkWaits.set(0);
        preventedFlakes.set(0);
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
