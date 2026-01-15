import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('BloodOxygenData', () {
    test('fromMap creates valid instance', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {'timestamp': timestamp, 'spO2': 98};

      final data = BloodOxygenData.fromMap(map);

      expect(data.timestamp.millisecondsSinceEpoch, timestamp);
      expect(data.spO2, 98);
    });

    test('fromMap handles missing spO2 with default', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {'timestamp': timestamp};

      final data = BloodOxygenData.fromMap(map);

      expect(data.timestamp.millisecondsSinceEpoch, timestamp);
      expect(data.spO2, 0);
    });

    test('toMap creates valid map', () {
      final timestamp = DateTime.now();
      final data = BloodOxygenData(timestamp: timestamp, spO2: 98);

      final map = data.toMap();

      expect(map['timestamp'], timestamp.millisecondsSinceEpoch);
      expect(map['spO2'], 98);
    });

    test('equality works correctly', () {
      final timestamp = DateTime.now();
      final data1 = BloodOxygenData(timestamp: timestamp, spO2: 98);
      final data2 = BloodOxygenData(timestamp: timestamp, spO2: 98);
      final data3 = BloodOxygenData(timestamp: timestamp, spO2: 95);

      expect(data1, equals(data2));
      expect(data1, isNot(equals(data3)));
    });

    test('round-trip serialization preserves data', () {
      final original = BloodOxygenData(timestamp: DateTime.now(), spO2: 98);

      final map = original.toMap();
      final restored = BloodOxygenData.fromMap(map);

      expect(
        restored.timestamp.millisecondsSinceEpoch,
        original.timestamp.millisecondsSinceEpoch,
      );
      expect(restored.spO2, original.spO2);
    });
  });
}
