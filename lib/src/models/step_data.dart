/// Daily step count, distance, and calorie data.
class StepData {
  /// Date of the step data
  final DateTime date;

  /// Total steps for the day
  final int totalSteps;

  /// Running steps for the day
  final int runningSteps;

  /// Calories burned
  final int calories;

  /// Distance in meters
  final int distanceMeters;

  /// Sport duration in seconds
  final int sportDurationSeconds;

  /// Sleep duration in seconds
  final int sleepDurationSeconds;

  StepData({
    required this.date,
    required this.totalSteps,
    required this.runningSteps,
    required this.calories,
    required this.distanceMeters,
    required this.sportDurationSeconds,
    required this.sleepDurationSeconds,
  });

  /// Create from map received from platform channel
  factory StepData.fromMap(Map<String, dynamic> map) {
    return StepData(
      date: DateTime.parse(map['date'] as String),
      totalSteps: map['totalSteps'] as int? ?? 0,
      runningSteps: map['runningSteps'] as int? ?? 0,
      calories: map['calories'] as int? ?? 0,
      distanceMeters: map['distanceMeters'] as int? ?? 0,
      sportDurationSeconds: map['sportDurationSeconds'] as int? ?? 0,
      sleepDurationSeconds: map['sleepDurationSeconds'] as int? ?? 0,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'date': date.toIso8601String(),
      'totalSteps': totalSteps,
      'runningSteps': runningSteps,
      'calories': calories,
      'distanceMeters': distanceMeters,
      'sportDurationSeconds': sportDurationSeconds,
      'sleepDurationSeconds': sleepDurationSeconds,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is StepData &&
          runtimeType == other.runtimeType &&
          date == other.date &&
          totalSteps == other.totalSteps &&
          runningSteps == other.runningSteps &&
          calories == other.calories &&
          distanceMeters == other.distanceMeters &&
          sportDurationSeconds == other.sportDurationSeconds &&
          sleepDurationSeconds == other.sleepDurationSeconds;

  @override
  int get hashCode =>
      date.hashCode ^
      totalSteps.hashCode ^
      runningSteps.hashCode ^
      calories.hashCode ^
      distanceMeters.hashCode ^
      sportDurationSeconds.hashCode ^
      sleepDurationSeconds.hashCode;

  @override
  String toString() =>
      'StepData(date: $date, totalSteps: $totalSteps, runningSteps: $runningSteps, '
      'calories: $calories, distanceMeters: $distanceMeters, '
      'sportDurationSeconds: $sportDurationSeconds, sleepDurationSeconds: $sleepDurationSeconds)';
}
