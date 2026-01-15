import 'dart:async';

import 'package:flutter/services.dart';
import 'package:qring_sdk_flutter/src/models/blood_oxygen_data.dart';
import 'package:qring_sdk_flutter/src/models/blood_pressure_data.dart';
import 'package:qring_sdk_flutter/src/models/health_measurement.dart';
import 'package:qring_sdk_flutter/src/models/heart_rate_data.dart';
import 'package:qring_sdk_flutter/src/models/sleep_data.dart';
import 'package:qring_sdk_flutter/src/models/step_data.dart';

/// API class for health data synchronization operations.
/// Provides methods to sync historical health data from the QC Ring device.
class QringHealthData {
  static const MethodChannel _channel = MethodChannel('qring_sdk_flutter');
  static const EventChannel _measurementChannel = EventChannel(
    'qring_sdk_flutter/measurement',
  );
  static const EventChannel _notificationChannel = EventChannel(
    'qring_sdk_flutter/notification',
  );

  static Stream<HealthMeasurement>? _measurementStream;
  static Stream<HealthMeasurement>? _notificationStream;

  /// Stream of real-time health measurements.
  /// Subscribe to this stream to receive manual measurement results.
  static Stream<HealthMeasurement> get measurementStream {
    _measurementStream ??= _measurementChannel.receiveBroadcastStream().map(
      (event) =>
          HealthMeasurement.fromMap(Map<String, dynamic>.from(event as Map)),
    );
    return _measurementStream!;
  }

  /// Stream of automatic health notifications.
  /// Subscribe to this stream to receive automatic measurements from the ring
  /// when continuous monitoring is enabled.
  ///
  /// This stream emits notifications for:
  /// - Heart rate measurements
  /// - Blood pressure measurements
  /// - Blood oxygen measurements
  /// - Temperature measurements
  /// - Step count changes
  static Stream<HealthMeasurement> get notificationStream {
    _notificationStream ??= _notificationChannel.receiveBroadcastStream().map(
      (event) =>
          HealthMeasurement.fromMap(Map<String, dynamic>.from(event as Map)),
    );
    return _notificationStream!;
  }

  /// Synchronize step data for a specific day.
  ///
  /// [dayOffset] specifies the number of days before today:
  /// - 0 = today
  /// - 1 = yesterday
  /// - 2 = 2 days ago
  /// - etc. (maximum 6 days)
  ///
  /// Returns a [StepData] object containing step count, distance, calories, etc.
  /// Throws an exception if the device is not connected or if the sync fails.
  static Future<StepData> syncStepData(int dayOffset) async {
    if (dayOffset < 0 || dayOffset > 6) {
      throw ArgumentError('Day offset must be between 0 and 6');
    }

    try {
      final result = await _channel.invokeMethod<Map>('syncStepData', {
        'dayOffset': dayOffset,
      });

      if (result == null) {
        throw Exception('No step data received');
      }

      final map = Map<String, dynamic>.from(result);
      return StepData.fromMap(map);
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to sync step data: ${e.message}');
    }
  }

  /// Synchronize heart rate data for a specific day.
  ///
  /// [dayOffset] specifies the number of days before today (0-6).
  ///
  /// Returns a list of [HeartRateData] objects containing heart rate measurements
  /// throughout the day (typically in 5-minute intervals).
  /// Throws an exception if the device is not connected or if the sync fails.
  static Future<List<HeartRateData>> syncHeartRateData(int dayOffset) async {
    if (dayOffset < 0 || dayOffset > 6) {
      throw ArgumentError('Day offset must be between 0 and 6');
    }

    try {
      final result = await _channel.invokeMethod<List>('syncHeartRateData', {
        'dayOffset': dayOffset,
      });

      if (result == null) {
        return [];
      }

      return result.map((item) {
        final map = Map<String, dynamic>.from(item as Map);
        return HeartRateData.fromMap(map);
      }).toList();
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to sync heart rate data: ${e.message}');
    }
  }

  /// Synchronize sleep data for a specific day.
  ///
  /// [dayOffset] specifies the number of days before today (0-6).
  ///
  /// Returns a [SleepData] object containing sleep stages, durations, and timing.
  /// Throws an exception if the device is not connected or if the sync fails.
  static Future<SleepData> syncSleepData(int dayOffset) async {
    if (dayOffset < 0 || dayOffset > 6) {
      throw ArgumentError('Day offset must be between 0 and 6');
    }

    try {
      final result = await _channel.invokeMethod<Map>('syncSleepData', {
        'dayOffset': dayOffset,
      });

      if (result == null) {
        throw Exception('No sleep data received');
      }

      final map = Map<String, dynamic>.from(result);
      return SleepData.fromMap(map);
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to sync sleep data: ${e.message}');
    }
  }

  /// Synchronize blood oxygen (SpO2) data for a specific day.
  ///
  /// [dayOffset] specifies the number of days before today (0-6).
  ///
  /// Returns a list of [BloodOxygenData] objects containing SpO2 measurements
  /// throughout the day.
  /// Throws an exception if the device is not connected or if the sync fails.
  static Future<List<BloodOxygenData>> syncBloodOxygenData(
    int dayOffset,
  ) async {
    if (dayOffset < 0 || dayOffset > 6) {
      throw ArgumentError('Day offset must be between 0 and 6');
    }

    try {
      final result = await _channel.invokeMethod<List>('syncBloodOxygenData', {
        'dayOffset': dayOffset,
      });

      if (result == null) {
        return [];
      }

      return result.map((item) {
        final map = Map<String, dynamic>.from(item as Map);
        return BloodOxygenData.fromMap(map);
      }).toList();
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to sync blood oxygen data: ${e.message}');
    }
  }

  /// Synchronize blood pressure data for a specific day.
  ///
  /// [dayOffset] specifies the number of days before today (0-6).
  ///
  /// Returns a list of [BloodPressureData] objects containing systolic and
  /// diastolic blood pressure measurements throughout the day.
  /// Throws an exception if the device is not connected or if the sync fails.
  static Future<List<BloodPressureData>> syncBloodPressureData(
    int dayOffset,
  ) async {
    if (dayOffset < 0 || dayOffset > 6) {
      throw ArgumentError('Day offset must be between 0 and 6');
    }

    try {
      final result = await _channel.invokeMethod<List>(
        'syncBloodPressureData',
        {'dayOffset': dayOffset},
      );

      if (result == null) {
        return [];
      }

      return result.map((item) {
        final map = Map<String, dynamic>.from(item as Map);
        return BloodPressureData.fromMap(map);
      }).toList();
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to sync blood pressure data: ${e.message}');
    }
  }

  /// Start a manual heart rate measurement.
  ///
  /// The measurement result will be emitted through the [measurementStream].
  /// Requires an active connection to the device.
  /// Throws an exception if the device is not connected.
  static Future<void> startHeartRateMeasurement() async {
    try {
      await _channel.invokeMethod('startHeartRateMeasurement');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to start heart rate measurement: ${e.message}');
    }
  }

  /// Start a manual blood pressure measurement.
  ///
  /// The measurement result will be emitted through the [measurementStream].
  /// Requires an active connection to the device.
  /// Throws an exception if the device is not connected.
  static Future<void> startBloodPressureMeasurement() async {
    try {
      await _channel.invokeMethod('startBloodPressureMeasurement');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception(
        'Failed to start blood pressure measurement: ${e.message}',
      );
    }
  }

  /// Start a manual blood oxygen (SpO2) measurement.
  ///
  /// The measurement result will be emitted through the [measurementStream].
  /// Requires an active connection to the device.
  /// Throws an exception if the device is not connected.
  static Future<void> startBloodOxygenMeasurement() async {
    try {
      await _channel.invokeMethod('startBloodOxygenMeasurement');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to start blood oxygen measurement: ${e.message}');
    }
  }

  /// Start a manual temperature measurement.
  ///
  /// The measurement result will be emitted through the [measurementStream].
  /// Requires an active connection to the device.
  /// Throws an exception if the device is not connected.
  static Future<void> startTemperatureMeasurement() async {
    try {
      await _channel.invokeMethod('startTemperatureMeasurement');
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to start temperature measurement: ${e.message}');
    }
  }

  /// Stop any active manual measurement.
  ///
  /// This will cancel the currently running measurement if any.
  /// Safe to call even if no measurement is active.
  static Future<void> stopMeasurement() async {
    try {
      await _channel.invokeMethod('stopMeasurement');
    } on PlatformException catch (e) {
      throw Exception('Failed to stop measurement: ${e.message}');
    }
  }
}
