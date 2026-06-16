package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.pageobjects.LoginPage;
import com.egalvanic.qa.testcase.api.RbacFixtures;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ScreenshotUtil;

import io.restassured.RestAssured;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * <b>Work-order edit — UI side (Project Manager).</b>
 *
 * <p>Pairs with {@link com.egalvanic.qa.testcase.api.WorkOrderEditEnforcementApiTest}
 * (the backend-authorization side). In the <b>real browser</b>, a Project Manager (who has
 * {@code jobs.manage} + {@code features.jobs.view}) opens a real work order and reaches its
 * <b>edit surfaces</b>.</p>
 *
 * <p>In this app a "work order" is an EMP/commitment with a tabbed detail — there is no single
 * "Edit" button; editing happens through the <i>Planned Work Orders</i> / <i>Change Orders</i>
 * tabs, which only a {@code jobs.manage} role can act on. So the UI assertion here is: PM can
 * open the work-order detail and those edit-capable tabs are present. The actual write being
 * <i>allowed</i> for PM and <i>denied</i> for a no-manage role (Client Portal →
 * {@code permission_denied}) is proven definitively by the paired API test.</p>
 */
public class WorkOrderEditUiTest {

    private WebDriver driver;
    private LoginPage loginPage;

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        ExtentReportManager.initReports();
        RestAssured.baseURI = AppConstants.API_BASE_URL; // for the live-permission oracle + job id
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() { ExtentReportManager.flushReports(); }

    @AfterMethod(alwaysRun = true)
    public void teardown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && driver != null) {
            ScreenshotUtil.captureScreenshot(result.getMethod().getMethodName() + "_FAIL");
        }
        ExtentReportManager.removeTests();
        if (driver != null) { try { driver.quit(); } catch (Exception ignored) {} driver = null; }
    }

    @Test(description = "Project Manager can open a work order and reach its edit surfaces in the live UI")
    public void projectManagerCanReachWorkOrderEdit() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS,
                "WO Edit UI: Project Manager");

        LiveAuth pm = RbacFixtures.cachedLiveAuth(RbacFixtures.ROLES.stream()
                .filter(r -> "Project Manager".equals(r.name)).findFirst().orElseThrow(IllegalStateException::new));
        if (!pm.provisioned) {
            throw new SkipException("Project Manager account not provisioned (login " + pm.loginStatus + ").");
        }
        Assert.assertTrue(pm.permissions.contains("jobs.manage"),
                "Precondition: PM should have jobs.manage in the live permission set.");

        String jobId = firstJobId(pm.token);
        if (jobId == null) {
            throw new SkipException("No work order (job) available on the QA tenant to open.");
        }

        startBrowser();
        navigateToLogin();
        loginPage.login(AppConstants.PM_EMAIL, AppConstants.PM_PASSWORD);
        waitForPostLogin();
        Assert.assertFalse(onLoginPage(), "PM login did not complete. URL: " + driver.getCurrentUrl());

        ExtentReportManager.logInfo("Opening work order (job) detail: /jobs/" + jobId);
        driver.get(AppConstants.BASE_URL + "/jobs/" + jobId);

        // Wait for the job-detail CONTENT to render (not just app chrome). Signal = the detail tabs.
        boolean detailRendered = waitForText(
                "//*[normalize-space()='Planned Work Orders' or normalize-space()='Change Orders' "
                        + "or normalize-space()='All Procedures' or normalize-space()='Overview']", 45);
        Assert.assertTrue(detailRendered,
                "PM could not open the work order detail at /jobs/" + jobId
                        + " (content never rendered). URL: " + driver.getCurrentUrl());

        // The edit surfaces for a work order (EMP) are the Planned Work Orders / Change Orders tabs,
        // which are actionable for a jobs.manage role. Their presence = PM can reach editing.
        boolean editSurfaces = present("//*[normalize-space()='Planned Work Orders']")
                && present("//*[normalize-space()='Change Orders']");
        Assert.assertTrue(editSurfaces,
                "PM opened the work order but its edit surfaces (Planned Work Orders / Change Orders) "
                        + "are not present.");

        ExtentReportManager.logPass("Project Manager opened a work order (/jobs/" + jobId
                + ") and its edit surfaces (Planned Work Orders + Change Orders) are present. "
                + "The actual edit being authorized for PM and denied for a no-manage role is proven "
                + "by WorkOrderEditEnforcementApiTest.");
    }

    // ---- helpers ----

    private boolean present(String xpath) {
        return driver.findElements(By.xpath(xpath)).stream().anyMatch(WorkOrderEditUiTest::shown);
    }

    private boolean waitForText(String xpath, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(d -> d.findElements(By.xpath(xpath)).stream().anyMatch(WorkOrderEditUiTest::shown));
            return true;
        } catch (Exception e) { return false; }
    }

    private static boolean shown(org.openqa.selenium.WebElement e) {
        try { return e.isDisplayed(); } catch (Exception x) { return false; }
    }

    /** First job id from GET /job/ using the given bearer token (robust to array or wrapped list). */
    private String firstJobId(String token) {
        try {
            io.restassured.response.Response r = RestAssured.given()
                    .header("Authorization", "Bearer " + token).when().get("/job/").then().extract().response();
            String raw = r.asString();
            if (r.getStatusCode() != 200 || raw == null) return null;
            String t = raw.trim();
            org.json.JSONArray arr;
            if (t.startsWith("[")) {
                arr = new org.json.JSONArray(t);
            } else {
                org.json.JSONObject o = new org.json.JSONObject(t);
                String key = o.has("jobs") ? "jobs" : o.has("data") ? "data" : o.has("items") ? "items" : null;
                arr = key != null ? o.getJSONArray(key) : new org.json.JSONArray();
            }
            for (int i = 0; i < arr.length(); i++) {
                String id = arr.getJSONObject(i).optString("id", "");
                if (!id.isEmpty() && !"null".equals(id)) return id;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void startBrowser() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--remote-allow-origins=*",
                "--disable-blink-features=AutomationControlled", "--no-sandbox", "--disable-dev-shm-usage");
        opts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        opts.setExperimentalOption("prefs", prefs);
        if ("true".equals(System.getProperty("headless"))) opts.addArguments("--headless=new");
        driver = new ChromeDriver(opts);
        driver.manage().window().maximize();
        ScreenshotUtil.setDriver(driver);
        loginPage = new LoginPage(driver);
    }

    private void navigateToLogin() {
        driver.get(AppConstants.BASE_URL);
        sleep(2000);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        } catch (Exception ignored) {}
    }

    private void waitForPostLogin() {
        try { new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> !onLoginPage()); }
        catch (Exception ignored) {}
        sleep(2000);
    }

    private boolean onLoginPage() {
        try {
            List<org.openqa.selenium.WebElement> email = driver.findElements(By.id("email"));
            return !email.isEmpty() && email.get(0).isDisplayed();
        } catch (Exception e) { return false; }
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
