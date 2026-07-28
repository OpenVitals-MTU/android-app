import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/external_link.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/app_language_dropdown.dart';
import '../../../ui/components/instruction_steps.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/step_bar.dart';
import '../../../ui/theme/design_tokens.dart';
import '../application/onboarding_view_model.dart';

const _healthConnectPackage = 'com.google.android.apps.healthdata';
const _playStoreUrl =
    'https://play.google.com/store/apps/details?id=$_healthConnectPackage';

/// Onboarding, shown as the start destination until it has been completed.
/// Rendered full-screen outside the adaptive shell.
///
/// Four steps ([OnboardingStep]), grouped the way **Health Connect** groups
/// records rather than the way the app's own repositories do — Activity, Body
/// measurements, Nutrition, Sleep, Vitals, then mindfulness, cycle tracking and
/// the leftovers. The user is about to be shown Android's permission dialog, and
/// these are the headings it draws.
///
/// Only Activity and Sleep are required. Everything else can be skipped and
/// picked up later from Settings.
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
    // Returning from Health Connect is the ONLY way route access is ever
    // noticed — no callback reports it. The observer lives on the screen rather
    // than on a step so it survives every step change.
    if (lifecycleState == AppLifecycleState.resumed) {
      ref.read(onboardingProvider.notifier).refreshGrantedPermissions();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(onboardingProvider);
    final notifier = ref.read(onboardingProvider.notifier);
    final l10n = AppLocalizations.of(context);

    // The grant flow is a command: a failed request (or a refused trip to the
    // Health Connect page) is surfaced once, then returned to idle.
    ref.listen(onboardingProvider.select((s) => s.grant), (previous, next) {
      if (next is! CommandFailure<void>) return;
      ScaffoldMessenger.maybeOf(context)
          ?.showSnackBar(SnackBar(content: Text(_errorText(next.error))));
      notifier.clearGrantCommand();
    });

    if (state.isCheckingPermissions) {
      return const Scaffold(body: FullScreenLoading());
    }

    // A device with no Health Connect has nothing to step through.
    if (state.availability != HealthConnectAvailability.available) {
      return Scaffold(
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(
              horizontal: Metrics.screenGutter,
              vertical: Spacing.xxl,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _Header(notifier: notifier),
                const SizedBox(height: Spacing.xxl),
                _UnavailableCard(availability: state.availability),
              ],
            ),
          ),
        ),
      );
    }

    void complete() {
      notifier.completeOnboarding();
      widget.onOnboardingComplete?.call();
    }

    return PopScope(
      // Back walks the wizard rather than leaving it — except on the first step,
      // where there is nothing behind us and the system should pop to the
      // launcher as it always did.
      canPop: !notifier.canGoBack,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) notifier.back();
      },
      child: Scaffold(
        body: SafeArea(
          child: Column(
            children: [
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(
                    horizontal: Metrics.screenGutter,
                    vertical: Spacing.xxl,
                  ),
                  child: switch (state.step) {
                    OnboardingStep.categories =>
                      _CategoriesStep(state: state, notifier: notifier),
                    OnboardingStep.mindfulness =>
                      _MindfulnessStep(state: state, notifier: notifier),
                    OnboardingStep.cycleTracking =>
                      _CycleStep(notifier: notifier),
                    OnboardingStep.additionalAccess =>
                      _AdditionalAccessStep(notifier: notifier),
                  },
                ),
              ),
              StepBar(
                onBack: notifier.canGoBack ? notifier.back : null,
                // Disabled only where a step gates: step 1, until Activity and
                // Sleep are in.
                onNext: notifier.canAdvance
                    ? (notifier.isOnLastStep ? complete : notifier.next)
                    : null,
                // "Not now" only where something is genuinely being left
                // behind. Step 1 always reads "Next" (it gates instead), and an
                // optional step the user has completed reads "Next" too —
                // calling it "Not now" after they granted it says they didn't.
                nextLabel: notifier.isOnLastStep
                    ? l10n.onboardingActionDone
                    : (state.step == OnboardingStep.categories ||
                            notifier.currentStepSatisfied
                        ? l10n.onboardingActionNext
                        : l10n.onboardingActionSkip),
                backLabel: l10n.onboardingActionBack,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// The language picker, wordmark and tagline. Only the first step carries it —
/// later steps are about one decision each and a logo above them is noise.
class _Header extends ConsumerWidget {
  const _Header({required this.notifier});

  final OnboardingViewModel notifier;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: SizedBox(
            width: 200,
            // Changing this re-renders the whole app: `appLanguageProvider`
            // drives `MaterialApp.locale` in app.dart.
            child: AppLanguageDropdown(
              selected: ref.watch(appLanguageProvider),
              onSelect: notifier.selectLanguage,
            ),
          ),
        ),
        const SizedBox(height: Spacing.lg),
        Image.asset(
          'assets/icon/openvitals_logo_wide.png',
          width: 152,
          height: 104,
          fit: BoxFit.contain,
          excludeFromSemantics: true,
        ),
        const SizedBox(height: Spacing.xxl),
        Text(
          l10n.appName,
          style: theme.textTheme.headlineMedium
              ?.copyWith(fontWeight: FontWeight.bold),
          textAlign: TextAlign.center,
        ),
        Padding(
          padding: const EdgeInsets.only(top: Spacing.sm),
          child: Text(
            l10n.onboardingTagline,
            style: theme.textTheme.bodyLarge
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
        ),
      ],
    );
  }
}

/// Step 1 — the five Health Connect categories, one tap each.
///
/// Carries the app's introduction as well, because the privacy framing has to
/// come before the first permission is asked for, not after.
class _CategoriesStep extends StatelessWidget {
  const _CategoriesStep({required this.state, required this.notifier});

  final OnboardingState state;
  final OnboardingViewModel notifier;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _Header(notifier: notifier),
        const SizedBox(height: Spacing.xxl),
        _FeatureCard(
          icon: Icons.lock_outline,
          title: l10n.onboardingPrivacyTitle,
          body: l10n.onboardingPrivacyBody,
        ),
        const SizedBox(height: Spacing.md),
        _FeatureCard(
          icon: Icons.health_and_safety_outlined,
          title: l10n.onboardingHealthConnectTitle,
          body: l10n.onboardingHealthConnectBody,
        ),
        const SizedBox(height: Spacing.md),
        _FeatureCard(
          icon: Icons.info_outline,
          title: l10n.healthDisclaimerTitle,
          body: l10n.healthDisclaimerBody,
        ),
        const SizedBox(height: Spacing.xxl),
        Text(
          l10n.onboardingStepCategoriesTitle,
          style: theme.textTheme.titleLarge,
        ),
        Padding(
          padding: const EdgeInsets.only(top: Spacing.sm),
          child: Text(
            l10n.onboardingStepCategoriesBody,
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ),
        const SizedBox(height: Spacing.lg),
        for (final row in notifier.rowsForStep(OnboardingStep.categories))
          Padding(
            padding: const EdgeInsets.only(bottom: Spacing.sm),
            child: _PermissionCategoryRow(
              row: row,
              onGrant: () => _grant(notifier, row),
            ),
          ),
        if (!state.display.requiredGranted)
          Padding(
            padding: const EdgeInsets.only(top: Spacing.sm),
            child: Text(
              l10n.onboardingCoreRequired,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ),
      ],
    );
  }
}

/// Step 2 — mindfulness, and only where the provider has it.
///
/// The switch is the point. Mindfulness is the one permission the app will not
/// ask for on its own initiative: some Health Connect builds define it but have
/// no UI category to draw it in, throw when asked, and take down the whole
/// permission screen — after which nothing at all can be granted. Turning this
/// on is the user accepting that risk, and the grant that follows asks for
/// mindfulness *alone* so it cannot take any other category with it.
class _MindfulnessStep extends StatelessWidget {
  const _MindfulnessStep({required this.state, required this.notifier});

  final OnboardingState state;
  final OnboardingViewModel notifier;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final rows = notifier.rowsForStep(OnboardingStep.mindfulness);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepHero(
          icon: Icons.self_improvement_outlined,
          title: l10n.onboardingStepMindfulnessTitle,
          body: l10n.onboardingStepMindfulnessBody,
        ),
        const SizedBox(height: Spacing.xxl),
        OpenVitalsCard(
          color: theme.colorScheme.surfaceContainer,
          child: SwitchListTile(
            value: state.mindfulnessOptIn,
            onChanged: notifier.setMindfulnessOptIn,
            title: Text(
              l10n.onboardingMindfulnessOptInTitle,
              style: theme.textTheme.bodyMedium,
            ),
            subtitle: Text(
              l10n.onboardingMindfulnessOptInBody,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
        ),
        // The row only exists once the opt-in is on — the catalog drops the
        // category while `mindfulnessAvailable` is false.
        for (final row in rows)
          Padding(
            padding: const EdgeInsets.only(top: Spacing.sm),
            child: _PermissionCategoryRow(
              row: row,
              onGrant: () => _grant(notifier, row),
            ),
          ),
      ],
    );
  }
}

/// Step 3 — cycle tracking. Always offered, never required.
class _CycleStep extends StatelessWidget {
  const _CycleStep({required this.notifier});

  final OnboardingViewModel notifier;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepHero(
          icon: Icons.calendar_month_outlined,
          title: l10n.onboardingStepCycleTitle,
          body: l10n.onboardingStepCycleBody,
        ),
        const SizedBox(height: Spacing.xxl),
        for (final row in notifier.rowsForStep(OnboardingStep.cycleTracking))
          _PermissionCategoryRow(
            row: row,
            onGrant: () => _grant(notifier, row),
          ),
      ],
    );
  }
}

/// Step 4 — history and background access by dialog, exercise routes by hand.
///
/// Routes have no deep link. `MANAGE_HEALTH_PERMISSIONS` lands on the app's own
/// Health Connect page and "Additional access" is one tap below that, which is
/// as close as Android lets anyone get — hence a walkthrough rather than a
/// button that claims to do it.
class _AdditionalAccessStep extends StatelessWidget {
  const _AdditionalAccessStep({required this.notifier});

  final OnboardingViewModel notifier;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final rows = notifier.rowsForStep(OnboardingStep.additionalAccess);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepHero(
          icon: Icons.tune_outlined,
          title: l10n.onboardingStepAdditionalTitle,
          body: l10n.onboardingStepAdditionalBody,
        ),
        const SizedBox(height: Spacing.xxl),
        for (final row in rows)
          Padding(
            padding: const EdgeInsets.only(bottom: Spacing.lg),
            child: _PermissionCategoryRow(
              row: row,
              onGrant: () => _grant(notifier, row),
            ),
          ),
        // Route READ only. The WRITE side is an ordinary toggle in the Activity
        // request and is already handled there; this is for the one Health
        // Connect keeps on its Additional access page, out of reach of any
        // intent.
        if (notifier.routesOutstanding)
          OpenVitalsCard(
            child: Padding(
              padding: const EdgeInsets.all(Metrics.cardPadding),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    l10n.onboardingRoutesTitle,
                    style: theme.textTheme.titleSmall,
                  ),
                  Padding(
                    padding: const EdgeInsets.only(
                      top: Spacing.xs,
                      bottom: Spacing.lg,
                    ),
                    child: Text(
                      l10n.onboardingRoutesBody,
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ),
                  InstructionSteps(
                    steps: [
                      l10n.onboardingRoutesStep1,
                      l10n.onboardingRoutesStep2,
                      l10n.onboardingRoutesStep3,
                    ],
                  ),
                  const SizedBox(height: Spacing.lg),
                  FilledButton.tonal(
                    onPressed: notifier.openHealthConnectSettings,
                    child: Text(l10n.settingsOpenHealthPermissions),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}

/// Anything the runtime dialog can ask for is requested; a category whose only
/// missing permissions are manual-only (exercise routes) opens Health Connect
/// instead of firing a dialog that would silently ignore them.
Future<void> _grant(
  OnboardingViewModel notifier,
  OnboardingCategoryRow row,
) async {
  if (!row.category.available) return;
  if (row.missingRequestable.isNotEmpty) {
    await notifier.requestPermissions(row.missingRequestable);
  } else if (row.missingManual.isNotEmpty) {
    await notifier.openHealthConnectSettings();
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
        padding: const EdgeInsets.all(Metrics.cardPadding),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: theme.colorScheme.primary, size: 24),
            const SizedBox(width: Spacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: theme.textTheme.titleSmall),
                  Padding(
                    padding: const EdgeInsets.only(top: Spacing.xs),
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
  const _PermissionCategoryRow({required this.row, required this.onGrant});

  final OnboardingCategoryRow row;
  final VoidCallback onGrant;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final category = row.category;
    final fullyGranted = row.fullyGranted;
    final partial = row.partial;
    // A category whose remaining permissions are all manual-only can't be
    // granted through the runtime dialog — it shows "Open settings" and an
    // "Open" action instead of "Grant".
    final isManualGrant = row.isManualGrant;

    final baseDescription = _categoryDescription(
      l10n,
      category.id,
      available: category.available,
    );
    final description = category.available && row.missingManual.isNotEmpty
        ? l10n.onboardingCategoryAdditionalDataAccessManualNote(baseDescription)
        : baseDescription;
    final status = !category.available
        ? l10n.onboardingStatusNotSupported
        : fullyGranted
            ? l10n.onboardingStatusGranted
            : partial
                ? l10n.onboardingStatusPartiallyGranted(
                    row.grantedCount, row.total)
                : isManualGrant
                    ? l10n.onboardingStatusManual
                    : category.isRequired
                        ? l10n.onboardingStatusRequired
                        : l10n.onboardingStatusOptional;

    return OpenVitalsCard(
      color: fullyGranted
          ? scheme.primaryContainer.withValues(alpha: Emphasis.disabled)
          : scheme.surfaceContainer,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: Metrics.cardPadding,
          vertical: Metrics.metricTilePadding,
        ),
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
              padding: const EdgeInsets.only(top: Spacing.sm),
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
                  padding: const EdgeInsets.only(top: Spacing.sm),
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
    // Only "needs provider update" is actionable, so it is toned tertiary (not
    // error) and carries an install action; the other two are dead ends.
    final needsUpdate =
        availability == HealthConnectAvailability.needsProviderUpdate;
    final card = OpenVitalsCard(
      color: needsUpdate ? scheme.tertiaryContainer : scheme.errorContainer,
      child: Padding(
        padding: const EdgeInsets.all(Metrics.cardPadding),
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
        const SizedBox(height: Spacing.lg),
        FilledButton(
          onPressed: () => openExternalUrl(context, _playStoreUrl),
          child: Text(l10n.onboardingInstallHealthConnect),
        ),
      ],
    );
  }
}

/// Resolves a [ScreenError] into the message the grant-failure SnackBar shows.
String _errorText(ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => 'Not found.',
      ScreenErrorMissingArgument() => 'Something went wrong.',
      ScreenErrorPermissionDenied() => 'Permission denied.',
      ScreenErrorHealthConnectUnavailable() => 'Health Connect is unavailable.',
    };

/// Category title, keyed by [OnboardingPermissionCategory.id]. The ids are
/// Health Connect's categories, so these read the way the system dialog does.
String _categoryTitle(AppLocalizations l10n, String id) => switch (id) {
      'activity' => l10n.onboardingCategoryActivity,
      'body' => l10n.onboardingCategoryBody,
      'nutrition' => l10n.onboardingCategoryNutrition,
      'sleep' => l10n.onboardingCategorySleep,
      'vitals' => l10n.onboardingCategoryVitals,
      'mindfulness' => l10n.onboardingCategoryMindfulness,
      'cycle_tracking' => l10n.onboardingCategoryCycleTracking,
      'additional_data_access' => l10n.onboardingCategoryAdditionalDataAccess,
      _ => id,
    };

/// Category description. Mindfulness falls back to its "requires a newer Health
/// Connect version" copy when the feature is unavailable.
String _categoryDescription(
  AppLocalizations l10n,
  String id, {
  required bool available,
}) {
  if (id == 'mindfulness' && !available) {
    return l10n.onboardingCategoryMindfulnessUnavailable;
  }
  return switch (id) {
    'activity' => l10n.onboardingCategoryActivityDesc,
    'body' => l10n.onboardingCategoryBodyDesc,
    'nutrition' => l10n.onboardingCategoryNutritionDesc,
    'sleep' => l10n.onboardingCategorySleepDesc,
    'vitals' => l10n.onboardingCategoryVitalsDesc,
    'mindfulness' => l10n.onboardingCategoryMindfulnessDesc,
    'cycle_tracking' => l10n.onboardingCategoryCycleTrackingDesc,
    'additional_data_access' =>
      l10n.onboardingCategoryAdditionalDataAccessDesc,
    _ => '',
  };
}
