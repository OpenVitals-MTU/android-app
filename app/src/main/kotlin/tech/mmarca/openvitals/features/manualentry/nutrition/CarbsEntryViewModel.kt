package tech.mmarca.openvitals.features.manualentry.nutrition

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest

private const val MaxCarbsGrams = 10000.0

enum class CarbsEntryError {
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

@Immutable
data class CarbsEntryUiState(
    val inputText: String = "",
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val saveCompleted: Boolean = false,
    val entryError: CarbsEntryError? = null,
    val writeError: ScreenError? = null,
)

@HiltViewModel
class CarbsEntryViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CarbsEntryUiState())
    val uiState: StateFlow<CarbsEntryUiState>
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
                    entryError = CarbsEntryError.WRITE_FAILED,
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

    fun addEntry(carbsGrams: Double?) {
        val current = _uiState.value
        if (!current.canWrite) {
            _uiState.value = current.copy(
                entryError = CarbsEntryError.MISSING_WRITE_PERMISSION,
                writeError = null,
            )
            return
        }
        if (carbsGrams == null || !carbsGrams.isValidCarbsGrams()) {
            _uiState.value = current.copy(
                entryError = CarbsEntryError.INVALID_VALUE,
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
                repository.writeCarbsEntry(
                    NutritionWriteRequest(
                        time = Instant.now(),
                        carbsGrams = carbsGrams,
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    inputText = "",
                    isSavingEntry = false,
                    saveCompleted = true,
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = CarbsEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun Double.isValidCarbsGrams(): Boolean =
        isFinite() && this > 0.0 && this <= MaxCarbsGrams
}
