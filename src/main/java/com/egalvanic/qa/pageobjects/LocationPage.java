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

    // Dialog / Form fields (scoped to MUI Dialog when open)
    private static final By DIALOG = By.xpath("//div[@role='dialog'] | //div[contains(@class,'MuiDialog-paper')]");
    private static final By NAME_INPUT = By.xpath("//div[@role='dialog']//label[contains(.,'Name')]/following::input[1] | //label[contains(.,'Name')]/following::input[1]");
    private static final By ACCESS_NOTES_TEXTAREA = By.xpath("//div[@role='dialog']//label[contains(.,'Access Notes')]/following::textarea[1] | //label[contains(.,'Access Notes')]/following::textarea[1]");
    private static final By CREATE_BTN = By.xpath("//div[@role='dialog']//button[normalize-space()='Create'] | //button[normalize-space()='Create']");

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
        waitForDialog();
        typeField(NAME_INPUT, name);
        try { typeFieldTextarea(ACCESS_NOTES_TEXTAREA, accessNotes); } catch (Exception ignored) {}
        // Use Selenium click to properly trigger React's onClick
        try {
            wait.until(ExpectedConditions.elementToBeClickable(CREATE_BTN)).click();
        } catch (Exception e) {
            jsClick(CREATE_BTN);
        }
        waitForDialogClose();
    }

    // --- CREATE Floor ---

    public void createFloor(String parentBuilding, String name, String accessNotes) {
        // Select the building first
        WebElement buildingItem = getLastMatching(parentBuilding);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", buildingItem);
        pause(300);
        buildingItem.click();
        pause(800);

        // Re-find building and click its Add Floor button
        buildingItem = getLastMatching(parentBuilding);
        boolean clicked = false;

        // Find Add Floor button within the building's container
        try {
            WebElement container = buildingItem.findElement(
                    By.xpath("./ancestor::*[.//button[@aria-label='Add Floor']][1]"));
            WebElement addFloorBtn = container.findElement(
                    By.xpath(".//button[@aria-label='Add Floor']"));
            js.executeScript("arguments[0].click();", addFloorBtn);
            clicked = true;
        } catch (Exception ignored) {}

        // Fallback: first following Add Floor button
        if (!clicked) {
            try {
                WebElement addFloorBtn = buildingItem.findElement(
                        By.xpath("./following::button[@aria-label='Add Floor'][1]"));
                js.executeScript("arguments[0].click();", addFloorBtn);
                clicked = true;
            } catch (Exception ignored) {}
        }

        if (!clicked) {
            click(ADD_FLOOR_BTN);
        }

        // Fill dialog
        waitForDialog();
        typeField(NAME_INPUT, name);
        try { typeFieldTextarea(ACCESS_NOTES_TEXTAREA, accessNotes); } catch (Exception ignored) {}

        // Submit via Selenium click
        try {
            wait.until(ExpectedConditions.elementToBeClickable(CREATE_BTN)).click();
        } catch (Exception e) {
            jsClick(CREATE_BTN);
        }
        waitForDialogClose();
        pause(1000);
    }

    // --- CREATE Room ---

    public void createRoom(String parentFloor, String name, String accessNotes) {
        expandNode(parentFloor);
        pause(500);

        // Find Add Room button near the floor
        WebElement floorItem = getLastMatching(parentFloor);
        boolean clicked = false;

        // Strategy 1: aria-label='Add Room'
        try {
            WebElement addRoomBtn = floorItem.findElement(
                    By.xpath("./following::button[@aria-label='Add Room'][1]"));
            js.executeScript("arguments[0].click();", addRoomBtn);
            clicked = true;
            System.out.println("[LocationPage] Add Room clicked via aria-label");
        } catch (Exception ignored) {}

        // Strategy 2: Any add/+ button in the floor's row
        if (!clicked) {
            try {
                WebElement row = floorItem.findElement(
                        By.xpath("./ancestor::div[contains(@class,'MuiListItem') or contains(@class,'MuiTreeItem')][1]"));
                List<WebElement> buttons = row.findElements(By.xpath(".//button"));
                for (WebElement btn : buttons) {
                    String label = btn.getAttribute("aria-label");
                    String title = btn.getAttribute("title");
                    if ((label != null && (label.contains("Add") || label.equals("+")))
                            || (title != null && title.contains("Add"))
                            || btn.getText().trim().equals("+")) {
                        js.executeScript("arguments[0].click();", btn);
                        clicked = true;
                        System.out.println("[LocationPage] Add Room clicked via row button scan");
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Strategy 3: Click floor to select, then find Add Room
        if (!clicked) {
            try {
                floorItem.click();
                pause(800);
                By addRoom = By.xpath("//button[contains(@aria-label,'Add Room') or contains(@aria-label,'Add room') or contains(@title,'Add Room')]");
                if (driver.findElements(addRoom).size() > 0) {
                    js.executeScript("arguments[0].click();", driver.findElement(addRoom));
                    clicked = true;
                    System.out.println("[LocationPage] Add Room clicked after selecting floor");
                }
            } catch (Exception ignored) {}
        }

        // Strategy 4: Global fallback
        if (!clicked) {
            click(ADD_ROOM_BTN);
            System.out.println("[LocationPage] Add Room clicked via global fallback");
        }

        waitForDialog();
        typeField(NAME_INPUT, name);
        try { typeFieldTextarea(ACCESS_NOTES_TEXTAREA, accessNotes); } catch (Exception ignored) {}
        jsClick(CREATE_BTN);
        waitForDialogClose();
        pause(500);
        System.out.println("[LocationPage] Room creation completed for: " + name);
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
        // Click on the location in the tree to show its detail panel
        selectNode(locationName);
        pause(1000);

        // The Edit button is a pencil icon in the detail panel header (top-right).
        // It's the last MuiIconButton in the header area, after the refresh button.
        boolean clicked = false;

        // Strategy 1: Find the pencil/edit SVG icon button
        // The edit icon uses an SVG with a pencil path (data-testid='EditIcon' or 'CreateIcon')
        try {
            By editIcons = By.xpath("//button[.//svg[@data-testid='EditIcon' or @data-testid='CreateIcon']]");
            if (driver.findElements(editIcons).size() > 0) {
                js.executeScript("arguments[0].click();", driver.findElement(editIcons));
                clicked = true;
                System.out.println("[LocationPage] Edit clicked via SVG data-testid");
            }
        } catch (Exception ignored) {}

        // Strategy 2: aria-label based
        if (!clicked) {
            try {
                By editBtns = By.xpath("//button[@aria-label='Edit' or @title='Edit' or @aria-label='edit' or @aria-label='Edit location']");
                if (driver.findElements(editBtns).size() > 0) {
                    js.executeScript("arguments[0].click();", driver.findElement(editBtns));
                    clicked = true;
                    System.out.println("[LocationPage] Edit clicked via aria-label");
                }
            } catch (Exception ignored) {}
        }

        // Strategy 3: Click the last MuiIconButton on the page (pencil icon is typically last)
        if (!clicked) {
            try {
                List<WebElement> iconButtons = driver.findElements(By.xpath("//button[contains(@class,'MuiIconButton')]"));
                System.out.println("[LocationPage] Found " + iconButtons.size() + " MuiIconButtons");
                if (iconButtons.size() > 0) {
                    WebElement lastBtn = iconButtons.get(iconButtons.size() - 1);
                    js.executeScript("arguments[0].click();", lastBtn);
                    clicked = true;
                    System.out.println("[LocationPage] Edit clicked via last MuiIconButton");
                }
            } catch (Exception ignored) {}
        }

        if (!clicked) {
            click(EDIT_LOCATION_BTN);
        }
        pause(1500);

        // After clicking edit, the detail panel may switch to inline edit mode
        // or open a dialog. Look for a Name input field.
        // Check if we're in a dialog or inline edit mode
        boolean hasDialog = driver.findElements(DIALOG).size() > 0;
        System.out.println("[LocationPage] Edit mode: dialog=" + hasDialog);

        // Find the name input — could be in dialog or inline
        By nameInput = hasDialog ? NAME_INPUT :
                By.xpath("//input[contains(@name,'name') or contains(@placeholder,'Name') or @id='name'] | " + NAME_INPUT.toString().replace("By.xpath: ", ""));

        typeField(NAME_INPUT, newName);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(SAVE_BTN)).click();
        } catch (Exception e) {
            jsClick(SAVE_BTN);
        }
        pause(900);
    }

    // --- DELETE ---

    public void deleteLocation(String locationName) {
        selectNode(locationName);
        pause(800);

        boolean clicked = false;

        // Strategy 1: Delete button via SVG data-testid='DeleteIcon' or 'DeleteOutlineIcon'
        try {
            By deleteIcon = By.xpath("//button[.//svg[@data-testid='DeleteIcon' or @data-testid='DeleteOutlineIcon' or @data-testid='DeleteForeverIcon']]");
            List<WebElement> delBtns = driver.findElements(deleteIcon);
            if (!delBtns.isEmpty()) {
                js.executeScript("arguments[0].click();", delBtns.get(delBtns.size() - 1));
                clicked = true;
                System.out.println("[LocationPage] Delete clicked via SVG DeleteIcon");
            }
        } catch (Exception ignored) {}

        // Strategy 2: aria-label or title
        if (!clicked) {
            try {
                By deleteBtns = By.xpath("//button[@aria-label='Delete' or @title='Delete' or @aria-label='delete' or @aria-label='Delete location' or @aria-label='Remove']");
                if (driver.findElements(deleteBtns).size() > 0) {
                    js.executeScript("arguments[0].click();", driver.findElement(deleteBtns));
                    clicked = true;
                    System.out.println("[LocationPage] Delete clicked via aria-label/title");
                }
            } catch (Exception ignored) {}
        }

        // Strategy 3: Look for a kebab/more-options menu (three dots) in the detail panel
        if (!clicked) {
            try {
                By moreMenu = By.xpath("//button[.//svg[@data-testid='MoreVertIcon' or @data-testid='MoreHorizIcon']] | //button[@aria-label='More' or @aria-label='more options' or @aria-label='Options']");
                List<WebElement> menuBtns = driver.findElements(moreMenu);
                if (!menuBtns.isEmpty()) {
                    js.executeScript("arguments[0].click();", menuBtns.get(menuBtns.size() - 1));
                    pause(800);
                    // Look for Delete option in the menu
                    By deleteOption = By.xpath("//li[contains(.,'Delete')] | //div[@role='menuitem'][contains(.,'Delete')] | //span[normalize-space()='Delete']/ancestor::li");
                    if (driver.findElements(deleteOption).size() > 0) {
                        js.executeScript("arguments[0].click();", driver.findElement(deleteOption));
                        clicked = true;
                        System.out.println("[LocationPage] Delete clicked via kebab menu");
                    }
                }
            } catch (Exception ignored) {}
        }

        // Strategy 4: Open the edit dialog and look for delete button inside it
        if (!clicked) {
            System.out.println("[LocationPage] Trying delete via edit dialog...");
            try {
                // Click the edit/pencil icon to open the edit dialog
                By editIcons = By.xpath("//button[.//svg[@data-testid='EditIcon' or @data-testid='CreateIcon']]");
                List<WebElement> editBtns = driver.findElements(editIcons);
                if (editBtns.isEmpty()) {
                    // Fallback: last MuiIconButton
                    editBtns = driver.findElements(By.xpath("//button[contains(@class,'MuiIconButton')]"));
                }
                if (!editBtns.isEmpty()) {
                    js.executeScript("arguments[0].click();", editBtns.get(editBtns.size() - 1));
                    pause(1500);

                    // Look for Delete button inside the dialog
                    By dialogDelete = By.xpath("//div[@role='dialog']//button[contains(.,'Delete') or contains(@class,'error') or contains(@class,'danger')]");
                    List<WebElement> dialogDelBtns = driver.findElements(dialogDelete);
                    if (!dialogDelBtns.isEmpty()) {
                        // Scroll to the delete button and click
                        WebElement delBtn = dialogDelBtns.get(dialogDelBtns.size() - 1);
                        js.executeScript("arguments[0].scrollIntoView({block:'center'});", delBtn);
                        pause(300);
                        js.executeScript("arguments[0].click();", delBtn);
                        clicked = true;
                        System.out.println("[LocationPage] Delete clicked inside edit dialog");
                    } else {
                        // Dump all buttons in dialog for debugging
                        List<WebElement> allDialogBtns = driver.findElements(By.xpath("//div[@role='dialog']//button"));
                        System.out.println("[LocationPage] Dialog has " + allDialogBtns.size() + " buttons:");
                        for (WebElement btn : allDialogBtns) {
                            System.out.println("  - text='" + btn.getText().trim() + "' aria-label='" + btn.getAttribute("aria-label") + "'");
                        }
                        // Close dialog so we can try other approaches
                        try {
                            By cancelBtn = By.xpath("//div[@role='dialog']//button[contains(.,'Cancel') or contains(.,'Close')]");
                            if (driver.findElements(cancelBtn).size() > 0) {
                                driver.findElement(cancelBtn).click();
                                pause(500);
                            }
                        } catch (Exception ignored2) {}
                    }
                }
            } catch (Exception e) {
                System.out.println("[LocationPage] Edit dialog delete strategy failed: " + e.getMessage());
            }
        }

        // Strategy 5: Scan ALL buttons on the page for any containing "Delete" text
        if (!clicked) {
            try {
                List<WebElement> allButtons = driver.findElements(By.xpath("//button"));
                System.out.println("[LocationPage] Scanning all " + allButtons.size() + " buttons on page for Delete:");
                for (WebElement btn : allButtons) {
                    String text = "";
                    String ariaLabel = "";
                    try { text = btn.getText().trim(); } catch (Exception ignored) {}
                    try { ariaLabel = btn.getAttribute("aria-label"); } catch (Exception ignored) {}
                    if (text.toLowerCase().contains("delete") || (ariaLabel != null && ariaLabel.toLowerCase().contains("delete"))) {
                        System.out.println("  FOUND: text='" + text + "' aria-label='" + ariaLabel + "'");
                        js.executeScript("arguments[0].click();", btn);
                        clicked = true;
                        break;
                    }
                }
                if (!clicked) {
                    System.out.println("[LocationPage] No Delete button found anywhere on page");
                }
            } catch (Exception ignored) {}
        }

        if (!clicked) {
            throw new RuntimeException("Delete button not found for location: " + locationName);
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

        boolean expanded = false;

        // Strategy 1: Find expand arrow button (any SVG button in the row)
        try {
            WebElement parent = item.findElement(
                    By.xpath("./ancestor::div[contains(@class,'MuiListItem') or contains(@class,'MuiTreeItem')][1]"));
            // Try specific expand arrow paths
            List<WebElement> svgButtons = parent.findElements(By.xpath(".//button[.//svg]"));
            System.out.println("[LocationPage] expandNode: Found " + svgButtons.size() + " SVG buttons in row for: " + name);
            for (WebElement btn : svgButtons) {
                String ariaLabel = "";
                String ariaExpanded = "";
                try { ariaLabel = btn.getAttribute("aria-label"); } catch (Exception ignored) {}
                try { ariaExpanded = btn.getAttribute("aria-expanded"); } catch (Exception ignored) {}
                System.out.println("[LocationPage] expandNode: SVG button aria-label='" + ariaLabel + "' aria-expanded='" + ariaExpanded + "'");

                // Click expand buttons (not Add buttons)
                if (ariaLabel == null || (!ariaLabel.contains("Add") && !ariaLabel.contains("Delete") && !ariaLabel.contains("Edit"))) {
                    js.executeScript("arguments[0].click();", btn);
                    expanded = true;
                    System.out.println("[LocationPage] expandNode: Clicked expand button");
                    pause(800);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[LocationPage] expandNode: No SVG buttons in row: " + e.getMessage());
        }

        // Strategy 2: Click the item text directly (toggle expansion)
        if (!expanded) {
            System.out.println("[LocationPage] expandNode: Clicking item text directly");
            js.executeScript("arguments[0].click();", item);
            pause(800);
        }

        // Strategy 3: Double-click to expand
        if (!expanded) {
            try {
                new org.openqa.selenium.interactions.Actions(driver).doubleClick(item).perform();
                pause(800);
            } catch (Exception ignored) {}
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
        long end = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < end) {
            scrollCollapseToBottom();
            pause(500);
            // Try multiple element types — MUI tree might use p, span, div, or other tags
            List<WebElement> items = driver.findElements(
                    By.xpath("//*[self::p or self::span or self::div or self::h6][normalize-space()='" + text + "']"));
            if (items.isEmpty()) {
                // Also try contains match for partial text
                items = driver.findElements(
                        By.xpath("//*[self::p or self::span][contains(normalize-space(),'" + text + "')]"));
            }
            if (!items.isEmpty()) {
                WebElement el = items.get(items.size() - 1);
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
                pause(200);
                return el;
            }
            pause(500);
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
        el.click();
        // Select all existing text and replace — el.clear() doesn't work with React controlled inputs
        String selectAll = org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a");
        String selectAllMac = org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.COMMAND, "a");
        try { el.sendKeys(selectAll); } catch (Exception ignored) {}
        try { el.sendKeys(selectAllMac); } catch (Exception ignored) {}
        pause(100);
        el.sendKeys(text);
    }

    private void waitForDialog() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(DIALOG));
            pause(500);
        } catch (Exception e) {
            // Dialog might use a different structure — wait a bit anyway
            pause(1000);
        }
    }

    private void waitForDialogClose() {
        try {
            // Wait for dialog to disappear
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.invisibilityOfElementLocated(DIALOG));
        } catch (Exception ignored) {}
        pause(1000);
    }

    private void typeFieldTextarea(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        try { el.clear(); } catch (Exception ignored) {}
        el.click();
        el.sendKeys(text);
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
