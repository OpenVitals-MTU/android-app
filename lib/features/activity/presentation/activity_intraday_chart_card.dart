import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/time/local_date.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/day_axis.dart';
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
    // The x axis is the WHOLE day, and a point sits at its real hour. This used to
    // scale by the time ELAPSED so far, so on a chart opened at 12:49 a reading from
    // 09:29 landed at 74% of the width -- under a fixed 00:00/06:00/12:00/18:00 axis
    // that put it at quarter past five. See [DayAxis]: the same twenty lines were
    // written five times and four of them were wrong the same way.
    final axis = DayAxis(selectedDate, now: now);
    final isToday = axis.isToday;

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
                      DateFormat.yMMMd(locale).format(axis.start),
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
                points: _plotPoints(axis),
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
                    for (final label in const [
                      '00:00',
                      '06:00',
                      '12:00',
                      '18:00',
                      '24:00',
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

  /// Anchored at (0, 0) and held out to [DayAxis.endFraction], so a day that
  /// stopped updating reads as a plateau rather than a drop -- but only as far as
  /// NOW, never to the right edge, because the rest of today has not happened.
  List<MetricLinePlotPoint> _plotPoints(DayAxis axis) => [
        const MetricLinePlotPoint(xFraction: 0, value: 0),
        for (final point in points)
          MetricLinePlotPoint(
            xFraction: axis.fractionOf(point.time),
            value: point.value,
          ),
        MetricLinePlotPoint(
          xFraction: axis.endFraction,
          value: points.last.value,
        ),
      ];
}
