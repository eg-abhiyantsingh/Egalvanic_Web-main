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
 * Web v2.3 role probe (QA): one login (the seat given by -DUSER_EMAIL / -DUSER_PASSWORD), then the checks named in
 * -Dv23.checks (comma-separated). Read-only: it opens pages, menus and dialogs and closes them again.
 * <ul>
 *   <li>{@code nav} — the rail sections this seat sees, the Maintenance Portal items and their padlocks/tooltips, and the
 *       permissions from /auth/me that the v2.3 tickets depend on (ZP-4148, ZP-4370, ZP-4449).</li>
 *   <li>{@code mp} — what each Maintenance Portal route shows this seat: the page, a permission message, or a redirect
 *       (ZP-4148: staff without Portal Sales should not reach /maintenance-portal/*).</li>
 *   <li>{@code asset} — /assets/{v23.asset}: does the Location card show the location-photos control, and does its
 *       dialog offer Upload / Delete to this seat (ZP-4370: view needs nodes.view, writing needs locations.manage).</li>
 *   <li>{@code report} — /maintenance-portal/reports: maps the page (buttons, selects, report types) for ZP-4449.</li>
 * </ul>
 * Evidence: test-output/v23-role/&lt;seat&gt;_&lt;check&gt;*.png and &lt;seat&gt;_notes.txt.
 */
public class V23RoleProbe extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "v23-role");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    /** -Dv23.checks is a comma list; match whole names so "collapse" does not also run "collapse2". */
    private static boolean hasCheck(String checks, String name) { return java.util.Arrays.asList(checks.split("\\s*,\\s*")).contains(name); }

    private String seat() { return AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1"); }

    private void open(String route) {
        driver.get(AppConstants.BASE_URL + route);
        // dismiss the 2FA chooser once, not on every tick: its self-healing lookups take minutes on pages without it
        sleep(2500);
        if (Boolean.TRUE.equals(js("return /two-factor/i.test((document.body&&document.body.innerText)||'');"))) loginPage.dismissMfaPromptIfShowing();
        for (int i = 0; i < 30; i++) {
            if (Boolean.TRUE.equals(js("var t=(document.body&&document.body.innerText)||'';return !/^\\s*Loading/.test(t)&&document.querySelectorAll('a[href],button').length>3;"))) break;
            sleep(1000);
        }
        sleep(4000);
    }

    private void shot(String name, String caption) throws Exception {
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();o=document.createElement('div');o.id='__qaOverlay';"
                + "o.style.cssText='position:fixed;right:12px;bottom:12px;z-index:2147483647;max-width:760px;background:rgba(15,27,33,.93);color:#e4edef;"
                + "font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap;pointer-events:none';"
                + "o.textContent=arguments[0];document.body.appendChild(o);", "QA · Web v2.3 · " + seat() + " · " + caption);
        Files.write(OUT.resolve(seat() + "_" + name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();");
    }

    private static final String ME = "var d=arguments[arguments.length-1];fetch('/api/auth/me',{credentials:'include',headers:{Accept:'application/json'}})"
            + ".then(function(r){return r.json();}).then(function(j){var u=j.user||j;var p=u.permissions||j.permissions||[];"
            + "var keep=p.filter(function(x){return /portal|location|nodes\\.view|nodes\\.manage|report|sales/i.test(String(x));});"
            + "d('roles '+JSON.stringify((u.roles||[]).map(function(x){return x.name||x;}))+' · '+p.length+' permissions · relevant: '+JSON.stringify(keep));})"
            + ".catch(function(e){d('auth/me failed: '+e);});";

    @Test(description = "Web v2.3: what this seat sees for ZP-4148 / ZP-4370 / ZP-4449")
    public void probe() throws Exception {
        ExtentReportManager.createTest("Web v2.3", "Role probe", seat());
        Files.createDirectories(OUT);
        List<String> notes = new ArrayList<>();
        String checks = System.getProperty("v23.checks", "nav,mp");
        open("/dashboard");
        notes.add("landed on " + js("return location.pathname;") + " · " + ((JavascriptExecutor) driver).executeAsyncScript(ME));
        // the portal gates key off the tier and the user_roles rows (ZP-4148), so record what /features/access says
        notes.add("features/access: " + ((JavascriptExecutor) driver).executeAsyncScript("var d=arguments[arguments.length-1];fetch('/api/features/access',{credentials:'include',headers:{Accept:'application/json'}})"
                + ".then(function(r){return r.text().then(function(t){var j=null;try{j=JSON.parse(t);}catch(e){}if(!j)return d(r.status+' (not JSON)');"
                + "var x=j.data||j;d(r.status+' tier='+JSON.stringify(x.tier)+' user_roles='+JSON.stringify((x.user_roles||[]).map(function(u){return u.name||u.role_name||u;}))"
                + "+' features='+JSON.stringify((x.features||x.company_features||[]).filter(function(f){return /portal/i.test(String(f.key||f.name||f));}).map(function(f){return f.key||f.name||f;})));});})"
                + ".catch(function(e){d('failed '+e);});"));

        if (hasCheck(checks, "nav")) {
            notes.add("rail: " + js("return [].slice.call(document.querySelectorAll('nav button, nav a, [aria-label=navigation] button')).map(function(b){return (b.innerText||b.getAttribute('aria-label')||'').replace(/\\s+/g,' ').trim();}).filter(function(t){return t&&t.length<30;}).join(' | ');"));
            Object mp = js("var b=[].slice.call(document.querySelectorAll('button,a')).find(function(x){return x.offsetParent!==null&&/^Maintenance\\s*Portal$/i.test((x.innerText||'').replace(/\\s+/g,' ').trim());});"
                    + "if(!b)return 'Maintenance Portal: NOT in the rail';b.click();return 'Maintenance Portal: in the rail (clicked)';");
            notes.add(String.valueOf(mp));
            sleep(2500);
            notes.add("after clicking it: " + js("return location.pathname;") + " · panel items: " + js(
                    "return [].slice.call(document.querySelectorAll('a[href*=\"maintenance-portal\"],[href*=\"maintenance-portal\"]')).filter(function(x){return x.offsetParent!==null;})"
                    + ".map(function(x){var lock=!!x.querySelector('svg[data-testid*=\"Lock\"]')||x.getAttribute('aria-disabled')==='true';return (x.innerText||'').replace(/\\s+/g,' ').trim()+(lock?' [LOCKED]':'')+' → '+x.getAttribute('href');}).join(' | ');"));
            notes.add("tooltips/locks on page: " + js("return [].slice.call(document.querySelectorAll('[aria-label],[title]')).map(function(e){return e.getAttribute('aria-label')||e.getAttribute('title');}).filter(function(t){return /permission|contact your admin|locked/i.test(t||'');}).slice(0,4).join(' | ');"));
            shot("nav", "rail after clicking Maintenance Portal · " + js("return location.pathname;"));
        }

        if (hasCheck(checks, "mp")) {
            for (String r : System.getProperty("v23.mpRoutes", "/maintenance-portal,/maintenance-portal/condition,/maintenance-portal/sld,/maintenance-portal/reports").split(",")) {
                open(r);
                String what = String.valueOf(js("var t=(document.querySelector('main')||document.body).innerText||'';var perm=/don.t have permission|not authorized|access denied|contact your admin/i.test(document.body.innerText);"
                        + "return 'now '+location.pathname+(perm?' · PERMISSION MESSAGE':'')+' · headings: '+[].slice.call(document.querySelectorAll('h1,h2,h3,h4,h5,h6')).map(function(h){return h.innerText.trim();}).filter(Boolean).slice(0,4).join(' / ')"
                        + "+' · text: '+t.replace(/\\s+/g,' ').trim().slice(0,160);"));
                notes.add("route " + r + " → " + what);
                shot("mp" + r.replaceAll("[^a-z]", "_"), r + " → " + what.substring(0, Math.min(140, what.length())));
            }
        }

        if (hasCheck(checks, "asset")) {
            String asset = System.getProperty("v23.asset", "eb9bc916-4ba4-4c1d-9096-b46dacdbb154");
            open("/assets/" + asset);
            notes.add("asset page: now " + js("return location.pathname;") + " · title " + js("var h=document.querySelector('main h1,main h2,main h3,main h4,main h5,main h6,h4,h5');return h?h.innerText.trim():'(none)';")
                    + " · location card: " + js("var c=[].slice.call(document.querySelectorAll('*')).find(function(x){return x.children.length<4&&/^LOCATION$/i.test((x.innerText||'').trim());});return c?c.parentElement.innerText.replace(/\\s+/g,' ').trim():'(no Location card)';")
                    + " · control: " + js("var b=document.querySelector('button[aria-label=\"Location Photos\"],button[aria-label=\"Add location photo\"]');return b?b.getAttribute('aria-label'):'(no location-photos control)';"));
            shot("asset_page", "asset page · Location card");
            Object opened = js("var b=document.querySelector('button[aria-label=\"Location Photos\"],button[aria-label=\"Add location photo\"]');if(!b)return false;b.click();return true;");
            if (Boolean.TRUE.equals(opened)) {
                sleep(2500);
                notes.add("dialog: " + js("var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).filter(function(x){return x.offsetParent!==null;}).pop();"
                        + "if(!d)return '(no dialog)';return d.innerText.replace(/\\s+/g,' ').slice(0,200)+' · upload buttons '+[].slice.call(d.querySelectorAll('button,label')).filter(function(b){return /upload photo/i.test(b.innerText||'');}).length"
                        + "+' · file inputs '+d.querySelectorAll('input[type=file]').length+' · photo menus '+d.querySelectorAll('.MuiPaper-root button').length;"));
                shot("asset_dialog", "Location Photos dialog");
                js("var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).filter(function(x){return x.offsetParent!==null;}).pop();if(d){var c=[].slice.call(d.querySelectorAll('button')).find(function(b){return b.innerText.trim()==='Close';});if(c)c.click();}");
            }
        }

        if (hasCheck(checks, "report")) {
            open("/maintenance-portal/reports");
            notes.add("reports page: now " + js("return location.pathname;") + " · buttons: " + js("return [].slice.call(document.querySelectorAll('button')).filter(function(b){return b.offsetParent!==null;}).map(function(b){return (b.innerText||b.getAttribute('aria-label')||'').replace(/\\s+/g,' ').trim();}).filter(function(t){return t&&t.length<40;}).slice(0,30).join(' | ');")
                    + " · inputs: " + js("return [].slice.call(document.querySelectorAll('input')).filter(function(i){return i.offsetParent!==null;}).map(function(i){return (i.placeholder||i.getAttribute('aria-label')||i.name||i.type)+'='+i.value;}).slice(0,10).join(' | ');")
                    + " · text: " + js("return ((document.querySelector('main')||document.body).innerText||'').replace(/\\s+/g,' ').trim().slice(0,300);"));
            shot("reports", "Maintenance Portal › Reports");
        }

        if (hasCheck(checks, "genreport")) {
            // ZP-4449: generate a portal report the way a user does. The final download is caught in the page (its URL is
            // recorded, nothing is saved to disk); every /reporting call is recorded with its status.
            java.util.Map<String, Object> src = new java.util.HashMap<>();
            src.put("source", "(function(){if(window.__qaRep)return;window.__qaRep=1;function log(o){try{var a=JSON.parse(sessionStorage.getItem('__qaRep')||'[]');a.push(o);sessionStorage.setItem('__qaRep',JSON.stringify(a));}catch(e){}}"
                    + "var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i),m=(o&&o.method)||'GET',t0=Date.now();var p=f.apply(this,arguments);"
                    + "if(/reporting|report/.test(u)){p.then(function(r){r.clone().text().then(function(b){log({u:u.replace(location.origin,'').split('?')[0],m:m,s:r.status,ms:Date.now()-t0,b:b.slice(0,300)});});},function(e){log({u:u.split('?')[0],m:m,s:'ERR',b:String(e)});});}return p;};"
                    + "var ac=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){if(this.download||/amazonaws|\\.pdf|\\.docx|blob:/.test(this.href||'')){log({download:(this.href||'').split('?')[0].slice(0,120),name:this.download||''});return;}return ac.apply(this,arguments);};"
                    + "var wo=window.open;window.open=function(u){log({windowOpen:String(u||'').split('?')[0].slice(0,120)});return null;};})();");
            ((org.openqa.selenium.chrome.ChromeDriver) ((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver()).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
            open("/maintenance-portal/reports");
            String site = System.getProperty("v23.site", "test kd 1111");
            org.openqa.selenium.WebElement siteBox = driver.findElements(org.openqa.selenium.By.cssSelector("input[placeholder='Select facility'],input[role=combobox]")).stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
            if (siteBox != null) {
                siteBox.click();
                siteBox.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.COMMAND, "a"));
                siteBox.sendKeys(site);
                sleep(2500);
                Object picked = js("var want=arguments[0];var o=[].slice.call(document.querySelectorAll('[role=option]')).find(function(x){return x.innerText.trim()===want;});"
                        + "if(!o){document.activeElement&&document.activeElement.blur();document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));"
                        + "return 'site option not found (options: '+[].slice.call(document.querySelectorAll('[role=option]')).map(function(x){return x.innerText.trim();}).slice(0,5).join(' | ')+')';}o.click();return 'site '+want;", site);
                notes.add("genreport: " + picked);
                sleep(5000);
            }
            notes.add("genreport: current site now " + js("var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return x.offsetParent!==null&&/facility/i.test(x.placeholder||'');});return i?i.value:'(no site box)';"));
            String card = System.getProperty("v23.card", "Issue Report");
            Object clicked = js("var want=arguments[0];var h=[].slice.call(document.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span,div')).find(function(x){return x.children.length===0&&x.getBoundingClientRect().width>0&&(x.innerText||'').trim()===want;});"
                    + "if(!h)return 'card not found';var c=h.closest('button,[role=button],a,.MuiCard-root,.MuiPaper-root')||h;c.scrollIntoView({block:'center'});c.click();return 'clicked '+c.tagName+'.'+String(c.className).slice(0,30);", card);
            notes.add("genreport: " + card + " → " + clicked);
            // The card opens a "Generate Report" dialog (configuration, format, options); the report starts only from its button.
            sleep(2500);
            shot("genreport_dialog", card + " · dialog");
            Object pressed = js("var d=[].slice.call(document.querySelectorAll('[role=dialog]')).filter(function(x){return x.getBoundingClientRect().width>0;}).pop();if(!d)return 'no dialog';"
                    + "var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^generate( report)?$/i.test((x.innerText||'').trim());});"
                    + "if(!b)return 'dialog without a Generate button: '+d.innerText.replace(/\\s+/g,' ').slice(0,120);if(b.disabled)return 'Generate button disabled';b.click();return 'pressed Generate Report in the dialog';");
            notes.add("genreport: dialog → " + pressed);
            List<String> seen = new ArrayList<>();
            java.util.Set<String> raw = new java.util.HashSet<>();
            boolean shotFirst = false;
            for (int i = 0; i < 120; i++) {
                sleep(1000);
                String t = String.valueOf(js("return [].slice.call(document.querySelectorAll('[data-sonner-toast],.MuiSnackbar-root,.MuiAlert-message,[role=alert],[role=dialog]')).filter(function(x){return x.getBoundingClientRect().width>0;}).map(function(x){return x.innerText.replace(/\\s+/g,' ').trim();}).filter(Boolean).join(' || ');"));
                if (!t.isEmpty() && raw.add(t)) {
                    seen.add(i + " s: " + t);
                    if (!shotFirst) { shot("genreport_first_message", card + " · " + t.substring(0, Math.min(120, t.length()))); shotFirst = true; }
                }
                if (t.matches("(?is).*(report ready|download started|failed|denied|error|could not|couldn't|not allowed).*")) { shot("genreport_outcome", card + " · " + t.substring(0, Math.min(120, t.length()))); sleep(3000); break; }
            }
            notes.add("genreport: messages → " + String.join(" | ", seen));
            notes.add("genreport: calls → " + js("return sessionStorage.getItem('__qaRep');"));
            shot("genreport_end", card + " · end");
        }

        if (hasCheck(checks, "bulkskip")) {
            // ZP-4214: re-run the bulk AI extraction on an asset that was configured AND applied with nothing changed since.
            // It must come back "Skipped — information unchanged since last run" (no paid run). If it runs instead, that is
            // recorded (one paid run on one asset) — the dialog text and the per-node status say which happened.
            js("localStorage.setItem('activeSiteId', arguments[0]);", System.getProperty("v23.sld", "d5d99deb-9554-4774-99d1-16eb1ded0735"));
            open("/equipment-designations");
            String name = System.getProperty("v23.assetName", "Circuit Breaker nameplate");
            org.openqa.selenium.WebElement search = driver.findElements(org.openqa.selenium.By.cssSelector("input[placeholder='Search']")).stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
            if (search == null) { notes.add("bulkskip: no search box on " + js("return location.pathname;")); }
            else {
                search.sendKeys(name);
                sleep(3000);
                notes.add("bulkskip: site " + js("var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return x.getBoundingClientRect().width>0&&x.value;});return i?i.value:'?';")
                        + " · rows " + js("return (document.querySelector('.MuiTablePagination-displayedRows')||{}).innerText||'?';"));
                js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return (x.innerText||'').trim()==='Bulk Ops';});if(b)b.click();");
                sleep(1500);
                Object ticked = js("var want=arguments[0];var r=[].slice.call(document.querySelectorAll('.MuiDataGrid-row,tbody tr')).find(function(x){return (x.innerText||'').indexOf(want)>=0;});"
                        + "var c=r&&r.querySelector('input[type=checkbox]');if(!c)return 'row not found';if(!c.checked)c.click();return 'ticked';", name);
                sleep(1200);
                shot("bulkskip_selected", "Equipment Designations › Bulk Ops · " + name + " ticked");
                Object pressed = js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /^AI Extraction \\(1\\)$/.test((x.innerText||'').trim());});if(!b)return 'no AI Extraction (1) button';b.click();return 'pressed AI Extraction (1)';");
                notes.add("bulkskip: " + ticked + " · " + pressed);
                String last = "";
                for (int i = 0; i < 90; i++) {
                    sleep(1000);
                    String t = String.valueOf(js("var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).pop();return d?d.innerText.replace(/\\s+/g,' ').trim():'';"));
                    if (!t.equals(last)) { notes.add("bulkskip: " + i + " s: " + t.substring(0, Math.min(300, t.length()))); last = t; }
                    if (t.matches("(?is).*(skipped|\\d+ done|failed).*") && !t.matches("(?is).*\\d+ running.*")) { sleep(1500); break; }
                }
                shot("bulkskip_result", "Bulk Extraction Job · " + last.substring(0, Math.min(110, last.length())));
            }
        }

        if (hasCheck(checks, "collapse")) {
            // ZP-4302 / ZP-4301: deletes of assets that are COLLAPSED in a custom SLD view. Test data (all "QA-DEMO … (delete me)")
            // is made through the app's own API from the signed-in page: 5 nodes A–E on a small site, one custom view holding
            // them, and A, B, C, E collapsed in that view (D stays normal). Then the user actions run in the UI:
            // A from the /assets row, B from its asset page, C + D as one bulk delete, E in the SLD editor (must be refused).
            String sld = System.getProperty("v23.sld", "aae9883c-b198-49b7-978d-979cbe045fa4");
            java.util.Map<String, Object> rec = new java.util.HashMap<>();
            rec.put("source", "(function(){if(window.__qaDel)return;window.__qaDel=1;function log(o){try{var a=JSON.parse(sessionStorage.getItem('__qaDel')||'[]');a.push(o);sessionStorage.setItem('__qaDel',JSON.stringify(a));}catch(e){}}"
                    + "var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i),m=(o&&o.method)||'GET';var p=f.apply(this,arguments);"
                    + "if(/node\\/delete|bulk-delete/.test(u)){p.then(function(r){r.clone().text().then(function(b){log({m:m,u:u.replace(location.origin,'').replace(/[0-9a-f-]{36}/g,function(x){return x.slice(0,8);}),s:r.status,b:b.slice(0,240)});});});}return p;};})();");
            ((org.openqa.selenium.chrome.ChromeDriver) ((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver()).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", rec);
            js("localStorage.setItem('activeSiteId', arguments[0]);", sld);
            open("/assets");
            String setup = String.valueOf(((JavascriptExecutor) driver).executeAsyncScript(
                    "var done=arguments[arguments.length-1],sld=arguments[0],tag=arguments[1];"
                    + "var H={'Content-Type':'application/json','X-Direct-Write':'true'};"
                    + "function j(u,o){return fetch(u,Object.assign({credentials:'include'},o||{})).then(function(r){return r.text().then(function(t){var d=null;try{d=JSON.parse(t);}catch(e){}return {s:r.status,d:d,t:t.slice(0,200)};});});}"
                    + "(async function(){try{"
                    + "var list=(await j('/api/lookup/nodes/'+sld)).d;list=Array.isArray(list)?list:(list.data||list.nodes||[]);"
                    + "var tpl=list.find(function(n){return /Panelboard/.test(n.node_class_name||'');})||list[0];"
                    + "var ids={};for(var k of ['A','B','C','D','E']){var body={label:'QA-DEMO ZP-4302 '+k+' '+tag+' (delete me)',sld_id:sld,node_class:tpl.node_class,node_subtype:tpl.node_subtype||null,type:tpl.type||'default',room_id:null,parent_id:null,x:200*'ABCDE'.indexOf(k),y:0,width:150,height:80,default_photo_id:null};"
                    + "var r=await j('/api/node/create',{method:'POST',headers:H,body:JSON.stringify(body)});var n=r.d&&(r.d.data||r.d.node||r.d);if(!n||!n.id){done('create '+k+' failed '+r.s+' '+r.t);return;}ids[k]=n.id;}"
                    + "var v=await j('/api/sld-view/',{method:'POST',headers:H,body:JSON.stringify({sld_id:sld,name:'QA-DEMO ZP-4302 view '+tag+' (delete me)',view_type:'custom'})});var view=v.d&&(v.d.view||v.d.data);if(!view||!view.id){done('view failed '+v.s+' '+v.t);return;}"
                    + "await new Promise(function(r){setTimeout(r,9000);});"
                    + "var add=await j('/api/sld-view/'+view.id+'/nodes/add',{method:'POST',headers:H,body:JSON.stringify({nodes:Object.values(ids)})});"
                    + "var col=[];for(var k2 of ['A','B','C','E']){var c=await j('/api/sld-view/'+view.id+'/nodes/'+ids[k2]+'/collapse',{method:'PUT',headers:H,body:JSON.stringify({is_collapsed:true})});col.push(k2+':'+c.s);}"
                    + "window.__zp4302={ids:ids,view:view.id};sessionStorage.setItem('__zp4302',JSON.stringify(window.__zp4302));"
                    + "done(JSON.stringify({class:tpl.node_class_name,ids:ids,view:view.id,add:add.s+' '+add.t.slice(0,80),collapse:col}));"
                    + "}catch(e){done('setup error '+e);}})();", sld, String.valueOf(System.currentTimeMillis() % 100000)));
            notes.add("collapse setup: " + setup);
            if (setup.startsWith("{")) {
                String a = setup.replaceAll(".*\"A\":\"([^\"]+)\".*", "$1"), b = setup.replaceAll(".*\"B\":\"([^\"]+)\".*", "$1"),
                        c = setup.replaceAll(".*\"C\":\"([^\"]+)\".*", "$1"), d = setup.replaceAll(".*\"D\":\"([^\"]+)\".*", "$1"),
                        e = setup.replaceAll(".*\"E\":\"([^\"]+)\".*", "$1"), view = setup.replaceAll(".*\"view\":\"([^\"]+)\".*", "$1");
                String tag = setup.replaceAll(".*QA-DEMO ZP-4302 A ([0-9]+).*", "$1");
                notes.add("collapse view: " + js("return fetch('/api/sld-view/'+arguments[0]+'/graph',{credentials:'include'}).then(function(r){return r.text();}).then(function(t){return (t.match(/\"is_collapsed\":\\s*true/g)||[]).length+' collapsed of '+(t.match(/\"id\":/g)||[]).length+' ids';});", view));
                // 1) row Delete on /assets for A
                open("/assets");
                String label = "QA-DEMO ZP-4302 A";
                Object rowDel = js("var want=arguments[0];var rows=[].slice.call(document.querySelectorAll('.MuiDataGrid-row'));var r=rows.find(function(x){return (x.innerText||'').indexOf(want)>=0;});"
                        + "if(!r)return 'row not found (rows '+rows.length+')';var b=[].slice.call(r.querySelectorAll('button')).find(function(x){return /delete/i.test(x.title||x.getAttribute('aria-label')||'');});if(!b)return 'no delete button';b.scrollIntoView({block:'center',inline:'center'});b.click();return 'row delete clicked';", label);
                sleep(1500);
                shot("collapse_1_row_delete_confirm", "ZP-4302 · /assets row Delete on collapsed asset A · confirm");
                notes.add("collapse A: " + rowDel + " · dialog " + js("var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).pop();return d?d.innerText.replace(/\\s+/g,' ').slice(0,200):'none';"));
                js("var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).pop();if(d){var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^(delete|confirm|yes)/i.test((x.innerText||'').trim());});if(b)b.click();}");
                sleep(5000);
                shot("collapse_2_row_delete_done", "ZP-4302 · after deleting collapsed asset A from the list");
                notes.add("collapse A after: row still there? " + js("var want=arguments[0];return [].slice.call(document.querySelectorAll('.MuiDataGrid-row')).some(function(x){return (x.innerText||'').indexOf(want)>=0;});", label)
                        + " · toasts " + js("return [].slice.call(document.querySelectorAll('[data-sonner-toast]')).map(function(x){return x.innerText.replace(/\\s+/g,' ');}).join(' | ');"));
                // 2) Delete from the asset page for B
                open("/assets/" + b);
                Object menu = js("var b=[].slice.call(document.querySelectorAll('button')).filter(function(x){return x.getBoundingClientRect().width>0&&x.querySelector('svg')&&!(x.innerText||'').trim();});var m=b.find(function(x){return /more|menu|options/i.test(x.getAttribute('aria-label')||'');})||b[b.length-1];if(!m)return 'no menu';m.click();return 'menu opened';");
                sleep(1000);
                Object delItem = js("var i=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){return /delete/i.test(x.innerText||'');});if(!i)return 'no Delete item: '+[].slice.call(document.querySelectorAll('[role=menuitem]')).map(function(x){return x.innerText.trim();}).join('|');i.click();return 'Delete item clicked';");
                sleep(1500);
                shot("collapse_3_asset_page_delete_confirm", "ZP-4302 · asset page Delete on collapsed asset B · confirm");
                js("var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).pop();if(d){var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^(delete|confirm|yes)/i.test((x.innerText||'').trim());});if(b)b.click();}");
                sleep(5000);
                notes.add("collapse B: " + menu + " · " + delItem + " · now " + js("return location.pathname;") + " · toasts " + js("return [].slice.call(document.querySelectorAll('[data-sonner-toast]')).map(function(x){return x.innerText.replace(/\\s+/g,' ');}).join(' | ');"));
                shot("collapse_4_asset_page_delete_done", "ZP-4302 · after deleting collapsed asset B from its page");
                // 3) bulk delete C (collapsed) + D (normal)
                open("/assets");
                js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return (x.innerText||'').trim()==='Bulk Ops';});if(b)b.click();");
                sleep(1500);
                Object ticks = js("var out=[];['QA-DEMO ZP-4302 C','QA-DEMO ZP-4302 D'].forEach(function(want){var r=[].slice.call(document.querySelectorAll('.MuiDataGrid-row')).find(function(x){return (x.innerText||'').indexOf(want)>=0;});var c=r&&r.querySelector('input[type=checkbox]');if(c&&!c.checked){c.click();out.push(want.slice(-1));}});return out.join('+');");
                sleep(1200);
                Object bulkBtn = js("var b=[].slice.call(document.querySelectorAll('button')).filter(function(x){return x.getBoundingClientRect().width>0;}).find(function(x){return /^delete/i.test((x.innerText||'').trim())||/delete/i.test(x.getAttribute('aria-label')||x.title||'')&&!x.closest('.MuiDataGrid-row');});if(!b)return 'no bulk delete: '+[].slice.call(document.querySelectorAll('button')).map(function(x){return (x.innerText||x.title||'').trim();}).filter(Boolean).slice(0,30).join('|');b.click();return 'bulk delete clicked: '+(b.innerText||b.title);");
                sleep(1500);
                shot("collapse_5_bulk_delete_confirm", "ZP-4302 · bulk delete C (collapsed) + D (normal) · confirm");
                js("var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).pop();if(d){var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^(delete|confirm|yes)/i.test((x.innerText||'').trim());});if(b)b.click();}");
                sleep(7000);
                notes.add("collapse C+D: ticked " + ticks + " · " + bulkBtn + " · left " + js("return [].slice.call(document.querySelectorAll('.MuiDataGrid-row')).filter(function(x){return /QA-DEMO ZP-4302 [CD]/.test(x.innerText||'');}).length;")
                        + " · toasts " + js("return [].slice.call(document.querySelectorAll('[data-sonner-toast]')).map(function(x){return x.innerText.replace(/\\s+/g,' ');}).join(' | ');"));
                shot("collapse_6_bulk_delete_done", "ZP-4302 · after bulk delete of C + D");
                // 4) SLD editor: E is collapsed in the custom view — select it and press Delete: must be refused
                open("/sld");
                sleep(6000);
                Object pickView = js("var want='QA-DEMO ZP-4302 view '+arguments[0];var b=[].slice.call(document.querySelectorAll('button,[role=combobox],[role=button]')).find(function(x){return /All Nodes/.test(x.innerText||'');});if(!b)return 'no view selector';b.click();return 'view menu opened';", tag);
                sleep(1500);
                Object viewItem = js("var want='QA-DEMO ZP-4302 view '+arguments[0];var i=[].slice.call(document.querySelectorAll('[role=menuitem],[role=option],li')).find(function(x){return (x.innerText||'').indexOf(want)>=0;});if(!i)return 'view not listed';i.click();return 'view picked';", tag);
                sleep(6000);
                org.openqa.selenium.WebElement canvas = driver.findElements(org.openqa.selenium.By.cssSelector("canvas")).stream().filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
                String keyRes = "no canvas";
                if (canvas != null) {
                    new org.openqa.selenium.interactions.Actions(((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver()).moveToElement(canvas, 5, 5).click().perform();
                    sleep(500);
                    new org.openqa.selenium.interactions.Actions(((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver()).keyDown(org.openqa.selenium.Keys.COMMAND).sendKeys("a").keyUp(org.openqa.selenium.Keys.COMMAND).perform();
                    sleep(800);
                    shot("collapse_7_sld_view_selected", "ZP-4302 · SLD custom view · collapsed asset E selected");
                    new org.openqa.selenium.interactions.Actions(((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver()).sendKeys(org.openqa.selenium.Keys.DELETE).perform();
                    sleep(1500);
                    keyRes = "delete pressed · toasts " + js("return [].slice.call(document.querySelectorAll('[data-sonner-toast],.MuiSnackbar-root,[role=alert]')).map(function(x){return x.innerText.replace(/\\s+/g,' ');}).join(' | ');");
                    shot("collapse_8_sld_delete_refused", "ZP-4302 · SLD editor · Delete on collapsed E");
                }
                notes.add("collapse E (SLD): " + pickView + " · " + viewItem + " · " + keyRes);
                // 5) ZP-4301: the server's own refusal for a collapsed node when the flag is NOT sent (the named-view message)
                notes.add("collapse E API without allow_collapsed: " + js("return fetch('/api/node/delete/'+arguments[0],{method:'DELETE',credentials:'include',headers:{'X-Direct-Write':'true'}}).then(function(r){return r.text().then(function(t){return r.status+' '+t.replace(/\\s+/g,' ').slice(0,300);});});", e));
                notes.add("collapse deletes seen: " + js("return sessionStorage.getItem('__qaDel');"));
                notes.add("collapse ids: A " + a + " · B " + b + " · C " + c + " · D " + d + " · E " + e + " · view " + view);
            }
        }

        if (hasCheck(checks, "collapse2")) {
            // ZP-4302 / ZP-4301, second pass on the test data the first pass left (A, B, E still collapsed in the QA-DEMO view):
            // A from the /assets row, B from its asset page, E in the SLD editor. Notes are written after every step.
            String sld = System.getProperty("v23.sld", "aae9883c-b198-49b7-978d-979cbe045fa4");
            String a = System.getProperty("v23.a"), b = System.getProperty("v23.b"), e = System.getProperty("v23.e"), view = System.getProperty("v23.view");
            java.util.Map<String, Object> rec = new java.util.HashMap<>();
            rec.put("source", "(function(){if(window.__qaDel)return;window.__qaDel=1;function log(o){try{var a=JSON.parse(sessionStorage.getItem('__qaDel')||'[]');a.push(o);sessionStorage.setItem('__qaDel',JSON.stringify(a));}catch(e){}}"
                    + "var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i),m=(o&&o.method)||'GET';var p=f.apply(this,arguments);"
                    + "if(/node\\/delete|bulk-delete/.test(u)){p.then(function(r){r.clone().text().then(function(b){log({m:m,u:u.replace(location.origin,'').replace(/[0-9a-f-]{36}/g,function(x){return x.slice(0,8);}),s:r.status,b:b.slice(0,240)});});});}return p;};})();");
            org.openqa.selenium.WebDriver raw = ((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver();
            ((org.openqa.selenium.chrome.ChromeDriver) raw).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", rec);
            js("localStorage.setItem('activeSiteId', arguments[0]);", sld);
            String confirm = "var d=[].slice.call(document.querySelectorAll('.MuiDialog-paper')).pop();if(!d)return 'no dialog';var t=d.innerText.replace(/\\s+/g,' ').slice(0,200);"
                    + "var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^(delete|confirm|yes)/i.test((x.innerText||'').trim());});if(!b)return 'dialog without Delete: '+t;b.click();return 'confirmed: '+t;";
            String toasts = "return [].slice.call(document.querySelectorAll('[data-sonner-toast],.MuiSnackbar-root,[role=alert]')).map(function(x){return x.innerText.replace(/\\s+/g,' ');}).join(' | ');";
            // A — row Delete on /assets (the Actions column is virtualised: scroll the grid to the far right first)
            open("/assets");
            js("var s=document.querySelector('.MuiDataGrid-virtualScroller');if(s)s.scrollLeft=s.scrollWidth;");
            sleep(1500);
            Object rowDel = js("var want=arguments[0];var r=document.querySelector('.MuiDataGrid-row[data-id=\"'+want+'\"]');if(!r)return 'row not found';"
                    + "var b=[].slice.call(r.querySelectorAll('button')).find(function(x){return /delete/i.test((x.title||'')+(x.getAttribute('aria-label')||''));});if(!b)return 'no delete button: '+[].slice.call(r.querySelectorAll('button')).map(function(x){return x.title||x.getAttribute('aria-label')||'?';}).join('|');b.click();return 'row Delete clicked ('+(b.title||b.getAttribute('aria-label'))+')';", a);
            sleep(1500);
            shot("collapse2_1_row_delete_confirm", "ZP-4302 · /assets row Delete on collapsed asset A · confirm dialog");
            notes.add("collapse2 A: " + rowDel + " · " + js(confirm));
            sleep(5000);
            notes.add("collapse2 A after: row gone? " + js("return !document.querySelector('.MuiDataGrid-row[data-id=\"'+arguments[0]+'\"]');", a) + " · toasts " + js(toasts) + " · calls " + js("return sessionStorage.getItem('__qaDel');"));
            shot("collapse2_2_row_delete_done", "ZP-4302 · after deleting collapsed asset A from the list");
            Files.write(OUT.resolve(seat() + "_notes.txt"), String.join("\n", notes).getBytes(StandardCharsets.UTF_8));
            // B — ⋮ → Delete Asset on the asset page
            open("/assets/" + b);
            Object menu = js("var b=[].slice.call(document.querySelectorAll('button')).filter(function(x){var r=x.getBoundingClientRect();return r.width>0&&r.top<60&&x.querySelector('svg')&&!(x.innerText||'').trim();}).sort(function(p,q){return q.getBoundingClientRect().left-p.getBoundingClientRect().left;})[0];if(!b)return 'no menu button';b.click();return 'menu opened';");
            sleep(1000);
            Object delItem = js("var i=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){return /^delete/i.test((x.innerText||'').trim());});if(!i)return 'no Delete item';i.click();return 'Delete Asset clicked';");
            sleep(1500);
            shot("collapse2_3_asset_page_delete_confirm", "ZP-4302 · asset page ⋮ → Delete Asset on collapsed asset B · confirm dialog");
            notes.add("collapse2 B: " + menu + " · " + delItem + " · " + js(confirm));
            sleep(5000);
            notes.add("collapse2 B after: now " + js("return location.pathname;") + " · toasts " + js(toasts) + " · calls " + js("return sessionStorage.getItem('__qaDel');"));
            shot("collapse2_4_asset_page_delete_done", "ZP-4302 · after deleting collapsed asset B from its page");
            Files.write(OUT.resolve(seat() + "_notes.txt"), String.join("\n", notes).getBytes(StandardCharsets.UTF_8));
            // E — SLD editor, QA-DEMO view: select all (only E is left in the view) and press Delete → must be refused
            open("/sld");
            sleep(5000);
            Object views = js("return [].slice.call(document.querySelectorAll('button,[role=combobox],[role=button]')).filter(function(x){return x.getBoundingClientRect().width>0&&/All Nodes/.test(x.innerText||'');}).map(function(x){return x.tagName+':'+(x.innerText||'').replace(/\\s+/g,' ').slice(0,40);}).join(' | ');");
            js("var b=[].slice.call(document.querySelectorAll('button,[role=combobox],[role=button]')).filter(function(x){return x.getBoundingClientRect().width>0&&/All Nodes/.test(x.innerText||'');}).pop();if(b)b.click();");
            sleep(1500);
            Object viewItem = js("var want=arguments[0];var i=[].slice.call(document.querySelectorAll('[role=menuitem],[role=option],li')).find(function(x){return (x.innerText||'').indexOf(want)>=0;});if(!i)return 'view not listed: '+[].slice.call(document.querySelectorAll('[role=menuitem],[role=option]')).map(function(x){return x.innerText.trim();}).slice(0,12).join('|');i.click();return 'view picked: '+want;", System.getProperty("v23.viewName", "QA-DEMO ZP-4302 view"));
            sleep(7000);
            shot("collapse2_5_sld_view", "ZP-4302 · SLD › QA-DEMO view · collapsed asset E");
            org.openqa.selenium.WebElement canvas = driver.findElements(org.openqa.selenium.By.cssSelector("canvas")).stream().filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
            String keyRes = "no canvas";
            if (canvas != null) {
                new org.openqa.selenium.interactions.Actions(raw).moveToElement(raw.findElements(org.openqa.selenium.By.cssSelector("canvas")).stream().filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().get(), 10, 10).click().perform();
                sleep(500);
                new org.openqa.selenium.interactions.Actions(raw).keyDown(org.openqa.selenium.Keys.COMMAND).sendKeys("a").keyUp(org.openqa.selenium.Keys.COMMAND).perform();
                sleep(1000);
                Object tip = js("return [].slice.call(document.querySelectorAll('button')).filter(function(x){return x.getBoundingClientRect().width>0&&/collapsed/i.test((x.title||'')+(x.getAttribute('aria-label')||''));}).map(function(x){return (x.title||x.getAttribute('aria-label'));}).join(' | ');");
                shot("collapse2_6_sld_selected", "ZP-4302 · SLD › QA-DEMO view · E selected (⌘A)");
                new org.openqa.selenium.interactions.Actions(raw).sendKeys(org.openqa.selenium.Keys.DELETE).perform();
                sleep(1200);
                keyRes = "delete-control tooltip: " + tip + " · after Delete toasts: " + js(toasts);
                shot("collapse2_7_sld_delete_refused", "ZP-4302 · SLD editor · Delete on collapsed E");
                sleep(2500);
            }
            notes.add("collapse2 E (SLD): selector " + views + " · " + viewItem + " · " + keyRes + " · calls " + js("return sessionStorage.getItem('__qaDel');")
                    + " · E still in list? " + js("var sld=arguments[0],eid=arguments[1];return fetch('/api/lookup/nodes/'+sld,{credentials:'include'}).then(function(r){return r.json();}).then(function(d){var a=Array.isArray(d)?d:(d.data||d.nodes||[]);return a.some(function(n){return n.id===eid;});});", sld, e) + " · view " + view);
            Files.write(OUT.resolve(seat() + "_notes.txt"), String.join("\n", notes).getBytes(StandardCharsets.UTF_8));
            // ZP-4301: the server's own refusal when the flag is NOT sent (this is where the view-naming sentence lives)
            notes.add("collapse2 E API without allow_collapsed: " + js("return fetch('/api/node/delete/'+arguments[0],{method:'DELETE',credentials:'include',headers:{'X-Direct-Write':'true'}}).then(function(r){return r.text().then(function(t){return r.status+' '+t.replace(/\\s+/g,' ').slice(0,300);});});", e));
        }

        if (hasCheck(checks, "sldguard")) {
            // ZP-4302 negative: in the SLD editor a collapsed node must still be refused (toast, no delete call).
            String sld = System.getProperty("v23.sld", "aae9883c-b198-49b7-978d-979cbe045fa4");
            String viewName = System.getProperty("v23.viewName", "QA-DEMO ZP-4302 view");
            java.util.Map<String, Object> rec = new java.util.HashMap<>();
            rec.put("source", "(function(){if(window.__qaDel)return;window.__qaDel=1;function log(o){try{var a=JSON.parse(sessionStorage.getItem('__qaDel')||'[]');a.push(o);sessionStorage.setItem('__qaDel',JSON.stringify(a));}catch(e){}}"
                    + "var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i),m=(o&&o.method)||'GET';var p=f.apply(this,arguments);"
                    + "if(/node\\/delete|bulk-delete/.test(u)){p.then(function(r){log({m:m,u:u.replace(location.origin,'').split('?')[0],s:r.status});});}return p;};})();");
            org.openqa.selenium.WebDriver raw = ((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver();
            ((org.openqa.selenium.chrome.ChromeDriver) raw).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", rec);
            js("localStorage.setItem('activeSiteId', arguments[0]);", sld);
            open("/sld");
            sleep(6000);
            // Edit mode is entered from All Nodes (a custom view has no Edit control), then the view is switched.
            Object edit = "no Edit control in All Nodes after 25 s";
            for (int i = 0; i < 25; i++) {
                Object r = js("var e=[].slice.call(document.querySelectorAll('main button,main [role=button]')).filter(function(x){return (x.innerText||'').trim()==='Edit'&&x.getBoundingClientRect().width>0;});if(!e.length)return null;e[0].click();return 'Edit clicked in All Nodes';");
                if (r != null) { edit = r + " after " + (6 + i) + " s"; break; }
                sleep(1000);
            }
            sleep(3000);
            js("var b=[].slice.call(document.querySelectorAll('button')).filter(function(x){return x.getBoundingClientRect().width>0&&/All Nodes|Current view/.test((x.innerText||'')+(x.getAttribute('aria-label')||''));}).pop();if(b)b.click();");
            sleep(1500);
            Object picked = js("var want=arguments[0];var o=[].slice.call(document.querySelectorAll('div.view-option')).find(function(x){return (x.textContent||'').indexOf(want)>=0;});if(!o)return 'view option not found';o.click();return 'view picked';", viewName);
            sleep(7000);
            Object stillEditing = js("return [].slice.call(document.querySelectorAll('main button,main [role=button]')).filter(function(x){return x.getBoundingClientRect().width>0;}).map(function(x){return (x.innerText||'').trim()||x.getAttribute('aria-label')||'';}).filter(Boolean).slice(0,14).join(' | ');");
            edit = edit + " · controls in the view: " + stillEditing;
            shot("sldguard_1_view_edit_mode", "ZP-4302 · SLD › " + viewName + " · edit mode");
            org.openqa.selenium.WebElement canvas = raw.findElements(org.openqa.selenium.By.cssSelector("canvas")).stream().filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
            String res = "no canvas";
            if (canvas != null) {
                new org.openqa.selenium.interactions.Actions(raw).moveToElement(canvas, 12, 12).click().perform();
                sleep(500);
                new org.openqa.selenium.interactions.Actions(raw).keyDown(org.openqa.selenium.Keys.COMMAND).sendKeys("a").keyUp(org.openqa.selenium.Keys.COMMAND).perform();
                sleep(1200);
                Object tip = js("return [].slice.call(document.querySelectorAll('button,[role=button]')).filter(function(x){return x.getBoundingClientRect().width>0&&/collapsed/i.test((x.title||'')+(x.getAttribute('aria-label')||'')+(x.getAttribute('data-tooltip')||''));}).map(function(x){return x.title||x.getAttribute('aria-label')||x.getAttribute('data-tooltip');}).join(' | ');");
                shot("sldguard_2_selected", "ZP-4302 · SLD › " + viewName + " · all selected (the collapsed QA-DEMO nodes)");
                new org.openqa.selenium.interactions.Actions(raw).sendKeys(org.openqa.selenium.Keys.DELETE).perform();
                sleep(1000);
                Object t1 = js("return [].slice.call(document.querySelectorAll('[data-sonner-toast],.MuiSnackbar-root,[role=alert]')).map(function(x){return x.innerText.replace(/\\s+/g,' ');}).join(' | ');");
                shot("sldguard_3_delete_refused", "ZP-4302 · SLD editor · Delete on collapsed nodes → " + String.valueOf(t1).substring(0, Math.min(90, String.valueOf(t1).length())));
                sleep(3000);
                res = "tooltip: " + tip + " · toasts after Delete: " + t1 + " · delete calls: " + js("return sessionStorage.getItem('__qaDel');");
            }
            notes.add("sldguard: " + picked + " · " + edit + " · " + res);
            notes.add("sldguard: E still in the site list? " + js("var sld=arguments[0],eid=arguments[1];return fetch('/api/lookup/nodes/'+sld,{credentials:'include'}).then(function(r){return r.json();}).then(function(d){var a=Array.isArray(d)?d:(d.data||d.nodes||[]);return a.some(function(n){return n.id===eid;});});", sld, System.getProperty("v23.e", "")));
        }

        if (hasCheck(checks, "sites")) {
            // Which sites this seat can pick (the site picker's full list), and what /auth/me says about site scope.
            open("/assets");
            org.openqa.selenium.WebElement box = driver.findElements(org.openqa.selenium.By.cssSelector("input[placeholder='Select facility'],input[role=combobox]")).stream()
                    .filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
            if (box != null) {
                box.click();
                box.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.COMMAND, "a"), org.openqa.selenium.Keys.BACK_SPACE);
                sleep(2500);
                js("var l=document.querySelector('[role=listbox]');if(l){for(var i=0;i<40;i++){l.scrollTop+=400;}}");
                sleep(1500);
                notes.add("sites: picker " + js("var o=[].slice.call(document.querySelectorAll('[role=option]')).map(function(x){return x.innerText.trim();});return o.length+' options · Android Site 2 listed: '+o.some(function(t){return /^Android Site 2$/.test(t);})+' · first '+o.slice(0,8).join(' | ');"));
                box.sendKeys(org.openqa.selenium.Keys.ESCAPE);
            }
            notes.add("sites: /auth/me " + ((JavascriptExecutor) driver).executeAsyncScript("var d=arguments[arguments.length-1];fetch('/api/auth/me',{credentials:'include'}).then(function(r){return r.json();}).then(function(j){var u=j.user||j.data||j;var ks=Object.keys(u).filter(function(k){return /sld|site|scope|restrict|access/i.test(k);});d(JSON.stringify(ks.map(function(k){var v=u[k];return k+'='+(Array.isArray(v)?('['+v.length+']'):String(v).slice(0,60));})));}).catch(function(e){d('err '+e);});"));
        }

        if (hasCheck(checks, "shots")) {
            // Plain page captures for tickets whose proof is an API or a page state: -Dv23.shots="route|caption;route|caption".
            for (String pair : System.getProperty("v23.shots", "").split(";")) {
                if (pair.trim().isEmpty()) continue;
                String[] p = pair.split("\\|", 2);
                if (p[0].startsWith("site=")) { js("localStorage.setItem('activeSiteId', arguments[0]);", p[0].substring(5)); continue; }
                open(p[0]);
                String label = p.length > 1 ? p[1] : p[0];
                shot("shot_" + label.replaceAll("[^A-Za-z0-9]+", "_").toLowerCase(), label + " · " + js("return location.pathname;"));
                notes.add("shot: " + p[0] + " → " + js("return location.pathname;") + " · " + js("return ((document.querySelector('main')||document.body).innerText||'').replace(/\\s+/g,' ').trim().slice(0,160);"));
            }
        }

        String all = String.join("\n", notes);
        System.out.println("[V23] " + seat() + ":\n  " + String.join("\n  ", notes));
        Files.write(OUT.resolve(seat() + "_notes.txt"), all.getBytes(StandardCharsets.UTF_8));
        Assert.assertFalse(notes.isEmpty());
    }
}
