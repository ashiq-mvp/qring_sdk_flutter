# QRing SDK Flutter Plugin - Quick Setup Guide

This guide will help you get started with the QRing SDK Flutter plugin quickly.

## 📚 Documentation Overview

The plugin includes comprehensive documentation:

- **[README.md](README.md)** - Main documentation with installation, usage examples, and troubleshooting
- **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)** - Detailed integration guide with architecture and testing
- **[CHANGELOG.md](CHANGELOG.md)** - Version history and release notes
- **[example/README.md](example/README.md)** - Example app documentation

## 🚀 Quick Start (5 Minutes)

### 1. Add Dependency

```yaml
# pubspec.yaml
dependencies:
  qring_sdk_flutter: ^0.0.1
```

```bash
flutter pub get
```

### 2. Configure Android

**Minimum SDK** (`android/app/build.gradle`):
```gradle
minSdkVersion 23
```

**Permissions** (`android/app/src/main/AndroidManifest.xml`):
```xml
<!-- Bluetooth permissions -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Location permission (required for BLE) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Feature declaration -->
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

### 3. Request Permissions

Add `permission_handler` to your `pubspec.yaml`:
```yaml
dependencies:
  permission_handler: ^11.0.0
```

Request permissions in your app:
```dart
import 'package:permission_handler/permission_handler.dart';

Future<void> requestPermissions() async {
  await [
    Permission.bluetoothScan,
    Permission.bluetoothConnect,
    Permission.locationWhenInUse,
  ].request();
}
```

### 4. Use the Plugin

```dart
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

// Scan for devices
await QringSdkFlutter.startScan();

// Listen for devices
QringSdkFlutter.devicesStream.listen((devices) {
  print('Found ${devices.length} devices');
});

// Connect to a device
await QringSdkFlutter.connect('AA:BB:CC:DD:EE:FF');

// Use features
await QringSdkFlutter.findRing();
int battery = await QringSdkFlutter.getBattery();
```

## 📖 Next Steps

### Learn More
- Read the [README.md](README.md) for detailed usage examples
- Check the [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for architecture details
- Run the example app: `cd example && flutter run`

### Key Features to Explore
1. **Device Discovery** - Scan and connect to QC Ring devices
2. **Find My Ring** - Locate your device with vibration
3. **Health Data** - Sync steps, heart rate, sleep, SpO2, blood pressure
4. **Manual Measurements** - Take on-demand health measurements
5. **Continuous Monitoring** - Configure automatic periodic measurements
6. **Exercise Tracking** - Track workouts with real-time metrics
7. **Firmware Updates** - Update device firmware over-the-air

### Example App
The plugin includes a comprehensive example app demonstrating all features:

```bash
cd example
flutter pub get
flutter run
```

The example app includes:
- Device scanning and connection UI
- Find My Ring button
- Battery and device info display
- Manual health measurements
- Historical data synchronization
- Continuous monitoring settings
- Display and user settings
- Exercise tracking
- Comprehensive error handling

## 🔧 Troubleshooting

### Common Issues

**Can't find devices?**
- Enable Bluetooth
- Grant all permissions
- Enable location services
- Ensure ring is charged and nearby

**Connection fails?**
- Check device is in range
- Stop scanning before connecting
- Verify permissions are granted

**Permissions not working?**
- Use `permission_handler` plugin for runtime requests
- Check AndroidManifest.xml has all required permissions
- Manually grant permissions in device settings if needed

### Get Help
- Check [README.md](README.md) troubleshooting section
- Review [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for detailed guidance
- Open an issue on GitHub
- Check the example app for working code

## 📱 Platform Support

| Platform | Status | Version |
|----------|--------|---------|
| Android  | ✅ Supported | 6.0+ (API 23+) |
| iOS      | ⏳ Planned | Coming soon |

## 📄 License

MIT License - See [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please read the contributing guidelines before submitting pull requests.

---

**Ready to build?** Start with the [README.md](README.md) for comprehensive documentation!
