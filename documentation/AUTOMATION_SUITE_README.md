# Enhanced ACME Automation Suite

This is an enhanced automation suite that performs comprehensive testing including UI automation, API testing, performance testing, and security testing.

## Features

1. **UI Automation**: 
   - Login to ACME application
   - Site selection from dropdown
   - Asset creation and editing

2. **API Testing**:
   - Authentication testing
   - User endpoint validation
   - Response size validation

3. **Performance Testing**:
   - UI page load time measurement
   - Concurrent API requests testing
   - Response time analysis

4. **Security Testing**:
   - SQL injection protection testing
   - XSS protection testing
   - Authentication validation

5. **Reporting**:
   - Detailed Extent Reports with screenshots
   - Performance metrics
   - Test execution status

## Prerequisites

- Java 11 or higher
- Maven
- Chrome browser

## Dependencies Included

- Selenium WebDriver 4.15.0
- REST Assured 5.4.0
- Extent Reports 5.0.9
- JSON 20231013
- WebDriverManager 5.6.3

## How to Run

### Option 1: Using Maven
```bash
# Compile the project
mvn compile

# Run the automation
mvn exec:java
```

### Option 2: Using the Script
```bash
# Make sure the script is executable
chmod +x run_full_automation.sh

# Run the automation suite
./run_full_automation.sh
```

## Output

After execution, the following will be generated:

1. **HTML Report**: `test-output/reports/AutomationReport.html`
2. **Screenshots**: `test-output/screenshots/`

## Sharing Reports with Others

When sharing reports with colleagues or managers, use the packaging script to ensure all images display correctly:

```bash
./package_report.sh
```

This creates a zip file containing:
- The HTML report with embedded images
- All screenshots
- Technical explanation file for developers
- Manager explanation file (MANAGER_EXPLANATION.md)

The packaged report will display images correctly on any computer, even without access to the original file structure.

## Report Contents

The Extent Report includes:

- Test execution timeline
- Detailed step-by-step logs
- Performance metrics
- Screenshots for each major step
- API test results
- Security test results
- Performance analysis

## Customization

You can customize the following in `Egalvanic.java`:

- Login credentials (EMAIL, PASSWORD)
- Asset details (ASSET_NAME, QR_CODE, etc.)
- Test timeouts (DEFAULT_TIMEOUT)
- API endpoints (API_BASE_URL)

## Troubleshooting

If you encounter issues:

1. Ensure all dependencies are properly downloaded by running `mvn compile`
2. Check that Chrome browser is installed
3. Verify internet connectivity for API tests
4. Check the console output for specific error messages

## Support

For issues or enhancements, please contact the QA team.