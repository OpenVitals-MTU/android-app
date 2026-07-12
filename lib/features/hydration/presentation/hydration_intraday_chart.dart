import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/day_axis.dart';
import '../../../ui/charts/metric_day_chart.dart';
import '../../../ui/theme/app_colors.dart';

/// The day's hydration, as it actually accumulated.
///
/// The Day view used to draw the WEEK chart with a single day in it: one fat bar
/// labelled "Sun 12", which says nothing the cards above it have not already said
/// twice. A day is not a bar. A day is a line that climbs from nothing at midnight
/// to whatever you have drunk by now, and its shape is the whole point: it shows
/// you that you drank everything at 9am and nothing since.
///
/// The curve arrives precomputed (`cumulativeHydration`, built by the
/// view-model); this card only paints it.
class HydrationIntradayChartCard extends StatelessWidget {
  const HydrationIntradayChartCard({
    super.key,
    required this.selectedDate,
    required this.samples,
    required this.dailyGoalLiters,
    required this.formatter,
    this.now,
  });

  final LocalDate selectedDate;
  final List<DaySample> samples;
  final double dailyGoalLiters;
  final UnitFormatter formatter;

  /// Injectable clock: today's line stops at "now", a past day's runs to midnight.
  final DateTime? now;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    final total = samples.isEmpty ? 0.0 : samples.last.value;

    return MetricDayChart(
      axis: DayAxis(selectedDate, now: now),
      samples: samples,
      shape: DaySeriesShape.cumulative,
      // The goal holds the axis open on a thin day: without it a single 200 ml
      // glass would fill the chart and read like a good one.
      range: ChartRange(0, math.max(math.max(total, dailyGoalLiters), 0.5)),
      accentColor: AppColors.hydration,
      metricName: l10n.metricHydration,
      emptyLabel: l10n.metricHydration,
      headlineText: formatter.hydration(total).text,
      valueFormatter: (value) => formatter.hydration(value).text,
    );
  }
}
