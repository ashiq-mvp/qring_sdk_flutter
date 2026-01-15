/// Represents the current connection status.
///
/// This enum represents the various states of the BLE connection lifecycle,
/// including pairing and automatic reconnection states introduced by the
/// production-grade BLE Connection Manager.
enum ConnectionState {
  /// Device is disconnected
  disconnected,

  /// Device is connecting
  connecting,

  /// Device is pairing/bonding (establishing secure connection)
  pairing,

  /// Device is connected
  connected,

  /// Device is disconnecting
  disconnecting,

  /// Device is attempting automatic reconnection
  reconnecting;

  /// Create from string received from platform channel
  static ConnectionState fromString(String value) {
    switch (value.toLowerCase()) {
      case 'disconnected':
        return ConnectionState.disconnected;
      case 'connecting':
        return ConnectionState.connecting;
      case 'pairing':
        return ConnectionState.pairing;
      case 'connected':
        return ConnectionState.connected;
      case 'disconnecting':
        return ConnectionState.disconnecting;
      case 'reconnecting':
        return ConnectionState.reconnecting;
      default:
        return ConnectionState.disconnected;
    }
  }

  /// Convert to string for platform channel
  String toStringValue() {
    return name;
  }
}
