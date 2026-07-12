import 'package:flutter/material.dart';

import '../../../core/time/local_date.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/day_axis.dart';
import '../../../ui/charts/metric_day_chart.dart';
import '../application/activity_metric_display.dart';

/// The day's steps / calories / distance, as they accumulated.
class IntradayActivityChartCard extends StatelessWidget {
  const IntradayActivityChartCard({
    super.key,
    required this.selectedDate,
    required this.title,
    required this.valueText,
    required this.emptyText,
    required this.points,
    required this.accentColor,
    this.valueFormatter = formatCompactAxisValue,
    this.now,
  });

  final LocalDate selectedDate;
  final String title;
  final String valueText;
  final String emptyText;
  final List<ActivityIntradayPoint> points;
  final Color accentColor;
  final String Function(double value) valueFormatter;

  /// Injectable clock: today's series stops at "now", a past day's runs to midnight.
  final DateTime? now;

  @override
  Widget build(BuildContext context) {
    final samples = [
      for (final point in points) (time: point.time, value: point.value),
    ];
    final total = samples.isEmpty ? 0.0 : samples.last.value;

    return MetricDayChart(
      axis: DayAxis(selectedDate, now: now),
      samples: samples,
      shape: DaySeriesShape.cumulative,
      // Cumulative, so the last sample is the day's maximum. Floored at 1 so a day
      // with a single step does not draw as a full-height climb.
      range: ChartRange(0, total < 1.0 ? 1.0 : total),
      accentColor: accentColor,
      metricName: title,
      emptyLabel: emptyText,
      headlineText: valueText,
      valueFormatter: valueFormatter,
    );
  }
}
