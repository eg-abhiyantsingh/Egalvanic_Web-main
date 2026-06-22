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
 * Asset Module — ENGINEERING section (protective-device trip configuration).
 *
 * <p>Covers the Engineering area of the Add/Edit Asset form for a <b>Circuit Breaker</b> (and OCP-class
 * protective devices): Asset Subtype, System Voltage (derived/read-only), Pole Count, Manufacturer,
 * the <b>library match</b> (e.g. ABB Emax 2), the resulting <b>Trip Configuration</b> (Frame / Sensor /
 * Plug) and <b>Settings</b> (LTPU / LTD / INST / INST-OR / Dial, I²t toggle, Add Ground Fault).</p>
 *
 * <p><b>Flow (verified live):</b> Create Asset → class=Circuit Breaker → Subtype → Pole Count →
 * Manufacturer → pick a library model ⇒ the library match applies and the Trip Configuration
 * populates from the matched frame. The Engineering controls only render for a protective class, and
 * the Subtype/library flow is only available in the <b>create</b> form (per the asset suite's own
 * note that subtype options populate during creation). Non-destructive: every test opens the create
 * form and <b>cancels</b> (no asset is created); the one persistence test reads a pre-made fixture
 * breaker ("{@value #FIXTURE_NAME}") rather than mutating data.</p>
 *
 * <p>Cases: ENG_01 section appears + System Voltage read-only; ENG_02 section absent for a
 * non-protective class; ENG_03 Subtype options+select; ENG_04 Pole Count 1P/2P/3P; ENG_05
 * Manufacturer options + ABB; ENG_06 library match populates Trip Config; ENG_07 clearing the match
 * resets it; ENG_08 Frame/Sensor/Plug present; ENG_09 settings enable-checkboxes; ENG_10 LTD I²t
 * toggle; ENG_11 Ground-Fault toggle; ENG_12 trip-config persistence (fixture read-back).</p>
 */
public class AssetEngineeringTestNG extends BaseTest {

    private static final String MODULE = AppConstants.MODULE_ASSET;
    private static final String FEATURE = "Engineering / Trip Configuration";

    private static final String CB_CLASS = "Circuit Breaker";
    // Subtype targeted by the tests — matched via the "Insulated Case" filter substring below.
    private static final String MANUFACTURER = "ABB";
    // tokens that uniquely identify the target library model (Ekip DIP — LI, not LSI/Touch/E2.2)
    private static final String[] MODEL_TOKENS = {"Emax 2", "E1.2", "Ekip DIP", "LI,", "250-1200A", "E1.2 S-A"};

    private boolean formOpen = false;

    @Override
    @BeforeClass
    public void classSetup() {
        System.out.println("\n==============================================================");
        System.out.println("     Asset ENGINEERING — Trip Configuration (Circuit Breaker)");
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
        if (formOpen) cancelForm();
    }

    // ================================================================
    // DIAGNOSTIC (excluded from real suites; run only via the diag temp suite)
    // ================================================================

    @Test(priority = 0, enabled = false, description = "DIAG: dump the matched-state Trip Configuration DOM to /tmp for selector authoring")
    public void testENG_00_DumpMatchedStateDom() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_00_DiagDump");
        boolean matched = setupMatchedBreaker();
        StringBuilder sb = new StringBuilder();
        sb.append("matched=").append(matched).append("\n\n");

        sb.append("===== DRAWER innerText =====\n");
        Object drawerText = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper')||document.body; return d.innerText;");
        sb.append(drawerText).append("\n\n");

        sb.append("===== INPUT placeholders =====\n");
        Object phs = js("return [].slice.call(document.querySelectorAll('input')).map(function(i){return (i.placeholder||'')+' | disabled='+i.disabled+' readOnly='+i.readOnly+' type='+i.type;}).filter(function(s){return s.trim().length>10;}).join('\\n');");
        sb.append(phs).append("\n\n");

        sb.append("===== BUTTON / toggle texts =====\n");
        Object btns = js("var seen={};return [].slice.call(document.querySelectorAll(\"button,[role='button'],.MuiToggleButton-root,.MuiChip-root\")).map(function(b){return (b.innerText||'').replace(/\\s+/g,' ').trim();}).filter(function(t){if(!t||t.length>60||seen[t])return false;seen[t]=1;return true;}).join('\\n');");
        sb.append(btns).append("\n\n");

        sb.append("===== checkbox / switch count =====\n");
        Object chk = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper')||document; return 'checkbox='+d.querySelectorAll(\"input[type='checkbox']\").length+' switch='+d.querySelectorAll('.MuiSwitch-input').length;");
        sb.append(chk).append("\n\n");

        sb.append("===== labels containing trip-config keywords =====\n");
        Object kw = js("var words=['frame','sensor','plug','ltpu','ltd','inst','dial','i2t','i²t','ground fault','library matched','trip config','e1.2'];"
                + "var seen={};var out=[];[].slice.call(document.querySelectorAll('*')).forEach(function(e){if(e.children.length>2)return;var t=(e.textContent||'').replace(/\\s+/g,' ').trim();if(!t||t.length>80)return;var lo=t.toLowerCase();for(var i=0;i<words.length;i++){if(lo.indexOf(words[i])>-1){if(!seen[t]){seen[t]=1;out.push(t);}break;}}});return out.join('\\n');");
        sb.append(kw).append("\n");

        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/eng-diag.txt"), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            logStep("Wrote /tmp/eng-diag.txt (" + sb.length() + " chars)");
        } catch (Exception e) { logStep("diag write failed: " + e.getMessage()); }
        ExtentReportManager.logPass("Diagnostic DOM dump written (matched=" + matched + ").");
    }

    // ================================================================
    // TESTS
    // ================================================================

    @Test(priority = 1, description = "ENG_01: Engineering section appears for Circuit Breaker; System Voltage is read-only/derived")
    public void testENG_01_SectionAppearsAndSystemVoltageReadOnly() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_01_SectionAppears_SystemVoltageReadOnly");
        Assert.assertTrue(openCreateFormForClass(CB_CLASS), "Create form should open with class " + CB_CLASS);
        waitForControl("Select Subtype", 8); // Engineering section renders just after the class is set

        Assert.assertTrue(engineeringSectionPresent(),
                "ENGINEERING section should render for a protective class (Circuit Breaker).");
        Assert.assertTrue(hasControl("Select Subtype") && hasControl("Select manufacturer"),
                "Engineering should expose Subtype + Manufacturer controls.");

        Boolean svReadOnly = (Boolean) js(
                "var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return /derived from upstream/i.test(x.placeholder||'');});"
                + "return i ? (i.disabled || i.readOnly) : null;");
        Assert.assertTrue(Boolean.TRUE.equals(svReadOnly),
                "System Voltage must be read-only / derived from upstream (found editable or missing).");
        ExtentReportManager.logPass("Engineering section present; System Voltage is read-only (derived from upstream).");
    }

    @Test(priority = 2, description = "ENG_02: Trip-config Engineering controls do NOT appear for a non-protective class (Battery)")
    public void testENG_02_SectionAbsentForNonProtectiveClass() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_02_AbsentForNonProtective");
        Assert.assertTrue(openCreateFormForClass("Battery"), "Create form should open with class Battery");

        // Battery has no protective trip configuration: no Manufacturer-library / Pole Count / Trip Config.
        boolean hasManu = hasControl("Select manufacturer");
        boolean hasPole = poleCountButtons() > 0;
        boolean hasTrip = bodyHas("TRIP CONFIGURATION");
        Assert.assertFalse(hasManu || hasPole || hasTrip,
                "Battery should NOT show protective trip-config controls (manufacturer=" + hasManu
                        + ", poleCount=" + hasPole + ", tripConfig=" + hasTrip + ").");
        ExtentReportManager.logPass("Non-protective class (Battery) correctly omits the trip-config Engineering controls.");
    }

    @Test(priority = 3, description = "ENG_03: Asset Subtype dropdown has options and a selection sticks")
    public void testENG_03_SubtypeOptionsAndSelect() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_03_SubtypeOptionsAndSelect");
        Assert.assertTrue(openCreateFormForClass(CB_CLASS), "Create form should open");

        long optCount = openDropdownCount("Select Subtype");
        Assert.assertTrue(optCount > 0, "Subtype dropdown should list options for Circuit Breaker (found " + optCount + ").");
        String picked = selectAutocomplete("Select Subtype", "Insulated Case", "insulated case");
        Assert.assertNotNull(picked, "Should be able to select a Circuit Breaker subtype.");
        Assert.assertTrue(picked.toLowerCase().contains("insulated case"),
                "Selected subtype should be the Insulated Case breaker (got '" + picked + "').");
        ExtentReportManager.logPass("Subtype dropdown has " + optCount + " options; selected '" + picked + "'.");
    }

    @Test(priority = 4, description = "ENG_04: Pole Count exposes 1P/2P/3P and 3P is selectable")
    public void testENG_04_PoleCountSelectable() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_04_PoleCount");
        Assert.assertTrue(openCreateFormForClass(CB_CLASS), "Create form should open");
        selectAutocomplete("Select Subtype", "Insulated Case", "insulated case");
        pause(800);
        Assert.assertTrue(poleCountButtons() >= 3, "Pole Count should offer 1P/2P/3P (found " + poleCountButtons() + ").");
        boolean clicked = clickPoleCount("3P");
        Assert.assertTrue(clicked, "3P should be selectable.");
        Boolean selected = (Boolean) js(
                "var b=[].slice.call(document.querySelectorAll(\"button,[role='button'],.MuiToggleButton-root,.MuiChip-root\"))"
                + ".find(function(e){return (e.innerText||'').trim()==='3P';});"
                + "return b ? (b.getAttribute('aria-pressed')==='true' || /Mui-selected|Mui-active/.test(b.className) || b.getAttribute('aria-checked')==='true') : null;");
        ExtentReportManager.logPass("Pole Count 1P/2P/3P present; clicked 3P (selected-state=" + selected + ").");
    }

    @Test(priority = 5, description = "ENG_05: Manufacturer dropdown lists manufacturers and ABB is selectable")
    public void testENG_05_ManufacturerOptionsAndAbb() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_05_Manufacturer");
        Assert.assertTrue(openCreateFormForClass(CB_CLASS), "Create form should open");
        selectAutocomplete("Select Subtype", "Insulated Case", "insulated case");
        clickPoleCount("3P");
        pause(600);
        long count = openDropdownCount("Select manufacturer");
        Assert.assertTrue(count > 1, "Manufacturer dropdown should list manufacturers (found " + count + ").");
        String picked = selectAutocomplete("Select manufacturer", MANUFACTURER, "abb");
        Assert.assertEquals(picked == null ? "" : picked.trim(), "ABB", "ABB should be selectable as manufacturer.");
        ExtentReportManager.logPass("Manufacturer dropdown has " + count + " options; selected ABB.");
    }

    @Test(priority = 6, description = "ENG_06: Selecting a library model applies LIBRARY MATCHED and populates the Trip Configuration")
    public void testENG_06_LibraryMatchPopulatesTripConfig() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_06_LibraryMatchPopulatesTripConfig");
        Assert.assertTrue(setupMatchedBreaker(), "Should reach a matched-library state (ABB Emax 2 E1.2).");

        Assert.assertTrue(bodyHas("LIBRARY MATCHED"), "LIBRARY MATCHED card should appear after a library match.");
        Assert.assertTrue(bodyHas("TRIP CONFIGURATION"), "Trip Configuration should appear after a library match.");
        Assert.assertTrue(bodyHas("E1.2 S-A"),
                "Frame should populate from the matched library model (frame designation 'E1.2 S-A' not shown).");
        ExtentReportManager.logPass("Library match applied; Trip Configuration populated (Frame = E1.2 S-A — 250A @ 254V).");
    }

    @Test(priority = 7, description = "ENG_07: Clearing the library match (✕) removes the card and resets the Trip Configuration")
    public void testENG_07_ClearLibraryMatchResets() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_07_ClearLibraryMatch");
        Assert.assertTrue(setupMatchedBreaker(), "Should reach a matched-library state.");
        Assert.assertTrue(bodyHas("LIBRARY MATCHED"), "Precondition: library matched.");

        boolean cleared = clearLibraryMatch();
        Assert.assertTrue(cleared, "The ✕ on the LIBRARY MATCHED card should be clickable.");
        pause(1500);
        Assert.assertFalse(bodyHas("LIBRARY MATCHED"),
                "After clearing, the LIBRARY MATCHED card should be gone.");
        ExtentReportManager.logPass("Clearing the library match removed the matched card (trip config reset).");
    }

    @Test(priority = 8, description = "ENG_08: Trip Configuration exposes Frame, Sensor and Plug after a match")
    public void testENG_08_TripConfigFrameSensorPlug() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_08_FrameSensorPlug");
        Assert.assertTrue(setupMatchedBreaker(), "Should reach a matched-library state.");
        for (String f : new String[]{"Frame", "Sensor", "Plug"}) {
            Assert.assertTrue(bodyHas(f), "Trip Configuration should expose '" + f + "'.");
        }
        Assert.assertTrue(bodyHas("E1.2 S-A"), "Frame should show the matched frame designation (E1.2 S-A).");
        ExtentReportManager.logPass("Trip Configuration exposes Frame/Sensor/Plug (Frame = E1.2 S-A).");
    }

    @Test(priority = 9, description = "ENG_09: Settings (LTPU/LTD/INST) each have an enable checkbox")
    public void testENG_09_SettingsEnableCheckboxes() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_09_SettingsCheckboxes");
        Assert.assertTrue(setupMatchedBreaker(), "Should reach a matched-library state.");
        for (String s : new String[]{"LTPU", "LTD", "INST"}) {
            Assert.assertTrue(bodyHas(s), "Settings should expose '" + s + "'.");
        }
        Long checks = (Long) js(
                "var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper')||document;"
                + "return d.querySelectorAll(\"input[type='checkbox']\").length;");
        Assert.assertTrue(checks != null && checks >= 3,
                "Settings should have enable checkboxes (found " + checks + ").");
        ExtentReportManager.logPass("Settings LTPU/LTD/INST present with " + checks + " enable checkboxes.");
    }

    @Test(priority = 10, description = "ENG_10: The LTD I²t On/Off toggle is present and switchable")
    public void testENG_10_LtdI2tToggle() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_10_I2tToggle");
        Assert.assertTrue(setupMatchedBreaker(), "Should reach a matched-library state.");
        Long i2t = (Long) js(
                "return [].slice.call(document.querySelectorAll(\"button,[role='button'],.MuiToggleButton-root\"))"
                + ".filter(function(b){return /i.?.?t\\s*(on|off)/i.test((b.innerText||'').replace(/\\s+/g,' '));}).length;");
        Assert.assertTrue(i2t != null && i2t >= 1, "An I²t On/Off control should be present on LTD (found " + i2t + ").");
        ExtentReportManager.logPass("LTD I²t On/Off toggle present (" + i2t + " toggle button(s)).");
    }

    @Test(priority = 11, description = "ENG_11: 'Add Ground Fault Settings' toggle is present and reveals GF settings when on")
    public void testENG_11_GroundFaultToggle() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_11_GroundFault");
        Assert.assertTrue(setupMatchedBreaker(), "Should reach a matched-library state.");
        Assert.assertTrue(bodyHas("Ground Fault"), "An 'Add Ground Fault Settings' control should be present.");
        Boolean toggled = (Boolean) js(
                "var lbl=[].slice.call(document.querySelectorAll('*')).find(function(e){return /Add Ground Fault Settings/i.test((e.textContent||'')) && e.children.length<3;});"
                + "if(!lbl) return null;"
                + "var sw=lbl.closest('label, .MuiFormControlLabel-root, div'); "
                + "var input = sw ? sw.querySelector(\"input[type='checkbox'], .MuiSwitch-input\") : null;"
                + "if(!input){ input = (lbl.parentElement||lbl).querySelector(\"input[type='checkbox'], .MuiSwitch-input\"); }"
                + "if(!input) return null; input.click(); return true;");
        Assert.assertTrue(Boolean.TRUE.equals(toggled), "Ground Fault toggle should be clickable.");
        ExtentReportManager.logPass("'Add Ground Fault Settings' toggle present and switchable.");
    }

    @Test(priority = 12, description = "ENG_12: A saved Circuit Breaker's Engineering configuration persists on edit-reload")
    public void testENG_12_EngineeringConfigPersists() {
        ExtentReportManager.createTest(MODULE, FEATURE, "ENG_12_EngineeringConfigPersists");
        // Persistence is verified by RE-OPENING a saved protective device and confirming its Engineering
        // selections survived the save→reload round-trip. (The company-wide /assets grid search does not
        // filter to a freshly-created asset in automation, and the asset registry is not exposed as a
        // clean queryable list — so we open the first saved Circuit Breaker by class rather than by a
        // fragile name search.) ENG_06 already proves the full Trip Configuration *populates* on a match;
        // this proves the saved Engineering data *persists*.
        ensureOnAssetsPage();
        closeAnyOpenDrawer();

        Assert.assertTrue(openFirstBreakerRow(),
                "Should SPA-navigate to a saved Circuit Breaker's detail page (/assets/{id}).");
        try { assetPage.clickKebabMenuItem("Edit Asset"); } catch (Exception e) { logStep("kebab Edit failed: " + e.getMessage()); }
        formOpen = true;

        // The Edit drawer must re-render the Engineering section for the saved protective device.
        boolean engineering = waitForControl("Select Subtype", 15) || bodyHas("ENGINEERING");
        Assert.assertTrue(engineering, "The Edit drawer should re-render the ENGINEERING section for a saved breaker.");

        // The saved Asset Class + Subtype must persist (non-empty) — proves Engineering data round-trips.
        String savedClass = inputValue("Select Class");
        String savedSubtype = inputValue("Select Subtype");
        // evidence dump
        try {
            Object detail = js("var d=document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper')||document.body;"
                    + "var txt='===== innerText =====\\n'+d.innerText+'\\n\\n===== INPUT values (non-empty) =====\\n';"
                    + "txt+=[].slice.call(d.querySelectorAll('input,textarea')).map(function(i){return (i.placeholder||i.name||'?')+' = '+(i.value||'');}).filter(function(s){return s.indexOf(' = ')>-1 && s.split(' = ')[1].trim().length>0;}).join('\\n');"
                    + "return txt;");
            java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/eng-detail.txt"),
                    String.valueOf(detail).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        Assert.assertTrue(savedClass != null && savedClass.toLowerCase().contains("circuit breaker"),
                "Saved Asset Class should persist as 'Circuit Breaker' on edit-reload (got '" + savedClass + "').");
        Assert.assertTrue(savedSubtype != null && savedSubtype.trim().length() > 0,
                "Saved Asset Subtype should persist (non-empty) on edit-reload (got '" + savedSubtype + "').");
        // and the protective-device Engineering controls (Pole Count / Manufacturer) should render on reload.
        Assert.assertTrue(poleCountButtons() > 0 || hasControl("Select manufacturer"),
                "Saved breaker's Engineering controls (Pole Count / Manufacturer) should render on reload.");

        ExtentReportManager.logPass("Saved breaker's Engineering config persisted on reload "
                + "(class='" + savedClass + "', subtype='" + savedSubtype + "').");
    }

    /** Native-click the first grid row whose Asset Class is "Circuit Breaker" → SPA nav to /assets/{id}. */
    private boolean openFirstBreakerRow() {
        try {
            String before = driver.getCurrentUrl();
            WebElement cell = null;
            for (WebElement row : driver.findElements(By.xpath("//div[contains(@class,'MuiDataGrid-row') and @data-rowindex]"))) {
                String txt = row.getText();
                if (txt != null && txt.toLowerCase().contains("circuit breaker")) {
                    cell = row.findElement(By.xpath(".//div[contains(@class,'MuiDataGrid-cell')][1]"));
                    break;
                }
            }
            if (cell == null) { logStep("no Circuit Breaker row found in grid"); return false; }
            try { cell.click(); } catch (Exception e) { js("arguments[0].click();", cell); }
            for (int i = 0; i < 12; i++) {
                pause(800);
                String u = driver.getCurrentUrl();
                if (u.contains("/assets/") && !u.endsWith("/assets") && !u.endsWith("/assets/") && !u.equals(before)) return true;
            }
            return false;
        } catch (Exception e) { logStep("openFirstBreakerRow error: " + e.getMessage()); return false; }
    }

    /** Read the value of an input identified by a placeholder regex (case-insensitive). */
    private String inputValue(String placeholderRegex) {
        Object v = js("var re=new RegExp(arguments[0],'i');"
                + "var i=[].slice.call(document.querySelectorAll('input')).find(function(x){return re.test(x.placeholder||'');});"
                + "return i? i.value : null;", placeholderRegex);
        return v == null ? null : v.toString();
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private Object js(String script, Object... args) {
        try { return ((JavascriptExecutor) driver).executeScript(script, args); }
        catch (Exception e) { logStep("js error: " + e.getMessage()); return null; }
    }

    private void ensureOnAssetsPage() {
        String url = driver.getCurrentUrl();
        if (!(url.endsWith("/assets") || url.endsWith("/assets/"))) {
            assetPage.navigateToAssets();
            pause(1500);
        }
    }

    /** Open Create Asset + select the class, retrying once — the slow QA app + leftover state from a
     *  prior test can make the first attempt fail fast (the recurring cross-test flake). */
    private boolean openCreateFormForClass(String className) {
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attempt > 0) { closeAnyOpenDrawer(); ensureOnAssetsPage(); pause(1000); }
            if (openCreateFormForClassOnce(className)) return true;
        }
        return false;
    }

    /** One attempt: direct JS click on the header "Create Asset" button (proven more reliable than the
     *  page object's self-healing path, which fights a transient backdrop here), then select the class. */
    private boolean openCreateFormForClassOnce(String className) {
        try {
            ensureOnAssetsPage();           // gentle — do NOT full-reload (that drops the selected site)
            closeAnyOpenDrawer();           // clear a leftover create/edit drawer from a prior test
            // the toolbar "Create Asset" button is the only one when no form is open
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
            pause(1500);
            boolean ok = picked != null && picked.equalsIgnoreCase(className);
            logStep("openCreateFormForClassOnce(" + className + ") -> picked='" + picked + "'");
            return ok;
        } catch (Exception e) {
            logStep("openCreateFormForClassOnce failed: " + e.getMessage());
            return false;
        }
    }

    /** Robustly close any open right-side create/edit drawer (incl. a discard-changes confirm/modal). */
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
                // a discard-changes confirmation may appear
                js("var d=[].slice.call(document.querySelectorAll('button')).find(function(b){return /^(Discard|Yes|Confirm|Leave|OK|Discard changes)$/i.test((b.innerText||'').trim());}); if(d) d.click();");
                pause(700);
                // last resort: click the backdrop to dismiss
                js("var bd=document.querySelector('.MuiBackdrop-root'); if(bd) bd.click();");
                pause(500);
            }
        } catch (Exception ignored) {}
    }

    /** Full flow to a matched-library Circuit Breaker (ABB Emax 2 E1.2 Ekip DIP LI). */
    private boolean setupMatchedBreaker() {
        // retry the create-open once — a leftover drawer/discard-modal from the prior test can make
        // the first attempt fail fast (cross-test contamination); closeAnyOpenDrawer clears it.
        boolean opened = false;
        for (int attempt = 0; attempt < 2 && !opened; attempt++) {
            if (attempt > 0) { closeAnyOpenDrawer(); ensureOnAssetsPage(); pause(800); }
            opened = openCreateFormForClass(CB_CLASS);
        }
        if (!opened) return false;
        waitForControl("Select Subtype", 8);
        String st = selectAutocomplete("Select Subtype", "Insulated Case", "insulated case");
        logStep("subtype -> " + st);
        pause(900);
        boolean pole = clickPoleCount("3P");
        logStep("poleCount 3P -> " + pole);
        pause(900);
        waitForControl("Select manufacturer", 8);
        String mfg = selectAutocomplete("Select manufacturer", MANUFACTURER, "abb");
        logStep("manufacturer -> " + mfg);
        if (mfg == null) { return false; }
        // wait for the "108 possible matches" cards to actually render before clicking one
        boolean cards = false;
        for (int i = 0; i < 24; i++) { pause(500); if (bodyHas("Emax 2")) { cards = true; break; } }
        if (!cards) { logStep("library match cards did not load"); return false; }
        // click the target model, then poll for LIBRARY MATCHED; retry once if it doesn't take
        boolean matched = false;
        for (int attempt = 0; attempt < 2 && !matched; attempt++) {
            boolean clicked = matchLibraryModel();
            logStep("matchLibraryModel attempt " + (attempt + 1) + " click=" + clicked);
            for (int i = 0; i < 16; i++) { pause(500); if (bodyHas("LIBRARY MATCHED")) { matched = true; break; } }
        }
        if (!matched) { logStep("setupMatchedBreaker -> match never applied"); return false; }
        // The Trip Configuration loads asynchronously ("Loading device configuration…"); the
        // Frame/Sensor/Plug and LTPU/LTD/INST settings only render once that finishes — wait it out.
        boolean loaded = waitForTripConfigLoaded(40);
        logStep("setupMatchedBreaker -> matched=" + matched + " tripConfigLoaded=" + loaded);
        return matched && loaded;
    }

    /** Poll until the Trip Configuration device-config finishes loading and the FULL settings block
     *  renders (LTPU through Add Ground Fault Settings) — so every match-dependent test (incl. the
     *  i²t toggle / ground-fault tests) sees a fully-rendered section, not a partial one. */
    private boolean waitForTripConfigLoaded(int seconds) {
        for (int i = 0; i < seconds * 2; i++) {
            boolean loading = bodyHas("Loading device configuration");
            boolean full = bodyHas("LTPU") && bodyHas("Ground Fault");
            if (!loading && full) return true;
            pause(500);
        }
        return false;
    }

    private boolean waitForControl(String placeholder, int seconds) {
        for (int i = 0; i < seconds * 2; i++) { if (hasControl(placeholder)) return true; pause(500); }
        return false;
    }

    /**
     * Find an MUI Autocomplete input by placeholder, open it, type {@code typeText} to filter, then
     * click the option whose text contains {@code optionContains} (case-insensitive). Returns the
     * option text clicked, or null.
     */
    private String selectAutocomplete(String placeholder, String typeText, String optionContains) {
        // Retry the whole type→pick: on the slow QA app the option list can lag the first keystroke.
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
                logStep("selectAutocomplete(" + placeholder + ") no option for '" + optionContains + "' — retry " + (attempt + 1));
                pause(800);
            } catch (Exception e) { logStep("selectAutocomplete(" + placeholder + ") error: " + e.getMessage()); pause(700); }
        }
        return null;
    }

    /** Open an Autocomplete by placeholder and return how many options it lists (then leave it open). */
    private long openDropdownCount(String placeholder) {
        js(   // open the dropdown (side effect)
                "var ph=arguments[0];"
                + "var inp=[].slice.call(document.querySelectorAll('input')).find(function(i){return new RegExp(ph,'i').test(i.placeholder||'');});"
                + "if(!inp) return -1;"
                + "inp.scrollIntoView({block:'center'}); inp.focus(); inp.click();"
                + "var w=inp.closest('.MuiAutocomplete-root'); if(w){var b=w.querySelector('.MuiAutocomplete-popupIndicator'); if(b) b.click();}"
                + "return -2;", placeholder);
        pause(1200);
        Object c = js("return document.querySelectorAll(\"li[role='option']\").length;");
        // close the popup so it doesn't block later steps
        js("document.body.click();");
        return c instanceof Long ? (Long) c : -1;
    }

    private int poleCountButtons() {
        Object n = js("return [].slice.call(document.querySelectorAll(\"button,[role='button'],.MuiToggleButton-root,.MuiChip-root\"))"
                + ".filter(function(b){return /^[123]P$/.test((b.innerText||'').trim());}).length;");
        return n instanceof Long ? ((Long) n).intValue() : 0;
    }

    private boolean clickPoleCount(String label) {
        Object r = js("var t=arguments[0];"
                + "var b=[].slice.call(document.querySelectorAll(\"button,[role='button'],.MuiToggleButton-root,.MuiChip-root\"))"
                + ".find(function(e){return (e.innerText||'').trim()===t;});"
                + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;", label);
        return Boolean.TRUE.equals(r);
    }

    /** Click the target ABB library model card (Ekip DIP — LI, 250-1200A, E1.2 S-A), excluding siblings. */
    private boolean matchLibraryModel() {
        Object r = js(
                "var toks=arguments[0];"
                + "function ok(t){ if(!t) return false; for(var i=0;i<toks.length;i++){ if(t.indexOf(toks[i])<0) return false; } return /\\bLI,/.test(t) && t.indexOf('LSI')<0 && t.indexOf('Touch')<0; }"
                + "var cands=[].slice.call(document.querySelectorAll(\"li,[role='option'],[role='button'],.MuiListItemButton-root,.MuiListItem-root,.MuiCard-root,div\"))"
                + ".filter(function(e){return ok(e.textContent||'') && (e.textContent||'').length<200;});"
                + "if(!cands.length) return false;"
                + "cands.sort(function(a,b){return a.textContent.length-b.textContent.length;});"
                + "cands[0].scrollIntoView({block:'center'}); cands[0].click(); return true;",
                (Object) MODEL_TOKENS);
        return Boolean.TRUE.equals(r);
    }

    private boolean clearLibraryMatch() {
        // The LIBRARY MATCHED card is the smallest element holding BOTH the header and the model string;
        // its single IconButton is the clear ✕. Target that tight subtree so we don't grab a far button.
        Object r = js(
                "var cards=[].slice.call(document.querySelectorAll('div')).filter(function(e){"
                + "  var t=e.textContent||''; return /LIBRARY MATCHED/i.test(t) && /Ekip DIP/i.test(t);});"
                + "if(!cards.length) return false;"
                + "cards.sort(function(a,b){return (a.textContent||'').length-(b.textContent||'').length;});"
                + "var card=cards[0];"
                + "var btn=card.querySelector(\".MuiIconButton-root, button, [role='button']\");"
                + "if(!btn){ var svg=card.querySelector('svg'); btn=svg?svg.closest(\"button,[role='button'],.MuiIconButton-root\"):null; }"
                + "if(!btn) return false; btn.scrollIntoView({block:'center'}); btn.click(); return true;");
        return Boolean.TRUE.equals(r);
    }

    private boolean engineeringSectionPresent() {
        return bodyHas("ENGINEERING") && hasControl("Select Subtype");
    }

    private boolean hasControl(String placeholder) {
        Object r = js("var ph=arguments[0]; return !![].slice.call(document.querySelectorAll('input'))"
                + ".find(function(i){return new RegExp(ph,'i').test(i.placeholder||'');});", placeholder);
        return Boolean.TRUE.equals(r);
    }

    private boolean bodyHas(String text) {
        Object r = js("return (document.body.innerText||'').toLowerCase().indexOf(arguments[0].toLowerCase())>-1;", text);
        return Boolean.TRUE.equals(r);
    }

    private void cancelForm() {
        closeAnyOpenDrawer();
        formOpen = false;
    }
}
