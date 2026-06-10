package tech.mmarca.openvitals.features.manualentry.body

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.navigation.BODY_ENTRY_ID_ARG

private const val MaxWeightKg = 1000.0
private const val MaxHeightCm = 300.0
private const val MaxBodyFatPercent = 100.0
private const val PoundsPerKilogram = 2.2046226218
private const val CentimetersPerInch = 2.54

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
    val editRecordId: String? = null,
    val editTime: Instant? = null,
    val saveCompleted: Boolean = false,
    val entryError: BodyMeasurementEntryError? = null,
    val writeErrorMessage: String? = null,
) {
    val isEditMode: Boolean
        get() = editRecordId != null
}

@HiltViewModel
class BodyMeasurementEntryViewModel @Inject constructor(
    private val repository: BodyRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    constructor(repository: BodyRepository) : this(repository, SavedStateHandle())

    private val editRecordId: String? = savedStateHandle[BODY_ENTRY_ID_ARG]

    private val _uiState = MutableStateFlow(BodyMeasurementEntryUiState(editRecordId = editRecordId))
    val uiState: StateFlow<BodyMeasurementEntryUiState> = _uiState.asStateFlow()

    fun setType(type: BodyMeasurementType, unitSystem: UnitSystem = UnitSystem.METRIC) {
        if (_uiState.value.type == type) {
            refreshPermission()
            if (_uiState.value.isEditMode && _uiState.value.editTime == null) {
                loadEditEntry(type, unitSystem)
            }
            return
        }
        _uiState.value = BodyMeasurementEntryUiState(
            type = type,
            editRecordId = editRecordId,
        )
        refreshPermission()
        loadEditEntry(type, unitSystem)
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
            saveCompleted = false,
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
                val request = BodyMeasurementWriteRequest(
                    type = current.type,
                    time = current.editTime ?: Instant.now(),
                    value = canonicalValue,
                )
                if (current.editRecordId == null) {
                    repository.writeBodyMeasurementEntry(request)
                } else {
                    repository.updateBodyMeasurementEntry(current.editRecordId, request)
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    inputText = if (_uiState.value.isEditMode) _uiState.value.inputText else "",
                    isSavingEntry = false,
                    saveCompleted = _uiState.value.isEditMode,
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

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun loadEditEntry(type: BodyMeasurementType, unitSystem: UnitSystem) {
        val recordId = editRecordId ?: return
        viewModelScope.launch {
            runCatching {
                repository.loadBodyMeasurementEntry(type, recordId)
            }.onSuccess { entry ->
                if (entry == null || !entry.isOpenVitalsEntry) {
                    _uiState.value = _uiState.value.copy(
                        entryError = BodyMeasurementEntryError.WRITE_FAILED,
                        writeErrorMessage = "Only OpenVitals entries can be edited.",
                    )
                    return@onSuccess
                }
                _uiState.value = _uiState.value.copy(
                    inputText = entry.value.toDisplayInput(type, unitSystem),
                    editTime = entry.time,
                    entryError = null,
                    writeErrorMessage = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
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

private fun Double.toDisplayInput(type: BodyMeasurementType, unitSystem: UnitSystem): String {
    val displayValue = when (type) {
        BodyMeasurementType.WEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) this * PoundsPerKilogram else this
        BodyMeasurementType.HEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) this / CentimetersPerInch else this
        BodyMeasurementType.BODY_FAT -> this
    }
    return displayValue.toInputText()
}

private fun Double.toInputText(): String =
    "%.2f"
        .format(java.util.Locale.US, this)
        .trimEnd('0')
        .trimEnd('.')
