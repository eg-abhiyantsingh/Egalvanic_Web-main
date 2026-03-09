#!/bin/bash

# Script to run IOSLaunchTest with proper classpath

echo "Building the project..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "Build failed. Please check for compilation errors."
    exit 1
fi

echo "Checking if Appium server is running..."
# Check if Appium server is running
if ! nc -z localhost 4723; then
    echo "Appium server is not running on port 4723."
    echo "Please start Appium server with: appium"
    echo "Or install and start Appium if not installed:"
    echo "npm install -g appium"
    echo "appium"
    exit 1
else
    echo "Appium server is running."
fi

echo "Running IOSLaunchTest..."
# Run the IOSLaunchTest class with all dependencies in classpath
mvn exec:java@ios-test

# Check the exit code
if [ $? -eq 0 ]; then
    echo "IOSLaunchTest execution completed successfully."
else
    echo "IOSLaunchTest execution failed."
    echo "Common issues and solutions:"
    echo "1. Make sure the iOS device is connected and trusted"
    echo "2. Verify the bundleId 'com.egalvanic.zplatform-qa' matches an installed app"
    echo "3. Ensure Xcode and Xcode Command Line Tools are properly installed"
    echo "4. Check that WebDriverAgent is properly signed and installed on the device"
    exit 1
fi