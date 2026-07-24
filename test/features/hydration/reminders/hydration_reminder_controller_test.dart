import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/domain/model/hydration_reminder_config.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Reads today's litres (for the goal) from [loadDailyHydration] and the last
/// intake instant (for the anchor) from [loadHydrationEntries]. Everything else
/// falls through [noSuchMethod] — the controller only touches these two.
class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository(
    this.litersToday, {
    this.throwsOnLoad = false,
    this.entries = const [],
  });

  final double litersToday;
  final bool throwsOnLoad;
  final List<HydrationEntry> entries;

  /// The range the anchor read asked for, so tests can pin that it spans back
  /// into yesterday (a 23:50 drink must survive midnight).
  LocalDate? entriesRangeStart;
  LocalDate? entriesRangeEnd;

  @override
  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async {
    if (throwsOnLoad) throw StateError('health connect unavailable');
    return Ok([DailyHydration(date: LocalDate.now(), liters: litersToday)]);
  }

  @override
  Future<Result<List<HydrationEntry>>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    entriesRangeStart = start;
    entriesRangeEnd = end;
    if (throwsOnLoad) throw StateError('health connect unavailable');
    return Ok(entries);
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

HydrationEntry _entryAt(DateTime startTime) => HydrationEntry(
      startTime: startTime,
      endTime: startTime,
      liters: 0.25,
      source: 'test',
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _RecordingScheduler scheduler;

  setUp(() {
    scheduler = _RecordingScheduler();
  });

  late _FakeHydrationRepository repository;

  HydrationReminderController controller(
    PreferencesRepository prefs, {
    double litersToday = 0.0,
    bool repositoryThrows = false,
    List<HydrationEntry> entries = const [],
    DateTime Function()? now,
  }) {
    repository = _FakeHydrationRepository(
      litersToday,
      throwsOnLoad: repositoryThrows,
      entries: entries,
    );
    return HydrationReminderController(
      preferences: prefs,
      hydrationRepository: repository,
      scheduler: scheduler,
      now: now ?? DateTime.now,
    );
  }

  test('disabled config clears and schedules nothing', () async {
    final prefs = await newPrefs();

    await controller(prefs).applyConfig(
      const HydrationReminderConfig(enabled: false),
    );

    expect(scheduler.cancelCount, 1);
    expect(scheduler.batches, isEmpty);
  });

  test('enabled config schedules a batch', () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;

    await controller(prefs, litersToday: 1.0).applyConfig(
      const HydrationReminderConfig(enabled: true),
    );

    expect(scheduler.batches, hasLength(1));
    expect(scheduler.lastBatch, isNotEmpty);
  });

  test('anchors the first reminder to the last logged drink', () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;

    // Default window 07:00–23:00, interval 120 min. Last drink at 09:00, now
    // 10:00 → first reminder 09:00 + 2h = 11:00, not now + 2h.
    await controller(
      prefs,
      litersToday: 1.0,
      entries: [_entryAt(DateTime.utc(2026, 6, 1, 9))],
      now: () => DateTime.utc(2026, 6, 1, 10),
    ).applyConfig(const HydrationReminderConfig(enabled: true));

    expect(scheduler.lastBatch.first, DateTime.utc(2026, 6, 1, 11));
  });

  test('the anchor read spans back into yesterday, not just today', () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;

    // A drink at 23:50 must still anchor the schedule after midnight — a
    // today-only read made the pre-midnight intake vanish at 00:10 and let an
    // early reminder fire from the window start.
    await controller(
      prefs,
      litersToday: 0.0,
      entries: [
        _entryAt(DateTime.now().subtract(const Duration(minutes: 30))),
      ],
    ).applyConfig(const HydrationReminderConfig(enabled: true));

    expect(repository.entriesRangeStart, LocalDate.now().minusDays(1));
    expect(repository.entriesRangeEnd, LocalDate.now());
  });

  test('a met goal schedules only tomorrow onward', () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;

    await controller(
      prefs,
      litersToday: 2.0,
      now: () => DateTime.utc(2026, 6, 1, 10),
    ).applyConfig(const HydrationReminderConfig(enabled: true));

    // Tomorrow's active start (07:00) plus the interval, and nothing today.
    expect(scheduler.lastBatch.first, DateTime.utc(2026, 6, 2, 9));
  });

  test('an intake read failure counts as zero and still schedules', () async {
    // Kotlin's `runCatching { … }.getOrDefault(0.0)`: the user still gets
    // reminded when Health Connect cannot be read.
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;

    await controller(prefs, repositoryThrows: true).applyConfig(
      const HydrationReminderConfig(enabled: true),
    );

    expect(scheduler.batches, hasLength(1));
    expect(scheduler.lastBatch, isNotEmpty);
  });

  test('logging a drink re-anchors and reschedules', () async {
    final prefs = await newPrefs();
    prefs.hydrationDailyGoalLiters = 2.0;
    prefs.setHydrationReminderConfig(
      const HydrationReminderConfig(enabled: true),
    );

    await controller(prefs, litersToday: 1.0).onHydrationLogged();

    expect(scheduler.batches, hasLength(1));
    expect(scheduler.lastBatch, isNotEmpty);
  });
}
