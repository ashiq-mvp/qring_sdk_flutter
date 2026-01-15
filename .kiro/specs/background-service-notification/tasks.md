# Implementation Plan: Background Service with Persistent Notification

## Overview

This implementation plan creates an Android Foreground Service that maintains continuous communication with the QRing smart ring, even when the Flutter app is killed. The service provides a persistent notification with a "Find My Ring" action button, automatic reconnection, and full Flutter integration via MethodChannel.

## Tasks

- [x] 1. Create Core Service Infrastructure
  - [x] 1.1 Create QRingBackgroundService class extending Service
    - Implement onCreate(), onStartCommand(), onDestroy(), onBind()
    - Return START_STICKY from onStartCommand() for auto-restart
    - Add service lifecycle logging
    - _Requirements: 1.1, 1.4_

  - [x] 1.2 Create NotificationConfig constants class
    - Define CHANNEL_ID, CHANNEL_NAME, NOTIFICATION_ID
    - Define action constants (ACTION_FIND_MY_RING, ACTION_OPEN_APP)
    - Define intent extra keys
    - _Requirements: 3.1, 4.1_

  - [x] 1.3 Update AndroidManifest.xml
    - Add FOREGROUND_SERVICE permission
    - Add FOREGROUND_SERVICE_CONNECTED_DEVICE permission
    - Add POST_NOTIFICATIONS permission
    - Declare QRingBackgroundService with foregroundServiceType="connectedDevice"
    - Declare NotificationActionReceiver
    - _Requirements: 8.1, 8.4_

  - [ ]* 1.4 Write unit tests for service lifecycle
    - Test onCreate() initializes components
    - Test onStartCommand() returns START_STICKY
    - Test onDestroy() cleans up resources
    - _Requirements: 1.1, 1.4, 1.6_

- [x] 2. Implement Notification Management
  - [x] 2.1 Create ServiceNotificationManager class
    - Implement createNotificationChannel() for Android 8.0+
    - Set channel importance to IMPORTANCE_LOW
    - Implement buildNotification() with title, message, status, actions
    - Implement updateNotification() method
    - _Requirements: 3.1, 3.2, 3.8_

  - [x] 2.2 Implement notification content builder
    - Add app icon to notification
    - Add title "Smart Ring Connected" / "Smart Ring Disconnected"
    - Add status description (Connected, Disconnected, Reconnecting)
    - Create PendingIntent for opening app (content intent)
    - _Requirements: 3.3, 3.4, 3.5, 5.1_

  - [x] 2.3 Implement "Find My Ring" action button
    - Create PendingIntent for Find My Ring action
    - Add action button to notification when device is connected
    - Use FLAG_IMMUTABLE for Android 12+ compatibility
    - _Requirements: 4.1, 8.2_

  - [x] 2.4 Create NotificationActionReceiver BroadcastReceiver
    - Handle ACTION_FIND_MY_RING intent
    - Forward action to QRingBackgroundService
    - Add intent filter in manifest
    - _Requirements: 4.2, 12.2_

  - [ ]* 2.5 Write property test for notification structure
    - **Property: Notification Content Completeness**
    - **Validates: Requirements 3.3, 3.4, 3.5, 4.1**
    - Generate random connection states
    - Verify notification contains icon, title, description, and actions

  - [ ]* 2.6 Write property test for notification status updates
    - **Property: Status Text Matches Connection State**
    - **Validates: Requirements 3.6, 3.7**
    - Generate random connection states (connected, disconnected, reconnecting)
    - Verify notification text matches state

- [x] 3. Implement Connection Management
  - [x] 3.1 Create ServiceConnectionManager class
    - Implement connect(deviceMac, callback) method
    - Implement disconnect() method
    - Implement isConnected() method
    - Initialize QRing SDK BleOperateManager
    - Handle connection state callbacks
    - _Requirements: 2.1, 2.2, 7.1_

  - [x] 3.2 Implement automatic reconnection logic
    - Implement startAutoReconnect(deviceMac) method
    - Implement stopAutoReconnect() method
    - Use Handler for scheduling reconnection attempts
    - Initial retry interval: 10 seconds
    - _Requirements: 7.1, 7.3_

  - [x] 3.3 Implement exponential backoff strategy
    - Implement calculateBackoffDelay(attemptNumber) method
    - After 5 failures: increase to 30 seconds
    - After 10 failures: increase to 60 seconds
    - Maximum delay: 5 minutes (300 seconds)
    - Add jitter to prevent thundering herd
    - _Requirements: 7.2, 9.2_

  - [x] 3.4 Implement Bluetooth state monitoring
    - Register BroadcastReceiver for BluetoothAdapter.ACTION_STATE_CHANGED
    - Pause reconnection when Bluetooth is disabled
    - Resume reconnection immediately when Bluetooth is enabled
    - _Requirements: 7.4, 7.5_

  - [ ]* 3.5 Write property test for reconnection backoff
    - **Property: Reconnection Backoff**
    - **Validates: Requirements 7.1, 7.2, 7.3, 9.2**
    - Generate random number of failed attempts (1-20)
    - Verify delay increases exponentially
    - Verify maximum delay cap is respected
    - Verify delay resets after successful connection

  - [ ]* 3.6 Write property test for Bluetooth state handling
    - **Property: Bluetooth State Handling**
    - **Validates: Requirements 7.4, 7.5**
    - Generate random Bluetooth state changes
    - Verify reconnection pauses when disabled
    - Verify reconnection resumes when enabled

- [x] 4. Checkpoint - Verify service and notification basics
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement State Persistence
  - [x] 5.1 Create state persistence methods in QRingBackgroundService
    - Implement saveConnectionState(deviceMac) using SharedPreferences
    - Implement loadSavedDeviceMac() method
    - Implement clearSavedState() method
    - Use MODE_PRIVATE for SharedPreferences
    - _Requirements: 11.1, 11.2, 11.4_

  - [x] 5.2 Integrate state persistence with connection lifecycle
    - Save device MAC when connection succeeds
    - Load saved MAC on service start
    - Attempt reconnection if saved MAC exists
    - Clear saved MAC on manual disconnect
    - Save connection state on service stop
    - _Requirements: 11.3, 11.5_

  - [ ]* 5.3 Write property test for state persistence round trip
    - **Property: State Persistence Round Trip**
    - **Validates: Requirements 11.1, 11.2, 11.3**
    - Generate random device MAC addresses
    - Save MAC, restart service, verify reconnection attempt to same device

- [x] 6. Implement Command Handling
  - [x] 6.1 Create ServiceCommandHandler class
    - Implement handleFindMyRing(callback) method
    - Validate device is connected before execution
    - Execute FindDeviceReq through QRing SDK
    - Return success/error through callback
    - _Requirements: 4.2, 4.6, 12.3_

  - [x] 6.2 Implement command execution in QRingBackgroundService
    - Handle ACTION_FIND_MY_RING from NotificationActionReceiver
    - Call ServiceCommandHandler.handleFindMyRing()
    - Update notification with feedback on success/failure
    - Show "Ring activated" on success
    - Show "Ring not connected" on failure
    - _Requirements: 4.3, 4.4, 4.5_

  - [x] 6.3 Implement generic command handler for future extensibility
    - Implement handleCommand(command, params, callback) method
    - Support custom commands from Flutter
    - Validate connection state before execution
    - _Requirements: 6.4, 12.3_

  - [ ]* 6.4 Write property test for command validation
    - **Property: Command Execution Validation**
    - **Validates: Requirements 4.6, 12.3**
    - Generate random commands
    - Verify commands only execute when connected
    - Verify error returned when disconnected

  - [ ]* 6.5 Write property test for command feedback
    - **Property: Notification Action Feedback**
    - **Validates: Requirements 4.4, 4.5, 12.4**
    - Generate random command results (success/failure)
    - Verify notification updates with appropriate feedback within 2 seconds

- [x] 7. Implement Flutter Integration
  - [x] 7.1 Add MethodChannel methods to QringSdkFlutterPlugin
    - Implement startBackgroundService(deviceMac) method
    - Implement stopBackgroundService() method
    - Implement isServiceRunning() method
    - Implement sendRingCommand(command, params) method
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 7.2 Implement service control in startBackgroundService
    - Create Intent with device MAC extra
    - Call startForegroundService() on Android 8.0+
    - Call startService() on older Android versions
    - Return success/error to Flutter
    - _Requirements: 6.1_

  - [x] 7.3 Implement service control in stopBackgroundService
    - Create Intent for stopping service
    - Call stopService()
    - Return success to Flutter
    - _Requirements: 6.2_

  - [x] 7.4 Implement isServiceRunning check
    - Query ActivityManager for running services
    - Check if QRingBackgroundService is running
    - Return boolean to Flutter
    - _Requirements: 6.3_

  - [x] 7.5 Create EventChannel for service state updates
    - Create "qring_service_state" EventChannel
    - Implement EventChannel.StreamHandler
    - Emit events: serviceStarted, serviceStopped, deviceConnected, deviceDisconnected
    - _Requirements: 6.5_

  - [x] 7.6 Implement event emission in QRingBackgroundService
    - Emit serviceStarted when service starts
    - Emit serviceStopped when service stops
    - Emit deviceConnected when ring connects
    - Emit deviceDisconnected when ring disconnects
    - Use LocalBroadcastManager to send events to plugin
    - _Requirements: 6.5_

  - [ ]* 7.7 Write property test for Flutter method channel
    - **Property: Service Control Methods**
    - **Validates: Requirements 6.1, 6.2, 6.3**
    - Test startBackgroundService starts service
    - Test stopBackgroundService stops service
    - Test isServiceRunning returns accurate state

  - [ ]* 7.8 Write property test for event channel
    - **Property: Connection State Synchronization**
    - **Validates: Requirements 6.5**
    - Generate random connection state changes
    - Verify events are emitted to Flutter within 1 second

- [x] 8. Checkpoint - Verify Flutter integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement Error Handling and Recovery
  - [x] 9.1 Add try-catch blocks for QRing SDK operations
    - Wrap all SDK calls in try-catch
    - Catch BluetoothException, SecurityException, generic Exception
    - Log errors to Android logcat
    - Update notification with error status
    - _Requirements: 10.1_

  - [x] 9.2 Implement SDK reinitialization on critical errors
    - Detect critical errors (SDK initialization failure)
    - Attempt to reinitialize QRing SDK
    - Retry connection after reinitialization
    - _Requirements: 10.2_

  - [x] 9.3 Implement graceful shutdown on unrecoverable errors
    - Detect unrecoverable errors (permission denied, SDK unavailable)
    - Display error notification
    - Stop service gracefully
    - Clear saved state
    - _Requirements: 10.6_

  - [x] 9.4 Implement low memory handling
    - Override onLowMemory() callback
    - Release non-essential resources (cached data, unused objects)
    - Keep connection alive
    - _Requirements: 10.5_

  - [x] 9.5 Implement crash recovery
    - Verify START_STICKY is set in onStartCommand()
    - Implement state restoration in onCreate()
    - Load saved device MAC and attempt reconnection
    - _Requirements: 10.3, 10.4_

  - [ ]* 9.6 Write property test for error recovery
    - **Property: Error Recovery**
    - **Validates: Requirements 10.1, 10.2**
    - Generate random SDK exceptions
    - Verify service catches exceptions and continues running
    - Verify SDK reinitialization on critical errors

  - [ ]* 9.7 Write property test for resource cleanup
    - **Property: Resource Cleanup**
    - **Validates: Requirements 2.6, 10.5**
    - Test service termination scenarios
    - Verify all QRing SDK resources are released
    - Verify wake locks are released

- [x] 10. Implement Permission Handling
  - [x] 10.1 Create permission check methods
    - Implement checkBluetoothPermissions() for Android 12+
    - Check BLUETOOTH_CONNECT permission
    - Check BLUETOOTH_SCAN permission
    - Implement checkNotificationPermission() for Android 13+
    - _Requirements: 8.2, 8.3, 8.4_

  - [x] 10.2 Implement permission validation on service start
    - Check all required permissions in onStartCommand()
    - If permissions missing, show error notification
    - Stop service if permissions not granted
    - _Requirements: 8.5_

  - [x] 10.3 Create error notification for permission issues
    - Build notification with error message
    - Add action button to open app settings
    - Display notification and stop service
    - _Requirements: 8.5_

  - [ ]* 10.4 Write property test for permission validation
    - **Property: Permission Validation**
    - **Validates: Requirements 8.5**
    - Generate random permission states (granted/denied)
    - Verify service stops when permissions denied
    - Verify error notification is displayed

- [x] 11. Implement Battery Optimization
  - [x] 11.1 Implement wake lock management
    - Acquire partial wake lock only during active operations
    - Release wake lock when idle
    - Implement releaseWakeLocks() method
    - _Requirements: 9.5_

  - [x] 11.2 Optimize reconnection strategy
    - Use exponential backoff (already implemented in task 3.3)
    - Add jitter to prevent synchronized reconnections
    - Pause reconnection during Doze mode (foreground service continues)
    - _Requirements: 9.2, 9.3_

  - [x] 11.3 Implement operation batching
    - Batch multiple SDK commands when possible
    - Reduce BLE operation frequency during idle
    - _Requirements: 9.4_

- [x] 12. Create Dart API for Flutter
  - [x] 12.1 Create ServiceState data model in Dart
    - Define ServiceState class with isRunning, isConnected, deviceMac, etc.
    - Implement toMap() and fromMap() methods
    - _Requirements: 6.5_

  - [x] 12.2 Add service control methods to QringSdkFlutter class
    - Implement startBackgroundService(String deviceMac)
    - Implement stopBackgroundService()
    - Implement isServiceRunning()
    - Implement sendRingCommand(String command, Map params)
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 12.3 Add serviceStateStream getter
    - Create EventChannel stream for service state
    - Parse events into ServiceState objects
    - Expose as Stream<ServiceState>
    - _Requirements: 6.5_

  - [ ]* 12.4 Write unit tests for Dart API
    - Test ServiceState serialization/deserialization
    - Test method channel calls
    - Test event channel stream parsing

- [x] 13. Update Example App UI
  - [x] 13.1 Add service control UI to QuickActionsScreen
    - Add "Start Background Service" button
    - Add "Stop Background Service" button
    - Show service status indicator
    - Display connection state from serviceStateStream
    - _Requirements: 6.1, 6.2_

  - [x] 13.2 Implement service control logic
    - Call startBackgroundService() with current device MAC
    - Call stopBackgroundService() on button tap
    - Listen to serviceStateStream and update UI
    - Show error messages if service fails to start
    - _Requirements: 6.1, 6.2, 6.5_

  - [x] 13.3 Add service status to app bar or persistent widget
    - Show badge/icon when service is running
    - Show connection status (connected/disconnected)
    - Allow quick access to stop service
    - _Requirements: 6.5_

- [x] 14. Integration Testing
  - [x] 14.1 Test service lifecycle
    - Start service from Flutter app
    - Verify notification appears
    - Kill Flutter app
    - Verify service continues running
    - Verify notification remains visible
    - _Requirements: 1.3, 3.2_

  - [x] 14.2 Test Find My Ring from notification
    - Start service and connect device
    - Tap "Find My Ring" action in notification
    - Verify command executes without app running
    - Verify notification shows feedback
    - _Requirements: 4.2, 4.3, 4.4_

  - [x] 14.3 Test automatic reconnection
    - Start service and connect device
    - Simulate device disconnection (turn off ring)
    - Verify reconnection attempts start
    - Verify notification shows "Reconnecting..." status
    - Turn on ring and verify reconnection succeeds
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 14.4 Test service restart after system kill
    - Start service
    - Force stop service using adb or system settings
    - Verify service restarts automatically
    - Verify service attempts to reconnect to saved device
    - _Requirements: 1.4, 1.5, 10.4_

  - [x] 14.5 Test notification tap behavior
    - With app killed: tap notification, verify app launches
    - With app running: tap notification, verify app comes to foreground
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 14.6 Test Bluetooth state changes
    - Start service with connected device
    - Disable Bluetooth
    - Verify reconnection pauses
    - Enable Bluetooth
    - Verify reconnection resumes immediately
    - _Requirements: 7.4, 7.5_

  - [x] 14.7 Test permission handling
    - Revoke Bluetooth permissions
    - Attempt to start service
    - Verify error notification appears
    - Verify service stops
    - _Requirements: 8.5_

- [x] 15. Documentation
  - [x] 15.1 Create service implementation documentation
    - Document service architecture and components
    - Explain service lifecycle and state transitions
    - Document reconnection strategy and backoff algorithm
    - Add inline code comments to key methods
    - _Requirements: 13.1, 13.3_

  - [x] 15.2 Create Flutter integration guide
    - Document how to start/stop service from Flutter
    - List all MethodChannel methods and parameters
    - Provide code examples for each method
    - Document EventChannel events and their payloads
    - _Requirements: 13.2, 13.4_

  - [x] 15.3 Create troubleshooting guide
    - Document common issues (permissions, Bluetooth, battery optimization)
    - Provide solutions for each issue
    - Add debugging tips (logcat filters, notification inspection)
    - _Requirements: 13.5_

- [x] 16. Final Checkpoint - Complete Implementation
  - Ensure all tests pass, verify service works correctly, ask the user if ready for deployment.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- The service uses Java for Android native implementation
- Flutter integration uses Dart for the plugin API
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end workflows with real device
- Service must work on Android 8.0+ with special handling for Android 12+ and 13+
- Battery optimization is critical for user acceptance
- All SDK operations must be wrapped in error handling
