# ACME QA Automation Suite

This comprehensive QA automation suite tests the ACME application (https://acme.egalvanic.ai) using industry-standard practices and tools. The suite provides full coverage for UI, API, security, and performance testing with detailed reporting.

## 🏗️ Project Architecture

```
qa-automation-suite/
├── pom.xml                           # Maven configuration with all dependencies
├── testng.xml                        # TestNG suite configuration
├── src/
│   ├── main/java/com/acme/
│   │   ├── config/
│   │   │   └── BaseConfig.java              # Global configuration and WebDriver management
│   │   ├── pages/
│   │   │   ├── LoginPage.java               # Page Object Model for Login functionality
│   │   │   └── DashboardPage.java           # Page Object Model for Dashboard functionality
│   │   ├── mobile/
│   │   │   ├── BaseMobileTest.java          # Base class for mobile tests
│   │   │   ├── MobileConfig.java            # Mobile testing configuration
│   │   │   ├── MobileActions.java           # Common mobile interactions
│   │   │   └── pages/
│   │   │       └── MobileLoginPage.java     # Mobile POM for Login functionality
│   │   └── utils/
│   │       ├── ExtentReporterNG.java        # Extent Reports integration with TestNG
│   │       ├── PerformanceUtils.java        # Performance measurement utilities
│   │       ├── RetryAnalyzer.java           # Test retry mechanism
│   │       ├── DataProviderUtil.java        # Data-driven testing utilities
│   │       ├── TestDataManager.java         # Test data management utilities
│   │       ├── LoggerUtil.java              # Comprehensive logging utilities
│   │       └── WaitUtils.java              # Advanced wait utilities
│   └── test/java/com/acme/
│       └── tests/
│           ├── ui/
│           │   ├── LoginPageTest.java       # UI functional tests for login scenarios
│           │   ├── ComprehensiveWebsiteTest.java
│           │   └── DropdownFunctionalityTest.java
│           ├── api/
│           │   ├── BaseAPITest.java         # Base class for API tests
│           │   ├── UserAPITest.java         # User management API tests
│           │   └── AuthenticationTest.java  # Authentication API tests
│           ├── mobile/
│           │   ├── SampleMobileTest.java    # Sample mobile functional tests
│           │   └── MobileLoginTest.java    # Mobile login functionality tests
│           ├── security/
│           │   ├── UISecurityTest.java      # UI security vulnerability tests
│           │   └── APISecurityTest.java    # API security vulnerability tests
│           ├── performance/
│               ├── UIPerformanceTest.java   # UI performance benchmarking
│               └── APIPerformanceTest.java  # API response time testing
│           └── utilities/
│               └── TestDataManagerTest.java # Test data management utilities tests
├── src/main/resources/
│   ├── config.properties                    # Configuration properties
│   └── testdata/
│       └── login_test_data.csv              # Sample test data for data-driven testing
└── test-output/
    ├── reports/                   # Generated HTML reports (Extent & Allure)
    └── screenshots/               # Screenshots captured during test execution
```

## 🛠️ Technologies & Frameworks

- **Core**: Java 11+, Maven 3.6+
- **UI Testing**: Selenium WebDriver 4.15.0, TestNG 7.8.0
- **API Testing**: REST Assured 5.4.0
- **Mobile Testing**: Appium Java Client 8.6.0
- **Reporting**: Extent Reports 5.1.1, Allure 2.24.0
- **Utilities**: WebDriverManager 5.6.3, Jackson 2.16.0, Apache Commons CSV 1.10.0, JSON 20231013

## ▶️ Quick Start

```bash
# 1. Install dependencies
mvn clean install

# 2. Run all tests
mvn test

# 3. Run specific test suites
mvn test -DsuiteXmlFile=testng.xml         # All tests
mvn test -DsuiteXmlFile=testng-ui.xml      # UI tests only
mvn test -DsuiteXmlFile=testng-api.xml     # API tests only
mvn test -DsuiteXmlFile=testng-security.xml # Security tests only
mvn test -DsuiteXmlFile=mobile-testng.xml  # Mobile tests only

# 4. Run with specific browser
mvn test -Dbrowser=firefox    # Run tests in Firefox
mvn test -Dbrowser=edge       # Run tests in Edge

# 5. Run mobile tests with specific platform
mvn test -DsuiteXmlFile=mobile-testng.xml -Dmobile.platform=android
mvn test -DsuiteXmlFile=mobile-testng.xml -Dmobile.platform=ios
```

Reports are automatically generated in `test-output/reports/` after execution.

## 🧪 Enhanced Test Features

### 1. **Advanced Configuration Management**
- Externalized configuration in `config.properties`
- Support for multiple environments
- Runtime configuration via system properties

### 2. **Robust Error Handling & Retries**
- Intelligent retry mechanism for flaky tests
- Comprehensive exception handling
- Detailed error logging with context

### 3. **Data-Driven Testing**
- CSV-based test data management
- Flexible data providers
- Easy test data maintenance

### 4. **Cross-Browser Testing**
- Support for Chrome, Firefox, Edge, and Safari
- Consistent test execution across browsers
- Browser-specific configurations

### 5. **Hierarchical Reporting**
- Class and method-level test organization
- Enhanced Extent Reports with nested structure
- Automatic failure screenshot capture

### 6. **Advanced Wait Strategies**
- Smart wait utilities for element interactions
- Fluent wait implementations
- Custom condition handling

## 🧪 Test Coverage Matrix

| Test Type | Coverage Areas | Tools Used |
|-----------|----------------|------------|
| **UI Functional** | Login validation, Form handling, Navigation, Dropdown interactions | Selenium WebDriver, TestNG |
| **API Testing** | Authentication endpoints, User management, CRUD operations | REST Assured |
| **Mobile Testing** | App launch, Basic interactions, Gestures, Cross-platform support | Appium |
| **Security** | SQL injection, XSS protection, JWT validation, Parameter tampering | Custom security tests |
| **Performance** | Page load times, API response metrics, Resource optimization | Custom performance utilities |
| **Utilities** | Test data management, Random data generation, Sensitive data masking | Custom utilities |

## 📊 Reporting Features

- **Extent Reports**: Detailed HTML reports with step-by-step execution logs
- **Allure Reports**: Interactive dashboards with real-time test analytics
- **Screenshot Evidence**: Automatic capture for failed tests and key milestones
- **Performance Metrics**: Response time tracking and bottleneck identification
- **Hierarchical Structure**: Class and method-level organization for better navigation

## 🏆 Best Practices Implemented

1. ✅ Page Object Model for maintainable test code
2. ✅ TestNG annotations for proper test organization
3. ✅ Parallel test execution capability
4. ✅ Comprehensive error handling and logging
5. ✅ Cross-browser compatibility support
6. ✅ CI/CD pipeline readiness
7. ✅ Data-driven testing approach
8. ✅ Retry mechanism for flaky tests
9. ✅ Advanced wait strategies
10. ✅ Hierarchical reporting structure
11. ✅ Mobile testing capabilities with Appium
12. ✅ Cross-platform mobile testing (Android & iOS)
13. ✅ Comprehensive test data management utilities
14. ✅ JSON and properties file test data handling
15. ✅ Random test data generation
16. ✅ Sensitive data masking for secure logging

## 👥 Team Guidelines

- All new test cases should follow the existing POM structure
- Test methods should have clear priorities and descriptions
- Utility functions should be reusable across test classes
- All changes require peer review before merging
- Use data-driven approach for parametrized tests
- Follow retry analyzer for flaky test handling

## 🚀 Advanced Usage

### Running Tests with Different Configurations

```bash
# Run with specific browser
mvn test -Dbrowser=firefox

# Run with custom report title
mvn test -Dreport.title="Custom Report Title"

# Run with specific environment
mvn test -Denvironment=Staging

# Run with retry count override
mvn test -Dretry.count=5
```

### Creating New Tests

1. Extend existing page objects or create new ones in `src/main/java/com/acme/pages/`
2. Add new test methods in appropriate test classes in `src/test/java/com/acme/tests/`
3. Use provided utilities for logging, waiting, and data handling
4. Follow the existing naming conventions and structure
5. Use TestDataManager for complex test data handling
6. Leverage mobile testing capabilities for cross-platform coverage

---

*For questions or support, contact the QA team.*