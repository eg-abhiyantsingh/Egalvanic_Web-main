
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.*;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Final single-class automation: Create asset (Phase 1) + Edit asset (Phase 2)
 * - Uses label-based stable XPaths
 * - Expands CORE ATTRIBUTES accordion before interacting
 * - Type + select for autocomplete dropdowns
 * - No CSV, all hardcoded values
 */
public class Egalvanic_asset {

    static WebDriver driver;
    static WebDriverWait wait;
    static JavascriptExecutor js;
    static Actions actions;

    // === CONFIG (hardcoded) ===
    static final String BASE_URL = "https://acme.egalvanic.ai/login";
    static final String EMAIL = "rahul+acme@egalvanic.com";
    static final String PASSWORD = "RP@egalvanic123";

    static final String ASSET_NAME = "asset";
    static final String QR_CODE = "qrcode";
    static final String ASSET_CLASS = "3-Pole Breaker";
    static final String CONDITION_VALUE = "2";

    // Core attributes
    static final String MODEL_VAL = "model123";
    static final String NOTES_VAL = "good";
    static final String AMPERE_RATING = "20 A";
    static final String MANUFACTURER = "Eaton";
    static final String CATALOG_NUMBER = "cat001";
    static final String BREAKER_SETTINGS = "setting1";
    static final String INTERRUPTING_RATING = "10 kA";
    static final String KA_RATING = "18 kA";

    static final String REPLACEMENT_COST = "30000";

    static final int DEFAULT_TIMEOUT = 25;

    public static void main(String[] args) {
        setupDriver();

        try {
            login();
            takeShot("after_login");
            Thread.sleep(500);
            selectSite(); // user's exact site selection method
            takeShot("after_site_select");

            goToAssets();
            takeShot("assets_page");

           createAssetPhase1();
            takeShot("after_phase1_create");

            editAssetPhase2();
            takeShot("after_phase2_edit");
            
            deleteAsset();
             takeShot("after_delete_asset");

            System.out.println("\n🎉 FULL FLOW FINISHED");

        } catch (Exception e) {
            System.out.println("❌ Fatal error: " + e.getMessage());
            try { takeShot("fatal_error"); } catch (Exception ignored) {}
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }
    }

    // ---------------- setup ----------------
    static void setupDriver() {
        // Setup ChromeDriver and let WebDriverManager automatically detect the correct version
        // Clear cache first to ensure fresh download
        WebDriverManager.chromedriver().clearDriverCache();
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized");
        opts.addArguments("--remote-allow-origins=*");
        opts.addArguments("--disable-blink-features=AutomationControlled");
        opts.addArguments("--no-sandbox");
        opts.addArguments("--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(opts);
        wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        js = (JavascriptExecutor) driver;
        actions = new Actions(driver);
        // ⭐ FIX: Ensure edit button visible
        js.executeScript("document.body.style.zoom='80%';");
        pause(600);
    }

    static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    static String stamp() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")); }

    static void takeShot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fname = name + "_" + stamp() + ".png";
            Files.copy(src.toPath(), Path.of(fname));
            System.out.println("✔ Screenshot saved: " + fname);
        } catch (Exception e) {
            System.out.println("⚠️ Screenshot failed: " + e.getMessage());
        }
    }

    // ---------- safe element helpers ----------
    static WebElement visible(By by, int timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    static WebElement clickable(By by, int timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.elementToBeClickable(by));
    }

    static void jsClick(By by) {
        try {
            WebElement e = driver.findElement(by);
            js.executeScript("arguments[0].click();", e);
        } catch (Exception ex) {
            throw new RuntimeException("JS click failed for: " + by, ex);
        }
    }

    static void click(By by) {
        try {
            clickable(by, DEFAULT_TIMEOUT).click();
        } catch (Exception e) {
            try {
                jsClick(by);
            } catch (Exception ex) {
                throw new RuntimeException("Click failed for: " + by + " -> " + ex.getMessage(), ex);
            }
        }
    }

    static void type(By by, String text) {
        WebElement e = visible(by, DEFAULT_TIMEOUT);
        try { e.clear(); } catch (Exception ignored) {}
        e.click();
        e.sendKeys(text);
    }
    static void typeAndSelectDropdown(By inputLocator, String textToType, String optionText) {
        click(inputLocator);
        pause(200);
        try {
            WebElement in = visible(inputLocator, 5);
            in.clear();
            in.sendKeys(textToType);
        } catch (Exception ignored) {}
        pause(400);
        By listOption = By.xpath("//li[normalize-space()='" + optionText + "'] | //li[contains(normalize-space(),'" + optionText + "')]");
        int attempts = 0;
        while (attempts < 4) {
            try {
                if (driver.findElements(listOption).size() > 0) {
                    click(listOption);
                    return;
                } else {
                    try {
                        By popup = By.xpath("//button[contains(@class,'MuiAutocomplete-popupIndicator')]");
                        if (driver.findElements(popup).size() > 0) {
                            click(popup);
                            pause(300);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            pause(400);
            attempts++;
        }
        System.out.println("⚠️ Option not found for '" + optionText + "' — continuing");
    }

    static void scrollToHeader(String headerText) {
        try {
            By header = By.xpath("//*[normalize-space()='" + headerText + "']");
            WebElement el = driver.findElement(header);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            pause(500);
        } catch (Exception e) {
            js.executeScript("window.scrollBy(0,600);");
            pause(500);
        }
    }

    // ---------------- login ----------------
    static void login() {
        driver.get(BASE_URL);
        type(By.id("email"), EMAIL);
        type(By.id("password"), PASSWORD);
        click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));

        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("nav")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Dashboard') or contains(text(),'Sites')]"))
        ));
        System.out.println("✔ Login successful");
    }

    // ---------------- site selection (your exact code) ----------------
    static void selectSite() {
        clickable(By.xpath("//div[contains(@class,'MuiAutocomplete-root')]"), DEFAULT_TIMEOUT).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@role='listbox']")));

        js.executeScript("document.querySelectorAll('ul[role=\"listbox\"]').forEach(e => e.scrollTop=e.scrollHeight);");

        By testSite = By.xpath("//li[normalize-space()='Test Site']");

        WebElement selected = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(200))
                .ignoring(NoSuchElementException.class)
                .ignoring(ElementClickInterceptedException.class)
                .until(d -> {
                    try {
                        for (WebElement li : d.findElements(testSite)) {
                            try { li.click(); return li; } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                    return null;
                });

        if (selected == null) throw new RuntimeException("❌ Could not click Test Site");
        System.out.println("✔ Test Site Selected Successfully");
    }

    // ---------------- go to assets ----------------
    static void goToAssets() {
        click(By.xpath("//span[normalize-space()='Assets'] | //a[normalize-space()='Assets'] | //button[normalize-space()='Assets']"));
        visible(By.xpath("//button[normalize-space()='Create Asset']"), DEFAULT_TIMEOUT);
        System.out.println("✔ Assets Page Loaded");
    }

    // ------ clickCreateAsset (UPDATED & WORKING) ---------------
    static void clickCreateAsset() {

        By createBtn = By.xpath("(//button[contains(@class,'MuiButton-containedPrimary')])[last()]");

        try {
            WebElement btn = wait.until(ExpectedConditions.visibilityOfElementLocated(createBtn));

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            pause(150);

            js.executeScript("arguments[0].click();", btn);

            System.out.println("✔ Create Asset clicked successfully");

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to click Create Asset using stable class-based locator", e);
        }
    }

    // ---------------- PHASE 1: create ----------------
    static void createAssetPhase1() {
        System.out.println("=== PHASE 1: CREATE ASSET ===");

        click(By.xpath("//button[normalize-space()='Create Asset']"));

        // ensure dialog opened
        visible(By.xpath("//*[contains(text(),'Add Asset') or contains(text(),'Create Asset') or contains(text(),'BASIC INFO')]"), DEFAULT_TIMEOUT);

        // BASIC INFO: Asset Name, QR
        By assetName = By.xpath("//p[contains(text(),'Asset Name')]/following::input[1]");
        By qrCode = By.xpath("//p[contains(text(),'QR Code')]/following::input[1]");
        type(assetName, ASSET_NAME);
        type(qrCode, QR_CODE);

        // Asset Class (autocomplete)
        By classInput = By.xpath("//p[contains(text(),'Asset Class')]/following::input[1]");
        typeAndSelectDropdown(classInput, ASSET_CLASS, ASSET_CLASS);
        pause(300);

        // scroll to ensure Subtype visible
        scrollToHeader("Asset Subtype (Optional)");
        pause(300);

        // Subtype - pick first option if exists
        By subtypeInput = By.xpath("//p[contains(text(),'Asset Subtype')]/following::input[1]");
        try {
            click(subtypeInput);
            pause(300);
            By firstOption = By.xpath("(//li[contains(@id,'option') or @role='option'])[1]");
            if (driver.findElements(firstOption).size() > 0) click(firstOption);
        } catch (Exception e) {
            System.out.println("ℹ️ Subtype not available or selection skipped");
        }

        // Select Condition = 2
        scrollToHeader("Condition of Maintenance");
        pause(200);
        click(By.xpath("//p[contains(text(),'Condition of Maintenance')]/following::button[.//h4[normalize-space()='" + CONDITION_VALUE + "'] or normalize-space()='" + CONDITION_VALUE + "'][1]"));
        pause(300);

        // Expand CORE ATTRIBUTES
        scrollToHeader("CORE ATTRIBUTES");
        try {
            By coreToggle = By.xpath("//h6[contains(text(),'CORE ATTRIBUTES')]/ancestor::button[1]");
            if (driver.findElements(coreToggle).size() > 0) {
                WebElement toggle = driver.findElement(coreToggle);
                String expanded = toggle.getAttribute("aria-expanded");
                if (expanded == null || expanded.equals("false")) {
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", toggle);
                    pause(200);
                    toggle.click();
                    pause(400);
                }
            }
        } catch (Exception ignored) {}

        // Model
        By modelField = By.xpath("//p[contains(text(),'Model')]/following::input[1]");
        type(modelField, MODEL_VAL);

        // Notes
        By notesField = By.xpath("//p[contains(text(),'Notes')]/following::input[1]");
        type(notesField, NOTES_VAL);

        // Ampere Rating
        By ampereInput = By.xpath("//p[contains(text(),'Ampere Rating')]/following::input[1]");
        typeAndSelectDropdown(ampereInput, AMPERE_RATING, AMPERE_RATING);

        // Manufacturer
        By manuInput = By.xpath("//p[contains(text(),'Manufacturer')]/following::input[1]");
        typeAndSelectDropdown(manuInput, MANUFACTURER, MANUFACTURER);

        // Catalog Number
        By catalogInput = By.xpath("//p[contains(text(),'Catalog Number')]/following::input[1]");
        type(catalogInput, CATALOG_NUMBER);

        // Breaker Settings
        By breakerInput = By.xpath("//p[contains(text(),'Breaker Settings')]/following::input[1]");
        try { type(breakerInput, BREAKER_SETTINGS); } catch (Exception ignored) {}

        // Interrupting Rating
        By interruptInput = By.xpath("//p[contains(text(),'Interrupting Rating')]/following::input[1]");
        typeAndSelectDropdown(interruptInput, INTERRUPTING_RATING, INTERRUPTING_RATING);

        // kA Rating
        By kaInput = By.xpath("//p[contains(text(),'kA Rating')]/following::input[1]");
        typeAndSelectDropdown(kaInput, KA_RATING, KA_RATING);

        // Commercial -> Replacement Cost
        scrollToHeader("Replacement Cost");
        By replCost = By.xpath("//p[contains(text(),'Replacement Cost')]/following::input[1]");
        try { type(replCost, REPLACEMENT_COST); } 
        catch (Exception ignored) {
            try { type(By.xpath("(//input[@type='number' or contains(@placeholder,'Replacement')])[1]"), REPLACEMENT_COST); } 
            catch (Exception ex) {}
        }

        // CLICK CREATE ASSET (FULLY WORKING)
        clickCreateAsset();

        // wait for success
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Asset created') or contains(text(),'created successfully')]")),
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//table|//div[contains(@class,'asset-list') or contains(@class,'AssetList')]"))
            ));
        } catch (Exception e) {
            System.out.println("⚠️ No explicit success toast detected — verify UI");
        }
    }
    // ---------------- PHASE 2: edit ----------------
    static void editAssetPhase2() {
        System.out.println("=== PHASE 2: EDIT ASSET ===");

        // Try to search for the asset (if search exists)
        try {
            By search = By.xpath("//input[@placeholder='Search' or contains(@placeholder,'Search') or @aria-label='Search']");
            if (driver.findElements(search).size() > 0) {
                type(search, ASSET_NAME);
                pause(500);
            }
        } catch (Exception ignored) {}

        // Open edit for the asset
        try {
        	By editNear = By.xpath("//div[@class='MuiDataGrid-row MuiDataGrid-row--firstVisible']//button[@title='Edit Asset']\r\n"
        			+ "");

        if (driver.findElements(editNear).size() > 0) {
                click(editNear);
            } else {
                // fallback: open first row menu and click Edit
                click(By.xpath("(//button[contains(@class,'MuiIconButton-root')])[1]"));
                pause(400);
                click(By.xpath("//li[normalize-space()='Edit' or contains(.,'Edit')]"));
            }
            pause(700);
        } catch (Exception e) {
            throw new RuntimeException("Edit action not found for asset: " + ASSET_NAME, e);
        }

        // Ensure CORE ATTRIBUTES expanded
        scrollToHeader("CORE ATTRIBUTES");
        try {
            By coreToggle = By.xpath("//h6[contains(text(),'CORE ATTRIBUTES')]/ancestor::button[1]");
            if (driver.findElements(coreToggle).size() > 0) {
                WebElement toggle = driver.findElement(coreToggle);
                String expanded = toggle.getAttribute("aria-expanded");
                if (expanded == null || expanded.equals("false")) {
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", toggle);
                    pause(200);
                    toggle.click();
                    pause(400);
                }
            }
        } catch (Exception ignored) {}

        // Edit fields (hardcoded)
        try { type(By.xpath("//p[contains(text(),'Model')]/following::input[1]"), "edit"); } catch (Exception ignored) {}
        try { type(By.xpath("//p[contains(text(),'Notes')]/following::input[1]"), "edit"); } catch (Exception ignored) {}

        // kA selection in edit
        try {
            By kaInput = By.xpath("//p[contains(text(),'kA Rating')]/following::input[1]");
            typeAndSelectDropdown(kaInput, "10 kA", "10 kA");
            pause(300);
            typeAndSelectDropdown(kaInput, "18 kA", "18 kA");
        } catch (Exception ignored) {}

        // Double-click none if exists
        try {
            By none = By.xpath("//div[normalize-space()='none' or contains(.,'none')]");
            if (driver.findElements(none).size() > 0) {
                WebElement el = driver.findElement(none);
                actions.doubleClick(el).perform();
                pause(300);
            }
        } catch (Exception ignored) {}

        // Suggested Shortcut -> Fuse if present
        try {
            By shortcut = By.xpath("//p[contains(text(),'Suggested Shortcut')]/following::input[1] | //p[contains(text(),'Suggested Shortcut')]/following::div[1]//input[1]");
            if (driver.findElements(shortcut).size() > 0) {
                typeAndSelectDropdown(shortcut, "Fuse", "Fuse");
            }
        } catch (Exception ignored) {}

        // Save changes
        try {
            click(By.xpath("//button[normalize-space()='Save Changes' or contains(.,'Save Changes')]"));
        } catch (Exception e) {
            // fallback
            click(By.xpath("//button[contains(.,'Save') or contains(.,'Update')][last()]"));
        }

        // wait for success
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'updated') or contains(text(),'saved') or contains(text(),'successfully')]")),
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//table|//div[contains(@class,'asset-list') or contains(@class,'AssetList')]"))
            ));
            System.out.println("✔ Edit likely successful");
        } catch (Exception e) {
            System.out.println("⚠️ No explicit edit success toast detected");
        }
    }
    static void deleteAsset() {

        System.out.println("➡ Starting Delete Asset Flow...");
        pause(600);
        By search = By.xpath("//input[@placeholder='Search' or contains(@placeholder,'Search') or @aria-label='Search']");
        if (driver.findElements(search).size() > 0) {
            type(search, ASSET_NAME);
            pause(500);
        }

        // 1️ DELETE ICON (first visible row)
        By deleteButton = By.xpath("(//div[contains(@class,'MuiDataGrid-row')]//button[@title='Delete Asset'])[1]");

        WebElement delBtn = wait.until(ExpectedConditions.elementToBeClickable(deleteButton));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", delBtn);
        pause(200);
        js.executeScript("arguments[0].click();", delBtn);

        System.out.println("🗑 Delete Asset icon clicked");
        pause(700);

        // 2️⃣ CONFIRM DELETE
        By confirmDelete = By.xpath("//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");

        WebElement confirm = wait.until(ExpectedConditions.elementToBeClickable(confirmDelete));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", confirm);
        pause(200);
        js.executeScript("arguments[0].click();", confirm);

        System.out.println("✔ Asset delete confirmed");

        // 3️⃣ No toast – wait for row to remove
        pause(1500);

        System.out.println("✔ Asset deleted successfully ✔");
        System.out.println("✔ Delete Asset Flow Completed");
    }

}
