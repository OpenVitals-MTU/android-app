import 'package:flutter/material.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../components/ov_card.dart';
import '../theme/chart_tokens.dart';
import 'chart_axis.dart';
import 'chart_reveal.dart';
import '../../core/stats/stats.dart';

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
      return averageOrZero(values);
    case PeriodBarAggregation.averageNonZero:
      return averageOrZero(values.where((value) => value > 0.0).toList());
  }
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

    _BarChartPainter painterAt(double progress) => _BarChartPainter(
          buckets: buckets,
          accentColor: accentColor,
          selectedDate: selectedDate,
          selectedRange: selectedRange,
          labelStyle: labelStyle,
          valueFormatter: valueFormatter,
          textDirection: Directionality.of(context),
          progress: progress,
        );
    final painter = painterAt(1);
    Widget chart = ChartReveal(
      builder: (context, t) =>
          CustomPaint(size: Size.infinite, painter: painterAt(t)),
    );
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
    required this.progress,
  });

  final List<PeriodChartBucket> buckets;
  final Color accentColor;
  final LocalDate? selectedDate;
  final TimeRange selectedRange;
  final TextStyle labelStyle;
  final String Function(double) valueFormatter;
  final TextDirection textDirection;

  /// 0 → 1: how far the bars have grown. See [ChartReveal].
  final double progress;

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
      final labelLayout = layoutBarLabel(
        text: valueFormatter(value),
        style: labelStyle,
        maxWidth: slotWidth - 2.0,
        textDirection: textDirection,
      );
      final minLabelHeight =
          labelLayout != null ? labelLayout.height + 4.0 : minVisibleHeight;
      final barHeight = (size.height * fraction)
          .clamp(
            minVisibleHeight > minLabelHeight ? minVisibleHeight : minLabelHeight,
            size.height,
          )
          .toDouble();
      final left = slotLeft + (slotWidth - barWidth) / 2.0;
      // Grown by `progress`. The LABEL is not: it is laid out against the bar's
      // final height (that is what reserves room for it) and drawn only once the
      // bar has arrived — a number sliding up inside a growing rectangle is a
      // number nobody can read, and a label that overflows a half-grown bar is
      // worse than one that waits.
      final drawnHeight = barHeight * progress.clamp(0.0, 1.0);
      final top = size.height - drawnHeight;
      // `chartBarRadius` — the rule this chart had already worked out for itself,
      // and which the influence strip and the schedule chart had each answered
      // differently. A bar is a pill until it gets fat.
      final radius = chartBarRadius(barWidth);

      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(left, top, barWidth, drawnHeight),
          Radius.circular(radius),
        ),
        Paint()..color = accentColor,
      );

      if (labelLayout != null) {
        if (progress >= 1.0) {
          _drawLabel(canvas, labelLayout, left, top, barWidth, barHeight);
        }
      }
    }
  }

  double _gapFor(int count) {
    if (count <= 7) return 8;
    if (count <= 12) return 6;
    if (count <= 31) return 3;
    return 1;
  }


  void _drawLabel(
    Canvas canvas,
    BarLabelLayout layout,
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

@visibleForTesting
class BarLabelLayout {
  const BarLabelLayout(this.lines, this.width, this.height);

  final List<TextPainter> lines;
  final double width;
  final double height;
}

/// The value label drawn on a bar, laid out to fit the space a bar actually has.
///
/// It used to be measured against the BAR width and simply dropped when it did
/// not fit — so a day over 10,000 steps lost its number entirely, because
/// "21,104" is one character wider than "9,785" and that one character was the
/// difference between fitting and vanishing. The chart said nothing at all
/// about its own biggest day.
///
/// Two changes stop that. The label may use the whole SLOT (a bar has a gap
/// either side of it, and a label centred over its own bar cannot reach its
/// neighbour's), and if it still does not fit it is stepped down a point at a
/// time rather than abandoned. Only a label that cannot be read at the smallest
/// size we are willing to draw is dropped.
@visibleForTesting
BarLabelLayout? layoutBarLabel({
  required String text,
  required TextStyle style,
  required double maxWidth,
  required TextDirection textDirection,
}) {
  if (text.trim().isEmpty || maxWidth <= 0) return null;
  final lines = splitBarLabel(text) ?? [text.trim()];

  final baseSize = style.fontSize ?? 11.0;
  for (var size = baseSize; size >= _minBarLabelFontSize; size -= 1.0) {
    final scaled = style.copyWith(fontSize: size);
    final painters = <TextPainter>[];
    var fits = true;
    for (final line in lines) {
      final painter = TextPainter(
        text: TextSpan(text: line, style: scaled),
        maxLines: 1,
        textDirection: textDirection,
      )..layout();
      if (painter.width > maxWidth) {
        fits = false;
        break;
      }
      painters.add(painter);
    }
    if (!fits) continue;

    const lineGap = 1.0;
    final height = painters.fold<double>(0, (sum, p) => sum + p.height) +
        lineGap * (painters.length - 1).clamp(0, painters.length);
    final width =
        painters.fold<double>(0, (maxW, p) => p.width > maxW ? p.width : maxW);
    return BarLabelLayout(painters, width, height);
  }
  return null;
}

/// Below this the number is not worth drawing.
const double _minBarLabelFontSize = 8.0;

/// Splits "21,104 steps" into its number and its unit, so the unit can go on a
/// second line instead of squeezing the number off the bar.
@visibleForTesting
List<String>? splitBarLabel(String text) {
  final trimmed = text.trim();
  final splitIndex = trimmed.lastIndexOf(' ');
  if (splitIndex <= 0 || splitIndex >= trimmed.length - 1) return null;
  return [
    trimmed.substring(0, splitIndex),
    trimmed.substring(splitIndex + 1),
  ];
}
