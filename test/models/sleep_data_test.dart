import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('SleepStage', () {
    test('fromInt creates correct stage', () {
      expect(SleepStage.fromInt(0), SleepStage.notSleeping);
      expect(SleepStage.fromInt(1), SleepStage.removed);
      expect(SleepStage.fromInt(2), SleepStage.lightSleep);
      expect(SleepStage.fromInt(3), SleepStage.deepSleep);
      expect(SleepStage.fromInt(4), SleepStage.rem);
      expect(SleepStage.fromInt(5), SleepStage.awake);
    });

    test('fromInt handles invalid values', () {
      expect(SleepStage.fromInt(99), SleepStage.notSleeping);
      expect(SleepStage.fromInt(-1), SleepStage.notSleeping);
    });

    test('toInt returns correct value', () {
      expect(SleepStage.notSleeping.toInt(), 0);
      expect(SleepStage.removed.toInt(), 1);
      expect(SleepStage.lightSleep.toInt(), 2);
      expect(SleepStage.deepSleep.toInt(), 3);
      expect(SleepStage.rem.toInt(), 4);
      expect(SleepStage.awake.toInt(), 5);
    });

    test('round-trip conversion preserves stage', () {
      for (final stage in SleepStage.values) {
        final intValue = stage.toInt();
        final restored = SleepStage.fromInt(intValue);
        expect(restored, stage);
      }
    });
  });

  group('SleepDetail', () {
    test('fromMap creates valid instance', () {
      final map = {'durationMinutes': 120, 'stage': 3};

      final detail = SleepDetail.fromMap(map);

      expect(detail.durationMinutes, 120);
      expect(detail.stage, SleepStage.deepSleep);
    });

    test('fromMap handles missing values with defaults', () {
      final map = <String, dynamic>{};

      final detail = SleepDetail.fromMap(map);

      expect(detail.durationMinutes, 0);
      expect(detail.stage, SleepStage.notSleeping);
    });

    test('toMap creates valid map', () {
      final detail = SleepDetail(
        durationMinutes: 120,
        stage: SleepStage.deepSleep,
      );

      final map = detail.toMap();

      expect(map['durationMinutes'], 120);
      expect(map['stage'], 3);
    });

    test('round-trip serialization preserves data', () {
      final original = SleepDetail(
        durationMinutes: 120,
        stage: SleepStage.deepSleep,
      );

      final map = original.toMap();
      final restored = SleepDetail.fromMap(map);

      expect(restored, equals(original));
    });
  });

  group('SleepData', () {
    test('fromMap creates valid instance', () {
      final startTime = DateTime.now().millisecondsSinceEpoch;
      final endTime = startTime + 28800000; // 8 hours later
      final map = {
        'startTime': startTime,
        'endTime': endTime,
        'details': [
          {'durationMinutes': 120, 'stage': 2},
          {'durationMinutes': 180, 'stage': 3},
          {'durationMinutes': 60, 'stage': 4},
        ],
        'hasLunchBreak': false,
      };

      final data = SleepData.fromMap(map);

      expect(data.startTime.millisecondsSinceEpoch, startTime);
      expect(data.endTime.millisecondsSinceEpoch, endTime);
      expect(data.details.length, 3);
      expect(data.details[0].durationMinutes, 120);
      expect(data.details[0].stage, SleepStage.lightSleep);
      expect(data.hasLunchBreak, false);
      expect(data.lunchStartTime, null);
      expect(data.lunchEndTime, null);
    });

    test('fromMap handles lunch break data', () {
      final startTime = DateTime.now().millisecondsSinceEpoch;
      final endTime = startTime + 28800000;
      final lunchStart = startTime + 14400000;
      final lunchEnd = lunchStart + 1800000;
      final map = {
        'startTime': startTime,
        'endTime': endTime,
        'details': [],
        'hasLunchBreak': true,
        'lunchStartTime': lunchStart,
        'lunchEndTime': lunchEnd,
      };

      final data = SleepData.fromMap(map);

      expect(data.hasLunchBreak, true);
      expect(data.lunchStartTime?.millisecondsSinceEpoch, lunchStart);
      expect(data.lunchEndTime?.millisecondsSinceEpoch, lunchEnd);
    });

    test('fromMap handles missing details', () {
      final startTime = DateTime.now().millisecondsSinceEpoch;
      final endTime = startTime + 28800000;
      final map = {'startTime': startTime, 'endTime': endTime};

      final data = SleepData.fromMap(map);

      expect(data.details, isEmpty);
      expect(data.hasLunchBreak, false);
    });

    test('toMap creates valid map', () {
      final startTime = DateTime.now();
      final endTime = startTime.add(const Duration(hours: 8));
      final data = SleepData(
        startTime: startTime,
        endTime: endTime,
        details: [
          SleepDetail(durationMinutes: 120, stage: SleepStage.lightSleep),
          SleepDetail(durationMinutes: 180, stage: SleepStage.deepSleep),
        ],
        hasLunchBreak: false,
      );

      final map = data.toMap();

      expect(map['startTime'], startTime.millisecondsSinceEpoch);
      expect(map['endTime'], endTime.millisecondsSinceEpoch);
      expect(map['details'], isA<List>());
      expect((map['details'] as List).length, 2);
      expect(map['hasLunchBreak'], false);
      expect(map['lunchStartTime'], null);
      expect(map['lunchEndTime'], null);
    });

    test('equality works correctly', () {
      final startTime = DateTime.now();
      final endTime = startTime.add(const Duration(hours: 8));
      final details = [
        SleepDetail(durationMinutes: 120, stage: SleepStage.lightSleep),
      ];

      final data1 = SleepData(
        startTime: startTime,
        endTime: endTime,
        details: details,
      );
      final data2 = SleepData(
        startTime: startTime,
        endTime: endTime,
        details: details,
      );
      final data3 = SleepData(
        startTime: startTime,
        endTime: endTime,
        details: [],
      );

      expect(data1, equals(data2));
      expect(data1, isNot(equals(data3)));
    });

    test('round-trip serialization preserves data', () {
      final original = SleepData(
        startTime: DateTime.now(),
        endTime: DateTime.now().add(const Duration(hours: 8)),
        details: [
          SleepDetail(durationMinutes: 120, stage: SleepStage.lightSleep),
          SleepDetail(durationMinutes: 180, stage: SleepStage.deepSleep),
        ],
        hasLunchBreak: true,
        lunchStartTime: DateTime.now().add(const Duration(hours: 4)),
        lunchEndTime: DateTime.now().add(const Duration(hours: 4, minutes: 30)),
      );

      final map = original.toMap();
      final restored = SleepData.fromMap(map);

      expect(
        restored.startTime.millisecondsSinceEpoch,
        original.startTime.millisecondsSinceEpoch,
      );
      expect(
        restored.endTime.millisecondsSinceEpoch,
        original.endTime.millisecondsSinceEpoch,
      );
      expect(restored.details, original.details);
      expect(restored.hasLunchBreak, original.hasLunchBreak);
      expect(
        restored.lunchStartTime?.millisecondsSinceEpoch,
        original.lunchStartTime?.millisecondsSinceEpoch,
      );
      expect(
        restored.lunchEndTime?.millisecondsSinceEpoch,
        original.lunchEndTime?.millisecondsSinceEpoch,
      );
    });
  });
}
