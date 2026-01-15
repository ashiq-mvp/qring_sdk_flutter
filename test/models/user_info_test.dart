import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('UserInfo', () {
    test('fromMap creates valid instance', () {
      final map = {'age': 30, 'heightCm': 175, 'weightKg': 70, 'isMale': true};

      final info = UserInfo.fromMap(map);

      expect(info.age, 30);
      expect(info.heightCm, 175);
      expect(info.weightKg, 70);
      expect(info.isMale, true);
    });

    test('fromMap handles missing values with defaults', () {
      final map = <String, dynamic>{};

      final info = UserInfo.fromMap(map);

      expect(info.age, 0);
      expect(info.heightCm, 0);
      expect(info.weightKg, 0);
      expect(info.isMale, true);
    });

    test('toMap creates valid map', () {
      final info = UserInfo(age: 30, heightCm: 175, weightKg: 70, isMale: true);

      final map = info.toMap();

      expect(map['age'], 30);
      expect(map['heightCm'], 175);
      expect(map['weightKg'], 70);
      expect(map['isMale'], true);
    });

    test('equality works correctly', () {
      final info1 = UserInfo(
        age: 30,
        heightCm: 175,
        weightKg: 70,
        isMale: true,
      );
      final info2 = UserInfo(
        age: 30,
        heightCm: 175,
        weightKg: 70,
        isMale: true,
      );
      final info3 = UserInfo(
        age: 25,
        heightCm: 175,
        weightKg: 70,
        isMale: true,
      );

      expect(info1, equals(info2));
      expect(info1, isNot(equals(info3)));
    });

    test('round-trip serialization preserves data', () {
      final original = UserInfo(
        age: 30,
        heightCm: 175,
        weightKg: 70,
        isMale: true,
      );

      final map = original.toMap();
      final restored = UserInfo.fromMap(map);

      expect(restored, equals(original));
    });

    test('handles female user', () {
      final info = UserInfo(
        age: 28,
        heightCm: 165,
        weightKg: 60,
        isMale: false,
      );

      expect(info.isMale, false);

      final map = info.toMap();
      final restored = UserInfo.fromMap(map);

      expect(restored.isMale, false);
    });
  });
}
