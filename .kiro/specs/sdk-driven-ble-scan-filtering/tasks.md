# Implementation Plan: SDK-Driven BLE Scan Filtering

## Overview

This implementation plan adds SDK-driven BLE scan filtering to the QRing Flutter plugin. The approach moves all device filtering logic from Flutter (Dart) to the Android native layer (Java), where we can access raw BLE advertisement data and apply SDK validation rules. This ensures users only see QRing-compatible devices in scan results.

The implementation will be done in Java for Android native code and Dart for Flutter bridge code.

## Tasks

- [x] 1. Create Scanned Device Model
  - Create new `ScannedDevice.java` class
  - Implement constructor accepting BluetoothDevice and RSSI
  - Implement `updateRssi()` method with 5 dBm threshold for significant changes
  - Implement `toMap()` method for Flutter bridge conversion
  - Implement `equals()` and `hashCode()` based on MAC address
  - Add getters for macAddress, name, rssi, lastSeenTimestamp
  - _Requirements: 6.1, 6.2, 6.3, 6.5_

- [x] 1.1 Write unit tests for Scanned Device Model
  - Test RSSI update threshold (5 dBm)
  - Test timestamp updates
  - Test map conversion
  - Test equality based on MAC address
  - _Requirements: 6.1, 6.2, 6.3, 6.5_

- [x] 2. Create BLE Scan Filter
  - Create new `BleScanFilter.java` class
  - Define SDK validation rules constants (device name prefixes: "O_", "Q_", "R")
  - Define RSSI threshold constant (-100 dBm minimum)
  - Implement `validateDevice()` method checking MAC address, RSSI, and device name
  - Implement `handleDiscoveredDevice()` method for filtering and deduplication
  - Implement device tracking with HashMap<String, ScannedDevice>
  - Implement `reset()` method to clear discovered devices
  - Implement `setCallback()` method for device emission
  - Add logging for accepted and rejected devices
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 7.1, 7.2, 7.3_

- [x] 2.1 Write property test for SDK rules application
  - **Property 1: SDK Rules Application**
  - **Validates: Requirements 1.1**

- [x] 2.2 Write property test for name-independent validation
  - **Property 4: Name-Independent Validation**
  - **Property 5: Null Name Acceptance**
  - **Validates: Requirements 1.4, 1.5**

- [x] 2.3 Write property test for device name validation
  - Test valid QRing name prefixes (O_, Q_, R) are accepted
  - Test invalid name prefixes are rejected
  - Test null/empty names with valid other properties are accepted
  - _Requirements: 1.4, 1.5_

- [x] 2.4 Write property test for RSSI threshold
  - Test devices with RSSI below -100 dBm are rejected
  - Test devices with RSSI above -100 dBm are accepted
  - _Requirements: 7.3_

- [x] 2.5 Write property test for MAC-based deduplication
  - **Property 13: MAC-Based Deduplication**
  - **Property 14: Update on Rediscovery**
  - **Validates: Requirements 5.1, 5.2**

- [x] 2.6 Write property test for RSSI updates
  - **Property 15: RSSI Update**
  - **Validates: Requirements 5.3**

- [x] 2.7 Write property test for exclusion rules
  - **Property 21: Exclude Non-QRing UUIDs**
  - **Property 22: Exclude Non-QRing Manufacturer Data**
  - **Property 23: Exclude Failed Validation**
  - **Property 25: No False Positives**
  - **Validates: Requirements 7.1, 7.2, 7.3, 8.5**

- [x] 3. Update BLE Manager to Use Scan Filter
  - Update `BleManager.java` to create BleScanFilter instance
  - Wire BleScanFilter callback to emit devices to Flutter
  - Update `startScan()` to call `scanFilter.reset()` before scanning
  - Update `onLeScan()` callback to delegate to `scanFilter.handleDiscoveredDevice()`
  - Update `onParsedData()` callback to delegate to scan filter
  - Update `onBatchScanResults()` callback to delegate to scan filter
  - Remove old device filtering logic from `handleDeviceFound()`
  - _Requirements: 2.1, 2.2, 2.3, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3.1 Write property test for validation before emission
  - **Property 6: Validation Before Emission**
  - **Property 9: Only Valid Devices to Flutter**
  - **Property 24: SDK Compatibility Guarantee**
  - **Validates: Requirements 2.1, 2.5, 8.2**

- [x] 3.2 Write property test for data extraction
  - **Property 7: Service UUID Extraction**
  - **Property 8: Manufacturer Data Extraction**
  - **Property 10: MAC Address Extraction**
  - **Property 11: RSSI Extraction**
  - **Property 12: Device Name Extraction**
  - **Validates: Requirements 2.2, 2.3, 3.3, 3.4, 3.5**

- [x] 3.3 Write property test for required fields
  - **Property 16: MAC Address Presence**
  - **Property 17: Device Name Field Presence**
  - **Property 18: RSSI Field Presence**
  - **Property 20: Timestamp Presence**
  - **Validates: Requirements 6.1, 6.2, 6.3, 6.5**

- [x] 4. Checkpoint - Test Scan Filter Integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Add Error Handling for Scan Failures
  - Update `onScanFailed()` callback to emit specific error codes
  - Add error handling for Bluetooth disabled state
  - Add error handling for missing permissions
  - Implement error logging with timestamps and context
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 5.1 Write unit tests for error handling
  - Test Bluetooth disabled error reporting
  - Test permission denied error reporting
  - Test scan failure error reporting
  - Test empty results handling (not an error)
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 5.2 Write property test for scan failure reporting
  - **Property 26: Scan Failure Reporting**
  - **Validates: Requirements 10.3**

- [x] 6. Add Debug Logging Support
  - Add debug flag to BleScanFilter
  - Implement raw advertisement data logging when debug enabled
  - Add device discovery logging (MAC, name, RSSI)
  - Add validation decision logging (accepted/rejected with reason)
  - Add scan start/stop event logging
  - _Requirements: 6.4, 7.5, 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 6.1 Write property test for debug metadata
  - **Property 19: Debug Metadata Presence**
  - **Validates: Requirements 6.4**

- [x] 7. Update Flutter Device Scanning Screen
  - Remove device name filtering logic from `device_scanning_screen.dart`
  - Update to display all devices received from native layer
  - Simplify device list rendering (no filtering needed)
  - Update UI to show device name, MAC address, and RSSI
  - Add empty state message when no devices found
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 8. Update Flutter Bridge (if needed)
  - Verify existing `startScan()` and `stopScan()` methods work with new filter
  - Verify device stream correctly receives filtered devices
  - Update device model in Dart if needed to match new native format
  - Ensure error stream receives scan errors
  - _Requirements: 11.1, 11.2, 11.3, 11.5_

- [x] 9. Write Integration Tests
  - Test scan shows only QRing devices (O_, Q_, R prefixes)
  - Test non-QRing devices are filtered out
  - Test devices with no name but valid properties appear
  - Test same device doesn't appear multiple times
  - Test RSSI updates reflect signal strength changes
  - Test Bluetooth disabled error is shown
  - Test permission denied error is shown
  - Test empty scan results handled gracefully
  - _Requirements: All_

- [x] 10. Update Documentation
  - Update README.md with new scan filtering behavior
  - Document SDK validation rules (device name prefixes)
  - Document that Flutter receives only validated devices
  - Add troubleshooting guide for scan issues
  - Document debug logging capabilities
  - _Requirements: All_

- [x] 11. Final Checkpoint - Complete System Test
  - Run all unit tests
  - Run all property tests
  - Run all integration tests
  - Test on physical device with QRing hardware
  - Test on physical device with non-QRing BLE devices
  - Verify all requirements are met
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end scenarios
- The implementation follows a bottom-up approach: models first, then filter, then integration
- Java is used for Android native code, Dart is used for Flutter code
- All filtering must happen in native layer before emitting to Flutter
- Device name prefixes (O_, Q_, R) are the primary filtering mechanism based on SDK sample code
- Future enhancements (Service UUID, Manufacturer Data) should be added when SDK documentation is available
