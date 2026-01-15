import 'dart:async';

import 'package:flutter/services.dart';
import 'package:qring_sdk_flutter/src/models/exercise_data.dart';
import 'package:qring_sdk_flutter/src/models/exercise_summary.dart';
import 'package:qring_sdk_flutter/src/models/exercise_type.dart';

/// API class for exercise tracking operations.
/// Provides methods to start, pause, resume, and stop exercise sessions,
/// and streams real-time exercise data.
class QringExercise {
  static const MethodChannel _channel = MethodChannel('qring_sdk_flutter');
  static const EventChannel _exerciseChannel = EventChannel(
    'qring_sdk_flutter/exercise',
  );

  static Stream<ExerciseData>? _exerciseDataStream;

  /// Stream of real-time exercise data.
  /// Subscribe to this stream to receive live updates during an active exercise session.
  ///
  /// The stream emits [ExerciseData] objects containing:
  /// - Duration in seconds
  /// - Current heart rate
  /// - Steps taken
  /// - Distance covered in meters
  /// - Calories burned
  ///
  /// Example:
  /// ```dart
  /// QringExercise.exerciseDataStream.listen((data) {
  ///   print('Duration: ${data.durationSeconds}s');
  ///   print('Heart Rate: ${data.heartRate} bpm');
  ///   print('Steps: ${data.steps}');
  /// });
  /// ```
  static Stream<ExerciseData> get exerciseDataStream {
    _exerciseDataStream ??= _exerciseChannel.receiveBroadcastStream().map(
      (event) => ExerciseData.fromMap(Map<String, dynamic>.from(event as Map)),
    );
    return _exerciseDataStream!;
  }

  /// Start an exercise session with the specified exercise type.
  ///
  /// [type] specifies the type of exercise (walking, running, cycling, etc.)
  ///
  /// Once started, real-time exercise data will be emitted through [exerciseDataStream].
  ///
  /// Requires an active connection to the device.
  /// Throws an exception if the device is not connected or if the operation fails.
  ///
  /// Example:
  /// ```dart
  /// await QringExercise.startExercise(ExerciseType.running);
  /// ```
  static Future<void> startExercise(ExerciseType type) async {
    try {
      await _channel.invokeMethod('startExercise', {
        'exerciseType': type.value,
      });
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to start exercise: ${e.message}');
    }
  }

  /// Pause the active exercise session.
  ///
  /// The exercise session remains active but data collection is paused.
  /// Use [resumeExercise] to continue the session.
  ///
  /// Requires an active connection to the device and an active exercise session.
  /// Throws an exception if the device is not connected, no exercise is active,
  /// or if the operation fails.
  ///
  /// Example:
  /// ```dart
  /// await QringExercise.pauseExercise();
  /// ```
  static Future<void> pauseExercise() async {
    try {
      await _channel.invokeMethod('pauseExercise');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      if (e.code == 'NO_ACTIVE_EXERCISE') {
        throw Exception('No active exercise session');
      }
      throw Exception('Failed to pause exercise: ${e.message}');
    }
  }

  /// Resume a paused exercise session.
  ///
  /// Continues data collection for a previously paused exercise session.
  ///
  /// Requires an active connection to the device and a paused exercise session.
  /// Throws an exception if the device is not connected, no exercise is active,
  /// or if the operation fails.
  ///
  /// Example:
  /// ```dart
  /// await QringExercise.resumeExercise();
  /// ```
  static Future<void> resumeExercise() async {
    try {
      await _channel.invokeMethod('resumeExercise');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      if (e.code == 'NO_ACTIVE_EXERCISE') {
        throw Exception('No active exercise session');
      }
      throw Exception('Failed to resume exercise: ${e.message}');
    }
  }

  /// Stop the active exercise session and retrieve summary data.
  ///
  /// Ends the current exercise session and returns an [ExerciseSummary] containing:
  /// - Total duration in seconds
  /// - Total steps taken
  /// - Total distance covered in meters
  /// - Total calories burned
  /// - Average heart rate
  /// - Maximum heart rate
  ///
  /// Requires an active connection to the device and an active exercise session.
  /// Throws an exception if the device is not connected, no exercise is active,
  /// or if the operation fails.
  ///
  /// Example:
  /// ```dart
  /// final summary = await QringExercise.stopExercise();
  /// print('Total duration: ${summary.durationSeconds}s');
  /// print('Total steps: ${summary.totalSteps}');
  /// print('Calories burned: ${summary.calories}');
  /// ```
  static Future<ExerciseSummary> stopExercise() async {
    try {
      final result = await _channel.invokeMethod<Map>('stopExercise');

      if (result == null) {
        throw Exception('No exercise summary received');
      }

      final map = Map<String, dynamic>.from(result);
      return ExerciseSummary.fromMap(map);
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      if (e.code == 'NO_ACTIVE_EXERCISE') {
        throw Exception('No active exercise session');
      }
      throw Exception('Failed to stop exercise: ${e.message}');
    }
  }
}
