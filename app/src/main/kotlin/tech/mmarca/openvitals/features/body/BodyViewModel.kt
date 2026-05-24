package tech.mmarca.openvitals.features.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyRepository
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
    val heightCm: Double? = null,
    val bodyFatEntries: List<BodyFatEntry> = emptyList(),
    val previousBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val leanMassKg: Double? = null,
    val bmrKcal: Double? = null,
    val boneMassKg: Double? = null,
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                when (selectedMetric) {
                    BodyMetric.WEIGHT -> BodyLoadResult(
                        weightEntries = repository.loadWeightEntries(period.start, period.end),
                        previousWeightEntries = repository.loadWeightEntries(previousPeriod.start, previousPeriod.end),
                    )
                    BodyMetric.HEIGHT -> BodyLoadResult(
                        heightCm = repository.loadLatestHeight(),
                    )
                    BodyMetric.BMI -> BodyLoadResult(
                        weightEntries = repository.loadWeightEntries(period.start, period.end),
                        previousWeightEntries = repository.loadWeightEntries(previousPeriod.start, previousPeriod.end),
                        heightCm = repository.loadLatestHeight(),
                    )
                    BodyMetric.BODY_FAT -> BodyLoadResult(
                        bodyFatEntries = repository.loadBodyFatEntries(period.start, period.end),
                        previousBodyFatEntries = repository.loadBodyFatEntries(previousPeriod.start, previousPeriod.end),
                    )
                    BodyMetric.LEAN_MASS -> BodyLoadResult(
                        leanMassKg = repository.loadLatestLeanBodyMass(),
                    )
                    BodyMetric.BMR -> BodyLoadResult(
                        bmrKcal = repository.loadLatestBMR(),
                    )
                    BodyMetric.BONE_MASS -> BodyLoadResult(
                        boneMassKg = repository.loadLatestBoneMass(),
                    )
                }
            }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        weightEntries = result.weightEntries,
                        previousWeightEntries = result.previousWeightEntries,
                        heightCm = result.heightCm,
                        bodyFatEntries = result.bodyFatEntries,
                        previousBodyFatEntries = result.previousBodyFatEntries,
                        leanMassKg = result.leanMassKg,
                        bmrKcal = result.bmrKcal,
                        boneMassKg = result.boneMassKg,
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
        val heightCm: Double? = null,
        val bodyFatEntries: List<BodyFatEntry> = emptyList(),
        val previousBodyFatEntries: List<BodyFatEntry> = emptyList(),
        val leanMassKg: Double? = null,
        val bmrKcal: Double? = null,
        val boneMassKg: Double? = null,
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
