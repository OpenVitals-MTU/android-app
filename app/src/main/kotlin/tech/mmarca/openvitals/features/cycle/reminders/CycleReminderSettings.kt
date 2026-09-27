package tech.mmarca.openvitals.features.cycle.reminders

import tech.mmarca.openvitals.domain.model.CycleReminderConfig

/**
 * What a screen may do with the cycle reminders. The controller behind it
 * needs a Context for the alarms; a screen needs only these three.
 */
interface CycleReminderSettings {

    fun config(): CycleReminderConfig

    /** Stores the config after normalizing it and re-plans the alarms. */
    fun updateConfig(config: CycleReminderConfig)

    /** Re-plans the alarms from the stored config, after a day log or a cycle change. */
    fun applyStoredConfig()
}
