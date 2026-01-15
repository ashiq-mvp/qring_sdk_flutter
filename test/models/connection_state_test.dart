import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('ConnectionState', () {
    test('fromString parses all states correctly', () {
      expect(
        ConnectionState.fromString('disconnected'),
        ConnectionState.disconnected,
      );
      expect(
        ConnectionState.fromString('connecting'),
        ConnectionState.connecting,
      );
      expect(
        ConnectionState.fromString('connected'),
        ConnectionState.connected,
      );
      expect(
        ConnectionState.fromString('disconnecting'),
        ConnectionState.disconnecting,
      );
    });

    test('fromString is case-insensitive', () {
      expect(
        ConnectionState.fromString('CONNECTED'),
        ConnectionState.connected,
      );
      expect(
        ConnectionState.fromString('Connecting'),
        ConnectionState.connecting,
      );
    });

    test('fromString handles invalid values', () {
      expect(
        ConnectionState.fromString('invalid'),
        ConnectionState.disconnected,
      );
      expect(ConnectionState.fromString(''), ConnectionState.disconnected);
    });

    test('toStringValue returns correct string', () {
      expect(ConnectionState.disconnected.toStringValue(), 'disconnected');
      expect(ConnectionState.connecting.toStringValue(), 'connecting');
      expect(ConnectionState.connected.toStringValue(), 'connected');
      expect(ConnectionState.disconnecting.toStringValue(), 'disconnecting');
    });

    test('round-trip conversion preserves state', () {
      for (final state in ConnectionState.values) {
        final str = state.toStringValue();
        final restored = ConnectionState.fromString(str);
        expect(restored, state);
      }
    });
  });
}
