import '../../data/repository/contract/health_repository.dart';
import '../model/onboarding_permission_category.dart';

/// Assembles the permission rows onboarding offers — **synchronously**, because
/// they are a static description of what the app can ask for, not a question for
/// the platform.
///
/// The grouping is the point. Health Connect grants permissions one record type at
/// a time, which is unanswerable as a list of forty toggles; onboarding instead
/// asks for a handful of *reasons* ("heart & recovery", "nutrition & hydration"),
/// each of which expands to the permissions that reason needs.
///
/// Two of the groups are not a free choice:
///
/// * Mindfulness is an optional Health Connect feature, so its row is offered only
///   when the device has it — hence [mindfulnessAvailable] as the one input.
/// * Exercise routes cannot be granted by the runtime dialog at all. They ride
///   along with the "additional data access" row but are flagged as manual, so the
///   screen can send the user to the Health Connect page instead of asking for
///   something the dialog will silently ignore.
///
/// An empty group is dropped rather than rendered as a row that grants nothing.
class ReadOnboardingPermissionCatalogUseCase {
  const ReadOnboardingPermissionCatalogUseCase(this._healthRepository);

  final HealthRepository _healthRepository;

  OnboardingPermissionCatalog call({required bool mindfulnessAvailable}) {
    final repo = _healthRepository;
    final categories = <OnboardingPermissionCategory>[
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
        available: mindfulnessAvailable,
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

    return OnboardingPermissionCatalog(
      categories: categories,
      minimumPermissions: repo.minimumOnboardingPermissions,
      allPermissions: repo.onboardingPermissions,
    );
  }
}
