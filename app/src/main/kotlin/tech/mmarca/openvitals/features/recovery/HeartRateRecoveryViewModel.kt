package tech.mmarca.openvitals.features.recovery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
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
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.domain.insights.calculateHeartRateRecovery
import tech.mmarca.openvitals.domain.insights.heartRateRecoveryWindowFor
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** Shorter sessions cannot have a recovery worth measuring, and each costs a read. */
private val minimumHeartRateRecoverySessionDuration: Duration = Duration.ofMinutes(5)

/** How many sessions the reads fan out over at once, to bound memory. */
private const val heartRateRecoveryReadConcurrency = 8

/** The most sessions a period looks at. The newest are taken and the truncation is reported. */
internal const val maxHeartRateRecoverySessions = 400

/** How far back of the period end the observed maximum heart rate is looked for. */
private const val observedMaxHeartRateLookbackDays = 90L

/** One session's recovery, with enough of the session to label it on a chart. */
@Immutable
data class HeartRateRecoverySessionReading(
    val sessionId: String,
    val title: String?,
    val exerciseType: Int,
    val startTime: Instant,
    val reading: HeartRateRecoveryReading,
)

@Immutable
data class HeartRateRecoveryUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,

    /** Every guided test in the period, newest first, measurable or not. The unmeasured ones matter. */
    val readings: List<HeartRateRecoverySessionReading> = emptyList(),

    /** The period held more sessions than the cap and only the most recent were read. */
    val truncated: Boolean = false,
    val error: ScreenError? = null,
) {
    /** The ones that may be plotted: a comparable fall with a one-minute mark. Often none on watch data. */
    val comparable: List<HeartRateRecoverySessionReading>
        get() = readings.filter { it.reading.isComparable }
}

/**
 * The read path for the recovery history. Nothing is stored: every point
 * is recomputed from Health Connect samples by the same function the
 * single-workout card uses.
 */
@HiltViewModel
class HeartRateRecoveryViewModel(
    private val activityRepository: ActivityRepository,
    private val heartRepository: HeartRepository,
    private val bodyProfileProvider: () -> BodyProfile = { BodyProfile() },
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    initialRange: TimeRange = TimeRange.MONTH,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    private val weekPeriodModeChanges: Flow<WeekPeriodMode> = emptyFlow(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        activityRepository: ActivityRepository,
        heartRepository: HeartRepository,
        preferencesRepository: PreferencesRepository,
        dispatchers: DispatcherProvider,
    ) : this(
        activityRepository = activityRepository,
        heartRepository = heartRepository,
        bodyProfileProvider = preferencesRepository::bodyProfile,
        dispatchers = dispatchers,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.HEART_RATE_RECOVERY),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        weekPeriodModeChanges = preferencesRepository.weekPeriodModeFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.HEART_RATE_RECOVERY, range)
        },
    )

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        HeartRateRecoveryUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
        )
    )
    val uiState: StateFlow<HeartRateRecoveryUiState> = _uiState.asStateFlow()
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

    fun selectDay(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDay(date))
        load()
    }

    fun resumeCurrentPeriod() {
        val selection = periodDriver.resumeCurrentPeriod() ?: return
        applyPeriodSelection(selection)
        load()
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = periodDriver.weekPeriodMode,
            )
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                loadPeriod(query)
            }.onSuccess { data ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    readings = data.readings,
                    truncated = data.truncated,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.toScreenError("Unable to load heart rate recovery."),
                )
            }
        }
    }

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }

    private class PeriodData(
        val readings: List<HeartRateRecoverySessionReading>,
        val truncated: Boolean,
    )

    private suspend fun loadPeriod(query: PeriodLoadQuery): PeriodData {
        val window = query.windows.current
        val workouts = activityRepository.loadWorkouts(window.start, window.end)

        // Only guided tests are measurable; other workouts cost no read.
        val candidates = workouts
            .filter { workout ->
                Duration.between(workout.startTime, workout.endTime) >=
                    minimumHeartRateRecoverySessionDuration &&
                    heartRateRecoveryWindowFor(workout) != null
            }
            .sortedByDescending { it.startTime }

        val truncated = candidates.size > maxHeartRateRecoverySessions
        val considered = if (truncated) candidates.take(maxHeartRateRecoverySessions) else candidates
        if (considered.isEmpty()) {
            return PeriodData(readings = emptyList(), truncated = truncated)
        }

        // Asked once for the whole period: the observed maximum covers the trailing 90 days.
        val profile = bodyProfileProvider()
        val observedMaxHeartRateBpm = observedMaxHeartRate(window.end)
        val restingHeartRateBpm = profile.restingHeartRateBpm
            ?: heartRepository.loadRestingHeartRate(window.end)?.toInt()

        val readings = withContext(dispatchers.default) {
            considered
                .chunked(heartRateRecoveryReadConcurrency)
                .flatMap { chunk ->
                    coroutineScope {
                        chunk.map { workout ->
                            async {
                                readingFor(
                                    workout = workout,
                                    profile = profile,
                                    observedMaxHeartRateBpm = observedMaxHeartRateBpm,
                                    restingHeartRateBpm = restingHeartRateBpm,
                                )
                            }
                        }.awaitAll()
                    }
                }
        }
        return PeriodData(readings = readings, truncated = truncated)
    }

    private suspend fun readingFor(
        workout: ExerciseData,
        profile: BodyProfile,
        observedMaxHeartRateBpm: Int?,
        restingHeartRateBpm: Int?,
    ): HeartRateRecoverySessionReading {
        val window = heartRateRecoveryWindowFor(workout)

        // A failed or empty read is the ordinary answer for a watch that stopped recording.
        val samples = if (window == null) {
            emptyList()
        } else {
            runCatching {
                heartRepository.loadHeartRateSamples(window.readStart, window.readEnd)
            }.getOrDefault(emptyList())
        }

        val reading = if (window == null || samples.isEmpty()) {
            HeartRateRecoveryReading.NoData
        } else {
            calculateHeartRateRecovery(
                recoveryStart = window.recoveryStart,
                samples = samples,
                restingHeartRateBpm = restingHeartRateBpm,
                ageYears = profile.ageYears(),
                observedMaxHeartRateBpm = observedMaxHeartRateBpm,
                explicitMaxHeartRateBpm = profile.maxHeartRateBpm,
            )
        }

        return HeartRateRecoverySessionReading(
            sessionId = workout.id,
            title = workout.title,
            exerciseType = workout.exerciseType,
            startTime = workout.startTime,
            reading = reading,
        )
    }

    private suspend fun observedMaxHeartRate(periodEnd: LocalDate): Int? =
        heartRepository
            .loadDailyHeartRateSummaries(
                periodEnd.minusDays(observedMaxHeartRateLookbackDays),
                periodEnd,
            )
            .maxOfOrNull { it.maxBpm }
            ?.toInt()
}
