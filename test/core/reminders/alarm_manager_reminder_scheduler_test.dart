import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/alarm_manager_reminder_scheduler.dart';
import 'package:openvitals/features/homewidgets/home_widget_alarm.dart';

class _RecordingAlarms implements AndroidAlarmManagerApi {
  final List<
      ({
        DateTime time,
        int id,
        bool exact,
        bool wakeup,
        bool allowWhileIdle,
        bool rescheduleOnReboot,
      })> armed = [];
  final List<int> cancelled = [];
  int initializeCount = 0;

  @override
  Future<bool> initialize() async {
    initializeCount++;
    return true;
  }

  @override
  Future<bool> oneShotAt(
    DateTime time,
    int id,
    void Function() callback, {
    bool exact = false,
    bool wakeup = false,
    bool allowWhileIdle = false,
    bool rescheduleOnReboot = false,
  }) async {
    armed.add((
      time: time,
      id: id,
      exact: exact,
      wakeup: wakeup,
      allowWhileIdle: allowWhileIdle,
      rescheduleOnReboot: rescheduleOnReboot,
    ));
    return true;
  }

  @override
  Future<bool> cancel(int id) async {
    cancelled.add(id);
    return true;
  }
}

@pragma('vm:entry-point')
void _callback() {}

void main() {
  late _RecordingAlarms alarms;

  setUp(() => alarms = _RecordingAlarms());

  AlarmManagerReminderScheduler scheduler({
    int alarmId = 42,
    Future<bool> Function()? canScheduleExact,
  }) =>
      AlarmManagerReminderScheduler(
        alarmId: alarmId,
        callback: _callback,
        alarms: alarms,
        canScheduleExact: canScheduleExact,
      );

  test('arms a wake-up, doze-proof alarm that survives reboot', () async {
    final when = DateTime(2026, 6, 1, 9);
    await scheduler().schedule(when);

    expect(alarms.armed, hasLength(1));
    final alarm = alarms.armed.single;
    expect(alarm.time, when);
    expect(alarm.id, 42);

    // These three are load-bearing regardless of exactness: no wakeup means the
    // reminder waits for the next unlock, Doze eats it overnight without
    // allowWhileIdle, and without rescheduleOnReboot the chain dies at restart.
    expect(alarm.wakeup, isTrue);
    expect(alarm.allowWhileIdle, isTrue);
    expect(alarm.rescheduleOnReboot, isTrue);
  });

  test('arms EXACT when the exact-alarm permission is granted', () async {
    await scheduler(canScheduleExact: () async => true)
        .schedule(DateTime(2026, 6, 1, 9));
    expect(alarms.armed.single.exact, isTrue);
  });

  test('degrades to INEXACT when the permission is not granted', () async {
    // The plugin silently DROPS an exact alarm it lacks permission for, so the
    // scheduler must downgrade itself rather than let the reminder chain die.
    await scheduler(canScheduleExact: () async => false)
        .schedule(DateTime(2026, 6, 1, 9));
    expect(alarms.armed.single.exact, isFalse);
  });

  test('arms INEXACT when no exact-alarm gate is wired', () async {
    await scheduler().schedule(DateTime(2026, 6, 1, 9));
    expect(alarms.armed.single.exact, isFalse);
  });

  test('consults the gate on every schedule, not just the first', () async {
    var granted = false;
    final s = scheduler(canScheduleExact: () async => granted);

    await s.schedule(DateTime(2026, 6, 1, 9));
    granted = true; // permission granted between fires
    await s.schedule(DateTime(2026, 6, 1, 10));

    expect(alarms.armed.map((a) => a.exact), [false, true]);
  });

  test('cancels its own alarm id', () async {
    await scheduler(alarmId: 7).cancel();
    expect(alarms.cancelled, [7]);
  });

  test('the home-widget refresh alarm is wired to a vm:entry-point callback', () {
    // This scheduler now serves only the home-widget refresh. A closure or
    // instance method could not be resolved from a raw callback handle in the
    // alarm isolate, so the callback must be this top-level one.
    expect(
      homeWidgetRefreshAlarmScheduler.callback,
      same(homeWidgetRefreshAlarmCallback),
    );
    expect(homeWidgetRefreshAlarmScheduler.alarmId, homeWidgetRefreshAlarmId);
    expect(homeWidgetRefreshAlarmId.bitLength, lessThan(32));
  });
}
