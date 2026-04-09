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
    public static final String VALID_EMAIL = getEnv("USER_EMAIL", "abhiyant.singh+admin@egalvanic.com");
    public static final String VALID_PASSWORD = getEnv("USER_PASSWORD", "RP@egalvanic123");
    public static final String INVALID_EMAIL = "invalidemail@test.com";
    public static final String INVALID_PASSWORD = "wrongpassword123";

    // ============================================
    // COMPANY CODE / SUBDOMAIN CONFIGURATION
    // ============================================
    public static final String VALID_COMPANY_CODE = "acme";
    public static final String INVALID_COMPANY_CODE = "invalidxyz999";
    public static final String QA_DOMAIN = "qa.egalvanic.ai";

    // ============================================
    // API CONFIGURATION
    // ============================================
    public static final String API_BASE_URL = BASE_URL + "/api";

    // ============================================
    // FEATURE NAMES - COMPANY CODE
    // ============================================
    public static final String FEATURE_COMPANY_CODE = "Company Code";

    // ============================================
    // MODULE & FEATURE NAMES - API
    // ============================================
    public static final String MODULE_API = "API Tests";
    public static final String FEATURE_API_AUTH = "API Authentication";
    public static final String FEATURE_API_USERS = "API Users";
    public static final String FEATURE_API_SECURITY = "API Security";
    public static final String FEATURE_API_PERFORMANCE = "API Performance";

    // ============================================
    // ROLE-BASED CREDENTIALS
    // ============================================
    public static final String ADMIN_EMAIL = getEnv("ADMIN_EMAIL", "abhiyant.singh+admin@egalvanic.com");
    public static final String ADMIN_PASSWORD = getEnv("ADMIN_PASSWORD", "RP@egalvanic123");
    public static final String PM_EMAIL = getEnv("PM_EMAIL", "abhiyant.singh+projectmanger@egalvanic.com");
    public static final String PM_PASSWORD = getEnv("PM_PASSWORD", "RP@egalvanic123");
    public static final String TECH_EMAIL = getEnv("TECH_EMAIL", "abhiyant.singh+technician@egalvanic.com");
    public static final String TECH_PASSWORD = getEnv("TECH_PASSWORD", "RP@egalvanic123");
    public static final String FM_EMAIL = getEnv("FM_EMAIL", "abhiyant.singh+facilitymanager@egalvanic.com");
    public static final String FM_PASSWORD = getEnv("FM_PASSWORD", "RP@egalvanic123");
    public static final String CP_EMAIL = getEnv("CP_EMAIL", "abhiyant.singh+clientportal@egalvanic.com");
    public static final String CP_PASSWORD = getEnv("CP_PASSWORD", "RP@egalvanic123");

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
    public static final String DETAILED_REPORT_PATH = "reports/detail-report/";
    public static final String CLIENT_REPORT_PATH = "reports/client-report/";
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
    // FEATURE NAMES - SITE SELECTION
    // ============================================
    public static final String FEATURE_SELECT_SITE = "Select Site";
    public static final String FEATURE_SEARCH_SITES = "Search Sites";
    public static final String FEATURE_ONLINE_OFFLINE = "Online/Offline";
    public static final String FEATURE_OFFLINE_SYNC = "Offline Sync";
    public static final String FEATURE_DASHBOARD = "Dashboard";
    public static final String FEATURE_DASHBOARD_BADGES = "Dashboard Badges";
    public static final String FEATURE_DASHBOARD_HEADER = "Dashboard Header";
    public static final String FEATURE_JOB_SELECTION = "Job Selection";
    public static final String FEATURE_EDGE_CASES = "Edge Cases";
    public static final String FEATURE_PERFORMANCE = "Performance";

    // ============================================
    // FEATURE NAMES - AUTHENTICATION
    // ============================================
    public static final String FEATURE_LOGIN = "Login";
    public static final String FEATURE_SESSION = "Session Management";
    public static final String FEATURE_ROLE_ACCESS = "Role-Based Access";

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
    public static final String FEATURE_RESOLVE_ISSUE = "Resolve Issue";
    public static final String FEATURE_ACTIVATE_JOBS = "Activate Jobs";
    public static final String FEATURE_SEARCH_ISSUE = "Search Issue";
    public static final String FEATURE_ISSUE_PHOTOS = "Issue Photos";

    // ============================================
    // MODULE & FEATURE NAMES - WORK ORDERS
    // ============================================
    public static final String MODULE_WORK_ORDERS = "Work Orders";
    public static final String FEATURE_CREATE_WORK_ORDER = "Create Work Order";
    public static final String FEATURE_EDIT_WORK_ORDER = "Edit Work Order";
    public static final String FEATURE_WO_IR_PHOTOS = "IR Photos";
    public static final String FEATURE_WO_LOCATIONS = "Locations";
    public static final String FEATURE_WO_TASKS = "Tasks";
    public static final String FEATURE_WO_FILTER = "Filter";

    // ============================================
    // MODULE & FEATURE NAMES - BUG HUNT / SECURITY
    // ============================================
    public static final String MODULE_BUG_HUNT = "Bug Hunt & Security";
    public static final String FEATURE_XSS_PROTECTION = "XSS Protection";
    public static final String FEATURE_INPUT_VALIDATION = "Input Validation";
    public static final String FEATURE_CONSOLE_ERRORS = "Console Errors";
    public static final String FEATURE_LOGIN_UX = "Login UX";
    public static final String FEATURE_SESSION_SECURITY = "Session Security";
    public static final String FEATURE_HTTP_SECURITY = "HTTP Security Headers";
    public static final String FEATURE_ERROR_HANDLING = "Error Handling";

    // ============================================
    // FEATURE NAMES - BUG HUNT VERIFICATION
    // ============================================
    public static final String FEATURE_DEVREV_SDK = "DevRev SDK";
    public static final String FEATURE_API_N_PLUS_1 = "N+1 API Pattern";
    public static final String FEATURE_SENTRY_EXPOSURE = "Sentry Credentials Exposure";
    public static final String FEATURE_UPDATE_BANNER = "Update Banner";
    public static final String FEATURE_URL_STATE = "URL State Persistence";
    public static final String FEATURE_DUPLICATE_API = "Duplicate API Calls";
    public static final String FEATURE_SITE_SELECTOR = "Site Selector";
    public static final String FEATURE_SENTRY_REPORTING = "Sentry Reporting";
    public static final String FEATURE_BEAMER_LEAK = "Beamer Data Leak";
    public static final String FEATURE_BUNDLE_HASH = "JS Bundle Consistency";
    public static final String FEATURE_DASHBOARD_LAYOUT = "Dashboard Layout";
    public static final String FEATURE_COMPANY_INFO = "Company Information";
    public static final String FEATURE_TASKS_DATA = "Tasks Data Quality";
    public static final String FEATURE_DATE_FORMAT = "Date Format Consistency";
    public static final String FEATURE_CONNECTIONS_GRID = "Connections Grid";
    public static final String FEATURE_ARC_FLASH = "Arc Flash Readiness";
    public static final String FEATURE_WO_GRID = "Work Order Grid";
    public static final String FEATURE_LOCATION_DATA = "Location Data";
    public static final String FEATURE_TEST_DATA = "Test Data Pollution";
    public static final String FEATURE_ADMIN_SETTINGS = "Admin Settings";
    public static final String FEATURE_OPPORTUNITIES = "Opportunities";
    public static final String FEATURE_SCHEDULING = "Scheduling";
    public static final String FEATURE_CONDITION_ASSESSMENT = "Condition Assessment";
    public static final String FEATURE_EQUIPMENT_INSIGHTS = "Equipment Insights";
    public static final String FEATURE_SLDS_PAGE = "SLDs Page";
    public static final String FEATURE_ATTACHMENTS = "Attachments";
    public static final String FEATURE_AUDIT_LOG = "Audit Log";
    public static final String FEATURE_Z_UNIVERSITY = "Z University";

    // ============================================
    // EMAIL CONFIGURATION
    // ============================================
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 587;
    public static final String EMAIL_FROM = getEnv("EMAIL_FROM", "abhiyant.singh@egalvanic.com");
    public static final String EMAIL_PASSWORD = getEnv("EMAIL_PASSWORD", "onmzhjxnacinjfun");
   public static final String EMAIL_TO = "dharmesh.avaiya@egalvanic.com, abhiyant.singh@egalvanic.com, ";
    public static final String EMAIL_SUBJECT = "eGalvanic Web Automation - Test Report";
    public static final boolean SEND_EMAIL_ENABLED =
            Boolean.parseBoolean(getEnv("SEND_EMAIL_ENABLED", "true"));

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
