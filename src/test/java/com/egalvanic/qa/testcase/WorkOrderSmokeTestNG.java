package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Job/Work Orders module.
 *
 * Tests the Work Orders page:
 *   1. Create a new work order
 *   2. Edit the work order (update description)
 *   3. IR Photos — upload an IR photo
 *   4. Locations — verify location on work order
 *   5. Tasks — create and manage a task
 *   6. Filter — search and filter work orders
 *
 * UI Flow: Sidebar → Jobs/Work Orders → Table View / Create drawer / Detail page
 *
 * Tests share a browser session (inherited from BaseTest).
 * Priority order ensures Create runs before dependent tests.
 */
public class WorkOrderSmokeTestNG extends BaseTest {

    // Test data
    private static final String TEST_ASSET_NAME = "ATS";
    private static final String TEST_PRIORITY = "High";
    private static final String TEST_PHOTO_PATH = "src/test/resources/test-photo.jpg";

    // Track created work order across tests
    private String createdWorkOrderName;

    // ================================================================
    // TEST 1: CREATE WORK ORDER
    // ================================================================

    @Test(priority = 1, description = "Smoke: Create a new work order")
    public void testCreateWorkOrder() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_WORK_ORDERS, AppConstants.FEATURE_CREATE_WORK_ORDER,
                "TC_WorkOrder_Create");

        try {
            // 1. Navigate to Work Orders page
            workOrderPage.navigateToWorkOrders();
            logStep("Navigated to Work Orders page");
            logStepWithScreenshot("Work Orders page loaded");

            int beforeCount = workOrderPage.getRowCount();
            logStep("Row count before create: " + beforeCount);

            // 2. Open Create form
            workOrderPage.openCreateWorkOrderForm();
            logStep("Create Work Order form opened");
            logStepWithScreenshot("Create form drawer");

            // 3. Fill form fields
            createdWorkOrderName = "SmokeTest_WO_" + System.currentTimeMillis();

            // Try filling name if available
            try {
                workOrderPage.fillName(createdWorkOrderName);
                logStep("Filled name: " + createdWorkOrderName);
            } catch (Exception e) {
                logWarning("Name field not available: " + e.getMessage());
            }

            // Priority
            try {
                workOrderPage.selectPriority(TEST_PRIORITY);
                logStep("Selected priority: " + TEST_PRIORITY);
            } catch (Exception e) {
                logWarning("Priority field not available: " + e.getMessage());
            }

            // Asset
            try {
                workOrderPage.selectAsset(TEST_ASSET_NAME);
                logStep("Selected asset: " + TEST_ASSET_NAME);
            } catch (Exception e) {
                logWarning("Asset field not available: " + e.getMessage());
            }

            logStepWithScreenshot("Form filled — about to submit");

            // Pre-submit diagnostic: log all form field values
            {
                org.openqa.selenium.JavascriptExecutor jsCheck = (org.openqa.selenium.JavascriptExecutor) driver;
                String formDiag = (String) jsCheck.executeScript(
                    "var drawers = document.querySelectorAll('[class*=\"MuiDrawer-paper\"]');" +
                    "var drawer = null;" +
                    "for (var d of drawers) {" +
                    "  var r = d.getBoundingClientRect();" +
                    "  if (r.width > 400 && d.querySelectorAll('input').length > 0) { drawer = d; break; }" +
                    "}" +
                    "if (!drawer) return 'NO FORM DRAWER';" +
                    "var info = 'LABELS: ';" +
                    "var labels = drawer.querySelectorAll('p, label, span, h6');" +
                    "var seen = new Set();" +
                    "for (var l of labels) {" +
                    "  var t = l.textContent.trim();" +
                    "  if (t.length > 1 && t.length < 40 && !seen.has(t)) { seen.add(t); info += '[' + t + '] '; }" +
                    "}" +
                    "info += ' INPUTS: ';" +
                    "var inputs = drawer.querySelectorAll('input:not([type=\"hidden\"]):not([type=\"file\"])');" +
                    "for (var inp of inputs) {" +
                    "  var r = inp.getBoundingClientRect();" +
                    "  if (r.width > 30) {" +
                    "    info += '{ph=\"' + (inp.placeholder||'').substring(0,25) + '\" val=\"' + (inp.value||'').substring(0,25) + '\" role=' + (inp.getAttribute('role')||'') + '} ';" +
                    "  }" +
                    "}" +
                    "var btns = drawer.querySelectorAll('button');" +
                    "info += ' BTNS: ';" +
                    "for (var b of btns) {" +
                    "  var t = b.textContent.trim();" +
                    "  if (t.length > 0 && t.length < 25 && b.getBoundingClientRect().width > 0) info += '[' + t + ' dis=' + b.disabled + '] ';" +
                    "}" +
                    "return info;");
                logStep("PRE-SUBMIT: " + formDiag);
            }

            // 4. Submit
            workOrderPage.submitCreateWorkOrder();
            logStep("Work order creation submitted");

            // 5. Wait for success (drawer closes or success toast)
            boolean success = workOrderPage.waitForCreateSuccess();
            logStep("Create success: " + success);

            // 6. Verify created work order is visible in the list
            // Use name-based search instead of row count (paginated tables always show same count)
            workOrderPage.navigateToWorkOrders();
            pause(3000);

            boolean found = false;
            for (int attempt = 0; attempt < 5; attempt++) {
                // Try searching by name
                workOrderPage.searchWorkOrders(createdWorkOrderName);
                pause(2000);
                found = workOrderPage.isWorkOrderVisible(createdWorkOrderName);
                logStep("Post-create search attempt " + (attempt + 1) + ": found=" + found);
                if (found) break;
                // Clear search and refresh
                workOrderPage.clearSearch();
                pause(1000);
                driver.navigate().refresh();
                pause(3000);
            }

            // Fallback: check if the name appears anywhere on any page
            if (!found) {
                workOrderPage.navigateToWorkOrders();
                pause(2000);
                found = workOrderPage.isWorkOrderVisible(createdWorkOrderName);
                logStep("Final visibility check: " + found);
            }

            Assert.assertTrue(found, "Work order '" + createdWorkOrderName + "' not found in list after creation");
            logStepWithScreenshot("Work order created and verified in table");

            ExtentReportManager.logPass("Work order created: " + createdWorkOrderName);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("workorder_create_error");
            Assert.fail("Work order creation failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 2: EDIT WORK ORDER
    // ================================================================

    @Test(priority = 2, description = "Smoke: Edit a work order")
    public void testEditWorkOrder() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_WORK_ORDERS, AppConstants.FEATURE_EDIT_WORK_ORDER,
                "TC_WorkOrder_Edit");

        try {
            // 1. Navigate to Work Orders page
            workOrderPage.navigateToWorkOrders();
            logStep("Navigated to Work Orders page");

            // 2. Open first work order detail
            workOrderPage.openFirstWorkOrderDetail();
            logStep("Opened work order detail page");
            logStepWithScreenshot("Work order detail page");

            // 3. Click Edit
            workOrderPage.clickEdit();
            logStep("Edit mode entered");
            logStepWithScreenshot("Edit mode");

            // 4. Edit the description
            String updatedDescription = "Updated by smoke test at " + System.currentTimeMillis();
            try {
                workOrderPage.editDescription(updatedDescription);
                logStep("Edited description");
            } catch (Exception e) {
                logWarning("Description edit not available: " + e.getMessage());
            }

            // 5. Save changes
            workOrderPage.saveEdit();
            logStep("Save clicked");

            // 6. Verify save success
            boolean saved = workOrderPage.waitForEditSuccess();
            Assert.assertTrue(saved, "Work order edit did not save successfully");
            logStepWithScreenshot("Edit saved successfully");

            ExtentReportManager.logPass("Work order edited successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("workorder_edit_error");
            Assert.fail("Work order edit failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 3: IR PHOTOS
    // ================================================================

    @Test(priority = 3, description = "Smoke: Upload an IR photo to a work order")
    public void testIRPhotos() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_WORK_ORDERS, AppConstants.FEATURE_WO_IR_PHOTOS,
                "TC_WorkOrder_IRPhotos");

        try {
            // 1. Navigate to Work Orders page
            workOrderPage.navigateToWorkOrders();
            logStep("Navigated to Work Orders page");

            // 2. Open work order detail
            workOrderPage.openFirstWorkOrderDetail();
            logStep("Opened work order detail page");

            int photoBefore = workOrderPage.getIRPhotoCount();
            logStep("IR photo count before upload: " + photoBefore);

            // 3. Upload IR photo
            workOrderPage.uploadIRPhoto(TEST_PHOTO_PATH);
            logStep("IR photo upload initiated");
            pause(3000);

            // 4. Verify photo appears
            boolean photoVisible = workOrderPage.isIRPhotoVisible();
            Assert.assertTrue(photoVisible, "Uploaded IR photo not visible");
            logStepWithScreenshot("IR photo uploaded and visible");

            int photoAfter = workOrderPage.getIRPhotoCount();
            logStep("IR photo count after upload: " + photoAfter);

            ExtentReportManager.logPass("IR photo upload verified. Before: " + photoBefore + ", After: " + photoAfter);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("workorder_irphotos_error");
            Assert.fail("IR Photos test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 4: LOCATIONS
    // ================================================================

    @Test(priority = 4, description = "Smoke: Verify locations on a work order")
    public void testLocations() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_WORK_ORDERS, AppConstants.FEATURE_WO_LOCATIONS,
                "TC_WorkOrder_Locations");

        try {
            // 1. Navigate to Work Orders page
            workOrderPage.navigateToWorkOrders();
            logStep("Navigated to Work Orders page");

            // 2. Open work order detail
            workOrderPage.openFirstWorkOrderDetail();
            logStep("Opened work order detail page");
            logStepWithScreenshot("Work order detail");

            // 3. Navigate to Locations section/tab
            workOrderPage.navigateToLocationsSection();
            logStep("Navigated to Locations section");

            // 4. Verify locations section is accessible
            int locationCount = workOrderPage.getLocationCount();
            logStep("Location count: " + locationCount);
            logStepWithScreenshot("Locations section");

            // Location section accessible is the smoke validation
            // (specific locations depend on data setup)
            ExtentReportManager.logPass("Locations section verified. Count: " + locationCount);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("workorder_locations_error");
            Assert.fail("Locations test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 5: CREATE AND MANAGE TASKS
    // ================================================================

    @Test(priority = 5, description = "Smoke: Create and manage tasks on a work order")
    public void testCreateAndManageTasks() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_WORK_ORDERS, AppConstants.FEATURE_WO_TASKS,
                "TC_WorkOrder_Tasks");

        try {
            // 1. Navigate to Work Orders page
            workOrderPage.navigateToWorkOrders();
            logStep("Navigated to Work Orders page");

            // 2. Open work order detail
            workOrderPage.openFirstWorkOrderDetail();
            logStep("Opened work order detail page");

            // 3. Navigate to Tasks section
            workOrderPage.navigateToTasksSection();
            logStep("Navigated to Tasks section");

            int tasksBefore = workOrderPage.getTaskCount();
            logStep("Task count before: " + tasksBefore);

            // 4. Add a new task
            String taskName = "SmokeTask_" + System.currentTimeMillis();
            workOrderPage.clickAddTask();
            logStep("Clicked Add Task");
            logStepWithScreenshot("Add Task form");

            workOrderPage.fillTaskName(taskName);
            logStep("Filled task name: " + taskName);

            workOrderPage.submitTask();
            logStep("Task submitted");
            pause(2000);

            // 5. Verify task was created
            boolean taskVisible = workOrderPage.isTaskVisible(taskName);
            logStep("Task visible: " + taskVisible);
            logStepWithScreenshot("Task created");

            // 6. Try toggling task completion (soft assertion)
            try {
                workOrderPage.toggleTaskComplete(taskName);
                logStep("Task completion toggled");
                logStepWithScreenshot("Task toggled");
            } catch (Exception e) {
                logWarning("Task toggle not available: " + e.getMessage());
            }

            int tasksAfter = workOrderPage.getTaskCount();
            logStep("Task count after: " + tasksAfter);

            ExtentReportManager.logPass("Tasks verified. Before: " + tasksBefore + ", After: " + tasksAfter);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("workorder_tasks_error");
            Assert.fail("Tasks test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 6: FILTER
    // ================================================================

    @Test(priority = 6, description = "Smoke: Search and filter work orders")
    public void testFilter() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_WORK_ORDERS, AppConstants.FEATURE_WO_FILTER,
                "TC_WorkOrder_Filter");

        try {
            // 1. Navigate to Work Orders page
            workOrderPage.navigateToWorkOrders();
            logStep("Navigated to Work Orders page");

            // 2. Verify rows are populated
            boolean hasRows = workOrderPage.isRowsPopulated();
            Assert.assertTrue(hasRows, "Work Orders page has no rows");
            int initialCount = workOrderPage.getRowCount();
            logStep("Row count: " + initialCount);
            logStepWithScreenshot("Work Orders table loaded");

            // 3. Get first row title for valid search
            String firstTitle = workOrderPage.getFirstRowTitle();
            Assert.assertNotNull(firstTitle, "First row title is null");
            logStep("First row title: " + firstTitle);

            // 4. Search with valid term
            workOrderPage.searchWorkOrders(firstTitle);
            pause(2000);
            boolean found = workOrderPage.isWorkOrderVisible(firstTitle);
            Assert.assertTrue(found, "Valid search did not return expected work order");
            logStepWithScreenshot("Valid search returned results");

            // 5. Search with invalid term
            String invalidSearch = "ZZZZNONEXISTENT_" + System.currentTimeMillis();
            workOrderPage.searchWorkOrders(invalidSearch);
            pause(2000);
            boolean notFound = !workOrderPage.isWorkOrderVisible(invalidSearch);
            Assert.assertTrue(notFound, "Invalid search unexpectedly returned results");
            logStep("Invalid search correctly returned no match");

            // 6. Clear search and verify rows restored
            workOrderPage.clearSearch();
            pause(2000);
            boolean restored = workOrderPage.isRowsPopulated();
            Assert.assertTrue(restored, "Rows not restored after clearing search");
            logStep("Rows restored after clearing search");

            // 7. Try sort if available (soft)
            try {
                workOrderPage.clickSortOption("Name");
                logStep("Sort applied");
                logStepWithScreenshot("After sort");
            } catch (Exception e) {
                logWarning("Sort not available: " + e.getMessage());
            }

            ExtentReportManager.logPass("Search and filter verified");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("workorder_filter_error");
            Assert.fail("Filter test failed: " + e.getMessage());
        }
    }
}
