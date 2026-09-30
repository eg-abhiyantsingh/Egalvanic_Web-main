package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ZP-4464: where does the subscription banner sit on each page? The owner saw it "up" on some pages and "down" on
 * others. For a fixed list of routes this records the banner's box (top, left, width, height), the page title's box,
 * the gap between them, and the banner text, with a screenshot per page.
 * Evidence: test-output/zp4464-banner-position/&lt;seat&gt;_&lt;route&gt;.png and &lt;seat&gt;_positions.txt.
 */
public class ZP4464BannerPositionProbe extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "zp4464-banner-position");
    // override with -Dzp4464.routes=/a,/b  (Builder › Reports is /reporting/builder, Admin › Platform Users is /users)
    private static final String[] ROUTES = System.getProperty("zp4464.routes",
            "/dashboard,/sessions,/assets,/reporting/builder,/users,/admin-dashboard,/admin/subscription,/issues,/maintenance-portal").split(",");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private String seat() { return AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1"); }

    @Test(description = "ZP-4464: banner position and text on each page, same login, full page loads")
    public void bannerPositionPerPage() throws Exception {
        ExtentReportManager.createTest("ZP-4464", "Subscription banner", "Position per page · " + seat());
        Files.createDirectories(OUT);
        List<String> notes = new ArrayList<>();
        // -Dzp4464.inapp=true: load the first route fully, then reach the others the way a user does inside the app
        // (pushState + popstate = the router's own navigation, no reload), because the banner may mount in a different
        // place depending on whether the page header mounted before or after it.
        boolean inApp = Boolean.getBoolean("zp4464.inapp");
        int n = 0;
        for (String route : ROUTES) {
            if (inApp && n++ > 0) {
                js("window.__qaDoc=1;history.pushState({},'',arguments[0]);window.dispatchEvent(new PopStateEvent('popstate',{state:{}}));", route);
                sleep(4000);
                if (!Boolean.TRUE.equals(js("return window.__qaDoc===1;"))) notes.add(route + ": page RELOADED (not in-app)");
            } else {
                driver.get(AppConstants.BASE_URL + route);
                for (int i = 0; i < 40; i++) {
                    loginPage.dismissMfaPromptIfShowing();
                    if (Boolean.TRUE.equals(js("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('a[href],button').length>5;"))) break;
                    sleep(1000);
                }
                sleep(5000);
            }
            String m = String.valueOf(js(
                    "var a=[].slice.call(document.querySelectorAll('a[href^=\"mailto:customer-success\"]')).find(function(x){return x.offsetParent!==null;});"
                    + "if(!a)return 'no banner';var b=a;while(b.parentElement&&!b.getAttribute('role'))b=b.parentElement;var r=b.getBoundingClientRect();"
                    + "var h=[].slice.call(document.querySelectorAll('main h1,main h2,main h3,main h4,main h5,main h6,h1,h2,h3,h4,h5,h6')).find(function(e){return e.offsetParent!==null&&e.getBoundingClientRect().top<r.top;});"
                    + "var hr=h?h.getBoundingClientRect():null;var prev=b.previousElementSibling;var pr=prev?prev.getBoundingClientRect():null;"
                    + "return 'banner top '+Math.round(r.top)+' left '+Math.round(r.left)+' width '+Math.round(r.width)+' height '+Math.round(r.height)"
                    + "+' · title \"'+(h?(h.innerText||'').trim().slice(0,30):'none')+'\" top '+(hr?Math.round(hr.top):'-')+' bottom '+(hr?Math.round(hr.bottom):'-')"
                    + "+' · gap title→banner '+(hr?Math.round(r.top-hr.bottom):'-')+' · element above the banner: '+(prev?prev.tagName+'.'+String(prev.className).slice(0,30)+' bottom '+Math.round(pr.bottom):'none')"
                    + "+' · text \"'+(b.innerText||'').replace(/\\s+/g,' ').trim().slice(0,70)+'\"';"));
            String path = String.valueOf(js("return location.pathname;"));
            notes.add(route + " (now " + path + "): " + m);
            String name = seat() + "_" + route.replaceAll("[^a-z]", "_");
            js("var o=document.getElementById('__qaOverlay');if(o)o.remove();o=document.createElement('div');o.id='__qaOverlay';o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;max-width:760px;background:rgba(15,27,33,.93);color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #f0a553;white-space:pre-wrap';o.textContent=arguments[0];document.body.appendChild(o);",
                    "QA · banner position · " + route + "\n" + m);
            Files.write(OUT.resolve(name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
            js("var o=document.getElementById('__qaOverlay');if(o)o.remove();");
            System.out.println("[ZP-4464 banner position] " + notes.get(notes.size() - 1));
        }
        Files.write(OUT.resolve(seat() + "_positions.txt"), String.join("\n", notes).getBytes(StandardCharsets.UTF_8));
        Assert.assertFalse(notes.isEmpty());
    }
}
