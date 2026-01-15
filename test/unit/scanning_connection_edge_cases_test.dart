import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Scanning and Connection Edge Cases', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');
    const EventChannel devicesChannel = EventChannel(
      'qring_sdk_flutter/devices',
    );
    const EventChannel stateChannel = EventChannel('qring_sdk_flutter/state');

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(devicesChannel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(stateChannel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(devicesChannel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(stateChannel, null);
    });

    group('Double-Scan Handling', () {
      test('should handle double-scan gracefully without error', () async {
        // Validates: Requirements 1.5

        int scanCallCount = 0;
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'scan') {
                scanCallCount++;
                return null;
              }
              return null;
            });

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              devicesChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success([]);
                    },
              ),
            );

        // Start first scan
        await QringSdkFlutter.startScan();
        expect(scanCallCount, equals(1));

        // Start second scan while first is active
        await QringSdkFlutter.startScan();
        expect(scanCallCount, equals(2));

        // Both calls should succeed without throwing
        // The native layer should handle the duplicate scan request gracefully
      });

      test('should not throw when stopping scan that is not active', () async {
        // Validates: Requirements 1.5

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'stopScan') {
                return null;
              }
              return null;
            });

        // Stop scan without starting it first
        // Should not throw an exception
        await expectLater(QringSdkFlutter.stopScan(), completes);
      });

      test('should handle rapid start/stop scan cycles', () async {
        // Validates: Requirements 1.5

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'scan' ||
                  methodCall.method == 'stopScan') {
                return null;
              }
              return null;
            });

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              devicesChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success([]);
                    },
              ),
            );

        // Rapid start/stop cycles
        for (int i = 0; i < 10; i++) {
          await QringSdkFlutter.startScan();
          await QringSdkFlutter.stopScan();
        }

        // Should complete without errors
      });
    });

    group('Double-Connect Handling', () {
      test('should handle double-connect to same device gracefully', () async {
        // Validates: Requirements 2.5

        const macAddress = 'AA:BB:CC:DD:EE:FF';
        int connectCallCount = 0;

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'connect') {
                connectCallCount++;
                return null;
              }
              return null;
            });

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              stateChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success('connecting');
                      Future.delayed(const Duration(milliseconds: 10), () {
                        events.success('connected');
                      });
                    },
              ),
            );

        // First connection
        await QringSdkFlutter.connect(macAddress);
        expect(connectCallCount, equals(1));

        // Second connection to same device
        await QringSdkFlutter.connect(macAddress);
        expect(connectCallCount, equals(2));

        // Both calls should succeed
        // The native layer should handle the duplicate connection gracefully
      });

      test('should handle connect with empty MAC address', () async {
        // Validates: Requirements 2.5

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'connect') {
                throw PlatformException(
                  code: 'INVALID_ARGUMENT',
                  message: 'MAC address is required',
                );
              }
              return null;
            });

        // Attempt to connect with empty MAC address
        expect(
          () => QringSdkFlutter.connect(''),
          throwsA(isA<ArgumentError>()),
        );
      });

      test('should handle rapid connect/disconnect cycles', () async {
        // Validates: Requirements 2.5

        const macAddress = 'AA:BB:CC:DD:EE:FF';

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'connect' ||
                  methodCall.method == 'disconnect') {
                return null;
              }
              return null;
            });

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              stateChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success('connecting');
                      Future.delayed(const Duration(milliseconds: 5), () {
                        events.success('connected');
                      });
                    },
              ),
            );

        // Rapid connect/disconnect cycles
        for (int i = 0; i < 5; i++) {
          await QringSdkFlutter.connect(macAddress);
          await QringSdkFlutter.disconnect();
        }

        // Should complete without errors
      });
    });

    group('Disconnect When Not Connected', () {
      test('should handle disconnect when not connected gracefully', () async {
        // Validates: Requirements 2.5

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'disconnect') {
                return null;
              }
              return null;
            });

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              stateChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success('disconnected');
                    },
              ),
            );

        // Disconnect without being connected
        await expectLater(QringSdkFlutter.disconnect(), completes);
      });

      test('should handle multiple disconnect calls', () async {
        // Validates: Requirements 2.5

        int disconnectCallCount = 0;

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'disconnect') {
                disconnectCallCount++;
                return null;
              }
              return null;
            });

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              stateChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success('disconnected');
                    },
              ),
            );

        // Multiple disconnect calls
        await QringSdkFlutter.disconnect();
        await QringSdkFlutter.disconnect();
        await QringSdkFlutter.disconnect();

        expect(disconnectCallCount, equals(3));
        // All calls should succeed without throwing
      });
    });

    group('Error Handling', () {
      test('should handle platform exceptions from scan', () async {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'scan') {
                throw PlatformException(
                  code: 'PERMISSION_DENIED',
                  message: 'Bluetooth permission not granted',
                );
              }
              return null;
            });

        // Should throw an exception with proper error message
        expect(() => QringSdkFlutter.startScan(), throwsA(isA<Exception>()));
      });

      test('should handle platform exceptions from connect', () async {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'connect') {
                throw PlatformException(
                  code: 'CONNECTION_FAILED',
                  message: 'Failed to connect to device',
                );
              }
              return null;
            });

        // Should throw an exception with proper error message
        expect(
          () => QringSdkFlutter.connect('AA:BB:CC:DD:EE:FF'),
          throwsA(isA<Exception>()),
        );
      });
    });
  });
}
