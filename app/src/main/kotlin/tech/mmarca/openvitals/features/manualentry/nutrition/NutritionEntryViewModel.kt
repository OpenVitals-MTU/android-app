package tech.mmarca.openvitals.features.manualentry.nutrition

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** Always on the form, in this order. */
internal val PrimaryNutritionEntryNutrients: List<NutritionNutrient> = listOf(
    NutritionNutrient.ENERGY,
    NutritionNutrient.PROTEIN,
    NutritionNutrient.TOTAL_FAT,
    NutritionNutrient.TOTAL_CARBOHYDRATE,
)

/** Every other Health Connect nutrient. Caffeine is logged as a drink, so it is not here. */
internal val AddableNutritionEntryNutrients: List<NutritionNutrient> =
    NutritionNutrient.entries.filter { nutrient ->
        nutrient !in PrimaryNutritionEntryNutrients && nutrient != NutritionNutrient.CAFFEINE
    }

enum class NutritionEntryError {
    NO_VALUES,
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

@Immutable
data class NutritionEntryUiState(
    val rows: List<NutrientInputRow> = PrimaryNutritionEntryNutrients.map(::NutrientInputRow),
    /** Null means now. */
    val timestamp: Instant? = null,
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val saveCompleted: Boolean = false,
    val entryError: NutritionEntryError? = null,
    val writeError: ScreenError? = null,
) {
    val addableNutrients: List<NutritionNutrient>
        get() = AddableNutritionEntryNutrients - rows.map { it.nutrient }.toSet()
}

/**
 * One Health Connect nutrition record from typed amounts: the main macros plus any other
 * nutrient the user adds. Blank fields are left out of the record.
 */
@HiltViewModel
class NutritionEntryViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NutritionEntryUiState())
    val uiState: StateFlow<NutritionEntryUiState>
        get() = _uiState.asStateFlow()

    init {
        refreshPermission()
    }

    fun refreshPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                repository.nutritionWritePermissions to repository.hasNutritionWritePermission()
            }.onSuccess { (writePermissions, canWrite) ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = writePermissions,
                    canWrite = canWrite,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = repository.nutritionWritePermissions,
                    canWrite = false,
                    entryError = NutritionEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun updateAmount(nutrient: NutritionNutrient, text: String) {
        editForm { rows ->
            rows.map { row -> if (row.nutrient == nutrient) row.copy(amountText = text) else row }
        }
    }

    fun addNutrient(nutrient: NutritionNutrient) {
        if (nutrient !in _uiState.value.addableNutrients) return
        editForm { rows -> rows + NutrientInputRow(nutrient) }
    }

    fun removeNutrient(nutrient: NutritionNutrient) {
        if (nutrient in PrimaryNutritionEntryNutrients) return
        editForm { rows -> rows.filterNot { it.nutrient == nutrient } }
    }

    fun updateTimestamp(timestamp: Instant) {
        _uiState.value = _uiState.value.copy(
            timestamp = timestamp,
            saveCompleted = false,
            entryError = null,
            writeError = null,
        )
    }

    fun addEntry(unitSystem: UnitSystem) {
        val current = _uiState.value
        if (!current.canWrite) {
            failEntry(NutritionEntryError.MISSING_WRITE_PERMISSION)
            return
        }
        val nutrientValues = current.rows.parsedMetricNutrientValues(unitSystem)
        if (nutrientValues == null) {
            failEntry(NutritionEntryError.INVALID_VALUE)
            return
        }
        if (nutrientValues.isEmpty()) {
            failEntry(NutritionEntryError.NO_VALUES)
            return
        }
        val now = Instant.now()
        val time = current.timestamp?.coerceAtMost(now) ?: now

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                // Save means saved: a screen closed mid-write must not lose the record.
                withContext(NonCancellable) {
                    repository.writeNutritionEntry(
                        NutritionWriteRequest(
                            time = time,
                            nutrientValues = nutrientValues,
                            isManualNutritionEntry = true,
                        )
                    )
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    rows = PrimaryNutritionEntryNutrients.map(::NutrientInputRow),
                    timestamp = null,
                    isSavingEntry = false,
                    saveCompleted = true,
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = NutritionEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun editForm(transform: (List<NutrientInputRow>) -> List<NutrientInputRow>) {
        val current = _uiState.value
        _uiState.value = current.copy(
            rows = transform(current.rows),
            saveCompleted = false,
            entryError = null,
            writeError = null,
        )
    }

    private fun failEntry(error: NutritionEntryError) {
        _uiState.value = _uiState.value.copy(entryError = error, writeError = null)
    }
}
