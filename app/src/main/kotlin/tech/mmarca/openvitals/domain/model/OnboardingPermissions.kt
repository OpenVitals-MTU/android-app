package tech.mmarca.openvitals.domain.model

/**
 * The onboarding permission rows, grouped the way Health Connect itself groups
 * permissions — so a row named "Activity" produces a system dialog headed
 * "Activity" — rather than by app feature. Each category carries read and
 * write together: entries the user creates in the app go back to Health
 * Connect too.
 */
enum class OnboardingCategoryId {
    ACTIVITY,
    BODY,
    NUTRITION,
    SLEEP,
    VITALS,
    CYCLE_TRACKING,
    MINDFULNESS,
    ADDITIONAL_ACCESS,
}

data class OnboardingPermissionCategory(
    val id: OnboardingCategoryId,
    val permissions: Set<String>,
    val required: Boolean = false,
    val available: Boolean = true,
)

data class OnboardingPermissionCatalog(
    val categories: List<OnboardingPermissionCategory>,
    /** Activity + Sleep, minus manual-only — the gate for finishing step one. */
    val requiredPermissions: Set<String>,
    /**
     * Health Connect keeps route READS behind a setting no app can request, so
     * this permission belongs to no category; step four walks the user through
     * granting it by hand while it is outstanding.
     */
    val routeReadPermission: String,
    val mindfulnessSupportedByDevice: Boolean,
) {
    fun category(id: OnboardingCategoryId): OnboardingPermissionCategory? =
        categories.firstOrNull { it.id == id }
}
