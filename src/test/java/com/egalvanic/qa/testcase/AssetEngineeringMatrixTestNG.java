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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Asset Module — ENGINEERING section <b>across every asset class</b> (breadth coverage; the deep
 * library-match flows live in {@link AssetEngineeringTestNG} (Circuit Breaker) and
 * {@link TransformerEngineeringTestNG}).
 *
 * <p>The Engineering block the Add Asset form renders is class-dependent. Mapped live for all 39 classes
 * (see {@code testcase/node_classes_gold.json}); each class is verified to show exactly the Engineering
 * fields the user will see, plus the correct <b>voltage mode</b>:</p>
 * <ul>
 *   <li><b>DERIVED</b> — read-only "System Voltage" ("Derived from upstream"). Most classes.</li>
 *   <li><b>EDITABLE</b> — a selectable "Voltage" ("Select voltage"): Battery, Generator, Utility.</li>
 *   <li><b>BOTH</b> — derived Primary + selectable Secondary/Tertiary: Transformer, Transformer (3-Winding).</li>
 *   <li><b>NONE</b> — no voltage field (conductor classes): Busway, Cable.</li>
 * </ul>
 * Distinctive per-class fields include Asset Subtype, Pole Count / Fuse Count, Manufacturer (library
 * match), Phase Configuration, Mains Type, kVA Rating / % Impedance / Tertiary Voltage, and the
 * conductor fields (Length / Conductor Material / Size / Insulation).
 *
 * <p>{@code testMATRIX_00_Discover} is a disabled discovery diagnostic (dumps each class's section to
 * {@code /tmp/eng-matrix.txt}).</p>
 */
public class AssetEngineeringMatrixTestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_ASSET;
    private static final String FEATURE = "Engineering — per-class matrix";

    private static final String DERIVED = "DERIVED", EDITABLE = "EDITABLE", BOTH = "BOTH", NONE = "NONE";

    /** All 39 asset classes (from testcase/node_classes_gold.json) — used by the discovery diagnostic. */
    private static final String[] ALL_CLASSES = {
            "ATS", "Battery", "Busduct", "Busway", "Cable", "Capacitor", "Capacitor Bank", "Circuit Breaker",
            "Default", "Disconnect Switch", "Fuse", "Generator", "Junction Box", "Lighting Controls", "Load",
            "Loadcenter", "MCC", "MCC Bucket", "Meter", "Motor", "Motor Controller", "Motor Starter", "Other",
            "Other (OCP)", "PDU", "Panelboard", "Rectifier", "Relay", "Series Reactor", "Shunt Reactor",
            "Switch", "Switchboard", "Tie Breaker", "Transformer", "Transformer (3-Winding)", "UPS", "Utility",
            "VFD", "VFD Panel"
    };

    private boolean formOpen = false;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Asset ENGINEERING — per-class matrix (all 39 classes)");
        System.out.println("     " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a - dd MMM")));
        System.out.println("==============================================================\n");
        super.classSetup();
    }

    @BeforeMethod
    @Override
    public void testSetup() {
        super.testSetup();
        try { closeAnyOpenDrawer(); ensureOnAssetsPage(); } catch (Exception ignored) {}
        formOpen = false;
    }

    @AfterMethod(alwaysRun = true)
    public void closeFormAfter(ITestResult result) {
        if (formOpen) { closeAnyOpenDrawer(); formOpen = false; }
    }

    /**
     * Per-class expectations: {className, distinctive Engineering labels (must appear), voltage mode}.
     * Authored from the live discovery dump (all 39 classes).
     */
    @DataProvider(name = "classExpectations")
    public Object[][] classExpectations() {
        return new Object[][]{
            {"ATS",                     new String[]{"Asset Subtype"},                                          DERIVED},
            {"Battery",                 new String[]{"Asset Subtype"},                                          EDITABLE},
            {"Busduct",                 new String[]{"Mains Type"},                                             DERIVED},
            {"Busway",                  new String[]{"Length", "Conductor Material", "Busway Size", "Construction", "Insulation"}, NONE},
            {"Cable",                   new String[]{"Length", "Conductor Material", "Cable Size", "Insulation"}, NONE},
            {"Capacitor",               new String[]{"Asset Subtype"},                                          DERIVED},
            {"Capacitor Bank",          new String[]{},                                                         DERIVED},
            {"Circuit Breaker",         new String[]{"Asset Subtype", "Pole Count", "Manufacturer"},            DERIVED},
            {"Default",                 new String[]{},                                                         DERIVED},
            {"Disconnect Switch",       new String[]{"Asset Subtype", "Mains Type"},                            DERIVED},
            {"Fuse",                    new String[]{"Asset Subtype", "Fuse Count", "Manufacturer"},            DERIVED},
            {"Generator",               new String[]{},                                                         EDITABLE},
            {"Junction Box",            new String[]{"Mains Type"},                                             DERIVED},
            {"Lighting Controls",       new String[]{},                                                         DERIVED},
            {"Load",                    new String[]{"Asset Subtype"},                                          DERIVED},
            {"Loadcenter",              new String[]{},                                                         DERIVED},
            {"MCC",                     new String[]{"Asset Subtype", "Phase Configuration", "Mains Type"},     DERIVED},
            {"MCC Bucket",              new String[]{},                                                         DERIVED},
            {"Meter",                   new String[]{},                                                         DERIVED},
            {"Motor",                   new String[]{"Asset Subtype"},                                          DERIVED},
            {"Motor Controller",        new String[]{},                                                         DERIVED},
            {"Motor Starter",           new String[]{"Phase Configuration", "Mains Type"},                      DERIVED},
            {"Other",                   new String[]{"Asset Subtype", "Phase Configuration", "Mains Type"},     DERIVED},
            {"Other (OCP)",             new String[]{"Pole Count"},                                             DERIVED},
            {"PDU",                     new String[]{"Phase Configuration", "Mains Type"},                      DERIVED},
            {"Panelboard",              new String[]{"Asset Subtype", "Phase Configuration", "Mains Type"},     DERIVED},
            {"Rectifier",               new String[]{},                                                         DERIVED},
            {"Relay",                   new String[]{"Asset Subtype", "Manufacturer"},                          DERIVED},
            {"Series Reactor",          new String[]{},                                                         DERIVED},
            {"Shunt Reactor",           new String[]{},                                                         DERIVED},
            {"Switch",                  new String[]{"Pole Count", "Manufacturer"},                             DERIVED},
            {"Switchboard",             new String[]{"Asset Subtype", "Phase Configuration", "Mains Type"},     DERIVED},
            {"Tie Breaker",             new String[]{},                                                         DERIVED},
            {"Transformer",             new String[]{"Asset Subtype", "kVA Rating", "% Impedance", "Type", "Manufacturer"}, BOTH},
            {"Transformer (3-Winding)", new String[]{"Tertiary Voltage", "kVA Rating", "% Impedance"},          BOTH},
            {"UPS",                     new String[]{"Asset Subtype"},                                          DERIVED},
            {"Utility",                 new String[]{"Configuration"},                                          EDITABLE},
            {"VFD",                     new String[]{"Phase Configuration", "Mains Type"},                      DERIVED},
            {"VFD Panel",               new String[]{"Phase Configuration", "Mains Type"},                      DERIVED},
        };
    }

    // ================================================================
    // TEST — one invocation per asset class
    // ================================================================

    @Test(dataProvider = "classExpectations",
          description = "MATRIX: each asset class renders its expected Engineering fields + voltage mode")
    public void testEngineeringSectionPerClass(String cls, String[] labels, String voltageMode) {
        ExtentReportManager.createTest(MODULE, FEATURE, "MATRIX — " + cls);
        Assert.assertTrue(openCreateFormForClass(cls), "Create form should open for class '" + cls + "'.");

        // the ENGINEERING section renders just after the class is set
        String sec = "";
        for (int i = 0; i < 12; i++) { sec = engSectionText(); if (!sec.isEmpty()) break; pause(400); }
        Assert.assertFalse(sec.isEmpty(), "'" + cls + "' should render an ENGINEERING section (none found).");
        String lo = sec.toLowerCase();

        // 1) distinctive labels for this class must all be present
        List<String> missing = new ArrayList<>();
        for (String t : labels) if (!lo.contains(t.toLowerCase())) missing.add(t);
        Assert.assertTrue(missing.isEmpty(),
                "'" + cls + "' Engineering should show " + Arrays.toString(labels) + " — missing " + missing
                        + ". Section='" + sec + "'");

        // 2) voltage mode (the read-only/editable distinction the user sees)
        switch (voltageMode) {
            case DERIVED:
                Assert.assertTrue(lo.contains("system voltage"),
                        "'" + cls + "' should show derived 'System Voltage'. Section='" + sec + "'");
                Assert.assertTrue(hasDisabledControl("Derived from upstream"),
                        "'" + cls + "' System Voltage should be read-only (Derived from upstream).");
                break;
            case EDITABLE:
                Assert.assertTrue(hasControl("Select voltage"),
                        "'" + cls + "' should expose an editable Voltage ('Select voltage'). Section='" + sec + "'");
                Assert.assertFalse(lo.contains("system voltage"),
                        "'" + cls + "' should show an editable Voltage, not a derived 'System Voltage'.");
                break;
            case BOTH:
                Assert.assertTrue(hasDisabledControl("Derived from upstream"),
                        "'" + cls + "' should show a derived Primary Voltage.");
                Assert.assertTrue(hasControl("Select voltage"),
                        "'" + cls + "' should show a selectable Secondary/Tertiary Voltage ('Select voltage').");
                break;
            case NONE:
                Assert.assertFalse(lo.contains("voltage"),
                        "'" + cls + "' (conductor class) should not show a voltage field. Section='" + sec + "'");
                break;
            default:
                Assert.fail("Unknown voltage mode: " + voltageMode);
        }
        ExtentReportManager.logPass("'" + cls + "' Engineering OK [" + voltageMode + " voltage] — " + sec);
    }

    // ================================================================
    // DISCOVERY (disabled; dumps each class's ENGINEERING section to /tmp/eng-matrix.txt)
    // ================================================================

    @Test(priority = 99, enabled = false, description = "DIAG: dump the ENGINEERING section for every asset class")
    public void testMATRIX_00_Discover() {
        ExtentReportManager.createTest(MODULE, FEATURE, "MATRIX_00_Discover");
        StringBuilder sb = new StringBuilder();
        for (String cls : ALL_CLASSES) {
            String eng = "(form did not open)";
            try {
                if (openCreateFormForClass(cls)) { waitForControl("Select Subtype", 4); pause(800); eng = engineeringSectionDiag(); }
            } catch (Exception e) { eng = "(error: " + e.getMessage() + ")"; }
            sb.append("=== ").append(cls).append(" ===\n").append(eng).append("\n\n");
            closeAnyOpenDrawer();
            formOpen = false;
            pause(400);
        }
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/eng-matrix.txt"),
                    sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) { logStep("matrix write failed: " + e.getMessage()); }
        ExtentReportManager.logPass("Engineering matrix discovery dumped for " + ALL_CLASSES.length + " classes.");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private Object js(String script, Object... args) {
        try { return ((JavascriptExecutor) driver).executeScript(script, args); }
        catch (Exception e) { logStep("js error: " + e.getMessage()); return null; }
    }

    /** Text of the ENGINEERING section in the create drawer (between the heading and the next section). */
    private String engSectionText() {
        Object r = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper')||document.body;"
                + "var t=d.innerText||'';"
                + "var m=t.match(/ENGINEERING([\\s\\S]*?)(CUSTOM ATTRIBUTES|COMMERCIAL|NOTES|$)/);"
                + "return m? m[1].replace(/[\\u200b]/g,'').replace(/\\s+/g,' ').trim() : '';");
        return r == null ? "" : r.toString();
    }

    private String engineeringSectionDiag() {
        Object r = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper')||document.body;"
                + "var txt=d.innerText||'';"
                + "var m=txt.match(/ENGINEERING([\\s\\S]*?)(CUSTOM ATTRIBUTES|COMMERCIAL|NOTES|$)/);"
                + "var sec=m? m[1].replace(/[\\u200b]/g,'').replace(/\\s+/g,' ').trim() : '(NO ENGINEERING SECTION)';"
                + "var phs=[].slice.call(d.querySelectorAll('input')).map(function(i){return i.placeholder||'';}).filter(function(s){return s && /subtype|voltage|manufacturer|select|search|derived/i.test(s);});"
                + "return 'SECTION: '+sec.substring(0,420)+'\\nPLACEHOLDERS: '+phs.join(' | ');");
        return r == null ? "" : r.toString();
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
            if (attempt > 0) { closeAnyOpenDrawer(); ensureOnAssetsPage(); pause(1000); }
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
                        + "var dlg=document.querySelector(\"[role='dialog'], .MuiDialog-root .MuiPaper-root\");"
                        + "return !!((d && d.getBoundingClientRect().width>0) || (dlg && dlg.getBoundingClientRect().width>0));"));
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

    private boolean hasDisabledControl(String placeholder) {
        Object r = js("var ph=arguments[0]; var i=[].slice.call(document.querySelectorAll('input'))"
                + ".find(function(x){return new RegExp(ph,'i').test(x.placeholder||'');}); return i? (i.disabled || i.readOnly) : false;", placeholder);
        return Boolean.TRUE.equals(r);
    }
}
