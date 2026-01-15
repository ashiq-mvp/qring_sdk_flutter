/// Real-time exercise metrics during an active exercise session.
///
/// This data is streamed continuously while an exercise is in progress,
/// providing live updates on duration, heart rate, steps, distance, and calories.
class ExerciseData {
  /// Duration of the exercise in seconds
  final int durationSeconds;

  /// Current heart rate in beats per minute
  final int heartRate;

  /// Total steps taken during the exercise
  final int steps;

  /// Distance covered in meters
  final int distanceMeters;

  /// Calories burned during the exercise
  final int calories;

  const ExerciseData({
    required this.durationSeconds,
    required this.heartRate,
    required this.steps,
    required this.distanceMeters,
    required this.calories,
  });

  /// Create ExerciseData from a map (typically from native platform)
  factory ExerciseData.fromMap(Map<String, dynamic> map) {
    return ExerciseData(
      durationSeconds: map['durationSeconds'] as int? ?? 0,
      heartRate: map['heartRate'] as int? ?? 0,
      steps: map['steps'] as int? ?? 0,
      distanceMeters: map['distanceMeters'] as int? ?? 0,
      calories: map['calories'] as int? ?? 0,
    );
  }

  /// Convert ExerciseData to a map
  Map<String, dynamic> toMap() {
    return {
      'durationSeconds': durationSeconds,
      'heartRate': heartRate,
      'steps': steps,
      'distanceMeters': distanceMeters,
      'calories': calories,
    };
  }

  @override
  String toString() {
    return 'ExerciseData(duration: ${durationSeconds}s, HR: $heartRate bpm, '
        'steps: $steps, distance: ${distanceMeters}m, calories: $calories)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExerciseData &&
        other.durationSeconds == durationSeconds &&
        other.heartRate == heartRate &&
        other.steps == steps &&
        other.distanceMeters == distanceMeters &&
        other.calories == calories;
  }

  @override
  int get hashCode {
    return Object.hash(
      durationSeconds,
      heartRate,
      steps,
      distanceMeters,
      calories,
    );
  }
}
