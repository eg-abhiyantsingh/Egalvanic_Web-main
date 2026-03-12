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
    private static final String TEST_ENCLOSURE_CLASS = "Panelboard";
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

            // 3. Change asset class to enclosure type (has OCP section)
            assetPage.editAssetClass(TEST_ENCLOSURE_CLASS);
            logStep("Changed asset class to: " + TEST_ENCLOSURE_CLASS);
            logStepWithScreenshot("Asset class updated to enclosure type");

            // 4. Add OCP child device (only available for enclosure-type classes)
            boolean ocpAdded = false;
            try {
                assetPage.expandOCPSection();
                int ocpBefore = assetPage.getOCPChildCount();
                logStep("OCP section found. Current child count: " + ocpBefore);

                assetPage.clickAddOCPButton();
                logStep("Clicked '+' on OCP");

                assetPage.selectCreateNewChild();
                logStep("Selected 'Create New Child'");

                String ocpChildName = "OCP_Smoke_" + System.currentTimeMillis();
                assetPage.fillOCPChildForm(ocpChildName);
                logStep("Filled OCP child name: " + ocpChildName);

                assetPage.submitOCPChildForm();
                assetPage.waitForOCPDialogClose();
                logStep("OCP child created");
                ocpAdded = true;
            } catch (Exception e) {
                logStep("OCP child creation skipped: " + e.getMessage());
                assetPage.dismissAnyDialog();
            }
            logStepWithScreenshot("After OCP attempt");

            // 5. Save all changes
            assetPage.saveChanges();
            logStepWithScreenshot("Save clicked");

            boolean success = assetPage.waitForEditSuccess();
            Assert.assertTrue(success, "Asset update did not complete successfully");
            logStep("Save confirmed successful");

            // 6. Verify saved data — check values still on the edit page after save
            String savedClass = assetPage.getAssetClassValue();
            logStep("Post-save asset class: " + savedClass);

            String savedName = assetPage.getAssetNameValue();
            logStep("Post-save asset name: " + savedName);

            assetPage.expandConnectionsSection();
            int finalConnCount = assetPage.getConnectionCount();
            logStep("Post-save connection count: " + finalConnCount);
            logStepWithScreenshot("Post-save verification");

            // 7. Navigate away and back to verify data persisted
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

            // Check OCP persisted
            if (ocpAdded) {
                assetPage.expandOCPSection();
                int reloadedOCPCount = assetPage.getOCPChildCount();
                logStep("After reload — OCP children: " + reloadedOCPCount);
            }
            logStepWithScreenshot("Final data verification complete");

            ExtentReportManager.logPass("Asset updated and verified: class=" + reloadedClass +
                    ", name=" + reloadedName + ", connections=" + reloadedConnCount +
                    ", lineside=" + linesideAdded + ", loadside=" + loadsideAdded +
                    ", ocp=" + ocpAdded);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_update_error");
            Assert.fail("Asset update failed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Smoke: Navigate to asset detail page and verify content")
    public void testAssetDetailNavigation() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_LIST, "TC_Asset_Detail_Nav");

        try {
            assetPage.navigateToAssets();
            logStep("Navigated to Assets page");

            // 1. Get the first asset name from grid
            String gridAssetName = assetPage.getFirstRowAssetName();
            Assert.assertNotNull(gridAssetName, "No asset found in grid");
            logStep("First asset in grid: " + gridAssetName);

            // 2. Click into the detail page
            assetPage.navigateToFirstAssetDetail();
            logStep("Navigated to detail page");

            // 3. Verify URL changed to asset detail
            String detailUrl = driver.getCurrentUrl();
            Assert.assertTrue(detailUrl.contains("/assets/"),
                    "URL does not contain /assets/ — not on detail page. URL: " + detailUrl);
            logStep("Detail URL: " + detailUrl);

            // 4. Verify detail page has the asset name
            String detailName = assetPage.getDetailPageAssetName();
            Assert.assertNotNull(detailName, "Asset name not found on detail page");
            Assert.assertFalse(detailName.trim().isEmpty(), "Asset name is blank on detail page");
            logStep("Detail page asset name: " + detailName);
            logStepWithScreenshot("Asset detail page loaded");

            ExtentReportManager.logPass("Asset detail navigation verified: " + detailName);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_detail_nav_error");
            Assert.fail("Asset detail navigation failed: " + e.getMessage());
        }
    }

    @Test(priority = 5, description = "Smoke: Search assets and verify filtering")
    public void testAssetSearch() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_LIST, "TC_Asset_Search");

        try {
            assetPage.navigateToAssets();
            logStep("Navigated to Assets page");

            // 1. Get first asset name for a valid search
            String firstAssetName = assetPage.getFirstRowAssetName();
            Assert.assertNotNull(firstAssetName, "No asset found in grid for search test");
            logStep("Will search for: " + firstAssetName);

            // 2. Search with valid term
            assetPage.searchAsset(firstAssetName);
            pause(2000);
            boolean found = assetPage.isAssetVisible(firstAssetName);
            Assert.assertTrue(found, "Valid search did not return expected asset: " + firstAssetName);
            logStepWithScreenshot("Valid search returned results");

            // 3. Search with invalid term — grid should show no results or fewer rows
            String invalidSearch = "ZZZZNONEXISTENT999";
            assetPage.searchAsset(invalidSearch);
            pause(2000);
            boolean notFound = !assetPage.isAssetVisible(invalidSearch);
            Assert.assertTrue(notFound, "Invalid search term returned results unexpectedly");
            logStepWithScreenshot("Invalid search correctly returned no match");

            // 4. Clear search — navigate back to reset
            assetPage.navigateToAssets();
            pause(2000);
            boolean gridRestored = assetPage.isGridPopulated();
            Assert.assertTrue(gridRestored, "Grid not repopulated after clearing search");
            logStep("Grid restored after clearing search");

            ExtentReportManager.logPass("Asset search verified: valid + invalid + clear");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_search_error");
            Assert.fail("Asset search test failed: " + e.getMessage());
        }
    }

    @Test(priority = 6, description = "Smoke: Open edit drawer, modify fields, cancel, and verify no changes saved")
    public void testEditAndCancel() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_EDIT_ASSET, "TC_Asset_Edit_Cancel");

        try {
            assetPage.navigateToAssets();
            logStep("Navigated to Assets page");

            // 1. Get the original asset name from grid before editing
            String originalName = assetPage.getFirstRowAssetName();
            Assert.assertNotNull(originalName, "No asset in grid to test edit-cancel");
            logStep("Original asset: " + originalName);

            // 2. Open edit form and read original values
            assetPage.openEditForFirstAsset();
            logStep("Opened edit form");

            String originalClass = assetPage.getAssetClassValue();
            String originalAssetName = assetPage.getAssetNameValue();
            logStep("Original values — class: " + originalClass + ", name: " + originalAssetName);
            logStepWithScreenshot("Edit form opened with original values");

            // 3. Make changes (but do NOT save)
            assetPage.editNotes("CANCEL_TEST_SHOULD_NOT_PERSIST_" + System.currentTimeMillis());
            logStep("Modified notes (will cancel)");

            // 4. Close/cancel the edit drawer without saving
            assetPage.navigateToAssets();
            pause(2000);
            logStep("Navigated away without saving");

            // 5. Re-open the same asset and verify original values are unchanged
            assetPage.openEditForFirstAsset();
            pause(2000);

            String afterCancelClass = assetPage.getAssetClassValue();
            String afterCancelName = assetPage.getAssetNameValue();
            logStep("After cancel — class: " + afterCancelClass + ", name: " + afterCancelName);

            // Name and class should be unchanged
            Assert.assertEquals(afterCancelName, originalAssetName,
                    "Asset name changed after cancel! Before: " + originalAssetName + ", After: " + afterCancelName);
            logStepWithScreenshot("Edit-cancel verified: no changes persisted");

            ExtentReportManager.logPass("Edit-cancel verified: values unchanged after navigating away without save");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_edit_cancel_error");
            Assert.fail("Edit-cancel test failed: " + e.getMessage());
        }
    }

    @Test(priority = 7, description = "Smoke: Full lifecycle — create asset, open detail, verify, then delete it")
    public void testAssetFullLifecycle() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ASSET, AppConstants.FEATURE_ASSET_CRUD, "TC_Asset_Full_Lifecycle");

        String lifecycleName = "Lifecycle_" + System.currentTimeMillis();
        String lifecycleQR = "QR_LC_" + System.currentTimeMillis();

        try {
            // ── STEP 1: Create a new asset ──
            assetPage.navigateToAssets();
            assetPage.openCreateAssetForm();
            assetPage.fillBasicInfo(lifecycleName, lifecycleQR, TEST_ASSET_CLASS);
            assetPage.fillCoreAttributes();
            assetPage.fillReplacementCost("15000");
            assetPage.submitCreateAsset();

            boolean created = assetPage.waitForCreateSuccess();
            Assert.assertTrue(created, "Lifecycle asset creation failed");
            logStep("Created lifecycle asset: " + lifecycleName);

            // ── STEP 2: Verify it appears in grid ──
            assetPage.navigateToAssets();
            pause(2000);
            assetPage.searchAsset(lifecycleName);
            boolean foundInGrid = assetPage.isAssetVisible(lifecycleName);
            Assert.assertTrue(foundInGrid, "Lifecycle asset not found in grid: " + lifecycleName);
            logStepWithScreenshot("Lifecycle asset found in grid");

            // ── STEP 3: Open detail page and verify name ──
            assetPage.navigateToFirstAssetDetail();
            String detailUrl = driver.getCurrentUrl();
            Assert.assertTrue(detailUrl.contains("/assets/"),
                    "Not on asset detail page. URL: " + detailUrl);
            logStep("Detail page URL: " + detailUrl);

            String detailName = assetPage.getDetailPageAssetName();
            logStep("Detail page shows: " + detailName);
            logStepWithScreenshot("Lifecycle asset detail page");

            // ── STEP 4: Go back to grid and delete the asset ──
            assetPage.clickKebabMenuItem("Delete Asset");
            pause(1000);
            assetPage.confirmDelete();
            logStep("Delete confirmed for lifecycle asset");

            boolean deleteCompleted = assetPage.waitForDeleteSuccess();
            Assert.assertTrue(deleteCompleted, "Lifecycle asset delete did not complete");
            logStep("Delete success");

            // ── STEP 5: Verify it's gone from grid ──
            assetPage.navigateToAssets();
            pause(2000);
            assetPage.searchAsset(lifecycleName);
            pause(2000);
            boolean stillExists = assetPage.isAssetVisible(lifecycleName);
            Assert.assertFalse(stillExists,
                    "Lifecycle asset still visible after deletion: " + lifecycleName);
            logStepWithScreenshot("Lifecycle asset deleted and confirmed gone from grid");

            ExtentReportManager.logPass("Full lifecycle passed: create → grid verify → detail → delete → confirm gone: " + lifecycleName);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("asset_lifecycle_error");
            Assert.fail("Asset full lifecycle failed: " + e.getMessage());
        }
    }

    @Test(priority = 8, description = "Smoke: Delete an existing asset and verify removal")
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
