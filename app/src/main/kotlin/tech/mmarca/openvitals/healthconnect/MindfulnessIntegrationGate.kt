package tech.mmarca.openvitals.healthconnect

import tech.mmarca.openvitals.data.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Gates the mindfulness integration behind the settings toggle. Disabled empties its permission sets. */
@Singleton
class MindfulnessIntegrationGate @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    val isEnabled: Boolean
        get() = preferencesRepository.healthConnectMindfulnessEnabled
}
