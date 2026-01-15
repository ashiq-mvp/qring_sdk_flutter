import 'package:flutter/material.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart' as qring;

/// Widget to display BLE errors in a user-friendly banner
///
/// Shows error type, message, and provides action buttons based on error type
class BleErrorBanner extends StatelessWidget {
  final qring.BleError error;
  final VoidCallback? onDismiss;
  final VoidCallback? onAction;

  const BleErrorBanner({
    super.key,
    required this.error,
    this.onDismiss,
    this.onAction,
  });

  /// Get icon for error type
  IconData _getErrorIcon() {
    switch (error.type) {
      case qring.BleErrorType.permissionDenied:
        return Icons.lock;
      case qring.BleErrorType.bluetoothOff:
        return Icons.bluetooth_disabled;
      case qring.BleErrorType.deviceNotFound:
        return Icons.search_off;
      case qring.BleErrorType.pairingFailed:
        return Icons.link_off;
      case qring.BleErrorType.connectionFailed:
      case qring.BleErrorType.connectionTimeout:
        return Icons.signal_wifi_off;
      case qring.BleErrorType.gattError:
        return Icons.error_outline;
      case qring.BleErrorType.commandFailed:
        return Icons.warning;
      case qring.BleErrorType.reconnectionFailed:
        return Icons.refresh;
      case qring.BleErrorType.unknown:
        return Icons.help_outline;
    }
  }

  /// Get action button text based on error type
  String? _getActionText() {
    switch (error.type) {
      case qring.BleErrorType.permissionDenied:
        return 'Grant Permissions';
      case qring.BleErrorType.bluetoothOff:
        return 'Enable Bluetooth';
      case qring.BleErrorType.deviceNotFound:
        return 'Retry Scan';
      case qring.BleErrorType.pairingFailed:
      case qring.BleErrorType.connectionFailed:
      case qring.BleErrorType.connectionTimeout:
        return 'Retry Connection';
      case qring.BleErrorType.reconnectionFailed:
        return 'Reconnect';
      default:
        return null;
    }
  }

  /// Get user-friendly suggestion based on error type
  String _getSuggestion() {
    switch (error.type) {
      case qring.BleErrorType.permissionDenied:
        return 'Please grant Bluetooth and Location permissions in Settings.';
      case qring.BleErrorType.bluetoothOff:
        return 'Please enable Bluetooth on your device.';
      case qring.BleErrorType.deviceNotFound:
        return 'Make sure your ring is nearby and powered on.';
      case qring.BleErrorType.pairingFailed:
        return 'Failed to pair with device. Please try again.';
      case qring.BleErrorType.connectionFailed:
        return 'Connection failed. The device will automatically retry.';
      case qring.BleErrorType.connectionTimeout:
        return 'Connection timed out. Make sure device is in range.';
      case qring.BleErrorType.gattError:
        return 'Communication error occurred. Connection will retry.';
      case qring.BleErrorType.commandFailed:
        return 'Command failed. Please ensure device is connected.';
      case qring.BleErrorType.reconnectionFailed:
        return 'Automatic reconnection failed. Will keep trying.';
      case qring.BleErrorType.unknown:
        return 'An unexpected error occurred.';
    }
  }

  @override
  Widget build(BuildContext context) {
    final actionText = _getActionText();

    return Material(
      color: Colors.red.shade50,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Icon(_getErrorIcon(), color: Colors.red, size: 28),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        error.type.description,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.red,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        error.message,
                        style: const TextStyle(
                          fontSize: 13,
                          color: Colors.black87,
                        ),
                      ),
                    ],
                  ),
                ),
                if (onDismiss != null)
                  IconButton(
                    icon: const Icon(Icons.close, size: 20),
                    onPressed: onDismiss,
                    color: Colors.red,
                  ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              _getSuggestion(),
              style: const TextStyle(
                fontSize: 12,
                color: Colors.black54,
                fontStyle: FontStyle.italic,
              ),
            ),
            if (actionText != null && onAction != null) ...[
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: onAction,
                  icon: const Icon(Icons.refresh, size: 18),
                  label: Text(actionText),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 4),
            Text(
              'Error Code: ${error.code}',
              style: const TextStyle(fontSize: 10, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
}
