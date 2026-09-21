package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.model.MindfulnessTimerConfig

/** How the mindfulness timer was last set up: length, interval bell and sound. */
interface MindfulnessTimerPreferences {

    fun mindfulnessTimerConfig(): MindfulnessTimerConfig

    fun setMindfulnessTimerConfig(config: MindfulnessTimerConfig)
}
