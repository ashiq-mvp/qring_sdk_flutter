# QRing SDK Flutter Example App

This example application demonstrates how to use the QRing SDK Flutter plugin to interact with QC smart ring devices.

## Features

The example app showcases all major features of the QRing SDK:

### 1. Device Scanning and Connection
- Scan for nearby QRing devices via Bluetooth Low Energy (BLE)
- View discovered devices with signal strength indicators
- Connect to and disconnect from devices
- Real-time connection status monitoring

### 2. Quick Actions
- **Find My Ring**: Make the ring vibrate to help locate it
- **Battery Level**: View current battery percentage
- **Device Information**: Display firmware version, hardware version, and supported features

### 3. Health Data
The app provides two tabs for health data management:

#### Manual Measurements
- Heart Rate measurement
- Blood Pressure measurement
- Blood Oxygen (SpO2) measurement
- Temperature measurement
- Real-time measurement results
- Measurement history

#### Data Synchronization
- Sync historical data for up to 7 days
- Step count, distance, and calories
- Heart rate data with statistics
- Sleep data with stage breakdown
- Blood oxygen measurements
- Blood pressure readings

### 4. Settings
Configure device and monitoring settings:

#### Continuous Monitoring
- Enable/disable continuous heart rate monitoring
- Set heart rate monitoring interval (10, 15, 20, 30, or 60 minutes)
- Enable/disable continuous blood oxygen monitoring
- Enable/disable continuous blood pressure monitoring

#### Display Settings
- Toggle display on/off
- Set left/right hand orientation
- Adjust screen brightness
- Configure Do Not Disturb mode
- Set screen-on time window

#### User Profile
- Set age, height, weight, and gender
- Configure user ID
- Factory reset device

### 5. Exercise Tracking
- Start exercise sessions with 20+ exercise types
- Real-time metrics during exercise:
  - Duration
  - Heart Rate
  - Steps
  - Distance
  - Calories burned
- Pause, resume, and stop exercise sessions
- View exercise summary after completion

## Getting Started

### Prerequisites

1. **Flutter SDK**: Install Flutter 3.0 or higher
2. **Android Device**: Android 6.0 (API level 23) or higher with Bluetooth support
3. **QRing Device**: A QC smart ring device for testing

### Required Permissions

The app requires the following permissions:

- `BLUETOOTH_SCAN` - To scan for nearby devices (Android 12+)
- `BLUETOOTH_CONNECT` - To connect to devices (Android 12+)
- `ACCESS_FINE_LOCATION` - Required for BLE scanning
- `BLUETOOTH` - Legacy Bluetooth permission
- `BLUETOOTH_ADMIN` - Legacy Bluetooth admin permission

These permissions are automatically requested when you start scanning for devices.

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd qring_sdk_flutter/example
   ```

2. Install dependencies:
   ```bash
   flutter pub get
   ```

3. Run the app:
   ```bash
   flutter run
   ```

## Usage Guide

### Connecting to a Device

1. Launch the app - you'll see the Device Scanner screen
2. Tap **Start Scan** to begin searching for nearby QRing devices
3. Grant Bluetooth and Location permissions when prompted
4. Wait for devices to appear in the list
5. Tap **Connect** on your device
6. Once connected, you'll be taken to the Home screen

### Using Quick Actions

1. **Find My Ring**: Tap the large "Find My Ring" button to make your ring vibrate
2. **Battery Level**: View current battery percentage (refreshes automatically)
3. **Device Info**: See firmware version, hardware version, and supported features
4. **Disconnect**: Tap the red "Disconnect Device" button at the bottom

### Taking Manual Measurements

1. Navigate to the **Health Data** tab
2. Select the **Manual Measurements** tab
3. Tap one of the measurement buttons:
   - Heart Rate
   - Blood Pressure
   - Blood Oxygen
   - Temperature
4. Wait for the measurement to complete
5. View results in the measurement card
6. Check measurement history below

### Syncing Historical Data

1. Navigate to the **Health Data** tab
2. Select the **Data Sync** tab
3. Choose a date using the date selector (Today, Yesterday, or up to 7 days ago)
4. Tap one of the sync buttons:
   - Steps
   - Heart Rate
   - Sleep
   - Blood Oxygen
   - Blood Pressure
5. View the synchronized data in cards below

### Configuring Settings

1. Navigate to the **Settings** tab
2. **Continuous Monitoring**:
   - Toggle switches to enable/disable monitoring
   - Adjust intervals using sliders or segmented buttons
   - Settings save automatically
3. **Display Settings**:
   - Adjust brightness, hand orientation, and Do Not Disturb
   - Set screen-on time window
   - Tap "Save Display Settings" to apply
4. **User Profile**:
   - Enter age, height, weight, and gender
   - Tap "Save User Info" to apply
   - Set user ID and tap "Save User ID"
   - Use "Factory Reset" to reset device (requires confirmation)

### Tracking Exercise

1. Navigate to the **Exercise** tab
2. Select an exercise type from the dropdown
3. Tap **Start Exercise**
4. View real-time metrics during the exercise
5. Use **Pause** and **Resume** as needed
6. Tap **Stop** to end the exercise
7. View the exercise summary

## Troubleshooting

### Connection Issues

**Problem**: Can't find devices when scanning
- Ensure Bluetooth is enabled on your phone
- Grant all required permissions
- Make sure the ring is charged and nearby
- Try restarting Bluetooth on your phone

**Problem**: Connection fails or drops
- Ensure the ring is within range (typically 10 meters)
- Check that the ring battery isn't too low
- Try disconnecting and reconnecting
- Restart the app if issues persist

### Permission Issues

**Problem**: Permission denied errors
- Go to your phone's Settings > Apps > QRing Example
- Grant all requested permissions manually
- Restart the app

### Measurement Issues

**Problem**: Measurements fail or timeout
- Ensure the ring is properly worn on your finger
- Keep your hand still during measurements
- Check that the ring is connected
- Ensure the ring battery isn't too low

### Data Sync Issues

**Problem**: No data returned when syncing
- Ensure you're syncing for a day when you wore the ring
- Check that the ring has collected data for that day
- Try syncing a different data type
- Ensure stable connection to the ring

## Code Structure

```
example/
├── lib/
│   ├── main.dart                          # App entry point
│   ├── screens/
│   │   ├── device_scanning_screen.dart    # Device discovery and connection
│   │   ├── home_screen.dart               # Main navigation screen
│   │   ├── quick_actions_screen.dart      # Find ring, battery, device info
│   │   ├── health_data_screen.dart        # Measurements and data sync
│   │   ├── settings_screen.dart           # Device and monitoring settings
│   │   └── exercise_screen.dart           # Exercise tracking
│   ├── widgets/
│   │   ├── error_card.dart                # Reusable error display widget
│   │   ├── loading_overlay.dart           # Loading indicator overlay
│   │   └── success_snackbar.dart          # Success/error snackbar helpers
│   └── utils/
│       └── responsive_helper.dart         # Responsive design utilities
└── README.md                              # This file
```

## Key Concepts

### Connection State Management
The app uses streams to monitor connection state changes:
```dart
QringSdkFlutter.connectionStateStream.listen((state) {
  // Handle connection state changes
});
```

### Device Discovery
Discovered devices are emitted through a stream:
```dart
QringSdkFlutter.devicesStream.listen((devices) {
  // Update UI with discovered devices
});
```

### Real-time Measurements
Measurement results stream in real-time:
```dart
QringHealthData.measurementStream.listen((measurement) {
  // Display measurement result
});
```

### Error Handling
All operations include comprehensive error handling:
```dart
try {
  await QringSdkFlutter.connect(macAddress);
} catch (e) {
  // Show user-friendly error message
}
```

## Best Practices

1. **Always check connection state** before performing operations
2. **Handle errors gracefully** and show user-friendly messages
3. **Request permissions** before attempting BLE operations
4. **Dispose of stream subscriptions** to prevent memory leaks
5. **Use loading indicators** for async operations
6. **Provide feedback** for user actions (success/error messages)

## Additional Resources

- [QRing SDK Flutter Plugin Documentation](../README.md)
- [Flutter Documentation](https://flutter.dev/docs)
- [Dart Documentation](https://dart.dev/guides)

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the main plugin README
3. Check existing GitHub issues
4. Create a new issue with detailed information

## License

This example app is part of the QRing SDK Flutter plugin and follows the same license.
