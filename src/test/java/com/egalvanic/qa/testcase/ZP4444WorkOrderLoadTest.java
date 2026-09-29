package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;
import org.testng.SkipException;
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

    /** The grid's own row-selection boxes (MUI DataGrid selection column) — never the in-cell Forms check-off. */
    private static final String ROW_SELECT = "[role='row'][data-rowindex] [data-field='__check__'] input[type=checkbox]";

    /** Top-most open dialog, modal drawer or menu (portals stack in DOM order), or null; docked nav drawers and snackbars are ignored. */
    private WebElement topOverlay() {
        return (WebElement) js("var els=[].slice.call(document.querySelectorAll(\"[role=dialog], .MuiDrawer-paper, [role=presentation] .MuiPaper-root\")).filter(function(e){"
                + "if(e.closest('.MuiDrawer-docked, .MuiSnackbar-root'))return false;var r=e.getBoundingClientRect();"
                + "return r.width>0&&r.height>0&&r.right>0&&r.left<innerWidth&&getComputedStyle(e).visibility!=='hidden';});"
                + "els=els.filter(function(e){return !els.some(function(o){return o!==e&&o.contains(e);});});return els.length?els[els.length-1]:null;");
    }

    private String overlayText(WebElement o) {
        return o == null ? null : (String) js("return (arguments[0].innerText||'').replace(/\\s+/g,' ').trim().slice(0,220);", o);
    }

    private boolean isShown(WebElement o) {
        try {
            return Boolean.TRUE.equals(js("var e=arguments[0];if(!e.isConnected)return false;var r=e.getBoundingClientRect();"
                    + "return r.width>0&&r.height>0&&r.right>0&&r.left<innerWidth&&getComputedStyle(e).visibility!=='hidden';", o));
        } catch (StaleElementReferenceException gone) { return false; }
    }

    /** A real key press: MUI only closes on a trusted Escape, a synthetic KeyboardEvent never reaches it. */
    private void pressEscape() {
        new Actions(((com.egalvanic.qa.utils.ai.SelfHealingDriver) driver).getWrappedDriver()).sendKeys(Keys.ESCAPE).perform();
    }

    /** Closes an overlay with its own Cancel/Close button, else a real Escape; true once it is gone. */
    private boolean closeOverlay(WebElement o) {
        if (o == null) return true;
        Object viaButton = js("var b=[].slice.call(arguments[0].querySelectorAll('button')).filter(function(x){return !x.disabled;});"
                + "var c=b.find(function(x){return /^(cancel|close)$/i.test((x.innerText||'').trim());})||b.find(function(x){return /^close$/i.test(x.getAttribute('aria-label')||'');});"
                + "if(c)c.click();return !!c;", o);
        if (!Boolean.TRUE.equals(viaButton)) pressEscape();
        for (int i = 0; i < 20 && isShown(o); i++) sleep(250);
        if (isShown(o)) { pressEscape(); for (int i = 0; i < 12 && isShown(o); i++) sleep(250); }
        return !isShown(o);
    }

    /** Each rendered row's in-cell check-offs (every checkbox outside the selection column), keyed by row id. Read only. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> checkOffs() {
        return (Map<String, Object>) js("var o={};[].slice.call(document.querySelectorAll(\"[role='row'][data-rowindex]\")).forEach(function(r){"
                + "var s=[].slice.call(r.querySelectorAll('input[type=checkbox]')).filter(function(c){return !c.closest(\"[data-field='__check__']\");}).map(function(c){"
                + "var f=c.closest('[data-field]');return (f?f.getAttribute('data-field'):'?')+'='+(c.getAttribute('data-indeterminate')==='true'?'partial':c.checked);});"
                + "if(s.length)o[r.getAttribute('data-id')]=s.join(',');});return o;");
    }

    private void waitForGrid() {
        for (int i = 0; i < 80 && js("return document.querySelector(\"" + GRID_ROWS + "\")?1:null;") == null; i++) {
            loginPage.dismissMfaPromptIfShowing(); sleep(250);
        }
        sleep(5000);
    }

    /**
     * Plan section 5.1 + 5.2: the data the page stopped loading on open now loads when its panel opens.
     * Bulk Edit (a right-hand drawer) fires /node_classes/user once (not again on re-open); Generate Report
     * fires /reporting/configs once. Only the grid's row-selection boxes are ticked, panels are closed with
     * Cancel/Close or a real Escape, nothing is submitted, and in-cell check-offs must be unchanged after a reload.
     */
    @SuppressWarnings("unchecked")
    @Test(priority = 2, description = "ZP-4444 plan §5: Bulk Actions and Generate Report load their data on demand")
    @Parameters({"zp4444.wo", "zp4444.label"})
    public void dialogsLoadOnDemand(@Optional("f532bf11-41e0-4b50-b310-bb82237d0d9b") String woId,
                                    @Optional("small") String label) throws Exception {
        ExtentReportManager.createTest("ZP-4444", "Work order loading time", "On-demand loads " + label + " " + woId);
        driver.get(AppConstants.BASE_URL + "/sessions/" + woId);
        waitForGrid();
        List<String> notes = new ArrayList<>();
        Files.createDirectories(OUT);
        Path notesFile = OUT.resolve(label + "_" + woId.substring(0, 8) + "_on_demand.txt");

        // A closed work order's asset grid is read-only and renders no Bulk Ops, so 5.1 cannot run there
        String mode = null;
        for (int i = 0; i < 20 && mode == null; i++) {
            mode = (String) js("var v=function(x){return x.offsetParent!==null;};"
                    + "if([].slice.call(document.querySelectorAll('button')).some(function(x){return /^bulk ops$/i.test((x.innerText||'').trim())&&v(x);}))return 'bulk';"
                    + "return [].slice.call(document.querySelectorAll('.MuiChip-label')).some(function(x){return /^closed$/i.test((x.innerText||'').trim())&&v(x);})?'closed':null;");
            if (mode == null) sleep(250);
        }
        if ("closed".equals(mode)) {
            String why = "Work order " + woId.substring(0, 8) + " is Closed, so its asset grid offers no Bulk Ops; Bulk Edit on-demand loading cannot be exercised here";
            Files.write(notesFile, ("Skipped: " + why).getBytes());
            throw new SkipException(why);
        }
        // an OPEN work order without Bulk Ops is a regression or a locator break, never a skip
        if (!"bulk".equals(mode)) {
            Files.write(notesFile, ("Failed: no Bulk Ops button and no Closed chip on work order " + woId.substring(0, 8)).getBytes());
            Assert.fail("Bulk Ops button not found on work order " + woId.substring(0, 8) + ", which is not shown as Closed");
        }
        Map<String, Object> checksBefore = checkOffs();
        Files.write(OUT.resolve(label + "_before.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));

        // 5.1 Bulk Edit: turn on "Bulk Ops", tick the row-selection box of the first two rows, open it with "Edit"
        String bulkOps = clickButton("^bulk ops$");
        Object hasSelect = null;
        for (int i = 0; i < 24 && hasSelect == null; i++) { sleep(250); hasSelect = js("return document.querySelector(\"" + ROW_SELECT + "\")?1:null;"); }
        if (hasSelect == null) {
            Object cells = js("var r=document.querySelector(\"[role='row'][data-rowindex]\");return r?[].slice.call(r.querySelectorAll('input[type=checkbox]'))"
                    + ".map(function(c){var f=c.closest('[data-field]');return f?f.getAttribute('data-field'):'?';}):null;");
            String why = "No row-selection checkbox (" + ROW_SELECT + ") after Bulk Ops (" + bulkOps + "), so nothing was ticked; checkbox cells in the first row: " + cells;
            Files.write(notesFile, why.getBytes());
            Assert.fail(why);
        }
        List<Object> ticked = (List<Object>) js("var rows=[].slice.call(document.querySelectorAll(\"[role='row'][data-rowindex]\"))"
                + ".sort(function(a,b){return a.getAttribute('data-rowindex')-b.getAttribute('data-rowindex');});var out=[];"
                + "for(var i=0;i<rows.length&&out.length<2;i++){var c=rows[i].querySelector(\"[data-field='__check__'] input[type=checkbox]\");"
                + "if(!c||c.disabled)continue;if(!c.checked)c.click();out.push(rows[i].getAttribute('data-id'));}return out;");
        sleep(1000);
        Object selected = js("return document.querySelectorAll(\"" + ROW_SELECT + ":checked\").length;");
        double t1 = now();
        // with Bulk Ops on and rows ticked the toolbar shows Services / Edit / Mark As; "Edit" opens the Bulk Edit drawer
        Object menu = js("return [].slice.call(document.querySelectorAll('main button')).filter(function(x){return x.offsetParent!==null;}).map(function(x){return (x.innerText||'').trim();}).filter(Boolean).slice(0,14);");
        String bulk = clickButton("^edit$");
        sleep(4000);
        long nc1 = callsSince(t1, "/api/node_classes/user/");
        WebElement drawer = topOverlay();
        String bulkText = overlayText(drawer);
        java.util.regex.Matcher cm = java.util.regex.Pattern.compile("Bulk Edit \\((\\d+) asset").matcher(String.valueOf(bulkText));
        int bulkCount = cm.find() ? Integer.parseInt(cm.group(1)) : -1;
        notes.add("Bulk Edit drawer text: " + bulkText);
        Files.write(OUT.resolve(label + "_bulk_actions_open.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        String opened = "Bulk Ops toggle: " + bulkOps + "; ticked row-selection boxes of rows " + ticked + " (" + selected + " selected); toolbar buttons " + menu
                + "; opened: " + bulk + " → /node_classes/user fired " + nc1 + "× on first open (should be 1)";
        if (bulk == null || bulkCount < 0) {
            Files.write(notesFile, (opened + "\nBulk Edit drawer did not open; top overlay: " + bulkText).getBytes());
            Assert.fail("Bulk Edit drawer did not open (Edit button " + (bulk == null ? "not found" : "clicked") + " after ticking rows " + ticked + "); top overlay: " + bulkText);
        }
        boolean closed1 = closeOverlay(drawer);
        if (!closed1) {
            Files.write(notesFile, (opened + "\nBulk Edit drawer still open after Cancel/Close and a real Escape").getBytes());
            Assert.fail("Bulk Edit drawer did not close (Cancel/Close, then a real Escape), so the re-open check was not run");
        }
        double t2 = now();
        String bulk2 = clickButton("^edit$");
        sleep(3000);
        long nc2 = callsSince(t2, "/api/node_classes/user/");
        WebElement drawer2 = topOverlay();
        String reopenText = overlayText(drawer2);
        boolean closed2 = drawer2 != null && closeOverlay(drawer2);
        notes.add(opened + "; drawer gone before re-open: " + closed1 + "; re-opened: " + bulk2 + " (" + reopenText + ") → /node_classes/user fired "
                + nc2 + "× on re-open (should be 0); closed again: " + closed2);

        // 5.2 Generate Report: a visible "Generate Report" button, else "Actions" → its Generate Report item; close without generating
        for (int i = 0; i < 3; i++) { WebElement o = topOverlay(); if (o == null) break; closeOverlay(o); }
        double t3 = now();
        Object menuItems = null;
        String gen = clickButton("^generate report$");
        if (gen == null && clickButton("^actions$") != null) {
            sleep(700);
            WebElement actionsMenu = topOverlay();
            if (actionsMenu != null) {
                menuItems = js("return [].slice.call(arguments[0].querySelectorAll('[role=menuitem]')).map(function(x){return (x.innerText||'').trim();});", actionsMenu);
                gen = (String) js("var it=[].slice.call(arguments[0].querySelectorAll('[role=menuitem]')).find(function(x){return /generate report/i.test((x.innerText||'').trim())"
                        + "&&x.getAttribute('aria-disabled')!=='true';});if(!it)return null;it.click();return (it.innerText||'').trim();", actionsMenu);
            }
        }
        if (gen == null) { pressEscape(); sleep(500); } else sleep(4000);
        long rc = callsSince(t3, "/api/reporting/configs");
        WebElement dlg = gen == null ? null : topOverlay();
        String options = overlayText(dlg);
        Files.write(OUT.resolve(label + "_generate_report_open.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        boolean closed3 = closeOverlay(dlg);
        notes.add("Actions menu items: " + menuItems + "; Generate Report: " + gen + " → /reporting/configs fired " + rc + "× (should be 1); dialog: " + options + "; closed: " + closed3);

        // Nothing may change: in-cell check-offs (Forms) on a fresh load must match the state before any click
        driver.get(AppConstants.BASE_URL + "/sessions/" + woId);
        waitForGrid();
        Map<String, Object> checksAfter = checkOffs();
        Files.write(OUT.resolve(label + "_after_reload.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        List<String> changed = new ArrayList<>();
        int compared = 0;
        for (Map.Entry<String, Object> e : checksBefore.entrySet()) {
            if (!checksAfter.containsKey(e.getKey())) continue;
            compared++;
            if (!e.getValue().equals(checksAfter.get(e.getKey()))) changed.add(e.getKey() + " " + e.getValue() + " → " + checksAfter.get(e.getKey()));
        }
        List<String> tickedState = new ArrayList<>();
        for (Object id : ticked) tickedState.add(id + " " + checksBefore.get(String.valueOf(id)) + " → " + checksAfter.get(String.valueOf(id)));
        notes.add("In-cell check-offs, before any click vs a fresh reload at the end: " + compared + " rows compared, "
                + (changed.isEmpty() ? "unchanged" : "CHANGED " + changed) + "; ticked rows " + tickedState);
        System.out.println("[ZP-4444] " + label + " on-demand:\n  " + String.join("\n  ", notes));
        Files.write(notesFile, String.join("\n", notes).getBytes());

        Assert.assertTrue(checksBefore.isEmpty() || compared > 0,
                "Data-safety check compared 0 of " + checksBefore.size() + " rows with check-offs, so it proved nothing");
        Assert.assertTrue(changed.isEmpty(), "The test must not change data, but in-cell check-offs changed: " + changed);
        Assert.assertEquals(bulkCount, 2, "Bulk Edit should say 2 assets after ticking two rows; drawer: " + bulkText);
        Assert.assertEquals(nc1, 1L, "Bulk Edit should load /node_classes/user once when it opens");
        Assert.assertTrue(String.valueOf(reopenText).contains("Bulk Edit"), "Re-opening (" + bulk2 + ") did not show the Bulk Edit drawer: " + reopenText);
        Assert.assertEquals(nc2, 0L, "Re-opening Bulk Edit should not load /node_classes/user again");
        Assert.assertNotNull(gen, "No visible Generate Report button and no Generate Report item in the Actions menu (items: " + menuItems + ")");
        Assert.assertEquals(rc, 1L, "Generate Report should load /reporting/configs once when it opens");
        Assert.assertTrue(closed2 && closed3, "An overlay did not close: Bulk Edit re-open " + closed2 + ", Generate Report " + closed3);
    }
}
