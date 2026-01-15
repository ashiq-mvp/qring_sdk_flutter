/// Blood pressure measurement data.
class BloodPressureData {
  /// Timestamp of the measurement
  final DateTime timestamp;

  /// Systolic blood pressure (mmHg)
  final int systolic;

  /// Diastolic blood pressure (mmHg)
  final int diastolic;

  BloodPressureData({
    required this.timestamp,
    required this.systolic,
    required this.diastolic,
  });

  /// Create from map received from platform channel
  factory BloodPressureData.fromMap(Map<String, dynamic> map) {
    return BloodPressureData(
      timestamp: DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int),
      systolic: map['systolic'] as int? ?? 0,
      diastolic: map['diastolic'] as int? ?? 0,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'timestamp': timestamp.millisecondsSinceEpoch,
      'systolic': systolic,
      'diastolic': diastolic,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is BloodPressureData &&
          runtimeType == other.runtimeType &&
          timestamp == other.timestamp &&
          systolic == other.systolic &&
          diastolic == other.diastolic;

  @override
  int get hashCode =>
      timestamp.hashCode ^ systolic.hashCode ^ diastolic.hashCode;

  @override
  String toString() =>
      'BloodPressureData(timestamp: $timestamp, systolic: $systolic, diastolic: $diastolic)';
}
