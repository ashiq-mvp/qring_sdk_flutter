import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('StepData', () {
    test('fromMap creates valid instance', () {
      final map = {
        'date': '2024-01-15T00:00:00.000',
        'totalSteps': 10000,
        'runningSteps': 2000,
        'calories': 450,
        'distanceMeters': 7500,
        'sportDurationSeconds': 3600,
        'sleepDurationSeconds': 28800,
      };

      final data = StepData.fromMap(map);

      expect(data.date, DateTime.parse('2024-01-15T00:00:00.000'));
      expect(data.totalSteps, 10000);
      expect(data.runningSteps, 2000);
      expect(data.calories, 450);
      expect(data.distanceMeters, 7500);
      expect(data.sportDurationSeconds, 3600);
      expect(data.sleepDurationSeconds, 28800);
    });

    test('fromMap handles missing values with defaults', () {
      final map = {'date': '2024-01-15T00:00:00.000'};

      final data = StepData.fromMap(map);

      expect(data.date, DateTime.parse('2024-01-15T00:00:00.000'));
      expect(data.totalSteps, 0);
      expect(data.runningSteps, 0);
      expect(data.calories, 0);
      expect(data.distanceMeters, 0);
      expect(data.sportDurationSeconds, 0);
      expect(data.sleepDurationSeconds, 0);
    });

    test('toMap creates valid map', () {
      final date = DateTime.parse('2024-01-15T00:00:00.000');
      final data = StepData(
        date: date,
        totalSteps: 10000,
        runningSteps: 2000,
        calories: 450,
        distanceMeters: 7500,
        sportDurationSeconds: 3600,
        sleepDurationSeconds: 28800,
      );

      final map = data.toMap();

      expect(map['date'], date.toIso8601String());
      expect(map['totalSteps'], 10000);
      expect(map['runningSteps'], 2000);
      expect(map['calories'], 450);
      expect(map['distanceMeters'], 7500);
      expect(map['sportDurationSeconds'], 3600);
      expect(map['sleepDurationSeconds'], 28800);
    });

    test('equality works correctly', () {
      final date = DateTime.parse('2024-01-15T00:00:00.000');
      final data1 = StepData(
        date: date,
        totalSteps: 10000,
        runningSteps: 2000,
        calories: 450,
        distanceMeters: 7500,
        sportDurationSeconds: 3600,
        sleepDurationSeconds: 28800,
      );
      final data2 = StepData(
        date: date,
        totalSteps: 10000,
        runningSteps: 2000,
        calories: 450,
        distanceMeters: 7500,
        sportDurationSeconds: 3600,
        sleepDurationSeconds: 28800,
      );
      final data3 = StepData(
        date: date,
        totalSteps: 5000,
        runningSteps: 2000,
        calories: 450,
        distanceMeters: 7500,
        sportDurationSeconds: 3600,
        sleepDurationSeconds: 28800,
      );

      expect(data1, equals(data2));
      expect(data1, isNot(equals(data3)));
    });

    test('round-trip serialization preserves data', () {
      final original = StepData(
        date: DateTime.parse('2024-01-15T00:00:00.000'),
        totalSteps: 10000,
        runningSteps: 2000,
        calories: 450,
        distanceMeters: 7500,
        sportDurationSeconds: 3600,
        sleepDurationSeconds: 28800,
      );

      final map = original.toMap();
      final restored = StepData.fromMap(map);

      expect(restored, equals(original));
    });
  });
}
