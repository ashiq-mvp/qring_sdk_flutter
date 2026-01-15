/// Summary data for a completed exercise session.
///
/// This data is returned when an exercise session is stopped,
/// providing aggregate statistics for the entire workout.
class ExerciseSummary {
  /// Total duration of the exercise in seconds
  final int durationSeconds;

  /// Total steps taken during the exercise
  final int totalSteps;

  /// Total distance covered in meters
  final int distanceMeters;

  /// Total calories burned during the exercise
  final int calories;

  /// Average heart rate during the exercise in beats per minute
  final int averageHeartRate;

  /// Maximum heart rate reached during the exercise in beats per minute
  final int maxHeartRate;

  const ExerciseSummary({
    required this.durationSeconds,
    required this.totalSteps,
    required this.distanceMeters,
    required this.calories,
    required this.averageHeartRate,
    required this.maxHeartRate,
  });

  /// Create ExerciseSummary from a map (typically from native platform)
  factory ExerciseSummary.fromMap(Map<String, dynamic> map) {
    return ExerciseSummary(
      durationSeconds: map['durationSeconds'] as int? ?? 0,
      totalSteps: map['totalSteps'] as int? ?? 0,
      distanceMeters: map['distanceMeters'] as int? ?? 0,
      calories: map['calories'] as int? ?? 0,
      averageHeartRate: map['averageHeartRate'] as int? ?? 0,
      maxHeartRate: map['maxHeartRate'] as int? ?? 0,
    );
  }

  /// Convert ExerciseSummary to a map
  Map<String, dynamic> toMap() {
    return {
      'durationSeconds': durationSeconds,
      'totalSteps': totalSteps,
      'distanceMeters': distanceMeters,
      'calories': calories,
      'averageHeartRate': averageHeartRate,
      'maxHeartRate': maxHeartRate,
    };
  }

  @override
  String toString() {
    return 'ExerciseSummary(duration: ${durationSeconds}s, steps: $totalSteps, '
        'distance: ${distanceMeters}m, calories: $calories, '
        'avgHR: $averageHeartRate bpm, maxHR: $maxHeartRate bpm)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExerciseSummary &&
        other.durationSeconds == durationSeconds &&
        other.totalSteps == totalSteps &&
        other.distanceMeters == distanceMeters &&
        other.calories == calories &&
        other.averageHeartRate == averageHeartRate &&
        other.maxHeartRate == maxHeartRate;
  }

  @override
  int get hashCode {
    return Object.hash(
      durationSeconds,
      totalSteps,
      distanceMeters,
      calories,
      averageHeartRate,
      maxHeartRate,
    );
  }
}
