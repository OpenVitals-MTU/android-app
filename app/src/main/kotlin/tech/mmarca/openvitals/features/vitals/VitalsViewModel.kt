package tech.mmarca.openvitals.features.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.repository.VitalsRepository
import tech.mmarca.openvitals.ui.components.periodFor
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VitalsUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
    val missingPermissions: Set<String> = emptySet(),
    val error: String? = null,
) {
    val hasData: Boolean
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

class VitalsViewModel(private val repository: VitalsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    val phase3Permissions: Set<String> get() = repository.phase3Permissions

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
        val today = LocalDate.now()
        if (!periodFor(_uiState.value.selectedRange, nextDate).end.isAfter(today)) {
            _uiState.value = _uiState.value.copy(selectedDate = nextDate)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date.coerceAtMost(LocalDate.now()))
        load()
    }

    fun onPermissionsResult(granted: Set<String>) {
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
                    val missingDeferred = async { repository.missingPermissions() }
                    val bloodPressureDeferred = async { repository.loadBloodPressure(period.start, period.end) }
                    val spO2Deferred = async { repository.loadSpO2(period.start, period.end) }
                    val respiratoryRateDeferred = async { repository.loadRespiratoryRate(period.start, period.end) }
                    val bodyTemperatureDeferred = async { repository.loadBodyTemperature(period.start, period.end) }
                    val vo2MaxDeferred = async { repository.loadVo2Max(period.start, period.end) }
                    VitalsLoadResult(
                        missingPermissions = missingDeferred.await(),
                        bloodPressure = bloodPressureDeferred.await(),
                        spO2 = spO2Deferred.await(),
                        respiratoryRate = respiratoryRateDeferred.await(),
                        bodyTemperature = bodyTemperatureDeferred.await(),
                        vo2Max = vo2MaxDeferred.await(),
                    )
                }
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    missingPermissions = result.missingPermissions,
                    bloodPressure = result.bloodPressure,
                    spO2 = result.spO2,
                    respiratoryRate = result.respiratoryRate,
                    bodyTemperature = result.bodyTemperature,
                    vo2Max = result.vo2Max,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDate = date,
                    error = error.message,
                )
            }
        }
    }

    private data class VitalsLoadResult(
        val missingPermissions: Set<String>,
        val bloodPressure: List<BloodPressureEntry>,
        val spO2: List<SpO2Entry>,
        val respiratoryRate: List<RespiratoryRateEntry>,
        val bodyTemperature: List<BodyTempEntry>,
        val vo2Max: List<Vo2MaxEntry>,
    )
}
