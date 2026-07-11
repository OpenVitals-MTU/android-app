import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../data/prefs/preferences_repository.dart';
import '../../data/repository/contract/health_repository.dart';
import '../../di/providers.dart';
import '../../domain/model/health_connect_availability.dart';
import '../../ui/components/health_connect_gate.dart';

part 'onboarding_notifier.freezed.dart';

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

/// A grantable permission group shown as a row in onboarding. Port of the Kotlin
/// `OnboardingPermissionCategory`. Title/description are resolved from [id] in
/// the screen via `AppLocalizations` (the Kotlin `titleRes`/`descriptionRes`),
/// so this class carries no user-facing strings.
class OnboardingPermissionCategory {
  const OnboardingPermissionCategory({
    required this.id,
    required this.permissions,
    this.manualPermissions = const <String>{},
    this.isRequired = false,
    this.available = true,
  });

  final String id;
  final Set<String> permissions;

  /// The subset of [permissions] that can't be granted through the runtime
  /// dialog and must be toggled manually in Health Connect settings (Kotlin
  /// `OnboardingPermissionCategory.manualPermissions`, e.g. exercise routes).
  final Set<String> manualPermissions;
  final bool isRequired;
  final bool available;
}

/// The Riverpod port of the Kotlin `OnboardingViewModel`.
///
/// Manual [Notifier] (no codegen): [build] kicks off [checkState], which reads
/// Health Connect availability + the currently-granted permissions. The screen
/// drives [requestPermissions] for a phase/category and [completeOnboarding] to
/// persist the onboarding-complete + privacy-policy prefs so the app routes to
/// the dashboard on next launch.
class OnboardingNotifier extends Notifier<OnboardingState> {
  @override
  OnboardingState build() {
    Future.microtask(() {
      if (ref.mounted) checkState();
    });
    return const OnboardingState();
  }

  HealthRepository get _repo => ref.read(healthRepositoryProvider);
  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  Set<String> get minimumOnboardingPermissions =>
      _repo.minimumOnboardingPermissions;

  Set<String> get onboardingPermissions => _repo.onboardingPermissions;

  /// The grantable permission groups, filtered to the non-empty ones (mirrors
  /// the Kotlin `permissionCategories.filter { it.permissions.isNotEmpty() }`).
  List<OnboardingPermissionCategory> get permissionCategories {
    final repo = _repo;
    return <OnboardingPermissionCategory>[
      OnboardingPermissionCategory(
        id: 'activity_sleep',
        permissions: repo.corePermissions,
        isRequired: true,
      ),
      OnboardingPermissionCategory(
        id: 'heart_recovery',
        permissions: repo.heartPermissions,
        isRequired: true,
      ),
      OnboardingPermissionCategory(
        id: 'vitals',
        permissions: repo.vitalsPermissions,
        isRequired: true,
      ),
      OnboardingPermissionCategory(
        id: 'body',
        permissions: repo.bodyPermissions,
      ),
      OnboardingPermissionCategory(
        id: 'activity_extras',
        permissions: repo.activityExtrasPermissions,
      ),
      OnboardingPermissionCategory(
        id: 'nutrition_hydration',
        permissions: repo.nutritionHydrationPermissions,
      ),
      OnboardingPermissionCategory(
        id: 'manual_entry_write',
        permissions: repo.requestableWritePermissions,
      ),
      OnboardingPermissionCategory(
        id: 'data_import_write',
        permissions: repo.dataImportWritePermissions,
      ),
      OnboardingPermissionCategory(
        id: 'mindfulness',
        permissions: repo.mindfulnessPermissions,
        available: state.mindfulnessAvailable,
      ),
      // Access past data (history) + access data in the background can be
      // requested directly via the dialog; exercise-route access needs the
      // "Always" toggle in Health Connect settings (opened via the fallback).
      // Mirrors the Kotlin OnboardingViewModel's additionalDataAccess +
      // routePermissions category, with routes flagged as manual-only.
      OnboardingPermissionCategory(
        id: 'additional_data_access',
        permissions: {
          ...repo.additionalDataAccessPermissions,
          ...repo.routePermissions,
        },
        manualPermissions: repo.routePermissions,
      ),
      OnboardingPermissionCategory(
        id: 'cycle_tracking',
        permissions: repo.cyclePermissions,
      ),
    ].where((category) => category.permissions.isNotEmpty).toList();
  }

  Future<void> checkState() async {
    // Resolve availability from the platform (async plugin boundary) rather than
    // reading the still-default cached value.
    final availability = await _repo.refreshAvailability();
    if (!ref.mounted) return;
    if (availability != HealthConnectAvailability.available) {
      if (!ref.mounted) return;
      state = OnboardingState(
        availability: availability,
        isCheckingPermissions: false,
      );
      return;
    }
    final mindfulnessAvailable = _repo.isMindfulnessAvailable();
    final granted = await _repo.grantedPermissions();
    if (!ref.mounted) return;
    state = OnboardingState(
      availability: availability,
      grantedPermissions: granted,
      mindfulnessAvailable: mindfulnessAvailable,
      isCheckingPermissions: false,
    );
  }

  /// Re-reads the granted permission set without the full-screen loader. Called
  /// when the app returns to the foreground (e.g. after the user granted access
  /// manually in the Health Connect settings page), so category rows flip to
  /// "Granted" without needing an app restart.
  Future<void> refreshGrantedPermissions() async {
    if (state.availability != HealthConnectAvailability.available) return;
    final granted = await _repo.grantedPermissions();
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
    final before = _repo.availability() == HealthConnectAvailability.available
        ? await _repo.grantedPermissions()
        : const <String>{};
    await _repo.requestPermissions(permissions);
    final granted = await _repo.grantedPermissions();
    if (!ref.mounted) return;
    state = state.copyWith(grantedPermissions: granted);
    // Keep the shared gate providers fresh for screens shown after onboarding.
    ref.invalidate(grantedHealthPermissionsProvider);

    final gainedAny =
        permissions.any((p) => granted.contains(p) && !before.contains(p));
    if (!gainedAny && !permissions.every(granted.contains)) {
      await _repo.openHealthConnectSettings();
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
final onboardingNotifierProvider =
    NotifierProvider<OnboardingNotifier, OnboardingState>(
  OnboardingNotifier.new,
);
