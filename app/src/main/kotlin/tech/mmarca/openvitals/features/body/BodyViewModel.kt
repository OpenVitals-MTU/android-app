package tech.mmarca.openvitals.features.body

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class BodyUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    val weightEntries: List<WeightEntry> = emptyList(),
    val previousWeightEntries: List<WeightEntry> = emptyList(),
    val baselineWeightEntries: List<WeightEntry> = emptyList(),
    val heightEntries: List<HeightEntry> = emptyList(),
    val previousHeightEntries: List<HeightEntry> = emptyList(),
    val baselineHeightEntries: List<HeightEntry> = emptyList(),
    val bodyFatEntries: List<BodyFatEntry> = emptyList(),
    val previousBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val baselineBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val leanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val previousLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val baselineLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val bmrEntries: List<BmrEntry> = emptyList(),
    val previousBmrEntries: List<BmrEntry> = emptyList(),
    val baselineBmrEntries: List<BmrEntry> = emptyList(),
    val boneMassEntries: List<BoneMassEntry> = emptyList(),
    val previousBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val baselineBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val bodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val previousBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val baselineBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val display: BodyDisplayState = BodyDisplayState(),
    val error: ScreenError? = null,
)

@HiltViewModel
class BodyViewModel(
    private val repository: BodyRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    initialRange: TimeRange = TimeRange.MONTH,
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    private val weekPeriodModeChanges: Flow<WeekPeriodMode> = emptyFlow(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: BodyRepository,
        preferencesRepository: PreferencesRepository,
        dispatchers: DispatcherProvider,
    ) : this(
        repository = repository,
        dispatchers = dispatchers,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.BODY),
        initialWeekPeriodMode = preferencesRepository.weekPeriodMode,
        weekPeriodModeChanges = preferencesRepository.weekPeriodModeFlow,
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.BODY, range)
        },
    )

    private val periodDriver = PeriodSelectionDriver(
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        onRangeSelected = onRangeSelected,
    )
    private val _uiState = MutableStateFlow(
        BodyUiState(
            selectedRange = initialRange,
            weekPeriodMode = initialWeekPeriodMode,
        )
    )
    val uiState: StateFlow<BodyUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observeWeekPeriodMode()
        load()
    }

    private fun observeWeekPeriodMode() {
        viewModelScope.launch {
            weekPeriodModeChanges.drop(1).collect { mode ->
                periodDriver.weekPeriodMode = mode
                _uiState.value = _uiState.value.copy(weekPeriodMode = mode)
                if (_uiState.value.selectedRange == TimeRange.WEEK) {
                    load()
                }
            }
        }
    }

    fun selectRange(range: TimeRange) {
        applyPeriodSelection(periodDriver.selectRange(range))
        load()
    }

    fun previousPeriod() {
        applyPeriodSelection(periodDriver.previousPeriod())
        load()
    }

    fun nextPeriod() {
        periodDriver.nextPeriod()?.let { next ->
            applyPeriodSelection(next)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDate(date))
        load()
    }

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load(RefreshMode.FORCE)
            return
        }
        applyPeriodSelection(selection)
        load()
    }

    fun deleteBodyMeasurementEntry(type: BodyMeasurementType, entryId: String) {
        if (entryId.isBlank()) return
        val entryIsOpenVitals = when (type) {
            BodyMeasurementType.WEIGHT -> _uiState.value.weightEntries
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
            BodyMeasurementType.HEIGHT -> _uiState.value.heightEntries
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
            BodyMeasurementType.BODY_FAT -> _uiState.value.bodyFatEntries
                .firstOrNull { it.id == entryId }
                ?.isOpenVitalsEntry
        } ?: return
        if (!entryIsOpenVitals) return

        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.withDeletedBodyMeasurementEntry(type, entryId)
            runCatching {
                repository.deleteBodyMeasurementEntry(type, entryId)
            }.onSuccess {
                load(RefreshMode.FORCE)
            }.onFailure { error ->
                _uiState.value = previous.copy(error = error.toScreenError())
            }
        }
    }

    fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
        loadCoordinator.launch(viewModelScope) load@{
            val query = PeriodLoadQuery(
                range = periodDriver.selection.selectedRange,
                anchorDate = periodDriver.selection.selectedDate,
                weekPeriodMode = _uiState.value.weekPeriodMode,
            )
            val date = query.selectedDate
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                if (refreshMode == RefreshMode.NORMAL) {
                    repository.loadBodyPeriod(query, BodyPeriodMetric.ALL)
                } else {
                    repository.loadBodyPeriod(query, BodyPeriodMetric.ALL, refreshMode)
                }
            }
                .onSuccess { result ->
                    if (!isCurrent) return@load
                    val display = withContext(dispatchers.default) {
                        BodyPresentationMapper.build(query = query, data = result)
                    }
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        weightEntries = result.weightEntries,
                        previousWeightEntries = result.previousWeightEntries,
                        baselineWeightEntries = result.baselineWeightEntries,
                        heightEntries = result.heightEntries,
                        previousHeightEntries = result.previousHeightEntries,
                        baselineHeightEntries = result.baselineHeightEntries,
                        bodyFatEntries = result.bodyFatEntries,
                        previousBodyFatEntries = result.previousBodyFatEntries,
                        baselineBodyFatEntries = result.baselineBodyFatEntries,
                        leanMassEntries = result.leanMassEntries,
                        previousLeanMassEntries = result.previousLeanMassEntries,
                        baselineLeanMassEntries = result.baselineLeanMassEntries,
                        bmrEntries = result.bmrEntries,
                        previousBmrEntries = result.previousBmrEntries,
                        baselineBmrEntries = result.baselineBmrEntries,
                        boneMassEntries = result.boneMassEntries,
                        previousBoneMassEntries = result.previousBoneMassEntries,
                        baselineBoneMassEntries = result.baselineBoneMassEntries,
                        bodyWaterMassEntries = result.bodyWaterMassEntries,
                        previousBodyWaterMassEntries = result.previousBodyWaterMassEntries,
                        baselineBodyWaterMassEntries = result.baselineBodyWaterMassEntries,
                        display = display,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedDate = date,
                        error = it.toScreenError(),
                    )
                }
        }
    }

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = selection.selectedRange,
            selectedDate = selection.selectedDate,
        )
    }
}

private fun BodyUiState.withDisplay(): BodyUiState {
    val query = PeriodLoadQuery(
        range = selectedRange,
        anchorDate = selectedDate,
        weekPeriodMode = weekPeriodMode,
    )
    return copy(display = BodyPresentationMapper.build(query = query, state = this))
}

private fun BodyUiState.withDeletedBodyMeasurementEntry(
    type: BodyMeasurementType,
    entryId: String,
): BodyUiState =
    when (type) {
        BodyMeasurementType.WEIGHT -> copy(
            weightEntries = weightEntries.filterNot { it.id == entryId },
            error = null,
        )
        BodyMeasurementType.HEIGHT -> copy(
            heightEntries = heightEntries.filterNot { it.id == entryId },
            error = null,
        )
        BodyMeasurementType.BODY_FAT -> copy(
            bodyFatEntries = bodyFatEntries.filterNot { it.id == entryId },
            error = null,
        )
    }.withDisplay()
