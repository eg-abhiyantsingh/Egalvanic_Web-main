package com.egalvanic.qa.utils.verify;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BrowserErrorCapture — the missing half of console-error detection.
 *
 * WHY THIS EXISTS
 * ---------------
 * FlakinessPrevention.installConsoleErrorCapture() only overrides
 * {@code console.error}. That misses the three error classes that actually
 * indicate a broken page:
 *   1. Uncaught exceptions          -> window.onerror
 *   2. Unhandled promise rejections -> 'unhandledrejection' event
 *   3. Failed resource loads        -> 'error' event (capture phase) on
 *                                      img/script/link/iframe/embed
 * It also never recorded the SEVERE entries Chrome itself emits.
 *
 * This installer captures ALL of the above into window.__bugHunterErrors and,
 * if the driver was started with goog:loggingPrefs BROWSER=SEVERE, also drains
 * the native Chrome log. {@link #assertNoSevereErrors} turns silent captures
 * into HARD build failures — the framework's #1 missing gate.
 *
 * Usage (install once after every navigation, assert at end of a test):
 *   BrowserErrorCapture.install(driver);
 *   ... exercise the page ...
 *   BrowserErrorCapture.assertNoSevereErrors(driver, "Dashboard load");
 */
public final class BrowserErrorCapture {

    private BrowserErrorCapture() {}

    /** Substrings that are noise rather than real bugs — keep this list tight and reviewed. */
    private static final String[] IGNORED = {
            "ResizeObserver loop",          // benign Chromium warning
            "favicon.ico",                  // missing favicon is not a functional bug
            "beamer",                       // 3rd-party notification widget
            "devrev", "plug"                // 3rd-party chat widget
    };

    private static final String INSTALL_JS =
        "if (window.__bugHunterInstalled) return;" +
        "window.__bugHunterInstalled = true;" +
        "window.__bugHunterErrors = [];" +
        "function __push(type, msg, src){" +
        "  if(!msg) return;" +
        "  window.__bugHunterErrors.push({type:type, message:String(msg), source:String(src||'')});" +
        "  if(window.__bugHunterErrors.length>200) window.__bugHunterErrors.shift();" +
        "}" +
        // 1. Uncaught exceptions
        "window.addEventListener('error', function(e){" +
        "  if(e && e.target && e.target!==window && (e.target.src||e.target.href)){" +
        // 3. Resource load failure (img/script/link/iframe). 'error' bubbles here only in capture phase.
        "    __push('RESOURCE', 'Failed to load: '+(e.target.src||e.target.href), e.target.tagName);" +
        "  } else if(e){" +
        "    __push('JS_ERROR', e.message, (e.filename||'')+':'+(e.lineno||''));" +
        "  }" +
        "}, true);" +
        // 2. Unhandled promise rejections
        "window.addEventListener('unhandledrejection', function(e){" +
        "  var r = e && e.reason;" +
        "  __push('PROMISE', (r && (r.message||r)) , 'unhandledrejection');" +
        "});" +
        // also keep console.error (some libs only log, never throw)
        "var _ce = console.error;" +
        "console.error = function(){" +
        "  __push('CONSOLE', Array.prototype.slice.call(arguments).map(function(a){" +
        "    try{return typeof a==='string'?a:JSON.stringify(a);}catch(_){return String(a);}" +
        "  }).join(' '), 'console.error');" +
        "  return _ce.apply(console, arguments);" +
        "};";

    /** Install all capture hooks. Idempotent per page; re-run after every navigation/reload. */
    public static void install(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(INSTALL_JS);
        } catch (Exception ignored) {
            // page mid-navigation; caller re-installs on next step
        }
    }

    /** A single captured browser-side error. */
    public static final class JsError {
        public final String type;     // JS_ERROR | PROMISE | RESOURCE | CONSOLE | NATIVE_SEVERE
        public final String message;
        public final String source;
        public JsError(String type, String message, String source) {
            this.type = type; this.message = message; this.source = source;
        }
        @Override public String toString() {
            return "[" + type + "] " + message + (source.isEmpty() ? "" : "  (" + source + ")");
        }
    }

    /** Return every captured error (JS + native SEVERE), minus the ignore-list noise. */
    @SuppressWarnings("unchecked")
    public static List<JsError> getErrors(WebDriver driver) {
        List<JsError> out = new ArrayList<>();

        // Injected JS errors
        try {
            Object raw = ((JavascriptExecutor) driver)
                    .executeScript("return window.__bugHunterErrors || [];");
            if (raw instanceof List) {
                for (Object o : (List<Object>) raw) {
                    if (o instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) o;
                        out.add(new JsError(str(m.get("type")), str(m.get("message")), str(m.get("source"))));
                    }
                }
            }
        } catch (Exception ignored) {}

        // Native Chrome SEVERE log (only present if goog:loggingPrefs BROWSER=SEVERE was set)
        try {
            for (LogEntry e : driver.manage().logs().get(LogType.BROWSER)) {
                if (e.getLevel().getName().equals("SEVERE")) {
                    out.add(new JsError("NATIVE_SEVERE", e.getMessage(), "chrome-log"));
                }
            }
        } catch (Exception ignored) {
            // logging prefs not enabled — JS capture above is the fallback
        }

        out.removeIf(BrowserErrorCapture::isNoise);
        return out;
    }

    private static boolean isNoise(JsError e) {
        String low = (e.message + " " + e.source).toLowerCase();
        for (String ig : IGNORED) {
            if (low.contains(ig.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * HARD-FAIL the test if any severe browser error was captured.
     * This is the gate that ~87 of the current test classes are missing.
     *
     * @throws AssertionError listing every captured error (fails the TestNG build)
     */
    public static void assertNoSevereErrors(WebDriver driver, String context) {
        assertNoSevereErrors(driver, context, new String[0]);
    }

    /**
     * Hard-assert no severe console errors, ignoring any whose text contains one of
     * {@code ignoreSubstrings} (3rd-party SDKs + baseline auth/transient noise). Keeps
     * gate signal-quality high: fails on genuine app errors, not on environment noise
     * that fires on every page load.
     */
    public static void assertNoSevereErrors(WebDriver driver, String context, String... ignoreSubstrings) {
        List<JsError> real = new java.util.ArrayList<>();
        for (JsError e : getErrors(driver)) {
            String text = String.valueOf(e).toLowerCase();
            boolean skip = false;
            if (ignoreSubstrings != null) {
                for (String ig : ignoreSubstrings) {
                    if (ig != null && !ig.isEmpty() && text.contains(ig.toLowerCase())) { skip = true; break; }
                }
            }
            if (!skip) real.add(e);
        }
        if (!real.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Browser console reported ").append(real.size())
              .append(" severe error(s) during [").append(context).append("]:\n");
            for (JsError e : real) sb.append("  - ").append(e).append('\n');
            throw new AssertionError(sb.toString());
        }
    }

    /** Clear captured errors — call at the start of a logical step to scope detection. */
    public static void clear(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript("window.__bugHunterErrors = [];");
        } catch (Exception ignored) {}
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
}
