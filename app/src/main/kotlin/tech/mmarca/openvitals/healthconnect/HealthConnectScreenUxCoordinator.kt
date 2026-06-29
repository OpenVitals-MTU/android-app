package tech.mmarca.openvitals.healthconnect

import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectScreenUxCoordinator @Inject constructor(
    private val healthRepository: HealthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val permissionUxState: HealthConnectPermissionUxState,
    private val healthConnectManager: HealthConnectManager,
) {
    val syncEnabledFlow get() = preferencesRepository.healthConnectSyncEnabledFlow

    var syncEnabled: Boolean
        get() = preferencesRepository.healthConnectSyncEnabled
        set(value) {
            preferencesRepository.healthConnectSyncEnabled = value
        }

    suspend fun loadState(
        feature: HealthConnectFeature,
        isLoading: Boolean = false,
    ): HealthConnectScreenUxState {
        val availability = healthRepository.availability()
        val granted = if (availability == HealthConnectAvailability.AVAILABLE) {
            healthRepository.grantedPermissions()
        } else {
            emptySet()
        }
        return buildHealthConnectScreenUxState(
            feature = feature,
            manager = healthConnectManager,
            availability = availability,
            syncEnabled = preferencesRepository.healthConnectSyncEnabled,
            grantedPermissions = granted,
            showDoubleCancelRecovery = permissionUxState.shouldShowDoubleCancelRecovery(),
            acknowledgedPermissions = preferencesRepository.acknowledgedPermissionsFor(feature),
            isLoading = isLoading,
        )
    }

    fun acknowledgeFeaturePermissions(feature: HealthConnectFeature, permissions: Set<String>) {
        preferencesRepository.acknowledgePermissionsFor(feature, permissions)
    }

    fun recordPermissionRequestCancelled() {
        permissionUxState.recordPermissionRequestCancelled()
    }

    fun recordPermissionRequestGranted() {
        permissionUxState.recordPermissionRequestGranted()
    }

    suspend fun shouldShowNewPermissionsDialog(): Boolean = false

    fun markNewPermissionsPrompted() {
        preferencesRepository.lastPromptedPermissionSetVersion = HealthConnectPermissionService.PERMISSION_SET_VERSION
    }
}
