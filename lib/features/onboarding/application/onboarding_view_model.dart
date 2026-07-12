import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/onboarding_permission_category.dart';
import '../../../ui/components/health_connect_gate.dart';

// The rows onboarding offers are a domain description of what the app can ask
// for; the screen renders them straight out of here.
export '../../../domain/model/onboarding_permission_category.dart';

part 'onboarding_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `OnboardingUiState`.
///
/// The phased-grant booleans of the Kotlin state are derived on demand in the
/// screen from [grantedPermissions] rather than duplicated here.
@freezed
abstract class OnboardingState with _$OnboardingState {
  const factory OnboardingState({
    @Default(HealthConnectAvailability.available)
    HealthConnectAvailability availability,
    @Default(<String>{}) Set<String> grantedPermissions,
    @Default(false) bool mindfulnessAvailable,
    @Default(true) bool isCheckingPermissions,
  }) = _OnboardingState;
}

/// The Riverpod port of the Kotlin `OnboardingViewModel`.
///
/// Manual [Notifier] (no codegen): [build] kicks off [checkState], which reads
/// Health Connect availability + the currently-granted permissions. The screen
/// drives [requestPermissions] for a phase/category and [completeOnboarding] to
/// persist the onboarding-complete + privacy-policy prefs so the app routes to
/// the dashboard on next launch.
class OnboardingViewModel extends Notifier<OnboardingState> {
  @override
  OnboardingState build() {
    Future.microtask(() {
      if (ref.mounted) checkState();
    });
    return const OnboardingState();
  }

  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  /// The rows, the required minimum and the full offer — assembled from the
  /// device's permission catalog, with the mindfulness row present only where
  /// mindfulness exists (see [ReadOnboardingPermissionCatalogUseCase]).
  OnboardingPermissionCatalog get _catalog =>
      ref.read(readOnboardingPermissionCatalogUseCaseProvider)(
        mindfulnessAvailable: state.mindfulnessAvailable,
      );

  Set<String> get minimumOnboardingPermissions => _catalog.minimumPermissions;

  Set<String> get onboardingPermissions => _catalog.allPermissions;

  /// The grantable permission groups, filtered to the non-empty ones (mirrors
  /// the Kotlin `permissionCategories.filter { it.permissions.isNotEmpty() }`).
  List<OnboardingPermissionCategory> get permissionCategories =>
      _catalog.categories;

  Future<void> checkState() async {
    // Availability is resolved from the platform (async plugin boundary) rather
    // than read from the still-default cache, and nothing else is asked of a
    // device that has no Health Connect — see [CheckOnboardingStateUseCase].
    final onboarding = await ref.read(checkOnboardingStateUseCaseProvider)();
    if (!ref.mounted) return;
    state = OnboardingState(
      availability: onboarding.availability,
      grantedPermissions: onboarding.grantedPermissions,
      mindfulnessAvailable: onboarding.mindfulnessAvailable,
      isCheckingPermissions: false,
    );
  }

  /// Re-reads the granted permission set without the full-screen loader. Called
  /// when the app returns to the foreground (e.g. after the user granted access
  /// manually in the Health Connect settings page), so category rows flip to
  /// "Granted" without needing an app restart.
  Future<void> refreshGrantedPermissions() async {
    if (state.availability != HealthConnectAvailability.available) return;
    final granted = await ref.read(loadGrantedHealthPermissionsUseCaseProvider)();
    if (!ref.mounted || granted == state.grantedPermissions) return;
    state = state.copyWith(grantedPermissions: granted);
    ref.invalidate(grantedHealthPermissionsProvider);
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
    // Whether the dialog achieved anything is not something it will say — see
    // [GrantOnboardingPermissionsUseCase], which works it out by comparing the
    // granted set on either side of the request.
    final grant =
        await ref.read(grantOnboardingPermissionsUseCaseProvider)(permissions);
    if (!ref.mounted) return;
    state = state.copyWith(grantedPermissions: grant.grantedPermissions);
    // Keep the shared gate providers fresh for screens shown after onboarding.
    ref.invalidate(grantedHealthPermissionsProvider);

    // Opened here rather than inside the use case so the new granted set is
    // already published before the user disappears into Health Connect's UI.
    if (grant.needsManualGrant) {
      await ref.read(openHealthConnectSettingsUseCaseProvider)();
    }
  }

  /// Persists the privacy-policy acceptance + onboarding-complete prefs so the
  /// router picks the dashboard as the start destination next launch.
  void completeOnboarding() {
    _prefs
      ..acceptedPrivacyPolicyVersion =
          PreferencesRepository.currentPrivacyPolicyVersion
      ..privacyPolicyAcceptedAtMillis = DateTime.now().millisecondsSinceEpoch
      ..onboardingDone = true;
  }
}

/// The onboarding state provider — a manually-declared [NotifierProvider].
final onboardingProvider =
    NotifierProvider<OnboardingViewModel, OnboardingState>(
  OnboardingViewModel.new,
);
