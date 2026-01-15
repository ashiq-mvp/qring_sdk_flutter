/// Sleep stage enumeration.
enum SleepStage {
  /// Not sleeping
  notSleeping,

  /// Device removed
  removed,

  /// Light sleep
  lightSleep,

  /// Deep sleep
  deepSleep,

  /// REM sleep
  rem,

  /// Awake
  awake;

  /// Create from integer value received from platform channel
  static SleepStage fromInt(int value) {
    switch (value) {
      case 0:
        return SleepStage.notSleeping;
      case 1:
        return SleepStage.removed;
      case 2:
        return SleepStage.lightSleep;
      case 3:
        return SleepStage.deepSleep;
      case 4:
        return SleepStage.rem;
      case 5:
        return SleepStage.awake;
      default:
        return SleepStage.notSleeping;
    }
  }

  /// Convert to integer for platform channel
  int toInt() {
    return index;
  }
}

/// Sleep detail for a specific period.
class SleepDetail {
  /// Duration in minutes
  final int durationMinutes;

  /// Sleep stage
  final SleepStage stage;

  SleepDetail({required this.durationMinutes, required this.stage});

  /// Create from map received from platform channel
  factory SleepDetail.fromMap(Map<String, dynamic> map) {
    // Handle stage as either int or string
    SleepStage stage;
    final stageValue = map['stage'];
    if (stageValue is int) {
      stage = SleepStage.fromInt(stageValue);
    } else if (stageValue is String) {
      stage = SleepStage.values.firstWhere(
        (e) => e.name == stageValue,
        orElse: () => SleepStage.notSleeping,
      );
    } else {
      stage = SleepStage.notSleeping;
    }

    return SleepDetail(
      durationMinutes: map['durationMinutes'] as int? ?? 0,
      stage: stage,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {'durationMinutes': durationMinutes, 'stage': stage.toInt()};
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is SleepDetail &&
          runtimeType == other.runtimeType &&
          durationMinutes == other.durationMinutes &&
          stage == other.stage;

  @override
  int get hashCode => durationMinutes.hashCode ^ stage.hashCode;

  @override
  String toString() =>
      'SleepDetail(durationMinutes: $durationMinutes, stage: $stage)';
}

/// Sleep stage and duration data.
class SleepData {
  /// Sleep start time
  final DateTime startTime;

  /// Sleep end time
  final DateTime endTime;

  /// List of sleep details
  final List<SleepDetail> details;

  /// Whether there was a lunch break
  final bool hasLunchBreak;

  /// Lunch break start time
  final DateTime? lunchStartTime;

  /// Lunch break end time
  final DateTime? lunchEndTime;

  SleepData({
    required this.startTime,
    required this.endTime,
    required this.details,
    this.hasLunchBreak = false,
    this.lunchStartTime,
    this.lunchEndTime,
  });

  /// Create from map received from platform channel
  factory SleepData.fromMap(Map<String, dynamic> map) {
    final detailsList =
        (map['details'] as List?)?.map((e) {
          final detailMap = Map<String, dynamic>.from(e as Map);
          return SleepDetail.fromMap(detailMap);
        }).toList() ??
        [];

    return SleepData(
      startTime: DateTime.fromMillisecondsSinceEpoch(map['startTime'] as int),
      endTime: DateTime.fromMillisecondsSinceEpoch(map['endTime'] as int),
      details: detailsList,
      hasLunchBreak: map['hasLunchBreak'] as bool? ?? false,
      lunchStartTime: map['lunchStartTime'] != null
          ? DateTime.fromMillisecondsSinceEpoch(map['lunchStartTime'] as int)
          : null,
      lunchEndTime: map['lunchEndTime'] != null
          ? DateTime.fromMillisecondsSinceEpoch(map['lunchEndTime'] as int)
          : null,
    );
  }

  /// Convert to map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'startTime': startTime.millisecondsSinceEpoch,
      'endTime': endTime.millisecondsSinceEpoch,
      'details': details.map((e) => e.toMap()).toList(),
      'hasLunchBreak': hasLunchBreak,
      'lunchStartTime': lunchStartTime?.millisecondsSinceEpoch,
      'lunchEndTime': lunchEndTime?.millisecondsSinceEpoch,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is SleepData &&
          runtimeType == other.runtimeType &&
          startTime == other.startTime &&
          endTime == other.endTime &&
          _listEquals(details, other.details) &&
          hasLunchBreak == other.hasLunchBreak &&
          lunchStartTime == other.lunchStartTime &&
          lunchEndTime == other.lunchEndTime;

  @override
  int get hashCode =>
      startTime.hashCode ^
      endTime.hashCode ^
      details.hashCode ^
      hasLunchBreak.hashCode ^
      lunchStartTime.hashCode ^
      lunchEndTime.hashCode;

  @override
  String toString() =>
      'SleepData(startTime: $startTime, endTime: $endTime, details: $details, '
      'hasLunchBreak: $hasLunchBreak, lunchStartTime: $lunchStartTime, '
      'lunchEndTime: $lunchEndTime)';

  bool _listEquals<T>(List<T>? a, List<T>? b) {
    if (a == null) return b == null;
    if (b == null || a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
