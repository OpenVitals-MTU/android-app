package tech.mmarca.openvitals.features.body

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyPeriodData
import tech.mmarca.openvitals.data.repository.BodyPeriodMetric
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
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

data class BodyUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val weightEntries: List<WeightEntry> = emptyList(),
    val previousWeightEntries: List<WeightEntry> = emptyList(),
    val baselineWeightEntries: List<WeightEntry> = emptyList(),
    val heightCm: Double? = null,
    val heightEntries: List<HeightEntry> = emptyList(),
    val previousHeightEntries: List<HeightEntry> = emptyList(),
    val baselineHeightEntries: List<HeightEntry> = emptyList(),
    val bodyFatEntries: List<BodyFatEntry> = emptyList(),
    val previousBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val baselineBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val leanMassKg: Double? = null,
    val leanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val previousLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val baselineLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val bmrKcal: Double? = null,
    val bmrEntries: List<BmrEntry> = emptyList(),
    val previousBmrEntries: List<BmrEntry> = emptyList(),
    val baselineBmrEntries: List<BmrEntry> = emptyList(),
    val boneMassKg: Double? = null,
    val boneMassEntries: List<BoneMassEntry> = emptyList(),
    val previousBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val baselineBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val bodyWaterMassKg: Double? = null,
    val bodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val previousBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val baselineBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val latestWeightKg: Double? = weightEntries.maxByOrNull { it.time }?.weightKg,
    val previousLatestWeightKg: Double? = previousWeightEntries.maxByOrNull { it.time }?.weightKg,
    val firstWeightKg: Double? = weightEntries.minByOrNull { it.time }?.weightKg,
    val weightChangKg: Double? = if (latestWeightKg != null && firstWeightKg != null && latestWeightKg != firstWeightKg) {
        latestWeightKg - firstWeightKg
    } else {
        null
    },
    val latestBodyFatPercent: Double? = bodyFatEntries.maxByOrNull { it.time }?.percent,
    val previousLatestBodyFatPercent: Double? = previousBodyFatEntries.maxByOrNull { it.time }?.percent,
    val bmi: Double? = latestWeightKg.bmiWith(heightCm),
    val latestHeightCm: Double? = heightEntries.maxByOrNull { it.time }?.heightCm ?: heightCm,
    val previousLatestHeightCm: Double? = previousHeightEntries.maxByOrNull { it.time }?.heightCm,
    val latestLeanMassKg: Double? = leanMassEntries.maxByOrNull { it.time }?.massKg ?: leanMassKg,
    val previousLatestLeanMassKg: Double? = previousLeanMassEntries.maxByOrNull { it.time }?.massKg,
    val latestBmrKcal: Double? = bmrEntries.maxByOrNull { it.time }?.kcalPerDay ?: bmrKcal,
    val previousLatestBmrKcal: Double? = previousBmrEntries.maxByOrNull { it.time }?.kcalPerDay,
    val latestBoneMassKg: Double? = boneMassEntries.maxByOrNull { it.time }?.massKg ?: boneMassKg,
    val previousLatestBoneMassKg: Double? = previousBoneMassEntries.maxByOrNull { it.time }?.massKg,
    val latestBodyWaterMassKg: Double? = bodyWaterMassEntries.maxByOrNull { it.time }?.massKg ?: bodyWaterMassKg,
    val previousLatestBodyWaterMassKg: Double? = previousBodyWaterMassEntries.maxByOrNull { it.time }?.massKg,
    val previousBmi: Double? = previousLatestWeightKg.bmiWith(heightCm),
    val error: String? = null,
)

@HiltViewModel
class BodyViewModel(
    private val repository: BodyRepository,
    initialRange: TimeRange = TimeRange.MONTH,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    private val selectedMetric: BodyMetric = BodyMetric.WEIGHT,
    private val weekPeriodModeChanges: Flow<WeekPeriodMode> = emptyFlow(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: BodyRepository,
        preferencesRepository: PreferencesRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.BODY),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        selectedMetric = bodyMetricFromRoute(savedStateHandle[METRIC_ID_ARG]),
        weekPeriodModeChanges = preferencesRepository.weekPeriodModeFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.BODY, range)
        },
    )

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        BodyUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
        )
    )
    val uiState: StateFlow<BodyUiState> = _uiState.asStateFlow()
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

    fun deleteBodyMeasurementEntry(type: BodyMeasurementType, entryId: String) {
        if (entryId.isBlank()) return
        val entryIsOpenVitals = when (type) {
            BodyMeasurementType.WEIGHT -> _uiState.value.weightEntries
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
            BodyMeasurementType.HEIGHT -> _uiState.value.heightEntries
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
            BodyMeasurementType.BODY_FAT -> _uiState.value.bodyFatEntries
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
        } ?: return
        if (!entryIsOpenVitals) return

        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.withDeletedBodyMeasurementEntry(type, entryId)
            runCatching {
                repository.deleteBodyMeasurementEntry(type, entryId)
            }.onSuccess {
                load(RefreshMode.FORCE)
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.message)
            }
        }
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
                if (refreshMode == RefreshMode.NORMAL) {
                    repository.loadBodyPeriod(query, BodyPeriodMetric.ALL)
                } else {
                    repository.loadBodyPeriod(query, BodyPeriodMetric.ALL, refreshMode)
                }
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    val heightCm = result.heightEntries.maxByOrNull { it.time }?.heightCm ?: result.heightCm
                    val leanMassKg = result.leanMassEntries.maxByOrNull { it.time }?.massKg ?: result.leanMassKg
                    val bmrKcal = result.bmrEntries.maxByOrNull { it.time }?.kcalPerDay ?: result.bmrKcal
                    val boneMassKg = result.boneMassEntries.maxByOrNull { it.time }?.massKg ?: result.boneMassKg
                    val bodyWaterMassKg = result.bodyWaterMassEntries.maxByOrNull { it.time }?.massKg
                        ?: result.bodyWaterMassKg
                    val summary = result.summary(
                        heightCm = heightCm,
                        leanMassKg = leanMassKg,
                        bmrKcal = bmrKcal,
                        boneMassKg = boneMassKg,
                        bodyWaterMassKg = bodyWaterMassKg,
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        weightEntries = result.weightEntries,
                        previousWeightEntries = result.previousWeightEntries,
                        baselineWeightEntries = result.baselineWeightEntries,
                        heightCm = heightCm,
                        heightEntries = result.heightEntries,
                        previousHeightEntries = result.previousHeightEntries,
                        baselineHeightEntries = result.baselineHeightEntries,
                        bodyFatEntries = result.bodyFatEntries,
                        previousBodyFatEntries = result.previousBodyFatEntries,
                        baselineBodyFatEntries = result.baselineBodyFatEntries,
                        leanMassKg = leanMassKg,
                        leanMassEntries = result.leanMassEntries,
                        previousLeanMassEntries = result.previousLeanMassEntries,
                        baselineLeanMassEntries = result.baselineLeanMassEntries,
                        bmrKcal = bmrKcal,
                        bmrEntries = result.bmrEntries,
                        previousBmrEntries = result.previousBmrEntries,
                        baselineBmrEntries = result.baselineBmrEntries,
                        boneMassKg = boneMassKg,
                        boneMassEntries = result.boneMassEntries,
                        previousBoneMassEntries = result.previousBoneMassEntries,
                        baselineBoneMassEntries = result.baselineBoneMassEntries,
                        bodyWaterMassKg = bodyWaterMassKg,
                        bodyWaterMassEntries = result.bodyWaterMassEntries,
                        previousBodyWaterMassEntries = result.previousBodyWaterMassEntries,
                        baselineBodyWaterMassEntries = result.baselineBodyWaterMassEntries,
                        latestWeightKg = summary.latestWeightKg,
                        previousLatestWeightKg = summary.previousLatestWeightKg,
                        firstWeightKg = summary.firstWeightKg,
                        weightChangKg = summary.weightChangeKg,
                        latestBodyFatPercent = summary.latestBodyFatPercent,
                        previousLatestBodyFatPercent = summary.previousLatestBodyFatPercent,
                        bmi = summary.bmi,
                        latestHeightCm = summary.latestHeightCm,
                        previousLatestHeightCm = summary.previousLatestHeightCm,
                        latestLeanMassKg = summary.latestLeanMassKg,
                        previousLatestLeanMassKg = summary.previousLatestLeanMassKg,
                        latestBmrKcal = summary.latestBmrKcal,
                        previousLatestBmrKcal = summary.previousLatestBmrKcal,
                        latestBoneMassKg = summary.latestBoneMassKg,
                        previousLatestBoneMassKg = summary.previousLatestBoneMassKg,
                        latestBodyWaterMassKg = summary.latestBodyWaterMassKg,
                        previousLatestBodyWaterMassKg = summary.previousLatestBodyWaterMassKg,
                        previousBmi = summary.previousBmi,
                    )
                }
                .onFailure {
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
}

private fun BodyUiState.withDeletedBodyMeasurementEntry(
    type: BodyMeasurementType,
    entryId: String,
): BodyUiState =
    when (type) {
        BodyMeasurementType.WEIGHT -> copy(
            weightEntries = weightEntries.filterNot { it.id == entryId },
            error = null,
        )
        BodyMeasurementType.HEIGHT -> copy(
            heightEntries = heightEntries.filterNot { it.id == entryId },
            error = null,
        )
        BodyMeasurementType.BODY_FAT -> copy(
            bodyFatEntries = bodyFatEntries.filterNot { it.id == entryId },
            error = null,
        )
    }

private fun BodyMetric.toPeriodMetric(): BodyPeriodMetric =
    when (this) {
        BodyMetric.WEIGHT -> BodyPeriodMetric.WEIGHT
        BodyMetric.HEIGHT -> BodyPeriodMetric.HEIGHT
        BodyMetric.BMI -> BodyPeriodMetric.BMI
        BodyMetric.BODY_FAT -> BodyPeriodMetric.BODY_FAT
        BodyMetric.LEAN_MASS -> BodyPeriodMetric.LEAN_MASS
        BodyMetric.BMR -> BodyPeriodMetric.BMR
        BodyMetric.BONE_MASS -> BodyPeriodMetric.BONE_MASS
        BodyMetric.BODY_WATER_MASS -> BodyPeriodMetric.BODY_WATER_MASS
    }

private fun bodyMetricFromRoute(metricId: String?): BodyMetric =
    runCatching { metricId?.let(BodyMetric::valueOf) }.getOrNull() ?: BodyMetric.WEIGHT

private data class BodySummary(
    val latestWeightKg: Double?,
    val previousLatestWeightKg: Double?,
    val firstWeightKg: Double?,
    val weightChangeKg: Double?,
    val latestBodyFatPercent: Double?,
    val previousLatestBodyFatPercent: Double?,
    val bmi: Double?,
    val latestHeightCm: Double?,
    val previousLatestHeightCm: Double?,
    val latestLeanMassKg: Double?,
    val previousLatestLeanMassKg: Double?,
    val latestBmrKcal: Double?,
    val previousLatestBmrKcal: Double?,
    val latestBoneMassKg: Double?,
    val previousLatestBoneMassKg: Double?,
    val latestBodyWaterMassKg: Double?,
    val previousLatestBodyWaterMassKg: Double?,
    val previousBmi: Double?,
)

private fun BodyPeriodData.summary(
    heightCm: Double?,
    leanMassKg: Double?,
    bmrKcal: Double?,
    boneMassKg: Double?,
    bodyWaterMassKg: Double?,
): BodySummary {
    val latestWeightKg = weightEntries.maxByOrNull { it.time }?.weightKg
    val previousLatestWeightKg = previousWeightEntries.maxByOrNull { it.time }?.weightKg
    val firstWeightKg = weightEntries.minByOrNull { it.time }?.weightKg
    val latestHeightCm = heightEntries.maxByOrNull { it.time }?.heightCm ?: heightCm
    return BodySummary(
        latestWeightKg = latestWeightKg,
        previousLatestWeightKg = previousLatestWeightKg,
        firstWeightKg = firstWeightKg,
        weightChangeKg = if (latestWeightKg != null && firstWeightKg != null && latestWeightKg != firstWeightKg) {
            latestWeightKg - firstWeightKg
        } else {
            null
        },
        latestBodyFatPercent = bodyFatEntries.maxByOrNull { it.time }?.percent,
        previousLatestBodyFatPercent = previousBodyFatEntries.maxByOrNull { it.time }?.percent,
        bmi = latestWeightKg.bmiWith(heightCm),
        latestHeightCm = latestHeightCm,
        previousLatestHeightCm = previousHeightEntries.maxByOrNull { it.time }?.heightCm,
        latestLeanMassKg = leanMassEntries.maxByOrNull { it.time }?.massKg ?: leanMassKg,
        previousLatestLeanMassKg = previousLeanMassEntries.maxByOrNull { it.time }?.massKg,
        latestBmrKcal = bmrEntries.maxByOrNull { it.time }?.kcalPerDay ?: bmrKcal,
        previousLatestBmrKcal = previousBmrEntries.maxByOrNull { it.time }?.kcalPerDay,
        latestBoneMassKg = boneMassEntries.maxByOrNull { it.time }?.massKg ?: boneMassKg,
        previousLatestBoneMassKg = previousBoneMassEntries.maxByOrNull { it.time }?.massKg,
        latestBodyWaterMassKg = bodyWaterMassEntries.maxByOrNull { it.time }?.massKg ?: bodyWaterMassKg,
        previousLatestBodyWaterMassKg = previousBodyWaterMassEntries.maxByOrNull { it.time }?.massKg,
        previousBmi = previousLatestWeightKg.bmiWith(heightCm),
    )
}

private fun Double?.bmiWith(heightCm: Double?): Double? {
    val weight = this ?: return null
    val height = heightCm ?: return null
    if (height <= 0.0) return null
    val heightMeters = height / 100.0
    return weight / (heightMeters * heightMeters)
}
