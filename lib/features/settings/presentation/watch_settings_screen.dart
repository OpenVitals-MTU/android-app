import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../data/source/sensors/garmin/garmin_settings_screen.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../application/watch_settings_view_model.dart';

/// One screen of the watch's own settings, rendered from what the watch sent.
///
/// The app defines none of this. Titles, option lists and the order of rows all
/// come from the watch, already translated into the locale it was handed — so
/// this widget's whole job is to turn declared controls into real ones and to be
/// honest when it cannot.
class WatchSettingsScreenPage extends ConsumerWidget {
  const WatchSettingsScreenPage({
    required this.deviceId,
    required this.screenId,
    this.titleOverride,
    super.key,
  });

  final String deviceId;
  final int screenId;

  /// Shown until the watch supplies its own title, so the app bar is not blank
  /// for the second or two a screen takes to arrive.
  final String? titleOverride;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final target = WatchSettingsTarget(deviceId: deviceId, screenId: screenId);
    final async = ref.watch(watchSettingsScreenProvider(target));

    return Scaffold(
      appBar: AppBar(
        title: Text(
          async.asData?.value?.title ?? titleOverride ?? l10n.settingsWatchSettingsSection,
        ),
      ),
      body: async.when(
        loading: () => _Message(text: l10n.settingsWatchSettingsReading, busy: true),
        error: (_, _) => _Message(
          text: l10n.settingsWatchSettingsUnreachable,
          onRetry: () => ref.invalidate(watchSettingsLinkProvider(deviceId)),
          retryLabel: l10n.settingsWatchSettingsRetry,
        ),
        data: (screen) {
          if (screen == null || screen.isEmpty) {
            return _Message(
              text: l10n.settingsWatchSettingsEmpty,
              onRetry: () => ref.invalidate(watchSettingsScreenProvider(target)),
              retryLabel: l10n.settingsWatchSettingsRetry,
            );
          }
          return ListView(
            padding: screenScrollPadding(context),
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                child: Text(
                  l10n.settingsWatchSettingsFromWatch,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                ),
              ),
              for (final entry in screen.entries)
                _EntryRow(deviceId: deviceId, target: target, entry: entry),
            ],
          );
        },
      ),
    );
  }
}

class _EntryRow extends ConsumerStatefulWidget {
  const _EntryRow({
    required this.deviceId,
    required this.target,
    required this.entry,
  });

  final String deviceId;
  final WatchSettingsTarget target;
  final GarminSettingsEntry entry;

  @override
  ConsumerState<_EntryRow> createState() => _EntryRowState();
}

class _EntryRowState extends ConsumerState<_EntryRow> {
  bool _busy = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final entry = widget.entry;
    final title = entry.title ?? '';

    Widget card(Widget child) => Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Card(child: child),
        );

    switch (entry.kind) {
      case GarminEntryKind.toggle:
        return card(SwitchListTile(
          title: Text(title),
          subtitle: entry.summary == null ? null : Text(entry.summary!),
          value: entry.switchedOn ?? false,
          // Disabled while in flight rather than optimistic: the value shown is
          // the watch's, and a switch that flips before the watch agrees would
          // be reporting a request as a result.
          onChanged: _busy ? null : (value) => _setSwitch(value),
          secondary: _busy
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : null,
        ));

      case GarminEntryKind.subscreen:
        return card(ListTile(
          title: Text(title),
          subtitle: entry.summary == null ? null : Text(entry.summary!),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => context.push(
            AppRoutes.watchSettingsLocation(
              widget.deviceId,
              entry.subscreenId!,
            ),
            extra: title,
          ),
        ));

      case GarminEntryKind.options:
      case GarminEntryKind.time:
      case GarminEntryKind.number:
        // Readable now, not yet editable — a picker for each is the next piece.
        // Shown with its current value rather than hidden: knowing an alarm
        // repeats on weekdays is useful even before it can be changed here.
        return card(ListTile(
          title: Text(title),
          subtitle: entry.summary == null ? null : Text(entry.summary!),
          enabled: false,
        ));

      case GarminEntryKind.inert:
        // Present on the watch, not actionable from a phone. Dimmed rather than
        // dropped, so the screen still matches what is on the wrist.
        return card(ListTile(
          title: Text(
            title,
            style: TextStyle(color: theme.colorScheme.onSurfaceVariant),
          ),
          subtitle: entry.summary == null ? null : Text(entry.summary!),
          enabled: false,
        ));
    }
  }

  Future<void> _setSwitch(bool value) async {
    final l10n = AppLocalizations.of(context);
    final messenger = ScaffoldMessenger.of(context);
    setState(() => _busy = true);
    final result = await setWatchSwitch(
      ref,
      widget.target,
      widget.entry.id,
      value,
    );
    if (!mounted) return;
    setState(() => _busy = false);
    switch (result) {
      case WatchSettingsChangeResult.applied:
        break;
      case WatchSettingsChangeResult.refused:
        messenger.showSnackBar(
          SnackBar(content: Text(l10n.settingsWatchSettingsRefused)),
        );
      case WatchSettingsChangeResult.unanswered:
        // Not "it failed": the request may have landed. Saying so, and pointing
        // at the watch, is the only honest report.
        messenger.showSnackBar(
          SnackBar(content: Text(l10n.settingsWatchSettingsUnanswered)),
        );
    }
  }
}

class _Message extends StatelessWidget {
  const _Message({
    required this.text,
    this.busy = false,
    this.onRetry,
    this.retryLabel,
  });

  final String text;
  final bool busy;
  final VoidCallback? onRetry;
  final String? retryLabel;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (busy) ...[
              const CircularProgressIndicator(),
              const SizedBox(height: 16),
            ],
            Text(
              text,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 12),
              FilledButton.tonal(
                onPressed: onRetry,
                child: Text(retryLabel ?? ''),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
