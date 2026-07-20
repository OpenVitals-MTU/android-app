import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/domain/insights/daily_goals.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/model/mindfulness_reminder_config.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _FakeMindfulnessRepository implements MindfulnessRepository {
  _FakeMindfulnessRepository(this.minutesToday);

  final int minutesToday;

  @override
  Future<Result<List<MindfulnessSession>>> loadMindfulnessSessions(
    LocalDate start,
    LocalDate end,
  ) async {
    final now = DateTime.now();
    return Ok([
      MindfulnessSession(
        id: '$minutesToday',
        title: null,
        startTime: now.subtract(Duration(minutes: minutesToday)),
        endTime: now,
        durationMs: minutesToday * 60000,
        source: 'test',
      ),
    ]);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      super.noSuchMethod(invocation);
}

class _RecordingScheduler implements ReminderScheduler {
  final List<List<DateTime>> batches = [];
  int cancelCount = 0;

  List<DateTime> get lastBatch => batches.last;

  @override
  Future<void> scheduleAll(List<DateTime> triggers, ReminderGoalProgress progress) async =>
      batches.add(triggers);

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

  setUp(() {
    scheduler = _RecordingScheduler();
  });

  MindfulnessReminderController controller(
    PreferencesRepository prefs, {
    int minutesToday = 0,
    DateTime Function()? now,
  }) =>
      MindfulnessReminderController(
        preferences: prefs,
        mindfulnessRepository: _FakeMindfulnessRepository(minutesToday),
        scheduler: scheduler,
        now: now ?? DateTime.now,
      );

  test('disabled config clears and schedules nothing', () async {
    final prefs = await newPrefs();

    await controller(prefs).applyConfig(
      const MindfulnessReminderConfig(enabled: false),
    );

    expect(scheduler.cancelCount, 1);
    expect(scheduler.batches, isEmpty);
  });

  test('enabled config schedules a batch at the daily time', () async {
    final prefs = await newPrefs();
    prefs.setDailyGoalFor(MetricDailyGoalKey.mindfulnessMinutes, 10.0);

    await controller(
      prefs,
      minutesToday: 5,
      now: () => DateTime.utc(2026, 6, 1, 9),
    ).applyConfig(
      const MindfulnessReminderConfig(
        enabled: true,
        reminderTime: LocalTime(18, 0),
      ),
    );

    // 18:00 today is still ahead of 09:00.
    expect(scheduler.lastBatch.first, DateTime.utc(2026, 6, 1, 18));
  });

  test('a met goal schedules only tomorrow onward', () async {
    final prefs = await newPrefs();
    prefs.setDailyGoalFor(MetricDailyGoalKey.mindfulnessMinutes, 10.0);

    await controller(
      prefs,
      minutesToday: 10,
      now: () => DateTime.utc(2026, 6, 1, 9),
    ).applyConfig(
      const MindfulnessReminderConfig(
        enabled: true,
        reminderTime: LocalTime(18, 0),
      ),
    );

    expect(scheduler.lastBatch.first, DateTime.utc(2026, 6, 2, 18));
  });
}
