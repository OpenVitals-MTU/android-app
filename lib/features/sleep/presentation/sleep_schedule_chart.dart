import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart' hide TextDirection;

import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/sleep_display.dart';
import 'sleep_stage_chart.dart';

/// Port of the Kotlin `SleepScheduleChart.kt`: a time-aligned, stage-coloured
/// bar per night on a shared clock-time axis, for the week and month views.

/// Minute-of-day the vertical axis is anchored at (18:00), so a normal night —
/// which straddles midnight — stays one contiguous bar.
const int kAnchorMinuteOfDay = 18 * 60;
const int kMinutesPerDay = 24 * 60;

const double _chartHeight = 232;
/// This chart reserves its label column on the RIGHT — the painter writes the hour
/// scale there — so its plot starts at the card's left edge and its x-axis row is
/// padded on the right instead. That is deliberate, not a forgotten
/// `kChartPlotInset`: the charts built on `MetricLinePlot` put their value labels
/// on the LEFT, so their axis rows inset from the left to match. Two conventions,
/// each internally consistent; what is not allowed is a row that matches neither.
const double _axisLabelWidth = 46;

/// [SleepScheduleDay] and `toSleepScheduleDays` live in `application/
/// sleep_display.dart`: the nights arrive precomputed and this file only paints
/// them.

/// Kotlin `Instant.anchoredMinutes`: minutes since the 18:00 anchor, in [0, 1440).
double anchoredMinutes(DateTime time) {
  final local = time.toLocal();
  final minuteOfDay = local.hour * 60 + local.minute + local.second / 60.0;
  return (minuteOfDay - kAnchorMinuteOfDay + kMinutesPerDay) % kMinutesPerDay;
}

/// Kotlin `Instant.normalizedEndMinutes`: anchored minutes for [value] measured
/// from this night's [start], so a wake-up on the next calendar day stays
/// monotonically after the bedtime instead of wrapping to the top of the chart.
double normalizedEndMinutes(DateTime start, DateTime value) {
  final startMinute = anchoredMinutes(start);
  final valueMinute = anchoredMinutes(value);
  return valueMinute < startMinute ? valueMinute + kMinutesPerDay : valueMinute;
}

/// Kotlin `minuteOfDayToAnchored`.
double minuteOfDayToAnchored(int minuteOfDay) =>
    ((minuteOfDay - kAnchorMinuteOfDay + kMinutesPerDay) % kMinutesPerDay)
        .toDouble();

/// Kotlin `anchoredMinuteToClock`.
({int hour, int minute}) anchoredMinuteToClock(int anchoredMinute) {
  final minuteOfDay =
      ((kAnchorMinuteOfDay + anchoredMinute) % kMinutesPerDay + kMinutesPerDay) %
          kMinutesPerDay;
  return (hour: minuteOfDay ~/ 60, minute: minuteOfDay % 60);
}

/// Kotlin `ScheduleAxis`: the vertical range, in anchored minutes.
@immutable
class ScheduleAxis {
  const ScheduleAxis({required this.min, required this.max});

  final double min;
  final double max;

  double get span => math.max(1.0, max - min);

  /// Whole-hour tick positions, thinned to two-hourly once the range is tall.
  List<int> labelMinutes() {
    final step = span > 8 * 60 ? 120 : 60;
    final first = (min / step).ceil() * step;
    final last = (max / step).floor() * step;
    if (last < first) return [min.toInt()];
    return [for (var minute = first; minute <= last; minute += step) minute];
  }
}

/// Kotlin `scheduleAxisRange`. Null when no night has an in-bed window, which is
/// the caller's signal to fall back to the duration bar chart.
ScheduleAxis? scheduleAxisRange(List<SleepScheduleDay> days) {
  var min = double.maxFinite;
  var max = -double.maxFinite;
  for (final day in days) {
    final start = day.inBedStart;
    final end = day.inBedEnd;
    if (start == null || end == null) continue;
    final startMinute = anchoredMinutes(start);
    final endMinute = normalizedEndMinutes(start, end);
    if (startMinute < min) min = startMinute;
    if (endMinute > max) max = endMinute;
  }
  if (min == double.maxFinite || max <= min) return null;
  // Pad to whole hours so the top and bottom gridlines frame the bars.
  return ScheduleAxis(
    min: (min / 60.0).floorToDouble() * 60.0,
    max: (max / 60.0).ceilToDouble() * 60.0,
  );
}

/// Kotlin `SleepScheduleStageChart`.
class SleepScheduleStageChart extends StatelessWidget {
  const SleepScheduleStageChart({
    super.key,
    required this.title,
    required this.summaryText,
    required this.days,
    required this.selectedRange,
    this.averageSchedule,
    this.selectedDate,
    this.onDateSelected,
  });

  final String title;
  final String summaryText;
  final List<SleepScheduleDay> days;
  final TimeRange selectedRange;
  final SleepOverviewSchedule? averageSchedule;
  final LocalDate? selectedDate;
  final ValueChanged<LocalDate>? onDateSelected;

  @override
  Widget build(BuildContext context) {
    final axis = scheduleAxisRange(days);
    if (axis == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final timeFormat = DateFormat.jm(locale);

    String clockLabel(int anchoredMinute) {
      final clock = anchoredMinuteToClock(anchoredMinute);
      return timeFormat.format(DateTime(2000, 1, 1, clock.hour, clock.minute));
    }

    final schedule = averageSchedule;
    final averageMarkers = schedule == null
        ? const <(double, String)>[]
        : [
            (
              minuteOfDayToAnchored(schedule.startMinute),
              timeFormat.format(DateTime(2000, 1, 1, schedule.startMinute ~/ 60,
                  schedule.startMinute % 60)),
            ),
            (
              minuteOfDayToAnchored(schedule.endMinute),
              timeFormat.format(DateTime(2000, 1, 1, schedule.endMinute ~/ 60,
                  schedule.endMinute % 60)),
            ),
          ];

    final painter = _ScheduleChartPainter(
      days: days,
      axis: axis,
      selectedDate: selectedDate,
      selectedRange: selectedRange,
      axisLabels: [
        for (final minute in axis.labelMinutes()) (minute, clockLabel(minute)),
      ],
      averageMarkers: averageMarkers,
      labelStyle: theme.textTheme.labelSmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant) ??
          const TextStyle(fontSize: 11),
      gridColor: theme.colorScheme.outlineVariant.withValues(alpha: 0.5),
      selectionColor: AppColors.sleep.withValues(alpha: 0.16),
      emptyBarColor: AppColors.sleep.withValues(alpha: 0.5),
      averageLineColor: theme.colorScheme.onSurface.withValues(alpha: 0.85),
    );

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
            LayoutBuilder(
              builder: (context, constraints) => GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTapUp: onDateSelected == null || days.isEmpty
                    ? null
                    : (details) => _handleTap(details, constraints.maxWidth),
                child: CustomPaint(
                  size: Size(constraints.maxWidth, _chartHeight),
                  painter: painter,
                ),
              ),
            ),
            const SizedBox(height: 8),
            Padding(
              padding: const EdgeInsets.only(right: _axisLabelWidth),
              child: PeriodChartXAxis(
                dates: [for (final day in days) day.date],
                selectedRange: selectedRange,
              ),
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

  /// Maps a tap's x to the night whose slot it landed in. The right-hand axis
  /// gutter is not a slot, so the bars share only the remaining width.
  void _handleTap(TapUpDetails details, double width) {
    final barsWidth = math.max(1.0, width - _axisLabelWidth);
    final slotWidth = barsWidth / days.length;
    final index =
        (details.localPosition.dx / slotWidth).floor().clamp(0, days.length - 1);
    onDateSelected!(days[index].date);
  }
}

class _ScheduleChartPainter extends CustomPainter {
  const _ScheduleChartPainter({
    required this.days,
    required this.axis,
    required this.selectedDate,
    required this.selectedRange,
    required this.axisLabels,
    required this.averageMarkers,
    required this.labelStyle,
    required this.gridColor,
    required this.selectionColor,
    required this.emptyBarColor,
    required this.averageLineColor,
  });

  final List<SleepScheduleDay> days;
  final ScheduleAxis axis;
  final LocalDate? selectedDate;
  final TimeRange selectedRange;
  final List<(int, String)> axisLabels;
  final List<(double, String)> averageMarkers;
  final TextStyle labelStyle;
  final Color gridColor;
  final Color selectionColor;
  final Color emptyBarColor;
  final Color averageLineColor;

  TextPainter _measure(String text, TextStyle style) => TextPainter(
        text: TextSpan(text: text, style: style),
        textDirection: TextDirection.ltr,
      )..layout();

  @override
  void paint(Canvas canvas, Size size) {
    if (days.isEmpty || size.width <= 0 || size.height <= 0) return;

    final barsWidth = math.max(1.0, size.width - _axisLabelWidth);
    final slotWidth = barsWidth / days.length;
    final gap = math.min(
      days.length <= 7 ? 10.0 : (days.length <= 12 ? 6.0 : 3.0),
      slotWidth * 0.6,
    );
    final barWidth = math.max(1.0, slotWidth - gap);
    final cornerRadius = math.min(barWidth / 2, 8.0);

    double yFor(double anchoredMinute) =>
        (size.height * ((anchoredMinute - axis.min) / axis.span))
            .clamp(0.0, size.height);

    // Clock-time gridlines, labelled down the right-hand gutter.
    final gridPaint = Paint()
      ..color = gridColor
      ..strokeWidth = 1;
    for (final (minute, label) in axisLabels) {
      final y = yFor(minute.toDouble());
      canvas.drawLine(Offset(0, y), Offset(barsWidth, y), gridPaint);
      final measured = _measure(label, labelStyle);
      measured.paint(
        canvas,
        Offset(
          barsWidth + (_axisLabelWidth - measured.width) / 2,
          (y - measured.height / 2).clamp(0.0, size.height - measured.height),
        ),
      );
    }

    for (var index = 0; index < days.length; index++) {
      final day = days[index];
      final slotLeft = index * slotWidth;

      if (selectedDate == day.date && selectedRange == TimeRange.week) {
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            Rect.fromLTWH(slotLeft, 0, slotWidth, size.height),
            const Radius.circular(8),
          ),
          Paint()..color = selectionColor,
        );
      }

      final start = day.inBedStart;
      final end = day.inBedEnd;
      if (start == null || end == null) continue;
      final startMinute = anchoredMinutes(start);
      final endMinute = normalizedEndMinutes(start, end);
      if (endMinute <= startMinute) continue;

      final left = slotLeft + (slotWidth - barWidth) / 2;
      final barRect = Rect.fromLTRB(
        left,
        yFor(startMinute),
        left + barWidth,
        yFor(endMinute),
      );
      final barRRect =
          RRect.fromRectAndRadius(barRect, Radius.circular(cornerRadius));

      // Stage segments, measured from this night's start so they stay ordered.
      final segments = <(int, double, double)>[];
      for (final stage in day.stages) {
        final segStart = _clampTime(stage.startTime, start, end);
        final segEnd = _clampTime(stage.endTime, start, end);
        if (!segStart.isBefore(segEnd)) continue;
        segments.add((
          stage.stageType,
          normalizedEndMinutes(start, segStart).clamp(startMinute, endMinute),
          normalizedEndMinutes(start, segEnd).clamp(startMinute, endMinute),
        ));
      }

      if (segments.isEmpty) {
        // A night with no stage detail is a solid bar, not an empty slot.
        canvas.drawRRect(barRRect, Paint()..color = emptyBarColor);
        continue;
      }

      canvas.save();
      canvas.clipRRect(barRRect);
      for (final (stageType, segStart, segEnd) in segments) {
        final top = yFor(segStart);
        final bottom = yFor(segEnd);
        canvas.drawRect(
          Rect.fromLTWH(left, top, barWidth, math.max(0.0, bottom - top)),
          Paint()..color = sleepStageColor(stageType),
        );
      }
      canvas.restore();
    }

    _paintAverageMarkers(canvas, size, barsWidth, yFor);
  }

  /// Dashed average bedtime / wake-up lines, each with a time chip at the left.
  void _paintAverageMarkers(
    Canvas canvas,
    Size size,
    double barsWidth,
    double Function(double) yFor,
  ) {
    if (averageMarkers.isEmpty) return;
    final linePaint = Paint()
      ..color = averageLineColor
      ..strokeWidth = 1.5;
    final chipStyle = labelStyle.copyWith(color: Colors.white);

    for (final (anchoredMinute, label) in averageMarkers) {
      final y = yFor(anchoredMinute);
      _drawDashedLine(canvas, Offset(0, y), Offset(barsWidth, y), linePaint);

      final measured = _measure(label, chipStyle);
      const padH = 5.0;
      const padV = 2.0;
      final chipHeight = measured.height + padV * 2;
      final chipTop = (y - chipHeight / 2).clamp(0.0, size.height - chipHeight);
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(0, chipTop, measured.width + padH * 2, chipHeight),
          Radius.circular(chipHeight / 2),
        ),
        Paint()..color = averageLineColor,
      );
      measured.paint(canvas, Offset(padH, chipTop + padV));
    }
  }

  /// Flutter has no `PathEffect.dashPathEffect`, so the 8-on/6-off dash is
  /// stepped by hand.
  void _drawDashedLine(Canvas canvas, Offset from, Offset to, Paint paint) {
    const dash = 8.0;
    const space = 6.0;
    final total = (to - from).distance;
    if (total <= 0) return;
    final direction = (to - from) / total;
    var travelled = 0.0;
    while (travelled < total) {
      final end = math.min(travelled + dash, total);
      canvas.drawLine(
        from + direction * travelled,
        from + direction * end,
        paint,
      );
      travelled = end + space;
    }
  }

  static DateTime _clampTime(DateTime value, DateTime low, DateTime high) {
    if (value.isBefore(low)) return low;
    if (value.isAfter(high)) return high;
    return value;
  }

  @override
  bool shouldRepaint(_ScheduleChartPainter oldDelegate) =>
      oldDelegate.days != days ||
      oldDelegate.axis.min != axis.min ||
      oldDelegate.axis.max != axis.max ||
      oldDelegate.selectedDate != selectedDate ||
      oldDelegate.averageMarkers != averageMarkers;
}
