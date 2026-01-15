import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/src/models/health_measurement.dart';
import 'package:qring_sdk_flutter/src/qring_health_data.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const notificationChannel = EventChannel('qring_sdk_flutter/notification');

  group('Notification Properties', () {
    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(notificationChannel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(notificationChannel, null);
    });

    /// Property 25: Automatic Measurements Emit Notifications
    /// Feature: qc-wireless-sdk-integration
    /// Validates: Requirements 9.1
    test(
      'Property 25: For any automatic measurement, notification should be emitted through stream',
      () async {
        // Test with multiple notification types
        final notificationTypes = [
          {
            'type': 'heartRate',
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'heartRate': 75,
            'success': true,
          },
          {
            'type': 'bloodPressure',
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'systolic': 120,
            'diastolic': 80,
            'success': true,
          },
          {
            'type': 'bloodOxygen',
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'spO2': 98,
            'success': true,
          },
          {
            'type': 'temperature',
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'temperature': 36.5,
            'success': true,
          },
          {
            'type': 'stepCount',
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'steps': 1000,
            'success': true,
          },
        ];

        for (final notificationData in notificationTypes) {
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                notificationChannel,
                MockStreamHandler.inline(
                  onListen: (arguments, events) {
                    events.success(notificationData);
                    return null;
                  },
                ),
              );

          final stream = QringHealthData.notificationStream;
          final notification = await stream.first;

          expect(notification, isA<HealthMeasurement>());
          expect(notification.success, isTrue);
          expect(notification.type.toStringValue(), notificationData['type']);
        }
      },
    );

    /// Property 26: Notification Types Coverage
    /// Feature: qc-wireless-sdk-integration
    /// Validates: Requirements 9.2
    test(
      'Property 26: For any notification, type should be one of the supported types',
      () async {
        final supportedTypes = [
          'heartRate',
          'bloodPressure',
          'bloodOxygen',
          'temperature',
          'stepCount',
        ];

        for (int i = 0; i < 100; i++) {
          // Pick a random supported type
          final typeIndex = i % supportedTypes.length;
          final notificationType = supportedTypes[typeIndex];

          final notificationData = {
            'type': notificationType,
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'success': true,
          };

          // Add type-specific data
          switch (notificationType) {
            case 'heartRate':
              notificationData['heartRate'] = 60 + (i % 40);
              break;
            case 'bloodPressure':
              notificationData['systolic'] = 110 + (i % 30);
              notificationData['diastolic'] = 70 + (i % 20);
              break;
            case 'bloodOxygen':
              notificationData['spO2'] = 90 + (i % 10);
              break;
            case 'temperature':
              notificationData['temperature'] = 36.0 + (i % 3);
              break;
            case 'stepCount':
              notificationData['steps'] = i * 100;
              break;
          }

          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                notificationChannel,
                MockStreamHandler.inline(
                  onListen: (arguments, events) {
                    events.success(notificationData);
                    return null;
                  },
                ),
              );

          final stream = QringHealthData.notificationStream;
          final notification = await stream.first;

          // Verify the type is one of the supported types
          expect(
            supportedTypes.contains(notification.type.toStringValue()),
            isTrue,
            reason:
                'Notification type ${notification.type} should be in supported types',
          );
        }
      },
    );

    /// Property 27: Notifications Contain Type and Value
    /// Feature: qc-wireless-sdk-integration
    /// Validates: Requirements 9.3
    test(
      'Property 27: For any notification, it should contain both type and measured value',
      () async {
        final testCases = [
          {
            'data': {
              'type': 'heartRate',
              'timestamp': DateTime.now().millisecondsSinceEpoch,
              'heartRate': 75,
              'success': true,
            },
            'checkValue': (HealthMeasurement m) => m.heartRate != null,
          },
          {
            'data': {
              'type': 'bloodPressure',
              'timestamp': DateTime.now().millisecondsSinceEpoch,
              'systolic': 120,
              'diastolic': 80,
              'success': true,
            },
            'checkValue': (HealthMeasurement m) =>
                m.systolic != null && m.diastolic != null,
          },
          {
            'data': {
              'type': 'bloodOxygen',
              'timestamp': DateTime.now().millisecondsSinceEpoch,
              'spO2': 98,
              'success': true,
            },
            'checkValue': (HealthMeasurement m) => m.spO2 != null,
          },
          {
            'data': {
              'type': 'temperature',
              'timestamp': DateTime.now().millisecondsSinceEpoch,
              'temperature': 36.5,
              'success': true,
            },
            'checkValue': (HealthMeasurement m) => m.temperature != null,
          },
          {
            'data': {
              'type': 'stepCount',
              'timestamp': DateTime.now().millisecondsSinceEpoch,
              'steps': 1000,
              'success': true,
            },
            'checkValue': (HealthMeasurement m) => m.steps != null,
          },
        ];

        for (final testCase in testCases) {
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                notificationChannel,
                MockStreamHandler.inline(
                  onListen: (arguments, events) {
                    events.success(testCase['data']);
                    return null;
                  },
                ),
              );

          final stream = QringHealthData.notificationStream;
          final notification = await stream.first;

          // Verify type is present
          expect(notification.type, isNotNull);

          // Verify value is present using the check function
          final checkValue =
              testCase['checkValue'] as bool Function(HealthMeasurement);
          expect(
            checkValue(notification),
            isTrue,
            reason:
                'Notification of type ${notification.type} should contain measured value',
          );
        }
      },
    );

    /// Property 28: Multiple Listeners Receive Same Notifications
    /// Feature: qc-wireless-sdk-integration
    /// Validates: Requirements 9.4
    test(
      'Property 28: For any notification, all listeners should receive the same data',
      () async {
        final notificationData = {
          'type': 'heartRate',
          'timestamp': DateTime.now().millisecondsSinceEpoch,
          'heartRate': 75,
          'success': true,
        };

        // Set up mock to emit notification
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              notificationChannel,
              MockStreamHandler.inline(
                onListen: (arguments, events) {
                  events.success(notificationData);
                  return null;
                },
              ),
            );

        // Create multiple listeners
        final stream1 = QringHealthData.notificationStream;
        final stream2 = QringHealthData.notificationStream;
        final stream3 = QringHealthData.notificationStream;

        // All listeners should receive the same notification
        final notification1Future = stream1.first;
        final notification2Future = stream2.first;
        final notification3Future = stream3.first;

        final notification1 = await notification1Future;
        final notification2 = await notification2Future;
        final notification3 = await notification3Future;

        // Verify all listeners received the same data
        expect(notification1.type, equals(notification2.type));
        expect(notification1.type, equals(notification3.type));
        expect(notification1.heartRate, equals(notification2.heartRate));
        expect(notification1.heartRate, equals(notification3.heartRate));
        expect(notification1.timestamp, equals(notification2.timestamp));
        expect(notification1.timestamp, equals(notification3.timestamp));
        expect(notification1.success, equals(notification2.success));
        expect(notification1.success, equals(notification3.success));
      },
    );
  });
}
