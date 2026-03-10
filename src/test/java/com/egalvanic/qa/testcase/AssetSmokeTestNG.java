package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Asset CRUD operations.
 * All 4 tests share ONE browser session (via BaseTest @BeforeClass).
 *
 * Execution order is controlled by priority (1→2→3→4).
 * No dependsOnMethods — each test can be run individually from IDE.
 * When run individually, dependent tests skip with a clear message.
 */
public class AssetSmokeTestNG extends BaseTest {

    // Test data with unique timestamps (shared across all methods in this class instance)
    private static final String TEST_ASSET_NAME = "SmokeTest_Asset_" + System.currentTimeMillis();
    private static final String TEST_QR_CODE = "QR_" + System.currentTimeMillis();
    private static final String TEST_ASSET_CLASS = "Circuit Breaker";
    private static final String TEST_REPLACEMENT_COST = "30000";

    // State tracking — set by testCreate, checked by dependent tests
    private boolean assetCreated = false;

    @Test(priority = 1, description = "Smoke: Create a new asset with all fields")
    public void testCreateAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_CRUD, "TC_Asset_Create");

        try {
            assetPage.navigateToAssets();
            logStepWithScreenshot("Assets page loaded");

            assetPage.openCreateAssetForm();
            logStep("Opened create asset form");

            assetPage.fillBasicInfo(TEST_ASSET_NAME, TEST_QR_CODE, TEST_ASSET_CLASS);
            logStep("Filled basic info: " + TEST_ASSET_NAME);

            assetPage.fillCoreAttributes();
            logStep("Core attributes section checked");

            assetPage.fillReplacementCost(TEST_REPLACEMENT_COST);
            logStepWithScreenshot("Form filled — about to submit");
            assetPage.submitCreateAsset();
            logStepWithScreenshot("Asset creation submitted");

            boolean success = assetPage.waitForCreateSuccess();
            Assert.assertTrue(success, "Asset creation did not complete successfully");

            assetCreated = true;
            ExtentReportManager.logPass("Asset created successfully: " + TEST_ASSET_NAME);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_create_error");
            Assert.fail("Asset creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Read/verify asset exists in grid")
    public void testReadAsset() {
        requireAssetCreated("testReadAsset");
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

    @Test(priority = 3, description = "Smoke: Update asset model and notes")
    public void testUpdateAsset() {
        requireAssetCreated("testUpdateAsset");
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

    @Test(priority = 4, description = "Smoke: Delete the created asset")
    public void testDeleteAsset() {
        requireAssetCreated("testDeleteAsset");
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

    /**
     * Check precondition: asset must have been created by testCreateAsset.
     * When running a single test from IDE, testCreateAsset won't have run,
     * so this skips gracefully instead of throwing a cryptic dependency error.
     */
    private void requireAssetCreated(String testName) {
        if (!assetCreated) {
            throw new SkipException(testName + " requires testCreateAsset to run first. "
                    + "Run the full class or suite, not a single method.");
        }
    }
}
