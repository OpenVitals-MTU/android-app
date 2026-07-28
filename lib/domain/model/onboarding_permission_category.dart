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

/// Everything onboarding can ask for, grouped the way it asks for it.
class OnboardingPermissionCatalog {
  const OnboardingPermissionCatalog({
    required this.categories,
    required this.requiredPermissions,
    required this.allPermissions,
  });

  /// The rows, in the order they are shown.
  final List<OnboardingPermissionCategory> categories;

  /// The permissions onboarding will not finish without — everything the runtime
  /// dialog can grant on this device, asked for in one request. The opt-in rows
  /// (cycle tracking, mindfulness) and the ones the dialog cannot grant
  /// (exercise routes, history/background access) are deliberately not in here.
  final Set<String> requiredPermissions;

  /// Every permission onboarding offers, required and opt-in alike.
  final Set<String> allPermissions;
}
