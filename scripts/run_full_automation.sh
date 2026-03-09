#!/bin/bash

echo "🚀 Starting Full Automation Suite..."
echo "====================================="

# Compile the project
echo "🔧 Compiling the project..."
mvn compile

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
else
    echo "❌ Compilation failed"
    exit 1
fi

# Run the automation
echo "🏃 Running the automation suite..."
mvn exec:java

if [ $? -eq 0 ]; then
    echo "✅ Automation completed successfully"
else
    echo "❌ Automation failed"
    exit 1
fi

# Package report for sharing
./package_report.sh

echo "====================================="
echo "📋 Report generated at: test-output/reports/AutomationReport.html"
echo "📸 Screenshots saved at: test-output/screenshots/"
echo "📦 Packaged report created for sharing"
echo "🎉 Full automation suite completed!"