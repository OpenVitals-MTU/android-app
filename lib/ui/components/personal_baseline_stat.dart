import 'package:flutter/material.dart';

import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/insights/personal_baseline.dart';
import '../../l10n/app_localizations.dart';
import 'insight_cards.dart';

/// Port of the Kotlin `PersonalBaselineStat.kt`.

String _baselineStatusLabel(BaselineStatus status, AppLocalizations l10n) =>
    switch (status) {
      BaselineStatus.usual => l10n.baselineStatusUsual,
      BaselineStatus.above => l10n.baselineStatusAbove,
      BaselineStatus.below => l10n.baselineStatusBelow,
      BaselineStatus.unusualHigh => l10n.baselineStatusUnusualHigh,
      BaselineStatus.unusualLow => l10n.baselineStatusUnusualLow,
    };

/// Kotlin `signedValue(value, status)`.
String _signedValue(String value, BaselineStatus status) => switch (status) {
      BaselineStatus.above || BaselineStatus.unusualHigh => '+$value',
      BaselineStatus.below || BaselineStatus.unusualLow => '-$value',
      BaselineStatus.usual => value,
    };

IconData _statusIcon(BaselineStatus status) => switch (status) {
      BaselineStatus.usual => Icons.trending_flat,
      BaselineStatus.above || BaselineStatus.unusualHigh => Icons.trending_up,
      BaselineStatus.below || BaselineStatus.unusualLow => Icons.trending_down,
    };

/// Kotlin `personalBaselineInsightStats`: one card per baseline window, then the
/// usual-range verdict and the deviation. Empty when there is no baseline yet.
List<InsightStat> personalBaselineInsightStats({
  required PersonalBaselineInsight? insight,
  required UnitFormatter unitFormatter,
  required DisplayValue Function(double value) valueFormatter,
  required Color accentColor,
  required AppLocalizations l10n,
}) {
  if (insight == null) return const [];

  final stats = <InsightStat>[
    for (final summary in insight.summaries)
      InsightStat(
        title: switch (summary.windowDays) {
          30 => l10n.stat30DayBaseline,
          60 => l10n.stat60DayBaseline,
          90 => l10n.stat90DayBaseline,
          _ => l10n.statBaseline,
        },
        value: valueFormatter(summary.average).value,
        unit: valueFormatter(summary.average).unit,
        icon: Icons.calendar_month_outlined,
        accentColor: accentColor,
      ),
  ];

  final percent = insight.percentDeviation;
  final deviation = percent != null
      ? DisplayValue(
          _signedValue(unitFormatter.count(percent.abs().round()), insight.status),
          l10n.unitPercentSymbol,
        )
      : () {
          final absolute = valueFormatter(insight.absoluteDeviation);
          return DisplayValue(
            _signedValue(absolute.value, insight.status),
            absolute.unit,
          );
        }();

  return [
    ...stats,
    InsightStat(
      title: l10n.statUsualRange,
      value: _baselineStatusLabel(insight.status, l10n),
      unit: '',
      icon: _statusIcon(insight.status),
      accentColor: accentColor,
    ),
    InsightStat(
      title: l10n.statBaselineDeviation,
      value: deviation.value,
      unit: deviation.unit,
      icon: Icons.star_outline,
      accentColor: accentColor,
    ),
  ];
}
