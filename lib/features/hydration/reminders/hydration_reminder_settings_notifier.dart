import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/hydration_reminder_config.dart';

/// The hydration reminder card's state: the persisted config plus whether the
/// OS currently lets the app post notifications.
class HydrationReminderSettingsState {
  const HydrationReminderSettingsState({
    required this.config,
    required this.hasNotificationPermission,
  });

  final HydrationReminderConfig config;

  /// False only on Android 13+ with POST_NOTIFICATIONS denied.
  final bool hasNotificationPermission;

  /// The reminder is on, but the OS will silently drop every notification.
  bool get isBlockedByPermission => config.enabled && !hasNotificationPermission;

  bool get canDecreaseInterval =>
      config.intervalMinutes > HydrationReminderConfig.minIntervalMinutes;

  bool get canIncreaseInterval =>
      config.intervalMinutes < HydrationReminderConfig.maxIntervalMinutes;

  HydrationReminderSettingsState copyWith({
    HydrationReminderConfig? config,
    bool? hasNotificationPermission,
  }) =>
      HydrationReminderSettingsState(
        config: config ?? this.config,
        hasNotificationPermission:
            hasNotificationPermission ?? this.hasNotificationPermission,
      );
}

/// Drives the hydration reminder card. Port of the reminder slice of the Kotlin
/// `HydrationViewModel`: every mutation persists the config *and* re-applies the
/// schedule through the controller, so the alarm always matches what is on
/// screen.
class HydrationReminderSettingsNotifier
    extends Notifier<HydrationReminderSettingsState> {
  @override
  HydrationReminderSettingsState build() {
    final config = ref.read(hydrationReminderControllerProvider).config();
    // Optimistic until the platform answers; refreshed on the first frame.
    Future.microtask(refreshPermission);
    return HydrationReminderSettingsState(
      config: config.normalized(),
      hasNotificationPermission: true,
    );
  }

  /// Re-reads the OS permission — call when the screen regains focus, since the
  /// user may have changed it in system settings.
  Future<void> refreshPermission() async {
    final granted =
        await ref.read(reminderNotificationPermissionsProvider).isEnabled();
    if (!ref.mounted) return;
    state = state.copyWith(hasNotificationPermission: granted);
  }

  /// Turning reminders on when the permission is missing asks for it first, and
  /// only enables them if it is granted — otherwise the switch would flip on and
  /// nothing would ever fire.
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

  /// Prompts for POST_NOTIFICATIONS. Returns whether the app may now notify.
  Future<bool> requestPermission() async {
    final granted =
        await ref.read(reminderNotificationPermissionsProvider).request();
    if (!ref.mounted) return granted;
    state = state.copyWith(hasNotificationPermission: granted);
    // A reminder that is already on becomes live the moment permission lands.
    if (granted && state.config.enabled) await _update(state.config);
    return granted;
  }

  Future<void> increaseInterval() => _update(
        state.config.copyWith(
          intervalMinutes: state.config.intervalMinutes +
              HydrationReminderConfig.intervalStepMinutes,
        ),
      );

  Future<void> decreaseInterval() => _update(
        state.config.copyWith(
          intervalMinutes: state.config.intervalMinutes -
              HydrationReminderConfig.intervalStepMinutes,
        ),
      );

  Future<void> setActiveStartTime(LocalTime time) =>
      _update(state.config.copyWith(activeStartTime: time));

  Future<void> setActiveEndTime(LocalTime time) =>
      _update(state.config.copyWith(activeEndTime: time));

  /// Persists and re-arms. [HydrationReminderController.updateConfig] normalizes
  /// the interval, so an out-of-range step is clamped rather than stored.
  Future<void> _update(HydrationReminderConfig config) async {
    await ref.read(hydrationReminderControllerProvider).updateConfig(config);
    if (!ref.mounted) return;
    state = state.copyWith(config: config.normalized());
  }
}

final hydrationReminderSettingsProvider = NotifierProvider<
    HydrationReminderSettingsNotifier, HydrationReminderSettingsState>(
  HydrationReminderSettingsNotifier.new,
);
