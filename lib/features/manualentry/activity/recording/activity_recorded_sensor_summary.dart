import 'package:flutter/material.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/model/ble_sensor_models.dart';
import '../../../../domain/model/heart_models.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_surface.dart';
import '../../../activity/presentation/activity_heart_rate_chart_card.dart';
import '../../../../core/stats/stats.dart';

/// Port of the Kotlin `ActivityRecordedSensorSummary` (in
/// `recording/ActivityRecordingSensorUi.kt`): what a finished recording — or an
/// activity being edited — captured from connected BLE sensors.
///
/// Renders nothing when no sensor produced samples, which is the common case
/// for a hand-typed entry.
class ActivityRecordedSensorSummary extends StatelessWidget {
  const ActivityRecordedSensorSummary({
    super.key,
    required this.samples,
    required this.unitFormatter,
    this.sessionStart,
    this.sessionEnd,
    this.savedHeartRateSamples = const <HeartRateSample>[],
  });

  final BleRecordingSampleBuffer samples;
  final UnitFormatter unitFormatter;

  /// The session range the heart-rate chart's time axis spans. When null (the
  /// entry's date / time / duration fields do not parse yet) the axis falls
  /// back to the first and last sample times, as in Kotlin.
  final DateTime? sessionStart;
  final DateTime? sessionEnd;

  /// Heart-rate samples read back from a saved session, used when this entry is
  /// being edited rather than freshly recorded.
  final List<HeartRateSample> savedHeartRateSamples;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final heartRateSamples = samples.heartRateSamples.isNotEmpty
        ? [
            for (final sample in samples.heartRateSamples)
              HeartRateSample(
                time: sample.time,
                beatsPerMinute: sample.beatsPerMinute,
                source: 'sensor',
              ),
          ]
        : savedHeartRateSamples;

    final hasOtherSamples = samples.powerSamples.isNotEmpty ||
        samples.cyclingCadenceSamples.isNotEmpty ||
        samples.speedSamples.isNotEmpty ||
        samples.stepsCadenceSamples.isNotEmpty;

    if (heartRateSamples.isEmpty && !hasOtherSamples) {
      return const SizedBox.shrink();
    }

    // Kotlin falls back to the sample range when the caller has no session
    // range to offer.
    DateTime? chartStart = sessionStart;
    DateTime? chartEnd = sessionEnd;
    if (heartRateSamples.isNotEmpty) {
      chartStart ??= heartRateSamples
          .map((sample) => sample.time)
          .reduce((a, b) => a.isBefore(b) ? a : b);
      chartEnd ??= heartRateSamples
          .map((sample) => sample.time)
          .reduce((a, b) => a.isAfter(b) ? a : b);
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        if (heartRateSamples.isNotEmpty && chartStart != null && chartEnd != null)
          ActivityHeartRateChartCard(
            samples: heartRateSamples,
            sessionStart: chartStart,
            sessionEnd: chartEnd,
            unitFormatter: unitFormatter,
          ),
        if (hasOtherSamples)
          OpenVitalsSurface(
            style: OpenVitalsSurfaceStyle.metric,
            contentPadding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              spacing: 8,
              children: [
                Text(l10n.activityRecordingSensorsRecordedTitle,
                    style: theme.textTheme.titleSmall),
                for (final row in _metricRows(l10n))
                  Text(row, style: theme.textTheme.bodyMedium),
              ],
            ),
          ),
      ],
    );
  }

  /// The average power / cadence / speed lines, in the Kotlin order. Each is
  /// omitted when its sensor produced nothing.
  List<String> _metricRows(AppLocalizations l10n) {
    final rows = <String>[];

    final averagePower = samples.averagePowerWatts();
    if (averagePower != null) {
      rows.add('${l10n.metricAveragePower}: '
          '${unitFormatter.power(averagePower).text}');
    }

    if (samples.cyclingCadenceSamples.isNotEmpty) {
      final average = _average([
        for (final sample in samples.cyclingCadenceSamples) sample.rpm.toDouble(),
      ]);
      // Kotlin truncates the average to a whole rpm before formatting.
      rows.add('${l10n.metricCyclingCadence}: '
          '${unitFormatter.cadence(average.truncateToDouble()).text}');
    }

    if (samples.speedSamples.isNotEmpty) {
      final average = _average([
        for (final sample in samples.speedSamples) sample.metersPerSecond,
      ]);
      rows.add('${l10n.metricAverageSpeed}: '
          '${unitFormatter.speed(average).text}');
    }

    return rows;
  }

  /// Non-null at both call sites, which are guarded on `isNotEmpty`.
  static double _average(List<double> values) => average(values)!;
}
