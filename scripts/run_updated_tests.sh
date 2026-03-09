#!/bin/bash

echo "Running Updated Dropdown Selection Tests"
echo "======================================"

echo "1. Compiling Java test..."
javac -cp ".:selenium-java-4.15.0.jar:qa-automation-suite/target/classes:bin/selenium-java-4.15.0.jar" src/main/java/UpdatedDropdownTest.java

if [ $? -eq 0 ]; then
    echo "✅ Java compilation successful"
else
    echo "❌ Java compilation failed"
    exit 1
fi

echo ""
echo "2. Running Java test..."
java -cp ".:selenium-java-4.15.0.jar:qa-automation-suite/target/classes:bin/selenium-java-4.15.0.jar" UpdatedDropdownTest

echo ""
echo "3. Running Python test..."
python3 updated_final_login.py

echo ""
echo "All tests completed!"
echo "Check dropdown_test_screenshots/ directory for screenshots"