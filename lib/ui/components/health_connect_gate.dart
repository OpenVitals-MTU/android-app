import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../di/providers.dart';
import '../../domain/model/health_connect_availability.dart';
import 'loading_state.dart';
import 'ov_card.dart';

/// The current Health Connect / HealthKit availability, resolved from the
/// platform data source. Overridable in tests.
///
/// Goes through [HealthRepository.refreshAvailability] rather than the data
/// source directly so the optional-feature flags and the provider's supported
/// permission set are resolved on every launch, not only the one that runs
/// onboarding. Permission sets are derived from those, so leaving them at their
/// defaults makes every consumer require permissions the device cannot grant.
final healthConnectAvailabilityProvider =
    FutureProvider<HealthConnectAvailability>((ref) async {
  return ref.watch(healthRepositoryProvider).refreshAvailability();
});

/// The set of currently granted health permissions. Overridable in tests.
final grantedHealthPermissionsProvider =
    FutureProvider<Set<String>>((ref) async {
  return ref.watch(healthRepositoryProvider).grantedPermissions();
});

/// Whether background Health Connect sync is enabled (a user preference).
/// Reactive: re-reads when the preference changes.
final healthConnectSyncEnabledProvider = Provider<bool>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  final listenable = repo.healthConnectSyncEnabledListenable;
  void listener() => ref.invalidateSelf();
  listenable.addListener(listener);
  ref.onDispose(() => listenable.removeListener(listener));
  return listenable.value;
});

/// The access-gate variant to show, or null when the child content should be
/// shown. Port of Kotlin `resolveHealthConnectAccessGateMode`, extended with an
/// [HealthConnectGateMode.unavailable] state so the gate also covers the case
/// where Health Connect itself is missing.
///
/// DELIBERATE DEVIATION from the Kotlin app — do not "fix" this back.
/// Where Kotlin keeps the dashboard visible and shows a small inline
/// `DashboardHealthConnectPromo` card, Flutter replaces the whole screen with
/// this gate for the *unavailable* and *sync-paused* states — a stronger, harder
/// -to-ignore treatment. Only Kotlin's third promo variant (Health Connect is
/// available and syncing, but the minimum permissions are missing) is reproduced
/// inline on the dashboard, because the gate does not cover that case. A parity
/// audit will flag the two missing promo variants — that is intentional.
enum HealthConnectGateMode {
  unavailable,
  insufficientAccess,
  doubleCancelRecovery,
  syncPaused,
}

HealthConnectGateMode? resolveHealthConnectGateMode({
  required HealthConnectAvailability availability,
  required bool syncEnabled,
  required Set<String> requiredPermissions,
  required Set<String> grantedPermissions,
  bool showDoubleCancelRecovery = false,
}) {
  if (availability != HealthConnectAvailability.available) {
    return HealthConnectGateMode.unavailable;
  }
  if (!syncEnabled) return HealthConnectGateMode.syncPaused;
  final missing = requiredPermissions.difference(grantedPermissions);
  if (missing.isEmpty) return null;
  return showDoubleCancelRecovery
      ? HealthConnectGateMode.doubleCancelRecovery
      : HealthConnectGateMode.insufficientAccess;
}

/// The Flutter analogue of the Kotlin `WithHealthConnectFeatureScreen` /
/// `HealthConnectAccessGate`. Given the platform availability + granted
/// permission state (read from providers), it renders either an access
/// gate/permission callout, or the [child] (optionally topped with a sync
/// banner). A [ConsumerWidget] so it reads the health/permission providers.
class HealthConnectGate extends ConsumerWidget {
  const HealthConnectGate({
    super.key,
    required this.child,
    this.requiredPermissions = const <String>{},
    this.showInlineSyncBanner = true,
    this.showDoubleCancelRecovery = false,
    this.syncInProgress = false,
    this.onGrant,
    this.onOpenSettings,
  });

  final Widget child;
  final Set<String> requiredPermissions;
  final bool showInlineSyncBanner;
  final bool showDoubleCancelRecovery;
  final bool syncInProgress;

  /// Overrides the default grant action (which requests [requiredPermissions]
  /// and refreshes the permission/availability providers).
  final Future<void> Function()? onGrant;

  /// Overrides the "open Health Connect settings" action.
  final VoidCallback? onOpenSettings;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final availability =
        ref.watch(healthConnectAvailabilityProvider).value;
    final granted = ref.watch(grantedHealthPermissionsProvider).value;
    final syncEnabled = ref.watch(healthConnectSyncEnabledProvider);

    if (availability == null || granted == null) {
      return const FullScreenLoading();
    }

    final mode = resolveHealthConnectGateMode(
      availability: availability,
      syncEnabled: syncEnabled,
      requiredPermissions: requiredPermissions,
      grantedPermissions: granted,
      showDoubleCancelRecovery: showDoubleCancelRecovery,
    );

    if (mode == null) {
      if (showInlineSyncBanner && syncInProgress) {
        return Column(
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: HealthConnectSyncStatusBanner(syncInProgress: true),
            ),
            Expanded(child: child),
          ],
        );
      }
      return child;
    }

    return _AccessGate(
      mode: mode,
      onGrant: () => _handleGrant(ref),
      onOpenSettings: onOpenSettings ?? () => _handleGrant(ref),
    );
  }

  Future<void> _handleGrant(WidgetRef ref) async {
    if (onGrant != null) {
      await onGrant!();
      return;
    }
    if (requiredPermissions.isNotEmpty) {
      await ref.read(healthRepositoryProvider).requestPermissions(requiredPermissions);
    }
    ref.invalidate(grantedHealthPermissionsProvider);
    ref.invalidate(healthConnectAvailabilityProvider);
  }
}

class _GateCopy {
  const _GateCopy({
    required this.title,
    required this.body,
    required this.action,
    required this.icon,
    required this.usesGrant,
  });

  final String title;
  final String body;
  final String action;
  final IconData icon;
  final bool usesGrant;
}

_GateCopy _copyFor(HealthConnectGateMode mode) {
  switch (mode) {
    case HealthConnectGateMode.unavailable:
      return const _GateCopy(
        title: 'Health Connect unavailable',
        body:
            'Health Connect is not available on this device. Install or update '
            'it to sync your health data.',
        action: 'Set up Health Connect',
        icon: Icons.lock_outline,
        usesGrant: true,
      );
    case HealthConnectGateMode.insufficientAccess:
      return const _GateCopy(
        title: 'Permissions needed',
        body: 'Grant access to your health data to see this screen.',
        action: 'Grant permission',
        icon: Icons.lock_outline,
        usesGrant: true,
      );
    case HealthConnectGateMode.doubleCancelRecovery:
      return const _GateCopy(
        title: 'Manage permissions',
        body:
            'Open Health Connect settings to grant the required permissions.',
        action: 'Open settings',
        icon: Icons.lock_outline,
        usesGrant: false,
      );
    case HealthConnectGateMode.syncPaused:
      return const _GateCopy(
        title: 'Sync paused',
        body:
            'Health Connect sync is paused. Resume it to see up-to-date data.',
        action: 'Manage access',
        icon: Icons.health_and_safety_outlined,
        usesGrant: false,
      );
  }
}

class _AccessGate extends StatelessWidget {
  const _AccessGate({
    required this.mode,
    required this.onGrant,
    required this.onOpenSettings,
  });

  final HealthConnectGateMode mode;
  final VoidCallback onGrant;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final copy = _copyFor(mode);
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Icon(copy.icon, color: scheme.primary, size: 40),
          const SizedBox(height: 16),
          Text(
            copy.title,
            style: theme.textTheme.titleLarge,
            textAlign: TextAlign.center,
          ),
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              copy.body,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: scheme.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(top: 24),
            child: FilledButton(
              onPressed: copy.usesGrant ? onGrant : onOpenSettings,
              child: Text(copy.action),
            ),
          ),
        ],
      ),
    );
  }
}

/// The sync-status banner shown above period content. Port of Kotlin
/// `HealthConnectSyncStatusBanner`.
class HealthConnectSyncStatusBanner extends StatelessWidget {
  const HealthConnectSyncStatusBanner({
    super.key,
    this.syncPaused = false,
    this.syncInProgress = false,
  });

  final bool syncPaused;
  final bool syncInProgress;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final String text;
    final Color color;
    if (syncPaused) {
      text = 'Health Connect sync is paused';
      color = scheme.onSurfaceVariant;
    } else if (syncInProgress) {
      text = 'Syncing with Health Connect…';
      color = scheme.primary;
    } else {
      return const SizedBox.shrink();
    }
    return OpenVitalsCard(
      color: scheme.surfaceContainerHigh,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        child: Semantics(
          label: text,
          child: Text(
            text,
            style: theme.textTheme.bodySmall?.copyWith(color: color),
          ),
        ),
      ),
    );
  }
}
