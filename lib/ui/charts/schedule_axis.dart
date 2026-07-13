import 'dart:math' as math;

import 'package:flutter/foundation.dart';

import '../../features/sleep/application/sleep_display.dart';

/// Where a moment sits on the SLEEP SCHEDULE's axis, and where that axis starts.
///
/// The third axis in the library, beside [DayAxis] (the x axis is the whole day,
/// from midnight) and [SessionAxis] (the x axis is the whole session). This one is
/// vertical, it is measured in minutes, and it is anchored at 18:00 — because a
/// night straddles midnight, and an axis that started there would cut every normal
/// night in half and draw it as two bars.
///
/// It lived in the sleep feature, which is exactly how the app came to have two
/// unrelated answers to "where does a time go on an axis". The chart that uses it
/// keeps its own painter — a bar per night is not a shape anything else draws —
/// but the KNOWLEDGE belongs here, next to the other two.

/// Minute-of-day the vertical axis is anchored at (18:00), so a normal night —
/// which straddles midnight — stays one contiguous bar.
const int kAnchorMinuteOfDay = 18 * 60;
const int kMinutesPerDay = 24 * 60;

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
