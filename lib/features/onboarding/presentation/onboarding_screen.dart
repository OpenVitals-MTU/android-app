import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/external_link.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/app_language_dropdown.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../application/onboarding_view_model.dart';

// File-private constants, mirroring the Kotlin `OnboardingScreen.kt` HC_PACKAGE
// / PLAY_STORE_URL pair used by the "needs provider update" install action.
const _healthConnectPackage = 'com.google.android.apps.healthdata';
const _playStoreUrl =
    'https://play.google.com/store/apps/details?id=$_healthConnectPackage';

/// Onboarding flow, shown as the start destination when onboarding is not yet
/// complete. Rendered full-screen outside the adaptive shell.
///
/// Port of the Kotlin `OnboardingScreen`: a Health Connect availability check,
/// a one-tap "grant all" of the required (minimum) permissions, per-category
/// grant rows, and a Continue action that persists the onboarding-complete pref
/// (via [OnboardingViewModel.completeOnboarding]) before invoking
/// [onOnboardingComplete] so the app routes on to the dashboard.
///
/// All copy is localized one-to-one from the Kotlin `strings.xml` onboarding_*
/// resources via [AppLocalizations].
class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key, this.onOnboardingComplete});

  final VoidCallback? onOnboardingComplete;

  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState lifecycleState) {
    // When the user returns from the Health Connect page (or the permission
    // dialog), re-check what's granted so category rows update to "Granted"
    // without an app restart.
    if (lifecycleState == AppLifecycleState.resumed) {
      ref.read(onboardingProvider.notifier).refreshGrantedPermissions();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(onboardingProvider);
    final notifier = ref.read(onboardingProvider.notifier);

    if (state.isCheckingPermissions) {
      return const Scaffold(body: FullScreenLoading());
    }

    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          child: _OnboardingContent(
            state: state,
            notifier: notifier,
            onComplete: () {
              notifier.completeOnboarding();
              widget.onOnboardingComplete?.call();
            },
          ),
        ),
      ),
    );
  }
}

class _OnboardingContent extends ConsumerWidget {
  const _OnboardingContent({
    required this.state,
    required this.notifier,
    required this.onComplete,
  });

  final OnboardingState state;
  final OnboardingViewModel notifier;
  final VoidCallback onComplete;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    // Kotlin header order (OnboardingScreen.kt:118-150): the language dropdown
    // aligned to the end, then the wide logo, the app name and the tagline.
    // Changing the language re-renders the whole app: `appLanguageProvider`
    // drives `MaterialApp.locale` in app.dart.
    final header = Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: SizedBox(
            width: 200,
            child: AppLanguageDropdown(
              selected: ref.watch(appLanguageProvider),
              onSelect: (value) =>
                  ref.read(preferencesRepositoryProvider).appLanguage = value,
            ),
          ),
        ),
        const SizedBox(height: 16),
        Image.asset(
          'assets/icon/openvitals_logo_wide.png',
          width: 152,
          height: 104,
          fit: BoxFit.contain,
          excludeFromSemantics: true,
        ),
        const SizedBox(height: 24),
        Text(
          l10n.appName,
          style: theme.textTheme.headlineMedium
              ?.copyWith(fontWeight: FontWeight.bold),
          textAlign: TextAlign.center,
        ),
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Text(
            l10n.onboardingTagline,
            style: theme.textTheme.bodyLarge
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
        ),
      ],
    );

    if (state.availability != HealthConnectAvailability.available) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          header,
          const SizedBox(height: 24),
          _UnavailableCard(availability: state.availability),
        ],
      );
    }

    final minimum = notifier.minimumOnboardingPermissions;
    final onboardingPermissions = notifier.onboardingPermissions;
    final granted = state.grantedPermissions;
    final missingMinimum = minimum.difference(granted);
    final minimumGranted = missingMinimum.isEmpty;
    final missingOptional =
        onboardingPermissions.difference(granted).difference(minimum);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        header,
        const SizedBox(height: 24),
        _FeatureCard(
          icon: Icons.lock_outline,
          title: l10n.onboardingPrivacyTitle,
          body: l10n.onboardingPrivacyBody,
        ),
        const SizedBox(height: 12),
        _FeatureCard(
          icon: Icons.health_and_safety_outlined,
          title: l10n.onboardingHealthConnectTitle,
          body: l10n.onboardingHealthConnectBody,
        ),
        const SizedBox(height: 12),
        _FeatureCard(
          icon: Icons.info_outline,
          title: l10n.healthDisclaimerTitle,
          body: l10n.healthDisclaimerBody,
        ),
        const SizedBox(height: 24),
        FilledButton(
          onPressed: minimumGranted
              ? onComplete
              : () => notifier.requestPermissions(missingMinimum),
          child: Text(minimumGranted ? l10n.actionContinue : l10n.onboardingGrantAll),
        ),
        if (minimumGranted && missingOptional.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: FilledButton.tonal(
              onPressed: () => notifier.requestPermissions(missingOptional),
              child: Text(l10n.onboardingGrantRemaining),
            ),
          ),
        if (!minimumGranted)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              l10n.onboardingCoreRequired,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ),
        const SizedBox(height: 24),
        Text(
          l10n.onboardingPermissionsHeader,
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
            letterSpacing: 1,
          ),
        ),
        const SizedBox(height: 8),
        for (final category in notifier.permissionCategories)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: _PermissionCategoryRow(
              category: category,
              granted: granted,
              onGrant: () => _onGrant(ref, category, granted),
            ),
          ),
      ],
    );
  }

  /// Kotlin `PermissionCategoryRow(onGrant = ...)` (OnboardingScreen.kt:274-287):
  /// anything the runtime dialog can ask for is requested; a category whose only
  /// missing permissions are manual-only (e.g. exercise routes) instead opens the
  /// Health Connect settings page.
  Future<void> _onGrant(
    WidgetRef ref,
    OnboardingPermissionCategory category,
    Set<String> granted,
  ) async {
    if (!category.available) return;
    final missing = category.permissions.difference(granted);
    final requestable = missing.difference(category.manualPermissions);
    final manual = missing.intersection(category.manualPermissions);
    if (requestable.isNotEmpty) {
      await notifier.requestPermissions(requestable);
    } else if (manual.isNotEmpty) {
      (await ref.read(healthRepositoryProvider).openHealthConnectSettings())
          .orThrow();
    }
  }
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard({
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: theme.colorScheme.primary, size: 24),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: theme.textTheme.titleSmall),
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      body,
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
      ),
    );
  }
}

class _PermissionCategoryRow extends StatelessWidget {
  const _PermissionCategoryRow({
    required this.category,
    required this.granted,
    required this.onGrant,
  });

  final OnboardingPermissionCategory category;
  final Set<String> granted;
  final VoidCallback onGrant;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final total = category.permissions.length;
    final grantedCount = category.permissions.where(granted.contains).length;
    final fullyGranted = category.available && grantedCount == total;
    final partial = category.available && grantedCount > 0 && !fullyGranted;

    // When a manual-only permission (e.g. exercise routes) is still missing,
    // append the "open Health Connect settings" note to the description, mirror-
    // ing the Kotlin row's `manualPermissions`/`missingManualCount` handling.
    final baseDescription = _categoryDescription(
      l10n,
      category.id,
      available: category.available,
    );
    final missing = category.permissions.difference(granted);
    final missingRequestable = missing.difference(category.manualPermissions);
    final missingManual = missing.intersection(category.manualPermissions);
    // A category whose remaining permissions are all manual-only can't be
    // granted through the runtime dialog — it shows "Open settings" and an
    // "Open" action instead of "Grant" (Kotlin `isManualGrant`).
    final isManualGrant =
        missingRequestable.isEmpty && missingManual.isNotEmpty;
    final description = category.available && missingManual.isNotEmpty
        ? l10n.onboardingCategoryAdditionalDataAccessManualNote(baseDescription)
        : baseDescription;
    final status = !category.available
        ? l10n.onboardingStatusNotSupported
        : fullyGranted
            ? l10n.onboardingStatusGranted
            : partial
                ? l10n.onboardingStatusPartiallyGranted(grantedCount, total)
                : isManualGrant
                    ? l10n.onboardingStatusManual
                    : category.isRequired
                        ? l10n.onboardingStatusRequired
                        : l10n.onboardingStatusOptional;

    return OpenVitalsCard(
      color: fullyGranted
          ? scheme.primaryContainer.withValues(alpha: 0.4)
          : scheme.surfaceContainer,
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
                        _categoryTitle(l10n, category.id),
                        style: theme.textTheme.bodyMedium,
                      ),
                      Text(
                        status,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: fullyGranted
                              ? scheme.primary
                              : scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                if (fullyGranted)
                  Icon(Icons.check_circle_outline, color: scheme.primary)
                else if (!category.available)
                  Icon(Icons.lock_outline, color: scheme.onSurfaceVariant),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(
                description,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: scheme.onSurfaceVariant),
              ),
            ),
            if (!fullyGranted && category.available)
              Align(
                alignment: Alignment.centerRight,
                child: Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: FilledButton.tonal(
                    onPressed: onGrant,
                    child: Text(
                      isManualGrant
                          ? l10n.actionOpen
                          : partial
                              ? l10n.actionReview
                              : l10n.actionGrant,
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _UnavailableCard extends StatelessWidget {
  const _UnavailableCard({required this.availability});

  final HealthConnectAvailability availability;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final message = switch (availability) {
      HealthConnectAvailability.needsPlayStore =>
        l10n.onboardingHealthConnectNeedsPlayStore,
      HealthConnectAvailability.needsProviderUpdate =>
        l10n.onboardingHealthConnectUpdate,
      HealthConnectAvailability.notSupported =>
        l10n.onboardingHealthConnectNotSupported,
      HealthConnectAvailability.available => '',
    };
    // Only the "needs provider update" case is actionable, so — as in the Kotlin
    // `NeedsUpdateMessage` — it is toned tertiary (not error) and carries an
    // install action; the other two states are dead ends with no button.
    final needsUpdate =
        availability == HealthConnectAvailability.needsProviderUpdate;
    final card = OpenVitalsCard(
      color: needsUpdate ? scheme.tertiaryContainer : scheme.errorContainer,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Text(
          message,
          style: theme.textTheme.bodyMedium?.copyWith(
            color: needsUpdate
                ? scheme.onTertiaryContainer
                : scheme.onErrorContainer,
          ),
        ),
      ),
    );
    if (!needsUpdate) return card;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        card,
        const SizedBox(height: 16),
        FilledButton(
          onPressed: () => openExternalUrl(context, _playStoreUrl),
          child: Text(l10n.onboardingInstallHealthConnect),
        ),
      ],
    );
  }
}

/// Category title, localized one-to-one from the Kotlin
/// `onboarding_category_*` strings, keyed by [OnboardingPermissionCategory.id].
String _categoryTitle(AppLocalizations l10n, String id) {
  switch (id) {
    case 'activity_sleep':
      return l10n.onboardingCategoryActivitySleep;
    case 'heart_recovery':
      return l10n.onboardingCategoryHeartRecovery;
    case 'vitals':
      return l10n.onboardingCategoryVitals;
    case 'body':
      return l10n.onboardingCategoryBody;
    case 'activity_extras':
      return l10n.onboardingCategoryActivityExtras;
    case 'nutrition_hydration':
      return l10n.onboardingCategoryNutritionHydration;
    case 'manual_entry_write':
      return l10n.onboardingCategoryManualEntryWrite;
    case 'data_import_write':
      return l10n.onboardingCategoryDataImportWrite;
    case 'mindfulness':
      return l10n.onboardingCategoryMindfulness;
    case 'cycle_tracking':
      return l10n.onboardingCategoryCycleTracking;
    case 'additional_data_access':
      return l10n.onboardingCategoryAdditionalDataAccess;
    default:
      return id;
  }
}

/// Category description (the `onboarding_category_*_desc` strings). Mindfulness
/// falls back to its "requires a newer Health Connect version" copy when the
/// feature is unavailable.
String _categoryDescription(
  AppLocalizations l10n,
  String id, {
  required bool available,
}) {
  if (id == 'mindfulness' && !available) {
    return l10n.onboardingCategoryMindfulnessUnavailable;
  }
  switch (id) {
    case 'activity_sleep':
      return l10n.onboardingCategoryActivitySleepDesc;
    case 'heart_recovery':
      return l10n.onboardingCategoryHeartRecoveryDesc;
    case 'vitals':
      return l10n.onboardingCategoryVitalsDesc;
    case 'body':
      return l10n.onboardingCategoryBodyDesc;
    case 'activity_extras':
      return l10n.onboardingCategoryActivityExtrasDesc;
    case 'nutrition_hydration':
      return l10n.onboardingCategoryNutritionHydrationDesc;
    case 'manual_entry_write':
      return l10n.onboardingCategoryManualEntryWriteDesc;
    case 'data_import_write':
      return l10n.onboardingCategoryDataImportWriteDesc;
    case 'mindfulness':
      return l10n.onboardingCategoryMindfulnessDesc;
    case 'cycle_tracking':
      return l10n.onboardingCategoryCycleTrackingDesc;
    case 'additional_data_access':
      return l10n.onboardingCategoryAdditionalDataAccessDesc;
    default:
      return '';
  }
}
