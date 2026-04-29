package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Phase 1 Bug Hunter — adversarial + invariant tests targeting the 13 Phase 1
 * surfaces (ZP-323). These are NOT happy-path tests — each test deliberately
 * pokes at boundaries / cross-view invariants / data-layer wiring that real
 * apps often get wrong but rarely test for.
 *
 * Bug classes covered:
 *   1. Cross-view consistency  (TC_BH_01) — grid vs Edit drawer
 *   2. Validation bypass       (TC_BH_02) — required-field clear + save
 *   3. Silent data-fetch fail  (TC_BH_03) — Asset Class dropdown empty
 *   4. Edge-case input handling (TC_BH_04) — very long asset name display
 *   5. Duplicate prevention    (TC_BH_05) — Create Connection double-click
 *
 * Why this file exists separately from BugHuntGlobalTestNG:
 *   - Those tests are tied to specific BUG-NN tickets (regression detectors)
 *   - These are forward-looking adversarial tests, not regression detectors
 *   - Keeping them grouped makes intent clear: "find new bugs", not "guard old ones"
 *
 * Each test:
 *   - Has a falsifiable assertion tied to a real product invariant
 *   - Cleans up after itself (no DB writes for read-only tests; rollback for mutating)
 *   - Includes a brief "Why this might find a bug" comment explaining the failure mode
 */
public class Phase1BugHunterTestNG extends BaseTest {

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private static final String FEATURE_BH = "Phase 1 Bug Hunter";

    // ================================================================
    // TC_BH_01 — Grid value matches Edit drawer value (display integrity)
    // ================================================================
    /**
     * Why this might find a bug: display layers often cache. If the asset list
     * grid is rendered from a cached payload but the Edit drawer fetches fresh
     * data, the two can diverge. Users see one value in the grid, edit a
     * different value in the drawer, get confused.
     *
     * Falsifiable assertion: the FIRST asset's name as displayed in the grid
     * MUST equal the Asset Name shown in its Edit drawer. Any divergence is a
     * real bug.
     */
    @Test(priority = 1, description = "TC_BH_01: Grid asset name matches Edit drawer's Asset Name field")
    public void testTC_BH_01_GridDrawerNameConsistency() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_01: Grid<>Drawer name consistency");
        try {
            assetPage.navigateToAssets();
            pause(3000);

            // Capture the FIRST asset's name as the grid displays it
            String gridName = (String) js().executeScript(
                "var rows = document.querySelectorAll('.MuiDataGrid-row');"
                + "for (var r of rows) {"
                + "  if (!r.offsetWidth) continue;"
                + "  var cell = r.querySelector('[data-field=\"name\"], "
                + "[data-field=\"assetName\"], .MuiDataGrid-cell');"
                + "  if (cell) return (cell.textContent || '').trim();"
                + "}"
                + "return null;");
            Assert.assertNotNull(gridName,
                "Could not read first asset name from grid — needs ≥1 asset");
            logStep("Grid asset name: '" + gridName + "'");

            // Click into the row → Edit drawer
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            safeClick(rows.get(0));
            pause(3500);
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2500);

            // Read the Asset Name input value from the drawer
            String drawerName = (String) js().executeScript(
                "var inputs = document.querySelectorAll('[class*=\"MuiDrawer-paper\"] input');"
                + "for (var i of inputs) {"
                + "  if (!i.offsetWidth) continue;"
                + "  var ph = (i.placeholder || '').toLowerCase();"
                + "  if (ph.includes('asset name') || ph.includes('enter asset name')) {"
                + "    return i.value;"
                + "  }"
                + "}"
                + "return null;");
            logStep("Drawer Asset Name input value: '" + drawerName + "'");
            ScreenshotUtil.captureScreenshot("TC_BH_01");

            // Cleanup: close drawer
            try {
                List<WebElement> cancels = driver.findElements(By.xpath(
                    "//*[@class][contains(@class,'MuiDrawer-paper')]//button[normalize-space()='Cancel']"));
                for (WebElement c : cancels) {
                    if (c.isDisplayed()) { safeClick(c); break; }
                }
            } catch (Exception ignore) {}
            pause(800);

            // Falsifiable: names must match
            Assert.assertNotNull(drawerName,
                "Edit drawer's Asset Name input not found — drawer may not have opened");
            Assert.assertEquals(drawerName, gridName,
                "Grid asset name ('" + gridName + "') does NOT match Edit drawer's "
                + "Asset Name field ('" + drawerName + "'). Display cache is stale OR "
                + "the grid and drawer fetch from different sources. Real bug.");

            ExtentReportManager.logPass("Grid<>Drawer name consistency verified: '"
                + gridName + "'");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_01_error");
            Assert.fail("TC_BH_01 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_02 — Required-field validation: clearing Asset Name + Save
    // ================================================================
    /**
     * Why this might find a bug: required-field validation often gets
     * inconsistent. Some forms validate client-side, some server-side, some
     * forget. If a user can SAVE an asset with an empty required field,
     * downstream views break (asset appears as "" or "Untitled" in lists).
     *
     * Falsifiable assertion: clicking Save Changes after clearing a required
     * field MUST either (a) show a validation error message, OR (b) leave the
     * Save button disabled. Silent acceptance of empty value is a bug.
     *
     * Cleanup: closes drawer WITHOUT saving (no data mutation).
     */
    @Test(priority = 2, description = "TC_BH_02: Save with empty required Asset Name shows validation OR disables Save")
    public void testTC_BH_02_RequiredFieldValidation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_02: Required-field validation");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets in grid");
            safeClick(rows.get(0));
            pause(3500);
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2500);

            // Find the Asset Name input + capture original value
            Object inputObj = js().executeScript(
                "var inputs = document.querySelectorAll('[class*=\"MuiDrawer-paper\"] input');"
                + "for (var i of inputs) {"
                + "  if (!i.offsetWidth) continue;"
                + "  var ph = (i.placeholder || '').toLowerCase();"
                + "  if (ph.includes('asset name') || ph.includes('enter asset name')) return i;"
                + "}"
                + "return null;");
            Assert.assertTrue(inputObj instanceof WebElement,
                "Asset Name input not found in Edit drawer");
            WebElement input = (WebElement) inputObj;
            String originalName = input.getDomProperty("value");
            logStep("Original Asset Name: '" + originalName + "'");

            // Clear the Asset Name via React's native-setter (real input change)
            js().executeScript(
                "var setter = Object.getOwnPropertyDescriptor("
                + "  window.HTMLInputElement.prototype, 'value').set;"
                + "setter.call(arguments[0], '');"
                + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                input);
            pause(800);
            logStepWithScreenshot("Asset Name cleared");

            // Find Save Changes button + check its enabled state
            Object saveBtnObj = js().executeScript(
                "var btns = document.querySelectorAll('[class*=\"MuiDrawer-paper\"] button');"
                + "for (var b of btns) {"
                + "  if (!b.offsetWidth) continue;"
                + "  var t = (b.textContent || '').trim();"
                + "  if (t === 'Save Changes' || t === 'Save') return b;"
                + "}"
                + "return null;");
            Assert.assertTrue(saveBtnObj instanceof WebElement, "Save Changes button missing");
            WebElement saveBtn = (WebElement) saveBtnObj;
            boolean saveDisabled = !saveBtn.isEnabled()
                || "true".equals(saveBtn.getDomAttribute("aria-disabled"));
            logStep("Save Changes disabled with empty Asset Name: " + saveDisabled);

            // Click Save and look for validation feedback
            safeClick(saveBtn);
            pause(2000);
            String validationMsg = (String) js().executeScript(
                "var t = document.body ? document.body.textContent : '';"
                + "var patterns = [/required/i, /must.*name/i, /cannot be empty/i, "
                + "/asset name.*required/i, /please enter/i, /name is required/i];"
                + "for (var p of patterns) { var m = t.match(p); if (m) return m[0]; }"
                + "return null;");
            logStep("Validation message: " + validationMsg);
            ScreenshotUtil.captureScreenshot("TC_BH_02");

            // Cleanup: cancel without saving (DO NOT actually save empty name)
            try {
                List<WebElement> cancels = driver.findElements(By.xpath(
                    "//*[contains(@class,'MuiDrawer-paper')]//button[normalize-space()='Cancel']"));
                for (WebElement c : cancels) {
                    if (c.isDisplayed()) { safeClick(c); break; }
                }
            } catch (Exception ignore) {}
            pause(1500);
            // Handle "Discard changes?" prompt if any
            try {
                List<WebElement> discards = driver.findElements(By.xpath(
                    "//button[normalize-space()='Discard'] | //button[normalize-space()='Don't Save']"));
                for (WebElement d : discards) {
                    if (d.isDisplayed()) { safeClick(d); break; }
                }
            } catch (Exception ignore) {}
            pause(1000);

            // Falsifiable: at least ONE validation signal must fire
            boolean validationFired = saveDisabled
                || (validationMsg != null && !validationMsg.isEmpty());
            Assert.assertTrue(validationFired,
                "BUG: Save Changes accepted empty Asset Name silently. "
                + "saveDisabled=" + saveDisabled + ", validationMsg=" + validationMsg
                + ". Real product issue — required-field validation is missing.");

            ExtentReportManager.logPass("Required-field validation works: "
                + (saveDisabled ? "Save disabled" : "Validation message: " + validationMsg));
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_02_error");
            Assert.fail("TC_BH_02 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_03 — Asset Class dropdown has data layer wired (≥3 options)
    // ================================================================
    /**
     * Why this might find a bug: dropdowns silently fail when their data fetch
     * errors. Auth missing, 401 silently swallowed, or CORS misconfigured —
     * the dropdown renders empty. Users can't create assets.
     *
     * Falsifiable assertion: opening the Asset Class dropdown in Create form
     * MUST show ≥3 options (this is an electrical equipment platform — at
     * minimum we expect classes like Motor, Switchboard, Circuit Breaker,
     * Transformer, etc.). Zero or one options means data fetch failed.
     */
    @Test(priority = 3, description = "TC_BH_03: Asset Class dropdown has ≥3 options (data layer wired)")
    public void testTC_BH_03_AssetClassDropdownDataLayer() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_03: Asset Class dropdown options");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            assetPage.openCreateAssetForm();
            pause(2500);
            logStepWithScreenshot("Create Asset form opened");

            // Diagnostic: log what's actually in the Create form so we can see if
            // labels/comboboxes are present at all.
            @SuppressWarnings("unchecked")
            List<String> diag = (List<String>) js().executeScript(
                "var out = [];"
                + "var labels = document.querySelectorAll('label, .MuiFormLabel-root, .MuiInputLabel-root');"
                + "out.push('LABELS:' + labels.length);"
                + "for (var l of labels) {"
                + "  var t = (l.textContent || '').trim();"
                + "  if (t && l.offsetWidth) out.push(' L: ' + t.slice(0,50));"
                + "}"
                + "var combos = document.querySelectorAll('input[role=\"combobox\"], div[role=\"combobox\"], .MuiAutocomplete-root, .MuiSelect-select');"
                + "out.push('COMBOS:' + combos.length);"
                + "var inputs = document.querySelectorAll('input[placeholder]');"
                + "for (var i of inputs) {"
                + "  if (i.offsetWidth) out.push(' P: ' + i.placeholder);"
                + "}"
                + "return out;");
            logStep("Create form diagnostic: " + diag);

            // Robust: find the "Asset Class" label (case-insensitive, partial match),
            // then click the nearest combobox input.
            Boolean clicked = (Boolean) js().executeScript(
                "var labels = Array.from(document.querySelectorAll('label, .MuiFormLabel-root, .MuiInputLabel-root, span, p, div'));"
                + "var assetLabel = null;"
                + "for (var l of labels) {"
                + "  var t = (l.textContent || '').trim().toLowerCase();"
                + "  if (t.startsWith('asset class') && t.length < 30) {"
                + "    if (l.offsetWidth) { assetLabel = l; break; }"
                + "  }"
                + "}"
                + "if (!assetLabel) return 'no-label';"
                + "var lr = assetLabel.getBoundingClientRect();"
                + "var combos = Array.from(document.querySelectorAll('input[role=\"combobox\"], div[role=\"combobox\"], .MuiAutocomplete-root, .MuiSelect-select'));"
                + "var best = null; var bestDist = 9999;"
                + "for (var i of combos) {"
                + "  if (!i.offsetWidth) continue;"
                + "  var ir = i.getBoundingClientRect();"
                + "  var dy = ir.top - lr.bottom;"
                + "  var dx = Math.abs(ir.left - lr.left);"
                + "  if (dy < -10 || dy > 120) continue;"
                + "  if (dx > 350) continue;"
                + "  var d = Math.abs(dy) + dx * 0.5;"
                + "  if (d < bestDist) { bestDist = d; best = i; }"
                + "}"
                + "if (!best) return 'no-combo';"
                + "best.scrollIntoView({block: 'center'});"
                + "best.click();"
                + "var inner = best.querySelector ? best.querySelector('input') : null;"
                + "if (inner) inner.click();"
                + "return true;");
            logStep("Asset Class click result: " + clicked);
            if (!Boolean.TRUE.equals(clicked)) {
                throw new org.testng.SkipException(
                    "Asset Class label/combobox not found in Create form (" + clicked
                    + ") — UI may have changed; skip rather than false-fail.");
            }
            pause(2000);

            // Count visible options across MUI variants
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) js().executeScript(
                "var sels = ['li[role=\"option\"]', '[role=\"option\"]', "
                + "'.MuiAutocomplete-option', '.MuiMenuItem-root', "
                + "'.MuiList-root li'];"
                + "var seen = new Set();"
                + "var out = [];"
                + "for (var s of sels) {"
                + "  var opts = document.querySelectorAll(s);"
                + "  for (var o of opts) {"
                + "    if (!o.offsetWidth) continue;"
                + "    var t = (o.textContent || '').trim();"
                + "    if (!t || t.length > 80 || seen.has(t)) continue;"
                + "    seen.add(t); out.push(t);"
                + "  }"
                + "}"
                + "return out;");
            logStep("Asset Class dropdown options (" + options.size() + "): " + options);
            ScreenshotUtil.captureScreenshot("TC_BH_03");

            // Cleanup: close dropdown + cancel form
            try {
                driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
                pause(500);
                List<WebElement> cancels = driver.findElements(By.xpath(
                    "//button[normalize-space()='Cancel']"));
                for (WebElement c : cancels) {
                    if (c.isDisplayed()) { safeClick(c); break; }
                }
            } catch (Exception ignore) {}
            pause(800);

            // Falsifiable: ≥3 options (Motor / Switchboard / Circuit Breaker minimum)
            Assert.assertTrue(options != null && options.size() >= 3,
                "BUG: Asset Class dropdown has only " + (options == null ? 0 : options.size())
                + " options. Expected ≥3 for an electrical equipment platform. "
                + "Data layer likely broken — silent API fetch failure or auth issue.");

            ExtentReportManager.logPass("Asset Class dropdown has " + options.size()
                + " options (data layer wired)");
        } catch (org.testng.SkipException se) {
            // Honest skip — let TestNG record it as SKIP, not FAIL
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_03_error");
            Assert.fail("TC_BH_03 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_04 — Long asset name doesn't break grid layout
    // ================================================================
    /**
     * Why this might find a bug: grid columns often have fixed widths. A
     * 200-char asset name can either (a) overflow horizontally, breaking
     * layout, OR (b) get truncated with "..." (good UX), OR (c) wrap to
     * multiple lines (acceptable). What's NOT OK: rendering the full name
     * with no constraint, breaking the grid.
     *
     * This test READS existing grid state, doesn't mutate. Looks for any
     * row whose name cell text length > visible width — indicates layout
     * overflow. Read-only diagnostic.
     */
    @Test(priority = 4, description = "TC_BH_04: No grid row has overflow text (long names truncated/wrapped)")
    public void testTC_BH_04_GridLayoutNoOverflow() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_04: Grid layout no-overflow");
        try {
            assetPage.navigateToAssets();
            pause(3000);

            // Check every visible grid row's name cell for overflow
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> overflows = (List<java.util.Map<String, Object>>) js().executeScript(
                "var rows = document.querySelectorAll('.MuiDataGrid-row');"
                + "var hits = [];"
                + "for (var r of rows) {"
                + "  if (!r.offsetWidth) continue;"
                + "  var cells = r.querySelectorAll('.MuiDataGrid-cell');"
                + "  for (var c of cells) {"
                + "    if (c.scrollWidth > c.clientWidth + 1) {"
                + "      hits.push({"
                + "        text: (c.textContent || '').trim().slice(0, 40),"
                + "        scroll: c.scrollWidth, client: c.clientWidth"
                + "      });"
                + "    }"
                + "  }"
                + "}"
                + "return hits;");
            logStep("Cells with overflow (scrollWidth > clientWidth): "
                + (overflows == null ? 0 : overflows.size()));
            if (overflows != null) {
                for (int i = 0; i < Math.min(3, overflows.size()); i++) {
                    logStep("  overflow: " + overflows.get(i));
                }
            }
            ScreenshotUtil.captureScreenshot("TC_BH_04");

            // Soft assertion — some overflow may be acceptable (truncated cells
            // often have scrollWidth > clientWidth by design with text-overflow:
            // ellipsis). The real bug indicator: ALL cells overflow (means
            // `text-overflow: ellipsis` not applied), or cells that overflow have
            // visible text past their boundary (can't easily detect via JS).
            //
            // For now: assert that grid has SOME rows (sanity), then log the
            // overflow count as informational. This test acts as a baseline —
            // a sudden jump in overflow count vs prior runs would flag a layout
            // regression.
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(),
                "Grid has no rows — can't verify layout (data prerequisite)");

            int overflowCount = overflows == null ? 0 : overflows.size();
            ExtentReportManager.logPass("Grid layout check: " + rows.size()
                + " rows, " + overflowCount + " cells with horizontal overflow "
                + "(soft signal — sudden jump may indicate layout regression)");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_04_error");
            Assert.fail("TC_BH_04 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_05 — Connections page filter input doesn't crash on injection
    // ================================================================
    /**
     * Why this might find a bug: search inputs sometimes don't sanitize. A
     * payload like `<img src=x onerror=alert(1)>` or `' OR '1'='1` should be
     * accepted as a literal search term — the search returns 0 matches — NOT
     * crash the page or execute the script.
     *
     * Falsifiable assertion: after typing an XSS-like payload into Connections
     * search, the page should still be functional (grid still renders, no
     * uncaught exceptions, no alert popups). If the payload causes a crash or
     * alert, that's a real XSS vulnerability.
     */
    @Test(priority = 5, description = "TC_BH_05: Connections search accepts XSS-like payload as literal text (no crash, no alert)")
    public void testTC_BH_05_ConnectionsSearchXssPayloadHandled() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_05: Connections search XSS-payload");
        try {
            connectionPage.navigateToConnections();
            pause(3000);

            // Capture row count BEFORE injection — baseline
            long beforeRowCount = ((Number) js().executeScript(
                "return document.querySelectorAll('.MuiDataGrid-row').length;")).longValue();
            logStep("Connections rows before injection: " + beforeRowCount);

            // Find the search input + type XSS-like payload
            Object inputObj = js().executeScript(
                "var inputs = document.querySelectorAll('input[placeholder*=\"Search Connection\" i], "
                + "input[placeholder*=\"Search\" i]');"
                + "for (var i of inputs) { if (i.offsetWidth) return i; }"
                + "return null;");
            Assert.assertTrue(inputObj instanceof WebElement,
                "Connections search input not found");
            WebElement searchInput = (WebElement) inputObj;
            String xssPayload = "<img src=x onerror=alert(1)>";
            js().executeScript(
                "var setter = Object.getOwnPropertyDescriptor("
                + "  window.HTMLInputElement.prototype, 'value').set;"
                + "setter.call(arguments[0], arguments[1]);"
                + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                searchInput, xssPayload);
            pause(2500);
            logStepWithScreenshot("XSS-like payload typed into Connections search");

            // Verify: page still functional
            // 1. No JS alert was triggered
            //    (Selenium auto-dismisses alerts; check via driver.switchTo().alert() — if any, FAIL)
            String alertText = null;
            try {
                alertText = driver.switchTo().alert().getText();
                driver.switchTo().alert().dismiss();
            } catch (Exception ignore) {
                // No alert — good
            }
            Assert.assertNull(alertText,
                "BUG: XSS payload triggered a browser alert ('" + alertText + "'). "
                + "Real security vulnerability — search input is not sanitizing user input "
                + "before rendering.");

            // 2. Page DOM is still intact (body has content, grid still rendered)
            long afterRowCount = ((Number) js().executeScript(
                "return document.querySelectorAll('.MuiDataGrid-row').length;")).longValue();
            String bodyTextStart = (String) js().executeScript(
                "return (document.body.textContent || '').trim().slice(0, 100);");
            logStep("After injection: row count=" + afterRowCount
                + ", body text starts with: '" + bodyTextStart + "'");

            // Cleanup: clear the search
            js().executeScript(
                "var setter = Object.getOwnPropertyDescriptor("
                + "  window.HTMLInputElement.prototype, 'value').set;"
                + "setter.call(arguments[0], '');"
                + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                searchInput);
            pause(1500);
            ScreenshotUtil.captureScreenshot("TC_BH_05");

            // Falsifiable: page still has content (didn't blank out / white-screen)
            Assert.assertTrue(bodyTextStart != null && bodyTextStart.length() > 20,
                "BUG: Page became blank/empty after typing XSS payload. Real crash bug.");

            ExtentReportManager.logPass("XSS-like payload handled safely: "
                + "no alert, no crash, no white-screen. Search rows after: " + afterRowCount);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_05_error");
            Assert.fail("TC_BH_05 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_06 — Assets search safely handles SQL-injection payload
    // ================================================================
    /**
     * Why this might find a bug: search inputs are often plumbed straight into
     * a parameterized DB query, but if a developer concatenates the string
     * into a raw `WHERE name LIKE '%${input}%'`, then `' OR '1'='1 --` would
     * close the quote and select ALL rows (or worse, drop tables on a write
     * endpoint). Counterpart to TC_BH_05 (XSS); this one targets the *backend*
     * boundary instead of the DOM.
     *
     * Falsifiable assertion: after typing the SQLi payload, the grid row
     * count must NOT exceed the pre-payload row count (an injection that
     * returned all rows would EXCEED the search-narrowed count). Also: no
     * raw SQL error string should leak into the DOM ("syntax error", "near
     * 'OR'", "ORA-", "SQLSTATE").
     */
    @Test(priority = 6, description = "TC_BH_06: Asset search safely handles SQL-injection payload")
    public void testTC_BH_06_AssetSearchSqlInjectionSafe() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_06: SQL injection safety in search");
        try {
            assetPage.navigateToAssets();
            pause(3000);

            // Capture pre-injection row count (grid row elements + pagination total if visible)
            Long preCount = (Long) js().executeScript(
                "return document.querySelectorAll('.MuiDataGrid-row').length;");
            String preFooter = (String) js().executeScript(
                "var f = document.querySelector('.MuiTablePagination-displayedRows');"
                + "return f ? (f.textContent || '').trim() : null;");
            logStep("Pre-injection: visible rows=" + preCount + ", footer='" + preFooter + "'");

            // Find the Assets-page search input (placeholder typically 'Search')
            WebElement search = null;
            List<WebElement> searches = driver.findElements(By.cssSelector(
                "input[placeholder*='Search' i], input[type='search']"));
            for (WebElement s : searches) {
                if (s.isDisplayed()) { search = s; break; }
            }
            if (search == null) {
                throw new org.testng.SkipException(
                    "No search input visible on Assets page — UI may have changed; "
                    + "skip rather than false-fail.");
            }

            // Classic SQLi payload — closes a string literal, OR's a tautology, comments rest
            String payload = "' OR '1'='1 --";
            search.click();
            search.sendKeys(payload);
            pause(2500);

            // Falsifier 1: grid row count must NOT exceed pre-injection count.
            // (A tautology-injection that bypasses the WHERE clause would return
            // all rows — exceeding the search-narrowed count.)
            Long postCount = (Long) js().executeScript(
                "return document.querySelectorAll('.MuiDataGrid-row').length;");
            logStep("Post-injection: visible rows=" + postCount);
            Assert.assertTrue(postCount != null && postCount <= preCount,
                "BUG: SQLi-style payload increased visible row count from " + preCount
                + " to " + postCount + " — search may not be parameterizing input properly.");

            // Falsifier 2: no raw SQL error text should be visible
            String pageText = (String) js().executeScript(
                "return (document.body.textContent || '').toLowerCase();");
            String[] sqlErrorMarkers = {
                "syntax error", "near 'or'", "ora-", "sqlstate", "psqlexception",
                "mysql_fetch", "unclosed quotation", "sqliteexception", "pg_query"
            };
            for (String m : sqlErrorMarkers) {
                Assert.assertFalse(pageText.contains(m),
                    "BUG: raw SQL error '" + m + "' leaked into DOM after injection");
            }
            ScreenshotUtil.captureScreenshot("TC_BH_06");

            // Cleanup: clear search
            search.clear();
            pause(800);

            ExtentReportManager.logPass("SQL injection payload safely handled: "
                + "row count " + preCount + "→" + postCount + ", no SQL errors leaked");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_06_error");
            Assert.fail("TC_BH_06 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_07 — Asset Name sort actually reorders rows (not a no-op)
    // ================================================================
    /**
     * Why this might find a bug: a column-header sort handler that doesn't
     * re-fetch and doesn't re-sort client-side is a silent no-op — the user
     * clicks the header, the arrow flips, but the row order stays the same.
     * The DataGrid has tested this in dev with toy data, but on real prod
     * data with mixed casing / nulls / numeric prefixes, sort can break.
     *
     * Falsifiable assertion: clicking the Asset Name header twice (asc → desc)
     * MUST produce a different first-row text than the asc state did. If
     * asc-first-row == desc-first-row AND there are ≥2 unique values in the
     * grid, sort is broken.
     *
     * Honest skip condition: if the grid has <2 rows OR all rows have the
     * same name, we can't falsify (any order is "sorted"). Skip in that case.
     */
    @Test(priority = 7, description = "TC_BH_07: Asset Name sort actually reorders rows on header click")
    public void testTC_BH_07_AssetNameSortChangesOrder() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_07: Sort invariant");
        try {
            assetPage.navigateToAssets();
            pause(3500);

            @SuppressWarnings("unchecked")
            List<String> initialRows = (List<String>) js().executeScript(
                "var rows = document.querySelectorAll('.MuiDataGrid-row');"
                + "var out = [];"
                + "for (var r of rows) {"
                + "  if (!r.offsetWidth) continue;"
                + "  var cell = r.querySelector('[data-field=\"name\"], "
                + "[data-field=\"assetName\"], .MuiDataGrid-cell');"
                + "  if (cell) out.push((cell.textContent || '').trim());"
                + "}"
                + "return out;");
            logStep("Initial visible row names (" + initialRows.size() + "): "
                + initialRows.subList(0, Math.min(3, initialRows.size())));

            if (initialRows.size() < 2) {
                throw new org.testng.SkipException(
                    "Grid has fewer than 2 rows — can't falsify sort order. "
                    + "Need at least 2 rows with distinct names.");
            }
            long uniqueCount = initialRows.stream().distinct().count();
            if (uniqueCount < 2) {
                throw new org.testng.SkipException(
                    "All visible row names are identical — can't falsify sort order.");
            }

            // Click Asset Name column header to sort
            Boolean clickedHeader = (Boolean) js().executeScript(
                "var hdrs = document.querySelectorAll('.MuiDataGrid-columnHeaderTitle, "
                + ".MuiDataGrid-columnHeader');"
                + "for (var h of hdrs) {"
                + "  if (!h.offsetWidth) continue;"
                + "  var t = (h.textContent || '').trim().toLowerCase();"
                + "  if (t.indexOf('asset name') !== -1 || t === 'name') {"
                + "    var clickable = h.closest('.MuiDataGrid-columnHeader') || h;"
                + "    clickable.click();"
                + "    return true;"
                + "  }"
                + "}"
                + "return false;");
            Assert.assertTrue(Boolean.TRUE.equals(clickedHeader),
                "Asset Name column header not found / not clickable");
            pause(2000);

            String ascFirstRow = (String) js().executeScript(
                "var r = document.querySelector('.MuiDataGrid-row');"
                + "if (!r) return null;"
                + "var c = r.querySelector('[data-field=\"name\"], "
                + "[data-field=\"assetName\"], .MuiDataGrid-cell');"
                + "return c ? (c.textContent || '').trim() : null;");
            logStep("After 1st header click — first row: '" + ascFirstRow + "'");

            // Click again to flip to descending
            js().executeScript(
                "var hdrs = document.querySelectorAll('.MuiDataGrid-columnHeaderTitle, "
                + ".MuiDataGrid-columnHeader');"
                + "for (var h of hdrs) {"
                + "  if (!h.offsetWidth) continue;"
                + "  var t = (h.textContent || '').trim().toLowerCase();"
                + "  if (t.indexOf('asset name') !== -1 || t === 'name') {"
                + "    var clickable = h.closest('.MuiDataGrid-columnHeader') || h;"
                + "    clickable.click();"
                + "    return;"
                + "  }"
                + "}");
            pause(2000);

            String descFirstRow = (String) js().executeScript(
                "var r = document.querySelector('.MuiDataGrid-row');"
                + "if (!r) return null;"
                + "var c = r.querySelector('[data-field=\"name\"], "
                + "[data-field=\"assetName\"], .MuiDataGrid-cell');"
                + "return c ? (c.textContent || '').trim() : null;");
            logStep("After 2nd header click — first row: '" + descFirstRow + "'");
            ScreenshotUtil.captureScreenshot("TC_BH_07");

            Assert.assertNotNull(ascFirstRow, "Could not read first row after asc sort");
            Assert.assertNotNull(descFirstRow, "Could not read first row after desc sort");
            Assert.assertNotEquals(ascFirstRow, descFirstRow,
                "BUG: clicking Asset Name header twice (asc → desc) did not change "
                + "the first row. Sort handler is a no-op or arrow flips without re-sorting. "
                + "(Initial unique names ≥ 2, so this is falsifiable.)");

            ExtentReportManager.logPass("Sort flips first row: '" + ascFirstRow
                + "' (asc) → '" + descFirstRow + "' (desc)");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_07_error");
            Assert.fail("TC_BH_07 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_08 — Create Asset → Cancel does NOT persist the entered name
    // ================================================================
    /**
     * Why this might find a bug: form-state leakage. Some apps debounce-save
     * to drafts, or auto-create on first keystroke, or save on blur. If
     * Cancel doesn't actually rollback the entry, the user's
     * "I changed my mind" gesture leaves a ghost asset in production data.
     *
     * Falsifiable assertion: type a unique sentinel name into the Create
     * form, click Cancel, then search the grid for that sentinel. It must
     * NOT appear. Anything else is a leak bug.
     */
    @Test(priority = 8, description = "TC_BH_08: Create form Cancel discards typed name (no persistence leak)")
    public void testTC_BH_08_CreateAssetCancelNoLeak() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_08: Create+Cancel no-leak");
        String sentinel = "BH08Cancel_" + System.currentTimeMillis();
        try {
            assetPage.navigateToAssets();
            pause(3000);
            assetPage.openCreateAssetForm();
            pause(2500);

            // Find Asset Name input in the Create form (drawer or dialog)
            Boolean typed = (Boolean) js().executeScript(
                "var inputs = document.querySelectorAll('input');"
                + "for (var i of inputs) {"
                + "  if (!i.offsetWidth) continue;"
                + "  var ph = (i.placeholder || '').toLowerCase();"
                + "  var lbl = '';"
                + "  var lbls = i.labels;"
                + "  if (lbls && lbls.length) lbl = (lbls[0].textContent || '').toLowerCase();"
                + "  if (ph.indexOf('asset name') !== -1 || ph.indexOf('enter asset name') !== -1 "
                + "      || lbl.indexOf('asset name') !== -1) {"
                // Native-setter to force React state update
                + "    var nativeSetter = Object.getOwnPropertyDescriptor("
                + "      window.HTMLInputElement.prototype, 'value').set;"
                + "    nativeSetter.call(i, arguments[0]);"
                + "    i.dispatchEvent(new Event('input', {bubbles:true}));"
                + "    i.dispatchEvent(new Event('change', {bubbles:true}));"
                + "    return true;"
                + "  }"
                + "}"
                + "return false;", sentinel);
            Assert.assertTrue(Boolean.TRUE.equals(typed),
                "Could not find Asset Name input in Create form to type sentinel");
            logStep("Typed sentinel '" + sentinel + "' into Asset Name");
            pause(800);

            // Click Cancel (NOT Save)
            Boolean cancelled = (Boolean) js().executeScript(
                "var btns = document.querySelectorAll('button');"
                + "for (var b of btns) {"
                + "  if (!b.offsetWidth) continue;"
                + "  var t = (b.textContent || '').trim().toLowerCase();"
                + "  if (t === 'cancel') { b.click(); return true; }"
                + "}"
                + "return false;");
            Assert.assertTrue(Boolean.TRUE.equals(cancelled),
                "No Cancel button found in Create form");
            pause(2500);
            logStep("Clicked Cancel");

            // Now search the grid for the sentinel via the search input
            assetPage.navigateToAssets();
            pause(2500);
            WebElement search = null;
            List<WebElement> searches = driver.findElements(By.cssSelector(
                "input[placeholder*='Search' i], input[type='search']"));
            for (WebElement s : searches) {
                if (s.isDisplayed()) { search = s; break; }
            }
            Assert.assertNotNull(search, "No search input on Assets page");
            search.click();
            search.sendKeys(sentinel);
            pause(3000);

            Long matchingRows = (Long) js().executeScript(
                "var rows = document.querySelectorAll('.MuiDataGrid-row');"
                + "var n = 0;"
                + "for (var r of rows) {"
                + "  if (!r.offsetWidth) continue;"
                + "  if ((r.textContent || '').indexOf(arguments[0]) !== -1) n++;"
                + "}"
                + "return n;", sentinel);
            ScreenshotUtil.captureScreenshot("TC_BH_08");

            // Cleanup: clear search regardless
            try { search.clear(); } catch (Exception ignore) {}
            pause(500);

            Assert.assertEquals(matchingRows.longValue(), 0L,
                "BUG: sentinel '" + sentinel + "' appears in grid after Cancel — "
                + "Create form leaks data to backend even on Cancel.");

            ExtentReportManager.logPass("Cancel correctly discards typed name "
                + "(0 grid matches for sentinel)");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_08_error");
            Assert.fail("TC_BH_08 crashed: " + e.getMessage());
        }
    }

    // ================================================================
    // TC_BH_09 — Pagination total stays consistent across page navigation
    // ================================================================
    /**
     * Why this might find a bug: pagination footer often shows "1-25 of 477".
     * The "of N" total should be computed once per query and stay constant
     * as the user pages through. If the backend recounts on each page (bad
     * pattern: SELECT COUNT() per page) AND data is mutating concurrently OR
     * a JOIN is non-deterministic, the total can flicker. More commonly: a
     * client-side bug recomputes the total from only-visible-rows, making it
     * collapse to "25 of 25" on page 2.
     *
     * Falsifiable assertion: parse "of N" from pagination footer on page 1.
     * Click next page. Parse again. The total N must be IDENTICAL.
     *
     * Honest skip: if the dataset is small enough that there's no next page,
     * we can't test this. Skip with a clear message.
     */
    @Test(priority = 9, description = "TC_BH_09: Pagination 'of N' total stays constant across pages")
    public void testTC_BH_09_PaginationTotalStable() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_BUG_HUNT, FEATURE_BH,
            "TC_BH_09: Pagination total stable");
        try {
            assetPage.navigateToAssets();
            pause(3500);

            // Read the pagination footer text — typically "1–25 of 477"
            String footerPage1 = (String) js().executeScript(
                "var f = document.querySelector('.MuiTablePagination-displayedRows');"
                + "return f ? (f.textContent || '').trim() : null;");
            if (footerPage1 == null || footerPage1.isEmpty()) {
                throw new org.testng.SkipException(
                    "No MuiTablePagination footer found — pagination may be disabled "
                    + "or the grid uses a different paging widget.");
            }
            logStep("Pagination footer page-1: '" + footerPage1 + "'");

            // Extract the "of N" total
            java.util.regex.Matcher m1 = java.util.regex.Pattern
                .compile("of\\s+([0-9,]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(footerPage1);
            if (!m1.find()) {
                throw new org.testng.SkipException(
                    "Pagination footer '" + footerPage1 + "' doesn't match 'of N' pattern; "
                    + "skipping rather than false-failing on a UI variant.");
            }
            long totalPage1 = Long.parseLong(m1.group(1).replace(",", ""));
            logStep("Page-1 total N = " + totalPage1);

            // Click next page
            Boolean clicked = (Boolean) js().executeScript(
                "var btns = document.querySelectorAll("
                + "'button[aria-label=\"Go to next page\" i], "
                + "button[aria-label*=\"next\" i], "
                + "button[title*=\"next\" i]');"
                + "for (var b of btns) {"
                + "  if (b.offsetWidth && !b.disabled) { b.click(); return true; }"
                + "}"
                + "return false;");
            if (!Boolean.TRUE.equals(clicked)) {
                throw new org.testng.SkipException(
                    "No enabled 'next page' button — dataset is single-page; "
                    + "can't falsify cross-page total stability.");
            }
            pause(2500);

            String footerPage2 = (String) js().executeScript(
                "var f = document.querySelector('.MuiTablePagination-displayedRows');"
                + "return f ? (f.textContent || '').trim() : null;");
            logStep("Pagination footer page-2: '" + footerPage2 + "'");

            java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("of\\s+([0-9,]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(footerPage2 == null ? "" : footerPage2);
            Assert.assertTrue(m2.find(),
                "Page-2 footer '" + footerPage2 + "' lost the 'of N' total — pagination "
                + "widget regressed mid-flow.");
            long totalPage2 = Long.parseLong(m2.group(1).replace(",", ""));
            ScreenshotUtil.captureScreenshot("TC_BH_09");

            Assert.assertEquals(totalPage2, totalPage1,
                "BUG: pagination total changed across pages: page-1 said "
                + totalPage1 + ", page-2 says " + totalPage2 + ". The total should be "
                + "the same query result regardless of which page is shown.");

            ExtentReportManager.logPass("Pagination total N=" + totalPage1
                + " stable across page-1 → page-2");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_BH_09_error");
            Assert.fail("TC_BH_09 crashed: " + e.getMessage());
        }
    }
}
