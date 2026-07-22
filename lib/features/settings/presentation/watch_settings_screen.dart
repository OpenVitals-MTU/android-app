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
              if (!screen.hasState)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                  child: Text(
                    l10n.settingsWatchSettingsNoState,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: Theme.of(context).colorScheme.error,
                        ),
                  ),
                ),
              // Blank rows are dropped: an alarm list reserves one per slot and
              // leaves the unused ones with no title at all, which drew twenty
              // empty cards under a single real alarm.
              for (final entry in screen.entries)
                if (!entry.isBlank)
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
          onTap: () => _openSubscreen(entry.subscreenId!, title),
        ));

      case GarminEntryKind.options:
        // Only offered when the watch actually sent choices. An empty list here
        // would open a dialog with nothing in it.
        final canChoose = entry.options.isNotEmpty && !_busy;
        return card(ListTile(
          title: Text(title),
          subtitle: entry.summary == null ? null : Text(entry.summary!),
          trailing: _busy
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.chevron_right),
          enabled: canChoose,
          onTap: canChoose ? _chooseOption : null,
        ));

      case GarminEntryKind.time:
        return card(ListTile(
          title: Text(title),
          subtitle: entry.summary == null ? null : Text(entry.summary!),
          trailing: _busy
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.schedule),
          enabled: !_busy,
          onTap: _busy ? null : _chooseTime,
        ));

      case GarminEntryKind.number:
        // The watch bounds these itself and does not send the bounds, so a
        // picker here could offer a value it will refuse. Left readable until
        // the limits are known.
        final value = [
          if (entry.summary != null && entry.summary!.isNotEmpty) entry.summary!,
          if (entry.unit != null && entry.unit!.isNotEmpty) entry.unit!,
        ].join(' ');
        return card(ListTile(
          title: Text(title),
          subtitle: value.isEmpty ? null : Text(value),
          enabled: false,
        ));

      case GarminEntryKind.action:
        // A button the WATCH put here, run under the watch's own label. Asked
        // first, always: an action row has no value to inspect beforehand and
        // no way to undo afterwards.
        return card(ListTile(
          title: Text(
            title,
            style: TextStyle(color: theme.colorScheme.error),
          ),
          trailing: _busy
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : null,
          enabled: !_busy,
          onTap: _busy ? null : _runAction,
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

  /// Walks into a subscreen, and re-reads THIS one on the way back.
  ///
  /// What happens in there changes what belongs here: "Add Alarm" opens a
  /// screen that creates an alarm, and returning to a list still showing the
  /// rows from before it existed makes the watch and the phone disagree about
  /// something the person just did.
  Future<void> _openSubscreen(int subscreenId, String title) async {
    await context.push(
      AppRoutes.watchSettingsLocation(widget.deviceId, subscreenId),
      extra: title,
    );
    if (!mounted) return;
    ref.invalidate(watchSettingsScreenProvider(widget.target));
  }

  /// Offers the options the WATCH sent, and applies the one chosen.
  Future<void> _chooseOption() async {
    final entry = widget.entry;
    final chosen = await showDialog<GarminSettingsOption>(
      context: context,
      builder: (context) => SimpleDialog(
        title: Text(entry.title ?? ''),
        children: [
          for (final option in entry.options)
            SimpleDialogOption(
              onPressed: () => Navigator.of(context).pop(option),
              child: Row(
                children: [
                  // By POSITION, which is what the watch actually reports.
                  // Matching the summary text against the titles held up until
                  // a screen arrived with an empty summary, and then nothing
                  // looked selected at all.
                  Icon(
                    option.index == entry.selectedIndex
                        ? Icons.radio_button_checked
                        : Icons.radio_button_unchecked,
                    size: 20,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                  const SizedBox(width: 12),
                  Expanded(child: Text(option.title)),
                ],
              ),
            ),
        ],
      ),
    );
    if (chosen == null || !mounted) return;
    await _apply((ref) => setWatchOption(
          ref,
          widget.target,
          entry.id,
          chosen.index,
        ));
  }

  /// Runs an action row, after asking.
  Future<void> _runAction() async {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final title = widget.entry.title ?? '';
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        // The watch's own wording, not ours: this app does not know what the
        // row does beyond what the watch called it, and inventing a friendlier
        // description would be claiming knowledge it does not have.
        title: Text(l10n.settingsWatchSettingsConfirmAction(title)),
        content: Text(l10n.settingsWatchSettingsConfirmActionBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(l10n.actionCancel),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style:
                TextButton.styleFrom(foregroundColor: theme.colorScheme.error),
            child: Text(title),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    final result =
        await _apply((ref) => deleteWatchEntry(ref, widget.target, widget.entry.id));
    // The screen this row sat on described the thing just deleted, so staying
    // on it would leave a page for something that no longer exists — and the
    // watch answers a dead screen's id with its parent's contents. Back out to
    // the list, which re-reads itself.
    if (result == WatchSettingsChangeResult.applied &&
        mounted &&
        context.canPop()) {
      context.pop();
    }
  }

  Future<void> _chooseTime() async {
    // Opened at the watch's OWN time. Starting from "now" meant every edit
    // began at the wrong number, so nudging an alarm by ten minutes actually
    // reset it to whenever you happened to open the picker.
    final current = widget.entry.time;
    final picked = await showTimePicker(
      context: context,
      initialTime: current == null
          ? TimeOfDay.now()
          : TimeOfDay(
              hour: current.inHours % 24,
              minute: current.inMinutes % 60,
            ),
      helpText: widget.entry.title,
    );
    if (picked == null || !mounted) return;
    await _apply((ref) => setWatchTime(
          ref,
          widget.target,
          widget.entry.id,
          Duration(hours: picked.hour, minutes: picked.minute),
        ));
  }

  Future<void> _setSwitch(bool value) async {
    await _apply((ref) => setWatchSwitch(
          ref,
          widget.target,
          widget.entry.id,
          value,
        ));
  }

  /// Runs one change and reports what the watch made of it.
  Future<WatchSettingsChangeResult> _apply(
    Future<WatchSettingsChangeResult> Function(WidgetRef) change,
  ) async {
    final l10n = AppLocalizations.of(context);
    final messenger = ScaffoldMessenger.of(context);
    setState(() => _busy = true);
    final result = await change(ref);
    if (!mounted) return result;
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
    return result;
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
