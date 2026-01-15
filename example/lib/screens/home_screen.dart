import 'dart:async';

import 'package:flutter/material.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart' as qring;

import 'exercise_screen.dart';
import 'health_data_screen.dart';
import 'quick_actions_screen.dart';
import 'settings_screen.dart';

/// Main home screen with bottom navigation
///
/// Provides navigation between:
/// - Quick Actions (find ring, battery, device info)
/// - Health Data (measurements and sync)
/// - Settings (device configuration)
/// - Exercise (workout tracking)
///
/// Also displays connection status in the app bar
class HomeScreen extends StatefulWidget {
  final String deviceMac;

  const HomeScreen({super.key, required this.deviceMac});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;
  qring.ConnectionState _connectionState = qring.ConnectionState.disconnected;
  qring.ServiceState? _serviceState;
  StreamSubscription<qring.ServiceState>? _serviceStateSubscription;

  @override
  void initState() {
    super.initState();
    _setupConnectionListener();
    _setupServiceStateListener();
  }

  @override
  void dispose() {
    _serviceStateSubscription?.cancel();
    super.dispose();
  }

  /// Set up listener for connection state changes
  /// Navigates back to scanning screen if disconnected
  void _setupConnectionListener() {
    qring.QringSdkFlutter.connectionStateStream.listen((state) {
      if (mounted) {
        setState(() {
          _connectionState = state;
        });
        // If disconnected, navigate back to scanning screen
        if (state == qring.ConnectionState.disconnected) {
          Navigator.of(context).pop();
        }
      }
    });
  }

  /// Set up listener for service state changes
  void _setupServiceStateListener() {
    _serviceStateSubscription = qring.QringSdkFlutter.serviceStateStream.listen(
      (state) {
        if (mounted) {
          setState(() {
            _serviceState = state;
          });
        }
      },
    );
  }

  Color _getConnectionStateColor() {
    switch (_connectionState) {
      case qring.ConnectionState.connected:
        return Colors.green;
      case qring.ConnectionState.connecting:
        return Colors.orange;
      case qring.ConnectionState.pairing:
        return Colors.blue;
      case qring.ConnectionState.reconnecting:
        return Colors.amber;
      case qring.ConnectionState.disconnecting:
        return Colors.orange;
      case qring.ConnectionState.disconnected:
        return Colors.grey;
    }
  }

  String _getConnectionStateText() {
    switch (_connectionState) {
      case qring.ConnectionState.connected:
        return 'Connected';
      case qring.ConnectionState.connecting:
        return 'Connecting...';
      case qring.ConnectionState.pairing:
        return 'Pairing...';
      case qring.ConnectionState.reconnecting:
        return 'Reconnecting...';
      case qring.ConnectionState.disconnecting:
        return 'Disconnecting...';
      case qring.ConnectionState.disconnected:
        return 'Disconnected';
    }
  }

  @override
  Widget build(BuildContext context) {
    final isServiceRunning = _serviceState?.isRunning ?? false;
    final isServiceConnected = _serviceState?.isConnected ?? false;

    return Scaffold(
      appBar: AppBar(
        title: const Text('QRing Control'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          // Service status badge
          if (isServiceRunning)
            Padding(
              padding: const EdgeInsets.only(right: 8.0),
              child: Tooltip(
                message: isServiceConnected
                    ? 'Background service active - Connected'
                    : 'Background service active - Reconnecting',
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: isServiceConnected
                        ? Colors.green.shade100
                        : Colors.orange.shade100,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: isServiceConnected ? Colors.green : Colors.orange,
                      width: 1,
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.cloud_circle,
                        size: 16,
                        color: isServiceConnected
                            ? Colors.green
                            : Colors.orange,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        'Service',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: isServiceConnected
                              ? Colors.green.shade900
                              : Colors.orange.shade900,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          // Connection status
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Row(
              children: [
                Icon(Icons.circle, size: 12, color: _getConnectionStateColor()),
                const SizedBox(width: 8),
                Text(
                  _getConnectionStateText(),
                  style: TextStyle(
                    fontSize: 14,
                    color: _getConnectionStateColor(),
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      body: IndexedStack(
        index: _currentIndex,
        children: [
          QuickActionsScreen(deviceMac: widget.deviceMac),
          // Health Data tab (Task 22)
          const HealthDataScreen(),
          // Settings tab (Task 23)
          const SettingsScreen(),
          // Exercise tab (Task 24)
          const ExerciseScreen(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard),
            label: 'Quick Actions',
          ),
          NavigationDestination(
            icon: Icon(Icons.favorite),
            label: 'Health Data',
          ),
          NavigationDestination(icon: Icon(Icons.settings), label: 'Settings'),
          NavigationDestination(
            icon: Icon(Icons.directions_run),
            label: 'Exercise',
          ),
        ],
      ),
    );
  }
}
