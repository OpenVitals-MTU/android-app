import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/mindfulness_reminder_config.dart';

/// The mindfulness reminder card's state: the persisted config plus whether the
/// OS currently lets the app post notifications.
class MindfulnessReminderSettingsState {
  const MindfulnessReminderSettingsState({
    required this.config,
    required this.hasNotificationPermission,
    required this.hasExactAlarms,
  });

  final MindfulnessReminderConfig config;
  final bool hasNotificationPermission;

  /// Whether reminders may fire at their exact time. False on Android 12+ with
  /// SCHEDULE_EXACT_ALARM not granted — the reminder still fires, but inside
  /// Android's inexact window rather than on the dot.
  final bool hasExactAlarms;

  /// The reminder is on, but the OS will silently drop every notification.
  bool get isBlockedByPermission => config.enabled && !hasNotificationPermission;

  /// The reminder is on and delivering, but only approximately — offer to make it
  /// precise. Distinct from [isBlockedByPermission]: nothing is broken here.
  bool get isTimingInexact =>
      config.enabled && hasNotificationPermission && !hasExactAlarms;

  MindfulnessReminderSettingsState copyWith({
    MindfulnessReminderConfig? config,
    bool? hasNotificationPermission,
    bool? hasExactAlarms,
  }) =>
      MindfulnessReminderSettingsState(
        config: config ?? this.config,
        hasNotificationPermission:
            hasNotificationPermission ?? this.hasNotificationPermission,
        hasExactAlarms: hasExactAlarms ?? this.hasExactAlarms,
      );
}

/// Drives the mindfulness reminder card. Mirrors the hydration one, but the
/// schedule is a single daily time rather than an interval within a window.
class MindfulnessReminderSettingsViewModel
    extends Notifier<MindfulnessReminderSettingsState> {
  @override
  MindfulnessReminderSettingsState build() {
    final config = ref.read(mindfulnessReminderControllerProvider).config();
    Future.microtask(refreshPermission);
    return MindfulnessReminderSettingsState(
      config: config.normalized(),
      hasNotificationPermission: true,
      hasExactAlarms: true,
    );
  }

  /// Re-reads both POST_NOTIFICATIONS and SCHEDULE_EXACT_ALARM — call when the
  /// screen regains focus, since the user may have changed either in settings.
  Future<void> refreshPermission() async {
    final permissions = ref.read(reminderNotificationPermissionsProvider);
    final granted = await permissions.isEnabled();
    final exact = await permissions.canScheduleExact();
    if (!ref.mounted) return;
    state = state.copyWith(
      hasNotificationPermission: granted,
      hasExactAlarms: exact,
    );
  }

  /// Turning reminders on without permission asks for it first, and only enables
  /// them if granted — otherwise the switch would flip on and nothing would fire.
  Future<void> setEnabled(bool enabled) async {
    if (!enabled) {
      await _update(state.config.copyWith(enabled: false));
      return;
    }
    var granted = state.hasNotificationPermission;
    if (!granted) granted = await requestPermission();
    if (!ref.mounted || !granted) return;
    await _update(state.config.copyWith(enabled: true));
  }

  Future<bool> requestPermission() async {
    final granted =
        await ref.read(reminderNotificationPermissionsProvider).request();
    if (!ref.mounted) return granted;
    state = state.copyWith(hasNotificationPermission: granted);
    if (granted && state.config.enabled) await _update(state.config);
    return granted;
  }

  /// Opens system notification settings — the escape hatch when POST_NOTIFICATIONS
  /// is permanently denied and [requestPermission] can no longer prompt.
  Future<void> openNotificationSettings() =>
      ref.read(reminderNotificationPermissionsProvider).openSettings();

  /// Sends the user to the system SCHEDULE_EXACT_ALARM screen to upgrade the
  /// reminder from inexact to exact timing. Re-arms once granted so the already
  /// enabled reminder becomes precise immediately.
  Future<void> requestExactAlarms() async {
    final granted =
        await ref.read(reminderNotificationPermissionsProvider).requestExactAlarms();
    if (!ref.mounted) return;
    state = state.copyWith(hasExactAlarms: granted);
    if (granted && state.config.enabled) await _update(state.config);
  }

  Future<void> setReminderTime(LocalTime time) =>
      _update(state.config.copyWith(reminderTime: time));

  Future<void> _update(MindfulnessReminderConfig config) async {
    await ref.read(mindfulnessReminderControllerProvider).updateConfig(config);
    if (!ref.mounted) return;
    state = state.copyWith(config: config.normalized());
  }
}

final mindfulnessReminderSettingsProvider = NotifierProvider<
    MindfulnessReminderSettingsViewModel, MindfulnessReminderSettingsState>(
  MindfulnessReminderSettingsViewModel.new,
);
