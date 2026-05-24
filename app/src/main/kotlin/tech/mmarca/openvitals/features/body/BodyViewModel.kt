package tech.mmarca.openvitals.features.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.BmrEntry
import tech.mmarca.openvitals.data.model.BoneMassEntry
import tech.mmarca.openvitals.data.model.HeightEntry
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.LeanBodyMassEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BodyUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
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
    val error: String? = null,
) {
    val latestWeightKg: Double? get() = weightEntries.maxByOrNull { it.time }?.weightKg
    val previousLatestWeightKg: Double? get() = previousWeightEntries.maxByOrNull { it.time }?.weightKg
    val firstWeightKg: Double? get() = weightEntries.minByOrNull { it.time }?.weightKg
    val weightChangKg: Double?
        get() = if (latestWeightKg != null && firstWeightKg != null && latestWeightKg != firstWeightKg)
            latestWeightKg!! - firstWeightKg!! else null
    val latestBodyFatPercent: Double? get() = bodyFatEntries.maxByOrNull { it.time }?.percent
    val previousLatestBodyFatPercent: Double? get() = previousBodyFatEntries.maxByOrNull { it.time }?.percent
    val bmi: Double?
        get() {
            val w = latestWeightKg ?: return null
            val h = heightCm ?: return null
            if (h <= 0) return null
            val hm = h / 100.0
            return w / (hm * hm)
        }
    val latestHeightCm: Double? get() = heightEntries.maxByOrNull { it.time }?.heightCm ?: heightCm
    val previousLatestHeightCm: Double? get() = previousHeightEntries.maxByOrNull { it.time }?.heightCm
    val latestLeanMassKg: Double? get() = leanMassEntries.maxByOrNull { it.time }?.massKg ?: leanMassKg
    val previousLatestLeanMassKg: Double? get() = previousLeanMassEntries.maxByOrNull { it.time }?.massKg
    val latestBmrKcal: Double? get() = bmrEntries.maxByOrNull { it.time }?.kcalPerDay ?: bmrKcal
    val previousLatestBmrKcal: Double? get() = previousBmrEntries.maxByOrNull { it.time }?.kcalPerDay
    val latestBoneMassKg: Double? get() = boneMassEntries.maxByOrNull { it.time }?.massKg ?: boneMassKg
    val previousLatestBoneMassKg: Double? get() = previousBoneMassEntries.maxByOrNull { it.time }?.massKg
    val previousBmi: Double?
        get() {
            val w = previousLatestWeightKg ?: return null
            val h = heightCm ?: return null
            if (h <= 0) return null
            val hm = h / 100.0
            return w / (hm * hm)
        }
}

class BodyViewModel(
    private val repository: BodyRepository,
    initialRange: TimeRange = TimeRange.MONTH,
    private val selectedMetric: BodyMetric = BodyMetric.WEIGHT,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyUiState(selectedRange = initialRange))
    val uiState: StateFlow<BodyUiState> = _uiState.asStateFlow()

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

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            val previousPeriod = previousPeriodFor(range, date)
            val baselinePeriod = baselinePeriodBefore(period)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                when (selectedMetric) {
                    BodyMetric.WEIGHT -> BodyLoadResult(
                        weightEntries = repository.loadWeightEntries(period.start, period.end),
                        previousWeightEntries = repository.loadWeightEntries(previousPeriod.start, previousPeriod.end),
                        baselineWeightEntries = repository.loadWeightEntries(baselinePeriod.start, baselinePeriod.end),
                    )
                    BodyMetric.HEIGHT -> BodyLoadResult(
                        heightEntries = repository.loadHeightEntries(period.start, period.end),
                        previousHeightEntries = repository.loadHeightEntries(previousPeriod.start, previousPeriod.end),
                        baselineHeightEntries = repository.loadHeightEntries(baselinePeriod.start, baselinePeriod.end),
                    )
                    BodyMetric.BMI -> BodyLoadResult(
                        weightEntries = repository.loadWeightEntries(period.start, period.end),
                        previousWeightEntries = repository.loadWeightEntries(previousPeriod.start, previousPeriod.end),
                        baselineWeightEntries = repository.loadWeightEntries(baselinePeriod.start, baselinePeriod.end),
                        heightCm = repository.loadLatestHeight(),
                    )
                    BodyMetric.BODY_FAT -> BodyLoadResult(
                        bodyFatEntries = repository.loadBodyFatEntries(period.start, period.end),
                        previousBodyFatEntries = repository.loadBodyFatEntries(previousPeriod.start, previousPeriod.end),
                        baselineBodyFatEntries = repository.loadBodyFatEntries(baselinePeriod.start, baselinePeriod.end),
                    )
                    BodyMetric.LEAN_MASS -> BodyLoadResult(
                        leanMassEntries = repository.loadLeanBodyMassEntries(period.start, period.end),
                        previousLeanMassEntries = repository.loadLeanBodyMassEntries(previousPeriod.start, previousPeriod.end),
                        baselineLeanMassEntries = repository.loadLeanBodyMassEntries(baselinePeriod.start, baselinePeriod.end),
                    )
                    BodyMetric.BMR -> BodyLoadResult(
                        bmrEntries = repository.loadBmrEntries(period.start, period.end),
                        previousBmrEntries = repository.loadBmrEntries(previousPeriod.start, previousPeriod.end),
                        baselineBmrEntries = repository.loadBmrEntries(baselinePeriod.start, baselinePeriod.end),
                    )
                    BodyMetric.BONE_MASS -> BodyLoadResult(
                        boneMassEntries = repository.loadBoneMassEntries(period.start, period.end),
                        previousBoneMassEntries = repository.loadBoneMassEntries(previousPeriod.start, previousPeriod.end),
                        baselineBoneMassEntries = repository.loadBoneMassEntries(baselinePeriod.start, baselinePeriod.end),
                    )
                }
            }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        weightEntries = result.weightEntries,
                        previousWeightEntries = result.previousWeightEntries,
                        baselineWeightEntries = result.baselineWeightEntries,
                        heightCm = result.heightEntries.maxByOrNull { it.time }?.heightCm ?: result.heightCm,
                        heightEntries = result.heightEntries,
                        previousHeightEntries = result.previousHeightEntries,
                        baselineHeightEntries = result.baselineHeightEntries,
                        bodyFatEntries = result.bodyFatEntries,
                        previousBodyFatEntries = result.previousBodyFatEntries,
                        baselineBodyFatEntries = result.baselineBodyFatEntries,
                        leanMassKg = result.leanMassEntries.maxByOrNull { it.time }?.massKg ?: result.leanMassKg,
                        leanMassEntries = result.leanMassEntries,
                        previousLeanMassEntries = result.previousLeanMassEntries,
                        baselineLeanMassEntries = result.baselineLeanMassEntries,
                        bmrKcal = result.bmrEntries.maxByOrNull { it.time }?.kcalPerDay ?: result.bmrKcal,
                        bmrEntries = result.bmrEntries,
                        previousBmrEntries = result.previousBmrEntries,
                        baselineBmrEntries = result.baselineBmrEntries,
                        boneMassKg = result.boneMassEntries.maxByOrNull { it.time }?.massKg ?: result.boneMassKg,
                        boneMassEntries = result.boneMassEntries,
                        previousBoneMassEntries = result.previousBoneMassEntries,
                        baselineBoneMassEntries = result.baselineBoneMassEntries,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = it.message,
                    )
                }
        }
    }

    private data class BodyLoadResult(
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
    )

    private val periodSelection: PeriodSelection
        get() = PeriodSelection(_uiState.value.selectedRange, _uiState.value.selectedDate)

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}
