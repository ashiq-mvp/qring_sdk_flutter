/// Supported exercise types for the QC Ring device.
///
/// These exercise types correspond to the types supported by the native SDK.
enum ExerciseType {
  /// Walking exercise
  walking(0),

  /// Running exercise
  running(1),

  /// Cycling exercise
  cycling(2),

  /// Hiking exercise
  hiking(3),

  /// Swimming exercise
  swimming(4),

  /// Yoga exercise
  yoga(5),

  /// Basketball exercise
  basketball(6),

  /// Football/Soccer exercise
  football(7),

  /// Badminton exercise
  badminton(8),

  /// Table tennis exercise
  tableTennis(9),

  /// Tennis exercise
  tennis(10),

  /// Volleyball exercise
  volleyball(11),

  /// Baseball exercise
  baseball(12),

  /// Climbing exercise
  climbing(13),

  /// Skiing exercise
  skiing(14),

  /// Skating exercise
  skating(15),

  /// Rowing exercise
  rowing(16),

  /// Dancing exercise
  dancing(17),

  /// Boxing exercise
  boxing(18),

  /// Aerobics exercise
  aerobics(19),

  /// Elliptical exercise
  elliptical(20);

  const ExerciseType(this.value);

  /// The numeric value used by the native SDK
  final int value;

  /// Get ExerciseType from numeric value
  static ExerciseType fromValue(int value) {
    return ExerciseType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => ExerciseType.walking,
    );
  }
}
