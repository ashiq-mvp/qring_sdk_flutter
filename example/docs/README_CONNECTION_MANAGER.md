# QRing SDK Example App - Connection Manager Features

This document describes the new connection management features added to the example app to demonstrate the production-grade BLE Connection Manager.

## Overview

The example app has been updated to showcase all features of the production-grade BLE Connection Manager, including:

- **Complete connection state display** (idle, connecting, pairing, connected, reconnecting, disconnected, error)
- **Error handling and display** with typed error messages
- **Reconnection status** with attempt counter
- **Permission request flow** for Android 12+
- **Background service controls** for persistent connection
- **Real-time battery updates**
- **Service state monitoring**

## New Screens

### 1. Connection Manager Screen (`connection_manager_screen.dart`)

A comprehensive screen that demonstrates all production BLE features:

**Features:**
- Real-time connection status display with all states
- Device information (name, MAC address, battery level)
- Reconnection attempt counter
- Background service start/stop controls
- Quick actions (Find Ring, Get Battery)
- Error display with detailed information
- Permission request handling

**Usage:**
```dart
Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => ConnectionManagerScreen(
      deviceMac: 'AA:BB:CC:DD:EE:FF',
      deviceName: 'QRing-1234',
    ),
  ),
);
```

**Event Streams Monitored:**
- `connectionStateStream` - Connection state changes
- `serviceStateStream` - Background service state
- `errorStream` - Typed BLE errors
- `bleBatteryStream` - Battery level updates
- `bleConnectionStateStream` - Detailed state changes

### 2. Permissions Screen (`permissions_screen.dart`)

A dedicated screen for managing app permissions:

**Features:**
- Display all required permissions with status
- Individual permission request buttons
- "Grant All" button for convenience
- Open app settings button
- Permission descriptions and icons
- Status indicators (granted, denied, permanently denied)

**Permissions Managed:**
- Bluetooth Scan (Android 12+)
- Bluetooth Connect (Android 12+)
- Location (required for BLE scanning)
- Notifications (for background service)

**Usage:**
```dart
Navigator.pushNamed(context, '/permissions');
```

### 3. Updated Device Scanning Screen

Enhanced with new connection states and navigation:

**New Features:**
- Support for `pairing` and `reconnecting` states
- Permissions button in app bar
- Connection Manager button in app bar
- Better state visualization

## New Widgets

### 1. BLE Error Banner (`ble_error_banner.dart`)

A reusable widget for displaying BLE errors:

**Features:**
- Error type-specific icons
- User-friendly error descriptions
- Actionable suggestions
- Action buttons based on error type
- Dismissible

**Usage:**
```dart
BleErrorBanner(
  error: bleError,
  onDismiss: () => setState(() => error = null),
  onAction: () => retryConnection(),
)
```

**Error Types Handled:**
- Permission Denied
- Bluetooth Off
- Device Not Found
- Pairing Failed
- Connection Failed/Timeout
- GATT Error
- Command Failed
- Reconnection Failed

### 2. Reconnection Indicator (`reconnection_indicator.dart`)

A widget to display reconnection status:

**Features:**
- Attempt counter
- Progress indicator
- Color-coded by attempt number
- Exponential backoff strategy indicator
- Optional cancel button

**Usage:**
```dart
ReconnectionIndicator(
  attemptNumber: reconnectAttempts,
  onCancel: () => stopReconnection(),
)
```

## Connection States

The app now properly handles all connection states:

| State | Color | Icon | Description |
|-------|-------|------|-------------|
| `disconnected` | Grey | bluetooth_disabled | No active connection |
| `connecting` | Orange | sync | Connection attempt in progress |
| `pairing` | Blue | link | Device pairing/bonding in progress |
| `connected` | Green | check_circle | Successfully connected |
| `reconnecting` | Amber | refresh | Automatic reconnection in progress |
| `disconnecting` | Orange | sync | Disconnection in progress |

## Error Handling

The app demonstrates comprehensive error handling:

### Error Display
- Errors are shown in a banner with type-specific icons
- User-friendly descriptions and suggestions
- Action buttons for common fixes

### Error Types
- **Permission Denied**: Shows "Grant Permissions" button
- **Bluetooth Off**: Shows "Enable Bluetooth" button
- **Connection Failed**: Shows "Retry Connection" button
- **Pairing Failed**: Shows retry option
- **Device Not Found**: Shows "Retry Scan" button

### Error Recovery
- Automatic reconnection for transient errors
- User-initiated retry for recoverable errors
- Settings navigation for permission errors

## Background Service

The app demonstrates background service management:

### Service Controls
- **Start Service**: Begins background service with persistent connection
- **Stop Service**: Terminates service and disconnects
- **Service Status**: Visual indicator showing if service is running

### Service Features
- Maintains connection when app is killed
- Automatic reconnection with exponential backoff
- Persistent notification with device status
- Find My Ring from notification

### Service State Monitoring
```dart
QringSdkFlutter.serviceStateStream.listen((state) {
  print('Service running: ${state.isRunning}');
  print('Device connected: ${state.isConnected}');
  print('Reconnect attempts: ${state.reconnectAttempts}');
});
```

## Permission Flow

The app demonstrates proper permission handling:

### Permission Request Flow
1. Check permission status
2. Request missing permissions
3. Handle denial (show rationale)
4. Handle permanent denial (open settings)

### Android 12+ Permissions
- `BLUETOOTH_SCAN` - Required for scanning
- `BLUETOOTH_CONNECT` - Required for connecting
- `POST_NOTIFICATIONS` - Required for service notification

### Pre-Android 12 Permissions
- `ACCESS_FINE_LOCATION` - Required for BLE scanning
- `BLUETOOTH` - Legacy Bluetooth permission
- `BLUETOOTH_ADMIN` - Legacy admin permission

## Testing the Features

### Test Connection States
1. Connect to a device → See `connecting` → `pairing` → `connected`
2. Walk out of range → See `reconnecting` with attempt counter
3. Manually disconnect → See `disconnected` (no auto-reconnect)

### Test Error Handling
1. Revoke Bluetooth permission → See permission error
2. Turn off Bluetooth → See Bluetooth off error
3. Try to connect out of range → See connection timeout error

### Test Background Service
1. Start background service
2. Kill the app (swipe away)
3. Check notification → Service still running
4. Tap "Find My Ring" in notification → Ring vibrates

### Test Reconnection
1. Connect device
2. Start background service
3. Turn off ring or move out of range
4. Watch reconnection attempts with exponential backoff
5. Bring ring back in range → Automatic reconnection

## Code Examples

### Monitoring All Events
```dart
// Connection state
QringSdkFlutter.connectionStateStream.listen((state) {
  print('Connection: $state');
});

// Service state
QringSdkFlutter.serviceStateStream.listen((state) {
  print('Service: ${state.isRunning}, Connected: ${state.isConnected}');
});

// Errors
QringSdkFlutter.errorStream.listen((error) {
  print('Error: ${error.type.description} - ${error.message}');
});

// Battery
QringSdkFlutter.bleBatteryStream.listen((event) {
  print('Battery: ${event['batteryLevel']}%');
});
```

### Starting Background Service
```dart
// Request permissions first
await Permission.bluetoothScan.request();
await Permission.bluetoothConnect.request();
await Permission.notification.request();

// Start service
await QringSdkFlutter.startBackgroundService(deviceMac);
```

### Handling Errors
```dart
QringSdkFlutter.errorStream.listen((error) {
  switch (error.type) {
    case BleErrorType.permissionDenied:
      // Show permission dialog
      showPermissionDialog();
      break;
    case BleErrorType.bluetoothOff:
      // Show enable Bluetooth dialog
      showBluetoothDialog();
      break;
    case BleErrorType.connectionFailed:
      // Auto-reconnect will handle this
      showReconnectingIndicator();
      break;
    default:
      // Show generic error
      showErrorDialog(error.message);
  }
});
```

## Best Practices Demonstrated

1. **Permission Handling**: Always check and request permissions before BLE operations
2. **Error Display**: Show user-friendly error messages with actionable suggestions
3. **State Visualization**: Use colors and icons to make connection state clear
4. **Background Service**: Use for persistent connection when app is not in foreground
5. **Event Streams**: Subscribe to all relevant streams for complete state awareness
6. **Reconnection**: Let automatic reconnection handle transient failures
7. **User Feedback**: Provide clear feedback for all operations (success, error, progress)

## Navigation Flow

```
DeviceScanningScreen (Main)
├── Permissions Button → PermissionsScreen
├── Connection Manager Button → ConnectionManagerScreen
└── Connect to Device → HomeScreen
    └── (Existing functionality)
```

## Summary

The updated example app provides a complete demonstration of the production-grade BLE Connection Manager features, including:

- ✅ All connection states (idle, connecting, pairing, connected, reconnecting, disconnected)
- ✅ Comprehensive error handling with typed errors
- ✅ Reconnection status with attempt counter
- ✅ Permission request flow for Android 12+
- ✅ Background service start/stop controls
- ✅ Real-time battery updates
- ✅ Service state monitoring
- ✅ User-friendly error display
- ✅ Actionable error recovery

This serves as a reference implementation for integrating the QRing SDK with production-grade BLE connection management.
