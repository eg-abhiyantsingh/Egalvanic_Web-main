#!/bin/bash

echo "Opening ACME Automation Reports..."

# Open the comprehensive HTML report
echo "Opening Comprehensive HTML Report..."
open comprehensive_automation_report.html

# Open the summary markdown file
echo "Opening Summary Report..."
open AUTOMATION_SUMMARY.md

# Open the existing test report
echo "Opening Existing Test Report..."
open qa-automation-suite/test-output/reports/AutomationReport.html

# Show the final result screenshot
echo "Showing Final Result Screenshot..."
open final_result.png

echo "All reports opened successfully!"
echo ""
echo "Summary of findings:"
echo "===================="
echo "✅ Login functionality working correctly"
echo "❌ Site selector dropdown not found on dashboard"
echo "⚠️ Page load time needs optimization (7.5+ seconds)"
echo ""
echo "Check the comprehensive report for detailed analysis and recommendations."