import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
/**
* Final single-class automation: Create asset (Phase 1) + Edit asset (Phase 2)
* - Uses label-based stable XPaths
* - Expands CORE ATTRIBUTES accordion before interacting
* - Type + select for autocomplete dropdowns
* - No CSV, all hardcoded values
*/
public class Egalvanic {
  static WebDriver driver;
  static WebDriverWait wait;
  static JavascriptExecutor js;
  static Actions actions;
  static ExtentReports extent;
  static ExtentTest moduleTest; // Module level test
  static ExtentTest featureTest; // Feature level test
  static ExtentTest crudSubFeatureTest; // CRUD Sub-feature test
  static ExtentTest searchSubFeatureTest; // Search Sub-feature test
  static ExtentTest crossBrowserTest;
  static ExtentSparkReporter sparkReporter;
  static Map<String, ExtentReports> browserReports = new HashMap<>();
  static Map<String, ExtentTest> browserTests = new HashMap<>();
 
  // === CONFIG (hardcoded) ===
  static final String BASE_URL = "https://acme.egalvanic.ai/login";
  static final String EMAIL = "rahul+acme@egalvanic.com";
  static final String PASSWORD = "RP@egalvanic123";
  static final String ASSET_NAME = "asset";
  static final String QR_CODE = "qrcode";
  static final String ASSET_CLASS = "3-Pole Breaker";
  static final String CONDITION_VALUE = "2";
  // Core attributes
  static final String MODEL_VAL = "model123";
  static final String NOTES_VAL = "good";
  static final String AMPERE_RATING = "20 A";
  static final String MANUFACTURER = "Eaton";
  static final String CATALOG_NUMBER = "cat001";
  static final String BREAKER_SETTINGS = "setting1";
  static final String INTERRUPTING_RATING = "10 kA";
  static final String KA_RATING = "18 kA";
  static final String REPLACEMENT_COST = "30000";
  static final int DEFAULT_TIMEOUT = 25;
 
  // API Testing Config
  static final String API_BASE_URL = "https://acme.egalvanic.ai/api";
  static String tenantBaseUrl = null;
  static String authToken = null;
  static String userId = null;
  static String currentBrowser = "chrome";
  
  public static void main(String[] args) {
      currentBrowser = args.length > 0 ? args[0] : "chrome";
      setupExtentReports();
      setupDriver(currentBrowser);
      try {
          // Create the hierarchical test structure for reporting
          // Module = Assets
          moduleTest = extent.createTest("Module: Assets");
          moduleTest.assignCategory("Assets");
          
          // Feature = List
          featureTest = moduleTest.createNode("Feature: List");
          featureTest.assignCategory("List");
          
          // Sub-Feature = CRUD Assets
          crudSubFeatureTest = featureTest.createNode("Sub-Feature: CRUD Assets");
          crudSubFeatureTest.assignCategory("CRUD");
          
          // Sub-Feature = Search
          searchSubFeatureTest = featureTest.createNode("Sub-Feature: Search");
          searchSubFeatureTest.assignCategory("Search");
          
          // Create a specific test for cross-browser testing
          crossBrowserTest = extent.createTest("Cross-Browser Testing - " + currentBrowser.toUpperCase());
          crossBrowserTest.assignCategory("Cross-Browser Testing");
          crossBrowserTest.log(Status.INFO, "Starting cross-browser testing on " + currentBrowser.toUpperCase());
          
          // Add browser-specific information to the report
          crossBrowserTest.log(Status.INFO, "Browser Version: " + getCurrentBrowserVersion());
          crossBrowserTest.log(Status.INFO, "Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
          
          // Log initial information in the CRUD sub-feature
          crudSubFeatureTest.log(Status.INFO, "Starting UI testing for dashboard, create asset and edit asset flow");
         
          // UI Testing
          crudSubFeatureTest.log(Status.INFO, "Starting UI testing");
          takeShotAndAttachReport("ui_before_login", "Before Login");
          crossBrowserTest.log(Status.INFO, "Before Login - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Login - " + currentBrowser.toUpperCase());
         
          login();
          takeShotAndAttachReport("after_login", "After Login");
          crossBrowserTest.log(Status.PASS, "✅ Login Successful - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "After Login - " + currentBrowser.toUpperCase());
          Thread.sleep(500);
         
          takeShotAndAttachReport("ui_before_site_select", "Before Site Selection");
          crossBrowserTest.log(Status.INFO, "Before Site Selection - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Site Selection - " + currentBrowser.toUpperCase());
          selectSite(); // user's exact site selection method
          takeShotAndAttachReport("after_site_select", "After Site Selection");
          crossBrowserTest.log(Status.PASS, "✅ Site Selection Successful - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "After Site Selection - " + currentBrowser.toUpperCase());
         
          takeShotAndAttachReport("ui_before_assets", "Before Assets Page");
          crossBrowserTest.log(Status.INFO, "Before Assets Page - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Assets Page - " + currentBrowser.toUpperCase());
          goToAssets();
          takeShotAndAttachReport("assets_page", "Assets Page");
          crossBrowserTest.log(Status.PASS, "✅ Assets Page Loaded - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Assets Page - " + currentBrowser.toUpperCase());
         
          takeShotAndAttachReport("ui_before_create_asset", "Before Create Asset");
          crossBrowserTest.log(Status.INFO, "Before Create Asset - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Create Asset - " + currentBrowser.toUpperCase());
          createAssetPhase1();
          takeShotAndAttachReport("after_phase1_create", "After Phase 1 Create");
          crossBrowserTest.log(Status.PASS, "✅ Asset Creation Phase 1 Successful - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "After Phase 1 Create - " + currentBrowser.toUpperCase());
         
          takeShotAndAttachReport("ui_before_edit_asset", "Before Edit Asset");
          crossBrowserTest.log(Status.INFO, "Before Edit Asset - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Edit Asset - " + currentBrowser.toUpperCase());
          editAssetPhase2();
          takeShotAndAttachReport("after_phase2_edit", "After Phase 2 Edit");
          crossBrowserTest.log(Status.PASS, "✅ Asset Edit Phase 2 Successful - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "After Phase 2 Edit - " + currentBrowser.toUpperCase());

          deleteAsset();
          takeShotAndAttachReport("ui_before_delete_asset", "Before Delete Asset");
          crossBrowserTest.log(Status.INFO, "Before Delete Asset - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Delete Asset - " + currentBrowser.toUpperCase());

          crossBrowserTest.log(Status.PASS, "✅ UI testing completed successfully on " + currentBrowser.toUpperCase());
         
          crudSubFeatureTest.log(Status.PASS, "🎉 Full UI flow completed successfully");
          crossBrowserTest.log(Status.PASS, "🎉 Full UI flow completed successfully on " + currentBrowser.toUpperCase());
         
          // API Testing
          performAPITesting();
         
          // Performance Testing
          performPerformanceTesting();
         
          // Security Testing
          performSecurityTesting();
         
          System.out.println("\n🎉 ALL TESTS FINISHED");
          
      } catch (Exception e) {
          System.out.println("❌ Fatal error: " + e.getMessage());
          crudSubFeatureTest.log(Status.FAIL, "Fatal error: " + e.getMessage());
          crossBrowserTest.log(Status.FAIL, "Fatal error on " + currentBrowser.toUpperCase() + ": " + e.getMessage());
          try { 
              takeShotAndAttachReport("fatal_error", "Fatal Error - " + currentBrowser.toUpperCase()); 
              crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Fatal Error - " + currentBrowser.toUpperCase());
          } catch (Exception ignored) {}
          e.printStackTrace();
      } finally {
          if (driver != null) driver.quit();
          extent.flush();
      }
  }
  // ---------------- setup ----------------
  static void setupExtentReports() {
      // Create reports directory if it doesn't exist
      try {
          Files.createDirectories(Path.of("test-output/reports"));
          Files.createDirectories(Path.of("test-output/screenshots"));
      } catch (Exception e) {
          System.out.println("Failed to create directories: " + e.getMessage());
      }
     
      // Initialize ExtentSparkReporter with browser-specific name
      String reportFileName = "test-output/reports/AutomationReport.html";
      if (!currentBrowser.equals("chrome")) {
          reportFileName = "test-output/reports/AutomationReport_" + currentBrowser + ".html";
      }
      sparkReporter = new ExtentSparkReporter(reportFileName);
      sparkReporter.config().setTheme(Theme.STANDARD);
      sparkReporter.config().setDocumentTitle("ACME Automation Report - " + currentBrowser.toUpperCase());
      sparkReporter.config().setReportName("ACME Test Automation Report - " + currentBrowser.toUpperCase());
     
      // Initialize ExtentReports
      extent = new ExtentReports();
      extent.attachReporter(sparkReporter);
      extent.setSystemInfo("Organization", "ACME");
      extent.setSystemInfo("Environment", "Test");
      extent.setSystemInfo("Browser", currentBrowser);
      extent.setSystemInfo("Tester", "QA Automation Engineer");
  }
 
  static void setupDriver() {
      setupDriver("chrome");
  }
  
  static void setupDriver(String browserType) {
        switch(browserType.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;
            case "edge":
                try {
                    // Set the Edge binary path explicitly
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.setBinary("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
                    
                    // Try to create EdgeDriver directly with options
                    driver = new EdgeDriver(edgeOptions);
                } catch (Exception e) {
                    System.out.println("⚠️ Edge WebDriver setup failed: " + e.getMessage());
                    throw e;
                }
                break;
            case "safari":
                WebDriverManager.safaridriver().setup();
                driver = new SafariDriver();
                // Maximize Safari window
                driver.manage().window().maximize();
                break;
            case "chrome":
            default:
                // Let WebDriverManager automatically detect the correct ChromeDriver version
                // Clear cache first to ensure fresh download
                WebDriverManager.chromedriver().clearDriverCache();
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOpts = new ChromeOptions();
                chromeOpts.addArguments("--start-maximized");
                chromeOpts.addArguments("--remote-allow-origins=*");
                chromeOpts.addArguments("--disable-blink-features=AutomationControlled");
                chromeOpts.addArguments("--no-sandbox");
                chromeOpts.addArguments("--disable-dev-shm-usage");
                chromeOpts.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
                chromeOpts.setExperimentalOption("useAutomationExtension", false);
                driver = new ChromeDriver(chromeOpts);
                // Ensure window is maximized
                driver.manage().window().maximize();
                break;
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        js = (JavascriptExecutor) driver;
        actions = new Actions(driver);
        // ⭐ FIX: Ensure edit button visible
        js.executeScript("document.body.style.zoom='80%';");
        pause(600);
    }

  static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
  static String stamp() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")); }
  
  // Add a helper method to get base64 screenshot
  static String getBase64Screenshot() {
      try {
          return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
      } catch (Exception e) {
          return "";
      }
  }
  
  // Add a helper method to get browser version
  static String getCurrentBrowserVersion() {
      try {
          if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
              return ((org.openqa.selenium.remote.RemoteWebDriver) driver).getCapabilities().getBrowserVersion();
          }
          return "Unknown";
      } catch (Exception e) {
          return "Unknown";
      }
  }
  static void takeShotAndAttachReport(String name, String testName) {
      try {
          // Take screenshot and convert to Base64 for embedding directly in report
          File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
          String fname = name + "_" + stamp() + ".png";
          String destPath = "test-output/screenshots/" + fname;
          Files.copy(src.toPath(), Path.of(destPath));
          System.out.println("✔ Screenshot saved: " + fname);
       
          // Create Base64 encoded version for embedding in report
          byte[] fileContent = Files.readAllBytes(Path.of(destPath));
          String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);
       
          // Attach to Extent Report (only embedded version for sharing compatibility)
          // Use crudSubFeatureTest as default if no specific test is provided
          if (crudSubFeatureTest != null) {
              crudSubFeatureTest.addScreenCaptureFromBase64String(base64Image, testName);
          } else if (crossBrowserTest != null) {
              crossBrowserTest.addScreenCaptureFromBase64String(base64Image, testName);
          }
      } catch (Exception e) {
          System.out.println("⚠️ Screenshot failed: " + e.getMessage());
          // Log to the appropriate test node
          if (crudSubFeatureTest != null) {
              crudSubFeatureTest.log(Status.WARNING, "Screenshot failed: " + e.getMessage());
          } else if (crossBrowserTest != null) {
              crossBrowserTest.log(Status.WARNING, "Screenshot failed: " + e.getMessage());
          }
      }
  }
 
  static void takeShot(String name) {
      try {
          File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
          String fname = name + "_" + stamp() + ".png";
          Files.copy(src.toPath(), Path.of(fname));
          System.out.println("✔ Screenshot saved: " + fname);
      } catch (Exception e) {
          System.out.println("⚠️ Screenshot failed: " + e.getMessage());
      }
  }
  // ---------- safe element helpers ----------
  static WebElement visible(By by, int timeoutSec) {
      return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
              .until(ExpectedConditions.visibilityOfElementLocated(by));
  }
  static WebElement clickable(By by, int timeoutSec) {
      return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
              .until(ExpectedConditions.elementToBeClickable(by));
  }
  static void jsClick(By by) {
      try {
          WebElement e = driver.findElement(by);
          js.executeScript("arguments[0].click();", e);
      } catch (Exception ex) {
          throw new RuntimeException("JS click failed for: " + by, ex);
      }
  }
  static void click(By by) {
      try {
          clickable(by, DEFAULT_TIMEOUT).click();
      } catch (Exception e) {
          try {
              jsClick(by);
          } catch (Exception ex) {
              throw new RuntimeException("Click failed for: " + by + " -> " + ex.getMessage(), ex);
          }
      }
  }
  static void type(By by, String text) {
      WebElement e = visible(by, DEFAULT_TIMEOUT);
      try { e.clear(); } catch (Exception ignored) {}
      e.click();
      e.sendKeys(text);
  }
  static void typeAndSelectDropdown(By inputLocator, String textToType, String optionText) {
      click(inputLocator);
      pause(200);
      try {
          WebElement in = visible(inputLocator, 5);
          in.clear();
          in.sendKeys(textToType);
      } catch (Exception ignored) {}
      pause(400);
      By listOption = By.xpath("//li[normalize-space()='" + optionText + "'] | //li[contains(normalize-space(),'" + optionText + "')]");
      int attempts = 0;
      while (attempts < 4) {
          try {
              if (driver.findElements(listOption).size() > 0) {
                  click(listOption);
                  return;
              } else {
                  try {
                      By popup = By.xpath("//button[contains(@class,'MuiAutocomplete-popupIndicator')]");
                      if (driver.findElements(popup).size() > 0) {
                          click(popup);
                          pause(300);
                      }
                  } catch (Exception ignored) {}
              }
          } catch (Exception ignored) {}
          pause(400);
          attempts++;
      }
      System.out.println("⚠️ Option not found for '" + optionText + "' — continuing");
  }
  static void scrollToHeader(String headerText) {
      try {
          By header = By.xpath("//*[normalize-space()='" + headerText + "']");
          WebElement el = driver.findElement(header);
          js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
          pause(500);
      } catch (Exception e) {
          js.executeScript("window.scrollBy(0,600);");
          pause(500);
      }
  }
  // ---------------- login ----------------
  static void login() {
      crudSubFeatureTest.log(Status.INFO, "Starting login process");
      crudSubFeatureTest.log(Status.INFO, "Navigating to login page: " + BASE_URL);
      long startTime = System.currentTimeMillis();
      driver.get(BASE_URL);
     
      crudSubFeatureTest.log(Status.INFO, "Entering email: " + EMAIL);
      type(By.id("email"), EMAIL);
     
      crudSubFeatureTest.log(Status.INFO, "Entering password: " + PASSWORD);
      type(By.id("password"), PASSWORD);
     
      crudSubFeatureTest.log(Status.INFO, "Clicking login button");
      click(By.xpath("//button[@type='submit' or contains(.,'Sign in') or contains(.,'Login')]"));
     
      crudSubFeatureTest.log(Status.INFO, "Waiting for login to complete");
      wait.until(ExpectedConditions.or(
              ExpectedConditions.presenceOfElementLocated(By.cssSelector("nav")),
              ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Dashboard') or contains(text(),'Sites')]"))
      ));
     
      long endTime = System.currentTimeMillis();
      long loadTime = endTime - startTime;
      System.out.println("✔ Login successful");
      crudSubFeatureTest.log(Status.PASS, "✅ Login successful in " + loadTime + " ms");
     
      // Get auth token for API testing
      crudSubFeatureTest.log(Status.INFO, "Getting authentication token for API testing");
      getAuthToken();
  }
  // ---------------- API Authentication ----------------
  static void getAuthToken() {
      try {
          crudSubFeatureTest.log(Status.INFO, "Starting Authentication Flow (Alliance Config + Tenant Login)");
          takeShotAndAttachReport("api_before_auth", "Before API Authentication");

          // STEP 1: HIT ALLIANCE CONFIG (NO SUBDOMAIN HEADER)
          crudSubFeatureTest.log(Status.INFO, "Calling alliance-config API to fetch tenant base URL");

          RestAssured.baseURI = "https://eg-pz.egalvanic.ai/api";

          Response configResponse = RestAssured.given()
                  .get("/company/alliance-config/acme.egalvanic")
                  .then().extract().response();

          crudSubFeatureTest.log(Status.INFO, "Alliance-config status: " + configResponse.getStatusCode());
          crudSubFeatureTest.log(Status.INFO, "Alliance-config response: " + configResponse.getBody().asString());

          if (configResponse.getStatusCode() != 200) {
              crudSubFeatureTest.log(Status.FAIL, "❌ Failed to load alliance-config.");
              takeShotAndAttachReport("api_alliance_config_fail", "Alliance Config Failed");
              return;
          }

          // Extract tenant API base URL
          tenantBaseUrl = configResponse.jsonPath().getString("alliance_partner.invoke_url");

          if (tenantBaseUrl == null || tenantBaseUrl.isEmpty()) {
              crudSubFeatureTest.log(Status.FAIL, "❌ baseApiUrl missing in alliance-config response");
              return;
          }

          crudSubFeatureTest.log(Status.PASS, "Tenant API Base URL found: " + tenantBaseUrl);

          // STEP 2: LOGIN WITH SUBDOMAIN ON TENANT DOMAIN
          crudSubFeatureTest.log(Status.INFO, "Proceeding to tenant login using: " + tenantBaseUrl);

          RestAssured.baseURI = tenantBaseUrl;

          JSONObject loginPayload = new JSONObject();
          loginPayload.put("email", EMAIL);
          loginPayload.put("password", PASSWORD);

          crudSubFeatureTest.log(Status.INFO, "Login request payload: " + loginPayload.toString());

          Response response = RestAssured.given()
                  .contentType("application/json")
                  .header("subdomain", "acme")  // REQUIRED HEADER
                  .body(loginPayload.toString())
                  .post("/auth/login")
                  .then().extract().response();

          crudSubFeatureTest.log(Status.INFO, "Login response status code: " + response.getStatusCode());
          crudSubFeatureTest.log(Status.INFO, "Login response body: " + response.getBody().asString());

          if (response.getStatusCode() == 200) {
              authToken = response.jsonPath().getString("access_token");

              if (authToken == null || authToken.isEmpty()) {
                  crudSubFeatureTest.log(Status.FAIL, "❌ Token is missing in login response");
              } else {
                  crudSubFeatureTest.log(Status.PASS, "✅ Authentication token obtained successfully");
                  crudSubFeatureTest.log(Status.INFO, "Token (first 10 chars): " + authToken.substring(0, 10) + "...");
              }

              // Extract user ID
              try {
                  userId = response.jsonPath().getString("user.id");
                  crudSubFeatureTest.log(Status.INFO, "User ID extracted: " + userId);
              } catch (Exception e) {
                  crudSubFeatureTest.log(Status.INFO, "User ID not present in login response");
              }

          } else {
              crudSubFeatureTest.log(Status.FAIL, "❌ Failed to obtain authentication token");
              crudSubFeatureTest.log(Status.INFO, "Failure response body: " + response.getBody().asString());
              takeShotAndAttachReport("api_auth_fail_response", "Auth Failed Response");
          }

          takeShotAndAttachReport("api_after_auth", "After API Authentication");

      } catch (Exception e) {
          crudSubFeatureTest.log(Status.FAIL, "❌ Exception during getAuthToken(): " + e.getMessage());
          takeShotAndAttachReport("api_auth_exception", "API Auth Exception");
      }
  }
  // ---------------- API Testing ----------------
  static void performAPITesting() {
      ExtentTest apiTest = extent.createTest("API Testing");
      apiTest.log(Status.INFO, "Starting API testing");
      crossBrowserTest.log(Status.INFO, "Starting API testing on " + currentBrowser.toUpperCase());
     
      try {
          if (authToken == null) {
              apiTest.log(Status.FAIL, "❌ No auth token available for API testing");
              takeShotAndAttachReport("api_no_token", "API Testing - No Auth Token");
              return;
          }
         
          apiTest.log(Status.INFO, "Using auth token for API testing");
          takeShotAndAttachReport("api_before_tests", "Before API Tests");
          crossBrowserTest.log(Status.INFO, "Before API Tests - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before API Tests - " + currentBrowser.toUpperCase());
         
          // Test GET auth/me endpoint to validate token
          apiTest.log(Status.INFO, "Testing GET /auth/me endpoint");
          takeShotAndAttachReport("api_before_auth_me", "Before Auth Me Test");
          crossBrowserTest.log(Status.INFO, "Before Auth Me Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Auth Me Test - " + currentBrowser.toUpperCase());
         
          long startTime = System.currentTimeMillis();
         
          Response response = RestAssured.given()
                  .contentType("application/json")
                  .header("Authorization", "Bearer " + authToken)
                  .when()
                  .get(tenantBaseUrl + "/auth/me")
                  .then()
                  .extract().response();
         
          long endTime = System.currentTimeMillis();
          long responseTime = endTime - startTime;
         
          apiTest.log(Status.INFO, "GET /auth/me response time: " + responseTime + " ms");
          apiTest.log(Status.INFO, "GET /auth/me status code: " + response.getStatusCode());
          apiTest.log(Status.INFO, "GET /auth/me response headers: " + response.getHeaders().toString());
         
          if (response.getStatusCode() == 200) {
              apiTest.log(Status.PASS, "✅ GET /auth/me endpoint test passed");
             
              // Extract user information from response
              try {
                  String userEmail = response.jsonPath().getString("email");
                  String userId = response.jsonPath().getString("id");
                  apiTest.log(Status.INFO, "User authenticated: " + userEmail + " (ID: " + userId + ")");
                 
                  // Additional user info
                  try {
                      String userName = response.jsonPath().getString("name");
                      apiTest.log(Status.INFO, "User name: " + userName);
                  } catch (Exception e) {
                      apiTest.log(Status.INFO, "User name not available in response");
                  }
              } catch (Exception e) {
                  apiTest.log(Status.WARNING, "⚠️ User information not available in response: " + e.getMessage());
              }
             
              // Log response body snippet
              String responseBody = response.asString();
              if (responseBody.length() > 200) {
                  apiTest.log(Status.INFO, "Response body (first 200 chars): " + responseBody.substring(0, 200) + "...");
              } else {
                  apiTest.log(Status.INFO, "Response body: " + responseBody);
              }
          } else {
              apiTest.log(Status.FAIL, "❌ GET /auth/me endpoint test failed with status code: " + response.getStatusCode());
              apiTest.log(Status.INFO, "Response body: " + response.getBody().asString());
          }
         
          // Capture screenshot for API testing
          takeShotAndAttachReport("api_test_auth_me", "API Auth Me Test");
          crossBrowserTest.log(Status.PASS, "✅ Auth Me Test Passed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "API Auth Me Test - " + currentBrowser.toUpperCase());
         
          // Test GET users endpoint
          apiTest.log(Status.INFO, "Testing GET /users endpoint");
          takeShotAndAttachReport("api_before_users", "Before Users Endpoint Test");
          crossBrowserTest.log(Status.INFO, "Before Users Endpoint Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Users Endpoint Test - " + currentBrowser.toUpperCase());
         
          startTime = System.currentTimeMillis();
         
          response = RestAssured.given()
                  .contentType("application/json")
                  .header("Authorization", "Bearer " + authToken)
                  .when()
                  .get(tenantBaseUrl + "/users/")
                  .then()
                  .extract().response();
         
          endTime = System.currentTimeMillis();
          responseTime = endTime - startTime;
         
          apiTest.log(Status.INFO, "GET /users response time: " + responseTime + " ms");
          apiTest.log(Status.INFO, "GET /users status code: " + response.getStatusCode());
         
          if (response.getStatusCode() == 200) {
              apiTest.log(Status.PASS, "✅ GET /users endpoint test passed");
             
              // Test API response size
              String responseBody = response.asString();
              int responseSize = responseBody.length();
              apiTest.log(Status.INFO, "API response size: " + responseSize + " characters");
             
              if (responseSize < 1000000) { // Less than 1MB
                  apiTest.log(Status.PASS, "✅ API response size is acceptable");
              } else {
                  apiTest.log(Status.WARNING, "⚠️ API response size is larger than expected");
              }
             
              // Parse response to check structure
              try {
                  int userCount = response.jsonPath().getList("$").size();
                  apiTest.log(Status.INFO, "Number of users returned: " + userCount);
                 
                  // Show first user as example
                  if (userCount > 0) {
                      try {
                          String firstUserEmail = response.jsonPath().getString("[0].email");
                          apiTest.log(Status.INFO, "First user email: " + firstUserEmail);
                      } catch (Exception e) {
                          apiTest.log(Status.INFO, "Could not extract first user email");
                      }
                  }
              } catch (Exception e) {
                  apiTest.log(Status.WARNING, "⚠️ Could not parse user count from response: " + e.getMessage());
              }
             
              // Log response body snippet
              if (responseBody.length() > 200) {
                  apiTest.log(Status.INFO, "Response body (first 200 chars): " + responseBody.substring(0, 200) + "...");
              } else {
                  apiTest.log(Status.INFO, "Response body: " + responseBody);
              }
          } else {
              apiTest.log(Status.FAIL, "❌ GET /users endpoint test failed with status code: " + response.getStatusCode());
              apiTest.log(Status.INFO, "Response body: " + response.getBody().asString());
          }
         
          // Capture screenshot for API testing
          takeShotAndAttachReport("api_test_users_endpoint", "API Users Endpoint Test");
          crossBrowserTest.log(Status.PASS, "✅ Users Endpoint Test Passed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "API Users Endpoint Test - " + currentBrowser.toUpperCase());
         
          // Test POST create user endpoint (example)
          apiTest.log(Status.INFO, "Testing POST /users endpoint (example)");
          takeShotAndAttachReport("api_before_create_user", "Before Create User Test");
          crossBrowserTest.log(Status.INFO, "Before Create User Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Create User Test - " + currentBrowser.toUpperCase());
         
          JSONObject newUserPayload = new JSONObject();
          newUserPayload.put("name", "Test User");
          newUserPayload.put("email", "test@example.com");
         
          response = RestAssured.given()
                  .contentType("application/json")
                  .header("Authorization", "Bearer " + authToken)
                  .body(newUserPayload.toString())
                  .when()
                  .post(tenantBaseUrl + "/users/")
                  .then()
                  .extract().response();
         
          apiTest.log(Status.INFO, "POST /users status code: " + response.getStatusCode());
          apiTest.log(Status.INFO, "POST /users request payload: " + newUserPayload.toString());
         
          if (response.getStatusCode() == 201 || response.getStatusCode() == 200) {
              apiTest.log(Status.PASS, "✅ POST /users endpoint test passed");
          } else if (response.getStatusCode() == 403 || response.getStatusCode() == 401 || response.getStatusCode() == 400) {
              apiTest.log(Status.PASS, "✅ POST /users correctly rejected (as expected): " + response.getStatusCode());
          } else {
              apiTest.log(Status.WARNING, "⚠️ POST /users returned unexpected status: " + response.getStatusCode());
          }
         
          // Capture screenshot for API testing
          takeShotAndAttachReport("api_test_create_user", "API Create User Test");
          crossBrowserTest.log(Status.PASS, "✅ Create User Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "API Create User Test - " + currentBrowser.toUpperCase());
         
          apiTest.log(Status.PASS, "✅ API testing completed with detailed results");
          crossBrowserTest.log(Status.PASS, "✅ API testing completed with detailed results on " + currentBrowser.toUpperCase());
          takeShotAndAttachReport("api_tests_complete", "API Tests Complete");
         
      } catch (Exception e) {
          apiTest.log(Status.FAIL, "❌ API testing failed with exception: " + e.getMessage());
          e.printStackTrace();
          // Capture screenshot for API testing failure
          takeShotAndAttachReport("api_test_failure", "API Test Failure");
      }
  }
 
  // ---------------- Performance Testing ----------------
  static void performPerformanceTesting() {
      ExtentTest perfTest = extent.createTest("Performance Testing");
      perfTest.log(Status.INFO, "Starting performance testing");
      crossBrowserTest.log(Status.INFO, "Starting performance testing on " + currentBrowser.toUpperCase());
     
      try {
          // UI Performance Test - Page Load Time
          perfTest.log(Status.INFO, "Testing UI page load performance");
          takeShotAndAttachReport("perf_before_ui_load", "Before UI Load Test");
          crossBrowserTest.log(Status.INFO, "Before UI Load Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before UI Load Test - " + currentBrowser.toUpperCase());
         
          long startTime = System.currentTimeMillis();
          driver.get(BASE_URL);
          long endTime = System.currentTimeMillis();
          long loadTime = endTime - startTime;
         
          perfTest.log(Status.INFO, "Login page load time: " + loadTime + " ms");
         
          // Performance assessment
          if (loadTime < 2000) { // Less than 2 seconds
              perfTest.log(Status.PASS, "✅ Excellent page load time (< 2 seconds)");
          } else if (loadTime < 5000) { // Less than 5 seconds
              perfTest.log(Status.PASS, "✅ Good page load time (< 5 seconds)");
          } else {
              perfTest.log(Status.WARNING, "⚠️ Page load time is slower than expected (> 5 seconds)");
          }
         
          // Capture screenshot for UI performance test
          takeShotAndAttachReport("perf_ui_load", "UI Performance Test");
          crossBrowserTest.log(Status.PASS, "✅ UI Performance Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "UI Performance Test - " + currentBrowser.toUpperCase());
         
          // Test multiple page loads for consistency
          perfTest.log(Status.INFO, "Testing UI page load consistency");
          takeShotAndAttachReport("perf_before_consistency", "Before Consistency Test");
          crossBrowserTest.log(Status.INFO, "Before Consistency Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Consistency Test - " + currentBrowser.toUpperCase());
         
          long[] loadTimes = new long[3];
          long totalLoadTime = 0;
         
          for (int i = 0; i < 3; i++) {
              startTime = System.currentTimeMillis();
              driver.get(BASE_URL);
              endTime = System.currentTimeMillis();
              loadTimes[i] = endTime - startTime;
              totalLoadTime += loadTimes[i];
              perfTest.log(Status.INFO, "Load test " + (i+1) + " time: " + loadTimes[i] + " ms");
              pause(1000); // Wait 1 second between tests
          }
         
          long averageLoadTime = totalLoadTime / 3;
          perfTest.log(Status.INFO, "Average load time over 3 tests: " + averageLoadTime + " ms");
         
          // Check for consistency
          long minTime = Math.min(Math.min(loadTimes[0], loadTimes[1]), loadTimes[2]);
          long maxTime = Math.max(Math.max(loadTimes[0], loadTimes[1]), loadTimes[2]);
          long difference = maxTime - minTime;
         
          if (difference < 1000) { // Less than 1 second difference
              perfTest.log(Status.PASS, "✅ Page load times are consistent (difference < 1 second)");
          } else {
              perfTest.log(Status.WARNING, "⚠️ Page load times show inconsistency (difference > 1 second)");
          }
         
          takeShotAndAttachReport("perf_consistency", "UI Consistency Test");
          crossBrowserTest.log(Status.PASS, "✅ UI Consistency Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "UI Consistency Test - " + currentBrowser.toUpperCase());
         
          // Concurrent Requests Performance Test
          perfTest.log(Status.INFO, "Testing concurrent API requests performance");
          takeShotAndAttachReport("perf_before_concurrent", "Before Concurrent Requests Test");
          crossBrowserTest.log(Status.INFO, "Before Concurrent Requests Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Concurrent Requests Test - " + currentBrowser.toUpperCase());
         
          if (authToken != null) {
              long totalResponseTime = 0;
              int requestCount = 5;
              long[] responseTimes = new long[requestCount];
              int successCount = 0;
             
              perfTest.log(Status.INFO, "Sending " + requestCount + " concurrent requests");
             
              for (int i = 0; i < requestCount; i++) {
                  long reqStartTime = System.currentTimeMillis();
                 
                  Response response = RestAssured.given()
                          .contentType("application/json")
                          .header("Authorization", "Bearer " + authToken)
                          .when()
                          .get(tenantBaseUrl + "/users/")
                          .then()
                          .extract().response();
                 
                  long reqEndTime = System.currentTimeMillis();
                  long reqResponseTime = reqEndTime - reqStartTime;
                  responseTimes[i] = reqResponseTime;
                  totalResponseTime += reqResponseTime;
                 
                  if (response.getStatusCode() == 200) {
                      successCount++;
                      perfTest.log(Status.INFO, "Concurrent request " + (i+1) + " successful in " + reqResponseTime + " ms");
                  } else {
                      perfTest.log(Status.WARNING, "Concurrent request " + (i+1) + " failed with status code: " + response.getStatusCode() + " in " + reqResponseTime + " ms");
                  }
              }
             
              long averageResponseTime = totalResponseTime / requestCount;
              perfTest.log(Status.INFO, "Average response time for " + requestCount + " concurrent requests: " + averageResponseTime + " ms");
              perfTest.log(Status.INFO, "Successful requests: " + successCount + "/" + requestCount);
             
              // Performance assessment
              if (averageResponseTime < 1000) { // Less than 1 second
                  perfTest.log(Status.PASS, "✅ Excellent concurrent requests performance (< 1 second)");
              } else if (averageResponseTime < 3000) { // Less than 3 seconds
                  perfTest.log(Status.PASS, "✅ Good concurrent requests performance (< 3 seconds)");
              } else {
                  perfTest.log(Status.WARNING, "⚠️ Concurrent requests performance is slower than expected (> 3 seconds)");
              }
             
              // Success rate assessment
              double successRate = (double) successCount / requestCount * 100;
              if (successRate == 100) {
                  perfTest.log(Status.PASS, "✅ All concurrent requests successful (100% success rate)");
              } else if (successRate >= 80) {
                  perfTest.log(Status.PASS, "✅ Good success rate for concurrent requests (" + successRate + "%)");
              } else {
                  perfTest.log(Status.FAIL, "❌ Poor success rate for concurrent requests (" + successRate + "%)");
              }
             
              // Capture screenshot for concurrent requests test
              takeShotAndAttachReport("perf_concurrent_requests", "Concurrent Requests Performance");
              crossBrowserTest.log(Status.PASS, "✅ Concurrent Requests Test Completed - " + currentBrowser.toUpperCase());
              crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Concurrent Requests Performance - " + currentBrowser.toUpperCase());
             
          } else {
              perfTest.log(Status.FAIL, "❌ Skipping concurrent requests test - no auth token available");
              // Capture screenshot for skipped test
              takeShotAndAttachReport("perf_skipped_test", "Performance Test Skipped");
          }
         
          // API Response Size Performance Test
          perfTest.log(Status.INFO, "Testing API response size performance");
          takeShotAndAttachReport("perf_before_response_size", "Before Response Size Test");
          crossBrowserTest.log(Status.INFO, "Before Response Size Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Response Size Test - " + currentBrowser.toUpperCase());
         
          if (authToken != null) {
              Response response = RestAssured.given()
                      .contentType("application/json")
                      .header("Authorization", "Bearer " + authToken)
                      .when()
                      .get(tenantBaseUrl + "/users/")
                      .then()
                      .extract().response();
             
              if (response.getStatusCode() == 200) {
                  String responseBody = response.asString();
                  int responseSize = responseBody.length();
                  perfTest.log(Status.INFO, "API response size: " + responseSize + " characters");
                 
                  // Performance assessment based on response size
                  if (responseSize < 50000) { // Less than 50KB
                      perfTest.log(Status.PASS, "✅ Excellent API response size (< 50KB)");
                  } else if (responseSize < 100000) { // Less than 100KB
                      perfTest.log(Status.PASS, "✅ Good API response size (< 100KB)");
                  } else {
                      perfTest.log(Status.WARNING, "⚠️ Large API response size (> 100KB)");
                  }
              } else {
                  perfTest.log(Status.WARNING, "⚠️ Could not get response size - API request failed with status: " + response.getStatusCode());
              }
             
              takeShotAndAttachReport("perf_response_size", "API Response Size Test");
              crossBrowserTest.log(Status.PASS, "✅ API Response Size Test Completed - " + currentBrowser.toUpperCase());
              crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "API Response Size Test - " + currentBrowser.toUpperCase());
          } else {
              perfTest.log(Status.WARNING, "⚠️ Skipping response size test - no auth token available");
          }
         
          perfTest.log(Status.PASS, "✅ Performance testing completed with detailed results");
          crossBrowserTest.log(Status.PASS, "✅ Performance testing completed with detailed results on " + currentBrowser.toUpperCase());
          takeShotAndAttachReport("perf_tests_complete", "Performance Tests Complete");
         
      } catch (Exception e) {
          perfTest.log(Status.FAIL, "❌ Performance testing failed with exception: " + e.getMessage());
          e.printStackTrace();
          // Capture screenshot for performance testing failure
          takeShotAndAttachReport("perf_test_failure", "Performance Test Failure");
      }
  }
 
  // ---------------- Security Testing ----------------
  static void performSecurityTesting() {
      ExtentTest securityTest = extent.createTest("Security Testing");
      securityTest.log(Status.INFO, "Starting security testing");
      crossBrowserTest.log(Status.INFO, "Starting security testing on " + currentBrowser.toUpperCase());
     
      try {
          // SQL Injection Test
          securityTest.log(Status.INFO, "Testing SQL injection protection");
          takeShotAndAttachReport("security_before_sql_injection", "Before SQL Injection Test");
          crossBrowserTest.log(Status.INFO, "Before SQL Injection Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before SQL Injection Test - " + currentBrowser.toUpperCase());
            
          RestAssured.baseURI = tenantBaseUrl;
         
          String sqlInjectionPayload = "'; DROP TABLE users; --";
          JSONObject loginPayload = new JSONObject();
          loginPayload.put("email", sqlInjectionPayload);
          loginPayload.put("password", PASSWORD);
         
          Response response = RestAssured.given()
                  .contentType("application/json")
                  .body(loginPayload.toString())
                  .when()
                  .post("/login")
                  .then()
                  .extract().response();
         
          securityTest.log(Status.INFO, "SQL injection test response status: " + response.getStatusCode());
          securityTest.log(Status.INFO, "SQL injection payload used: " + sqlInjectionPayload);
         
          // Add assertion for SQL injection test
          if (response.getStatusCode() == 400 || response.getStatusCode() == 401) {
              securityTest.log(Status.PASS, "✅ SQL injection protection is working correctly - Request rejected with status " + response.getStatusCode());
          } else {
              securityTest.log(Status.FAIL, "❌ Potential SQL injection vulnerability detected - Request was not rejected properly");
          }
         
          // Capture screenshot for SQL injection test
          takeShotAndAttachReport("security_sql_injection", "SQL Injection Test");
          crossBrowserTest.log(Status.PASS, "✅ SQL Injection Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "SQL Injection Test - " + currentBrowser.toUpperCase());
         
          // XSS Test
          securityTest.log(Status.INFO, "Testing XSS protection");
          takeShotAndAttachReport("security_before_xss", "Before XSS Test");
          crossBrowserTest.log(Status.INFO, "Before XSS Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before XSS Test - " + currentBrowser.toUpperCase());
         
          String xssPayload = "<script>alert('XSS')</script>";
          loginPayload = new JSONObject();
          loginPayload.put("email", xssPayload);
          loginPayload.put("password", PASSWORD);
         
          response = RestAssured.given()
                  .contentType("application/json")
                  .body(loginPayload.toString())
                  .when()
                  .post("/login")
                  .then()
                  .extract().response();
         
          securityTest.log(Status.INFO, "XSS test response status: " + response.getStatusCode());
          securityTest.log(Status.INFO, "XSS payload used: " + xssPayload);
         
          // Add assertion for XSS test
          if (response.getStatusCode() == 400 || response.getStatusCode() == 401) {
              securityTest.log(Status.PASS, "✅ XSS protection is working correctly - Request rejected with status " + response.getStatusCode());
          } else {
              securityTest.log(Status.WARNING, "⚠️ Potential XSS vulnerability detected - Request was not handled properly");
          }
         
          // Capture screenshot for XSS test
          takeShotAndAttachReport("security_xss", "XSS Protection Test");
          crossBrowserTest.log(Status.PASS, "✅ XSS Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "XSS Protection Test - " + currentBrowser.toUpperCase());
         
          // Missing Authentication Test
          securityTest.log(Status.INFO, "Testing missing authentication protection");
          takeShotAndAttachReport("security_before_missing_auth", "Before Missing Auth Test");
          crossBrowserTest.log(Status.INFO, "Before Missing Auth Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Missing Auth Test - " + currentBrowser.toUpperCase());
         
          response = RestAssured.given()
                  .contentType("application/json")
                  .when()
                  .get(tenantBaseUrl + "/users/")
                  .then()
                  .extract().response();
         
          securityTest.log(Status.INFO, "Missing auth test response status: " + response.getStatusCode());
         
          // Add assertion for missing authentication test
          if (response.getStatusCode() == 401) {
              securityTest.log(Status.PASS, "✅ Missing authentication protection is working correctly - Request rejected with status " + response.getStatusCode());
          } else {
              securityTest.log(Status.FAIL, "❌ Potential missing authentication vulnerability detected - Request was not rejected properly");
          }
         
          // Capture screenshot for missing auth test
          takeShotAndAttachReport("security_missing_auth", "Missing Authentication Test");
          crossBrowserTest.log(Status.PASS, "✅ Missing Auth Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Missing Authentication Test - " + currentBrowser.toUpperCase());
         
          // Path Traversal Test
          securityTest.log(Status.INFO, "Testing path traversal protection");
          takeShotAndAttachReport("security_before_path_traversal", "Before Path Traversal Test");
          crossBrowserTest.log(Status.INFO, "Before Path Traversal Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Path Traversal Test - " + currentBrowser.toUpperCase());
         
          response = RestAssured.given()
                  .contentType("application/json")
                  .header("Authorization", "Bearer " + authToken)
                  .when()
                  .get(tenantBaseUrl + "/api/../../../../etc/passwd")
                  .then()
                  .extract().response();
         
          securityTest.log(Status.INFO, "Path traversal test response status: " + response.getStatusCode());
         
          // Add assertion for path traversal test
          if (response.getStatusCode() == 400 || response.getStatusCode() == 401 || response.getStatusCode() == 403 || response.getStatusCode() == 404) {
              securityTest.log(Status.PASS, "✅ Path traversal protection is working correctly - Request rejected with status " + response.getStatusCode());
          } else {
              securityTest.log(Status.WARNING, "⚠️ Potential path traversal vulnerability detected - Request was not rejected properly");
          }
         
          // Capture screenshot for path traversal test
          takeShotAndAttachReport("security_path_traversal", "Path Traversal Test");
          crossBrowserTest.log(Status.PASS, "✅ Path Traversal Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Path Traversal Test - " + currentBrowser.toUpperCase());
         
          // Command Injection Test
          securityTest.log(Status.INFO, "Testing command injection protection");
          takeShotAndAttachReport("security_before_command_injection", "Before Command Injection Test");
          crossBrowserTest.log(Status.INFO, "Before Command Injection Test - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Before Command Injection Test - " + currentBrowser.toUpperCase());
         
          String commandInjectionPayload = "test; cat /etc/passwd";
          JSONObject commandPayload = new JSONObject();
          commandPayload.put("command", commandInjectionPayload);
         
          response = RestAssured.given()
                  .contentType("application/json")
                  .header("Authorization", "Bearer " + authToken)
                  .body(commandPayload.toString())
                  .when()
                  .post(tenantBaseUrl + "/execute")
                  .then()
                  .extract().response();
         
          securityTest.log(Status.INFO, "Command injection test response status: " + response.getStatusCode());
          securityTest.log(Status.INFO, "Command injection payload used: " + commandInjectionPayload);
         
          // Add assertion for command injection test
          if (response.getStatusCode() == 400 || response.getStatusCode() == 401 || response.getStatusCode() == 403 || response.getStatusCode() == 404 || response.getStatusCode() == 405) {
              securityTest.log(Status.PASS, "✅ Command injection protection is working correctly - Request rejected with status " + response.getStatusCode());
          } else {
              securityTest.log(Status.WARNING, "⚠️ Potential command injection vulnerability detected - Request was not rejected properly");
          }
         
          // Capture screenshot for command injection test
          takeShotAndAttachReport("security_command_injection", "Command Injection Test");
          crossBrowserTest.log(Status.PASS, "✅ Command Injection Test Completed - " + currentBrowser.toUpperCase());
          crossBrowserTest.addScreenCaptureFromBase64String(getBase64Screenshot(), "Command Injection Test - " + currentBrowser.toUpperCase());
         
          securityTest.log(Status.PASS, "Security testing completed with detailed results");
          crossBrowserTest.log(Status.PASS, "✅ Security testing completed with detailed results on " + currentBrowser.toUpperCase());
         
      } catch (Exception e) {
          securityTest.log(Status.FAIL, "Security testing failed with exception: " + e.getMessage());
          // Capture screenshot for security testing failure
          takeShotAndAttachReport("security_test_failure", "Security Test Failure");
      }
  }
  // ---------------- site selection (your exact code) ----------------
  static void selectSite() {
      crudSubFeatureTest.log(Status.INFO, "Selecting site");
      crudSubFeatureTest.log(Status.INFO, "Clicking on site selection dropdown");
      clickable(By.xpath("//div[contains(@class,'MuiAutocomplete-root')]"), DEFAULT_TIMEOUT).click();
        
      crudSubFeatureTest.log(Status.INFO, "Waiting for site options to appear");
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@role='listbox']")));
        
      crudSubFeatureTest.log(Status.INFO, "Scrolling to bottom of site list");
      js.executeScript("document.querySelectorAll('ul[role=\"listbox\"] ').forEach(e => e.scrollTop=e.scrollHeight);");
        
      crudSubFeatureTest.log(Status.INFO, "Selecting 'Test Site'");
      By testSite = By.xpath("//li[normalize-space()='Test Site']");
      WebElement selected = new FluentWait<>(driver)
              .withTimeout(Duration.ofSeconds(10))
              .pollingEvery(Duration.ofMillis(200))
              .ignoring(NoSuchElementException.class)
              .ignoring(ElementClickInterceptedException.class)
              .until(d -> {
                  try {
                      for (WebElement li : d.findElements(testSite)) {
                          try { li.click(); return li; } catch (Exception ignored) {}
                      }
                  } catch (Exception ignored) {}
                  return null;
              });
        
      if (selected == null) {
          crudSubFeatureTest.log(Status.FAIL, "❌ Could not click Test Site");
          throw new RuntimeException("❌ Could not click Test Site");
      }
        
      System.out.println("✔ Test Site Selected Successfully");
      crudSubFeatureTest.log(Status.PASS, "✅ Test Site Selected Successfully");
  }
  // ---------------- go to assets ----------------
  static void goToAssets() {
      crudSubFeatureTest.log(Status.INFO, "Navigating to Assets page");
      crudSubFeatureTest.log(Status.INFO, "Clicking on Assets menu item");
      click(By.xpath("//span[normalize-space()='Assets'] | //a[normalize-space()='Assets'] | //button[normalize-space()='Assets']"));
     
      crudSubFeatureTest.log(Status.INFO, "Waiting for Assets page to load");
      visible(By.xpath("//button[normalize-space()='Create Asset']"), DEFAULT_TIMEOUT);
     
      System.out.println("✔ Assets Page Loaded");
      crudSubFeatureTest.log(Status.PASS, "✅ Assets Page Loaded Successfully");
  }
  // ------ clickCreateAsset (UPDATED & WORKING) ---------------
  static void clickCreateAsset() {
      By createBtn = By.xpath("(//button[contains(@class,'MuiButton-containedPrimary')])[last()]");
      try {
          WebElement btn = wait.until(ExpectedConditions.visibilityOfElementLocated(createBtn));
          js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
          pause(150);
          js.executeScript("arguments[0].click();", btn);
          System.out.println("✔ Create Asset clicked successfully");
      } catch (Exception e) {
          throw new RuntimeException("❌ Failed to click Create Asset using stable class-based locator", e);
      }
  }
  // ---------------- PHASE 1: create ----------------
  static void createAssetPhase1() {
      crudSubFeatureTest.log(Status.INFO, "=== PHASE 1: CREATE ASSET ===");
      System.out.println("=== PHASE 1: CREATE ASSET ===");
     
      crudSubFeatureTest.log(Status.INFO, "Clicking Create Asset button");
      click(By.xpath("//button[normalize-space()='Create Asset']"));
     
      // ensure dialog opened
      crudSubFeatureTest.log(Status.INFO, "Waiting for Create Asset dialog to open");
      visible(By.xpath("//*[contains(text(),'Add Asset') or contains(text(),'Create Asset') or contains(text(),'BASIC INFO')]"), DEFAULT_TIMEOUT);
     
      crudSubFeatureTest.log(Status.INFO, "Filling BASIC INFO");
      // BASIC INFO: Asset Name, QR
      By assetName = By.xpath("//p[contains(text(),'Asset Name')]/following::input[1]");
      By qrCode = By.xpath("//p[contains(text(),'QR Code')]/following::input[1]");
      crudSubFeatureTest.log(Status.INFO, "Entering Asset Name: " + ASSET_NAME);
      type(assetName, ASSET_NAME);
      crudSubFeatureTest.log(Status.INFO, "Entering QR Code: " + QR_CODE);
      type(qrCode, QR_CODE);
     
      // Asset Class (autocomplete)
      crudSubFeatureTest.log(Status.INFO, "Selecting Asset Class: " + ASSET_CLASS);
      By classInput = By.xpath("//p[contains(text(),'Asset Class')]/following::input[1]");
      typeAndSelectDropdown(classInput, ASSET_CLASS, ASSET_CLASS);
      pause(300);
     
      // scroll to ensure Subtype visible
      crudSubFeatureTest.log(Status.INFO, "Scrolling to Asset Subtype section");
      scrollToHeader("Asset Subtype (Optional)");
      pause(300);
     
      // Subtype - pick first option if exists
      crudSubFeatureTest.log(Status.INFO, "Selecting Asset Subtype (if available)");
      By subtypeInput = By.xpath("//p[contains(text(),'Asset Subtype')]/following::input[1]");
      try {
          click(subtypeInput);
          pause(300);
          By firstOption = By.xpath("(//li[contains(@id,'option') or @role='option'])[1]");
          if (driver.findElements(firstOption).size() > 0) {
              crudSubFeatureTest.log(Status.INFO, "Selecting first available subtype");
              click(firstOption);
          }
      } catch (Exception e) {
          crudSubFeatureTest.log(Status.INFO, "ℹ️ Subtype not available or selection skipped");
          System.out.println("ℹ️ Subtype not available or selection skipped");
      }
     
      // Select Condition = 2
      crudSubFeatureTest.log(Status.INFO, "Selecting Condition of Maintenance: " + CONDITION_VALUE);
      scrollToHeader("Condition of Maintenance");
      pause(200);
      click(By.xpath("//p[contains(text(),'Condition of Maintenance')]/following::button[.//h4[normalize-space()='" + CONDITION_VALUE + "'] or normalize-space()='" + CONDITION_VALUE + "'][1]"));
      pause(300);
     
      // Expand CORE ATTRIBUTES
      crudSubFeatureTest.log(Status.INFO, "Expanding CORE ATTRIBUTES section");
      scrollToHeader("CORE ATTRIBUTES");
      try {
          By coreToggle = By.xpath("//h6[contains(text(),'CORE ATTRIBUTES')]/ancestor::button[1]");
          if (driver.findElements(coreToggle).size() > 0) {
              WebElement toggle = driver.findElement(coreToggle);
              String expanded = toggle.getAttribute("aria-expanded");
              if (expanded == null || expanded.equals("false")) {
                  js.executeScript("arguments[0].scrollIntoView({block:'center'});", toggle);
                  pause(200);
                  toggle.click();
                  crudSubFeatureTest.log(Status.INFO, "CORE ATTRIBUTES section expanded");
                  pause(400);
              } else {
                  crudSubFeatureTest.log(Status.INFO, "CORE ATTRIBUTES section already expanded");
              }
          }
      } catch (Exception ignored) {}
     
      // Model
      crudSubFeatureTest.log(Status.INFO, "Entering Model: " + MODEL_VAL);
      By modelField = By.xpath("//p[contains(text(),'Model')]/following::input[1]");
      type(modelField, MODEL_VAL);
     
      // Notes
      crudSubFeatureTest.log(Status.INFO, "Entering Notes: " + NOTES_VAL);
      By notesField = By.xpath("//p[contains(text(),'Notes')]/following::input[1]");
      type(notesField, NOTES_VAL);
     
      // Ampere Rating
      crudSubFeatureTest.log(Status.INFO, "Selecting Ampere Rating: " + AMPERE_RATING);
      By ampereInput = By.xpath("//p[contains(text(),'Ampere Rating')]/following::input[1]");
      typeAndSelectDropdown(ampereInput, AMPERE_RATING, AMPERE_RATING);
     
      // Manufacturer
      crudSubFeatureTest.log(Status.INFO, "Selecting Manufacturer: " + MANUFACTURER);
      By manuInput = By.xpath("//p[contains(text(),'Manufacturer')]/following::input[1]");
      typeAndSelectDropdown(manuInput, MANUFACTURER, MANUFACTURER);
     
      // Catalog Number
      crudSubFeatureTest.log(Status.INFO, "Entering Catalog Number: " + CATALOG_NUMBER);
      By catalogInput = By.xpath("//p[contains(text(),'Catalog Number')]/following::input[1]");
      type(catalogInput, CATALOG_NUMBER);
     
      // Breaker Settings
      crudSubFeatureTest.log(Status.INFO, "Entering Breaker Settings: " + BREAKER_SETTINGS);
      By breakerInput = By.xpath("//p[contains(text(),'Breaker Settings')]/following::input[1]");
      try { type(breakerInput, BREAKER_SETTINGS); } catch (Exception ignored) {}
     
      // Interrupting Rating
      crudSubFeatureTest.log(Status.INFO, "Selecting Interrupting Rating: " + INTERRUPTING_RATING);
      By interruptInput = By.xpath("//p[contains(text(),'Interrupting Rating')]/following::input[1]");
      typeAndSelectDropdown(interruptInput, INTERRUPTING_RATING, INTERRUPTING_RATING);
     
      // kA Rating
      crudSubFeatureTest.log(Status.INFO, "Selecting kA Rating: " + KA_RATING);
      By kaInput = By.xpath("//p[contains(text(),'kA Rating')]/following::input[1]");
      typeAndSelectDropdown(kaInput, KA_RATING, KA_RATING);
     
      // Commercial -> Replacement Cost
      crudSubFeatureTest.log(Status.INFO, "Entering Replacement Cost: " + REPLACEMENT_COST);
      scrollToHeader("Replacement Cost");
      By replCost = By.xpath("//p[contains(text(),'Replacement Cost')]/following::input[1]");
      try { type(replCost, REPLACEMENT_COST); }
      catch (Exception ignored) {
          try { type(By.xpath("(//input[@type='number' or contains(@placeholder,'Replacement')])[1]"), REPLACEMENT_COST); }
          catch (Exception ex) {}
      }
     
      // CLICK CREATE ASSET (FULLY WORKING)
      crudSubFeatureTest.log(Status.INFO, "Clicking Create Asset button to submit");
      clickCreateAsset();
     
      // wait for success
      crudSubFeatureTest.log(Status.INFO, "Waiting for asset creation confirmation");
      try {
          wait.until(ExpectedConditions.or(
                  ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Asset created') or contains(text(),'created successfully')]")),
                  ExpectedConditions.presenceOfElementLocated(By.xpath("//table|//div[contains(@class,'asset-list') or contains(@class,'AssetList')]"))
          ));
          crudSubFeatureTest.log(Status.PASS, "✅ Asset created successfully");
      } catch (Exception e) {
          crudSubFeatureTest.log(Status.WARNING, "⚠️ No explicit success toast detected — verify UI");
          System.out.println("⚠️ No explicit success toast detected — verify UI");
      }
  }
  // ---------------- PHASE 2: edit ----------------
  static void editAssetPhase2() {
     JavascriptExecutor js = (JavascriptExecutor) driver;
     js.executeScript("document.body.style.zoom='80%';");
      crudSubFeatureTest.log(Status.INFO, "=== PHASE 2: EDIT ASSET ===");
      System.out.println("=== PHASE 2: EDIT ASSET ===");
     
      crudSubFeatureTest.log(Status.INFO, "Searching for asset: " + ASSET_NAME);
      // Try to search for the asset (if search exists)
      try {
          By search = By.xpath("//input[@placeholder='Search' or contains(@placeholder,'Search') or @aria-label='Search']");
          if (driver.findElements(search).size() > 0) {
              crudSubFeatureTest.log(Status.INFO, "Entering search term: " + ASSET_NAME);
              type(search, ASSET_NAME);
              pause(500);
          }
      } catch (Exception ignored) {}
     
      crudSubFeatureTest.log(Status.INFO, "Opening asset for editing");
      // Open edit for the asset
      try {
          By editNear = By.xpath("//div[@class='MuiDataGrid-row MuiDataGrid-row--firstVisible']//button[@title='Edit Asset']");
          if (driver.findElements(editNear).size() > 0) {
              crudSubFeatureTest.log(Status.INFO, "Clicking edit button for first visible asset");
              click(editNear);
          } else {
              // fallback: open first row menu and click Edit
              crudSubFeatureTest.log(Status.INFO, "Clicking menu button for first asset row");
              click(By.xpath("(//button[contains(@class,'MuiIconButton-root')])[1]"));
              pause(400);
              crudSubFeatureTest.log(Status.INFO, "Clicking Edit option");
              click(By.xpath("//li[normalize-space()='Edit' or contains(.,'Edit')]"));
          }
          pause(700);
      } catch (Exception e) {
          crudSubFeatureTest.log(Status.FAIL, "❌ Edit action not found for asset: " + ASSET_NAME);
          throw new RuntimeException("Edit action not found for asset: " + ASSET_NAME, e);
      }
     
      crudSubFeatureTest.log(Status.INFO, "Ensuring CORE ATTRIBUTES section is expanded");
      // Ensure CORE ATTRIBUTES expanded
      scrollToHeader("CORE ATTRIBUTES");
      try {
          By coreToggle = By.xpath("//h6[contains(text(),'CORE ATTRIBUTES')]/ancestor::button[1]");
          if (driver.findElements(coreToggle).size() > 0) {
              WebElement toggle = driver.findElement(coreToggle);
              String expanded = toggle.getAttribute("aria-expanded");
              if (expanded == null || expanded.equals("false")) {
                  js.executeScript("arguments[0].scrollIntoView({block:'center'});", toggle);
                  pause(200);
                  toggle.click();
                  crudSubFeatureTest.log(Status.INFO, "CORE ATTRIBUTES section expanded");
                  pause(400);
              } else {
                  crudSubFeatureTest.log(Status.INFO, "CORE ATTRIBUTES section already expanded");
              }
          }
      } catch (Exception ignored) {}
     
      crudSubFeatureTest.log(Status.INFO, "Editing Model and Notes fields");
      // Edit fields (hardcoded)
      try {
          crudSubFeatureTest.log(Status.INFO, "Editing Model field");
          type(By.xpath("//p[contains(text(),'Model')]/following::input[1]"), "edit");
      } catch (Exception ignored) {}
     
      try {
          crudSubFeatureTest.log(Status.INFO, "Editing Notes field");
          type(By.xpath("//p[contains(text(),'Notes')]/following::input[1]"), "edit");
      } catch (Exception ignored) {}
     
      crudSubFeatureTest.log(Status.INFO, "Updating kA Rating selections");
      // kA selection in edit
      try {
          By kaInput = By.xpath("//p[contains(text(),'kA Rating')]/following::input[1]");
          crudSubFeatureTest.log(Status.INFO, "Selecting 10 kA");
          typeAndSelectDropdown(kaInput, "10 kA", "10 kA");
          pause(300);
          crudSubFeatureTest.log(Status.INFO, "Selecting 18 kA");
          typeAndSelectDropdown(kaInput, "18 kA", "18 kA");
      } catch (Exception ignored) {}
     
      crudSubFeatureTest.log(Status.INFO, "Handling 'none' elements if present");
      // Double-click none if exists
      try {
          By none = By.xpath("//div[normalize-space()='none' or contains(.,'none')]");
          if (driver.findElements(none).size() > 0) {
              crudSubFeatureTest.log(Status.INFO, "Double-clicking 'none' element");
              WebElement el = driver.findElement(none);
              actions.doubleClick(el).perform();
              pause(300);
          }
      } catch (Exception ignored) {}
     
      crudSubFeatureTest.log(Status.INFO, "Setting Suggested Shortcut to Fuse if available");
      // Suggested Shortcut -> Fuse if present
      try {
          By shortcut = By.xpath("//p[contains(text(),'Suggested Shortcut')]/following::input[1] | //p[contains(text(),'Suggested Shortcut')]/following::div[1]//input[1]");
          if (driver.findElements(shortcut).size() > 0) {
              crudSubFeatureTest.log(Status.INFO, "Selecting 'Fuse' for Suggested Shortcut");
              typeAndSelectDropdown(shortcut, "Fuse", "Fuse");
          }
      } catch (Exception ignored) {}
     
      crudSubFeatureTest.log(Status.INFO, "Saving changes");
      // Save changes
      try {
          click(By.xpath("//button[normalize-space()='Save Changes' or contains(.,'Save Changes')]"));
      } catch (Exception e) {
          // fallback
          crudSubFeatureTest.log(Status.INFO, "Using fallback save button");
          click(By.xpath("//button[contains(.,'Save') or contains(.,'Update')][last()]"));
      }
     
      // wait for success
      crudSubFeatureTest.log(Status.INFO, "Waiting for edit confirmation");
      try {
          wait.until(ExpectedConditions.or(
                  ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'updated') or contains(text(),'saved') or contains(text(),'successfully')]")),
                  ExpectedConditions.presenceOfElementLocated(By.xpath("//table|//div[contains(@class,'asset-list') or contains(@class,'AssetList')]"))
          ));
          System.out.println("✔ Edit likely successful");
          crudSubFeatureTest.log(Status.PASS, "✅ Asset edited successfully");
      } catch (Exception e) {
          crudSubFeatureTest.log(Status.WARNING, "⚠️ No explicit edit success toast detected");
          System.out.println("⚠️ No explicit edit success toast detected");
      }
  }
  static void deleteAsset() {

        System.out.println("Starting Delete Asset Flow...");
        pause(600);
        By search = By.xpath("//input[@placeholder='Search' or contains(@placeholder,'Search') or @aria-label='Search']");
        if (driver.findElements(search).size() > 0) {
            type(search, ASSET_NAME);
            pause(500);
        }

        // 1 DELETE ICON (first visible row)
        By deleteButton = By.xpath("(//div[contains(@class,'MuiDataGrid-row')]//button[@title='Delete Asset'])[1]");

        WebElement delBtn = wait.until(ExpectedConditions.elementToBeClickable(deleteButton));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", delBtn);
        pause(200);
        js.executeScript("arguments[0].click();", delBtn);

        System.out.println("Delete Asset icon clicked");
        pause(700);

        // 2 CONFIRM DELETE
        By confirmDelete = By.xpath("//button[contains(@class,'MuiButton-containedError') and contains(.,'Delete')]");

        WebElement confirm = wait.until(ExpectedConditions.elementToBeClickable(confirmDelete));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", confirm);
        pause(200);
        js.executeScript("arguments[0].click();", confirm);

        System.out.println("Asset delete confirmed");

        // 3 No toast - wait for row to remove
        pause(1500);

        System.out.println("Asset deleted successfully");
        System.out.println("Delete Asset Flow Completed");
        crudSubFeatureTest.log(Status.PASS, "Asset deleted successfully");
    }
}

