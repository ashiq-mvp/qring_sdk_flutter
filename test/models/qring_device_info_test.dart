import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('QringDeviceInfo', () {
    test('fromMap creates valid instance', () {
      final map = {
        'firmwareVersion': '1.0.0',
        'hardwareVersion': '2.0',
        'supportsTemperature': true,
        'supportsBloodOxygen': true,
        'supportsBloodPressure': false,
        'supportsHrv': true,
        'supportsOneKeyCheck': false,
      };

      final info = QringDeviceInfo.fromMap(map);

      expect(info.firmwareVersion, '1.0.0');
      expect(info.hardwareVersion, '2.0');
      expect(info.supportsTemperature, true);
      expect(info.supportsBloodOxygen, true);
      expect(info.supportsBloodPressure, false);
      expect(info.supportsHrv, true);
      expect(info.supportsOneKeyCheck, false);
    });

    test('fromMap handles missing values with defaults', () {
      final map = <String, dynamic>{};

      final info = QringDeviceInfo.fromMap(map);

      expect(info.firmwareVersion, '');
      expect(info.hardwareVersion, '');
      expect(info.supportsTemperature, false);
      expect(info.supportsBloodOxygen, false);
      expect(info.supportsBloodPressure, false);
      expect(info.supportsHrv, false);
      expect(info.supportsOneKeyCheck, false);
    });

    test('toMap creates valid map', () {
      final info = QringDeviceInfo(
        firmwareVersion: '1.0.0',
        hardwareVersion: '2.0',
        supportsTemperature: true,
        supportsBloodOxygen: true,
        supportsBloodPressure: false,
        supportsHrv: true,
        supportsOneKeyCheck: false,
      );

      final map = info.toMap();

      expect(map['firmwareVersion'], '1.0.0');
      expect(map['hardwareVersion'], '2.0');
      expect(map['supportsTemperature'], true);
      expect(map['supportsBloodOxygen'], true);
      expect(map['supportsBloodPressure'], false);
      expect(map['supportsHrv'], true);
      expect(map['supportsOneKeyCheck'], false);
    });

    test('equality works correctly', () {
      final info1 = QringDeviceInfo(
        firmwareVersion: '1.0.0',
        hardwareVersion: '2.0',
        supportsTemperature: true,
        supportsBloodOxygen: true,
        supportsBloodPressure: false,
        supportsHrv: true,
        supportsOneKeyCheck: false,
      );
      final info2 = QringDeviceInfo(
        firmwareVersion: '1.0.0',
        hardwareVersion: '2.0',
        supportsTemperature: true,
        supportsBloodOxygen: true,
        supportsBloodPressure: false,
        supportsHrv: true,
        supportsOneKeyCheck: false,
      );
      final info3 = QringDeviceInfo(
        firmwareVersion: '2.0.0',
        hardwareVersion: '2.0',
        supportsTemperature: true,
        supportsBloodOxygen: true,
        supportsBloodPressure: false,
        supportsHrv: true,
        supportsOneKeyCheck: false,
      );

      expect(info1, equals(info2));
      expect(info1, isNot(equals(info3)));
    });

    test('round-trip serialization preserves data', () {
      final original = QringDeviceInfo(
        firmwareVersion: '1.0.0',
        hardwareVersion: '2.0',
        supportsTemperature: true,
        supportsBloodOxygen: true,
        supportsBloodPressure: false,
        supportsHrv: true,
        supportsOneKeyCheck: false,
      );

      final map = original.toMap();
      final restored = QringDeviceInfo.fromMap(map);

      expect(restored, equals(original));
    });
  });
}
