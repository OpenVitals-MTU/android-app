package tech.mmarca.openvitals.data.repository

import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : HealthRepository {

    override fun availability(): HealthConnectAvailability = hc.availability()

    override fun permissionContract() = hc.permissionContract()

    override val phase1Permissions get() = hc.phase1Permissions
    override val minimumOnboardingPermissions get() = hc.minimumOnboardingPermissions
    override val phase2Permissions get() = hc.phase2Permissions
    override val phase3Permissions get() = hc.phase3Permissions
    override val phase4Permissions get() = hc.phase4Permissions
    override val corePermissions get() = hc.corePermissions
    override val routePermissions get() = hc.routePermissions
    override val activityWritePermissions get() = hc.activityWritePermissions
    override val heartPermissions get() = hc.heartPermissions
    override val bodyPermissions get() = hc.bodyPermissions
    override val bodyWritePermissions get() = hc.bodyWritePermissions
    override val activityExtrasPermissions get() = hc.activityExtrasPermissions
    override val nutritionHydrationPermissions get() = hc.nutritionHydrationPermissions
    override val hydrationWritePermissions get() = hc.hydrationWritePermissions
    override val mindfulnessPermissions get() = hc.mindfulnessPermissions
    override val mindfulnessWritePermissions get() = hc.mindfulnessWritePermissions
    override val additionalDataAccessPermissions get() = hc.additionalDataAccessPermissions
    override val vitalsPermissions get() = hc.vitalsPermissions
    override val vitalsWritePermissions get() = hc.vitalsWritePermissions
    override val dataImportWritePermissions get() = hc.dataImportWritePermissions
    override val cyclePermissions get() = hc.cyclePermissions
    override val manualOnlyPermissions get() = hc.manualOnlyPermissions
    override val requestableWritePermissions get() = hc.requestableWritePermissions
    override val onboardingPermissions get() = hc.onboardingRequestablePermissions
    override val allPermissions get() = hc.allPermissions
    override val managedPermissions get() = hc.managedPermissions
    override fun grantModeFor(permission: String) = hc.grantModeFor(permission)

    override fun isMindfulnessAvailable(): Boolean = hc.isMindfulnessSessionAvailable()

    override suspend fun grantedPermissions(): Set<String> = hc.grantedPermissions()

    override suspend fun missingPhase1(): Set<String> {
        val granted = hc.grantedPermissions()
        return hc.phase1Permissions.filterNot { it in granted }.toSet()
    }
}
