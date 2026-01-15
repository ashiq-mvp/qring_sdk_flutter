/// Represents the state of the background service.
///
/// This model contains information about the background service that maintains
/// continuous communication with the QRing device, including connection status,
/// device information, and reconnection attempts.
class ServiceState {
  /// Whether the background service is currently running
  final bool isRunning;

  /// Whether the device is currently connected
  final bool isConnected;

  /// MAC address of the connected or target device
  final String? deviceMac;

  /// Name of the connected device
  final String? deviceName;

  /// Number of consecutive reconnection attempts
  final int reconnectAttempts;

  /// Timestamp of the last successful connection
  final DateTime? lastConnectedTime;

  ServiceState({
    required this.isRunning,
    required this.isConnected,
    this.deviceMac,
    this.deviceName,
    this.reconnectAttempts = 0,
    this.lastConnectedTime,
  });

  /// Create from map received from platform channel
  factory ServiceState.fromMap(Map<String, dynamic> map) {
    return ServiceState(
      isRunning: map['isRunning'] as bool? ?? false,
      isConnected: map['isConnected'] as bool? ?? false,
      deviceMac: map['deviceMac'] as String?,
      deviceName: map['deviceName'] as String?,
      reconnectAttempts: map['reconnectAttempts'] as int? ?? 0,
      lastConnectedTime: map['lastConnectedTime'] != null
          ? DateTime.parse(map['lastConnectedTime'] as String)
          : null,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'isRunning': isRunning,
      'isConnected': isConnected,
      'deviceMac': deviceMac,
      'deviceName': deviceName,
      'reconnectAttempts': reconnectAttempts,
      'lastConnectedTime': lastConnectedTime?.toIso8601String(),
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ServiceState &&
          runtimeType == other.runtimeType &&
          isRunning == other.isRunning &&
          isConnected == other.isConnected &&
          deviceMac == other.deviceMac &&
          deviceName == other.deviceName &&
          reconnectAttempts == other.reconnectAttempts &&
          lastConnectedTime == other.lastConnectedTime;

  @override
  int get hashCode =>
      isRunning.hashCode ^
      isConnected.hashCode ^
      deviceMac.hashCode ^
      deviceName.hashCode ^
      reconnectAttempts.hashCode ^
      lastConnectedTime.hashCode;

  @override
  String toString() =>
      'ServiceState(isRunning: $isRunning, isConnected: $isConnected, '
      'deviceMac: $deviceMac, deviceName: $deviceName, '
      'reconnectAttempts: $reconnectAttempts, lastConnectedTime: $lastConnectedTime)';
}
