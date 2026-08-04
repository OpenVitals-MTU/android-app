package tech.mmarca.openvitals.healthconnect

import kotlinx.coroutines.flow.StateFlow
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

class HealthConnectSyncDisabledException : IllegalStateException("Health Connect sync is paused")

@Singleton
class HealthConnectSyncGate @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    val isEnabled: Boolean
        get() = preferencesRepository.healthConnectSyncEnabled

    val isEnabledFlow: StateFlow<Boolean>
        get() = preferencesRepository.healthConnectSyncEnabledFlow

    fun requireEnabled() {
        if (!isEnabled) throw HealthConnectSyncDisabledException()
    }
}
