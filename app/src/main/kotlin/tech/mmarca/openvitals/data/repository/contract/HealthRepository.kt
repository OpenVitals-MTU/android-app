package tech.mmarca.openvitals.data.repository.contract

import androidx.activity.result.contract.ActivityResultContract
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.PermissionGrantMode

interface HealthRepository {
    fun availability(): HealthConnectAvailability

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>>

    val phase1Permissions: Set<String>
    val minimumOnboardingPermissions: Set<String>
    val phase2Permissions: Set<String>
    val phase3Permissions: Set<String>
    val phase4Permissions: Set<String>
    val corePermissions: Set<String>
    val routePermissions: Set<String>
    val activityWritePermissions: Set<String>
    val heartPermissions: Set<String>
    val bodyPermissions: Set<String>
    val bodyWritePermissions: Set<String>
    val activityExtrasPermissions: Set<String>
    val nutritionHydrationPermissions: Set<String>
    val hydrationWritePermissions: Set<String>
    val mindfulnessPermissions: Set<String>
    val mindfulnessWritePermissions: Set<String>
    val additionalDataAccessPermissions: Set<String>
    val vitalsPermissions: Set<String>
    val vitalsWritePermissions: Set<String>
    val dataImportWritePermissions: Set<String>
    val cyclePermissions: Set<String>
    val manualOnlyPermissions: Set<String>
    val requestableWritePermissions: Set<String>
    val onboardingPermissions: Set<String>
    val allPermissions: Set<String>
    val managedPermissions: Set<String>

    fun grantModeFor(permission: String): PermissionGrantMode

    fun isMindfulnessAvailable(): Boolean

    suspend fun grantedPermissions(): Set<String>

    suspend fun missingPhase1(): Set<String>
}
