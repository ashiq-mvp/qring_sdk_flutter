/// User profile information.
class UserInfo {
  /// User age in years
  final int age;

  /// User height in centimeters
  final int heightCm;

  /// User weight in kilograms
  final int weightKg;

  /// Whether user is male
  final bool isMale;

  UserInfo({
    required this.age,
    required this.heightCm,
    required this.weightKg,
    required this.isMale,
  });

  /// Create from map received from platform channel
  factory UserInfo.fromMap(Map<String, dynamic> map) {
    return UserInfo(
      age: map['age'] as int? ?? 0,
      heightCm: map['heightCm'] as int? ?? 0,
      weightKg: map['weightKg'] as int? ?? 0,
      isMale: map['isMale'] as bool? ?? true,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'age': age,
      'heightCm': heightCm,
      'weightKg': weightKg,
      'isMale': isMale,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is UserInfo &&
          runtimeType == other.runtimeType &&
          age == other.age &&
          heightCm == other.heightCm &&
          weightKg == other.weightKg &&
          isMale == other.isMale;

  @override
  int get hashCode =>
      age.hashCode ^ heightCm.hashCode ^ weightKg.hashCode ^ isMale.hashCode;

  @override
  String toString() =>
      'UserInfo(age: $age, heightCm: $heightCm, weightKg: $weightKg, isMale: $isMale)';
}
