import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/reminders/reminder_schedule.dart';
import 'package:openvitals/core/time/local_date.dart';

class _RecordingScheduler implements ReminderScheduler {
  final List<DateTime> scheduled = [];
  int cancelCount = 0;

  @override
  Future<void> schedule(DateTime triggerAt) async => scheduled.add(triggerAt);

  @override
  Future<void> cancel() async => cancelCount++;
}

class _RecordingNotifier implements ReminderNotifier {
  final List<ReminderGoalProgress> shown = [];
  int cancelCount = 0;

  @override
  Future<void> show(ReminderGoalProgress progress) async => shown.add(progress);

  @override
  Future<void> cancel() async => cancelCount++;
}

/// A notifier whose post fails, standing in for a flaky notification plugin at
/// fire time.
class _ThrowingNotifier implements ReminderNotifier {
  @override
  Future<void> show(ReminderGoalProgress progress) async =>
      throw StateError('notification post failed');

  @override
  Future<void> cancel() async {}
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
  late _RecordingNotifier notifier;

  setUp(() {
    scheduler = _RecordingScheduler();
    notifier = _RecordingNotifier();
  });

  ReminderController controller({
    bool enabled = true,
    ReminderSchedule? schedule,
    ReminderGoalProgress progress = const ReminderGoalProgress.none(),
    bool permission = true,
    int hour = 10,
  }) =>
      ReminderController(
        loadSettings: () => ReminderSettings(
          enabled: enabled,
          schedule: schedule ?? _window,
        ),
        readProgress: () async => progress,
        scheduler: scheduler,
        notifier: notifier,
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
    test('a disabled reminder clears the alarm and the notification', () async {
      await controller(enabled: false).apply();

      expect(scheduler.cancelCount, 1);
      expect(notifier.cancelCount, 1);
      expect(scheduler.scheduled, isEmpty);
    });

    test('missing notification permission clears, even when enabled', () async {
      await controller(permission: false).apply();

      expect(scheduler.cancelCount, 1);
      expect(notifier.cancelCount, 1);
      expect(scheduler.scheduled, isEmpty);
    });

    test('arms the next alarm without notifying', () async {
      await controller(
        progress: const ReminderGoalProgress(current: 1, target: 2),
      ).apply();

      expect(scheduler.scheduled, [_at(12)]);
      expect(notifier.shown, isEmpty);
    });

    test('a met goal pushes the next alarm past today', () async {
      await controller(
        progress: const ReminderGoalProgress(current: 2, target: 2),
      ).apply();

      // Tomorrow's active start (07:00) plus the interval.
      expect(scheduler.scheduled, [DateTime.utc(2026, 6, 2, 9)]);
    });

    test('explicit settings override the persisted ones', () async {
      // Loaded settings say enabled; the passed-in ones say off.
      await controller().apply(
        ReminderSettings(enabled: false, schedule: _window),
      );

      expect(scheduler.scheduled, isEmpty);
      expect(scheduler.cancelCount, 1);
    });
  });

  group('handleAlarm', () {
    test('notifies and re-arms when the goal is unmet and hours allow', () async {
      const progress = ReminderGoalProgress(current: 1, target: 2);
      await controller(progress: progress, hour: 10).handleAlarm();

      expect(notifier.shown, [progress]);
      expect(scheduler.scheduled, [_at(12)]);
    });

    test('does not notify once the goal is met, but still re-arms', () async {
      await controller(
        progress: const ReminderGoalProgress(current: 2, target: 2),
      ).handleAlarm();

      expect(notifier.shown, isEmpty);
      expect(scheduler.scheduled, hasLength(1));
    });

    test('does not notify during quiet hours, but still re-arms', () async {
      // 03:00 is outside the 07:00–23:00 window.
      await controller(
        progress: const ReminderGoalProgress(current: 1, target: 2),
        hour: 3,
      ).handleAlarm();

      expect(notifier.shown, isEmpty);
      expect(scheduler.scheduled, [_at(9)]);
    });

    test('a schedule without quiet hours notifies at any time', () async {
      const progress = ReminderGoalProgress(current: 1, target: 10);
      await controller(schedule: _daily, progress: progress, hour: 3)
          .handleAlarm();

      expect(notifier.shown, [progress]);
    });

    test('a disabled reminder clears instead of notifying', () async {
      await controller(
        enabled: false,
        progress: const ReminderGoalProgress(current: 1, target: 2),
      ).handleAlarm();

      expect(notifier.shown, isEmpty);
      expect(scheduler.cancelCount, 1);
      expect(notifier.cancelCount, 1);
    });

    // The chain is a self-perpetuating one-shot: each fire arms the next. A
    // transient failure reading progress or posting must never break that, or the
    // reminder goes silent until the app is reopened (the "reminders stopped" bug).
    test('re-arms even when reading progress throws', () async {
      final c = ReminderController(
        loadSettings: () =>
            ReminderSettings(enabled: true, schedule: _window),
        readProgress: () async => throw StateError('HC read failed'),
        scheduler: scheduler,
        notifier: notifier,
        now: () => _at(10),
        hasNotificationPermission: () async => true,
      );

      await c.handleAlarm();

      expect(notifier.shown, isEmpty);
      // goalMet defaults false on a failed read, so the next fire is now+interval
      // (retry soon) rather than rolling to tomorrow.
      expect(scheduler.scheduled, [_at(12)]);
    });

    test('re-arms even when posting the notification throws', () async {
      final c = ReminderController(
        loadSettings: () =>
            ReminderSettings(enabled: true, schedule: _window),
        readProgress: () async =>
            const ReminderGoalProgress(current: 1, target: 2),
        scheduler: scheduler,
        notifier: _ThrowingNotifier(),
        now: () => _at(10),
        hasNotificationPermission: () async => true,
      );

      await c.handleAlarm();

      expect(scheduler.scheduled, [_at(12)]);
    });
  });

  group('restoreSchedule', () {
    test('re-arms an enabled reminder', () async {
      await controller().restoreSchedule();
      expect(scheduler.scheduled, hasLength(1));
    });

    test('clears a disabled one', () async {
      await controller(enabled: false).restoreSchedule();
      expect(scheduler.scheduled, isEmpty);
      expect(scheduler.cancelCount, 1);
    });
  });

  test('hideNotification dismisses the notification but keeps the alarm armed',
      () async {
    // This is the "saving an entry hides the reminder" path — the schedule must
    // survive, or the reminder chain dies for the rest of the day.
    await controller().hideNotification();

    expect(notifier.cancelCount, 1);
    expect(scheduler.cancelCount, 0);
  });

  test('clear cancels both the alarm and the notification', () async {
    await controller().clear();

    expect(notifier.cancelCount, 1);
    expect(scheduler.cancelCount, 1);
  });
}
