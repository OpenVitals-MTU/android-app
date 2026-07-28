import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/onboarding_permission_category.dart';
import '../../../domain/preferences/app_language.dart';
import '../../../ui/components/health_connect_gate.dart';
import 'onboarding_display.dart';

// The rows onboarding offers are a domain description of what the app can ask
// for; the screen renders them straight out of here.
export '../../../domain/model/onboarding_permission_category.dart';
export 'onboarding_display.dart';

part 'onboarding_view_model.freezed.dart';

/// The display of a screen that has not read the catalog yet: no rows, and
/// nothing granted (so the primary action is still the grant, not "Continue").
const OnboardingDisplay _emptyDisplay = OnboardingDisplay(
  rows: <OnboardingCategoryRow>[],
  missingRequired: <String>{},
  requiredGranted: false,
);

/// The four screens onboarding walks through, in order.
///
/// A plain enum switched on in the screen's `build`, mirroring the two flows the
/// app already has — `DeviceSyncStep` and `CsvImportStep`. Deliberately NOT
/// router sub-routes: the screen owns a `WidgetsBindingObserver` that re-reads
/// the granted set on resume, which is the only way access granted by hand
/// inside Health Connect is ever noticed, and that observer has to outlive the
/// individual steps.
enum OnboardingStep {
  /// The five Health Connect categories, one tap each. Activity and Sleep must
  /// be granted before this step will let go.
  categories,

  /// Mindfulness — offered only where the provider has it. Skipped otherwise.
  mindfulness,

  /// Cycle tracking. Always offered, never required.
  cycleTracking,

  /// History and background access by dialog, exercise routes by hand.
  additionalAccess;

  bool get isFirst => this == OnboardingStep.categories;
  bool get isLast => this == OnboardingStep.additionalAccess;
}

/// The Riverpod port of the Kotlin `OnboardingUiState`.
///
/// The phased-grant booleans of the Kotlin state — and the per-row granted
/// counts the screen used to fold on every rebuild — are precomputed into
/// [display] at load time.
@freezed
abstract class OnboardingState with _$OnboardingState {
  const factory OnboardingState({
    @Default(HealthConnectAvailability.available)
    HealthConnectAvailability availability,
    @Default(<String>{}) Set<String> grantedPermissions,

    /// Which of the four screens is showing.
    @Default(OnboardingStep.categories) OnboardingStep step,

    /// Device feature AND user opt-in: whether the mindfulness row is offered
    /// and its permissions may be requested.
    @Default(false) bool mindfulnessAvailable,

    /// The device's answer alone. Drives whether the opt-in switch is shown at
    /// all — there is no point offering it on a phone that has no mindfulness.
    @Default(false) bool mindfulnessSupportedByDevice,

    /// The switch's own position, read from (and written to) preferences.
    @Default(false) bool mindfulnessOptIn,
    @Default(true) bool isCheckingPermissions,
    @Default(_emptyDisplay) OnboardingDisplay display,

    /// The permission-grant flow: the runtime dialog, and the trip to the
    /// Health Connect page a non-requestable permission needs. One user action,
    /// one command.
    @Default(CommandState<void>.idle()) CommandState<void> grant,

    /// Why the initial availability + granted-permission read failed, if it
    /// did. The screen degrades to the (empty) permission flow rather than
    /// hanging on the loader, which is what the thrown failure used to do.
    ScreenError? error,
  }) = _OnboardingState;
}

/// The Riverpod port of the Kotlin `OnboardingViewModel`.
///
/// Manual [Notifier] (no codegen): [build] kicks off [checkState], which reads
/// Health Connect availability + the currently-granted permissions. The screen
/// drives [requestPermissions] for a phase/category, [openHealthConnectSettings]
/// for a manual-only one, [selectLanguage] for the header dropdown, and
/// [completeOnboarding] to persist the onboarding-complete + privacy-policy
/// prefs so the app routes to the dashboard on next launch.
class OnboardingViewModel extends Notifier<OnboardingState> {
  @override
  OnboardingState build() {
    Future.microtask(() {
      if (ref.mounted) checkState();
    });
    return const OnboardingState();
  }

  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  /// The rows, the one required request and the full offer — assembled from the
  /// device's permission catalog, with the mindfulness row present only where
  /// mindfulness exists *and* the user opted in
  /// (see [ReadOnboardingPermissionCatalogUseCase]).
  OnboardingPermissionCatalog get _catalog =>
      _catalogFor(mindfulnessAvailable: state.mindfulnessAvailable);

  OnboardingPermissionCatalog _catalogFor({
    required bool mindfulnessAvailable,
  }) =>
      ref.read(readOnboardingPermissionCatalogUseCaseProvider)(
        mindfulnessAvailable: mindfulnessAvailable,
      );

  Set<String> get requiredOnboardingPermissions => _catalog.requiredPermissions;

  Set<String> get onboardingPermissions => _catalog.allPermissions;

  /// The grantable permission groups, filtered to the non-empty ones (mirrors
  /// the Kotlin `permissionCategories.filter { it.permissions.isNotEmpty() }`).
  List<OnboardingPermissionCategory> get permissionCategories =>
      _catalog.categories;

  Future<void> checkState() async {
    // Availability is resolved from the platform (async plugin boundary) rather
    // than read from the still-default cache, and nothing else is asked of a
    // device that has no Health Connect — see [CheckOnboardingStateUseCase].
    final result = await ref.read(checkOnboardingStateUseCaseProvider)();
    if (!ref.mounted) return;
    switch (result) {
      case Ok(:final value):
        state = OnboardingState(
          availability: value.availability,
          grantedPermissions: value.grantedPermissions,
          // Rebuilt from scratch rather than copyWith, so the step has to be
          // carried across by hand. It matters: [setMindfulnessOptIn] re-runs
          // this to re-resolve the feature flags, and the user flipping that
          // switch is standing on the mindfulness step at the time — resetting
          // to the first step would throw them back to the start of onboarding.
          step: state.step,
          mindfulnessAvailable: value.mindfulnessAvailable,
          mindfulnessSupportedByDevice: value.mindfulnessSupportedByDevice,
          mindfulnessOptIn: _prefs.healthConnectMindfulnessEnabled,
          isCheckingPermissions: false,
          display: buildOnboardingDisplay(
            _catalogFor(mindfulnessAvailable: value.mindfulnessAvailable),
            value.grantedPermissions,
          ),
        );
      case Err(:final failure):
        // The read that failed is the one that says what is granted, so the
        // rows fall back to "nothing is". The loader is dropped either way: a
        // thrown failure used to leave it spinning for good.
        state = state.copyWith(
          isCheckingPermissions: false,
          display: buildOnboardingDisplay(_catalog, const <String>{}),
          error: failure.toScreenError(
            fallback: 'Unable to check Health Connect.',
          ),
        );
    }
  }

  /// Re-reads the granted permission set without the full-screen loader. Called
  /// when the app returns to the foreground (e.g. after the user granted access
  /// manually in the Health Connect settings page), so category rows flip to
  /// "Granted" without needing an app restart.
  Future<void> refreshGrantedPermissions() async {
    if (state.availability != HealthConnectAvailability.available) return;
    final result =
        await ref.read(loadGrantedHealthPermissionsUseCaseProvider)();
    if (!ref.mounted) return;
    // A failed re-read is not worth a message: the rows keep the last set they
    // knew about, exactly as they would have on an unchanged one.
    if (result case Ok(:final value) when value != state.grantedPermissions) {
      state = state.copyWith(
        grantedPermissions: value,
        display: buildOnboardingDisplay(_catalog, value),
      );
      ref.invalidate(grantedHealthPermissionsProvider);
    }
  }

  /// Requests [permissions] then re-reads the granted set (Kotlin
  /// `onPermissionsResult`). No-op for an empty request.
  ///
  /// If the runtime dialog could not grant ANY of the requested permissions —
  /// which happens when Health Connect reports them as non-requestable (planned
  /// exercise, exercise routes, background/history access) — this falls back to
  /// opening the Health Connect page so the user can toggle them manually
  /// (mirrors the Kotlin "Open required Health Connect permissions" action).
  Future<void> requestPermissions(Set<String> permissions) async {
    if (permissions.isEmpty) return;
    state = state.copyWith(grant: const CommandState<void>.running());
    // Whether the dialog achieved anything is not something it will say — see
    // [GrantOnboardingPermissionsUseCase], which works it out by comparing the
    // granted set on either side of the request.
    final result =
        await ref.read(grantOnboardingPermissionsUseCaseProvider)(permissions);
    if (!ref.mounted) return;
    switch (result) {
      case Ok(:final value):
        state = state.copyWith(
          grantedPermissions: value.grantedPermissions,
          display: buildOnboardingDisplay(_catalog, value.grantedPermissions),
          grant: const CommandState<void>.success(null),
        );
        // Keep the shared gate providers fresh for screens shown after
        // onboarding.
        ref.invalidate(grantedHealthPermissionsProvider);

        // Opened here rather than inside the use case so the new granted set is
        // already published before the user disappears into Health Connect's UI.
        //
        // Only when the dialog achieved NOTHING and the permissions are still
        // missing — that is Health Connect saying "not requestable", and the
        // settings page is the only way through. Onboarding no longer opens
        // settings on its own initiative anywhere else: the last step asks
        // explicitly, because being thrown into another app unannounced is
        // worse than a button.
        if (value.needsManualGrant) await openHealthConnectSettings();
      case Err(:final failure):
        state = state.copyWith(
          grant: CommandState<void>.failure(
            failure.toScreenError(fallback: 'Unable to request permissions.'),
          ),
        );
    }
  }

  // ── Step navigation ───────────────────────────────────────────────────────

  /// The permissions behind the "additional data access" row: whichever of
  /// history / background reads this device supports. Exercise routes are NOT
  /// here — the last step walks the user to those by hand, because no intent
  /// can reach the toggle that grants them.
  Set<String> get additionalAccessPermissions => _catalog.categories
      .where((category) => category.id == 'additional_data_access')
      .expand((category) => category.permissions)
      .toSet();

  /// Whether [step] has anything to say on this device.
  ///
  /// A step with nothing to offer is not shown at all rather than rendered as a
  /// dead end — the same instinct as the activity flow's auto-advance past a
  /// single-option choice.
  bool _stepApplies(OnboardingStep step) => switch (step) {
        OnboardingStep.categories => true,
        // No provider feature means no switch to offer and nothing to grant.
        OnboardingStep.mindfulness => state.mindfulnessSupportedByDevice,
        OnboardingStep.cycleTracking =>
          _rowsFor(OnboardingStep.cycleTracking).isNotEmpty,
        // Either half is reason enough: the history/background row, or the
        // exercise-routes fallback. Routes are not a row here — they are asked
        // for with Activity — so checking only [additionalAccessPermissions]
        // would skip the step on a provider without history/background and take
        // the fallback with it.
        OnboardingStep.additionalAccess =>
          additionalAccessPermissions.isNotEmpty || routesOutstanding,
      };

  /// Whether route READ access is still missing.
  ///
  /// `WRITE_EXERCISE_ROUTE` comes with the Activity request like any other
  /// toggle; `READ_EXERCISE_ROUTES` does not — it lives under Health Connect's
  /// *Additional access* page, which no intent can deep-link to. So the
  /// walkthrough is shown exactly while that one is outstanding.
  bool get routesOutstanding {
    final routes = ref.read(healthRepositoryProvider).routePermissions;
    return routes.isNotEmpty &&
        routes.difference(state.grantedPermissions).isNotEmpty;
  }

  /// The next applicable step after [from], or null when [from] is the last one
  /// with anything to show.
  OnboardingStep? _stepAfter(OnboardingStep from) {
    for (var i = from.index + 1; i < OnboardingStep.values.length; i++) {
      final candidate = OnboardingStep.values[i];
      if (_stepApplies(candidate)) return candidate;
    }
    return null;
  }

  /// The previous applicable step before [from], or null when [from] is the
  /// first one — which is what tells the screen to let the system back out.
  OnboardingStep? _stepBefore(OnboardingStep from) {
    for (var i = from.index - 1; i >= 0; i--) {
      final candidate = OnboardingStep.values[i];
      if (_stepApplies(candidate)) return candidate;
    }
    return null;
  }

  /// Whether the current step will let go. Only the first one gates: Activity
  /// and Sleep are the floor the dashboard cannot render below.
  bool get canAdvance =>
      state.step != OnboardingStep.categories || state.display.requiredGranted;

  /// True when there is no further step — the action reads "Done" and finishes
  /// onboarding rather than advancing.
  bool get isOnLastStep => _stepAfter(state.step) == null;

  /// Whether the current step has nothing left outstanding.
  ///
  /// Drives what the forward button *says* on an optional step: "Not now" is a
  /// promise that you are leaving something behind, so it is a lie once the step
  /// is done — having granted mindfulness, the way on is "Next".
  ///
  /// A step with no rows at all is NOT satisfied. That is the mindfulness step
  /// with its opt-in switched off: there is nothing to grant precisely because
  /// the user declined it, and "Next" would claim otherwise.
  bool get currentStepSatisfied {
    final rows = _rowsFor(state.step);
    return rows.isNotEmpty && rows.every((row) => row.fullyGranted);
  }

  /// True when [back] has somewhere to go. False on the first step, where the
  /// screen lets the system pop instead.
  bool get canGoBack => _stepBefore(state.step) != null;

  /// Advances past any step that does not apply to this device. No-op when the
  /// current step is still gating.
  void next() {
    if (!canAdvance) return;
    final target = _stepAfter(state.step);
    if (target != null) state = state.copyWith(step: target);
  }

  void back() {
    final target = _stepBefore(state.step);
    if (target != null) state = state.copyWith(step: target);
  }

  /// The rows belonging to [step]. The catalog is one flat list; each screen
  /// renders only its own slice.
  List<OnboardingCategoryRow> rowsForStep(OnboardingStep step) =>
      _rowsFor(step);

  static const Map<OnboardingStep, List<String>> _stepCategoryIds = {
    OnboardingStep.categories: [
      'activity',
      'body',
      'nutrition',
      'sleep',
      'vitals',
    ],
    OnboardingStep.mindfulness: ['mindfulness'],
    OnboardingStep.cycleTracking: ['cycle_tracking'],
    OnboardingStep.additionalAccess: ['additional_data_access'],
  };

  List<OnboardingCategoryRow> _rowsFor(OnboardingStep step) {
    final ids = _stepCategoryIds[step] ?? const <String>[];
    // Ordered by the id list, not by catalog order, so the screens read the way
    // they are written here.
    return [
      for (final id in ids)
        ...state.display.rows.where((row) => row.category.id == id),
    ];
  }

  /// Turns the mindfulness opt-in on or off from the onboarding screen.
  ///
  /// The preference is an *input* to the resolved feature flags — see the crash
  /// it guards against in `HealthConnectNativeDataSource.resolveFeatureFlags` —
  /// so the entire taxonomy is stale the moment it flips. [checkState] goes
  /// through `refreshAvailability()`, which re-resolves the flags and the
  /// device-supported permission set before the catalog is rebuilt; the same
  /// reasoning as `SettingsViewModel.setHealthConnectMindfulnessEnabled`.
  Future<void> setMindfulnessOptIn(bool enabled) async {
    _prefs.healthConnectMindfulnessEnabled = enabled;
    state = state.copyWith(mindfulnessOptIn: enabled);
    ref
      ..invalidate(healthConnectAvailabilityProvider)
      ..invalidate(grantedHealthPermissionsProvider);
    await checkState();
  }

  /// Opens the Health Connect permission page — the only way to grant a
  /// permission the runtime dialog refuses to ask for (exercise routes,
  /// background/history access).
  Future<void> openHealthConnectSettings() async {
    state = state.copyWith(grant: const CommandState<void>.running());
    final result = await ref.read(openHealthConnectSettingsUseCaseProvider)();
    if (!ref.mounted) return;
    state = state.copyWith(
      grant: switch (result) {
        Ok() => const CommandState<void>.success(null),
        Err(:final failure) => CommandState<void>.failure(
            failure.toScreenError(
              fallback: 'Unable to open Health Connect.',
            ),
          ),
      },
    );
  }

  /// The screen has shown the outcome of the grant flow; put the command back to
  /// idle so re-entering the route cannot replay it.
  void clearGrantCommand() {
    if (state.grant is! CommandIdle<void>) {
      state = state.copyWith(grant: const CommandState<void>.idle());
    }
  }

  /// Persists the app language. Changing it re-renders the whole app:
  /// `appLanguageProvider` drives `MaterialApp.locale` in app.dart.
  void selectLanguage(AppLanguage language) => _prefs.appLanguage = language;

  /// Persists the privacy-policy acceptance + onboarding-complete prefs so the
  /// router picks the dashboard as the start destination next launch.
  void completeOnboarding() {
    _prefs
      ..acceptedPrivacyPolicyVersion =
          PreferencesRepository.currentPrivacyPolicyVersion
      ..privacyPolicyAcceptedAtMillis = DateTime.now().millisecondsSinceEpoch
      // Records WHICH permission set was asked for, so a future widening of it
      // can send this user back through onboarding rather than silently never
      // asking them — see [PreferencesRepository.lastPromptedPermissionSetVersion].
      ..lastPromptedPermissionSetVersion =
          HealthPermissionService.PERMISSION_SET_VERSION
      ..onboardingDone = true;
  }
}

/// The onboarding state provider — a manually-declared [NotifierProvider].
final onboardingProvider =
    NotifierProvider<OnboardingViewModel, OnboardingState>(
  OnboardingViewModel.new,
);
