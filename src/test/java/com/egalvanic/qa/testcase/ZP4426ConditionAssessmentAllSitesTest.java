package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZP-4426 "[Web] 500 on GET /api/condition-assessment/assets (new regression)" — the QA steps of
 * the fix, frontend PR #1556: with "All Sites" chosen the site picker stores the literal "all", and
 * Condition Assessment (/pm-readiness) sent ?sld_id=all to /condition-assessment/overview, /findings
 * and /assets, each answering 500. After the fix the page shows "Pick a single site…" and makes no
 * /condition-assessment/* request.
 *
 * <p>Every fetch/XHR is recorded from inside the page with its HTTP status, so "no request" and
 * "no failed request" are measured, not assumed. Screenshots carry the log drawn on the page.</p>
 */
public class ZP4426ConditionAssessmentAllSitesTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "screenshots", "zp4426");
    private static final String PAGE = "/pm-readiness";
    /** Step 2's single site; optional — site names differ per environment (see {@link #pickSingleSite()}). */
    private static final String ONE_SITE = System.getProperty("zp4426.site");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private long now() { return ((Number) js("return Date.now();")).longValue(); }
    private String seat() { return AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1"); }

    private void installRecorder() {
        ChromeDriver raw = (ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver();
        Map<String, Object> src = new HashMap<>();
        src.put("source", "(function(){if(window.__qaRec)return;window.__qaRec=1;"
                + "function log(u,m,s){try{var a=JSON.parse(sessionStorage.getItem('__qaReq')||'[]');"
                + "a.push({t:Date.now(),m:m||'GET',u:String(u),s:s,p:location.pathname});sessionStorage.setItem('__qaReq',JSON.stringify(a));}catch(e){}}"
                + "var f=window.fetch;window.fetch=function(i,o){var u=i&&i.url?i.url:i,m=(o&&o.method)||(i&&i.method);var p=f.apply(this,arguments);"
                + "p.then(function(r){log(u,m,r.status);},function(){log(u,m,'ERR');});return p;};"
                + "var op=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){var x=this;"
                + "x.addEventListener('loadend',function(){log(u,m,x.status);});return op.apply(this,arguments);};})();");
        raw.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
    }

    /** Recorded /api/ calls since {@code since}, as "STATUS METHOD path". */
    @SuppressWarnings("unchecked")
    private List<String> calls(long since, String regex) {
        Object r = js("var a=JSON.parse(sessionStorage.getItem('__qaReq')||'[]'),re=new RegExp(arguments[0]),since=arguments[1];"
                + "return a.filter(function(x){return x.t>=since&&x.u.indexOf('/api/')>=0&&re.test(x.u);}).map(function(x){"
                + "return x.s+' '+x.m+' '+x.u.replace(location.origin,'').replace(/[0-9a-f]{8}-[0-9a-f-]{27}/g,'{id}');});", regex, since);
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    private String mainText() {
        return String.valueOf(js("var m=document.querySelector('main')||document.body;return (m.innerText||'').replace(/\\s+/g,' ').slice(0,600);"));
    }

    private boolean errorScreen() {
        return Boolean.TRUE.equals(js("var t=(document.body.innerText||'');return /Application Error|Something went wrong|Unexpected Application Error/i.test(t);"));
    }

    private void waitLoaded() {
        for (int i = 0; i < 40; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('nav a[href]').length>0;"))) break;
            sleep(1000);
        }
        sleep(6000);
    }

    private String site() {
        return String.valueOf(js("var i=[].slice.call(document.querySelectorAll(\"input[placeholder='Select facility']\")).find(function(x){return x.offsetParent!==null;});return i?i.value:'(no site picker)';"));
    }

    private void evidence(String name, String title, List<String> lines) throws Exception {
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();o=document.createElement('div');o.id='__qaOverlay';"
                + "o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;max-width:780px;background:rgba(15,27,33,.93);color:#e4edef;"
                + "font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap';"
                + "o.textContent=arguments[0];document.body.appendChild(o);",
                "QA evidence ZP-4426 · " + title + "\n" + (lines.isEmpty() ? "(no /condition-assessment request)" : String.join("\n", lines)));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + "_" + name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();");
    }


    /**
     * The real user path to the bug: "All Facilities" is offered only on list pages such as Work
     * Orders (/sessions). Choose it there, the way a user does — open the picker with the filter
     * empty and click the entry (the store then holds the literal "all").
     */
    private boolean chooseAllFacilitiesOnWorkOrders() {
        String list = System.getProperty("zp4426.list", "/tasks");   // Tasks is site-scoped AND offers All Facilities
        driver.get(AppConstants.BASE_URL + list);
        waitLoaded();
        Object opened = openSitePicker();
        if (opened == null) { System.out.println("[ZP-4426] no visible site picker on " + list); return false; }
        Object clicked = null;
        for (int i = 0; i < 20 && clicked == null; i++) {
            sleep(250);
            clicked = js("var o=[].slice.call(document.querySelectorAll('li[role=option]'));"
                    + "var a=o.find(function(x){return /^all (facilities|sites)$/i.test((x.innerText||'').trim());});"
                    + "if(!a)return null;a.click();return (a.innerText||'').trim();");
        }
        if (clicked == null) System.out.println("[ZP-4426] 'All Facilities' not in the site picker; options: "
                + js("return [].slice.call(document.querySelectorAll('li[role=option]')).slice(0,6).map(function(x){return (x.innerText||'').trim();});"));
        sleep(2000);
        System.out.println("[ZP-4426] on " + list + " chose '" + clicked + "'; picker '" + site() + "', stored activeSiteId '" + js("return localStorage.getItem('activeSiteId');") + "'");
        return clicked != null;
    }

    /** Opens the visible site picker with its filter empty, so every option is listed; null when there is none. */
    private Object openSitePicker() {
        // several pickers can exist (one hidden in a collapsed panel): use the visible one
        return js("var i=[].slice.call(document.querySelectorAll(\"input[placeholder='Select facility']\")).find(function(x){return x.offsetParent!==null;});"
                + "if(!i)return null;i.scrollIntoView({block:'center'});i.focus();"
                + "var s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;s.call(i,'');i.dispatchEvent(new Event('input',{bubbles:true}));"
                + "var r=i.closest('.MuiAutocomplete-root');var t=r&&r.querySelector('.MuiAutocomplete-popupIndicator');"
                + "if(t)t.click();else i.dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));return 'ok';");
    }

    /**
     * Step 2's single site: -Dzp4426.site when it is set and in the picker, otherwise the first picker
     * option that is a real site other than the current one. Returns the site chosen, or null.
     */
    private String pickSingleSite() {
        if (ONE_SITE != null && !ONE_SITE.trim().isEmpty()) {
            if (selectSiteByName(ONE_SITE)) return ONE_SITE;
            // the typed filter matched nothing: blur closes the empty list and restores the picker's text
            js("var a=document.activeElement;if(a&&a.blur)a.blur();");
            sleep(800);
            System.out.println("[ZP-4426] '" + ONE_SITE + "' is not in the site picker — using the first other site instead");
        }
        String current = site();
        if (openSitePicker() == null) { System.out.println("[ZP-4426] no visible site picker for step 2"); return null; }
        Object clicked = null;
        for (int i = 0; i < 20 && clicked == null; i++) {
            sleep(250);
            clicked = js("var cur=String(arguments[0]).trim().toLowerCase();var o=[].slice.call(document.querySelectorAll('li[role=option]'));"
                    + "var a=o.find(function(x){var t=(x.innerText||'').trim();return t&&t.toLowerCase()!==cur&&!/^all (facilities|sites)$/i.test(t);});"
                    + "if(!a)return null;a.click();return (a.innerText||'').trim();", current);
        }
        System.out.println("[ZP-4426] step 2 site: '" + clicked + "' — the first picker option other than the current '" + current + "'"
                + (ONE_SITE == null ? " (no -Dzp4426.site given)" : ""));
        return clicked == null ? null : String.valueOf(clicked);
    }

    /** True when the rail offers the category at all (its label is visible in the narrow left rail). */
    private boolean railHas(String category) {
        return Boolean.TRUE.equals(js("var want=arguments[0];return [].slice.call(document.querySelectorAll('button,a,[role=button],div')).some(function(x){"
                + "return (x.innerText||'').replace(/\\s+/g,' ').trim()===want&&x.offsetParent!==null&&x.getBoundingClientRect().left<90;});", category));
    }

    /**
     * In-app navigation, no reload (a reload re-resolves "all" to a real site): open the rail
     * category whose panel holds the link, then click the link itself.
     */
    private String openFromMenu(String category, String href) {
        Object r = js("var h=arguments[1];var l=document.querySelector('a[href=\"'+h+'\"]');"
                + "if(!l||l.offsetParent===null){var c=[].slice.call(document.querySelectorAll('nav *,aside *')).find(function(x){"
                + "return x.children.length<=2&&(x.innerText||'').trim()===arguments[0]&&x.offsetParent!==null;}.bind(null));"
                + "var want=arguments[0];c=[].slice.call(document.querySelectorAll('button,a,[role=button],div')).find(function(x){"
                + "return (x.innerText||'').trim()===want&&x.offsetParent!==null&&x.getBoundingClientRect().left<90;});if(c)c.click();}"
                + "return 'ok';", category, href);
        sleep(1200);
        Object how = js("var l=[].slice.call(document.querySelectorAll('a[href=\"'+arguments[0]+'\"]')).find(function(x){return x.offsetParent!==null;});"
                + "if(l){l.click();return 'menu link';}return null;", href);
        for (int i = 0; i < 20 && !String.valueOf(js("return location.pathname;")).equals(href); i++) sleep(250);
        sleep(6000);
        System.out.println("[ZP-4426] opened " + href + " via " + how + " (rail '" + category + "') → now on " + js("return location.pathname;"));
        return how == null ? null : String.valueOf(how);
    }

    @Test(description = "ZP-4426 QA steps 1-4: Condition Assessment with All Sites never sends sld_id=all — guard or a real site, nothing fails")
    public void allSitesMakesNoConditionAssessmentRequest() throws Exception {
        ExtentReportManager.createTest("ZP-4426", "Condition Assessment with All Sites", "Steps 1-4");
        installRecorder();
        tileWindow(Integer.getInteger("zp4426.slot", 0) * 470, 0, 1100, 820);
        List<String> notes = new ArrayList<>();
        String CA = "/condition-assessment/";

        // Step 1: choose All Facilities on Work Orders, then open Condition Assessment from the menu
        long t1 = now();
        boolean picked = chooseAllFacilitiesOnWorkOrders();
        String nav1 = openFromMenu("Site Data", PAGE);
        List<String> s1 = calls(t1, CA);
        String text1 = mainText();
        boolean msg1 = text1.contains("Pick a single site");
        String pick1 = site(); boolean err1 = errorScreen();
        notes.add("Step 1 (All Facilities chosen on Work Orders: " + picked + "; Condition Assessment opened via " + nav1 + ", picker '" + site()
                + "'): condition-assessment calls " + s1 + ", 'Pick a single site' shown " + msg1 + ", error screen " + errorScreen()
                + " · page: " + text1.substring(0, Math.min(140, text1.length())));
        evidence("1_all_sites", "step 1 · All Facilities chosen on Work Orders, then Condition Assessment from the menu", s1);

        // Step 1b: a full reload of the page with "all" stored — this build re-resolves it to a real site
        long t1b = now();
        driver.get(AppConstants.BASE_URL + PAGE);
        waitLoaded();
        List<String> s1b = calls(t1b, CA);
        notes.add("Step 1b (page reloaded, stored activeSiteId '" + js("return localStorage.getItem('activeSiteId');") + "', picker '" + site()
                + "'): condition-assessment calls " + s1b + ", error screen " + errorScreen());

        // Step 2: pick a single site; overview, findings and assets load
        long t2 = now();
        String site2 = pickSingleSite();
        boolean picked2 = site2 != null;
        if (!String.valueOf(js("return location.pathname;")).equals(PAGE)) { driver.get(AppConstants.BASE_URL + PAGE); waitLoaded(); }
        sleep(7000);
        List<String> s2 = calls(t2, CA);
        long s2ok = s2.stream().filter(l -> l.startsWith("200 ")).count();
        String text2 = mainText();
        notes.add("Step 2 (picked '" + site2 + "': " + picked2 + ", picker '" + site() + "'): condition-assessment calls " + s2.size()
                + " (" + s2ok + " answered 200), message gone " + !text2.contains("Pick a single site") + ", error screen " + errorScreen()
                + " · page: " + text2.substring(0, Math.min(160, text2.length())));
        evidence("2_single_site", "step 2 · single site '" + site() + "'", s2);

        // Step 3: back to All Facilities (on Work Orders), then Condition Assessment again
        long t3 = now();
        boolean picked3 = chooseAllFacilitiesOnWorkOrders();
        String nav3 = openFromMenu("Site Data", PAGE);
        List<String> s3 = calls(t3, CA);
        List<String> failed3 = new ArrayList<>();
        for (String l : calls(t3, ".")) if (!l.matches("^(2|3)\\d\\d .*")) failed3.add(l);
        boolean msg3 = mainText().contains("Pick a single site");
        String pick3 = site(); boolean err3 = errorScreen();
        notes.add("Step 3 (All Facilities again: " + picked3 + ", opened via " + nav3 + ", picker '" + site() + "'): condition-assessment calls " + s3
                + ", failed API calls " + failed3 + ", message " + msg3 + ", error screen " + errorScreen());
        evidence("3_back_to_all_sites", "step 3 · All Facilities again, then Condition Assessment from the menu", s3);

        // Step 4: the portal's Condition Assessment, reached the same way
        long t4 = now();
        boolean picked4 = chooseAllFacilitiesOnWorkOrders();
        String nav4 = openFromMenu("Maintenance Portal", "/maintenance-portal/condition");
        List<String> s4 = calls(t4, CA);
        String text4 = mainText();
        boolean msg4 = text4.contains("Pick a single site");
        String pick4 = site(); boolean err4 = errorScreen();
        // Roles without Maintenance Portal (e.g. Electrical Engineer) have no such rail entry: the step does not apply to them
        boolean na4 = nav4 == null && !railHas("Maintenance Portal");
        String na4Note = "not applicable for this role (no Maintenance Portal in the rail)";
        notes.add("Step 4 (All Facilities: " + picked4 + ", /maintenance-portal/condition via " + nav4 + ", on " + js("return location.pathname;")
                + "): " + (na4 ? na4Note + "; " : "") + "condition-assessment calls " + s4 + ", Access Denied " + text4.contains("Access Denied")
                + ", message " + msg4 + ", error screen " + errorScreen());
        if (na4) ExtentReportManager.logInfo("Step 4 " + na4Note);
        evidence("4_portal_condition", "step 4 · " + (na4 ? na4Note : "All Facilities, then the portal's Condition Assessment"), s4);
        List<String> anyAll = calls(t1, "sld_id=all");
        System.out.println("[ZP-4426] " + seat() + ":\n  " + String.join("\n  ", notes));
        Files.write(OUT.resolve(seat() + "_steps.txt"), String.join("\n", notes).getBytes());

        Assert.assertTrue(picked, "Could not choose All Facilities on Work Orders");
        Assert.assertTrue(anyAll.isEmpty(), "No request may carry sld_id=all, got " + anyAll);
        // After All Facilities the app may do either of two correct things: keep "all" and show the guard
        // ("Pick a single site", no request), or re-resolve "all" to the first real site (site routes do this,
        // seen on staging and QA) and load that site with 200s. Either way: no sld_id=all, nothing fails.
        assertAllSitesHandled("Step 1", s1, msg1, pick1, err1);
        Assert.assertTrue(picked2, "Step 2: could not pick a single site in the picker");
        Assert.assertTrue(s2ok >= 3 && s2ok == s2.size(), "Step 2: overview, findings and assets should load with 200, got " + s2);
        Assert.assertTrue(failed3.isEmpty(), "Step 3: no failed request, got " + failed3);
        assertAllSitesHandled("Step 3", s3, msg3, pick3, err3);
        if (!na4) assertAllSitesHandled("Step 4", s4, msg4, pick4, err4);
    }

    /** The guard with no request, or a real site whose calls all answered 200 — and never an error screen. */
    private void assertAllSitesHandled(String step, List<String> caCalls, boolean guardShown, String picker, boolean errorShown) {
        Assert.assertFalse(errorShown, step + ": error screen shown");
        if (guardShown) {
            Assert.assertEquals(caCalls.size(), 0, step + ": 'Pick a single site' shown but requests still went out: " + caCalls);
            return;
        }
        Assert.assertFalse(picker.isEmpty() || picker.toLowerCase().startsWith("all "),
                step + ": no guard shown, yet the picker still reads '" + picker + "'");
        Assert.assertTrue(!caCalls.isEmpty() && caCalls.stream().allMatch(l -> l.startsWith("200 ")),
                step + ": moved to site '" + picker + "' but its condition-assessment calls did not all answer 200: " + caCalls);
    }
}
