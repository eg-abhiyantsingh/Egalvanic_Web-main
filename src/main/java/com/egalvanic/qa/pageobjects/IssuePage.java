package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * Page Object Model for the Issues page.
 * Supports CRUD operations on issues.
 *
 * UI structure:
 *   - Card/Tile layout (NOT MUI DataGrid)
 *   - "Create Issue" button opens a side drawer/dialog
 *   - Create form has: Name, Asset (autocomplete), Priority (dropdown), Type (dropdown)
 *   - Issue detail page has: Activate Jobs button, Photo upload
 *   - Search input filters cards
 */
public class IssuePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    // ================================================================
    // LOCATORS
    // ================================================================

    // Navigation
    private static final By ISSUES_NAV = By.xpath(
            "//a[normalize-space()='Issues'] | //span[normalize-space()='Issues']");
    private static final By ASSETS_NAV = By.xpath(
            "//a[normalize-space()='Assets'] | //span[normalize-space()='Assets']");

    // Create Issue form
    private static final By CREATE_ISSUE_BTN = By.xpath(
            "//button[normalize-space()='Create Issue' or contains(normalize-space(),'Create Issue')]");
    private static final By ISSUE_FORM_DIALOG = By.xpath(
            "//*[contains(text(),'Add Issue') or contains(text(),'Create Issue') or contains(text(),'BASIC INFO') or contains(text(),'New Issue')]");
    private static final By ISSUE_NAME_INPUT = By.xpath(
            "//input[@placeholder='Enter Issue Name' or @placeholder='Enter issue name' or @placeholder='Issue Name' or @placeholder='Enter name']");
    private static final By ASSET_INPUT = By.xpath(
            "//input[@placeholder='Select Asset' or @placeholder='Select asset' or @placeholder='Search asset' or @placeholder='Select Asset...']");
    private static final By PRIORITY_INPUT = By.xpath(
            "//input[@placeholder='Select Priority' or @placeholder='Select priority' or @placeholder='Priority']");
    private static final By TYPE_INPUT = By.xpath(
            "//input[@placeholder='Select Type' or @placeholder='Select type' or @placeholder='Select Issue Type' or @placeholder='Type']");

    // Search
    private static final By SEARCH_INPUT = By.xpath(
            "//input[contains(@placeholder,'Search') or contains(@placeholder,'search')]");

    // Issue cards (tile layout — use multiple strategies)
    private static final By ISSUE_CARDS = By.xpath(
            "//div[contains(@class,'MuiCard')] | //div[contains(@class,'card')] | //div[contains(@class,'issue-card')] | //div[contains(@class,'tile')]");

    // Issue detail page
    private static final By ACTIVATE_JOBS_BTN = By.xpath(
            "//button[normalize-space()='Activate Jobs' or contains(normalize-space(),'Activate Job') or contains(normalize-space(),'Activate')]");
    private static final By JOBS_ACTIVATED_INDICATOR = By.xpath(
            "//*[contains(text(),'Job activated') or contains(text(),'Jobs activated') or contains(text(),'Activated') or contains(@class,'activated')]");

    // Photo upload
    private static final By PHOTO_UPLOAD_INPUT = By.xpath("//input[@type='file']");
    private static final By PHOTOS_TAB = By.xpath(
            "//button[normalize-space()='Photos'] | //*[contains(@class,'MuiTab')][contains(normalize-space(),'Photos')]");
    private static final By PHOTO_THUMBNAILS = By.xpath(
            "//img[contains(@class,'thumbnail') or contains(@class,'photo') or contains(@src,'blob:') or contains(@src,'upload')]");

    // Delete
    private static final By DELETE_ISSUE_BTN = By.xpath(
            "//button[normalize-space()='Delete Issue' or normalize-space()='Delete']");
    private static final By CONFIRM_DELETE_BTN = By.xpath(
            "//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");

    // Submit
    private static final By SUBMIT_CREATE_BTN = By.xpath(
            "//button[normalize-space()='Create Issue' or normalize-space()='Create' or normalize-space()='Save']");

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public IssuePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /**
     * Navigate to the Issues page via sidebar.
     * If already on Issues, navigate away and back for fresh data.
     */
    public void navigateToIssues() {
        dismissAnyDrawerOrBackdrop();

        if (isOnIssuesPage()) {
            System.out.println("[IssuePage] Already on Issues — navigating away and back");
            try {
                js.executeScript(
                    "var links = document.querySelectorAll('a');" +
                    "for (var el of links) {" +
                    "  if (el.textContent.trim() === 'Assets') { el.click(); return; }" +
                    "}");
                pause(1500);
            } catch (Exception e) {
                System.out.println("[IssuePage] Nav away failed: " + e.getMessage());
            }
        }

        // Click Issues in sidebar
        js.executeScript(
            "var links = document.querySelectorAll('a');" +
            "for (var el of links) {" +
            "  if (el.textContent.trim() === 'Issues') { el.click(); return; }" +
            "}"
        );
        pause(2000);

        waitForSpinner();
        pause(1000);
        System.out.println("[IssuePage] On issues page: " + driver.getCurrentUrl());
    }

    public boolean isOnIssuesPage() {
        return driver.getCurrentUrl().contains("/issues");
    }

    // ================================================================
    // CREATE
    // ================================================================

    /**
     * Open the Create Issue form (drawer/dialog).
     */
    public void openCreateIssueForm() {
        dismissAnyDialog();
        pause(300);

        // Click the "Create Issue" button in page header
        js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Create Issue' || text.includes('Create Issue')) {" +
            "    var r = b.getBoundingClientRect();" +
            "    if (r.width > 0 && r.top < 300) { b.click(); return; }" +
            "  }" +
            "}"
        );
        pause(2000);

        // Wait for form to appear
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(ISSUE_FORM_DIALOG));
        } catch (Exception e) {
            System.out.println("[IssuePage] Form dialog not found with primary selector, trying name input");
            wait.until(ExpectedConditions.visibilityOfElementLocated(ISSUE_NAME_INPUT));
        }
        System.out.println("[IssuePage] Create Issue form opened");
    }

    /**
     * Fill the issue name field.
     */
    public void fillIssueName(String name) {
        typeField(ISSUE_NAME_INPUT, name);
        System.out.println("[IssuePage] Filled issue name: " + name);
    }

    /**
     * Select an asset from the autocomplete dropdown.
     */
    public void selectAsset(String assetName) {
        typeAndSelectDropdown(ASSET_INPUT, assetName, assetName);
        System.out.println("[IssuePage] Selected asset: " + assetName);
    }

    /**
     * Select priority from the dropdown.
     */
    public void selectPriority(String priority) {
        typeAndSelectDropdown(PRIORITY_INPUT, priority, priority);
        System.out.println("[IssuePage] Selected priority: " + priority);
    }

    /**
     * Select issue type from the dropdown.
     */
    public void selectType(String issueType) {
        typeAndSelectDropdown(TYPE_INPUT, issueType, issueType);
        System.out.println("[IssuePage] Selected type: " + issueType);
    }

    /**
     * Submit the Create Issue form.
     */
    public void submitCreateIssue() {
        // Find submit button inside the drawer (not the page-level Create Issue button)
        js.executeScript(
            "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"], [role=\"presentation\"]');" +
            "for (var d of drawers) {" +
            "  var btns = d.querySelectorAll('button');" +
            "  for (var b of btns) {" +
            "    var text = b.textContent.trim();" +
            "    if ((text === 'Create Issue' || text === 'Create' || text === 'Save') && b.getBoundingClientRect().width > 0) {" +
            "      b.scrollIntoView({block:'center'});" +
            "      b.click();" +
            "      return;" +
            "    }" +
            "  }" +
            "}"
        );
        System.out.println("[IssuePage] Clicked submit button");
    }

    /**
     * Wait for issue creation to succeed (drawer closes or success toast).
     */
    public boolean waitForCreateSuccess() {
        By panelHeader = By.xpath("//*[normalize-space()='Add Issue' or normalize-space()='Create Issue' or normalize-space()='New Issue']");

        for (int i = 0; i < 25; i++) {
            // Success indicator (toast message)
            try {
                List<WebElement> success = driver.findElements(By.xpath(
                    "//*[contains(text(),'created') or contains(text(),'Created') or contains(text(),'success')]"));
                if (!success.isEmpty()) {
                    System.out.println("[IssuePage] Success indicator found");
                    closeDrawer();
                    return true;
                }
            } catch (Exception ignored) {}

            // Panel closed = form submitted successfully
            if (driver.findElements(panelHeader).isEmpty()) {
                System.out.println("[IssuePage] Create panel closed — creation successful");
                return true;
            }
            pause(1000);
        }

        System.out.println("[IssuePage] Create panel still open after 25s — creation may have failed");
        closeDrawer();
        return false;
    }

    // ================================================================
    // CARDS / READ
    // ================================================================

    /**
     * Get the count of visible issue cards.
     */
    public int getCardCount() {
        try {
            // Use JS to find cards — try multiple selectors
            Long count = (Long) js.executeScript(
                "var cards = document.querySelectorAll('[class*=\"MuiCard\"], [class*=\"card\"], [class*=\"issue-card\"], [class*=\"tile\"]');" +
                "var visible = 0;" +
                "for (var c of cards) {" +
                "  var r = c.getBoundingClientRect();" +
                "  if (r.width > 50 && r.height > 50) visible++;" +
                "}" +
                "return visible;"
            );
            int result = count != null ? count.intValue() : 0;
            System.out.println("[IssuePage] Card count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error counting cards: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if the issues page has any cards populated.
     */
    public boolean isCardsPopulated() {
        for (int i = 0; i < 10; i++) {
            if (getCardCount() > 0) return true;
            pause(1000);
        }
        return false;
    }

    /**
     * Get the title/name from the first issue card.
     */
    public String getFirstCardTitle() {
        try {
            String title = (String) js.executeScript(
                "var cards = document.querySelectorAll('[class*=\"MuiCard\"], [class*=\"card\"], [class*=\"issue-card\"], [class*=\"tile\"]');" +
                "for (var c of cards) {" +
                "  var r = c.getBoundingClientRect();" +
                "  if (r.width > 50 && r.height > 50) {" +
                "    var heading = c.querySelector('h1, h2, h3, h4, h5, h6, [class*=\"title\"], [class*=\"Title\"], [class*=\"heading\"], [class*=\"name\"], [class*=\"Name\"]');" +
                "    if (heading) return heading.textContent.trim();" +
                "    var texts = c.querySelectorAll('p, span, div');" +
                "    for (var t of texts) {" +
                "      var txt = t.textContent.trim();" +
                "      if (txt.length > 3 && txt.length < 100) return txt;" +
                "    }" +
                "  }" +
                "}" +
                "return null;"
            );
            System.out.println("[IssuePage] First card title: " + title);
            return title;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error getting first card title: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if an issue with the given name is visible on the page.
     */
    public boolean isIssueVisible(String issueName) {
        try {
            Boolean found = (Boolean) js.executeScript(
                "var cards = document.querySelectorAll('[class*=\"MuiCard\"], [class*=\"card\"], [class*=\"issue-card\"], [class*=\"tile\"]');" +
                "for (var c of cards) {" +
                "  if (c.textContent.indexOf(arguments[0]) > -1) return true;" +
                "}" +
                // Also check plain page content as fallback
                "return document.body.textContent.indexOf(arguments[0]) > -1;",
                issueName);
            boolean result = Boolean.TRUE.equals(found);
            System.out.println("[IssuePage] Issue '" + issueName + "' visible: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error checking issue visibility: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SEARCH / FILTER / SORT
    // ================================================================

    /**
     * Search for issues by typing in the search input.
     */
    public void searchIssues(String query) {
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
            // Use JS to clear and type (avoids backdrop interception)
            js.executeScript(
                "var el = arguments[0];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(el, '');" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "setter.call(el, arguments[1]);" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "el.dispatchEvent(new Event('change', {bubbles: true}));",
                searchInput, query);
            pause(1500);
            System.out.println("[IssuePage] Searched for: " + query);
        } catch (Exception e) {
            System.out.println("[IssuePage] Search failed: " + e.getMessage());
        }
    }

    /**
     * Clear the search input.
     */
    public void clearSearch() {
        try {
            WebElement searchInput = driver.findElement(SEARCH_INPUT);
            js.executeScript(
                "var el = arguments[0];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(el, '');" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "el.dispatchEvent(new Event('change', {bubbles: true}));",
                searchInput);
            pause(1500);
            System.out.println("[IssuePage] Search cleared");
        } catch (Exception e) {
            System.out.println("[IssuePage] Clear search failed: " + e.getMessage());
        }
    }

    /**
     * Click a sort option (e.g., "Name", "Date", "Priority").
     * Sort UI varies — try common patterns: dropdown, button group, column headers.
     */
    public void clickSortOption(String sortLabel) {
        Boolean sorted = (Boolean) js.executeScript(
            "var sortLabel = arguments[0];" +
            // Strategy 1: Sort dropdown/select
            "var sortBtns = document.querySelectorAll('[class*=\"sort\"], [class*=\"Sort\"], [aria-label*=\"sort\"], [aria-label*=\"Sort\"]');" +
            "for (var b of sortBtns) {" +
            "  if (b.tagName === 'BUTTON' || b.tagName === 'SELECT') { b.click(); break; }" +
            "}" +
            // Strategy 2: Find option matching label
            "var opts = document.querySelectorAll('li[role=\"option\"], li[role=\"menuitem\"], [class*=\"MenuItem\"]');" +
            "for (var o of opts) {" +
            "  if (o.textContent.trim().includes(sortLabel)) { o.click(); return true; }" +
            "}" +
            // Strategy 3: Column header click
            "var headers = document.querySelectorAll('th, [role=\"columnheader\"], [class*=\"sortable\"]');" +
            "for (var h of headers) {" +
            "  if (h.textContent.trim().includes(sortLabel)) { h.click(); return true; }" +
            "}" +
            "return false;",
            sortLabel);
        System.out.println("[IssuePage] Sort by '" + sortLabel + "': " + (Boolean.TRUE.equals(sorted) ? "applied" : "not found"));
    }

    /**
     * Click a filter option (e.g., "High", "Open", etc.).
     */
    public void clickFilterOption(String filterLabel) {
        Boolean filtered = (Boolean) js.executeScript(
            "var label = arguments[0];" +
            // Strategy 1: Filter buttons/chips
            "var chips = document.querySelectorAll('[class*=\"MuiChip\"], [class*=\"filter\"], [class*=\"Filter\"], button');" +
            "for (var c of chips) {" +
            "  if (c.textContent.trim() === label) { c.click(); return true; }" +
            "}" +
            // Strategy 2: Dropdown menu items
            "var items = document.querySelectorAll('li[role=\"option\"], li[role=\"menuitem\"]');" +
            "for (var i of items) {" +
            "  if (i.textContent.trim() === label) { i.click(); return true; }" +
            "}" +
            "return false;",
            filterLabel);
        pause(1000);
        System.out.println("[IssuePage] Filter '" + filterLabel + "': " + (Boolean.TRUE.equals(filtered) ? "applied" : "not found"));
    }

    // ================================================================
    // DETAIL PAGE / ACTIVATE JOBS
    // ================================================================

    /**
     * Click on the first issue card to open its detail page.
     */
    public void openFirstIssueDetail() {
        js.executeScript(
            "var cards = document.querySelectorAll('[class*=\"MuiCard\"], [class*=\"card\"], [class*=\"issue-card\"], [class*=\"tile\"]');" +
            "for (var c of cards) {" +
            "  var r = c.getBoundingClientRect();" +
            "  if (r.width > 50 && r.height > 50) {" +
            // Try clicking a link inside the card first (SPA navigation)
            "    var link = c.querySelector('a');" +
            "    if (link) { link.click(); return; }" +
            // Otherwise click the card itself
            "    c.click(); return;" +
            "  }" +
            "}"
        );
        pause(3000);
        waitForDetailPageLoad();
        System.out.println("[IssuePage] Opened issue detail: " + driver.getCurrentUrl());
    }

    /**
     * Click on a specific issue card by name to open its detail page.
     */
    public void openIssueDetail(String issueName) {
        js.executeScript(
            "var cards = document.querySelectorAll('[class*=\"MuiCard\"], [class*=\"card\"], [class*=\"issue-card\"], [class*=\"tile\"]');" +
            "for (var c of cards) {" +
            "  if (c.textContent.indexOf(arguments[0]) > -1) {" +
            "    var link = c.querySelector('a');" +
            "    if (link) { link.click(); return; }" +
            "    c.click(); return;" +
            "  }" +
            "}",
            issueName);
        pause(3000);
        waitForDetailPageLoad();
        System.out.println("[IssuePage] Opened issue detail for: " + issueName);
    }

    /**
     * Wait for the issue detail page to fully load.
     */
    public void waitForDetailPageLoad() {
        for (int i = 0; i < 16; i++) {
            try {
                Long mainElements = (Long) js.executeScript(
                    "return document.querySelectorAll('main *, [class*=\"detail\"] *, [class*=\"content\"] *').length;");
                if (mainElements != null && mainElements > 10) {
                    System.out.println("[IssuePage] Detail page loaded after " + (i + 1) + "s — " + mainElements + " elements");
                    return;
                }
            } catch (Exception ignored) {}
            pause(2000);
        }
        System.out.println("[IssuePage] Detail page may not have fully loaded after 32s");
    }

    /**
     * Click the "Activate Jobs" button on the issue detail page.
     */
    public void clickActivateJobs() {
        try {
            // Try finding the button directly
            Boolean clicked = (Boolean) js.executeScript(
                "var btns = document.querySelectorAll('button');" +
                "for (var b of btns) {" +
                "  var text = b.textContent.trim();" +
                "  if (text.includes('Activate') && (text.includes('Job') || text.includes('job'))) {" +
                "    b.scrollIntoView({block:'center'});" +
                "    b.click();" +
                "    return true;" +
                "  }" +
                "}" +
                "return false;"
            );
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[IssuePage] Clicked Activate Jobs button");
                pause(3000);
                return;
            }
        } catch (Exception e) {
            System.out.println("[IssuePage] JS click failed: " + e.getMessage());
        }

        // Fallback: use Selenium locator
        click(ACTIVATE_JOBS_BTN);
        pause(3000);
        System.out.println("[IssuePage] Clicked Activate Jobs (fallback)");
    }

    /**
     * Verify that jobs have been activated.
     * Checks for: button text change, success toast, or status indicator.
     */
    public boolean isJobActivated() {
        for (int i = 0; i < 10; i++) {
            try {
                // Check for success toast or indicator
                if (!driver.findElements(JOBS_ACTIVATED_INDICATOR).isEmpty()) {
                    System.out.println("[IssuePage] Job activation indicator found");
                    return true;
                }

                // Check if Activate Jobs button text changed or button disappeared
                Boolean buttonGone = (Boolean) js.executeScript(
                    "var btns = document.querySelectorAll('button');" +
                    "for (var b of btns) {" +
                    "  var text = b.textContent.trim();" +
                    "  if (text.includes('Activate') && text.includes('Job')) return false;" +
                    "}" +
                    "return true;");
                if (Boolean.TRUE.equals(buttonGone)) {
                    System.out.println("[IssuePage] Activate Jobs button no longer visible — jobs activated");
                    return true;
                }

                // Check for any success toast
                Boolean hasToast = (Boolean) js.executeScript(
                    "return document.body.textContent.includes('activated') || " +
                    "document.body.textContent.includes('Activated') || " +
                    "document.body.textContent.includes('success') || " +
                    "document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"toast\"], [class*=\"alert-success\"]').length > 0;");
                if (Boolean.TRUE.equals(hasToast)) {
                    System.out.println("[IssuePage] Success toast detected — jobs activated");
                    return true;
                }
            } catch (Exception ignored) {}
            pause(1000);
        }
        System.out.println("[IssuePage] Job activation not confirmed after 10s");
        return false;
    }

    // ================================================================
    // PHOTOS
    // ================================================================

    /**
     * Navigate to the Photos section/tab on the issue detail page.
     */
    public void navigateToPhotosSection() {
        try {
            // Click Photos tab if it exists
            Boolean clicked = (Boolean) js.executeScript(
                "var tabs = document.querySelectorAll('[class*=\"MuiTab\"], [role=\"tab\"], button');" +
                "for (var t of tabs) {" +
                "  if (t.textContent.trim() === 'Photos') { t.click(); return true; }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[IssuePage] Clicked Photos tab");
                pause(1000);
            }
        } catch (Exception e) {
            System.out.println("[IssuePage] Photos tab not found: " + e.getMessage());
        }
    }

    /**
     * Upload a photo to the current issue.
     */
    public void uploadPhoto(String filePath) {
        navigateToPhotosSection();

        String absolutePath = new File(filePath).getAbsolutePath();
        System.out.println("[IssuePage] Uploading photo from: " + absolutePath);

        try {
            // Make hidden file input visible and accessible
            js.executeScript(
                "var inputs = document.querySelectorAll('input[type=\"file\"]');" +
                "for (var input of inputs) {" +
                "  input.style.display = 'block';" +
                "  input.style.visibility = 'visible';" +
                "  input.style.opacity = '1';" +
                "  input.style.width = '200px';" +
                "  input.style.height = '50px';" +
                "  input.style.position = 'relative';" +
                "}");
            pause(500);

            // Find file input and send the file path
            List<WebElement> fileInputs = driver.findElements(PHOTO_UPLOAD_INPUT);
            if (!fileInputs.isEmpty()) {
                fileInputs.get(0).sendKeys(absolutePath);
                System.out.println("[IssuePage] Photo file sent to file input");
                pause(3000); // Wait for upload to process
            } else {
                // Try clicking an upload button to trigger file dialog
                js.executeScript(
                    "var btns = document.querySelectorAll('button');" +
                    "for (var b of btns) {" +
                    "  var text = b.textContent.trim().toLowerCase();" +
                    "  if (text.includes('upload') || text.includes('photo') || text.includes('add photo')) {" +
                    "    b.click(); return;" +
                    "  }" +
                    "}");
                pause(1000);
                // Retry finding file input
                fileInputs = driver.findElements(PHOTO_UPLOAD_INPUT);
                if (!fileInputs.isEmpty()) {
                    fileInputs.get(0).sendKeys(absolutePath);
                    System.out.println("[IssuePage] Photo file sent (after clicking upload button)");
                    pause(3000);
                } else {
                    System.out.println("[IssuePage] WARNING: No file input found for photo upload");
                }
            }
        } catch (Exception e) {
            System.out.println("[IssuePage] Photo upload error: " + e.getMessage());
        }
    }

    /**
     * Get the number of photos/thumbnails visible.
     */
    public int getPhotoCount() {
        try {
            Long count = (Long) js.executeScript(
                "var imgs = document.querySelectorAll('img[class*=\"thumbnail\"], img[class*=\"photo\"], img[src*=\"blob:\"], img[src*=\"upload\"], [class*=\"photo-item\"], [class*=\"gallery\"] img');" +
                "var visible = 0;" +
                "for (var img of imgs) {" +
                "  var r = img.getBoundingClientRect();" +
                "  if (r.width > 20 && r.height > 20) visible++;" +
                "}" +
                "return visible;");
            int result = count != null ? count.intValue() : 0;
            System.out.println("[IssuePage] Photo count: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[IssuePage] Error counting photos: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if at least one photo is visible.
     */
    public boolean isPhotoVisible() {
        for (int i = 0; i < 10; i++) {
            if (getPhotoCount() > 0) return true;
            pause(1000);
        }
        return false;
    }

    // ================================================================
    // DELETE
    // ================================================================

    /**
     * Delete the current issue from its detail page via kebab menu or delete button.
     */
    public void deleteCurrentIssue() {
        // Strategy 1: Look for a direct Delete button
        Boolean clicked = (Boolean) js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var b of btns) {" +
            "  var text = b.textContent.trim();" +
            "  if (text === 'Delete Issue' || text === 'Delete') {" +
            "    b.click(); return true;" +
            "  }" +
            "}" +
            "return false;");

        if (!Boolean.TRUE.equals(clicked)) {
            // Strategy 2: Kebab menu (small icon buttons in header)
            js.executeScript(
                "var iconBtns = document.querySelectorAll('[class*=\"MuiIconButton\"]');" +
                "for (var b of iconBtns) {" +
                "  var r = b.getBoundingClientRect();" +
                "  if (r.width > 20 && r.width < 50 && r.top < 200) {" +
                "    b.click(); break;" +
                "  }" +
                "}");
            pause(500);

            // Click Delete in menu
            js.executeScript(
                "var items = document.querySelectorAll('li[role=\"menuitem\"], [class*=\"MuiMenuItem\"]');" +
                "for (var i of items) {" +
                "  if (i.textContent.trim().includes('Delete')) { i.click(); return; }" +
                "}");
        }
        pause(1000);
        System.out.println("[IssuePage] Delete initiated");
    }

    /**
     * Confirm the delete dialog.
     */
    public void confirmDelete() {
        for (int i = 0; i < 10; i++) {
            Boolean clicked = (Boolean) js.executeScript(
                "var errorBtns = document.querySelectorAll('button[class*=\"containedError\"]');" +
                "for (var b of errorBtns) {" +
                "  var r = b.getBoundingClientRect();" +
                "  if (r.width > 0 && b.textContent.trim() === 'Delete') {" +
                "    b.dispatchEvent(new MouseEvent('mousedown', {bubbles: true}));" +
                "    b.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}));" +
                "    b.dispatchEvent(new MouseEvent('click', {bubbles: true}));" +
                "    return true;" +
                "  }" +
                "}" +
                // Fallback: dialog Delete button
                "var dialogs = document.querySelectorAll('[role=\"dialog\"], [class*=\"MuiDialog-paper\"]');" +
                "for (var d of dialogs) {" +
                "  var text = (d.textContent||'').toLowerCase();" +
                "  if (text.includes('sure') || text.includes('confirm') || text.includes('delete')) {" +
                "    var btns = d.querySelectorAll('button');" +
                "    for (var b of btns) {" +
                "      if (b.textContent.trim() === 'Delete') { b.click(); return true; }" +
                "    }" +
                "  }" +
                "}" +
                "return false;");
            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("[IssuePage] Delete confirmed");
                break;
            }
            pause(500);
        }
        pause(3000);
    }

    /**
     * Wait for the delete to complete (redirect to issues list or card removal).
     */
    public boolean waitForDeleteSuccess() {
        for (int i = 0; i < 15; i++) {
            // If redirected back to issues list
            if (driver.getCurrentUrl().matches(".*/issues/?$")) {
                System.out.println("[IssuePage] Delete success — redirected to issues list");
                return true;
            }
            // Check for success toast
            Boolean hasToast = (Boolean) js.executeScript(
                "return document.body.textContent.includes('deleted') || " +
                "document.body.textContent.includes('Deleted') || " +
                "document.querySelectorAll('[class*=\"Snackbar\"], [class*=\"toast\"]').length > 0;");
            if (Boolean.TRUE.equals(hasToast)) {
                System.out.println("[IssuePage] Delete success — toast detected");
                return true;
            }
            pause(1000);
        }
        System.out.println("[IssuePage] Delete success not confirmed after 15s");
        return false;
    }

    // ================================================================
    // CLOSE / DISMISS
    // ================================================================

    /**
     * Close any open drawer or side panel.
     */
    public void closeDrawer() {
        try {
            js.executeScript(
                "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"], [class*=\"MuiDialog-paper\"], [role=\"dialog\"]');" +
                "for (var d of drawers) {" +
                "  var closeBtn = d.querySelector('[aria-label=\"Close\"], [aria-label=\"close\"], button[class*=\"close\"]');" +
                "  if (closeBtn) { closeBtn.click(); return; }" +
                "  var cancelBtns = d.querySelectorAll('button');" +
                "  for (var b of cancelBtns) {" +
                "    if (b.textContent.trim() === 'Cancel') { b.click(); return; }" +
                "  }" +
                "}");
            pause(500);
        } catch (Exception ignored) {}
    }

    /**
     * Dismiss any open MUI Drawer, Backdrop, or side panel.
     */
    private void dismissAnyDrawerOrBackdrop() {
        try {
            Boolean hasBackdrop = (Boolean) js.executeScript(
                "return document.querySelectorAll('.MuiBackdrop-root, .MuiDrawer-root, [role=\"presentation\"]').length > 0;");
            if (Boolean.TRUE.equals(hasBackdrop)) {
                System.out.println("[IssuePage] Backdrop/drawer detected — pressing Escape to dismiss");
                new Actions(driver).sendKeys(Keys.ESCAPE).perform();
                pause(800);
                // Press Escape again for nested overlays
                Boolean stillPresent = (Boolean) js.executeScript(
                    "return document.querySelectorAll('.MuiBackdrop-root').length > 0;");
                if (Boolean.TRUE.equals(stillPresent)) {
                    new Actions(driver).sendKeys(Keys.ESCAPE).perform();
                    pause(800);
                }
            }
        } catch (Exception e) {
            System.out.println("[IssuePage] dismissAnyDrawerOrBackdrop: " + e.getMessage());
        }
    }

    /**
     * Dismiss any open dialog (role=dialog).
     */
    public void dismissAnyDialog() {
        try {
            js.executeScript(
                "var dialogs = document.querySelectorAll('[role=\"dialog\"]');" +
                "for (var d of dialogs) {" +
                "  var closeBtn = d.querySelector('[aria-label=\"Close\"], [aria-label=\"close\"]');" +
                "  if (closeBtn) { closeBtn.click(); return; }" +
                "  var cancelBtns = d.querySelectorAll('button');" +
                "  for (var b of cancelBtns) {" +
                "    if (b.textContent.trim() === 'Cancel') { b.click(); return; }" +
                "  }" +
                "}");
            pause(500);
        } catch (Exception ignored) {}
    }

    // ================================================================
    // HELPERS (PRIVATE)
    // ================================================================

    private void typeField(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        js.executeScript(
            "arguments[0].scrollIntoView({block:'center'});" +
            "arguments[0].focus();" +
            "arguments[0].click();" +
            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "setter.call(arguments[0], '');" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
            el);
        pause(100);
        try {
            el.sendKeys(text);
        } catch (Exception e) {
            System.out.println("[IssuePage] Native sendKeys blocked, using JS fallback for: " + by);
            js.executeScript(
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(arguments[0], arguments[1]);" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                el, text);
        }
    }

    private void typeAndSelectDropdown(By inputLocator, String textToType, String optionText) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));

        js.executeScript(
            "arguments[0].scrollIntoView({block:'center'});" +
            "arguments[0].focus();" +
            "arguments[0].click();", input);
        pause(300);

        js.executeScript(
            "var input = arguments[0];" +
            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "setter.call(input, '');" +
            "input.dispatchEvent(new Event('input', {bubbles: true}));" +
            "input.dispatchEvent(new Event('change', {bubbles: true}));",
            input);
        pause(200);

        sendKeysWithJsFallback(input, textToType, inputLocator);
        pause(800);

        // Wait for the listbox dropdown to appear
        By listbox = By.xpath("//ul[@role='listbox']");
        for (int attempt = 0; attempt < 5; attempt++) {
            if (!driver.findElements(listbox).isEmpty()) break;

            if (attempt == 1) {
                try {
                    WebElement popup = driver.findElement(
                            By.xpath("//button[contains(@class,'MuiAutocomplete-popupIndicator')]"));
                    js.executeScript("arguments[0].click();", popup);
                    pause(500);
                    continue;
                } catch (Exception ignored) {}
            }

            js.executeScript(
                "var input = arguments[0];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(input, '');" +
                "input.dispatchEvent(new Event('input', {bubbles: true}));" +
                "input.focus(); input.click();",
                input);
            pause(300);
            sendKeysWithJsFallback(input, textToType, inputLocator);
            pause(800);
        }

        System.out.println("[IssuePage] Listbox visible: " + !driver.findElements(listbox).isEmpty());

        // Find and click the matching option
        By exactOption = By.xpath("//li[@role='option'][normalize-space()='" + optionText + "']");
        By partialOption = By.xpath("//li[@role='option'][contains(normalize-space(),'" + optionText + "')]");
        By anyOption = By.xpath("//li[contains(@id,'option') or @role='option'][contains(normalize-space(),'" + optionText + "')]");

        for (int attempt = 0; attempt < 5; attempt++) {
            for (By opt : new By[]{exactOption, partialOption, anyOption}) {
                if (!driver.findElements(opt).isEmpty()) {
                    WebElement option = driver.findElement(opt);
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                    pause(150);
                    js.executeScript("arguments[0].click();", option);
                    pause(300);
                    System.out.println("[IssuePage] Selected dropdown option: " + optionText);
                    return;
                }
            }
            pause(400);
        }
        System.out.println("[IssuePage] WARNING: Could not select dropdown option '" + optionText + "'");
    }

    private void sendKeysWithJsFallback(WebElement el, String text, By locator) {
        try {
            el.sendKeys(text);
        } catch (Exception e) {
            System.out.println("[IssuePage] Native sendKeys blocked, using JS fallback for: " + locator);
            js.executeScript(
                "var el = arguments[0];" +
                "var text = arguments[1];" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(el, text);" +
                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                "el.dispatchEvent(new Event('change', {bubbles: true}));" +
                "for (var i = 0; i < text.length; i++) {" +
                "  el.dispatchEvent(new KeyboardEvent('keydown', {key: text[i], bubbles: true}));" +
                "  el.dispatchEvent(new KeyboardEvent('keyup', {key: text[i], bubbles: true}));" +
                "}",
                el, text);
        }
    }

    private void click(By by) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(by)).click();
        } catch (Exception e) {
            try {
                WebElement el = driver.findElement(by);
                js.executeScript("arguments[0].click();", el);
            } catch (Exception ex) {
                throw new RuntimeException("Click failed for: " + by, ex);
            }
        }
    }

    private void waitForSpinner() {
        for (int i = 0; i < 15; i++) {
            Boolean spinning = (Boolean) js.executeScript(
                "return document.querySelectorAll('[class*=\"MuiCircularProgress\"], [class*=\"spinner\"], [class*=\"loading\"]').length > 0;");
            if (!Boolean.TRUE.equals(spinning)) return;
            pause(1000);
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
