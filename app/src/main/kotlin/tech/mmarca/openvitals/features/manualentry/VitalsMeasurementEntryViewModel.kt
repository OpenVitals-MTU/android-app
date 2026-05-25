package tech.mmarca.openvitals.features.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.data.repository.VitalsRepository

private const val MinSystolicMmHg = 20.0
private const val MaxSystolicMmHg = 200.0
private const val MinDiastolicMmHg = 10.0
private const val MaxDiastolicMmHg = 180.0
private const val MaxPercent = 100.0
private const val MaxRespiratoryRate = 1000.0
private const val MaxBodyTemperatureCelsius = 100.0

enum class VitalsMeasurementEntryError {
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

data class VitalsMeasurementEntryUiState(
    val type: VitalsMeasurementType = VitalsMeasurementType.BLOOD_PRESSURE,
    val inputText: String = "",
    val secondaryInputText: String = "",
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val entryError: VitalsMeasurementEntryError? = null,
    val writeErrorMessage: String? = null,
)

@HiltViewModel
class VitalsMeasurementEntryViewModel @Inject constructor(
    private val repository: VitalsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsMeasurementEntryUiState())
    val uiState: StateFlow<VitalsMeasurementEntryUiState> = _uiState.asStateFlow()

    fun setType(type: VitalsMeasurementType) {
        if (_uiState.value.type == type) {
            refreshPermission()
            return
        }
        _uiState.value = VitalsMeasurementEntryUiState(type = type)
        refreshPermission()
    }

    fun refreshPermission() {
        val type = _uiState.value.type
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeErrorMessage = null,
            )
            runCatching {
                repository.vitalsWritePermissions(type) to repository.hasVitalsWritePermission(type)
            }.onSuccess { (writePermissions, canWrite) ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = writePermissions,
                    canWrite = canWrite,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = repository.vitalsWritePermissions(type),
                    canWrite = false,
                    entryError = VitalsMeasurementEntryError.WRITE_FAILED,
                    writeErrorMessage = error.message,
                )
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(
            inputText = text,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun updateSecondaryInput(text: String) {
        _uiState.value = _uiState.value.copy(
            secondaryInputText = text,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun addEntry(value: Double?, secondaryValue: Double? = null) {
        val current = _uiState.value
        if (!current.canWrite) {
            _uiState.value = current.copy(
                entryError = VitalsMeasurementEntryError.MISSING_WRITE_PERMISSION,
                writeErrorMessage = null,
            )
            return
        }
        if (!isValidVitalsValue(current.type, value, secondaryValue)) {
            _uiState.value = current.copy(
                entryError = VitalsMeasurementEntryError.INVALID_VALUE,
                writeErrorMessage = null,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                writeErrorMessage = null,
            )
            runCatching {
                repository.writeVitalsMeasurementEntry(
                    VitalsMeasurementWriteRequest(
                        type = current.type,
                        time = Instant.now(),
                        value = requireNotNull(value),
                        secondaryValue = secondaryValue,
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    inputText = "",
                    secondaryInputText = "",
                    isSavingEntry = false,
                    entryError = null,
                    writeErrorMessage = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = VitalsMeasurementEntryError.WRITE_FAILED,
                    writeErrorMessage = error.message,
                )
            }
        }
    }
}

private fun isValidVitalsValue(
    type: VitalsMeasurementType,
    value: Double?,
    secondaryValue: Double?,
): Boolean {
    value ?: return false
    return when (type) {
        VitalsMeasurementType.BLOOD_PRESSURE -> {
            val diastolic = secondaryValue ?: return false
            value >= MinSystolicMmHg &&
                value <= MaxSystolicMmHg &&
                diastolic >= MinDiastolicMmHg &&
                diastolic <= MaxDiastolicMmHg &&
                value > diastolic
        }
        VitalsMeasurementType.SPO2 -> value > 0.0 && value <= MaxPercent
        VitalsMeasurementType.RESPIRATORY_RATE -> value > 0.0 && value <= MaxRespiratoryRate
        VitalsMeasurementType.BODY_TEMPERATURE -> value > 0.0 && value <= MaxBodyTemperatureCelsius
    }
}
