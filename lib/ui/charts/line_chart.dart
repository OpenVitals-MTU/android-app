import 'package:flutter/material.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../components/ov_card.dart';
import 'chart_axis.dart';
import 'chart_curve.dart';
import 'chart_viewport.dart';
import 'chart_zoom.dart';
import 'day_axis.dart';

/// A single line-chart point. [time] (an instant) is used for intraday (DAY)
/// positioning; otherwise the [date] slot is used. Port of Kotlin
/// `MetricLinePoint`.
class MetricLinePoint {
  const MetricLinePoint({required this.date, required this.value, this.time});

  final LocalDate date;
  final double value;
  final DateTime? time;
}

/// A named, coloured line series. Port of Kotlin `MetricLineSeries`.
class MetricLineSeries {
  const MetricLineSeries({required this.points, required this.color, this.label});

  final List<MetricLinePoint> points;
  final Color color;
  final String? label;

  MetricLineSeries copyWithPoints(List<MetricLinePoint> points) =>
      MetricLineSeries(points: points, color: color, label: label);
}

/// Collapses multiple same-day points into one daily average. Port of Kotlin
/// `dailyAverageLinePoints`.
List<MetricLinePoint> dailyAverageLinePoints(List<MetricLinePoint> points) {
  final byDate = <LocalDate, List<double>>{};
  for (final point in points) {
    byDate.putIfAbsent(point.date, () => <double>[]).add(point.value);
  }
  final result = byDate.entries
      .map((entry) => MetricLinePoint(
            date: entry.key,
            value: entry.value.reduce((a, b) => a + b) / entry.value.length,
          ))
      .toList()
    ..sort((a, b) => a.date.compareTo(b.date));
  return result;
}

/// A multi-series line chart card, ported from Kotlin `MetricLineChart`. The
/// axis labels, guide lines, per-day highlight and line series are drawn via a
/// [CustomPainter]; the X-axis (time-of-day for DAY, dates otherwise), optional
/// legend and summary text sit beneath.
class MetricLineChart extends StatelessWidget {
  const MetricLineChart({
    super.key,
    required this.title,
    required this.series,
    required this.selectedRange,
    required this.period,
    required this.accentColor,
    required this.summaryText,
    this.selectedDate,
    this.onDateSelected,
    this.valueFormatter = formatCompactAxisValue,
  });

  final String title;
  final List<MetricLineSeries> series;
  final TimeRange selectedRange;
  final DatePeriod period;
  final Color accentColor;
  final String summaryText;
  final LocalDate? selectedDate;
  final ValueChanged<LocalDate>? onDateSelected;
  final String Function(double) valueFormatter;

  static const double _chartHeight = 150;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    final visibleSeries = series
        .map((s) => s.copyWithPoints(
              s.points
                  .where((point) =>
                      point.value.isFinite &&
                      !point.date.isBefore(period.start) &&
                      !point.date.isAfter(period.end))
                  .toList(),
            ))
        .where((s) => s.points.isNotEmpty)
        .toList();
    final allPoints = visibleSeries.expand((s) => s.points).toList();
    if (allPoints.isEmpty) return const SizedBox.shrink();
    if (selectedRange == TimeRange.day &&
        allPoints
                .map((point) => point.time)
                .whereType<DateTime>()
                .map((time) => time.millisecondsSinceEpoch)
                .toSet()
                .length <=
            1) {
      return const SizedBox.shrink();
    }

    final values = allPoints.map((point) => point.value).toList();
    final minValue = values.reduce((a, b) => a < b ? a : b);
    final maxValue = values.reduce((a, b) => a > b ? a : b);
    final axisRange = _paddedLineAxisRange(minValue, maxValue);
    final axisMin = axisRange.$1;
    final axisMax = axisRange.$2;
    final axisDates = datesInPeriod(period.start, period.end);
    final periodDayCount = axisDates.isEmpty ? 1 : axisDates.length;
    final dayStart = DateTime(period.start.year, period.start.month, period.start.day);
    final dayEnd = dayStart.add(const Duration(days: 1));
    final dayDurationMillis = (dayEnd.millisecondsSinceEpoch -
            dayStart.millisecondsSinceEpoch)
        .clamp(1, 1 << 62);
    final gridColor = accentColor.withValues(alpha: 0.12);
    final axisColor = scheme.outlineVariant.withValues(alpha: 0.8);

    final canSelect = selectedRange.supportsChartDaySelection &&
        onDateSelected != null &&
        axisDates.isNotEmpty;

    // The chart plus its x axis, drawn for a given viewport so both stay in step
    // when the day chart is pinched.
    Widget chartWithAxis(ChartViewport viewport) {
      final painter = _LinePainter(
        series: visibleSeries,
        selectedRange: selectedRange,
        period: period,
        dayStartMillis: dayStart.millisecondsSinceEpoch,
        dayDurationMillis: dayDurationMillis,
        periodDayCount: periodDayCount,
        minValue: axisMin,
        maxValue: axisMax,
        gridColor: gridColor,
        axisColor: axisColor,
        selectedDate: selectedDate,
        axisDates: axisDates,
        highlightColor: accentColor.withValues(alpha: 0.16),
        viewport: viewport,
      );
      Widget plot = CustomPaint(size: Size.infinite, painter: painter);
      if (canSelect) {
        plot = LayoutBuilder(
          builder: (context, constraints) => GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTapUp: (details) {
              // Map the tap back through the viewport so a zoomed chart selects
              // the date actually under the finger, not the unzoomed slot.
              final visible = (details.localPosition.dx / constraints.maxWidth)
                  .clamp(0.0, 1.0);
              final index = (viewport.dataFraction(visible) * axisDates.length)
                  .floor()
                  .clamp(0, axisDates.length - 1);
              onDateSelected!(axisDates[index]);
            },
            child: CustomPaint(size: Size.infinite, painter: painter),
          ),
        );
      }
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          YAxisChart(
            labels: chartYAxisLabels(
              axisMin,
              axisMax,
              valueFormatter: valueFormatter,
            ),
            chartHeight: _chartHeight,
            chart: plot,
          ),
          const SizedBox(height: 8),
          if (selectedRange == TimeRange.day)
            ChartXAxisWithYAxis(
              child: DayAxisLabels(inset: 0, viewport: viewport),
            )
          else
            ChartXAxisWithYAxis(
              child: PeriodChartXAxis(
                dates: axisDates,
                selectedRange: selectedRange,
                viewport: viewport,
              ),
            ),
        ],
      );
    }

    // Every range pinches: the day chart on its hour scale, the week/month/year
    // charts on their date slots — the line maps its x through the viewport and
    // PeriodChartXAxis reflows its labels to match.
    final chart =
        ChartZoom(builder: (context, viewport) => chartWithAxis(viewport));

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: theme.textTheme.titleSmall),
            const SizedBox(height: 12),
            chart,
            if (visibleSeries.length > 1) ...[
              const SizedBox(height: 8),
              _LineLegend(series: visibleSeries),
            ],
            const SizedBox(height: 8),
            Text(
              summaryText,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

(double, double) _paddedLineAxisRange(double minValue, double maxValue) {
  final range = maxValue - minValue;
  final padding = range == 0.0
      ? (maxValue.abs() * 0.05 > 1.0 ? maxValue.abs() * 0.05 : 1.0)
      : range * 0.08;
  return (minValue - padding, maxValue + padding);
}

class _LineLegend extends StatelessWidget {
  const _LineLegend({required this.series});

  final List<MetricLineSeries> series;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        for (final item in series)
          if (item.label != null)
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(right: 12),
                child: Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      margin: const EdgeInsets.only(top: 5),
                      decoration: BoxDecoration(
                        color: item.color,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        item.label!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.labelSmall
                            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                    ),
                  ],
                ),
              ),
            ),
      ],
    );
  }
}

class _LinePainter extends CustomPainter {
  _LinePainter({
    required this.series,
    required this.selectedRange,
    required this.period,
    required this.dayStartMillis,
    required this.dayDurationMillis,
    required this.periodDayCount,
    required this.minValue,
    required this.maxValue,
    required this.gridColor,
    required this.axisColor,
    required this.selectedDate,
    required this.axisDates,
    required this.highlightColor,
    this.viewport = ChartViewport.full,
  });

  final List<MetricLineSeries> series;
  final TimeRange selectedRange;
  final DatePeriod period;
  final int dayStartMillis;
  final int dayDurationMillis;
  final int periodDayCount;
  final double minValue;
  final double maxValue;
  final Color gridColor;
  final Color axisColor;
  final LocalDate? selectedDate;
  final List<LocalDate> axisDates;
  final Color highlightColor;

  /// The visible slice of the x range when the chart has been pinched.
  final ChartViewport viewport;

  @override
  void paint(Canvas canvas, Size size) {
    drawYAxisGuides(canvas, size, gridColor: gridColor, axisColor: axisColor);
    _drawSelectedHighlight(canvas, size);
    // Zoomed, the line runs past the plot edges; clip so it ends at the plot
    // rather than spilling across the card.
    final zoomed = viewport.isZoomed;
    if (zoomed) {
      canvas.save();
      canvas.clipRect(Offset.zero & size);
    }
    for (final line in series) {
      _drawSeries(canvas, size, line);
    }
    if (zoomed) canvas.restore();
  }

  void _drawSelectedHighlight(Canvas canvas, Size size) {
    final date = selectedDate;
    if (!selectedRange.supportsChartDaySelection ||
        date == null ||
        date.isBefore(period.start) ||
        date.isAfter(period.end) ||
        axisDates.isEmpty) {
      return;
    }
    final index = axisDates.indexOf(date);
    if (index < 0) return;
    final left = size.width * viewport.visibleFraction(index / axisDates.length);
    final slotWidth = size.width / (axisDates.length * viewport.span);
    canvas.drawRect(
      Rect.fromLTWH(left, 0, slotWidth, size.height),
      Paint()..color = highlightColor,
    );
  }

  void _drawSeries(Canvas canvas, Size size, MetricLineSeries line) {
    final rawRange = maxValue - minValue;
    final range = rawRange < 1.0 ? 1.0 : rawRange;
    final positioned = <Offset>[];
    for (final point in line.points) {
      final double xFraction;
      if (selectedRange == TimeRange.day) {
        final pointMillis = (point.time ??
                DateTime(point.date.year, point.date.month, point.date.day))
            .millisecondsSinceEpoch;
        final elapsed =
            (pointMillis - dayStartMillis).clamp(0, dayDurationMillis);
        xFraction = elapsed / dayDurationMillis;
      } else {
        final daysFromStart = (point.date.epochDay - period.start.epochDay)
            .clamp(0, periodDayCount - 1);
        xFraction = (daysFromStart + 0.5) / periodDayCount;
      }
      // Full viewport is a no-op (visibleFraction(f) == f), so period charts and
      // an unzoomed day chart position exactly as before.
      final x = size.width * viewport.visibleFraction(xFraction);
      final y = size.height *
          (1.0 - ((point.value - minValue) / range).clamp(0.0, 1.0));
      positioned.add(Offset(x, y));
    }

    canvas.drawPath(
      smoothPath(positioned),
      Paint()
        ..color = line.color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round,
    );
    final pointPaint = Paint()..color = line.color;
    for (final point in positioned) {
      canvas.drawCircle(point, 3.5, pointPaint);
    }
  }

  @override
  bool shouldRepaint(covariant _LinePainter oldDelegate) => true;
}
