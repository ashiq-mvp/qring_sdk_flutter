import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/src/qring_health_data.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Manual Measurement Properties', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');
    const EventChannel measurementChannel = EventChannel(
      'qring_sdk_flutter/measurement',
    );

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(measurementChannel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(measurementChannel, null);
    });

    test('Property 18: Manual Measurements Trigger Native Commands - '
        'For any manual measurement method, the Flutter plugin should call '
        'the corresponding native SDK manual measurement command', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 18: Manual Measurements Trigger Native Commands
      // Validates: Requirements 7.1, 7.2, 7.3, 7.4

      final methodCalls = <String>[];

      // Setup mock method handler to track method calls
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
            methodCalls.add(methodCall.method);
            return null;
          });

      // Test with 100 iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        methodCalls.clear();

        // Test each measurement type
        final measurementTypes = [
          'startHeartRateMeasurement',
          'startBloodPressureMeasurement',
          'startBloodOxygenMeasurement',
          'startTemperatureMeasurement',
        ];

        for (final methodName in measurementTypes) {
          // Call the measurement method
          switch (methodName) {
            case 'startHeartRateMeasurement':
              await QringHealthData.startHeartRateMeasurement();
              break;
            case 'startBloodPressureMeasurement':
              await QringHealthData.startBloodPressureMeasurement();
              break;
            case 'startBloodOxygenMeasurement':
              await QringHealthData.startBloodOxygenMeasurement();
              break;
            case 'startTemperatureMeasurement':
              await QringHealthData.startTemperatureMeasurement();
              break;
          }

          // Verify the native method was called
          expect(
            methodCalls,
            contains(methodName),
            reason: '$methodName should trigger native SDK call',
          );
        }

        // Verify all measurement methods were called
        expect(
          methodCalls.length,
          equals(4),
          reason: 'All 4 measurement methods should be called',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test('Property 19: Stop Measurement Cancels Active Measurement - '
        'For any invocation of stopMeasurement during an active measurement, '
        'the Flutter plugin should call the native SDK stop command', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 19: Stop Measurement Cancels Active Measurement
      // Validates: Requirements 7.5

      final methodCalls = <String>[];

      // Setup mock method handler
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
            methodCalls.add(methodCall.method);
            return null;
          });

      // Test with 100 iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        methodCalls.clear();

        // Start a random measurement
        final measurementIndex = iteration % 4;
        switch (measurementIndex) {
          case 0:
            await QringHealthData.startHeartRateMeasurement();
            break;
          case 1:
            await QringHealthData.startBloodPressureMeasurement();
            break;
          case 2:
            await QringHealthData.startBloodOxygenMeasurement();
            break;
          case 3:
            await QringHealthData.startTemperatureMeasurement();
            break;
        }

        // Stop the measurement
        await QringHealthData.stopMeasurement();

        // Verify stopMeasurement was called
        expect(
          methodCalls,
          contains('stopMeasurement'),
          reason: 'stopMeasurement should trigger native SDK stop command',
        );

        // Verify both start and stop were called
        expect(
          methodCalls.length,
          equals(2),
          reason: 'Should have start and stop method calls',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test('Property 20: Measurement Results Stream Through Event Channel - '
        'For any manual measurement in progress, measurement results should be '
        'emitted through the measurementStream', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 20: Measurement Results Stream Through Event Channel
      // Validates: Requirements 7.6

      // Setup mock method handler
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
            return null;
          });

      // Test with 100 iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        // Generate random measurement data
        final measurementType = _getMeasurementType(iteration % 4);
        final timestamp = DateTime.now().millisecondsSinceEpoch;
        final measurementData = _generateMeasurementData(
          measurementType,
          timestamp,
          iteration,
        );

        // Setup mock stream handler that emits measurement results
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              measurementChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success(measurementData);
                    },
              ),
            );

        // Start listening to measurement stream
        final measurementStreamFuture = QringHealthData.measurementStream.first;

        // Trigger measurement (the mock will emit the result)
        switch (measurementType) {
          case 'heartRate':
            await QringHealthData.startHeartRateMeasurement();
            break;
          case 'bloodPressure':
            await QringHealthData.startBloodPressureMeasurement();
            break;
          case 'bloodOxygen':
            await QringHealthData.startBloodOxygenMeasurement();
            break;
          case 'temperature':
            await QringHealthData.startTemperatureMeasurement();
            break;
        }

        // Wait for measurement result
        final measurement = await measurementStreamFuture.timeout(
          const Duration(seconds: 2),
        );

        // Verify measurement was received through stream
        expect(
          measurement,
          isNotNull,
          reason: 'Measurement should be emitted through stream',
        );

        // Verify measurement contains correct type
        expect(
          measurement.type.toStringValue(),
          equals(measurementType),
          reason: 'Measurement type should match',
        );

        // Verify measurement contains timestamp
        expect(
          measurement.timestamp,
          isNotNull,
          reason: 'Measurement should have timestamp',
        );

        // Verify measurement contains appropriate value based on type
        switch (measurementType) {
          case 'heartRate':
            expect(
              measurement.heartRate,
              isNotNull,
              reason: 'Heart rate measurement should have heartRate value',
            );
            break;
          case 'bloodPressure':
            expect(
              measurement.systolic,
              isNotNull,
              reason: 'Blood pressure measurement should have systolic value',
            );
            expect(
              measurement.diastolic,
              isNotNull,
              reason: 'Blood pressure measurement should have diastolic value',
            );
            break;
          case 'bloodOxygen':
            expect(
              measurement.spO2,
              isNotNull,
              reason: 'Blood oxygen measurement should have spO2 value',
            );
            break;
          case 'temperature':
            expect(
              measurement.temperature,
              isNotNull,
              reason: 'Temperature measurement should have temperature value',
            );
            break;
        }
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test('Property 21: Failed Measurements Include Error Information - '
        'For any failed measurement, the HealthMeasurement object should have '
        'success=false and contain an error message', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 21: Failed Measurements Include Error Information
      // Validates: Requirements 7.7

      // Setup mock method handler
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
            return null;
          });

      // Test with 100 iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        // Generate random measurement type
        final measurementType = _getMeasurementType(iteration % 4);
        final timestamp = DateTime.now().millisecondsSinceEpoch;
        final errorCode = 'ERROR_${iteration % 10}';
        final errorMessage = 'Measurement failed with error code: $errorCode';

        // Create failed measurement data
        final measurementData = {
          'type': measurementType,
          'timestamp': timestamp,
          'success': false,
          'errorMessage': errorMessage,
        };

        // Setup mock stream handler that emits failed measurement
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              measurementChannel,
              MockStreamHandler.inline(
                onListen:
                    (Object? arguments, MockStreamHandlerEventSink events) {
                      events.success(measurementData);
                    },
              ),
            );

        // Start listening to measurement stream
        final measurementStreamFuture = QringHealthData.measurementStream.first;

        // Trigger measurement
        switch (measurementType) {
          case 'heartRate':
            await QringHealthData.startHeartRateMeasurement();
            break;
          case 'bloodPressure':
            await QringHealthData.startBloodPressureMeasurement();
            break;
          case 'bloodOxygen':
            await QringHealthData.startBloodOxygenMeasurement();
            break;
          case 'temperature':
            await QringHealthData.startTemperatureMeasurement();
            break;
        }

        // Wait for measurement result
        final measurement = await measurementStreamFuture.timeout(
          const Duration(seconds: 2),
        );

        // Verify measurement indicates failure
        expect(
          measurement.success,
          isFalse,
          reason: 'Failed measurement should have success=false',
        );

        // Verify error message is present
        expect(
          measurement.errorMessage,
          isNotNull,
          reason: 'Failed measurement should have error message',
        );
        expect(
          measurement.errorMessage,
          isNotEmpty,
          reason: 'Error message should not be empty',
        );
        expect(
          measurement.errorMessage,
          equals(errorMessage),
          reason: 'Error message should match',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));
  });
}

/// Get measurement type string based on index
String _getMeasurementType(int index) {
  switch (index) {
    case 0:
      return 'heartRate';
    case 1:
      return 'bloodPressure';
    case 2:
      return 'bloodOxygen';
    case 3:
      return 'temperature';
    default:
      return 'heartRate';
  }
}

/// Generate random measurement data for testing
Map<String, dynamic> _generateMeasurementData(
  String type,
  int timestamp,
  int seed,
) {
  final data = <String, dynamic>{
    'type': type,
    'timestamp': timestamp,
    'success': true,
  };

  switch (type) {
    case 'heartRate':
      data['heartRate'] = 60 + (seed % 60); // 60-120 bpm
      break;
    case 'bloodPressure':
      data['systolic'] = 100 + (seed % 40); // 100-140 mmHg
      data['diastolic'] = 60 + (seed % 30); // 60-90 mmHg
      break;
    case 'bloodOxygen':
      data['spO2'] = 90 + (seed % 10); // 90-100%
      break;
    case 'temperature':
      data['temperature'] = 36.0 + (seed % 30) / 10.0; // 36.0-39.0°C
      break;
  }

  return data;
}
