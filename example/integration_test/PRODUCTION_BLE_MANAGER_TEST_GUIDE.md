# Production BLE Manager Integration Test Guide

This guide explains how to run and verify the integration tests for the Production-Grade BLE Connection Manager feature.

## Overview

The integration tests verify the complete production BLE manager functionality, including:
- App killed → ring stays connected (Requirement 10.1)
- Bluetooth toggle → automatic reconnection (Requirement 10.2)
- Out-of-range → reconnection when back in range (Requirement 10.3)
- Permission revoked → graceful error handling (Requirement 10.4)
- Notification action → find my ring works (Requirement 10.5)
- Device reboot → service restarts and reconnects (Requirement 10.6)

## Prerequisites

### Required Hardware
- Android device running Android 12+ (API level 31+) for full feature testing
- Android device running Android 8.0+ (API level 26+) for basic testing
- Physical QRing device (charged and ready to pair)

### Required Software
- Flutter SDK
- Android SDK with ADB tools
- USB debugging enabled on test device

### Required Permissions
- Bluetooth permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT on Android 12+)
- Location permissions (ACCESS_FINE_LOCATION on Android < 12)
- Notification permissions (POST_NOTIFICATIONS on Android 13+)
- Foreground service permissions

## Running the Tests

### 1. Basic Test Execution

Run all integration tests:
```bash
cd example
flutter test integration_test/production_ble_manager_integration_test.dart
```

Run on a specific device:
```bash
flutter test integration_test/production_ble_manager_integration_test.dart -d <device_id>
```

### 2. Running Individual Test Groups

Run only app kill tests:
```bash
flutter test integration_test/production_ble_manager_integration_test.dart --plain-name "23.1 App Killed"
```

Run only Bluetooth toggle tests:
```bash
flutter test integration_test/production_ble_manager_integration_test.dart --plain-name "23.2 Bluetooth Toggle"
```

Run only out-of-range tests:
```bash
flutter test integration_test/production_ble_manager_integration_test.dart --plain-name "23.3 Out-of-Range"
```

Run only permission tests:
```bash
flutter test integration_test/production_ble_manager_integration_test.dart --plain-name "23.4 Permission Revoked"
```

Run only notification action tests:
```bash
flutter test integration_test/production_ble_manager_integration_test.dart --plain-name "23.5 Notification Action"
```

Run only device reboot tests:
```bash
flutter test integration_test/production_ble_manager_integration_test.dart --plain-name "23.6 Device Reboot"
```

## Test Details

### Test 23.1: App Killed → Ring Stays Connected

**Requirement:** 10.1 - WHEN the app is killed by the system, THE Foreground_Service SHALL maintain QRing connection

**Automated Tests:**
- Discovers and connects to QRing device
- Starts background service
- Verifies service is running

**Manual Verification Required:**
1. Run the test
2. When prompted, kill the Flutter app (swipe away from recent apps)
3. Wait 10 seconds
4. Check that the notification remains visible in the notification bar
5. Verify notification shows "Connected" status
6. Reopen the app
7. Verify service is still running
8. Verify connection is maintained

**ADB Verification:**
```bash
# Check if service is running after app kill
adb shell dumpsys activity services | grep QRingBackgroundService

# Check if process is still alive
adb shell ps | grep qring
```

**Expected Results:**
- ✅ Notification remains visible after app kill
- ✅ Service continues running independently
- ✅ Connection is maintained
- ✅ Service survives force stop (START_STICKY)

### Test 23.2: Bluetooth Toggle → Automatic Reconnection

**Requirement:** 10.2 - WHEN Bluetooth is toggled off then on, THE BLE_Manager SHALL automatically reconnect to the QRing

**Automated Tests:**
- Connects to device
- Starts background service
- Monitors connection state changes

**Manual Verification Required:**
1. Run the test
2. Disable Bluetooth via system settings
3. Wait 5 seconds
4. Verify notification shows "Disconnected" or "Reconnecting"
5. Enable Bluetooth
6. Wait 10 seconds
7. Verify notification shows "Connected"
8. Verify device reconnects automatically

**Logcat Monitoring:**
```bash
# Monitor reconnection behavior
adb logcat | grep -E "(BleConnectionManager|AutoReconnect|Bluetooth)"

# Look for these log messages:
# - "Bluetooth disabled, pausing reconnection"
# - "Bluetooth enabled, resuming reconnection"
# - "Reconnection attempt #N"
# - "Connection successful"
```

**Expected Results:**
- ✅ Reconnection pauses when Bluetooth is disabled
- ✅ Reconnection resumes immediately when Bluetooth is enabled
- ✅ Device reconnects successfully
- ✅ Notification updates to reflect connection state

### Test 23.3: Out-of-Range → Reconnection When Back in Range

**Requirement:** 10.3 - WHEN the QRing goes out of range then returns, THE BLE_Manager SHALL automatically reconnect

**Automated Tests:**
- Connects to device
- Starts background service
- Monitors connection state changes

**Manual Verification Required:**
1. Run the test
2. Move the QRing device far away (out of Bluetooth range) OR turn off the ring
3. Wait 10 seconds
4. Verify notification shows "Reconnecting..."
5. Bring the ring back in range OR turn it back on
6. Wait 20 seconds for reconnection
7. Verify notification shows "Connected"

**Exponential Backoff Verification:**
```bash
# Monitor reconnection attempts and delays
adb logcat | grep -E "(AutoReconnect|backoff|delay)"

# Expected delay progression:
# Attempts 1-5: ~10 seconds (±20% jitter)
# Attempts 6-10: ~30 seconds (±20% jitter)
# Attempts 11+: ~60s, ~120s, ~240s... (max 5 minutes)
```

**Expected Results:**
- ✅ Service detects disconnection
- ✅ Enters RECONNECTING state
- ✅ Uses exponential backoff strategy
- ✅ Reconnects when device returns to range
- ✅ Notification updates to show reconnection status

### Test 23.4: Permission Revoked → Graceful Error Handling

**Requirement:** 10.4 - WHEN BLE permissions are revoked while connected, THE BLE_Manager SHALL handle gracefully and report error

**Automated Tests:**
- Connects to device
- Starts background service
- Monitors error stream

**Manual Verification Required:**
1. Run the test
2. Go to Settings > Apps > QRing SDK Flutter Example
3. Go to Permissions
4. Revoke "Nearby devices" or "Bluetooth" permission
5. Wait 5 seconds
6. Verify notification shows error message
7. Verify app shows permission error
8. Re-grant the permission
9. Verify service recovers

**Logcat Monitoring:**
```bash
# Monitor permission errors
adb logcat | grep -E "(Permission|SecurityException|BleError)"

# Look for:
# - Specific permission error details
# - Graceful error handling (no crash)
# - Error reporting to Flutter
```

**Expected Results:**
- ✅ Permission error is detected
- ✅ Specific error details are reported
- ✅ Service handles gracefully (no crash)
- ✅ Error is reported to Flutter via errorStream
- ✅ Service remains running
- ✅ Service recovers when permission is re-granted

### Test 23.5: Notification Action → Find My Ring Works

**Requirement:** 10.5 - WHEN the notification Find My Ring action is tapped, THE BLE_Manager SHALL trigger ring vibration

**Automated Tests:**
- Connects to device
- Starts background service
- Verifies connection

**Manual Verification Required:**

**Test 1: Find My Ring with Connected Device**
1. Run the test
2. Check the notification in the notification bar
3. Verify "Find My Ring" button is visible
4. Tap the "Find My Ring" button
5. Verify the ring vibrates/beeps
6. Verify notification shows "Ring activated" feedback
7. Wait 3 seconds
8. Verify notification returns to normal state

**Test 2: Find My Ring with Disconnected Device**
1. Start service without connecting to a device
2. Tap the "Find My Ring" button
3. Verify notification shows "Ring not connected" error
4. Wait 3 seconds
5. Verify notification returns to normal state

**Expected Results:**
- ✅ "Find My Ring" button appears in notification when connected
- ✅ Ring responds to the command (vibrates/beeps)
- ✅ Notification shows success feedback
- ✅ Notification shows error feedback when not connected
- ✅ Feedback notification disappears after 3 seconds
- ✅ Connection is maintained after command

### Test 23.6: Device Reboot → Service Restarts and Reconnects

**Requirement:** 10.6 - WHEN the device reboots with a previously connected ring, THE Foreground_Service SHALL restart and reconnect

**Manual Verification Required:**
1. Connect to QRing device
2. Start background service
3. Verify connection is established
4. Note the device MAC address
5. Reboot the Android device
6. After reboot, unlock the device
7. Wait 30 seconds
8. Check notification bar for QRing notification
9. Verify notification shows "Reconnecting..." or "Connected"
10. Open the app to verify service restarted
11. Verify device reconnects automatically

**Configuration Verification:**
```bash
# Verify BootReceiver is registered
adb shell dumpsys package com.example.qring_sdk_flutter_example | grep -A 5 BootReceiver

# Verify BluetoothReceiver is registered
adb shell dumpsys package com.example.qring_sdk_flutter_example | grep -A 5 BluetoothReceiver

# Check AndroidManifest.xml for:
# - RECEIVE_BOOT_COMPLETED permission
# - BootReceiver with BOOT_COMPLETED intent filter
# - BluetoothReceiver with STATE_CHANGED intent filter
```

**Expected Results:**
- ✅ Service restarts automatically after reboot
- ✅ Notification appears automatically
- ✅ Service attempts to reconnect to saved device
- ✅ Device reconnects successfully
- ✅ BootReceiver is properly configured
- ✅ BluetoothReceiver is properly configured

### Test 23.7: Comprehensive E2E Test

**Purpose:** Combines multiple scenarios to verify the complete production workflow

**Test Sequence:**
1. Connect to device and start service
2. Test Find My Ring from notification
3. Test app kill survival
4. Test Bluetooth toggle reconnection
5. Test out-of-range reconnection
6. Verify final connection state

**Expected Results:**
- ✅ All scenarios work in sequence
- ✅ Service remains stable throughout
- ✅ Connection is maintained or recovered
- ✅ No crashes or unexpected errors

## Monitoring and Debugging

### View Service Status
```bash
adb shell dumpsys activity services | grep QRingBackgroundService
```

### View Connection State
```bash
adb logcat | grep "BleConnectionManager: State"
```

### View Reconnection Attempts
```bash
adb logcat | grep "AutoReconnect"
```

### View All BLE Manager Logs
```bash
adb logcat | grep -E "(BleConnectionManager|PairingManager|GattConnectionManager|AutoReconnect)"
```

### View Notification Updates
```bash
adb logcat | grep "ServiceNotificationManager"
```

### View Error Logs
```bash
adb logcat *:E | grep qring
```

## Common Issues

### Service Doesn't Start
- Check all permissions are granted
- Verify Bluetooth is enabled
- Check logcat for error messages
- Verify foreground service permission

### Notification Doesn't Appear
- Verify notification permissions are granted (Android 13+)
- Check notification channel is created
- Verify foreground service is running
- Check notification settings for the app

### Reconnection Doesn't Work
- Check device is in range
- Verify Bluetooth is enabled
- Check logcat for reconnection attempts
- Verify exponential backoff is working

### Service Doesn't Restart After Kill
- Verify START_STICKY is set in onStartCommand
- Check device battery optimization settings
- Some manufacturers may prevent service restart
- Try on different device/manufacturer

### Permission Errors Not Reported
- Verify error stream is being monitored
- Check logcat for SecurityException
- Verify permission checking is implemented
- Check error reporting to Flutter

## Test Results Checklist

Use this checklist to track test results:

### Test 23.1: App Killed → Ring Stays Connected
- [ ] Service survives app kill
- [ ] Notification remains visible
- [ ] Connection is maintained
- [ ] Service survives force stop

### Test 23.2: Bluetooth Toggle → Automatic Reconnection
- [ ] Reconnection pauses when Bluetooth off
- [ ] Reconnection resumes when Bluetooth on
- [ ] Device reconnects successfully
- [ ] Notification updates correctly

### Test 23.3: Out-of-Range → Reconnection
- [ ] Service detects disconnection
- [ ] Enters RECONNECTING state
- [ ] Uses exponential backoff
- [ ] Reconnects when back in range

### Test 23.4: Permission Revoked → Graceful Error
- [ ] Permission error detected
- [ ] Specific error details reported
- [ ] Service handles gracefully (no crash)
- [ ] Error reported to Flutter
- [ ] Service recovers when permission granted

### Test 23.5: Notification Action → Find My Ring
- [ ] "Find My Ring" button appears
- [ ] Ring responds to command
- [ ] Success feedback shown
- [ ] Error feedback shown when not connected
- [ ] Connection maintained after command

### Test 23.6: Device Reboot → Service Restarts
- [ ] Service restarts after reboot
- [ ] Notification appears automatically
- [ ] Device reconnects automatically
- [ ] BootReceiver configured correctly
- [ ] BluetoothReceiver configured correctly

### Test 23.7: Comprehensive E2E
- [ ] All scenarios work in sequence
- [ ] Service remains stable
- [ ] Connection maintained/recovered
- [ ] No crashes or unexpected errors

## Requirements Validation

Each test validates specific requirements from the design document:

- **Test 23.1**: Requirement 10.1 (App killed → ring stays connected)
- **Test 23.2**: Requirement 10.2 (Bluetooth toggle → automatic reconnection)
- **Test 23.3**: Requirement 10.3 (Out-of-range → reconnection when back in range)
- **Test 23.4**: Requirement 10.4 (Permission revoked → graceful error handling)
- **Test 23.5**: Requirement 10.5 (Notification action → find my ring works)
- **Test 23.6**: Requirement 10.6 (Device reboot → service restarts and reconnects)

## Notes

- Most tests require manual verification due to the nature of system-level features
- Tests marked with `skip: true` require manual execution
- Always test on multiple Android versions (8.0, 10, 12, 13+)
- Test on different device manufacturers (Samsung, Google, Xiaomi, etc.) as behavior may vary
- Battery optimization settings can affect service behavior
- Some manufacturers have aggressive battery optimization that may prevent service restart
- Root access is NOT required for these tests

## Reporting Results

After completing all tests, document:
1. Which scenarios were tested
2. Any crashes or issues encountered
3. Logcat excerpts showing key events
4. Overall stability assessment
5. Any unexpected behavior
6. Device manufacturer and Android version
7. Any manufacturer-specific issues

## Troubleshooting

### If Service Doesn't Survive App Kill:
1. Check START_STICKY is returned from onStartCommand
2. Verify foreground notification is shown
3. Check device battery optimization settings
4. Try on different device/manufacturer

### If Reconnection Doesn't Work:
1. Check Bluetooth is enabled
2. Verify device is in range
3. Check logcat for reconnection attempts
4. Verify exponential backoff is working
5. Check device MAC is persisted correctly

### If Permissions Aren't Handled:
1. Verify SecurityException catching is implemented
2. Check error reporting to Flutter
3. Verify permission checking before operations
4. Check logcat for permission errors

### If Notification Actions Don't Work:
1. Verify PendingIntent is created correctly
2. Check notification action is registered
3. Verify service handles ACTION_FIND_MY_RING intent
4. Check logcat for intent handling

### If Service Doesn't Restart After Reboot:
1. Verify RECEIVE_BOOT_COMPLETED permission
2. Check BootReceiver is registered in manifest
3. Verify BootReceiver is enabled
4. Check device MAC is persisted
5. Check logcat after reboot for BootReceiver logs

## Success Criteria

### ✅ All Tests Pass If:
1. Service survives app kill and maintains connection
2. Bluetooth toggle triggers automatic reconnection
3. Out-of-range reconnection works with exponential backoff
4. Permission revocation is handled gracefully
5. Find My Ring works from notification
6. Service restarts after device reboot
7. All functionality works as designed
8. No crashes or unexpected errors
9. Notification updates correctly
10. Error reporting works correctly

### ❌ Tests Fail If:
1. Service is killed when app is killed
2. Reconnection doesn't work after Bluetooth toggle
3. Out-of-range reconnection fails
4. Permission revocation causes crash
5. Find My Ring doesn't work from notification
6. Service doesn't restart after reboot
7. Any crashes occur
8. Notification doesn't update
9. Errors aren't reported correctly
10. Connection is lost permanently

## Additional Resources

- [Android Foreground Services Documentation](https://developer.android.com/guide/components/foreground-services)
- [Android Bluetooth Permissions](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions)
- [Android BroadcastReceiver](https://developer.android.com/guide/components/broadcasts)
- [Android Notifications](https://developer.android.com/develop/ui/views/notifications)
- [ADB Commands Reference](https://developer.android.com/studio/command-line/adb)
