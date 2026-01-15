import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('BleError', () {
    test('fromMap creates BleError correctly', () {
      final map = {
        'errorCode': 'PERMISSION_DENIED',
        'errorMessage': 'Bluetooth permission not granted',
        'timestamp': 1234567890,
        'deviceMac': 'AA:BB:CC:DD:EE:FF',
      };

      final error = BleError.fromMap(map);

      expect(error.code, 'PERMISSION_DENIED');
      expect(error.message, 'Bluetooth permission not granted');
      expect(error.timestamp, 1234567890);
      expect(error.deviceMac, 'AA:BB:CC:DD:EE:FF');
      expect(error.type, BleErrorType.permissionDenied);
    });

    test('fromMap handles missing optional fields', () {
      final map = {
        'errorCode': 'BLUETOOTH_OFF',
        'errorMessage': 'Bluetooth is disabled',
      };

      final error = BleError.fromMap(map);

      expect(error.code, 'BLUETOOTH_OFF');
      expect(error.message, 'Bluetooth is disabled');
      expect(error.deviceMac, isNull);
      expect(error.type, BleErrorType.bluetoothOff);
      expect(error.timestamp, isNotNull);
    });

    test('toMap converts BleError to map', () {
      final error = BleError(
        code: 'PAIRING_FAILED',
        message: 'Failed to pair with device',
        timestamp: 1234567890,
        deviceMac: 'AA:BB:CC:DD:EE:FF',
        type: BleErrorType.pairingFailed,
      );

      final map = error.toMap();

      expect(map['errorCode'], 'PAIRING_FAILED');
      expect(map['errorMessage'], 'Failed to pair with device');
      expect(map['timestamp'], 1234567890);
      expect(map['deviceMac'], 'AA:BB:CC:DD:EE:FF');
      expect(map['errorType'], 'pairingFailed');
    });

    test('equality works correctly', () {
      final error1 = BleError(
        code: 'CONNECTION_FAILED',
        message: 'Connection failed',
        timestamp: 1234567890,
        type: BleErrorType.connectionFailed,
      );

      final error2 = BleError(
        code: 'CONNECTION_FAILED',
        message: 'Connection failed',
        timestamp: 1234567890,
        type: BleErrorType.connectionFailed,
      );

      final error3 = BleError(
        code: 'GATT_ERROR',
        message: 'GATT error',
        timestamp: 1234567890,
        type: BleErrorType.gattError,
      );

      expect(error1, equals(error2));
      expect(error1, isNot(equals(error3)));
    });
  });

  group('BleErrorType', () {
    test('fromCode maps error codes correctly', () {
      expect(
        BleErrorType.fromCode('PERMISSION_DENIED'),
        BleErrorType.permissionDenied,
      );
      expect(BleErrorType.fromCode('BLUETOOTH_OFF'), BleErrorType.bluetoothOff);
      expect(
        BleErrorType.fromCode('DEVICE_NOT_FOUND'),
        BleErrorType.deviceNotFound,
      );
      expect(
        BleErrorType.fromCode('PAIRING_FAILED'),
        BleErrorType.pairingFailed,
      );
      expect(
        BleErrorType.fromCode('CONNECTION_FAILED'),
        BleErrorType.connectionFailed,
      );
      expect(
        BleErrorType.fromCode('CONNECTION_TIMEOUT'),
        BleErrorType.connectionTimeout,
      );
      expect(BleErrorType.fromCode('GATT_ERROR'), BleErrorType.gattError);
      expect(
        BleErrorType.fromCode('COMMAND_FAILED'),
        BleErrorType.commandFailed,
      );
      expect(
        BleErrorType.fromCode('RECONNECTION_FAILED'),
        BleErrorType.reconnectionFailed,
      );
      expect(BleErrorType.fromCode('UNKNOWN'), BleErrorType.unknown);
    });

    test('fromCode handles unknown codes', () {
      expect(BleErrorType.fromCode('INVALID_CODE'), BleErrorType.unknown);
      expect(BleErrorType.fromCode(''), BleErrorType.unknown);
    });

    test('description provides user-friendly messages', () {
      expect(
        BleErrorType.permissionDenied.description,
        'Bluetooth permission not granted',
      );
      expect(BleErrorType.bluetoothOff.description, 'Bluetooth is disabled');
      expect(
        BleErrorType.pairingFailed.description,
        'Failed to pair with device',
      );
      expect(BleErrorType.connectionFailed.description, 'Connection failed');
      expect(BleErrorType.gattError.description, 'Communication error');
      expect(BleErrorType.unknown.description, 'Unknown error');
    });
  });
}
