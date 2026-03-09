#!/bin/bash

echo "🚀 Running Consolidated Cross-Browser Tests..."
echo "=========================================="

# Make sure the scripts are executable
chmod +x run_cross_browser_tests.sh
chmod +x consolidate_reports.sh

# Run cross-browser tests
echo "🔧 Step 1: Running cross-browser tests..."
./run_cross_browser_tests.sh

echo ""
echo "📊 Step 2: Consolidating reports..."
./consolidate_reports.sh

echo ""
echo "🎉 All processes completed!"
echo "📄 Consolidated report available at: test-output/reports/ConsolidatedAutomationReport.html"