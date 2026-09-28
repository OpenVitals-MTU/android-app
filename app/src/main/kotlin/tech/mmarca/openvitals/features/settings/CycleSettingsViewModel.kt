package tech.mmarca.openvitals.features.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.CyclePreferences
import tech.mmarca.openvitals.data.repository.contract.PillIntakeRepository
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.CycleReminderVisibility
import tech.mmarca.openvitals.domain.model.PillPlan
import tech.mmarca.openvitals.features.cycle.cycleJournalExportJson
import tech.mmarca.openvitals.features.cycle.parseCycleJournalExport
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderController
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderSettings
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler

/** What the last backup action did, shown once under the card. */
enum class CycleBackupMessage {
    EXPORTED,
    IMPORTED,
    IMPORT_FAILED,
}

@Immutable
data class CycleSettingsUiState(
    val profile: CycleTrackingProfile = CycleTrackingProfile(),
    /** The band the body profile's birth year gives, or null when none is set. */
    val derivedAgeBand: AgeBand? = null,
    val reminders: CycleReminderConfig = CycleReminderConfig(),
    val pill: PillPlan = PillPlan(),
    val hasNotificationPermission: Boolean = true,
    val isDeletingJournal: Boolean = false,
    val backupMessage: CycleBackupMessage? = null,
    val importedDays: Int = 0,
)

/** The cycle section: declared contexts, the age band, the reminders, the pill scheme, and the journal's off switch. */
@HiltViewModel
class CycleSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: CyclePreferences,
    private val bodyProfilePreferences: BodyProfilePreferences,
    private val journal: CycleJournalRepository,
    private val pillIntakes: PillIntakeRepository,
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

    /** Turning the pill on without a pack start counts today as one. */
    fun setPillEnabled(enabled: Boolean) = updatePill { copy(enabled = enabled, packStart = packStart ?: LocalDate.now()) }

    fun setPillActiveDays(days: Int) = updatePill { copy(activeDays = days) }

    fun setPillPauseDays(days: Int) = updatePill { copy(pauseDays = days) }

    fun setPillPackStart(date: LocalDate) = updatePill { copy(packStart = date) }

    fun setPillReminder(enabled: Boolean) = updatePill { copy(reminderEnabled = enabled) }

    fun setPillReminderTime(time: LocalTime) = updatePill { copy(reminderTime = time) }

    /** The whole journal as the backup file's text. */
    suspend fun exportJson(): String = cycleJournalExportJson(
        entries = journal.allEntries(),
        exclusions = journal.exclusions(),
        profile = preferences.cycleTrackingProfile(),
        exportedAt = Instant.now(),
    )

    fun onExported() {
        _uiState.value = _uiState.value.copy(backupMessage = CycleBackupMessage.EXPORTED)
    }

    /**
     * Merges a backup file: a day both sides hold keeps the newer edit,
     * exclusions are added, and the contexts and age band fill a phone that
     * declared none. A file that is not a journal export imports nothing.
     */
    fun importJson(text: String) {
        val import = parseCycleJournalExport(text)
        if (import == null) {
            _uiState.value = _uiState.value.copy(backupMessage = CycleBackupMessage.IMPORT_FAILED)
            return
        }
        viewModelScope.launch {
            var imported = 0
            for (entry in import.entries) {
                val local = journal.entry(entry.date)
                if (local == null || entry.updatedAt.isAfter(local.updatedAt)) {
                    journal.restore(entry)
                    imported += 1
                }
            }
            import.exclusions.forEach { (start, reason) -> journal.exclude(start, start, reason) }
            val current = preferences.cycleTrackingProfile()
            val incoming = import.profile
            if (incoming != null && current.contexts.isEmpty() && current.ageBand == null &&
                (incoming.contexts.isNotEmpty() || incoming.ageBand != null)
            ) {
                preferences.setCycleTrackingProfile(incoming)
            }
            reminders.applyStoredConfig()
            homeWidgetRefreshScheduler?.refreshNow()
            _uiState.value = snapshot().copy(backupMessage = CycleBackupMessage.IMPORTED, importedDays = imported)
        }
    }

    /** Wipes the journal, the exclusions and every cycle setting on this device. Health Connect is not touched. */
    fun deleteCycleJournal() {
        if (_uiState.value.isDeletingJournal) return
        _uiState.value = _uiState.value.copy(isDeletingJournal = true)
        viewModelScope.launch {
            runCatching { journal.deleteAll() }
            runCatching { pillIntakes.deleteAll() }
            // Disabled first: the controller cancels the alarms and any posted notification.
            reminders.updateConfig(CycleReminderConfig())
            preferences.clearCyclePreferences()
            // The pill alarm follows the plan just cleared.
            reminders.applyStoredConfig()
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

    /** Every edit is stamped, so the newer scheme wins when two phones sync. */
    private inline fun updatePill(transform: PillPlan.() -> PillPlan) {
        val plan = _uiState.value.pill.transform().copy(updatedAt = Instant.now()).normalized()
        preferences.setPillPlan(plan)
        reminders.applyStoredConfig()
        _uiState.value = _uiState.value.copy(pill = plan)
    }

    private fun snapshot() = CycleSettingsUiState(
        profile = preferences.cycleTrackingProfile(),
        derivedAgeBand = bodyProfilePreferences.bodyProfile().ageYears()?.let(AgeBand::forAge),
        reminders = reminders.config(),
        pill = preferences.pillPlan(),
        hasNotificationPermission = CycleReminderController.hasNotificationPermission(context),
    )
}
