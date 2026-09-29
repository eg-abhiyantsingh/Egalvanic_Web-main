package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZP-4464 deep checks on stage, read-only.
 *
 * <p>{@link #simulatedStates()}: stage's acme company has only a Foundation subscription, so every other state is
 * rendered by the REAL stage frontend fed with a simulated API answer — a fetch() override installed before the app
 * starts returns a scenario's banner and page JSON for {@code /api/subscription/banner} and {@code /api/subscription}
 * (scenarios in {@code -Dzp4464.simdir}, optional filter {@code -Dzp4464.only=f3,m1}). Nothing reaches the server.
 * This proves wording, colours, buttons, dismissal and layout; it does NOT prove the backend computes these states.</p>
 *
 * <p>{@link #realDataChecks()}: on the real data — banner fetch count across in-app navigation, layout shift, axe
 * accessibility scan of the banner, and API refusal/tampering probes (all GETs).</p>
 *
 * <p>{@link #flagOff()}: LaunchDarkly blocked in the browser, so every flag defaults to off (the documented behaviour
 * when LD is unreachable) — the banner, menu item and page must disappear.</p>
 */
public class ZP4464SubscriptionDeepTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "zp4464-deep");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private Object jsAsync(String s, Object... a) { return ((JavascriptExecutor) driver).executeAsyncScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private ChromeDriver raw() { return (ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver(); }
    private String seat() { return AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1"); }

    private void open(String path) {
        driver.get(AppConstants.BASE_URL + path);
        for (int i = 0; i < 40; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('a[href],button').length>5;"))) break;
            sleep(1000);
        }
        sleep(5000);
    }

    /** Banner = the element holding the customer-success mail link; text, colours, role and its own buttons. */
    private String banner() {
        return String.valueOf(js("var a=[].slice.call(document.querySelectorAll('a[href^=\"mailto:customer-success\"]')).find(function(x){return x.offsetParent!==null;});"
                + "if(!a)return 'none';var b=a;while(b.parentElement&&!b.getAttribute('role'))b=b.parentElement;if(!b.getAttribute('role'))return 'no role container';"
                + "var btn=[].slice.call(b.querySelectorAll('button')).map(function(x){return (x.innerText||'').trim()||('['+(x.getAttribute('aria-label')||'icon')+']');});var cs=getComputedStyle(b);"
                + "return (b.innerText||'').replace(/\\s+/g,' ').trim()+' || role='+b.getAttribute('role')+' || bg='+cs.backgroundColor+' || buttons='+JSON.stringify(btn);"));
    }

    private void shot(String name, String caption) throws Exception {
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();o=document.createElement('div');o.id='__qaOverlay';o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;"
                + "max-width:760px;background:rgba(15,27,33,.93);color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #f0a553;white-space:pre-wrap';"
                + "o.textContent=arguments[0];document.body.appendChild(o);", caption);
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();");
    }

    private String sectionShot(String heading, String name, String caption) throws Exception {
        Object found = js("var s=arguments[0];var h=[].slice.call(document.querySelectorAll('main *')).find(function(e){return e.children.length===0&&(e.innerText||'').trim()===s;});"
                + "if(!h)return false;h.scrollIntoView({block:'start'});window.scrollBy(0,-90);return true;", heading);
        sleep(700);
        if (Boolean.TRUE.equals(found)) shot(name, caption);
        return String.valueOf(found);
    }

    // ------------------------------------------------------------------ simulated states
    @Test(description = "ZP-4464 deep: every plan state rendered by the real stage frontend from a simulated API answer")
    public void simulatedStates() throws Exception {
        ExtentReportManager.createTest("ZP-4464", "Subscription deep", "Simulated states · " + seat());
        File dir = new File(System.getProperty("zp4464.simdir", "src/test/resources/zp4464-sim"));
        List<String> only = Arrays.asList(System.getProperty("zp4464.only", "").split(","));
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        Assert.assertNotNull(files, "no scenario folder " + dir);
        Arrays.sort(files);
        String scriptId = null;
        List<String> summary = new ArrayList<>();
        for (File f : files) {
            String name = f.getName().replace(".json", "");
            if (!only.get(0).isEmpty() && only.stream().noneMatch(name::startsWith)) continue;
            String scen = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            if (scriptId != null) {
                Map<String, Object> rm = new HashMap<>();
                rm.put("identifier", scriptId);
                raw().executeCdpCommand("Page.removeScriptToEvaluateOnNewDocument", rm);
            }
            Map<String, Object> src = new HashMap<>();
            src.put("source", "(function(){var S=" + scen + ";window.__qaSim=S.name;var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i).split('?')[0];"
                    + "if(/\\/api\\/subscription\\/banner$/.test(u)){return Promise.resolve(new Response(JSON.stringify({success:true,banner:S.banner}),{status:200,headers:{'Content-Type':'application/json'}}));}"
                    + "if(/\\/api\\/subscription$/.test(u)){return Promise.resolve(new Response(JSON.stringify(S.page.body),{status:S.page.status,headers:{'Content-Type':'application/json'}}));}"
                    + "return f.apply(this,arguments);};})();");
            scriptId = String.valueOf(raw().executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src).get("identifier"));
            js("try{Object.keys(localStorage).forEach(function(k){if(k.indexOf('subscriptionBanner.dismissed:')===0)localStorage.removeItem(k);});}catch(e){}");

            List<String> notes = new ArrayList<>();
            open("/dashboard");
            String b1 = banner();
            notes.add("sim active: " + js("return window.__qaSim||'(not installed)';"));
            notes.add("dashboard banner: " + b1);
            shot(seat() + "_" + name + "_1_banner", "SIMULATED API answer · " + name + " · seat " + seat() + "\n" + b1);

            // first build: a "Dismiss" text button; owner-note build (index-BFc32lrB): an X icon button labelled "Close subscription notice"
            if (b1.contains("\"Dismiss\"") || b1.contains("[Close subscription notice]")) {
                js("var a=[].slice.call(document.querySelectorAll('a[href^=\"mailto:customer-success\"]')).find(function(x){return x.offsetParent!==null;});var b=a;while(b.parentElement&&!b.getAttribute('role'))b=b.parentElement;"
                        + "var d=[].slice.call(b.querySelectorAll('button')).find(function(x){return /^dismiss$/i.test((x.innerText||'').trim())||/close subscription notice/i.test(x.getAttribute('aria-label')||'');});if(d)d.click();");
                sleep(1500);
                String afterClick = banner();
                shot(seat() + "_" + name + "_1a_after_close", "SIMULATED · " + name + " · right after clicking close\nbanner: " + afterClick);
                Object key = js("return Object.keys(localStorage).filter(function(k){return k.indexOf('subscriptionBanner.dismissed:')===0;}).join(', ')||'none';");
                // real in-app navigation: click a visible menu link, same document (the marker survives only without a reload)
                Object went = js("window.__qaSameDoc=1;var a=[].slice.call(document.querySelectorAll('nav a[href^=\"/\"],aside a[href^=\"/\"],a[href^=\"/\"]')).filter(function(x){"
                        + "var h=x.getAttribute('href');return x.offsetParent!==null&&h!==location.pathname&&h.indexOf('/admin/subscription')<0&&h.length>1;});if(!a.length)return 'no link';a[0].click();return a[0].getAttribute('href');");
                sleep(3000);
                String afterNav = banner();
                notes.add("in-app click to " + went + " → now " + js("return location.pathname;") + ", same document (no reload): " + js("return window.__qaSameDoc===1;"));
                driver.navigate().refresh();
                sleep(8000);
                String afterReload = banner();
                notes.add("Close/Dismiss clicked → banner " + (afterClick.equals("none") ? "hidden" : "STILL SHOWN: " + afterClick) + "; storage key: " + key
                        + "; after in-app navigation: " + (afterNav.equals("none") ? "hidden" : "shown") + "; after browser refresh: " + (afterReload.equals("none") ? "hidden (kept in localStorage)" : "shown again"));
                shot(seat() + "_" + name + "_1b_after_dismiss_refresh", "SIMULATED · " + name + " · after Dismiss + browser refresh\nbanner: " + afterReload);
                js("try{Object.keys(localStorage).forEach(function(k){if(k.indexOf('subscriptionBanner.dismissed:')===0)localStorage.removeItem(k);});}catch(e){}");
            }

            open("/admin/subscription");
            String page = String.valueOf(js("var m=document.querySelector('main')||document.body;return (m.innerText||'');"));
            Files.createDirectories(OUT);
            Files.write(OUT.resolve(seat() + "_" + name + "_page.txt"), page.getBytes(StandardCharsets.UTF_8));
            notes.add("page (first 700): " + page.replaceAll("\\s+", " ").substring(0, Math.min(700, page.replaceAll("\\s+", " ").length())));
            shot(seat() + "_" + name + "_2_page_top", "SIMULATED API answer · " + name + " · /admin/subscription");
            sectionShot("Modules", seat() + "_" + name + "_3_modules", "SIMULATED · " + name + " · Modules");
            sectionShot("Licensed sites", seat() + "_" + name + "_3b_sites", "SIMULATED · " + name + " · Licensed sites");
            sectionShot("Customer portal licences", seat() + "_" + name + "_3c_t2", "SIMULATED · " + name + " · Customer portal licences");
            sectionShot("Activity", seat() + "_" + name + "_4_activity", "SIMULATED · " + name + " · Activity");
            Files.write(OUT.resolve(seat() + "_" + name + ".txt"), String.join("\n", notes).getBytes(StandardCharsets.UTF_8));
            summary.add(name + ": " + b1.substring(0, Math.min(160, b1.length())));
            System.out.println("[ZP-4464 sim] " + name + ":\n  " + String.join("\n  ", notes));
        }
        if (scriptId != null) {
            Map<String, Object> rm = new HashMap<>();
            rm.put("identifier", scriptId);
            raw().executeCdpCommand("Page.removeScriptToEvaluateOnNewDocument", rm);
        }
        Files.write(OUT.resolve(seat() + "_sim_summary.txt"), String.join("\n", summary).getBytes(StandardCharsets.UTF_8));
        Assert.assertFalse(summary.isEmpty(), "no scenario ran");
    }

    // ------------------------------------------------------------------ real data
    @SuppressWarnings("unchecked")
    @Test(priority = 2, description = "ZP-4464 deep: fetch count, layout shift, accessibility and API probes on real stage data")
    public void realDataChecks() throws Exception {
        ExtentReportManager.createTest("ZP-4464", "Subscription deep", "Real data · " + seat());
        List<String> notes = new ArrayList<>();
        Map<String, Object> src = new HashMap<>();
        src.put("source", "(function(){if(window.__qaCnt)return;window.__qaCnt=1;var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i);"
                + "if(u.indexOf('/api/subscription')>=0){try{var a=JSON.parse(sessionStorage.getItem('__qaSubCalls')||'[]');a.push(Date.now()+' '+u.replace(location.origin,''));sessionStorage.setItem('__qaSubCalls',JSON.stringify(a));}catch(e){}}"
                + "return f.apply(this,arguments);};"
                + "window.__qaCls=[];try{new PerformanceObserver(function(l){l.getEntries().forEach(function(e){if(!e.hadRecentInput)window.__qaCls.push({v:e.value,t:Math.round(e.startTime),"
                + "src:(e.sources||[]).map(function(s){var n=s.node;return n&&n.nodeType===1?(n.tagName+'.'+String(n.className||'').slice(0,40)+' '+String(n.innerText||'').slice(0,40)):'?';})});});}).observe({type:'layout-shift',buffered:true});}catch(e){}"
                + "})();");
        String id = String.valueOf(raw().executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src).get("identifier"));
        js("try{sessionStorage.removeItem('__qaSubCalls');}catch(e){}");
        open("/dashboard");
        sleep(3000);
        Object cls = js("var s=0;(window.__qaCls||[]).forEach(function(e){s+=e.v;});return 'CLS '+s.toFixed(4)+' · shifts '+JSON.stringify(window.__qaCls||[]).slice(0,900);");
        notes.add("dashboard load layout shift: " + cls);
        // in-app navigation through visible menu links (no reloads)
        List<String> visited = new ArrayList<>();
        for (int k = 0; k < 6; k++) {
            Object went = js("var seen=arguments[0];var a=[].slice.call(document.querySelectorAll('nav a[href^=\"/\"], aside a[href^=\"/\"]')).filter(function(x){return x.offsetParent!==null&&seen.indexOf(x.getAttribute('href'))<0&&!/logout|subscription/i.test(x.getAttribute('href'));});"
                    + "if(!a.length)return null;a[0].click();return a[0].getAttribute('href');", visited);
            if (went == null) break;
            visited.add(String.valueOf(went));
            sleep(3500);
        }
        Object calls = js("return sessionStorage.getItem('__qaSubCalls')||'[]';");
        notes.add("in-app navigation through " + visited + " → subscription API calls since load: " + calls);
        // axe accessibility scan of the banner only
        String axe = new String(Files.readAllBytes(Paths.get(System.getProperty("zp4464.axe", "/tmp/zp4464stage/axe.min.js"))), StandardCharsets.UTF_8);
        js(axe);
        Object axeRes = jsAsync("var done=arguments[arguments.length-1];var a=[].slice.call(document.querySelectorAll('a[href^=\"mailto:customer-success\"]')).find(function(x){return x.offsetParent!==null;});"
                + "if(!a){done('no banner');return;}var b=a;while(b.parentElement&&!b.getAttribute('role'))b=b.parentElement;"
                + "axe.run(b,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21a','wcag21aa']}}).then(function(r){done('violations '+r.violations.length+' '+JSON.stringify(r.violations.map(function(v){return {id:v.id,impact:v.impact,help:v.help,n:v.nodes.length,t:v.nodes.map(function(n){return n.failureSummary;}).join(' | ').slice(0,300)};}))+' · passes '+r.passes.length+' · incomplete '+JSON.stringify(r.incomplete.map(function(v){return v.id;})));}).catch(function(e){done('axe error '+e);});");
        notes.add("axe (WCAG 2.1 A/AA) on the banner: " + axeRes);
        // API probes (GET only)
        Map<String, Object> probes = (Map<String, Object>) jsAsync("var done=arguments[arguments.length-1];var tok=null;[localStorage,sessionStorage].forEach(function(s){for(var i=0;i<s.length;i++){"
                + "var m=String(s.getItem(s.key(i))).match(/eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+/);if(m&&!tok)tok=m[0];}});"
                + "function g(label,u,h,cred){return fetch(u,{headers:h||{},credentials:cred||'include'}).then(function(r){return r.text().then(function(b){var j=null;try{j=JSON.parse(b);}catch(e){}"
                + "return label+' → '+r.status+' '+(j?JSON.stringify(j).replace(/\\s+/g,' ').slice(0,160):'(HTML page, x-cache '+(r.headers.get('x-cache')||'?')+')');});}).catch(function(e){return label+' → ERR '+e;});}"
                + "var A={Authorization:'Bearer '+tok,'X-Subdomain':'acme'};var fake='00000000-0000-4000-8000-000000000001';"
                + "Promise.all([g('banner, no token','/api/subscription/banner',{'X-Subdomain':'acme'},'omit'),g('page, no token','/api/subscription',{'X-Subdomain':'acme'},'omit'),"
                + "g('banner, forged token','/api/subscription/banner',{Authorization:'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.bad','X-Subdomain':'acme'},'omit'),"
                + "g('page, ?company_id=<other uuid>','/api/subscription?company_id='+fake,A),g('page, X-Subdomain: demo','/api/subscription',{Authorization:'Bearer '+tok,'X-Subdomain':'demo'}),"
                + "g('page, /api/subscription/<uuid>','/api/subscription/'+fake,A),g('banner, X-Subdomain: demo','/api/subscription/banner',{Authorization:'Bearer '+tok,'X-Subdomain':'demo'})])"
                + ".then(function(a){done({r:a});});");
        for (Object line : (List<Object>) probes.get("r")) notes.add("API " + line);
        Map<String, Object> rm = new HashMap<>();
        rm.put("identifier", id);
        raw().executeCdpCommand("Page.removeScriptToEvaluateOnNewDocument", rm);
        shot(seat() + "_real_checks", "REAL stage data · seat " + seat() + "\n" + String.join("\n", notes).substring(0, Math.min(900, String.join("\n", notes).length())));
        System.out.println("[ZP-4464 real] " + seat() + ":\n  " + String.join("\n  ", notes));
        Files.write(OUT.resolve(seat() + "_real_checks.txt"), String.join("\n", notes).getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ flag off
    @Test(priority = 3, description = "ZP-4464 deep: LaunchDarkly unreachable (all flags off) hides banner, menu item and page")
    public void flagOff() throws Exception {
        ExtentReportManager.createTest("ZP-4464", "Subscription deep", "Flag off · " + seat());
        List<String> notes = new ArrayList<>();
        raw().executeCdpCommand("Network.enable", new HashMap<>());
        Map<String, Object> block = new HashMap<>();
        block.put("urls", Arrays.asList("*launchdarkly.com*", "*launchdarkly.us*"));
        raw().executeCdpCommand("Network.setBlockedURLs", block);
        open("/dashboard");
        String b = banner();
        notes.add("LaunchDarkly blocked · dashboard banner: " + b);
        shot(seat() + "_flag_off_1_dashboard", "LaunchDarkly blocked (all flags default off) · banner: " + b);
        open("/admin/subscription");
        String page = String.valueOf(js("var m=document.querySelector('main')||document.body;return (m.innerText||'').replace(/\\s+/g,' ').slice(0,500);"));
        Object menu = js("return [].slice.call(document.querySelectorAll('a[href=\"/admin/subscription\"]')).filter(function(x){return x.offsetParent!==null;}).length;");
        notes.add("LaunchDarkly blocked · /admin/subscription: " + page + " · visible menu links: " + menu);
        shot(seat() + "_flag_off_2_page", "LaunchDarkly blocked · /admin/subscription\n" + page.substring(0, Math.min(300, page.length())));
        block.put("urls", new ArrayList<String>());
        raw().executeCdpCommand("Network.setBlockedURLs", block);
        System.out.println("[ZP-4464 flag off] " + seat() + ":\n  " + String.join("\n  ", notes));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + "_flag_off.txt"), String.join("\n", notes).getBytes(StandardCharsets.UTF_8));
    }
}
