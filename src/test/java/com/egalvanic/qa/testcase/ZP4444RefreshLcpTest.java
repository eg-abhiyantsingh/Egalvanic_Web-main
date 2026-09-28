package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.Optional;
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
 * ZP-4444 follow-up: what a browser REFRESH of a page costs — the number Chrome DevTools shows as
 * "Largest Contentful Paint". The ticket's plan measured the work order's own loading; a refresh also
 * re-runs the whole app start-up (sign-in checks, feature flags, site list) before the page can ask
 * for its data.
 *
 * <p>Per refresh it records, from inside the page (script installed before the app loads): every
 * LCP candidate (time, size, element), First Contentful Paint, the first asset-grid row, navigation
 * timing (TTFB, DOMContentLoaded, load) and every resource with start/end/size. Warm refreshes use
 * the browser cache like a user pressing F5; cold ones disable the cache like DevTools' "Disable
 * cache". One JSON line per refresh goes to test-output/lcp/&lt;label&gt;.jsonl.</p>
 *
 * <p>Parameters: {@code lcp.path} (e.g. /sessions/&lt;id&gt; or /dashboard), {@code lcp.label};
 * system properties {@code lcp.warm} (default 5) and {@code lcp.cold} (default 3).</p>
 */
public class ZP4444RefreshLcpTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "lcp");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private ChromeDriver raw() { return (ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver(); }

    private static final String OBSERVER = "(function(){if(window.__qaLcp)return;var q=window.__qaLcp={lcp:[],fcp:null,grid:null,shell:null};"
            + "function d(el){if(!el)return null;var r=el.getBoundingClientRect();return {tag:el.tagName,id:el.id||null,cls:String(el.className||'').slice(0,90),"
            + "text:((el.innerText||el.alt||el.getAttribute('aria-label')||'')+'').replace(/\\s+/g,' ').trim().slice(0,70),w:Math.round(r.width),h:Math.round(r.height)};}"
            + "try{new PerformanceObserver(function(l){l.getEntries().forEach(function(e){q.lcp.push({t:Math.round(e.startTime),size:e.size,url:e.url||null,"
            + "render:Math.round(e.renderTime||0),load:Math.round(e.loadTime||0),el:d(e.element)});});}).observe({type:'largest-contentful-paint',buffered:true});}catch(e){}"
            + "try{new PerformanceObserver(function(l){l.getEntries().forEach(function(e){if(e.name==='first-contentful-paint')q.fcp=Math.round(e.startTime);});})"
            + ".observe({type:'paint',buffered:true});}catch(e){}"
            + "var sel=\"[role='row'][data-rowindex], .MuiDataGrid-row\";"
            + "new MutationObserver(function(m,o){var t=Math.round(performance.now());if(!q.shell&&document.querySelector('nav a[href]'))q.shell=t;"
            + "if(!q.grid&&document.querySelector(sel))q.grid=t;if(q.grid&&q.shell)o.disconnect();}).observe(document,{childList:true,subtree:true});})();";

    @SuppressWarnings("unchecked")
    private Map<String, Object> collect() {
        return (Map<String, Object>) js("var q=window.__qaLcp||{};var n=performance.getEntriesByType('navigation')[0]||{};"
                + "var res=performance.getEntriesByType('resource').map(function(e){return {n:e.name.replace(location.origin,'').replace(/[0-9a-f]{8}-[0-9a-f-]{27}/g,'{id}').slice(0,160),"
                + "i:e.initiatorType,s:Math.round(e.startTime),rs:Math.round(e.requestStart||0),r:Math.round(e.responseStart||0),e:Math.round(e.responseEnd),"
                + "tx:e.transferSize||0,sz:e.decodedBodySize||0};});"
                + "return {lcp:q.lcp||[],fcp:q.fcp,grid:q.grid,shell:q.shell,ttfb:Math.round(n.responseStart||0),dcl:Math.round(n.domContentLoadedEventEnd||0),"
                + "load:Math.round(n.loadEventEnd||0),navType:n.type||null,docTx:n.transferSize||0,res:res,path:location.pathname};");
    }

    @SuppressWarnings("unchecked")
    @Test(description = "ZP-4444 follow-up: Largest Contentful Paint on a browser refresh (warm and cold cache)")
    @Parameters({"lcp.path", "lcp.label"})
    public void refreshLcp(@Optional("/sessions/f64fd11b-d223-4162-b42e-1cf071e0f7d7") String path,
                           @Optional("wo-large") String label) throws Exception {
        ExtentReportManager.createTest("ZP-4444", "Refresh LCP", label + " " + path);
        tileWindow(Integer.getInteger("lcp.slot", 0) * 380, Integer.getInteger("lcp.slot", 0) * 40, 1280, 820);
        Map<String, Object> src = new HashMap<>();
        src.put("source", OBSERVER);
        raw().executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
        raw().executeCdpCommand("Network.enable", new HashMap<>());
        Files.createDirectories(OUT);
        Path out = OUT.resolve(label + ".jsonl");
        Files.deleteIfExists(out);

        driver.get(AppConstants.BASE_URL + path);        // first visit primes the session and the cache
        sleep(12000);
        loginPage.dismissMfaPromptIfShowing();

        int warm = Integer.getInteger("lcp.warm", 5), cold = Integer.getInteger("lcp.cold", 3);
        List<String> summary = new ArrayList<>();
        for (int i = 1; i <= warm + cold; i++) {
            boolean isCold = i > warm;
            Map<String, Object> cd = new HashMap<>();
            cd.put("cacheDisabled", isCold);
            raw().executeCdpCommand("Network.setCacheDisabled", cd);
            driver.navigate().refresh();                 // what F5 / the reload button does
            sleep(14000);                                // let LCP settle; nothing clicks, so it is not finalised early
            Map<String, Object> r = collect();
            r.put("label", label); r.put("run", i); r.put("cache", isCold ? "cold" : "warm");
            List<Map<String, Object>> l = (List<Map<String, Object>>) r.get("lcp");
            Map<String, Object> last = l.isEmpty() ? null : l.get(l.size() - 1);
            String line = String.format("%s run %d (%s): LCP %s ms [%s] · FCP %s · shell %s · grid %s · TTFB %s · DCL %s",
                    label, i, r.get("cache"), last == null ? "-" : last.get("t"),
                    last == null ? "-" : String.valueOf(last.get("el")).replaceAll("\\s+", " "), r.get("fcp"), r.get("shell"), r.get("grid"), r.get("ttfb"), r.get("dcl"));
            System.out.println("[LCP] " + line);
            summary.add(line);
            Files.write(out, (new org.json.JSONObject(r).toString() + "\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            if (i == warm || i == warm + cold) {
                js("var o=document.createElement('div');o.style.cssText='position:fixed;right:10px;bottom:10px;z-index:2147483647;max-width:760px;background:rgba(15,27,33,.93);"
                        + "color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap';o.textContent=arguments[0];document.body.appendChild(o);",
                        "QA evidence ZP-4444 refresh · " + AppConstants.BASE_URL.replace("https://", "") + "\n" + line);
                Files.write(OUT.resolve(label + "_" + r.get("cache") + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
            }
        }
        Map<String, Object> cd = new HashMap<>();
        cd.put("cacheDisabled", false);
        raw().executeCdpCommand("Network.setCacheDisabled", cd);
        Files.write(OUT.resolve(label + "_summary.txt"), String.join("\n", summary).getBytes());
    }
}
