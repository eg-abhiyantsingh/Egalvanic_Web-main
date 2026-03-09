#!/bin/bash

echo "Starting ACME Login Automation..."

# Run the Python automation script
python3 final_login.py

echo "Automation completed."

# Try to take a screenshot (if on macOS)
if command -v screencapture &> /dev/null; then
    echo "Taking screenshot..."
    screencapture -x final_result.png
    echo "Screenshot saved as final_result.png"
else
    echo "Screenshot tool not available"
fi

echo "Process finished."