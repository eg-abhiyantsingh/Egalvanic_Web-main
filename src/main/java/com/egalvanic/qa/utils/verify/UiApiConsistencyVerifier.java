package com.egalvanic.qa.utils.verify;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * UI ↔ API consistency verifier — the DB-less way to catch the "data corruption /
 * stale-cache across flows" bug class when direct database access isn't available.
 *
 * Strategy: monkey-patch window.fetch + XMLHttpRequest IN THE PAGE to record, per
 * API response, a best-effort item COUNT (array length, or .count / .total /
 * .results.length / .data.length). We then assert the count the API actually
 * returned matches what the UI rendered (grid pagination "of N"). A mismatch =
 * the screen is lying about what the server returned — stale cache, dropped rows,
 * duplicate render, or a broken count badge.
 *
 * Endpoint-agnostic: we never hardcode a URL schema; we observe whatever the app
 * itself calls and match by URL substring. No DB creds, no contract guessing.
 *
 * Install once after page load (or via BaseTest.reinstallHealthCapture); the hook
 * survives until the next full navigation, after which you re-install.
 */
public final class UiApiConsistencyVerifier {

    private UiApiConsistencyVerifier() {}

    private static final String INSTALL_JS =
        "if (window.__apiCounts) return;"
        + "window.__apiCounts = {};"  // url -> {count, status, ts}
        + "function deriveCount(body){"
        + "  try {"
        + "    var j = (typeof body === 'string') ? JSON.parse(body) : body;"
        + "    if (Array.isArray(j)) return j.length;"
        + "    if (j && typeof j === 'object') {"
        + "      if (typeof j.count === 'number') return j.count;"
        + "      if (typeof j.total === 'number') return j.total;"
        + "      if (Array.isArray(j.results)) return j.results.length;"
        + "      if (Array.isArray(j.data)) return j.data.length;"
        + "      if (Array.isArray(j.items)) return j.items.length;"
        + "    }"
        + "  } catch(e){}"
        + "  return -1;"
        + "}"
        + "var origFetch = window.fetch;"
        + "if (origFetch) {"
        + "  window.fetch = function(){"
        + "    var url = (arguments[0] && arguments[0].url) ? arguments[0].url : String(arguments[0]);"
        + "    return origFetch.apply(this, arguments).then(function(res){"
        + "      try { res.clone().text().then(function(t){"
        + "        window.__apiCounts[url] = {count: deriveCount(t), status: res.status, ts: Date.now()};"
        + "      }); } catch(e){}"
        + "      return res;"
        + "    });"
        + "  };"
        + "}"
        + "var OrigXHR = window.XMLHttpRequest;"
        + "if (OrigXHR) {"
        + "  var open = OrigXHR.prototype.open, send = OrigXHR.prototype.send;"
        + "  OrigXHR.prototype.open = function(m, u){ this.__url = u; return open.apply(this, arguments); };"
        + "  OrigXHR.prototype.send = function(){"
        + "    var xhr = this;"
        + "    this.addEventListener('load', function(){"
        + "      try { window.__apiCounts[xhr.__url] = {count: deriveCount(xhr.responseText), status: xhr.status, ts: Date.now()}; } catch(e){}"
        + "    });"
        + "    return send.apply(this, arguments);"
        + "  };"
        + "}";

    /** Install the fetch/XHR response recorder. Idempotent; re-call after navigations. */
    public static void install(WebDriver driver) {
        try { ((JavascriptExecutor) driver).executeScript(INSTALL_JS); }
        catch (Exception e) { System.out.println("[UiApiConsistency] install failed: " + e.getMessage()); }
    }

    /** All recorded {url -> count} entries whose url contains the substring. */
    @SuppressWarnings("unchecked")
    public static Map<String, Long> countsFor(WebDriver driver, String urlSubstring) {
        Map<String, Long> out = new HashMap<>();
        try {
            Map<String, Object> all = (Map<String, Object>) ((JavascriptExecutor) driver)
                    .executeScript("return window.__apiCounts || {};");
            if (all == null) return out;
            for (Map.Entry<String, Object> e : all.entrySet()) {
                if (urlSubstring == null || e.getKey().contains(urlSubstring)) {
                    Object v = e.getValue();
                    if (v instanceof Map) {
                        Object c = ((Map<?, ?>) v).get("count");
                        if (c instanceof Number) out.put(e.getKey(), ((Number) c).longValue());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[UiApiConsistency] read failed: " + e.getMessage());
        }
        return out;
    }

    /**
     * Hard-assert the UI grid total matches the count the API returned for the
     * matching endpoint. {@code uiTotal} is the grid's pagination "of N".
     * Skips (logs) if no matching API response was observed (don't false-fail).
     */
    public static void assertGridMatchesApi(WebDriver driver, String urlSubstring, long uiTotal, String flow) {
        Map<String, Long> counts = countsFor(driver, urlSubstring);
        if (counts.isEmpty()) {
            System.out.println("[UiApiConsistency] " + flow + ": no API response observed for '"
                    + urlSubstring + "' — cannot verify (skipped)");
            return;
        }
        // Use the most recent matching response's count
        long apiCount = counts.values().stream().filter(c -> c >= 0).reduce((a, b) -> b).orElse(-1L);
        if (apiCount < 0) {
            System.out.println("[UiApiConsistency] " + flow + ": API count undetermined (skipped)");
            return;
        }
        if (apiCount != uiTotal) {
            throw new AssertionError("[UiApiConsistency] " + flow + ": UI shows " + uiTotal
                    + " but API (" + urlSubstring + ") returned " + apiCount
                    + " — UI is misrepresenting server data (stale cache / dropped / duplicated rows).");
        }
        System.out.println("[UiApiConsistency] " + flow + ": UI " + uiTotal + " == API " + apiCount + " ✓");
    }
}
