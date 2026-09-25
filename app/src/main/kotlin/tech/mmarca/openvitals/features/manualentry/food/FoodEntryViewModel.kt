package tech.mmarca.openvitals.features.manualentry.food

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.FoodRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.features.manualentry.nutrition.isValidNutrientInputValue

enum class FoodEntryError {
    INVALID_AMOUNT,
    INVALID_FOOD,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

@Immutable
data class FoodEntryUiState(
    val isCheckingPermission: Boolean = true,
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val todayEnergyKcal: Double = 0.0,
    val isSavingEntry: Boolean = false,
    val foodOptions: List<CustomFood> = emptyList(),
    val saveCompleted: Boolean = false,
    val entryError: FoodEntryError? = null,
    val writeError: ScreenError? = null,
)

/** The food catalog lives in Room; a logged portion is one Health Connect nutrition record. */
@HiltViewModel
class FoodEntryViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodEntryUiState())
    val uiState: StateFlow<FoodEntryUiState> = _uiState.asStateFlow()

    // Food edits reach Room in the order the user made them.
    private val foodEditMutex = Mutex()

    init {
        // Foods come from Room, so they load after construction.
        viewModelScope.launch { refreshFoodOptions() }
        refreshPermission()
        refreshTodayEnergy()
    }

    fun refresh() {
        refreshPermission()
        refreshTodayEnergy()
    }

    fun refreshPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                nutritionRepository.nutritionWritePermissions to nutritionRepository.hasNutritionWritePermission()
            }.onSuccess { (writePermissions, canWrite) ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = writePermissions,
                    canWrite = canWrite,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = nutritionRepository.nutritionWritePermissions,
                    canWrite = false,
                    entryError = FoodEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun refreshTodayEnergy(today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            runCatching {
                nutritionRepository.loadDailyMacros(today, today).sumOf { it.energyKcal }
            }.onSuccess { kcal ->
                _uiState.value = _uiState.value.copy(todayEnergyKcal = kcal)
            }
        }
    }

    fun saveCustomFood(
        input: CustomFoodInput,
        existingFoodId: String? = null,
    ) {
        val food = input.toCustomFood(id = existingFoodId ?: UUID.randomUUID().toString())
        if (food == null) {
            failEntry(FoodEntryError.INVALID_FOOD)
            return
        }
        editFoods { foodRepository.saveCustomFood(food) }
    }

    fun deleteCustomFood(food: CustomFood) {
        editFoods { foodRepository.deleteCustomFood(food.id) }
    }

    /** Writes one nutrition record for a portion of [food]. The nutrients scale with the amount. */
    fun logFood(
        food: CustomFood,
        amountGrams: Double = food.amountGrams,
        entryTime: Instant? = null,
    ) {
        if (!food.isValidCustomFood()) {
            failEntry(FoodEntryError.INVALID_FOOD)
            return
        }
        if (!isValidFoodAmountGrams(amountGrams)) {
            failEntry(FoodEntryError.INVALID_AMOUNT)
            return
        }
        if (!_uiState.value.canWrite) {
            failEntry(FoodEntryError.MISSING_WRITE_PERMISSION)
            return
        }
        val nutrientValues = food.nutrientValuesFor(amountGrams)
        // A large portion can push a nutrient past what Health Connect accepts.
        if (!nutrientValues.values.all(::isValidNutrientInputValue)) {
            failEntry(FoodEntryError.INVALID_AMOUNT)
            return
        }
        val now = Instant.now()
        val time = entryTime?.coerceAtMost(now) ?: now
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                saveCompleted = false,
                entryError = null,
                writeError = null,
            )
            runCatching {
                // Save means saved: a screen closed mid-write must not lose the record.
                withContext(NonCancellable) {
                    nutritionRepository.writeNutritionEntry(
                        NutritionWriteRequest(
                            time = time,
                            nutrientValues = nutrientValues,
                            name = food.name,
                            foodId = food.id,
                        )
                    )
                }
            }.onSuccess {
                val energyKcal = nutrientValues[NutritionNutrient.ENERGY] ?: 0.0
                val current = _uiState.value
                _uiState.value = current.copy(
                    isSavingEntry = false,
                    saveCompleted = true,
                    todayEnergyKcal = if (time.isToday()) current.todayEnergyKcal + energyKcal else current.todayEnergyKcal,
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = FoodEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun failEntry(error: FoodEntryError) {
        _uiState.value = _uiState.value.copy(
            entryError = error,
            writeError = null,
            saveCompleted = false,
        )
    }

    /** One food edit, then the list as Room now has it. */
    private fun editFoods(edit: suspend () -> Unit) {
        viewModelScope.launch {
            foodEditMutex.withLock {
                runCatching { edit() }
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            entryError = null,
                            writeError = null,
                            saveCompleted = false,
                        )
                        refreshFoodOptions()
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            entryError = FoodEntryError.WRITE_FAILED,
                            writeError = error.toScreenError(),
                        )
                    }
            }
        }
    }

    private suspend fun refreshFoodOptions() {
        val foods = runCatching { foodRepository.customFoods() }
            .getOrDefault(emptyList())
            .filter(CustomFood::isValidCustomFood)
        _uiState.value = _uiState.value.copy(foodOptions = foods)
    }
}

private fun Instant.isToday(): Boolean =
    atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
