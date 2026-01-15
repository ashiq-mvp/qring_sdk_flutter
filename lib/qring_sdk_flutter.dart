import 'dart:async';

import 'package:flutter/services.dart';
import 'package:qring_sdk_flutter/src/models/ble_error.dart';
import 'package:qring_sdk_flutter/src/models/connection_state.dart';
import 'package:qring_sdk_flutter/src/models/qring_device.dart';
import 'package:qring_sdk_flutter/src/models/qring_device_info.dart';
import 'package:qring_sdk_flutter/src/models/service_state.dart';

// Export data models
export 'src/models/ble_error.dart';
export 'src/models/blood_oxygen_data.dart';
export 'src/models/blood_pressure_data.dart';
export 'src/models/connection_state.dart';
export 'src/models/display_settings.dart';
export 'src/models/exercise_data.dart';
export 'src/models/exercise_summary.dart';
export 'src/models/exercise_type.dart';
export 'src/models/health_measurement.dart';
export 'src/models/heart_rate_data.dart';
export 'src/models/qring_device.dart';
export 'src/models/qring_device_info.dart';
export 'src/models/service_state.dart';
export 'src/models/sleep_data.dart';
export 'src/models/step_data.dart';
export 'src/models/user_info.dart';
// Export API classes
export 'src/qring_exercise.dart';
export 'src/qring_firmware.dart';
export 'src/qring_health_data.dart';
export 'src/qring_settings.dart';

/// Main API class for the QRing SDK Flutter plugin.
///
/// This class provides the primary interface for interacting with QC smart ring devices.
/// It handles device discovery, connection management, and basic device operations.
///
/// ## Production-Grade BLE Connection Manager
///
/// The plugin now features a production-grade BLE Connection Manager that provides:
/// - **Automatic Reconnection**: Reconnects automatically when device comes back in range
/// - **Background Service**: Maintains connection even when app is killed
/// - **Reliable Pairing**: Handles device bonding with retry logic
/// - **State Management**: Clear state machine with IDLE, SCANNING, CONNECTING, PAIRING, CONNECTED, DISCONNECTED, RECONNECTING, ERROR states
/// - **Error Handling**: Comprehensive error reporting with specific error codes
///
/// ## Features
///
/// - **Device Discovery**: Scan for nearby QC Ring devices via BLE
/// - **Connection Management**: Connect, disconnect, and monitor connection status
/// - **Automatic Reconnection**: Reconnects automatically after unexpected disconnections
/// - **Background Service**: Maintains connection when app is in background or killed
/// - **Find My Ring**: Trigger vibration to locate the device
/// - **Battery Monitoring**: Check device battery level with real-time updates
/// - **Device Information**: Retrieve firmware version, hardware version, and capabilities
/// - **Permission Management**: Check and request required permissions
/// - **Factory Reset**: Reset device to factory defaults
///
/// ## Usage
///
/// ### Basic Connection Flow
///
/// ```dart
/// // Start scanning for devices
/// await QringSdkFlutter.startScan();
///
/// // Listen for discovered devices
/// QringSdkFlutter.devicesStream.listen((devices) {
///   for (var device in devices) {
///     print('Found: ${device.name}');
///   }
/// });
///
/// // Connect to a device
/// await QringSdkFlutter.connect('AA:BB:CC:DD:EE:FF');
///
/// // Monitor connection state (includes pairing and reconnecting states)
/// QringSdkFlutter.connectionStateStream.listen((state) {
///   switch (state) {
///     case ConnectionState.connecting:
///       print('Connecting...');
///       break;
///     case ConnectionState.pairing:
///       print('Pairing with device...');
///       break;
///     case ConnectionState.connected:
///       print('Connected!');
///       break;
///     case ConnectionState.reconnecting:
///       print('Reconnecting...');
///       break;
///     case ConnectionState.disconnected:
///       print('Disconnected');
///       break;
///     default:
///       print('State: $state');
///   }
/// });
///
/// // Use device features
/// await QringSdkFlutter.findRing();
/// int battery = await QringSdkFlutter.getBattery();
/// ```
///
/// ### Background Service for Persistent Connection
///
/// ```dart
/// // Start background service to maintain connection
/// await QringSdkFlutter.startBackgroundService('AA:BB:CC:DD:EE:FF');
///
/// // Monitor service state
/// QringSdkFlutter.serviceStateStream.listen((state) {
///   print('Service running: ${state.isRunning}');
///   print('Device connected: ${state.isConnected}');
///   if (state.reconnectAttempts > 0) {
///     print('Reconnecting... (attempt ${state.reconnectAttempts})');
///   }
/// });
///
/// // Stop background service
/// await QringSdkFlutter.stopBackgroundService();
/// ```
///
/// ### BLE Connection Manager Event Streams
///
/// The plugin provides specialized event streams for monitoring BLE operations:
///
/// ```dart
/// // Monitor BLE connection state changes
/// QringSdkFlutter.bleConnectionStateStream.listen((event) {
///   String newState = event['newState'];
///   print('BLE state: $newState');
///
///   if (newState == 'pairing') {
///     print('Pairing with device...');
///   } else if (newState == 'reconnecting') {
///     print('Attempting automatic reconnection...');
///   }
/// });
///
/// // Monitor battery updates
/// QringSdkFlutter.bleBatteryStream.listen((event) {
///   int battery = event['batteryLevel'];
///   print('Battery: $battery%');
/// });
///
/// // Handle BLE errors with typed error objects
/// QringSdkFlutter.errorStream.listen((error) {
///   print('Error [${error.code}]: ${error.message}');
///
///   switch (error.type) {
///     case BleErrorType.permissionDenied:
///       print('Please grant Bluetooth permissions');
///       break;
///     case BleErrorType.bluetoothOff:
///       print('Please enable Bluetooth');
///       break;
///     case BleErrorType.pairingFailed:
///       print('Failed to pair with device');
///       break;
///     case BleErrorType.connectionFailed:
///       print('Connection failed, will retry automatically');
///       break;
///     default:
///       print('Error: ${error.type.description}');
///   }
/// });
/// ```
///
/// ## Additional APIs
///
/// For specialized functionality, use the dedicated API classes:
/// - [QringHealthData]: Health data synchronization and manual measurements
/// - [QringSettings]: Device settings and continuous monitoring configuration
/// - [QringExercise]: Exercise tracking and workout management
/// - [QringFirmware]: Firmware update management
///
/// ## Platform Support
///
/// Currently supports Android only (API level 23+).
/// iOS support is planned for future releases.
///
/// ## Permissions
///
/// ### Android 12+ (API 31+)
/// - BLUETOOTH_SCAN: Required for scanning
/// - BLUETOOTH_CONNECT: Required for connecting
/// - POST_NOTIFICATIONS: Required for foreground service notification
///
/// ### Android < 12
/// - BLUETOOTH: Legacy Bluetooth permission
/// - BLUETOOTH_ADMIN: Legacy Bluetooth admin permission
/// - ACCESS_FINE_LOCATION: Required for BLE scanning
///
/// Use [checkPermissions] to verify permission status before operations.
class QringSdkFlutter {
  static const MethodChannel _channel = MethodChannel('qring_sdk_flutter');
  static const EventChannel _stateChannel = EventChannel(
    'qring_sdk_flutter/state',
  );
  static const EventChannel _devicesChannel = EventChannel(
    'qring_sdk_flutter/devices',
  );
  static const EventChannel _measurementChannel = EventChannel(
    'qring_sdk_flutter/measurement',
  );
  static const EventChannel _serviceStateChannel = EventChannel(
    'qring_sdk_flutter/service_state',
  );

  // New event channels for BLE Connection Manager events
  static const EventChannel _bleConnectionStateChannel = EventChannel(
    'qring_sdk_flutter/ble_connection_state',
  );
  static const EventChannel _bleBatteryChannel = EventChannel(
    'qring_sdk_flutter/ble_battery',
  );
  static const EventChannel _bleErrorChannel = EventChannel(
    'qring_sdk_flutter/ble_error',
  );

  /// Start BLE scanning for QC Ring devices.
  ///
  /// Initiates a Bluetooth Low Energy scan to discover nearby QC Ring devices.
  /// Discovered devices will be emitted through [devicesStream] as they are found.
  ///
  /// ## Requirements
  ///
  /// - Bluetooth must be enabled on the device
  /// - Required permissions must be granted (see [checkPermissions])
  /// - Location services must be enabled (Android requirement for BLE scanning)
  ///
  /// ## Behavior
  ///
  /// - Scanning continues until [stopScan] is called
  /// - Multiple calls to startScan while scanning is active are handled gracefully
  /// - Devices may appear multiple times in the stream with updated RSSI values
  ///
  /// Throws [Exception] if scanning fails to start (e.g., Bluetooth disabled,
  /// permissions denied, or BLE not available).
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   await QringSdkFlutter.startScan();
  ///   print('Scanning started');
  /// } catch (e) {
  ///   print('Failed to start scan: $e');
  /// }
  /// ```
  ///
  /// See also:
  /// - [stopScan] to stop scanning
  /// - [devicesStream] to receive discovered devices
  /// - [checkPermissions] to verify required permissions
  static Future<void> startScan() async {
    try {
      await _channel.invokeMethod('scan');
    } on PlatformException catch (e) {
      throw Exception('Failed to start scan: ${e.message}');
    }
  }

  /// Stop BLE scanning.
  ///
  /// Terminates the active Bluetooth Low Energy scan for QC Ring devices.
  /// After calling this method, no new devices will be discovered and emitted
  /// through [devicesStream].
  ///
  /// Safe to call even if no scan is currently active.
  ///
  /// Throws [Exception] if the operation fails.
  ///
  /// Example:
  /// ```dart
  /// await QringSdkFlutter.stopScan();
  /// print('Scanning stopped');
  /// ```
  ///
  /// See also:
  /// - [startScan] to start scanning
  static Future<void> stopScan() async {
    try {
      await _channel.invokeMethod('stopScan');
    } on PlatformException catch (e) {
      throw Exception('Failed to stop scan: ${e.message}');
    }
  }

  /// Connect to a QC Ring device by MAC address.
  ///
  /// Establishes a Bluetooth Low Energy connection to the specified device.
  /// Connection state changes will be emitted through [connectionStateStream].
  ///
  /// Parameters:
  /// - [macAddress]: The MAC address of the device to connect to (format: "AA:BB:CC:DD:EE:FF")
  ///
  /// ## Requirements
  ///
  /// - Device must be discoverable and within range
  /// - Bluetooth must be enabled
  /// - Required permissions must be granted
  ///
  /// ## Behavior
  ///
  /// - Connection attempt may take several seconds
  /// - If already connected to a device, that connection will be terminated first
  /// - Connection state changes are emitted through [connectionStateStream]
  /// - On successful connection, [ConnectionState.connected] will be emitted
  ///
  /// Throws [ArgumentError] if [macAddress] is empty.
  /// Throws [Exception] if the connection fails.
  ///
  /// Example:
  /// ```dart
  /// // Listen for connection state changes
  /// QringSdkFlutter.connectionStateStream.listen((state) {
  ///   if (state == ConnectionState.connected) {
  ///     print('Connected successfully!');
  ///   }
  /// });
  ///
  /// // Connect to device
  /// try {
  ///   await QringSdkFlutter.connect('AA:BB:CC:DD:EE:FF');
  /// } catch (e) {
  ///   print('Connection failed: $e');
  /// }
  /// ```
  ///
  /// See also:
  /// - [disconnect] to disconnect from the device
  /// - [connectionStateStream] to monitor connection status
  static Future<void> connect(String macAddress) async {
    if (macAddress.isEmpty) {
      throw ArgumentError('MAC address cannot be empty');
    }
    try {
      await _channel.invokeMethod('connect', {'mac': macAddress});
    } on PlatformException catch (e) {
      throw Exception('Failed to connect: ${e.message}');
    }
  }

  /// Disconnect from the currently connected device.
  ///
  /// Terminates the active Bluetooth connection to the QC Ring device.
  /// After disconnection, [ConnectionState.disconnected] will be emitted
  /// through [connectionStateStream].
  ///
  /// Safe to call even if no device is currently connected.
  ///
  /// Throws [Exception] if the disconnection operation fails.
  ///
  /// Example:
  /// ```dart
  /// await QringSdkFlutter.disconnect();
  /// print('Disconnected');
  /// ```
  ///
  /// See also:
  /// - [connect] to connect to a device
  /// - [connectionStateStream] to monitor connection status
  static Future<void> disconnect() async {
    try {
      await _channel.invokeMethod('disconnect');
    } on PlatformException catch (e) {
      throw Exception('Failed to disconnect: ${e.message}');
    }
  }

  /// Trigger the "Find My Ring" feature to make the ring vibrate.
  ///
  /// Sends a command to the connected QC Ring device to vibrate, helping you
  /// locate the device if it's misplaced. The ring will vibrate for a few seconds.
  ///
  /// ## Requirements
  ///
  /// - Device must be connected (check [connectionStateStream])
  /// - Device must be within Bluetooth range
  ///
  /// Throws [Exception] with message "Device is not connected" if no device is connected.
  /// Throws [Exception] if the command fails to send.
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   await QringSdkFlutter.findRing();
  ///   print('Ring is vibrating!');
  /// } catch (e) {
  ///   if (e.toString().contains('not connected')) {
  ///     print('Please connect to a device first');
  ///   } else {
  ///     print('Failed: $e');
  ///   }
  /// }
  /// ```
  ///
  /// See also:
  /// - [connect] to establish a connection
  /// - [connectionStateStream] to check connection status
  static Future<void> findRing() async {
    try {
      await _channel.invokeMethod('findRing');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to find ring: ${e.message}');
    }
  }

  /// Get the battery level of the connected device.
  ///
  /// Retrieves the current battery level as a percentage.
  ///
  /// Returns:
  /// - An integer between 0-100 representing the battery percentage if connected
  /// - -1 if the device is not connected
  ///
  /// ## Requirements
  ///
  /// - Device should be connected for accurate battery reading
  /// - If not connected, returns -1 instead of throwing an exception
  ///
  /// Throws [Exception] if the battery query fails (other than not being connected).
  ///
  /// Example:
  /// ```dart
  /// int battery = await QringSdkFlutter.getBattery();
  /// if (battery == -1) {
  ///   print('Device not connected');
  /// } else {
  ///   print('Battery: $battery%');
  ///   if (battery < 20) {
  ///     print('Low battery warning!');
  ///   }
  /// }
  /// ```
  ///
  /// See also:
  /// - [connect] to establish a connection
  /// - [getDeviceInfo] to get additional device information
  static Future<int> getBattery() async {
    try {
      final b = await _channel.invokeMethod<int>('battery');
      return b ?? -1;
    } on PlatformException catch (e) {
      throw Exception('Failed to get battery: ${e.message}');
    }
  }

  /// Get device information including firmware version, hardware version, etc.
  ///
  /// Retrieves comprehensive information about the connected QC Ring device,
  /// including version information and supported features.
  ///
  /// Returns a [QringDeviceInfo] object containing:
  /// - `firmwareVersion`: Current firmware version string
  /// - `hardwareVersion`: Hardware version string
  /// - `supportsTemperature`: Whether device supports temperature measurement
  /// - `supportsBloodOxygen`: Whether device supports SpO2 measurement
  /// - `supportsBloodPressure`: Whether device supports blood pressure measurement
  /// - `supportsHrv`: Whether device supports HRV measurement
  /// - `supportsOneKeyCheck`: Whether device supports one-key health check
  ///
  /// If the device is not connected, returns a [QringDeviceInfo] with empty/default values.
  ///
  /// Throws [Exception] if the query fails (other than not being connected).
  ///
  /// Example:
  /// ```dart
  /// QringDeviceInfo info = await QringSdkFlutter.getDeviceInfo();
  /// print('Firmware: ${info.firmwareVersion}');
  /// print('Hardware: ${info.hardwareVersion}');
  ///
  /// if (info.supportsTemperature) {
  ///   print('Device supports temperature measurement');
  /// }
  ///
  /// if (info.supportsBloodOxygen) {
  ///   print('Device supports SpO2 measurement');
  /// }
  /// ```
  ///
  /// See also:
  /// - [connect] to establish a connection
  /// - [getBattery] to get battery level
  static Future<QringDeviceInfo> getDeviceInfo() async {
    try {
      final info = await _channel.invokeMethod<Map>('deviceInfo');
      final map = Map<String, dynamic>.from(info ?? {});
      return QringDeviceInfo.fromMap(map);
    } on PlatformException catch (e) {
      throw Exception('Failed to get device info: ${e.message}');
    }
  }

  /// Perform factory reset on the connected device.
  ///
  /// Resets all device settings to factory defaults, including:
  /// - User profile information
  /// - Display settings
  /// - Continuous monitoring settings
  /// - Stored health data (may vary by device)
  ///
  /// **Warning**: This operation cannot be undone. All personalized settings
  /// will be lost.
  ///
  /// ## Requirements
  ///
  /// - Device must be connected
  /// - User should be warned before calling this method
  ///
  /// Throws [Exception] with message "Device is not connected" if no device is connected.
  /// Throws [Exception] if the factory reset operation fails.
  ///
  /// Example:
  /// ```dart
  /// // Show confirmation dialog first
  /// bool confirmed = await showConfirmationDialog(
  ///   'Are you sure you want to reset the device? This cannot be undone.',
  /// );
  ///
  /// if (confirmed) {
  ///   try {
  ///     await QringSdkFlutter.factoryReset();
  ///     print('Device reset successfully');
  ///   } catch (e) {
  ///     print('Failed to reset device: $e');
  ///   }
  /// }
  /// ```
  ///
  /// See also:
  /// - [connect] to establish a connection
  static Future<void> factoryReset() async {
    try {
      await _channel.invokeMethod('factoryReset');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to perform factory reset: ${e.message}');
    }
  }

  /// Check the status of all required permissions.
  ///
  /// Queries the current status of all permissions required by the plugin.
  /// Use this method before attempting BLE operations to ensure all necessary
  /// permissions are granted.
  ///
  /// Returns a map with permission names as keys and boolean status as values:
  ///
  /// **Android < 12:**
  /// - `bluetooth`: Legacy Bluetooth permission
  /// - `bluetoothAdmin`: Legacy Bluetooth admin permission
  /// - `locationFine`: Fine location permission (required for BLE scanning)
  /// - `locationCoarse`: Coarse location permission
  ///
  /// **Android 12+:**
  /// - `bluetoothScan`: Bluetooth scan permission
  /// - `bluetoothConnect`: Bluetooth connect permission
  /// - `bluetoothAdvertise`: Bluetooth advertise permission
  /// - `locationFine`: Fine location permission
  ///
  /// **All versions:**
  /// - `storageRead`: Read external storage permission (for firmware updates)
  /// - `storageWrite`: Write external storage permission (for firmware updates)
  ///
  /// Example:
  /// ```dart
  /// Map<String, bool> permissions = await QringSdkFlutter.checkPermissions();
  ///
  /// if (permissions['bluetoothScan'] == false) {
  ///   print('Bluetooth scan permission not granted');
  /// }
  ///
  /// if (permissions['locationFine'] == false) {
  ///   print('Location permission not granted');
  /// }
  ///
  /// bool allGranted = permissions.values.every((granted) => granted);
  /// if (allGranted) {
  ///   print('All permissions granted');
  /// }
  /// ```
  ///
  /// See also:
  /// - [requestPermissions] to request missing permissions
  static Future<Map<String, bool>> checkPermissions() async {
    try {
      final result = await _channel.invokeMethod<Map>('checkPermissions');
      final map = Map<String, dynamic>.from(result ?? {});
      return map.map((key, value) => MapEntry(key, value as bool));
    } on PlatformException catch (e) {
      throw Exception('Failed to check permissions: ${e.message}');
    }
  }

  /// Request missing permissions from the user.
  ///
  /// **Important**: Due to Flutter plugin architecture limitations, this method
  /// cannot directly trigger the system permission dialog. Instead, it returns
  /// information about missing permissions.
  ///
  /// For actual permission requests, use a dedicated permission plugin like
  /// [permission_handler](https://pub.dev/packages/permission_handler).
  ///
  /// Returns a map with:
  /// - `missingPermissions`: List of Android permission strings that are missing
  /// - `canRequest`: Boolean indicating if permissions can be requested (always false from plugin)
  ///
  /// Example:
  /// ```dart
  /// Map<String, dynamic> result = await QringSdkFlutter.requestPermissions();
  /// List<String> missing = result['missingPermissions'];
  ///
  /// if (missing.isNotEmpty) {
  ///   print('Missing permissions: $missing');
  ///   // Use permission_handler or similar plugin to request permissions
  /// }
  /// ```
  ///
  /// **Recommended approach using permission_handler:**
  /// ```dart
  /// import 'package:permission_handler/permission_handler.dart';
  ///
  /// Future<void> requestAllPermissions() async {
  ///   // Request Bluetooth permissions
  ///   await Permission.bluetoothScan.request();
  ///   await Permission.bluetoothConnect.request();
  ///
  ///   // Request location permission
  ///   await Permission.locationWhenInUse.request();
  ///
  ///   // Check if all granted
  ///   Map<String, bool> status = await QringSdkFlutter.checkPermissions();
  ///   bool allGranted = status.values.every((granted) => granted);
  ///   print('All permissions granted: $allGranted');
  /// }
  /// ```
  ///
  /// See also:
  /// - [checkPermissions] to check current permission status
  static Future<Map<String, dynamic>> requestPermissions() async {
    try {
      final result = await _channel.invokeMethod<Map>('requestPermissions');
      return Map<String, dynamic>.from(result ?? {});
    } on PlatformException catch (e) {
      throw Exception('Failed to request permissions: ${e.message}');
    }
  }

  /// Stream of connection state changes.
  ///
  /// Subscribe to this stream to receive real-time updates about the connection
  /// status with the QC Ring device.
  ///
  /// Emits [ConnectionState] values:
  /// - `ConnectionState.disconnected`: No active connection
  /// - `ConnectionState.connecting`: Connection attempt in progress
  /// - `ConnectionState.pairing`: Device pairing/bonding in progress
  /// - `ConnectionState.connected`: Successfully connected
  /// - `ConnectionState.disconnecting`: Disconnection in progress
  /// - `ConnectionState.reconnecting`: Automatic reconnection in progress
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// **Note**: The `pairing` and `reconnecting` states are new in the production-grade
  /// BLE Connection Manager and provide better visibility into the connection lifecycle.
  ///
  /// Example:
  /// ```dart
  /// StreamSubscription? subscription;
  ///
  /// void startListening() {
  ///   subscription = QringSdkFlutter.connectionStateStream.listen((state) {
  ///     switch (state) {
  ///       case ConnectionState.disconnected:
  ///         print('Disconnected');
  ///         showDisconnectedUI();
  ///         break;
  ///       case ConnectionState.connecting:
  ///         print('Connecting...');
  ///         showConnectingIndicator();
  ///         break;
  ///       case ConnectionState.pairing:
  ///         print('Pairing with device...');
  ///         showPairingIndicator();
  ///         break;
  ///       case ConnectionState.connected:
  ///         print('Connected!');
  ///         showConnectedUI();
  ///         break;
  ///       case ConnectionState.disconnecting:
  ///         print('Disconnecting...');
  ///         showDisconnectingIndicator();
  ///         break;
  ///       case ConnectionState.reconnecting:
  ///         print('Reconnecting...');
  ///         showReconnectingIndicator();
  ///         break;
  ///     }
  ///   });
  /// }
  ///
  /// void stopListening() {
  ///   subscription?.cancel();
  /// }
  /// ```
  ///
  /// See also:
  /// - [connect] to establish a connection
  /// - [disconnect] to terminate a connection
  /// - [bleConnectionStateStream] for more detailed state information
  static Stream<ConnectionState> get connectionStateStream {
    return _stateChannel.receiveBroadcastStream().map((event) {
      final stateString = event.toString();
      return ConnectionState.fromString(stateString);
    });
  }

  /// Stream of discovered devices during scanning.
  ///
  /// Subscribe to this stream to receive updates about QC Ring devices discovered
  /// during BLE scanning. The stream emits a complete list of all discovered devices
  /// each time a new device is found or an existing device's signal strength changes.
  ///
  /// Emits a list of [QringDevice] objects, each containing:
  /// - `name`: Device name (e.g., "QRing-XXXX")
  /// - `macAddress`: Device MAC address (e.g., "AA:BB:CC:DD:EE:FF")
  /// - `rssi`: Signal strength in dBm (typically -100 to 0, higher is better)
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// **Note**: The stream only emits while scanning is active (between [startScan]
  /// and [stopScan] calls).
  ///
  /// Example:
  /// ```dart
  /// StreamSubscription? subscription;
  ///
  /// Future<void> startScanning() async {
  ///   subscription = QringSdkFlutter.devicesStream.listen((devices) {
  ///     print('Found ${devices.length} devices:');
  ///     for (var device in devices) {
  ///       print('  ${device.name}');
  ///       print('    MAC: ${device.macAddress}');
  ///       print('    Signal: ${device.rssi} dBm');
  ///
  ///       // Strong signal is typically > -70 dBm
  ///       if (device.rssi > -70) {
  ///         print('    (Strong signal)');
  ///       }
  ///     }
  ///   });
  ///
  ///   await QringSdkFlutter.startScan();
  /// }
  ///
  /// Future<void> stopScanning() async {
  ///   await QringSdkFlutter.stopScan();
  ///   await subscription?.cancel();
  /// }
  /// ```
  ///
  /// See also:
  /// - [startScan] to begin scanning
  /// - [stopScan] to stop scanning
  /// - [connect] to connect to a discovered device
  static Stream<List<QringDevice>> get devicesStream {
    return _devicesChannel.receiveBroadcastStream().map((event) {
      final list = List<Map<dynamic, dynamic>>.from(event as List);
      return list.map((deviceMap) {
        final map = Map<String, dynamic>.from(deviceMap);
        return QringDevice.fromMap(map);
      }).toList();
    });
  }

  /// Stream of real-time health measurements.
  ///
  /// Subscribe to this stream to receive measurement data during manual measurements
  /// or automatic monitoring. This is a low-level stream that emits raw measurement
  /// data as maps.
  ///
  /// **Recommended**: Use [QringHealthData.measurementStream] or
  /// [QringHealthData.notificationStream] instead, which provide typed
  /// [HealthMeasurement] objects.
  ///
  /// Emits measurement data during:
  /// - Manual measurements (heart rate, blood pressure, SpO2, temperature)
  /// - Automatic measurements (when continuous monitoring is enabled)
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// Example:
  /// ```dart
  /// QringSdkFlutter.measurementStream.listen((data) {
  ///   print('Measurement type: ${data['type']}');
  ///   print('Value: ${data['value']}');
  ///   print('Success: ${data['success']}');
  /// });
  /// ```
  ///
  /// See also:
  /// - [QringHealthData.measurementStream] for typed measurement results
  /// - [QringHealthData.notificationStream] for automatic measurements
  /// - [QringHealthData.startHeartRateMeasurement] to start a measurement
  static Stream<Map<String, dynamic>> get measurementStream {
    return _measurementChannel.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event as Map);
    });
  }

  // ============================================================================
  // Background Service Methods
  // ============================================================================

  /// Start the background service to maintain continuous ring connection.
  ///
  /// Starts an Android Foreground Service that maintains a persistent connection
  /// to the QRing device, even when the Flutter app is killed. The service displays
  /// a persistent notification with quick actions like "Find My Ring".
  ///
  /// Parameters:
  /// - [deviceMac]: The MAC address of the device to connect to (format: "AA:BB:CC:DD:EE:FF")
  ///
  /// ## Requirements
  ///
  /// - All required permissions must be granted (Bluetooth, notifications)
  /// - Device should be discoverable and within range
  /// - Android 8.0+ required for foreground service
  ///
  /// ## Behavior
  ///
  /// - Creates a persistent notification showing connection status
  /// - Maintains connection even when app is killed
  /// - Automatically reconnects if device disconnects
  /// - Service continues until [stopBackgroundService] is called
  ///
  /// Throws [ArgumentError] if [deviceMac] is empty.
  /// Throws [Exception] if the service fails to start (e.g., permissions denied).
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   await QringSdkFlutter.startBackgroundService('AA:BB:CC:DD:EE:FF');
  ///   print('Background service started');
  /// } catch (e) {
  ///   print('Failed to start service: $e');
  /// }
  /// ```
  ///
  /// See also:
  /// - [stopBackgroundService] to stop the service
  /// - [isServiceRunning] to check service status
  /// - [serviceStateStream] to monitor service state
  static Future<void> startBackgroundService(String deviceMac) async {
    if (deviceMac.isEmpty) {
      throw ArgumentError('Device MAC address cannot be empty');
    }
    try {
      await _channel.invokeMethod('startBackgroundService', {
        'deviceMac': deviceMac,
      });
    } on PlatformException catch (e) {
      throw Exception('Failed to start background service: ${e.message}');
    }
  }

  /// Stop the background service.
  ///
  /// Terminates the background service, disconnects from the device, and removes
  /// the persistent notification. The service will no longer maintain connection
  /// when the app is killed.
  ///
  /// Safe to call even if the service is not currently running.
  ///
  /// Throws [Exception] if the operation fails.
  ///
  /// Example:
  /// ```dart
  /// await QringSdkFlutter.stopBackgroundService();
  /// print('Background service stopped');
  /// ```
  ///
  /// See also:
  /// - [startBackgroundService] to start the service
  /// - [isServiceRunning] to check service status
  static Future<void> stopBackgroundService() async {
    try {
      await _channel.invokeMethod('stopBackgroundService');
    } on PlatformException catch (e) {
      throw Exception('Failed to stop background service: ${e.message}');
    }
  }

  /// Check if the background service is currently running.
  ///
  /// Queries the system to determine if the QRing background service is active.
  ///
  /// Returns `true` if the service is running, `false` otherwise.
  ///
  /// Example:
  /// ```dart
  /// bool isRunning = await QringSdkFlutter.isServiceRunning();
  /// if (isRunning) {
  ///   print('Service is active');
  /// } else {
  ///   print('Service is not running');
  /// }
  /// ```
  ///
  /// See also:
  /// - [startBackgroundService] to start the service
  /// - [stopBackgroundService] to stop the service
  static Future<bool> isServiceRunning() async {
    try {
      final result = await _channel.invokeMethod<bool>('isServiceRunning');
      return result ?? false;
    } on PlatformException catch (e) {
      throw Exception('Failed to check service status: ${e.message}');
    }
  }

  /// Send a command to the background service.
  ///
  /// Sends a custom command to the running background service for execution.
  /// This method is designed for extensibility, allowing future commands to be
  /// added without modifying the core API.
  ///
  /// Parameters:
  /// - [command]: The command name (e.g., "findMyRing", "getBattery")
  /// - [params]: Optional parameters for the command
  ///
  /// Returns a map containing the command result. The structure depends on the
  /// specific command executed.
  ///
  /// ## Requirements
  ///
  /// - Background service must be running
  /// - Device should be connected for most commands
  ///
  /// Throws [ArgumentError] if [command] is empty.
  /// Throws [Exception] if the service is not running or command execution fails.
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   // Send Find My Ring command
  ///   Map<String, dynamic> result = await QringSdkFlutter.sendRingCommand(
  ///     'findMyRing',
  ///     {},
  ///   );
  ///
  ///   if (result['success'] == true) {
  ///     print('Ring activated');
  ///   } else {
  ///     print('Failed: ${result['error']}');
  ///   }
  /// } catch (e) {
  ///   print('Command failed: $e');
  /// }
  /// ```
  ///
  /// See also:
  /// - [startBackgroundService] to start the service
  /// - [isServiceRunning] to check if service is active
  static Future<Map<String, dynamic>> sendRingCommand(
    String command,
    Map<String, dynamic> params,
  ) async {
    if (command.isEmpty) {
      throw ArgumentError('Command cannot be empty');
    }
    try {
      final result = await _channel.invokeMethod<Map>('sendRingCommand', {
        'command': command,
        'params': params,
      });
      return Map<String, dynamic>.from(result ?? {});
    } on PlatformException catch (e) {
      throw Exception('Failed to send command: ${e.message}');
    }
  }

  /// Stream of background service state changes.
  ///
  /// Subscribe to this stream to receive real-time updates about the background
  /// service status and device connection state maintained by the service.
  ///
  /// Emits [ServiceState] objects containing:
  /// - `isRunning`: Whether the background service is active
  /// - `isConnected`: Whether the device is connected
  /// - `deviceMac`: MAC address of the connected/target device
  /// - `deviceName`: Name of the connected device
  /// - `reconnectAttempts`: Number of consecutive reconnection attempts
  /// - `lastConnectedTime`: Timestamp of last successful connection
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// ## Events
  ///
  /// The stream emits events when:
  /// - Service starts or stops
  /// - Device connects or disconnects
  /// - Reconnection attempts occur
  /// - Service state changes
  ///
  /// Example:
  /// ```dart
  /// StreamSubscription? subscription;
  ///
  /// void startListening() {
  ///   subscription = QringSdkFlutter.serviceStateStream.listen((state) {
  ///     print('Service running: ${state.isRunning}');
  ///     print('Device connected: ${state.isConnected}');
  ///
  ///     if (state.isConnected) {
  ///       print('Connected to: ${state.deviceName ?? state.deviceMac}');
  ///     } else if (state.reconnectAttempts > 0) {
  ///       print('Reconnecting... (attempt ${state.reconnectAttempts})');
  ///     }
  ///   });
  /// }
  ///
  /// void stopListening() {
  ///   subscription?.cancel();
  /// }
  /// ```
  ///
  /// See also:
  /// - [startBackgroundService] to start the service
  /// - [stopBackgroundService] to stop the service
  /// - [isServiceRunning] to check current service status
  static Stream<ServiceState> get serviceStateStream {
    return _serviceStateChannel.receiveBroadcastStream().map((event) {
      final map = Map<String, dynamic>.from(event as Map);
      return ServiceState.fromMap(map);
    });
  }

  // ============================================================================
  // BLE Connection Manager Event Streams
  // ============================================================================

  /// Stream of BLE connection state changes from the BLE Connection Manager.
  ///
  /// Subscribe to this stream to receive real-time updates about BLE connection
  /// state changes, including connected, disconnected, and reconnecting events.
  ///
  /// Emits a map containing:
  /// - `oldState`: The previous BLE state (string)
  /// - `newState`: The new BLE state (string)
  /// - `timestamp`: Timestamp of the state change (milliseconds since epoch)
  /// - `deviceMac`: MAC address of the device (if available)
  /// - `deviceName`: Name of the device (if available)
  /// - `errorCode`: Error code (if transitioning to error state)
  /// - `errorMessage`: Error message (if transitioning to error state)
  ///
  /// Possible state values:
  /// - `idle`: No operation in progress
  /// - `scanning`: Actively scanning for devices
  /// - `connecting`: Connection attempt in progress
  /// - `pairing`: Bonding/pairing in progress
  /// - `connected`: Successfully connected and services discovered
  /// - `disconnected`: Disconnected (not attempting reconnect)
  /// - `reconnecting`: Auto-reconnect in progress
  /// - `error`: Error state with details
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// Example:
  /// ```dart
  /// StreamSubscription? subscription;
  ///
  /// void startListening() {
  ///   subscription = QringSdkFlutter.bleConnectionStateStream.listen((event) {
  ///     String oldState = event['oldState'];
  ///     String newState = event['newState'];
  ///
  ///     print('BLE state changed: $oldState -> $newState');
  ///
  ///     if (newState == 'connected') {
  ///       print('Device connected: ${event['deviceName']}');
  ///     } else if (newState == 'reconnecting') {
  ///       print('Attempting to reconnect...');
  ///     } else if (newState == 'error') {
  ///       print('Error: ${event['errorMessage']}');
  ///     }
  ///   });
  /// }
  ///
  /// void stopListening() {
  ///   subscription?.cancel();
  /// }
  /// ```
  ///
  /// See also:
  /// - [connectRing] to establish a connection
  /// - [disconnectRing] to terminate a connection
  /// - [getConnectionState] to get current state
  static Stream<Map<String, dynamic>> get bleConnectionStateStream {
    return _bleConnectionStateChannel.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event as Map);
    });
  }

  /// Stream of battery level updates from the connected device.
  ///
  /// Subscribe to this stream to receive real-time battery level updates
  /// from the connected QRing device.
  ///
  /// Emits a map containing:
  /// - `batteryLevel`: Battery level as a percentage (0-100)
  /// - `timestamp`: Timestamp of the update (milliseconds since epoch)
  /// - `deviceMac`: MAC address of the device (if available)
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// Example:
  /// ```dart
  /// StreamSubscription? subscription;
  ///
  /// void startListening() {
  ///   subscription = QringSdkFlutter.bleBatteryStream.listen((event) {
  ///     int batteryLevel = event['batteryLevel'];
  ///     print('Battery: $batteryLevel%');
  ///
  ///     if (batteryLevel < 20) {
  ///       print('Low battery warning!');
  ///     }
  ///   });
  /// }
  ///
  /// void stopListening() {
  ///   subscription?.cancel();
  /// }
  /// ```
  ///
  /// See also:
  /// - [getBattery] to query battery level on demand
  static Stream<Map<String, dynamic>> get bleBatteryStream {
    return _bleBatteryChannel.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event as Map);
    });
  }

  /// Stream of BLE error events.
  ///
  /// Subscribe to this stream to receive error notifications from the
  /// BLE Connection Manager when operations fail.
  ///
  /// Emits a map containing:
  /// - `errorCode`: Error code identifying the type of error
  /// - `errorMessage`: Human-readable error message
  /// - `timestamp`: Timestamp of the error (milliseconds since epoch)
  /// - `deviceMac`: MAC address of the device (if available)
  ///
  /// Common error codes:
  /// - `PERMISSION_DENIED`: Required Bluetooth permission not granted
  /// - `BLUETOOTH_OFF`: Bluetooth is disabled
  /// - `PAIRING_FAILED`: Device pairing/bonding failed
  /// - `CONNECTION_FAILED`: Connection attempt failed
  /// - `CONNECTION_TIMEOUT`: Connection attempt timed out
  /// - `GATT_ERROR`: GATT operation failed
  /// - `COMMAND_FAILED`: Device command execution failed
  /// - `RECONNECTION_FAILED`: Auto-reconnection failed
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// **Note**: For typed error handling, use [errorStream] instead, which
  /// provides [BleError] objects with categorized error types.
  ///
  /// Example:
  /// ```dart
  /// StreamSubscription? subscription;
  ///
  /// void startListening() {
  ///   subscription = QringSdkFlutter.bleErrorStream.listen((event) {
  ///     String errorCode = event['errorCode'];
  ///     String errorMessage = event['errorMessage'];
  ///
  ///     print('BLE Error [$errorCode]: $errorMessage');
  ///
  ///     // Handle specific errors
  ///     if (errorCode == 'PERMISSION_DENIED') {
  ///       print('Please grant Bluetooth permissions');
  ///     } else if (errorCode == 'BLUETOOTH_OFF') {
  ///       print('Please enable Bluetooth');
  ///     } else if (errorCode == 'PAIRING_FAILED') {
  ///       print('Failed to pair with device');
  ///     }
  ///   });
  /// }
  ///
  /// void stopListening() {
  ///   subscription?.cancel();
  /// }
  /// ```
  ///
  /// See also:
  /// - [errorStream] for typed error handling with [BleError] objects
  /// - [bleConnectionStateStream] for connection state changes
  /// - [connectRing] to establish a connection
  static Stream<Map<String, dynamic>> get bleErrorStream {
    return _bleErrorChannel.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event as Map);
    });
  }

  /// Stream of typed BLE error events.
  ///
  /// Subscribe to this stream to receive typed error notifications from the
  /// BLE Connection Manager when operations fail. This is the recommended way
  /// to handle errors as it provides [BleError] objects with categorized error types.
  ///
  /// Emits [BleError] objects containing:
  /// - `code`: Error code identifying the specific error
  /// - `message`: Human-readable error message
  /// - `type`: Categorized error type for easier handling
  /// - `timestamp`: When the error occurred
  /// - `deviceMac`: MAC address of the device (if available)
  ///
  /// The stream is a broadcast stream, so multiple listeners can subscribe.
  ///
  /// Example:
  /// ```dart
  /// StreamSubscription<BleError>? subscription;
  ///
  /// void startListening() {
  ///   subscription = QringSdkFlutter.errorStream.listen((error) {
  ///     print('BLE Error [${error.code}]: ${error.message}');
  ///
  ///     // Handle errors by type
  ///     switch (error.type) {
  ///       case BleErrorType.permissionDenied:
  ///         showPermissionDialog();
  ///         break;
  ///       case BleErrorType.bluetoothOff:
  ///         showBluetoothEnableDialog();
  ///         break;
  ///       case BleErrorType.pairingFailed:
  ///         showPairingErrorDialog();
  ///         break;
  ///       case BleErrorType.connectionFailed:
  ///         // Auto-reconnect will handle this
  ///         showReconnectingIndicator();
  ///         break;
  ///       case BleErrorType.gattError:
  ///         showCommunicationErrorDialog();
  ///         break;
  ///       default:
  ///         showGenericErrorDialog(error.message);
  ///     }
  ///   });
  /// }
  ///
  /// void stopListening() {
  ///   subscription?.cancel();
  /// }
  /// ```
  ///
  /// See also:
  /// - [bleErrorStream] for raw error event maps
  /// - [BleError] for error object structure
  /// - [BleErrorType] for error type categories
  /// - [bleConnectionStateStream] for connection state changes
  static Stream<BleError> get errorStream {
    return bleErrorStream.map((event) => BleError.fromMap(event));
  }
}
