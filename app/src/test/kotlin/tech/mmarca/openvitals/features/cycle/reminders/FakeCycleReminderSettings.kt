package tech.mmarca.openvitals.features.cycle.reminders

import tech.mmarca.openvitals.domain.model.CycleReminderConfig

/** Records the calls; a test reads back how often the alarms were re-planned. */
class FakeCycleReminderSettings(
    private var stored: CycleReminderConfig = CycleReminderConfig(),
) : CycleReminderSettings {
    var applied = 0
        private set

    override fun config(): CycleReminderConfig = stored

    override fun updateConfig(config: CycleReminderConfig) {
        stored = config.normalized()
        applied++
    }

    override fun applyStoredConfig() {
        applied++
    }
}
