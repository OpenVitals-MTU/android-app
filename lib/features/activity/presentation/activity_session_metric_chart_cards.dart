import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/activity_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/metric_line_plot.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import 'activity_heart_rate_chart_card.dart' show sessionElapsedLabels;

/// Port of the Kotlin `ActivitySessionMetricChartCards.kt`: the speed and cadence
/// traces recorded during a session, on the same time axis as the heart-rate card.
///
/// These were dropped in the Flutter port, which is why a cycling activity showed
/// no speed and no cadence even when a sensor had recorded both.

/// Port of the Kotlin `ActivitySpeedChartCard`.
class ActivitySpeedChartCard extends StatelessWidget {
  const ActivitySpeedChartCard({
    super.key,
    required this.samples,
    required this.sessionStart,
    required this.sessionEnd,
    required this.unitFormatter,
  });

  final List<SpeedSample> samples;
  final DateTime sessionStart;
  final DateTime sessionEnd;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    if (samples.isEmpty) return const SizedBox.shrink();
    final sorted = [...samples]..sort((a, b) => a.time.compareTo(b.time));

    return _ActivitySessionMetricChartCard(
      title: AppLocalizations.of(context).activityRecordingLiveSpeed,
      times: [for (final sample in sorted) sample.time],
      values: [for (final sample in sorted) sample.metersPerSecond],
      sessionStart: sessionStart,
      sessionEnd: sessionEnd,
      unitFormatter: unitFormatter,
      accentColor: AppColors.distance,
      valueFormatter: (value) => unitFormatter.speed(value).text,
    );
  }
}

/// Port of the Kotlin `ActivityCadenceChartCard`.
///
/// Takes the whole sample list and filters by [kind] itself, exactly as Kotlin
/// does, so the screen can just render one card per kind. Which kind is right for
/// an activity is not decided by its exercise type anywhere -- Health Connect
/// tags each sample by the record it came from (`CyclingPedalingCadenceRecord` ->
/// cycling, `StepsCadenceRecord` -> steps), so a bike ride simply has no
/// steps-kind samples and the card for it never appears.
class ActivityCadenceChartCard extends StatelessWidget {
  const ActivityCadenceChartCard({
    super.key,
    required this.samples,
    required this.kind,
    required this.sessionStart,
    required this.sessionEnd,
    required this.unitFormatter,
  });

  final List<ActivityCadenceSample> samples;
  final ActivityCadenceKind kind;
  final DateTime sessionStart;
  final DateTime sessionEnd;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    final filtered = [
      for (final sample in samples)
        if (sample.kind == kind) sample,
    ]..sort((a, b) => a.time.compareTo(b.time));
    if (filtered.isEmpty) return const SizedBox.shrink();

    final l10n = AppLocalizations.of(context);
    final (title, accentColor, format) = switch (kind) {
      ActivityCadenceKind.cycling => (
          l10n.metricCyclingCadence,
          AppColors.cycle,
          unitFormatter.cadence,
        ),
      ActivityCadenceKind.steps => (
          l10n.metricStepsCadence,
          AppColors.steps,
          unitFormatter.stepsCadence,
        ),
    };

    return _ActivitySessionMetricChartCard(
      title: title,
      times: [for (final sample in filtered) sample.time],
      values: [for (final sample in filtered) sample.rate],
      sessionStart: sessionStart,
      sessionEnd: sessionEnd,
      unitFormatter: unitFormatter,
      accentColor: accentColor,
      valueFormatter: (value) => format(value).text,
    );
  }
}

/// Port of the Kotlin private `ActivitySessionMetricChartCard`: the renderer both
/// cards share. [times] and [values] are parallel and already sorted by time.
class _ActivitySessionMetricChartCard extends StatelessWidget {
  const _ActivitySessionMetricChartCard({
    required this.title,
    required this.times,
    required this.values,
    required this.sessionStart,
    required this.sessionEnd,
    required this.unitFormatter,
    required this.accentColor,
    required this.valueFormatter,
  });

  final String title;
  final List<DateTime> times;
  final List<double> values;
  final DateTime sessionStart;
  final DateTime sessionEnd;
  final UnitFormatter unitFormatter;
  final Color accentColor;
  final String Function(double value) valueFormatter;

  static const double _chartHeight = 180;

  @override
  Widget build(BuildContext context) {
    if (values.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();

    final minValue = values.reduce(math.min);
    final maxValue = values.reduce(math.max);
    final avgValue = values.reduce((a, b) => a + b) / values.length;
    // Kotlin pads the Y range by 10% of its span, flooring the minimum at zero.
    // The 0.001 floor keeps a flat series (every sample identical) from
    // collapsing the range to nothing and dividing by zero in the painter.
    final valueRange = math.max(maxValue - minValue, 0.001);
    final paddedMin = math.max(minValue - valueRange * 0.1, 0.0);
    final paddedMax = maxValue + valueRange * 0.1;
    final sessionDurationMillis =
        math.max(sessionEnd.difference(sessionStart).inMilliseconds, 1);
    final drawPoints = values.length <= 120;
    final timeFormatter = DateFormat.jm(locale);

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
            Row(
              spacing: 16,
              children: [
                Expanded(
                  child: _ActivitySessionMetricStat(
                    label: l10n.summaryAverage,
                    value: valueFormatter(avgValue),
                    accentColor: accentColor,
                  ),
                ),
                Expanded(
                  child: _ActivitySessionMetricStat(
                    label: l10n.summaryRange,
                    value: '${valueFormatter(minValue)}-'
                        '${valueFormatter(maxValue)}',
                    accentColor: accentColor,
                  ),
                ),
                Expanded(
                  child: _ActivitySessionMetricStat(
                    label: l10n.summarySamples,
                    value: unitFormatter.count(values.length),
                    accentColor: accentColor,
                  ),
                ),
              ],
            ),
            MetricLinePlot(
              points: [
                for (var i = 0; i < values.length; i++)
                  MetricLinePlotPoint(
                    xFraction: times[i]
                            .difference(sessionStart)
                            .inMilliseconds
                            .clamp(0, sessionDurationMillis) /
                        sessionDurationMillis,
                    value: values[i],
                  ),
              ],
              minValue: paddedMin,
              maxValue: paddedMax,
              accentColor: accentColor,
              chartHeight: _chartHeight,
              valueFormatter: valueFormatter,
              pointRadius: drawPoints ? 2 : 0,
              lineStrokeWidth: 2,
              drawPoints: drawPoints,
            ),
            const SizedBox(height: 4),
            ChartXAxisWithYAxis(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  for (final label in sessionElapsedLabels(
                      Duration(milliseconds: sessionDurationMillis)))
                    Text(
                      label,
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                ],
              ),
            ),
            Text(
              l10n.summaryRecorded(
                timeFormatter.format(times.first.toLocal()),
                timeFormatter.format(times.last.toLocal()),
              ),
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `ActivitySessionMetricStat`.
class _ActivitySessionMetricStat extends StatelessWidget {
  const _ActivitySessionMetricStat({
    required this.label,
    required this.value,
    required this.accentColor,
  });

  final String label;
  final String value;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: theme.textTheme.titleMedium?.copyWith(color: accentColor),
        ),
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}
