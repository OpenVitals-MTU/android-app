@Tags(['golden'])
library;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/day_axis.dart';
import 'package:openvitals/ui/charts/session_axis.dart';

import '../../support/golden_harness.dart';

/// The axis rows, photographed WITHOUT a chart on top of them.
///
/// Every one of these is a strip of text that only means something in relation to
/// the plot above it, which is why they are worth shooting alone: the bug they
/// exist to prevent is a row that is internally perfect and lines up with
/// nothing. Four of the five intraday cards drew a `00:00 … 24:00` row that was
/// out of step with their own plot, and it took a year for anyone to notice,
/// because each row was fine on its own terms.
///
/// So each row that carries an inset is shot at BOTH — the 64px inset that meets
/// a plot with a y-axis column, and the 0 that meets a plot without one. Two
/// legitimate conventions; the failure mode is a row that matches neither.
void main() {
  // A plot that draws only its guide lines — the y-axis column has to be
  // photographed against something, and the real painters are all private.
  Widget guidePlot() => const _GuidePlot();

  testWidgets('the y-axis label column, beside its plot', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => YAxisChart(
        // Three labels, top-to-bottom: max, mid, min. `chartYAxisLabels` steps up
        // to a finer format when the compact one would print the same string
        // twice — a real risk on a narrow range like a body weight.
        labels: chartYAxisLabels(0, 11200),
        chartHeight: 150,
        chart: guidePlot(),
      ),
      name: 'axis_y_column',
    );
  });

  testWidgets('the hour row under a plot that HAS a y axis', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          YAxisChart(
            labels: chartYAxisLabels(0, 11200),
            chartHeight: 150,
            chart: guidePlot(),
          ),
          const SizedBox(height: 8),
          // The default. 12:00 must land halfway across the PLOT, not halfway
          // across the card.
          const DayAxisLabels(),
        ],
      ),
      name: 'axis_day_labels_inset',
    );
  });

  testWidgets('the hour row under a plot that has NO y axis', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(height: 150, child: guidePlot()),
          const SizedBox(height: 8),
          // The body-energy strip's case: the plot starts at the card's edge, so
          // the row does too.
          const DayAxisLabels(inset: 0),
        ],
      ),
      name: 'axis_day_labels_inset0',
    );
  });

  testWidgets('the elapsed row, inset and not', (tester) async {
    // A 1h 12m session, so the quarter labels are 0:00 / 18:00 / 36:00 / 54:00 /
    // 1:12:00 — the point at which `formatRecordingElapsed` starts printing an
    // hour field and the row's five labels stop being the same width.
    final axis = SessionAxis(
      start: DateTime(2026, 6, 22, 9, 0),
      end: DateTime(2026, 6, 22, 10, 12),
    );
    await expectChartGoldenBothThemes(
      tester,
      () => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SessionAxisLabels(axis: axis),
          const SizedBox(height: 16),
          SessionAxisLabels(axis: axis, inset: 0),
        ],
      ),
      name: 'axis_session_labels',
    );
  });

  testWidgets('the date strip — a week keeps every label', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => PeriodChartXAxis(
        dates: datesInPeriod(
          const LocalDate(2026, 6, 16),
          const LocalDate(2026, 6, 22),
        ),
        selectedRange: TimeRange.week,
      ),
      name: 'axis_period_week',
    );
  });

  testWidgets('the date strip — a month drops all but every fifth',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => PeriodChartXAxis(
        dates: datesInPeriod(
          const LocalDate(2026, 6, 1),
          const LocalDate(2026, 6, 30),
        ),
        selectedRange: TimeRange.month,
      ),
      name: 'axis_period_month',
    );
  });

  testWidgets('the date strip — a year keeps the twelve month names',
      (tester) async {
    // Twelve buckets, so `isPeriodChartLabelVisible` shows all of them; a year
    // fed raw DAYS instead would thin to every thirtieth.
    await expectChartGoldenBothThemes(
      tester,
      () => PeriodChartXAxis(
        dates: [
          for (var month = 7; month <= 12; month++) LocalDate(2025, month, 1),
          for (var month = 1; month <= 6; month++) LocalDate(2026, month, 1),
        ],
        selectedRange: TimeRange.year,
      ),
      name: 'axis_period_year',
    );
  });
}

/// A plot that draws nothing but [drawYAxisGuides] — the shared primitive every
/// real painter opens with. The painters themselves are private to their charts,
/// and the axis rows need SOMETHING above them to be aligned against.
class _GuidePlot extends StatelessWidget {
  const _GuidePlot();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return CustomPaint(
      size: Size.infinite,
      painter: _GuidePainter(
        gridColor: scheme.primary.withValues(alpha: 0.12),
        axisColor: scheme.outlineVariant.withValues(alpha: 0.8),
      ),
    );
  }
}

class _GuidePainter extends CustomPainter {
  const _GuidePainter({required this.gridColor, required this.axisColor});

  final Color gridColor;
  final Color axisColor;

  @override
  void paint(Canvas canvas, Size size) =>
      drawYAxisGuides(canvas, size, gridColor: gridColor, axisColor: axisColor);

  @override
  bool shouldRepaint(_GuidePainter oldDelegate) =>
      oldDelegate.gridColor != gridColor || oldDelegate.axisColor != axisColor;
}
