#!/bin/bash

echo "Compiling EnhancedFixedDropdownTest..."
javac -cp "src/main/java:lib/*" src/main/java/EnhancedFixedDropdownTest.java

echo "Running EnhancedFixedDropdownTest..."
java -cp "src/main/java:lib/*" EnhancedFixedDropdownTest

echo "Test execution completed. Check test-output/reports/ for the HTML report."