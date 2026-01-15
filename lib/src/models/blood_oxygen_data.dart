/// Blood oxygen saturation data.
class BloodOxygenData {
  /// Timestamp of the measurement
  final DateTime timestamp;

  /// Blood oxygen saturation percentage (SpO2)
  final int spO2;

  BloodOxygenData({required this.timestamp, required this.spO2});

  /// Create from map received from platform channel
  factory BloodOxygenData.fromMap(Map<String, dynamic> map) {
    return BloodOxygenData(
      timestamp: DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int),
      spO2: map['spO2'] as int? ?? 0,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {'timestamp': timestamp.millisecondsSinceEpoch, 'spO2': spO2};
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is BloodOxygenData &&
          runtimeType == other.runtimeType &&
          timestamp == other.timestamp &&
          spO2 == other.spO2;

  @override
  int get hashCode => timestamp.hashCode ^ spO2.hashCode;

  @override
  String toString() => 'BloodOxygenData(timestamp: $timestamp, spO2: $spO2)';
}
