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
import tech.mmarca.openvitals.data.model.BodyMeasurementType
import tech.mmarca.openvitals.data.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.data.repository.BodyRepository

private const val MaxWeightKg = 1000.0
private const val MaxHeightCm = 300.0
private const val MaxBodyFatPercent = 100.0

enum class BodyMeasurementEntryError {
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

data class BodyMeasurementEntryUiState(
    val type: BodyMeasurementType = BodyMeasurementType.WEIGHT,
    val inputText: String = "",
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val entryError: BodyMeasurementEntryError? = null,
    val writeErrorMessage: String? = null,
)

@HiltViewModel
class BodyMeasurementEntryViewModel @Inject constructor(
    private val repository: BodyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementEntryUiState())
    val uiState: StateFlow<BodyMeasurementEntryUiState> = _uiState.asStateFlow()

    fun setType(type: BodyMeasurementType) {
        if (_uiState.value.type == type) {
            refreshPermission()
            return
        }
        _uiState.value = BodyMeasurementEntryUiState(type = type)
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
                repository.bodyWritePermissions(type) to repository.hasBodyWritePermission(type)
            }.onSuccess { (writePermissions, canWrite) ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = writePermissions,
                    canWrite = canWrite,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = repository.bodyWritePermissions(type),
                    canWrite = false,
                    entryError = BodyMeasurementEntryError.WRITE_FAILED,
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

    fun addEntry(canonicalValue: Double?) {
        val current = _uiState.value
        if (!current.canWrite) {
            _uiState.value = current.copy(
                entryError = BodyMeasurementEntryError.MISSING_WRITE_PERMISSION,
                writeErrorMessage = null,
            )
            return
        }
        if (canonicalValue == null || !canonicalValue.isValidFor(current.type)) {
            _uiState.value = current.copy(
                entryError = BodyMeasurementEntryError.INVALID_VALUE,
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
                repository.writeBodyMeasurementEntry(
                    BodyMeasurementWriteRequest(
                        type = current.type,
                        time = Instant.now(),
                        value = canonicalValue,
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    inputText = "",
                    isSavingEntry = false,
                    entryError = null,
                    writeErrorMessage = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = BodyMeasurementEntryError.WRITE_FAILED,
                    writeErrorMessage = error.message,
                )
            }
        }
    }

    private fun Double.isValidFor(type: BodyMeasurementType): Boolean =
        when (type) {
            BodyMeasurementType.WEIGHT -> this > 0.0 && this <= MaxWeightKg
            BodyMeasurementType.HEIGHT -> this > 0.0 && this <= MaxHeightCm
            BodyMeasurementType.BODY_FAT -> this >= 0.0 && this <= MaxBodyFatPercent
        }
}
