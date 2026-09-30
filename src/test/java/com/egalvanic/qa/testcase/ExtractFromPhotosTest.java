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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Edit Asset › "Extract from Photos" (AI nameplate extraction), the way a user runs it: open the
 * asset, open the editor, press the button, wait for the answer.
 *
 * <p>Up to Web v2.2.1 the page sent ONE synchronous request, {@code POST /extraction/extract-nameplate-data
 * {node_ids:[id]}}, which outlived the gateway (~60 s) at about 12 photos and came back as CloudFront's HTML 504
 * (ZP-4463). From Web v2.2.2 it runs a job: {@code POST /extraction/bulk-job/submit} → {@code GET
 * /extraction/bulk-job/status?execution_arn=…} every ~4 s → {@code POST /extraction/nameplate-agent/apply}, which
 * writes the fields. The test records either flow, what the user is shown (and whether it is plain words and on
 * screen), which form fields changed, and — after a failure — whether the asset still changes on the server
 * afterwards (re-read {@code /api/graph/nodes/{id}/enriched} every 15 s). {@code -Dext.simulate} makes the calls fail
 * inside the page without reaching the AI, to check the error path for free.</p>
 *
 * <p>Parameters: {@code ext.asset} (asset id), {@code ext.label}. The extraction itself is an AI call
 * (billable), so each run presses the button exactly once. Nothing is saved: the editor is closed
 * with Cancel (the server-side write of edit-mode extraction still happens — it is the feature).</p>
 */
public class ExtractFromPhotosTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "screenshots", "extract-from-photos");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private static final String RECORDER = "(function(){if(window.__qaRec)return;window.__qaRec=1;"
            + "function log(o){try{var a=JSON.parse(sessionStorage.getItem('__qaExt')||'[]');a.push(o);sessionStorage.setItem('__qaExt',JSON.stringify(a));}catch(e){}}"
            + "var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i),m=(o&&o.method)||(i&&i.method)||'GET',t0=Date.now();var p=f.apply(this,arguments);"
            + "if(/extraction|graph\\/nodes/.test(u)){p.then(function(r){r.clone().text().then(function(b){var st=null,er=null;"
            + "try{var j=JSON.parse(b);st=j.status||null;if(j.results&&j.results[0])er=j.results[0].error||null;}catch(e){}"
            + "log({u:u,m:m,s:r.status,ms:Date.now()-t0,t:t0,st:st,er:er,"
            + "ct:r.headers.get('content-type'),b:b.slice(0,600)});}).catch(function(){log({u:u,m:m,s:r.status,ms:Date.now()-t0,t:t0,b:'(body unreadable)'});});},"
            + "function(e){log({u:u,m:m,s:'ERR',ms:Date.now()-t0,t:t0,b:String(e)});});}return p;};})();";

    // Answers the extraction calls inside the page for -Dext.simulate (installed after RECORDER, so it wraps it; the
    // answers it makes up are logged with sim:true). CloudFront's real 504 is an HTML page — the case of ZP-4463.
    private static final String SIMULATOR = "(function(){if(window.__qaSimExt)return;window.__qaSimExt='__MODE__';var mode='__MODE__',node='__NODE__',polls=0;"
            + "function log(o){try{var a=JSON.parse(sessionStorage.getItem('__qaExt')||'[]');o.sim=true;a.push(o);sessionStorage.setItem('__qaExt',JSON.stringify(a));}catch(e){}}"
            + "var H='<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"><HTML><HEAD><META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">"
            + "<TITLE>ERROR: The request could not be satisfied</TITLE></HEAD><BODY><H1>504 Gateway Timeout ERROR</H1><H2>The request could not be satisfied.</H2></BODY></HTML>';"
            + "function answer(u,m,s,ct,b,delay){var t0=Date.now();return new Promise(function(res){setTimeout(function(){var st=null;try{st=JSON.parse(b).status||null;}catch(e){}"
            + "log({u:u,m:m,s:s,ms:Date.now()-t0,t:t0,st:st,ct:ct,b:b.slice(0,600)});"
            + "res(new Response(b,{status:s,headers:{'content-type':ct}}));},delay);});}"
            + "var J='application/json',fake=JSON.stringify({execution_arn:'arn:aws:states:qa-sim:execution:qa-sim',job_id:'qa-sim',mode:'nameplate',skipped:0,success:true,total:1});"
            + "var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i),m=(o&&o.method)||(i&&i.method)||'GET';"
            + "if(/extraction\\/extract-(temp-)?nameplate-data/.test(u))return answer(u,m,504,'text/html',H,3000);"
            + "if(/extraction\\/bulk-job\\/submit/.test(u))return mode==='submit504'?answer(u,m,504,'text/html',H,3000):answer(u,m,200,J,fake,400);"
            + "if(/extraction\\/bulk-job\\/status/.test(u)){polls++;if(mode==='status504'&&polls>=2)return answer(u,m,504,'text/html',H,1500);"
            + "if(mode==='statusFailed'&&polls>=3)return answer(u,m,200,J,JSON.stringify({done:1,job_id:'qa-sim',status:'FAILED',success:true,results:[{config:null,"
            + "error:'Could not read the nameplate photos (simulated by QA)',label:'QA sim',node_id:node,stage:'Failed',status:'failed'}]}),400);"
            + "return answer(u,m,200,J,JSON.stringify({done:0,job_id:'qa-sim',status:'RUNNING',success:true,results:[]}),400);}"
            + "return f.apply(this,arguments);};})();";

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> recorded() {
        Object r = js("return JSON.parse(sessionStorage.getItem('__qaExt')||'[]');");
        return r instanceof List ? (List<Map<String, Object>>) r : new ArrayList<>();
    }

    /** Label → value of every visible input/select/textarea in the open editor drawer. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> formValues() {
        Object r = js("var d=[].slice.call(document.querySelectorAll('.MuiDrawer-paper,[role=dialog]')).pop()||document;var o={};"
                + "[].slice.call(d.querySelectorAll('input,textarea')).forEach(function(x,i){if(x.type==='file'||x.type==='hidden')return;"
                + "var f=x.closest('.MuiFormControl-root,.MuiTextField-root');var l=f&&f.querySelector('label');var k=(l?l.innerText:(x.name||x.placeholder||('field'+i))).replace(/\\s+/g,' ').trim();"
                + "var v=x.type==='checkbox'?String(x.checked):x.value;if(k)o[k]=v;});return o;");
        return r instanceof Map ? (Map<String, Object>) r : new HashMap<>();
    }

    private String drawerText() {
        return String.valueOf(js("var d=[].slice.call(document.querySelectorAll('.MuiDrawer-paper,[role=dialog]')).pop();return d?(d.innerText||'').replace(/\\s+/g,' ').slice(0,1500):'';"));
    }

    private String alerts() {
        return String.valueOf(js("return [].slice.call(document.querySelectorAll('.MuiAlert-message,.MuiSnackbarContent-message'))"
                + ".map(function(x){return (x.innerText||'').replace(/\\s+/g,' ').trim();}).filter(Boolean).join(' | ');"));
    }

    private String clickButton(String re) {
        return (String) js("var r=new RegExp(arguments[0],'i');var b=[].slice.call(document.querySelectorAll('button,[role=button]')).find(function(x){"
                + "return r.test((x.innerText||x.getAttribute('aria-label')||'').trim())&&!x.disabled&&x.offsetParent!==null;});if(!b)return null;"
                + "b.scrollIntoView({block:'center'});b.click();return (b.innerText||b.getAttribute('aria-label')||'').trim();", re);
    }

    private void shot(String name) throws Exception {
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(name + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }

    private void overlay(String text) {
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();o=document.createElement('div');o.id='__qaOverlay';"
                + "o.style.cssText='position:fixed;left:10px;bottom:10px;z-index:2147483647;max-width:760px;background:rgba(15,27,33,.93);color:#e4edef;"
                + "font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap;pointer-events:none';"
                + "o.textContent=arguments[0];document.body.appendChild(o);", text);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enriched(String id) {
        return (Map<String, Object>) ((JavascriptExecutor) driver).executeAsyncScript(
                "var d=arguments[arguments.length-1];fetch('/api/graph/nodes/'+arguments[0]+'/enriched',{credentials:'include',headers:{Accept:'application/json'}})"
                + ".then(function(r){return r.text().then(function(b){d({s:r.status,b:b});});}).catch(function(e){d({s:'ERR',b:String(e)});});", id);
    }

    @SuppressWarnings("unchecked")
    @Test(description = "Edit Asset › Extract from Photos: one AI extraction, measured end to end")
    @Parameters({"ext.asset", "ext.label"})
    public void extractFromPhotos(@Optional("") String assetId, @Optional("asset") String label) throws Exception {
        ExtentReportManager.createTest("AI extraction", "Extract from Photos", label + " " + assetId);
        Assert.assertFalse(assetId.isEmpty(), "ext.asset (asset id) is required");
        tileWindow(Integer.getInteger("ext.slot", 0) * 380, 0, 1400, 900);
        Map<String, Object> src = new HashMap<>();
        src.put("source", RECORDER);
        ChromeDriver cdp = (ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver();
        cdp.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
        List<String> notes = new ArrayList<>();
        // -Dext.simulate=submit504|status504|statusFailed: make the extraction fail on purpose WITHOUT calling the AI
        // (nothing billable reaches the server — the submit itself is answered in the page). It checks what a user is
        // shown when the gateway times out or the job fails, and that nothing is written to the asset behind the error.
        String simulate = System.getProperty("ext.simulate", "");
        if (!simulate.isEmpty()) {
            Map<String, Object> sim = new HashMap<>();
            sim.put("source", SIMULATOR.replace("__MODE__", simulate).replace("__NODE__", assetId));
            cdp.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", sim);
            notes.add("SIMULATED failure mode: " + simulate + " (the AI is not called)");
        }

        // 1. open the asset and its editor
        driver.get(AppConstants.BASE_URL + "/assets/" + assetId);
        for (int i = 0; i < 40 && js("return document.querySelector('nav a[href]')?1:null;") == null; i++) { loginPage.dismissMfaPromptIfShowing(); sleep(1000); }
        sleep(6000);
        Map<String, Object> before = enriched(assetId);
        // the asset page keeps Edit in its ⋮ menu (top right)
        String edit = clickButton("^edit( asset)?$");
        if (edit == null) {
            // the ⋮ is the icon-only button in the page's top-right corner; on stage it isn't inside main/header, so search the
            // whole document and pick by position (top band, right edge) rather than by container
            // the subscription banner (ZP-4464) now sits above the page header and pushes the ⋮ down to ~96 px, and its own ✕
            // is also an icon button in that corner — so look 140 px down and skip the banner's close button
            js("var W=window.innerWidth;var b=[].slice.call(document.querySelectorAll('button,[role=button]')).filter(function(x){var r=x.getBoundingClientRect();"
                    + "return x.offsetParent!==null&&!(x.innerText||'').trim()&&x.querySelector('svg')&&r.top<140&&r.right>W-200"
                    + "&&!/close subscription notice/i.test(x.getAttribute('aria-label')||'');});"
                    + "b.sort(function(p,q){return q.getBoundingClientRect().right-p.getBoundingClientRect().right;});if(b[0])b[0].click();");
            sleep(900);
            edit = (String) js("var m=[].slice.call(document.querySelectorAll('[role=menuitem],li.MuiMenuItem-root')).find(function(x){return /edit/i.test(x.innerText||'')&&x.offsetParent!==null;});"
                    + "if(!m)return null;m.click();return '⋮ › '+(m.innerText||'').trim();");
        }
        // wait for the editor to show THIS asset (name filled, class chosen) — record how long that takes
        long e0 = System.currentTimeMillis();
        Object filled = null;
        for (int i = 0; i < 60 && filled == null; i++) {
            sleep(500);
            filled = js("var n=[].slice.call(document.querySelectorAll(\"input[placeholder='Enter Asset Name']\")).find(function(x){return x.offsetParent!==null;});"
                    + "return n&&n.value?n.value:null;");
        }
        long editorMs = System.currentTimeMillis() - e0;
        notes.add("editor showed the asset's data after " + editorMs + " ms (" + (filled == null ? "NEVER within 30 s — form stayed empty" : "name '" + filled + "'") + ")");
        if (filled == null) shot(label + "_0_editor_empty");
        sleep(1500);
        String dText = drawerText();
        String nameplateTab = dText.replaceAll(".*?(Nameplate \\(\\d+\\)).*", "$1");
        notes.add("asset " + assetId + " (" + label + "): opened editor via '" + edit + "'; photos tab: " + (nameplateTab.length() < 30 ? nameplateTab : "(not shown)"));
        Map<String, Object> f0 = formValues();
        overlay("QA evidence · Extract from Photos · " + label + " · before · " + (nameplateTab.length() < 30 ? nameplateTab : ""));
        shot(label + "_1_before");
        js("var o=document.getElementById('__qaOverlay');if(o)o.remove();");

        // 2. press Extract from Photos (the main part of the split button) and wait for the answer
        long t0 = System.currentTimeMillis();
        long pressedAt = ((Number) js("return Date.now();")).longValue();   // page clock, to ignore calls made before the press
        // in the Edit Asset header it is a menu button: pick the option like a user does. The first click sometimes lands
        // before the editor is ready and opens nothing (seen on the 12-photo asset), so try up to 3 times.
        String option = System.getProperty("ext.option", "Attributes");
        String pressed = null;
        Object menu = null, picked = null;
        for (int attempt = 1; attempt <= 3 && picked == null; attempt++) {
            pressed = clickButton("^extract from photos$");
            Assert.assertNotNull(pressed, "'Extract from Photos' button not found in the editor");
            for (int i = 0; i < 6 && picked == null; i++) {
                sleep(500);
                menu = js("return [].slice.call(document.querySelectorAll('[role=menuitem]')).filter(function(x){return x.offsetParent!==null;})"
                        + ".map(function(x){return (x.innerText||'').split('\\n')[0].trim();});");
                picked = js("var want=arguments[0];var m=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){"
                        + "return x.offsetParent!==null&&(x.innerText||'').split('\\n')[0].trim()===want;});if(!m)return null;m.click();return want;", option);
            }
            if (picked == null) { notes.add("menu did not open on click " + attempt); js("document.body.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));"); sleep(2000); }
        }
        pressed = pressed + (picked == null ? " (no menu — plain button)" : " › " + picked);
        notes.add("menu options: " + menu);
        pressedAt = ((Number) js("return Date.now();")).longValue() - 1000;
        long pressedWall = System.currentTimeMillis();
        String during = null, buttonDuring = null;
        List<String> progress = new ArrayList<>();
        // the extraction calls a user's press starts: the old synchronous extract-nameplate-data, or (Web v2.2.2) the job
        // flow bulk-job/submit → bulk-job/status (polled) → nameplate-agent/apply. The editor also fires
        // engineering-matches / ocr-signature lookups on its own; those are ignored.
        List<Map<String, Object>> ext = new ArrayList<>(), sync = new ArrayList<>(), submit = new ArrayList<>(), polls = new ArrayList<>(), apply = new ArrayList<>();
        long resultAt = -1;
        String lastStatus = "";
        long limit = Long.getLong("ext.waitMs", 420_000L);
        while (System.currentTimeMillis() - t0 < limit) {
            sleep(1000);
            if (during == null && System.currentTimeMillis() - t0 > 4000) {
                buttonDuring = String.valueOf(js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /extract/i.test(x.innerText||'');});"
                        + "return b?((b.innerText||'').trim()+' disabled='+b.disabled):'(no button)';"));
                shot(label + "_2_during");
                during = buttonDuring;
            }
            // what the page tells the user while the job runs (progress line, stage text)
            String p = String.valueOf(js("var d=[].slice.call(document.querySelectorAll('.MuiDrawer-paper,[role=dialog]')).pop()||document.body;"
                    + "var t=(d.innerText||'').split('\\n').map(function(x){return x.trim();}).filter(function(x){return /reading|extracting|photo\\(s\\)|in progress|queued|running|%/i.test(x)&&x.length<140;});"
                    + "return t.slice(0,3).join(' / ');"));
            if (!p.isEmpty() && !progress.contains(p) && progress.size() < 12) progress.add(((System.currentTimeMillis() - pressedWall) / 1000) + " s: " + p);
            ext.clear(); sync.clear(); submit.clear(); polls.clear(); apply.clear();
            for (Map<String, Object> r : recorded()) {
                if (!(r.get("t") instanceof Number) || ((Number) r.get("t")).longValue() < pressedAt) continue;
                String u = String.valueOf(r.get("u"));
                if (u.matches(".*/extraction/extract-(temp-)?nameplate-data.*")) sync.add(r);
                else if (u.contains("/extraction/bulk-job/submit")) submit.add(r);
                else if (u.contains("/extraction/bulk-job/status")) polls.add(r);
                else if (u.contains("/extraction/nameplate-agent/apply")) apply.add(r);
            }
            ext.addAll(sync); ext.addAll(submit); ext.addAll(polls); ext.addAll(apply);
            // the recorder parses the job's top-level status itself (st): the body it keeps is cut at 600 characters
            if (!polls.isEmpty()) lastStatus = String.valueOf(polls.get(polls.size() - 1).get("st"));
            boolean terminal = lastStatus.matches("SUCCEEDED|FAILED|TIMED_OUT|ABORTED")
                    || (!polls.isEmpty() && !(polls.get(polls.size() - 1).get("s") instanceof Number && ((Number) polls.get(polls.size() - 1).get("s")).intValue() < 400));
            boolean submitFailed = !submit.isEmpty() && !(submit.get(0).get("s") instanceof Number && ((Number) submit.get(0).get("s")).intValue() < 400);
            String msgNow = alerts();
            boolean message = msgNow.toLowerCase().matches("(?s).*(extract|photo|fail|error|could not|couldn|timed|try again).*");
            if (resultAt < 0 && (!sync.isEmpty() || !apply.isEmpty() || terminal || submitFailed || (message && !submit.isEmpty())))
                resultAt = System.currentTimeMillis();
            // stop 20 s after the outcome, so a late apply / reload / second job shows up in the log too
            if (resultAt > 0 && System.currentTimeMillis() - resultAt > 20_000) break;
        }
        long waited = System.currentTimeMillis() - t0;
        sleep(2000);
        String shown = alerts();
        Map<String, Object> f1 = formValues();
        List<String> changed = new ArrayList<>();
        for (String k : f1.keySet()) if (!String.valueOf(f1.get(k)).equals(String.valueOf(f0.get(k)))) changed.add(k + ": '" + f0.get(k) + "' → '" + f1.get(k) + "'");
        // the call that decides the outcome: apply (job flow, success), else the failing poll / submit, else the sync call
        Map<String, Object> main = !apply.isEmpty() ? apply.get(0) : !polls.isEmpty() && !lastStatus.equals("RUNNING") ? polls.get(polls.size() - 1)
                : !submit.isEmpty() && !(submit.get(0).get("s") instanceof Number && ((Number) submit.get(0).get("s")).intValue() < 400) ? submit.get(0)
                : !sync.isEmpty() ? sync.get(0) : !polls.isEmpty() ? polls.get(polls.size() - 1) : !submit.isEmpty() ? submit.get(0) : null;
        String req = main == null ? "(no extraction request recorded within " + waited / 1000 + " s)"
                : main.get("m") + " " + String.valueOf(main.get("u")).replace(AppConstants.BASE_URL, "").replaceAll("\\?.*", "?…") + " → " + main.get("s")
                + " after " + main.get("ms") + " ms (" + main.get("ct") + ")" + (Boolean.TRUE.equals(main.get("sim")) ? " [simulated]" : "");
        String body = main == null ? "" : String.valueOf(main.get("b")).replaceAll("\\s+", " ");
        if (!submit.isEmpty()) {
            long s0 = ((Number) submit.get(0).get("t")).longValue();
            long end = !apply.isEmpty() ? ((Number) apply.get(0).get("t")).longValue() : !polls.isEmpty() ? ((Number) polls.get(polls.size() - 1).get("t")).longValue() : s0;
            notes.add("job flow: submit → " + submit.get(0).get("s") + " (" + submit.get(0).get("ms") + " ms) · " + polls.size() + " status polls, last status "
                    + (lastStatus.isEmpty() ? "-" : lastStatus) + " · apply " + (apply.isEmpty() ? "NOT called" : "→ " + apply.get(0).get("s"))
                    + " · submit→" + (apply.isEmpty() ? "last poll" : "apply") + " " + Math.round((end - s0) / 100.0) / 10.0 + " s · submit calls: " + submit.size());
        }
        notes.add("press → outcome: " + (resultAt < 0 ? "no outcome within " + waited / 1000 + " s" : Math.round((resultAt - pressedWall) / 100.0) / 10.0 + " s"));
        notes.add("progress shown while waiting: " + (progress.isEmpty() ? "(none)" : String.join(" | ", progress)));
        notes.add("pressed '" + pressed + "'; button while waiting: " + buttonDuring);
        notes.add("request: " + req);
        notes.add("response body: " + body.substring(0, Math.min(400, body.length())));
        notes.add("shown to the user: " + (shown.isEmpty() ? "(no alert/toast)" : shown));
        notes.add("form fields changed on screen (" + changed.size() + "): " + String.join("; ", changed));
        // where is the result message? is it on screen without scrolling?
        Object where = js("var a=[].slice.call(document.querySelectorAll('.MuiAlert-root')).find(function(x){return x.offsetParent!==null&&/extract|photo|fail|error|skipped/i.test(x.innerText||'');});"
                + "if(!a)return 'no result message on the page';var r=a.getBoundingClientRect();var sc=a.closest('.MuiDrawer-paper')||document.body;"
                + "return 'message top '+Math.round(r.top)+'px in a '+window.innerHeight+'px window → '+(r.top>=0&&r.bottom<=window.innerHeight?'VISIBLE without scrolling':'OUT OF VIEW (user must scroll)');");
        notes.add("result message position: " + where);
        overlay("QA evidence · Extract from Photos · " + label + "\n" + req + "\nshown: " + (shown.isEmpty() ? "(nothing)" : shown)
                + "\nfields changed on screen: " + changed.size());
        shot(label + "_3_after");
        js("var a=[].slice.call(document.querySelectorAll('.MuiAlert-root')).find(function(x){return x.offsetParent!==null&&/extract|photo|fail|error|skipped/i.test(x.innerText||'');});"
                + "if(a)a.scrollIntoView({block:'center'});");
        sleep(800);
        shot(label + "_4_message");

        // 3b. what a user does next after an error: ext.afterError = save | retry | (none)
        String after = System.getProperty("ext.afterError", "");
        // failed = no call, an HTTP error, a job that ended FAILED/TIMED_OUT/ABORTED, or a job whose results were never applied
        boolean failed = main == null || !(main.get("s") instanceof Number) || ((Number) main.get("s")).intValue() >= 400
                || lastStatus.matches("FAILED|TIMED_OUT|ABORTED") || (!submit.isEmpty() && apply.isEmpty() && sync.isEmpty());
        notes.add("outcome: " + (failed ? "FAILED" : "succeeded"));
        if (failed && after.equals("save")) {
            // wait until the server has written the late result, then press Save Changes on the still-open (stale) form
            String b0 = String.valueOf(before.get("b")), late = b0;
            for (int i = 0; i < 12 && late.equals(b0); i++) { sleep(10000); late = String.valueOf(enriched(assetId).get("b")); }
            notes.add("before pressing Save: server record " + (late.equals(b0) ? "still unchanged" : "already has the late extraction result"));
            Files.write(OUT.resolve(label + "_enriched_late.json"), late.getBytes());
            String saved = clickButton("^save changes$");
            sleep(6000);
            shot(label + "_5_after_save");
            String afterSave = String.valueOf(enriched(assetId).get("b"));
            Files.write(OUT.resolve(label + "_enriched_after_save.json"), afterSave.getBytes());
            notes.add("pressed '" + saved + "' on the stale form; record after save " + (afterSave.equals(late) ? "== late result (kept)" : "DIFFERS from the late result (see json files)"));
        } else if (failed && after.equals("retry")) {
            long r0 = ((Number) js("return Date.now();")).longValue();
            clickButton("^extract from photos$");
            sleep(700);
            js("var m=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){return x.offsetParent!==null&&(x.innerText||'').split('\\n')[0].trim()==='Attributes';});if(m)m.click();");
            Map<String, Object> second = null;
            for (int i = 0; i < 240 && second == null; i++) {
                sleep(1000);
                for (Map<String, Object> r : recorded())
                    if (String.valueOf(r.get("u")).matches(".*/extraction/(extract-nameplate-data|bulk-job/submit).*") && ((Number) r.get("t")).longValue() >= r0) second = r;
            }
            notes.add("retry: " + (second == null ? "no second request" : "second request → " + second.get("s") + " after " + second.get("ms") + " ms · "
                    + String.valueOf(second.get("b")).replaceAll("\\s+", " ").substring(0, Math.min(220, String.valueOf(second.get("b")).length()))));
            // job flow: does the retry start a NEW (paid) job, or pick up the one that is still running (same execution_arn)?
            sleep(15000);
            int subs = 0, pollsAfter = 0;
            java.util.Set<String> arns = new java.util.LinkedHashSet<>();
            for (Map<String, Object> r : recorded()) {
                if (!(r.get("t") instanceof Number) || ((Number) r.get("t")).longValue() < r0) continue;
                String u = String.valueOf(r.get("u"));
                if (u.contains("/extraction/bulk-job/submit")) subs++;
                if (u.contains("/extraction/bulk-job/status")) { pollsAfter++; arns.add(u.replaceAll(".*execution_arn=([^&]*).*", "$1")); }
            }
            notes.add("retry (job flow): new submits " + subs + " · status polls " + pollsAfter + " · polled job(s) " + arns);
            notes.add("shown after retry: " + alerts());
            shot(label + "_5_after_retry");
        }
        if (System.getProperty("ext.option", "Attributes").contains("Library")) {
            sleep(5000);
            Object dlg = js("var d=[].slice.call(document.querySelectorAll('[role=dialog]')).filter(function(x){return x.offsetParent!==null;}).pop();"
                    + "return d?(d.innerText||'').replace(/\\s+/g,' ').slice(0,400):'(no dialog)';");
            List<String> bulk = new ArrayList<>();
            for (Map<String, Object> r : recorded()) if (String.valueOf(r.get("u")).contains("/extraction/bulk-job")) bulk.add(r.get("m") + " " + String.valueOf(r.get("u")).replace(AppConstants.BASE_URL, "") + " → " + r.get("s"));
            notes.add("after 'Attributes + Library': dialog: " + dlg);
            notes.add("bulk-job calls: " + bulk);
            shot(label + "_6_library_dialog");
        }

        // 3. close without saving
        clickButton("^cancel$");
        sleep(1500);
        clickButton("^(discard|discard changes|yes|leave)$");
        sleep(1500);

        // 4. if the extraction failed, does the asset still change on the server afterwards? (ZP-4463: it must not)
        String beforeBody = String.valueOf(before.get("b"));
        String later;
        boolean changedBehindError = false;
        if (failed) {
            int rounds = simulate.isEmpty() ? 12 : 3;   // a simulated failure never reaches the AI, 45 s is enough
            later = "no change on the server within " + rounds * 15 + " s";
            for (int i = 1; i <= rounds; i++) {
                sleep(15000);
                String now = String.valueOf(enriched(assetId).get("b"));
                if (!now.equals(beforeBody)) { later = "asset CHANGED on the server " + (15 * i) + " s after the error"; changedBehindError = true; break; }
            }
        } else {
            String now = String.valueOf(enriched(assetId).get("b"));
            later = now.equals(beforeBody) ? "extraction succeeded, asset record unchanged" : "extraction succeeded, asset record changed";
        }
        notes.add("after the answer: " + later);
        // plain words: none of the technical text of ZP-4463 ("…is not valid JSON", "Unexpected token '<'", the gateway's HTML)
        boolean raw = shown.toLowerCase().matches("(?s).*(json|unexpected token|<!doctype|syntaxerror|failed to execute|gateway|cloudfront|\\b50[234]\\b).*");
        notes.add("message check: " + (shown.isEmpty() ? "NOTHING shown to the user" : raw ? "RAW technical error shown" : "plain words"));
        System.out.println("[EXTRACT] " + label + ":\n  " + String.join("\n  ", notes));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(label + "_notes.txt"), (String.join("\n", notes) + "\n\nall recorded calls:\n" + recorded()).getBytes());
        Files.write(OUT.resolve(label + "_enriched_before.json"), beforeBody.getBytes());
        Assert.assertFalse(ext.isEmpty(), "No extraction request was made");
        Assert.assertFalse(raw, "The user was shown a raw technical error: " + shown);
        Assert.assertFalse(changedBehindError, "The asset changed on the server after the user was told the extraction failed");
        if (simulate.isEmpty()) {
            Assert.assertFalse(failed, "Extraction failed: " + req + " — user saw: " + shown);
        } else {
            Assert.assertTrue(apply.isEmpty(), "Results were applied although the job failed (simulated " + simulate + ")");
            Assert.assertFalse(shown.isEmpty(), "Simulated " + simulate + ": the user was shown nothing");
        }
    }
}
