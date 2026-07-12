import 'package:flutter/material.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/day_axis.dart';
import '../../../ui/charts/metric_day_chart.dart';
import '../../../ui/theme/app_colors.dart';

/// The day's mindfulness, as it accumulated.
///
/// Kotlin had this card; the Flutter port dropped it, so the Day range fell back to
/// the WEEK chart with a single day in it — one fat bar, which repeats the number
/// already printed on the card above it and tells you nothing else. A day is a
/// shape: when you sat, and for how long.
///
/// The curve arrives precomputed (`cumulativeMindfulness`, built by the
/// view-model); this card only paints it.
class MindfulnessIntradayChartCard extends StatelessWidget {
  const MindfulnessIntradayChartCard({
    super.key,
    required this.selectedDate,
    required this.samples,
    required this.formatter,
    this.now,
  });

  final LocalDate selectedDate;
  final List<DaySample> samples;
  final UnitFormatter formatter;

  /// Injectable clock: today's line stops at "now", a past day's runs to midnight.
  final DateTime? now;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final totalMinutes = samples.isEmpty ? 0.0 : samples.last.value;

    return MetricDayChart(
      axis: DayAxis(selectedDate, now: now),
      samples: samples,
      shape: DaySeriesShape.cumulative,
      range: ChartRange(0, totalMinutes < 1 ? 1 : totalMinutes),
      accentColor: AppColors.mindfulness,
      metricName: l10n.metricMindfulness,
      emptyLabel: l10n.screenMindfulness,
      headlineText: formatter.minutes(totalMinutes.round()).text,
      valueFormatter: (value) => formatter.minutes(value.round()).text,
    );
  }
}
