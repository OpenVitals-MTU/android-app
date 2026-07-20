import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/reminders/reminder_schedule.dart';
import 'package:openvitals/core/time/local_date.dart';

class _RecordingScheduler implements ReminderScheduler {
  final List<List<DateTime>> batches = [];
  final List<ReminderGoalProgress> progresses = [];
  int cancelCount = 0;

  List<DateTime> get lastBatch => batches.last;

  @override
  Future<void> scheduleAll(
    List<DateTime> triggers,
    ReminderGoalProgress progress,
  ) async {
    batches.add(triggers);
    progresses.add(progress);
  }

  @override
  Future<void> cancel() async => cancelCount++;
}

/// 07:00–23:00, every two hours — the hydration defaults.
final _window = IntervalWindowReminderSchedule(
  intervalMinutes: 120,
  activeStartTime: const LocalTime(7, 0),
  activeEndTime: const LocalTime(23, 0),
);

/// A schedule with no quiet hours — the mindfulness shape.
const _daily = DailyTimeReminderSchedule(LocalTime(18, 0));

DateTime _at(int hour) => DateTime.utc(2026, 6, 1, hour);

void main() {
  late _RecordingScheduler scheduler;

  setUp(() {
    scheduler = _RecordingScheduler();
  });

  ReminderController controller({
    bool enabled = true,
    ReminderSchedule? schedule,
    ReminderGoalProgress progress = const ReminderGoalProgress.none(),
    bool permission = true,
    int hour = 10,
    Future<DateTime?> Function()? loadAnchor,
  }) =>
      ReminderController(
        loadSettings: () => ReminderSettings(
          enabled: enabled,
          schedule: schedule ?? _window,
        ),
        readProgress: () async => progress,
        loadAnchor: loadAnchor,
        scheduler: scheduler,
        now: () => _at(hour),
        hasNotificationPermission: () async => permission,
      );

  group('goal progress', () {
    test('is met only at or above a positive target', () {
      expect(
        const ReminderGoalProgress(current: 1.9, target: 2.0).isMet,
        isFalse,
      );
      expect(const ReminderGoalProgress(current: 2.0, target: 2.0).isMet, isTrue);
      expect(const ReminderGoalProgress(current: 3.0, target: 2.0).isMet, isTrue);
    });

    test('a zero or absent target is never met, however much is logged', () {
      expect(const ReminderGoalProgress(current: 99, target: 0).isMet, isFalse);
      expect(const ReminderGoalProgress.none().isMet, isFalse);
    });
  });

  group('apply', () {
    test('a disabled reminder clears and schedules nothing', () async {
      await controller(enabled: false).apply();

      expect(scheduler.cancelCount, 1);
      expect(scheduler.batches, isEmpty);
    });

    test('missing notification permission clears, even when enabled', () async {
      await controller(permission: false).apply();

      expect(scheduler.cancelCount, 1);
      expect(scheduler.batches, isEmpty);
    });

    test('schedules a batch whose first fire is the next interval', () async {
      await controller(
        progress: const ReminderGoalProgress(current: 1, target: 2),
      ).apply();

      expect(scheduler.lastBatch.first, _at(12));
    });

    test('every scheduled time falls inside the active window', () async {
      await controller(
        progress: const ReminderGoalProgress(current: 1, target: 2),
      ).apply();

      expect(scheduler.lastBatch, isNotEmpty);
      for (final trigger in scheduler.lastBatch) {
        expect(_window.allowsNotificationAt(trigger), isTrue,
            reason: '$trigger is outside the active window');
      }
    });

    test('a met goal pushes the whole batch past today', () async {
      await controller(
        progress: const ReminderGoalProgress(current: 2, target: 2),
      ).apply();

      // Tomorrow's active start (07:00) plus the interval, and nothing today.
      expect(scheduler.lastBatch.first, DateTime.utc(2026, 6, 2, 9));
      for (final trigger in scheduler.lastBatch) {
        expect(trigger.isAfter(DateTime.utc(2026, 6, 2)), isTrue);
      }
    });

    test("passes today's progress to the scheduler for the notification", () async {
      const progress = ReminderGoalProgress(current: 1, target: 2);
      await controller(progress: progress).apply();

      expect(scheduler.progresses.single, progress);
    });

    test('anchors the first fire to the last logged time', () async {
      // Last drink at 09:00, now 10:00 → first reminder is 09:00 + 2h = 11:00,
      // not now + 2h = 12:00.
      await controller(
        progress: const ReminderGoalProgress(current: 1, target: 2),
        loadAnchor: () async => _at(9),
      ).apply();

      expect(scheduler.lastBatch.first, _at(11));
    });

    test('a daily schedule notifies at any time (no quiet hours)', () async {
      await controller(
        schedule: _daily,
        progress: const ReminderGoalProgress(current: 1, target: 10),
        hour: 3,
      ).apply();

      // 18:00 today is still ahead of 03:00.
      expect(scheduler.lastBatch.first, DateTime.utc(2026, 6, 1, 18));
    });

    test('explicit settings override the persisted ones', () async {
      // Loaded settings say enabled; the passed-in ones say off.
      await controller().apply(
        ReminderSettings(enabled: false, schedule: _window),
      );

      expect(scheduler.batches, isEmpty);
      expect(scheduler.cancelCount, 1);
    });
  });

  group('restoreSchedule', () {
    test('re-plans an enabled reminder', () async {
      await controller().restoreSchedule();
      expect(scheduler.batches, hasLength(1));
      expect(scheduler.lastBatch, isNotEmpty);
    });

    test('clears a disabled one', () async {
      await controller(enabled: false).restoreSchedule();
      expect(scheduler.batches, isEmpty);
      expect(scheduler.cancelCount, 1);
    });
  });

  test('clear cancels the batch', () async {
    await controller().clear();
    expect(scheduler.cancelCount, 1);
  });
}
