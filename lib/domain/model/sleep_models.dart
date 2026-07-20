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

  /// Total milliseconds the stages account for, summed (hypnogram stages do not
  /// overlap). Tells a fully-staged night from one the device only staged part of.
  int get totalStageMs =>
      fold<int>(0, (sum, stage) => sum + math.max(stage.durationMs, 0));
}

/// Below this share of a session's span, its stage data is treated as too partial
/// to draw a hypnogram from. A device that only staged the tail of the night — or
/// a preliminary sync that has not finished — would otherwise render as a
/// near-empty chart with a fragment at one edge. A fully-staged night is ~1.0.
const double kMinSleepStageCoverage = 0.5;

/// Whether [session] has enough stage coverage to draw a meaningful hypnogram:
/// it has stages, and they cover at least [minCoverage] of its span. False for a
/// session the device only partially staged, so callers can show a note instead
/// of a misleading chart.
bool sleepSessionHasReliableStages(
  SleepData session, {
  double minCoverage = kMinSleepStageCoverage,
}) {
  if (session.stages.isEmpty) return false;
  final spanMs = session.endTime.difference(session.startTime).inMilliseconds;
  if (spanMs <= 0) return false;
  return session.stages.totalStageMs / spanMs >= minCoverage;
}

/// Wall-clock milliseconds covered by [sessions], counting overlapping time
/// once. Sweep-merges each session's `[startTime, endTime]` interval, so two
/// overlapping sessions from different sources yield the real time in bed, not
/// the sum of their durations. For non-overlapping input this equals the sum.
int sleepSessionsUnionMs(Iterable<SleepData> sessions) {
  final intervals = [
    for (final session in sessions)
      if (session.endTime.isAfter(session.startTime))
        (session.startTime, session.endTime),
  ]..sort((a, b) => a.$1.compareTo(b.$1));
  if (intervals.isEmpty) return 0;

  var totalMs = 0;
  var currentStart = intervals.first.$1;
  var currentEnd = intervals.first.$2;
  for (final interval in intervals.skip(1)) {
    if (interval.$1.isAfter(currentEnd)) {
      totalMs += currentEnd.difference(currentStart).inMilliseconds;
      currentStart = interval.$1;
      currentEnd = interval.$2;
    } else if (interval.$2.isAfter(currentEnd)) {
      currentEnd = interval.$2;
    }
  }
  totalMs += currentEnd.difference(currentStart).inMilliseconds;
  return totalMs;
}

/// Combines the stages of a night's [orderedSessions] (sorted by start) into one
/// timeline: identical stages are deduped, and the wake gap between consecutive
/// segments (up to [maxGap]) is filled with an Awake stage. This is what lets a
/// night broken by a wake render as one continuous hypnogram — and have the wake
/// count toward the awake share and the reliable-stage coverage — instead of
/// blocks separated by holes. Gaps larger than [maxGap] (a daytime nap, not part
/// of the night) are left unfilled. Empty when no segment carries any stage.
List<SleepStage> combineNightStages(
  List<SleepData> orderedSessions, {
  required Duration maxGap,
}) {
  final seenKeys = <(DateTime, DateTime, int)>{};
  final stages = <SleepStage>[];
  for (final stage in orderedSessions.expand((session) => session.stages)) {
    final key = (stage.startTime, stage.endTime, stage.stageType);
    if (seenKeys.add(key)) stages.add(stage);
  }
  if (stages.isEmpty) return const <SleepStage>[];

  final gapStages = <SleepStage>[];
  for (var index = 0; index < orderedSessions.length - 1; index++) {
    final previous = orderedSessions[index];
    final next = orderedSessions[index + 1];
    final gap = next.startTime.difference(previous.endTime);
    if (!gap.isNegative && gap > Duration.zero && gap <= maxGap) {
      gapStages.add(
        SleepStage(
          startTime: previous.endTime,
          endTime: next.startTime,
          stageType: SleepStage.stageAwake,
        ),
      );
    }
  }

  return [...stages, ...gapStages]..sort((a, b) {
      final byStart = a.startTime.compareTo(b.startTime);
      if (byStart != 0) return byStart;
      return a.endTime.compareTo(b.endTime);
    });
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
