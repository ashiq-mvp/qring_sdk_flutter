import 'dart:async';

import 'package:flutter/material.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart' as qring;

import '../widgets/error_card.dart';

/// Screen for quick device actions
///
/// Provides:
/// - Find My Ring feature (make ring vibrate)
/// - Battery level display with refresh
/// - Device information (firmware, hardware, features)
/// - Disconnect functionality
/// - Background service control
class QuickActionsScreen extends StatefulWidget {
  final String deviceMac;

  const QuickActionsScreen({super.key, required this.deviceMac});

  @override
  State<QuickActionsScreen> createState() => _QuickActionsScreenState();
}

class _QuickActionsScreenState extends State<QuickActionsScreen> {
  int _batteryLevel = -1;
  qring.QringDeviceInfo? _deviceInfo;
  bool _isLoadingBattery = false;
  bool _isLoadingDeviceInfo = false;
  bool _isFindingRing = false;
  String? _errorMessage;
  String? _successMessage;

  // Background service state
  qring.ServiceState? _serviceState;
  StreamSubscription<qring.ServiceState>? _serviceStateSubscription;

  @override
  void initState() {
    super.initState();
    _loadDeviceData();
    _setupServiceStateListener();
    _checkServiceStatus();
  }

  @override
  void dispose() {
    _serviceStateSubscription?.cancel();
    super.dispose();
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

  /// Check initial service status
  Future<void> _checkServiceStatus() async {
    try {
      final isRunning = await qring.QringSdkFlutter.isServiceRunning();
      if (mounted && isRunning) {
        // Service is running, state will come through stream
      }
    } catch (e) {
      // Ignore errors on initial check
    }
  }

  /// Load device data (battery and device info) on screen init
  Future<void> _loadDeviceData() async {
    await Future.wait([_refreshBattery(), _refreshDeviceInfo()]);
  }

  Future<void> _refreshBattery() async {
    setState(() {
      _isLoadingBattery = true;
      _errorMessage = null;
    });

    try {
      // Add timeout to prevent infinite loading
      final battery = await qring.QringSdkFlutter.getBattery().timeout(
        const Duration(seconds: 10),
        onTimeout: () {
          throw TimeoutException('Battery request timed out');
        },
      );

      if (mounted) {
        setState(() {
          _batteryLevel = battery;
        });
      }
    } on TimeoutException {
      if (mounted) {
        setState(() {
          _errorMessage = 'Request timed out. Please try again.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to get battery: $e';
        });
      }
    } finally {
      // Always clear loading state
      if (mounted) {
        setState(() {
          _isLoadingBattery = false;
        });
      }
    }
  }

  Future<void> _refreshDeviceInfo() async {
    setState(() {
      _isLoadingDeviceInfo = true;
      _errorMessage = null;
    });

    try {
      // Add timeout to prevent infinite loading
      final info = await qring.QringSdkFlutter.getDeviceInfo().timeout(
        const Duration(seconds: 10),
        onTimeout: () {
          throw TimeoutException('Device info request timed out');
        },
      );

      if (mounted) {
        setState(() {
          _deviceInfo = info;
        });
      }
    } on TimeoutException {
      if (mounted) {
        setState(() {
          _errorMessage = 'Request timed out. Please try again.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to get device info: $e';
        });
      }
    } finally {
      // Always clear loading state
      if (mounted) {
        setState(() {
          _isLoadingDeviceInfo = false;
        });
      }
    }
  }

  /// Trigger the Find My Ring feature
  /// Makes the ring vibrate to help locate it
  Future<void> _findRing() async {
    setState(() {
      _isFindingRing = true;
      _errorMessage = null;
      _successMessage = null;
    });

    final startTime = DateTime.now();

    try {
      // Call the SDK method
      await qring.QringSdkFlutter.findRing();

      // Ensure minimum 500ms loading time for better UX
      final elapsed = DateTime.now().difference(startTime);
      if (elapsed.inMilliseconds < 500) {
        await Future.delayed(
          Duration(milliseconds: 500 - elapsed.inMilliseconds),
        );
      }

      if (mounted) {
        setState(() {
          _isFindingRing = false;
          _successMessage = 'Ring is vibrating!';
        });
        // Clear success message after 3 seconds
        Future.delayed(const Duration(seconds: 3), () {
          if (mounted) {
            setState(() {
              _successMessage = null;
            });
          }
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to find ring: $e';
          _isFindingRing = false;
        });
      }
    }

    // Safety timeout: ensure loading state clears after 2 seconds maximum
    Future.delayed(const Duration(seconds: 2), () {
      if (mounted && _isFindingRing) {
        setState(() {
          _isFindingRing = false;
        });
      }
    });
  }

  Future<void> _disconnect() async {
    try {
      await qring.QringSdkFlutter.disconnect();
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to disconnect: $e';
        });
      }
    }
  }

  /// Start the background service
  Future<void> _startBackgroundService() async {
    setState(() {
      _errorMessage = null;
      _successMessage = null;
    });

    // Use the device MAC passed from parent
    final deviceMac = widget.deviceMac;

    if (deviceMac.isEmpty) {
      if (mounted) {
        setState(() {
          _errorMessage =
              'No device connected. Please connect to a device first.';
        });
      }
      return;
    }

    try {
      await qring.QringSdkFlutter.startBackgroundService(deviceMac);
      if (mounted) {
        setState(() {
          _successMessage = 'Background service started';
        });
        // Clear success message after 3 seconds
        Future.delayed(const Duration(seconds: 3), () {
          if (mounted) {
            setState(() {
              _successMessage = null;
            });
          }
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to start background service: $e';
        });
      }
    }
  }

  /// Stop the background service
  Future<void> _stopBackgroundService() async {
    setState(() {
      _errorMessage = null;
      _successMessage = null;
    });

    try {
      await qring.QringSdkFlutter.stopBackgroundService();
      if (mounted) {
        setState(() {
          _successMessage = 'Background service stopped';
        });
        // Clear success message after 3 seconds
        Future.delayed(const Duration(seconds: 3), () {
          if (mounted) {
            setState(() {
              _successMessage = null;
            });
          }
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to stop background service: $e';
        });
      }
    }
  }

  Widget _buildFindMyRingButton() {
    return Card(
      elevation: 4,
      child: InkWell(
        onTap: _isFindingRing ? null : _findRing,
        borderRadius: BorderRadius.circular(12),
        child: Tooltip(
          message: 'Make your ring vibrate to help locate it',
          child: Container(
            padding: const EdgeInsets.all(32.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AnimatedScale(
                  scale: _isFindingRing ? 1.2 : 1.0,
                  duration: const Duration(milliseconds: 500),
                  curve: Curves.easeInOut,
                  child: Icon(
                    Icons.notifications_active,
                    size: 80,
                    color: _isFindingRing
                        ? Colors.grey
                        : Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  'Find My Ring',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  _isFindingRing
                      ? 'Sending signal...'
                      : 'Tap to make your ring vibrate',
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(color: Colors.grey[600]),
                  textAlign: TextAlign.center,
                ),
                if (_isFindingRing) ...[
                  const SizedBox(height: 16),
                  const CircularProgressIndicator(),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBatteryIndicator() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            Icon(_getBatteryIcon(), size: 48, color: _getBatteryColor()),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Battery Level',
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 4),
                  if (_isLoadingBattery)
                    const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  else
                    Text(
                      _batteryLevel >= 0 ? '$_batteryLevel%' : 'Unknown',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: _getBatteryColor(),
                      ),
                    ),
                ],
              ),
            ),
            Tooltip(
              message: 'Refresh battery level',
              child: IconButton(
                icon: const Icon(Icons.refresh),
                onPressed: _isLoadingBattery ? null : _refreshBattery,
              ),
            ),
          ],
        ),
      ),
    );
  }

  IconData _getBatteryIcon() {
    if (_batteryLevel < 0) return Icons.battery_unknown;
    if (_batteryLevel <= 10) return Icons.battery_0_bar;
    if (_batteryLevel <= 20) return Icons.battery_1_bar;
    if (_batteryLevel <= 40) return Icons.battery_2_bar;
    if (_batteryLevel <= 60) return Icons.battery_3_bar;
    if (_batteryLevel <= 80) return Icons.battery_4_bar;
    if (_batteryLevel <= 95) return Icons.battery_5_bar;
    return Icons.battery_full;
  }

  Color _getBatteryColor() {
    if (_batteryLevel < 0) return Colors.grey;
    if (_batteryLevel <= 20) return Colors.red;
    if (_batteryLevel <= 50) return Colors.orange;
    return Colors.green;
  }

  Widget _buildDeviceInfoCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.info_outline, size: 24),
                const SizedBox(width: 12),
                const Text(
                  'Device Information',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const Spacer(),
                Tooltip(
                  message: 'Refresh device information',
                  child: IconButton(
                    icon: const Icon(Icons.refresh),
                    onPressed: _isLoadingDeviceInfo ? null : _refreshDeviceInfo,
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            if (_isLoadingDeviceInfo)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(16.0),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (_deviceInfo != null) ...[
              _buildInfoRow(
                'Firmware Version',
                _deviceInfo!.firmwareVersion.isEmpty
                    ? 'Unknown'
                    : _deviceInfo!.firmwareVersion,
                Icons.system_update,
              ),
              const SizedBox(height: 12),
              _buildInfoRow(
                'Hardware Version',
                _deviceInfo!.hardwareVersion.isEmpty
                    ? 'Unknown'
                    : _deviceInfo!.hardwareVersion,
                Icons.memory,
              ),
              const SizedBox(height: 16),
              const Text(
                'Supported Features',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: Colors.grey,
                ),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _buildFeatureChip(
                    'Temperature',
                    _deviceInfo!.supportsTemperature,
                  ),
                  _buildFeatureChip(
                    'Blood Oxygen',
                    _deviceInfo!.supportsBloodOxygen,
                  ),
                  _buildFeatureChip(
                    'Blood Pressure',
                    _deviceInfo!.supportsBloodPressure,
                  ),
                  _buildFeatureChip('HRV', _deviceInfo!.supportsHrv),
                  _buildFeatureChip(
                    'One-Key Check',
                    _deviceInfo!.supportsOneKeyCheck,
                  ),
                ],
              ),
            ] else
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(16.0),
                  child: Text(
                    'No device information available',
                    style: TextStyle(color: Colors.grey),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value, IconData icon) {
    return Row(
      children: [
        Icon(icon, size: 20, color: Colors.grey[600]),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
              const SizedBox(height: 2),
              Text(
                value,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildFeatureChip(String label, bool supported) {
    return Chip(
      avatar: Icon(
        supported ? Icons.check_circle : Icons.cancel,
        size: 18,
        color: supported ? Colors.green : Colors.grey,
      ),
      label: Text(label),
      backgroundColor: supported ? Colors.green.shade50 : Colors.grey.shade100,
      labelStyle: TextStyle(
        fontSize: 12,
        color: supported ? Colors.green.shade900 : Colors.grey.shade700,
      ),
    );
  }

  Widget _buildDisconnectButton() {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: _disconnect,
        icon: const Icon(Icons.link_off),
        label: const Text('Disconnect Device'),
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.red,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 16),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  /// Build the background service control card
  Widget _buildBackgroundServiceCard() {
    final isServiceRunning = _serviceState?.isRunning ?? false;
    final isServiceConnected = _serviceState?.isConnected ?? false;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.cloud_circle,
                  size: 24,
                  color: isServiceRunning ? Colors.blue : Colors.grey,
                ),
                const SizedBox(width: 12),
                const Text(
                  'Background Service',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              'Keep your ring connected even when the app is closed',
              style: TextStyle(fontSize: 12, color: Colors.grey[600]),
            ),
            const Divider(height: 24),

            // Service status indicator
            Row(
              children: [
                Icon(
                  Icons.circle,
                  size: 12,
                  color: isServiceRunning
                      ? (isServiceConnected ? Colors.green : Colors.orange)
                      : Colors.grey,
                ),
                const SizedBox(width: 8),
                Text(
                  isServiceRunning
                      ? (isServiceConnected
                            ? 'Service Active - Connected'
                            : 'Service Active - Reconnecting')
                      : 'Service Inactive',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: isServiceRunning
                        ? (isServiceConnected ? Colors.green : Colors.orange)
                        : Colors.grey,
                  ),
                ),
              ],
            ),

            if (isServiceRunning &&
                _serviceState?.reconnectAttempts != null &&
                _serviceState!.reconnectAttempts > 0) ...[
              const SizedBox(height: 8),
              Text(
                'Reconnection attempts: ${_serviceState!.reconnectAttempts}',
                style: TextStyle(fontSize: 12, color: Colors.grey[600]),
              ),
            ],

            const SizedBox(height: 16),

            // Control buttons
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
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      disabledBackgroundColor: Colors.grey[300],
                      disabledForegroundColor: Colors.grey[600],
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
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      disabledBackgroundColor: Colors.grey[300],
                      disabledForegroundColor: Colors.grey[600],
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
      body: RefreshIndicator(
        onRefresh: _loadDeviceData,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (_successMessage != null) ...[
                Card(
                  color: Colors.green.shade50,
                  child: Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Row(
                      children: [
                        const Icon(Icons.check_circle, color: Colors.green),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            _successMessage!,
                            style: const TextStyle(color: Colors.green),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),
              ],
              if (_errorMessage != null) ...[
                ErrorCard(
                  message: _errorMessage!,
                  onRetry: _loadDeviceData,
                  onDismiss: () {
                    setState(() {
                      _errorMessage = null;
                    });
                  },
                ),
                const SizedBox(height: 16),
              ],
              _buildFindMyRingButton(),
              const SizedBox(height: 16),
              _buildBackgroundServiceCard(),
              const SizedBox(height: 16),
              _buildBatteryIndicator(),
              const SizedBox(height: 16),
              _buildDeviceInfoCard(),
              const SizedBox(height: 24),
              _buildDisconnectButton(),
            ],
          ),
        ),
      ),
    );
  }
}
