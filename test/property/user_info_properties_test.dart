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
            case 'setUserInfo':
              if (!isConnected) {
                throw PlatformException(
                  code: 'NOT_CONNECTED',
                  message: 'Device is not connected',
                );
              }
              return null;

            case 'setUserId':
              if (!isConnected) {
                throw PlatformException(
                  code: 'NOT_CONNECTED',
                  message: 'Device is not connected',
                );
              }
              final args = methodCall.arguments as Map;
              final userId = args['userId'] as String;

              if (userId.isEmpty) {
                throw PlatformException(
                  code: 'INVALID_ARGUMENT',
                  message: 'User ID cannot be empty',
                );
              }
              return null;

            case 'factoryReset':
              if (!isConnected) {
                throw PlatformException(
                  code: 'NOT_CONNECTED',
                  message: 'Device is not connected',
                );
              }
              return null;

            default:
              return null;
          }
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  group('User Info Properties', () {
    // Property 38: User Info Configuration Calls Native SDK
    // Feature: qc-wireless-sdk-integration, Property 38: User Info Configuration Calls Native SDK
    // Validates: Requirements 13.2
    test(
      'Property 38: setUserInfo calls native SDK with correct parameters',
      () async {
        final random = Random();

        for (int i = 0; i < 100; i++) {
          methodCallLog.clear();

          final age = random.nextInt(100) + 1; // 1 to 100
          final heightCm = random.nextInt(150) + 50; // 50 to 199
          final weightKg = random.nextInt(150) + 30; // 30 to 179
          final isMale = random.nextBool();

          final userInfo = UserInfo(
            age: age,
            heightCm: heightCm,
            weightKg: weightKg,
            isMale: isMale,
          );

          await QringSettings.setUserInfo(userInfo);

          // Verify native method was called
          expect(methodCallLog.length, 1);
          expect(methodCallLog[0].method, 'setUserInfo');

          // Verify all parameters were passed correctly
          final args = methodCallLog[0].arguments as Map;
          expect(args['age'], age);
          expect(args['heightCm'], heightCm);
          expect(args['weightKg'], weightKg);
          expect(args['isMale'], isMale);
        }
      },
    );

    test('setUserId calls native SDK with user ID', () async {
      final random = Random();

      for (int i = 0; i < 100; i++) {
        methodCallLog.clear();

        // Generate random user ID
        final userId = 'user_${random.nextInt(100000)}';

        await QringSettings.setUserId(userId);

        // Verify native method was called
        expect(methodCallLog.length, 1);
        expect(methodCallLog[0].method, 'setUserId');

        // Verify user ID was passed correctly
        final args = methodCallLog[0].arguments as Map;
        expect(args['userId'], userId);
      }
    });

    test('setUserId rejects empty user ID', () async {
      // Should throw ArgumentError for empty user ID
      expect(() => QringSettings.setUserId(''), throwsA(isA<ArgumentError>()));
    });

    test('factoryReset calls native SDK', () async {
      for (int i = 0; i < 100; i++) {
        methodCallLog.clear();

        await QringSdkFlutter.factoryReset();

        // Verify native method was called
        expect(methodCallLog.length, 1);
        expect(methodCallLog[0].method, 'factoryReset');
      }
    });

    test('user info methods require connection', () async {
      isConnected = false;

      final userInfo = UserInfo(
        age: 30,
        heightCm: 170,
        weightKg: 70,
        isMale: true,
      );

      // setUserInfo should throw when not connected
      expect(
        () => QringSettings.setUserInfo(userInfo),
        throwsA(isA<Exception>()),
      );

      // setUserId should throw when not connected
      expect(
        () => QringSettings.setUserId('user123'),
        throwsA(isA<Exception>()),
      );

      // factoryReset should throw when not connected
      expect(() => QringSdkFlutter.factoryReset(), throwsA(isA<Exception>()));
    });
  });
}
