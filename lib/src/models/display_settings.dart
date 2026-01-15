/// Ring display configuration.
class DisplaySettings {
  /// Whether display is enabled
  final bool enabled;

  /// Whether ring is worn on left hand
  final bool leftHand;

  /// Screen brightness level
  final int brightness;

  /// Maximum brightness level
  final int maxBrightness;

  /// Whether Do Not Disturb mode is enabled
  final bool doNotDisturb;

  /// Screen-on start time in minutes from midnight
  final int screenOnStartMinutes;

  /// Screen-on end time in minutes from midnight
  final int screenOnEndMinutes;

  DisplaySettings({
    required this.enabled,
    required this.leftHand,
    required this.brightness,
    required this.maxBrightness,
    required this.doNotDisturb,
    required this.screenOnStartMinutes,
    required this.screenOnEndMinutes,
  });

  /// Create from map received from platform channel
  factory DisplaySettings.fromMap(Map<String, dynamic> map) {
    return DisplaySettings(
      enabled: map['enabled'] as bool? ?? true,
      leftHand: map['leftHand'] as bool? ?? false,
      brightness: map['brightness'] as int? ?? 1,
      maxBrightness: map['maxBrightness'] as int? ?? 5,
      doNotDisturb: map['doNotDisturb'] as bool? ?? false,
      screenOnStartMinutes: map['screenOnStartMinutes'] as int? ?? 0,
      screenOnEndMinutes: map['screenOnEndMinutes'] as int? ?? 1440,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'enabled': enabled,
      'leftHand': leftHand,
      'brightness': brightness,
      'maxBrightness': maxBrightness,
      'doNotDisturb': doNotDisturb,
      'screenOnStartMinutes': screenOnStartMinutes,
      'screenOnEndMinutes': screenOnEndMinutes,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is DisplaySettings &&
          runtimeType == other.runtimeType &&
          enabled == other.enabled &&
          leftHand == other.leftHand &&
          brightness == other.brightness &&
          maxBrightness == other.maxBrightness &&
          doNotDisturb == other.doNotDisturb &&
          screenOnStartMinutes == other.screenOnStartMinutes &&
          screenOnEndMinutes == other.screenOnEndMinutes;

  @override
  int get hashCode =>
      enabled.hashCode ^
      leftHand.hashCode ^
      brightness.hashCode ^
      maxBrightness.hashCode ^
      doNotDisturb.hashCode ^
      screenOnStartMinutes.hashCode ^
      screenOnEndMinutes.hashCode;

  @override
  String toString() =>
      'DisplaySettings(enabled: $enabled, leftHand: $leftHand, brightness: $brightness, '
      'maxBrightness: $maxBrightness, doNotDisturb: $doNotDisturb, '
      'screenOnStartMinutes: $screenOnStartMinutes, screenOnEndMinutes: $screenOnEndMinutes)';
}
