import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'sleep_models.freezed.dart';

@freezed
abstract class SleepData with _$SleepData {
  const SleepData._();

  const factory SleepData({
    required String id,
    required DateTime startTime,
    required DateTime endTime,
    required int durationMs,
    required String source,
    String? title,
    String? notes,
    Duration? startZoneOffset,
    Duration? endZoneOffset,
    DateTime? lastModifiedTime,
    String? clientRecordId,
    int? clientRecordVersion,
    int? recordingMethod,
    SleepDeviceData? device,
    @Default(<SleepStage>[]) List<SleepStage> stages,
  }) = _SleepData;

  double get durationHours => durationMs / 3600000.0;
}

@freezed
abstract class SleepDeviceData with _$SleepDeviceData {
  const factory SleepDeviceData({
    required int type,
    required String? manufacturer,
    required String? model,
  }) = _SleepDeviceData;
}

@freezed
abstract class SleepStage with _$SleepStage {
  const SleepStage._();

  const factory SleepStage({
    required DateTime startTime,
    required DateTime endTime,
    required int stageType,
  }) = _SleepStage;

  int get durationMs =>
      endTime.millisecondsSinceEpoch - startTime.millisecondsSinceEpoch;

  static const int stageAwake = 1;
  static const int stageSleeping = 2;
  static const int stageOutOfBed = 3;
  static const int stageLight = 4;
  static const int stageDeep = 5;
  static const int stageRem = 6;
  static const int stageAwakeInBed = 7;
}

@freezed
abstract class DailySleepDuration with _$DailySleepDuration {
  const DailySleepDuration._();

  const factory DailySleepDuration({
    required LocalDate date,
    required int durationMs,
  }) = _DailySleepDuration;

  double get durationHours => durationMs / 3600000.0;
}

@freezed
abstract class SleepReadData with _$SleepReadData {
  const factory SleepReadData({
    @Default(<SleepData>[]) List<SleepData> sessions,
    @Default(<DailySleepDuration>[])
    List<DailySleepDuration> dailyAggregateDurations,
  }) = _SleepReadData;
}

/// Stage types shown as "Awake" in grouped breakdowns (in-bed awake, excluding
/// out-of-bed).
const Set<int> awakeStageTypes = {
  SleepStage.stageAwake,
  SleepStage.stageAwakeInBed,
};

/// Stage types shown as "Core" (Apple naming) — light plus generic sleeping.
const Set<int> coreStageTypes = {
  SleepStage.stageLight,
  SleepStage.stageSleeping,
};

extension SleepStageDurations on List<SleepStage> {
  /// Total duration of the stages whose [SleepStage.stageType] is in [types].
  int durationMsForTypes(Set<int> types) => where(
        (stage) => types.contains(stage.stageType),
      ).fold<int>(0, (sum, stage) => sum + math.max(stage.durationMs, 0));
}

int sleepDurationMsFromStages(
  List<SleepStage> stages,
  int fallbackDurationMs,
) {
  if (stages.isEmpty) return math.max(fallbackDurationMs, 0);

  final sleepStageDurationMs = stages
      .where((stage) => _isSleepDurationStage(stage.stageType))
      .fold<int>(0, (sum, stage) => sum + math.max(stage.durationMs, 0));

  return sleepStageDurationMs > 0
      ? sleepStageDurationMs
      : math.max(fallbackDurationMs, 0);
}

bool _isSleepDurationStage(int stageType) {
  switch (stageType) {
    case SleepStage.stageSleeping:
    case SleepStage.stageLight:
    case SleepStage.stageDeep:
    case SleepStage.stageRem:
      return true;
    default:
      return false;
  }
}
