# QA Automation Suite

This is a comprehensive QA automation suite for testing the ACME application authentication functionality.

## Project Structure

```
qa-automation-suite/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/egalvanic/qa/
│   │   │       ├── Main.java
│   │   │       ├── testcase/
│   │   │       │   ├── BaseTest.java
│   │   │       │   ├── AuthenticationTest.java
│   │   │       │   └── AuthenticationTestNG.java
│   │   │       ├── pageobjects/
│   │   │       │   ├── LoginPage.java
│   │   │       │   └── DashboardPage.java
│   │   │       ├── locators/
│   │   │       │   ├── LoginLocators.java
│   │   │       │   └── DashboardLocators.java
│   │   │       └── utils/
│   │   │           ├── DriverManager.java
│   │   │           ├── ReportManager.java
│   │   │           └── ConfigReader.java
│   │   └── resources/
│   │       └── config.properties
├── test-output/
│   ├── reports/
│   └── screenshots/
├── pom.xml
└── testng.xml
```

## Prerequisites

- Java 11 or higher
- Maven
- Chrome browser

## Configuration

The application uses a property file for configuration located at `src/main/resources/config.properties`:

```properties
# Application Configuration
base.url=https://acme.qa.egalvanic.ai
user.email=rahul+acme@egalvanic.com

# Test Configuration
default.timeout=25
browser.type=chrome

# Report Configuration
report.directory=test-output/reports
screenshot.directory=test-output/screenshots
```

## Running Tests

### Option 1: Using TestNG (Recommended)

To run the TestNG test suite with all 22 test cases:

```bash
mvn test
```

This will execute all tests defined in the `testng.xml` file and generate detailed reports.

### Option 2: Using Main Class

To run the original test suite:

```bash
mvn exec:java
```

## Test Cases

The suite includes 22 comprehensive authentication test cases covering:

1. Valid credentials login
2. Invalid credentials login
3. Username with trailing spaces
4. Username with leading spaces
5. Username with leading and trailing spaces
6. Username with only spaces
7. Password with trailing spaces
8. Username with special characters
9. Numeric-only username
10. Case sensitivity in username
11. Exceeding maximum length username
12. Password with leading spaces
13. Password with only spaces
14. Minimum length password
15. Maximum length password
16. SQL injection in username
17. XSS attack in username
18. Error message clearing
19. Unauthorized dashboard access
20. Session persistence after refresh
21. Login using Enter key
22. Session handling with back button

## Reports

Test execution generates detailed HTML reports using Extent Reports:

- **Primary Report**: `test-output/reports/TestReport.html`
- **Authentication Report**: `test-output/reports/AuthenticationReport.html`
- **Screenshots**: `test-output/screenshots/`

## Technologies Used

- Java 11+
- Selenium WebDriver 4.16.1
- WebDriverManager 5.6.3
- TestNG 7.8.0
- ExtentReports 5.0.9
- REST Assured 5.4.0
- Apache HttpClient 4.5.13
