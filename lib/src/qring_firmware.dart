import 'dart:async';

import 'package:flutter/services.dart';

/// API for managing firmware updates on the QC Ring device.
///
/// This class provides methods to validate firmware files and perform
/// over-the-air (OTA) firmware updates via Bluetooth.
class QringFirmware {
  static const MethodChannel _channel = MethodChannel('qring_sdk_flutter');
  static const EventChannel _progressChannel = EventChannel(
    'qring_sdk_flutter/firmware_progress',
  );

  /// Validate a firmware file before starting the update process.
  ///
  /// This method checks if the firmware file exists, is readable, and
  /// passes the SDK's validation checks.
  ///
  /// Parameters:
  /// - [filePath]: Absolute path to the firmware file
  ///
  /// Returns:
  /// - `true` if the file is valid and can be used for update
  /// - `false` if the file is invalid
  ///
  /// Throws:
  /// - [Exception] if validation fails due to file access issues or SDK errors
  ///
  /// Example:
  /// ```dart
  /// final isValid = await QringFirmware.validateFirmwareFile('/path/to/firmware.bin');
  /// if (isValid) {
  ///   print('Firmware file is valid');
  /// } else {
  ///   print('Invalid firmware file');
  /// }
  /// ```
  static Future<bool> validateFirmwareFile(String filePath) async {
    if (filePath.isEmpty) {
      throw ArgumentError('File path cannot be empty');
    }

    try {
      final result = await _channel.invokeMethod<bool>('validateFirmwareFile', {
        'filePath': filePath,
      });
      return result ?? false;
    } on PlatformException catch (e) {
      if (e.code == 'FILE_NOT_FOUND') {
        throw Exception('Firmware file not found: ${e.message}');
      } else if (e.code == 'FILE_NOT_READABLE') {
        throw Exception('Cannot read firmware file: ${e.message}');
      } else if (e.code == 'INVALID_FILE') {
        throw Exception('Invalid firmware file: ${e.message}');
      }
      throw Exception('Failed to validate firmware file: ${e.message}');
    }
  }

  /// Start the firmware update process.
  ///
  /// This method initiates a Device Firmware Update (DFU) process using the
  /// specified firmware file. The update progress can be monitored through
  /// the [updateProgressStream].
  ///
  /// Important:
  /// - The firmware file should be validated using [validateFirmwareFile] first
  /// - Only one update can be in progress at a time
  /// - The device should remain connected during the entire update process
  /// - Do not disconnect or turn off the device during update
  ///
  /// Parameters:
  /// - [filePath]: Absolute path to the validated firmware file
  ///
  /// Throws:
  /// - [Exception] if the update fails to start or if an update is already in progress
  ///
  /// Example:
  /// ```dart
  /// // Listen to progress updates
  /// QringFirmware.updateProgressStream.listen((progress) {
  ///   print('Update progress: $progress%');
  /// });
  ///
  /// // Start the update
  /// try {
  ///   await QringFirmware.startFirmwareUpdate('/path/to/firmware.bin');
  ///   print('Firmware update started');
  /// } catch (e) {
  ///   print('Failed to start update: $e');
  /// }
  /// ```
  static Future<void> startFirmwareUpdate(String filePath) async {
    if (filePath.isEmpty) {
      throw ArgumentError('File path cannot be empty');
    }

    try {
      await _channel.invokeMethod('startFirmwareUpdate', {
        'filePath': filePath,
      });
    } on PlatformException catch (e) {
      if (e.code == 'FILE_NOT_FOUND') {
        throw Exception('Firmware file not found: ${e.message}');
      } else if (e.code == 'UPDATE_IN_PROGRESS') {
        throw Exception('Firmware update is already in progress');
      } else if (e.code == 'UPDATE_FAILED') {
        throw Exception('Firmware update failed: ${e.message}');
      }
      throw Exception('Failed to start firmware update: ${e.message}');
    }
  }

  /// Stream of firmware update progress events.
  ///
  /// This stream emits progress updates during a firmware update process.
  /// The stream provides a map with the following keys:
  ///
  /// - `progress` (int): Update progress percentage (0-100)
  /// - `status` (String): Current status - "updating", "completed", or "failed"
  /// - `errorCode` (int, optional): Error code if status is "failed"
  /// - `errorMessage` (String, optional): Error message if status is "failed"
  ///
  /// The stream will emit:
  /// 1. Multiple progress updates with status "updating" and progress 0-99
  /// 2. A final event with status "completed" and progress 100 on success
  /// 3. An error event with status "failed" if the update fails
  ///
  /// Example:
  /// ```dart
  /// QringFirmware.updateProgressStream.listen((event) {
  ///   final progress = event['progress'] as int;
  ///   final status = event['status'] as String;
  ///
  ///   if (status == 'updating') {
  ///     print('Updating: $progress%');
  ///   } else if (status == 'completed') {
  ///     print('Update completed successfully!');
  ///   } else if (status == 'failed') {
  ///     final errorMsg = event['errorMessage'] as String;
  ///     print('Update failed: $errorMsg');
  ///   }
  /// });
  /// ```
  static Stream<Map<String, dynamic>> get updateProgressStream {
    return _progressChannel.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event as Map);
    });
  }
}
