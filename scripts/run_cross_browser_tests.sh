#!/bin/bash

echo "🚀 Running Cross-Browser Tests..."
echo "================================"

# Create output directories
mkdir -p test-output/reports
mkdir -p test-output/screenshots

# Array of browsers to test (removed Edge)
browsers=("chrome" "firefox" "safari")

# Run tests for each browser
for browser in "${browsers[@]}"; do
    echo "🔧 Running tests on $browser..."
    
    # Run the Egalvanic test with the specific browser
    mvn exec:java -Dexec.mainClass="Egalvanic" -Dexec.args="$browser"
    
    # Rename the report to include browser name
    if [ -f "test-output/reports/AutomationReport.html" ]; then
        mv "test-output/reports/AutomationReport.html" "test-output/reports/AutomationReport_${browser}.html"
        echo "✅ Report generated for $browser"
    else
        echo "❌ No report found for $browser"
    fi
    
    echo ""
done

# Generate consolidated report
echo "📊 Generating consolidated report..."
mvn compile exec:java -Dexec.mainClass="ConsolidatedReportGenerator"

echo "🎉 All cross-browser tests completed!"
echo ""
echo "📊 Individual reports generated:"
for browser in "${browsers[@]}"; do
    if [ -f "test-output/reports/AutomationReport_${browser}.html" ]; then
        echo "   - test-output/reports/AutomationReport_${browser}.html"
    fi
done

echo ""
echo "📋 Consolidated report generated at: test-output/reports/ConsolidatedAutomationReport.html"