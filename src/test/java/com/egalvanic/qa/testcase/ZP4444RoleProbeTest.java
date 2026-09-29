package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZP-4444 plan §6 (permissions, backend change): open a work order as the signed-in seat and record
 * what the page shows — asset-grid rows, the visible text, and every /api/ call with its HTTP status —
 * so a non-admin role that cannot see the grid is explained by evidence, not guessed.
 */
public class ZP4444RoleProbeTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "screenshots", "zp4444-roles");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    @SuppressWarnings("unchecked")
    @Test(description = "ZP-4444 §6: what a work order shows for this seat")
    @Parameters({"zp4444.wo", "zp4444.label"})
    public void workOrderAsThisSeat(@Optional String woParam, @Optional String labelParam) throws Exception {
        String wo = woParam != null ? woParam : System.getProperty("zp4444.wo", "f532bf11-41e0-4b50-b310-bb82237d0d9b");
        String label = labelParam != null ? labelParam : System.getProperty("zp4444.label", "seat");
        String seat = AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1");
        ExtentReportManager.createTest("ZP-4444", "Work order as " + seat, label + " " + wo);
        tileWindow(Integer.getInteger("zp4444.slot", 2) * 380, 40, 1100, 820);
        Map<String, Object> src = new HashMap<>();
        src.put("source", "(function(){if(window.__qaRec)return;window.__qaRec=1;function log(u,m,s){try{var a=JSON.parse(sessionStorage.getItem('__qaReq')||'[]');"
                + "a.push({u:String(u),m:m||'GET',s:s});sessionStorage.setItem('__qaReq',JSON.stringify(a));}catch(e){}}"
                + "var f=window.fetch;window.fetch=function(i,o){var u=i&&i.url?i.url:i,m=(o&&o.method)||(i&&i.method);var p=f.apply(this,arguments);"
                + "p.then(function(r){log(u,m,r.status);},function(){log(u,m,'ERR');});return p;};})();");
        ((ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver()).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);

        driver.get(AppConstants.BASE_URL + "/sessions/" + wo);
        Object rows = null;
        for (int i = 0; i < 40 && rows == null; i++) {
            sleep(500);
            rows = js("var n=document.querySelectorAll(\"[role='row'][data-rowindex], .MuiDataGrid-row\").length;return n>0?n:null;");
        }
        sleep(4000);
        String text = String.valueOf(js("var m=document.querySelector('main')||document.body;return (m.innerText||'').replace(/\\s+/g,' ').slice(0,700);"));
        List<Object> calls = (List<Object>) js("return JSON.parse(sessionStorage.getItem('__qaReq')||'[]').filter(function(x){return x.u.indexOf('/api/')>=0;})"
                + ".map(function(x){return x.s+' '+x.m+' '+x.u.replace(location.origin,'').replace(/[0-9a-f]{8}-[0-9a-f-]{27}/g,'{id}');});");
        StringBuilder bad = new StringBuilder();
        for (Object c : calls) if (!String.valueOf(c).matches("^(2|3)\\d\\d .*")) bad.append("\n  ").append(c);
        System.out.println("[ZP-4444 role] " + seat + " " + label + " on /sessions/" + wo + ": grid rows " + rows + ", url " + js("return location.pathname;")
                + "\n  page: " + text + "\n  non-2xx calls:" + (bad.length() == 0 ? " none" : bad) + "\n  all calls: " + calls.size());
        js("var o=document.createElement('div');o.style.cssText='position:fixed;right:10px;bottom:10px;z-index:2147483647;max-width:760px;background:rgba(15,27,33,.93);"
                + "color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap';o.textContent=arguments[0];document.body.appendChild(o);",
                "QA evidence ZP-4444 · seat " + seat + " · /sessions/" + wo.substring(0, 8) + " · grid rows: " + rows + "\nnon-2xx API calls:" + (bad.length() == 0 ? " none" : bad));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat + "_" + label + "_" + wo.substring(0, 8) + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        Files.write(OUT.resolve(seat + "_" + label + "_" + wo.substring(0, 8) + "_calls.txt"), (text + "\n\n" + String.join("\n", (List) calls)).getBytes());
        Assert.assertNotNull(rows, "The asset grid did not show for seat " + seat);
    }
}
