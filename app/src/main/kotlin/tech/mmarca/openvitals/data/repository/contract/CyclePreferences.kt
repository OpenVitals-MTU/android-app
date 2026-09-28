package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.PillPlan

/** The cycle screens' settings: declared contexts, the age band, and the reminders. */
interface CyclePreferences {

    fun cycleTrackingProfile(): CycleTrackingProfile

    val cycleTrackingProfileFlow: Flow<CycleTrackingProfile>

    fun setCycleTrackingProfile(profile: CycleTrackingProfile)

    fun cycleReminderConfig(): CycleReminderConfig

    /** Stores the config after normalizing it. */
    fun setCycleReminderConfig(config: CycleReminderConfig)

    fun pillPlan(): PillPlan

    /** Stores the plan after normalizing it. */
    fun setPillPlan(plan: PillPlan)

    /** Forgets every cycle setting: contexts, age band and reminders. */
    fun clearCyclePreferences()
}
