package com.egalvanic.qa.utils.verify;

import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * UIStateValidator — detects blank pages, error banners, and broken layout
 * that "element present" assertions sail straight past.
 *
 * WHY THIS EXISTS
 * ---------------
 * 182 assertions in the suite are isDisplayed()/count>0 style. A page that
 * rendered a white screen, an MUI error Alert, or content clipped off-screen
 * still satisfies those. This validator inspects the RENDERED state.
 *
 * Checks:
 *   - BLANK    : almost no visible text and no meaningful content nodes
 *   - ERROR    : visible MUI Alert[severity=error] or known crash copy
 *   - CLIPPED  : critical content rendered with zero box / off the viewport
 *   - MISSING  : a required selector you pass in is absent or invisible
 */
public final class UIStateValidator {

    private UIStateValidator() {}

    public static final class UIStateReport {
        public final List<String> problems = new ArrayList<>();
        public String screenshotPath;
        public boolean ok() { return problems.isEmpty(); }
        @Override public String toString() { return String.join("; ", problems); }
    }

    /**
     * Validate the current page.
     * @param requiredCss optional CSS selectors that MUST be present & visible
     *                    (e.g. ".MuiDataGrid-root" on a grid page). May be empty.
     */
    public static UIStateReport validate(WebDriver driver, String context, String... requiredCss) {
        UIStateReport rep = new UIStateReport();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // BLANK detection
        try {
            Object info = js.executeScript(
                "var body=document.body; if(!body) return JSON.stringify({text:0,nodes:0});" +
                "var text=(body.innerText||'').trim().length;" +
                // count visible, non-script structural nodes
                "var vis=0; document.querySelectorAll('main *,#root *,#__next *').forEach(function(e){" +
                "  var r=e.getBoundingClientRect(); if(r.width>1&&r.height>1) vis++; });" +
                "return JSON.stringify({text:text,nodes:vis});");
            String s = String.valueOf(info);
            int text = intField(s, "text");
            int nodes = intField(s, "nodes");
            if (text < 5 && nodes < 5) {
                rep.problems.add("BLANK page (visible text=" + text + " chars, visible nodes=" + nodes + ")");
            }
        } catch (Exception e) {
            rep.problems.add("BLANK probe failed: " + e.getMessage());
        }

        // ERROR banner detection.
        // NOTE: MUI sets role="alert" on EVERY Alert severity (info/warning/success/error),
        // so we must NOT flag a bare role="alert" — a benign empty-state like
        // "No SLD selected. Please select a site/SLD" is an info Alert, not a bug.
        // Only error-SEVERITY MUI alerts, or role=alert text with error keywords, count.
        try {
            Object err = js.executeScript(
                "var hits=[];" +
                "document.querySelectorAll('.MuiAlert-standardError,.MuiAlert-filledError,.MuiAlert-outlinedError').forEach(function(a){" +
                "  if(a.offsetParent!==null){var t=(a.innerText||'').trim(); if(t) hits.push(t.slice(0,160));}});" +
                "document.querySelectorAll('[role=\"alert\"]').forEach(function(a){" +
                "  if(a.offsetParent===null) return;" +
                "  if(((a.className||'')+'').indexOf('Error')>=0) return;" +   // MUI error already captured above
                "  var t=(a.innerText||'').trim(); if(!t) return;" +
                "  if(/error|failed|unable|went wrong|cannot|exception|crash|denied|forbidden/i.test(t)) hits.push(t.slice(0,160));" +
                "});" +
                "var body=(document.body && document.body.innerText)||'';" +
                "['Application Error','We encountered an error','Something went wrong','Unexpected error','Cannot read properties']" +
                "  .forEach(function(p){ if(body.indexOf(p)>=0) hits.push('crash-copy: '+p); });" +
                "return hits.join(' || ');");
            String e = String.valueOf(err);
            if (e != null && !e.isEmpty() && !"null".equals(e)) {
                rep.problems.add("ERROR banner/crash text visible: " + e);
            }
        } catch (Exception ignored) {}

        // CLIPPED / off-viewport critical content
        try {
            Object clip = js.executeScript(
                "var vw=window.innerWidth, n=0;" +
                "document.querySelectorAll('main,[role=\"main\"],.MuiDataGrid-root,table').forEach(function(e){" +
                "  var r=e.getBoundingClientRect();" +
                "  if(r.width>0 && (r.right<0 || r.left>vw)) n++;" +    // pushed entirely off-screen
                "});" +
                "return n;");
            long off = clip instanceof Number ? ((Number) clip).longValue() : 0;
            if (off > 0) rep.problems.add("CLIPPED: " + off + " main content block(s) rendered off-viewport");
        } catch (Exception ignored) {}

        // MISSING required content
        for (String css : requiredCss) {
            if (css == null || css.isEmpty()) continue;
            try {
                Boolean present = (Boolean) js.executeScript(
                    "var e=document.querySelector(arguments[0]);" +
                    "return !!(e && e.offsetParent!==null && e.getBoundingClientRect().height>0);", css);
                if (!Boolean.TRUE.equals(present)) {
                    rep.problems.add("MISSING required content: " + css);
                }
            } catch (Exception ex) {
                rep.problems.add("MISSING probe failed for " + css + ": " + ex.getMessage());
            }
        }

        if (!rep.ok()) {
            try { rep.screenshotPath = ScreenshotUtil.captureScreenshot("UISTATE_" + context.replaceAll("[^a-zA-Z0-9_-]", "_")); }
            catch (Exception ignored) {}
        }
        return rep;
    }

    /** Hard gate. */
    public static void assertHealthy(WebDriver driver, String context, String... requiredCss) {
        UIStateReport r = validate(driver, context, requiredCss);
        if (!r.ok()) {
            String shot = r.screenshotPath != null ? "\n  screenshot: " + r.screenshotPath : "";
            throw new AssertionError("Broken UI state on [" + context + "]: " + r + shot);
        }
    }

    private static int intField(String json, String key) {
        try {
            int i = json.indexOf("\"" + key + "\":");
            if (i < 0) return 0;
            int start = i + key.length() + 3;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) { return 0; }
    }
}
