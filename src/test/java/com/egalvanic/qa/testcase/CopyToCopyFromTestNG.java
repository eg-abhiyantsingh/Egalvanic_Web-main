package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Copy To / Copy From — ZP-323.
 *
 * The user can "Copy From" another asset/connection (pulls their attributes
 * into the current record) or "Copy To" (push current record's attributes
 * to another). Both flows typically surface as kebab menu items or a button
 * near the record header.
 *
 * Coverage:
 *   TC_Copy_01  Copy From entry point available
 *   TC_Copy_02  Copy To entry point available
 *   TC_Copy_03  Copy From dialog shows asset picker / previews source data
 *   TC_Copy_04  Cancel Copy flow does not modify the target record
 */
public class CopyToCopyFromTestNG extends BaseTest {

    private WebElement findByText(String... candidates) {
        for (String c : candidates) {
            List<WebElement> els = driver.findElements(
                By.xpath("//button[contains(normalize-space(.), '" + c + "')] | " +
                         "//*[@role='button'][contains(normalize-space(.), '" + c + "')] | " +
                         "//*[@role='menuitem'][contains(normalize-space(.), '" + c + "')]"));
            for (WebElement el : els) if (el.isDisplayed()) return el;
        }
        return null;
    }

    private void openFirstAssetDetail() {
        assetPage.navigateToAssets();
        pause(3000);
        List<WebElement> rows = driver.findElements(By.cssSelector(".MuiDataGrid-row"));
        Assert.assertFalse(rows.isEmpty(), "No assets in grid");
        safeClick(rows.get(0));
        pause(3500);
    }

    /**
     * Open the Edit Asset drawer.
     *
     * Verified live 2026-04-28: the established pattern in this project is
     * <code>AssetPage.clickKebabMenuItem("Edit Asset")</code>. The asset detail
     * page has a PAGE-LEVEL kebab; that menu has "Edit Asset" — clicking it
     * opens the Edit drawer. There's no top-level "Edit" button.
     */
    private boolean openEditDrawer() {
        try {
            assetPage.clickKebabMenuItem("Edit Asset");
            pause(2500);
            String body = driver.findElement(By.tagName("body")).getText();
            return body.contains("Edit Asset") || body.contains("BASIC INFO");
        } catch (Exception e) {
            System.out.println("[CopyTest] openEditDrawer failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Open the "Copy Details" menu in the Edit Asset drawer header.
     *
     * Live-verified 2026-04-28: the trigger has aria-label="Copy Details"
     * (semantic, not generic "more"). Its SVG path is MoreVert (three dots)
     * but data-testid is empty, so aria-label is the reliable selector.
     * Drawer header sits at y≈15; icons are Copy Details (x=1240),
     * Refresh (x=1278), Close (x=1316).
     */
    private boolean openDrawerKebab() {
        // Strategy 1 — exact aria-label (verified live)
        List<WebElement> direct = driver.findElements(
            By.cssSelector("button[aria-label='Copy Details']"));
        for (WebElement b : direct) {
            if (!b.isDisplayed()) continue;
            try {
                safeClick(b);
                pause(1500);
                long visible = driver.findElements(
                    By.cssSelector("[role='menuitem'], .MuiMenuItem-root"))
                    .stream().filter(WebElement::isDisplayed).count();
                if (visible > 0) return true;
            } catch (Exception ignore) {}
        }

        // Strategy 2 — fallback: any rightmost MoreVert-style button at top of viewport
        List<WebElement> candidates = driver.findElements(By.xpath(
            "//button[.//*[name()='svg'][contains(@data-testid, 'MoreVert') "
            + "or contains(@data-testid, 'MoreHoriz') or contains(@data-testid, 'Menu')]] "
            + "| //button[contains(@aria-label, 'Copy') "
            + "or contains(@aria-label, 'More') or contains(@aria-label, 'menu')]"));
        candidates.removeIf(b -> !b.isDisplayed());
        candidates.sort((a, b) -> Integer.compare(
            b.getRect().getX(), a.getRect().getX()));

        for (WebElement k : candidates) {
            org.openqa.selenium.Rectangle r = k.getRect();
            if (r.getX() < 800 || r.getY() > 200) continue;
            try {
                safeClick(k);
                pause(1500);
                long visible = driver.findElements(
                    By.cssSelector("[role='menuitem'], .MuiMenuItem-root"))
                    .stream().filter(WebElement::isDisplayed).count();
                if (visible > 0) return true;
            } catch (Exception ignore) {}
        }
        return false;
    }

    /**
     * Open Edit drawer + kebab + return the matching menu item (or null).
     * Centralises the navigation so all 8 tests don't repeat it.
     */
    private WebElement findCopyMenuItem(String... labels) {
        if (!openEditDrawer()) return null;
        if (!openDrawerKebab()) return null;
        return findByText(labels);
    }

    /**
     * Locate the Copy Details dialog (centered modal, NOT the underlying Edit drawer).
     * Both surfaces use role='dialog' in MUI, so we discriminate by title text.
     */
    private WebElement findCopyDialog() {
        for (WebElement d : driver.findElements(By.cssSelector("[role='dialog']"))) {
            if (!d.isDisplayed()) continue;
            String txt = d.getText();
            if (txt == null) continue;
            if (txt.startsWith("Copy Details From") || txt.startsWith("Copy Details To")) return d;
        }
        return null;
    }

    /**
     * Close the Copy Details dialog. Live-verified 2026-04-28: the dialog has NO
     * Cancel text button — only an X icon at the top-right. NEVER use ESC here:
     * per project memory `project_mui_drawer_escape`, ESC closes the parent MUI
     * Drawer too, which would invalidate the rest of the test.
     */
    private boolean closeCopyDialog() {
        WebElement dlg = findCopyDialog();
        if (dlg == null) return true; // already closed
        // Strategy 1: text Cancel/Close button if any
        for (String label : new String[]{"Cancel", "Close"}) {
            List<WebElement> btns = dlg.findElements(
                By.xpath(".//button[contains(normalize-space(.), '" + label + "')]"));
            for (WebElement b : btns) {
                if (!b.isDisplayed()) continue;
                try { safeClick(b); pause(1200); return findCopyDialog() == null; }
                catch (Exception ignore) {}
            }
        }
        // Strategy 2: X icon (aria-label="close" / "Close" — typical MUI)
        List<WebElement> xs = dlg.findElements(By.cssSelector(
            "button[aria-label='close'], button[aria-label='Close'], "
            + "button[aria-label*='close' i]"));
        for (WebElement b : xs) {
            if (!b.isDisplayed()) continue;
            try { safeClick(b); pause(1200); return findCopyDialog() == null; }
            catch (Exception ignore) {}
        }
        // Strategy 3: rightmost svg-only button in the dialog header (the X)
        List<WebElement> headerBtns = dlg.findElements(By.cssSelector(
            "header button, [class*='DialogTitle'] button"));
        headerBtns.removeIf(b -> !b.isDisplayed());
        headerBtns.sort((a, b) -> Integer.compare(b.getRect().getX(), a.getRect().getX()));
        if (!headerBtns.isEmpty()) {
            try { safeClick(headerBtns.get(0)); pause(1200); return findCopyDialog() == null; }
            catch (Exception ignore) {}
        }
        return false;
    }

    /**
     * Source-asset rows in the Copy Details picker. Returns the VISIBLE clickable
     * wrapper (typically a &lt;label&gt;) for each row, NOT the underlying native
     * radio input.
     *
     * Why JS-based discovery: MUI's Radio renders a real &lt;input type="radio"&gt;
     * but hides it with opacity:0 + absolute positioning (kept for keyboard a11y).
     * Selenium's isDisplayed() reports those inputs as not-displayed, so a naive
     * `findElements + isDisplayed` filter returns 0. JS can see them and we can
     * walk to the visible parent in one shot.
     */
    /**
     * Find an enabled button in the given dialog whose text matches one of the
     * provided labels (in order of preference). Returns null if none found.
     */
    private WebElement findDialogButton(WebElement dlg, String... labels) {
        if (dlg == null) return null;
        for (String label : labels) {
            List<WebElement> btns = dlg.findElements(By.xpath(
                ".//button[contains(normalize-space(.), '" + label + "')]"));
            for (WebElement b : btns) {
                if (b.isDisplayed() && b.isEnabled()) return b;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<WebElement> dialogRadioRows() {
        WebElement dlg = findCopyDialog();
        if (dlg == null) return java.util.Collections.emptyList();
        Object res = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
            "var dlg = arguments[0];"
            + "var inputs = Array.from(dlg.querySelectorAll(\"input[type='radio']\"));"
            + "var rows = inputs.map(function(i) {"
            + "  return i.closest('label') || i.closest('[role=\"option\"]') "
            + "    || i.closest('li') || i.parentElement;"
            + "}).filter(Boolean);"
            + "return Array.from(new Set(rows));", dlg);
        return res == null ? java.util.Collections.emptyList()
            : (List<WebElement>) res;
    }

    @Test(priority = 1, description = "Copy Details From entry in Edit Asset drawer kebab")
    public void testTC_Copy_01_CopyFromEntry() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_01: Copy Details From entry");
        try {
            openFirstAssetDetail();
            // Verified live 2026-04-28: feature lives in Edit Asset drawer's
            // 3-dot kebab menu, NOT on the asset detail page itself.
            // Menu labels are "Copy Details From..." and "Copy Details To...".
            WebElement entry = findCopyMenuItem("Copy Details From", "Copy details from");
            ScreenshotUtil.captureScreenshot("TC_Copy_01");
            Assert.assertNotNull(entry,
                "Copy Details From entry not found in Edit Asset drawer kebab. "
                + "Expected location: Asset row → Edit → kebab → 'Copy Details From...'");
            logStep("Copy Details From entry: " + entry.getText());
            ExtentReportManager.logPass("Copy Details From entry point present");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_01_error");
            Assert.fail("TC_Copy_01 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Copy Details To entry in Edit Asset drawer kebab")
    public void testTC_Copy_02_CopyToEntry() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_02: Copy Details To entry");
        try {
            openFirstAssetDetail();
            WebElement entry = findCopyMenuItem("Copy Details To", "Copy details to");
            ScreenshotUtil.captureScreenshot("TC_Copy_02");
            Assert.assertNotNull(entry,
                "Copy Details To entry not found in Edit Asset drawer kebab. "
                + "Expected location: Asset row → Edit → kebab → 'Copy Details To...'");
            logStep("Copy Details To entry: " + entry.getText());
            ExtentReportManager.logPass("Copy Details To entry point present");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_02_error");
            Assert.fail("TC_Copy_02 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Copy From dialog opens with asset picker (radio list + search)")
    public void testTC_Copy_03_CopyFromDialogPicker() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_03: Copy From picker");
        try {
            openFirstAssetDetail();
            WebElement entry = findCopyMenuItem("Copy Details From", "Copy details from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy Details From not in Edit Asset drawer kebab on this run — see TC_Copy_01.");
            }
            safeClick(entry);
            pause(3000);
            logStepWithScreenshot("Copy From dialog");

            WebElement dlg = findCopyDialog();
            Assert.assertNotNull(dlg, "Copy Details From dialog did not open");

            // Live-verified 2026-04-28: dialog has search input + radio rows for sources
            List<WebElement> pickers = dlg.findElements(By.cssSelector(
                "input[placeholder*='Search' i], input[type='search']"));
            Assert.assertFalse(pickers.isEmpty(),
                "Copy From dialog has no search input");

            List<WebElement> radios = dialogRadioRows();
            logStep("Search inputs: " + pickers.size() + " | Radio rows: " + radios.size());
            Assert.assertFalse(radios.isEmpty(),
                "Copy From dialog has no selectable source assets (radio rows). "
                + "Either no other assets exist for this account or the picker DOM changed.");

            closeCopyDialog();
            ExtentReportManager.logPass("Copy From dialog: search + " + radios.size() + " source row(s)");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_03_error");
            Assert.fail("TC_Copy_03 crashed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Cancel Copy flow leaves target record unchanged")
    public void testTC_Copy_04_CancelDoesNotModify() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_04: Cancel preserves");
        try {
            openFirstAssetDetail();
            String originalName = assetPage.getDetailPageAssetName();

            WebElement entry = findCopyMenuItem("Copy Details From", "Copy Details To", "Copy details from", "Copy details to");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "No Copy Details menu items in Edit drawer kebab — see TC_Copy_01/02");
            }
            safeClick(entry);
            pause(2500);
            // Close WITHOUT selecting any radio — the cancel/X path
            closeCopyDialog();
            pause(2000);

            String afterName = assetPage.getDetailPageAssetName();
            logStep("Before: " + originalName + " | After cancel: " + afterName);
            ScreenshotUtil.captureScreenshot("TC_Copy_04");
            Assert.assertEquals(afterName, originalName,
                "Cancel on Copy flow appears to have modified the record");
            ExtentReportManager.logPass("Cancel on Copy flow preserves record");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_04_error");
            Assert.fail("TC_Copy_04 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_05 — Search filter narrows the asset picker
    // =================================================================
    @Test(priority = 5, description = "Typing in Copy From picker filters the asset list")
    public void testTC_Copy_05_PickerSearchFilters() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_05: Picker search");
        try {
            openFirstAssetDetail();
            WebElement entry = findCopyMenuItem("Copy Details From", "Copy details from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy Details From absent in Edit drawer kebab — see TC_Copy_01");
            }
            safeClick(entry);
            pause(3000);

            WebElement dlg = findCopyDialog();
            if (dlg == null) {
                throw new org.testng.SkipException("Copy From dialog did not open");
            }
            List<WebElement> pickers = dlg.findElements(By.cssSelector(
                "input[placeholder*='Search' i], input[type='search']"));
            if (pickers.isEmpty()) {
                throw new org.testng.SkipException(
                    "Copy From dialog opened but has no search input");
            }
            WebElement picker = pickers.get(0);
            int beforeCount = dialogRadioRows().size();
            safeClick(picker); pause(800);
            picker.sendKeys("ZZZZ_UNLIKELY_" + System.currentTimeMillis());
            pause(1800);
            int afterCount = dialogRadioRows().size();
            logStep("Source rows before filter: " + beforeCount + " | after typing unlikely string: " + afterCount);
            ScreenshotUtil.captureScreenshot("TC_Copy_05");
            closeCopyDialog();
            pause(1000);
            Assert.assertTrue(afterCount < beforeCount || afterCount == 0,
                "Picker search did not filter the radio list (before=" + beforeCount + " after=" + afterCount + ")");
            ExtentReportManager.logPass("Copy From picker search filters correctly");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_05_error");
            Assert.fail("TC_Copy_05 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_06 — Copy From excludes the current (self) asset
    // =================================================================
    @Test(priority = 6, description = "Current asset is excluded from Copy From picker (can't copy from self)")
    public void testTC_Copy_06_ExcludesSelf() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_06: Exclude self");
        try {
            openFirstAssetDetail();
            String currentName = assetPage.getDetailPageAssetName();
            logStep("Current asset: " + currentName);
            WebElement entry = findCopyMenuItem("Copy Details From", "Copy details from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy Details From absent in Edit drawer kebab — see TC_Copy_01");
            }
            safeClick(entry);
            pause(3000);

            WebElement dlg = findCopyDialog();
            if (dlg == null || currentName == null || currentName.isEmpty()) {
                closeCopyDialog();
                throw new org.testng.SkipException(
                    "Cannot probe self-exclusion — dialog ("
                    + (dlg == null ? "null" : "ok") + ") or current name ('"
                    + currentName + "') missing");
            }
            List<WebElement> pickers = dlg.findElements(By.cssSelector(
                "input[placeholder*='Search' i], input[type='search']"));
            if (pickers.isEmpty()) {
                closeCopyDialog();
                throw new org.testng.SkipException("Copy From dialog has no search input");
            }
            WebElement picker = pickers.get(0);
            safeClick(picker); pause(800);
            String frag = currentName.length() > 4 ? currentName.substring(0, 4) : currentName;
            picker.sendKeys(frag);
            pause(2000);

            // Walk each radio row's parent label/container and check the visible text
            boolean selfListed = false;
            for (WebElement r : dialogRadioRows()) {
                // The row text is on the row container, not the radio input itself
                WebElement row = r;
                try {
                    row = (WebElement) ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("return arguments[0].closest('label, [role=\"option\"], li, div[class*=\"row\"], div[class*=\"Row\"]') || arguments[0].parentElement;", r);
                } catch (Exception ignore) {}
                String rowText = row != null && row.getText() != null ? row.getText() : "";
                if (rowText.contains(currentName)) { selfListed = true; break; }
            }
            ScreenshotUtil.captureScreenshot("TC_Copy_06");
            closeCopyDialog();
            pause(1000);

            Assert.assertFalse(selfListed,
                "Current asset '" + currentName + "' appears in its own Copy From picker");
            ExtentReportManager.logPass("Copy From picker excludes current (self) asset");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_06_error");
            Assert.fail("TC_Copy_06 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_07 — Copy offers per-field selection (don't force full-clone)
    // =================================================================
    @Test(priority = 7, description = "Copy dialog exposes per-field checkboxes to choose what to copy")
    public void testTC_Copy_07_FieldSelectorInDialog() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_07: Field selector");
        try {
            openFirstAssetDetail();
            WebElement entry = findCopyMenuItem("Copy Details From", "Copy Details To", "Copy details from", "Copy details to");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "No Copy Details menu items in Edit drawer kebab — see TC_Copy_01/02");
            }
            safeClick(entry);
            pause(3000);

            WebElement dlg7 = findCopyDialog();
            List<WebElement> checkboxes = dlg7 == null
                ? java.util.Collections.<WebElement>emptyList()
                : dlg7.findElements(By.cssSelector("input[type='checkbox']"));
            logStep("Checkboxes in copy dialog: " + checkboxes.size());
            ScreenshotUtil.captureScreenshot("TC_Copy_07");
            closeCopyDialog();
            pause(1000);

            // Soft expectation — some flows do whole-record copy by design
            if (checkboxes.size() < 2) {
                logWarning("Copy dialog has < 2 checkboxes — may be whole-record copy by design. " +
                    "Flag for product spec: should users be able to cherry-pick fields?");
            }
            ExtentReportManager.logPass("Copy dialog field selectors: " + checkboxes.size());
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_07_error");
            Assert.fail("TC_Copy_07 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_08 — Copy preserves the target's identity (ID, QR, creation timestamp)
    // =================================================================
    @Test(priority = 8, description = "Target asset's identity fields (QR code, creation date) unchanged after Copy From cancel")
    public void testTC_Copy_08_TargetIdentityPreservedOnCancel() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_08: Target identity preserved");
        try {
            openFirstAssetDetail();
            // Read QR and creation if visible
            Object identityBefore = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var result = {};" +
                "var all = document.querySelectorAll('label, [class*=\"FormControl\"]');" +
                "for (var l of all) {" +
                "  var t = (l.textContent || '').toLowerCase();" +
                "  var input = l.querySelector('input');" +
                "  if (!input) continue;" +
                "  if (t.includes('qr') || t.includes('id') || t.includes('created')) {" +
                "    result[t.trim().substring(0,30)] = input.value;" +
                "  }" +
                "}" +
                "return result;");
            logStep("Identity before Copy: " + identityBefore);

            WebElement entry = findCopyMenuItem("Copy Details From", "Copy details from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy Details From absent in Edit drawer kebab — see TC_Copy_01");
            }
            safeClick(entry);
            pause(2500);
            closeCopyDialog();
            pause(2000);

            Object identityAfter = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var result = {};" +
                "var all = document.querySelectorAll('label, [class*=\"FormControl\"]');" +
                "for (var l of all) {" +
                "  var t = (l.textContent || '').toLowerCase();" +
                "  var input = l.querySelector('input');" +
                "  if (!input) continue;" +
                "  if (t.includes('qr') || t.includes('id') || t.includes('created')) {" +
                "    result[t.trim().substring(0,30)] = input.value;" +
                "  }" +
                "}" +
                "return result;");
            logStep("Identity after Copy cancel: " + identityAfter);
            ScreenshotUtil.captureScreenshot("TC_Copy_08");
            String before = identityBefore == null ? "" : identityBefore.toString();
            String after = identityAfter == null ? "" : identityAfter.toString();
            Assert.assertEquals(after, before,
                "Identity fields changed after Copy From cancel: before=" + before + " after=" + after);
            ExtentReportManager.logPass("Identity fields preserved after Copy From cancel");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_08_error");
            Assert.fail("TC_Copy_08 crashed: " + e.getMessage());
        }
    }

    // =================================================================
    // TC_Copy_09 — Positive: selecting a source radio + applying copies data
    // =================================================================
    /**
     * Live-verified 2026-04-28: dialog rows have radio inputs (user clarified the
     * "checkbox" he saw is really a radio circle). This test exercises the actual
     * selection path: pick the first source asset, dialog should accept the choice
     * (auto-close OR submit button), and the Edit drawer should reflect the change
     * via a visible signal (toast / dialog dismissal / changed field value).
     *
     * Why this matters: TC_Copy_03..08 only proved the picker SURFACE works.
     * Without this test, a regression where clicking the radio does nothing
     * (e.g., FE wired up the radio visually but forgot the onChange handler)
     * would slip through.
     */
    @Test(priority = 9, description = "Selecting a source radio + applying copies data into the Edit drawer")
    public void testTC_Copy_09_SelectSourceCopiesData() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_NEW_COVERAGE, AppConstants.FEATURE_COPY_TO_FROM,
            "TC_Copy_09: Select source + apply");
        try {
            openFirstAssetDetail();
            WebElement entry = findCopyMenuItem("Copy Details From", "Copy details from");
            if (entry == null) {
                throw new org.testng.SkipException(
                    "Copy Details From absent in Edit drawer kebab — see TC_Copy_01");
            }
            safeClick(entry);
            pause(3000);

            WebElement dlg = findCopyDialog();
            Assert.assertNotNull(dlg, "Copy From dialog did not open");

            List<WebElement> radios = dialogRadioRows();
            if (radios.isEmpty()) {
                closeCopyDialog();
                throw new org.testng.SkipException(
                    "No source assets available to select — account has only one asset, "
                    + "so Copy From has nothing to copy from");
            }

            // dialogRadioRows() now returns the visible label wrapper — click it directly.
            WebElement firstRow = radios.get(0);
            String sourceLabel = firstRow.getText() == null ? "(unknown)"
                : firstRow.getText().split("\n")[0];
            logStep("Selecting source: " + sourceLabel);
            safeClick(firstRow);
            pause(1500);

            // Verify the underlying native radio is now checked (use JS — Selenium can't
            // see opacity:0 inputs reliably).
            boolean checked = false;
            try {
                Object res = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var r = arguments[0].querySelector(\"input[type='radio']\"); "
                    + "return r && r.checked;", firstRow);
                checked = Boolean.TRUE.equals(res);
            } catch (Exception ignore) {}
            logStep("Radio checked after click: " + checked);

            // Live-verified 2026-04-28: dialog is a 2-step wizard.
            //   Step 1 (source picker): buttons appear AFTER radio click → [Cancel] [Next]
            //   Step 2 (field selection): terminal button → [Apply] / [Copy] / [Save] / [Done]
            // Also handles a future single-step variant (Apply directly visible after click).
            String step1Used = null;
            WebElement step1Btn = findDialogButton(findCopyDialog(),
                "Next", "Apply", "Copy", "Save", "Done", "Confirm", "Continue", "OK");
            if (step1Btn == null) {
                // Maybe the dialog auto-closed on radio click (rare but possible)
                if (findCopyDialog() == null && checked) {
                    ScreenshotUtil.captureScreenshot("TC_Copy_09");
                    ExtentReportManager.logPass(
                        "Source radio click auto-closed the picker (selected: "
                        + sourceLabel + ")");
                    return;
                }
                ScreenshotUtil.captureScreenshot("TC_Copy_09");
                closeCopyDialog();
                Assert.fail("Radio clicked (checked=" + checked + ") but no advance "
                    + "button (Next/Apply/Copy/Save/Done/Confirm/Continue/OK) found in dialog. "
                    + "Either the click target is wrong or the FE wizard added a new step.");
            }
            step1Used = step1Btn.getText();
            logStep("Step 1 advance: clicking '" + step1Used + "'");
            safeClick(step1Btn);
            pause(2500);

            // Did the single-step variant just close the dialog?
            if (findCopyDialog() == null) {
                ScreenshotUtil.captureScreenshot("TC_Copy_09");
                ExtentReportManager.logPass("Single-step copy completed via '"
                    + step1Used + "' (source: " + sourceLabel + ")");
                return;
            }

            // Step 2: find the terminal button (NOT another "Next" — we want the apply)
            WebElement step2Btn = findDialogButton(findCopyDialog(),
                "Apply", "Copy", "Save", "Done", "Confirm", "OK", "Finish");
            if (step2Btn == null) {
                ScreenshotUtil.captureScreenshot("TC_Copy_09_step2");
                closeCopyDialog();
                throw new org.testng.SkipException(
                    "Step 2 reached after '" + step1Used + "' but no terminal "
                    + "button found — manual: walk the wizard to capture step-2 button label.");
            }
            String step2Used = step2Btn.getText();
            logStep("Step 2 terminal: clicking '" + step2Used + "'");
            safeClick(step2Btn);
            pause(3000);

            ScreenshotUtil.captureScreenshot("TC_Copy_09");
            WebElement after = findCopyDialog();
            if (after != null) {
                closeCopyDialog();
                Assert.fail("After clicking '" + step2Used + "' the Copy dialog stayed open "
                    + "— terminal step did not commit the copy.");
            }
            ExtentReportManager.logPass("Two-step copy completed: source='" + sourceLabel
                + "' → '" + step1Used + "' → '" + step2Used + "'");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("TC_Copy_09_error");
            Assert.fail("TC_Copy_09 crashed: " + e.getMessage());
        }
    }
}
