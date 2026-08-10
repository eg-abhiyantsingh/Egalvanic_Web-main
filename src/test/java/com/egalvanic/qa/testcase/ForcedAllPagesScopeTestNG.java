package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Regression net for eg-pz-frontend PR #1127 — "stop site-scoping the FORCED_ALL pages".
 *
 * <p><b>The product contract.</b> A handful of routes are in Layout's
 * {@code FORCED_ALL_PAGES} and render no site picker. Their data is company-wide, not site
 * data, so their requests must span every site the user can access. Sending the topbar's
 * site id from these pages is a defect: the user sees a silently truncated list with no
 * control visible to explain or undo it.</p>
 *
 * <p><b>Why this test exists rather than a manual check.</b> The bug was not a single bad
 * line — it was the store's {@code sldId} surviving as a stale leftover from the last
 * site-scoped page, which {@code Layout} only resets to "all" asynchronously and only on a
 * pathname change. That produced two windows: a persistent one (creating a quote from
 * /opportunities calls {@code setActiveSldId}; the pathname never changes, so the list stays
 * filtered until you navigate away and back) and a transient one (a direct navigation races
 * the force-to-"all", so the first result set is scoped and then corrects). Both are easy to
 * reintroduce from any page that writes {@code sldId}, and neither is visible in a
 * screenshot — the grid just looks short. The only durable guard is to assert on the wire.</p>
 *
 * <p><b>Method.</b> A fetch/XHR recorder is installed in the page, and each route is then
 * reached by <em>clicking its sidebar link</em> — a real SPA navigation, so the recorder
 * survives and we observe exactly what the route requests. Crucially the test first parks on
 * a SITE-SCOPED page so the store holds a real site id: arriving with a clean store would
 * pass even if the bug were fully reintroduced. Both the URL and the POST body are checked,
 * because these endpoints carry scope in either (e.g. {@code /planned-workorders?sld_id=}
 * vs a {@code quotes/v2} body field).</p>
 *
 * <p>Verified against QA V1.36 (2026-08-10): all four data-bearing routes fire zero scoped
 * requests, including on the stale-store path. {@code /jobs} is also a FORCED_ALL page but
 * issues no API call for this account, so it is skipped rather than asserted green.</p>
 *
 * <p>Safety: strictly read-only — navigation and observation only.</p>
 */
public class ForcedAllPagesScopeTestNG extends BaseTest {

    private static final String MODULE = "Sales";
    private static final String FEATURE = "FORCED_ALL page scope (PR #1127)";

    /** A site-scoped route used to seed a REAL site id into the store before each check. */
    private static final String SITE_SCOPED_SEED_ROUTE = "/assets";

    /** How long to wait for a route's own data call to appear in the recorder. */
    private static final int DATA_CALL_TIMEOUT_MS = 20000;

    /**
     * Installs a recorder for every /api request, capturing URL AND request body.
     * Survives SPA navigation (same document), which is why routes are reached by
     * clicking rather than by driver.get().
     */
    private static final String RECORDER_JS =
        "window.__egCap = [];"
      + "if (!window.__egHooked) {"
      + "  window.__egHooked = true;"
      + "  var of = window.fetch;"
      + "  window.fetch = function(input, init) {"
      + "    try {"
      + "      var url = (typeof input === 'string') ? input : ((input && input.url) || '');"
      + "      var body = (init && init.body) || null;"
      + "      if (String(url).indexOf('/api/') >= 0)"
      + "        window.__egCap.push({url: String(url), body: (typeof body === 'string') ? body : null});"
      + "    } catch (e) {}"
      + "    return of.apply(this, arguments);"
      + "  };"
      + "  var oo = XMLHttpRequest.prototype.open, os = XMLHttpRequest.prototype.send;"
      + "  XMLHttpRequest.prototype.open = function(m, u) { this.__egU = u; return oo.apply(this, arguments); };"
      + "  XMLHttpRequest.prototype.send = function(b) {"
      + "    try {"
      + "      if (this.__egU && String(this.__egU).indexOf('/api/') >= 0)"
      + "        window.__egCap.push({url: String(this.__egU), body: (typeof b === 'string') ? b : null});"
      + "    } catch (e) {}"
      + "    return os.apply(this, arguments);"
      + "  };"
      + "}"
      + "return 'ok';";

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    /** Park on a site-scoped page so the store's sldId holds a REAL site, not "all". */
    private void seedRealSiteInStore() {
        driver.get(AppConstants.BASE_URL + SITE_SCOPED_SEED_ROUTE);
        pause(6000);
    }

    /** Click a sidebar anchor by href — a genuine in-app SPA navigation. */
    private boolean spaNavigate(String href) {
        Object clicked = js().executeScript(
            "var a = document.querySelector(\"a[href='" + href + "']\");"
          + "if (!a) return false; a.click(); return true;");
        return Boolean.TRUE.equals(clicked);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> captured() {
        Object raw = js().executeScript("return window.__egCap || [];");
        return raw instanceof List ? (List<Map<String, Object>>) raw : new ArrayList<>();
    }

    /** Wait until the route's own data endpoint shows up, so we never assert on an empty capture. */
    private boolean awaitDataCall(String marker) {
        long deadline = System.currentTimeMillis() + DATA_CALL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            for (Map<String, Object> c : captured()) {
                if (String.valueOf(c.get("url")).contains(marker)) return true;
            }
            pause(500);
        }
        return false;
    }

    /**
     * The shared check: reach {@code href} from a site-scoped page and prove nothing it
     * requests carries a site id.
     *
     * @param marker a fragment of the route's own data endpoint, so we assert on a real load
     */
    private void assertRouteIsNotSiteScoped(String label, String href, String marker) {
        seedRealSiteInStore();
        js().executeScript(RECORDER_JS);

        if (!spaNavigate(href)) {
            throw new SkipException("No sidebar link for " + href
                + " — this account cannot reach " + label + ".");
        }
        pause(2500);
        Assert.assertTrue(driver.getCurrentUrl().contains(href),
                label + ": expected to land on " + href + " but was " + driver.getCurrentUrl());

        boolean sawData = awaitDataCall(marker);
        List<Map<String, Object>> calls = captured();
        // A capture with no data call proves nothing — fail loudly rather than pass vacuously.
        Assert.assertTrue(sawData,
                label + ": never observed its data call ('" + marker + "') within "
                + (DATA_CALL_TIMEOUT_MS / 1000) + "s, so the scope check would be vacuous. "
                + "Observed " + calls.size() + " /api call(s): " + summarize(calls));

        List<String> scoped = new ArrayList<>();
        for (Map<String, Object> c : calls) {
            String url = String.valueOf(c.get("url"));
            Object bodyObj = c.get("body");
            String body = bodyObj == null ? "" : String.valueOf(bodyObj);
            if (url.contains("sld_id") || url.contains("sldId")
                    || body.contains("sld_id") || body.contains("sldId")) {
                scoped.add(shortPath(url) + (body.isEmpty() ? "" : "  body=" + body));
            }
        }

        logStep(label + ": " + calls.size() + " /api call(s) observed, "
                + scoped.size() + " site-scoped.");
        Assert.assertTrue(scoped.isEmpty(),
                label + " is a FORCED_ALL page (no site picker) but still scoped its request(s) "
                + "to the topbar site — the grid silently hides other sites' rows with no visible "
                + "control to explain it. Offending call(s): " + scoped);
        ExtentReportManager.logPass(label + " spans all sites (no sld_id on the wire)");
    }

    private String shortPath(String url) {
        int i = url.indexOf("/api/");
        return i >= 0 ? url.substring(i) : url;
    }

    private String summarize(List<Map<String, Object>> calls) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < calls.size() && i < 8; i++) {
            sb.append(i == 0 ? "" : ", ").append(shortPath(String.valueOf(calls.get(i).get("url"))));
        }
        return sb.length() == 0 ? "(none)" : sb.toString();
    }

    // ────────────────────────────────────────────────────────────────
    // One test per FORCED_ALL route that actually fetches data.
    // ────────────────────────────────────────────────────────────────

    @Test(priority = 1, description = "TC_FA_001: /opportunities (Quotes) is not site-scoped")
    public void testFA_001_OpportunitiesSpansAllSites() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_FA_001_Opportunities");
        assertRouteIsNotSiteScoped("Quotes (/opportunities)", "/opportunities", "quotes/v2");
    }

    @Test(priority = 2, description = "TC_FA_002: /emps is not site-scoped")
    public void testFA_002_EmpsSpansAllSites() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_FA_002_EMPs");
        assertRouteIsNotSiteScoped("EMPs (/emps)", "/emps", "committed-quotes/v2");
    }

    @Test(priority = 3, description = "TC_FA_003: /planned-work is not site-scoped")
    public void testFA_003_PlannedWorkSpansAllSites() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_FA_003_PlannedWork");
        assertRouteIsNotSiteScoped("Planned Work (/planned-work)", "/planned-work",
                "planned-workorders");
    }

    @Test(priority = 4, description = "TC_FA_004: /scheduling is not site-scoped")
    public void testFA_004_SchedulingSpansAllSites() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_FA_004_Scheduling");
        // /scheduling had CLIENT-side filters removed (booked work + the calendar's
        // session→work-block expansion), so its own calls were never scoped by URL. The
        // check still matters: re-adding a scoped fetch here is exactly how it would regress.
        assertRouteIsNotSiteScoped("Scheduling (/scheduling)", "/scheduling",
                "workorders-with-jobs");
    }
}
