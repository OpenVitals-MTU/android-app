import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../application/activity_detail_display.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/insights/activity_splits.dart';
import '../../../domain/model/activity_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/metric_session_chart.dart';
import '../../../ui/charts/session_axis.dart';
import '../../../ui/theme/app_colors.dart';
import 'activity_split_distance_label.dart';

/// The speed and cadence traces recorded during a session, on the same axis as the
/// heart-rate card.
///
/// These were dropped in the Flutter port, which is why a cycling activity showed
/// no speed and no cadence even when a sensor had recorded both.

/// Speed over the session.
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

    return _sessionTrace(
      title: AppLocalizations.of(context).activityRecordingLiveSpeed,
      samples: [
        for (final sample in samples)
          (time: sample.time, value: sample.metersPerSecond),
      ],
      sessionStart: sessionStart,
      sessionEnd: sessionEnd,
      unitFormatter: unitFormatter,
      accentColor: AppColors.distance,
      valueFormatter: (value) => unitFormatter.speed(value).text,
    );
  }
}

/// Speed over the session for a device that recorded none — rebuilt from the
/// splits, which know how far each segment went and how long it took.
///
/// This is the watch that writes a route and a distance but no `SpeedRecord`,
/// which is most of them: the shape of the run is in the data, and until now
/// only the splits table showed it. The trace STEPS, one flat run per split,
/// because that is the resolution the numbers have — a split's speed is an
/// average over its window, not a reading at an instant.
///
/// The title says where it came from ("every 1 km", "per lap") and the stat row
/// counts SPLITS rather than samples, because there are no samples here. The
/// [ActivitySplitSpeedTrace] states its own average: the chart's own mean would
/// weigh a 200 m limp home equally with a 1 km split and quietly report a
/// faster session than happened.
class ActivitySplitSpeedChartCard extends StatelessWidget {
  const ActivitySplitSpeedChartCard({
    super.key,
    required this.trace,
    required this.source,
    required this.splitDistanceMeters,
    required this.sessionStart,
    required this.sessionEnd,
    required this.unitFormatter,
  });

  final ActivitySplitSpeedTrace trace;
  final SplitSource source;
  final double splitDistanceMeters;
  final DateTime sessionStart;
  final DateTime sessionEnd;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    if (trace.samples.isEmpty) return const SizedBox.shrink();

    final l10n = AppLocalizations.of(context);
    // `estimated` never reaches here (it is flat by construction, so the display
    // builds no trace for it), and neither does `speedSamples` (that source
    // exists only when speed WAS recorded, and the recorded card wins).
    final (title, countLabel) = switch (source) {
      SplitSource.deviceLaps => (
          l10n.activitySpeedPerLapTitle,
          l10n.activitySpeedLapsLabel,
        ),
      _ => (
          l10n.activitySpeedPerSplitTitle(
            splitDistanceLabel(l10n, unitFormatter, splitDistanceMeters),
          ),
          l10n.activitySpeedSplitsLabel,
        ),
    };

    return _sessionTrace(
      title: title,
      samples: [
        for (final sample in trace.samples)
          (time: sample.time, value: sample.metersPerSecond),
      ],
      sessionStart: sessionStart,
      sessionEnd: sessionEnd,
      unitFormatter: unitFormatter,
      accentColor: AppColors.distance,
      valueFormatter: (value) => unitFormatter.speed(value).text,
      countText: unitFormatter.count(trace.splitCount),
      countLabel: countLabel,
      averageOverride: trace.averageMetersPerSecond,
    );
  }
}

/// Cadence over the session.
///
/// Takes the whole sample list and filters by [kind] itself, exactly as Kotlin
/// does, so the screen can just render one card per kind. Which kind is right for
/// an activity is not decided by its exercise type anywhere -- Health Connect tags
/// each sample by the record it came from (`CyclingPedalingCadenceRecord` ->
/// cycling, `StepsCadenceRecord` -> steps), so a bike ride simply has no steps-kind
/// samples and the card for it never appears.
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
    final matching = [
      for (final sample in samples)
        if (sample.kind == kind) (time: sample.time, value: sample.rate),
    ];
    if (matching.isEmpty) return const SizedBox.shrink();

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

    return _sessionTrace(
      title: title,
      samples: matching,
      sessionStart: sessionStart,
      sessionEnd: sessionEnd,
      unitFormatter: unitFormatter,
      accentColor: accentColor,
      valueFormatter: (value) => format(value).text,
    );
  }
}


/// The height profile of the session.
///
/// Health Connect has no elevation series: `ElevationGainedRecord` is one total
/// for the whole session — it says you climbed 240 m, never where. So this is
/// drawn from the ROUTE's altitudes, which is the only thing in Health Connect
/// that knows the shape of a climb. An activity with no route, or a route
/// recorded without altitude, has no profile to show and no card.
class ActivityElevationChartCard extends StatelessWidget {
  const ActivityElevationChartCard({
    super.key,
    required this.samples,
    required this.sessionStart,
    required this.sessionEnd,
    required this.unitFormatter,
  });

  final List<ActivityElevationSample> samples;
  final DateTime sessionStart;
  final DateTime sessionEnd;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    if (samples.isEmpty) return const SizedBox.shrink();

    return _sessionTrace(
      title: AppLocalizations.of(context).metricElevation,
      samples: [
        for (final sample in samples) (time: sample.time, value: sample.meters),
      ],
      sessionStart: sessionStart,
      sessionEnd: sessionEnd,
      unitFormatter: unitFormatter,
      accentColor: AppColors.elevation,
      valueFormatter: (value) => unitFormatter.elevation(value).text,
      floorAtZero: false,
    );
  }
}

/// Speed and cadence share a y-axis rule: pad by a tenth of the spread, never dip
/// below zero. (Heart rate does not — it pads in absolute beats. That difference is
/// the only thing that ever separated these cards.)
MetricSessionChart _sessionTrace({
  required String title,
  required List<SessionSample> samples,
  required DateTime sessionStart,
  required DateTime sessionEnd,
  required UnitFormatter unitFormatter,
  required Color accentColor,
  required String Function(double value) valueFormatter,
  bool floorAtZero = true,
  String? countText,
  String? countLabel,
  double? averageOverride,
}) {
  final values = [for (final sample in samples) sample.value];
  final min = values.reduce(math.min);
  final max = values.reduce(math.max);
  // The 0.001 floor keeps a flat series -- every sample identical -- from
  // collapsing the range to nothing and dividing by zero in the painter.
  final spread = math.max(max - min, 0.001);

  return MetricSessionChart(
    title: title,
    axis: SessionAxis(start: sessionStart, end: sessionEnd),
    samples: samples,
    range: ChartRange(
      // Speed and cadence cannot be negative, and an axis that starts anywhere
      // else would be lying about how fast you were going. ELEVATION is the
      // opposite: a run between 300 m and 350 m has fifty metres of relief in
      // it, and pinning the axis to sea level would draw it as a flat line at
      // the top of the card. Height is read against the ground, not against
      // zero — and can be below it.
      floorAtZero ? math.max(min - spread * 0.1, 0.0) : min - spread * 0.1,
      max + spread * 0.1,
    ),
    accentColor: accentColor,
    valueFormatter: valueFormatter,
    // A recorded trace counts its samples; a trace stepped per split says so
    // itself, because "12 samples" for six splits would be counting the corners
    // of the steps.
    countText: countText ?? unitFormatter.count(samples.length),
    countLabel: countLabel,
    averageOverride: averageOverride,
  );
}
