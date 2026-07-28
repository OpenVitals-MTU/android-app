import '../../data/repository/contract/health_repository.dart';
import '../model/onboarding_permission_category.dart';

/// Assembles the permission rows onboarding offers — **synchronously**, because
/// they are a static description of what the app can ask for, not a question for
/// the platform.
///
/// The grouping is the point, and it is **Health Connect's own**: Activity, Body
/// measurements, Nutrition, Sleep, Vitals, Cycle tracking, Mindfulness. Those are
/// the exact headers the system permission dialog draws, so a row labelled
/// "Activity" opens a dialog labelled "Activity". An earlier version grouped by
/// app feature instead — `heart_recovery`, `activity_extras`, `manual_entry_write`,
/// `data_import_write` — which described the app's internals to someone who was
/// about to be shown Android's.
///
/// Each category carries **read and write together**, so granting one is a single
/// decision rather than two.
///
/// Only Activity and Sleep are required. The rest are asked for and can be
/// skipped: the dashboard renders nothing without those two, and a first run that
/// blocks on all seven is one a single stray refusal can trap a user inside.
///
/// Two rows are not a free choice:
///
/// * Mindfulness is offered only where the provider has the feature AND the user
///   opted in — hence [mindfulnessAvailable] as the input. Some providers crash
///   their own permission UI when asked for it, so it is requested alone.
/// * Exercise routes cannot be granted by the runtime dialog at all. They ride
///   with the "additional data access" row but are flagged manual, so the screen
///   sends the user to Health Connect instead of firing a dialog that would
///   silently ignore them.
///
/// An empty group is dropped rather than rendered as a row that grants nothing.
class ReadOnboardingPermissionCatalogUseCase {
  const ReadOnboardingPermissionCatalogUseCase(this._healthRepository);

  final HealthRepository _healthRepository;

  OnboardingPermissionCatalog call({required bool mindfulnessAvailable}) {
    final repo = _healthRepository;
    final categories = <OnboardingPermissionCategory>[
      // ── Step 1: the five categories every install is offered ──────────────
      OnboardingPermissionCategory(
        id: 'activity',
        permissions: repo.activityCategoryPermissions,
        isRequired: true,
      ),
      OnboardingPermissionCategory(
        id: 'body',
        permissions: repo.bodyCategoryPermissions,
      ),
      OnboardingPermissionCategory(
        id: 'nutrition',
        permissions: repo.nutritionCategoryPermissions,
      ),
      OnboardingPermissionCategory(
        id: 'sleep',
        permissions: repo.sleepCategoryPermissions,
        isRequired: true,
      ),
      OnboardingPermissionCategory(
        id: 'vitals',
        permissions: repo.vitalsCategoryPermissions,
      ),

      // ── Step 2 ────────────────────────────────────────────────────────────
      OnboardingPermissionCategory(
        id: 'mindfulness',
        permissions: repo.mindfulnessCategoryPermissions,
        available: mindfulnessAvailable,
      ),

      // ── Step 3 ────────────────────────────────────────────────────────────
      OnboardingPermissionCategory(
        id: 'cycle_tracking',
        permissions: repo.cycleCategoryPermissions,
      ),

      // ── Step 4 ────────────────────────────────────────────────────────────
      // History and background reads ONLY — both grantable by the dialog, so
      // this row can actually reach "granted".
      //
      // Exercise routes are deliberately not here. They need the "Always allow"
      // toggle inside Health Connect, which no intent can deep-link to, so the
      // step gives them their own walkthrough. Counting them in this row made it
      // read "2 of 3" no matter what the user did — the third could never be
      // granted from anywhere the row's button leads.
      OnboardingPermissionCategory(
        id: 'additional_data_access',
        permissions: repo.additionalDataAccessPermissions,
      ),
    ].where((category) => category.permissions.isNotEmpty).toList();

    return OnboardingPermissionCatalog(
      categories: categories,
      requiredPermissions: repo.requiredOnboardingPermissions,
      allPermissions: repo.onboardingPermissions,
    );
  }
}
