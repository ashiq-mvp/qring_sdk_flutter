import 'dart:async';

import 'package:flutter/material.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart';

import '../widgets/error_card.dart';

/// Screen for exercise tracking
///
/// Provides:
/// - Exercise type selection (20+ types)
/// - Start, pause, resume, stop controls
/// - Real-time exercise metrics (duration, HR, steps, distance, calories)
/// - Exercise summary after completion
class ExerciseScreen extends StatefulWidget {
  const ExerciseScreen({super.key});

  @override
  State<ExerciseScreen> createState() => _ExerciseScreenState();
}

class _ExerciseScreenState extends State<ExerciseScreen> {
  ExerciseType _selectedType = ExerciseType.walking;
  bool _isExerciseActive = false;
  bool _isPaused = false;
  ExerciseData? _currentData;
  ExerciseSummary? _lastSummary;
  StreamSubscription<ExerciseData>? _exerciseSubscription;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _exerciseSubscription?.cancel();
    super.dispose();
  }

  /// Start an exercise session of the selected type
  /// Subscribes to the exercise data stream for real-time updates
  Future<void> _startExercise() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _lastSummary = null;
    });

    try {
      await QringExercise.startExercise(_selectedType);

      // Subscribe to exercise data stream
      _exerciseSubscription = QringExercise.exerciseDataStream.listen(
        (data) {
          if (mounted) {
            setState(() {
              _currentData = data;
            });
          }
        },
        onError: (error) {
          if (mounted) {
            setState(() {
              _errorMessage = 'Stream error: $error';
            });
          }
        },
      );

      setState(() {
        _isExerciseActive = true;
        _isPaused = false;
        _currentData = null;
      });
    } catch (e) {
      setState(() {
        _errorMessage = e.toString();
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _pauseExercise() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await QringExercise.pauseExercise();
      setState(() {
        _isPaused = true;
      });
    } catch (e) {
      setState(() {
        _errorMessage = e.toString();
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _resumeExercise() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await QringExercise.resumeExercise();
      setState(() {
        _isPaused = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = e.toString();
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _stopExercise() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final summary = await QringExercise.stopExercise();
      await _exerciseSubscription?.cancel();
      _exerciseSubscription = null;

      setState(() {
        _isExerciseActive = false;
        _isPaused = false;
        _lastSummary = summary;
        _currentData = null;
      });
    } catch (e) {
      setState(() {
        _errorMessage = e.toString();
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  String _formatDuration(int seconds) {
    final hours = seconds ~/ 3600;
    final minutes = (seconds % 3600) ~/ 60;
    final secs = seconds % 60;

    if (hours > 0) {
      return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}';
    }
    return '${minutes.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}';
  }

  String _formatDistance(int meters) {
    if (meters >= 1000) {
      return '${(meters / 1000).toStringAsFixed(2)} km';
    }
    return '$meters m';
  }

  String _getExerciseTypeName(ExerciseType type) {
    return type.name[0].toUpperCase() + type.name.substring(1);
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Exercise Type Selector
          if (!_isExerciseActive) ...[
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Select Exercise Type',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<ExerciseType>(
                      initialValue: _selectedType,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 8,
                        ),
                      ),
                      items: ExerciseType.values.map((type) {
                        return DropdownMenuItem(
                          value: type,
                          child: Row(
                            children: [
                              Icon(_getExerciseIcon(type), size: 20),
                              const SizedBox(width: 8),
                              Text(_getExerciseTypeName(type)),
                            ],
                          ),
                        );
                      }).toList(),
                      onChanged: (value) {
                        if (value != null) {
                          setState(() {
                            _selectedType = value;
                          });
                        }
                      },
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
          ],

          // Control Buttons
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  if (!_isExerciseActive) ...[
                    ElevatedButton.icon(
                      onPressed: _isLoading ? null : _startExercise,
                      icon: _isLoading
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.play_arrow),
                      label: Text(
                        _isLoading ? 'Starting...' : 'Start Exercise',
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green,
                        foregroundColor: Colors.white,
                        minimumSize: const Size(double.infinity, 48),
                      ),
                    ),
                  ] else ...[
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: _isLoading
                                ? null
                                : (_isPaused
                                      ? _resumeExercise
                                      : _pauseExercise),
                            icon: Icon(
                              _isPaused ? Icons.play_arrow : Icons.pause,
                            ),
                            label: Text(_isPaused ? 'Resume' : 'Pause'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.orange,
                              foregroundColor: Colors.white,
                              minimumSize: const Size(0, 48),
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: _isLoading ? null : _stopExercise,
                            icon: const Icon(Icons.stop),
                            label: const Text('Stop'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.red,
                              foregroundColor: Colors.white,
                              minimumSize: const Size(0, 48),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ),

          // Error Message
          if (_errorMessage != null) ...[
            const SizedBox(height: 16),
            ErrorCard(
              message: _errorMessage!,
              onDismiss: () {
                setState(() {
                  _errorMessage = null;
                });
              },
            ),
          ],

          // Real-time Metrics
          if (_isExerciseActive && _currentData != null) ...[
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          _getExerciseIcon(_selectedType),
                          color: Theme.of(context).colorScheme.primary,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          _getExerciseTypeName(_selectedType),
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        const Spacer(),
                        if (_isPaused)
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.orange.shade100,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              'PAUSED',
                              style: TextStyle(
                                color: Colors.orange.shade700,
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    _buildMetricRow(
                      context,
                      Icons.timer,
                      'Duration',
                      _formatDuration(_currentData!.durationSeconds),
                    ),
                    const Divider(height: 24),
                    _buildMetricRow(
                      context,
                      Icons.favorite,
                      'Heart Rate',
                      '${_currentData!.heartRate} bpm',
                    ),
                    const Divider(height: 24),
                    _buildMetricRow(
                      context,
                      Icons.directions_walk,
                      'Steps',
                      '${_currentData!.steps}',
                    ),
                    const Divider(height: 24),
                    _buildMetricRow(
                      context,
                      Icons.straighten,
                      'Distance',
                      _formatDistance(_currentData!.distanceMeters),
                    ),
                    const Divider(height: 24),
                    _buildMetricRow(
                      context,
                      Icons.local_fire_department,
                      'Calories',
                      '${_currentData!.calories} kcal',
                    ),
                  ],
                ),
              ),
            ),
          ],

          // Exercise Summary
          if (_lastSummary != null) ...[
            const SizedBox(height: 16),
            Card(
              color: Colors.green.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          Icons.check_circle,
                          color: Colors.green.shade700,
                          size: 28,
                        ),
                        const SizedBox(width: 12),
                        Text(
                          'Exercise Complete!',
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(
                                fontWeight: FontWeight.bold,
                                color: Colors.green.shade700,
                              ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    _buildSummaryRow(
                      context,
                      'Total Duration',
                      _formatDuration(_lastSummary!.durationSeconds),
                    ),
                    const SizedBox(height: 8),
                    _buildSummaryRow(
                      context,
                      'Total Steps',
                      '${_lastSummary!.totalSteps}',
                    ),
                    const SizedBox(height: 8),
                    _buildSummaryRow(
                      context,
                      'Total Distance',
                      _formatDistance(_lastSummary!.distanceMeters),
                    ),
                    const SizedBox(height: 8),
                    _buildSummaryRow(
                      context,
                      'Calories Burned',
                      '${_lastSummary!.calories} kcal',
                    ),
                    const SizedBox(height: 8),
                    _buildSummaryRow(
                      context,
                      'Average Heart Rate',
                      '${_lastSummary!.averageHeartRate} bpm',
                    ),
                    const SizedBox(height: 8),
                    _buildSummaryRow(
                      context,
                      'Max Heart Rate',
                      '${_lastSummary!.maxHeartRate} bpm',
                    ),
                  ],
                ),
              ),
            ),
          ],

          // Placeholder when no exercise is active
          if (!_isExerciseActive &&
              _currentData == null &&
              _lastSummary == null) ...[
            const SizedBox(height: 32),
            Center(
              child: Column(
                children: [
                  Icon(
                    Icons.directions_run,
                    size: 64,
                    color: Colors.grey.shade400,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Select an exercise type and tap Start',
                    style: TextStyle(fontSize: 16, color: Colors.grey.shade600),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildMetricRow(
    BuildContext context,
    IconData icon,
    String label,
    String value,
  ) {
    return Row(
      children: [
        Icon(icon, color: Theme.of(context).colorScheme.primary),
        const SizedBox(width: 12),
        Expanded(
          child: Text(label, style: Theme.of(context).textTheme.bodyLarge),
        ),
        Text(
          value,
          style: Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
      ],
    );
  }

  Widget _buildSummaryRow(BuildContext context, String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: Theme.of(context).textTheme.bodyMedium),
        Text(
          value,
          style: Theme.of(
            context,
          ).textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.bold),
        ),
      ],
    );
  }

  IconData _getExerciseIcon(ExerciseType type) {
    switch (type) {
      case ExerciseType.walking:
        return Icons.directions_walk;
      case ExerciseType.running:
        return Icons.directions_run;
      case ExerciseType.cycling:
        return Icons.directions_bike;
      case ExerciseType.hiking:
        return Icons.terrain;
      case ExerciseType.swimming:
        return Icons.pool;
      case ExerciseType.yoga:
        return Icons.self_improvement;
      case ExerciseType.basketball:
      case ExerciseType.football:
      case ExerciseType.volleyball:
      case ExerciseType.baseball:
        return Icons.sports_basketball;
      case ExerciseType.badminton:
      case ExerciseType.tableTennis:
      case ExerciseType.tennis:
        return Icons.sports_tennis;
      case ExerciseType.climbing:
        return Icons.landscape;
      case ExerciseType.skiing:
        return Icons.downhill_skiing;
      case ExerciseType.skating:
        return Icons.ice_skating;
      case ExerciseType.rowing:
        return Icons.rowing;
      case ExerciseType.dancing:
        return Icons.music_note;
      case ExerciseType.boxing:
        return Icons.sports_mma;
      case ExerciseType.aerobics:
        return Icons.fitness_center;
      case ExerciseType.elliptical:
        return Icons.fitness_center;
    }
  }
}
