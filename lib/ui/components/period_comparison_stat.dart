import 'package:flutter/material.dart';

import '../../core/period/time_range.dart';
import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/insights/period_comparison.dart';
import '../../l10n/app_localizations.dart';
import 'insight_cards.dart';

/// Port of the Kotlin `PeriodComparisonStat.kt`.

/// Kotlin `signedValue(value, direction)`.
String _signedValue(String value, PeriodComparisonDirection direction) =>
    switch (direction) {
      PeriodComparisonDirection.up => '+$value',
      PeriodComparisonDirection.down => '-$value',
      PeriodComparisonDirection.same => value,
    };

IconData _directionIcon(PeriodComparisonDirection direction) =>
    switch (direction) {
      PeriodComparisonDirection.up => Icons.trending_up,
      PeriodComparisonDirection.down => Icons.trending_down,
      PeriodComparisonDirection.same => Icons.trending_flat,
    };

/// Kotlin `previousPeriodInsightStat`: "Vs previous week, +12 %".
///
/// Falls back to the absolute change when the previous period was ~zero and a
/// percentage would be meaningless (or infinite).
InsightStat previousPeriodInsightStat({
  required PeriodComparison comparison,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
  required DisplayValue Function(double value) valueFormatter,
  required Color accentColor,
  required AppLocalizations l10n,
}) {
  final title = switch (selectedRange) {
    TimeRange.day => l10n.statVsPreviousDay,
    TimeRange.week => l10n.statVsPreviousWeek,
    TimeRange.month => l10n.statVsPreviousMonth,
    TimeRange.year => l10n.statVsPreviousYear,
  };

  final percent = comparison.percentChange;
  final display = percent != null
      ? DisplayValue(
          _signedValue(
            unitFormatter.count(percent.abs().round()),
            comparison.direction,
          ),
          l10n.unitPercentSymbol,
        )
      : () {
          final absolute = valueFormatter(comparison.absoluteChange);
          return DisplayValue(
            _signedValue(absolute.value, comparison.direction),
            absolute.unit,
          );
        }();

  return InsightStat(
    title: title,
    value: display.value,
    unit: display.unit,
    icon: _directionIcon(comparison.direction),
    accentColor: accentColor,
  );
}
