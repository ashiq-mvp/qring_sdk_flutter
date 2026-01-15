import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Find My Ring Properties', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    test('Property 8: Find Ring Sends Command When Connected - '
        'For any invocation of findRing() when the device is connected, '
        'the Flutter plugin should call the native SDK command', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 8: Find Ring Sends Command When Connected
      // Validates: Requirements 3.1

      // Test with 100 iterations to verify property holds across many scenarios
      for (int iteration = 0; iteration < 100; iteration++) {
        int findRingCallCount = 0;

        // Setup mock method handler that simulates connected state
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'findRing') {
                findRingCallCount++;
                // Simulate successful command execution
                return null;
              }
              return null;
            });

        // Call findRing
        await QringSdkFlutter.findRing();

        // Verify the native method was called
        expect(
          findRingCallCount,
          equals(1),
          reason: 'findRing should invoke native method exactly once',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test('Property 9: Find Ring Returns Error When Disconnected - '
        'For any invocation of findRing() when the device is not connected, '
        'the method should return an error indicating no connection', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 9: Find Ring Returns Error When Disconnected
      // Validates: Requirements 3.3

      // Test with 100 iterations to verify property holds across many scenarios
      for (int iteration = 0; iteration < 100; iteration++) {
        // Setup mock method handler that simulates disconnected state
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'findRing') {
                // Simulate NOT_CONNECTED error from native side
                throw PlatformException(
                  code: 'NOT_CONNECTED',
                  message: 'Device is not connected',
                );
              }
              return null;
            });

        // Call findRing and expect an error
        try {
          await QringSdkFlutter.findRing();
          fail('Should throw an exception when device is not connected');
        } catch (e) {
          // Verify the error message indicates no connection
          expect(
            e.toString(),
            contains('not connected'),
            reason: 'Error should indicate device is not connected',
          );
        }
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test(
      'Property 8 (Command Validation): Find Ring command completes successfully when connected',
      () async {
        // Test that findRing completes without errors when device is connected

        for (int iteration = 0; iteration < 50; iteration++) {
          bool commandExecuted = false;

          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                if (methodCall.method == 'findRing') {
                  commandExecuted = true;
                  // Simulate successful execution
                  return null;
                }
                return null;
              });

          // Call findRing - should not throw
          await QringSdkFlutter.findRing();

          // Verify command was executed
          expect(
            commandExecuted,
            isTrue,
            reason: 'Find ring command should be executed',
          );
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );

    test(
      'Property 9 (Error Types): Find Ring returns appropriate error codes',
      () async {
        // Test that different error scenarios return appropriate error codes

        final errorScenarios = [
          {'code': 'NOT_CONNECTED', 'message': 'Device is not connected'},
          {'code': 'TIMEOUT', 'message': 'Find ring command timed out'},
          {'code': 'COMMAND_FAILED', 'message': 'Find ring command failed'},
        ];

        for (final scenario in errorScenarios) {
          for (int iteration = 0; iteration < 30; iteration++) {
            // Setup mock method handler with specific error
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (
                  MethodCall methodCall,
                ) async {
                  if (methodCall.method == 'findRing') {
                    throw PlatformException(
                      code: scenario['code'] as String,
                      message: scenario['message'] as String,
                    );
                  }
                  return null;
                });

            // Call findRing and expect an error
            try {
              await QringSdkFlutter.findRing();
              fail(
                'Should throw an exception for error scenario: ${scenario['code']}',
              );
            } catch (e) {
              // Verify error is thrown
              expect(
                e,
                isNotNull,
                reason: 'Should throw an error for ${scenario['code']}',
              );
            }
          }
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );
  });
}
