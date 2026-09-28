package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZP-4445 "[Web] Suggestion Polling Off" — the dashboard attention data must load ONCE per page open:
 * no 5-minute timer, no re-fetch when the user returns to the tab, and the Open-issues-by-site donut
 * reuses its data when the user comes back within 5 minutes.
 *
 * <p>Each network call is recorded by a fetch/XHR wrapper installed with
 * {@code Page.addScriptToEvaluateOnNewDocument}, so it sees the app's own requests from the first
 * byte, keeps them in sessionStorage across reloads, and needs no DevTools window. Evidence
 * screenshots show the dashboard with the captured request log drawn on top.</p>
 *
 * <p>Parameters (suite xml or -D): {@code zp4445.route} (/sales-overview, /admin-dashboard,
 * /ops-dashboard), {@code zp4445.idleSeconds} (default 360 — more than the old 5-minute timer).</p>
 */
public class ZP4445DashboardPollingTest extends BaseTest {

    /** The calls the ticket names: badge counts, Needs Attention lists (old and new routes), donut. */
    private static final String WATCHED = "attention|open-by-site";
    private static final Path OUT = Paths.get("test-output", "screenshots", "zp4445");

    private String route;
    private int idleSeconds;

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private void installRecorder() {
        ChromeDriver raw = (ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver();
        Map<String, Object> src = new HashMap<>();
        src.put("source", "(function(){if(window.__qaRec)return;window.__qaRec=1;"
                + "function log(u,m){try{var a=JSON.parse(sessionStorage.getItem('__qaReq')||'[]');"
                + "a.push({t:Date.now(),m:m||'GET',u:String(u),p:location.pathname,v:document.visibilityState});"
                + "sessionStorage.setItem('__qaReq',JSON.stringify(a));}catch(e){}}"
                + "var f=window.fetch;window.fetch=function(i,o){log(i&&i.url?i.url:i,(o&&o.method)||(i&&i.method));return f.apply(this,arguments);};"
                + "var op=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){log(u,m);return op.apply(this,arguments);};})();");
        raw.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
    }

    /** Watched requests recorded since {@code sinceMs}, as "HH:mm:ss METHOD path?query [visibility]". */
    @SuppressWarnings("unchecked")
    private List<String> watched(long sinceMs) {
        Object r = js("var a=JSON.parse(sessionStorage.getItem('__qaReq')||'[]');var re=new RegExp(arguments[0]);var since=arguments[1];"
                + "return a.filter(function(x){return x.t>=since&&re.test(x.u);}).map(function(x){"
                + "var d=new Date(x.t);var u=x.u.replace(location.origin,'').replace(/[0-9a-f]{8}-[0-9a-f-]{27}/g,'{id}');"
                + "return d.toTimeString().slice(0,8)+' '+x.m+' '+u+' ['+x.v+' on '+x.p+']';});", WATCHED, sinceMs);
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    private long now() { return ((Number) js("return Date.now();")).longValue(); }

    private void waitForDashboard() {
        for (int i = 0; i < 60; i++) {
            loginPage.dismissMfaPromptIfShowing();
            Object ok = js("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('nav a[href]').length>0;");
            if (Boolean.TRUE.equals(ok)) break;
            sleep(1000);
        }
        sleep(12000); // let every widget finish its first load
    }

    /** Screenshot with the captured request log drawn over the page (real app pixels underneath). */
    private void evidence(String name, String title, List<String> lines) {
        try {
            js("var o=document.getElementById('__qaOverlay');if(o)o.remove();o=document.createElement('div');o.id='__qaOverlay';"
                    + "o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;max-width:760px;background:rgba(15,27,33,.93);color:#e4edef;"
                    + "font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap';"
                    + "o.textContent=arguments[0]+'\\n'+(arguments[1].length?arguments[1].join('\\n'):'(no attention / open-by-site request)');document.body.appendChild(o);",
                    "QA evidence ZP-4445 · " + title, lines);
            Files.createDirectories(OUT);
            Files.write(OUT.resolve(name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
            js("var o=document.getElementById('__qaOverlay');if(o)o.remove();");
        } catch (Exception e) { System.out.println("[ZP-4445] screenshot failed: " + e.getMessage()); }
    }

    private String tag() { return route.replace("/", "").replace("-", "_"); }

    @Test(priority = 1, description = "ZP-4445: open the dashboard — each attention call fires once")
    @Parameters({"zp4445.route"})
    public void openLoadsOnce(@org.testng.annotations.Optional("/sales-overview") String routeParam) throws Exception {
        route = routeParam;   // per-<test> parameter: three dashboards run in parallel in one JVM
        idleSeconds = Integer.getInteger("zp4445.idleSeconds", 360);
        ExtentReportManager.createTest("ZP-4445", "Dashboard polling off", "Open " + route);
        // Three browsers run at once: tile them so none is fully covered. A covered Chrome window
        // reports document.visibilityState = "hidden", and the old code only polls visible pages.
        int slot = route.contains("sales") ? 0 : route.contains("admin") ? 1 : 2;
        tileWindow(slot * 470, slot * 40, 900, 820);
        installRecorder();
        long t0 = System.currentTimeMillis();
        driver.get(AppConstants.BASE_URL + route);
        waitForDashboard();
        List<String> first = watched(t0);
        System.out.println("[ZP-4445] " + route + " open → " + first.size() + " watched calls:\n  " + String.join("\n  ", first));
        evidence(tag() + "_1_open", "open " + route, first);
        Map<String, Integer> byPath = new HashMap<>();
        for (String l : first) { String k = l.split(" ")[2].split("\\?")[0]; byPath.merge(k, 1, Integer::sum); }
        System.out.println("[ZP-4445] per endpoint on open: " + byPath);
        Assert.assertFalse(first.isEmpty(), "Opening " + route + " should load its attention data at least once");
        for (Map.Entry<String, Integer> e : byPath.entrySet()) {
            Assert.assertEquals((int) e.getValue(), 1, "On open, " + e.getKey() + " should fire exactly once, fired " + e.getValue());
        }
    }

    @Test(priority = 2, description = "ZP-4445: idle past the old 5-minute timer — nothing is re-fetched")
    public void idleDoesNotRefetch() {
        long t0 = now();
        System.out.println("[ZP-4445] " + route + " idling " + idleSeconds + " s from " + new java.util.Date(t0));
        int hidden = 0, samples = 0;
        for (int waited = 0; waited < idleSeconds; waited += 30) {
            sleep(Math.min(30, idleSeconds - waited) * 1000L);
            samples++;
            if (!"visible".equals(js("return document.visibilityState;"))) hidden++;
        }
        System.out.println("[ZP-4445] " + route + " visibility during idle: " + (samples - hidden) + "/" + samples + " samples visible");
        List<String> during = watched(t0);
        System.out.println("[ZP-4445] " + route + " after " + idleSeconds + " s idle → " + during.size() + " watched calls:\n  " + String.join("\n  ", during));
        evidence(tag() + "_2_idle", "after " + idleSeconds + " s idle on " + route, during);
        if (during.isEmpty() && hidden > 0) {
            throw new org.testng.SkipException("INCONCLUSIVE: page was hidden in " + hidden + "/" + samples
                    + " samples; the old code does not poll hidden pages, so 'no request' proves nothing");
        }
        Assert.assertTrue(during.isEmpty(), "Idle " + idleSeconds + " s on " + route + " (visible " + (samples - hidden) + "/" + samples + ") re-fetched: " + during);
    }

    @Test(priority = 3, description = "ZP-4445: switch to another tab for a minute and come back — nothing is re-fetched")
    public void tabReturnDoesNotRefetch() {
        String home = driver.getWindowHandle();
        long t0 = now();
        driver.switchTo().newWindow(WindowType.TAB);
        driver.get("about:blank");
        sleep(70000);
        driver.close();
        driver.switchTo().window(home);
        sleep(8000);
        List<String> after = watched(t0);
        System.out.println("[ZP-4445] " + route + " after tab switch → " + after.size() + " watched calls:\n  " + String.join("\n  ", after));
        evidence(tag() + "_3_tab_return", "after 70 s in another tab on " + route, after);
        Assert.assertTrue(after.isEmpty(), "Returning to the tab on " + route + " re-fetched: " + after);
    }

    /** Polls location.pathname for up to {@code seconds}; returns it once {@code ok} holds, else null. */
    private String waitForPath(java.util.function.Predicate<String> ok, int seconds) {
        for (int i = 0; i < seconds * 4; i++) {
            String p = String.valueOf(js("return location.pathname;"));
            if (ok.test(p)) return p;
            sleep(250);
        }
        return null;
    }

    @Test(priority = 4, description = "ZP-4445 step 6: leave and come back within 5 minutes — the donut is reused, Needs Attention reloads")
    public void comeBackWithinFiveMinutes() {
        String list = route.contains("sales") ? "/attention/sales" : route.contains("admin") ? "/attention/admin" : "/attention/ops";
        boolean hasDonut = !route.contains("admin");
        long t0 = now();
        // In-app navigation, the way a user moves: a visible menu link to a page that is not a dashboard.
        Object away = js("var r=arguments[0];var l=[].slice.call(document.querySelectorAll('nav a[href], aside a[href]')).filter(function(x){"
                + "var h=x.getAttribute('href');return h&&h.charAt(0)==='/'&&h!==r&&h!=='/z-university'&&x.offsetParent!==null;});"
                + "var a=l.find(function(x){return !/dashboard|overview/.test(x.getAttribute('href'));})||l[0];"
                + "if(!a)return null;a.click();return a.getAttribute('href');", route);
        String awayPath = waitForPath(p -> !p.equals(route), 10);
        System.out.println("[ZP-4445] left " + route + " by clicking menu link " + away + " → now on " + awayPath);
        sleep(6000);
        Object how = js("var r=arguments[0];var a=[].slice.call(document.querySelectorAll('a[href]')).find(function(x){return x.getAttribute('href')===r&&x.offsetParent!==null;});"
                + "if(a){a.click();return 'menu link';} history.back(); return 'browser Back';", route);
        String backPath = waitForPath(p -> p.equals(route), 10);
        System.out.println("[ZP-4445] came back to " + backPath + " via " + how);
        sleep(12000);
        String onRoute = "on " + route + "]";
        List<String> after = new ArrayList<>();
        for (String l : watched(t0)) if (l.endsWith(onRoute)) after.add(l);
        long listCalls = after.stream().filter(l -> l.contains(list)).count();
        long donut = after.stream().filter(l -> l.contains("open-by-site")).count();
        System.out.println("[ZP-4445] " + route + " away (" + awayPath + ") and back via " + how + " → " + after.size() + " watched calls on the dashboard:\n  " + String.join("\n  ", after));
        System.out.println("[ZP-4445] " + route + " step 6: Needs Attention list " + list + " loaded " + listCalls + "× (should be ≥1), donut open-by-site " + donut + "× (should be 0)");
        evidence(tag() + "_4_back_within_5min", "went to " + awayPath + " and came back via " + how + " on " + route
                + " · Needs Attention " + listCalls + "×, donut " + (hasDonut ? donut + "×" : "n/a"), after);
        Assert.assertNotNull(awayPath, "The menu click never left " + route + " — step 6 was not exercised");
        Assert.assertEquals(backPath, route, "Did not get back to the dashboard");
        if (hasDonut) Assert.assertEquals(donut, 0L, "Step 6: the donut should reuse its data within 5 minutes");
        Assert.assertTrue(listCalls >= 1, "Step 6: the Needs Attention list should load again when the dashboard opens again");
    }

    @Test(priority = 5, description = "ZP-4445: hidden for more than 5 minutes, then come back — nothing is re-fetched")
    public void returnAfterLongHiddenDoesNotRefetch() {
        String home = driver.getWindowHandle();
        int hiddenSeconds = Integer.getInteger("zp4445.hiddenSeconds", 330);
        long t0 = now();
        driver.switchTo().newWindow(WindowType.TAB);
        driver.get("about:blank");
        sleep(hiddenSeconds * 1000L);
        driver.close();
        driver.switchTo().window(home);
        long back = now();
        sleep(10000);
        List<String> whileHidden = new ArrayList<>(), onReturn = new ArrayList<>();
        for (String l : watched(t0)) (l.contains("[hidden") ? whileHidden : onReturn).add(l);
        System.out.println("[ZP-4445] " + route + " hidden " + hiddenSeconds + " s → while hidden " + whileHidden.size()
                + ", after return " + onReturn.size() + ":\n  " + String.join("\n  ", watched(t0)));
        evidence(tag() + "_5_return_after_" + hiddenSeconds + "s_hidden", "back after " + hiddenSeconds + " s in another tab on " + route, watched(t0));
        Assert.assertTrue(onReturn.isEmpty(), "Coming back to " + route + " after " + hiddenSeconds + " s re-fetched: " + onReturn);
    }
}
