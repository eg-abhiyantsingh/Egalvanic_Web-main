# ACME QA Automation Suite

This is a comprehensive QA automation suite for testing the ACME application (https://acme.egalvanic.ai). The suite includes UI automation, API testing, security testing, and performance testing.

## Project Structure

```
qa-automation-suite/
├── pom.xml                     # Maven configuration file
├── testng.xml                  # TestNG suite configuration
├── src/
│   ├── main/java/com/acme/
│   │   ├── config/
│   │   │   └── BaseConfig.java          # Base configuration class
│   │   ├── pages/
│   │   │   ├── LoginPage.java           # Page Object Model for Login page
│   │   │   └── DashboardPage.java       # Page Object Model for Dashboard page
│   │   └── utils/
│   │       ├── ExtentReporterNG.java    # Extent Reports utility
│   │       └── PerformanceUtils.java    # Performance measurement utilities
│   └── test/java/com/acme/
│       ├── tests/
│       │   ├── ui/
│       │   │   └── LoginPageTest.java   # UI functional tests
│       │   ├── api/
│       │   │   ├── BaseAPITest.java     # Base API test class
│       │   │   ├── UserAPITest.java     # User API tests
│       │   │   └── AuthenticationTest.java # Authentication API tests
│       │   ├── security/
│       │   │   ├── UISecurityTest.java  # UI security tests
│       │   │   └── APISecurityTest.java # API security tests
│       │   └── performance/
│       │       ├── UIPerformanceTest.java  # UI performance tests
│       │       └── APIPerformanceTest.java # API performance tests
└── test-output/
    ├── reports/                # Generated Extent Reports
    └── screenshots/            # Screenshots captured during tests
```

## Technologies Used

- **Java 11+** - Primary programming language
- **Maven** - Build automation tool
- **TestNG** - Testing framework
- **Selenium WebDriver** - UI automation
- **REST Assured** - API testing
- **Extent Reports** - HTML reporting
- **Allure** - Advanced reporting framework
- **WebDriverManager** - Driver management

## Prerequisites

1. Java 11 or higher
2. Maven 3.6 or higher
3. Chrome browser (latest version)

## Installation

1. Clone the repository
2. Navigate to the project directory
3. Run `mvn clean install` to download dependencies

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Suites

```bash
# Run UI tests only
mvn test -Dtestng.suiteXmlFile=testng-ui.xml

# Run API tests only
mvn test -Dtestng.suiteXmlFile=testng-api.xml

# Run Security tests only
mvn test -Dtestng.suiteXmlFile=testng-security.xml

# Run Performance tests only
mvn test -Dtestng.suiteXmlFile=testng-performance.xml
```

### Run Individual Test Classes

```bash
# Run Login Page tests
mvn test -Dtest=com.acme.tests.ui.LoginPageTest

# Run User API tests
mvn test -Dtest=com.acme.tests.api.UserAPITest
```

## Test Coverage

### UI Functional Testing
- Login page validation
- Valid and invalid login scenarios
- Dashboard navigation
- Site dropdown functionality
- Form validation
- Error handling

### API Testing
- Authentication endpoints
- User management endpoints
- CRUD operations
- Response validation
- Schema validation
- Error message validation

### Security Testing
- SQL injection testing
- XSS injection testing
- JWT token validation
- Authentication bypass testing
- Parameter manipulation
- Session management

### Performance Testing
- Page load time measurement
- API response time tracking
- Resource loading performance
- Concurrent request handling
- Memory usage monitoring

## Reporting

### Extent Reports
- Detailed HTML reports with screenshots
- Test execution summary
- Step-by-step logging
- Performance metrics
- Failure analysis

### Allure Reports
- Interactive test reports
- Real-time test execution
- Detailed test history
- Trend analysis

Reports are generated in the `test-output/reports/` directory after test execution.

## Configuration

### Test Data
Test credentials and configuration are defined in `BaseConfig.java`:
- Base URL: https://acme.egalvanic.ai
- Test Email: rahul+acme@egalvanic.com
- Test Password: RP@egalvanic123

### Browser Configuration
Chrome browser is configured with:
- Maximized window
- Disabled automation indicators
- Performance optimization settings

## Screenshots

Screenshots are automatically captured for:
- Failed test cases
- Performance bottlenecks
- Security vulnerabilities
- UI rendering issues

Screenshots are saved in the `test-output/screenshots/` directory.

## CI/CD Integration

The test suite can be integrated with CI/CD pipelines:
- Jenkins
- GitHub Actions
- GitLab CI
- Azure DevOps

## Best Practices Implemented

1. **Page Object Model** - Maintainable and reusable page components
2. **TestNG Annotations** - Proper test organization and execution
3. **Extent Reports** - Comprehensive test reporting
4. **WebDriverManager** - Automatic driver management
5. **Performance Monitoring** - Response time tracking
6. **Security Validation** - Vulnerability assessment
7. **Screenshot Capture** - Visual evidence of test execution

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a pull request

## License

This project is licensed under the MIT License.

## Support

For support, please contact the QA team or create an issue in the repository.