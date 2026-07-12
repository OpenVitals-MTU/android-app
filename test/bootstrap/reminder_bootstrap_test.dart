import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/bootstrap/reminder_bootstrap.dart';
import 'package:openvitals/core/reminders/alarm_manager_reminder_scheduler.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/hydration_reminder_config.dart';
import 'package:openvitals/domain/model/mindfulness_reminder_config.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_controller.dart';

class RecordingScheduler implements ReminderScheduler {
  final List<DateTime> scheduled = [];
  int cancelCount = 0;

  @override
  Future<void> schedule(DateTime triggerAt) async => scheduled.add(triggerAt);

  @override
  Future<void> cancel() async => cancelCount++;
}

class RecordingNotifier implements ReminderNotifier {
  int cancelCount = 0;

  @override
  Future<void> show(ReminderGoalProgress progress) async {}

  @override
  Future<void> cancel() async => cancelCount++;
}

class _FakeHydrationRepository implements HydrationRepository {
  @override
  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<DailyHydration>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeMindfulnessRepository implements MindfulnessRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _RecordingAlarms implements AndroidAlarmManagerApi {
  int initializeCount = 0;
  bool throwOnInitialize = false;

  @override
  Future<bool> initialize() async {
    initializeCount++;
    if (throwOnInitialize) throw StateError('no alarm service');
    return true;
  }

  @override
  Future<bool> oneShotAt(
    DateTime time,
    int id,
    void Function() callback, {
    bool exact = false,
    bool wakeup = false,
    bool allowWhileIdle = false,
    bool rescheduleOnReboot = false,
  }) async =>
      true;

  @override
  Future<bool> cancel(int id) async => true;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late RecordingScheduler hydrationScheduler;
  late RecordingNotifier hydrationNotifier;
  late RecordingScheduler mindfulnessScheduler;
  late RecordingNotifier mindfulnessNotifier;
  late _RecordingAlarms alarms;

  Future<ProviderContainer> newContainer({
    bool hydrationEnabled = true,
    bool mindfulnessEnabled = false,
  }) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = PreferencesRepository(await SharedPreferences.getInstance());
    prefs.setHydrationReminderConfig(
      HydrationReminderConfig(enabled: hydrationEnabled),
    );
    prefs.setMindfulnessReminderConfig(
      MindfulnessReminderConfig(enabled: mindfulnessEnabled),
    );

    final container = ProviderContainer(
      overrides: [
        preferencesRepositoryProvider.overrideWithValue(prefs),
        hydrationReminderControllerProvider.overrideWith(
          (ref) => HydrationReminderController(
            preferences: prefs,
            hydrationRepository: _FakeHydrationRepository(),
            notifier: hydrationNotifier,
            scheduler: hydrationScheduler,
          ),
        ),
        mindfulnessReminderControllerProvider.overrideWith(
          (ref) => MindfulnessReminderController(
            preferences: prefs,
            mindfulnessRepository: _FakeMindfulnessRepository(),
            notifier: mindfulnessNotifier,
            scheduler: mindfulnessScheduler,
          ),
        ),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  setUp(() {
    hydrationScheduler = RecordingScheduler();
    hydrationNotifier = RecordingNotifier();
    mindfulnessScheduler = RecordingScheduler();
    mindfulnessNotifier = RecordingNotifier();
    alarms = _RecordingAlarms();
  });

  Future<ReminderBootstrapResult> boot(
    ProviderContainer container, {
    TargetPlatform platform = TargetPlatform.android,
    Future<bool> Function()? initializeTimeZone,
  }) =>
      bootstrapReminders(
        container,
        platform: platform,
        alarms: alarms,
        initializeTimeZone: initializeTimeZone ?? () async => true,
      );

  test('restores an enabled reminder and clears a disabled one', () async {
    final container = await newContainer(
      hydrationEnabled: true,
      mindfulnessEnabled: false,
    );

    final result = await boot(container);

    expect(result.schedulesRestored, isTrue);
    // Hydration re-arms; this is the app-start / post-update boot restore.
    expect(hydrationScheduler.scheduled, hasLength(1));
    // Mindfulness is off, so its alarm is cancelled rather than left dangling.
    expect(mindfulnessScheduler.scheduled, isEmpty);
    expect(mindfulnessScheduler.cancelCount, 1);
  });

  test('starts the alarm service on Android, before arming anything', () async {
    final container = await newContainer();

    final result = await boot(container);

    expect(alarms.initializeCount, 1);
    expect(result.alarmServiceReady, isTrue);
  });

  test('does not touch the alarm service off Android', () async {
    final container = await newContainer();

    final result = await boot(container, platform: TargetPlatform.iOS);

    expect(alarms.initializeCount, 0);
    expect(result.alarmServiceReady, isFalse);
    // The scheduled-notification fallback still gets armed.
    expect(hydrationScheduler.scheduled, hasLength(1));
  });

  test('a failed time-zone init still restores the schedules', () async {
    final container = await newContainer();

    final result = await boot(
      container,
      initializeTimeZone: () async => false,
    );

    expect(result.timeZoneReady, isFalse);
    expect(result.schedulesRestored, isTrue);
    expect(hydrationScheduler.scheduled, hasLength(1));
  });

  test('a throwing time-zone init is swallowed and does not abort startup',
      () async {
    final container = await newContainer();

    final result = await boot(
      container,
      initializeTimeZone: () async => throw StateError('no platform'),
    );

    expect(result.timeZoneReady, isFalse);
    expect(result.schedulesRestored, isTrue);
  });

  test('a throwing alarm service is swallowed and schedules still restore',
      () async {
    alarms.throwOnInitialize = true;
    final container = await newContainer();

    final result = await boot(container);

    expect(result.alarmServiceReady, isFalse);
    expect(result.schedulesRestored, isTrue);
    expect(hydrationScheduler.scheduled, hasLength(1));
  });
}
