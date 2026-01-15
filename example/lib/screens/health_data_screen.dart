import 'dart:async';

import 'package:flutter/material.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart' as qring;

import '../widgets/error_card.dart';

/// Screen for health data management
///
/// Provides two main tabs:
/// 1. Manual Measurements - Take real-time measurements (HR, BP, SpO2, Temp)
/// 2. Data Sync - Synchronize historical health data from the ring
class HealthDataScreen extends StatefulWidget {
  const HealthDataScreen({super.key});

  @override
  State<HealthDataScreen> createState() => _HealthDataScreenState();
}

class _HealthDataScreenState extends State<HealthDataScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  StreamSubscription<qring.HealthMeasurement>? _measurementSubscription;

  // Manual measurement state
  bool _isMeasuring = false;
  qring.MeasurementType? _currentMeasurementType;
  qring.HealthMeasurement? _latestMeasurement;
  final List<qring.HealthMeasurement> _measurementHistory = [];
  String? _measurementError;

  // Data sync state
  int _selectedDayOffset = 0;
  bool _isSyncing = false;
  String? _syncError;
  qring.StepData? _stepData;
  List<qring.HeartRateData> _heartRateData = [];
  qring.SleepData? _sleepData;
  List<qring.BloodOxygenData> _bloodOxygenData = [];
  List<qring.BloodPressureData> _bloodPressureData = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _setupMeasurementListener();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _measurementSubscription?.cancel();
    super.dispose();
  }

  /// Set up listener for measurement results stream
  void _setupMeasurementListener() {
    _measurementSubscription = qring.QringHealthData.measurementStream.listen(
      (measurement) {
        if (mounted) {
          setState(() {
            _latestMeasurement = measurement;
            _measurementHistory.insert(0, measurement);
            // Keep only last 10 measurements
            if (_measurementHistory.length > 10) {
              _measurementHistory.removeLast();
            }
            if (!measurement.success) {
              _measurementError =
                  measurement.errorMessage ?? 'Measurement failed';
            } else {
              _measurementError = null;
            }
            _isMeasuring = false;
            _currentMeasurementType = null;
          });
        }
      },
      onError: (error) {
        if (mounted) {
          setState(() {
            _measurementError = error.toString();
            _isMeasuring = false;
            _currentMeasurementType = null;
          });
        }
      },
    );
  }

  /// Start a manual measurement of the specified type
  /// Results will be received through the measurement stream
  Future<void> _startMeasurement(qring.MeasurementType type) async {
    setState(() {
      _isMeasuring = true;
      _currentMeasurementType = type;
      _measurementError = null;
      _latestMeasurement = null;
    });

    try {
      switch (type) {
        case qring.MeasurementType.heartRate:
          await qring.QringHealthData.startHeartRateMeasurement();
          break;
        case qring.MeasurementType.bloodPressure:
          await qring.QringHealthData.startBloodPressureMeasurement();
          break;
        case qring.MeasurementType.bloodOxygen:
          await qring.QringHealthData.startBloodOxygenMeasurement();
          break;
        case qring.MeasurementType.temperature:
          await qring.QringHealthData.startTemperatureMeasurement();
          break;
        default:
          throw Exception('Unsupported measurement type');
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _measurementError = e.toString();
          _isMeasuring = false;
          _currentMeasurementType = null;
        });
      }
    }
  }

  Future<void> _stopMeasurement() async {
    try {
      await qring.QringHealthData.stopMeasurement();
      if (mounted) {
        setState(() {
          _isMeasuring = false;
          _currentMeasurementType = null;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _measurementError = e.toString();
        });
      }
    }
  }

  Widget _buildManualMeasurementTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_measurementError != null) ...[
            ErrorCard(
              message: _measurementError!,
              onDismiss: () {
                setState(() {
                  _measurementError = null;
                });
              },
            ),
            const SizedBox(height: 16),
          ],
          _buildMeasurementButtons(),
          const SizedBox(height: 24),
          if (_isMeasuring) _buildMeasuringIndicator(),
          if (_latestMeasurement != null && !_isMeasuring)
            _buildLatestMeasurement(),
          const SizedBox(height: 24),
          _buildMeasurementHistory(),
        ],
      ),
    );
  }

  Widget _buildMeasurementButtons() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Manual Measurements',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            GridView.count(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisCount: 2,
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.2,
              children: [
                _buildMeasurementButton(
                  'Heart Rate',
                  Icons.favorite,
                  Colors.red,
                  qring.MeasurementType.heartRate,
                ),
                _buildMeasurementButton(
                  'Blood Pressure',
                  Icons.monitor_heart,
                  Colors.purple,
                  qring.MeasurementType.bloodPressure,
                ),
                _buildMeasurementButton(
                  'Blood Oxygen',
                  Icons.air,
                  Colors.blue,
                  qring.MeasurementType.bloodOxygen,
                ),
                _buildMeasurementButton(
                  'Temperature',
                  Icons.thermostat,
                  Colors.orange,
                  qring.MeasurementType.temperature,
                ),
              ],
            ),
            if (_isMeasuring) ...[
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: _stopMeasurement,
                  icon: const Icon(Icons.stop),
                  label: const Text('Stop Measurement'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildMeasurementButton(
    String label,
    IconData icon,
    Color color,
    qring.MeasurementType type,
  ) {
    final isActive = _isMeasuring && _currentMeasurementType == type;
    final isDisabled = _isMeasuring && _currentMeasurementType != type;

    return ElevatedButton(
      onPressed: isDisabled ? null : () => _startMeasurement(type),
      style: ElevatedButton.styleFrom(
        backgroundColor: isActive ? color.withOpacity(0.2) : Colors.white,
        foregroundColor: color,
        elevation: isActive ? 0 : 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(
            color: isActive ? color : Colors.transparent,
            width: 2,
          ),
        ),
        padding: const EdgeInsets.all(16),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 40),
          const SizedBox(height: 8),
          Text(
            label,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }

  Widget _buildMeasuringIndicator() {
    return Card(
      color: Theme.of(context).colorScheme.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            const SizedBox(
              width: 60,
              height: 60,
              child: CircularProgressIndicator(strokeWidth: 6),
            ),
            const SizedBox(height: 16),
            Text(
              'Measuring ${_getMeasurementTypeName(_currentMeasurementType!)}...',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Please wait while the ring takes the measurement',
              style: TextStyle(color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLatestMeasurement() {
    final measurement = _latestMeasurement!;
    return Card(
      color: measurement.success ? Colors.green.shade50 : Colors.red.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  measurement.success ? Icons.check_circle : Icons.error,
                  color: measurement.success ? Colors.green : Colors.red,
                ),
                const SizedBox(width: 12),
                Text(
                  measurement.success
                      ? 'Measurement Complete'
                      : 'Measurement Failed',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: measurement.success ? Colors.green : Colors.red,
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            _buildMeasurementDetails(measurement),
          ],
        ),
      ),
    );
  }

  Widget _buildMeasurementDetails(qring.HealthMeasurement measurement) {
    switch (measurement.type) {
      case qring.MeasurementType.heartRate:
        return _buildDetailRow(
          'Heart Rate',
          '${measurement.heartRate ?? '--'} bpm',
          Icons.favorite,
        );
      case qring.MeasurementType.bloodPressure:
        return Column(
          children: [
            _buildDetailRow(
              'Systolic',
              '${measurement.systolic ?? '--'} mmHg',
              Icons.arrow_upward,
            ),
            const SizedBox(height: 8),
            _buildDetailRow(
              'Diastolic',
              '${measurement.diastolic ?? '--'} mmHg',
              Icons.arrow_downward,
            ),
          ],
        );
      case qring.MeasurementType.bloodOxygen:
        return _buildDetailRow(
          'Blood Oxygen',
          '${measurement.spO2 ?? '--'}%',
          Icons.air,
        );
      case qring.MeasurementType.temperature:
        return _buildDetailRow(
          'Temperature',
          '${measurement.temperature?.toStringAsFixed(1) ?? '--'}°C',
          Icons.thermostat,
        );
      default:
        return const Text('Unknown measurement type');
    }
  }

  Widget _buildDetailRow(String label, String value, IconData icon) {
    return Row(
      children: [
        Icon(icon, size: 24, color: Colors.grey[600]),
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
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMeasurementHistory() {
    if (_measurementHistory.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            children: [
              Icon(Icons.history, size: 48, color: Colors.grey[400]),
              const SizedBox(height: 12),
              Text(
                'No measurement history',
                style: TextStyle(color: Colors.grey[600]),
              ),
              const SizedBox(height: 4),
              Text(
                'Start a measurement to see results here',
                style: TextStyle(fontSize: 12, color: Colors.grey[500]),
              ),
            ],
          ),
        ),
      );
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Recent Measurements',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _measurementHistory.length,
              separatorBuilder: (context, index) => const Divider(height: 24),
              itemBuilder: (context, index) {
                final measurement = _measurementHistory[index];
                return _buildHistoryItem(measurement);
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHistoryItem(qring.HealthMeasurement measurement) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: _getMeasurementColor(measurement.type).withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(
            _getMeasurementIcon(measurement.type),
            color: _getMeasurementColor(measurement.type),
            size: 24,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                _getMeasurementTypeName(measurement.type),
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                _formatTimestamp(measurement.timestamp),
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ],
          ),
        ),
        Text(
          _getMeasurementValue(measurement),
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
      ],
    );
  }

  String _getMeasurementTypeName(qring.MeasurementType type) {
    switch (type) {
      case qring.MeasurementType.heartRate:
        return 'Heart Rate';
      case qring.MeasurementType.bloodPressure:
        return 'Blood Pressure';
      case qring.MeasurementType.bloodOxygen:
        return 'Blood Oxygen';
      case qring.MeasurementType.temperature:
        return 'Temperature';
      default:
        return type.name;
    }
  }

  IconData _getMeasurementIcon(qring.MeasurementType type) {
    switch (type) {
      case qring.MeasurementType.heartRate:
        return Icons.favorite;
      case qring.MeasurementType.bloodPressure:
        return Icons.monitor_heart;
      case qring.MeasurementType.bloodOxygen:
        return Icons.air;
      case qring.MeasurementType.temperature:
        return Icons.thermostat;
      default:
        return Icons.health_and_safety;
    }
  }

  Color _getMeasurementColor(qring.MeasurementType type) {
    switch (type) {
      case qring.MeasurementType.heartRate:
        return Colors.red;
      case qring.MeasurementType.bloodPressure:
        return Colors.purple;
      case qring.MeasurementType.bloodOxygen:
        return Colors.blue;
      case qring.MeasurementType.temperature:
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  String _getMeasurementValue(qring.HealthMeasurement measurement) {
    if (!measurement.success) return 'Failed';

    switch (measurement.type) {
      case qring.MeasurementType.heartRate:
        return '${measurement.heartRate ?? '--'} bpm';
      case qring.MeasurementType.bloodPressure:
        return '${measurement.systolic ?? '--'}/${measurement.diastolic ?? '--'}';
      case qring.MeasurementType.bloodOxygen:
        return '${measurement.spO2 ?? '--'}%';
      case qring.MeasurementType.temperature:
        return '${measurement.temperature?.toStringAsFixed(1) ?? '--'}°C';
      default:
        return '--';
    }
  }

  String _formatTimestamp(DateTime timestamp) {
    final now = DateTime.now();
    final difference = now.difference(timestamp);

    if (difference.inMinutes < 1) {
      return 'Just now';
    } else if (difference.inMinutes < 60) {
      return '${difference.inMinutes}m ago';
    } else if (difference.inHours < 24) {
      return '${difference.inHours}h ago';
    } else {
      return '${timestamp.day}/${timestamp.month} ${timestamp.hour}:${timestamp.minute.toString().padLeft(2, '0')}';
    }
  }

  Widget _buildDataSyncTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_syncError != null) ...[
            ErrorCard(
              message: _syncError!,
              onDismiss: () {
                setState(() {
                  _syncError = null;
                });
              },
            ),
            const SizedBox(height: 16),
          ],
          _buildDaySelector(),
          const SizedBox(height: 16),
          _buildSyncButtons(),
          const SizedBox(height: 24),
          if (_stepData != null) _buildStepDataCard(),
          if (_heartRateData.isNotEmpty) ...[
            const SizedBox(height: 16),
            _buildHeartRateDataCard(),
          ],
          if (_sleepData != null) ...[
            const SizedBox(height: 16),
            _buildSleepDataCard(),
          ],
          if (_bloodOxygenData.isNotEmpty) ...[
            const SizedBox(height: 16),
            _buildBloodOxygenDataCard(),
          ],
          if (_bloodPressureData.isNotEmpty) ...[
            const SizedBox(height: 16),
            _buildBloodPressureDataCard(),
          ],
        ],
      ),
    );
  }

  Widget _buildDaySelector() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Select Date',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: List.generate(7, (index) {
                  final date = DateTime.now().subtract(Duration(days: index));
                  final isSelected = _selectedDayOffset == index;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: ChoiceChip(
                      label: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            _getDayLabel(index),
                            style: const TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Text(
                            '${date.day}/${date.month}',
                            style: const TextStyle(fontSize: 10),
                          ),
                        ],
                      ),
                      selected: isSelected,
                      onSelected: (selected) {
                        if (selected) {
                          setState(() {
                            _selectedDayOffset = index;
                            // Clear previous data when changing date
                            _stepData = null;
                            _heartRateData = [];
                            _sleepData = null;
                            _bloodOxygenData = [];
                            _bloodPressureData = [];
                          });
                        }
                      },
                    ),
                  );
                }),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _getDayLabel(int offset) {
    switch (offset) {
      case 0:
        return 'Today';
      case 1:
        return 'Yesterday';
      default:
        return '${offset}d ago';
    }
  }

  Widget _buildSyncButtons() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Synchronize Data',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildSyncButton(
                  'Steps',
                  Icons.directions_walk,
                  Colors.green,
                  () => _syncStepData(),
                ),
                _buildSyncButton(
                  'Heart Rate',
                  Icons.favorite,
                  Colors.red,
                  () => _syncHeartRateData(),
                ),
                _buildSyncButton(
                  'Sleep',
                  Icons.bedtime,
                  Colors.indigo,
                  () => _syncSleepData(),
                ),
                _buildSyncButton(
                  'Blood Oxygen',
                  Icons.air,
                  Colors.blue,
                  () => _syncBloodOxygenData(),
                ),
                _buildSyncButton(
                  'Blood Pressure',
                  Icons.monitor_heart,
                  Colors.purple,
                  () => _syncBloodPressureData(),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSyncButton(
    String label,
    IconData icon,
    Color color,
    VoidCallback onPressed,
  ) {
    return ElevatedButton.icon(
      onPressed: _isSyncing ? null : onPressed,
      icon: Icon(icon, size: 18),
      label: Text(label),
      style: ElevatedButton.styleFrom(
        backgroundColor: color.withOpacity(0.1),
        foregroundColor: color,
        elevation: 0,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      ),
    );
  }

  Future<void> _syncStepData() async {
    setState(() {
      _isSyncing = true;
      _syncError = null;
    });

    try {
      final data = await qring.QringHealthData.syncStepData(_selectedDayOffset);
      if (mounted) {
        setState(() {
          _stepData = data;
          _isSyncing = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _syncError = 'Failed to sync step data: $e';
          _isSyncing = false;
        });
      }
    }
  }

  Future<void> _syncHeartRateData() async {
    setState(() {
      _isSyncing = true;
      _syncError = null;
    });

    try {
      final data = await qring.QringHealthData.syncHeartRateData(
        _selectedDayOffset,
      );
      if (mounted) {
        setState(() {
          _heartRateData = data;
          _isSyncing = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _syncError = 'Failed to sync heart rate data: $e';
          _isSyncing = false;
        });
      }
    }
  }

  Future<void> _syncSleepData() async {
    setState(() {
      _isSyncing = true;
      _syncError = null;
    });

    try {
      final data = await qring.QringHealthData.syncSleepData(
        _selectedDayOffset,
      );
      if (mounted) {
        setState(() {
          _sleepData = data;
          _isSyncing = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _syncError = 'Failed to sync sleep data: $e';
          _isSyncing = false;
        });
      }
    }
  }

  Future<void> _syncBloodOxygenData() async {
    setState(() {
      _isSyncing = true;
      _syncError = null;
    });

    try {
      final data = await qring.QringHealthData.syncBloodOxygenData(
        _selectedDayOffset,
      );
      if (mounted) {
        setState(() {
          _bloodOxygenData = data;
          _isSyncing = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _syncError = 'Failed to sync blood oxygen data: $e';
          _isSyncing = false;
        });
      }
    }
  }

  Future<void> _syncBloodPressureData() async {
    setState(() {
      _isSyncing = true;
      _syncError = null;
    });

    try {
      final data = await qring.QringHealthData.syncBloodPressureData(
        _selectedDayOffset,
      );
      if (mounted) {
        setState(() {
          _bloodPressureData = data;
          _isSyncing = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _syncError = 'Failed to sync blood pressure data: $e';
          _isSyncing = false;
        });
      }
    }
  }

  Widget _buildStepDataCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.directions_walk, color: Colors.green),
                const SizedBox(width: 12),
                const Text(
                  'Step Data',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(height: 24),
            _buildDataRow('Total Steps', '${_stepData!.totalSteps}'),
            const SizedBox(height: 8),
            _buildDataRow('Running Steps', '${_stepData!.runningSteps}'),
            const SizedBox(height: 8),
            _buildDataRow('Distance', '${_stepData!.distanceMeters} m'),
            const SizedBox(height: 8),
            _buildDataRow('Calories', '${_stepData!.calories} kcal'),
            const SizedBox(height: 8),
            _buildDataRow(
              'Sport Duration',
              '${(_stepData!.sportDurationSeconds / 60).toStringAsFixed(0)} min',
            ),
            const SizedBox(height: 8),
            _buildDataRow(
              'Sleep Duration',
              '${(_stepData!.sleepDurationSeconds / 60).toStringAsFixed(0)} min',
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeartRateDataCard() {
    final avgHr = _heartRateData.isEmpty
        ? 0
        : _heartRateData.map((e) => e.heartRate).reduce((a, b) => a + b) ~/
              _heartRateData.length;
    final maxHr = _heartRateData.isEmpty
        ? 0
        : _heartRateData
              .map((e) => e.heartRate)
              .reduce((a, b) => a > b ? a : b);
    final minHr = _heartRateData.isEmpty
        ? 0
        : _heartRateData
              .map((e) => e.heartRate)
              .reduce((a, b) => a < b ? a : b);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.favorite, color: Colors.red),
                const SizedBox(width: 12),
                const Text(
                  'Heart Rate Data',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildStatColumn('Average', '$avgHr bpm', Colors.red),
                _buildStatColumn('Max', '$maxHr bpm', Colors.orange),
                _buildStatColumn('Min', '$minHr bpm', Colors.blue),
              ],
            ),
            const SizedBox(height: 16),
            const Text(
              'Measurements',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 100,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: _heartRateData.length,
                itemBuilder: (context, index) {
                  final data = _heartRateData[index];
                  return Container(
                    width: 80,
                    margin: const EdgeInsets.only(right: 8),
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.red.shade50,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          '${data.heartRate}',
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: Colors.red,
                          ),
                        ),
                        const Text(
                          'bpm',
                          style: TextStyle(fontSize: 10, color: Colors.grey),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${data.timestamp.hour}:${data.timestamp.minute.toString().padLeft(2, '0')}',
                          style: const TextStyle(
                            fontSize: 10,
                            color: Colors.grey,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSleepDataCard() {
    final totalMinutes = _sleepData!.details.fold<int>(
      0,
      (sum, detail) => sum + detail.durationMinutes,
    );
    final deepSleepMinutes = _sleepData!.details
        .where((d) => d.stage == qring.SleepStage.deepSleep)
        .fold<int>(0, (sum, detail) => sum + detail.durationMinutes);
    final lightSleepMinutes = _sleepData!.details
        .where((d) => d.stage == qring.SleepStage.lightSleep)
        .fold<int>(0, (sum, detail) => sum + detail.durationMinutes);
    final remMinutes = _sleepData!.details
        .where((d) => d.stage == qring.SleepStage.rem)
        .fold<int>(0, (sum, detail) => sum + detail.durationMinutes);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.bedtime, color: Colors.indigo),
                const SizedBox(width: 12),
                const Text(
                  'Sleep Data',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(height: 24),
            _buildDataRow(
              'Total Sleep',
              '${(totalMinutes / 60).toStringAsFixed(1)} hours',
            ),
            const SizedBox(height: 8),
            _buildDataRow(
              'Sleep Time',
              '${_sleepData!.startTime.hour}:${_sleepData!.startTime.minute.toString().padLeft(2, '0')} - '
                  '${_sleepData!.endTime.hour}:${_sleepData!.endTime.minute.toString().padLeft(2, '0')}',
            ),
            const SizedBox(height: 16),
            const Text(
              'Sleep Stages',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            _buildSleepStageBar(
              'Deep Sleep',
              deepSleepMinutes,
              totalMinutes,
              Colors.indigo,
            ),
            const SizedBox(height: 4),
            _buildSleepStageBar(
              'Light Sleep',
              lightSleepMinutes,
              totalMinutes,
              Colors.blue,
            ),
            const SizedBox(height: 4),
            _buildSleepStageBar('REM', remMinutes, totalMinutes, Colors.purple),
          ],
        ),
      ),
    );
  }

  Widget _buildSleepStageBar(
    String label,
    int minutes,
    int total,
    Color color,
  ) {
    final percentage = total > 0 ? (minutes / total * 100) : 0.0;
    return Row(
      children: [
        SizedBox(
          width: 80,
          child: Text(label, style: const TextStyle(fontSize: 12)),
        ),
        Expanded(
          child: Stack(
            children: [
              Container(
                height: 24,
                decoration: BoxDecoration(
                  color: Colors.grey.shade200,
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
              FractionallySizedBox(
                widthFactor: percentage / 100,
                child: Container(
                  height: 24,
                  decoration: BoxDecoration(
                    color: color,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ),
              Container(
                height: 24,
                alignment: Alignment.centerRight,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: Text(
                  '${minutes}m (${percentage.toStringAsFixed(0)}%)',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w600,
                    color: percentage > 30 ? Colors.white : Colors.black,
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildBloodOxygenDataCard() {
    final avgSpO2 = _bloodOxygenData.isEmpty
        ? 0
        : _bloodOxygenData.map((e) => e.spO2).reduce((a, b) => a + b) ~/
              _bloodOxygenData.length;
    final maxSpO2 = _bloodOxygenData.isEmpty
        ? 0
        : _bloodOxygenData.map((e) => e.spO2).reduce((a, b) => a > b ? a : b);
    final minSpO2 = _bloodOxygenData.isEmpty
        ? 0
        : _bloodOxygenData.map((e) => e.spO2).reduce((a, b) => a < b ? a : b);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.air, color: Colors.blue),
                const SizedBox(width: 12),
                const Text(
                  'Blood Oxygen Data',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildStatColumn('Average', '$avgSpO2%', Colors.blue),
                _buildStatColumn('Max', '$maxSpO2%', Colors.green),
                _buildStatColumn('Min', '$minSpO2%', Colors.orange),
              ],
            ),
            const SizedBox(height: 16),
            const Text(
              'Measurements',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 100,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: _bloodOxygenData.length,
                itemBuilder: (context, index) {
                  final data = _bloodOxygenData[index];
                  return Container(
                    width: 80,
                    margin: const EdgeInsets.only(right: 8),
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.blue.shade50,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          '${data.spO2}',
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: Colors.blue,
                          ),
                        ),
                        const Text(
                          '%',
                          style: TextStyle(fontSize: 10, color: Colors.grey),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${data.timestamp.hour}:${data.timestamp.minute.toString().padLeft(2, '0')}',
                          style: const TextStyle(
                            fontSize: 10,
                            color: Colors.grey,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBloodPressureDataCard() {
    final avgSystolic = _bloodPressureData.isEmpty
        ? 0
        : _bloodPressureData.map((e) => e.systolic).reduce((a, b) => a + b) ~/
              _bloodPressureData.length;
    final avgDiastolic = _bloodPressureData.isEmpty
        ? 0
        : _bloodPressureData.map((e) => e.diastolic).reduce((a, b) => a + b) ~/
              _bloodPressureData.length;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.monitor_heart, color: Colors.purple),
                const SizedBox(width: 12),
                const Text(
                  'Blood Pressure Data',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildStatColumn(
                  'Average',
                  '$avgSystolic/$avgDiastolic',
                  Colors.purple,
                ),
                _buildStatColumn(
                  'Measurements',
                  '${_bloodPressureData.length}',
                  Colors.grey,
                ),
              ],
            ),
            const SizedBox(height: 16),
            const Text(
              'Measurements',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 100,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: _bloodPressureData.length,
                itemBuilder: (context, index) {
                  final data = _bloodPressureData[index];
                  return Container(
                    width: 100,
                    margin: const EdgeInsets.only(right: 8),
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.purple.shade50,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          '${data.systolic}/${data.diastolic}',
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: Colors.purple,
                          ),
                        ),
                        const Text(
                          'mmHg',
                          style: TextStyle(fontSize: 10, color: Colors.grey),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${data.timestamp.hour}:${data.timestamp.minute.toString().padLeft(2, '0')}',
                          style: const TextStyle(
                            fontSize: 10,
                            color: Colors.grey,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatColumn(String label, String value, Color color) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
        const SizedBox(height: 4),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }

  Widget _buildDataRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(fontSize: 14, color: Colors.grey)),
        Text(
          value,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'Manual Measurements'),
            Tab(text: 'Data Sync'),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [_buildManualMeasurementTab(), _buildDataSyncTab()],
          ),
        ),
      ],
    );
  }
}
