package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Location CRUD operations
 * Extends BaseTest for driver lifecycle, login, and reporting
 * Create Building > Floor > Room, Read, Update, Delete
 */
public class LocationSmokeTestNG extends BaseTest {

    // Test data with unique timestamps
    private static final String BUILDING_NAME = "SmokeTest_Building_" + System.currentTimeMillis();
    private static final String FLOOR_NAME = "SmokeTest_Floor_" + System.currentTimeMillis();
    private static final String ROOM_NAME = "SmokeTest_Room_" + System.currentTimeMillis();
    private static final String ACCESS_NOTES = "Smoke test access notes";

    @Test(priority = 1, description = "Smoke: Create Building > Floor > Room hierarchy")
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
            logStepWithScreenshot("Building created");

            // Create Floor under Building
            locationPage.createFloor(BUILDING_NAME, FLOOR_NAME, ACCESS_NOTES + " floor");
            logStep("Floor created: " + FLOOR_NAME);
            logStepWithScreenshot("Floor created");

            // Create Room under Floor
            locationPage.createRoom(FLOOR_NAME, ROOM_NAME, ACCESS_NOTES + " room");
            logStep("Room created: " + ROOM_NAME);
            logStepWithScreenshot("Room created");

            ExtentReportManager.logPass("Full location hierarchy created successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_create_error");
            Assert.fail("Location creation failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Verify created locations are visible", dependsOnMethods = "testCreateLocationHierarchy")
    public void testReadLocations() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_LOCATION_LIST, "TC_Location_Read");

        try {
            locationPage.navigateToLocations();

            boolean buildingVisible = locationPage.isLocationVisible(BUILDING_NAME);
            Assert.assertTrue(buildingVisible, "Building not visible: " + BUILDING_NAME);
            logStep("Building visible: " + BUILDING_NAME);

            locationPage.expandNode(BUILDING_NAME);

            boolean floorVisible = locationPage.isLocationVisible(FLOOR_NAME);
            Assert.assertTrue(floorVisible, "Floor not visible: " + FLOOR_NAME);
            logStep("Floor visible: " + FLOOR_NAME);

            logStepWithScreenshot("Locations verified");
            ExtentReportManager.logPass("All created locations are visible in the tree");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_read_error");
            Assert.fail("Location read failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Update building name", dependsOnMethods = "testReadLocations")
    public void testUpdateLocation() {
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

    @Test(priority = 4, description = "Smoke: Delete the created room", dependsOnMethods = "testCreateLocationHierarchy")
    public void testDeleteLocation() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_LOCATIONS, AppConstants.FEATURE_DELETE_LOCATION, "TC_Location_Delete");

        try {
            locationPage.navigateToLocations();
            locationPage.expandNode(BUILDING_NAME);
            locationPage.expandNode(FLOOR_NAME);

            locationPage.deleteLocation(ROOM_NAME);
            locationPage.confirmDelete();
            logStepWithScreenshot("Location deleted");

            ExtentReportManager.logPass("Location deleted successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("location_delete_error");
            Assert.fail("Location delete failed: " + e.getMessage());
        }
    }
}
