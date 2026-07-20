import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:intl/intl.dart';

import '../../../core/period/period_calculations.dart';
import '../../../core/period/time_range.dart';
import '../../../core/stats/stats.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/cross_metric_insights.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/insights/data_confidence.dart';
import '../../../domain/insights/metric_interpretations.dart';
import '../../../domain/insights/period_comparison.dart';
import '../../../domain/insights/personal_baseline.dart';
import '../../../domain/insights/sleep_score.dart';
import '../../../domain/model/sleep_daily_summary.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../../domain/model/recording_method.dart';
import '../../../domain/usecase/load_sleep_period_use_case.dart';
import '../../../ui/charts/bar_chart.dart';

part 'sleep_display.freezed.dart';

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

/// One night as the schedule chart wants it. Kotlin `SleepScheduleDay`.
@immutable
class SleepScheduleDay {
  const SleepScheduleDay({
    required this.date,
    required this.inBedStart,
    required this.inBedEnd,
    this.stages = const [],
  });

  final LocalDate date;
  final DateTime? inBedStart;
  final DateTime? inBedEnd;
  final List<SleepStage> stages;
}

/// One row of the "share of time in bed" card: how long a stage lasted and how
/// much of the night that was. Precomputed, so the card only paints it.
class SleepStageShare {
  const SleepStageShare({
    required this.stageType,
    required this.durationMs,
    required this.fraction,
    required this.percent,
  });

  final int stageType;
  final int durationMs;

  /// The bar's width, clamped to the track.
  final double fraction;
  final int percent;
}

/// The precomputed sleep display, the Flutter analogue of the Kotlin
/// `SleepDisplayState`. Built purely from a [SleepPeriodLoadResult] + selection
/// by [buildSleepDisplay], once per load, in the view-model — the screen renders
/// it and derives nothing.
@freezed
abstract class SleepDisplay with _$SleepDisplay {
  const SleepDisplay._();

  const factory SleepDisplay({
    /// The period the display was built for (the same one the scaffold shows).
    required DatePeriod period,
    required bool isDay,
    required List<SleepData> dailySessions,

    /// Daytime naps for the selected day, reported separately from the night.
    required List<SleepData> dayNaps,
    required SleepData? dailySummary,
    required List<SleepDurationPoint> durationPoints,
    required List<SleepDurationPoint> previousDurationPoints,

    /// The 90 days before the period, for the personal-baseline stats.
    required List<SleepDurationPoint> baselineDurationPoints,
    required SleepOverviewSummary overviewSummary,

    /// The sessions of each night in the period, keyed by the night's date.
    required Map<LocalDate, List<SleepData>> sessionsByDate,

    /// Every session inside the selected period, newest night last.
    required List<SleepData> periodSessions,

    /// Daily HRV over the same period, for the sleep-vs-HRV correlation.
    required List<CrossMetricValue> crossMetricHrvValues,

    // ── The screen's own reading of the above ────────────────────────────────
    /// The nights that actually recorded sleep.
    required List<SleepDurationPoint> nights,
    required double totalHours,
    required double averageHours,
    required double longestHours,
    required double previousAverageHours,
    required List<PeriodChartValue> chartValues,
    required List<SleepScheduleDay> scheduleDays,
    required bool useScheduleChart,
    required List<SleepStageShare> stageShares,
    required DailyGoalProgress goalProgress,
    required PeriodComparison periodComparison,
    required PersonalBaselineInsight? baselineInsight,
    required SleepTargetInterpretation? targetInterpretation,
    required CrossMetricInsight? hrvInsight,
    required DataConfidence dataConfidence,

    /// The entry lists, newest night first.
    required Map<LocalDate, List<SleepData>> sortedSessionsByDate,
    required List<SleepData> sortedDailySessions,
    required List<SleepData> sortedPeriodSessions,
    required String? dayTimeRangeText,
  }) = _SleepDisplay;

  /// Only a single night can be opened. With two or more sessions
  /// [dailySummary] is a MERGED summary whose id belongs to no record, so there
  /// is nothing for the detail screen to load — Kotlin gated on
  /// `dailySessions.singleOrNull()` for exactly this.
  String? get openableDailySessionId =>
      dailySessions.length == 1 ? dailySessions.single.id : null;
}

const int _minutesPerDay = 24 * 60;

/// Builds the sleep display for [result] over the selected period. Port of the
/// Kotlin `SleepPresentationMapper.build`, trimmed to the primary sections, plus
/// the statistics the screen used to fold on every rebuild.
///
/// Pure: no clock, no ref, no I/O, no l10n — a fixture list is all it takes.
SleepDisplay buildSleepDisplay({
  required SleepPeriodLoadResult result,
  required TimeRange selectedRange,
  required LocalDate selectedDate,
  required SleepRangeMode sleepRangeMode,
  required WeekPeriodMode weekPeriodMode,
  required double dailyGoalHours,
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

  // Split the selected day into the night and its daytime naps: the timeline
  // card, duration and stage breakdown are the night only, naps reported apart.
  final daySplit = splitNightAndNaps(
    sleepSessionsForRange(result.sessions, selectedDate, sleepRangeMode),
  );
  final dailySessions = daySplit.night;
  final dayNaps = daySplit.naps;
  final dailySummary = dailySleepSummary(
    result.sessions,
    selectedDate,
    sleepRangeMode: sleepRangeMode,
  );

  final durationPoints = _sleepDurationPoints(
    result.sessions,
    selectedPeriod,
    sleepRangeMode,
  );
  final previousDurationPoints = _sleepDurationPoints(
    result.previousSessions,
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
    selectedPeriod,
    sleepRangeMode,
  );

  final baselineDurationPoints = _sleepDurationPoints(
    result.baselineSessions,
    baselinePeriodBefore(selectedPeriod),
    sleepRangeMode,
  );

  final overviewSummary = _overviewSummary(overviewDays);
  final sessionsByDate = {
    for (final date in _datesInPeriod(selectedPeriod.start, selectedPeriod.end))
      date: sleepSessionsForRange(result.sessions, date, sleepRangeMode),
  };
  final periodSessions = [
    for (final sessions in sessionsByDate.values) ...sessions,
  ];
  final crossMetricHrvValues = [
    for (final hrv in result.crossDailyHrv)
      CrossMetricValue(date: hrv.date, value: hrv.rmssdMs),
  ];

  final isDay = selectedRange == TimeRange.day;
  final nights = sleepNights(durationPoints);
  final averageHours = sleepAverageHours(nights);
  final previousAverageHours =
      sleepAverageHours(sleepNights(previousDurationPoints));
  final scheduleDays = toSleepScheduleDays(sessionsByDate);
  final confidenceSessions = isDay ? dailySessions : periodSessions;

  return SleepDisplay(
    period: selectedPeriod,
    isDay: isDay,
    dailySessions: dailySessions,
    dayNaps: dayNaps,
    dailySummary: dailySummary,
    durationPoints: durationPoints,
    previousDurationPoints: previousDurationPoints,
    baselineDurationPoints: baselineDurationPoints,
    overviewSummary: overviewSummary,
    sessionsByDate: sessionsByDate,
    periodSessions: periodSessions,
    crossMetricHrvValues: crossMetricHrvValues,
    nights: nights,
    totalHours: nights.fold(0.0, (sum, night) => sum + night.hours),
    averageHours: averageHours,
    longestHours: nights.isEmpty
        ? 0.0
        : nights.map((n) => n.hours).reduce((a, b) => a > b ? a : b),
    previousAverageHours: previousAverageHours,
    chartValues: [
      for (final point in durationPoints)
        PeriodChartValue(point.date, point.hours),
    ],
    scheduleDays: scheduleDays,
    // Kotlin `useScheduleChart`: week/month only, and only once some night has
    // a bedtime — the schedule axis is meaningless without one.
    useScheduleChart: (selectedRange == TimeRange.week ||
            selectedRange == TimeRange.month) &&
        scheduleDays.any((day) => day.inBedStart != null),
    stageShares: sleepStageShares(overviewSummary),
    goalProgress: sleepGoalProgress(
      durationPoints: durationPoints,
      period: selectedPeriod,
      targetHours: dailyGoalHours,
    ),
    // Sleep compares averages, not totals: a 5-night week is not worse than a
    // 7-night one just because it has fewer nights.
    periodComparison: periodComparison(averageHours, previousAverageHours),
    baselineInsight: personalBaselineInsight(
      averageHours,
      [
        for (final point in baselineDurationPoints)
          BaselineValue(date: point.date, value: point.hours),
      ],
      selectedPeriod.start.minusDays(1),
    ),
    targetInterpretation: nights.isEmpty
        ? null
        : sleepTargetInterpretation(averageHours, dailyGoalHours),
    hrvInsight: crossMetricInsight(
      [
        for (final point in durationPoints)
          CrossMetricValue(date: point.date, value: point.hours),
      ],
      crossMetricHrvValues,
    ),
    dataConfidence: dataConfidence(
      selectedPeriod,
      [for (final night in nights) night.date],
      confidenceSessions.length,
      sources: [for (final session in confidenceSessions) session.source],
      valueKind: DataValueKind.measured,
      manualEntryCount: confidenceSessions
          .where((session) =>
              session.recordingMethod == RecordingMethod.manualEntry)
          .length,
    ),
    sortedSessionsByDate: {
      for (final entry in sessionsByDate.entries)
        entry.key: _newestNightFirst(entry.value),
    },
    sortedDailySessions: _newestNightFirst(dailySessions),
    sortedPeriodSessions: _newestNightFirst(periodSessions),
    dayTimeRangeText: _dayTimeRangeText(dailySessions),
  );
}

/// The nights that actually recorded sleep. A zero-hour point is a night the
/// period covers but nothing was logged.
List<SleepDurationPoint> sleepNights(List<SleepDurationPoint> points) =>
    [for (final point in points) if (point.hours > 0.0) point];

/// Zero, not null, when nothing was logged: a period with no sleep really did
/// average zero hours, and the tiles and charts render it as such.
double sleepAverageHours(List<SleepDurationPoint> nights) =>
    averageOrZero(nights.map((night) => night.hours));

/// Kotlin `sleepGoalProgress`.
DailyGoalProgress sleepGoalProgress({
  required List<SleepDurationPoint> durationPoints,
  required DatePeriod period,
  required double targetHours,
}) =>
    dailyGoalProgress(
      [
        for (final point in durationPoints)
          DailyGoalValue(date: point.date, value: point.hours),
      ],
      period,
      targetHours,
      MetricDailyGoalKey.sleepHours.direction,
    );

/// The stage rows of the "share of time in bed" card, in Kotlin's order. Empty
/// when no stage was recorded — the card hides itself on that.
List<SleepStageShare> sleepStageShares(SleepOverviewSummary summary) {
  final rows = <(int, int)>[
    (SleepStage.stageAwake, summary.awakeDurationMs),
    (SleepStage.stageRem, summary.remDurationMs),
    (SleepStage.stageLight, summary.coreDurationMs),
    (SleepStage.stageDeep, summary.deepDurationMs),
  ].where((row) => row.$2 > 0).toList();
  final totalMs = rows.fold<int>(0, (sum, row) => sum + row.$2);
  if (totalMs <= 0) return const <SleepStageShare>[];
  return [
    for (final row in rows)
      SleepStageShare(
        stageType: row.$1,
        durationMs: row.$2,
        fraction: (row.$2 / totalMs).clamp(0.0, 1.0),
        percent: ((row.$2 / totalMs) * 100).round(),
      ),
  ];
}

/// Kotlin `List<SleepOverviewDay>.toSleepScheduleDays()`.
List<SleepScheduleDay> toSleepScheduleDays(
  Map<LocalDate, List<SleepData>> sessionsByDate,
) {
  final days = <SleepScheduleDay>[];
  final dates = sessionsByDate.keys.toList()..sort();
  for (final date in dates) {
    final sessions = sessionsByDate[date]!;
    // Fill the wake gaps between a night's segments with Awake so the schedule
    // bar is continuous instead of holed; gaps beyond kSleepNapGap (a daytime
    // nap) are left as a separate block. Sessions are already start-sorted.
    final stages = combineNightStages(sessions, maxGap: kSleepNapGap);
    days.add(SleepScheduleDay(
      date: date,
      inBedStart: sessions.isEmpty
          ? null
          : sessions.map((s) => s.startTime).reduce((a, b) => a.isBefore(b) ? a : b),
      inBedEnd: sessions.isEmpty
          ? null
          : sessions.map((s) => s.endTime).reduce((a, b) => a.isAfter(b) ? a : b),
      stages: stages,
    ));
  }
  return days;
}

List<SleepData> _newestNightFirst(List<SleepData> sessions) =>
    [...sessions]..sort((a, b) => b.endTime.compareTo(a.endTime));

final DateFormat _timeFormat = DateFormat('HH:mm');

String? _dayTimeRangeText(List<SleepData> sessions) {
  if (sessions.isEmpty) return null;
  final sorted = [...sessions]
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  return sorted
      .map((s) =>
          '${_timeFormat.format(s.startTime.toLocal())} - ${_timeFormat.format(s.endTime.toLocal())}')
      .join(' | ');
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

/// The hours actually **asleep** for [date]'s night: sleep-stage time only, so
/// wake epochs inside the session (WASO) are not counted toward the sleep
/// duration or the daily sleep goal — total sleep time is time in bed minus the
/// time awake, not time in bed. Falls back to wall-clock time in bed when the
/// night has no stage data to subtract awake from (an unstaged session is all we
/// can measure).
///
/// Research: total sleep time / sleep efficiency define sleep as time asleep, not
/// time in bed (AASM scoring; https://pmc.ncbi.nlm.nih.gov/articles/PMC5971842/).
/// See [sleepDurationMsFromStages].
double nightAsleepHours(
  List<SleepData> sessions,
  LocalDate date, {
  SleepRangeMode sleepRangeMode = SleepRangeMode.evening18h,
}) {
  final summary =
      dailySleepSummary(sessions, date, sleepRangeMode: sleepRangeMode);
  if (summary == null) return 0.0;
  return sleepDurationMsFromStages(summary.stages, summary.durationMs) /
      3600000.0;
}

List<SleepDurationPoint> _sleepDurationPoints(
  List<SleepData> sessions,
  DatePeriod period,
  SleepRangeMode sleepRangeMode,
) {
  // Hours ASLEEP per night (wake time within the session excluded), grouped by
  // the same window the sessions are. (The old Health-Connect daily aggregate
  // keyed sleep by its START date, so a night crossing midnight landed on the
  // wrong day — see dailySleepSummary.)
  return [
    for (final date in _datesInPeriod(period.start, period.end))
      SleepDurationPoint(
        date,
        nightAsleepHours(sessions, date, sleepRangeMode: sleepRangeMode),
      ),
  ];
}

/// One overview day: the night's sessions (naps excluded) + a sleep-score estimate.
class _OverviewDay {
  const _OverviewDay(this.sessions, this.sleepScore);

  /// The night's sessions only — daytime naps are reported separately.
  final List<SleepData> sessions;
  final SleepScoreEstimate sleepScore;

  /// Time actually **asleep**: sleep-stage time with wake excluded (falling back
  /// to the in-bed union when a night carries no stages). This is the figure the
  /// overview highlights and the goal counts — not time in bed.
  int get sleepDurationMs => sleepDurationMsFromStages(
        combineNightStages(sessions, maxGap: kSleepNapGap),
        sleepSessionsUnionMs(sessions),
      );

  /// Time in bed: the full span from the night's first bedtime to its last wake,
  /// wake gaps included (AASM "time in bed", and the sleep-efficiency
  /// denominator). Distinct from [sleepDurationMs], which is time asleep.
  int get timeInBedMs {
    if (sessions.isEmpty) return 0;
    final start = sessions
        .map((s) => s.startTime)
        .reduce((a, b) => a.isBefore(b) ? a : b);
    final end =
        sessions.map((s) => s.endTime).reduce((a, b) => a.isAfter(b) ? a : b);
    return math.max(0, end.difference(start).inMilliseconds);
  }

  int _stageMs(Set<int> types) =>
      combineNightStages(sessions, maxGap: kSleepNapGap)
          .durationMsForTypes(types);

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
  DatePeriod period,
  SleepRangeMode sleepRangeMode,
) {
  return [
    for (final date in _datesInPeriod(period.start, period.end))
      _OverviewDay(
        splitNightAndNaps(
          sleepSessionsForRange(sessions, date, sleepRangeMode),
        ).night,
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

List<SleepData> _distinctById(List<SleepData> sessions) {
  final seen = <String>{};
  final result = <SleepData>[];
  for (final s in sessions) {
    if (seen.add(s.id)) result.add(s);
  }
  return result;
}
