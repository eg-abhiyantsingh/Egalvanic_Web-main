package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZP-4548 extras, read-only, admin seat: (1) the Updates (release notes) entries — the design lists "the Updates entry
 * text" as a place the tier rename applies; (2) the Subscription page at phone width (420 px) — does the new T2 card or
 * the Foundation Modules card overflow; (3) the same page in America/Chicago — midnight-UTC subscription dates shown in
 * a US timezone. Evidence and notes: test-output/zp4548-extras/.
 */
public class ZP4548ExtrasProbe extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "zp4548-extras");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private ChromeDriver cdp() { return (ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver(); }

    private void open(String path) {
        driver.get(AppConstants.BASE_URL + path);
        for (int i = 0; i < 30; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('a[href],button').length>5;"))) break;
            sleep(1000);
        }
        sleep(6000);
    }

    private void shot(String name) throws Exception {
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }

    private void scrollTo(String cardTitle) {
        js("var w=arguments[0];var el=[].slice.call(document.querySelectorAll('main .MuiPaper-root *')).find(function(e){return e.childElementCount===0&&(e.textContent||'').trim()===w;});"
                + "if(el){el.scrollIntoView({block:'start'});var sc=el.parentElement;while(sc&&!(sc.scrollHeight>sc.clientHeight+5&&/(auto|scroll)/.test(getComputedStyle(sc).overflowY)))sc=sc.parentElement;if(sc)sc.scrollTop-=12;}", cardTitle);
        sleep(700);
    }

    /** Dates and day counts the Subscription page shows: header line, tiles, the Subscription card and the Modules access notes. */
    private String datesOnPage() {
        return String.valueOf(js("var m=document.querySelector('main');var t=(m.innerText||'');var head=(t.match(/Order[^\\n]*/)||[''])[0];"
                + "var tiles=(t.match(/(FULL ACCESS LEFT|TERM ENDS|TERM|NEXT SITE EXPIRY|NEXT MODULE EXPIRY|ANNUAL LICENSE FEES|ANNUAL FEES)\\n[^\\n]*\\n[^\\n]*/g)||[]).map(function(x){return x.replace(/\\n/g,' · ');});"
                + "var card=(t.match(/\\nFull access\\n\\n[^\\n]*/)||[''])[0].replace(/\\n+/g,' ');var term=(t.match(/\\nTerm\\n\\n[^\\n]*/)||[''])[0].replace(/\\n+/g,' ');"
                + "var mods=(t.match(/(Expires in \\d+ days?|Until [A-Z][a-z]{2} \\d+, \\d{4}|Through [A-Z][a-z]{2} \\d+, \\d{4})/g)||[]).slice(0,4);"
                + "return JSON.stringify({head:head,tiles:tiles,card:card.trim(),term:term.trim(),mods:mods});"));
    }

    @Test(description = "ZP-4548: Updates entries, phone width and a US timezone on the Subscription page (read-only)")
    public void extras() throws Exception {
        ExtentReportManager.createTest("ZP-4548", "Simplify Subscription Page", "Extras: updates, phone, timezone");
        List<String> notes = new ArrayList<>();

        // 1. Updates (release notes)
        open("/release-updates");
        String upd = String.valueOf(js("return ((document.querySelector('main')||document.body).innerText||'').replace(/\\s+/g,' ').trim();"));
        Object frames = js("return [].slice.call(document.querySelectorAll('iframe')).map(function(f){var h='';try{h=new URL(f.src).host;}catch(e){}var txt='';try{txt=(f.contentDocument&&f.contentDocument.body&&f.contentDocument.body.innerText)||'';}catch(e){txt='(cross-origin)';}return h+' '+f.clientWidth+'x'+f.clientHeight+' '+String(txt).replace(/\\s+/g,' ').slice(0,300);}).join(' | ');");
        notes.add("Updates page " + js("return location.pathname;") + " text: " + upd.substring(0, Math.min(500, upd.length())));
        notes.add("Updates frames: " + frames);
        notes.add("Updates old tier words: " + (upd.matches("(?s).*(Interactive|Read-only|Read-Only|No license|No License).*") ? "FOUND" : "none in page text"));
        shot("1_updates_page");
        Object railBtn = js("var b=[].slice.call(document.querySelectorAll('button,a,[role=button]')).find(function(x){return /^Updates/.test((x.innerText||'').trim());});if(!b)return 'no Updates rail button';b.click();return 'clicked';");
        sleep(5000);
        notes.add("Updates rail button: " + railBtn + " → " + js("return location.pathname;"));
        shot("2_updates_after_rail_click");

        // 2. Phone width
        Map<String, Object> m = new HashMap<>();
        m.put("width", 420); m.put("height", 900); m.put("deviceScaleFactor", 1); m.put("mobile", true);
        cdp().executeCdpCommand("Emulation.setDeviceMetricsOverride", m);
        open("/admin/subscription");
        Object layout = js("var d=document.documentElement;var t2=[].slice.call(document.querySelectorAll('main table')).map(function(tb){var p=tb.parentElement;return tb.scrollWidth+'/'+p.clientWidth;});"
                + "return 'page '+d.scrollWidth+' px wide in a '+window.innerWidth+' px viewport · tables (content/box) '+t2.join(', ');");
        notes.add("phone 420 px: " + layout);
        shot("3_phone_top");
        scrollTo("Modules"); shot("4_phone_modules");
        scrollTo("T2 licenses"); shot("5_phone_t2");
        Object t2cells = js("var t=[].slice.call(document.querySelectorAll('main .MuiPaper-root')).find(function(c){return /^T2 licenses/.test((c.innerText||'').trim());});if(!t)return 'no T2 card';"
                + "return [].slice.call(t.querySelectorAll('tbody tr')).slice(0,3).map(function(r){return [].slice.call(r.querySelectorAll('td')).map(function(c){return c.scrollWidth>c.clientWidth+1?'CLIPPED('+c.innerText.split('\\n')[0]+')':'ok';}).join('/');}).join(' | ');");
        notes.add("phone T2 cells (clipped?): " + t2cells);
        cdp().executeCdpCommand("Emulation.clearDeviceMetricsOverride", new HashMap<>());

        // 3. US timezone
        open("/admin/subscription");
        String ist = datesOnPage();
        notes.add("dates in " + js("return Intl.DateTimeFormat().resolvedOptions().timeZone;") + ": " + ist);
        Map<String, Object> tz = new HashMap<>();
        tz.put("timezoneId", "America/Chicago");
        cdp().executeCdpCommand("Emulation.setTimezoneOverride", tz);
        driver.navigate().refresh();
        sleep(8000);
        loginPage.dismissMfaPromptIfShowing();
        sleep(2000);
        notes.add("dates in " + js("return Intl.DateTimeFormat().resolvedOptions().timeZone;") + ": " + datesOnPage());
        shot("6_chicago_top");

        Files.createDirectories(OUT);
        Files.write(OUT.resolve("notes.txt"), String.join("\n", notes).getBytes());
        System.out.println("[ZP-4548 extras]\n  " + String.join("\n  ", notes));
    }
}
