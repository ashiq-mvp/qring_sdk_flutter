import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Device Discovery Properties', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');
    const EventChannel devicesChannel = EventChannel(
      'qring_sdk_flutter/devices',
    );

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(devicesChannel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(devicesChannel, null);
    });

    test('Property 2: Discovered Devices Appear in Stream - '
        'For any QC Ring device discovered during scanning, '
        'the device information should be emitted through devicesStream', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 2: Discovered Devices Appear in Stream
      // Validates: Requirements 1.2, 1.4

      // Setup mock method handler for scan
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
            if (methodCall.method == 'scan') {
              return null;
            }
            return null;
          });

      // Test with 100 iterations to verify property holds across many scenarios
      for (int iteration = 0; iteration < 100; iteration++) {
        // Generate random device data
        final deviceName =
            'QRing_${iteration}_${DateTime.now().millisecondsSinceEpoch % 1000}';
        final macAddress = _generateRandomMacAddress(iteration);
        final rssi = -50 - (iteration % 50); // RSSI between -50 and -100

        // Setup mock stream handler that emits discovered devices
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              devicesChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success([
                        {
                          'name': deviceName,
                          'macAddress': macAddress,
                          'rssi': rssi,
                        },
                      ]);
                    },
              ),
            );

        // Start listening to devices stream
        final devicesStreamFuture = QringSdkFlutter.devicesStream.first;

        // Trigger scan
        await QringSdkFlutter.startScan();

        // Wait for device to appear in stream
        final devices = await devicesStreamFuture.timeout(
          const Duration(seconds: 2),
          onTimeout: () => <QringDevice>[],
        );

        // Verify device appears in stream with correct information
        expect(
          devices,
          isNotEmpty,
          reason: 'Discovered device should appear in stream',
        );
        expect(
          devices.length,
          equals(1),
          reason: 'Should have exactly one device',
        );

        final device = devices.first;
        expect(
          device.name,
          equals(deviceName),
          reason: 'Device name should match',
        );
        expect(
          device.macAddress,
          equals(macAddress),
          reason: 'MAC address should match',
        );
        expect(device.rssi, equals(rssi), reason: 'RSSI should match');

        // Verify device information includes all required fields
        expect(
          device.name,
          isNotEmpty,
          reason: 'Device name should not be empty',
        );
        expect(
          device.macAddress,
          isNotEmpty,
          reason: 'MAC address should not be empty',
        );
        expect(
          device.rssi,
          isNegative,
          reason: 'RSSI should be negative (signal strength)',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test(
      'Property 2 (Multiple Devices): Multiple discovered devices all appear in stream',
      () async {
        // Test that when multiple devices are discovered, all appear in the stream

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'scan') {
                return null;
              }
              return null;
            });

        for (int iteration = 0; iteration < 50; iteration++) {
          // Generate multiple random devices
          final deviceCount = 2 + (iteration % 5); // 2-6 devices
          final deviceList = List.generate(deviceCount, (index) {
            return {
              'name': 'QRing_${iteration}_$index',
              'macAddress': _generateRandomMacAddress(iteration * 10 + index),
              'rssi': -50 - (index * 10),
            };
          });

          // Setup mock stream handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                devicesChannel,
                MockStreamHandler.inline(
                  onListen:
                      (Object? arguments, MockStreamHandlerEventSink events) {
                        events.success(deviceList);
                      },
                ),
              );

          // Start listening to devices stream
          final devicesStreamFuture = QringSdkFlutter.devicesStream.first;

          // Trigger scan
          await QringSdkFlutter.startScan();

          // Wait for devices to appear in stream
          final devices = await devicesStreamFuture.timeout(
            const Duration(seconds: 2),
            onTimeout: () => <QringDevice>[],
          );

          // Verify all devices appear in stream
          expect(
            devices.length,
            equals(deviceCount),
            reason: 'All discovered devices should appear in stream',
          );

          // Verify each device has correct information
          for (int i = 0; i < deviceCount; i++) {
            expect(devices[i].name, equals(deviceList[i]['name']));
            expect(devices[i].macAddress, equals(deviceList[i]['macAddress']));
            expect(devices[i].rssi, equals(deviceList[i]['rssi']));
          }
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );
  });
}

/// Generate a random MAC address for testing
String _generateRandomMacAddress(int seed) {
  final parts = List.generate(6, (index) {
    final value = ((seed * 17 + index * 23) % 256)
        .toRadixString(16)
        .padLeft(2, '0');
    return value.toUpperCase();
  });
  return parts.join(':');
}
