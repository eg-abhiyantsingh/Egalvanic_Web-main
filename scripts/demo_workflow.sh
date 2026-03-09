#!/bin/bash

echo "🚀 Starting Enhanced FixedDropdownTest Demo Workflow"
echo "=================================================="

# Step 1: Check prerequisites
echo "📋 Checking prerequisites..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 11 or higher."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "✅ Java version: $JAVA_VERSION"

MVN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
echo "✅ Maven version: $MVN_VERSION"

# Step 2: Make scripts executable
echo "🔧 Making scripts executable..."
chmod +x *.sh 2>/dev/null || echo "⚠️ Some scripts may already be executable"

# Step 3: Show available scripts
echo "📄 Available execution scripts:"
ls -la *.sh | grep -v demo_workflow.sh

# Step 4: Show project structure
echo "📂 Project structure:"
find . -name "*.java" -o -name "*.xml" -o -name "*.md" | head -10

# Step 5: Demonstrate compilation
echo "🔨 Compiling the enhanced test..."
if mvn compile; then
    echo "✅ Compilation successful"
else
    echo "❌ Compilation failed"
    exit 1
fi

# Step 6: Show what will be executed
echo "🧪 About to run EnhancedFixedDropdownTest with extensive reporting"
echo "📊 Features included:"
echo "   - Extent Reports HTML reporting"
echo "   - Performance metrics tracking"
echo "   - Assertions and bug detection"
echo "   - Screenshot capture"
echo "   - Detailed logging"

# Step 7: Provide execution options
echo "🎯 You can now run the enhanced test using any of these methods:"
echo ""
echo "   Option 1 (Recommended): ./run_complete_test_suite.sh"
echo "   Option 2 (Maven):      ./run_with_maven.sh"
echo "   Option 3 (Direct):     ./run_enhanced_test.sh"
echo ""
echo "📈 After execution, check:"
echo "   - HTML Report: test-output/reports/EnhancedFixedDropdownTestReport.html"
echo "   - Screenshots: test-output/screenshots/"
echo ""
echo "✨ The enhanced test preserves all original functionality while adding comprehensive reporting capabilities!"

echo "=================================================="
echo "🎉 Demo workflow completed. Ready to run enhanced tests!"