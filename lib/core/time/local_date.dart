import 'dart:math' as math;

import 'package:clock/clock.dart';

/// A date without a time zone, mirroring `java.time.LocalDate`.
///
/// In the Kotlin app instants are `java.time.Instant` and calendar dates are
/// `java.time.LocalDate`. In the Dart port instants are represented as UTC
/// [DateTime]s and calendar dates by this value type. Arithmetic matches the
/// Java semantics used by the source (notably: `plusMonths`/`plusYears` clamp
/// the day-of-month to the target month length).
class LocalDate implements Comparable<LocalDate> {
  // A gross-range guard (debug-only, and const-evaluable so `const LocalDate`
  // keeps working). It can't call _lengthOfMonth in a const context, so it can't
  // reject Feb-31 here — [withDayOfMonth] clamps to the month length instead, so
  // no phantom date (which downstream ISO-date route parsing silently rejects)
  // reaches the rest of the app.
  const LocalDate(this.year, this.month, this.day)
      : assert(month >= 1 && month <= 12, 'month must be 1-12'),
        assert(day >= 1 && day <= 31, 'day must be 1-31');

  final int year;

  /// 1-12.
  final int month;

  /// 1-31.
  final int day;

  /// Today, per [clock] — the system clock in production, a fixed one under
  /// `withClock` in tests, so "what day is it" is pinnable across the app.
  factory LocalDate.now() {
    final now = clock.now();
    return LocalDate(now.year, now.month, now.day);
  }

  /// The calendar date of [dateTime] in its own zone (call `.toLocal()` first
  /// to convert an instant into the device zone).
  factory LocalDate.fromDateTime(DateTime dateTime) =>
      LocalDate(dateTime.year, dateTime.month, dateTime.day);

  /// Inverse of [epochDay] — the calendar date [days] after the Unix epoch.
  factory LocalDate.fromEpochDay(int days) => LocalDate.fromDateTime(
        DateTime.fromMillisecondsSinceEpoch(
          days * Duration.millisecondsPerDay,
          isUtc: true,
        ),
      );

  /// Start of this day as a UTC instant (used for stable day arithmetic).
  DateTime atStartOfDayUtc() => DateTime.utc(year, month, day);

  /// This date at the given wall-clock time in the device zone, as an instant.
  DateTime atTimeInstant(int hour, [int minute = 0, int second = 0]) =>
      DateTime(year, month, day, hour, minute, second).toUtc();

  /// Days since the Unix epoch (matches `LocalDate.toEpochDay`).
  int get epochDay =>
      atStartOfDayUtc().millisecondsSinceEpoch ~/ Duration.millisecondsPerDay;

  /// Day of week, Monday = 1 … Sunday = 7 (matches `DayOfWeek.value`).
  int get dayOfWeek => DateTime.utc(year, month, day).weekday;

  int get lengthOfMonth => _lengthOfMonth(year, month);

  int get lengthOfYear => _isLeapYear(year) ? 366 : 365;

  int get dayOfYear =>
      epochDay - LocalDate(year, 1, 1).epochDay + 1;

  LocalDate plusDays(int days) {
    final shifted =
        DateTime.utc(year, month, day).add(Duration(days: days));
    return LocalDate(shifted.year, shifted.month, shifted.day);
  }

  LocalDate minusDays(int days) => plusDays(-days);

  LocalDate plusWeeks(int weeks) => plusDays(weeks * 7);

  LocalDate plusMonths(int months) {
    // DateTime normalizes out-of-range months into years; day is then clamped
    // to the target month length, matching java.time semantics.
    final base = DateTime.utc(year, month + months, 1);
    final length = _lengthOfMonth(base.year, base.month);
    return LocalDate(base.year, base.month, math.min(day, length));
  }

  LocalDate plusYears(int years) {
    final targetYear = year + years;
    final length = _lengthOfMonth(targetYear, month);
    return LocalDate(targetYear, month, math.min(day, length));
  }

  LocalDate withDayOfMonth(int dayOfMonth) =>
      // Clamp to the month length so a caller can't mint a phantom date like
      // Feb 31 (consistent with plusMonths/plusYears, which also clamp).
      LocalDate(year, month, math.min(math.max(dayOfMonth, 1), lengthOfMonth));

  LocalDate withDayOfYear(int dayOfYear) {
    final shifted =
        DateTime.utc(year, 1, 1).add(Duration(days: dayOfYear - 1));
    return LocalDate(shifted.year, shifted.month, shifted.day);
  }

  /// The most recent [weekday] (Monday = 1 … Sunday = 7) on or before this date.
  LocalDate previousOrSame(int weekday) {
    final difference = (dayOfWeek - weekday + 7) % 7;
    return minusDays(difference);
  }

  bool isBefore(LocalDate other) => compareTo(other) < 0;

  bool isAfter(LocalDate other) => compareTo(other) > 0;

  LocalDate coerceAtMost(LocalDate other) => isAfter(other) ? other : this;

  LocalDate coerceAtLeast(LocalDate other) => isBefore(other) ? other : this;

  bool isBetween(LocalDate start, LocalDate end) =>
      compareTo(start) >= 0 && compareTo(end) <= 0;

  @override
  int compareTo(LocalDate other) {
    if (year != other.year) return year - other.year;
    if (month != other.month) return month - other.month;
    return day - other.day;
  }

  @override
  bool operator ==(Object other) =>
      other is LocalDate &&
      other.year == year &&
      other.month == month &&
      other.day == day;

  @override
  int get hashCode => Object.hash(year, month, day);

  @override
  String toString() =>
      '${year.toString().padLeft(4, '0')}-'
      '${month.toString().padLeft(2, '0')}-'
      '${day.toString().padLeft(2, '0')}';
}

/// A wall-clock time of day, mirroring the subset of `java.time.LocalTime` used
/// by the source (minute-of-day arithmetic).
class LocalTime {
  const LocalTime(this.hour, this.minute, [this.second = 0]);

  final int hour;
  final int minute;
  final int second;

  factory LocalTime.fromDateTime(DateTime dateTime) =>
      LocalTime(dateTime.hour, dateTime.minute, dateTime.second);

  int get minuteOfDay => hour * 60 + minute;

  @override
  bool operator ==(Object other) =>
      other is LocalTime &&
      other.hour == hour &&
      other.minute == minute &&
      other.second == second;

  @override
  int get hashCode => Object.hash(hour, minute, second);

  @override
  String toString() =>
      '${hour.toString().padLeft(2, '0')}:'
      '${minute.toString().padLeft(2, '0')}';
}

/// The device-local calendar date of an instant (`instant.atZone(zone)
/// .toLocalDate()` with the system default zone).
LocalDate instantToLocalDate(DateTime instant) =>
    LocalDate.fromDateTime(instant.toLocal());

/// The device-local wall-clock time of an instant.
LocalTime instantToLocalTime(DateTime instant) =>
    LocalTime.fromDateTime(instant.toLocal());

int _lengthOfMonth(int year, int month) {
  const lengths = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  if (month == 2 && _isLeapYear(year)) return 29;
  return lengths[month - 1];
}

bool _isLeapYear(int year) =>
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
