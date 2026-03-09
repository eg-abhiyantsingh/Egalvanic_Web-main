package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object Model for Location Page
 * Supports CRUD operations on locations (Building > Floor > Room hierarchy)
 */
public class LocationPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    // Navigation
    private static final By LOCATIONS_TAB = By.xpath("//span[normalize-space()='Locations']");

    // Form fields
    private static final By NAME_INPUT = By.xpath("//label[contains(.,'Name')]/following::input[1]");
    private static final By ACCESS_NOTES_INPUT = By.xpath("//label[contains(.,'Access Notes')]/following::textarea[1]");
    private static final By CREATE_BTN = By.xpath("//button[contains(text(),'Create')]");

    // Add buttons
    private static final By ADD_BUILDING_BTN = By.xpath("//button[@aria-label='Add Building']");
    private static final By ADD_FLOOR_BTN = By.xpath("//button[@aria-label='Add Floor']");
    private static final By ADD_ROOM_BTN = By.xpath("//button[@aria-label='Add Room']");

    // Delete / Edit
    private static final By DELETE_LOCATION_BTN = By.xpath("//button[@aria-label='Delete' or @title='Delete']");
    private static final By CONFIRM_DELETE_BTN = By.xpath("//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");
    private static final By EDIT_LOCATION_BTN = By.xpath("//button[@aria-label='Edit' or @title='Edit']");
    private static final By SAVE_BTN = By.xpath("//button[contains(text(),'Save') or contains(text(),'Update')]");

    public LocationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // --- Navigation ---

    public void navigateToLocations() {
        click(LOCATIONS_TAB);
        pause(800);
    }

    public boolean isOnLocationsPage() {
        try {
            return driver.findElements(ADD_BUILDING_BTN).size() > 0
                    || driver.getCurrentUrl().contains("location");
        } catch (Exception e) {
            return false;
        }
    }

    // --- CREATE Building ---

    public void createBuilding(String name, String accessNotes) {
        click(ADD_BUILDING_BTN);
        pause(600);
        typeField(NAME_INPUT, name);
        typeField(ACCESS_NOTES_INPUT, accessNotes);
        jsClick(CREATE_BTN);
        pause(900);
    }

    // --- CREATE Floor ---

    public void createFloor(String parentBuilding, String name, String accessNotes) {
        expandNode(parentBuilding);
        selectNode(parentBuilding);
        pause(400);

        // Try row-specific Add Floor button first
        boolean clicked = false;
        try {
            WebElement buildingItem = getLastMatching(parentBuilding);
            WebElement parentRow = buildingItem.findElement(
                    By.xpath("./ancestor::div[contains(@class,'MuiListItemButton-root')][1]"));
            WebElement addFloorBtn = parentRow.findElement(
                    By.xpath(".//button[@aria-label='Add Floor']"));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", addFloorBtn);
            pause(200);
            js.executeScript("arguments[0].click();", addFloorBtn);
            clicked = true;
        } catch (Exception ignored) {}

        if (!clicked) {
            scrollCollapseToBottom();
            pause(400);
            click(ADD_FLOOR_BTN);
        }
        pause(600);

        typeField(NAME_INPUT, name);
        typeField(ACCESS_NOTES_INPUT, accessNotes);
        jsClick(CREATE_BTN);
        pause(900);
    }

    // --- CREATE Room ---

    public void createRoom(String parentFloor, String name, String accessNotes) {
        scrollCollapseToBottom();
        pause(700);
        expandNode(parentFloor);

        click(ADD_ROOM_BTN);
        pause(600);

        typeField(NAME_INPUT, name);
        typeField(ACCESS_NOTES_INPUT, accessNotes);
        jsClick(CREATE_BTN);
        pause(900);
    }

    // --- READ ---

    public boolean isLocationVisible(String locationName) {
        try {
            getLastMatching(locationName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- UPDATE ---

    public void selectAndEditLocation(String locationName, String newName) {
        selectNode(locationName);
        pause(400);

        // Look for an edit button near the selected node
        try {
            WebElement item = getLastMatching(locationName);
            WebElement parentRow = item.findElement(
                    By.xpath("./ancestor::div[contains(@class,'MuiListItemButton-root')][1]"));
            WebElement editBtn = parentRow.findElement(
                    By.xpath(".//button[@aria-label='Edit' or @title='Edit']"));
            js.executeScript("arguments[0].click();", editBtn);
        } catch (Exception e) {
            click(EDIT_LOCATION_BTN);
        }
        pause(600);

        typeField(NAME_INPUT, newName);
        jsClick(SAVE_BTN);
        pause(900);
    }

    // --- DELETE ---

    public void deleteLocation(String locationName) {
        selectNode(locationName);
        pause(400);

        try {
            WebElement item = getLastMatching(locationName);
            WebElement parentRow = item.findElement(
                    By.xpath("./ancestor::div[contains(@class,'MuiListItemButton-root')][1]"));
            WebElement deleteBtn = parentRow.findElement(
                    By.xpath(".//button[@aria-label='Delete' or @title='Delete']"));
            js.executeScript("arguments[0].click();", deleteBtn);
        } catch (Exception e) {
            click(DELETE_LOCATION_BTN);
        }
        pause(700);
    }

    public void confirmDelete() {
        WebElement confirm = wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_DELETE_BTN));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", confirm);
        pause(200);
        js.executeScript("arguments[0].click();", confirm);
        pause(1500);
    }

    // --- Tree helpers ---

    public void expandNode(String name) {
        WebElement item = getLastMatching(name);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", item);
        pause(400);

        try {
            WebElement parent = item.findElement(
                    By.xpath("./ancestor::div[contains(@class,'MuiListItem')][1]"));
            WebElement arrow = parent.findElement(
                    By.xpath(".//button[.//svg/path[contains(@d,'M10 6')]]"));
            js.executeScript("arguments[0].click();", arrow);
            pause(600);
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", item);
            pause(600);
        }
    }

    public void selectNode(String name) {
        WebElement item = getLastMatching(name);
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", item);
            pause(300);
            item.click();
        } catch (Exception ex) {
            js.executeScript("arguments[0].click();", item);
        }
    }

    WebElement getLastMatching(String text) {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            scrollCollapseToBottom();
            pause(500);
            List<WebElement> items = driver.findElements(
                    By.xpath("//*[self::p or self::span][normalize-space()='" + text + "']"));
            if (!items.isEmpty()) {
                return items.get(items.size() - 1);
            }
            pause(300);
        }
        throw new RuntimeException("Location element not found: " + text);
    }

    private void scrollCollapseToBottom() {
        try {
            js.executeScript("document.querySelectorAll('.MuiCollapse-wrapperInner').forEach(el => el.scrollTop = el.scrollHeight);");
            js.executeScript("let root=document.querySelector('.MuiList-root'); if(root){ root.scrollTop = root.scrollHeight; }");
        } catch (Exception ignored) {}
    }

    // --- Common helpers ---

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

    private void jsClick(By by) {
        WebElement el = driver.findElement(by);
        js.executeScript("arguments[0].click();", el);
    }

    private void typeField(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        try { el.clear(); } catch (Exception ignored) {}
        el.click();
        el.sendKeys(text);
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
