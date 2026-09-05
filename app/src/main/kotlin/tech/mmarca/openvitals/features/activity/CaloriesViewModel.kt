package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.navigation.selectedDayOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.sync.CaloriesHistorySyncService

@Immutable
data class CaloriesUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val dailySteps: List<DailySteps> = emptyList(),
    val previousDailySteps: List<DailySteps> = emptyList(),
    val baselineDailySteps: List<DailySteps> = emptyList(),
    val nutrition: List<DailyNutrition> = emptyList(),
    val previousNutrition: List<DailyNutrition> = emptyList(),
    val baselineNutrition: List<DailyNutrition> = emptyList(),
    val bmrEntries: List<BmrEntry> = emptyList(),
    // Derived where bmrEntries is set, not in the class body, which re-runs on every copy().
    val latestBmrEntry: BmrEntry? = null,
    val latestBmrKcal: Double? = null,
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
    val error: ScreenError? = null,
) {
    val displayBmrKcal: Double? = latestBmrEntry?.kcalPerDay ?: latestBmrKcal
}

@HiltViewModel
class CaloriesViewModel(
    private val activityRepository: ActivityRepository,
    private val bodyRepository: BodyRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    initialDate: java.time.LocalDate? = null,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    private val weekPeriodModeChanges: Flow<WeekPeriodMode> = emptyFlow(),
    private val calorieDataModeChanges: Flow<Boolean> = emptyFlow(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val caloriesSync: CaloriesHistorySyncService? = null,
) : ViewModel() {

    @Inject
    constructor(
        activityRepository: ActivityRepository,
        bodyRepository: BodyRepository,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: androidx.lifecycle.SavedStateHandle,
        caloriesSync: CaloriesHistorySyncService,
    ) : this(
        activityRepository = activityRepository,
        bodyRepository = bodyRepository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.CALORIES),
        initialDate = savedStateHandle.selectedDayOrNull(),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        weekPeriodModeChanges = preferencesRepository.weekPeriodModeFlow,
        calorieDataModeChanges = preferencesRepository.showOpenVitalsCalculatedCaloriesFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.CALORIES, range)
        },
        caloriesSync = caloriesSync,
    )

    private var caloriesSyncKicked = false

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialDate = initialDate ?: java.time.LocalDate.now(),
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        CaloriesUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
        )
    )
    val uiState: StateFlow<CaloriesUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observeWeekPeriodMode()
        observeCalorieDataMode()
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

    private fun observeCalorieDataMode() {
        viewModelScope.launch {
            calorieDataModeChanges.drop(1).collect {
                load()
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
            if (refreshCurrent) load(RefreshMode.FORCE)
            return
        }
        applyPeriodSelection(selection)
        load()
    }

    fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                coroutineScope {
                    val activity = async {
                        if (refreshMode == RefreshMode.NORMAL) {
                            activityRepository.loadActivityPeriod(
                                query = query,
                                includeSteps = true,
                                includeNutrition = true,
                                // This screen draws the intraday cards on Day, so it keeps the hourly aggregate.
                                includeActivityProgress = true,
                                // No comparison windows: this screen shows the current window alone.
                                includeComparisonWindows = false,
                            )
                        } else {
                            activityRepository.loadActivityPeriod(
                                query = query,
                                includeSteps = true,
                                includeNutrition = true,
                                // This screen draws the intraday cards on Day, so it keeps the hourly aggregate.
                                includeActivityProgress = true,
                                // No comparison windows: this screen shows the current window alone.
                                includeComparisonWindows = false,
                                refreshMode = refreshMode,
                            )
                        }
                    }
                    val bmr = async {
                        bodyRepository.loadBmrEntries(query.windows.current.start, query.windows.current.end)
                    }
                    val latestBmr = async {
                        bodyRepository.loadLatestBMR()
                    }
                    Triple(activity.await(), bmr.await(), latestBmr.await())
                }
            }.onSuccess { (activity, bmr, latestBmr) ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    dailySteps = activity.dailySteps,
                    previousDailySteps = activity.previousDailySteps,
                    baselineDailySteps = activity.baselineDailySteps,
                    nutrition = activity.nutrition,
                    previousNutrition = activity.previousNutrition,
                    baselineNutrition = activity.baselineNutrition,
                    bmrEntries = bmr,
                    latestBmrEntry = bmr.maxByOrNull { it.time },
                    latestBmrKcal = latestBmr,
                    activityProgress = activity.activityProgress,
                )
                kickCaloriesHistorySyncOnce()
            }.onFailure {
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = it.toScreenError(),
                )
            }
        }
    }

    /**
     * Kicks the calories history sync once per open, after the first load
     * settles. The first sync builds the cache every later open serves from.
     */
    private fun kickCaloriesHistorySyncOnce() {
        val sync = caloriesSync ?: return
        if (caloriesSyncKicked) return
        caloriesSyncKicked = true
        viewModelScope.launch {
            runCatching { sync.syncAll() }
            load()
        }
    }

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}
