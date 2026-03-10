package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Asset CRUD operations.
 * Every test is fully independent — can be run alone from IDE.
 *
 * When run as a suite (priority 1→2→3→4), tests share the asset
 * created by testCreateAsset to avoid redundant creation.
 * When run individually, each test creates its own test data first.
 */
public class AssetSmokeTestNG extends BaseTest {

    private static final String TEST_ASSET_CLASS = "Circuit Breaker";
    private static final String TEST_REPLACEMENT_COST = "30000";

    // Tracks the asset created in this session (shared when running full suite)
    private String createdAssetName;

    /**
     * Helper: create a test asset and return its name.
     * Called by any test that needs a pre-existing asset.
     */
    private String createTestAsset() {
        String name = "SmokeTest_Asset_" + System.currentTimeMillis();
        String qrCode = "QR_" + System.currentTimeMillis();

        System.out.println("[Setup] Creating test asset: " + name);
        assetPage.navigateToAssets();
        assetPage.openCreateAssetForm();
        assetPage.fillBasicInfo(name, qrCode, TEST_ASSET_CLASS);
        assetPage.fillCoreAttributes();
        assetPage.fillReplacementCost(TEST_REPLACEMENT_COST);
        assetPage.submitCreateAsset();

        boolean success = assetPage.waitForCreateSuccess();
        if (!success) {
            throw new RuntimeException("Setup failed: could not create test asset: " + name);
        }
        System.out.println("[Setup] Test asset created: " + name);
        return name;
    }

    /**
     * Ensures an asset exists for this session.
     * If running full suite, testCreateAsset already set createdAssetName.
     * If running a single test, creates an asset on demand.
     */
    private void ensureAssetExists() {
        if (createdAssetName == null) {
            createdAssetName = createTestAsset();
        }
    }

    @Test(priority = 1, description = "Smoke: Create a new asset with all fields")
    public void testCreateAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_CRUD, "TC_Asset_Create");

        try {
            String name = "SmokeTest_Asset_" + System.currentTimeMillis();
            String qrCode = "QR_" + System.currentTimeMillis();

            assetPage.navigateToAssets();
            logStepWithScreenshot("Assets page loaded");

            assetPage.openCreateAssetForm();
            logStep("Opened create asset form");

            assetPage.fillBasicInfo(name, qrCode, TEST_ASSET_CLASS);
            logStep("Filled basic info: " + name);

            assetPage.fillCoreAttributes();
            logStep("Core attributes section checked");

            assetPage.fillReplacementCost(TEST_REPLACEMENT_COST);
            logStepWithScreenshot("Form filled — about to submit");
            assetPage.submitCreateAsset();
            logStepWithScreenshot("Asset creation submitted");

            boolean success = assetPage.waitForCreateSuccess();
            Assert.assertTrue(success, "Asset creation did not complete successfully");

            createdAssetName = name;
            ExtentReportManager.logPass("Asset created successfully: " + name);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_create_error");
            Assert.fail("Asset creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Read/verify asset exists in grid")
    public void testReadAsset() {
        ensureAssetExists();
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_LIST, "TC_Asset_Read");

        try {
            assetPage.navigateToAssets();
            boolean found = assetPage.isAssetVisible(createdAssetName);
            logStepWithScreenshot("Asset search result");

            Assert.assertTrue(found, "Asset not found in grid: " + createdAssetName);
            ExtentReportManager.logPass("Asset found in grid: " + createdAssetName);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_read_error");
            Assert.fail("Asset read failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Update asset model and notes")
    public void testUpdateAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_EDIT_ASSET, "TC_Asset_Update");

        try {
            assetPage.navigateToAssets();
            assetPage.openEditForFirstAsset();
            logStep("Opened edit form for existing asset");

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
        ensureAssetExists();
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_DELETE_ASSET, "TC_Asset_Delete");

        try {
            assetPage.navigateToAssets();
            assetPage.searchAsset(createdAssetName);
            assetPage.deleteFirstAsset();
            logStep("Clicked delete on asset");

            assetPage.confirmDelete();
            logStepWithScreenshot("Asset deleted");

            createdAssetName = null; // asset is gone
            ExtentReportManager.logPass("Asset deleted successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_delete_error");
            Assert.fail("Asset delete failed: " + e.getMessage());
        }
    }
}
