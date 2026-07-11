import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/domain/insights/daily_goals.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/model/mindfulness_reminder_config.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Ported from the Kotlin `MindfulnessReminderControllerTest`.
class _FakeMindfulnessRepository implements MindfulnessRepository {
  _FakeMindfulnessRepository(this.minutesToday);

  final int minutesToday;

  @override
  Future<List<MindfulnessSession>> loadMindfulnessSessions(
    LocalDate start,
    LocalDate end,
  ) async {
    final now = DateTime.now();
    return [
      MindfulnessSession(
        id: '$minutesToday',
        title: null,
        startTime: now.subtract(Duration(minutes: minutesToday)),
        endTime: now,
        durationMs: minutesToday * 60000,
        source: 'test',
      ),
    ];
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      super.noSuchMethod(invocation);
}

class _RecordingScheduler implements ReminderScheduler {
  int scheduleCount = 0;
  int cancelCount = 0;

  @override
  Future<void> schedule(DateTime triggerAt) async => scheduleCount++;

  @override
  Future<void> cancel() async => cancelCount++;
}

class _RecordingNotifier implements ReminderNotifier {
  final List<(double, double)> shown = [];
  int cancelCount = 0;

  @override
  Future<void> show(ReminderGoalProgress progress) async =>
      shown.add((progress.current, progress.target));

  @override
  Future<void> cancel() async => cancelCount++;
}

Future<PreferencesRepository> newPrefs([
  Map<String, Object> initial = const {},
]) async {
  SharedPreferences.setMockInitialValues(initial);
  return PreferencesRepository(await SharedPreferences.getInstance());
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _RecordingScheduler scheduler;
  late _RecordingNotifier notifier;

  setUp(() {
    scheduler = _RecordingScheduler();
    notifier = _RecordingNotifier();
  });

  MindfulnessReminderController controller(
    PreferencesRepository prefs, {
    int minutesToday = 0,
  }) =>
      MindfulnessReminderController(
        preferences: prefs,
        mindfulnessRepository: _FakeMindfulnessRepository(minutesToday),
        notifier: notifier,
        scheduler: scheduler,
      );

  test('disabled config clears alarm and notification', () async {
    final prefs = await newPrefs();

    await controller(prefs).applyConfig(
      const MindfulnessReminderConfig(enabled: false),
    );

    expect(scheduler.cancelCount, 1);
    expect(notifier.cancelCount, 1);
    expect(scheduler.scheduleCount, 0);
  });

  test('enabled config schedules next reminder without notifying', () async {
    final prefs = await newPrefs();
    prefs.setDailyGoalFor(MetricDailyGoalKey.mindfulnessMinutes, 10.0);

    await controller(prefs, minutesToday: 5).applyConfig(
      const MindfulnessReminderConfig(enabled: true),
    );

    expect(scheduler.scheduleCount, 1);
    expect(notifier.shown, isEmpty);
  });

  test('alarm shows notification when goal is not met', () async {
    final prefs = await newPrefs();
    prefs.setDailyGoalFor(MetricDailyGoalKey.mindfulnessMinutes, 10.0);
    prefs.setMindfulnessReminderConfig(
      const MindfulnessReminderConfig(
        enabled: true,
        reminderTime: LocalTime(0, 0),
      ),
    );

    await controller(prefs, minutesToday: 5).handleReminderAlarm();

    expect(notifier.shown, [(5.0, 10.0)]);
    expect(scheduler.scheduleCount, 1);
  });

  test('alarm does not notify after goal is met', () async {
    final prefs = await newPrefs();
    prefs.setDailyGoalFor(MetricDailyGoalKey.mindfulnessMinutes, 10.0);
    prefs.setMindfulnessReminderConfig(
      const MindfulnessReminderConfig(
        enabled: true,
        reminderTime: LocalTime(0, 0),
      ),
    );

    await controller(prefs, minutesToday: 10).handleReminderAlarm();

    expect(notifier.shown, isEmpty);
    expect(scheduler.scheduleCount, 1);
  });
}
