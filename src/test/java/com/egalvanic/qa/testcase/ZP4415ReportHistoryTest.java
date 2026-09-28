package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.JavascriptExecutor;
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
 * ZP-4415 "[Web] Maintenance Portal report history returns 500" — step 1 of the ticket's QA steps,
 * in the real UI: open Maintenance Portal › Reports and check the page's own
 * {@code GET /api/reporting/history?sld_id=<site>&limit=1} call. Before the fix every call answers
 * 500 {@code internal_error} (NameError: User not imported), so the page can never show history.
 *
 * <p>The page's fetches are recorded with their HTTP status by a wrapper installed before the app
 * loads. The evidence screenshot shows the Reports page with that log drawn on top.</p>
 */
public class ZP4415ReportHistoryTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "screenshots", "zp4415");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
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
        sleep(12000);
        Object site = js("var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return x.closest('nav,aside')&&x.value;});return i?i.value:'';");
        List<Map<String, Object>> calls = (List<Map<String, Object>>) js("return JSON.parse(sessionStorage.getItem('__qaHist')||'[]');");
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> c : calls) {
            lines.add(c.get("s") + " GET " + String.valueOf(c.get("u")).replace(AppConstants.BASE_URL, "").replaceAll("[0-9a-f]{8}-[0-9a-f-]{27}", "{site}")
                    + "  →  " + String.valueOf(c.get("b")).replaceAll("\\s+", " "));
        }
        String pageText = String.valueOf(js("var m=document.querySelector('main')||document.body;return m.innerText;"));
        System.out.println("[ZP-4415] site='" + site + "' history calls:\n  " + String.join("\n  ", lines));
        System.out.println("[ZP-4415] page text (first 600): " + pageText.replace('\n', '|').substring(0, Math.min(600, pageText.length())));

        js("var o=document.createElement('div');o.id='__qaOverlay';o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;max-width:820px;"
                + "background:rgba(15,27,33,.93);color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #f0887f;white-space:pre-wrap';"
                + "o.textContent=arguments[0];document.body.appendChild(o);",
                "QA evidence ZP-4415 · report history calls made by this page (site: " + site + ")\n"
                        + (lines.isEmpty() ? "(no /reporting/history call)" : String.join("\n", lines)));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve("reports_page_history_calls.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));

        Assert.assertFalse(calls.isEmpty(), "The Reports page should ask for the site's report history");
        for (Map<String, Object> c : calls) {
            Assert.assertEquals(((Number) c.get("s")).intValue(), 200, "Report history call answered " + c.get("s") + ": " + c.get("b"));
        }
    }
}
