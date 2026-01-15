# QRing SDK Flutter Plugin - Integration Guide

This guide provides detailed information about integrating the QRing SDK Flutter plugin into your application, including QC SDK setup, architecture details, and testing approaches.

## Table of Contents

- [QC SDK Setup](#qc-sdk-setup)
- [Platform Channel Architecture](#platform-channel-architecture)
- [Integration Steps](#integration-steps)
- [Testing Approach](#testing-approach)
- [Advanced Topics](#advanced-topics)
- [Migration Guide](#migration-guide)
- [Troubleshooting](#troubleshooting)

## QC SDK Setup

### Overview

The QRing SDK Flutter plugin wraps the native QC Wireless SDK for Android. The native SDK is provided as an Android Archive (AAR) file and is included in the plugin.

### SDK Location

The QC Wireless SDK is located at:
```
android/libs/qring_sdk_1.0.0.4.aar
```

This AAR file is automatically included when you add the plugin to your Flutter project.

### SDK Version

- **Current Version**: 1.0.0.4
- **Minimum Android SDK**: API Level 23 (Android 6.0)
- **Target Android SDK**: API Level 34 (Android 14)

### SDK Components

The QC Wireless SDK provides the following key components:

1. **BleOperateManager**: Core BLE operations (scanning, connecting, disconnecting)
2. **BleScannerHelper**: Device discovery and scanning
3. **CommandHandle**: Command execution and response handling
4. **LargeDataHandler**: Large data transfer (sleep data, historical data)
5. **DfuHandle**: Device Firmware Update (DFU) operations

### Updating the SDK

If you need to update to a newer version of the QC Wireless SDK:

1. Obtain the new AAR file from QC Wireless
2. Replace the existing AAR in `android/libs/`
3. Update the version reference in `android/build.gradle` if needed
4. Test all functionality to ensure compatibility
5. Update the version number in this guide

## Platform Channel Architecture

### Overview

The plugin uses Flutter's platform channels to communicate between Dart and native Android code. This architecture provides a clean separation between the Flutter UI layer and native SDK operations.

### Channel Types

The plugin uses three types of channels:

#### 1. Method Channel

**Purpose**: Request-response communication for operations like connecting, getting battery, etc.

**Channel Name**: `qring_sdk_flutter`

**Flow**:
```
Dart (Flutter) → Method Channel → Android (Native) → QC SDK
                                                    ↓
Dart (Flutter) ← Method Channel ← Android (Native) ← Response
```

**Example Methods**:
- `scan`: Start BLE scanning
- `connect`: Connect to a device
- `battery`: Get battery level
- `findRing`: Trigger find my ring

#### 2. Event Channels

**Purpose**: Streaming data from native to Dart for real-time updates.

The plugin uses multiple event channels:

| Channel Name | Purpose | Data Type |
|--------------|---------|-----------|
| `qring_sdk_flutter/state` | Connection state changes | ConnectionState |
| `qring_sdk_flutter/devices` | Discovered devices | List<QringDevice> |
| `qring_sdk_flutter/measurement` | Manual measurement results | HealthMeasurement |
| `qring_sdk_flutter/notification` | Automatic measurements | HealthMeasurement |
| `qring_sdk_flutter/exercise` | Exercise data | ExerciseData |
| `qring_sdk_flutter/firmware_progress` | Firmware update progress | Map |

**Flow**:
```
QC SDK → Android (Native) → Event Channel → Dart (Flutter)
         (Listener)          (Stream)        (Stream Subscription)
```

### Data Serialization

Data is serialized between Dart and native code using Flutter's StandardMessageCodec:

**Dart to Native**:
- Dart objects → `toMap()` → Map<String, dynamic> → StandardMessageCodec → Native

**Native to Dart**:
- Native → HashMap → StandardMessageCodec → Map<dynamic, dynamic> → `fromMap()` → Dart objects

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Flutter Application                     │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Dart API Layer                            │ │
│  │  - QringSdkFlutter                                     │ │
│  │  - QringHealthData                                     │ │
│  │  - QringSettings                                       │ │
│  │  - QringExercise                                       │ │
│  │  - QringFirmware                                       │ │
│  └────────────────────────────────────────────────────────┘ │
│                           ↕                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │           Platform Channel Layer                       │ │
│  │  - MethodChannel (qring_sdk_flutter)                   │ │
│  │  - EventChannel (state, devices, measurement, etc.)    │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                           ↕
┌─────────────────────────────────────────────────────────────┐
│                    Android Native Layer                      │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         QringSdkFlutterPlugin (Main Plugin Class)      │ │
│  │  - MethodCallHandler                                   │ │
│  │  - EventChannel.StreamHandler                          │ │
│  └────────────────────────────────────────────────────────┘ │
│                           ↕                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Manager Classes                           │ │
│  │  - BleManager (scanning, connection)                   │ │
│  │  - DataSyncManager (health data sync)                  │ │
│  │  - MeasurementManager (manual measurements)            │ │
│  │  - NotificationManager (automatic measurements)        │ │
│  │  - SettingsManager (device settings)                   │ │
│  │  - ExerciseManager (exercise tracking)                 │ │
│  │  - FirmwareManager (firmware updates)                  │ │
│  │  - PermissionManager (permission handling)             │ │
│  └────────────────────────────────────────────────────────┘ │
│                           ↕                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Utility Classes                           │ │
│  │  - DataConverter (SDK ↔ Flutter data conversion)       │ │
│  │  - ValidationUtils (parameter validation)              │ │
│  │  - ErrorCodes (error code constants)                   │ │
│  │  - ExceptionHandler (exception handling)               │ │
│  │  - TimeoutHandler (operation timeouts)                 │ │
│  └────────────────────────────────────────────────────────┘ │
│                           ↕                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              QC Wireless SDK (AAR)                     │ │
│  │  - BleOperateManager                                   │ │
│  │  - BleScannerHelper                                    │ │
│  │  - CommandHandle                                       │ │
│  │  - LargeDataHandler                                    │ │
│  │  - DfuHandle                                           │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                           ↕
                    QC Ring Device (BLE)
```

## Integration Steps

### Step 1: Add Plugin Dependency

Add the plugin to your `pubspec.yaml`:

```yaml
dependencies:
  qring_sdk_flutter: ^0.0.1
```

Run:
```bash
flutter pub get
```

### Step 2: Configure Android Manifest

Add required permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Bluetooth permissions for Android < 12 -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    
    <!-- Bluetooth permissions for Android 12+ -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    
    <!-- Location permissions (required for BLE scanning) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <!-- Storage permissions (for firmware updates) -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <!-- Feature declarations -->
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
</manifest>
```

### Step 3: Set Minimum SDK Version

Update `android/app/build.gradle`:

```gradle
android {
    defaultConfig {
        minSdkVersion 23  // Required for QC SDK
        targetSdkVersion 34
        // ...
    }
}
```

### Step 4: Request Permissions at Runtime

Use the `permission_handler` plugin for runtime permissions:

```yaml
dependencies:
  permission_handler: ^11.0.0
```

```dart
import 'package:permission_handler/permission_handler.dart';

Future<bool> requestPermissions() async {
  Map<Permission, PermissionStatus> statuses = await [
    Permission.bluetoothScan,
    Permission.bluetoothConnect,
    Permission.locationWhenInUse,
  ].request();
  
  return statuses.values.every((status) => status.isGranted);
}
```

### Step 5: Initialize and Use the Plugin

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

class QRingService {
  StreamSubscription? _devicesSubscription;
  StreamSubscription? _stateSubscription;
  
  Future<void> initialize() async {
    // Request permissions
    bool permissionsGranted = await requestPermissions();
    if (!permissionsGranted) {
      throw Exception('Permissions not granted');
    }
    
    // Set up connection state listener
    _stateSubscription = QringSdkFlutter.connectionStateStream.listen((state) {
      print('Connection state: $state');
    });
  }
  
  Future<void> scanAndConnect() async {
    // Listen for devices
    _devicesSubscription = QringSdkFlutter.devicesStream.listen((devices) {
      if (devices.isNotEmpty) {
        // Auto-connect to first device (or implement device selection)
        connectToDevice(devices.first.macAddress);
      }
    });
    
    // Start scanning
    await QringSdkFlutter.startScan();
  }
  
  Future<void> connectToDevice(String macAddress) async {
    await QringSdkFlutter.stopScan();
    await QringSdkFlutter.connect(macAddress);
  }
  
  void dispose() {
    _devicesSubscription?.cancel();
    _stateSubscription?.cancel();
  }
}
```

## Testing Approach

### Testing Strategy

The plugin uses a comprehensive testing strategy with three levels:

#### 1. Unit Tests

**Purpose**: Test individual components in isolation

**Location**: `test/unit/` and `test/models/`

**Coverage**:
- Data model serialization/deserialization
- Parameter validation
- Error handling
- Data conversion utilities

**Example**:
```dart
test('QringDevice serialization', () {
  final device = QringDevice(
    name: 'QRing-1234',
    macAddress: 'AA:BB:CC:DD:EE:FF',
    rssi: -65,
  );
  
  final map = device.toMap();
  final restored = QringDevice.fromMap(map);
  
  expect(restored.name, device.name);
  expect(restored.macAddress, device.macAddress);
  expect(restored.rssi, device.rssi);
});
```

#### 2. Property-Based Tests

**Purpose**: Verify universal properties across many inputs

**Location**: `test/property/`

**Framework**: Custom property test helpers with Dart's `test` package

**Coverage**:
- Connection state transitions
- Data synchronization properties
- Measurement result validation
- Error handling consistency

**Example**:
```dart
@Tags(['Feature: qc-wireless-sdk-integration', 'Property 11: Battery Level Within Valid Range'])
test('battery level is always in range 0-100', () async {
  for (int i = 0; i < 100; i++) {
    final batteryLevel = await plugin.getBattery();
    expect(batteryLevel, inInclusiveRange(-1, 100));
    if (batteryLevel != -1) {
      expect(batteryLevel, greaterThanOrEqualTo(0));
      expect(batteryLevel, lessThanOrEqualTo(100));
    }
  }
});
```

#### 3. Integration Tests

**Purpose**: Test end-to-end workflows with real device interaction

**Location**: `example/integration_test/`

**Coverage**:
- Complete scan and connect workflow
- Data synchronization flow
- Measurement workflow
- Exercise tracking workflow

**Running Tests**:

```bash
# Run unit tests
flutter test

# Run property tests
flutter test test/property/

# Run integration tests (requires physical device)
cd example
flutter test integration_test/
```

### Test Organization

```
test/
├── unit/                           # Unit tests
│   ├── models/                     # Data model tests
│   │   ├── qring_device_test.dart
│   │   ├── health_data_test.dart
│   │   └── ...
│   ├── data_conversion_test.dart   # Data conversion tests
│   └── scanning_connection_edge_cases_test.dart
├── property/                       # Property-based tests
│   ├── connection_state_properties_test.dart
│   ├── data_sync_properties_test.dart
│   ├── device_discovery_properties_test.dart
│   ├── manual_measurement_properties_test.dart
│   └── ...
└── qring_sdk_flutter_test.dart    # Main plugin tests
```

### Mocking Strategy

**For Unit Tests**:
- Mock `MethodChannel` using `flutter_test`'s `TestDefaultBinaryMessengerBinding`
- Mock native SDK responses for testing platform channel layer

**For Property Tests**:
- Use real implementations where possible
- Mock only external dependencies (BLE hardware, actual ring device)
- Generate random valid inputs to test properties

**Example Mock Setup**:
```dart
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  
  const MethodChannel channel = MethodChannel('qring_sdk_flutter');
  
  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'battery':
          return 85; // Mock battery level
        case 'connect':
          return null; // Success
        default:
          return null;
      }
    });
  });
  
  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });
}
```

## Advanced Topics

### Custom Error Handling

Implement custom error handling for your application:

```dart
class QRingErrorHandler {
  static void handleError(dynamic error) {
    if (error is Exception) {
      final message = error.toString();
      
      if (message.contains('NOT_CONNECTED')) {
        // Show "Please connect to device" message
      } else if (message.contains('PERMISSION_DENIED')) {
        // Prompt user to grant permissions
      } else if (message.contains('TIMEOUT')) {
        // Show retry option
      } else {
        // Show generic error message
      }
    }
  }
}
```

### Connection State Management

Implement robust connection state management:

```dart
class ConnectionManager {
  ConnectionState _currentState = ConnectionState.disconnected;
  final _stateController = StreamController<ConnectionState>.broadcast();
  
  Stream<ConnectionState> get stateStream => _stateController.stream;
  ConnectionState get currentState => _currentState;
  
  void initialize() {
    QringSdkFlutter.connectionStateStream.listen((state) {
      _currentState = state;
      _stateController.add(state);
      
      // Handle state changes
      switch (state) {
        case ConnectionState.connected:
          _onConnected();
          break;
        case ConnectionState.disconnected:
          _onDisconnected();
          break;
        default:
          break;
      }
    });
  }
  
  void _onConnected() {
    // Initialize device, sync time, etc.
  }
  
  void _onDisconnected() {
    // Clean up, notify user, attempt reconnect, etc.
  }
  
  void dispose() {
    _stateController.close();
  }
}
```

### Data Caching

Implement local caching for health data:

```dart
class HealthDataCache {
  final _cache = <String, dynamic>{};
  
  Future<StepData> getStepData(int dayOffset) async {
    final key = 'steps_$dayOffset';
    
    // Check cache first
    if (_cache.containsKey(key)) {
      final cached = _cache[key];
      if (_isCacheValid(cached['timestamp'])) {
        return cached['data'] as StepData;
      }
    }
    
    // Fetch from device
    final data = await QringHealthData.syncStepData(dayOffset);
    
    // Cache the result
    _cache[key] = {
      'data': data,
      'timestamp': DateTime.now(),
    };
    
    return data;
  }
  
  bool _isCacheValid(DateTime timestamp) {
    return DateTime.now().difference(timestamp).inMinutes < 5;
  }
}
```

### Background Operation Handling

Handle app lifecycle for background operations:

```dart
class AppLifecycleManager extends WidgetsBindingObserver {
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.paused:
        // App in background - maintain connection if needed
        break;
      case AppLifecycleState.resumed:
        // App in foreground - check connection status
        _checkConnection();
        break;
      case AppLifecycleState.detached:
        // App closing - disconnect gracefully
        QringSdkFlutter.disconnect();
        break;
      default:
        break;
    }
  }
  
  Future<void> _checkConnection() async {
    // Verify connection is still active
    try {
      final battery = await QringSdkFlutter.getBattery();
      if (battery == -1) {
        // Connection lost, attempt reconnect
      }
    } catch (e) {
      // Handle error
    }
  }
}
```

## Migration Guide

### From Version 0.0.1 to Future Versions

This section will be updated when new versions are released.

**Current Version**: 0.0.1 (Initial Release)

### Breaking Changes

None yet - this is the initial release.

### Deprecations

None yet - this is the initial release.

### New Features

Initial release includes:
- Device scanning and connection
- Find My Ring feature
- Battery and device info
- Health data synchronization
- Manual measurements
- Continuous monitoring
- Display settings
- Exercise tracking
- Firmware updates
- Permission management

## Troubleshooting

### Build Issues

**Problem**: Build fails with "Duplicate class" error

**Solution**: Clean and rebuild:
```bash
cd android
./gradlew clean
cd ..
flutter clean
flutter pub get
flutter build apk
```

**Problem**: AAR file not found

**Solution**: Verify the AAR file exists at `android/libs/qring_sdk_1.0.0.4.aar`

### Runtime Issues

**Problem**: Plugin not registered

**Solution**: Ensure you've run `flutter pub get` and rebuilt the app after adding the plugin.

**Problem**: Method channel not found

**Solution**: Hot restart the app (not hot reload) after making changes to native code.

### Permission Issues

**Problem**: Permissions not being requested

**Solution**: Use `permission_handler` plugin for runtime permission requests. The plugin's `requestPermissions()` method only checks status.

**Problem**: Location permission required for BLE scanning

**Solution**: This is an Android requirement. Grant location permission even though the plugin doesn't use location data.

### Connection Issues

**Problem**: Cannot connect to device

**Solution**:
1. Ensure device is in range and powered on
2. Check that Bluetooth is enabled
3. Verify all permissions are granted
4. Try stopping scan before connecting
5. Check Android logs for native SDK errors

### Data Sync Issues

**Problem**: No data returned from sync

**Solution**:
1. Ensure device was worn during the requested period
2. Check that the ring has collected data
3. Verify day offset is valid (0-6)
4. Ensure stable connection

## Additional Resources

- [Plugin README](README.md) - Installation and usage guide
- [Example App](example/README.md) - Comprehensive example application
- [API Documentation](https://pub.dev/documentation/qring_sdk_flutter/latest/) - Complete API reference
- [QC Wireless SDK Documentation](android/libs/) - Native SDK documentation (if available)

## Support

For issues, questions, or feature requests:
- Open an issue on [GitHub](https://github.com/yourusername/qring_sdk_flutter/issues)
- Check existing issues for solutions
- Review the troubleshooting section
- Consult the example app for usage patterns

## License

This plugin is licensed under the MIT License. See [LICENSE](LICENSE) for details.
