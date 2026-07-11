import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/time/local_date.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/reminder_toggle_tile.dart';
import '../../../ui/theme/app_colors.dart';
import 'mindfulness_reminder_settings_notifier.dart';

/// The mindfulness reminder settings card. Port of the Kotlin
/// `MindfulnessReminderCard`.
///
/// A single daily time, so there are no quiet hours to configure — just the
/// switch, the time, and a note that the reminder pauses once the daily goal is
/// met.
class MindfulnessReminderCard extends ConsumerStatefulWidget {
  const MindfulnessReminderCard({super.key});

  @override
  ConsumerState<MindfulnessReminderCard> createState() =>
      _MindfulnessReminderCardState();
}

class _MindfulnessReminderCardState
    extends ConsumerState<MindfulnessReminderCard> {
  late final AppLifecycleListener _lifecycle;

  @override
  void initState() {
    super.initState();
    // Granting in system settings is not reported back to the app.
    _lifecycle = AppLifecycleListener(
      onResume: () => ref
          .read(mindfulnessReminderSettingsProvider.notifier)
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
    final state = ref.watch(mindfulnessReminderSettingsProvider);
    final notifier = ref.read(mindfulnessReminderSettingsProvider.notifier);
    final config = state.config;

    final time = _formatTime(context, config.reminderTime);
    final summary = switch (state) {
      _ when state.isBlockedByPermission =>
        l10n.mindfulnessRemindersPermissionNeeded,
      _ when config.enabled => l10n.mindfulnessRemindersSummaryOn(time),
      _ => l10n.mindfulnessRemindersSummaryOff,
    };

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ReminderToggleTile(
              icon: Icons.notifications_outlined,
              accentColor: AppColors.mindfulness,
              title: l10n.mindfulnessRemindersTitle,
              summary: summary,
              enabled: config.enabled,
              hasNotificationPermission: state.hasNotificationPermission,
              onToggle: notifier.setEnabled,
              onRequestPermission: notifier.requestPermission,
            ),
            if (config.enabled) ...[
              const SizedBox(height: 12),
              ReminderTimeRow(
                label: l10n.mindfulnessRemindersTime,
                value: time,
                onTap: () => _pickTime(
                  context,
                  title: l10n.mindfulnessRemindersTime,
                  initial: config.reminderTime,
                  onPicked: notifier.setReminderTime,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                l10n.mindfulnessRemindersGoalNote,
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
    onPicked(LocalTime(picked.hour, picked.minute));
  }
}
