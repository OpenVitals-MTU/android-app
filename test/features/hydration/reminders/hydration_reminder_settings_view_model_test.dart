import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/reminders/reminder_notifications.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/hydration_reminder_config.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_settings_view_model.dart';

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

class _RecordingScheduler implements ReminderScheduler {
  final List<DateTime> scheduled = [];
  int cancelCount = 0;

  @override
  Future<void> schedule(DateTime triggerAt) async => scheduled.add(triggerAt);

  @override
  Future<void> cancel() async => cancelCount++;
}

class _RecordingNotifier implements ReminderNotifier {
  int cancelCount = 0;

  @override
  Future<void> show(ReminderGoalProgress progress) async {}

  @override
  Future<void> cancel() async => cancelCount++;
}

/// Stands in for the Android POST_NOTIFICATIONS gate.
class _FakePermissions implements ReminderNotificationPermissions {
  _FakePermissions({this.enabled = true, this.grantOnRequest = true});

  bool enabled;
  bool grantOnRequest;
  int requestCount = 0;
  int openSettingsCount = 0;

  @override
  Future<bool> isEnabled() async => enabled;

  @override
  Future<bool> request() async {
    requestCount++;
    enabled = grantOnRequest;
    return enabled;
  }

  @override
  Future<bool> openSettings() async {
    openSettingsCount++;
    return true;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _RecordingScheduler scheduler;
  late _RecordingNotifier notifier;
  late _FakePermissions permissions;
  late PreferencesRepository prefs;

  Future<ProviderContainer> newContainer({
    HydrationReminderConfig? initial,
  }) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    prefs = PreferencesRepository(await SharedPreferences.getInstance());
    if (initial != null) prefs.setHydrationReminderConfig(initial);

    final container = ProviderContainer(
      overrides: [
        preferencesRepositoryProvider.overrideWithValue(prefs),
        reminderNotificationPermissionsProvider.overrideWithValue(permissions),
        hydrationReminderControllerProvider.overrideWith(
          (ref) => HydrationReminderController(
            preferences: prefs,
            hydrationRepository: _FakeHydrationRepository(),
            notifier: notifier,
            scheduler: scheduler,
          ),
        ),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  setUp(() {
    scheduler = _RecordingScheduler();
    notifier = _RecordingNotifier();
    permissions = _FakePermissions();
  });

  /// Builds the notifier and lets its permission microtask settle.
  Future<HydrationReminderSettingsViewModel> settled(
    ProviderContainer container,
  ) async {
    final subject = container.read(hydrationReminderSettingsProvider.notifier);
    for (var i = 0; i < 4; i++) {
      await Future<void>.delayed(Duration.zero);
    }
    return subject;
  }

  HydrationReminderSettingsState stateOf(ProviderContainer c) =>
      c.read(hydrationReminderSettingsProvider);

  test('starts from the persisted config', () async {
    final container = await newContainer(
      initial: const HydrationReminderConfig(enabled: true, intervalMinutes: 90),
    );
    await settled(container);

    expect(stateOf(container).config.enabled, isTrue);
    expect(stateOf(container).config.intervalMinutes, 90);
  });

  test('enabling persists the config and arms the alarm', () async {
    final container = await newContainer();
    final subject = await settled(container);

    await subject.setEnabled(true);

    expect(stateOf(container).config.enabled, isTrue);
    expect(prefs.hydrationReminderConfig().enabled, isTrue);
    expect(scheduler.scheduled, hasLength(1));
  });

  test('disabling persists and clears the alarm', () async {
    final container = await newContainer(
      initial: const HydrationReminderConfig(enabled: true),
    );
    final subject = await settled(container);

    await subject.setEnabled(false);

    expect(prefs.hydrationReminderConfig().enabled, isFalse);
    expect(scheduler.cancelCount, 1);
    expect(notifier.cancelCount, 1);
  });

  test('enabling without permission asks first, and enables once granted',
      () async {
    permissions = _FakePermissions(enabled: false, grantOnRequest: true);
    final container = await newContainer();
    final subject = await settled(container);
    expect(stateOf(container).hasNotificationPermission, isFalse);

    await subject.setEnabled(true);

    expect(permissions.requestCount, 1);
    expect(stateOf(container).config.enabled, isTrue);
    expect(scheduler.scheduled, hasLength(1));
  });

  test('a denied permission leaves the reminder off rather than silently dead',
      () async {
    permissions = _FakePermissions(enabled: false, grantOnRequest: false);
    final container = await newContainer();
    final subject = await settled(container);

    await subject.setEnabled(true);

    expect(permissions.requestCount, 1);
    expect(stateOf(container).config.enabled, isFalse);
    expect(prefs.hydrationReminderConfig().enabled, isFalse);
    // Nothing armed: the switch never flipped on.
    expect(scheduler.scheduled, isEmpty);
  });

  test('granting permission for an already-enabled reminder arms it', () async {
    // The config says on, but the OS was blocking it — the classic
    // "enabled but nothing ever fires" state.
    permissions = _FakePermissions(enabled: false, grantOnRequest: true);
    final container = await newContainer(
      initial: const HydrationReminderConfig(enabled: true),
    );
    final subject = await settled(container);
    expect(stateOf(container).isBlockedByPermission, isTrue);

    await subject.requestPermission();

    expect(stateOf(container).isBlockedByPermission, isFalse);
    expect(scheduler.scheduled, hasLength(1));
  });

  test('openNotificationSettings opens the OS settings — the permanently-denied '
      'escape hatch', () async {
    final container = await newContainer(
      initial: const HydrationReminderConfig(enabled: true),
    );
    final subject = await settled(container);

    await subject.openNotificationSettings();

    expect(permissions.openSettingsCount, 1);
  });

  group('interval', () {
    test('steps by 30 minutes and re-arms', () async {
      final container = await newContainer(
        initial:
            const HydrationReminderConfig(enabled: true, intervalMinutes: 120),
      );
      final subject = await settled(container);

      await subject.increaseInterval();
      expect(stateOf(container).config.intervalMinutes, 150);

      await subject.decreaseInterval();
      await subject.decreaseInterval();
      expect(stateOf(container).config.intervalMinutes, 90);

      expect(prefs.hydrationReminderConfig().intervalMinutes, 90);
      expect(scheduler.scheduled, hasLength(3));
    });

    test('is clamped to its bounds, and the buttons disable there', () async {
      final container = await newContainer(
        initial: const HydrationReminderConfig(
          enabled: true,
          intervalMinutes: HydrationReminderConfig.maxIntervalMinutes,
        ),
      );
      final subject = await settled(container);
      expect(stateOf(container).canIncreaseInterval, isFalse);
      expect(stateOf(container).canDecreaseInterval, isTrue);

      // Even if the UI let it through, the config normalizes rather than storing
      // an out-of-range interval.
      await subject.increaseInterval();
      expect(
        stateOf(container).config.intervalMinutes,
        HydrationReminderConfig.maxIntervalMinutes,
      );

      await subject.decreaseInterval();
      expect(stateOf(container).config.intervalMinutes, 210);
    });

    test('cannot go below the minimum', () async {
      final container = await newContainer(
        initial: const HydrationReminderConfig(
          enabled: true,
          intervalMinutes: HydrationReminderConfig.minIntervalMinutes,
        ),
      );
      final subject = await settled(container);
      expect(stateOf(container).canDecreaseInterval, isFalse);

      await subject.decreaseInterval();
      expect(
        stateOf(container).config.intervalMinutes,
        HydrationReminderConfig.minIntervalMinutes,
      );
    });
  });

  test('changing the active window persists and re-arms', () async {
    final container = await newContainer(
      initial: const HydrationReminderConfig(enabled: true),
    );
    final subject = await settled(container);

    await subject.setActiveStartTime(const LocalTime(6, 30));
    await subject.setActiveEndTime(const LocalTime(22, 15));

    final stored = prefs.hydrationReminderConfig();
    expect(stored.activeStartTime, const LocalTime(6, 30));
    expect(stored.activeEndTime, const LocalTime(22, 15));
    expect(scheduler.scheduled, hasLength(2));
  });

  test('refreshPermission picks up a revoked permission', () async {
    final container = await newContainer(
      initial: const HydrationReminderConfig(enabled: true),
    );
    final subject = await settled(container);
    expect(stateOf(container).isBlockedByPermission, isFalse);

    permissions.enabled = false;
    await subject.refreshPermission();

    expect(stateOf(container).isBlockedByPermission, isTrue);
  });
}
