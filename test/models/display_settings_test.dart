import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  group('DisplaySettings', () {
    test('fromMap creates valid instance', () {
      final map = {
        'enabled': true,
        'leftHand': false,
        'brightness': 3,
        'maxBrightness': 5,
        'doNotDisturb': false,
        'screenOnStartMinutes': 480,
        'screenOnEndMinutes': 1320,
      };

      final settings = DisplaySettings.fromMap(map);

      expect(settings.enabled, true);
      expect(settings.leftHand, false);
      expect(settings.brightness, 3);
      expect(settings.maxBrightness, 5);
      expect(settings.doNotDisturb, false);
      expect(settings.screenOnStartMinutes, 480);
      expect(settings.screenOnEndMinutes, 1320);
    });

    test('fromMap handles missing values with defaults', () {
      final map = <String, dynamic>{};

      final settings = DisplaySettings.fromMap(map);

      expect(settings.enabled, true);
      expect(settings.leftHand, false);
      expect(settings.brightness, 1);
      expect(settings.maxBrightness, 5);
      expect(settings.doNotDisturb, false);
      expect(settings.screenOnStartMinutes, 0);
      expect(settings.screenOnEndMinutes, 1440);
    });

    test('toMap creates valid map', () {
      final settings = DisplaySettings(
        enabled: true,
        leftHand: false,
        brightness: 3,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 480,
        screenOnEndMinutes: 1320,
      );

      final map = settings.toMap();

      expect(map['enabled'], true);
      expect(map['leftHand'], false);
      expect(map['brightness'], 3);
      expect(map['maxBrightness'], 5);
      expect(map['doNotDisturb'], false);
      expect(map['screenOnStartMinutes'], 480);
      expect(map['screenOnEndMinutes'], 1320);
    });

    test('equality works correctly', () {
      final settings1 = DisplaySettings(
        enabled: true,
        leftHand: false,
        brightness: 3,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 480,
        screenOnEndMinutes: 1320,
      );
      final settings2 = DisplaySettings(
        enabled: true,
        leftHand: false,
        brightness: 3,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 480,
        screenOnEndMinutes: 1320,
      );
      final settings3 = DisplaySettings(
        enabled: true,
        leftHand: true,
        brightness: 3,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 480,
        screenOnEndMinutes: 1320,
      );

      expect(settings1, equals(settings2));
      expect(settings1, isNot(equals(settings3)));
    });

    test('round-trip serialization preserves data', () {
      final original = DisplaySettings(
        enabled: true,
        leftHand: false,
        brightness: 3,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 480,
        screenOnEndMinutes: 1320,
      );

      final map = original.toMap();
      final restored = DisplaySettings.fromMap(map);

      expect(restored, equals(original));
    });

    test('handles brightness edge cases', () {
      final minBrightness = DisplaySettings(
        enabled: true,
        leftHand: false,
        brightness: 1,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 0,
        screenOnEndMinutes: 1440,
      );

      expect(minBrightness.brightness, 1);

      final maxBrightness = DisplaySettings(
        enabled: true,
        leftHand: false,
        brightness: 5,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 0,
        screenOnEndMinutes: 1440,
      );

      expect(maxBrightness.brightness, 5);
    });
  });
}
