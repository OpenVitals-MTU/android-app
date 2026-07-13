@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/insights/activity_splits.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/activity/presentation/activity_splits_card.dart';

import '../../support/golden_harness.dart';

/// [ActivitySplitsCard] — the pace bars.
///
/// The bar is deliberately NOT zero-based, and that is the whole point of
/// photographing it: the interesting range of a run is the thirty seconds between
/// its fastest and its slowest kilometre, and a zero-based bar squashes that into
/// a row of identical stripes. The slowest split fills the track, the fastest sits
/// at the 25% floor so it still reads as a bar rather than as nothing. A "fix"
/// that re-based the scale at zero would leave every existing test green.
///
/// The estimated source has no bar at all, and that is not an oversight either:
/// every estimated split has the same pace by construction, so a bar chart of it
/// would be a flat line pretending to be a measurement.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  final start = DateTime(2026, 6, 22, 7, 30);

  /// Seconds per kilometre for each split of a real-feeling 5.42 km run: a
  /// steady opening, a slow fourth (the hill), and a fast finish.
  const paceSeconds = <double>[320, 312, 330, 345, 305];
  const partialPace = 315.0;
  const partialMeters = 420.0;

  // Folded by the view-model, not rescanned by the card — a ratio between splits,
  // so the unit cancels and metric seconds-per-km are the right scale even for a
  // user reading min/mi.
  const slowest = 345.0;
  const fastest = 305.0;

  // The activity's own average pace, which is what each split's delta is measured
  // against: total time over total distance, NOT the mean of the split paces.
  const averagePace = 321.8;

  ActivitySplit split(int index, double meters, double pace, DateTime from) =>
      ActivitySplit(
        index: index,
        distanceMeters: meters,
        elapsed: Duration(
          milliseconds: (pace * meters / 1000.0 * 1000).round(),
        ),
        startTime: from,
        endTime: from.add(
          Duration(milliseconds: (pace * meters / 1000.0 * 1000).round()),
        ),
        isPartial: meters < 1000,
        averageHeartRateBpm: 148 + index * 3,
        elevationGainMeters: 4.0 + index * 2,
        elevationLossMeters: 6.0 - index * 0.5,
        paceDeltaSecondsPerKilometer: pace - averagePace,
      );

  List<ActivitySplit> runSplits() {
    final splits = <ActivitySplit>[];
    var cursor = start;
    for (var i = 0; i < paceSeconds.length; i++) {
      final s = split(i + 1, 1000, paceSeconds[i], cursor);
      splits.add(s);
      cursor = s.endTime;
    }
    // The trailing remainder. It keeps its real (short) distance and says so —
    // a partial split is short ON PURPOSE, and unlabelled it reads as a bad fix.
    splits.add(split(paceSeconds.length + 1, partialMeters, partialPace, cursor));
    return splits;
  }

  testWidgets('splits cut from the route', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => ActivitySplitsCard(
        splits: ActivitySplits(
          source: SplitSource.route,
          splits: runSplits(),
        ),
        formatter: formatter,
        splitDistanceMeters: 1000,
        slowestPaceSeconds: slowest,
        fastestPaceSeconds: fastest,
      ),
      name: 'activity_splits_route',
    );
  });

  testWidgets('laps the device recorded itself', (tester) async {
    // A lap is whatever the watch called a lap — never re-cut, never marked
    // partial, so the distances are uneven and none of them is an apology.
    final laps = <ActivitySplit>[
      split(1, 400, 298, start),
      split(2, 400, 306, start.add(const Duration(seconds: 120))),
      split(3, 800, 335, start.add(const Duration(seconds: 245))),
      split(4, 400, 291, start.add(const Duration(seconds: 515))),
    ];
    await expectChartGoldenBothThemes(
      tester,
      () => ActivitySplitsCard(
        splits: ActivitySplits(source: SplitSource.deviceLaps, splits: laps),
        formatter: formatter,
        splitDistanceMeters: 1000,
        slowestPaceSeconds: 335,
        fastestPaceSeconds: 291,
      ),
      name: 'activity_splits_laps',
    );
  });

  testWidgets('estimated splits — the numbers, and no bar', (tester) async {
    // Distance and duration only: the pace is the activity average, evenly
    // divided. The card says so in words and withholds the bar.
    final estimated = <ActivitySplit>[
      for (var i = 0; i < 4; i++)
        split(i + 1, 1000, averagePace, start.add(Duration(seconds: (322 * i)))),
    ];
    await expectChartGoldenBothThemes(
      tester,
      () => ActivitySplitsCard(
        splits: ActivitySplits(
          source: SplitSource.estimated,
          splits: estimated,
        ),
        formatter: formatter,
        splitDistanceMeters: 1000,
        slowestPaceSeconds: averagePace,
        fastestPaceSeconds: averagePace,
      ),
      name: 'activity_splits_estimated',
    );
  });
}
