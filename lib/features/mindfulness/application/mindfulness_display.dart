import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/time/local_date.dart';
import '../../../domain/model/mindfulness_models.dart';
import '../../../domain/query/mindfulness_period_data.dart';
import '../../../ui/charts/bar_chart.dart';
import '../../../ui/charts/metric_day_chart.dart';

part 'mindfulness_display.freezed.dart';

/// The screen-ready derivation of one loaded mindfulness period: the summary
/// statistics, the daily bar-chart series, the cumulative day curve, and the
/// session list in display order.
///
/// Built once per load by [buildMindfulnessDisplay] and stored on the state —
/// the view-model precomputes, the widgets only render (the Kotlin
/// `MindfulnessDisplayState` discipline, restored).
@freezed
abstract class MindfulnessDisplay with _$MindfulnessDisplay {
  const MindfulnessDisplay._();

  const factory MindfulnessDisplay({
    required int totalMs,
    required int sessionCount,
    required int averageDurationMs,
    required int longestSessionMs,
    required List<PeriodChartValue> chartValues,
    required List<DaySample> cumulativeSamples,
    required List<MindfulnessSession> sortedSessions,
  }) = _MindfulnessDisplay;

  int get totalMinutes => totalMs ~/ 60000;
}

/// Pure derivation from the loaded period to its display model. No clock, no
/// I/O — unit-testable with a fixture list.
MindfulnessDisplay buildMindfulnessDisplay(MindfulnessPeriodData data) {
  final sessions = data.sessions;
  final totalMs =
      sessions.fold<int>(0, (sum, session) => sum + session.durationMs);
  final longest = sessions.fold<int>(
    0,
    (m, session) => math.max(m, session.durationMs),
  );
  final count = sessions.length;
  return MindfulnessDisplay(
    totalMs: totalMs,
    sessionCount: count,
    averageDurationMs: count > 0 ? totalMs ~/ count : 0,
    longestSessionMs: longest,
    chartValues: _chartValues(sessions),
    cumulativeSamples: cumulativeMindfulness(sessions),
    sortedSessions: [...sessions]
      ..sort((a, b) => b.startTime.compareTo(a.startTime)),
  );
}

/// Total minutes per day, for the WEEK/MONTH/YEAR bar chart.
List<PeriodChartValue> _chartValues(List<MindfulnessSession> sessions) {
  final minutesByDate = <LocalDate, double>{};
  for (final session in sessions) {
    final date = instantToLocalDate(session.startTime);
    final minutes = math.max(0, session.durationMs) / 60000.0;
    minutesByDate.update(date, (value) => value + minutes,
        ifAbsent: () => minutes);
  }
  return [
    for (final entry in minutesByDate.entries)
      PeriodChartValue(entry.key, entry.value),
  ];
}

/// `(end time, running total minutes)` per session. Kotlin
/// `cumulativeMindfulnessPoints()`.
///
/// Keyed to when each session ENDED: the minutes are only in the bank once you
/// have actually sat them.
List<DaySample> cumulativeMindfulness(List<MindfulnessSession> sessions) {
  final ordered = [...sessions]..sort((a, b) => a.endTime.compareTo(b.endTime));
  var running = 0.0;
  return [
    for (final session in ordered)
      if (session.durationMs > 0)
        (
          time: session.endTime,
          value: running += session.durationMs / Duration.millisecondsPerMinute,
        ),
  ];
}
