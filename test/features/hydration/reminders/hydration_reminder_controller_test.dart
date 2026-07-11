import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/domain/model/hydration_reminder_config.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Ported from the Kotlin `HydrationReminderControllerTest`, with the device
/// scheduler / notifier replaced by recording fakes.
class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository(this.litersToday, {this.throwsOnLoad = false});

  final double litersToday;
  final bool throwsOnLoad;

  @override
  Future<List<DailyHydration>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async {
    if (throwsOnLoad) throw StateError('health connect unavailable');
    return [DailyHydration(date: LocalDate.now(), liters: litersToday)];
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

  HydrationReminderController controller(
    PreferencesRepository prefs, {
    double litersToday = 0.0,
    bool repositoryThrows = false,
  }) =>
      HydrationReminderController(
        preferences: prefs,
        hydrationRepository: _FakeHydrationRepository(
          litersToday,
          throwsOnLoad: repositoryThrows,
        ),
        notifier: notifier,
        scheduler: scheduler,
      );

  test('disabled config clears alarm and notification', () async {
    final prefs = await newPrefs();

    await controller(prefs).applyConfig(
      const HydrationReminderConfig(enabled: false),
    );

    expect(scheduler.cancelCount, 1);
    expect(notifier.cancelCount, 1);
    expect(scheduler.scheduleCount, 0);
  });

  test('enabled config schedules next reminder without notifying', () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;

    await controller(prefs, litersToday: 1.0).applyConfig(
      const HydrationReminderConfig(enabled: true),
    );

    expect(scheduler.scheduleCount, 1);
    expect(notifier.shown, isEmpty);
  });

  test('alarm shows notification when goal unmet and within active hours',
      () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;
    prefs.setHydrationReminderConfig(
      const HydrationReminderConfig(
        enabled: true,
        activeStartTime: LocalTime(0, 0),
        activeEndTime: LocalTime(0, 0),
      ),
    );

    await controller(prefs, litersToday: 1.0).handleReminderAlarm();

    expect(notifier.shown, [(1.0, 2.0)]);
    expect(scheduler.scheduleCount, 1);
  });

  test('alarm does not notify after goal is met', () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;
    prefs.setHydrationReminderConfig(
      const HydrationReminderConfig(
        enabled: true,
        activeStartTime: LocalTime(0, 0),
        activeEndTime: LocalTime(0, 0),
      ),
    );

    await controller(prefs, litersToday: 2.0).handleReminderAlarm();

    expect(notifier.shown, isEmpty);
    expect(scheduler.scheduleCount, 1);
  });

  test('an intake read failure counts as zero, never as a met goal', () async {
    // Kotlin's `runCatching { … }.getOrDefault(0.0)`: the user still gets
    // reminded when Health Connect cannot be read.
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;
    prefs.setHydrationReminderConfig(
      const HydrationReminderConfig(
        enabled: true,
        activeStartTime: LocalTime(0, 0),
        activeEndTime: LocalTime(0, 0),
      ),
    );

    await controller(prefs, repositoryThrows: true).handleReminderAlarm();

    expect(notifier.shown, [(0.0, 2.0)]);
    expect(scheduler.scheduleCount, 1);
  });

  test('hiding the reminder leaves the alarm chain armed', () async {
    final prefs = await newPrefs();

    await controller(prefs).hideReminderNotification();

    expect(notifier.cancelCount, 1);
    expect(scheduler.cancelCount, 0);
  });
}
