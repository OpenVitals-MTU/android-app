package tech.mmarca.openvitals.features.heart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.data.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.repository.HeartPeriodData
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.VitalsPeriodData
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.data.repository.VitalsRepository
import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

private const val HeartRateThresholdStepBpm = 5
private const val HeartRateThresholdMinimumGapBpm = 5

enum class HeartRateThresholdCheckType {
    HIGH,
    LOW,
}

data class HeartRateThresholdCheck(
    val type: HeartRateThresholdCheckType,
    val thresholdBpm: Int,
    val count: Int = 0,
    val hasData: Boolean = false,
)

data class HeartUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
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
    val hasVitalsData: Boolean = false,
    val latestBloodPressure: BloodPressureEntry? = null,
    val latestSpO2: SpO2Entry? = null,
    val latestRespiratoryRate: RespiratoryRateEntry? = null,
    val latestBodyTemperature: BodyTempEntry? = null,
    val latestVo2Max: Vo2MaxEntry? = null,
    val missingVitalsPermissions: Set<String> = emptySet(),
    val highHeartRateCheck: HeartRateThresholdCheck = HeartRateThresholdCheck(
        type = HeartRateThresholdCheckType.HIGH,
        thresholdBpm = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
    ),
    val lowHeartRateCheck: HeartRateThresholdCheck = HeartRateThresholdCheck(
        type = HeartRateThresholdCheckType.LOW,
        thresholdBpm = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
    ),
    val error: String? = null,
)

@HiltViewModel
class HeartViewModel(
    private val repository: HeartRepository,
    private val vitalsRepository: VitalsRepository,
    initialRange: TimeRange = TimeRange.WEEK,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    private val selectedMetric: HeartMetric? = HeartMetric.AVERAGE_HEART_RATE,
    private val weekPeriodModeChanges: Flow<WeekPeriodMode> = emptyFlow(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
    initialHighHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
    initialLowHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
    private val onHighHeartRateThresholdChanged: (Int) -> Unit = {},
    private val onLowHeartRateThresholdChanged: (Int) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: HeartRepository,
        vitalsRepository: VitalsRepository,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        vitalsRepository = vitalsRepository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.HEART),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        selectedMetric = heartMetricFromRoute(savedStateHandle[METRIC_ID_ARG]),
        weekPeriodModeChanges = preferencesRepository.weekPeriodModeFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.HEART, range)
        },
        initialHighHeartRateThresholdBpm = preferencesRepository.highHeartRateThresholdBpm,
        initialLowHeartRateThresholdBpm = preferencesRepository.lowHeartRateThresholdBpm,
        onHighHeartRateThresholdChanged = { threshold ->
            preferencesRepository.highHeartRateThresholdBpm = threshold
        },
        onLowHeartRateThresholdChanged = { threshold ->
            preferencesRepository.lowHeartRateThresholdBpm = threshold
        },
    )

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        HeartUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
            highHeartRateCheck = HeartRateThresholdCheck(
                type = HeartRateThresholdCheckType.HIGH,
                thresholdBpm = initialHighHeartRateThresholdBpm,
            ),
            lowHeartRateCheck = HeartRateThresholdCheck(
                type = HeartRateThresholdCheckType.LOW,
                thresholdBpm = initialLowHeartRateThresholdBpm,
            ),
        )
    )
    val uiState: StateFlow<HeartUiState> = _uiState.asStateFlow()
    val vitalsPermissions: Set<String> get() = vitalsRepository.phase3Permissions
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

    fun deleteVitalsMeasurementEntry(type: VitalsMeasurementType, entryId: String) {
        if (entryId.isBlank()) return
        val entryIsOpenVitals = when (type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> _uiState.value.bloodPressure
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
            VitalsMeasurementType.SPO2 -> _uiState.value.spO2
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
            VitalsMeasurementType.RESPIRATORY_RATE -> _uiState.value.respiratoryRate
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
            VitalsMeasurementType.BODY_TEMPERATURE -> _uiState.value.bodyTemperature
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
        } ?: return
        if (!entryIsOpenVitals) return

        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.withDeletedVitalsMeasurementEntry(type, entryId)
            runCatching {
                vitalsRepository.deleteVitalsMeasurementEntry(type, entryId)
            }.onSuccess {
                load()
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.message)
            }
        }
    }

    fun onVitalsPermissionsResult(granted: Set<String>) {
        load()
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                when (val metric = selectedMetric) {
                    null -> coroutineScope {
                        val heart = async {
                            repository
                                .loadHeartPeriod(query, HeartPeriodMetric.ALL)
                                .toLoadResult()
                        }
                        val vitals = async {
                            vitalsRepository
                                .loadVitalsPeriod(query, VitalsPeriodMetric.ALL)
                                .toLoadResult()
                        }
                        heart.await().merge(vitals.await())
                    }
                    HeartMetric.AVERAGE_HEART_RATE,
                    HeartMetric.RESTING_HEART_RATE,
                    HeartMetric.HRV -> repository
                        .loadHeartPeriod(query, metric.toHeartPeriodMetric())
                        .toLoadResult()
                    HeartMetric.BLOOD_PRESSURE,
                    HeartMetric.SPO2,
                    HeartMetric.VO2_MAX,
                    HeartMetric.RESPIRATORY_RATE,
                    HeartMetric.BODY_TEMPERATURE -> vitalsRepository
                        .loadVitalsPeriod(query, metric.toVitalsPeriodMetric())
                        .toLoadResult()
                }
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val vitalsSummary = result.vitalsSummary()
                val highHeartRateCheck = result.heartRateThresholdCheck(
                    selectedRange = query.range,
                    type = HeartRateThresholdCheckType.HIGH,
                    thresholdBpm = _uiState.value.highHeartRateCheck.thresholdBpm,
                )
                val lowHeartRateCheck = result.heartRateThresholdCheck(
                    selectedRange = query.range,
                    type = HeartRateThresholdCheckType.LOW,
                    thresholdBpm = _uiState.value.lowHeartRateCheck.thresholdBpm,
                )
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
                    hasVitalsData = vitalsSummary.hasVitalsData,
                    latestBloodPressure = vitalsSummary.latestBloodPressure,
                    latestSpO2 = vitalsSummary.latestSpO2,
                    latestRespiratoryRate = vitalsSummary.latestRespiratoryRate,
                    latestBodyTemperature = vitalsSummary.latestBodyTemperature,
                    latestVo2Max = vitalsSummary.latestVo2Max,
                    highHeartRateCheck = highHeartRateCheck,
                    lowHeartRateCheck = lowHeartRateCheck,
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

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }

    fun increaseHighHeartRateThreshold() {
        setHighHeartRateThreshold(_uiState.value.highHeartRateCheck.thresholdBpm + HeartRateThresholdStepBpm)
    }

    fun decreaseHighHeartRateThreshold() {
        setHighHeartRateThreshold(_uiState.value.highHeartRateCheck.thresholdBpm - HeartRateThresholdStepBpm)
    }

    fun increaseLowHeartRateThreshold() {
        setLowHeartRateThreshold(_uiState.value.lowHeartRateCheck.thresholdBpm + HeartRateThresholdStepBpm)
    }

    fun decreaseLowHeartRateThreshold() {
        setLowHeartRateThreshold(_uiState.value.lowHeartRateCheck.thresholdBpm - HeartRateThresholdStepBpm)
    }

    private fun setHighHeartRateThreshold(thresholdBpm: Int) {
        val current = _uiState.value
        val normalized = thresholdBpm
            .coerceAtLeast(current.lowHeartRateCheck.thresholdBpm + HeartRateThresholdMinimumGapBpm)
            .coerceIn(
                PreferencesRepository.MIN_HIGH_HEART_RATE_THRESHOLD_BPM,
                PreferencesRepository.MAX_HIGH_HEART_RATE_THRESHOLD_BPM,
            )
        onHighHeartRateThresholdChanged(normalized)
        _uiState.value = current.copy(
            highHeartRateCheck = current.heartRateThresholdCheck(
                type = HeartRateThresholdCheckType.HIGH,
                thresholdBpm = normalized,
            )
        )
    }

    private fun setLowHeartRateThreshold(thresholdBpm: Int) {
        val current = _uiState.value
        val normalized = thresholdBpm
            .coerceAtMost(current.highHeartRateCheck.thresholdBpm - HeartRateThresholdMinimumGapBpm)
            .coerceIn(
                PreferencesRepository.MIN_LOW_HEART_RATE_THRESHOLD_BPM,
                PreferencesRepository.MAX_LOW_HEART_RATE_THRESHOLD_BPM,
            )
        onLowHeartRateThresholdChanged(normalized)
        _uiState.value = current.copy(
            lowHeartRateCheck = current.heartRateThresholdCheck(
                type = HeartRateThresholdCheckType.LOW,
                thresholdBpm = normalized,
            )
        )
    }
}

private fun HeartUiState.withDeletedVitalsMeasurementEntry(
    type: VitalsMeasurementType,
    entryId: String,
): HeartUiState =
    when (type) {
        VitalsMeasurementType.BLOOD_PRESSURE -> copy(
            bloodPressure = bloodPressure.filterNot { it.id == entryId },
            error = null,
        )
        VitalsMeasurementType.SPO2 -> copy(
            spO2 = spO2.filterNot { it.id == entryId },
            error = null,
        )
        VitalsMeasurementType.RESPIRATORY_RATE -> copy(
            respiratoryRate = respiratoryRate.filterNot { it.id == entryId },
            error = null,
        )
        VitalsMeasurementType.BODY_TEMPERATURE -> copy(
            bodyTemperature = bodyTemperature.filterNot { it.id == entryId },
            error = null,
        )
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

private data class HeartVitalsSummary(
    val hasVitalsData: Boolean,
    val latestBloodPressure: BloodPressureEntry?,
    val latestSpO2: SpO2Entry?,
    val latestRespiratoryRate: RespiratoryRateEntry?,
    val latestBodyTemperature: BodyTempEntry?,
    val latestVo2Max: Vo2MaxEntry?,
)

private fun HeartLoadResult.vitalsSummary(): HeartVitalsSummary =
    HeartVitalsSummary(
        hasVitalsData = bloodPressure.isNotEmpty() ||
            spO2.isNotEmpty() ||
            respiratoryRate.isNotEmpty() ||
            bodyTemperature.isNotEmpty() ||
            vo2Max.isNotEmpty(),
        latestBloodPressure = bloodPressure.maxByOrNull { it.time },
        latestSpO2 = spO2.maxByOrNull { it.time },
        latestRespiratoryRate = respiratoryRate.maxByOrNull { it.time },
        latestBodyTemperature = bodyTemperature.maxByOrNull { it.time },
        latestVo2Max = vo2Max.maxByOrNull { it.time },
    )

private fun HeartLoadResult.heartRateThresholdCheck(
    selectedRange: TimeRange,
    type: HeartRateThresholdCheckType,
    thresholdBpm: Int,
): HeartRateThresholdCheck {
    val hasData = if (selectedRange == TimeRange.DAY) {
        daySamples.isNotEmpty()
    } else {
        dailySummaries.isNotEmpty()
    }
    val count = when (type) {
        HeartRateThresholdCheckType.HIGH -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute >= thresholdBpm }
        } else {
            dailySummaries.count { it.maxBpm >= thresholdBpm }
        }
        HeartRateThresholdCheckType.LOW -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute <= thresholdBpm }
        } else {
            dailySummaries.count { it.minBpm <= thresholdBpm }
        }
    }
    return HeartRateThresholdCheck(
        type = type,
        thresholdBpm = thresholdBpm,
        count = count,
        hasData = hasData,
    )
}

private fun HeartUiState.heartRateThresholdCheck(
    type: HeartRateThresholdCheckType,
    thresholdBpm: Int,
): HeartRateThresholdCheck {
    val hasData = if (selectedRange == TimeRange.DAY) {
        daySamples.isNotEmpty()
    } else {
        dailySummaries.isNotEmpty()
    }
    val count = when (type) {
        HeartRateThresholdCheckType.HIGH -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute >= thresholdBpm }
        } else {
            dailySummaries.count { it.maxBpm >= thresholdBpm }
        }
        HeartRateThresholdCheckType.LOW -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute <= thresholdBpm }
        } else {
            dailySummaries.count { it.minBpm <= thresholdBpm }
        }
    }
    return HeartRateThresholdCheck(
        type = type,
        thresholdBpm = thresholdBpm,
        count = count,
        hasData = hasData,
    )
}

private fun HeartMetric.toHeartPeriodMetric(): HeartPeriodMetric =
    when (this) {
        HeartMetric.AVERAGE_HEART_RATE -> HeartPeriodMetric.AVERAGE_HEART_RATE
        HeartMetric.RESTING_HEART_RATE -> HeartPeriodMetric.RESTING_HEART_RATE
        HeartMetric.HRV -> HeartPeriodMetric.HRV
        else -> error("$this is not a heart period metric")
    }

private fun HeartMetric.toVitalsPeriodMetric(): VitalsPeriodMetric =
    when (this) {
        HeartMetric.BLOOD_PRESSURE -> VitalsPeriodMetric.BLOOD_PRESSURE
        HeartMetric.SPO2 -> VitalsPeriodMetric.SPO2
        HeartMetric.VO2_MAX -> VitalsPeriodMetric.VO2_MAX
        HeartMetric.RESPIRATORY_RATE -> VitalsPeriodMetric.RESPIRATORY_RATE
        HeartMetric.BODY_TEMPERATURE -> VitalsPeriodMetric.BODY_TEMPERATURE
        else -> error("$this is not a vitals period metric")
    }

private fun HeartPeriodData.toLoadResult(): HeartLoadResult =
    HeartLoadResult(
        daySamples = daySamples,
        previousDaySamples = previousDaySamples,
        dailySummaries = dailySummaries,
        previousDailySummaries = previousDailySummaries,
        baselineDailySummaries = baselineDailySummaries,
        dayRestingBpm = dayRestingBpm,
        previousDayRestingBpm = previousDayRestingBpm,
        dayHrvMs = dayHrvMs,
        previousDayHrvMs = previousDayHrvMs,
        dailyRestingHR = dailyRestingHR,
        previousDailyRestingHR = previousDailyRestingHR,
        baselineDailyRestingHR = baselineDailyRestingHR,
        dailyHrv = dailyHrv,
        previousDailyHrv = previousDailyHrv,
        baselineDailyHrv = baselineDailyHrv,
    )

private fun VitalsPeriodData.toLoadResult(): HeartLoadResult =
    HeartLoadResult(
        missingVitalsPermissions = missingVitalsPermissions,
        bloodPressure = bloodPressure,
        previousBloodPressure = previousBloodPressure,
        baselineBloodPressure = baselineBloodPressure,
        spO2 = spO2,
        previousSpO2 = previousSpO2,
        baselineSpO2 = baselineSpO2,
        respiratoryRate = respiratoryRate,
        previousRespiratoryRate = previousRespiratoryRate,
        baselineRespiratoryRate = baselineRespiratoryRate,
        bodyTemperature = bodyTemperature,
        previousBodyTemperature = previousBodyTemperature,
        baselineBodyTemperature = baselineBodyTemperature,
        vo2Max = vo2Max,
        previousVo2Max = previousVo2Max,
        baselineVo2Max = baselineVo2Max,
    )

private fun HeartLoadResult.merge(other: HeartLoadResult): HeartLoadResult =
    HeartLoadResult(
        daySamples = daySamples + other.daySamples,
        previousDaySamples = previousDaySamples + other.previousDaySamples,
        dailySummaries = dailySummaries + other.dailySummaries,
        previousDailySummaries = previousDailySummaries + other.previousDailySummaries,
        baselineDailySummaries = baselineDailySummaries + other.baselineDailySummaries,
        dayRestingBpm = dayRestingBpm ?: other.dayRestingBpm,
        previousDayRestingBpm = previousDayRestingBpm ?: other.previousDayRestingBpm,
        dayHrvMs = dayHrvMs ?: other.dayHrvMs,
        previousDayHrvMs = previousDayHrvMs ?: other.previousDayHrvMs,
        dailyRestingHR = dailyRestingHR + other.dailyRestingHR,
        previousDailyRestingHR = previousDailyRestingHR + other.previousDailyRestingHR,
        baselineDailyRestingHR = baselineDailyRestingHR + other.baselineDailyRestingHR,
        dailyHrv = dailyHrv + other.dailyHrv,
        previousDailyHrv = previousDailyHrv + other.previousDailyHrv,
        baselineDailyHrv = baselineDailyHrv + other.baselineDailyHrv,
        missingVitalsPermissions = missingVitalsPermissions + other.missingVitalsPermissions,
        bloodPressure = bloodPressure + other.bloodPressure,
        previousBloodPressure = previousBloodPressure + other.previousBloodPressure,
        baselineBloodPressure = baselineBloodPressure + other.baselineBloodPressure,
        spO2 = spO2 + other.spO2,
        previousSpO2 = previousSpO2 + other.previousSpO2,
        baselineSpO2 = baselineSpO2 + other.baselineSpO2,
        respiratoryRate = respiratoryRate + other.respiratoryRate,
        previousRespiratoryRate = previousRespiratoryRate + other.previousRespiratoryRate,
        baselineRespiratoryRate = baselineRespiratoryRate + other.baselineRespiratoryRate,
        bodyTemperature = bodyTemperature + other.bodyTemperature,
        previousBodyTemperature = previousBodyTemperature + other.previousBodyTemperature,
        baselineBodyTemperature = baselineBodyTemperature + other.baselineBodyTemperature,
        vo2Max = vo2Max + other.vo2Max,
        previousVo2Max = previousVo2Max + other.previousVo2Max,
        baselineVo2Max = baselineVo2Max + other.baselineVo2Max,
    )

private fun heartMetricFromRoute(metricId: String?): HeartMetric? {
    if (metricId == null) return null
    return when (metricId) {
        "AVG_HEART_RATE",
        "AVERAGE_HEART_RATE" -> HeartMetric.AVERAGE_HEART_RATE
        "RESTING_HEART_RATE" -> HeartMetric.RESTING_HEART_RATE
        "HRV" -> HeartMetric.HRV
        "BLOOD_PRESSURE" -> HeartMetric.BLOOD_PRESSURE
        "SPO2" -> HeartMetric.SPO2
        "VO2_MAX" -> HeartMetric.VO2_MAX
        "RESPIRATORY_RATE" -> HeartMetric.RESPIRATORY_RATE
        "BODY_TEMPERATURE" -> HeartMetric.BODY_TEMPERATURE
        else -> HeartMetric.AVERAGE_HEART_RATE
    }
}
