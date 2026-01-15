import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('QringDevice', () {
    test('fromMap creates valid instance', () {
      final map = {
        'name': 'QC Ring',
        'macAddress': '00:11:22:33:44:55',
        'rssi': -65,
      };

      final device = QringDevice.fromMap(map);

      expect(device.name, 'QC Ring');
      expect(device.macAddress, '00:11:22:33:44:55');
      expect(device.rssi, -65);
    });

    test('fromMap handles missing values with defaults', () {
      final map = <String, dynamic>{};

      final device = QringDevice.fromMap(map);

      expect(device.name, '');
      expect(device.macAddress, '');
      expect(device.rssi, 0);
    });

    test('toMap creates valid map', () {
      final device = QringDevice(
        name: 'QC Ring',
        macAddress: '00:11:22:33:44:55',
        rssi: -65,
      );

      final map = device.toMap();

      expect(map['name'], 'QC Ring');
      expect(map['macAddress'], '00:11:22:33:44:55');
      expect(map['rssi'], -65);
    });

    test('equality works correctly', () {
      final device1 = QringDevice(
        name: 'QC Ring',
        macAddress: '00:11:22:33:44:55',
        rssi: -65,
      );
      final device2 = QringDevice(
        name: 'QC Ring',
        macAddress: '00:11:22:33:44:55',
        rssi: -65,
      );
      final device3 = QringDevice(
        name: 'Other Ring',
        macAddress: '00:11:22:33:44:55',
        rssi: -65,
      );

      expect(device1, equals(device2));
      expect(device1, isNot(equals(device3)));
    });

    test('round-trip serialization preserves data', () {
      final original = QringDevice(
        name: 'QC Ring',
        macAddress: '00:11:22:33:44:55',
        rssi: -65,
      );

      final map = original.toMap();
      final restored = QringDevice.fromMap(map);

      expect(restored, equals(original));
    });
  });
}
