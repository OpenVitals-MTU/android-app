@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/day_axis.dart';
import 'package:openvitals/ui/charts/metric_day_chart.dart';
import 'package:openvitals/ui/theme/app_colors.dart';

import '../../support/golden_harness.dart';

/// [MetricDayChart] — the most-used card in the app (six features).
///
/// The three shapes it can draw are already unit-tested as pure maths. What was
/// never verified is that any of it reaches the screen: these are the pictures.
void main() {
  const day = LocalDate(2026, 6, 22);
  DateTime at(int hour, [int minute = 0]) =>
      DateTime(2026, 6, 22, hour, minute);
  DaySample sample(int hour, double value) => (time: at(hour), value: value);

  // Today, half elapsed — so the axis greys out the hours that have not happened.
  final today = DayAxis(day, now: at(14, 30));
  final pastDay = DayAxis(day, now: DateTime(2026, 6, 30));

  final steps = <DaySample>[
    sample(7, 800),
    sample(9, 2400),
    sample(12, 5100),
    sample(14, 7300),
    sample(18, 9600),
    sample(21, 11200),
  ];

  testWidgets('cumulative, a day that is over', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => MetricDayChart(
        axis: pastDay,
        samples: steps,
        shape: DaySeriesShape.cumulative,
        // Floored at zero, exactly as every cumulative caller does it — a running
        // total cannot go below nothing, and `ChartRange.padded` would print a
        // "-32" tick under a step count. (`.padded` is for RAW readings, where a
        // weight of 74 kg should not be measured against sea level.)
        range: const ChartRange(0, 11200),
        accentColor: AppColors.steps,
        metricName: 'Steps',
        emptyLabel: 'steps',
      ),
      name: 'metric_day_chart_cumulative',
    );
  });

  testWidgets('cumulative, today — the rest of the day has not happened',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => MetricDayChart(
        axis: today,
        samples: steps.sublist(0, 4),
        shape: DaySeriesShape.cumulative,
        range: const ChartRange(0, 11200),
        accentColor: AppColors.steps,
        metricName: 'Steps',
        emptyLabel: 'steps',
      ),
      name: 'metric_day_chart_cumulative_today',
    );
  });

  testWidgets('raw readings, plotted where they were taken', (tester) async {
    final weights = <DaySample>[
      sample(7, 74.2),
      sample(13, 74.6),
      sample(21, 74.1),
    ];
    await expectChartGoldenBothThemes(
      tester,
      () => MetricDayChart(
        axis: pastDay,
        samples: weights,
        shape: DaySeriesShape.raw,
        range: ChartRange.padded([for (final s in weights) s.value]),
        accentColor: AppColors.weight,
        metricName: 'Weight',
        emptyLabel: 'readings',
        drawPoints: true,
      ),
      name: 'metric_day_chart_raw',
    );
  });

  testWidgets('a day with nothing in it', (tester) async {
    // The one chart in the library with a real empty state. The other three
    // vanish instead, which is what Phase B fixes.
    await expectChartGoldenBothThemes(
      tester,
      () => MetricDayChart(
        axis: pastDay,
        samples: const <DaySample>[],
        shape: DaySeriesShape.cumulative,
        range: const ChartRange(0, 100),
        accentColor: AppColors.hydration,
        metricName: 'Water',
        emptyLabel: 'water',
      ),
      name: 'metric_day_chart_empty',
    );
  });
}
