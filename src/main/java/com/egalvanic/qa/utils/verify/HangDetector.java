package com.egalvanic.qa.utils.verify;

import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * HangDetector — proves a page actually became interactive instead of
 * freezing on an infinite spinner or a never-resolving network request.
 *
 * WHY THIS EXISTS
 * ---------------
 * The suite uses EAGER page-load strategy + a 60s pageLoadTimeout, and
 * FlakinessPrevention.waitForPageReady() simply RETURNS after its timeout
 * ("continue anyway — don't block the test forever"). So a page that never
 * finishes loading is treated as ready and the test proceeds green. This
 * verifier instead makes a hang a HARD FAILURE, with console logs + screenshot.
 *
 * A page is "hung" if, after the budget, ANY of these hold:
 *   - document.readyState != complete
 *   - window.__pendingRequests > 0 (FlakinessPrevention's XHR/fetch counter)
 *   - a loading spinner is still visible (MUI CircularProgress / role=progressbar)
 *   - the DOM is still mutating (never settles)
 */
public final class HangDetector {

    private HangDetector() {}

    private static final long POLL_MS = 250;

    public static final class HangReport {
        public final boolean hung;
        public final String reason;
        public final String screenshotPath;
        public final List<BrowserErrorCapture.JsError> consoleErrors;
        HangReport(boolean hung, String reason, String shot, List<BrowserErrorCapture.JsError> errs) {
            this.hung = hung; this.reason = reason; this.screenshotPath = shot; this.consoleErrors = errs;
        }
    }

    /**
     * Wait up to {@code budgetSeconds} for the page to become genuinely
     * responsive. Returns a clean report if it settles; otherwise captures
     * evidence. Does NOT throw — use {@link #assertResponsive} for a hard gate.
     */
    public static HangReport check(WebDriver driver, String context, int budgetSeconds) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long deadline = System.currentTimeMillis() + budgetSeconds * 1000L;
        String lastReason = "unknown";

        // install a cheap DOM-quiet detector
        try {
            js.executeScript(
                "if(!window.__hangObs){window.__hangMut=0;window.__hangObs=new MutationObserver(" +
                "function(m){window.__hangMut+=m.length;});" +
                "window.__hangObs.observe(document.documentElement,{childList:true,subtree:true,attributes:true});}");
        } catch (Exception ignored) {}

        int quietStreak = 0;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object state = js.executeScript(
                    "var r={};" +
                    "r.ready = document.readyState;" +
                    "r.pending = window.__pendingRequests || 0;" +
                    // visible spinner?
                    "var sp = document.querySelector('.MuiCircularProgress-root,[role=\"progressbar\"],.MuiLinearProgress-root,.loading-spinner');" +
                    "r.spinner = !!(sp && sp.offsetParent !== null);" +
                    "r.mut = window.__hangMut||0; window.__hangMut=0;" +
                    "return JSON.stringify(r);");
                String s = String.valueOf(state);
                boolean ready   = s.contains("\"ready\":\"complete\"");
                boolean pending = !s.contains("\"pending\":0");
                boolean spinner = s.contains("\"spinner\":true");
                boolean mutating = !s.contains("\"mut\":0");

                if (!ready)        lastReason = "document.readyState != complete";
                else if (pending)  lastReason = "pending XHR/fetch requests did not resolve";
                else if (spinner)  lastReason = "loading spinner still visible";
                else if (mutating) lastReason = "DOM still mutating (never settled)";

                if (ready && !pending && !spinner && !mutating) {
                    quietStreak++;
                    if (quietStreak >= 3) {
                        return new HangReport(false, "responsive", null, null);
                    }
                } else {
                    quietStreak = 0;
                }
            } catch (Exception e) {
                lastReason = "JS probe failed (page navigating/crashed): " + e.getMessage();
            }
            sleep(POLL_MS);
        }

        // Hung — capture evidence
        String shot = safeShot("HANG_" + sanitize(context));
        List<BrowserErrorCapture.JsError> errs = BrowserErrorCapture.getErrors(driver);
        ExtentReportManager.logWarning("HANG detected during [" + context + "]: " + lastReason);
        return new HangReport(true, lastReason, shot, errs);
    }

    /** Hard gate: fail the test (with evidence) if the page hangs. */
    public static void assertResponsive(WebDriver driver, String context, int budgetSeconds) {
        HangReport r = check(driver, context, budgetSeconds);
        if (r.hung) {
            StringBuilder sb = new StringBuilder("Page HUNG during [").append(context)
                    .append("] after ").append(budgetSeconds).append("s: ").append(r.reason);
            if (r.screenshotPath != null) sb.append("\n  screenshot: ").append(r.screenshotPath);
            if (r.consoleErrors != null && !r.consoleErrors.isEmpty()) {
                sb.append("\n  console errors:");
                for (BrowserErrorCapture.JsError e : r.consoleErrors) sb.append("\n    - ").append(e);
            }
            throw new AssertionError(sb.toString());
        }
    }

    private static String safeShot(String name) {
        try { return ScreenshotUtil.captureScreenshot(name); } catch (Exception e) { return null; }
    }
    private static String sanitize(String s) { return s == null ? "ctx" : s.replaceAll("[^a-zA-Z0-9_-]", "_"); }
    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
