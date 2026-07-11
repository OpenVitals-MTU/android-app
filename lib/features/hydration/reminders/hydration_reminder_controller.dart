import '../../../core/reminders/reminder_controller.dart';
import '../../../core/reminders/reminder_schedule.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/contract/hydration_repository.dart';
import '../../../domain/model/hydration_reminder_config.dart';

/// The hydration reminder, expressed against the generic [ReminderController].
///
/// This class owns only what is hydration-specific: the persisted
/// [HydrationReminderConfig], the interval-in-a-window schedule it describes,
/// and today's intake read from the [HydrationRepository]. Everything else —
/// the enabled/permission gate, the fire-then-rearm chain, the goal-met pause —
/// lives in the shared controller.
class HydrationReminderController {
  HydrationReminderController({
    required this.preferences,
    required this.hydrationRepository,
    required ReminderNotifier notifier,
    required ReminderScheduler scheduler,
    DateTime Function() now = DateTime.now,
    Future<bool> Function()? hasNotificationPermission,
  }) : reminders = ReminderController(
          loadSettings: () => _settingsFor(preferences.hydrationReminderConfig()),
          readProgress: () => _readProgress(preferences, hydrationRepository),
          scheduler: scheduler,
          notifier: notifier,
          now: now,
          hasNotificationPermission:
              hasNotificationPermission ?? _alwaysGranted,
        );

  final PreferencesRepository preferences;
  final HydrationRepository hydrationRepository;
  final ReminderController reminders;

  HydrationReminderConfig config() => preferences.hydrationReminderConfig();

  /// Persists [config] and (re)applies the schedule.
  Future<void> updateConfig(HydrationReminderConfig config) async {
    final normalized = config.normalized();
    preferences.setHydrationReminderConfig(normalized);
    await applyConfig(normalized);
  }

  /// (Re)arms from [config], or from the persisted config when omitted.
  Future<void> applyConfig([HydrationReminderConfig? config]) =>
      reminders.apply(config == null ? null : _settingsFor(config));

  Future<void> handleReminderAlarm() => reminders.handleAlarm();

  Future<void> restoreSchedule() => reminders.restoreSchedule();

  /// Dismisses a visible reminder — the Kotlin behaviour where saving a
  /// hydration entry hides the notification that prompted it.
  Future<void> hideReminderNotification() => reminders.hideNotification();

  static ReminderSettings _settingsFor(HydrationReminderConfig config) {
    final normalized = config.normalized();
    return ReminderSettings(
      enabled: normalized.enabled,
      schedule: IntervalWindowReminderSchedule(
        intervalMinutes: normalized.intervalMinutes,
        activeStartTime: normalized.activeStartTime,
        activeEndTime: normalized.activeEndTime,
      ),
    );
  }

  /// Today's litres against the daily goal. A read failure counts as zero
  /// intake, not as a met goal (Kotlin `runCatching { … }.getOrDefault(0.0)`).
  static Future<ReminderGoalProgress> _readProgress(
    PreferencesRepository preferences,
    HydrationRepository repository,
  ) async {
    final target = preferences.hydrationDailyGoalLiters;
    final today = LocalDate.now();
    try {
      final daily = await repository.loadDailyHydration(today, today);
      final current = daily.fold<double>(0.0, (sum, day) => sum + day.liters);
      return ReminderGoalProgress(current: current, target: target);
    } catch (_) {
      return ReminderGoalProgress(current: 0.0, target: target);
    }
  }
}

Future<bool> _alwaysGranted() async => true;
