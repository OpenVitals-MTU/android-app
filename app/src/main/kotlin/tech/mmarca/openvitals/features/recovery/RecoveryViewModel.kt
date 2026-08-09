package tech.mmarca.openvitals.features.recovery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.domain.insights.calculateSleepScoresByDate
import tech.mmarca.openvitals.domain.insights.overnightHrvInputsByDate
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.sleepDurationMsFromStages
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository

private const val RecoveryLookbackDays = 7L

@Immutable
data class RecoveryDay(
    val date: LocalDate,
    val sessions: List<SleepData> = emptyList(),
    val sleepScore: SleepScoreEstimate = SleepScoreEstimate.NoData,
) {
    val mainSleepSession: SleepData?
        get() = sessions.maxByOrNull { sleepDurationMsFromStages(it.stages, it.durationMs) }

    val sleepDurationMs: Long
        get() = sessions.sumOf { sleepDurationMsFromStages(it.stages, it.durationMs) }

    val remDurationMs: Long
        get() = sessions.stageDurationMs(SleepStage.STAGE_REM)

    val deepDurationMs: Long
        get() = sessions.stageDurationMs(SleepStage.STAGE_DEEP)
}

@Immutable
data class RecoveryUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val days: List<RecoveryDay> = emptyList(),
    val error: ScreenError? = null,
) {
    val today: RecoveryDay
        get() = days.firstOrNull { it.date == selectedDate } ?: RecoveryDay(selectedDate)

    val metricDays: List<RecoveryDay>
        get() = days.takeLast(RecoveryLookbackDays.toInt())
}

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val heartRepository: HeartRepository,
    private val preferencesRepository: PreferencesRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryUiState())
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    fun load(today: LocalDate = LocalDate.now()) {
        loadCoordinator.launch(viewModelScope) load@{
            val start = today.minusDays(RecoveryLookbackDays - 1)
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedDate = today,
                error = null,
            )
            runCatching {
                coroutineScope {
                    val sessionsDeferred = async { sleepRepository.loadSleepSessions(start, today) }
                    val hrvDeferred = async {
                        val zone = ZoneId.systemDefault()
                        val windowStart = start.atStartOfDay(zone).toInstant()
                        val windowEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
                        runCatching {
                            heartRepository.loadHrvSamples(windowStart, windowEnd)
                        }.getOrDefault(emptyList())
                    }
                    LoadedRecoveryInputs(
                        sessions = sessionsDeferred.await(),
                        hrvSamples = hrvDeferred.await(),
                        ageYears = preferencesRepository.bodyProfile().ageYears(today),
                    )
                }
            }.onSuccess { inputs ->
                if (!isCurrent) return@load
                val days = withContext(dispatchers.default) {
                    inputs.sessions.toRecoveryDays(
                        start = start,
                        end = today,
                        ageYears = inputs.ageYears,
                        hrvSamples = inputs.hrvSamples,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = today,
                    days = days,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = today,
                    error = error.toScreenError(),
                )
            }
        }
    }
}

private data class LoadedRecoveryInputs(
    val sessions: List<SleepData>,
    val hrvSamples: List<HrvSample>,
    val ageYears: Int?,
)

private fun List<SleepData>.toRecoveryDays(
    start: LocalDate,
    end: LocalDate,
    ageYears: Int?,
    hrvSamples: List<HrvSample>,
): List<RecoveryDay> {
    val zone = ZoneId.systemDefault()
    val sessionsByDate = groupBy { it.endTime.atZone(zone).toLocalDate() }
    val overnightHrvByDate = overnightHrvInputsByDate(
        sessions = this,
        hrvSamples = hrvSamples,
        start = start,
        end = end,
        zone = zone,
    )
    val sleepScores = calculateSleepScoresByDate(
        sessions = this,
        start = start,
        end = end,
        zone = zone,
        ageYears = ageYears,
        overnightHrvByDate = overnightHrvByDate,
    )
    return generateSequence(start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(end) }
    }.map { date ->
        RecoveryDay(
            date = date,
            sessions = sessionsByDate[date].orEmpty(),
            sleepScore = sleepScores[date] ?: SleepScoreEstimate.NoData,
        )
    }.toList()
}

private fun List<SleepData>.stageDurationMs(stageType: Int): Long =
    sumOf { session ->
        session.stages
            .filter { it.stageType == stageType }
            .sumOf { it.durationMs.coerceAtLeast(0L) }
    }
