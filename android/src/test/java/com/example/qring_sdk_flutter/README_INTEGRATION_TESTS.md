# SDK-Driven BLE Scan Filtering Integration Tests

## Overview

The `SdkDrivenScanFilteringIntegrationTest.java` file contains comprehensive integration tests for the SDK-driven BLE scan filtering feature. These tests validate the complete end-to-end behavior of the scan filtering system.

## Test Coverage

The integration tests cover all requirements specified in task 9:

### Test 1: Scan shows only QRing devices (O_, Q_, R prefixes)
- **Requirements**: 1.1, 1.4, 2.1, 2.5, 8.2
- **Validates**: Devices with valid QRing name prefixes (O_, Q_, R) are accepted and emitted to Flutter

### Test 2: Non-QRing devices are filtered out
- **Requirements**: 1.1, 7.1, 7.2, 7.3, 8.5
- **Validates**: Devices without valid QRing name prefixes are rejected and not emitted to Flutter

### Test 3: Devices with no name but valid properties appear
- **Requirements**: 1.4, 1.5, 2.1, 2.5
- **Validates**: Devices with null or empty names are accepted if they pass other validation criteria

### Test 4: Same device doesn't appear multiple times (deduplication)
- **Requirements**: 5.1, 5.2, 5.3
- **Validates**: Devices are deduplicated based on MAC address, rediscovery updates existing entry

### Test 5: RSSI updates reflect signal strength changes
- **Requirements**: 5.3, 6.3
- **Validates**: Significant RSSI changes (>= 5 dBm) trigger device re-emission with updated RSSI

### Test 6: Bluetooth disabled error is shown
- **Requirements**: 10.1, 10.2
- **Validates**: Error handling at BleManager level for Bluetooth disabled state

### Test 7: Permission denied error is shown
- **Requirements**: 10.2, 10.4
- **Validates**: Error handling at BleManager level for missing permissions

### Test 8: Empty scan results handled gracefully
- **Requirements**: 10.4
- **Validates**: System handles empty results without errors (not an error condition)

### Test 9: RSSI threshold filtering
- **Requirements**: 7.3
- **Validates**: Devices with RSSI below -100 dBm are filtered out

### Test 10: Complete scan workflow
- **Requirements**: All
- **Validates**: Complete end-to-end workflow with mixed devices, filtering, deduplication, and RSSI updates

## Running the Tests

### Prerequisites

The integration tests require:
- JUnit 5 (Jupiter) test framework
- Mockito for mocking Android components
- Android SDK with Bluetooth APIs

### Running from Command Line

Due to Flutter plugin dependency issues in the standalone Android module, these tests are best run from an IDE with proper Android SDK configuration.

#### Option 1: Android Studio / IntelliJ IDEA

1. Open the `android` directory in Android Studio
2. Navigate to `src/test/java/com/example/qring_sdk_flutter/SdkDrivenScanFilteringIntegrationTest.java`
3. Right-click on the test class or individual test methods
4. Select "Run 'SdkDrivenScanFilteringIntegrationTest'"

#### Option 2: Gradle (if Flutter dependencies are resolved)

```bash
cd android
./gradlew test --tests "SdkDrivenScanFilteringIntegrationTest"
```

#### Option 3: Run all tests

```bash
cd android
./gradlew test
```

### Expected Results

All 10 integration tests should pass, demonstrating:
- ✅ QRing devices (O_, Q_, R prefixes) are accepted
- ✅ Non-QRing devices are filtered out
- ✅ Devices with no name but valid properties are accepted
- ✅ Devices are deduplicated by MAC address
- ✅ RSSI updates work correctly
- ✅ Error handling is implemented
- ✅ Empty results are handled gracefully
- ✅ RSSI threshold filtering works
- ✅ Complete workflow functions correctly

## Test Architecture

The integration tests use:

1. **Mock BluetoothDevice**: Simulates Android Bluetooth devices
2. **BleScanFilter**: The actual production filter implementation
3. **Callback Pattern**: Captures emitted devices for verification
4. **CountDownLatch**: Synchronizes asynchronous device emissions
5. **JUnit 5 Assertions**: Validates expected behavior

## Integration vs Unit Tests

These are **integration tests** because they:
- Test multiple components working together (BleScanFilter + ScannedDevice)
- Validate end-to-end workflows
- Test realistic scenarios with multiple devices
- Verify the complete filtering pipeline

They differ from **unit tests** which:
- Test individual methods in isolation
- Focus on specific edge cases
- Use more extensive mocking

They differ from **property-based tests** which:
- Generate random inputs to test universal properties
- Run hundreds of iterations with different data
- Validate mathematical properties hold for all inputs

## Troubleshooting

### Tests don't compile

Ensure you have:
- JUnit 5 (Jupiter) dependencies in build.gradle
- Mockito dependencies
- Android SDK properly configured

### Tests fail with NullPointerException

Check that:
- Mock devices are properly created with `mockDevice()` helper
- Callback is set before handling devices
- CountDownLatch is properly initialized

### Tests timeout

Increase the timeout in `await()` calls:
```java
boolean completed = deviceLatch.await(5, TimeUnit.SECONDS); // Increase from 2 to 5
```

## Contributing

When adding new integration tests:
1. Follow the existing test structure
2. Use descriptive test names with `@DisplayName`
3. Document which requirements are validated
4. Include arrange-act-assert comments
5. Use CountDownLatch for async operations
6. Verify both positive and negative cases

## Related Files

- `BleScanFilter.java` - The filter implementation being tested
- `ScannedDevice.java` - Device model used by the filter
- `BleManager.java` - Manager that uses the filter
- `BleScanFilterPropertyTest.java` - Property-based tests for the filter
- `ScannedDeviceTest.java` - Unit tests for the device model
