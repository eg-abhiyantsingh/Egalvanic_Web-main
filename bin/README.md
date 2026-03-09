# Selenium Automation Project

This project automates the login process for https://acme.egalvanic.ai using Selenium WebDriver with Java.

## Prerequisites

- Java 11 or higher
- Maven
- Chrome browser

## Dependencies

- Selenium WebDriver
- WebDriverManager

## Project Structure

```
.
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── LoginAutomation.java
```

## How to Run

1. Compile the project:
   ```
   mvn compile
   ```

2. Run the automation:
   ```
   mvn exec:java -Dexec.mainClass="com.example.LoginAutomation"
   ```

## What the Script Does

1. Opens Chrome browser
2. Navigates to https://acme.egalvanic.ai
3. Enters email: rahul+acme@egalvanic.com
4. Enters password: RP@egalvanic123
5. Clicks the login button
6. Waits for the page to load
7. Looks for a site dropdown and selects "test site"

## Customization

You can modify the following values in the [LoginAutomation.java](file:///Users/vishwa/Downloads/Scupltsoft/Selenium_automation_qoder/src/main/java/com/example/LoginAutomation.java#L17-L82) file:
- Email address (line 32)
- Password (line 37)
- Target URL (line 29)