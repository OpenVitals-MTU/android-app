package tech.mmarca.openvitals.features.cycle

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.navigation.selectedDayOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.cycle.NightlyVitals
import tech.mmarca.openvitals.domain.usecase.LoadCycleNightlyVitalsUseCase
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.CyclePreferences
import tech.mmarca.openvitals.data.repository.contract.PillIntakeRepository
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderSettings
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class CycleUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val data: CycleData = CycleData(),
    val display: CycleDisplayState = CycleDisplayState(),
    val statistics: CycleStatistics? = null,
    val journalEntries: List<CycleJournalEntry> = emptyList(),
    val missingPermissions: Set<String> = emptySet(),
    /** The current cycle's nightly signals, or null before they loaded or without a current cycle. */
    val nightly: NightlyVitals? = null,
    /** The optional overnight reads not granted yet; the card offers a button while this is not empty. */
    val missingNightlyPermissions: Set<String> = emptySet(),
    val error: ScreenError? = null,
)

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val repository: CycleRepository,
    private val periodPreferences: PeriodPreferences,
    private val journal: CycleJournalRepository,
    private val pillIntakes: PillIntakeRepository,
    private val cyclePreferences: CyclePreferences,
    private val bodyProfilePreferences: BodyProfilePreferences,
    private val reminders: CycleReminderSettings,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val homeWidgetRefreshScheduler: HomeWidgetRefreshScheduler? = null,
    private val nightlyVitals: LoadCycleNightlyVitalsUseCase? = null,
) : ViewModel() {

    private val initialDate = savedStateHandle.selectedDayOrNull()
    private val initialWeekPeriodMode = periodPreferences.weekPeriodMode

    // The screen is a month calendar, so the range is fixed; the arrows move month by month.
    private val periodDriver = PeriodSelectionDriver(
        initialRange = TimeRange.MONTH,
        initialDate = initialDate ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
    )
    private val _uiState = MutableStateFlow(
        CycleUiState(
            selectedRange = TimeRange.MONTH,
            weekPeriodMode = initialWeekPeriodMode,
        )
    )
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()
    private val nightlyCoordinator = LoadCoordinator()

    /** The cycle start and day the overnight values on screen were read for. */
    private var nightlyLoadedFor: Pair<LocalDate, LocalDate>? = null

    val cyclePermissions: Set<String> get() = repository.phase4Permissions

    init {
        observeWeekPeriodMode()
        load()
    }

    private fun observeWeekPeriodMode() {
        viewModelScope.launch {
            periodPreferences.weekPeriodModeFlow.drop(1).collect { mode ->
                periodDriver.weekPeriodMode = mode
                _uiState.value = _uiState.value.copy(weekPeriodMode = mode)
            }
        }
    }

    fun previousPeriod() {
        applyPeriodSelection(periodDriver.previousPeriod())
        loadPeriod(forceNightly = false)
    }

    fun nextPeriod() {
        periodDriver.nextPeriod()?.let { next ->
            applyPeriodSelection(next)
            loadPeriod(forceNightly = false)
        }
    }

    /** Moves the calendar to the month holding [date]. There is no day view to drill into. */
    fun selectDate(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDate(date))
        loadPeriod(forceNightly = false)
    }

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) loadPeriod(forceNightly = false)
            return
        }
        applyPeriodSelection(selection)
        loadPeriod(forceNightly = false)
    }

    fun onCyclePermissionsResult(granted: Set<String>) {
        load()
    }

    /** The optional overnight reads were asked for: read them again, whatever the answer. */
    fun onNightlyPermissionsResult() {
        load()
    }

    /** What the nightly card's button asks Health Connect for. */
    val nightlyPermissions: Set<String> get() = repository.cycleNightlyPermissions

    /** A full reload, the overnight values included (pull to refresh, a saved entry, a grant). */
    fun load() = loadPeriod(forceNightly = true)

    /**
     * Month navigation reloads the calendar only: the overnight values are of the current
     * cycle, which a browsed month does not change, so they are not read again.
     */
    private fun loadPeriod(forceNightly: Boolean) {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.loadCyclePeriod(query)
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val profile = cyclePreferences.cycleTrackingProfile().resolved(bodyProfilePreferences.bodyProfile())
                val pill = pillTodayDisplay()
                val display = withContext(dispatchers.default) {
                    CyclePresentationMapper.build(
                        query = query,
                        data = result.data,
                        statistics = result.statistics,
                        journalEntries = result.journalEntries,
                        allJournalEntries = result.allJournalEntries,
                        currentCycleTemperatures = result.currentCycleTemperatures,
                        currentCycleSkinTemperature = result.currentCycleSkinTemperature,
                        profile = profile,
                        pill = pill,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    data = result.data,
                    display = display,
                    statistics = result.statistics,
                    journalEntries = result.journalEntries,
                    missingPermissions = result.missingPermissions,
                )
                loadNightly(result.statistics?.currentCycle?.startDate, forceNightly)
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = error.toScreenError(),
                )
            }
        }
    }

    /**
     * The overnight signals are read after the screen shows, so the slow Health Connect
     * reads never hold the calendar back. They never feed the estimate.
     */
    private fun loadNightly(cycleStart: LocalDate?, force: Boolean) {
        val useCase = nightlyVitals
        if (useCase == null || cycleStart == null) {
            // No current cycle: whatever was reading is for a cycle that is gone.
            nightlyCoordinator.cancel()
            nightlyLoadedFor = null
            _uiState.value = _uiState.value.copy(nightly = null, missingNightlyPermissions = emptySet())
            return
        }
        val key = cycleStart to LocalDate.now()
        if (!force && nightlyLoadedFor == key && _uiState.value.nightly != null) return
        nightlyCoordinator.launch(viewModelScope) load@{
            val missing = runCatching { repository.missingNightlyPermissions() }.getOrDefault(emptySet())
            val vitals = withContext(dispatchers.default) { useCase(cycleStart) }
            if (!isCurrent) return@load
            nightlyLoadedFor = key
            _uiState.value = _uiState.value.copy(nightly = vitals, missingNightlyPermissions = missing)
        }
    }

    fun deleteCycleEntry(kind: CycleEntryKind, entryId: String) {
        if (entryId.isBlank()) return
        val previous = _uiState.value
        val prunedData = previous.data.without(kind, entryId) ?: return
        _uiState.value = previous.copy(data = prunedData)
        viewModelScope.launch {
            runCatching {
                repository.deleteCycleEntry(kind, entryId)
            }.onSuccess {
                historyChanged()
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.toScreenError())
            }
        }
    }

    /** Removes a day's journal row. Its Health Connect records stay. */
    fun deleteJournalEntry(date: LocalDate) {
        val previous = _uiState.value
        _uiState.value = previous.copy(journalEntries = previous.journalEntries.filterNot { it.date == date })
        viewModelScope.launch {
            runCatching { journal.delete(date) }
                .onSuccess {
                    historyChanged()
                    load()
                }
                .onFailure { error -> _uiState.value = previous.copy(error = error.toScreenError()) }
        }
    }

    /** Keeps a cycle in the history but out of the estimate, or brings it back. */
    fun setCycleExclusion(start: LocalDate, end: LocalDate?, excluded: Boolean, reason: CycleExclusionReason?) {
        viewModelScope.launch {
            runCatching {
                if (excluded) journal.exclude(start, end, reason) else journal.include(start, end)
            }.onSuccess {
                historyChanged()
                load()
            }
                .onFailure { error -> _uiState.value = _uiState.value.copy(error = error.toScreenError()) }
        }
    }

    /** A past period start: one light-flow day, which starts a cycle. */
    fun addPastPeriod(date: LocalDate) {
        viewModelScope.launch {
            runCatching {
                repository.writeCycleEntry(
                    CycleEntryWriteRequest(
                        kind = CycleEntryKind.MENSTRUATION_FLOW,
                        time = date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(),
                        flow = CycleRecordValues.FLOW_LIGHT,
                    ),
                )
            }.onSuccess {
                historyChanged()
                load()
            }
                .onFailure { error -> _uiState.value = _uiState.value.copy(error = error.toScreenError()) }
        }
    }

    /** A changed history moves the estimate: the alarms and the home widget follow. */
    /** Today's place in the pill scheme, or null when the pill is not tracked. */
    private suspend fun pillTodayDisplay(): PillTodayDisplay? {
        val plan = cyclePreferences.pillPlan()
        if (!plan.enabled) return null
        val today = LocalDate.now()
        val taken = runCatching { pillIntakes.isTaken(today) }.getOrDefault(false)
        return PillTodayDisplay(plan = plan, today = today, day = plan.dayAt(today), taken = taken)
    }

    /** Marks or unmarks today's pill and re-plans its reminder. */
    fun setPillTaken(taken: Boolean) {
        val pill = _uiState.value.display.today.pill ?: return
        viewModelScope.launch {
            runCatching { pillIntakes.setTaken(pill.today, taken) }.onSuccess {
                reminders.applyStoredConfig()
                val display = _uiState.value.display
                _uiState.value = _uiState.value.copy(
                    display = display.copy(today = display.today.copy(pill = pill.copy(taken = taken))),
                )
            }
        }
    }

    private fun historyChanged() {
        reminders.applyStoredConfig()
        homeWidgetRefreshScheduler?.refreshNow()
    }

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}

private fun CycleData.without(kind: CycleEntryKind, entryId: String): CycleData? {
    val hasEntry = when (kind) {
        CycleEntryKind.MENSTRUATION_FLOW -> menstruationFlows.any { it.id == entryId && it.isOpenVitalsEntry }
        CycleEntryKind.SPOTTING -> intermenstrualBleeding.any { it.id == entryId && it.isOpenVitalsEntry }
        CycleEntryKind.SEXUAL_ACTIVITY -> sexualActivity.any { it.id == entryId && it.isOpenVitalsEntry }
        CycleEntryKind.OVULATION_TEST -> ovulationTests.any { it.id == entryId && it.isOpenVitalsEntry }
        CycleEntryKind.CERVICAL_MUCUS -> cervicalMucus.any { it.id == entryId && it.isOpenVitalsEntry }
        CycleEntryKind.BASAL_BODY_TEMPERATURE -> basalBodyTemperature.any { it.id == entryId && it.isOpenVitalsEntry }
    }
    if (!hasEntry) return null
    return when (kind) {
        CycleEntryKind.MENSTRUATION_FLOW -> copy(menstruationFlows = menstruationFlows.filterNot { it.id == entryId })
        CycleEntryKind.SPOTTING -> copy(intermenstrualBleeding = intermenstrualBleeding.filterNot { it.id == entryId })
        CycleEntryKind.SEXUAL_ACTIVITY -> copy(sexualActivity = sexualActivity.filterNot { it.id == entryId })
        CycleEntryKind.OVULATION_TEST -> copy(ovulationTests = ovulationTests.filterNot { it.id == entryId })
        CycleEntryKind.CERVICAL_MUCUS -> copy(cervicalMucus = cervicalMucus.filterNot { it.id == entryId })
        CycleEntryKind.BASAL_BODY_TEMPERATURE -> copy(basalBodyTemperature = basalBodyTemperature.filterNot { it.id == entryId })
    }
}
