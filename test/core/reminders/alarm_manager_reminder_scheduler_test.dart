import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/alarm_manager_reminder_scheduler.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_alarm.dart';

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

  AlarmManagerReminderScheduler scheduler({int alarmId = 42}) =>
      AlarmManagerReminderScheduler(
        alarmId: alarmId,
        callback: _callback,
        alarms: alarms,
      );

  test('arms an INEXACT, wake-up, doze-proof alarm that survives reboot',
      () async {
    final when = DateTime(2026, 6, 1, 9);
    await scheduler().schedule(when);

    expect(alarms.armed, hasLength(1));
    final alarm = alarms.armed.single;
    expect(alarm.time, when);
    expect(alarm.id, 42);

    // `exact` must stay FALSE, and this is a Play Store constraint, not a
    // preference. An exact alarm needs SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM, and
    // USE_EXACT_ALARM is restricted to alarm-clock and calendar apps -- declaring
    // it on a health dashboard risks the app being pulled. Flipping this back to
    // true without also removing USE_EXACT_ALARM from the manifest and completing
    // the Play declaration is how the app gets rejected.
    expect(
      alarm.exact,
      isFalse,
      reason: 'exact alarms require a Play declaration; see the manifest comment',
    );

    // The other three are still load-bearing: no wakeup means the reminder waits
    // for the next unlock, Doze eats it overnight without allowWhileIdle, and
    // without rescheduleOnReboot the chain dies at the next restart.
    expect(alarm.wakeup, isTrue);
    expect(alarm.allowWhileIdle, isTrue);
    expect(alarm.rescheduleOnReboot, isTrue);
  });

  test('cancels its own alarm id', () async {
    await scheduler(alarmId: 7).cancel();
    expect(alarms.cancelled, [7]);
  });

  test('the hydration alarm is wired to a vm:entry-point callback', () {
    // A closure or instance method could not be resolved from a raw callback
    // handle in the alarm isolate, so the callback must be this top-level one.
    expect(
      hydrationReminderAlarmScheduler.callback,
      same(hydrationReminderAlarmCallback),
    );
    expect(hydrationReminderAlarmScheduler.alarmId, hydrationReminderAlarmId);
    expect(hydrationReminderAlarmId.bitLength, lessThan(32));
  });
}
