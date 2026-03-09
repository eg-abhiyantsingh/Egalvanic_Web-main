
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import com.aventstack.extentreports.Status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Updated Egalvanic3 - robust addConnection() without direct /connections navigation.
 * - Tries multiple sidebar locators for Connections (no driver.get to /connections)
 * - Robust MUI Autocomplete handling with many fallbacks and debug dumps
 * - Preserves original helpers and style
 */
public class Egalvanic_location {

    static WebDriver driver;
    static WebDriverWait wait;
    static JavascriptExecutor js;
    static Actions actions;

    // === CONFIG (hardcoded) ===
    static final String BASE_URL = "https://acme.egalvanic.ai/login";
    static final String EMAIL = "rahul+acme@egalvanic.com";
    static final String PASSWORD = "RP@egalvanic123";

    static final int DEFAULT_TIMEOUT = 25;

    public static void main(String[] args) {
        setupDriver();

        try {
            login();
            takeShot("after_login");
            Thread.sleep(500);
            selectSite();
            takeShot("after_site_select");

            createLocations();
            takeShot("create_location");

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
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized");
        opts.addArguments("--remote-allow-origins=*");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(opts);
        wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        js = (JavascriptExecutor) driver;
        actions = new Actions(driver);
        js.executeScript("document.body.style.zoom='90%';");
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

    // ---------------- site selection (existing) ----------------
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

    // ================= NEW METHODS =================
    static WebElement getLastMatching(String text) {

        long end = System.currentTimeMillis() + 8000; // retry 8 sec

        while (System.currentTimeMillis() < end) {

            try {
                // Scroll ALL collapses in page
                js.executeScript(
                    "document.querySelectorAll('.MuiCollapse-wrapperInner').forEach(el => el.scrollTop = el.scrollHeight);"
                );

                js.executeScript(
                    "let root=document.querySelector('.MuiList-root'); if(root){ root.scrollTop = root.scrollHeight; }"
                );

            } catch (Exception ignored) {}

            pause(500);

            List<WebElement> items = driver.findElements(
                    By.xpath("//*[self::p or self::span][normalize-space()='" + text + "']")
            );


            if (!items.isEmpty()) {
                return items.get(items.size() - 1); // the newest one
            }

            pause(300);
        }

        throw new RuntimeException("Element not found: " + text);
    }



    static void expandNode(String name) {

        WebElement item = getLastMatching(name);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", item);
        pause(400);

        try {
            WebElement parent = item.findElement(
                    By.xpath("./ancestor::div[contains(@class,'MuiListItem')][1]")
            );

            WebElement arrow = parent.findElement(
                    By.xpath(".//button[.//svg/path[contains(@d,'M10 6')]]")
            );

            js.executeScript("arguments[0].click();", arrow);
            pause(600);

        } catch (Exception e) {
            System.out.println("⚠ Arrow not found → clicking label instead");
            js.executeScript("arguments[0].click();", item);
            pause(600);
        }
    }


    // ================= CREATE LOCATIONS =================
    static void createLocations() {

        System.out.println("➡ Starting Location Creation...");

        By locationsTab = By.xpath("//span[normalize-space()='Locations']");
        click(locationsTab);
        pause(800);

        By nameInput  = By.xpath("//label[contains(.,'Name')]/following::input[1]");
        By notesInput = By.xpath("//label[contains(.,'Access Notes')]/following::textarea[1]");
        By createBtn  = By.xpath("//button[contains(text(),'Create')]");

        // 1️⃣ CREATE BUILDING
        System.out.println("➡ Creating Building...");
        click(By.xpath("//button[@aria-label='Add Building']"));
        pause(600);

        click(nameInput);
        type(nameInput, "abhiyant building 3");

        click(notesInput);
        type(notesInput, "access notes 3");

        js.executeScript("arguments[0].click();", driver.findElement(createBtn));
        System.out.println("✔ Building Created");
        pause(900);

        // ⭐ Expand NEWEST Building
        expandNode("abhiyant building 3");

        // =============================
        // ⭐ ENSURE NEW BUILDING IS SELECTED
        // =============================
        System.out.println("➡ Selecting newest building to ensure floor created under it");

        WebElement buildingItem = getLastMatching("abhiyant building 3");

        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", buildingItem);
            pause(300);
            buildingItem.click();
        } catch (Exception ex) {
            try { js.executeScript("arguments[0].click();", buildingItem); } catch (Exception ignored) {}
        }

        pause(400);

        // =============================
        // ⭐ TRY ROW-SPECIFIC "ADD FLOOR" BUTTON
        // =============================
        boolean clickedAddFloor = false;

        try {
            WebElement parentRow = buildingItem.findElement(
                By.xpath("./ancestor::div[contains(@class,'MuiListItemButton-root')][1]")
            );

            WebElement addFloorBtn = parentRow.findElement(
                By.xpath(".//button[@aria-label='Add Floor']")
            );

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", addFloorBtn);
            pause(200);
            js.executeScript("arguments[0].click();", addFloorBtn);

            clickedAddFloor = true;
            System.out.println("✔ Clicked Add Floor button inside the selected building row");

        } catch (Exception e) {
            System.out.println("⚠ Per-row Add Floor not found — will use global Add Floor button");
        }

        // =============================
        // ⭐ FALLBACK GLOBAL ADD FLOOR
        // =============================
        if (!clickedAddFloor) {
            try {
                js.executeScript("let box=document.querySelector('.MuiCollapse-wrapperInner'); if(box){ box.scrollTop = box.scrollHeight; }");
            } catch (Exception ignored) {}

            pause(400);
            click(By.xpath("//button[@aria-label='Add Floor']"));
            System.out.println("✔ Clicked global Add Floor button");
        }

        pause(600);

        // 2️⃣ CREATE FLOOR
        System.out.println("➡ Creating Floor...");

        click(nameInput);
        type(nameInput, "abhiyant floor 3");

        click(notesInput);
        type(notesInput, "access notes floor 3");

        js.executeScript("arguments[0].click();", driver.findElement(createBtn));
        System.out.println("✔ Floor Created");
        pause(900);

        // ⭐ Scroll again so new floor becomes visible at bottom
        try {
            js.executeScript("let box=document.querySelector('.MuiCollapse-wrapperInner'); if(box){ box.scrollTop = box.scrollHeight; }");
        } catch (Exception ignored) {}
        pause(700);

        // ⭐ Expand NEWEST Floor
        expandNode("abhiyant floor 3");

        // 3️⃣ CREATE ROOM
        System.out.println("➡ Creating Room...");
        click(By.xpath("//button[@aria-label='Add Room']"));
        pause(600);

        click(nameInput);
        type(nameInput, "abhiyant room 2");

        click(notesInput);
        type(notesInput, "room 3 access notes");

        js.executeScript("arguments[0].click();", driver.findElement(createBtn));
        System.out.println("✔ Room Created");

        pause(1000);

        System.out.println("🎉 LOCATION MODULE COMPLETED SUCCESSFULLY");
    }
} 