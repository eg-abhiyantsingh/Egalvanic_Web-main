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
 * <p>In edit mode the page sends ONE synchronous request, {@code POST /extraction/extract-nameplate-data
 * {node_ids:[id]}}; the server reads every nameplate photo of the asset with AI before it answers, and
 * writes the extracted values itself (the page then re-reads the asset). A request that outlives the
 * API gateway (~30 s) comes back 504. The test records that request's status, duration and body, what
 * the user is shown, which form fields changed, and — after a failure — whether the asset still
 * changes on the server afterwards (re-read {@code /api/graph/nodes/{id}/enriched} every 15 s for 3 min).</p>
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
            + "if(/extraction|graph\\/nodes/.test(u)){p.then(function(r){r.clone().text().then(function(b){log({u:u,m:m,s:r.status,ms:Date.now()-t0,t:t0,"
            + "ct:r.headers.get('content-type'),b:b.slice(0,600)});}).catch(function(){log({u:u,m:m,s:r.status,ms:Date.now()-t0,t:t0,b:'(body unreadable)'});});},"
            + "function(e){log({u:u,m:m,s:'ERR',ms:Date.now()-t0,t:t0,b:String(e)});});}return p;};})();";

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
        ((ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver()).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
        List<String> notes = new ArrayList<>();

        // 1. open the asset and its editor
        driver.get(AppConstants.BASE_URL + "/assets/" + assetId);
        for (int i = 0; i < 40 && js("return document.querySelector('nav a[href]')?1:null;") == null; i++) { loginPage.dismissMfaPromptIfShowing(); sleep(1000); }
        sleep(6000);
        Map<String, Object> before = enriched(assetId);
        // the asset page keeps Edit in its ⋮ menu (top right)
        String edit = clickButton("^edit( asset)?$");
        if (edit == null) {
            js("var b=[].slice.call(document.querySelectorAll('main button,header button')).filter(function(x){return x.offsetParent!==null&&!(x.innerText||'').trim()&&x.querySelector('svg')"
                    + "&&x.getBoundingClientRect().top<90;});var k=b[b.length-1];if(k)k.click();");
            sleep(800);
            edit = (String) js("var m=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){return /edit/i.test(x.innerText||'')&&x.offsetParent!==null;});"
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
        String pressed = clickButton("^extract from photos$");
        Assert.assertNotNull(pressed, "'Extract from Photos' button not found in the editor");
        // in the Edit Asset header it is a menu button: pick the option like a user does
        String option = System.getProperty("ext.option", "Attributes");
        sleep(700);
        Object menu = js("return [].slice.call(document.querySelectorAll('[role=menuitem]')).filter(function(x){return x.offsetParent!==null;})"
                + ".map(function(x){return (x.innerText||'').split('\\n')[0].trim();});");
        Object picked = js("var want=arguments[0];var m=[].slice.call(document.querySelectorAll('[role=menuitem]')).find(function(x){"
                + "return x.offsetParent!==null&&(x.innerText||'').split('\\n')[0].trim()===want;});if(!m)return null;m.click();return want;", option);
        pressed = pressed + (picked == null ? " (no menu — plain button)" : " › " + picked);
        notes.add("menu options: " + menu);
        pressedAt = ((Number) js("return Date.now();")).longValue() - 1000;
        String during = null, buttonDuring = null;
        List<Map<String, Object>> ext = new ArrayList<>();
        while (System.currentTimeMillis() - t0 < 240_000) {
            sleep(1000);
            if (during == null && System.currentTimeMillis() - t0 > 4000) {
                buttonDuring = String.valueOf(js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /extract/i.test(x.innerText||'');});"
                        + "return b?((b.innerText||'').trim()+' disabled='+b.disabled):'(no button)';"));
                shot(label + "_2_during");
                during = buttonDuring;
            }
            ext.clear();
            // only the extraction itself (extract-nameplate-data in edit mode), started after the press;
            // the editor also fires engineering-matches / ocr-signature lookups on its own
            for (Map<String, Object> r : recorded())
                if (String.valueOf(r.get("u")).matches(".*/extraction/extract-(temp-)?nameplate-data.*")
                        && r.get("t") instanceof Number && ((Number) r.get("t")).longValue() >= pressedAt) ext.add(r);
            if (!ext.isEmpty()) { sleep(3000); break; }
        }
        long waited = System.currentTimeMillis() - t0;
        sleep(2000);
        String shown = alerts();
        String dAfter = drawerText();
        Map<String, Object> f1 = formValues();
        List<String> changed = new ArrayList<>();
        for (String k : f1.keySet()) if (!String.valueOf(f1.get(k)).equals(String.valueOf(f0.get(k)))) changed.add(k + ": '" + f0.get(k) + "' → '" + f1.get(k) + "'");
        String req = ext.isEmpty() ? "(no extraction request recorded within " + waited / 1000 + " s)"
                : ext.get(0).get("m") + " " + String.valueOf(ext.get(0).get("u")).replace(AppConstants.BASE_URL, "") + " → " + ext.get(0).get("s")
                + " after " + ext.get(0).get("ms") + " ms (" + ext.get(0).get("ct") + ")";
        String body = ext.isEmpty() ? "" : String.valueOf(ext.get(0).get("b")).replaceAll("\\s+", " ");
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
        Object st0 = ext.isEmpty() ? null : ext.get(0).get("s");
        boolean failed = !(st0 instanceof Number) || ((Number) st0).intValue() >= 400;
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
                    if (String.valueOf(r.get("u")).contains("/extraction/extract-nameplate-data") && ((Number) r.get("t")).longValue() >= r0) second = r;
            }
            notes.add("retry: " + (second == null ? "no second request" : "second request → " + second.get("s") + " after " + second.get("ms") + " ms · "
                    + String.valueOf(second.get("b")).replaceAll("\\s+", " ").substring(0, Math.min(220, String.valueOf(second.get("b")).length()))));
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

        // 4. if the request failed, does the asset still change on the server afterwards?
        Object status = ext.isEmpty() ? null : ext.get(0).get("s");
        String beforeBody = String.valueOf(before.get("b"));
        String later = "not checked (request succeeded)";
        if (!(status instanceof Number) || ((Number) status).intValue() >= 400) {
            later = "no change on the server within 180 s";
            for (int i = 1; i <= 12; i++) {
                sleep(15000);
                String now = String.valueOf(enriched(assetId).get("b"));
                if (!now.equals(beforeBody)) { later = "asset CHANGED on the server " + (15 * i) + " s after the error"; break; }
            }
        } else {
            String now = String.valueOf(enriched(assetId).get("b"));
            later = now.equals(beforeBody) ? "request succeeded, asset record unchanged" : "request succeeded, asset record changed";
        }
        notes.add("after the answer: " + later);
        System.out.println("[EXTRACT] " + label + ":\n  " + String.join("\n  ", notes));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(label + "_notes.txt"), (String.join("\n", notes) + "\n\nall recorded calls:\n" + recorded()).getBytes());
        Files.write(OUT.resolve(label + "_enriched_before.json"), beforeBody.getBytes());
        Assert.assertFalse(ext.isEmpty(), "No extraction request was made");
        Assert.assertTrue(status instanceof Number && ((Number) status).intValue() < 400, "Extraction failed: " + req + " — user saw: " + shown);
    }
}
