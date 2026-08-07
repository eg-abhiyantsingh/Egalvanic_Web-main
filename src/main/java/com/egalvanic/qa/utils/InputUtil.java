package com.egalvanic.qa.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Reliable text-field clearing for this React/MUI app.
 *
 * WHY THIS EXISTS — a macOS-specific bug that manufactured "product defects":
 *
 * The idiom used across the suite was
 * <pre>
 *     el.sendKeys(Keys.chord(Keys.CONTROL, "a"));   // intended: select all
 *     el.sendKeys(Keys.DELETE);                     // intended: delete the selection
 *     el.sendKeys(newText);
 * </pre>
 * On Windows/Linux that clears the field. On <b>macOS it does not</b>: select-all is
 * <b>COMMAND</b>+A, not CONTROL+A. Nothing gets selected, the subsequent forward {@code DELETE} is a
 * no-op at end-of-text, and the new text is simply <b>CONCATENATED onto whatever was already in the
 * box</b>.
 *
 * Measured on this machine (macOS, Chrome 151) against a fixture pre-filled with {@code "SmokeTask"}:
 * <pre>
 *     OLD (CONTROL+A, DELETE, type "AutoTest") -> "SmokeTaskAutoTest"   cleared: false
 *     NEW (clearReactInput,    type "AutoTest") -> "AutoTest"           cleared: true
 * </pre>
 *
 * Concrete consequence: {@code TaskTestNG.TC_SF_002} searched for {@code "AutoTest"} while the box
 * still held a previous test's term, so it actually queried {@code "SmokeTaskAutoTest"}, got 0 rows,
 * and reported <i>"SEARCH DEFECT: 'AutoTest' is present in the unfiltered task grid, but searching
 * for it returned 0 rows"</i>. Verified 2026-08-08 that the search itself is FINE — typing
 * {@code AutoTest} by hand returns 4 matching rows in ~3s. The defect was ours, and it is invisible
 * on CI-style Linux runs while reproducing on the maintainer's Mac.
 *
 * The clear below is platform-independent AND React-safe. It sets {@code value} through the native
 * {@code HTMLInputElement} setter and dispatches {@code input}, which is what React's synthetic
 * event system listens for — a plain {@code el.clear()} can leave React's internal state holding the
 * old value, so the UI re-renders the stale text back.
 */
public final class InputUtil {

    private InputUtil() {}

    /** True when running on macOS, where select-all is COMMAND+A rather than CONTROL+A. */
    public static final boolean IS_MAC =
            System.getProperty("os.name", "").toLowerCase().contains("mac");

    /** The correct select-all modifier for the current platform. */
    public static Keys selectAllModifier() {
        return IS_MAC ? Keys.COMMAND : Keys.CONTROL;
    }

    /**
     * Clear a React-controlled input so the framework actually sees it as empty.
     * Silent no-op if the element has gone stale — callers re-find and retry anyway.
     */
    public static void clearReactInput(WebDriver driver, WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var setter = Object.getOwnPropertyDescriptor("
                    + "  window.HTMLInputElement.prototype, 'value').set;"
                    + "setter.call(arguments[0], '');"
                    + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                    + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    el);
        } catch (Exception jsFailed) {
            // Fall back to keyboard, with the PLATFORM-CORRECT modifier.
            try {
                el.sendKeys(Keys.chord(selectAllModifier(), "a"));
                el.sendKeys(Keys.DELETE);
            } catch (Exception ignored) { /* stale — caller retries */ }
        }
    }

    /**
     * Clear then type, verifying the field really holds {@code text} afterwards.
     *
     * @return true if the committed value equals {@code text} (trimmed)
     */
    public static boolean setText(WebDriver driver, WebElement el, String text) {
        clearReactInput(driver, el);
        try {
            if (text != null && !text.isEmpty()) el.sendKeys(text);
            String actual = el.getAttribute("value");
            return actual != null && actual.trim().equals(text == null ? "" : text.trim());
        } catch (Exception e) {
            return false;
        }
    }
}
