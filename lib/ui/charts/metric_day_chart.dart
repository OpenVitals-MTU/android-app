import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../l10n/app_localizations.dart';
import '../components/ov_card.dart';
import 'chart_axis.dart';
import 'chart_empty_state.dart';
import 'day_axis.dart';
import 'metric_line_plot.dart';

/// One metric across one day, as a whole card.
///
/// This is the Day-range sibling of [MetricLineChart] and [MetricBarChart], and it
/// is the card Kotlin should have written but never did.
///
/// Kotlin's chart layer had two clean tiers: complete cards you call once
/// (`MetricLineChart`, `PeriodBarChart`, the heatmaps — each renders its own
/// `OpenVitalsCard`, title, plot, axis and summary), sitting on plot-only
/// primitives (`MetricLinePlot`, `YAxisChart`). It is a good design and this app
/// ported it faithfully.
///
/// Except for the day charts. There Kotlin dropped to the raw plot primitive and
/// hand-built the whole card — six times, for hydration, nutrition, body, activity,
/// mindfulness and heart. Every copy re-derived where an instant sits in the day,
/// and every copy got it wrong the same way: they scaled x by the time ELAPSED so
/// far, so at 12:49 a 09:29 reading was drawn at 74% of the width, under an axis
/// labelled `00:00 / 06:00 / 12:00 / 18:00`. The chart's only job is to say WHEN,
/// and all six said the wrong hour. We inherited all six.
///
/// So the day chart becomes what its siblings already are: one call, one card. The
/// per-metric parts stay with the metric — the data, the colour, the y bounds, the
/// words. Everything a day chart KNOWS lives here, once.
class MetricDayChart extends StatelessWidget {
  const MetricDayChart({
    super.key,
    required this.axis,
    required this.samples,
    required this.shape,
    required this.range,
    required this.accentColor,
    required this.metricName,
    required this.emptyLabel,
    this.header,
    this.footer,
    this.valueFormatter = formatCompactAxisValue,
    this.headlineText,
    this.drawPoints = false,
    this.pointRadius = 3.5,
    this.lineStrokeWidth = 3,
  });

  final DayAxis axis;
  final List<DaySample> samples;
  final DaySeriesShape shape;
  final ChartRange range;
  final Color accentColor;

  /// Names the metric in the "Today" / "On `<date>`" subtitle.
  final String metricName;

  /// Names the thing there is none of, in the empty message.
  final String emptyLabel;

  /// The big number above the chart. Defaults to the last sample, formatted.
  final String? headlineText;

  /// Replaces the default [DayChartHeader] — the heart timeline puts a row of
  /// statistics here instead of a headline.
  final Widget? header;

  /// Replaces the default "last update" line — the heart timeline reports the
  /// recording window instead.
  final Widget? footer;

  final String Function(double) valueFormatter;
  final bool drawPoints;
  final double pointRadius;
  final double lineStrokeWidth;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();

    final ordered = [...samples]..sort((a, b) => a.time.compareTo(b.time));

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            header ??
                DayChartHeader(
                  axis: axis,
                  value: headlineText ??
                      (ordered.isEmpty
                          ? l10n.noData
                          : valueFormatter(ordered.last.value)),
                  metricName: metricName,
                  accentColor: accentColor,
                  dateText: DateFormat.yMMMd(locale).format(axis.start),
                ),
            const SizedBox(height: 16),
            if (ordered.isEmpty)
              ChartEmptyState(
                message: axis.isToday
                    ? l10n.summaryEmptyToday(emptyLabel)
                    : l10n.summaryEmptyDay(emptyLabel),
              )
            else ...[
              MetricLinePlot(
                points: shape.plot(ordered, axis),
                minValue: range.min,
                maxValue: range.max,
                accentColor: accentColor,
                valueFormatter: valueFormatter,
                drawPoints: drawPoints,
                pointRadius: pointRadius,
                lineStrokeWidth: lineStrokeWidth,
              ),
              const SizedBox(height: 8),
              const DayAxisLabels(),
              const SizedBox(height: 12),
              footer ??
                  Text(
                    l10n.summaryLastUpdate(
                      DateFormat.jm(locale).format(ordered.last.time.toLocal()),
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
}

/// A reading, and when it was taken.
typedef DaySample = ({DateTime time, double value});

/// How a day's readings become a line.
///
/// A property of the DATA, not of the screen — a running total is drawn the same
/// way whether it is water or calories — so it lives here, named, tested once,
/// rather than as an inline point-builder in each of six features.
///
/// There used to be a third shape, `step`, which drew hydration as risers and flat
/// stretches so that "nothing since nine" was legible as a flat line. The lines are
/// now smoothed, and a curve through a step is just the cumulative curve — so the
/// step is gone and hydration is cumulative like everything else. The cost, stated
/// plainly: between two drinks the curve slopes gently upward through hours you
/// drank nothing.
enum DaySeriesShape {
  /// A running total: climbs from nothing at midnight, never falls.
  ///
  /// Anchored at `(0, 0)` and held flat at the last value out to
  /// [DayAxis.endFraction] — so a day that stopped accumulating reads as a plateau
  /// rather than a cliff, but only as far as NOW, because the rest of today has not
  /// happened yet.
  cumulative,

  /// The readings themselves, unanchored — weight, heart rate. A reading at 08:00
  /// is a fact about 08:00 and implies nothing about midnight.
  raw;

  List<MetricLinePlotPoint> plot(List<DaySample> ordered, DayAxis axis) {
    if (ordered.isEmpty) return const [];
    return switch (this) {
      DaySeriesShape.raw => [
          for (final sample in ordered)
            MetricLinePlotPoint(
              xFraction: axis.fractionOf(sample.time),
              value: sample.value,
            ),
        ],
      DaySeriesShape.cumulative => [
          const MetricLinePlotPoint(xFraction: 0, value: 0),
          for (final sample in ordered)
            MetricLinePlotPoint(
              xFraction: axis.fractionOf(sample.time),
              value: sample.value,
            ),
          MetricLinePlotPoint(
            xFraction: axis.endFraction,
            value: ordered.last.value,
          ),
        ],
    };
  }
}
