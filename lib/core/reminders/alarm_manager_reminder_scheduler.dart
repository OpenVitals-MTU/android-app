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
    this.canScheduleExact,
  });

  /// Must be unique per reminder feature and fit in 32 bits (plugin assertion).
  final int alarmId;
  final void Function() callback;
  final AndroidAlarmManagerApi alarms;

  /// Resolves whether an EXACT alarm may be armed right now — i.e. whether
  /// `SCHEDULE_EXACT_ALARM` is granted (Android 12+) or implicit (below 12).
  ///
  /// Consulted on EVERY [schedule], not cached, because the permission can be
  /// revoked between fires. When it is null or answers false the alarm is armed
  /// INEXACT — never dropped. This gate is mandatory: `android_alarm_manager_plus`
  /// silently drops an exact alarm it lacks permission for (it logs and schedules
  /// nothing, with no fallback of its own), so an ungated `exact: true` would kill
  /// the self-perpetuating reminder chain the instant the permission is absent.
  final Future<bool> Function()? canScheduleExact;

  /// Exact when the permission allows it, INEXACT otherwise.
  ///
  /// Exact reminders use `SCHEDULE_EXACT_ALARM` ONLY — the user-grantable,
  /// broadly-eligible permission. Never `USE_EXACT_ALARM`, which is RESTRICTED to
  /// alarm-clock and calendar apps; declaring it on a health dashboard risks the
  /// app being rejected or pulled from Play. When the permission is not granted
  /// (denied by default on Android 14+) the alarm degrades to an inexact,
  /// Doze-surviving alarm that lands inside a window rather than at the instant.
  @override
  Future<void> schedule(DateTime triggerAt) async {
    final exact = canScheduleExact != null && await canScheduleExact!();
    await alarms.oneShotAt(
      triggerAt,
      alarmId,
      callback,
      exact: exact,
      wakeup: true,
      // Fire even in Doze, or an overnight reminder silently slips.
      allowWhileIdle: true,
      rescheduleOnReboot: true,
    );
  }

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
