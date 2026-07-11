import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';

/// Hand-rolled chart axis primitives, ported from the Kotlin `ui/charts/
/// ChartAxis.kt`. These are the shared building blocks the bar/line charts use
/// to draw a Y-axis label column, horizontal guide lines, and the X-axis date
/// strip. No third-party chart library is involved: every chart is a
/// [CustomPainter] plus a thin [StatelessWidget] wrapper.

/// Width of the leading Y-axis label column (matches Kotlin `ChartYAxisWidth`).
const double kChartYAxisWidth = 56;

/// Gap between the Y-axis label column and the plot (Kotlin `ChartAxisGap`).
const double kChartAxisGap = 8;

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

/// Draws the horizontal guide lines and the leading vertical axis line inside a
/// plot [Canvas]. Port of Kotlin `DrawScope.drawYAxisGuides`.
void drawYAxisGuides(
  Canvas canvas,
  Size size, {
  required Color gridColor,
  Color? axisColor,
  int lineCount = 3,
  double strokeWidth = 1,
}) {
  if (lineCount < 2) return;
  final gridPaint = Paint()
    ..color = gridColor
    ..strokeWidth = strokeWidth;
  for (var index = 0; index < lineCount; index++) {
    final y = size.height * index / (lineCount - 1);
    canvas.drawLine(Offset(0, y), Offset(size.width, y), gridPaint);
  }
  final axisPaint = Paint()
    ..color = axisColor ?? gridColor
    ..strokeWidth = strokeWidth;
  canvas.drawLine(Offset.zero, Offset(0, size.height), axisPaint);
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

final DateFormat _chartDayFormat = DateFormat('EEE d');
final DateFormat _chartDayOfMonthFormat = DateFormat('d');
final DateFormat _chartMonthFormat = DateFormat('LLL');

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
    required this.selectedRange,
  });

  final List<LocalDate> dates;
  final TimeRange selectedRange;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final labelStyle = theme.textTheme.labelSmall?.copyWith(
      color: theme.colorScheme.onSurfaceVariant,
    );
    final lastIndex = dates.length - 1;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var index = 0; index < dates.length; index++)
          Expanded(
            child: Align(
              alignment: Alignment.topCenter,
              child: isPeriodChartLabelVisible(index, lastIndex, selectedRange)
                  ? Text(
                      _periodChartLabel(dates[index], selectedRange),
                      style: labelStyle,
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      softWrap: false,
                      overflow: TextOverflow.clip,
                    )
                  : const SizedBox(height: 16),
            ),
          ),
      ],
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
