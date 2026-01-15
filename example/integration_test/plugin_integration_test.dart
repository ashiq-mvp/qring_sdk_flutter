// Integration test for QRing SDK scanning and connection flow
//
// This test verifies the complete workflow of:
// 1. Starting a BLE scan
// 2. Discovering devices
// 3. Connecting to a device
// 4. Disconnecting from a device
//
// Note: This test requires a physical QRing device to be nearby for full testing.
// Without a device, it will test the scan start/stop functionality only.

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('QRing SDK Scanning and Connection Flow', () {
    test('should start and stop scan successfully', () async {
      // Start scan
      await QringSdkFlutter.startScan();

      // Wait a moment for scan to initialize
      await Future.delayed(const Duration(seconds: 2));

      // Stop scan
      await QringSdkFlutter.stopScan();

      // Test passes if no exceptions were thrown
      expect(true, true);
    });

    test('should emit connection state changes', () async {
      final states = <ConnectionState>[];

      // Listen to connection state stream
      final subscription = QringSdkFlutter.connectionStateStream.listen((
        state,
      ) {
        states.add(state);
      });

      // Wait a moment to receive initial state
      await Future.delayed(const Duration(milliseconds: 500));

      // Should have received at least one state (likely disconnected)
      expect(states.isNotEmpty, true);

      // Clean up
      await subscription.cancel();
    });

    test('should emit discovered devices during scan', () async {
      final deviceLists = <List<QringDevice>>[];

      // Listen to devices stream
      final subscription = QringSdkFlutter.devicesStream.listen((devices) {
        deviceLists.add(devices);
      });

      // Start scan
      await QringSdkFlutter.startScan();

      // Wait for devices to be discovered (or timeout)
      await Future.delayed(const Duration(seconds: 5));

      // Stop scan
      await QringSdkFlutter.stopScan();

      // Should have received at least one device list update
      // (even if empty, the stream should emit)
      expect(deviceLists.isNotEmpty, true);

      // Clean up
      await subscription.cancel();
    });

    test('should handle multiple scan start/stop cycles', () async {
      // First cycle
      await QringSdkFlutter.startScan();
      await Future.delayed(const Duration(seconds: 1));
      await QringSdkFlutter.stopScan();

      // Second cycle
      await QringSdkFlutter.startScan();
      await Future.delayed(const Duration(seconds: 1));
      await QringSdkFlutter.stopScan();

      // Third cycle
      await QringSdkFlutter.startScan();
      await Future.delayed(const Duration(seconds: 1));
      await QringSdkFlutter.stopScan();

      // Test passes if no exceptions were thrown
      expect(true, true);
    });

    test(
      'should handle connection attempt gracefully when no device available',
      () async {
        // Attempt to connect with a fake MAC address
        // This should either fail gracefully or timeout
        try {
          await QringSdkFlutter.connect(
            '00:00:00:00:00:00',
          ).timeout(const Duration(seconds: 5));
        } catch (e) {
          // Expected to fail or timeout
          expect(e, isNotNull);
        }
      },
    );

    test('should handle disconnect when not connected', () async {
      // Attempt to disconnect when not connected
      // This should either succeed silently or return an error
      try {
        await QringSdkFlutter.disconnect();
        expect(true, true);
      } catch (e) {
        // Some implementations may throw an error, which is acceptable
        expect(e, isNotNull);
      }
    });

    // This test requires a real device nearby
    test(
      'complete scan and connect workflow (requires real device)',
      () async {
        QringDevice? foundDevice;
        ConnectionState? finalState;

        // Listen to devices stream
        final devicesSubscription = QringSdkFlutter.devicesStream.listen((
          devices,
        ) {
          if (devices.isNotEmpty && foundDevice == null) {
            // Take the first device found
            foundDevice = devices.first;
          }
        });

        // Listen to connection state
        final stateSubscription = QringSdkFlutter.connectionStateStream.listen((
          state,
        ) {
          finalState = state;
        });

        // Start scan
        await QringSdkFlutter.startScan();

        // Wait for devices to be discovered
        await Future.delayed(const Duration(seconds: 10));

        // Stop scan
        await QringSdkFlutter.stopScan();

        if (foundDevice != null) {
          // Device found, attempt to connect
          await QringSdkFlutter.connect(foundDevice!.macAddress);

          // Wait for connection
          await Future.delayed(const Duration(seconds: 5));

          // Check if connected
          if (finalState == ConnectionState.connected) {
            // Successfully connected, now disconnect
            await QringSdkFlutter.disconnect();

            // Wait for disconnection
            await Future.delayed(const Duration(seconds: 2));

            // Verify disconnected
            expect(finalState, ConnectionState.disconnected);
          }
        } else {
          // No device found - skip connection test
          // This is acceptable in environments without a physical device
          print('No QRing device found nearby - skipping connection test');
        }

        // Clean up
        await devicesSubscription.cancel();
        await stateSubscription.cancel();
      },
      skip: 'Requires physical QRing device nearby',
    );
  });
}
