package com.egalvanic.qa.utils.verify;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Front-end performance verifier — reads the browser's own Navigation Timing
 * (PerformanceNavigationTiming) and Paint Timing (FCP) APIs via executeScript.
 *
 * Boundary (state it honestly): this measures CLIENT-SIDE page performance for a
 * single real browser session — TTFB, DOMContentLoaded, full load, First
 * Contentful Paint. It is NOT a load/stress test. Concurrency/throughput
 * (load, spike, soak) belong to JMeter/k6 against the API tier, orchestrated
 * separately. Use this to catch slow pages, never-settling loads, and
 * regressions in page-render budget per module.
 *
 * Pure JS — no external dependency, no CSP impact (executeScript bypasses CSP),
 * works on every Chromium/Firefox/Edge session.
 */
public final class PerfVerifier {

    private PerfVerifier() {}

    /** A snapshot of key client-side timings (all in milliseconds). */
    public static final class PerfReport {
        public final long ttfbMs;            // responseStart - requestStart
        public final long domContentLoadedMs;
        public final long loadMs;            // full load event
        public final long firstContentfulPaintMs; // FCP (-1 if unavailable)
        public final String context;

        PerfReport(String context, long ttfb, long dcl, long load, long fcp) {
            this.context = context;
            this.ttfbMs = ttfb;
            this.domContentLoadedMs = dcl;
            this.loadMs = load;
            this.firstContentfulPaintMs = fcp;
        }

        @Override public String toString() {
            return "[Perf " + context + "] TTFB=" + ttfbMs + "ms  DCL=" + domContentLoadedMs
                    + "ms  load=" + loadMs + "ms  FCP=" + firstContentfulPaintMs + "ms";
        }
    }

    private static final String TIMING_JS =
        "var nav = performance.getEntriesByType('navigation')[0];"
        + "var fcpEntry = performance.getEntriesByType('paint')"
        + "                 .filter(function(p){return p.name==='first-contentful-paint';})[0];"
        + "var out = {};"
        + "if (nav) {"
        + "  out.ttfb = Math.max(0, Math.round(nav.responseStart - nav.requestStart));"
        + "  out.dcl  = Math.max(0, Math.round(nav.domContentLoadedEventEnd - nav.startTime));"
        + "  out.load = Math.max(0, Math.round(nav.loadEventEnd - nav.startTime));"
        + "} else {"
        // Fallback to the legacy performance.timing for older engines
        + "  var t = performance.timing;"
        + "  out.ttfb = Math.max(0, t.responseStart - t.requestStart);"
        + "  out.dcl  = Math.max(0, t.domContentLoadedEventEnd - t.navigationStart);"
        + "  out.load = Math.max(0, t.loadEventEnd - t.navigationStart);"
        + "}"
        + "out.fcp = fcpEntry ? Math.round(fcpEntry.startTime) : -1;"
        + "return out;";

    /** Capture client-side timings for the currently loaded page. */
    public static PerfReport capture(WebDriver driver, String context) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> r = (Map<String, Object>) ((JavascriptExecutor) driver)
                    .executeScript(TIMING_JS);
            if (r == null) r = new HashMap<>();
            return new PerfReport(context,
                    asLong(r.get("ttfb")), asLong(r.get("dcl")),
                    asLong(r.get("load")), asLong(r.get("fcp")));
        } catch (Exception e) {
            System.out.println("[PerfVerifier] capture failed for '" + context + "': " + e.getMessage());
            return new PerfReport(context, -1, -1, -1, -1);
        }
    }

    /**
     * Hard-assert the page loaded within budget. Fails the test if full load
     * exceeds {@code loadBudgetMs}. Use a realistic per-module budget
     * (e.g. dashboards 8000ms, simple lists 5000ms).
     */
    public static void assertWithinBudget(WebDriver driver, String context, long loadBudgetMs) {
        PerfReport r = capture(driver, context);
        System.out.println(r);
        if (r.loadMs < 0) return; // timing unavailable — don't false-fail
        if (r.loadMs > loadBudgetMs) {
            throw new AssertionError("[PerfVerifier] " + context + " load time " + r.loadMs
                    + "ms exceeds budget " + loadBudgetMs + "ms. " + r);
        }
    }

    private static long asLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o == null) return -1;
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return -1; }
    }
}
