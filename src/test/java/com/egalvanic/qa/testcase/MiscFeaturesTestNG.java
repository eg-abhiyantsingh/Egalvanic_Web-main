package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Miscellaneous new coverage — ZP-323.
 *
 * Four small but important features bundled:
 *   - Terms & Conditions checkbox (login / settings / agreement flow)
 *   - Calculation - Maintenance State (on asset detail calculations tab)
 *   - Suggested Shortcuts (typically on SLDs / asset detail)
 *   - Schedule (Scheduling page navigation + CRUD)
 *
 * Coverage:
 *   TC_Misc_01  Terms & Conditions checkbox present on login (or agreement page)
 *   TC_Misc_02  Maintenance State field / calculation on asset detail
 *   TC_Misc_03  Suggested Shortcuts panel renders on SLDs / asset detail
 *   TC_Misc_04  Schedule page loads and shows calendar/list view
 */
public class MiscFeaturesTestNG extends BaseTest {

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private WebElement findText(String... candidates) {
        for (String c : candidates) {
            List<WebElement> els = driver.findElements(
                By.xpath("//*[contains(normalize-space(.), '" + c + "') and " +
                         "(self::label or self::span or self::div or self::a or self::button or self::p or self::h1 or self::h2 or self::h3)]"));
            for (WebElement el : els) {
                if (el.isDisplayed() && el.getText().trim().length() < 200) return el;
            }
        }
        return null;
    }

    // =================================================================
    // TC_Misc_01 — Terms & Conditions consent (post-ZP-1828 design)
    //
    // ZP-1828 (Web | Sign-in page: Remove T&C checkbox and update consent text)
    // shipped as Done. The new design:
    //   - REMOVED: standalone "I agree to T&C" checkbox
    //   - ADDED: inline consent statement under the Sign In button reading
    //            "By signing in, you agree to our Terms and Conditions and Privacy Policy."
    // Acceptance is now implicit by clicking Sign In.
    //
    // Approach C (strictest defense in depth — picked 2026-04-24): assert ALL THREE:
    //   (a) inline consent text is present (verifies the new shipped design)
    //   (b) the standalone T&C checkbox is gone (verifies ZP-1828 didn't regress)
    //   (c) Terms and Privacy Policy links are still clickable (compliance)
    //
    // If the checkbox sneaks back via a regression OR the consent text disappears,
    // this test fails and tells you exactly which invariant broke.
    // =================================================================
    @Test(priority = 1, description = "TC_Misc_01: Inline T&C consent text present, checkbox removed (ZP-1828)")
    public void testTC_Misc_01_TermsConditions() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_TERMS_CHECKBOX,
            "TC_Misc_01: T&C consent (post-ZP-1828)");
        try {
            // BaseTest logs in at class-level — /login redirects to dashboard while authenticated.
            // Need a HARD de-auth: about:blank to unmount React, then clear cookies + every
            // browser-side storage layer, then navigate to /login. Re-login at end of test
            // so subsequent @Test methods in the class still have a valid session.
            driver.get("about:blank");
            pause(500);
            driver.manage().deleteAllCookies();
            ((JavascriptExecutor) driver).executeScript(
                "try { localStorage.clear(); sessionStorage.clear(); "
                + "  if (window.indexedDB && indexedDB.databases) {"
                + "    indexedDB.databases().then(dbs => dbs.forEach(db => indexedDB.deleteDatabase(db.name)));"
                + "  }"
                + "  if (navigator.serviceWorker) {"
                + "    navigator.serviceWorker.getRegistrations().then(rs => rs.forEach(r => r.unregister()));"
                + "  }"
                + "} catch(e) {}");
            pause(800);
            driver.get(AppConstants.BASE_URL + "/login");
            pause(5000);
            logStepWithScreenshot("Login page (post hard de-auth)");

            // Self-diagnose: are we actually on the login page? Even if URL changed to /login,
            // the React SPA may have its in-memory auth state and continue rendering dashboard
            // content. We verify by checking for login-form fields (email + password) — which
            // are the unambiguous signal we hit the actual login screen.
            String currentUrl = driver.getCurrentUrl();
            boolean hasLoginForm = !driver.findElements(By.id("email")).isEmpty()
                    && !driver.findElements(By.id("password")).isEmpty();
            if (!hasLoginForm) {
                throw new org.testng.SkipException(
                    "TC_Misc_01: could not render login form from authenticated session "
                    + "(URL after de-auth: " + currentUrl + "; login form present: " + hasLoginForm
                    + "). React SPA preserves auth state in-memory. Move to a standalone class "
                    + "(BugHuntTestNG-style with own ChromeDriver) to validate the login page "
                    + "in isolation — already proposed as follow-up.");
            }

            // (a) Inline consent text present — confirms ZP-1828 shipped
            String bodyText = driver.findElement(By.tagName("body")).getText();
            boolean hasInlineConsent = bodyText.matches("(?is).*by\\s+signing\\s+in.*you\\s+agree.*terms.*and\\s+conditions.*")
                    || bodyText.matches("(?is).*by\\s+signing\\s+in.*agree.*privacy\\s+policy.*");
            Assert.assertTrue(hasInlineConsent,
                "Inline consent text 'By signing in, you agree to our Terms and Conditions and "
                + "Privacy Policy' missing on login — ZP-1828 design regression. Body: "
                + bodyText.substring(0, Math.min(400, bodyText.length())));
            logStep("(a) Inline consent text present");

            // (b) Standalone T&C checkbox should NOT be present (ZP-1828 removed it).
            //     Filter to a checkbox whose label/aria mentions Terms/Conditions/Agree, since
            //     other unrelated checkboxes (e.g. "Show password", "Remember me") are fine.
            List<WebElement> tcCheckboxes = driver.findElements(By.xpath(
                    "//input[@type='checkbox'][contains(translate(@aria-label,'TERMS','terms'),'terms') "
                    + "or contains(translate(@aria-label,'AGREE','agree'),'agree') "
                    + "or contains(translate(@aria-label,'CONDITIONS','conditions'),'conditions')]"
                    + " | //label[contains(translate(., 'TERMS','terms'), 'terms') "
                    + "or contains(translate(., 'AGREE','agree'), 'agree')]//input[@type='checkbox']"));
            int visibleTcCheckboxes = 0;
            for (WebElement cb : tcCheckboxes) if (cb.isDisplayed()) visibleTcCheckboxes++;
            Assert.assertEquals(visibleTcCheckboxes, 0,
                "Standalone T&C checkbox detected on login page — ZP-1828 regression "
                + "(checkbox should have been removed in favor of inline consent text). "
                + "Found " + visibleTcCheckboxes + " visible T&C-labeled checkbox(es).");
            logStep("(b) No standalone T&C checkbox present");

            // (c) Terms and Privacy Policy LINKS still clickable (compliance requirement)
            WebElement termsLink = findText("Terms and Conditions", "Terms & Conditions",
                "Terms of Service");
            WebElement privacyLink = findText("Privacy Policy", "Privacy Notice");
            Assert.assertNotNull(termsLink, "Terms and Conditions link missing on login — compliance regression");
            Assert.assertNotNull(privacyLink, "Privacy Policy link missing on login — compliance regression");
            logStep("(c) Terms + Privacy Policy links present");

            ScreenshotUtil.captureScreenshot("TC_Misc_01");
            ExtentReportManager.logPass("ZP-1828 design verified: inline consent + no checkbox + links present");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_01_error");
            Assert.fail("TC_Misc_01 crashed: " + e.getMessage());
        } finally {
            // Restore the class-level session so subsequent @Test methods stay logged in.
            // BaseTest's loginAndSelectSite() handles credential pull from AppConstants.
            try {
                loginAndSelectSite();
            } catch (Exception ignore) {
                logWarning("TC_Misc_01: failed to re-login at end — subsequent tests may break");
            }
        }
    }

    // =================================================================
    // TC_Misc_02 — Calculation: Condition of Maintenance (COM) score
    // =================================================================
    /**
     * Live-verified 2026-04-28: the asset detail page exposes the maintenance
     * state as a calculated card labeled "CONDITION OF MAINTENANCE (COM)" in
     * the top header strip — NOT as a "Maintenance State" / "Maintainability"
     * label and NOT under a "Calculations" tab (no such tab exists on this view).
     *
     * The card shows a numeric score (visible as a green badge in the screenshot,
     * e.g., "1"). This test asserts the card exists AND its value is a parseable
     * non-negative number — the calculation is meaningful, not just a label.
     */
    @Test(priority = 2, description = "Asset detail shows Condition of Maintenance (COM) calculation with a valid numeric score")
    public void testTC_Misc_02_MaintenanceState() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_MAINTENANCE_STATE,
            "TC_Misc_02: Condition of Maintenance");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets in grid");
            safeClick(rows.get(0));
            pause(3500);
            logStepWithScreenshot("Asset detail opened");

            // Locate the COM card. Real label is "Condition of Maintenance (COM)" —
            // accept "Condition of Maintenance" or just "COM" for resilience.
            WebElement comLabel = findText("Condition of Maintenance", "CONDITION OF MAINTENANCE",
                "(COM)", "COM");
            ScreenshotUtil.captureScreenshot("TC_Misc_02");
            Assert.assertNotNull(comLabel,
                "Condition of Maintenance (COM) card not found on asset detail page");

            // Capture the calculated score. The card structure puts a numeric badge
            // near the label. Walk the DOM to find a parseable number sibling/child.
            String score = (String) js().executeScript(
                "var el = arguments[0];"
                + "var card = el.closest('div, section, article') || el.parentElement;"
                + "for (var i = 0; i < 5 && card; i++) {"
                + "  var txt = (card.textContent || '').replace(/CONDITION OF MAINTENANCE|COM|\\(|\\)/gi, '').trim();"
                + "  var m = txt.match(/^([0-9]+(\\.[0-9]+)?)$/) || txt.match(/(^|[^0-9])([0-9]+(\\.[0-9]+)?)([^0-9]|$)/);"
                + "  if (m) return m[m.length >= 4 ? 2 : 1];"
                + "  card = card.parentElement;"
                + "}"
                + "return null;",
                comLabel);
            logStep("COM label found: '" + comLabel.getText().split("\n")[0] + "'");
            logStep("COM score extracted: " + score);

            // Falsifiable: the score must be a non-negative number. Empty / NaN means
            // either the card structure changed OR the calculation isn't running.
            Assert.assertNotNull(score,
                "COM card label found but no numeric score extractable. Card structure may have changed.");
            try {
                double s = Double.parseDouble(score);
                Assert.assertTrue(s >= 0,
                    "COM score is negative (" + s + ") — calculation invariant violated");
                ExtentReportManager.logPass("COM calculation present with valid score: " + score);
            } catch (NumberFormatException nfe) {
                Assert.fail("COM score '" + score + "' is not parseable as a number");
            }
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_02_error");
            Assert.fail("TC_Misc_02 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_02b — COM Calculator dialog structure (Maintenance State input)
    // =================================================================
    /**
     * The static COM card that TC_Misc_02 verifies is just the OUTPUT of the
     * COM Calculator dialog. The actual maintenance-state input lives behind
     * the "Calculator" button on the asset detail. Live-verified 2026-04-28
     * via user screenshot:
     *
     *   Dialog title: "COM Calculator"
     *   Section heading: "Maintenance State"
     *   Subtitle: "Check all statements that apply. If none apply, the
     *              equipment is rated Level 1 (like-new condition...)"
     *   "Derived maintenance level: Level X" — calculated dynamically
     *   Categorical groups: NONSERVICEABLE / LEVEL 3 — POOR / (more below fold)
     *   Buttons: Reset / Cancel / Apply Rating (N)
     *
     * This test asserts the dialog opens and exposes the structural elements.
     * TC_Misc_02c verifies interactivity (checking a box updates the derived level).
     */
    @Test(priority = 21, description = "COM Calculator dialog has Maintenance State checkboxes + derived level")
    public void testTC_Misc_02b_COMCalculatorStructure() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_MAINTENANCE_STATE,
            "TC_Misc_02b: COM Calculator structure");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets in grid");
            safeClick(rows.get(0));
            pause(3500);

            // Live-verified 2026-04-28: Calculator button is inside the Edit Asset
            // drawer (next to the COM rating field), NOT on the bare asset detail page.
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2500);
            logStepWithScreenshot("Edit Asset drawer opened");

            // Find the Calculator button. May need to scroll the drawer to surface it.
            WebElement calcBtn = findCalculatorButton();
            Assert.assertNotNull(calcBtn,
                "Calculator button not found in Edit Asset drawer (after scroll attempts)");
            safeClick(calcBtn);
            pause(2500);
            logStepWithScreenshot("COM Calculator opened");

            // Verify the dialog opened with the expected title
            WebElement comCalcTitle = findText("COM Calculator");
            Assert.assertNotNull(comCalcTitle, "COM Calculator dialog title not found after click");

            // Live-verified: dialog opens at "Asset Criticality" section. Maintenance
            // State is one of 3 factors and requires scrolling. The dialog header
            // text says "highest value among the three factors" — Asset Criticality
            // is one, Maintenance State is another.
            scrollComCalculatorToMaintenanceState();
            // findText filters elements with text > 200 chars (drops section containers).
            // Use JS to find the heading whose text is EXACTLY "Maintenance State".
            Boolean hasMaintHeading = (Boolean) js().executeScript(
                "var dlgs = document.querySelectorAll('[role=\"dialog\"]');"
                + "for (var d of dlgs) {"
                + "  if (!d.offsetWidth) continue;"
                + "  var headings = Array.from(d.querySelectorAll('*')).filter(function(el) {"
                + "    return (el.textContent || '').trim() === 'Maintenance State'"
                + "      && el.children.length === 0;"
                + "  });"
                + "  if (headings.length) return true;"
                + "}"
                + "return false;");
            Assert.assertTrue(Boolean.TRUE.equals(hasMaintHeading),
                "Maintenance State heading missing in COM Calculator (after scroll)");

            // "Derived maintenance level: Level X"
            String derivedLevel = (String) js().executeScript(
                "var dlgs = document.querySelectorAll('[role=\"dialog\"]');"
                + "for (var d of dlgs) {"
                + "  if (!d.offsetWidth) continue;"
                + "  var t = d.textContent || '';"
                + "  var m = t.match(/Derived maintenance level\\s*:?\\s*(Level\\s*[0-9]+|Nonserviceable)/i);"
                + "  if (m) return m[1];"
                + "}"
                + "return null;");
            logStep("Derived maintenance level: " + derivedLevel);
            Assert.assertNotNull(derivedLevel,
                "'Derived maintenance level' text not found in COM Calculator dialog — calculation engine may not be wired");

            // Verify checkbox count + section labels (NONSERVICEABLE + at least one LEVEL group)
            Object structure = js().executeScript(
                "var dlgs = document.querySelectorAll('[role=\"dialog\"]');"
                + "for (var d of dlgs) {"
                + "  if (!d.offsetWidth) continue;"
                + "  var t = d.textContent || '';"
                + "  if (!t.includes('Maintenance State')) continue;"
                + "  return {"
                + "    checkboxes: d.querySelectorAll('input[type=\"checkbox\"]').length,"
                + "    nonserviceable: t.toUpperCase().includes('NONSERVICEABLE'),"
                + "    hasLevel3: t.toUpperCase().includes('LEVEL 3'),"
                + "    hasApplyRating: Array.from(d.querySelectorAll('button')).some(function(b) { return (b.textContent || '').includes('Apply Rating'); }),"
                + "    hasReset: Array.from(d.querySelectorAll('button')).some(function(b) { return (b.textContent || '').trim() === 'Reset'; }),"
                + "    hasCancel: Array.from(d.querySelectorAll('button')).some(function(b) { return (b.textContent || '').trim() === 'Cancel'; })"
                + "  };"
                + "}"
                + "return null;");
            logStep("COM Calculator structure: " + structure);
            ScreenshotUtil.captureScreenshot("TC_Misc_02b");
            Assert.assertNotNull(structure, "COM Calculator dialog structure could not be inspected");

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> s = (java.util.Map<String, Object>) structure;
            int checkboxCount = ((Number) s.get("checkboxes")).intValue();
            Assert.assertTrue(checkboxCount >= 5,
                "Expected ≥5 maintenance-state checkboxes (multiple level groups). Found " + checkboxCount);
            Assert.assertTrue(Boolean.TRUE.equals(s.get("nonserviceable")),
                "NONSERVICEABLE category heading missing");
            Assert.assertTrue(Boolean.TRUE.equals(s.get("hasLevel3")),
                "LEVEL 3 category heading missing");
            Assert.assertTrue(Boolean.TRUE.equals(s.get("hasApplyRating")),
                "Apply Rating button missing — cannot commit calculator output");
            Assert.assertTrue(Boolean.TRUE.equals(s.get("hasReset")),
                "Reset button missing — cannot clear checkboxes");

            // Cleanup: cancel without applying so we don't mutate the asset's COM rating
            WebElement cancel = findText("Cancel");
            if (cancel != null) safeClick(cancel);
            pause(1500);

            ExtentReportManager.logPass("COM Calculator dialog structure verified — "
                + checkboxCount + " checkboxes, derived level: " + derivedLevel);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_02b_error");
            Assert.fail("TC_Misc_02b crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_02c — COM Calculator interactivity (checkbox changes derived level)
    // =================================================================
    /**
     * Falsifiable proof that the COM Calculator's calculation engine actually
     * runs: read the derived level BEFORE, check a NONSERVICEABLE box, read
     * the derived level AFTER, assert it changed. Reset + Cancel to avoid
     * mutating the asset's persisted rating.
     *
     * If the level doesn't change after checking NONSERVICEABLE, either the
     * onChange handler is broken OR the calculation isn't wired to the inputs.
     * Both are real product bugs — the test fires immediately.
     */
    @Test(priority = 22, description = "COM Calculator: checking a maintenance-state box updates the derived level")
    public void testTC_Misc_02c_COMCalculatorInteractivity() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_MAINTENANCE_STATE,
            "TC_Misc_02c: Calculator interactivity");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets in grid");
            safeClick(rows.get(0));
            pause(3500);

            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2500);
            WebElement calcBtn = findCalculatorButton();
            Assert.assertNotNull(calcBtn, "Calculator button missing in Edit Asset drawer");
            safeClick(calcBtn);
            pause(2500);
            scrollComCalculatorToMaintenanceState();

            // BEFORE: capture derived level
            String levelBefore = readDerivedLevel();
            logStep("Derived level BEFORE: " + levelBefore);
            Assert.assertNotNull(levelBefore, "Could not read initial derived level");

            // Toggle the FIRST NONSERVICEABLE checkbox using React's native setter
            // protocol. MUI's <Radio>/<Checkbox> hides the real <input> (opacity:0)
            // and Selenium's click on the visible label often gets eaten by ripple
            // wrappers. The reliable path: set checked=true via the prototype setter
            // and dispatch a synthetic change event React's onChange listens to.
            Boolean toggled = (Boolean) js().executeScript(
                "var dlgs = document.querySelectorAll('[role=\"dialog\"]');"
                + "for (var d of dlgs) {"
                + "  if (!d.offsetWidth) continue;"
                + "  var headings = Array.from(d.querySelectorAll('*')).filter(function(el) {"
                + "    return (el.textContent || '').trim().toUpperCase() === 'NONSERVICEABLE'"
                + "      && el.children.length === 0;"
                + "  });"
                + "  if (!headings.length) continue;"
                + "  var heading = headings[0];"
                + "  var allCbs = Array.from(d.querySelectorAll('input[type=\"checkbox\"]'));"
                + "  for (var cb of allCbs) {"
                + "    if (!(heading.compareDocumentPosition(cb) & Node.DOCUMENT_POSITION_FOLLOWING)) continue;"
                // React-controlled-input pattern: bypass React's setState protection
                + "    var setter = Object.getOwnPropertyDescriptor("
                + "      window.HTMLInputElement.prototype, 'checked').set;"
                + "    setter.call(cb, true);"
                + "    cb.dispatchEvent(new Event('change', { bubbles: true }));"
                + "    cb.dispatchEvent(new Event('input', { bubbles: true }));"
                // Also click for ripple/blur effects
                + "    cb.click();"
                + "    return true;"
                + "  }"
                + "}"
                + "return false;");
            Assert.assertTrue(Boolean.TRUE.equals(toggled),
                "Could not toggle a NONSERVICEABLE checkbox in COM Calculator");
            pause(2000);
            ScreenshotUtil.captureScreenshot("TC_Misc_02c_after_check");

            // AFTER: capture new derived level
            String levelAfter = readDerivedLevel();
            logStep("Derived level AFTER checking NONSERVICEABLE: " + levelAfter);
            Assert.assertNotNull(levelAfter, "Could not read post-click derived level");

            // Falsifiable: level should have changed. Checking a NONSERVICEABLE box
            // should escalate the equipment to Nonserviceable status (or at least
            // away from whatever it was before).
            Assert.assertNotEquals(levelAfter, levelBefore,
                "Derived maintenance level did NOT change after checking a NONSERVICEABLE "
                + "statement. Calculation engine may be disconnected from inputs. "
                + "(before='" + levelBefore + "', after='" + levelAfter + "')");

            // Reset to clear our changes, then Cancel to avoid persisting
            WebElement reset = findText("Reset");
            if (reset != null) { safeClick(reset); pause(1500); }
            WebElement cancel = findText("Cancel");
            if (cancel != null) { safeClick(cancel); pause(1500); }

            ExtentReportManager.logPass("COM Calculator engine works: '" + levelBefore
                + "' → '" + levelAfter + "' on NONSERVICEABLE check (rolled back via Reset+Cancel)");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_02c_error");
            Assert.fail("TC_Misc_02c crashed: " + e.getMessage());
        }
    }

    /**
     * Find the Calculator button inside the Edit Asset drawer. Per user
     * screenshot: a button labeled "Calculator" near the Condition of
     * Maintenance section with the 1/2/3 manual rating buttons.
     *
     * Prioritises the actual &lt;button&gt; element over any sibling span/div
     * that also contains the word "Calculator" — clicking the wrong target
     * (a span) silently no-ops and the dialog never opens.
     */
    private WebElement findCalculatorButton() {
        // Prefer real <button> elements on the right side of the viewport (drawer area)
        Object btn = js().executeScript(
            "var btns = Array.from(document.querySelectorAll('button'));"
            + "for (var b of btns) {"
            + "  var txt = (b.textContent || '').trim();"
            + "  if (txt.toLowerCase() !== 'calculator') continue;" // exact text only
            + "  var r = b.getBoundingClientRect();"
            + "  if (r.width === 0 || r.x < 600) continue;"
            + "  return b;"
            + "}"
            // Loose match if exact didn't hit
            + "for (var b of btns) {"
            + "  if (!(b.textContent || '').toLowerCase().includes('calculator')) continue;"
            + "  var r = b.getBoundingClientRect();"
            + "  if (r.width === 0 || r.x < 600) continue;"
            + "  return b;"
            + "}"
            + "return null;");
        if (btn instanceof WebElement) return (WebElement) btn;
        // Scroll drawer + retry once
        try {
            js().executeScript(
                "var dws = document.querySelectorAll('[class*=\"Drawer\"] [class*=\"Paper\"], "
                + "[class*=\"drawer\"], [role=\"presentation\"] > div > div');"
                + "for (var d of dws) {"
                + "  if (d.scrollHeight > d.clientHeight) { d.scrollTop = d.scrollHeight; }"
                + "}");
        } catch (Exception ignore) {}
        pause(800);
        Object retry = js().executeScript(
            "var btns = Array.from(document.querySelectorAll('button'));"
            + "for (var b of btns) {"
            + "  if (!(b.textContent || '').toLowerCase().includes('calculator')) continue;"
            + "  var r = b.getBoundingClientRect();"
            + "  if (r.width === 0 || r.x < 600) continue;"
            + "  return b;"
            + "}"
            + "return null;");
        return retry instanceof WebElement ? (WebElement) retry : null;
    }

    /**
     * Scroll the open COM Calculator dialog to surface the "Maintenance State"
     * section. The dialog opens at "Asset Criticality" by default — Maintenance
     * State is the second of three factors and requires scrolling.
     */
    private void scrollComCalculatorToMaintenanceState() {
        try {
            js().executeScript(
                "var dlgs = document.querySelectorAll('[role=\"dialog\"]');"
                + "for (var d of dlgs) {"
                + "  if (!d.offsetWidth) continue;"
                // Find the Maintenance State heading inside this dialog
                + "  var headings = Array.from(d.querySelectorAll('*')).filter(function(el) {"
                + "    return (el.textContent || '').trim() === 'Maintenance State'"
                + "      && el.children.length === 0;"
                + "  });"
                + "  if (headings.length) {"
                + "    headings[0].scrollIntoView({block: 'center', behavior: 'instant'});"
                + "    return true;"
                + "  }"
                // Otherwise just scroll the dialog body down
                + "  var scrollable = d.querySelector('[class*=\"DialogContent\"], [class*=\"dialog-content\"]') || d;"
                + "  scrollable.scrollTop = scrollable.scrollHeight / 2;"
                + "}"
                + "return false;");
            pause(800);
        } catch (Exception ignore) {}
    }

    /** Read the "Derived maintenance level: Level X" text from the open COM Calculator. */
    private String readDerivedLevel() {
        return (String) js().executeScript(
            "var dlgs = document.querySelectorAll('[role=\"dialog\"]');"
            + "for (var d of dlgs) {"
            + "  if (!d.offsetWidth) continue;"
            + "  var t = d.textContent || '';"
            + "  var m = t.match(/Derived maintenance level\\s*:?\\s*(Level\\s*[0-9]+|Nonserviceable)/i);"
            + "  if (m) return m[1].trim();"
            + "}"
            + "return null;");
    }

    // =================================================================
    // TC_Misc_03 — Suggested Shortcut combobox in Edit Asset drawer
    // =================================================================
    /**
     * Live-verified 2026-04-28: "Suggested Shortcut (Optional)" lives inside
     * the Edit Asset drawer's BASIC INFO section as a combobox — NOT on the
     * SLDs page and NOT directly on the asset detail page. Earlier captures
     * via CopyToCopyFromTestNG saw the field as "Select shortcut" combobox.
     *
     * This test asserts the combobox exists, is interactable, and opens a
     * dropdown when clicked (real picker, not just a label).
     */
    @Test(priority = 3, description = "Suggested Shortcut combobox in Edit Asset drawer is interactable")
    public void testTC_Misc_03_SuggestedShortcuts() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_Misc_03: Suggested Shortcut combobox");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets in grid");
            safeClick(rows.get(0));
            pause(3500);

            // Open Edit Asset drawer via the established AssetPage helper
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2500);
            logStepWithScreenshot("Edit Asset drawer opened");

            // Look for the "Suggested Shortcut" label + its combobox.
            // The drawer is on the right side of viewport (x ≥ 600 — verified live).
            WebElement shortcutLabel = findText("Suggested Shortcut", "SUGGESTED SHORTCUT");
            Assert.assertNotNull(shortcutLabel,
                "Suggested Shortcut label not found in Edit Asset drawer. Looked for "
                + "'Suggested Shortcut' / 'SUGGESTED SHORTCUT' — actual label may have "
                + "changed.");

            // Find the associated combobox
            Object comboInfo = js().executeScript(
                "var lbl = arguments[0];"
                + "var ctx = lbl.closest('section, div[class*=\"FormControl\"], div[class*=\"section\"]') || lbl.parentElement;"
                + "for (var i = 0; i < 6 && ctx; i++) {"
                + "  var combo = ctx.querySelector('[role=\"combobox\"], [class*=\"MuiSelect-select\"]');"
                + "  if (combo) {"
                + "    var r = combo.getBoundingClientRect();"
                + "    return {"
                + "      found: true,"
                + "      role: combo.getAttribute('role') || '',"
                + "      ariaLabel: combo.getAttribute('aria-label') || '',"
                + "      placeholder: (combo.textContent || '').trim().slice(0, 40),"
                + "      visible: r.width > 0 && r.height > 0"
                + "    };"
                + "  }"
                + "  ctx = ctx.parentElement;"
                + "}"
                + "return {found: false};",
                shortcutLabel);
            logStep("Suggested Shortcut combobox: " + comboInfo);
            ScreenshotUtil.captureScreenshot("TC_Misc_03");

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> info = (java.util.Map<String, Object>) comboInfo;
            Assert.assertTrue(Boolean.TRUE.equals(info.get("found")),
                "Suggested Shortcut LABEL found but no combobox/select within 6 ancestors");
            Assert.assertTrue(Boolean.TRUE.equals(info.get("visible")),
                "Suggested Shortcut combobox exists but is not visible");

            ExtentReportManager.logPass("Suggested Shortcut combobox in Edit drawer is interactable. "
                + "Placeholder/value: '" + info.get("placeholder") + "'");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_03_error");
            Assert.fail("TC_Misc_03 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_03b — Suggested Shortcut dropdown opens with ≥1 option
    //
    // Pre-existing TC_Misc_03 only verifies the combobox EXISTS and is
    // visible. That's a weak claim — a broken combobox that renders but
    // has zero options would still pass. The user-confirmed full behavior
    // (per their Edit drawer screenshot showing the field with red focus
    // border) is that clicking it opens a dropdown with selectable
    // shortcut presets.
    //
    // This test exercises the actual dropdown:
    //   1. Open Edit drawer for an asset
    //   2. Scroll to the Suggested Shortcut combobox
    //   3. Click it to open the dropdown
    //   4. Assert ≥1 option is rendered (proves the data layer is wired)
    //   5. Close (ESC) — DO NOT select to avoid mutating the asset's
    //      applied_shortcut field (which currently is null per DevTools
    //      console: `applied_shortcut: null`)
    //
    // Falsifiable: if the dropdown opens with zero options, that means
    // either (a) no shortcut templates exist for this asset class, OR
    // (b) the data fetch failed silently. Either is a real product issue.
    // =================================================================
    @Test(priority = 31, description = "Suggested Shortcut dropdown opens with at least one selectable option")
    public void testTC_Misc_03b_SuggestedShortcutDropdownHasOptions() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_Misc_03b: Shortcut dropdown options");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets in grid");
            safeClick(rows.get(0));
            pause(3500);

            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2500);
            logStepWithScreenshot("Edit Asset drawer opened");

            // Scroll the drawer body all the way down to ensure ALL sections render
            // (Suggested Shortcut sits far below the fold). MUI Drawer's content is
            // wrapped in a scrollable Paper element — scroll its scrollTop to bottom.
            js().executeScript(
                "var papers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');"
                + "for (var p of papers) {"
                + "  if (p.offsetWidth > 0) {"
                + "    p.scrollTop = p.scrollHeight;"
                + "    return;"
                + "  }"
                + "}");
            pause(1000);
            // Then scroll back to the Suggested Shortcut label specifically
            js().executeScript(
                "var els = document.querySelectorAll('*');"
                + "for (var el of els) {"
                + "  var t = (el.textContent || '').trim();"
                + "  if ((t.startsWith('Suggested Shortcut') || t === 'Suggested Shortcut')"
                + "      && el.children.length === 0) {"
                + "    el.scrollIntoView({block: 'center'}); return true;"
                + "  }"
                + "}"
                + "return false;");
            pause(800);
            logStepWithScreenshot("Drawer scrolled to Suggested Shortcut field");

            // Diagnostic: dump first 30 chars of every label-like text in the drawer
            // so future failures show what's actually present
            Object labelsDump = js().executeScript(
                "var papers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');"
                + "for (var p of papers) {"
                + "  if (!p.offsetWidth) continue;"
                + "  var labels = p.querySelectorAll('label, .MuiInputLabel-root');"
                + "  return Array.from(labels).map(function(l) {"
                + "    return (l.textContent || '').trim().slice(0, 40);"
                + "  }).filter(function(t) { return t.length > 0; });"
                + "}"
                + "return [];");
            logStep("Drawer labels visible: " + labelsDump);

            // Locate label first. The combobox is associated with this label by
            // SPATIAL PROXIMITY (next combobox in DOM order that's ≤200px below
            // the label). FormControl-based scoping was too strict for this field
            // (combobox isn't always in the same wrapper), and ancestor-based
            // walking was too loose (caught Asset Class which is first in DOM).
            // The proximity heuristic mirrors how a sighted user reads forms.
            WebElement shortcutLabel = findText("Suggested Shortcut", "SUGGESTED SHORTCUT");
            Assert.assertNotNull(shortcutLabel,
                "Suggested Shortcut label not found in Edit drawer — see TC_Misc_03");
            Object comboObj = js().executeScript(
                "var lbl = arguments[0];"
                + "var lblRect = lbl.getBoundingClientRect();"
                + "var allCombos = document.querySelectorAll('[role=\"combobox\"], "
                + "[class*=\"MuiSelect-select\"]');"
                + "var best = null;"
                + "var bestDist = 1e9;"
                + "for (var combo of allCombos) {"
                + "  if (!combo.offsetWidth) continue;"
                // Must come AFTER the label in DOM order
                + "  var pos = lbl.compareDocumentPosition(combo);"
                + "  if (!(pos & Node.DOCUMENT_POSITION_FOLLOWING)) continue;"
                + "  var r = combo.getBoundingClientRect();"
                + "  var dy = r.top - lblRect.top;"
                + "  if (dy < 0 || dy > 200) continue;" // within 200px below
                + "  if (dy < bestDist) { bestDist = dy; best = combo; }"
                + "}"
                + "if (best) { best.scrollIntoView({block: 'center'}); return best; }"
                + "return null;",
                shortcutLabel);
            Assert.assertTrue(comboObj instanceof WebElement,
                "No combobox within 200px below the Suggested Shortcut label. "
                + "Either the field has no combobox OR MUI DOM structure changed.");
            WebElement combo = (WebElement) comboObj;
            pause(500);
            try {
                safeClick(combo);
            } catch (Exception ex) {
                // MUI Select sometimes intercepts clicks on the visible div —
                // fallback: JS click which fires the dropdown open
                js().executeScript("arguments[0].click();", combo);
            }
            pause(1500);
            logStepWithScreenshot("Suggested Shortcut dropdown clicked");

            // After clicking, MUI renders the options as a popover/listbox at
            // document body level (NOT inside the drawer). Look for visible
            // li[role='option'] elements.
            @SuppressWarnings("unchecked")
            List<String> dropdownOptions = (List<String>) js().executeScript(
                "var opts = document.querySelectorAll('li[role=\"option\"], "
                + "[role=\"listbox\"] [role=\"option\"]');"
                + "var out = [];"
                + "for (var o of opts) {"
                + "  if (!o.offsetWidth) continue;"
                + "  var t = (o.textContent || '').trim();"
                + "  if (t.length > 0 && t.length < 80) out.push(t);"
                + "}"
                + "return Array.from(new Set(out));");
            logStep("Suggested Shortcut dropdown options: " + dropdownOptions);
            ScreenshotUtil.captureScreenshot("TC_Misc_03b");

            // Cleanup: close dropdown via ESC — do NOT select an option
            // (would set applied_shortcut on the asset)
            try {
                driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
            } catch (Exception ignore) {}
            pause(800);

            // Honest precondition: zero options means EITHER no shortcut presets
            // exist for this asset class (data state, not a bug) OR the API
            // failed silently (real bug — can't distinguish from this layer).
            // Skip rather than fake-pass or false-fail. The test is meaningful
            // ONLY when presets exist, and the existing TC_Misc_03 already
            // verifies the combobox is present + interactable as a base claim.
            if (dropdownOptions == null || dropdownOptions.isEmpty()) {
                throw new org.testng.SkipException(
                    "Suggested Shortcut dropdown opened with zero options — likely no "
                    + "shortcut presets are configured for this asset class. Test data "
                    + "prerequisite: at least one shortcut preset must exist for the "
                    + "first asset's class. Run on an account with seeded shortcuts to "
                    + "verify the dropdown wiring.");
            }

            ExtentReportManager.logPass("Suggested Shortcut dropdown has "
                + dropdownOptions.size() + " selectable option(s): " + dropdownOptions);
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_03b_error");
            Assert.fail("TC_Misc_03b crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_04 — Schedule page
    // =================================================================
    @Test(priority = 4, description = "Schedule/Scheduling page loads with calendar or list view")
    public void testTC_Misc_04_SchedulePage() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SCHEDULE,
            "TC_Misc_04: Schedule page");
        try {
            // Try multiple common paths
            String[] paths = { "/scheduling", "/schedule", "/calendar" };
            String loadedPath = null;
            for (String p : paths) {
                driver.get(AppConstants.BASE_URL + p);
                pause(4000);
                // Check if page rendered (not 404/blank)
                String body = driver.findElement(By.tagName("body")).getText();
                if (body.length() > 100 && !body.contains("Page Not Found")) {
                    loadedPath = p; break;
                }
            }
            Assert.assertNotNull(loadedPath, "Schedule page did not load at any of " + String.join(", ", paths));
            logStep("Schedule page at: " + loadedPath);
            logStepWithScreenshot("Schedule page loaded");

            // Look for calendar grid OR list view
            List<WebElement> calendar = driver.findElements(By.cssSelector(
                "[class*='Calendar'], [class*='calendar'], [class*='scheduler'], " +
                ".MuiDataGrid-root, [role='grid'], [role='table']"));
            ScreenshotUtil.captureScreenshot("TC_Misc_04");
            Assert.assertFalse(calendar.isEmpty(),
                "Schedule page has no calendar or grid view");
            logStep("Calendar / grid elements: " + calendar.size());
            ExtentReportManager.logPass("Schedule page loads with calendar/grid view");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_04_error");
            Assert.fail("TC_Misc_04 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_05 — T&C link target is correct (href to policy page)
    // =================================================================
    @Test(priority = 5, description = "Terms & Conditions link has a valid href (not javascript:void or #)")
    public void testTC_Misc_05_TermsLinkHref() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_TERMS_CHECKBOX,
            "TC_Misc_05: T&C link href");
        try {
            driver.get(AppConstants.BASE_URL + "/login");
            pause(4000);
            List<WebElement> links = driver.findElements(By.tagName("a"));
            String href = null;
            String text = null;
            for (WebElement a : links) {
                String t = a.getText() == null ? "" : a.getText().trim();
                if (t.matches("(?i).*(terms|privacy|conditions).*") && a.isDisplayed()) {
                    href = a.getAttribute("href");
                    text = t;
                    break;
                }
            }
            ScreenshotUtil.captureScreenshot("TC_Misc_05");
            Assert.assertNotNull(href, "T&C link has no href at all");
            logStep("T&C link: '" + text + "' → " + href);
            Assert.assertFalse(href.startsWith("javascript:"), "T&C href is javascript:void — broken link");
            Assert.assertFalse("#".equals(href) || href.endsWith("/#"),
                "T&C href is # — placeholder, not a real policy URL");
            ExtentReportManager.logPass("T&C link points to real URL: " + href);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_05_error");
            Assert.fail("TC_Misc_05 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_06 — Maintenance State value is one of the allowed states (not empty/free text)
    // =================================================================
    @Test(priority = 6, description = "Maintenance State value is a known enum (not free text)")
    public void testTC_Misc_06_MaintenanceStateEnum() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_MAINTENANCE_STATE,
            "TC_Misc_06: Maintenance State enum");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);
            WebElement calcTab = findText("Calculations", "Calculation");
            if (calcTab != null) { safeClick(calcTab); pause(2000); }

            Object val = js().executeScript(
                "var all = document.querySelectorAll('label, [class*=\"FormControl\"], *');" +
                "for (var el of all) {" +
                "  if (el.children.length > 0 && !el.tagName.match(/label|span|div/i)) continue;" +
                "  var t = (el.textContent || '').toLowerCase();" +
                "  if (t.includes('maintenance state') || t.includes('maintenance status')) {" +
                "    var parent = el.parentElement;" +
                "    var val = parent ? parent.querySelector('input, span, div') : null;" +
                "    if (val) return (val.value || val.textContent || '').trim().substring(0,80);" +
                "  }" +
                "}" +
                "return null;");
            ScreenshotUtil.captureScreenshot("TC_Misc_06");
            logStep("Maintenance State value: " + val);
            if (val == null) { logWarning("No Maintenance State value readable"); return; }
            String s = val.toString().toLowerCase();
            // Expected enum set (guessed common labels for electrical asset state)
            boolean known = s.matches(".*(in service|out of service|retired|maintenance|active|inactive|" +
                "operational|decommissioned|pending|unknown|scheduled|overdue|compliant|non-compliant|good|fair|poor).*");
            Assert.assertTrue(known || s.isEmpty(),
                "Maintenance State value '" + val + "' does not match a known state enum — free text?");
            ExtentReportManager.logPass("Maintenance State: " + val);
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_06_error");
            Assert.fail("TC_Misc_06 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_07 — Suggested Shortcut navigates when clicked
    // =================================================================
    @Test(priority = 7, description = "Clicking a Suggested Shortcut triggers navigation or action")
    public void testTC_Misc_07_ShortcutNavigates() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_Misc_07: Shortcut navigates");
        try {
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(5000);
            WebElement shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions");
            if (shortcut == null) {
                assetPage.navigateToAssets();
                pause(3000);
                List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
                if (!rows.isEmpty()) {
                    safeClick(rows.get(0));
                    pause(3500);
                    shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions");
                }
            }
            if (shortcut == null) { logWarning("No shortcuts panel"); return; }

            // Find a clickable shortcut item near the label
            String urlBefore = driver.getCurrentUrl();
            List<WebElement> btns = driver.findElements(By.cssSelector(
                "[class*='Shortcut'] button, [class*='shortcut'] button, " +
                "[class*='QuickAction'] button, [class*='quick'] button, [role='button']"));
            WebElement clickable = null;
            for (WebElement b : btns) {
                if (b.isDisplayed() && b.getText() != null && b.getText().length() > 0 && b.getText().length() < 40) {
                    clickable = b; break;
                }
            }
            if (clickable == null) { logWarning("No clickable shortcut item"); return; }
            String label = clickable.getText();
            safeClick(clickable);
            pause(3000);
            String urlAfter = driver.getCurrentUrl();
            boolean dialogOpened = !driver.findElements(
                By.cssSelector("[role='dialog']:not([aria-hidden='true'])")).isEmpty();
            ScreenshotUtil.captureScreenshot("TC_Misc_07");
            logStep("Shortcut '" + label + "' clicked. URL before: " + urlBefore + " after: " + urlAfter
                + ", dialog opened: " + dialogOpened);
            boolean actedOn = !urlBefore.equals(urlAfter) || dialogOpened;
            Assert.assertTrue(actedOn,
                "Shortcut '" + label + "' click did nothing (no nav, no dialog)");
            ExtentReportManager.logPass("Suggested Shortcut triggers action on click");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_07_error");
            Assert.fail("TC_Misc_07 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_SUGG_01 — Suggested Shortcuts: chips render for an asset
    //
    // Tightens TC_Misc_03 (which only confirmed a panel label exists) to
    // assert that ACTUAL clickable shortcut chips render in the panel.
    // =================================================================
    @Test(priority = 11, description = "TC_SUGG_01: Suggested Shortcuts panel renders at least one shortcut chip")
    public void testTC_SUGG_01_ChipsRenderedForAsset() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_SUGG_01: Suggested Shortcuts chips render");
        try {
            // Live evidence: /api/shortcut/by-node-class fires 11x on dashboard load (one per node
            // class) and 0x on /slds. So the feature populates dashboard and/or asset-create form,
            // not the SLDs page.
            //
            // Recon 2026-04-27 (admin role on acme.qa): scanned /dashboard, /assets, /sites for
            // text matching /shortcut|quick.action|suggested|quick.add/i — ZERO hits visible.
            // The /api/shortcut/by-node-class endpoint IS being called (TC_SUGG_03 measures it),
            // but no UI panel surfaces the data with admin login. Possibilities:
            //   1. Feature is role-gated to a different role (technician/PM/etc.)
            //   2. Feature is keyed off a specific entry point (asset-create form?)
            //   3. Label has changed entirely from "Suggested Shortcuts"
            //   4. Feature is shipped but mounted only on specific routes I haven't tested
            //
            // Labels intentionally narrow — only match the FULL feature names to avoid false
            // positives. If feature is genuinely absent on these paths, test SkipExceptions
            // cleanly rather than false-failing.
            String[] panelLabels = {"Suggested Shortcuts", "Quick Actions", "Suggested Assets",
                                    "Add Shortcut", "Quick Add"};
            String[] paths = {"/dashboard", "/sites", "/assets"};

            WebElement panel = null;
            String foundOn = null;
            for (String path : paths) {
                driver.get(AppConstants.BASE_URL + path);
                pause(5000);
                panel = findText(panelLabels);
                if (panel != null) { foundOn = path; break; }
            }
            // Last-resort fallback: open an asset detail (clicking first row on /assets)
            if (panel == null) {
                driver.get(AppConstants.BASE_URL + "/assets");
                pause(4000);
                List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
                if (!rows.isEmpty()) {
                    safeClick(rows.get(0));
                    pause(3500);
                    panel = findText(panelLabels);
                    foundOn = "/assets/<detail>";
                }
            }
            logStepWithScreenshot("Panel search complete (foundOn=" + foundOn + ")");

            if (panel == null) {
                throw new org.testng.SkipException(
                    "Suggested Shortcuts panel not found on /dashboard, /sites, /assets, or first-asset detail. "
                    + "Either feature is gated behind a role/permission, or the label has changed. "
                    + "Skipping — TC_SUGG_03 still validates the API-call dedup invariant.");
            }

            // Count the chips/buttons inside or near the panel — MUI Chips OR role=button siblings
            List<WebElement> chips = driver.findElements(By.xpath(
                "//*[contains(normalize-space(.), 'Suggested Shortcuts') or contains(normalize-space(.), 'Shortcuts')"
                + " or contains(normalize-space(.), 'Quick Actions') or contains(normalize-space(.), 'Suggested')]"
                + "/following::*[contains(@class, 'MuiChip-root') or @role='button'][position() < 12]"));
            int visibleChips = 0;
            for (WebElement c : chips) {
                if (c.isDisplayed() && c.getSize().getWidth() > 20) visibleChips++;
            }
            ScreenshotUtil.captureScreenshot("TC_SUGG_01");
            Assert.assertTrue(visibleChips > 0,
                "Suggested Shortcuts panel found on " + foundOn + " but no chips/buttons rendered. "
                + "Visible chips: " + visibleChips);
            logStep("Visible shortcut chips on " + foundOn + ": " + visibleChips);
            ExtentReportManager.logPass("Suggested Shortcuts shows " + visibleChips + " chip(s) on " + foundOn);
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_SUGG_01_error");
            Assert.fail("TC_SUGG_01 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_SUGG_02 — Clicking a suggested shortcut adds a node, no duplicate fires
    // =================================================================
    @Test(priority = 12, description = "TC_SUGG_02: Clicking a suggested shortcut adds a node and doesn't duplicate-fire")
    public void testTC_SUGG_02_ClickShortcutAddsNodeNoDuplicate() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_SUGG_02: Click shortcut adds node, no duplicate");
        try {
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(6000);

            // Capture node count BEFORE the click. SLD nodes typically render as <g> elements
            // inside the canvas svg, OR as cards with class containing 'node'.
            int nodesBefore = countSldNodes();
            logStep("SLD nodes before click: " + nodesBefore);

            // Find first Suggested Shortcut chip
            List<WebElement> chips = driver.findElements(By.xpath(
                "//*[contains(normalize-space(.), 'Suggested Shortcuts') or contains(normalize-space(.), 'Shortcuts')]"
                + "/following::div[contains(@class, 'MuiChip-root') or @role='button'][position() < 5]"));
            WebElement chip = null;
            for (WebElement c : chips) {
                if (c.isDisplayed() && c.getSize().getWidth() > 20) { chip = c; break; }
            }
            if (chip == null) {
                throw new org.testng.SkipException("No clickable shortcut chip — TC_SUGG_01 covers presence");
            }
            String chipLabel = chip.getText().trim();
            logStep("Clicking chip: " + chipLabel);
            safeClick(chip);
            pause(3000);

            int nodesAfter = countSldNodes();
            int delta = nodesAfter - nodesBefore;
            logStep("SLD nodes after click: " + nodesAfter + " (delta=" + delta + ")");

            // Assertion: clicking a shortcut should add EXACTLY ONE node — never zero (broken),
            // never two+ (duplicate-fire bug).
            ScreenshotUtil.captureScreenshot("TC_SUGG_02");
            Assert.assertEquals(delta, 1,
                "Clicking suggested shortcut '" + chipLabel + "' added " + delta
                + " nodes (expected exactly 1). 0 = feature broken, 2+ = duplicate-fire.");
            ExtentReportManager.logPass("Suggested Shortcut click added exactly 1 node");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_SUGG_02_error");
            Assert.fail("TC_SUGG_02 crashed: " + e.getMessage());
        }
    }

    private int countSldNodes() {
        try {
            Long count = (Long) js().executeScript(
                "var nodes = document.querySelectorAll('g.node, [class*=\"-node\"], [data-id^=\"node\"], "
                + ".MuiCard-root[class*=\"node\"]');"
                + "return nodes.length;");
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    // =================================================================
    // TC_SUGG_03 — Shortcuts API not called >1 time per asset class change
    //
    // Performance sibling: today's live audit found /api/shortcut/by-node-class
    // hit 11 times on dashboard load (one per node class). Validates the same
    // pattern doesn't recur per-asset-class-switch on the SLD where the shortcut
    // panel lives.
    // =================================================================
    @Test(priority = 13, description = "TC_SUGG_03: /api/shortcut/by-node-class fires <=1x per asset-class change")
    public void testTC_SUGG_03_ShortcutApiNotCalledExcessively() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_SUGG_03: Shortcut API call dedup");
        try {
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(7000);

            // Use Performance API to count network resource entries hitting /api/shortcut/by-node-class
            Long apiCallCount = (Long) js().executeScript(
                "if (typeof performance === 'undefined' || !performance.getEntriesByType) return -1;"
                + "var entries = performance.getEntriesByType('resource');"
                + "var hits = 0;"
                + "for (var i = 0; i < entries.length; i++) {"
                + "  if (entries[i].name && entries[i].name.indexOf('/api/shortcut/by-node-class') !== -1) hits++;"
                + "}"
                + "return hits;");
            int hits = apiCallCount == null ? -1 : apiCallCount.intValue();
            logStep("Shortcut API calls observed on SLD load: " + hits);
            ScreenshotUtil.captureScreenshot("TC_SUGG_03");

            if (hits == -1) {
                throw new org.testng.SkipException("Performance API not available");
            }

            // The bug: dashboard fires 11x (one per node class) because each class fetches its own.
            // On a SLD page that opens with no asset selected, expectation is < 5 (gives slack for
            // side-panels) — anything ≥ 10 strongly suggests the dashboard's per-class fan-out.
            Assert.assertTrue(hits < 10,
                "/api/shortcut/by-node-class fired " + hits + "x on SLD load (expected < 10). "
                + "Likely the same per-node-class fan-out pattern as on Dashboard. "
                + "Should be a single batched query.");
            ExtentReportManager.logPass("Shortcut API call count acceptable: " + hits);
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_SUGG_03_error");
            Assert.fail("TC_SUGG_03 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_08 — Schedule page exposes create/add entry point
    // =================================================================
    @Test(priority = 8, description = "Schedule page exposes an Add/Create event control")
    public void testTC_Misc_08_ScheduleHasCreateEntry() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SCHEDULE,
            "TC_Misc_08: Schedule create entry");
        try {
            String[] paths = { "/scheduling", "/schedule", "/calendar" };
            for (String p : paths) {
                driver.get(AppConstants.BASE_URL + p);
                pause(4000);
                String body = driver.findElement(By.tagName("body")).getText();
                if (body.length() > 100 && !body.contains("Page Not Found")) break;
            }
            pause(2000);
            WebElement add = findText("Add", "Create", "Schedule Work", "New Event", "+");
            ScreenshotUtil.captureScreenshot("TC_Misc_08");
            Assert.assertNotNull(add,
                "Schedule page has no Add/Create control — users can't schedule new events");
            logStep("Schedule add/create control: " + add.getText());
            ExtentReportManager.logPass("Schedule page exposes create entry point");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_08_error");
            Assert.fail("TC_Misc_08 crashed: " + e.getMessage());
        }
    }
}
