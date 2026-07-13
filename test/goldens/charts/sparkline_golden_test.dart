@Tags(['golden'])
library;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/ui/charts/sparkline_chart.dart';
import 'package:openvitals/ui/theme/app_colors.dart';

import '../../support/golden_harness.dart';

/// [SparklineChart] — the mini trend line on the activity summary rows.
///
/// It has no axis, no labels and no card: everything it says, it says with the
/// line's shape against its baseline. Which is precisely why it needs a picture —
/// there is nothing else in it to assert on.
///
/// The one-point case is a real state, not a degenerate one: a person with a
/// single workout in the window still gets a row, and `singlePointLine` is what
/// keeps that row from rendering as one lonely dot in the middle of nowhere.
void main() {
  // The height the activity rows give it. A sparkline in an unbounded box paints
  // nothing, so the box is part of the fixture.
  Widget sized(Widget child) => SizedBox(height: 58, child: child);

  testWidgets('a week of buckets', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => sized(
        const SparklineChart(
          // Minutes of activity per day. The zero is deliberate: a rest day has
          // to reach the floor of the chart, not merely dip.
          values: [42, 0, 65, 30, 0, 88, 55],
          accentColor: AppColors.workout,
          singlePointLine: true,
        ),
      ),
      name: 'sparkline_week',
    );
  });

  testWidgets('one point — a flat run across the whole width', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => sized(
        const SparklineChart(
          values: [42],
          accentColor: AppColors.workout,
          singlePointLine: true,
        ),
      ),
      name: 'sparkline_single',
    );
  });

  testWidgets('one point with singlePointLine off — the dot on its own',
      (tester) async {
    // The default. Kept as a golden because the difference between this and the
    // one above is the entire reason the flag exists, and a refactor that flips
    // the default would otherwise pass silently.
    await expectChartGoldenBothThemes(
      tester,
      () => sized(
        const SparklineChart(
          values: [42],
          accentColor: AppColors.workout,
        ),
      ),
      name: 'sparkline_single_dot',
    );
  });
}
