package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZP-4464 Subscription module, read-only, for the signed-in seat (run once per role with -DUSER_EMAIL).
 * Records what this seat gets from GET /api/subscription/banner and GET /api/subscription, whether the
 * banner shows on two pages (and with which buttons), whether the menu offers Admin › Billing ›
 * Subscription, and what /admin/subscription shows when opened directly. Nothing is clicked except
 * page navigation. Evidence: test-output/zp4464-stage/&lt;seat&gt;_*.png and &lt;seat&gt;.txt.
 */
public class ZP4464SubscriptionStageTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "zp4464-stage");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private Object jsAsync(String s, Object... a) { return ((JavascriptExecutor) driver).executeAsyncScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private String seat() { return AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1"); }

    private void open(String path) {
        driver.get(AppConstants.BASE_URL + path);
        for (int i = 0; i < 40; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('a[href],button').length>5;"))) break;
            sleep(1000);
        }
        sleep(6000);
    }

    /** The banner is the element that names the sales address; returns its text and buttons, or null. */
    private String banner() {
        return String.valueOf(js("var e=[].slice.call(document.querySelectorAll('a[href^=\"mailto:customer-success\"]')).find(function(a){return a.offsetParent!==null;});"
                + "if(!e)return 'none';var b=e;for(var i=0;i<6&&b.parentElement;i++){b=b.parentElement;if(b.querySelector('button'))break;}"
                + "var btns=[].slice.call(b.querySelectorAll('button')).map(function(x){return (x.innerText||'').trim();}).filter(Boolean);"
                + "var cs=getComputedStyle(b);return (b.innerText||'').replace(/\\s+/g,' ').trim().slice(0,400)+' || buttons='+JSON.stringify(btns)+' || bg='+cs.backgroundColor+' border='+cs.borderLeftColor;"));
    }

    private void shot(String name, String caption) throws Exception {
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();o=document.createElement('div');o.id='__qaOverlay';o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;"
                + "max-width:820px;background:rgba(15,27,33,.93);color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap';"
                + "o.textContent=arguments[0];document.body.appendChild(o);", "QA evidence ZP-4464 · " + AppConstants.BASE_URL.replace("https://", "") + " · seat " + seat() + "\n" + caption);
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + "_" + name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();");
    }

    /**
     * Admin seats only (skips itself otherwise): the banner's "View subscription" leads to the page, the Admin
     * rail lists Billing › Subscription, the full API payloads are saved, and the banner is captured at phone width.
     * Run with TZ=America/Chicago in the environment to see the viewer-timezone dates.
     */
    @SuppressWarnings("unchecked")
    @Test(priority = 2, description = "ZP-4464: admin deep checks — View subscription, menu item, API payloads, phone width")
    public void adminDeepChecks() throws Exception {
        ExtentReportManager.createTest("ZP-4464", "Subscription module", "Admin deep checks " + seat());
        List<String> notes = new ArrayList<>();
        open("/dashboard");
        Object tz = js("return Intl.DateTimeFormat().resolvedOptions().timeZone+' · local now '+new Date().toString();");
        notes.add("browser timezone: " + tz);
        String bText = banner();
        notes.add("banner: " + bText);
        // save the raw API answers first, so a plan with no banner (no View subscription button) still leaves evidence
        Map<String, Object> api = (Map<String, Object>) jsAsync("var done=arguments[arguments.length-1];var tok=null;[localStorage,sessionStorage].forEach(function(s){for(var i=0;i<s.length;i++){"
                + "var m=String(s.getItem(s.key(i))).match(/eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+/);if(m&&!tok)tok=m[0];}});var h=tok?{Authorization:'Bearer '+tok}:{};"
                + "function g(u){return fetch(u,{headers:h,credentials:'include'}).then(function(r){return r.text().then(function(b){return {s:r.status,b:b};});});}"
                + "Promise.all([g('/api/subscription/banner'),g('/api/subscription')]).then(function(a){done({banner:a[0],page:a[1]});});");
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + "_api_banner.json"), String.valueOf(((Map<String, Object>) api.get("banner")).get("b")).getBytes());
        Files.write(OUT.resolve(seat() + "_api_subscription.json"), String.valueOf(((Map<String, Object>) api.get("page")).get("b")).getBytes());
        Object clicked = js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /^view subscription$/i.test((x.innerText||'').trim())&&x.offsetParent!==null;});if(!b)return null;b.click();return 'clicked';");
        if (clicked == null) throw new org.testng.SkipException("No 'View subscription' button for seat " + seat() + " (not an admin seat)");
        sleep(5000);
        notes.add("View subscription → " + js("return location.pathname;"));
        Object rail = js("var a=[].slice.call(document.querySelectorAll('a[href=\"/admin/subscription\"]')).filter(function(x){return x.offsetParent!==null;});"
                + "if(!a.length)return 'no visible menu link';var g=a[0];var t='';for(var p=g;p&&!t;p=p.parentElement){var h=[].slice.call(p.parentElement?p.parentElement.children:[]).map(function(c){return (c.innerText||'').trim();}).filter(function(s){return /^billing$/i.test(s);});if(h.length)t='under BILLING';}"
                + "return 'visible menu link: '+(g.innerText||'').trim()+' '+t+' · selected '+(g.getAttribute('aria-current')||g.className.indexOf('active')>=0||g.className.indexOf('selected')>=0);");
        notes.add("menu: " + rail);
        shot("5_after_view_subscription", "View subscription → " + js("return location.pathname;") + "\n" + rail + "\n" + tz);
        // phone width
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(420, 900));
        sleep(2500);
        String phone = banner();
        notes.add("banner at 420 px: " + phone);
        Object overflow = js("return document.documentElement.scrollWidth+' px page width vs '+window.innerWidth+' px viewport';");
        notes.add("phone layout: " + overflow);
        shot("6_phone_width", "420 px wide · " + overflow);
        System.out.println("[ZP-4464 deep] " + seat() + ":\n  " + String.join("\n  ", notes));
        Files.write(OUT.resolve(seat() + "_deep.txt"), String.join("\n", notes).getBytes());
        Assert.assertEquals(String.valueOf(js("return location.pathname;")), "/admin/subscription", "View subscription should open the Subscription page");
    }

    @SuppressWarnings("unchecked")
    @Test(description = "ZP-4464: subscription banner, page and API as seen by the signed-in seat (read-only)")
    public void subscriptionAsThisSeat() throws Exception {
        ExtentReportManager.createTest("ZP-4464", "Subscription module", "Seat " + seat());
        List<String> notes = new ArrayList<>();
        open("/dashboard");

        Map<String, Object> api = (Map<String, Object>) jsAsync("var done=arguments[arguments.length-1];var tok=null;[localStorage,sessionStorage].forEach(function(s){for(var i=0;i<s.length;i++){"
                + "var m=String(s.getItem(s.key(i))).match(/eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+/);if(m&&!tok)tok=m[0];}});var h=tok?{Authorization:'Bearer '+tok}:{};"
                + "function g(u){return fetch(u,{headers:h,credentials:'include'}).then(function(r){return r.text().then(function(b){return {s:r.status,b:b.replace(/\\s+/g,' ').slice(0,1500)};});}).catch(function(e){return {s:'ERR',b:String(e)};});}"
                + "Promise.all([g('/api/auth/v2/me'),g('/api/subscription/banner'),g('/api/subscription')]).then(function(a){done({me:a[0],banner:a[1],page:a[2]});});");
        String me = String.valueOf(((Map<String, Object>) api.get("me")).get("b"));
        String roles = me.replaceAll(".*?\"roles\":\\s*(\\[[^\\]]*\\]).*", "$1");
        notes.add("roles: " + (roles.length() < 300 ? roles : "(not found)") + " · is_eg_admin " + me.replaceAll(".*?\"is_eg_admin\":\\s*(\\w+).*", "$1"));
        Map<String, Object> b = (Map<String, Object>) api.get("banner"), p = (Map<String, Object>) api.get("page");
        notes.add("GET /api/subscription/banner → " + b.get("s") + " " + b.get("b"));
        notes.add("GET /api/subscription → " + p.get("s") + " " + String.valueOf(p.get("b")).substring(0, Math.min(700, String.valueOf(p.get("b")).length())));

        String bDash = banner();
        notes.add("banner on /dashboard: " + bDash);
        Object navItem = js("return [].slice.call(document.querySelectorAll('a[href=\"/admin/subscription\"]')).map(function(a){return (a.innerText||'').trim()+(a.offsetParent?'':' (hidden)');}).join(' | ')||'none';");
        notes.add("menu link to /admin/subscription (in DOM now): " + navItem);
        shot("1_dashboard", "banner: " + bDash);

        open("/sessions");
        String bWo = banner();
        notes.add("banner on /sessions: " + bWo);
        shot("2_work_orders", "banner: " + bWo);

        open("/admin/subscription");
        String path = String.valueOf(js("return location.pathname;"));
        String text = String.valueOf(js("var m=document.querySelector('main')||document.body;return (m.innerText||'').replace(/\\s+/g,' ').slice(0,900);"));
        notes.add("/admin/subscription opened directly → now on " + path + " · page: " + text);
        shot("3_subscription_page", "direct URL /admin/subscription → " + path + "\n" + text.substring(0, Math.min(300, text.length())));
        // the whole page, for seats that can read it: full text plus the lower sections scrolled into view
        if (!text.contains("Access Denied") && text.contains("Modules")) {
            String full = String.valueOf(js("var m=document.querySelector('main')||document.body;return (m.innerText||'');"));
            Files.createDirectories(OUT);
            Files.write(OUT.resolve(seat() + "_page_full.txt"), full.getBytes());
            String[] sections = {"Modules", "Licensed sites", "Modules by site", "Customer portal licences", "Included access", "Activity"};
            for (int k = 0; k < sections.length; k++) {
                Object found = js("var s=arguments[0];var h=[].slice.call(document.querySelectorAll('main *')).find(function(e){return e.children.length===0&&(e.innerText||'').trim()===s;});"
                        + "if(!h)return false;h.scrollIntoView({block:'start'});return true;", sections[k]);
                sleep(800);
                if (Boolean.TRUE.equals(found)) {
                    Files.write(OUT.resolve(seat() + "_4" + (char) ('a' + k) + "_" + sections[k].toLowerCase().replace(' ', '_') + ".png"),
                            ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
                }
            }
        }

        System.out.println("[ZP-4464] " + seat() + ":\n  " + String.join("\n  ", notes));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + ".txt"), String.join("\n", notes).getBytes());
        Assert.assertNotEquals(String.valueOf(b.get("s")), "500", "The banner API must not error for any seat");
        Assert.assertNotEquals(String.valueOf(p.get("s")), "500", "The subscription API must not error for any seat");
    }
}
