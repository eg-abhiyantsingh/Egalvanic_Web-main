package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
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
 * ZP-4415 "[Web] Maintenance Portal report history returns 500" — step 1 of the ticket's QA steps,
 * in the real UI: open Maintenance Portal › Reports and check the page's own
 * {@code GET /api/reporting/history?sld_id=<site>&limit=1} call. Before the fix every call answers
 * 500 {@code internal_error} (NameError: User not imported), so the page can never show history.
 *
 * <p>The page asks for history only on the Free license ({@code latestReportOnly}), so the test
 * first picks "Free" in the sidebar License select (a per-browser preview, restored afterwards).
 * The page's fetches are recorded with their HTTP status by a wrapper installed before the app
 * loads. The evidence screenshot shows the Reports page with that log drawn on top.</p>
 */
public class ZP4415ReportHistoryTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "screenshots", "zp4415");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private Object jsAsync(String s, Object... a) { return ((JavascriptExecutor) driver).executeAsyncScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    @SuppressWarnings("unchecked")
    @Test(description = "ZP-4415 step 1: Maintenance Portal › Reports loads the site's report history (200, not 500)")
    public void reportsPageLoadsHistory() throws Exception {
        ExtentReportManager.createTest("ZP-4415", "Report history", "Maintenance Portal › Reports");
        ChromeDriver raw = (ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver();
        Map<String, Object> src = new HashMap<>();
        src.put("source", "(function(){if(window.__qaRec)return;window.__qaRec=1;var f=window.fetch;"
                + "window.fetch=function(i,o){var u=String(i&&i.url?i.url:i);var p=f.apply(this,arguments);"
                + "if(/reporting\\/history/.test(u)){p.then(function(r){r.clone().text().then(function(b){"
                + "var a=JSON.parse(sessionStorage.getItem('__qaHist')||'[]');a.push({t:Date.now(),u:u,s:r.status,b:b.slice(0,160)});"
                + "sessionStorage.setItem('__qaHist',JSON.stringify(a));});}).catch(function(){});}return p;};})();");
        raw.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);

        driver.get(AppConstants.BASE_URL + "/maintenance-portal/reports");
        for (int i = 0; i < 60; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("return !((document.body&&document.body.innerText)||'').startsWith('Loading')&&document.querySelectorAll('nav a[href]').length>0;"))) break;
            sleep(1000);
        }
        sleep(6000);
        Object lic0 = js("return localStorage.getItem('eg.maintenancePortal.previewLicense');");
        Object sel = js("var c=[].slice.call(document.querySelectorAll('span,p,div,label')).filter(function(e){return e.children.length===0&&(e.textContent||'').trim()==='License';});"
                + "for(var i=0;i<c.length;i++){var p=c[i];for(var k=0;k<4&&p;k++){p=p.parentElement;if(!p)break;var s=p.querySelector('[aria-haspopup=listbox],[role=combobox]');if(s){s.scrollIntoView({block:'center'});return s;}}}return null;");
        // Tier-2 tenants get no License select (their server licence applies); the page must then already be on Free
        boolean hasSelect = sel instanceof WebElement;
        if (hasSelect) {
            ((WebElement) sel).click();
            sleep(800);
            List<WebElement> free = driver.findElements(By.xpath("//li[@role='option'][normalize-space(.)='Free']"));
            Assert.assertFalse(free.isEmpty(), "The sidebar License select has no 'Free' option");
            free.get(0).click();
        } else {
            System.out.println("[ZP-4415] no sidebar License select (tier-2 tenant): relying on the server licence being Free");
        }
        Object lic1 = js("return localStorage.getItem('eg.maintenancePortal.previewLicense');");
        for (int i = 0; i < 20 && Boolean.FALSE.equals(js("return JSON.parse(sessionStorage.getItem('__qaHist')||'[]').length>0;")); i++) sleep(1000);
        sleep(2000);
        String siteId = String.valueOf(js("return localStorage.getItem('activeSiteId')||'';"));
        Object site = js("var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return x.closest('nav,aside')&&x.value;});return i?i.value:'';");
        List<Map<String, Object>> calls = (List<Map<String, Object>>) js("return JSON.parse(sessionStorage.getItem('__qaHist')||'[]');");
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> c : calls) {
            lines.add(c.get("s") + " GET " + String.valueOf(c.get("u")).replace(AppConstants.BASE_URL, "").replaceAll("[0-9a-f]{8}-[0-9a-f-]{27}", "{site}")
                    + "  →  " + String.valueOf(c.get("b")).replaceAll("\\s+", " "));
        }
        String pageText = String.valueOf(js("var m=document.querySelector('main')||document.body;return m.innerText;"));
        System.out.println("[ZP-4415] License Free, site='" + site + "' " + siteId + " history calls:\n  " + String.join("\n  ", lines));
        System.out.println("[ZP-4415] page text (first 600): " + pageText.replace('\n', '|').substring(0, Math.min(600, pageText.length())));

        js("var o=document.createElement('div');o.id='__qaOverlay';o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;max-width:820px;"
                + "background:rgba(15,27,33,.93);color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #f0887f;white-space:pre-wrap';"
                + "o.textContent=arguments[0];document.body.appendChild(o);",
                "QA evidence ZP-4415 · report history calls made by this page (License Free, site: " + site + ")\n"
                        + (lines.isEmpty() ? "(no /reporting/history call)" : String.join("\n", lines)));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve("reports_page_history_calls.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        js("if(arguments[0]==null)localStorage.removeItem('eg.maintenancePortal.previewLicense');else localStorage.setItem('eg.maintenancePortal.previewLicense',arguments[0]);", lic0);

        if (hasSelect) Assert.assertEquals(lic1, "no_license", "License preview after picking Free");
        Assert.assertFalse(calls.isEmpty(), hasSelect ? "On the Free license the Reports page should ask for the site's report history"
                : "No License select and no history call: this tier-2 tenant's server licence is not Free, so step 1 cannot run here");
        Assert.assertTrue(calls.stream().anyMatch(c -> String.valueOf(c.get("u")).contains("/reporting/history?sld_id=" + siteId + "&limit=1")),
                "Expected GET /api/reporting/history?sld_id=" + siteId + "&limit=1, got " + lines);
        for (Map<String, Object> c : calls) {
            Assert.assertEquals(((Number) c.get("s")).intValue(), 200, "Report history call answered " + c.get("s") + ": " + c.get("b"));
        }
    }

    /**
     * The history API as the signed-in app calls it, for a seat that cannot open the Maintenance
     * Portal (the portal needs the "Portal Sales" role or a T2 tenant): same token, same origin.
     * Asks for the current site's history the way the portal does (limit=1, with and without a
     * report config), plus the input checks. Before the fix every one of these answered 500.
     */
    @SuppressWarnings("unchecked")
    @Test(priority = 3, description = "ZP-4415: report history API from the signed-in app — 200 for a real site, 400 without a site")
    public void historyApiFromSignedInApp() throws Exception {
        ExtentReportManager.createTest("ZP-4415", "Report history", "History API from the signed-in app");
        driver.get(AppConstants.BASE_URL + "/dashboard");
        for (int i = 0; i < 40; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("return document.querySelectorAll('nav a[href]').length>0;"))) break;
            sleep(1000);
        }
        sleep(4000);
        List<Map<String, Object>> res = (List<Map<String, Object>>) ((JavascriptExecutor) driver).executeAsyncScript(
                "var done=arguments[arguments.length-1];var tok=null;[localStorage,sessionStorage].forEach(function(s){for(var i=0;i<s.length;i++){"
                + "var m=String(s.getItem(s.key(i))).match(/eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+/);if(m&&!tok)tok=m[0];}});"
                + "var site=localStorage.getItem('activeSiteId')||'';"
                + "var probes=[['site history, limit=1 (what the portal asks)','/api/reporting/history?sld_id='+site+'&limit=1'],"
                + "['site history, limit=50','/api/reporting/history?sld_id='+site+'&limit=50'],"
                + "['site history for one report config','/api/reporting/history?sld_id='+site+'&reporting_config_id=6bd0c71c-c86c-442c-b4ed-91ce0145acf0&limit=1'],"
                + "['no site id (should be 400)','/api/reporting/history?limit=1']];"
                + "Promise.all(probes.map(function(p){return fetch(p[1],{headers:tok?{Authorization:'Bearer '+tok}:{}}).then(function(r){return r.text().then(function(b){"
                + "return {label:p[0],s:r.status,b:b.replace(/\\s+/g,' ').slice(0,90)};});}).catch(function(e){return {label:p[0],s:'ERR',b:String(e)};});}))"
                + ".then(function(a){a.unshift({label:'site '+site+(tok?'':' (no token found)'),s:'',b:''});done(a);});");
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> c : res) lines.add(String.format("%-4s", c.get("s")) + c.get("label") + (String.valueOf(c.get("b")).isEmpty() ? "" : "  →  " + c.get("b")));
        System.out.println("[ZP-4415] history API from the app:\n  " + String.join("\n  ", lines));
        js("var o=document.createElement('div');o.id='__qaOverlay';o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;max-width:860px;"
                + "background:rgba(15,27,33,.93);color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap';"
                + "o.textContent=arguments[0];document.body.appendChild(o);",
                "QA evidence ZP-4415 · " + AppConstants.BASE_URL.replace("https://", "") + " · report history, called from this signed-in page\n" + String.join("\n", lines));
        Files.createDirectories(OUT);
        String seat = AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1");   // +stageam@ → stageam
        Files.write(OUT.resolve("history_api_from_app_" + seat + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        for (int i = 1; i <= 3; i++) Assert.assertEquals(res.get(i).get("s"), 200L, res.get(i).get("label") + ": " + res.get(i).get("b"));
        Assert.assertEquals(res.get(4).get("s"), 400L, "History without a site id should be refused with 400: " + res.get(4).get("b"));
    }

    /**
     * Stage/QA end-to-end for steps 1 + 2 + 4: generate one report from the Reports page the way a
     * user does (click a report card, then Generate), then ask the history API — from inside the
     * signed-in page, same cookies — whether the new report is listed, and try its download.
     */
    @SuppressWarnings("unchecked")
    @Test(priority = 2, description = "ZP-4415 steps 1/2/4: a generated report shows in history and downloads")
    public void generatedReportAppearsInHistory() throws Exception {
        ExtentReportManager.createTest("ZP-4415", "Report history", "Generate then list + download");
        String siteName = System.getProperty("zp4415.site", "Test site 4/12");
        Assert.assertTrue(selectSiteByName(siteName), "Could not select site '" + siteName + "' (-Dzp4415.site)");
        driver.get(AppConstants.BASE_URL + "/maintenance-portal/reports");
        sleep(10000);
        loginPage.dismissMfaPromptIfShowing();
        String siteId = String.valueOf(js("return localStorage.getItem('activeSiteId')||'';"));
        Map<String, Object> before = (Map<String, Object>) jsAsync("var d=arguments[arguments.length-1];fetch('/api/reporting/history?sld_id='+arguments[0]+'&limit=5',{credentials:'include'})"
                + ".then(function(r){return r.text().then(function(b){d({s:r.status,b:b.slice(0,400)});});}).catch(function(e){d({s:'ERR',b:String(e)});});", siteId);
        System.out.println("[ZP-4415] site " + siteId + " history BEFORE: " + before);
        // click the first report card whose title is a site-level report
        Object card = js("var t=['Issue Report','Condition Assessment','Annual Maintenance Report','Program Compliance','EMP Lite'];"
                + "var h=[].slice.call(document.querySelectorAll('main *')).find(function(e){return e.children.length===0&&t.indexOf((e.textContent||'').trim())>=0;});"
                + "if(!h)return null;var c=h.closest('button,[role=button],a,.MuiCard-root,.MuiPaper-root')||h;c.scrollIntoView({block:'center'});c.click();return (h.textContent||'').trim();");
        System.out.println("[ZP-4415] clicked report card: " + card);
        sleep(4000);
        Files.createDirectories(OUT);
        Files.write(OUT.resolve("stage_after_card_click.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        Object gen = js("var b=[].slice.call(document.querySelectorAll('button')).filter(function(x){return /generate|create report|download|export/i.test((x.innerText||'').trim())&&!x.disabled&&x.offsetParent!==null;});"
                + "if(!b.length)return null;var x=b[b.length-1];x.click();return (x.innerText||'').trim();");
        System.out.println("[ZP-4415] clicked: " + gen);
        long t0 = System.currentTimeMillis(); Map<String, Object> after = null;
        while (System.currentTimeMillis() - t0 < 180000) {
            sleep(10000);
            after = (Map<String, Object>) jsAsync("var d=arguments[arguments.length-1];fetch('/api/reporting/history?sld_id='+arguments[0]+'&limit=5',{credentials:'include'})"
                    + ".then(function(r){return r.text().then(function(b){d({s:r.status,b:b.slice(0,1200)});});}).catch(function(e){d({s:'ERR',b:String(e)});});", siteId);
            if (String.valueOf(after.get("b")).contains("\"id\"")) break;
        }
        System.out.println("[ZP-4415] history AFTER generate: " + after);
        Files.write(OUT.resolve("stage_after_generate.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        String body = String.valueOf(after == null ? "" : after.get("b"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-f-]{36})\"").matcher(body);
        if (m.find()) {
            Object dl = jsAsync("var d=arguments[arguments.length-1];fetch('/api/reporting/history/'+arguments[0]+'/download',{credentials:'include'})"
                    + ".then(function(r){return r.text().then(function(b){d({s:r.status,ct:r.headers.get('content-type'),b:b.slice(0,300)});});}).catch(function(e){d({s:'ERR',b:String(e)});});", m.group(1));
            System.out.println("[ZP-4415] download of " + m.group(1) + ": " + dl);
        } else System.out.println("[ZP-4415] no report id listed after generating");
        Assert.assertTrue(m.find(0), "The generated report should be listed by /reporting/history");
    }
}
