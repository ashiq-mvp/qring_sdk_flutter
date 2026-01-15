/// Represents a BLE error event from the BLE Connection Manager.
///
/// This class encapsulates error information emitted by the production-grade
/// BLE Connection Manager when operations fail.
class BleError {
  /// Error code identifying the type of error
  final String code;

  /// Human-readable error message
  final String message;

  /// Timestamp of the error (milliseconds since epoch)
  final int timestamp;

  /// MAC address of the device (if available)
  final String? deviceMac;

  /// Type of error for easier categorization
  final BleErrorType type;

  const BleError({
    required this.code,
    required this.message,
    required this.timestamp,
    this.deviceMac,
    required this.type,
  });

  /// Create from map received from platform channel
  factory BleError.fromMap(Map<String, dynamic> map) {
    final code = map['errorCode'] as String? ?? 'UNKNOWN_ERROR';
    return BleError(
      code: code,
      message: map['errorMessage'] as String? ?? 'Unknown error occurred',
      timestamp:
          map['timestamp'] as int? ?? DateTime.now().millisecondsSinceEpoch,
      deviceMac: map['deviceMac'] as String?,
      type: BleErrorType.fromCode(code),
    );
  }

  /// Convert to map for serialization
  Map<String, dynamic> toMap() {
    return {
      'errorCode': code,
      'errorMessage': message,
      'timestamp': timestamp,
      if (deviceMac != null) 'deviceMac': deviceMac,
      'errorType': type.name,
    };
  }

  @override
  String toString() {
    return 'BleError{code: $code, message: $message, type: $type, deviceMac: $deviceMac}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is BleError &&
        other.code == code &&
        other.message == message &&
        other.timestamp == timestamp &&
        other.deviceMac == deviceMac &&
        other.type == type;
  }

  @override
  int get hashCode {
    return Object.hash(code, message, timestamp, deviceMac, type);
  }
}

/// Categorizes BLE errors for easier handling in the UI.
enum BleErrorType {
  /// Required Bluetooth permission not granted
  permissionDenied,

  /// Bluetooth is disabled on the device
  bluetoothOff,

  /// Device not found during scan or connection
  deviceNotFound,

  /// Device pairing/bonding failed
  pairingFailed,

  /// Connection attempt failed
  connectionFailed,

  /// Connection attempt timed out
  connectionTimeout,

  /// GATT operation failed
  gattError,

  /// Device command execution failed
  commandFailed,

  /// Auto-reconnection failed
  reconnectionFailed,

  /// Unknown or unclassified error
  unknown;

  /// Create from error code string
  static BleErrorType fromCode(String code) {
    switch (code.toUpperCase()) {
      case 'PERMISSION_DENIED':
        return BleErrorType.permissionDenied;
      case 'BLUETOOTH_OFF':
        return BleErrorType.bluetoothOff;
      case 'DEVICE_NOT_FOUND':
        return BleErrorType.deviceNotFound;
      case 'PAIRING_FAILED':
        return BleErrorType.pairingFailed;
      case 'CONNECTION_FAILED':
        return BleErrorType.connectionFailed;
      case 'CONNECTION_TIMEOUT':
        return BleErrorType.connectionTimeout;
      case 'GATT_ERROR':
        return BleErrorType.gattError;
      case 'COMMAND_FAILED':
        return BleErrorType.commandFailed;
      case 'RECONNECTION_FAILED':
        return BleErrorType.reconnectionFailed;
      default:
        return BleErrorType.unknown;
    }
  }

  /// Get a user-friendly description of the error type
  String get description {
    switch (this) {
      case BleErrorType.permissionDenied:
        return 'Bluetooth permission not granted';
      case BleErrorType.bluetoothOff:
        return 'Bluetooth is disabled';
      case BleErrorType.deviceNotFound:
        return 'Device not found';
      case BleErrorType.pairingFailed:
        return 'Failed to pair with device';
      case BleErrorType.connectionFailed:
        return 'Connection failed';
      case BleErrorType.connectionTimeout:
        return 'Connection timed out';
      case BleErrorType.gattError:
        return 'Communication error';
      case BleErrorType.commandFailed:
        return 'Command failed';
      case BleErrorType.reconnectionFailed:
        return 'Reconnection failed';
      case BleErrorType.unknown:
        return 'Unknown error';
    }
  }
}
