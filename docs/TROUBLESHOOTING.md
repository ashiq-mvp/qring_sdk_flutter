# Background Service Troubleshooting Guide

## Overview

This guide helps you diagnose and resolve common issues with the QRing Background Service. Issues are organized by category with symptoms, causes, and solutions.

## Quick Diagnostic Checklist

Before diving into specific issues, check these common requirements:

- [ ] Bluetooth is enabled on the device
- [ ] All required permissions are granted
- [ ] Device is running Android 8.0 (API 26) or higher
- [ ] QRing device is powered on and in range
- [ ] App has been granted notification permissions (Android 13+)
- [ ] Battery optimization is disabled for the app (optional but recommended)

---

## Common Issues

### 1. Service Won't Start

#### Symptoms
- `startBackgroundService()` throws an exception
- No notification appears
- Service status remains "not running"

#### Possible Causes and Solutions

**A. Missing Permissions**

**Symptoms:**
- Error message: "Permission denied"
- PlatformException with code `PERMISSION_DENIED`

**Solution:**
```dart
// Check and request permissions before starting service
import 'package:permission_handler/permission_handler.dart';

Future<bool> checkPermissions() async {
  if (Platform.isAndroid) {
    final androidInfo = await DeviceInfoPlugin().androidInfo;
    
    if (androidInfo.version.sdkInt >= 31) {
      // Android 12+
      final bluetoothConnect = await Permission.bluetoothConnect.request();
      final bluetoothScan = await Permission.bluetoothScan.request();
      
      if (androidInfo.version.sdkInt >= 33) {
        // Android 13+
        final notification = await Permission.notification.request();
        return bluetoothConnect.isGranted && 
               bluetoothScan.isGranted && 
               notification.isGranted;
      }
      
      return bluetoothConnect.isGranted && bluetoothScan.isGranted;
    } else {
      // Android < 12
      final bluetooth = await Permission.bluetooth.request();
      final location = await Permission.location.request();
      return bluetooth.isGranted && location.isGranted;
    }
  }
  return true;
}
```

**B. Bluetooth Disabled**

**Symptoms:**
- Error message: "Bluetooth is disabled"
- PlatformException with code `BLUETOOTH_DISABLED`

**Solution:**
```dart
// Check Bluetooth state before starting service
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

Future<void> startServiceWithBluetoothCheck(String deviceMac) async {
  // Check if Bluetooth is on
  final isOn = await FlutterBluePlus.isOn;
  
  if (!isOn) {
    // Prompt user to enable Bluetooth
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Bluetooth Required'),
        content: const Text('Please enable Bluetooth to use the background service.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
    return;
  }
  
  // Start service
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
}
```


**C. Invalid Device MAC Address**

**Symptoms:**
- Service starts but immediately stops
- Error in logs: "Invalid MAC address"

**Solution:**
```dart
// Validate MAC address format
bool isValidMacAddress(String mac) {
  final regex = RegExp(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$');
  return regex.hasMatch(mac);
}

Future<void> startServiceSafely(String deviceMac) async {
  if (!isValidMacAddress(deviceMac)) {
    throw ArgumentError('Invalid MAC address format: $deviceMac');
  }
  
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
}
```

**D. Service Already Running**

**Symptoms:**
- Error message: "Service already running"
- Duplicate notifications appear

**Solution:**
```dart
// Check if service is running before starting
Future<void> startServiceIfNotRunning(String deviceMac) async {
  final isRunning = await QringSdkFlutter.instance.isServiceRunning();
  
  if (isRunning) {
    print('Service is already running');
    return;
  }
  
  await QringSdkFlutter.instance.startBackgroundService(deviceMac);
}
```

---

### 2. Device Won't Connect

#### Symptoms
- Service starts but device remains disconnected
- Notification shows "Reconnecting..." indefinitely
- No connection established after multiple attempts

#### Possible Causes and Solutions

**A. Device Out of Range**

**Symptoms:**
- Connection attempts fail consistently
- Logs show "Device not found" or "Connection timeout"

**Solution:**
- Ensure QRing device is within 10 meters (30 feet) of the phone
- Remove obstacles between phone and ring (walls, metal objects)
- Check that the ring is powered on (LED indicator)
- Try moving closer to the device

**B. Device Already Connected to Another App**

**Symptoms:**
- Connection fails with "Device busy" error
- Device connects to other apps but not background service

**Solution:**
```bash
# Disconnect device from all apps
adb shell am force-stop com.example.qring_sdk_flutter

# Clear Bluetooth cache (requires root or system app)
adb shell pm clear com.android.bluetooth
```

Or manually:
1. Go to Android Settings → Bluetooth
2. Find the QRing device
3. Tap "Forget" or "Unpair"
4. Restart the background service

**C. Bluetooth Pairing Issues**

**Symptoms:**
- Connection fails with "Pairing required" error
- Device shows as unpaired in Bluetooth settings

**Solution:**
1. Open Android Bluetooth settings
2. Pair with the QRing device manually
3. Accept any pairing requests
4. Restart the background service

**D. SDK Initialization Failure**

**Symptoms:**
- Logs show "SDK initialization failed"
- Connection attempts fail immediately

**Solution:**
```bash
# View SDK initialization logs
adb logcat -s QRingBackgroundService ServiceConnectionManager

# Look for initialization errors
# Common issues:
# - Missing SDK files
# - Incompatible SDK version
# - Corrupted SDK cache
```

If SDK initialization fails:
1. Uninstall and reinstall the app
2. Clear app data: Settings → Apps → Your App → Storage → Clear Data
3. Restart the device

---

### 3. Service Stops Unexpectedly

#### Symptoms
- Service runs for a while then stops
- Notification disappears
- No error messages in logs

#### Possible Causes and Solutions

**A. Battery Optimization Killing Service**

**Symptoms:**
- Service stops after screen turns off
- Service stops after a few minutes of inactivity
- Service doesn't restart automatically

**Solution:**
```dart
// Prompt user to disable battery optimization
import 'package:battery_optimization/battery_optimization.dart';

Future<void> requestBatteryOptimizationExemption() async {
  final isIgnoring = await BatteryOptimization.isIgnoringBatteryOptimizations();
  
  if (!isIgnoring) {
    await BatteryOptimization.openBatteryOptimizationSettings();
  }
}
```

Manual steps:
1. Go to Settings → Apps → Your App
2. Tap "Battery" or "Battery usage"
3. Select "Unrestricted" or "Don't optimize"
4. Restart the app

**B. Low Memory Conditions**

**Symptoms:**
- Service stops when many apps are running
- Logs show "onLowMemory" or "onTrimMemory"

**Solution:**
- Close unused apps to free memory
- Restart the device
- The service should automatically restart (START_STICKY)

**C. Manual Force Stop**

**Symptoms:**
- Service stops immediately
- User or system force-stopped the app

**Solution:**
- Restart the service from the app
- Avoid force-stopping the app from system settings
- Service will not auto-restart after force stop (Android limitation)

---

### 4. Notification Issues

#### Symptoms
- Notification doesn't appear
- Notification appears but has no actions
- Notification tap doesn't open app

#### Possible Causes and Solutions

**A. Notification Permission Denied (Android 13+)**

**Symptoms:**
- Service runs but no notification visible
- Logs show "Notification permission denied"

**Solution:**
```dart
// Request notification permission on Android 13+
import 'package:permission_handler/permission_handler.dart';

Future<void> requestNotificationPermission() async {
  if (Platform.isAndroid) {
    final androidInfo = await DeviceInfoPlugin().androidInfo;
    
    if (androidInfo.version.sdkInt >= 33) {
      final status = await Permission.notification.request();
      
      if (status.isDenied) {
        // Show explanation and open settings
        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Notification Permission Required'),
            content: const Text(
              'The background service requires notification permission to display connection status.',
            ),
            actions: [
              TextButton(
                onPressed: () => openAppSettings(),
                child: const Text('Open Settings'),
              ),
            ],
          ),
        );
      }
    }
  }
}
```

**B. Notification Channel Disabled**

**Symptoms:**
- Notification appears but is silent/hidden
- User disabled notification channel

**Solution:**
1. Go to Settings → Apps → Your App → Notifications
2. Find "Smart Ring Service" channel
3. Enable the channel
4. Restart the service

**C. "Find My Ring" Button Not Working**

**Symptoms:**
- Button appears but nothing happens when tapped
- No feedback after tapping button

**Solution:**
```bash
# Check notification action logs
adb logcat -s NotificationActionReceiver ServiceCommandHandler

# Verify device is connected
adb logcat | grep "Device connected"
```

Common causes:
- Device is disconnected (button only works when connected)
- Permission issue with BroadcastReceiver
- SDK command execution failure

---

### 5. Reconnection Issues

#### Symptoms
- Device disconnects and never reconnects
- Reconnection attempts fail repeatedly
- Notification shows high reconnection attempt count

#### Possible Causes and Solutions

**A. Bluetooth Disabled**

**Symptoms:**
- Reconnection paused
- Notification shows "Bluetooth disabled"

**Solution:**
- Enable Bluetooth in Android settings
- Service will automatically resume reconnection

**B. Device Powered Off**

**Symptoms:**
- Reconnection attempts continue indefinitely
- Device never reconnects

**Solution:**
- Power on the QRing device
- Ensure device is charged
- Service will automatically connect when device is available

**C. Exponential Backoff Delays**

**Symptoms:**
- Long delays between reconnection attempts
- Notification shows "Attempt 15+"

**Solution:**
This is expected behavior after many failed attempts:
- Attempts 1-5: 10 seconds between retries
- Attempts 6-10: 30 seconds between retries
- Attempts 11+: 60 seconds between retries
- Maximum delay: 5 minutes

To reset:
1. Stop the service
2. Ensure device is available
3. Restart the service

**D. Saved Device MAC Incorrect**

**Symptoms:**
- Service restarts but connects to wrong device
- Reconnection fails with "Device not found"

**Solution:**
```dart
// Clear saved state and restart with correct MAC
await QringSdkFlutter.instance.stopBackgroundService();
await Future.delayed(const Duration(seconds: 1));
await QringSdkFlutter.instance.startBackgroundService(correctDeviceMac);
```

---

### 6. Flutter Integration Issues

#### Symptoms
- MethodChannel calls fail
- EventChannel doesn't emit events
- Service state not synchronized with app

#### Possible Causes and Solutions

**A. Platform Channel Not Initialized**

**Symptoms:**
- MissingPluginException
- "No implementation found" error

**Solution:**
```dart
// Ensure plugin is properly initialized
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(MyApp());
}
```

**B. EventChannel Stream Not Listening**

**Symptoms:**
- No state updates in Flutter app
- Service state changes not reflected in UI

**Solution:**
```dart
// Properly set up stream listener
class MyWidget extends StatefulWidget {
  @override
  State<MyWidget> createState() => _MyWidgetState();
}

class _MyWidgetState extends State<MyWidget> {
  StreamSubscription<ServiceState>? _subscription;
  
  @override
  void initState() {
    super.initState();
    
    // Start listening to service state
    _subscription = QringSdkFlutter.instance.serviceStateStream.listen(
      (state) {
        setState(() {
          // Update UI with new state
        });
      },
      onError: (error) {
        print('Stream error: $error');
      },
    );
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

**C. Service State Desync**

**Symptoms:**
- `isServiceRunning()` returns incorrect value
- UI shows wrong service state

**Solution:**
```dart
// Refresh service state
Future<void> refreshServiceState() async {
  final isRunning = await QringSdkFlutter.instance.isServiceRunning();
  setState(() {
    _isServiceRunning = isRunning;
  });
}

// Call on app resume
@override
void didChangeAppLifecycleState(AppLifecycleState state) {
  if (state == AppLifecycleState.resumed) {
    refreshServiceState();
  }
}
```

---

## Debugging Tools

### Logcat Filters

View all QRing-related logs:
```bash
adb logcat | grep -i qring
```

View service-specific logs:
```bash
adb logcat -s QRingBackgroundService
```

View connection logs:
```bash
adb logcat -s ServiceConnectionManager
```

View notification logs:
```bash
adb logcat -s ServiceNotificationManager NotificationActionReceiver
```

View SDK logs:
```bash
adb logcat -s BleOperateManager CommandHandle
```

### Service Status Commands

Check if service is running:
```bash
adb shell dumpsys activity services | grep QRingBackgroundService
```

View service details:
```bash
adb shell dumpsys activity services com.example.qring_sdk_flutter/.QRingBackgroundService
```

Force stop service:
```bash
adb shell am stopservice com.example.qring_sdk_flutter/.QRingBackgroundService
```

Start service manually:
```bash
adb shell am startforegroundservice \
  -n com.example.qring_sdk_flutter/.QRingBackgroundService \
  --es device_mac "AA:BB:CC:DD:EE:FF"
```

### Notification Inspection

View active notifications:
```bash
adb shell dumpsys notification | grep -A 20 "qring_service_channel"
```

View notification settings:
```bash
adb shell dumpsys notification | grep -A 10 "NotificationChannel"
```

### Bluetooth Debugging

View Bluetooth state:
```bash
adb shell dumpsys bluetooth_manager
```

View connected devices:
```bash
adb shell dumpsys bluetooth_manager | grep "Connected"
```

Enable Bluetooth HCI logging:
```bash
adb shell settings put secure bluetooth_hci_log 1
```

### Permission Debugging

Check granted permissions:
```bash
adb shell dumpsys package com.example.qring_sdk_flutter | grep permission
```

Grant permission manually:
```bash
# Bluetooth Connect (Android 12+)
adb shell pm grant com.example.qring_sdk_flutter android.permission.BLUETOOTH_CONNECT

# Notification (Android 13+)
adb shell pm grant com.example.qring_sdk_flutter android.permission.POST_NOTIFICATIONS
```

---

## Performance Issues

### High Battery Drain

**Symptoms:**
- App uses excessive battery
- Device gets warm
- Battery drains faster than expected

**Diagnosis:**
```bash
# Check battery stats
adb shell dumpsys batterystats | grep qring

# Check wake locks
adb shell dumpsys power | grep -A 5 "Wake Locks"
```

**Solutions:**
1. Ensure exponential backoff is working (check logs)
2. Verify wake locks are released when idle
3. Check for excessive reconnection attempts
4. Disable service when not needed

### High Memory Usage

**Symptoms:**
- App uses excessive memory
- Device slows down
- Service gets killed frequently

**Diagnosis:**
```bash
# Check memory usage
adb shell dumpsys meminfo com.example.qring_sdk_flutter
```

**Solutions:**
1. Restart the service
2. Clear app cache
3. Update to latest SDK version
4. Report issue if memory usage exceeds 50 MB

### Slow Reconnection

**Symptoms:**
- Takes long time to reconnect after disconnection
- Delays between reconnection attempts are too long

**Diagnosis:**
```bash
# Check reconnection logs
adb logcat -s ServiceConnectionManager | grep "Reconnect attempt"
```

**Solutions:**
1. This is expected behavior (exponential backoff)
2. To reset backoff: stop and restart service
3. Ensure Bluetooth is enabled
4. Move device closer to phone

---

## Error Codes Reference

### PlatformException Codes

| Code | Meaning | Solution |
|------|---------|----------|
| `PERMISSION_DENIED` | Required permission not granted | Request permissions before starting service |
| `BLUETOOTH_DISABLED` | Bluetooth is turned off | Enable Bluetooth in settings |
| `SERVICE_START_FAILED` | Failed to start foreground service | Check logs for specific error |
| `DEVICE_NOT_FOUND` | Cannot find device with given MAC | Verify MAC address and device is powered on |
| `CONNECTION_FAILED` | Failed to connect to device | Check Bluetooth, permissions, and device availability |
| `COMMAND_FAILED` | Command execution failed | Ensure device is connected |
| `NOT_CONNECTED` | Device is not connected | Wait for connection or reconnect |
| `UNSUPPORTED_PLATFORM` | Feature not available on this platform | Android-only feature |
| `INVALID_ARGUMENT` | Invalid parameter provided | Check method parameters |

---

## Getting Help

### Before Reporting an Issue

1. Check this troubleshooting guide
2. Review the [Flutter Integration Guide](FLUTTER_BACKGROUND_SERVICE_INTEGRATION.md)
3. Review the [Architecture Documentation](../android/BACKGROUND_SERVICE_ARCHITECTURE.md)
4. Collect relevant logs using the debugging tools above

### Information to Include

When reporting an issue, include:

1. **Device Information:**
   - Android version
   - Device manufacturer and model
   - App version

2. **Steps to Reproduce:**
   - Exact steps that cause the issue
   - Expected behavior
   - Actual behavior

3. **Logs:**
   ```bash
   # Collect comprehensive logs
   adb logcat -d > logcat.txt
   ```

4. **Service State:**
   ```bash
   # Dump service state
   adb shell dumpsys activity services com.example.qring_sdk_flutter/.QRingBackgroundService > service_dump.txt
   ```

5. **Permissions:**
   ```bash
   # Dump permissions
   adb shell dumpsys package com.example.qring_sdk_flutter | grep permission > permissions.txt
   ```

---

## Additional Resources

- [Flutter Integration Guide](FLUTTER_BACKGROUND_SERVICE_INTEGRATION.md)
- [Background Service Architecture](../android/BACKGROUND_SERVICE_ARCHITECTURE.md)
- [Integration Test Guide](../example/integration_test/BACKGROUND_SERVICE_TEST_GUIDE.md)
- [Android Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [Android Bluetooth Permissions](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions)
