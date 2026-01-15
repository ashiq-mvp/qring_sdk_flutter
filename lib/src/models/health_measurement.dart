/// Measurement type enumeration.
enum MeasurementType {
  /// Heart rate measurement
  heartRate,

  /// Blood pressure measurement
  bloodPressure,

  /// Blood oxygen measurement
  bloodOxygen,

  /// Temperature measurement
  temperature,

  /// Pressure measurement
  pressure,

  /// HRV measurement
  hrv,

  /// Step count change notification
  stepCount;

  /// Create from string received from platform channel
  static MeasurementType fromString(String value) {
    switch (value.toLowerCase()) {
      case 'heartrate':
      case 'heart_rate':
        return MeasurementType.heartRate;
      case 'bloodpressure':
      case 'blood_pressure':
        return MeasurementType.bloodPressure;
      case 'bloodoxygen':
      case 'blood_oxygen':
        return MeasurementType.bloodOxygen;
      case 'temperature':
        return MeasurementType.temperature;
      case 'pressure':
        return MeasurementType.pressure;
      case 'hrv':
        return MeasurementType.hrv;
      case 'stepcount':
      case 'step_count':
        return MeasurementType.stepCount;
      default:
        return MeasurementType.heartRate;
    }
  }

  /// Convert to string for platform channel
  String toStringValue() {
    return name;
  }
}

/// Real-time measurement result.
class HealthMeasurement {
  /// Type of measurement
  final MeasurementType type;

  /// Timestamp of the measurement
  final DateTime timestamp;

  /// Heart rate in beats per minute (if applicable)
  final int? heartRate;

  /// Systolic blood pressure in mmHg (if applicable)
  final int? systolic;

  /// Diastolic blood pressure in mmHg (if applicable)
  final int? diastolic;

  /// Blood oxygen saturation percentage (if applicable)
  final int? spO2;

  /// Temperature in Celsius (if applicable)
  final double? temperature;

  /// Pressure value (if applicable)
  final int? pressure;

  /// HRV value (if applicable)
  final int? hrv;

  /// Step count (if applicable)
  final int? steps;

  /// Whether the measurement was successful
  final bool success;

  /// Error message if measurement failed
  final String? errorMessage;

  HealthMeasurement({
    required this.type,
    required this.timestamp,
    this.heartRate,
    this.systolic,
    this.diastolic,
    this.spO2,
    this.temperature,
    this.pressure,
    this.hrv,
    this.steps,
    required this.success,
    this.errorMessage,
  });

  /// Create from map received from platform channel
  factory HealthMeasurement.fromMap(Map<String, dynamic> map) {
    return HealthMeasurement(
      type: MeasurementType.fromString(map['type'] as String? ?? 'heartRate'),
      timestamp: DateTime.fromMillisecondsSinceEpoch(
        map['timestamp'] as int? ?? DateTime.now().millisecondsSinceEpoch,
      ),
      heartRate: map['heartRate'] as int?,
      systolic: map['systolic'] as int?,
      diastolic: map['diastolic'] as int?,
      spO2: map['spO2'] as int?,
      temperature: (map['temperature'] as num?)?.toDouble(),
      pressure: map['pressure'] as int?,
      hrv: map['hrv'] as int?,
      steps: map['steps'] as int?,
      success: map['success'] as bool? ?? false,
      errorMessage: map['errorMessage'] as String?,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'type': type.toStringValue(),
      'timestamp': timestamp.millisecondsSinceEpoch,
      'heartRate': heartRate,
      'systolic': systolic,
      'diastolic': diastolic,
      'spO2': spO2,
      'temperature': temperature,
      'pressure': pressure,
      'hrv': hrv,
      'steps': steps,
      'success': success,
      'errorMessage': errorMessage,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is HealthMeasurement &&
          runtimeType == other.runtimeType &&
          type == other.type &&
          timestamp == other.timestamp &&
          heartRate == other.heartRate &&
          systolic == other.systolic &&
          diastolic == other.diastolic &&
          spO2 == other.spO2 &&
          temperature == other.temperature &&
          pressure == other.pressure &&
          hrv == other.hrv &&
          steps == other.steps &&
          success == other.success &&
          errorMessage == other.errorMessage;

  @override
  int get hashCode =>
      type.hashCode ^
      timestamp.hashCode ^
      heartRate.hashCode ^
      systolic.hashCode ^
      diastolic.hashCode ^
      spO2.hashCode ^
      temperature.hashCode ^
      pressure.hashCode ^
      hrv.hashCode ^
      steps.hashCode ^
      success.hashCode ^
      errorMessage.hashCode;

  @override
  String toString() =>
      'HealthMeasurement(type: $type, timestamp: $timestamp, heartRate: $heartRate, '
      'systolic: $systolic, diastolic: $diastolic, spO2: $spO2, temperature: $temperature, '
      'pressure: $pressure, hrv: $hrv, steps: $steps, success: $success, errorMessage: $errorMessage)';
}
