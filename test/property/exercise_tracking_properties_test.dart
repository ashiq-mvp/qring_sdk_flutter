import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Exercise Tracking Properties', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');
    const EventChannel exerciseChannel = EventChannel(
      'qring_sdk_flutter/exercise',
    );

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(exerciseChannel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(exerciseChannel, null);
    });

    test('Property 32: Exercise Control Methods Call Native SDK - '
        'For any exercise control method, the Flutter plugin should call '
        'the corresponding native SDK PhoneSportReq command', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 32: Exercise Control Methods Call Native SDK
      // Validates: Requirements 11.1, 11.2, 11.3, 11.4

      final methodCalls = <MethodCall>[];

      // Setup mock method handler to track method calls
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
            methodCalls.add(methodCall);

            // Return mock summary for stopExercise
            if (methodCall.method == 'stopExercise') {
              return {
                'durationSeconds': 600,
                'totalSteps': 1000,
                'distanceMeters': 800,
                'calories': 50,
                'averageHeartRate': 120,
                'maxHeartRate': 150,
              };
            }
            return null;
          });

      // Test with 100 iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        methodCalls.clear();

        // Test each exercise type
        final exerciseType =
            ExerciseType.values[iteration % ExerciseType.values.length];

        // Start exercise
        await QringExercise.startExercise(exerciseType);

        // Verify startExercise was called with correct exercise type
        expect(
          methodCalls.any(
            (call) =>
                call.method == 'startExercise' &&
                call.arguments['exerciseType'] == exerciseType.value,
          ),
          isTrue,
          reason:
              'startExercise should call native SDK with exercise type ${exerciseType.value}',
        );

        // Pause exercise
        await QringExercise.pauseExercise();

        // Verify pauseExercise was called
        expect(
          methodCalls.any((call) => call.method == 'pauseExercise'),
          isTrue,
          reason: 'pauseExercise should call native SDK',
        );

        // Resume exercise
        await QringExercise.resumeExercise();

        // Verify resumeExercise was called
        expect(
          methodCalls.any((call) => call.method == 'resumeExercise'),
          isTrue,
          reason: 'resumeExercise should call native SDK',
        );

        // Stop exercise
        final summary = await QringExercise.stopExercise();

        // Verify stopExercise was called
        expect(
          methodCalls.any((call) => call.method == 'stopExercise'),
          isTrue,
          reason: 'stopExercise should call native SDK',
        );

        // Verify summary is returned
        expect(
          summary,
          isNotNull,
          reason: 'stopExercise should return exercise summary',
        );

        // Verify all 4 control methods were called
        expect(
          methodCalls.length,
          equals(4),
          reason: 'All 4 exercise control methods should be called',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test(
      'Property 33: Exercise Data Streams Real-Time Metrics - '
      'For any active exercise session, the exerciseDataStream should emit '
      'ExerciseData objects containing duration, heart rate, steps, distance, and calories',
      () async {
        // Tag: Feature: qc-wireless-sdk-integration, Property 33: Exercise Data Streams Real-Time Metrics
        // Validates: Requirements 11.5

        // Setup mock method handler
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              return null;
            });

        // Test with 100 iterations
        for (int iteration = 0; iteration < 100; iteration++) {
          // Generate random exercise data
          final durationSeconds = 60 + (iteration * 10) % 3600; // 60s to 1 hour
          final heartRate = 60 + (iteration % 100); // 60-160 bpm
          final steps = iteration * 10; // Increasing steps
          final distanceMeters = iteration * 8; // Increasing distance
          final calories = iteration * 5; // Increasing calories

          final exerciseData = {
            'durationSeconds': durationSeconds,
            'heartRate': heartRate,
            'steps': steps,
            'distanceMeters': distanceMeters,
            'calories': calories,
          };

          // Setup mock stream handler that emits exercise data
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                exerciseChannel,
                MockStreamHandler.inline(
                  onListen:
                      (Object? arguments, MockStreamHandlerEventSink events) {
                        events.success(exerciseData);
                      },
                ),
              );

          // Start listening to exercise stream
          final exerciseStreamFuture = QringExercise.exerciseDataStream.first;

          // Start exercise (the mock will emit the data)
          final exerciseType =
              ExerciseType.values[iteration % ExerciseType.values.length];
          await QringExercise.startExercise(exerciseType);

          // Wait for exercise data
          final data = await exerciseStreamFuture.timeout(
            const Duration(seconds: 2),
          );

          // Verify exercise data was received through stream
          expect(
            data,
            isNotNull,
            reason: 'Exercise data should be emitted through stream',
          );

          // Verify all required fields are present
          expect(
            data.durationSeconds,
            equals(durationSeconds),
            reason: 'Exercise data should contain duration',
          );

          expect(
            data.heartRate,
            equals(heartRate),
            reason: 'Exercise data should contain heart rate',
          );

          expect(
            data.steps,
            equals(steps),
            reason: 'Exercise data should contain steps',
          );

          expect(
            data.distanceMeters,
            equals(distanceMeters),
            reason: 'Exercise data should contain distance',
          );

          expect(
            data.calories,
            equals(calories),
            reason: 'Exercise data should contain calories',
          );

          // Verify data types are correct
          expect(
            data.durationSeconds,
            isA<int>(),
            reason: 'Duration should be an integer',
          );

          expect(
            data.heartRate,
            isA<int>(),
            reason: 'Heart rate should be an integer',
          );

          expect(data.steps, isA<int>(), reason: 'Steps should be an integer');

          expect(
            data.distanceMeters,
            isA<int>(),
            reason: 'Distance should be an integer',
          );

          expect(
            data.calories,
            isA<int>(),
            reason: 'Calories should be an integer',
          );

          // Verify values are non-negative
          expect(
            data.durationSeconds,
            greaterThanOrEqualTo(0),
            reason: 'Duration should be non-negative',
          );

          expect(
            data.heartRate,
            greaterThanOrEqualTo(0),
            reason: 'Heart rate should be non-negative',
          );

          expect(
            data.steps,
            greaterThanOrEqualTo(0),
            reason: 'Steps should be non-negative',
          );

          expect(
            data.distanceMeters,
            greaterThanOrEqualTo(0),
            reason: 'Distance should be non-negative',
          );

          expect(
            data.calories,
            greaterThanOrEqualTo(0),
            reason: 'Calories should be non-negative',
          );
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );

    test('Exercise Summary Contains All Required Fields - '
        'For any completed exercise, stopExercise should return a summary '
        'with all required fields', () async {
      // Additional test to verify exercise summary structure

      // Setup mock method handler
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
            if (methodCall.method == 'stopExercise') {
              return {
                'durationSeconds': 1800,
                'totalSteps': 2500,
                'distanceMeters': 2000,
                'calories': 150,
                'averageHeartRate': 130,
                'maxHeartRate': 165,
              };
            }
            return null;
          });

      // Test with 100 iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        // Start and stop exercise
        final exerciseType =
            ExerciseType.values[iteration % ExerciseType.values.length];
        await QringExercise.startExercise(exerciseType);
        final summary = await QringExercise.stopExercise();

        // Verify all required fields are present
        expect(
          summary.durationSeconds,
          isA<int>(),
          reason: 'Summary should contain duration',
        );

        expect(
          summary.totalSteps,
          isA<int>(),
          reason: 'Summary should contain total steps',
        );

        expect(
          summary.distanceMeters,
          isA<int>(),
          reason: 'Summary should contain distance',
        );

        expect(
          summary.calories,
          isA<int>(),
          reason: 'Summary should contain calories',
        );

        expect(
          summary.averageHeartRate,
          isA<int>(),
          reason: 'Summary should contain average heart rate',
        );

        expect(
          summary.maxHeartRate,
          isA<int>(),
          reason: 'Summary should contain max heart rate',
        );

        // Verify values are non-negative
        expect(
          summary.durationSeconds,
          greaterThanOrEqualTo(0),
          reason: 'Duration should be non-negative',
        );

        expect(
          summary.totalSteps,
          greaterThanOrEqualTo(0),
          reason: 'Total steps should be non-negative',
        );

        expect(
          summary.distanceMeters,
          greaterThanOrEqualTo(0),
          reason: 'Distance should be non-negative',
        );

        expect(
          summary.calories,
          greaterThanOrEqualTo(0),
          reason: 'Calories should be non-negative',
        );

        expect(
          summary.averageHeartRate,
          greaterThanOrEqualTo(0),
          reason: 'Average heart rate should be non-negative',
        );

        expect(
          summary.maxHeartRate,
          greaterThanOrEqualTo(0),
          reason: 'Max heart rate should be non-negative',
        );

        // Verify max heart rate >= average heart rate
        expect(
          summary.maxHeartRate,
          greaterThanOrEqualTo(summary.averageHeartRate),
          reason: 'Max heart rate should be >= average heart rate',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test(
      'Exercise Type Values Are Valid - '
      'For any exercise type, the value should be within valid range',
      () async {
        // Test that all exercise types have valid values

        // Test with 100 iterations
        for (int iteration = 0; iteration < 100; iteration++) {
          final exerciseType =
              ExerciseType.values[iteration % ExerciseType.values.length];

          // Verify exercise type value is non-negative
          expect(
            exerciseType.value,
            greaterThanOrEqualTo(0),
            reason: 'Exercise type value should be non-negative',
          );

          // Verify exercise type value is within expected range (0-20+)
          expect(
            exerciseType.value,
            lessThan(50),
            reason: 'Exercise type value should be within reasonable range',
          );

          // Verify fromValue works correctly
          final retrievedType = ExerciseType.fromValue(exerciseType.value);
          expect(
            retrievedType,
            equals(exerciseType),
            reason: 'fromValue should return the correct exercise type',
          );
        }
      },
    );
  });
}
