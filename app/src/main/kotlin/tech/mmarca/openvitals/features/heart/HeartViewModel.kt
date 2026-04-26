package tech.mmarca.openvitals.features.heart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.VitalsRepository
import tech.mmarca.openvitals.ui.components.periodFor
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HeartUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val daySamples: List<HeartRateSample> = emptyList(),
    val dailySummaries: List<HeartRateSummary> = emptyList(),
    val dayRestingBpm: Long? = null,
    val dayHrvMs: Double? = null,
    val dailyRestingHR: List<DailyRestingHR> = emptyList(),
    val dailyHrv: List<DailyHrv> = emptyList(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
    val missingVitalsPermissions: Set<String> = emptySet(),
    val error: String? = null,
) {
    val hasVitalsData: Boolean
        get() = bloodPressure.isNotEmpty() ||
            spO2.isNotEmpty() ||
            respiratoryRate.isNotEmpty() ||
            bodyTemperature.isNotEmpty() ||
            vo2Max.isNotEmpty()

    val latestBloodPressure: BloodPressureEntry? get() = bloodPressure.maxByOrNull { it.time }
    val latestSpO2: SpO2Entry? get() = spO2.maxByOrNull { it.time }
    val latestRespiratoryRate: RespiratoryRateEntry? get() = respiratoryRate.maxByOrNull { it.time }
    val latestBodyTemperature: BodyTempEntry? get() = bodyTemperature.maxByOrNull { it.time }
    val latestVo2Max: Vo2MaxEntry? get() = vo2Max.maxByOrNull { it.time }
}

class HeartViewModel(
    private val repository: HeartRepository,
    private val vitalsRepository: VitalsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeartUiState())
    val uiState: StateFlow<HeartUiState> = _uiState.asStateFlow()
    val vitalsPermissions: Set<String> get() = vitalsRepository.phase3Permissions

    init {
        load()
    }

    fun selectRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(
            selectedRange = range,
            selectedDate = _uiState.value.selectedDate.coerceAtMost(LocalDate.now()),
        )
        load()
    }

    fun previousPeriod() {
        _uiState.value = _uiState.value.copy(
            selectedDate = when (_uiState.value.selectedRange) {
                TimeRange.DAY -> _uiState.value.selectedDate.minusDays(1)
                TimeRange.WEEK -> _uiState.value.selectedDate.minusWeeks(1)
                TimeRange.MONTH -> _uiState.value.selectedDate.minusMonths(1)
                TimeRange.YEAR -> _uiState.value.selectedDate.minusYears(1)
            },
        )
        load()
    }

    fun nextPeriod() {
        val nextDate = when (_uiState.value.selectedRange) {
            TimeRange.DAY -> _uiState.value.selectedDate.plusDays(1)
            TimeRange.WEEK -> _uiState.value.selectedDate.plusWeeks(1)
            TimeRange.MONTH -> _uiState.value.selectedDate.plusMonths(1)
            TimeRange.YEAR -> _uiState.value.selectedDate.plusYears(1)
        }
        if (!periodFor(_uiState.value.selectedRange, nextDate).end.isAfter(LocalDate.now())) {
            _uiState.value = _uiState.value.copy(selectedDate = nextDate)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date.coerceAtMost(LocalDate.now()))
        load()
    }

    fun onVitalsPermissionsResult(granted: Set<String>) {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                coroutineScope {
                    val missingVitalsPermissions = async { vitalsRepository.missingPermissions() }
                    val bloodPressure = async { vitalsRepository.loadBloodPressure(period.start, period.end) }
                    val spO2 = async { vitalsRepository.loadSpO2(period.start, period.end) }
                    val respiratoryRate = async { vitalsRepository.loadRespiratoryRate(period.start, period.end) }
                    val bodyTemperature = async { vitalsRepository.loadBodyTemperature(period.start, period.end) }
                    val vo2Max = async { vitalsRepository.loadVo2Max(period.start, period.end) }

                    if (range == TimeRange.DAY) {
                        val samples = async { repository.loadHeartRateSamples(date) }
                        val restingBpm = async { repository.loadRestingHeartRate(date) }
                        val hrvMs = async { repository.loadHrvRmssd(date) }
                        HeartLoadResult(
                            daySamples = samples.await(),
                            dailySummaries = emptyList(),
                            dayRestingBpm = restingBpm.await(),
                            dayHrvMs = hrvMs.await(),
                            dailyRestingHR = emptyList(),
                            dailyHrv = emptyList(),
                            missingVitalsPermissions = missingVitalsPermissions.await(),
                            bloodPressure = bloodPressure.await(),
                            spO2 = spO2.await(),
                            respiratoryRate = respiratoryRate.await(),
                            bodyTemperature = bodyTemperature.await(),
                            vo2Max = vo2Max.await(),
                        )
                    } else {
                        val summaries = async { repository.loadDailyHeartRateSummaries(period.start, period.end) }
                        val restingHR = async { repository.loadDailyRestingHR(period.start, period.end) }
                        val hrv = async { repository.loadDailyHRV(period.start, period.end) }
                        HeartLoadResult(
                            daySamples = emptyList(),
                            dailySummaries = summaries.await(),
                            dayRestingBpm = null,
                            dayHrvMs = null,
                            dailyRestingHR = restingHR.await(),
                            dailyHrv = hrv.await(),
                            missingVitalsPermissions = missingVitalsPermissions.await(),
                            bloodPressure = bloodPressure.await(),
                            spO2 = spO2.await(),
                            respiratoryRate = respiratoryRate.await(),
                            bodyTemperature = bodyTemperature.await(),
                            vo2Max = vo2Max.await(),
                        )
                    }
                }
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    daySamples = result.daySamples,
                    dailySummaries = result.dailySummaries,
                    dayRestingBpm = result.dayRestingBpm,
                    dayHrvMs = result.dayHrvMs,
                    dailyRestingHR = result.dailyRestingHR,
                    dailyHrv = result.dailyHrv,
                    missingVitalsPermissions = result.missingVitalsPermissions,
                    bloodPressure = result.bloodPressure,
                    spO2 = result.spO2,
                    respiratoryRate = result.respiratoryRate,
                    bodyTemperature = result.bodyTemperature,
                    vo2Max = result.vo2Max,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = it.message,
                )
            }
        }
    }
}

private data class HeartLoadResult(
    val daySamples: List<HeartRateSample>,
    val dailySummaries: List<HeartRateSummary>,
    val dayRestingBpm: Long?,
    val dayHrvMs: Double?,
    val dailyRestingHR: List<DailyRestingHR>,
    val dailyHrv: List<DailyHrv>,
    val missingVitalsPermissions: Set<String>,
    val bloodPressure: List<BloodPressureEntry>,
    val spO2: List<SpO2Entry>,
    val respiratoryRate: List<RespiratoryRateEntry>,
    val bodyTemperature: List<BodyTempEntry>,
    val vo2Max: List<Vo2MaxEntry>,
)
