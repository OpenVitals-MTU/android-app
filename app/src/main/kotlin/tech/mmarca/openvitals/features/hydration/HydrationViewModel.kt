package tech.mmarca.openvitals.features.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

private const val DefaultHydrationDailyGoalLiters = 2.0
private const val HydrationGoalStepLiters = 0.25
private const val MinHydrationDailyGoalLiters = 0.25
private const val MaxHydrationDailyGoalLiters = 10.0

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
    val totalLiters: Double = 0.0,
    val trackedDays: Int = 0,
    val averageLiters: Double = 0.0,
    val bestDayLiters: Double = 0.0,
    val goalMetDays: Int = 0,
    val goalSuccessRatePercent: Int = 0,
    val currentTrackedStreakDays: Int = 0,
    val currentGoalStreakDays: Int = 0,
    val longestGoalStreakDays: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class HydrationViewModel(
    private val repository: HydrationRepository,
    private val bodyRepository: BodyRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    initialDailyGoalLiters: Double = DefaultHydrationDailyGoalLiters,
    initialReminderConfig: HydrationReminderConfig = HydrationReminderConfig(),
    private val weekPeriodModeChanges: Flow<WeekPeriodMode> = emptyFlow(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
    private val onReminderConfigChanged: (HydrationReminderConfig) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: HydrationRepository,
        bodyRepository: BodyRepository,
        preferencesRepository: PreferencesRepository,
        reminderController: HydrationReminderController,
    ) : this(
        repository = repository,
        bodyRepository = bodyRepository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.HYDRATION),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        initialDailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters,
        initialReminderConfig = reminderController.config(),
        weekPeriodModeChanges = preferencesRepository.weekPeriodModeFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.HYDRATION, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.hydrationDailyGoalLiters = goal
            reminderController.applyConfig()
        },
        onReminderConfigChanged = { config ->
            reminderController.updateConfig(config)
        },
    )

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        HydrationUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            dailyGoalLiters = normalizeHydrationGoalLiters(initialDailyGoalLiters),
            reminderConfig = initialReminderConfig.normalized(),
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
            weekPeriodModeChanges.drop(1).collect { mode ->
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

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load(RefreshMode.FORCE)
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
        onDailyGoalChanged(goal)
        _uiState.value = _uiState.value.withHydrationSummary(
            dailyGoalLiters = goal,
            dailyHydration = _uiState.value.dailyHydration,
        )
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
                repository.deleteHydrationEntry(entryId)
            }.onSuccess {
                load(RefreshMode.FORCE)
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.message)
            }
        }
    }

    fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
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
                val periodData = if (refreshMode == RefreshMode.NORMAL) {
                    repository.loadHydrationPeriod(query)
                } else {
                    repository.loadHydrationPeriod(query, refreshMode)
                }
                HydrationLoadResult(
                    dailyHydration = periodData.dailyHydration,
                    previousDailyHydration = periodData.previousDailyHydration,
                    baselineDailyHydration = periodData.baselineDailyHydration,
                    hydrationEntries = periodData.hydrationEntries,
                    crossWeightEntries = bodyRepository
                        ?.loadWeightEntries(windows.current.start, windows.current.end)
                        .orEmpty(),
                )
            }.onSuccess { result ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.withHydrationSummary(
                    isLoading = false,
                    selectedDate = date,
                    dailyHydration = result.dailyHydration,
                    previousDailyHydration = result.previousDailyHydration,
                    baselineDailyHydration = result.baselineDailyHydration,
                    hydrationEntries = result.hydrationEntries,
                    crossWeightEntries = result.crossWeightEntries,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = error.message,
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
        onReminderConfigChanged(config)
        _uiState.value = _uiState.value.copy(reminderConfig = config)
    }

    private fun normalizeHydrationGoalLiters(liters: Double): Double =
        liters.coerceIn(MinHydrationDailyGoalLiters, MaxHydrationDailyGoalLiters)
}

private data class HydrationSummary(
    val totalLiters: Double,
    val trackedDays: Int,
    val averageLiters: Double,
    val bestDayLiters: Double,
    val goalMetDays: Int,
    val goalSuccessRatePercent: Int,
    val currentTrackedStreakDays: Int,
    val currentGoalStreakDays: Int,
    val longestGoalStreakDays: Int,
)

private fun HydrationUiState.withHydrationSummary(
    isLoading: Boolean = this.isLoading,
    selectedDate: LocalDate = this.selectedDate,
    dailyGoalLiters: Double = this.dailyGoalLiters,
    dailyHydration: List<DailyHydration> = this.dailyHydration,
    previousDailyHydration: List<DailyHydration> = this.previousDailyHydration,
    baselineDailyHydration: List<DailyHydration> = this.baselineDailyHydration,
    hydrationEntries: List<HydrationEntry> = this.hydrationEntries,
    crossWeightEntries: List<WeightEntry> = this.crossWeightEntries,
): HydrationUiState {
    val summary = dailyHydration.summaryForGoal(dailyGoalLiters)
    return copy(
        isLoading = isLoading,
        selectedDate = selectedDate,
        dailyGoalLiters = dailyGoalLiters,
        dailyHydration = dailyHydration,
        previousDailyHydration = previousDailyHydration,
        baselineDailyHydration = baselineDailyHydration,
        hydrationEntries = hydrationEntries,
        crossWeightEntries = crossWeightEntries,
        totalLiters = summary.totalLiters,
        trackedDays = summary.trackedDays,
        averageLiters = summary.averageLiters,
        bestDayLiters = summary.bestDayLiters,
        goalMetDays = summary.goalMetDays,
        goalSuccessRatePercent = summary.goalSuccessRatePercent,
        currentTrackedStreakDays = summary.currentTrackedStreakDays,
        currentGoalStreakDays = summary.currentGoalStreakDays,
        longestGoalStreakDays = summary.longestGoalStreakDays,
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
    return withHydrationSummary(
        dailyHydration = updatedDailyHydration,
        hydrationEntries = hydrationEntries.filterNot { it.id == entryId },
    ).copy(error = null)
}

private fun List<DailyHydration>.summaryForGoal(dailyGoalLiters: Double): HydrationSummary {
    val sorted = sortedBy { it.date }
    val totalLiters = sumOf { it.liters }
    val trackedDays = count { it.liters > 0.0 }
    val goalMetDays = count { it.meetsDailyGoal(dailyGoalLiters) }
    var currentGoalStreak = 0
    var longestGoalStreak = 0
    sorted.forEach { day ->
        if (day.meetsDailyGoal(dailyGoalLiters)) {
            currentGoalStreak += 1
            longestGoalStreak = maxOf(longestGoalStreak, currentGoalStreak)
        } else {
            currentGoalStreak = 0
        }
    }
    val reversed = sorted.asReversed()
    return HydrationSummary(
        totalLiters = totalLiters,
        trackedDays = trackedDays,
        averageLiters = trackedDays.takeIf { it > 0 }?.let { totalLiters / it } ?: 0.0,
        bestDayLiters = maxOfOrNull { it.liters } ?: 0.0,
        goalMetDays = goalMetDays,
        goalSuccessRatePercent = trackedDays.takeIf { it > 0 }?.let { goalMetDays * 100 / it } ?: 0,
        currentTrackedStreakDays = reversed.takeWhile { it.liters > 0.0 }.count(),
        currentGoalStreakDays = reversed.takeWhile { it.meetsDailyGoal(dailyGoalLiters) }.count(),
        longestGoalStreakDays = longestGoalStreak,
    )
}

private fun DailyHydration.meetsDailyGoal(dailyGoalLiters: Double): Boolean =
    dailyGoalLiters > 0.0 && liters >= dailyGoalLiters
