import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../di/providers.dart';
import '../../../../domain/model/health_connect_availability.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../../ui/components/health_connect_gate.dart';

/// A single Health Connect permission category, mirroring the Kotlin
/// `SettingsPermissionCategory` data class. Carries the permission strings that
/// make up the category plus the subset that can only be granted manually
/// (via the Health Connect settings screen rather than the runtime dialog).
class PermissionCategory {
  const PermissionCategory({
    required this.id,
    required this.permissions,
    this.manualPermissions = const <String>{},
    this.available = true,
  });

  final String id;
  final Set<String> permissions;
  final Set<String> manualPermissions;

  /// False when the installed Health Connect version cannot support the
  /// category (e.g. mindfulness on an older provider).
  final bool available;
}

/// Builds the settings permission categories from the [HealthRepository]
/// permission taxonomy. Faithful port of `SettingsViewModel.permissionCategories`
/// — same ids, same membership, same `isNotEmpty` filter and mindfulness gate.
final permissionCategoriesProvider = Provider<List<PermissionCategory>>((ref) {
  final repo = ref.watch(healthRepositoryProvider);
  final availability = ref.watch(healthConnectAvailabilityProvider).value;
  final mindfulnessAvailable =
      availability == HealthConnectAvailability.available &&
          repo.isMindfulnessAvailable();

  return <PermissionCategory>[
    PermissionCategory(id: 'activity_sleep', permissions: repo.corePermissions),
    PermissionCategory(id: 'heart_recovery', permissions: repo.heartPermissions),
    PermissionCategory(id: 'body', permissions: repo.bodyPermissions),
    PermissionCategory(
      id: 'activity_extras',
      permissions: repo.activityExtrasPermissions,
    ),
    PermissionCategory(
      id: 'nutrition_hydration',
      permissions: repo.nutritionHydrationPermissions,
    ),
    PermissionCategory(
      id: 'manual_entry_write',
      permissions: repo.requestableWritePermissions,
    ),
    PermissionCategory(
      id: 'mindfulness',
      permissions: repo.mindfulnessPermissions,
      available: mindfulnessAvailable,
    ),
    PermissionCategory(
      id: 'additional_data_access',
      permissions: {
        ...repo.additionalDataAccessPermissions,
        ...repo.routePermissions,
      },
      manualPermissions: repo.routePermissions,
    ),
    PermissionCategory(id: 'vitals', permissions: repo.vitalsPermissions),
    PermissionCategory(
      id: 'cycle_tracking',
      permissions: repo.cyclePermissions,
    ),
    // Drop a category with nothing to show — but KEEP one that is explicitly
    // unavailable, so Settings can still say "Not supported" with a reason
    // (Kotlin's SettingsViewModel does not filter these out; only *onboarding*
    // does). Since Kotlin 1.9.0 (1f2b435) `mindfulnessPermissions` is correctly
    // empty on a provider that lacks mindfulness, so filtering on emptiness
    // alone would silently delete that row.
  ].where((c) => c.permissions.isNotEmpty || !c.available).toList();
});

/// The per-category Health Connect permission breakdown. Self-contained port of
/// the Kotlin `PermissionCategoryCard` list rendered by `SettingsScreenContent`
/// for the Health Connect section: each category shows its title, a
/// granted/partial/optional/manual status, an optional description and a
/// Grant/Review/Open action.
class PermissionCategoriesCard extends ConsumerWidget {
  const PermissionCategoriesCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final availability = ref.watch(healthConnectAvailabilityProvider).value;
    final granted = ref.watch(grantedHealthPermissionsProvider).value;
    final categories = ref.watch(permissionCategoriesProvider);

    if (availability == null || granted == null) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        child: _CheckingCard(),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final category in categories)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: _PermissionCategoryTile(
              category: category,
              granted: granted,
              availability: availability,
            ),
          ),
      ],
    );
  }
}

class _CheckingCard extends StatelessWidget {
  const _CheckingCard();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Text(
          'Checking Health Connect access…',
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ),
    );
  }
}

class _PermissionCategoryTile extends ConsumerWidget {
  const _PermissionCategoryTile({
    required this.category,
    required this.granted,
    required this.availability,
  });

  final PermissionCategory category;
  final Set<String> granted;
  final HealthConnectAvailability availability;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final grantedCount =
        category.permissions.where(granted.contains).length;
    final isGranted =
        category.available && grantedCount == category.permissions.length;
    final partial = category.available && grantedCount > 0 && !isGranted;

    final missing = category.permissions.difference(granted);
    final missingRequestable = missing.difference(category.manualPermissions);
    final missingManual = missing.intersection(category.manualPermissions);
    final isManualGrant =
        missingRequestable.isEmpty && missingManual.isNotEmpty;

    final String status;
    if (!category.available) {
      status = l10n.onboardingStatusNotSupported;
    } else if (isGranted) {
      status = l10n.onboardingStatusGranted;
    } else if (partial) {
      status = l10n.onboardingStatusPartiallyGranted(
        grantedCount,
        category.permissions.length,
      );
    } else if (isManualGrant) {
      status = l10n.onboardingStatusManual;
    } else {
      status = l10n.onboardingStatusOptional;
    }

    final baseDescription = _description(l10n, category.id);
    final String description;
    if (!category.available) {
      description = _unavailableReason(l10n, category.id) ?? baseDescription;
    } else if (category.manualPermissions.isNotEmpty &&
        missingManual.isNotEmpty) {
      description =
          l10n.onboardingCategoryAdditionalDataAccessManualNote(baseDescription);
    } else {
      description = baseDescription;
    }

    return OpenVitalsCard(
      color: isGranted
          ? theme.colorScheme.primaryContainer.withValues(alpha: 0.4)
          : theme.colorScheme.surfaceContainer,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _title(l10n, category.id),
                        style: theme.textTheme.bodyMedium,
                      ),
                      Text(
                        status,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: isGranted
                              ? theme.colorScheme.primary
                              : theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                if (isGranted)
                  Padding(
                    padding: const EdgeInsets.only(left: 12),
                    child: Icon(
                      Icons.check_circle_outline,
                      color: theme.colorScheme.primary,
                      semanticLabel: l10n.onboardingStatusGranted,
                    ),
                  )
                else if (!category.available)
                  Padding(
                    padding: const EdgeInsets.only(left: 12),
                    child: Icon(
                      Icons.lock_outline,
                      color: theme.colorScheme.onSurfaceVariant,
                      semanticLabel: l10n.onboardingStatusNotSupported,
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              description,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            if (!isGranted && category.available) ...[
              const SizedBox(height: 10),
              Align(
                alignment: Alignment.centerRight,
                child: FilledButton.tonal(
                  onPressed:
                      availability == HealthConnectAvailability.available
                          ? () => _onGrant(
                                ref,
                                requestable: missingRequestable,
                                manual: missingManual,
                              )
                          : null,
                  child: Text(
                    isManualGrant
                        ? l10n.actionOpen
                        : partial
                            ? l10n.actionReview
                            : l10n.actionGrant,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _onGrant(
    WidgetRef ref, {
    required Set<String> requestable,
    required Set<String> manual,
  }) async {
    final repo = ref.read(healthRepositoryProvider);
    if (requestable.isNotEmpty) {
      await repo.requestPermissions(requestable);
    } else if (manual.isNotEmpty) {
      await repo.openHealthConnectSettings();
    }
    ref.invalidate(grantedHealthPermissionsProvider);
  }

  String _title(AppLocalizations l10n, String id) => switch (id) {
        'activity_sleep' => l10n.onboardingCategoryActivitySleep,
        'heart_recovery' => l10n.onboardingCategoryHeartRecovery,
        'body' => l10n.onboardingCategoryBody,
        'activity_extras' => l10n.onboardingCategoryActivityExtras,
        'nutrition_hydration' => l10n.onboardingCategoryNutritionHydration,
        'manual_entry_write' => l10n.onboardingCategoryManualEntryWrite,
        'mindfulness' => l10n.onboardingCategoryMindfulness,
        'additional_data_access' => l10n.onboardingCategoryAdditionalDataAccess,
        'vitals' => l10n.onboardingCategoryVitals,
        'cycle_tracking' => l10n.onboardingCategoryCycleTracking,
        _ => id,
      };

  String _description(AppLocalizations l10n, String id) => switch (id) {
        'activity_sleep' => l10n.onboardingCategoryActivitySleepDesc,
        'heart_recovery' => l10n.onboardingCategoryHeartRecoveryDesc,
        'body' => l10n.onboardingCategoryBodyDesc,
        'activity_extras' => l10n.onboardingCategoryActivityExtrasDesc,
        'nutrition_hydration' => l10n.onboardingCategoryNutritionHydrationDesc,
        'manual_entry_write' => l10n.onboardingCategoryManualEntryWriteDesc,
        'mindfulness' => l10n.onboardingCategoryMindfulnessDesc,
        'additional_data_access' =>
          l10n.onboardingCategoryAdditionalDataAccessDesc,
        'vitals' => l10n.onboardingCategoryVitalsDesc,
        'cycle_tracking' => l10n.onboardingCategoryCycleTrackingDesc,
        _ => '',
      };

  String? _unavailableReason(AppLocalizations l10n, String id) => switch (id) {
        'mindfulness' => l10n.onboardingCategoryMindfulnessUnavailable,
        _ => null,
      };
}
