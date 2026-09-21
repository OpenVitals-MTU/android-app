package tech.mmarca.openvitals.features.mindfulness.reminders

import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig

/** In-memory stand-in for the reminder controller: keeps the config, counts the re-plans. */
class FakeMindfulnessReminderSettings(
    private var stored: MindfulnessReminderConfig = MindfulnessReminderConfig(),
) : MindfulnessReminderSettings {

    /** Every config the screen stored, in order, as the controller normalized it. */
    val updates = mutableListOf<MindfulnessReminderConfig>()

    /** How often the screen asked for a re-plan from the stored config. */
    var appliedFromStore = 0
        private set

    override fun config(): MindfulnessReminderConfig = stored

    override fun updateConfig(config: MindfulnessReminderConfig) {
        stored = config.normalized()
        updates += stored
    }

    override fun applyStoredConfig() {
        appliedFromStore++
    }
}
