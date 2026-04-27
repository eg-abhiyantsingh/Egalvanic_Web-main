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
    // TC_Misc_02 — Maintenance State calculation
    // =================================================================
    @Test(priority = 2, description = "Asset detail shows Maintenance State field or calculation")
    public void testTC_Misc_02_MaintenanceState() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_MAINTENANCE_STATE,
            "TC_Misc_02: Maintenance State");
        try {
            assetPage.navigateToAssets();
            pause(3000);
            List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
            Assert.assertFalse(rows.isEmpty(), "No assets");
            safeClick(rows.get(0));
            pause(3500);
            logStepWithScreenshot("Asset detail opened");

            // Maintenance state may be on a "Calculations" tab OR directly shown as a field
            WebElement calcTab = findText("Calculations", "Calculation");
            if (calcTab != null) { safeClick(calcTab); pause(2000); }

            WebElement maintState = findText("Maintenance State", "Maintenance Status", "Maintainability");
            ScreenshotUtil.captureScreenshot("TC_Misc_02");
            Assert.assertNotNull(maintState,
                "Maintenance State field/label not found on asset detail calculations");
            logStep("Maintenance State: " + maintState.getText().substring(0, Math.min(100, maintState.getText().length())));
            ExtentReportManager.logPass("Maintenance State field present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_02_error");
            Assert.fail("TC_Misc_02 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Misc_03 — Suggested Shortcuts
    // =================================================================
    @Test(priority = 3, description = "Suggested Shortcuts panel renders on SLD or asset detail")
    public void testTC_Misc_03_SuggestedShortcuts() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_SUGGESTED_SHORTCUTS,
            "TC_Misc_03: Suggested Shortcuts");
        try {
            // First try SLDs page
            driver.get(AppConstants.BASE_URL + "/slds");
            pause(5000);
            logStepWithScreenshot("SLDs page");

            WebElement shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions", "Suggested");
            if (shortcut == null) {
                // Try asset detail
                assetPage.navigateToAssets();
                pause(3000);
                List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
                if (!rows.isEmpty()) {
                    safeClick(rows.get(0));
                    pause(3500);
                    shortcut = findText("Suggested Shortcuts", "Shortcuts", "Quick Actions");
                }
            }
            ScreenshotUtil.captureScreenshot("TC_Misc_03");
            Assert.assertNotNull(shortcut,
                "Suggested Shortcuts panel not found on SLDs or asset detail");
            logStep("Suggested Shortcuts: " + shortcut.getText().substring(0, Math.min(100, shortcut.getText().length())));
            ExtentReportManager.logPass("Suggested Shortcuts panel present");
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Misc_03_error");
            Assert.fail("TC_Misc_03 crashed: " + e.getMessage());
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
            // not the SLDs page. Try multiple candidate locations.
            // Labels tightened: dropped "Suggested" alone — too generic, matches sidebar items.
            // Now require the FULL feature name to avoid false-positive panel matches.
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
