import 'dart:math' as math;

import '../../core/period/period_calculations.dart';
import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../../domain/insights/cross_metric_insights.dart';
import '../../domain/insights/sleep_score.dart';
import '../../domain/model/sleep_daily_summary.dart';
import '../../domain/model/sleep_models.dart';
import '../../domain/preferences/sleep_range_mode.dart';
import '../../domain/usecase/load_sleep_period_use_case.dart';

/// One night's sleep duration (hours) for the period chart. Port of the Kotlin
/// `SleepDurationPoint`.
class SleepDurationPoint {
  const SleepDurationPoint(this.date, this.hours);

  final LocalDate date;
  final double hours;
}

/// The averaged bedtime/wake schedule (minute-of-day). Port of the Kotlin
/// `SleepOverviewSchedule`.
class SleepOverviewSchedule {
  const SleepOverviewSchedule(this.startMinute, this.endMinute);

  final int startMinute;
  final int endMinute;
}

/// The aggregated period summary shown by the overview + stage cards. A trimmed
/// port of the Kotlin `SleepOverviewSummary` (the fields the primary cards use).
class SleepOverviewSummary {
  const SleepOverviewSummary({
    this.sleepScore,
    this.sleepDurationMs = 0,
    this.timeInBedMs = 0,
    this.awakeDurationMs = 0,
    this.remDurationMs = 0,
    this.coreDurationMs = 0,
    this.deepDurationMs = 0,
    this.sleepEfficiencyPercent,
    this.schedule,
  });

  final int? sleepScore;
  final int sleepDurationMs;
  final int timeInBedMs;
  final int awakeDurationMs;
  final int remDurationMs;
  final int coreDurationMs;
  final int deepDurationMs;
  final double? sleepEfficiencyPercent;
  final SleepOverviewSchedule? schedule;

  bool get hasStageBreakdown =>
      awakeDurationMs + remDurationMs + coreDurationMs + deepDurationMs > 0;
}

/// The precomputed sleep display, the Flutter analogue of the Kotlin
/// `SleepDisplayState`. Built purely from a [SleepPeriodLoadResult] + selection.
class SleepDisplay {
  const SleepDisplay({
    required this.dailySessions,
    required this.dailySummary,
    required this.durationPoints,
    required this.previousDurationPoints,
    required this.baselineDurationPoints,
    required this.overviewSummary,
    required this.sessionsByDate,
    required this.crossMetricHrvValues,
  });

  final List<SleepData> dailySessions;
  final SleepData? dailySummary;
  final List<SleepDurationPoint> durationPoints;
  final List<SleepDurationPoint> previousDurationPoints;

  /// The 90 days before the period, for the personal-baseline stats.
  final List<SleepDurationPoint> baselineDurationPoints;
  final SleepOverviewSummary overviewSummary;

  /// The sessions of each night in the period, keyed by the night's date. The
  /// entries list flattens it; the selected-day section reads one key.
  final Map<LocalDate, List<SleepData>> sessionsByDate;

  /// Every session inside the selected period, newest night last.
  List<SleepData> get periodSessions =>
      [for (final sessions in sessionsByDate.values) ...sessions];

  /// Daily HRV over the same period, for the sleep-vs-HRV correlation.
  final List<CrossMetricValue> crossMetricHrvValues;
}

const int _minutesPerDay = 24 * 60;

/// Builds the sleep display for [result] over the scaffold's [period]. Port of
/// the Kotlin `SleepPresentationMapper.build`, trimmed to the primary sections.
SleepDisplay buildSleepDisplay({
  required SleepPeriodLoadResult result,
  required TimeRange selectedRange,
  required LocalDate selectedDate,
  required SleepRangeMode sleepRangeMode,
  required WeekPeriodMode weekPeriodMode,
}) {
  final selectedPeriod = displayPeriodFor(
    selectedRange,
    selectedDate,
    weekPeriodMode: weekPeriodMode,
  );
  final previousPeriod = previousPeriodFor(
    selectedRange,
    selectedDate,
    weekPeriodMode: weekPeriodMode,
  );

  final dailySessions = sleepSessionsForRange(
    result.sessions,
    selectedDate,
    sleepRangeMode,
  );
  final dailySummary = _withDurationOverride(
    dailySleepSummary(
      result.sessions,
      selectedDate,
      sleepRangeMode: sleepRangeMode,
    ),
    result.dailyDurations,
    selectedDate,
  );

  final durationPoints = _sleepDurationPoints(
    result.sessions,
    result.dailyDurations,
    selectedPeriod,
    sleepRangeMode,
  );
  final previousDurationPoints = _sleepDurationPoints(
    result.previousSessions,
    result.previousDailyDurations,
    previousPeriod,
    sleepRangeMode,
  );

  final scoreSessions = _distinctById([
    ...result.baselineSessions,
    ...result.sessions,
  ]);
  final overviewDays = _overviewDays(
    result.sessions,
    scoreSessions,
    result.dailyDurations,
    selectedPeriod,
    sleepRangeMode,
  );

  final baselineDurationPoints = _sleepDurationPoints(
    result.baselineSessions,
    result.baselineDailyDurations,
    baselinePeriodBefore(selectedPeriod),
    sleepRangeMode,
  );

  return SleepDisplay(
    dailySessions: dailySessions,
    dailySummary: dailySummary,
    durationPoints: durationPoints,
    previousDurationPoints: previousDurationPoints,
    baselineDurationPoints: baselineDurationPoints,
    overviewSummary: _overviewSummary(overviewDays),
    sessionsByDate: {
      for (final date in _datesInPeriod(selectedPeriod.start, selectedPeriod.end))
        date: sleepSessionsForRange(result.sessions, date, sleepRangeMode),
    },
    crossMetricHrvValues: [
      for (final hrv in result.crossDailyHrv)
        CrossMetricValue(date: hrv.date, value: hrv.rmssdMs),
    ],
  );
}

List<LocalDate> _datesInPeriod(LocalDate start, LocalDate end) {
  final dates = <LocalDate>[];
  var date = start;
  while (!date.isAfter(end)) {
    dates.add(date);
    date = date.plusDays(1);
  }
  return dates;
}

List<SleepDurationPoint> _sleepDurationPoints(
  List<SleepData> sessions,
  List<DailySleepDuration> dailyDurations,
  DatePeriod period,
  SleepRangeMode sleepRangeMode,
) {
  final durationsByDate = {for (final d in dailyDurations) d.date: d};
  return [
    for (final date in _datesInPeriod(period.start, period.end))
      SleepDurationPoint(
        date,
        durationsByDate[date]?.durationHours ??
            dailySleepSummary(
              sessions,
              date,
              sleepRangeMode: sleepRangeMode,
            )?.durationHours ??
            0.0,
      ),
  ];
}

/// One overview day: the night's sessions + a sleep-score estimate.
class _OverviewDay {
  const _OverviewDay(this.sessions, this.aggregateDurationMs, this.sleepScore);

  final List<SleepData> sessions;
  final int? aggregateDurationMs;
  final SleepScoreEstimate sleepScore;

  int get sleepDurationMs {
    final aggregate = aggregateDurationMs;
    if (aggregate != null && aggregate > 0) return aggregate;
    return sessions.fold<int>(
      0,
      (sum, s) => sum + sleepDurationMsFromStages(s.stages, s.durationMs),
    );
  }

  int get timeInBedMs => sessions.fold<int>(
        0,
        (sum, s) => sum +
            math.max(
              0,
              s.endTime.difference(s.startTime).inMilliseconds,
            ),
      );

  int _stageMs(Set<int> types) => sessions.fold<int>(
        0,
        (sum, s) => sum + s.stages.durationMsForTypes(types),
      );

  int get awakeDurationMs => _stageMs(awakeStageTypes);
  int get remDurationMs => _stageMs({SleepStage.stageRem});
  int get coreDurationMs => _stageMs(coreStageTypes);
  int get deepDurationMs => _stageMs({SleepStage.stageDeep});

  SleepData? get mainSession {
    SleepData? best;
    int? bestDuration;
    for (final s in sessions) {
      final duration = sleepDurationMsFromStages(s.stages, s.durationMs);
      if (bestDuration == null || duration > bestDuration) {
        bestDuration = duration;
        best = s;
      }
    }
    return best;
  }
}

List<_OverviewDay> _overviewDays(
  List<SleepData> sessions,
  List<SleepData> scoreSessions,
  List<DailySleepDuration> dailyDurations,
  DatePeriod period,
  SleepRangeMode sleepRangeMode,
) {
  final durationsByDate = {for (final d in dailyDurations) d.date: d};
  return [
    for (final date in _datesInPeriod(period.start, period.end))
      _OverviewDay(
        sleepSessionsForRange(sessions, date, sleepRangeMode),
        durationsByDate[date]?.durationMs,
        calculateSleepScoreForDate(date, scoreSessions, sleepRangeMode),
      ),
  ];
}

SleepOverviewSummary _overviewSummary(List<_OverviewDay> days) {
  if (days.isEmpty) return const SleepOverviewSummary();
  final nights = days.where((d) => d.sleepDurationMs > 0).toList();
  final scoredDays = days
      .where((d) => d.sleepScore.confidence != SleepScoreConfidence.noData)
      .toList();
  final mainSessions =
      nights.map((d) => d.mainSession).whereType<SleepData>().toList();
  final durationSource = days.length > 1 ? nights : days;

  int average(Iterable<int> Function(_OverviewDay) selector) {
    final values = durationSource
        .expand(selector)
        .where((value) => value > 0)
        .toList();
    if (values.isEmpty) return 0;
    return (values.fold<int>(0, (a, b) => a + b) / values.length).round();
  }

  return SleepOverviewSummary(
    sleepScore: scoredDays.isEmpty
        ? null
        : (scoredDays.map((d) => d.sleepScore.score).fold<int>(0, (a, b) => a + b) /
                scoredDays.length)
            .round(),
    sleepDurationMs: average((d) => [d.sleepDurationMs]),
    timeInBedMs: average((d) => [d.timeInBedMs]),
    awakeDurationMs: average((d) => [d.awakeDurationMs]),
    remDurationMs: average((d) => [d.remDurationMs]),
    coreDurationMs: average((d) => [d.coreDurationMs]),
    deepDurationMs: average((d) => [d.deepDurationMs]),
    sleepEfficiencyPercent: scoredDays.isEmpty
        ? null
        : scoredDays
                .map((d) => d.sleepScore.sleepEfficiencyPercent)
                .fold<double>(0, (a, b) => a + b) /
            scoredDays.length,
    schedule: _averageSchedule(mainSessions),
  );
}

SleepOverviewSchedule? _averageSchedule(List<SleepData> sessions) {
  if (sessions.isEmpty) return null;
  final startMinute = _circularMeanMinutes([
    for (final s in sessions) instantToLocalTime(s.startTime).minuteOfDay,
  ]);
  final endMinute = _circularMeanMinutes([
    for (final s in sessions) instantToLocalTime(s.endTime).minuteOfDay,
  ]);
  return SleepOverviewSchedule(startMinute, endMinute);
}

int _circularMeanMinutes(List<int> values) {
  if (values.isEmpty) return 0;
  final sinMean = values.fold<double>(
        0,
        (sum, v) => sum + math.sin(v / _minutesPerDay * 2 * math.pi),
      ) /
      values.length;
  final cosMean = values.fold<double>(
        0,
        (sum, v) => sum + math.cos(v / _minutesPerDay * 2 * math.pi),
      ) /
      values.length;
  var angle = math.atan2(sinMean, cosMean);
  if (angle < 0) angle += 2 * math.pi;
  return (angle / (2 * math.pi) * _minutesPerDay).round() % _minutesPerDay;
}

SleepData? _withDurationOverride(
  SleepData? summary,
  List<DailySleepDuration> dailyDurations,
  LocalDate date,
) {
  if (summary == null) return null;
  final durationMs = dailyDurations
      .where((d) => d.date == date)
      .map((d) => d.durationMs)
      .where((ms) => ms > 0)
      .cast<int?>()
      .firstWhere((ms) => ms != null, orElse: () => null);
  if (durationMs == null) return summary;
  return summary.copyWith(durationMs: durationMs);
}

List<SleepData> _distinctById(List<SleepData> sessions) {
  final seen = <String>{};
  final result = <SleepData>[];
  for (final s in sessions) {
    if (seen.add(s.id)) result.add(s);
  }
  return result;
}
