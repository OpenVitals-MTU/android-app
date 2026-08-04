package tech.mmarca.openvitals.healthconnect

import tech.mmarca.openvitals.data.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectPermissionUxState @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    fun recordPermissionRequestCancelled() {
        preferencesRepository.healthConnectPermissionCancelCount =
            preferencesRepository.healthConnectPermissionCancelCount + 1
    }

    fun recordPermissionRequestGranted() {
        preferencesRepository.healthConnectPermissionCancelCount = 0
    }

    fun shouldShowDoubleCancelRecovery(): Boolean =
        preferencesRepository.healthConnectPermissionCancelCount >= DOUBLE_CANCEL_THRESHOLD

    fun resetCancelCount() {
        preferencesRepository.healthConnectPermissionCancelCount = 0
    }

    companion object {
        const val DOUBLE_CANCEL_THRESHOLD = 2
    }
}
