import 'package:flutter/material.dart';

import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../../l10n/app_localizations.dart';
import 'bar_chart.dart';
import 'chart_axis.dart';
import 'heatmap_chart.dart';

export 'bar_chart.dart' show PeriodChartValue, PeriodBarAggregation;

/// The period history chart: dispatches to a month calendar heatmap (MONTH), a
/// full-year dot heatmap (YEAR) or a bar chart (DAY/WEEK) based on the selected
/// range. Port of Kotlin `PeriodHistoryChart`.
class PeriodHistoryChart extends StatelessWidget {
  const PeriodHistoryChart({
    super.key,
    required this.title,
    required this.values,
    required this.selectedRange,
    required this.period,
    required this.accentColor,
    required this.summaryText,
    this.yearAggregation = PeriodBarAggregation.sum,
    this.selectedDate,
    this.onDateSelected,
    this.valueFormatter = formatCompactAxisValue,
    this.weekPeriodMode = WeekPeriodMode.mondayToSunday,
  });

  final String title;
  final List<PeriodChartValue> values;
  final TimeRange selectedRange;
  final DatePeriod period;
  final Color accentColor;
  final String summaryText;
  final PeriodBarAggregation yearAggregation;
  final WeekPeriodMode weekPeriodMode;
  final LocalDate? selectedDate;
  final ValueChanged<LocalDate>? onDateSelected;
  final String Function(double) valueFormatter;

  @override
  Widget build(BuildContext context) {
    switch (selectedRange) {
      case TimeRange.month:
        return PeriodMonthHeatmap(
          title: title,
          values: values,
          period: period,
          accentColor: accentColor,
          summaryText: summaryText,
          selectedDate: selectedDate,
          onDateSelected: onDateSelected,
          rolling: weekPeriodMode.usesRollingDates,
        );
      case TimeRange.year:
        return PeriodYearHeatmap(
          title: title,
          values: values,
          period: period,
          accentColor: accentColor,
          summaryText: summaryText,
        );
      case TimeRange.day:
      case TimeRange.week:
        return PeriodBarChart(
          title: title,
          values: values,
          selectedRange: selectedRange,
          period: period,
          accentColor: accentColor,
          summaryText: summaryText,
          yearAggregation: yearAggregation,
          selectedDate: selectedDate,
          onDateSelected: onDateSelected,
          valueFormatter: valueFormatter,
        );
    }
  }
}

/// Convenience over [PeriodHistoryChart] that builds the summary string from the
/// period title and a single summary value, and applies the accent alpha. Port
/// of Kotlin `MetricBarChart`.
class MetricBarChart extends StatelessWidget {
  const MetricBarChart({
    super.key,
    required this.title,
    required this.values,
    required this.selectedRange,
    required this.period,
    required this.accentColor,
    required this.summaryValue,
    this.weekPeriodMode = WeekPeriodMode.mondayToSunday,
    this.accentAlpha = 0.85,
    this.yearAggregation = PeriodBarAggregation.sum,
    this.selectedDate,
    this.onDateSelected,
    this.valueFormatter = formatCompactAxisValue,
  });

  final String title;
  final List<PeriodChartValue> values;
  final TimeRange selectedRange;
  final DatePeriod period;
  final Color accentColor;
  final String summaryValue;

  /// The week/period mode, so the summary's period title agrees with the period
  /// navigator above it ("Last 30 days", not "This month", on a rolling month).
  /// Threaded in by every screen; the calendar default only serves tests.
  final WeekPeriodMode weekPeriodMode;
  final double accentAlpha;
  final PeriodBarAggregation yearAggregation;
  final LocalDate? selectedDate;
  final ValueChanged<LocalDate>? onDateSelected;
  final String Function(double) valueFormatter;

  @override
  Widget build(BuildContext context) {
    return PeriodHistoryChart(
      title: title,
      values: values,
      selectedRange: selectedRange,
      period: period,
      accentColor: accentColor.withValues(alpha: accentAlpha),
      summaryText: '${periodTitle(
        AppLocalizations.of(context),
        selectedRange,
        period,
        weekPeriodMode: weekPeriodMode,
      )} · $summaryValue',
      yearAggregation: yearAggregation,
      selectedDate: selectedDate,
      onDateSelected: onDateSelected,
      valueFormatter: valueFormatter,
      weekPeriodMode: weekPeriodMode,
    );
  }
}
