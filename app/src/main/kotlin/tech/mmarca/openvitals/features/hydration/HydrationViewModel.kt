package tech.mmarca.openvitals.features.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.HydrationEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DefaultHydrationDailyGoalLiters = 2.0
private const val HydrationGoalStepLiters = 0.25
private const val MinHydrationDailyGoalLiters = 0.25
private const val MaxHydrationDailyGoalLiters = 10.0

data class HydrationUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoalLiters: Double = DefaultHydrationDailyGoalLiters,
    val dailyHydration: List<DailyHydration> = emptyList(),
    val previousDailyHydration: List<DailyHydration> = emptyList(),
    val baselineDailyHydration: List<DailyHydration> = emptyList(),
    val hydrationEntries: List<HydrationEntry> = emptyList(),
    val crossWeightEntries: List<WeightEntry> = emptyList(),
    val error: String? = null,
) {
    val totalLiters: Double get() = dailyHydration.sumOf { it.liters }
    val trackedDays: Int get() = dailyHydration.count { it.liters > 0.0 }
    val averageLiters: Double get() = trackedDays.takeIf { it > 0 }?.let { totalLiters / it } ?: 0.0
    val bestDayLiters: Double get() = dailyHydration.maxOfOrNull { it.liters } ?: 0.0
    val goalMetDays: Int get() = dailyHydration.count { it.meetsDailyGoal() }
    val goalSuccessRatePercent: Int get() = trackedDays.takeIf { it > 0 }?.let { goalMetDays * 100 / it } ?: 0
    val currentTrackedStreakDays: Int
        get() = dailyHydration
            .sortedBy { it.date }
            .asReversed()
            .takeWhile { it.liters > 0.0 }
            .count()
    val currentGoalStreakDays: Int
        get() = dailyHydration
            .sortedBy { it.date }
            .asReversed()
            .takeWhile { it.meetsDailyGoal() }
            .count()
    val longestGoalStreakDays: Int
        get() {
            var current = 0
            var longest = 0
            dailyHydration.sortedBy { it.date }.forEach { day ->
                if (day.meetsDailyGoal()) {
                    current += 1
                    longest = maxOf(longest, current)
                } else {
                    current = 0
                }
            }
            return longest
        }

    private fun DailyHydration.meetsDailyGoal(): Boolean =
        dailyGoalLiters > 0.0 && liters >= dailyGoalLiters
}

class HydrationViewModel(
    private val repository: HydrationRepository,
    private val bodyRepository: BodyRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialDailyGoalLiters: Double = DefaultHydrationDailyGoalLiters,
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HydrationUiState(
            selectedRange = initialRange,
            dailyGoalLiters = normalizeHydrationGoalLiters(initialDailyGoalLiters),
        )
    )
    val uiState: StateFlow<HydrationUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectRange(range: TimeRange) {
        onRangeSelected(range)
        applyPeriodSelection(periodSelection.selectRange(range))
        load()
    }

    fun previousPeriod() {
        applyPeriodSelection(periodSelection.previousPeriod())
        load()
    }

    fun nextPeriod() {
        val current = periodSelection
        val next = current.nextPeriod()
        if (next != current) {
            applyPeriodSelection(next)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        applyPeriodSelection(periodSelection.selectDate(date))
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
        _uiState.value = _uiState.value.copy(dailyGoalLiters = goal)
    }

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            val previousPeriod = previousPeriodFor(range, date)
            val baselinePeriod = baselinePeriodBefore(period)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                HydrationLoadResult(
                    dailyHydration = repository.loadDailyHydration(period.start, period.end),
                    previousDailyHydration = repository.loadDailyHydration(previousPeriod.start, previousPeriod.end),
                    baselineDailyHydration = repository.loadDailyHydration(baselinePeriod.start, baselinePeriod.end),
                    hydrationEntries = repository.loadHydrationEntries(period.start, period.end),
                    crossWeightEntries = bodyRepository?.loadWeightEntries(period.start, period.end).orEmpty(),
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailyHydration = result.dailyHydration,
                    previousDailyHydration = result.previousDailyHydration,
                    baselineDailyHydration = result.baselineDailyHydration,
                    hydrationEntries = result.hydrationEntries,
                    crossWeightEntries = result.crossWeightEntries,
                )
            }.onFailure { error ->
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

    private val periodSelection: PeriodSelection
        get() = PeriodSelection(_uiState.value.selectedRange, _uiState.value.selectedDate)

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }

    private fun normalizeHydrationGoalLiters(liters: Double): Double =
        liters.coerceIn(MinHydrationDailyGoalLiters, MaxHydrationDailyGoalLiters)
}
