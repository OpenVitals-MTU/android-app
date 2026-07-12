import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../l10n/app_localizations.dart';
import '../components/ov_card.dart';
import 'chart_axis.dart';
import 'metric_line_plot.dart';
import 'session_axis.dart';

/// One metric traced across one recorded session, as a whole card.
///
/// The session sibling of [MetricDayChart]: a title, the average / range / sample
/// count, the trace, elapsed labels, and the window it was recorded in.
///
/// Heart rate, speed and cadence each had their own copy of this card. They were
/// structural clones — same scaffold, same stat row, same plot settings
/// (`chartHeight: 180`, `lineStrokeWidth: 2`, points only when the trace is sparse
/// enough to see them), same footer — differing only in accent colour and in how
/// they padded the y axis. The tell was already in the imports: the speed/cadence
/// file reached into the heart-rate file for `sessionElapsedLabels`, because the
/// shared helper had nowhere else to live.
class MetricSessionChart extends StatelessWidget {
  const MetricSessionChart({
    super.key,
    required this.title,
    required this.axis,
    required this.samples,
    required this.range,
    required this.accentColor,
    required this.valueFormatter,
    required this.countText,
    this.countLabel,
    this.averageOverride,
    this.chartHeight = 180,
  });

  final String title;
  final SessionAxis axis;

  /// Time → value, in order.
  final List<SessionSample> samples;
  final ChartRange range;
  final Color accentColor;
  final String Function(double value) valueFormatter;

  /// The sample count, formatted — the caller owns number formatting.
  final String countText;

  /// What [countText] counts. Defaults to "samples", which is what a recorded
  /// trace has; a trace stepped one value per split counts splits.
  final String? countLabel;

  /// The average to STATE, for a trace whose points are not evenly spaced in
  /// time and whose arithmetic mean would therefore misreport it.
  ///
  /// Five 1 km splits and a 200 m limp home are six equal terms in a mean — but
  /// average speed over equal DISTANCES is the harmonic mean of their speeds,
  /// not the arithmetic one, and over unequal ones it is neither. Only the
  /// caller holds the distances and the times, so only the caller can say. A
  /// recorded trace leaves this null and the mean of its samples is the truth.
  final double? averageOverride;

  final double chartHeight;

  /// Beyond this many samples the dots merge into a smear and only cost frames.
  static const int _maxVisiblePoints = 120;

  @override
  Widget build(BuildContext context) {
    if (samples.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();
    final timeFormat = DateFormat.jm(locale);

    final ordered = [...samples]..sort((a, b) => a.time.compareTo(b.time));
    final values = [for (final sample in ordered) sample.value];
    final min = values.reduce(math.min);
    final max = values.reduce(math.max);
    final average =
        averageOverride ?? values.reduce((a, b) => a + b) / values.length;
    final drawPoints = ordered.length <= _maxVisiblePoints;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            Text(
              title,
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            ChartStatRow(
              accentColor: accentColor,
              stats: [
                (label: l10n.summaryAverage, value: valueFormatter(average)),
                (
                  label: l10n.summaryRange,
                  value: '${valueFormatter(min)}-${valueFormatter(max)}',
                ),
                (label: countLabel ?? l10n.summarySamples, value: countText),
              ],
            ),
            MetricLinePlot(
              points: [
                for (final sample in ordered)
                  MetricLinePlotPoint(
                    xFraction: axis.fractionOf(sample.time),
                    value: sample.value,
                  ),
              ],
              minValue: range.min,
              maxValue: range.max,
              accentColor: accentColor,
              chartHeight: chartHeight,
              valueFormatter: valueFormatter,
              pointRadius: drawPoints ? 2 : 0,
              lineStrokeWidth: 2,
              drawPoints: drawPoints,
            ),
            const SizedBox(height: 4),
            SessionAxisLabels(axis: axis),
            Text(
              l10n.summaryRecorded(
                timeFormat.format(ordered.first.time.toLocal()),
                timeFormat.format(ordered.last.time.toLocal()),
              ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

/// A reading during a session, and when it was taken.
typedef SessionSample = ({DateTime time, double value});

/// One statistic under a chart's title.
typedef ChartStat = ({String label, String value});

/// The average / range / samples row a session chart wears above its trace.
class ChartStatRow extends StatelessWidget {
  const ChartStatRow({
    super.key,
    required this.stats,
    required this.accentColor,
  });

  final List<ChartStat> stats;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      spacing: 16,
      children: [
        for (final stat in stats)
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  stat.value,
                  style:
                      theme.textTheme.titleMedium?.copyWith(color: accentColor),
                ),
                Text(
                  stat.label,
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
