// Integration tests for Background Service with Persistent Notification
//
// These tests verify the complete workflow of the background service including:
// 1. Service lifecycle (start, stop, survive app kill)
// 2. Notification management and actions
// 3. Automatic reconnection
// 4. Service restart after system kill
// 5. Bluetooth state handling
// 6. Permission handling
//
// Note: These tests require:
// - A physical QRing device nearby for connection tests
// - Manual interaction for some tests (e.g., tapping notifications)
// - ADB access for system-level tests (e.g., force stopping service)

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Background Service Integration Tests', () {
    // Test 14.1: Service Lifecycle
    group('14.1 Service Lifecycle', () {
      test('should start service and show notification', () async {
        // Start service with a test device MAC
        final testDeviceMac = '00:11:22:33:44:55';

        try {
          await QringSdkFlutter.startBackgroundService(testDeviceMac);

          // Wait for service to start
          await Future.delayed(const Duration(seconds: 2));

          // Verify service is running
          final isRunning = await QringSdkFlutter.isServiceRunning();
          expect(
            isRunning,
            true,
            reason: 'Service should be running after start',
          );

          // Note: Notification visibility must be verified manually
          // The tester should check that a notification appears in the notification bar
        } finally {
          // Clean up: stop service
          await QringSdkFlutter.stopBackgroundService();
        }
      });

      test(
        'should keep service running when app is killed',
        () async {
          // This test requires manual verification:
          // 1. Start the service
          // 2. Kill the Flutter app (swipe away from recent apps)
          // 3. Check that notification remains visible
          // 4. Check that service continues running (via adb or system settings)

          final testDeviceMac = '00:11:22:33:44:55';

          await QringSdkFlutter.startBackgroundService(testDeviceMac);

          // Wait for service to start
          await Future.delayed(const Duration(seconds: 2));

          // Verify service is running
          final isRunning = await QringSdkFlutter.isServiceRunning();
          expect(isRunning, true);

          // Manual verification required:
          // - Kill the app from recent apps
          // - Verify notification remains visible
          // - Verify service continues running
          // - Reopen app and stop service

          // Note: This test will pass, but manual verification is required
          // Requirements: 1.3, 3.2
        },
        skip: 'Requires manual app kill and verification',
      );

      test('should maintain notification visibility', () async {
        final testDeviceMac = '00:11:22:33:44:55';

        try {
          await QringSdkFlutter.startBackgroundService(testDeviceMac);

          // Wait for service to start
          await Future.delayed(const Duration(seconds: 2));

          // Service should be running
          final isRunning = await QringSdkFlutter.isServiceRunning();
          expect(isRunning, true);

          // Wait longer to ensure notification persists
          await Future.delayed(const Duration(seconds: 5));

          // Service should still be running
          final stillRunning = await QringSdkFlutter.isServiceRunning();
          expect(stillRunning, true, reason: 'Service should remain running');

          // Manual verification: Check that notification is still visible
        } finally {
          await QringSdkFlutter.stopBackgroundService();
        }
      });
    });

    // Test 14.2: Find My Ring from Notification
    group('14.2 Find My Ring from Notification', () {
      test(
        'should execute Find My Ring command from notification',
        () async {
          // This test requires:
          // 1. A real QRing device connected
          // 2. Manual tap on "Find My Ring" button in notification

          // First, discover and connect to a device
          QringDevice? foundDevice;

          final devicesSubscription = QringSdkFlutter.devicesStream.listen((
            devices,
          ) {
            if (devices.isNotEmpty && foundDevice == null) {
              foundDevice = devices.first;
            }
          });

          try {
            // Start scan
            await QringSdkFlutter.startScan();
            await Future.delayed(const Duration(seconds: 10));
            await QringSdkFlutter.stopScan();

            if (foundDevice != null) {
              // Connect to device
              await QringSdkFlutter.connect(foundDevice!.macAddress);
              await Future.delayed(const Duration(seconds: 3));

              // Start background service with connected device
              await QringSdkFlutter.startBackgroundService(
                foundDevice!.macAddress,
              );

              await Future.delayed(const Duration(seconds: 2));

              // Manual verification required:
              // 1. Check notification shows "Find My Ring" button
              // 2. Tap the "Find My Ring" button
              // 3. Verify ring emits sound/vibration
              // 4. Verify notification shows feedback ("Ring activated")

              // Wait for manual interaction
              await Future.delayed(const Duration(seconds: 10));

              // Requirements: 4.2, 4.3, 4.4
            }
          } finally {
            await devicesSubscription.cancel();
            await QringSdkFlutter.stopBackgroundService();
            await QringSdkFlutter.disconnect();
          }
        },
        skip: 'Requires real device and manual notification interaction',
      );
    });

    // Test 14.3: Automatic Reconnection
    group('14.3 Automatic Reconnection', () {
      test(
        'should attempt reconnection after device disconnects',
        () async {
          // This test requires:
          // 1. A real QRing device
          // 2. Manual device power off/on

          QringDevice? foundDevice;
          final connectionStates = <ConnectionState>[];

          final devicesSubscription = QringSdkFlutter.devicesStream.listen((
            devices,
          ) {
            if (devices.isNotEmpty && foundDevice == null) {
              foundDevice = devices.first;
            }
          });

          final stateSubscription = QringSdkFlutter.connectionStateStream
              .listen((state) {
                connectionStates.add(state);
              });

          try {
            // Discover and connect to device
            await QringSdkFlutter.startScan();
            await Future.delayed(const Duration(seconds: 10));
            await QringSdkFlutter.stopScan();

            if (foundDevice != null) {
              await QringSdkFlutter.connect(foundDevice!.macAddress);
              await Future.delayed(const Duration(seconds: 3));

              // Start background service
              await QringSdkFlutter.startBackgroundService(
                foundDevice!.macAddress,
              );

              await Future.delayed(const Duration(seconds: 2));

              // Manual steps:
              // 1. Turn off the ring (simulate disconnection)
              // 2. Verify notification shows "Reconnecting..." status
              // 3. Wait for reconnection attempts (check notification)
              // 4. Turn on the ring
              // 5. Verify reconnection succeeds
              // 6. Verify notification shows "Connected" status

              // Wait for manual interaction and reconnection
              await Future.delayed(const Duration(seconds: 60));

              // Requirements: 7.1, 7.2, 7.3
            }
          } finally {
            await devicesSubscription.cancel();
            await stateSubscription.cancel();
            await QringSdkFlutter.stopBackgroundService();
          }
        },
        skip: 'Requires real device and manual power cycling',
      );
    });

    // Test 14.4: Service Restart After System Kill
    group('14.4 Service Restart After System Kill', () {
      test(
        'should restart service after force stop',
        () async {
          // This test requires ADB access:
          // adb shell am force-stop com.example.qring_sdk_flutter_example
          // or force stop via system settings

          final testDeviceMac = '00:11:22:33:44:55';

          await QringSdkFlutter.startBackgroundService(testDeviceMac);
          await Future.delayed(const Duration(seconds: 2));

          final isRunning = await QringSdkFlutter.isServiceRunning();
          expect(isRunning, true);

          // Manual steps:
          // 1. Run: adb shell am force-stop com.example.qring_sdk_flutter_example
          // 2. Wait a few seconds
          // 3. Check if service restarts automatically (notification reappears)
          // 4. Verify service attempts to reconnect to saved device
          // 5. Check logcat for service restart logs

          // Requirements: 1.4, 1.5, 10.4
        },
        skip: 'Requires ADB force stop and manual verification',
      );
    });

    // Test 14.5: Notification Tap Behavior
    group('14.5 Notification Tap Behavior', () {
      test(
        'should launch app when notification is tapped (app killed)',
        () async {
          final testDeviceMac = '00:11:22:33:44:55';

          await QringSdkFlutter.startBackgroundService(testDeviceMac);
          await Future.delayed(const Duration(seconds: 2));

          // Manual steps:
          // 1. Kill the Flutter app (swipe away from recent apps)
          // 2. Tap the notification
          // 3. Verify app launches and shows main screen

          // Requirements: 5.1, 5.2, 5.3
        },
        skip: 'Requires manual app kill and notification tap',
      );

      test(
        'should bring app to foreground when notification is tapped (app running)',
        () async {
          final testDeviceMac = '00:11:22:33:44:55';

          try {
            await QringSdkFlutter.startBackgroundService(testDeviceMac);
            await Future.delayed(const Duration(seconds: 2));

            // Manual steps:
            // 1. Put app in background (press home button)
            // 2. Tap the notification
            // 3. Verify app comes to foreground

            // Requirements: 5.1, 5.2, 5.3
          } finally {
            await QringSdkFlutter.stopBackgroundService();
          }
        },
        skip: 'Requires manual notification tap',
      );
    });

    // Test 14.6: Bluetooth State Changes
    group('14.6 Bluetooth State Changes', () {
      test(
        'should handle Bluetooth disable/enable',
        () async {
          // This test requires:
          // 1. A real QRing device connected
          // 2. Manual Bluetooth toggle

          QringDevice? foundDevice;

          final devicesSubscription = QringSdkFlutter.devicesStream.listen((
            devices,
          ) {
            if (devices.isNotEmpty && foundDevice == null) {
              foundDevice = devices.first;
            }
          });

          try {
            // Discover and connect to device
            await QringSdkFlutter.startScan();
            await Future.delayed(const Duration(seconds: 10));
            await QringSdkFlutter.stopScan();

            if (foundDevice != null) {
              await QringSdkFlutter.connect(foundDevice!.macAddress);
              await Future.delayed(const Duration(seconds: 3));

              // Start background service
              await QringSdkFlutter.startBackgroundService(
                foundDevice!.macAddress,
              );

              await Future.delayed(const Duration(seconds: 2));

              // Manual steps:
              // 1. Disable Bluetooth via system settings
              // 2. Verify reconnection attempts pause (check logcat)
              // 3. Enable Bluetooth
              // 4. Verify reconnection resumes immediately
              // 5. Verify device reconnects

              // Wait for manual Bluetooth toggling
              await Future.delayed(const Duration(seconds: 60));

              // Requirements: 7.4, 7.5
            }
          } finally {
            await devicesSubscription.cancel();
            await QringSdkFlutter.stopBackgroundService();
          }
        },
        skip: 'Requires real device and manual Bluetooth toggle',
      );
    });

    // Test 14.7: Permission Handling
    group('14.7 Permission Handling', () {
      test(
        'should handle missing Bluetooth permissions',
        () async {
          // This test requires:
          // 1. Revoke Bluetooth permissions via system settings
          // 2. Attempt to start service

          final testDeviceMac = '00:11:22:33:44:55';

          // Manual steps:
          // 1. Go to app settings
          // 2. Revoke Bluetooth permissions (BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
          // 3. Attempt to start service
          // 4. Verify error notification appears
          // 5. Verify service stops
          // 6. Check logcat for permission error logs

          try {
            await QringSdkFlutter.startBackgroundService(testDeviceMac);

            // If permissions are missing, service should fail to start
            // or show error notification
            await Future.delayed(const Duration(seconds: 3));

            // Requirements: 8.5
          } catch (e) {
            // Expected to fail if permissions are revoked
            expect(e, isNotNull);
          }
        },
        skip: 'Requires manual permission revocation',
      );
    });
  });
}
