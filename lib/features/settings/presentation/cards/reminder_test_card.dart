import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../../core/reminders/reminder_notifications.dart';
import '../../../../di/providers.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../hydration/reminders/hydration_reminder_device.dart';
import '../../../hydration/reminders/hydration_reminder_quick_add.dart';

/// Debug-diagnostics "Test reminders" card: posts the hydration reminder
/// notification immediately, built exactly as a scheduled fire builds it —
/// same channel, same live-progress body and bar, same quick-add actions — so
/// the notification (and its one-tap logging) can be exercised without waiting
/// out the reminder interval.
///
/// Only reachable in diagnostics-enabled builds, like the rest of the section
/// (gated on `kDiagnosticsEnabled` in the hub + router).
class ReminderTestCard extends ConsumerWidget {
  const ReminderTestCard({super.key, this.showReminder});

  /// Test seam for posting; defaults to [_defaultShowReminder]. Returns whether
  /// the notification was posted (false = notifications disabled).
  final Future<bool> Function(WidgetRef ref)? showReminder;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Icon(
                      Icons.notification_add_outlined,
                      size: 20,
                      color: theme.colorScheme.primary,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.settingsReminderTestTitle,
                          style: theme.textTheme.titleSmall,
                        ),
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(
                            l10n.settingsReminderTestBody,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: () => _showHydrationReminder(context, ref, l10n),
                    icon: const Icon(Icons.water_drop_outlined, size: 18),
                    label: Text(l10n.settingsReminderTestShowHydration),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showHydrationReminder(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
  ) async {
    String message;
    try {
      final posted = await (showReminder ?? _defaultShowReminder)(ref);
      message = posted
          ? l10n.settingsReminderTestPosted
          : l10n.settingsReminderTestNotificationsDisabled;
    } catch (_) {
      message = l10n.settingsReminderTestFailed;
    }
    if (!context.mounted) return;
    ScaffoldMessenger.maybeOf(context)?.showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  /// The real path: today's progress and the current quick-add sizes, posted
  /// through the same helper the scheduled batch's details come from.
  static Future<bool> _defaultShowReminder(WidgetRef ref) async {
    final plugin = ref.read(flutterLocalNotificationsProvider);
    if (!await areReminderNotificationsEnabled(plugin)) return false;
    final progress =
        await ref.read(hydrationReminderControllerProvider).readProgress();
    await showReminderNotificationNow(
      plugin,
      hydrationReminderNotificationSpec,
      progress: progress,
      actions: hydrationReminderQuickAddActions(
        ref.read(preferencesRepositoryProvider),
      ),
    );
    return true;
  }
}
