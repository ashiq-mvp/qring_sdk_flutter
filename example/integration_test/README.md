# Integration Tests

This directory contains integration tests for the QRing SDK Flutter plugin.

## Test Files

### 1. production_ble_manager_integration_test.dart

Comprehensive integration tests for the Production-Grade BLE Connection Manager feature (Task 23).

**Tests Requirements 10.1-10.6:**
- **23.1**: App killed → ring stays connected (Requirement 10.1)
- **23.2**: Bluetooth toggle → automatic reconnection (Requirement 10.2)
- **23.3**: Out-of-range → reconnection when back in range (Requirement 10.3)
- **23.4**: Permission revoked → graceful error handling (Requirement 10.4)
- **23.5**: Notification action → find my ring works (Requirement 10.5)
- **23.6**: Device reboot → service restarts and reconnects (Requirement 10.6)
- **23.7**: Comprehensive end-to-end workflow test

**Test Guide:** See `PRODUCTION_BLE_MANAGER_TEST_GUIDE.md` for detailed test procedures.

### 2. background_service_integration_test.dart

Integration tests for the Background Service with Persistent Notification feature.

**Test Guide:** See `BACKGROUND_SERVICE_TEST_GUIDE.md` for detailed test procedures.

### 3. plugin_integration_test.dart

Basic plugin integration tests.

## Running Integration Tests

### Prerequisites

- Physical Android device or emulator
- Physical QRing device (for connection tests)
- USB debugging enabled
- All required permissions granted

### Run All Integration Tests

```bash
cd example
flutter test integration_test/
```

### Run Specific Test File

```bash
cd example
flutter test integration_test/production_ble_manager_integration_test.dart
```

### Run on Specific Device

```bash
cd example
flutter test integration_test/production_ble_manager_integration_test.dart -d <device_id>
```

### Run Specific Test Group

```bash
cd example
flutter test integration_test/production_ble_manager_integration_test.dart --plain-name "23.1 App Killed"
```

## Important Notes

### Manual Interaction Required

Most integration tests require manual interaction due to the nature of system-level features:
- Killing the app
- Toggling Bluetooth
- Moving device out of range
- Revoking permissions
- Tapping notifications
- Rebooting device

### Test Environment

- Tests should be run on physical devices (not emulators) for best results
- Some tests require ADB access for advanced operations
- Battery optimization settings may affect test results
- Manufacturer-specific behavior may vary

### Test Duration

- Individual tests: 30-90 seconds each
- Comprehensive E2E test: 3-5 minutes
- Full test suite: 15-30 minutes (with manual interaction)

## Test Documentation

Each test file has a corresponding guide with detailed procedures:

- `PRODUCTION_BLE_MANAGER_TEST_GUIDE.md` - Production BLE Manager tests
- `BACKGROUND_SERVICE_TEST_GUIDE.md` - Background Service tests

These guides include:
- Detailed test procedures
- Expected results
- Troubleshooting tips
- ADB commands for verification
- Requirements validation checklist

## Continuous Integration

Integration tests are designed for manual execution and are not suitable for automated CI/CD pipelines due to:
- Required manual interaction
- Need for physical hardware
- System-level operations
- Long execution times

For CI/CD, use unit tests and property-based tests instead.

## Troubleshooting

### Tests Fail to Compile

```bash
cd example
flutter pub get
flutter clean
flutter pub get
```

### Device Not Found

```bash
# List connected devices
flutter devices

# Check ADB connection
adb devices
```

### Permission Errors

Ensure all permissions are granted:
- Bluetooth permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
- Location permissions (for BLE scanning)
- Notification permissions (Android 13+)

### Service Not Starting

Check:
- Bluetooth is enabled
- All permissions granted
- Foreground service permission granted
- Battery optimization disabled for the app

## Contributing

When adding new integration tests:
1. Follow the existing test structure
2. Document manual steps clearly
3. Include expected results
4. Reference specific requirements
5. Update this README
6. Create or update test guide documentation

## Support

For issues or questions:
1. Check the test guide documentation
2. Review troubleshooting sections
3. Check logcat for error messages
4. Verify device and permission setup
