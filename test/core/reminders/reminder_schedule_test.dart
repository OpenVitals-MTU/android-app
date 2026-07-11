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

  test('schedules preserve the reference instant’s zone', () {
    // A local-zone `now` must not silently produce a UTC trigger.
    final local = DateTime(2026, 6, 1, 10);
    expect(window().nextTrigger(local).isUtc, isFalse);
    expect(window().nextTrigger(at(hour: 10, minute: 0)).isUtc, isTrue);
  });
}
