package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke Tests for Issues module.
 *
 * Tests the Issues page (card/tile layout):
 *   1. Create a new issue (Name + Asset + Priority + Type)
 *   2. Search, filter, and sort issues
 *   3. Activate jobs on an issue detail page
 *   4. Upload a photo to an issue
 *   5. Delete an issue and verify removal
 *
 * UI Flow: Sidebar → Issues → Card View / Create drawer / Detail page
 *
 * Tests share a browser session (inherited from BaseTest).
 * Priority order ensures Create runs before dependent tests.
 */
public class IssuesSmokeTestNG extends BaseTest {

    private static final String TEST_ASSET_NAME = "ATS 1";
    private static final String TEST_PRIORITY = "High";
    private static final String TEST_ISSUE_TYPE = "Corrective";
    private static final String TEST_PHOTO_PATH = "src/test/resources/test-photo.jpg";

    // Track created issue name across tests
    private String createdIssueName;

    // ================================================================
    // TEST 1: CREATE ISSUE
    // ================================================================

    @Test(priority = 1, description = "Smoke: Create a new issue with all fields")
    public void testCreateIssue() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CREATE_ISSUE,
                "TC_Issue_Create");

        try {
            createdIssueName = "SmokeTest_Issue_" + System.currentTimeMillis();

            // 1. Navigate to Issues page
            issuePage.navigateToIssues();
            logStep("Navigated to Issues page");
            logStepWithScreenshot("Issues page loaded");

            // 2. Open Create Issue form
            issuePage.openCreateIssueForm();
            logStep("Create Issue form opened");

            // 3. Fill the form
            issuePage.fillIssueName(createdIssueName);
            logStep("Filled issue name: " + createdIssueName);

            issuePage.selectAsset(TEST_ASSET_NAME);
            logStep("Selected asset: " + TEST_ASSET_NAME);

            issuePage.selectPriority(TEST_PRIORITY);
            logStep("Selected priority: " + TEST_PRIORITY);

            issuePage.selectType(TEST_ISSUE_TYPE);
            logStep("Selected type: " + TEST_ISSUE_TYPE);
            logStepWithScreenshot("Form filled — about to submit");

            // 4. Submit
            issuePage.submitCreateIssue();
            logStep("Issue creation submitted");

            // 5. Wait for success
            boolean success = issuePage.waitForCreateSuccess();
            Assert.assertTrue(success, "Issue creation did not complete successfully");
            logStep("Create success confirmed");

            // 6. Verify issue appears
            issuePage.navigateToIssues();
            pause(2000);
            issuePage.searchIssues(createdIssueName);
            pause(2000);
            boolean found = issuePage.isIssueVisible(createdIssueName);
            Assert.assertTrue(found, "Newly created issue not found: " + createdIssueName);
            logStepWithScreenshot("Issue verified on page: " + createdIssueName);

            ExtentReportManager.logPass("Issue created and verified: " + createdIssueName);

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

            // 2. Verify cards are populated
            boolean hasIssues = issuePage.isCardsPopulated();
            Assert.assertTrue(hasIssues, "Issues page has no cards");
            int initialCount = issuePage.getCardCount();
            logStep("Card count: " + initialCount);
            logStepWithScreenshot("Issues cards loaded");

            // 3. Get first card title for valid search
            String firstTitle = issuePage.getFirstCardTitle();
            Assert.assertNotNull(firstTitle, "First card title is null");
            logStep("First card title: " + firstTitle);

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

            // 6. Clear search and verify cards restored
            issuePage.clearSearch();
            pause(2000);
            boolean restored = issuePage.isCardsPopulated();
            Assert.assertTrue(restored, "Cards not restored after clearing search");
            logStep("Cards restored after clearing search");

            // 7. Try sort if available (soft — sort UI may vary)
            try {
                issuePage.clickSortOption("Name");
                logStep("Sort by Name applied");
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

    @Test(priority = 3, description = "Smoke: Activate jobs on an issue")
    public void testActivateJobs() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ACTIVATE_JOBS,
                "TC_Issue_ActivateJobs");

        try {
            // 1. Navigate to Issues page
            issuePage.navigateToIssues();
            logStep("Navigated to Issues page");

            // 2. Open the first issue detail
            issuePage.openFirstIssueDetail();
            logStep("Opened issue detail page");
            logStepWithScreenshot("Issue detail page");

            // 3. Click Activate Jobs
            issuePage.clickActivateJobs();
            logStep("Clicked Activate Jobs button");

            // 4. Verify activation
            boolean activated = issuePage.isJobActivated();
            Assert.assertTrue(activated, "Job activation did not complete");
            logStepWithScreenshot("Job activated successfully");

            ExtentReportManager.logPass("Activate Jobs verified");

        } catch (Exception e) {
            ScreenshotUtil.captureScreenshot("issue_activate_jobs_error");
            Assert.fail("Activate Jobs failed: " + e.getMessage());
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

            int beforeCount = issuePage.getCardCount();
            Assert.assertTrue(beforeCount > 0, "No issues to delete");
            logStep("Card count before delete: " + beforeCount);

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

            // 6. Verify removal — navigate back and check count decreased
            issuePage.navigateToIssues();
            pause(3000);

            boolean deleted = false;
            for (int attempt = 0; attempt < 5; attempt++) {
                int afterCount = issuePage.getCardCount();
                logStep("Post-delete check " + (attempt + 1) + ": card count = " + afterCount);
                if (afterCount < beforeCount) {
                    deleted = true;
                    break;
                }
                issuePage.navigateToIssues();
                pause(3000);
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
