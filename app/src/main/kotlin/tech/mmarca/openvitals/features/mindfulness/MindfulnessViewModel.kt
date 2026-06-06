package tech.mmarca.openvitals.features.mindfulness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SleepRepository
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MindfulnessUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyGoalMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
    val reminderConfig: MindfulnessReminderConfig = MindfulnessReminderConfig(),
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val sessions: List<MindfulnessSession> = emptyList(),
    val previousSessions: List<MindfulnessSession> = emptyList(),
    val baselineSessions: List<MindfulnessSession> = emptyList(),
    val crossSleepSessions: List<SleepData> = emptyList(),
    val error: String? = null,
) {
    val totalMinutes: Long get() = sessions.sumOf { it.durationMinutes }
}

@HiltViewModel
class MindfulnessViewModel(
    private val repository: MindfulnessRepository,
    private val sleepRepository: SleepRepository? = null,
    initialRange: TimeRange = TimeRange.WEEK,
    initialSleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    initialDailyGoalMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
    initialReminderConfig: MindfulnessReminderConfig = MindfulnessReminderConfig(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
    private val onDailyGoalChanged: (Double) -> Unit = {},
    private val onReminderConfigChanged: (MindfulnessReminderConfig) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: MindfulnessRepository,
        sleepRepository: SleepRepository,
        preferencesRepository: PreferencesRepository,
        reminderController: MindfulnessReminderController,
    ) : this(
        repository = repository,
        sleepRepository = sleepRepository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.MINDFULNESS),
        initialSleepRangeMode = preferencesRepository.sleepRangeMode,
        initialDailyGoalMinutes = preferencesRepository.dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES),
        initialReminderConfig = reminderController.config(),
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.MINDFULNESS, range)
        },
        onDailyGoalChanged = { goal ->
            preferencesRepository.setDailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES, goal)
            reminderController.applyConfig()
        },
        onReminderConfigChanged = { config ->
            reminderController.updateConfig(config)
        },
    )

    private val goalKey = MetricDailyGoalKey.MINDFULNESS_MINUTES
    private val periodDriver = PeriodSelectionDriver(initialRange, onRangeSelected = onRangeSelected)
    private val _uiState = MutableStateFlow(
        MindfulnessUiState(
            selectedRange = initialRange,
            sleepRangeMode = initialSleepRangeMode,
            dailyGoalMinutes = goalKey.normalize(initialDailyGoalMinutes),
            reminderConfig = initialReminderConfig.normalized(),
        )
    )
    val uiState: StateFlow<MindfulnessUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
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

    fun increaseDailyGoal() {
        setDailyGoalMinutes(_uiState.value.dailyGoalMinutes + goalKey.step)
    }

    fun decreaseDailyGoal() {
        setDailyGoalMinutes(_uiState.value.dailyGoalMinutes - goalKey.step)
    }

    fun setDailyGoalMinutes(minutes: Double) {
        val goal = goalKey.normalize(minutes)
        onDailyGoalChanged(goal)
        _uiState.value = _uiState.value.copy(dailyGoalMinutes = goal)
    }

    fun setMindfulnessRemindersEnabled(enabled: Boolean) {
        updateReminderConfig { config -> config.copy(enabled = enabled) }
    }

    fun setMindfulnessReminderTime(time: LocalTime) {
        updateReminderConfig { config -> config.copy(reminderTime = time) }
    }

    fun deleteMindfulnessSessionEntry(entryId: String) {
        if (entryId.isBlank()) return
        val entry = _uiState.value.sessions.firstOrNull { it.id == entryId } ?: return
        if (!entry.isOpenVitalsEntry) return
        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.copy(
                sessions = previous.sessions.filterNot { it.id == entryId },
                error = null,
            )
            runCatching {
                repository.deleteMindfulnessSessionEntry(entryId)
            }.onSuccess {
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.message)
            }
        }
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
            )
            val period = query.windows.current
            val date = query.selectedDate
            val sleepRangeMode = _uiState.value.sleepRangeMode
            val sleepQueryStart = when (sleepRangeMode) {
                SleepRangeMode.ROLLING_24H -> period.start
                SleepRangeMode.NOON,
                SleepRangeMode.EVENING_18H -> period.start.minusDays(1)
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val periodData = repository.loadMindfulnessPeriod(query)
                MindfulnessLoadResult(
                    sessions = periodData.sessions,
                    previousSessions = periodData.previousSessions,
                    baselineSessions = periodData.baselineSessions,
                    crossSleepSessions = sleepRepository?.loadSleepSessions(sleepQueryStart, period.end).orEmpty(),
                )
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        sessions = result.sessions,
                        previousSessions = result.previousSessions,
                        baselineSessions = result.baselineSessions,
                        crossSleepSessions = result.crossSleepSessions,
                    )
                }
                .onFailure { error ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = error.message,
                    )
                }
        }
    }

    private data class MindfulnessLoadResult(
        val sessions: List<MindfulnessSession>,
        val previousSessions: List<MindfulnessSession>,
        val baselineSessions: List<MindfulnessSession>,
        val crossSleepSessions: List<SleepData>,
    )

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }

    private fun updateReminderConfig(update: (MindfulnessReminderConfig) -> MindfulnessReminderConfig) {
        val normalized = update(_uiState.value.reminderConfig).normalized()
        _uiState.value = _uiState.value.copy(reminderConfig = normalized)
        onReminderConfigChanged(normalized)
    }
}
