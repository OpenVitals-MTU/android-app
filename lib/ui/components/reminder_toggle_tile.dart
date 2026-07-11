import 'package:flutter/material.dart';

import '../../l10n/app_localizations.dart';

/// The header of a reminder settings card: icon, title, a one-line summary and
/// the on/off switch, plus the "grant permission" affordance when the reminder
/// is on but the OS will not let it notify.
///
/// Shared by every reminder feature; the schedule controls below it differ per
/// feature (an interval + window for hydration, a single time for mindfulness).
class ReminderToggleTile extends StatelessWidget {
  const ReminderToggleTile({
    super.key,
    required this.icon,
    required this.accentColor,
    required this.title,
    required this.summary,
    required this.enabled,
    required this.hasNotificationPermission,
    required this.onToggle,
    required this.onRequestPermission,
  });

  final IconData icon;
  final Color accentColor;
  final String title;

  /// Already resolved by the caller: "off", the schedule, or the permission
  /// warning.
  final String summary;
  final bool enabled;
  final bool hasNotificationPermission;
  final void Function(bool enabled) onToggle;
  final VoidCallback onRequestPermission;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Icon(icon, color: accentColor, size: 22),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: theme.textTheme.titleSmall),
                  Text(
                    summary,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            Switch(
              value: enabled,
              // Flipping on without permission asks for it first; the switch
              // only moves if it is granted.
              onChanged: (value) => value && !hasNotificationPermission
                  ? onRequestPermission()
                  : onToggle(value),
            ),
          ],
        ),
        if (enabled && !hasNotificationPermission)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: OutlinedButton(
              onPressed: onRequestPermission,
              child: Text(l10n.actionGrantPermission),
            ),
          ),
      ],
    );
  }
}

/// A labelled value row with decrement / increment buttons, used for a
/// reminder's interval. Port of the Kotlin `HydrationReminderIntervalRow`.
class ReminderStepperRow extends StatelessWidget {
  const ReminderStepperRow({
    super.key,
    required this.label,
    required this.value,
    required this.onDecrease,
    required this.onIncrease,
    required this.decreaseTooltip,
    required this.increaseTooltip,
  });

  final String label;
  final String value;

  /// Null disables the button — the value is at its bound.
  final VoidCallback? onDecrease;
  final VoidCallback? onIncrease;
  final String decreaseTooltip;
  final String increaseTooltip;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Icon(Icons.schedule,
            size: 20, color: theme.colorScheme.onSurfaceVariant),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: theme.textTheme.labelMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              Text(value, style: theme.textTheme.bodyMedium),
            ],
          ),
        ),
        IconButton(
          onPressed: onDecrease,
          tooltip: decreaseTooltip,
          icon: const Icon(Icons.remove),
        ),
        IconButton(
          onPressed: onIncrease,
          tooltip: increaseTooltip,
          icon: const Icon(Icons.add),
        ),
      ],
    );
  }
}

/// A labelled time that opens a time picker when tapped. Port of the Kotlin
/// `HydrationReminderTimeRow`.
class ReminderTimeRow extends StatelessWidget {
  const ReminderTimeRow({
    super.key,
    required this.label,
    required this.value,
    required this.onTap,
  });

  final String label;
  final String value;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            Icon(Icons.schedule,
                size: 20, color: theme.colorScheme.onSurfaceVariant),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: theme.textTheme.labelMedium
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                  Text(value, style: theme.textTheme.bodyMedium),
                ],
              ),
            ),
            Icon(Icons.edit_outlined,
                size: 18, color: theme.colorScheme.onSurfaceVariant),
          ],
        ),
      ),
    );
  }
}
