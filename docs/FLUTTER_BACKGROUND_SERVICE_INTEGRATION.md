# Flutter Background Service Integration Guide

## Overview

This guide explains how to integrate and use the QRing Background Service from your Flutter application. The background service maintains continuous communication with the QRing smart ring device, even when the Flutter app is killed.

## Quick Start

### 1. Import the Package

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';
```

### 2. Start the Background Service

```dart
// Get the device MAC address from a connected device
String deviceMac = "AA:BB:CC:DD:EE:FF";

// Start the background service
try {
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
  print('Background service started successfully');
} catch (e) {
  print('Failed to start background service: $e');
}
```

### 3. Listen to Service State Changes

```dart
// Listen to service state updates
QringSdkFlutter.instance.serviceStateStream.listen((state) {
  print('Service running: ${state.isRunning}');
  print('Device connected: ${state.isConnected}');
  print('Device MAC: ${state.deviceMac}');
});
```

### 4. Stop the Background Service

```dart
try {
  await QringSdkFlutter.instance.stopBackgroundService();
  print('Background service stopped');
} catch (e) {
  print('Failed to stop background service: $e');
}
```

## API Reference

### MethodChannel Methods

All methods are accessed through `QringSdkFlutter.instance`.


#### startBackgroundService(String deviceMac)

Starts the Android foreground service and connects to the specified QRing device.

**Parameters:**
- `deviceMac` (String, required): The MAC address of the QRing device to connect to

**Returns:** `Future<void>`

**Throws:** `PlatformException` if the service fails to start

**Example:**
```dart
String deviceMac = "AA:BB:CC:DD:EE:FF";

try {
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
  print('Service started successfully');
} catch (e) {
  if (e is PlatformException) {
    print('Error code: ${e.code}');
    print('Error message: ${e.message}');
  }
}
```

**Behavior:**
- Creates a foreground service with persistent notification
- Attempts to connect to the specified device
- Service continues running even if app is killed
- Automatically reconnects if device disconnects

**Requirements:**
- Bluetooth must be enabled
- Required permissions must be granted (Bluetooth, notifications)
- Device MAC address must be valid

---

#### stopBackgroundService()

Stops the Android foreground service and disconnects from the QRing device.

**Parameters:** None

**Returns:** `Future<void>`

**Throws:** `PlatformException` if the service fails to stop

**Example:**
```dart
try {
  await QringSdkFlutter.instance.stopBackgroundService();
  print('Service stopped successfully');
} catch (e) {
  print('Failed to stop service: $e');
}
```

**Behavior:**
- Disconnects from the QRing device
- Removes the persistent notification
- Clears saved connection state
- Terminates the foreground service

---

#### isServiceRunning()

Checks whether the background service is currently running.

**Parameters:** None

**Returns:** `Future<bool>` - `true` if service is running, `false` otherwise

**Throws:** `PlatformException` on error

**Example:**
```dart
bool isRunning = await QringSdkFlutter.instance.isServiceRunning();

if (isRunning) {
  print('Background service is active');
} else {
  print('Background service is not running');
}
```

**Use Cases:**
- Check service status before starting/stopping
- Update UI based on service state
- Prevent duplicate service starts

---

#### sendRingCommand(String command, Map<String, dynamic> params)

Sends a custom command to the QRing device through the background service.

**Parameters:**
- `command` (String, required): The command name to execute
- `params` (Map<String, dynamic>, optional): Command parameters

**Returns:** `Future<Map<String, dynamic>>` - Command result

**Throws:** `PlatformException` if command fails

**Example:**
```dart
try {
  Map<String, dynamic> result = await QringSdkFlutter.instance.sendRingCommand(
    'findMyRing',
    {},
  );
  print('Command executed: ${result['success']}');
} catch (e) {
  print('Command failed: $e');
}
```

**Supported Commands:**
- `findMyRing`: Trigger the Find My Ring feature
- Custom commands can be added in the future

**Behavior:**
- Validates device is connected before execution
- Returns error if device is disconnected
- Executes command through QRing SDK

---

### EventChannel Stream

#### serviceStateStream

A stream that emits service state changes in real-time.

**Type:** `Stream<ServiceState>`

**Events Emitted:**
- Service started
- Service stopped
- Device connected
- Device disconnected
- Reconnection attempts

**Example:**
```dart
StreamSubscription<ServiceState>? subscription;

// Start listening
subscription = QringSdkFlutter.instance.serviceStateStream.listen(
  (state) {
    print('Service state changed:');
    print('  Running: ${state.isRunning}');
    print('  Connected: ${state.isConnected}');
    print('  Device MAC: ${state.deviceMac}');
    print('  Reconnect attempts: ${state.reconnectAttempts}');
  },
  onError: (error) {
    print('Stream error: $error');
  },
);

// Stop listening when done
subscription?.cancel();
```

**ServiceState Properties:**
- `isRunning` (bool): Whether the service is active
- `isConnected` (bool): Whether device is connected
- `deviceMac` (String?): MAC address of connected device
- `deviceName` (String?): Name of connected device
- `reconnectAttempts` (int): Number of reconnection attempts
- `lastConnectedTime` (DateTime?): Last successful connection time

---

## Data Models

### ServiceState

Represents the current state of the background service.

```dart
class ServiceState {
  final bool isRunning;
  final bool isConnected;
  final String? deviceMac;
  final String? deviceName;
  final int reconnectAttempts;
  final DateTime? lastConnectedTime;
  
  ServiceState({
    required this.isRunning,
    required this.isConnected,
    this.deviceMac,
    this.deviceName,
    this.reconnectAttempts = 0,
    this.lastConnectedTime,
  });
  
  // Serialization methods
  Map<String, dynamic> toMap();
  factory ServiceState.fromMap(Map<String, dynamic> map);
}
```

---

## Complete Integration Example

### Basic Service Control

```dart
import 'package:flutter/material.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

class BackgroundServiceControl extends StatefulWidget {
  final String deviceMac;
  
  const BackgroundServiceControl({
    Key? key,
    required this.deviceMac,
  }) : super(key: key);
  
  @override
  State<BackgroundServiceControl> createState() => _BackgroundServiceControlState();
}

class _BackgroundServiceControlState extends State<BackgroundServiceControl> {
  bool _isServiceRunning = false;
  ServiceState? _currentState;
  
  @override
  void initState() {
    super.initState();
    _checkServiceStatus();
    _listenToServiceState();
  }
  
  Future<void> _checkServiceStatus() async {
    bool isRunning = await QringSdkFlutter.instance.isServiceRunning();
    setState(() {
      _isServiceRunning = isRunning;
    });
  }
  
  void _listenToServiceState() {
    QringSdkFlutter.instance.serviceStateStream.listen((state) {
      setState(() {
        _currentState = state;
        _isServiceRunning = state.isRunning;
      });
    });
  }
  
  Future<void> _startService() async {
    try {
      await QringSdkFlutter.instance.startBackgroundService(widget.deviceMac);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Background service started')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to start service: $e')),
      );
    }
  }
  
  Future<void> _stopService() async {
    try {
      await QringSdkFlutter.instance.stopBackgroundService();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Background service stopped')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to stop service: $e')),
      );
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Background Service',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 16),
            
            // Service status
            Row(
              children: [
                Icon(
                  _isServiceRunning ? Icons.check_circle : Icons.cancel,
                  color: _isServiceRunning ? Colors.green : Colors.grey,
                ),
                const SizedBox(width: 8),
                Text(_isServiceRunning ? 'Running' : 'Stopped'),
              ],
            ),
            
            // Connection status
            if (_currentState != null) ...[
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(
                    _currentState!.isConnected ? Icons.bluetooth_connected : Icons.bluetooth_disabled,
                    color: _currentState!.isConnected ? Colors.blue : Colors.grey,
                  ),
                  const SizedBox(width: 8),
                  Text(_currentState!.isConnected ? 'Connected' : 'Disconnected'),
                ],
              ),
            ],
            
            const SizedBox(height: 16),
            
            // Control buttons
            Row(
              children: [
                ElevatedButton(
                  onPressed: _isServiceRunning ? null : _startService,
                  child: const Text('Start Service'),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _isServiceRunning ? _stopService : null,
                  child: const Text('Stop Service'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
```

### Advanced: Service with Command Execution

```dart
class AdvancedServiceControl extends StatefulWidget {
  final String deviceMac;
  
  const AdvancedServiceControl({
    Key? key,
    required this.deviceMac,
  }) : super(key: key);
  
  @override
  State<AdvancedServiceControl> createState() => _AdvancedServiceControlState();
}

class _AdvancedServiceControlState extends State<AdvancedServiceControl> {
  ServiceState? _serviceState;
  String _statusMessage = '';
  
  @override
  void initState() {
    super.initState();
    _listenToServiceState();
  }
  
  void _listenToServiceState() {
    QringSdkFlutter.instance.serviceStateStream.listen(
      (state) {
        setState(() {
          _serviceState = state;
          _updateStatusMessage(state);
        });
      },
      onError: (error) {
        setState(() {
          _statusMessage = 'Error: $error';
        });
      },
    );
  }
  
  void _updateStatusMessage(ServiceState state) {
    if (!state.isRunning) {
      _statusMessage = 'Service is not running';
    } else if (state.isConnected) {
      _statusMessage = 'Connected to ${state.deviceName ?? state.deviceMac}';
    } else if (state.reconnectAttempts > 0) {
      _statusMessage = 'Reconnecting... (Attempt ${state.reconnectAttempts})';
    } else {
      _statusMessage = 'Disconnected';
    }
  }
  
  Future<void> _executeFindMyRing() async {
    if (_serviceState?.isConnected != true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Device is not connected')),
      );
      return;
    }
    
    try {
      await QringSdkFlutter.instance.sendRingCommand('findMyRing', {});
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Find My Ring activated')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Command failed: $e')),
      );
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Status display
        Card(
          child: ListTile(
            leading: Icon(
              _serviceState?.isConnected == true
                  ? Icons.bluetooth_connected
                  : Icons.bluetooth_disabled,
              color: _serviceState?.isConnected == true
                  ? Colors.blue
                  : Colors.grey,
            ),
            title: Text(_statusMessage),
            subtitle: _serviceState?.lastConnectedTime != null
                ? Text('Last connected: ${_formatTime(_serviceState!.lastConnectedTime!)}')
                : null,
          ),
        ),
        
        // Command buttons
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: ElevatedButton.icon(
            onPressed: _serviceState?.isConnected == true ? _executeFindMyRing : null,
            icon: const Icon(Icons.search),
            label: const Text('Find My Ring'),
          ),
        ),
      ],
    );
  }
  
  String _formatTime(DateTime time) {
    return '${time.hour}:${time.minute.toString().padLeft(2, '0')}';
  }
}
```

---

## Event Channel Events

### Event Types and Payloads

The `serviceStateStream` emits events with the following structure:

#### Service Started Event

```dart
{
  'event': 'serviceStarted',
  'isRunning': true,
  'isConnected': false,
  'deviceMac': 'AA:BB:CC:DD:EE:FF',
  'timestamp': 1234567890
}
```

#### Service Stopped Event

```dart
{
  'event': 'serviceStopped',
  'isRunning': false,
  'isConnected': false,
  'timestamp': 1234567890
}
```

#### Device Connected Event

```dart
{
  'event': 'deviceConnected',
  'isRunning': true,
  'isConnected': true,
  'deviceMac': 'AA:BB:CC:DD:EE:FF',
  'deviceName': 'QRing-ABC123',
  'timestamp': 1234567890
}
```

#### Device Disconnected Event

```dart
{
  'event': 'deviceDisconnected',
  'isRunning': true,
  'isConnected': false,
  'deviceMac': 'AA:BB:CC:DD:EE:FF',
  'reconnectAttempts': 0,
  'timestamp': 1234567890
}
```

#### Reconnection Attempt Event

```dart
{
  'event': 'reconnecting',
  'isRunning': true,
  'isConnected': false,
  'deviceMac': 'AA:BB:CC:DD:EE:FF',
  'reconnectAttempts': 3,
  'timestamp': 1234567890
}
```

---

## Best Practices

### 1. Check Service Status Before Starting

```dart
bool isRunning = await QringSdkFlutter.instance.isServiceRunning();
if (!isRunning) {
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
}
```

### 2. Handle Service State in UI

```dart
StreamBuilder<ServiceState>(
  stream: QringSdkFlutter.instance.serviceStateStream,
  builder: (context, snapshot) {
    if (!snapshot.hasData) {
      return const CircularProgressIndicator();
    }
    
    final state = snapshot.data!;
    return Text(state.isConnected ? 'Connected' : 'Disconnected');
  },
)
```

### 3. Clean Up Subscriptions

```dart
class MyWidget extends StatefulWidget {
  @override
  State<MyWidget> createState() => _MyWidgetState();
}

class _MyWidgetState extends State<MyWidget> {
  StreamSubscription<ServiceState>? _subscription;
  
  @override
  void initState() {
    super.initState();
    _subscription = QringSdkFlutter.instance.serviceStateStream.listen((state) {
      // Handle state changes
    });
  }
  
  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    return Container();
  }
}
```

### 4. Handle Errors Gracefully

```dart
try {
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
} on PlatformException catch (e) {
  if (e.code == 'PERMISSION_DENIED') {
    // Show permission request dialog
  } else if (e.code == 'BLUETOOTH_DISABLED') {
    // Prompt user to enable Bluetooth
  } else {
    // Show generic error message
  }
}
```

### 5. Validate Device MAC Address

```dart
bool isValidMac(String mac) {
  final regex = RegExp(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$');
  return regex.hasMatch(mac);
}

if (isValidMac(deviceMac)) {
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
} else {
  print('Invalid MAC address format');
}
```

---

## Platform-Specific Considerations

### Android

#### Permissions

The following permissions are required and automatically requested:

- `BLUETOOTH` (Android < 12)
- `BLUETOOTH_ADMIN` (Android < 12)
- `BLUETOOTH_CONNECT` (Android 12+)
- `BLUETOOTH_SCAN` (Android 12+)
- `POST_NOTIFICATIONS` (Android 13+)
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_CONNECTED_DEVICE`

#### Notification

A persistent notification is displayed while the service is running:
- Title: "Smart Ring Connected" or "Smart Ring Disconnected"
- Description: Connection status and device name
- Action: "Find My Ring" button (when connected)
- Tap: Opens the Flutter app

#### Battery Optimization

The service is optimized for battery efficiency:
- Foreground service priority (not killed by system)
- Exponential backoff for reconnection attempts
- Minimal wake lock usage
- Doze mode compatible

### iOS

The background service is **Android-only**. iOS does not support this feature due to platform limitations.

On iOS, the methods will return errors:
```dart
try {
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
} catch (e) {
  // Will throw PlatformException with code 'UNSUPPORTED_PLATFORM'
}
```

---

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues and solutions.

---

## Additional Resources

- [Background Service Architecture](../android/BACKGROUND_SERVICE_ARCHITECTURE.md)
- [Integration Test Guide](../example/integration_test/BACKGROUND_SERVICE_TEST_GUIDE.md)
- [Android Foreground Services Documentation](https://developer.android.com/guide/components/foreground-services)
