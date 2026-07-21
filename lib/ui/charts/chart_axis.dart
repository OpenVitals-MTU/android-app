import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'chart_viewport.dart';
import 'package:intl/intl.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../theme/chart_tokens.dart';

// The plot's layout constants (kChartYAxisWidth / kChartAxisGap / kChartPlotInset)
// live in `chart_tokens.dart` now, with the rest of the chart design tokens — and
// were declared HERE as well, which is exactly the duplication this refactor exists
// to remove. I put it there myself, in the commit that created the tokens. Exported
// rather than moved-and-forgotten, so the dozen files importing them through this
// one keep working.
export '../theme/chart_tokens.dart'
    show kChartYAxisWidth, kChartAxisGap, kChartPlotInset;

/// Hand-rolled chart axis primitives, ported from the Kotlin `ui/charts/
/// ChartAxis.kt`. These are the shared building blocks the bar/line charts use
/// to draw a Y-axis label column, horizontal guide lines, and the X-axis date
/// strip. No third-party chart library is involved: every chart is a
/// [CustomPainter] plus a thin [StatelessWidget] wrapper.

/// The y bounds of a plot.
///
/// Not just a pair: the *rule* for choosing bounds was written twice — the body
/// day chart pads by 8% of the span, the session trace cards by 10% — and both are
/// this idea. A flat series has no span to take a percentage of, so it falls back
/// to the magnitude of the value itself, and a series that never goes negative
/// keeps its floor at zero rather than dipping below it.
@immutable
class ChartRange {
  const ChartRange(this.min, this.max);

  /// Bounds that clear the data by [fraction] of its span, top and bottom.
  factory ChartRange.padded(
    Iterable<double> values, {
    double fraction = 0.08,
    double? floor,
  }) {
    if (values.isEmpty) return const ChartRange(0, 1);
    final min = values.reduce(math.min);
    final max = values.reduce(math.max);
    final span = max - min;
    // A flat line has no span; pad against how big the value is instead, so a
    // steady 70 kg does not get a hairline axis around it.
    final basis = span > 0 ? span : (max.abs() < 1 ? 1.0 : max.abs());
    final padding = basis * fraction;
    final low = min - padding;
    return ChartRange(
      floor != null && low < floor ? floor : low,
      max + padding,
    );
  }

  final double min;
  final double max;
}

/// Whether the chart supports tap-to-select of an individual day. Mirrors the
/// Kotlin `TimeRange.supportsChartDaySelection()`.
extension TimeRangeChartSelection on TimeRange {
  bool get supportsChartDaySelection =>
      this == TimeRange.week || this == TimeRange.month;
}

/// Lays out a Y-axis label column (top→bottom, right-aligned) next to a plot
/// region ([chart]). The plot is expected to be a [CustomPaint] that draws its
/// guide lines via [drawYAxisGuides]. Direct port of Kotlin `YAxisChart`.
class YAxisChart extends StatelessWidget {
  const YAxisChart({
    super.key,
    required this.labels,
    required this.chartHeight,
    required this.chart,
    this.axisWidth = kChartYAxisWidth,
    this.axisGap = kChartAxisGap,
  });

  final List<String> labels;
  final double chartHeight;
  final Widget chart;
  final double axisWidth;
  final double axisGap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final labelStyle = theme.textTheme.labelSmall?.copyWith(
      color: theme.colorScheme.onSurfaceVariant,
    );
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: axisWidth,
          height: chartHeight,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              for (final label in labels)
                Text(
                  label,
                  maxLines: 1,
                  textAlign: TextAlign.end,
                  overflow: TextOverflow.clip,
                  softWrap: false,
                  style: labelStyle,
                ),
            ],
          ),
        ),
        SizedBox(width: axisGap),
        Expanded(child: SizedBox(height: chartHeight, child: chart)),
      ],
    );
  }
}

/// Aligns [child] with the plot region of a [YAxisChart] by insetting it past
/// the Y-axis label column. Port of Kotlin `ChartXAxisWithYAxis`.
class ChartXAxisWithYAxis extends StatelessWidget {
  const ChartXAxisWithYAxis({
    super.key,
    required this.child,
    this.axisWidth = kChartYAxisWidth,
    this.axisGap = kChartAxisGap,
  });

  final Widget child;
  final double axisWidth;
  final double axisGap;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(width: axisWidth + axisGap),
        Expanded(child: child),
      ],
    );
  }
}

/// Which edge a chart draws its axis line along.
///
/// The library's charts draw it down the LEFT, beside the value labels. The
/// body-energy timeline draws it along the BOTTOM, under a 0–100 score that needs
/// a floor rather than a scale. Both are right; having two functions for it was
/// not.
enum ChartAxisLine {
  /// Down the left edge — the default, and what every existing caller gets.
  leading,

  /// Along the bottom edge.
  baseline,

  none,
}

void drawYAxisGuides(
  Canvas canvas,
  Size size, {
  required Color gridColor,
  Color? axisColor,
  int lineCount = 3,
  double strokeWidth = 1,
  ChartAxisLine axisLine = ChartAxisLine.leading,
}) {
  if (lineCount < 2) return;
  final gridPaint = Paint()
    ..color = gridColor
    ..strokeWidth = strokeWidth;
  for (var index = 0; index < lineCount; index++) {
    final y = size.height * index / (lineCount - 1);
    canvas.drawLine(Offset(0, y), Offset(size.width, y), gridPaint);
  }
  if (axisLine == ChartAxisLine.none) return;
  final axisPaint = Paint()
    ..color = axisColor ?? gridColor
    ..strokeWidth = strokeWidth;
  switch (axisLine) {
    case ChartAxisLine.leading:
      canvas.drawLine(Offset.zero, Offset(0, size.height), axisPaint);
    case ChartAxisLine.baseline:
      canvas.drawLine(
        Offset(0, size.height),
        Offset(size.width, size.height),
        axisPaint,
      );
    case ChartAxisLine.none:
      break;
  }
}

/// The three Y-axis labels (max, mid, min). Falls back to a higher-precision
/// formatter when the compact labels collide. Port of Kotlin `chartYAxisLabels`.
List<String> chartYAxisLabels(
  double minValue,
  double maxValue, {
  String Function(double) valueFormatter = formatCompactAxisValue,
}) {
  final min = minValue.isFinite ? minValue : 0.0;
  final max =
      (maxValue.isFinite && maxValue > min) ? maxValue : (min + 1.0);
  final mid = min + (max - min) / 2.0;
  final values = <double>[max, mid, min];
  final labels = values.map(valueFormatter).toList();
  if (labels.toSet().length == labels.length) {
    return labels;
  }
  return values.map((value) => _formatPreciseAxisValue(value, max - min)).toList();
}

/// Compact axis value: `1.2k`, `3M`, `12`, `0`, `4.5`. Port of Kotlin
/// `formatCompactAxisValue`.
String formatCompactAxisValue(double value) {
  final absValue = value.abs();
  if (absValue >= 1000000.0) return '${_trimAxisDecimal(value / 1000000.0, 1)}M';
  if (absValue >= 1000.0) return '${_trimAxisDecimal(value / 1000.0, 1)}k';
  if (absValue >= 10.0) return value.round().toString();
  if (absValue == 0.0) return '0';
  return _trimAxisDecimal(value, 1);
}

String _formatPreciseAxisValue(double value, double range) {
  final decimals = range < 1.0
      ? 2
      : range < 10.0
          ? 1
          : 0;
  final absValue = value.abs();
  if (absValue >= 1000000.0) {
    return '${_trimAxisDecimal(value / 1000000.0, math.max(decimals, 1))}M';
  }
  if (absValue >= 1000.0) {
    return '${_trimAxisDecimal(value / 1000.0, math.max(decimals, 1))}k';
  }
  return _trimAxisDecimal(value, decimals);
}

String _trimAxisDecimal(double value, int decimals) {
  final scale = math.pow(10, decimals).toDouble();
  final rounded = (value * scale).round() / scale;
  if (rounded % 1.0 == 0.0) {
    return rounded.round().toString();
  }
  return rounded.toString();
}

/// Whether the X-axis label at [index] should be rendered, avoiding clutter on
/// dense month/year strips. Port of Kotlin `isPeriodChartLabelVisible`.
bool isPeriodChartLabelVisible(int index, int lastIndex, TimeRange selectedRange) {
  switch (selectedRange) {
    case TimeRange.day:
    case TimeRange.week:
      return true;
    case TimeRange.year:
      return lastIndex <= 11 || index % 30 == 0 || index == lastIndex;
    case TimeRange.month:
      return index % 5 == 0 || index == lastIndex;
  }
}

// Getters, not cached finals, so axis labels follow the current
// Intl.defaultLocale (the app language) rather than freezing at first access.
DateFormat get _chartDayFormat => DateFormat('EEE d');
DateFormat get _chartDayOfMonthFormat => DateFormat('d');
DateFormat get _chartMonthFormat => DateFormat('LLL');

String _periodChartLabel(LocalDate date, TimeRange selectedRange) {
  final dateTime = DateTime(date.year, date.month, date.day);
  switch (selectedRange) {
    case TimeRange.day:
      return _chartDayFormat.format(dateTime);
    case TimeRange.week:
    case TimeRange.month:
      return _chartDayOfMonthFormat.format(dateTime);
    case TimeRange.year:
      return _chartMonthFormat.format(dateTime);
  }
}

/// The X-axis date strip beneath a bar/line chart. Port of Kotlin
/// `PeriodChartXAxis`.
class PeriodChartXAxis extends StatelessWidget {
  const PeriodChartXAxis({
    super.key,
    required this.dates,
    this.viewport = ChartViewport.full,
    required this.selectedRange,
  });

  final List<LocalDate> dates;
  final TimeRange selectedRange;

  /// The slice of the period on show, when the chart above has been pinched.
  final ChartViewport viewport;

  bool _slotIsVisible(int index, double width, double slotWidth) {
    final left = viewport.visibleFraction(index / dates.length) * width;
    return left + slotWidth > 0 && left < width;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final labelStyle = theme.textTheme.labelSmall?.copyWith(
      color: theme.colorScheme.onSurfaceVariant,
    );
    final lastIndex = dates.length - 1;

    Widget label(int index) => isPeriodChartLabelVisible(
          index,
          lastIndex,
          selectedRange,
        )
            ? Text(
                _periodChartLabel(dates[index], selectedRange),
                style: labelStyle,
                textAlign: TextAlign.center,
                maxLines: 1,
                softWrap: false,
                overflow: TextOverflow.clip,
              )
            : const SizedBox(height: 16);

    if (!viewport.isZoomed) {
      // Unzoomed, the slots are the whole row: even Expandeds, exactly as before.
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var index = 0; index < dates.length; index++)
            Expanded(child: Align(alignment: Alignment.topCenter, child: label(index))),
        ],
      );
    }

    // Zoomed, a date has to sit over ITS OWN bar. Evenly spacing whichever labels survive
    // would drift them off the bars they name -- and a bar chart whose dates belong to
    // the wrong bars is worse than one that does not zoom.
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final slotWidth = width / (dates.length * viewport.span);
        return SizedBox(
          height: 16,
          width: width,
          child: Stack(
            children: [
              for (var index = 0; index < dates.length; index++)
                if (_slotIsVisible(index, width, slotWidth))
                  Positioned(
                    left: viewport.visibleFraction(index / dates.length) * width,
                    width: slotWidth,
                    top: 0,
                    child: Align(
                      alignment: Alignment.topCenter,
                      child: label(index),
                    ),
                  ),
            ],
          ),
        );
      },
    );
  }
}

/// The dates in [start]..[end] inclusive.
List<LocalDate> datesInPeriod(LocalDate start, LocalDate end) {
  final result = <LocalDate>[];
  var date = start;
  while (!date.isAfter(end)) {
    result.add(date);
    date = date.plusDays(1);
  }
  return result;
}
