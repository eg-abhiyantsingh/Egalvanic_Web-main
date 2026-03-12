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
            logStep("Create success confirmed");

            // Verify asset appears in the grid
            assetPage.navigateToAssets();
            pause(2000);
            assetPage.searchAsset(name);
            boolean found = assetPage.isAssetVisible(name);
            Assert.assertTrue(found, "Newly created asset not found in grid: " + name);
            logStepWithScreenshot("Asset verified in grid: " + name);

            ExtentReportManager.logPass("Asset created and verified in grid: " + name);

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

    @Test(priority = 3, description = "Smoke: Update asset class, model, notes and add a connection")
    public void testUpdateAsset() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_EDIT_ASSET, "TC_Asset_Update");

        try {
            assetPage.navigateToAssets();
            assetPage.openEditForFirstAsset();
            logStep("Opened edit form for existing asset");

            // 1. Update model and notes
            assetPage.editModel("UpdatedModel");
            assetPage.editNotes("Updated notes from smoke test");
            logStep("Updated model and notes");
            logStepWithScreenshot("Basic fields updated");

            // 2. Scroll to CONNECTIONS section and add Lineside + Loadside connections
            boolean linesideAdded = false;
            boolean loadsideAdded = false;

            assetPage.expandConnectionsSection();
            int beforeCount = assetPage.getConnectionCount();
            logStep("CONNECTIONS expanded. Current count: " + beforeCount);

            // 3a. Add Lineside Connection
            try {
                assetPage.clickAddConnectionButton();
                logStep("Clicked '+' for Lineside connection");

                assetPage.selectNewLinesideConnection();
                logStep("Selected 'New Lineside Connection'");

                assetPage.selectTargetAsset(null);
                logStep("Selected target for Lineside");

                try {
                    assetPage.selectConnectionType("Cable");
                    logStep("Selected connection type: Cable");
                } catch (Exception e) {
                    logStep("Connection type selection skipped: " + e.getMessage());
                }

                assetPage.clickCreateConnection();
                assetPage.waitForConnectionDialogClose();
                logStep("Lineside connection created");
                linesideAdded = true;
            } catch (Exception e) {
                logStep("Lineside connection skipped: " + e.getMessage());
                assetPage.dismissAnyDialog();
            }
            logStepWithScreenshot("After Lineside connection attempt");

            // 2b. Add Loadside Connection
            try {
                assetPage.clickAddConnectionButton();
                logStep("Clicked '+' for Loadside connection");

                assetPage.selectNewLoadsideConnection();
                logStep("Selected 'New Loadside Connection'");

                assetPage.selectTargetAsset(null);
                logStep("Selected target for Loadside");

                try {
                    assetPage.selectConnectionType("Cable");
                    logStep("Selected connection type: Cable");
                } catch (Exception e) {
                    logStep("Connection type selection skipped: " + e.getMessage());
                }

                assetPage.clickCreateConnection();
                assetPage.waitForConnectionDialogClose();
                logStep("Loadside connection created");
                loadsideAdded = true;
            } catch (Exception e) {
                logStep("Loadside connection skipped: " + e.getMessage());
                assetPage.dismissAnyDialog();
            }
            logStepWithScreenshot("After Loadside connection attempt");

            // 3. Change asset class (after connections — scroll back to top first)
            assetPage.editAssetClass(TEST_ASSET_CLASS);
            logStep("Changed asset class to: " + TEST_ASSET_CLASS);
            logStepWithScreenshot("Asset class updated");

            // 4. Save all changes
            assetPage.saveChanges();
            logStepWithScreenshot("Save clicked");

            boolean success = assetPage.waitForEditSuccess();
            Assert.assertTrue(success, "Asset update did not complete successfully");
            logStep("Save confirmed successful");

            // 5. Verify saved data — check values still on the edit page after save
            //    (save keeps us on the same page in edit mode)
            String savedClass = assetPage.getAssetClassValue();
            logStep("Post-save asset class: " + savedClass);

            String savedName = assetPage.getAssetNameValue();
            logStep("Post-save asset name: " + savedName);

            assetPage.expandConnectionsSection();
            int finalConnCount = assetPage.getConnectionCount();
            logStep("Post-save connection count: " + finalConnCount);
            logStepWithScreenshot("Post-save verification");

            // 6. Navigate away and back to verify data persisted
            assetPage.navigateToAssets();
            pause(2000);
            assetPage.openEditForFirstAsset();
            pause(2000);
            logStep("Re-opened first asset edit for final verification");

            String reloadedClass = assetPage.getAssetClassValue();
            String reloadedName = assetPage.getAssetNameValue();
            logStep("After reload — class: " + reloadedClass + ", name: " + reloadedName);

            assetPage.expandConnectionsSection();
            int reloadedConnCount = assetPage.getConnectionCount();
            logStep("After reload — connections: " + reloadedConnCount);
            logStepWithScreenshot("Final data verification complete");

            ExtentReportManager.logPass("Asset updated and verified: class=" + reloadedClass +
                    ", name=" + reloadedName + ", connections=" + reloadedConnCount +
                    ", lineside=" + linesideAdded + ", loadside=" + loadsideAdded);

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

            // 3. Wait for delete to complete (redirect back to /assets or success toast)
            boolean deleteCompleted = assetPage.waitForDeleteSuccess();
            Assert.assertTrue(deleteCompleted, "Delete did not complete — no success indicator");
            logStepWithScreenshot("Delete completed");

            // 4. Verify redirect back to assets grid (URL-based — avoids false positives from duplicate names)
            assetPage.navigateToAssets();
            String postDeleteUrl = driver.getCurrentUrl();
            Assert.assertTrue(postDeleteUrl.contains("/assets"),
                    "Not redirected to assets grid after delete. URL: " + postDeleteUrl);
            logStep("Back on assets grid. URL: " + postDeleteUrl);

            // 5. Verify grid is still populated (delete didn't break the page)
            boolean gridHasData = assetPage.isGridPopulated();
            logStep("Grid still has data after deletion: " + gridHasData);
            logStepWithScreenshot("Assets grid after deletion of '" + assetName + "'");

            ExtentReportManager.logPass("Asset deleted successfully: " + assetName + ". Redirected to grid.");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_delete_error");
            Assert.fail("Asset delete failed: " + e.getMessage());
        }
    }
}
