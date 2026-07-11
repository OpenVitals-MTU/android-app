import 'package:android_alarm_manager_plus/android_alarm_manager_plus.dart';

import 'reminder_controller.dart';

/// Arms an exact Android alarm that wakes the app at the trigger time, mirroring
/// the Kotlin `HydrationReminderAlarmManager`.
///
/// This is the faithful model: the alarm wakes a background isolate, which
/// re-reads today's progress and *then* decides whether to notify. A
/// pre-scheduled notification cannot do that — its body is fixed at schedule
/// time, and it fires even after the user has already hit the goal.
///
/// [callback] must be a top-level or static function annotated with
/// `@pragma('vm:entry-point')`: the plugin resolves it to a raw callback handle
/// and invokes it in a fresh isolate, so a closure would not survive.
///
/// `rescheduleOnReboot` makes Android persist the alarm across a reboot, which
/// is what the Kotlin `HydrationReminderBootReceiver` did by hand. The alarm
/// re-fires after boot, the callback recomputes, and the chain continues.
class AlarmManagerReminderScheduler implements ReminderScheduler {
  const AlarmManagerReminderScheduler({
    required this.alarmId,
    required this.callback,
    this.alarms = const AndroidAlarmManagerApi(),
  });

  /// Must be unique per reminder feature and fit in 32 bits (plugin assertion).
  final int alarmId;
  final void Function() callback;
  final AndroidAlarmManagerApi alarms;

  /// DELIBERATE DEVIATION from the Kotlin app — do not "fix" this back.
  ///
  /// Kotlin arms *inexact* alarms (`setAndAllowWhileIdle`) and declares no
  /// exact-alarm permission. Flutter arms **exact** alarms (and so declares
  /// `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`), which fires reminders on time
  /// instead of whenever Doze next relents. The trade is a Play Store
  /// exact-alarm justification that Kotlin never had; that was accepted
  /// knowingly. A parity audit will flag this — it is intentional.
  @override
  Future<void> schedule(DateTime triggerAt) => alarms.oneShotAt(
        triggerAt,
        alarmId,
        callback,
        exact: true,
        wakeup: true,
        // Fire even in Doze, or an overnight reminder silently slips.
        allowWhileIdle: true,
        rescheduleOnReboot: true,
      );

  @override
  Future<void> cancel() => alarms.cancel(alarmId);
}

/// Seam over the plugin's static API so [AlarmManagerReminderScheduler] can be
/// unit-tested without an Android host.
class AndroidAlarmManagerApi {
  const AndroidAlarmManagerApi();

  Future<bool> initialize() => AndroidAlarmManager.initialize();

  Future<bool> oneShotAt(
    DateTime time,
    int id,
    void Function() callback, {
    bool exact = false,
    bool wakeup = false,
    bool allowWhileIdle = false,
    bool rescheduleOnReboot = false,
  }) =>
      AndroidAlarmManager.oneShotAt(
        time,
        id,
        callback,
        exact: exact,
        wakeup: wakeup,
        allowWhileIdle: allowWhileIdle,
        rescheduleOnReboot: rescheduleOnReboot,
      );

  Future<bool> cancel(int id) => AndroidAlarmManager.cancel(id);
}
