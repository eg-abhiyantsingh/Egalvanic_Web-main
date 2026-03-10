package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Location CRUD operations.
 * All 4 tests share ONE browser session (via BaseTest @BeforeClass).
 *
 * Execution order is controlled by priority (1→2→3→4).
 * No dependsOnMethods — each test can be run individually from IDE.
 * When run individually, dependent tests skip with a clear message.
 */
public class LocationSmokeTestNG extends BaseTest {

    // Test data with unique timestamps
    private static final String BUILDING_NAME = "SmokeTest_Building_" + System.currentTimeMillis();
    private static final String FLOOR_NAME = "SmokeTest_Floor_" + System.currentTimeMillis();
    private static final String ACCESS_NOTES = "Smoke test access notes";

    // State tracking
    private boolean buildingCreated = false;

    @Test(priority = 1, description = "Smoke: Create Building and Floor")
    public void testCreateLocationHierarchy() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_LOCATION_CRUD, "TC_Location_Create");

        try {
            locationPage.navigateToLocations();
            Assert.assertTrue(locationPage.isOnLocationsPage(), "Not on Locations page");
            logStepWithScreenshot("Locations page loaded");

            // Create Building
            locationPage.createBuilding(BUILDING_NAME, ACCESS_NOTES);
            logStep("Building created: " + BUILDING_NAME);

            // Verify building appears in tree
            boolean buildingVisible = locationPage.isLocationVisible(BUILDING_NAME);
            Assert.assertTrue(buildingVisible, "Building not visible after creation: " + BUILDING_NAME);
            logStepWithScreenshot("Building verified in tree");

            // Create Floor under Building (verified via dialog completion —
            // the Locations tree uses lazy loading so floors are not visible in the flat tree)
            locationPage.createFloor(BUILDING_NAME, FLOOR_NAME, ACCESS_NOTES + " floor");
            logStep("Floor creation dialog completed: " + FLOOR_NAME);
            logStepWithScreenshot("Floor created");

            buildingCreated = true;
            ExtentReportManager.logPass("Building and Floor created successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_create_error");
            Assert.fail("Location creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Verify building is visible in tree")
    public void testReadLocations() {
        requireBuildingCreated("testReadLocations");
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_LOCATION_LIST, "TC_Location_Read");

        try {
            locationPage.navigateToLocations();

            boolean buildingVisible = locationPage.isLocationVisible(BUILDING_NAME);
            Assert.assertTrue(buildingVisible, "Building not visible: " + BUILDING_NAME);
            logStep("Building visible: " + BUILDING_NAME);

            logStepWithScreenshot("Locations verified");
            ExtentReportManager.logPass("Building is visible in the tree");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_read_error");
            Assert.fail("Location read failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Update building name")
    public void testUpdateLocation() {
        requireBuildingCreated("testUpdateLocation");
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_EDIT_LOCATION, "TC_Location_Update");

        String updatedName = BUILDING_NAME + "_Updated";
        try {
            locationPage.navigateToLocations();
            locationPage.selectAndEditLocation(BUILDING_NAME, updatedName);
            logStepWithScreenshot("Location updated");

            ExtentReportManager.logPass("Location updated successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_update_error");
            Assert.fail("Location update failed: " + e.getMessage());
        }
    }

    @Test(priority = 4, description = "Smoke: Delete the created building")
    public void testDeleteLocation() {
        requireBuildingCreated("testDeleteLocation");
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_DELETE_LOCATION, "TC_Location_Delete");

        try {
            locationPage.navigateToLocations();
            locationPage.deleteLocation(BUILDING_NAME);
            locationPage.confirmDelete();
            logStepWithScreenshot("Location deleted");

            ExtentReportManager.logPass("Location deleted successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_delete_error");
            Assert.fail("Location delete failed: " + e.getMessage());
        }
    }

    /**
     * Check precondition: building must have been created by testCreateLocationHierarchy.
     */
    private void requireBuildingCreated(String testName) {
        if (!buildingCreated) {
            throw new SkipException(testName + " requires testCreateLocationHierarchy to run first. "
                    + "Run the full class or suite, not a single method.");
        }
    }
}
