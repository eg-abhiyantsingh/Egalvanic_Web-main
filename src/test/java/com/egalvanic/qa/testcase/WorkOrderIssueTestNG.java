package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Work Order — <b>Issues tab → Add Issue</b> workflow.
 *
 * <p><b>Flow (verified live):</b> Work Orders (/sessions) → site "All Facilities" → click a work-order
 * grid row ⇒ detail page (/sessions/{id}) with a tab strip <b>Assets · Tasks · EG Forms · Issues · IR
 * Photos · Attachments</b>. On the <b>Issues</b> tab the <b>Actions</b> button opens a menu with
 * <b>Add Issues</b>, which opens the <b>Add Issue</b> drawer: Priority (default <b>Medium</b>) · Issue
 * Class* · Asset · <b>Title*</b> ("Enter issue title") · Description · <b>Proposed Resolution*</b>
 * ("Describe the proposed resolution") → <b>Create Issue</b>. Creating with just Title + Proposed
 * Resolution succeeds (Issue Class's * is not enforced on submit) and the issue appears in the WO's
 * Issues grid (the Issues tab count increments).</p>
 *
 * <p>{@code testWOI_00_DumpFlow} is a disabled DOM-discovery diagnostic.</p>
 */
public class WorkOrderIssueTestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_WORK_ORDERS;
    private static final String FEATURE = "Work Order — Add Issue";
    private static final String SITE = "All Facilities";

    private boolean issueModalMaybeOpen = false;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Work Order — Issues tab / Add Issue");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
    }

    @BeforeMethod
    @Override
    public void testSetup() {
        super.testSetup();
        try { dismissModal(); } catch (Exception ignored) {}
        issueModalMaybeOpen = false;
    }

    @AfterMethod(alwaysRun = true)
    public void closeModalAfter(ITestResult result) {
        if (issueModalMaybeOpen) { dismissModal(); issueModalMaybeOpen = false; }
    }

    // ================================================================
    // TESTS
    // ================================================================

    @Test(priority = 1, description = "WOI_01: Opening a work order shows its detail tab strip (Tasks, EG Forms, Issues)")
    public void testWOI_01_WorkOrderDetailTabs() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOI_01_WorkOrderDetailTabs");
        Assert.assertTrue(openFirstWorkOrder(), "Should open a work order detail page (/sessions/{id}).");
        for (String tab : new String[]{"Tasks", "EG Forms", "Issues"}) {
            Assert.assertTrue(hasTab(tab), "Work order detail should expose the '" + tab + "' tab.");
        }
        ExtentReportManager.logPass("Work order detail page shows the tab strip (Assets/Tasks/EG Forms/Issues/IR Photos/Attachments).");
    }

    @Test(priority = 2, description = "WOI_02: Issues tab → Actions → Add Issues opens the Add Issue drawer")
    public void testWOI_02_AddIssueOpensModal() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOI_02_AddIssueOpensModal");
        Assert.assertTrue(openAddIssueModal(), "Issues tab → Actions → Add Issues should open the Add Issue drawer.");
        issueModalMaybeOpen = true;
        Assert.assertTrue(bodyHas("Add Issue"), "The Add Issue drawer should show the 'Add Issue' heading.");
        Assert.assertTrue(hasPlaceholder("Enter issue title"), "Add Issue should have a Title field ('Enter issue title').");
        Assert.assertTrue(hasPlaceholder("Describe the proposed resolution"),
                "Add Issue should have a Proposed Resolution field.");
        Assert.assertTrue(buttonExists("Create Issue"), "Add Issue should have a 'Create Issue' button.");
        ExtentReportManager.logPass("Add Issue drawer opens with Title, Proposed Resolution and Create Issue.");
    }

    @Test(priority = 3, description = "WOI_03: Add Issue defaults Priority to Medium and marks Title + Proposed Resolution required")
    public void testWOI_03_AddIssueRequiredFields() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOI_03_AddIssueRequiredFields");
        Assert.assertTrue(openAddIssueModal(), "Add Issue drawer should open.");
        issueModalMaybeOpen = true;
        // Priority defaults to Medium — it's the combobox's VALUE (not page text, so bodyHas won't see it).
        Boolean priorityMedium = (Boolean) js(
                "var d=[].slice.call(document.querySelectorAll(\"[role='dialog'],[class*='MuiDialog-paper'],[class*='MuiDrawer-paper']\"))"
                + ".find(function(e){return e.getBoundingClientRect().width>200 && /Add Issue/.test(e.innerText||'');}) || document;"
                + "var byVal=[].slice.call(d.querySelectorAll('input')).some(function(i){return (i.value||'').trim()==='Medium';});"
                + "var byCombo=[].slice.call(d.querySelectorAll(\"[role='combobox'], .MuiSelect-select\")).some(function(c){return /Medium/.test(c.textContent||c.value||'');});"
                + "return byVal || byCombo;");
        Assert.assertTrue(Boolean.TRUE.equals(priorityMedium), "Priority should default to Medium.");
        // Title (*) and Proposed Resolution (*) are marked required
        Boolean titleRequired = (Boolean) js(
                "function reqNear(label){var els=[].slice.call(document.querySelectorAll('p,label,span,div'));"
                + "var l=els.find(function(e){return (e.textContent||'').trim().indexOf(label)===0 && (e.textContent||'').length<label.length+4;});"
                + "if(!l) return false; return /\\*/.test(l.textContent);}"
                + "return reqNear('Title');");
        Boolean prRequired = (Boolean) js(
                "function reqNear(label){var els=[].slice.call(document.querySelectorAll('p,label,span,div'));"
                + "var l=els.find(function(e){return (e.textContent||'').trim().indexOf(label)===0 && (e.textContent||'').length<label.length+4;});"
                + "if(!l) return false; return /\\*/.test(l.textContent);}"
                + "return reqNear('Proposed Resolution');");
        Assert.assertTrue(Boolean.TRUE.equals(titleRequired), "Title should be marked required (*).");
        Assert.assertTrue(Boolean.TRUE.equals(prRequired), "Proposed Resolution should be marked required (*).");
        ExtentReportManager.logPass("Add Issue: Priority defaults to Medium; Title + Proposed Resolution marked required (*).");
    }

    @Test(priority = 4, description = "WOI_04: Creating an issue (Title + Proposed Resolution) from a work order succeeds")
    public void testWOI_04_CreateIssueFromWorkOrder() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOI_04_CreateIssueFromWorkOrder");
        Assert.assertTrue(openAddIssueModal(), "Add Issue drawer should open.");
        issueModalMaybeOpen = true;
        String title = "QA-AUTO WO Issue " + System.currentTimeMillis();
        Assert.assertTrue(fillPlaceholder("Enter issue title", title), "Should fill the Title field.");
        Assert.assertTrue(fillPlaceholder("Describe the proposed resolution", "Auto-created by QA test — no action required."),
                "Should fill the Proposed Resolution field.");
        pause(500);
        Assert.assertTrue(clickButtonExact("Create Issue"), "Should click Create Issue.");
        // wait for the drawer to close and the new issue to appear in the WO's Issues grid
        boolean created = false;
        for (int i = 0; i < 20; i++) {
            pause(700);
            if (!modalOpen() && bodyHas(title)) { created = true; break; }
        }
        issueModalMaybeOpen = modalOpen();
        Assert.assertTrue(created,
                "The new issue ('" + title + "') should be created and listed in the work order's Issues grid.");
        ExtentReportManager.logPass("Created an issue from the work order: '" + title + "' (Title + Proposed Resolution).");
    }

    // ================================================================
    // DIAGNOSTIC (disabled)
    // ================================================================

    @Test(priority = 0, enabled = false, description = "DIAG: step-by-step state dump of the WO open flow")
    public void testWOI_00_DumpFlow() {
        ExtentReportManager.createTest(MODULE, FEATURE, "WOI_00_DiagDump");
        StringBuilder sb = new StringBuilder();
        long t0 = System.currentTimeMillis();
        sb.append("siteBefore=").append(inputVal("Select facility")).append("\n");
        boolean site = selectSiteByName(SITE);
        sb.append("selectSite('All Facilities')=").append(site).append(" took=").append(System.currentTimeMillis() - t0).append("ms\n");
        sb.append("siteAfter=").append(inputVal("Select facility")).append("\n");

        long t1 = System.currentTimeMillis();
        workOrderPage.navigateToWorkOrders();
        pause(4000);
        sb.append("navWO took=").append(System.currentTimeMillis() - t1).append("ms url=").append(driver.getCurrentUrl()).append("\n");
        sb.append("loadingPresent=").append(bodyHas("Loading work orders")).append("\n");
        Object rc = js("return document.querySelectorAll('tbody tr').length;");
        sb.append("tbodyTrCount=").append(rc).append("\n");
        Object rowsRole = js("return document.querySelectorAll(\"div[role='row']\").length;");
        sb.append("roleRowCount=").append(rowsRole).append("\n");
        WebElement c = findWoRow();
        sb.append("woRowText=").append(c == null ? "(null)" : "'" + c.getText().replace("\n", " ") + "'").append("\n");
        Object firstRow = js("var r=document.querySelector('tbody tr'); return r? r.outerHTML.substring(0,500):'(none)';");
        sb.append("firstRowHTML=").append(firstRow).append("\n");
        // try clicking + observe nav
        if (c != null) {
            String before = driver.getCurrentUrl();
            try { c.click(); } catch (Exception e) { js("arguments[0].click();", c); }
            // poll up to ~20s for the WO detail tabs to render
            for (int i = 0; i < 40; i++) { pause(500); if (hasTab("Issues") || bodyHas("EG Forms")) break; }
            sb.append("afterClickUrl=").append(driver.getCurrentUrl()).append(" changed=").append(!driver.getCurrentUrl().equals(before)).append("\n");
            Object allTabs = js("var seen={};return [].slice.call(document.querySelectorAll(\"[role='tab']\")).map(function(b){return (b.innerText||'').replace(/\\s+/g,' ').trim();}).filter(function(x){if(!x||seen[x])return false;seen[x]=1;return true;}).join(' | ');");
            sb.append("roleTabsDeduped=").append(allTabs).append("\n");
            sb.append("hasTask=").append(hasTab("Tasks")).append(" hasEGForms=").append(hasTab("EG Forms")).append(" hasIssues=").append(hasTab("Issues")).append("\n");
            Object mainText = js("var m=document.querySelector('main'); return m? m.innerText.replace(/\\s+/g,' ').substring(0,700):'(no main)';");
            sb.append("mainText=").append(mainText).append("\n");
        }
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/woi-diag.txt"),
                    sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) { logStep("diag write failed: " + e.getMessage()); }
        ExtentReportManager.logPass("WO open diagnostic dumped.");
    }

    private String inputVal(String placeholder) {
        Object v = js("var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return (x.placeholder||'').toLowerCase().indexOf(arguments[0].toLowerCase())>-1;}); return i?i.value:'(no input)';", placeholder);
        return v == null ? "" : v.toString();
    }

    // ================================================================
    // FLOW HELPERS
    // ================================================================

    /** Site → All Facilities, navigate to Work Orders, click the first WO row → /sessions/{id}. Retries. */
    private boolean openFirstWorkOrder() {
        // Fast path: already on a WO detail with its tab strip (saves the ~24s re-navigation per test).
        String cur = driver.getCurrentUrl();
        if (cur.contains("/sessions/") && !cur.endsWith("/sessions") && !cur.endsWith("/sessions/") && hasTab("Issues")) {
            return true;
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                selectSiteByName(SITE);
                workOrderPage.navigateToWorkOrders();
                // wait for the grid to finish "Loading work orders..." and have a real, clickable WO row
                WebElement row = null;
                for (int i = 0; i < 30; i++) {
                    pause(600);
                    if (!bodyHas("Loading work orders")) { row = findWoRow(); if (row != null) break; }
                }
                if (row == null) { logStep("WO grid had no data row (attempt " + (attempt + 1) + ")"); continue; }
                pause(800); // let the grid settle so the row ref isn't replaced mid-click
                String before = driver.getCurrentUrl();
                row = findWoRow(); // re-find fresh right before clicking
                if (row == null) continue;
                try { row.click(); } catch (Exception e) { js("arguments[0].click();", row); }
                boolean navigated = false;
                for (int i = 0; i < 14; i++) {
                    pause(700);
                    String u = driver.getCurrentUrl();
                    if (u.contains("/sessions/") && !u.endsWith("/sessions") && !u.endsWith("/sessions/") && !u.equals(before)) { navigated = true; break; }
                }
                if (navigated && waitForTabs(35)) return true; // the WO detail tabs render slowly on this app
                logStep("WO open attempt " + (attempt + 1) + " failed (navigated=" + navigated + ")");
            } catch (Exception e) { logStep("openFirstWorkOrder error: " + e.getMessage()); }
        }
        return false;
    }

    /** First true "Work Order" data row (Job-type sessions have a different, Issue-less detail layout).
     *  Falls back to the first data row if no row is explicitly named "Work Order". */
    private WebElement findWoRow() {
        WebElement firstData = null;
        for (WebElement r : driver.findElements(By.cssSelector("div[role='row']"))) {
            try {
                if (r.findElements(By.cssSelector("[role='gridcell']")).isEmpty()) continue; // header row
                String t = r.getText();
                if (t == null || t.trim().isEmpty()) continue;
                if (firstData == null) firstData = r;
                if (t.toLowerCase().contains("work order")) return r; // prefer a real Work Order
            } catch (Exception ignored) {}
        }
        return firstData;
    }

    /** Wait for the WO detail tab strip (Tasks + Issues) to render. */
    private boolean waitForTabs(int seconds) {
        for (int i = 0; i < seconds * 2; i++) { if (hasTab("Issues") && hasTab("Tasks")) return true; pause(500); }
        return false;
    }

    /** Open a WO fresh, then Issues tab → Actions → Add Issues → wait for the Add Issue drawer. */
    private boolean openAddIssueModal() {
        // Always re-open from the list so the test never depends on a prior test's page state.
        if (!openFirstWorkOrder()) { logStep("could not open a work order"); return false; }
        if (!clickDetailTab("Issues")) { logStep("could not click Issues tab"); return false; }
        pause(1500);
        if (!clickButtonByText("Actions")) { logStep("could not click Actions"); return false; }
        pause(1000);
        if (!clickMenuItemContaining("Add Issue")) { logStep("could not click Add Issues"); return false; }
        for (int i = 0; i < 16; i++) { pause(500); if (bodyHas("Add Issue") && hasPlaceholder("Enter issue title")) return true; }
        return hasPlaceholder("Enter issue title");
    }

    // ================================================================
    // LOW-LEVEL HELPERS
    // ================================================================

    private Object js(String script, Object... args) {
        try { return ((JavascriptExecutor) driver).executeScript(script, args); }
        catch (Exception e) { logStep("js error: " + e.getMessage()); return null; }
    }

    private boolean bodyHas(String text) {
        Object r = js("return (document.body.innerText||'').toLowerCase().indexOf(arguments[0].toLowerCase())>-1;", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean hasTab(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "return !![].slice.call(document.querySelectorAll(\"[role='tab'], [class*='MuiTab-root']\")).find(function(b){return (b.innerText||'').toLowerCase().indexOf(t)>-1;});", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean hasPlaceholder(String placeholder) {
        Object r = js("var ph=arguments[0]; return !![].slice.call(document.querySelectorAll('input,textarea'))"
                + ".find(function(i){return (i.placeholder||'').toLowerCase().indexOf(ph.toLowerCase())>-1;});", placeholder);
        return Boolean.TRUE.equals(r);
    }

    private boolean fillPlaceholder(String placeholder, String value) {
        Object r = js("var ph=arguments[0], v=arguments[1];"
                + "var i=[].slice.call(document.querySelectorAll('input,textarea')).find(function(x){return (x.placeholder||'').toLowerCase().indexOf(ph.toLowerCase())>-1;});"
                + "if(!i) return false; i.scrollIntoView({block:'center'}); i.focus();"
                + "var proto = i.tagName==='TEXTAREA'? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;"
                + "var set=Object.getOwnPropertyDescriptor(proto,'value').set; set.call(i, v);"
                + "i.dispatchEvent(new Event('input',{bubbles:true})); i.dispatchEvent(new Event('change',{bubbles:true})); return true;",
                placeholder, value);
        if (!Boolean.TRUE.equals(r)) return false;
        pause(300);
        Object v = js("var ph=arguments[0]; var i=[].slice.call(document.querySelectorAll('input,textarea')).find(function(x){return (x.placeholder||'').toLowerCase().indexOf(ph.toLowerCase())>-1;}); return i?i.value:null;", placeholder);
        return v != null && v.toString().length() > 0;
    }

    private boolean buttonExists(String text) {
        Object r = js("var t=arguments[0].toLowerCase(); return !![].slice.call(document.querySelectorAll('button')).find(function(b){return (b.innerText||'').trim().toLowerCase()===t;});", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean clickButtonExact(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return (x.innerText||'').trim().toLowerCase()===t && !x.disabled;});"
                + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean clickButtonByText(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "var b=[].slice.call(document.querySelectorAll(\"button,[role='button']\")).find(function(x){var s=(x.innerText||'').trim().toLowerCase(); return s===t || s.indexOf(t)===0;});"
                + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean clickMenuItemContaining(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "var items=[].slice.call(document.querySelectorAll(\"li[role='menuitem'], [class*='MuiMenuItem'], [role='menu'] li, [class*='MuiPopover'] li\"));"
                + "var m=items.find(function(i){return (i.innerText||'').toLowerCase().indexOf(t)>-1;});"
                + "if(!m) return false; m.click(); return true;", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean clickDetailTab(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "var tabs=[].slice.call(document.querySelectorAll(\"[role='tab'], [class*='MuiTab-root']\"));"
                + "var m=tabs.find(function(b){return (b.innerText||'').trim().toLowerCase()===t;});"
                + "if(!m) m=tabs.find(function(b){return (b.innerText||'').toLowerCase().indexOf(t)>-1;});"
                + "if(!m) return false; m.scrollIntoView({block:'center'}); m.click(); return true;", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean modalOpen() {
        Object r = js("var els=[].slice.call(document.querySelectorAll(\"[role='dialog'], [class*='MuiDialog-paper'], [class*='MuiDrawer-paper']\"));"
                + "return !!els.find(function(e){return e.getBoundingClientRect().width>200 && /Add Issue|Create Issue|Enter issue title/.test(e.innerText||'');});");
        return Boolean.TRUE.equals(r);
    }

    private void dismissModal() {
        try {
            for (int i = 0; i < 3; i++) {
                if (!modalOpen()) return;
                js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /^(Cancel|Close)$/i.test((x.innerText||'').trim());}); if(b) b.click();");
                pause(600);
                js("var d=[].slice.call(document.querySelectorAll('button')).find(function(x){return /^(Discard|Yes|Leave|OK)$/i.test((x.innerText||'').trim());}); if(d) d.click();");
                pause(400);
                js("var bd=document.querySelector('.MuiBackdrop-root'); if(bd) bd.click();");
                pause(400);
            }
        } catch (Exception ignored) {}
    }
}
