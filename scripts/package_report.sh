#!/bin/bash

# Package the automation report for sharing
echo "Packaging automation report for sharing..."

# Create timestamp
timestamp=$(date +"%Y%m%d_%H%M%S")

# Create package directory
package_name="automation_report_$timestamp"
mkdir -p "$package_name"

# Copy report files
echo "Copying report files..."
cp -r test-output/reports/* "$package_name/"
cp -r test-output/screenshots "$package_name/"

# Copy documentation
echo "Copying documentation..."
cp AUTOMATION_REPORT_EMAIL_TEMPLATE.md "$package_name/"
cp MANAGER_EXPLANATION.md "$package_name/"
cp AUTOMATION_SUITE_README.md "$package_name/"

# Create a simple README
cat > "$package_name/README.md" << EOF
# ACME Automation Test Report

This package contains the complete results of our automation testing suite.

## Contents:
- AutomationReport.html: Main test report with embedded screenshots
- screenshots/: Directory containing all supporting screenshots
- AUTOMATION_REPORT_EMAIL_TEMPLATE.md: Email template for sharing results
- MANAGER_EXPLANATION.md: Technical explanation of report features
- AUTOMATION_SUITE_README.md: Overview of the automation suite

## How to Review:
1. Open AutomationReport.html in any web browser
2. All screenshots are embedded directly in the report for easy viewing
3. Navigate through the different test sections using the sidebar

## Report Sections:
1. UI Testing Results
2. API Testing Results
3. Performance Testing Results
4. Security Testing Results

Each section contains detailed logs, pass/fail indicators, and visual evidence.
EOF

# Create ZIP file
echo "Creating ZIP package..."
zip -r "${package_name}.zip" "$package_name"

# Clean up directory
rm -rf "$package_name"

echo "Package created: ${package_name}.zip"
echo "Ready for sharing with your manager!"