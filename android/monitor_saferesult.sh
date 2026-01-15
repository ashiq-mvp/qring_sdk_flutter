#!/bin/bash

# SafeResult Monitoring Script
# Monitors Android logcat for SafeResult activity and crash indicators

echo "=========================================="
echo "SafeResult Real-Time Monitoring"
echo "=========================================="
echo ""
echo "Monitoring for:"
echo "  - SafeResult duplicate reply warnings"
echo "  - Reply already submitted errors"
echo "  - IllegalStateException crashes"
echo "  - QRing SDK plugin activity"
echo ""
echo "Press Ctrl+C to stop monitoring"
echo ""
echo "=========================================="
echo ""

# Enable debug logging for SafeResult
adb shell setprop log.tag.SafeResult DEBUG 2>/dev/null

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Monitor logcat with filtering and coloring
adb logcat -v time | grep --line-buffered -E "(SafeResult|Reply already|IllegalStateException|QringSdkFlutterPlugin)" | while read line; do
    if echo "$line" | grep -q "Duplicate reply attempt"; then
        echo -e "${YELLOW}[DUPLICATE CAUGHT] $line${NC}"
    elif echo "$line" | grep -q "Reply already submitted"; then
        echo -e "${RED}[CRASH DETECTED] $line${NC}"
    elif echo "$line" | grep -q "IllegalStateException"; then
        echo -e "${RED}[EXCEPTION] $line${NC}"
    elif echo "$line" | grep -q "SafeResult"; then
        echo -e "${GREEN}[SAFERESULT] $line${NC}"
    else
        echo -e "${BLUE}[PLUGIN] $line${NC}"
    fi
done
