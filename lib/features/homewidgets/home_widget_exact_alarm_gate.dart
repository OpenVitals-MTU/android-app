import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../di/providers.dart';
import '../../l10n/app_localizations.dart';
import 'home_widget_alarm.dart';
import 'home_widget_configure.dart';

/// The add-time exact-alarm gate for the zero-config status widgets (Body
/// Energy, Daily Readiness, Today Vitals).
///
/// The widgets' 30-minute refresh chain is deferred to Doze maintenance
/// windows — hours apart overnight — unless it may arm exact, which is what
/// froze a freshly-placed widget on its pre-dawn snapshot every morning. This
/// screen is the one chance to ask at the moment the user demonstrably cares
/// about widgets: when they add one.
///
/// Contract with the launcher (via [HomeWidgetConfigureChannel.finish]):
/// * Permission already granted → finish immediately; the user never sees this
///   screen beyond a frame of scaffold.
/// * "Allow" → the system settings screen opens; on return with the grant, the
///   refresh chain is re-armed (now exact) and the configure finishes.
/// * "Not now" → finish anyway. Declining the permission must never cost the
///   user the widget; only backing out cancels the placement.
/// * ANY failure → finish. A crash here would make the widget un-addable.
class HomeWidgetExactAlarmGateScreen extends ConsumerStatefulWidget {
  const HomeWidgetExactAlarmGateScreen({
    super.key,
    required this.appWidgetId,
    this.rearmRefresh = scheduleHomeWidgetRefresh,
  });

  final int appWidgetId;

  /// Re-arms the widget refresh alarm after a grant, so the chain switches to
  /// exact immediately instead of on its next natural fire. Injectable so the
  /// widget test does not touch the real alarm manager.
  final Future<void> Function() rearmRefresh;

  @override
  ConsumerState<HomeWidgetExactAlarmGateScreen> createState() =>
      _HomeWidgetExactAlarmGateScreenState();
}

class _HomeWidgetExactAlarmGateScreenState
    extends ConsumerState<HomeWidgetExactAlarmGateScreen>
    with WidgetsBindingObserver {
  /// Null while the initial permission check runs; the screen renders nothing
  /// but scaffold until it resolves, so a granted user never sees the prompt.
  bool? _needsPermission;
  bool _finishing = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _checkAndMaybeFinish();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// The system settings screen opens over this activity; when the user comes
  /// back, re-check — a grant out there finishes the configure here.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _needsPermission == true) {
      _checkAndMaybeFinish(rearmOnGrant: true);
    }
  }

  Future<void> _checkAndMaybeFinish({bool rearmOnGrant = false}) async {
    try {
      final granted = await ref
          .read(reminderNotificationPermissionsProvider)
          .canScheduleExact();
      if (!mounted) return;
      if (granted) {
        if (rearmOnGrant) await widget.rearmRefresh();
        await _finish();
      } else {
        setState(() => _needsPermission = true);
      }
    } catch (_) {
      // Never block a widget placement on a permission check.
      await _finish();
    }
  }

  Future<void> _requestPermission() async {
    try {
      final granted = await ref
          .read(reminderNotificationPermissionsProvider)
          .requestExactAlarms();
      if (!mounted) return;
      if (granted) {
        await widget.rearmRefresh();
        await _finish();
      }
      // Not granted (yet): the settings screen is likely open on top; the
      // lifecycle observer re-checks when the user returns.
    } catch (_) {
      await _finish();
    }
  }

  Future<void> _finish() async {
    if (_finishing) return;
    _finishing = true;
    await ref
        .read(homeWidgetConfigureChannelProvider)
        .finish(widget.appWidgetId);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: _needsPermission != true
            ? const SizedBox.shrink()
            : Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 480),
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Icon(Icons.update,
                            size: 48, color: theme.colorScheme.primary),
                        const SizedBox(height: 16),
                        Text(
                          l10n.homeWidgetExactAlarmTitle,
                          style: theme.textTheme.headlineSmall,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          l10n.homeWidgetExactAlarmBody,
                          style: theme.textTheme.bodyMedium,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 24),
                        FilledButton(
                          onPressed: _requestPermission,
                          child: Text(l10n.homeWidgetExactAlarmAllow),
                        ),
                        const SizedBox(height: 8),
                        TextButton(
                          onPressed: _finish,
                          child: Text(l10n.homeWidgetExactAlarmNotNow),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
      ),
    );
  }
}

/// The inline exact-alarm nudge for the metric and beverage pickers — the same
/// ask as [HomeWidgetExactAlarmGateScreen], compressed into a card above the
/// picker list. Renders nothing while the permission check runs or once the
/// permission is granted (including a grant made from this card's button).
class HomeWidgetExactAlarmCard extends ConsumerStatefulWidget {
  const HomeWidgetExactAlarmCard({super.key});

  @override
  ConsumerState<HomeWidgetExactAlarmCard> createState() =>
      _HomeWidgetExactAlarmCardState();
}

class _HomeWidgetExactAlarmCardState
    extends ConsumerState<HomeWidgetExactAlarmCard>
    with WidgetsBindingObserver {
  bool _needsPermission = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _check();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _needsPermission) _check();
  }

  Future<void> _check() async {
    try {
      final granted = await ref
          .read(reminderNotificationPermissionsProvider)
          .canScheduleExact();
      if (mounted) setState(() => _needsPermission = !granted);
    } catch (_) {
      // On any doubt, no nudge.
      if (mounted) setState(() => _needsPermission = false);
    }
  }

  Future<void> _request() async {
    try {
      final granted = await ref
          .read(reminderNotificationPermissionsProvider)
          .requestExactAlarms();
      if (mounted && granted) setState(() => _needsPermission = false);
      // Otherwise the settings screen is open; the lifecycle re-check hides
      // the card if the user grants out there.
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    if (!_needsPermission) return const SizedBox.shrink();
    final l10n = AppLocalizations.of(context);
    return Card(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 8, 4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.homeWidgetExactAlarmBody,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            Align(
              alignment: AlignmentDirectional.centerEnd,
              child: TextButton(
                onPressed: _request,
                child: Text(l10n.homeWidgetExactAlarmAllow),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
