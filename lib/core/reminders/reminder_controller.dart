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

/// (Re)schedules the batch of upcoming reminder notifications, replacing whatever
/// was scheduled before. Device-specific; injected so the controller stays
/// testable.
///
/// [scheduleAll] takes the whole upcoming plan (see [ReminderSchedule.plan]) and
/// must cancel the previous batch before scheduling the new one, or a shrunken
/// plan leaves stale notifications from a longer previous plan firing later. An
/// empty list clears everything. [cancel] clears the batch (pending and any
/// already-shown notification in the reserved id range).
abstract interface class ReminderScheduler {
  /// [progress] is today's value against today's goal, so the scheduler can bake
  /// it into the notification (a "1.3 L / 2.0 L" body and a progress bar). It is
  /// the progress as of scheduling — accurate for same-day reminders, which is
  /// why the scheduler only shows it on those.
  Future<void> scheduleAll(List<DateTime> triggers, ReminderGoalProgress progress);

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
///
/// The engine pre-schedules a rolling batch of notifications rather than waking
/// the app to recompute at fire time, so there is no fire callback here: every
/// path funnels through [apply], which recomputes the plan and re-schedules the
/// whole batch. [apply] is re-run on every foreground opportunity (app start,
/// resume, a relevant log, a config change) so the plan tracks reality.
class ReminderController {
  ReminderController({
    required this.loadSettings,
    required this.readProgress,
    required this.scheduler,
    this.loadAnchor,
    this.now = DateTime.now,
    this.hasNotificationPermission = _permissionGranted,
  });

  final ReminderSettings Function() loadSettings;
  final Future<ReminderGoalProgress> Function() readProgress;
  final ReminderScheduler scheduler;

  /// The last relevant user action to measure the countdown from — e.g. the
  /// timestamp of the last logged drink. Null (the default) means the schedule's
  /// own baseline is used; a feature with no such anchor (a fixed daily time)
  /// leaves it null. Must not throw: return null on a read failure.
  final Future<DateTime?> Function()? loadAnchor;

  final DateTime Function() now;

  /// Asynchronous because the Android 13+ POST_NOTIFICATIONS check is a platform
  /// call; caching it would go stale when the user revokes the permission.
  final Future<bool> Function() hasNotificationPermission;

  /// Serializes every operation that touches the reserved notification-id range.
  /// A cold-start apply (fired unawaited from bootstrap) and a resume re-plan
  /// could otherwise interleave one run's cancel-range with the other's
  /// schedule-batch, dropping arbitrary entries from the surviving batch.
  Future<void> _queue = Future<void>.value();

  Future<void> _serialize(Future<void> Function() op) {
    final result = _queue.then((_) => op());
    _queue = result.then((_) {}, onError: (_) {});
    return result;
  }

  /// Recomputes the upcoming reminder plan and (re)schedules the whole batch, or
  /// clears everything when the reminder is off or notifications are not
  /// permitted.
  ///
  /// Pass [settings] to apply a config that has not been persisted yet;
  /// otherwise the current settings are read.
  Future<void> apply([ReminderSettings? settings]) =>
      _serialize(() => _applyOnce(settings));

  Future<void> _applyOnce([ReminderSettings? settings]) async {
    final resolved = settings ?? loadSettings();
    if (!resolved.enabled || !await hasNotificationPermission()) {
      // The unserialized clear — we already hold the queue.
      await _clearOnce();
      return;
    }
    final progress = await readProgress();
    final anchor = loadAnchor == null ? null : await loadAnchor!();
    final triggers = resolved.schedule
        .plan(now(), anchor: anchor, goalMet: progress.isMet)
        // Belt-and-suspenders: plan() already omits out-of-window moments, but
        // filtering here keeps quiet hours enforced even if a strategy's plan
        // ever changes.
        .where(resolved.schedule.allowsNotificationAt)
        .toList();
    await scheduler.scheduleAll(triggers, progress);
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

  /// Cancels the whole batch (pending notifications and any already-shown one).
  Future<void> clear() => _serialize(_clearOnce);

  Future<void> _clearOnce() => scheduler.cancel();
}

/// Default permission gate. On-device the Android 13+ POST_NOTIFICATIONS check
/// is injected; the pure default assumes granted so tests and non-Android
/// targets behave predictably.
Future<bool> _permissionGranted() async => true;
