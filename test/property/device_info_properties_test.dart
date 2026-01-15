import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Device Info Properties', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    test('Property 13: Device Info Contains Required Fields - '
        'For any successful getDeviceInfo() call, '
        'the returned object should contain all required fields', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 13: Device Info Contains Required Fields
      // Validates: Requirements 5.2, 5.3

      // Test with 100 iterations to verify property holds across many scenarios
      for (int iteration = 0; iteration < 100; iteration++) {
        // Generate random device info with varying feature flags
        final mockDeviceInfo = _generateRandomDeviceInfo(iteration);

        // Setup mock method handler for deviceInfo
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'deviceInfo') {
                return mockDeviceInfo;
              }
              return null;
            });

        // Get device info
        final deviceInfo = await QringSdkFlutter.getDeviceInfo();

        // Verify all required fields are present
        expect(
          deviceInfo.firmwareVersion,
          isNotNull,
          reason: 'Firmware version should not be null',
        );
        expect(
          deviceInfo.hardwareVersion,
          isNotNull,
          reason: 'Hardware version should not be null',
        );

        // Verify feature flags are present (can be true or false)
        expect(
          deviceInfo.supportsTemperature,
          isNotNull,
          reason: 'Temperature support flag should not be null',
        );
        expect(
          deviceInfo.supportsBloodOxygen,
          isNotNull,
          reason: 'Blood oxygen support flag should not be null',
        );
        expect(
          deviceInfo.supportsBloodPressure,
          isNotNull,
          reason: 'Blood pressure support flag should not be null',
        );
        expect(
          deviceInfo.supportsHrv,
          isNotNull,
          reason: 'HRV support flag should not be null',
        );
        expect(
          deviceInfo.supportsOneKeyCheck,
          isNotNull,
          reason: 'One-key check support flag should not be null',
        );

        // Verify values match mock data
        expect(
          deviceInfo.firmwareVersion,
          equals(mockDeviceInfo['firmwareVersion']),
          reason: 'Firmware version should match mock data',
        );
        expect(
          deviceInfo.hardwareVersion,
          equals(mockDeviceInfo['hardwareVersion']),
          reason: 'Hardware version should match mock data',
        );
        expect(
          deviceInfo.supportsTemperature,
          equals(mockDeviceInfo['supportsTemperature']),
          reason: 'Temperature support should match mock data',
        );
        expect(
          deviceInfo.supportsBloodOxygen,
          equals(mockDeviceInfo['supportsBloodOxygen']),
          reason: 'Blood oxygen support should match mock data',
        );
        expect(
          deviceInfo.supportsBloodPressure,
          equals(mockDeviceInfo['supportsBloodPressure']),
          reason: 'Blood pressure support should match mock data',
        );
        expect(
          deviceInfo.supportsHrv,
          equals(mockDeviceInfo['supportsHrv']),
          reason: 'HRV support should match mock data',
        );
        expect(
          deviceInfo.supportsOneKeyCheck,
          equals(mockDeviceInfo['supportsOneKeyCheck']),
          reason: 'One-key check support should match mock data',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test('Property 14: Device Info Returns Empty Values When Disconnected - '
        'For any invocation of getDeviceInfo() when disconnected, '
        'the method should return a QringDeviceInfo with empty values', () async {
      // Tag: Feature: qc-wireless-sdk-integration, Property 14: Device Info Returns Empty Map When Disconnected
      // Validates: Requirements 5.4

      // Test with 100 iterations to verify property holds consistently
      for (int iteration = 0; iteration < 100; iteration++) {
        // Setup mock method handler that simulates disconnected state
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'deviceInfo') {
                // Return empty map to indicate disconnected state
                return <String, dynamic>{};
              }
              return null;
            });

        // Get device info when disconnected
        final deviceInfo = await QringSdkFlutter.getDeviceInfo();

        // Verify device info has empty/default values when disconnected
        expect(
          deviceInfo.firmwareVersion,
          equals(''),
          reason: 'Firmware version should be empty when disconnected',
        );
        expect(
          deviceInfo.hardwareVersion,
          equals(''),
          reason: 'Hardware version should be empty when disconnected',
        );
        expect(
          deviceInfo.supportsTemperature,
          equals(false),
          reason: 'Temperature support should be false when disconnected',
        );
        expect(
          deviceInfo.supportsBloodOxygen,
          equals(false),
          reason: 'Blood oxygen support should be false when disconnected',
        );
        expect(
          deviceInfo.supportsBloodPressure,
          equals(false),
          reason: 'Blood pressure support should be false when disconnected',
        );
        expect(
          deviceInfo.supportsHrv,
          equals(false),
          reason: 'HRV support should be false when disconnected',
        );
        expect(
          deviceInfo.supportsOneKeyCheck,
          equals(false),
          reason: 'One-key check support should be false when disconnected',
        );
      }
    }, timeout: const Timeout(Duration(minutes: 2)));

    test(
      'Property 13 (Feature Flags): Device info correctly represents all feature flag combinations',
      () async {
        // Test all possible combinations of feature flags

        // Test various feature flag combinations
        final featureCombinations = [
          // All features supported
          {
            'supportsTemperature': true,
            'supportsBloodOxygen': true,
            'supportsBloodPressure': true,
            'supportsHrv': true,
            'supportsOneKeyCheck': true,
          },
          // No features supported
          {
            'supportsTemperature': false,
            'supportsBloodOxygen': false,
            'supportsBloodPressure': false,
            'supportsHrv': false,
            'supportsOneKeyCheck': false,
          },
          // Only temperature
          {
            'supportsTemperature': true,
            'supportsBloodOxygen': false,
            'supportsBloodPressure': false,
            'supportsHrv': false,
            'supportsOneKeyCheck': false,
          },
          // Temperature and blood oxygen
          {
            'supportsTemperature': true,
            'supportsBloodOxygen': true,
            'supportsBloodPressure': false,
            'supportsHrv': false,
            'supportsOneKeyCheck': false,
          },
          // All except one-key check
          {
            'supportsTemperature': true,
            'supportsBloodOxygen': true,
            'supportsBloodPressure': true,
            'supportsHrv': true,
            'supportsOneKeyCheck': false,
          },
        ];

        for (final features in featureCombinations) {
          final mockDeviceInfo = {
            'firmwareVersion': '1.0.0',
            'hardwareVersion': '2.0',
            ...features,
          };

          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                if (methodCall.method == 'deviceInfo') {
                  return mockDeviceInfo;
                }
                return null;
              });

          // Get device info
          final deviceInfo = await QringSdkFlutter.getDeviceInfo();

          // Verify all feature flags match
          expect(
            deviceInfo.supportsTemperature,
            equals(features['supportsTemperature']),
          );
          expect(
            deviceInfo.supportsBloodOxygen,
            equals(features['supportsBloodOxygen']),
          );
          expect(
            deviceInfo.supportsBloodPressure,
            equals(features['supportsBloodPressure']),
          );
          expect(deviceInfo.supportsHrv, equals(features['supportsHrv']));
          expect(
            deviceInfo.supportsOneKeyCheck,
            equals(features['supportsOneKeyCheck']),
          );
        }
      },
    );

    test(
      'Property 13 (Version Formats): Device info handles various version formats',
      () async {
        // Test various version string formats

        final versionFormats = [
          {'firmware': '1.0.0', 'hardware': '1.0'},
          {'firmware': '2.5.3', 'hardware': '3.2'},
          {'firmware': '10.20.30', 'hardware': '5.10'},
          {'firmware': '0.0.1', 'hardware': '0.1'},
        ];

        for (final versions in versionFormats) {
          final mockDeviceInfo = {
            'firmwareVersion': versions['firmware'],
            'hardwareVersion': versions['hardware'],
            'supportsTemperature': true,
            'supportsBloodOxygen': true,
            'supportsBloodPressure': true,
            'supportsHrv': true,
            'supportsOneKeyCheck': true,
          };

          // Setup mock method handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
                if (methodCall.method == 'deviceInfo') {
                  return mockDeviceInfo;
                }
                return null;
              });

          // Get device info
          final deviceInfo = await QringSdkFlutter.getDeviceInfo();

          // Verify versions are correctly parsed
          expect(
            deviceInfo.firmwareVersion,
            equals(versions['firmware']),
            reason: 'Firmware version should match',
          );
          expect(
            deviceInfo.hardwareVersion,
            equals(versions['hardware']),
            reason: 'Hardware version should match',
          );
        }
      },
    );
  });
}

/// Generate random device info for testing
Map<String, dynamic> _generateRandomDeviceInfo(int seed) {
  // Generate version numbers based on seed
  final fwMajor = (seed % 10) + 1;
  final fwMinor = ((seed * 7) % 10);
  final fwPatch = ((seed * 13) % 10);
  final hwMajor = (seed % 5) + 1;
  final hwMinor = ((seed * 11) % 10);

  // Generate feature flags based on seed bits
  final features = seed % 32; // 5 bits for 5 features

  return {
    'firmwareVersion': '$fwMajor.$fwMinor.$fwPatch',
    'hardwareVersion': '$hwMajor.$hwMinor',
    'supportsTemperature': (features & 0x01) != 0,
    'supportsBloodOxygen': (features & 0x02) != 0,
    'supportsBloodPressure': (features & 0x04) != 0,
    'supportsHrv': (features & 0x08) != 0,
    'supportsOneKeyCheck': (features & 0x10) != 0,
  };
}
