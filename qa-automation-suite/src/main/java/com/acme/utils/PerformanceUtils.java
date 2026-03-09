package com.acme.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

public class PerformanceUtils {
    
    /**
     * Get page load timing metrics
     */
    public static Map<String, Object> getPageLoadMetrics(WebDriver driver) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Navigation Timing API metrics
            String script = "var timing = window.performance.timing; " +
                    "return {" +
                    "navigationStart: timing.navigationStart," +
                    "unloadEventStart: timing.unloadEventStart," +
                    "unloadEventEnd: timing.unloadEventEnd," +
                    "redirectStart: timing.redirectStart," +
                    "redirectEnd: timing.redirectEnd," +
                    "fetchStart: timing.fetchStart," +
                    "domainLookupStart: timing.domainLookupStart," +
                    "domainLookupEnd: timing.domainLookupEnd," +
                    "connectStart: timing.connectStart," +
                    "connectEnd: timing.connectEnd," +
                    "secureConnectionStart: timing.secureConnectionStart," +
                    "requestStart: timing.requestStart," +
                    "responseStart: timing.responseStart," +
                    "responseEnd: timing.responseEnd," +
                    "domLoading: timing.domLoading," +
                    "domInteractive: timing.domInteractive," +
                    "domContentLoadedEventStart: timing.domContentLoadedEventStart," +
                    "domContentLoadedEventEnd: timing.domContentLoadedEventEnd," +
                    "domComplete: timing.domComplete," +
                    "loadEventStart: timing.loadEventStart," +
                    "loadEventEnd: timing.loadEventEnd" +
                    "};";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> timing = (Map<String, Object>) js.executeScript(script);
            
            // Calculate metrics
            long navigationStart = (Long) timing.get("navigationStart");
            long fetchStart = (Long) timing.get("fetchStart");
            long responseStart = (Long) timing.get("responseStart");
            long responseEnd = (Long) timing.get("responseEnd");
            long domInteractive = (Long) timing.get("domInteractive");
            long domComplete = (Long) timing.get("domComplete");
            long loadEventEnd = (Long) timing.get("loadEventEnd");
            
            // Time to First Byte (TTFB)
            metrics.put("TTFB", responseStart - fetchStart);
            
            // DOM Loading Time
            metrics.put("DOMLoadingTime", domInteractive - navigationStart);
            
            // Page Load Time
            metrics.put("PageLoadTime", loadEventEnd - navigationStart);
            
            // DOM Complete Time
            metrics.put("DOMCompleteTime", domComplete - navigationStart);
            
            // Response Time
            metrics.put("ResponseTime", responseEnd - responseStart);
            
        } catch (Exception e) {
            System.err.println("Failed to get page load metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * Log performance metrics
     */
    public static void logPerformanceMetrics(Map<String, Object> metrics) {
        System.out.println("=== Performance Metrics ===");
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " ms");
        }
        System.out.println("==========================");
    }
}