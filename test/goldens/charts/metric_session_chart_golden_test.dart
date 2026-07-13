@Tags(['golden'])
library;

import 'dart:math' as math;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/metric_session_chart.dart';
import 'package:openvitals/ui/charts/session_axis.dart';
import 'package:openvitals/ui/theme/app_colors.dart';

import '../../support/golden_harness.dart';

/// [MetricSessionChart] — the trace card the recorded session screens wear.
///
/// The one thing it decides for itself is whether to draw the DOTS: past 120
/// samples they merge into a smear and are dropped. Both sides of that threshold
/// are photographed here, because a sparse trace losing its points and a dense
/// trace growing them are both regressions no existing test would notice.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  // A 45-minute ride, on the golden clock. Sessions are instants, not days: the
  // axis spans the RECORDING, so a trace that stops early stops early.
  final start = DateTime(2026, 6, 22, 9, 0);
  final end = start.add(const Duration(minutes: 45));
  final axis = SessionAxis(start: start, end: end);

  /// The y bounds `activity_session_metric_chart_cards.dart` gives every speed
  /// and cadence card: a tenth of the spread of headroom, floored at zero.
  /// Speed cannot be negative, and an axis that dipped below zero would put the
  /// trace in the top half of the card and flatter the ride.
  ChartRange sessionRange(List<double> values) {
    final min = values.reduce(math.min);
    final max = values.reduce(math.max);
    final spread = math.max(max - min, 0.001);
    return ChartRange(
      math.max(min - spread * 0.1, 0.0),
      max + spread * 0.1,
    );
  }

  testWidgets('a recorded trace, dense enough that the dots come off',
      (tester) async {
    // One sample every 15 seconds for 45 minutes: 180 of them, which is what a
    // sensor actually writes and comfortably past the 120 the card will dot.
    final samples = <SessionSample>[
      for (var i = 0; i < 180; i++)
        (
          time: start.add(Duration(seconds: i * 15)),
          // Rolling terrain: a slow climb, a fast descent, and no noise, so the
          // picture changes only when the PAINTER changes.
          value: 7.2 + 2.4 * math.sin(i / 14.0) + 0.8 * math.sin(i / 3.5),
        ),
    ];
    await expectChartGoldenBothThemes(
      tester,
      () => MetricSessionChart(
        title: 'Speed',
        axis: axis,
        samples: samples,
        range: sessionRange([for (final s in samples) s.value]),
        accentColor: AppColors.distance,
        valueFormatter: (value) => formatter.speed(value).text,
        countText: formatter.count(samples.length),
      ),
      name: 'metric_session_chart_dense',
    );
  });

  testWidgets('a trace stepped per split, sparse enough to show its points',
      (tester) async {
    // The watch that writes a distance and no speed: the shape of the run is
    // rebuilt from the splits, so the trace STEPS — two samples per split, at
    // its ends — and the card counts splits rather than samples.
    final splitSpeeds = <double>[3.05, 3.18, 3.11, 2.86, 3.22, 3.30];
    final samples = <SessionSample>[
      for (var i = 0; i < splitSpeeds.length; i++) ...[
        (
          time: start.add(Duration(minutes: i * 7)),
          value: splitSpeeds[i],
        ),
        (
          time: start.add(Duration(minutes: (i + 1) * 7)),
          value: splitSpeeds[i],
        ),
      ],
    ];
    await expectChartGoldenBothThemes(
      tester,
      () => MetricSessionChart(
        title: 'Speed per 1 km',
        axis: axis,
        samples: samples,
        range: sessionRange(splitSpeeds),
        accentColor: AppColors.distance,
        valueFormatter: (value) => formatter.speed(value).text,
        countText: formatter.count(splitSpeeds.length),
        countLabel: 'Splits',
        // The card's own mean would weigh the corners of the steps equally with
        // the splits; only the caller knows the distances, so only the caller
        // can state the average.
        averageOverride: 3.12,
      ),
      name: 'metric_session_chart_splits',
    );
  });

  testWidgets('cadence, the other card that shares this scaffold',
      (tester) async {
    // Cycling cadence over the same ride, once a minute — the second caller, and
    // the one whose only difference from speed used to be a whole duplicate file.
    final samples = <SessionSample>[
      for (var i = 0; i <= 45; i++)
        (
          time: start.add(Duration(minutes: i)),
          value: 84.0 + 8.0 * math.sin(i / 6.0),
        ),
    ];
    await expectChartGoldenBothThemes(
      tester,
      () => MetricSessionChart(
        title: 'Cycling cadence',
        axis: axis,
        samples: samples,
        range: sessionRange([for (final s in samples) s.value]),
        accentColor: AppColors.cycle,
        valueFormatter: (value) => formatter.cadence(value).text,
        countText: formatter.count(samples.length),
      ),
      name: 'metric_session_chart_cadence',
    );
  });
}
