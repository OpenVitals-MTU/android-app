package tech.mmarca.openvitals.features.hydration

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.navigation.selectedDayOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationEntryRecordType
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.model.valueFor
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationGoalPreferences
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import tech.mmarca.openvitals.domain.insights.CaffeineHealthDrinkCatalog
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DefaultHydrationDailyGoalLiters = 2.0
private const val HydrationGoalStepLiters = 0.25
private const val MinHydrationDailyGoalLiters = 0.25
private const val MaxHydrationDailyGoalLiters = 10.0
private const val OpenVitalsStandaloneNutritionPrefix = "openvitals_nutrition_"
private const val OpenVitalsPairedHydrationNutritionPrefix = "openvitals_hydration_nutrition_"
private const val OpenVitalsCarbsEntryName = "OpenVitals carbs"

@Immutable
data class HydrationUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val dailyGoalLiters: Double = DefaultHydrationDailyGoalLiters,
    val reminderConfig: HydrationReminderConfig = HydrationReminderConfig(),
    val dailyHydration: List<DailyHydration> = emptyList(),
    val previousDailyHydration: List<DailyHydration> = emptyList(),
    val baselineDailyHydration: List<DailyHydration> = emptyList(),
    val hydrationEntries: List<HydrationEntry> = emptyList(),
    val crossWeightEntries: List<WeightEntry> = emptyList(),
    val display: HydrationDisplayState = HydrationDisplayState(),
    val error: ScreenError? = null,
)

@HiltViewModel
class HydrationViewModel @Inject constructor(
    private val repository: HydrationRepository,
    private val nutritionRepository: NutritionRepository,
    private val bodyRepository: BodyRepository,
    private val periodPreferences: PeriodPreferences,
    private val hydrationGoalPreferences: HydrationGoalPreferences,
    private val reminders: HydrationReminderSettings,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {

    private val initialRange = periodPreferences.timeRangeFor(PeriodRangePreferenceKey.HYDRATION)
    private val initialWeekPeriodMode = periodPreferences.weekPeriodMode

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = savedStateHandle.selectedDayOrNull() ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = { range ->
            periodPreferences.setTimeRangeFor(PeriodRangePreferenceKey.HYDRATION, range)
        },
    )
    private val _uiState = MutableStateFlow(
        HydrationUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            dailyGoalLiters = normalizeHydrationGoalLiters(hydrationGoalPreferences.hydrationDailyGoalLiters),
            reminderConfig = reminders.config().normalized(),
        )
    )
    val uiState: StateFlow<HydrationUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

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

    fun increaseDailyGoal() {
        setDailyGoalLiters(_uiState.value.dailyGoalLiters + HydrationGoalStepLiters)
    }

    fun decreaseDailyGoal() {
        setDailyGoalLiters(_uiState.value.dailyGoalLiters - HydrationGoalStepLiters)
    }

    fun setDailyGoalLiters(liters: Double) {
        val goal = normalizeHydrationGoalLiters(liters)
        hydrationGoalPreferences.hydrationDailyGoalLiters = goal
        // The reminder text quotes the goal, so the alarm is re-planned.
        reminders.applyStoredConfig()
        _uiState.value = _uiState.value.copy(dailyGoalLiters = goal).withDisplay()
    }

    fun setHydrationRemindersEnabled(enabled: Boolean) {
        updateReminderConfig { config -> config.copy(enabled = enabled) }
    }

    fun increaseHydrationReminderInterval() {
        updateReminderConfig { config ->
            config.copy(intervalMinutes = config.intervalMinutes + HydrationReminderConfig.IntervalStepMinutes)
        }
    }

    fun decreaseHydrationReminderInterval() {
        updateReminderConfig { config ->
            config.copy(intervalMinutes = config.intervalMinutes - HydrationReminderConfig.IntervalStepMinutes)
        }
    }

    fun setHydrationReminderActiveStartTime(time: LocalTime) {
        updateReminderConfig { config -> config.copy(activeStartTime = time.withSecond(0).withNano(0)) }
    }

    fun setHydrationReminderActiveEndTime(time: LocalTime) {
        updateReminderConfig { config -> config.copy(activeEndTime = time.withSecond(0).withNano(0)) }
    }

    fun deleteHydrationEntry(entryId: String) {
        if (entryId.isBlank()) return
        val entry = _uiState.value.hydrationEntries.firstOrNull { it.id == entryId } ?: return
        if (!entry.isOpenVitalsEntry) return
        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.withDeletedHydrationEntry(entryId)
            runCatching {
                when (entry.recordType) {
                    HydrationEntryRecordType.HYDRATION -> repository.deleteHydrationEntry(entryId)
                    HydrationEntryRecordType.NUTRITION_ONLY -> {
                        nutritionRepository.deleteNutritionEntry(entryId)
                    }
                }
            }.onSuccess {
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.toScreenError())
            }
        }
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val windows = query.windows
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val periodData = repository.loadHydrationPeriod(query)
                val hydrationEntries = periodData.hydrationEntries
                val nutritionOnlyEntries = nutritionRepository
                    .loadNutritionEntries(windows.current.start, windows.current.end)
                    .toHydrationNutritionOnlyEntries(hydrationEntries)
                HydrationLoadResult(
                    dailyHydration = periodData.dailyHydration,
                    previousDailyHydration = periodData.previousDailyHydration,
                    baselineDailyHydration = periodData.baselineDailyHydration,
                    hydrationEntries = hydrationEntries + nutritionOnlyEntries,
                    crossWeightEntries = bodyRepository
                        .loadWeightEntries(windows.current.start, windows.current.end),
                )
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val display = withContext(dispatchers.default) {
                    HydrationPresentationMapper.build(
                        query = query,
                        dailyGoalLiters = _uiState.value.dailyGoalLiters,
                        dailyHydration = result.dailyHydration,
                        previousDailyHydration = result.previousDailyHydration,
                        baselineDailyHydration = result.baselineDailyHydration,
                        crossWeightEntries = result.crossWeightEntries,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailyHydration = result.dailyHydration,
                    previousDailyHydration = result.previousDailyHydration,
                    baselineDailyHydration = result.baselineDailyHydration,
                    hydrationEntries = result.hydrationEntries,
                    crossWeightEntries = result.crossWeightEntries,
                    display = display,
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

    private data class HydrationLoadResult(
        val dailyHydration: List<DailyHydration>,
        val previousDailyHydration: List<DailyHydration>,
        val baselineDailyHydration: List<DailyHydration>,
        val hydrationEntries: List<HydrationEntry>,
        val crossWeightEntries: List<WeightEntry>,
    )

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }

    private fun updateReminderConfig(update: (HydrationReminderConfig) -> HydrationReminderConfig) {
        val config = update(_uiState.value.reminderConfig).normalized()
        reminders.updateConfig(config)
        _uiState.value = _uiState.value.copy(reminderConfig = config)
    }

    private fun normalizeHydrationGoalLiters(liters: Double): Double =
        liters.coerceIn(MinHydrationDailyGoalLiters, MaxHydrationDailyGoalLiters)
}

private fun HydrationUiState.withDisplay(): HydrationUiState {
    val query = PeriodLoadQuery(
        range = selectedRange,
        anchorDate = selectedDate,
        weekPeriodMode = weekPeriodMode,
    )
    return copy(
        display = HydrationPresentationMapper.build(
            query = query,
            dailyGoalLiters = dailyGoalLiters,
            dailyHydration = dailyHydration,
            previousDailyHydration = previousDailyHydration,
            baselineDailyHydration = baselineDailyHydration,
            crossWeightEntries = crossWeightEntries,
        ),
    )
}

private fun HydrationUiState.withDeletedHydrationEntry(entryId: String): HydrationUiState {
    val entry = hydrationEntries.firstOrNull { it.id == entryId } ?: return this
    val entryDate = entry.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
    val updatedDailyHydration = dailyHydration.map { day ->
        if (day.date == entryDate) {
            day.copy(liters = (day.liters - entry.liters).coerceAtLeast(0.0))
        } else {
            day
        }
    }
    return copy(
        dailyHydration = updatedDailyHydration,
        hydrationEntries = hydrationEntries.filterNot { it.id == entryId },
        error = null,
    ).withDisplay()
}

private fun List<NutritionEntry>.toHydrationNutritionOnlyEntries(
    hydrationEntries: List<HydrationEntry>,
): List<HydrationEntry> =
    filter { entry ->
        entry.shouldAppearInBeverageHistory() &&
            entry.id.isNotBlank() &&
            entry.name != OpenVitalsCarbsEntryName &&
            entry.isStandaloneHydrationNutrition(hydrationEntries)
    }.map { entry ->
        HydrationEntry(
            startTime = entry.time,
            endTime = entry.time.plusSeconds(1),
            liters = 0.0,
            source = entry.source,
            id = entry.id,
            clientRecordId = entry.clientRecordId,
            isOpenVitalsEntry = entry.isOpenVitalsEntry,
            recordType = HydrationEntryRecordType.NUTRITION_ONLY,
            displayName = entry.name?.takeIf { it.isNotBlank() },
            nutrientValues = entry.nutrientValues,
        )
    }

private fun NutritionEntry.shouldAppearInBeverageHistory(): Boolean =
    isOpenVitalsEntry ||
        valueFor(NutritionNutrient.CAFFEINE)?.let { it > 0.0 && it.isFinite() } == true ||
        CaffeineHealthDrinkCatalog.matchName(name) != null

private fun NutritionEntry.isStandaloneHydrationNutrition(
    hydrationEntries: List<HydrationEntry>,
): Boolean =
    when {
        clientRecordId?.startsWith(OpenVitalsStandaloneNutritionPrefix) == true -> true
        clientRecordId?.startsWith(OpenVitalsPairedHydrationNutritionPrefix) == true -> false
        else -> hydrationEntries.none { hydrationEntry ->
            hydrationEntry.startTime == time && hydrationEntry.isOpenVitalsEntry
        }
    }
