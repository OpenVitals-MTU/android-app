package tech.mmarca.openvitals.features.manualentry.hydration

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.NutritionPeriodData
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.navigation.HYDRATION_ENTRY_ID_ARG

internal const val MillilitersPerLiter = 1000.0
private const val MaxHealthConnectHydrationLiters = 100.0
private const val MaxCustomDrinkNutrientValue = 10000.0
private const val DefaultCustomDrinkHydrationMultiplier = 1.0
internal const val MinHydrationContainerMilliliters = 1.0
internal const val MaxHydrationContainerMilliliters =
    MaxHealthConnectHydrationLiters * MillilitersPerLiter

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

data class CustomHydrationDrinkInput(
    val name: String,
    val volumeMilliliters: Double,
    val hydrationMultiplier: Double = 1.0,
    val nutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
)

enum class HydrationEntryError {
    INVALID_AMOUNT,
    INVALID_CUSTOM_DRINK,
    MISSING_WRITE_PERMISSION,
    MISSING_NUTRITION_WRITE_PERMISSION,
    WRITE_FAILED,
}

enum class HydrationEntryNotice {
    NON_HYDRATING_DRINK_SAVED,
}

@Immutable
data class HydrationEntryUiState(
    val isCheckingPermission: Boolean = true,
    val hydrationWritePermissions: Set<String> = emptySet(),
    val nutritionWritePermissions: Set<String> = emptySet(),
    val canWriteHydration: Boolean = false,
    val canWriteNutrition: Boolean = false,
    val todayHydrationLiters: Double = 0.0,
    val dailyGoalLiters: Double = 2.0,
    val isSavingEntry: Boolean = false,
    val containerOptions: List<HydrationContainerOption> = HydrationContainerOption.Defaults,
    val selectedContainer: HydrationContainerOption = HydrationContainerOption.Defaults.first(),
    val lastCustomAmountMilliliters: Double? = null,
    val customDrinkOptions: List<CustomHydrationDrink> = emptyList(),
    val editRecordId: String? = null,
    val editTime: Instant? = null,
    val saveCompleted: Boolean = false,
    val entryNotice: HydrationEntryNotice? = null,
    val entryError: HydrationEntryError? = null,
    val writeError: ScreenError? = null,
) {
    val isEditMode: Boolean
        get() = editRecordId != null

    val writePermissions: Set<String>
        get() = hydrationWritePermissions + nutritionWritePermissions
}

@HiltViewModel
class HydrationEntryViewModel @Inject constructor(
    private val repository: HydrationRepository,
    private val nutritionRepository: NutritionRepository,
    savedStateHandle: SavedStateHandle,
    private val reminderController: HydrationReminderController? = null,
) : ViewModel() {
    constructor(repository: HydrationRepository) : this(
        repository,
        NoopNutritionRepository,
        SavedStateHandle(),
        null,
    )

    constructor(
        repository: HydrationRepository,
        nutritionRepository: NutritionRepository,
    ) : this(repository, nutritionRepository, SavedStateHandle(), null)

    constructor(
        repository: HydrationRepository,
        reminderController: HydrationReminderController,
    ) : this(repository, NoopNutritionRepository, SavedStateHandle(), reminderController)

    private val editRecordId: String? = savedStateHandle[HYDRATION_ENTRY_ID_ARG]

    private val _uiState = MutableStateFlow(
        initialHydrationEntryState(repository, editRecordId)
    )
    val uiState: StateFlow<HydrationEntryUiState> = _uiState.asStateFlow()

    init {
        refresh()
        loadEditEntry()
    }

    fun refresh() {
        refreshPermission()
        refreshDailyGoal()
        refreshTodayHydration()
    }

    fun refreshPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                HydrationEntryPermissionState(
                    hydrationWritePermissions = repository.hydrationWritePermissions,
                    nutritionWritePermissions = nutritionRepository.nutritionWritePermissions,
                    canWriteHydration = repository.hasHydrationWritePermission(),
                    canWriteNutrition = nutritionRepository.hasNutritionWritePermission(),
                )
            }.onSuccess { permissions ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    hydrationWritePermissions = permissions.hydrationWritePermissions,
                    nutritionWritePermissions = permissions.nutritionWritePermissions,
                    canWriteHydration = permissions.canWriteHydration,
                    canWriteNutrition = permissions.canWriteNutrition,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    hydrationWritePermissions = repository.hydrationWritePermissions,
                    nutritionWritePermissions = nutritionRepository.nutritionWritePermissions,
                    canWriteHydration = false,
                    canWriteNutrition = false,
                    entryError = HydrationEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun refreshDailyGoal() {
        _uiState.value = _uiState.value.copy(
            dailyGoalLiters = repository.hydrationDailyGoalLiters(),
        )
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

    fun selectContainer(container: HydrationContainerOption) {
        _uiState.value = _uiState.value.copy(
            selectedContainer = container,
            saveCompleted = false,
            entryNotice = null,
            entryError = null,
            writeError = null,
        )
    }

    fun updateEntryTime(time: Instant) {
        _uiState.value = _uiState.value.copy(
            editTime = time.coerceAtMost(Instant.now()),
            saveCompleted = false,
            entryNotice = null,
            entryError = null,
            writeError = null,
        )
    }

    fun updateContainerSize(container: HydrationContainerOption, milliliters: Double) {
        if (!isValidHydrationContainerMilliliters(milliliters)) {
            _uiState.value = _uiState.value.copy(
                entryError = HydrationEntryError.INVALID_AMOUNT,
                entryNotice = null,
                writeError = null,
            )
            return
        }

        val updatedContainer = container.copy(volumeMilliliters = milliliters)
        if (HydrationContainerOption.Defaults.any { it.id == container.id }) {
            repository.setHydrationContainerVolumeMilliliters(container.id, milliliters)
        }
        _uiState.value = _uiState.value.copy(
            containerOptions = _uiState.value.containerOptions.map { option ->
                if (option.id == container.id) updatedContainer else option
            },
            selectedContainer = updatedContainer,
            saveCompleted = false,
            entryNotice = null,
            entryError = null,
            writeError = null,
        )
    }

    fun addSelectedHydrationEntry() {
        saveHydrationEntry(_uiState.value.selectedContainer.volumeLiters)
    }

    fun addContainerHydrationEntry(container: HydrationContainerOption) {
        if (_uiState.value.isEditMode) {
            selectContainer(container)
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedContainer = container,
            saveCompleted = false,
            entryNotice = null,
            entryError = null,
            writeError = null,
        )
        saveHydrationEntry(container.volumeLiters)
    }

    fun addCustomHydrationEntry(milliliters: Double) {
        if (!isValidHydrationContainerMilliliters(milliliters)) {
            _uiState.value = _uiState.value.copy(
                entryError = HydrationEntryError.INVALID_AMOUNT,
                entryNotice = null,
                writeError = null,
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            lastCustomAmountMilliliters = milliliters,
            entryNotice = null,
        )
        repository.setLastCustomHydrationAmountMilliliters(milliliters)
        saveHydrationEntry(milliliters / MillilitersPerLiter)
    }

    fun saveCustomDrink(
        input: CustomHydrationDrinkInput,
        existingDrinkId: String? = null,
    ) {
        val existingDrink = existingDrinkId
            ?.let { id -> _uiState.value.customDrinkOptions.firstOrNull { it.id == id } }
        val drink = input.toCustomHydrationDrink(
            id = existingDrinkId ?: UUID.randomUUID().toString(),
        )?.copy(
            category = existingDrink?.category,
            isPreloaded = existingDrink?.isPreloaded ?: false,
        )
        if (drink == null) {
            _uiState.value = _uiState.value.copy(
                entryError = HydrationEntryError.INVALID_CUSTOM_DRINK,
                entryNotice = null,
                writeError = null,
            )
            return
        }
        repository.saveCustomHydrationDrink(drink)
        refreshDrinkOptions {
            copy(
                entryError = null,
                entryNotice = null,
                writeError = null,
                saveCompleted = false,
            )
        }
    }

    fun deleteCustomDrink(drink: CustomHydrationDrink) {
        repository.deleteCustomHydrationDrink(drink.id)
        refreshDrinkOptions {
            copy(
                entryError = null,
                entryNotice = null,
                writeError = null,
                saveCompleted = false,
            )
        }
    }

    fun moveCustomDrinkToTarget(
        drinkId: String,
        targetDrinkId: String,
    ) {
        if (drinkId == targetDrinkId) return
        val current = _uiState.value.customDrinkOptions
        val fromIndex = current.indexOfFirst { it.id == drinkId }
        val targetIndex = current.indexOfFirst { it.id == targetDrinkId }
        if (fromIndex < 0 || targetIndex < 0) return
        val updated = current.toMutableList().apply {
            val drink = removeAt(fromIndex)
            add(targetIndex.coerceIn(0, size), drink)
        }
        repository.reorderCustomHydrationDrinks(updated.map { it.id })
        _uiState.value = _uiState.value.copy(
            customDrinkOptions = updated,
            entryError = null,
            entryNotice = null,
            writeError = null,
            saveCompleted = false,
        )
    }

    fun moveCustomDrinkToCategory(
        drinkId: String,
        category: CaffeineSourceCategory?,
    ) {
        repository.moveCustomHydrationDrinkToCategory(drinkId, category)
        refreshDrinkOptions {
            copy(
                entryError = null,
                entryNotice = null,
                writeError = null,
                saveCompleted = false,
            )
        }
    }

    fun addSavedCustomDrinkEntry(drink: CustomHydrationDrink) {
        logCustomDrinkEntry(drink)
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun logCustomDrinkEntry(drink: CustomHydrationDrink) {
        if (!drink.isValidCustomHydrationDrink()) {
            _uiState.value = _uiState.value.copy(
                entryError = HydrationEntryError.INVALID_CUSTOM_DRINK,
                entryNotice = null,
                writeError = null,
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            lastCustomAmountMilliliters = drink.volumeMilliliters,
            entryNotice = null,
        )
        repository.setLastCustomHydrationAmountMilliliters(drink.volumeMilliliters)
        saveHydrationEntry(
            rawLiters = drink.volumeLiters,
            hydrationMultiplier = drink.hydrationMultiplier,
            nutritionName = drink.name,
            nutrientValues = drink.nutrientValues,
        )
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
                        writeError = ScreenError.Message("Only OpenVitals entries can be edited."),
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
                    containerOptions = options,
                    selectedContainer = option,
                    editTime = entry.startTime.coerceAtMost(Instant.now()),
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    entryError = HydrationEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    private fun refreshDrinkOptions(
        transform: HydrationEntryUiState.() -> HydrationEntryUiState = { this },
    ) {
        _uiState.value = _uiState.value.transform().copy(
            customDrinkOptions = repository.customHydrationDrinks()
                .filter(CustomHydrationDrink::isValidCustomHydrationDrink),
        )
    }

    private fun saveHydrationEntry(
        rawLiters: Double,
        hydrationMultiplier: Double = DefaultCustomDrinkHydrationMultiplier,
        nutritionName: String? = null,
        nutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
    ) {
        val current = _uiState.value
        if (!isValidCustomDrinkHydrationMultiplier(hydrationMultiplier)) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.INVALID_CUSTOM_DRINK,
                entryNotice = null,
                writeError = null,
            )
            return
        }

        val effectiveLiters = rawLiters * hydrationMultiplier
        val writesHydration = effectiveLiters > 0.0
        val writesNutrition = nutrientValues.isNotEmpty()
        if (current.editRecordId != null && !writesHydration) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.INVALID_AMOUNT,
                entryNotice = null,
                writeError = null,
            )
            return
        }
        if (writesHydration && !current.canWriteHydration) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.MISSING_WRITE_PERMISSION,
                entryNotice = null,
                writeError = null,
            )
            return
        }
        if (writesNutrition && !current.canWriteNutrition) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.MISSING_NUTRITION_WRITE_PERMISSION,
                entryNotice = null,
                writeError = null,
            )
            return
        }
        if (writesHydration && effectiveLiters > MaxHealthConnectHydrationLiters) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.INVALID_AMOUNT,
                entryNotice = null,
                writeError = null,
            )
            return
        }
        if (!writesHydration && !writesNutrition) {
            _uiState.value = current.copy(
                entryError = HydrationEntryError.INVALID_CUSTOM_DRINK,
                entryNotice = null,
                writeError = null,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                saveCompleted = false,
                entryNotice = null,
                entryError = null,
                writeError = null,
            )
            runCatching {
                val entryTime = current.editTime?.coerceAtMost(Instant.now()) ?: Instant.now()
                if (current.editRecordId == null) {
                    val hydrationClientRecordId = if (writesHydration) {
                        repository.writeHydrationEntry(
                            HydrationWriteRequest(
                                time = entryTime,
                                volumeLiters = effectiveLiters,
                            )
                        )
                    } else {
                        null
                    }
                    if (writesNutrition) {
                        nutritionRepository.writeNutritionEntry(
                            NutritionWriteRequest(
                                time = entryTime,
                                nutrientValues = nutrientValues,
                                name = nutritionName,
                                associatedHydrationClientRecordId = hydrationClientRecordId,
                            )
                        )
                    }
                } else {
                    repository.updateHydrationEntry(
                        current.editRecordId,
                        HydrationWriteRequest(
                            time = entryTime,
                            volumeLiters = effectiveLiters,
                        )
                    )
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    todayHydrationLiters = if (current.isEditMode) {
                        _uiState.value.todayHydrationLiters
                    } else {
                        _uiState.value.todayHydrationLiters + effectiveLiters
                    },
                    saveCompleted = true,
                    entryNotice = if (!writesHydration && writesNutrition) {
                        HydrationEntryNotice.NON_HYDRATING_DRINK_SAVED
                    } else {
                        null
                    },
                    entryError = null,
                    writeError = null,
                )
                if (effectiveLiters > 0.0) {
                    runCatching { reminderController?.hideReminderNotification() }
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = HydrationEntryError.WRITE_FAILED,
                    entryNotice = null,
                    writeError = error.toScreenError(),
                )
            }
        }
    }
}

private fun initialHydrationEntryState(
    repository: HydrationRepository,
    editRecordId: String?,
): HydrationEntryUiState {
    val options = hydrationContainerOptions(repository)
    return HydrationEntryUiState(
        containerOptions = options,
        selectedContainer = options.first(),
        dailyGoalLiters = repository.hydrationDailyGoalLiters(),
        lastCustomAmountMilliliters = repository.lastCustomHydrationAmountMilliliters()
            ?.takeIf(::isValidHydrationContainerMilliliters),
        customDrinkOptions = repository.customHydrationDrinks()
            .filter(CustomHydrationDrink::isValidCustomHydrationDrink),
        editRecordId = editRecordId,
    )
}

private fun hydrationContainerOptions(repository: HydrationRepository): List<HydrationContainerOption> {
    val volumeOverrides = repository.hydrationContainerVolumeMilliliters()
    return HydrationContainerOption.Defaults.map { option ->
        volumeOverrides[option.id]
            ?.takeIf(::isValidHydrationContainerMilliliters)
            ?.let { option.copy(volumeMilliliters = it) }
            ?: option
    }
}

internal fun isValidHydrationContainerMilliliters(milliliters: Double): Boolean =
    milliliters >= MinHydrationContainerMilliliters &&
        milliliters <= MaxHydrationContainerMilliliters &&
        !milliliters.isNaN() &&
        !milliliters.isInfinite()

private data class HydrationEntryPermissionState(
    val hydrationWritePermissions: Set<String>,
    val nutritionWritePermissions: Set<String>,
    val canWriteHydration: Boolean,
    val canWriteNutrition: Boolean,
)

private fun CustomHydrationDrinkInput.toCustomHydrationDrink(
    id: String,
): CustomHydrationDrink? {
    val normalizedName = name.trim()
    if (normalizedName.isBlank()) return null
    if (!isValidHydrationContainerMilliliters(volumeMilliliters)) return null
    if (!isValidCustomDrinkHydrationMultiplier(hydrationMultiplier)) return null
    val normalizedNutrients = nutrientValues
        .filterValues(::isValidCustomDrinkNutrientValue)
        .toSortedMap(compareBy { it.name })
    if (normalizedNutrients.size != nutrientValues.size) return null
    return CustomHydrationDrink(
        id = id,
        name = normalizedName,
        volumeMilliliters = volumeMilliliters,
        hydrationMultiplier = hydrationMultiplier,
        nutrientValues = normalizedNutrients,
    )
}

private fun CustomHydrationDrink.isValidCustomHydrationDrink(): Boolean =
    id.isNotBlank() &&
        name.isNotBlank() &&
        isValidHydrationContainerMilliliters(volumeMilliliters) &&
        isValidCustomDrinkHydrationMultiplier(hydrationMultiplier) &&
        nutrientValues.values.all(::isValidCustomDrinkNutrientValue)

internal fun isValidCustomDrinkHydrationMultiplier(value: Double): Boolean =
    value >= 0.0 &&
        value <= 1.0 &&
        value.isFinite()

internal fun isValidCustomDrinkNutrientValue(value: Double): Boolean =
    value > 0.0 &&
        value <= MaxCustomDrinkNutrientValue &&
        value.isFinite()

private object NoopNutritionRepository : NutritionRepository {
    override val nutritionWritePermissions: Set<String> = emptySet()

    override suspend fun loadNutritionPeriod(
        query: tech.mmarca.openvitals.core.period.PeriodLoadQuery,
        refreshMode: RefreshMode,
    ): NutritionPeriodData = NutritionPeriodData()

    override suspend fun loadDailyMacros(
        start: LocalDate,
        end: LocalDate,
    ): List<DailyMacros> = emptyList()

    override suspend fun loadNutritionEntries(
        start: LocalDate,
        end: LocalDate,
    ): List<NutritionEntry> = emptyList()

    override suspend fun hasNutritionWritePermission(): Boolean = true

    override suspend fun writeCarbsEntry(request: NutritionWriteRequest): String =
        error("Nutrition repository is not configured.")

    override suspend fun writeNutritionEntry(request: NutritionWriteRequest): String =
        error("Nutrition repository is not configured.")

    override suspend fun deleteNutritionEntry(id: String) = Unit
}
