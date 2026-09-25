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
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import java.time.LocalDate
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
    val missingPermissions: Set<String> = emptySet(),
    val error: ScreenError? = null,
)

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val repository: CycleRepository,
    private val periodPreferences: PeriodPreferences,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {

    private val initialRange = periodPreferences.timeRangeFor(PeriodRangePreferenceKey.CYCLE)
    private val initialDate = savedStateHandle.selectedDayOrNull()
    private val initialWeekPeriodMode = periodPreferences.weekPeriodMode

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = initialDate ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = { range ->
            periodPreferences.setTimeRangeFor(PeriodRangePreferenceKey.CYCLE, range)
        },
    )
    private val _uiState = MutableStateFlow(
        CycleUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
        )
    )
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

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
                if (_uiState.value.selectedRange == TimeRange.WEEK) {
                    load()
                }
            }
        }
    }

    fun selectRange(range: TimeRange) {
        applyPeriodSelection(periodDriver.selectRange(range))
        load()
    }

    fun previousPeriod() {
        applyPeriodSelection(periodDriver.previousPeriod())
        load()
    }

    fun nextPeriod() {
        periodDriver.nextPeriod()?.let { next ->
            applyPeriodSelection(next)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDate(date))
        load()
    }

    fun selectDay(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDay(date))
        load()
    }

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load()
            return
        }
        applyPeriodSelection(selection)
        load()
    }

    fun onCyclePermissionsResult(granted: Set<String>) {
        load()
    }

    fun load() {
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
                val display = withContext(dispatchers.default) {
                    CyclePresentationMapper.build(
                        query = query,
                        data = result.data,
                        statistics = result.statistics,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    data = result.data,
                    display = display,
                    statistics = result.statistics,
                    missingPermissions = result.missingPermissions,
                )
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

    fun deleteCycleEntry(kind: CycleEntryKind, entryId: String) {
        if (entryId.isBlank()) return
        val previous = _uiState.value
        val prunedData = previous.data.without(kind, entryId) ?: return
        _uiState.value = previous.copy(data = prunedData)
        viewModelScope.launch {
            runCatching {
                repository.deleteCycleEntry(kind, entryId)
            }.onSuccess {
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.toScreenError())
            }
        }
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
