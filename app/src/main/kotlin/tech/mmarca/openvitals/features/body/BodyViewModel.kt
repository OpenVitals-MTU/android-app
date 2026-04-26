package tech.mmarca.openvitals.features.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.ui.components.periodFor
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BodyUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val weightEntries: List<WeightEntry> = emptyList(),
    val heightCm: Double? = null,
    val bodyFatEntries: List<BodyFatEntry> = emptyList(),
    val leanMassKg: Double? = null,
    val bmrKcal: Double? = null,
    val boneMassKg: Double? = null,
    val error: String? = null,
) {
    val latestWeightKg: Double? get() = weightEntries.maxByOrNull { it.time }?.weightKg
    val firstWeightKg: Double? get() = weightEntries.minByOrNull { it.time }?.weightKg
    val weightChangKg: Double?
        get() = if (latestWeightKg != null && firstWeightKg != null && latestWeightKg != firstWeightKg)
            latestWeightKg!! - firstWeightKg!! else null
    val latestBodyFatPercent: Double? get() = bodyFatEntries.maxByOrNull { it.time }?.percent
    val bmi: Double?
        get() {
            val w = latestWeightKg ?: return null
            val h = heightCm ?: return null
            if (h <= 0) return null
            val hm = h / 100.0
            return w / (hm * hm)
        }
}

class BodyViewModel(private val repository: BodyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyUiState())
    val uiState: StateFlow<BodyUiState> = _uiState.asStateFlow()

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

    fun load() {
        viewModelScope.launch {
            val range = _uiState.value.selectedRange
            val date = _uiState.value.selectedDate.coerceAtMost(LocalDate.now())
            val period = periodFor(range, date)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                coroutineScope {
                    val weightDeferred = async { repository.loadWeightEntries(period.start, period.end) }
                    val heightDeferred = async { repository.loadLatestHeight() }
                    val bodyFatDeferred = async { repository.loadBodyFatEntries(period.start, period.end) }
                    val leanMassDeferred = async { repository.loadLatestLeanBodyMass() }
                    val bmrDeferred = async { repository.loadLatestBMR() }
                    val boneMassDeferred = async { repository.loadLatestBoneMass() }
                    BodyLoadResult(
                        weightEntries = weightDeferred.await(),
                        heightCm = heightDeferred.await(),
                        bodyFatEntries = bodyFatDeferred.await(),
                        leanMassKg = leanMassDeferred.await(),
                        bmrKcal = bmrDeferred.await(),
                        boneMassKg = boneMassDeferred.await(),
                    )
                }
            }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        weightEntries = result.weightEntries,
                        heightCm = result.heightCm,
                        bodyFatEntries = result.bodyFatEntries,
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
        val weightEntries: List<WeightEntry>,
        val heightCm: Double?,
        val bodyFatEntries: List<BodyFatEntry>,
        val leanMassKg: Double?,
        val bmrKcal: Double?,
        val boneMassKg: Double?,
    )
}
