import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('HeartRateData', () {
    test('fromMap creates valid instance', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {'timestamp': timestamp, 'heartRate': 75};

      final data = HeartRateData.fromMap(map);

      expect(data.timestamp.millisecondsSinceEpoch, timestamp);
      expect(data.heartRate, 75);
    });

    test('fromMap handles missing heartRate with default', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {'timestamp': timestamp};

      final data = HeartRateData.fromMap(map);

      expect(data.timestamp.millisecondsSinceEpoch, timestamp);
      expect(data.heartRate, 0);
    });

    test('toMap creates valid map', () {
      final timestamp = DateTime.now();
      final data = HeartRateData(timestamp: timestamp, heartRate: 75);

      final map = data.toMap();

      expect(map['timestamp'], timestamp.millisecondsSinceEpoch);
      expect(map['heartRate'], 75);
    });

    test('equality works correctly', () {
      final timestamp = DateTime.now();
      final data1 = HeartRateData(timestamp: timestamp, heartRate: 75);
      final data2 = HeartRateData(timestamp: timestamp, heartRate: 75);
      final data3 = HeartRateData(timestamp: timestamp, heartRate: 80);

      expect(data1, equals(data2));
      expect(data1, isNot(equals(data3)));
    });

    test('round-trip serialization preserves data', () {
      final original = HeartRateData(timestamp: DateTime.now(), heartRate: 75);

      final map = original.toMap();
      final restored = HeartRateData.fromMap(map);

      expect(
        restored.timestamp.millisecondsSinceEpoch,
        original.timestamp.millisecondsSinceEpoch,
      );
      expect(restored.heartRate, original.heartRate);
    });
  });
}
