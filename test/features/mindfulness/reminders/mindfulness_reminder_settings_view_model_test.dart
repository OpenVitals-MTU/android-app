import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/reminders/reminder_notifications.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/model/mindfulness_reminder_config.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_controller.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_settings_view_model.dart';

class _FakeMindfulnessRepository implements MindfulnessRepository {
  @override
  Future<Result<List<MindfulnessSession>>> loadMindfulnessSessions(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<MindfulnessSession>[]);

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

/// Stands in for the Android POST_NOTIFICATIONS + SCHEDULE_EXACT_ALARM gates.
class _FakePermissions implements ReminderNotificationPermissions {
  _FakePermissions({
    this.enabled = true,
    this.grantOnRequest = true,
    this.exact = true,
    this.grantExactOnRequest = true,
  });

  bool enabled;
  bool grantOnRequest;
  bool exact;
  bool grantExactOnRequest;
  int requestCount = 0;
  int openSettingsCount = 0;
  int requestExactCount = 0;

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

  @override
  Future<bool> canScheduleExact() async => exact;

  @override
  Future<bool> requestExactAlarms() async {
    requestExactCount++;
    exact = grantExactOnRequest;
    return exact;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _RecordingScheduler scheduler;
  late _RecordingNotifier notifier;
  late _FakePermissions permissions;
  late PreferencesRepository prefs;

  Future<ProviderContainer> newContainer({
    MindfulnessReminderConfig? initial,
  }) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    prefs = PreferencesRepository(await SharedPreferences.getInstance());
    if (initial != null) prefs.setMindfulnessReminderConfig(initial);

    final container = ProviderContainer(
      overrides: [
        preferencesRepositoryProvider.overrideWithValue(prefs),
        reminderNotificationPermissionsProvider.overrideWithValue(permissions),
        mindfulnessReminderControllerProvider.overrideWith(
          (ref) => MindfulnessReminderController(
            preferences: prefs,
            mindfulnessRepository: _FakeMindfulnessRepository(),
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
  Future<MindfulnessReminderSettingsViewModel> settled(
    ProviderContainer container,
  ) async {
    final subject = container.read(mindfulnessReminderSettingsProvider.notifier);
    for (var i = 0; i < 4; i++) {
      await Future<void>.delayed(Duration.zero);
    }
    return subject;
  }

  MindfulnessReminderSettingsState stateOf(ProviderContainer c) =>
      c.read(mindfulnessReminderSettingsProvider);

  test('starts from the persisted config', () async {
    final container = await newContainer(
      initial: const MindfulnessReminderConfig(
        enabled: true,
        reminderTime: LocalTime(21, 0),
      ),
    );
    await settled(container);

    expect(stateOf(container).config.enabled, isTrue);
    expect(stateOf(container).config.reminderTime, const LocalTime(21, 0));
  });

  test('enabling persists the config and arms the alarm', () async {
    final container = await newContainer();
    final subject = await settled(container);

    await subject.setEnabled(true);

    expect(stateOf(container).config.enabled, isTrue);
    expect(prefs.mindfulnessReminderConfig().enabled, isTrue);
    expect(scheduler.scheduled, hasLength(1));
  });

  test('disabling persists and clears the alarm', () async {
    final container = await newContainer(
      initial: const MindfulnessReminderConfig(enabled: true),
    );
    final subject = await settled(container);

    await subject.setEnabled(false);

    expect(prefs.mindfulnessReminderConfig().enabled, isFalse);
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
    expect(prefs.mindfulnessReminderConfig().enabled, isFalse);
    // Nothing armed: the switch never flipped on.
    expect(scheduler.scheduled, isEmpty);
  });

  test('granting permission for an already-enabled reminder arms it', () async {
    // The config says on, but the OS was blocking it — the classic
    // "enabled but nothing ever fires" state.
    permissions = _FakePermissions(enabled: false, grantOnRequest: true);
    final container = await newContainer(
      initial: const MindfulnessReminderConfig(enabled: true),
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
      initial: const MindfulnessReminderConfig(enabled: true),
    );
    final subject = await settled(container);

    await subject.openNotificationSettings();

    expect(permissions.openSettingsCount, 1);
  });

  test('changing the reminder time persists and re-arms', () async {
    final container = await newContainer(
      initial: const MindfulnessReminderConfig(enabled: true),
    );
    final subject = await settled(container);

    await subject.setReminderTime(const LocalTime(7, 45));

    expect(prefs.mindfulnessReminderConfig().reminderTime,
        const LocalTime(7, 45));
    expect(scheduler.scheduled, hasLength(1));
  });

  test('refreshPermission picks up a revoked permission', () async {
    final container = await newContainer(
      initial: const MindfulnessReminderConfig(enabled: true),
    );
    final subject = await settled(container);
    expect(stateOf(container).isBlockedByPermission, isFalse);

    permissions.enabled = false;
    await subject.refreshPermission();

    expect(stateOf(container).isBlockedByPermission, isTrue);
  });

  group('exact alarms', () {
    test('an enabled reminder without exact alarms surfaces inexact timing',
        () async {
      permissions = _FakePermissions(exact: false);
      final container = await newContainer(
        initial: const MindfulnessReminderConfig(enabled: true),
      );
      await settled(container);

      expect(stateOf(container).hasExactAlarms, isFalse);
      expect(stateOf(container).isBlockedByPermission, isFalse);
      expect(stateOf(container).isTimingInexact, isTrue);
    });

    test('granting exact alarms clears the nudge and re-arms precisely',
        () async {
      permissions = _FakePermissions(exact: false, grantExactOnRequest: true);
      final container = await newContainer(
        initial: const MindfulnessReminderConfig(enabled: true),
      );
      final subject = await settled(container);
      expect(stateOf(container).isTimingInexact, isTrue);
      final armedBefore = scheduler.scheduled.length;

      await subject.requestExactAlarms();

      expect(permissions.requestExactCount, 1);
      expect(stateOf(container).hasExactAlarms, isTrue);
      expect(stateOf(container).isTimingInexact, isFalse);
      expect(scheduler.scheduled.length, armedBefore + 1);
    });

    test('a declined exact-alarm request leaves timing inexact', () async {
      permissions = _FakePermissions(exact: false, grantExactOnRequest: false);
      final container = await newContainer(
        initial: const MindfulnessReminderConfig(enabled: true),
      );
      final subject = await settled(container);
      final armedBefore = scheduler.scheduled.length;

      await subject.requestExactAlarms();

      expect(stateOf(container).isTimingInexact, isTrue);
      expect(scheduler.scheduled.length, armedBefore);
    });
  });
}
