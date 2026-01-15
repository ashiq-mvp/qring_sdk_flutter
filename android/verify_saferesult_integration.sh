#!/bin/bash

# SafeResult Integration Verification Script
# This script verifies that SafeResult is properly integrated in all required methods

echo "=========================================="
echo "SafeResult Integration Verification"
echo "=========================================="
echo ""

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter for results
PASS=0
FAIL=0
WARN=0

# Check if SafeResult.java exists
echo "1. Checking SafeResult class exists..."
if [ -f "src/main/java/com/example/qring_sdk_flutter/SafeResult.java" ]; then
    echo -e "${GREEN}✓ SafeResult.java found${NC}"
    ((PASS++))
else
    echo -e "${RED}✗ SafeResult.java not found${NC}"
    ((FAIL++))
fi
echo ""

# Check SafeResult usage in QringSdkFlutterPlugin
echo "2. Checking SafeResult usage in QringSdkFlutterPlugin..."
PLUGIN_FILE="src/main/java/com/example/qring_sdk_flutter/QringSdkFlutterPlugin.java"

if [ ! -f "$PLUGIN_FILE" ]; then
    echo -e "${RED}✗ QringSdkFlutterPlugin.java not found${NC}"
    ((FAIL++))
else
    # Count SafeResult instantiations
    SAFERESULT_COUNT=$(grep -c "new SafeResult(" "$PLUGIN_FILE")
    echo -e "${GREEN}✓ Found $SAFERESULT_COUNT SafeResult instantiations${NC}"
    ((PASS++))
fi
echo ""

# Check specific methods are wrapped
echo "3. Verifying critical methods use SafeResult..."

METHODS=(
    "deviceInfo:Device Info (Primary Fix)"
    "battery:Battery Level"
    "connect:Connection"
    "findRing:Find Ring"
    "syncStepData:Step Data Sync"
    "syncHeartRateData:Heart Rate Sync"
    "syncSleepData:Sleep Data Sync"
    "syncBloodOxygenData:Blood Oxygen Sync"
    "syncBloodPressureData:Blood Pressure Sync"
    "setContinuousHeartRate:Continuous Heart Rate Settings"
    "setContinuousBloodOxygen:Continuous Blood Oxygen Settings"
    "setContinuousBloodPressure:Continuous Blood Pressure Settings"
    "setDisplaySettings:Display Settings"
    "setUserInfo:User Info"
    "startExercise:Start Exercise"
    "stopExercise:Stop Exercise"
    "validateFirmwareFile:Firmware Validation"
    "startFirmwareUpdate:Firmware Update"
)

for method_info in "${METHODS[@]}"; do
    IFS=':' read -r method_name description <<< "$method_info"
    
    # Check if method uses SafeResult
    if grep -q "SafeResult.*new SafeResult(result, \"$method_name\")" "$PLUGIN_FILE"; then
        echo -e "${GREEN}✓ $description ($method_name)${NC}"
        ((PASS++))
    else
        echo -e "${RED}✗ $description ($method_name) - SafeResult not found${NC}"
        ((FAIL++))
    fi
done
echo ""

# Check for any remaining raw result.success/error/notImplemented calls in async contexts
echo "4. Checking for potential unprotected async result calls..."
echo -e "${YELLOW}Note: Some synchronous methods may legitimately use raw result calls${NC}"

# Look for result.success/error calls that might be in callbacks
UNPROTECTED=$(grep -n "result\\.success\\|result\\.error\\|result\\.notImplemented" "$PLUGIN_FILE" | grep -v "SafeResult" | grep -v "//")

if [ -z "$UNPROTECTED" ]; then
    echo -e "${GREEN}✓ No obvious unprotected result calls found${NC}"
    ((PASS++))
else
    echo -e "${YELLOW}⚠ Found potential unprotected result calls (review manually):${NC}"
    echo "$UNPROTECTED"
    ((WARN++))
fi
echo ""

# Check SafeResult implementation details
echo "5. Verifying SafeResult implementation..."
SAFERESULT_FILE="src/main/java/com/example/qring_sdk_flutter/SafeResult.java"

if [ -f "$SAFERESULT_FILE" ]; then
    # Check for AtomicBoolean
    if grep -q "AtomicBoolean" "$SAFERESULT_FILE"; then
        echo -e "${GREEN}✓ Uses AtomicBoolean for thread safety${NC}"
        ((PASS++))
    else
        echo -e "${RED}✗ AtomicBoolean not found${NC}"
        ((FAIL++))
    fi
    
    # Check for logging
    if grep -q "Log.w" "$SAFERESULT_FILE"; then
        echo -e "${GREEN}✓ Includes duplicate attempt logging${NC}"
        ((PASS++))
    else
        echo -e "${RED}✗ Logging not found${NC}"
        ((FAIL++))
    fi
    
    # Check for compareAndSet
    if grep -q "compareAndSet" "$SAFERESULT_FILE"; then
        echo -e "${GREEN}✓ Uses compareAndSet for atomic reply tracking${NC}"
        ((PASS++))
    else
        echo -e "${RED}✗ compareAndSet not found${NC}"
        ((FAIL++))
    fi
else
    echo -e "${RED}✗ SafeResult.java not found${NC}"
    ((FAIL++))
fi
echo ""

# Summary
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
echo -e "${GREEN}Passed: $PASS${NC}"
echo -e "${RED}Failed: $FAIL${NC}"
echo -e "${YELLOW}Warnings: $WARN${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ All critical checks passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Build the Android app: cd example && flutter build apk"
    echo "2. Run on device: flutter run"
    echo "3. Follow the integration test guide in INTEGRATION_TEST_GUIDE.md"
    echo "4. Monitor logcat: adb logcat | grep -E '(SafeResult|Reply already)'"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Please review the output above.${NC}"
    exit 1
fi
