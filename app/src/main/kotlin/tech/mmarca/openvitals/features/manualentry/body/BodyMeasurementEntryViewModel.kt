package tech.mmarca.openvitals.features.manualentry.body

import tech.mmarca.openvitals.R

import androidx.compose.runtime.Immutable
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
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.navigation.BODY_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.BODY_MEASUREMENT_TYPE_ARG

enum class BodyMeasurementEntryError {
    NOTHING_TO_SAVE,
    INVALID_VALUE,
    BODY_WATER_NEEDS_WEIGHT,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

/**
 * One form for body values. Opened without a type it is a weigh-in: every value a body
 * analysis scale shows, saved together with one time. Opened for one type, or to edit
 * one record, it shows that value alone.
 */
@Immutable
data class BodyMeasurementEntryUiState(
    val types: List<BodyMeasurementType> = BodyCompositionTypes,
    val inputs: Map<BodyMeasurementType, String> = emptyMap(),
    val bodyWaterInput: BodyWaterInput = BodyWaterInput.PERCENT,
    /** The fields the last save attempt rejected. */
    val invalidTypes: Set<BodyMeasurementType> = emptySet(),
    val writePermissions: Set<String> = emptySet(),
    val grantedTypes: Set<BodyMeasurementType> = emptySet(),
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val editRecordId: String? = null,
    val entryTime: Instant? = null,
    val saveCompleted: Boolean = false,
    val entryError: BodyMeasurementEntryError? = null,
    val writeError: ScreenError? = null,
) {
    val isEditMode: Boolean
        get() = editRecordId != null

    val isComposition: Boolean
        get() = types.size > 1

    val filledTypes: List<BodyMeasurementType>
        get() = types.filter { inputs[it].orEmpty().isNotBlank() }

    /** A share of body weight needs the weight on the same form; alone, body water is a mass. */
    val offersBodyWaterPercent: Boolean
        get() = BodyMeasurementType.WEIGHT in types && BodyMeasurementType.BODY_WATER_MASS in types

    val bodyWaterUnit: BodyWaterInput
        get() = if (offersBodyWaterPercent) bodyWaterInput else BodyWaterInput.MASS

    /** Nothing on the form can be written, or a value already typed cannot. */
    val needsWritePermission: Boolean
        get() = !isCheckingPermission &&
            (types.none { it in grantedTypes } || filledTypes.any { it !in grantedTypes })

    val canSave: Boolean
        get() = !isSavingEntry && !isCheckingPermission &&
            filledTypes.isNotEmpty() && filledTypes.all { it in grantedTypes }
}

@HiltViewModel
class BodyMeasurementEntryViewModel @Inject constructor(
    private val repository: BodyRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val editRecordId: String? = savedStateHandle[BODY_ENTRY_ID_ARG]

    private val _uiState = MutableStateFlow(
        BodyMeasurementEntryUiState(
            types = savedStateHandle.get<String>(BODY_MEASUREMENT_TYPE_ARG)
                ?.let { name -> BodyMeasurementType.entries.firstOrNull { it.name == name } }
                ?.let(::listOf)
                ?: BodyCompositionTypes,
            editRecordId = editRecordId,
        )
    )
    val uiState: StateFlow<BodyMeasurementEntryUiState> = _uiState.asStateFlow()

    private var editEntryRequested = false

    fun start(units: BodyEntryUnits = BodyEntryUnits()) {
        refreshPermission()
        if (editRecordId != null && !editEntryRequested) {
            editEntryRequested = true
            loadEditEntry(editRecordId, units)
        }
    }

    fun refreshPermission() {
        val types = _uiState.value.types
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                types.filterTo(mutableSetOf()) { repository.hasBodyWritePermission(it) }
            }.onSuccess { grantedTypes ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = types.writePermissions(),
                    grantedTypes = grantedTypes,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = types.writePermissions(),
                    grantedTypes = emptySet(),
                    entryError = BodyMeasurementEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun updateInput(type: BodyMeasurementType, text: String) = update {
        copy(inputs = inputs + (type to text), invalidTypes = invalidTypes - type)
    }

    fun setBodyWaterInput(input: BodyWaterInput) = update {
        copy(bodyWaterInput = input, invalidTypes = invalidTypes - BodyMeasurementType.BODY_WATER_MASS)
    }

    fun updateEntryTime(time: Instant) = update { copy(entryTime = time.coerceAtMost(Instant.now())) }

    fun save(units: BodyEntryUnits = BodyEntryUnits()) {
        val current = _uiState.value
        val values = when (val result = bodyEntryValues(current.types, current.inputs, current.bodyWaterUnit, units)) {
            is BodyEntryValues.Invalid -> {
                _uiState.value = current.copy(
                    invalidTypes = result.types,
                    entryError = result.error,
                    writeError = null,
                )
                return
            }
            is BodyEntryValues.Valid -> result.values
        }
        if (values.keys.any { it !in current.grantedTypes }) {
            _uiState.value = current.copy(
                entryError = BodyMeasurementEntryError.MISSING_WRITE_PERMISSION,
                writeError = null,
            )
            return
        }
        val time = current.entryTime?.coerceAtMost(Instant.now()) ?: Instant.now()
        val requests = values.map { (type, value) -> BodyMeasurementWriteRequest(type = type, time = time, value = value) }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                if (current.editRecordId == null) {
                    repository.writeBodyMeasurementEntries(requests)
                } else {
                    repository.updateBodyMeasurementEntry(current.editRecordId, requests.single())
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    inputs = if (_uiState.value.isEditMode) _uiState.value.inputs else emptyMap(),
                    isSavingEntry = false,
                    saveCompleted = true,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = BodyMeasurementEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun loadEditEntry(recordId: String, units: BodyEntryUnits) {
        val type = _uiState.value.types.single()
        viewModelScope.launch {
            runCatching {
                repository.loadBodyMeasurementEntry(type, recordId)
            }.onSuccess { entry ->
                if (entry == null || !entry.isOpenVitalsEntry) {
                    _uiState.value = _uiState.value.copy(
                        entryError = BodyMeasurementEntryError.WRITE_FAILED,
                        writeError = ScreenError.Text(R.string.screen_error_entry_not_editable),
                    )
                    return@onSuccess
                }
                _uiState.value = _uiState.value.copy(
                    inputs = mapOf(type to entry.value.toBodyMeasurementInput(type, units.systemFor(type))),
                    entryTime = entry.time.coerceAtMost(Instant.now()),
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    entryError = BodyMeasurementEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    private fun List<BodyMeasurementType>.writePermissions(): Set<String> =
        flatMapTo(linkedSetOf()) { repository.bodyWritePermissions(it) }

    private inline fun update(transform: BodyMeasurementEntryUiState.() -> BodyMeasurementEntryUiState) {
        _uiState.value = _uiState.value.transform().copy(saveCompleted = false, entryError = null, writeError = null)
    }
}
