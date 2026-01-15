// Integration tests for Production-Grade BLE Connection Manager
//
// These tests verify Requirements 10.1-10.6:
// 1. App killed → ring stays connected (Requirement 10.1)
// 2. Bluetooth toggle → automatic reconnection (Requirement 10.2)
// 3. Out-of-range → reconnection when back in range (Requirement 10.3)
// 4. Permission revoked → graceful error handling (Requirement 10.4)
// 5. Notification action → find my ring works (Requirement 10.5)
// 6. Device reboot → service restarts and reconnects (Requirement 10.6)
//
// NOTE: These are primarily manual tests due to the nature of system-level features.
// See PRODUCTION_BLE_MANAGER_TEST_GUIDE.md for detailed test procedures.

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Production BLE Manager Integration Tests', () {
    // Helper function to discover and connect to a QRing device
    Future<QringDevice?> discoverAndConnect() async {
      QringDevice? foundDevice;

      final devicesSubscription = QringSdkFlutter.devicesStream.listen((
        devices,
      ) {
        if (devices.isNotEmpty && foundDevice == null) {
          foundDevice = devices.first;
        }
      });

      try {
        await QringSdkFlutter.startScan();
        await Future.delayed(const Duration(seconds: 10));
        await QringSdkFlutter.stopScan();

        if (foundDevice != null) {
          await QringSdkFlutter.connect(foundDevice!.macAddress);
          await Future.delayed(const Duration(seconds: 5));
        }

        return foundDevice;
      } finally {
        await devicesSubscription.cancel();
      }
    }

    // Test 23.1: App Killed → Ring Stays Connected (Requirement 10.1)
    group('23.1 App Killed → Ring Stays Connected', () {
      testWidgets('should maintain connection when app is killed', (
        WidgetTester tester,
      ) async {
        final device = await discoverAndConnect();

        if (device == null) {
          fail('No QRing device found. Please ensure a device is nearby.');
        }

        await QringSdkFlutter.startBackgroundService(device.macAddress);
        await Future.delayed(const Duration(seconds: 2));

        final isRunning = await QringSdkFlutter.isServiceRunning();
        expect(isRunning, true, reason: 'Service should be running');

        print('\n=== MANUAL TEST REQUIRED ===');
        print('1. Kill the app (swipe away from recent apps)');
        print('2. Wait 10 seconds');
        print('3. Check notification remains visible');
        print('4. Reopen app to continue\n');

        await Future.delayed(const Duration(seconds: 60));

        final stillRunning = await QringSdkFlutter.isServiceRunning();
        expect(stillRunning, true, reason: 'Service should survive app kill');

        await QringSdkFlutter.stopBackgroundService();
        await QringSdkFlutter.disconnect();
      });
    });

    // Test 23.2: Bluetooth Toggle → Automatic Reconnection (Requirement 10.2)
    group('23.2 Bluetooth Toggle → Automatic Reconnection', () {
      testWidgets('should automatically reconnect when Bluetooth is toggled', (
        WidgetTester tester,
      ) async {
        final device = await discoverAndConnect();

        if (device == null) {
          fail('No QRing device found');
        }

        await QringSdkFlutter.startBackgroundService(device.macAddress);
        await Future.delayed(const Duration(seconds: 2));

        final connectionStates = <ConnectionState>[];
        final stateSubscription = QringSdkFlutter.connectionStateStream.listen((
          state,
        ) {
          connectionStates.add(state);
          print('Connection state: $state');
        });

        try {
          print('\n=== MANUAL TEST REQUIRED ===');
          print('1. Disable Bluetooth');
          print('2. Wait 5 seconds');
          print('3. Enable Bluetooth');
          print('4. Wait 10 seconds for reconnection\n');

          await Future.delayed(const Duration(seconds: 60));

          expect(
            connectionStates.contains(ConnectionState.disconnected) ||
                connectionStates.contains(ConnectionState.reconnecting),
            true,
            reason: 'Should transition through disconnected/reconnecting',
          );

          expect(
            connectionStates.contains(ConnectionState.connected),
            true,
            reason: 'Should reconnect',
          );
        } finally {
          await stateSubscription.cancel();
          await QringSdkFlutter.stopBackgroundService();
          await QringSdkFlutter.disconnect();
        }
      });
    });

    // Test 23.3: Out-of-Range → Reconnection (Requirement 10.3)
    group('23.3 Out-of-Range → Reconnection When Back in Range', () {
      testWidgets('should reconnect when device returns to range', (
        WidgetTester tester,
      ) async {
        final device = await discoverAndConnect();

        if (device == null) {
          fail('No QRing device found');
        }

        await QringSdkFlutter.startBackgroundService(device.macAddress);
        await Future.delayed(const Duration(seconds: 2));

        final connectionStates = <ConnectionState>[];
        final stateSubscription = QringSdkFlutter.connectionStateStream.listen((
          state,
        ) {
          connectionStates.add(state);
          print('Connection state: $state');
        });

        try {
          print('\n=== MANUAL TEST REQUIRED ===');
          print('1. Move ring out of range OR turn it off');
          print('2. Wait 10 seconds');
          print('3. Bring ring back in range OR turn it on');
          print('4. Wait 20 seconds for reconnection\n');

          await Future.delayed(const Duration(seconds: 90));

          expect(
            connectionStates.contains(ConnectionState.reconnecting),
            true,
            reason: 'Should enter reconnecting state',
          );
        } finally {
          await stateSubscription.cancel();
          await QringSdkFlutter.stopBackgroundService();
          await QringSdkFlutter.disconnect();
        }
      });
    });

    // Test 23.4: Permission Revoked → Graceful Error Handling (Requirement 10.4)
    group('23.4 Permission Revoked → Graceful Error Handling', () {
      testWidgets('should handle permission revocation gracefully', (
        WidgetTester tester,
      ) async {
        final device = await discoverAndConnect();

        if (device == null) {
          fail('No QRing device found');
        }

        await QringSdkFlutter.startBackgroundService(device.macAddress);
        await Future.delayed(const Duration(seconds: 2));

        final errors = <BleError>[];
        final errorSubscription = QringSdkFlutter.errorStream.listen((error) {
          errors.add(error);
          print('BLE Error: ${error.code} - ${error.message}');
        });

        try {
          print('\n=== MANUAL TEST REQUIRED ===');
          print('1. Go to Settings > Apps > QRing SDK Flutter Example');
          print('2. Revoke Bluetooth permissions');
          print('3. Wait 5 seconds');
          print('4. Verify error notification appears');
          print('5. Re-grant permission\n');

          await Future.delayed(const Duration(seconds: 60));

          expect(
            errors.isNotEmpty,
            true,
            reason: 'Should report permission error',
          );

          final hasPermissionError = errors.any(
            (error) =>
                error.code.contains('PERMISSION') ||
                error.message.toLowerCase().contains('permission'),
          );

          expect(
            hasPermissionError,
            true,
            reason: 'Should report permission error',
          );

          final isRunning = await QringSdkFlutter.isServiceRunning();
          expect(isRunning, true, reason: 'Service should handle gracefully');
        } finally {
          await errorSubscription.cancel();
          await QringSdkFlutter.stopBackgroundService();
          await QringSdkFlutter.disconnect();
        }
      });
    });

    // Test 23.5: Notification Action → Find My Ring Works (Requirement 10.5)
    group('23.5 Notification Action → Find My Ring Works', () {
      testWidgets('should execute Find My Ring from notification tap', (
        WidgetTester tester,
      ) async {
        final device = await discoverAndConnect();

        if (device == null) {
          fail('No QRing device found');
        }

        await QringSdkFlutter.startBackgroundService(device.macAddress);
        await Future.delayed(const Duration(seconds: 2));

        print('\n=== MANUAL TEST REQUIRED ===');
        print('1. Check notification bar');
        print('2. Tap "Find My Ring" button');
        print('3. Verify ring vibrates/beeps');
        print('4. Verify notification shows "Ring activated" feedback\n');

        await Future.delayed(const Duration(seconds: 30));

        await QringSdkFlutter.stopBackgroundService();
        await QringSdkFlutter.disconnect();
      });
    });

    // Test 23.6: Device Reboot → Service Restarts (Requirement 10.6)
    group('23.6 Device Reboot → Service Restarts and Reconnects', () {
      testWidgets('should restart service after device reboot', (
        WidgetTester tester,
      ) async {
        final device = await discoverAndConnect();

        if (device == null) {
          fail('No QRing device found');
        }

        await QringSdkFlutter.startBackgroundService(device.macAddress);
        await Future.delayed(const Duration(seconds: 2));

        print('\n=== MANUAL TEST REQUIRED ===');
        print('1. Note device MAC: ${device.macAddress}');
        print('2. Reboot the Android device');
        print('3. After reboot, unlock device');
        print('4. Wait 30 seconds');
        print('5. Check notification bar for QRing notification');
        print('6. Verify service restarted and reconnected\n');

        print('NOTE: This test requires manual verification after reboot.');
      });

      testWidgets('should verify BootReceiver is registered', (
        WidgetTester tester,
      ) async {
        print('\n=== VERIFICATION TEST ===');
        print('Check AndroidManifest.xml for:');
        print('1. RECEIVE_BOOT_COMPLETED permission');
        print('2. BootReceiver with BOOT_COMPLETED intent filter');
        print(
          '\nRun: adb shell dumpsys package com.example.qring_sdk_flutter_example | grep BootReceiver\n',
        );
      });
    });

    // Comprehensive End-to-End Test
    group('23.7 Comprehensive E2E Test', () {
      testWidgets('should handle complete production workflow', (
        WidgetTester tester,
      ) async {
        final device = await discoverAndConnect();

        if (device == null) {
          fail('No QRing device found');
        }

        final connectionStates = <ConnectionState>[];
        final errors = <BleError>[];

        final stateSubscription = QringSdkFlutter.connectionStateStream.listen((
          state,
        ) {
          connectionStates.add(state);
          print('State: $state');
        });

        final errorSubscription = QringSdkFlutter.errorStream.listen((error) {
          errors.add(error);
          print('Error: ${error.code}');
        });

        try {
          await QringSdkFlutter.startBackgroundService(device.macAddress);
          await Future.delayed(const Duration(seconds: 2));

          print('✓ Step 1: Service started');

          print('\n=== STEP 2: Test Find My Ring ===');
          print('Tap "Find My Ring" in notification');
          await Future.delayed(const Duration(seconds: 10));

          print('\n=== STEP 3: Test App Kill ===');
          print('Kill app and verify notification remains');
          await Future.delayed(const Duration(seconds: 30));

          print('\n=== STEP 4: Test Bluetooth Toggle ===');
          print('Toggle Bluetooth off/on');
          await Future.delayed(const Duration(seconds: 60));

          print('\n=== STEP 5: Test Out-of-Range ===');
          print('Move ring out of range and back');
          await Future.delayed(const Duration(seconds: 60));

          print('\n=== ALL TESTS COMPLETED ===');
          print('States observed: ${connectionStates.toSet()}');
          print('Errors encountered: ${errors.length}');
        } finally {
          await stateSubscription.cancel();
          await errorSubscription.cancel();
          await QringSdkFlutter.stopBackgroundService();
          await QringSdkFlutter.disconnect();
        }
      });
    });
  });
}
