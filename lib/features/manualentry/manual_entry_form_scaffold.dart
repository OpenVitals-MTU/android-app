import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/presentation/screen_error.dart';
import '../../l10n/app_localizations.dart';

/// Renders a human-readable message for a [ScreenError], falling back to the
/// localized "unknown error". Port of the Kotlin `ScreenError.resolve()`.
String screenErrorText(ScreenError? error, AppLocalizations l10n) {
  if (error is ScreenErrorMessage) return error.text;
  return l10n.unknownError;
}

/// Signals a successful manual-entry write: shows a confirmation snackbar and,
/// unless [pop] is false, leaves the entry route (the Kotlin `onEntrySaved`
/// navigates back). Safe to call after the write completes on the still-mounted
/// screen context.
///
/// Hydration passes `pop: false` when logging a new drink: the catalog is a
/// place you stay in to log several drinks, and the today counter updates in
/// place. Kotlin pops there too; this is a deliberate divergence.
void onManualEntrySaved(
  BuildContext context,
  String message, {
  bool pop = true,
}) {
  if (!context.mounted) return;
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
  if (pop) Navigator.of(context).maybePop();
}

/// Re-checks the Health Connect write permission whenever the screen regains
/// focus, port of the Kotlin `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)`
/// that every manual-entry screen installs.
///
/// Without it, a user who leaves to grant the permission in Health Connect comes
/// back to a form that still believes it cannot write.
mixin RefreshPermissionOnResume<T extends ConsumerStatefulWidget>
    on ConsumerState<T> {
  late final AppLifecycleListener _manualEntryLifecycle;

  /// Re-reads the write permission. Implementations delegate to their notifier.
  void refreshPermission();

  @override
  void initState() {
    super.initState();
    _manualEntryLifecycle = AppLifecycleListener(onResume: refreshPermission);
  }

  @override
  void dispose() {
    _manualEntryLifecycle.dispose();
    super.dispose();
  }
}
