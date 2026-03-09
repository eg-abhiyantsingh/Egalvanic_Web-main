# iOS Launch Test

This directory contains a simple iOS launch test that demonstrates how to use Appium to launch an iOS application.

## Prerequisites

1. Java 11 or higher
2. Maven
3. Appium Server
4. Xcode (for iOS)
5. Connected iOS device or iOS Simulator

## Setup

1. Start Appium server:
   ```
   appium
   ```

2. Make sure your iOS device is connected and trusted, or start an iOS simulator.

3. Update the device UDID and other capabilities in `IOSLaunchTest.java` if needed:
   - `MobileCapabilityType.UDID`: Your device UDID
   - `platformVersion`: Your iOS version
   - `xcodeOrgId`: Your Apple Developer Team ID
   - `xcodeSigningId`: Your signing identity
   - `updatedWDABundleId`: Your WebDriverAgent bundle ID

## Running the Test

You can run the test in several ways:

### Option 1: Using the Batch Script (Windows)
```
run-ios-test.bat
```

### Option 2: Using the PowerShell Script (Windows)
```
.\run-ios-test.ps1
```

### Option 3: Manual Compilation and Execution
1. Compile the qa-automation-suite:
   ```
   cd qa-automation-suite
   mvn compile
   cd ..
   ```

2. Compile and run the IOSLaunchTest:
   ```
   javac -cp "src/main/java;qa-automation-suite/target/classes" src/main/java/IOSLaunchTest.java
   java -cp "src/main/java;qa-automation-suite/target/classes" IOSLaunchTest
   ```

## Expected Output

If everything is set up correctly, you should see:
```
App launched successfully.
```

The app will stay open for 5 seconds before closing.

## Troubleshooting

1. **Appium server not running**: Make sure Appium is running on port 4723
2. **Device not found**: Check that your device is connected and trusted
3. **Signing issues**: Verify your Xcode signing credentials
4. **WebDriverAgent issues**: Make sure WebDriverAgent is properly signed and installed on your device