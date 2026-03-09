package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Asset CRUD operations
 * Extends BaseTest for driver lifecycle, login, and reporting
 * Create -> Read -> Update -> Delete
 */
public class AssetSmokeTestNG extends BaseTest {

    // Test data with unique timestamps
    private static final String TEST_ASSET_NAME = "SmokeTest_Asset_" + System.currentTimeMillis();
    private static final String TEST_QR_CODE = "QR_" + System.currentTimeMillis();
    private static final String TEST_ASSET_CLASS = "3-Pole Breaker";
    private static final String TEST_CONDITION = "2";
    private static final String TEST_MODEL = "TestModel";
    private static final String TEST_NOTES = "Smoke test notes";
    private static final String TEST_AMPERE_RATING = "20 A";
    private static final String TEST_MANUFACTURER = "Eaton";
    private static final String TEST_CATALOG_NUMBER = "CAT001";
    private static final String TEST_BREAKER_SETTINGS = "Setting1";
    private static final String TEST_INTERRUPTING_RATING = "10 kA";
    private static final String TEST_KA_RATING = "18 kA";
    private static final String TEST_REPLACEMENT_COST = "30000";

    @Test(priority = 1, description = "Smoke: Create a new asset with all fields")
    public void testCreateAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_CRUD, "TC_Asset_Create");

        try {
            assetPage.navigateToAssets();
            logStepWithScreenshot("Assets page loaded");

            assetPage.openCreateAssetForm();
            logStep("Opened create asset form");

            assetPage.fillBasicInfo(TEST_ASSET_NAME, TEST_QR_CODE, TEST_ASSET_CLASS, TEST_CONDITION);
            logStep("Filled basic info: " + TEST_ASSET_NAME);

            assetPage.fillCoreAttributes(TEST_MODEL, TEST_NOTES, TEST_AMPERE_RATING,
                    TEST_MANUFACTURER, TEST_CATALOG_NUMBER, TEST_BREAKER_SETTINGS,
                    TEST_INTERRUPTING_RATING, TEST_KA_RATING);
            logStep("Filled core attributes");

            assetPage.fillReplacementCost(TEST_REPLACEMENT_COST);
            assetPage.submitCreateAsset();
            logStepWithScreenshot("Asset creation submitted");

            boolean success = assetPage.waitForCreateSuccess();
            Assert.assertTrue(success, "Asset creation did not complete successfully");

            ExtentReportManager.logPass("Asset created successfully: " + TEST_ASSET_NAME);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_create_error");
            Assert.fail("Asset creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Read/verify asset exists in grid", dependsOnMethods = "testCreateAsset")
    public void testReadAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_LIST, "TC_Asset_Read");

        try {
            assetPage.navigateToAssets();
            boolean found = assetPage.isAssetVisible(TEST_ASSET_NAME);
            logStepWithScreenshot("Asset search result");

            Assert.assertTrue(found, "Asset not found in grid: " + TEST_ASSET_NAME);
            ExtentReportManager.logPass("Asset found in grid: " + TEST_ASSET_NAME);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_read_error");
            Assert.fail("Asset read failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Update asset model and notes", dependsOnMethods = "testReadAsset")
    public void testUpdateAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_EDIT_ASSET, "TC_Asset_Update");

        try {
            assetPage.navigateToAssets();
            assetPage.searchAsset(TEST_ASSET_NAME);
            assetPage.openEditForFirstAsset();
            logStep("Opened edit form");

            assetPage.editModel("UpdatedModel");
            assetPage.editNotes("Updated notes from smoke test");
            assetPage.saveChanges();
            logStepWithScreenshot("Asset updated");

            boolean success = assetPage.waitForEditSuccess();
            Assert.assertTrue(success, "Asset update did not complete successfully");

            ExtentReportManager.logPass("Asset updated successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_update_error");
            Assert.fail("Asset update failed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Smoke: Delete the created asset", dependsOnMethods = "testUpdateAsset")
    public void testDeleteAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_DELETE_ASSET, "TC_Asset_Delete");

        try {
            assetPage.navigateToAssets();
            assetPage.searchAsset(TEST_ASSET_NAME);
            assetPage.deleteFirstAsset();
            logStep("Clicked delete on asset");

            assetPage.confirmDelete();
            logStepWithScreenshot("Asset deleted");

            ExtentReportManager.logPass("Asset deleted successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_delete_error");
            Assert.fail("Asset delete failed: " + e.getMessage());
        }
    }
}
