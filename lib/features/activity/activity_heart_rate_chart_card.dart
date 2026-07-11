import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/presentation/unit_formatter.dart';
import '../../domain/model/heart_models.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/charts/chart_axis.dart';
import '../../ui/charts/metric_line_plot.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';
import '../manualentry/activity/recording/activity_recording_dashboard.dart'
    show formatRecordingElapsed;

/// Port of the Kotlin `ActivityHeartRateChartCard`: the recorded heart-rate
/// trace on a real time axis keyed to the session range, with the avg / range /
/// sample-count stat row above it and elapsed-time labels below.
class ActivityHeartRateChartCard extends StatelessWidget {
  const ActivityHeartRateChartCard({
    super.key,
    required this.samples,
    required this.sessionStart,
    required this.sessionEnd,
    required this.unitFormatter,
  });

  final List<HeartRateSample> samples;
  final DateTime sessionStart;
  final DateTime sessionEnd;
  final UnitFormatter unitFormatter;

  /// Kotlin `chartHeight`.
  static const double _chartHeight = 180;

  @override
  Widget build(BuildContext context) {
    if (samples.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();

    final sorted = [...samples]..sort((a, b) => a.time.compareTo(b.time));
    final bpms = [for (final sample in sorted) sample.beatsPerMinute];
    final minBpm = bpms.reduce(math.min);
    final maxBpm = bpms.reduce(math.max);
    final avgBpm = (bpms.reduce((a, b) => a + b) / bpms.length).round();
    // Kotlin pads the Y range by 5 bpm, flooring the minimum at 30.
    final paddedMin = math.max(minBpm - 5, 30);
    final paddedMax = maxBpm + 5;
    final sessionDurationMillis =
        math.max(sessionEnd.difference(sessionStart).inMilliseconds, 1);
    final drawPoints = sorted.length <= 120;
    final timeFormatter = DateFormat.jm(locale);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            Text(
              l10n.activityRecordingLiveHeartRate,
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            Row(
              spacing: 16,
              children: [
                Expanded(
                  child: _ActivityHeartRateStat(
                    label: l10n.summaryAverage,
                    value: unitFormatter.heartRate(avgBpm).text,
                  ),
                ),
                Expanded(
                  child: _ActivityHeartRateStat(
                    label: l10n.summaryRange,
                    value: '${unitFormatter.heartRate(minBpm).text}-'
                        '${unitFormatter.heartRate(maxBpm).text}',
                  ),
                ),
                Expanded(
                  child: _ActivityHeartRateStat(
                    label: l10n.summarySamples,
                    value: unitFormatter.count(sorted.length),
                  ),
                ),
              ],
            ),
            MetricLinePlot(
              points: [
                for (final sample in sorted)
                  MetricLinePlotPoint(
                    xFraction: sample.time
                            .difference(sessionStart)
                            .inMilliseconds
                            .clamp(0, sessionDurationMillis) /
                        sessionDurationMillis,
                    value: sample.beatsPerMinute.toDouble(),
                  ),
              ],
              minValue: paddedMin.toDouble(),
              maxValue: paddedMax.toDouble(),
              accentColor: AppColors.heart,
              chartHeight: _chartHeight,
              valueFormatter: (value) =>
                  unitFormatter.heartRate(value.round()).text,
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
                timeFormatter.format(sorted.first.time.toLocal()),
                timeFormatter.format(sorted.last.time.toLocal()),
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

/// Kotlin `ActivityHeartRateStat`.
class _ActivityHeartRateStat extends StatelessWidget {
  const _ActivityHeartRateStat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: theme.textTheme.titleMedium?.copyWith(color: AppColors.heart),
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

/// Kotlin `sessionElapsedLabels`: X-axis labels at 0, ¼, ½, ¾ and the full
/// session duration.
List<String> sessionElapsedLabels(Duration duration) => [
      formatRecordingElapsed(Duration.zero),
      formatRecordingElapsed(duration ~/ 4),
      formatRecordingElapsed(duration ~/ 2),
      formatRecordingElapsed(duration * 3 ~/ 4),
      formatRecordingElapsed(duration),
    ];
