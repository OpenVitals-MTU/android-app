package tech.mmarca.openvitals.healthconnect

import tech.mmarca.openvitals.data.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gates the mindfulness Health Connect integration behind the settings toggle.
 * When disabled, the declared mindfulness permission sets are empty and the
 * feature reads as unavailable everywhere — settings category, onboarding,
 * and manual entry included.
 */
@Singleton
class MindfulnessIntegrationGate @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    val isEnabled: Boolean
        get() = preferencesRepository.healthConnectMindfulnessEnabled
}
