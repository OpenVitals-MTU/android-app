import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/time/local_date.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/metric_line_plot.dart';
import '../../../ui/components/ov_card.dart';
import 'activity_metric_display.dart';

/// Port of the Kotlin `IntradayActivityChartCard`: the cumulative curve of a
/// metric across one day, plotted against wall-clock time.
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

  /// Injectable clock: the x axis ends at "now" for today, midnight otherwise.
  final DateTime? now;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final currentTime = now ?? DateTime.now();
    final isToday = selectedDate == LocalDate.now();

    final dayStart =
        DateTime(selectedDate.year, selectedDate.month, selectedDate.day);
    final chartEnd = isToday ? currentTime : dayStart.add(const Duration(days: 1));
    // Never zero: a same-instant window would divide by zero below.
    final elapsedMillis =
        (chartEnd.difference(dayStart).inMilliseconds).clamp(1, 1 << 62);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              valueText,
              style: theme.textTheme.headlineMedium?.copyWith(color: accentColor),
            ),
            Text(
              isToday
                  ? l10n.summaryToday(title)
                  : l10n.summaryOnDate(
                      title,
                      DateFormat.yMMMd(locale).format(dayStart),
                    ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            if (points.isEmpty)
              Text(
                isToday
                    ? l10n.summaryEmptyToday(emptyText)
                    : l10n.summaryEmptyDay(emptyText),
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else ...[
              MetricLinePlot(
                points: _plotPoints(dayStart, elapsedMillis),
                minValue: 0,
                // Cumulative: the last sample is the day's maximum.
                maxValue: points.last.value < 1.0 ? 1.0 : points.last.value,
                accentColor: accentColor,
                valueFormatter: valueFormatter,
              ),
              const SizedBox(height: 8),
              ChartXAxisWithYAxis(
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    for (final label in [
                      '00:00',
                      '06:00',
                      '12:00',
                      '18:00',
                      if (isToday) l10n.summaryNow else '24:00',
                    ])
                      Text(
                        label,
                        style: theme.textTheme.labelSmall
                            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Text(
                l10n.summaryLastUpdate(
                  DateFormat.jm(locale).format(points.last.time.toLocal()),
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ],
        ),
      ),
    );
  }

  /// Kotlin anchors the curve at (0, 0) and extends the last value out to the
  /// right edge, so a day that stopped updating reads as a plateau, not a drop.
  List<MetricLinePlotPoint> _plotPoints(DateTime dayStart, int elapsedMillis) {
    return [
      const MetricLinePlotPoint(xFraction: 0, value: 0),
      for (final point in points)
        MetricLinePlotPoint(
          xFraction: (point.time.toLocal().difference(dayStart).inMilliseconds)
                  .clamp(0, elapsedMillis) /
              elapsedMillis,
          value: point.value,
        ),
      MetricLinePlotPoint(xFraction: 1, value: points.last.value),
    ];
  }
}
