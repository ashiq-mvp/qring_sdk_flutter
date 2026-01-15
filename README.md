# QRing SDK Flutter Plugin

A production-grade Flutter plugin for integrating QC Wireless smart ring devices into your Flutter applications. This plugin provides a complete Dart API that wraps the native Android QC Wireless SDK, enabling seamless communication with QC smart rings via Bluetooth Low Energy (BLE).

Built with reliability in mind, the plugin features a robust background service architecture that maintains connections even when your app is closed, automatic reconnection with exponential backoff, and comprehensive error handling.

[![pub package](https://img.shields.io/pub/v/qring_sdk_flutter.svg)](https://pub.dev/packages/qring_sdk_flutter)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Features

### Core Features
- 🔍 **Device Discovery**: Scan for and discover nearby QC smart ring devices
- 🔗 **Production-Grade Connection Management**: Reliable pairing, automatic reconnection, and background service support
- 📍 **Find My Ring**: Trigger vibration to locate misplaced rings (works from notification)
- 🔋 **Battery Monitoring**: Real-time battery level tracking with notifications
- 📊 **Health Data Sync**: Retrieve historical health data (steps, heart rate, sleep, SpO2, blood pressure)
- 💓 **Manual Measurements**: Trigger on-demand health measurements
- ⚙️ **Device Settings**: Configure display, continuous monitoring, and user profiles
- 🏃 **Exercise Tracking**: Track workouts with 20+ exercise types
- 🔄 **Firmware Updates**: Update device firmware via DFU
- 🔔 **Real-time Notifications**: Receive automatic measurement notifications

### Production-Grade Reliability
- 🔄 **Automatic Reconnection**: Exponential backoff strategy with intelligent retry logic
- 🔋 **Background Service**: Maintains connection even when app is closed or killed
- 🔐 **Reliable Pairing**: Robust bonding workflow with automatic retry
- 📱 **Persistent Notification**: Always-visible connection status with quick actions
- 🔌 **System Integration**: Survives device reboots and Bluetooth toggles
- ⚡ **Permission Management**: Comprehensive Android 12+ permission handling
- 🛡️ **Error Recovery**: Graceful error handling with detailed error reporting

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Installation](#installation)
- [Required Permissions](#required-permissions)
  - [Android 12+ Permissions](#android-12-permissions)
- [Background Service](#background-service)
- [Auto-Reconnect Strategy](#auto-reconnect-strategy)
- [Quick Start](#quick-start)
- [Usage Examples](#usage-examples)
  - [Device Scanning and Connection](#device-scanning-and-connection)
  - [Find My Ring](#find-my-ring)
  - [Battery and Device Info](#battery-and-device-info)
  - [Health Data Synchronization](#health-data-synchronization)
  - [Manual Measurements](#manual-measurements)
  - [Continuous Monitoring](#continuous-monitoring)
  - [Display Settings](#display-settings)
  - [Exercise Tracking](#exercise-tracking)
  - [Firmware Updates](#firmware-updates)
- [Error Handling](#error-handling)
- [Error Codes Reference](#error-codes-reference)
- [Troubleshooting](#troubleshooting)
- [Migration Guide](#migration-guide)
- [Example App](#example-app)
- [Platform Support](#platform-support)
- [License](#license)

## Architecture Overview

The QRing SDK Flutter plugin is built on a production-grade architecture designed for reliability and stability, similar to professional fitness band applications (Fitbit, Oura, etc.).

### Key Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Flutter App Layer                        │
│  (UI, Business Logic, State Management)                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Flutter Bridge / Plugin                    │
│  (Simplified API, Event Streams, Error Handling)            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              BLE Connection Manager (State Machine)          │
│  • Centralized state management (IDLE, CONNECTING, etc.)   │
│  • Permission validation                                     │
│  • Pairing/bonding workflow                                 │
│  • GATT lifecycle management                                │
└────────┬────────────────┬────────────────┬─────────────────┘
         │                │                │
         ▼                ▼                ▼
┌────────────────┐ ┌──────────────┐ ┌─────────────────────┐
│ Auto-Reconnect │ │   Pairing    │ │  GATT Connection    │
│     Engine     │ │   Manager    │ │      Manager        │
│ • Exponential  │ │ • Bond state │ │ • Service discovery │
│   backoff      │ │   checking   │ │ • MTU negotiation   │
│ • Bluetooth    │ │ • Retry logic│ │ • Proper cleanup    │
│   monitoring   │ │              │ │                     │
└────────────────┘ └──────────────┘ └─────────────────────┘
         │                │                │
         └────────────────┴────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  Foreground Service                          │
│  • START_STICKY lifecycle                                   │
│  • Persistent notification                                   │
│  • Boot & Bluetooth restart                                 │
│  • Independent of app lifecycle                             │
└─────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Single Source of Truth**: The BLE Connection Manager is the only component that manages BLE state
2. **State Machine**: All operations validate current state before proceeding
3. **Background Reliability**: Foreground service maintains connection independently of app lifecycle
4. **Automatic Recovery**: Exponential backoff reconnection handles temporary disconnections
5. **Permission Safety**: All operations check permissions before proceeding
6. **Error Transparency**: Detailed error reporting with specific error codes

### Connection States

The plugin uses a well-defined state machine:

- **IDLE**: No operation in progress
- **SCANNING**: Actively scanning for devices
- **CONNECTING**: Connection attempt in progress
- **PAIRING**: Bonding/pairing in progress
- **CONNECTED**: Successfully connected with services discovered
- **DISCONNECTED**: Disconnected (not attempting reconnect)
- **RECONNECTING**: Auto-reconnect in progress
- **ERROR**: Error state with details

State transitions are validated to prevent invalid operations.

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  qring_sdk_flutter: ^0.0.1
```

Then run:

```bash
flutter pub get
```

### Android Setup

1. **Minimum SDK Version**: Ensure your `android/app/build.gradle` has minimum SDK version 23:

```gradle
android {
    defaultConfig {
        minSdkVersion 23
        // ...
    }
}
```

2. **QC Wireless SDK**: The plugin includes the QC Wireless SDK (qring_sdk_1.0.0.4.aar) in `android/libs/`. No additional setup is required.

3. **Permissions**: Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Bluetooth permissions for Android < 12 -->
    <uses-permission android:name="android.permission.BLUETOOTH" 
        android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" 
        android:maxSdkVersion="30" />
    
    <!-- Bluetooth permissions for Android 12+ (API 31+) -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    
    <!-- Location permissions (required for BLE scanning on Android < 12) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <!-- Notification permission for Android 13+ (API 33+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Foreground service permission -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
    
    <!-- Boot receiver permission (for service restart) -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    
    <!-- Storage permissions (for firmware updates) -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
        android:maxSdkVersion="28" />
    
    <!-- Feature declarations -->
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
    
    <application>
        <!-- Your app configuration -->
    </application>
</manifest>
```

**Important**: The plugin automatically registers the required service and broadcast receivers. You don't need to add them manually to your manifest.

### iOS Setup

**Note**: iOS support is not yet implemented. This plugin currently supports Android only.

## Required Permissions

The plugin requires different permissions depending on the Android version. The permission system was significantly updated in Android 12 (API 31) and Android 13 (API 33).

### Android 12+ Permissions

Starting with Android 12, Google introduced new runtime permissions specifically for Bluetooth operations:

#### BLUETOOTH_SCAN (Android 12+)
- **Purpose**: Required to scan for BLE devices
- **Runtime**: Yes (must be requested at runtime)
- **When needed**: Before calling `startScan()`
- **Note**: Use `neverForLocation` flag if you don't need location data

#### BLUETOOTH_CONNECT (Android 12+)
- **Purpose**: Required to connect to BLE devices
- **Runtime**: Yes (must be requested at runtime)
- **When needed**: Before calling `connect()` or any device operations
- **Note**: Required for all device interactions

#### POST_NOTIFICATIONS (Android 13+)
- **Purpose**: Required to show foreground service notification
- **Runtime**: Yes (must be requested at runtime)
- **When needed**: Before starting background service
- **Note**: Critical for background service operation

### Legacy Permissions (Android < 12)

For devices running Android 11 and below:

#### ACCESS_FINE_LOCATION
- **Purpose**: Required for BLE scanning (Android requirement)
- **Runtime**: Yes (must be requested at runtime)
- **When needed**: Before calling `startScan()`
- **Note**: Location services must also be enabled

#### BLUETOOTH & BLUETOOTH_ADMIN
- **Purpose**: Basic Bluetooth operations
- **Runtime**: No (install-time only)
- **When needed**: Always
- **Note**: Automatically granted on install

### Permission Matrix

| Permission | Android < 12 | Android 12+ | Android 13+ | Runtime |
|------------|--------------|-------------|-------------|---------|
| `BLUETOOTH` | ✅ Required | ❌ Deprecated | ❌ Deprecated | No |
| `BLUETOOTH_ADMIN` | ✅ Required | ❌ Deprecated | ❌ Deprecated | No |
| `ACCESS_FINE_LOCATION` | ✅ Required | ⚠️ Optional* | ⚠️ Optional* | Yes |
| `BLUETOOTH_SCAN` | ❌ N/A | ✅ Required | ✅ Required | Yes |
| `BLUETOOTH_CONNECT` | ❌ N/A | ✅ Required | ✅ Required | Yes |
| `POST_NOTIFICATIONS` | ❌ N/A | ❌ N/A | ✅ Required | Yes |
| `FOREGROUND_SERVICE` | ✅ Required | ✅ Required | ✅ Required | No |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | ❌ N/A | ❌ N/A | ✅ Required | No |
| `RECEIVE_BOOT_COMPLETED` | ✅ Required | ✅ Required | ✅ Required | No |

*Optional on Android 12+ if using `neverForLocation` flag with `BLUETOOTH_SCAN`

### Requesting Permissions

The plugin provides methods to check and request permissions:

```dart
// Check all permission status
Map<String, bool> permissions = await QringSdkFlutter.checkPermissions();

// Check specific permissions
if (!permissions['bluetoothScan']!) {
  print('Bluetooth scan permission not granted');
}
if (!permissions['bluetoothConnect']!) {
  print('Bluetooth connect permission not granted');
}
if (!permissions['notification']!) {
  print('Notification permission not granted (Android 13+)');
}

// Request missing permissions
Map<String, dynamic> result = await QringSdkFlutter.requestPermissions();
List<String> missing = result['missingPermissions'];

if (missing.isNotEmpty) {
  print('Still missing permissions: $missing');
  // Guide user to app settings
}
```

### Best Practices

1. **Request Early**: Request permissions before attempting any BLE operations
2. **Explain Why**: Show rationale before requesting permissions
3. **Handle Denial**: Gracefully handle permission denials with clear messaging
4. **Settings Redirect**: If permanently denied, guide users to app settings
5. **Check Before Operations**: Always verify permissions before BLE operations

### Permission Errors

The plugin will return specific error codes when permissions are missing:

- `PERMISSION_DENIED`: General permission denial
- `BLUETOOTH_PERMISSION_REQUIRED`: Bluetooth permission needed
- `LOCATION_PERMISSION_REQUIRED`: Location permission needed (Android < 12)
- `NOTIFICATION_PERMISSION_REQUIRED`: Notification permission needed (Android 13+)

### Recommended Permission Plugin

For comprehensive permission management, use the [permission_handler](https://pub.dev/packages/permission_handler) plugin:

```dart
import 'package:permission_handler/permission_handler.dart';

Future<bool> requestAllPermissions() async {
  Map<Permission, PermissionStatus> statuses = await [
    Permission.bluetoothScan,
    Permission.bluetoothConnect,
    Permission.location,
    Permission.notification,
  ].request();
  
  return statuses.values.every((status) => status.isGranted);
}
```

## Background Service

The QRing SDK uses a foreground service to maintain reliable BLE connections even when your app is closed or the device is locked. This is essential for continuous health monitoring and automatic reconnection.

### How It Works

1. **Foreground Service**: Runs with a persistent notification (required by Android)
2. **START_STICKY**: Service automatically restarts if killed by the system
3. **Boot Receiver**: Service restarts after device reboot (if ring was connected)
4. **Bluetooth Receiver**: Service restarts when Bluetooth is turned on (if ring was connected)
5. **Independent Lifecycle**: Service runs independently of your Flutter app

### Service Lifecycle

```
App Start → Connect to Ring → Service Starts → Persistent Notification
                                      ↓
                              Service Runs in Background
                                      ↓
                    ┌─────────────────┴─────────────────┐
                    ↓                                   ↓
            App Closed/Killed                   Device Reboot
                    ↓                                   ↓
            Service Continues                   Service Restarts
                    ↓                                   ↓
            Connection Maintained              Auto-Reconnect to Ring
```

### Persistent Notification

While the service is running, a persistent notification is displayed showing:

- **Device Name**: Name of the connected ring
- **Connection State**: Connected, Connecting, Reconnecting, or Disconnected
- **Battery Level**: Current battery percentage (when available)
- **Quick Action**: Tap to trigger "Find My Ring"

The notification updates in real-time as connection state or battery level changes.

### Service Behavior

**When App is Closed:**
- Service continues running
- Connection is maintained
- Automatic reconnection works
- Notification remains visible

**When Device is Rebooted:**
- Service automatically restarts (if ring was previously connected)
- Attempts to reconnect to last known ring
- Notification reappears

**When Bluetooth is Toggled:**
- Service pauses reconnection when Bluetooth is OFF
- Service resumes reconnection when Bluetooth is ON
- Connection is automatically restored

**When Manually Disconnected:**
- Service stops
- Notification disappears
- Auto-reconnect is disabled
- Saved device info is cleared

### Battery Optimization

The service is designed to be battery-efficient:

- Uses BLE (low energy) protocol
- Minimal wake locks (only during active operations)
- Efficient exponential backoff for reconnection
- No unnecessary background processing

### Controlling the Service

The service is automatically managed by the plugin:

```dart
// Connect to ring - service starts automatically
await QringSdkFlutter.connect(macAddress);
// Service is now running with notification

// Disconnect from ring - service stops automatically
await QringSdkFlutter.disconnect();
// Service stops, notification disappears
```

You don't need to manually start or stop the service.

## Auto-Reconnect Strategy

The plugin implements an intelligent auto-reconnect strategy to handle temporary disconnections (out of range, interference, etc.) without user intervention.

### When Auto-Reconnect Activates

Auto-reconnect is triggered when:
- Connection is lost unexpectedly (not manual disconnect)
- Device goes out of range and returns
- Bluetooth is toggled off then on
- Temporary interference causes disconnection
- Device reboots (if ring was previously connected)

Auto-reconnect is **NOT** triggered when:
- User manually disconnects
- User explicitly stops the service
- Permissions are revoked

### Exponential Backoff Algorithm

The plugin uses exponential backoff with jitter to prevent battery drain and network congestion:

```
Attempt 1-5:   10 seconds  (±20% jitter) = 8-12 seconds
Attempt 6-10:  30 seconds  (±20% jitter) = 24-36 seconds
Attempt 11-15: 60 seconds  (±20% jitter) = 48-72 seconds
Attempt 16-20: 120 seconds (±20% jitter) = 96-144 seconds
Attempt 21+:   300 seconds (±20% jitter) = 240-360 seconds (max)
```

**Jitter**: Random variation (±20%) prevents multiple devices from reconnecting simultaneously

**Max Delay**: Capped at 5 minutes to ensure eventual reconnection

### Reconnection Behavior

**During Reconnection:**
- Connection state changes to `RECONNECTING`
- Notification shows "Reconnecting... (attempt X)"
- Flutter app receives `onBleReconnecting` events
- Background service continues running

**On Successful Reconnection:**
- Full GATT setup is performed (service discovery, MTU negotiation)
- Connection state changes to `CONNECTED`
- Notification updates to show "Connected"
- Flutter app receives `onBleConnected` event
- Attempt counter resets

**On Continued Failure:**
- Exponential backoff increases delay
- Service continues trying indefinitely
- User can manually disconnect to stop attempts

### Bluetooth State Monitoring

The plugin monitors Bluetooth state and adjusts reconnection accordingly:

**When Bluetooth is Turned OFF:**
- Reconnection attempts pause
- Service remains running
- Notification shows "Bluetooth Off"

**When Bluetooth is Turned ON:**
- Reconnection resumes immediately
- No delay for first attempt after Bluetooth ON
- Normal exponential backoff applies to subsequent attempts

### Device Persistence

The plugin persists the last connected device to enable reconnection after app restart or device reboot:

```dart
// Automatically saved on successful connection
await QringSdkFlutter.connect(macAddress);
// Device MAC and name are saved to SharedPreferences

// Automatically loaded on app restart
// Service will attempt to reconnect to saved device

// Automatically cleared on manual disconnect
await QringSdkFlutter.disconnect();
// Saved device info is cleared
```

### Monitoring Reconnection

You can monitor reconnection attempts in your Flutter app:

```dart
// Listen for reconnection events
QringSdkFlutter.connectionStateStream.listen((state) {
  if (state == ConnectionState.reconnecting) {
    print('Attempting to reconnect...');
    // Show reconnecting UI
  } else if (state == ConnectionState.connected) {
    print('Reconnected successfully!');
    // Show connected UI
  }
});

// Listen for errors during reconnection
QringSdkFlutter.errorStream.listen((error) {
  if (error.type == ErrorType.connectionFailed) {
    print('Reconnection attempt failed: ${error.message}');
    // Optionally show error to user
  }
});
```

### Disabling Auto-Reconnect

Auto-reconnect is automatically disabled when you manually disconnect:

```dart
// Manual disconnect disables auto-reconnect
await QringSdkFlutter.disconnect();
// Service stops, auto-reconnect disabled, saved device cleared
```

To reconnect, simply call `connect()` again:

```dart
// Re-enable connection and auto-reconnect
await QringSdkFlutter.connect(macAddress);
// Service starts, auto-reconnect enabled
```

## Quick Start

Here's a minimal example to get you started:

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

// 1. Start scanning for devices
await QringSdkFlutter.startScan();

// 2. Listen for discovered devices
QringSdkFlutter.devicesStream.listen((devices) {
  for (var device in devices) {
    print('Found: ${device.name} (${device.macAddress})');
  }
});

// 3. Connect to a device
await QringSdkFlutter.connect('AA:BB:CC:DD:EE:FF');

// 4. Monitor connection state
QringSdkFlutter.connectionStateStream.listen((state) {
  print('Connection state: $state');
});

// 5. Use device features
await QringSdkFlutter.findRing();
int battery = await QringSdkFlutter.getBattery();
print('Battery: $battery%');
```

## Usage Examples

### Device Scanning and Connection

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

class DeviceScanner {
  StreamSubscription? _devicesSubscription;
  StreamSubscription? _stateSubscription;
  
  Future<void> startScanning() async {
    try {
      // Listen for discovered devices
      _devicesSubscription = QringSdkFlutter.devicesStream.listen((devices) {
        print('Discovered ${devices.length} devices');
        for (var device in devices) {
          print('  ${device.name} - ${device.macAddress} (RSSI: ${device.rssi})');
        }
      });
      
      // Start scanning
      await QringSdkFlutter.startScan();
      print('Scanning started');
    } catch (e) {
      print('Failed to start scanning: $e');
    }
  }
  
  Future<void> stopScanning() async {
    try {
      await QringSdkFlutter.stopScan();
      await _devicesSubscription?.cancel();
      print('Scanning stopped');
    } catch (e) {
      print('Failed to stop scanning: $e');
    }
  }
  
  Future<void> connectToDevice(String macAddress) async {
    try {
      // Monitor connection state
      _stateSubscription = QringSdkFlutter.connectionStateStream.listen((state) {
        print('Connection state: $state');
        if (state == ConnectionState.connected) {
          print('Successfully connected!');
        }
      });
      
      // Connect to device
      await QringSdkFlutter.connect(macAddress);
    } catch (e) {
      print('Failed to connect: $e');
    }
  }
  
  Future<void> disconnect() async {
    try {
      await QringSdkFlutter.disconnect();
      await _stateSubscription?.cancel();
    } catch (e) {
      print('Failed to disconnect: $e');
    }
  }
  
  void dispose() {
    _devicesSubscription?.cancel();
    _stateSubscription?.cancel();
  }
}
```

### SDK-Driven Scan Filtering

The plugin implements intelligent BLE scan filtering at the native Android layer to ensure only QRing-compatible devices appear in scan results. This eliminates the need for manual filtering in your Flutter code and guarantees that every device you see can be successfully connected.

#### How It Works

**Native Layer Filtering:**
- All BLE advertisement data is analyzed in the Android native layer
- SDK validation rules are applied before devices reach Flutter
- Only QRing-compatible devices are emitted to the device stream
- No additional filtering needed in your Dart code

**Validation Rules:**

The plugin uses the following criteria to identify QRing devices:

1. **Device Name Prefixes** (Primary Filter):
   - Devices with names starting with `O_`, `Q_`, or `R` are recognized as QRing devices
   - Based on official QC Wireless SDK sample code
   - Devices with null/empty names are allowed if they pass other checks

2. **RSSI Threshold**:
   - Minimum signal strength: -100 dBm
   - Filters out devices with extremely weak signals
   - Ensures reliable connection quality

3. **MAC Address Validation**:
   - All devices must have a valid MAC address
   - Used for device deduplication

4. **Automatic Deduplication**:
   - Each device appears only once in the scan list
   - Duplicate advertisements are merged based on MAC address
   - RSSI values are updated when devices are rediscovered

#### What You Get

**Guaranteed Compatibility:**
```dart
// Every device in the stream is QRing-compatible
QringSdkFlutter.devicesStream.listen((devices) {
  for (var device in devices) {
    // This device is guaranteed to be connectable
    // No need to check device name or apply filters
    print('QRing device: ${device.name} (${device.macAddress})');
  }
});
```

**Clean Device List:**
- No duplicate devices
- No non-QRing devices
- No devices with weak signals
- Automatically updated RSSI values

**Simplified Flutter Code:**
```dart
// Old approach (manual filtering - NOT NEEDED)
// ❌ Don't do this anymore
QringSdkFlutter.devicesStream.listen((devices) {
  final qringDevices = devices.where((d) => 
    d.name.toLowerCase().startsWith('r') ||
    d.name.toLowerCase().startsWith('q')
  ).toList();
});

// New approach (automatic filtering)
// ✅ Just use the devices directly
QringSdkFlutter.devicesStream.listen((devices) {
  // All devices are already filtered
  displayDevices(devices);
});
```

#### Device Information

Each discovered device includes:

- **name**: Device name (may be "Unknown Device" if not advertised)
- **macAddress**: Unique MAC address for connection
- **rssi**: Signal strength in dBm (updated automatically)
- **lastSeen**: Timestamp of last advertisement

#### Debug Logging

The native layer logs all filtering decisions for troubleshooting:

```
// Accepted devices
D/BleScanFilter: Device accepted: Q_Ring_ABC (AA:BB:CC:DD:EE:FF) RSSI: -65

// Rejected devices
D/BleScanFilter: Device rejected: FitBit (11:22:33:44:55:66) - Device name doesn't match QRing pattern
D/BleScanFilter: Device rejected: Unknown (77:88:99:AA:BB:CC) - RSSI too low: -105
```

Use `adb logcat | grep BleScanFilter` to monitor filtering in real-time.

#### Best Practices

**Trust the Filter:**
```dart
// The plugin guarantees all devices are QRing-compatible
// No need for additional validation
Future<void> connectToFirstDevice() async {
  final devices = await QringSdkFlutter.devicesStream.first;
  if (devices.isNotEmpty) {
    // Safe to connect - device is guaranteed compatible
    await QringSdkFlutter.connect(devices.first.macAddress);
  }
}
```

**Handle Empty Results:**
```dart
QringSdkFlutter.devicesStream.listen((devices) {
  if (devices.isEmpty) {
    // No QRing devices found
    // Possible reasons:
    // - No QRing devices nearby
    // - Devices out of range
    // - Devices already connected to another phone
    showMessage('No QRing devices found. Make sure your ring is nearby and charged.');
  } else {
    displayDevices(devices);
  }
});
```

**Monitor Signal Strength:**
```dart
// Use RSSI to show signal quality
Widget buildDeviceCard(QRingDevice device) {
  String signalQuality;
  if (device.rssi > -60) {
    signalQuality = 'Excellent';
  } else if (device.rssi > -70) {
    signalQuality = 'Good';
  } else if (device.rssi > -80) {
    signalQuality = 'Fair';
  } else {
    signalQuality = 'Weak';
  }
  
  return Card(
    child: ListTile(
      title: Text(device.name),
      subtitle: Text('${device.macAddress} • Signal: $signalQuality'),
      trailing: Text('${device.rssi} dBm'),
    ),
  );
}
```

#### Future Enhancements

The filtering system is designed to be extensible. Future versions may include:

- **Service UUID Filtering**: Validate devices based on advertised BLE service UUIDs
- **Manufacturer Data Filtering**: Check manufacturer-specific data in advertisements
- **SDK Validation Methods**: Use native SDK validation if available
- **Custom Filter Rules**: Allow apps to add custom filtering criteria

These enhancements will be added as the QC Wireless SDK documentation becomes available, without requiring changes to your Flutter code.

### Find My Ring

```dart
Future<void> findMyRing() async {
  try {
    await QringSdkFlutter.findRing();
    print('Ring is vibrating!');
  } on Exception catch (e) {
    if (e.toString().contains('not connected')) {
      print('Please connect to a device first');
    } else {
      print('Failed to find ring: $e');
    }
  }
}
```

### Battery and Device Info

```dart
Future<void> checkDeviceStatus() async {
  try {
    // Get battery level
    int battery = await QringSdkFlutter.getBattery();
    if (battery == -1) {
      print('Device not connected');
    } else {
      print('Battery: $battery%');
    }
    
    // Get device information
    QringDeviceInfo info = await QringSdkFlutter.getDeviceInfo();
    print('Firmware: ${info.firmwareVersion}');
    print('Hardware: ${info.hardwareVersion}');
    print('Supports Temperature: ${info.supportsTemperature}');
    print('Supports Blood Oxygen: ${info.supportsBloodOxygen}');
    print('Supports Blood Pressure: ${info.supportsBloodPressure}');
    print('Supports HRV: ${info.supportsHrv}');
  } catch (e) {
    print('Failed to get device status: $e');
  }
}
```

### Health Data Synchronization

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

Future<void> syncHealthData() async {
  final healthData = QringHealthData();
  
  try {
    // Sync today's step data (dayOffset = 0)
    StepData steps = await healthData.syncStepData(0);
    print('Steps: ${steps.totalSteps}');
    print('Distance: ${steps.distanceMeters}m');
    print('Calories: ${steps.calories}');
    
    // Sync yesterday's heart rate data (dayOffset = 1)
    List<HeartRateData> heartRates = await healthData.syncHeartRateData(1);
    print('Heart rate measurements: ${heartRates.length}');
    for (var hr in heartRates) {
      print('  ${hr.timestamp}: ${hr.heartRate} bpm');
    }
    
    // Sync sleep data
    SleepData sleep = await healthData.syncSleepData(0);
    print('Sleep duration: ${sleep.endTime.difference(sleep.startTime)}');
    print('Sleep stages:');
    for (var detail in sleep.details) {
      print('  ${detail.stage}: ${detail.durationMinutes} minutes');
    }
    
    // Sync blood oxygen data
    List<BloodOxygenData> spo2 = await healthData.syncBloodOxygenData(0);
    print('SpO2 measurements: ${spo2.length}');
    
    // Sync blood pressure data
    List<BloodPressureData> bp = await healthData.syncBloodPressureData(0);
    print('Blood pressure measurements: ${bp.length}');
    for (var reading in bp) {
      print('  ${reading.timestamp}: ${reading.systolic}/${reading.diastolic}');
    }
  } catch (e) {
    print('Failed to sync data: $e');
  }
}
```

### Manual Measurements

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

class MeasurementManager {
  final healthData = QringHealthData();
  StreamSubscription? _measurementSubscription;
  
  Future<void> startHeartRateMeasurement() async {
    try {
      // Listen for measurement results
      _measurementSubscription = healthData.measurementStream.listen((measurement) {
        if (measurement.success) {
          print('Heart Rate: ${measurement.heartRate} bpm');
        } else {
          print('Measurement failed: ${measurement.errorMessage}');
        }
      });
      
      // Start measurement
      await healthData.startHeartRateMeasurement();
      print('Heart rate measurement started');
    } catch (e) {
      print('Failed to start measurement: $e');
    }
  }
  
  Future<void> startBloodPressureMeasurement() async {
    try {
      _measurementSubscription = healthData.measurementStream.listen((measurement) {
        if (measurement.success) {
          print('Blood Pressure: ${measurement.systolic}/${measurement.diastolic}');
        } else {
          print('Measurement failed: ${measurement.errorMessage}');
        }
      });
      
      await healthData.startBloodPressureMeasurement();
    } catch (e) {
      print('Failed to start measurement: $e');
    }
  }
  
  Future<void> stopMeasurement() async {
    try {
      await healthData.stopMeasurement();
      await _measurementSubscription?.cancel();
      print('Measurement stopped');
    } catch (e) {
      print('Failed to stop measurement: $e');
    }
  }
  
  void dispose() {
    _measurementSubscription?.cancel();
  }
}
```

### Continuous Monitoring

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

Future<void> configureContinuousMonitoring() async {
  final settings = QringSettings();
  
  try {
    // Enable continuous heart rate monitoring every 30 minutes
    await settings.setContinuousHeartRate(true, 30);
    print('Continuous heart rate enabled');
    
    // Read current settings
    var hrSettings = await settings.getContinuousHeartRateSettings();
    print('HR monitoring: ${hrSettings['enabled']}');
    print('HR interval: ${hrSettings['intervalMinutes']} minutes');
    
    // Enable continuous blood oxygen monitoring
    await settings.setContinuousBloodOxygen(true, 30);
    print('Continuous SpO2 enabled');
    
    // Enable continuous blood pressure monitoring
    await settings.setContinuousBloodPressure(true);
    print('Continuous BP enabled');
    
    // Listen for automatic measurements
    QringHealthData().notificationStream.listen((measurement) {
      print('Automatic measurement: ${measurement.type}');
      if (measurement.heartRate != null) {
        print('  Heart Rate: ${measurement.heartRate} bpm');
      }
    });
  } catch (e) {
    print('Failed to configure monitoring: $e');
  }
}
```

### Display Settings

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

Future<void> configureDisplay() async {
  final settings = QringSettings();
  
  try {
    // Read current display settings
    DisplaySettings current = await settings.getDisplaySettings();
    print('Current brightness: ${current.brightness}/${current.maxBrightness}');
    
    // Update display settings
    final newSettings = DisplaySettings(
      enabled: true,
      leftHand: false,  // Right hand
      brightness: 5,
      maxBrightness: current.maxBrightness,
      doNotDisturb: false,
      screenOnStartMinutes: 480,  // 8:00 AM
      screenOnEndMinutes: 1320,   // 10:00 PM
    );
    
    await settings.setDisplaySettings(newSettings);
    print('Display settings updated');
    
    // Set user information
    final userInfo = UserInfo(
      age: 30,
      heightCm: 175,
      weightKg: 70,
      isMale: true,
    );
    await settings.setUserInfo(userInfo);
    print('User info updated');
    
    // Set user ID
    await settings.setUserId('user123');
    print('User ID set');
  } catch (e) {
    print('Failed to configure display: $e');
  }
}
```

### Exercise Tracking

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

class ExerciseTracker {
  final exercise = QringExercise();
  StreamSubscription? _exerciseSubscription;
  
  Future<void> startExercise() async {
    try {
      // Listen for real-time exercise data
      _exerciseSubscription = exercise.exerciseDataStream.listen((data) {
        print('Duration: ${data.durationSeconds}s');
        print('Heart Rate: ${data.heartRate} bpm');
        print('Steps: ${data.steps}');
        print('Distance: ${data.distanceMeters}m');
        print('Calories: ${data.calories}');
      });
      
      // Start running exercise
      await exercise.startExercise(ExerciseType.running);
      print('Exercise started');
    } catch (e) {
      print('Failed to start exercise: $e');
    }
  }
  
  Future<void> pauseExercise() async {
    try {
      await exercise.pauseExercise();
      print('Exercise paused');
    } catch (e) {
      print('Failed to pause exercise: $e');
    }
  }
  
  Future<void> resumeExercise() async {
    try {
      await exercise.resumeExercise();
      print('Exercise resumed');
    } catch (e) {
      print('Failed to resume exercise: $e');
    }
  }
  
  Future<void> stopExercise() async {
    try {
      ExerciseSummary summary = await exercise.stopExercise();
      print('Exercise completed!');
      print('Total duration: ${summary.totalDurationSeconds}s');
      print('Total steps: ${summary.totalSteps}');
      print('Total distance: ${summary.totalDistanceMeters}m');
      print('Total calories: ${summary.totalCalories}');
      print('Average heart rate: ${summary.averageHeartRate} bpm');
      
      await _exerciseSubscription?.cancel();
    } catch (e) {
      print('Failed to stop exercise: $e');
    }
  }
  
  void dispose() {
    _exerciseSubscription?.cancel();
  }
}
```

### Firmware Updates

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

class FirmwareUpdater {
  final firmware = QringFirmware();
  StreamSubscription? _progressSubscription;
  
  Future<void> updateFirmware(String filePath) async {
    try {
      // Validate firmware file
      bool isValid = await firmware.validateFirmwareFile(filePath);
      if (!isValid) {
        print('Invalid firmware file');
        return;
      }
      
      // Listen for update progress
      _progressSubscription = firmware.updateProgressStream.listen((progress) {
        print('Update progress: $progress%');
        if (progress == 100) {
          print('Update completed!');
        }
      });
      
      // Start firmware update
      await firmware.startFirmwareUpdate(filePath);
      print('Firmware update started');
    } catch (e) {
      print('Failed to update firmware: $e');
      await _progressSubscription?.cancel();
    }
  }
  
  void dispose() {
    _progressSubscription?.cancel();
  }
}
```

## Error Handling

All plugin methods throw exceptions on failure. Always wrap calls in try-catch blocks:

```dart
try {
  await QringSdkFlutter.connect(macAddress);
} on Exception catch (e) {
  // Handle specific error types
  if (e.toString().contains('NOT_CONNECTED')) {
    print('Device is not connected');
  } else if (e.toString().contains('PERMISSION_DENIED')) {
    print('Permission denied');
  } else {
    print('Error: $e');
  }
}
```

### Common Error Patterns

```dart
// Check connection before operations
Future<void> safeOperation() async {
  try {
    int battery = await QringSdkFlutter.getBattery();
    if (battery == -1) {
      print('Not connected');
      return;
    }
    // Proceed with operation
  } catch (e) {
    print('Operation failed: $e');
  }
}

// Validate parameters
Future<void> syncWithValidation(int dayOffset) async {
  if (dayOffset < 0 || dayOffset > 6) {
    print('Day offset must be between 0 and 6');
    return;
  }
  
  try {
    await QringHealthData().syncStepData(dayOffset);
  } catch (e) {
    print('Sync failed: $e');
  }
}
```

## Error Codes Reference

The plugin uses standardized error codes for consistent error handling:

### Connection Errors
- `NOT_CONNECTED`: Device is not connected
- `CONNECTION_FAILED`: Failed to establish connection
- `CONNECTION_TIMEOUT`: Connection attempt timed out
- `ALREADY_CONNECTED`: Device is already connected
- `DISCONNECTION_FAILED`: Failed to disconnect

### Parameter Validation Errors
- `INVALID_ARGUMENT`: Invalid argument provided
- `MISSING_ARGUMENT`: Required argument is missing
- `INVALID_RANGE`: Value is outside valid range
- `INVALID_FORMAT`: Invalid format for provided value

### BLE Operation Errors
- `BLE_NOT_AVAILABLE`: Bluetooth LE not available
- `BLE_NOT_ENABLED`: Bluetooth is not enabled
- `SCAN_FAILED`: Failed to start device scanning
- `OPERATION_TIMEOUT`: Operation timed out
- `OPERATION_FAILED`: Operation failed

### Permission Errors
- `PERMISSION_DENIED`: Required permission was denied
- `BLUETOOTH_PERMISSION_REQUIRED`: Bluetooth permission required
- `LOCATION_PERMISSION_REQUIRED`: Location permission required

### Data Sync Errors
- `SYNC_FAILED`: Failed to synchronize data
- `DATA_PARSE_ERROR`: Failed to parse data
- `NO_DATA_AVAILABLE`: No data available for requested period

### Measurement Errors
- `MEASUREMENT_FAILED`: Measurement failed
- `MEASUREMENT_IN_PROGRESS`: Measurement already in progress
- `MEASUREMENT_TIMEOUT`: Measurement timed out

### Firmware Update Errors
- `FILE_NOT_FOUND`: Firmware file not found
- `FILE_NOT_READABLE`: Cannot read firmware file
- `INVALID_FILE`: Invalid firmware file format
- `UPDATE_IN_PROGRESS`: Update already in progress
- `UPDATE_FAILED`: Firmware update failed

### SDK Errors
- `SDK_ERROR`: Error in native SDK
- `SDK_NOT_INITIALIZED`: SDK not initialized
- `UNSUPPORTED_FEATURE`: Feature not supported by device

## Troubleshooting

### Permission Issues

**Problem**: `PERMISSION_DENIED` error when scanning or connecting

**Solutions**:
- **Android 12+**: Ensure `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` permissions are granted
- **Android < 12**: Ensure `ACCESS_FINE_LOCATION` permission is granted and location services are enabled
- **Android 13+**: Ensure `POST_NOTIFICATIONS` permission is granted for background service
- Check permissions in device Settings > Apps > Your App > Permissions
- Request permissions at runtime before attempting BLE operations
- Use `QringSdkFlutter.checkPermissions()` to verify permission status

**Problem**: Permissions granted but operations still fail

**Solutions**:
- Ensure location services are enabled (Settings > Location)
- Restart the app after granting permissions
- Check that Bluetooth is enabled
- Verify permissions in AndroidManifest.xml match the required permissions
- On Android 12+, ensure you're not requesting deprecated permissions

**Problem**: Permission request dialog doesn't appear

**Solutions**:
- Check that permissions are declared in AndroidManifest.xml
- Ensure you're using a permission plugin like `permission_handler`
- Verify the app targets the correct SDK version
- Check if permission was permanently denied (requires manual settings change)

### Scanning Issues

**Problem**: No devices found during scanning

**Solutions**:
- Ensure Bluetooth is enabled on the device
- Grant all required permissions (Bluetooth, Location)
- Enable location services (required for BLE scanning)
- Check that the ring is charged and nearby (within 10 meters)
- Try restarting Bluetooth
- On Android 12+, ensure `BLUETOOTH_SCAN` permission is granted
- Verify the ring is not already connected to another device
- Check that the ring is in pairing mode (if required)

**Problem**: Scan fails to start with error

**Solutions**:
- Check that location services are enabled (required for BLE scanning)
- Verify all permissions are granted
- Ensure Bluetooth is enabled
- Try stopping any active scan before starting a new one
- Restart the app if issues persist
- Check Android logs for native SDK errors

**Problem**: Scan finds devices but not the ring

**Solutions**:
- Ensure the ring is powered on and charged
- Move closer to the ring (within 5 meters for initial pairing)
- Check that the ring is not connected to another device
- Try restarting the ring (if possible)
- Verify the ring is a QC Wireless device
- Check that the ring's device name starts with `O_`, `Q_`, or `R` (QRing naming convention)
- Use `adb logcat | grep BleScanFilter` to see if the ring is being detected but filtered out
- Verify the ring's signal strength is above -100 dBm (move closer if needed)

**Problem**: Scan shows non-QRing devices

**Solutions**:
- This should not happen - the plugin filters at the native layer
- If you see non-QRing devices, please report this as a bug
- Check plugin version - ensure you're using the latest version
- Verify the native layer filtering is active (check Android logs)
- As a workaround, you can add additional filtering in your Flutter code

**Problem**: Same device appears multiple times in scan results

**Solutions**:
- This should not happen - the plugin deduplicates by MAC address
- If you see duplicates, please report this as a bug
- Check that you're not creating multiple scan subscriptions
- Verify you're using the latest plugin version
- Check Android logs for native layer errors

**Problem**: Device RSSI not updating

**Solutions**:
- RSSI updates only when the change is significant (≥5 dBm)
- Small signal fluctuations are ignored to reduce UI updates
- Move the ring closer or farther to see RSSI changes
- Check that the device is still being discovered (not out of range)
- Verify the scan is still active (not stopped)

### Connection Issues

**Problem**: Connection fails or times out

**Solutions**:
- Ensure the ring is within range (typically 10 meters)
- Check that the ring battery isn't too low (< 10%)
- Try stopping any active scan before connecting
- Restart Bluetooth if issues persist
- Ensure only one app is trying to connect to the ring
- Verify `BLUETOOTH_CONNECT` permission is granted (Android 12+)
- Check that the device is not already bonded to another phone
- Try forgetting the device in Bluetooth settings and re-pairing

**Problem**: Connection drops frequently

**Solutions**:
- Keep the ring within range (< 10 meters)
- Ensure the ring battery is adequately charged (> 20%)
- Minimize interference from other Bluetooth devices
- Check for Android power-saving settings that might affect Bluetooth
- Disable battery optimization for your app
- Ensure the foreground service is running (check notification)
- Check for Android system updates that might fix Bluetooth issues

**Problem**: Pairing fails repeatedly

**Solutions**:
- Forget the device in Bluetooth settings
- Clear Bluetooth cache (Settings > Apps > Bluetooth > Storage > Clear Cache)
- Restart both phone and ring
- Ensure the ring is not bonded to another device
- Try pairing from a different phone to verify ring functionality
- Check that the ring firmware is up to date

**Problem**: Connection works but then fails after app restart

**Solutions**:
- Ensure the foreground service has permission to run in background
- Check that battery optimization is disabled for your app
- Verify `RECEIVE_BOOT_COMPLETED` permission is granted
- Check that the service is properly registered in AndroidManifest.xml
- Ensure device persistence is working (check SharedPreferences)

### Background Service Issues

**Problem**: Service stops when app is closed

**Solutions**:
- Ensure `FOREGROUND_SERVICE` permission is granted
- Verify `POST_NOTIFICATIONS` permission is granted (Android 13+)
- Disable battery optimization for your app (Settings > Apps > Your App > Battery)
- Check that the persistent notification is visible
- Ensure the service is using START_STICKY (automatic in plugin)
- Some manufacturers (Xiaomi, Huawei) have aggressive battery management - add app to whitelist

**Problem**: Service doesn't restart after device reboot

**Solutions**:
- Ensure `RECEIVE_BOOT_COMPLETED` permission is granted
- Verify the BootReceiver is registered in AndroidManifest.xml (automatic in plugin)
- Check that the device was connected before reboot
- Disable battery optimization for your app
- Some manufacturers block boot receivers - check manufacturer-specific settings

**Problem**: Notification doesn't appear

**Solutions**:
- Ensure `POST_NOTIFICATIONS` permission is granted (Android 13+)
- Check notification settings for your app (Settings > Apps > Your App > Notifications)
- Verify notification channel is created (automatic in plugin)
- Restart the app if notification doesn't appear
- Check that the service is actually running

**Problem**: Service consumes too much battery

**Solutions**:
- This is expected behavior for continuous BLE connection
- Typical battery usage: 2-5% per day
- Ensure auto-reconnect isn't stuck in a loop (check notification for attempt count)
- If reconnection attempts are very high, manually disconnect and reconnect
- Check for other apps interfering with Bluetooth
- Ensure ring firmware is up to date

### Auto-Reconnect Issues

**Problem**: Auto-reconnect doesn't work

**Solutions**:
- Ensure the disconnection was unexpected (not manual disconnect)
- Check that the foreground service is running
- Verify Bluetooth is enabled
- Check that device persistence is working (device MAC saved)
- Ensure battery optimization is disabled for your app
- Check notification for reconnection status
- Look for error events in `errorStream`

**Problem**: Auto-reconnect takes too long

**Solutions**:
- This is expected behavior - exponential backoff increases delay
- First attempts: 10 seconds
- Later attempts: up to 5 minutes
- Turn Bluetooth off then on to trigger immediate reconnection
- Manually disconnect and reconnect to reset attempt counter

**Problem**: Auto-reconnect stuck in loop

**Solutions**:
- Check that the ring is powered on and in range
- Verify the ring battery isn't dead
- Manually disconnect to stop reconnection attempts
- Forget device in Bluetooth settings and re-pair
- Check for Bluetooth interference
- Restart both phone and ring

**Problem**: Auto-reconnect works but connection drops again

**Solutions**:
- Ensure the ring is within stable range
- Check for Bluetooth interference sources
- Verify ring battery is adequate
- Check for Android system issues with Bluetooth
- Try updating Android system
- Check ring firmware version

### Measurement Issues

**Problem**: Measurements fail or timeout

**Solutions**:
- Ensure the ring is properly worn on the finger
- Keep your hand still during measurements
- Verify the device is connected (check connection state)
- Check that the ring battery isn't too low (< 20%)
- Ensure the ring supports the measurement type (check device info)
- Wait for previous measurement to complete before starting new one
- Try restarting the measurement

**Problem**: No measurement results received

**Solutions**:
- Verify you're subscribed to the measurement stream before starting
- Check that the measurement completed successfully
- Ensure the ring is worn correctly (snug but not too tight)
- Check for error events in the measurement stream
- Verify the ring supports the measurement type
- Try a different measurement type to verify ring functionality

**Problem**: Measurement results seem inaccurate

**Solutions**:
- Ensure proper ring placement (correct finger, correct orientation)
- Keep hand still and relaxed during measurement
- Avoid measurements immediately after exercise
- Check that the ring sensors are clean
- Verify ring firmware is up to date
- Compare with other devices to verify accuracy
- Consult ring documentation for proper usage

### Data Sync Issues

**Problem**: No data returned when syncing

**Solutions**:
- Ensure you're syncing for a day when the ring was worn
- Check that the ring has collected data for that day
- Verify the day offset is valid (0-6, where 0 is today)
- Try syncing a different data type to verify connection
- Ensure stable connection to the ring (check connection state)
- Check that the ring has sufficient battery
- Verify the ring was worn during the requested time period

**Problem**: Data sync times out

**Solutions**:
- Ensure strong Bluetooth connection (keep ring close)
- Try syncing smaller date ranges (one day at a time)
- Check that the ring isn't performing other operations
- Restart the connection if issues persist
- Verify the ring has sufficient battery (> 20%)
- Try syncing during a time with less Bluetooth interference

**Problem**: Synced data seems incomplete

**Solutions**:
- Check that the ring was worn during the entire period
- Verify the ring battery didn't die during the period
- Ensure the ring was properly connected during data collection
- Check ring firmware version for known issues
- Try syncing again to verify data consistency
- Consult ring documentation for data collection requirements

### Notification Issues

**Problem**: "Find My Ring" from notification doesn't work

**Solutions**:
- Ensure the ring is connected (check notification status)
- Verify the ring is within range
- Check that the ring battery isn't too low
- Try using "Find My Ring" from the app instead
- Restart the connection if issues persist
- Check for error events in `errorStream`

**Problem**: Notification shows wrong information

**Solutions**:
- Check that the connection state is correct
- Verify battery level is being updated
- Restart the service to refresh notification
- Check for errors in Android logs
- Ensure the plugin version is up to date

**Problem**: Can't dismiss notification

**Solutions**:
- This is expected - foreground service notification cannot be dismissed
- Disconnect from the ring to stop the service and remove notification
- The notification is required by Android for foreground services

### General Issues

**Problem**: Plugin methods throw unexpected errors

**Solutions**:
- Check that the device is connected before calling methods that require connection
- Validate all parameters before calling methods
- Ensure the native SDK is properly initialized
- Check Android logs for native SDK errors
- Try disconnecting and reconnecting
- Restart the app if issues persist
- Verify plugin version is compatible with your Flutter version

**Problem**: App crashes when using plugin

**Solutions**:
- Ensure all required permissions are declared in AndroidManifest.xml
- Check that minimum SDK version is 23 or higher
- Verify the QC Wireless SDK AAR is properly included
- Check Android logs for native exceptions
- Ensure proper error handling in your code (try-catch blocks)
- Update to the latest plugin version
- Report crash with stack trace to plugin maintainers

**Problem**: Plugin works on one device but not another

**Solutions**:
- Check Android version differences (permissions, APIs)
- Verify both devices have BLE support
- Check manufacturer-specific Bluetooth implementations
- Test on a different device to isolate the issue
- Check for manufacturer-specific battery optimization settings
- Consult manufacturer documentation for BLE quirks

**Problem**: Performance issues or lag

**Solutions**:
- Ensure you're not calling methods too frequently
- Use streams efficiently (don't create multiple subscriptions)
- Dispose of streams when not needed
- Check for memory leaks in your app
- Verify the ring firmware is up to date
- Check for Android system performance issues

### Getting Help

If you're still experiencing issues:

1. **Check Android Logs**: Use `adb logcat` to see native errors
2. **Enable Debug Logging**: Check plugin documentation for debug mode
3. **Verify Setup**: Review installation and permission setup
4. **Test Example App**: Run the included example app to verify plugin functionality
5. **Check GitHub Issues**: Search for similar issues
6. **Report Bug**: Open a GitHub issue with:
   - Android version
   - Device model
   - Plugin version
   - Steps to reproduce
   - Error messages and logs
   - Expected vs actual behavior

### Debug Logging

The plugin provides comprehensive logging at the native Android layer to help troubleshoot issues.

#### Viewing Native Logs

Use Android Debug Bridge (ADB) to view native logs:

```bash
# View all plugin logs
adb logcat | grep -E "BleScanFilter|BleManager|QRingBackgroundService"

# View scan filtering logs only
adb logcat | grep BleScanFilter

# View connection logs only
adb logcat | grep BleManager

# Save logs to file
adb logcat | grep -E "BleScanFilter|BleManager" > qring_logs.txt
```

#### Scan Filtering Logs

The scan filter logs every device discovery and filtering decision:

**Accepted Devices:**
```
D/BleScanFilter: Device accepted: Q_Ring_ABC (AA:BB:CC:DD:EE:FF) RSSI: -65
D/BleScanFilter: Device accepted: O_Ring_XYZ (11:22:33:44:55:66) RSSI: -72
```

**Rejected Devices:**
```
D/BleScanFilter: Device rejected: FitBit (AA:BB:CC:DD:EE:FF) - Device name doesn't match QRing pattern: FitBit
D/BleScanFilter: Device rejected: Unknown (11:22:33:44:55:66) - RSSI too low: -105
D/BleScanFilter: Device rejected: null (77:88:99:AA:BB:CC) - No MAC address
```

**Device Updates:**
```
D/BleScanFilter: Device RSSI updated: Q_Ring_ABC (AA:BB:CC:DD:EE:FF) -65 -> -60
D/BleScanFilter: Device rediscovered: Q_Ring_ABC (AA:BB:CC:DD:EE:FF) RSSI: -65
```

#### Connection Logs

The BLE manager logs connection lifecycle events:

**Scan Events:**
```
D/BleManager: Starting BLE scan with SDK filtering
D/BleManager: Scan started
D/BleManager: Scan stopped
```

**Connection Events:**
```
D/BleManager: Connecting to device: AA:BB:CC:DD:EE:FF
D/BleManager: Connection established
D/BleManager: Services discovered
D/BleManager: Device connected successfully
```

**Error Events:**
```
E/BleManager: Scan failed with error code: 2
E/BleManager: Connection failed: GATT error 133
E/BleManager: Permission denied: BLUETOOTH_SCAN
```

#### Background Service Logs

The background service logs lifecycle and reconnection events:

**Service Lifecycle:**
```
D/QRingBackgroundService: Service started
D/QRingBackgroundService: Foreground service started with notification
D/QRingBackgroundService: Service stopped
```

**Reconnection Events:**
```
D/QRingBackgroundService: Auto-reconnect attempt 1
D/QRingBackgroundService: Reconnection successful
D/QRingBackgroundService: Reconnection failed, retry in 10 seconds
```

#### Common Log Patterns

**Successful Scan:**
```
D/BleManager: Starting BLE scan with SDK filtering
D/BleManager: Scan started
D/BleScanFilter: Device accepted: Q_Ring_ABC (AA:BB:CC:DD:EE:FF) RSSI: -65
D/BleScanFilter: Device accepted: O_Ring_XYZ (11:22:33:44:55:66) RSSI: -72
```

**Permission Issues:**
```
E/BleManager: Permission denied: BLUETOOTH_SCAN
E/BleManager: Cannot start scan without required permissions
```

**Filtering in Action:**
```
D/BleScanFilter: Device rejected: FitBit (AA:BB:CC:DD:EE:FF) - Device name doesn't match QRing pattern
D/BleScanFilter: Device rejected: Unknown (11:22:33:44:55:66) - RSSI too low: -105
D/BleScanFilter: Device accepted: Q_Ring_ABC (77:88:99:AA:BB:CC) RSSI: -65
```

**Connection Issues:**
```
D/BleManager: Connecting to device: AA:BB:CC:DD:EE:FF
E/BleManager: Connection failed: GATT error 133
D/QRingBackgroundService: Auto-reconnect attempt 1
D/QRingBackgroundService: Reconnection failed, retry in 10 seconds
```

#### Interpreting Logs

**GATT Error Codes:**
- `133`: Connection failed (device out of range, interference, or already connected)
- `8`: Connection timeout
- `22`: Device not found
- `257`: Device disconnected

**Scan Error Codes:**
- `1`: Already started
- `2`: Application registration failed
- `3`: Internal error
- `4`: Feature unsupported
- `5`: Out of hardware resources

**RSSI Values:**
- `-30 to -60 dBm`: Excellent signal (very close)
- `-60 to -70 dBm`: Good signal (close)
- `-70 to -80 dBm`: Fair signal (moderate distance)
- `-80 to -90 dBm`: Weak signal (far)
- `-90 to -100 dBm`: Very weak signal (very far)
- `< -100 dBm`: Too weak (filtered out)

#### Troubleshooting with Logs

**No Devices Found:**
```bash
# Check if devices are being discovered but filtered
adb logcat | grep BleScanFilter

# Look for "Device rejected" messages
# If you see your ring being rejected, check the reason
```

**Connection Failures:**
```bash
# Check connection attempts and errors
adb logcat | grep BleManager

# Look for GATT error codes
# Error 133 usually means device is out of range or already connected
```

**Permission Issues:**
```bash
# Check for permission errors
adb logcat | grep -i permission

# Look for "Permission denied" messages
# Verify all required permissions are granted
```

**Reconnection Issues:**
```bash
# Check auto-reconnect behavior
adb logcat | grep QRingBackgroundService

# Look for reconnection attempts and delays
# Verify exponential backoff is working correctly
```

#### Enabling Verbose Logging

For more detailed logs, you can filter by log level:

```bash
# View all logs (verbose)
adb logcat *:V | grep -E "BleScanFilter|BleManager"

# View debug and above
adb logcat *:D | grep -E "BleScanFilter|BleManager"

# View errors only
adb logcat *:E | grep -E "BleScanFilter|BleManager"
```

#### Reporting Issues

When reporting bugs, always include:

1. **Full log output**: Capture logs from app start to error
2. **Filtered logs**: Use grep to focus on relevant components
3. **Timestamps**: Include timestamps to correlate events
4. **Device info**: Android version, device model, plugin version
5. **Steps to reproduce**: Exact steps that trigger the issue

Example bug report command:
```bash
# Capture comprehensive logs for bug report
adb logcat -c  # Clear old logs
# Reproduce the issue
adb logcat -d > bug_report_$(date +%Y%m%d_%H%M%S).txt
```

## Migration Guide

### Migrating from Previous Versions

If you're upgrading from an earlier version of the plugin, this guide will help you migrate to the new production-grade architecture.

### What's Changed

The plugin has been significantly enhanced with:

1. **Background Service Architecture**: Connections now persist when app is closed
2. **Auto-Reconnect**: Automatic reconnection with exponential backoff
3. **Enhanced Permissions**: Full Android 12+ permission support
4. **State Machine**: Explicit connection states with validation
5. **Persistent Notification**: Always-visible connection status
6. **Device Persistence**: Remembers last connected device
7. **Improved Error Handling**: Detailed error codes and recovery

### Breaking Changes

#### 1. Connection State Enum

**Old:**
```dart
enum ConnectionState {
  disconnected,
  connecting,
  connected,
}
```

**New:**
```dart
enum ConnectionState {
  disconnected,
  connecting,
  pairing,        // NEW
  connected,
  reconnecting,   // NEW
  error,          // NEW
}
```

**Migration:**
```dart
// Update your connection state handling
QringSdkFlutter.connectionStateStream.listen((state) {
  switch (state) {
    case ConnectionState.disconnected:
      // Handle disconnected
      break;
    case ConnectionState.connecting:
      // Handle connecting
      break;
    case ConnectionState.pairing:  // NEW
      // Handle pairing (bonding in progress)
      break;
    case ConnectionState.connected:
      // Handle connected
      break;
    case ConnectionState.reconnecting:  // NEW
      // Handle reconnecting (auto-reconnect in progress)
      break;
    case ConnectionState.error:  // NEW
      // Handle error state
      break;
  }
});
```

#### 2. Error Stream

**New Feature:**
```dart
// Listen for detailed error information
QringSdkFlutter.errorStream.listen((error) {
  print('Error: ${error.code} - ${error.message}');
  print('Type: ${error.type}');
  
  // Handle specific error types
  if (error.type == ErrorType.permissionDenied) {
    // Guide user to grant permissions
  } else if (error.type == ErrorType.connectionFailed) {
    // Show connection error message
  }
});
```

#### 3. Permission Handling

**Old:**
```dart
// Basic permission check
Map<String, bool> permissions = await QringSdkFlutter.checkPermissions();
```

**New:**
```dart
// Enhanced permission check with Android 12+ support
Map<String, bool> permissions = await QringSdkFlutter.checkPermissions();

// Check specific Android 12+ permissions
if (!permissions['bluetoothScan']!) {
  // Request BLUETOOTH_SCAN permission
}
if (!permissions['bluetoothConnect']!) {
  // Request BLUETOOTH_CONNECT permission
}
if (!permissions['notification']!) {
  // Request POST_NOTIFICATIONS permission (Android 13+)
}
```

#### 4. Background Service

**Old Behavior:**
- Connection lost when app closed
- Manual reconnection required

**New Behavior:**
- Connection persists when app closed
- Automatic reconnection
- Persistent notification

**Migration:**
```dart
// No code changes required!
// Background service starts automatically when you connect
await QringSdkFlutter.connect(macAddress);
// Service now runs in background with notification

// Service stops automatically when you disconnect
await QringSdkFlutter.disconnect();
// Service stops, notification disappears
```

#### 5. Device Persistence

**New Feature:**
```dart
// Device is automatically remembered after connection
await QringSdkFlutter.connect(macAddress);
// Device MAC and name saved

// After app restart, service will auto-reconnect
// No code changes needed - happens automatically

// Device info cleared on manual disconnect
await QringSdkFlutter.disconnect();
// Saved device info cleared
```

### Migration Steps

#### Step 1: Update Dependencies

Update your `pubspec.yaml`:

```yaml
dependencies:
  qring_sdk_flutter: ^1.0.0  # Update to latest version
```

Run:
```bash
flutter pub get
```

#### Step 2: Update AndroidManifest.xml

Add new permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<!-- Add these new permissions -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Update existing permissions with maxSdkVersion -->
<uses-permission android:name="android.permission.BLUETOOTH" 
    android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" 
    android:maxSdkVersion="30" />
```

#### Step 3: Update Permission Requests

Update your permission request code:

```dart
// Old
Future<void> requestPermissions() async {
  // Request BLUETOOTH and LOCATION
}

// New
Future<void> requestPermissions() async {
  if (Platform.isAndroid) {
    final androidInfo = await DeviceInfoPlugin().androidInfo;
    
    if (androidInfo.version.sdkInt >= 31) {
      // Android 12+: Request new Bluetooth permissions
      await [
        Permission.bluetoothScan,
        Permission.bluetoothConnect,
      ].request();
    } else {
      // Android < 12: Request location permission
      await Permission.location.request();
    }
    
    if (androidInfo.version.sdkInt >= 33) {
      // Android 13+: Request notification permission
      await Permission.notification.request();
    }
  }
}
```

#### Step 4: Update Connection State Handling

Update your UI to handle new connection states:

```dart
// Old
Widget buildConnectionStatus(ConnectionState state) {
  switch (state) {
    case ConnectionState.disconnected:
      return Text('Disconnected');
    case ConnectionState.connecting:
      return Text('Connecting...');
    case ConnectionState.connected:
      return Text('Connected');
  }
}

// New
Widget buildConnectionStatus(ConnectionState state) {
  switch (state) {
    case ConnectionState.disconnected:
      return Text('Disconnected');
    case ConnectionState.connecting:
      return Text('Connecting...');
    case ConnectionState.pairing:
      return Text('Pairing...');  // NEW
    case ConnectionState.connected:
      return Text('Connected');
    case ConnectionState.reconnecting:
      return Text('Reconnecting...');  // NEW
    case ConnectionState.error:
      return Text('Error');  // NEW
  }
}
```

#### Step 5: Add Error Stream Handling

Add error stream listener:

```dart
// New feature - add to your initialization
StreamSubscription? _errorSubscription;

void initializePlugin() {
  // Listen for errors
  _errorSubscription = QringSdkFlutter.errorStream.listen((error) {
    // Show error to user
    showErrorDialog(error.message);
    
    // Handle specific error types
    if (error.type == ErrorType.permissionDenied) {
      // Guide to permissions
    }
  });
}

@override
void dispose() {
  _errorSubscription?.cancel();
  super.dispose();
}
```

#### Step 6: Update UI for Background Service

Add UI to indicate background service status:

```dart
// Show notification status
Widget buildServiceStatus() {
  return StreamBuilder<ConnectionState>(
    stream: QringSdkFlutter.connectionStateStream,
    builder: (context, snapshot) {
      final state = snapshot.data ?? ConnectionState.disconnected;
      
      if (state == ConnectionState.connected || 
          state == ConnectionState.reconnecting) {
        return Card(
          child: ListTile(
            leading: Icon(Icons.notifications_active),
            title: Text('Background Service Active'),
            subtitle: Text('Ring will stay connected when app is closed'),
          ),
        );
      }
      
      return SizedBox.shrink();
    },
  );
}
```

#### Step 7: Test Migration

Test the following scenarios:

1. ✅ Connect to ring - verify notification appears
2. ✅ Close app - verify connection maintained
3. ✅ Reopen app - verify connection still active
4. ✅ Turn Bluetooth off/on - verify auto-reconnect
5. ✅ Walk out of range - verify reconnection when back
6. ✅ Reboot device - verify service restarts
7. ✅ Manual disconnect - verify service stops
8. ✅ Check all permissions granted (Android 12+)

### Compatibility

- **Minimum Flutter Version**: 2.0.0
- **Minimum Dart Version**: 2.12.0
- **Minimum Android Version**: 6.0 (API 23)
- **Target Android Version**: 13.0 (API 33)

### Deprecated Features

None. All previous features are still supported with enhanced functionality.

### New Features to Adopt

1. **Error Stream**: Monitor detailed errors
2. **Reconnecting State**: Show reconnection progress
3. **Pairing State**: Show pairing progress
4. **Background Service**: Automatic - no code needed
5. **Device Persistence**: Automatic - no code needed
6. **Auto-Reconnect**: Automatic - no code needed

### Need Help?

If you encounter issues during migration:

1. Check the [example app](example/) for reference implementation
2. Review the [troubleshooting guide](#troubleshooting)
3. Open an issue on [GitHub](https://github.com/yourusername/qring_sdk_flutter/issues)
4. Include your migration context in the issue description

## Example App

The plugin includes a comprehensive example application demonstrating all features. To run the example:

```bash
cd example
flutter pub get
flutter run
```

The example app includes:
- Device scanning and connection UI
- Find My Ring feature
- Battery and device info display
- Manual health measurements
- Historical data synchronization
- Continuous monitoring configuration
- Display and user settings
- Exercise tracking
- Comprehensive error handling

See [example/README.md](example/README.md) for detailed documentation.

## Platform Support

| Platform | Supported | Version |
|----------|-----------|---------|
| Android  | ✅ Yes    | 6.0+ (API 23+) |
| iOS      | ❌ No     | Planned |
| Web      | ❌ No     | Not planned |
| Windows  | ❌ No     | Not planned |
| macOS    | ❌ No     | Not planned |
| Linux    | ❌ No     | Not planned |

## API Documentation

For detailed API documentation, see the [API Reference](https://pub.dev/documentation/qring_sdk_flutter/latest/).

All public APIs include comprehensive dartdoc comments with:
- Method descriptions
- Parameter documentation
- Return type documentation
- Usage examples
- Platform-specific notes

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues, questions, or feature requests:
- Open an issue on [GitHub](https://github.com/yourusername/qring_sdk_flutter/issues)
- Check existing issues for solutions
- Review the troubleshooting section
- Consult the example app for usage patterns

## Acknowledgments

This plugin wraps the QC Wireless SDK for Android. Special thanks to QC Wireless for providing the native SDK.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed history of changes.
# qring_sdk_flutter
