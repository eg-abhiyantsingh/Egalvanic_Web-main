#!/bin/bash

echo "Compiling and running EnhancedFixedDropdownTest with Maven..."

# Compile the project
mvn compile

# Run the enhanced test
mvn exec:java

echo "Test execution completed. Check test-output/reports/ for the HTML report."