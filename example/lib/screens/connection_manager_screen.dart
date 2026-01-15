import 'dart:async';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart' as qring;

/// Comprehensive connection management screen demonstrating production-grade BLE features
///
/// This screen showcases:
/// - Connection state display with all states (idle, connecting, pairing, connected, reconnecting, disconnected, error)
/// - Error handling and display with typed error messages
/// - Reconnection status with attempt counter
/// - Permission request flow for Android 12+
/// - Background service start/stop controls
/// - Real-time battery updates
/// - Service state monitoring
class ConnectionManagerScreen extends StatefulWidget {
  final String deviceMac;
  final String deviceName;

  const ConnectionManagerScreen({
    super.key,
    required this.deviceMac,
    required this.deviceName,
  });

  @override
  State<ConnectionManagerScreen> createState() =>
      _ConnectionManagerScreenState();
}

class _ConnectionManagerScreenState extends State<ConnectionManagerScreen> {
  qring.ConnectionState _connectionState = qring.ConnectionState.disconnected;
  qring.ServiceState? _serviceState;
  qring.BleError? _lastError;
  int _batteryLevel = -1;
  int _reconnectAttempts = 0;

  StreamSubscription<qring.ConnectionState>? _connectionSubscription;
  StreamSubscription<qring.ServiceState>? _serviceSubscription;
  StreamSubscription<qring.BleError>? _errorSubscription;
  StreamSubscription<Map<String, dynamic>>? _batterySubscription;
  StreamSubscription<Map<String, dynamic>>? _bleStateSubscription;

  @override
  void initState() {
    super.initState();
    _setupListeners();
    _checkInitialState();
  }

  @override
  void dispose() {
    _connectionSubscription?.cancel();
    _serviceSubscription?.cancel();
    _errorSubscription?.cancel();
    _batterySubscription?.cancel();
    _bleStateSubscription?.cancel();
    super.dispose();
  }

  /// Set up all event stream listeners
  void _setupListeners() {
    // Connection state changes
    _connectionSubscription = qring.QringSdkFlutter.connectionStateStream
        .listen((state) {
          if (mounted) {
            setState(() {
              _connectionState = state;
            });
          }
        });

    // Service state changes
    _serviceSubscription = qring.QringSdkFlutter.serviceStateStream.listen((
      state,
    ) {
      if (mounted) {
        setState(() {
          _serviceState = state;
          _reconnectAttempts = state.reconnectAttempts;
        });
      }
    });

    // Error events
    _errorSubscription = qring.QringSdkFlutter.errorStream.listen((error) {
      if (mounted) {
        setState(() {
          _lastError = error;
        });
        _showErrorSnackBar(error);
      }
    });

    // Battery updates
    _batterySubscription = qring.QringSdkFlutter.bleBatteryStream.listen((
      event,
    ) {
      if (mounted) {
        setState(() {
          _batteryLevel = event['batteryLevel'] as int? ?? -1;
        });
      }
    });

    // BLE connection state changes (for detailed state info)
    _bleStateSubscription = qring.QringSdkFlutter.bleConnectionStateStream
        .listen((event) {
          if (mounted) {
            final newState = event['newState'] as String?;
            debugPrint('BLE State Change: ${event['oldState']} -> $newState');
          }
        });
  }

  /// Check initial connection and service state
  Future<void> _checkInitialState() async {
    try {
      final isRunning = await qring.QringSdkFlutter.isServiceRunning();
      if (mounted && isRunning) {
        // Service is already running, get battery level
        _getBatteryLevel();
      }
    } catch (e) {
      debugPrint('Failed to check initial state: $e');
    }
  }

  /// Request all required permissions
  Future<bool> _requestPermissions() async {
    final statuses = await [
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.location,
      Permission.notification,
    ].request();

    final allGranted = statuses.values.every((status) => status.isGranted);

    if (!allGranted && mounted) {
      _showPermissionDeniedDialog();
    }

    return allGranted;
  }

  /// Start background service
  Future<void> _startBackgroundService() async {
    if (!await _requestPermissions()) {
      return;
    }

    try {
      await qring.QringSdkFlutter.startBackgroundService(widget.deviceMac);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Background service started'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to start service: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  /// Stop background service
  Future<void> _stopBackgroundService() async {
    try {
      await qring.QringSdkFlutter.stopBackgroundService();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Background service stopped'),
            backgroundColor: Colors.orange,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to stop service: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  /// Get current battery level
  Future<void> _getBatteryLevel() async {
    try {
      final battery = await qring.QringSdkFlutter.getBattery();
      if (mounted) {
        setState(() {
          _batteryLevel = battery;
        });
      }
    } catch (e) {
      debugPrint('Failed to get battery: $e');
    }
  }

  /// Trigger Find My Ring
  Future<void> _findRing() async {
    try {
      await qring.QringSdkFlutter.findRing();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Ring is vibrating!'),
            backgroundColor: Colors.blue,
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to find ring: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  /// Show error in snackbar
  void _showErrorSnackBar(qring.BleError error) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Error: ${error.type.description}',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            Text(error.message),
          ],
        ),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 5),
        action: SnackBarAction(
          label: 'Dismiss',
          textColor: Colors.white,
          onPressed: () {},
        ),
      ),
    );
  }

  /// Show permission denied dialog
  void _showPermissionDeniedDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Permissions Required'),
        content: const Text(
          'This app requires Bluetooth and Location permissions to connect to your QRing device. '
          'Please grant the required permissions in Settings.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              openAppSettings();
            },
            child: const Text('Open Settings'),
          ),
        ],
      ),
    );
  }

  /// Get color for connection state
  Color _getStateColor() {
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

  /// Get icon for connection state
  IconData _getStateIcon() {
    switch (_connectionState) {
      case qring.ConnectionState.connected:
        return Icons.check_circle;
      case qring.ConnectionState.connecting:
        return Icons.sync;
      case qring.ConnectionState.pairing:
        return Icons.link;
      case qring.ConnectionState.reconnecting:
        return Icons.refresh;
      case qring.ConnectionState.disconnecting:
        return Icons.sync;
      case qring.ConnectionState.disconnected:
        return Icons.bluetooth_disabled;
    }
  }

  /// Build connection status card
  Widget _buildConnectionStatusCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(_getStateIcon(), color: _getStateColor(), size: 32),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Connection Status',
                        style: TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _connectionState.name.toUpperCase(),
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: _getStateColor(),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(),
            const SizedBox(height: 8),
            _buildInfoRow('Device', widget.deviceName),
            _buildInfoRow('MAC Address', widget.deviceMac),
            if (_batteryLevel >= 0) _buildInfoRow('Battery', '$_batteryLevel%'),
            if (_reconnectAttempts > 0)
              _buildInfoRow(
                'Reconnect Attempts',
                '$_reconnectAttempts',
                color: Colors.amber,
              ),
          ],
        ),
      ),
    );
  }

  /// Build info row
  Widget _buildInfoRow(String label, String value, {Color? color}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.grey)),
          Text(
            value,
            style: TextStyle(fontWeight: FontWeight.w500, color: color),
          ),
        ],
      ),
    );
  }

  /// Build service controls card
  Widget _buildServiceControlsCard() {
    final isServiceRunning = _serviceState?.isRunning ?? false;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  isServiceRunning ? Icons.cloud_done : Icons.cloud_off,
                  color: isServiceRunning ? Colors.green : Colors.grey,
                ),
                const SizedBox(width: 12),
                const Text(
                  'Background Service',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              isServiceRunning
                  ? 'Service is running. Connection will be maintained even when app is closed.'
                  : 'Service is not running. Start it to maintain connection in background.',
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: isServiceRunning
                        ? null
                        : _startBackgroundService,
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('Start Service'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.green,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: isServiceRunning ? _stopBackgroundService : null,
                    icon: const Icon(Icons.stop),
                    label: const Text('Stop Service'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// Build error display card
  Widget _buildErrorCard() {
    if (_lastError == null) return const SizedBox.shrink();

    return Card(
      color: Colors.red.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.error, color: Colors.red),
                const SizedBox(width: 12),
                const Text(
                  'Last Error',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.red,
                  ),
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close, size: 20),
                  onPressed: () {
                    setState(() {
                      _lastError = null;
                    });
                  },
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              _lastError!.type.description,
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
            ),
            const SizedBox(height: 4),
            Text(
              _lastError!.message,
              style: const TextStyle(fontSize: 12, color: Colors.black87),
            ),
            const SizedBox(height: 4),
            Text(
              'Code: ${_lastError!.code}',
              style: const TextStyle(fontSize: 10, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }

  /// Build quick actions card
  Widget _buildQuickActionsCard() {
    final isConnected = _connectionState == qring.ConnectionState.connected;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.flash_on, color: Colors.blue),
                SizedBox(width: 12),
                Text(
                  'Quick Actions',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: isConnected ? _findRing : null,
                    icon: const Icon(Icons.vibration),
                    label: const Text('Find Ring'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: isConnected ? _getBatteryLevel : null,
                    icon: const Icon(Icons.battery_charging_full),
                    label: const Text('Get Battery'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.orange,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Connection Manager'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildConnectionStatusCard(),
            const SizedBox(height: 16),
            _buildServiceControlsCard(),
            const SizedBox(height: 16),
            _buildQuickActionsCard(),
            const SizedBox(height: 16),
            _buildErrorCard(),
          ],
        ),
      ),
    );
  }
}
