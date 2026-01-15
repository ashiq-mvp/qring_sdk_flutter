import 'package:flutter/material.dart';

/// Widget to display reconnection status with attempt counter
///
/// Shows a visual indicator when automatic reconnection is in progress
class ReconnectionIndicator extends StatelessWidget {
  final int attemptNumber;
  final VoidCallback? onCancel;

  const ReconnectionIndicator({
    super.key,
    required this.attemptNumber,
    this.onCancel,
  });

  /// Get reconnection message based on attempt number
  String _getMessage() {
    if (attemptNumber <= 5) {
      return 'Reconnecting... (attempt $attemptNumber)';
    } else if (attemptNumber <= 10) {
      return 'Still trying to reconnect... (attempt $attemptNumber)';
    } else {
      return 'Persistent reconnection... (attempt $attemptNumber)';
    }
  }

  /// Get color based on attempt number
  Color _getColor() {
    if (attemptNumber <= 5) {
      return Colors.orange;
    } else if (attemptNumber <= 10) {
      return Colors.amber;
    } else {
      return Colors.red.shade300;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: _getColor().withOpacity(0.1),
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                valueColor: AlwaysStoppedAnimation<Color>(_getColor()),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    _getMessage(),
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _getColor().withOpacity(0.9),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Using exponential backoff strategy',
                    style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
                  ),
                ],
              ),
            ),
            if (onCancel != null)
              TextButton(onPressed: onCancel, child: const Text('Cancel')),
          ],
        ),
      ),
    );
  }
}
