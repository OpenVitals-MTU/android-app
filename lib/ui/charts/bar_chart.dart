import 'package:flutter/material.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../components/ov_card.dart';
import 'chart_axis.dart';

/// A single dated value fed into the period charts. Port of Kotlin
/// `PeriodChartValue`.
class PeriodChartValue {
  const PeriodChartValue(this.date, this.value);

  final LocalDate date;
  final double value;
}

/// An aggregated bar bucket (one bar). Port of Kotlin `PeriodChartBucket`.
class PeriodChartBucket {
  const PeriodChartBucket(this.date, this.value);

  final LocalDate date;
  final double value;
}

/// How daily values are rolled up into monthly bars in the YEAR range. Port of
/// Kotlin `PeriodBarAggregation`.
enum PeriodBarAggregation { sum, average, averageNonZero }

/// Buckets [values] into one bar per day (DAY/WEEK/MONTH) or one bar per month
/// (YEAR, aggregated by [yearAggregation]). Port of Kotlin `periodBarBuckets`.
List<PeriodChartBucket> periodBarBuckets(
  List<PeriodChartValue> values,
  TimeRange selectedRange,
  DatePeriod period, {
  PeriodBarAggregation yearAggregation = PeriodBarAggregation.sum,
}) {
  final dailyBuckets = _dailyBuckets(values, period);
  if (selectedRange != TimeRange.year) {
    return dailyBuckets;
  }

  final endMonth = period.end.withDayOfMonth(1);
  final result = <PeriodChartBucket>[];
  var monthStart = period.start.withDayOfMonth(1);
  while (!monthStart.isAfter(endMonth)) {
    final monthEnd = monthStart.plusMonths(1).minusDays(1);
    final monthValues = dailyBuckets
        .where((bucket) =>
            !bucket.date.isBefore(monthStart) && !bucket.date.isAfter(monthEnd))
        .map((bucket) => bucket.value)
        .toList();
    result.add(
      PeriodChartBucket(monthStart, _aggregate(monthValues, yearAggregation)),
    );
    monthStart = monthStart.plusMonths(1);
  }
  return result;
}

double _aggregate(List<double> values, PeriodBarAggregation aggregation) {
  switch (aggregation) {
    case PeriodBarAggregation.sum:
      return values.fold(0.0, (sum, value) => sum + value);
    case PeriodBarAggregation.average:
      return _averageOrZero(values);
    case PeriodBarAggregation.averageNonZero:
      return _averageOrZero(values.where((value) => value > 0.0).toList());
  }
}

double _averageOrZero(List<double> values) {
  if (values.isEmpty) return 0.0;
  return values.fold(0.0, (sum, value) => sum + value) / values.length;
}

List<PeriodChartBucket> _dailyBuckets(
  List<PeriodChartValue> values,
  DatePeriod period,
) {
  final byDate = <LocalDate, double>{};
  for (final value in values) {
    byDate[value.date] = (byDate[value.date] ?? 0.0) + value.value;
  }
  final result = <PeriodChartBucket>[];
  var date = period.start;
  while (!date.isAfter(period.end)) {
    result.add(PeriodChartBucket(date, byDate[date] ?? 0.0));
    date = date.plusDays(1);
  }
  return result;
}

/// A bar chart card for the DAY/WEEK/MONTH (and YEAR month-rollup) ranges. Port
/// of Kotlin `PeriodBarChart`. The bars, value labels and week selection
/// highlight are drawn by a [CustomPainter]; the X-axis and summary text sit
/// beneath.
class PeriodBarChart extends StatelessWidget {
  const PeriodBarChart({
    super.key,
    required this.title,
    required this.values,
    required this.selectedRange,
    required this.period,
    required this.accentColor,
    required this.summaryText,
    this.yearAggregation = PeriodBarAggregation.sum,
    this.chartHeight = 120,
    this.selectedDate,
    this.onDateSelected,
    this.valueFormatter = formatCompactAxisValue,
  });

  final String title;
  final List<PeriodChartValue> values;
  final TimeRange selectedRange;
  final DatePeriod period;
  final Color accentColor;
  final String summaryText;
  final PeriodBarAggregation yearAggregation;
  final double chartHeight;
  final LocalDate? selectedDate;
  final ValueChanged<LocalDate>? onDateSelected;
  final String Function(double) valueFormatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final buckets = periodBarBuckets(
      values,
      selectedRange,
      period,
      yearAggregation: yearAggregation,
    );
    final labelColor = accentColor.computeLuminance() > 0.25
        ? Colors.black.withValues(alpha: 0.78)
        : Colors.white;
    final labelStyle = theme.textTheme.labelSmall!.copyWith(
      color: labelColor,
      fontWeight: FontWeight.bold,
    );

    final canSelect = selectedRange == TimeRange.week &&
        onDateSelected != null &&
        buckets.isNotEmpty;

    final painter = _BarChartPainter(
      buckets: buckets,
      accentColor: accentColor,
      selectedDate: selectedDate,
      selectedRange: selectedRange,
      labelStyle: labelStyle,
      valueFormatter: valueFormatter,
      textDirection: Directionality.of(context),
    );
    Widget chart = CustomPaint(size: Size.infinite, painter: painter);
    if (canSelect) {
      // Wrap in a LayoutBuilder so the tap resolves against the real plot width.
      chart = LayoutBuilder(
        builder: (context, constraints) {
          return GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTapUp: (details) => _handleTap(
              details.localPosition.dx,
              constraints.maxWidth,
              buckets,
            ),
            child: CustomPaint(size: Size.infinite, painter: painter),
          );
        },
      );
    }

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: theme.textTheme.titleSmall
                  ?.copyWith(color: theme.colorScheme.onSurface),
            ),
            const SizedBox(height: 12),
            SizedBox(height: chartHeight, child: chart),
            const SizedBox(height: 8),
            PeriodChartXAxis(
              dates: buckets.map((bucket) => bucket.date).toList(),
              selectedRange: selectedRange,
            ),
            const SizedBox(height: 8),
            Text(
              summaryText,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }

  void _handleTap(double x, double width, List<PeriodChartBucket> buckets) {
    if (buckets.isEmpty || width <= 0) return;
    final slotWidth = width / buckets.length;
    final index = (x / slotWidth).floor().clamp(0, buckets.length - 1);
    onDateSelected?.call(buckets[index].date);
  }
}

class _BarChartPainter extends CustomPainter {
  _BarChartPainter({
    required this.buckets,
    required this.accentColor,
    required this.selectedDate,
    required this.selectedRange,
    required this.labelStyle,
    required this.valueFormatter,
    required this.textDirection,
  });

  final List<PeriodChartBucket> buckets;
  final Color accentColor;
  final LocalDate? selectedDate;
  final TimeRange selectedRange;
  final TextStyle labelStyle;
  final String Function(double) valueFormatter;
  final TextDirection textDirection;

  @override
  void paint(Canvas canvas, Size size) {
    if (buckets.isEmpty) return;

    final maxValue = buckets
        .map((bucket) => bucket.value)
        .fold<double>(1.0, (currentMax, value) => value > currentMax ? value : currentMax);

    final slotWidth = size.width / buckets.length;
    final gap = _gapFor(buckets.length).clamp(0.0, slotWidth * 0.6);
    final barWidth = (slotWidth - gap).clamp(1.0, double.infinity);
    const minVisibleHeight = 4.0;

    for (var index = 0; index < buckets.length; index++) {
      final bucket = buckets[index];
      final slotLeft = index * slotWidth;
      final isSelected =
          selectedDate == bucket.date && selectedRange == TimeRange.week;
      if (isSelected) {
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            Rect.fromLTWH(slotLeft, 0, slotWidth, size.height),
            const Radius.circular(8),
          ),
          Paint()..color = accentColor.withValues(alpha: 0.16),
        );
      }

      final value = bucket.value < 0.0 ? 0.0 : bucket.value;
      if (value <= 0.0) continue;

      final fraction = (value / maxValue).clamp(0.0, 1.0);
      final labelLayout = _measureLabel(valueFormatter(value), barWidth);
      final minLabelHeight =
          labelLayout != null ? labelLayout.height + 4.0 : minVisibleHeight;
      final barHeight = (size.height * fraction)
          .clamp(
            minVisibleHeight > minLabelHeight ? minVisibleHeight : minLabelHeight,
            size.height,
          )
          .toDouble();
      final left = slotLeft + (slotWidth - barWidth) / 2.0;
      final top = size.height - barHeight;
      final radius = (barWidth / 2.0).clamp(0.0, 8.0);

      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(left, top, barWidth, barHeight),
          Radius.circular(radius),
        ),
        Paint()..color = accentColor,
      );

      if (labelLayout != null) {
        _drawLabel(canvas, labelLayout, left, top, barWidth, barHeight);
      }
    }
  }

  double _gapFor(int count) {
    if (count <= 7) return 8;
    if (count <= 12) return 6;
    if (count <= 31) return 3;
    return 1;
  }

  _BarLabelLayout? _measureLabel(String text, double width) {
    if (text.trim().isEmpty) return null;
    const horizontalPadding = 2.0;
    final maxWidth = width - horizontalPadding * 2.0;
    final split = _splitLabel(text);
    final lines = split ?? [text.trim()];
    final painters = <TextPainter>[];
    for (final line in lines) {
      final painter = TextPainter(
        text: TextSpan(text: line, style: labelStyle),
        maxLines: 1,
        textDirection: textDirection,
      )..layout();
      if (painter.width > maxWidth) return null;
      painters.add(painter);
    }
    const lineGap = 1.0;
    final height = painters.fold<double>(0, (sum, p) => sum + p.height) +
        lineGap * (painters.length - 1).clamp(0, painters.length);
    final labelWidth =
        painters.fold<double>(0, (maxW, p) => p.width > maxW ? p.width : maxW);
    return _BarLabelLayout(painters, labelWidth, height);
  }

  List<String>? _splitLabel(String text) {
    final trimmed = text.trim();
    final splitIndex = trimmed.lastIndexOf(' ');
    if (splitIndex <= 0 || splitIndex >= trimmed.length - 1) return null;
    return [
      trimmed.substring(0, splitIndex),
      trimmed.substring(splitIndex + 1),
    ];
  }

  void _drawLabel(
    Canvas canvas,
    _BarLabelLayout layout,
    double left,
    double top,
    double width,
    double height,
  ) {
    const verticalPadding = 2.0;
    if (height < layout.height + verticalPadding * 2.0) return;
    var lineTop = top + (height - layout.height) / 2.0;
    for (final painter in layout.lines) {
      painter.paint(canvas, Offset(left + (width - painter.width) / 2.0, lineTop));
      lineTop += painter.height + 1.0;
    }
  }

  @override
  bool shouldRepaint(covariant _BarChartPainter oldDelegate) => true;
}

class _BarLabelLayout {
  const _BarLabelLayout(this.lines, this.width, this.height);

  final List<TextPainter> lines;
  final double width;
  final double height;
}
