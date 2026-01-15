# Changelog

All notable changes to the QRing SDK Flutter plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - 2024-01-14

### Added

#### Core Features
- **Device Discovery**: BLE scanning for QC Ring devices with signal strength indicators
- **Connection Management**: Connect, disconnect, and monitor connection state in real-time
- **Find My Ring**: Trigger vibration to locate misplaced devices
- **Battery Monitoring**: Check device battery level (0-100%)
- **Device Information**: Retrieve firmware version, hardware version, and supported features

#### Health Data
- **Data Synchronization**: Sync historical data for up to 7 days
  - Step count, distance, and calories
  - Heart rate measurements
  - Sleep data with stage breakdown (deep, light, REM)
  - Blood oxygen (SpO2) measurements
  - Blood pressure readings
- **Manual Measurements**: On-demand health measurements
  - Heart rate
  - Blood pressure
  - Blood oxygen (SpO2)
  - Temperature
- **Real-time Notifications**: Automatic measurement notifications when continuous monitoring is enabled

#### Device Settings
- **Continuous Monitoring**: Configure automatic periodic measurements
  - Heart rate (10, 15, 20, 30, or 60 minute intervals)
  - Blood oxygen
  - Blood pressure
- **Display Settings**: Customize device display
  - Brightness control
  - Left/right hand orientation
  - Do Not Disturb mode
  - Screen-on time window
- **User Profile**: Configure user information (age, height, weight, gender)
- **Factory Reset**: Reset device to factory defaults

#### Exercise Tracking
- **Exercise Sessions**: Track workouts with 20+ exercise types
- **Real-time Metrics**: Live updates during exercise
  - Duration
  - Heart rate
  - Steps
  - Distance
  - Calories burned
- **Exercise Controls**: Start, pause, resume, and stop sessions
- **Exercise Summary**: Detailed summary after completion

#### Firmware Management
- **Firmware Validation**: Validate firmware files before update
- **OTA Updates**: Over-the-air firmware updates via Bluetooth
- **Progress Tracking**: Real-time update progress monitoring

#### Permission Management
- **Permission Checking**: Check status of all required permissions
- **Permission Information**: Get list of missing permissions
- **Android 12+ Support**: Support for new Bluetooth permissions

#### Documentation
- Comprehensive README with installation instructions and usage examples
- Complete API documentation with dartdoc comments
- Integration guide with architecture details and testing approach
- Example application demonstrating all features

#### Testing
- Unit tests for data models and core functionality
- Property-based tests for universal correctness properties
- Integration tests for end-to-end workflows
- 80%+ code coverage

### Platform Support
- Android 6.0+ (API Level 23+)
- iOS support planned for future releases

### Dependencies
- Flutter SDK: >=3.3.0
- Dart SDK: ^3.10.7
- QC Wireless SDK: 1.0.0.4 (included)

### Known Limitations
- iOS platform not yet supported
- Permission request method returns information only (use permission_handler plugin for actual requests)
- Requires physical QC Ring device for full functionality testing

### Notes
- This is the initial release of the QRing SDK Flutter plugin
- All core features from the QC Wireless SDK are implemented
- Comprehensive example app included demonstrating all features
- Full API documentation available at pub.dev
