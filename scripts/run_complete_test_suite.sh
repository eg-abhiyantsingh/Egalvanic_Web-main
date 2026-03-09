#!/bin/bash

echo "========================================="
echo "  Enhanced Fixed Dropdown Test Suite     "
echo "========================================="

# Create necessary directories
echo "Creating output directories..."
mkdir -p test-output/reports
mkdir -p test-output/screenshots

echo "Starting test execution..."

# Record start time
START_TIME=$(date +%s)

# Run the enhanced test
echo "Executing EnhancedFixedDropdownTest..."
java -cp "src/main/java:lib/*" EnhancedFixedDropdownTest

# Record end time
END_TIME=$(date +%s)

# Calculate execution time
EXECUTION_TIME=$((END_TIME - START_TIME))

echo "========================================="
echo "Test Execution Summary"
echo "========================================="
echo "Start Time: $(date -r $START_TIME)"
echo "End Time: $(date -r $END_TIME)"
echo "Total Execution Time: ${EXECUTION_TIME} seconds"
echo ""

# Check if report was generated
if [ -f "test-output/reports/EnhancedFixedDropdownTestReport.html" ]; then
    echo "✅ HTML Report Generated Successfully"
    echo "   Location: test-output/reports/EnhancedFixedDropdownTestReport.html"
else
    echo "❌ HTML Report Not Found"
fi

# Count screenshots
if [ -d "test-output/screenshots" ]; then
    SCREENSHOT_COUNT=$(ls -1 test-output/screenshots/*.png 2>/dev/null | wc -l)
    echo "📸 Screenshots Captured: $SCREENSHOT_COUNT"
else
    echo "❌ Screenshots Directory Not Found"
fi

echo ""
echo "========================================="
echo "Next Steps:"
echo "1. Open the HTML report in your browser"
echo "2. Review screenshots for visual verification"
echo "3. Analyze performance metrics"
echo "4. Address any identified bugs"
echo "========================================="

echo ""
echo "Test suite execution completed!"