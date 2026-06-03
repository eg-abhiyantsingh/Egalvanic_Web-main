package com.egalvanic.qa.utils.verify;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Network resilience helper — drives Chrome DevTools Protocol (CDP) to emulate
 * offline / slow / throttled networks so tests can exercise the RESILIENCE class
 * of bugs (timeouts, infinite spinners, lost data on reconnect, optimistic-UI
 * that never reconciles).
 *
 * Uses raw {@code executeCdpCommand} (Network.emulateNetworkConditions) rather
 * than Selenium's version-pinned DevTools classes, so it survives Chrome/Selenium
 * upgrades. Works on Chromium-family drivers (Chrome + Edge); no-op with a clear
 * log on Firefox (use BrowserMobProxy there if needed).
 *
 * Typical use:
 *   NetworkConditions.goOffline(driver);
 *   page.clickSave();                       // attempt an action with no network
 *   verifyPageHealth("save offline");       // assert graceful error, no hang/blank
 *   NetworkConditions.goOnline(driver);
 *   StateIntegrityChecker.assertUnchanged(before, after, "offline save reconnect");
 */
public final class NetworkConditions {

    private NetworkConditions() {}

    // Throughput presets in bytes/sec (CDP wants bytes/sec; -1 = no limit).
    private static final long SLOW_3G_DOWN = 50 * 1024 / 8 * 1024 / 100;  // ~ very slow
    private static final long SLOW_3G_UP   = 50 * 1024 / 8 * 1024 / 100;

    private static boolean isChromium(WebDriver driver) {
        return driver instanceof ChromiumDriver;
    }

    private static void send(WebDriver driver, String cmd, Map<String, Object> params) {
        ((ChromiumDriver) driver).executeCdpCommand(cmd, params);
    }

    /** Cut the network entirely (offline mode). */
    public static void goOffline(WebDriver driver) {
        if (!isChromium(driver)) { System.out.println("[NetworkConditions] goOffline skipped (non-Chromium)"); return; }
        send(driver, "Network.enable", new HashMap<>());
        Map<String, Object> p = new HashMap<>();
        p.put("offline", true);
        p.put("latency", 0);
        p.put("downloadThroughput", 0);
        p.put("uploadThroughput", 0);
        send(driver, "Network.emulateNetworkConditions", p);
        System.out.println("[NetworkConditions] OFFLINE");
    }

    /** Throttle to a slow connection with added latency (ms). */
    public static void throttle(WebDriver driver, long downBytesPerSec, long upBytesPerSec, int latencyMs) {
        if (!isChromium(driver)) { System.out.println("[NetworkConditions] throttle skipped (non-Chromium)"); return; }
        send(driver, "Network.enable", new HashMap<>());
        Map<String, Object> p = new HashMap<>();
        p.put("offline", false);
        p.put("latency", latencyMs);
        p.put("downloadThroughput", downBytesPerSec);
        p.put("uploadThroughput", upBytesPerSec);
        send(driver, "Network.emulateNetworkConditions", p);
        System.out.println("[NetworkConditions] THROTTLED down=" + downBytesPerSec
                + "B/s up=" + upBytesPerSec + "B/s latency=" + latencyMs + "ms");
    }

    /** Slow-3G-ish preset with 400ms latency. */
    public static void slow3G(WebDriver driver) {
        throttle(driver, SLOW_3G_DOWN, SLOW_3G_UP, 400);
    }

    /** Restore full, unthrottled connectivity. */
    public static void goOnline(WebDriver driver) {
        if (!isChromium(driver)) return;
        Map<String, Object> p = new HashMap<>();
        p.put("offline", false);
        p.put("latency", 0);
        p.put("downloadThroughput", -1);
        p.put("uploadThroughput", -1);
        send(driver, "Network.emulateNetworkConditions", p);
        System.out.println("[NetworkConditions] ONLINE (restored)");
    }
}
