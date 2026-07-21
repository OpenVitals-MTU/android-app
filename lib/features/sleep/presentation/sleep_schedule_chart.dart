import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart' hide TextDirection;

import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/chart_paint.dart';
import '../../../ui/charts/schedule_axis.dart';
import '../../../ui/theme/chart_tokens.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/sleep_display.dart';
import '../../../ui/theme/chart_colors.dart';

/// Port of the Kotlin `SleepScheduleChart.kt`: a time-aligned, stage-coloured
/// bar per night on a shared clock-time axis, for the week and month views.

const double _chartHeight = kChartHeightSchedule;

/// This chart reserves its label column on the RIGHT — the painter writes the hour
/// scale there — so its plot starts at the card's left edge and its x-axis row is
/// padded on the right instead. That is deliberate, not a forgotten
/// `kChartPlotInset`: the charts built on `MetricLinePlot` put their value labels
/// on the LEFT, so their axis rows inset from the left to match. Two conventions,
/// each internally consistent; what is not allowed is a row that matches neither.
const double _axisLabelWidth = kChartRightAxisWidth;

/// [SleepScheduleDay] and `toSleepScheduleDays` live in `application/
/// sleep_display.dart`: the nights arrive precomputed and this file only paints
/// them.

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

      // Draw the whole time in bed as one base bar, then overlay stage colour
      // where the device staged it. So a night the device recorded but only
      // partly staged (or never staged) reads as its FULL duration — a solid bar
      // with detail on the staged part — rather than a tiny fragment floating in
      // an empty slot, or (the "dark blue nights with data" bug) a uniform block.
      // The bar carries the SAME merged night the day card shows: naps are peeled
      // off upstream and internal wake gaps are Awake-filled, so there are no
      // large holes to split around.
      final barRRect = RRect.fromRectAndRadius(
        Rect.fromLTRB(
            left, yFor(startMinute), left + barWidth, yFor(endMinute)),
        Radius.circular(cornerRadius),
      );
      canvas.drawRRect(barRRect, Paint()..color = emptyBarColor);
      if (segments.isEmpty) continue;

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
      drawDashedLine(canvas, Offset(0, y), Offset(barsWidth, y), linePaint);

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
