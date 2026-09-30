package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Diagnostic: on an asset page, list the icon buttons in the top band and the menu that the ⋮ opens, so tests that
 * open Edit Asset can target the right control on a new build. Read-only (opens a menu, closes it with Escape).
 */
public class AssetMenuProbe extends BaseTest {

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    @Test
    @Parameters({"ext.asset"})
    public void probe(@Optional("") String assetId) throws Exception {
        driver.get(AppConstants.BASE_URL + "/assets/" + assetId);
        for (int i = 0; i < 40 && js("return document.querySelector('nav a[href]')?1:null;") == null; i++) { loginPage.dismissMfaPromptIfShowing(); sleep(1000); }
        sleep(6000);
        System.out.println("[PROBE] url " + driver.getCurrentUrl() + " · window " + js("return innerWidth+'x'+innerHeight;"));
        System.out.println("[PROBE] top-band controls: " + js(
                "return [].slice.call(document.querySelectorAll('button,[role=button],a')).filter(function(x){var r=x.getBoundingClientRect();return x.offsetParent!==null&&r.top<100&&r.width>0;})"
                + ".map(function(x){var r=x.getBoundingClientRect();return x.tagName+'['+(x.getAttribute('aria-label')||'')+'|'+(x.innerText||'').trim().slice(0,20)+'|'+String(x.className).slice(0,40)+'] @'+Math.round(r.left)+','+Math.round(r.top)+' '+Math.round(r.width)+'x'+Math.round(r.height)+' svg='+!!x.querySelector('svg');}).join('\\n  ');"));
        Object clicked = js("var W=innerWidth;var b=[].slice.call(document.querySelectorAll('button,[role=button]')).filter(function(x){var r=x.getBoundingClientRect();"
                + "return x.offsetParent!==null&&!(x.innerText||'').trim()&&x.querySelector('svg')&&r.top<100&&r.right>W-200;});"
                + "b.sort(function(p,q){return q.getBoundingClientRect().right-p.getBoundingClientRect().right;});if(!b[0])return 'nothing to click';b[0].click();"
                + "var r=b[0].getBoundingClientRect();return 'clicked '+(b[0].getAttribute('aria-label')||'(no label)')+' @'+Math.round(r.left)+','+Math.round(r.top);");
        System.out.println("[PROBE] " + clicked);
        sleep(1200);
        System.out.println("[PROBE] menus/popovers now: " + js(
                "return [].slice.call(document.querySelectorAll('[role=menu],[role=menuitem],.MuiPopover-paper,.MuiMenu-paper,.MuiPopper-root,li')).filter(function(x){return x.offsetParent!==null;})"
                + ".map(function(x){return x.tagName+'.'+String(x.className).slice(0,30)+'[role='+x.getAttribute('role')+'] \"'+(x.innerText||'').replace(/\\s+/g,' ').trim().slice(0,60)+'\"';}).slice(0,25).join('\\n  ');"));
        Files.createDirectories(Paths.get("test-output", "screenshots", "extract-from-photos"));
        Files.write(Paths.get("test-output", "screenshots", "extract-from-photos", "probe_menu_open.png"),
                ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
}
