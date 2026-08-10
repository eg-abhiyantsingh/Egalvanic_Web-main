package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Regression net for eg-pz-frontend PR #1127 — "suppress the Upload Anything invite during a
 * running job".
 *
 * <p><b>The product contract.</b> A site with no assets shows the invite "Let's get your assets
 * in". But a site is also empty <em>while its extraction job is still running</em> — and there
 * the invite asks the user to start the very thing they just started. So the invite must be
 * held until {@code GET /api/onboarding/jobs/active?sld_id=…} resolves for the CURRENT site,
 * skipped entirely when that job is running or pending, and — critically — still shown when the
 * lookup FAILS, so a flaky call cannot hide the onboarding path forever.</p>
 *
 * <p><b>Why this needs fault injection.</b> Three of the four states cannot be produced by
 * clicking: you cannot make the backend return "running" on demand, and you certainly cannot
 * make it fail on demand. The test therefore wraps {@code window.fetch} and answers that one
 * endpoint itself. That is also the only honest way to test the fail-open requirement, which is
 * defined entirely in terms of the call breaking.</p>
 *
 * <p><b>Control-first.</b> Every "invite is hidden" assertion is worthless unless the invite can
 * be shown at all on that page and site — a page that never renders it would pass every
 * suppression check. So the test asserts the CONTROL (empty site, no job, invite visible)
 * first and skips the whole class if it cannot establish it.</p>
 *
 * <p><b>Deliberately NOT asserted: /dashboard.</b> Dashboard is itself a FORCED_ALL page, so
 * {@code sldId} is "all" and its invite bails on "all" — verified on QA V1.36 (2026-08-10) to be
 * absent even with NO job. A "hidden during a job" assertion there passes for the wrong reason.
 * See docs/product-knowledge/upload-anything-onboarding-jobs.md.</p>
 *
 * <p>Safety: read-only. Creates nothing; only observes an empty site and stubs one GET in the
 * browser. Verified against QA V1.36 on 2026-08-10.</p>
 */
public class UploadInviteGatingTestNG extends BaseTest {

    private static final String MODULE = "Assets";
    private static final String FEATURE = "Upload Anything invite gating (PR #1127)";
    private static final String INVITE_RE = "get your assets in";

    /**
     * Wraps fetch and answers /api/onboarding/jobs/active according to window.__jobMode:
     *   off      → real endpoint (the control)
     *   running  → 200 with a job whose status is "running"
     *   pending  → 200 with a job whose status is "pending"
     *   break    → the promise REJECTS (network failure)
     *   http500  → 200-level machinery returns a 500 response
     * Installed once; the mode is flipped between checks. Survives SPA navigation, which is why
     * the page is re-entered by clicking the sidebar rather than by driver.get().
     */
    private static final String SHIM_JS =
        "window.__jobMode = window.__jobMode || 'off'; window.__jobHits = 0;"
      + "if (!window.__egJobShim) {"
      + "  window.__egJobShim = true;"
      + "  var of = window.fetch;"
      + "  window.fetch = function(input, init) {"
      + "    var url = (typeof input === 'string') ? input : ((input && input.url) || '');"
      + "    if (String(url).indexOf('/api/onboarding/jobs/active') >= 0) {"
      + "      window.__jobHits++;"
      + "      var m = window.__jobMode;"
      + "      if (m === 'break') return Promise.reject(new TypeError('injected network failure'));"
      + "      if (m === 'http500') return Promise.resolve(new Response('{\"error\":\"injected\"}',"
      + "          {status:500, headers:{'Content-Type':'application/json'}}));"
      + "      if (m === 'running' || m === 'pending') {"
      + "        var sld = (String(url).match(/sld_id=([^&]+)/) || [])[1] || null;"
      + "        return Promise.resolve(new Response(JSON.stringify({success:true,"
      + "          job:{id:'injected', sld_id:sld, status:m, counts:{}}}),"
      + "          {status:200, headers:{'Content-Type':'application/json'}}));"
      + "      }"
      + "    }"
      + "    return of.apply(this, arguments);"
      + "  };"
      + "}"
      + "return 'ok';";

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    /** Ask the API for a site with zero assets. Emptiness drifts on this shared QA tenant, so it
     *  is discovered at run time rather than hardcoded. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findEmptySite() {
        Object res = js().executeAsyncScript(
            "var done = arguments[arguments.length - 1];"
          + "(async function () {"
          + "  try {"
          + "    var cid = document.location.pathname && null;"
          + "    var me = await (await fetch('/api/auth/v2/me', {credentials:'include'})).json();"
          + "    var companyId = me.company_id || (me.user && me.user.company_id) || me.companyId;"
          + "    var r = await fetch('/api/company/' + companyId + '/slds', {credentials:'include'});"
          + "    var j = await r.json();"
          + "    var list = Array.isArray(j) ? j : (j.slds || j.data || []);"
          + "    for (var i = 0; i < list.length && i < 60; i++) {"
          + "      var s = list[i], id = s.id || s.sld_id;"
          + "      var rr = await fetch('/api/lookup/v2/nodes/' + id + '?page=1&page_size=1', {credentials:'include'});"
          + "      var jj = await rr.json();"
          + "      var total = (jj.total !== undefined) ? jj.total : jj.count;"
          + "      if (total === 0) {"
          + "        var aj = await (await fetch('/api/onboarding/jobs/active?sld_id=' + id, {credentials:'include'})).json();"
          + "        if (!aj.job) { done({id:id, name:(s.name||s.sld_name)}); return; }"   // empty AND no job
          + "      }"
          + "    }"
          + "    done(null);"
          + "  } catch (e) { done(null); }"
          + "})();");
        return (Map<String, Object>) res;
    }

    /** Current value of the topbar site picker ("" when the page has none). */
    private String pickedSite() {
        Object v = js().executeScript(
            "var i = document.querySelector(\"input[placeholder='Select facility']\");"
          + "return i ? i.value : '';");
        return v == null ? "" : String.valueOf(v);
    }

    /**
     * Switch the topbar site, then VERIFY it took. Silent failure here is the whole reason the
     * first version of this test tripped its own control: typing a name that is already selected
     * opens no dropdown, and duplicate site names mean the first option may not be the one meant.
     * The field is cleared first, and the option is matched on exact text.
     */
    private void selectSite(String siteName) {
        if (siteName.equals(pickedSite())) return;               // already there
        js().executeScript(
            "var inp = document.querySelector(\"input[placeholder='Select facility']\");"
          + "if (!inp) return 'no-picker';"
          + "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
          + "inp.focus();"
          + "setter.call(inp, '');"                              // clear so the list re-opens
          + "inp.dispatchEvent(new Event('input', {bubbles:true}));"
          + "setter.call(inp, arguments[0]);"
          + "inp.dispatchEvent(new Event('input', {bubbles:true}));"
          + "return 'typed';", siteName);
        pause(2000);
        js().executeScript(
            "var want = arguments[0];"
          + "var opts = [].slice.call(document.querySelectorAll('li[role=\"option\"]'));"
          + "var exact = opts.filter(function(o){ return o.textContent.trim() === want; });"
          + "var pick = exact.length ? exact[0] : opts[0];"
          + "if (pick) pick.click();", siteName);
        pause(5000);
        Assert.assertEquals(pickedSite(), siteName,
            "Site switch did not take: the picker still reads '" + pickedSite() + "'. Every "
          + "assertion after this would be about the wrong site.");
    }

    /** Leave Assets and come back, inside the SPA, so the component remounts and re-fetches
     *  through the shim. A driver.get() would reload the document and drop the shim. */
    private void remountAssets() {
        js().executeScript("var a = document.querySelector(\"a[href='/issues']\"); if (a) a.click();");
        pause(3000);
        js().executeScript("var a = document.querySelector(\"a[href='/assets']\"); if (a) a.click();");
        pause(4500);
    }

    private boolean inviteVisible() {
        Object r = js().executeScript(
            "return /" + INVITE_RE + "/i.test(document.body.innerText);");
        return Boolean.TRUE.equals(r);
    }

    /**
     * Poll for the invite instead of sampling once. The gate deliberately HOLDS the invite until
     * the job lookup resolves, so a single read taken too early sees "hidden" for a page that is
     * merely still deciding — which would make the control flaky and the suppression checks
     * falsely green.
     */
    private boolean inviteVisibleWithin(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (inviteVisible()) return true;
            pause(750);
        }
        return false;
    }

    private void setMode(String mode) {
        js().executeScript("window.__jobMode = arguments[0]; window.__jobHits = 0;", mode);
    }

    private long jobHits() {
        Object n = js().executeScript("return window.__jobHits || 0;");
        return n instanceof Number ? ((Number) n).longValue() : 0L;
    }

    @Test(priority = 1,
          description = "TC_UI_001: the invite is gated by the active-job lookup "
                      + "(control shown; running/pending hidden; failure shows it)")
    public void testUploadInviteGating() {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_UI_001_InviteGating");

        driver.get(AppConstants.BASE_URL + "/assets");
        pause(6000);

        Map<String, Object> site = findEmptySite();
        if (site == null || site.get("name") == null) {
            throw new SkipException("No site with zero assets AND no active job is available on "
                + "this tenant right now — the invite cannot be produced, so gating is untestable.");
        }
        String siteName = String.valueOf(site.get("name"));
        logStep("Using empty site '" + siteName + "' (" + site.get("id") + ")");
        selectSite(siteName);

        js().executeScript(SHIM_JS);

        // ── CONTROL: empty site, real endpoint, no job -> the invite MUST be visible.
        // Without this the suppression checks below could pass on a page that never shows it.
        setMode("off");
        remountAssets();
        Assert.assertTrue(inviteVisibleWithin(15000),
            "CONTROL FAILED: on empty site '" + siteName + "' with no active job the invite "
          + "should be visible. Every suppression assertion below would otherwise be vacuous.");
        ExtentReportManager.logPass("Control: invite is visible on an empty site with no job");

        // ── A running job must suppress it.
        setMode("running");
        remountAssets();
        Assert.assertTrue(jobHits() > 0, "The active-job lookup was never called — the shim did "
            + "not intercept, so this check proves nothing.");
        pause(4000);
        Assert.assertFalse(inviteVisible(),
            "Invite is showing while an extraction job is RUNNING — it asks the user to start "
          + "the very thing already in progress.");
        ExtentReportManager.logPass("Invite suppressed while the job is running");

        // ── A pending job must suppress it too.
        setMode("pending");
        remountAssets();
        pause(4000);
        Assert.assertFalse(inviteVisible(),
            "Invite is showing while an extraction job is PENDING.");
        ExtentReportManager.logPass("Invite suppressed while the job is pending");

        // ── FAIL OPEN: a broken lookup must NOT hide the onboarding path.
        setMode("break");
        remountAssets();
        Assert.assertTrue(inviteVisibleWithin(15000),
            "Invite is hidden after the active-job lookup FAILED. It must fail open — otherwise "
          + "one flaky call permanently hides the only route to onboarding an empty site.");
        ExtentReportManager.logPass("Invite still shown when the lookup rejects (fails open)");

        setMode("http500");
        remountAssets();
        Assert.assertTrue(inviteVisibleWithin(15000),
            "Invite is hidden after the active-job lookup returned HTTP 500 — must fail open.");
        ExtentReportManager.logPass("Invite still shown on HTTP 500 (fails open)");

        // restore the real endpoint so nothing leaks into a later test in this class
        setMode("off");
    }
}
