import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('Data Conversion Tests', () {
    group('StepData Conversion', () {
      test('should convert valid step data from native response', () {
        final map = {
          'date': '2024-01-15',
          'totalSteps': 10000,
          'runningSteps': 2000,
          'calories': 450,
          'distanceMeters': 7500,
          'sportDurationSeconds': 3600,
          'sleepDurationSeconds': 28800,
        };

        final stepData = StepData.fromMap(map);

        expect(stepData.date.year, equals(2024));
        expect(stepData.date.month, equals(1));
        expect(stepData.date.day, equals(15));
        expect(stepData.totalSteps, equals(10000));
        expect(stepData.runningSteps, equals(2000));
        expect(stepData.calories, equals(450));
        expect(stepData.distanceMeters, equals(7500));
        expect(stepData.sportDurationSeconds, equals(3600));
        expect(stepData.sleepDurationSeconds, equals(28800));
      });

      test('should handle zero values in step data', () {
        final map = {
          'date': '2024-01-15',
          'totalSteps': 0,
          'runningSteps': 0,
          'calories': 0,
          'distanceMeters': 0,
          'sportDurationSeconds': 0,
          'sleepDurationSeconds': 0,
        };

        final stepData = StepData.fromMap(map);

        expect(stepData.totalSteps, equals(0));
        expect(stepData.runningSteps, equals(0));
        expect(stepData.calories, equals(0));
        expect(stepData.distanceMeters, equals(0));
        expect(stepData.sportDurationSeconds, equals(0));
        expect(stepData.sleepDurationSeconds, equals(0));
      });

      test('should handle large values in step data', () {
        final map = {
          'date': '2024-01-15',
          'totalSteps': 50000,
          'runningSteps': 10000,
          'calories': 2500,
          'distanceMeters': 40000,
          'sportDurationSeconds': 14400,
          'sleepDurationSeconds': 32400,
        };

        final stepData = StepData.fromMap(map);

        expect(stepData.totalSteps, equals(50000));
        expect(stepData.runningSteps, equals(10000));
        expect(stepData.calories, equals(2500));
        expect(stepData.distanceMeters, equals(40000));
      });
    });

    group('HeartRateData Array Conversion', () {
      test('should convert valid heart rate array', () {
        final list = [
          {
            'timestamp': DateTime(2024, 1, 15, 10, 0).millisecondsSinceEpoch,
            'heartRate': 72,
          },
          {
            'timestamp': DateTime(2024, 1, 15, 10, 5).millisecondsSinceEpoch,
            'heartRate': 75,
          },
          {
            'timestamp': DateTime(2024, 1, 15, 10, 10).millisecondsSinceEpoch,
            'heartRate': 78,
          },
        ];

        final hrDataList = list
            .map((map) => HeartRateData.fromMap(map))
            .toList();

        expect(hrDataList.length, equals(3));
        expect(hrDataList[0].heartRate, equals(72));
        expect(hrDataList[1].heartRate, equals(75));
        expect(hrDataList[2].heartRate, equals(78));
        expect(hrDataList[0].timestamp.hour, equals(10));
        expect(hrDataList[0].timestamp.minute, equals(0));
      });

      test('should handle empty heart rate array', () {
        final list = <Map<String, dynamic>>[];

        final hrDataList = list
            .map((map) => HeartRateData.fromMap(map))
            .toList();

        expect(hrDataList, isEmpty);
      });

      test('should handle heart rate values at boundaries', () {
        final list = [
          {
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'heartRate': 40, // Low heart rate
          },
          {
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'heartRate': 200, // High heart rate
          },
        ];

        final hrDataList = list
            .map((map) => HeartRateData.fromMap(map))
            .toList();

        expect(hrDataList[0].heartRate, equals(40));
        expect(hrDataList[1].heartRate, equals(200));
      });
    });

    group('SleepData Conversion', () {
      test('should convert valid sleep data with sleep stages', () {
        final map = {
          'startTime': DateTime(2024, 1, 15, 22, 0).millisecondsSinceEpoch,
          'endTime': DateTime(2024, 1, 16, 6, 0).millisecondsSinceEpoch,
          'details': [
            {'durationMinutes': 120, 'stage': 'lightSleep'},
            {'durationMinutes': 180, 'stage': 'deepSleep'},
            {'durationMinutes': 90, 'stage': 'rem'},
            {'durationMinutes': 30, 'stage': 'awake'},
          ],
          'hasLunchBreak': false,
        };

        final sleepData = SleepData.fromMap(map);

        expect(sleepData.startTime.hour, equals(22));
        expect(sleepData.endTime.hour, equals(6));
        expect(sleepData.details.length, equals(4));
        expect(sleepData.details[0].durationMinutes, equals(120));
        expect(sleepData.details[0].stage, equals(SleepStage.lightSleep));
        expect(sleepData.details[1].durationMinutes, equals(180));
        expect(sleepData.details[1].stage, equals(SleepStage.deepSleep));
        expect(sleepData.details[2].stage, equals(SleepStage.rem));
        expect(sleepData.details[3].stage, equals(SleepStage.awake));
        expect(sleepData.hasLunchBreak, isFalse);
      });

      test('should handle sleep data with lunch break', () {
        final map = {
          'startTime': DateTime(2024, 1, 15, 22, 0).millisecondsSinceEpoch,
          'endTime': DateTime(2024, 1, 16, 6, 0).millisecondsSinceEpoch,
          'details': [
            {'durationMinutes': 120, 'stage': 'lightSleep'},
          ],
          'hasLunchBreak': true,
          'lunchStartTime': DateTime(2024, 1, 15, 12, 0).millisecondsSinceEpoch,
          'lunchEndTime': DateTime(2024, 1, 15, 13, 0).millisecondsSinceEpoch,
        };

        final sleepData = SleepData.fromMap(map);

        expect(sleepData.hasLunchBreak, isTrue);
        expect(sleepData.lunchStartTime, isNotNull);
        expect(sleepData.lunchEndTime, isNotNull);
        expect(sleepData.lunchStartTime!.hour, equals(12));
        expect(sleepData.lunchEndTime!.hour, equals(13));
      });

      test('should handle empty sleep details', () {
        final map = {
          'startTime': DateTime(2024, 1, 15, 22, 0).millisecondsSinceEpoch,
          'endTime': DateTime(2024, 1, 16, 6, 0).millisecondsSinceEpoch,
          'details': <Map<String, dynamic>>[],
          'hasLunchBreak': false,
        };

        final sleepData = SleepData.fromMap(map);

        expect(sleepData.details, isEmpty);
        expect(sleepData.hasLunchBreak, isFalse);
      });

      test('should handle all sleep stage types', () {
        final stages = [
          'notSleeping',
          'removed',
          'lightSleep',
          'deepSleep',
          'rem',
          'awake',
        ];

        for (final stageName in stages) {
          final map = {
            'startTime': DateTime.now().millisecondsSinceEpoch,
            'endTime': DateTime.now().millisecondsSinceEpoch,
            'details': [
              {'durationMinutes': 60, 'stage': stageName},
            ],
            'hasLunchBreak': false,
          };

          final sleepData = SleepData.fromMap(map);
          expect(sleepData.details.length, equals(1));
          expect(sleepData.details[0].stage.name, equals(stageName));
        }
      });
    });

    group('BloodOxygenData Conversion', () {
      test('should convert valid blood oxygen data', () {
        final list = [
          {
            'timestamp': DateTime(2024, 1, 15, 10, 0).millisecondsSinceEpoch,
            'spO2': 98,
          },
          {
            'timestamp': DateTime(2024, 1, 15, 10, 5).millisecondsSinceEpoch,
            'spO2': 97,
          },
        ];

        final spo2DataList = list
            .map((map) => BloodOxygenData.fromMap(map))
            .toList();

        expect(spo2DataList.length, equals(2));
        expect(spo2DataList[0].spO2, equals(98));
        expect(spo2DataList[1].spO2, equals(97));
      });

      test('should handle SpO2 values at boundaries', () {
        final list = [
          {
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'spO2': 90, // Lower boundary
          },
          {
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'spO2': 100, // Upper boundary
          },
        ];

        final spo2DataList = list
            .map((map) => BloodOxygenData.fromMap(map))
            .toList();

        expect(spo2DataList[0].spO2, equals(90));
        expect(spo2DataList[1].spO2, equals(100));
      });
    });

    group('BloodPressureData Conversion', () {
      test('should convert valid blood pressure data', () {
        final list = [
          {
            'timestamp': DateTime(2024, 1, 15, 10, 0).millisecondsSinceEpoch,
            'systolic': 120,
            'diastolic': 80,
          },
          {
            'timestamp': DateTime(2024, 1, 15, 10, 5).millisecondsSinceEpoch,
            'systolic': 125,
            'diastolic': 82,
          },
        ];

        final bpDataList = list
            .map((map) => BloodPressureData.fromMap(map))
            .toList();

        expect(bpDataList.length, equals(2));
        expect(bpDataList[0].systolic, equals(120));
        expect(bpDataList[0].diastolic, equals(80));
        expect(bpDataList[1].systolic, equals(125));
        expect(bpDataList[1].diastolic, equals(82));
      });

      test('should handle blood pressure values at boundaries', () {
        final list = [
          {
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'systolic': 90,
            'diastolic': 60,
          },
          {
            'timestamp': DateTime.now().millisecondsSinceEpoch,
            'systolic': 180,
            'diastolic': 120,
          },
        ];

        final bpDataList = list
            .map((map) => BloodPressureData.fromMap(map))
            .toList();

        expect(bpDataList[0].systolic, equals(90));
        expect(bpDataList[0].diastolic, equals(60));
        expect(bpDataList[1].systolic, equals(180));
        expect(bpDataList[1].diastolic, equals(120));
      });
    });

    group('Edge Cases', () {
      test('should handle invalid timestamps gracefully', () {
        // Test with timestamp of 0 (epoch)
        final map = {'timestamp': 0, 'heartRate': 72};

        final hrData = HeartRateData.fromMap(map);
        expect(hrData.timestamp.year, equals(1970));
      });

      test('should handle very large timestamps', () {
        // Test with far future timestamp
        final futureTimestamp = DateTime(2100, 1, 1).millisecondsSinceEpoch;
        final map = {'timestamp': futureTimestamp, 'heartRate': 72};

        final hrData = HeartRateData.fromMap(map);
        expect(hrData.timestamp.year, equals(2100));
      });

      test('should handle date parsing edge cases', () {
        final dates = [
          '2024-01-01', // Start of year
          '2024-12-31', // End of year
          '2024-02-29', // Leap year
        ];

        for (final dateStr in dates) {
          final map = {
            'date': dateStr,
            'totalSteps': 1000,
            'runningSteps': 200,
            'calories': 50,
            'distanceMeters': 800,
            'sportDurationSeconds': 600,
            'sleepDurationSeconds': 28800,
          };

          final stepData = StepData.fromMap(map);
          expect(stepData.date, isA<DateTime>());
        }
      });
    });
  });
}
