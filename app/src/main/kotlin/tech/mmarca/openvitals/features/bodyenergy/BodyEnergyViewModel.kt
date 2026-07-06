package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile

@Immutable
data class BodyEnergyUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.DAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val result: BodyEnergyTimelineResult? = null,
    val display: BodyEnergyDisplayState = BodyEnergyDisplayState(),
    val calibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    val error: ScreenError? = null,
)

@HiltViewModel
class BodyEnergyViewModel(
    private val repository: BodyEnergyRepository,
    private val preferencesRepository: PreferencesRepository,
    private val calibrationChanges: Flow<BodyEnergyCalibration> = emptyFlow(),
    private val bodyProfileChanges: Flow<BodyProfile> = emptyFlow(),
) : ViewModel() {

    @Inject
    constructor(
        repository: BodyEnergyRepository,
        preferencesRepository: PreferencesRepository,
    ) : this(
        repository = repository,
        preferencesRepository = preferencesRepository,
        calibrationChanges = preferencesRepository.bodyEnergyCalibrationFlow,
        bodyProfileChanges = preferencesRepository.bodyProfileFlow,
    )

    private val periodDriver = PeriodSelectionDriver(
        initialRange = TimeRange.DAY,
        initialWeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        onRangeSelected = {},
    )
    private val loadCoordinator = LoadCoordinator()
    private val _uiState = MutableStateFlow(
        BodyEnergyUiState(
            selectedRange = TimeRange.DAY,
            selectedDate = periodDriver.selection.selectedDate,
            calibration = preferencesRepository.bodyEnergyCalibration(),
        )
    )
    val uiState: StateFlow<BodyEnergyUiState> = _uiState.asStateFlow()

    init {
        observeCalibration()
        observeBodyProfile()
        load()
    }

    private fun observeCalibration() {
        viewModelScope.launch {
            calibrationChanges.drop(1).collect { calibration ->
                _uiState.value = _uiState.value.copy(calibration = calibration)
            }
        }
    }

    private fun observeBodyProfile() {
        viewModelScope.launch {
            bodyProfileChanges.drop(1).collect {
                load(RefreshMode.FORCE)
            }
        }
    }

    fun completeSetup(calibration: BodyEnergyCalibration) {
        preferencesRepository.setBodyEnergyCalibration(calibration.copy(setupCompleted = true))
        load(RefreshMode.FORCE)
    }

    fun useAutomatic() {
        preferencesRepository.setBodyEnergyCalibration(
            BodyEnergyCalibration.Automatic.copy(setupCompleted = true)
        )
        load(RefreshMode.FORCE)
    }

    fun selectRange(range: TimeRange) {
        if (range != TimeRange.DAY) return
    }

    fun previousPeriod() {
        applyPeriodSelection(periodDriver.previousPeriod())
        load()
    }

    fun nextPeriod() {
        periodDriver.nextPeriod()?.let { selection ->
            applyPeriodSelection(selection)
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

    fun refresh() {
        load(RefreshMode.FORCE)
    }

    private fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val selection = periodDriver.selection
        val period = DatePeriod(selection.selectedDate, selection.selectedDate)
        loadCoordinator.launch(viewModelScope) load@{
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
            )
            runCatching {
                repository.loadTimeline(
                    BodyEnergyTimelineQuery(
                        period = period,
                        range = TimeRange.DAY,
                        refreshMode = refreshMode,
                    )
                )
            }.onSuccess { result ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    result = result,
                    display = result.toBodyEnergyDisplayState(),
                    error = null,
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.toScreenError("Unable to load Body Energy."),
                )
            }
        }
    }

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = TimeRange.DAY,
            selectedDate = selection.selectedDate,
        )
    }
}
