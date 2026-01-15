import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart' as qring;

import '../widgets/error_card.dart';
import 'connection_manager_screen.dart';
import 'home_screen.dart';

/// Screen for scanning and connecting to QRing devices
///
/// This screen provides:
/// - BLE device scanning functionality with SDK-driven filtering
/// - List of discovered QRing-compatible devices with signal strength
/// - Connection management
/// - Real-time connection status updates
///
/// All devices displayed are pre-validated by the native layer and guaranteed
/// to be QRing-compatible. No additional filtering is needed in Flutter.
class DeviceScanningScreen extends StatefulWidget {
  const DeviceScanningScreen({super.key});

  @override
  State<DeviceScanningScreen> createState() => _DeviceScanningScreenState();
}

class _DeviceScanningScreenState extends State<DeviceScanningScreen> {
  qring.ConnectionState _connectionState = qring.ConnectionState.disconnected;
  List<qring.QringDevice> _devices = [];
  bool _isScanning = false;
  String? _errorMessage;
  String? _connectingDeviceMac; // Track which device is currently connecting

  @override
  void initState() {
    super.initState();
    _setupListeners();
  }

  /// Set up stream listeners for connection state and discovered devices
  void _setupListeners() {
    // Listen to connection state changes
    qring.QringSdkFlutter.connectionStateStream.listen((state) {
      if (mounted) {
        setState(() {
          _connectionState = state;
          // Clear connecting device when connection completes (success or failure)
          if (state == qring.ConnectionState.connected ||
              state == qring.ConnectionState.disconnected) {
            _connectingDeviceMac = null;
          }
        });
        // Navigate to home screen when connected
        if (state == qring.ConnectionState.connected) {
          Navigator.of(context).push(
            PageRouteBuilder(
              pageBuilder: (context, animation, secondaryAnimation) =>
                  HomeScreen(deviceMac: _connectingDeviceMac ?? ''),
              transitionsBuilder:
                  (context, animation, secondaryAnimation, child) {
                    const begin = Offset(1.0, 0.0);
                    const end = Offset.zero;
                    const curve = Curves.easeInOut;
                    var tween = Tween(
                      begin: begin,
                      end: end,
                    ).chain(CurveTween(curve: curve));
                    var offsetAnimation = animation.drive(tween);
                    return SlideTransition(
                      position: offsetAnimation,
                      child: child,
                    );
                  },
            ),
          );
        }
      }
    });

    // Listen to discovered devices
    // All devices received from native layer are already validated
    qring.QringSdkFlutter.devicesStream.listen((devices) {
      if (mounted) {
        setState(() {
          // No additional filtering needed - SDK already filters for QRing devices
          _devices = devices
              .where((e) => e.name.toLowerCase().startsWith('r'))
              .toList();
        });
      }
    });
  }

  /// Request required Bluetooth and Location permissions
  /// Returns true if all permissions are granted
  Future<bool> _requestPermissions() async {
    final statuses = await [
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.location,
    ].request();

    final allGranted = statuses.values.every((status) => status.isGranted);

    if (!allGranted && mounted) {
      setState(() {
        _errorMessage = 'Required permissions not granted';
      });
    }

    return allGranted;
  }

  /// Start BLE scanning for QRing devices
  /// Requests permissions first if not already granted
  Future<void> _startScan() async {
    setState(() {
      _errorMessage = null;
      _devices = [];
    });

    if (!await _requestPermissions()) {
      return;
    }

    try {
      await qring.QringSdkFlutter.startScan();
      if (mounted) {
        setState(() {
          _isScanning = true;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to start scan: $e';
          _isScanning = false;
        });
      }
    }
  }

  /// Stop the active BLE scan
  Future<void> _stopScan() async {
    try {
      await qring.QringSdkFlutter.stopScan();
      if (mounted) {
        setState(() {
          _isScanning = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to stop scan: $e';
        });
      }
    }
  }

  /// Connect to a QRing device by MAC address
  /// Requests permissions first if not already granted
  Future<void> _connectToDevice(String macAddress) async {
    if (!await _requestPermissions()) {
      return;
    }

    setState(() {
      _errorMessage = null;
      _connectingDeviceMac = macAddress; // Set connecting state
    });

    try {
      await qring.QringSdkFlutter.connect(macAddress);
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to connect: $e';
          _connectingDeviceMac = null; // Clear connecting state on error
        });
      }
    }
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

  IconData _getConnectionStateIcon() {
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

  Widget _buildConnectionStatusCard() {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              AnimatedSwitcher(
                duration: const Duration(milliseconds: 300),
                child: Icon(
                  _getConnectionStateIcon(),
                  key: ValueKey(_connectionState),
                  color: _getConnectionStateColor(),
                  size: 32,
                ),
              ),
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
                    AnimatedDefaultTextStyle(
                      duration: const Duration(milliseconds: 300),
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: _getConnectionStateColor(),
                      ),
                      child: Text(_getConnectionStateText()),
                    ),
                  ],
                ),
              ),
              if (_connectionState == qring.ConnectionState.connected)
                ElevatedButton.icon(
                  onPressed: _disconnect,
                  icon: const Icon(Icons.link_off, size: 18),
                  label: const Text('Disconnect'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildScanControls() {
    return Row(
      children: [
        Expanded(
          child: Tooltip(
            message: 'Start scanning for nearby QRing devices',
            child: ElevatedButton.icon(
              onPressed: _isScanning ? null : _startScan,
              icon: const Icon(Icons.search),
              label: const Text('Start Scan'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Tooltip(
            message: 'Stop the current scan',
            child: ElevatedButton.icon(
              onPressed: _isScanning ? _stopScan : null,
              icon: const Icon(Icons.stop),
              label: const Text('Stop Scan'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.orange,
                foregroundColor: Colors.white,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDeviceList() {
    if (_isScanning && _devices.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Scanning for QRing devices...'),
            SizedBox(height: 8),
            Text(
              'Only QRing-compatible devices will appear',
              style: TextStyle(fontSize: 12, color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    if (_devices.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.bluetooth_searching, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'No QRing devices found',
              style: TextStyle(fontSize: 16, color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              'Tap "Start Scan" to search for QRing devices',
              style: TextStyle(fontSize: 12, color: Colors.grey[500]),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 4),
            Text(
              'Make sure your ring is nearby and powered on',
              style: TextStyle(fontSize: 12, color: Colors.grey[500]),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      itemCount: _devices.length,
      itemBuilder: (context, index) {
        final device = _devices[index];
        final isConnecting = _connectingDeviceMac == device.macAddress;
        final isAnyDeviceConnecting = _connectingDeviceMac != null;

        return AnimatedContainer(
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeInOut,
          child: Card(
            margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            child: ListTile(
              leading: Icon(
                Icons.watch,
                size: 40,
                color: Theme.of(context).colorScheme.primary,
              ),
              title: Text(
                device.name.isEmpty ? 'Unknown QRing Device' : device.name,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      const Icon(
                        Icons.fingerprint,
                        size: 14,
                        color: Colors.grey,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        device.macAddress,
                        style: const TextStyle(
                          fontSize: 12,
                          fontFamily: 'monospace',
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(
                        _getSignalIcon(device.rssi),
                        size: 16,
                        color: _getSignalColor(device.rssi),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        'Signal: ${device.rssi} dBm',
                        style: TextStyle(
                          fontSize: 12,
                          color: _getSignalColor(device.rssi),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              trailing: Tooltip(
                message: isConnecting
                    ? 'Connecting to device...'
                    : 'Connect to this device',
                child: ElevatedButton(
                  onPressed:
                      (_connectionState == qring.ConnectionState.disconnected &&
                          !isAnyDeviceConnecting)
                      ? () => _connectToDevice(device.macAddress)
                      : null,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green,
                    foregroundColor: Colors.white,
                  ),
                  child: isConnecting
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor: AlwaysStoppedAnimation<Color>(
                              Colors.white,
                            ),
                          ),
                        )
                      : const Text('Connect'),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  IconData _getSignalIcon(int rssi) {
    if (rssi >= -60) return Icons.signal_cellular_4_bar;
    if (rssi >= -70) return Icons.signal_cellular_alt;
    if (rssi >= -80) return Icons.signal_cellular_alt_2_bar;
    return Icons.signal_cellular_alt_1_bar;
  }

  Color _getSignalColor(int rssi) {
    if (rssi >= -60) return Colors.green;
    if (rssi >= -70) return Colors.orange;
    return Colors.red;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('QRing Device Scanner'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          // Permissions button
          IconButton(
            icon: const Icon(Icons.security),
            tooltip: 'Permissions',
            onPressed: () {
              Navigator.pushNamed(context, '/permissions');
            },
          ),
          // Connection Manager button
          IconButton(
            icon: const Icon(Icons.settings_remote),
            tooltip: 'Connection Manager',
            onPressed: () {
              // Navigate to connection manager if we have a device
              if (_connectingDeviceMac != null) {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => ConnectionManagerScreen(
                      deviceMac: _connectingDeviceMac!,
                      deviceName: _devices
                          .firstWhere(
                            (d) => d.macAddress == _connectingDeviceMac,
                            orElse: () => qring.QringDevice(
                              name: 'Unknown',
                              macAddress: _connectingDeviceMac!,
                              rssi: 0,
                            ),
                          )
                          .name,
                    ),
                  ),
                );
              } else {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Please connect to a device first'),
                  ),
                );
              }
            },
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildConnectionStatusCard(),
            const SizedBox(height: 16),
            _buildScanControls(),
            if (_errorMessage != null) ...[
              const SizedBox(height: 16),
              ErrorCard(
                message: _errorMessage!,
                onRetry: () {
                  setState(() {
                    _errorMessage = null;
                  });
                  _startScan();
                },
                onDismiss: () {
                  setState(() {
                    _errorMessage = null;
                  });
                },
              ),
            ],
            const SizedBox(height: 16),
            const Text(
              'Discovered Devices',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Expanded(child: _buildDeviceList()),
          ],
        ),
      ),
    );
  }
}
