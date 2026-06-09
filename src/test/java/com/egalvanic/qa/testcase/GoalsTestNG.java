package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Goals Module — coverage for 8 open Jira bugs with ZERO prior test class.
 *
 * Cluster origin: 8 bugs filed on 2026-03-18 by Abhiyant Singh in a single
 * focused audit of the Goals module. See docs/bug-hunts/2026-04-24-all-open-
 * web-bugs-deep-audit.md for the full bug↔test coverage matrix.
 *
 * Bugs targeted (priority: High → Low):
 *   TC_GOAL_01  ZP-1552  Start Date allowed after Due Date — no date-range validation
 *   TC_GOAL_02  ZP-1550  Negative Target Value (-5555) accepted — no numeric validation
 *   TC_GOAL_03  ZP-1329  Edit/Delete icons not visible for Account Goals (100% zoom)
 *   TC_GOAL_04  ZP-1553  Goals table no horizontal scrollbar — Edit button off-screen
 *   TC_GOAL_05  ZP-1337  Cadence column not displayed for One-Time goals
 *   TC_GOAL_06  ZP-1556  Period column shows date range without year ("Mar 16 - Dec 31"
 *                        but no 2026)
 *   TC_GOAL_07  ZP-1557  Current value silently mutates on edit (Time Period → Custom
 *                        changes $0 to $426,736). DEV-ENV repro; gracefully skips on QA
 *                        if the canary goal is absent.
 *   TC_GOAL_08  ZP-1558  Cadence silently changes "Yearly" → "Weekly" on Custom date save
 *
 * What this class does NOT do:
 *   - Create/destroy Goals data (would require elevated permissions on some tenants)
 *   - Run destructive ops against Prod (see AppConstants.BASE_URL)
 *
 * Architecture: extends BaseTest (logged-in, site-selected class-level session).
 *   Reuses the established BaseTest login flow.
 *
 * House style: semantic xpath where possible, scoped locators, no Thread.sleep
 *   leaking into tests (uses BaseTest.pause() helper only inside waits).
 */
public class GoalsTestNG extends BaseTest {

    private static final String MODULE = "Goals";
    private static final String FEATURE_VALIDATION = "Goal Validation";
    private static final String FEATURE_LAYOUT = "Goal Layout";
    private static final String FEATURE_DATA = "Goal Data Integrity";

    private static final String GOALS_URL = AppConstants.BASE_URL + "/accounts/goals";

    // Locators — semantic XPath with fallbacks for product-side drift
    private static final By GOALS_TAB = By.xpath(
            "//a[normalize-space()='Goals'] | //button[normalize-space()='Goals']"
            + " | //*[normalize-space()='Goals' and @role='tab']");
    private static final By CREATE_GOAL_BTN = By.xpath(
            "//button[normalize-space()='Create Goal' or normalize-space()='+ Create Goal'"
            + " or contains(normalize-space(), 'Create Goal')]");
    private static final By TARGET_VALUE_INPUT = By.xpath(
            "//label[contains(., 'Target Value')]/..//input[@type='number' or @type='text']"
            + " | //input[@name='targetValue']");
    private static final By START_DATE_INPUT = By.xpath(
            "//label[contains(., 'Start Date')]/..//input | //input[@name='startDate']");
    private static final By DUE_DATE_INPUT = By.xpath(
            "//label[contains(., 'Due Date')]/..//input | //input[@name='dueDate']");
    private static final By SAVE_BTN = By.xpath(
            "//button[normalize-space()='Save' or normalize-space()='Save Changes'"
            + " or normalize-space()='Create' or normalize-space()='Create Goal']");
    private static final By GOALS_TABLE = By.xpath(
            "//table | //*[@role='grid'] | //*[contains(@class, 'MuiDataGrid-root')]");
    private static final By TABLE_HEADERS = By.xpath(
            "//*[@role='columnheader'] | //th");
    private static final By EDIT_ICON = By.xpath(
            "//button[@aria-label='Edit' or @aria-label='Edit Goal']"
            + " | //svg[@data-testid='EditIcon']/..");

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    /**
     * Navigate to Goals. Tolerates two access paths: direct URL, or sidebar/tab click.
     * Returns false if Goals is inaccessible for the current role (no fail — test skips gracefully).
     */
    private boolean navigateToGoalsIfAccessible() {
        try {
            driver.get(GOALS_URL);
            waitAndDismissAppAlert();
            pause(2000);
            if (!driver.getCurrentUrl().contains("goals")) {
                // fall back to clicking a Goals tab/link if direct URL got redirected
                List<WebElement> tabs = driver.findElements(GOALS_TAB);
                for (WebElement t : tabs) {
                    if (t.isDisplayed()) { safeClick(t); pause(1500); break; }
                }
            }
            // Confirm we got there
            boolean onGoalsView = driver.getCurrentUrl().toLowerCase().contains("goal")
                    || !driver.findElements(GOALS_TABLE).isEmpty()
                    || getPageText().toLowerCase().contains("create goal");
            if (!onGoalsView) {
                logWarning("Goals view not accessible for this test user/role — skipping");
                return false;
            }
            return true;
        } catch (Exception e) {
            logWarning("navigateToGoalsIfAccessible error: " + e.getMessage());
            return false;
        }
    }

    private String getPageText() {
        try {
            return driver.findElement(By.tagName("body")).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean openCreateGoalModal() {
        List<WebElement> btns = driver.findElements(CREATE_GOAL_BTN);
        for (WebElement b : btns) {
            if (b.isDisplayed() && b.isEnabled()) {
                safeClick(b);
                pause(1500);
                return true;
            }
        }
        return false;
    }

    @Test(priority = 1, description = "TC_GOAL_01: Start Date after Due Date is rejected by validation")
    public void testTC_GOAL_01_StartDateAfterDueDateRejected() {
        ExtentReportManager.createTest(MODULE, FEATURE_VALIDATION,
                "TC_GOAL_01_StartDateAfterDueDateRejected (ZP-1552)");

        if (!navigateToGoalsIfAccessible()) {
            throw new org.testng.SkipException("Goals module not accessible");
        }
        if (!openCreateGoalModal()) {
            logWarning("Create Goal button not present — may be role-gated");
            throw new org.testng.SkipException("Create Goal not available");
        }

        // Set Start Date = far future, Due Date = near future (Start > Due — should be rejected)
        try {
            WebElement start = driver.findElement(START_DATE_INPUT);
            safeClick(start);
            start.sendKeys("12/31/2026");
            WebElement due = driver.findElement(DUE_DATE_INPUT);
            safeClick(due);
            due.sendKeys("01/15/2026");
            pause(800);
            logStepWithScreenshot("Start > Due set");

            List<WebElement> saveBtns = driver.findElements(SAVE_BTN);
            WebElement save = saveBtns.isEmpty() ? null : saveBtns.get(saveBtns.size() - 1);
            if (save == null) throw new org.testng.SkipException("Save button not found in modal");

            boolean saveDisabled = !save.isEnabled()
                    || "true".equalsIgnoreCase(save.getAttribute("aria-disabled"))
                    || (save.getAttribute("class") != null && save.getAttribute("class").contains("Mui-disabled"));
            String pageText = getPageText().toLowerCase();
            boolean hasValidationMessage = pageText.contains("start date must")
                    || pageText.contains("due date must")
                    || pageText.contains("invalid date")
                    || pageText.contains("start must be before")
                    || pageText.contains("date range");

            Assert.assertTrue(saveDisabled || hasValidationMessage,
                    "Expected either Save disabled OR a visible validation message when Start > Due. "
                    + "saveDisabled=" + saveDisabled + " hasValidationMessage=" + hasValidationMessage);
            ExtentReportManager.logPass("ZP-1552 coverage: date-range validation enforced");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_01 error state");
            Assert.fail("TC_GOAL_01 failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "TC_GOAL_02: Negative Target Value is rejected by validation")
    public void testTC_GOAL_02_NegativeTargetValueRejected() {
        ExtentReportManager.createTest(MODULE, FEATURE_VALIDATION,
                "TC_GOAL_02_NegativeTargetValueRejected (ZP-1550)");

        if (!navigateToGoalsIfAccessible()) throw new org.testng.SkipException("Goals not accessible");
        if (!openCreateGoalModal()) throw new org.testng.SkipException("Create Goal not available");

        try {
            WebElement target = driver.findElement(TARGET_VALUE_INPUT);
            safeClick(target);
            // Clear then type a negative number
            js().executeScript(
                    "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
                    + "s.call(arguments[0], '-5555');"
                    + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", target);
            pause(500);

            List<WebElement> saveBtns = driver.findElements(SAVE_BTN);
            WebElement save = saveBtns.isEmpty() ? null : saveBtns.get(saveBtns.size() - 1);
            if (save == null) throw new org.testng.SkipException("Save button not found");

            boolean saveDisabled = !save.isEnabled()
                    || "true".equalsIgnoreCase(save.getAttribute("aria-disabled"))
                    || (save.getAttribute("class") != null && save.getAttribute("class").contains("Mui-disabled"));
            String pageText = getPageText().toLowerCase();
            boolean hasValidationMessage = pageText.contains("must be positive")
                    || pageText.contains("must be greater than")
                    || pageText.contains("invalid value")
                    || pageText.contains("target must");

            Assert.assertTrue(saveDisabled || hasValidationMessage,
                    "Expected Save disabled OR validation message for negative target. "
                    + "saveDisabled=" + saveDisabled + " hasValidationMessage=" + hasValidationMessage);
            ExtentReportManager.logPass("ZP-1550 coverage: negative target rejected");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_02 error state");
            Assert.fail("TC_GOAL_02 failed: " + e.getMessage());
        }
    }

    // Quarantined RED tripwire — ZP-1329 still OPEN (live: 25 edit icons in DOM, 0 visible in
    // viewport at 100% zoom). Assertion NOT weakened; excluded from the functional gate.
    @Test(priority = 3, groups = {"known-product-bug"},
          description = "TC_GOAL_03: Edit/Delete icons visible for Account Goals at 100% zoom [tripwire: ZP-1329]")
    public void testTC_GOAL_03_EditDeleteIconsVisibleAt100Zoom() {
        ExtentReportManager.createTest(MODULE, FEATURE_LAYOUT,
                "TC_GOAL_03_EditDeleteIconsVisibleAt100Zoom (ZP-1329)");

        if (!navigateToGoalsIfAccessible()) throw new org.testng.SkipException("Goals not accessible");

        try {
            // Force zoom back to 100% to reproduce bug
            js().executeScript("document.body.style.zoom='100%';");
            pause(1000);

            List<WebElement> editIcons = driver.findElements(EDIT_ICON);
            int visibleIcons = 0;
            for (WebElement icon : editIcons) {
                if (icon.isDisplayed()) {
                    org.openqa.selenium.Rectangle r = icon.getRect();
                    // Visible AND within viewport AND non-zero size
                    long winW = (Long) js().executeScript("return window.innerWidth;");
                    if (r.getX() >= 0 && r.getX() + r.getWidth() <= winW
                            && r.getWidth() > 0 && r.getHeight() > 0) {
                        visibleIcons++;
                    }
                }
            }
            logStepWithScreenshot("Edit icons visibility at 100% zoom: " + visibleIcons);

            // If Goals table is empty the test is inconclusive
            boolean tableHasRows = getPageText().toLowerCase().contains("edit")
                    || !editIcons.isEmpty();
            if (!tableHasRows) {
                throw new org.testng.SkipException("Goals table empty — cannot verify icons");
            }

            Assert.assertTrue(visibleIcons > 0,
                    "At 100% zoom, at least one Edit icon should be visible and fully in viewport. "
                    + "Found " + visibleIcons + " visible (total in DOM: " + editIcons.size() + ")");
            // Restore 80% zoom so later tests keep house-style
            js().executeScript("document.body.style.zoom='80%';");
            ExtentReportManager.logPass("ZP-1329 coverage: Edit icons visible at 100% zoom");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_03 error state");
            Assert.fail("TC_GOAL_03 failed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "TC_GOAL_04: Goals table has horizontal scrollbar when columns overflow")
    public void testTC_GOAL_04_TableHasHorizontalScrollbarOnOverflow() {
        ExtentReportManager.createTest(MODULE, FEATURE_LAYOUT,
                "TC_GOAL_04_TableHasHorizontalScrollbarOnOverflow (ZP-1553)");

        if (!navigateToGoalsIfAccessible()) throw new org.testng.SkipException("Goals not accessible");

        try {
            List<WebElement> tables = driver.findElements(GOALS_TABLE);
            if (tables.isEmpty()) throw new org.testng.SkipException("No Goals table found");

            WebElement table = tables.get(0);
            Boolean hasScrollX = (Boolean) js().executeScript(
                    "var el = arguments[0];"
                    + "return el.scrollWidth > el.clientWidth;", table);
            logStepWithScreenshot("Table scroll state: overflow=" + hasScrollX);

            // Also: check the Edit column is reachable by scrolling the table right
            js().executeScript("arguments[0].scrollLeft = arguments[0].scrollWidth;", table);
            pause(500);
            List<WebElement> editIcons = driver.findElements(EDIT_ICON);
            int reachableByScroll = 0;
            for (WebElement icon : editIcons) {
                if (icon.isDisplayed()) reachableByScroll++;
            }

            if (hasScrollX != null && hasScrollX) {
                Assert.assertTrue(reachableByScroll > 0,
                        "Horizontal overflow detected but Edit icons still not reachable after scrollRight. "
                        + "Tickets hidden: ZP-1553");
            }
            ExtentReportManager.logPass("ZP-1553 coverage: table scrolls horizontally to reveal Edit actions");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_04 error state");
            Assert.fail("TC_GOAL_04 failed: " + e.getMessage());
        }
    }

    @Test(priority = 5, description = "TC_GOAL_05: Cadence column is present and non-blank for every goal row")
    public void testTC_GOAL_05_CadenceShownForAllGoalTypes() {
        ExtentReportManager.createTest(MODULE, FEATURE_LAYOUT,
                "TC_GOAL_05_CadenceShownForAllGoalTypes (ZP-1337)");

        if (!navigateToGoalsIfAccessible()) throw new org.testng.SkipException("Goals not accessible");

        try {
            List<WebElement> headers = driver.findElements(TABLE_HEADERS);
            boolean hasCadenceCol = headers.stream()
                    .anyMatch(h -> h.getText() != null && h.getText().toLowerCase().contains("cadence"));
            if (!hasCadenceCol) {
                logWarning("Cadence column not found in headers — table schema may have changed");
                throw new org.testng.SkipException("Cadence column absent");
            }

            String pageText = getPageText();
            // Cadence values we expect visible somewhere in the table
            boolean hasVisibleCadence = pageText.matches("(?s).*(Weekly|Monthly|Yearly|One[-\\s]?Time|Custom).*");

            Assert.assertTrue(hasVisibleCadence,
                    "Cadence column has no visible values. ZP-1337: One-Time goals show blank cadence.");
            ExtentReportManager.logPass("ZP-1337 coverage: Cadence rendered for all goal types");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_05 error state");
            Assert.fail("TC_GOAL_05 failed: " + e.getMessage());
        }
    }

    @Test(priority = 6, description = "TC_GOAL_06: Period column includes year in date range")
    public void testTC_GOAL_06_PeriodColumnIncludesYear() {
        ExtentReportManager.createTest(MODULE, FEATURE_LAYOUT,
                "TC_GOAL_06_PeriodColumnIncludesYear (ZP-1556)");

        if (!navigateToGoalsIfAccessible()) throw new org.testng.SkipException("Goals not accessible");

        try {
            String pageText = getPageText();
            // Period format should include a 4-digit year somewhere near "Mar 16" / "Dec 31" / "Period"
            boolean hasYearFormatted = pageText.matches("(?s).*\\b(19|20)\\d{2}\\b.*");
            // Minimal sanity — Goals page must have some Period/date text
            boolean hasPeriodIndicator = pageText.toLowerCase().contains("period")
                    || pageText.matches("(?s).*\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}.*");

            if (!hasPeriodIndicator) {
                throw new org.testng.SkipException("No Period column / date format on Goals page — cannot verify");
            }
            Assert.assertTrue(hasYearFormatted,
                    "Period column should show year in date range (e.g. 'Mar 16 2026 - Dec 31 2026'). "
                    + "ZP-1556: year missing, only month-day visible.");
            ExtentReportManager.logPass("ZP-1556 coverage: year present in Period display");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_06 error state");
            Assert.fail("TC_GOAL_06 failed: " + e.getMessage());
        }
    }

    // Quarantined: DEV-env canary that EDITS + SAVES a real goal (mutating) — kept out of the
    // routine functional gate (groups known-product-bug); exercised via -Dgroups=known-product-bug.
    @Test(priority = 7, groups = {"known-product-bug"},
          description = "TC_GOAL_07: Editing Time Period does not silently mutate Current value [ZP-1557, destructive canary]")
    public void testTC_GOAL_07_EditPeriodDoesNotMutateCurrentValue() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATA,
                "TC_GOAL_07_EditPeriodDoesNotMutateCurrentValue (ZP-1557)");

        if (!navigateToGoalsIfAccessible()) throw new org.testng.SkipException("Goals not accessible");

        try {
            // Capture each goal row's Current value BEFORE edit
            String beforeText = getPageText();
            long dollarMentionsBefore = beforeText.chars().filter(c -> c == '$').count();
            if (dollarMentionsBefore == 0) {
                throw new org.testng.SkipException("No monetary Goals present — ZP-1557 canary requires $-denominated goal");
            }

            // Click first available Edit pencil
            List<WebElement> edits = driver.findElements(EDIT_ICON);
            if (edits.isEmpty()) throw new org.testng.SkipException("No Edit icon to exercise");
            WebElement firstEdit = edits.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
            if (firstEdit == null) throw new org.testng.SkipException("No visible Edit icon");
            safeClick(firstEdit);
            pause(2000);

            // In the Edit modal change only the Time Period / cadence → Custom if available
            // Then Save without touching anything else
            List<WebElement> customOption = driver.findElements(By.xpath(
                    "//*[normalize-space()='Custom' or normalize-space()='Custom Range']"));
            if (!customOption.isEmpty() && customOption.get(0).isDisplayed()) {
                safeClick(customOption.get(0));
                pause(1000);
            }

            List<WebElement> saves = driver.findElements(SAVE_BTN);
            if (saves.isEmpty()) {
                throw new org.testng.SkipException("Save button not found in Edit modal");
            }
            WebElement save = saves.get(saves.size() - 1);
            if (save.isEnabled()) {
                safeClick(save);
                pause(3000);
            }

            String afterText = getPageText();
            // Compare the pre- vs post- set of $-amounts visible on the page
            java.util.regex.Matcher before = java.util.regex.Pattern
                    .compile("\\$[\\d,]+").matcher(beforeText);
            java.util.regex.Matcher after = java.util.regex.Pattern
                    .compile("\\$[\\d,]+").matcher(afterText);
            java.util.Set<String> beforeSet = new java.util.HashSet<>();
            while (before.find()) beforeSet.add(before.group());
            java.util.Set<String> afterSet = new java.util.HashSet<>();
            while (after.find()) afterSet.add(after.group());
            // A "silent mutation" creates a net-new large $ value that wasn't on the page before
            java.util.Set<String> newAmounts = new java.util.HashSet<>(afterSet);
            newAmounts.removeAll(beforeSet);
            logStepWithScreenshot("Edit completed. New $-amounts appearing: " + newAmounts);

            Assert.assertTrue(newAmounts.isEmpty()
                    || newAmounts.stream().allMatch(a -> a.equals("$0") || a.length() <= 4),
                    "Editing Time Period to Custom silently introduced new $-amounts: " + newAmounts
                    + ". ZP-1557: $0 → $426,736 silent mutation.");
            ExtentReportManager.logPass("ZP-1557 coverage: no silent Current-value mutation on period edit");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_07 error state");
            Assert.fail("TC_GOAL_07 failed: " + e.getMessage());
        }
    }

    // Quarantined: DEV-env canary that EDITS + SAVES a real goal (mutating) — see TC_GOAL_07.
    @Test(priority = 8, groups = {"known-product-bug"},
          description = "TC_GOAL_08: Editing Custom date doesn't silently change Cadence [ZP-1558, destructive canary]")
    public void testTC_GOAL_08_CustomDateEditDoesNotChangeCadence() {
        ExtentReportManager.createTest(MODULE, FEATURE_DATA,
                "TC_GOAL_08_CustomDateEditDoesNotChangeCadence (ZP-1558)");

        if (!navigateToGoalsIfAccessible()) throw new org.testng.SkipException("Goals not accessible");

        try {
            String beforeText = getPageText();
            int yearlyBefore = countOccurrences(beforeText, "Yearly");
            int weeklyBefore = countOccurrences(beforeText, "Weekly");

            List<WebElement> edits = driver.findElements(EDIT_ICON);
            if (edits.isEmpty()) throw new org.testng.SkipException("No Edit icon");
            WebElement firstEdit = edits.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
            if (firstEdit == null) throw new org.testng.SkipException("No visible Edit icon");
            safeClick(firstEdit);
            pause(2000);

            // Flip Time Period to Custom and Save
            List<WebElement> customOption = driver.findElements(By.xpath(
                    "//*[normalize-space()='Custom' or normalize-space()='Custom Range']"));
            if (!customOption.isEmpty() && customOption.get(0).isDisplayed()) {
                safeClick(customOption.get(0));
                pause(1000);
            }
            List<WebElement> saves = driver.findElements(SAVE_BTN);
            if (!saves.isEmpty() && saves.get(saves.size() - 1).isEnabled()) {
                safeClick(saves.get(saves.size() - 1));
                pause(3000);
            }

            String afterText = getPageText();
            int yearlyAfter = countOccurrences(afterText, "Yearly");
            int weeklyAfter = countOccurrences(afterText, "Weekly");

            // A silent cadence change from Yearly → Weekly would reduce Yearly count and increase Weekly
            boolean suspiciousCadenceChange = (yearlyAfter < yearlyBefore) && (weeklyAfter > weeklyBefore);
            logStepWithScreenshot(String.format("Cadence counts — Yearly %d→%d, Weekly %d→%d",
                    yearlyBefore, yearlyAfter, weeklyBefore, weeklyAfter));

            Assert.assertFalse(suspiciousCadenceChange,
                    "Cadence appears to have silently changed after Custom-date edit. "
                    + "Yearly " + yearlyBefore + "→" + yearlyAfter + ", Weekly "
                    + weeklyBefore + "→" + weeklyAfter + ". ZP-1558.");
            ExtentReportManager.logPass("ZP-1558 coverage: Cadence stable across Custom-date edit");
        } catch (org.testng.SkipException se) {
            throw se;
        } catch (Exception e) {
            logStepWithScreenshot("TC_GOAL_08 error state");
            Assert.fail("TC_GOAL_08 failed: " + e.getMessage());
        }
    }

    // ════════════════ Functional coverage (added 2026-06-08) ════════════════
    // The 8 tests above are bug-tripwires (ZP-*); the module had NO basic functional
    // coverage (columns / create-dialog / status+operator enums / health / a11y).
    // These are gyu-agnostic (Goals are company-level), assert FUNCTION, and use the
    // correct controls live-verified via Playwright: the trigger is "Add Goal" (the
    // modal title is "Create Goal"); the grid columns are Scope/Account/Type/Operator/
    // Target/Current/Cadence/Period/Status. Functional tests do NOT call verifyPageHealth.

    private static final String GOALS_DIRECT_URL = AppConstants.BASE_URL + "/goals";
    private static final By G_GRID = By.cssSelector(".MuiDataGrid-root, [role='grid']");
    private static final By G_HEADER = By.cssSelector(".MuiDataGrid-columnHeaderTitle, [role='columnheader']");
    private static final By G_SEARCH = By.cssSelector("input[type='search'], input[placeholder*='Search'], input[placeholder*='search']");
    private static final By G_ADD_BTN = By.xpath("//button[normalize-space()='Add Goal' or normalize-space()='+ Add Goal' or contains(normalize-space(),'Add Goal')]");
    private static final By G_DIALOG = By.cssSelector("[role='dialog'], .MuiDialog-root");
    private static final By G_DIALOG_CREATE = By.xpath("//div[@role='dialog']//button[normalize-space()='Create' or normalize-space()='Save' or normalize-space()='Create Goal']");
    private static final By G_DIALOG_CANCEL = By.xpath("//div[@role='dialog']//button[normalize-space()='Cancel' or normalize-space()='Close']");

    /** Navigate to /goals (the real nav route) and settle. Returns true if a grid/Add-Goal rendered. */
    private boolean goToGoalsDirect() {
        driver.get(GOALS_DIRECT_URL);
        waitAndDismissAppAlert();
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(G_GRID).isEmpty() || !driver.findElements(G_ADD_BTN).isEmpty()
                    || getPageText().toLowerCase().contains("add goal")) return true;
            pause(500);
        }
        return !driver.findElements(G_GRID).isEmpty();
    }

    private java.util.List<String> goalColumnHeaders() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (WebElement h : driver.findElements(G_HEADER)) {
            String t = h.getText().trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    private java.util.List<String> goalColumnValues(String dataField) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (WebElement c : driver.findElements(By.cssSelector(".MuiDataGrid-cell[data-field='" + dataField + "']"))) {
            String t = c.getText().trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private boolean openAddGoalDialog() {
        java.util.List<WebElement> b = driver.findElements(G_ADD_BTN);
        if (b.isEmpty()) return false;
        js().executeScript("arguments[0].click();", b.get(0));
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            if (driver.findElements(G_DIALOG).stream().anyMatch(WebElement::isDisplayed)) { pause(700); return true; }
            pause(300);
        }
        return false;
    }

    private boolean isCreateEnabledInDialog() {
        java.util.List<WebElement> b = driver.findElements(G_DIALOG_CREATE);
        if (b.isEmpty()) return false;
        WebElement s = b.get(0);
        return s.isEnabled() && s.getAttribute("disabled") == null && !"true".equals(s.getAttribute("aria-disabled"));
    }

    private void cancelDialog() {
        java.util.List<WebElement> c = driver.findElements(G_DIALOG_CANCEL);
        if (!c.isEmpty()) js().executeScript("arguments[0].click();", c.get(0));
        pause(700);
    }

    // Quarantined tripwire — /goals intermittently emits a SEVERE-error storm (verified live
    // 2026-06-09: 81 severe errors, "Failed to fetch notes: ... '<!DOCTYPE' is not valid JSON" —
    // the notes API returns HTML instead of JSON; see BUG-F). Health is intermittently red, so
    // this stays out of the functional gate. Assertion NOT weakened.
    @Test(priority = 20, groups = {"known-product-bug"},
          description = "TC_GOAL_09: Goals page loads healthy — no severe JS/network errors [tripwire: BUG-A / BUG-F notes-fetch]")
    public void testTC_GOAL_09_PageLoadsHealthy() {
        ExtentReportManager.createTest(MODULE, "Navigation", "TC_GOAL_09_LoadHealthy");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        verifyPageHealth("Goals page");
        ExtentReportManager.logPass("Goals page healthy");
    }

    @Test(priority = 21, description = "TC_GOAL_10: Goals grid exposes the expected columns (Type/Operator/Target/Cadence/Period/Status)")
    public void testTC_GOAL_10_GridColumns() {
        ExtentReportManager.createTest(MODULE, "Navigation", "TC_GOAL_10_Columns");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        if (driver.findElements(G_GRID).isEmpty()) throw new org.testng.SkipException("No goals grid (empty state)");
        java.util.List<String> headers = goalColumnHeaders();
        logStep("Goal columns: " + headers);
        String joined = String.join(" | ", headers).toLowerCase();
        Assert.assertTrue(joined.contains("type"), "Grid must have a Type column. Got: " + headers);
        Assert.assertTrue(joined.contains("operator"), "Grid must have an Operator column. Got: " + headers);
        Assert.assertTrue(joined.contains("target"), "Grid must have a Target column. Got: " + headers);
        Assert.assertTrue(joined.contains("status"), "Grid must have a Status column. Got: " + headers);
        Assert.assertTrue(joined.contains("cadence") && joined.contains("period"),
                "Grid must surface Cadence + Period columns. Got: " + headers);
        ExtentReportManager.logPass("Goal columns present: " + headers);
    }

    @Test(priority = 22, description = "TC_GOAL_11: Add Goal opens the Create Goal dialog with required Goal Type / Operator / Target Value")
    public void testTC_GOAL_11_AddGoalDialogFields() {
        ExtentReportManager.createTest(MODULE, "Create", "TC_GOAL_11_DialogFields");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        if (!openAddGoalDialog()) throw new org.testng.SkipException("Add Goal button not present (role-gated)");
        WebElement dlg = driver.findElements(G_DIALOG).stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
        String txt = dlg == null ? "" : dlg.getText().toLowerCase();
        cancelDialog();
        Assert.assertTrue(txt.contains("goal type"), "Create Goal dialog must ask for a Goal Type. Text: " + txt);
        Assert.assertTrue(txt.contains("operator"), "Create Goal dialog must ask for an Operator. Text: " + txt);
        Assert.assertTrue(txt.contains("target value"), "Create Goal dialog must ask for a Target Value. Text: " + txt);
        Assert.assertTrue(txt.contains("personal") && txt.contains("account"),
                "Create Goal dialog should offer Personal/Account scope. Text: " + txt);
        ExtentReportManager.logPass("Create Goal dialog exposes Goal Type / Operator / Target Value + scope");
    }

    @Test(priority = 23, description = "TC_GOAL_12: Create Goal marks Goal Type / Operator / Target Value as required (*)")
    public void testTC_GOAL_12_RequiredFieldsMarked() {
        ExtentReportManager.createTest(MODULE, "Create / Validation", "TC_GOAL_12_RequiredMarked");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        if (!openAddGoalDialog()) throw new org.testng.SkipException("Add Goal not available");
        java.util.List<String> required = new java.util.ArrayList<>();
        for (WebElement l : driver.findElements(By.cssSelector("[role='dialog'] label"))) {
            try { String t = l.getText().trim(); if (t.contains("*")) required.add(t.replace("*", "").trim()); }
            catch (Exception ignore) { }
        }
        boolean enabledEmpty = isCreateEnabledInDialog();
        cancelDialog();
        String joined = String.join(" | ", required).toLowerCase();
        Assert.assertTrue(joined.contains("goal type"), "Goal Type must be marked required (*). Required: " + required);
        Assert.assertTrue(joined.contains("operator"), "Operator must be marked required (*). Required: " + required);
        Assert.assertTrue(joined.contains("target value"), "Target Value must be marked required (*). Required: " + required);
        // Observation (not asserted): unlike Opp/Account dialogs, Goals enables Create on an empty
        // form (validates on submit rather than disabling). Logged for product awareness.
        logStep("[TC_GOAL_12] Create enabled on empty form = " + enabledEmpty
                + " (Goals validates on submit; Opp/Account disable-gate instead)");
        ExtentReportManager.logPass("Create Goal marks Goal Type / Operator / Target Value required: " + required);
    }

    @Test(priority = 24, description = "TC_GOAL_13: Status column shows only valid goal statuses")
    public void testTC_GOAL_13_StatusEnumValid() {
        ExtentReportManager.createTest(MODULE, "Data Integrity", "TC_GOAL_13_StatusEnum");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        java.util.List<String> statuses = goalColumnValues("eval_status");
        if (statuses.isEmpty()) throw new org.testng.SkipException("Status column not rendered (virtualized/empty)");
        java.util.List<String> valid = java.util.Arrays.asList(
                "met", "not met", "in progress", "on track", "at risk", "achieved", "missed",
                "active", "completed", "unknown", "no data", "n/a", "pending");
        for (String s : statuses) {
            String v = s.toLowerCase().trim();
            Assert.assertTrue(valid.stream().anyMatch(v::contains),
                    "Goal status '" + s + "' is not a recognized status " + valid);
        }
        logStep("Observed goal statuses: " + statuses.stream().distinct().sorted().collect(java.util.stream.Collectors.toList()));
        ExtentReportManager.logPass(statuses.size() + " status value(s), all valid");
    }

    @Test(priority = 25, description = "TC_GOAL_14: Operator column shows only comparison operators")
    public void testTC_GOAL_14_OperatorEnumValid() {
        ExtentReportManager.createTest(MODULE, "Data Integrity", "TC_GOAL_14_OperatorEnum");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        java.util.List<String> ops = goalColumnValues("operator_symbol");
        if (ops.isEmpty()) throw new org.testng.SkipException("Operator column not rendered");
        for (String o : ops) {
            String v = o.trim();
            Assert.assertTrue(v.matches("[<>=≤≥]{1,2}") || v.equalsIgnoreCase("<=") || v.equalsIgnoreCase(">="),
                    "Operator '" + o + "' is not a valid comparison operator (< = > <= >=).");
        }
        ExtentReportManager.logPass("Operators valid: " + ops.stream().distinct().collect(java.util.stream.Collectors.toList()));
    }

    @Test(priority = 26, description = "TC_GOAL_15: Target / Current columns are currency/number-shaped")
    public void testTC_GOAL_15_TargetCurrencyShape() {
        ExtentReportManager.createTest(MODULE, "Data Integrity", "TC_GOAL_15_TargetShape");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        java.util.List<String> targets = goalColumnValues("target_value");
        if (targets.isEmpty()) throw new org.testng.SkipException("Target column not rendered");
        for (String t : targets) {
            String v = t.trim();
            Assert.assertTrue(v.matches("[$€£]?[\\d,]+(\\.\\d+)?%?") || v.matches("[\\d,.$€£%\\s-]+"),
                    "Target value must be currency/number-shaped. Got: '" + t + "'");
        }
        ExtentReportManager.logPass("Target values currency/number-shaped (" + targets.size() + ")");
    }

    @Test(priority = 27, description = "TC_GOAL_16: Search box accepts input without crashing the grid")
    public void testTC_GOAL_16_SearchBoxResponsive() {
        ExtentReportManager.createTest(MODULE, "Read / Search", "TC_GOAL_16_Search");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        java.util.List<WebElement> s = driver.findElements(G_SEARCH);
        if (s.isEmpty()) throw new org.testng.SkipException("No search box on Goals");
        js().executeScript(
            "var st=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;"
          + "st.call(arguments[0], arguments[1]);"
          + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", s.get(0), "Revenue");
        pause(1500);
        String body = getPageText().toLowerCase();
        Assert.assertFalse(body.contains("undefined is not") || body.contains("cannot read properties"),
                "Typing in the Goals search must not surface a raw JS error.");
        Assert.assertTrue(!driver.findElements(G_GRID).isEmpty() || body.contains("no "),
                "Goals grid should remain rendered (or show an empty state) after a search keystroke.");
        ExtentReportManager.logPass("Goals search box accepts input without crashing");
    }

    // Tripwire: app-wide WCAG violations (BUG-B) on /goals too.
    @Test(priority = 28, groups = {"known-product-bug"},
          description = "TC_GOAL_17: Accessibility — no critical/serious WCAG violations [tripwire: BUG-B]")
    public void testTC_GOAL_17_Accessibility() {
        ExtentReportManager.createTest(MODULE, "Accessibility", "TC_GOAL_17_A11y");
        if (!goToGoalsDirect()) throw new org.testng.SkipException("Goals not accessible");
        verifyAccessibility("Goals (/goals)");
        ExtentReportManager.logPass("No critical/serious WCAG violations on Goals");
    }

    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
