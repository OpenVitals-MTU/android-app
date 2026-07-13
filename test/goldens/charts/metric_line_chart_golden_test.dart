@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/ui/charts/line_chart.dart';
import 'package:openvitals/ui/theme/app_colors.dart';

import '../../support/golden_harness.dart';

/// [MetricLineChart] — the heart screen's chart, and the only one with a legend.
///
/// It pads its own y axis (8% of the span, or ±1 for a flat series), so unlike
/// the bar charts the caller hands it no range at all — which means the padding
/// rule is only ever visible in a picture. Two series is not a cosmetic variant
/// either: the legend row exists only when more than one series survives the
/// period filter, so a one-series chart and a two-series chart are different
/// layouts, not the same one with an extra line.
void main() {
  const week = DatePeriod(LocalDate(2026, 6, 16), LocalDate(2026, 6, 22));

  // Resting heart rate over the week, one reading a night. Real RHR moves in a
  // band of a few beats, which is exactly the case the 8% padding exists for: a
  // zero-based axis would draw this as a flat line along the top of the card.
  const restingBpm = <int>[54, 55, 53, 57, 58, 55, 52];
  final restingPoints = <MetricLinePoint>[
    for (var i = 0; i < restingBpm.length; i++)
      MetricLinePoint(
        date: week.start.plusDays(i),
        value: restingBpm[i].toDouble(),
      ),
  ];

  testWidgets('one series, no legend', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => MetricLineChart(
        title: 'Resting heart rate',
        series: [
          MetricLineSeries(points: restingPoints, color: AppColors.heart),
        ],
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.heart,
        summaryText: 'This week · 55 bpm avg (52-58 bpm)',
        valueFormatter: (value) => '${value.round()} bpm',
      ),
      name: 'metric_line_chart_single',
    );
  });

  testWidgets('two series and the legend that comes with them', (tester) async {
    // The heart screen's day-average line against its daily low: the shape
    // `heartRateSeries` builds, minus the third (highest) line, so the legend
    // stays two columns wide and the y padding has a real spread to work on.
    const averageBpm = <int>[72, 74, 71, 78, 80, 73, 69];
    await expectChartGoldenBothThemes(
      tester,
      () => MetricLineChart(
        title: 'Average heart rate',
        series: [
          MetricLineSeries(
            points: [
              for (var i = 0; i < averageBpm.length; i++)
                MetricLinePoint(
                  date: week.start.plusDays(i),
                  value: averageBpm[i].toDouble(),
                ),
            ],
            color: AppColors.heart,
            label: 'Average',
          ),
          MetricLineSeries(
            points: restingPoints,
            // The dimmer sibling: same hue, less weight, so the eye reads the
            // average as the subject and the low as its floor.
            color: AppColors.heart.withValues(alpha: 0.55),
            label: 'Lowest',
          ),
        ],
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.heart,
        summaryText: 'This week · 74 bpm avg (52-80 bpm)',
        valueFormatter: (value) => '${value.round()} bpm',
      ),
      name: 'metric_line_chart_two_series',
    );
  });

  testWidgets('two series with a day selected', (tester) async {
    // Selection is a full-height column wash behind the lines, not a bar
    // highlight — the only chart that draws it this way.
    await expectChartGoldenBothThemes(
      tester,
      () => MetricLineChart(
        title: 'Resting heart rate',
        series: [
          MetricLineSeries(
            points: restingPoints,
            color: AppColors.heart,
            label: 'Resting',
          ),
          MetricLineSeries(
            points: [
              for (var i = 0; i < restingBpm.length; i++)
                MetricLinePoint(
                  date: week.start.plusDays(i),
                  value: restingBpm[i] + 12.0,
                ),
            ],
            color: AppColors.heart.withValues(alpha: 0.9),
            label: 'Sleeping',
          ),
        ],
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.heart,
        summaryText: 'This week · 55 bpm avg (52-58 bpm)',
        selectedDate: const LocalDate(2026, 6, 19),
        onDateSelected: (_) {},
        valueFormatter: (value) => '${value.round()} bpm',
      ),
      name: 'metric_line_chart_selected',
    );
  });
}
