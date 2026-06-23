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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Asset Module — ENGINEERING section <b>exhaustive option coverage</b> (308 data-driven cases).
 *
 * <p>Where the matrix suite ({@link AssetEngineeringMatrixTestNG}) verifies each class shows the right
 * <i>fields</i>, this suite verifies the actual <b>option values</b> the Engineering controls offer —
 * every (class, subtype), (class, phase-config), (class, mains-type), (class, voltage-level) pair, plus
 * per-class section presence, voltage mode, conductor fields, transformer fields, manufacturer and
 * pole/fuse-count presence. Authored from the live option sets (subtypes from
 * {@code testcase/node_classes_gold.json}; voltages/phase/mains discovered live).</p>
 *
 * <p><b>Efficiency:</b> each class's create form is opened <i>once</i> and its dropdown option lists are
 * cached; every per-value test is then a cache lookup. Non-destructive — every open is cancelled.</p>
 */
public class AssetEngineeringExhaustiveTestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_ASSET;
    private static final String FEATURE = "Engineering — exhaustive options";

    private static final String[] ALL_CLASSES = {
            "ATS", "Battery", "Busduct", "Busway", "Cable", "Capacitor", "Capacitor Bank", "Circuit Breaker",
            "Default", "Disconnect Switch", "Fuse", "Generator", "Junction Box", "Lighting Controls", "Load",
            "Loadcenter", "MCC", "MCC Bucket", "Meter", "Motor", "Motor Controller", "Motor Starter", "Other",
            "Other (OCP)", "PDU", "Panelboard", "Rectifier", "Relay", "Series Reactor", "Shunt Reactor",
            "Switch", "Switchboard", "Tie Breaker", "Transformer", "Transformer (3-Winding)", "UPS", "Utility",
            "VFD", "VFD Panel"
    };

    private static final String[] PHASE_MAINS_CLASSES = {
            "Panelboard", "MCC", "Switchboard", "PDU", "Motor Starter", "Other", "VFD", "VFD Panel"
    };
    private static final String[] PHASE_CONFIGS = {"3P4W", "3P4W-HLD", "3P3W", "1P3W", "1P2W"};
    private static final String[] MAINS_TYPES = {"MCB", "MLO", "FDS", "NFDS"};

    private static final String[] VOLTAGE_CLASSES = {"Battery", "Generator", "Utility", "Transformer"};
    private static final String[] VOLTAGES = {
            "120V", "208V", "240V", "277V", "347V", "480V", "600V", "2.4kV", "4.16kV",
            "12.47kV", "13.2kV", "13.8kV", "14.4kV", "23kV", "34.5kV", "69kV"
    };

    /** Asset subtypes per class (from node_classes_gold.json) — 65 (class, subtype) pairs. */
    private static final Map<String, String[]> SUBTYPES = new LinkedHashMap<>();
    static {
        SUBTYPES.put("ATS", new String[]{"Automatic Transfer Switch (<= 1000V)", "Automatic Transfer Switch (> 1000V)", "Transfer Switch (<= 1000V)", "Transfer Switch (> 1000V)"});
        SUBTYPES.put("Battery", new String[]{"Lithium-Ion"});
        SUBTYPES.put("Capacitor", new String[]{"Power Factor Correction"});
        SUBTYPES.put("Circuit Breaker", new String[]{"Low-Voltage Insulated Case Circuit Breaker", "Low-Voltage Molded Case Circuit Breaker (<= 225A)", "Low-Voltage Molded Case Circuit Breaker (> 225A)", "Low-Voltage Power Circuit Breaker", "Medium-Voltage Air Magnetic Circuit Breaker", "Medium-Voltage Gas Insulated Circuit Breaker", "Medium-Voltage Oil Insulated Circuit Breaker", "Medium-Voltage Vacuum Circuit Breaker", "Motor Circuit Protector", "Recloser (<= 1000V)", "Recloser (> 1000V)"});
        SUBTYPES.put("Disconnect Switch", new String[]{"Bolted-Pressure Switch (BPS)", "Bypass-Isolation Switch (<= 1000V)", "Bypass-Isolation Switch (> 1000V)", "Disconnect Switch (<= 1000V)", "Disconnect Switch (> 1000V)", "Fused Disconnect Switch (<= 1000V)", "Fused Disconnect Switch (>1000V)", "High-Pressure Contact Switch (HPC)", "Load-Interruptor Switch", "Scrubtype"});
        SUBTYPES.put("Fuse", new String[]{"Fuse (<= 1000V)", "Fuse (> 1000V)"});
        SUBTYPES.put("Load", new String[]{"General Load", "Resistive Load"});
        SUBTYPES.put("MCC", new String[]{"Motor Control Equipment (<= 1000V)", "Motor Control Equipment (> 1000V)"});
        SUBTYPES.put("Motor", new String[]{"Low-Voltage Machine (<= 200hp)", "Low-Voltage Machine (>200hp)", "Medium-Voltage Induction Machine", "Medium-Voltage Synchronous Machine"});
        SUBTYPES.put("Other", new String[]{"Battery Energy Storage System (ESS)", "Electrical Vehicle Charging Station", "Ni-Cad Battery", "Solar Photovoltaic System", "Valve-Regulated Lead-Acid Battery", "Vented Lead-Acid Battery", "Wind Power System"});
        SUBTYPES.put("Panelboard", new String[]{"Branch Panel", "Control Panel", "Panelboard", "Power Panel"});
        SUBTYPES.put("Relay", new String[]{"Electromechanical Relay", "Microprocessor Relay", "Solid-State Relay"});
        SUBTYPES.put("Switchboard", new String[]{"Distribution Panelboard", "Switchboard", "Switchgear (<= 1000V)", "Switchgear (> 1000V)", "Unitized Substation (USS) (<= 1000V)", "Unitized Substation (USS) (> 1000V)"});
        SUBTYPES.put("Transformer", new String[]{"Dry Transformer", "Dry-Type Transformer (<= 600V)", "Dry-Type Transformer (> 600V)", "Oil-Filled Transformer"});
        SUBTYPES.put("UPS", new String[]{"Hybrid UPS System", "I don't node", "Rotary UPS System", "Static UPS System"});
    }

    /** Voltage mode per class (DERIVED read-only / EDITABLE select / BOTH / NONE). */
    private static final Map<String, String> VOLTAGE_MODE = new HashMap<>();
    static {
        for (String c : ALL_CLASSES) VOLTAGE_MODE.put(c, "DERIVED");
        VOLTAGE_MODE.put("Battery", "EDITABLE"); VOLTAGE_MODE.put("Generator", "EDITABLE"); VOLTAGE_MODE.put("Utility", "EDITABLE");
        VOLTAGE_MODE.put("Transformer", "BOTH"); VOLTAGE_MODE.put("Transformer (3-Winding)", "BOTH");
        VOLTAGE_MODE.put("Busway", "NONE"); VOLTAGE_MODE.put("Cable", "NONE");
    }

    /** Cached per-class Engineering snapshot (form opened once). */
    static class ClassEng {
        boolean opened;
        String section = "", subtypes = "", phase = "", mains = "", voltage = "";
        boolean derivedV, editableV, manufacturer;
    }

    private final Map<String, ClassEng> cache = new HashMap<>();
    private boolean formOpen = false;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Asset ENGINEERING — exhaustive option coverage (308 TCs)");
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

    // ================================================================
    // DATA PROVIDERS
    // ================================================================

    @DataProvider(name = "subtypePairs")
    public Object[][] subtypePairs() {
        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<String, String[]> e : SUBTYPES.entrySet())
            for (String s : e.getValue()) rows.add(new Object[]{e.getKey(), s});
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "phasePairs")
    public Object[][] phasePairs() {
        List<Object[]> rows = new ArrayList<>();
        for (String c : PHASE_MAINS_CLASSES) for (String p : PHASE_CONFIGS) rows.add(new Object[]{c, p});
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "mainsPairs")
    public Object[][] mainsPairs() {
        List<Object[]> rows = new ArrayList<>();
        for (String c : PHASE_MAINS_CLASSES) for (String m : MAINS_TYPES) rows.add(new Object[]{c, m});
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "voltagePairs")
    public Object[][] voltagePairs() {
        List<Object[]> rows = new ArrayList<>();
        for (String c : VOLTAGE_CLASSES) for (String v : VOLTAGES) rows.add(new Object[]{c, v});
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "allClasses")
    public Object[][] allClasses() {
        Object[][] d = new Object[ALL_CLASSES.length][1];
        for (int i = 0; i < ALL_CLASSES.length; i++) d[i][0] = ALL_CLASSES[i];
        return d;
    }

    @DataProvider(name = "conductorPairs")
    public Object[][] conductorPairs() {
        Object[][] busway = {{"Busway", "Length"}, {"Busway", "Conductor Material"}, {"Busway", "Busway Size"}, {"Busway", "Construction"}, {"Busway", "Insulation"}};
        Object[][] cable = {{"Cable", "Length"}, {"Cable", "Conductor Material"}, {"Cable", "Cable Size"}, {"Cable", "Conductor Description"}, {"Cable", "Insulation Class"}, {"Cable", "Insulation Type"}, {"Cable", "Installation"}, {"Cable", "Duct Material"}};
        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : busway) rows.add(r);
        for (Object[] r : cable) rows.add(r);
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "transformerPairs")
    public Object[][] transformerPairs() {
        return new Object[][]{
            {"Transformer", "Dry Type"}, {"Transformer", "Oil-Filled"}, {"Transformer", "kVA Rating"}, {"Transformer", "% Impedance"},
            {"Transformer (3-Winding)", "Tertiary Voltage"}, {"Transformer (3-Winding)", "kVA Rating"}, {"Transformer (3-Winding)", "% Impedance"}
        };
    }

    @DataProvider(name = "manufacturerClasses")
    public Object[][] manufacturerClasses() {
        return new Object[][]{{"Circuit Breaker"}, {"Fuse"}, {"Relay"}, {"Switch"}, {"Transformer"}};
    }

    @DataProvider(name = "countPairs")
    public Object[][] countPairs() {
        return new Object[][]{{"Circuit Breaker", "Pole Count"}, {"Switch", "Pole Count"}, {"Other (OCP)", "Pole Count"}, {"Fuse", "Fuse Count"}};
    }

    // ================================================================
    // TESTS (one invocation = one test case)
    // ================================================================

    @Test(priority = 1, dataProvider = "allClasses",
          description = "ENGX-PRESENT: the ENGINEERING section renders for the asset class")
    public void testEngineeringSectionPresent(String cls) {
        ExtentReportManager.createTest(MODULE, FEATURE, "PRESENT — " + cls);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for class '" + cls + "'.");
        Assert.assertFalse(e.section.isEmpty(), "'" + cls + "' should render an ENGINEERING section.");
        ExtentReportManager.logPass("'" + cls + "' ENGINEERING present: " + trim(e.section));
    }

    @Test(priority = 2, dataProvider = "allClasses",
          description = "ENGX-VMODE: the asset class shows the correct voltage mode")
    public void testVoltageMode(String cls) {
        ExtentReportManager.createTest(MODULE, FEATURE, "VMODE — " + cls);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        String mode = VOLTAGE_MODE.get(cls);
        switch (mode) {
            case "DERIVED":  Assert.assertTrue(e.derivedV && !e.editableV, "'" + cls + "' should have a read-only (derived) System Voltage. section=" + trim(e.section)); break;
            case "EDITABLE": Assert.assertTrue(e.editableV && !e.derivedV, "'" + cls + "' should have an editable Voltage (Select voltage)."); break;
            case "BOTH":     Assert.assertTrue(e.derivedV && e.editableV, "'" + cls + "' should have derived Primary + selectable Secondary voltage."); break;
            case "NONE":     Assert.assertTrue(!e.derivedV && !e.editableV, "'" + cls + "' (conductor) should have no voltage field."); break;
            default: Assert.fail("unknown mode " + mode);
        }
        ExtentReportManager.logPass("'" + cls + "' voltage mode = " + mode + ".");
    }

    @Test(priority = 3, dataProvider = "subtypePairs",
          description = "ENGX-SUBTYPE: the Asset Subtype dropdown offers the expected subtype")
    public void testSubtypeOffered(String cls, String subtype) {
        ExtentReportManager.createTest(MODULE, FEATURE, "SUBTYPE — " + cls + " / " + subtype);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.subtypes.toLowerCase().contains(subtype.toLowerCase()),
                "'" + cls + "' Asset Subtype should offer '" + subtype + "'. Options: " + e.subtypes);
        ExtentReportManager.logPass("'" + cls + "' offers subtype '" + subtype + "'.");
    }

    @Test(priority = 4, dataProvider = "phasePairs",
          description = "ENGX-PHASE: the Phase Configuration dropdown offers the expected configuration")
    public void testPhaseConfigOffered(String cls, String config) {
        ExtentReportManager.createTest(MODULE, FEATURE, "PHASE — " + cls + " / " + config);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.phase.contains(config),
                "'" + cls + "' Phase Configuration should offer '" + config + "'. Options: " + e.phase);
        ExtentReportManager.logPass("'" + cls + "' Phase Configuration offers '" + config + "'.");
    }

    @Test(priority = 5, dataProvider = "mainsPairs",
          description = "ENGX-MAINS: the Mains Type dropdown offers the expected type")
    public void testMainsTypeOffered(String cls, String mainsType) {
        ExtentReportManager.createTest(MODULE, FEATURE, "MAINS — " + cls + " / " + mainsType);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.mains.contains(mainsType),
                "'" + cls + "' Mains Type should offer '" + mainsType + "'. Options: " + e.mains);
        ExtentReportManager.logPass("'" + cls + "' Mains Type offers '" + mainsType + "'.");
    }

    @Test(priority = 6, dataProvider = "voltagePairs",
          description = "ENGX-VOLTAGE: the Voltage dropdown offers the expected level")
    public void testVoltageLevelOffered(String cls, String voltage) {
        ExtentReportManager.createTest(MODULE, FEATURE, "VOLTAGE — " + cls + " / " + voltage);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.voltage.contains(voltage),
                "'" + cls + "' Voltage should offer '" + voltage + "'. Options: " + e.voltage);
        ExtentReportManager.logPass("'" + cls + "' Voltage offers '" + voltage + "'.");
    }

    @Test(priority = 7, dataProvider = "conductorPairs",
          description = "ENGX-CONDUCTOR: the conductor class exposes the expected engineering field")
    public void testConductorField(String cls, String field) {
        ExtentReportManager.createTest(MODULE, FEATURE, "CONDUCTOR — " + cls + " / " + field);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.section.toLowerCase().contains(field.toLowerCase()),
                "'" + cls + "' Engineering should expose '" + field + "'. section=" + trim(e.section));
        ExtentReportManager.logPass("'" + cls + "' exposes '" + field + "'.");
    }

    @Test(priority = 8, dataProvider = "transformerPairs",
          description = "ENGX-XFMR: the transformer class exposes the expected engineering field")
    public void testTransformerField(String cls, String field) {
        ExtentReportManager.createTest(MODULE, FEATURE, "XFMR — " + cls + " / " + field);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.section.toLowerCase().contains(field.toLowerCase()),
                "'" + cls + "' Engineering should expose '" + field + "'. section=" + trim(e.section));
        ExtentReportManager.logPass("'" + cls + "' exposes '" + field + "'.");
    }

    @Test(priority = 9, dataProvider = "manufacturerClasses",
          description = "ENGX-MFR: the library-match class exposes a Manufacturer control")
    public void testManufacturerPresent(String cls) {
        ExtentReportManager.createTest(MODULE, FEATURE, "MFR — " + cls);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.manufacturer, "'" + cls + "' Engineering should expose a Manufacturer (library match) control.");
        ExtentReportManager.logPass("'" + cls + "' exposes a Manufacturer control.");
    }

    @Test(priority = 10, dataProvider = "countPairs",
          description = "ENGX-COUNT: the class exposes its Pole/Fuse Count control")
    public void testPoleFuseCount(String cls, String label) {
        ExtentReportManager.createTest(MODULE, FEATURE, "COUNT — " + cls + " / " + label);
        ClassEng e = eng(cls);
        Assert.assertTrue(e.opened, "Create form should open for '" + cls + "'.");
        Assert.assertTrue(e.section.toLowerCase().contains(label.toLowerCase()),
                "'" + cls + "' Engineering should expose '" + label + "'. section=" + trim(e.section));
        ExtentReportManager.logPass("'" + cls + "' exposes '" + label + "'.");
    }

    // ================================================================
    // CACHE — open each class's form ONCE, snapshot its Engineering options
    // ================================================================

    private ClassEng eng(String cls) {
        ClassEng c = cache.get(cls);
        if (c != null) return c;
        c = new ClassEng();
        try {
            if (openCreateFormForClass(cls)) {
                pause(800);
                c.opened = true;
                c.section = engSectionText();
                c.derivedV = hasDisabledControl("Derived from upstream");
                c.editableV = hasControl("Select voltage");
                c.manufacturer = hasControl("Select manufacturer");
                if (hasControl("Select Subtype")) c.subtypes = dropdownOptions("Select Subtype");
                if (hasControl("Select phase configuration")) c.phase = dropdownOptions("Select phase configuration");
                if (hasControl("Select mains type")) c.mains = dropdownOptions("Select mains type");
                if (c.editableV) c.voltage = dropdownOptions("Select voltage");
            }
        } catch (Exception e) { logStep("eng(" + cls + ") error: " + e.getMessage()); }
        finally { closeAnyOpenDrawer(); formOpen = false; }
        cache.put(cls, c);
        return c;
    }

    private static String trim(String s) { return s == null ? "" : (s.length() > 160 ? s.substring(0, 160) + "…" : s); }

    // ================================================================
    // HELPERS (proven across the engineering suites)
    // ================================================================

    private Object js(String script, Object... args) {
        try { return ((JavascriptExecutor) driver).executeScript(script, args); }
        catch (Exception e) { logStep("js error: " + e.getMessage()); return null; }
    }

    private String engSectionText() {
        Object r = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper')||document.body;"
                + "var t=d.innerText||'';"
                + "var m=t.match(/ENGINEERING([\\s\\S]*?)(CUSTOM ATTRIBUTES|COMMERCIAL|NOTES|SCHEDULE|$)/);"
                + "return m? m[1].replace(/[\\u200b]/g,'').replace(/\\s+/g,' ').trim() : '';");
        return r == null ? "" : r.toString();
    }

    private String dropdownOptions(String placeholder) {
        for (int attempt = 0; attempt < 3; attempt++) {
            // focus + click + popup-indicator, then ArrowDown — single-option MUI Autocompletes
            // (e.g. Battery/Capacitor with one subtype) only open their listbox on ArrowDown.
            js("var ph=arguments[0];"
                    + "var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return new RegExp(ph,'i').test(x.placeholder||'');});"
                    + "if(i){i.scrollIntoView({block:'center'}); i.focus(); i.click();"
                    + " var w=i.closest('.MuiAutocomplete-root'); if(w){var b=w.querySelector('.MuiAutocomplete-popupIndicator'); if(b) b.click();}"
                    + " i.dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowDown',code:'ArrowDown',keyCode:40,which:40,bubbles:true}));}", placeholder);
            for (int i = 0; i < 8; i++) {
                pause(400);
                Object n = js("return document.querySelectorAll(\"li[role='option']\").length;");
                if (n instanceof Long && (Long) n > 0) break;
            }
            Object t = js("return [].slice.call(document.querySelectorAll(\"li[role='option']\")).map(function(o){return o.textContent.trim();}).join(' || ');");
            js("document.body.click();");
            pause(300);
            if (t != null && t.toString().length() > 0) return t.toString();
            pause(400);
        }
        return "";
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
}
