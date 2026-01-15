/// Contains device information and capabilities.
class QringDeviceInfo {
  /// Firmware version
  final String firmwareVersion;

  /// Hardware version
  final String hardwareVersion;

  /// Whether device supports temperature measurement
  final bool supportsTemperature;

  /// Whether device supports blood oxygen measurement
  final bool supportsBloodOxygen;

  /// Whether device supports blood pressure measurement
  final bool supportsBloodPressure;

  /// Whether device supports HRV measurement
  final bool supportsHrv;

  /// Whether device supports one-key check
  final bool supportsOneKeyCheck;

  QringDeviceInfo({
    required this.firmwareVersion,
    required this.hardwareVersion,
    required this.supportsTemperature,
    required this.supportsBloodOxygen,
    required this.supportsBloodPressure,
    required this.supportsHrv,
    required this.supportsOneKeyCheck,
  });

  /// Create from map received from platform channel
  factory QringDeviceInfo.fromMap(Map<String, dynamic> map) {
    return QringDeviceInfo(
      firmwareVersion: map['firmwareVersion'] as String? ?? '',
      hardwareVersion: map['hardwareVersion'] as String? ?? '',
      supportsTemperature: map['supportsTemperature'] as bool? ?? false,
      supportsBloodOxygen: map['supportsBloodOxygen'] as bool? ?? false,
      supportsBloodPressure: map['supportsBloodPressure'] as bool? ?? false,
      supportsHrv: map['supportsHrv'] as bool? ?? false,
      supportsOneKeyCheck: map['supportsOneKeyCheck'] as bool? ?? false,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'firmwareVersion': firmwareVersion,
      'hardwareVersion': hardwareVersion,
      'supportsTemperature': supportsTemperature,
      'supportsBloodOxygen': supportsBloodOxygen,
      'supportsBloodPressure': supportsBloodPressure,
      'supportsHrv': supportsHrv,
      'supportsOneKeyCheck': supportsOneKeyCheck,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is QringDeviceInfo &&
          runtimeType == other.runtimeType &&
          firmwareVersion == other.firmwareVersion &&
          hardwareVersion == other.hardwareVersion &&
          supportsTemperature == other.supportsTemperature &&
          supportsBloodOxygen == other.supportsBloodOxygen &&
          supportsBloodPressure == other.supportsBloodPressure &&
          supportsHrv == other.supportsHrv &&
          supportsOneKeyCheck == other.supportsOneKeyCheck;

  @override
  int get hashCode =>
      firmwareVersion.hashCode ^
      hardwareVersion.hashCode ^
      supportsTemperature.hashCode ^
      supportsBloodOxygen.hashCode ^
      supportsBloodPressure.hashCode ^
      supportsHrv.hashCode ^
      supportsOneKeyCheck.hashCode;

  @override
  String toString() =>
      'QringDeviceInfo(firmwareVersion: $firmwareVersion, hardwareVersion: $hardwareVersion, '
      'supportsTemperature: $supportsTemperature, supportsBloodOxygen: $supportsBloodOxygen, '
      'supportsBloodPressure: $supportsBloodPressure, supportsHrv: $supportsHrv, '
      'supportsOneKeyCheck: $supportsOneKeyCheck)';
}
