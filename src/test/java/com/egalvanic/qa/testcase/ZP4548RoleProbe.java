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
 * ZP-4548 (Simplify Subscription Page), read-only, for the signed-in seat (run once per role with -DUSER_EMAIL).
 * Records what this seat gets from GET /api/subscription and the new GET /api/subscription/t2-accounts, and what
 * /admin/subscription shows. Admin seats must get the T2 card; every other seat must be refused by both the page
 * and the T2 endpoint. Nothing is clicked except page navigation. Evidence: test-output/zp4548-roles/&lt;seat&gt;*.
 */
public class ZP4548RoleProbe extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "zp4548-roles");
    private static final String OLD_WORDS = "Interactive|Read-only|Read-Only|No license|No License|Rate card|Sept 2026|Renewal options|Platform users|AI users";

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

    private void shot(String name) throws Exception {
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + "_" + name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }

    @SuppressWarnings("unchecked")
    @Test(description = "ZP-4548: Subscription page + T2 accounts API as seen by the signed-in seat (read-only)")
    public void subscriptionAndT2AsThisSeat() throws Exception {
        ExtentReportManager.createTest("ZP-4548", "Simplify Subscription Page", "Seat " + seat());
        List<String> notes = new ArrayList<>();
        open("/dashboard");

        Map<String, Object> api = (Map<String, Object>) jsAsync("var done=arguments[arguments.length-1];var tok=null;[localStorage,sessionStorage].forEach(function(s){for(var i=0;i<s.length;i++){"
                + "var m=String(s.getItem(s.key(i))).match(/eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+/);if(m&&!tok)tok=m[0];}});var h=tok?{Authorization:'Bearer '+tok}:{};"
                // roles / is_eg_admin / t2 are read from the parsed FULL body; b is only a short excerpt for the notes
                + "function g(u){return fetch(u,{headers:h,credentials:'include'}).then(function(r){var ct=r.headers.get('content-type')||'';return r.text().then(function(b){var j=null;try{j=JSON.parse(b);}catch(e){}"
                + "return {s:r.status,ct:ct,b:b.replace(/\\s+/g,' ').slice(0,600),full:b,hasT2:!!(j&&j.t2),roles:j&&j.roles?JSON.stringify(j.roles):'(none)',eg:j?String(j.is_eg_admin):'(none)'};});}).catch(function(e){return {s:'ERR',ct:'',b:String(e),full:''};});}"
                + "Promise.all([g('/api/auth/v2/me'),g('/api/subscription'),g('/api/subscription/t2-accounts?page=1&page_size=10')]).then(function(a){done({me:a[0],page:a[1],t2:a[2]});});");
        Map<String, Object> meMap = (Map<String, Object>) api.get("me");
        String roles = String.valueOf(meMap.get("roles"));
        String egAdmin = String.valueOf(meMap.get("eg"));
        notes.add("roles: " + roles + " · is_eg_admin " + egAdmin);
        Map<String, Object> p = (Map<String, Object>) api.get("page"), t2 = (Map<String, Object>) api.get("t2");
        String pBody = String.valueOf(p.get("b")), t2Body = String.valueOf(t2.get("b"));
        boolean pageJson = String.valueOf(p.get("ct")).contains("json"), t2Json = String.valueOf(t2.get("ct")).contains("json");
        notes.add("GET /api/subscription → " + p.get("s") + " " + p.get("ct") + " · has t2: " + p.get("hasT2")
                + " · " + (pageJson ? pBody.substring(0, Math.min(160, pBody.length())) : "(not JSON)"));
        notes.add("GET /api/subscription/t2-accounts → " + t2.get("s") + " " + t2.get("ct") + " · "
                + (t2Json ? t2Body.substring(0, Math.min(220, t2Body.length())) : "(not JSON)"));

        open("/admin/subscription");
        String path = String.valueOf(js("return location.pathname;"));
        String text = String.valueOf(js("var m=document.querySelector('main')||document.body;return (m.innerText||'').replace(/\\s+/g,' ').trim();"));
        boolean adminMsg = text.contains("Only administrators can view the subscription");
        boolean t2Card = text.contains("T2 licenses");
        String oldHits = String.valueOf(js("var m=document.querySelector('main')||document.body;var t=m.innerText||'';var r=new RegExp(arguments[0],'g');return (t.match(r)||[]).join(', ');", OLD_WORDS));
        notes.add("/admin/subscription → path " + path + " · admin-only message: " + adminMsg + " · T2 card: " + t2Card
                + " · old words: [" + oldHits + "]");
        notes.add("page text: " + text.substring(0, Math.min(400, text.length())));
        shot("admin_subscription");

        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + ".txt"), String.join("\n", notes).getBytes());
        // full payloads stay in the local test-output folder (they carry prices; never committed)
        Files.write(OUT.resolve(seat() + "_api_subscription.json"), String.valueOf(p.get("full")).getBytes());
        Files.write(OUT.resolve(seat() + "_api_t2.json"), String.valueOf(t2.get("full")).getBytes());
        System.out.println("[ZP-4548 roles] " + seat() + ":\n  " + String.join("\n  ", notes));

        // roles come as names or as {name: ...} objects depending on the endpoint version
        boolean isAdmin = "true".equals(egAdmin) || roles.matches(".*\"(Admin|Super Admin|EG Admin)\".*");
        Assert.assertEquals(oldHits, "", "Old tier / rate-card words on the Subscription page");
        if (isAdmin) {
            Assert.assertEquals(String.valueOf(t2.get("s")), "200", "Admin seat should read the T2 accounts");
            Assert.assertTrue(t2Card, "Admin seat should see the T2 licenses card");
        } else {
            Assert.assertFalse("200".equals(String.valueOf(t2.get("s"))) && t2Json && t2Body.contains("\"accounts\""),
                    "Non-admin seat must not read the T2 accounts list: " + t2.get("s") + " " + t2Body.substring(0, Math.min(160, t2Body.length())));
            Assert.assertFalse(t2Card, "Non-admin seat must not see the T2 licenses card");
        }
    }
}
