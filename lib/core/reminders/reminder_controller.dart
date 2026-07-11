import 'reminder_schedule.dart';

/// Today's progress toward a reminder's daily goal. A reminder stops nagging
/// once [isMet]; a zero or negative [target] means "no goal", never met.
class ReminderGoalProgress {
  const ReminderGoalProgress({required this.current, required this.target});

  const ReminderGoalProgress.none() : current = 0, target = 0;

  /// In the feature's own unit (litres, minutes, …).
  final double current;
  final double target;

  bool get isMet => target > 0.0 && current >= target;

  @override
  bool operator ==(Object other) =>
      other is ReminderGoalProgress &&
      other.current == current &&
      other.target == target;

  @override
  int get hashCode => Object.hash(current, target);

  @override
  String toString() => 'ReminderGoalProgress($current/$target)';
}

/// Arms or cancels the OS-level alarm that fires the next reminder.
/// Device-specific; injected so the controller stays testable.
abstract interface class ReminderScheduler {
  Future<void> schedule(DateTime triggerAt);

  Future<void> cancel();
}

/// Posts or clears a reminder notification. Device-specific.
abstract interface class ReminderNotifier {
  Future<void> show(ReminderGoalProgress progress);

  Future<void> cancel();
}

/// A reminder's current state, read fresh on every decision so a config change
/// or a goal edit takes effect without re-wiring the controller.
class ReminderSettings {
  const ReminderSettings({required this.enabled, required this.schedule});

  final bool enabled;
  final ReminderSchedule schedule;
}

/// Feature-agnostic reminder orchestration, generalized from the Kotlin
/// `HydrationReminderController` / `MindfulnessReminderController`, which were
/// line-for-line duplicates apart from three things — now the three seams below.
///
/// A feature supplies:
/// - [loadSettings]: whether the reminder is on, and *when* it fires
///   ([ReminderSchedule]);
/// - [readProgress]: today's value against today's goal. It must not throw;
///   return [ReminderGoalProgress.none] (or a zero `current`) on a read failure,
///   as Kotlin's `runCatching { … }.getOrDefault(0.0)` does;
/// - [scheduler] / [notifier]: the device seams.
///
/// Whether a fired reminder may actually notify is the schedule's business
/// ([ReminderSchedule.allowsNotificationAt]) — that is what made hydration
/// (quiet outside its window) differ from mindfulness (always allowed).
class ReminderController {
  ReminderController({
    required this.loadSettings,
    required this.readProgress,
    required this.scheduler,
    required this.notifier,
    this.now = DateTime.now,
    this.hasNotificationPermission = _permissionGranted,
  });

  final ReminderSettings Function() loadSettings;
  final Future<ReminderGoalProgress> Function() readProgress;
  final ReminderScheduler scheduler;
  final ReminderNotifier notifier;
  final DateTime Function() now;

  /// Asynchronous because the Android 13+ POST_NOTIFICATIONS check is a platform
  /// call; caching it would go stale when the user revokes the permission.
  final Future<bool> Function() hasNotificationPermission;

  /// (Re)computes the next reminder and arms the scheduler, or clears everything
  /// when the reminder is off or notifications are not permitted.
  ///
  /// Pass [settings] to apply a config that has not been persisted yet;
  /// otherwise the current settings are read.
  Future<void> apply([ReminderSettings? settings]) async {
    final resolved = settings ?? loadSettings();
    if (!resolved.enabled || !await hasNotificationPermission()) {
      await clear();
      return;
    }
    final progress = await readProgress();
    await _scheduleNext(resolved.schedule, goalMet: progress.isMet);
  }

  /// Runs when an alarm fires: notifies unless the goal is already met or the
  /// schedule is in quiet hours, then arms the next alarm either way.
  Future<void> handleAlarm() async {
    final settings = loadSettings();
    if (!settings.enabled || !await hasNotificationPermission()) {
      await clear();
      return;
    }
    final progress = await readProgress();
    if (!progress.isMet && settings.schedule.allowsNotificationAt(now())) {
      await notifier.show(progress);
    }
    // Armed even while quiet or goal-met, so the chain survives to tomorrow.
    await _scheduleNext(settings.schedule, goalMet: progress.isMet);
  }

  /// Re-arms (or clears) the schedule, e.g. after a device reboot or app update.
  Future<void> restoreSchedule() async {
    final settings = loadSettings();
    if (settings.enabled) {
      await apply(settings);
    } else {
      await clear();
    }
  }

  /// Dismisses a reminder that is on screen, without touching the schedule —
  /// e.g. once the user logs the entry the reminder was nagging about.
  Future<void> hideNotification() => notifier.cancel();

  /// Cancels the pending alarm *and* any visible notification.
  Future<void> clear() async {
    await scheduler.cancel();
    await notifier.cancel();
  }

  Future<void> _scheduleNext(
    ReminderSchedule schedule, {
    required bool goalMet,
  }) =>
      scheduler.schedule(schedule.nextTrigger(now(), goalMet: goalMet));
}

/// Default permission gate. On-device the Android 13+ POST_NOTIFICATIONS check
/// is injected; the pure default assumes granted so tests and non-Android
/// targets behave predictably.
Future<bool> _permissionGranted() async => true;
