import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/presentation/command_state.dart';
import '../../../../domain/model/health_connect_availability.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../../application/permission_categories_view_model.dart';
import '../settings_error_text.dart';

/// The per-category Health Connect permission breakdown. Self-contained port of
/// the Kotlin `PermissionCategoryCard` list rendered by `SettingsScreenContent`
/// for the Health Connect section: each category shows its title, a
/// granted/partial/optional/manual status, an optional description and a
/// Grant/Review/Open action.
///
/// The taxonomy and the grant action live in [PermissionCategoriesViewModel];
/// this card only renders its state and calls its intents.
class PermissionCategoriesCard extends ConsumerWidget {
  const PermissionCategoriesCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(permissionCategoriesProvider);
    final availability = state.availability;
    final granted = state.granted;

    if (availability == null || granted == null) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        child: _CheckingCard(),
      );
    }

    final theme = Theme.of(context);
    final isRequesting = state.request is CommandRunning<void>;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (state.request case CommandFailure<void>(:final error))
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Text(
              settingsErrorText(error),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.error),
            ),
          ),
        for (final category in state.categories)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: _PermissionCategoryTile(
              category: category,
              granted: granted,
              availability: availability,
              isRequesting: isRequesting,
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
    required this.isRequesting,
  });

  final PermissionCategory category;
  final Set<String> granted;
  final HealthConnectAvailability availability;

  /// A grant is already in flight (in any category) — the Kotlin launcher is
  /// single-shot, so every Grant button waits for it.
  final bool isRequesting;

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
                  onPressed: availability ==
                              HealthConnectAvailability.available &&
                          !isRequesting
                      ? () => ref
                          .read(permissionCategoriesProvider.notifier)
                          .requestCategory(
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
