package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZP-4444 "[Web] Work Order Loading Time" — QA plan sections 1 and 3, in a real browser.
 *
 * <p>Opens a work order at /sessions/{id}, measures how long until the asset grid shows rows,
 * reads the Resource Timing of every API call (server wait = responseStart − requestStart), and
 * checks which calls fire before the grid is visible. Repeats the open three times (plan 1.4).</p>
 *
 * <p>Pass criteria from the plan: grid in ~1–2 s on a small work order; assets/v2 server wait
 * &lt; 1 s; plain GET /ir_session/{id}, /session-work-blocks/session/{id}, /node_classes/user/{id} and
 * /reporting/configs?type=session NOT before the grid; /ir_session/{id}/full?include=header exactly
 * once; /location-readiness/v2 once per open.</p>
 */
public class ZP4444WorkOrderLoadTest extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "screenshots", System.getProperty("zp4444.out", "zp4444"));

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private static final String GRID_ROWS = "[role='row'][data-rowindex], .MuiDataGrid-row, table tbody tr";

    /** Timestamps the first grid row the moment React inserts it (polling from Java lags by up to ~0.3 s). */
    private void installGridObserver() {
        java.util.Map<String, Object> src = new java.util.HashMap<>();
        src.put("source", "(function(){var sel=\"" + GRID_ROWS + "\";function chk(){if(window.__qaGridAt)return true;"
                + "if(document.querySelector(sel)){window.__qaGridAt=Math.round(performance.now());return true;}return false;}"
                + "new MutationObserver(function(m,o){if(chk())o.disconnect();}).observe(document,{childList:true,subtree:true});})();");
        ((org.openqa.selenium.chrome.ChromeDriver) ((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver())
                .executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
    }

    /** One open of the work order: returns grid time, the API calls, and the server wait of assets/v2. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> openOnce(String woId) {
        driver.get(AppConstants.BASE_URL + "/sessions/" + woId);
        long t0 = System.currentTimeMillis();
        Object gridMs = null;
        for (int i = 0; i < 120; i++) {
            loginPage.dismissMfaPromptIfShowing();
            gridMs = js("if(window.__qaGridAt) return window.__qaGridAt;"
                    + "return document.querySelector(\"" + GRID_ROWS + "\") ? Math.round(performance.now()) : null;");
            if (gridMs != null) break;
            sleep(250);
        }
        long wall = System.currentTimeMillis() - t0;
        sleep(6000); // let the calls that come after the grid finish, so they are in the log
        Map<String, Object> r = (Map<String, Object>) js(
                "var grid=arguments[0];var out=[];performance.getEntriesByType('resource').forEach(function(e){"
                + "if(e.name.indexOf('/api/')<0) return; var u=e.name.replace(location.origin,'').replace(/[0-9a-f]{8}-[0-9a-f-]{27}/g,'{id}');"
                + "out.push({u:u,start:Math.round(e.startTime),wait:Math.round((e.responseStart||0)-(e.requestStart||0)),end:Math.round(e.responseEnd),dur:Math.round(e.duration),before:grid!=null&&e.startTime<grid});});"
                + "return {grid:grid,calls:out};", gridMs);
        r.put("wall", wall);
        return r;
    }

    private static int count(List<Map<String, Object>> calls, String regex, boolean beforeOnly) {
        int n = 0;
        for (Map<String, Object> c : calls) {
            if (beforeOnly && !Boolean.TRUE.equals(c.get("before"))) continue;
            if (String.valueOf(c.get("u")).matches(regex)) n++;
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    @Test(description = "ZP-4444 plan §1 + §3: grid time, assets/v2 server wait, and calls on open")
    @Parameters({"zp4444.wo", "zp4444.label"})
    public void openWorkOrder(@Optional("f532bf11-41e0-4b50-b310-bb82237d0d9b") String woId,
                              @Optional("small") String label) throws Exception {
        ExtentReportManager.createTest("ZP-4444", "Work order loading time", label + " " + woId);
        // run tiled when three browsers work at once, so every window stays visible
        int slot = Integer.getInteger("zp4444.slot", label.startsWith("small") ? 0 : label.startsWith("large") ? 1 : 2);
        tileWindow(slot * 380, slot * 40, 1100, 820);
        installGridObserver();

        List<String> summary = new ArrayList<>();
        List<Map<String, Object>> firstCalls = null;
        List<Object> grids = new ArrayList<>(), waits = new ArrayList<>(), woGrids = new ArrayList<>();
        for (int run = 1; run <= 3; run++) {
            Map<String, Object> r = openOnce(woId);
            List<Map<String, Object>> calls = (List<Map<String, Object>>) r.get("calls");
            if (run == 1) firstCalls = calls;
            Object assetsWait = calls.stream().filter(c -> String.valueOf(c.get("u")).contains("/assets/v2")).map(c -> c.get("wait")).findFirst().orElse(null);
            // app start-up (auth, features, site list) is the same on every page; the work order's own
            // loading starts at its first /ir_session/{id} call, so grid minus that is the ticket's number
            Long woStart = calls.stream().filter(c -> String.valueOf(c.get("u")).contains("/ir_session/{id}"))
                    .map(c -> ((Number) c.get("start")).longValue()).min(Long::compare).orElse(null);
            Object woToGrid = woStart != null && r.get("grid") instanceof Number ? ((Number) r.get("grid")).longValue() - woStart : null;
            grids.add(r.get("grid")); waits.add(assetsWait); woGrids.add(woToGrid);
            System.out.println("[ZP-4444] " + label + " run " + run + ": grid visible at " + r.get("grid") + " ms after navigation, " + woToGrid + " ms after the work order's first call (wall " + r.get("wall") + " ms); assets/v2 server wait " + assetsWait + " ms; " + calls.size() + " API calls");
        }
        summary.add(label + " work order " + woId.substring(0, 8) + " — grid " + woGrids + " ms after the work order's first call ("
                + grids + " ms after page load), assets/v2 server wait " + waits + " ms (3 opens)");

        String id = "\\{id\\}";
        int plainSession = count(firstCalls, "/api/ir_session/" + id, true);
        int workBlocks = count(firstCalls, ".*/session-work-blocks/session/" + id + ".*", true);
        int nodeClassesUser = count(firstCalls, ".*/node_classes/user/" + id + ".*", true);
        int reportConfigs = count(firstCalls, ".*/reporting/configs\\?type=session.*", true);
        int header = count(firstCalls, ".*/ir_session/" + id + "/full\\?include=header.*", false);
        int fullAny = count(firstCalls, ".*/ir_session/" + id + "/full.*", false);
        int readiness = count(firstCalls, ".*/location-readiness/v2.*", false);
        int deferredEarly = count(firstCalls, ".*/api/(lookup/(nodes|procedures|node-classes)|issue_classes/user|model/(task|issue|node)/schema).*", true);
        String[][] rows = {
                {"plain GET /ir_session/{id} before grid (should be 0)", String.valueOf(plainSession)},
                {"/session-work-blocks/session/{id} before grid (should be 0)", String.valueOf(workBlocks)},
                {"/node_classes/user/{id} before grid (should be 0)", String.valueOf(nodeClassesUser)},
                {"/reporting/configs?type=session before grid (should be 0)", String.valueOf(reportConfigs)},
                {"/ir_session/{id}/full?include=header (should be 1)", String.valueOf(header)},
                {"/ir_session/{id}/full (any form)", String.valueOf(fullAny)},
                {"/location-readiness/v2 per open (should be 1)", String.valueOf(readiness)},
                {"lookup/*, issue_classes/user, model/*/schema started before grid (plan 3.3: should be 0)", String.valueOf(deferredEarly)}};
        for (String[] rw : rows) summary.add(rw[0] + ": " + rw[1]);
        StringBuilder all = new StringBuilder();
        for (Map<String, Object> c : firstCalls) {
            all.append(Boolean.TRUE.equals(c.get("before")) ? "  before-grid " : "  after-grid  ")
               .append(String.format("%6s–%-6s ms wait %5s ms  ", c.get("start"), c.get("end"), c.get("wait"))).append(c.get("u")).append('\n');
        }
        System.out.println("[ZP-4444] " + label + " checks:\n  " + String.join("\n  ", summary) + "\n[ZP-4444] " + label + " calls (first open):\n" + all);

        js("var o=document.createElement('div');o.id='__qaOverlay';o.style.cssText='position:fixed;right:10px;bottom:10px;z-index:2147483647;max-width:720px;"
                + "background:rgba(15,27,33,.93);color:#e4edef;font:12px/1.5 Menlo,monospace;padding:10px 12px;border-radius:6px;border:2px solid #4fbacb;white-space:pre-wrap';"
                + "o.textContent=arguments[0];document.body.appendChild(o);", "QA evidence ZP-4444 · " + String.join("\n", summary));
        Files.createDirectories(OUT);
        Files.write(OUT.resolve(label + "_" + woId.substring(0, 8) + ".png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        Files.write(OUT.resolve(label + "_" + woId.substring(0, 8) + "_calls.txt"), (String.join("\n", summary) + "\n\n" + all).getBytes());

        // The plan's pass criteria, as hard checks
        Assert.assertEquals(plainSession + workBlocks + nodeClassesUser + reportConfigs, 0,
                "Calls the fix removes still fire before the grid: plain session " + plainSession + ", work blocks " + workBlocks
                        + ", node_classes/user " + nodeClassesUser + ", reporting configs " + reportConfigs);
        Assert.assertEquals(header, 1, "/ir_session/{id}/full?include=header should fire exactly once");
        Assert.assertEquals(deferredEarly, 0, "Plan 3.3: lookup/schema calls should start only after the grid is visible");
        for (Object w : waits) Assert.assertTrue(w instanceof Number && ((Number) w).intValue() < 1000, "assets/v2 server wait should be < 1 s, got " + waits);
    }

    /** API calls whose URL matches {@code regex} that started at or after {@code sinceMs} (performance.now()). */
    private long callsSince(double sinceMs, String regex) {
        return ((Number) js("var n=0,since=arguments[0],re=new RegExp(arguments[1]);performance.getEntriesByType('resource').forEach(function(e){"
                + "if(e.startTime>=since&&re.test(e.name))n++;});return n;", sinceMs, regex)).longValue();
    }

    private double now() { return ((Number) js("return performance.now();")).doubleValue(); }

    /** Clicks the first visible, enabled button whose text matches; returns its text or null. */
    private String clickButton(String textRegex) {
        return (String) js("var re=new RegExp(arguments[0],'i');var b=[].slice.call(document.querySelectorAll('button,[role=button],[role=menuitem]'))"
                + ".find(function(x){return re.test((x.innerText||x.getAttribute('aria-label')||'').trim())&&!x.disabled&&x.offsetParent!==null;});"
                + "if(!b)return null;b.scrollIntoView({block:'center'});b.click();return (b.innerText||b.getAttribute('aria-label')||'').trim();", textRegex);
    }

    private void closeDialog() {
        js("var d=[].slice.call(document.querySelectorAll('[role=dialog]')).pop();if(!d)return;"
                + "var c=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^(close|cancel)$/i.test((x.innerText||x.getAttribute('aria-label')||'').trim());});"
                + "if(c)c.click();else document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));");
        sleep(1500);
    }

    /**
     * Plan section 5.1 + 5.2: the data the page stopped loading on open now loads when its dialog opens.
     * Bulk Actions fires /node_classes/user once (not again on re-open); Generate Report fires
     * /reporting/configs once. Dialogs are closed without submitting anything.
     */
    @Test(priority = 2, description = "ZP-4444 plan §5: Bulk Actions and Generate Report load their data on demand")
    @Parameters({"zp4444.wo", "zp4444.label"})
    public void dialogsLoadOnDemand(@Optional("f532bf11-41e0-4b50-b310-bb82237d0d9b") String woId,
                                    @Optional("small") String label) throws Exception {
        ExtentReportManager.createTest("ZP-4444", "Work order loading time", "On-demand loads " + label + " " + woId);
        driver.get(AppConstants.BASE_URL + "/sessions/" + woId);
        for (int i = 0; i < 80 && js("return document.querySelector(\"" + GRID_ROWS + "\")?1:null;") == null; i++) {
            loginPage.dismissMfaPromptIfShowing(); sleep(250);
        }
        sleep(5000);
        List<String> notes = new ArrayList<>();
        Files.createDirectories(OUT);

        // 5.1 Bulk actions: turn on "Bulk Ops", select two assets, open the bulk dialog from "Actions"
        String bulkOps = clickButton("^bulk ops$");
        sleep(1200);
        js("var c=[].slice.call(document.querySelectorAll(\"[role='row'][data-rowindex] input[type=checkbox]\")).slice(0,2);c.forEach(function(x){x.click();});");
        sleep(1000);
        double t1 = now();
        // with Bulk Ops on and rows ticked the toolbar shows Services / Edit / Mark As; "Edit" opens the bulk dialog
        String actions = "(not needed)";
        Object menu = js("return [].slice.call(document.querySelectorAll('main button')).filter(function(x){return x.offsetParent!==null;}).map(function(x){return (x.innerText||'').trim();}).filter(Boolean).slice(0,14);");
        String bulk = clickButton("^edit$");
        sleep(4000);
        long nc1 = callsSince(t1, "/api/node_classes/user/");
        Object bulkDialog = js("var d=[].slice.call(document.querySelectorAll('[role=dialog]')).pop();return d?(d.innerText||'').replace(/\\s+/g,' ').slice(0,220):null;");
        notes.add("Bulk dialog text: " + bulkDialog);
        Files.write(OUT.resolve(label + "_bulk_actions_open.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        closeDialog();
        double t2 = now();
        String bulk2 = null;
        if (bulk != null) bulk2 = clickButton("^edit$");
        sleep(3000);
        long nc2 = callsSince(t2, "/api/node_classes/user/");
        closeDialog();
        notes.add("Bulk Ops toggle: " + bulkOps + "; toolbar buttons " + menu + "; opened: " + bulk + " " + actions
                + " → /node_classes/user fired " + nc1 + "× on first open (should be 1), " + nc2 + "× on re-open (should be 0)");

        // 5.2 Generate Report: from the page's ⋮ menu or Actions; count /reporting/configs, close without generating
        js("document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));");
        sleep(500);
        double t3 = now();
        String gen = clickButton("^generate report$");
        Object kebab = null;
        if (gen == null) {
            kebab = js("var b=[].slice.call(document.querySelectorAll('header button, main button')).filter(function(x){return x.offsetParent!==null&&!(x.innerText||'').trim()&&x.querySelector('svg');});"
                    + "var r=b.filter(function(x){return x.getBoundingClientRect().top<80;});var k=r[r.length-1];if(!k)return null;k.click();"
                    + "return [].slice.call(document.querySelectorAll('[role=menuitem]')).filter(function(x){return x.offsetParent!==null;}).map(function(x){return (x.innerText||'').trim();});");
            sleep(700);
            gen = clickButton("generate report|^report$|create report");
        }
        if (gen == null) { clickButton("^actions$"); sleep(700); gen = clickButton("generate report|create report"); }
        sleep(4000);
        long rc = callsSince(t3, "/api/reporting/configs");
        Object options = js("var d=[].slice.call(document.querySelectorAll('[role=dialog]')).pop();return d?(d.innerText||'').replace(/\\s+/g,' ').slice(0,200):null;");
        Files.write(OUT.resolve(label + "_generate_report_open.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        closeDialog();
        notes.add("⋮ menu items: " + kebab + "; Generate Report: " + gen + " → /reporting/configs fired " + rc + "× (should be 1); dialog: " + options);
        System.out.println("[ZP-4444] " + label + " on-demand:\n  " + String.join("\n  ", notes));
        Files.write(OUT.resolve(label + "_" + woId.substring(0, 8) + "_on_demand.txt"), String.join("\n", notes).getBytes());

        Assert.assertNotNull(bulk, "Bulk Actions button not found after selecting assets");
        Assert.assertEquals(nc1, 1L, "Bulk Actions should load /node_classes/user once when it opens");
        Assert.assertEquals(nc2, 0L, "Re-opening Bulk Actions should not load /node_classes/user again");
        Assert.assertNotNull(gen, "Generate Report button not found");
        Assert.assertEquals(rc, 1L, "Generate Report should load /reporting/configs once when it opens");
    }
}
