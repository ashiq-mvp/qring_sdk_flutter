/// Heart rate measurement data.
class HeartRateData {
  /// Timestamp of the measurement
  final DateTime timestamp;

  /// Heart rate in beats per minute
  final int heartRate;

  HeartRateData({required this.timestamp, required this.heartRate});

  /// Create from map received from platform channel
  factory HeartRateData.fromMap(Map<String, dynamic> map) {
    return HeartRateData(
      timestamp: DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int),
      heartRate: map['heartRate'] as int? ?? 0,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'timestamp': timestamp.millisecondsSinceEpoch,
      'heartRate': heartRate,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is HeartRateData &&
          runtimeType == other.runtimeType &&
          timestamp == other.timestamp &&
          heartRate == other.heartRate;

  @override
  int get hashCode => timestamp.hashCode ^ heartRate.hashCode;

  @override
  String toString() =>
      'HeartRateData(timestamp: $timestamp, heartRate: $heartRate)';
}
