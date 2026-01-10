#!/bin/bash
# MT5 Stock Decision System Test Script

echo "========================================="
echo "MT5 Stock Decision System - Test Suite"
echo "========================================="

echo -e "\n[1/4] Building project..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo -e "\n[2/4] Running Unit Tests..."
mvn test

echo -e "\n[3/4] Running Integration Test..."
mvn exec:java -Dexec.mainClass="com.mt5decision.test.IntegrationTest"

echo -e "\n[4/4] Running Complete Test Suite..."
mvn exec:java -Dexec.mainClass="com.mt5decision.TestRunner"

echo -e "\n========================================="
echo "Test completed!"
echo "Check target/surefire-reports for detailed test results"
echo "========================================="