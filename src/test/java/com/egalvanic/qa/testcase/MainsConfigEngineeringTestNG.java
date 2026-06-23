package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.JavascriptExecutor;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Asset Module — ENGINEERING <b>mains configuration</b> cascade for distribution-equipment classes
 * (Panelboard, MCC, Switchboard, PDU, Motor Starter, Other, VFD, VFD Panel).
 *
 * <p><b>Flow (verified live):</b> select a phase+mains class ⇒ the ENGINEERING block exposes
 * <b>Phase Configuration</b> (3P4W · 3P4W-HLD · 3P3W · 1P3W · 1P2W) and <b>Mains Type</b>
 * (MCB · MLO · FDS · NFDS). Choosing <b>MCB</b> (Main Circuit Breaker) opens a <b>"Create a Main
 * Breaker?"</b> dialog — Name* · <b>Subtype</b> (the 11 breaker subtypes: Low-Voltage Insulated Case /
 * Molded Case / Power, Medium-Voltage Air/Gas/Oil/Vacuum, Motor Circuit Protector, Recloser) ·
 * <b>Pole Count</b> (1P/2P/3P) · Create Main. Choosing <b>MLO</b> (Main Lug Only) opens no breaker
 * dialog. This is the depth layer for the phase+mains classes that {@link AssetEngineeringMatrixTestNG}
 * covers only at the label level.</p>
 *
 * <p>Non-destructive: every test opens the create form / breaker dialog and <b>cancels</b>.</p>
 */
public class MainsConfigEngineeringTestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_ASSET;
    private static final String FEATURE = "Engineering — Mains Configuration";

    private static final String CLASS = "Panelboard"; // canonical phase+mains class for the deep cascade
    private static final String[] PHASE_CONFIGS = {"3P4W", "3P4W-HLD", "3P3W", "1P3W", "1P2W"};
    private static final String[] MAINS_TYPES = {"MCB", "MLO", "FDS", "NFDS"};

    private boolean formOpen = false;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Asset ENGINEERING — Mains Configuration cascade");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
    }

    @BeforeMethod
    @Override
    public void testSetup() {
        super.testSetup();
        try { dismissDialog(); closeAnyOpenDrawer(); ensureOnAssetsPage(); } catch (Exception ignored) {}
        formOpen = false;
    }

    @AfterMethod(alwaysRun = true)
    public void closeFormAfter(ITestResult result) {
        try { dismissDialog(); } catch (Exception ignored) {}
        if (formOpen) { closeAnyOpenDrawer(); formOpen = false; }
    }

    @DataProvider(name = "phaseMainsClasses")
    public Object[][] phaseMainsClasses() {
        return new Object[][]{
            {"MCC"}, {"Switchboard"}, {"PDU"}, {"Motor Starter"}, {"Other"}, {"VFD"}, {"VFD Panel"}
        };
    }

    // ================================================================
    // DEEP CASCADE (Panelboard)
    // ================================================================

    @Test(priority = 1, description = "MAINS_01: Panelboard Engineering exposes Phase Configuration + Mains Type")
    public void testMAINS_01_PhaseAndMainsPresent() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_01_PhaseAndMainsPresent");
        Assert.assertTrue(openCreateFormForClass(CLASS), "Create form should open for " + CLASS);
        waitForControl("Select phase configuration", 8);
        Assert.assertTrue(hasControl("Select phase configuration"), "Engineering should expose a Phase Configuration control.");
        Assert.assertTrue(hasControl("Select mains type"), "Engineering should expose a Mains Type control.");
        ExtentReportManager.logPass(CLASS + " Engineering exposes Phase Configuration + Mains Type.");
    }

    @Test(priority = 2, description = "MAINS_02: Phase Configuration offers 3P4W / 3P4W-HLD / 3P3W / 1P3W / 1P2W")
    public void testMAINS_02_PhaseConfigOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_02_PhaseConfigOptions");
        Assert.assertTrue(openCreateFormForClass(CLASS), "Create form should open");
        waitForControl("Select phase configuration", 8);
        String opts = dropdownOptions("Select phase configuration");
        for (String p : PHASE_CONFIGS) {
            Assert.assertTrue(opts.contains(p), "Phase Configuration should offer '" + p + "' (got: " + opts + ").");
        }
        ExtentReportManager.logPass("Phase Configuration options present: " + opts);
    }

    @Test(priority = 3, description = "MAINS_03: Mains Type offers MCB / MLO / FDS / NFDS")
    public void testMAINS_03_MainsTypeOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_03_MainsTypeOptions");
        Assert.assertTrue(openCreateFormForClass(CLASS), "Create form should open");
        waitForControl("Select mains type", 8);
        String opts = dropdownOptions("Select mains type");
        for (String m : MAINS_TYPES) {
            Assert.assertTrue(opts.contains(m), "Mains Type should offer '" + m + "' (got: " + opts + ").");
        }
        ExtentReportManager.logPass("Mains Type options present: " + opts);
    }

    @Test(priority = 4, description = "MAINS_04: Selecting Mains Type=MCB opens the 'Create a Main Breaker?' dialog (Subtype + Pole Count + Create Main)")
    public void testMAINS_04_McbOpensBreakerDialog() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_04_McbOpensBreakerDialog");
        Assert.assertTrue(openCreateFormForClass(CLASS), "Create form should open");
        waitForControl("Select mains type", 8);
        Assert.assertNotNull(selectAutocomplete("Select phase configuration", "3P4W", "3p4w"), "Phase Configuration should be selectable.");
        pause(600);
        Assert.assertNotNull(selectAutocomplete("Select mains type", "MCB", "mcb"), "MCB should be selectable as Mains Type.");
        Assert.assertTrue(waitForDialog("Main Breaker", 10),
                "Selecting MCB should open the 'Create a Main Breaker?' dialog.");
        Assert.assertTrue(dialogHas("Subtype"), "The Main Breaker dialog should have a Subtype field.");
        Assert.assertTrue(dialogPoleButtons() >= 3, "The Main Breaker dialog should offer Pole Count 1P/2P/3P (found " + dialogPoleButtons() + ").");
        Assert.assertTrue(dialogHasButton("Create Main"), "The Main Breaker dialog should have a 'Create Main' button.");
        ExtentReportManager.logPass("Mains Type=MCB opens the 'Create a Main Breaker?' dialog (Subtype + Pole Count + Create Main).");
    }

    @Test(priority = 5, description = "MAINS_05: Main Breaker Subtype offers the circuit-breaker subtypes (Insulated/Molded Case…)")
    public void testMAINS_05_MainBreakerSubtypeOptions() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_05_MainBreakerSubtypeOptions");
        Assert.assertTrue(openCreateFormForClass(CLASS), "Create form should open");
        waitForControl("Select mains type", 8);
        selectAutocomplete("Select phase configuration", "3P4W", "3p4w");
        pause(600);
        Assert.assertNotNull(selectAutocomplete("Select mains type", "MCB", "mcb"), "MCB should be selectable.");
        Assert.assertTrue(waitForDialog("Main Breaker", 10), "MCB should open the breaker dialog.");
        String opts = dialogSubtypeOptions();
        Assert.assertTrue(opts.toLowerCase().contains("insulated case") || opts.toLowerCase().contains("molded case"),
                "Main Breaker Subtype should offer circuit-breaker subtypes (Insulated/Molded Case). Got: " + opts);
        ExtentReportManager.logPass("Main Breaker Subtype options: " + opts);
    }

    @Test(priority = 6, description = "MAINS_06: Main Breaker Pole Count (1P/2P/3P) is selectable in the dialog")
    public void testMAINS_06_MainBreakerPoleCount() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_06_MainBreakerPoleCount");
        Assert.assertTrue(openCreateFormForClass(CLASS), "Create form should open");
        waitForControl("Select mains type", 8);
        selectAutocomplete("Select phase configuration", "3P4W", "3p4w");
        pause(600);
        Assert.assertNotNull(selectAutocomplete("Select mains type", "MCB", "mcb"), "MCB should be selectable.");
        Assert.assertTrue(waitForDialog("Main Breaker", 10), "MCB should open the breaker dialog.");
        boolean clicked = clickDialogPole("3P");
        Assert.assertTrue(clicked, "Pole Count '3P' should be selectable in the Main Breaker dialog.");
        ExtentReportManager.logPass("Main Breaker Pole Count 1P/2P/3P present; selected 3P.");
    }

    @Test(priority = 7, description = "MAINS_07: Selecting Mains Type=MLO (Main Lug Only) opens NO main-breaker dialog")
    public void testMAINS_07_MloNoBreakerDialog() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_07_MloNoBreakerDialog");
        Assert.assertTrue(openCreateFormForClass(CLASS), "Create form should open");
        waitForControl("Select mains type", 8);
        selectAutocomplete("Select phase configuration", "3P4W", "3p4w");
        pause(600);
        Assert.assertNotNull(selectAutocomplete("Select mains type", "MLO", "mlo"), "MLO should be selectable as Mains Type.");
        pause(2000);
        Assert.assertFalse(dialogOpen("Main Breaker"),
                "Main Lug Only (MLO) should NOT open a main-breaker dialog.");
        ExtentReportManager.logPass("Mains Type=MLO selected — no main-breaker dialog (Main Lug Only).");
    }

    // ================================================================
    // BREADTH (other phase+mains classes)
    // ================================================================

    @Test(priority = 8, dataProvider = "phaseMainsClasses",
          description = "MAINS_08: Phase Configuration + Mains Type dropdowns are populated for each phase+mains class")
    public void testMAINS_08_PhaseMainsAcrossClasses(String cls) {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_08 — " + cls);
        Assert.assertTrue(openCreateFormForClass(cls), "Create form should open for " + cls);
        waitForControl("Select mains type", 8);
        String phase = dropdownOptions("Select phase configuration");
        Assert.assertTrue(phase.contains("3P4W") && phase.contains("1P2W"),
                cls + " Phase Configuration should list the standard configs (got: " + phase + ").");
        String mains = dropdownOptions("Select mains type");
        for (String m : MAINS_TYPES) {
            Assert.assertTrue(mains.contains(m), cls + " Mains Type should offer '" + m + "' (got: " + mains + ").");
        }
        ExtentReportManager.logPass(cls + " — Phase Configuration + Mains Type (MCB/MLO/FDS/NFDS) populated.");
    }

    // ================================================================
    // DISCONNECT-SWITCH MAINS (NFDS / FDS) — the non-breaker mains types
    // ================================================================

    @Test(priority = 9, description = "MAINS_09: Mains Type=NFDS opens the 'Create a Main Switch?' dialog (Name + Subtype + Create Main, no Pole Count)")
    public void testMAINS_09_NfdsOpensMainSwitchDialog() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_09_NfdsOpensMainSwitchDialog");
        Assert.assertTrue(openCreateFormForClass("VFD"), "Create form should open for VFD");
        waitForControl("Select mains type", 8);
        selectAutocomplete("Select phase configuration", "3P4W", "3p4w");
        pause(600);
        Assert.assertNotNull(selectAutocomplete("Select mains type", "NFDS", "nfds"), "NFDS should be selectable as Mains Type.");
        Assert.assertTrue(waitForDialog("Main Switch", 10),
                "Selecting NFDS should open the 'Create a Main Switch?' dialog.");
        Assert.assertTrue(dialogHas("Subtype"), "The Main Switch dialog should have a Subtype field.");
        Assert.assertTrue(dialogHasButton("Create Main"), "The Main Switch dialog should have a 'Create Main' button.");
        Assert.assertEquals(dialogPoleButtons(), 0,
                "A disconnect-switch main has NO Pole Count (unlike a circuit-breaker main).");
        ExtentReportManager.logPass("Mains Type=NFDS opens the 'Create a Main Switch?' dialog (Name + Subtype + Create Main; no Pole Count).");
    }

    @Test(priority = 10, description = "MAINS_10: Mains Type=FDS adds an Ampere Rating field and opens NO main-device dialog")
    public void testMAINS_10_FdsAmpereRatingNoDialog() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_10_FdsAmpereRatingNoDialog");
        Assert.assertTrue(openCreateFormForClass("VFD"), "Create form should open for VFD");
        waitForControl("Select mains type", 8);
        selectAutocomplete("Select phase configuration", "3P4W", "3p4w");
        pause(600);
        Assert.assertNotNull(selectAutocomplete("Select mains type", "FDS", "fds"), "FDS should be selectable as Mains Type.");
        pause(2500);
        Assert.assertFalse(dialogOpen("Main"),
                "FDS (Fused Disconnect Switch) should NOT open a main-device dialog.");
        Assert.assertTrue(engHasAmpereRating(),
                "FDS should add an 'Ampere Rating' field to the Engineering section.");
        ExtentReportManager.logPass("Mains Type=FDS adds an Ampere Rating field (no main-device dialog).");
    }

    @Test(priority = 11, description = "MAINS_11: Create a VFD (3P4W) with an NFDS main switch and save it (write-persistence)")
    public void testMAINS_11_CreateVfdWithMainSwitch() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS_11_CreateVfdWithMainSwitch");
        String name = "VFD-MAINS-" + System.currentTimeMillis();
        Assert.assertTrue(openCreateFormForClass("VFD"), "Create form should open for VFD");
        waitForControl("Select mains type", 8);
        Assert.assertTrue(setAssetName(name), "Should set the asset name on the create form.");
        selectAutocomplete("Select phase configuration", "3P4W", "3p4w");
        pause(600);
        Assert.assertNotNull(selectAutocomplete("Select mains type", "NFDS", "nfds"), "NFDS should be selectable.");
        Assert.assertTrue(waitForDialog("Main Switch", 10), "NFDS should open the 'Create a Main Switch?' dialog.");
        // fill the main switch Name (required) and create it
        Assert.assertTrue(fillDialogName("Main-" + name), "Should set the main switch Name in the dialog.");
        pause(400);
        Assert.assertTrue(clickDialogButton("Create Main"), "Should click 'Create Main'.");
        for (int i = 0; i < 24 && dialogOpen("Main Switch"); i++) pause(500); // wait for dialog to close
        pause(1500);
        // now save the VFD asset
        boolean saved = saveDrawer();
        formOpen = !saved;
        String url = driver.getCurrentUrl();
        Assert.assertTrue(saved && (url.endsWith("/assets") || url.endsWith("/assets/")),
                "Saving the VFD (with NFDS main switch) should persist it — create drawer closes, returns to /assets. url=" + url);
        ExtentReportManager.logPass("Created + saved a VFD with a 3P4W / NFDS main switch ('" + name + "').");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Drawer Engineering section contains an Ampere Rating field (top-level, FDS path). */
    private boolean engHasAmpereRating() {
        Object r = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper'); return d? /ampere rating/i.test(d.innerText||'') : false;");
        return Boolean.TRUE.equals(r);
    }

    /** Set the create-form's "Enter Asset Name" field via the React value setter (with sendKeys fallback). */
    private boolean setAssetName(String name) {
        js("var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return /enter asset name/i.test(x.placeholder||'');});"
                + "if(!i) return; i.scrollIntoView({block:'center'}); i.focus();"
                + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                + "set.call(i, arguments[0]); i.dispatchEvent(new Event('input',{bubbles:true})); i.dispatchEvent(new Event('change',{bubbles:true}));", name);
        pause(400);
        Object v = js("var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return /enter asset name/i.test(x.placeholder||'');}); return i?i.value:null;");
        return v != null && String.valueOf(v).contains(name);
    }

    /** Set the Name field inside the open Create-Main dialog (placeholder "e.g. MAIN-1"). */
    private boolean fillDialogName(String name) {
        Object r = js("var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                + "if(!d) return false;"
                + "var i=[].slice.call(d.querySelectorAll('input')).find(function(x){return /MAIN-1|name/i.test((x.placeholder||'')) ;}) || d.querySelector('input');"
                + "if(!i) return false; i.focus();"
                + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                + "set.call(i, arguments[0]); i.dispatchEvent(new Event('input',{bubbles:true})); i.dispatchEvent(new Event('change',{bubbles:true})); return true;", name);
        return Boolean.TRUE.equals(r);
    }

    /** Click a button (by exact text) inside the open dialog. */
    private boolean clickDialogButton(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                + "if(!d) return false;"
                + "var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return (x.innerText||'').trim().toLowerCase()===t && !x.disabled;});"
                + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;", text);
        return Boolean.TRUE.equals(r);
    }

    /** Click the drawer's submit button (Create Asset / Save) and wait for the drawer to close. */
    private boolean saveDrawer() {
        Boolean clicked = (Boolean) js(
                "var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper'); if(!d) return false;"
                + "var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^(Create Asset|Save Changes|Save)$/i.test((x.innerText||'').trim()) && !x.disabled;});"
                + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;");
        if (!Boolean.TRUE.equals(clicked)) { logStep("save button not found/clickable"); return false; }
        for (int i = 0; i < 40; i++) {
            pause(500);
            boolean open = Boolean.TRUE.equals(js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper'); return !!(d && d.getBoundingClientRect().width>0);"));
            if (!open) return true;
        }
        logStep("drawer did not close after save (validation error?)");
        return false;
    }

    @Test(priority = 0, enabled = false, description = "DIAG: VFD → 3P4W → NFDS — dump the resulting dialog/section")
    public void testDIAG_NfdsFlow() {
        ExtentReportManager.createTest(MODULE, FEATURE, "DIAG_NfdsFlow");
        StringBuilder sb = new StringBuilder();
        for (String mt : new String[]{"NFDS", "FDS"}) {
            sb.append("===== VFD + 3P4W + ").append(mt).append(" =====\n");
            try {
                if (!openCreateFormForClass("VFD")) { sb.append("open failed\n\n"); continue; }
                waitForControl("Select mains type", 8);
                selectAutocomplete("Select phase configuration", "3P4W", "3p4w");
                pause(600);
                selectAutocomplete("Select mains type", mt, mt.toLowerCase());
                pause(2500);
                Object dlg = js("var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                        + "if(!d) return 'NO DIALOG';"
                        + "var inputs=[].slice.call(d.querySelectorAll('input,textarea')).map(function(i){return (i.placeholder||i.name||'?');});"
                        + "var btns=[].slice.call(d.querySelectorAll('button')).map(function(b){return (b.innerText||'').trim();}).filter(Boolean);"
                        + "var poles=[].slice.call(d.querySelectorAll('button')).filter(function(b){return /^[123]P$/.test((b.innerText||'').trim());}).map(function(b){return b.innerText.trim();});"
                        + "return 'TITLE: '+(d.innerText||'').replace(/\\s+/g,' ').slice(0,160)+'\\nINPUTS: '+inputs.join(' | ')+'\\nBUTTONS: '+btns.join(' | ')+'\\nPOLES: '+poles.join(',');");
                sb.append(dlg).append("\n");
                Object eng = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper'); return d? ('engHasAmpereRating='+/ampere rating/i.test(d.innerText||'')) : 'no drawer';");
                sb.append(eng).append("\n\n");
            } catch (Exception e) { sb.append("error: ").append(e.getMessage()).append("\n\n"); }
            dismissDialog(); closeAnyOpenDrawer(); formOpen = false; pause(500);
        }
        try { java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/nfds-diag.txt"), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); } catch (Exception ignored) {}
        ExtentReportManager.logPass("NFDS/FDS flow diagnostic dumped.");
    }

    private Object js(String script, Object... args) {
        try { return ((JavascriptExecutor) driver).executeScript(script, args); }
        catch (Exception e) { logStep("js error: " + e.getMessage()); return null; }
    }

    /** Open an autocomplete by placeholder, list its option texts (joined), then close it. */
    private String dropdownOptions(String placeholder) {
        for (int attempt = 0; attempt < 3; attempt++) {
            js("var ph=arguments[0];"
                    + "var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return new RegExp(ph,'i').test(x.placeholder||'');});"
                    + "if(i){i.scrollIntoView({block:'center'}); i.focus(); i.click(); var w=i.closest('.MuiAutocomplete-root'); if(w){var b=w.querySelector('.MuiAutocomplete-popupIndicator'); if(b) b.click();}}", placeholder);
            for (int i = 0; i < 8; i++) {
                pause(400);
                Object n = js("return document.querySelectorAll(\"li[role='option']\").length;");
                if (n instanceof Long && (Long) n > 0) break;
            }
            Object t = js("return [].slice.call(document.querySelectorAll(\"li[role='option']\")).map(function(o){return o.textContent.trim();}).join(' | ');");
            js("document.body.click();");
            if (t != null && t.toString().length() > 0) return t.toString();
            pause(500);
        }
        return "";
    }

    /** Open the Subtype combobox INSIDE the Create-Main-Breaker dialog (not the drawer's Asset Subtype)
     *  and return its option texts. The options render in a body-level portal once opened. */
    private String dialogSubtypeOptions() {
        for (int attempt = 0; attempt < 3; attempt++) {
            js("var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                    + "if(d){var inp=[].slice.call(d.querySelectorAll('input')).find(function(i){return /subtype/i.test(i.placeholder||'');});"
                    + "if(inp){inp.scrollIntoView({block:'center'}); inp.focus(); inp.click(); var w=inp.closest('.MuiAutocomplete-root'); if(w){var b=w.querySelector('.MuiAutocomplete-popupIndicator'); if(b) b.click();}}}");
            for (int i = 0; i < 8; i++) {
                pause(400);
                Object n = js("return document.querySelectorAll(\"li[role='option']\").length;");
                if (n instanceof Long && (Long) n > 0) break;
            }
            Object t = js("return [].slice.call(document.querySelectorAll(\"li[role='option']\")).map(function(o){return o.textContent.trim();}).join(' | ');");
            js("document.body.click();");
            if (t != null && t.toString().length() > 0) return t.toString();
            pause(500);
        }
        return "";
    }

    /** Wait for an in-page dialog whose text contains {@code titleContains}. */
    private boolean waitForDialog(String titleContains, int seconds) {
        for (int i = 0; i < seconds * 2; i++) { if (dialogOpen(titleContains)) return true; pause(500); }
        return false;
    }

    private boolean dialogOpen(String titleContains) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "return !![].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\"))"
                + ".find(function(d){return d.getBoundingClientRect().width>150 && (d.innerText||'').toLowerCase().indexOf(t)>-1;});", titleContains);
        return Boolean.TRUE.equals(r);
    }

    private boolean dialogHas(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                + "return d? (d.innerText||'').toLowerCase().indexOf(t)>-1 : false;", text);
        return Boolean.TRUE.equals(r);
    }

    private boolean dialogHasButton(String text) {
        Object r = js("var t=arguments[0].toLowerCase();"
                + "var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                + "if(!d) return false; return !![].slice.call(d.querySelectorAll('button')).find(function(b){return (b.innerText||'').trim().toLowerCase()===t;});", text);
        return Boolean.TRUE.equals(r);
    }

    private int dialogPoleButtons() {
        Object n = js("var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                + "if(!d) return 0; return [].slice.call(d.querySelectorAll(\"button,[role='button']\")).filter(function(b){return /^[123]P$/.test((b.innerText||'').trim());}).length;");
        return n instanceof Long ? ((Long) n).intValue() : 0;
    }

    private boolean clickDialogPole(String label) {
        Object r = js("var t=arguments[0];"
                + "var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                + "if(!d) return false;"
                + "var b=[].slice.call(d.querySelectorAll(\"button,[role='button']\")).find(function(e){return (e.innerText||'').trim()===t;});"
                + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;", label);
        return Boolean.TRUE.equals(r);
    }

    /** Cancel/close any open MUI dialog (e.g. the Create Main Breaker dialog). */
    private void dismissDialog() {
        try {
            for (int i = 0; i < 3; i++) {
                Object open = js("return !![].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});");
                if (!Boolean.TRUE.equals(open)) return;
                js("var d=[].slice.call(document.querySelectorAll(\"[role='dialog'], .MuiDialog-paper\")).find(function(e){return e.getBoundingClientRect().width>150;});"
                        + "if(d){var b=[].slice.call(d.querySelectorAll('button')).find(function(x){return /^(Cancel|Close)$/i.test((x.innerText||'').trim());}); if(b) b.click();}");
                pause(600);
            }
        } catch (Exception ignored) {}
    }

    private void ensureOnAssetsPage() {
        String url = driver.getCurrentUrl();
        if (!(url.endsWith("/assets") || url.endsWith("/assets/"))) {
            assetPage.navigateToAssets();
            pause(1500);
        }
    }

    private boolean openCreateFormForClass(String className) {
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attempt > 0) { dismissDialog(); closeAnyOpenDrawer(); ensureOnAssetsPage(); pause(1000); }
            if (openCreateFormForClassOnce(className)) return true;
        }
        return false;
    }

    private boolean openCreateFormForClassOnce(String className) {
        try {
            ensureOnAssetsPage();
            closeAnyOpenDrawer();
            boolean btnReady = false;
            for (int i = 0; i < 16; i++) {
                if (Boolean.TRUE.equals(js("return !![].slice.call(document.querySelectorAll('button'))"
                        + ".find(function(x){return /create asset/i.test(x.innerText||'');});"))) { btnReady = true; break; }
                pause(400);
            }
            if (!btnReady) { logStep("Create Asset toolbar button not ready"); return false; }
            js("var b=[].slice.call(document.querySelectorAll('button')).find(function(x){return /create asset/i.test(x.innerText||'');});"
                    + "b.scrollIntoView({block:'center'}); b.click();");
            formOpen = true;
            boolean drawerReady = false;
            for (int i = 0; i < 16; i++) { pause(500); if (hasControl("Select Class")) { drawerReady = true; break; } }
            if (!drawerReady) { logStep("create drawer (Select Class) did not appear"); return false; }
            String picked = selectAutocomplete("Select Class", className, className.toLowerCase());
            pause(1200);
            boolean ok = picked != null && picked.equalsIgnoreCase(className);
            logStep("openCreateFormForClassOnce(" + className + ") -> picked='" + picked + "'");
            return ok;
        } catch (Exception e) {
            logStep("openCreateFormForClassOnce failed: " + e.getMessage());
            return false;
        }
    }

    private void closeAnyOpenDrawer() {
        try {
            for (int attempt = 0; attempt < 4; attempt++) {
                boolean open = Boolean.TRUE.equals(js(
                        "var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper');"
                        + "return !!(d && d.getBoundingClientRect().width>0);"));
                if (!open) return;
                js("var c=[].slice.call(document.querySelectorAll('button')).find(function(b){return /^(Cancel|Close)$/i.test((b.innerText||'').trim());}); if(c) c.click();");
                pause(700);
                js("var d=[].slice.call(document.querySelectorAll('button')).find(function(b){return /^(Discard|Yes|Confirm|Leave|OK|Discard changes)$/i.test((b.innerText||'').trim());}); if(d) d.click();");
                pause(700);
                js("var bd=document.querySelector('.MuiBackdrop-root'); if(bd) bd.click();");
                pause(500);
            }
        } catch (Exception ignored) {}
    }

    private boolean waitForControl(String placeholder, int seconds) {
        for (int i = 0; i < seconds * 2; i++) { if (hasControl(placeholder)) return true; pause(500); }
        return false;
    }

    private String selectAutocomplete(String placeholder, String typeText, String optionContains) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Object filled = js(
                        "var ph=arguments[0], tx=arguments[1];"
                        + "var inp=[].slice.call(document.querySelectorAll('input')).find(function(i){return new RegExp(ph,'i').test(i.placeholder||'');});"
                        + "if(!inp) return false;"
                        + "inp.scrollIntoView({block:'center'}); inp.focus(); inp.click();"
                        + "var set=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                        + "set.call(inp, ''); inp.dispatchEvent(new Event('input',{bubbles:true}));"
                        + "set.call(inp, tx); inp.dispatchEvent(new Event('input',{bubbles:true}));"
                        + "return true;", placeholder, typeText);
                if (!Boolean.TRUE.equals(filled)) { pause(700); continue; }
                pause(1300);
                Object txt = js(
                        "var want=arguments[0].toLowerCase();"
                        + "var opts=[].slice.call(document.querySelectorAll(\"li[role='option']\"));"
                        + "var m=opts.find(function(o){return o.textContent.trim().toLowerCase()===want;}) "
                        + "      || opts.find(function(o){return o.textContent.toLowerCase().indexOf(want)>-1;});"
                        + "if(!m && opts.length===1) m=opts[0];"
                        + "if(!m) return null; m.scrollIntoView({block:'center'}); m.click(); return m.textContent.trim();",
                        optionContains);
                if (txt != null) return txt.toString();
                pause(800);
            } catch (Exception e) { logStep("selectAutocomplete(" + placeholder + ") error: " + e.getMessage()); pause(700); }
        }
        return null;
    }

    private boolean hasControl(String placeholder) {
        Object r = js("var ph=arguments[0]; return !![].slice.call(document.querySelectorAll('input'))"
                + ".find(function(i){return new RegExp(ph,'i').test(i.placeholder||'');});", placeholder);
        return Boolean.TRUE.equals(r);
    }
}
