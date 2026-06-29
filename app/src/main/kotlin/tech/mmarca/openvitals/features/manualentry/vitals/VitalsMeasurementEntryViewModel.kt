package tech.mmarca.openvitals.features.manualentry.vitals

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.navigation.VITALS_ENTRY_ID_ARG

private const val MinSystolicMmHg = 20.0
private const val MaxSystolicMmHg = 200.0
private const val MinDiastolicMmHg = 10.0
private const val MaxDiastolicMmHg = 180.0
private const val MaxPercent = 100.0
private const val MaxRespiratoryRate = 1000.0
private const val MaxBodyTemperatureCelsius = 100.0
private const val FahrenheitFreezingPoint = 32.0
private const val FahrenheitPerCelsius = 1.8

enum class VitalsMeasurementEntryError {
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

@Immutable
data class VitalsMeasurementEntryUiState(
    val type: VitalsMeasurementType = VitalsMeasurementType.BLOOD_PRESSURE,
    val inputText: String = "",
    val secondaryInputText: String = "",
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val editRecordId: String? = null,
    val editTime: Instant? = null,
    val saveCompleted: Boolean = false,
    val entryError: VitalsMeasurementEntryError? = null,
    val writeError: ScreenError? = null,
) {
    val isEditMode: Boolean
        get() = editRecordId != null
}

@HiltViewModel
class VitalsMeasurementEntryViewModel @Inject constructor(
    private val repository: VitalsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    constructor(repository: VitalsRepository) : this(repository, SavedStateHandle())

    private val editRecordId: String? = savedStateHandle[VITALS_ENTRY_ID_ARG]

    private val _uiState = MutableStateFlow(VitalsMeasurementEntryUiState(editRecordId = editRecordId))
    val uiState: StateFlow<VitalsMeasurementEntryUiState> = _uiState.asStateFlow()

    fun setType(type: VitalsMeasurementType, unitSystem: UnitSystem = UnitSystem.METRIC) {
        if (_uiState.value.type == type) {
            refreshPermission()
            if (_uiState.value.isEditMode && _uiState.value.editTime == null) {
                loadEditEntry(type, unitSystem)
            }
            return
        }
        _uiState.value = VitalsMeasurementEntryUiState(
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
                writeError = null,
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
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(
            inputText = text,
            saveCompleted = false,
            entryError = null,
            writeError = null,
        )
    }

    fun updateSecondaryInput(text: String) {
        _uiState.value = _uiState.value.copy(
            secondaryInputText = text,
            saveCompleted = false,
            entryError = null,
            writeError = null,
        )
    }

    fun updateEntryTime(time: Instant) {
        _uiState.value = _uiState.value.copy(
            editTime = time.coerceAtMost(Instant.now()),
            saveCompleted = false,
            entryError = null,
            writeError = null,
        )
    }

    fun addEntry(value: Double?, secondaryValue: Double? = null) {
        val current = _uiState.value
        if (!current.canWrite) {
            _uiState.value = current.copy(
                entryError = VitalsMeasurementEntryError.MISSING_WRITE_PERMISSION,
                writeError = null,
            )
            return
        }
        if (!isValidVitalsValue(current.type, value, secondaryValue)) {
            _uiState.value = current.copy(
                entryError = VitalsMeasurementEntryError.INVALID_VALUE,
                writeError = null,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                val request = VitalsMeasurementWriteRequest(
                    type = current.type,
                    time = current.editTime?.coerceAtMost(Instant.now()) ?: Instant.now(),
                    value = requireNotNull(value),
                    secondaryValue = secondaryValue,
                )
                if (current.editRecordId == null) {
                    repository.writeVitalsMeasurementEntry(request)
                } else {
                    repository.updateVitalsMeasurementEntry(current.editRecordId, request)
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    inputText = if (_uiState.value.isEditMode) _uiState.value.inputText else "",
                    secondaryInputText = if (_uiState.value.isEditMode) _uiState.value.secondaryInputText else "",
                    isSavingEntry = false,
                    saveCompleted = true,
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = VitalsMeasurementEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun loadEditEntry(type: VitalsMeasurementType, unitSystem: UnitSystem) {
        val recordId = editRecordId ?: return
        viewModelScope.launch {
            runCatching {
                repository.loadVitalsMeasurementEntry(type, recordId)
            }.onSuccess { entry ->
                if (entry == null || !entry.isOpenVitalsEntry) {
                    _uiState.value = _uiState.value.copy(
                        entryError = VitalsMeasurementEntryError.WRITE_FAILED,
                        writeError = ScreenError.Message("Only OpenVitals entries can be edited."),
                    )
                    return@onSuccess
                }
                _uiState.value = _uiState.value.copy(
                    inputText = entry.value.toDisplayInput(type, unitSystem),
                    secondaryInputText = entry.secondaryValue?.toInputText().orEmpty(),
                    editTime = entry.time.coerceAtMost(Instant.now()),
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    entryError = VitalsMeasurementEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
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

private fun Double.toDisplayInput(type: VitalsMeasurementType, unitSystem: UnitSystem): String {
    val displayValue = when (type) {
        VitalsMeasurementType.BODY_TEMPERATURE -> if (unitSystem == UnitSystem.IMPERIAL) {
            this * FahrenheitPerCelsius + FahrenheitFreezingPoint
        } else {
            this
        }
        else -> this
    }
    return displayValue.toInputText()
}

private fun Double.toInputText(): String =
    "%.2f"
        .format(Locale.US, this)
        .trimEnd('0')
        .trimEnd('.')
