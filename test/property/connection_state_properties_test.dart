import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Connection State Properties', () {
    const MethodChannel channel = MethodChannel('qring_sdk_flutter');
    const EventChannel stateChannel = EventChannel('qring_sdk_flutter/state');

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(stateChannel, null);
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(stateChannel, null);
    });

    test(
      'Property 5: Successful Connection Emits Connected State - '
      'For any successful BLE connection, '
      'the connectionStateStream should emit ConnectionState.connected',
      () async {
        // Tag: Feature: qc-wireless-sdk-integration, Property 5: Successful Connection Emits Connected State
        // Validates: Requirements 2.2

        // Setup mock method handler for connect
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'connect') {
                return null;
              }
              return null;
            });

        // Test with 100 iterations to verify property holds across many scenarios
        for (int iteration = 0; iteration < 100; iteration++) {
          // Generate random MAC address
          final macAddress = _generateRandomMacAddress(iteration);

          // Setup mock stream handler that emits connection states
          final states = <String>[];
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                stateChannel,
                MockStreamHandler.inline(
                  onListen:
                      (Object? arguments, MockStreamHandlerEventSink events) {
                        // Simulate connection sequence: connecting -> connected
                        events.success('connecting');
                        states.add('connecting');

                        // Simulate successful connection after a delay
                        Future.delayed(const Duration(milliseconds: 10), () {
                          events.success('connected');
                          states.add('connected');
                        });
                      },
                ),
              );

          // Start listening to connection state stream
          final stateStreamFuture = QringSdkFlutter.connectionStateStream
              .where((state) => state == ConnectionState.connected)
              .first;

          // Trigger connection
          await QringSdkFlutter.connect(macAddress);

          // Wait for connected state to appear in stream
          final connectedState = await stateStreamFuture.timeout(
            const Duration(seconds: 2),
            onTimeout: () => ConnectionState.disconnected,
          );

          // Verify connected state was emitted
          expect(
            connectedState,
            equals(ConnectionState.connected),
            reason: 'Successful connection should emit connected state',
          );

          // Verify the state sequence is correct
          expect(
            states,
            contains('connecting'),
            reason: 'Should emit connecting state first',
          );
          expect(
            states,
            contains('connected'),
            reason: 'Should emit connected state after connecting',
          );
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );

    test(
      'Property 5 (State Sequence): Connection state transitions follow correct sequence',
      () async {
        // Test that connection states transition in the correct order

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'connect') {
                return null;
              }
              return null;
            });

        for (int iteration = 0; iteration < 50; iteration++) {
          final macAddress = _generateRandomMacAddress(iteration);
          final receivedStates = <ConnectionState>[];

          // Setup mock stream handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                stateChannel,
                MockStreamHandler.inline(
                  onListen:
                      (Object? arguments, MockStreamHandlerEventSink events) {
                        // Emit connection state sequence
                        events.success('connecting');
                        Future.delayed(const Duration(milliseconds: 10), () {
                          events.success('connected');
                        });
                      },
                ),
              );

          // Listen to all connection states
          final subscription = QringSdkFlutter.connectionStateStream.listen((
            state,
          ) {
            receivedStates.add(state);
          });

          // Trigger connection
          await QringSdkFlutter.connect(macAddress);

          // Wait for states to be emitted
          await Future.delayed(const Duration(milliseconds: 100));

          // Verify state sequence
          expect(
            receivedStates,
            isNotEmpty,
            reason: 'Should receive connection states',
          );

          // First state should be connecting
          expect(
            receivedStates.first,
            equals(ConnectionState.connecting),
            reason: 'First state should be connecting',
          );

          // Should eventually reach connected state
          expect(
            receivedStates,
            contains(ConnectionState.connected),
            reason: 'Should eventually emit connected state',
          );

          // Verify connecting comes before connected
          final connectingIndex = receivedStates.indexOf(
            ConnectionState.connecting,
          );
          final connectedIndex = receivedStates.indexOf(
            ConnectionState.connected,
          );
          expect(
            connectingIndex,
            lessThan(connectedIndex),
            reason: 'Connecting state should come before connected state',
          );

          await subscription.cancel();
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );

    test(
      'Property 5 (Disconnect): Disconnect emits disconnected state',
      () async {
        // Test that disconnection emits the disconnected state

        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
              if (methodCall.method == 'disconnect') {
                return null;
              }
              return null;
            });

        for (int iteration = 0; iteration < 50; iteration++) {
          // Setup mock stream handler
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                stateChannel,
                MockStreamHandler.inline(
                  onListen:
                      (Object? arguments, MockStreamHandlerEventSink events) {
                        // Emit disconnecting then disconnected
                        events.success('disconnecting');
                        Future.delayed(const Duration(milliseconds: 10), () {
                          events.success('disconnected');
                        });
                      },
                ),
              );

          // Start listening to connection state stream
          final stateStreamFuture = QringSdkFlutter.connectionStateStream
              .where((state) => state == ConnectionState.disconnected)
              .first;

          // Trigger disconnection
          await QringSdkFlutter.disconnect();

          // Wait for disconnected state
          final disconnectedState = await stateStreamFuture.timeout(
            const Duration(seconds: 2),
            onTimeout: () => ConnectionState.connected,
          );

          // Verify disconnected state was emitted
          expect(
            disconnectedState,
            equals(ConnectionState.disconnected),
            reason: 'Disconnect should emit disconnected state',
          );
        }
      },
      timeout: const Timeout(Duration(minutes: 2)),
    );
  });
}

/// Generate a random MAC address for testing
String _generateRandomMacAddress(int seed) {
  final parts = List.generate(6, (index) {
    final value = ((seed * 17 + index * 23) % 256)
        .toRadixString(16)
        .padLeft(2, '0');
    return value.toUpperCase();
  });
  return parts.join(':');
}
