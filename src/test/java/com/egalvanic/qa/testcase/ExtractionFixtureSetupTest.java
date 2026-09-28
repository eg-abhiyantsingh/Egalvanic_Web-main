package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
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
 * Builds a labelled QA-DEMO asset with N nameplate photos through the real web path (Assets ›
 * Create Asset › Asset Photos › Nameplate › Upload Nameplate › Create Asset), for the AI-extraction
 * tests. Optionally presses "Extract from Photos" BEFORE saving — the create-mode path
 * ({@code POST /extraction/extract-temp-nameplate-data} with the uploaded photo URLs) — and records it.
 *
 * <p>Parameters: {@code fx.name}, {@code fx.class}, {@code fx.photos} (comma-separated absolute paths),
 * {@code fx.createExtract} (true/false). Prints {@code [FIXTURE] <name> -> <asset id>}.</p>
 */
public class ExtractionFixtureSetupTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "screenshots", "extract-from-photos");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private String nameplateTab() {
        return String.valueOf(js("var t=[].slice.call(document.querySelectorAll('[role=tab],button')).find(function(x){return /^Nameplate \\(\\d+\\)$/.test((x.innerText||'').trim());});"
                + "return t?(t.innerText||'').trim():'';"));
    }

    @SuppressWarnings("unchecked")
    @Test(description = "Create a QA-DEMO asset with N nameplate photos (and optionally extract before saving)")
    @Parameters({"fx.name", "fx.class", "fx.photos", "fx.createExtract"})
    public void createAssetWithNameplates(@Optional("QA-DEMO extract fixture (delete me)") String name,
                                          @Optional("Circuit Breaker") String cls,
                                          @Optional("") String photos,
                                          @Optional("false") String createExtract) throws Exception {
        ExtentReportManager.createTest("AI extraction", "Fixture", name);
        String[] files = photos.split(",");
        tileWindow(Integer.getInteger("ext.slot", 0) * 380, 0, 1400, 900);
        Map<String, Object> src = new HashMap<>();
        src.put("source", "(function(){if(window.__qaRec)return;window.__qaRec=1;function log(o){try{var a=JSON.parse(sessionStorage.getItem('__qaExt')||'[]');a.push(o);"
                + "sessionStorage.setItem('__qaExt',JSON.stringify(a));}catch(e){}}var f=window.fetch;window.fetch=function(i,o){var u=String(i&&i.url?i.url:i),"
                + "m=(o&&o.method)||(i&&i.method)||'GET',t0=Date.now();var p=f.apply(this,arguments);if(/extraction|photo\\/create|s3\\/url/.test(u)){p.then(function(r){"
                + "r.clone().text().then(function(b){log({u:u,m:m,s:r.status,ms:Date.now()-t0,b:b.slice(0,600)});}).catch(function(){});},function(e){log({u:u,m:m,s:'ERR',ms:Date.now()-t0});});}return p;};})();");
        ((ChromeDriver) ((SelfHealingDriver) driver).getWrappedDriver()).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);

        // load the page fresh so the request recorder (installed for NEW documents) is in place
        driver.get(com.egalvanic.qa.constants.AppConstants.BASE_URL + "/assets");
        sleep(5000);
        assetPage.navigateToAssets();
        sleep(3000);
        assetPage.openCreateAssetForm();
        sleep(1500);
        assetPage.fillBasicInfo(name, "", cls);
        sleep(1500);

        // Asset Photos › Nameplate tab › Upload Nameplate (a hidden multi-file input behind a label)
        js("var t=[].slice.call(document.querySelectorAll('[role=tab],button')).find(function(x){return /^Nameplate \\(\\d+\\)$/.test((x.innerText||'').trim());});"
                + "if(t){t.scrollIntoView({block:'center'});t.click();}");
        sleep(1000);
        Object inputId = js("var l=[].slice.call(document.querySelectorAll('label[for]')).find(function(x){return /upload nameplate/i.test(x.innerText||'');});"
                + "if(!l)return null;var i=document.getElementById(l.getAttribute('for'));if(!i)return null;i.style.display='block';i.style.opacity='1';return i.id;");
        Assert.assertNotNull(inputId, "Upload Nameplate input not found in the Create Asset drawer (tab: " + nameplateTab() + ")");
        WebElement input = driver.findElement(By.id(String.valueOf(inputId)));
        input.sendKeys(String.join("\n", files));
        String tab = "";
        for (int i = 0; i < 90; i++) {
            sleep(1000);
            tab = nameplateTab();
            if (tab.equals("Nameplate (" + files.length + ")") && js("return document.querySelector('.MuiCircularProgress-root')?1:null;") == null) break;
        }
        System.out.println("[FIXTURE] " + name + ": uploaded " + files.length + " → tab shows '" + tab + "'");
        Files.createDirectories(OUT);
        Files.write(OUT.resolve("fixture_" + files.length + "_uploaded.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));

        if (Boolean.parseBoolean(createExtract)) {        // create-mode extraction before saving
            long t0 = System.currentTimeMillis();
            Object pressed = js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /^extract from photos$/i.test((x.innerText||'').trim())&&!x.disabled;});"
                    + "if(!b)return null;b.click();return 'ok';");
            Map<String, Object> req = null;
            while (pressed != null && System.currentTimeMillis() - t0 < 240_000) {
                sleep(1000);
                for (Map<String, Object> r : (List<Map<String, Object>>) js("return JSON.parse(sessionStorage.getItem('__qaExt')||'[]');"))
                    if (String.valueOf(r.get("u")).contains("extract-temp-nameplate-data")) req = r;
                if (req != null) break;
            }
            sleep(2000);
            String shown = String.valueOf(js("return [].slice.call(document.querySelectorAll('.MuiAlert-message,[role=alert]')).map(function(x){return (x.innerText||'').replace(/\\s+/g,' ').trim();}).join(' | ');"));
            System.out.println("[FIXTURE] create-mode extract: " + (req == null ? "(no request within 240 s)" : req.get("s") + " after " + req.get("ms") + " ms · body "
                    + String.valueOf(req.get("b")).replaceAll("\\s+", " ").substring(0, Math.min(300, String.valueOf(req.get("b")).length()))) + " · shown: " + shown);
            Files.write(OUT.resolve("fixture_" + files.length + "_create_mode_extract.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        }

        assetPage.submitCreateAsset();
        boolean ok = assetPage.waitForCreateSuccess();
        sleep(2000);
        // find the new asset's id: search it, open it
        assetPage.searchAsset(name);
        sleep(3000);
        assetPage.navigateToFirstAssetDetail();
        sleep(3000);
        String url = driver.getCurrentUrl();
        String id = url.replaceAll(".*/assets/([0-9a-f-]{36}).*", "$1");
        Object count = ((JavascriptExecutor) driver).executeAsyncScript("var d=arguments[arguments.length-1];fetch('/api/photo/by_entity/'+arguments[0],{credentials:'include'})"
                + ".then(function(r){return r.json();}).then(function(a){d(a.filter(function(p){return p.type==='node_nameplate'&&!p.is_deleted;}).length);}).catch(function(e){d('ERR '+e);});", id);
        System.out.println("[FIXTURE] " + name + " -> " + id + " · created " + ok + " · nameplate photos on the server: " + count + " · " + url);
        Files.write(OUT.resolve("fixture_" + files.length + "_id.txt"), (name + "\n" + id + "\n" + count + "\n").getBytes());
        Assert.assertTrue(ok, "Asset was not created");
        Assert.assertEquals(String.valueOf(count), String.valueOf(files.length), "Nameplate photos on the server");
    }
}
