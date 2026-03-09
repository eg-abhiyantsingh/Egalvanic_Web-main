# Selenium Automation for ACME Login

This project automates the login process for https://acme.egalvanic.ai using Selenium WebDriver.

## Project Structure

- `final_login.py` - The main Python script that performs the automation
- `pom.xml` - Maven configuration for Java version
- `LoginAutomation.java` - Java implementation
- `requirements.txt` - Python dependencies
- Various test and helper scripts

## What the Automation Does

1. Opens Chrome browser
2. Navigates to https://acme.egalvanic.ai
3. Automatically fills in the login credentials:
   - Email: rahul+acme@egalvanic.com
   - Password: RP@egalvanic123
4. Clicks the login button
5. Waits for the page to load after login
6. Looks for a site dropdown and attempts to select "test site"

## How to Run

### Python Version (Recommended)
```bash
python3 final_login.py
```

### Java Version
```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.example.LoginAutomation"
```

## Requirements

- Python 3.6+
- Java 11+ (for Java version)
- Chrome browser
- ChromeDriver (automatically managed by WebDriverManager)

## Dependencies

Python:
- selenium==4.15.0
- webdriver-manager==4.0.1

Java:
- Selenium WebDriver
- WebDriverManager

## Success

The automation successfully:
- Opens the browser and navigates to the site
- Finds and fills the email and password fields
- Submits the login form
- Waits for the page to load
- Looks for the site dropdown (though it may not be immediately visible)

The script runs to completion and automatically closes the browser after 30 seconds of displaying the results.