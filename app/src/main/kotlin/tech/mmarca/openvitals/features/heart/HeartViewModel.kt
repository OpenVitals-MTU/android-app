package tech.mmarca.openvitals.features.heart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.core.performance.LoadCoordinator
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
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HeartUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val daySamples: List<HeartRateSample> = emptyList(),
    val previousDaySamples: List<HeartRateSample> = emptyList(),
    val dailySummaries: List<HeartRateSummary> = emptyList(),
    val previousDailySummaries: List<HeartRateSummary> = emptyList(),
    val baselineDailySummaries: List<HeartRateSummary> = emptyList(),
    val dayRestingBpm: Long? = null,
    val previousDayRestingBpm: Long? = null,
    val dayHrvMs: Double? = null,
    val previousDayHrvMs: Double? = null,
    val dailyRestingHR: List<DailyRestingHR> = emptyList(),
    val previousDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val baselineDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val dailyHrv: List<DailyHrv> = emptyList(),
    val previousDailyHrv: List<DailyHrv> = emptyList(),
    val baselineDailyHrv: List<DailyHrv> = emptyList(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val previousBloodPressure: List<BloodPressureEntry> = emptyList(),
    val baselineBloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val previousSpO2: List<SpO2Entry> = emptyList(),
    val baselineSpO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val previousRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val baselineRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val previousBodyTemperature: List<BodyTempEntry> = emptyList(),
    val baselineBodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
    val previousVo2Max: List<Vo2MaxEntry> = emptyList(),
    val baselineVo2Max: List<Vo2MaxEntry> = emptyList(),
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
    private val loadCoordinator = LoadCoordinator()

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
        loadCoordinator.launch(viewModelScope) load@{
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            val previousPeriod = previousPeriodFor(range, date)
            val baselinePeriod = baselinePeriodBefore(period)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                when (selectedMetric) {
                    HeartMetric.AVERAGE_HEART_RATE -> if (range == TimeRange.DAY) {
                        HeartLoadResult(
                            daySamples = repository.loadHeartRateSamples(date),
                            previousDaySamples = repository.loadHeartRateSamples(previousPeriod.start),
                            baselineDailySummaries = repository.loadDailyHeartRateSummaries(baselinePeriod.start, baselinePeriod.end),
                        )
                    } else {
                        HeartLoadResult(
                            dailySummaries = repository.loadDailyHeartRateSummaries(period.start, period.end),
                            previousDailySummaries = repository.loadDailyHeartRateSummaries(previousPeriod.start, previousPeriod.end),
                            baselineDailySummaries = repository.loadDailyHeartRateSummaries(baselinePeriod.start, baselinePeriod.end),
                        )
                    }
                    HeartMetric.RESTING_HEART_RATE -> if (range == TimeRange.DAY) {
                        HeartLoadResult(
                            dayRestingBpm = repository.loadRestingHeartRate(date),
                            previousDayRestingBpm = repository.loadRestingHeartRate(previousPeriod.start),
                            baselineDailyRestingHR = repository.loadDailyRestingHR(baselinePeriod.start, baselinePeriod.end),
                        )
                    } else {
                        HeartLoadResult(
                            dailyRestingHR = repository.loadDailyRestingHR(period.start, period.end),
                            previousDailyRestingHR = repository.loadDailyRestingHR(previousPeriod.start, previousPeriod.end),
                            baselineDailyRestingHR = repository.loadDailyRestingHR(baselinePeriod.start, baselinePeriod.end),
                        )
                    }
                    HeartMetric.HRV -> if (range == TimeRange.DAY) {
                        HeartLoadResult(
                            dayHrvMs = repository.loadHrvRmssd(date),
                            previousDayHrvMs = repository.loadHrvRmssd(previousPeriod.start),
                            baselineDailyHrv = repository.loadDailyHRV(baselinePeriod.start, baselinePeriod.end),
                        )
                    } else {
                        HeartLoadResult(
                            dailyHrv = repository.loadDailyHRV(period.start, period.end),
                            previousDailyHrv = repository.loadDailyHRV(previousPeriod.start, previousPeriod.end),
                            baselineDailyHrv = repository.loadDailyHRV(baselinePeriod.start, baselinePeriod.end),
                        )
                    }
                    HeartMetric.BLOOD_PRESSURE -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        bloodPressure = vitalsRepository.loadBloodPressure(period.start, period.end),
                        previousBloodPressure = vitalsRepository.loadBloodPressure(previousPeriod.start, previousPeriod.end),
                        baselineBloodPressure = vitalsRepository.loadBloodPressure(baselinePeriod.start, baselinePeriod.end),
                    )
                    HeartMetric.SPO2 -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        spO2 = vitalsRepository.loadSpO2(period.start, period.end),
                        previousSpO2 = vitalsRepository.loadSpO2(previousPeriod.start, previousPeriod.end),
                        baselineSpO2 = vitalsRepository.loadSpO2(baselinePeriod.start, baselinePeriod.end),
                    )
                    HeartMetric.VO2_MAX -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        vo2Max = vitalsRepository.loadVo2Max(period.start, period.end),
                        previousVo2Max = vitalsRepository.loadVo2Max(previousPeriod.start, previousPeriod.end),
                        baselineVo2Max = vitalsRepository.loadVo2Max(baselinePeriod.start, baselinePeriod.end),
                    )
                    HeartMetric.RESPIRATORY_RATE -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        respiratoryRate = vitalsRepository.loadRespiratoryRate(period.start, period.end),
                        previousRespiratoryRate = vitalsRepository.loadRespiratoryRate(previousPeriod.start, previousPeriod.end),
                        baselineRespiratoryRate = vitalsRepository.loadRespiratoryRate(baselinePeriod.start, baselinePeriod.end),
                    )
                    HeartMetric.BODY_TEMPERATURE -> HeartLoadResult(
                        missingVitalsPermissions = vitalsRepository.missingPermissions(),
                        bodyTemperature = vitalsRepository.loadBodyTemperature(period.start, period.end),
                        previousBodyTemperature = vitalsRepository.loadBodyTemperature(previousPeriod.start, previousPeriod.end),
                        baselineBodyTemperature = vitalsRepository.loadBodyTemperature(baselinePeriod.start, baselinePeriod.end),
                    )
                }
            }.onSuccess { result ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    daySamples = result.daySamples,
                    previousDaySamples = result.previousDaySamples,
                    dailySummaries = result.dailySummaries,
                    previousDailySummaries = result.previousDailySummaries,
                    baselineDailySummaries = result.baselineDailySummaries,
                    dayRestingBpm = result.dayRestingBpm,
                    previousDayRestingBpm = result.previousDayRestingBpm,
                    dayHrvMs = result.dayHrvMs,
                    previousDayHrvMs = result.previousDayHrvMs,
                    dailyRestingHR = result.dailyRestingHR,
                    previousDailyRestingHR = result.previousDailyRestingHR,
                    baselineDailyRestingHR = result.baselineDailyRestingHR,
                    dailyHrv = result.dailyHrv,
                    previousDailyHrv = result.previousDailyHrv,
                    baselineDailyHrv = result.baselineDailyHrv,
                    missingVitalsPermissions = result.missingVitalsPermissions,
                    bloodPressure = result.bloodPressure,
                    previousBloodPressure = result.previousBloodPressure,
                    baselineBloodPressure = result.baselineBloodPressure,
                    spO2 = result.spO2,
                    previousSpO2 = result.previousSpO2,
                    baselineSpO2 = result.baselineSpO2,
                    respiratoryRate = result.respiratoryRate,
                    previousRespiratoryRate = result.previousRespiratoryRate,
                    baselineRespiratoryRate = result.baselineRespiratoryRate,
                    bodyTemperature = result.bodyTemperature,
                    previousBodyTemperature = result.previousBodyTemperature,
                    baselineBodyTemperature = result.baselineBodyTemperature,
                    vo2Max = result.vo2Max,
                    previousVo2Max = result.previousVo2Max,
                    baselineVo2Max = result.baselineVo2Max,
                )
            }.onFailure {
                if (!isCurrent) return@load
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
    val previousDaySamples: List<HeartRateSample> = emptyList(),
    val dailySummaries: List<HeartRateSummary> = emptyList(),
    val previousDailySummaries: List<HeartRateSummary> = emptyList(),
    val baselineDailySummaries: List<HeartRateSummary> = emptyList(),
    val dayRestingBpm: Long? = null,
    val previousDayRestingBpm: Long? = null,
    val dayHrvMs: Double? = null,
    val previousDayHrvMs: Double? = null,
    val dailyRestingHR: List<DailyRestingHR> = emptyList(),
    val previousDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val baselineDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val dailyHrv: List<DailyHrv> = emptyList(),
    val previousDailyHrv: List<DailyHrv> = emptyList(),
    val baselineDailyHrv: List<DailyHrv> = emptyList(),
    val missingVitalsPermissions: Set<String> = emptySet(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val previousBloodPressure: List<BloodPressureEntry> = emptyList(),
    val baselineBloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val previousSpO2: List<SpO2Entry> = emptyList(),
    val baselineSpO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val previousRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val baselineRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val previousBodyTemperature: List<BodyTempEntry> = emptyList(),
    val baselineBodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
    val previousVo2Max: List<Vo2MaxEntry> = emptyList(),
    val baselineVo2Max: List<Vo2MaxEntry> = emptyList(),
)
