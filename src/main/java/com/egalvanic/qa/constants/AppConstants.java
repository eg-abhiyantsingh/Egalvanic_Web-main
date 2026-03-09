package com.egalvanic.qa.constants;

/**
 * Application Constants
 * Centralized configuration for the entire web test framework
 */
public class AppConstants {

    private AppConstants() {
        // Private constructor to prevent instantiation
    }

    // ============================================
    // WEB APPLICATION CONFIGURATION
    // ============================================
    public static final String BASE_URL = getEnv("BASE_URL", "https://acme.qa.egalvanic.ai");
    public static final String BROWSER = getEnv("BROWSER", "chrome");

    // ============================================
    // TEST DATA - AUTHENTICATION
    // ============================================
    public static final String VALID_EMAIL = getEnv("USER_EMAIL", "rahul+acme@egalvanic.com");
    public static final String VALID_PASSWORD = getEnv("USER_PASSWORD", "RP@egalvanic123");
    public static final String INVALID_EMAIL = "invalidemail@test.com";
    public static final String INVALID_PASSWORD = "wrongpassword123";

    // ============================================
    // TEST DATA - SITE SELECTION
    // ============================================
    public static final String TEST_SITE_NAME = "Test Site";

    // ============================================
    // TIMEOUTS (in seconds)
    // ============================================
    public static final int IMPLICIT_WAIT = 5;
    public static final int EXPLICIT_WAIT = 10;
    public static final int DEFAULT_TIMEOUT = 25;
    public static final int PAGE_LOAD_TIMEOUT = 45;

    // ============================================
    // REPORT PATHS
    // ============================================
    public static final String DETAILED_REPORT_PATH = "reports/detailed/";
    public static final String CLIENT_REPORT_PATH = "reports/client/";
    public static final String SCREENSHOT_PATH = "test-output/screenshots/";

    // ============================================
    // MODULE NAMES (for Reports)
    // ============================================
    public static final String MODULE_AUTHENTICATION = "Authentication";
    public static final String MODULE_SITE_SELECTION = "Site Selection";
    public static final String MODULE_ASSET = "Asset Management";
    public static final String MODULE_LOCATIONS = "Locations";
    public static final String MODULE_CONNECTIONS = "Connections";
    public static final String MODULE_ISSUES = "Issues";
    public static final String MODULE_SMOKE_TEST = "Smoke Test";

    // ============================================
    // FEATURE NAMES - AUTHENTICATION
    // ============================================
    public static final String FEATURE_LOGIN = "Login";
    public static final String FEATURE_SESSION = "Session Management";

    // ============================================
    // FEATURE NAMES - ASSET MANAGEMENT
    // ============================================
    public static final String FEATURE_ASSET_LIST = "Asset List";
    public static final String FEATURE_CREATE_ASSET = "Create Asset";
    public static final String FEATURE_EDIT_ASSET = "Edit Asset";
    public static final String FEATURE_DELETE_ASSET = "Delete Asset";
    public static final String FEATURE_ASSET_CRUD = "Asset CRUD";

    // ============================================
    // FEATURE NAMES - LOCATIONS
    // ============================================
    public static final String FEATURE_LOCATION_LIST = "Location List";
    public static final String FEATURE_CREATE_BUILDING = "Create Building";
    public static final String FEATURE_CREATE_FLOOR = "Create Floor";
    public static final String FEATURE_CREATE_ROOM = "Create Room";
    public static final String FEATURE_EDIT_LOCATION = "Edit Location";
    public static final String FEATURE_DELETE_LOCATION = "Delete Location";
    public static final String FEATURE_LOCATION_CRUD = "Location CRUD";

    // ============================================
    // FEATURE NAMES - CONNECTIONS
    // ============================================
    public static final String FEATURE_ADD_CONNECTION = "Add Connection";
    public static final String FEATURE_DELETE_CONNECTION = "Delete Connection";

    // ============================================
    // FEATURE NAMES - ISSUES
    // ============================================
    public static final String FEATURE_CREATE_ISSUE = "Create Issue";
    public static final String FEATURE_EDIT_ISSUE = "Edit Issue";
    public static final String FEATURE_DELETE_ISSUE = "Delete Issue";

    // ============================================
    // EMAIL CONFIGURATION
    // ============================================
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 587;
    public static final String EMAIL_FROM = getEnv("EMAIL_FROM", "abhiyant.singh@egalvanic.com");
    public static final String EMAIL_PASSWORD = getEnv("EMAIL_PASSWORD", "");
    public static final String EMAIL_TO = getEnv("EMAIL_TO", "abhiyant.singh@egalvanic.com");
    public static final String EMAIL_SUBJECT = "eGalvanic Web Automation - Test Report";
    public static final boolean SEND_EMAIL_ENABLED = false;

    // ============================================
    // HELPER METHODS
    // ============================================

    private static String getEnv(String key, String defaultValue) {
        // First check system properties (set via Maven -D flags)
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Then check environment variables
        value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }
}
