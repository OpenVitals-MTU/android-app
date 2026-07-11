import '../time/local_date.dart';

/// When a reminder should next fire, and whether it may notify right now.
///
/// The two concrete strategies cover both Kotlin reminder features — hydration's
/// interval-within-a-window and mindfulness's single daily time — and a new
/// feature only has to pick one (or add a third) rather than copy a controller.
///
/// Everything here is device-independent: it operates on plain [DateTime]s (the
/// port's stand-in for `java.time.ZonedDateTime`) and preserves the reference
/// instant's zone (UTC vs local) across wall-clock arithmetic.
sealed class ReminderSchedule {
  const ReminderSchedule();

  /// The next moment the reminder should fire, relative to [now].
  ///
  /// When [goalMet] is true the user already hit today's target, so the next
  /// reminder rolls to tomorrow rather than nagging again today.
  DateTime nextTrigger(DateTime now, {bool goalMet = false});

  /// Whether a reminder that fires at [moment] may actually notify. Schedules
  /// with no quiet hours always allow it.
  bool allowsNotificationAt(DateTime moment) => true;
}

/// A single daily reminder at a fixed wall-clock time. Port of the Kotlin
/// `calculateNextMindfulnessReminderTime`.
class DailyTimeReminderSchedule extends ReminderSchedule {
  const DailyTimeReminderSchedule(this.time);

  final LocalTime time;

  @override
  DateTime nextTrigger(DateTime now, {bool goalMet = false}) {
    final todayReminder = _atTime(now, _dateOf(now), time);
    if (!goalMet && todayReminder.isAfter(now)) return todayReminder;
    return _atTime(now, _dateOf(now).plusDays(1), time);
  }
}

/// A reminder every [intervalMinutes] inside a daily active window. Port of the
/// Kotlin `calculateNextHydrationReminderTime`.
///
/// The window may wrap past midnight (end before start). A window whose start
/// equals its end is treated as always active.
class IntervalWindowReminderSchedule extends ReminderSchedule {
  const IntervalWindowReminderSchedule({
    required this.intervalMinutes,
    required this.activeStartTime,
    required this.activeEndTime,
  });

  final int intervalMinutes;
  final LocalTime activeStartTime;
  final LocalTime activeEndTime;

  Duration get _interval => Duration(minutes: intervalMinutes);

  /// Whether [time] falls inside the active window.
  bool isWithinActiveHours(LocalTime time) {
    final start = activeStartTime.minuteOfDay;
    final end = activeEndTime.minuteOfDay;
    if (start == end) return true;
    final value = time.minuteOfDay;
    if (end > start) return value >= start && value < end;
    return value >= start || value < end;
  }

  @override
  bool allowsNotificationAt(DateTime moment) =>
      isWithinActiveHours(_timeOf(moment));

  @override
  DateTime nextTrigger(DateTime now, {bool goalMet = false}) {
    if (goalMet) {
      return _atTime(now, _dateOf(now).plusDays(1), activeStartTime)
          .add(_interval);
    }
    if (!isWithinActiveHours(_timeOf(now))) {
      return _nextActiveStartAfter(now).add(_interval);
    }
    final candidate = now.add(_interval);
    if (isWithinActiveHours(_timeOf(candidate))) return candidate;
    return _nextActiveStartAfter(candidate).add(_interval);
  }

  DateTime _nextActiveStartAfter(DateTime moment) {
    final todayStart = _atTime(moment, _dateOf(moment), activeStartTime);
    if (todayStart.isAfter(moment)) return todayStart;
    return _atTime(moment, _dateOf(moment).plusDays(1), activeStartTime);
  }
}

LocalDate _dateOf(DateTime moment) =>
    LocalDate(moment.year, moment.month, moment.day);

LocalTime _timeOf(DateTime moment) => LocalTime(moment.hour, moment.minute);

/// Builds an instant at [date]/[time] in the same zone (UTC vs local) as
/// [reference], mirroring `LocalDate.atTime(...).atZone(reference.zone)`.
DateTime _atTime(DateTime reference, LocalDate date, LocalTime time) =>
    reference.isUtc
        ? DateTime.utc(date.year, date.month, date.day, time.hour, time.minute)
        : DateTime(date.year, date.month, date.day, time.hour, time.minute);
