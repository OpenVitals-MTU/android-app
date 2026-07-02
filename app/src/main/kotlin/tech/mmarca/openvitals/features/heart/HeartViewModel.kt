package tech.mmarca.openvitals.features.heart

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.usecase.HeartPeriodLoadRequest
import tech.mmarca.openvitals.domain.usecase.HeartPeriodLoadResult
import tech.mmarca.openvitals.domain.usecase.LoadHeartPeriodUseCase
import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

@Immutable
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
    val dayRestingSamples: List<RestingHeartRateSample> = emptyList(),
    val dayRestingBpm: Long? = null,
    val previousDayRestingBpm: Long? = null,
    val dayHrvSamples: List<HrvSample> = emptyList(),
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
    val bloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val previousBloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val baselineBloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val skinTemperature: List<SkinTemperatureEntry> = emptyList(),
    val previousSkinTemperature: List<SkinTemperatureEntry> = emptyList(),
    val baselineSkinTemperature: List<SkinTemperatureEntry> = emptyList(),
    val hasVitalsData: Boolean = false,
    val latestBloodPressure: BloodPressureEntry? = null,
    val latestSpO2: SpO2Entry? = null,
    val latestRespiratoryRate: RespiratoryRateEntry? = null,
    val latestBodyTemperature: BodyTempEntry? = null,
    val latestVo2Max: Vo2MaxEntry? = null,
    val latestBloodGlucose: BloodGlucoseEntry? = null,
    val latestSkinTemperature: SkinTemperatureEntry? = null,
    val missingVitalsPermissions: Set<String> = emptySet(),
    val highHeartRateCheck: HeartRateThresholdCheck = HeartRateThresholdCheck(
        type = HeartRateThresholdCheckType.HIGH,
        thresholdBpm = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
    ),
    val lowHeartRateCheck: HeartRateThresholdCheck = HeartRateThresholdCheck(
        type = HeartRateThresholdCheckType.LOW,
        thresholdBpm = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
    ),
    val display: HeartDisplayState = HeartDisplayState(),
    val error: ScreenError? = null,
)

@HiltViewModel
class HeartViewModel(
    private val loadHeartPeriodUseCase: LoadHeartPeriodUseCase,
    private val vitalsRepository: VitalsRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
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
        loadHeartPeriodUseCase: LoadHeartPeriodUseCase,
        vitalsRepository: VitalsRepository,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: SavedStateHandle,
        dispatchers: DispatcherProvider,
    ) : this(
        loadHeartPeriodUseCase = loadHeartPeriodUseCase,
        vitalsRepository = vitalsRepository,
        dispatchers = dispatchers,
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

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load(RefreshMode.FORCE)
            return
        }
        applyPeriodSelection(selection)
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
                load(RefreshMode.FORCE)
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.toScreenError())
            }
        }
    }

    fun onVitalsPermissionsResult(granted: Set<String>) {
        load(RefreshMode.FORCE)
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
                loadHeartPeriodUseCase(
                    query = query,
                    request = selectedMetric.toLoadRequest(),
                    refreshMode = refreshMode,
                )
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val loadedState = withContext(dispatchers.default) {
                    HeartPresentationMapper.applyLoadResult(
                        current = _uiState.value,
                        query = query,
                        metric = selectedMetric,
                        result = result,
                    )
                }
                if (!isCurrent) return@load
                _uiState.value = loadedState
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
            ),
        ).withDisplay(selectedMetric)
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
            ),
        ).withDisplay(selectedMetric)
    }

    private fun HeartUiState.withDisplay(metric: HeartMetric?): HeartUiState {
        val query = PeriodLoadQuery(
            range = selectedRange,
            anchorDate = selectedDate,
            weekPeriodMode = weekPeriodMode,
        )
        return copy(
            display = HeartPresentationMapper.build(
                query = query,
                metric = metric,
                state = this,
            ),
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

private fun HeartMetric?.toLoadRequest(): HeartPeriodLoadRequest =
    when (this) {
        null -> HeartPeriodLoadRequest.Combined
        HeartMetric.AVERAGE_HEART_RATE -> HeartPeriodLoadRequest.HeartOnly(HeartPeriodMetric.AVERAGE_HEART_RATE)
        HeartMetric.RESTING_HEART_RATE -> HeartPeriodLoadRequest.HeartOnly(HeartPeriodMetric.RESTING_HEART_RATE)
        HeartMetric.HRV -> HeartPeriodLoadRequest.HeartOnly(HeartPeriodMetric.HRV)
        HeartMetric.BLOOD_PRESSURE -> HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.BLOOD_PRESSURE)
        HeartMetric.SPO2 -> HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.SPO2)
        HeartMetric.VO2_MAX -> HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.VO2_MAX)
        HeartMetric.RESPIRATORY_RATE -> HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.RESPIRATORY_RATE)
        HeartMetric.BODY_TEMPERATURE -> HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.BODY_TEMPERATURE)
        HeartMetric.BLOOD_GLUCOSE -> HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.BLOOD_GLUCOSE)
        HeartMetric.SKIN_TEMPERATURE -> HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.SKIN_TEMPERATURE)
    }

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
        "BLOOD_GLUCOSE" -> HeartMetric.BLOOD_GLUCOSE
        "SKIN_TEMPERATURE" -> HeartMetric.SKIN_TEMPERATURE
        else -> HeartMetric.AVERAGE_HEART_RATE
    }
}
