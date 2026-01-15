# Implementation Plan: Production-Grade BLE Connection Manager

## Overview

This implementation plan transforms the QRing Flutter plugin into a production-grade BLE connection manager. The approach follows a phased strategy: first creating the core BLE state machine and permission handling, then enhancing the foreground service and auto-reconnect capabilities, and finally integrating everything with comprehensive testing.

The implementation will be done in Java for Android native code and Dart for Flutter bridge code.

## Tasks

- [x] 1. Create BLE Connection Manager with State Machine
  - Create new `BleConnectionManager.java` class with state enum (IDLE, SCANNING, CONNECTING, PAIRING, CONNECTED, DISCONNECTED, RECONNECTING, ERROR)
  - Implement state transition validation logic
  - Implement observer pattern for state change notifications
  - Add state transition logging
  - _Requirements: 1.2, 1.3, 1.4, 1.5_

- [x] 1.1 Write property test for state machine
  - **Property 1: Valid State Enum**
  - **Validates: Requirements 1.2**

- [x] 1.2 Write property test for state transition validation
  - **Property 2: State Validation Before Operations**
  - **Validates: Requirements 1.3, 1.5**

- [x] 1.3 Write property test for observer notifications
  - **Property 3: Observer Notification on State Change**
  - **Validates: Requirements 1.4**

- [x] 2. Enhance Permission Manager for Android 12+
  - Update `PermissionManager.java` to check BLUETOOTH_SCAN permission (Android 12+)
  - Update `PermissionManager.java` to check BLUETOOTH_CONNECT permission (Android 12+)
  - Add Android version detection logic
  - Implement permission error message generation
  - Add method to get list of missing permissions
  - _Requirements: 2.1, 2.2, 2.3, 2.6, 2.7_

- [x] 2.1 Write property test for permission checking
  - **Property 4: Permission Check Before Scan (Android 12+)**
  - **Property 5: Permission Check Before Connect (Android 12+)**
  - **Property 6: Location Permission Check (Android < 12)**
  - **Validates: Requirements 2.1, 2.2, 2.3**

- [x] 2.2 Write property test for permission error reporting
  - **Property 7: Permission Error Reporting**
  - **Validates: Requirements 2.6**

- [x] 3. Implement Pairing and Bonding Manager
  - Create new `PairingManager.java` class
  - Implement bond state checking logic
  - Implement createBond() trigger for unbonded devices
  - Add BroadcastReceiver for BOND_STATE_CHANGED events
  - Implement bonding retry logic (max 1 retry)
  - Add timeout handling for bonding (30 seconds)
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 3.1 Write property test for bond state checking
  - **Property 8: Bond State Check Before Connection**
  - **Validates: Requirements 3.1**

- [x] 3.2 Write property test for bonding workflow
  - **Property 9: Bonding Trigger for Unbonded Devices**
  - **Property 10: GATT Connection After Bonding**
  - **Validates: Requirements 3.2, 3.3, 3.6**

- [x] 3.3 Write property test for bonding retry
  - **Property 11: Bonding Retry on Failure**
  - **Validates: Requirements 3.4**

- [x] 4. Implement GATT Connection Manager
  - Create new `GattConnectionManager.java` class
  - Implement GATT connection with autoConnect=true
  - Implement service discovery workflow
  - Implement MTU negotiation (request 512 bytes)
  - Add BluetoothGattCallback implementation
  - Implement proper disconnect and close sequence
  - Add GATT resource cleanup logic
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4.1 Write property test for GATT connection parameters
  - **Property 12: AutoConnect Parameter**
  - **Validates: Requirements 4.1**

- [x] 4.2 Write property test for service discovery
  - **Property 13: Service Discovery Before Data Operations**
  - **Property 14: MTU Negotiation After Service Discovery**
  - **Property 15: Disconnect on Service Discovery Failure**
  - **Validates: Requirements 4.2, 4.3, 4.4**

- [x] 4.3 Write property test for GATT cleanup
  - **Property 16: GATT Resource Cleanup**
  - **Validates: Requirements 4.5, 9.5**

- [x] 5. Integrate BLE Components into Connection Manager
  - Wire `PairingManager` into `BleConnectionManager`
  - Wire `GattConnectionManager` into `BleConnectionManager`
  - Implement connection workflow: check bond → pair if needed → connect GATT → discover services → negotiate MTU
  - Add connection timeout handling
  - Implement disconnect workflow: disconnect GATT → close GATT → clear state
  - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 9.1, 9.2_

- [x] 5.1 Write property test for connection workflow
  - **Property 36: Disconnect Then Close Sequence**
  - **Validates: Requirements 9.1, 9.2**

- [x] 6. Checkpoint - Test Basic Connection Flow
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Enhance Auto-Reconnect Engine with Exponential Backoff
  - Update `ServiceConnectionManager.java` (or create `AutoReconnectEngine.java`)
  - Implement exponential backoff calculation: 10s (attempts 1-5), 30s (6-10), 60s+ (11+)
  - Add jitter calculation (±20% random variation)
  - Implement max delay cap (5 minutes)
  - Add reconnection scheduling with Handler
  - Implement reconnection attempt counter
  - _Requirements: 5.2, 5.6_

- [x] 7.1 Write property test for exponential backoff
  - **Property 18: Exponential Backoff Strategy**
  - **Validates: Requirements 5.2**

- [x] 7.2 Write property test for reconnection limits
  - **Property 22: Reconnection Attempt Limit**
  - **Validates: Requirements 5.6**

- [x] 8. Implement Device Persistence
  - Create `DevicePersistenceModel.java` class
  - Implement SharedPreferences save for device MAC and name
  - Implement SharedPreferences load for device info
  - Implement SharedPreferences clear for manual disconnect
  - Add persistence calls to connection success handler
  - Add persistence clear to manual disconnect handler
  - _Requirements: 5.3, 12.1, 12.2, 12.3, 12.4_

- [x] 8.1 Write property test for device persistence
  - **Property 19: Device MAC Persistence on Connection**
  - **Property 46: Device Name Persistence on Connection**
  - **Property 47: Device Info Loading on App Restart**
  - **Property 48: Clear Persisted Info on Manual Disconnect**
  - **Validates: Requirements 5.3, 12.1, 12.2, 12.3, 12.4**

- [x] 9. Implement Auto-Reconnect State Management
  - Add auto-reconnect enable/disable flag to `BleConnectionManager`
  - Implement reconnecting state transition on unexpected disconnect
  - Implement auto-reconnect disable on manual disconnect
  - Add reconnection success handler (restore full GATT setup)
  - Integrate auto-reconnect engine with connection manager
  - _Requirements: 5.1, 5.4, 5.5, 9.3, 9.4_

- [x] 9.1 Write property test for auto-reconnect behavior
  - **Property 17: Reconnecting State on Unexpected Disconnection**
  - **Property 20: Auto-Reconnect Disabled on Manual Disconnect**
  - **Property 21: Full GATT Setup on Reconnection**
  - **Property 37: Disconnected State After Manual Disconnect**
  - **Validates: Requirements 5.1, 5.4, 5.5, 9.3, 9.4**

- [x] 10. Implement Bluetooth State Monitoring
  - Create BroadcastReceiver for BluetoothAdapter.ACTION_STATE_CHANGED
  - Register receiver in `ServiceConnectionManager` or `BleConnectionManager`
  - Implement pause reconnection on Bluetooth OFF
  - Implement resume reconnection on Bluetooth ON
  - Add Bluetooth state tracking variable
  - _Requirements: 10.2_

- [x] 10.1 Write property test for Bluetooth state monitoring
  - **Property 38: Bluetooth Toggle Reconnection**
  - **Validates: Requirements 10.2**

- [x] 11. Checkpoint - Test Reconnection Logic
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Enhance Foreground Service with START_STICKY
  - Update `QRingBackgroundService.java` onStartCommand to return START_STICKY
  - Implement state restoration in onCreate (load saved device MAC)
  - Add service restart logic on boot (BootReceiver)
  - Add service restart logic on Bluetooth ON (BluetoothReceiver)
  - Update AndroidManifest.xml with RECEIVE_BOOT_COMPLETED permission
  - Register BootReceiver and BluetoothReceiver in manifest
  - _Requirements: 6.2, 6.3, 6.4_

- [x] 12.1 Write unit test for START_STICKY
  - Verify onStartCommand returns START_STICKY
  - **Validates: Requirements 6.2**

- [x] 12.2 Write property test for boot receiver
  - **Property 23: Boot Receiver Service Restart**
  - **Validates: Requirements 6.3**

- [x] 12.3 Write property test for Bluetooth receiver
  - **Property 24: Bluetooth ON Service Restart**
  - **Validates: Requirements 6.4**

- [x] 13. Enhance Notification Manager with Dynamic Content
  - Update `ServiceNotificationManager.java` to build connected notification with device name and battery
  - Implement disconnected notification builder
  - Implement reconnecting notification builder with attempt number
  - Implement error notification builder
  - Add notification update method
  - Implement Find My Ring notification action with PendingIntent
  - Add notification channel creation for Android 8+
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 13.1 Write property test for notification content
  - **Property 25: Persistent Notification While Service Running**
  - **Property 26: Notification Contains Device Name**
  - **Property 27: Notification Contains Connection State**
  - **Property 28: Notification Contains Battery When Available**
  - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [x] 13.2 Write property test for notification actions
  - **Property 29: Find My Ring on Notification Tap**
  - **Validates: Requirements 7.5, 10.5**

- [x] 13.3 Write property test for notification updates
  - **Property 30: Notification Updates on State Change**
  - **Validates: Requirements 7.6**

- [x] 14. Implement Find My Ring from Notification
  - Update `QRingBackgroundService.java` to handle ACTION_FIND_MY_RING intent
  - Implement find ring command execution in service
  - Add notification feedback for success (show "Ring activated" for 3 seconds)
  - Add notification feedback for failure (show "Ring not connected" for 3 seconds)
  - Restore normal notification after feedback timeout
  - _Requirements: 7.5, 8.4_

- [x] 15. Simplify Flutter Bridge API
  - Update `QringSdkFlutterPlugin.java` to use new `BleConnectionManager`
  - Implement connectRing method
  - Implement disconnectRing method
  - Implement getConnectionState method
  - Implement findMyRing method (delegate to BleConnectionManager)
  - Remove direct BLE control methods (keep only observation)
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 16. Implement Flutter Event Streams
  - Add EventChannel for connection state changes
  - Implement onBleConnected event emission
  - Implement onBleDisconnected event emission
  - Implement onBleReconnecting event emission
  - Implement onBatteryUpdated event emission
  - Implement onBleError event emission with error details
  - Wire event emissions to BleConnectionManager state changes
  - _Requirements: 8.5, 8.6, 8.7, 8.8, 8.9_

- [x] 16.1 Write property test for Flutter event emissions
  - **Property 31: Connected Event Emission**
  - **Property 32: Disconnected Event Emission**
  - **Property 33: Reconnecting Event Emission**
  - **Property 34: Battery Updated Event Emission**
  - **Property 35: Error Event Emission**
  - **Validates: Requirements 8.5, 8.6, 8.7, 8.8, 8.9**

- [x] 17. Update Dart API in qring_sdk_flutter.dart
  - Update connection state enum to include PAIRING and RECONNECTING
  - Add error stream for BLE errors
  - Update documentation for simplified API
  - Add examples for new event streams
  - _Requirements: 8.5, 8.6, 8.7, 8.8, 8.9_

- [x] 18. Checkpoint - Test Flutter Integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 19. Implement Error Handling and Reporting
  - Create error code constants (PERMISSION_DENIED, BLUETOOTH_OFF, PAIRING_FAILED, etc.)
  - Implement error state transition in BleConnectionManager
  - Implement permission error reporting to Flutter
  - Implement pairing error reporting with reason
  - Implement GATT error reporting with operation details
  - Implement error state recovery (allow retry after acknowledgment)
  - Add error logging with timestamp, code, message, and stack trace
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [x] 19.1 Write property test for error handling
  - **Property 41: Error State on Operation Failure**
  - **Property 42: Specific Permission Error Reporting**
  - **Property 43: Pairing Error with Reason**
  - **Property 44: GATT Error with Operation Details**
  - **Property 45: Retry Allowed After Error Acknowledgment**
  - **Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5**

- [x] 20. Implement Permission Revocation Handling
  - Add SecurityException catching in all BLE operations
  - Implement graceful handling when permissions revoked during connection
  - Add permission check before each BLE operation
  - Implement error reporting for permission revocation
  - _Requirements: 10.4_

- [x] 20.1 Write property test for permission revocation
  - **Property 40: Graceful Permission Revocation Handling**
  - **Validates: Requirements 10.4**

- [x] 21. Implement Out-of-Range Reconnection
  - Verify auto-reconnect handles out-of-range scenarios
  - Test reconnection when device returns to range
  - Ensure exponential backoff applies to out-of-range reconnection
  - _Requirements: 10.3_

- [x] 21.1 Write property test for out-of-range reconnection
  - **Property 39: Out-of-Range Reconnection**
  - **Validates: Requirements 10.3**

- [x] 22. Update AndroidManifest.xml
  - Add BLUETOOTH_SCAN permission with minSdkVersion="31"
  - Add BLUETOOTH_CONNECT permission with minSdkVersion="31"
  - Add POST_NOTIFICATIONS permission with minSdkVersion="33"
  - Add RECEIVE_BOOT_COMPLETED permission
  - Update service declaration with foregroundServiceType="connectedDevice"
  - Register BootReceiver with BOOT_COMPLETED intent filter
  - Register BluetoothReceiver with STATE_CHANGED intent filter
  - _Requirements: 2.1, 2.2, 2.4, 6.3, 6.4_

- [x] 23. Write Integration Tests
  - Test app killed → ring stays connected
  - Test Bluetooth toggle → automatic reconnection
  - Test out-of-range → reconnection when back in range
  - Test permission revoked → graceful error handling
  - Test notification action → find my ring works
  - Test device reboot → service restarts and reconnects
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [x] 24. Write Example App Updates
  - Update example app to demonstrate new connection flow
  - Add UI for connection state display
  - Add UI for error display
  - Add UI for reconnection status
  - Add permission request flow
  - Add background service start/stop controls
  - _Requirements: All_

- [x] 25. Update Documentation
  - Update README.md with new architecture overview
  - Document permission requirements for Android 12+
  - Document background service behavior
  - Document auto-reconnect strategy
  - Add troubleshooting guide
  - Add migration guide from old API
  - _Requirements: All_

- [x] 26. Final Checkpoint - Complete System Test
  - Run all unit tests
  - Run all property tests
  - Run all integration tests
  - Test on physical device (Android 12+)
  - Test on physical device (Android < 12)
  - Verify all requirements are met
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end scenarios
- The implementation follows a bottom-up approach: core components first, then integration, then testing
- Java is used for Android native code, Dart is used for Flutter bridge code
- All BLE operations must check permissions before proceeding
- All state transitions must be validated
- All errors must be reported to Flutter with specific error codes
- The foreground service must survive app termination and system restarts
