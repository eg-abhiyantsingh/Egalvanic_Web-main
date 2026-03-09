#!/bin/bash

# Run the Python automation script in the background
echo "Starting automation script..."
python3 simple_login_automation.py &

# Wait for Chrome to start (adjust timing as needed)
sleep 15

# Take a screenshot (macOS)
echo "Taking screenshot..."
screencapture -x automation_screenshot.png

echo "Screenshot saved as automation_screenshot.png"

# Wait for the automation to complete
wait

echo "Automation completed."