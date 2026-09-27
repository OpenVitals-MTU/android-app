package tech.mmarca.openvitals.features.caffeine

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.CaffeineModelPreferences
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import tech.mmarca.openvitals.domain.insights.CaffeineInsightCalculator
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.navigation.CAFFEINE_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.selectedDayOrNull

@Immutable
data class CaffeineUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.DAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    /** Every drink loaded, the modeling lookback included. */
    val entries: List<CaffeineEntry> = emptyList(),
    /** The selected period's insights. */
    val display: CaffeineInsights = CaffeineInsights(),
    val periodComparison: PeriodComparison = PeriodComparison(0.0, 0.0),
    val preferences: CaffeinePreferences = CaffeinePreferences(),
    val bodyProfile: BodyProfile = BodyProfile(),
    val showSetup: Boolean = false,
    val selectedEntryId: String? = null,
    val error: ScreenError? = null,
)

@HiltViewModel
class CaffeineViewModel @Inject constructor(
    private val repository: CaffeineRepository,
    private val caffeineModel: CaffeineModelPreferences,
    private val bodyProfilePreferences: BodyProfilePreferences,
    // A caffeine entry is a nutrition record, so deletion goes through the nutrition repository.
    private val nutritionRepository: NutritionRepository,
    private val periodPreferences: PeriodPreferences,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    // The drink screen shares this model. It needs only its drink's day, and must not move the saved range.
    private val opensOneDrink = savedStateHandle.contains(CAFFEINE_ENTRY_ID_ARG)
    private val initialRange = if (opensOneDrink) {
        TimeRange.DAY
    } else {
        periodPreferences.timeRangeFor(PeriodRangePreferenceKey.CAFFEINE)
    }
    private val initialWeekPeriodMode = periodPreferences.weekPeriodMode

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = savedStateHandle.selectedDayOrNull() ?: LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = { range ->
            periodPreferences.setTimeRangeFor(PeriodRangePreferenceKey.CAFFEINE, range)
        },
    )
    private val _uiState = MutableStateFlow(
        CaffeineUiState(
            selectedRange = initialRange,
            selectedDate = periodDriver.selection.selectedDate,
            weekPeriodMode = initialWeekPeriodMode,
            preferences = caffeineModel.caffeinePreferences(),
            bodyProfile = bodyProfilePreferences.bodyProfile(),
        )
    )
    val uiState: StateFlow<CaffeineUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observePreferences()
        observeBodyProfile()
        observeWeekPeriodMode()
        load()
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

    /** Back on screen: follow the day forward, or reload the same one, since "now" has moved. */
    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load()
            return
        }
        applyPeriodSelection(selection)
        load()
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = currentQuery()
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.loadCaffeinePeriod(query)
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val preferences = _uiState.value.preferences
                val bodyProfile = _uiState.value.bodyProfile
                val display = withContext(dispatchers.default) {
                    CaffeineInsightCalculator.build(
                        entries = result.entries,
                        period = query.windows.current,
                        preferences = preferences,
                        bodyProfile = bodyProfile,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    entries = result.entries,
                    display = display,
                    periodComparison = periodComparison(
                        currentValue = display.periodTotalMg,
                        previousValue = result.previousTotalMg,
                    ),
                    showSetup = shouldShowSetup(preferences, result.entries),
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

    fun completeSetup(preferences: CaffeinePreferences) {
        caffeineModel.setCaffeinePreferences(preferences.copy(profileCompleted = true))
    }

    fun skipSetup() {
        caffeineModel.setCaffeinePreferences(
            _uiState.value.preferences.copy(profileCompleted = true)
        )
    }

    /** Removes a drink optimistically, deletes its nutrition record, then force-reloads. */
    fun deleteCaffeineEntry(entryId: String) {
        if (entryId.isBlank()) return
        val entry = _uiState.value.entries.firstOrNull { it.id == entryId } ?: return
        if (!entry.isOpenVitalsEntry) return
        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.copy(
                entries = previous.entries.filterNot { it.id == entryId },
                error = null,
            )
            runCatching {
                nutritionRepository.deleteNutritionEntry(entryId)
            }.onSuccess {
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.toScreenError())
            }
        }
    }

    fun selectEntry(entryId: String?) {
        _uiState.value = _uiState.value.copy(selectedEntryId = entryId)
    }

    private fun observePreferences() {
        viewModelScope.launch {
            caffeineModel.caffeinePreferencesFlow.drop(1).collect { preferences ->
                _uiState.value = _uiState.value.copy(preferences = preferences)
                rebuildDisplay()
            }
        }
    }

    private fun observeBodyProfile() {
        viewModelScope.launch {
            bodyProfilePreferences.bodyProfileFlow.drop(1).collect { bodyProfile ->
                _uiState.value = _uiState.value.copy(bodyProfile = bodyProfile)
                rebuildDisplay()
            }
        }
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

    /** The model changed, the drinks did not: recompute without a reload. */
    private suspend fun rebuildDisplay() {
        val state = _uiState.value
        val display = withContext(dispatchers.default) {
            CaffeineInsightCalculator.build(
                entries = state.entries,
                period = currentQuery().windows.current,
                preferences = state.preferences,
                bodyProfile = state.bodyProfile,
            )
        }
        _uiState.value = _uiState.value.copy(
            display = display,
            showSetup = shouldShowSetup(state.preferences, state.entries),
        )
    }

    private fun currentQuery(): PeriodLoadQuery =
        PeriodLoadQuery(
            range = periodDriver.selection.selectedRange,
            anchorDate = periodDriver.selection.selectedDate,
            weekPeriodMode = _uiState.value.weekPeriodMode,
        )

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }

    private fun shouldShowSetup(
        preferences: CaffeinePreferences,
        entries: List<CaffeineEntry>,
    ): Boolean {
        if (preferences.profileCompleted) return false
        return entries.isNotEmpty()
    }
}
