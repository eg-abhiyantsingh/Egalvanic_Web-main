package com.egalvanic.qa.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for Asset Page
 * Supports CRUD operations on assets
 */
public class AssetPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private static final int TIMEOUT = 25;

    // Navigation
    private static final By ASSETS_NAV = By.xpath("//span[normalize-space()='Assets'] | //a[normalize-space()='Assets'] | //button[normalize-space()='Assets']");
    private static final By CREATE_ASSET_BTN = By.xpath("//button[normalize-space()='Create Asset']");

    // Asset form fields
    private static final By ASSET_NAME_INPUT = By.xpath("//p[contains(text(),'Asset Name')]/following::input[1]");
    private static final By QR_CODE_INPUT = By.xpath("//p[contains(text(),'QR Code')]/following::input[1]");
    private static final By ASSET_CLASS_INPUT = By.xpath("//p[contains(text(),'Asset Class')]/following::input[1]");
    private static final By ASSET_SUBTYPE_INPUT = By.xpath("//p[contains(text(),'Asset Subtype')]/following::input[1]");
    // Core attributes
    private static final By CORE_ATTRIBUTES_TOGGLE = By.xpath("//h6[contains(text(),'CORE ATTRIBUTES')]/ancestor::button[1]");
    private static final By MODEL_INPUT = By.xpath("//p[contains(text(),'Model')]/following::input[1]");
    private static final By NOTES_INPUT = By.xpath("//p[contains(text(),'Notes')]/following::input[1]");
    private static final By AMPERE_RATING_INPUT = By.xpath("//p[contains(text(),'Ampere Rating')]/following::input[1]");
    private static final By MANUFACTURER_INPUT = By.xpath("//p[contains(text(),'Manufacturer')]/following::input[1]");
    private static final By CATALOG_NUMBER_INPUT = By.xpath("//p[contains(text(),'Catalog Number')]/following::input[1]");
    private static final By BREAKER_SETTINGS_INPUT = By.xpath("//p[contains(text(),'Breaker Settings')]/following::input[1]");
    private static final By INTERRUPTING_RATING_INPUT = By.xpath("//p[contains(text(),'Interrupting Rating')]/following::input[1]");
    private static final By KA_RATING_INPUT = By.xpath("//p[contains(text(),'kA Rating')]/following::input[1]");
    private static final By REPLACEMENT_COST_INPUT = By.xpath("//p[contains(text(),'Replacement Cost')]/following::input[1]");

    // Submit / Save
    private static final By SUBMIT_CREATE_BTN = By.xpath("(//button[contains(@class,'MuiButton-containedPrimary')])[last()]");
    private static final By SAVE_CHANGES_BTN = By.xpath("//button[normalize-space()='Save Changes' or contains(.,'Save Changes')]");

    // Data grid actions
    private static final By SEARCH_INPUT = By.xpath("//input[@placeholder='Search' or contains(@placeholder,'Search') or @aria-label='Search']");
    private static final By FIRST_ROW_EDIT_BTN = By.xpath("//div[@class='MuiDataGrid-row MuiDataGrid-row--firstVisible']//button[@title='Edit Asset']");
    private static final By FIRST_ROW_DELETE_BTN = By.xpath("(//div[contains(@class,'MuiDataGrid-row')]//button[@title='Delete Asset'])[1]");
    private static final By CONFIRM_DELETE_BTN = By.xpath("//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");

    // Success / form indicators
    private static final By ASSET_FORM_DIALOG = By.xpath("//*[contains(text(),'Add Asset') or contains(text(),'Create Asset') or contains(text(),'BASIC INFO')]");
    private static final By SUCCESS_INDICATOR = By.xpath("//*[contains(text(),'Asset created') or contains(text(),'created successfully')]");
    private static final By ASSET_LIST_INDICATOR = By.xpath("//table|//div[contains(@class,'asset-list') or contains(@class,'AssetList') or contains(@class,'MuiDataGrid')]");

    public AssetPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.js = (JavascriptExecutor) driver;
    }

    // --- Navigation ---

    public void navigateToAssets() {
        click(ASSETS_NAV);
        wait.until(ExpectedConditions.visibilityOfElementLocated(CREATE_ASSET_BTN));
    }

    public boolean isOnAssetsPage() {
        try {
            return driver.findElements(CREATE_ASSET_BTN).size() > 0
                    || driver.getCurrentUrl().contains("asset");
        } catch (Exception e) {
            return false;
        }
    }

    // --- CREATE ---

    public void openCreateAssetForm() {
        click(CREATE_ASSET_BTN);
        wait.until(ExpectedConditions.visibilityOfElementLocated(ASSET_FORM_DIALOG));
    }

    public void fillBasicInfo(String assetName, String qrCode, String assetClass, String conditionValue) {
        typeField(ASSET_NAME_INPUT, assetName);
        typeField(QR_CODE_INPUT, qrCode);
        typeAndSelectDropdown(ASSET_CLASS_INPUT, assetClass, assetClass);
        pause(300);

        // Select subtype (first available option)
        try {
            click(ASSET_SUBTYPE_INPUT);
            pause(300);
            By firstOption = By.xpath("(//li[contains(@id,'option') or @role='option'])[1]");
            if (driver.findElements(firstOption).size() > 0) click(firstOption);
        } catch (Exception ignored) {}

        // Condition
        scrollToView("Condition of Maintenance");
        pause(200);
        By condBtn = By.xpath("//p[contains(text(),'Condition of Maintenance')]/following::button[.//h4[normalize-space()='" + conditionValue + "'] or normalize-space()='" + conditionValue + "'][1]");
        click(condBtn);
    }

    public void fillCoreAttributes(String model, String notes, String ampereRating, String manufacturer,
                                    String catalogNumber, String breakerSettings, String interruptingRating, String kaRating) {
        expandCoreAttributes();
        typeField(MODEL_INPUT, model);
        typeField(NOTES_INPUT, notes);
        typeAndSelectDropdown(AMPERE_RATING_INPUT, ampereRating, ampereRating);
        typeAndSelectDropdown(MANUFACTURER_INPUT, manufacturer, manufacturer);
        typeField(CATALOG_NUMBER_INPUT, catalogNumber);
        try { typeField(BREAKER_SETTINGS_INPUT, breakerSettings); } catch (Exception ignored) {}
        typeAndSelectDropdown(INTERRUPTING_RATING_INPUT, interruptingRating, interruptingRating);
        typeAndSelectDropdown(KA_RATING_INPUT, kaRating, kaRating);
    }

    public void fillReplacementCost(String cost) {
        scrollToView("Replacement Cost");
        try {
            typeField(REPLACEMENT_COST_INPUT, cost);
        } catch (Exception ignored) {
            try { typeField(By.xpath("(//input[@type='number' or contains(@placeholder,'Replacement')])[1]"), cost); } catch (Exception ex) {}
        }
    }

    public void submitCreateAsset() {
        WebElement btn = wait.until(ExpectedConditions.visibilityOfElementLocated(SUBMIT_CREATE_BTN));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        pause(150);
        js.executeScript("arguments[0].click();", btn);
    }

    public boolean waitForCreateSuccess() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(SUCCESS_INDICATOR),
                    ExpectedConditions.presenceOfElementLocated(ASSET_LIST_INDICATOR)
            ));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- READ ---

    public void searchAsset(String assetName) {
        if (driver.findElements(SEARCH_INPUT).size() > 0) {
            typeField(SEARCH_INPUT, assetName);
            pause(500);
        }
    }

    public boolean isAssetVisible(String assetName) {
        try {
            searchAsset(assetName);
            By assetRow = By.xpath("//div[contains(@class,'MuiDataGrid-row')]//*[contains(text(),'" + assetName + "')]");
            return driver.findElements(assetRow).size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // --- UPDATE ---

    public void openEditForFirstAsset() {
        if (driver.findElements(FIRST_ROW_EDIT_BTN).size() > 0) {
            click(FIRST_ROW_EDIT_BTN);
        } else {
            click(By.xpath("(//button[contains(@class,'MuiIconButton-root')])[1]"));
            pause(400);
            click(By.xpath("//li[normalize-space()='Edit' or contains(.,'Edit')]"));
        }
        pause(700);
    }

    public void editModel(String newModel) {
        expandCoreAttributes();
        typeField(MODEL_INPUT, newModel);
    }

    public void editNotes(String newNotes) {
        typeField(NOTES_INPUT, newNotes);
    }

    public void saveChanges() {
        try {
            click(SAVE_CHANGES_BTN);
        } catch (Exception e) {
            click(By.xpath("//button[contains(.,'Save') or contains(.,'Update')][last()]"));
        }
    }

    public boolean waitForEditSuccess() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'updated') or contains(text(),'saved') or contains(text(),'successfully')]")),
                    ExpectedConditions.presenceOfElementLocated(ASSET_LIST_INDICATOR)
            ));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- DELETE ---

    public void deleteFirstAsset() {
        WebElement delBtn = wait.until(ExpectedConditions.elementToBeClickable(FIRST_ROW_DELETE_BTN));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", delBtn);
        pause(200);
        js.executeScript("arguments[0].click();", delBtn);
        pause(700);
    }

    public void confirmDelete() {
        WebElement confirm = wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_DELETE_BTN));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", confirm);
        pause(200);
        js.executeScript("arguments[0].click();", confirm);
        pause(1500);
    }

    // --- Helpers ---

    private void expandCoreAttributes() {
        scrollToView("CORE ATTRIBUTES");
        try {
            if (driver.findElements(CORE_ATTRIBUTES_TOGGLE).size() > 0) {
                WebElement toggle = driver.findElement(CORE_ATTRIBUTES_TOGGLE);
                String expanded = toggle.getAttribute("aria-expanded");
                if (expanded == null || expanded.equals("false")) {
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", toggle);
                    pause(200);
                    toggle.click();
                    pause(400);
                }
            }
        } catch (Exception ignored) {}
    }

    private void scrollToView(String headerText) {
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

    private void typeField(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        try { el.clear(); } catch (Exception ignored) {}
        el.click();
        el.sendKeys(text);
    }

    private void typeAndSelectDropdown(By inputLocator, String textToType, String optionText) {
        click(inputLocator);
        pause(200);
        try {
            WebElement in = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            in.clear();
            in.sendKeys(textToType);
        } catch (Exception ignored) {}
        pause(400);
        By listOption = By.xpath("//li[normalize-space()='" + optionText + "'] | //li[contains(normalize-space(),'" + optionText + "')]");
        for (int attempts = 0; attempts < 4; attempts++) {
            try {
                if (driver.findElements(listOption).size() > 0) {
                    click(listOption);
                    return;
                }
                By popup = By.xpath("//button[contains(@class,'MuiAutocomplete-popupIndicator')]");
                if (driver.findElements(popup).size() > 0) {
                    click(popup);
                    pause(300);
                }
            } catch (Exception ignored) {}
            pause(400);
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
