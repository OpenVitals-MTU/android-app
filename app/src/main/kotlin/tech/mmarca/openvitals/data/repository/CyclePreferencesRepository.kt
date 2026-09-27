package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.mmarca.openvitals.data.repository.contract.CyclePreferences
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.CycleReminderVisibility

/**
 * The cycle settings, in their own preference file. They stay out of
 * [PreferencesRepository], which is at its size ceiling.
 */
@Singleton
class CyclePreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : CyclePreferences {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val profile = MutableStateFlow(readProfile())

    override fun cycleTrackingProfile(): CycleTrackingProfile = profile.value

    override val cycleTrackingProfileFlow: Flow<CycleTrackingProfile> = profile.asStateFlow()

    override fun setCycleTrackingProfile(profile: CycleTrackingProfile) {
        prefs.edit {
            putString(KEY_CONTEXTS, profile.contexts.map { it.id }.sorted().joinToString(SEPARATOR))
            if (profile.ageBand == null) remove(KEY_AGE_BAND) else putString(KEY_AGE_BAND, profile.ageBand.id)
        }
        this.profile.value = profile
    }

    override fun cycleReminderConfig(): CycleReminderConfig = CycleReminderConfig(
        enabled = prefs.getBoolean(KEY_REMINDERS_ENABLED, false),
        dailyCheckInEnabled = prefs.getBoolean(KEY_DAILY_ENABLED, false),
        dailyCheckInTime = prefs.getString(KEY_DAILY_TIME, null)?.let { stored ->
            runCatching { LocalTime.parse(stored) }.getOrNull()
        } ?: CycleReminderConfig.DefaultCheckInTime,
        periodWindowEnabled = prefs.getBoolean(KEY_WINDOW_ENABLED, false),
        periodWindowLeadDays = prefs.getInt(KEY_WINDOW_LEAD_DAYS, 2),
        lateCycleEnabled = prefs.getBoolean(KEY_LATE_ENABLED, false),
        lateCycleGraceDays = prefs.getInt(KEY_LATE_GRACE_DAYS, 1),
        visibility = prefs.getString(KEY_VISIBILITY, null)?.let { stored ->
            CycleReminderVisibility.entries.firstOrNull { it.name == stored }
        } ?: CycleReminderVisibility.CONCEALED,
        customTitle = prefs.getString(KEY_CUSTOM_TITLE, null).orEmpty(),
        customBody = prefs.getString(KEY_CUSTOM_BODY, null).orEmpty(),
    ).normalized()

    override fun setCycleReminderConfig(config: CycleReminderConfig) {
        val normalized = config.normalized()
        prefs.edit {
            putBoolean(KEY_REMINDERS_ENABLED, normalized.enabled)
            putBoolean(KEY_DAILY_ENABLED, normalized.dailyCheckInEnabled)
            putString(KEY_DAILY_TIME, normalized.dailyCheckInTime.toString())
            putBoolean(KEY_WINDOW_ENABLED, normalized.periodWindowEnabled)
            putInt(KEY_WINDOW_LEAD_DAYS, normalized.periodWindowLeadDays)
            putBoolean(KEY_LATE_ENABLED, normalized.lateCycleEnabled)
            putInt(KEY_LATE_GRACE_DAYS, normalized.lateCycleGraceDays)
            putString(KEY_VISIBILITY, normalized.visibility.name)
            putString(KEY_CUSTOM_TITLE, normalized.customTitle)
            putString(KEY_CUSTOM_BODY, normalized.customBody)
        }
    }

    override fun clearCyclePreferences() {
        prefs.edit { clear() }
        profile.value = CycleTrackingProfile()
    }

    private fun readProfile(): CycleTrackingProfile = CycleTrackingProfile(
        contexts = prefs.getString(KEY_CONTEXTS, null).orEmpty()
            .split(SEPARATOR)
            .mapNotNullTo(mutableSetOf()) { TrackingContext.fromId(it.trim()) },
        ageBand = AgeBand.fromId(prefs.getString(KEY_AGE_BAND, null)),
    )

    private companion object {
        const val PREFS_FILE = "openvitals_cycle_preferences"
        const val SEPARATOR = ","
        const val KEY_CONTEXTS = "tracking_contexts"
        const val KEY_AGE_BAND = "age_band"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        const val KEY_DAILY_ENABLED = "daily_check_in_enabled"
        const val KEY_DAILY_TIME = "daily_check_in_time"
        const val KEY_WINDOW_ENABLED = "period_window_enabled"
        const val KEY_WINDOW_LEAD_DAYS = "period_window_lead_days"
        const val KEY_LATE_ENABLED = "late_cycle_enabled"
        const val KEY_LATE_GRACE_DAYS = "late_cycle_grace_days"
        const val KEY_VISIBILITY = "reminder_visibility"
        const val KEY_CUSTOM_TITLE = "reminder_custom_title"
        const val KEY_CUSTOM_BODY = "reminder_custom_body"
    }
}
