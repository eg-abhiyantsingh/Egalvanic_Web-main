package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Connection CRUD operations on /connections page.
 *
 * Tests the standalone Connections page:
 *   1. Create a new connection (Source → Target with type)
 *   2. Verify the connection is visible in the grid
 *   3. Delete the connection and verify removal
 *
 * UI Flow: Sidebar → Connections → Grid / Create drawer / Delete dialog
 *
 * Tests share a browser session (inherited from BaseTest).
 * Priority order ensures Create runs before Verify and Delete.
 */
public class ConnectionSmokeTestNG extends BaseTest {

    private static final String CONNECTION_TYPE = "Cable";

    @Test(priority = 1, description = "Smoke: Create a new connection from the Connections page")
    public void testAddConnection() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_CONNECTIONS, AppConstants.FEATURE_ADD_CONNECTION,
                "TC_Connection_Add");

        try {
            // 1. Navigate to Connections page
            connectionPage.navigateToConnections();
            logStep("Navigated to Connections page");

            int beforeCount = connectionPage.getGridRowCount();
            logStep("Grid rows before create: " + beforeCount);
            logStepWithScreenshot("Connections page loaded");

            // 2. Open Create Connection drawer
            connectionPage.openCreateConnectionDrawer();
            logStep("Create Connection drawer opened");

            // 3. Fill the form — select first available from each dropdown
            connectionPage.selectFirstAvailableSource();
            logStep("Selected source node (first available)");

            connectionPage.selectFirstAvailableTarget();
            logStep("Selected target node (first available)");

            connectionPage.selectConnectionType(CONNECTION_TYPE);
            logStep("Selected connection type: " + CONNECTION_TYPE);
            logStepWithScreenshot("Connection form filled");

            // 4. Submit the form
            connectionPage.submitCreateConnection();
            logStep("Create Connection submitted");

            // 5. Verify the connection was created
            boolean success = connectionPage.waitForCreateSuccess();
            logStepWithScreenshot("After connection create");

            int afterCount = connectionPage.getGridRowCount();
            logStep("Grid rows after create: " + afterCount);

            Assert.assertTrue(afterCount > beforeCount || success,
                    "Connection was not created. Before: " + beforeCount + ", After: " + afterCount);

            // Close drawer if still open
            connectionPage.closeDrawer();

            ExtentReportManager.logPass("Connection created (" + CONNECTION_TYPE + ")");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("connection_add_error");
            Assert.fail("Add connection failed: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Smoke: Verify connection is visible in the grid")
    public void testVerifyConnection() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_CONNECTIONS, AppConstants.FEATURE_ADD_CONNECTION,
                "TC_Connection_Verify");

        try {
            // 1. Navigate to Connections page
            connectionPage.navigateToConnections();
            logStep("Navigated to Connections page");

            // 2. Verify grid has data
            boolean hasData = connectionPage.isGridPopulated();
            Assert.assertTrue(hasData, "Connections grid is empty — no connections found");
            logStep("Grid has data rows");

            // 3. Read first row data
            String sourceNode = connectionPage.getFirstRowSourceNode();
            String targetNode = connectionPage.getFirstRowTargetNode();
            String connType = connectionPage.getFirstRowConnectionType();
            logStep("First connection: " + sourceNode + " → " + targetNode + " (" + connType + ")");

            Assert.assertNotNull(sourceNode, "Source node is null");
            Assert.assertNotNull(targetNode, "Target node is null");

            // 4. Check pagination
            String pagination = connectionPage.getPaginationText();
            logStep("Pagination: " + pagination);
            logStepWithScreenshot("Connections grid verified");

            ExtentReportManager.logPass("Connection verified: " + sourceNode + " → " + targetNode);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("connection_verify_error");
            Assert.fail("Verify connection failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, description = "Smoke: Delete a connection and verify removal")
    public void testDeleteConnection() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_CONNECTIONS, AppConstants.FEATURE_DELETE_CONNECTION,
                "TC_Connection_Delete");

        try {
            // 1. Navigate to Connections page
            connectionPage.navigateToConnections();
            logStep("Navigated to Connections page");

            int beforeCount = connectionPage.getGridRowCount();
            Assert.assertTrue(beforeCount > 0,
                    "No connections to delete. Grid rows: " + beforeCount);
            logStep("Grid rows before delete: " + beforeCount);

            // 2. Click Delete on first row
            connectionPage.clickDeleteOnRow(0);
            logStep("Delete clicked on first row");
            logStepWithScreenshot("Delete confirmation dialog");

            // 3. Confirm the deletion
            connectionPage.confirmDelete();
            logStep("Delete confirmed");

            // 4. Wait for delete to complete
            boolean deleteSuccess = connectionPage.waitForDeleteSuccess();
            logStep("Delete result: " + deleteSuccess);

            // 5. Wait for server to process, then hard refresh for truly fresh data
            pause(5000);

            // Use hard page refresh to bypass SPA caching
            driver.navigate().refresh();
            pause(5000);

            // 6. Poll for count decrease (handles eventual consistency)
            boolean deleted = false;
            for (int attempt = 0; attempt < 5; attempt++) {
                int afterCount = connectionPage.getGridRowCount();
                logStep("Post-delete check " + (attempt + 1) + ": grid rows = " + afterCount);
                if (afterCount < beforeCount) {
                    deleted = true;
                    logStep("Grid rows after delete: " + afterCount);
                    break;
                }
                // Hard refresh each retry
                driver.navigate().refresh();
                pause(5000);
            }
            logStepWithScreenshot("Grid after deletion");

            Assert.assertTrue(deleted,
                    "Connection was not deleted. Before: " + beforeCount + ", After: " + connectionPage.getGridRowCount());

            ExtentReportManager.logPass("Connection deleted. Before: " + beforeCount);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("connection_delete_error");
            Assert.fail("Delete connection failed: " + e.getMessage());
        }
    }
}
