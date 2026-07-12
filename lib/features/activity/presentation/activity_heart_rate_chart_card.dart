import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/heart_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/metric_session_chart.dart';
import '../../../ui/charts/session_axis.dart';
import '../../../ui/theme/app_colors.dart';

/// The heart-rate trace recorded during a session.
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

  @override
  Widget build(BuildContext context) {
    if (samples.isEmpty) return const SizedBox.shrink();

    final bpms = [for (final sample in samples) sample.beatsPerMinute];
    final minBpm = bpms.reduce(math.min);
    final maxBpm = bpms.reduce(math.max);

    return MetricSessionChart(
      title: AppLocalizations.of(context).activityRecordingLiveHeartRate,
      axis: SessionAxis(start: sessionStart, end: sessionEnd),
      samples: [
        for (final sample in samples)
          (time: sample.time, value: sample.beatsPerMinute.toDouble()),
      ],
      // Heart rate pads in absolute beats, not by a fraction of the span: a steady
      // effort has a narrow spread, and 10% of nothing is nothing. Floored at 30,
      // because an axis that starts below a plausible resting rate is wasted paper.
      range: ChartRange(
        math.max(minBpm - 5, 30).toDouble(),
        (maxBpm + 5).toDouble(),
      ),
      accentColor: AppColors.heart,
      valueFormatter: (value) => unitFormatter.heartRate(value.round()).text,
      countText: unitFormatter.count(samples.length),
    );
  }
}
