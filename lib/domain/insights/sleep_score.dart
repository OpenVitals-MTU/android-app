import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import '../model/sleep_daily_summary.dart';
import '../model/sleep_models.dart';
import '../preferences/sleep_range_mode.dart';

part 'sleep_score.freezed.dart';

// The sleep score is a weighted blend of four sub-scores, each grounded in
// published sleep science (the same sources the sleep-score detail screen links).
//
// Research:
//   duration    https://aasm.org/advocacy/position-statements/adult-sleep-duration-health-advisory/
//               https://pubmed.ncbi.nlm.nih.gov/24470692/
//   efficiency  https://www.ncbi.nlm.nih.gov/medgen/1669302
//               https://pmc.ncbi.nlm.nih.gov/articles/PMC4751425/
//   continuity  https://pmc.ncbi.nlm.nih.gov/articles/PMC5971842/  (WASO / sleep vs time in bed)
//   regularity  https://www.nature.com/articles/s41598-017-03171-4
const double _durationWeight = 35.0;
const double _efficiencyWeight = 30.0;
const double _continuityWeight = 20.0;
const double _regularityWeight = 15.0;
const double _minimumScoredSleepMinutes = 60.0;
const double _neutralRegularityRatio = 0.7;
const int _minutesPerDay = 24 * 60;
const int sleepScoreLookbackDays = 7;

enum SleepScoreConfidence {
  high('HIGH'),
  medium('MEDIUM'),
  low('LOW'),
  noData('NO_DATA');

  const SleepScoreConfidence(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static SleepScoreConfidence? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class SleepScoreEstimate with _$SleepScoreEstimate {
  const factory SleepScoreEstimate({
    @Default(0) int score,
    @Default(SleepScoreConfidence.noData) SleepScoreConfidence confidence,
    @Default(0.0) double durationPoints,
    @Default(0.0) double efficiencyPoints,
    @Default(0.0) double continuityPoints,
    @Default(0.0) double regularityPoints,
    @Default(0.0) double sleepDurationMinutes,
    @Default(0.0) double timeInBedMinutes,
    @Default(0.0) double sleepEfficiencyPercent,
    @Default(0.0) double wakeAfterSleepOnsetMinutes,
    double? regularityDifferenceMinutes,
    @Default(0) int regularityBaselineNights,
    @Default(0) int sleepStageCount,
    @Default(false) bool usesSleepStages,
    @Default(false) bool usesExplicitAwakeStages,
  }) = _SleepScoreEstimate;

  static const SleepScoreEstimate noData = SleepScoreEstimate();
}

Map<LocalDate, SleepScoreEstimate> calculateSleepScoresByDate(
  List<SleepData> sessions,
  LocalDate start,
  LocalDate end,
) {
  if (end.isBefore(start)) return <LocalDate, SleepScoreEstimate>{};

  final dates = <LocalDate>[];
  var date = start;
  while (!date.isAfter(end)) {
    dates.add(date);
    date = date.plusDays(1);
  }

  final sessionsByDate = <LocalDate, List<SleepData>>{};
  for (final session in sessions) {
    final key = instantToLocalDate(session.endTime);
    (sessionsByDate[key] ??= <SleepData>[]).add(session);
  }
  final mainSessions = <LocalDate, SleepData?>{
    for (final date in dates)
      date: _mainSleepSession(sessionsByDate[date] ?? const <SleepData>[]),
  };

  final result = <LocalDate, SleepScoreEstimate>{};
  for (var index = 0; index < dates.length; index++) {
    final date = dates[index];
    final previousSessions = <SleepData>[];
    for (var i = 0; i < index; i++) {
      final previous = mainSessions[dates[i]];
      if (previous != null) previousSessions.add(previous);
    }
    result[date] = calculateSleepScore(
      mainSessions[date],
      previousSessions,
    );
  }
  return result;
}

SleepScoreEstimate calculateSleepScoreForDate(
  LocalDate selectedDate,
  List<SleepData> sessions,
  SleepRangeMode sleepRangeMode,
) {
  final selectedSleep = dailySleepSummary(
    sessions,
    selectedDate,
    sleepRangeMode: sleepRangeMode,
  );
  final startDate = selectedDate.minusDays(sleepScoreLookbackDays - 1);
  final previousSessions = <SleepData>[];
  var date = startDate;
  while (date.isBefore(selectedDate)) {
    final summary = dailySleepSummary(
      sessions,
      date,
      sleepRangeMode: sleepRangeMode,
    );
    if (summary != null) previousSessions.add(summary);
    date = date.plusDays(1);
  }

  return calculateSleepScore(
    selectedSleep,
    previousSessions,
  );
}

SleepScoreEstimate calculateSleepScore(
  SleepData? session,
  List<SleepData> previousSessions,
) {
  if (session == null) return SleepScoreEstimate.noData;
  final timeInBedMs =
      math.max(0, session.endTime.difference(session.startTime).inMilliseconds);
  final sleepDurationMs =
      math.max(0, sleepDurationMsFromStages(session.stages, session.durationMs));
  if (timeInBedMs <= 0 ||
      sleepDurationMs <
          Duration(minutes: _minimumScoredSleepMinutes.toInt()).inMilliseconds) {
    return SleepScoreEstimate.noData;
  }

  final sleepDurationMinutes = sleepDurationMs.toDouble() / 60000.0;
  final timeInBedMinutes = timeInBedMs.toDouble() / 60000.0;
  final sleepEfficiencyPercent =
      (sleepDurationMs / timeInBedMs.toDouble() * 100.0).clamp(0.0, 100.0).toDouble();
  final explicitWakeMs = _wakeAfterSleepOnsetMs(session);
  final wakeAfterSleepOnsetMinutes =
      (explicitWakeMs ?? math.max(0, timeInBedMs - sleepDurationMs)).toDouble() /
          60000.0;
  final midpoint = _sleepMidpointMinute(session);
  final baselineMidpoints =
      previousSessions.map(_sleepMidpointMinute).toList();
  final double? regularityDifference = baselineMidpoints.length >= 2
      ? _circularMinuteDifference(midpoint, _circularMeanMinutes(baselineMidpoints))
          .toDouble()
      : null;
  final hasSleepStages =
      session.stages.any((stage) => _isSleepStage(stage.stageType));
  final hasExplicitAwakeStages =
      session.stages.any((stage) => _isAwakeStage(stage.stageType));

  final durationPoints = _durationPoints(sleepDurationMinutes / 60.0);
  final efficiencyPoints = _efficiencyPoints(sleepEfficiencyPercent);
  final continuityPoints = _continuityPoints(wakeAfterSleepOnsetMinutes);
  final regularityPoints = regularityDifference != null
      ? _regularityPoints(regularityDifference)
      : _regularityWeight * _neutralRegularityRatio;
  final score =
      (durationPoints + efficiencyPoints + continuityPoints + regularityPoints)
          .round()
          .clamp(0, 100)
          .toInt();

  final SleepScoreConfidence confidence;
  if (hasSleepStages && hasExplicitAwakeStages && baselineMidpoints.length >= 3) {
    confidence = SleepScoreConfidence.high;
  } else if (hasSleepStages || baselineMidpoints.length >= 2) {
    confidence = SleepScoreConfidence.medium;
  } else {
    confidence = SleepScoreConfidence.low;
  }

  return SleepScoreEstimate(
    score: score,
    confidence: confidence,
    durationPoints: durationPoints,
    efficiencyPoints: efficiencyPoints,
    continuityPoints: continuityPoints,
    regularityPoints: regularityPoints,
    sleepDurationMinutes: sleepDurationMinutes,
    timeInBedMinutes: timeInBedMinutes,
    sleepEfficiencyPercent: sleepEfficiencyPercent,
    wakeAfterSleepOnsetMinutes: wakeAfterSleepOnsetMinutes,
    regularityDifferenceMinutes: regularityDifference,
    regularityBaselineNights: baselineMidpoints.length,
    sleepStageCount: session.stages.length,
    usesSleepStages: hasSleepStages,
    usesExplicitAwakeStages: hasExplicitAwakeStages,
  );
}

SleepData? _mainSleepSession(List<SleepData> sessions) {
  SleepData? best;
  int? bestDuration;
  for (final session in sessions) {
    final duration =
        sleepDurationMsFromStages(session.stages, session.durationMs);
    if (bestDuration == null || duration > bestDuration) {
      bestDuration = duration;
      best = session;
    }
  }
  return best;
}

double _durationPoints(double hours) {
  final double ratio;
  if (hours >= 7.0 && hours <= 9.0) {
    ratio = 1.0;
  } else if (hours < 7.0) {
    ratio = ((hours - 4.0) / 3.0).clamp(0.0, 1.0).toDouble();
  } else {
    ratio = ((11.0 - hours) / 2.0).clamp(0.0, 1.0).toDouble();
  }
  return _durationWeight * ratio;
}

double _efficiencyPoints(double efficiencyPercent) =>
    _efficiencyWeight * ((efficiencyPercent - 65.0) / 20.0).clamp(0.0, 1.0);

double _continuityPoints(double wakeAfterSleepOnsetMinutes) =>
    _continuityWeight *
    ((90.0 - wakeAfterSleepOnsetMinutes) / 70.0).clamp(0.0, 1.0);

double _regularityPoints(double regularityDifferenceMinutes) =>
    _regularityWeight *
    ((180.0 - regularityDifferenceMinutes) / 150.0).clamp(0.0, 1.0);

int _sleepMidpointMinute(SleepData session) {
  final durationMs =
      math.max(0, session.endTime.difference(session.startTime).inMilliseconds);
  final midpoint = session.startTime.add(Duration(milliseconds: durationMs ~/ 2));
  final localTime = instantToLocalTime(midpoint);
  return localTime.hour * 60 + localTime.minute;
}

int _circularMeanMinutes(List<int> values) {
  final sinMean = values.fold<double>(
        0.0,
        (sum, value) => sum + math.sin(value / _minutesPerDay * 2.0 * math.pi),
      ) /
      values.length;
  final cosMean = values.fold<double>(
        0.0,
        (sum, value) => sum + math.cos(value / _minutesPerDay * 2.0 * math.pi),
      ) /
      values.length;
  var angle = math.atan2(sinMean, cosMean);
  if (angle < 0.0) angle += 2.0 * math.pi;
  return (angle / (2.0 * math.pi) * _minutesPerDay).round() % _minutesPerDay;
}

int _circularMinuteDifference(int first, int second) {
  final difference = (first - second).abs();
  return math.min(difference, _minutesPerDay - difference);
}

int? _wakeAfterSleepOnsetMs(SleepData session) {
  final sleepStages = session.stages
      .where((stage) => _isSleepStage(stage.stageType))
      .toList()
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  if (sleepStages.isEmpty) return null;

  final sleepStart = sleepStages.first.startTime;
  final sleepEnd = sleepStages.last.endTime;
  return session.stages
      .where((stage) => _isAwakeStage(stage.stageType))
      .fold<int>(0, (sum, stage) => sum + _overlapMs(stage, sleepStart, sleepEnd));
}

int _overlapMs(SleepStage stage, DateTime windowStart, DateTime windowEnd) {
  final overlapStart =
      stage.startTime.isAfter(windowStart) ? stage.startTime : windowStart;
  final overlapEnd =
      stage.endTime.isBefore(windowEnd) ? stage.endTime : windowEnd;
  if (!overlapEnd.isAfter(overlapStart)) return 0;
  return math.max(0, overlapEnd.difference(overlapStart).inMilliseconds);
}

bool _isSleepStage(int stageType) {
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

bool _isAwakeStage(int stageType) {
  switch (stageType) {
    case SleepStage.stageAwake:
    case SleepStage.stageAwakeInBed:
      return true;
    default:
      return false;
  }
}
