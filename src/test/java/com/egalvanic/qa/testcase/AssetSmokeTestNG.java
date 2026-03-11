package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Asset CRUD operations.
 * Every test is fully independent — can be run alone from IDE.
 * Tests operate on existing data in the grid (no inter-test dependencies).
 */
public class AssetSmokeTestNG extends BaseTest {

    private static final String TEST_ASSET_CLASS = "Circuit Breaker";
    private static final String TEST_REPLACEMENT_COST = "30000";

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

            ExtentReportManager.logPass("Asset created successfully: " + name);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_create_error");
            Assert.fail("Asset creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Read and verify asset data in grid")
    public void testReadAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_LIST, "TC_Asset_Read");

        try {
            assetPage.navigateToAssets();
            logStep("Navigated to Assets page");

            // 1. Verify grid is populated
            boolean hasAssets = assetPage.isGridPopulated();
            Assert.assertTrue(hasAssets, "Asset grid is empty — no assets found");
            logStep("Grid has data rows");

            // 2. Read first row data and verify it has actual content
            String firstAssetName = assetPage.getFirstRowAssetName();
            Assert.assertNotNull(firstAssetName, "First asset name is null");
            Assert.assertFalse(firstAssetName.trim().isEmpty(), "First asset name is blank");
            logStep("First asset in grid: " + firstAssetName);

            // 3. Search for that asset and verify it appears in filtered results
            assetPage.searchAsset(firstAssetName);
            boolean found = assetPage.isAssetVisible(firstAssetName);
            Assert.assertTrue(found, "Asset not found after searching: " + firstAssetName);
            logStepWithScreenshot("Asset found via search: " + firstAssetName);

            ExtentReportManager.logPass("Asset data verified: " + firstAssetName);

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

    @Test(priority = 4, description = "Smoke: Delete an existing asset and verify removal")
    public void testDeleteAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_DELETE_ASSET, "TC_Asset_Delete");

        try {
            // 1. Navigate to assets and capture the name of the first asset
            assetPage.navigateToAssets();
            String assetName = assetPage.getFirstRowAssetName();
            Assert.assertNotNull(assetName, "No asset found in grid to delete");
            logStep("Target asset for deletion: " + assetName);

            // 2. Open detail page → kebab → Delete Asset → confirm
            assetPage.deleteFirstAssetFromGrid();
            logStep("Clicked delete on asset: " + assetName);

            assetPage.confirmDelete();
            logStep("Delete confirmed");

            // 3. Wait for delete to complete (redirect back to grid or success toast)
            boolean deleteCompleted = assetPage.waitForDeleteSuccess();
            Assert.assertTrue(deleteCompleted, "Delete did not complete — no success indicator");
            logStepWithScreenshot("Delete completed");

            // 4. Navigate back to assets grid and verify the deleted asset is gone
            assetPage.navigateToAssets();
            logStep("Back on assets grid — searching for deleted asset");

            assetPage.searchAsset(assetName);
            boolean stillVisible = assetPage.isAssetVisible(assetName);
            Assert.assertFalse(stillVisible,
                    "Deleted asset '" + assetName + "' is still visible in the grid after deletion");
            logStepWithScreenshot("Verified: asset '" + assetName + "' no longer in grid");

            ExtentReportManager.logPass("Asset deleted and verified removed: " + assetName);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_delete_error");
            Assert.fail("Asset delete failed: " + e.getMessage());
        }
    }
}
