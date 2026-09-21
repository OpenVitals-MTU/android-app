package tech.mmarca.openvitals.features.mindfulness.reminders

import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig

/**
 * What the Mindfulness screen may do with its reminder.
 *
 * The controller behind it needs a Context for the alarm; a screen needs only these three.
 */
interface MindfulnessReminderSettings {

    fun config(): MindfulnessReminderConfig

    /** Stores the config after normalizing it and re-plans the alarm. */
    fun updateConfig(config: MindfulnessReminderConfig)

    /** Re-plans the alarm from the stored config, after a change it depends on. */
    fun applyStoredConfig()
}
