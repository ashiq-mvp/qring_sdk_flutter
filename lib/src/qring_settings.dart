import 'dart:async';

import 'package:flutter/services.dart';

import 'models/models.dart';

/// Settings API for QC Ring device configuration.
/// Manages continuous monitoring settings and device preferences.
class QringSettings {
  static const MethodChannel _channel = MethodChannel('qring_sdk_flutter');

  /// Configure continuous heart rate monitoring.
  ///
  /// [enable] - Enable or disable continuous monitoring
  /// [intervalMinutes] - Monitoring interval (must be 10, 15, 20, 30, or 60 minutes)
  ///
  /// Throws [ArgumentError] if interval is not one of the valid values.
  /// Throws [Exception] if device is not connected or command fails.
  static Future<void> setContinuousHeartRate({
    required bool enable,
    required int intervalMinutes,
  }) async {
    // Validate interval
    const validIntervals = [10, 15, 20, 30, 60];
    if (!validIntervals.contains(intervalMinutes)) {
      throw ArgumentError(
        'Heart rate interval must be one of: 10, 15, 20, 30, 60 minutes. Got: $intervalMinutes',
      );
    }

    try {
      await _channel.invokeMethod('setContinuousHeartRate', {
        'enable': enable,
        'intervalMinutes': intervalMinutes,
      });
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      if (e.code == 'INVALID_INTERVAL') {
        throw ArgumentError(e.message);
      }
      throw Exception('Failed to set continuous heart rate: ${e.message}');
    }
  }

  /// Get current continuous heart rate monitoring settings.
  ///
  /// Returns a map with keys:
  /// - 'enabled' (bool): Whether continuous monitoring is enabled
  /// - 'intervalMinutes' (int): Current monitoring interval
  ///
  /// Throws [Exception] if device is not connected or command fails.
  static Future<ContinuousHeartRateSettings>
  getContinuousHeartRateSettings() async {
    try {
      final result = await _channel.invokeMethod<Map>(
        'getContinuousHeartRateSettings',
      );
      final map = Map<String, dynamic>.from(result ?? {});
      return ContinuousHeartRateSettings.fromMap(map);
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception(
        'Failed to get continuous heart rate settings: ${e.message}',
      );
    }
  }

  /// Configure continuous blood oxygen monitoring.
  ///
  /// [enable] - Enable or disable continuous monitoring
  /// [intervalMinutes] - Monitoring interval in minutes (must be positive)
  ///
  /// Throws [ArgumentError] if interval is not positive.
  /// Throws [Exception] if device is not connected or command fails.
  static Future<void> setContinuousBloodOxygen({
    required bool enable,
    required int intervalMinutes,
  }) async {
    if (intervalMinutes <= 0) {
      throw ArgumentError(
        'Blood oxygen interval must be positive. Got: $intervalMinutes',
      );
    }

    try {
      await _channel.invokeMethod('setContinuousBloodOxygen', {
        'enable': enable,
        'intervalMinutes': intervalMinutes,
      });
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      if (e.code == 'INVALID_INTERVAL') {
        throw ArgumentError(e.message);
      }
      throw Exception('Failed to set continuous blood oxygen: ${e.message}');
    }
  }

  /// Get current continuous blood oxygen monitoring settings.
  ///
  /// Returns a map with keys:
  /// - 'enabled' (bool): Whether continuous monitoring is enabled
  /// - 'intervalMinutes' (int): Current monitoring interval
  ///
  /// Throws [Exception] if device is not connected or command fails.
  static Future<ContinuousBloodOxygenSettings>
  getContinuousBloodOxygenSettings() async {
    try {
      final result = await _channel.invokeMethod<Map>(
        'getContinuousBloodOxygenSettings',
      );
      final map = Map<String, dynamic>.from(result ?? {});
      return ContinuousBloodOxygenSettings.fromMap(map);
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception(
        'Failed to get continuous blood oxygen settings: ${e.message}',
      );
    }
  }

  /// Configure continuous blood pressure monitoring.
  ///
  /// [enable] - Enable or disable continuous monitoring
  ///
  /// Throws [Exception] if device is not connected or command fails.
  static Future<void> setContinuousBloodPressure({required bool enable}) async {
    try {
      await _channel.invokeMethod('setContinuousBloodPressure', {
        'enable': enable,
      });
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to set continuous blood pressure: ${e.message}');
    }
  }

  /// Get current continuous blood pressure monitoring settings.
  ///
  /// Returns a map with keys:
  /// - 'enabled' (bool): Whether continuous monitoring is enabled
  ///
  /// Throws [Exception] if device is not connected or command fails.
  static Future<ContinuousBloodPressureSettings>
  getContinuousBloodPressureSettings() async {
    try {
      final result = await _channel.invokeMethod<Map>(
        'getContinuousBloodPressureSettings',
      );
      final map = Map<String, dynamic>.from(result ?? {});
      return ContinuousBloodPressureSettings.fromMap(map);
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception(
        'Failed to get continuous blood pressure settings: ${e.message}',
      );
    }
  }

  /// Configure display settings.
  ///
  /// [settings] - Display settings to apply
  ///
  /// Throws [ArgumentError] if brightness is not in valid range.
  /// Throws [Exception] if device is not connected or command fails.
  static Future<void> setDisplaySettings(DisplaySettings settings) async {
    // Validate brightness
    if (settings.brightness < 1 ||
        settings.brightness > settings.maxBrightness) {
      throw ArgumentError(
        'Brightness must be between 1 and ${settings.maxBrightness}. Got: ${settings.brightness}',
      );
    }

    try {
      await _channel.invokeMethod('setDisplaySettings', settings.toMap());
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      if (e.code == 'INVALID_BRIGHTNESS') {
        throw ArgumentError(e.message);
      }
      throw Exception('Failed to set display settings: ${e.message}');
    }
  }

  /// Get current display settings.
  ///
  /// Returns the current display configuration.
  ///
  /// Throws [Exception] if device is not connected or command fails.
  static Future<DisplaySettings> getDisplaySettings() async {
    try {
      final result = await _channel.invokeMethod<Map>('getDisplaySettings');
      final map = Map<String, dynamic>.from(result ?? {});
      return DisplaySettings.fromMap(map);
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to get display settings: ${e.message}');
    }
  }

  /// Configure user information.
  ///
  /// [userInfo] - User profile information
  ///
  /// Throws [Exception] if device is not connected or command fails.
  static Future<void> setUserInfo(UserInfo userInfo) async {
    try {
      await _channel.invokeMethod('setUserInfo', userInfo.toMap());
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to set user info: ${e.message}');
    }
  }

  /// Set user ID on the device.
  ///
  /// [userId] - User identifier string
  ///
  /// Throws [ArgumentError] if userId is empty.
  /// Throws [Exception] if device is not connected or command fails.
  static Future<void> setUserId(String userId) async {
    if (userId.isEmpty) {
      throw ArgumentError('User ID cannot be empty');
    }

    try {
      await _channel.invokeMethod('setUserId', {'userId': userId});
    } on PlatformException catch (e) {
      if (e.code == 'NOT_CONNECTED') {
        throw Exception('Device is not connected');
      }
      throw Exception('Failed to set user ID: ${e.message}');
    }
  }
}

/// Continuous heart rate monitoring settings.
class ContinuousHeartRateSettings {
  /// Whether continuous monitoring is enabled.
  final bool enabled;

  /// Monitoring interval in minutes (10, 15, 20, 30, or 60).
  final int intervalMinutes;

  ContinuousHeartRateSettings({
    required this.enabled,
    required this.intervalMinutes,
  });

  factory ContinuousHeartRateSettings.fromMap(Map<String, dynamic> map) {
    return ContinuousHeartRateSettings(
      enabled: map['enabled'] as bool? ?? false,
      intervalMinutes: map['intervalMinutes'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toMap() {
    return {'enabled': enabled, 'intervalMinutes': intervalMinutes};
  }

  @override
  String toString() {
    return 'ContinuousHeartRateSettings(enabled: $enabled, intervalMinutes: $intervalMinutes)';
  }
}

/// Continuous blood oxygen monitoring settings.
class ContinuousBloodOxygenSettings {
  /// Whether continuous monitoring is enabled.
  final bool enabled;

  /// Monitoring interval in minutes.
  final int intervalMinutes;

  ContinuousBloodOxygenSettings({
    required this.enabled,
    required this.intervalMinutes,
  });

  factory ContinuousBloodOxygenSettings.fromMap(Map<String, dynamic> map) {
    return ContinuousBloodOxygenSettings(
      enabled: map['enabled'] as bool? ?? false,
      intervalMinutes: map['intervalMinutes'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toMap() {
    return {'enabled': enabled, 'intervalMinutes': intervalMinutes};
  }

  @override
  String toString() {
    return 'ContinuousBloodOxygenSettings(enabled: $enabled, intervalMinutes: $intervalMinutes)';
  }
}

/// Continuous blood pressure monitoring settings.
class ContinuousBloodPressureSettings {
  /// Whether continuous monitoring is enabled.
  final bool enabled;

  ContinuousBloodPressureSettings({required this.enabled});

  factory ContinuousBloodPressureSettings.fromMap(Map<String, dynamic> map) {
    return ContinuousBloodPressureSettings(
      enabled: map['enabled'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toMap() {
    return {'enabled': enabled};
  }

  @override
  String toString() {
    return 'ContinuousBloodPressureSettings(enabled: $enabled)';
  }
}
