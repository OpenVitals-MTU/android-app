import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../l10n/app_localizations.dart';

/// The pieces the device view and the watch-data screen both use, so the two
/// cannot drift into looking like different features.

/// The round glyph used wherever a watch is identified. [icon] overrides the
/// watch face for a non-watch GFDI device — a cycling glyph for an Edge bike
/// computer.
class WatchAvatar extends StatelessWidget {
  const WatchAvatar({this.size = 40, this.icon, super.key});

  final double size;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: theme.colorScheme.secondaryContainer,
      ),
      child: Icon(
        icon ?? Icons.watch_outlined,
        size: size * 0.55,
        color: theme.colorScheme.onSecondaryContainer,
      ),
    );
  }
}

/// One icon action in the device view's action band.
///
/// Actions are icons because they are verbs — things asked of the watch now.
/// Anything that changes what happens NEXT time is a row further down instead.
class WatchAction extends StatelessWidget {
  const WatchAction({
    required this.icon,
    required this.label,
    required this.onPressed,
    this.onLongPress,
    this.busy = false,
    super.key,
  });

  final IconData icon;
  final String label;
  final VoidCallback? onPressed;

  /// Debug-only affordances hang off this; nothing user-facing should.
  final VoidCallback? onLongPress;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final enabled = onPressed != null;
    return Semantics(
      button: true,
      enabled: enabled,
      label: label,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          GestureDetector(
            onLongPress: onLongPress,
            child: IconButton.filledTonal(
            onPressed: onPressed,
            icon: busy
                ? SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: theme.colorScheme.onSecondaryContainer,
                    ),
                  )
                : Icon(icon),
          ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: theme.textTheme.labelSmall?.copyWith(
              color: enabled
                  ? theme.colorScheme.onSurface
                  : theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.6),
            ),
          ),
        ],
      ),
    );
  }
}

/// A label/value row for a stored watch metric, with optional supporting text.
class WatchValueRow extends StatelessWidget {
  const WatchValueRow({
    required this.label,
    required this.value,
    this.supporting,
    this.trailingWidget,
    super.key,
  });

  final String label;
  final String value;
  final String? supporting;
  final Widget? trailingWidget;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final sub = supporting;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(label, style: theme.textTheme.bodyLarge),
                    if (sub != null)
                      Text(
                        sub,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                  ],
                ),
              ),
              trailingWidget ??
                  Text(
                    value,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontFeatures: const [FontFeature.tabularFigures()],
                    ),
                  ),
            ],
          ),
        ),
      ),
    );
  }
}

/// `7h 50m`, `17 min`, `45s` — the coarsest unit that still says something.
///
/// Durations here span three orders of magnitude (time awake in minutes, sleep
/// need in hours), so a single format would either bury the hours or pad the
/// minutes with a pointless `0h`.
String formatWatchDuration(AppLocalizations l10n, Duration duration) {
  final minutes = duration.inMinutes;
  if (minutes >= 60) {
    final hours = minutes ~/ 60;
    final rest = minutes % 60;
    return rest == 0 ? '${hours}h' : '${hours}h ${rest}m';
  }
  if (minutes > 0) return '$minutes min';
  return '${duration.inSeconds}s';
}

/// A sync timestamp as the device view shows it: the time for today, the date
/// once it is older, because "11:35" on its own is a lie after midnight.
String formatWatchSyncTime(BuildContext context, DateTime at) {
  final local = at.toLocal();
  final now = DateTime.now();
  final sameDay = local.year == now.year &&
      local.month == now.month &&
      local.day == now.day;
  final locale = Localizations.localeOf(context).toLanguageTag();
  return sameDay
      ? DateFormat.Hm(locale).format(local)
      : DateFormat.yMMMd(locale).add_Hm().format(local);
}

/// Whole days since [at], for the summary tile's stale state.
int watchDaysSince(DateTime at, DateTime now) {
  final a = DateTime(at.year, at.month, at.day);
  final b = DateTime(now.year, now.month, now.day);
  return b.difference(a).inDays;
}

/// Confirms removing a paired device.
///
/// Both kinds ask, because both lose something the user cannot get back by
/// re-pairing: a sensor loses its capability assignment and wheel size, and a
/// watch additionally drops its Bluetooth bond, its companion association and
/// the record of which files it has already copied. The body says which.
Future<bool> confirmRemoveDevice(
  BuildContext context, {
  required String deviceName,
  required bool isWatch,
}) async {
  final l10n = AppLocalizations.of(context);
  final theme = Theme.of(context);
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(l10n.settingsDeviceRemoveConfirmTitle(deviceName)),
      content: Text(
        isWatch
            ? l10n.settingsWatchRemoveConfirmBody
            : l10n.settingsSensorRemoveConfirmBody,
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: Text(l10n.actionCancel),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).pop(true),
          style: TextButton.styleFrom(foregroundColor: theme.colorScheme.error),
          child: Text(l10n.actionRemove),
        ),
      ],
    ),
  );
  return confirmed ?? false;
}
