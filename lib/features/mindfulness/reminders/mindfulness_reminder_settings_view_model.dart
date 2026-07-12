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
  });

  final MindfulnessReminderConfig config;
  final bool hasNotificationPermission;

  /// The reminder is on, but the OS will silently drop every notification.
  bool get isBlockedByPermission => config.enabled && !hasNotificationPermission;

  MindfulnessReminderSettingsState copyWith({
    MindfulnessReminderConfig? config,
    bool? hasNotificationPermission,
  }) =>
      MindfulnessReminderSettingsState(
        config: config ?? this.config,
        hasNotificationPermission:
            hasNotificationPermission ?? this.hasNotificationPermission,
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
    );
  }

  Future<void> refreshPermission() async {
    final granted =
        await ref.read(reminderNotificationPermissionsProvider).isEnabled();
    if (!ref.mounted) return;
    state = state.copyWith(hasNotificationPermission: granted);
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
