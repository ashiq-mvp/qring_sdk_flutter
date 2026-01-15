import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('MeasurementType', () {
    test('fromString parses all types correctly', () {
      expect(
        MeasurementType.fromString('heartRate'),
        MeasurementType.heartRate,
      );
      expect(
        MeasurementType.fromString('heart_rate'),
        MeasurementType.heartRate,
      );
      expect(
        MeasurementType.fromString('bloodPressure'),
        MeasurementType.bloodPressure,
      );
      expect(
        MeasurementType.fromString('blood_pressure'),
        MeasurementType.bloodPressure,
      );
      expect(
        MeasurementType.fromString('bloodOxygen'),
        MeasurementType.bloodOxygen,
      );
      expect(
        MeasurementType.fromString('temperature'),
        MeasurementType.temperature,
      );
      expect(MeasurementType.fromString('pressure'), MeasurementType.pressure);
      expect(MeasurementType.fromString('hrv'), MeasurementType.hrv);
    });

    test('fromString is case-insensitive', () {
      expect(
        MeasurementType.fromString('HEARTRATE'),
        MeasurementType.heartRate,
      );
      expect(
        MeasurementType.fromString('Temperature'),
        MeasurementType.temperature,
      );
    });

    test('fromString handles invalid values', () {
      expect(MeasurementType.fromString('invalid'), MeasurementType.heartRate);
    });

    test('toStringValue returns correct string', () {
      expect(MeasurementType.heartRate.toStringValue(), 'heartRate');
      expect(MeasurementType.bloodPressure.toStringValue(), 'bloodPressure');
      expect(MeasurementType.bloodOxygen.toStringValue(), 'bloodOxygen');
      expect(MeasurementType.temperature.toStringValue(), 'temperature');
      expect(MeasurementType.pressure.toStringValue(), 'pressure');
      expect(MeasurementType.hrv.toStringValue(), 'hrv');
    });
  });

  group('HealthMeasurement', () {
    test('fromMap creates valid heart rate measurement', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {
        'type': 'heartRate',
        'timestamp': timestamp,
        'heartRate': 75,
        'success': true,
      };

      final measurement = HealthMeasurement.fromMap(map);

      expect(measurement.type, MeasurementType.heartRate);
      expect(measurement.timestamp.millisecondsSinceEpoch, timestamp);
      expect(measurement.heartRate, 75);
      expect(measurement.success, true);
      expect(measurement.errorMessage, null);
    });

    test('fromMap creates valid blood pressure measurement', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {
        'type': 'bloodPressure',
        'timestamp': timestamp,
        'systolic': 120,
        'diastolic': 80,
        'success': true,
      };

      final measurement = HealthMeasurement.fromMap(map);

      expect(measurement.type, MeasurementType.bloodPressure);
      expect(measurement.systolic, 120);
      expect(measurement.diastolic, 80);
      expect(measurement.success, true);
    });

    test('fromMap creates valid blood oxygen measurement', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {
        'type': 'bloodOxygen',
        'timestamp': timestamp,
        'spO2': 98,
        'success': true,
      };

      final measurement = HealthMeasurement.fromMap(map);

      expect(measurement.type, MeasurementType.bloodOxygen);
      expect(measurement.spO2, 98);
      expect(measurement.success, true);
    });

    test('fromMap creates valid temperature measurement', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {
        'type': 'temperature',
        'timestamp': timestamp,
        'temperature': 36.5,
        'success': true,
      };

      final measurement = HealthMeasurement.fromMap(map);

      expect(measurement.type, MeasurementType.temperature);
      expect(measurement.temperature, 36.5);
      expect(measurement.success, true);
    });

    test('fromMap handles failed measurement', () {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final map = {
        'type': 'heartRate',
        'timestamp': timestamp,
        'success': false,
        'errorMessage': 'Measurement failed',
      };

      final measurement = HealthMeasurement.fromMap(map);

      expect(measurement.success, false);
      expect(measurement.errorMessage, 'Measurement failed');
    });

    test('fromMap handles missing timestamp', () {
      final map = {'type': 'heartRate', 'heartRate': 75, 'success': true};

      final measurement = HealthMeasurement.fromMap(map);

      expect(measurement.timestamp, isNotNull);
      expect(measurement.type, MeasurementType.heartRate);
    });

    test('toMap creates valid map', () {
      final timestamp = DateTime.now();
      final measurement = HealthMeasurement(
        type: MeasurementType.heartRate,
        timestamp: timestamp,
        heartRate: 75,
        success: true,
      );

      final map = measurement.toMap();

      expect(map['type'], 'heartRate');
      expect(map['timestamp'], timestamp.millisecondsSinceEpoch);
      expect(map['heartRate'], 75);
      expect(map['success'], true);
      expect(map['errorMessage'], null);
    });

    test('toMap includes all measurement types', () {
      final timestamp = DateTime.now();
      final measurement = HealthMeasurement(
        type: MeasurementType.bloodPressure,
        timestamp: timestamp,
        heartRate: 75,
        systolic: 120,
        diastolic: 80,
        spO2: 98,
        temperature: 36.5,
        pressure: 100,
        hrv: 50,
        success: true,
      );

      final map = measurement.toMap();

      expect(map['heartRate'], 75);
      expect(map['systolic'], 120);
      expect(map['diastolic'], 80);
      expect(map['spO2'], 98);
      expect(map['temperature'], 36.5);
      expect(map['pressure'], 100);
      expect(map['hrv'], 50);
    });

    test('equality works correctly', () {
      final timestamp = DateTime.now();
      final measurement1 = HealthMeasurement(
        type: MeasurementType.heartRate,
        timestamp: timestamp,
        heartRate: 75,
        success: true,
      );
      final measurement2 = HealthMeasurement(
        type: MeasurementType.heartRate,
        timestamp: timestamp,
        heartRate: 75,
        success: true,
      );
      final measurement3 = HealthMeasurement(
        type: MeasurementType.heartRate,
        timestamp: timestamp,
        heartRate: 80,
        success: true,
      );

      expect(measurement1, equals(measurement2));
      expect(measurement1, isNot(equals(measurement3)));
    });

    test('round-trip serialization preserves data', () {
      final original = HealthMeasurement(
        type: MeasurementType.bloodPressure,
        timestamp: DateTime.now(),
        systolic: 120,
        diastolic: 80,
        success: true,
      );

      final map = original.toMap();
      final restored = HealthMeasurement.fromMap(map);

      expect(restored.type, original.type);
      expect(
        restored.timestamp.millisecondsSinceEpoch,
        original.timestamp.millisecondsSinceEpoch,
      );
      expect(restored.systolic, original.systolic);
      expect(restored.diastolic, original.diastolic);
      expect(restored.success, original.success);
    });
  });
}
