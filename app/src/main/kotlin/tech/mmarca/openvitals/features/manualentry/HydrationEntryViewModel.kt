package tech.mmarca.openvitals.features.manualentry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.model.HydrationWriteRequest
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.navigation.HYDRATION_ENTRY_ID_ARG

internal const val MillilitersPerLiter = 1000.0
private const val MaxHealthConnectHydrationLiters = 100.0
internal const val MinHydrationContainerMilliliters = 1.0
internal const val MaxHydrationContainerMilliliters =
    MaxHealthConnectHydrationLiters * MillilitersPerLiter

enum class HydrationBeverage(
    val hydrationMultiplier: Double,
) {
    WATER(1.0),
    COFFEE(1.0),
    TEA(1.0),
    SOFT_DRINK(1.0),
    ENERGY_DRINK(1.0),
    SPORTS_DRINK(1.1),
    ORAL_REHYDRATION_SOLUTION(1.5),
    MILK(1.5),
    FRUIT_JUICE(1.3);

    companion object {
        val DisplayOrder = listOf(
            WATER,
            COFFEE,
            ENERGY_DRINK,
            FRUIT_JUICE,
            MILK,
            ORAL_REHYDRATION_SOLUTION,
            SOFT_DRINK,
            SPORTS_DRINK,
            TEA,
        )
    }
}

data class HydrationContainerOption(
    val id: String,
    val volumeMilliliters: Double,
) {
    val volumeLiters: Double
        get() = volumeMilliliters / MillilitersPerLiter

    companion object {
        val Defaults = listOf(
            HydrationContainerOption("coffee_cup", 100.0),
            HydrationContainerOption("tea_cup", 150.0),
            HydrationContainerOption("small_cup", 175.0),
            HydrationContainerOption("medium_glass", 200.0),
            HydrationContainerOption("large_glass", 300.0),
            HydrationContainerOption("water_bottle", 500.0),
            HydrationContainerOption("large_bottle", 1000.0),
        )
    }
}

enum class HydrationEntryError {
    INVALID_AMOUNT,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

data class HydrationEntryUiState(
    val isCheckingPermission: Boolean = true,
    val hydrationWritePermissions: Set<String> = emptySet(),
    val canWriteHydration: Boolean = false,
    val todayHydrationLiters: Double = 0.0,
    val isSavingEntry: Boolean = false,
    val beverageOptions: List<HydrationBeverage> = HydrationBeverage.DisplayOrder,
    val selectedBeverage: HydrationBeverage = HydrationBeverage.WATER,
    val containerOptions: List<HydrationContainerOption> = HydrationContainerOption.Defaults,
    val selectedContainer: HydrationContainerOption = HydrationContainerOption.Defaults.first(),
    val editRecordId: String? = null,
    val editTime: Instant? = null,
    val saveCompleted: Boolean = false,
    val entryError: HydrationEntryError? = null,
    val writeErrorMessage: String? = null,
) {
    val isEditMode: Boolean
        get() = editRecordId != null

    val selectedContainerEffectiveLiters: Double
        get() = selectedContainer.volumeLiters * selectedBeverage.hydrationMultiplier
}

@HiltViewModel
class HydrationEntryViewModel @Inject constructor(
    private val repository: HydrationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    constructor(repository: HydrationRepository) : this(repository, SavedStateHandle())

    private val editRecordId: String? = savedStateHandle[HYDRATION_ENTRY_ID_ARG]

    private val _uiState = MutableStateFlow(
        HydrationEntryUiState()
    )
    val uiState: StateFlow<HydrationEntryUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(editRecordId = editRecordId)
        refresh()
        loadEditEntry()
    }

    fun refresh() {
        refreshPermission()
        refreshTodayHydration()
    }

    fun refreshPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeErrorMessage = null,
            )
            runCatching {
                repository.hydrationWritePermissions to repository.hasHydrationWritePermission()
            }.onSuccess { (hydrationWritePermissions, canWriteHydration) ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    hydrationWritePermissions = hydrationWritePermissions,
                    canWriteHydration = canWriteHydration,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    hydrationWritePermissions = repository.hydrationWritePermissions,
                    canWriteHydration = false,
                    entryError = HydrationEntryError.WRITE_FAILED,
                    writeErrorMessage = error.message,
                )
            }
        }
    }

    fun refreshTodayHydration(today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            runCatching {
                repository.loadDailyHydration(today, today).sumOf { it.liters }
            }.onSuccess { liters ->
                _uiState.value = _uiState.value.copy(todayHydrationLiters = liters)
            }
        }
    }

    fun selectBeverage(beverage: HydrationBeverage) {
        _uiState.value = _uiState.value.copy(
            selectedBeverage = beverage,
            saveCompleted = false,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun selectContainer(container: HydrationContainerOption) {
        _uiState.value = _uiState.value.copy(
            selectedContainer = container,
            saveCompleted = false,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun updateContainerSize(container: HydrationContainerOption, milliliters: Double) {
        if (!isValidHydrationContainerMilliliters(milliliters)) {
            _uiState.value = _uiState.value.copy(
                entryError = HydrationEntryError.INVALID_AMOUNT,
                writeErrorMessage = null,
            )
            return
        }

        val updatedContainer = container.copy(volumeMilliliters = milliliters)
        _uiState.value = _uiState.value.copy(
            containerOptions = _uiState.value.containerOptions.map { option ->
                if (option.id == container.id) updatedContainer else option
            },
            selectedContainer = updatedContainer,
            saveCompleted = false,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun addSelectedHydrationEntry() {
        saveHydrationEntry(_uiState.value.selectedContainer.volumeLiters)
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun loadEditEntry() {
        val recordId = editRecordId ?: return
        viewModelScope.launch {
            runCatching {
                repository.loadHydrationEntry(recordId)
            }.onSuccess { entry ->
                if (entry == null || !entry.isOpenVitalsEntry) {
                    _uiState.value = _uiState.value.copy(
                        entryError = HydrationEntryError.WRITE_FAILED,
                        writeErrorMessage = "Only OpenVitals entries can be edited.",
                    )
                    return@onSuccess
                }
                val existingOptions = _uiState.value.containerOptions
                val option = existingOptions
                    .firstOrNull { kotlin.math.abs(it.volumeLiters - entry.liters) < 0.0001 }
                    ?: HydrationContainerOption("current_entry", entry.liters * MillilitersPerLiter)
                val options = (listOf(option) + existingOptions)
                    .distinctBy { it.id }
                _uiState.value = _uiState.value.copy(
                    selectedBeverage = HydrationBeverage.WATER,
                    containerOptions = options,
                    selectedContainer = option,
                    editTime = entry.startTime,
                    entryError = null,
                    writeErrorMessage = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    entryError = HydrationEntryError.WRITE_FAILED,
                    writeErrorMessage = error.message,
                )
            }
        }
    }

    private fun saveHydrationEntry(rawLiters: Double) {
        val current = _uiState.value
        if (!current.canWriteHydration) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.MISSING_WRITE_PERMISSION,
                writeErrorMessage = null,
            )
            return
        }

        val effectiveLiters = rawLiters * current.selectedBeverage.hydrationMultiplier
        if (effectiveLiters <= 0.0 || effectiveLiters > MaxHealthConnectHydrationLiters) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.INVALID_AMOUNT,
                writeErrorMessage = null,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                saveCompleted = false,
                entryError = null,
                writeErrorMessage = null,
            )
            runCatching {
                val request = HydrationWriteRequest(
                    time = current.editTime ?: Instant.now(),
                    volumeLiters = effectiveLiters,
                )
                if (current.editRecordId == null) {
                    repository.writeHydrationEntry(request)
                } else {
                    repository.updateHydrationEntry(current.editRecordId, request)
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    todayHydrationLiters = if (current.isEditMode) {
                        _uiState.value.todayHydrationLiters
                    } else {
                        _uiState.value.todayHydrationLiters + effectiveLiters
                    },
                    saveCompleted = current.isEditMode,
                    entryError = null,
                    writeErrorMessage = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = HydrationEntryError.WRITE_FAILED,
                    writeErrorMessage = error.message,
                )
            }
        }
    }
}

internal fun isValidHydrationContainerMilliliters(milliliters: Double): Boolean =
    milliliters >= MinHydrationContainerMilliliters &&
        milliliters <= MaxHydrationContainerMilliliters &&
        !milliliters.isNaN() &&
        !milliliters.isInfinite()
