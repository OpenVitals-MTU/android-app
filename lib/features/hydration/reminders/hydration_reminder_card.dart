import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/time/local_date.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/reminder_toggle_tile.dart';
import '../../../ui/theme/app_colors.dart';
import 'hydration_reminder_settings_view_model.dart';

/// The hydration reminder settings card on the hydration detail screen. Port of
/// the Kotlin `HydrationReminderCard`.
///
/// Off: just the switch. On: the reminder interval (30-minute steps, 30–240 min)
/// and the active window, plus a note that reminders pause once the daily goal
/// is met. On Android 13+ with notifications denied, the summary says so and a
/// "Grant permission" button appears.
class HydrationReminderCard extends ConsumerStatefulWidget {
  const HydrationReminderCard({super.key});

  @override
  ConsumerState<HydrationReminderCard> createState() =>
      _HydrationReminderCardState();
}

class _HydrationReminderCardState extends ConsumerState<HydrationReminderCard> {
  late final AppLifecycleListener _lifecycle;

  @override
  void initState() {
    super.initState();
    // The user may grant (or revoke) notifications in system settings, which
    // Android does not report back — re-read it whenever we regain focus.
    _lifecycle = AppLifecycleListener(
      onResume: () => ref
          .read(hydrationReminderSettingsProvider.notifier)
          .refreshPermission(),
    );
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(hydrationReminderSettingsProvider);
    final notifier = ref.read(hydrationReminderSettingsProvider.notifier);
    final config = state.config;

    final startTime = _formatTime(context, config.activeStartTime);
    final endTime = _formatTime(context, config.activeEndTime);
    final summary = switch (state) {
      _ when state.isBlockedByPermission => l10n.hydrationRemindersPermissionNeeded,
      _ when config.enabled => l10n.hydrationRemindersSummaryOn(
          config.intervalMinutes,
          startTime,
          endTime,
        ),
      _ => l10n.hydrationRemindersSummaryOff,
    };

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ReminderToggleTile(
              icon: Icons.notifications_outlined,
              accentColor: AppColors.hydration,
              title: l10n.hydrationRemindersTitle,
              summary: summary,
              enabled: config.enabled,
              hasNotificationPermission: state.hasNotificationPermission,
              onToggle: notifier.setEnabled,
              onRequestPermission: notifier.requestPermission,
              onOpenSettings: notifier.openNotificationSettings,
              isTimingInexact: state.isTimingInexact,
              onEnableExactTiming: notifier.requestExactAlarms,
            ),
            if (config.enabled) ...[
              const SizedBox(height: 12),
              ReminderStepperRow(
                label: l10n.hydrationRemindersInterval,
                value: l10n.hydrationRemindersIntervalValue(
                  config.intervalMinutes,
                ),
                onDecrease:
                    state.canDecreaseInterval ? notifier.decreaseInterval : null,
                onIncrease:
                    state.canIncreaseInterval ? notifier.increaseInterval : null,
                decreaseTooltip: l10n.cdDecreaseHydrationReminderInterval,
                increaseTooltip: l10n.cdIncreaseHydrationReminderInterval,
              ),
              const SizedBox(height: 4),
              ReminderTimeRow(
                label: l10n.hydrationRemindersActiveStart,
                value: startTime,
                onTap: () => _pickTime(
                  context,
                  title: l10n.hydrationRemindersActiveStart,
                  initial: config.activeStartTime,
                  onPicked: notifier.setActiveStartTime,
                ),
              ),
              ReminderTimeRow(
                label: l10n.hydrationRemindersActiveEnd,
                value: endTime,
                onTap: () => _pickTime(
                  context,
                  title: l10n.hydrationRemindersActiveEnd,
                  initial: config.activeEndTime,
                  onPicked: notifier.setActiveEndTime,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                l10n.hydrationRemindersGoalNote,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  /// Renders in the user's locale + 12/24-hour preference, as the Kotlin
  /// `DateTimeFormatterProvider.shortTime()` does.
  String _formatTime(BuildContext context, LocalTime time) =>
      MaterialLocalizations.of(context).formatTimeOfDay(
        TimeOfDay(hour: time.hour, minute: time.minute),
      );

  Future<void> _pickTime(
    BuildContext context, {
    required String title,
    required LocalTime initial,
    required void Function(LocalTime time) onPicked,
  }) async {
    final picked = await showTimePicker(
      context: context,
      helpText: title,
      initialTime: TimeOfDay(hour: initial.hour, minute: initial.minute),
    );
    if (picked == null) return;
    // Seconds are dropped, matching Kotlin's `time.withSecond(0).withNano(0)`.
    onPicked(LocalTime(picked.hour, picked.minute));
  }
}
