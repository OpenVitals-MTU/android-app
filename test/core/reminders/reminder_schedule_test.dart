import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/reminder_schedule.dart';
import 'package:openvitals/core/time/local_date.dart';

/// Ported from the Kotlin `HydrationReminderScheduleTest` /
/// `MindfulnessReminderScheduleTest`. Uses UTC instants so wall-clock arithmetic
/// is deterministic across time zones.
DateTime at({int day = 1, required int hour, required int minute}) =>
    DateTime.utc(2026, 6, day, hour, minute);

/// The hydration defaults: every 2h between 07:00 and 23:00.
IntervalWindowReminderSchedule window({
  int intervalMinutes = 120,
  LocalTime start = const LocalTime(7, 0),
  LocalTime end = const LocalTime(23, 0),
}) =>
    IntervalWindowReminderSchedule(
      intervalMinutes: intervalMinutes,
      activeStartTime: start,
      activeEndTime: end,
    );

void main() {
  group('IntervalWindowReminderSchedule.nextTrigger', () {
    test('inside active hours adds the interval', () {
      expect(
        window(intervalMinutes: 90).nextTrigger(at(hour: 10, minute: 0)),
        at(hour: 11, minute: 30),
      );
    });

    test('before active hours waits until active start plus the interval', () {
      expect(
        window().nextTrigger(at(hour: 5, minute: 0)),
        at(hour: 9, minute: 0),
      );
    });

    test('crossing active end moves to the next active start plus interval', () {
      expect(
        window(intervalMinutes: 60).nextTrigger(at(hour: 22, minute: 30)),
        at(day: 2, hour: 8, minute: 0),
      );
    });

    test('a met goal schedules tomorrow after active start plus interval', () {
      expect(
        window().nextTrigger(at(hour: 12, minute: 0), goalMet: true),
        at(day: 2, hour: 9, minute: 0),
      );
    });
  });

  group('IntervalWindowReminderSchedule.isWithinActiveHours', () {
    test('an overnight window includes times after midnight, before the end',
        () {
      final schedule =
          window(start: const LocalTime(7, 0), end: const LocalTime(1, 0));

      expect(schedule.isWithinActiveHours(const LocalTime(23, 0)), isTrue);
      expect(schedule.isWithinActiveHours(const LocalTime(0, 30)), isTrue);
      expect(schedule.isWithinActiveHours(const LocalTime(2, 0)), isFalse);
    });

    test('an equal start and end means always active', () {
      final schedule =
          window(start: const LocalTime(0, 0), end: const LocalTime(0, 0));
      expect(schedule.isWithinActiveHours(const LocalTime(3, 0)), isTrue);
    });

    test('allowsNotificationAt gates on the window', () {
      final schedule = window();
      expect(schedule.allowsNotificationAt(at(hour: 10, minute: 0)), isTrue);
      expect(schedule.allowsNotificationAt(at(hour: 3, minute: 0)), isFalse);
    });
  });

  group('DailyTimeReminderSchedule', () {
    const schedule = DailyTimeReminderSchedule(LocalTime(18, 0));

    test('fires later today when the time is still ahead', () {
      expect(
        schedule.nextTrigger(at(hour: 9, minute: 0)),
        at(hour: 18, minute: 0),
      );
    });

    test('rolls to tomorrow once the time has passed', () {
      expect(
        schedule.nextTrigger(at(hour: 19, minute: 0)),
        at(day: 2, hour: 18, minute: 0),
      );
    });

    test('rolls to tomorrow when the goal is already met', () {
      expect(
        schedule.nextTrigger(at(hour: 9, minute: 0), goalMet: true),
        at(day: 2, hour: 18, minute: 0),
      );
    });

    test('has no quiet hours — it may always notify', () {
      expect(schedule.allowsNotificationAt(at(hour: 3, minute: 0)), isTrue);
    });
  });

  group('IntervalWindowReminderSchedule.plan', () {
    test('lists the upcoming fires at the interval cadence', () {
      final triggers = window().plan(at(hour: 10, minute: 0));
      expect(triggers.first, at(hour: 12, minute: 0));
      expect(triggers[1], at(hour: 14, minute: 0));
      expect(triggers.length, greaterThan(1));
    });

    test('is strictly ascending and never leaves the window', () {
      final triggers = window().plan(at(hour: 10, minute: 0));
      for (var i = 1; i < triggers.length; i++) {
        expect(triggers[i].isAfter(triggers[i - 1]), isTrue);
      }
      for (final trigger in triggers) {
        expect(window().allowsNotificationAt(trigger), isTrue);
      }
    });

    test('snaps across the window end to the next active start', () {
      // 21:00 + 2h = 23:00 is outside 07:00–23:00 (end exclusive), so the first
      // fire is the next day's active start plus the interval.
      expect(
        window().plan(at(hour: 21, minute: 0)).first,
        at(day: 2, hour: 9, minute: 0),
      );
    });

    test('anchors the first fire to the anchor plus the interval', () {
      final triggers = window().plan(
        at(hour: 10, minute: 0),
        anchor: at(hour: 9, minute: 0),
      );
      expect(triggers.first, at(hour: 11, minute: 0));
    });

    test('rolls a stale anchor forward past now', () {
      final triggers = window().plan(
        at(hour: 10, minute: 0),
        anchor: at(hour: 3, minute: 0),
      );
      expect(triggers.first.isAfter(at(hour: 10, minute: 0)), isTrue);
      expect(window().allowsNotificationAt(triggers.first), isTrue);
    });

    test('with no anchor the first fire matches nextTrigger', () {
      final now = at(hour: 10, minute: 0);
      expect(window().plan(now).first, window().nextTrigger(now));
    });

    test('a met goal lists only tomorrow onward', () {
      final triggers = window().plan(at(hour: 12, minute: 0), goalMet: true);
      expect(triggers.first, at(day: 2, hour: 9, minute: 0));
      for (final trigger in triggers) {
        expect(trigger.isAfter(DateTime.utc(2026, 6, 2)), isTrue);
      }
    });

    test('respects maxCount and horizon bounds', () {
      expect(
        window().plan(at(hour: 10, minute: 0), maxCount: 3),
        hasLength(3),
      );
      // 12:00 and 14:00 fall within 5h of 10:00; 16:00 does not.
      expect(
        window().plan(
          at(hour: 10, minute: 0),
          horizon: const Duration(hours: 5),
        ),
        [at(hour: 12, minute: 0), at(hour: 14, minute: 0)],
      );
    });
  });

  group('DailyTimeReminderSchedule.plan', () {
    const schedule = DailyTimeReminderSchedule(LocalTime(18, 0));

    test('lists today then the following days at the same time', () {
      final triggers = schedule.plan(at(hour: 9, minute: 0));
      expect(triggers.first, at(hour: 18, minute: 0));
      expect(triggers[1], at(day: 2, hour: 18, minute: 0));
    });

    test('a met goal starts tomorrow', () {
      expect(
        schedule.plan(at(hour: 9, minute: 0), goalMet: true).first,
        at(day: 2, hour: 18, minute: 0),
      );
    });

    test('the default horizon pre-schedules about two weeks of daily reminders',
        () {
      // H13: the old 48h default left only ~2 daily reminders, so a long weekend
      // away silenced the feature until the app was next opened.
      final triggers = schedule.plan(at(hour: 9, minute: 0));
      expect(triggers.length, greaterThanOrEqualTo(14));
    });
  });

  test('schedules preserve the reference instant’s zone', () {
    // A local-zone `now` must not silently produce a UTC trigger.
    final local = DateTime(2026, 6, 1, 10);
    expect(window().nextTrigger(local).isUtc, isFalse);
    expect(window().nextTrigger(at(hour: 10, minute: 0)).isUtc, isTrue);
    // The whole plan preserves the zone too.
    expect(window().plan(local).every((trigger) => !trigger.isUtc), isTrue);
  });
}
