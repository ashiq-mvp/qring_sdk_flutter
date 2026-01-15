# Background Service Integration Test Guide

This guide explains how to run and verify the integration tests for the Background Service with Persistent Notification feature.

## Overview

The integration tests verify the complete workflow of the background service, including:
- Service lifecycle (start, stop, survive app kill)
- Notification management and actions
- Automatic reconnection
- Service restart after system kill
- Bluetooth state handling
- Permission handling

## Prerequisites

### Required Hardware
- Android device or emulator running Android 8.0+ (API level 26+)
- Physical QRing device (for connection tests)

### Required Software
- Flutter SDK
- Android SDK with ADB tools
- USB debugging enabled on test device

### Required Permissions
- Bluetooth permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
- Notification permissions (POST_NOTIFICATIONS on Android 13+)
- Location permissions (for BLE scanning)

## Running the Tests

### 1. Basic Test Execution

Run all integration tests:
```bash
cd example
flutter test integration_test/background_service_integration_test.dart
```

Run on a specific device:
```bash
flutter test integration_test/background_service_integration_test.dart -d <device_id>
```

### 2. Running Individual Test Groups

Run only service lifecycle tests:
```bash
flutter test integration_test/background_service_integration_test.dart --plain-name "14.1 Service Lifecycle"
```

Run only Find My Ring tests:
```bash
flutter test integration_test/background_service_integration_test.dart --plain-name "14.2 Find My Ring"
```

## Test Details

### Test 14.1: Service Lifecycle

**Automated Tests:**
- ✅ `should start service and show notification` - Verifies service starts and isServiceRunning returns true

**Manual Verification Required:**
- `should keep service running when app is killed` - Requires manually killing the app
- `should maintain notification visibility` - Requires visual verification of notification

**Steps for Manual Tests:**
1. Run the test
2. When prompted, kill the Flutter app (swipe away from recent apps)
3. Check that the notification remains visible in the notification bar
4. Verify service is still running via system settings or ADB:
   ```bash
   adb shell dumpsys activity services | grep QRingBackgroundService
   ```
5. Reopen the app and stop the service

**Expected Results:**
- Notification appears when service starts
- Notification remains visible after app is killed
- Service continues running independently

### Test 14.2: Find My Ring from Notification

**Requirements:**
- Physical QRing device
- Device must be connected

**Steps:**
1. Run the test
2. Wait for device discovery and connection
3. Tap the "Find My Ring" button in the notification
4. Verify the ring vibrates/emits sound
5. Check notification shows "Ring activated" feedback

**Expected Results:**
- "Find My Ring" button appears in notification when connected
- Ring responds to the command
- Notification updates with feedback

### Test 14.3: Automatic Reconnection

**Requirements:**
- Physical QRing device
- Ability to power cycle the device

**Steps:**
1. Run the test
2. Wait for device connection
3. Turn off the ring (or move it out of range)
4. Observe notification shows "Reconnecting..." status
5. Check logcat for reconnection attempts:
   ```bash
   adb logcat | grep QRingBackgroundService
   ```
6. Turn on the ring (or move it back in range)
7. Verify reconnection succeeds
8. Check notification shows "Connected" status

**Expected Results:**
- Service detects disconnection
- Reconnection attempts start automatically
- Notification updates to show reconnection status
- Service reconnects when device is available

### Test 14.4: Service Restart After System Kill

**Requirements:**
- ADB access

**Steps:**
1. Run the test to start the service
2. Force stop the app using ADB:
   ```bash
   adb shell am force-stop com.example.qring_sdk_flutter_example
   ```
3. Wait 5-10 seconds
4. Check if service restarts automatically:
   ```bash
   adb shell dumpsys activity services | grep QRingBackgroundService
   ```
5. Verify notification reappears
6. Check logcat for service restart logs:
   ```bash
   adb logcat | grep "QRingBackgroundService: onCreate"
   ```

**Expected Results:**
- Service restarts automatically after force stop
- Notification reappears
- Service attempts to reconnect to saved device

### Test 14.5: Notification Tap Behavior

**Steps (App Killed):**
1. Start the service
2. Kill the Flutter app (swipe away from recent apps)
3. Tap the notification
4. Verify app launches and shows main screen

**Steps (App Running):**
1. Start the service
2. Put app in background (press home button)
3. Tap the notification
4. Verify app comes to foreground

**Expected Results:**
- Tapping notification launches app when killed
- Tapping notification brings app to foreground when running

### Test 14.6: Bluetooth State Changes

**Requirements:**
- Physical QRing device
- Device must be connected

**Steps:**
1. Run the test
2. Wait for device connection
3. Disable Bluetooth via system settings
4. Check logcat to verify reconnection attempts pause:
   ```bash
   adb logcat | grep "Bluetooth disabled"
   ```
5. Enable Bluetooth
6. Verify reconnection resumes immediately
7. Check device reconnects

**Expected Results:**
- Reconnection pauses when Bluetooth is disabled
- Reconnection resumes immediately when Bluetooth is enabled
- Device reconnects successfully

### Test 14.7: Permission Handling

**Steps:**
1. Go to app settings on the device
2. Revoke Bluetooth permissions (BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
3. Run the test to attempt starting the service
4. Verify error notification appears
5. Verify service stops
6. Check logcat for permission error logs:
   ```bash
   adb logcat | grep "Permission"
   ```

**Expected Results:**
- Service fails to start when permissions are missing
- Error notification is displayed
- Service stops gracefully

## Monitoring and Debugging

### View Service Status
```bash
adb shell dumpsys activity services | grep QRingBackgroundService
```

### View Notifications
```bash
adb shell dumpsys notification
```

### View Logcat (Filtered)
```bash
adb logcat | grep QRingBackgroundService
```

### View All Logs
```bash
adb logcat *:E
```

### Check Running Processes
```bash
adb shell ps | grep qring
```

## Common Issues

### Service Doesn't Start
- Check permissions are granted
- Verify Bluetooth is enabled
- Check logcat for error messages

### Notification Doesn't Appear
- Verify notification permissions are granted (Android 13+)
- Check notification channel is created
- Verify foreground service is running

### Reconnection Doesn't Work
- Check device is in range
- Verify Bluetooth is enabled
- Check logcat for reconnection attempts

### Service Doesn't Restart After Kill
- Verify START_STICKY is set in onStartCommand
- Check device battery optimization settings
- Some manufacturers may prevent service restart

## Test Results Checklist

Use this checklist to track test results:

- [ ] 14.1.1: Service starts and shows notification
- [ ] 14.1.2: Service survives app kill
- [ ] 14.1.3: Notification remains visible
- [ ] 14.2: Find My Ring works from notification
- [ ] 14.3: Automatic reconnection works
- [ ] 14.4: Service restarts after system kill
- [ ] 14.5.1: Notification tap launches app (killed)
- [ ] 14.5.2: Notification tap brings app to foreground (running)
- [ ] 14.6: Bluetooth state changes handled correctly
- [ ] 14.7: Permission errors handled gracefully

## Notes

- Most tests require manual verification due to the nature of system-level features
- Tests marked with `skip: true` require manual execution
- Always test on multiple Android versions (8.0, 10, 12, 13+)
- Test on different device manufacturers (Samsung, Google, etc.) as behavior may vary
- Battery optimization settings can affect service behavior

## Requirements Validation

Each test validates specific requirements from the design document:

- **Test 14.1**: Requirements 1.3, 3.2 (Service lifecycle and notification)
- **Test 14.2**: Requirements 4.2, 4.3, 4.4 (Find My Ring action)
- **Test 14.3**: Requirements 7.1, 7.2, 7.3 (Automatic reconnection)
- **Test 14.4**: Requirements 1.4, 1.5, 10.4 (Service restart)
- **Test 14.5**: Requirements 5.1, 5.2, 5.3 (Notification tap behavior)
- **Test 14.6**: Requirements 7.4, 7.5 (Bluetooth state handling)
- **Test 14.7**: Requirements 8.5 (Permission handling)
