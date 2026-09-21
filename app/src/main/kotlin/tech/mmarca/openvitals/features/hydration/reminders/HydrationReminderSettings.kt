package tech.mmarca.openvitals.features.hydration.reminders

import tech.mmarca.openvitals.domain.model.HydrationReminderConfig

/**
 * What the Hydration screen may do with its reminders.
 *
 * The controller behind it needs a Context for the alarm; a screen needs only these three.
 */
interface HydrationReminderSettings {

    fun config(): HydrationReminderConfig

    /** Stores the config after normalizing it and re-plans the alarm. */
    fun updateConfig(config: HydrationReminderConfig)

    /** Re-plans the alarm from the stored config, after a change it depends on. */
    fun applyStoredConfig()
}
