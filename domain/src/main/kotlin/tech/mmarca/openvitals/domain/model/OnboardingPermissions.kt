package tech.mmarca.openvitals.domain.model

/** The onboarding rows, grouped as Health Connect groups permissions. Each carries read and write. */
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
    /** Route reads sit behind a setting no app can request; step four walks the user through it. */
    val routeReadPermission: String,
    val mindfulnessSupportedByDevice: Boolean,
) {
    fun category(id: OnboardingCategoryId): OnboardingPermissionCategory? =
        categories.firstOrNull { it.id == id }
}
