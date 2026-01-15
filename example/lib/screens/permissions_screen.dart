import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

/// Screen to display and request required permissions
///
/// Shows all required permissions with their status and provides
/// buttons to request permissions or open app settings
class PermissionsScreen extends StatefulWidget {
  const PermissionsScreen({super.key});

  @override
  State<PermissionsScreen> createState() => _PermissionsScreenState();
}

class _PermissionsScreenState extends State<PermissionsScreen> {
  Map<Permission, PermissionStatus> _permissionStatuses = {};
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  /// Check status of all required permissions
  Future<void> _checkPermissions() async {
    setState(() {
      _isLoading = true;
    });

    final permissions = [
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.location,
      Permission.notification,
    ];

    final statuses = <Permission, PermissionStatus>{};
    for (final permission in permissions) {
      statuses[permission] = await permission.status;
    }

    setState(() {
      _permissionStatuses = statuses;
      _isLoading = false;
    });
  }

  /// Request a specific permission
  Future<void> _requestPermission(Permission permission) async {
    final status = await permission.request();
    setState(() {
      _permissionStatuses[permission] = status;
    });

    if (status.isPermanentlyDenied) {
      _showSettingsDialog();
    }
  }

  /// Request all permissions
  Future<void> _requestAllPermissions() async {
    setState(() {
      _isLoading = true;
    });

    final permissions = _permissionStatuses.keys.toList();
    final statuses = await permissions.request();

    setState(() {
      _permissionStatuses = statuses;
      _isLoading = false;
    });

    final anyPermanentlyDenied = statuses.values.any(
      (status) => status.isPermanentlyDenied,
    );
    if (anyPermanentlyDenied) {
      _showSettingsDialog();
    }
  }

  /// Show dialog to open app settings
  void _showSettingsDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Permission Required'),
        content: const Text(
          'Some permissions have been permanently denied. '
          'Please enable them in Settings to use all features.',
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

  /// Get permission name
  String _getPermissionName(Permission permission) {
    if (permission == Permission.bluetoothScan) {
      return 'Bluetooth Scan';
    } else if (permission == Permission.bluetoothConnect) {
      return 'Bluetooth Connect';
    } else if (permission == Permission.location) {
      return 'Location';
    } else if (permission == Permission.notification) {
      return 'Notifications';
    }
    return permission.toString();
  }

  /// Get permission description
  String _getPermissionDescription(Permission permission) {
    if (permission == Permission.bluetoothScan) {
      return 'Required to scan for nearby QRing devices';
    } else if (permission == Permission.bluetoothConnect) {
      return 'Required to connect to your QRing device';
    } else if (permission == Permission.location) {
      return 'Required for Bluetooth scanning on Android';
    } else if (permission == Permission.notification) {
      return 'Required for background service notifications';
    }
    return 'Required for app functionality';
  }

  /// Get icon for permission
  IconData _getPermissionIcon(Permission permission) {
    if (permission == Permission.bluetoothScan ||
        permission == Permission.bluetoothConnect) {
      return Icons.bluetooth;
    } else if (permission == Permission.location) {
      return Icons.location_on;
    } else if (permission == Permission.notification) {
      return Icons.notifications;
    }
    return Icons.security;
  }

  /// Get color for permission status
  Color _getStatusColor(PermissionStatus status) {
    if (status.isGranted) {
      return Colors.green;
    } else if (status.isDenied) {
      return Colors.orange;
    } else if (status.isPermanentlyDenied) {
      return Colors.red;
    } else if (status.isRestricted) {
      return Colors.grey;
    } else if (status.isLimited) {
      return Colors.amber;
    }
    return Colors.grey;
  }

  /// Get status text
  String _getStatusText(PermissionStatus status) {
    if (status.isGranted) {
      return 'Granted';
    } else if (status.isDenied) {
      return 'Denied';
    } else if (status.isPermanentlyDenied) {
      return 'Permanently Denied';
    } else if (status.isRestricted) {
      return 'Restricted';
    } else if (status.isLimited) {
      return 'Limited';
    }
    return 'Unknown';
  }

  /// Build permission card
  Widget _buildPermissionCard(Permission permission, PermissionStatus status) {
    return Card(
      child: ListTile(
        leading: Icon(
          _getPermissionIcon(permission),
          size: 32,
          color: Theme.of(context).colorScheme.primary,
        ),
        title: Text(
          _getPermissionName(permission),
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Text(
              _getPermissionDescription(permission),
              style: const TextStyle(fontSize: 12),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(
                  status.isGranted ? Icons.check_circle : Icons.cancel,
                  size: 16,
                  color: _getStatusColor(status),
                ),
                const SizedBox(width: 4),
                Text(
                  _getStatusText(status),
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: _getStatusColor(status),
                  ),
                ),
              ],
            ),
          ],
        ),
        trailing: !status.isGranted
            ? ElevatedButton(
                onPressed: () => _requestPermission(permission),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue,
                  foregroundColor: Colors.white,
                ),
                child: const Text('Grant'),
              )
            : null,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final allGranted = _permissionStatuses.values.every(
      (status) => status.isGranted,
    );

    return Scaffold(
      appBar: AppBar(
        title: const Text('Permissions'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Card(
                    color: allGranted
                        ? Colors.green.shade50
                        : Colors.orange.shade50,
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Row(
                        children: [
                          Icon(
                            allGranted ? Icons.check_circle : Icons.warning,
                            color: allGranted ? Colors.green : Colors.orange,
                            size: 32,
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  allGranted
                                      ? 'All Permissions Granted'
                                      : 'Permissions Required',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                    color: allGranted
                                        ? Colors.green
                                        : Colors.orange,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  allGranted
                                      ? 'You can use all app features'
                                      : 'Grant permissions to use all features',
                                  style: const TextStyle(fontSize: 12),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  ..._permissionStatuses.entries.map(
                    (entry) => Padding(
                      padding: const EdgeInsets.only(bottom: 8.0),
                      child: _buildPermissionCard(entry.key, entry.value),
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (!allGranted)
                    ElevatedButton.icon(
                      onPressed: _requestAllPermissions,
                      icon: const Icon(Icons.check_circle),
                      label: const Text('Grant All Permissions'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.blue,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.all(16),
                      ),
                    ),
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: _checkPermissions,
                    icon: const Icon(Icons.refresh),
                    label: const Text('Refresh Status'),
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: openAppSettings,
                    icon: const Icon(Icons.settings),
                    label: const Text('Open App Settings'),
                  ),
                ],
              ),
            ),
    );
  }
}
