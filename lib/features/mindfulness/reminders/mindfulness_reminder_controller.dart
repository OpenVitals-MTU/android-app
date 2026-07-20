import '../../../core/reminders/reminder_controller.dart';
import '../../../core/reminders/reminder_schedule.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/contract/mindfulness_repository.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/model/mindfulness_reminder_config.dart';

/// The daily mindfulness reminder, expressed against the generic
/// [ReminderController]. Only the config, its single-daily-time schedule and
/// today's mindful minutes are mindfulness-specific.
class MindfulnessReminderController {
  MindfulnessReminderController({
    required this.preferences,
    required this.mindfulnessRepository,
    required ReminderScheduler scheduler,
    DateTime Function() now = DateTime.now,
    Future<bool> Function()? hasNotificationPermission,
  }) : reminders = ReminderController(
          loadSettings: () =>
              _settingsFor(preferences.mindfulnessReminderConfig()),
          readProgress: () => _readProgress(preferences, mindfulnessRepository),
          // A fixed daily time, so there is no last-action anchor to reset from.
          scheduler: scheduler,
          now: now,
          hasNotificationPermission:
              hasNotificationPermission ?? _alwaysGranted,
        );

  final PreferencesRepository preferences;
  final MindfulnessRepository mindfulnessRepository;
  final ReminderController reminders;

  static const double _millisPerMinute = 60000.0;

  MindfulnessReminderConfig config() => preferences.mindfulnessReminderConfig();

  Future<void> updateConfig(MindfulnessReminderConfig config) async {
    final normalized = config.normalized();
    preferences.setMindfulnessReminderConfig(normalized);
    await applyConfig(normalized);
  }

  Future<void> applyConfig([MindfulnessReminderConfig? config]) =>
      reminders.apply(config == null ? null : _settingsFor(config));

  Future<void> restoreSchedule() => reminders.restoreSchedule();

  static ReminderSettings _settingsFor(MindfulnessReminderConfig config) {
    final normalized = config.normalized();
    return ReminderSettings(
      enabled: normalized.enabled,
      // A single daily time, so there are no quiet hours to respect — this is
      // exactly where the Kotlin controllers diverged.
      schedule: DailyTimeReminderSchedule(normalized.reminderTime),
    );
  }

  /// Today's mindful minutes against the daily goal. A read failure counts as
  /// zero minutes, not as a met goal.
  static Future<ReminderGoalProgress> _readProgress(
    PreferencesRepository preferences,
    MindfulnessRepository repository,
  ) async {
    final target =
        preferences.dailyGoalFor(MetricDailyGoalKey.mindfulnessMinutes);
    final today = LocalDate.now();
    try {
      final sessions =
          (await repository.loadMindfulnessSessions(today, today)).orThrow();
      final totalMs = sessions.fold<int>(
        0,
        (sum, session) =>
            sum + (session.durationMs < 0 ? 0 : session.durationMs),
      );
      return ReminderGoalProgress(
        current: totalMs / _millisPerMinute,
        target: target,
      );
    } catch (_) {
      return ReminderGoalProgress(current: 0.0, target: target);
    }
  }
}

Future<bool> _alwaysGranted() async => true;
