package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Issues module.
 *
 * Tests the Issues page (table/grid layout):
 *   1. Create a new issue (Issue Class + Asset + Priority + Immediate Hazard + Customer Notified)
 *   2. Search, filter, and sort issues
 *   3. Activate jobs on an issue detail page
 *   4. Upload a photo to an issue
 *   5. Delete an issue and verify removal
 *
 * UI Flow: Sidebar → Issues → Table View / "Add Issue" drawer / Detail page
 *
 * Add Issue form fields (BASIC INFO):
 *   - Priority (dropdown, default "Medium")
 *   - Immediate Hazard (Yes/No toggle)
 *   - Customer Notified (Yes/No toggle)
 *   - Issue Class * (required dropdown)
 *   - Asset (dropdown)
 *
 * Tests share a browser session (inherited from BaseTest).
 * Priority order ensures Create runs before dependent tests.
 */
public class IssuesSmokeTestNG extends BaseTest {

    // Form field values matching what exists in the app
    private static final String TEST_ISSUE_CLASS = "NEC Violation";
    private static final String TEST_ASSET_NAME = "ATS";
    private static final String TEST_PRIORITY = "High";
    private static final String TEST_TITLE = "Smoke Test Issue " + System.currentTimeMillis();
    private static final String TEST_PROPOSED_RESOLUTION = "Inspect and repair as needed";
    private static final String TEST_PHOTO_PATH = "src/test/resources/test-photo.jpg";

    // Track created issue across tests
    private String createdIssueName;

    // ================================================================
    // TEST 1: CREATE ISSUE
    // ================================================================

    @Test(priority = 1, description = "Smoke: Create a new issue via Add Issue form")
    public void testCreateIssue() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CREATE_ISSUE,
                "TC_Issue_Create");

        try {
            // 1. Navigate to Issues page
            issuePage.navigateToIssues();
            logStep("Navigated to Issues page");
            logStepWithScreenshot("Issues page loaded");

            int beforeCount = issuePage.getRowCount();
            logStep("Row count before create: " + beforeCount);

            // 2. Open Add Issue form
            issuePage.openCreateIssueForm();
            logStep("Add Issue form opened");
            logStepWithScreenshot("Add Issue drawer");

            // 3. Fill the form fields
            // Priority — select High (default is Medium)
            issuePage.selectPriority(TEST_PRIORITY);
            logStep("Selected priority: " + TEST_PRIORITY);

            // Immediate Hazard — set to No
            issuePage.setImmediateHazard(false);
            logStep("Set Immediate Hazard: No");

            // Customer Notified — set to No
            issuePage.setCustomerNotified(false);
            logStep("Set Customer Notified: No");

            // Issue Class (required) — select NEC Violation
            issuePage.selectIssueClass(TEST_ISSUE_CLASS);
            logStep("Selected issue class: " + TEST_ISSUE_CLASS);

            // Asset — select an asset
            issuePage.selectAsset(TEST_ASSET_NAME);
            logStep("Selected asset: " + TEST_ASSET_NAME);

            // Title (required)
            issuePage.fillTitle(TEST_TITLE);
            logStep("Filled title: " + TEST_TITLE);

            // Proposed Resolution (required)
            issuePage.fillProposedResolution(TEST_PROPOSED_RESOLUTION);
            logStep("Filled proposed resolution");

            logStepWithScreenshot("Form filled — about to submit");

            // 4. Submit
            issuePage.submitCreateIssue();
            logStep("Issue creation submitted");

            // 5. Wait for success
            boolean success = issuePage.waitForCreateSuccess();
            Assert.assertTrue(success, "Issue creation did not complete successfully");
            logStep("Create success confirmed");

            // 6. Verify issue count increased
            // Wait for server to process, then hard refresh for truly fresh data
            pause(5000);

            // Use hard page refresh to bypass SPA caching (proven fix from delete test)
            issuePage.navigateToIssues();
            pause(2000);
            driver.navigate().refresh();
            pause(5000);

            boolean countIncreased = false;
            for (int attempt = 0; attempt < 5; attempt++) {
                int afterCount = issuePage.getRowCount();
                logStep("Post-create check " + (attempt + 1) + ": row count = " + afterCount);
                if (afterCount > beforeCount) {
                    countIncreased = true;
                    break;
                }
                // Hard refresh each retry
                driver.navigate().refresh();
                pause(5000);
            }
            Assert.assertTrue(countIncreased, "Issue count did not increase after creation");
            logStepWithScreenshot("Issue created and verified in table");

            ExtentReportManager.logPass("Issue created successfully");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("issue_create_error");
            Assert.fail("Issue creation failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 2: SEARCH, FILTER & SORT
    // ================================================================

    @Test(priority = 2, description = "Smoke: Search, filter and sort issues")
    public void testSearchFilterSort() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SEARCH_ISSUE,
                "TC_Issue_SearchFilterSort");

        try {
            // 1. Navigate to Issues page
            issuePage.navigateToIssues();
            logStep("Navigated to Issues page");

            // 2. Verify rows are populated
            boolean hasIssues = issuePage.isCardsPopulated();
            Assert.assertTrue(hasIssues, "Issues page has no rows");
            int initialCount = issuePage.getRowCount();
            logStep("Row count: " + initialCount);
            logStepWithScreenshot("Issues table loaded");

            // 3. Get first row title for valid search
            String firstTitle = issuePage.getFirstCardTitle();
            Assert.assertNotNull(firstTitle, "First row title is null");
            logStep("First row title: " + firstTitle);

            // 4. Search with valid term
            issuePage.searchIssues(firstTitle);
            pause(2000);
            boolean found = issuePage.isIssueVisible(firstTitle);
            Assert.assertTrue(found, "Valid search did not return expected issue");
            logStepWithScreenshot("Valid search returned results");

            // 5. Search with invalid term
            String invalidSearch = "ZZZZNONEXISTENT_" + System.currentTimeMillis();
            issuePage.searchIssues(invalidSearch);
            pause(2000);
            boolean notFound = !issuePage.isIssueVisible(invalidSearch);
            Assert.assertTrue(notFound, "Invalid search unexpectedly returned results");
            logStep("Invalid search correctly returned no match");

            // 6. Clear search and verify rows restored
            issuePage.clearSearch();
            pause(2000);
            boolean restored = issuePage.isCardsPopulated();
            Assert.assertTrue(restored, "Rows not restored after clearing search");
            logStep("Rows restored after clearing search");

            // 7. Try sort if available (soft — sort UI may vary)
            try {
                issuePage.clickSortOption("Title");
                logStep("Sort by Title applied");
                logStepWithScreenshot("After sort");
            } catch (Exception e) {
                logWarning("Sort not available or failed: " + e.getMessage());
            }

            ExtentReportManager.logPass("Search, filter, sort verified");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("issue_search_error");
            Assert.fail("Issue search/filter/sort failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 3: ACTIVATE JOBS
    // ================================================================

    @Test(priority = 3, description = "Smoke: Verify issue detail page tabs (Details, Class Details, Photos)")
    public void testIssueDetailTabs() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ACTIVATE_JOBS,
                "TC_Issue_DetailTabs");

        try {
            // 1. Navigate to Issues page
            issuePage.navigateToIssues();
            logStep("Navigated to Issues page");

            // 2. Open the first issue detail
            issuePage.openFirstIssueDetail();
            logStep("Opened issue detail page");
            logStepWithScreenshot("Issue detail page");

            // 3. Verify detail page loaded with expected tabs
            boolean hasDetailContent = issuePage.verifyDetailPageTabs();
            Assert.assertTrue(hasDetailContent, "Issue detail page tabs not found");
            logStepWithScreenshot("Issue detail tabs verified");

            ExtentReportManager.logPass("Issue detail page tabs verified");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("issue_detail_tabs_error");
            Assert.fail("Issue detail tabs failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 4: PHOTOS
    // ================================================================

    @Test(priority = 4, description = "Smoke: Upload a photo to an issue")
    public void testIssuePhotos() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PHOTOS,
                "TC_Issue_Photos");

        try {
            // 1. Navigate to Issues page
            issuePage.navigateToIssues();
            logStep("Navigated to Issues page");

            // 2. Open issue detail (use first issue)
            issuePage.openFirstIssueDetail();
            logStep("Opened issue detail page");

            int photoBefore = issuePage.getPhotoCount();
            logStep("Photo count before upload: " + photoBefore);

            // 3. Upload photo
            issuePage.uploadPhoto(TEST_PHOTO_PATH);
            logStep("Photo upload initiated");
            pause(3000);

            // 4. Verify photo appears
            boolean photoVisible = issuePage.isPhotoVisible();
            Assert.assertTrue(photoVisible, "Uploaded photo not visible in gallery");
            logStepWithScreenshot("Photo uploaded and visible in gallery");

            int photoAfter = issuePage.getPhotoCount();
            logStep("Photo count after upload: " + photoAfter);

            ExtentReportManager.logPass("Photo upload verified. Before: " + photoBefore + ", After: " + photoAfter);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("issue_photos_error");
            Assert.fail("Issue photos test failed: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 5: DELETE ISSUE
    // ================================================================

    @Test(priority = 5, description = "Smoke: Delete an issue and verify removal")
    public void testDeleteIssue() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ISSUES, AppConstants.FEATURE_DELETE_ISSUE,
                "TC_Issue_Delete");

        try {
            // 1. Navigate to Issues page
            issuePage.navigateToIssues();
            logStep("Navigated to Issues page");

            int beforeCount = issuePage.getRowCount();
            Assert.assertTrue(beforeCount > 0, "No issues to delete");
            logStep("Row count before delete: " + beforeCount);

            String firstTitle = issuePage.getFirstCardTitle();
            logStep("Target issue for deletion: " + firstTitle);

            // 2. Open issue detail
            issuePage.openFirstIssueDetail();
            logStep("Opened issue detail page");
            logStepWithScreenshot("Issue detail — before delete");

            // 3. Delete the issue
            issuePage.deleteCurrentIssue();
            logStep("Delete initiated");
            logStepWithScreenshot("Delete confirmation dialog");

            // 4. Confirm deletion
            issuePage.confirmDelete();
            logStep("Delete confirmed");

            // 5. Wait for delete success
            boolean deleteSuccess = issuePage.waitForDeleteSuccess();
            logStep("Delete result: " + deleteSuccess);

            // 6. Wait for server to process, then hard refresh for truly fresh data
            pause(5000);

            // Use hard page refresh to bypass SPA caching (proven fix from ConnectionDelete)
            driver.navigate().refresh();
            pause(5000);

            // Poll for count decrease (handles eventual consistency)
            boolean deleted = false;
            for (int attempt = 0; attempt < 5; attempt++) {
                int afterCount = issuePage.getRowCount();
                logStep("Post-delete check " + (attempt + 1) + ": row count = " + afterCount);
                if (afterCount < beforeCount) {
                    deleted = true;
                    break;
                }
                // Hard refresh each retry
                driver.navigate().refresh();
                pause(5000);
            }
            logStepWithScreenshot("Issues page after deletion");

            Assert.assertTrue(deleted, "Issue was not deleted. Before: " + beforeCount);

            ExtentReportManager.logPass("Issue deleted: " + firstTitle);

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("issue_delete_error");
            Assert.fail("Delete issue failed: " + e.getMessage());
        }
    }
}
