import 'dart:math';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const MethodChannel channel = MethodChannel('qring_sdk_flutter');
  final List<MethodCall> methodCallLog = [];
  bool isConnected = true;

  setUp(() {
    methodCallLog.clear();
    isConnected = true;

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
          methodCallLog.add(methodCall);

          switch (methodCall.method) {
            case 'setDisplaySettings':
              if (!isConnected) {
                throw PlatformException(
                  code: 'NOT_CONNECTED',
                  message: 'Device is not connected',
                );
              }
              final args = methodCall.arguments as Map;
              final brightness = args['brightness'] as int;
              final maxBrightness = args['maxBrightness'] as int;

              if (brightness < 1 || brightness > maxBrightness) {
                throw PlatformException(
                  code: 'INVALID_BRIGHTNESS',
                  message: 'Brightness must be between 1 and $maxBrightness',
                );
              }
              return null;

            case 'getDisplaySettings':
              if (!isConnected) {
                throw PlatformException(
                  code: 'NOT_CONNECTED',
                  message: 'Device is not connected',
                );
              }
              // Return mock display settings
              return {
                'enabled': true,
                'leftHand': false,
                'brightness': 3,
                'maxBrightness': 5,
                'doNotDisturb': false,
                'screenOnStartMinutes': 480, // 8:00 AM
                'screenOnEndMinutes': 1320, // 10:00 PM
              };

            default:
              return null;
          }
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  group('Display Settings Properties', () {
    // Property 29: Display Settings Configuration
    // Feature: qc-wireless-sdk-integration, Property 29: Display Settings Configuration
    // Validates: Requirements 10.1
    test(
      'Property 29: setDisplaySettings calls native SDK with correct parameters',
      () async {
        final random = Random();

        for (int i = 0; i < 100; i++) {
          methodCallLog.clear();

          final enabled = random.nextBool();
          final leftHand = random.nextBool();
          final maxBrightness = 5;
          final brightness = random.nextInt(maxBrightness) + 1; // 1 to 5
          final doNotDisturb = random.nextBool();
          final screenOnStartMinutes = random.nextInt(1440); // 0 to 1439
          final screenOnEndMinutes = random.nextInt(1440);

          final settings = DisplaySettings(
            enabled: enabled,
            leftHand: leftHand,
            brightness: brightness,
            maxBrightness: maxBrightness,
            doNotDisturb: doNotDisturb,
            screenOnStartMinutes: screenOnStartMinutes,
            screenOnEndMinutes: screenOnEndMinutes,
          );

          await QringSettings.setDisplaySettings(settings);

          // Verify native method was called
          expect(methodCallLog.length, 1);
          expect(methodCallLog[0].method, 'setDisplaySettings');

          // Verify all parameters were passed correctly
          final args = methodCallLog[0].arguments as Map;
          expect(args['enabled'], enabled);
          expect(args['leftHand'], leftHand);
          expect(args['brightness'], brightness);
          expect(args['maxBrightness'], maxBrightness);
          expect(args['doNotDisturb'], doNotDisturb);
          expect(args['screenOnStartMinutes'], screenOnStartMinutes);
          expect(args['screenOnEndMinutes'], screenOnEndMinutes);
        }
      },
    );

    // Property 30: Display Settings Can Be Read
    // Feature: qc-wireless-sdk-integration, Property 30: Display Settings Can Be Read
    // Validates: Requirements 10.2
    test(
      'Property 30: getDisplaySettings returns current configuration',
      () async {
        for (int i = 0; i < 100; i++) {
          methodCallLog.clear();

          final settings = await QringSettings.getDisplaySettings();

          // Verify native method was called
          expect(methodCallLog.length, 1);
          expect(methodCallLog[0].method, 'getDisplaySettings');

          // Verify returned settings contain all required fields
          expect(settings.enabled, isA<bool>());
          expect(settings.leftHand, isA<bool>());
          expect(settings.brightness, isA<int>());
          expect(settings.maxBrightness, isA<int>());
          expect(settings.doNotDisturb, isA<bool>());
          expect(settings.screenOnStartMinutes, isA<int>());
          expect(settings.screenOnEndMinutes, isA<int>());
        }
      },
    );

    // Property 31: Brightness Validation
    // Feature: qc-wireless-sdk-integration, Property 31: Brightness Validation
    // Validates: Requirements 10.3
    test(
      'Property 31: brightness must be in range [1, maxBrightness]',
      () async {
        final random = Random();

        for (int i = 0; i < 100; i++) {
          final maxBrightness = random.nextInt(10) + 1; // 1 to 10

          // Test invalid brightness values (0 or > maxBrightness)
          final invalidBrightness = random.nextBool()
              ? 0 // Below minimum
              : maxBrightness + random.nextInt(10) + 1; // Above maximum

          final invalidSettings = DisplaySettings(
            enabled: true,
            leftHand: false,
            brightness: invalidBrightness,
            maxBrightness: maxBrightness,
            doNotDisturb: false,
            screenOnStartMinutes: 0,
            screenOnEndMinutes: 1440,
          );

          // Should throw ArgumentError for invalid brightness
          expect(
            () => QringSettings.setDisplaySettings(invalidSettings),
            throwsA(isA<ArgumentError>()),
          );

          // Test valid brightness values (1 to maxBrightness)
          final validBrightness = random.nextInt(maxBrightness) + 1;

          final validSettings = DisplaySettings(
            enabled: true,
            leftHand: false,
            brightness: validBrightness,
            maxBrightness: maxBrightness,
            doNotDisturb: false,
            screenOnStartMinutes: 0,
            screenOnEndMinutes: 1440,
          );

          // Should not throw for valid brightness
          await QringSettings.setDisplaySettings(validSettings);
        }
      },
    );

    test('display settings require connection', () async {
      isConnected = false;

      final settings = DisplaySettings(
        enabled: true,
        leftHand: false,
        brightness: 3,
        maxBrightness: 5,
        doNotDisturb: false,
        screenOnStartMinutes: 0,
        screenOnEndMinutes: 1440,
      );

      // setDisplaySettings should throw when not connected
      expect(
        () => QringSettings.setDisplaySettings(settings),
        throwsA(isA<Exception>()),
      );

      // getDisplaySettings should throw when not connected
      expect(
        () => QringSettings.getDisplaySettings(),
        throwsA(isA<Exception>()),
      );
    });
  });
}
