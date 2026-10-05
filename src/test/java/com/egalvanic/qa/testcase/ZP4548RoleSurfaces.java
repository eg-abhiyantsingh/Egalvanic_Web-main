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

/**
 * ZP-4548 tier-name surfaces as seen by the signed-in seat (run once per role with -DUSER_EMAIL). For each place the
 * ticket renames tiers, records whether the role can open it and what it shows: banner, Admin › Subscription, Guest
 * Portal Users, Customers (+ an account's details, its ⋮ menu, Manage License options), Create Customer license options,
 * the Maintenance Portal license picker. Read-only: dialogs are opened and cancelled, nothing is saved. Fails when an old
 * tier name ("Interactive", "Read-only", "No license") shows anywhere it looked. Evidence: test-output/zp4548-surfaces/.
 */
public class ZP4548RoleSurfaces extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "zp4548-surfaces");
    private static final String OLD = "Interactive|Read-only|Read-Only|No license|No License|Lecture seule|Aucune licence";

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    private String seat() { return AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1"); }

    private void open(String path) {
        driver.get(AppConstants.BASE_URL + path);
        for (int i = 0; i < 30; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('a[href],button').length>5;"))) break;
            sleep(1000);
        }
        sleep(5000);
    }

    private String main() { return String.valueOf(js("return ((document.querySelector('main')||document.body).innerText||'').replace(/\\s+/g,' ').trim();")); }
    private String dialog() { return String.valueOf(js("var d=document.querySelector('[role=dialog]');return d?(d.innerText||'').replace(/\\s+/g,' ').trim():'';")); }
    private String oldHits(String text) { java.util.regex.Matcher m = java.util.regex.Pattern.compile(OLD).matcher(text); List<String> h = new ArrayList<>(); while (m.find()) h.add(m.group()); return String.join(", ", h); }
    private String state(String text) {
        if (text.contains("Access Denied")) return "Access Denied";
        if (text.contains("Web Access Restricted")) return "Web Access Restricted";
        if (text.contains("Only administrators can view")) return "admins only message";
        return "opens";
    }
    private void shot(String name) throws Exception {
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + "_" + name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
    private void cancelDialog() {
        js("var d=document.querySelector('[role=dialog]');if(!d)return;var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^Cancel$/.test((x.innerText||'').trim());});if(b)b.click();");
        sleep(800);
    }
    private void closeMenu() { js("var b=document.querySelector('.MuiBackdrop-invisible');if(b)b.click();"); sleep(400); }

    @Test(description = "ZP-4548: tier-name surfaces and the Subscription page as seen by the signed-in seat (read-only)")
    public void surfacesAsThisSeat() throws Exception {
        ExtentReportManager.createTest("ZP-4548", "Simplify Subscription Page", "Tier surfaces · " + seat());
        List<String> notes = new ArrayList<>();
        List<String> old = new ArrayList<>();

        open("/dashboard");
        Object banner = js("var e=[].slice.call(document.querySelectorAll('a[href^=\"mailto:customer-success\"]')).find(function(a){return a.offsetParent!==null;});"
                + "if(!e)return 'none';var b=e;for(var i=0;i<6&&b.parentElement;i++){b=b.parentElement;if(b.querySelector('button'))break;}"
                + "var btns=[].slice.call(b.querySelectorAll('button')).map(function(x){return (x.innerText||'').trim()||'['+(x.getAttribute('aria-label')||'icon')+']';});return (b.innerText||'').replace(/\\s+/g,' ').trim()+' || buttons '+JSON.stringify(btns);");
        notes.add("banner on /dashboard: " + banner);
        String webBlocked = main().contains("Web Access Restricted") ? "yes" : "no";
        notes.add("web access restricted: " + webBlocked);

        // 1. Admin › Subscription
        open("/admin/subscription");
        String t = main();
        notes.add("Subscription page: " + state(t) + (t.contains("T2 licenses") ? " · T2 card shown" : "") + " · " + t.substring(0, Math.min(160, t.length())));
        if (!oldHits(t).isEmpty()) old.add("subscription: " + oldHits(t));
        shot("1_subscription");

        // 2. Guest Portal Users
        open("/guest-portal-users");
        t = main();
        Object chips = js("return [].slice.call(document.querySelectorAll('.MuiDataGrid-row [data-field=license_type]')).map(function(c){return (c.innerText||'').trim();}).join(', ');");
        notes.add("Guest Portal Users: " + state(t) + " · License column: [" + chips + "]");
        if (!oldHits(t).isEmpty()) old.add("guest portal users: " + oldHits(t));
        shot("2_guest_portal_users");

        // 3. Customers list → Create Customer license options → an account's details, ⋮ menu, Manage License options
        open("/customers");
        t = main();
        notes.add("Customers: " + state(t));
        if (!oldHits(t).isEmpty()) old.add("customers: " + oldHits(t));
        Object newBtn = js("var b=[].slice.call(document.querySelectorAll('main button')).find(function(x){return /^New Customer$/.test((x.innerText||'').trim());});if(!b)return 'no New Customer button';b.click();return 'clicked';");
        if ("clicked".equals(newBtn)) {
            sleep(1800);
            String d = dialog();
            int i = d.indexOf("LICENSE TYPE"); if (i < 0) i = d.indexOf("License Type");
            notes.add("Create Customer → " + (i >= 0 ? d.substring(i, Math.min(d.length(), i + 330)) : "(no license section) " + d.substring(0, Math.min(120, d.length()))));
            if (!oldHits(d).isEmpty()) old.add("create customer: " + oldHits(d));
            js("var d=document.querySelector('[role=dialog]');if(!d)return;var s=[].slice.call(d.querySelectorAll('*')).find(function(e){return e.scrollHeight>e.clientHeight+20&&getComputedStyle(e).overflowY!=='visible';});if(s)s.scrollTop=s.scrollHeight;");
            sleep(500);
            shot("3_create_customer");
            cancelDialog();
        } else {
            notes.add("Create Customer: " + newBtn);
        }
        Object view = js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /^Actions for /.test(x.getAttribute('aria-label')||'')&&/Paloma Thomas/.test(x.getAttribute('aria-label'));})"
                + "||[].slice.call(document.querySelectorAll('button')).find(function(x){return /^Actions for /.test(x.getAttribute('aria-label')||'');});if(!b)return 'no account actions';b.click();return b.getAttribute('aria-label');");
        if (!String.valueOf(view).startsWith("no ")) {
            sleep(800);
            Object items = js("return [].slice.call(document.querySelectorAll('[role=menuitem]')).map(function(m){return (m.innerText||'').trim();}).join(' / ');");
            notes.add("list " + view + " menu: " + items);
            Object went = js("var m=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){return /^View Customer$/.test((x.innerText||'').trim());});if(!m)return 'no View Customer';m.click();return 'ok';");
            if (!"ok".equals(went)) closeMenu();
            sleep(4500);
        } else {
            notes.add("Customers: " + view);
        }
        if (String.valueOf(js("return location.pathname;")).startsWith("/accounts/")) {
            t = main();
            int i = t.indexOf("License Type");
            notes.add("account " + js("return location.pathname;") + " → " + (i >= 0 ? t.substring(i, Math.min(t.length(), i + 26)) : "(no License Type) " + state(t)));
            if (!oldHits(t).isEmpty()) old.add("account details: " + oldHits(t));
            shot("4_account_details");
            Object kebab = js("var bs=[].slice.call(document.querySelectorAll('main button')).filter(function(b){var r=b.getBoundingClientRect();return r.top<160&&!((b.innerText||'').trim())&&!b.getAttribute('aria-label')&&b.querySelector('svg');});"
                    + "var k=bs[bs.length-1];if(!k)return 'no ⋮';k.click();return 'clicked';");
            if ("clicked".equals(kebab)) {
                sleep(700);
                Object items = js("return [].slice.call(document.querySelectorAll('[role=menuitem]')).map(function(m){return (m.innerText||'').trim();}).join(' / ');");
                notes.add("account ⋮ menu: " + items);
                Object ml = js("var m=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){return /^Manage License$/.test((x.innerText||'').trim());});if(!m)return 'no Manage License';m.click();return 'ok';");
                if ("ok".equals(ml)) {
                    sleep(1500);
                    String d = dialog();
                    notes.add("Manage License → " + d.substring(0, Math.min(330, d.length())));
                    if (!oldHits(d).isEmpty()) old.add("manage license: " + oldHits(d));
                    shot("5_manage_license");
                    cancelDialog();
                } else {
                    notes.add("Manage License: " + ml);
                    closeMenu();
                }
            } else {
                notes.add("account ⋮: " + kebab);
            }
        }

        // 4. Maintenance Portal license picker
        open("/maintenance-portal/condition");
        t = main();
        Object picker = js("var l=[].slice.call(document.querySelectorAll('*')).find(function(e){return e.childElementCount<=2&&/^LICENSE$/i.test(((e.firstChild&&e.firstChild.textContent)||'').trim());});if(!l)return 'no LICENSE picker';"
                + "var box=l.parentElement;var c=box.querySelector('[role=combobox]')||box.parentElement.querySelector('[role=combobox]');return 'LICENSE picker shows '+(c?(c.innerText||'').trim():'(no combobox)');");
        notes.add("Maintenance Portal: " + state(t) + " · " + picker + " · " + t.substring(0, Math.min(140, t.length())));
        Object opts = js("var c=[].slice.call(document.querySelectorAll('[role=combobox]')).find(function(x){return /^(Free|Lite|Full Access|Interactive|Read-only|No license)$/.test((x.innerText||'').trim())&&x.getBoundingClientRect().width>0;});"
                + "if(!c)return null;c.dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));return 'opened';");
        if (opts != null) {
            sleep(800);
            Object o = js("return [].slice.call(document.querySelectorAll('[role=option]')).map(function(x){return (x.innerText||'').trim();}).join(' / ');");
            notes.add("license picker options: " + o);
            if (!oldHits(String.valueOf(o)).isEmpty()) old.add("portal picker: " + oldHits(String.valueOf(o)));
            shot("6_portal_license_picker");
            js("var s=[].slice.call(document.querySelectorAll('[role=option]')).find(function(x){return x.getAttribute('aria-selected')==='true';});if(s)s.click();");
            sleep(500);
        } else {
            shot("6_maintenance_portal");
        }
        if (!oldHits(t).isEmpty()) old.add("maintenance portal: " + oldHits(t));

        notes.add("old tier names seen: " + (old.isEmpty() ? "none" : String.join(" · ", old)));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(seat() + ".txt"), String.join("\n", notes).getBytes());
        System.out.println("[ZP-4548 surfaces] " + seat() + ":\n  " + String.join("\n  ", notes));
        Assert.assertTrue(old.isEmpty(), "Old tier names shown to " + seat() + ": " + old);
    }
}
