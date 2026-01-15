import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Continuous Monitoring Properties', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');
    const EventChannel stateChannel = EventChannel('qring_sdk_flutter/state');

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(stateChannel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(stateChannel, null);
    });

    test(
      'Property 22: Continuous Monitoring Configuration Calls Native SDK - '
      'For any continuous monitoring configuration method, '
      'the Flutter plugin should call the corresponding native SDK setting command',
      () async {
        // Tag: Feature: qc-wireless-sdk-integration, Property 22: Continuous Monitoring Configuration Calls Native SDK
        // Validates: Requirements 8.1, 8.2, 8.3

        // Test with 100 iterations to verify property holds across many scenarios
        for (int iteration = 0; iteration < 100; iteration++) {
          // Generate random settings
          final enableHR = iteration % 2 == 0;
          final hrInterval = [10, 15, 20, 30, 60][iteration % 5];
          final enableSpO2 = iteration % 3 == 0;
          final spo2Interval = 10 + (iteration % 50);
          final enableBP = iteration % 4 == 0;

          // Track which methods were called
          final calledMethods = <String>[];

          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                calledMethods.add(methodCall.method);

                if (methodCall.method == 'setContinuousHeartRate') {
                  expect(methodCall.arguments['enable'], equals(enableHR));
                  expect(
                    methodCall.arguments['intervalMinutes'],
                    equals(hrInterval),
                  );
                  return null;
                } else if (methodCall.method == 'setContinuousBloodOxygen') {
                  expect(methodCall.arguments['enable'], equals(enableSpO2));
                  expect(
                    methodCall.arguments['intervalMinutes'],
                    equals(spo2Interval),
                  );
                  return null;
                } else if (methodCall.method == 'setContinuousBloodPressure') {
                  expect(methodCall.arguments['enable'], equals(enableBP));
                  return null;
                }
                return null;
              });

          // Setup mock connection state (connected)
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                stateChannel,
                MockStreamHandler.inline(
                  onListen:
                      (Object? arguments, MockStreamHandlerEventSink events) {
                        events.success('connected');
                      },
                ),
              );

          // Test setContinuousHeartRate
          await QringSettings.setContinuousHeartRate(
            enable: enableHR,
            intervalMinutes: hrInterval,
          );
          expect(
            calledMethods,
            contains('setContinuousHeartRate'),
            reason: 'setContinuousHeartRate should call native SDK',
          );

          // Test setContinuousBloodOxygen
          await QringSettings.setContinuousBloodOxygen(
            enable: enableSpO2,
            intervalMinutes: spo2Interval,
          );
          expect(
            calledMethods,
            contains('setContinuousBloodOxygen'),
            reason: 'setContinuousBloodOxygen should call native SDK',
          );

          // Test setContinuousBloodPressure
          await QringSettings.setContinuousBloodPressure(enable: enableBP);
          expect(
            calledMethods,
            contains('setContinuousBloodPressure'),
            reason: 'setContinuousBloodPressure should call native SDK',
          );

          // Verify all methods were called
          expect(calledMethods.length, equals(3));
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );

    test(
      'Property 23: Continuous Monitoring Settings Can Be Read - '
      'For any continuous monitoring setting type, '
      'there should be a corresponding get method that returns the current configuration',
      () async {
        // Tag: Feature: qc-wireless-sdk-integration, Property 23: Continuous Monitoring Settings Can Be Read
        // Validates: Requirements 8.4

        // Test with 100 iterations to verify property holds across many scenarios
        for (int iteration = 0; iteration < 100; iteration++) {
          // Generate random settings
          final enabledHR = iteration % 2 == 0;
          final hrInterval = [10, 15, 20, 30, 60][iteration % 5];
          final enabledSpO2 = iteration % 3 == 0;
          final spo2Interval = 10 + (iteration % 50);
          final enabledBP = iteration % 4 == 0;

          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                if (methodCall.method == 'getContinuousHeartRateSettings') {
                  return {'enabled': enabledHR, 'intervalMinutes': hrInterval};
                } else if (methodCall.method ==
                    'getContinuousBloodOxygenSettings') {
                  return {
                    'enabled': enabledSpO2,
                    'intervalMinutes': spo2Interval,
                  };
                } else if (methodCall.method ==
                    'getContinuousBloodPressureSettings') {
                  return {'enabled': enabledBP};
                }
                return null;
              });

          // Test getContinuousHeartRateSettings
          final hrSettings =
              await QringSettings.getContinuousHeartRateSettings();
          expect(hrSettings.enabled, equals(enabledHR));
          expect(hrSettings.intervalMinutes, equals(hrInterval));

          // Test getContinuousBloodOxygenSettings
          final spo2Settings =
              await QringSettings.getContinuousBloodOxygenSettings();
          expect(spo2Settings.enabled, equals(enabledSpO2));
          expect(spo2Settings.intervalMinutes, equals(spo2Interval));

          // Test getContinuousBloodPressureSettings
          final bpSettings =
              await QringSettings.getContinuousBloodPressureSettings();
          expect(bpSettings.enabled, equals(enabledBP));
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );

    test(
      'Property 24: Heart Rate Interval Validation - '
      'For any setContinuousHeartRate call, '
      'the interval parameter should only accept values from the set {10, 15, 20, 30, 60} minutes',
      () async {
        // Tag: Feature: qc-wireless-sdk-integration, Property 24: Heart Rate Interval Validation
        // Validates: Requirements 8.5

        // Valid intervals
        const validIntervals = [10, 15, 20, 30, 60];

        // Test with 100 iterations to verify property holds
        for (int iteration = 0; iteration < 100; iteration++) {
          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                if (methodCall.method == 'setContinuousHeartRate') {
                  return null;
                }
                return null;
              });

          // Test valid intervals - should succeed
          final validInterval =
              validIntervals[iteration % validIntervals.length];
          try {
            await QringSettings.setContinuousHeartRate(
              enable: true,
              intervalMinutes: validInterval,
            );
            // Should not throw
          } catch (e) {
            fail('Valid interval $validInterval should not throw: $e');
          }

          // Test invalid intervals - should throw ArgumentError
          final invalidIntervals = [
            0,
            1,
            5,
            9,
            11,
            14,
            16,
            19,
            21,
            29,
            31,
            59,
            61,
            90,
            120,
            -1,
            -10,
          ];
          final invalidInterval =
              invalidIntervals[iteration % invalidIntervals.length];

          try {
            await QringSettings.setContinuousHeartRate(
              enable: true,
              intervalMinutes: invalidInterval,
            );
            fail(
              'Invalid interval $invalidInterval should throw ArgumentError',
            );
          } on ArgumentError catch (e) {
            // Expected - verify error message mentions valid intervals
            expect(
              e.message,
              contains('10, 15, 20, 30, 60'),
              reason: 'Error message should list valid intervals',
            );
          } catch (e) {
            fail('Should throw ArgumentError, got: $e');
          }
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );

    test(
      'Property 24 (Edge Case): Blood oxygen interval must be positive',
      () async {
        // Test that blood oxygen interval validation works correctly

        for (int iteration = 0; iteration < 50; iteration++) {
          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                if (methodCall.method == 'setContinuousBloodOxygen') {
                  return null;
                }
                return null;
              });

          // Test valid positive intervals - should succeed
          final validInterval = 1 + (iteration % 100);
          try {
            await QringSettings.setContinuousBloodOxygen(
              enable: true,
              intervalMinutes: validInterval,
            );
            // Should not throw
          } catch (e) {
            fail('Valid interval $validInterval should not throw: $e');
          }

          // Test invalid intervals (zero and negative) - should throw ArgumentError
          final invalidIntervals = [0, -1, -5, -10, -100];
          final invalidInterval =
              invalidIntervals[iteration % invalidIntervals.length];

          try {
            await QringSettings.setContinuousBloodOxygen(
              enable: true,
              intervalMinutes: invalidInterval,
            );
            fail(
              'Invalid interval $invalidInterval should throw ArgumentError',
            );
          } on ArgumentError catch (e) {
            // Expected - verify error message mentions positive requirement
            expect(
              e.message,
              contains('positive'),
              reason: 'Error message should mention positive requirement',
            );
          } catch (e) {
            fail('Should throw ArgumentError, got: $e');
          }
        }
      },
      timeout: const Timeout(Duration(minutes: 1)),
    );

    test(
      'Property 22 (Round Trip): Settings can be set and then read back',
      () async {
        // Test that settings persist correctly through set/get operations

        for (int iteration = 0; iteration < 50; iteration++) {
          final enableHR = iteration % 2 == 0;
          final hrInterval = [10, 15, 20, 30, 60][iteration % 5];

          // Track the last set values
          bool? lastSetEnabledHR;
          int? lastSetIntervalHR;

          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                if (methodCall.method == 'setContinuousHeartRate') {
                  lastSetEnabledHR = methodCall.arguments['enable'] as bool;
                  lastSetIntervalHR =
                      methodCall.arguments['intervalMinutes'] as int;
                  return null;
                } else if (methodCall.method ==
                    'getContinuousHeartRateSettings') {
                  // Return the last set values
                  return {
                    'enabled': lastSetEnabledHR ?? false,
                    'intervalMinutes': lastSetIntervalHR ?? 10,
                  };
                }
                return null;
              });

          // Set the settings
          await QringSettings.setContinuousHeartRate(
            enable: enableHR,
            intervalMinutes: hrInterval,
          );

          // Read back the settings
          final settings = await QringSettings.getContinuousHeartRateSettings();

          // Verify they match what was set
          expect(
            settings.enabled,
            equals(enableHR),
            reason: 'Read settings should match set settings',
          );
          expect(
            settings.intervalMinutes,
            equals(hrInterval),
            reason: 'Read interval should match set interval',
          );
        }
      },
      timeout: const Timeout(Duration(minutes: 1)),
    );
  });
}
