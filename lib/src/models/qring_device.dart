/// Represents a discovered BLE device.
class QringDevice {
  /// Device name
  final String name;

  /// Device MAC address
  final String macAddress;

  /// Signal strength (RSSI)
  final int rssi;

  /// Timestamp when device was last seen (milliseconds since epoch)
  final int? lastSeen;

  /// Raw advertisement data (hex string, only available in debug mode)
  final String? rawAdvertisementData;

  QringDevice({
    required this.name,
    required this.macAddress,
    required this.rssi,
    this.lastSeen,
    this.rawAdvertisementData,
  });

  /// Create from map received from platform channel
  factory QringDevice.fromMap(Map<String, dynamic> map) {
    return QringDevice(
      name: map['name'] as String? ?? '',
      macAddress: map['macAddress'] as String? ?? '',
      rssi: map['rssi'] as int? ?? 0,
      lastSeen: map['lastSeen'] as int?,
      rawAdvertisementData: map['rawAdvertisementData'] as String?,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    final map = {'name': name, 'macAddress': macAddress, 'rssi': rssi};
    if (lastSeen != null) {
      map['lastSeen'] = lastSeen!;
    }
    if (rawAdvertisementData != null) {
      map['rawAdvertisementData'] = rawAdvertisementData!;
    }
    return map;
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is QringDevice &&
          runtimeType == other.runtimeType &&
          name == other.name &&
          macAddress == other.macAddress;

  @override
  int get hashCode => name.hashCode ^ macAddress.hashCode;

  @override
  String toString() =>
      'QringDevice(name: $name, macAddress: $macAddress, rssi: $rssi, lastSeen: $lastSeen)';
}
