package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Location CRUD operations.
 * Every test is fully independent — can be run alone from IDE.
 *
 * When run as a suite (priority 1→2→3→4), tests share the building
 * created by testCreateLocationHierarchy to avoid redundant creation.
 * When run individually, each test creates its own test data first.
 */
public class LocationSmokeTestNG extends BaseTest {

    private static final String ACCESS_NOTES = "Smoke test access notes";

    // Tracks the building name in this session (updated if renamed)
    private String currentBuildingName;

    /**
     * Helper: create a test building and return its name.
     * Called by any test that needs a pre-existing building.
     */
    private String createTestBuilding() {
        String name = "SmokeTest_Building_" + System.currentTimeMillis();
        System.out.println("[Setup] Creating test building: " + name);

        locationPage.navigateToLocations();
        Assert.assertTrue(locationPage.isOnLocationsPage(), "Not on Locations page");

        locationPage.createBuilding(name, ACCESS_NOTES);

        boolean visible = locationPage.isLocationVisible(name);
        if (!visible) {
            throw new RuntimeException("Setup failed: building not visible after creation: " + name);
        }
        System.out.println("[Setup] Test building created: " + name);
        return name;
    }

    /**
     * Ensures a building exists for this session.
     * If running full suite, testCreateLocationHierarchy already set currentBuildingName.
     * If running a single test, creates a building on demand.
     */
    private void ensureBuildingExists() {
        if (currentBuildingName == null) {
            currentBuildingName = createTestBuilding();
        }
    }

    @Test(priority = 1, description = "Smoke: Create Building and Floor")
    public void testCreateLocationHierarchy() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_LOCATION_CRUD, "TC_Location_Create");

        try {
            String buildingName = "SmokeTest_Building_" + System.currentTimeMillis();
            String floorName = "SmokeTest_Floor_" + System.currentTimeMillis();

            locationPage.navigateToLocations();
            Assert.assertTrue(locationPage.isOnLocationsPage(), "Not on Locations page");
            logStepWithScreenshot("Locations page loaded");

            // Create Building
            locationPage.createBuilding(buildingName, ACCESS_NOTES);
            logStep("Building created: " + buildingName);

            // Verify building appears in tree
            boolean buildingVisible = locationPage.isLocationVisible(buildingName);
            Assert.assertTrue(buildingVisible, "Building not visible after creation: " + buildingName);
            logStepWithScreenshot("Building verified in tree");

            // Create Floor under Building
            locationPage.createFloor(buildingName, floorName, ACCESS_NOTES + " floor");
            logStep("Floor creation dialog completed: " + floorName);
            logStepWithScreenshot("Floor created");

            currentBuildingName = buildingName;
            ExtentReportManager.logPass("Building and Floor created successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_create_error");
            Assert.fail("Location creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Verify building is visible in tree")
    public void testReadLocations() {
        ensureBuildingExists();
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_LOCATION_LIST, "TC_Location_Read");

        try {
            locationPage.navigateToLocations();

            boolean buildingVisible = locationPage.isLocationVisible(currentBuildingName);
            Assert.assertTrue(buildingVisible, "Building not visible: " + currentBuildingName);
            logStep("Building visible: " + currentBuildingName);

            logStepWithScreenshot("Locations verified");
            ExtentReportManager.logPass("Building is visible in the tree");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_read_error");
            Assert.fail("Location read failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Update building name")
    public void testUpdateLocation() {
        ensureBuildingExists();
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_EDIT_LOCATION, "TC_Location_Update");

        String updatedName = currentBuildingName + "_Updated";
        try {
            locationPage.navigateToLocations();
            locationPage.selectAndEditLocation(currentBuildingName, updatedName);
            logStepWithScreenshot("Location updated");

            currentBuildingName = updatedName; // track the rename
            ExtentReportManager.logPass("Location updated successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_update_error");
            Assert.fail("Location update failed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Smoke: Delete the created building")
    public void testDeleteLocation() {
        ensureBuildingExists();
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_DELETE_LOCATION, "TC_Location_Delete");

        try {
            locationPage.navigateToLocations();
            locationPage.deleteLocation(currentBuildingName);
            locationPage.confirmDelete();
            logStepWithScreenshot("Location deleted");

            currentBuildingName = null; // building is gone
            ExtentReportManager.logPass("Location deleted successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_delete_error");
            Assert.fail("Location delete failed: " + e.getMessage());
        }
    }
}
