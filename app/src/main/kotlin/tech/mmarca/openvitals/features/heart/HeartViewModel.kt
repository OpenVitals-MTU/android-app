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
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.VitalsRepository
import tech.mmarca.openvitals.core.period.periodFor
import java.time.LocalDate
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
    initialRange: TimeRange = TimeRange.WEEK,
    private val selectedMetric: HeartMetric = HeartMetric.AVERAGE_HEART_RATE,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeartUiState(selectedRange = initialRange))
    val uiState: StateFlow<HeartUiState> = _uiState.asStateFlow()
    val vitalsPermissions: Set<String> get() = vitalsRepository.phase3Permissions

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
                when (selectedMetric) {
                    HeartMetric.AVERAGE_HEART_RATE -> if (range == TimeRange.DAY) {
                        HeartLoadResult(daySamples = repository.loadHeartRateSamples(date))
                    } else {
                        HeartLoadResult(dailySummaries = repository.loadDailyHeartRateSummaries(period.start, period.end))
                    }
                    HeartMetric.RESTING_HEART_RATE -> if (range == TimeRange.DAY) {
                        HeartLoadResult(dayRestingBpm = repository.loadRestingHeartRate(date))
                    } else {
                        HeartLoadResult(dailyRestingHR = repository.loadDailyRestingHR(period.start, period.end))
                    }
                    HeartMetric.HRV -> if (range == TimeRange.DAY) {
                        HeartLoadResult(dayHrvMs = repository.loadHrvRmssd(date))
                    } else {
                        HeartLoadResult(dailyHrv = repository.loadDailyHRV(period.start, period.end))
                    }
                    HeartMetric.BLOOD_PRESSURE -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        bloodPressure = vitalsRepository.loadBloodPressure(period.start, period.end),
                    )
                    HeartMetric.SPO2 -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        spO2 = vitalsRepository.loadSpO2(period.start, period.end),
                    )
                    HeartMetric.VO2_MAX -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        vo2Max = vitalsRepository.loadVo2Max(period.start, period.end),
                    )
                    HeartMetric.RESPIRATORY_RATE -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        respiratoryRate = vitalsRepository.loadRespiratoryRate(period.start, period.end),
                    )
                    HeartMetric.BODY_TEMPERATURE -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        bodyTemperature = vitalsRepository.loadBodyTemperature(period.start, period.end),
                    )
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

    private val periodSelection: PeriodSelection
        get() = PeriodSelection(_uiState.value.selectedRange, _uiState.value.selectedDate)

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}

private data class HeartLoadResult(
    val daySamples: List<HeartRateSample> = emptyList(),
    val dailySummaries: List<HeartRateSummary> = emptyList(),
    val dayRestingBpm: Long? = null,
    val dayHrvMs: Double? = null,
    val dailyRestingHR: List<DailyRestingHR> = emptyList(),
    val dailyHrv: List<DailyHrv> = emptyList(),
    val missingVitalsPermissions: Set<String> = emptySet(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
)
