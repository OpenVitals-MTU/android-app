package tech.mmarca.openvitals.features.hydration.reminders

import tech.mmarca.openvitals.domain.model.HydrationReminderConfig

/** In-memory stand-in for the reminder controller: keeps the config, counts the re-plans. */
class FakeHydrationReminderSettings(
    private var stored: HydrationReminderConfig = HydrationReminderConfig(),
) : HydrationReminderSettings {

    /** Every config the screen stored, in order, as the controller normalized it. */
    val updates = mutableListOf<HydrationReminderConfig>()

    /** How often the screen asked for a re-plan from the stored config. */
    var appliedFromStore = 0
        private set

    override fun config(): HydrationReminderConfig = stored

    override fun updateConfig(config: HydrationReminderConfig) {
        stored = config.normalized()
        updates += stored
    }

    override fun applyStoredConfig() {
        appliedFromStore++
    }
}
