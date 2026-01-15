import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('BloodPressureData', () {
    test('fromMap creates valid instance', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {'timestamp': timestamp, 'systolic': 120, 'diastolic': 80};

      final data = BloodPressureData.fromMap(map);

      expect(data.timestamp.millisecondsSinceEpoch, timestamp);
      expect(data.systolic, 120);
      expect(data.diastolic, 80);
    });

    test('fromMap handles missing values with defaults', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {'timestamp': timestamp};

      final data = BloodPressureData.fromMap(map);

      expect(data.timestamp.millisecondsSinceEpoch, timestamp);
      expect(data.systolic, 0);
      expect(data.diastolic, 0);
    });

    test('toMap creates valid map', () {
      final timestamp = DateTime.now();
      final data = BloodPressureData(
        timestamp: timestamp,
        systolic: 120,
        diastolic: 80,
      );

      final map = data.toMap();

      expect(map['timestamp'], timestamp.millisecondsSinceEpoch);
      expect(map['systolic'], 120);
      expect(map['diastolic'], 80);
    });

    test('equality works correctly', () {
      final timestamp = DateTime.now();
      final data1 = BloodPressureData(
        timestamp: timestamp,
        systolic: 120,
        diastolic: 80,
      );
      final data2 = BloodPressureData(
        timestamp: timestamp,
        systolic: 120,
        diastolic: 80,
      );
      final data3 = BloodPressureData(
        timestamp: timestamp,
        systolic: 130,
        diastolic: 85,
      );

      expect(data1, equals(data2));
      expect(data1, isNot(equals(data3)));
    });

    test('round-trip serialization preserves data', () {
      final original = BloodPressureData(
        timestamp: DateTime.now(),
        systolic: 120,
        diastolic: 80,
      );

      final map = original.toMap();
      final restored = BloodPressureData.fromMap(map);

      expect(
        restored.timestamp.millisecondsSinceEpoch,
        original.timestamp.millisecondsSinceEpoch,
      );
      expect(restored.systolic, original.systolic);
      expect(restored.diastolic, original.diastolic);
    });
  });
}
