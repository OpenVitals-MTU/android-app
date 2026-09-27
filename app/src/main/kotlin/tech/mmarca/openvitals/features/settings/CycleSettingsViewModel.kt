package tech.mmarca.openvitals.features.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.CyclePreferences
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.CycleReminderVisibility
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderController
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderSettings
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler

@Immutable
data class CycleSettingsUiState(
    val profile: CycleTrackingProfile = CycleTrackingProfile(),
    /** The band the body profile's birth year gives, or null when none is set. */
    val derivedAgeBand: AgeBand? = null,
    val reminders: CycleReminderConfig = CycleReminderConfig(),
    val hasNotificationPermission: Boolean = true,
    val isDeletingJournal: Boolean = false,
)

/** The cycle section: declared contexts, the age band, the reminders, and the journal's off switch. */
@HiltViewModel
class CycleSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: CyclePreferences,
    private val bodyProfilePreferences: BodyProfilePreferences,
    private val journal: CycleJournalRepository,
    private val reminders: CycleReminderSettings,
    private val homeWidgetRefreshScheduler: HomeWidgetRefreshScheduler? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(snapshot())
    val uiState: StateFlow<CycleSettingsUiState> = _uiState.asStateFlow()

    /** Set while the notification prompt is up, so a grant turns the reminders on. */
    private var enableAfterPermission = false

    fun refresh() {
        _uiState.value = snapshot()
    }

    fun requestEnableAfterPermission() {
        enableAfterPermission = true
    }

    /** Enabling before the grant would plan alarms the controller cancels at once. */
    fun onNotificationPermissionResult(granted: Boolean) {
        refresh()
        if (granted && enableAfterPermission) setRemindersEnabled(true)
        enableAfterPermission = false
    }

    fun setContext(trackingContext: TrackingContext, enabled: Boolean) {
        val profile = _uiState.value.profile
        val contexts = if (enabled) profile.contexts + trackingContext else profile.contexts - trackingContext
        saveProfile(profile.copy(contexts = contexts))
    }

    fun setAgeBand(band: AgeBand?) = saveProfile(_uiState.value.profile.copy(ageBand = band))

    fun setRemindersEnabled(enabled: Boolean) = updateReminders { copy(enabled = enabled) }

    fun setDailyCheckIn(enabled: Boolean) = updateReminders { copy(dailyCheckInEnabled = enabled) }

    fun setDailyCheckInTime(time: LocalTime) = updateReminders { copy(dailyCheckInTime = time) }

    fun setPeriodWindow(enabled: Boolean) = updateReminders { copy(periodWindowEnabled = enabled) }

    fun setPeriodWindowLeadDays(days: Int) = updateReminders { copy(periodWindowLeadDays = days) }

    fun setLateCycle(enabled: Boolean) = updateReminders { copy(lateCycleEnabled = enabled) }

    fun setLateCycleGraceDays(days: Int) = updateReminders { copy(lateCycleGraceDays = days) }

    fun setVisibility(visibility: CycleReminderVisibility) = updateReminders { copy(visibility = visibility) }

    fun setCustomTitle(title: String) = updateReminders { copy(customTitle = title) }

    fun setCustomBody(body: String) = updateReminders { copy(customBody = body) }

    /** Wipes the journal, the exclusions and every cycle setting on this device. Health Connect is not touched. */
    fun deleteCycleJournal() {
        if (_uiState.value.isDeletingJournal) return
        _uiState.value = _uiState.value.copy(isDeletingJournal = true)
        viewModelScope.launch {
            runCatching { journal.deleteAll() }
            // Disabled first: the controller cancels the alarms and any posted notification.
            reminders.updateConfig(CycleReminderConfig())
            preferences.clearCyclePreferences()
            homeWidgetRefreshScheduler?.refreshNow()
            _uiState.value = snapshot()
        }
    }

    private fun saveProfile(profile: CycleTrackingProfile) {
        preferences.setCycleTrackingProfile(profile)
        // The estimate's width follows the profile, so the reminders and the widget that show it move too.
        reminders.applyStoredConfig()
        homeWidgetRefreshScheduler?.refreshNow()
        _uiState.value = _uiState.value.copy(profile = profile)
    }

    private inline fun updateReminders(transform: CycleReminderConfig.() -> CycleReminderConfig) {
        val config = _uiState.value.reminders.transform().normalized()
        reminders.updateConfig(config)
        _uiState.value = _uiState.value.copy(reminders = config)
    }

    private fun snapshot() = CycleSettingsUiState(
        profile = preferences.cycleTrackingProfile(),
        derivedAgeBand = bodyProfilePreferences.bodyProfile().ageYears()?.let(AgeBand::forAge),
        reminders = reminders.config(),
        hasNotificationPermission = CycleReminderController.hasNotificationPermission(context),
    )
}
