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

  /// The ordered upcoming fire instants in `(now, now+horizon]`, capped at
  /// [maxCount]. This is what the notification engine pre-schedules as a batch,
  /// since — unlike the old wake-and-recheck alarm — a plain scheduled
  /// notification cannot recompute at fire time. Suppressed moments are simply
  /// omitted: [goalMet] pushes the whole plan to tomorrow, and any strategy with
  /// quiet hours never emits a moment outside its window (its [nextTrigger]
  /// already snaps into the window).
  ///
  /// [anchor] is the last relevant user action (e.g. the last logged drink): the
  /// first fire is then measured from it rather than from [now], which is how the
  /// countdown resets on a drink and how "skip if you drank recently" falls out.
  /// A null anchor (or [goalMet]) uses the strategy's own baseline. Strategies
  /// with no interval (a single daily time) ignore the anchor.
  List<DateTime> plan(
    DateTime now, {
    DateTime? anchor,
    bool goalMet = false,
    // Two weeks, not two days: re-planning only happens on a foreground event, so
    // a 48h horizon silenced a low-frequency (daily) reminder after a weekend
    // away. A high-frequency schedule is still bounded by [maxCount] and its
    // device batch size, so this only extends the schedules that can benefit.
    Duration horizon = const Duration(days: 14),
    int maxCount = 64,
  }) {
    final deadline = now.add(horizon);
    final triggers = <DateTime>[];
    var next = _firstTrigger(now, anchor: anchor, goalMet: goalMet);
    while (!next.isAfter(deadline) && triggers.length < maxCount) {
      triggers.add(next);
      final following = nextTrigger(next);
      // Defensive: every strategy's nextTrigger must strictly advance, but never
      // spin if one does not.
      if (!following.isAfter(next)) break;
      next = following;
    }
    return triggers;
  }

  /// The first fire of a [plan]. With an [anchor] (and no met goal) the countdown
  /// is measured from it: `nextTrigger(anchor)` yields anchor+interval snapped
  /// into the window, then it is rolled forward past [now] for a stale anchor.
  DateTime _firstTrigger(DateTime now, {DateTime? anchor, bool goalMet = false}) {
    if (goalMet || anchor == null) return nextTrigger(now, goalMet: goalMet);
    var candidate = nextTrigger(anchor);
    while (!candidate.isAfter(now)) {
      candidate = nextTrigger(candidate);
    }
    return candidate;
  }

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
